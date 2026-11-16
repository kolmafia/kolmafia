package net.sourceforge.kolmafia.swingui;

import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.session.UserPrompt;
import net.sourceforge.kolmafia.ui.UserInterface;

public class SwingUserInterface implements UserInterface {
  @Override
  public <R> void showPrompt(final UserPrompt<R> prompt) {
    UserPromptDialog.show(prompt);
  }

  @Override
  public void showBalloon(final String message, final boolean onlyShowWhenHidden) {
    SystemTrayFrame.showBalloon(message, onlyShowWhenHidden);
  }

  @Override
  public boolean isDarkTheme() {
    return KoLmafiaGUI.isDarkTheme();
  }

  @Override
  public boolean isEventDispatchThread() {
    return SwingUtilities.isEventDispatchThread();
  }
}
