package net.sourceforge.kolmafia.swingui.panel;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceAdventures;
import net.sourceforge.kolmafia.session.ChoiceAdventures.ChoiceAdventure;
import net.sourceforge.kolmafia.session.ChoiceAdventures.Option;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.OceanManager.Destination;
import net.sourceforge.kolmafia.session.OceanManager.Point;
import net.sourceforge.kolmafia.session.VioletFogManager;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.widget.EditableAutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.textui.command.GongCommand;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

/**
 * This panel allows the user to select which item they would like to do for each of the different
 * choice adventures.
 */
public class ChoiceOptionsPanel extends JTabbedPane implements Listener {
  private final TreeMap<String, ArrayList> choiceMap;
  private final HashMap<String, ArrayList<JComponent>> selectMap;
  private final CardLayout choiceCards;
  private final JPanel choicePanel;

  private final List<JComboBox<Object>> optionSelects;

  private final JComboBox<String> palindomePapayaSelect;
  private final JComboBox<String> spookyForestSelect;
  private final JComboBox<String> violetFogSelect;
  private final JComboBox<String> maidenSelect;
  private final JComboBox<String> louvreSelect;
  private final JComboBox<String> manualLouvre;
  private final JComboBox<String> riseSelect, fallSelect;
  private final JComboBox<String> lightsOutSelect;
  private final OceanDestinationComboBox oceanDestSelect;
  private final JComboBox<String> oceanActionSelect;
  private final JComboBox<String> darkAtticSelect;
  private final JComboBox<String> unlivingRoomSelect;
  private final JComboBox<String> debasementSelect;
  private final JComboBox<String> propDeportmentSelect;
  private final JComboBox<String> reloadedSelect;
  private final JComboBox<String> sororityGuideSelect;
  private final ShrineComboBox hiddenShrineNWSelect;
  private final ShrineComboBox hiddenShrineSWSelect;
  private final ShrineComboBox hiddenShrineNESelect;
  private final ShrineComboBox hiddenShrineSESelect;
  private final JComboBox<String> hiddenApartmentSelect;
  private final JComboBox<String> hiddenHospitalSelect;
  private final JComboBox<String> hiddenParkSelect;
  private final JComboBox<String> hiddenBowlingAlleySelect;
  private final JComboBox<String> hiddenOfficeSelect;
  private final JComboBox<String> massiveZigguratSelect;
  private final JComboBox<String> gongSelect;
  private final JComboBox<String> kolhsCafeteriaSelect;
  private final JComboBox<String> dailyDungeonDoorSelect;
  private final JComboBox<String> basementMallSelect;
  private final JComboBox<String> breakableSelect;
  private final JComboBox<String> addingSelect;
  private final JComboBox<String> paranormalLabSelect;
  private final JComboBox<String> containmentSelect;

