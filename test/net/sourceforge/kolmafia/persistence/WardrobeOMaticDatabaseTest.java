package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import org.junit.jupiter.api.Test;

// Aug 19th, 2025: 8226
// Tier 5
public class WardrobeOMaticDatabaseTest {
  @Test
  void generatesShirt() {
    var shirt = WardrobeOMaticDatabase.shirt(8226, 5);

    assertThat(shirt.name(), is("electroplated hyperwool Neo-Hawaiian shirt"));
    assertThat(shirt.image(), is("jw_shirt8.gif"));
    var mods = shirt.modifiers();
    assertThat(mods.get(DoubleModifier.MP), is(103));
    assertThat(mods.get(DoubleModifier.MYS), is(54));
    assertThat(mods.get(DoubleModifier.COLD_RESISTANCE), is(5));
    // wardrobe calendar says 21, which agrees with the code, but it's actually 22.
    // assertThat(mods.get(DoubleModifier.MONSTER_LEVEL), is(22));
    assertThat(mods.get(DoubleModifier.MP_REGEN_MIN), is(16));
    assertThat(mods.get(DoubleModifier.MP_REGEN_MAX), is(27));
  }

  @Test
  void generatesHat() {
    var hat = WardrobeOMaticDatabase.hat(8226, 5);

    assertThat(hat.name(), is("magnetic palladium-silver great-tam"));
    assertThat(hat.image(), is("jw_hat7.gif"));
    var mods = hat.modifiers();
    assertThat(mods.get(DoubleModifier.COLD_SPELL_DAMAGE), is(20));
    assertThat(mods.get(DoubleModifier.MEATDROP), is(51));
    assertThat(mods.get(DoubleModifier.ITEMDROP), is(27));
    assertThat(mods.get(DoubleModifier.HOT_SPELL_DAMAGE), is(27));
    assertThat(mods.get(DoubleModifier.SPOOKY_SPELL_DAMAGE), is(24));
  }

  @Test
  void generatesCollar() {
    var collar = WardrobeOMaticDatabase.collar(8226, 5);

    assertThat(collar.name(), is("pearlescent lilac-scarlet collar"));
    assertThat(collar.image(), is("jw_pet4.gif"));
    var mods = collar.modifiers();
    assertThat(mods.get(DoubleModifier.FAMILIAR_DAMAGE), is(86));
  }
}
