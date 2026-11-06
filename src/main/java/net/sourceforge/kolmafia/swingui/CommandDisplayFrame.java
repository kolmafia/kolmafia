package net.sourceforge.kolmafia.swingui;

import javax.swing.JTabbedPane;
import net.sourceforge.kolmafia.session.CommandQueueManager;
import net.sourceforge.kolmafia.swingui.panel.CommandDisplayPanel;

public class CommandDisplayFrame extends GenericFrame {
  public CommandDisplayFrame() {
    super("Graphical CLI");

    this.setCenterComponent(new CommandDisplayPanel("commandBufferGCLI"));
  }

  @Override
  public JTabbedPane getTabbedPane() {
    return null;
  }

  @Override
  public boolean shouldAddStatusBar() {
    return false;
  }

  @Override
  public boolean useSidePane() {
    return true;
  }

  public static boolean hasQueuedCommands() {
    return CommandQueueManager.hasQueuedCommands();
  }

  public static void executeCommand(final String command) {
    CommandQueueManager.executeCommand(command);
  }
}
