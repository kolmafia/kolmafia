package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CakeArenaManager;
import net.sourceforge.kolmafia.CakeArenaManager.ArenaOpponent;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.FamiliarTool;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.chat.ChatBuffer;
import net.sourceforge.kolmafia.chat.StyledChatBuffer;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CakeArenaRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

/** Trains the current familiar in the Cake-Shaped Arena until it reaches a goal. */
@SuppressWarnings("incomplete-switch")
public class FamiliarTrainingManager {
  private static final Pattern PRIZE_PATTERN =
      Pattern.compile(
          "You've earned a prize from the Arena Goodies Sack!.*You acquire an item: <b>(.*?)</b>");
  private static final Pattern STEAL_PATTERN =
      Pattern.compile("She also drops an item from her mouth.*You acquire an item: <b>(.*?)</b>");

  private static final StyledChatBuffer results = new StyledChatBuffer("", "blue", false);

  private static int losses = 0;
  private static boolean stop = false;

  public enum Goal {
    BASE,
    BUFFED,
    TURNS,
    LEARN
  }

  // Familiar buffing skills and effects
  public static final AdventureResult EMPATHY = EffectPool.get(EffectPool.EMPATHY);
  private static final AdventureResult LEASH = EffectPool.get(EffectPool.LEASH_OF_LINGUINI);
  private static final AdventureResult BESTIAL_SYMPATHY =
      EffectPool.get(EffectPool.BESTIAL_SYMPATHY);
  private static final AdventureResult BLACK_TONGUE = EffectPool.get(EffectPool.BLACK_TONGUE);
  private static final AdventureResult GREEN_GLOW = EffectPool.get(EffectPool.HEALTHY_GREEN_GLOW);
  private static final AdventureResult GREEN_HEART = EffectPool.get(EffectPool.HEART_OF_GREEN);
  private static final AdventureResult GREEN_TONGUE = EffectPool.get(EffectPool.GREEN_TONGUE);
  private static final AdventureResult HEAVY_PETTING = EffectPool.get(EffectPool.HEAVY_PETTING);
  private static final AdventureResult WORST_ENEMY = EffectPool.get(EffectPool.MANS_WORST_ENEMY);

  // Familiar buffing items
  private static final AdventureResult PITH_HELMET = ItemPool.get(ItemPool.PLEXIGLASS_PITH_HELMET);
  private static final AdventureResult CRUMPLED_FEDORA =
      ItemPool.get(ItemPool.CRUMPLED_FELT_FEDORA);

  private static final AdventureResult BUFFING_SPRAY = ItemPool.get(ItemPool.PET_BUFFING_SPRAY);
  private static final AdventureResult GREEN_SNOWCONE = ItemPool.get(ItemPool.GREEN_SNOWCONE);
  private static final AdventureResult BLACK_SNOWCONE = ItemPool.get(ItemPool.BLACK_SNOWCONE);
  private static final AdventureResult GREEN_CANDY = ItemPool.get(ItemPool.GREEN_CANDY);
  private static final AdventureResult HALF_ORCHID = ItemPool.get(ItemPool.HALF_ORCHID);
  private static final AdventureResult SPIKY_COLLAR = ItemPool.get(ItemPool.SPIKY_COLLAR);

  private static final AdventureResult BAR_WHIP = ItemPool.get(ItemPool.BAR_WHIP);
  private static final int[] tinyPlasticNormal =
      new int[] {
        969, 970, 971, 972, 973, 974, 975, 976, 977, 978, 979, 980, 981, 982, 983, 984, 985, 986,
        987, 988
      };
  private static final int[] tinyPlasticCrimbo = new int[] {1377, 1378, 2201, 2202};

  // Available skills which affect weight
  private static boolean sympathyAvailable;
  private static boolean leashAvailable;
  private static boolean empathyAvailable;

  // Available effects which affect weight
  private static boolean bestialAvailable;
  private static boolean blackConeAvailable;
  private static boolean greenConeAvailable;
  private static boolean greenHeartAvailable;
  private static boolean heavyPettingAvailable;
  private static boolean worstEnemyAvailable;

  // Active effects which affect weight
  private static int leashActive;
  private static int empathyActive;
  private static int bestialActive;
  private static int blackTongueActive;
  private static int greenGlowActive;
  private static int greenHeartActive;
  private static int greenTongueActive;
  private static int heavyPettingActive;
  private static int worstEnemyActive;

  private FamiliarTrainingManager() {}

  public static void stopTraining() {
    FamiliarTrainingManager.stop = true;
  }

  public static final ChatBuffer getResults() {
    return FamiliarTrainingManager.results;
  }

  public static boolean levelFamiliar(final int goal, final Goal type) {
    return FamiliarTrainingManager.levelFamiliar(
        goal, type, Preferences.getBoolean("debugFamiliarTraining"));
  }

  /**
   * Utility method to level the current familiar by fighting the current arena opponents.
   *
   * @param goal Weight goal for the familiar
   * @param type BASE, BUFF, or TURNS
   * @param debug true if we are debugging
   */
  public static final boolean levelFamiliar(final int goal, final Goal type, final boolean debug) {
    // Clear the output
    FamiliarTrainingManager.results.clear();

    // Permit training session to proceed
    FamiliarTrainingManager.stop = false;
    KoLmafia.forceContinue();

    // Get current familiar
    FamiliarData familiar = KoLCharacter.getFamiliar();

    if (familiar == FamiliarData.NO_FAMILIAR) {
      FamiliarTrainingManager.statusMessage(MafiaState.ERROR, "No familiar selected to train.");
      return false;
    }

    if (!familiar.trainable()) {
      FamiliarTrainingManager.statusMessage(
          MafiaState.ERROR, "Don't know how to train a " + familiar.getRace() + " yet.");
      return false;
    }

    try (Checkpoint checkpoint = new Checkpoint()) {
      if (!trainFamiliar(goal, type, debug)) {
        return false;
      }
    }

    if (familiar.canEquipAny()) {
      // Find and wear an appropriate item
      familiar.findAndWearItem(false);
    }

    boolean result = type == Goal.BUFFED ? FamiliarTrainingManager.buffFamiliar(goal) : true;

    FamiliarTrainingManager.statusMessage(MafiaState.CONTINUE, "Training session completed.");
    return result;
  }

  private static boolean trainFamiliar(final int goal, final Goal type, final boolean debug) {
    // Get current familiar
    FamiliarData familiar = KoLCharacter.getFamiliar();

    // Get the status of current familiar
    FamiliarStatus status = new FamiliarStatus();

    // Identify the familiar we are training
    FamiliarTrainingManager.printFamiliar(status, goal, type);
    FamiliarTrainingManager.results.append("<br>");

    // Print available buffs and items and current buffs
    FamiliarTrainingManager.results.append(status.printCurrentBuffs());
    FamiliarTrainingManager.results.append(FamiliarStatus.printAvailableBuffs());
    FamiliarTrainingManager.results.append(status.printCurrentEquipment());
    FamiliarTrainingManager.results.append(status.printAvailableEquipment());
    FamiliarTrainingManager.results.append("<br>");

    // Get opponent list
    LockableListModel<ArenaOpponent> opponents = CakeArenaManager.getOpponentList();

    // Print the opponents
    FamiliarTrainingManager.printOpponents(opponents);
    FamiliarTrainingManager.results.append("<br>");

    // Make a Familiar Tool
    FamiliarTool tool = new FamiliarTool(opponents);

    // Let the battles begin!
    KoLmafia.updateDisplay("Starting training session...");

    // Iterate until we reach the goal
    FamiliarTrainingManager.losses = 0;
    while (!FamiliarTrainingManager.goalMet(status, goal, type)
        && FamiliarTrainingManager.losses < 5) {
      // If user canceled, bail now
      if (FamiliarTrainingManager.stop || !KoLmafia.permitsContinue()) {
        FamiliarTrainingManager.statusMessage(MafiaState.ERROR, "Training session aborted.");
        return false;
      }

      // Make sure you have an adventure left
      if (KoLCharacter.getAdventuresLeft() < 1) {
        FamiliarTrainingManager.statusMessage(
            MafiaState.ERROR, "Training stopped: out of adventures.");
        return false;
      }

      // Make sure you have enough meat to pay for the contest
      if (KoLCharacter.getAvailableMeat() < 100) {
        FamiliarTrainingManager.statusMessage(MafiaState.ERROR, "Training stopped: out of meat.");
        return false;
      }

      // Switch to the required familiar
      if (KoLCharacter.getFamiliar() != familiar) {
        RequestThread.postRequest(new FamiliarRequest(familiar));
      }

      // Choose possible weights
      int[] weights = status.getWeights();

      // Choose next opponent
      ArenaOpponent opponent = tool.bestOpponent(familiar.getId(), weights);

      if (opponent == null) {
        FamiliarTrainingManager.statusMessage(
            MafiaState.ERROR, "Couldn't choose a suitable opponent.");
        return false;
      }

      // Change into appropriate gear
      status.changeGear(tool.bestWeight());
      if (!KoLmafia.permitsContinue()) {
        FamiliarTrainingManager.statusMessage(
            MafiaState.ERROR, "Training stopped: internal error.");
        return false;
      }

      if (debug) {
        break;
      }

      // Enter the contest
      if (FamiliarTrainingManager.fightMatch(status, tool, opponent, tool.bestMatch(), false)
          <= 0) {
        ++FamiliarTrainingManager.losses;
      } else {
        FamiliarTrainingManager.losses = 0;
      }
    }

    if (FamiliarTrainingManager.losses >= 5) {
      FamiliarTrainingManager.statusMessage(MafiaState.ERROR, "Too many consecutive losses.");
      return false;
    }

    return true;
  }

