package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withNoItems;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.shop.MrStore2002Request;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MrStore2002RequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("MrStore2002RequestTest");
  }

  @BeforeEach
  void beforeEach() {
    Preferences.reset("MrStore2002RequestTest");
  }

  @Test
  void visitingMrStore2020UsesCatalog() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPath(Path.STANDARD),
            withItem(ItemPool.MR_STORE_2002_CATALOG),
            withProperty("availableMrStore2002Credits", 0),
            withProperty("_2002MrStoreCreditsCollected", false));
    try (cleanups) {
      client.addResponse(200, html("request/test_use_mr_store_2002_catalog.html"));
      client.addResponse(200, html("request/test_visit_mr_store_2002.html"));

      var request = MrStore2002Request.MR_STORE_2002.getRequest();
      request.run();

      assertThat("_2002MrStoreCreditsCollected", isSetTo(true));
      assertThat("availableMrStore2002Credits", isSetTo(3));

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0),
          "/inv_use.php",
          "whichitem=" + ItemPool.MR_STORE_2002_CATALOG + "&ajax=1");
      assertPostRequest(requests.get(1), "/shop.php", "whichshop=mrstore2002");
    }
  }

  @Test
  void buyingFromvisitingMrStore2020UsesCatalog() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPath(Path.STANDARD),
            withNoItems(),
            withItem(ItemPool.MR_STORE_2002_CATALOG),
            withProperty("availableMrStore2002Credits", 3),
            withProperty("_2002MrStoreCreditsCollected", false));
    try (cleanups) {
      client.addResponse(200, html("request/test_use_mr_store_2002_catalog.html"));
      client.addResponse(200, html("request/test_buy_from_mr_store_2002_ajax.html"));
      client.addResponse(200, ""); // api.php

      var request =
          MrStore2002Request.MR_STORE_2002.getRequest(
              true,
              new AdventureResult[] {
                ItemPool.get(ItemPool.FLASH_LIQUIDIZER_ULTRA_DOUSING_ACCESSORY, 1)
              });
      request.run();

      assertThat("_2002MrStoreCreditsCollected", isSetTo(true));
      assertThat(
          InventoryManager.hasItem(ItemPool.FLASH_LIQUIDIZER_ULTRA_DOUSING_ACCESSORY), is(true));
      assertThat("availableMrStore2002Credits", isSetTo(2));

      var requests = client.getRequests();
      assertThat(requests, hasSize(3));
      assertPostRequest(
          requests.get(0),
          "/inv_use.php",
          "whichitem=" + ItemPool.MR_STORE_2002_CATALOG + "&ajax=1");
      assertPostRequest(
          requests.get(1),
          "/shop.php",
          "whichshop=mrstore2002&action=buyitem&quantity=1&whichrow=1387");
      assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
    }
  }

  @Test
  void visitingMrStore2020RetrievesCatalog() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPath(Path.STANDARD),
            withNoItems(),
            withItemInCloset(ItemPool.MR_STORE_2002_CATALOG),
            withProperty("autoSatisfyWithCloset", true),
            withProperty("availableMrStore2002Credits", 0),
            withProperty("_2002MrStoreCreditsCollected", false));
    try (cleanups) {
      client.addResponse(200, html("request/test_uncloset_mr_store_2002_catalog.html"));
      client.addResponse(200, html("request/test_use_mr_store_2002_catalog.html"));
      client.addResponse(200, html("request/test_visit_mr_store_2002.html"));

      var request = MrStore2002Request.MR_STORE_2002.getRequest();
      request.run();

      assertThat(InventoryManager.hasItem(ItemPool.MR_STORE_2002_CATALOG), is(true));
      assertThat("_2002MrStoreCreditsCollected", isSetTo(true));
      assertThat("availableMrStore2002Credits", isSetTo(3));

      var requests = client.getRequests();
      assertThat(requests, hasSize(3));
      assertGetRequest(
          requests.get(0), "/inventory.php", "action=closetpull&ajax=1&whichitem=11257&qty=1");
      assertPostRequest(
          requests.get(1),
          "/inv_use.php",
          "whichitem=" + ItemPool.MR_STORE_2002_CATALOG + "&ajax=1");
      assertPostRequest(requests.get(2), "/shop.php", "whichshop=mrstore2002");
    }
  }

  @Test
  void visitingMrStore2020UsesReplicaCatalog() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPath(Path.LEGACY_OF_LOATHING),
            withItem(ItemPool.REPLICA_MR_STORE_2002_CATALOG),
            withProperty("availableMrStore2002Credits", 0),
            withProperty("_2002MrStoreCreditsCollected", false));
    try (cleanups) {
      client.addResponse(200, html("request/test_use_mr_store_2002_catalog.html"));
      client.addResponse(200, html("request/test_visit_mr_store_2002.html"));

      var request = MrStore2002Request.MR_STORE_2002.getRequest();
      request.run();

      assertThat("_2002MrStoreCreditsCollected", isSetTo(true));
      assertThat("availableMrStore2002Credits", isSetTo(3));

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0),
          "/inv_use.php",
          "whichitem=" + ItemPool.REPLICA_MR_STORE_2002_CATALOG + "&ajax=1");
      assertPostRequest(requests.get(1), "/shop.php", "whichshop=mrstore2002");
    }
  }

  @Test
  void visitingMrStore2020InLoLCanUseCatalog() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPath(Path.LEGACY_OF_LOATHING),
            withItem(ItemPool.MR_STORE_2002_CATALOG),
            withProperty("availableMrStore2002Credits", 0),
            withProperty("_2002MrStoreCreditsCollected", false));
    try (cleanups) {
      client.addResponse(200, html("request/test_use_mr_store_2002_catalog.html"));
      client.addResponse(200, html("request/test_visit_mr_store_2002.html"));

      var request = MrStore2002Request.MR_STORE_2002.getRequest();
      request.run();

      assertThat("_2002MrStoreCreditsCollected", isSetTo(true));
      assertThat("availableMrStore2002Credits", isSetTo(3));

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0),
          "/inv_use.php",
          "whichitem=" + ItemPool.MR_STORE_2002_CATALOG + "&ajax=1");
      assertPostRequest(requests.get(1), "/shop.php", "whichshop=mrstore2002");
    }
  }

  @Test
  void visitingMrStore2020WithNoCatalogFails() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("availableMrStore2002Credits", 0),
            withProperty("_2002MrStoreCreditsCollected", false));
    try (cleanups) {
      client.addResponse(200, html("request/test_use_mr_store_2002_catalog_fails.html"));
      client.addResponse(200, "");

      var request = MrStore2002Request.MR_STORE_2002.getRequest();
      request.run();

      assertThat("_2002MrStoreCreditsCollected", isSetTo(false));
      assertThat("availableMrStore2002Credits", isSetTo(0));

      var requests = client.getRequests();
      assertThat(requests, hasSize(0));
    }
  }

  @Test
  void usingCatalogParsesStore() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.MR_STORE_2002_CATALOG),
            withProperty("availableMrStore2002Credits", 0),
            withProperty("_2002MrStoreCreditsCollected", false));
    try (cleanups) {
      client.addResponse(200, html("request/test_use_mr_store_2002_catalog.html"));
      client.addResponse(200, html("request/test_visit_mr_store_2002.html"));

      var request = UseItemRequest.getInstance(ItemPool.MR_STORE_2002_CATALOG);
      request.run();

      assertThat("_2002MrStoreCreditsCollected", isSetTo(true));
      assertThat("availableMrStore2002Credits", isSetTo(3));

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0),
          "/inv_use.php",
          "whichitem=" + ItemPool.MR_STORE_2002_CATALOG + "&ajax=1");
      assertGetRequest(requests.get(1), "/shop.php", "whichshop=mrstore2002");
    }
  }
}
