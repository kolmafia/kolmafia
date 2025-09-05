package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.utilities.WikiUtilities.WikiType;
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
        "https://wiki.kingdomofloathing.com/Data:Spooky_vampire_%28Spooky_Forest%29", link);
  }

  @Test
  public void getWikiLocationWithQuotes() {
    var drippyPlum = AdventureResult.tallyItem("\"caramel\" orange");
    String link = WikiUtilities.getWikiLocation(drippyPlum, true);
    assertEquals("https://wiki.kingdomofloathing.com/Data:%22caramel%22_orange", link);
  }

  @Test
  public void getWikiLocationWithQuestionMark() {
    var drippyPlum = AdventureResult.tallyItem("drippy plum(?)");
    String link = WikiUtilities.getWikiLocation(drippyPlum);
    assertEquals("https://wiki.kingdomofloathing.com/Drippy_plum%28%3F%29", link);
  }

  @Test
  public void getWikiLocationForItemThatIsAlsoMonster() {
    var flange = AdventureResult.tallyItem("flange");
    String link = WikiUtilities.getWikiLocation(flange);
    assertEquals("https://wiki.kingdomofloathing.com/Flange_%28item%29", link);
  }

  @Test
  public void getWikiLocationForMonsterThatIsAlsoItem() {
    var flange = MonsterDatabase.findMonster("Flange");
    String link = WikiUtilities.getWikiLocation(flange, true);
    assertEquals("https://wiki.kingdomofloathing.com/Data:Flange_%28monster%29", link);
  }

  @Test
  public void icePorterItemHasANonStandardDisambiguation() {
    var icePorter = AdventureResult.tallyItem("ice porter");
    String link = WikiUtilities.getWikiLocation(icePorter);
    assertEquals("https://wiki.kingdomofloathing.com/Ice_porter_%28drink%29", link);
  }

  @Test
  public void getWikiLocationUsesModifiers() {
    var hankyu = AdventureResult.tallyItem("frigid hanky&#363;");
    String link = WikiUtilities.getWikiLocation(hankyu);
    assertEquals("https://wiki.kingdomofloathing.com/Frigid_hankyu", link);
  }

  @Test
  public void errorCaseHandled() {
    assertNull(WikiUtilities.getWikiLocation(null));
  }

  @Test
  public void lookupByStringWorks() {
    var link = WikiUtilities.getWikiLocation("Your #1 Problem");
    assertEquals("https://wiki.kingdomofloathing.com/Your_1_Problem", link);
  }

  @Test
  public void getWikiLocationForBossBatQuestionMark() {
    var edBossBat = MonsterDatabase.findMonster("Boss Bat?");
    String link = WikiUtilities.getWikiLocation(edBossBat, false);
    assertEquals("https://wiki.kingdomofloathing.com/Boss_Bat%3F", link);
  }

  @Test
  public void getWikiLocationForWaluigi() {
    var waluigi = MonsterDatabase.findMonster("Wa%playername/lowercase%");
    String link = WikiUtilities.getWikiLocation(waluigi, false);
    assertEquals("https://wiki.kingdomofloathing.com/Wa%25playername/lowercase%25", link);
  }

  private static Stream<Arguments> wikiPages() {
    return Stream.of(
        Arguments.of("seal-clubbing club", WikiType.ANY, "Seal-clubbing_club"),
        Arguments.of("sweet tooth", WikiType.ITEM, "Sweet_tooth"),
        Arguments.of("water wings", WikiType.ITEM, "Water_wings"),
        Arguments.of("knuckle sandwich", WikiType.ITEM, "Knuckle_sandwich"),
        Arguments.of("industrial strength starch", WikiType.ITEM, "Industrial_strength_starch"),
        Arguments.of("black pudding", WikiType.ITEM, "Black_pudding_%28food%29"),
        Arguments.of("zmobie", WikiType.ITEM, "Zmobie_%28drink%29"),
        Arguments.of("ice porter", WikiType.ITEM, "Ice_porter_%28drink%29"),
        Arguments.of("Bulky Buddy Box", WikiType.ITEM, "Bulky_Buddy_Box_%28hatchling%29"),
        Arguments.of("The Sword in the Steak", WikiType.ITEM, "The_Sword_in_the_Steak_%28item%29"),
        Arguments.of("BRICKO bat", WikiType.ITEM, "BRICKO_bat_%28item%29"),
        Arguments.of(
            "Chorale of Companionship", WikiType.ITEM, "Chorale_of_Companionship_%28item%29"),
        Arguments.of("sonar-in-a-biscuit", WikiType.ITEM, "Sonar-in-a-biscuit"),
        Arguments.of(
            "Chorale of Companionship", WikiType.SKILL, "Chorale_of_Companionship_%28skill%29"),
        Arguments.of("Knuckle Sandwich", WikiType.SKILL, "Knuckle_Sandwich_%28skill%29"),
        Arguments.of(
            "Chorale of Companionship", WikiType.EFFECT, "Chorale_of_Companionship_%28effect%29"),
        Arguments.of("Sweet Tooth", WikiType.EFFECT, "Sweet_Tooth"),
        Arguments.of("Water Wings", WikiType.EFFECT, "Water_Wings"),
        Arguments.of("Industrial Strength Starch", WikiType.EFFECT, "Industrial_Strength_Starch"),
        Arguments.of("ice porter", WikiType.MONSTER, "Ice_porter"),
        Arguments.of(
            "undead elbow macaroni", WikiType.MONSTER, "Undead_elbow_macaroni_%28monster%29"),
        Arguments.of("Souped Up", WikiType.EFFECT, "Souped_Up_%28effect%29"));
  }

  @ParameterizedTest
  @MethodSource("wikiPages")
  public void correctAnswerForWikiPagesOfVariousTypes(
      String name, WikiType type, String expectedPage) {
    var link = WikiUtilities.getWikiLocation(name, type, false);
    assertEquals("https://wiki.kingdomofloathing.com/" + expectedPage, link);
  }
}
