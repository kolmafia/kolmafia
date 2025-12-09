package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class PokemporiumRequest extends CoinMasterShopRequest {
  public static final String master = "The Pok&eacute;mporium";
  public static final String SHOPID = "pokefam";

  private static final Pattern POKEDOLLAR_PATTERN =
      Pattern.compile("([\\d,]+) 1,960 pok&eacute;dollar bills");

  public static final AdventureResult POKEDOLLAR =
      new AdventureResult(ItemPool.POKEDOLLAR_BILLS, 1, false) {
        @Override
        public String getPluralName(long price) {
          return price == 1 ? "pok&eacute;dollar bill" : "pok&eacute;dollar bills";
        }
      };

  public static final CoinmasterData POKEMPORIUM =
      new CoinmasterData(master, "pokefam", PokemporiumRequest.class)
          .withToken("pok&eacute;dollar bills")
          .withPluralToken("pok&eacute;dollar bills")
          .withTokenTest("no pok&eacute;dollar bills")
          .withTokenPattern(POKEDOLLAR_PATTERN)
          .withItem(POKEDOLLAR)
          .withShopRowFields(master, SHOPID)
          .withAccessible(PokemporiumRequest::accessible);

  public static String accessible() {
    if (!KoLCharacter.inPokefam()) {
      return "You're not in PokeFam";
    }

    return null;
  }
}
