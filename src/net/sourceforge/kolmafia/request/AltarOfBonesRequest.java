package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class AltarOfBonesRequest extends CoinMasterRequest {
  public static final String master = "Altar of Bones";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(AltarOfBonesRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(AltarOfBonesRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("You have ([\\d,]+).*?bone chips");
  public static final AdventureResult BONE_CHIPS = ItemPool.get(ItemPool.BONE_CHIPS, 1);
  public static final CoinmasterData ALTAR_OF_BONES =
      new CoinmasterData(
          AltarOfBonesRequest.master,
          "bonealtar",
          AltarOfBonesRequest.class,
          "bone chips",
          "You have no bone chips",
          false,
          AltarOfBonesRequest.TOKEN_PATTERN,
          AltarOfBonesRequest.BONE_CHIPS,
          null,
          null,
          "bone_altar.php",
          "buy",
          AltarOfBonesRequest.buyItems,
          AltarOfBonesRequest.buyPrices,
          null,
          null,
          null,
          null,
          "whichitem",
          GenericRequest.WHICHITEM_PATTERN,
          null,
          null,
          null,
          null,
          true);

  public AltarOfBonesRequest() {
    super(AltarOfBonesRequest.ALTAR_OF_BONES);
  }

  public AltarOfBonesRequest(final boolean buying, final AdventureResult[] attachments) {
    super(AltarOfBonesRequest.ALTAR_OF_BONES, buying, attachments);
  }

  public AltarOfBonesRequest(final boolean buying, final AdventureResult attachment) {
    super(AltarOfBonesRequest.ALTAR_OF_BONES, buying, attachment);
  }

  public AltarOfBonesRequest(final boolean buying, final int itemId, final int quantity) {
    super(AltarOfBonesRequest.ALTAR_OF_BONES, buying, itemId, quantity);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    CoinMasterRequest.parseResponse(AltarOfBonesRequest.ALTAR_OF_BONES, urlString, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    // We only claim bone_altar.php?action=buy
    if (!urlString.startsWith("bone_altar.php")) {
      return false;
    }

    CoinmasterData data = AltarOfBonesRequest.ALTAR_OF_BONES;
    return CoinMasterRequest.registerRequest(data, urlString);
  }

  public static String accessible() {
    return "The Altar of Bones is not available";
  }
}
