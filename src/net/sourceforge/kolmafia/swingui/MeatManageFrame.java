package net.sourceforge.kolmafia.swingui;

import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.ShrineRequest;
import net.sourceforge.kolmafia.swingui.panel.LabeledPanel;
import net.sourceforge.kolmafia.swingui.panel.MeatTransferPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MeatManageFrame extends GenericFrame {
  public MeatManageFrame() {
    super("Meat Manager");

    JPanel container = new JPanel(new GridLayout(4, 1));
    container.add(new HeroDonationPanel());
    container.add(new MeatTransferPanel(MeatTransferPanel.MEAT_TO_CLOSET));
    container.add(new MeatTransferPanel(MeatTransferPanel.MEAT_TO_INVENTORY));
    container.add(new MeatTransferPanel(MeatTransferPanel.PULL_MEAT_FROM_STORAGE));

    this.setCenterComponent(container);
  }

  @Override
  public JTabbedPane getTabbedPane() {
    return null;
  }

  /**
   * An internal class which represents the panel used for donations to the statues in the shrine.
   */
  private class HeroDonationPanel extends LabeledPanel {
    private final JComboBox<String> heroField;
    private final AutoHighlightTextField amountField;

    public HeroDonationPanel() {
      super(
          "Donations to the Greater Good",
          "donate",
          "explode",
          new Dimension(80, 20),
          new Dimension(240, 20));

      LockableListModel<String> heroes = new LockableListModel<String>();
      heroes.add("Statue of Boris");
      heroes.add("Statue of Jarlsberg");
      heroes.add("Statue of Sneaky Pete");

      this.heroField = new JComboBox<>(heroes);
      this.amountField = new AutoHighlightTextField();

      VerifiableElement[] elements = new VerifiableElement[2];
      elements[0] = new VerifiableElement("Donate To: ", this.heroField);
      elements[1] = new VerifiableElement("Amount: ", this.amountField);

      this.setContent(elements);
    }

    @Override
    public void actionConfirmed() {
      if (this.heroField.getSelectedIndex() != -1) {
        RequestThread.postRequest(
            new ShrineRequest(
                this.heroField.getSelectedIndex() + 1,
                InputFieldUtilities.getValue(this.amountField)));
      }
    }

    @Override
    public void actionCancelled() {
      this.setStatusMessage("The Frost poem you dialed is unavailable at this time.");
    }
  }
}
