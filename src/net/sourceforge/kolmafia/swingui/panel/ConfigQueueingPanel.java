package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import net.sourceforge.kolmafia.KoLGUIConstants;

public class ConfigQueueingPanel extends JPanel {
  private List<Component> componentQueue = new ArrayList<>();

  public ConfigQueueingPanel() {
    // 5 px inset
    this.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
    // box layoutmanager
    this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
  }

  protected JSeparator newSeparator() {
    JSeparator sep = new JSeparator();
    // again, JSeparators have unbounded max size, which messes with boxlayout.  Fix it.
    int width = sep.getMaximumSize().width;
    int height = sep.getPreferredSize().height;
    Dimension size = new Dimension(width, height);
    sep.setMaximumSize(size);
    return sep;
  }

  protected JSeparator newSeparator(JLabel label) {
    JSeparator sep = new JSeparator();
    // again, JSeparators have unbounded max size, which messes with boxlayout.  Fix it.
    int width = label.getFontMetrics(label.getFont()).stringWidth(label.getText());
    int height = sep.getPreferredSize().height;
    Dimension size = new Dimension(width, height);
    sep.setMaximumSize(size);
    return sep;
  }

  protected JTextArea newTextArea(String content) {
    JTextArea message =
        new JTextArea(content) {
          // don't let boxlayout expand the JTextArea ridiculously
          @Override
          public Dimension getMaximumSize() {
            return this.getPreferredSize();
          }
        };

    message.setColumns(40);
    message.setLineWrap(true);
    message.setWrapStyleWord(true);
    message.setEditable(false);
    message.setOpaque(false);
    message.setFont(KoLGUIConstants.DEFAULT_FONT);

    return message;
  }

  public <T extends Component> T queue(T comp) {
    this.componentQueue.add(comp);
    return comp;
  }

  public void makeLayout() {
    for (Component comp : this.componentQueue) {
      if (comp instanceof JComponent jcomp) {
        jcomp.setAlignmentX(LEFT_ALIGNMENT);
      }
      this.add(comp);
    }
    this.componentQueue = null;
  }
}
