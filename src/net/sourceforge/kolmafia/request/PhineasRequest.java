package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PhineasRequest extends CreateItemRequest {
  private static final Pattern WHICH_PATTERN = Pattern.compile("makewhich=(\\d+)");

  public PhineasRequest(final Concoction conc) {
    super("volcanoisland.php", conc);

    this.addFormField("action", "npc");
    this.addFormField("subaction", "make");
    this.addFormField("makewhich", String.valueOf(this.getItemId()));
  }

  @Override
  public void run() {
    // Attempting to make the ingredients will pull the
    // needed items from the closet if they are missing.
    // In this case, it will also create the needed white
    // pixels if they are not currently available.

    if (!this.makeIngredients()) {
      return;
    }

    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + this.getName() + "...");
    this.addFormField("quantity", String.valueOf(this.getQuantityNeeded()));
    super.run();
  }

  private static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "name=makewhich value=([\\d]+)[^>]*?>.*?descitem.([\\d]+)[^>]*>([^&]*)&nbsp;",
          Pattern.DOTALL);

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("volcanoisland.php") || urlString.indexOf("action=npc") == -1) {
      return;
    }

    // Learn new trade items by simply visiting Phineas
    Matcher matcher = ITEM_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int id = StringUtilities.parseInt(matcher.group(1));
      String desc = matcher.group(2);
      String name = matcher.group(3);
      String data = ItemDatabase.getItemDataName(id);
      if (data == null || !data.equals(name)) {
        ItemDatabase.registerItem(id, name, desc);
      }
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("volcanoisland.php")
        || urlString.indexOf("action=npc") == -1
        || urlString.indexOf("subaction=make") == -1) {
      return false;
    }

    Matcher itemMatcher = PhineasRequest.WHICH_PATTERN.matcher(urlString);
    if (!itemMatcher.find()) {
      return true;
    }

    int itemId = StringUtilities.parseInt(itemMatcher.group(1));
    int quantity = 1;

    if (urlString.indexOf("makemax=1") != -1) {
      quantity = CreateItemRequest.getInstance(itemId).getQuantityPossible();
    } else {
      Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher(urlString);
      if (quantityMatcher.find()) {
        String quantityString = quantityMatcher.group(1).trim();
        quantity = quantityString.length() == 0 ? 1 : StringUtilities.parseInt(quantityString);
      }
    }

    StringBuffer sealString = new StringBuffer();
    sealString.append("Trade ");

    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(itemId);
    for (int i = 0; i < ingredients.length; ++i) {
      if (i > 0) {
        sealString.append(", ");
      }

      sealString.append(ingredients[i].getCount() * quantity);
      sealString.append(" ");
      sealString.append(ingredients[i].getName());

      ResultProcessor.processResult(
          ingredients[i].getInstance(-1 * ingredients[i].getCount() * quantity));
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(sealString.toString());

    return true;
  }
}
