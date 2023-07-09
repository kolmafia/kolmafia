package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ConsequenceManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("ConsequenceManager");
    Preferences.reset("ConsequenceManager");
  }

  @Nested
  class IntegerPreferences {
    @Test
    public void canParseDescItemIntegerPreference() {
      var cleanups = new Cleanups(withProperty("boneAbacusVictories", "0"));

      try (cleanups) {
        var descid = ItemDatabase.getDescriptionId(ItemPool.BONE_ABACUS);
        var responseText = html("request/test_consequences_bone_abacus.html");

        // You have defeated 1,000 opponents while holding this abacus.
        ConsequenceManager.parseItemDesc(descid, responseText);
        assertThat("boneAbacusVictories", isSetTo("1000"));
      }
    }

    @Test
    public void canParseQuestLogIntegerPreference() {
      var cleanups = new Cleanups(withProperty("elfGratitude", "0"));

      try (cleanups) {
        var request = new GenericRequest("questlog.php?which=3");
        request.responseText = html("request/test_consequences_elf_gratitude.html");

        // You earned 10,000 Elf Gratitude during Crimbo 2022.
        QuestManager.handleQuestChange(request);
        assertThat("elfGratitude", isSetTo("10000"));
      }
    }
  }

  @Nested
  class MonkeyPaw {
    @Test
    public void canParsePoint() {
      var cleanups = new Cleanups(withProperty("monkeyPointMonster", ""));

      try (cleanups) {
        var descid = ItemDatabase.getDescriptionId(ItemPool.CURSED_MONKEY_PAW);
        var responseText = html("request/test_consequences_cursed_monkey_point.html");

        ConsequenceManager.parseItemDesc(descid, responseText);
        assertThat("monkeyPointMonster", isSetTo("BRICKO ooze"));
      }
    }

    @Test
    public void canParsePointWithBanish() {
      var cleanups = new Cleanups(withProperty("monkeyPointMonster", ""));

      try (cleanups) {
        var descid = ItemDatabase.getDescriptionId(ItemPool.CURSED_MONKEY_PAW);
        var responseText = html("request/test_consequences_cursed_monkey_point_banish.html");

        ConsequenceManager.parseItemDesc(descid, responseText);
        assertThat("monkeyPointMonster", isSetTo("BRICKO ooze"));
      }
    }
  }

  @Test
  public void canParseCitizenOfAZone() {
    var cleanups = new Cleanups(withProperty("_citizenZone"), withProperty("_citizenZoneMods"));

    try (cleanups) {
      var descid = EffectDatabase.getDescriptionId(EffectPool.CITIZEN_OF_A_ZONE);
      var responseText = html("request/test_desc_effect_citizen_of_a_zone.html");

      ConsequenceManager.parseEffectDesc(descid, responseText);
      assertThat(Preferences.getString("_citizenZone"), equalTo("A Mob of Zeppelin Protesters"));
      assertThat(
          Preferences.getString("_citizenZoneMods"),
          equalTo(
              "Hot Resistance: +4, Cold Resistance: +4, Sleaze Resistance: +4, Stench Resistance: +4, Spooky Resistance: +4, Sleaze Damage: +10, Sleaze Spell Damage: +10, Muscle: +10"));
    }
  }
}
