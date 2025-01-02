package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withNoEffects;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withPostChoice1;
import static internal.helpers.Player.withPostChoice2;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withTurnsPlayed;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ChoiceControlTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("ChoiceControlTest");
    Preferences.reset("ChoiceControlTest");
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
  class ChestMimic {
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
              withProperty("mimicEggMonsters", "374:1,378:2"),
              withPostChoice1(
                  1517, 1, "mid=374", html("request/test_choice_mimic_dna_bank_donate.html")));
      try (cleanups) {
        assertThat("_mimicEggsDonated", isSetTo(2));
        assertThat("mimicEggMonsters", isSetTo("378:2"));
      }
    }

    @Test
    void handlesInvalidFight() {
      var cleanups =
          new Cleanups(
              withProperty("mimicEggMonsters", "823:1"),
              withPostChoice1(
                  1516, 1, "mid=1", html("request/test_choice_mimic_egg_invalid.html")));
      try (cleanups) {
        assertThat("mimicEggMonsters", isSetTo("823:1"));
      }
    }

    @Test
    void handlesExtracting() {
      var cleanups =
          new Cleanups(
              withProperty("_mimicEggsObtained", 6),
              withProperty("mimicEggMonsters", "374:1,378:2"),
              withPostChoice1(
                  1517, 2, "mid=374", html("request/test_choice_mimic_dna_bank_extract.html")));
      try (cleanups) {
        assertThat("_mimicEggsObtained", isSetTo(7));
        assertThat("mimicEggMonsters", isSetTo("374:2,378:2"));
      }
    }

    @Test
    void doesNotCountFailedExtracts() {
      var cleanups =
          new Cleanups(
              withProperty("_mimicEggsObtained", 6),
              withProperty("mimicEggMonsters", "374:1,378:2"),
              withPostChoice1(
                  1517,
                  2,
                  "mid=374",
                  html("request/test_choice_mimic_dna_bank_not_enough_samples.html")));
      try (cleanups) {
        assertThat("_mimicEggsObtained", isSetTo(6));
        assertThat("mimicEggMonsters", isSetTo("374:1,378:2"));
      }
    }

    @Test
    void adjustsForTooManyExtracts() {
      var cleanups =
          new Cleanups(
              withProperty("_mimicEggsObtained", 6),
              withProperty("mimicEggMonsters", "374:1,378:2"),
              withPostChoice1(
                  1517,
                  2,
                  "mid=374",
                  html("request/test_choice_mimic_dna_bank_too_many_extracts.html")));
      try (cleanups) {
        assertThat("_mimicEggsObtained", isSetTo(11));
        assertThat("mimicEggMonsters", isSetTo("374:1,378:2"));
      }
    }
  }

  @Nested
  class WereProfessorResearch {
    @ParameterizedTest
    @CsvSource({
      "0, test_choice_wereprofessor_no_upgrades.html",
      "0, test_choice_wereprofessor_upgrades_before_organs.html",
      "1, test_choice_wereprofessor_one_of_each_organ.html",
      "2, test_choice_wereprofessor_two_of_each_organ.html",
      "3, test_choice_wereprofessor_three_of_each_organ.html"
    })
    void stomachTracking(int wereStomach, String fileName) {
      var cleanups = new Cleanups(withPostChoice1(0, 0));

      try (cleanups) {
        var req = new GenericRequest("choice.php?whichchoice=1523");
        req.responseText = html("request/" + fileName);

        ChoiceManager.visitChoice(req);

        assertThat("wereProfessorStomach", isSetTo(wereStomach));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "0, test_choice_wereprofessor_no_upgrades.html",
      "0, test_choice_wereprofessor_upgrades_before_organs.html",
      "1, test_choice_wereprofessor_one_of_each_organ.html",
      "2, test_choice_wereprofessor_two_of_each_organ.html",
      "3, test_choice_wereprofessor_three_of_each_organ.html"
    })
    void liverTracking(int wereLiver, String fileName) {
      var cleanups = new Cleanups(withPostChoice1(0, 0));

      try (cleanups) {
        var req = new GenericRequest("choice.php?whichchoice=1523");
        req.responseText = html("request/" + fileName);

        ChoiceManager.visitChoice(req);

        assertThat("wereProfessorLiver", isSetTo(wereLiver));
      }
    }
  }

  @Nested
  class SavageBeast {
    @Test
    void canDetectSavageBeastTransformation() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.WEREPROFESSOR),
              withNoEffects(),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withHandlingChoice(0),
              withProperty("_savageBeastMods", ""));

      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_wereprofessor_contest_booth.html"));
        client.addResponse(200, "");
        client.addResponse(200, html("request/test_mild_mannered_professor_contest_booth.html"));
        client.addResponse(200, html("request/test_savage_beast_effect.html"));
        client.addResponse(200, "");

        var boothRequest = new PlaceRequest("nstower", "ns_01_contestbooth", true);
        boothRequest.run();

        assertThat(ChoiceManager.handlingChoice, is(true));
        assertThat(ChoiceManager.lastChoice, is(1003));
        assertThat(KoLCharacter.isMildManneredProfessor(), is(true));
        assertThat(KoLCharacter.isSavageBeast(), is(false));

        var moonRequest = new GenericRequest("choice.php?whichchoice=1003&option=5", true);
        moonRequest.run();

        assertThat(ChoiceManager.handlingChoice, is(false));
        assertThat(KoLCharacter.isMildManneredProfessor(), is(false));
        assertThat(KoLCharacter.isSavageBeast(), is(true));
        assertThat(
            "_savageBeastMods",
            isSetTo(
                "Combat Rate: +25, Muscle Percent: 100, Maximum HP Percent: 100, Damage Reduction: 90, Mysticality Percent: 100, Hot Resistance: 6, Cold Resistance: 6, Stench Resistance: 6, Sleaze Resistance: 6, Spooky Resistance: 6, Item Drop: 75, Monster Level: 50, Moxie Percent: 100, Initiative: 200, Meat Drop: 150, Experience: +5, HP Regen Min: 9, HP Regen Max: 11"));

        var requests = client.getRequests();

        assertThat(requests, hasSize(6));

        assertPostRequest(
            requests.get(0), "/place.php", "whichplace=nstower&action=ns_01_contestbooth");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/choice.php", "whichchoice=1003&option=5");
        assertPostRequest(
            requests.get(4), "/desc_effect.php", "whicheffect=d9cb5591b2638f856456d356f5c4fb0e");
        assertPostRequest(requests.get(5), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void canDetectAlreadySavageBeast() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.WEREPROFESSOR),
              withNoEffects(),
              withIntrinsicEffect(EffectPool.SAVAGE_BEAST),
              withHandlingChoice(0),
              withProperty(
                  "_savageBeastMods",
                  "Combat Rate: +25, Muscle Percent: 100, Maximum HP Percent: 100, Damage Reduction: 90, Mysticality Percent: 100, Hot Resistance: 6, Cold Resistance: 6, Stench Resistance: 6, Sleaze Resistance: 6, Spooky Resistance: 6, Item Drop: 75, Monster Level: 50, Moxie Percent: 100, Initiative: 200, Meat Drop: 150, Experience: +5, HP Regen Min: 9, HP Regen Max: 11"));

      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_wereprofessor_contest_booth.html"));
        client.addResponse(200, "");
        client.addResponse(200, html("request/test_savage_beast_contest_booth.html"));

        var boothRequest = new PlaceRequest("nstower", "ns_01_contestbooth", true);
        boothRequest.run();

        assertThat(ChoiceManager.handlingChoice, is(true));
        assertThat(ChoiceManager.lastChoice, is(1003));
        assertThat(KoLCharacter.isMildManneredProfessor(), is(false));
        assertThat(KoLCharacter.isSavageBeast(), is(true));

        var moonRequest = new GenericRequest("choice.php?whichchoice=1003&option=5", true);
        moonRequest.run();

        assertThat(ChoiceManager.handlingChoice, is(false));
        assertThat(KoLCharacter.isMildManneredProfessor(), is(false));
        assertThat(KoLCharacter.isSavageBeast(), is(true));

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0), "/place.php", "whichplace=nstower&action=ns_01_contestbooth");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/choice.php", "whichchoice=1003&option=5");
      }
    }
  }

  @Test
  void canDetectSmashedScientificEquipment() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    String prev = "Cobb's Knob Laboratory";
    String location = "Cobb's Knob Menagerie, Level 1";

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withLastLocation(location),
            withProperty("antiScientificMethod", prev),
            withHandlingChoice(1522));

    try (cleanups) {
      client.addResponse(200, html("request/test_smashed_scientific_equipment.html"));

      // choice.php?pwd&whichchoice=1522&option=1
      var request = new GenericRequest("choice.php?whichchoice=1522&option=1", true);
      request.run();

      assertThat(ChoiceManager.handlingChoice, is(false));
      assertThat("antiScientificMethod", isSetTo(prev + "|" + location));

      var requests = client.getRequests();
      assertThat(requests, hasSize(1));

      assertPostRequest(requests.get(0), "/choice.php", "whichchoice=1522&option=1");
    }
  }

  @Test
  void canUseCandyCaneSwordToClearEvil() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    String location = "The Defiled Cranny";

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withEquipped(Slot.WEAPON, ItemPool.CANDY_CANE_SWORD),
            withLastLocation("The Defiled Cranny"),
            withProperty("cyrptCrannyEvilness", 50));

    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_defiled_cranny_choice.html"));
      client.addResponse(200, html("request/test_defiled_cranny_choice_candy_sword.html"));
      client.addResponse(200, "");

      var request = new GenericRequest("adventure.php?snarfblat=262", true);
      request.run();

      assertThat(ChoiceManager.handlingChoice, is(true));
      assertThat(ChoiceManager.lastChoice, is(523));

      var choice = new GenericRequest("choice.php?whichchoice=523&option=5", true);
      choice.run();

      assertThat(ChoiceManager.handlingChoice, is(false));
      assertThat("cyrptCrannyEvilness", isSetTo(39));

      var requests = client.getRequests();
      assertThat(requests, hasSize(4));

      assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=262");
      assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      assertPostRequest(requests.get(2), "/choice.php", "whichchoice=523&option=5");
      assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
    }
  }

  @Nested
  class AprilConduct {
    @Test
    void choosingAConductSetsPreference() {
      var cleanups =
          new Cleanups(
              withTurnsPlayed(6), withProperty("nextAprilBandTurn", 0), withPostChoice2(1526, 2));
      try (cleanups) {
        assertThat("nextAprilBandTurn", isSetTo(17));
      }
    }

    @Test
    void choosingAnInstrumentSetsPreference() {
      var cleanups =
          new Cleanups(withProperty("_aprilBandInstruments", 0), withPostChoice2(1526, 7));
      try (cleanups) {
        assertThat("_aprilBandInstruments", isSetTo(1));
      }
    }
  }

  @Nested
  class Mayam {
    @Test
    void parsesUnusedCalendarOnVisit() {
      var cleanups =
          new Cleanups(
              withProperty("_mayamSymbolsUsed", "yam1,yam2,yam3,yam4"),
              withChoice(1527, html("request/test_choice_mayam_unused.html")));

      try (cleanups) {
        assertThat("_mayamSymbolsUsed", isSetTo(""));
      }
    }

    @Test
    void parsesUsedCalendarOnVisit() {
      var cleanups =
          new Cleanups(
              withProperty("_mayamSymbolsUsed", ""),
              withChoice(1527, html("request/test_choice_mayam_used.html")));

      try (cleanups) {
        assertThat("_mayamSymbolsUsed", isSetTo("sword,meat,wall,explosion"));
      }
    }

    @Test
    void parsesWoodNotBoard() {
      var cleanups =
          new Cleanups(
              withProperty("_mayamSymbolsUsed", ""),
              withChoice(1527, html("request/test_choice_mayam_used_wood.html")));

      try (cleanups) {
        assertThat("_mayamSymbolsUsed", isSetTo("sword,wood,wall,explosion"));
      }
    }

    @Test
    void parsesUsedCalendarAfterUse() {
      var cleanups =
          new Cleanups(
              withProperty("_mayamSymbolsUsed", ""),
              withPostChoice2(1527, 1, html("request/test_choice_mayam_used.html")));

      try (cleanups) {
        assertThat("_mayamSymbolsUsed", isSetTo("sword,meat,wall,explosion"));
      }
    }

    @Test
    void parsesUsedYamsCorrectly() {
      var cleanups =
          new Cleanups(
              withProperty("_mayamSymbolsUsed", ""),
              withChoice(1527, html("request/test_choice_mayam_used_some_yams.html")));

      try (cleanups) {
        assertThat("_mayamSymbolsUsed", isSetTo("yam1,sword,wood,meat,yam3,wall,clock,explosion"));
      }
    }

    @Test
    void choosingChairGivesFreeRests() {
      var cleanups =
          new Cleanups(
              withProperty("_mayamRests", 0),
              withPostChoice2(1527, 1, html("request/test_choice_mayam_chair.html")));
      try (cleanups) {
        assertThat("_mayamRests", isSetTo(5));
      }
    }
  }

  @Nested
  class TrickOrTreat {
    @ParameterizedTest
    @CsvSource({
      "full,DLDLLLDLLDDL",
      "some_used,DldLLLDLLDDL",
      "starhouse,LLLDLDLdSDDL",
      "starhouse_used,LLLDLDLdsDDL",
    })
    void parsesBlockState(final String text, final String expected) {
      var cleanups =
          new Cleanups(
              withProperty("_trickOrTreatBlock", ""),
              withChoice(804, html("request/test_halloween_" + text + ".html")));

      try (cleanups) {
        assertThat("_trickOrTreatBlock", isSetTo(expected));
      }
    }

    @Test
    void changesBlockStateOnSelection() {
      var cleanups =
          new Cleanups(
              withProperty("_trickOrTreatBlock", "DLDLLLDLLDDL"),
              withChoice((url, req) -> ChoiceControl.preChoice(req), 804, 3, "whichhouse=2", ""));

      try (cleanups) {
        assertThat("_trickOrTreatBlock", isSetTo("DLdLLLDLLDDL"));
      }
    }
  }

  @Nested
  class PirateRealm {
    @Test
    void canParseCrewmates() {
      var responseText = html("request/test_choice_piraterealm_three_crewmates.html");
      var cleanups =
          new Cleanups(
              withProperty("_pirateRealmCrewmate", ""),
              withProperty("_pirateRealmCrewmate1", ""),
              withProperty("_pirateRealmCrewmate2", ""),
              withProperty("_pirateRealmCrewmate3", ""),
              withProperty("pirateRealmUnlockedThirdCrewmate", false),
              withChoice(1347, responseText));

      try (cleanups) {
        assertThat("_pirateRealmCrewmate", isSetTo(""));
        assertThat("_pirateRealmCrewmate1", isSetTo("Beligerent Coxswain"));
        assertThat("_pirateRealmCrewmate2", isSetTo("Pinch-Fisted Cryptobotanist"));
        assertThat("_pirateRealmCrewmate3", isSetTo("Dipsomaniacal Cuisinier"));
        assertThat("pirateRealmUnlockedThirdCrewmate", isSetTo(true));
      }
    }

    @Test
    void canSelectCrewmate() {
      var cleanups =
          new Cleanups(
              withProperty("_pirateRealmCrewmate", ""),
              withProperty("_pirateRealmCrewmate1", "Beligerent Coxswain"),
              withProperty("_pirateRealmCrewmate2", "Pinch-Fisted Cryptobotanist"),
              withProperty("_pirateRealmCrewmate3", "Dipsomaniacal Cuisinier"),
              withChoice(1347, 2, ""));

      try (cleanups) {
        assertThat("_pirateRealmCrewmate", isSetTo("Pinch-Fisted Cryptobotanist"));
      }
    }

    @Test
    void canParseShips() {
      var responseText = html("request/test_choice_piraterealm_manowar.html");
      var cleanups =
          new Cleanups(
              withProperty("pirateRealmUnlockedManOWar", false),
              withProperty("pirateRealmUnlockedClipper", false),
              withChoice(1349, responseText));

      try (cleanups) {
        assertThat("pirateRealmUnlockedManOWar", isSetTo(true));
        assertThat("pirateRealmUnlockedClipper", isSetTo(false));
      }
    }

    @CsvSource({
      "1, Rigged Frigate, 7",
      "2, Intimidating Galleon, 7",
      "3, Speedy Caravel, 6",
      "4, Swift Clipper, 4",
      "5, Menacing Man o' War, 9"
    })
    @ParameterizedTest
    void canSelectShip(final int decision, final String name, final int speed) {
      var cleanups =
          new Cleanups(
              withProperty("_pirateRealmShip", ""),
              withProperty("_pirateRealmShipSpeed", 0),
              withChoice(1349, decision, ""));

      try (cleanups) {
        assertThat("_pirateRealmShip", isSetTo(name));
        assertThat("_pirateRealmShipSpeed", isSetTo(speed));
      }
    }

    @CsvSource({
      "1356, 0, false",
      "1356, 1, true",
      "1357, 1, true",
      "1360, 1, false",
      "1360, 6, true",
      "1361, 1, true",
      "1362, 1, true",
      "1363, 1, true",
      "1364, 1, true",
      "1365, 1, true",
    })
    @ParameterizedTest
    void makesSailingProgressInValidChoices(
        final int choice, final int decision, final boolean addsTurn) {
      var cleanups =
          new Cleanups(
              withProperty("_pirateRealmSailingTurns", 0), withChoice(choice, decision, ""));
      try (cleanups) {
        assertThat("_pirateRealmSailingTurns", isSetTo(addsTurn ? 1 : 0));
      }
    }

    @ParameterizedTest
    @CsvSource({"2,3", "3,3", "7,8", "8,8", "12,13", "13,13"})
    void questReflectsCompletedSail(final int startingStep, final int finishingStep) {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.PIRATEREALM, startingStep),
              withProperty("_pirateRealmShipSpeed", 7),
              withProperty("_pirateRealmSailingTurns", 6),
              withChoice(1356, 1, ""));
      try (cleanups) {
        assertThat("_pirateRealmSailingTurns", isSetTo(7));
        var test2 = QuestDatabase.getQuest(Quest.PIRATEREALM);
        assertThat(Quest.PIRATEREALM, isStep(finishingStep));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void handlesOutsailingStorm(final boolean overshoot) {
      var responseText = html("request/test_choice_piraterealm_outsailed_storm.html");

      var cleanups =
          new Cleanups(
              withProperty("_pirateRealmSailingTurns", 0),
              withProperty("pirateRealmStormsEscaped", 0),
              withQuestProgress(Quest.PIRATEREALM, overshoot ? 3 : 2),
              withChoice(1362, 2, responseText));

      try (cleanups) {
        assertThat("_pirateRealmSailingTurns", isSetTo(2));
        assertThat("pirateRealmStormsEscaped", isSetTo(1));
        assertThat(Quest.PIRATEREALM, isStep(3));
      }
    }
  }

  @Nested
  class BWApron {
    @Test
    void handlesSuccess() {
      var responseText = html("request/test_choice_bw_apron_success.html");
      try (var cleanups =
          new Cleanups(
              withProperty("bwApronMealsEaten", 1),
              withItem(ItemPool.BLACK_AND_WHITE_APRON_MEAL_KIT),
              withChoice(1518, 1, responseText))) {
        assertThat("bwApronMealsEaten", isSetTo(2));
        assertThat(InventoryManager.getCount(ItemPool.BLACK_AND_WHITE_APRON_MEAL_KIT), is(0));
      }
    }

    @Test
    void handlesUnknownMealsEaten() {
      var responseText = html("request/test_choice_bw_apron_success.html");
      try (var cleanups =
          new Cleanups(
              withItem(ItemPool.BLACK_AND_WHITE_APRON_MEAL_KIT),
              withChoice(1518, 1, responseText))) {
        assertThat("bwApronMealsEaten", isSetTo(-1));
        assertThat(InventoryManager.getCount(ItemPool.BLACK_AND_WHITE_APRON_MEAL_KIT), is(0));
      }
    }

    @Test
    void handlesFull() {
      var responseText = html("request/test_choice_bw_apron_full.html");
      try (var cleanups =
          new Cleanups(
              withProperty("bwApronMealsEaten", 1),
              withItem(ItemPool.BLACK_AND_WHITE_APRON_MEAL_KIT),
              withChoice(1518, 1, responseText))) {
        assertThat("bwApronMealsEaten", isSetTo(1));
        assertThat(InventoryManager.getCount(ItemPool.BLACK_AND_WHITE_APRON_MEAL_KIT), is(1));
      }
    }
  }

  @Nested
  class BodyguardChat {
    @Test
    void tracksChattedBodyguard() {
      var responseText = html("request/test_choice_bodyguard_chat_success.html");
      try (var cleanups =
          new Cleanups(
              withPath(Path.AVANT_GUARD),
              withFamiliar(FamiliarPool.BURLY_BODYGUARD),
              withProperty("bodyguardCharge", 50),
              withProperty("bodyguardChatMonster", ""),
              withChoice(1532, 1, "bgid=1430", responseText))) {
        assertThat("bodyguardCharge", isSetTo(0));
        assertThat("bodyguardChatMonster", isSetTo("pygmy witch accountant"));
      }
    }
  }

  @Nested
  class TakerSpace {
    @ParameterizedTest
    @CsvSource({
      "first,3,15,26,26,7,1",
      "parse,9,15,28,27,5,6",
    })
    void parsesIngredientsOnFirstVisit(
        String frag, int spices, int rum, int anchor, int mast, int silk, int gold) {
      var cleanups =
          new Cleanups(
              withProperty("takerSpaceSpice", 0),
              withProperty("takerSpaceRum", 0),
              withProperty("takerSpaceAnchor", 0),
              withProperty("takerSpaceMast", 0),
              withProperty("takerSpaceSilk", 0),
              withProperty("takerSpaceGold", 0),
              withChoice(1537, html("request/test_campground_takerspace_" + frag + ".html")));

      try (cleanups) {
        assertThat("_takerSpaceSuppliesDelivered", isSetTo(true));
        assertThat("takerSpaceSpice", isSetTo(spices));
        assertThat("takerSpaceRum", isSetTo(rum));
        assertThat("takerSpaceAnchor", isSetTo(anchor));
        assertThat("takerSpaceMast", isSetTo(mast));
        assertThat("takerSpaceSilk", isSetTo(silk));
        assertThat("takerSpaceGold", isSetTo(gold));
      }
    }
  }

  @Test
  void devilsEgg() {
    var cleanups =
        new Cleanups(
            withProperty("_candyEggsDeviled", 1),
            withItem(ItemPool.BLACK_CANDY_HEART, 3),
            withPostChoice2(1544, 1, "a=3054", html("request/test_choice_devilegg.html")));

    try (cleanups) {
      assertThat("_candyEggsDeviled", isSetTo(2));
      assertThat(InventoryManager.getCount(ItemPool.BLACK_CANDY_HEART), is(2));
    }
  }
}
