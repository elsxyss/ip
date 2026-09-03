#!/usr/bin/env python3
"""
Generate a GitHub-style split-view diff of a local Git repo, as a single HTML page.

    python3 generate-split-view-diff.py <repo> <from-rev> <to-rev> [output.html]

Example:

    python3 generate-split-view-diff.py ~/Repos/ip HEAD~1 HEAD _temp/compare-last-two-commits.html

The page shows every changed file one after another (no clicking file-to-file),
each as a two-column before/after table with syntax highlighting, word-level
highlighting inside changed lines, and long runs of unchanged lines folded into
"N unchanged lines" strips that expand on click. Files with no changes are listed
at the bottom as collapsed panels.

Only Python's standard library is needed. Syntax highlighting is done in the
browser by highlight.js loaded from a CDN; if there is no network the page still
works, just without colored tokens.

Either comparison point can be anything `git rev-parse` accepts -- commit SHAs,
tags, branch names, `HEAD~5` -- or the word WORKTREE, meaning the working folder
as it stands right now. So the changes made since the last commit are:

    python3 generate-split-view-diff.py ~/Repos/ip HEAD WORKTREE _temp/since-last-commit.html

The working folder side includes staged and unstaged edits, and treats files Git
does not track yet as additions (gitignored files are left out).
"""
from __future__ import annotations

import argparse
import html
import json
import re
import subprocess
import sys
import webbrowser
from datetime import datetime
from difflib import SequenceMatcher
from pathlib import Path

# --- configuration -----------------------------------------------------------

# Files bigger than this (in either rev) are reported but not diffed line by line.
MAX_FILE_BYTES = 500_000

# Unchanged lines are folded away when a run is longer than FOLD_THRESHOLD;
# CONTEXT_LINES of them are kept visible on each side of the fold.
FOLD_THRESHOLD = 20
CONTEXT_LINES = 6

# Below this similarity, two lines paired by the diff are treated as unrelated
# and get no word-level highlighting (avoids noisy speckling).
MIN_PAIR_SIMILARITY = 0.35

HLJS_VERSION = "11.9.0"
HLJS_URL = f"https://cdn.jsdelivr.net/gh/highlightjs/cdn-release@{HLJS_VERSION}/build/highlight.min.js"

# File extension -> highlight.js language name.
LANGUAGES = {
    "bash": "bash", "sh": "bash", "zsh": "bash", "bat": "dos", "ps1": "powershell",
    "c": "c", "h": "c", "cc": "cpp", "cpp": "cpp", "cxx": "cpp", "hpp": "cpp",
    "cs": "csharp", "css": "css", "scss": "scss", "less": "less",
    "dart": "dart", "diff": "diff", "patch": "diff", "dockerfile": "dockerfile",
    "go": "go", "gradle": "groovy", "groovy": "groovy",
    "html": "xml", "htm": "xml", "xml": "xml", "xsl": "xml", "svg": "xml", "vue": "xml",
    "java": "java", "js": "javascript", "jsx": "javascript", "mjs": "javascript",
    "json": "json", "kt": "kotlin", "kts": "kotlin", "lua": "lua",
    "md": "markdown", "markdown": "markdown", "mdx": "markdown",
    "php": "php", "pl": "perl", "properties": "properties", "puml": "plaintext",
    "py": "python", "r": "r", "rb": "ruby", "rs": "rust",
    "sql": "sql", "swift": "swift", "toml": "ini", "ini": "ini", "cfg": "ini",
    "ts": "typescript", "tsx": "typescript", "txt": "plaintext",
    "yml": "yaml", "yaml": "yaml",
}
LANGUAGES_BY_NAME = {
    "dockerfile": "dockerfile", "makefile": "makefile", "gemfile": "ruby",
    ".gitignore": "plaintext", ".gitattributes": "plaintext",
}

# Either comparison point can be one of these instead of a commit, meaning
# "the working folder exactly as it is now", staged or not.
WORKTREE_ALIASES = {"worktree", "working", "workdir", "wt"}

# Row op codes shared with the JavaScript renderer.
OP_EQUAL, OP_DELETE, OP_INSERT, OP_REPLACE = 0, 1, 2, 3


# --- git helpers -------------------------------------------------------------


class GitError(RuntimeError):
    pass


def git_text(repo: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), *args], capture_output=True, text=True
    )
    if result.returncode != 0:
        raise GitError(result.stderr.strip() or f"git {' '.join(args)} failed")
    return result.stdout


def git_bytes(repo: Path, *args: str) -> bytes | None:
    """Raw output, or None when git refuses (e.g. the path is absent in that rev)."""
    result = subprocess.run(["git", "-C", str(repo), *args], capture_output=True)
    return result.stdout if result.returncode == 0 else None


