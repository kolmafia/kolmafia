package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.OrcChasmRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ConditionsCommand extends AbstractCommand {
  private static final Pattern MEAT_PATTERN = Pattern.compile("[\\d,]+ meat");

  public ConditionsCommand() {
    this.usage =
        " clear | check | add <condition> | remove <condition> | set <condition> - modify your adventuring goals.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.equals("")) {
      RequestLogger.printList(GoalManager.getGoals());
      return;
    }

    String option = parameters.split(" ")[0];

    if (option.equals("list")) {
      RequestLogger.printList(GoalManager.getGoals());
      return;
    }

    if (option.equals("clear")) {
      ConditionsCommand.clear();
      return;
    }

    if (option.equals("check")) {
      ConditionsCommand.check();
      return;
    }

    if (option.equals("add") || option.equals("remove") || option.equals("set")) {
      String conditionListString = parameters.substring(option.length()).toLowerCase().trim();
      ConditionsCommand.update(option, conditionListString);
      return;
    }
  }

  public static void clear() {
    GoalManager.clearGoals();
    RequestLogger.printLine("Conditions list cleared.");
  }

  public static void check() {
    KoLmafia.checkRequirements(GoalManager.getGoals());
    RequestLogger.printLine("Conditions list validated against available items.");
  }

  public static boolean update(final String option, final String conditionListString) {
    String[] conditionList = conditionListString.split("\\s*,\\s*");

    boolean hasUpdate = false;

    for (int i = 0; i < conditionList.length; ++i) {
      AdventureResult condition = ConditionsCommand.extractCondition(conditionList[i]);

      if (condition != null) {
        if (option.equals("set")) {
          GoalManager.setGoal(condition);
        } else if (option.equals("remove")) {
          GoalManager.addGoal(condition.getNegation());
        } else if (condition.getCount() > 0) {
          GoalManager.addGoal(condition);
        }

        hasUpdate = true;
      }
    }

    return hasUpdate;
  }

  private static AdventureResult extractCondition(String conditionString) {
    if (conditionString.length() == 0) {
      return null;
    }

    conditionString = conditionString.toLowerCase();

    Matcher meatMatcher = ConditionsCommand.MEAT_PATTERN.matcher(conditionString);
    boolean isMeatCondition =
        meatMatcher.find() ? meatMatcher.group().length() == conditionString.length() : false;

    if (isMeatCondition) {
      String[] splitCondition = conditionString.split("\\s+");
      long amount = StringUtilities.parseInt(splitCondition[0]);
      return new AdventureLongCountResult(AdventureResult.MEAT, amount);
    }

    if (conditionString.endsWith("choiceadv")
        || conditionString.endsWith("choices")
        || conditionString.endsWith("choice")) {
      // If it's a choice adventure condition, parse out the
      // number of choice adventures the user wishes to do.

      String[] splitCondition = conditionString.split("\\s+");
      int count = splitCondition.length > 1 ? StringUtilities.parseInt(splitCondition[0]) : 1;
      return GoalManager.GOAL_CHOICE.getInstance(count);
    }

    if (conditionString.endsWith("manuel")
        || conditionString.endsWith("factoid")
        || conditionString.endsWith("factoids")) {
      // parse the number of manuel entries the user wishes to find

      String[] splitCondition = conditionString.split("\\s+");
      int count = splitCondition.length > 1 ? StringUtilities.parseInt(splitCondition[0]) : 1;
      return GoalManager.GOAL_FACTOID.getInstance(count);
    }

    if (conditionString.endsWith("floundry fish")) {
      // parse the number of fish the user wishes to catch

      String[] splitCondition = conditionString.split("\\s+");
      int count = splitCondition.length > 1 ? StringUtilities.parseInt(splitCondition[0]) : 1;
      return GoalManager.GOAL_FLOUNDRY.getInstance(count);
    }

    if (conditionString.endsWith("autostop")) {
      String[] splitCondition = conditionString.split("\\s+");
      int count = splitCondition.length > 1 ? StringUtilities.parseInt(splitCondition[0]) : 1;
      return GoalManager.GOAL_AUTOSTOP.getInstance(count);
    }

    if (conditionString.endsWith("pirate insult") || conditionString.endsWith("pirate insults")) {
      String[] splitCondition = conditionString.split("\\s+");
      int count = splitCondition.length > 1 ? StringUtilities.parseInt(splitCondition[0]) : 1;
      return new AdventureResult(AdventureResult.PSEUDO_ITEM_PRIORITY, "pirate insult", count) {

        @Override
        public int getCount(List<AdventureResult> list) {
          if (list != KoLConstants.inventory) {
            return 0;
          }
          return BeerPongRequest.countPirateInsults();
        }
      };
    }

    if (conditionString.endsWith("arena flyer ml")) {
      String[] splitCondition = conditionString.split("\\s+");
      int count = splitCondition.length > 1 ? StringUtilities.parseInt(splitCondition[0]) : 1;
      return new AdventureResult(AdventureResult.PSEUDO_ITEM_PRIORITY, "Arena flyer ML", count) {

        @Override
        public int getCount(List<AdventureResult> list) {
          if (list != KoLConstants.inventory) {
            return 0;
          }
          return Preferences.getInteger("flyeredML");
        }
      };
    }

    if (conditionString.equals("chasm bridge")
        || conditionString.endsWith("chasm bridge progress")) {
      String[] splitCondition = conditionString.split("\\s+");
      int count =
          Math.min(
              StringUtilities.isNumeric(splitCondition[0])
                  ? StringUtilities.parseInt(splitCondition[0])
                  : 30,
              30);
      return new AdventureResult(
          AdventureResult.PSEUDO_ITEM_PRIORITY, "Chasm Bridge Progress", count) {

        @Override
        public int getCount(List<AdventureResult> list) {
          if (list != KoLConstants.inventory) {
            return 0;
          }
          return OrcChasmRequest.getChasmProgress();
        }
      };
    }

    if (conditionString.startsWith("level")) {
      // If the condition is a level, then determine how many
      // substat points are required to the next level and
      // add the substat points as a condition.

      String[] splitCondition = conditionString.split("\\s+");
      int level = StringUtilities.parseInt(splitCondition[1]);

      int primeIndex = KoLCharacter.getPrimeIndex();

      GoalManager.GOAL_SUBSTATS_COUNTS[primeIndex] =
          (int)
              (KoLCharacter.calculateSubpoints((level - 1) * (level - 1) + 4, 0)
                  - KoLCharacter.getTotalPrime());

      return GoalManager.GOAL_SUBSTATS;
    }

    if (conditionString.endsWith("muscle")
        || conditionString.endsWith("mysticality")
        || conditionString.endsWith("moxie")
        || conditionString.endsWith("mus")
        || conditionString.endsWith("mys")
        || conditionString.endsWith("myst")
        || conditionString.endsWith("mox")) {
      String[] splitCondition = conditionString.split("\\s+");

      int points = StringUtilities.parseInt(splitCondition[0]);
      int statIndex =
          conditionString.indexOf("mus") != -1 ? 0 : conditionString.indexOf("mys") != -1 ? 1 : 2;

      GoalManager.GOAL_SUBSTATS_COUNTS[statIndex] =
          (int) KoLCharacter.calculateSubpoints(points, 0);
      GoalManager.GOAL_SUBSTATS_COUNTS[statIndex] =
          Math.max(
              0,
              GoalManager.GOAL_SUBSTATS_COUNTS[statIndex]
                  - (int)
                      (conditionString.indexOf("mus") != -1
                          ? KoLCharacter.getTotalMuscle()
                          : conditionString.indexOf("mys") != -1
                              ? KoLCharacter.getTotalMysticality()
                              : KoLCharacter.getTotalMoxie()));

      return GoalManager.GOAL_SUBSTATS;
    }

    if (conditionString.endsWith("health") || conditionString.endsWith("mana")) {
      String type;
      long max, current;

      if (conditionString.endsWith("health")) {
        type = AdventureResult.HP;
        max = KoLCharacter.getMaximumHP();
        current = KoLCharacter.getCurrentHP();
      } else {
        type = AdventureResult.MP;
        max = KoLCharacter.getMaximumMP();
        current = KoLCharacter.getCurrentMP();
      }

      String numberString = conditionString.split("\\s+")[0];
      int points;

      if (numberString.endsWith("%")) {
        int num = StringUtilities.parseInt(numberString.substring(0, numberString.length() - 1));
        points = (int) ((float) num * (float) max / 100.0f);
      } else {
        points = StringUtilities.parseInt(numberString);
      }

      points -= current;

      AdventureResult condition = new AdventureLongCountResult(type, points);

      condition = condition.getInstance(condition.getCount() - GoalManager.getGoalCount(condition));

      return condition;
    }

    if (conditionString.endsWith("outfit")) {
      // Usage: conditions add <location> outfit
      String outfitLocation;

      if (conditionString.equals("outfit")) {
        outfitLocation = Preferences.getString("lastAdventure");
      } else {
        outfitLocation = conditionString.substring(0, conditionString.length() - 7);
      }

      // Try to support outfit names by mapping some outfits to their locations
      if (outfitLocation.equals("guard")
          || outfitLocation.equals("elite")
          || outfitLocation.equals("elite guard")) {
        outfitLocation = "treasury";
      }

      if (outfitLocation.equals("rift")) {
        outfitLocation = "battlefield";
      }

      if (outfitLocation.equals("cloaca-cola") || outfitLocation.equals("cloaca cola")) {
        outfitLocation = "cloaca";
      }

      if (outfitLocation.equals("dyspepsi-cola") || outfitLocation.equals("dyspepsi cola")) {
        outfitLocation = "dyspepsi";
      }

      KoLAdventure lastAdventure = AdventureDatabase.getAdventure(outfitLocation);

      if (!EquipmentManager.addOutfitConditions(lastAdventure)) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "No outfit corresponds to " + lastAdventure.getAdventureName() + ".");
      }

      return null;
    }

    AdventureResult rv = AdventureResult.WildcardResult.getInstance(conditionString);
    return rv != null ? rv : ItemFinder.getFirstMatchingItem(conditionString);
  }
}
