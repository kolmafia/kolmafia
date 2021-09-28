package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.listener.CharacterListener;
import net.sourceforge.kolmafia.listener.CharacterListenerRegistry;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.swingui.panel.AdventureSelectPanel;

public class CharSheetFrame extends GenericFrame {
  private final AvatarLabel avatar;
  private JProgressBar[] tnpDisplay;
  private final CharacterListener statusRefresher;

  /** Constructs a new character sheet, using the data located in the provided session. */
  public CharSheetFrame() {
    super("Daily Deeds");

    JPanel statusPanel = new JPanel(new BorderLayout(10, 10));

    this.avatar = new AvatarLabel();

    statusPanel.add(this.createStatusPanel(), BorderLayout.CENTER);
    statusPanel.add(this.avatar, BorderLayout.WEST);

    JPanel statusContainer = new JPanel(new CardLayout(10, 10));
    statusContainer.add(statusPanel, "");

    JPanel summaryContainer = new JPanel(new CardLayout(10, 10));
    summaryContainer.add(AdventureSelectPanel.getAdventureSummary("statusDropdown"), "");

    JPanel charSheetPanel = new JPanel(new BorderLayout());
    charSheetPanel.add(statusContainer, BorderLayout.NORTH);
    charSheetPanel.add(summaryContainer, BorderLayout.CENTER);

    this.statusRefresher = new CharacterListener(new StatusRefreshRunnable());
    CharacterListenerRegistry.addCharacterListener(this.statusRefresher);

    this.statusRefresher.update();
    JComponentUtilities.setComponentSize(charSheetPanel, -1, 480);

    this.setCenterComponent(charSheetPanel);
  }

  @Override
  public void dispose() {
    CharacterListenerRegistry.removeCharacterListener(this.statusRefresher);
    super.dispose();
  }

  @Override
  public boolean useSidePane() {
    return false;
  }

  /**
   * Utility method for modifying a panel that displays the given label, using formatting if the
   * values are different.
   */
  private void refreshValuePanel(
      final int displayIndex, final int baseValue, final int tillNextPoint, final String label) {
    JProgressBar tnp = this.tnpDisplay[displayIndex];

    if (baseValue == KoLCharacter.MAX_BASEPOINTS) {
      tnp.setMaximum(0);
      tnp.setValue(0);
      tnp.setString("No more progress possible");
    } else {
      tnp.setMaximum(2 * baseValue + 1);
      tnp.setValue(2 * baseValue + 1 - tillNextPoint);
      tnp.setString(
          label
              + KoLConstants.COMMA_FORMAT.format(tnp.getValue())
              + " / "
              + KoLConstants.COMMA_FORMAT.format(tnp.getMaximum()));
    }

    int points = KoLCharacter.getTriggerPoints(displayIndex);

    int triggerItemId = KoLCharacter.getTriggerItem(displayIndex);

    if (points == Integer.MAX_VALUE || triggerItemId <= 0) {
      tnp.setToolTipText("You can equip everything you have!");
    } else {
      String triggerItem = ItemDatabase.getItemName(triggerItemId);

      tnp.setToolTipText(
          "At "
              + points
              + " points, you'll be able to equip a "
              + triggerItem
              + " (and maybe more)");
    }
  }

  public class AvatarLabel extends JLabel implements Listener {
    public AvatarLabel() {
      super();
      NamedListenerRegistry.registerNamedListener("(avatar)", this);
      this.update();
    }

    public void update() {
      ImageIcon icon = JComponentUtilities.getImage(KoLCharacter.getAvatar());
      this.setIcon(icon);
    }
  }

  /**
   * Utility method for creating a panel displaying the character's vital statistics, including a
   * basic stat overview and available turns/meat.
   *
   * @return a <code>JPanel</code> displaying the character's statistics
   */
  private Box createStatusPanel() {
    Box statusPanel = Box.createVerticalBox();
    statusPanel.add(Box.createVerticalGlue());

    this.tnpDisplay = new JProgressBar[3];
    for (int i = 0; i < 3; ++i) {
      this.tnpDisplay[i] = new JProgressBar();
      this.tnpDisplay[i].setValue(0);
      this.tnpDisplay[i].setStringPainted(true);
      statusPanel.add(this.tnpDisplay[i]);
      statusPanel.add(Box.createVerticalGlue());
    }

    statusPanel.setBorder(BorderFactory.createTitledBorder("Substats till next point"));
    return statusPanel;
  }

  private class StatusRefreshRunnable implements Runnable {
    public void run() {
      CharSheetFrame.this.refreshValuePanel(
          0, KoLCharacter.getBaseMuscle(), KoLCharacter.getMuscleTNP(), "Mus: ");
      CharSheetFrame.this.refreshValuePanel(
          1, KoLCharacter.getBaseMysticality(), KoLCharacter.getMysticalityTNP(), "Mys: ");
      CharSheetFrame.this.refreshValuePanel(
          2, KoLCharacter.getBaseMoxie(), KoLCharacter.getMoxieTNP(), "Mox: ");

      // Set the current avatar
      CharSheetFrame.this.avatar.setIcon(JComponentUtilities.getImage(KoLCharacter.getAvatar()));
    }
  }
}
