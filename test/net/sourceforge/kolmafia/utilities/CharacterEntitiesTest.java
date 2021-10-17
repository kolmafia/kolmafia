package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * CharacterEntities is pretty well tested by other tests. This one is mostly to increase the Clover
 * coverage by filling in things that are not exercised elsewhere.
 */
public class CharacterEntitiesTest {
  @Test
  public void itShouldEscape() {
    String a = "This is mixed and matched &pound; &curren;";
    String ea = CharacterEntities.escape(a);
    String uea = CharacterEntities.unescape(ea);
    assertEquals(a, uea);
    String b = "This is mixed and matched \u00a3 \u00a4";
    String eb = CharacterEntities.escape(b);
    String ueb = CharacterEntities.unescape(eb);
    assertEquals(b, ueb);
  }

  @Test
  // This test discovered that escape and unescape are not inverses for all inputs.
  public void itShouldUnescape() {
    String a = "This is mixed and matched &pound; &curren;";
    String ua = CharacterEntities.unescape(a);
    String eua = CharacterEntities.escape(ua);
    assertEquals(a, eua);
    String b = "This is mixed and matched \u00a3 \u00a4";
    String ub = CharacterEntities.unescape(b); // nothing to unescape so should be equal
    assertEquals(b, ub);
    String eub = CharacterEntities.escape(ub); // there should be a replacement
    assertNotEquals(ub, eub); // so not equal
    assertNotEquals(b, eub); // and not inverse
    assertEquals(a, eub); // true because of data choice.
  }

  @Test
  public void itShouldHandleSomeInterestingCases() {
    String a = null;
    String ea = CharacterEntities.escape(a);
    String ua = CharacterEntities.unescape(a);
    assertEquals(ea, "");
    assertEquals(ua, "");
    a = "This & that";
    ea = CharacterEntities.escape(a);
    ua = CharacterEntities.unescape(a);
    assertEquals(ea, "This &amp; that");
    assertEquals(a, ua);
    a = "&xyzzy;";
    ea = CharacterEntities.escape(a);
    ua = CharacterEntities.unescape(a);
    assertEquals(ea, "&amp;xyzzy;");
    assertEquals(a, ua);
  }
}
