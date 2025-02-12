package net.sourceforge.kolmafia.request.coinmaster;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;

public class AltarOfBonesRequest extends CoinMasterRequest {
  public static final String master = "Altar of Bones";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("You have ([\\d,]+).*?bone chips");
  public static final AdventureResult BONE_CHIPS = ItemPool.get(ItemPool.BONE_CHIPS, 1);

  public static final CoinmasterData ALTAR_OF_BONES =
      new CoinmasterData(master, "bonealtar", AltarOfBonesRequest.class)
          .withToken("bone chips")
          .withTokenTest("You have no bone chips")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(BONE_CHIPS)
          .withBuyURL("bone_altar.php")
          .withBuyAction("buy")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withItemField("whichitem")
          .withItemPattern(GenericRequest.WHICHITEM_PATTERN)
          .withAccessible(AltarOfBonesRequest::accessible);

  public AltarOfBonesRequest() {
    super(ALTAR_OF_BONES);
  }

  public AltarOfBonesRequest(final boolean buying, final AdventureResult[] attachments) {
    super(ALTAR_OF_BONES, buying, attachments);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    CoinMasterRequest.parseResponse(ALTAR_OF_BONES, urlString, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    // We only claim bone_altar.php?action=buy
    if (!urlString.startsWith("bone_altar.php")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(ALTAR_OF_BONES, urlString);
  }

  public static String accessible() {
    return "The Altar of Bones is not available";
  }
}
