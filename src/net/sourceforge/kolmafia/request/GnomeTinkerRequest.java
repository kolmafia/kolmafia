package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class GnomeTinkerRequest extends CreateItemRequest {
  private final AdventureResult[] ingredients;
  private static final Pattern ITEM_PATTERN = Pattern.compile("item[123]=(\\d+)");

  public GnomeTinkerRequest(final Concoction conc) {
    super("gnomes.php", conc);

    this.addFormField("place", "tinker");
    this.addFormField("action", "tinksomething");

    this.ingredients = conc.getIngredients();

    if (this.ingredients != null && this.ingredients.length == 3) {
      this.addFormField("item1", String.valueOf(this.ingredients[0].getItemId()));
      this.addFormField("item2", String.valueOf(this.ingredients[1].getItemId()));
      this.addFormField("item3", String.valueOf(this.ingredients[2].getItemId()));
    }
  }

  @Override
  public void run() {
    // If this doesn't contain a valid number of ingredients,
    // just return from the method call to avoid hitting on
    // the server as a result of a bad mixture in the database.

    if (this.ingredients == null || this.ingredients.length != 3) {
      return;
    }

    // Attempting to make the ingredients will pull the
    // needed items from the closet if they are missing.

    if (!this.makeIngredients()) {
      return;
    }

    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + this.getName() + "...");
    this.addFormField("qty", String.valueOf(this.getQuantityNeeded()));
    super.run();
  }

  @Override
  public void processResults() {
    // Since we create one at a time, override processResults so
    // superclass method doesn't undo ingredient usage.

    if (GnomeTinkerRequest.parseCreation(this.getURLString(), this.responseText)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't tinker those.");
    }
  }

  public static final boolean parseCreation(final String urlString, final String responseText) {
    // Gnorman deftly assembles your items into something new.
    // Gnorman deftly assembles your items into some new stuff.

    if (!responseText.contains("Gnorman deftly assembles your items")) {
      return true;
    }

    AdventureResult[] ingredients = CreateItemRequest.findIngredients(urlString);
    int quantity = CreateItemRequest.getQuantity(urlString, ingredients, 1);

    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult item = ingredients[i];
      ResultProcessor.processItem(item.getItemId(), -quantity);
    }

    return false;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("gnomes.php") || !urlString.contains("action=tinksomething")) {
      return false;
    }

    String line = CreateItemRequest.getCreationCommand("Tinker", urlString, 1, false);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(line);

    return true;
  }
}
