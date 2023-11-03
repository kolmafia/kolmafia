package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BurningLeavesRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LeavesCommand extends AbstractCommand {
  public LeavesCommand() {
    this.usage =
        "X | item | monster - burn X leaves, or enough to summon the requested item or monster.";
  }

  private static final Map<String, Integer> STRINGS_TO_LEAVES =
      Arrays.stream(BurningLeavesRequest.Outcome.values())
          .filter(o -> o.getLeaves() > 0)
          .map(
              o -> {
                String name;
                if (o.getItemId() > 0) {
                  name = ItemPool.get(o.getItemId()).getName();
                } else {
                  name = o.getMonsterName();
                }
                return Map.entry(name, o.getLeaves());
              })
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

  @Override
  public void run(final String cmd, String parameters) {
    if (!KoLConstants.campground.contains(ItemPool.get(ItemPool.A_GUIDE_TO_BURNING_LEAVES))) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "You must have a Pile of Burning Leaves to have a pile in which you can burn leaves.");
      return;
    }

    if (parameters.isBlank()) {
      BurningLeavesRequest.visit();
      return;
    }

    int leaves;

    if (StringUtilities.isNumeric(parameters)) {
      leaves = StringUtilities.parseInt(parameters);
    } else {
      var match = StringUtilities.getMatchingNames(STRINGS_TO_LEAVES.keySet(), parameters);

      if (match.size() != 1) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            "What is a " + parameters + "? Try a number of leaves, or a relevant item or monster.");
        return;
      }

      leaves = STRINGS_TO_LEAVES.get(match.get(0));
    }

    if (leaves <= 0) return;

    if (InventoryManager.getAccessibleCount(ItemPool.INFLAMMABLE_LEAF) < leaves) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have that many leaves.");
      return;
    }

    InventoryManager.retrieveItem(ItemPool.INFLAMMABLE_LEAF, leaves);

    var outcome = BurningLeavesRequest.Outcome.findByLeaves(leaves);

    if (outcome.getMonsterName() != null) {
      Preferences.setString("nextAdventure", "None");
      RecoveryManager.runBetweenBattleChecks(true);
    }

    BurningLeavesRequest.visit();
    RequestThread.postRequest(new BurningLeavesRequest(leaves));
  }
}
