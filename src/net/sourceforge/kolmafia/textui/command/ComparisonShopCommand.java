package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.ZapRequest;
import net.sourceforge.kolmafia.session.StoreManager;

public class ComparisonShopCommand extends AbstractCommand implements Comparator<AdventureResult> {
  public ComparisonShopCommand() {
    this.usage =
        "[?] [+]<item> [,[-]item]... [; <cmds>] - compare prices, do cmds with \"it\" replaced with best.";
    this.flags = KoLmafiaCLI.FULL_LINE_CMD;
  }

  @Override
  public void run(final String cmd, String parameters) {
    boolean expensive = cmd.equals("expensive");
    String commands = null;
    int pos = parameters.indexOf(";");
    if (pos != -1) {
      commands = parameters.substring(pos + 1).trim();
      parameters = parameters.substring(0, pos).trim();
    }
    String[] pieces = parameters.split("\\s*,\\s*");
    TreeSet<String> names = new TreeSet<>();
    for (int i = 0; i < pieces.length; ++i) {
      String piece = pieces[i];
      if (piece.startsWith("+")) {
        AdventureResult item = ItemFinder.getFirstMatchingItem(piece.substring(1).trim());
        if (item == null) {
          return;
        }
        names.addAll(ZapRequest.getZapGroup(item.getItemId()));
      } else if (piece.startsWith("-")) {
        names.removeAll(ItemDatabase.getMatchingNames(piece.substring(1).trim()));
      } else {
        names.addAll(ItemDatabase.getMatchingNames(piece));
      }
    }
    if (names.size() == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "No matching items!");
      return;
    }
    if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
      RequestLogger.printList(Arrays.asList(names.toArray()));
      return;
    }

    List<AdventureResult> results = new ArrayList<>();

    for (String name : names) {
      int itemId = ItemDatabase.getItemId(name);
      AdventureResult item = ItemPool.get(itemId);
      if (!ItemDatabase.isTradeable(itemId) || StoreManager.getMallPrice(item) <= 0) {
        continue;
      }
      if (!KoLmafia.permitsContinue()) {
        return;
      }
      results.add(item);
    }
    if (results.size() == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "No tradeable items!");
      return;
    }
    results.sort(this);
    if (expensive) {
      Collections.reverse(results);
    }
    if (commands != null) {
      this.CLI.executeLine(commands.replaceAll("\\bit\\b", results.get(0).getName()));
      return;
    }

    for (AdventureResult item : results) {
      RequestLogger.printLine(
          item.getName()
              + " @ "
              + KoLConstants.COMMA_FORMAT.format(StoreManager.getMallPrice(item)));
    }
  }

  @Override
  public int compare(final AdventureResult o1, final AdventureResult o2) {
    return StoreManager.getMallPrice(o1) - StoreManager.getMallPrice(o2);
  }
}
