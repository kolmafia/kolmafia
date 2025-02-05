package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class TerrifiedEagleInnRequest extends CoinMasterRequest {
  public static final String master = "The Terrified Eagle Inn";
  public static final String SHOPID = "dv";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>(\\w+) Freddy Kruegerand(?:s?)</td>");
  public static final AdventureResult KRUEGERAND = ItemPool.get(ItemPool.KRUEGERAND, 1);

  public static final CoinmasterData TERRIFIED_EAGLE_INN =
      new CoinmasterData(master, "dreadsylvania", TerrifiedEagleInnRequest.class)
          .withToken("Freddy Kruegerand")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(KRUEGERAND)
          .withShopRowFields(master, SHOPID)
          .withCanBuyItem(TerrifiedEagleInnRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.TALES_OF_DREAD -> !Preferences.getBoolean("itemBoughtPerCharacter6423");
      case ItemPool.BRASS_DREAD_FLASK -> !Preferences.getBoolean("itemBoughtPerCharacter6428");
      case ItemPool.SILVER_DREAD_FLASK -> !Preferences.getBoolean("itemBoughtPerCharacter6429");
      case ItemPool.FOLDER_21 -> KoLCharacter.hasEquipped(EquipmentManager.FOLDER_HOLDER)
          || KoLCharacter.hasEquipped(EquipmentManager.REPLICA_FOLDER_HOLDER);
      default -> ItemPool.get(itemId).getCount(TERRIFIED_EAGLE_INN.getBuyItems()) > 0;
    };
  }

  public TerrifiedEagleInnRequest() {
    super(TERRIFIED_EAGLE_INN);
  }

  public TerrifiedEagleInnRequest(final boolean buying, final AdventureResult[] attachments) {
    super(TERRIFIED_EAGLE_INN, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }
}
