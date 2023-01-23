package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class BigBrotherRequest extends CoinMasterRequest {
  public static final String master = "Big Brother";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? sand dollar");

  public static final AdventureResult SAND_DOLLAR = ItemPool.get(ItemPool.SAND_DOLLAR, 1);
  public static final AdventureResult BLACK_GLASS = ItemPool.get(ItemPool.BLACK_GLASS, 1);

  public static final CoinmasterData BIG_BROTHER =
      new CoinmasterData(master, "bigbrother", BigBrotherRequest.class)
          .withToken("sand dollar")
          .withTokenTest("You haven't got any sand dollars")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(SAND_DOLLAR)
          .withBuyURL("monkeycastle.php")
          .withBuyAction("buyitem")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withItemField("whichitem")
          .withItemPattern(GenericRequest.WHICHITEM_PATTERN)
          .withCountField("quantity")
          .withCountPattern(GenericRequest.QUANTITY_PATTERN)
          .withCanBuyItem(BigBrotherRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.MADNESS_REEF_MAP,
          ItemPool.MARINARA_TRENCH_MAP,
          ItemPool.ANEMONE_MINE_MAP,
          ItemPool.DIVE_BAR_MAP,
          ItemPool.SKATE_PARK_MAP -> !ItemDatabase.haveVirtualItem(itemId);
      case ItemPool.DAMP_OLD_BOOT -> !Preferences.getBoolean("dampOldBootPurchased");
      case ItemPool.BLACK_GLASS -> BLACK_GLASS.getCount(KoLConstants.inventory) == 0;
      case ItemPool.FOLDER_19 -> KoLCharacter.hasEquipped(EquipmentManager.FOLDER_HOLDER);
      default -> ItemPool.get(itemId).getCount(BIG_BROTHER.getBuyItems()) > 0;
    };
  }

  public static final AdventureResult AERATED_DIVING_HELMET =
      ItemPool.get(ItemPool.AERATED_DIVING_HELMET);
  public static final AdventureResult SCHOLAR_MASK = ItemPool.get(ItemPool.SCHOLAR_MASK);
  public static final AdventureResult GLADIATOR_MASK = ItemPool.get(ItemPool.GLADIATOR_MASK);
  public static final AdventureResult CRAPPY_MASK = ItemPool.get(ItemPool.CRAPPY_MASK);

  public static final AdventureResult SCUBA_GEAR = ItemPool.get(ItemPool.SCUBA_GEAR);
  public static final AdventureResult OLD_SCUBA_TANK = ItemPool.get(ItemPool.OLD_SCUBA_TANK);

  public static final AdventureResult BUBBLIN_STONE = ItemPool.get(ItemPool.BUBBLIN_STONE);

  public static final AdventureResult BATHYSPHERE = ItemPool.get(ItemPool.BATHYSPHERE);
  public static final AdventureResult DAS_BOOT = ItemPool.get(ItemPool.DAS_BOOT);
  public static final AdventureResult AMPHIBIOUS_TOPHAT = ItemPool.get(ItemPool.AMPHIBIOUS_TOPHAT);

  private static AdventureResult self = null;
  private static AdventureResult familiar = null;
  private static boolean rescuedBigBrother = false;

  public BigBrotherRequest() {
    super(BIG_BROTHER);
    this.addFormField("who", "2");
  }

  public BigBrotherRequest(final boolean buying, final AdventureResult[] attachments) {
    super(BIG_BROTHER, buying, attachments);
  }

  public BigBrotherRequest(final boolean buying, final AdventureResult attachment) {
    super(BIG_BROTHER, buying, attachment);
  }

  public BigBrotherRequest(final boolean buying, final int itemId, final int quantity) {
    super(BIG_BROTHER, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    CoinmasterData data = BIG_BROTHER;
    String action = GenericRequest.getAction(location);
    if (action == null) {
      if (!location.contains("who=2") || !responseText.contains("sand dollar")) {
        return;
      }

      // We know for sure that we have rescued Big Brother
      // this ascension
      Preferences.setBoolean("bigBrotherRescued", true);

      // Parse current coin balances
      CoinMasterRequest.parseBalance(data, responseText);

      // Look at his inventory
      if (!responseText.contains("damp old boot")) {
        Preferences.setBoolean("dampOldBootPurchased", true);
        QuestDatabase.setQuestIfBetter(Quest.SEA_OLD_GUY, "step1");
      }
      if (!responseText.contains("map to Anemone Mine") && !KoLCharacter.isMuscleClass()) {
        // Little Brother unlocks Anemone Mine for Muscle classes
        Preferences.setBoolean("mapToAnemoneMinePurchased", true);
      }
      if (!responseText.contains("map to the Marinara Trench")
          && !KoLCharacter.isMysticalityClass()) {
        // Little Brother unlocks the Marinara Trench for Mysticality classes
        Preferences.setBoolean("mapToTheMarinaraTrenchPurchased", true);
      }
      if (!responseText.contains("map to the Dive Bar") && !KoLCharacter.isMoxieClass()) {
        // Little Brother unlocks The Dive Bar for Moxie classes
        Preferences.setBoolean("mapToTheDiveBarPurchased", true);
      }
      Preferences.setBoolean(
          "mapToMadnessReefPurchased", !responseText.contains("map to Madness Reef"));
      Preferences.setBoolean(
          "mapToTheSkateParkPurchased", !responseText.contains("map to the Skate Park"));

      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);

    int itemId = CoinMasterRequest.extractItemId(data, location);
    switch (itemId) {
      case ItemPool.MADNESS_REEF_MAP:
        if (responseText.contains("Big Brother shows you the map")) {
          Preferences.setBoolean("mapToMadnessReefPurchased", true);
        }
        break;
      case ItemPool.MARINARA_TRENCH_MAP:
        if (responseText.contains("Big Brother shows you the map")) {
          Preferences.setBoolean("mapToTheMarinaraTrenchPurchased", true);
        }
        break;
      case ItemPool.ANEMONE_MINE_MAP:
        if (responseText.contains("Big Brother shows you the map")) {
          Preferences.setBoolean("mapToAnemoneMinePurchased", true);
        }
        break;
      case ItemPool.DIVE_BAR_MAP:
        if (responseText.contains("Big Brother shows you the map")) {
          Preferences.setBoolean("mapToTheDiveBarPurchased", true);
        }
        break;
      case ItemPool.SKATE_PARK_MAP:
        if (responseText.contains("Big Brother shows you the map")) {
          Preferences.setBoolean("mapToTheSkateParkPurchased", true);
        }
        break;
    }
  }

  private static void update() {
    // Definitive checks that we've rescued Big Brother:
    // - We saw it happen
    // - You have a bubblin' stone (a quest item)
    // - We have visited his store

    rescuedBigBrother =
        Preferences.getBoolean("bigBrotherRescued")
            || InventoryManager.getAccessibleCount(BUBBLIN_STONE) > 0;

    if (InventoryManager.getAccessibleCount(AERATED_DIVING_HELMET) > 0) {
      self = AERATED_DIVING_HELMET;
    } else if (InventoryManager.getAccessibleCount(SCHOLAR_MASK) > 0) {
      self = SCHOLAR_MASK;
    } else if (InventoryManager.getAccessibleCount(GLADIATOR_MASK) > 0) {
      self = GLADIATOR_MASK;
    } else if (InventoryManager.getAccessibleCount(CRAPPY_MASK) > 0) {
      self = CRAPPY_MASK;
    } else if (InventoryManager.getAccessibleCount(SCUBA_GEAR) > 0) {
      self = SCUBA_GEAR;
    } else if (InventoryManager.getAccessibleCount(OLD_SCUBA_TANK) > 0) {
      self = OLD_SCUBA_TANK;
    }

    // For the dancing frog, the amphibious tophat is the best familiar equipment
    if (KoLCharacter.getFamiliar().getId() == FamiliarPool.DANCING_FROG
        && InventoryManager.getAccessibleCount(AMPHIBIOUS_TOPHAT) > 0) {
      familiar = AMPHIBIOUS_TOPHAT;
    } else if (InventoryManager.getAccessibleCount(DAS_BOOT) > 0) {
      familiar = DAS_BOOT;
    } else if (InventoryManager.getAccessibleCount(BATHYSPHERE) > 0) {
      familiar = BATHYSPHERE;
    }
  }

  public static String accessible() {
    update();

    if (!rescuedBigBrother) {
      return "You haven't rescued Big Brother yet.";
    }

    if (self == null
        && !KoLCharacter.currentBooleanModifier(BooleanModifier.ADVENTURE_UNDERWATER)) {
      return "You don't have the right equipment to adventure underwater.";
    }

    if (familiar == null
        && !KoLCharacter.currentBooleanModifier(BooleanModifier.UNDERWATER_FAMILIAR)) {
      return "Your familiar doesn't have the right equipment to adventure underwater.";
    }

    return null;
  }

  @Override
  public void equip() {
    update();
    if (!KoLCharacter.currentBooleanModifier(BooleanModifier.ADVENTURE_UNDERWATER)) {
      EquipmentRequest request = new EquipmentRequest(self);
      RequestThread.postRequest(request);
    }

    if (!KoLCharacter.currentBooleanModifier(BooleanModifier.UNDERWATER_FAMILIAR)) {
      EquipmentRequest request = new EquipmentRequest(familiar);
      RequestThread.postRequest(request);
    }
  }

  public static final boolean registerRequest(final String urlString) {
    // We only claim monkeycastle.php?action=buyitem or
    // monkeycastle.php?who=2
    if (!urlString.startsWith("monkeycastle.php")) {
      return false;
    }

    if (!urlString.contains("action=buyitem") && !urlString.contains("who=2")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(BIG_BROTHER, urlString, true);
  }
}
