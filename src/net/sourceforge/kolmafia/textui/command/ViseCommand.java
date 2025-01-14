package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.HashingViseRequest;

public class ViseCommand extends AbstractCommand {

  public ViseCommand() {
    this.usage = " usage";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    List<AdventureResult> schematics = HashingViseRequest.schematics;
    AdventureResult[] items = ItemFinder.getMatchingItemList(parameters, schematics);
    if (items.length == 0) {
      return;
    }

    // RequestThread.postRequest(new HashingViceRequest(items));
  }
}
