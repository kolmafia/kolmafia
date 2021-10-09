package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class QuarkCommand extends AbstractCommand implements Comparator {
  public QuarkCommand() {
    this.usage =
        "[?] [<itemList>...] - gain MP by pasting unstable quark with best item from itemList (or your junk list).";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (ItemPool.get(ItemPool.UNSTABLE_QUARK, 1).getCount(KoLConstants.inventory) < 1) {
      KoLmafia.updateDisplay("You have no unstable quarks.");
      return;
    }
    if (!KoLCharacter.knollAvailable() || KoLCharacter.inZombiecore()) {
      AdventureResult paste = ItemPool.get(ItemPool.MEAT_PASTE, 1);

      if (!InventoryManager.retrieveItem(paste)) {
        KoLmafia.updateDisplay("Can't afford gluons.");
        return;
      }
    }

    List items = KoLConstants.junkList;
    if (!parameters.equals("")) {
      items = Arrays.asList(ItemFinder.getMatchingItemList(parameters, KoLConstants.inventory));
      if (items.size() == 0) {
        return;
      }
    }

    ArrayList usables = new ArrayList();
    Iterator i = items.iterator();
    while (i.hasNext()) {
      AdventureResult item = (AdventureResult) i.next();
      if (item.getCount(KoLConstants.inventory)
          < (KoLConstants.singletonList.contains(item) ? 2 : 1)) {
        continue;
      }
      int price = ItemDatabase.getPriceById(item.getItemId());
      if (price < 20 || KoLCharacter.getCurrentMP() + price > KoLCharacter.getMaximumMP()) {
        continue;
      }
      if (this.isPasteable(item)) {
        usables.add(item.getInstance(price));
      }
    }
    if (usables.size() == 0) {
      KoLmafia.updateDisplay("No suitable quark-pasteable items found.");
      return;
    }

    Collections.sort(usables, this);
    if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
      RequestLogger.printLine(usables.get(0).toString());
      return;
    }

    AdventureResult item = (AdventureResult) usables.get(0);
    RequestLogger.printLine("Pasting unstable quark with " + item);
    GenericRequest visitor =
        new GenericRequest(
            "craft.php?action=craft&mode=combine&ajax=1&pwd&qty=1&a=3743&b=" + item.getItemId());
    RequestThread.postRequest(visitor);
  }

  private boolean isPasteable(final AdventureResult item) {
    for (AdventureResult use : ConcoctionDatabase.getKnownUses(item)) {
      CraftingType mixMethod = ConcoctionDatabase.getMixingMethod(use.getItemId());
      // Should CraftingType.ACOMBINE be included?
      if (mixMethod == CraftingType.COMBINE || mixMethod == CraftingType.JEWELRY) {
        return true;
      }
    }
    return false;
  }

  public int compare(final Object o1, final Object o2) {
    return ((AdventureResult) o2).getCount() - ((AdventureResult) o1).getCount();
  }
}
