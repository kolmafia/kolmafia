package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.HashingViseRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class ViseCommand extends AbstractCommand {

  public ViseCommand() {
    this.usage =
        " [count] <item> [, <another>]... - use your hashing vise to smash schematics into bits.";
  }

  static final Pattern ITEM_PATTERN = Pattern.compile("((\\d+)\\s+)?(.*)");

  @Override
  public void run(final String cmd, final String parameters) {
    if (InventoryManager.getAccessibleCount(HashingViseRequest.HASHING_VISE) < 1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have an available hashing vise.");
      return;
    }

    String[] itemNames = parameters.split("\\s*,\\s*");
    List<AdventureResult> items = new ArrayList<>();

    for (String itemName : itemNames) {
      if (itemName.equals("")) {
        continue;
      }

      Matcher matcher = ITEM_PATTERN.matcher(itemName);
      if (!matcher.find()) {
        RequestLogger.printLine("Usage: vise" + this.usage);
        return;
      }

      int count = (matcher.group(1) != null) ? Integer.valueOf(matcher.group(2)) : 1;
      String substring = matcher.group(3);
      List<String> matches = HashingViseRequest.getMatchingNames(substring);
      if (matches.size() == 0) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "'" + substring + "' matches no schematics");
        return;
      }

      if (matches.size() > 1) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "'" + substring + "' matches " + matches.size() + " schematics");
        return;
      }

      AdventureResult item = new AdventureResult(matches.get(0), count, false);
      items.add(item);
    }

    if (items.size() == 0) {
      RequestLogger.printLine("Usage: vise" + this.usage);
      return;
    }

    for (AdventureResult item : items) {
      if (item.getCount() >= 1) {
        RequestThread.postRequest(new HashingViseRequest(item));
      }
    }
  }
}
