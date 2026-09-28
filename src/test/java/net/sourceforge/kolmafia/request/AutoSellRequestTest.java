package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.LimitMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AutoSellRequestTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset("AutoSellRequestTest");
    Preferences.reset("AutoSellRequestTest");
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Nested
  class EffectiveAutosellPrices {
    @ParameterizedTest
    @CsvSource({"0, 0", "1, 60", "2, 121", "20, 1218", "100000000, 6090000000"})
    void roundsAfterMultiplyingQuantity(int quantity, long expected) {
      try (var cleanups =
          new Cleanups(
              withProperty("autoSellingShorts", true), withItem(ItemPool.SELLING_SHORTS))) {
        assertThat(
            AutoSellRequest.getEffectiveAutosellPrice(ItemPool.MOUNTAIN_STREAM_SODA, quantity),
            is(expected));
        assertThat(ItemDatabase.getPriceById(ItemPool.MOUNTAIN_STREAM_SODA), is(58));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {ItemPool.MEAT_PASTE, ItemPool.MEAT_STACK, ItemPool.DENSE_STACK})
    void excludesMeatItems(int itemId) {
      try (var cleanups = withEquipped(Slot.PANTS, ItemPool.SELLING_SHORTS)) {
        assertThat(
            AutoSellRequest.getEffectiveAutosellPrice(itemId, 2),
            is(2L * ItemDatabase.getPriceById(itemId)));
      }
    }

    @ParameterizedTest
    @CsvSource({"false, false, 58", "true, false, 58", "false, true, 58", "true, true, 60"})
    void requiresPreferenceAndAvailableShorts(boolean enabled, boolean available, int expected) {
      try (var cleanups =
          new Cleanups(
              withProperty("autoSellingShorts", enabled),
              withItem(ItemPool.SELLING_SHORTS, available ? 1 : 0))) {
        assertThat(
            AutoSellRequest.getEffectiveAutosellPrice(ItemPool.MOUNTAIN_STREAM_SODA), is(expected));
      }
    }

    @Test
    void includesWornShortsWithAutomationDisabled() {
      try (var cleanups =
          new Cleanups(
              withProperty("autoSellingShorts", false),
              withEquipped(Slot.PANTS, ItemPool.SELLING_SHORTS))) {
        assertThat(
            AutoSellRequest.getEffectiveAutosellPrice(ItemPool.MOUNTAIN_STREAM_SODA), is(60));
        assertThat(AutoSellRequest.getEffectiveAutosellPrice(ItemPool.SELLING_SHORTS), is(0));
      }
    }

    @Test
    void doesNotAssumeShortsCanBeEquippedInRestrictedModes() {
      try (var cleanups =
          new Cleanups(
              withProperty("autoSellingShorts", true),
              withItem(ItemPool.SELLING_SHORTS),
              withLimitMode(LimitMode.SPELUNKY))) {
        assertThat(
            AutoSellRequest.getEffectiveAutosellPrice(ItemPool.MOUNTAIN_STREAM_SODA), is(58));
      }
    }
  }

  @Nested
  class SellingShorts {
    private FakeHttpClientBuilder client() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(
          request ->
              new FakeHttpResponse<>(
                  200,
                  request.uri().getPath().equals("/inv_equip.php")
                      ? "Item equipped. Item unequipped"
                      : "{}"));
      return builder;
    }

    @ParameterizedTest
    @ValueSource(strings = {"autosell", "relayCompact", "relayDetailed", "noPants"})
    void equipsForSaleAndRestoresPants(String kind) {
      var builder = client();
      try (var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("autoSellingShorts", true),
              withItem(ItemPool.SELLING_SHORTS),
              withItem(ItemPool.SEAL_TOOTH),
              withEquipped(
                  Slot.PANTS,
                  kind.equals("noPants")
                      ? EquipmentRequest.UNEQUIP
                      : ItemPool.get("old sweatpants", 1)))) {
        var pants = EquipmentManager.getEquipment(Slot.PANTS);
        GenericRequest request;
        if (kind.startsWith("relay")) {
          request = new RelayRequest(false);
          request.constructURLString(
              kind.equals("relayDetailed")
                  ? "sellstuff_ugly.php?action=sell&quantity=1&item2=1"
                  : "sellstuff.php?action=sell&type=all&howmany=1&whichitem[]=2");
        } else {
          request = new AutoSellRequest(ItemPool.get(ItemPool.SEAL_TOOTH, 1));
        }
        request.run();
        var requests =
            builder.client.getRequests().stream()
                .filter(r -> !r.uri().getPath().equals("/api.php"))
                .toList();
        assertThat(requests, hasSize(3));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=12300");
        assertThat(requests.get(1).uri().getPath(), startsWith("/sellstuff"));
        assertPostRequest(
            requests.get(2),
            "/inv_equip.php",
            kind.equals("noPants")
                ? "which=2&ajax=1&action=unequip&type=pants"
                : "which=2&ajax=1&action=equip&whichitem=" + pants.getItemId());
        assertThat(EquipmentManager.getEquipment(Slot.PANTS), equalTo(pants));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "missing", "equipped", "browse", "sellingPants", "limited"})
    void skipsUnnecessaryChanges(String reason) {
      var builder = client();
      try (var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("autoSellingShorts", !reason.equals("disabled")),
              withLimitMode(reason.equals("limited") ? LimitMode.SPELUNKY : LimitMode.NONE),
              withItem(ItemPool.SELLING_SHORTS, reason.equals("missing") ? 0 : 1),
              withEquipped(
                  Slot.PANTS, reason.equals("equipped") ? "selling shorts" : "old sweatpants"))) {
        int item =
            reason.equals("sellingPants")
                ? EquipmentManager.getEquipment(Slot.PANTS).getItemId()
                : ItemPool.SEAL_TOOTH;
        String url =
            reason.equals("browse")
                ? "sellstuff.php"
                : "sellstuff.php?action=sell&type=all&whichitem[]=" + item;
        var sold = new AtomicBoolean();
        AutoSellRequest.withSellingShorts(new GenericRequest(url), () -> sold.set(true));
        assertThat(sold.get(), is(true));
        assertThat(builder.client.getRequests(), empty());
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void restoresAfterFailedSale(boolean throwsException) {
      var builder = client();
      try (var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withProperty("autoSellingShorts", true),
              withItem(ItemPool.SELLING_SHORTS),
              withEquipped(Slot.PANTS, "old sweatpants"))) {
        var pants = EquipmentManager.getEquipment(Slot.PANTS);
        Runnable sale =
            () ->
                AutoSellRequest.withSellingShorts(
                    new GenericRequest("sellstuff.php?action=sell&whichitem[]=2"),
                    () -> {
                      assertThat(
                          KoLCharacter.hasEquipped(ItemPool.SELLING_SHORTS, Slot.PANTS), is(true));
                      if (throwsException) throw new IllegalStateException("Sale failed");
                      StaticEntity.setContinuationState(MafiaState.ERROR);
                    });
        if (throwsException) assertThrows(IllegalStateException.class, sale::run);
        else sale.run();
        assertThat(EquipmentManager.getEquipment(Slot.PANTS), equalTo(pants));
        assertThat(
            StaticEntity.getContinuationState(),
            is(throwsException ? MafiaState.CONTINUE : MafiaState.ERROR));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"main.php", "sellstuff.php?action=sell&whichitem[]=2"})
    void genericRequestsDoNotChangeEquipment(String url) {
      var builder = client();
      try (var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("autoSellingShorts", true),
              withItem(ItemPool.SELLING_SHORTS),
              withItem(ItemPool.SEAL_TOOTH))) {
        new GenericRequest(url).run();
        assertThat(
            builder.client.getRequests().stream()
                .anyMatch(r -> r.uri().getPath().equals("/inv_equip.php")),
            is(false));
      }
    }
  }
}
