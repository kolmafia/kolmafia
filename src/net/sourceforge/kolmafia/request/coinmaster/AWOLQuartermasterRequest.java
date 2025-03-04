package net.sourceforge.kolmafia.request.coinmaster;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AWOLQuartermasterRequest extends CoinMasterRequest {
  public static final String master = "A. W. O. L. Quartermaster";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile(
          "(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? A. W. O. L. commendation");
  public static final AdventureResult COMMENDATION = ItemPool.get(ItemPool.AWOL_COMMENDATION, 1);
  private static final Pattern TOBUY_PATTERN = Pattern.compile("tobuy=(\\d+)");

  public static final CoinmasterData AWOL =
      new CoinmasterData(master, "awol", AWOLQuartermasterRequest.class)
          .withToken("commendation")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COMMENDATION)
          .withBuyURL("inv_use.php?whichitem=5116&ajax=1")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withItemField("tobuy")
          .withItemPattern(TOBUY_PATTERN)
          .withCountField("howmany")
          .withCountPattern(GenericRequest.HOWMANY_PATTERN)
          .withAccessible(AWOLQuartermasterRequest::accessible);

  private static String lastURL = null;

  public AWOLQuartermasterRequest() {
    super(AWOL);
  }

  public AWOLQuartermasterRequest(final boolean buying, final AdventureResult[] attachments) {
    super(AWOL, buying, attachments);
  }

  @Override
  public void run() {
    if (this.attachments != null) {
      this.addFormField("doit", "69");
    }

    super.run();
  }

  @Override
  public void processResults() {
    parseResponse(this.responseText);
  }

  private static final Pattern TATTOO_PATTERN = Pattern.compile("sigils/aol(\\d+).gif");

  public static void parseResponse(final String responseText) {
    if (lastURL == null) {
      return;
    }

    String location = lastURL;
    lastURL = null;

    // If you don't have enough commendations, you are redirected to inventory.php
    if (responseText.indexOf("You don't have enough commendations") == -1) {
      // inv_use.php?whichitem=5116&pwd&doit=69&tobuy=xxx&howmany=yyy
      CoinMasterRequest.completePurchase(AWOL, location);
    }

    // Check which tattoo - if any - is for sale: sigils/aol3.gif
    Matcher m = TATTOO_PATTERN.matcher(responseText);
    KoLCharacter.AWOLtattoo = m.find() ? StringUtilities.parseInt(m.group(1)) : 0;

    CoinMasterRequest.parseBalance(AWOL, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    // inv_use.php?whichitem=5116&pwd&doit=69&tobuy=xxx&howmany=yyy
    if (!urlString.startsWith("inv_use.php") || urlString.indexOf("whichitem=5116") == -1) {
      return false;
    }

    // Save URL. If request fails, we are redirected to inventory.php
    lastURL = urlString;

    if (urlString.indexOf("doit=69") != -1) {
      CoinMasterRequest.registerRequest(AWOL, urlString);
    }

    return true;
  }

  public static String accessible() {
    int commendations = COMMENDATION.getCount(KoLConstants.inventory);
    if (commendations == 0) {
      return "You don't have any A. W. O. L. commendations";
    }
    return null;
  }
}
