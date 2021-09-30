package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import net.sourceforge.kolmafia.KoLGUIConstants;
import org.jdesktop.swingx.JXCollapsiblePane;

/*
A replacement for JTextArea that places a small expand/collapse icon to the left.

Does not extend JTextComponent, so some finagling does have to be done to get it to drop
in to some installations.
*/

public class CollapsibleTextArea extends JXCollapsiblePane {
  private final JLabel label;
  private final JTextArea area;

  public CollapsibleTextArea(String label) {
    this.label = new JLabel(label);

    this.area = new JTextArea(1, 10);
    area.setLineWrap(true);
    area.setFont(KoLGUIConstants.DEFAULT_FONT);
    area.setBorder(BorderFactory.createLineBorder(Color.black));
    area.setMinimumSize(area.getPreferredSize());

    this.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();

    Action act = this.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION);

    JButton collapser = new JButton(act);
    collapser.setText("");
    collapser.setContentAreaFilled(false);
    collapser.setBorderPainted(false);
    collapser.setFocusPainted(false);
    collapser.setPreferredSize(new Dimension(12, 12));
    collapser.setMinimumSize(new Dimension(12, 12));
    collapser.setMaximumSize(new Dimension(12, 12));

    act.putValue(JXCollapsiblePane.COLLAPSE_ICON, UIManager.getIcon("Tree.expandedIcon"));
    act.putValue(JXCollapsiblePane.EXPAND_ICON, UIManager.getIcon("Tree.collapsedIcon"));

    c.anchor = GridBagConstraints.WEST;
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0;
    c.fill = GridBagConstraints.NONE;
    this.add(collapser, c);

    c.anchor = GridBagConstraints.EAST;
    c.gridx = 1;
    c.gridy = 0;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    this.add(area, c);

    this.setMinimumSize(new Dimension(1, this.getPreferredSize().height));
  }

  public CollapsibleTextArea() {
    this("");
  }

  public JLabel getLabel() {
    return label;
  }

  public String getText() {
    return this.area.getText();
  }

  public JTextArea getArea() {
    return this.area;
  }

  public void setText(String text) {
    this.area.setText(text);
  }
}
