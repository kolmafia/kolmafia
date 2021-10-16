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
  // That may be because of the aggressive way Java substitutes unicode
  public void itShouldUnescape() {
    String a = "This is mixed and matched &pound; &curren;";
    String ua = CharacterEntities.unescape(a);
    String eua = CharacterEntities.escape(ua);
    assertEquals(a, eua);
    String b = "This is mixed and matched \u00a3 \u00a4";
    String ub = CharacterEntities.unescape(b); // no & so unchanged
    assertEquals(b, ub);
    String eub = CharacterEntities.escape(ub); // should replace
    assertEquals(a, eub); // if really inverse should compare b and eub
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
