package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ForbiddenManagerTest {

  private static final String TESTUSERNAME = "ForbiddenManager";

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset(TESTUSERNAME);
    MallPurchaseRequest.reset();
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset(TESTUSERNAME);
  }

  @AfterEach
  public void afterEach() {
    MallPurchaseRequest.reset();
  }

  @Test
  public void willLoadFromProperty() {
    var cleanups = new Cleanups(withProperty("forbiddenStores", "123,456,789"));
    try (cleanups) {
      var forbidden = MallPurchaseRequest.getForbiddenStores();
      assertEquals(3, forbidden.size());
      assertTrue(forbidden.contains(123));
      assertTrue(forbidden.contains(456));
      assertTrue(forbidden.contains(789));
    }
  }

  @Test
  public void checksIsForbidden() {
    var cleanups = new Cleanups(withProperty("forbiddenStores", "123"));
    try (cleanups) {
      assertTrue(MallPurchaseRequest.isForbidden(123));
      assertFalse(MallPurchaseRequest.isForbidden(456));
    }
  }

  @Test
  public void canAddForbiddenStore() {
    var cleanups = new Cleanups(withProperty("forbiddenStores", ""));
    try (cleanups) {
      var forbidden = MallPurchaseRequest.getForbiddenStores();
      assertEquals(0, forbidden.size());
      assertFalse(MallPurchaseRequest.isForbidden(123));
      MallPurchaseRequest.addForbiddenStore(123);
      assertEquals(Preferences.getString("forbiddenStores"), "123");
      forbidden = MallPurchaseRequest.getForbiddenStores();
      assertEquals(1, forbidden.size());
      assertTrue(MallPurchaseRequest.isForbidden(123));
    }
  }

  @Test
  public void canRemoveForbiddenStore() {
    var cleanups = new Cleanups(withProperty("forbiddenStores", "123"));
    try (cleanups) {
      var forbidden = MallPurchaseRequest.getForbiddenStores();
      assertEquals(1, forbidden.size());
      assertTrue(MallPurchaseRequest.isForbidden(123));
      MallPurchaseRequest.removeForbiddenStore(123);
      assertEquals(Preferences.getString("forbiddenStores"), "");
      forbidden = MallPurchaseRequest.getForbiddenStores();
      assertEquals(0, forbidden.size());
      assertFalse(MallPurchaseRequest.isForbidden(123));
    }
  }

  @Test
  public void canToggleForbiddenStore() {
    var cleanups = new Cleanups(withProperty("forbiddenStores", ""));
    try (cleanups) {
      var forbidden = MallPurchaseRequest.getForbiddenStores();
      assertEquals(0, forbidden.size());
      assertFalse(MallPurchaseRequest.isForbidden(123));
      MallPurchaseRequest.toggleForbiddenStore(123);
      assertEquals(Preferences.getString("forbiddenStores"), "123");
      forbidden = MallPurchaseRequest.getForbiddenStores();
      assertEquals(1, forbidden.size());
      assertTrue(MallPurchaseRequest.isForbidden(123));
      MallPurchaseRequest.toggleForbiddenStore(123);
      assertEquals(Preferences.getString("forbiddenStores"), "");
      forbidden = MallPurchaseRequest.getForbiddenStores();
      assertEquals(0, forbidden.size());
      assertFalse(MallPurchaseRequest.isForbidden(123));
    }
  }

  @Test
  public void canUpdateForbiddenStores() {
    var cleanups = new Cleanups(withProperty("forbiddenStores", "123"));
    try (cleanups) {
      var forbidden = MallPurchaseRequest.getForbiddenStores();
      assertEquals(1, forbidden.size());
      assertTrue(MallPurchaseRequest.isForbidden(123));
      Preferences.setString("forbiddenStores", "456,789");
      forbidden = MallPurchaseRequest.getForbiddenStores();
      assertEquals(2, forbidden.size());
      assertFalse(MallPurchaseRequest.isForbidden(123));
      assertTrue(MallPurchaseRequest.isForbidden(456));
      assertTrue(MallPurchaseRequest.isForbidden(789));
    }
  }
}