  /**
   * Utility method to derive the arena parameters of the current familiar
   *
   * @param trials How many trials per event
   */
  public static int[] learnFamiliarParameters(final int trials) {
    // Clear the output
    FamiliarTrainingManager.results.clear();

    // Permit training session to proceed
    FamiliarTrainingManager.stop = false;

    // Get current familiar

    if (KoLCharacter.getFamiliar() == FamiliarData.NO_FAMILIAR) {
      FamiliarTrainingManager.statusMessage(MafiaState.ERROR, "No familiar selected to train.");
      return null;
    }

    int events = 12 * trials;
    // Make sure you have enough adventures left
    if (KoLCharacter.getAdventuresLeft() < events) {
      FamiliarTrainingManager.statusMessage(
          MafiaState.ERROR, "You need to have at least " + events + " adventures available.");
      return null;
    }

    // Make sure you have enough meat to pay for the contests
    if (KoLCharacter.getAvailableMeat() < 100 * events) {
      FamiliarTrainingManager.statusMessage(
          MafiaState.ERROR,
          "You need to have at least "
              + KoLConstants.COMMA_FORMAT.format(100 * events)
              + " meat available.");
      return null;
    }

    // Get the status of current familiar
    FamiliarStatus status = new FamiliarStatus();

    // Identify the familiar we are training
    FamiliarTrainingManager.printFamiliar(status, trials, Goal.LEARN);
    FamiliarTrainingManager.results.append("<br>");

    // Print available buffs and items and current buffs
    FamiliarTrainingManager.results.append(status.printCurrentBuffs());
    FamiliarTrainingManager.results.append(FamiliarStatus.printAvailableBuffs());
    FamiliarTrainingManager.results.append(status.printCurrentEquipment());
    FamiliarTrainingManager.results.append(status.printAvailableEquipment());
    FamiliarTrainingManager.results.append("<br>");

    // Get opponent list
    LockableListModel<ArenaOpponent> opponents = CakeArenaManager.getOpponentList();

    // Print the opponents
    FamiliarTrainingManager.printOpponents(opponents);
    FamiliarTrainingManager.results.append("<br>");

    // Make a Familiar Tool
    FamiliarTool tool = new FamiliarTool(opponents);

    // Learned skills
    int[] skills = new int[4];

    try (Checkpoint checkpoint = new Checkpoint()) {
      // Let the battles begin!
      KoLmafia.updateDisplay("Starting training session...");

      // XP earned indexed by [event][rank]
      int[][] xp = new int[4][3];

      // Array of skills to test with
      int[] test = new int[4];

      // Array of contest suckage
      boolean[] suckage = new boolean[4];

      // Iterate for the specified number of trials
      for (int trial = 1; trial <= trials && KoLmafia.permitsContinue(); ++trial) {
        skills =
            FamiliarTrainingManager.learnFamiliarParameters(trial, status, tool, xp, test, suckage);
      }
    }

    return skills;
  }

  private static int[] learnFamiliarParameters(
      final int trial,
      final FamiliarStatus status,
      final FamiliarTool tool,
      final int[][] xp,
      final int[] test,
      final boolean[] suckage) {
    // Iterate through the contests
    for (int contest = 0; contest < 4; ++contest) {
      // Initialize test parameters
      test[0] = test[1] = test[2] = test[3] = 0;

      // Iterate through the ranks
      for (int rank = 0; rank < 3; ++rank) {
        // Skip contests in which the familiar sucks
        if (suckage[contest]) {
          continue;
        }

        // If user canceled, bail now
        if (FamiliarTrainingManager.stop || !KoLmafia.permitsContinue()) {
          FamiliarTrainingManager.printTrainingResults(trial, status, xp, suckage);
          FamiliarTrainingManager.statusMessage(MafiaState.ERROR, "Training session aborted.");
          return null;
        }

        // Initialize test parameters
        test[contest] = rank + 1;

        FamiliarTrainingManager.statusMessage(
            MafiaState.CONTINUE,
            CakeArenaManager.eventIdToName(contest + 1)
                + " rank "
                + (rank + 1)
                + ": trial "
                + trial);

        // Choose possible weights
        int[] weights = status.getWeights();

        // Choose next opponent
        ArenaOpponent opponent = tool.bestOpponent(test, weights);

        if (opponent == null) {
          FamiliarTrainingManager.printTrainingResults(trial, status, xp, suckage);
          FamiliarTrainingManager.statusMessage(
              MafiaState.ERROR, "Couldn't choose a suitable opponent.");
          return null;
        }

        int match = tool.bestMatch();
        if (match != contest + 1) {
          // Informative message only. Do not stop session.
          FamiliarTrainingManager.statusMessage(
              MafiaState.ERROR,
              "Internal error: Selected "
                  + CakeArenaManager.eventIdToName(match)
                  + " rather than "
                  + CakeArenaManager.eventIdToName(contest + 1));
          // Use contest, even if with bad weight
          match = contest + 1;
        }

        // Change into appropriate gear
        status.changeGear(tool.bestWeight());
        if (!KoLmafia.permitsContinue()) {
          FamiliarTrainingManager.printTrainingResults(trial, status, xp, suckage);
          FamiliarTrainingManager.statusMessage(
              MafiaState.ERROR, "Training stopped: internal error.");
          return null;
        }

        // Enter the contest
        int trialXP = FamiliarTrainingManager.fightMatch(status, tool, opponent, match, true);

        if (trialXP < 0) {
          suckage[contest] = true;
        } else {
          xp[contest][rank] += trialXP;
        }
      }
    }

    return FamiliarTrainingManager.printTrainingResults(trial, status, xp, suckage);
  }

  private static int[] printTrainingResults(
      final int trial, final FamiliarStatus status, final int[][] xp, final boolean[] suckage) {
    // Original skill rankings
    int[] original = FamiliarDatabase.getFamiliarSkills(KoLCharacter.getFamiliar().getId());

    // Derived skill rankings
    int[] skills = new int[4];

    StringBuilder text = new StringBuilder();

    text.append(
        "<br>Results for "
            + KoLCharacter.getFamiliar().getRace()
            + " after "
            + trial
            + " trials using "
            + status.turnsUsed()
            + " turns:<br><br>");

    // Open the table
    text.append("<table>");

    // Table header
    text.append("<tr>");
    text.append("<th>Contest</th>");
    text.append("<th>XP[1]</th>");
    text.append("<th>XP[2]</th>");
    text.append("<th>XP[3]</th>");
    text.append("<th>Original Rank</th>");
    text.append("<th>Derived Rank</th>");
    text.append("</tr>");

    for (int contest = 0; contest < 4; ++contest) {
      text.append("<tr>");
      text.append("<td>" + CakeArenaManager.eventIdToName(contest + 1) + "</td>");

      int bestXP = 0;
      int bestRank = 0;
      for (int rank = 0; rank < 3; ++rank) {
        int rankXP = suckage[contest] ? 0 : xp[contest][rank];
        text.append("<td align=center>" + rankXP + "</td>");
        if (rankXP > bestXP) {
          bestXP = rankXP;
          bestRank = rank + 1;
        }
      }

      skills[contest] = bestRank;

      text.append("<td align=center>" + original[contest] + "</td>");
      text.append("<td align=center>" + bestRank + "</td>");
      text.append("</tr>");
    }

    // Close the table
    text.append("</table><br><br>");

    FamiliarTrainingManager.results.append(text.toString());

    return skills;
  }

  /**
   * Utility method to buff the current familiar to the specified weight or higher.
   *
   * @param weight Weight goal for the familiar
   */
  public static final boolean buffFamiliar(final int weight) {
    // Get current familiar. If none, punt.

    FamiliarData familiar = KoLCharacter.getFamiliar();
    if (familiar == FamiliarData.NO_FAMILIAR) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have a familiar equipped.");
      return false;
    }

    // If the familiar is already heavy enough, nothing to do
    if (familiar.getModifiedWeight() >= weight) {
      return true;
    }

    FamiliarStatus status = new FamiliarStatus();
    int[] weights = status.getWeights();
    Arrays.sort(weights);

    status.changeGear(weights[weights.length - 1]);
    if (familiar.getModifiedWeight() >= weight) {
      return true;
    }

    if (!InventoryManager.hasItem(FamiliarData.PUMPKIN_BUCKET)
        && !InventoryManager.hasItem(FamiliarData.FLOWER_BOUQUET)
        && !InventoryManager.hasItem(FamiliarData.FIREWORKS)
        && !InventoryManager.hasItem(FamiliarData.PET_SWEATER)
        && !InventoryManager.hasItem(FamiliarData.SUGAR_SHIELD)
        && status.familiarItemWeight != 0
        && !InventoryManager.hasItem(status.familiarItem)
        && InventoryManager.itemAvailable(status.familiarItem)) {
      KoLmafiaCLI.DEFAULT_SHELL.executeLine("buy 1 " + status.familiarItem.getName());
      RequestThread.postRequest(new EquipmentRequest(status.familiarItem));

      if (familiar.getModifiedWeight() >= weight) {
        return true;
      }
    }

    if (FamiliarTrainingManager.leashAvailable && FamiliarTrainingManager.leashActive == 0) {
      RequestThread.postRequest(UseSkillRequest.getInstance(SkillPool.LEASH_OF_LINGUINI, 1));
      if (familiar.getModifiedWeight() >= weight) {
        return true;
      }
    }

    if (FamiliarTrainingManager.empathyAvailable && FamiliarTrainingManager.empathyActive == 0) {
      RequestThread.postRequest(UseSkillRequest.getInstance(SkillPool.EMPATHY_OF_THE_NEWT, 1));
      if (familiar.getModifiedWeight() >= weight) {
        return true;
      }
    }

