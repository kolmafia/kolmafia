package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPostChoice1;
import static internal.helpers.Player.withPostChoice2;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
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

  @Nested
  class StillSuit {
    @Test
    void canReadInformationFromChoicePage() {
      var cleanups = new Cleanups(withProperty("familiarSweat"));

      try (cleanups) {
        var urlString = "choice.php?forceoption=0";
        var responseText = html("request/test_choice_stillsuit.html");
        var request = new GenericRequest(urlString);
        request.responseText = responseText;
        ChoiceManager.preChoice(request);
        request.processResponse();

        assertThat("familiarSweat", isSetTo(81));
        assertThat(
            "nextDistillateMods",
            isSetTo(
                "Experience (Muscle): +5, Experience (Moxie): +4, Spooky Damage: +15, Spooky Spell Damage: +25"));
      }
    }

    @Test
    void canReadInformationFromChoicePageWhenLessThan10Drams() {
      var cleanups = new Cleanups(withProperty("familiarSweat"));

      try (cleanups) {
        var urlString = "choice.php?forceoption=0";
        var responseText = html("request/test_choice_stillsuit_sub_10.html");
        var request = new GenericRequest(urlString);
        request.responseText = responseText;
        ChoiceManager.preChoice(request);
        request.processResponse();

        assertThat("familiarSweat", isSetTo(3));
        assertThat("nextDistillateMods", isSetTo(""));
      }
    }

    @Test
    void noNextDistillateModsWhenLessThan10Dram() {
      var cleanups = new Cleanups(withProperty("nextDistillateMods", "Cold Resistance: +1"));

      try (cleanups) {
        RequestLoggerOutput.startStream();
        var urlString = "choice.php?forceoption=0";
        var responseText = html("request/test_choice_stillsuit_sub_10.html");
        var request = new GenericRequest(urlString);
        request.responseText = responseText;
        ChoiceManager.preChoice(request);
        request.processResponse();

        assertThat("nextDistillateMods", isSetTo(""));
      }
    }

    @Test
    void canDrinkSweat() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, html("request/test_desc_effect_buzzed_on_distillate.html"));
      var cleanups =
          new Cleanups(
              withProperty("nextDistillateMods", "Cold Resistance: 1"),
              withProperty("currentDistillateMods", ""),
              withProperty("familiarSweat", 1234),
              withHttpClientBuilder(builder),
              withPostChoice2(1476, 1, html("request/test_choice_stillsuit_drank.html")));

      try (cleanups) {
        assertThat("familiarSweat", isSetTo(0));
        assertThat("nextDistillateMods", isSetTo(""));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/desc_effect.php", "whicheffect=d64eab33f648e1a77da23ae516353fb2");
        assertThat(
            "currentDistillateMods",
            isSetTo(
                "Experience (Muscle): +3, Experience (Mysticality): +2, Experience (Moxie): +2, Damage Reduction: 9, Sleaze Damage: +6, Sleaze Spell Damage: +10"));
      }
    }
  }

  @Nested
  class Entauntauned {
    @Test
    void canDetectEntauntaunedLevel() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, html("request/test_desc_effect_entauntauned.html"));
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.MELODRAMEDARY, 1000),
              withProperty("_entauntaunedToday", false),
              withProperty("entauntaunedColdRes", 0),
              withHttpClientBuilder(builder),
              withChoice(1418, 1, html("request/test_choice_so_cold.html")));

      try (cleanups) {
        var melo = KoLCharacter.usableFamiliar(FamiliarPool.MELODRAMEDARY);
        assertThat(melo.getTotalExperience(), is(0));
        assertThat("_entauntaunedToday", isSetTo(true));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/desc_effect.php", "whicheffect=297ee9fadfb5560e5142e0b3a88456db");
        assertThat("entauntaunedColdRes", isSetTo(16));
      }
    }
  }

  @Nested
  class Cartography {
    @ParameterizedTest
    @CsvSource({
      "lastCartographyGuanoJunction, 1427",
      "lastCartographyHauntedBilliards, 1436",
      "lastCartographyDefiledNook, 1429",
      "lastCartographyFratHouse, 1425",
      "lastCartographyFratHouseVerge, 1433",
      "lastCartographyDarkNeck, 1428",
      "lastCartographyCastleTop, 1431",
      "lastCartographyHippyCampVerge, 1434",
      "lastCartographyBooPeak, 1430",
      "lastCartographyZeppelinProtesters, 1432"
    })
    void canDetectCartographyChoice(String property, int choice) {
      var cleanups =
          new Cleanups(withAscensions(5), withProperty(property, -1), withPostChoice1(0, 0));

      try (cleanups) {
        var req = new GenericRequest("choice.php?whichchoice=" + choice);
        req.responseText = "choice.php?whichchoice=" + choice;

        ChoiceManager.visitChoice(req);

        assertThat(property, isSetTo(5));
      }
    }
  }

  @Nested
  class Speakeasy {
    @Test
    void visitingPlaqueIdentifiesName() {
      var cleanups =
          new Cleanups(withProperty("speakeasyName", "Oliver's Place"), withPostChoice1(0, 0));

      try (cleanups) {
        var req = new GenericRequest("choice.php?whichchoice=1484");
        req.responseText = html("request/test_place_speakeasy_plaque.html");

        ChoiceManager.visitChoice(req);

        assertThat(
            "speakeasyName",
            isSetTo(
                "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"));
      }
    }

    @Test
    void changingPlaqueRecordsNewName() {
      var cleanups =
          new Cleanups(withProperty("speakeasyName", "Oliver's Place"), withPostChoice2(0, 0));

      try (cleanups) {
        var req = new GenericRequest("choice.php?whichchoice=1484&pwd&option=1&name=new+name");
        req.responseText = html("request/test_place_speakeasy_plaque_changed.html");

        ChoiceManager.preChoice(req);
        ChoiceManager.postChoice2(req.getURLString(), req);

        assertThat("speakeasyName", isSetTo("new name"));
      }
    }
  }

  @Nested
  class Yachtzee {
    @Test
    void choosingAnNCOptionSetsPreference() {
      var cleanups =
          new Cleanups(withProperty("encountersUntilYachtzeeChoice", 0), withPostChoice2(918, 2));
      try (cleanups) {
        assertThat("encountersUntilYachtzeeChoice", isSetTo(19));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"3, true", "5, false"})
  void sneakisolForcesNC(String decision, String propertyValue) {
    var cleanups =
        new Cleanups(
            withProperty("noncombatForcerActive", false),
            withPostChoice2(1395, Integer.parseInt(decision), "day's worth of pills"));
    try (cleanups) {
      assertThat("noncombatForcerActive", isSetTo(Boolean.parseBoolean(propertyValue)));
    }
  }

  @Nested
  class AutomatedFuture {
    @Test
    void choosingSolenoids() {
      var cleanups =
          new Cleanups(
              withProperty("_automatedFutureSide", ""),
              withProperty("_automatedFutureManufactures", 0),
              withChoice(
                  1512, 1, html("request/test_choice_automated_future_choose_solenoids.html")));

      try (cleanups) {
        assertThat("_automatedFutureSide", isSetTo("solenoids"));
        assertThat("_automatedFutureManufactures", isSetTo(1));
      }
    }

    @Test
    void correctWrongSide() {
      var cleanups =
          new Cleanups(
              withProperty("_automatedFutureSide", "bearings"),
              withProperty("_automatedFutureManufactures", 2),
              withChoice(
                  1512, 1, html("request/test_choice_automated_future_choose_solenoids.html")));

      try (cleanups) {
        assertThat("_automatedFutureSide", isSetTo("solenoids"));
        assertThat("_automatedFutureManufactures", isSetTo(3));
      }
    }

    @Test
    void discoverSide() {
      var cleanups =
          new Cleanups(
              withProperty("_automatedFutureSide", ""),
              withProperty("_automatedFutureManufactures", 2),
              withChoice(1513, html("request/test_choice_automated_future_try_other_side.html")));

      try (cleanups) {
        assertThat("_automatedFutureSide", isSetTo("solenoids"));
        assertThat("_automatedFutureManufactures", isSetTo(2));
      }
    }

    @Test
    void adjustWhenHitMaxManufactures() {
      var cleanups =
          new Cleanups(
              withProperty("_automatedFutureSide", ""),
              withProperty("_automatedFutureManufactures", 5),
              withChoice(1512, html("request/test_choice_automated_future_max_manufactures.html")));

      try (cleanups) {
        assertThat("_automatedFutureSide", isSetTo("solenoids"));
        assertThat("_automatedFutureManufactures", isSetTo(11));
      }
    }
  }

  @Test
  void updatesProtestorsWithCandyCane() {
    var cleanups =
        new Cleanups(
            withProperty("zeppelinProtestors", 30),
            withPostChoice2(857, 2, html("request/test_choice_bench_warrant_candy.html")));
    try (cleanups) {
      assertThat("zeppelinProtestors", isSetTo(78));
    }
  }

  @Nested
  class MimicDnaBank {
    @Test
    void updatesEggsObtainedAndDonatedIfPresent() {
      var cleanups =
          new Cleanups(withProperty("_mimicEggsDonated", 0), withProperty("_mimicEggsObtained", 0));

      try (cleanups) {
        var urlString = "choice.php?forceoption=0";
        var responseText = html("request/test_choice_mimic_dna_bank.html");
        var request = new GenericRequest(urlString);
        request.responseText = responseText;
        ChoiceManager.preChoice(request);
        request.processResponse();

        assertThat("_mimicEggsObtained", isSetTo(6));
        assertThat("_mimicEggsDonated", isSetTo(1));
      }
    }

    @Test
    void handlesDonating() {
      var cleanups =
          new Cleanups(
              withProperty("_mimicEggsDonated", 1),
              withPostChoice1(1517, 1, html("request/test_choice_mimic_dna_bank_donate.html")));
      try (cleanups) {
        assertThat("_mimicEggsDonated", isSetTo(2));
      }
    }

    @Test
    void handlesExtracting() {
      var cleanups =
          new Cleanups(
              withProperty("_mimicEggsObtained", 6),
              withPostChoice1(1517, 2, html("request/test_choice_mimic_dna_bank_extract.html")));
      try (cleanups) {
        assertThat("_mimicEggsObtained", isSetTo(7));
      }
    }
  }
}
