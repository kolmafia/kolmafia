package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

public class MomRequest extends GenericRequest {
  public static final AdventureResult AERATED_DIVING_HELMET =
      ItemPool.get(ItemPool.AERATED_DIVING_HELMET, 1);
  public static final AdventureResult SCUBA_GEAR = ItemPool.get(ItemPool.SCUBA_GEAR, 1);
  public static final AdventureResult BATHYSPHERE = ItemPool.get(ItemPool.BATHYSPHERE, 1);
  public static final AdventureResult DAS_BOOT = ItemPool.get(ItemPool.DAS_BOOT, 1);
  public static final AdventureResult AMPHIBIOUS_TOPHAT =
      ItemPool.get(ItemPool.AMPHIBIOUS_TOPHAT, 1);
  public static final AdventureResult BUBBLIN_STONE = ItemPool.get(ItemPool.BUBBLIN_STONE, 1);
  public static final AdventureResult OLD_SCUBA_TANK = ItemPool.get(ItemPool.OLD_SCUBA_TANK, 1);
  public static final AdventureResult SCHOLAR_MASK = ItemPool.get(ItemPool.SCHOLAR_MASK, 1);
  public static final AdventureResult GLADIATOR_MASK = ItemPool.get(ItemPool.GLADIATOR_MASK, 1);
  public static final AdventureResult CRAPPY_MASK = ItemPool.get(ItemPool.CRAPPY_MASK, 1);

  private static AdventureResult self = null;
  private static AdventureResult familiar = null;

  private int option = 0;

  private static final Pattern ID_PATTERN = Pattern.compile("action=mombuff.*?whichbuff=(\\d+)");

  public static final String[] FOOD = {
    "hot", "cold", "stench", "spooky", "sleaze", "critical", "stats",
  };
  public static final String[] EFFECT = {
    "Hot Sweat",
    "Cold Sweat",
    "Rank Sweat",
    "Black Sweat",
    "Flop Sweat",
    "Mark of Candy Cain",
    "Cereal Killer",
  };

  public MomRequest(final int option) {
    super("monkeycastle.php");

    this.addFormField("action", "mombuff");
    if (option >= 1 && option <= 7) {
      this.option = option;
      this.addFormField("whichbuff", String.valueOf(option));
    }
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public void run() {
    if (this.option == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Decide which food to get.");
      return;
    }

    String reason = MomRequest.accessible();
    if (reason != null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, reason);
      return;
    }
    if (Preferences.getBoolean("_momFoodReceived")) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You have already had food from Mom Sea Monkee today.");
      return;
    }

    try (Checkpoint checkpoint = new Checkpoint()) {
      this.equip();
      KoLmafia.updateDisplay("Visiting Mom...");
      super.run();
    }
  }

  @Override
  public void processResults() {
    if (this.responseText == null
        || this.responseText.equals("")
        || this.responseText.equals(
            "visit the Sea Monkees without some way of breathing underwater")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't get to Mom Sea Monkee");
      return;
    }

    MomRequest.parseResponse(this.getURLString(), this.responseText);

    if (!this.responseText.contains("You acquire an effect")) {
      KoLmafia.updateDisplay("You can only get one food a day from Mom Sea Monkee.");
      Preferences.setBoolean("_momFoodReceived", true);
      return;
    }

    KoLmafia.updateDisplay("You've had some of Mom's food.");
  }

  public static final void parseResponse(final String location, final String responseText) {
    // She looks up at you, and you begin to sweat.
    // You look down at it in horror and break out in a cold sweat as you back away.
    // You feel gross.
    // You feel... wrong.
    // You begin to sweat with anxiety.
    // As the blood spreads out around it, she leans toward you and kisses you on the cheek.
    // The lullaby echoes in your head. You've heard it before. Where?
    if (responseText.contains("begin to sweat")
        || responseText.contains("break out in a cold sweat")
        || responseText.contains("feel gross")
        || responseText.contains("feel... wrong")
        || responseText.contains("begin to sweat with anxiety")
        || responseText.contains("blood spreads out around")
        || responseText.contains("heard it before")) {
      Preferences.setBoolean("_momFoodReceived", true);
      QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, QuestDatabase.FINISHED);
    }
  }

  private static void update() {
    if (InventoryManager.getAccessibleCount(MomRequest.AERATED_DIVING_HELMET) > 0) {
      MomRequest.self = MomRequest.AERATED_DIVING_HELMET;
    } else if (InventoryManager.getAccessibleCount(MomRequest.SCHOLAR_MASK) > 0) {
      MomRequest.self = MomRequest.SCHOLAR_MASK;
    } else if (InventoryManager.getAccessibleCount(MomRequest.GLADIATOR_MASK) > 0) {
      MomRequest.self = MomRequest.GLADIATOR_MASK;
    } else if (InventoryManager.getAccessibleCount(MomRequest.CRAPPY_MASK) > 0) {
      MomRequest.self = MomRequest.CRAPPY_MASK;
    } else if (InventoryManager.getAccessibleCount(MomRequest.SCUBA_GEAR) > 0) {
      MomRequest.self = MomRequest.SCUBA_GEAR;
    } else if (InventoryManager.getAccessibleCount(MomRequest.OLD_SCUBA_TANK) > 0) {
      MomRequest.self = MomRequest.OLD_SCUBA_TANK;
    }

    FamiliarData familiar = KoLCharacter.getFamiliar();

    // For the dancing frog, the amphibious tophat is the best familiar equipment
    if (familiar.getId() == FamiliarPool.DANCING_FROG
        && InventoryManager.getAccessibleCount(MomRequest.AMPHIBIOUS_TOPHAT) > 0) {
      MomRequest.familiar = MomRequest.AMPHIBIOUS_TOPHAT;
    } else if (InventoryManager.getAccessibleCount(MomRequest.DAS_BOOT) > 0) {
      MomRequest.familiar = MomRequest.DAS_BOOT;
    } else if (InventoryManager.getAccessibleCount(MomRequest.BATHYSPHERE) > 0) {
      MomRequest.familiar = MomRequest.BATHYSPHERE;
    }
  }

  public static String accessible() {
    MomRequest.update();

    if (!QuestDatabase.isQuestFinished(Quest.SEA_MONKEES)) {
      return "You haven't rescued Mom yet.";
    }

    if (MomRequest.self == null
        && !KoLCharacter.currentBooleanModifier(BooleanModifier.ADVENTURE_UNDERWATER)) {
      return "You don't have the right equipment to adventure underwater.";
    }

    if (MomRequest.familiar == null
        && !KoLCharacter.currentBooleanModifier(BooleanModifier.UNDERWATER_FAMILIAR)) {
      return "Your familiar doesn't have the right equipment to adventure underwater.";
    }

    return null;
  }

  private void equip() {
    MomRequest.update();
    if (!KoLCharacter.currentBooleanModifier(BooleanModifier.ADVENTURE_UNDERWATER)) {
      EquipmentRequest request = new EquipmentRequest(MomRequest.self);
      RequestThread.postRequest(request);
    }

    if (!KoLCharacter.currentBooleanModifier(BooleanModifier.UNDERWATER_FAMILIAR)) {
      EquipmentRequest request = new EquipmentRequest(familiar);
      RequestThread.postRequest(request);
    }
  }

  public static final boolean registerRequest(final String location) {
    if (!location.startsWith("monkeycastle.php")) {
      return false;
    }

    Matcher matcher = MomRequest.ID_PATTERN.matcher(location);

    if (!matcher.find()) {
      return true;
    }

    RequestLogger.updateSessionLog("mom food " + matcher.group(1));
    return true;
  }
}
