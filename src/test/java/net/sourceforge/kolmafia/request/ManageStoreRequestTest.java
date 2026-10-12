package net.sourceforge.kolmafia.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Networking;
import net.sourceforge.kolmafia.session.StoreManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ManageStoreRequestTest {
  @BeforeEach
  @AfterEach
  void clearStore() {
    StoreManager.clearCache();
  }

  @ParameterizedTest
  @ValueSource(strings = {"backoffice.php", "backoffice.php?which=1"})
  void parsesStoreStock(String url) {
    ManageStoreRequest.parseResponse(
        url, Networking.html("request/test_backoffice_inventory.html"));

    assertThat(StoreManager.getSoldItemList(), hasSize(16));
    assertThat(StoreManager.shopAmount(7818), equalTo(35));
  }

  @ParameterizedTest
  @ValueSource(strings = {"backoffice.php", "backoffice.php?which=2"})
  void unrelatedBackofficePagesDoNotParseStock(String url) {
    ManageStoreRequest.parseResponse(
        "backoffice.php?which=1", Networking.html("request/test_backoffice_inventory.html"));

    ManageStoreRequest.parseResponse(
        url,
        "[<a href=\"backoffice.php?which=1\">inventory management</a>]&nbsp;&nbsp;[store management]");

    assertThat(StoreManager.getSoldItemList(), hasSize(16));
  }
}
