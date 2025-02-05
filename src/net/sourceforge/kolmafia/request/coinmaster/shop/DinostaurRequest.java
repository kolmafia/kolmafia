package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class DinostaurRequest extends CoinMasterRequest {
  public static final String master = "Dino World Gift Shop (The Dinostaur)";
  public static final String SHOPID = "dino";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Dinodollar");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.DINODOLLAR, 1);

  public static final CoinmasterData DINOSTAUR =
      new CoinmasterData(master, "Dinostaur", DinostaurRequest.class)
          .withToken("Dinodollar")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(DinostaurRequest::accessible);

  public DinostaurRequest() {
    super(DINOSTAUR);
  }

  public DinostaurRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DINOSTAUR, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static String accessible() {
    if (!KoLCharacter.inDinocore()) {
      return "Dino World is not available";
    }
    return null;
  }
}
