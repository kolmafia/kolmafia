package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SuburbanDisRequest extends GenericRequest {
  private static final Pattern STONE1_PATTERN = Pattern.compile("stone1=(\\d+)");
  private static final Pattern STONE2_PATTERN = Pattern.compile("stone2=(\\d+)");
  public static final AdventureResult FOLIO = ItemPool.get(ItemPool.DEVILISH_FOLIO, 1);

  private final String action;

  public SuburbanDisRequest(String action) {
    super("suburbandis.php");
    this.action = action;
    this.addFormField("action", action);
  }

  public SuburbanDisRequest() {
    this("altar");
  }

  public SuburbanDisRequest(int stone1, int stone2) {
    this("stoned");
    this.addFormField("stone1", String.valueOf(stone1));
    this.addFormField("stone2", String.valueOf(stone2));
  }

  @Override
  public int getAdventuresUsed() {
    return this.action.equals("altar") ? 1 : 0;
  }

  @Override
  public void processResults() {
    SuburbanDisRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static final void parseResponse(final String location, final String responseText) {
    if (location.equals("suburbandis.php")) {
      return;
    }

    String action = GenericRequest.getAction(location);
    if (action == null) {
      return;
    }

    if (action.equals("altar")) {
      if (responseText.indexOf("You place your six stones in the holes on the altar") != -1) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.FURIOUS_STONE, -1));
        ResultProcessor.processResult(ItemPool.get(ItemPool.VANITY_STONE, -1));
        ResultProcessor.processResult(ItemPool.get(ItemPool.LECHEROUS_STONE, -1));
        ResultProcessor.processResult(ItemPool.get(ItemPool.JEALOUSY_STONE, -1));
        ResultProcessor.processResult(ItemPool.get(ItemPool.AVARICE_STONE, -1));
        ResultProcessor.processResult(ItemPool.get(ItemPool.GLUTTONOUS_STONE, -1));
      }
      return;
    }

    if (action.equals("stoned")) {
      // Look for success.
      if (responseText.indexOf("You acquire an effect") == -1) {
        return;
      }

      Matcher matcher = SuburbanDisRequest.STONE1_PATTERN.matcher(location);
      if (matcher.find()) {
        int stone1 = StringUtilities.parseInt(matcher.group(1));
        ResultProcessor.processResult(ItemPool.get(stone1, -1));
      }

      matcher = SuburbanDisRequest.STONE2_PATTERN.matcher(location);
      if (matcher.find()) {
        int stone2 = StringUtilities.parseInt(matcher.group(1));
        ResultProcessor.processResult(ItemPool.get(stone2, -1));
      }

      return;
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("suburbandis.php")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      return false;
    }

    String message;
    if (action.equals("altar")) {
      // Visiting the altar
      return true;
    }

    if (action.equals("dothis")) {
      message = "[" + KoLAdventure.getAdventureCount() + "] An Altar in The Suburbs of Dis";
    } else if (action.equals("stoned")) {
      Matcher matcher = SuburbanDisRequest.STONE1_PATTERN.matcher(urlString);
      if (!matcher.find()) {
        return true;
      }

      int stone1 = StringUtilities.parseInt(matcher.group(1));
      matcher = SuburbanDisRequest.STONE2_PATTERN.matcher(urlString);
      if (!matcher.find()) {
        return true;
      }

      int stone2 = StringUtilities.parseInt(matcher.group(1));
      message =
          "Placing "
              + ItemDatabase.getItemName(stone1)
              + " and "
              + ItemDatabase.getItemName(stone2)
              + " into altar.";
    } else {
      return false;
    }

    RequestLogger.printLine("");
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
