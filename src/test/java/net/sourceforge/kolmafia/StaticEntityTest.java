package net.sourceforge.kolmafia;

import static internal.helpers.Player.withUserInterface;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.ui.UserInterface;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StaticEntityTest {
  private static class RecordingUserInterface implements UserInterface {
    final List<String> shown = new ArrayList<>();

    @Override
    public void showDescription(final GenericRequest request) {
      this.shown.add(request.getURLString());
    }

    @Override
    public void showPlayerProfile(final String player) {
      this.shown.add("profile " + player);
    }
  }

  @Nested
  class GameDescriptions {
    @Test
    void showsItemDescriptions() {
      var ui = new RecordingUserInterface();

      try (var cleanups = withUserInterface(ui)) {
        StaticEntity.showGameDescription(ItemPool.get(ItemPool.SEAL_TOOTH));

        assertThat(
            ui.shown,
            contains(
                "desc_item.php?whichitem=" + ItemDatabase.getDescriptionId(ItemPool.SEAL_TOOTH)));
      }
    }

    @Test
    void showsEffectDescriptions() {
      var ui = new RecordingUserInterface();

      try (var cleanups = withUserInterface(ui)) {
        StaticEntity.showGameDescription(EffectPool.get(EffectPool.LEASH_OF_LINGUINI));

        assertThat(
            ui.shown,
            contains(
                "desc_effect.php?whicheffect="
                    + EffectDatabase.getDescriptionId(EffectPool.LEASH_OF_LINGUINI)));
      }
    }

    @Test
    void showsSkillDescriptions() {
      var ui = new RecordingUserInterface();

      try (var cleanups = withUserInterface(ui)) {
        StaticEntity.showGameDescription(UseSkillRequest.getInstance(SkillPool.LEASH_OF_LINGUINI));

        assertThat(
            ui.shown,
            contains("desc_skill.php?whichskill=" + SkillPool.LEASH_OF_LINGUINI + "&self=true"));
      }
    }

    @Test
    void showsPlayerProfiles() {
      var ui = new RecordingUserInterface();

      try (var cleanups = withUserInterface(ui)) {
        StaticEntity.showGameDescription("Easyfax (#2504737)");

        assertThat(ui.shown, contains("profile #2504737"));
      }
    }

    @Test
    void ignoresStringsWithoutAPlayer() {
      var ui = new RecordingUserInterface();

      try (var cleanups = withUserInterface(ui)) {
        StaticEntity.showGameDescription("just some text");

        assertThat(ui.shown, empty());
      }
    }
  }
}
