package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPostChoice1;
import static internal.helpers.Player.withPostChoice2;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChoiceControlTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("ChoiceControlTest");
    KoLCharacter.reset(true);
    Preferences.saveSettingsToFile = false;
  }

  @Nested
  class SuburbsOfDisTest {
    @BeforeEach
    public void beforeEach() {
      QuestDatabase.setQuest(Quest.CLUMSINESS, QuestDatabase.UNSTARTED);
      QuestDatabase.setQuest(Quest.GLACIER, QuestDatabase.UNSTARTED);
      QuestDatabase.setQuest(Quest.MAELSTROM, QuestDatabase.UNSTARTED);
    }

    @Test
    public void foreshadowingDemonStartsClumsinessQuest() {
      var cleanups = withPostChoice2(560, 1);

      try (cleanups) {
        assertThat(Quest.CLUMSINESS, isStep(QuestDatabase.STARTED));
      }
    }

    @ParameterizedTest
    @CsvSource({"1, The Thorax", "2, The Bat in the Spats"})
    public void chooseYourDestructionSetsBoss(String decision, String monster) {
      var cleanups = withPostChoice2(561, Integer.parseInt(decision));

      try (cleanups) {
        assertThat(Quest.CLUMSINESS, isStep(1));
        assertThat("clumsinessGroveBoss", isSetTo(monster));
      }
    }

    @ParameterizedTest
    @CsvSource({
      ItemPool.VANITY_STONE + ", The Thorax",
      ItemPool.FURIOUS_STONE + ", The Bat in the Spats",
    })
    public void testOfYourMettleSetsNextBoss(String stoneId, String monster) {
      var cleanups = new Cleanups(withItem(Integer.parseInt(stoneId)));

      try (cleanups) {
        var choice = withPostChoice2(563, 1);
        try (choice) {
          assertThat("clumsinessGroveBoss", isSetTo(monster));
          assertThat(Quest.CLUMSINESS, isStep(3));
        }
      }
    }

    @Test
    public void maelstromOfTroubleStartsMaelstromQuest() {
      var cleanups = withPostChoice2(564, 1);

      try (cleanups) {
        assertThat(Quest.MAELSTROM, isStep(QuestDatabase.STARTED));
      }
    }

    @ParameterizedTest
    @CsvSource({"1, The Terrible Pinch", "2, Thug 1 and Thug 2"})
    public void getGropedOrGetMuggedSetsBoss(String decision, String monster) {
      var cleanups = withPostChoice2(565, Integer.parseInt(decision));

      try (cleanups) {
        assertThat(Quest.MAELSTROM, isStep(1));
        assertThat("maelstromOfLoversBoss", isSetTo(monster));
      }
    }

    @ParameterizedTest
    @CsvSource({
      ItemPool.JEALOUSY_STONE + ", The Terrible Pinch",
      ItemPool.LECHEROUS_STONE + ", Thug 1 and Thug 2",
    })
    public void choiceToBeMadeSetsNextBoss(String stoneId, String monster) {
      var cleanups = new Cleanups(withItem(Integer.parseInt(stoneId)));

      try (cleanups) {
        var choice = withPostChoice2(566, 1);
        try (choice) {
          assertThat("maelstromOfLoversBoss", isSetTo(monster));
          assertThat(Quest.MAELSTROM, isStep(3));
        }
      }
    }

    @Test
    public void mayBeOnThinIceStartsGlacierQuest() {
      var cleanups = withPostChoice2(567, 1);

      try (cleanups) {
        assertThat(Quest.GLACIER, isStep(QuestDatabase.STARTED));
      }
    }

    @ParameterizedTest
    @CsvSource({"1, Mammon the Elephant", "2, The Large-Bellied Snitch"})
    public void someSoundsMostUnnervingSetsBoss(String decision, String monster) {
      var cleanups = withPostChoice2(568, Integer.parseInt(decision));

      try (cleanups) {
        assertThat(Quest.GLACIER, isStep(1));
        assertThat("glacierOfJerksBoss", isSetTo(monster));
      }
    }

    @ParameterizedTest
    @CsvSource({
      ItemPool.GLUTTONOUS_STONE + ", Mammon the Elephant",
      ItemPool.AVARICE_STONE + ", The Large-Bellied Snitch",
    })
    public void oneMoreDemonToSlaySetsBoss(String stoneId, String monster) {
      var cleanups = new Cleanups(withItem(Integer.parseInt(stoneId)));

      try (cleanups) {
        var choice = withPostChoice2(569, 1);
        try (choice) {
          assertThat("glacierOfJerksBoss", isSetTo(monster));
          assertThat(Quest.GLACIER, isStep(3));
        }
      }
    }
  }

  @Nested
  class ColdMedicineCabinet {
    @ParameterizedTest
    @CsvSource({"ice_crown, 0", "frozen_jeans, 1", "ice_wrap, 2"})
    void seeingEquipmentCorrectsTotalTakenToday(String itemSlug, int impliedEquipmentTaken) {
      var cleanups = new Cleanups(withProperty("_coldMedicineEquipmentTaken", -1));

      try (cleanups) {
        var urlString = "choice.php?forceoption=0";
        var responseText = html("request/test_choice_cmc_" + itemSlug + ".html");
        var request = new GenericRequest(urlString);
        request.responseText = responseText;
        ChoiceManager.preChoice(request);
        request.processResponse();

        assertThat("_coldMedicineEquipmentTaken", isSetTo(impliedEquipmentTaken));
      }
    }

    @Test
    void takingEquipmentIncrementsCounter() {
      var cleanups =
          new Cleanups(withProperty("_coldMedicineEquipmentTaken", 0), withPostChoice1(1455, 1));

      try (cleanups) {
        assertThat("_coldMedicineEquipmentTaken", isSetTo(1));
      }
    }
  }
}
