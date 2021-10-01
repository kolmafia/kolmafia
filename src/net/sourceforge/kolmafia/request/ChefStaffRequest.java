package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChefStaffRequest extends CreateItemRequest {
  private static final Pattern WHICH_PATTERN = Pattern.compile("whichstaff=(\\d+)");

  public ChefStaffRequest(final Concoction conc) {
    super("guild.php", conc);

    this.addFormField("action", "makestaff");

    // The first ingredient is the staff we will upgrade
    AdventureResult[] ingredients = conc.getIngredients();
    AdventureResult staff = ingredients[0];

    this.addFormField("whichstaff", String.valueOf(staff.getItemId()));
  }

  @Override
  public void run() {
    // Attempting to make the ingredients will pull the
    // needed items from the closet if they are missing.

    if (this.makeIngredients()) {
      super.run();
    }
  }

  @Override
  public void processResults() {
    // Since we create one at a time, override processResults so
    // superclass method doesn't undo ingredient usage.

    if (ChefStaffRequest.parseCreation(this.getURLString(), this.responseText)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You're missing some ingredients.");
    }
  }

  public static final boolean parseCreation(final String urlString, final String responseText) {
    if (responseText.indexOf("You don't have all of the items I'll need to make that Chefstaff.")
        != -1) {
      return true;
    }

    AdventureResult[] ingredients = ChefStaffRequest.staffIngredients(urlString);
    if (ingredients == null) {
      return false;
    }

    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult ingredient = ingredients[i];
      ResultProcessor.processResult(ingredient.getInstance(-1 * ingredient.getCount()));
    }

    return false;
  }

  private static AdventureResult[] staffIngredients(final String urlString) {
    Matcher itemMatcher = ChefStaffRequest.WHICH_PATTERN.matcher(urlString);
    if (!itemMatcher.find()) {
      return null;
    }

    // Item ID of the base staff
    int baseId = StringUtilities.parseInt(itemMatcher.group(1));

    // Find chefstaff recipe
    Concoction concoction = ConcoctionDatabase.chefStaffCreation(baseId);
    return concoction == null ? null : concoction.getIngredients();
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("guild.php") || urlString.indexOf("action=makestaff") == -1) {
      return false;
    }

    AdventureResult[] ingredients = ChefStaffRequest.staffIngredients(urlString);
    if (ingredients == null) {
      return true;
    }

    StringBuffer chefstaffString = new StringBuffer();
    chefstaffString.append("Chefstaff ");

    for (int i = 0; i < ingredients.length; ++i) {
      if (i > 0) {
        chefstaffString.append(", ");
      }

      chefstaffString.append(ingredients[i].getCount());
      chefstaffString.append(" ");
      chefstaffString.append(ingredients[i].getName());
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(chefstaffString.toString());

    return true;
  }
}