def resolve_point(repo: Path, rev: str) -> dict:
    """A comparison point: either a commit, or the working folder as it is now."""
    if rev.strip().lower() in WORKTREE_ALIASES:
        branch = git_text(repo, "rev-parse", "--abbrev-ref", "HEAD").strip()
        where = "on a detached HEAD" if branch == "HEAD" else f"on {branch}"
        return {"rev": rev, "worktree": True, "sha": None, "slug": "worktree",
                "short": "working folder", "subject": f"uncommitted state, {where}",
                "author": "", "date": datetime.now().strftime("%Y-%m-%d %H:%M")}
    try:
        sha = git_text(repo, "rev-parse", "--verify", f"{rev}^{{commit}}").strip()
    except GitError as exc:
        raise GitError(f"cannot resolve '{rev}' in {repo}: {exc}") from exc
    fields = git_text(
        repo, "log", "-1", "--format=%h%x1f%s%x1f%an%x1f%ad", "--date=short", sha
    ).strip().split("\x1f")
    short, subject, author, date = (fields + ["", "", "", ""])[:4]
    return {"rev": rev, "worktree": False, "sha": sha, "slug": short, "short": short,
            "subject": subject, "author": author, "date": date}


def parse_name_status(raw: str) -> list[dict]:
    """Turn `git diff --name-status -z` output into one entry per changed file."""
    fields = [f for f in raw.split("\0") if f != ""]
    entries: list[dict] = []
    i = 0
    while i < len(fields):
        code = fields[i][0]
        if code in ("R", "C"):
            entries.append({"status": code, "old_path": fields[i + 1], "path": fields[i + 2]})
            i += 3
        else:
            entries.append({"status": code, "old_path": fields[i + 1], "path": fields[i + 1]})
            i += 2
    return entries


def untracked_paths(repo: Path) -> list[str]:
    """New files on disk that git does not track yet, minus anything gitignored."""
    raw = git_text(repo, "ls-files", "--others", "--exclude-standard", "-z")
    return [f for f in raw.split("\0") if f != ""]


def reversed_entry(entry: dict) -> dict:
    """Flip a commit->worktree entry into a worktree->commit one."""
    flipped = {"A": "D", "D": "A"}.get(entry["status"], entry["status"])
    return {"status": flipped, "old_path": entry["path"], "path": entry["old_path"]}


def changed_entries(repo: Path, base: dict, head: dict) -> list[dict]:
    """One entry per changed file, with renames detected."""
    if not base["worktree"] and not head["worktree"]:
        return parse_name_status(git_text(
            repo, "diff", "--name-status", "-M", "--find-renames=50%",
            "-z", base["sha"], head["sha"]))

    # `git diff <commit>` reads commit -> working folder, staged changes included.
    commit = head if base["worktree"] else base
    entries = parse_name_status(git_text(
        repo, "diff", "--name-status", "-M", "--find-renames=50%", "-z", commit["sha"]))
    entries += [{"status": "A", "old_path": path, "path": path}
                for path in untracked_paths(repo)]
    if base["worktree"]:
        entries = [reversed_entry(entry) for entry in entries]
    return sorted(entries, key=lambda entry: entry["path"])


def files_at(repo: Path, point: dict) -> list[str]:
    """Every file present at a comparison point."""
    if point["worktree"]:
        raw = git_text(repo, "ls-files", "-z")
        tracked = [f for f in raw.split("\0") if f != "" and (repo / f).exists()]
        return sorted(tracked + untracked_paths(repo))
    raw = git_text(repo, "ls-tree", "-r", "--name-only", "-z", point["sha"])
    return [f for f in raw.split("\0") if f != ""]


def file_lines(repo: Path, point: dict, path: str) -> tuple[list[str] | None, str]:
    """(lines, note). lines is None when the file is binary, oversized, or absent."""
    if point["worktree"]:
        target = repo / path
        try:
            data = target.read_bytes() if target.is_file() else None
        except OSError:
            data = None
    else:
        data = git_bytes(repo, "show", f"{point['sha']}:{path}")
    if data is None:
        return None, "missing"
    if len(data) > MAX_FILE_BYTES:
        return None, f"file too large ({len(data):,} bytes)"
    if b"\0" in data:
        return None, "binary file"
    try:
        text = data.decode("utf-8")
    except UnicodeDecodeError:
        try:
            text = data.decode("latin-1")
        except UnicodeDecodeError:
            return None, "binary file"
    if text == "":
        return [], ""
    if text.endswith("\n"):
        text = text[:-1]
    return text.split("\n"), ""


# --- diffing -----------------------------------------------------------------

WORD_RE = re.compile(r"\w+|\s+|.", re.UNICODE)