  /** Constructs a new <code>ChoiceOptionsPanel</code>. */
  public ChoiceOptionsPanel() {
    super(JTabbedPane.LEFT);
    this.choiceCards = new CardLayout(10, 10);

    this.choiceMap = new TreeMap<>();
    this.selectMap = new HashMap<>();

    this.choicePanel = new JPanel(this.choiceCards);
    this.choicePanel.add(new JPanel(), "");
    this.addTab("Zone", new GenericScrollPane(this.choicePanel));
    this.setToolTipTextAt(0, "Choices specific to the current adventure zone");

    this.optionSelects = new ArrayList<>(ChoiceAdventures.CHOICE_ADVS.length);
    for (int i = 0; i < ChoiceAdventures.CHOICE_ADVS.length; ++i) {
      this.optionSelects.add(new JComboBox<>());
      this.optionSelects.get(i).addItem("show in browser");
      Option[] options = ChoiceAdventures.CHOICE_ADVS[i].getOptions();
      for (int j = 0; j < options.length; ++j) {
        this.optionSelects.get(i).addItem(options[j]);
      }
    }

    this.palindomePapayaSelect = new JComboBox<>();
    this.palindomePapayaSelect.addItem("3 papayas");
    this.palindomePapayaSelect.addItem("Trade papayas for stats");
    this.palindomePapayaSelect.addItem("Fewer stats");
    this.palindomePapayaSelect.addItem("Stats until out of papayas then papayas");
    this.palindomePapayaSelect.addItem("Stats until out of papayas then fewer stats");

    this.spookyForestSelect = new JComboBox<>();
    this.spookyForestSelect.addItem("show in browser");
    this.spookyForestSelect.addItem("mosquito larva or spooky mushrooms");
    this.spookyForestSelect.addItem("Spooky-Gro fertilizer");
    this.spookyForestSelect.addItem("spooky sapling & sell bar skins");
    this.spookyForestSelect.addItem("Spooky Temple map then skip adventure");
    this.spookyForestSelect.addItem("meet vampire hunter");
    this.spookyForestSelect.addItem("meet vampire");
    this.spookyForestSelect.addItem("gain meat");
    this.spookyForestSelect.addItem("loot Seal Clubber corpse");
    this.spookyForestSelect.addItem("loot Turtle Tamer corpse");
    this.spookyForestSelect.addItem("loot Pastamancer corpse");
    this.spookyForestSelect.addItem("loot Sauceror corpse");
    this.spookyForestSelect.addItem("loot Disco Bandit corpse");
    this.spookyForestSelect.addItem("loot Accordion Thief corpse");

    this.violetFogSelect = new JComboBox<>();
    for (int i = 0; i < VioletFogManager.FogGoals.length; ++i) {
      this.violetFogSelect.addItem(VioletFogManager.FogGoals[i]);
    }

    this.louvreSelect = new JComboBox<>();
    this.louvreSelect.addItem("Ignore this adventure");
    for (int i = 0; i < LouvreManager.LouvreGoals.length; ++i) {
      this.louvreSelect.addItem(LouvreManager.LouvreGoals[i]);
    }

    this.louvreSelect.addItem("Boost Prime Stat");
    this.louvreSelect.addItem("Boost Lowest Stat");

    LockableListModel<String> overrideList = new LockableListModel<>();

    this.manualLouvre = new EditableAutoFilterComboBox(overrideList);
    overrideList.add("Use specified goal");

    for (int i = 1; i <= 3; ++i) {
      for (int j = 1; j <= 3; ++j) {
        for (int k = 1; k <= 3; ++k) {
          overrideList.add(
              this.getLouvreDirection(i)
                  + ", "
                  + this.getLouvreDirection(j)
                  + ", "
                  + this.getLouvreDirection(k));
        }
      }
    }

    String overrideSetting = Preferences.getString("louvreOverride");
    if (!overrideSetting.equals("") && !overrideList.contains(overrideSetting)) {
      overrideList.add(1, overrideSetting);
    }

    this.maidenSelect = new JComboBox<>();
    this.maidenSelect.addItem("Ignore this adventure");
    this.maidenSelect.addItem("Fight a random knight");
    this.maidenSelect.addItem("Only fight the wolf knight");
    this.maidenSelect.addItem("Only fight the snake knight");
    this.maidenSelect.addItem("Maidens, then fight a random knight");
    this.maidenSelect.addItem("Maidens, then fight the wolf knight");
    this.maidenSelect.addItem("Maidens, then fight the snake knight");

    this.riseSelect = new JComboBox<>();
    this.riseSelect.addItem("ignore this adventure");
    this.riseSelect.addItem("boost mysticality substats");
    this.riseSelect.addItem("boost moxie substats");
    this.riseSelect.addItem("acquire mysticality skill");

    this.fallSelect = new JComboBox<>();
    this.fallSelect.addItem("ignore this adventure");
    this.fallSelect.addItem("boost muscle substats");

    this.lightsOutSelect = new JComboBox<>();
    this.lightsOutSelect.addItem("show in browser");
    this.lightsOutSelect.addItem("take quest option if available");
    this.lightsOutSelect.addItem("skip adventure");

    this.oceanDestSelect = new OceanDestinationComboBox();

    this.oceanActionSelect = new JComboBox<>();
    this.oceanActionSelect.addItem("continue");
    this.oceanActionSelect.addItem("show");
    this.oceanActionSelect.addItem("stop");
    this.oceanActionSelect.addItem("save and continue");
    this.oceanActionSelect.addItem("save and show");
    this.oceanActionSelect.addItem("save and stop");

    this.darkAtticSelect = new JComboBox<>();
    this.darkAtticSelect.addItem("show in browser");
    this.darkAtticSelect.addItem("staff guides");
    this.darkAtticSelect.addItem("ghost trap");
    this.darkAtticSelect.addItem("mass kill werewolves with silver shotgun shell");
    this.darkAtticSelect.addItem("raise area ML, then staff guides");
    this.darkAtticSelect.addItem("raise area ML, then ghost trap");
    this.darkAtticSelect.addItem("raise area ML, then mass kill werewolves");
    this.darkAtticSelect.addItem("raise area ML, then mass kill werewolves or ghost trap");
    this.darkAtticSelect.addItem("lower area ML, then staff guides");
    this.darkAtticSelect.addItem("lower area ML, then ghost trap");
    this.darkAtticSelect.addItem("lower area ML, then mass kill werewolves");
    this.darkAtticSelect.addItem("lower area ML, then mass kill werewolves or ghost trap");

    this.unlivingRoomSelect = new JComboBox<>();
    this.unlivingRoomSelect.addItem("show in browser");
    this.unlivingRoomSelect.addItem("mass kill zombies with chainsaw chain");
    this.unlivingRoomSelect.addItem("mass kill skeletons with funhouse mirror");
    this.unlivingRoomSelect.addItem("get costume item");
    this.unlivingRoomSelect.addItem("raise area ML, then mass kill zombies");
    this.unlivingRoomSelect.addItem("raise area ML, then mass kill skeletons");
    this.unlivingRoomSelect.addItem("raise area ML, then mass kill zombies/skeletons");
    this.unlivingRoomSelect.addItem("raise area ML, then get costume item");
    this.unlivingRoomSelect.addItem("lower area ML, then mass kill zombies");
    this.unlivingRoomSelect.addItem("lower area ML, then mass kill skeletons");
    this.unlivingRoomSelect.addItem("lower area ML, then get costume item");
    this.unlivingRoomSelect.addItem("lower area ML, then mass kill zombies/skeletons");

    this.debasementSelect = new JComboBox<>();
    this.debasementSelect.addItem("show in browser");
    this.debasementSelect.addItem("Prop Deportment");
    this.debasementSelect.addItem("mass kill vampires with plastic vampire fangs");
    this.debasementSelect.addItem("raise area ML, then Prop Deportment");
    this.debasementSelect.addItem("raise area ML, then mass kill vampires");
    this.debasementSelect.addItem("lower area ML, then Prop Deportment");
    this.debasementSelect.addItem("lower area ML, then mass kill vampires");

    this.propDeportmentSelect = new JComboBox<>();
    this.propDeportmentSelect.addItem("show in browser");
    this.propDeportmentSelect.addItem("chainsaw chain");
    this.propDeportmentSelect.addItem("silver item");
    this.propDeportmentSelect.addItem("funhouse mirror");
    this.propDeportmentSelect.addItem("chainsaw/mirror");

    this.reloadedSelect = new JComboBox<>();
    this.reloadedSelect.addItem("show in browser");
    this.reloadedSelect.addItem("melt Maxwell's Silver Hammer");
    this.reloadedSelect.addItem("melt silver tongue charrrm bracelet");
    this.reloadedSelect.addItem("melt silver cheese-slicer");
    this.reloadedSelect.addItem("melt silver shrimp fork");
    this.reloadedSelect.addItem("melt silver paté knife");
    this.reloadedSelect.addItem("don't melt anything");

    this.sororityGuideSelect = new JComboBox<>();
    this.sororityGuideSelect.addItem("show in browser");
    this.sororityGuideSelect.addItem("attic");
    this.sororityGuideSelect.addItem("main floor");
    this.sororityGuideSelect.addItem("basement");

    // Hidden City Non-combats

    this.hiddenShrineNWSelect =
        new ShrineComboBox("choiceAdventure781", "Blessing of Bulbazinalli");
    this.hiddenShrineSWSelect =
        new ShrineComboBox("choiceAdventure783", "Blessing of Squirtlcthulli");
    this.hiddenShrineNESelect =
        new ShrineComboBox("choiceAdventure785", "Blessing of Pikachutlotal");
    this.hiddenShrineSESelect = new ShrineComboBox("choiceAdventure787", "Blessing of Charcoatl");

    this.hiddenApartmentSelect = new JComboBox<>();
    this.hiddenApartmentSelect.addItem("show in browser");
    this.hiddenApartmentSelect.addItem("fight spirit or get cursed");
    this.hiddenApartmentSelect.addItem("banish lawyers or skip adventure");
    this.hiddenApartmentSelect.addItem("skip adventure");

    this.hiddenHospitalSelect = new JComboBox<>();
    this.hiddenHospitalSelect.addItem("show in browser");
    this.hiddenHospitalSelect.addItem("fight spirit");

    this.hiddenParkSelect = new JComboBox<>();
    this.hiddenParkSelect.addItem("show in browser");
    this.hiddenParkSelect.addItem("get random items");
    this.hiddenParkSelect.addItem("relocate pygmy janitors then get random items");
    this.hiddenParkSelect.addItem("skip adventure");

    this.hiddenBowlingAlleySelect = new JComboBox<>();
    this.hiddenBowlingAlleySelect.addItem("show in browser");
    this.hiddenBowlingAlleySelect.addItem("bowl and may fight spirit");

    this.hiddenOfficeSelect = new JComboBox<>();
    this.hiddenOfficeSelect.addItem("show in browser");
    this.hiddenOfficeSelect.addItem("fight spirit or get binder clip or fight accountant");
    this.hiddenOfficeSelect.addItem("fight accountant");
    this.hiddenOfficeSelect.addItem("skip adventure");

    this.massiveZigguratSelect = new JComboBox<>();
    this.massiveZigguratSelect.addItem("show in browser");
    this.massiveZigguratSelect.addItem("fight Protector Spectre");
    this.massiveZigguratSelect.addItem("skip adventure");

    this.kolhsCafeteriaSelect = new JComboBox<>();
    this.kolhsCafeteriaSelect.addItem("show in browser");
    this.kolhsCafeteriaSelect.addItem("get stats if possible else lose hp");

    this.dailyDungeonDoorSelect = new JComboBox<>();
    this.dailyDungeonDoorSelect.addItem("show in browser");
    this.dailyDungeonDoorSelect.addItem("suffer trap effects");
    this.dailyDungeonDoorSelect.addItem("unlock door using PYEC, lockpicks, or skeleton key");
    this.dailyDungeonDoorSelect.addItem("try to avoid trap using highest buffed stat");

    this.gongSelect = new JComboBox<>();
    for (int i = 0; i < GongCommand.GONG_PATHS.length; ++i) {
      this.gongSelect.addItem(GongCommand.GONG_PATHS[i]);
    }

    this.basementMallSelect = new JComboBox<>();
    this.basementMallSelect.addItem("do not show Mall prices");
    this.basementMallSelect.addItem("show Mall prices for items you don't have");
    this.basementMallSelect.addItem("show Mall prices for all items");

    this.breakableSelect = new JComboBox<>();
    this.breakableSelect.addItem("abort on breakage");
    this.breakableSelect.addItem("equip previous");
    this.breakableSelect.addItem("re-equip from inventory, or abort");
    this.breakableSelect.addItem("re-equip from inventory, or previous");
    this.breakableSelect.addItem("acquire & re-equip");

    this.addingSelect = new JComboBox<>();
    this.addingSelect.addItem("show in browser");
    this.addingSelect.addItem("create goal scrolls only");
    this.addingSelect.addItem("create goal & 668 scrolls");
    this.addingSelect.addItem("create goal, 31337, 668 scrolls");

    this.paranormalLabSelect = new JComboBox<>();
    this.paranormalLabSelect.addItem("show in browser");
    this.paranormalLabSelect.addItem("automate");

    this.containmentSelect = new JComboBox<>();
    this.containmentSelect.addItem("show in browser");
    this.containmentSelect.addItem("automate");

    this.addChoiceSelect("Item-Driven", "Llama Gong", this.gongSelect);
    this.addChoiceSelect("Item-Driven", "Breakable Equipment", this.breakableSelect);
    this.addChoiceSelect("Plains", "Papaya War", this.palindomePapayaSelect);
    this.addChoiceSelect("Fernswarthy's Tower", "Fernswarthy's Basement", this.basementMallSelect);
    this.addChoiceSelect("Woods", "Spooky Forest", this.spookyForestSelect);
    this.addChoiceSelect("Astral", "Violet Fog", this.violetFogSelect);
    // This Should be available for all Manor zones
    this.addChoiceSelect("Manor1", "Lights Out", this.lightsOutSelect);
    this.addChoiceSelect("Manor1", "Rise of Spookyraven", this.riseSelect);
    this.addChoiceSelect("Manor1", "Fall of Spookyraven", this.fallSelect);
    this.addChoiceSelect("Manor2", "Louvre Goal", this.louvreSelect);
    this.addChoiceSelect("Manor2", "Louvre Override", this.manualLouvre);
    this.addChoiceSelect("Manor2", "The Maidens", this.maidenSelect);
    this.addChoiceSelect("Pirate", "Ocean Destination", this.oceanDestSelect);
    this.addChoiceSelect("Pirate", "Ocean Action", this.oceanActionSelect);
    this.addChoiceSelect("Mountain", "The Valley of Rof L'm Fao", this.addingSelect);
    this.addChoiceSelect("Events", "Sorority House Attic", this.darkAtticSelect);
    this.addChoiceSelect("Events", "Sorority House Unliving Room", this.unlivingRoomSelect);
    this.addChoiceSelect("Events", "Sorority House Debasement", this.debasementSelect);
    this.addChoiceSelect("Events", "Sorority House Prop Deportment", this.propDeportmentSelect);
    this.addChoiceSelect("Events", "Sorority House Relocked and Reloaded", this.reloadedSelect);
    this.addChoiceSelect("Item-Driven", "Sorority Staff Guide", this.sororityGuideSelect);
    this.addChoiceSelect("HiddenCity", "Shrine NW", this.hiddenShrineNWSelect);
    this.addChoiceSelect("HiddenCity", "Shrine SW", this.hiddenShrineSWSelect);
    this.addChoiceSelect("HiddenCity", "Shrine NE", this.hiddenShrineNESelect);
    this.addChoiceSelect("HiddenCity", "Shrine SE", this.hiddenShrineSESelect);
    this.addChoiceSelect("HiddenCity", "Hidden Apartment", this.hiddenApartmentSelect);
    this.addChoiceSelect("HiddenCity", "Hidden Hospital", this.hiddenHospitalSelect);
    this.addChoiceSelect("HiddenCity", "Hidden Park", this.hiddenParkSelect);
    this.addChoiceSelect("HiddenCity", "Hidden Office", this.hiddenOfficeSelect);
    this.addChoiceSelect("HiddenCity", "Hidden Bowling Alley", this.hiddenBowlingAlleySelect);
    this.addChoiceSelect("HiddenCity", "Massive Ziggurat", this.massiveZigguratSelect);
    this.addChoiceSelect(
        "KOL High School", "Delirium in the Cafeterium", this.kolhsCafeteriaSelect);
    this.addChoiceSelect("Dungeon", "Daily Dungeon: Doors", this.dailyDungeonDoorSelect);
    this.addChoiceSelect("Conspiracy Island", "Paranormal Test Lab", this.paranormalLabSelect);
    this.addChoiceSelect("Conspiracy Island", "Containment Unit", this.containmentSelect);

    for (int i = 0; i < this.optionSelects.size(); ++i) {
      this.addChoiceSelect(
          ChoiceAdventures.CHOICE_ADVS[i].getZone(),
          ChoiceAdventures.CHOICE_ADVS[i].getName(),
          this.optionSelects.get(i));
    }

    this.addChoiceSelect("Item-Driven", "Item", new CommandButton("use 1 llama lama gong"));
    this.addChoiceSelect("Item-Driven", "Item", new CommandButton("use 1 tiny bottle of absinthe"));
    this.addChoiceSelect(
        "Item-Driven", "Item", new CommandButton("use 1 haunted sorority house staff guide"));
    this.addChoiceSelect("Item-Driven", "Item", new CommandButton("use 1 skeleton"));

    PreferenceListenerRegistry.registerPreferenceListener("choiceAdventure*", this);
    PreferenceListenerRegistry.registerPreferenceListener("violetFogGoal", this);
    PreferenceListenerRegistry.registerPreferenceListener("louvreOverride", this);
    PreferenceListenerRegistry.registerPreferenceListener("louvreDesiredGoal", this);
    PreferenceListenerRegistry.registerPreferenceListener("gongPath", this);
    PreferenceListenerRegistry.registerPreferenceListener("oceanAction", this);
    PreferenceListenerRegistry.registerPreferenceListener("oceanDestination", this);
    PreferenceListenerRegistry.registerPreferenceListener("basementMallPrices", this);
    PreferenceListenerRegistry.registerPreferenceListener("breakableHandling", this);
    PreferenceListenerRegistry.registerPreferenceListener("addingScrolls", this);

    this.loadSettings();

    ArrayList optionsList;
    String[] keys = this.choiceMap.keySet().toArray(new String[0]);

    for (int i = 0; i < keys.length; ++i) {
      optionsList = this.choiceMap.get(keys[i]);
      if (keys[i].equals("Item-Driven")) {
        this.addTab("Item", new GenericScrollPane(new ChoicePanel(optionsList)));
        this.setToolTipTextAt(1, "Choices related to the use of an item");
      } else {
        this.choicePanel.add(new ChoicePanel(optionsList), keys[i]);
      }
    }
  }

