package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AWOLQuartermasterRequest extends CoinMasterRequest {
  public static final String master = "A. W. O. L. Quartermaster";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(AWOLQuartermasterRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(AWOLQuartermasterRequest.master);

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile(
          "(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? A. W. O. L. commendation");
  public static final AdventureResult COMMENDATION = ItemPool.get(ItemPool.AWOL_COMMENDATION, 1);
  private static final Pattern TOBUY_PATTERN = Pattern.compile("tobuy=(\\d+)");
  public static final CoinmasterData AWOL =
      new CoinmasterData(
          AWOLQuartermasterRequest.master,
          "awol",
          AWOLQuartermasterRequest.class,
          "commendation",
          null,
          false,
          AWOLQuartermasterRequest.TOKEN_PATTERN,
          AWOLQuartermasterRequest.COMMENDATION,
          null,
          null,
          "inv_use.php?whichitem=5116&ajax=1",
          null,
          AWOLQuartermasterRequest.buyItems,
          AWOLQuartermasterRequest.buyPrices,
          null,
          null,
          null,
          null,
          "tobuy",
          AWOLQuartermasterRequest.TOBUY_PATTERN,
          "howmany",
          GenericRequest.HOWMANY_PATTERN,
          null,
          null,
          true);

  private static String lastURL = null;

  public AWOLQuartermasterRequest() {
    super(AWOLQuartermasterRequest.AWOL);
  }

  public AWOLQuartermasterRequest(final boolean buying, final AdventureResult[] attachments) {
    super(AWOLQuartermasterRequest.AWOL, buying, attachments);
  }

  public AWOLQuartermasterRequest(final boolean buying, final AdventureResult attachment) {
    super(AWOLQuartermasterRequest.AWOL, buying, attachment);
  }

  public AWOLQuartermasterRequest(final boolean buying, final int itemId, final int quantity) {
    super(AWOLQuartermasterRequest.AWOL, buying, itemId, quantity);
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
    AWOLQuartermasterRequest.parseResponse(this.responseText);
  }

  private static final Pattern TATTOO_PATTERN = Pattern.compile("sigils/aol(\\d+).gif");

  public static void parseResponse(final String responseText) {
    if (AWOLQuartermasterRequest.lastURL == null) {
      return;
    }

    String location = AWOLQuartermasterRequest.lastURL;
    AWOLQuartermasterRequest.lastURL = null;

    CoinmasterData data = AWOLQuartermasterRequest.AWOL;

    // If you don't have enough commendations, you are redirected to inventory.php
    if (responseText.indexOf("You don't have enough commendations") == -1) {
      // inv_use.php?whichitem=5116&pwd&doit=69&tobuy=xxx&howmany=yyy
      CoinMasterRequest.completePurchase(data, location);
    }

    // Check which tattoo - if any - is for sale: sigils/aol3.gif
    Matcher m = TATTOO_PATTERN.matcher(responseText);
    KoLCharacter.AWOLtattoo = m.find() ? StringUtilities.parseInt(m.group(1)) : 0;

    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    // inv_use.php?whichitem=5116&pwd&doit=69&tobuy=xxx&howmany=yyy
    if (!urlString.startsWith("inv_use.php") || urlString.indexOf("whichitem=5116") == -1) {
      return false;
    }

    // Save URL. If request fails, we are redirected to inventory.php
    AWOLQuartermasterRequest.lastURL = urlString;

    if (urlString.indexOf("doit=69") != -1) {
      CoinmasterData data = AWOLQuartermasterRequest.AWOL;
      CoinMasterRequest.registerRequest(data, urlString);
    }

    return true;
  }

  public static String accessible() {
    int commendations = AWOLQuartermasterRequest.COMMENDATION.getCount(KoLConstants.inventory);
    if (commendations == 0) {
      return "You don't have any A. W. O. L. commendations";
    }
    return null;
  }
}