def word_ranges(old_line: str, new_line: str) -> tuple[list[int], list[int]]:
    """Char ranges (flat [start, end, start, end, ...]) of what actually changed."""
    old_tokens = WORD_RE.findall(old_line)
    new_tokens = WORD_RE.findall(new_line)
    matcher = SequenceMatcher(None, old_tokens, new_tokens, autojunk=False)
    if matcher.ratio() < MIN_PAIR_SIMILARITY:
        return [], []

    old_offsets, pos = [], 0
    for token in old_tokens:
        old_offsets.append(pos)
        pos += len(token)
    old_offsets.append(pos)
    new_offsets, pos = [], 0
    for token in new_tokens:
        new_offsets.append(pos)
        pos += len(token)
    new_offsets.append(pos)

    old_ranges: list[int] = []
    new_ranges: list[int] = []
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag in ("replace", "delete") and i2 > i1:
            old_ranges += [old_offsets[i1], old_offsets[i2]]
        if tag in ("replace", "insert") and j2 > j1:
            new_ranges += [new_offsets[j1], new_offsets[j2]]
    return old_ranges, new_ranges


def build_rows(old_lines: list[str], new_lines: list[str]) -> tuple[list[list], int, int]:
    """Align the two versions into split-view rows; also count added/deleted lines."""
    rows: list[list] = []
    additions = deletions = 0
    matcher = SequenceMatcher(None, old_lines, new_lines, autojunk=False)
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            for offset in range(i2 - i1):
                rows.append([OP_EQUAL, i1 + offset, j1 + offset])
        elif tag == "delete":
            deletions += i2 - i1
            for index in range(i1, i2):
                rows.append([OP_DELETE, index, -1])
        elif tag == "insert":
            additions += j2 - j1
            for index in range(j1, j2):
                rows.append([OP_INSERT, -1, index])
        else:  # replace: pair up line-for-line, spare lines become one-sided rows
            deletions += i2 - i1
            additions += j2 - j1
            paired = min(i2 - i1, j2 - j1)
            for offset in range(paired):
                old_index, new_index = i1 + offset, j1 + offset
                old_range, new_range = word_ranges(old_lines[old_index], new_lines[new_index])
                row = [OP_REPLACE, old_index, new_index]
                if old_range or new_range:
                    row += [old_range, new_range]
                rows.append(row)
            for index in range(i1 + paired, i2):
                rows.append([OP_DELETE, index, -1])
            for index in range(j1 + paired, j2):
                rows.append([OP_INSERT, -1, index])
    return rows, additions, deletions


def language_of(path: str) -> str:
    name = Path(path).name.lower()
    if name in LANGUAGES_BY_NAME:
        return LANGUAGES_BY_NAME[name]
    suffix = Path(name).suffix.lstrip(".")
    return LANGUAGES.get(suffix, "plaintext")


# --- file collection ---------------------------------------------------------

STATUS_LABELS = {"A": "added", "D": "deleted", "M": "modified",
                 "R": "renamed", "C": "copied", "T": "type changed"}


def collect_changed_files(repo: Path, base: dict, head: dict) -> list[dict]:
    files = []
    for entry in changed_entries(repo, base, head):
        status, path, old_path = entry["status"], entry["path"], entry["old_path"]
        old_lines, old_note = ([], "") if status == "A" else file_lines(repo, base, old_path)
        new_lines, new_note = ([], "") if status == "D" else file_lines(repo, head, path)

        record = {
            "path": path,
            "oldPath": old_path if old_path != path else None,
            "status": status,
            "statusLabel": STATUS_LABELS.get(status, status),
            "language": language_of(path),
            "changed": True,
        }
        if old_lines is None or new_lines is None:
            record["note"] = old_note or new_note or "not shown"
            record["additions"] = record["deletions"] = 0
            record["old"], record["new"], record["rows"] = [], [], []
        else:
            rows, additions, deletions = build_rows(old_lines, new_lines)
            record["old"], record["new"], record["rows"] = old_lines, new_lines, rows
            record["additions"], record["deletions"] = additions, deletions
        files.append(record)
    return files


def collect_unchanged_files(repo: Path, head: dict, changed_paths: set[str]) -> list[dict]:
    files = []
    for path in files_at(repo, head):
        if path in changed_paths:
            continue
        lines, note = file_lines(repo, head, path)
        record = {
            "path": path, "oldPath": None, "status": "=", "statusLabel": "unchanged",
            "language": language_of(path), "changed": False,
            "additions": 0, "deletions": 0, "old": [], "new": [], "rows": [],
        }
        if lines is None:
            record["note"] = note
        else:
            record["new"] = lines
            record["rows"] = [[OP_EQUAL, -1, index] for index in range(len(lines))]
        files.append(record)
    return files


# --- page assembly -----------------------------------------------------------

