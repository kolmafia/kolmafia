# Contributing to KoLmafia

First off, thank you for contributing your time and knowledge. This project
would not exist if not for volunteers like you!

## Workflow Overview

Some suggested reading (lifted from [Github docs](https://github.com/github/docs/blob/main/CONTRIBUTING.md)):

- [Set up Git](https://docs.github.com/en/get-started/quickstart/set-up-git)
- [GitHub flow](https://docs.github.com/en/get-started/quickstart/github-flow)
- [Collaborating with pull requests](https://docs.github.com/en/github/collaborating-with-pull-requests)

In the spirit of Github flow, all work must be done in feature branches
(typically within your fork of the project), and reviewed by at least one of the
core developers through Github's Pull Request (PR) feature.

We strongly encourage that you provide tests as part of your pull request, as
this helps us understand the problem at hand and avoid breaking your change in
the future.

## Guidelines (adapted from [our previous guide](https://kolmafia.us/threads/25364))

Before creating a PR, we strongly recommend creating a bug in the [KoLmafia
forums](https://kolmafia.us/forums/bug-reports.24/), in order to provide broader
context regarding the change, and to discuss whether the change is a good
idea. We'd hate for you to waste effort and be discouraged from submitting
future PRs because some fundamental concerns arise during code review.

The ability to determine the appropriateness of a patch and the level of support
for it is extremely useful. A patch that violates the spirit of KoLmafia (for
example, that allows access to non-KoLmafia directories in the user's file
system) will be rejected. A patch that changes an existing feature will need
some discussion. A patch that changes a lot of code to implement a feature of at
best niche interest is unlikely to be accepted.

The technical skills are the ability to write and test Java code that works in
the context of KoLmafia. A prerequisite for success is the ability to read and
understand KoLmafia's code and infer its architecture since those are not well
documented in the traditional sense.

Individuals with a good track record of creating PRs and constructively engaging
in other pull requests can ask to be made a dev. The active devs who can do this
are MCroft and gausie. We may also reach out to you.

## Project-specific details

### Gradle

We use Gradle for managing dependencies and defining more complex workflows. To
provide consistency across build environments, we provide a gradle wrapper
(gradlew) with the repository that can be run directly.

Some tasks of interest:

 task             | description
:----------------:|----------------------------------------------------
 tasks            | List all Gradle tasks.
 runShadow        | Build and run the application.
 shadowJar        | Create a new jar in dist/.
 spotlessApply    | Apply formatting changes.
 check            | Run all checks, including formatting and tests.
 jacocoTestReport | Generate a coverage report in build/reports/jacoco.

We currently require at least Java 9, but strongly recommend building
and running via the latest LTS release (Java 17 as of October 2021).

See [this thread](https://kolmafia.us/threads/rocking-the-gradle.26583/) for
some additional notes on Gradle usage.

### Testing

We use [JUnit 5](https://junit.org/junit5/docs/current/user-guide/) for all of
our tests. If you wish to stub out complex behavior, consider using
[Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html).

In general, the test/ directory structure mirrors that of src/, with
test/path/to/FooTest.java corresponding to src/path/to/Foo.java.

#### Script tests

We provide CustomScriptTest.java if you wish to write ASH / CLI / JS scripts in
test/root/scripts. This wrapper compares the output of these scripts against
golden files in test/root/expected. Tests of this form must clean up after
themselves, to avoid polluting the shared environment.

See [this thread](https://kolmafia.us/threads/scripts-in-tests.26380/) for a
discussion on setting up the environment for these custom script tests.

### Code style

`./gradlew spotlessCheck` is enforced for all pull requests. We base our
formatting on Google Java Style, for ease of integration with existing
tooling. The output of `./gradlew spotlessApply` should be considered the source
of truth.

As we move to newer Java versions, we may add new guidelines regarding language
and library changes. At that point, we may move this section into a standalone
document.
