package net.sourceforge.kolmafia.swingui.menu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.widget.PossibleSelection;
import net.sourceforge.kolmafia.utilities.IntWrapper;

public class LootHunterMenuItem extends ThreadedMenuItem {
  public LootHunterMenuItem() {
    super("Visit Bounty Hunter", new LootHunterListener());
  }

  private static class LootHunterListener extends ThreadedListener {
    @Override
    protected void execute() {
      GenericRequest hunterRequest = new BountyHunterHunterRequest();
      RequestThread.postRequest(hunterRequest);

      IntWrapper wrapper = new IntWrapper();

      List<PossibleSelection> bounties = new ArrayList<>();
      String[] results = new String[2];

      // Add Easy Bounty Item
      String untakenBounty = Preferences.getString("_untakenEasyBountyItem");
      if (!untakenBounty.equals("")) {
        results = LootHunterMenuItem.buildInformation("easy", untakenBounty, 0);
        bounties.add(new PossibleSelection(results[0], results[1], 1, wrapper));
      }

      // Add Hard Bounty Item
      untakenBounty = Preferences.getString("_untakenHardBountyItem");
      if (!untakenBounty.equals("")) {
        results = LootHunterMenuItem.buildInformation("hard", untakenBounty, 0);
        bounties.add(new PossibleSelection(results[0], results[1], 2, wrapper));
      }

      // Add Speciality Bounty Item
      untakenBounty = Preferences.getString("_untakenSpecialBountyItem");
      if (!untakenBounty.equals("")) {
        results = LootHunterMenuItem.buildInformation("speciality", untakenBounty, 0);
        bounties.add(new PossibleSelection(results[0], results[1], 3, wrapper));
      }

      if (bounties.isEmpty()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "No more bounties available today.");
        return;
      }

      boolean selectedAValue =
          LootHunterMenuItem.getSelectedValueFromList(
              "Select bounty",
              "Choose a bounty to collect:",
              "Choose this bounty",
              "Don't choose a bounty",
              bounties);
      if (!selectedAValue) {
        return;
      }

      switch (wrapper.getChoice()) {
        case 1:
          RequestThread.postRequest(new BountyHunterHunterRequest("takelow"));
          break;
        case 2:
          RequestThread.postRequest(new BountyHunterHunterRequest("takehigh"));
          break;
        case 3:
          RequestThread.postRequest(new BountyHunterHunterRequest("takespecial"));
          break;
      }
    }
  }

  /**
   * Asks the user to make a selection from a fixed list.  The choices are
   * presented to the user as radio buttons in a dialog.  Note that this method
   * only returns an indication of success or failure.  The actual value
   * selected will be stored in the {@link IntWrapper} assigned to the
   * choices.
   *
   * @param title	a string to show in the titlebar
   * @param message	a message to preface the list of choices with
   * @param yesLabel	a label to place on the button denoting confirmation
   * @param noLabel	a label to place on the button denoting cancellation
   * @param choices	a list of possible choices
   * @return	<code>true<code> if a choice was made, <code>false</code> otherwise.
   */
  private static boolean getSelectedValueFromList(
      final String title,
      final String message,
      final String yesLabel,
      final String noLabel,
      final List<PossibleSelection> choices) {
    JPanel choicePanel = new JPanel();
    choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));

    choicePanel.add(new JLabel(message));

    ButtonGroup buttonGroup = new ButtonGroup();

    Iterator<PossibleSelection> it = choices.iterator();
    while (it.hasNext()) {
      PossibleSelection c = it.next();

      JRadioButton radio =
          new JRadioButton("<html>" + c.getLabel() + "<br>" + c.getDescription() + "</html>");
      radio.addActionListener(c);

      if (choicePanel.getComponentCount() < 2) radio.doClick();

      choicePanel.add(radio);
      buttonGroup.add(radio);
    }

    String[] dialogOptions = {yesLabel, noLabel};

    int result =
        JOptionPane.showOptionDialog(
            null,
            choicePanel,
            title,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            dialogOptions,
            null);

    return (result == 0);
  }

  private static String[] buildInformation(String type, String item, int number) {
    StringBuffer label = new StringBuffer();
    StringBuffer description = new StringBuffer();

    if (item == null || item.equals("")) {
      label.setLength(0);
      label.append("<b>No ");
      label.append(type);
      label.append(" bounty available.");
      description.setLength(0);
    } else {
      String location = BountyDatabase.getLocation(item);
      if (location != null) {
        KoLAdventure adventure = AdventureDatabase.getAdventure(location);
        if (adventure != null) {
          AreaCombatData locationInfo = adventure.getAreaSummary();

          int totalNumber = BountyDatabase.getNumber(item);
          String plural = BountyDatabase.getPlural(item);
          if (plural != null) {
            label.setLength(0);
            label.append("Get <b>");
            if (number != 0) {
              label.append((totalNumber - number));
              label.append(" of ");
            }
            label.append(totalNumber);
            label.append(" ");
            label.append(plural);
            label.append("</b> from ");
            label.append(adventure.getAdventureName());

            description.setLength(0);
            description.append("<i>Combat rate: ");
            description.append(Math.round(locationInfo.areaCombatPercent()));
            description.append("%; ");
            description.append("1/");
            description.append(locationInfo.getAvailableMonsterCount());
            description.append(" monsters drop bounty item.</i>");
          }
        }
      }
    }
    String[] results = new String[2];
    results[0] = label.toString();
    results[1] = description.toString();
    return results;
  }
}
