package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class WikiUtilitiesTest {
  @Test
  public void getWikiLocationForMonster() {
    var vampire = MonsterDatabase.findMonster("spooky vampire");
    String link = WikiUtilities.getWikiLocation(vampire, true);
    assertEquals(
        "https://kol.coldfront.net/thekolwiki/index.php/Data:Spooky_vampire_%28Spooky_Forest%29",
        link);
  }

  @Test
  public void getWikiLocationWithQuotes() {
    var drippyPlum = AdventureResult.tallyItem("\"caramel\" orange");
    String link = WikiUtilities.getWikiLocation(drippyPlum, true);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Data:%22caramel%22_orange", link);
  }

  @Test
  public void getWikiLocationWithQuestionMark() {
    var drippyPlum = AdventureResult.tallyItem("drippy plum(?)");
    String link = WikiUtilities.getWikiLocation(drippyPlum);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Drippy_plum%28%3F%29", link);
  }

  @Test
  public void getWikiLocationForItemThatIsAlsoMonster() {
    var flange = AdventureResult.tallyItem("flange");
    String link = WikiUtilities.getWikiLocation(flange);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Flange_%28item%29", link);
  }

  @Test
  public void getWikiLocationForMonsterThatIsAlsoItem() {
    var flange = MonsterDatabase.findMonster("Flange");
    String link = WikiUtilities.getWikiLocation(flange, true);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Data:Flange_%28monster%29", link);
  }

  @Test
  public void icePorterItemHasANonStandardDisambiguation() {
    var icePorter = AdventureResult.tallyItem("ice porter");
    String link = WikiUtilities.getWikiLocation(icePorter);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Ice_porter_%28drink%29", link);
  }

  @Test
  public void getWikiLocationUsesModifiers() {
    var hankyu = AdventureResult.tallyItem("frigid hanky&#363;");
    String link = WikiUtilities.getWikiLocation(hankyu);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Frigid_hankyu", link);
  }

  @Test
  public void errorCaseHandled() {
    assertNull(WikiUtilities.getWikiLocation(null));
  }

  @Test
  public void lookupByStringWorks() {
    var link = WikiUtilities.getWikiLocation("Your #1 Problem");
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Your_1_Problem", link);
  }

  @Test
  public void getWikiLocationForBossBatQuestionMark() {
    var edBossBat = MonsterDatabase.findMonster("Boss Bat?");
    String link = WikiUtilities.getWikiLocation(edBossBat, false);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Boss_Bat%3F", link);
  }

  @Test
  public void getWikiLocationForWaluigi() {
    var waluigi = MonsterDatabase.findMonster("Wa%playername/lowercase%");
    String link = WikiUtilities.getWikiLocation(waluigi, false);
    assertEquals(
        "https://kol.coldfront.net/thekolwiki/index.php/Wa%25playername/lowercase%25", link);
  }

  private static Stream<Arguments> wikiPages() {
    return Stream.of(
        Arguments.of("seal-clubbing club", WikiUtilities.ANY_TYPE, "Seal-clubbing_club"),
        Arguments.of("sweet tooth", WikiUtilities.ITEM_TYPE, "Sweet_tooth"),
        Arguments.of("water wings", WikiUtilities.ITEM_TYPE, "Water_wings"),
        Arguments.of("knuckle sandwich", WikiUtilities.ITEM_TYPE, "Knuckle_sandwich"),
        Arguments.of(
            "industrial strength starch", WikiUtilities.ITEM_TYPE, "Industrial_strength_starch"),
        Arguments.of("black pudding", WikiUtilities.ITEM_TYPE, "Black_pudding_%28food%29"),
        Arguments.of("zmobie", WikiUtilities.ITEM_TYPE, "Zmobie_%28drink%29"),
        Arguments.of("ice porter", WikiUtilities.ITEM_TYPE, "Ice_porter_%28drink%29"),
        Arguments.of("Bulky Buddy Box", WikiUtilities.ITEM_TYPE, "Bulky_Buddy_Box_%28hatchling%29"),
        Arguments.of(
            "The Sword in the Steak", WikiUtilities.ITEM_TYPE, "The_Sword_in_the_Steak_%28item%29"),
        Arguments.of("BRICKO bat", WikiUtilities.ITEM_TYPE, "BRICKO_bat_%28item%29"),
        Arguments.of(
            "Chorale of Companionship",
            WikiUtilities.ITEM_TYPE,
            "Chorale_of_Companionship_%28item%29"),
        Arguments.of("sonar-in-a-biscuit", WikiUtilities.ITEM_TYPE, "Sonar-in-a-biscuit"),
        Arguments.of(
            "Chorale of Companionship",
            WikiUtilities.SKILL_TYPE,
            "Chorale_of_Companionship_%28skill%29"),
        Arguments.of("Knuckle Sandwich", WikiUtilities.SKILL_TYPE, "Knuckle_Sandwich_%28skill%29"),
        Arguments.of(
            "Chorale of Companionship",
            WikiUtilities.EFFECT_TYPE,
            "Chorale_of_Companionship_%28effect%29"),
        Arguments.of("Sweet Tooth", WikiUtilities.EFFECT_TYPE, "Sweet_Tooth"),
        Arguments.of("Water Wings", WikiUtilities.EFFECT_TYPE, "Water_Wings"),
        Arguments.of(
            "Industrial Strength Starch", WikiUtilities.EFFECT_TYPE, "Industrial_Strength_Starch"),
        Arguments.of("ice porter", WikiUtilities.MONSTER_TYPE, "Ice_porter"),
        Arguments.of(
            "undead elbow macaroni",
            WikiUtilities.MONSTER_TYPE,
            "Undead_elbow_macaroni_%28monster%29"),
        Arguments.of("Souped Up", WikiUtilities.EFFECT_TYPE, "Souped_Up_%28effect%29"));
  }

  @ParameterizedTest
  @MethodSource("wikiPages")
  public void correctAnswerForWikiPagesOfVariousTypes(String name, int type, String expectedPage) {
    var link = WikiUtilities.getWikiLocation(name, type, false);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/" + expectedPage, link);
  }
}