  public UpdateChoicesListener getUpdateListener() {
    return new UpdateChoicesListener();
  }

  private String getLouvreDirection(final int i) {
    switch (i) {
      case 1:
        return "up";
      case 2:
        return "down";
      default:
        return "side";
    }
  }

  private void addChoiceSelect(final String zone, final String name, final JComponent option) {
    if (zone == null) {
      return;
    }

    if (!this.choiceMap.containsKey(zone)) {
      this.choiceMap.put(zone, new ArrayList<>());
    }

    ArrayList options = this.choiceMap.get(zone);

    if (!options.contains(name)) {
      options.add(name);
      this.selectMap.put(name, new ArrayList<>());
    }

    options = this.selectMap.get(name);
    options.add(option);
  }

  private class ChoicePanel extends GenericPanel {
    public ChoicePanel(final ArrayList options) {
      super(new Dimension(150, 20), new Dimension(300, 20));

      ArrayList<VerifiableElement> elementList = new ArrayList<>();

      for (int i = 0; i < options.size(); ++i) {
        Object key = options.get(i);
        ArrayList<JComponent> value = ChoiceOptionsPanel.this.selectMap.get(key);

        if (value.size() == 1) {
          elementList.add(new VerifiableElement(key + ":  ", value.get(0)));
        } else {
          for (int j = 0; j < value.size(); ++j) {
            elementList.add(new VerifiableElement(key + " " + (j + 1) + ":  ", value.get(j)));
          }
        }
      }

      VerifiableElement[] elements = new VerifiableElement[elementList.size()];
      elementList.toArray(elements);

      this.setContent(elements);
    }

