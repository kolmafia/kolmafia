package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import net.sourceforge.kolmafia.KoLCharacter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DemonNamesCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("DemonNamesCommandTest");
  }

  public DemonNamesCommandTest() {
    this.command = "demons";
  }

  @Test
  void listsDemonsWithNoParams() {
    String output = execute("");
    assertThat(output, containsString("Found in the Summoning Chamber"));
  }

  @Nested
  class Solve14 {
    @Test
    void noSegments() {
      var cleanups = withProperty("demonName14Segments", "");

      try (cleanups) {
        String output = execute("solve14");
        assertThat(output, containsString("You need to make bad requests"));
      }
    }

    @Test
    void fewSolutions() {
      var cleanups =
          withProperty("demonName14Segments", "Arg:5,Bal,Ball,Bar,Bob:2,But,Cak,Cal,Call");

      try (cleanups) {
        String output = execute("solve14");
        assertThat(output, containsString("do not have enough segments"));
      }
    }

    @Test
    void fewSegments() {
      var cleanups = withProperty("demonName14Segments", "Car,arC,rCa");

      try (cleanups) {
        String output = execute("solve14");
        assertThat(output, containsString("Unless you have a really unfortunate demon name"));
      }
    }

    @Test
    void listsOptions() {
      var cleanups =
          withProperty(
              "demonName14Segments",
              "Hut,utR,tRo,Rog,ogN,orN,rNi,Nix:5,ixA,Arg,rgP,gPh,Pha,haD,arH,aDa,ixK,xKr");

      try (cleanups) {
        String output = execute("solve14");
        assertThat(output, containsString("3 solution(s) found:"));
        assertThat(output, containsString(": MorNixArgPhaDarHutRogNixKru"));
        assertThat(output, containsString(": DorNixArgPhaDarHutRogNixKru"));
        assertThat(output, containsString(": CorNixArgPhaDarHutRogNixKru"));
      }
    }
  }
}
