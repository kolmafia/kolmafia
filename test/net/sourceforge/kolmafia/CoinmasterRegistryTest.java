package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.DimemasterRequest;
import net.sourceforge.kolmafia.request.EdShopRequest;
import org.junit.jupiter.api.Test;

public class CoinmasterRegistryTest {
  @Test
  public void canFindCoinmasterByNickname() {
    var cm = CoinmasterRegistry.findCoinmasterByNickname("bonealtar");
    assertEquals(AltarOfBonesRequest.ALTAR_OF_BONES, cm);
  }

  @Test
  public void returnsNullWhenNicknameDoesntExist() {
    var cm = CoinmasterRegistry.findCoinmasterByNickname("thisdoesntexist");
    assertNull(cm);
  }

  @Test
  public void canFindBuyerForItem() {
    var cm = CoinmasterRegistry.findBuyer(2065);
    assertEquals(DimemasterRequest.HIPPY, cm);
  }

  @Test
  public void returnsNulLWhenBuyerDoesntExist() {
    var cm = CoinmasterRegistry.findBuyer(1);
    assertNull(cm);
  }

  @Test
  public void canFindSellerForItem() {
    var cm = CoinmasterRegistry.findSeller(7972);
    assertEquals(EdShopRequest.EDSHOP, cm);
  }

  @Test
  public void returnsNulLWhenSellerDoesntExist() {
    var cm = CoinmasterRegistry.findSeller(1);
    assertNull(cm);
  }
}