    @Override
    public void actionConfirmed() {
      ChoiceOptionsPanel.this.saveSettings();
    }

    @Override
    public void actionCancelled() {}

    @Override
    public void addStatusLabel() {}

    @Override
    public void setEnabled(final boolean isEnabled) {}
  }

  private class ShrineComboBox extends JComboBox<String> {
    final String setting;

    public ShrineComboBox(final String setting, final String blessing) {
      super();
      this.setting = setting;
      this.addItem("show in browser");
      this.addItem("unlock hidden apartment building or get stone triangle");
      this.addItem("gain the " + blessing);
      this.addItem("skip this adventure");
    }

    public void selectedToSetting() {
      // Index 0 is "show in browser"
      // Index 1 maps to 1 or 2 at runtime
      // Index 2 maps to 3
      // Index 3 maps to 6
      int index = this.getSelectedIndex();
      int value = index == 2 ? 3 : index == 3 ? 6 : index;
      Preferences.setString(this.setting, String.valueOf(value));
    }

    public void settingToSelected() {
      int value = Preferences.getInteger(this.setting);
      int index = value == 6 ? 3 : value == 3 ? 2 : value == 1 ? 1 : value == 0 ? 0 : -1;

      if (index != -1) {
        this.setSelectedIndex(index);
      } else {
        System.out.println("Invalid setting " + value + " for " + this.setting);
      }
    }
  }

  private class OceanDestinationComboBox extends JComboBox<String> {
    public OceanDestinationComboBox() {
      super();
      this.createMenu(Preferences.getString("oceanDestination"));
      this.addActionListener(this);
    }

    private void createMenu(String dest) {
      this.addItem("ignore adventure");
      this.addItem("manual control");
      this.addItem("muscle");
      this.addItem("mysticality");
      this.addItem("moxie");
      this.addItem("rainbow sand");
      this.addItem("altar fragment");
      this.addItem("El Vibrato power sphere");
      this.addItem("the plinth");
      this.addItem("random choice");
      if (dest.indexOf(",") != -1) {
        this.addItem("go to " + dest);
      }
      this.addItem("choose destination...");
    }

    public void loadSettings() {
      String dest = Preferences.getString("oceanDestination");
      this.removeAllItems();
      this.createMenu(dest);
      this.loadSettings(dest);
    }

    private void loadSettings(String dest) {
      // Default is "Manual"
      int index = 1;

      if (dest.equals("ignore")) {
        index = 0;
      } else if (dest.equals("manual")) {
        index = 1;
      } else if (dest.equals("muscle")) {
        index = 2;
      } else if (dest.equals("mysticality")) {
        index = 3;
      } else if (dest.equals("moxie")) {
        index = 4;
      } else if (dest.equals("sand")) {
        index = 5;
      } else if (dest.equals("altar")) {
        index = 6;
      } else if (dest.equals("sphere")) {
        index = 7;
      } else if (dest.equals("plinth")) {
        index = 8;
      } else if (dest.equals("random")) {
        index = 9;
      } else if (dest.indexOf(",") != -1) {
        index = 10;
      }

      this.setSelectedIndex(index);
    }

