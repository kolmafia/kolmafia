package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withBanishedMonsters;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withNoEffects;
import static internal.helpers.Player.withNoItems;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withPostChoice1;
import static internal.helpers.Player.withPostChoice2;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withTrackedMonsters;
import static internal.helpers.Player.withTurnsPlayed;
import static internal.matchers.Preference.hasStringValue;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.StringContains.containsString;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
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
      try (var _ = withPostChoice2(560, 1)) {
        assertThat(Quest.CLUMSINESS, isStep(QuestDatabase.STARTED));
      }
    }

    @ParameterizedTest
    @CsvSource({"1, The Thorax", "2, The Bat in the Spats"})
    public void chooseYourDestructionSetsBoss(String decision, String monster) {
      try (var _ = withPostChoice2(561, Integer.parseInt(decision))) {
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
      try (var _ = new Cleanups(withItem(Integer.parseInt(stoneId)))) {
        try (var _ = withPostChoice2(563, 1)) {
          assertThat("clumsinessGroveBoss", isSetTo(monster));
          assertThat(Quest.CLUMSINESS, isStep(3));
        }
      }
    }

    @Test
    public void maelstromOfTroubleStartsMaelstromQuest() {
      try (var _ = withPostChoice2(564, 1)) {
        assertThat(Quest.MAELSTROM, isStep(QuestDatabase.STARTED));
      }
    }

    @ParameterizedTest
    @CsvSource({"1, The Terrible Pinch", "2, Thug 1 and Thug 2"})
    public void getGropedOrGetMuggedSetsBoss(String decision, String monster) {
      try (var _ = withPostChoice2(565, Integer.parseInt(decision))) {
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
      try (var _ = new Cleanups(withItem(Integer.parseInt(stoneId)))) {
        try (var _ = withPostChoice2(566, 1)) {
          assertThat("maelstromOfLoversBoss", isSetTo(monster));
          assertThat(Quest.MAELSTROM, isStep(3));
        }
      }
    }

    @Test
    public void mayBeOnThinIceStartsGlacierQuest() {
      try (var _ = withPostChoice2(567, 1)) {
        assertThat(Quest.GLACIER, isStep(QuestDatabase.STARTED));
      }
    }

    @ParameterizedTest
    @CsvSource({"1, Mammon the Elephant", "2, The Large-Bellied Snitch"})
    public void someSoundsMostUnnervingSetsBoss(String decision, String monster) {
      try (var _ = withPostChoice2(568, Integer.parseInt(decision))) {
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
      try (var _ = new Cleanups(withItem(Integer.parseInt(stoneId)))) {
        try (var _ = withPostChoice2(569, 1)) {
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
      try (var _ = new Cleanups(withProperty("_coldMedicineEquipmentTaken", -1))) {
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
      try (var _ =
          new Cleanups(withProperty("_coldMedicineEquipmentTaken", 0), withPostChoice1(1455, 1))) {
        assertThat("_coldMedicineEquipmentTaken", isSetTo(1));
      }
    }
  }

  @Nested
  class StillSuit {
    @Test
    void canReadInformationFromChoicePage() {
      try (var _ = new Cleanups(withProperty("familiarSweat"))) {
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
                "Muscle Experience: +5, Moxie Experience: +4, Spooky Damage: +15, Spooky Spell Damage: +25"));
      }
    }

    @Test
    void canReadInformationFromChoicePageWhenLessThan10Drams() {
      try (var _ = new Cleanups(withProperty("familiarSweat"))) {
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
      try (var _ = new Cleanups(withProperty("nextDistillateMods", "Cold Resistance: +1"))) {
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
      try (var _ =
          new Cleanups(
              withProperty("nextDistillateMods", "Cold Resistance: 1"),
              withProperty("currentDistillateMods", ""),
              withProperty("familiarSweat", 1234),
              withHttpClientBuilder(builder),
              withPostChoice2(1476, 1, html("request/test_choice_stillsuit_drank.html")))) {
        assertThat("familiarSweat", isSetTo(0));
        assertThat("nextDistillateMods", isSetTo(""));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/desc_effect.php", "whicheffect=d64eab33f648e1a77da23ae516353fb2");
        assertThat(
            "currentDistillateMods",
            isSetTo(
                "Muscle Experience: +3, Mysticality Experience: +2, Moxie Experience: +2, Damage Reduction: 9, Sleaze Damage: +6, Sleaze Spell Damage: +10"));
      }
    }
  }

  @Nested
  class Entauntauned {
    @Test
    void canDetectEntauntaunedLevel() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, html("request/test_desc_effect_entauntauned.html"));
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.MELODRAMEDARY, 1000),
              withProperty("_entauntaunedToday", false),
              withProperty("entauntaunedColdRes", 0),
              withHttpClientBuilder(builder),
              withChoice(1418, 1, html("request/test_choice_so_cold.html")))) {
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
      try (var _ =
          new Cleanups(withAscensions(5), withProperty(property, -1), withPostChoice1(0, 0))) {
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
      try (var _ =
          new Cleanups(withProperty("speakeasyName", "Oliver's Place"), withPostChoice1(0, 0))) {
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
      try (var _ =
          new Cleanups(withProperty("speakeasyName", "Oliver's Place"), withPostChoice2(0, 0))) {
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
      try (var _ =
          new Cleanups(withProperty("encountersUntilYachtzeeChoice", 0), withPostChoice2(918, 2))) {
        assertThat("encountersUntilYachtzeeChoice", isSetTo(19));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"3, true", "5, false"})
  void sneakisolForcesNC(String decision, String propertyValue) {
    try (var _ =
        new Cleanups(
            withProperty("noncombatForcerActive", false),
            withPostChoice2(1395, Integer.parseInt(decision), "day's worth of pills"))) {
      assertThat("noncombatForcerActive", isSetTo(Boolean.parseBoolean(propertyValue)));
    }
  }

  @Nested
  class AutomatedFuture {
    @Test
    void choosingSolenoids() {
      try (var _ =
          new Cleanups(
              withProperty("_automatedFutureSide", ""),
              withProperty("_automatedFutureManufactures", 0),
              withChoice(
                  1512, 1, html("request/test_choice_automated_future_choose_solenoids.html")))) {
        assertThat("_automatedFutureSide", isSetTo("solenoids"));
        assertThat("_automatedFutureManufactures", isSetTo(1));
      }
    }

    @Test
    void correctWrongSide() {
      try (var _ =
          new Cleanups(
              withProperty("_automatedFutureSide", "bearings"),
              withProperty("_automatedFutureManufactures", 2),
              withChoice(
                  1512, 1, html("request/test_choice_automated_future_choose_solenoids.html")))) {
        assertThat("_automatedFutureSide", isSetTo("solenoids"));
        assertThat("_automatedFutureManufactures", isSetTo(3));
      }
    }

    @Test
    void discoverSide() {
      try (var _ =
          new Cleanups(
              withProperty("_automatedFutureSide", ""),
              withProperty("_automatedFutureManufactures", 2),
              withChoice(1513, html("request/test_choice_automated_future_try_other_side.html")))) {
        assertThat("_automatedFutureSide", isSetTo("solenoids"));
        assertThat("_automatedFutureManufactures", isSetTo(2));
      }
    }

    @Test
    void adjustWhenHitMaxManufactures() {
      try (var _ =
          new Cleanups(
              withProperty("_automatedFutureSide", ""),
              withProperty("_automatedFutureManufactures", 5),
              withChoice(
                  1512, html("request/test_choice_automated_future_max_manufactures.html")))) {
        assertThat("_automatedFutureSide", isSetTo("solenoids"));
        assertThat("_automatedFutureManufactures", isSetTo(11));
      }
    }
  }

  @Test
  void updatesProtestorsWithCandyCane() {
    try (var _ =
        new Cleanups(
            withProperty("zeppelinProtestors", 30),
            withPostChoice2(857, 2, html("request/test_choice_bench_warrant_candy.html")))) {
      assertThat("zeppelinProtestors", isSetTo(78));
    }
  }

  @Nested
  class ChestMimic {
    @Test
    void updatesEggsObtainedAndDonatedIfPresent() {
      try (var _ =
          new Cleanups(
              withProperty("_mimicEggsDonated", 0), withProperty("_mimicEggsObtained", 0))) {
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
      try (var _ =
          new Cleanups(
              withProperty("_mimicEggsDonated", 1),
              withProperty("mimicEggMonsters", "374:1,378:2"),
              withPostChoice1(
                  1517, 1, "mid=374", html("request/test_choice_mimic_dna_bank_donate.html")))) {
        assertThat("_mimicEggsDonated", isSetTo(2));
        assertThat("mimicEggMonsters", isSetTo("378:2"));
      }
    }

    @Test
    void handlesInvalidFight() {
      try (var _ =
          new Cleanups(
              withProperty("mimicEggMonsters", "823:1"),
              withPostChoice1(
                  1516, 1, "mid=1", html("request/test_choice_mimic_egg_invalid.html")))) {
        assertThat("mimicEggMonsters", isSetTo("823:1"));
      }
    }

    @Test
    void handlesExtracting() {
      try (var _ =
          new Cleanups(
              withProperty("_mimicEggsObtained", 6),
              withProperty("mimicEggMonsters", "374:1,378:2"),
              withPostChoice1(
                  1517, 2, "mid=374", html("request/test_choice_mimic_dna_bank_extract.html")))) {
        assertThat("_mimicEggsObtained", isSetTo(7));
        assertThat("mimicEggMonsters", isSetTo("374:2,378:2"));
      }
    }

    @Test
    void doesNotCountFailedExtracts() {
      try (var _ =
          new Cleanups(
              withProperty("_mimicEggsObtained", 6),
              withProperty("mimicEggMonsters", "374:1,378:2"),
              withPostChoice1(
                  1517,
                  2,
                  "mid=374",
                  html("request/test_choice_mimic_dna_bank_not_enough_samples.html")))) {
        assertThat("_mimicEggsObtained", isSetTo(6));
        assertThat("mimicEggMonsters", isSetTo("374:1,378:2"));
      }
    }

    @Test
    void adjustsForTooManyExtracts() {
      try (var _ =
          new Cleanups(
              withProperty("_mimicEggsObtained", 6),
              withProperty("mimicEggMonsters", "374:1,378:2"),
              withPostChoice1(
                  1517,
                  2,
                  "mid=374",
                  html("request/test_choice_mimic_dna_bank_too_many_extracts.html")))) {
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
      try (var _ = new Cleanups(withPostChoice1(0, 0))) {
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
      try (var _ = new Cleanups(withPostChoice1(0, 0))) {
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

      try (var _ =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.WEREPROFESSOR),
              withNoEffects(),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withHandlingChoice(0),
              withProperty("_savageBeastMods", ""))) {
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
                "Combat Rate: +25, Muscle Percent: 100, Maximum HP Percent: 100, Damage Reduction: 30, Mysticality Percent: 100, Hot Resistance: 6, Cold Resistance: 6, Stench Resistance: 6, Sleaze Resistance: 6, Spooky Resistance: 6, Item Drop: 75, Monster Level: 50, Moxie Percent: 100, Initiative: 200, Meat Drop: 150, Experience: +5, HP Regen Min: 9, HP Regen Max: 11"));

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

      try (var _ =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPath(Path.WEREPROFESSOR),
              withNoEffects(),
              withIntrinsicEffect(EffectPool.SAVAGE_BEAST),
              withHandlingChoice(0),
              withProperty(
                  "_savageBeastMods",
                  "Combat Rate: +25, Muscle Percent: 100, Maximum HP Percent: 100, Damage Reduction: 90, Mysticality Percent: 100, Hot Resistance: 6, Cold Resistance: 6, Stench Resistance: 6, Sleaze Resistance: 6, Spooky Resistance: 6, Item Drop: 75, Monster Level: 50, Moxie Percent: 100, Initiative: 200, Meat Drop: 150, Experience: +5, HP Regen Min: 9, HP Regen Max: 11"))) {
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

    try (var _ =
        new Cleanups(
            withHttpClientBuilder(builder),
            withLastLocation(location),
            withProperty("antiScientificMethod", prev),
            withHandlingChoice(1522))) {
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

    try (var _ =
        new Cleanups(
            withHttpClientBuilder(builder),
            withEquipped(Slot.WEAPON, ItemPool.CANDY_CANE_SWORD),
            withLastLocation("The Defiled Cranny"),
            withProperty("cyrptCrannyEvilness", 50))) {
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
      try (var _ =
          new Cleanups(
              withTurnsPlayed(6), withProperty("nextAprilBandTurn", 0), withPostChoice2(1526, 2))) {
        assertThat("nextAprilBandTurn", isSetTo(17));
      }
    }

    @Test
    void choosingAnInstrumentSetsPreference() {
      try (var _ =
          new Cleanups(withProperty("_aprilBandInstruments", 0), withPostChoice2(1526, 7))) {
        assertThat("_aprilBandInstruments", isSetTo(1));
      }
    }
  }

  @Nested
  class Mayam {
    @Test
    void parsesUnusedCalendarOnVisit() {
      try (var _ =
          new Cleanups(
              withProperty("_mayamSymbolsUsed", "yam1,yam2,yam3,yam4"),
              withChoice(1527, html("request/test_choice_mayam_unused.html")))) {
        assertThat("_mayamSymbolsUsed", isSetTo(""));
      }
    }

    @Test
    void parsesUsedCalendarOnVisit() {
      try (var _ =
          new Cleanups(
              withProperty("_mayamSymbolsUsed", ""),
              withChoice(1527, html("request/test_choice_mayam_used.html")))) {
        assertThat("_mayamSymbolsUsed", isSetTo("sword,meat,wall,explosion"));
      }
    }

    @Test
    void parsesWoodNotBoard() {
      try (var _ =
          new Cleanups(
              withProperty("_mayamSymbolsUsed", ""),
              withChoice(1527, html("request/test_choice_mayam_used_wood.html")))) {
        assertThat("_mayamSymbolsUsed", isSetTo("sword,wood,wall,explosion"));
      }
    }

    @Test
    void parsesUsedCalendarAfterUse() {
      try (var _ =
          new Cleanups(
              withProperty("_mayamSymbolsUsed", ""),
              withPostChoice2(1527, 1, html("request/test_choice_mayam_used.html")))) {
        assertThat("_mayamSymbolsUsed", isSetTo("sword,meat,wall,explosion"));
      }
    }

    @Test
    void parsesUsedYamsCorrectly() {
      try (var _ =
          new Cleanups(
              withProperty("_mayamSymbolsUsed", ""),
              withChoice(1527, html("request/test_choice_mayam_used_some_yams.html")))) {
        assertThat("_mayamSymbolsUsed", isSetTo("yam1,sword,wood,meat,yam3,wall,clock,explosion"));
      }
    }

    @Test
    void choosingChairGivesFreeRests() {
      try (var _ =
          new Cleanups(
              withProperty("_mayamRests", 0),
              withPostChoice2(1527, 1, html("request/test_choice_mayam_chair.html")))) {
        assertThat("_mayamRests", isSetTo(5));
      }
    }

    @Test
    void choosingFurGivesExperience() {
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.JILL_OF_ALL_TRADES),
              withPostChoice2(1527, 1, html("request/test_choice_mayam_fur.html")))) {
        assertThat(KoLCharacter.getFamiliar().getTotalExperience(), equalTo(100));
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
      try (var _ =
          new Cleanups(
              withProperty("_trickOrTreatBlock", ""),
              withChoice(804, html("request/test_halloween_" + text + ".html")))) {
        assertThat("_trickOrTreatBlock", isSetTo(expected));
      }
    }

    @Test
    void changesBlockStateOnSelection() {
      try (var _ =
          new Cleanups(
              withProperty("_trickOrTreatBlock", "DLDLLLDLLDDL"),
              withChoice((_, req) -> ChoiceControl.preChoice(req), 804, 3, "whichhouse=2", ""))) {
        assertThat("_trickOrTreatBlock", isSetTo("DLdLLLDLLDDL"));
      }
    }

    // Using this as an example of a pref that would be errantly incremented if the zone wasn't
    // cleared
    @Test
    void doesntIncrementShadowRiftPrefsOnSelection() {
      try (var _ =
          new Cleanups(
              withProperty("encountersUntilSRChoice", 10),
              withProperty("_trickOrTreatBlock", "DLDLLLDLLDDL"),
              withLastLocation(AdventureDatabase.getAdventureByName("Shadow Rift (Desert Beach)")),
              withChoice(
                  (_, req) -> ChoiceControl.preChoice(req),
                  804,
                  3,
                  "whichhouse=2",
                  html("request/test_halloween_starhouse.html")))) {
        assertThat("encountersUntilSRChoice", isSetTo(10));
      }
    }

    @Test
    void clearsZone() {
      try (var _ =
          new Cleanups(
              withProperty("_trickOrTreatBlock", "DLDLLLDLLDDL"),
              withChoice((_, req) -> ChoiceControl.preChoice(req), 804, 3, "whichhouse=2", ""))) {
        assertThat(KoLAdventure.lastVisitedLocation, nullValue());
      }
    }
  }

  @Nested
  class PirateRealm {
    @Test
    void canParseCrewmates() {
      var responseText = html("request/test_choice_piraterealm_three_crewmates.html");
      try (var _ =
          new Cleanups(
              withProperty("_pirateRealmCrewmate", ""),
              withProperty("_pirateRealmCrewmate1", ""),
              withProperty("_pirateRealmCrewmate2", ""),
              withProperty("_pirateRealmCrewmate3", ""),
              withProperty("pirateRealmUnlockedThirdCrewmate", false),
              withChoice(1347, responseText))) {
        assertThat("_pirateRealmCrewmate", isSetTo(""));
        assertThat("_pirateRealmCrewmate1", isSetTo("Beligerent Coxswain"));
        assertThat("_pirateRealmCrewmate2", isSetTo("Pinch-Fisted Cryptobotanist"));
        assertThat("_pirateRealmCrewmate3", isSetTo("Dipsomaniacal Cuisinier"));
        assertThat("pirateRealmUnlockedThirdCrewmate", isSetTo(true));
      }
    }

    @Test
    void canSelectCrewmate() {
      try (var _ =
          new Cleanups(
              withProperty("_pirateRealmCrewmate", ""),
              withProperty("_pirateRealmCrewmate1", "Beligerent Coxswain"),
              withProperty("_pirateRealmCrewmate2", "Pinch-Fisted Cryptobotanist"),
              withProperty("_pirateRealmCrewmate3", "Dipsomaniacal Cuisinier"),
              withChoice(1347, 2, ""))) {
        assertThat("_pirateRealmCrewmate", isSetTo("Pinch-Fisted Cryptobotanist"));
      }
    }

    @Test
    void canParseShips() {
      var responseText = html("request/test_choice_piraterealm_manowar.html");
      try (var _ =
          new Cleanups(
              withProperty("pirateRealmUnlockedManOWar", false),
              withProperty("pirateRealmUnlockedClipper", false),
              withChoice(1349, responseText))) {
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
      try (var _ =
          new Cleanups(
              withProperty("_pirateRealmShip", ""),
              withProperty("_pirateRealmShipSpeed", 0),
              withChoice(1349, decision, ""))) {
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
      try (var _ =
          new Cleanups(
              withProperty("_pirateRealmSailingTurns", 0), withChoice(choice, decision, ""))) {
        assertThat("_pirateRealmSailingTurns", isSetTo(addsTurn ? 1 : 0));
      }
    }

    @ParameterizedTest
    @CsvSource({"2,3", "3,3", "7,8", "8,8", "12,13", "13,13"})
    void questReflectsCompletedSail(final int startingStep, final int finishingStep) {
      try (var _ =
          new Cleanups(
              withQuestProgress(Quest.PIRATEREALM, startingStep),
              withProperty("_pirateRealmShipSpeed", 7),
              withProperty("_pirateRealmSailingTurns", 6),
              withChoice(1356, 1, ""))) {
        assertThat("_pirateRealmSailingTurns", isSetTo(7));
        var test2 = QuestDatabase.getQuest(Quest.PIRATEREALM);
        assertThat(Quest.PIRATEREALM, isStep(finishingStep));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void handlesOutsailingStorm(final boolean overshoot) {
      var responseText = html("request/test_choice_piraterealm_outsailed_storm.html");

      try (var _ =
          new Cleanups(
              withProperty("_pirateRealmSailingTurns", 0),
              withProperty("pirateRealmStormsEscaped", 0),
              withQuestProgress(Quest.PIRATEREALM, overshoot ? 3 : 2),
              withChoice(1362, 2, responseText))) {
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
      try (var _ =
          new Cleanups(
              withProperty("bwApronMealsEaten", 1),
              withItem(ItemPool.BLACK_AND_WHITE_APRON_MEAL_KIT),
              withChoice(1518, 1, responseText))) {
        assertThat("bwApronMealsEaten", isSetTo(2));
        assertThat(InventoryManager.getCount(ItemPool.BLACK_AND_WHITE_APRON_MEAL_KIT), is(0));
      }
    }

    @Test
    void handlesFull() {
      var responseText = html("request/test_choice_bw_apron_full.html");
      try (var _ =
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
      try (var _ =
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
      try (var _ =
          new Cleanups(
              withProperty("takerSpaceSpice", 0),
              withProperty("takerSpaceRum", 0),
              withProperty("takerSpaceAnchor", 0),
              withProperty("takerSpaceMast", 0),
              withProperty("takerSpaceSilk", 0),
              withProperty("takerSpaceGold", 0),
              withChoice(1537, html("request/test_campground_takerspace_" + frag + ".html")))) {
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
    try (var _ =
        new Cleanups(
            withProperty("_candyEggsDeviled", 1),
            withItem(ItemPool.BLACK_CANDY_HEART, 3),
            withPostChoice2(1544, 1, "a=3054", html("request/test_choice_devilegg.html")))) {
      assertThat("_candyEggsDeviled", isSetTo(2));
      assertThat(InventoryManager.getCount(ItemPool.BLACK_CANDY_HEART), is(2));
    }
  }

  @Nested
  class CyberRealm {
    @ParameterizedTest
    @CsvSource({"1, 1545", "2, 1547", "3, 1549"})
    public void cyberRealmHalfWaySetsTurns(int securityLevel, int choice) {
      String fileName = "request/test_cyber_zone" + securityLevel + "_choice1.html";
      String property = "_cyberZone" + securityLevel + "Turns";
      String html = html(fileName);
      try (var _ =
          new Cleanups(
              withProperty("_cyberZone1Turns", 5),
              withProperty("_cyberZone2Turns", 6),
              withProperty("_cyberZone3Turns", 7),
              withChoice(choice, html))) {
        assertThat("_cyberZone1Turns", isSetTo(securityLevel == 1 ? 10 : 5));
        assertThat("_cyberZone2Turns", isSetTo(securityLevel == 2 ? 10 : 6));
        assertThat("_cyberZone3Turns", isSetTo(securityLevel == 3 ? 10 : 7));
      }
    }

    @ParameterizedTest
    @CsvSource({"1, 1546", "2, 1548", "3, 1550"})
    public void cyberRealmFinalSetsTurns(int securityLevel, int choice) {
      String fileName = "request/test_cyber_zone" + securityLevel + "_choice1.html";
      String property = "_cyberZone" + securityLevel + "Turns";
      String html = html(fileName);
      try (var _ =
          new Cleanups(
              withProperty("_cyberZone1Turns", 15),
              withProperty("_cyberZone2Turns", 16),
              withProperty("_cyberZone3Turns", 17),
              withChoice(choice, html))) {
        assertThat("_cyberZone1Turns", isSetTo(securityLevel == 1 ? 20 : 15));
        assertThat("_cyberZone2Turns", isSetTo(securityLevel == 2 ? 20 : 16));
        assertThat("_cyberZone3Turns", isSetTo(securityLevel == 3 ? 20 : 17));
      }
    }
  }

  @Nested
  class HashingVise {
    AdventureResult cybeer = new AdventureResult("dedigitizer schematic: cybeer", 1, false);
    AdventureResult one = ItemPool.get(ItemPool.ONE);
    AdventureResult zero = ItemPool.get(ItemPool.ZERO);

    @Test
    void canSmashSchematicsIntoBits() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      try (var _ =
          new Cleanups(
              withHttpClientBuilder(builder), withItem(cybeer), withHandlingChoice(1551))) {
        client.addResponse(200, html("request/test_choice_hashing_vise_result.html"));

        // choice.php?iid=11198&pwd&whichchoice=1551&option=1
        var request = new GenericRequest("choice.php?iid=11198&whichchoice=1551&option=1", true);
        request.run();

        // You stay in the choice
        assertThat(ChoiceManager.handlingChoice, is(true));
        // The hashing vise smashed the schematic
        assertThat(InventoryManager.getCount(cybeer), is(0));

        int ones = InventoryManager.getCount(one);
        int zeroes = InventoryManager.getCount(zero);

        // This particular result has 0 (2) and 1 (14)
        assertThat((ones + zeroes), is(16));

        var requests = client.getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/choice.php", "iid=11198&whichchoice=1551&option=1");
      }
    }
  }

  @Nested
  class BoxingDaycare {
    @Test
    void canParseInstructorItems() {
      try (var _ =
          new Cleanups(
              withPostChoice1(0, 0),
              withProperty("daycareInstructorItem", 0),
              withProperty("daycareInstructorItemQuantity", 0))) {
        var req = new GenericRequest("choice.php?whichchoice=1336");
        req.responseText = html("request/test_choice_boxing_daycare.html");

        ChoiceManager.visitChoice(req);
        assertThat("daycareInstructorItem", isSetTo(5816));
        assertThat("daycareInstructorItemQuantity", isSetTo(11));
      }
    }

    @Test
    void canParseRecruits() {
      try (var _ = new Cleanups(withPostChoice1(0, 0), withProperty("_daycareRecruits", 0))) {
        var req = new GenericRequest("choice.php?whichchoice=1336");
        req.responseText = html("request/test_choice_boxing_daycare.html");

        ChoiceManager.visitChoice(req);
        assertThat("_daycareRecruits", isSetTo(1));
      }
    }

    @Test
    void canParseToddlersLate() {
      try (var _ = new Cleanups(withPostChoice1(0, 0), withProperty("daycareToddlers", 0))) {
        var req = new GenericRequest("choice.php?whichchoice=1336");
        req.responseText = html("request/test_choice_boxing_daycare.html");

        ChoiceManager.visitChoice(req);
        assertThat("daycareToddlers", isSetTo(2782));
      }
    }

    @Test
    void canParseGymEquipment() {
      try (var _ = new Cleanups(withPostChoice1(0, 0), withProperty("daycareEquipment", 0))) {
        var req = new GenericRequest("choice.php?whichchoice=1336");
        req.responseText = html("request/test_choice_boxing_daycare.html");

        ChoiceManager.visitChoice(req);
        assertThat("daycareEquipment", isSetTo(2875));
      }
    }

    @Test
    void handlesBrokenReturnRecruits() {
      try (var _ = new Cleanups(withPostChoice1(0, 0), withProperty("_daycareRecruits", 11))) {
        var req = new GenericRequest("choice.php?whichchoice=1336");
        req.responseText = "";

        ChoiceManager.visitChoice(req);
        assertThat("_daycareRecruits", isSetTo(11));
      }
    }

    @Test
    void handlesBrokenReturnInstructorCost() {
      try (var _ =
          new Cleanups(
              withPostChoice1(0, 0),
              withProperty("daycareInstructorItem", 11),
              withProperty("daycareInstructorItemQuantity", 69))) {
        var req = new GenericRequest("choice.php?whichchoice=1336");
        req.responseText = "";

        ChoiceManager.visitChoice(req);
        assertThat("daycareInstructorItem", isSetTo(11));
        assertThat("daycareInstructorItemQuantity", isSetTo(69));
      }
    }
  }

  @Nested
  class Zootomist {
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void canDetectSpecimensPrepared(int numUsed) {
      try (var _ = new Cleanups(withPostChoice1(0, 0), withProperty("zootSpecimensPrepared"))) {
        var req = new GenericRequest("choice.php?whichchoice=1555");
        req.responseText = html("request/test_choice_zoot_specimen_bench_" + numUsed + ".html");

        ChoiceManager.visitChoice(req);
        assertThat("zootSpecimensPrepared", isSetTo(numUsed));
      }
    }

    @Test
    void addsFamiliarExperience() {
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.MECHANICAL_SONGBIRD),
              withProperty("zootSpecimensPrepared"),
              withPostChoice1(
                  1555, 1, html("request/test_choice_zoot_specimen_bench_post.html")))) {
        assertThat("zootSpecimensPrepared", isSetTo(1));
        assertThat(KoLCharacter.getFamiliar().getTotalExperience(), equalTo(20));
      }
    }
  }

  @Nested
  class Leprecondo {
    @ParameterizedTest
    @CsvSource(
        value = {"1|1,2,3,4,5,6,8,9,12,13,21,24"},
        delimiter = '|')
    void canDetectFurnitureDiscovered(final String version, final String discoveries) {
      try (var _ = new Cleanups(withProperty("leprecondoDiscovered", ""))) {
        var req = new GenericRequest("choice.php?whichchoice=1556");
        req.responseText = html("request/test_choice_leprecondo_" + version + ".html");
        ChoiceManager.visitChoice(req);
        assertThat("leprecondoDiscovered", isSetTo(discoveries));
      }
    }

    @ParameterizedTest
    @CsvSource(
        value = {"1|21,12,8,9", "empty_spots|2,0,0,5", "mancave|17,10,19,25"},
        delimiter = '|')
    void canDetectFurnitureInstalled(final String version, final String installed) {
      try (var _ = new Cleanups(withProperty("leprecondoDiscovered", ""))) {
        var req = new GenericRequest("choice.php?whichchoice=1556");
        req.responseText = html("request/test_choice_leprecondo_" + version + ".html");
        ChoiceManager.visitChoice(req);
        assertThat("leprecondoInstalled", isSetTo(installed));
      }
    }

    @Test
    void handleNoMoreRearrangements() {
      try (var _ =
          new Cleanups(
              withProperty("leprecondoDiscovered", "1,2,3,4,5,6,8,9,13,21"),
              withProperty("leprecondoInstalled", ""))) {
        var req = new GenericRequest("choice.php?whichchoice=1556");
        req.responseText = html("request/test_choice_leprecondo_cannot_rearrange.html");
        ChoiceManager.visitChoice(req);
        // Discoveries left alone
        assertThat("leprecondoDiscovered", isSetTo("1,2,3,4,5,6,8,9,13,21"));
        // Installed items detected
        assertThat("leprecondoInstalled", isSetTo("9,8,13,21"));
      }
    }

    @ParameterizedTest
    @CsvSource(value = {"1,0", "cannot_rearrange,3"})
    void canDetectRearrangements(final String version, final String rearrangements) {
      try (var _ = new Cleanups(withProperty("_leprecondoRearrangements", "1"))) {
        var req = new GenericRequest("choice.php?whichchoice=1556");
        req.responseText = html("request/test_choice_leprecondo_" + version + ".html");
        ChoiceManager.visitChoice(req);
        assertThat("_leprecondoRearrangements", isSetTo(rearrangements));
      }
    }
  }

  @Nested
  class PeridotOfPeril {
    @CsvSource({"2_left,1", "blank,0", "none_left,3"})
    @ParameterizedTest
    void canDetectForeseesLeft(final String file, final int expected) {
      try (var _ = new Cleanups(withProperty("_perilsForeseen", "0"))) {
        var req = new GenericRequest("choice.php?whichchoice=1558");
        req.responseText = html("request/test_choice_peridot_foresee_" + file + ".html");
        ChoiceManager.visitChoice(req);
        assertThat("_perilsForeseen", isSetTo(expected));
      }
    }

    @Test
    void marksLocationAsSeenWhenGivenPeridotChoice() {
      try (var _ =
          new Cleanups(
              withProperty("_perilLocations", "113,115"),
              withLastLocation("The Outskirts of Cobb's Knob"))) {
        var req = new GenericRequest("choice.php?whichchoice=1557");
        req.responseText = html("request/test_choice_peridot_zone.html");
        ChoiceManager.visitChoice(req);
        assertThat("_perilLocations", isSetTo("113,114,115"));
      }
    }

    @Test
    void cantPerceiveOwnPeril() {
      try (var _ =
          new Cleanups(
              withContinuationState(),
              withPostChoice1(1558, 1, html("request/test_choice_peridot_foresee_self.html")))) {
        assertThat(StaticEntity.getContinuationState(), is(KoLConstants.MafiaState.ERROR));
      }
    }

    @Test
    void cantPerceivePerilOfRonin() {
      try (var _ =
          new Cleanups(
              withContinuationState(),
              withPostChoice1(1558, 1, html("request/test_choice_peridot_foresee_ronin.html")))) {
        assertThat(StaticEntity.getContinuationState(), is(KoLConstants.MafiaState.ERROR));
      }
    }

    @Test
    void detectsMaxPeril() {
      try (var _ =
          new Cleanups(
              withProperty("_perilsForeseen", 0),
              withContinuationState(),
              withPostChoice1(1558, 1, html("request/test_choice_peridot_foresee_max.html")))) {
        assertThat("_perilsForeseen", isSetTo(3));
      }
    }

    @Test
    void successfullyPerceivesPeril() {
      try (var _ =
          new Cleanups(
              withContinuationState(),
              withProperty("_perilsForeseen", 0),
              withPostChoice1(1558, 1, html("request/test_choice_peridot_foresee_success.html")))) {
        assertThat(StaticEntity.getContinuationState(), is(KoLConstants.MafiaState.CONTINUE));
        assertThat("_perilsForeseen", isSetTo(1));
      }
    }
  }

  @Nested
  class CoolerYeti {
    @Test
    void allChoicesAvailable() {
      try (var _ =
          new Cleanups(
              withProperty("_coolerYetiAdventures", true),
              withProperty("coolerYetiMode", "adventures"),
              withChoice(1560, html("request/test_cooler_yeti_all_choices.html")))) {
        assertThat("_coolerYetiAdventures", isSetTo(false));
        assertThat("coolerYetiMode", isSetTo(""));
      }
    }

    @Test
    void busyWithCooler() {
      try (var _ =
          new Cleanups(
              withProperty("_coolerYetiAdventures", false),
              withProperty("coolerYetiMode", ""),
              withChoice(1560, html("request/test_cooler_yeti_busy_with_cooler.html")))) {
        assertThat("_coolerYetiAdventures", isSetTo(true));
        assertThat("coolerYetiMode", isSetTo("adventures"));
      }
    }

    @Test
    void alwaysFriendsDoesNotChangeMode() {
      try (var _ =
          new Cleanups(
              withProperty("coolerYetiMode", "adventures"),
              withPostChoice2(1560, 1, html("request/test_cooler_yeti_always_friends.html")))) {
        assertThat("coolerYetiMode", isSetTo("adventures"));
      }
    }

    @Test
    void impossiblyColdSetsAdventures() {
      try (var _ =
          new Cleanups(
              withProperty("_coolerYetiAdventures", false),
              withProperty("coolerYetiMode", ""),
              withPostChoice2(1560, 2, html("request/test_cooler_yeti_always_friends.html")))) {
        assertThat("_coolerYetiAdventures", isSetTo(true));
        assertThat("coolerYetiMode", isSetTo("adventures"));
      }
    }
  }

  @Nested
  class CatalogCard {
    @Test
    void parsesChoiceAdventure() {
      try (var _ =
          new Cleanups(
              withProperty("merkinCatalogChoices"),
              withChoice(704, html("request/test_choice_catalog_0.html")))) {
        assertThat(
            "merkinCatalogChoices",
            isSetTo(
                "AF531.55:1:unknown,AW393.55:2:unknown,CF473.85:11:unknown,CK171.48:4:unknown,DZ919.41:13:unknown,LS807.86:10:unknown,NZ395.76:5:unknown,OF298.41:12:unknown,RH380.67:9:unknown,WG526.35:3:unknown,XQ903.56:6:unknown,ZI598.93:8:unknown,ZM359.31:7:unknown"));
      }
    }

    @Test
    void postChoiceUpdatesSpoilers() {
      try (var _ =
          new Cleanups(
              withProperty("merkinCatalogChoices", "AF531.55:1:unknown,AW393.55:2:unknown"),
              withPostChoice2(704, 1, html("request/test_choice_catalog_post_0.html")))) {
        assertThat("merkinCatalogChoices", isSetTo("AF531.55:1:stats,AW393.55:2:unknown"));
      }
    }

    @Test
    void keepsSpoilersInReshuffle() {
      try (var _ =
          new Cleanups(
              withProperty(
                  "merkinCatalogChoices",
                  "AF531.55:1:stats,AW393.55:2:unknown,CF473.85:11:unknown,CK171.48:4:unknown,DZ919.41:13:unknown,LS807.86:10:unknown,NZ395.76:5:unknown,OF298.41:12:unknown,RH380.67:9:unknown,WG526.35:3:unknown,XQ903.56:6:unknown,ZI598.93:8:unknown,ZM359.31:7:unknown"),
              withChoice(704, html("request/test_choice_catalog_1.html")))) {
        assertThat(
            "merkinCatalogChoices",
            isSetTo(
                "AF531.55:8:stats,AW393.55:10:unknown,CF473.85:9:unknown,CK171.48:1:unknown,DZ919.41:2:unknown,NZ395.76:4:unknown,OF298.41:11:unknown,RH380.67:7:unknown,WG526.35:3:unknown,XQ903.56:5:unknown,ZI598.93:12:unknown,ZM359.31:6:unknown"));
      }
    }
  }

  @Test
  void seaPathFreeKing() {
    try (var _ =
        new Cleanups(
            withProperty("kingLiberated", false),
            withPostChoice2(1565, 1, html("request/test_choice_sea_path_free_king.html")))) {
      assertThat("kingLiberated", isSetTo(true));
    }
  }

  @Nested
  class DreadScroll {
    @Test
    void failureAddsGuessesToBlank() {
      try (var _ =
          new Cleanups(
              withProperty("dreadScrollGuesses"),
              withPostChoice1(
                  703,
                  1,
                  "pro1=4&pro2=1&pro3=2&pro4=1&pro5=1&pro6=1&pro7=1&pro8=3",
                  html("request/test_choice_dreadscroll_failure.html")))) {
        assertThat("dreadScrollGuesses", isSetTo("41211113:2"));
      }
    }

    @Test
    void failureAddsGuesses() {
      try (var _ =
          new Cleanups(
              withProperty("dreadScrollGuesses", "42211113:3"),
              withPostChoice1(
                  703,
                  1,
                  "pro1=4&pro2=1&pro3=2&pro4=1&pro5=1&pro6=1&pro7=1&pro8=3",
                  html("request/test_choice_dreadscroll_failure.html")))) {
        assertThat("dreadScrollGuesses", isSetTo("42211113:3,41211113:2"));
      }
    }

    @Test
    void successLeavesGuesses() {
      try (var _ =
          new Cleanups(
              withProperty("dreadScrollGuesses", "31424213:3"),
              withPostChoice1(
                  703,
                  1,
                  "pro1=3&pro2=1&pro3=4&pro4=2&pro5=4&pro6=2&pro7=1&pro8=4",
                  html("request/test_choice_dreadscroll_success.html")))) {
        assertThat("dreadScrollGuesses", isSetTo("31424213:3"));
      }
    }
  }

  @Test
  void seadentWaveZone() {
    try (var _ =
        new Cleanups(
            withProperty("_seadentWaveZone"),
            withProperty("_seadentWaveUsed"),
            withPostChoice2(1566, 1, html("request/test_choice_summon_waves.html")))) {
      assertThat("_seadentWaveZone", isSetTo("Barf Mountain"));
      assertThat("_seadentWaveUsed", isSetTo(true));
    }
  }

  @Test
  void visitingCrimboPastSkeletonSetsDailies() {
    try (var _ =
        new Cleanups(
            withProperty("_crimboPastSmokingPope"),
            withProperty("_crimboPastPrizeTurkey"),
            withProperty("_crimboPastMedicalGruel"),
            withProperty("_crimboPastDailySpecial"),
            withProperty("_crimboPastDailySpecialItem"),
            withProperty("_crimboPastDailySpecialPrice"))) {
      var req = new GenericRequest("choice.php?whichchoice=1567");
      req.responseText = html("request/test_choice_crimbo_past_none.html");

      ChoiceManager.visitChoice(req);

      assertThat("_crimboPastSmokingPope", isSetTo(true));
      assertThat("_crimboPastPrizeTurkey", isSetTo(true));
      assertThat("_crimboPastMedicalGruel", isSetTo(true));
      assertThat("_crimboPastDailySpecial", isSetTo(false));
      assertThat("_crimboPastDailySpecialItem", isSetTo(ItemPool.ELVEN_SOCKS));
      assertThat("_crimboPastDailySpecialPrice", isSetTo(442));

      var items = CoinmastersDatabase.getBuyItems("Skeleton of Crimbo Past");
      assertThat(items, hasSize(4));
      assertThat(items, hasItem(ItemPool.get(ItemPool.ELVEN_SOCKS)));
      var prices = CoinmastersDatabase.getBuyPrices("Skeleton of Crimbo Past");
      assertThat(prices.get(ItemPool.ELVEN_SOCKS), is(442));
    }
  }

  @Test
  void diggingUpGiftLogsNote() {
    RequestLoggerOutput.startStream();
    try (var _ =
        new Cleanups(
            withNoItems(),
            withPostChoice2(1591, 1, html("request/test_choice_diggin_up_a_gift.html")))) {
      var stream = RequestLoggerOutput.stopStream();
      assertThat(stream, containsString("Note: bottom text"));
    }
  }

  @Test
  void canParseArchSpadeUses() {
    try (var _ = new Cleanups(withPostChoice1(0, 0), withProperty("_archSpadeDigs", 0))) {
      var req = new GenericRequest("choice.php?whichchoice=1596");
      req.responseText = html("request/test_choice_arch_spade.html");

      ChoiceManager.visitChoice(req);
      assertThat("_archSpadeDigs", isSetTo(1));
    }
  }

  @Nested
  class BaseballDiamond {
    @Test
    void visitingIncrementsInnings() {
      try (var _ =
          new Cleanups(
              withProperty("_baseballInnings", 1),
              withChoice(1598, html("request/test_choice_baseball_no_bats.html")))) {
        assertThat("_baseballInnings", isSetTo(2));
      }
    }

    @Test
    void visitingMidPitchDoesNotIncrementInnings() {
      try (var _ =
          new Cleanups(
              withProperty("_baseballInnings", 1),
              withChoice(1598, html("request/test_choice_baseball_ice.html")))) {
        assertThat("_baseballInnings", isSetTo(1));
      }
    }

    @Test
    void iceHitBanishes() {
      try (var _ =
          new Cleanups(
              withBanishedMonsters(""),
              withPostChoice2(1598, 2, html("request/test_choice_baseball_ice.html")))) {
        assertThat(
            "banishedMonsters", hasStringValue(startsWith("chalkdust wraith:Baseball Diamond:")));
      }
    }

    @Test
    void skullBallPref() {
      try (var _ =
          new Cleanups(
              withProperty("_skullballMonster"),
              withPostChoice2(1598, 3, html("request/test_choice_baseball_skull.html")))) {
        assertThat("_skullballMonster", isSetTo("skullery maid"));
      }
    }

    @Test
    void curveBallPrefs() {
      try (var _ =
          new Cleanups(
              withProperty("_curveballMonster"),
              withProperty("_curveballFightsLeft"),
              withPostChoice2(1598, 3, html("request/test_choice_baseball_curve.html")))) {
        assertThat("_curveballMonster", isSetTo("modern zmobie"));
        assertThat("_curveballFightsLeft", isSetTo(3));
      }
    }

    @Test
    void beanBallPref() {
      try (var _ =
          new Cleanups(
              withProperty("_beanballMonster"),
              withPostChoice2(1598, 4, html("request/test_choice_baseball_bean.html")))) {
        assertThat("_beanballMonster", isSetTo("Hellion"));
      }
    }

    @Test
    void cheddarTracks() {
      try (var _ =
          new Cleanups(
              withTrackedMonsters(""),
              withPostChoice2(1598, 4, html("request/test_choice_baseball_cheddar.html")))) {
        assertThat("trackedMonsters", hasStringValue(startsWith("P imp:Baseball Diamond:")));
      }
    }

    @Test
    void screwBallPref() {
      try (var _ =
          new Cleanups(
              withProperty("_screwballMonster"),
              withPostChoice2(1598, 5, html("request/test_choice_baseball_screw.html")))) {
        assertThat("_screwballMonster", isSetTo("MagiMechTech MechaMech"));
      }
    }
  }

  @Nested
  class CupOf13s {
    @Test
    void setsJewelsOnVisit() {
      try (var _ =
          new Cleanups(
              withProperty("_cupOf13sJewels"),
              withChoice(1601, html("request/test_cup_of_13s_start.html")))) {
        assertThat("_cupOf13sJewels", isSetTo(7));
      }
    }

    @Test
    void decrementsItemsAndJewelsOnDrink() {
      try (var _ =
          new Cleanups(
              withProperty("_cupOf13sJewels", 7),
              withItem(ItemPool.LOOSE_PURSE_STRINGS, 10),
              withPostChoice2(
                  1601,
                  1,
                  "whichitem1=7031&whichitem2=7031&whichitem3=7031",
                  html("request/test_cup_of_13s_drink.html")))) {
        assertThat("_cupOf13sJewels", isSetTo(1));
        assertThat(InventoryManager.getCount(ItemPool.LOOSE_PURSE_STRINGS), equalTo(7));
      }
    }
  }

  @Nested
  class BlueVsRed {
    @Test
    void readsMonsterIdFromNC() {
      try (var _ =
          new Cleanups(withChoice(1619, html("request/test_bluevsred_nc_monsterid.html")))) {
        assertThat("lastBlueVsRedNCMonster", isSetTo(1232));
      }
    }

    @Test
    void incrementsDesertExplorationInBlueVsRed() {
      try (var _ =
          new Cleanups(
              withProperty("desertExploration", 7),
              withLastLocation("The Arid, Extra-Dry Desert"),
              withEquipped(Slot.OFFHAND, ItemPool.UV_RESISTANT_COMPASS),
              withPostChoice2(1622, 1, html("request/test_bluevsred_desert_exploration.html")))) {
        assertThat("desertExploration", isSetTo(9));
      }
    }

    @Test
    void tracksWarProgressInBlueVsRed() {
      try (var _ =
          new Cleanups(
              withProperty("fratboysDefeated", 72),
              withProperty("lastBlueVsRedNCMonster", 423),
              withProperty("sidequestOrchardCompleted", "hippy"),
              withProperty("sidequestFarmCompleted", "hippy"),
              withProperty("sidequestLighthouseCompleted", "hippy"),
              withPostChoice2(1619, 1, html("request/test_bluevsred_war_progress.html")))) {
        assertThat("fratboysDefeated", isSetTo(80));
      }
    }

    @Test
    void decreasesEvilnessInBlueVsRed() {
      try (var _ =
          new Cleanups(
              withProperty("lastBlueVsRedNCMonster", 190), // gaunt ghuol
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptTotalEvilness", 200),
              withPostChoice2(1624, 1, html("request/test_bluevsred_evilometer.html")))) {
        assertThat("cyrptCrannyEvilness", isSetTo(49));
        assertThat("cyrptTotalEvilness", isSetTo(199));
      }
    }
  }

  @Nested
  class VotingBooth {
    @Test
    void parsesVotingBooth() {
      try (var _ =
          new Cleanups(
              withProperty("_voteMonster1"),
              withProperty("_voteMonster2"),
              withProperty("_voteLocal1"),
              withProperty("_voteLocal2"),
              withProperty("_voteLocal3"),
              withProperty("_voteLocal4"))) {
        var req = new GenericRequest("choice.php?forceoption=0");
        req.responseText = html("request/test_choice_votingbooth.html");

        ChoiceManager.preChoice(req);
        ChoiceManager.visitChoice(req);

        assertThat("_voteMonster1", isSetTo("angry ghost"));
        assertThat("_voteMonster2", isSetTo("government bureaucrat"));
        assertThat("_voteLocal1", isSetTo("Ranged Damage Percent: +100"));
        assertThat("_voteLocal2", isSetTo("Initiative: -30"));
        assertThat("_voteLocal3", isSetTo("Experience: +3"));
        assertThat("_voteLocal4", isSetTo("Mysticality Experience: +4"));
      }
    }

    @Test
    void parsesVoteResult() {
      try (var _ =
          new Cleanups(
              withProperty("_voteMonster1", "angry ghost"),
              withProperty("_voteMonster2", "government bureaucrat"),
              withProperty("_voteLocal1", "Ranged Damage Percent: +100"),
              withProperty("_voteLocal2", "Initiative: -30"),
              withProperty("_voteLocal3", "Experience: +3"),
              withProperty("_voteLocal4", "Mysticality Experience: +4"),
              withPostChoice1(
                  1331,
                  1,
                  "g=1&local[0]=1&local[1]=3",
                  html("request/test_choice_votingbooth_voted.html")))) {
        assertThat("_voteModifier", isSetTo("Initiative: -30, Mysticality Experience: +4"));
      }
    }
  }
}
