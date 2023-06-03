package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
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

      var request = new MrStore2002Request();
      request.run();

      assertThat("_2002MrStoreCreditsCollected", isSetTo(true));
      assertThat("availableMrStore2002Credits", isSetTo(3));

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0),
          "/inv_use.php",
          "which=3&ajax=1&whichitem=" + ItemPool.MR_STORE_2002_CATALOG);
      assertGetRequest(requests.get(1), "/shop.php", "whichshop=mrstore2002");
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

      var request = new MrStore2002Request();
      request.run();

      assertThat("_2002MrStoreCreditsCollected", isSetTo(true));
      assertThat("availableMrStore2002Credits", isSetTo(3));

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0),
          "/inv_use.php",
          "which=3&ajax=1&whichitem=" + ItemPool.REPLICA_MR_STORE_2002_CATALOG);
      assertGetRequest(requests.get(1), "/shop.php", "whichshop=mrstore2002");
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

      var request = new MrStore2002Request();
      request.run();

      assertThat("_2002MrStoreCreditsCollected", isSetTo(true));
      assertThat("availableMrStore2002Credits", isSetTo(3));

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0),
          "/inv_use.php",
          "which=3&ajax=1&whichitem=" + ItemPool.MR_STORE_2002_CATALOG);
      assertGetRequest(requests.get(1), "/shop.php", "whichshop=mrstore2002");
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

      var request = new MrStore2002Request();
      request.run();

      assertThat("_2002MrStoreCreditsCollected", isSetTo(false));
      assertThat("availableMrStore2002Credits", isSetTo(0));

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0),
          "/inv_use.php",
          "which=3&ajax=1&whichitem=" + ItemPool.MR_STORE_2002_CATALOG);
      assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
    }
  }

  @Test
  void visitingMrStore2020NeedNotUseCatalog() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPath(Path.STANDARD),
            withProperty("availableMrStore2002Credits", 0),
            withProperty("_2002MrStoreCreditsCollected", true));
    try (cleanups) {
      client.addResponse(200, html("request/test_visit_mr_store_2002.html"));

      var request = new MrStore2002Request();
      request.run();

      assertThat("availableMrStore2002Credits", isSetTo(3));

      var requests = client.getRequests();
      assertThat(requests, hasSize(1));
      assertPostRequest(requests.get(0), "/shop.php", "whichshop=mrstore2002");
    }
  }
}
