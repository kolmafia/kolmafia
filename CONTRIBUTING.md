# Contributing to KoLmafia

First off, thank you for contributing your time and knowledge. This
project would not exist if not for volunteers like you!

## Some useful prerequisites (lifted from Github docs)

- [Set up Git](https://docs.github.com/en/get-started/quickstart/set-up-git)
- [GitHub flow](https://docs.github.com/en/get-started/quickstart/github-flow)
- [Collaborating with pull requests](https://docs.github.com/en/github/collaborating-with-pull-requests)

All work must be done in feature branches (typically within your fork
of the project), and reviewed by at least one of the core developers
through Github's Pull Request feature.

We strongly encourage that you provide tests as part of your pull
request, as this helps us understand the problem at hand and avoid
breaking your change in the future.

## Guidelines (adapted from [our previous guide](https://kolmafia.us/threads/25364))

(Editor's note: you can mentally replace all instances of "patch" with
"pull request".)

A successful submit requires social and technical skills.

The set of social skills includes the ability to get along with devs
at the KoLmafia.us community. Users that have managed to alienate
themselves from members of the dev team tend to have their proposed
patches ignored.

The ability to determine the appropriateness of a patch and the level
of support for it is extremely useful. A patch that violates the
spirit of KoLmafia (for example, that allows access to non-KoLmafia
directories in the user's file system) will be rejected. A patch that
changes an existing feature would need some discussion. A patch that
changes a lot of code to implement a feature of at best niche interest
is unlikely to be accepted.

The technical skills are the ability to write and test Java code that
works in the context of KoLmafia. A prerequisite for success is the
ability to read and understand KoLmafia's code and infer its
archetecture since those are not well documented in the traditional
sense.

Individuals with a good track record of creating and submitting
patches can ask to be made a dev. The active devs who can do this are
MCroft and gausie.

## Project-specific details

### Gradle

We use Gradle for managing dependencies and defining more complex
workflows. To provide consistency across build environments, we
provide a gradle wrapper (gradlew) with the repository that can be run
directly.

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

### Code style

`./gradlew spotlessCheck` is enforced for all pull requests. We base
our formatting on Google Java Style, for ease of integration with
existing tooling. The output of `./gradlew spotlessApply` should be
considered the source of truth.

As we move to newer Java versions, we may add new guidelines regarding
language and library changes. At that point, we may move this section
into a standalone document.
