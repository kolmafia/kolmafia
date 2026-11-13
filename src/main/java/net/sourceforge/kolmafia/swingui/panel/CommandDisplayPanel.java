package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.CommandHistory;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;
import net.sourceforge.kolmafia.swingui.listener.HyperlinkAdapter;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.FindBar;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;

public class CommandDisplayPanel extends JPanel implements FocusListener {
  private final AutoHighlightTextField entryField;
  private final JButton entryButton;

  private int commandIndex = -1;
  private String draft = "";

  public CommandDisplayPanel() {
    RequestPane outputDisplay = new RequestPane();
    outputDisplay.addHyperlinkListener(new HyperlinkAdapter());

    JScrollPane scrollPane = KoLConstants.commandBuffer.addDisplay(outputDisplay);
    JComponentUtilities.setComponentSize(scrollPane, 400, 300);

    JPanel entryPanel = new JPanel(new BorderLayout());
    this.entryField = new AutoHighlightTextField();
    this.entryField.addKeyListener(new CommandEntryListener());

    var findBar = new FindBar(outputDisplay, this.entryField::requestFocusInWindow);
    var outputPanel = new JPanel(new BorderLayout());
    outputPanel.add(findBar, BorderLayout.NORTH);
    outputPanel.add(scrollPane, BorderLayout.CENTER);

    this.entryButton = new JButton("exec");
    this.entryButton.addActionListener(new CommandEntryListener());

    entryPanel.add(this.entryField, BorderLayout.CENTER);
    entryPanel.add(this.entryButton, BorderLayout.EAST);

    this.setLayout(new BorderLayout(1, 1));
    this.add(outputPanel, BorderLayout.CENTER);
    this.add(entryPanel, BorderLayout.SOUTH);

    findBar.installShortcut(this);

    this.setFocusCycleRoot(true);
    this.setFocusTraversalPolicy(new DefaultComponentFocusTraversalPolicy(this.entryField));

    this.addFocusListener(this);
  }

  @Override
  public void focusGained(FocusEvent e) {
    this.entryField.requestFocus();
  }

  @Override
  public void focusLost(FocusEvent e) {}

  private class CommandEntryListener extends ThreadedListener {
    @Override
    protected boolean isValidKeyCode(int keyCode) {
      return keyCode == KeyEvent.VK_UP
          || keyCode == KeyEvent.VK_DOWN
          || keyCode == KeyEvent.VK_ENTER;
    }

    @Override
    protected void execute() {
      if (this.isAction()) {
        this.submitCommand();
        return;
      }

      int keyCode = getKeyCode();

      if (keyCode == KeyEvent.VK_UP) {
        var history = CommandHistory.get();
        if (CommandDisplayPanel.this.commandIndex + 1 >= history.size()) {
          return;
        }

        if (CommandDisplayPanel.this.commandIndex == -1) {
          CommandDisplayPanel.this.draft = CommandDisplayPanel.this.entryField.getText();
        }
        CommandDisplayPanel.this.commandIndex++;
        CommandDisplayPanel.this.entryField.setText(
            history.get(CommandDisplayPanel.this.commandIndex));
      } else if (keyCode == KeyEvent.VK_DOWN) {
        var history = CommandHistory.get();
        CommandDisplayPanel.this.commandIndex =
            Math.min(CommandDisplayPanel.this.commandIndex, history.size() - 1);
        if (CommandDisplayPanel.this.commandIndex < 0) {
          return;
        }

        CommandDisplayPanel.this.commandIndex--;
        CommandDisplayPanel.this.entryField.setText(
            CommandDisplayPanel.this.commandIndex == -1
                ? CommandDisplayPanel.this.draft
                : history.get(CommandDisplayPanel.this.commandIndex));
      } else if (keyCode == KeyEvent.VK_ENTER) {
        this.submitCommand();
      }
    }

    /**
     * Handler for keyReleased events which will end up running this object's execute() method.
     *
     * <p>This implementation is identical to ThreadedListener.keyReleased (which this overrides),
     * except it runs on the Event Dispatch Thread. This is vital for command entry since
     * dispatching a parallel thread will create a race condition.
     *
     * @see
     *     net.sourceforge.kolmafia.swingui.listener.ThreadedListener#keyReleased(java.awt.event.KeyEvent)
     */
    @Override
    public void keyReleased(final KeyEvent e) {
      if (e.isConsumed()) {
        return;
      }

      if (!this.isValidKeyCode(e.getKeyCode())) {
        return;
      }

      this.keyEvent = e;
      try {
        this.run();
      } catch (Exception e1) {
        StaticEntity.printStackTrace(e1);
      }

      e.consume();
    }

    private void submitCommand() {
      String command = CommandDisplayPanel.this.entryField.getText().trim();
      CommandDisplayPanel.this.entryField.setText("");

      CommandHistory.add(command);
      CommandDisplayPanel.this.commandIndex = -1;
      CommandDisplayFrame.executeCommand(command);
    }
  }
}
