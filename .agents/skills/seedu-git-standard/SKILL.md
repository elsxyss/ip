---
name: seedu-git-standard
description: Apply the project-mandated SE-EDU Git conventions when proposing or creating commits, commit messages, or branches in this repository.
---

# SE-EDU Git Standard

Use the [SE-EDU Git conventions](https://se-education.org/guides/conventions/git.html) for every future commit and
branch in this project. Do not commit, push, or create a branch unless the user authorizes that action.

## Commit subject

- Write a meaningful subject in the imperative mood.
- Capitalize its first letter and do not end it with a period.
- Aim for at most 50 characters; 72 characters is the hard limit.
- Add an optional `<scope>:` or `<category>:` prefix only when it clarifies the change.

## Commit body

- Give every non-trivial commit a body, separated from the subject by one blank line.
- Wrap body text at 72 characters and separate paragraphs with blank lines. Use bullet points when they improve
  clarity.
- Explain what changed and why; leave implementation mechanics to the diff. Provide enough context for a reader to
  judge the change without opening the diff, while avoiding repetition of code comments.
- Describe the existing situation in the present tense, explain why it needs to change, describe the change in the
  imperative mood, and record the rationale or other relevant context.
- Split the work into finer-grained commits when the message becomes too long or combines unrelated purposes.

## Branch names

- Use a meaningful kebab-case name made from relevant keywords, such as `refactor-ui-tests`.
- For work tied to an issue, use `issueNumber-keywords-from-title`, such as `1234-ui-freeze-error`.

Before proposing or creating a commit, review the actual diff so the message describes only that commit's contents.
