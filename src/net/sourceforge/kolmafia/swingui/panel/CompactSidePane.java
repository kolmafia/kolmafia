package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.menu.ThreadedMenuItem;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.webui.CharPaneDecorator;

public class CompactSidePane extends JPanel implements Runnable {
  private final JPanel levelPanel;
  private final JProgressBar levelMeter;
  private final JLabel levelLabel, roninLabel, mcdLabel;
  private final int STAT_LABELS = 3;
  private final JLabel[] statLabel = new JLabel[STAT_LABELS];
  private final JLabel[] statValueLabel = new JLabel[STAT_LABELS];
  private final int STATUS_LABELS = 8;
  private final JLabel[] statusLabel = new JLabel[STATUS_LABELS];
  private final JLabel[] statusValueLabel = new JLabel[STATUS_LABELS];
  private final int CONSUMPTION_LABELS = 3;
  private final JLabel[] consumptionLabel = new JLabel[CONSUMPTION_LABELS];
  private final JLabel[] consumptionValueLabel = new JLabel[CONSUMPTION_LABELS];
  private final FamiliarLabel familiarLabel;
  private final int BONUS_LABELS = 10;
  private final JLabel[] bonusLabel = new JLabel[BONUS_LABELS];
  private final JLabel[] bonusValueLabel = new JLabel[BONUS_LABELS];
  protected final JPopupMenu modPopup;
  private final JLabel modPopLabel;

  // Sneaky Pete's Motorcycle
  protected final JPopupMenu motPopup;
  private final JLabel motPopLabel;

  // Quantum Familiar
  private final JPanel quantumFamiliarPanel;

  private static final AdventureResult CLUMSY = EffectPool.get(EffectPool.CLUMSY);
  private static final AdventureResult SLIMED = EffectPool.get(EffectPool.COATED_IN_SLIME);

  public CompactSidePane() {
    super(new BorderLayout());

    JPanel labelPanel, valuePanel;

    JPanel[] panels = new JPanel[6];
    int panelCount = -1;

    panels[++panelCount] = new JPanel(new BorderLayout());
    this.levelPanel = panels[0];

    panels[panelCount].add(this.levelLabel = new JLabel(" ", JLabel.CENTER), BorderLayout.NORTH);

    panels[panelCount].add(this.levelMeter = new JProgressBar(), BorderLayout.CENTER);
    this.levelMeter.setOpaque(true);
    this.levelMeter.setStringPainted(true);
    JComponentUtilities.setComponentSize(this.levelMeter, 40, 6);
    panels[panelCount].add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panels[panelCount].add(Box.createHorizontalStrut(10), BorderLayout.EAST);
    panels[panelCount].setOpaque(false);

    JPanel holderPanel = new JPanel(new GridLayout(2, 1));
    holderPanel.add(this.roninLabel = new JLabel(" ", JLabel.CENTER));
    holderPanel.add(this.mcdLabel = new JLabel(" ", JLabel.CENTER));
    holderPanel.setOpaque(false);
    panels[panelCount].add(holderPanel, BorderLayout.SOUTH);

    panels[++panelCount] = new JPanel(new BorderLayout());
    panels[panelCount].setOpaque(false);

    labelPanel = new JPanel(new GridLayout(this.STAT_LABELS, 1));
    labelPanel.setOpaque(false);

    for (int i = 0; i < this.STAT_LABELS; i++) {
      labelPanel.add(this.statLabel[i] = new JLabel(" ", JLabel.RIGHT));
    }

    valuePanel = new JPanel(new GridLayout(this.STAT_LABELS, 1));
    valuePanel.setOpaque(false);

    for (int i = 0; i < this.STAT_LABELS; i++) {
      valuePanel.add(this.statValueLabel[i] = new JLabel(" ", JLabel.LEFT));
    }

    panels[panelCount].add(labelPanel, BorderLayout.WEST);
    panels[panelCount].add(valuePanel, BorderLayout.CENTER);

    panels[++panelCount] = new JPanel(new BorderLayout());
    panels[panelCount].setOpaque(false);

    labelPanel = new JPanel(new GridLayout(this.STATUS_LABELS, 1));
    labelPanel.setOpaque(false);

    for (int i = 0; i < this.STATUS_LABELS; i++) {
      labelPanel.add(this.statusLabel[i] = new JLabel(" ", JLabel.RIGHT));
    }

    valuePanel = new JPanel(new GridLayout(this.STATUS_LABELS, 1));
    valuePanel.setOpaque(false);

    for (int i = 0; i < this.STATUS_LABELS; i++) {
      valuePanel.add(this.statusValueLabel[i] = new JLabel(" ", JLabel.LEFT));
    }

    panels[panelCount].add(labelPanel, BorderLayout.WEST);
    panels[panelCount].add(valuePanel, BorderLayout.CENTER);

    panels[++panelCount] = new JPanel(new BorderLayout());
    panels[panelCount].setOpaque(false);

    labelPanel = new JPanel(new GridLayout(this.CONSUMPTION_LABELS, 1));
    labelPanel.setOpaque(false);

    for (int i = 0; i < this.CONSUMPTION_LABELS; i++) {
      labelPanel.add(this.consumptionLabel[i] = new JLabel(" ", JLabel.RIGHT));
    }

    valuePanel = new JPanel(new GridLayout(this.CONSUMPTION_LABELS, 1));
    valuePanel.setOpaque(false);

    for (int i = 0; i < this.CONSUMPTION_LABELS; i++) {
      valuePanel.add(this.consumptionValueLabel[i] = new JLabel(" ", JLabel.LEFT));
    }

    panels[panelCount].add(labelPanel, BorderLayout.WEST);
    panels[panelCount].add(valuePanel, BorderLayout.CENTER);

    panels[++panelCount] = new JPanel(new GridLayout(2, 1));
    panels[panelCount].add(this.familiarLabel = new FamiliarLabel());
    panels[panelCount].add(this.quantumFamiliarPanel = new QuantumFamiliarPanel());

    // Make a popup label for Sneaky Pete's motorcycle. Clicking on
    // the motorcycle image (which replaces the familiar icon)
    // activates it.
    this.motPopLabel = new JLabel();
    this.motPopup = new JPopupMenu();

    this.motPopup.insert(this.motPopLabel, 0);

    panels[++panelCount] = new JPanel(new GridLayout(this.BONUS_LABELS, 2));

    for (int i = 0; i < this.BONUS_LABELS; i++) {
      panels[panelCount].add(this.bonusLabel[i] = new JLabel(" ", JLabel.RIGHT));
      panels[panelCount].add(this.bonusValueLabel[i] = new JLabel(" ", JLabel.LEFT));
    }

    this.modPopLabel = new JLabel();
    this.modPopup = new JPopupMenu();
    this.modPopup.insert(this.modPopLabel, 0);
    panels[panelCount].addMouseListener(new ModPopListener());

    JPanel compactContainer = new JPanel();
    compactContainer.setOpaque(false);
    compactContainer.setLayout(new BoxLayout(compactContainer, BoxLayout.Y_AXIS));

    for (JPanel panel : panels) {
      panel.setOpaque(false);
      compactContainer.add(panel);
      compactContainer.add(Box.createVerticalStrut(20));
    }

    compactContainer.add(Box.createHorizontalStrut(110));

    JPanel compactCard = new JPanel(new CardLayout(8, 8));
    compactCard.setOpaque(false);
    compactCard.add(compactContainer, "");

    JPanel refreshPanel = new JPanel();
    refreshPanel.setOpaque(false);
    String iconSetPrefix;
    iconSetPrefix = KoLmafiaGUI.isDarkTheme() ? "themes/dark/" : "";
    InvocationButton refreshButton =
        new InvocationButton(
            "Refresh Status", iconSetPrefix + "refresh.gif", ApiRequest.class, "updateStatus");
    refreshButton.setContentAreaFilled(false);
    refreshPanel.add(refreshButton);
    this.add(refreshPanel, BorderLayout.SOUTH);
    this.add(compactCard, BorderLayout.NORTH);

    /* We're going to try just letting the Look and Feel take care of this...
    this.levelLabel.setForeground( Color.BLACK );
    this.roninLabel.setForeground( Color.BLACK );
    this.mcdLabel.setForeground( Color.BLACK );
    for ( int i = 0; i < this.STAT_LABELS ; i++ )
    {
      this.statLabel[ i ].setForeground( Color.BLACK );
      this.statValueLabel[ i ].setForeground( Color.BLACK );
    }
    for ( int i = 0; i < this.STATUS_LABELS ; i++ )
    {
      this.statusLabel[ i ].setForeground( Color.BLACK );
      this.statusValueLabel[ i ].setForeground( Color.BLACK );
    }
    for ( int i = 0; i < this.CONSUMPTION_LABELS ; i++ )
    {
      this.consumptionLabel[ i ].setForeground( Color.BLACK );
      this.consumptionValueLabel[ i ].setForeground( Color.BLACK );
    }
    for ( int i = 0; i < this.BONUS_LABELS ; i++ )
    {
      this.bonusLabel[ i ].setForeground( Color.BLACK );
      this.bonusValueLabel[ i ].setForeground( Color.BLACK );
    }


    */
  }