STYLE = """
:root {
  --fg: #1f2328; --muted: #656d76; --border: #d1d9e0; --bg: #ffffff;
  --bg-soft: #f6f8fa; --accent: #0969da;
  --del-bg: #ffebe9; --del-strong: #ffc9c4; --del-num: #ffd8d3;
  --add-bg: #e6ffec; --add-strong: #abf2bc; --add-num: #ccffd8;
  --mono: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
}
* { box-sizing: border-box; }
body {
  margin: 0; background: var(--bg-soft); color: var(--fg);
  font: 14px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
}
a { color: var(--accent); text-decoration: none; }
a:hover { text-decoration: underline; }
.wrap { max-width: 1600px; margin: 0 auto; padding: 0 16px 80px; }

header.page { padding: 24px 0 12px; }
header.page h1 { font-size: 20px; margin: 0 0 12px; }
.revs { display: flex; flex-wrap: wrap; gap: 12px; align-items: stretch; }
.rev {
  flex: 1 1 320px; background: var(--bg); border: 1px solid var(--border);
  border-radius: 6px; padding: 10px 12px;
}
.rev .label { font-size: 11px; text-transform: uppercase; letter-spacing: .04em; color: var(--muted); }
.rev .sha { font-family: var(--mono); font-weight: 600; }
.rev .subject { color: var(--muted); overflow-wrap: anywhere; }
.summary { margin: 14px 0 4px; color: var(--muted); }
.summary strong { color: var(--fg); }
.add-count { color: #1a7f37; font-weight: 600; }
.del-count { color: #cf222e; font-weight: 600; }

.toolbar {
  position: sticky; top: 0; z-index: 20; display: flex; gap: 8px; align-items: center;
  flex-wrap: wrap; padding: 10px 0; background: var(--bg-soft);
  border-bottom: 1px solid var(--border); margin-bottom: 16px;
}
.toolbar input[type=search] {
  flex: 1 1 220px; min-width: 160px; padding: 5px 10px; font-size: 13px;
  border: 1px solid var(--border); border-radius: 6px; background: var(--bg);
}
button {
  font: inherit; font-size: 13px; padding: 5px 10px; cursor: pointer;
  background: var(--bg); border: 1px solid var(--border); border-radius: 6px; color: var(--fg);
}
button:hover { background: var(--bg-soft); }

.index { background: var(--bg); border: 1px solid var(--border); border-radius: 6px; margin-bottom: 16px; }
.index > summary { cursor: pointer; padding: 10px 14px; font-weight: 600; }
.index ol { margin: 0; padding: 0 14px 12px 14px; list-style: none; }
.index li { display: flex; gap: 10px; padding: 2px 0; align-items: baseline; }
.index .path { font-family: var(--mono); font-size: 12px; overflow-wrap: anywhere; }
.index .counts { margin-left: auto; font-family: var(--mono); font-size: 12px; white-space: nowrap; }

.file { background: var(--bg); border: 1px solid var(--border); border-radius: 6px; margin-bottom: 16px; }
.file[hidden] { display: none; }
.file > summary {
  position: sticky; top: 47px; z-index: 10; cursor: pointer; list-style: none;
  display: flex; gap: 10px; align-items: center; flex-wrap: wrap;
  padding: 8px 12px; background: var(--bg-soft);
  border-bottom: 1px solid var(--border); border-radius: 6px 6px 0 0;
}
.file:not([open]) > summary { border-bottom: none; border-radius: 6px; }
.file > summary::-webkit-details-marker { display: none; }
.file > summary::before { content: "\\25be"; color: var(--muted); width: 10px; }
.file:not([open]) > summary::before { content: "\\25b8"; }
.file .name { font-family: var(--mono); font-size: 12.5px; font-weight: 600; overflow-wrap: anywhere; }
.file .from { color: var(--muted); font-weight: 400; }
.badge {
  font-size: 11px; padding: 1px 7px; border-radius: 999px;
  border: 1px solid var(--border); color: var(--muted); background: var(--bg);
}
.badge.added { color: #1a7f37; border-color: #aceebb; background: var(--add-bg); }
.badge.deleted { color: #cf222e; border-color: #ffcecb; background: var(--del-bg); }
.badge.renamed, .badge.copied { color: #9a6700; border-color: #f5d5a3; background: #fff8c5; }
.file .counts { margin-left: auto; font-family: var(--mono); font-size: 12px; white-space: nowrap; }
.note { padding: 14px; color: var(--muted); font-style: italic; }

.diff-wrap { overflow-x: auto; }
table.diff { width: 100%; border-collapse: collapse; table-layout: fixed; font-family: var(--mono); font-size: 12px; }
table.diff col.num { width: 50px; }
table.diff col.code { width: calc(50% - 50px); }
table.diff.single col.code { width: calc(100% - 50px); }
td.num {
  text-align: right; padding: 0 8px; color: var(--muted); background: var(--bg-soft);
  border-right: 1px solid var(--border); user-select: none; vertical-align: top;
  white-space: nowrap; width: 50px;
}
td.code {
  padding: 0 8px; white-space: pre-wrap; overflow-wrap: anywhere; vertical-align: top; tab-size: 4;
}
td.code.side-old { border-right: 1px solid var(--border); }
tr.del td.code, tr.rep td.code.side-old { background: var(--del-bg); }
tr.del td.num.old, tr.rep td.num.old { background: var(--del-num); }
tr.ins td.code, tr.rep td.code.side-new { background: var(--add-bg); }
tr.ins td.num.new, tr.rep td.num.new { background: var(--add-num); }
table.diff td.code.blank {
  background: repeating-linear-gradient(45deg, #fafbfc, #fafbfc 6px, #f0f2f4 6px, #f0f2f4 12px);
}
mark.word { padding: 0; border-radius: 2px; }
td.code.side-old mark.word { background: var(--del-strong); }
td.code.side-new mark.word { background: var(--add-strong); }
tr.fold td { padding: 0; background: #eef3f9; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); }
tr.fold button {
  width: 100%; text-align: left; border: 0; border-radius: 0; background: transparent;
  color: var(--accent); font-family: var(--mono); font-size: 12px; padding: 4px 12px;
}
.pending { padding: 14px; color: var(--muted); }

.hljs-comment, .hljs-quote { color: #6a737d; font-style: italic; }
.hljs-keyword, .hljs-selector-tag, .hljs-doctag, .hljs-formula { color: #cf222e; }
.hljs-string, .hljs-regexp, .hljs-addition, .hljs-attribute, .hljs-meta .hljs-string { color: #0a3069; }
.hljs-number, .hljs-literal, .hljs-variable, .hljs-template-variable, .hljs-tag .hljs-attr { color: #0550ae; }
.hljs-title, .hljs-section, .hljs-title.class_, .hljs-title.function_, .hljs-name { color: #8250df; }
.hljs-built_in, .hljs-type, .hljs-symbol, .hljs-bullet, .hljs-selector-id, .hljs-selector-class { color: #953800; }
.hljs-meta, .hljs-attr, .hljs-params { color: #116329; }
.hljs-emphasis { font-style: italic; }
.hljs-strong { font-weight: 600; }
.hljs-deletion { color: #82071e; }

@media print {
  .toolbar, .index { display: none; }
  .file, .rev { break-inside: avoid; }
}
"""

