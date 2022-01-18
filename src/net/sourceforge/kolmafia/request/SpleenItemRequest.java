package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.GenericFrame;

public class SpleenItemRequest extends UseItemRequest {
  public SpleenItemRequest(final AdventureResult item) {
    super(ItemDatabase.getConsumptionType(item.getItemId()), item);
  }

  @Override
  public int getAdventuresUsed() {
    return 0;
  }

  public static final int maximumUses(
      final int itemId, final String itemName, final int spleenHit) {
    if (KoLCharacter.inNuclearAutumn() && ConsumablesDatabase.getSpleenHit(itemName) > 1) {
      return 0;
    }

    // Some spleen items also heal HP or MP
    long restorationMaximum = UseItemRequest.getRestorationMaximum(itemName);

    UseItemRequest.limiter =
        (restorationMaximum < Long.MAX_VALUE) ? "needed restoration or spleen" : "spleen";

    int limit = KoLCharacter.getSpleenLimit();
    int spleenLeft = limit - KoLCharacter.getSpleenUse();
    int usableMaximum = spleenLeft / spleenHit;

    switch (itemId) {
      case ItemPool.TURKEY_BLASTER:
        UseItemRequest.limiter = "daily limit";
        return Math.min(usableMaximum, (3 - Preferences.getInteger("_turkeyBlastersUsed")));
      case ItemPool.MOJO_FILTER:
        UseItemRequest.limiter = "daily limit";
        return Math.min(usableMaximum, (3 - Preferences.getInteger("currentMojoFilters")));
      case ItemPool.MANSQUITO_SERUM:
        UseItemRequest.limiter = "daily limit";
        return Preferences.getBoolean("_mansquitoSerumUsed") ? 0 : 1;
      case ItemPool.AUTHORS_INK:
        UseItemRequest.limiter = "daily limit";
        return Preferences.getBoolean("_authorsInkUsed") ? 0 : 1;
      case ItemPool.INQUISITORS_UNIDENTIFIABLE_OBJECT:
        UseItemRequest.limiter = "daily limit";
        return Preferences.getBoolean("_inquisitorsUnidentifiableObjectUsed") ? 0 : 1;
    }

    return (int) Math.min(restorationMaximum, usableMaximum);
  }

