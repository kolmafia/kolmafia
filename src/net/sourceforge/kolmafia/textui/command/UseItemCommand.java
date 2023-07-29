package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.request.DrinkItemRequest;
import net.sourceforge.kolmafia.request.EatItemRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.StillSuitRequest;
import net.sourceforge.kolmafia.request.SushiRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class UseItemCommand extends AbstractCommand {
  public UseItemCommand() {
    this.usage = "[?] [either] <item> [, <item>]... - use/consume items";
  }

  @Override
  public void run(String command, final String parameters) {
    if (command.equals("overdrink") || command.equals("drinksilent")) {
      DrinkItemRequest.ignorePrompt();
      command = "drink";
    } else if (command.equals("eatsilent")) {
      EatItemRequest.ignorePrompt();
      command = "eat";
    }

    var limitmode = KoLCharacter.getLimitMode();

    try (Checkpoint checkpoint = new Checkpoint(() -> KoLCharacter.getLimitMode() != limitmode)) {
      UseItemCommand.use(command, parameters);
    }
  }

  public static void use(final String command, String parameters) {
    UseItemCommand.use(command, parameters, false);
  }

  public static boolean use(final String command, String parameters, boolean sim) {
    if (parameters.equals("")) {
      return false;
    }

    boolean either = parameters.startsWith("either ");
    if (either) {
      parameters = parameters.substring(7).trim();
    }

    if (!sim && (command.equals("eat") || command.equals("eatsilent"))) {
      if (KoLCharacter.inBadMoon() && KitchenCommand.visit(parameters)) {
        return false;
      }
      if (KoLCharacter.canadiaAvailable() && RestaurantCommand.makeChezSnooteeRequest(parameters)) {
        return false;
      }
      if (RestaurantCommand.makeHotDogStandRequest(command, parameters)) {
        return false;
      }
    }

    if (!sim && (command.equals("drink") || command.equals("overdrink"))) {
      if (KoLCharacter.inBadMoon() && KitchenCommand.visit(parameters)) {
        return false;
      }
      if (KoLCharacter.gnomadsAvailable()
          && RestaurantCommand.makeMicroBreweryRequest(parameters)) {
        return false;
      }
      if (RestaurantCommand.makeSpeakeasyRequest(command, parameters)) {
        return false;
      }
      if (StillSuitRequest.isDistillate(parameters)) {
        RequestThread.postRequest(new StillSuitRequest());
        return false;
      }
    }

    // Now, handle the instance where the first item is actually
    // the quantity desired, and the next is the amount to use
    ConsumptionType consumptionType = ConsumptionType.NONE;
    Match filter;

    switch (command) {
      case "eat", "eatsilent" -> {
        consumptionType = ConsumptionType.EAT;
        filter = Match.FOOD;
      }
      case "ghost" -> {
        consumptionType = ConsumptionType.GLUTTONOUS_GHOST;
        filter = Match.FOOD;
      }
      case "drink", "overdrink" -> {
        consumptionType = ConsumptionType.DRINK;
        filter = Match.BOOZE;
      }
      case "hobo" -> {
        consumptionType = ConsumptionType.SPIRIT_HOBO;
        filter = Match.BOOZE;
      }
      case "chew" -> {
        consumptionType = ConsumptionType.SPLEEN;
        filter = Match.SPLEEN;
      }
      case "slimeling" -> {
        consumptionType = ConsumptionType.SLIMELING;
        filter = Match.EQUIP;
      }
      case "robo" -> {
        consumptionType = ConsumptionType.ROBORTENDER;
        filter = Match.ROBO;
      }
      default -> filter = Match.USE;
    }

    AdventureResult[] itemList = ItemFinder.getMatchingItemList(parameters, !sim, null, filter);

    for (int level = either ? 0 : 2; level <= 2; ++level) {
      // level=0: use only items in inventory, exit on first success
      // level=1: buy/make as needed, exit on first success
      // level=2: use all items in list, buy/make as needed
      for (AdventureResult currentMatch : itemList) {
        int itemId = currentMatch.getItemId();
        if (itemId == -1) {
          // We matched a name but didn't resolve
          // it to item ID. This can happen with
          // unidentified bang potions and slime
          // vials - or sushi
          String name = currentMatch.toString();
          String sushi = SushiRequest.isSushiName(name);
          if (sushi != null) {
            if (!sim) {
              RequestLogger.printLine("For now, you must 'create " + sushi + "'");
            }
            continue;
          }

          currentMatch = currentMatch.resolveBangPotion();
          itemId = currentMatch.getItemId();
        }

        if (itemId == -1) {
          if (!sim) {
            RequestLogger.printLine("You have not yet identified the " + currentMatch.toString());
          }
          continue;
        }

        ConsumptionType consumpt = ItemDatabase.getConsumptionType(itemId);

        if (command.equals("eat") && consumpt == ConsumptionType.FOOD_HELPER) { // allowed
        } else if (command.equals("eat") || command.equals("ghost")) {
          if (consumpt != ConsumptionType.EAT) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, currentMatch.getName() + " cannot be consumed.");
            return false;
          }
        }

        if (command.equals("drink") && consumpt == ConsumptionType.DRINK_HELPER) { // allowed
        } else if (command.equals("drink") || command.equals("hobo")) {
          if (consumpt != ConsumptionType.DRINK) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, currentMatch.getName() + " is not an alcoholic beverage.");
            return false;
          }
        } else if (command.equals("chew")) {
          if (consumpt != ConsumptionType.SPLEEN) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, currentMatch.getName() + " is not a spleen toxin.");
            return false;
          }
        }

        if (command.equals("use") && !ItemDatabase.isUsable(itemId)) {
          var correctedUsage =
              switch (consumpt) {
                case EAT, FOOD_HELPER -> "eaten";
                case DRINK, DRINK_HELPER -> "drunk";
                case SPLEEN -> "chewed";
                default -> null;
              };
          if (correctedUsage != null) {
            KoLmafia.updateDisplay(
                MafiaState.ERROR, currentMatch.getName() + " must be " + correctedUsage + ".");
            return false;
          }
        }

        int have = currentMatch.getCount(KoLConstants.inventory);
        if (level > 0 || have > 0) {
          if (level == 0 && have < currentMatch.getCount()) {
            currentMatch = currentMatch.getInstance(have);
          }
          if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
            RequestLogger.printLine(currentMatch.toString());
          } else {
            UseItemRequest request =
                consumptionType != ConsumptionType.NONE
                    ? UseItemRequest.getInstance(consumptionType, currentMatch)
                    : UseItemRequest.getInstance(currentMatch);

            if (sim) {
              // UseItemRequest doesn't really have a "sim" mode, but we can do a pretty good
              // approximation
              // by checking if maximumUses > 0 and we can physically retrieve the item.
              return UseItemRequest.maximumUses(currentMatch.getItemId()) > 0
                  && !InventoryManager.simRetrieveItem(currentMatch, true, true, false)
                      .equalsIgnoreCase("fail");
            }
            RequestThread.postRequest(request.showHTML(false));
            while (FightRequest.inMultiFight && KoLmafia.permitsContinue()) {
              FightRequest.INSTANCE.run();
            }
          }

          if (level < 2) {
            return false;
          }
        }
      }
    }
    return true;
  }
}