  private class ModPopListener extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      JPopupMenu JPM = CompactSidePane.this.modPopup;
      SwingUtilities.invokeLater(
          () -> {
            SwingUtilities.updateComponentTreeUI(JPM); // update components
            JPM.pack();
          });
      JPM.show(e.getComponent(), e.getX(), e.getY());
    }
  }

  private class FamPopListener extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      if (KoLCharacter.isSneakyPete()) {
        CompactSidePane.this.motPopup.show(e.getComponent(), e.getX(), e.getY());
        return;
      }

      JPopupMenu famPopup = new JPopupMenu();

      if (KoLCharacter.inAxecore()) {
        this.addInstruments(famPopup);
      } else if (KoLCharacter.isEd()) {
        this.addServants(famPopup);
      } else if (!KoLCharacter.inPokefam() && !KoLCharacter.inQuantum()) {
        this.addFamiliars(famPopup);
      }

      famPopup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void addInstruments(JPopupMenu famPopup) {
      AdventureResult item = CharPaneRequest.SACKBUT;
      if (item.getCount(KoLConstants.inventory) > 0) {
        famPopup.add(new InstrumentMenuItem(item));
      }
      item = CharPaneRequest.CRUMHORN;
      if (item.getCount(KoLConstants.inventory) > 0) {
        famPopup.add(new InstrumentMenuItem(item));
      }
      item = CharPaneRequest.LUTE;
      if (item.getCount(KoLConstants.inventory) > 0) {
        famPopup.add(new InstrumentMenuItem(item));
      }
    }

    private void addServants(JPopupMenu famPopup) {
      EdServantData current = EdServantData.currentServant();
      for (EdServantData servant : EdServantData.getServants()) {
        if (servant != current && servant != EdServantData.NO_SERVANT) {
          famPopup.add(new ServantMenuItem(servant));
        }
      }
    }

    private void addFamiliars(JPopupMenu famPopup) {
      famPopup.setLayout(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.CENTER;
      c.insets = new Insets(5, 0, 0, 0);
      c.weightx = 1;
      c.gridy = 0;

      JMenu stat = new JMenu("stat gain");
      JMenu item = new JMenu("item drop");
      JMenu meat = new JMenu("meat drop");
      JMenu drops = new JMenu("special drops");

      // Combat submenus
      JMenu combat0 = new JMenu("physical only");
      JMenu combat1 = new JMenu("elemental only");
      JMenu combat01 = new JMenu("physical and elemental");
      JMenu block = new JMenu("block");
      JMenu delevel = new JMenu("delevel");
      JMenu hp0 = new JMenu("restore HP");
      JMenu mp0 = new JMenu("restore MP");
      JMenu stat2 = new JMenu("stats");
      JMenu meat1 = new JMenu("meat");
      JMenu other0 = new JMenu("anything else");

      // After Combat submenu
      JMenu hp1 = new JMenu("restore HP");
      JMenu mp1 = new JMenu("restore MP");
      JMenu stat3 = new JMenu("stats");
      JMenu other1 = new JMenu("anything else");

      JMenu passive = new JMenu("passive");
      JMenu underwater = new JMenu("underwater");
      JMenu variable = new JMenu("configurable");

      // None of the above
      JMenu other = new JMenu("other");

      String[] custom = new String[9];
      JMenu[] customMenu = new JMenu[9];
      for (int i = 0; i < 9; ++i) {
        String pref = Preferences.getString("familiarCategory" + (i + 1));
        if (pref.length() > 0) {
          custom[i] = pref.toLowerCase();
          customMenu[i] = new JMenu(pref.split("\\|", 2)[0]);
        }
      }
      for (FamiliarData fam : KoLCharacter.getFamiliarList()) {
        if (fam == FamiliarData.NO_FAMILIAR) {
          continue; // no menu item for this one
        }

        // If we cannot equip this familiar for some reason, skip it.
        if (!fam.canEquip()) {
          continue;
        }

        if (fam.equals(KoLCharacter.getFamiliar())) {
          continue; // no menu item for this one
        }

        if (fam.getFavorite()) {
          if (++c.gridx >= 3) {
            c.gridx = 0;
            c.gridy++;
          }

          famPopup.add(new FamiliarMenuItem(fam), c);
          continue;
        }

        int id = fam.getId();
        Modifiers mods = Modifiers.getModifiers("Familiar", fam.getRace());
        boolean added = false;

        // Stat Gain
        if (FamiliarDatabase.isVolleyType(id)
            || FamiliarDatabase.isSombreroType(id)
            || (mods != null && mods.get(Modifiers.VOLLEYBALL_WEIGHT) != 0.0)) {
          stat.add(new FamiliarMenuItem(fam));
          added = true;
        }

        // Item Drop
        if (FamiliarDatabase.isFairyType(id)) {
          item.add(new FamiliarMenuItem(fam));
          added = true;
        }

        // Meat Drop
        if (FamiliarDatabase.isMeatDropType(id)) {
          meat.add(new FamiliarMenuItem(fam));
          added = true;
        }

        // Special drops
        if (FamiliarDatabase.isDropType(id)) {
          drops.add(new FamiliarMenuItem(fam));
          added = true;
        }

        // Combat submenus
        boolean is0 = FamiliarDatabase.isCombat0Type(id);
        boolean is1 = FamiliarDatabase.isCombat1Type(id);

        if (is0 && !is1) {
          combat0.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (is1 && !is0) {
          combat1.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (is0 && is1) {
          combat01.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isBlockType(id)) {
          block.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isDelevelType(id)) {
          delevel.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isHp0Type(id)) {
          hp0.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isMp0Type(id)) {
          mp0.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isStat2Type(id)) {
          stat2.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isMeat1Type(id)) {
          meat1.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isOther0Type(id)) {
          other0.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isHp1Type(id)) {
          hp1.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isMp1Type(id)) {
          mp1.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isStat3Type(id)) {
          stat3.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isOther1Type(id)) {
          other1.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isPassiveType(id)) {
          passive.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isUnderwaterType(id)) {
          underwater.add(new FamiliarMenuItem(fam));
          added = true;
        }
        if (FamiliarDatabase.isVariableType(id)) {
          variable.add(new FamiliarMenuItem(fam));
          added = true;
        }

        String key = "|" + fam.getRace().toLowerCase();
        for (int i = 0; i < 9; ++i) {
          if (custom[i] != null && custom[i].contains(key)) {
            customMenu[i].add(new FamiliarMenuItem(fam));
            added = true;
          }
        }

        if (!added) {
          other.add(new FamiliarMenuItem(fam));
        }
      }

      c.gridx = 0;
      c.gridwidth = 3;

      // Unless we have no familiar equipped, add "no familiar" at end of favorites
      if (KoLCharacter.currentFamiliar != FamiliarData.NO_FAMILIAR) {
        c.gridy++;
        famPopup.add(new FamiliarMenuItem(FamiliarData.NO_FAMILIAR), c);
      }

      c.insets = new Insets(0, 0, 0, 0);

      if (stat.getMenuComponentCount() > 0) {
        c.gridy++;
        famPopup.add(stat, c);
      }
      if (item.getMenuComponentCount() > 0) {
        c.gridy++;
        famPopup.add(item, c);
      }
      if (meat.getMenuComponentCount() > 0) {
        c.gridy++;
        famPopup.add(meat, c);
      }

      if (drops.getMenuComponentCount() > 0) {
        c.gridy++;
        famPopup.add(drops, c);
      }

      if (combat0.getMenuComponentCount() > 0
          || combat1.getMenuComponentCount() > 0
          || combat01.getMenuComponentCount() > 0
          || block.getMenuComponentCount() > 0
          || delevel.getMenuComponentCount() > 0
          || hp0.getMenuComponentCount() > 0
          || mp0.getMenuComponentCount() > 0
          || stat2.getMenuComponentCount() > 0
          || meat1.getMenuComponentCount() > 0
          || other0.getMenuComponentCount() > 0) {
        JMenu combat = new JMenu("combat");

        if (combat0.getMenuComponentCount() > 0) {
          combat.add(combat0);
        }
        if (combat1.getMenuComponentCount() > 0) {
          combat.add(combat1);
        }
        if (combat01.getMenuComponentCount() > 0) {
          combat.add(combat01);
        }
        if (block.getMenuComponentCount() > 0) {
          combat.add(block);
        }
        if (delevel.getMenuComponentCount() > 0) {
          combat.add(delevel);
        }
        if (hp0.getMenuComponentCount() > 0) {
          combat.add(hp0);
        }
        if (mp0.getMenuComponentCount() > 0) {
          combat.add(mp0);
        }
        if (stat2.getMenuComponentCount() > 0) {
          combat.add(stat2);
        }
        if (meat1.getMenuComponentCount() > 0) {
          combat.add(meat1);
        }
        if (other0.getMenuComponentCount() > 0) {
          combat.add(other0);
        }

        c.gridy++;
        famPopup.add(combat, c);
      }

      if (hp1.getMenuComponentCount() > 0
          || mp1.getMenuComponentCount() > 0
          || stat3.getMenuComponentCount() > 0
          || other1.getMenuComponentCount() > 0) {
        JMenu aftercombat = new JMenu("after combat");

        if (hp1.getMenuComponentCount() > 0) {
          aftercombat.add(hp1);
        }
        if (mp1.getMenuComponentCount() > 0) {
          aftercombat.add(mp1);
        }
        if (stat3.getMenuComponentCount() > 0) {
          aftercombat.add(stat3);
        }
        if (other1.getMenuComponentCount() > 0) {
          aftercombat.add(other1);
        }

        c.gridy++;
        famPopup.add(aftercombat, c);
      }

      if (passive.getMenuComponentCount() > 0) {
        c.gridy++;
        famPopup.add(passive, c);
      }
      if (underwater.getMenuComponentCount() > 0) {
        c.gridy++;
        famPopup.add(underwater, c);
      }
      if (variable.getMenuComponentCount() > 0) {
        c.gridy++;
        famPopup.add(variable, c);
      }

      for (int i = 0; i < 9; ++i) {
        JMenu menu = customMenu[i];

        if (menu != null && menu.getMenuComponentCount() > 0) {
          c.gridy++;
          famPopup.add(menu, c);
        }
      }

      if (other.getMenuComponentCount() > 0) {
        c.gridy++;
        famPopup.add(other, c);
      }
    }
  }

  private static class FamiliarMenuItem extends ThreadedMenuItem {
    public FamiliarMenuItem(final FamiliarData fam) {
      super(fam.getRace(), new FamiliarListener(fam));

      if (fam.getFavorite()) {
        ImageIcon icon = FamiliarDatabase.getFamiliarImage(fam.getId());
        this.setIcon(icon);
        this.setText("");
        this.setToolTipText(fam.getRace());
        Dimension size = new Dimension(icon.getIconWidth(), icon.getIconHeight());
        this.setPreferredSize(size);
        this.setMinimumSize(size);
        icon.setImageObserver(this);
      }
    }
  }

  private static class FamiliarListener extends ThreadedListener {
    private final FamiliarData familiar;

    public FamiliarListener(FamiliarData familiar) {
      this.familiar = familiar;
    }

    @Override
    protected void execute() {
      String arg = this.familiar == FamiliarData.NO_FAMILIAR ? " none" : this.familiar.getRace();
      CommandDisplayFrame.executeCommand("familiar " + arg);
    }
  }

  private static class InstrumentMenuItem extends ThreadedMenuItem {
    public InstrumentMenuItem(final AdventureResult item) {
      super(item.getName(), new UseItemListener(item));
      ImageIcon icon = ItemDatabase.getItemImage(item.getItemId());
      this.setIcon(icon);
      icon.setImageObserver(this);
    }
  }

  private static class UseItemListener extends ThreadedListener {
    private final String command;

    public UseItemListener(AdventureResult item) {
      this.command = "use " + item.getName();
    }

    @Override
    protected void execute() {
      CommandDisplayFrame.executeCommand(this.command);
    }
  }

  private static class ServantMenuItem extends ThreadedMenuItem {
    public ServantMenuItem(final EdServantData servant) {
      super(servant.getType(), new ChangeServantListener(servant));
      ImageIcon icon = FileUtilities.downloadIcon(servant.getImage(), "itemimages", "debug.gif");
      this.setIcon(icon);
      icon.setImageObserver(this);
    }
  }

  private static class ChangeServantListener extends ThreadedListener {
    private final EdServantData servant;

    public ChangeServantListener(EdServantData servant) {
      this.servant = servant;
    }

    @Override
    protected void execute() {
      CommandDisplayFrame.executeCommand("servant " + this.servant.getType());
    }
  }

  public String getStatText(final int adjusted, final int base) {
    String statText;
    if (KoLmafiaGUI.isDarkTheme()) {

      statText =
          adjusted == base
              ? "<html><font color=#dddddd>" + base + "</font>"
              : adjusted > base
                  ? "<html><font color=#00d4ff>"
                      + adjusted
                      + "</font> (<font color=#dddddd>"
                      + base
                      + "</font>"
                      + ")"
                  : "<html><font color=#ff8a93>"
                      + adjusted
                      + "</font> (<font color=#dddddd>"
                      + base
                      + "</font>"
                      + ")";
    } else {
      statText =
          adjusted == base
              ? "<html>" + base
              : adjusted > base
                  ? "<html><font color=blue>" + adjusted + "</font> (" + base + ")"
                  : "<html><font color=red>" + adjusted + "</font> (" + base + ")";
    }
    return statText;
  }

  public void run() {
    String limitmode = KoLCharacter.getLimitmode();

    if (limitmode != Limitmode.SPELUNKY) {
      this.levelLabel.setText("Level " + KoLCharacter.getLevel());
    } else {
      this.levelLabel.setText(" ");
    }

    if (limitmode == Limitmode.SPELUNKY) {
      this.roninLabel.setText("(Spelunkin')");
    } else if (CharPaneRequest.inValhalla()) {
      this.roninLabel.setText("(Valhalla)");
    } else if (KoLCharacter.inBadMoon()) {
      this.roninLabel.setText("(Bad Moon)");
    } else if (KoLCharacter.isHardcore()) {
      this.roninLabel.setText("(Hardcore)");
    } else if (KoLCharacter.canInteract()) {
      this.roninLabel.setText("(Ronin Clear)");
    } else if (KoLCharacter.inPokefam()) {
      this.roninLabel.setText("(Endless Ronin)");
    } else {
      this.roninLabel.setText("(Ronin for " + KoLCharacter.roninLeft() + ")");
    }

    if (limitmode != Limitmode.SPELUNKY) {
      this.mcdLabel.setText("ML @ " + KoLCharacter.getMindControlLevel());
    } else {
      this.mcdLabel.setText("");
    }

    int count = 0;
    this.statLabel[count].setText("   Mus: ");
    this.statValueLabel[count].setText(
        this.getStatText(KoLCharacter.getAdjustedMuscle(), KoLCharacter.getBaseMuscle()));
    count++;
    if (limitmode != Limitmode.SPELUNKY) {
      this.statLabel[count].setText("   Mys: ");
      this.statValueLabel[count].setText(
          this.getStatText(
              KoLCharacter.getAdjustedMysticality(), KoLCharacter.getBaseMysticality()));
      count++;
    }
    this.statLabel[count].setText("   Mox: ");
    this.statValueLabel[count].setText(
        this.getStatText(KoLCharacter.getAdjustedMoxie(), KoLCharacter.getBaseMoxie()));
    count++;
    for (int i = count; i < STAT_LABELS; i++) {
      this.statLabel[i].setText("");
      this.statValueLabel[i].setText("");
    }

    count = 0;
    int limit = KoLCharacter.getFullnessLimit();
    if (limit > 0) {
      this.consumptionLabel[count].setText("  Full: ");
      this.consumptionValueLabel[count].setText(KoLCharacter.getFullness() + " / " + limit);
      count++;
    }
    limit = KoLCharacter.getInebrietyLimit();
    if (limit > 0) {
      this.consumptionLabel[count].setText(" Drunk: ");
      this.consumptionValueLabel[count].setText(KoLCharacter.getInebriety() + " / " + limit);
      count++;
    }
    limit = KoLCharacter.getSpleenLimit();
    if (limit > 0) {
      this.consumptionLabel[count].setText("Spleen: ");
      this.consumptionValueLabel[count].setText(KoLCharacter.getSpleenUse() + " / " + limit);
      count++;
    }
    for (int i = count; i < CONSUMPTION_LABELS; i++) {
      this.consumptionLabel[i].setText("");
      this.consumptionValueLabel[i].setText("");
    }

    count = 0;
    this.statusLabel[count].setText("    HP: ");
    this.statusValueLabel[count].setText(
        KoLConstants.COMMA_FORMAT.format(KoLCharacter.getCurrentHP())
            + " / "
            + KoLConstants.COMMA_FORMAT.format(KoLCharacter.getMaximumHP()));
    count++;

    if (limitmode != Limitmode.SPELUNKY) {
      // Paths
      if (!KoLCharacter.isVampyre()) {
        this.statusLabel[count].setText("    MP: ");
        this.statusValueLabel[count].setText(
            KoLConstants.COMMA_FORMAT.format(KoLCharacter.getCurrentMP())
                + " / "
                + KoLConstants.COMMA_FORMAT.format(KoLCharacter.getMaximumMP()));
        count++;
      }

      if (KoLCharacter.inBeecore()) {
        this.statusLabel[count].setText(" Bees: ");
        this.statusValueLabel[count].setText(String.valueOf(KoLCharacter.getBeeosity()));
        count++;
      } else if (KoLCharacter.inZombiecore()) {
        this.statusLabel[count].setText(" Horde: ");
        this.statusValueLabel[count].setText(String.valueOf(KoLCharacter.getCurrentMP()));
        count++;
      } else if (KoLCharacter.isSneakyPete()) {
        limit = KoLCharacter.getAudienceLimit();
        this.statusLabel[count].setText("   Aud: ");
        this.statusValueLabel[count].setText(KoLCharacter.getAudience() + " / " + limit);
        count++;
      } else if (KoLCharacter.isEd()) {
        this.statusLabel[count].setText("    Ka: ");
        this.statusValueLabel[count].setText(
            String.valueOf(InventoryManager.getCount(ItemPool.KA_COIN)));
        count++;
      } else if (KoLCharacter.inNoobcore()) {
        limit = KoLCharacter.getAbsorbsLimit();
        this.statusLabel[count].setText("   Abs: ");
        this.statusValueLabel[count].setText(KoLCharacter.getAbsorbs() + " / " + limit);
        count++;
      } else if (KoLCharacter.isPlumber()) {
        this.statusLabel[count].setText("    PP: ");
        this.statusValueLabel[count].setText(
            KoLConstants.COMMA_FORMAT.format(KoLCharacter.getCurrentPP())
                + " / "
                + KoLConstants.COMMA_FORMAT.format(KoLCharacter.getMaximumPP()));
        count++;
      } else if (KoLCharacter.inRobocore()) {
        this.statusLabel[count].setText(" Energy: ");
        this.statusValueLabel[count].setText(
            KoLConstants.COMMA_FORMAT.format(KoLCharacter.getYouRobotEnergy()));
        count++;
        this.statusLabel[count].setText(" Scraps: ");
        this.statusValueLabel[count].setText(
            KoLConstants.COMMA_FORMAT.format(KoLCharacter.getYouRobotScraps()));
        count++;
      } else if (KoLCharacter.inFirecore()) {
        this.statusLabel[count].setText(" Water: ");
        this.statusValueLabel[count].setText(
            KoLConstants.COMMA_FORMAT.format(KoLCharacter.getWildfireWater()));
        count++;
      }

      // Classes
      if (KoLCharacter.getFuryLimit() > 0) {
        this.statusLabel[count].setText("  Fury: ");
        this.statusValueLabel[count].setText(
            KoLCharacter.getFury() + " / " + KoLCharacter.getFuryLimit());
        count++;
      } else if (KoLCharacter.getClassType().equals(KoLCharacter.SAUCEROR)
          && !KoLCharacter.inNuclearAutumn()) {
        this.statusLabel[count].setText("Soulsauce: ");
        this.statusValueLabel[count].setText(KoLCharacter.getSoulsauce() + " / 100");
        count++;
      } else if (KoLCharacter.getClassType().equals(KoLCharacter.DISCO_BANDIT)
          && !KoLCharacter.inNuclearAutumn()) {
        this.statusLabel[count].setText(" Disco: ");
        this.statusValueLabel[count].setText(KoLCharacter.getDiscoMomentum() + " / 3");
        count++;
      }
      this.statusLabel[count].setText("  Meat: ");
      this.statusValueLabel[count].setText(
          KoLConstants.COMMA_FORMAT.format(KoLCharacter.getAvailableMeat()));
      this.statusValueLabel[count].setToolTipText(
          "Closet: " + KoLConstants.COMMA_FORMAT.format(KoLCharacter.getClosetMeat()));
      count++;
      if (KoLCharacter.getHippyStoneBroken()) {
        this.statusLabel[count].setText("   PvP: ");
        this.statusValueLabel[count].setText(String.valueOf(KoLCharacter.getAttacksLeft()));
        count++;
      }
      this.statusLabel[count].setText("   Adv: ");
      this.statusValueLabel[count].setText(String.valueOf(KoLCharacter.getAdventuresLeft()));
      count++;
    } else {
      this.statusLabel[count].setText("  Gold: ");
      this.statusValueLabel[count].setText(String.valueOf(SpelunkyRequest.getGold()));
      count++;
      this.statusLabel[count].setText("  Bomb: ");
      this.statusValueLabel[count].setText(String.valueOf(SpelunkyRequest.getBomb()));
      count++;
      this.statusLabel[count].setText("  Rope: ");
      this.statusValueLabel[count].setText(String.valueOf(SpelunkyRequest.getRope()));
      count++;
      this.statusLabel[count].setText("   Key: ");
      this.statusValueLabel[count].setText(String.valueOf(SpelunkyRequest.getKey()));
      count++;
      this.statusLabel[count].setText(" Turns: ");
      this.statusValueLabel[count].setText(String.valueOf(SpelunkyRequest.getTurnsLeft()));
      count++;
    }
    for (int i = count; i < STATUS_LABELS; i++) {
      this.statusLabel[i].setText("");
      this.statusValueLabel[i].setText("");
    }

    count = 0;
    if (limitmode != Limitmode.SPELUNKY) {
      // Remove this if/when KoL supports Water Level effect on Oil Peak/Tavern
      if (KoLCharacter.inRaincore()) {
        this.bonusLabel[count].setText("    ML: ");
        this.bonusValueLabel[count].setText(
            KoLConstants.MODIFIER_FORMAT.format(KoLCharacter.getMonsterLevelAdjustment())
                + " ("
                + KoLConstants.MODIFIER_FORMAT.format(
                    KoLCharacter.currentNumericModifier(Modifiers.MONSTER_LEVEL))
                + ")");
        count++;
      } else {
        this.bonusLabel[count].setText("    ML: ");
        this.bonusValueLabel[count].setText(
            KoLConstants.MODIFIER_FORMAT.format(KoLCharacter.getMonsterLevelAdjustment()));
        count++;
      }
      this.bonusLabel[count].setText("   Enc: ");
      this.bonusValueLabel[count].setText(
          KoLConstants.ROUNDED_MODIFIER_FORMAT.format(KoLCharacter.getCombatRateAdjustment())
              + "%");
      count++;
      this.bonusLabel[count].setText("  Init: ");
      this.bonusValueLabel[count].setText(
          KoLConstants.ROUNDED_MODIFIER_FORMAT.format(KoLCharacter.getInitiativeAdjustment())
              + "%");
      count++;
      this.bonusLabel[count].setText("   Exp: ");
      this.bonusValueLabel[count].setText(
          KoLConstants.ROUNDED_MODIFIER_FORMAT.format(KoLCharacter.getExperienceAdjustment()));
      count++;
      this.bonusLabel[count].setText("  Meat: ");
      this.bonusValueLabel[count].setText(
          KoLConstants.ROUNDED_MODIFIER_FORMAT.format(KoLCharacter.getMeatDropPercentAdjustment())
              + "%");
      count++;
      this.bonusLabel[count].setText("  Item: ");
      this.bonusValueLabel[count].setText(
          KoLConstants.ROUNDED_MODIFIER_FORMAT.format(KoLCharacter.getItemDropPercentAdjustment())
              + "%");
      count++;
      int hobo = KoLCharacter.getHoboPower();
      if (hobo != 0 && count < this.BONUS_LABELS) {
        this.bonusLabel[count].setText("Hobo: ");
        this.bonusValueLabel[count].setText(KoLConstants.MODIFIER_FORMAT.format(hobo));
        count++;
      }
      int smithsness = KoLCharacter.getSmithsness();
      if (smithsness != 0 && count < this.BONUS_LABELS) {
        this.bonusLabel[count].setText("Smithsness: ");
        this.bonusValueLabel[count].setText(KoLConstants.MODIFIER_FORMAT.format(smithsness));
        count++;
      }
      int surgeon = (int) KoLCharacter.currentNumericModifier(Modifiers.SURGEONOSITY);
      if (surgeon != 0 && count < this.BONUS_LABELS) {
        this.bonusLabel[count].setText("Surgeon: ");
        this.bonusValueLabel[count].setText(surgeon + " / 5");
        count++;
      }
      int rave = KoLCharacter.currentBitmapModifier(Modifiers.RAVEOSITY);
      if (rave != 0 && count < this.BONUS_LABELS) {
        this.bonusLabel[count].setText("Rave: ");
        this.bonusValueLabel[count].setText(rave + " / 7");
        count++;
      }
      int clown = KoLCharacter.getClownosity();
      if (clown != 0 && count < this.BONUS_LABELS) {
        this.bonusLabel[count].setText("Clown: ");
        this.bonusValueLabel[count].setText(clown + " / 4");
        count++;
      }
    } else {
      this.bonusLabel[count].setText("DR: ");
      this.bonusValueLabel[count].setText(
          String.valueOf((int) KoLCharacter.currentNumericModifier(Modifiers.DAMAGE_REDUCTION)));
      count++;
      this.bonusLabel[count].setText("Luck: ");
      this.bonusValueLabel[count].setText(
          String.valueOf((int) KoLCharacter.currentNumericModifier(Modifiers.LUCK)));
      count++;
    }

    for (int i = count; i < BONUS_LABELS; i++) {
      this.bonusLabel[i].setText("");
      this.bonusValueLabel[i].setText("");
    }

    try {
      String popText = CompactSidePane.modifierPopupText();
      this.modPopLabel.setText(popText);
    } catch (Exception e) {
      // Ignore errors - there seems to be a Java bug that
      // occasionally gets triggered during the setText().
    }

    if (limitmode != Limitmode.SPELUNKY) {
      long currentLevel = KoLCharacter.calculateLastLevel();
      long nextLevel = KoLCharacter.calculateNextLevel();
      long totalPrime = KoLCharacter.getTotalPrime();
      this.levelMeter.setMaximum((int) (nextLevel - currentLevel));
      this.levelMeter.setValue((int) (totalPrime - currentLevel));
      this.levelMeter.setString(" ");
      this.levelPanel.setToolTipText(
          "<html>&nbsp;&nbsp;"
              + KoLCharacter.getAdvancement()
              + "&nbsp;&nbsp;<br>&nbsp;&nbsp;("
              + KoLConstants.COMMA_FORMAT.format(nextLevel - totalPrime)
              + " subpoints needed)&nbsp;&nbsp;</html>");
    } else {
      this.levelMeter.setMaximum(1);
      this.levelMeter.setValue(1);
      this.levelMeter.setString(" ");
      this.levelPanel.setToolTipText("");
    }

    if (limitmode == Limitmode.SPELUNKY) {
      String imageName = SpelunkyRequest.getBuddyImageName();
      if (imageName == null) {
        this.familiarLabel.setNoIcon();
        return;
      }

      this.familiarLabel.setIcon(imageName, "otherimages/");
      this.familiarLabel.setText(SpelunkyRequest.getBuddyName());
    } else if (KoLCharacter.inAxecore()) {
      AdventureResult item = KoLCharacter.getCurrentInstrument();
      if (item == null) {
        this.familiarLabel.setNoIcon();
        return;
      }

      this.familiarLabel.setIcon(ItemDatabase.getItemImageLocation(item.getItemId()));
      this.familiarLabel.setText("Level " + KoLCharacter.getMinstrelLevel());
    } else if (KoLCharacter.isJarlsberg()) {
      Companion companion = KoLCharacter.getCompanion();
      if (companion == null) {
        this.familiarLabel.setNoIcon();
        return;
      }

      this.familiarLabel.setIcon(companion.imageName());
    } else if (KoLCharacter.isSneakyPete()) {
      this.familiarLabel.setIcon("motorbike.gif");

      String popText = CompactSidePane.motorcyclePopupText();
      try {
        this.motPopLabel.setText(popText);
      } catch (Exception e) {
        // Ignore errors - there seems to be a Java bug that
        // occasionally gets triggered during the setText().
      }
    } else if (KoLCharacter.isEd()) {
      EdServantData servant = EdServantData.currentServant();
      if (servant == null) {
        this.familiarLabel.setNoIcon();
        return;
      }

      this.familiarLabel.setIcon(servant.getImage());
      this.familiarLabel.setText("<HTML><center>level " + servant.getLevel() + "</center></HTML>");
    } else if (KoLCharacter.isVampyre()) {
      MonsterData ensorcelee = MonsterDatabase.findMonster(Preferences.getString("ensorcelee"));

      if (ensorcelee == null) {
        this.familiarLabel.setNoIcon();
        return;
      }

      this.familiarLabel.setToolTipText(ensorcelee.toString());
      this.familiarLabel.setIcon(ensorcelee.getPhylum().getImage());
      this.familiarLabel.setText(
          "<HTML><center>level "
              + Preferences.getInteger("ensorceleeLevel")
              + " "
              + ensorcelee.getPhylum().toString()
              + "</center></HTML>");
    } else {
      this.familiarLabel.update();
    }

    quantumFamiliarPanel.setVisible(KoLCharacter.inQuantum());
  }

  private class FamiliarLabel extends JLabel implements Listener {
    private final ImageIcon noFamiliarImage = FamiliarDatabase.getNoFamiliarImage();

    public FamiliarLabel() {
      super(" ", null, SwingConstants.CENTER);
      // this.setForeground( Color.BLACK );
      this.setVerticalTextPosition(JLabel.BOTTOM);
      this.setHorizontalTextPosition(JLabel.CENTER);
      this.addMouseListener(new FamPopListener());

      NamedListenerRegistry.registerNamedListener("(familiar image)", this);
    }

    private void setNoIcon() {
      this.setIcon(this.noFamiliarImage);
      this.setText("");
    }

    public void setIcon(final String path) {
      String prefix = "itemimages/";
      String image = path;
      int slash = path.indexOf("/");
      if (slash != -1) {
        // includes slash
        prefix = path.substring(0, slash + 1);
        image = path.substring(slash + 1);
      }
      this.setIcon(image, prefix);
    }

    public void setIcon(final String image, final String prefix) {
      if (image == null) {
        this.setNoIcon();
        return;
      }

      String path = prefix + image;
      FileUtilities.downloadImage(KoLmafia.imageServerPath() + path);
      ImageIcon icon = JComponentUtilities.getImage(path);
      if (icon == null) {
        this.setNoIcon();
        return;
      }
      super.setIcon(icon);
      icon.setImageObserver(this);
    }

    public void update() {
      FamiliarData current = KoLCharacter.getFamiliar();
      FamiliarData effective = KoLCharacter.getEffectiveFamiliar();
      int id = effective == null ? -1 : effective.getId();

      if (id == -1) {
        this.setToolTipText("");
        this.setNoIcon();
        return;
      }

      this.setToolTipText(effective.getRace());
      this.setIcon(KoLCharacter.getFamiliarImage());

      StringBuffer anno = CharPaneDecorator.getFamiliarAnnotation();
      int weight = current.getModifiedWeight();
      this.setText(
          "<HTML><center>"
              + weight
              + (weight == 1 ? " lb." : " lbs.")
              + (anno == null ? "" : "<br>" + anno.toString())
              + "</center></HTML>");
    }
  }

  private class QuantumFamiliarPanel extends JPanel {
    QuantumFamiliarLabel quantumFamiliarLabel = null;

    public QuantumFamiliarPanel() {
      super(new FlowLayout());
      this.setOpaque(false);
      this.setVisible(false);
    }

    @Override
    public void setVisible(final boolean visible) {
      super.setVisible(visible);

      if (visible) {
        if (this.quantumFamiliarLabel == null) {
          quantumFamiliarPanel.add(this.quantumFamiliarLabel = new QuantumFamiliarLabel());
        }

        this.quantumFamiliarLabel.update();
      }
    }
  }

  private class QuantumFamiliarLabel extends FamiliarLabel {
    public QuantumFamiliarLabel() {
      this.setHorizontalTextPosition(JLabel.RIGHT);
      this.setVerticalTextPosition(JLabel.CENTER);
    }

    private String familiar = "none";

    @Override
    public void update() {
      int turns = Preferences.getInteger("nextQuantumFamiliarTurn") - KoLCharacter.getTurnsPlayed();

      if (turns == 0) {
        this.setIcon((Icon) null);
        this.setText("<html><center>checking...</center></html>");
        return;
      }

      this.setText("<html><center>in " + turns + " turns</center></html>");

      String nextFamiliarRace = Preferences.getString("nextQuantumFamiliar");

      if (nextFamiliarRace == familiar) {
        return;
      }

      familiar = nextFamiliarRace;

      this.setToolTipText(nextFamiliarRace);

      ImageIcon icon = FamiliarDatabase.getFamiliarImage(nextFamiliarRace);
      ImageIcon scaled =
          new ImageIcon(icon.getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));
      this.setIcon(scaled);
    }
  }

  private static String motorcyclePopupText() {
    String tires = Preferences.getString("peteMotorbikeTires");
    String gasTank = Preferences.getString("peteMotorbikeGasTank");
    String headlight = Preferences.getString("peteMotorbikeHeadlight");
    String cowling = Preferences.getString("peteMotorbikeCowling");
    String muffler = Preferences.getString("peteMotorbikeMuffler");
    String seat = Preferences.getString("peteMotorbikeSeat");

    return "<html><table border=1>"
        + "<tr><td>Tires</td><td>"
        + tires
        + "</td></tr>"
        + "<tr><td>Gas Tank</td><td>"
        + gasTank
        + "</td></tr>"
        + "<tr><td>Headlight</td><td>"
        + headlight
        + "</td></tr>"
        + "<tr><td>Cowling</td><td>"
        + cowling
        + "</td></tr>"
        + "<tr><td>Muffler</td><td>"
        + muffler
        + "</td></tr>"
        + "<tr><td>Seat</td><td>"
        + seat
        + "</td></tr>"
        + "</table></html>";
  }

  private static String modifierPopupText() {
    StringBuffer buf = new StringBuffer("<html><body><table border=1>");
    int[] predicted = KoLCharacter.getCurrentModifiers().predict();
    int mus = Math.max(1, predicted[Modifiers.BUFFED_MUS]);
    int mys = Math.max(1, predicted[Modifiers.BUFFED_MYS]);
    int mox = Math.max(1, predicted[Modifiers.BUFFED_MOX]);
    int dmus = KoLCharacter.getAdjustedMuscle() - mus;
    int dmys = KoLCharacter.getAdjustedMysticality() - mys;
    int dmox = KoLCharacter.getAdjustedMoxie() - mox;
    if (dmus != 0 || dmys != 0 || dmox != 0) {
      buf.append("<tr><td colspan=4>Predicted: Mus ");
      buf.append(mus);
      buf.append(" (");
      buf.append(KoLConstants.MODIFIER_FORMAT.format(dmus));
      buf.append("), Mys ");
      buf.append(mys);
      buf.append(" (");
      buf.append(KoLConstants.MODIFIER_FORMAT.format(dmys));
      buf.append("), Mox ");
      buf.append(mox);
      buf.append(" (");
      buf.append(KoLConstants.MODIFIER_FORMAT.format(dmox));
      buf.append(")</td></tr>");
    }
    long hp = Math.max(1, predicted[Modifiers.BUFFED_HP]);
    long mp = Math.max(1, predicted[Modifiers.BUFFED_MP]);
    long dhp = KoLCharacter.getMaximumHP() - hp;
    long dmp = KoLCharacter.getMaximumMP() - mp;
    if (dhp != 0 || dmp != 0) {
      buf.append("<tr><td colspan=4>Predicted: Max HP ");
      buf.append(hp);
      buf.append(" (");
      buf.append(KoLConstants.MODIFIER_FORMAT.format(dhp));
      buf.append("), Max MP ");
      buf.append(mp);
      buf.append(" (");
      buf.append(KoLConstants.MODIFIER_FORMAT.format(dmp));
      buf.append(")</td></tr>");
    }

    buf.append("<tr><td></td><td>Damage</td><td>Spell dmg</td><td>Resistance</td></tr>");
    CompactSidePane.addElement(buf, "Hot", Modifiers.HOT_DAMAGE);
    CompactSidePane.addElement(buf, "Cold", Modifiers.COLD_DAMAGE);
    CompactSidePane.addElement(buf, "Stench", Modifiers.STENCH_DAMAGE);
    CompactSidePane.addElement(buf, "Spooky", Modifiers.SPOOKY_DAMAGE);
    CompactSidePane.addElement(buf, "Sleaze", Modifiers.SLEAZE_DAMAGE);
    CompactSidePane.addSlime(buf);
    CompactSidePane.addSupercold(buf);
    buf.append("<tr><td>Weapon</td><td>");
    buf.append(
        KoLConstants.MODIFIER_FORMAT.format(
            KoLCharacter.currentNumericModifier(Modifiers.WEAPON_DAMAGE)));
    buf.append("<br>");
    buf.append(
        KoLConstants.MODIFIER_FORMAT.format(
            KoLCharacter.currentNumericModifier(Modifiers.WEAPON_DAMAGE_PCT)));
    buf.append("%</td><td rowspan=2>General<br>spell dmg:<br>");
    buf.append(
        KoLConstants.MODIFIER_FORMAT.format(
            KoLCharacter.currentNumericModifier(Modifiers.SPELL_DAMAGE)));
    buf.append("<br>");
    buf.append(
        KoLConstants.MODIFIER_FORMAT.format(
            KoLCharacter.currentNumericModifier(Modifiers.SPELL_DAMAGE_PCT)));
    buf.append("%</td><td rowspan=2>DA: ");
    buf.append(KoLConstants.COMMA_FORMAT.format(KoLCharacter.getDamageAbsorption()));
    buf.append("<br>(");
    buf.append(
        KoLConstants.ROUNDED_MODIFIER_FORMAT.format(
            Math.max(
                0.0,
                (Math.sqrt(Math.min(10000.0, KoLCharacter.getDamageAbsorption() * 10.0)) - 10.0))));
    buf.append("%)<br>DR: ");
    buf.append(KoLConstants.MODIFIER_FORMAT.format(KoLCharacter.getDamageReduction()));
    buf.append("</td></tr><tr><td>Ranged</td><td>");
    buf.append(
        KoLConstants.MODIFIER_FORMAT.format(
            KoLCharacter.currentNumericModifier(Modifiers.RANGED_DAMAGE)));
    buf.append("<br>");
    buf.append(
        KoLConstants.MODIFIER_FORMAT.format(
            KoLCharacter.currentNumericModifier(Modifiers.RANGED_DAMAGE_PCT)));
    buf.append("%</td></tr><tr><td>Critical</td><td>");
    buf.append(
        KoLConstants.MODIFIER_FORMAT.format(
            KoLCharacter.currentNumericModifier(Modifiers.CRITICAL_PCT)));
    buf.append("%</td><td rowspan=2>MP cost:<br>");
    buf.append(KoLConstants.MODIFIER_FORMAT.format(KoLCharacter.getManaCostAdjustment()));
    int hpmin = (int) KoLCharacter.currentNumericModifier(Modifiers.HP_REGEN_MIN);
    int hpmax = (int) KoLCharacter.currentNumericModifier(Modifiers.HP_REGEN_MAX);
    int mpmin = (int) KoLCharacter.currentNumericModifier(Modifiers.MP_REGEN_MIN);
    int mpmax = (int) KoLCharacter.currentNumericModifier(Modifiers.MP_REGEN_MAX);
    if (hpmax != 0 || mpmax != 0) {
      buf.append("<br>Regenerate:<br>HP ");
      buf.append(hpmin);
      if (hpmin != hpmax) {
        buf.append("-");
        buf.append(hpmax);
      }
      buf.append("<br>MP ");
      buf.append(mpmin);
      if (mpmin != mpmax) {
        buf.append("-");
        buf.append(mpmax);
      }
    }
    buf.append("</td><td rowspan=2>Rollover:<br>Adv ");
    buf.append(
        KoLConstants.MODIFIER_FORMAT.format(
            KoLCharacter.currentNumericModifier(Modifiers.ADVENTURES)
                + Preferences.getInteger("extraRolloverAdventures")));
    buf.append("<br>PvP ");
    buf.append(
        KoLConstants.MODIFIER_FORMAT.format(
            KoLCharacter.currentNumericModifier(Modifiers.PVP_FIGHTS)));
    buf.append("<br>HP ~");
    buf.append(KoLCharacter.getRestingHP());
    buf.append("<br>MP ");
    buf.append(KoLCharacter.getRestingMP());
    buf.append("</td></tr><tr><td>Fumble</td><td>");
    if (KoLConstants.activeEffects.contains(CompactSidePane.CLUMSY)) {
      buf.append("always");
    } else if (KoLCharacter.currentBooleanModifier(Modifiers.NEVER_FUMBLE)) {
      buf.append("never");
    } else {
      buf.append(
          KoLConstants.MODIFIER_FORMAT.format(
              KoLCharacter.currentNumericModifier(Modifiers.FUMBLE)));
      buf.append(" X");
    }
    buf.append("</td></tr>");
    double food = KoLCharacter.currentNumericModifier(Modifiers.FOODDROP);
    double booze = KoLCharacter.currentNumericModifier(Modifiers.BOOZEDROP);
    double candy = KoLCharacter.currentNumericModifier(Modifiers.CANDYDROP);
    double hat = KoLCharacter.currentNumericModifier(Modifiers.HATDROP);
    double weapon = KoLCharacter.currentNumericModifier(Modifiers.WEAPONDROP);
    double offhand = KoLCharacter.currentNumericModifier(Modifiers.OFFHANDDROP);
    double shirt = KoLCharacter.currentNumericModifier(Modifiers.SHIRTDROP);
    double pants = KoLCharacter.currentNumericModifier(Modifiers.PANTSDROP);
    double acc = KoLCharacter.currentNumericModifier(Modifiers.ACCESSORYDROP);
    if (food != 0
        || booze != 0
        || hat != 0
        || weapon != 0
        || offhand != 0
        || shirt != 0
        || pants != 0
        || acc != 0) {
      buf.append("<tr><td colspan=4>Special drops:");
      if (food != 0) {
        buf.append(" Food ");
        buf.append(KoLConstants.MODIFIER_FORMAT.format(food));
        buf.append('%');
      }
      if (booze != 0) {
        buf.append(" Booze ");
        buf.append(KoLConstants.MODIFIER_FORMAT.format(booze));
        buf.append('%');
      }
      if (candy != 0) {
        buf.append(" Candy ");
        buf.append(KoLConstants.MODIFIER_FORMAT.format(candy));
        buf.append('%');
      }
      if (hat != 0) {
        buf.append(" Hat ");
        buf.append(KoLConstants.MODIFIER_FORMAT.format(hat));
        buf.append('%');
      }
      if (weapon != 0) {
        buf.append(" Weapon ");
        buf.append(KoLConstants.MODIFIER_FORMAT.format(weapon));
        buf.append('%');
      }
      if (offhand != 0) {
        buf.append(" Offhand ");
        buf.append(KoLConstants.MODIFIER_FORMAT.format(offhand));
        buf.append('%');
      }
      if (shirt != 0) {
        buf.append(" Shirt ");
        buf.append(KoLConstants.MODIFIER_FORMAT.format(shirt));
        buf.append('%');
      }
      if (pants != 0) {
        buf.append(" Pants ");
        buf.append(KoLConstants.MODIFIER_FORMAT.format(pants));
        buf.append('%');
      }
      if (acc != 0) {
        buf.append(" Accessory ");
        buf.append(KoLConstants.MODIFIER_FORMAT.format(acc));
        buf.append('%');
      }
      buf.append("</td></tr>");
    }
    buf.append("</table></body></html>");

    return buf.toString();
  }

  private static void addElement(StringBuffer buf, String name, int dmgModifier) {
    double wdmg = KoLCharacter.currentNumericModifier(dmgModifier);
    double sdmg =
        KoLCharacter.currentNumericModifier(
            dmgModifier - Modifiers.COLD_DAMAGE + Modifiers.COLD_SPELL_DAMAGE);
    int resist =
        (int)
            KoLCharacter.currentNumericModifier(
                dmgModifier - Modifiers.COLD_DAMAGE + Modifiers.COLD_RESISTANCE);
    if (wdmg == 0.0 && sdmg == 0.0 && resist == 0) {
      return; // skip this row entirely, it's all zeros
    }
    buf.append("<tr><td>");
    buf.append(name);
    buf.append("</td><td>");
    buf.append(KoLConstants.MODIFIER_FORMAT.format(wdmg));
    buf.append("</td><td>");
    buf.append(KoLConstants.MODIFIER_FORMAT.format(sdmg));
    buf.append("</td><td>");
    buf.append(KoLConstants.MODIFIER_FORMAT.format(resist));
    buf.append(" (");
    buf.append(
        KoLConstants.ROUNDED_MODIFIER_FORMAT.format(
            KoLCharacter.elementalResistanceByLevel(resist)));
    buf.append("%)</td></tr>");
  }

  private static void addSlime(StringBuffer buf) {
    int resist = (int) KoLCharacter.currentNumericModifier(Modifiers.SLIME_RESISTANCE);
    double percent = KoLCharacter.elementalResistanceByLevel(resist, false);
    int turns = CompactSidePane.SLIMED.getCount(KoLConstants.activeEffects);
    if (resist == 0 && turns == 0) {
      return; // skip this row entirely, it's all zeros
    }
    buf.append("<tr><td>Slime</td><td colspan=2>");
    if (turns > 0) {
      buf.append("Expected dmg ");
      buf.append(
          KoLConstants.COMMA_FORMAT.format(
              Math.ceil(
                  Math.pow(Math.max(0, 11 - turns), 2.727)
                      * (100.0 - percent)
                      * KoLCharacter.getMaximumHP()
                      / 10000.0)));
    }
    buf.append("</td><td>");
    buf.append(KoLConstants.MODIFIER_FORMAT.format(resist));
    buf.append(" (");
    buf.append(KoLConstants.ROUNDED_MODIFIER_FORMAT.format(percent));
    buf.append("%)</td></tr>");
  }

  private static void addSupercold(StringBuffer buf) {
    int resist = (int) KoLCharacter.currentNumericModifier(Modifiers.SUPERCOLD_RESISTANCE);
    double percent = KoLCharacter.elementalResistanceByLevel(resist, false);
    if (resist == 0) {
      return; // skip this row entirely, it's all zeros
    }
    buf.append("<tr><td>Supercold</td><td colspan=2>");
    buf.append("</td><td>");
    buf.append(KoLConstants.MODIFIER_FORMAT.format(resist));
    buf.append(" (");
    buf.append(KoLConstants.ROUNDED_MODIFIER_FORMAT.format(percent));
    buf.append("%)</td></tr>");
  }
}
