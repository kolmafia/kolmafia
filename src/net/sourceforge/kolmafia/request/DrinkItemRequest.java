package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class DrinkItemRequest extends UseItemRequest {
  private static int ignorePrompt = 0;
  private static int askedAboutOde = 0;
  private static int askedAboutTuxedo = 0;
  private static int askedAboutPinkyRing = 0;
  private static int askedAboutDrunkAvuncular = 0;
  private static AdventureResult queuedDrinkHelper = null;
  private static int queuedDrinkHelperCount = 0;
  public static int boozeConsumed = 0;

  private static final Pattern EVERFULL_GLASS_PATTERN =
      Pattern.compile("Someone must have poured some of their (.*?) into it");

  public DrinkItemRequest(final AdventureResult item) {
    super(ItemDatabase.getConsumptionType(item.getItemId()), item);
  }

  @Override
  public int getAdventuresUsed() {
    return 0;
  }

  public static final void ignorePrompt() {
    DrinkItemRequest.ignorePrompt = KoLCharacter.getUserId();
  }

  public static final void clearBoozeHelper() {
    DrinkItemRequest.queuedDrinkHelper = null;
    DrinkItemRequest.queuedDrinkHelperCount = 0;
  }

  public static final AdventureResult currentDrinkHelper() {
    return (DrinkItemRequest.queuedDrinkHelper != null
            && DrinkItemRequest.queuedDrinkHelperCount > 0)
        ? DrinkItemRequest.queuedDrinkHelper.getInstance(DrinkItemRequest.queuedDrinkHelperCount)
        : null;
  }

  public static final int maximumUses(
      final int itemId, final String itemName, final int inebriety, boolean allowOverDrink) {
    if (KoLCharacter.isJarlsberg()
        && ConcoctionDatabase.getMixingMethod(itemId) != CraftingType.JARLS
        && !itemName.equals("steel margarita")
        && !itemName.equals("mediocre lager")) {
      UseItemRequest.limiter = "its non-Jarlsbergian nature";
      return 0;
    }

    if (KoLCharacter.inHighschool()
        && !itemName.equals("steel margarita")
        && (ConsumablesDatabase.getNotes(itemName) == null
            || !ConsumablesDatabase.getNotes(itemName).startsWith("KOLHS"))) {
      UseItemRequest.limiter = "your unrefined palate";
      return 0;
    }

    if (KoLCharacter.inNuclearAutumn() && ConsumablesDatabase.getInebriety(itemName) > 1) {
      return 0;
    }

    if (KoLCharacter.inBondcore() && !"martini.gif".equals(ItemDatabase.getImage(itemId))) {
      return 0;
    }

    if (KoLCharacter.isVampyre()
        && (ConsumablesDatabase.getNotes(itemName) == null
            || !ConsumablesDatabase.getNotes(itemName).startsWith("Vampyre"))) {
      return 0;
    } else if (!KoLCharacter.isVampyre()
        && ConsumablesDatabase.getNotes(itemName) != null
        && ConsumablesDatabase.getNotes(itemName).startsWith("Vampyre")) {
      return 0;
    }

    UseItemRequest.limiter = "inebriety";
    int limit = KoLCharacter.getInebrietyLimit();
    int maxAvailable = Integer.MAX_VALUE;

    switch (itemId) {
      case ItemPool.MISS_GRAVES_VERMOUTH:
        UseItemRequest.limiter = "daily limit";
        return Preferences.getBoolean("_missGravesVermouthDrunk") ? 0 : 1;

      case ItemPool.MAD_LIQUOR:
        UseItemRequest.limiter = "daily limit";
        return Preferences.getBoolean("_madLiquorDrunk") ? 0 : 1;

      case ItemPool.DOC_CLOCKS_THYME_COCKTAIL:
        UseItemRequest.limiter = "daily limit";
        return Preferences.getBoolean("_docClocksThymeCocktailDrunk") ? 0 : 1;

      case ItemPool.DRIPPY_PILSNER:
        UseItemRequest.limiter = "daily limit";
        return Preferences.getBoolean("_drippyPilsnerUsed") ? 0 : 1;

      case ItemPool.DRIPPY_WINE:
        UseItemRequest.limiter = "daily limit";
        return Preferences.getBoolean("_drippyWineUsed") ? 0 : 1;

      case ItemPool.GREEN_BEER:
        // Green Beer allows drinking to limit + 10,
        // but only on SSPD. For now, always allow
        limit += 10;
        break;

      case ItemPool.RED_DRUNKI_BEAR:
      case ItemPool.GREEN_DRUNKI_BEAR:
      case ItemPool.YELLOW_DRUNKI_BEAR:
        // drunki-bears give inebriety but are limited by your fullness.
        return EatItemRequest.maximumUses(itemId, itemName, 4);

      case ItemPool.EVERFULL_GLASS:
        UseItemRequest.limiter = "daily limit";
        return Preferences.getBoolean("_everfullGlassUsed") ? 0 : 1;
    }

    int inebrietyLeft = limit - KoLCharacter.getInebriety();

    if (inebrietyLeft < 0) {
      // We are already drunk
      return 0;
    }

    int shotglass = 0;
    if (inebriety == 1
        && !ConcoctionDatabase.queuedMimeShotglass
        && InventoryManager.getCount(ItemPool.MIME_SHOTGLASS) > 0
        && !Preferences.getBoolean("_mimeArmyShotglassUsed")) {
      shotglass = 1;
    }

    if (ClanLoungeRequest.isSpeakeasyDrink(ItemDatabase.getItemName(itemId))) {
      // Speakeasy not available in Bad Moon, or without VIP key
      if (KoLCharacter.inBadMoon()) {
        return 0;
      }
      if (InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) == 0) {
        return 0;
      }
      maxAvailable = 3 - Preferences.getInteger("_speakeasyDrinksDrunk");
    }

    if (inebrietyLeft < inebriety) {
      // One drink will make us drunk
      allowOverDrink = true;
    }

    int maxNumber = inebrietyLeft / inebriety + shotglass;

    if (allowOverDrink) {
      // Multiple drinks will make us drunk
      maxNumber++;
    }

    if (maxNumber > maxAvailable) {
      maxNumber = maxAvailable;
    }

    if (itemName.equals("ice stein")) {
      int sixpacks = InventoryManager.getAccessibleCount(ItemPool.ICE_COLD_SIX_PACK);
      if (maxNumber > sixpacks) {
        UseItemRequest.limiter = "ice-cold six-packs";
        return sixpacks;
      }
    }

    return maxNumber;
  }

  @Override
  public void run() {
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    if (this.consumptionType == KoLConstants.CONSUME_DRINK_HELPER) {
      int count = this.itemUsed.getCount();

      if (!InventoryManager.retrieveItem(this.itemUsed)) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Helper not available.");
        return;
      }

      if (this.itemUsed.equals(DrinkItemRequest.queuedDrinkHelper)) {
        DrinkItemRequest.queuedDrinkHelperCount += count;
      } else {
        DrinkItemRequest.queuedDrinkHelper = this.itemUsed;
        DrinkItemRequest.queuedDrinkHelperCount = count;
      }

      KoLmafia.updateDisplay(
          this.itemUsed.getName()
              + " queued for next "
              + count
              + " beverage"
              + (count == 1 ? "" : "s")
              + " drunk.");

      return;
    }

    if (!ConsumablesDatabase.meetsLevelRequirement(this.itemUsed.getName())) {
      UseItemRequest.lastUpdate = "Insufficient level to consume " + this.itemUsed;
      KoLmafia.updateDisplay(MafiaState.ERROR, UseItemRequest.lastUpdate);
      return;
    }

    int itemId = this.itemUsed.getItemId();
    UseItemRequest.lastUpdate = "";

    int maximumUses = UseItemRequest.maximumUses(itemId);
    if (maximumUses < this.itemUsed.getCount()) {
      KoLmafia.updateDisplay(
          "(usable quantity of "
              + this.itemUsed
              + " is limited to "
              + maximumUses
              + " by "
              + UseItemRequest.limiter
              + ")");
      this.itemUsed = this.itemUsed.getInstance(maximumUses);
    }

    if (this.itemUsed.getCount() < 1) {
      return;
    }

    if (itemId == ItemPool.ICE_STEIN) {
      if (!InventoryManager.retrieveItem(ItemPool.ICE_COLD_SIX_PACK, this.itemUsed.getCount())) {
        // This shouldn't happen, since
        // maximumUses( ICE_STEIN ) considers
        // availableCount( ICE_COLD_SIXPACK )
        KoLmafia.updateDisplay(MafiaState.ERROR, "Insufficient ice-cold-six-packs available.");
        return;
      }
    }

    if (!DrinkItemRequest.sequentialConsume(itemId)
        && !InventoryManager.retrieveItem(this.itemUsed)) {
      return;
    }

    int iterations = 1;
    int origCount = this.itemUsed.getCount();

    // The miracle of "consume some" does not apply to TPS drinks
    if (origCount != 1
        && (DrinkItemRequest.singleConsume(itemId)
            || (DrinkItemRequest.sequentialConsume(itemId)
                && InventoryManager.getCount(itemId) < origCount))) {
      iterations = origCount;
      this.itemUsed = this.itemUsed.getInstance(1);
    }

    String originalURLString = this.getURLString();

    for (int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i) {
      DrinkItemRequest.boozeConsumed = i - 1;
      if (!this.allowBoozeConsumption()) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            "Aborted drinking " + this.itemUsed.getCount() + " " + this.itemUsed.getName() + ".");
        return;
      }

      this.constructURLString(originalURLString);
      this.useOnce(i, iterations, "Drinking");
    }

    if (KoLmafia.permitsContinue()) {
      DrinkItemRequest.boozeConsumed = origCount;
      KoLmafia.updateDisplay(
          "Finished drinking " + origCount + " " + this.itemUsed.getName() + ".");
    }
  }

  @Override
  public void useOnce(
      final int currentIteration, final int totalIterations, String useTypeAsString) {
    UseItemRequest.lastUpdate = "";

    // Check to make sure the character has the item in their
    // inventory first - if not, report the error message and
    // return from the method.

    if (!InventoryManager.retrieveItem(this.itemUsed)) {
      UseItemRequest.lastUpdate = "Insufficient items to use.";
      return;
    }

    this.addFormField("ajax", "1");
    this.addFormField("quantity", String.valueOf(this.itemUsed.getCount()));

    if (DrinkItemRequest.queuedDrinkHelper != null && DrinkItemRequest.queuedDrinkHelperCount > 0) {
      int helperItemId = DrinkItemRequest.queuedDrinkHelper.getItemId();
      switch (helperItemId) {
        case ItemPool.FROSTYS_MUG:
          // Check it can be safely used
          UseItemRequest.lastUpdate =
              UseItemRequest.elementalHelper("Coldform", Element.COLD, 1000);
          if (!UseItemRequest.lastUpdate.equals("")) {
            KoLmafia.updateDisplay(MafiaState.ERROR, UseItemRequest.lastUpdate);
            DrinkItemRequest.queuedDrinkHelper = null;
            return;
          }
          // deliberate fallthrough
        case ItemPool.DIVINE_FLUTE:
        case ItemPool.CRIMBCO_MUG:
        case ItemPool.BGE_SHOTGLASS:
          // Items submitted with utensil
          this.addFormField("utensil", String.valueOf(helperItemId));
          break;
        default:
          // Autoused helpers are ignored
          this.removeFormField("utensil");
      }
      DrinkItemRequest.queuedDrinkHelperCount -= 1;
    } else {
      this.removeFormField("utensil");
    }

    super.runOneIteration(currentIteration, totalIterations, useTypeAsString);
  }

  private static boolean singleConsume(final int itemId) {
    // Consume one at a time when a helper is involved.
    // Multi-consume with a helper actually DOES work, even though
    // there is no interface for doing so in game, but that's
    // probably not something that should be relied on.
    return itemId == ItemPool.ICE_STEIN
        || (DrinkItemRequest.queuedDrinkHelper != null
            && DrinkItemRequest.queuedDrinkHelperCount > 0);
  }

  private static boolean sequentialConsume(final int itemId) {
    switch (itemId) {
      case ItemPool.DIRTY_MARTINI:
      case ItemPool.GROGTINI:
      case ItemPool.CHERRY_BOMB:
      case ItemPool.VESPER:
      case ItemPool.BODYSLAM:
      case ItemPool.SANGRIA_DEL_DIABLO:
        // Allow player who owns a single tiny plastic sword to
        // make and drink multiple drinks in succession.
        return true;
    }
    return false;
  }

  private boolean allowBoozeConsumption() {
    // Always allow the steel margarita
    int itemId = this.itemUsed.getItemId();
    if (itemId == ItemPool.STEEL_LIVER) {
      return true;
    }

    int count = this.itemUsed.getCount();
    String itemName = this.itemUsed.getName();

    return DrinkItemRequest.allowBoozeConsumption(itemName, count);
  }

  public static final boolean allowBoozeConsumption(String itemName, int count) {
    int inebriety = ConsumablesDatabase.getInebriety(itemName);
    int mimeShotglass = 0;
    if (inebriety == 1
        && InventoryManager.getCount(ItemPool.MIME_SHOTGLASS) > 0
        && !Preferences.getBoolean("_mimeArmyShotglassUsed")) {
      mimeShotglass = 1;
    }
    int inebrietyBonus = inebriety * count;
    if (inebrietyBonus < 1) {
      return true;
    }

    if (KoLCharacter.isFallingDown()) {
      return true;
    }

    if (!GenericFrame.instanceExists()) {
      return true;
    }

    // Before prompt suppression as it contains automation
    if (!DrinkItemRequest.askAboutOde(itemName, inebriety, count, mimeShotglass)) {
      return false;
    }

    // Before prompt suppression as it contains automation
    if (!DrinkItemRequest.askAboutTuxedo(itemName)) {
      return false;
    }

    // Before prompt suppression as it contains automation
    if (!DrinkItemRequest.askAboutPinkyRing(itemName)) {
      return false;
    }

    if (DrinkItemRequest.ignorePrompt == KoLCharacter.getUserId()) {
      return true;
    }

    if (!UseItemRequest.askAboutPvP(itemName)) {
      return false;
    }

    // Make sure the player does not overdrink if they still
    // have adventures or fullness remaining.

    if (KoLCharacter.getInebriety() + inebrietyBonus - mimeShotglass
        > KoLCharacter.getInebrietyLimit()) {
      FamiliarData stooper = KoLCharacter.findFamiliar(FamiliarPool.STOOPER);
      FamiliarData current = KoLCharacter.getFamiliar();
      if (stooper != null
          && (current == null || stooper != current)
          && !KoLCharacter.inPokefam()
          && !KoLCharacter.inQuantum()
          && !InputFieldUtilities.confirm(
              "Are you sure you want to overdrink before using Stooper?")) {
        return false;
      }
      if ((KoLCharacter.getAdventuresLeft() > 0
              || KoLCharacter.getFullness() < KoLCharacter.getFullnessLimit())
          && !InputFieldUtilities.confirm("Are you sure you want to overdrink?")) {
        return false;
      }
    }

    return true;
  }

  private static boolean askAboutOde(
      String itemName, final int inebriety, final int count, final int shotglass) {
    // Can't use ode if shotglass brings inebriety to 0
    if (inebriety * count - shotglass == 0) {
      return true;
    }

    // If the item doesn't give any adventures, it won't benefit from ode
    String advGain = ConsumablesDatabase.getAdvRangeByName(itemName);
    if (advGain.equals("0")) {
      String note = ConsumablesDatabase.getNotes(itemName);
      if (note == null || (note != null && !note.contains("Unspaded"))) {
        return true;
      }
    }

    if (itemName.equals("Temps Tempranillo")) {
      return true;
    }

    int myUserId = KoLCharacter.getUserId();

    // If we are not in Nuclear Autumn or don't have 7th Floor, don't even consider Drunk and
    // Avuncular
    if (!KoLCharacter.inNuclearAutumn() || Preferences.getInteger("falloutShelterLevel") < 7) {
      DrinkItemRequest.askedAboutDrunkAvuncular = myUserId;
    }

    // Check for Drunk and Avuncular
    boolean skipDrunkAvuncularNag =
        (DrinkItemRequest.askedAboutDrunkAvuncular == myUserId
            || DrinkItemRequest.ignorePrompt == myUserId);
    if (!skipDrunkAvuncularNag) {
      // See if already have Drunk and Avuncular effect
      int drunkAvuncularTurns =
          ConsumablesDatabase.DRUNK_AVUNCULAR.getCount(KoLConstants.activeEffects);

      if (drunkAvuncularTurns < 1) {
        String message = "Are you sure you want to drink without Drunk and Avuncular?";
        if (!InputFieldUtilities.confirm(message)) {
          return false;
        }

        DrinkItemRequest.askedAboutDrunkAvuncular = myUserId;
      }
    }

    // See if already have enough turns of Ode to Booze
    int odeTurns = ConsumablesDatabase.ODE.getCount(KoLConstants.activeEffects);
    int consumptionTurns = count * inebriety - shotglass;

    if (consumptionTurns <= odeTurns) {
      return true;
    }

    // Check if character can cast Ode.
    UseSkillRequest ode = UseSkillRequest.getInstance("The Ode to Booze");
    boolean canOde =
        !KoLCharacter.inGLover()
            && KoLConstants.availableSkills.contains(ode)
            && UseSkillRequest.hasAccordion();
    boolean shouldOde = canOde && KoLCharacter.canInteract();

    if (shouldOde) {
      // Cast Ode automatically if you have enough mana,
      // when you are out of Ronin/HC
      long odeCost = SkillDatabase.getMPConsumptionById(SkillPool.ODE_TO_BOOZE);
      while (odeTurns < consumptionTurns
          && KoLCharacter.getCurrentMP() >= odeCost
          && KoLmafia.permitsContinue()) {
        ode.setBuffCount(1);
        RequestThread.postRequest(ode);
        int newTurns = ConsumablesDatabase.ODE.getCount(KoLConstants.activeEffects);
        if (odeTurns == newTurns) {
          // No progress
          break;
        }
        odeTurns = newTurns;
      }
    }

    if (consumptionTurns <= odeTurns) {
      return true;
    }

    boolean requestBuffOde =
        KoLCharacter.canInteract() && Preferences.getBoolean("odeBuffbotCheck");
    if (canOde || requestBuffOde) {
      boolean skipOdeNag =
          (DrinkItemRequest.askedAboutOde == myUserId || DrinkItemRequest.ignorePrompt == myUserId);

      String message =
          odeTurns > 0
              ? "The Ode to Booze will run out before you finish drinking that. Are you sure?"
              : "Are you sure you want to drink without ode?";

      if (!skipOdeNag && !InputFieldUtilities.confirm(message)) {
        return false;
      }

      DrinkItemRequest.askedAboutOde = myUserId;
    }

    return true;
  }

  private static boolean askAboutTuxedo(String itemName) {
    // Only affects some drinks
    if (!ConsumablesDatabase.isMartini(ItemDatabase.getItemId(itemName))) {
      return true;
    }

    // If we've already asked about Tuxedo, don't nag
    if (DrinkItemRequest.askedAboutTuxedo == KoLCharacter.getUserId()) {
      return true;
    }

    // If equipped already or can't be equipped, or we can't get one, no need to ask
    if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.TUXEDO_SHIRT, 1))
        || !EquipmentManager.canEquip(ItemPool.TUXEDO_SHIRT)
        || !InventoryManager.itemAvailable(ItemPool.TUXEDO_SHIRT)) {
      return true;
    }

    // If autoTuxedo is true, put on Tuxedo
    if (Preferences.getBoolean("autoTuxedo")) {
      if (!InventoryManager.hasItem(ItemPool.TUXEDO_SHIRT, false)) {
        // get tuxedo
        InventoryManager.retrieveItem(ItemPool.TUXEDO_SHIRT);
      }
      RequestThread.postRequest(
          new EquipmentRequest(ItemPool.get(ItemPool.TUXEDO_SHIRT, 1), EquipmentManager.SHIRT));
      if (EquipmentManager.getEquipment(EquipmentManager.SHIRT).getItemId()
          != ItemPool.TUXEDO_SHIRT) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to equip Tuxedo Shirt.");
        return false;
      } else {
        return true;
      }
    }

    if (DrinkItemRequest.ignorePrompt != KoLCharacter.getUserId()
        && !InputFieldUtilities.confirm("Are you sure you want to drink without Tuxedo ?")) {
      return false;
    }

    DrinkItemRequest.askedAboutTuxedo = KoLCharacter.getUserId();

    return true;
  }

  private static boolean askAboutPinkyRing(String itemName) {
    // Only affects some drinks
    if (!ConsumablesDatabase.isWine(ItemDatabase.getItemId(itemName))) {
      return true;
    }

    // If we've already asked about Pinky Ring, don't nag
    if (DrinkItemRequest.askedAboutPinkyRing == KoLCharacter.getUserId()) {
      return true;
    }

    // If equipped already or can't be equipped, or we can't get one, no need to ask
    if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MAFIA_PINKY_RING, 1))
        || !EquipmentManager.canEquip(ItemPool.MAFIA_PINKY_RING)
        || !InventoryManager.itemAvailable(ItemPool.MAFIA_PINKY_RING)) {
      return true;
    }

    // If autoTuxedo is true, put on Mafia Pinky Ring
    if (Preferences.getBoolean("autoPinkyRing")) {
      if (!InventoryManager.hasItem(ItemPool.MAFIA_PINKY_RING, false)) {
        // get Mafia Pinky Ring
        InventoryManager.retrieveItem(ItemPool.MAFIA_PINKY_RING);
      }
      RequestThread.postRequest(
          new EquipmentRequest(
              ItemPool.get(ItemPool.MAFIA_PINKY_RING, 1), EquipmentManager.ACCESSORY3));
      if (EquipmentManager.getEquipment(EquipmentManager.ACCESSORY3).getItemId()
          != ItemPool.MAFIA_PINKY_RING) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to equip mafia pinky ring.");
        return false;
      } else {
        return true;
      }
    }

    if (DrinkItemRequest.ignorePrompt != KoLCharacter.getUserId()
        && !InputFieldUtilities.confirm(
            "Are you sure you want to drink without mafia pinky ring ?")) {
      return false;
    }

    DrinkItemRequest.askedAboutPinkyRing = KoLCharacter.getUserId();

    return true;
  }

  public static final void parseConsumption(
      AdventureResult item, final AdventureResult helper, final String responseText) {
    if (responseText.contains("too drunk")) {
      UseItemRequest.lastUpdate = "Inebriety limit reached.";
      KoLmafia.updateDisplay(MafiaState.ERROR, UseItemRequest.lastUpdate);
      return;
    }

    // You can only drink from your everfull glass once a day.
    if (responseText.contains("only drink from your everfull glass once a day")) {
      UseItemRequest.lastUpdate = "You may only drink from the everfull glass once a day.";
      KoLmafia.updateDisplay(MafiaState.ERROR, UseItemRequest.lastUpdate);
      Preferences.setBoolean("_everfullGlassUsed", true);
      return;
    }

    // Booze is restricted by Standard.
    if (responseText.contains("That item is too old to be used on this path")) {
      UseItemRequest.lastUpdate = item.getName() + " is too old to be used on this path.";
      KoLmafia.updateDisplay(MafiaState.ERROR, UseItemRequest.lastUpdate);
      return;
    }

    // You only have 1 of those, not 7.
    if (responseText.contains("You only have")) {
      return;
    }

    if (responseText.contains("You pour your drink into your mime army shotglass.")) {
      Preferences.setBoolean("_mimeArmyShotglassUsed", true);
    }

    // Check for consumption helpers, which will need to be removed
    // from inventory if they were successfully used.

    if (helper != null) {
      // Check for success message, since there are multiple
      // ways these could fail:

      boolean success = true;

      switch (helper.getItemId()) {
        case ItemPool.DIVINE_FLUTE:
          // "You pour the <drink> into your divine champagne flute, and
          // it immediately begins fizzing over. You drink it quickly,
          // then throw the flute in front of a plastic fireplace and
          // break it."
          //
          // However, the Wiki says this:
          //
          // "When used with booze which grants special effects (such as
          // dusty bottles of wine, tiny plastic sword drinks, or gloomy
          // mushroom wine), all messages related to effects, items, or
          // HP gains/losses are suppressed (though they still take
          // place as usual)."
          //
          // Therefore, just assume it worked.
          break;

        case ItemPool.FROSTYS_MUG:

          // "Brisk! Refreshing! You drink the frigid
          // <drink> and discard the no-longer-frosty
          // mug."

          if (!responseText.contains("discard the no-longer-frosty")) {
            success = false;
          }
          break;
      }

      if (!success) {
        UseItemRequest.lastUpdate = "Consumption helper failed.";
        KoLmafia.updateDisplay(MafiaState.ERROR, UseItemRequest.lastUpdate);
        return;
      }

      // Remove the consumption helper from inventory.
      ResultProcessor.processResult(helper.getNegation());
    }

    int consumptionType = UseItemRequest.getConsumptionType(item);

    // Assume initially that this causes the item to disappear.
    // In the event that the item is not used, then proceed to
    // undo the consumption.

    if (consumptionType == KoLConstants.CONSUME_DRINK_HELPER) {
      // Consumption helpers are removed above when you
      // successfully eat or drink.
      return;
    }

    int itemId = item.getItemId();
    if (itemId == ItemPool.ICE_STEIN) {
      // You're way too drunk already. (checked above)
      // Hmm. One can of beer isn't going to be sufficient for this. This is a job for a six-pack.
      if (responseText.contains("This is a job for a six-pack")) {
        // This shouldn't happen, since maximumUses
        // checked for availability of six-packs
        UseItemRequest.lastUpdate = "Your ice-stein needs an ice-cold six-pack.";
        KoLmafia.updateDisplay(MafiaState.ERROR, UseItemRequest.lastUpdate);
        return;
      }

      // You pull a beer from your six-pack, open it, pour it into the stein, and chug it down.
      // It succeeded. Remove a six-pack from inventory
      ResultProcessor.processItem(ItemPool.ICE_COLD_SIX_PACK, -1);
    }

    // The drink was consumed successfully
    // Everfull glass is not consumed
    if (itemId == ItemPool.EVERFULL_GLASS) {
      Preferences.setBoolean("_everfullGlassUsed", true);
    }
    // Everything else is consumed
    else {
      ResultProcessor.processResult(item.getNegation());
    }

    // Swizzlers and twists of lime are consumed when you drink booze
    int swizzlerCount = InventoryManager.getCount(ItemPool.SWIZZLER);
    if (swizzlerCount > 0) {
      ResultProcessor.processResult(
          ItemPool.get(ItemPool.SWIZZLER, Math.max(-item.getCount(), -swizzlerCount)));
    }

    int limeCount = InventoryManager.getCount(ItemPool.TWIST_OF_LIME);
    if (limeCount > 0) {
      ResultProcessor.processResult(
          ItemPool.get(ItemPool.TWIST_OF_LIME, Math.max(-item.getCount(), -limeCount)));
    }

    // So are black labels, for base booze. Check response text to see if it was used.
    // "You slap a black label on the bottle to make it fancier."
    int labelCount = InventoryManager.getCount(ItemPool.BLACK_LABEL);
    if (labelCount > 0 && responseText.contains("You slap a black label on the bottle")) {
      ResultProcessor.processResult(
          ItemPool.get(ItemPool.BLACK_LABEL, Math.max(-item.getCount(), -labelCount)));
    }

    KoLCharacter.updateStatus();

    // Re-sort consumables list if needed
    if (Preferences.getBoolean("sortByRoom")) {
      ConcoctionDatabase.getUsables().sort();
    }

    // Perform item-specific processing

    switch (itemId) {
      case ItemPool.STEEL_LIVER:
        if (responseText.contains("You acquire a skill")) {
          ResponseTextParser.learnSkill("Liver of Steel");
        }
        return;

      case ItemPool.FERMENTED_PICKLE_JUICE:
        KoLCharacter.setSpleenUse(KoLCharacter.getSpleenUse() - 5 * item.getCount());
        KoLCharacter.updateStatus();
        return;

      case ItemPool.MINI_MARTINI:
        Preferences.increment("miniMartinisDrunk", item.getCount());
        return;

      case ItemPool.BLOODWEISER:
        Preferences.increment("bloodweiserDrunk", item.getCount());
        return;

      case ItemPool.MISS_GRAVES_VERMOUTH:
        Preferences.setBoolean("_missGravesVermouthDrunk", true);
        return;

      case ItemPool.MAD_LIQUOR:
        Preferences.setBoolean("_madLiquorDrunk", true);
        return;

      case ItemPool.DOC_CLOCKS_THYME_COCKTAIL:
        Preferences.setBoolean("_docClocksThymeCocktailDrunk", true);
        return;

      case ItemPool.DRIPPY_PILSNER:
        Preferences.setBoolean("_drippyPilsnerUsed", true);
        Preferences.increment("drippyJuice", 5);
        return;

      case ItemPool.DRIPPY_WINE:
        Preferences.setBoolean("_drippyWineUsed", true);
        Preferences.increment("drippyJuice", 5);
        return;

      case ItemPool.EVERFULL_GLASS:
        {
          // You drink the liquid in the cup.  Someone must have poured some of their horizontal
          // tango into it.
          if (responseText.contains("You drink the liquid in the cup")) {
            Matcher m = EVERFULL_GLASS_PATTERN.matcher(responseText);
            if (m.find()) {
              String booze = m.group(1);
              String message = "Your everfull glass contained some " + booze + "!";
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
            }
          }
          break;
        }
    }
  }

  public static final boolean registerRequest() {
    AdventureResult item = UseItemRequest.lastItemUsed;
    int count = item.getCount();
    String name = item.getName();

    String useString = "drink " + count + " " + name;

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(useString);
    return true;
  }
}
