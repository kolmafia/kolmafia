package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class GoalManager {
  public static final AdventureResult GOAL_CHOICE = new AdventureResult(AdventureResult.CHOICE, 1);
  public static final AdventureResult GOAL_AUTOSTOP =
      new AdventureResult(AdventureResult.AUTOSTOP, 1);
  public static final AdventureResult GOAL_FACTOID =
      new AdventureResult(AdventureResult.FACTOID, 1);
  public static final AdventureResult GOAL_FLOUNDRY =
      new AdventureResult(AdventureResult.FLOUNDRY, 1);

  public static final int[] GOAL_SUBSTATS_COUNTS = new int[3];
  public static final AdventureResult GOAL_SUBSTATS =
      new AdventureResult.AdventureMultiResult(AdventureResult.SUBSTATS, GOAL_SUBSTATS_COUNTS);

  private static final LockableListModel<AdventureResult> goals =
      new LockableListModel<AdventureResult>();

  private GoalManager() {}

  public static final LockableListModel<AdventureResult> getGoals() {
    return GoalManager.goals;
  }

  public static final String getGoalString() {
    StringBuilder conditionString = new StringBuilder();

    for (int i = 0; i < GoalManager.goals.size(); ++i) {
      if (i > 0) {
        conditionString.append(", ");
      }

      AdventureResult goal = GoalManager.goals.get(i);
      conditionString.append(goal.toConditionString());
    }

    return conditionString.toString();
  }

  public static final void clearGoals() {
    GoalManager.goals.clear();

    for (int i = 0; i < 3; ++i) {
      GoalManager.GOAL_SUBSTATS_COUNTS[i] = 0;
    }
  }

  public static final boolean hasGoals() {
    return !GoalManager.goals.isEmpty();
  }

  public static final boolean hasItemGoal(int itemId) {
    return GoalManager.hasGoal(ItemPool.get(itemId, 1));
  }

  public static final boolean hasGoal(AdventureResult goal) {
    return GoalManager.goals.contains(goal);
  }

  public static final int getGoalCount(AdventureResult goal) {
    return goal.getCount(GoalManager.goals);
  }

  public static final void addItemGoal(int itemId, int count) {
    GoalManager.addGoal(ItemPool.get(itemId, count));
  }

  public static final void addGoal(AdventureResult goal) {
    String goalName = goal.getName();

    if (goalName.equals(AdventureResult.SUBSTATS)) {
      if (!GoalManager.goals.contains(goal)) {
        GoalManager.goals.add(goal);
      }
    } else {
      AdventureResult.addResultToList(GoalManager.goals, goal);
    }

    if (goal.getCount() > 0) {
      RequestLogger.printLine("Condition added: " + goal.toConditionString());
    } else {
      RequestLogger.printLine("Condition removed: " + goal.getNegation());
    }
  }

  public static final void setItemGoal(int itemId, int count) {
    GoalManager.setGoal(ItemPool.get(itemId, count));
  }

  public static final void setGoal(AdventureResult goal) {
    String goalName = goal.getName();

    if (goalName.equals(AdventureResult.SUBSTATS)) {
      if (goal.getCount() == 0) {
        GoalManager.goals.remove(goal);
      } else if (!GoalManager.goals.contains(goal)) {
        GoalManager.addGoal(goal);
      }

      return;
    }

    int currentGoalCount = goal.getCount(GoalManager.goals);
    int desiredGoalCount = goal.getCount();

    if (currentGoalCount >= desiredGoalCount) {
      RequestLogger.printLine("Condition already exists: " + goal);
      return;
    }

    int currentCount = 0;

    if (goal.isItem()) {
      currentCount = goal.getCount(KoLConstants.inventory);

      if (InventoryManager.canUseCloset()) {
        currentCount += goal.getCount(KoLConstants.closet);
      }

      for (int j = 0; j < EquipmentManager.FAMILIAR; ++j) {
        if (EquipmentManager.getEquipment(j).equals(goal)) {
          ++currentCount;
        }
      }
    }

    if (currentCount >= desiredGoalCount) {
      RequestLogger.printLine("Condition already met: " + goal);
      return;
    }

    goal = goal.getInstance(desiredGoalCount - currentCount);
    GoalManager.addGoal(goal);
  }

  public static final void makeSideTrip(KoLAdventure location, AdventureResult goal) {
    List<AdventureResult> previousGoals = new ArrayList<>(GoalManager.goals);

    GoalManager.clearGoals();
    GoalManager.addGoal(goal);

    KoLmafia.makeRequest(location, KoLCharacter.getAdventuresLeft());

    if (!GoalManager.goals.isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.ABORT, "Unable to obtain " + goal);
      GoalManager.clearGoals();
    }

    GoalManager.goals.addAll(previousGoals);
  }

  public static final void checkAutoStop(String message) {
    GoalManager.updateProgress(GoalManager.GOAL_AUTOSTOP);

    if (!KoLmafia.isAdventuring()) {
      return;
    }

    if (message.equals("Under the Knife")
        && Preferences.getString("choiceAdventure21").equals("2")) {
      return;
    } else if (GoalManager.goals.isEmpty()) {
      KoLmafia.updateDisplay(MafiaState.PENDING, message);
    } else {
      RequestLogger.printLine(message);
      RequestLogger.printLine("There are still unsatisfied conditions.");
    }
  }

  public static final void updateProgress(AdventureResult goal) {
    int goalIndex = GoalManager.goals.indexOf(goal);

    if (goalIndex == -1) {
      return;
    }

    String goalName = goal.getName();

    if (goalName.equals(AdventureResult.SUBSTATS)) {
      // If the condition is a substat condition, then zero out the
      // appropriate count and remove if all counts dropped to zero.

      for (int i = 0; i < 3; ++i) {
        if (GoalManager.GOAL_SUBSTATS_COUNTS[i] == 0) {
          continue;
        }

        GoalManager.GOAL_SUBSTATS_COUNTS[i] =
            Math.max(0, GoalManager.GOAL_SUBSTATS_COUNTS[i] - goal.getCount(i));
      }

      if (GoalManager.GOAL_SUBSTATS.getCount() == 0) {
        GoalManager.goals.remove(goalIndex);
      } else {
        GoalManager.goals.fireContentsChanged(GoalManager.goals, goalIndex, goalIndex);
      }

      return;
    }

    AdventureResult previousGoal = GoalManager.goals.get(goalIndex);

    if (previousGoal.getCount() <= goal.getCount()) {
      GoalManager.goals.remove(goalIndex);
    } else {
      goal = previousGoal.getInstance(previousGoal.getCount() - goal.getCount());
      GoalManager.goals.set(goalIndex, goal);
    }
  }
}
