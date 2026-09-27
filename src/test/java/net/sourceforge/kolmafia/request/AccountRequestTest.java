package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withTopMenuStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.request.GenericRequest.TopMenuStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

class AccountRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("AccountRequestTest");
    CharPaneRequest.reset();
  }

  @AfterEach
  public void afterEach() {
    CharPaneRequest.reset();
  }

  @Nested
  class MenuStyle {
    private TopMenuStyle nameToStyle(String name) {
      return switch (name) {
        case "normal" -> TopMenuStyle.NORMAL;
        case "compact" -> TopMenuStyle.COMPACT;
        case "fancy" -> TopMenuStyle.FANCY;
        default -> TopMenuStyle.UNKNOWN;
      };
    }

    @CartesianTest
    void canDetectMenuStyleFromAccountInterfaceTab(
        @Values(strings = {"normal", "compact", "fancy"}) String styleName,
        @Enum TopMenuStyle style) {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withTopMenuStyle(style));
      try (cleanups) {
        String location = "account.php";
        String path = "request/test_account_" + styleName + "_topmenu.html";
        client.addResponse(200, html(path));
        var request = new GenericRequest(location);
        request.run();

        // Verify that we have detected and set the topmenu style
        assertEquals(GenericRequest.topMenuStyle, nameToStyle(styleName));
      }
    }

    @CartesianTest
    void canDetectMenuStyleChangeFromAccountInterfaceTab(
        @Values(strings = {"normal", "compact", "fancy"}) String styleName,
        @Enum TopMenuStyle style) {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withTopMenuStyle(style));
      try (cleanups) {
        String location = "account.php?am=1&action=menu&ajax=1&value=" + styleName;
        String path = "request/test_account_switch_" + styleName + "_topmenu.html";
        client.addResponse(200, html(path));
        var request = new GenericRequest(location);
        request.run();

        // Verify that we have detected and set the topmenu style
        assertEquals(GenericRequest.topMenuStyle, nameToStyle(styleName));
      }
    }
  }
}
