package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.listener.ItemListenerRegistry;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.persistence.ItemFinder;

public class ItemTraceCommand extends AbstractCommand {
  private static ArrayList<Listener> audience = null; // keeps listeners from being GC'd

  public ItemTraceCommand() {
    this.usage = " <item> [, <item>]... - watch changes to inventory count of items";
  }

  @Override
  public synchronized void run(String command, final String parameters) {
    if (audience != null) {
      audience = null;
      RequestLogger.printLine("Previously watched items have been cleared.");
    }

    if (parameters.equals("")) {
      return;
    }

    AdventureResult[] items = ItemFinder.getMatchingItemList(parameters, KoLConstants.inventory);
    audience = new ArrayList<Listener>();
    for (AdventureResult item : items) {
      audience.add(new ItemListener(item));
    }
  }

  private static class ItemListener implements Listener {
    AdventureResult item;

    public ItemListener(AdventureResult item) {
      this.item = item;
      ItemListenerRegistry.registerItemListener(item.getItemId(), this);
      this.update();
    }

    @Override
    public void update() {
      String msg = "itrace: " + this.item.getName() + " = " + item.getCount(KoLConstants.inventory);
      RequestLogger.updateSessionLog(msg);
      if (RequestLogger.isDebugging()) {
        StaticEntity.printStackTrace(msg);
        // msg also gets displayed in CLI
      } else {
        RequestLogger.printLine(msg);
      }
    }
  }
}
