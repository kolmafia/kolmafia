package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.LimitMode;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightSpinner;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class GearChangePanel extends JPanel {
  private static GearChangePanel INSTANCE = null;

  private JTabbedPane tabs;

  private boolean isEnabled;
  private JButton outfitButton;

  private JRadioButton[] weaponTypes;
  private JCheckBox weapon1H;
  private JRadioButton[] offhandTypes;

  private int deferredUpdateLevel = 0;
  private final EquipmentComboBox[] equipment;
  private final List<SortedListModel<AdventureResult>> equipmentModels;

  private final SortedListModel<FamiliarData> familiars = new SortedListModel<>();
  private final SortedListModel<FamiliarData> crownFamiliars = new SortedListModel<>();
  private final SortedListModel<FamiliarData> bjornFamiliars = new SortedListModel<>();

  private final EquipmentPanel equipmentPanel;
  private final CustomizablePanel customizablePanel;

  private final OutfitComboBox outfitSelect, customSelect;
  private final FamiliarComboBox familiarSelect;
  private final ThroneComboBox crownSelect;
  private final BjornComboBox bjornSelect;
  private JLabel sticker1Label, sticker2Label, sticker3Label;
  private FamLockCheckbox famLockCheckbox;
  private FakeHandsSpinner fakeHands;

  public GearChangePanel() {
    super(new BorderLayout());

    this.equipment = new EquipmentComboBox[EquipmentManager.ALL_SLOTS];
    this.equipmentModels = new ArrayList<>(EquipmentManager.ALL_SLOTS);

    List<List<AdventureResult>> lists = EquipmentManager.getEquipmentLists();

    for (int i = 0; i < this.equipment.length; ++i) {
      LockableListModel<AdventureResult> list;

      // We maintain our own lists for certain slots
      switch (i) {
        case EquipmentManager.HAT,
            EquipmentManager.PANTS,
            EquipmentManager.SHIRT,
            EquipmentManager.CONTAINER,
            EquipmentManager.WEAPON,
            EquipmentManager.OFFHAND,
            EquipmentManager.ACCESSORY1,
            EquipmentManager.ACCESSORY2,
            EquipmentManager.ACCESSORY3,
            EquipmentManager.FAMILIAR,
            EquipmentManager.BOOTSKIN,
            EquipmentManager.BOOTSPUR,
            EquipmentManager.HOLSTER -> {
          list = new SortedListModel<>();
          this.equipmentModels.add((SortedListModel<AdventureResult>) list);
        }
        default -> {
          list = (LockableListModel<AdventureResult>) lists.get(i);
          this.equipmentModels.add(null);
        }
      }

      this.equipment[i] = new EquipmentComboBox(list, i);
    }

    this.familiarSelect = new FamiliarComboBox(this.familiars);
    this.crownSelect = new ThroneComboBox(this.crownFamiliars);
    this.bjornSelect = new BjornComboBox(this.bjornFamiliars);
    this.outfitSelect =
        new OutfitComboBox((LockableListModel<SpecialOutfit>) EquipmentManager.getOutfits());
    this.customSelect =
        new OutfitComboBox((LockableListModel<SpecialOutfit>) EquipmentManager.getCustomOutfits());

    this.equipmentPanel = new EquipmentPanel();
    this.customizablePanel = new CustomizablePanel();

    this.tabs = KoLmafiaGUI.getTabbedPane();
    this.tabs.addTab("Equipment", this.equipmentPanel);
    this.tabs.addTab("Customizable", this.customizablePanel);

    this.add(this.tabs, BorderLayout.CENTER);

    GearChangePanel.INSTANCE = this;

    RequestThread.executeMethodAfterInitialization(this, "validateSelections");
  }

  public static StringBuffer getModifiers(
      Object value, final int slot, final boolean isCustomizablePanel, final int width) {
    StringBuffer buff = new StringBuffer();
    Modifiers mods;

    if (value instanceof AdventureResult item) {
      mods = new Modifiers();
      var taoFactor = KoLCharacter.hasSkill(SkillPool.TAO_OF_THE_TERRAPIN) ? 2 : 1;
      KoLCharacter.addItemAdjustment(
          mods,
          slot,
          item,
          EquipmentManager.allEquipment(),
          KoLCharacter.getEnthroned(),
          KoLCharacter.getBjorned(),
          Modeable.getStateMap(),
          true,
          taoFactor);
    } else if (value instanceof SpecialOutfit outfit) {
      mods = Modifiers.getModifiers(ModifierType.OUTFIT, outfit.getName());
    } else if (value instanceof FamiliarData familiar && isCustomizablePanel) {
      mods = Modifiers.getModifiers(ModifierType.THRONE, familiar.getRace());
    } else {
      return null;
    }

    if (mods == null) {
      return buff;
    }

    String name = mods.getString(Modifiers.INTRINSIC_EFFECT);
    if (name.length() > 0) {
      Modifiers newMods = new Modifiers();
      newMods.add(mods);
      newMods.add(Modifiers.getModifiers(ModifierType.EFFECT, name));
      mods = newMods;
    }

    buff.append("<html><table><tr><td width=");
    buff.append(width);
    buff.append(">");

    for (int i = 0; i < Modifiers.DOUBLE_MODIFIERS; ++i) {
      double val = mods.get(i);
      if (val == 0.0f) continue;
      name = Modifiers.getModifierName(i);
      name = StringUtilities.singleStringReplace(name, "Familiar", "Fam");
      name = StringUtilities.singleStringReplace(name, "Experience", "Exp");
      name = StringUtilities.singleStringReplace(name, "Damage", "Dmg");
      name = StringUtilities.singleStringReplace(name, "Resistance", "Res");
      name = StringUtilities.singleStringReplace(name, "Percent", "%");
      buff.append(name);
      buff.append(":<div align=right>");
      buff.append(KoLConstants.ROUNDED_MODIFIER_FORMAT.format(val));
      buff.append("</div>");
    }

    boolean anyBool = false;
    for (int i = 1; i < Modifiers.BITMAP_MODIFIERS; ++i) {
      if (mods.getRawBitmap(i) == 0) continue;
      if (anyBool) {
        buff.append(", ");
      }
      anyBool = true;
      buff.append(Modifiers.getBitmapModifierName(i));
    }

    for (int i = 1; i < Modifiers.BOOLEAN_MODIFIERS; ++i) {
      if (!mods.getBoolean(i)) continue;
      if (anyBool) {
        buff.append(", ");
      }
      anyBool = true;
      buff.append(Modifiers.getBooleanModifierName(i));
    }

    for (int i = 1; i < Modifiers.STRING_MODIFIERS; ++i) {
      if (i == Modifiers.WIKI_NAME
          || i == Modifiers.MODIFIERS
          || i == Modifiers.OUTFIT
          || i == Modifiers.FAMILIAR_EFFECT) {
        continue;
      }

      String strval = mods.getString(i);
      if (strval.equals("")) continue;
      name = Modifiers.getStringModifierName(i);
      name = StringUtilities.singleStringReplace(name, "Familiar", "Fam");
      if (anyBool) {
        buff.append(", ");
      }
      buff.append(name);
      buff.append(": ");
      buff.append(strval);
    }

    buff.append("</td></tr></table></html>");
    return buff;
  }

  public static void showModifiers(Object value) {
    var slot =
        (value instanceof AdventureResult item)
            ? EquipmentManager.consumeFilterToEquipmentType(ItemDatabase.getConsumptionType(item))
            : -1;
    showModifiers(value, slot);
  }

  public static void showModifiers(Object value, final int slot) {
    if (GearChangePanel.INSTANCE == null) {
      return;
    }

    EquipmentTabPanel pane =
        (EquipmentTabPanel) GearChangePanel.INSTANCE.tabs.getSelectedComponent();

    var isCustomizablePanel = pane == GearChangePanel.INSTANCE.customizablePanel;

    StringBuffer buff = getModifiers(value, slot, isCustomizablePanel, pane.getModifiersWidth());

    if (buff != null) {
      pane.getModifiersLabel().setText(buff.toString());
    }
  }

  private abstract static class EquipmentTabPanel extends GenericPanel {
    protected JLabel modifiersLabel;
    protected int modifiersWidth;

    public EquipmentTabPanel(
        final String confirmedText, final String cancelledText, Dimension left, Dimension right) {
      super(confirmedText, cancelledText, left, right);
    }

    public EquipmentTabPanel(final String confirmedText, Dimension left, Dimension right) {
      super(confirmedText, null, left, right);
    }

    public JLabel getModifiersLabel() {
      return this.modifiersLabel;
    }

    public int getModifiersWidth() {
      return this.modifiersWidth;
    }
  }

  private class EquipmentPanel extends EquipmentTabPanel {
    public EquipmentPanel() {
      super("change gear", "save as outfit", new Dimension(120, 20), new Dimension(320, 20));

      ArrayList<VerifiableElement> rows = new ArrayList<>();

      rows.add(new VerifiableElement("Hat:", GearChangePanel.this.equipment[EquipmentManager.HAT]));
      rows.add(
          new VerifiableElement(
              "Weapon:", GearChangePanel.this.equipment[EquipmentManager.WEAPON]));

      JPanel radioPanel1 = new JPanel(new GridLayout(1, 4));
      ButtonGroup radioGroup1 = new ButtonGroup();
      GearChangePanel.this.weaponTypes = new JRadioButton[3];

      GearChangePanel.this.weaponTypes[0] = new JRadioButton("all", true);
      GearChangePanel.this.weaponTypes[1] = new JRadioButton("melee");
      GearChangePanel.this.weaponTypes[2] = new JRadioButton("ranged");

      for (JRadioButton weaponType : weaponTypes) {
        radioGroup1.add(weaponType);
        radioPanel1.add(weaponType);
        weaponType.addActionListener(new RefilterListener());
      }

      GearChangePanel.this.weapon1H = new JCheckBox("1-hand");
      radioPanel1.add(GearChangePanel.this.weapon1H);
      GearChangePanel.this.weapon1H.addActionListener(new RefilterListener());

      rows.add(new VerifiableElement("", radioPanel1));

      rows.add(
          new AWoLClassVerifiableElement(
              "Holstered:", GearChangePanel.this.equipment[EquipmentManager.HOLSTER]));

      rows.add(
          new VerifiableElement(
              "Off-Hand:", GearChangePanel.this.equipment[EquipmentManager.OFFHAND]));

      JPanel radioPanel2 = new JPanel(new GridLayout(1, 4));
      ButtonGroup radioGroup2 = new ButtonGroup();
      GearChangePanel.this.offhandTypes = new JRadioButton[4];

      GearChangePanel.this.offhandTypes[0] = new JRadioButton("all", true);
      GearChangePanel.this.offhandTypes[1] = new JRadioButton("weapon");
      GearChangePanel.this.offhandTypes[2] = new JRadioButton("shields");
      GearChangePanel.this.offhandTypes[3] = new JRadioButton("other");

      for (JRadioButton offhandType : offhandTypes) {
        radioGroup2.add(offhandType);
        radioPanel2.add(offhandType);
        offhandType.addActionListener(new RefilterListener());
      }

      rows.add(new VerifiableElement("", radioPanel2));

      rows.add(
          new VerifiableElement(
              "Back:", GearChangePanel.this.equipment[EquipmentManager.CONTAINER]));

      rows.add(
          new VerifiableElement("Shirt:", GearChangePanel.this.equipment[EquipmentManager.SHIRT]));
      rows.add(
          new VerifiableElement("Pants:", GearChangePanel.this.equipment[EquipmentManager.PANTS]));

      rows.add(new VerifiableElement());

      rows.add(
          new VerifiableElement(
              "Accessory:", GearChangePanel.this.equipment[EquipmentManager.ACCESSORY1]));
      rows.add(
          new VerifiableElement(
              "Accessory:", GearChangePanel.this.equipment[EquipmentManager.ACCESSORY2]));
      rows.add(
          new VerifiableElement(
              "Accessory:", GearChangePanel.this.equipment[EquipmentManager.ACCESSORY3]));

      rows.add(new VerifiableElement());

      rows.add(new VerifiableElement("Familiar:", GearChangePanel.this.familiarSelect));
      rows.add(
          new VerifiableElement(
              "Fam Item:", GearChangePanel.this.equipment[EquipmentManager.FAMILIAR]));

      GearChangePanel.this.famLockCheckbox = new FamLockCheckbox();
      JPanel boxholder = new JPanel(new BorderLayout());
      if (!KoLCharacter.inQuantum()) {
        boxholder.add(GearChangePanel.this.famLockCheckbox);
      }
      rows.add(new VerifiableElement("", boxholder));

      rows.add(new VerifiableElement("Outfit:", GearChangePanel.this.outfitSelect));
      rows.add(new VerifiableElement("Custom:", GearChangePanel.this.customSelect));

      VerifiableElement[] elements = new VerifiableElement[rows.size()];
      elements = rows.toArray(elements);

      this.setContent(elements);

      GearChangePanel.this.outfitButton = this.cancelledButton;

      this.modifiersLabel = new JLabel();
      this.confirmedButton.getParent().getParent().add(this.modifiersLabel, BorderLayout.CENTER);
      this.modifiersWidth = this.eastContainer.getPreferredSize().width;
      this.setEnabled(true);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled);
      GearChangePanel.this.isEnabled = isEnabled;

      GearChangePanel.this.outfitSelect.setEnabled(isEnabled);
      GearChangePanel.this.customSelect.setEnabled(isEnabled);
      GearChangePanel.this.familiarSelect.setEnabled(isEnabled);
      GearChangePanel.this.outfitButton.setEnabled(isEnabled);
      GearChangePanel.this.famLockCheckbox.setEnabled(isEnabled);

      if (isEnabled) {
        GearChangePanel.this.updateAllModels();
      }
    }

    @Override
    public void actionConfirmed() {
      synchronized (GearChangePanel.class) {
        GearChangePanel.this.changeItems();
      }
    }

    @Override
    public void actionCancelled() {
      synchronized (GearChangePanel.class) {
        GearChangePanel.this.changeItems();
      }

      String currentValue = InputFieldUtilities.input("Name your outfit!", "Backup");
      if (currentValue != null) {
        RequestThread.postRequest(new EquipmentRequest(currentValue));
      }
    }

    private class AWoLClassVerifiableElement extends HideableVerifiableElement {
      public AWoLClassVerifiableElement(final String label, final JComponent inputField) {
        super(label, inputField);
      }

      @Override
      public boolean isHidden() {
        return !KoLCharacter.isAWoLClass();
      }
    }
  }

  private void changeItems() {
    // Find out what changed

    AdventureResult[] pieces = new AdventureResult[EquipmentManager.ALL_SLOTS];

    for (int i = 0; i < EquipmentManager.SLOTS; ++i) {
      pieces[i] = (AdventureResult) this.equipment[i].getSelectedItem();
      if (EquipmentManager.getEquipment(i).equals(pieces[i])) {
        pieces[i] = null;
      }
    }

    AdventureResult famitem =
        (AdventureResult) this.equipment[EquipmentManager.FAMILIAR].getSelectedItem();

    // Start with accessories

    for (int i : EquipmentManager.ACCESSORY_SLOTS) {
      if (pieces[i] != null) {
        RequestThread.postRequest(new EquipmentRequest(pieces[i], i, true));
        pieces[i] = null;
      }
    }

    // Move on to other equipment

    for (int i = 0; i < EquipmentManager.ACCESSORY1; ++i) {
      if (pieces[i] != null) {
        RequestThread.postRequest(new EquipmentRequest(pieces[i], i, true));
        pieces[i] = null;
      }
    }

    if (KoLCharacter.getFamiliar().canEquip(famitem)) {
      RequestThread.postRequest(new EquipmentRequest(famitem, EquipmentManager.FAMILIAR));
    }
  }

  private class CustomizablePanel extends EquipmentTabPanel {
    public CustomizablePanel() {
      super("change gear", new Dimension(120, 20), new Dimension(300, 20));

      ArrayList<VerifiableElement> rows = new ArrayList<>();
      VerifiableElement element;

      rows.add(new VerifiableElement("Crown of Thrones:", GearChangePanel.this.crownSelect));

      rows.add(new VerifiableElement());

      rows.add(new VerifiableElement("Buddy Bjorn:", GearChangePanel.this.bjornSelect));

      rows.add(new VerifiableElement());

      element =
          new VerifiableElement(
              "Sticker:", GearChangePanel.this.equipment[EquipmentManager.STICKER1]);
      GearChangePanel.this.sticker1Label = element.getLabel();
      rows.add(element);

      element =
          new VerifiableElement(
              "Sticker:", GearChangePanel.this.equipment[EquipmentManager.STICKER2]);
      GearChangePanel.this.sticker2Label = element.getLabel();
      rows.add(element);

      element =
          new VerifiableElement(
              "Sticker:", GearChangePanel.this.equipment[EquipmentManager.STICKER3]);
      GearChangePanel.this.sticker3Label = element.getLabel();
      rows.add(element);

      rows.add(new VerifiableElement());

      GearChangePanel.this.fakeHands = new FakeHandsSpinner();
      GearChangePanel.this.fakeHands.setHorizontalAlignment(AutoHighlightTextField.RIGHT);
      GearChangePanel.this
          .fakeHands
          .getEditor()
          .setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

      rows.add(new VerifiableElement("Fake Hands:", GearChangePanel.this.fakeHands));

      rows.add(new VerifiableElement());

      rows.add(
          new VerifiableElement(
              "Card Sleeve:", GearChangePanel.this.equipment[EquipmentManager.CARDSLEEVE]));

      rows.add(new VerifiableElement());
      rows.add(
          new VerifiableElement(
              "Folder:", GearChangePanel.this.equipment[EquipmentManager.FOLDER1]));
      rows.add(
          new VerifiableElement(
              "Folder:", GearChangePanel.this.equipment[EquipmentManager.FOLDER2]));
      rows.add(
          new VerifiableElement(
              "Folder:", GearChangePanel.this.equipment[EquipmentManager.FOLDER3]));
      rows.add(
          new VerifiableElement(
              "Folder:", GearChangePanel.this.equipment[EquipmentManager.FOLDER4]));
      rows.add(
          new VerifiableElement(
              "Folder:", GearChangePanel.this.equipment[EquipmentManager.FOLDER5]));

      rows.add(new VerifiableElement());
      rows.add(
          new VerifiableElement(
              "Boot Skin:", GearChangePanel.this.equipment[EquipmentManager.BOOTSKIN]));
      rows.add(
          new VerifiableElement(
              "Boot Spur:", GearChangePanel.this.equipment[EquipmentManager.BOOTSPUR]));

      VerifiableElement[] elements = new VerifiableElement[rows.size()];
      elements = rows.toArray(elements);

      this.setContent(elements);

      this.modifiersLabel = new JLabel();
      this.confirmedButton.getParent().getParent().add(this.modifiersLabel, BorderLayout.CENTER);
      this.modifiersWidth = this.eastContainer.getPreferredSize().width;
      this.setEnabled(true);
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled);

      boolean hasCrownOfThrones = KoLCharacter.hasEquipped(EquipmentManager.CROWN_OF_THRONES);
      GearChangePanel.this.crownSelect.setEnabled(isEnabled && hasCrownOfThrones);

      boolean hasBuddyBjorn = KoLCharacter.hasEquipped(EquipmentManager.BUDDY_BJORN);
      GearChangePanel.this.bjornSelect.setEnabled(isEnabled && hasBuddyBjorn);

      boolean hasFakeHands = GearChangePanel.this.fakeHands.getAvailableFakeHands() > 0;
      GearChangePanel.this.fakeHands.setEnabled(isEnabled && hasFakeHands);

      boolean hasCardSleeve =
          EquipmentManager.CARD_SLEEVE.getCount(KoLConstants.inventory) > 0
              || KoLCharacter.hasEquipped(EquipmentManager.CARD_SLEEVE);
      GearChangePanel.this.equipment[EquipmentManager.CARDSLEEVE].setEnabled(
          isEnabled && hasCardSleeve);

      boolean hasFolderHolder =
          EquipmentManager.FOLDER_HOLDER.getCount(KoLConstants.inventory) > 0
              || KoLCharacter.hasEquipped(EquipmentManager.FOLDER_HOLDER);
      boolean inHighSchool = KoLCharacter.inHighschool();

      GearChangePanel.this.equipment[EquipmentManager.FOLDER1].setEnabled(
          isEnabled && hasFolderHolder);
      GearChangePanel.this.equipment[EquipmentManager.FOLDER2].setEnabled(
          isEnabled && hasFolderHolder);
      GearChangePanel.this.equipment[EquipmentManager.FOLDER3].setEnabled(
          isEnabled && hasFolderHolder);
      GearChangePanel.this.equipment[EquipmentManager.FOLDER4].setEnabled(
          isEnabled && hasFolderHolder && inHighSchool);
      GearChangePanel.this.equipment[EquipmentManager.FOLDER5].setEnabled(
          isEnabled && hasFolderHolder && inHighSchool);

      boolean hasBoots =
          EquipmentManager.COWBOY_BOOTS.getCount(KoLConstants.inventory) > 0
              || KoLCharacter.hasEquipped(EquipmentManager.COWBOY_BOOTS);
      GearChangePanel.this.equipment[EquipmentManager.BOOTSKIN].setEnabled(isEnabled && hasBoots);
      GearChangePanel.this.equipment[EquipmentManager.BOOTSPUR].setEnabled(isEnabled && hasBoots);
    }

    @Override
    public void actionConfirmed() {
      synchronized (GearChangePanel.class) {
        GearChangePanel.this.customizeItems();
      }
    }

    @Override
    public void actionCancelled() {}
  }

  private void customizeItems() {
    // Crown of Thrones
    FamiliarData familiar = (FamiliarData) crownSelect.getSelectedItem();
    FamiliarData enthronedFamiliar = KoLCharacter.getEnthroned();
    if (familiar != enthronedFamiliar) {
      RequestThread.postRequest(FamiliarRequest.enthroneRequest(familiar));
    }

    // Buddy Bjorn
    familiar = (FamiliarData) bjornSelect.getSelectedItem();
    FamiliarData bjornedFamiliar = KoLCharacter.getBjorned();
    if (familiar != bjornedFamiliar) {
      RequestThread.postRequest(FamiliarRequest.bjornifyRequest(familiar));
    }

    // Card Sleeve
    AdventureResult card =
        (AdventureResult) this.equipment[EquipmentManager.CARDSLEEVE].getSelectedItem();
    if (!EquipmentManager.getEquipment(EquipmentManager.CARDSLEEVE).equals(card)) {
      RequestThread.postRequest(new EquipmentRequest(card, EquipmentManager.CARDSLEEVE, true));
    }

    // Stickers
    AdventureResult[] stickers =
        new AdventureResult[] {
          (AdventureResult) this.equipment[EquipmentManager.STICKER1].getSelectedItem(),
          (AdventureResult) this.equipment[EquipmentManager.STICKER2].getSelectedItem(),
          (AdventureResult) this.equipment[EquipmentManager.STICKER3].getSelectedItem(),
        };

    for (int i = 0; i < stickers.length; ++i) {
      AdventureResult sticker = stickers[i];
      int slot = EquipmentManager.STICKER1 + i;
      if (!EquipmentManager.getEquipment(slot).equals(sticker)) {
        RequestThread.postRequest(new EquipmentRequest(sticker, slot, true));
      }
    }

    // Folders
    AdventureResult[] folders =
        new AdventureResult[] {
          (AdventureResult) this.equipment[EquipmentManager.FOLDER1].getSelectedItem(),
          (AdventureResult) this.equipment[EquipmentManager.FOLDER2].getSelectedItem(),
          (AdventureResult) this.equipment[EquipmentManager.FOLDER3].getSelectedItem(),
          (AdventureResult) this.equipment[EquipmentManager.FOLDER4].getSelectedItem(),
          (AdventureResult) this.equipment[EquipmentManager.FOLDER5].getSelectedItem(),
        };

    for (int i = 0; i < folders.length; ++i) {
      AdventureResult folder = folders[i];
      int slot = EquipmentManager.FOLDER1 + i;
      if (!EquipmentManager.getEquipment(slot).equals(folder)) {
        RequestThread.postRequest(new EquipmentRequest(folder, slot, true));
      }
    }

    // Cowboy Boots
    AdventureResult[] bootDecorations =
        new AdventureResult[] {
          (AdventureResult) this.equipment[EquipmentManager.BOOTSKIN].getSelectedItem(),
          (AdventureResult) this.equipment[EquipmentManager.BOOTSPUR].getSelectedItem(),
        };

    for (int i = 0; i < bootDecorations.length; ++i) {
      AdventureResult decoration = bootDecorations[i];
      int slot = EquipmentManager.BOOTSKIN + i;
      if (!EquipmentManager.getEquipment(slot).equals(decoration)) {
        RequestThread.postRequest(new EquipmentRequest(decoration, slot, true));
      }
    }

    int oldFakeHands = EquipmentManager.getFakeHands();
    int newFakeHands = (Integer) this.fakeHands.getValue();
    if (oldFakeHands != newFakeHands) {
      // If we want fewer fake hands than we currently have, unequip one - which will unequip all of
      // them.
      if (newFakeHands < oldFakeHands) {
        EquipmentRequest request =
            new EquipmentRequest(EquipmentRequest.UNEQUIP, EquipmentManager.FAKEHAND);
        RequestThread.postRequest(request);
        oldFakeHands = 0;
      }

      // Equip fake hands one at a time until we have enough
      while (oldFakeHands++ < newFakeHands) {
        EquipmentRequest request =
            new EquipmentRequest(EquipmentManager.FAKE_HAND, EquipmentManager.FAKEHAND);
        RequestThread.postRequest(request);
      }
    }
  }

  public static final void validateSelections() {
    if (GearChangePanel.INSTANCE == null) {
      return;
    }

    GearChangePanel.INSTANCE.updateAllModels();
    GearChangePanel.INSTANCE.equipmentPanel.hideOrShowElements();
  }

  public static final void updateSlot(final int slot) {
    if (GearChangePanel.INSTANCE == null) {
      return;
    }

    if (slot < 0 || slot >= EquipmentManager.ALL_SLOTS) {
      return;
    }

    LockableListModel<AdventureResult> model = GearChangePanel.INSTANCE.equipmentModels.get(slot);
    if (model == null) {
      return;
    }

    model.setSelectedItem(EquipmentManager.getEquipment(slot));

    GearChangePanel.INSTANCE.updateAllModels();
  }

  public static final LockableListModel<AdventureResult> getModel(final int slot) {
    if (GearChangePanel.INSTANCE == null) {
      return null;
    }

    if (slot < 0 || slot >= EquipmentManager.ALL_SLOTS) {
      return null;
    }

    return GearChangePanel.INSTANCE.equipmentModels.get(slot);
  }

  public static final void updateStickers(int st1, int st2, int st3) {
    if (GearChangePanel.INSTANCE == null) {
      return;
    }

    GearChangePanel.INSTANCE.sticker1Label.setText("Sticker (" + st1 + "): ");
    GearChangePanel.INSTANCE.sticker2Label.setText("Sticker (" + st2 + "): ");
    GearChangePanel.INSTANCE.sticker3Label.setText("Sticker (" + st3 + "): ");
  }

  public static final void clearEquipmentModels() {
    if (GearChangePanel.INSTANCE == null) {
      return;
    }

    for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
      LockableListModel<AdventureResult> model = GearChangePanel.INSTANCE.equipmentModels.get(slot);
      if (model != null) {
        model.clear();
      }
    }
  }

  private class EquipmentComboBox extends JComboBox<AdventureResult> {
    public EquipmentComboBox(final LockableListModel<AdventureResult> model, final int slot) {
      super(model);

      DefaultListCellRenderer renderer = ListCellRendererFactory.getUsableEquipmentRenderer(slot);

      this.setRenderer(renderer);
      this.addPopupMenuListener(new ChangeItemListener());
    }

    private class ChangeItemListener extends ThreadedListener {
      @Override
      protected void execute() {
        ComboBoxModel<AdventureResult> model = EquipmentComboBox.this.getModel();
        if (model.getSize() == 0) {
          return;
        }

        // Simply re-validate what it is you need to
        // equip.

        GearChangePanel.this.updateAllModels();
      }
    }
  }

  private static class OutfitComboBox extends JComboBox<SpecialOutfit> {
    public OutfitComboBox(final LockableListModel<SpecialOutfit> model) {
      super(model);

      this.setRenderer(ListCellRendererFactory.getDefaultRenderer());
      this.addActionListener(new ChangeOutfitListener());
    }

    private class ChangeOutfitListener extends ThreadedListener {
      @Override
      protected void execute() {
        ComboBoxModel<SpecialOutfit> model = OutfitComboBox.this.getModel();
        if (model.getSize() == 0) {
          return;
        }

        // If you're changing an outfit, then the
        // change must occur right away.

        SpecialOutfit outfit = (SpecialOutfit) OutfitComboBox.this.getSelectedItem();
        if (outfit == null) {
          return;
        }
        synchronized (GearChangePanel.class) {
          RequestThread.postRequest(new EquipmentRequest(outfit));
        }

        OutfitComboBox.this.setSelectedItem(null);
      }
    }
  }

  public static final void updateFamiliars() {
    if (GearChangePanel.INSTANCE == null) {
      return;
    }

    FamiliarData current = KoLCharacter.getFamiliar();
    GearChangePanel.INSTANCE.familiars.setSelectedItem(current);
    GearChangePanel.INSTANCE.updateAllModels();
  }

  public static final void clearFamiliarList() {
    if (GearChangePanel.INSTANCE == null) {
      return;
    }

    GearChangePanel.INSTANCE.familiars.clear();
  }

  private static class CarriedFamiliarComboBox extends JComboBox<FamiliarData> {
    public CarriedFamiliarComboBox(final LockableListModel<FamiliarData> model) {
      super(model);
      DefaultListCellRenderer renderer = ListCellRendererFactory.getFamiliarRenderer();
      this.setRenderer(renderer);
    }
  }

  private static class ThroneComboBox extends CarriedFamiliarComboBox implements Listener {
    public ThroneComboBox(final LockableListModel<FamiliarData> model) {
      super(model);
      NamedListenerRegistry.registerNamedListener("(throne)", this);
      this.update();
    }

    @Override
    public void update() {
      FamiliarData enthronedFamiliar = KoLCharacter.getEnthroned();
      FamiliarData selectedThroneFamiliar = (FamiliarData) this.getSelectedItem();
      if (enthronedFamiliar != selectedThroneFamiliar) {
        this.setSelectedItem(enthronedFamiliar);
      }
    }
  }

  private static class BjornComboBox extends CarriedFamiliarComboBox implements Listener {
    public BjornComboBox(final LockableListModel<FamiliarData> model) {
      super(model);
      NamedListenerRegistry.registerNamedListener("(bjorn)", this);
      this.update();
    }

    @Override
    public void update() {
      FamiliarData bjornedFamiliar = KoLCharacter.getBjorned();
      FamiliarData selectedBjornFamiliar = (FamiliarData) this.getSelectedItem();
      if (bjornedFamiliar != selectedBjornFamiliar) {
        this.setSelectedItem(bjornedFamiliar);
      }
    }
  }

  private class FamiliarComboBox extends JComboBox<FamiliarData> {
    public FamiliarComboBox(final LockableListModel<FamiliarData> model) {
      super(model);
      this.addActionListener(new ChangeFamiliarListener());
    }

    private class ChangeFamiliarListener extends ThreadedListener {
      @Override
      protected void execute() {
        ComboBoxModel<FamiliarData> model = FamiliarComboBox.this.getModel();
        if (model.getSize() == 0) {
          return;
        }

        // If you're changing your familiar, then make
        // sure all the equipment pieces get changed
        // and the familiar gets changed right after.

        FamiliarData familiar = (FamiliarData) FamiliarComboBox.this.getSelectedItem();
        if (familiar == null || familiar.equals(KoLCharacter.getFamiliar())) {
          return;
        }

        synchronized (GearChangePanel.class) {
          GearChangePanel.this.changeItems();
          RequestThread.postRequest(new FamiliarRequest(familiar));
        }
      }
    }
  }

  private class RefilterListener extends ThreadedListener {
    @Override
    protected void execute() {
      GearChangePanel.this.updateAllModels();
    }
  }

  private boolean slotItemCanBeNone(final int slot) {
    return switch (slot) {
      case EquipmentManager.BOOTSKIN, EquipmentManager.BOOTSPUR ->
      // You cannot remove the item in this slot, but if
      // nothing is equipped, need a placeholder
      EquipmentManager.getEquipment(slot).equals(EquipmentRequest.UNEQUIP);
      default -> true;
    };
  }

  private Optional<FamiliarData> familiarCarryingEquipment(final int slot) {
    return switch (slot) {
      case EquipmentManager.HAT -> KoLCharacter.ownedFamiliar(FamiliarPool.HATRACK);
      case EquipmentManager.PANTS -> KoLCharacter.ownedFamiliar(FamiliarPool.SCARECROW);
      case EquipmentManager.WEAPON -> KoLCharacter.ownedFamiliar(FamiliarPool.HAND);
      case EquipmentManager.OFFHAND -> KoLCharacter.ownedFamiliar(FamiliarPool.LEFT_HAND);
      default -> Optional.empty();
    };
  }

  private List<List<AdventureResult>> populateEquipmentLists() {
    List<List<AdventureResult>> lists = new ArrayList<>(EquipmentManager.ALL_SLOTS);

    // Create all equipment lists
    for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
      List<AdventureResult> items = new ArrayList<>();

      // Almost every list gets a "(none)"
      if (this.slotItemCanBeNone(slot)) {
        items.add(EquipmentRequest.UNEQUIP);
      }

      lists.add(items);
    }

    // Certain familiars can carry non-familiar-items
    FamiliarData myFamiliar = KoLCharacter.getFamiliar();
    ConsumptionType specialFamiliarType = myFamiliar.specialEquipmentType();
    boolean specialFamiliar = (specialFamiliarType != ConsumptionType.NONE);

    // Look at every item in inventory
    for (AdventureResult item : KoLConstants.inventory) {
      ConsumptionType consumption = ItemDatabase.getConsumptionType(item.getItemId());
      int slot = EquipmentManager.consumeFilterToEquipmentType(consumption);
      switch (consumption) {
        case WEAPON:
          if (this.shouldAddItem(item, consumption, EquipmentManager.WEAPON)) {
            lists.get(EquipmentManager.WEAPON).add(item);
          }
          if (this.shouldAddItem(item, consumption, EquipmentManager.OFFHAND)) {
            lists.get(EquipmentManager.OFFHAND).add(item);
          }
          break;

        case ACCESSORY:
          if (this.shouldAddItem(item, consumption, slot)) {
            lists.get(EquipmentManager.ACCESSORY1).add(item);
            lists.get(EquipmentManager.ACCESSORY2).add(item);
            lists.get(EquipmentManager.ACCESSORY3).add(item);
          }
          break;

          /*
          case CONSUME_STICKER:
            if (this.shouldAddItem(item, consumption, slot)) {
              lists.get(EquipmentManager.STICKER1).add(item);
              lists.get(EquipmentManager.STICKER2).add(item);
              lists.get(EquipmentManager.STICKER3).add(item);
            }
            break;

          case CONSUME_FOLDER:
            if (this.shouldAddItem(item, consumption, slot)) {
              lists.get(EquipmentManager.FOLDER1).add(item);
              lists.get(EquipmentManager.FOLDER2).add(item);
              lists.get(EquipmentManager.FOLDER3).add(item);
              lists.get(EquipmentManager.FOLDER4).add(item);
              lists.get(EquipmentManager.FOLDER5).add(item);
            }
            break;
          */

        default:
          if (this.shouldAddItem(item, consumption, slot)) {
            lists.get(slot).add(item);
          }
          break;
      }

      if (specialFamiliar && (consumption == specialFamiliarType) && myFamiliar.canEquip(item)) {
        lists.get(EquipmentManager.FAMILIAR).add(item);
      }
    }

    // Add current equipment
    for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
      List<AdventureResult> items = lists.get(slot);
      if (items == null) {
        continue;
      }

      AdventureResult currentItem = EquipmentManager.getEquipment(slot);
      if (!items.contains(currentItem) && this.filterItem(currentItem, slot)) {
        items.add(currentItem);
      }

      // If a non-current familiar has an appropriate item, add it.
      if (slot != EquipmentManager.FAMILIAR) {
        var familiar = familiarCarryingEquipment(slot);
        if (familiar.isPresent() && familiar.get() != KoLCharacter.getFamiliar()) {
          AdventureResult familiarItem = familiar.get().getItem();
          if (!items.contains(familiarItem) && this.filterItem(familiarItem, slot)) {
            items.add(familiarItem);
          }
        }
      }
    }

    // Add stealable familiar equipment
    if (myFamiliar != FamiliarData.NO_FAMILIAR) {
      List<AdventureResult> items = lists.get(EquipmentManager.FAMILIAR);
      for (FamiliarData familiar : KoLCharacter.ownedFamiliars()) {
        if (familiar == myFamiliar) {
          continue;
        }
        AdventureResult famItem = familiar.getItem();
        if (famItem != EquipmentRequest.UNEQUIP
            && myFamiliar.canEquip(famItem)
            && !items.contains(famItem)) {
          items.add(famItem);
        }
      }
    }

    return lists;
  }

  private boolean filterItem(AdventureResult item, int slot) {
    return this.shouldAddItem(item, ItemDatabase.getConsumptionType(item.getItemId()), slot);
  }

  private boolean shouldAddItem(AdventureResult item, ConsumptionType consumption, int slot) {
    switch (consumption) {
        // The following lists are local to GearChanger
      case HAT:
      case SHIRT:
      case CONTAINER:
      case PANTS:
      case ACCESSORY:
      case BOOTSKIN:
      case BOOTSPUR:
      case SIXGUN:
        break;
      case WEAPON:
        if (!this.filterWeapon(item, slot)) {
          return false;
        }
        break;
      case OFFHAND:
        if (!this.filterOffhand(item, ConsumptionType.OFFHAND)) {
          return false;
        }
        break;
      case FAMILIAR_EQUIPMENT:
        if (!KoLCharacter.getFamiliar().canEquip(item)) {
          return false;
        }
        break;
        // The following lists are in EquipmentManager
      case STICKER:
      case CARD:
      case FOLDER:
        break;
      default:
        return false;
    }

    return KoLCharacter.getLimitMode() == LimitMode.NONE || EquipmentManager.canEquip(item);
  }

  private boolean filterWeapon(final AdventureResult weapon, final int slot) {
    if (KoLCharacter.inFistcore()) {
      return false;
    }

    if (slot == EquipmentManager.OFFHAND) {
      return this.filterOffhand(weapon, ConsumptionType.WEAPON);
    }

    if (KoLCharacter.inAxecore()) {
      return weapon.getItemId() == ItemPool.TRUSTY;
    }

    if (this.weapon1H.isSelected() && EquipmentDatabase.getHands(weapon.getItemId()) > 1) {
      return false;
    }

    if (this.weaponTypes[0].isSelected()) {
      return true;
    }

    return switch (EquipmentDatabase.getWeaponType(weapon.getItemId())) {
      case MELEE -> this.weaponTypes[1].isSelected();
      case RANGED -> this.weaponTypes[2].isSelected();
      default -> false;
    };
  }

  private boolean filterOffhand(final AdventureResult offhand, ConsumptionType consumption) {
    // In Fistcore, you must have both hands free.
    // In Axecore, you can equip only Trusty, a two-handed axe
    if (KoLCharacter.inFistcore() || KoLCharacter.inAxecore()) {
      return false;
    }

    int offhandId = offhand.getItemId();

    // Fake hands are handled specially
    if (offhandId == ItemPool.FAKE_HAND) {
      return false;
    }

    // Do not even consider weapons unless we can dual-wield
    if (consumption == ConsumptionType.WEAPON) {
      if (!KoLCharacter.hasSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING)) {
        return false;
      }

      // Only consider 1-handed weapons
      if (EquipmentDatabase.getHands(offhandId) != 1) {
        return false;
      }

      // There must be a current weapon
      AdventureResult weapon = this.currentOrSelectedItem(EquipmentManager.WEAPON);
      if (weapon == EquipmentRequest.UNEQUIP) {
        return false;
      }

      // The current weapon must be 1-handed
      int weaponId = weapon.getItemId();
      if (EquipmentDatabase.getHands(weaponId) != 1) {
        return false;
      }

      if (EquipmentDatabase.isMainhandOnly(offhandId)) {
        return false;
      }

      // The weapon types must agree
      if (EquipmentDatabase.getWeaponType(weaponId) != EquipmentDatabase.getWeaponType(offhandId)) {
        return false;
      }

      // Now check filters
    }

    if (this.offhandTypes[0].isSelected()) {
      return true;
    }

    if (consumption == ConsumptionType.WEAPON) {
      return this.offhandTypes[1].isSelected();
    }

    String type = EquipmentDatabase.getItemType(offhandId);
    if (this.offhandTypes[2].isSelected()) {
      // Shields
      return type.equals("shield");
    }

    if (this.offhandTypes[3].isSelected()) {
      // Everything Else
      return type.equals("offhand");
    }

    return false;
  }

  private boolean shouldAddItem(final AdventureResult item, final ConsumptionType type) {
    // Only add items of specified type
    if (type != ItemDatabase.getConsumptionType(item.getItemId())) {
      return false;
    }

    // Make sure we meet requirements in Limitmode, otherwise show (greyed out)
    return KoLCharacter.getLimitMode() == LimitMode.NONE
        || EquipmentManager.canEquip(item.getName());
  }

  private AdventureResult currentOrSelectedItem(final int slot) {
    AdventureResult item = (AdventureResult) this.equipment[slot].getSelectedItem();
    return (item != null) ? item : EquipmentManager.getEquipment(slot);
  }

  private static void updateEquipmentList(
      final LockableListModel<AdventureResult> currentItems,
      final List<AdventureResult> newItems,
      final AdventureResult equippedItem) {
    currentItems.retainAll(newItems);
    newItems.removeAll(currentItems);
    currentItems.addAll(newItems);
    currentItems.setSelectedItem(equippedItem);
  }

  private void updateEquipmentModelsInternal(final List<List<AdventureResult>> equipmentLists) {
    // For all the slots that we maintain a custom list, update the model specially
    for (int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot) {
      LockableListModel<AdventureResult> model = equipmentModels.get(slot);
      if (model == null) {
        continue;
      }

      if (slot == EquipmentManager.WEAPON || slot == EquipmentManager.OFFHAND) {
        if (KoLCharacter.inFistcore()) {
          this.equipment[slot].setEnabled(false);
          continue;
        }
      }

      List<AdventureResult> items = equipmentLists.get(slot);
      AdventureResult selectedItem = this.currentOrSelectedItem(slot);
      GearChangePanel.updateEquipmentList(model, items, selectedItem);
      this.equipment[slot].setEnabled(
          this.isEnabled && !KoLCharacter.getLimitMode().limitSlot(slot));

      if (slot == EquipmentManager.WEAPON) {
        // Equipping 2 or more handed weapon: nothing in off-hand
        if (EquipmentDatabase.getHands(selectedItem.getItemId()) > 1) {
          this.equipment[EquipmentManager.OFFHAND].setSelectedItem(EquipmentRequest.UNEQUIP);
          this.equipment[EquipmentManager.OFFHAND].setEnabled(false);
        }
      }
    }
  }

  private void updateEquipmentModels(final List<List<AdventureResult>> equipmentLists) {
    if (SwingUtilities.isEventDispatchThread() || GraphicsEnvironment.isHeadless()) {
      updateEquipmentModelsInternal(equipmentLists);
    } else {
      try {
        SwingUtilities.invokeAndWait(() -> updateEquipmentModelsInternal(equipmentLists));
      } catch (Exception ie) {
      }
    }
  }

  // For performance reasons, allow deferral of updateAllModels, which recomputes the lists that
  // show in GearChangeFrame. This way we can perform multiple equipment updates at once without
  // triggering several runs through the entire list of equipment in a player's inventory to see
  // if it is equip-able.
  public static void deferUpdate() {
    if (GearChangePanel.INSTANCE == null) {
      return;
    }

    GearChangePanel.INSTANCE.deferredUpdateLevel++;
  }

  public static void resolveDeferredUpdate() {
    if (GearChangePanel.INSTANCE == null) {
      return;
    }

    GearChangePanel.INSTANCE.deferredUpdateLevel--;
    if (GearChangePanel.INSTANCE.deferredUpdateLevel == 0) {
      GearChangePanel.INSTANCE.updateAllModels();
    }
  }

  private void updateAllModels() {
    // If we are still logging in, defer this
    if (KoLmafia.isRefreshing()) {
      return;
    }

    // If we are deferring updates to the GCF UI, wait.
    if (this.deferredUpdateLevel > 0) {
      return;
    }

    // Calculate all the AdventureResult lists
    List<List<AdventureResult>> equipmentLists = this.populateEquipmentLists();

    // Update the models in the Swing Thread
    this.updateEquipmentModels(equipmentLists);

    FamiliarData currentFamiliar = KoLCharacter.getFamiliar();
    FamiliarData selectedFamiliar = (FamiliarData) this.familiars.getSelectedItem();
    if (selectedFamiliar == null) {
      selectedFamiliar = currentFamiliar;
    }
    if (KoLCharacter.inPokefam() || KoLCharacter.inQuantum()) {
      this.familiarSelect.setEnabled(false);
    }

    FamiliarData enthronedFamiliar = KoLCharacter.getEnthroned();
    FamiliarData selectedThroneFamiliar = (FamiliarData) this.crownFamiliars.getSelectedItem();
    if (selectedThroneFamiliar == null) {
      selectedThroneFamiliar = enthronedFamiliar;
    }

    FamiliarData bjornedFamiliar = KoLCharacter.getBjorned();
    FamiliarData selectedBjornFamiliar = (FamiliarData) this.bjornFamiliars.getSelectedItem();
    if (selectedBjornFamiliar == null) {
      selectedBjornFamiliar = bjornedFamiliar;
    }

    this.updateFamiliarList(this.familiars, this.validFamiliars(currentFamiliar), selectedFamiliar);
    this.equipment[EquipmentManager.FAMILIAR].setEnabled(
        this.isEnabled
            && !KoLCharacter.getLimitMode().limitFamiliars()
            && !KoLCharacter.inPokefam());
    this.updateFamiliarList(
        this.crownFamiliars,
        this.carriableFamiliars(currentFamiliar, bjornedFamiliar),
        selectedThroneFamiliar);
    this.equipment[EquipmentManager.CROWNOFTHRONES].setEnabled(
        this.isEnabled
            && !KoLCharacter.getLimitMode().limitFamiliars()
            && !KoLCharacter.inPokefam());
    this.updateFamiliarList(
        this.bjornFamiliars,
        this.carriableFamiliars(currentFamiliar, enthronedFamiliar),
        selectedBjornFamiliar);
    this.equipment[EquipmentManager.BUDDYBJORN].setEnabled(
        this.isEnabled
            && !KoLCharacter.getLimitMode().limitFamiliars()
            && !KoLCharacter.inPokefam());

    this.outfitSelect.setEnabled(this.isEnabled && !KoLCharacter.getLimitMode().limitOutfits());
    this.customSelect.setEnabled(this.isEnabled && !KoLCharacter.getLimitMode().limitOutfits());
  }

  private List<FamiliarData> validFamiliars(final FamiliarData currentFamiliar) {
    List<FamiliarData> familiars = new ArrayList<>();

    for (FamiliarData fam : KoLCharacter.usableFamiliars()) {
      // Only add it once
      if (familiars.contains(fam)) {
        continue;
      }

      if (filterFamiliar(fam)) {
        familiars.add(fam);
      }
    }

    // Add the current familiar

    if (!familiars.contains(currentFamiliar) && filterFamiliar(currentFamiliar)) {
      familiars.add(currentFamiliar);
    }

    // Add "(none)"
    if (!familiars.contains(FamiliarData.NO_FAMILIAR)) {
      familiars.add(FamiliarData.NO_FAMILIAR);
    }

    return familiars;
  }

  private boolean filterFamiliar(final FamiliarData familiar) {
    return familiar.canEquip();
  }

  private List<FamiliarData> carriableFamiliars(
      final FamiliarData exclude1, final FamiliarData exclude2) {
    List<FamiliarData> familiars = new ArrayList<>();

    for (FamiliarData fam : KoLCharacter.usableFamiliars()) {
      // Cannot carry a familiar if it is current familiar or is carried elsewhere
      if (fam == exclude1 || fam == exclude2) {
        continue;
      }

      // Certain familiars cannot be carried
      if (!fam.canCarry()) {
        continue;
      }

      // Certain familiars can not be equipped in certain paths
      if (!fam.canEquip()) {
        continue;
      }

      // Only add it once
      if (familiars.contains(fam)) {
        continue;
      }

      familiars.add(fam);
    }

    // Add "(none)"
    if (!familiars.contains(FamiliarData.NO_FAMILIAR)) {
      familiars.add(FamiliarData.NO_FAMILIAR);
    }

    return familiars;
  }

  private void updateFamiliarList(
      final LockableListModel<FamiliarData> currentFamiliars,
      final List<FamiliarData> newFamiliars,
      final FamiliarData activeFamiliar) {
    currentFamiliars.retainAll(newFamiliars);
    newFamiliars.removeAll(currentFamiliars);
    currentFamiliars.addAll(newFamiliars);
    currentFamiliars.setSelectedItem(activeFamiliar);
  }

  private static class FakeHandsSpinner extends AutoHighlightSpinner
      implements ChangeListener, Listener {
    private int currentFakeHands = 0;
    private int availableFakeHands = 0;

    public FakeHandsSpinner() {
      super();
      this.addChangeListener(this);
      NamedListenerRegistry.registerNamedListener("(fakehands)", this);
      this.update();
    }

    @Override
    public void stateChanged(final ChangeEvent e) {
      int maximum = this.availableFakeHands;
      if (maximum == 0) {
        this.setValue(0);
        return;
      }

      int desired = InputFieldUtilities.getValue(this, maximum);
      if (desired == maximum + 1) {
        this.setValue(0);
      } else if (desired < 0 || desired > maximum) {
        this.setValue(maximum);
      }
    }

    public int getAvailableFakeHands() {
      return this.availableFakeHands;
    }

    @Override
    public void update() {
      int available = EquipmentManager.FAKE_HAND.getCount(KoLConstants.inventory);
      this.currentFakeHands = EquipmentManager.getFakeHands();
      this.availableFakeHands = this.currentFakeHands + available;
      this.setValue(this.currentFakeHands);
    }
  }

  private static class FamLockCheckbox extends JCheckBox implements Listener {
    public FamLockCheckbox() {
      super("familiar item locked");
      this.addActionListener(new LockFamiliarItemListener());
      NamedListenerRegistry.registerNamedListener("(familiarLock)", this);
      this.update();
    }

    private static class LockFamiliarItemListener extends ThreadedListener {
      @Override
      protected void execute() {
        RequestThread.postRequest(new FamiliarRequest(true));
      }
    }

    @Override
    public void update() {
      this.setSelected(EquipmentManager.familiarItemLocked());
      this.setEnabled(this.isEnabled());
    }

    @Override
    public void setEnabled(final boolean isEnabled) {
      super.setEnabled(isEnabled && EquipmentManager.familiarItemLockable());
    }
  }
}