    // Add on a green heart first, if you know the difference is
    // less than three.

    if (FamiliarTrainingManager.greenHeartAvailable
        && FamiliarTrainingManager.greenHeartActive == 0
        && familiar.getModifiedWeight() + 3 >= weight) {
      RequestThread.postRequest(UseItemRequest.getInstance(FamiliarTrainingManager.GREEN_CANDY));
      if (familiar.getModifiedWeight() >= weight) {
        return true;
      }
    }

    if (FamiliarTrainingManager.bestialAvailable && FamiliarTrainingManager.bestialActive == 0) {
      RequestThread.postRequest(UseItemRequest.getInstance(FamiliarTrainingManager.HALF_ORCHID));
      if (familiar.getModifiedWeight() >= weight) {
        return true;
      }
    }

    if (FamiliarTrainingManager.heavyPettingAvailable
        && FamiliarTrainingManager.heavyPettingActive == 0) {
      RequestThread.postRequest(UseItemRequest.getInstance(FamiliarTrainingManager.BUFFING_SPRAY));
      if (familiar.getModifiedWeight() >= weight) {
        return true;
      }
    }

    if (FamiliarTrainingManager.greenConeAvailable
        && FamiliarTrainingManager.greenTongueActive == 0) {
      RequestThread.postRequest(UseItemRequest.getInstance(FamiliarTrainingManager.GREEN_SNOWCONE));
      if (familiar.getModifiedWeight() >= weight) {
        return true;
      }
    }

    if (!FamiliarTrainingManager.greenConeAvailable
        && FamiliarTrainingManager.greenTongueActive == 0
        && FamiliarTrainingManager.blackConeAvailable
        && FamiliarTrainingManager.blackTongueActive == 0) {
      RequestThread.postRequest(UseItemRequest.getInstance(FamiliarTrainingManager.BLACK_SNOWCONE));
      if (familiar.getModifiedWeight() >= weight) {
        return true;
      }
    }

    if (FamiliarTrainingManager.worstEnemyAvailable
        && FamiliarTrainingManager.worstEnemyActive == 0) {
      RequestThread.postRequest(UseItemRequest.getInstance(FamiliarTrainingManager.SPIKY_COLLAR));
      if (familiar.getModifiedWeight() >= weight) {
        return true;
      }
    }

