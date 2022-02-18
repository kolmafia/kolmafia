package net.sourceforge.kolmafia.swingui.panel;

import static internal.helpers.Player.addCampgroundItem;
import static internal.helpers.Player.addItem;
import static internal.helpers.Player.hasFamiliar;
import static internal.helpers.Player.inPath;
import static internal.helpers.Player.isClass;
import static internal.helpers.Player.isHardcore;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.panel.DailyDeedsPanel.FreeFightsDaily;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DailyDeedsPanelTest {
  @BeforeAll
  public static void beforeAll() {
    Preferences.saveSettingsToFile = false;
    KoLCharacter.reset("fakeUserName");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("fakeUsername");
  }

  @Nested
  class FreeFights {
    @Test
    public void hardcoreNoItomsHasOnlyTentacle() {
      var ff = new FreeFightsDaily();
      var cleanups = isHardcore();
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), is("<html>Fights: tentacle</html>"));
      }
    }

    @Test
    public void panelCountsFights() {
      var ff = new FreeFightsDaily();
      ff.update();
      assertThat(ff.getText(), containsString("0/10 BRICKO"));
      Preferences.setInteger("_brickoFights", 5);
      ff.update();
      assertThat(ff.getText(), containsString("5/10 BRICKO"));
    }

    @Test
    public void crossesMultipleLinesWithMoreThanFourEntries() {
      var ff = new FreeFightsDaily();
      var cleanups =
          new Cleanups(
              hasFamiliar(FamiliarPool.HIPSTER),
              hasFamiliar(FamiliarPool.ARTISTIC_GOTH_KID),
              hasFamiliar(FamiliarPool.GOD_LOBSTER),
              hasFamiliar(FamiliarPool.MACHINE_ELF));
      try (cleanups) {
        ff.update();
        assertThat(
            ff.getText(),
            is(
                "<html>Fights: 0/3 lynyrd, 0/10 BRICKO, 0/7 hipster+goth, 0/5 machine elf<br>Fights: 0/3 god lobster, tentacle</html>"));
      }
    }

    @Test
    public void showsHipsterFights() {
      var ff = new FreeFightsDaily();
      var cleanups = hasFamiliar(FamiliarPool.HIPSTER);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/7 hipster"));
      }
    }

    @Test
    public void showsGothKidFights() {
      var ff = new FreeFightsDaily();
      var cleanups = hasFamiliar(FamiliarPool.ARTISTIC_GOTH_KID);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/7 goth"));
      }
    }

    @Test
    public void showsSeals() {
      var ff = new FreeFightsDaily();
      var cleanups = isClass(AscensionClass.SEAL_CLUBBER);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/5 seals"));
      }
    }

    @Test
    public void summonTenSealsWithInfernalClaw() {
      var ff = new FreeFightsDaily();
      var cleanups =
          new Cleanups(isClass(AscensionClass.SEAL_CLUBBER), addItem(ItemPool.INFERNAL_SEAL_CLAW));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/10 seals"));
      }
    }

    @Test
    public void showsMachineElf() {
      var ff = new FreeFightsDaily();
      var cleanups = hasFamiliar(FamiliarPool.MACHINE_ELF);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/5 machine elf"));
      }
    }

    @Test
    public void showsSnojo() {
      var ff = new FreeFightsDaily();
      Preferences.setBoolean("snojoAvailable", true);
      ff.update();
      assertThat(ff.getText(), containsString("0/10 snojo"));
    }

    @Test
    public void showsWitchess() {
      var ff = new FreeFightsDaily();
      var cleanups = addCampgroundItem(ItemPool.WITCHESS_SET);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/5 witchess"));
      }
    }

    @Test
    public void showsGodLobster() {
      var ff = new FreeFightsDaily();
      var cleanups = hasFamiliar(FamiliarPool.GOD_LOBSTER);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/3 god lobster"));
      }
    }

    @Test
    public void showsParty() {
      var ff = new FreeFightsDaily();
      Preferences.setBoolean("_neverendingPartyToday", true);
      ff.update();
      assertThat(ff.getText(), containsString("0/10 party"));
    }

    @Test
    public void showsVote() {
      var ff = new FreeFightsDaily();
      Preferences.setBoolean("_voteToday", true);
      ff.update();
      assertThat(ff.getText(), containsString("0/3 vote"));
    }

    @Test
    public void showsLynyrd() {
      var ff = new FreeFightsDaily();
      var cleanups = new Cleanups(addItem(ItemPool.LYNYRD_SNARE));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/3 lynyrd"));
      }
    }

    @Test
    public void showsKramco() {
      var ff = new FreeFightsDaily();
      var cleanups = new Cleanups(addItem(ItemPool.SAUSAGE_O_MATIC));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0 sausage goblin"));
      }
    }

    @Test
    public void showsGlitchMonster() {
      var ff = new FreeFightsDaily();
      var cleanups = new Cleanups(addItem(ItemPool.GLITCH_ITEM));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("%monster%"));
      }
    }

    @Test
    public void showsMushroom() {
      var ff = new FreeFightsDaily();
      var cleanups = addCampgroundItem(ItemPool.MUSHROOM_SPORES);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/1 piranha plant"));
      }
    }

    @Test
    public void showsMushroomGivingFiveFightsInPlumber() {
      var ff = new FreeFightsDaily();
      var cleanups =
          new Cleanups(
              addCampgroundItem(ItemPool.MUSHROOM_SPORES), inPath(Path.PATH_OF_THE_PLUMBER));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/5 piranha plant"));
      }
    }

    @Test
    public void showsVoid() {
      var ff = new FreeFightsDaily();
      var cleanups = new Cleanups(addItem(ItemPool.CURSED_MAGNIFYING_GLASS));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/5 void"));
      }
    }
  }
}
