package net.sourceforge.kolmafia.request.coinmaster;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.request.GenericRequest;

public class FudgeWandRequest extends CoinMasterRequest {
  public static final String master = "Fudge Wand";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? fudgecule");
  public static final AdventureResult FUDGECULE = ItemPool.get(ItemPool.FUDGECULE, 1);
  public static final AdventureResult FUDGE_WAND = ItemPool.get(ItemPool.FUDGE_WAND, 1);
  private static final Pattern OPTION_PATTERN = Pattern.compile("option=(\\d+)");

  public static final CoinmasterData FUDGEWAND =
      new CoinmasterData(master, "fudge", FudgeWandRequest.class)
          .withToken("fudgecule")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(FUDGECULE)
          .withBuyURL("choice.php?whichchoice=562")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withEquip(FudgeWandRequest::equip)
          .withUnequip(FudgeWandRequest::unequip)
          .withAccessible(FudgeWandRequest::accessible);

  private static String lastURL = null;

  private record Option(String option, int id) {}

  private static final Option[] OPTIONS = {
    new Option("1", ItemPool.FUDGIE_ROLL),
    new Option("2", ItemPool.FUDGE_SPORK),
    new Option("3", ItemPool.FUDGE_CUBE),
    new Option("4", ItemPool.FUDGE_BUNNY),
    new Option("5", ItemPool.FUDGECYCLE),
  };

  private static String idToOption(final int id) {
    for (Option option : OPTIONS) {
      if (option.id == id) {
        return option.option;
      }
    }

    return null;
  }

  private static int optionToId(final String opt) {
    for (Option option : OPTIONS) {
      if (opt.equals(option.option)) {
        return option.id;
      }
    }

    return -1;
  }

  public FudgeWandRequest() {
    super(FUDGEWAND);
  }

  public FudgeWandRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FUDGEWAND, buying, attachments);
  }

  @Override
  public void setItem(final AdventureResult item) {
    int itemId = item.getItemId();
    String option = idToOption(itemId);
    this.addFormField("option", option);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    CoinmasterData data = FUDGEWAND;

    if (location.indexOf("option=6") != -1) {
      // We exited the choice adventure.
      return;
    }

    Matcher optionMatcher = OPTION_PATTERN.matcher(location);
    if (!optionMatcher.find()) {
      return;
    }

    String option = optionMatcher.group(1);
    int itemId = optionToId(option);

    if (itemId != -1) {
      CoinMasterRequest.completePurchase(data, itemId, 1, false);
    }

    CoinMasterRequest.parseBalance(data, responseText);
    ConcoctionDatabase.setRefreshNeeded(true);
  }

  public static final boolean registerRequest(final String urlString) {
    CoinmasterData data = FUDGEWAND;

    // using the wand is simply a visit
    // inv_use.php?whichitem=5441
    if (urlString.startsWith("inv_use.php") && urlString.contains("whichitem=5441")) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("Visiting " + data.getMaster());
      return true;
    }

    // choice.php?pwd&whichchoice=562&option=3
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=562")) {
      return false;
    }

    if (urlString.contains("option=6")) {
      // We exited the choice adventure.
      return true;
    }

    // Save URL.
    lastURL = urlString;

    Matcher optionMatcher = OPTION_PATTERN.matcher(urlString);
    if (!optionMatcher.find()) {
      return true;
    }

    String option = optionMatcher.group(1);
    int itemId = optionToId(option);

    if (itemId != -1) {
      CoinMasterRequest.buyStuff(data, itemId, 1, false);
    }

    return true;
  }

  public static String accessible() {
    int wand = FUDGE_WAND.getCount(KoLConstants.inventory);
    if (wand == 0) {
      return "You don't have a wand of fudge control";
    }

    int fudgecules = FUDGECULE.getCount(KoLConstants.inventory);
    if (fudgecules == 0) {
      return "You don't have any fudgecules";
    }
    return null;
  }

  public static Boolean equip() {
    // Use the wand of fudge control
    RequestThread.postRequest(new GenericRequest("inv_use.php?whichitem=5441"));
    return true;
  }

  public static Boolean unequip() {
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=562&option=6"));
    return true;
  }
}