    public void saveSettings() {
      String dest = (String) this.getSelectedItem();
      if (dest == null) {
        return;
      }

      if (dest.startsWith("ignore")) {
        Preferences.setString("choiceAdventure189", "2");
        Preferences.setString("oceanDestination", "ignore");
        return;
      }

      String value = "";
      if (dest.startsWith("muscle")) {
        value = "muscle";
      } else if (dest.startsWith("mysticality")) {
        value = "mysticality";
      } else if (dest.startsWith("moxie")) {
        value = "moxie";
      } else if (dest.startsWith("rainbow sand")) {
        value = "sand";
      } else if (dest.startsWith("altar fragment")) {
        value = "altar";
      } else if (dest.startsWith("El Vibrato power sphere")) {
        value = "sphere";
      } else if (dest.startsWith("the plinth")) {
        value = "plinth";
      } else if (dest.startsWith("random")) {
        value = "random";
      } else if (dest.startsWith("go to ")) {
        value = dest.substring(6);
      } else if (dest.startsWith("choose ")) {
        return;
      } else { // For anything else, assume Manual Control
        // For manual control, do not take a choice first
        Preferences.setString("choiceAdventure189", "0");
        Preferences.setString("oceanDestination", "manual");
        return;
      }

      Preferences.setString("choiceAdventure189", "1");
      Preferences.setString("oceanDestination", value);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      String dest = (String) this.getSelectedItem();
      if (dest == null) {
        return;
      }

      // Are we choosing a custom destination?
      if (!dest.startsWith("choose")) {
        return;
      }

      // Prompt for a new destination
      String coords = getCoordinates();
      if (coords == null) {
        // Restore previous selection
        this.loadSettings();
        return;
      }

      // Rebuild combo box
      this.removeAllItems();
      this.createMenu(coords);

      // Select the "go to" menu item
      this.setSelectedIndex(10);

      // Request that the settings be saved in a different thread.
      RequestThread.runInParallel(new SaveOceanDestinationSettingsRunnable(this));
    }

    private String getCoordinates() {
      String coords = InputFieldUtilities.input("Longitude, Latitude");
      if (coords == null) {
        return null;
      }

      int index = coords.indexOf(",");
      if (index == -1) {
        return null;
      }

      int longitude = StringUtilities.parseInt(coords.substring(0, index));
      int latitude = StringUtilities.parseInt(coords.substring(index + 1));

      if (!Point.valid(longitude, latitude)) {
        // longitude and/or latitude is out of range
        KoLmafia.updateDisplay("(" + coords + ") are not valid ocean coordinates");
        return null;
      }

      Point point = new Point(longitude, latitude);
      if (Destination.MAINLAND.getLocations().contains(point)) {
        // The destination is on the mainland and you cannot sail there
        KoLmafia.updateDisplay("(" + coords + ") is on the mainland");
        return null;
      }

      return point.toString();
    }
  }

  private static class SaveOceanDestinationSettingsRunnable implements Runnable {
    private final OceanDestinationComboBox dest;

    public SaveOceanDestinationSettingsRunnable(OceanDestinationComboBox dest) {
      this.dest = dest;
    }

    @Override
    public void run() {
      this.dest.saveSettings();
    }
  }

  private class UpdateChoicesListener implements ListSelectionListener {
    @Override
    public void valueChanged(final ListSelectionEvent e) {
      JList<KoLAdventure> source = (JList<KoLAdventure>) e.getSource();
      KoLAdventure location = source.getSelectedValue();
      if (location == null) {
        return;
      }
      String zone = location.getZone();
      if (zone.equals("Item-Driven")) {
        ChoiceOptionsPanel.this.setSelectedIndex(1);
        ChoiceOptionsPanel.this.choiceCards.show(ChoiceOptionsPanel.this.choicePanel, "");
      } else {
        ChoiceOptionsPanel.this.setSelectedIndex(0);
        ChoiceOptionsPanel.this.choiceCards.show(
            ChoiceOptionsPanel.this.choicePanel,
            ChoiceOptionsPanel.this.choiceMap.containsKey(zone) ? zone : "");
      }
      KoLCharacter.updateSelectedLocation(location);
    }
  }

  private boolean isAdjusting = false;

  @Override
  public synchronized void update() {
    if (!this.isAdjusting) {
      this.loadSettings();
    }
  }

