package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class RubeeRequest extends CoinMasterRequest {
  public static final String master = "FantasyRealm Rubee&trade; Store";
  public static final String SHOPID = "fantasyrealm";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Rubees&trade;");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.RUBEE, 1);

  public static final CoinmasterData RUBEE =
      new CoinmasterData(master, "FantasyRealm Store", RubeeRequest.class)
          .withToken("Rubee&trade;")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(RubeeRequest::accessible);

  public RubeeRequest() {
    super(RUBEE);
  }

  public RubeeRequest(final boolean buying, final AdventureResult[] attachments) {
    super(RUBEE, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static String accessible() {
    if (Preferences.getBoolean("_frToday") || Preferences.getBoolean("frAlways")) {
      return null;
    }
    return "Need access to Fantasy Realm";
  }

  @Override
  public void equip() {
    if (!KoLCharacter.hasEquipped(ItemPool.FANTASY_REALM_GEM)) {
      EquipmentRequest request =
          new EquipmentRequest(ItemPool.get(ItemPool.FANTASY_REALM_GEM), Slot.ACCESSORY3);
      RequestThread.postRequest(request);
    }
  }
}