SCRIPT = r"""
const OP_EQUAL = 0, OP_DELETE = 1, OP_INSERT = 2, OP_REPLACE = 3;
const FOLD_THRESHOLD = __FOLD_THRESHOLD__, CONTEXT_LINES = __CONTEXT_LINES__;

function escapeHtml(text) {
  return text.replace(/[&<>]/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;" }[ch]));
}

/* highlight.js colors a whole file at once (so block comments and multi-line
   strings are read correctly), then we cut the result into lines, re-opening
   any spans that straddle a line break. */
function highlightLines(lines, language) {
  const plain = () => lines.map(escapeHtml);
  if (!lines.length) return [];
  if (!window.hljs || !hljs.getLanguage(language)) return plain();
  let html;
  try {
    html = hljs.highlight(lines.join("\n"), { language, ignoreIllegals: true }).value;
  } catch (err) {
    return plain();
  }
  const out = [];
  const open = [];
  let current = "";
  let i = 0;
  while (i < html.length) {
    const next = html.slice(i).search(/[<\n]/);
    if (next === -1) { current += html.slice(i); break; }
    current += html.slice(i, i + next);
    i += next;
    if (html[i] === "\n") {
      out.push(current + "</span>".repeat(open.length));
      current = open.join("");
      i += 1;
    } else {
      const end = html.indexOf(">", i);
      const tag = html.slice(i, end + 1);
      if (tag[1] === "/") open.pop(); else open.push(tag);
      current += tag;
      i = end + 1;
    }
  }
  out.push(current);
  return out.length === lines.length ? out : plain();
}

/* Wrap the changed character ranges of one line, working on the DOM so the
   offsets stay in sync with the text rather than with the highlighting markup. */
function applyWordRanges(cell, ranges) {
  if (!ranges || !ranges.length) return;
  const walker = document.createTreeWalker(cell, NodeFilter.SHOW_TEXT);
  const nodes = [];
  let node;
  while ((node = walker.nextNode())) nodes.push(node);
  let offset = 0;
  for (const textNode of nodes) {
    const start = offset, end = offset + textNode.nodeValue.length;
    offset = end;
    const pieces = [];
    for (let r = 0; r < ranges.length; r += 2) {
      const from = Math.max(ranges[r], start), to = Math.min(ranges[r + 1], end);
      if (to > from) pieces.push([from - start, to - start]);
    }
    let rest = textNode;
    let consumed = 0;
    for (const [from, to] of pieces) {
      const tail = rest.splitText(from - consumed);
      const after = tail.splitText(to - from);
      const mark = document.createElement("mark");
      mark.className = "word";
      tail.parentNode.replaceChild(mark, tail);
      mark.appendChild(tail);
      rest = after;
      consumed = to;
    }
  }
}

function numCell(index, side) {
  return index >= 0
    ? `<td class="num ${side}">${index + 1}</td>`
    : `<td class="num ${side}"></td>`;
}

/* mode is "split", or "old"/"new" for the one-column view used by files that
   were added, deleted, or not changed at all. */
function renderRow(row, oldHtml, newHtml, mode) {
  const [op, oldIndex, newIndex] = row;
  const cls = { [OP_EQUAL]: "eq", [OP_DELETE]: "del", [OP_INSERT]: "ins", [OP_REPLACE]: "rep" }[op];
  const cell = (index, side, lines) => index >= 0
    ? `<td class="code side-${side}">${lines[index] || "&nbsp;"}</td>`
    : `<td class="code side-${side} blank"></td>`;
  if (mode === "old") return `<tr class="${cls}">${numCell(oldIndex, "old")}${cell(oldIndex, "old", oldHtml)}</tr>`;
  if (mode === "new") return `<tr class="${cls}">${numCell(newIndex, "new")}${cell(newIndex, "new", newHtml)}</tr>`;
  return `<tr class="${cls}">${numCell(oldIndex, "old")}${cell(oldIndex, "old", oldHtml)}`
       + `${numCell(newIndex, "new")}${cell(newIndex, "new", newHtml)}</tr>`;
}

/* Group the rows into segments: long stretches of untouched lines become a
   single clickable "N unchanged lines" strip. */
function segmentRows(rows, foldAll) {
  const segments = [];
  let run = [];
  const flushRun = () => {
    if (!run.length) return;
    if (foldAll || run.length > FOLD_THRESHOLD) {
      const head = foldAll ? [] : run.slice(0, segments.length ? CONTEXT_LINES : 0);
      const tail = foldAll ? [] : run.slice(-CONTEXT_LINES);
      const hidden = run.slice(head.length, run.length - tail.length);
      if (head.length) segments.push({ type: "rows", rows: head });
      if (hidden.length) segments.push({ type: "fold", rows: hidden });
      if (tail.length) segments.push({ type: "rows", rows: tail });
    } else {
      segments.push({ type: "rows", rows: run });
    }
    run = [];
  };
  for (const row of rows) {
    if (row[0] === OP_EQUAL) run.push(row);
    else { flushRun(); segments.push({ type: "rows", rows: [row] }); }
  }
  flushRun();
  /* The last stretch of a file has no following change, so drop its trailing context. */
  return segments;
}

function renderFile(file, container) {
  const mode = file.status === "D" ? "old"
    : (!file.changed || file.status === "A") ? "new" : "split";
  const single = mode !== "split";
  const oldHtml = highlightLines(file.old, file.language);
  const newHtml = highlightLines(file.new, file.language);
  const table = document.createElement("table");
  table.className = "diff" + (single ? " single" : "");
  table.innerHTML = single
    ? '<colgroup><col class="num"><col class="code"></colgroup>'
    : '<colgroup><col class="num"><col class="code"><col class="num"><col class="code"></colgroup>';
  const body = document.createElement("tbody");
  table.appendChild(body);

  const segments = segmentRows(file.rows, !file.changed);
  const chunks = [];
  segments.forEach((segment, index) => {
    if (segment.type === "fold") {
      const label = `⋯  ${segment.rows.length} unchanged line${segment.rows.length === 1 ? "" : "s"}`;
      chunks.push(
        `<tr class="fold" data-segment="${index}"><td colspan="${single ? 2 : 4}">` +
        `<button type="button">${label}</button></td></tr>`
      );
    } else {
      for (const row of segment.rows) chunks.push(renderRow(row, oldHtml, newHtml, mode));
    }
  });
  body.innerHTML = chunks.join("");

  const markRows = (scope) => {
    for (const tr of scope.querySelectorAll("tr.rep")) {
      const row = tr._row;
      if (!row || row.length < 5) continue;
      applyWordRanges(tr.querySelector("td.side-old"), row[3]);
      applyWordRanges(tr.querySelector("td.side-new"), row[4]);
    }
  };
  /* Attach each rendered <tr> to its row data so word marks can be applied. */
  const attach = (scope, rows) => {
    const trs = scope.querySelectorAll("tr:not(.fold)");
    let cursor = 0;
    for (const tr of trs) { if (!tr._row) tr._row = rows[cursor++]; }
  };
  const flatRows = [];
  segments.forEach(segment => { if (segment.type !== "fold") flatRows.push(...segment.rows); });
  attach(body, flatRows);
  markRows(body);

  body.addEventListener("click", event => {
    const button = event.target.closest("tr.fold button");
    if (!button) return;
    const tr = button.closest("tr.fold");
    const segment = segments[Number(tr.dataset.segment)];
    const holder = document.createElement("tbody");
    holder.innerHTML = segment.rows.map(row => renderRow(row, oldHtml, newHtml, mode)).join("");
    const fragment = document.createDocumentFragment();
    while (holder.firstChild) fragment.appendChild(holder.firstChild);
    tr.replaceWith(fragment);
  });

  container.innerHTML = "";
  const scroller = document.createElement("div");
  scroller.className = "diff-wrap";
  scroller.appendChild(table);
  container.appendChild(scroller);
}

function setup() {
  const files = window.DIFF_DATA.files;
  const byIndex = new Map();
  document.querySelectorAll(".file").forEach(element => {
    byIndex.set(element, files[Number(element.dataset.index)]);
  });

  const render = element => {
    const file = byIndex.get(element);
    const slot = element.querySelector(".slot");
    if (!file || !slot || slot.dataset.done === "1") return;
    slot.dataset.done = "1";
    renderFile(file, slot);
  };

  const sections = [...document.querySelectorAll(".file")];
  sections.forEach(element => {
    element.addEventListener("toggle", () => { if (element.open) render(element); });
  });

  /* Changed files are all built up front -- so browser find-in-page and printing
     see everything -- but in small batches, so the page is usable right away.
     Files that did not change are built only when their panel is opened. */
  const queue = sections.filter(element => element.open);
  const renderBatch = () => {
    const started = performance.now();
    while (queue.length && performance.now() - started < 40) render(queue.shift());
    if (queue.length) setTimeout(renderBatch, 0);
  };
  renderBatch();
  window.addEventListener("beforeprint", () => sections.forEach(render));

  document.getElementById("expand-all").addEventListener("click", () => {
    document.querySelectorAll(".file").forEach(element => { element.open = true; render(element); });
  });
  document.getElementById("collapse-all").addEventListener("click", () => {
    document.querySelectorAll(".file").forEach(element => { element.open = false; });
  });

  const filter = document.getElementById("filter");
  filter.addEventListener("input", () => {
    const needle = filter.value.trim().toLowerCase();
    document.querySelectorAll(".file").forEach(element => {
      const path = element.dataset.path.toLowerCase();
      element.hidden = needle !== "" && !path.includes(needle);
    });
    document.querySelectorAll(".index li").forEach(item => {
      item.hidden = needle !== "" && !item.dataset.path.toLowerCase().includes(needle);
    });
  });

  document.querySelectorAll(".index a").forEach(link => {
    link.addEventListener("click", () => {
      const target = document.querySelector(link.getAttribute("href"));
      if (target) { target.open = true; render(target); }
    });
  });
}

if (window.hljs) setup();
else window.addEventListener("load", setup, { once: true });
"""