  public synchronized void saveSettings() {
    if (this.isAdjusting) {
      return;
    }
    this.isAdjusting = true;

    Object override = this.manualLouvre.getSelectedItem();
    int overrideIndex = this.manualLouvre.getSelectedIndex();
    Preferences.setString(
        "louvreOverride", overrideIndex == 0 || override == null ? "" : (String) override);

    Preferences.setInteger("violetFogGoal", this.violetFogSelect.getSelectedIndex());
    Preferences.setString(
        "choiceAdventure127", String.valueOf(this.palindomePapayaSelect.getSelectedIndex() + 1));
    Preferences.setString(
        "choiceAdventure549", String.valueOf(this.darkAtticSelect.getSelectedIndex()));
    Preferences.setString(
        "choiceAdventure550", String.valueOf(this.unlivingRoomSelect.getSelectedIndex()));
    Preferences.setString(
        "choiceAdventure551", String.valueOf(this.debasementSelect.getSelectedIndex()));
    Preferences.setString(
        "choiceAdventure552", String.valueOf(this.propDeportmentSelect.getSelectedIndex()));
    Preferences.setString(
        "choiceAdventure553", String.valueOf(this.reloadedSelect.getSelectedIndex()));
    Preferences.setString(
        "choiceAdventure554", String.valueOf(this.sororityGuideSelect.getSelectedIndex()));

    this.hiddenShrineNWSelect.selectedToSetting();
    this.hiddenShrineSWSelect.selectedToSetting();
    this.hiddenShrineNESelect.selectedToSetting();
    this.hiddenShrineSESelect.selectedToSetting();

    int hiddenApartmentIndex = this.hiddenApartmentSelect.getSelectedIndex();
    Preferences.setString(
        "choiceAdventure780",
        hiddenApartmentIndex == 1
            ? "1"
            : hiddenApartmentIndex == 2 ? "3" : hiddenApartmentIndex == 3 ? "6" : "0");
    Preferences.setString(
        "choiceAdventure784", String.valueOf(this.hiddenHospitalSelect.getSelectedIndex()));
    int hiddenParkIndex = this.hiddenParkSelect.getSelectedIndex();
    Preferences.setString(
        "choiceAdventure789",
        hiddenParkIndex == 1 ? "1" : hiddenParkIndex == 2 ? "2" : hiddenParkIndex == 3 ? "6" : "0");
    Preferences.setString(
        "choiceAdventure788", String.valueOf(this.hiddenBowlingAlleySelect.getSelectedIndex()));
    int hiddenOfficeIndex = this.hiddenOfficeSelect.getSelectedIndex();
    Preferences.setString(
        "choiceAdventure786",
        hiddenOfficeIndex == 1
            ? "1"
            : hiddenOfficeIndex == 2 ? "3" : hiddenOfficeIndex == 3 ? "6" : "0");
    int massiveZigguratIndex = this.massiveZigguratSelect.getSelectedIndex();
    Preferences.setString(
        "choiceAdventure791",
        massiveZigguratIndex == 0 ? "0" : massiveZigguratIndex == 1 ? "1" : "6");

    Preferences.setString(
        "choiceAdventure700", String.valueOf(this.kolhsCafeteriaSelect.getSelectedIndex()));

    Preferences.setInteger("lightsOutAutomation", this.lightsOutSelect.getSelectedIndex());

    int dailyDungeonDoorIndex = this.dailyDungeonDoorSelect.getSelectedIndex();
    String currentSetting = Preferences.getString("choiceAdventure692");
    Preferences.setString(
        "choiceAdventure692",
        dailyDungeonDoorIndex == 0
            ? "0"
            : dailyDungeonDoorIndex == 1
                ? "1"
                : dailyDungeonDoorIndex == 2
                    ? "11"
                    : dailyDungeonDoorIndex == 3 ? "12" : currentSetting);

    Preferences.setString(
        "choiceAdventure989", String.valueOf(this.paranormalLabSelect.getSelectedIndex()));
    Preferences.setString(
        "choiceAdventure988", String.valueOf(this.containmentSelect.getSelectedIndex()));

    Preferences.setInteger("basementMallPrices", this.basementMallSelect.getSelectedIndex());
    Preferences.setInteger("breakableHandling", this.breakableSelect.getSelectedIndex() + 1);
    Preferences.setInteger("addingScrolls", this.addingSelect.getSelectedIndex());
    Preferences.setInteger("gongPath", this.gongSelect.getSelectedIndex());
    GongCommand.setPath(this.gongSelect.getSelectedIndex());

    int louvreGoal = this.louvreSelect.getSelectedIndex();
    Preferences.setString("choiceAdventure91", overrideIndex > 0 || louvreGoal > 0 ? "1" : "2");
    Preferences.setInteger("louvreDesiredGoal", louvreGoal);

    for (int i = 0; i < this.optionSelects.size(); ++i) {
      ChoiceAdventure choiceAdventure = ChoiceAdventures.CHOICE_ADVS[i];
      String setting = choiceAdventure.getSetting();
      int index = this.optionSelects.get(i).getSelectedIndex();
      Object option = this.optionSelects.get(i).getSelectedItem();
      if (option instanceof Option) {
        index = ((Option) option).getDecision(index);
      }
      Preferences.setString(setting, String.valueOf(index));
    }

    switch (this.spookyForestSelect.getSelectedIndex()) {
      case 0: // Manual Control
        Preferences.setString("choiceAdventure502", "0");
        break;
      case 1: // Mosquito Larva or Spooky Mushrooms
        Preferences.setString("choiceAdventure502", "2");
        Preferences.setString("choiceAdventure505", "1");
        break;
      case 2: // Spooky-Gro Fertilizer
        Preferences.setString("choiceAdventure502", "3");
        Preferences.setString("choiceAdventure506", "2");
        break;
      case 3: // Spooky Sapling & Sell Bar Skins
        Preferences.setString("choiceAdventure502", "1");
        Preferences.setString("choiceAdventure503", "3");
        // If we have no Spooky Sapling
        // Preferences.setString( "choiceAdventure504", "3" );
        // If we have bear skins:
        // Preferences.setString( "choiceAdventure504", "2" );
        // Exit choice
        Preferences.setString("choiceAdventure504", "4");
        break;
      case 4: // Spooky Temple Map then skip adventure
        // Without tree-holed coin
        Preferences.setString("choiceAdventure502", "2");
        Preferences.setString("choiceAdventure505", "2");
        // With tree-holed coin
        // Preferences.setString( "choiceAdventure502", "3" );
        Preferences.setString("choiceAdventure506", "3");
        Preferences.setString("choiceAdventure507", "1");
        break;
      case 5: // Meet Vampire Hunter
        Preferences.setString("choiceAdventure502", "1");
        Preferences.setString("choiceAdventure503", "2");
        break;
      case 6: // Meet Vampire
        Preferences.setString("choiceAdventure502", "2");
        Preferences.setString("choiceAdventure505", "3");
        break;
      case 7: // Gain Meat
        Preferences.setString("choiceAdventure502", "1");
        Preferences.setString("choiceAdventure503", "1");
        break;
      case 8: // Seal clubber corpse
        Preferences.setString("choiceAdventure502", "3");
        Preferences.setString("choiceAdventure506", "1");
        Preferences.setString("choiceAdventure26", "1");
        Preferences.setString("choiceAdventure27", "1");
        break;
      case 9: // Loot Turtle Tamer corpse
        Preferences.setString("choiceAdventure502", "3");
        Preferences.setString("choiceAdventure506", "1");
        Preferences.setString("choiceAdventure26", "1");
        Preferences.setString("choiceAdventure27", "2");
        break;
      case 10: // Loot Pastamancer corpse
        Preferences.setString("choiceAdventure502", "3");
        Preferences.setString("choiceAdventure506", "1");
        Preferences.setString("choiceAdventure26", "2");
        Preferences.setString("choiceAdventure28", "1");
        break;
      case 11: // Loot Sauceror corpse
        Preferences.setString("choiceAdventure502", "3");
        Preferences.setString("choiceAdventure506", "1");
        Preferences.setString("choiceAdventure26", "2");
        Preferences.setString("choiceAdventure28", "2");
        break;
      case 12: // Loot Disco Bandit corpse
        Preferences.setString("choiceAdventure502", "3");
        Preferences.setString("choiceAdventure506", "1");
        Preferences.setString("choiceAdventure26", "3");
        Preferences.setString("choiceAdventure29", "1");
        break;
      case 13: // Loot Accordion Thief corpse
        Preferences.setString("choiceAdventure502", "3");
        Preferences.setString("choiceAdventure506", "1");
        Preferences.setString("choiceAdventure26", "3");
        Preferences.setString("choiceAdventure29", "2");
        break;
    }

    switch (this.riseSelect.getSelectedIndex()) {
      case 0: // Ignore this adventure
        Preferences.setString("choiceAdventure888", "4");
        break;

      case 1: // Mysticality
        Preferences.setString("choiceAdventure888", "3");
        Preferences.setString("choiceAdventure88", "1");
        break;

      case 2: // Moxie
        Preferences.setString("choiceAdventure888", "3");
        Preferences.setString("choiceAdventure88", "2");
        break;

      case 3: // Mysticality Class Skill
        Preferences.setString("choiceAdventure888", "3");
        Preferences.setString("choiceAdventure88", "3");
        break;
    }

    switch (this.fallSelect.getSelectedIndex()) {
      case 0: // Ignore this adventure
        Preferences.setString("choiceAdventure889", "5");
        break;

      case 1: // Muscle
        Preferences.setString("choiceAdventure889", "3");
        break;
    }

    // necessary for backwards-compatibility
    switch (this.maidenSelect.getSelectedIndex()) {
      case 0: // Ignore this adventure
        Preferences.setString("choiceAdventure89", "6");
        break;

      case 1: // Fight a random knight
      case 2: // Only fight the wolf knight
      case 3: // Only fight the snake knight
      case 4: // Maidens, then fight a random knight
      case 5: // Maidens, then fight the wolf knight
      case 6: // Maidens, then fight the snake knight
        Preferences.setString(
            "choiceAdventure89", String.valueOf(this.maidenSelect.getSelectedIndex() - 1));
        break;
    }

    // OceanDestinationComboBox handles its own settings.
    this.oceanDestSelect.saveSettings();

    switch (this.oceanActionSelect.getSelectedIndex()) {
      case 0:
        Preferences.setString("oceanAction", "continue");
        break;
      case 1:
        Preferences.setString("oceanAction", "show");
        break;
      case 2:
        Preferences.setString("oceanAction", "stop");
        break;
      case 3:
        Preferences.setString("oceanAction", "savecontinue");
        break;
      case 4:
        Preferences.setString("oceanAction", "saveshow");
        break;
      case 5:
        Preferences.setString("oceanAction", "savestop");
        break;
    }

    this.isAdjusting = false;
  }

