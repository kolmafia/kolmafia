package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

enum Skeleton {
  WARRIOR("warrior", 1),
  CLERIC("cleric", 2),
  WIZARD("wizard", 3),
  ROGUE("rogue", 4),
  BUDDY("buddy", 5);

  private final String name;
  private final int num;

  Skeleton(String name, int num) {
    this.name = name;
    this.num = num;
  }

  public String getName() {
    return name;
  }

  public int getNum() {
    return num;
  }
}

public class SkeletonCommand extends AbstractCommand {
  public SkeletonCommand() {
    this.usage = " warrior | cleric | wizard | rogue | buddy";
  }

  public static final int findSkeleton(final String name) {
    for (Skeleton skeleton : Skeleton.values()) {
      if (name.equals(skeleton.getName())) {
        return skeleton.getNum();
      }
    }

    return 0;
  }

  @Override
  public void run(final String cmd, String parameters) {
    parameters = parameters.trim();
    if (parameters.equals("")) {
      RequestLogger.printLine("Usage: skeleton" + this.usage);
      RequestLogger.printLine("warrior: damage, delevel");
      RequestLogger.printLine("cleric: hot damage, hp");
      RequestLogger.printLine("wizard: cold damage, mp");
      RequestLogger.printLine("rogue: damage, meat");
      RequestLogger.printLine("buddy: delevel, exp");
      return;
    }

    int option;
    option = SkeletonCommand.findSkeleton(parameters);
    if (option == 0) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what a '" + parameters + "' skeleton is.");
      return;
    }

    if (!InventoryManager.retrieveItem(ItemPool.SKELETON)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You have no skeletons and can't get any with your current settings.");
      return;
    }

    GenericRequest request =
        new GenericRequest("inv_use.php?which=3&whichitem=" + ItemPool.SKELETON);
    RequestThread.postRequest(request);
    request.constructURLString("choice.php?whichchoice=603&option=" + option);
    RequestThread.postRequest(request);
  }
}
