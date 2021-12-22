package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class StorageCommand extends AbstractCommand {
  public StorageCommand() {
    this.usage = " all | outfit <name> | <item> [, <item>]... - pull items from Hagnk's storage.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    boolean inHardcore = KoLCharacter.isHardcore();

    if (parameters.trim().equals("all")) {
      if (inHardcore) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You cannot empty storage when you are in Hardcore.");
      } else if (!KoLCharacter.canInteract()) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You cannot pull everything while your pulls are limited.");
      } else {
        RequestThread.postRequest(new StorageRequest(StorageRequest.EMPTY_STORAGE));
      }
      return;
    }

    AdventureResult[] items;

    if (parameters.startsWith("outfit ")) {
      if (inHardcore) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You cannot pull things from storage when you are in Hardcore.");
        return;
      }
      String name = parameters.substring(7).trim();
      SpecialOutfit outfit = EquipmentManager.getMatchingOutfit(name);
      if (outfit == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "No such outfit.");
        return;
      }

      List<AdventureResult> have = new ArrayList<>();
      List<AdventureResult> missing = new ArrayList<>();
      List<AdventureResult> needed = new ArrayList<>();

      AdventureResult[] pieces = outfit.getPieces();
      for (int i = 0; i < pieces.length; ++i) {
        AdventureResult piece = pieces[i];

        // Count of item from all "autoSatisfy" source
        int availableCount = InventoryManager.getAccessibleCount(piece);

        // Count of item in storage
        int storageCount = piece.getCount(KoLConstants.storage);

        if (InventoryManager.canUseStorage()) {
          // Don't double-count items in storage
          availableCount -= storageCount;
        }

        if (availableCount > 0) {
          // Don't need to pull; it's in inventory or closet or equipped
          KoLmafia.updateDisplay(piece.getName() + " is available without pulling.");
          have.add(piece);
          continue;
        }

        if (storageCount == 0) {
          // None available outside of storage - and none in storage
          KoLmafia.updateDisplay(piece.getName() + " is not in storage.");
          missing.add(piece);
          continue;
        }

        needed.add(piece);
      }

      if (missing.size() > 0) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            "You are missing "
                + missing.size()
                + " pieces of outfit '"
                + outfit.getName()
                + "'; pull aborted.");
        return;
      }

      if (needed.size() == 0) {
        KoLmafia.updateDisplay(
            "All pieces of outfit '" + outfit.getName() + "' are available without pulling.");
        return;
      }

      if (have.size() > 0) {
        KoLmafia.updateDisplay(
            have.size()
                + " pieces of outfit '"
                + outfit.getName()
                + "' are available without pulling; the remaining "
                + needed.size()
                + " will be pulled.");
      }

      items = needed.toArray(new AdventureResult[0]);
    } else if (inHardcore) {
      items = ItemFinder.getMatchingItemList(parameters, KoLConstants.freepulls);
    } else {
      items = ItemFinder.getMatchingItemList(parameters, KoLConstants.storage);
    }

    if (items.length == 0) {
      return;
    }

    int meatAttachmentCount = 0;

    for (int i = 0; i < items.length; ++i) {
      AdventureResult item = items[i];
      if (item.getName().equals(AdventureResult.MEAT)) {
        if (!inHardcore) {
          RequestThread.postRequest(
              new StorageRequest(StorageRequest.PULL_MEAT_FROM_STORAGE, item.getCount()));
        }

        items[i] = null;
        ++meatAttachmentCount;
      }
    }

    if (meatAttachmentCount == items.length) {
      return;
    }

    // Double check to make sure you have all items on hand
    // since a failure to get something from Hagnk's is bad.

    for (int i = 0; i < items.length; ++i) {
      AdventureResult item = items[i];
      if (item == null) {
        continue;
      }

      String itemName = item.getName();
      int storageCount =
          item.getCount(KoLConstants.storage) + item.getCount(KoLConstants.freepulls);

      if (item.getCount() == 0) {
        KoLmafia.updateDisplay("You have " + storageCount + " " + itemName + " in storage.");
        items[i] = null;
      } else if (storageCount < item.getCount()) {
        KoLmafia.updateDisplay(
            "You only have "
                + storageCount
                + " "
                + itemName
                + " in storage (you wanted "
                + item.getCount()
                + ")");
        items[i] = item.getInstance(storageCount);
      }

      if (!StandardRequest.isAllowed("Items", itemName)) {
        KoLmafia.updateDisplay(itemName + " is not allowed right now.");
        items[i] = null;
      }
    }

    // Submit a StorageRequest if there is at least one item to pull.
    for (AdventureResult attachment : items) {
      if (attachment != null) {
        RequestThread.postRequest(new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, items));
        break;
      }
    }

    if (!inHardcore && !KoLCharacter.canInteract()) {
      int pulls = ConcoctionDatabase.getPullsRemaining();
      if (pulls >= 0 && KoLmafia.permitsContinue()) {
        KoLmafia.updateDisplay(
            pulls
                + (pulls == 1 ? " pull" : " pulls")
                + " remaining, "
                + ConcoctionDatabase.getPullsBudgeted()
                + " budgeted for automatic use.");
      }
    }
  }
}
