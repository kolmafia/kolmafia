# Keeping the engine separate from the UI

KoLmafia's engine (requests, parsing, session state, scripts, the CLI) doesn't use Swing, so it can
run headless, in tests, or behind another frontend.

## The rule

Only `net.sourceforge.kolmafia.swingui` may use Swing, AWT or the Swing-only libraries. Drawing on
in-memory images (`java.awt.Graphics`, `java.awt.image`,
`javax.imageio`) is fine anywhere.

Checkstyle enforces this on imports (`config/checkstyle/import-control.xml`). The one exception is
`KoLmafia.main` installing `SwingUserInterface`.

## When the engine needs the UI

Call a method on `UserInterfaceManager.get()`. `UserInterface` lists everything the engine asks of a
UI: prompts, windows, display state, change notifications and the UI thread (`invokeLater`). Each
method has a default that works without a display: prompts ask on the console and the rest do nothing.
`SwingUserInterface` implements them for the desktop.

To add one, give it a headless default and implement it in `SwingUserInterface`. Name it after what
happened in the engine (`meatChanged`), not after the window that shows it. If a listener registry
(`NamedListenerRegistry`, `PreferenceListenerRegistry`) already carries the event, use that instead.

## Keep Swing types out of engine data

- Colors are `"#rrggbb"` strings and images are file paths.
- Lists are `LockableListModel`; `SwingListModel.of(list)` shows one in a Swing component.
- State lives in the engine, not in a widget. A widget writes its value to the engine.

## Testing

Engine tests run headless. To check what the engine asks of the UI, install a recording
`UserInterface` with `Player.withUserInterface`.
