# Keeping the engine separate from the UI

KoLmafia's game logic (requests, parsing, session state, ASH/JS, the CLI) runs without Swing or
AWT, so it can be driven by the desktop GUI, the CLI, the relay browser, tests on a headless CI
machine, or another frontend (a browser-based UI, a mobile app). This page sets the rules for
new code.

## Terms

- **Engine:** every class under `net.sourceforge.kolmafia` except `swingui` and its subpackages.
  `textui` and `webui` are engine packages.
- **UI contract:** `net.sourceforge.kolmafia.ui`: the `UserInterface` interface, which lists
  everything the engine asks of a user interface, and `UserInterfaceManager`, which holds the one in
  use.
- **Swing UI:** `swingui.SwingUserInterface`, the desktop implementation.

## Rules

1. **The engine never uses `javax.swing`, `java.awt` or `swingui`**, nor the Swing-only libraries
   (FlatLaf, SwingX, JGoodies, ...) or the Swing widgets in `net.java.dev.spellcast.utilities`
   (`ActionPanel`, `JComponentUtilities`, ...). Drawing on in-memory images (`java.awt.Graphics`,
   `java.awt.image`, `javax.imageio`) needs no display and is allowed. Checkstyle's `ImportControl`
   (`config/checkstyle/import-control.xml`) enforces this for imports. Its one exception, in
   `config/checkstyle/suppressions.xml`, is `KoLmafia.main` installing `SwingUserInterface`; don't add
   others. Fully qualified names are not allowed either.
2. **The UI may depend on the engine; the engine never depends on the UI.**
3. **When the engine needs the user, or needs to tell the user something, it calls
   `UserInterfaceManager.get()`.** Prompts, windows, display state, refresh notifications
   (`meatChanged`, `familiarsChanged`, ...), the UI thread (`isEventDispatchThread`, `invokeLater`) and the like are
   methods of `UserInterface`. Each has a default that works with no display: prompts ask on the console,
   the way KoLmafia always has when running headless, and everything else does nothing. A frontend
   overrides what it supports. Add a method when the engine needs something new; name it after what
   happened in the engine, not after the frame that shows it. Where an existing listener registry
   (`NamedListenerRegistry`, `PreferenceListenerRegistry`, ...) already carries the event, use it.
4. **`KoLmafia.main` installs the Swing UI** with `UserInterfaceManager.set(new SwingUserInterface())`
   unless KoLmafia runs headless. Other frontends install their own the same way. Tests use
   `Player.withUserInterface` (null restores the default).
5. **Behaviour and threads stay as they were.** A `UserInterface` method runs on the caller's thread and
   does exactly what the engine did before; work the engine handed to the Swing thread goes through
   `invokeLater`.
6. **State belongs to the engine, never to a widget.** If engine code needs a value a component holds (for
   example the maximizer expression), the value lives in an engine class and the component writes it there.
7. **Logic lives where the engine's other logic for that feature lives:** `request/` (`XxxRequest`),
   `session/` (`XxxManager`), `persistence/` (`XxxDatabase`), `textui/command/` (`XxxCommand`).
   `swingui/` holds frames, panels and widgets, which call the engine.
8. **Engine data uses plain types.** Colors are `"#rrggbb"` strings; images are paths; trees and lists use
   engine or `java.util` types. Converting them to Swing types happens in `swingui`.
9. **Lists:** engine lists are `LockableListModel`/`SortedListModel`, which know nothing of Swing.
   To show one in a `JList`, `JComboBox` or table, use `SwingListModel.of(list)`: one Swing model per list,
   sharing the list's selection. `SwingListModel.listOf(model)` goes back from a Swing model to the list.
10. **Engine tests run headless.** UI behaviour is tested by installing a recording `UserInterface`, or in
    the `swingui` tests.

## Moving code

Each change moves one piece and changes no behaviour.

1. **Characterize first:** add or extend tests that pin the current behaviour (JUnit 5, Hamcrest
   `assertThat`, `internal.helpers`). Add tests for code paths that had none.
2. **Move the code unchanged** to the engine class chosen by rule 7. Leave the Swing class as a thin
   caller; keep its public methods as delegates when scripts or tests use them.
3. **Run `./gradlew spotlessApply` and `./gradlew check`.**
4. **Title the commit `refactor: <what moved>`.**