  public synchronized void loadSettings() {
    this.isAdjusting = true;
    ActionPanel.enableActions(false); // prevents recursive actions from being triggered

    int index = Preferences.getInteger("violetFogGoal");
    if (index >= 0) {
      this.violetFogSelect.setSelectedIndex(index);
    }

    String setting = Preferences.getString("louvreOverride");
    if (setting.equals("")) {
      this.manualLouvre.setSelectedIndex(0);
    } else {
      this.manualLouvre.setSelectedItem(setting);
    }

    index = Preferences.getInteger("louvreDesiredGoal");
    if (index >= 0) {
      this.louvreSelect.setSelectedIndex(index);
    }

    this.palindomePapayaSelect.setSelectedIndex(
        Math.max(0, Preferences.getInteger("choiceAdventure127") - 1));
    this.darkAtticSelect.setSelectedIndex(Preferences.getInteger("choiceAdventure549"));
    this.unlivingRoomSelect.setSelectedIndex(Preferences.getInteger("choiceAdventure550"));
    this.debasementSelect.setSelectedIndex(Preferences.getInteger("choiceAdventure551"));
    this.propDeportmentSelect.setSelectedIndex(Preferences.getInteger("choiceAdventure552"));
    this.reloadedSelect.setSelectedIndex(Preferences.getInteger("choiceAdventure553"));
    this.sororityGuideSelect.setSelectedIndex(Preferences.getInteger("choiceAdventure554"));
    this.lightsOutSelect.setSelectedIndex(Preferences.getInteger("lightsOutAutomation"));

    this.hiddenShrineNWSelect.settingToSelected();
    this.hiddenShrineSWSelect.settingToSelected();
    this.hiddenShrineNESelect.settingToSelected();
    this.hiddenShrineSESelect.settingToSelected();

    int hiddenApartmentIndex = Preferences.getInteger("choiceAdventure780");
    if (hiddenApartmentIndex <= 6 && hiddenApartmentIndex >= 0) {
      this.hiddenApartmentSelect.setSelectedIndex(
          hiddenApartmentIndex == 1
              ? 1
              : hiddenApartmentIndex == 3 ? 2 : hiddenApartmentIndex == 6 ? 3 : 0);
    } else {
      System.out.println("Invalid setting " + hiddenApartmentIndex + " for choiceAdventure780.");
    }
    int hiddenHospitalIndex = Preferences.getInteger("choiceAdventure784");
    if (hiddenHospitalIndex <= 1 && hiddenHospitalIndex >= 0) {
      this.hiddenHospitalSelect.setSelectedIndex(hiddenHospitalIndex);
    } else {
      System.out.println("Invalid setting " + hiddenHospitalIndex + " for choiceAdventure784.");
    }
    int hiddenParkIndex = Preferences.getInteger("choiceAdventure789");
    if (hiddenParkIndex <= 6 && hiddenParkIndex >= 0) {
      this.hiddenParkSelect.setSelectedIndex(
          hiddenParkIndex == 1 ? 1 : hiddenParkIndex == 2 ? 2 : hiddenParkIndex == 6 ? 3 : 0);
    } else {
      System.out.println("Invalid setting " + hiddenParkIndex + " for choiceAdventure789.");
    }
    int hiddenBowlingAlleyIndex = Preferences.getInteger("choiceAdventure788");
    if (hiddenBowlingAlleyIndex <= 1 && hiddenBowlingAlleyIndex >= 0) {
      this.hiddenBowlingAlleySelect.setSelectedIndex(hiddenBowlingAlleyIndex);
    } else {
      System.out.println("Invalid setting " + hiddenBowlingAlleyIndex + " for choiceAdventure788.");
    }
    int hiddenOfficeIndex = Preferences.getInteger("choiceAdventure786");
    if (hiddenOfficeIndex <= 6 && hiddenOfficeIndex >= 0) {
      this.hiddenOfficeSelect.setSelectedIndex(
          hiddenOfficeIndex == 1 ? 1 : hiddenOfficeIndex == 3 ? 2 : hiddenOfficeIndex == 6 ? 3 : 0);
    } else {
      System.out.println("Invalid setting " + hiddenOfficeIndex + " for choiceAdventure786.");
    }
    int massiveZigguratIndex = Preferences.getInteger("choiceAdventure791");
    switch (massiveZigguratIndex) {
      case 0:
      case 1:
        this.massiveZigguratSelect.setSelectedIndex(massiveZigguratIndex);
        break;
      default:
        System.out.println("Invalid setting " + massiveZigguratIndex + " for choiceAdventure791.");
      case 6:
        this.massiveZigguratSelect.setSelectedIndex(2);
        break;
    }

    int kolhsCafeteriaIndex = Preferences.getInteger("choiceAdventure700");
    if (kolhsCafeteriaIndex <= 1 && kolhsCafeteriaIndex >= 0) {
      this.kolhsCafeteriaSelect.setSelectedIndex(kolhsCafeteriaIndex);
    } else {
      System.out.println("Invalid setting " + kolhsCafeteriaIndex + " for choiceAdventure700.");
    }

    switch (Preferences.getInteger("choiceAdventure692")) {
      case 0:
        this.dailyDungeonDoorSelect.setSelectedIndex(0);
        break;
      case 1:
        this.dailyDungeonDoorSelect.setSelectedIndex(1);
        break;
      case 2:
      case 3:
      case 7:
      case 11:
        // unlock door
        this.dailyDungeonDoorSelect.setSelectedIndex(2);
        break;
      case 4:
      case 5:
      case 6:
      case 12:
        // stat test
        this.dailyDungeonDoorSelect.setSelectedIndex(3);
        break;
    }

    int paranormalLabIndex = Preferences.getInteger("choiceAdventure989");
    if (paranormalLabIndex <= 1 && paranormalLabIndex >= 0) {
      this.paranormalLabSelect.setSelectedIndex(paranormalLabIndex);
    } else {
      System.out.println("Invalid setting " + paranormalLabIndex + " for choiceAdventure989.");
    }

    int containmentIndex = Preferences.getInteger("choiceAdventure988");
    if (containmentIndex <= 1 && containmentIndex >= 0) {
      this.containmentSelect.setSelectedIndex(containmentIndex);
    } else {
      System.out.println("Invalid setting " + containmentIndex + " for choiceAdventure988.");
    }

    this.basementMallSelect.setSelectedIndex(Preferences.getInteger("basementMallPrices"));
    this.breakableSelect.setSelectedIndex(
        Math.max(0, Preferences.getInteger("breakableHandling") - 1));

    int adding = Preferences.getInteger("addingScrolls");
    if (adding == -1) {
      adding = Preferences.getBoolean("createHackerSummons") ? 3 : 2;
      Preferences.setInteger("addingScrolls", adding);
    }
    this.addingSelect.setSelectedIndex(adding);

    this.gongSelect.setSelectedIndex(Preferences.getInteger("gongPath"));

    for (int i = 0; i < this.optionSelects.size(); ++i) {
      ChoiceAdventure choiceAdventure = ChoiceAdventures.CHOICE_ADVS[i];
      setting = choiceAdventure.getSetting();
      index = Preferences.getInteger(setting);
      if (index < 0) {
        continue;
      }

      if (index > 0) {
        Option[] options = choiceAdventure.getOptions();
        Option option = ChoiceAdventures.findOption(options, index);
        if (option != null) {
          this.optionSelects.get(i).setSelectedItem(option);
          continue;
        }

        System.out.println("Invalid setting " + index + " for " + setting);
      }

      this.optionSelects.get(i).setSelectedIndex(0);
    }

    // Figure out what to do in the spooky forest
    switch (Preferences.getInteger("choiceAdventure502")) {
      default:
      case 0:
        // Manual Control
        index = 0;
        break;

      case 1:
        switch (Preferences.getInteger("choiceAdventure503")) {
          case 1: // Get Meat
            index = 7;
            break;
          case 2: // Meet Vampire Hunter
            index = 5;
            break;
          case 3: // Spooky Sapling & Sell Bar Skins
            index = 3;
            break;
        }
        break;
      case 2:
        switch (Preferences.getInteger("choiceAdventure505")) {
          case 1: // Mosquito Larva or Spooky Mushrooms
            index = 1;
            break;
          case 2: // Tree-holed coin -> Spooky Temple Map
            index = 4;
            break;
          case 3: // Meet Vampire
            index = 6;
            break;
        }
        break;
      case 3:
        switch (Preferences.getInteger("choiceAdventure506")) {
          case 1: // Forest Corpses
            index = Preferences.getInteger("choiceAdventure26");
            index = index * 2 + Preferences.getInteger("choiceAdventure" + (26 + index)) - 3;
            index += 8;
            break;
          case 2: // Spooky-Gro Fertilizer
            index = 2;
            break;
          case 3: // Spooky Temple Map
            index = 4;
            break;
        }
        break;
    }

    this.spookyForestSelect.setSelectedIndex(index < 0 || index > 13 ? 0 : index);

    // Figure out what to do at the bookcases

    index = Preferences.getInteger("choiceAdventure888");
    if (index == 4) {
      this.riseSelect.setSelectedIndex(0);
    } else {
      this.riseSelect.setSelectedIndex(Preferences.getInteger("choiceAdventure88"));
    }

    index = Preferences.getInteger("choiceAdventure889");
    if (index == 5) {
      this.fallSelect.setSelectedIndex(0);
    } else {
      this.fallSelect.setSelectedIndex(1);
    }

    // Figure out what to do at the maidens
    // necessary for backwards-compatibility

    index = Preferences.getInteger("choiceAdventure89");
    if (index == 6) {
      this.maidenSelect.setSelectedIndex(0);
    } else {
      this.maidenSelect.setSelectedIndex(index + 1);
    }

    // OceanDestinationComboBox handles its own settings.
    this.oceanDestSelect.loadSettings();

    String action = Preferences.getString("oceanAction");
    if (action.equals("continue")) {
      this.oceanActionSelect.setSelectedIndex(0);
    } else if (action.equals("show")) {
      this.oceanActionSelect.setSelectedIndex(1);
    } else if (action.equals("stop")) {
      this.oceanActionSelect.setSelectedIndex(2);
    } else if (action.equals("savecontinue")) {
      this.oceanActionSelect.setSelectedIndex(3);
    } else if (action.equals("saveshow")) {
      this.oceanActionSelect.setSelectedIndex(4);
    } else if (action.equals("savestop")) {
      this.oceanActionSelect.setSelectedIndex(5);
    }

    this.isAdjusting = false;
    ActionPanel.enableActions(true);
  }

  public static class CommandButton extends JButton implements ActionListener {
    public CommandButton(String cmd) {
      super(cmd);

      this.setHorizontalAlignment(SwingConstants.LEFT);

      this.setActionCommand(cmd);
      this.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      CommandDisplayFrame.executeCommand(e.getActionCommand());
    }
  }
}