def json_for_script(payload: dict) -> str:
    text = json.dumps(payload, ensure_ascii=False)
    return text.replace("</", "<\\/").replace("<!--", "<\\!--")


def counts_html(file: dict) -> str:
    parts = []
    if file["additions"]:
        parts.append(f'<span class="add-count">+{file["additions"]}</span>')
    if file["deletions"]:
        parts.append(f'<span class="del-count">−{file["deletions"]}</span>')
    return " ".join(parts)


def file_section(index: int, file: dict) -> str:
    anchor = f"file-{index}"
    name = html.escape(file["path"])
    if file["oldPath"]:
        name = f'<span class="from">{html.escape(file["oldPath"])} → </span>{name}'
    badge_class = file["statusLabel"].replace(" ", "-")
    open_attr = " open" if file["changed"] else ""
    inner = (
        f'<div class="note">{html.escape(file["note"])}</div>'
        if file.get("note")
        else '<div class="slot"><div class="pending">Loading…</div></div>'
    )
    return (
        f'<details class="file" id="{anchor}" data-index="{index}" '
        f'data-path="{html.escape(file["path"], quote=True)}"{open_attr}>'
        f'<summary><span class="badge {badge_class}">{file["statusLabel"]}</span>'
        f'<span class="name">{name}</span>'
        f'<span class="counts">{counts_html(file)}</span></summary>'
        f"{inner}</details>"
    )


