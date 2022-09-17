package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class LimitModeTest {
  @Test
  void nameOfNoneIsBlank() {
    assertThat(LimitMode.NONE.getName(), is(""));
  }

  @Test
  void canFindByName() {
    assertThat(LimitMode.find("batman"), is(LimitMode.BATMAN));
  }

  @Test
  void findsUnknownWithUnknownName() {
    assertThat(LimitMode.find("gausie mode engage!"), is(LimitMode.UNKNOWN));
  }

  @Test
  void requiresReset() {
    assertThat(LimitMode.SPELUNKY.requiresReset(), is(true));
    assertThat(LimitMode.BATMAN.requiresReset(), is(true));
    assertThat(LimitMode.ED.requiresReset(), is(false));
  }

  @Nested
  class Skills {
    @Test
    void noLimitModeNoSkillsLimited() {
      assertThat(LimitMode.NONE.limitSkill(95), is(false));
    }

    @Test
    void spelunkyLimitsNonSpelunkySkills() {
      assertThat(LimitMode.SPELUNKY.limitSkill(7001), is(true));
      assertThat(LimitMode.SPELUNKY.limitSkill(7240), is(false));
    }

    @Test
    void batmanLimitsSkills() {
      assertThat(LimitMode.BATMAN.limitSkill(UseSkillRequest.getInstance(95)), is(true));
      assertThat(LimitMode.BATMAN.limitSkill(95), is(true));
    }
  }

  @Nested
  class Items {
    @Test
    void nothingLimitedNormally() {
      assertThat(LimitMode.NONE.limitItem(1), is(false));
    }

    @Test
    void everythingLimitedInEd() {
      assertThat(LimitMode.ED.limitItem(1), is(true));
    }

    @Test
    void itemsLimitedInSpelunky() {
      assertThat(LimitMode.SPELUNKY.limitItem(8030), is(true));
      assertThat(LimitMode.SPELUNKY.limitItem(8045), is(false));
    }

    @Test
    void itemsLimitedInBatman() {
      assertThat(LimitMode.BATMAN.limitItem(8790), is(true));
      assertThat(LimitMode.BATMAN.limitItem(8801), is(false));
      // In the range but not included (ROM of Optimality PvP reward)
      assertThat(LimitMode.BATMAN.limitItem(8800), is(true));
    }
  }

  @Nested
  class Slot {
    @Test
    void nothingLimitedNormally() {
      assertThat(LimitMode.NONE.limitSlot(EquipmentManager.HAT), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"BATMAN", "ED"})
    void someCantWearAnything(final LimitMode lm) {
      assertThat(lm.limitSlot(EquipmentManager.HAT), is(true));
    }

    @Test
    void spelunkersCanWearSomeThings() {
      assertThat(LimitMode.SPELUNKY.limitSlot(EquipmentManager.ACCESSORY1), is(false));
      assertThat(LimitMode.SPELUNKY.limitSlot(EquipmentManager.ACCESSORY2), is(true));
    }
  }

  @Nested
  class Outfits {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED"})
    void someDontLimitOutfits(final LimitMode lm) {
      assertThat(lm.limitOutfits(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitOutfits(final LimitMode lm) {
      assertThat(lm.limitOutfits(), is(true));
    }
  }

  @Nested
  class Familiars {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED"})
    void someDontLimitFamiliars(final LimitMode lm) {
      assertThat(lm.limitFamiliars(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitFamiliars(final LimitMode lm) {
      assertThat(lm.limitFamiliars(), is(true));
    }
  }

  @Nested
  class Adventures {
    @Test
    void mostZonesUnlimitedNormally() {
      var adventure = AdventureDatabase.getAdventureByName("The Dire Warren");
      assertThat(LimitMode.NONE.limitAdventure(adventure), is(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"The Mean Streets", "The Mines"})
    void cantAccessLimitZonesInUnlimited(final String adventureName) {
      var adventure = AdventureDatabase.getAdventureByName(adventureName);
      assertThat(LimitMode.NONE.limitAdventure(adventure), is(true));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "UNKNOWN"},
        mode = EnumSource.Mode.EXCLUDE)
    void cantAccessRegularZonesInLimitModes(final LimitMode lm) {
      var adventure = AdventureDatabase.getAdventureByName("The Dire Warren");
      assertThat(lm.limitAdventure(adventure), is(true));
    }
  }

  @Nested
  class Meat {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED"})
    void someDontLimitMeat(final LimitMode lm) {
      assertThat(lm.limitMeat(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitMeat(final LimitMode lm) {
      assertThat(lm.limitMeat(), is(true));
    }
  }

  @Nested
  class Mall {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED"})
    void someDontLimitMall(final LimitMode lm) {
      assertThat(lm.limitMall(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitMall(final LimitMode lm) {
      assertThat(lm.limitMall(), is(true));
    }
  }

  @Nested
  class NPCStores {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE"})
    void someDontLimitNPCStores(final LimitMode lm) {
      assertThat(lm.limitNPCStores(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitNPCStores(final LimitMode lm) {
      assertThat(lm.limitNPCStores(), is(true));
    }
  }

  @Nested
  class Coinmasters {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED"})
    void someDontLimitCoinmasters(final LimitMode lm) {
      assertThat(lm.limitCoinmasters(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitCoinmasters(final LimitMode lm) {
      assertThat(lm.limitCoinmasters(), is(true));
    }
  }

  @Nested
  class Clan {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE"})
    void someDontLimitClan(final LimitMode lm) {
      assertThat(lm.limitClan(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitClan(final LimitMode lm) {
      assertThat(lm.limitClan(), is(true));
    }
  }

  @Nested
  class Campground {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE"})
    void someDontLimitCampground(final LimitMode lm) {
      assertThat(lm.limitCampground(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitCampground(final LimitMode lm) {
      assertThat(lm.limitCampground(), is(true));
    }
  }

  @Nested
  class Storage {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED"})
    void someDontLimitStorage(final LimitMode lm) {
      assertThat(lm.limitStorage(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitStorage(final LimitMode lm) {
      assertThat(lm.limitStorage(), is(true));
    }
  }

  @Nested
  class Eating {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE"})
    void someDontLimitEating(final LimitMode lm) {
      assertThat(lm.limitEating(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitEating(final LimitMode lm) {
      assertThat(lm.limitEating(), is(true));
    }
  }

  @Nested
  class Drinking {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE"})
    void someDontLimitDrinking(final LimitMode lm) {
      assertThat(lm.limitDrinking(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitDrinking(final LimitMode lm) {
      assertThat(lm.limitDrinking(), is(true));
    }
  }

  @Nested
  class Spleening {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE"})
    void someDontLimitSpleening(final LimitMode lm) {
      assertThat(lm.limitSpleening(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitSpleening(final LimitMode lm) {
      assertThat(lm.limitSpleening(), is(true));
    }
  }

  @Nested
  class Pickpocket {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED"})
    void someDontLimitPickpocket(final LimitMode lm) {
      assertThat(lm.limitPickpocket(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitPickpocket(final LimitMode lm) {
      assertThat(lm.limitPickpocket(), is(true));
    }
  }

  @Nested
  class MCD {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED"})
    void someDontLimitMCD(final LimitMode lm) {
      assertThat(lm.limitMCD(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitMCD(final LimitMode lm) {
      assertThat(lm.limitMCD(), is(true));
    }
  }
}
