package net.sourceforge.kolmafia.request;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EquipmentRequest extends PasswordHashRequest {
  private static final Pattern CELL_PATTERN = Pattern.compile("<td>(.*?)</td>");

  // With images:
  //
  // <table class='item' id="ic653" rel="id=653&s=0&q=1&d=0&g=0&t=0&n=1&m=0&u=."><td
  // class="img"><img src="http://images.kingdomofloathing.com/itemimages/airboat.gif" class="hand
  // ircm" onClick='descitem(126122919,0, event);'></td><td id='i653' valign=top><b
  // class="ircm">intragalactic rowboat</b>&nbsp;<span></span><font size=1><br></font></td></table>
  //
  // Without images:
  //
  // <table class='item' id="ic653" rel="id=653&s=0&q=1&d=0&g=0&t=0&n=1&m=0&u=."><td id='i653'
  // valign=top><b class="ircm"><a onClick='javascript:descitem(126122919,0, event);'>intragalactic
  // rowboat</a></b>&nbsp;<span></span><font size=1><br></font></td></table>

  private static final Pattern HAT_PATTERN =
      Pattern.compile(
          "Hat</a>:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>(.*?)</b>.*?unequip&type=hat");
  private static final Pattern WEAPON_PATTERN =
      Pattern.compile(
          "Weapon</a>:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>(.*?)</b>.*?unequip&type=weapon");
  private static final Pattern HOLSTER_PATTERN =
      Pattern.compile(
          "Holstered</a>:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>(.*?)</b>.*?action=holster&holster=0");
  private static final Pattern OFFHAND_PATTERN =
      Pattern.compile(
          "Off-Hand</a>:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>(.*?)</b>.*?unequip&type=offhand");
  private static final Pattern CONTAINER_PATTERN =
      Pattern.compile(
          "Back</a>:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>(.*?)</b>.*?unequip&type=container");
  private static final Pattern SHIRT_PATTERN =
      Pattern.compile(
          "Shirt</a>:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>(.*?)</b>.*?unequip&type=shirt");
  private static final Pattern PANTS_PATTERN =
      Pattern.compile(
          "Pants</a>:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>(.*?)</b>.*?unequip&type=pants");
  private static final Pattern ACC1_PATTERN =
      Pattern.compile(
          "Accessory</a>(?:&nbsp;1)?:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>([^<]+)</b> *<a[^>]*unequip&type=acc1");
  private static final Pattern ACC2_PATTERN =
      Pattern.compile(
          "Accessory</a>(?:&nbsp;2)?:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>([^<]+)</b> *<a[^>]*unequip&type=acc2");
  private static final Pattern ACC3_PATTERN =
      Pattern.compile(
          "Accessory</a>(?:&nbsp;3)?:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>([^<]+)</b> *<a[^>]*unequip&type=acc3");
  private static final Pattern FAMILIARITEM_PATTERN =
      Pattern.compile(
          "Familiar</a>:</td>(<td><img[^']*'descitem\\((\\d+)[^>]*></td>)?<td><b[^>]*>(.*?)</b>.*?unequip&type=familiarequip\"");
  private static final Pattern OUTFITLIST_PATTERN =
      Pattern.compile("<select name=whichoutfit>.*?</select>");
  private static final Pattern STICKER_PATTERN =
      Pattern.compile(
          "<td>\\s*(shiny|dull)?\\s*([^<]+)<a [^>]+action=peel|<td>\\s*<img [^>]+magnify");
  private static final Pattern FOLDER_PATTERN = Pattern.compile("folders/folder(\\d+).gif");

  private static final Pattern OUTFIT_ACTION_PATTERN = Pattern.compile("([a-zA-Z])=([^=]+)(?!=)");

  private static final Pattern OUTFIT_PATTERN = Pattern.compile("whichoutfit=(-?\\d+|last)");
  private static final Pattern SLOT_PATTERN = Pattern.compile("type=([a-z123]+)");
  private static final Pattern STICKERITEM_PATTERN = Pattern.compile("sticker=(\\d+)");
  private static final Pattern SLOT1_PATTERN = Pattern.compile("slot=(\\d+)");
  private static final Pattern OUTFITNAME_PATTERN = Pattern.compile("outfitname=([^&]*)");
  private static final Pattern OUTFITID_PATTERN = Pattern.compile("outfitid: (\\d+)");

  private static final Pattern EQUIPPED_PATTERN =
      Pattern.compile(
          "(?:Item equipped|equip an item):</td><td>.*?descitem\\((.*?)\\)'> <b>(.*?)</b></td>");
  private static final Pattern UNEQUIPPED_PATTERN =
      Pattern.compile("Item unequipped:</td><td>.*?descitem\\((.*?)\\)'> <b>(.*?)</b></td>");
  private static final Pattern HOLSTER_URL_PATTERN = Pattern.compile("holster=(\\d+)");

  public static final AdventureResult UNEQUIP = ItemPool.get("(none)", 1);

  public enum EquipmentRequestType {
    REFRESH,
    EQUIPMENT,

    SAVE_OUTFIT,
    CHANGE_OUTFIT,

    CHANGE_ITEM,
    REMOVE_ITEM,
    UNEQUIP_ALL,

    BEDAZZLEMENTS
  }

  private EquipmentRequestType requestType;
  private Slot equipmentSlot;
  private AdventureResult changeItem;
  private int itemId;
  private ConsumptionType equipmentType;
  private SpecialOutfit outfit;
  private String outfitName;
  private String error;

  private static int customOutfitId = 0;

  public EquipmentRequest(final EquipmentRequestType requestType) {
    super(EquipmentRequest.choosePage(requestType));

    this.requestType = requestType;
    this.outfit = null;
    this.outfitName = null;
    this.error = null;

    // Otherwise, add the form field indicating which page
    // of the inventory you want to request

    switch (requestType) {
      case EQUIPMENT:
        this.addFormField("which", "2");
        break;
      case BEDAZZLEMENTS:
        // no fields necessary
        break;
      case SAVE_OUTFIT:
        this.addFormField("ajax", "1");
        this.addFormField("which", "2");
        break;
      case UNEQUIP_ALL:
        this.addFormField("ajax", "1");
        this.addFormField("which", "2");
        this.addFormField("action", "unequipall");
        break;
    }
  }

  private static String choosePage(final EquipmentRequestType requestType) {
    return switch (requestType) {
      case BEDAZZLEMENTS -> "bedazzle.php";
      case SAVE_OUTFIT, UNEQUIP_ALL -> "inv_equip.php";
      default -> "inventory.php";
    };
  }

  public EquipmentRequest(final String changeName) {
    this(EquipmentRequestType.SAVE_OUTFIT);
    this.addFormField("action", "customoutfit");
    this.addFormField("outfitname", changeName);
    this.addFormField("ajax", "1");
    this.outfitName = changeName;
  }

  public EquipmentRequest(final AdventureResult changeItem) {
    this(changeItem, EquipmentRequest.chooseEquipmentSlot(changeItem.getItemId()));
  }

  public EquipmentRequest(final AdventureResult changeItem, final Slot equipmentSlot) {
    super(EquipmentRequest.chooseEquipmentLocation(equipmentSlot));

    this.error = null;

    switch (equipmentSlot) {
      case CROWNOFTHRONES:
        this.error = "Cannot change enthronement using equip command; use enthrone command instead";
        break;
      case BUDDYBJORN:
        this.error =
            "Cannot change bjorned familiar using equip command; use bjornify command instead";
        break;
      case STICKER1:
      case STICKER2:
      case STICKER3:
        this.initializeStickerData(changeItem, equipmentSlot);
        break;
      case CARDSLEEVE:
        this.initializeCardSleeveData(changeItem);
        break;
      case FOLDER1:
      case FOLDER2:
      case FOLDER3:
      case FOLDER4:
      case FOLDER5:
        this.initializeFolderData(changeItem, equipmentSlot);
        break;
      case BOOTSKIN:
      case BOOTSPUR:
        this.initializeBootData(changeItem, equipmentSlot);
        break;
      case HOLSTER:
        this.initializeSixgunData(changeItem, equipmentSlot);
        break;
      default:
        this.initializeChangeData(changeItem, equipmentSlot);
    }
  }

  public EquipmentRequest(final SpecialOutfit change) {
    super("inv_equip.php");

    this.addFormField("which", "2");
    this.addFormField("action", "outfit");
    this.addFormField(
        "whichoutfit",
        change == SpecialOutfit.PREVIOUS_OUTFIT ? "last" : String.valueOf(change.getOutfitId()));
    this.addFormField("ajax", "1");

    this.requestType = EquipmentRequestType.CHANGE_OUTFIT;
    this.outfit = change;
    this.error = null;
  }

  private static String chooseEquipmentLocation(final Slot slot) {
    return slot == Slot.HOLSTER
        ? "inventory.php"
        : SlotSet.SLOTS.contains(slot)
            ? "inv_equip.php"
            : slot == Slot.CROWNOFTHRONES || slot == Slot.BUDDYBJORN
                ? "bogus.php"
                : SlotSet.STICKER_SLOTS.contains(slot)
                    ? "bedazzle.php"
                    : slot == Slot.CARDSLEEVE
                        ? "inv_use.php"
                        : slot == Slot.FAKEHAND
                            ? "inv_equip.php"
                            : SlotSet.FOLDER_SLOTS.contains(slot)
                                ? "choice.php"
                                : (slot == Slot.BOOTSKIN || slot == Slot.BOOTSPUR)
                                    ? "inv_use.php"
                                    : "bogus.php";
  }

  public static boolean isEquipmentChange(final String path) {
    return path.startsWith("inv_equip.php")
        &&
        // Saving a custom outfit is OK
        !path.contains("action=customoutfit");
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  private void initializeChangeData(final AdventureResult changeItem, final Slot equipmentSlot) {
    this.addFormField("which", "2");
    this.addFormField("ajax", "1");
    this.equipmentSlot = equipmentSlot;

    if (changeItem.equals(EquipmentRequest.UNEQUIP)) {
      this.requestType = EquipmentRequestType.REMOVE_ITEM;
      this.addFormField("action", "unequip");
      this.addFormField("type", equipmentSlot.phpName);
      return;
    }

    // Find out what item is being equipped
    this.itemId = changeItem.getItemId();

    // Find out what kind of item it is
    this.equipmentType = ItemDatabase.getConsumptionType(this.itemId);

    if (this.equipmentSlot == Slot.NONE) {
      this.error = "No suitable slot available for " + changeItem;
      return;
    }

    // Make sure you can equip it in the requested slot
    String action = this.getAction();
    if (action == null) {
      return;
    }

    this.requestType = EquipmentRequestType.CHANGE_ITEM;
    this.changeItem = changeItem.getCount() == 1 ? changeItem : changeItem.getInstance(1);

    this.addFormField("action", action);
    this.addFormField("whichitem", String.valueOf(this.itemId));
  }

  private void initializeStickerData(final AdventureResult sticker, final Slot equipmentSlot) {
    this.equipmentSlot = equipmentSlot;
    var slot =
        switch (equipmentSlot) {
          case STICKER1 -> "1";
          case STICKER2 -> "2";
          case STICKER3 -> "3";
          default -> throw new IllegalStateException("Unexpected value: " + equipmentSlot);
        };
    this.addFormField("slot", slot);

    if (sticker.equals(EquipmentRequest.UNEQUIP)) {
      this.requestType = EquipmentRequestType.REMOVE_ITEM;
      this.addFormField("action", "peel");
      return;
    }

    // Find out what item is being equipped
    this.itemId = sticker.getItemId();

    // Find out what kind of item it is
    this.equipmentType = ItemDatabase.getConsumptionType(this.itemId);

    if (this.equipmentType != ConsumptionType.STICKER) {
      this.error =
          "You can't equip a " + ItemDatabase.getItemName(this.itemId) + " in a sticker slot.";
      return;
    }

    this.addFormField("sticker", String.valueOf(this.itemId));
    this.requestType = EquipmentRequestType.CHANGE_ITEM;
    this.changeItem = sticker.getCount() == 1 ? sticker : sticker.getInstance(1);

    if (EquipmentManager.hasStickerWeapon()) {
      this.addFormField("action", "stick");
    } else {
      this.addFormField("action", "juststick");
      this.removeFormField("slot");
    }
  }

  private void initializeCardSleeveData(final AdventureResult card) {
    this.equipmentSlot = Slot.CARDSLEEVE;
    this.addFormField("whichitem", String.valueOf(ItemPool.CARD_SLEEVE));

    if (card.equals(EquipmentRequest.UNEQUIP)) {
      this.requestType = EquipmentRequestType.REMOVE_ITEM;
      this.addFormField("removecard", "1");
      return;
    }

    // Find out what item is being equipped
    this.itemId = card.getItemId();

    // Find out what kind of item it is
    this.equipmentType = ItemDatabase.getConsumptionType(this.itemId);

    if (this.equipmentType != ConsumptionType.CARD) {
      this.error =
          "You can't slide a " + ItemDatabase.getItemName(this.itemId) + " into a card sleeze.";
      return;
    }

    this.addFormField("sleevecard", String.valueOf(this.itemId));
    this.requestType = EquipmentRequestType.CHANGE_ITEM;
    this.changeItem = card.getCount() == 1 ? card : card.getInstance(1);
  }

  private void initializeFolderData(final AdventureResult folder, final Slot slot) {
    this.equipmentSlot = slot;
    this.addFormField("whichchoice", "774");

    if (folder.equals(EquipmentRequest.UNEQUIP)) {
      this.requestType = EquipmentRequestType.REMOVE_ITEM;
      var slotOpt =
          switch (equipmentSlot) {
            case FOLDER1 -> "0";
            case FOLDER2 -> "1";
            case FOLDER3 -> "2";
            case FOLDER4 -> "3";
            case FOLDER5 -> "4";
            default -> throw new IllegalStateException("Unexpected value: " + equipmentSlot);
          };
      this.addFormField("slot", slotOpt);
      this.addFormField("option", "2");
      return;
    }

    if (SlotSet.FOLDER_SLOTS.stream()
        .filter(s -> s != slot)
        .map(EquipmentManager::getEquipment)
        .anyMatch(folder::equals)) {
      this.error = "You can't equip two of the same folder";
      return;
    }

    // Find out what item is being equipped
    this.itemId = folder.getItemId();

    // Find out what kind of item it is
    this.equipmentType = ItemDatabase.getConsumptionType(this.itemId);

    if (this.equipmentType != ConsumptionType.FOLDER) {
      this.error =
          "You can't equip a " + ItemDatabase.getItemName(this.itemId) + " in a folder slot.";
      return;
    }

    this.requestType = EquipmentRequestType.CHANGE_ITEM;
    this.changeItem = folder;
    this.addFormField("option", "1");
    this.addFormField("folder", String.valueOf(this.itemId - ItemPool.FOLDER_01 + 1));
  }

  private void initializeBootData(final AdventureResult decoration, final Slot slot) {
    this.equipmentSlot = slot;

    if (decoration.equals(EquipmentRequest.UNEQUIP)) {
      this.error = "You can't undecorate your cowboy boots.";
      return;
    }

    // Find out what item is being equipped
    this.itemId = decoration.getItemId();

    // Find out what kind of item it is
    this.equipmentType = ItemDatabase.getConsumptionType(this.itemId);

    if (this.equipmentType != ConsumptionType.BOOTSKIN
        && this.equipmentType != ConsumptionType.BOOTSPUR) {
      this.error =
          "You can't equip a " + ItemDatabase.getItemName(this.itemId) + " on your cowboy boots.";
      return;
    }

    if (this.equipmentSlot == Slot.BOOTSKIN && this.equipmentType == ConsumptionType.BOOTSPUR) {
      this.error = ItemDatabase.getItemName(this.itemId) + " is a spur, not a skin.";
      return;
    }

    if (this.equipmentSlot == Slot.BOOTSPUR && this.equipmentType == ConsumptionType.BOOTSKIN) {
      this.error = ItemDatabase.getItemName(this.itemId) + " is a skin, not a spur.";
      return;
    }

    this.requestType = EquipmentRequestType.CHANGE_ITEM;
    this.changeItem = decoration;
    this.addFormField("whichitem", String.valueOf(this.itemId));
    this.addFormField("ajax", "1");

    UseItemRequest.setLastItemUsed(decoration);
  }

  private void initializeSixgunData(final AdventureResult sixgun, final Slot slot) {
    this.equipmentSlot = slot;

    if (sixgun.equals(EquipmentRequest.UNEQUIP)) {
      this.requestType = EquipmentRequestType.REMOVE_ITEM;
      this.addFormField("which", "2h");
      this.addFormField("action", "holster");
      this.addFormField("holster", "0");
      this.addFormField("ajax", "1");
      return;
    }

    // Find out what item is being equipped
    this.itemId = sixgun.getItemId();

    // Find out what kind of item it is
    this.equipmentType = ItemDatabase.getConsumptionType(this.itemId);

    if (this.equipmentType != ConsumptionType.SIXGUN) {
      this.error = "You can't holster a " + ItemDatabase.getItemName(this.itemId);
      return;
    }

    this.requestType = EquipmentRequestType.CHANGE_ITEM;
    this.changeItem = sixgun;

    this.addFormField("which", "2");
    this.addFormField("action", "holster");
    this.addFormField("holster", String.valueOf(this.itemId));
    this.addFormField("ajax", "1");
  }

  private String getAction() {
    switch (this.equipmentSlot) {
      case HAT -> {
        if (this.equipmentType == ConsumptionType.HAT) {
          return "equip";
        }
      }
      case WEAPON -> {
        if (this.equipmentType == ConsumptionType.WEAPON) {
          return "equip";
        }
      }
      case OFFHAND -> {
        if (this.equipmentType == ConsumptionType.OFFHAND) {
          return "equip";
        }
        if (this.equipmentType == ConsumptionType.WEAPON
            && EquipmentDatabase.getHands(this.itemId) == 1) {
          return "dualwield";
        }
      }
      case CONTAINER -> {
        if (this.equipmentType == ConsumptionType.CONTAINER) {
          return "equip";
        }
      }
      case SHIRT -> {
        if (this.equipmentType == ConsumptionType.SHIRT) {
          return "equip";
        }
      }
      case PANTS -> {
        if (this.equipmentType == ConsumptionType.PANTS) {
          return "equip";
        }
      }
      case ACCESSORY1 -> {
        if (this.equipmentType == ConsumptionType.ACCESSORY) {
          this.addFormField("slot", "1");
          return "equip";
        }
      }
      case ACCESSORY2 -> {
        if (this.equipmentType == ConsumptionType.ACCESSORY) {
          this.addFormField("slot", "2");
          return "equip";
        }
      }
      case ACCESSORY3 -> {
        if (this.equipmentType == ConsumptionType.ACCESSORY) {
          this.addFormField("slot", "3");
          return "equip";
        }
      }
      case FAMILIAR -> {
        switch (this.equipmentType) {
          case FAMILIAR_EQUIPMENT -> {
            return "equip";
          }
          case HAT, WEAPON, OFFHAND, PANTS -> {
            return "hatrack";
          }
        }
      }
      case FAKEHAND -> {
        if (this.itemId == ItemPool.FAKE_HAND) {
          return "equip";
        }
      }
      default -> {
        return "equip";
      }
    }

    this.error =
        "You can't equip a "
            + ItemDatabase.getItemName(this.itemId)
            + " in the "
            + this.equipmentSlot.name
            + " slot.";

    return null;
  }

  public static final Slot chooseEquipmentSlot(final int itemId) {
    switch (itemId) {
      case ItemPool.SPELUNKY_SPRING_BOOTS, ItemPool.SPELUNKY_SPIKED_BOOTS -> {
        // Spelunky only has one "accessory" slot
        return Slot.ACCESSORY1;
      }
      case ItemPool.WORK_BOOTS, ItemPool.FANCY_BOOTS -> {
        // These are mutually exclusive. If you are wearing one and
        // want to equip the other, choose the same accessory slot
        Slot slot = KoLCharacter.equipmentSlot(ItemPool.get(ItemPool.WORK_BOOTS, 1));
        if (slot != Slot.NONE) {
          return slot;
        }
        slot = KoLCharacter.equipmentSlot(ItemPool.get(ItemPool.FANCY_BOOTS, 1));
        if (slot != Slot.NONE) {
          return slot;
        }
        return EquipmentRequest.availableAccessory();
      }
    }

    ConsumptionType equipmentType = ItemDatabase.getConsumptionType(itemId);
    return switch (equipmentType) {
      case HAT -> Slot.HAT;
      case WEAPON -> Slot.WEAPON;
      case OFFHAND -> itemId == ItemPool.FAKE_HAND ? Slot.FAKEHAND : Slot.OFFHAND;
      case CONTAINER -> Slot.CONTAINER;
      case SHIRT -> Slot.SHIRT;
      case PANTS -> Slot.PANTS;
      case ACCESSORY -> EquipmentRequest.availableAccessory();
      case FAMILIAR_EQUIPMENT -> Slot.FAMILIAR;
      case STICKER -> EquipmentRequest.availableSticker();
      case CARD -> Slot.CARDSLEEVE;
      case FOLDER -> EquipmentRequest.availableFolder();
      case SIXGUN -> Slot.HOLSTER;
      default -> Slot.NONE;
    };
  }

  private static Slot availableAccessory() {
    AdventureResult test = EquipmentManager.getEquipment(Slot.ACCESSORY1);
    if (test == null || test.equals(EquipmentRequest.UNEQUIP)) {
      return Slot.ACCESSORY1;
    }

    test = EquipmentManager.getEquipment(Slot.ACCESSORY2);
    if (test == null || test.equals(EquipmentRequest.UNEQUIP)) {
      return Slot.ACCESSORY2;
    }

    test = EquipmentManager.getEquipment(Slot.ACCESSORY3);
    if (test == null || test.equals(EquipmentRequest.UNEQUIP)) {
      return Slot.ACCESSORY3;
    }

    // All accessory slots are in use. Pick #1
    return Slot.ACCESSORY1;
  }

  private static Slot availableSlot(final Set<Slot> slots) {
    for (var slot : slots) {
      AdventureResult test = EquipmentManager.getEquipment(slot);
      if (test == null || test.equals(EquipmentRequest.UNEQUIP)) {
        return slot;
      }
    }

    // All slots are in use.  Abort rather than risk peeling the wrong one.
    return Slot.NONE;
  }

  private static Slot availableSticker() {
    return EquipmentRequest.availableSlot(SlotSet.STICKER_SLOTS);
  }

  public static Slot availableFolder() {
    return EquipmentRequest.availableSlot(
        KoLCharacter.inHighschool() ? SlotSet.FOLDER_SLOTS : SlotSet.FOLDER_SLOTS_AFTERCORE);
  }

  /**
   * Executes the <code>EquipmentRequest</code>. Note that at the current time, only the character's
   * currently equipped items and familiar item will be stored.
   */
  @Override
  public void run() {
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    if (this.requestType == EquipmentRequestType.REFRESH) {
      InventoryManager.refresh();
      return;
    }

    // If we were given bogus parameters, report the error now
    if (this.error != null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, this.error);
      return;
    }

    // Outfit changes are a bit quirky, so they're handled
    // first for easy visibility.

    if (this.requestType == EquipmentRequestType.CHANGE_OUTFIT) {
      // If this is a birthday suit outfit, then remove everything.
      if (this.outfit == SpecialOutfit.BIRTHDAY_SUIT) {
        // See if you are wearing anything.
        boolean found = false;
        for (var slot : SlotSet.CORE_EQUIP_SLOTS) {
          if (!EquipmentManager.getEquipment(slot).equals(EquipmentRequest.UNEQUIP)) {
            found = true;
            break;
          }
        }

        // If not, nothing to do
        if (!found) {
          return;
        }

        // Tell KoL to unequip everything
        (new EquipmentRequest(EquipmentRequestType.UNEQUIP_ALL)).run();

        return;
      } else if (this.outfit == SpecialOutfit.PREVIOUS_OUTFIT) {
        // If we are donning KoL's idea of your previous
        // outfit, we have no idea what the pieces are.

        // *** KoL bug: whichoutfit=last doesn't actually work
        // *** Therefore, look up the actual outfit id.
        CustomOutfitRequest request = new CustomOutfitRequest(true);
        request.run();
        int previousOutfitId = request.getPreviousOutfitId();
        if (previousOutfitId >= 0) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "No previous outfit saved");
          return;
        }
        this.addFormField("whichoutfit", String.valueOf(previousOutfitId));
      } else {
        // Otherwise, try to retrieve them.

        // If you are already wearing the outfit, nothing to do
        if (EquipmentManager.isWearingOutfit(this.outfit)) {
          return;
        }

        // Make sure we have all the pieces
        if (!EquipmentManager.retrieveOutfit(this.outfit)) {
          return;
        }
      }
    }

    if (this.requestType == EquipmentRequestType.CHANGE_ITEM) {
      // Do not submit a request if the item matches what you
      // want to equip on the character.

      if (EquipmentManager.getEquipment(this.equipmentSlot).equals(this.changeItem)) {
        return;
      }

      // If we are equipping a new weapon, a two-handed
      // weapon will unequip any pair of weapons. But a
      // one-handed weapon much match the type of the
      // off-hand weapon. If it doesn't, unequip the off-hand
      // weapon first

      int itemId = this.changeItem.getItemId();

      if (this.equipmentSlot == Slot.WEAPON && EquipmentDatabase.getHands(itemId) == 1) {
        int offhand = EquipmentManager.getEquipment(Slot.OFFHAND).getItemId();

        if (ItemDatabase.getConsumptionType(offhand) == ConsumptionType.WEAPON
            && EquipmentDatabase.getWeaponType(itemId)
                != EquipmentDatabase.getWeaponType(offhand)) {
          (new EquipmentRequest(EquipmentRequest.UNEQUIP, Slot.OFFHAND)).run();
        }
      }

      // If you are equipping an off-hand weapon, don't
      // bother trying if unless it is compatible with the
      // main weapon.

      if (this.equipmentSlot == Slot.OFFHAND) {
        ConsumptionType itemType = ItemDatabase.getConsumptionType(itemId);
        AdventureResult weapon = EquipmentManager.getEquipment(Slot.WEAPON);
        int weaponItemId = weapon.getItemId();

        if (itemType == ConsumptionType.WEAPON && weaponItemId <= 0) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "You can't dual wield unless you already have a main weapon.");
          return;
        }

        if (EquipmentDatabase.getHands(weaponItemId) > 1) {
          String message =
              itemType == ConsumptionType.WEAPON
                  ? ("You can't wield a "
                      + this.changeItem.getName()
                      + " in your off-hand while wielding a 2-handed weapon.")
                  : ("You can't equip a "
                      + this.changeItem.getName()
                      + " in your off-hand while wielding a 2-handed weapon.");
          KoLmafia.updateDisplay(MafiaState.ERROR, message);
          return;
        }

        if (itemType == ConsumptionType.WEAPON
            && EquipmentDatabase.getWeaponType(itemId)
                != EquipmentDatabase.getWeaponType(weaponItemId)) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR,
              "You can't hold a "
                  + this.changeItem.getName()
                  + " in your off-hand when wielding a "
                  + weapon.getName());
          return;
        }
      }

      if (!InventoryManager.retrieveItem(this.changeItem)) {
        return;
      }

      // Must remove an existing sticker or folder before
      // installing a new one in the same slot.
      if ((SlotSet.STICKER_SLOTS.contains(this.equipmentSlot)
              || SlotSet.FOLDER_SLOTS.contains(this.equipmentSlot))
          && !EquipmentManager.getEquipment(this.equipmentSlot).equals(EquipmentRequest.UNEQUIP)) {
        (new EquipmentRequest(EquipmentRequest.UNEQUIP, this.equipmentSlot)).run();
      }
    }

    if (this.requestType == EquipmentRequestType.REMOVE_ITEM
        && equipmentSlot != Slot.FAKEHAND
        && EquipmentManager.getEquipment(this.equipmentSlot).equals(EquipmentRequest.UNEQUIP)) {
      return;
    }

    if (SlotSet.FOLDER_SLOTS.contains(this.equipmentSlot)) {
      (new GenericRequest("inventory.php?action=useholder")).run();
    }

    switch (this.requestType) {
      case EQUIPMENT -> KoLmafia.updateDisplay("Retrieving equipment...");
      case BEDAZZLEMENTS -> KoLmafia.updateDisplay("Refreshing stickers...");
      case SAVE_OUTFIT -> KoLmafia.updateDisplay("Saving outfit: " + this.outfitName);
      case CHANGE_OUTFIT -> KoLmafia.updateDisplay("Putting on outfit: " + this.outfit);
      case CHANGE_ITEM -> KoLmafia.updateDisplay(
          (this.equipmentSlot == Slot.WEAPON
                  ? "Wielding "
                  : this.equipmentSlot == Slot.OFFHAND
                      ? "Holding "
                      : this.equipmentSlot == Slot.CARDSLEEVE
                          ? "Sliding in "
                          : this.equipmentSlot == Slot.HOLSTER ? "Holstering " : "Putting on ")
              + ItemDatabase.getItemName(this.itemId)
              + "...");
      case REMOVE_ITEM -> KoLmafia.updateDisplay(
          (this.equipmentSlot == Slot.CARDSLEEVE
                  ? "Sliding out "
                  : this.equipmentSlot == Slot.HOLSTER ? "Unholstering " : "Taking off ")
              + (this.equipmentSlot == Slot.FAKEHAND
                  ? "fake hands"
                  : EquipmentManager.getEquipment(this.equipmentSlot).getName())
              + "...");
      case UNEQUIP_ALL -> KoLmafia.updateDisplay("Taking off everything...");
    }

    // You can only change a card in the card sleeve while it is in inventory
    boolean changeCardSleeve =
        this.equipmentSlot == Slot.CARDSLEEVE
            && KoLCharacter.hasEquipped(EquipmentManager.CARD_SLEEVE);

    if (changeCardSleeve) {
      RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, Slot.OFFHAND));
    }

    super.run();

    if (!KoLmafia.permitsContinue()) {
      return;
    }

    if (changeCardSleeve) {
      RequestThread.postRequest(new EquipmentRequest(EquipmentManager.CARD_SLEEVE, Slot.OFFHAND));
    }

    switch (this.requestType) {
      case REFRESH -> {
        return;
      }
      case SAVE_OUTFIT -> {
        KoLmafia.updateDisplay("Outfit saved");
        return;
      }
      case CHANGE_ITEM, CHANGE_OUTFIT, REMOVE_ITEM -> KoLmafia.updateDisplay("Equipment changed.");
      case UNEQUIP_ALL -> KoLmafia.updateDisplay("Everything removed.");
    }
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    String responseText = this.responseText;

    if (urlString.startsWith("bedazzle.php")) {
      EquipmentRequest.parseBedazzlements(responseText);
      return;
    }

    if (urlString.startsWith("choice.php") && urlString.contains("whichchoice=774")) {
      EquipmentRequest.parseFolders(responseText);
      return;
    }

    if (this.equipmentSlot == Slot.BOOTSKIN || this.equipmentSlot == Slot.BOOTSPUR) {
      UseItemRequest.parseConsumption("", false);
      return;
    }

    switch (this.requestType) {
      case REFRESH -> {
        return;
      }
      case EQUIPMENT -> {
        EquipmentRequest.parseEquipment(urlString, responseText);
        return;
      }
      case CHANGE_ITEM, CHANGE_OUTFIT -> {
        String text = this.responseText == null ? "" : this.responseText;
        // What SHOULD we do if get a null responseText?

        Matcher resultMatcher = EquipmentRequest.CELL_PATTERN.matcher(text);
        if (resultMatcher.find()) {
          String result = resultMatcher.group(1).replaceAll("</?b>", "");
          if (result.contains("You are already wearing")) {
            // Not an error
            KoLmafia.updateDisplay(result);
            return;
          }

          // It appears you're already wearing all the
          // parts of the outfit 'outfitname' which you
          // possess or can wear.	 ... followed by a
          // table of missing pieces

          if (result.contains("which you possess or can wear")) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, "You're already wearing as much of that outfit as you can.");
            return;
          }

          if (this.equipmentSlot != Slot.HOLSTER
              && !result.contains("You put")
              && !result.contains("You equip")
              && !result.contains("Item equipped")
              && !result.contains("equips an item")
              && !result.contains("as you put it on")
              && !result.contains("You take the existing card out of the sleeve to make room")
              && !result.contains("You apply the shiny sticker")
              && !result.contains("fold it into an impromptu sword")) {
            KoLmafia.updateDisplay(MafiaState.ERROR, result);
            return;
          }
        }
        if (this.equipmentSlot == Slot.CARDSLEEVE) {
          EquipmentRequest.parseCardSleeve(responseText);
        } else if (this.getURLString().contains("ajax=1")) {
          if (EquipmentRequest.parseEquipmentChange(urlString, responseText)) {
            this.setHasResult(false);
          }
        } else if (this.equipmentSlot == Slot.HOLSTER) {
          // This redirects to equipment.php?action=message
          EquipmentRequest.parseEquipment(urlString, responseText);
        }
        return;
      }
      case SAVE_OUTFIT, REMOVE_ITEM, UNEQUIP_ALL -> {
        if (this.equipmentSlot == Slot.CARDSLEEVE) {
          EquipmentRequest.parseCardSleeve(responseText);
        } else if (this.getURLString().contains("ajax=1")) {
          EquipmentRequest.parseEquipmentChange(urlString, responseText);
        } else if (this.equipmentSlot == Slot.HOLSTER) {
          // This redirects to equipment.php?action=message
          // with the previous equipment page of the inventory.
          // I.e., with a sixgun holstered.
          EquipmentManager.setEquipment(Slot.HOLSTER, EquipmentRequest.UNEQUIP);
        }
        return;
      }
    }
  }

  private static boolean switchItem(final AdventureResult oldItem, final AdventureResult newItem) {
    // If the items are not equivalent, make sure
    // the items should get switched out.

    int oldItemId = oldItem.getItemId();
    int newItemId = newItem.getItemId();

    if (oldItemId == newItemId) {
      return false;
    }

    if (KoLCharacter.isPlumber()) {
      // If we put on or removed power pants, our maximum PP changes
      int delta =
          (oldItemId == ItemPool.POWER_PANTS) ? -1 : (newItemId == ItemPool.POWER_PANTS) ? 1 : 0;
      if (delta != 0) {
        KoLCharacter.deltaPP(delta);
      }
    }

    // Manually subtract item from inventory to avoid
    // excessive list updating.

    if (newItem != EquipmentRequest.UNEQUIP) {
      AdventureResult.addResultToList(KoLConstants.inventory, newItem.getInstance(-1));
      QuestManager.updateQuestItemEquipped(newItem.getItemId());
    }

    if (oldItem != EquipmentRequest.UNEQUIP) {
      AdventureResult.addResultToList(KoLConstants.inventory, oldItem.getInstance(1));
    }

    // If we remove Special Sauce Glove, also remove Chefstaff
    if (oldItemId == ItemPool.SPECIAL_SAUCE_GLOVE
        && !KoLCharacter.hasSkill(SkillPool.SPIRIT_OF_RIGATONI)
        && !KoLCharacter.isJarlsberg()
        && EquipmentManager.usingChefstaff()) {
      EquipmentManager.removeEquipment(EquipmentManager.getEquipment(Slot.WEAPON), Slot.WEAPON);
    }

    return !ConcoctionDatabase.getKnownUses(oldItem).isEmpty()
        || !ConcoctionDatabase.getKnownUses(newItem).isEmpty();
  }

  public static void parseBedazzlements(final String responseText) {
    Matcher matcher = EquipmentRequest.STICKER_PATTERN.matcher(responseText);
    for (Slot slot : SlotSet.STICKER_SLOTS) {
      if (!matcher.find()) {
        return; // presumably doesn't have a sticker weapon
      }
      AdventureResult newItem;
      if (matcher.group(2) == null) {
        newItem = EquipmentRequest.UNEQUIP;
      } else {
        newItem = ItemPool.get(matcher.group(2).trim(), 1);
      }
      AdventureResult oldItem = EquipmentManager.getEquipment(slot);
      EquipmentManager.setEquipment(slot, newItem);
      if (!KoLmafia.isRefreshing() && !newItem.equals(oldItem)) {
        if (!oldItem.equals(EquipmentRequest.UNEQUIP)
            && !KoLConstants.inventory.contains(oldItem)) {
          // Item was in the list for this slot
          // only so that it could be displayed
          // as the current item.	 Remove it.
          EquipmentManager.getEquipmentLists().get(slot).remove(oldItem);
        }
        if (!newItem.equals(EquipmentRequest.UNEQUIP)) {
          ResultProcessor.processResult(newItem.getInstance(-1));
        }
        EquipmentManager.setTurns(slot, 20, 20);
      }

      if (matcher.group(1) != null) {
        String adjective = matcher.group(1);
        if (adjective.equals("shiny")) {
          EquipmentManager.setTurns(slot, 16, 20);
        } else if (adjective.equals("dull")) {
          EquipmentManager.setTurns(slot, 1, 5);
        } else {
          EquipmentManager.setTurns(slot, 6, 15);
        }
      }
    }
  }

  private static final Pattern ACQUIRE_PATTERN =
      Pattern.compile("You acquire an item: <b>(.*?)</b>");
  private static final Pattern CONTAINS_PATTERN =
      Pattern.compile("Your card sleeve currently contains an <b>(.*?)</b>");

  public static void parseCardSleeve(final String responseText) {
    // Putting a card into an empty card sleeve
    //
    // You put the Alice's Army Sniper in the card sleeve.
    // Your card sleeve currently contains an Alice's Army Sniper.
    //
    // Putting a card into an occupied card sleeve
    //
    // You take the existing card out of the sleeve to make room:
    // You acquire an item: Alice's Army Sniper
    // You put the Alice's Army Bowman in the card sleeve.
    // Your card sleeve currently contains an Alice's Army Bowman.
    //
    // Removing a card from a card sleeve
    //
    // You pull the card out of the sleeve.
    // You acquire an item: Alice's Army Sniper
    // Your card sleeve is currently empty.

    Matcher acquiresMatcher = EquipmentRequest.ACQUIRE_PATTERN.matcher(responseText);
    String acquired = acquiresMatcher.find() ? acquiresMatcher.group(1) : null;
    int acquiredId = ItemDatabase.getItemId(acquired);
    Matcher containsMatcher = EquipmentRequest.CONTAINS_PATTERN.matcher(responseText);
    String contains = containsMatcher.find() ? containsMatcher.group(1) : null;
    int containsId = ItemDatabase.getItemId(contains);
    AdventureResult oldItem =
        acquired != null ? ItemPool.get(acquiredId) : EquipmentRequest.UNEQUIP;
    AdventureResult newItem =
        contains != null ? ItemPool.get(containsId) : EquipmentRequest.UNEQUIP;

    if (acquired != null) {
      // *** result processing added it to inventory and tally.
      AdventureResult remove = oldItem.getInstance(-1);
      AdventureResult.addResultToList(KoLConstants.tally, remove);
      AdventureResult.addResultToList(KoLConstants.inventory, remove);
    }

    // Put the old item into inventory and remove the new one
    EquipmentRequest.switchItem(oldItem, newItem);
    EquipmentManager.setEquipment(Slot.CARDSLEEVE, newItem);
  }

  public static AdventureResult idToFolder(final String id) {
    int itemId = ItemPool.FOLDER_01 + StringUtilities.parseInt(id) - 1;
    return (itemId < ItemPool.FOLDER_01 || itemId > ItemPool.FOLDER_28)
        ? EquipmentRequest.UNEQUIP
        : ItemPool.get(itemId, 1);
  }

  public static void parseFolders(String responseText) {
    int startIndex = responseText.indexOf("Contents of your Folder Holder");
    int stopIndex = responseText.indexOf("Folders in your Inventory");
    if (stopIndex == -1) {
      stopIndex = responseText.indexOf("You don't have any folders to add.");
    }
    if (startIndex == -1 || stopIndex == -1) {
      return;
    }

    String text = responseText.substring(startIndex, stopIndex);
    Matcher folderMatcher = EquipmentRequest.FOLDER_PATTERN.matcher(text);

    boolean failed = false;
    for (var slot : SlotSet.FOLDER_SLOTS) {
      if (failed) {
        EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);
      } else if (folderMatcher.find()) {
        AdventureResult folder = EquipmentRequest.idToFolder(folderMatcher.group(1));
        EquipmentManager.setEquipment(slot, folder);
      } else {
        failed = true;
        EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);
      }
    }
  }

  public static void parseEquipment(final String location, final String responseText) {
    if (location.contains("onlyitem=")
        || location.contains("ajax=1")
        || location.contains("ftext=")) {
      return;
    }

    EnumMap<Slot, AdventureResult> oldEquipment = EquipmentManager.currentEquipment();
    int oldFakeHands = EquipmentManager.getFakeHands();
    int newFakeHands = 0;

    // Ensure that the inventory stays up-to-date by switching
    // items around, as needed.

    EnumMap<Slot, AdventureResult> equipment = EquipmentManager.emptyEquipmentArray();

    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "unequip&type=hat",
        EquipmentRequest.HAT_PATTERN,
        "Hat: ",
        Slot.HAT);
    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "unequip&type=weapon",
        EquipmentRequest.WEAPON_PATTERN,
        "Weapon: ",
        Slot.WEAPON);
    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "action=holster&holster=0",
        EquipmentRequest.HOLSTER_PATTERN,
        "Holstered: ",
        Slot.HOLSTER);
    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "unequip&type=offhand",
        EquipmentRequest.OFFHAND_PATTERN,
        "Offhand: ",
        Slot.OFFHAND);
    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "unequip&type=container",
        EquipmentRequest.CONTAINER_PATTERN,
        "Back: ",
        Slot.CONTAINER);
    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "unequip&type=shirt",
        EquipmentRequest.SHIRT_PATTERN,
        "Shirt: ",
        Slot.SHIRT);
    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "unequip&type=pants",
        EquipmentRequest.PANTS_PATTERN,
        "Pants: ",
        Slot.PANTS);
    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "unequip&type=acc1",
        EquipmentRequest.ACC1_PATTERN,
        "Accessory 1: ",
        Slot.ACCESSORY1);
    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "unequip&type=acc2",
        EquipmentRequest.ACC2_PATTERN,
        "Accessory 2: ",
        Slot.ACCESSORY2);
    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "unequip&type=acc3",
        EquipmentRequest.ACC3_PATTERN,
        "Accessory 3: ",
        Slot.ACCESSORY3);
    EquipmentRequest.parseEquipment(
        responseText,
        equipment,
        "unequip&type=familiarequip",
        EquipmentRequest.FAMILIARITEM_PATTERN,
        "Familiar: ",
        Slot.FAMILIAR);

    int index = 0;
    while ((index = responseText.indexOf("unequip&type=fakehand", index)) != -1) {
      newFakeHands += 1;
      index += 21;
    }

    // First, handle all of the equipment pre-processing,
    // like inventory shuffling and the like.

    boolean refresh = EquipmentRequest.switchEquipment(oldEquipment, equipment);

    // Adjust inventory of fake hands

    if (oldFakeHands != newFakeHands) {
      AdventureResult.addResultToList(
          KoLConstants.inventory, ItemPool.get(ItemPool.FAKE_HAND, oldFakeHands - newFakeHands));
      EquipmentManager.setFakeHands(newFakeHands);
    }

    EquipmentManager.updateNormalOutfits();

    // Look for custom outfits

    Matcher outfitsMatcher = EquipmentRequest.OUTFITLIST_PATTERN.matcher(responseText);
    SpecialOutfit.checkOutfits(outfitsMatcher.find() ? outfitsMatcher.group() : null);

    // Check if familiar equipment is locked
    FamiliarData.checkLockedItem(responseText);

    // If he's wearing a custom outfit, do additional processing
    EquipmentRequest.wearCustomOutfit();

    // If you need to update your creatables list, do so at
    // the end of the processing.

    if (refresh) {
      ConcoctionDatabase.setRefreshNeeded(false);
    }
  }

  private static void parseEquipment(
      final String responseText,
      Map<Slot, AdventureResult> equipment,
      final String test,
      final Pattern pattern,
      final String tag,
      final Slot slot) {
    if (!responseText.contains(test)) {
      return;
    }

    Matcher matcher = pattern.matcher(responseText);
    if (!matcher.find()) {
      return;
    }

    String descId = matcher.group(1) != null ? matcher.group(2) : "";
    String name = matcher.group(3).trim();
    // This will register a new item from the descid, if needed
    int itemId = ItemDatabase.lookupItemIdFromDescription(descId);

    if (slot != Slot.FAMILIAR && slot != Slot.HOLSTER && !EquipmentDatabase.contains(itemId)) {
      RequestLogger.printLine("Found unknown equipped item: \"" + name + "\" descid = " + descId);
    }

    AdventureResult item = ItemPool.get(itemId);

    equipment.put(slot, item);

    if (RequestLogger.isDebugging()) {
      RequestLogger.updateDebugLog(tag + equipment.get(slot));
    }
  }

  public static boolean switchEquipment(
      final Map<Slot, AdventureResult> oldEquipment,
      final Map<Slot, AdventureResult> newEquipment) {
    boolean refresh = false;

    if (!KoLmafia.isRefreshing()) {
      for (var slot : SlotSet.SLOTS) {
        if (slot == Slot.HOLSTER) {
          // No inventory swapping for holstered sixguns
          continue;
        }

        AdventureResult oldItem = oldEquipment.get(slot);
        AdventureResult newItem = newEquipment.get(slot);

        refresh |= EquipmentRequest.switchItem(oldItem, newItem);
      }
    }

    // Now update your equipment to make sure that selected items are properly selected in the
    // dropdowns.

    EquipmentManager.setEquipment(newEquipment);

    return refresh;
  }

  private static boolean switchItem(final Slot type, final AdventureResult newItem) {
    boolean refresh = false;

    switch (type) {
      case FAMILIAR:
      case HOLSTER:
        // Does not change inventory
        break;

      case WEAPON:
        // Wielding a two-handed weapon automatically unequips
        // anything in the off-hand.
        // Dropping a weapon in the main hand automatically drops a weapon in the off-hand
        if (EquipmentDatabase.getHands(newItem.getItemId()) > 1
            || (newItem.equals(EquipmentRequest.UNEQUIP) && EquipmentManager.isDualWielding())) {
          refresh |= EquipmentRequest.switchItem(Slot.OFFHAND, EquipmentRequest.UNEQUIP);
        }
        // fall through
      default:
        AdventureResult oldItem = EquipmentManager.getEquipment(type);
        refresh |= EquipmentRequest.switchItem(oldItem, newItem);
        break;
    }

    // Now update your equipment to make sure that selected
    // items are properly selected in the dropdowns.

    EquipmentManager.setEquipment(type, newItem);

    if (type == Slot.FAMILIAR) {
      FamiliarData familiar = KoLCharacter.getFamiliar();
      if (familiar != FamiliarData.NO_FAMILIAR) {
        familiar.setItem(newItem);
      }
    }

    return refresh;
  }

  public static boolean parseEquipmentChange(final String location, final String responseText) {
    Matcher matcher = GenericRequest.ACTION_PATTERN.matcher(location);

    // We have nothing special to do for simple visits.
    if (!matcher.find()) {
      return false;
    }

    String action = matcher.group(1);

    // inv_equip.php?action=equip&whichitem=2764&slot=1&ajax=1
    // inv_equip.php?action=equip&whichitem=1234&ajax=1

    if (action.equals("equip")) {
      // Detect failure to equip
      if (!responseText.contains("You equip an item")
          && !responseText.contains("Item equipped")
          && !responseText.contains("equips an item")) {
        return false;
      }

      // We equipped an item.
      int itemId = EquipmentRequest.parseItemId(location);
      if (itemId < 0) {
        return false;
      }

      if (itemId == ItemPool.FAKE_HAND) {
        int oldFakeHands = EquipmentManager.getFakeHands();
        EquipmentManager.setFakeHands(oldFakeHands + 1);
        AdventureResult.addResultToList(
            KoLConstants.inventory, ItemPool.get(ItemPool.FAKE_HAND, -1));
        return false;
      }

      Slot slot = EquipmentRequest.findEquipmentSlot(itemId, location);
      if (EquipmentRequest.switchItem(slot, ItemPool.get(itemId, 1))) {
        ConcoctionDatabase.setRefreshNeeded(false);
      }

      return false;
    }

    // inv_equip.php?action=dualwield&whichitem=1325&ajax=1
    if (action.equals("dualwield")) {
      // Detect failure to equip
      if (!responseText.contains("You equip an item") && !responseText.contains("Item equipped")) {
        return false;
      }

      // We equipped an item.
      int itemId = EquipmentRequest.parseItemId(location);
      if (itemId < 0) {
        return false;
      }

      if (EquipmentRequest.switchItem(Slot.OFFHAND, ItemPool.get(itemId, 1))) {
        ConcoctionDatabase.setRefreshNeeded(false);
      }

      return false;
    }

    // inv_equip.php?action=unequipall&ajax=1
    if (action.equals("unequipall")) {
      // We unequipped everything
      if (!responseText.contains("All items unequipped")) {
        return false;
      }

      boolean switched = false;
      for (var slot : SlotSet.SLOTS) {
        // Whether the familiar item is unequipped on
        // an unequip all is an account preference.
        if (slot == Slot.FAMILIAR && !KoLCharacter.getUnequipFamiliar()) {
          continue;
        }

        if (EquipmentRequest.switchItem(slot, EquipmentRequest.UNEQUIP)) {
          switched = true;
        }
      }

      if (switched) {
        ConcoctionDatabase.setRefreshNeeded(false);
      }
    }

    // inv_equip.php?action=unequip&type=acc3&ajax=1
    if (action.equals("unequip")) {
      // Detect failure to equip
      if (!responseText.contains("Item unequipped")) {
        return false;
      }

      // We unequipped an item.
      String slotName = EquipmentRequest.parseSlotName(location);
      if (slotName == null) {
        return false;
      }

      Slot type = EquipmentRequest.slotNumber(slotName);
      if (type == Slot.NONE) {
        return false;
      }

      if (type == Slot.FAKEHAND) {
        int oldFakeHands = EquipmentManager.getFakeHands();
        AdventureResult.addResultToList(
            KoLConstants.inventory, ItemPool.get(ItemPool.FAKE_HAND, oldFakeHands));
        EquipmentManager.setFakeHands(0);
        return false;
      }

      if (EquipmentRequest.switchItem(type, EquipmentRequest.UNEQUIP)) {
        ConcoctionDatabase.setRefreshNeeded(false);
      }

      return false;
    }

    // inv_equip.php?action=hatrack&whichitem=308&ajax=1
    if (action.equals("hatrack")) {
      // Detect failure to equip
      if (!responseText.contains("equips an item")) {
        return false;
      }

      // We equipped an item.
      int itemId = EquipmentRequest.parseItemId(location);
      if (itemId < 0) {
        return false;
      }

      if (EquipmentRequest.switchItem(Slot.FAMILIAR, ItemPool.get(itemId, 1))) {
        ConcoctionDatabase.setRefreshNeeded(false);
      }

      return false;
    }

    // inventory.php?action=holster&holster=8970&ajax=1
    if (action.equals("holster")) {
      // We equipped an item.
      int itemId = EquipmentRequest.parseHolster(location);
      if (itemId < 0) {
        return false;
      }

      if (itemId == 0) {
        EquipmentManager.setEquipment(Slot.HOLSTER, EquipmentRequest.UNEQUIP);
      } else {
        EquipmentRequest.switchItem(Slot.HOLSTER, ItemPool.get(itemId, 1));
      }

      return false;
    }

    // inv_equip.php?action=customoutfit&outfitname=Backup
    if (action.equals("customoutfit")) {
      // Detect failure to equip
      if (!responseText.contains("Your custom outfit has been saved")) {
        return false;
      }

      // We saved a custom outfit. KoL assigned a new outfit
      // ID to it and was kind enough to tell it to us in an
      // HTML comment: <!-- outfitid: 61 -->

      matcher = OUTFITNAME_PATTERN.matcher(location);
      if (!matcher.find()) {
        return false;
      }
      String name = GenericRequest.decodeField(matcher.group(1));

      matcher = OUTFITID_PATTERN.matcher(responseText);
      if (!matcher.find()) {
        return false;
      }
      int id = StringUtilities.parseInt(matcher.group(1));

      // Make a new custom outfit
      SpecialOutfit outfit = new SpecialOutfit(-id, name);

      EnumMap<Slot, AdventureResult> equipment = EquipmentManager.currentEquipment();
      // Add our current equipment to it
      for (var slot : SlotSet.CORE_EQUIP_SLOTS) {
        AdventureResult piece = equipment.get(slot);
        // Make a brand-new AdventureResult for each item
        if (piece != EquipmentRequest.UNEQUIP) {
          piece = ItemPool.get(piece.getItemId());
        }
        outfit.addPiece(piece);
      }

      // Add this outfit to the list of custom outfits.
      EquipmentManager.addCustomOutfit(outfit);

      return false;
    }

    // inv_equip.php?action=outfit&whichoutfit=-28&ajax=1
    if (action.equals("outfit")) {
      // Detect failure to equip
      if (!responseText.contains("You put on")) {
        return false;
      }

      // We changed into an outfit.

      // Since KoL doesn't tell us where accessories and
      // dual-wielded weapons end up, apply heuristics and
      // hope for the best.

      EquipmentRequest.donOutfit(responseText);

      // Until/unless KoL includes the equipment slot for
      // each item changed, "hope" is not enough.
      //
      // Request status refresh via api.php in order to make
      // sure equipment ends up in correct slots.

      ApiRequest.updateStatus(true);

      // Trigger actions based on outfit name

      EquipmentRequest.wearCustomOutfit();

      return true;
    }

    return false;
  }

  private static void donOutfit(final String responseText) {
    // Since KoL doesn't tell us where accessories end up,
    // we could ask for an update, but we'll apply
    // heuristics and hope for the best.

    // Similarly, if you are dual-wielding two of the same
    // weapon, if you replace one of them with another
    // weapon, it's problematic.

    EnumMap<Slot, AdventureResult> oldEquipment = EquipmentManager.currentEquipment();
    EnumMap<Slot, AdventureResult> newEquipment = EquipmentManager.currentEquipment();

    // Experimentation suggests that accessories are
    // installed in	 "Item Equipped" order like this:
    // - fill empty accessory slots from 1 to 3
    // - replace previous accessories from 3 to 1
    //
    // Note that if an already equipped accessory is part
    // of the new outfit, it stays exactly where it was.

    // Weapons that are part of the new outfit stay where
    // they currently are.

    // Iterate over all unequipped items.
    Matcher unequipped = UNEQUIPPED_PATTERN.matcher(responseText);
    while (unequipped.find()) {
      String descId = unequipped.group(1);

      int itemId = ItemDatabase.getItemIdFromDescription(descId);
      if (!EquipmentDatabase.contains(itemId)) {
        continue;
      }

      AdventureResult item = ItemPool.get(itemId);
      Slot slot = EquipmentManager.itemIdToEquipmentType(itemId);
      switch (slot) {
        case ACCESSORY1 -> {
          if (newEquipment.get(Slot.ACCESSORY3).equals(item)) {
            slot = Slot.ACCESSORY3;
          } else if (newEquipment.get(Slot.ACCESSORY2).equals(item)) {
            slot = Slot.ACCESSORY2;
          } else if (!newEquipment.get(Slot.ACCESSORY1).equals(item)) {
            // KoL error: accessory not found
            continue;
          }
        }
        case WEAPON -> {
          if (newEquipment.get(Slot.OFFHAND).equals(item)) {
            // Heuristic: unequip duplicate
            // weapon from offhand slot first
            slot = Slot.OFFHAND;
          } else if (!newEquipment.get(Slot.WEAPON).equals(item)) {
            // KoL error: weapon not found
            continue;
          }
        }
        default -> {
          // Everything else goes into an
          // unambiguous slot.
        }
      }

      newEquipment.put(slot, EquipmentRequest.UNEQUIP);
    }

    // Calculate accessory fill order
    int accessoryIndex = 0;

    Slot[] accessories =
        new Slot[] {
          Slot.ACCESSORY1, Slot.ACCESSORY2, Slot.ACCESSORY3,
        };
    // Consume unfilled slots from 1 to 3
    for (Slot slot : SlotSet.ACCESSORY_SLOTS) {
      if (oldEquipment.get(slot) == EquipmentRequest.UNEQUIP) {
        accessories[accessoryIndex++] = slot;
      }
    }
    // Consume filled slots from 3 to 1
    for (Slot slot : List.of(Slot.ACCESSORY3, Slot.ACCESSORY2, Slot.ACCESSORY1)) {
      if (accessoryIndex >= 3) break;
      if (oldEquipment.get(slot) != EquipmentRequest.UNEQUIP
          && newEquipment.get(slot) == EquipmentRequest.UNEQUIP) {
        accessories[accessoryIndex++] = slot;
      }
    }

    // Calculate weapon fill order
    Slot[] weapons =
        new Slot[] {
          Slot.WEAPON, Slot.OFFHAND,
        };
    int weaponIndex = 0;

    // If the offhand slot is empty and the weapon slot is
    // not, put new weapon into offhand slot
    if (newEquipment.get(Slot.OFFHAND) == EquipmentRequest.UNEQUIP
        && newEquipment.get(Slot.WEAPON) != EquipmentRequest.UNEQUIP) {
      weapons[0] = Slot.OFFHAND;
      weapons[1] = Slot.WEAPON;
    }

    // Reset equip indices
    accessoryIndex = 0;
    weaponIndex = 0;

    // Iterate over all equipped items.
    Matcher equipped = EQUIPPED_PATTERN.matcher(responseText);
    while (equipped.find()) {
      String descId = equipped.group(1);

      int itemId = ItemDatabase.getItemIdFromDescription(descId);
      if (!EquipmentDatabase.contains(itemId)) {
        continue;
      }

      AdventureResult item = ItemPool.get(itemId);
      Slot slot = EquipmentManager.itemIdToEquipmentType(itemId);
      switch (slot) {
        case ACCESSORY1 -> {
          if (accessoryIndex >= 3) {
            // KoL error: four accessories
            continue;
          }
          slot = accessories[accessoryIndex++];
        }
        case WEAPON -> {
          if (weaponIndex >= 2) {
            // KoL error: three weapons
            continue;
          }
          slot = weapons[weaponIndex];

          // A chefstaff must go in the weapon slot,
          // but KoL does not always list it first.
          if (slot == Slot.OFFHAND && EquipmentDatabase.isChefStaff(item)) {
            slot = Slot.WEAPON;

            // Move other weapon to offhand
            newEquipment.put(Slot.OFFHAND, newEquipment.get(Slot.WEAPON));

            // If we thought we were unequipping offhand
            // and leaving weapon equipped, reverse that
            if (weaponIndex == 0) {
              weapons[1] = Slot.OFFHAND;
            }
          }
          weaponIndex++;
        }
        default -> {}
          // Everything else goes into an
          // unambiguous slot.
      }

      newEquipment.put(slot, item);
    }

    if (EquipmentRequest.switchEquipment(oldEquipment, newEquipment)) {
      ConcoctionDatabase.setRefreshNeeded(false);
    }
  }

  private static void wearCustomOutfit() {
    int outfitId = EquipmentRequest.customOutfitId;
    EquipmentRequest.customOutfitId = 0;

    SpecialOutfit outfit = EquipmentManager.getCustomOutfit(outfitId);
    if (outfit == null) {
      return;
    }

    Matcher m = EquipmentRequest.OUTFIT_ACTION_PATTERN.matcher(outfit.getName());
    while (m.find()) {
      String text = m.group(2).trim();
      switch (m.group(1).toLowerCase().charAt(0)) {
        case 'c' -> KoLmafiaCLI.DEFAULT_SHELL.executeLine(text);
        case 'e' -> KoLmafiaCLI.DEFAULT_SHELL.executeCommand("equip", "familiar " + text);
        case 'f' -> KoLmafiaCLI.DEFAULT_SHELL.executeCommand("familiar", text);
        case 'm' -> KoLmafiaCLI.DEFAULT_SHELL.executeCommand("mood", text);
        case 't' -> KoLmafiaCLI.DEFAULT_SHELL.executeCommand("enthrone", text);
        case 'b' -> KoLmafiaCLI.DEFAULT_SHELL.executeCommand("bjornify", text);
      }
    }
  }

  private static int parseItemId(final String location) {
    Matcher matcher = GenericRequest.WHICHITEM_PATTERN.matcher(location);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  private static int parseSlot(final String location) {
    Matcher matcher = SLOT1_PATTERN.matcher(location);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  private static String parseSlotName(final String location) {
    Matcher matcher = EquipmentRequest.SLOT_PATTERN.matcher(location);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static int parseHolster(final String location) {
    Matcher matcher = HOLSTER_URL_PATTERN.matcher(location);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  public static Slot slotNumber(final String name) {
    var slot = Slot.byCaselessName(name);
    if (slot != Slot.NONE) return slot;

    return Slot.byCaselessPhpName(name);
  }

  private static Slot findEquipmentSlot(final int itemId, final String location) {
    Slot type = EquipmentManager.itemIdToEquipmentType(itemId);

    // If it's not an accessory, slot is unambiguous
    if (type != Slot.ACCESSORY1) {
      return type;
    }

    // Accessories might specify the slot in the URL
    return switch (EquipmentRequest.parseSlot(location)) {
      case 1 -> Slot.ACCESSORY1;
      case 2 -> Slot.ACCESSORY2;
      case 3 -> Slot.ACCESSORY3;
        // Otherwise, KoL picks the first empty accessory slot.
      default -> EquipmentRequest.availableAccessory();
    };
  }

  public static boolean registerRequest(final String urlString) {
    if (urlString.startsWith("bedazzle.php")) {
      registerBedazzlement(urlString);
      return true;
    }

    if (urlString.startsWith("inv_use.php") && urlString.contains("whichitem=5009")) {
      return EquipmentRequest.registerCardSleeve(urlString);
    }

    if (urlString.startsWith("inventory.php") && urlString.contains("action=holster")) {
      return EquipmentRequest.registerHolster(urlString);
    }

    if (!urlString.startsWith("inv_equip.php")) {
      return false;
    }

    EquipmentRequest.customOutfitId = 0;

    Matcher outfitMatcher = EquipmentRequest.OUTFIT_PATTERN.matcher(urlString);
    if (outfitMatcher.find()) {
      String outfitString = outfitMatcher.group(1);
      if (outfitString.equals("last")) {
        RequestLogger.updateSessionLog("outfit last");
        return true;
      }

      int outfitId = StringUtilities.parseInt(outfitString);
      if (outfitId > 0) {
        RequestLogger.updateSessionLog("outfit " + EquipmentDatabase.getOutfit(outfitId));
        return true;
      }

      SpecialOutfit outfit = EquipmentManager.getCustomOutfit(outfitId);
      String name = outfit == null ? String.valueOf(outfitId) : outfit.getName();

      RequestLogger.updateSessionLog("custom outfit " + name);
      EquipmentRequest.customOutfitId = outfitId;
      return true;
    }

    if (urlString.contains("action=unequip")) {
      if (urlString.contains("terrarium=1")) {
        FamiliarRequest.unequipCurrentFamiliar();
        return true;
      }

      String slotName = parseSlotName(urlString);
      if (slotName != null) {
        RequestLogger.updateSessionLog("unequip " + slotName);
      }

      return true;
    }

    int itemId = EquipmentRequest.parseItemId(urlString);
    if (itemId == -1) {
      return true;
    }

    String itemName = ItemDatabase.getItemName(itemId);
    if (itemName == null) {
      return true;
    }

    if (urlString.contains("dualwield")) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("equip off-hand " + itemName);
    } else if (urlString.contains("slot=1")) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("equip acc1 " + itemName);
    } else if (urlString.contains("slot=2")) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("equip acc2 " + itemName);
    } else if (urlString.contains("slot=3")) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("equip acc3 " + itemName);
    } else if (urlString.contains("terrarium=1")) {
      FamiliarRequest.equipCurrentFamiliar(itemId);
    } else {
      Slot slot = EquipmentRequest.chooseEquipmentSlot(ItemDatabase.getItemId(itemName));
      if (slot != Slot.NONE) {
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("equip " + slot.name + " " + itemName);
      }
    }

    return true;
  }

  private static final Pattern SLEEVECARD_PATTERN = Pattern.compile("sleevecard=(\\d+)");

  public static boolean registerCardSleeve(final String urlString) {
    UseItemRequest.setLastItemUsed(EquipmentManager.CARD_SLEEVE);
    Matcher m = SLEEVECARD_PATTERN.matcher(urlString);
    String message =
        m.find()
            ? "equip card-sleeve " + ItemDatabase.getItemName(StringUtilities.parseInt(m.group(1)))
            : urlString.contains("removecard=1") ? "unequip card-sleeve " : null;
    if (message != null) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog(message);
    }

    return true;
  }

  public static void registerBedazzlement(final String urlString) {
    if (urlString.contains("action=fold")) {
      RequestLogger.updateSessionLog("folded sticker weapon");
      return;
    }

    if (urlString.contains("action=peel")) {
      Matcher slotMatcher = EquipmentRequest.SLOT1_PATTERN.matcher(urlString);
      if (slotMatcher.find()) {
        RequestLogger.updateSessionLog("peeled sticker " + slotMatcher.group(1));
      }
      return;
    }

    Matcher itemMatcher = EquipmentRequest.STICKERITEM_PATTERN.matcher(urlString);
    if (!itemMatcher.find()) {
      return;
    }

    int itemId = StringUtilities.parseInt(itemMatcher.group(1));
    String itemName = ItemDatabase.getItemName(itemId);
    if (itemName == null) {
      return;
    }

    if (urlString.contains("action=juststick")) {
      RequestLogger.updateSessionLog("stuck " + itemName + " in empty slot");
    } else if (urlString.contains("action=stick")) {
      int slot = EquipmentRequest.parseSlot(urlString);
      if (slot > 0) {
        RequestLogger.updateSessionLog("stuck " + itemName + " in slot " + slot);
      }
    }
  }

  public static boolean registerHolster(final String urlString) {
    Matcher m = HOLSTER_URL_PATTERN.matcher(urlString);
    int itemId = m.find() ? StringUtilities.parseInt(m.group(1)) : -1;
    String message =
        itemId > 0
            ? "equip holster " + ItemDatabase.getItemName(itemId)
            : itemId == 0 ? "unequip holster" : null;

    if (message != null) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog(message);
    }

    return true;
  }

  public static void checkCowboyBoots() {
    if (!InventoryManager.hasItem(EquipmentManager.COWBOY_BOOTS)
        && !KoLCharacter.hasEquipped(EquipmentManager.COWBOY_BOOTS)) {
      EquipmentManager.setEquipment(Slot.BOOTSKIN, EquipmentRequest.UNEQUIP);
      EquipmentManager.setEquipment(Slot.BOOTSPUR, EquipmentRequest.UNEQUIP);
      return;
    }

    String text = DebugDatabase.itemDescriptionText(ItemPool.COWBOY_BOOTS, true);

    // This is your favorite old pair of trail-worn cowboy boots,
    // made of diamondback skin and all gussied up with nicksilver spurs.

    // This is your favorite old pair of trail-worn cowboy boots,
    // made of fine Corinthian leather and all gussied up with invisible spurs (which might, I
    // admit, not be real).

    Pattern BOOT_PATTERN = Pattern.compile("made of (.*?) and all gussied up with (.* spurs)");
    Matcher matcher = BOOT_PATTERN.matcher(text);
    if (matcher.find()) {
      String skin = matcher.group(1);
      if (skin.equals("fine Corinthian leather")) {
        EquipmentManager.setEquipment(Slot.BOOTSKIN, EquipmentRequest.UNEQUIP);
      } else {
        EquipmentManager.setEquipment(Slot.BOOTSKIN, new AdventureResult(skin));
      }

      String spur = matcher.group(2);
      if (spur.equals("invisible spurs")) {
        EquipmentManager.setEquipment(Slot.BOOTSPUR, EquipmentRequest.UNEQUIP);
      } else {
        EquipmentManager.setEquipment(Slot.BOOTSPUR, new AdventureResult(spur));
      }
    }
  }

  public static void checkHolster() {
    if (!KoLCharacter.isAWoLClass()) {
      EquipmentManager.setEquipment(Slot.HOLSTER, EquipmentRequest.UNEQUIP);
      return;
    }

    // Have to get it from the Equipment page of the Inventory
    RequestThread.postRequest(new EquipmentRequest(EquipmentRequestType.EQUIPMENT));
  }
}
