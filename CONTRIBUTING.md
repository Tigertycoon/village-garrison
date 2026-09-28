# Contributing

Use Java 21 and the committed Gradle wrapper. Run `./gradlew clean build` and
`python3 scripts/check_resources.py` before sending a change. On Windows use
`gradlew.bat` and `python` instead.

Describe the Minecraft, NeoForge and dependency versions in bug reports, with
steps to reproduce. Remove addresses, tokens and player information from logs.
Keep changes focused; include a regression test when fixing gameplay state or
inventory handling. Do not commit mod dependencies, game worlds or runtime logs.
