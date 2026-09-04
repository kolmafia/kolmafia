package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MallPriceManager;

/**
 * Applies behavior common to every dispatched effect-source plan.
 *
 * <p>After {@link EffectSourceDispatcher} handles the exceptional game rule, this pipeline checks
 * shared resources and item legality, chooses an acquisition method, validates prices and costs,
 * and renders the final recommendation text. Centralizing those steps keeps source handlers small
 * and prevents subtly different purchase or affordability rules.
 */
final class EffectSourcePlanFinalizer {
  private EffectSourcePlanFinalizer() {}

  static EffectSourceDispatcher.Plan finish(
      EffectSourceDispatcher.Plan plan, EffectSourceDispatcher.Context context, double delta) {
    if (plan.skip) return plan;
    if (plan.item != null && !prepareItem(plan, context)) return plan.skip();
    if (!checkCosts(plan, context)) return plan.skip();
    render(plan, delta);
    return plan;
  }

  private static boolean prepareItem(
      EffectSourceDispatcher.Plan plan, EffectSourceDispatcher.Context context) {
    String itemName = plan.item.getName();
    if (!Maximizer.character().resourceUsage(itemName).isZero()
        || !ItemDatabase.isAllowed(plan.item)) {
      return false;
    }

    plan.fullnessCost = ConsumablesDatabase.getFullness(itemName);
    if (plan.fullnessCost != 0
        && KoLCharacter.getFullness() + plan.fullnessCost > KoLCharacter.getStomachCapacity()) {
      plan.disable();
    }
    plan.inebrietyCost = ConsumablesDatabase.getInebriety(itemName);
    if (plan.inebrietyCost != 0
        && KoLCharacter.getInebriety() + plan.inebrietyCost > KoLCharacter.getLiverCapacity()) {
      plan.disable();
    }
    plan.spleenCost = ConsumablesDatabase.getSpleenHit(itemName);
    if (plan.spleenCost != 0 && !plan.command.contains("chew")) {
      RequestLogger.printLine(
          "(Note: extender for " + context.name() + " is a spleen item that doesn't use 'chew')");
    }
    if (plan.spleenCost != 0
        && KoLCharacter.getSpleenUse() + plan.spleenCost > KoLCharacter.getSpleenLimit()) {
      plan.disable();
    }
    if (!ConsumablesDatabase.meetsLevelRequirement(itemName)) {
      if (!context.includeAll()) return false;
      plan.text = "level up & " + plan.text;
      plan.disable();
    }

    if (!plan.command.isEmpty()) {
      if (!prepareAcquisition(plan, context, itemName)) return false;
    } else if (plan.item.getCount(KoLConstants.inventory) == 0) {
      return false;
    }
    plan.itemsRemaining = plan.item.getCount(KoLConstants.inventory);
    return true;
  }

  private static boolean prepareAcquisition(
      EffectSourceDispatcher.Plan plan, EffectSourceDispatcher.Context context, String itemName) {
    int itemId = plan.item.getItemId();
    EquipScope showScope =
        KoLCharacter.canInteract() ? EquipScope.SPECULATE_ANY : context.equipScope();
    ItemAvailability availability =
        new CheckedItem(itemId, showScope, context.maxPrice(), context.priceLevel()).availability();
    if (availability.total() == 0) return false;

    switch (availability.acquisitionMethod(0)) {
      case ACCESSIBLE -> {
        if (availability.inventory() == 0) {
          String method =
              InventoryManager.simRetrieveItem(
                  plan.item, context.equipScope() == EquipScope.EQUIP_NOW, false);
          if (!method.equals("have")) plan.text = method + " & " + plan.text;
          if (method.equals("uncloset")) {
            plan.command = "closet take 1 \u00B6" + itemId + ";" + plan.command;
          } else if (method.equals("pull")) {
            plan.command = "pull 1 \u00B6" + itemId + ";" + plan.command;
          }
        }
      }
      case CREATE -> {
        plan.text = "make & " + plan.text;
        plan.command = "make \u00B6" + itemId + ";" + plan.command;
        plan.price = ConcoctionPool.get(plan.item).price;
        plan.adventureCost = ConcoctionPool.get(plan.item).getAdventuresNeeded(1);
      }
      case NPC_BUY -> {
        plan.text = "buy & " + plan.text;
        plan.command = "buy 1 \u00B6" + itemId + ";" + plan.command;
        plan.price = ConcoctionPool.get(plan.item).price;
      }
      case PULL -> {
        plan.text = "pull & " + plan.text;
        plan.command = "pull \u00B6" + itemId + ";" + plan.command;
      }
      case MALL_BUY -> {
        plan.text = "acquire & " + plan.text;
        plan.command = "acquire 1 \u00B6" + itemId + ";" + plan.command;
        if (!setMallPrice(plan, itemId, context.maxPrice(), context.priceLevel())) return false;
      }
      case STORAGE_BUY -> {
        plan.text = "buy & pull & " + plan.text;
        plan.command =
            "buy using storage 1 \u00B6" + itemId + ";pull \u00B6" + itemId + ";" + plan.command;
        if (!setMallPrice(plan, itemId, context.maxPrice(), context.priceLevel())) return false;
      }
      case FOLD, PULL_FOLD -> {
        return false;
      }
    }

    if (plan.price > context.maxPrice() || plan.price == -1) return false;
    if (context.priceLevel() == PriceLevel.ALL
        && (availability.initial() > 0
            || availability.creatable() > 0
            || availability.pullable() > 0
            || availability.npcBuyable() > 0)
        && ItemDatabase.isTradeable(itemId)
        && !ClanLoungeRequest.isSpeakeasyDrink(itemName)
        && !setMallPrice(plan, itemId, context.maxPrice(), context.priceLevel())) {
      return false;
    }
    return true;
  }

