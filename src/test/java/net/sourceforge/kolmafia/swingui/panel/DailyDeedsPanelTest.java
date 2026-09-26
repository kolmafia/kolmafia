package net.sourceforge.kolmafia.swingui.panel;

import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.panel.DailyDeedsPanel.AdvsDaily;
import net.sourceforge.kolmafia.swingui.panel.DailyDeedsPanel.FreeFightsDaily;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DailyDeedsPanelTest {

  private static final String TESTUSERNAME = "DailyDeedsPanelTestUser";

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset(TESTUSERNAME);
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset(TESTUSERNAME);
  }

  @Nested
  class FreeFights {
    private static final FreeFightsDaily ff = new FreeFightsDaily();

    @Test
    public void hardcoreNoItomsHasOnlyTentacle() {
      var cleanups = withHardcore();
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), is("<html>Fights: tentacle</html>"));
      }
    }

    @Test
    public void panelCountsFights() {
      ff.update();
      assertThat(ff.getText(), containsString("0/10 BRICKO"));
      Preferences.setInteger("_brickoFights", 5);
      ff.update();
      assertThat(ff.getText(), containsString("5/10 BRICKO"));
    }

    @Test
    public void crossesMultipleLinesWithMoreThanFourEntries() {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.HIPSTER),
              withFamiliarInTerrarium(FamiliarPool.ARTISTIC_GOTH_KID),
              withFamiliarInTerrarium(FamiliarPool.GOD_LOBSTER),
              withFamiliarInTerrarium(FamiliarPool.MACHINE_ELF));
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
      var cleanups = withFamiliarInTerrarium(FamiliarPool.HIPSTER);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/7 hipster"));
      }
    }

    @Test
    public void showsGothKidFights() {
      var cleanups = withFamiliarInTerrarium(FamiliarPool.ARTISTIC_GOTH_KID);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/7 goth"));
      }
    }

    @Test
    public void showsSeals() {
      var cleanups = withClass(AscensionClass.SEAL_CLUBBER);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/5 seals"));
      }
    }

    @Test
    public void summonTenSealsWithInfernalClaw() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.SEAL_CLUBBER), withItem(ItemPool.INFERNAL_SEAL_CLAW));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/10 seals"));
      }
    }

    @Test
    public void showsMachineElf() {
      var cleanups = withFamiliarInTerrarium(FamiliarPool.MACHINE_ELF);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/5 machine elf"));
      }
    }

    @Test
    public void showsSnojo() {
      Preferences.setBoolean("snojoAvailable", true);
      ff.update();
      assertThat(ff.getText(), containsString("0/10 snojo"));
    }

    @Test
    public void showsWitchess() {
      var cleanups = withCampgroundItem(ItemPool.WITCHESS_SET);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/5 witchess"));
      }
    }

    @Test
    public void showsGodLobster() {
      var cleanups = withFamiliarInTerrarium(FamiliarPool.GOD_LOBSTER);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/3 god lobster"));
      }
    }

    @Test
    public void showsParty() {
      Preferences.setBoolean("_neverendingPartyToday", true);
      ff.update();
      assertThat(ff.getText(), containsString("0/10 party"));
    }

    @Test
    public void showsVote() {
      Preferences.setBoolean("_voteToday", true);
      ff.update();
      assertThat(ff.getText(), containsString("0/3 vote"));
    }

    @Test
    public void showsLynyrd() {
      var cleanups = new Cleanups(withItem(ItemPool.LYNYRD_SNARE));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/3 lynyrd"));
      }
    }

    @Test
    public void showsKramco() {
      var cleanups = new Cleanups(withItem(ItemPool.SAUSAGE_O_MATIC));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0 sausage goblin"));
      }
    }

    @Test
    public void showsGlitchMonster() {
      var cleanups = new Cleanups(withItem(ItemPool.GLITCH_ITEM));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("%monster%"));
      }
    }

    @Test
    public void showsMushroom() {
      var cleanups = withCampgroundItem(ItemPool.MUSHROOM_SPORES);
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/1 piranha plant"));
      }
    }

    @Test
    public void showsMushroomGivingFiveFightsInPlumber() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.MUSHROOM_SPORES), withPath(Path.PATH_OF_THE_PLUMBER));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/5 piranha plant"));
      }
    }

    @Test
    public void showsVoid() {
      var cleanups = new Cleanups(withItem(ItemPool.CURSED_MAGNIFYING_GLASS));
      try (cleanups) {
        ff.update();
        assertThat(ff.getText(), containsString("0/5 void"));
      }
    }
  }

  @Nested
  class CombatLoversLocket {
    @Test
    public void showsLocket() {
      var cld = new DailyDeedsPanel.CombatLocketDaily();
      var cleanups = withItem(ItemPool.COMBAT_LOVERS_LOCKET);
      try (cleanups) {
        cld.update();
        assertThat(cld.getText(), containsString("0/3 locket"));
      }
    }

    @Test
    public void showsLocket1Monster() {
      var cld = new DailyDeedsPanel.CombatLocketDaily();
      var cleanups =
          new Cleanups(
              withItem(ItemPool.COMBAT_LOVERS_LOCKET), withProperty("_locketMonstersFought", "1"));
      try (cleanups) {
        cld.update();
        assertThat(cld.getText(), containsString("1/3 locket: spooky vampire"));
      }
    }

    @Test
    public void showsLocket3Monsters() {
      var cld = new DailyDeedsPanel.CombatLocketDaily();
      var cleanups =
          new Cleanups(
              withItem(ItemPool.COMBAT_LOVERS_LOCKET),
              withProperty("_locketMonstersFought", "1,11,111"));
      try (cleanups) {
        cld.update();
        assertThat(
            cld.getText(),
            containsString("3/3 locket: spooky vampire, Gnollish Flyslayer, scary clown"));
      }
    }
  }

  @Nested
  class DropsFamiliars {

    @BeforeEach
    public void beforeEach() {
      FamiliarData.reset();
    }

    @Test
    public void showsCookbookbat() {
      var dd = new DailyDeedsPanel.DropsDaily();
      var cleanups = withFamiliarInTerrarium(FamiliarPool.COOKBOOKBAT);
      try (cleanups) {
        dd.update();
        assertThat(dd.getText(), containsString("0/1 cookbookbat recipe"));
      }
    }

    @Test
    public void cookbookbatRecipeDrop() {
      var dd = new DailyDeedsPanel.DropsDaily();
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.COOKBOOKBAT),
              withProperty("_cookbookbatRecipeDrops", true));
      try (cleanups) {
        dd.update();
        assertThat(dd.getText(), containsString("1/1 cookbookbat recipe"));
      }
    }
  }

  @Nested
  class AdvsGained {
    private static final AdvsDaily advs = new AdvsDaily();

    @Test
    public void showsMafiaThumbRing() {
      advs.update();
      assertFalse(advs.isVisible());
      assertThat(advs.getText(), not(containsString(" thumb ring")));
      Preferences.setInteger("_mafiaThumbRingAdvs", 5);
      advs.update();
      assertThat(advs.getText(), containsString("Advs: 5 thumb ring"));
      assertTrue(advs.isVisible());
    }

    @Test
    public void showsPottedPlant() {
      advs.update();
      assertFalse(advs.isVisible());
      assertThat(advs.getText(), not(containsString(" potted plant")));
      Preferences.setInteger("_carnivorousPottedPlantWins", 5);
      advs.update();
      assertThat(advs.getText(), containsString("Advs: 5 potted plant"));
      assertTrue(advs.isVisible());
    }

    @Test
    public void showsBatWings() {
      advs.update();
      assertFalse(advs.isVisible());
      assertThat(advs.getText(), not(containsString(" bat wings")));
      Preferences.setInteger("_batWingsFreeFights", 5);
      advs.update();
      assertThat(advs.getText(), containsString("Advs: 5 bat wings"));
      assertTrue(advs.isVisible());
    }

    @Test
    public void showsTimeHelmetAndPottedPlant() {
      advs.update();
      assertFalse(advs.isVisible());
      assertThat(advs.getText(), not(containsString(" time helmet")));
      assertThat(advs.getText(), not(containsString(" potted plant")));
      Preferences.setInteger("_carnivorousPottedPlantWins", 5);
      Preferences.setInteger("_timeHelmetAdv", 5);
      advs.update();
      assertThat(advs.getText(), containsString("Advs: 5 time helmet, 5 potted plant"));
      assertTrue(advs.isVisible());
    }
  }
}
