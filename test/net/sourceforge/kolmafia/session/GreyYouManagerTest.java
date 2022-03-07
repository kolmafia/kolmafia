package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GreyYouManagerTest {

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("grey you manager user");
    KoLCharacter.setPath(Path.GREY_YOU);
  }

  @BeforeEach
  private void beforeEach() {
    GreyYouManager.resetAbsorptions();
  }

  static String loadHTMLResponse(String path) throws IOException {
    // Load the responseText from saved HTML file
    return Files.readString(Paths.get(path)).trim();
  }

  static void printSizes() {
    System.out.println(
        "absorbed monsters: "
            + GreyYouManager.absorbedMonsters.size()
            + " unknown absorptions = "
            + GreyYouManager.unknownAbsorptions.size()
            + " learned skills = "
            + GreyYouManager.learnedSkills.size());
  }

  @Test
  public void canFindAbsorptionsOnCharSheet() throws IOException {
    String responseText = loadHTMLResponse("request/test_grey_you_charsheet.html");
    GreyYouManager.parseMonsterAbsorptions(responseText);
    assertEquals(92, GreyYouManager.absorbedMonsters.size());
    assertEquals(0, GreyYouManager.unknownAbsorptions.size());
    assertEquals(0, GreyYouManager.learnedSkills.size());
    GreyYouManager.parseSkillAbsorptions(responseText);
    // 92 + 49 = 141, but Pseudopod Slap is not learned by absorption
    assertEquals(140, GreyYouManager.absorbedMonsters.size());
    assertEquals(0, GreyYouManager.unknownAbsorptions.size());
    assertEquals(49, GreyYouManager.learnedSkills.size());
  }

  @Test
  public void canFindUnknownAbsorptionsOnCharSheet() throws IOException {
    String responseText = loadHTMLResponse("request/test_grey_you_unknown_absorptions.html");
    GreyYouManager.parseMonsterAbsorptions(responseText);
    assertEquals(0, GreyYouManager.absorbedMonsters.size());
    assertEquals(2, GreyYouManager.unknownAbsorptions.size());
    assertTrue(GreyYouManager.unknownAbsorptions.containsKey(1800));
    assertEquals(GreyYouManager.unknownAbsorptions.get(1800), "5 adventures");
    assertTrue(GreyYouManager.unknownAbsorptions.containsKey(231));
    assertEquals(GreyYouManager.unknownAbsorptions.get(231), "100 adventures");
  }

  @Test
  public void canParseCharsheetOnlyInGreyYou() throws IOException {
    String responseText = loadHTMLResponse("request/test_grey_you_charsheet.html");

    // This is the method that CharSheetResponse calls
    GreyYouManager.parseAbsorptions(responseText);
    assertEquals(140, GreyYouManager.absorbedMonsters.size());
    assertEquals(0, GreyYouManager.unknownAbsorptions.size());
    assertEquals(49, GreyYouManager.learnedSkills.size());

    // Exit Grey You and try again
    KoLCharacter.setPath(Path.NONE);
    GreyYouManager.parseAbsorptions(responseText);
    assertEquals(0, GreyYouManager.absorbedMonsters.size());
    assertEquals(0, GreyYouManager.unknownAbsorptions.size());
    assertEquals(0, GreyYouManager.learnedSkills.size());
  }

  @Test
  public void canRegisterAbsorptionAfterFight() throws IOException {
    // This is the method that FightRequest calls
    String name1 = "oil baron";
    MonsterData monster1 = MonsterDatabase.findMonster(name1);
    assertNotNull(monster1);
    assertEquals(0, GreyYouManager.absorbedMonsters.size());
    GreyYouManager.absorbMonster(monster1);
    assertEquals(1, GreyYouManager.absorbedMonsters.size());
    assertTrue(GreyYouManager.absorbedMonsters.contains(monster1.getId()));

    String name2 = "Baiowulf";
    MonsterData monster2 = MonsterDatabase.findMonster(name2);
    assertNotNull(monster2);
    GreyYouManager.absorbMonster(monster2);
    assertEquals(1, GreyYouManager.absorbedMonsters.size());
    assertFalse(GreyYouManager.absorbedMonsters.contains(monster2.getId()));
  }
}
