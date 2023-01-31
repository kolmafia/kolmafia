package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
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
}
