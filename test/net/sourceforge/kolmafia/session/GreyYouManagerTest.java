package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
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
}
