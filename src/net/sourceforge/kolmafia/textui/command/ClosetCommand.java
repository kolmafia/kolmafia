package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.ClosetRequest;

public class ClosetCommand extends AbstractCommand {
  public ClosetCommand() {
    this.usage =
        " list <filter> | empty | put <item>... | take <item>... - list or manipulate your closet.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (parameters.startsWith("list")) {
      ShowDataCommand.show("closet " + parameters.substring(4).trim());
      return;
    } else if (parameters.length() == 0) {
      ShowDataCommand.show("closet");
      return;
    }

    if (parameters.length() <= 4) {
      RequestLogger.printList(KoLConstants.closet);
      return;
    }

    if (parameters.equals("empty")) {
      RequestThread.postRequest(new ClosetRequest(ClosetRequest.EMPTY_CLOSET));
      return;
    }

    boolean isTake;
    if (parameters.startsWith("take")) {
      parameters = parameters.substring(4).trim();
      isTake = true;
    } else if (parameters.startsWith("put")) {
      parameters = parameters.substring(3).trim();
      isTake = false;
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Invalid closet command.");
      return;
    }

    List<AdventureResult> source = isTake ? KoLConstants.closet : KoLConstants.inventory;
    AdventureResult[] itemList = ItemFinder.getMatchingItemList(parameters, source);

    if (itemList.length == 0) {
      return;
    }

    var split = Arrays.stream(itemList).collect(Collectors.partitioningBy(AdventureResult::isMeat));
    var meat = split.get(true);
    var items = split.get(false);

    if (meat.size() > 0) {
      long meatCount =
          meat.stream().map(AdventureResult::getLongCount).mapToLong(Long::longValue).sum();
      if (meatCount > 0) {
        int moveType = isTake ? ClosetRequest.MEAT_TO_INVENTORY : ClosetRequest.MEAT_TO_CLOSET;
        RequestThread.postRequest(new ClosetRequest(moveType, meatCount));
      }
    }

    if (items.stream().allMatch(x -> x.getCount() <= 0)) {
      return;
    }

    int moveType = isTake ? ClosetRequest.CLOSET_TO_INVENTORY : ClosetRequest.INVENTORY_TO_CLOSET;
    RequestThread.postRequest(new ClosetRequest(moveType, itemList));

    // update "Hatter" daily deed
    if (items.stream().anyMatch(EquipmentDatabase::isHat)) {
      PreferenceListenerRegistry.firePreferenceChanged("(hats)");
    }
  }
}