def index_list(files: list[dict]) -> str:
    items = []
    for index, file in enumerate(files):
        if not file["changed"]:
            continue
        label = file["path"]
        if file["oldPath"]:
            label = f'{file["oldPath"]} → {file["path"]}'
        items.append(
            f'<li data-path="{html.escape(file["path"], quote=True)}">'
            f'<span class="badge {file["statusLabel"].replace(" ", "-")}">{file["statusLabel"]}</span>'
            f'<a class="path" href="#file-{index}">{html.escape(label)}</a>'
            f'<span class="counts">{counts_html(file)}</span></li>'
        )
    if not items:
        return ""
    return (
        f'<details class="index" open><summary>{len(items)} changed '
        f'file{"" if len(items) == 1 else "s"}</summary><ol>{"".join(items)}</ol></details>'
    )


def rev_card(label: str, info: dict) -> str:
    # Echo what the user typed ("HEAD~2", a branch name) unless it was the sha itself.
    given = "" if info["worktree"] or info["sha"].startswith(info["rev"]) else info["rev"]
    return (
        f'<div class="rev"><div class="label">{label}</div>'
        f'<div><span class="sha">{html.escape(info["short"])}</span> '
        f'<span class="subject">{html.escape(given)}</span></div>'
        f'<div class="subject">{html.escape(info["subject"])}</div>'
        f'<div class="subject">{html.escape(" · ".join(filter(None, [info["author"], info["date"]])))}'
        f'</div></div>'
    )


