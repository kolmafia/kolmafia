# Keeping the engine separate from the UI

KoLmafia's game logic (requests, parsing, session state, ASH/JS, the CLI) should run without
Swing or AWT. Then it can be driven by the desktop GUI, the CLI/TUI, the relay browser, tests on a
headless CI machine, or any other frontend. This page sets the rules for new code and describes how
existing code is moved over, one small PR at a time.

## Terms

- **Engine:** every class under `net.sourceforge.kolmafia` except the UI packages below.
- **UI packages:** `swingui` (desktop GUI) and everything under it. `textui` and `webui` contain
  engine code and stay engine packages; only their Swing usage (if any) is moved out.
- **UI contract:** the small set of engine-side interfaces in `net.sourceforge.kolmafia.ui` that a
  frontend implements. The package holds interfaces and headless defaults only: no Swing, no AWT.

## Rules

1. **The engine never imports `javax.swing.*`, `java.awt.*` or `net.sourceforge.kolmafia.swingui.*`.**
   This is enforced by checkstyle `ImportControl` (`config/checkstyle/import-control.xml`). Classes that
   have not been migrated yet are listed in `config/checkstyle/suppressions.xml`. The list only ever
   shrinks: new entries are not accepted. The check only sees `import` statements, so fully qualified
   names (`javax.swing.SwingUtilities.invokeLater(...)`) are not allowed in the engine either.
2. **The UI may depend on the engine; the engine never depends on the UI.** Dependencies point one way.
3. **When the engine's state changes, it fires an event and does not call into the UI.** Use the
   existing registries in `net.sourceforge.kolmafia.listener`:
   - `NamedListenerRegistry.fireChange("(name)")` for named events, with lowercase keys in parentheses
     as today (`"(coinmaster)"`, `"(outfit)"`);
   - `PreferenceListenerRegistry` when the state is a preference;
   - `CharacterListenerRegistry` for character updates.
   A frame registers a `Listener` and refreshes itself. Example: instead of `KoLCharacter` calling
   `MallSearchFrame.updateMeat()`, it fires `"(meat)"` and `MallSearchFrame` listens for it.
4. **When the engine needs something from the user, it asks through the UI contract.** Alerts, confirmations,
   text input, choosing from a list, showing a window, and running work on the UI thread all go through
   interfaces in `net.sourceforge.kolmafia.ui`. The engine installs a headless default (today's console
   behaviour, e.g. the `StaticEntity.isHeadless()` branches of `InputFieldUtilities`); `KoLmafiaGUI`
   installs the Swing implementation at startup. Windows are named by the existing `KoLConstants.FRAME_NAMES`
   ids (the same ids the `window` command uses), never by `Class` objects from `swingui`.
5. **State belongs to the engine, never to a widget.** If engine code reads a value from a component
   (for example the maximizer expression in `MaximizerFrame.expressionSelect`), the value moves to an
   engine class with a getter/setter, and the component reflects it.
6. **Logic lives where the engine's other logic for that feature lives.** Follow the existing layout:
   - `request/`: talking to KoL (`XxxRequest`);
   - `session/`: state and workflows (`XxxManager`);
   - `persistence/`: data files (`XxxDatabase`);
   - `textui/command/`: CLI commands (`XxxCommand`);
   - `swingui/`: frames and panels only (`XxxFrame`, `XxxPanel`), which call the engine class.
   There are no parallel `model`/`viewmodel` trees and no new naming suffixes.
7. **Engine data uses plain types.** Colours are strings (`"#rrggbb"` or a colour name); images are
   paths or URLs; checkboxes become `boolean`s; trees and lists use `java.util` types. Converting
   them to Swing types happens in `swingui`.
8. **Engine tests run headless.** Tests of engine classes must not need a display or a
   `@DisabledIf("java.awt.GraphicsEnvironment#isHeadless")` guard. UI behaviour is tested through
   the UI contract, with a recording fake installed via `Cleanups`, or in the existing `swingui` tests.

## Moving existing code

Each PR moves **one** link (one engine class's use of the UI, or one feature) and changes no behaviour.

1. **Characterize first.** Before moving anything, add or extend tests that pin the current behaviour
   (JUnit 5, Hamcrest `assertThat`, `internal.helpers` such as `Player`, `Cleanups`,
   `FakeHttpClientBuilder`). They must pass on `main` before the move.
2. **Move.** Put the logic in the engine class chosen by rule 6. Leave the frame as a thin caller.
   Keep public signatures that scripts or other code use, delegating to the new location where needed.
3. **Remove the suppression.** Delete the class's entry from `suppressions.xml` in the same PR.
4. **Verify.** Run `./gradlew spotlessApply` and `./gradlew check`. The characterization tests pass
   unchanged (moved alongside the code, mirroring its new package).
5. **Title** the PR `refactor: <what moved>` and link this page.

## What goes where

| Today in the engine | Becomes |
| --- | --- |
| `SomeFrame.refresh()` / `SomeFrame.INSTANCE…` | `NamedListenerRegistry.fireChange("(name)")`; the frame listens |
| `GenericFrame.createDisplay(SomeFrame.class)` | open the window by its `FRAME_NAMES` id through the UI contract |
| `GenericFrame.instanceExists()`, `appearsInTab(…)` | query through the UI contract (headless: `false`) |
| `InputFieldUtilities.alert/confirm/input` | prompt through the UI contract (headless: console prompt, as today) |
| `SwingUtilities.invokeLater(…)` | run on the UI thread through the UI contract (headless: run now) |
| `java.awt.Color`, `ImageIcon`, `JCheckBox`, Swing tree nodes in data classes | plain types (rule 7) |
| Swing-only classes in engine packages (editor kits, renderers, GUI bootstrap) | moved into `swingui` unchanged |
| `LockableListModel` / `SortedListModel` | later: a plain observable list in the engine, wrapped by a Swing `ListModel` adapter in `swingui` |