    KoLmafia.updateDisplay(
        MafiaState.ERROR, "Can't buff and equip familiar to reach " + weight + " lbs.");
    return false;
  }

  private static void statusMessage(final MafiaState state, final String message) {
    if (state == MafiaState.ERROR || message.endsWith("lost.")) {
      FamiliarTrainingManager.results.append("<font color=red>" + message + "</font><br>");
    } else if (message.contains("experience")) {
      FamiliarTrainingManager.results.append("<font color=green>" + message + "</font><br>");
    } else if (message.contains("prize")) {
      FamiliarTrainingManager.results.append("<font color=blue>" + message + "</font><br>");
    } else {
      FamiliarTrainingManager.results.append(message + "<br>");
    }

    KoLmafia.updateDisplay(state, message);
  }

  private static void printFamiliar(final FamiliarStatus status, final int goal, final Goal type) {
    FamiliarData familiar = status.getFamiliar();
    String name = familiar.getName();
    String race = familiar.getRace();
    int weight = familiar.getWeight();

    String hope =
        switch (type) {
          case BASE -> " to " + goal + " lbs. base weight";
          case BUFFED -> " to " + goal + " lbs. buffed weight";
          case TURNS -> " for " + goal + " turns";
          case LEARN -> " for " + goal + " iterations to learn arena strengths";
        };

    FamiliarTrainingManager.results.append(
        "Training " + name + " the " + weight + " lb. " + race + hope + ".<br>");
  }

  private static void printOpponents(final LockableListModel<ArenaOpponent> opponents) {
    FamiliarTrainingManager.results.append("Opponents:<br>");
    int opponentCount = opponents.size();
    for (ArenaOpponent opponent : opponents) {
      String name = opponent.getName();
      String race = opponent.getRace();
      int weight = opponent.getWeight();
      FamiliarTrainingManager.results.append(name + " the " + weight + " lb. " + race + "<br>");
    }
  }

  private static boolean goalMet(final FamiliarStatus status, final int goal, final Goal type) {
    return switch (type) {
      case BASE -> status.baseWeight() >= goal;
      case BUFFED -> status.maxWeight(true) >= goal;
      case TURNS -> status.turnsUsed() >= goal;
      default -> false;
    };
  }

  private static void printMatch(
      final FamiliarStatus status,
      final ArenaOpponent opponent,
      final FamiliarTool tool,
      final int match) {
    FamiliarData familiar = status.getFamiliar();
    int weight = tool.bestWeight();
    int diff = tool.difference();

    StringBuilder text = new StringBuilder();
    int round = status.turnsUsed() + 1;
    text.append("Round " + round + ": ");
    text.append(familiar.getName());
    text.append(" (" + weight + " lbs.");
    if (diff != 0) {
      text.append("; optimum = ");
      text.append(weight - diff);
      text.append(" lbs.");
    }
    text.append(") vs. " + opponent.getName());
    text.append(" in the " + CakeArenaManager.eventIdToName(match));
    text.append(" event.<br>");

    FamiliarTrainingManager.results.append(text.toString());

    KoLmafia.updateDisplay(
        "Round " + round + ": " + familiar.getName() + " vs. " + opponent.getName() + "...");
  }

  private static int fightMatch(
      final FamiliarStatus status,
      final FamiliarTool tool,
      final ArenaOpponent opponent,
      final int match,
      final boolean ignoreCounters) {
    // If user aborted, bail now
    if (KoLmafia.refusesContinue()) {
      return 0;
    }

    // Tell the user about the match
    FamiliarTrainingManager.printMatch(status, opponent, tool, match);

    // Run the match
    CakeArenaRequest request = new CakeArenaRequest(opponent.getId(), match, ignoreCounters);
    RequestThread.postRequest(request);

    // If the request failed to produce responseText, bail
    if (request.responseText == null) {
      return 0;
    }

    // Figure out how much xp the familiar earned in this contest
    int xp = request.earnedXP();

    // Log the results, add familiar items, deduct turn.
    status.processMatchResult(request, xp);

    // Return the amount of XP the familiar earned
    return request.badContest() ? -1 : xp;
  }

  /**
   * A class to hold everything that can modify the weight of the current familiar: available items
   * and whether they are equipped available skills and whether they are active
   */
  private static class FamiliarStatus {
    // The familiar we are tracking
    FamiliarData familiar;

    // How many turns we have trained it
    int turns;

    // Details about its special familiar item
    AdventureResult familiarItem;
    int familiarItemWeight;

    // Currently equipped gear which affects weight
    AdventureResult hat;
    AdventureResult item;
    AdventureResult weapon;
    AdventureResult offhand;

    AdventureResult[] acc = new AdventureResult[3];

    // Available equipment which affects weight
    AdventureResult specItem;
    int specWeight;

    boolean pithHelmet;
    boolean crumpledFedora;
    boolean leadNecklace;
    boolean ratHeadBalloon;
    boolean bathysphere;
    boolean dasBoot;
    boolean pumpkinBucket;
    boolean flowerBouquet;
    boolean boxFireworks;
    boolean petSweater;
    boolean sugarShield;
    boolean doppelganger;

    int tpCount;
    int whipCount;
    AdventureResult[] tp = new AdventureResult[3];

    // Weights
    TreeSet<Integer> weights;

    // Gear sets
    ArrayList<GearSet> gearSets;

    public FamiliarStatus() {
      // Find out which familiar we are working with
      this.familiar = KoLCharacter.getFamiliar();

      // Get details about the special item it can wear
      int itemId = FamiliarDatabase.getFamiliarItemId(this.familiar.getId());
      this.familiarItem = ItemPool.get(itemId);
      this.familiarItemWeight = FamiliarData.itemWeightModifier(itemId);

      // No turns have been used yet
      this.turns = 0;

      // Initialize set of weights
      this.weights = new TreeSet<>();

      // Initialize the list of GearSets
      this.gearSets = new ArrayList<>();

      // Check skills and equipment
      this.updateStatus();
    }

    public void updateStatus() {
      // Check available skills
      FamiliarStatus.checkSkills();

      // Check current equipment
      this.checkCurrentEquipment();

      // Check available equipment
      this.checkAvailableEquipment((SortedListModel<AdventureResult>) KoLConstants.inventory);
    }

    private static void checkSkills() {
      // Look at skills to decide which ones are possible
      FamiliarTrainingManager.sympathyAvailable = KoLCharacter.hasAmphibianSympathy();
      FamiliarTrainingManager.empathyAvailable =
          KoLCharacter.hasSkill(SkillPool.EMPATHY_OF_THE_NEWT) && UseSkillRequest.hasTotem();
      FamiliarTrainingManager.leashAvailable = KoLCharacter.hasSkill(SkillPool.LEASH_OF_LINGUINI);

      FamiliarTrainingManager.bestialAvailable =
          InventoryManager.itemAvailable(FamiliarTrainingManager.HALF_ORCHID);
      FamiliarTrainingManager.blackConeAvailable =
          InventoryManager.itemAvailable(FamiliarTrainingManager.BLACK_SNOWCONE);
      FamiliarTrainingManager.greenConeAvailable =
          InventoryManager.itemAvailable(FamiliarTrainingManager.GREEN_SNOWCONE);
      FamiliarTrainingManager.greenHeartAvailable =
          InventoryManager.itemAvailable(FamiliarTrainingManager.GREEN_CANDY);
      FamiliarTrainingManager.heavyPettingAvailable =
          InventoryManager.itemAvailable(FamiliarTrainingManager.BUFFING_SPRAY);
      FamiliarTrainingManager.worstEnemyAvailable =
          InventoryManager.itemAvailable(FamiliarTrainingManager.SPIKY_COLLAR);

      // Look at effects to decide which ones are active;
      FamiliarTrainingManager.empathyActive =
          FamiliarTrainingManager.EMPATHY.getCount(KoLConstants.activeEffects);
      FamiliarTrainingManager.leashActive =
          FamiliarTrainingManager.LEASH.getCount(KoLConstants.activeEffects);
      FamiliarTrainingManager.bestialActive =
          FamiliarTrainingManager.BESTIAL_SYMPATHY.getCount(KoLConstants.activeEffects);
      FamiliarTrainingManager.blackTongueActive =
          FamiliarTrainingManager.BLACK_TONGUE.getCount(KoLConstants.activeEffects);
      FamiliarTrainingManager.greenGlowActive =
          FamiliarTrainingManager.GREEN_GLOW.getCount(KoLConstants.activeEffects);
      FamiliarTrainingManager.greenHeartActive =
          FamiliarTrainingManager.GREEN_HEART.getCount(KoLConstants.activeEffects);
      FamiliarTrainingManager.greenTongueActive =
          FamiliarTrainingManager.GREEN_TONGUE.getCount(KoLConstants.activeEffects);
      FamiliarTrainingManager.heavyPettingActive =
          FamiliarTrainingManager.HEAVY_PETTING.getCount(KoLConstants.activeEffects);
      FamiliarTrainingManager.worstEnemyActive =
          FamiliarTrainingManager.WORST_ENEMY.getCount(KoLConstants.activeEffects);
    }

    private void checkCurrentEquipment() {
      this.checkCurrentEquipment(
          EquipmentManager.getEquipment(Slot.WEAPON),
          EquipmentManager.getEquipment(Slot.OFFHAND),
          EquipmentManager.getEquipment(Slot.HAT),
          EquipmentManager.getEquipment(Slot.FAMILIAR),
          EquipmentManager.getEquipment(Slot.ACCESSORY1),
          EquipmentManager.getEquipment(Slot.ACCESSORY2),
          EquipmentManager.getEquipment(Slot.ACCESSORY3));
    }

    private void checkCurrentEquipment(
        final AdventureResult weapon,
        final AdventureResult offhand,
        final AdventureResult hat,
        final AdventureResult item,
        final AdventureResult acc1,
        final AdventureResult acc2,
        final AdventureResult acc3) {
      // Initialize equipment to default
      this.weapon = null;
      this.offhand = null;
      this.hat = null;
      this.item = null;

      this.specItem = null;
      this.specWeight = 0;

      this.pithHelmet = false;
      this.crumpledFedora = false;
      this.leadNecklace = false;
      this.ratHeadBalloon = false;
      this.bathysphere = false;
      this.dasBoot = false;
      this.pumpkinBucket = false;
      this.flowerBouquet = false;
      this.boxFireworks = false;
      this.petSweater = false;
      this.sugarShield = false;
      this.doppelganger = false;

      this.whipCount = 0;
      this.tpCount = 0;

      // Check hat for pithiness
      if (hat.getItemId() == FamiliarTrainingManager.PITH_HELMET.getItemId()) {
        this.pithHelmet = true;
        this.hat = FamiliarTrainingManager.PITH_HELMET;
      }

      if (hat.getItemId() == FamiliarTrainingManager.CRUMPLED_FEDORA.getItemId()) {
        this.crumpledFedora = true;
        this.hat = FamiliarTrainingManager.CRUMPLED_FEDORA;
      }

      if (weapon != null && weapon.getItemId() == FamiliarTrainingManager.BAR_WHIP.getItemId()) {
        ++this.whipCount;
        this.weapon = FamiliarTrainingManager.BAR_WHIP;
      }

      if (offhand != null && offhand.getItemId() == FamiliarTrainingManager.BAR_WHIP.getItemId()) {
        ++this.whipCount;
        this.offhand = FamiliarTrainingManager.BAR_WHIP;
      }

      // Check current familiar item
      if (item != null) {
        int itemId = item.getItemId();

        if (itemId == this.familiarItem.getItemId()) {
          this.item = this.specItem = this.familiarItem;
          this.specWeight = this.familiarItemWeight;
        }

        if (itemId == FamiliarData.PUMPKIN_BUCKET.getItemId()) {
          this.pumpkinBucket = true;
          this.item = FamiliarData.PUMPKIN_BUCKET;
        }

        if (itemId == FamiliarData.FLOWER_BOUQUET.getItemId()) {
          this.flowerBouquet = true;
          this.item = FamiliarData.FLOWER_BOUQUET;
        }

        if (itemId == FamiliarData.FIREWORKS.getItemId()) {
          this.boxFireworks = true;
          this.item = FamiliarData.FIREWORKS;
        }

        if (itemId == FamiliarData.PET_SWEATER.getItemId()) {
          this.petSweater = true;
          this.item = FamiliarData.PET_SWEATER;
        }

        if (itemId == FamiliarData.SUGAR_SHIELD.getItemId()) {
          this.sugarShield = true;
          this.item = FamiliarData.SUGAR_SHIELD;
        }

        if (itemId == FamiliarData.LEAD_NECKLACE.getItemId()) {
          this.leadNecklace = true;
          this.item = FamiliarData.LEAD_NECKLACE;
        }

        if (itemId == FamiliarData.RAT_HEAD_BALLOON.getItemId()) {
          this.ratHeadBalloon = true;
          this.item = FamiliarData.RAT_HEAD_BALLOON;
        }

        if (itemId == ItemPool.BATHYSPHERE) {
          this.bathysphere = true;
          this.item = FamiliarData.BATHYSPHERE;
        }

        if (itemId == ItemPool.DAS_BOOT) {
          this.dasBoot = true;
          this.item = FamiliarData.DAS_BOOT;
        }

        if (itemId == FamiliarData.DOPPELGANGER.getItemId()) {
          this.doppelganger = true;
          this.item = FamiliarData.DOPPELGANGER;
        }
      }

      // Check accessories for tininess and plasticity

      this.checkAccessory(0, acc1);
      this.checkAccessory(1, acc2);
      this.checkAccessory(2, acc3);
    }

    private void checkAccessory(final int index, final AdventureResult accessory) {
      if (FamiliarStatus.isTinyPlasticItem(accessory)) {
        this.acc[index] = accessory;
        this.tp[this.tpCount++] = accessory;
      }
    }

    public static boolean isTinyPlasticItem(final AdventureResult ar) {
      if (ar == null) {
        return false;
      }

      int id = ar.getItemId();
      for (int i = 0; i < FamiliarTrainingManager.tinyPlasticNormal.length; ++i) {
        if (id == FamiliarTrainingManager.tinyPlasticNormal[i]) {
          return true;
        }
      }

      for (int i = 0; i < FamiliarTrainingManager.tinyPlasticCrimbo.length; ++i) {
        if (id == FamiliarTrainingManager.tinyPlasticCrimbo[i]) {
          return true;
        }
      }

      return false;
    }

    private void checkAvailableEquipment(final LockableListModel<AdventureResult> inventory) {
      // If not wearing a pith helmet, search inventory
      this.pithHelmet |=
          FamiliarTrainingManager.PITH_HELMET.getCount(inventory) > 0
              && EquipmentManager.canEquip("plexiglass pith helmet");

      // If not wearing a crumpled fedora helmet, search inventory
      this.crumpledFedora |=
          FamiliarTrainingManager.CRUMPLED_FEDORA.getCount(inventory) > 0
              && EquipmentManager.canEquip("crumpled felt fedora");

      // If current familiar item is not the special item and
      // such an item affects weight, search inventory
      if (this.familiarItem != this.item
          && this.familiarItemWeight != 0
          && this.familiarItem.getCount(inventory) > 0) {
        this.specItem = this.familiarItem;
        this.specWeight = this.familiarItemWeight;
      }

      if (!KoLCharacter.isHardcore()) {
        // If current familiar is not wearing a pumpkin bucket,
        // search inventory
        this.pumpkinBucket |= FamiliarData.PUMPKIN_BUCKET.getCount(inventory) > 0;

        // If current familiar is not wearing a Mayflower bouquet,
        // search inventory
        this.flowerBouquet |= FamiliarData.FLOWER_BOUQUET.getCount(inventory) > 0;

        // If current familiar is not wearing a box of fireworks,
        // search inventory
        this.boxFireworks |= FamiliarData.FIREWORKS.getCount(inventory) > 0;
      }

      // If current familiar is not wearing an astral pet sweater
      // search inventory
      this.petSweater |= FamiliarData.PET_SWEATER.getCount(inventory) > 0;

      // If current familiar is not wearing a sugar shield,
      // search inventory
      this.sugarShield |= FamiliarData.SUGAR_SHIELD.getCount(inventory) > 0;

      // If current familiar is not wearing a lead necklace,
      // search inventory
      this.leadNecklace |= FamiliarData.LEAD_NECKLACE.getCount(inventory) > 0;

      // If current familiar is not wearing a rat head
      // balloon, search inventory
      this.ratHeadBalloon |= FamiliarData.RAT_HEAD_BALLOON.getCount(inventory) > 0;

      // If current familiar is not wearing a bathysphere,
      // search inventory
      this.bathysphere |= FamiliarData.BATHYSPHERE.getCount(inventory) > 0;

      // If current familiar is not wearing das boot
      // search inventory
      this.dasBoot |= FamiliarData.DAS_BOOT.getCount(inventory) > 0;

      // If current familiar is not wearing a doppel,
      // search inventory
      this.doppelganger |= FamiliarData.DOPPELGANGER.getCount(inventory) > 0;

      this.whipCount =
          Math.min(
              KoLCharacter.hasSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING) ? 2 : 1,
              this.whipCount + FamiliarTrainingManager.BAR_WHIP.getCount(inventory));

      // If equipped with fewer than three tiny plastic items
      // equipped, search inventory for more
      for (int i = 0; i < FamiliarTrainingManager.tinyPlasticNormal.length; ++i) {
        this.addTinyPlastic(FamiliarTrainingManager.tinyPlasticNormal[i]);
      }

      // Check Tiny Plastic Crimbo objects
      for (int i = 0; i < FamiliarTrainingManager.tinyPlasticCrimbo.length; ++i) {
        this.addTinyPlastic(FamiliarTrainingManager.tinyPlasticCrimbo[i]);
      }

      // If we're not training a chameleon and we don't have
      // a lead necklace or a rat head balloon, search other
      // familiars; we'll steal it from them if necessary

      if (this.familiar.getId() == FamiliarPool.CHAMELEON
          || this.leadNecklace
              && this.ratHeadBalloon
              && this.pumpkinBucket
              && this.flowerBouquet
              && this.boxFireworks
              && this.petSweater
              && this.sugarShield
              && this.bathysphere
              && this.dasBoot) {
        return;
      }

      // Find first familiar with item

      List<FamiliarData> familiars = KoLCharacter.ownedFamiliars();
      for (FamiliarData familiar : familiars) {
        AdventureResult item = familiar.getItem();

        if (item == null) {
          continue;
        }

        this.leadNecklace |= item.getItemId() == FamiliarData.LEAD_NECKLACE.getItemId();
        this.ratHeadBalloon |= item.getItemId() == FamiliarData.RAT_HEAD_BALLOON.getItemId();
        this.bathysphere |= item.getItemId() == ItemPool.BATHYSPHERE;
        this.dasBoot |= item.getItemId() == ItemPool.DAS_BOOT;
        this.pumpkinBucket |= item.getItemId() == FamiliarData.PUMPKIN_BUCKET.getItemId();
        this.flowerBouquet |= item.getItemId() == FamiliarData.FLOWER_BOUQUET.getItemId();
        this.doppelganger |= item.getItemId() == FamiliarData.DOPPELGANGER.getItemId();
        this.boxFireworks |= item.getItemId() == FamiliarData.FIREWORKS.getItemId();
        this.petSweater |= item.getItemId() == FamiliarData.PET_SWEATER.getItemId();
        this.sugarShield |= item.getItemId() == FamiliarData.SUGAR_SHIELD.getItemId();
      }
    }

    private void addTinyPlastic(final int id) {
      if (this.tpCount == 3) {
        return;
      }

      AdventureResult ar = ItemPool.get(id);
      int count = ar.getCount(KoLConstants.inventory);

      // Make a new one for each slot
      while (count-- > 0 && this.tpCount < 3) {
        this.tp[this.tpCount++] = ItemPool.get(id);
      }
    }

    /** *********************************************************** */
    public int[] getWeights() {
      // Clear the set of possible weights
      this.weights.clear();

      // Calculate base weight
      int weight = this.familiar.getWeight();

      // Sympathy adds 5 lbs. except to a dodecapede, for
      // which it subtracts 5 lbs.
      if (FamiliarTrainingManager.sympathyAvailable) {
        weight += this.familiar.getId() == FamiliarPool.DODECAPEDE ? -5 : 5;
      }
      if (FamiliarTrainingManager.empathyActive > 0) {
        weight += 5;
      }
      if (FamiliarTrainingManager.leashActive > 0) {
        weight += 5;
      }

      // One snowcone effect at a time
      if (FamiliarTrainingManager.greenTongueActive > 0
          || FamiliarTrainingManager.blackTongueActive > 0) {
        weight += 5;
      }

      if (FamiliarTrainingManager.bestialActive > 0) {
        weight += 3;
      }

      if (FamiliarTrainingManager.greenGlowActive > 0) {
        weight += 10;
      }

      if (FamiliarTrainingManager.greenHeartActive > 0) {
        weight += 3;
      }

      if (FamiliarTrainingManager.heavyPettingActive > 0) {
        weight += 5;
      }

      if (FamiliarTrainingManager.worstEnemyActive > 0) {
        weight += 5;
      }

      this.getItemWeights(weight);

      // Make an array to hold values
      Integer[] vals = this.weights.toArray(new Integer[0]);
      int[] value = new int[vals.length];

      // Read Integers from the set and store ints
      for (int i = 0; i < vals.length; ++i) {
        value[i] = vals[i];
      }

      return value;
    }

    private void getItemWeights(final int weight) {
      // Get current familiar
      FamiliarData familiar = KoLCharacter.getFamiliar();

      // Calculate Accessory Weights with no Familiar Items
      this.getAccessoryWeights(weight);

      // Only consider familiar items if current familiar is
      // not a chameleon and you have no doppelganger
      if (familiar.getId() == FamiliarPool.CHAMELEON || this.doppelganger) {
        return;
      }

      // If familiar specific item adds weight, calculate
      if (this.specWeight != 0) {
        this.getAccessoryWeights(weight + this.specWeight);
      }

      // If we have an astral pet sweater, use it
      if (this.petSweater) {
        this.getAccessoryWeights(weight + 10);
      }

      // If we have a sugar shield, use it
      if (this.sugarShield) {
        this.getAccessoryWeights(weight + 10);
      }

      // If we have a pumpkin bucket, use it
      if (this.pumpkinBucket) {
        this.getAccessoryWeights(weight + 5);
      }

      // If we have a Mayflower bouquet, use it
      if (this.flowerBouquet) {
        this.getAccessoryWeights(weight + 5);
      }

      // If we have a little box of fireworks, use it
      if (this.boxFireworks) {
        this.getAccessoryWeights(weight + 5);
      }

      // If we have a lead necklace, use it
      if (this.leadNecklace) {
        this.getAccessoryWeights(weight + 3);
      }

      // If we have a rat head balloon, use it
      if (this.ratHeadBalloon) {
        this.getAccessoryWeights(weight - 3);
      }

      // If we have das boot, use it
      if (this.dasBoot) {
        this.getAccessoryWeights(weight - 10);
      }

      // If we have a bathysphere, use it
      if (this.bathysphere) {
        this.getAccessoryWeights(weight - 20);
      }
    }

    private void getAccessoryWeights(final int weight) {
      // Calculate using variable #s of tiny plastic objects
      for (int i = 0; i <= this.tpCount; ++i) {
        this.getWhipWeights(weight + i);
      }
    }

    private void getWhipWeights(final int weight) {
      // Calculate using variable #s of whips
      for (int i = 0; i <= this.whipCount; ++i) {
        this.getHatWeights(weight + i * 2);
      }
    }

    private void getHatWeights(final int weight) {
      // Add weight with helmet
      if (this.pithHelmet) {
        this.weights.add(Math.max(weight + 5, 1));
      }

      // Add weight with fedora
      if (this.crumpledFedora) {
        this.weights.add(Math.max(weight + 10, 1));
      }

      // Add weight with no helmet
      this.weights.add(Math.max(weight, 1));
    }

    /** *********************************************************** */

    /*
     * Change gear and cast buffs such that your familiar's modified weight is as specified.
     */
    public void changeGear(final int weight) {
      // Make a GearSet describing what we have now
      GearSet current = new GearSet();

      if (this.doppelganger) {
        RequestThread.postRequest(new EquipmentRequest(FamiliarData.DOPPELGANGER, Slot.FAMILIAR));
      }

      // If we are already suitably equipped, stop now
      if (weight == current.weight()) {
        return;
      }

      // Choose a new GearSet with desired weight
      GearSet next = this.chooseGearSet(current, weight);

      // If we couldn't pick one, that's an internal error
      if (next == null || weight < next.weight()) {
        FamiliarTrainingManager.statusMessage(
            MafiaState.ERROR, "Could not select gear set to achieve " + weight + " lbs.");

        if (next == null) {
          FamiliarTrainingManager.results.append("No gear set found.<br>");
        } else {
          FamiliarTrainingManager.results.append(
              "Selected gear set provides " + next.weight() + " lbs.<br>");
        }

        return;
      }

      // Change into the new GearSet
      this.changeGear(current, next);
    }

    /*
     * Debug: choose and print desired GearSet
     */
    public void chooseGear(final int weight) {
      // Make a GearSet describing what we have now
      GearSet current = new GearSet();

      // If we are already suitably equipped, stop now
      if (weight == current.weight()) {
        FamiliarTrainingManager.results.append("Current gear is acceptable/<br>");
        return;
      }

      // Choose a new GearSet with desired weight
      GearSet next = this.chooseGearSet(current, weight);

      if (next == null) {
        FamiliarTrainingManager.results.append(
            "Could not find a gear set to achieve " + weight + " lbs.<br>");
        return;
      }

      FamiliarTrainingManager.results.append(
          "Chosen gear set: " + next + " provides " + next.weight() + " lbs.<br>");
    }

    /*
     * Swap gear and cast buffs to match desired GearSet. Return false if failed to swap or buff
     */

    public void changeGear(final GearSet current, final GearSet next) {
      this.swapItem(current.weapon, next.weapon, Slot.WEAPON);
      this.swapItem(current.offhand, next.offhand, Slot.OFFHAND);
      this.swapItem(current.hat, next.hat, Slot.HAT);
      this.swapItem(current.item, next.item, Slot.FAMILIAR);
      this.swapItem(current.acc1, next.acc1, Slot.ACCESSORY1);
      this.swapItem(current.acc2, next.acc2, Slot.ACCESSORY2);
      this.swapItem(current.acc3, next.acc3, Slot.ACCESSORY3);
    }

    private void swapItem(
        final AdventureResult current, final AdventureResult next, final Slot slot) {
      // Nothing to do if already wearing this item
      if (current == next) {
        return;
      }

      // EquipmentRequest will notice if something else is in
      // the slot and will remove it first, if necessary
      //
      // Therefore, we can simply equip the new item.

      if (next != null && next.getItemId() != -1) {
        FamiliarTrainingManager.results.append("Putting on " + next.getName() + "<br>");
        RequestThread.postRequest(new EquipmentRequest(next, slot));
        this.setItem(slot, next);
        return;
      }

      // Sanity check
      if (next != null) {
        // Internal error: non-null item but itemId == -1
        // Treat it like removing the current item
        System.out.println("Internal error: itemId = -1 in slot " + slot);
        StaticEntity.printStackTrace();
      }

      if (current != null) {
        FamiliarTrainingManager.results.append("Taking off " + current.getName() + "<br>");
        RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, slot));
        this.setItem(slot, null);
      }
    }

    private void setItem(final Slot slot, final AdventureResult item) {
      switch (slot) {
        case WEAPON -> this.weapon = item;
        case OFFHAND -> this.offhand = item;
        case HAT -> this.hat = item;
        case FAMILIAR -> this.item = item;
        case ACCESSORY1 -> this.acc[0] = item;
        case ACCESSORY2 -> this.acc[1] = item;
        case ACCESSORY3 -> this.acc[2] = item;
      }
    }

    /*
     * Choose a GearSet that gives the current familiar the desired weight. Of the (potentially many) possible such
     * sets, return the one that requires the smallest number of changes from what is currently in effect.
     */
    private GearSet chooseGearSet(final GearSet current, final int weight) {
      // Clear out the accumulated list of GearSets

      this.gearSets.clear();
      this.getHatGearSets(weight);

      // Iterate over all the GearSets and choose the first
      // one which is closest to the current GearSet

      Collections.sort(this.gearSets);
      return this.gearSets.isEmpty() ? null : this.gearSets.get(0);
    }

    private void getHatGearSets(final int weight) {
      if (this.pithHelmet) {
        this.getItemGearSets(weight, FamiliarTrainingManager.PITH_HELMET);
      }

      if (this.crumpledFedora) {
        this.getItemGearSets(weight, FamiliarTrainingManager.CRUMPLED_FEDORA);
      }

      this.getItemGearSets(weight, null);
    }

    private void getItemGearSets(final int weight, final AdventureResult hat) {
      // If it's a comma chameleon or we have a doppelganger,
      // don't look at familiar items.

      if (KoLCharacter.getFamiliar().getId() == FamiliarPool.CHAMELEON) {
        this.getAccessoryGearSets(weight, null, hat);
        return;
      }

      if (this.doppelganger) {
        this.getAccessoryGearSets(weight, FamiliarData.DOPPELGANGER, hat);
        return;
      }

      if (this.specItem != null) {
        this.getAccessoryGearSets(weight, this.specItem, hat);
      }
      if (this.pumpkinBucket) {
        this.getAccessoryGearSets(weight, FamiliarData.PUMPKIN_BUCKET, hat);
      }
      if (this.flowerBouquet) {
        this.getAccessoryGearSets(weight, FamiliarData.FLOWER_BOUQUET, hat);
      }
      if (this.boxFireworks) {
        this.getAccessoryGearSets(weight, FamiliarData.FIREWORKS, hat);
      }
      if (this.petSweater) {
        this.getAccessoryGearSets(weight, FamiliarData.PET_SWEATER, hat);
      }
      if (this.sugarShield) {
        this.getAccessoryGearSets(weight, FamiliarData.SUGAR_SHIELD, hat);
      }
      if (this.leadNecklace) {
        this.getAccessoryGearSets(weight, FamiliarData.LEAD_NECKLACE, hat);
      }
      if (this.ratHeadBalloon) {
        this.getAccessoryGearSets(weight, FamiliarData.RAT_HEAD_BALLOON, hat);
      }
      if (this.bathysphere) {
        this.getAccessoryGearSets(weight, FamiliarData.BATHYSPHERE, hat);
      }
      if (this.dasBoot) {
        this.getAccessoryGearSets(weight, FamiliarData.DAS_BOOT, hat);
      }

      this.getAccessoryGearSets(weight, null, hat);
    }

    private void getAccessoryGearSets(
        final int weight, AdventureResult item, final AdventureResult hat) {
      if (item != null && item.getItemId() == -1) {
        System.out.println("Internal error: familiar item has itemId = -1");
        StaticEntity.printStackTrace();
        item = null;
      }

      // No matter how many Tiny Plastic Objects we have, a
      // configuration with none equipped is legal
      this.addGearSet(weight, null, null, null, item, hat);
      if (this.tpCount == 0) {
        return;
      }

      // If we have at least one and it started out equipped,
      // then it might be in any of the three accessory
      // slots.
      this.addGearSet(weight, this.tp[0], null, null, item, hat);
      this.addGearSet(weight, null, this.tp[0], null, item, hat);
      this.addGearSet(weight, null, null, this.tp[0], item, hat);
      if (this.tpCount == 1) {
        return;
      }

      // If we have at least two and they were both equipped
      // when we came in, they'll be in one of the following
      // patterns.

      this.addGearSet(weight, this.tp[0], this.tp[1], null, item, hat);
      this.addGearSet(weight, this.tp[0], null, this.tp[1], item, hat);
      this.addGearSet(weight, null, this.tp[0], this.tp[1], item, hat);

      // If one of the two was in the inventory, the first
      // could have been in any of the three accessory
      // slots. Add a pattern for where it was in the third
      // slot.

      this.addGearSet(weight, this.tp[1], null, this.tp[0], item, hat);

      if (this.tpCount == 2) {
        return;
      }

      // If we have three and they were all equipped when we
      // came in, they'll be in the following pattern
      this.addGearSet(weight, this.tp[0], this.tp[1], this.tp[2], item, hat);

      // If two of them were equipped and the third was in
      // the inventory, the following patterns are legal
      this.addGearSet(weight, this.tp[0], this.tp[2], this.tp[1], item, hat);
      this.addGearSet(weight, this.tp[2], this.tp[0], this.tp[1], item, hat);

      // If only one was equipped, based on which
      // two-accessory patterns are legal, the following
      // three-accessory patterns are also legal
      this.addGearSet(weight, this.tp[1], this.tp[2], this.tp[0], item, hat);
    }

    private void addGearSet(
        final int weight,
        final AdventureResult acc1,
        final AdventureResult acc2,
        final AdventureResult acc3,
        final AdventureResult item,
        final AdventureResult hat) {
      int gearWeight = this.gearSetWeight(null, null, acc1, acc2, acc3, item, hat);
      if (weight == gearWeight) {
        this.gearSets.add(new GearSet(null, null, acc1, acc2, acc3, item, hat));
      }

      if (this.whipCount > 0) {
        gearWeight =
            this.gearSetWeight(FamiliarTrainingManager.BAR_WHIP, null, acc1, acc2, acc3, item, hat);
        if (weight == gearWeight) {
          this.gearSets.add(
              new GearSet(FamiliarTrainingManager.BAR_WHIP, null, acc1, acc2, acc3, item, hat));
        }
      }

      if (this.whipCount > 1) {
        gearWeight =
            this.gearSetWeight(
                FamiliarTrainingManager.BAR_WHIP,
                FamiliarTrainingManager.BAR_WHIP,
                acc1,
                acc2,
                acc3,
                item,
                hat);
        if (weight == gearWeight) {
          this.gearSets.add(
              new GearSet(
                  FamiliarTrainingManager.BAR_WHIP,
                  FamiliarTrainingManager.BAR_WHIP,
                  acc1,
                  acc2,
                  acc3,
                  item,
                  hat));
        }
      }
    }

    private int gearSetWeight(
        final AdventureResult weapon,
        final AdventureResult offhand,
        final AdventureResult acc1,
        final AdventureResult acc2,
        final AdventureResult acc3,
        final AdventureResult item,
        final AdventureResult hat) {
      int weight = this.familiar.getWeight();

      if (hat == FamiliarTrainingManager.CRUMPLED_FEDORA) {
        weight += 10;
      } else if (hat == FamiliarTrainingManager.PITH_HELMET) {
        weight += 5;
      }

      if (FamiliarTrainingManager.sympathyAvailable) {
        weight += this.familiar.getId() == FamiliarPool.DODECAPEDE ? -5 : 5;
      }

      if (FamiliarTrainingManager.leashActive > 0) {
        weight += 5;
      }
      if (FamiliarTrainingManager.empathyActive > 0) {
        weight += 5;
      }
      if (FamiliarTrainingManager.bestialActive > 0) {
        weight += 3;
      }
      if (FamiliarTrainingManager.greenGlowActive > 0) {
        weight += 10;
      }
      if (FamiliarTrainingManager.greenHeartActive > 0) {
        weight += 3;
      }
      if (FamiliarTrainingManager.greenTongueActive > 0
          || FamiliarTrainingManager.blackTongueActive > 0) {
        weight += 5;
      }
      if (FamiliarTrainingManager.heavyPettingActive > 0) {
        weight += 5;
      }
      if (FamiliarTrainingManager.worstEnemyActive > 0) {
        weight += 5;
      }

      if (item == FamiliarData.DOPPELGANGER) {
      } else if (item == this.specItem) {
        weight += this.specWeight;
      } else if (item == FamiliarData.PET_SWEATER) {
        weight += 10;
      } else if (item == FamiliarData.SUGAR_SHIELD) {
        weight += 10;
      } else if (item == FamiliarData.PUMPKIN_BUCKET) {
        weight += 5;
      } else if (item == FamiliarData.FLOWER_BOUQUET) {
        weight += 5;
      } else if (item == FamiliarData.FIREWORKS) {
        weight += 5;
      } else if (item == FamiliarData.LEAD_NECKLACE) {
        weight += 3;
      } else if (item == FamiliarData.RAT_HEAD_BALLOON) {
        weight -= 3;
      } else if (item == FamiliarData.BATHYSPHERE) {
        weight -= 20;
      } else if (item == FamiliarData.DAS_BOOT) {
        weight -= 10;
      }

      if (weapon == FamiliarTrainingManager.BAR_WHIP) {
        weight += 2;
      }
      if (offhand == FamiliarTrainingManager.BAR_WHIP) {
        weight += 2;
      }

      if (FamiliarStatus.isTinyPlasticItem(acc1)) {
        weight += 1;
      }
      if (FamiliarStatus.isTinyPlasticItem(acc2)) {
        weight += 1;
      }
      if (FamiliarStatus.isTinyPlasticItem(acc3)) {
        weight += 1;
      }

      return Math.max(weight, 1);
    }

    /** *********************************************************** */
    public void processMatchResult(final CakeArenaRequest request, final int xp) {
      String response = request.responseText;

      // If the contest did not take place, bail now
      if (!response.contains("You enter")) {
        return;
      }

      // Find and report how much experience was gained
      String message;
      if (xp > 0) {
        message =
            this.familiar.getName()
                + " gains "
                + xp
                + " experience"
                + (response.contains("gains a pound") ? " and a pound." : ".");
      } else {
        message = this.familiar.getName() + " lost.";
      }

      FamiliarTrainingManager.statusMessage(MafiaState.CONTINUE, message);

      // If a prize was won, report it
      Matcher prizeMatcher = FamiliarTrainingManager.PRIZE_PATTERN.matcher(response);
      Matcher stealMatcher = FamiliarTrainingManager.STEAL_PATTERN.matcher(response);
      String prize = null;

      if (prizeMatcher.find()) {
        prize = prizeMatcher.group(1);
        FamiliarTrainingManager.statusMessage(
            MafiaState.CONTINUE, "You win a prize: " + prize + ".");
      } else if (stealMatcher.find()) {
        prize = stealMatcher.group(1);
        FamiliarTrainingManager.statusMessage(
            MafiaState.CONTINUE, "Your familiar steals an item: " + prize + ".");
      }

      if (prize != null) {
        if (prize.equals(FamiliarData.LEAD_NECKLACE.getName())) {
          this.leadNecklace = true;
        } else if (this.familiarItemWeight > 0) {
          if (this.specItem == null) {
            this.specItem = this.familiarItem;
            this.specWeight = this.familiarItemWeight;
          }
        }
      }

      // Increment count of turns in this training session
      this.turns++;

      // Decrement buffs
      if (FamiliarTrainingManager.leashActive > 0) {
        FamiliarTrainingManager.leashActive--;
      }
      if (FamiliarTrainingManager.empathyActive > 0) {
        FamiliarTrainingManager.empathyActive--;
      }
      if (FamiliarTrainingManager.bestialActive > 0) {
        FamiliarTrainingManager.bestialActive--;
      }
      if (FamiliarTrainingManager.blackTongueActive > 0) {
        FamiliarTrainingManager.blackTongueActive--;
      }
      if (FamiliarTrainingManager.greenGlowActive > 0) {
        FamiliarTrainingManager.greenHeartActive--;
      }
      if (FamiliarTrainingManager.greenHeartActive > 0) {
        FamiliarTrainingManager.greenHeartActive--;
      }
      if (FamiliarTrainingManager.greenTongueActive > 0) {
        FamiliarTrainingManager.greenTongueActive--;
      }
      if (FamiliarTrainingManager.heavyPettingActive > 0) {
        FamiliarTrainingManager.heavyPettingActive--;
      }
      if (FamiliarTrainingManager.worstEnemyActive > 0) {
        FamiliarTrainingManager.worstEnemyActive--;
      }
    }

    public FamiliarData getFamiliar() {
      return this.familiar;
    }

    public int turnsUsed() {
      return this.turns;
    }

    public int baseWeight() {
      return this.familiar.getWeight();
    }

    public int maxWeight(final boolean buffs) {
      // Start with current base weight of familiar
      int weight = this.familiar.getWeight();

      // Add possible skills
      if (FamiliarTrainingManager.sympathyAvailable) {
        weight += this.familiar.getId() == FamiliarPool.DODECAPEDE ? -5 : 5;
      }

      if (buffs) {
        if (FamiliarTrainingManager.leashAvailable || FamiliarTrainingManager.leashActive > 0) {
          weight += 5;
        }
        if (FamiliarTrainingManager.empathyAvailable || FamiliarTrainingManager.empathyActive > 0) {
          weight += 5;
        }
        if (FamiliarTrainingManager.bestialAvailable || FamiliarTrainingManager.bestialActive > 0) {
          weight += 3;
        }
        if (FamiliarTrainingManager.greenConeAvailable
            || FamiliarTrainingManager.greenTongueActive > 0
            || FamiliarTrainingManager.blackConeAvailable
            || FamiliarTrainingManager.blackTongueActive > 0) {
          weight += 5;
        }
        if (FamiliarTrainingManager.greenGlowActive > 0) {
          weight += 10;
        }
        if (FamiliarTrainingManager.greenHeartAvailable
            || FamiliarTrainingManager.greenHeartActive > 0) {
          weight += 3;
        }
        if (FamiliarTrainingManager.heavyPettingAvailable
            || FamiliarTrainingManager.heavyPettingActive > 0) {
          weight += 5;
        }
        if (FamiliarTrainingManager.worstEnemyAvailable
            || FamiliarTrainingManager.worstEnemyActive > 0) {
          weight += 5;
        }
      } else {
        if (FamiliarTrainingManager.leashActive > 0) {
          weight += 5;
        }
        if (FamiliarTrainingManager.empathyActive > 0) {
          weight += 5;
        }
        if (FamiliarTrainingManager.bestialActive > 0) {
          weight += 3;
        }
        if (FamiliarTrainingManager.greenGlowActive > 0) {
          weight += 10;
        }
        if (FamiliarTrainingManager.greenHeartActive > 0) {
          weight += 3;
        }
        if (FamiliarTrainingManager.greenTongueActive > 0
            || FamiliarTrainingManager.blackTongueActive > 0) {
          weight += 5;
        }
        if (FamiliarTrainingManager.heavyPettingActive > 0) {
          weight += 5;
        }
        if (FamiliarTrainingManager.worstEnemyActive > 0) {
          weight += 5;
        }
      }

      // Add available familiar items
      if (this.pumpkinBucket) {
        weight += 5;
      } else if (this.flowerBouquet) {
        weight += 5;
      } else if (this.boxFireworks) {
        weight += 5;
      } else if (this.petSweater) {
        weight += 10;
      } else if (this.sugarShield) {
        weight += 10;
      } else if (this.specWeight > 3) {
        weight += this.specWeight;
      } else if (this.leadNecklace) {
        weight += 3;
      }

      // Add available tiny plastic items
      weight += this.tpCount;
      weight += 2 * this.whipCount;

      // Add crumpled fedora
      if (this.crumpledFedora) {
        weight += 10;
      } else if (this.pithHelmet) {
        weight += 5;
      }

      return Math.max(weight, 1);
    }

    public static String printAvailableBuffs() {
      StringBuilder text = new StringBuilder();

      text.append("Castable buffs:");
      if (FamiliarTrainingManager.empathyAvailable) {
        text.append(" Empathy (+5)");
      }
      if (FamiliarTrainingManager.leashAvailable) {
        text.append(" Leash (+5)");
      }
      if (!FamiliarTrainingManager.empathyAvailable && !FamiliarTrainingManager.leashAvailable) {
        text.append(" None");
      }
      text.append("<br>");

      return text.toString();
    }

    public String printCurrentBuffs() {
      StringBuilder text = new StringBuilder();

      text.append("Current buffs:");
      if (FamiliarTrainingManager.sympathyAvailable) {
        text.append(
            " Sympathy ("
                + (this.familiar.getId() == FamiliarPool.DODECAPEDE ? "-" : "+")
                + "5 permanent)");
      }
      if (FamiliarTrainingManager.empathyActive > 0) {
        text.append(" Empathy (+5 for " + FamiliarTrainingManager.empathyActive + " turns)");
      }
      if (FamiliarTrainingManager.leashActive > 0) {
        text.append(" Leash (+5 for " + FamiliarTrainingManager.leashActive + " turns)");
      }
      if (FamiliarTrainingManager.bestialActive > 0) {
        text.append(
            " Bestial Sympathy (+3 for " + FamiliarTrainingManager.bestialActive + " turns)");
      }
      if (FamiliarTrainingManager.blackTongueActive > 0) {
        text.append(
            " Black Tongue (+5 for " + FamiliarTrainingManager.blackTongueActive + " turns)");
      }
      if (FamiliarTrainingManager.greenGlowActive > 0) {
        text.append(
            " Healthy Green Glow (+10 for " + FamiliarTrainingManager.greenGlowActive + " turns)");
      }
      if (FamiliarTrainingManager.greenHeartActive > 0) {
        text.append(
            " Heart of Green (+3 for " + FamiliarTrainingManager.greenHeartActive + " turns)");
      }
      if (FamiliarTrainingManager.greenTongueActive > 0) {
        text.append(
            " Green Tongue (+5 for " + FamiliarTrainingManager.greenTongueActive + " turns)");
      }
      if (FamiliarTrainingManager.heavyPettingActive > 0) {
        text.append(
            " Heavy Petting (+5 for " + FamiliarTrainingManager.heavyPettingActive + " turns)");
      }
      if (FamiliarTrainingManager.worstEnemyActive > 0) {
        text.append(
            " Man's Worst Enemy (+5 for " + FamiliarTrainingManager.worstEnemyActive + " turns)");
      }

      if (!FamiliarTrainingManager.sympathyAvailable
          && FamiliarTrainingManager.empathyActive == 0
          && FamiliarTrainingManager.leashActive == 0
          && FamiliarTrainingManager.bestialActive == 0
          && FamiliarTrainingManager.blackTongueActive == 0
          && FamiliarTrainingManager.greenGlowActive == 0
          && FamiliarTrainingManager.greenHeartActive == 0
          && FamiliarTrainingManager.greenTongueActive == 0
          && FamiliarTrainingManager.heavyPettingActive == 0
          && FamiliarTrainingManager.worstEnemyActive == 0) {
        text.append(" None");
      }

      text.append("<br>");

      return text.toString();
    }

    public String printCurrentEquipment() {
      StringBuilder text = new StringBuilder();

      text.append("Current equipment:");

      if (this.weapon == FamiliarTrainingManager.BAR_WHIP) {
        text.append(" bar whip (+2)");
      }
      if (this.offhand == FamiliarTrainingManager.BAR_WHIP) {
        text.append(" bar whip (+2)");
      }

      if (this.hat == FamiliarTrainingManager.CRUMPLED_FEDORA) {
        text.append(" crumpled felt fedora (+10)");
      }

      if (this.hat == FamiliarTrainingManager.PITH_HELMET) {
        text.append(" plexiglass pith helmet (+5)");
      }

      if (this.item == FamiliarData.DOPPELGANGER) {
        text.append(" " + FamiliarData.DOPPELGANGER.getName() + " (+0)");
      } else if (this.item == FamiliarData.PUMPKIN_BUCKET) {
        text.append(" " + FamiliarData.PUMPKIN_BUCKET.getName() + " (+5)");
      } else if (this.item == FamiliarData.FLOWER_BOUQUET) {
        text.append(" " + FamiliarData.FLOWER_BOUQUET.getName() + " (+5)");
      } else if (this.item == FamiliarData.FIREWORKS) {
        text.append(" " + FamiliarData.FIREWORKS.getName() + " (+5)");
      } else if (this.item == FamiliarData.PET_SWEATER) {
        text.append(" " + FamiliarData.PET_SWEATER.getName() + " (+10)");
      } else if (this.item == FamiliarData.SUGAR_SHIELD) {
        text.append(" " + FamiliarData.SUGAR_SHIELD.getName() + " (+10)");
      } else if (this.item == FamiliarData.LEAD_NECKLACE) {
        text.append(" " + FamiliarData.LEAD_NECKLACE.getName() + " (+3)");
      } else if (this.item == FamiliarData.RAT_HEAD_BALLOON) {
        text.append(" " + FamiliarData.RAT_HEAD_BALLOON.getName() + " (-3)");
      } else if (this.item == FamiliarData.BATHYSPHERE) {
        text.append(" " + FamiliarData.BATHYSPHERE.getName() + " (-20)");
      } else if (this.item == FamiliarData.DAS_BOOT) {
        text.append(" " + FamiliarData.DAS_BOOT.getName() + " (-10)");
      } else if (this.item != null) {
        text.append(" " + this.specItem.getName() + " (+" + this.specWeight + ")");
      }

      for (int i = 0; i < 3; ++i) {
        if (this.acc[i] != null) {
          text.append(" " + this.acc[i].getName() + " (+1)");
        }
      }
      text.append("<br>");

      return text.toString();
    }

    public String printAvailableEquipment() {
      StringBuilder text = new StringBuilder();

      text.append("Available equipment:");

      text.append(" bar whip (+2)".repeat(Math.max(0, this.whipCount)));

      if (this.hat == FamiliarTrainingManager.CRUMPLED_FEDORA) {
        text.append(" crumpled felt fedora (+10)");
      }

      if (this.hat == FamiliarTrainingManager.PITH_HELMET) {
        text.append(" plexiglass pith helmet (+5)");
      }

      if (this.doppelganger) {
        text.append(" flaming familiar doppelg&auml;nger (+0)");
      } else {
        if (this.crumpledFedora) {
          text.append(" crumpled felt fedora (+10)");
        }
        if (this.pithHelmet) {
          text.append(" plexiglass pith helmet (+5)");
        }
        if (this.specItem != null) {
          text.append(" " + this.specItem.getName() + " (+" + this.specWeight + ")");
        }
        if (this.petSweater) {
          text.append(" astral pet sweater (+10)");
        }
        if (this.sugarShield) {
          text.append(" sugar shield (+10)");
        }
        if (this.pumpkinBucket) {
          text.append(" plastic pumpkin bucket (+5)");
        }
        if (this.flowerBouquet) {
          text.append(" mayflower bouquet (+5)");
        }
        if (this.boxFireworks) {
          text.append(" little box of fireworks (+5)");
        }
        if (this.leadNecklace) {
          text.append(" lead necklace (+3)");
        }
        if (this.ratHeadBalloon) {
          text.append(" rat head balloon (-3)");
        }
        if (this.bathysphere) {
          text.append(" little bitty bathysphere (-20)");
        }
        if (this.dasBoot) {
          text.append(" das boot (-10)");
        }
      }

      for (int i = 0; i < this.tpCount; ++i) {
        text.append(" " + this.tp[i].getName() + " (+1)");
      }

      text.append("<br>");

      return text.toString();
    }

    private class GearSet implements Comparable<GearSet> {
      public AdventureResult weapon;
      public AdventureResult offhand;
      public AdventureResult hat;
      public AdventureResult item;
      public AdventureResult acc1;
      public AdventureResult acc2;
      public AdventureResult acc3;

      public GearSet() {
        this(
            FamiliarStatus.this.weapon,
            FamiliarStatus.this.offhand,
            FamiliarStatus.this.acc[0],
            FamiliarStatus.this.acc[1],
            FamiliarStatus.this.acc[2],
            FamiliarStatus.this.item,
            FamiliarStatus.this.hat);
      }

      public GearSet(
          final AdventureResult weapon,
          final AdventureResult offhand,
          final AdventureResult acc1,
          final AdventureResult acc2,
          final AdventureResult acc3,
          final AdventureResult item,
          final AdventureResult hat) {
        this.weapon = weapon;
        this.offhand = offhand;
        this.acc1 = acc1;
        this.acc2 = acc2;
        this.acc3 = acc3;
        this.item = item;
        this.hat = hat;
      }

      public int weight() {
        return FamiliarStatus.this.gearSetWeight(
            this.weapon, this.offhand, this.acc1, this.acc2, this.acc3, this.item, this.hat);
      }

      @Override
      public int compareTo(final GearSet o) {
        // Keep in mind that all unequips are considered
        // better than equips, so unequips have a change
        // weight of 1.	 All others vary from that.

        GearSet that = o;
        int changes = 0;

        // Crumpled felt fedora is considered the
        // most ideal change, if it exists.

        if (this.hat != that.hat) {
          changes += that.item == null ? 1 : 10;
        }

        // Pumpkin bucket is also ideal, lead necklace
        // is less than ideal, standard item is ideal.

        if (this.item != that.item) {
          changes +=
              that.item == null
                  ? 1
                  : that.item.getItemId() == FamiliarData.LEAD_NECKLACE.getItemId() ? 10 : 5;
        }

        if (this.weapon != that.weapon) {
          changes += 15;
        }

        if (this.offhand != that.offhand) {
          changes += 10;
        }

        // Tiny plastic accessory changes are expensive
        // because they involve frequent changes.

        if (this.acc1 != that.acc1) {
          changes += 20;
        }
        if (this.acc2 != that.acc2) {
          changes += 20;
        }
        if (this.acc3 != that.acc3) {
          changes += 20;
        }

        return changes;
      }

      @Override
      public String toString() {
        StringBuilder text = new StringBuilder();
        text.append("(");
        if (this.weapon == null) {
          text.append("null");
        } else {
          text.append(this.weapon.getItemId());
        }
        text.append(", ");
        if (this.offhand == null) {
          text.append("null");
        } else {
          text.append(this.offhand.getItemId());
        }
        text.append(", ");
        if (this.hat == null) {
          text.append("null");
        } else {
          text.append(this.hat.getItemId());
        }
        text.append(", ");
        if (this.item == null) {
          text.append("null");
        } else {
          text.append(this.item.getItemId());
        }
        text.append(", ");
        if (this.acc1 == null) {
          text.append("null");
        } else {
          text.append(this.acc1.getItemId());
        }
        text.append(", ");
        if (this.acc2 == null) {
          text.append("null");
        } else {
          text.append(this.acc2.getItemId());
        }
        text.append(", ");
        if (this.acc3 == null) {
          text.append("null");
        } else {
          text.append(this.acc3.getItemId());
        }

        text.append(")");
        return text.toString();
      }
    }
  }

  /**
   * An internal class used to handle requests which resets a property for the duration of the
   * current session.
   */
}