def worktree_note(base: dict, head: dict) -> str:
    if not (base["worktree"] or head["worktree"]):
        return ""
    return ('<p class="summary">The working folder side covers staged and unstaged '
            'work alike, and counts files Git does not track yet as additions. '
            'Gitignored files are left out.</p>')


def build_page(repo: Path, base: dict, head: dict, files: list[dict]) -> str:
    changed = [f for f in files if f["changed"]]
    unchanged = [f for f in files if not f["changed"]]
    additions = sum(f["additions"] for f in changed)
    deletions = sum(f["deletions"] for f in changed)
    title = f"{repo.name}: {base['short']}...{head['short']}"

    payload = {
        "files": [
            {k: f[k] for k in ("path", "oldPath", "status", "language", "changed",
                               "old", "new", "rows")}
            for f in files
        ]
    }
    script = (SCRIPT
              .replace("__FOLD_THRESHOLD__", str(FOLD_THRESHOLD))
              .replace("__CONTEXT_LINES__", str(CONTEXT_LINES)))

    sections = [file_section(index, file) for index, file in enumerate(files)
                if file["changed"]]
    unchanged_sections = [file_section(index, file) for index, file in enumerate(files)
                          if not file["changed"]]
    unchanged_block = ""
    if unchanged_sections:
        unchanged_block = (
            f'<h2 style="font-size:16px;margin:28px 0 12px">'
            f'{len(unchanged)} unchanged file{"" if len(unchanged) == 1 else "s"}</h2>'
            + "".join(unchanged_sections)
        )

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title>
<style>{STYLE}</style>
</head>
<body>
<div class="wrap">
<header class="page">
  <h1>Comparing {html.escape(base['short'])}…{html.escape(head['short'])}
      in <code>{html.escape(repo.name)}</code></h1>
  <div class="revs">{rev_card('Base (before)', base)}{rev_card('Compare (after)', head)}</div>
  <p class="summary"><strong>{len(changed)}</strong> changed
     file{'' if len(changed) == 1 else 's'} with
     <span class="add-count">{additions} addition{'' if additions == 1 else 's'}</span> and
     <span class="del-count">{deletions} deletion{'' if deletions == 1 else 's'}</span>.</p>
  {worktree_note(base, head)}
</header>
<div class="toolbar">
  <input id="filter" type="search" placeholder="Filter files by path…" autocomplete="off">
  <button id="expand-all" type="button">Expand all</button>
  <button id="collapse-all" type="button">Collapse all</button>
</div>
{index_list(files)}
{''.join(sections) or '<div class="note">No differences between these two points.</div>'}
{unchanged_block}
</div>
<script>window.DIFF_DATA = {json_for_script(payload)};</script>
<script src="{HLJS_URL}"></script>
<script>{script}</script>
</body>
</html>
"""


# --- entry point -------------------------------------------------------------


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(
        description="Generate a GitHub-style split-view diff page for a local Git repo.")
    parser.add_argument("repo", help="path to the Git repository")
    parser.add_argument("base", help="the 'before' point: a commit, tag, branch, "
                                     "HEAD~2, ... or WORKTREE for the working folder")
    parser.add_argument("head", help="the 'after' point, same forms as base")
    parser.add_argument("output", nargs="?", help="output HTML file "
                                                  "(default: _temp/compare-<base>-<head>.html)")
    parser.add_argument("--no-unchanged", action="store_true",
                        help="omit the collapsed panels for files that did not change")
    parser.add_argument("--open", action="store_true", help="open the page in a browser when done")
    args = parser.parse_args(argv)

    repo = Path(args.repo).expanduser().resolve()
    if not (repo / ".git").exists():
        try:
            git_text(repo, "rev-parse", "--git-dir")
        except (GitError, FileNotFoundError):
            print(f"error: {repo} is not a Git repository", file=sys.stderr)
            return 1

    try:
        base = resolve_point(repo, args.base)
        head = resolve_point(repo, args.head)
        if base["worktree"] and head["worktree"]:
            raise GitError("both comparison points are the working folder")
        files = collect_changed_files(repo, base, head)
        if not args.no_unchanged:
            changed_paths = {f["path"] for f in files}
            files += collect_unchanged_files(repo, head, changed_paths)
    except GitError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    output = Path(args.output).expanduser() if args.output else Path(
        "_temp") / f"compare-{base['slug']}-{head['slug']}.html"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(build_page(repo, base, head, files), encoding="utf-8")

    changed_count = sum(1 for f in files if f["changed"])
    size_kb = output.stat().st_size / 1024
    print(f"{changed_count} changed file(s), {len(files) - changed_count} unchanged")
    print(f"wrote {output} ({size_kb:,.0f} KB)")
    if args.open:
        webbrowser.open(output.resolve().as_uri())
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
