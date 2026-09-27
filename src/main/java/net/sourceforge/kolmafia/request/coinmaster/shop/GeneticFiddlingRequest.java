package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class GeneticFiddlingRequest extends CoinMasterShopRequest {
  public static final String master = "Genetic Fiddling";
  public static final String SHOPID = "mutate";

  public static final AdventureResult COIN = ItemPool.get(ItemPool.RAD, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "GeneticFiddling", GeneticFiddlingRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withItem(COIN)
          .withAccessible(GeneticFiddlingRequest::accessible);

  // More work will be needed to make this fully functional.
  //
  // - Presumably, skills are unavailable if you already have them.
  // - Skills from the level 6 shop require access to that level.

  public static String accessible() {
    if (!KoLCharacter.inNuclearAutumn()) {
      return "You don't have a Fallout Shelter";
    }
    return null;
  }
}
