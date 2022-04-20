package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AltarOfBonesRequest;
import net.sourceforge.kolmafia.request.BrogurtRequest;
import net.sourceforge.kolmafia.request.DimemasterRequest;
import net.sourceforge.kolmafia.request.EdShopRequest;
import net.sourceforge.kolmafia.request.TacoDanRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CoinmasterRegistryTest {
  @BeforeEach
  public void init() {
    KoLCharacter.reset("CoinmasterTest");
    Preferences.reset("CoinmasterTest");
  }

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

  @Test
  public void brogurtQuestTest() {
    Preferences.setString("questESlBacteria", "unstarted");

    assertNull(CoinmasterRegistry.findSeller(7457));

    Preferences.setString("questESlBacteria", "finished");
    assertEquals(BrogurtRequest.BROGURT, CoinmasterRegistry.findSeller(7457));
  }

  @Test
  public void tacoDansTacoQuestsTest() {
    Preferences.setString("questESlFish", "unstarted");
    Preferences.setString("questESlSprinkles", "unstarted");

    assertNull(CoinmasterRegistry.findSeller(7451));
    assertNull(CoinmasterRegistry.findSeller(7452));

    Preferences.setString("questESlFish", "finished");
    assertEquals(TacoDanRequest.TACO_DAN, CoinmasterRegistry.findSeller(7451));
    assertNull(CoinmasterRegistry.findSeller(7452));

    Preferences.setString("questESlSprinkles", "finished");
    assertEquals(TacoDanRequest.TACO_DAN, CoinmasterRegistry.findSeller(7452));
  }
}
