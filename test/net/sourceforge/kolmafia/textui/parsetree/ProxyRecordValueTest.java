package net.sourceforge.kolmafia.textui.parsetree;

import static internal.helpers.Player.withAdventuresSpent;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Utilities.deleteSerFiles;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.textui.DataTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ProxyRecordValueTest {
  private static final String TESTUSER = "ProxyRecordValueTestUser";

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset(TESTUSER);
    AdventureSpentDatabase.resetTurns();
  }

  @AfterAll
  static void afterAll() {
    deleteSerFiles(TESTUSER);
  }

  @Nested
  class LocationProxy {
    @Test
    public void fieldsWorkWithoutContent() {
      ProxyRecordValue.LocationProxy location =
          new ProxyRecordValue.LocationProxy(DataTypes.LOCATION_INIT);

      assertThat(location.get_id(), is(-1));
      assertThat(location.get_nocombats(), is(false));
      assertThat(location.get_combat_percent(), is(0.0));
      assertThat(location.get_zone(), is(""));
      assertThat(location.get_parent(), is(""));
      assertThat(location.get_parentdesc(), is(""));
      assertThat(location.get_root(), is(""));
      assertThat(location.get_difficulty_level(), is(""));
      assertThat(location.get_environment(), is("none"));
      assertThat(location.get_fire_level(), is(0));
      assertThat(location.get_bounty(), is(DataTypes.BOUNTY_INIT));
      assertThat(location.get_combat_queue(), is(""));
      assertThat(location.get_noncombat_queue(), is(""));
      assertThat(location.get_turns_spent(), is(0));
      assertThat(location.get_last_noncombat_turns_spent(), is(-1));
      assertThat(location.get_force_noncombat(), is(-1));
      assertThat(location.get_kisses(), is(0));
      assertThat(location.get_recommended_stat(), is(0));
      assertThat(location.get_poison(), is(Integer.MAX_VALUE));
      assertThat(location.get_water_level(), is(0));
      assertThat(location.get_wanderers(), is(false));
      assertThat(location.get_pledge_allegiance(), is(""));
    }

    @Test
    public void fieldsWorkWithContent() {
      ProxyRecordValue.LocationProxy location =
          new ProxyRecordValue.LocationProxy(
              DataTypes.makeLocationValue(
                  AdventureDatabase.getAdventure(AdventurePool.HAUNTED_BILLIARDS_ROOM)));

      var cleanups =
          new Cleanups(
              withAdventuresSpent(AdventurePool.HAUNTED_BILLIARDS_ROOM, 5),
              withProperty("lastNoncombat" + AdventurePool.HAUNTED_BILLIARDS_ROOM, 3));
      try (cleanups) {
        assertThat(location.get_id(), is(AdventurePool.HAUNTED_BILLIARDS_ROOM));
        assertThat(location.get_nocombats(), is(false));
        assertThat(location.get_combat_percent(), is(85.0));
        assertThat(location.get_zone(), is("Manor1"));
        assertThat(location.get_parent(), is("Manor"));
        assertThat(location.get_parentdesc(), is("Spookyraven Manor"));
        assertThat(location.get_root(), is("Town"));
        assertThat(location.get_difficulty_level(), is("mid"));
        assertThat(location.get_environment(), is("indoor"));
        assertThat(location.get_fire_level(), is(0));
        assertThat(location.get_bounty(), is(DataTypes.BOUNTY_INIT));
        assertThat(location.get_combat_queue(), is(""));
        assertThat(location.get_noncombat_queue(), is(""));
        assertThat(location.get_turns_spent(), is(5));
        assertThat(location.get_last_noncombat_turns_spent(), is(3));
        assertThat(location.get_force_noncombat(), is(10));
        assertThat(location.get_kisses(), is(0));
        assertThat(location.get_recommended_stat(), is(20));
        assertThat(location.get_poison(), is(Integer.MAX_VALUE));
        assertThat(location.get_water_level(), is(0));
        assertThat(location.get_wanderers(), is(true));
        assertThat(
            location.get_pledge_allegiance(),
            is(
                "Item Drop: 15, Meat Drop: 25, Stench Damage: 10, Stench Spell Damage: 10, Mysticality Percent: 10"));
      }
    }
  }
}
