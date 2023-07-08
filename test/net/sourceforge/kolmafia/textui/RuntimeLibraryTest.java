package net.sourceforge.kolmafia.textui;

import static internal.helpers.Networking.html;
import static internal.helpers.Networking.json;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withFamiliarInTerrariumWithItem;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNextMonster;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withTurnsPlayed;
import static internal.helpers.Player.withValueOfAdventure;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.network.FakeHttpClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.session.GreyYouManager;
import net.sourceforge.kolmafia.session.MallPriceManager;
import net.sourceforge.kolmafia.textui.command.AbstractCommandTestBase;
import net.sourceforge.kolmafia.utilities.NullStream;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class RuntimeLibraryTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);
    Preferences.reset("testUser");
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
                      Freddy Kruegerand => 0.0
                      muddy skirt => 0.1
                      Dreadsylvanian Almanac page => 0.0
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
                    Freddy Kruegerand => 0.0
                    grandfather watch => 0.1
                    Dreadsylvanian Almanac page => 0.0
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
                        rate => 0.0
                        type => f
                      1 => record {item drop; float rate; string type;}
                        drop => Freddy Kruegerand
                        rate => 0.0
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
                      rate => 0.0
                      type => f
                    1 => record {item drop; float rate; string type;}
                      drop => Freddy Kruegerand
                      rate => 0.0
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
    String input1 = "line1\\nline2\\nline3";
    String input2 = "foo bar baz";

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
  }
}
