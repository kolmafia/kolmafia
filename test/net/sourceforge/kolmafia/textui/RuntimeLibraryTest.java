package net.sourceforge.kolmafia.textui;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.getPostRequestBody;
import static internal.helpers.Networking.html;
import static internal.helpers.Networking.json;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withAdventuresSpent;
import static internal.helpers.Player.withBanishedPhyla;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withCurrentRun;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withFamiliarInTerrariumWithItem;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInDisplay;
import static internal.helpers.Player.withItemInShop;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withNextMonster;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withNoEffects;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSign;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static internal.helpers.Player.withTrackedMonsters;
import static internal.helpers.Player.withTrackedPhyla;
import static internal.helpers.Player.withTurnsPlayed;
import static internal.helpers.Player.withUnequipped;
import static internal.helpers.Player.withValueOfAdventure;
import static internal.helpers.Utilities.deleteSerFiles;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.alibaba.fastjson2.JSONObject;
import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.network.FakeHttpClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.session.GreyYouManager;
import net.sourceforge.kolmafia.session.MallPriceManager;
import net.sourceforge.kolmafia.textui.command.AbstractCommandTestBase;
import net.sourceforge.kolmafia.utilities.NullStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class RuntimeLibraryTest extends AbstractCommandTestBase {

  private static final String TESTUSER = "RuntimeLibraryTestUser";

  @BeforeEach
  public void initEach() {
    KoLCharacter.reset(TESTUSER);
    deleteSerFiles(TESTUSER);
    KoLCharacter.reset(true);
    Preferences.reset(TESTUSER);
  }

  public RuntimeLibraryTest() {
    this.command = "ash";
  }

  public RuntimeLibraryTest getInstance() {
    return this;
  }

  @Test
  void normalMonsterExpectedDamage() {
    String output = execute("expected_damage($monster[blooper])");

    assertContinueState();
    assertThat(output, containsString("Returned: 35"));
  }

  @Test
  void ninjaSnowmanAssassinExpectedDamage() {
    String output = execute("expected_damage($monster[ninja snowman assassin])");

    assertContinueState();
    assertThat(output, containsString("Returned: 297"));
  }

  @Test
  void getPermedSkills() {
    CharSheetRequest.parseStatus(html("request/test_charsheet_normal.html"));

    String outputHardcore = execute("get_permed_skills()[$skill[Nimble Fingers]]");

    assertContinueState();
    assertThat(outputHardcore, containsString("Returned: true"));

    String outputSoftcore = execute("get_permed_skills()[$skill[Entangling Noodles]]");

    assertContinueState();
    assertThat(outputSoftcore, containsString("Returned: false"));

    String outputUnpermed =
        execute(
            "if (get_permed_skills() contains $skill[Emotionally Chipped]) {print(\"permed\");} else {print(\"unpermed\");}");

    assertContinueState();
    assertThat(outputUnpermed, containsString("unpermed"));
  }

  @Test
  void zapWandUnavailable() {
    String output = execute("get_zap_wand()");

    assertContinueState();
    assertThat(output, containsString("Returned: none"));
  }

  @Test
  void zapWandAvailable() {
    final var cleanups = new Cleanups(withItem("marble wand"));

    try (cleanups) {
      String output = execute("get_zap_wand()");

      assertContinueState();
      assertThat(output, containsString("name => marble wand"));
    }
  }

  @Test
  void floundryLocations() {
    // don't try to visit the fireworks shop
    Preferences.setBoolean("_fireworksShop", true);

    var cleanups = withNextResponse(200, html("request/test_clan_floundry.html"));

    try (cleanups) {
      String output = execute("get_fishing_locations()");

      assertContinueState();
      assertThat(output, containsString("Returned: aggregate location [string]"));
      assertThat(output, containsString("bass => Guano Junction"));
      assertThat(output, containsString("carp => Pirates of the Garbage Barges"));
      assertThat(output, containsString("cod => Thugnderdome"));
      assertThat(output, containsString("hatchetfish => The Skeleton Store"));
      assertThat(output, containsString("trout => The Haunted Conservatory"));
      assertThat(output, containsString("tuna => The Oasis"));
    }
  }

  @Test
  void testPrintHtmlDoesNotWriteToSessionLog() {
    var html = "<td><p>word</p></td>";

    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.setSessionStream(out);

      // Confirm that print_html doesn't log to session
      execute("print_html('" + html + "')");

      assertThat(ostream.toString(), is(""));
      RequestLogger.setSessionStream(NullStream.INSTANCE);
    }
  }

  @Test
  void testPrintHtmlFalseDoesNotWriteToSessionLog() {
    var html = "<td><p>word</p></td>";

    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.setSessionStream(out);

      // Confirm that print_html doesn't log to session
      execute("print_html('" + html + "', false)");

      assertThat(ostream.toString(), is(""));
      RequestLogger.setSessionStream(NullStream.INSTANCE);
    }
  }

  @Test
  void testPrintHtmlTrueWritesToSessionLog() {
    var html = "<td><p>word</p></td>";

    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.setSessionStream(out);

      // Confirm that print_html doesn't log to session
      execute("print_html('" + html + "', true)");

      assertThat(ostream.toString(), is("> word\n"));
      RequestLogger.setSessionStream(NullStream.INSTANCE);
    }
  }

  @Test
  void testPrintHtmlWritesSingleLineToSessionLog() {
    var html = "<td><p>word1</p><p>word2</p></td>";

    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.setSessionStream(out);

      // Confirm that print_html doesn't log to session
      execute("print_html('" + html + "', true)");

      assertThat(ostream.toString(), is("> word1word2\n"));
      RequestLogger.setSessionStream(NullStream.INSTANCE);
    }
  }

  @Test
  void testPrintHtmlWritesMultipleLinesToSessionLog() {
    var html = "<td><p>word1</p><br><p>word2</p></td>";

    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.setSessionStream(out);

      // Confirm that print_html doesn't log to session
      execute("print_html('" + html + "', true)");

      assertThat(ostream.toString(), is("> word1\n> word2\n"));
      RequestLogger.setSessionStream(NullStream.INSTANCE);
    }
  }

  @Nested
  class ExpectedCmc {
    @BeforeEach
    public void beforeEach() {
      HttpClientWrapper.setupFakeClient();
    }

    @Test
    void canVisitCabinet() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withHandlingChoice(false));

      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_choice_cmc_frozen_jeans.html"));
        String output = execute("expected_cold_medicine_cabinet()");
        assertThat(
            output,
            equalTo(
                """
                    Returned: aggregate item [string]
                    booze => Doc's Fortifying Wine
                    equipment => frozen jeans
                    food => frozen tofu pop
                    pill => Breathitin&trade;
                    potion => anti-odor cream
                    """));
      }
    }

    @Test
    void canHandleUnexpectedCabinetResponse() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withHandlingChoice(false));

      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, "huh?");
        String output = execute("expected_cold_medicine_cabinet()");
        assertThat(
            output,
            equalTo(
                """
                    Could not parse cabinet.
                    Returned: aggregate item [string]
                    booze => none
                    equipment => none
                    food => none
                    pill => none
                    potion => none
                    """));
      }
    }

    @Test
    void canGuessCabinet() {
      var cleanups =
          new Cleanups(withProperty("lastCombatEnvironments", "iiiiiiiiiiioooouuuuu"), withFight());

      try (cleanups) {
        String output = execute("expected_cold_medicine_cabinet()");
        assertThat(
            output,
            equalTo(
                """
                    Returned: aggregate item [string]
                    booze => Doc's Medical-Grade Wine
                    equipment => ice crown
                    food => none
                    pill => Extrovermectin&trade;
                    potion => none
                    """));
      }
    }

    @Test
    void canGuessCabinetWithUnknownPill() {
      var cleanups =
          new Cleanups(withProperty("lastCombatEnvironments", "????????????????????"), withFight());

      try (cleanups) {
        String output = execute("expected_cold_medicine_cabinet()");
        assertThat(
            output,
            equalTo(
                """
                    Returned: aggregate item [string]
                    booze => Doc's Medical-Grade Wine
                    equipment => ice crown
                    food => none
                    pill => none
                    potion => none
                    """));
      }
    }
  }

  @Test
  void canSeeGreyYouMonsterAbsorbs() {
    var cleanups = new Cleanups(GreyYouManager::resetAbsorptions);

    try (cleanups) {
      KoLCharacter.setPath(Path.GREY_YOU);

      String name1 = "oil baron";
      MonsterData monster1 = MonsterDatabase.findMonster(name1);
      GreyYouManager.absorbMonster(monster1, "a lot of potential energy!");
      String name2 = "warwelf";
      MonsterData monster2 = MonsterDatabase.findMonster(name2);
      GreyYouManager.absorbMonster(monster2, "a lot of potential energy!");

      String output = execute("absorbed_monsters()");
      assertThat(
          output,
          equalTo(
              """
                Returned: aggregate boolean [monster]
                warwelf => true
                oil baron => true
                """));
    }
  }

  @Nested
  class Zap {
    @Test
    void noWandReturnsNone() {
      var cleanups = withItem("Dreadsylvanian spooky pocket");

      try (cleanups) {
        String output = execute("zap($item[Dreadsylvanian spooky pocket])");
        assertThat(output, containsString("Returned: none"));
      }
    }

    @Test
    void canZapItem() {
      var cleanups =
          new Cleanups(
              withItem("hexagonal wand"),
              withItem("Dreadsylvanian spooky pocket"),
              withNextResponse(200, html("request/test_zap_pockets.html")));

      try (cleanups) {
        String output = execute("zap($item[Dreadsylvanian spooky pocket])");
        assertThat(output, containsString("Returned: Dreadsylvanian hot pocket"));
      }
    }
  }

  @Nested
  class Equip {
    @Test
    void canEquipItem() {
      var cleanups = new Cleanups(withEquippableItem("crowbar"));

      try (cleanups) {
        String output = execute("equip($item[crowbar])");
        assertThat(output, endsWith("Returned: true\n"));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void canEquipItemToSlot(final boolean switched) {
      var cleanups = new Cleanups(withEquippableItem("crowbar"), withFamiliar(FamiliarPool.HAND));

      var a = "$item[crowbar]";
      var b = "$slot[familiar]";
      var command = "equip(" + (switched ? a : b) + ", " + (switched ? b : a) + ")";

      try (cleanups) {
        String output = execute(command);
        assertThat(output, endsWith("Returned: true\n"));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void canEquipItemToFamiliarInTerrarium(final boolean switched) {
      var cleanups =
          new Cleanups(withItem("lead necklace"), withFamiliarInTerrarium(FamiliarPool.BADGER));

      var a = "$item[lead necklace]";
      var b = "$familiar[Astral Badger]";
      var command = "equip(" + (switched ? a : b) + ", " + (switched ? b : a) + ")";

      try (cleanups) {
        String output = execute(command);
        assertThat(output, endsWith("Returned: true\n"));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void canEquipItemToCurrentFamiliar(final boolean switched) {
      var cleanups = new Cleanups(withItem("lead necklace"), withFamiliar(FamiliarPool.BADGER));

      var a = "$item[lead necklace]";
      var b = "$familiar[Astral Badger]";
      var command = "equip(" + (switched ? a : b) + ", " + (switched ? b : a) + ")";

      try (cleanups) {
        String output = execute(command);
        assertThat(output, endsWith("Returned: true\n"));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cannotEquipUnequippableItemToFamiliarInTerrarium(final boolean switched) {
      var cleanups =
          new Cleanups(
              withItem("gatorskin umbrella"), withFamiliarInTerrarium(FamiliarPool.BADGER));

      var a = "$item[gatorskin umbrella]";
      var b = "$familiar[Astral Badger]";
      var command = "equip(" + (switched ? a : b) + ", " + (switched ? b : a) + ")";

      try (cleanups) {
        String output = execute(command);
        assertThat(output, endsWith("Returned: false\n"));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void equippedAmountIncludesFamiliarsIfSpecified(final boolean include) {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrariumWithItem(FamiliarPool.MOSQUITO, ItemPool.LEAD_NECKLACE),
              withFamiliarInTerrariumWithItem(FamiliarPool.POTATO, ItemPool.LEAD_NECKLACE),
              withFamiliar(FamiliarPool.GOAT),
              withEquipped(Slot.FAMILIAR, ItemPool.LEAD_NECKLACE));

      try (cleanups) {
        String output = execute("equipped_amount($item[lead necklace], " + include + ")");
        assertThat(output, endsWith("Returned: " + (include ? 3 : 1) + "\n"));
      }
    }
  }

  @Nested
  class PathFunctions {
    @ParameterizedTest
    @ValueSource(
        strings = {
          "boolean test(string p) { return p == \"Trendy\"; } test($path[Trendy])",
          "string p = my_path(); (p == \"Trendy\")",
          "(my_path() == \"Trendy\")",
          "my_path().starts_with(\"Tre\")",
          "boolean test() { switch (my_path()) { case \"Trendy\": return true; default: return false; } } test()",
          "boolean test(string path_name) { switch (path_name) { case $path[Trendy]: return true; default: return false; } } test(\"Trendy\")",
          "($strings[Trendy] contains my_path())",
          "($paths[Trendy] contains \"Trendy\")"
        })
    void myPathCoercesToString(String command) {
      // my_path() used to return a string, we want to make sure that we don't break old scripts
      // where possible
      var cleanups = new Cleanups(withPath(Path.TRENDY));

      try (cleanups) {
        String output = execute(command);
        assertThat(output, endsWith("Returned: true\n"));
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "(my_path() == \"None\")",
          "boolean test() { switch (my_path()) { case \"None\": return true; default: return false; } } test()",
        })
    void nonePathIsTitleCases(String command) {
      // Unrestricted used to be "None" but now it's technically "none". These tests make sure
      // coercion is handling this
      // We know it won't work in one case: "None" == my_path(). So if you wrote that, you're SOL
      // :)
      var cleanups = new Cleanups(withPath(Path.NONE));

      try (cleanups) {
        String output = execute(command);
        assertThat(output, endsWith("Returned: true\n"));
      }
    }

    @Test
    void myPathCoercionWorksInJs() {
      getInstance().command = "js";

      var cleanups = new Cleanups(withPath(Path.TRENDY));

      try (cleanups) {
        String output = execute("myPath() == \"Trendy\"");
        assertThat(output, endsWith("Returned: true\n"));
      }

      getInstance().command = "ash";
    }
  }

  @Nested
  class BufferFunctions {
    @ParameterizedTest
    @ValueSource(
        strings = {"buffer b = \"Initial content\"; (b.to_string() == \"Initial content\")"})
    void stringCanInitializeBuffer(String command) {
      String output = execute(command);
      assertThat(output, endsWith("Returned: true\n"));
    }
  }

  @Test
  void environmentIsLowercase() {
    String output = execute("($location[Noob Cave].environment == 'underground')");
    assertThat(output, endsWith("Returned: true\n"));
  }

  @Test
  void diffLevelIsLowercase() {
    String output = execute("($location[Noob Cave].difficulty_level == 'low')");
    assertThat(output, endsWith("Returned: true\n"));
  }

  @Nested
  class ConcoctionPrice {
    @BeforeAll
    public static void setupPrices() {
      MallPriceDatabase.savePricesToFile = false;
      addSearchResults(ItemPool.VYKEA_INSTRUCTIONS, 1);
      addSearchResults(ItemPool.VYKEA_RAIL, 10);
      addSearchResults(ItemPool.VYKEA_PLANK, 100);
      addSearchResults(ItemPool.VYKEA_DOWEL, 1000);
      addSearchResults(ItemPool.VYKEA_BRACKET, 10000);

      addSearchResults(ItemPool.LUMP_OF_BRITUMINOUS_COAL, 2);
      addNpcResults(ItemPool.LOOSE_PURSE_STRINGS);
    }

    @AfterAll
    public static void tearDown() {
      MallPriceDatabase.savePricesToFile = true;
      MallPriceManager.reset();
    }

    @ParameterizedTest
    @CsvSource({
      "level 1 couch, 551",
      "level 2 couch, 1551",
      "level 3 couch, 11551",
      "level 1 ceiling fan, 50501",
    })
    public void getConcoctionVykeaPrice(String vykea, int price) {
      String output = execute("concoction_price($vykea[" + vykea + "])");
      assertThat(output, endsWith("Returned: " + price + "\n"));
    }

    @Test
    public void getConcoctionHalfPurse() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.TENDER_HAMMER), withAdventuresLeft(2), withValueOfAdventure(0));

      try (cleanups) {
        String output = execute("concoction_price($item[Half a Purse])");
        assertThat(output, endsWith("Returned: 102\n"));
      }
    }

    @Test
    public void getConcoctionHalfPurseWhenSmithingExpensive() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.TENDER_HAMMER), withAdventuresLeft(2), withValueOfAdventure(10000));

      try (cleanups) {
        String output = execute("concoction_price($item[Half a Purse])");
        assertThat(output, endsWith("Returned: 10102\n"));
      }
    }

    @Test
    public void setCcs() {
      String output = execute("set_ccs(\"default\");");
      assertThat(output, endsWith("Returned: true\n"));
      output = execute("set_ccs(\"fhghqwgads\");");
      assertThat(output, endsWith("Returned: false\n"));
    }

    private static void addNpcResults(int itemId) {
      List<PurchaseRequest> results =
          List.of(Objects.requireNonNull(NPCStoreDatabase.getPurchaseRequest(itemId)));
      updateResults(itemId, results);
    }

    private static void addSearchResults(int itemId, int price) {
      List<PurchaseRequest> results =
          List.of(new MallPurchaseRequest(itemId, 100, 1, "Test Shop", price, 100, true));
      updateResults(itemId, results);
    }

    private static void updateResults(int itemId, List<PurchaseRequest> results) {
      MallPriceManager.saveMallSearch(itemId, results);
      MallPriceManager.updateMallPrice(ItemPool.get(itemId), results);
    }
  }

  @Test
  void canSeeDaycount() {
    var cleanups = withFamiliar(FamiliarPool.TRICK_TOT);

    try (cleanups) {
      String text = html("request/test_status.json");
      JSONObject JSON = json(text);
      ApiRequest.parseStatus(JSON);
      String output = execute("daycount()");
      assertContinueState();
      assertThat(output, is("Returned: 7302\n"));
    } finally {
      /*
       ApiRequest.parseStatus sets a password hash which persists to other tests and causes them to pass or fail
       based upon whether this test was run first, or not.  Explicitly clear the hash when this test ends.
      */
      ApiRequest.setPasswordHash("");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"16000", "zero placeholder", "[zero placeholder]", "[16000]"})
  void canIdentifySkill(final String skillIdentifier) {
    String output = execute("$skill[" + skillIdentifier + "].name");
    assertThat(output, endsWith("Returned: [zero placeholder]\n"));
  }

  @Nested
  class EightBitPoints {
    @Test
    void zeroPointsInNon8BitZone() {
      String output = execute("eight_bit_points($location[The Dire Warren])");
      assertThat(output, endsWith("Returned: 0\n"));
    }

    @ParameterizedTest
    @CsvSource({"red, 300", "black, 150"})
    void fungusPlains(String color, int points) {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.SYNTHESIS_GREED), // 300% meat drop
              withEquipped(ItemPool.CARPE), // 50% meat drop
              withProperty("8BitColor", color));

      try (cleanups) {
        String output = execute("eight_bit_points($location[The Fungus Plains])");
        assertThat(output, endsWith("Returned: " + points + "\n"));
      }
    }

    @ParameterizedTest
    @CsvSource({"green, 380", "black, 190"})
    void herosField(String color, int points) {
      var cleanups =
          new Cleanups(
              withEffect("Frosty"), // 100% item drop
              withEffect("Certainty"), // 100% item drop
              withEffect(EffectPool.SYNTHESIS_COLLECTION), // 150% item drop
              withEquipped(ItemPool.GRIMACITE_GO_GO_BOOTS), // 30% item drop
              withProperty("8BitColor", color));

      try (cleanups) {
        String output = execute("eight_bit_points($location[Hero's Field])");
        assertThat(output, endsWith("Returned: " + points + "\n"));
      }
    }

    @ParameterizedTest
    @CsvSource({"black, 400", "red, 200"})
    void vanyasCastle(String color, int points) {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.RACING), // 200% init
              withEffect("Memory of Speed"), // 200% init
              withEffect("Industrially Lubricated"), // 150% init
              withEquipped(ItemPool.ROCKET_BOOTS), // 100% init
              withProperty("8BitColor", color));

      try (cleanups) {
        String output = execute("eight_bit_points($location[Vanya's Castle])");
        assertThat(output, endsWith("Returned: " + points + "\n"));
      }
    }

    @ParameterizedTest
    @CsvSource({"blue, 300", "black, 150"})
    void megaloCity(String color, int points) {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.SUPER_STRUCTURE), // 500 DA
              withProperty("8BitColor", color));

      try (cleanups) {
        String output = execute("eight_bit_points($location[Megalo-City])");
        assertThat(output, endsWith("Returned: " + points + "\n"));
      }
    }
  }

  @Nested
  class ItemDrops {
    @Test
    void itemDrops() {
      var cleanups = withNextMonster("stench zombie");

      try (cleanups) {
        String output = execute("item_drops()");
        assertThat(
            output,
            is(
                """
                      Returned: aggregate float [item]
                      Freddy Kruegerand => 5.0
                      muddy skirt => 0.1
                      Dreadsylvanian Almanac page => 1.0
                      """));
      }
    }

    @Test
    void itemDropsMonster() {
      String output = execute("item_drops($monster[spooky zombie])");
      assertThat(
          output,
          is(
              """
                    Returned: aggregate float [item]
                    Freddy Kruegerand => 5.0
                    grandfather watch => 0.1
                    Dreadsylvanian Almanac page => 1.0
                    """));
    }

    @Test
    void itemDropsArray() {
      var cleanups = withNextMonster("stench zombie");

      try (cleanups) {
        String output = execute("item_drops_array()");
        assertThat(
            output,
            is(
                """
                      Returned: aggregate {item drop; float rate; string type;} [3]
                      0 => record {item drop; float rate; string type;}
                        drop => Dreadsylvanian Almanac page
                        rate => 1.0
                        type => f
                      1 => record {item drop; float rate; string type;}
                        drop => Freddy Kruegerand
                        rate => 5.0
                        type => f
                      2 => record {item drop; float rate; string type;}
                        drop => muddy skirt
                        rate => 0.1
                        type => c
                      """));
      }
    }

    @Test
    void itemDropsArrayMonster() {
      String output = execute("item_drops_array($monster[spooky zombie])");
      assertThat(
          output,
          is(
              """
                    Returned: aggregate {item drop; float rate; string type;} [3]
                    0 => record {item drop; float rate; string type;}
                      drop => Dreadsylvanian Almanac page
                      rate => 1.0
                      type => f
                    1 => record {item drop; float rate; string type;}
                      drop => Freddy Kruegerand
                      rate => 5.0
                      type => f
                    2 => record {item drop; float rate; string type;}
                      drop => grandfather watch
                      rate => 0.1
                      type => c
                    """));
    }
  }

  @Test
  void setLocation() {
    String output = execute("set_location($location[Barf Mountain])");
    assertThat(output, containsString("Returned: void"));
    output = execute("my_location()");
    assertThat(output, containsString("Returned: Barf Mountain"));
    output = execute("set_location($location[none])");
    assertThat(output, containsString("Returned: void"));
    output = execute("my_location()");
    assertThat(output, containsString("Returned: none"));
  }

  @Test
  void numericModifierHandlesCrimboTrainingSkills() {
    String output = execute("numeric_modifier($skill[Crimbo Training: Bartender], \"booze drop\")");
    assertThat(output, containsString("15.0"));
  }

  @Test
  void holiday() {
    var cleanups = withDay(2023, Month.FEBRUARY, 10);

    try (cleanups) {
      String output = execute("holiday()");

      assertContinueState();
      assertThat(output, is("Returned: St. Sneaky Pete's Day\n"));
    }
  }

  @Test
  void statBonusToday() {
    var cleanups = withDay(2023, Month.SEPTEMBER, 12);

    try (cleanups) {
      String output = execute("stat_bonus_today()");

      assertContinueState();
      assertThat(output, is("Returned: Muscle\n"));
    }
  }

  @Test
  void statBonusTomorrow() {
    var cleanups = withDay(2023, Month.SEPTEMBER, 19);

    try (cleanups) {
      String output = execute("stat_bonus_tomorrow()");

      assertContinueState();
      assertThat(output, is("Returned: Moxie\n"));
    }
  }

  @Nested
  class Ids {
    @ParameterizedTest
    @CsvSource({
      "$item[pirate radio ring].id, 10210",
      "$skill[Unleash Terra Cotta Army].id, 7321",
      "$effect[Wings].id, 6",
      "$familiar[Cat Burglar].id, 267",
      "$monster[Knob Goblin Embezzler].id, 530",
      "$location[The Dire Warren].id, 92",
      "$path[Trendy].id, 7",
      "$class[Pig Skinner].id, 28"
    })
    void exposesIds(String exec, String value) {
      String output = execute(exec);
      assertThat(output, containsString("Returned: " + value));
    }
  }

  @Nested
  class SplitJoinStrings {
    final String input1 = "line1\\nline2\\nline3";
    final String input2 = "foo bar baz";

    @Test
    void canSplitOnNewLine() {
      String input = "string str = \"" + input1 + "\"; split_string(str)";
      String output = execute(input);
      assertThat(
          output,
          is(
              """
              Returned: aggregate string [3]
              0 => line1
              1 => line2
              2 => line3
              """));
    }

    @Test
    void canSplitOnSpace() {
      String input = "string str = \"" + input2 + "\"; split_string(str, \" \")";
      String output = execute(input);
      assertThat(
          output,
          is(
              """
              Returned: aggregate string [3]
              0 => foo
              1 => bar
              2 => baz
              """));
    }

    @Test
    void canSplitJoinOnNewLine() {
      String input = "string str = \"" + input1 + "\"; split_string(str).join_strings()";
      String output = execute(input);
      assertThat(
          output,
          is(
              """
              Returned: line1
              line2
              line3
              """));
    }

    @Test
    void canSplitJoinOnSpace() {
      String input =
          "string str = \"" + input2 + "\"; split_string(str, \" \").join_strings(\" \")";
      String output = execute(input);
      assertThat(output, is("""
              Returned: foo bar baz
              """));
    }
  }

  @Nested
  class PledgeAllegiance {
    @Test
    void pledgeAllegiance() {
      String input = "$location[Noob Cave].pledge_allegiance";
      String output = execute(input);
      assertThat(
          output,
          is(
              """
          Returned: Item Drop: 30, Spooky Damage: 10, Spooky Spell Damage: 10, Muscle: 10
          """));
    }

    @Test
    void pledgeAllegianceComplex() {
      String input = "$location[Hobopolis Town Square].pledge_allegiance";
      String output = execute(input);
      assertThat(
          output,
          is(
              """
          Returned: Initiative: 50, Hot Damage: 10, Hot Spell Damage: 10, MP Regen Min: 10, MP Regen Max: 15, Moxie Percent: 10
          """));
    }

    @Test
    void pledgeAllegianceResistance() {
      String input = "$location[Outskirts of Camp Logging Camp].pledge_allegiance";
      String output = execute(input);
      assertThat(
          output,
          is(
              """
          Returned: Meat Drop: 25, Hot Resistance: 2, Cold Resistance: 2, Spooky Resistance: 2, Stench Resistance: 2, Sleaze Resistance: 2, Cold Damage: 10, Cold Spell Damage: 10
          """));
    }
  }

  @Nested
  class SausageGoblinProbability {
    @ParameterizedTest
    @CsvSource({
      "0,0,0,0.2",
      "1,0,0,0.4",
      "3,0,0,0.8",
      "4,0,0,1.0",
      "5,0,0,1.0",
      "5,1,5,0.125",
      "6,1,5,0.25",
      "0,8,0,0.017857",
      "1,8,0,0.035714",
    })
    void calculatesGoblinChance(int turnsPlayed, int goblinsFought, int lastGoblin, String chance) {
      var cleanups =
          new Cleanups(
              withTurnsPlayed(turnsPlayed),
              withProperty("_sausageFights", goblinsFought),
              withProperty("_lastSausageMonsterTurn", lastGoblin));

      try (cleanups) {
        String input = "sausage_goblin_chance()";
        String output = execute(input);
        assertThat(output, containsString(chance));
      }
    }
  }

  @Nested
  class Modifier {
    @Test
    void canGetRecord() {
      String input = "$modifier[meat drop]";
      String output = execute(input);
      assertThat(
          output,
          is(
              """
                 Returned: Meat Drop
                 name => Meat Drop
                 type => numeric
                 """));
    }

    @Test
    void canGetAllModifiers() {
      String input = "$modifiers[]";
      String output = execute(input);
      assertThat(output, containsString("Four Songs"));
      assertThat(output, containsString("Meat Drop"));
    }

    @Test
    void canCallNumericWithModifier() {
      String input = "numeric_modifier($item[ring of the Skeleton Lord], $modifier[Meat Drop])";
      String output = execute(input);
      assertThat(output, is("""
                 Returned: 50.0
                 """));
    }

    @Test
    void numericErrorsWithWrongModifierType() {
      String input = "numeric_modifier($item[ring of the Skeleton Lord], $modifier[Unarmed])";
      String output = execute(input);
      assertThat(output, startsWith("numeric modifier required"));
    }

    @Test
    void canCallBooleanWithModifier() {
      String input = "boolean_modifier($item[Brimstone Beret], $modifier[Four Songs])";
      String output = execute(input);
      assertThat(output, is("""
                 Returned: true
                 """));
    }

    @Test
    void booleanErrorsWithWrongModifierType() {
      String input = "boolean_modifier($item[Brimstone Beret], $modifier[Moxie])";
      String output = execute(input);
      assertThat(output, startsWith("boolean modifier required"));
    }

    @Test
    void canCallStringWithModifier() {
      String input = "string_modifier(\"Sign:Marmot\", $modifier[Modifiers])";
      String output = execute(input);
      assertThat(
          output,
          is(
              """
                 Returned: Experience Percent (Moxie): +10, Cold Resistance: +1, Hot Resistance: +1, Sleaze Resistance: +1, Spooky Resistance: +1, Stench Resistance: +1
                 """));
    }

    @Test
    void stringErrorsWithWrongModifierType() {
      String input = "string_modifier(\"Sign:Marmot\", $modifier[Cold Resistance])";
      String output = execute(input);
      assertThat(output, startsWith("string modifier required"));
    }

    @Test
    void canCallEffectWithModifier() {
      String input = "effect_modifier($item[blackberry polite], $modifier[Effect])";
      String output = execute(input);
      assertThat(output, startsWith("Returned: Blackberry Politeness"));
    }

    @Test
    void effectErrorsWithWrongModifierType() {
      String input = "effect_modifier($item[blackberry polite], $modifier[Meat Drop])";
      String output = execute(input);
      assertThat(output, startsWith("string modifier required"));
    }

    @Test
    void canCallClassWithModifier() {
      String input = "class_modifier($item[chintzy noodle ring], $modifier[Class])";
      String output = execute(input);
      assertThat(output, startsWith("Returned: Pastamancer"));
    }

    @Test
    void classErrorsWithWrongModifierType() {
      String input = "class_modifier($item[chintzy noodle ring], $modifier[Muscle])";
      String output = execute(input);
      assertThat(output, startsWith("string modifier required"));
    }

    @Test
    void canCallSkillWithModifier() {
      String input = "skill_modifier($item[alien source code printout], $modifier[Skill])";
      String output = execute(input);
      assertThat(output, startsWith("Returned: Alien Source Code"));
    }

    @Test
    void skillErrorsWithWrongModifierType() {
      String input = "skill_modifier($item[alien source code printout], $modifier[Maximum MP])";
      String output = execute(input);
      assertThat(output, startsWith("string modifier required"));
    }

    @Test
    void canCallStatWithModifier() {
      String input = "stat_modifier($effect[Stabilizing Oiliness], $modifier[Equalize])";
      String output = execute(input);
      assertThat(output, is("""
                 Returned: Muscle
                 """));
    }

    @Test
    void statErrorsWithWrongModifierType() {
      String input = "stat_modifier($effect[Stabilizing Oiliness], $modifier[Muscle])";
      String output = execute(input);
      assertThat(output, startsWith("string modifier required"));
    }

    @Test
    void canCallMonsterWithModifier() {
      String input = "monster_modifier($effect[A Lovely Day for a Beatnik], $modifier[Avatar])";
      String output = execute(input);
      assertThat(output, startsWith("Returned: Savage Beatnik"));
    }

    @Test
    void monsterErrorsWithWrongModifierType() {
      String input = "monster_modifier($effect[A Lovely Day for a Beatnik], $modifier[Muscle])";
      String output = execute(input);
      assertThat(output, startsWith("string modifier required"));
    }

    @Test
    void parsesModifierString() {
      String input =
          "split_modifiers(\"Meat Drop: 25, Hot Resistance: 2, Cold Resistance: 2, Unarmed, Cold Damage: 10, Cold Spell Damage: 10\")";
      String output = execute(input);
      assertThat(
          output,
          is(
              """
                 Returned: aggregate string [modifier]
                 Cold Damage => 10
                 Cold Resistance => 2
                 Cold Spell Damage => 10
                 Hot Resistance => 2
                 Meat Drop => 25
                 Unarmed =>
                 """));
    }

    @Test
    void parsesModifiersWithDifferentNamesToTags() {
      String input =
          "split_modifiers(\"Experience (Muscle): +11, Experience (Mysticality): +9, Experience (Moxie): +7, Damage Reduction: 24\")";
      String output = execute(input);
      assertThat(
          output,
          is(
              """
                 Returned: aggregate string [modifier]
                 Damage Reduction => 24
                 Moxie Experience => +7
                 Muscle Experience => +11
                 Mysticality Experience => +9
                 """));
    }
  }

  @Nested
  class Ping {
    @Test
    void parsesPropertyASH() {
      final var cleanups =
          new Cleanups(withProperty("pingLatest", "api.php:10:26:31:283:19620:28"));
      try (cleanups) {
        String input = "ping(\"pingLatest\")";
        String output = execute(input);
        assertThat(
            output,
            is(
                """
              Returned: record {string page; int count; int low; int high; int total; int bytes; int average; int bps;}
              page => api
              count => 10
              low => 26
              high => 31
              total => 283
              bytes => 19620
              average => 28
              bps => 69329
              """));
      }
    }
  }

  @Nested
  class BookOfFacts {
    @ParameterizedTest
    @CsvSource({
      "ACCORDION_THIEF, CRAZY_RANDOM_SUMMER, topiary golem, stats, +1 all substats",
      "TURTLE_TAMER, OXYGENARIAN, Blooper, meat, 10 Meat",
      "PASTAMANCER, COMMUNITY_SERVICE, bookbat, modifier, Experience (familiar): +1",
      "SEAL_CLUBBER, KINGDOM_OF_EXPLOATHING, Jefferson pilot, item, foon"
    })
    void exposesFactAndFactTypeInMonsterProxy(
        final AscensionClass ascensionClass,
        final Path path,
        final String monsterName,
        final String factType,
        final String fact) {
      final var cleanups = new Cleanups(withClass(ascensionClass), withPath(path));
      try (cleanups) {
        String actualFactType = execute("$monster[" + monsterName + "].fact_type");
        assertThat(actualFactType, equalTo("Returned: " + factType + "\n"));
        String actualFact = execute("$monster[" + monsterName + "].fact");
        assertThat(actualFact, equalTo("Returned: " + fact + "\n"));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "1, item, ' pocket wish'",
      "3, none, ''",
    })
    void factIsStatefulInMonsterProxy(final int wishes, final String factType, final String fact) {
      final var cleanups =
          new Cleanups(
              withClass(AscensionClass.DISCO_BANDIT),
              withPath(Path.THE_SOURCE),
              withProperty("_bookOfFactsWishes", wishes));
      try (cleanups) {
        String actualFactType = execute("$monster[triffid].fact_type");
        assertThat(actualFactType, equalTo("Returned: " + factType + "\n"));
        String actualFact = execute("$monster[triffid].fact");
        assertThat(actualFact, equalTo("Returned:" + fact + "\n"));
      }
    }

    @Test
    void factIsNotStatefulInFunction() {
      final var cleanups =
          new Cleanups(
              withClass(AscensionClass.DISCO_BANDIT),
              withPath(Path.THE_SOURCE),
              withProperty("_bookOfFactsWishes", 3));
      try (cleanups) {
        String actualFactType =
            execute("fact_type($class[Disco Bandit], $path[The Source], $monster[triffid])");
        assertThat(actualFactType, equalTo("Returned: item\n"));
        String actualFact =
            execute("item_fact($class[Disco Bandit], $path[The Source], $monster[triffid]).name");
        assertThat(actualFact, equalTo("Returned: pocket wish\n"));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "fact_type, briefcase bat, modifier",
      "string_fact, briefcase bat, Experience (familiar): +1",
      "item_fact, goblin conspirator, Knob mushroom",
      "effect_fact, trophyfish, Fishy",
      "numeric_fact, trophyfish, 10",
      "fact_type, none,",
      "string_fact, none,",
      "item_fact, none, none",
      "effect_fact, none, none",
      "numeric_fact, none, 0",
    })
    void functionsHaveVersionsThatUseCurrentClassPath(
        final String fn, final String monsterName, final String expected) {
      final var cleanups =
          new Cleanups(withClass(AscensionClass.SEAL_CLUBBER), withPath(Path.NONE));
      try (cleanups) {
        var code = fn + "($monster[" + monsterName + "])";
        String actual = execute(code);
        var startsWith = expected == null ? "\n" : " " + expected + "\n";
        assertThat(actual, startsWith("Returned:" + startsWith));
      }
    }
  }

  @Nested
  class Tracking {
    @Test
    void noCopiesIsZeroCount() {
      final var cleanups = withTrackedMonsters("");

      try (cleanups) {
        var code = "track_copy_count($monster[crate])";
        String actual = execute(code);
        assertThat(actual, equalTo("Returned: 0\n"));
      }
    }

    @Test
    void noCopiesIsNotIgnoreQueue() {
      final var cleanups = withTrackedMonsters("");

      try (cleanups) {
        var code = "track_ignore_queue($monster[crate])";
        String actual = execute(code);
        assertThat(actual, equalTo("Returned: false\n"));
      }
    }

    @Test
    void copyCountIncludesAllCopies() {
      final var cleanups =
          new Cleanups(
              withTrackedMonsters(
                  "crate:Transcendent Olfaction:1:crate:Gallapagosian Mating Call:2"),
              withFamiliar(FamiliarPool.RED_SNAPPER),
              withTrackedPhyla("construct:Red-Nosed Snapper:3"));

      try (cleanups) {
        var code = "track_copy_count($monster[crate])";
        String actual = execute(code);
        assertThat(actual, equalTo("Returned: 6\n"));
      }
    }

    @Test
    void copyCountIsIgnoreQueueIfAnyCopyIs() {
      final var cleanups =
          withTrackedMonsters("crate:Gallapagosian Mating Call:1:crate:Transcendent Olfaction:2");

      try (cleanups) {
        var code = "track_ignore_queue($monster[crate])";
        String actual = execute(code);
        assertThat(actual, equalTo("Returned: true\n"));
      }
    }

    @Test
    void trackedByIncludesAllTracks() {
      final var cleanups =
          new Cleanups(
              withTrackedMonsters(
                  "crate:Transcendent Olfaction:1:crate:Gallapagosian Mating Call:2"),
              withFamiliar(FamiliarPool.RED_SNAPPER),
              withTrackedPhyla("construct:Red-Nosed Snapper:3"));

      try (cleanups) {
        var code = "tracked_by($monster[crate])";
        String actual = execute(code);
        assertThat(
            actual,
            equalTo(
                """
            Returned: aggregate string [3]
            0 => Transcendent Olfaction
            1 => Gallapagosian Mating Call
            2 => Red-Nosed Snapper
            """));
      }
    }
  }

  /**
   * This test is intended to test running the maximizer from a command or script (not the GUI),
   * show that the maximizer chooses a weapon and then emits the commands to equip that weapon.
   */
  @Test
  public void itShouldMaximizeAndEquipSelectedWeapon() {
    String maxStr = "effective";
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(
            withStats(10, 5, 5),
            withEquippableItem("seal-skull helmet"),
            withEquippableItem("astral shirt"),
            withEquippableItem("old sweatpants"),
            withEquippableItem("sewer snake"),
            withEquippableItem("seal-clubbing club"));
    String out;
    String cmd = "maximize(\"" + maxStr + "\", false)";
    try (cleanups) {
      out = execute(cmd);
    }
    assertFalse(out.isEmpty());
    assertTrue(out.contains("Putting on seal-skull helmet..."));
    assertTrue(out.contains("Wielding seal-clubbing club..."));
    assertTrue(out.contains("Putting on old sweatpants..."));
    assertContinueState();
    var requests = getRequests();
    assertFalse(requests.isEmpty());
    var checkMe =
        requests.stream().filter(x -> getPostRequestBody(x).contains("whichitem=1")).findFirst();
    if (checkMe.isPresent()) {
      assertPostRequest(checkMe.get(), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=1");
    } else {
      fail("Could not find expected equipment request.");
    }
  }

  @Test
  public void itShouldEquipCleaver() {
    String maxStr =
        "5item,meat,0.5initiative,0.1da 1000max,dr,0.5all res,1.5mainstat,-fumble,mox,0.4hp,0.2mp 1000max,3mp regen,0.25spell damage,1.75spell damage percent,2familiar weight,5familiar exp,10exp,5Mysticality experience percent,+200bonus spring shoes,+200bonus June cleaver,+200bonus designer sweatpants,2 dump";
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(
            withClass(AscensionClass.PASTAMANCER),
            withHardcore(),
            withPath(Path.AVANT_GUARD),
            withSign(ZodiacSign.OPOSSUM),
            withInteractivity(false),
            withNoEffects(),
            withProperty("umbrellaState", "broken"),
            withEquipped(Slot.HAT, "Apriling band helmet"),
            withUnequipped(Slot.WEAPON),
            withUnequipped(Slot.OFFHAND),
            withEquipped(Slot.SHIRT, "Jurassic Parka"),
            withProperty("parkaMode", "kachungasaur"),
            withEquipped(Slot.PANTS, "designer sweapants"),
            withEquipped(Slot.CONTAINER, "bat wings"),
            withEquipped(Slot.ACCESSORY1, "Cincho de Mayo"),
            withEquipped(Slot.ACCESSORY2, "combat lover's locket"),
            withEquipped(Slot.ACCESSORY3, "spring shoes"),
            withEquippableItem("seal-skull helmet"), // 2283
            withEquippableItem("ravioli hat"),
            withEquippableItem("Hollandaise helmet"),
            withEquippableItem("disco mask"),
            withEquippableItem("mariachi hat"),
            withEquippableItem("helmet turtle"),
            withEquippableItem("coconut shell"),
            withEquippableItem("Apriling band helmet"),
            withEquippableItem("toy accordion"),
            withEquippableItem("hobo code binder"),
            withEquippableItem("stuffed spooky gravy fairy "),
            withEquippableItem("stuffed astral badger"),
            withEquippableItem("magical ice cubes"),
            withEquippableItem("Roman Candelabra"),
            withEquippableItem("unbreakable umbrella (broken)"),
            withEquippableItem("august scepter"),
            withEquippableItem("bat wings"),
            withEquippableItem("Jurassic Parka (kachungasaur mode)"),
            withEquippableItem("old sweatpants"),
            withEquippableItem("tearaway pants"),
            withEquippableItem("designer sweatpants"),
            withEquippableItem("Everfull Dart Holster"),
            withEquippableItem("cursed monkey's paw"),
            withEquippableItem("astral mask"),
            withEquippableItem("Cincho de Mayo"),
            withEquippableItem("combat lover's locket"),
            withEquippableItem("spring shoes"),
            withEquippableItem("turtle totem"),
            withEquippableItem("pasta spoon"),
            withEquippableItem("saucepan"),
            withEquippableItem("disco ball"),
            withEquippableItem("little paper umbrella"),
            withEquippableItem("candy cane sword cane"),
            withEquippableItem("June cleaver"), // 10920
            withEquippableItem("seal-clubbing club")); // 1
    String out;
    String cmd = "maximize(\"" + maxStr + "\", false)";
    try (cleanups) {
      out = execute(cmd);
    }
    assertFalse(out.isEmpty());
    assertTrue(out.contains("Wielding June cleaver"));
    assertTrue(out.contains("Putting on designer sweatpants..."));
    assertContinueState();
    var requests = getRequests();
    assertFalse(requests.isEmpty());
    boolean passed = false;
    for (var req : requests) {
      if (req.method().contains("POST")) {
        passed = passed || getPostRequestBody(req).contains("whichitem=10920");
      }
    }
    assertTrue(passed, "Did not find expected equip request.");
  }

  @Nested
  class Darts {
    @Test
    void canCalculateSkillsToParts() {
      final var cleanups =
          withProperty("_currentDartboard", "7513:torso,7514:head,7515:butt,7516:arm,7517:leg");

      try (cleanups) {
        String actual = execute("dart_skills_to_parts()");
        String expected =
            """
            Returned: aggregate string [skill]
            Darts: Throw at %part1 => torso
            Darts: Throw at %part2 => head
            Darts: Throw at %part3 => butt
            Darts: Throw at %part4 => arm
            Darts: Throw at %part5 => leg
            """;
        assertThat(actual, equalTo(expected));
      }
    }

    @Test
    void canCalculatePartsToSkills() {
      final var cleanups =
          withProperty("_currentDartboard", "7513:torso,7514:head,7515:butt,7516:arm,7517:leg");

      try (cleanups) {
        String actual = execute("dart_parts_to_skills()");
        String expected =
            """
            Returned: aggregate skill [string]
            arm => Darts: Throw at %part4
            butt => Darts: Throw at %part3
            head => Darts: Throw at %part2
            leg => Darts: Throw at %part5
            torso => Darts: Throw at %part1
            """;
        assertThat(actual, equalTo(expected));
      }
    }
  }

  @Nested
  class Curse {
    @Test
    void canThrowBrick() {
      setupFakeClient();
      var cleanup = withItem(ItemPool.BRICK);

      try (cleanup) {
        var output = execute("curse($item[brick], \"StuBorn\")");
        assertThat(output, endsWith("Returned: true\n"));

        var requests = getRequests();
        assertThat(requests.size(), is(1));
        assertPostRequest(
            requests.get(0), "/curse.php", "action=use&whichitem=1649&targetplayer=StuBorn");
      }
    }

    @Test
    void canThrowMultipleBricks() {
      setupFakeClient();
      var cleanup = withItem(ItemPool.BRICK, 3);

      try (cleanup) {
        var output = execute("curse(3, $item[brick], \"StuBorn\", \"\")");
        assertThat(output, endsWith("Returned: true\n"));

        var requests = getRequests();
        assertThat(requests.size(), is(3));
        requests.forEach(
            x ->
                assertPostRequest(
                    x, "/curse.php", "action=use&whichitem=1649&targetplayer=StuBorn"));
      }
    }

    @Test
    void canSendCandyHeartMessage() {
      setupFakeClient();
      var cleanup = withItem(ItemPool.GREEN_CANDY);

      try (cleanup) {
        var output = execute("curse($item[green candy heart], \"StuBorn\", \"You|rock!\")");
        assertThat(output, endsWith("Returned: true\n"));

        var requests = getRequests();
        assertThat(requests.size(), is(1));
        assertPostRequest(
            requests.get(0),
            "/curse.php",
            "action=use&whichitem=2309&targetplayer=StuBorn&texta=You&textb=rock!");
      }
    }

    @Test
    void cannotThrowMissingItem() {
      var output = execute("curse($item[brick], \"StuBorn\")");
      assertThat(output, startsWith("You need 1 more brick to continue"));
    }

    @Test
    void cannotThrowNonCurseItem() {
      var cleanup = withItem(ItemPool.DISCO_BALL);

      try (cleanup) {
        var output = execute("curse($item[disco ball], \"StuBorn\")");
        assertThat(output, startsWith("The disco ball cannot be used for cursing"));
      }
    }
  }

  @Nested
  class ToInt {
    @ParameterizedTest
    @ValueSource(
        strings = {
          "item",
          "familiar",
          "location",
          "skill",
          "effect",
          "class",
          "monster",
          "thrall",
          "servant",
          "vykea",
          "path"
        })
    void worksOnNoneValues(String type) {
      assertThat(
          execute("$" + type + "[none].to_int()"),
          both(startsWith("Returned: ")).and(not(containsString("Script execution aborted"))));
    }
  }

  @Nested
  class TurnsUntilForcedNoncombat {
    @Test
    void works() {
      AdventureSpentDatabase.resetTurns();
      var cleanups =
          new Cleanups(
              withAdventuresSpent(AdventurePool.HAUNTED_BILLIARDS_ROOM, 5),
              withProperty("lastNoncombat" + AdventurePool.HAUNTED_BILLIARDS_ROOM, 3));
      try (cleanups) {
        assertThat(
            execute("$location[The Haunted Billiards Room].turns_until_forced_noncombat()").trim(),
            is("Returned: 8"));
      }
    }

    @Test
    void worksOnNotApplicableValues() {
      assertThat(
          execute("$location[The Haunted Kitchen].turns_until_forced_noncombat()").trim(),
          is("Returned: -1"));
    }

    @Test
    void worksOnNoneValues() {
      assertThat(
          execute("$location[none].turns_until_forced_noncombat()").trim(), is("Returned: -1"));
    }
  }

  @Nested
  class ItemsHash {
    @ParameterizedTest
    @CsvSource({
      "inventory,1,3010618080379317301",
      "inventory,2,5098877678963069302",
      "inventory,1|2,-5775617302849066554",
      "inventory,1|2|3,8531806457801124500",
      "closet,1,3010618080379317301",
      "closet,2,5098877678963069302",
      "storage,1,3010618080379317301",
      "storage,2,5098877678963069302",
      "display,1,3010618080379317301",
      "display,2,5098877678963069302",
      "shop,1,3366927173997222913",
      "shop,2,-7673585838980678286"
    })
    void producesHashForValidItemsSource(String itemsSource, String itemIdsString, long expected) {
      List<Integer> itemIds =
          Arrays.stream(itemIdsString.split("\\|")).map(Integer::parseInt).toList();
      var cleanups =
          new Cleanups(
              itemIds.stream()
                  .map(
                      itemId ->
                          switch (itemsSource) {
                            case "inventory" -> withItem(itemId);
                            case "closet" -> withItemInCloset(itemId);
                            case "storage" -> withItemInStorage(itemId);
                            case "display" -> withItemInDisplay(itemId);
                            case "shop" -> withItemInShop(itemId, 100, 0);
                            default -> new Cleanups();
                          })
                  .toArray(Cleanups[]::new));
      try (cleanups) {
        assertThat(
            execute("get_items_hash(\"%s\")".formatted(itemsSource)).trim(),
            is("Returned: " + expected));
        assertContinueState();
      }
    }

    @Test
    void errorsOnInvalidItemsSource() {
      assertThat(
          execute("get_items_hash(\"xxxxxx\")").trim(),
          is(
              "get_items_hash: Invalid items source. Valid are inventory, closet, storage, display, shop.\nReturned: -3750763034362895579"));
    }
  }

  @Test
  void getAvatar() {
    KoLCharacter.setAvatar("otherimages/giant_foodie.gif");
    assertThat(
        execute("get_avatar()").trim(),
        is("Returned: aggregate string [1]\n0 => otherimages/giant_foodie.gif"));
  }

  @Test
  void getTitle() {
    KoLCharacter.setTitle("NO PEEKING");
    assertThat(execute("get_title()").trim(), is("Returned: NO PEEKING"));
  }

  @Nested
  class XPath {
    @AfterAll
    static void reset() {
      GenericRequest.setPasswordHash("");
    }

    private String xpath(String html, String xpath) {
      return execute("xpath(`" + html + "`, `" + xpath + "`)");
    }

    @Test
    void blankXpathReturnsFullHtml() {
      var fullHtml = "<html><head><title>Hello</title></head><body>World</body></html>";
      assertThat(
          xpath(fullHtml, ""),
          is(
              """
          Returned: aggregate string [1]
          0 => &lt;html&gt;
          &lt;head&gt;&lt;title&gt;Hello&lt;/title&gt;&lt;/head&gt;
          &lt;body&gt;World&lt;/body&gt;&lt;/html&gt;
          """));
    }

    @Test
    void invalidXPathIsError() {
      assertThat(xpath("<p>", "//p["), startsWith("invalid xpath expression"));
    }

    @Test
    void xpathFullPage() {
      var cleanups = withNextResponse(200, html("request/test_account_tab_combat.html"));

      try (cleanups) {
        assertThat(
            execute(
                "string page = visit_url(\"account.php?tab=combat\"); xpath(page, `//*[@id=\"opt_flag_aabosses\"]/label/input[@type='checkbox']@checked`)"),
            is(
                """
                Returned: aggregate string [1]
                0 => checked
                """));
      }
    }

    @Test
    void xpathJoinsNodes() {
      var cleanups = withNextResponse(200, html("request/test_clan_signup.html"));

      try (cleanups) {
        assertThat(
            execute(
                "string page = visit_url(\"clan_signup.php\"); xpath(page, `//select[@name=\"whichclan\"]//option`)"),
            is(
                """
                    Returned: aggregate string [21]
                    0 => &lt;option value=&quot;2046996836&quot;&gt;&Icirc;&copy;&Iuml;&lt;/option&gt;
                    1 => &lt;option value=&quot;84165&quot;&gt;Alliance From Heck&lt;/option&gt;
                    2 => &lt;option value=&quot;2046994401&quot;&gt;Black Mesa&lt;/option&gt;
                    3 => &lt;option value=&quot;90485&quot;&gt;Bonus Adventures from Hell&lt;/option&gt;
                    4 => &lt;option value=&quot;2047008364&quot;&gt;Collaborative Dungeon Central&lt;/option&gt;
                    5 => &lt;option value=&quot;2047008362&quot;&gt;Collaborative Dungeon Running 1&lt;/option&gt;
                    6 => &lt;option value=&quot;2047008363&quot;&gt;Collaborative Dungeon Running 2&lt;/option&gt;
                    7 => &lt;option value=&quot;2046987880&quot;&gt;Easter Pink&lt;/option&gt;
                    8 => &lt;option value=&quot;18112&quot;&gt;Evil for Fun and Profit&lt;/option&gt;
                    9 => &lt;option value=&quot;2047004665&quot;&gt;factnet_0136&lt;/option&gt;
                    10 => &lt;option value=&quot;52111&quot;&gt;First Lives Club&lt;/option&gt;
                    11 => &lt;option value=&quot;2046994693&quot;&gt;From the Ashes&lt;/option&gt;
                    12 => &lt;option value=&quot;2047005067&quot;&gt;Funking Good&lt;/option&gt;
                    13 => &lt;option value=&quot;2047009544&quot;&gt;LUPO&lt;/option&gt;
                    14 => &lt;option value=&quot;2046987019&quot;&gt;Not Dead Yet&lt;/option&gt;
                    15 => &lt;option value=&quot;83922&quot;&gt;Prinny Land&lt;/option&gt;
                    16 => &lt;option value=&quot;2047000135&quot;&gt;Reddit United&lt;/option&gt;
                    17 => &lt;option value=&quot;2046997154&quot;&gt;The Fax Dump&lt;/option&gt;
                    18 => &lt;option value=&quot;2046986725&quot;&gt;The Meowing Squirrel&lt;/option&gt;
                    19 => &lt;option value=&quot;20655&quot;&gt;The Ugly Ducklings&lt;/option&gt;
                    20 => &lt;option value=&quot;2046997063&quot;&gt;Ultimate Durs&lt;/option&gt;
                    """));
      }
    }

    @Test
    void xpathWorksWithFragments() {
      var fragment = "<select><option value=\"90485\">Bonus Adventures from Hell</option></select>";
      assertThat(
          xpath(fragment, "//@value"),
          is("""
          Returned: aggregate string [1]
          0 => 90485
          """));
      assertThat(
          xpath(fragment, "//text()"),
          is(
              """
          Returned: aggregate string [1]
          0 => Bonus Adventures from Hell
          """));
    }
  }

  @Test
  void computesFreeCrafts() {
    var cleanups =
        new Cleanups(
            withEffect(EffectPool.INIGOS, 6),
            withEffect(EffectPool.COOKING_CONCENTRATE, 7),
            withItem(ItemPool.THORS_PLIERS),
            withSkill(SkillPool.RAPID_PROTOTYPING),
            withSkill(SkillPool.HOLIDAY_MULTITASKING),
            withSkill(SkillPool.OLD_SCHOOL_COCKTAILCRAFTING),
            withProperty("_thorsPliersCrafting", 1),
            withProperty("_rapidPrototypingUsed", 5),
            withProperty("_oldSchoolCocktailCraftingUsed", 1),
            withProperty("_holidayMultitaskingUsed"));

    try (cleanups) {
      assertThat(execute("free_crafts()").trim(), is("Returned: 4"));
      assertThat(execute("free_cooks()").trim(), is("Returned: 1"));
      assertThat(execute("free_mixes()").trim(), is("Returned: 2"));
      assertThat(execute("free_smiths()").trim(), is("Returned: 9"));
    }
  }

  @Test
  void determinesBanishes() {
    var cleanups =
        new Cleanups(withCurrentRun(128), withBanishedPhyla("undead:Patriotic Screech:119"));

    try (cleanups) {
      assertThat(execute("is_banished($monster[ghuol])").trim(), is("Returned: true"));
      assertThat(execute("is_banished($phylum[undead])").trim(), is("Returned: true"));
      assertThat(execute("is_banished($monster[zombie process])").trim(), is("Returned: false"));
      assertThat(execute("is_banished($phylum[construct])").trim(), is("Returned: false"));
      assertThat(execute("is_banished($monster[none])").trim(), is("Returned: false"));
      assertThat(execute("is_banished($phylum[none])").trim(), is("Returned: false"));
    }
  }

  @Nested
  class SkillCoinmasters {
    @Test
    void sellsSkillWorks() {
      assertThat(
          execute("sells_skill($coinmaster[Genetic Fiddling], $skill[Boiling Tear Ducts])").trim(),
          is("Returned: true"));
      assertThat(
          execute("sells_skill($coinmaster[Genetic Fiddling], $skill[Magic Sweat])").trim(),
          is("Returned: true"));
      assertThat(
          execute("sells_skill($coinmaster[Genetic Fiddling], $skill[Extra Brain])").trim(),
          is("Returned: true"));
      assertThat(
          execute("sells_skill($coinmaster[Genetic Fiddling], $skill[Sucker Fingers])").trim(),
          is("Returned: true"));
      assertThat(
          execute("sells_skill($coinmaster[Genetic Fiddling], $skill[Stream of sauce])").trim(),
          is("Returned: false"));
      assertThat(
          execute("sells_skill($coinmaster[Kiwi Kwiki Mart], $skill[Boiling Tear Ducts])").trim(),
          is("Returned: false"));
    }

    @Test
    void sellPriceWorks() {
      assertThat(
          execute("sell_price($coinmaster[Genetic Fiddling], $skill[Boiling Tear Ducts])").trim(),
          is("Returned: 30"));
      assertThat(
          execute("sell_price($coinmaster[Genetic Fiddling], $skill[Magic Sweat])").trim(),
          is("Returned: 60"));
      assertThat(
          execute("sell_price($coinmaster[Genetic Fiddling], $skill[Extra Brain])").trim(),
          is("Returned: 90"));
      assertThat(
          execute("sell_price($coinmaster[Genetic Fiddling], $skill[Sucker Fingers])").trim(),
          is("Returned: 120"));
      assertThat(
          execute("sell_price($coinmaster[Genetic Fiddling], $skill[Stream of sauce])").trim(),
          is("Returned: 0"));
      assertThat(
          execute("sell_price($coinmaster[Kiwi Kwiki Mart], $skill[Boiling Tear Ducts])").trim(),
          is("Returned: 0"));
    }
  }
}
