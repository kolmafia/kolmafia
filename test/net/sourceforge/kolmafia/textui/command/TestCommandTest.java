package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withStats;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TestCommandTest extends AbstractCommandTestBase {

  public TestCommandTest() {
    this.command = "test";
  }

  @BeforeAll
  public static void init() {
    KoLCharacter.reset("TestCommandTest");
  }

  @Nested
  class Evilometer {
    private static final String[] filesToMove = {"evilometer_fight.html"};

    @BeforeAll
    static void copyDataFiles() throws IOException {
      for (String s : filesToMove) {
        Path source = Paths.get(KoLConstants.ROOT_LOCATION + "/request/" + s);
        Path dest = Paths.get(KoLConstants.ROOT_LOCATION + "/data/" + s);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
      }
    }

    @AfterAll
    static void deleteCopiedDataFiles() {
      for (String s : filesToMove) {
        Path dest = Paths.get(KoLConstants.ROOT_LOCATION + "/data/" + s);
        try {
          Files.delete(dest);
        } catch (IOException e) {
          // leave it
        }
      }
    }

    @Test
    public void loadsFight() {
      // avoid "you can now equip a X"
      var cleanups = withStats(300, 300, 300);

      try (cleanups) {
        String outputLoad = execute("load evilometer_fight.html");
        assertThat(outputLoad, startsWith("Read 9,727 bytes into a 9,727 character string"));

        String outputFight = execute("fight 1");
        assertThat(
            outputFight,
            equalTo(
                """
            Round 2: gargantulihc takes 91 damage.
            Round 2: gargantulihc takes 80 damage.
            Round 2: You lose 17 hit points
            Round 2: TestCommandTest wins the fight!
            After Battle: As the gargantulihc fades away into nothingness, your Evilometer emits a single loud beep.
            After Battle: Ben stands up on his hind legs and sniffs the air, chirping adorably.
            After Battle: You gain 3 Mana Points
            You acquire an item: lihc eye
            You acquire an item: lihc eye
            After Battle: You gain 18 Fortitude
            You gain a Muscle point!
            After Battle: You gain 44 Wizardliness
            You gain a Mysticality point!
            After Battle: You gain 19 Smarm
            """));
      }
    }
  }
}
