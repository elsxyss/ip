# Project context

This repository is a starter template for a greenfield Java project used in an introductory software engineering course in an undergraduate computer science program. Students use it as the starting point for their own projects.

# Default user context

Unless the user says otherwise, assume that you are assisting a student working on a project in this repository. If the user identifies themselves as an instructor or another project stakeholder, adapt your response to that role.

# Student profile

* Prior knowledge: Basic Java and OOP concepts.
* Level of programming experience: Intermediate
* IDE and level of expertise: IntelliJ IDEA, Intermediate

# Guidance for interacting with users

* Explain the rationale for significant actions: what you did and why.
* Keep explanations brief but instructive, supporting learning through responsible use of AI. For example:

  * When suggesting a Git command, briefly explain what it does.
  * Add explanatory Javadoc comments to all classes and to nontrivial methods and fields when their purpose or behavior is not obvious.
  * Make generated code as self-explanatory as possible, and include explanatory comments where they improve understanding.
  * When faced with a design choice, choose the simplest option that is sufficient for the requirements, while briefly explaining relevant more advanced alternatives.

# Project-specific requirements

## Java coding standards

Before creating, modifying, or reviewing any Java code, load and follow the
project-specific `seedu-java-coding-standard` skill at
`.agents/skills/seedu-java-coding-standard/SKILL.md`. All Java code in this
repository must conform to that skill. If another repository rule is stricter,
follow the stricter rule.

## Java version:

Ensure that Java 25 is used when running the application or build tasks. On macOS, use `sdk use java 25.0.3.fx-zulu` to switch to Java 25 if needed.

## Test coverage

Maintain JUnit test coverage of at least 50%, prioritizing complex, core, and critical
business logic. Update the relevant JUnit tests after every code change so the coverage
target continues to be met and changed behavior is protected against regressions.

## UI regression testing

After every code change:

1. Review `test/ui-test-plan.md` and update it if the change adds or alters a UI test
   scenario, its inputs, its expected output, or relevant test configuration.
2. Invoke the project-specific `test-ui` skill at `.agents/skills/test-ui/SKILL.md` and
   run the relevant UI test cases. Follow the skill's fail-fast and console-session
   reporting requirements. If the affected cases cannot be identified confidently, run
   the full UI test plan.

## Git

Before proposing or creating any commit, commit message, or branch, load and
follow the project-specific `seedu-git-standard` skill at
`.agents/skills/seedu-git-standard/SKILL.md`. All future commits and branches
must conform to that skill.

Use lightweight tags unless the user requests an annotated tag.
When proposing or creating a commit message, include enough detail to explain the rationale for the change.
Do not commit or push unless explicitly asked.
