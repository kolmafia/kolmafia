package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

public abstract class GeneFiddlingRequest extends CoinMasterShopRequest {
  public static final String master = "Fiddling With Your Genes";
  public static final String SHOPID = "mutate";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "GeneFiddling", GeneFiddlingRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(GeneFiddlingRequest::accessible);

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