  @Override
  public void run() {
    if (GenericRequest.abortIfInFightOrChoice()) {
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

    int iterations = 1;
    int origCount = this.itemUsed.getCount();

    if (origCount > 1 && this.singleConsume()) {
      iterations = origCount;
      this.itemUsed = this.itemUsed.getInstance(1);
    }

    String originalURLString = this.getURLString();

    for (int i = 1; i <= iterations && KoLmafia.permitsContinue(); ++i) {
      if (!this.allowSpleenConsumption()) {
        return;
      }

      this.constructURLString(originalURLString);
      this.useOnce(i, iterations, "Chewing");
    }

    if (KoLmafia.permitsContinue()) {
      KoLmafia.updateDisplay("Finished chewing " + origCount + " " + this.itemUsed.getName() + ".");
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
      UseItemRequest.lastUpdate = "Insufficient items to chew.";
      return;
    }

    this.addFormField("ajax", "1");
    this.addFormField("quantity", String.valueOf(this.itemUsed.getCount()));

    super.runOneIteration(currentIteration, totalIterations, useTypeAsString);
  }

  private boolean singleConsume() {
    return this.consumptionType == KoLConstants.CONSUME_USE;
  }

  private boolean allowSpleenConsumption() {
    if (!GenericFrame.instanceExists()) {
      return true;
    }

    String itemName = this.itemUsed.getName();

    if (!UseItemRequest.askAboutPvP(itemName)) {
      return false;
    }

    return true;
  }

  public static final void parseConsumption(
      final AdventureResult item, final AdventureResult helper, final String responseText) {
    if (responseText.contains("You don't have the item")) {
      // Double clicked a use link, say
      return;
    }

    // Spleen is restricted by Standard.
    if (responseText.contains("That item is too old to be used on this path")) {
      UseItemRequest.lastUpdate = item.getName() + " is too old to be used on this path.";
      KoLmafia.updateDisplay(MafiaState.ERROR, UseItemRequest.lastUpdate);
      return;
    }

    if (responseText.contains("That item isn't usable in quantity")) {
      int attrs = ItemDatabase.getAttributes(item.getItemId());
      if ((attrs & ItemDatabase.ATTR_MULTIPLE) == 0) {
        // Multi-use was attempted and failed, but the request was not generated by KoLmafia
        // because KoLmafia already knows that it cannot be multi-used
        return;
      }
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Internal data error: item incorrectly flagged as multi-usable.");
      return;
    }

    int spleenHit = ConsumablesDatabase.getSpleenHit(item.getName());
    int count = item.getCount();

    if (responseText.contains("rupture")) {
      UseItemRequest.lastUpdate = "Your spleen might go kablooie.";
      KoLmafia.updateDisplay(MafiaState.ERROR, UseItemRequest.lastUpdate);

      // If we have no spleen data for this item, we can't tell what,
      // if anything, consumption did to our spleen.
      if (spleenHit == 0) {
        return;
      }

      int spleenLimit = KoLCharacter.getSpleenLimit();
      int currentSpleen = KoLCharacter.getSpleenUse();

      // Based on what we think our current spleen is,
      // calculate how many of this item we have room for.
      int maxSpleen = (spleenLimit - currentSpleen) / spleenHit;

      // We know that KoL did not let us spleen as many as we
      // requested, so adjust for how many we could spleen.
      int couldSpleen = Math.max(0, Math.min(count - 1, maxSpleen));

      if (couldSpleen > 0) {
        KoLCharacter.setSpleenUse(currentSpleen + couldSpleen * spleenHit);
        ResultProcessor.processResult(item.getInstance(-couldSpleen));
      }

      int estimatedSpleen = spleenLimit - spleenHit + 1;

      if (estimatedSpleen > currentSpleen) {
        KoLCharacter.setSpleenUse(estimatedSpleen);
      }

      KoLCharacter.updateStatus();

      return;
    }

    // The spleen item was consumed successfully
    KoLCharacter.setSpleenUse(KoLCharacter.getSpleenUse() + count * spleenHit);

    ResultProcessor.processResult(item.getNegation());
    KoLCharacter.updateStatus();

    // Re-sort consumables list if needed
    if (Preferences.getBoolean("sortByRoom")) {
      ConcoctionDatabase.getUsables().sort();
    }

    // Perform item-specific processing

    switch (item.getItemId()) {
      case ItemPool.STEEL_SPLEEN:
        if (responseText.contains("You acquire a skill")) {
          ResponseTextParser.learnSkill("Spleen of Steel");
        }
        break;

      case ItemPool.TURKEY_BLASTER:
        if (responseText.contains("can't handle")) {
          Preferences.setInteger("_turkeyBlastersUsed", 3);
        } else {
          int turns = AdventureSpentDatabase.getTurns(Preferences.getString("lastAdventure"));
          if (turns >= 0) {
            AdventureSpentDatabase.setTurns(
                Preferences.getString("lastAdventure"), turns + 5 * count);
          }

          Preferences.increment("_turkeyBlastersUsed", count);
        }
        break;

      case ItemPool.MANSQUITO_SERUM:
        Preferences.setBoolean("_mansquitoSerumUsed", true);
        break;

      case ItemPool.AUTHORS_INK:
        Preferences.setBoolean("_authorsInkUsed", true);
        break;

      case ItemPool.INQUISITORS_UNIDENTIFIABLE_OBJECT:
        Preferences.setBoolean("_inquisitorsUnidentifiableObjectUsed", true);
        break;

      case ItemPool.HOT_JELLY:
        Preferences.increment("_hotJellyUses", count);
        break;

      case ItemPool.SPOOKY_JELLY:
        Preferences.increment("_spookyJellyUses", count);
        break;

      case ItemPool.NIGHTMARE_FUEL:
        Preferences.increment("_nightmareFuelCharges", count);
        break;

      case ItemPool.HOMEBODYL:
        if (responseText.contains("You pop the pill and feel an immediate desire")) {
          Preferences.increment("homebodylCharges", 11);
        }
        break;

      case ItemPool.EXTROVERMECTIN:
        if (responseText.contains("You pop the pill and are immediately overcome")) {
          Preferences.increment("beGregariousCharges", count);
        }
        break;

      case ItemPool.BREATHITIN:
        if (responseText.contains("You pop the pill in your mouth")) {
          Preferences.increment("breathitinCharges", 5);
        }
        break;
    }
  }

  public static final boolean registerRequest() {
    AdventureResult item = UseItemRequest.lastItemUsed;
    int count = item.getCount();
    String name = item.getName();

    String useString = "chew " + count + " " + name;

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(useString);
    return true;
  }
}