  private static boolean setMallPrice(
      EffectSourceDispatcher.Plan plan, int itemId, int maxPrice, PriceLevel priceLevel) {
    if (priceLevel == PriceLevel.DONT_CHECK) return true;
    if (MallPriceDatabase.getPrice(itemId) > maxPrice * 2) return false;
    plan.price =
        Preferences.getBoolean("maximizerCurrentMallPrices")
            ? MallPriceManager.getMallPrice(itemId)
            : MallPriceManager.getMallPrice(itemId, 7.0f);
    return true;
  }

  private static boolean checkCosts(
      EffectSourceDispatcher.Plan plan, EffectSourceDispatcher.Context context) {
    if (plan.adventureCost > 0) {
      if (Preferences.getBoolean("maximizerNoAdventures")) return false;
      if (plan.adventureCost > KoLCharacter.getAdventuresLeft()) plan.disable();
    }
    if (plan.fullnessCost != 0
        && KoLCharacter.getFullness() + plan.fullnessCost > KoLCharacter.getStomachCapacity()) {
      plan.disable();
    }
    if (plan.inebrietyCost != 0
        && KoLCharacter.getInebriety() + plan.inebrietyCost > KoLCharacter.getLiverCapacity()) {
      plan.disable();
    }
    if (plan.spleenCost != 0
        && KoLCharacter.getSpleenUse() + plan.spleenCost > KoLCharacter.getSpleenLimit()) {
      plan.disable();
    }
    if (plan.soulsauceCost > 0 && plan.soulsauceCost > KoLCharacter.getSoulsauce()) {
      plan.disable();
    }
    if (plan.thunderCost > 0) {
      if (plan.thunderCost > KoLCharacter.getThunder()) plan.disable();
    } else if (plan.rainCost > 0) {
      if (plan.rainCost > KoLCharacter.getRain()) plan.disable();
    } else if (plan.lightningCost > 0 && plan.lightningCost > KoLCharacter.getLightning()) {
      plan.disable();
    }
    if (plan.hpCost > 0 && plan.hpCost > KoLCharacter.getCurrentHP()) plan.disable();
    if (plan.price > 0) {
      if (plan.command.startsWith("buy using storage")) {
        if (plan.price > KoLCharacter.getStorageMeat()) plan.disable();
      } else if ((plan.command.startsWith("acquire")
              || plan.command.startsWith("make")
              || plan.command.startsWith("buy"))
          && plan.price > KoLCharacter.getAvailableMeat()) {
        plan.disable();
      }
    }
    return true;
  }

  private static void render(EffectSourceDispatcher.Plan plan, double delta) {
    List<String> costs = new ArrayList<>();
    if (plan.adventureCost > 0) costs.add(plan.adventureCost + " adv");
    if (plan.fullnessCost != 0) costs.add(plan.fullnessCost + " full");
    if (plan.inebrietyCost != 0) costs.add(plan.inebrietyCost + " drunk");
    if (plan.spleenCost != 0) costs.add(plan.spleenCost + " spleen");
    if (plan.mpCost > 0) costs.add(plan.mpCost + " mp");
    if (plan.soulsauceCost > 0) costs.add(plan.soulsauceCost + " soulsauce");
    if (plan.thunderCost > 0) costs.add(plan.thunderCost + " dB of thunder");
    else if (plan.rainCost > 0) costs.add(plan.rainCost + " drops of rain");
    else if (plan.lightningCost > 0) costs.add(plan.lightningCost + " bolts of lightning");
    if (plan.hpCost > 0) costs.add(plan.hpCost + " hp");
    if (plan.fuelCost > 0) costs.add(plan.fuelCost + " fuel");
    if (plan.price > 0) {
      costs.add(KoLConstants.COMMA_FORMAT.format(plan.price) + " meat");
    }
    costs.add(KoLConstants.MODIFIER_FORMAT.format(delta));
    plan.text += " (" + String.join(", ", costs) + ")";

    if (!Preferences.getBoolean("verboseMaximizer")) return;
    List<String> details = new ArrayList<>();
    if (plan.duration > 0) {
      details.add(
          plan.duration == 999
              ? "intrinsic"
              : plan.duration == 1 ? "1 adv duration" : plan.duration + " advs duration");
    }
    if (plan.usesRemaining > 0 && plan.usesRemaining < Integer.MAX_VALUE) {
      details.add(
          plan.usesRemaining == 1 ? "1 use remaining" : plan.usesRemaining + " uses remaining");
    }
    if (plan.itemsRemaining > 0) details.add(plan.itemsRemaining + " in inventory");
    if (plan.itemsCreatable > 0) details.add(plan.itemsCreatable + " creatable");
    if (!details.isEmpty()) plan.text += " [" + String.join(", ", details) + "]";
  }
}
