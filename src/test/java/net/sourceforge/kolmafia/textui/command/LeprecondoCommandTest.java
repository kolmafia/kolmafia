package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LeprecondoCommandTest extends AbstractCommandTestBase {

  public LeprecondoCommandTest() {
    this.command = "leprecondo";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("LeprecondoCommandTest");
    Preferences.reset("LeprecondoCommandTest");
    ChoiceManager.handlingChoice = false;
  }

  private Cleanups hasLeprecondo() {
    return withItem(ItemPool.LEPRECONDO);
  }

  @Test
  void errorsWithNoLeprecondo() {
    String output = execute("");
    assertErrorState();
    assertThat(output, containsString("You need a Leprecondo"));
  }

  @Test
  void errorsWithInvalidCommand() {
    var cleanups = hasLeprecondo();

    try (cleanups) {
      String output = execute("frobnort");
      assertErrorState();
      assertThat(output, containsString("Usage: leprecondo <blank>"));
    }
  }

  @Nested
  class Status {
    @Test
    void showsFurniture() {
      var cleanups =
          new Cleanups(hasLeprecondo(), withProperty("leprecondoInstalled", "3,4,17,20"));

      try (cleanups) {
        String output = execute("");
        assertThat(
            output.trim(),
            equalTo(
                "You have the following furniture installed:<ul><li>boxes of old comic books</li><li>second-hand hot plate</li><li>ManCave&trade; sports bar set</li><li>fine upholstered dining table set</li></ul>"));
      }
    }

    @Test
    void showsNoFurniture() {
      var cleanups = new Cleanups(hasLeprecondo(), withProperty("leprecondoInstalled", "0,0,0,0"));

      try (cleanups) {
        String output = execute("");
        assertThat(
            output.trim(),
            equalTo(
                "You have the following furniture installed:<ul><li>nothing</li><li>nothing</li><li>nothing</li><li>nothing</li></ul>"));
      }
    }
  }

  @Nested
  class Furnish {
    @Test
    void errorsWithNoRearrangements() {
      var cleanups = new Cleanups(hasLeprecondo(), withProperty("_leprecondoRearrangements", 3));

      try (cleanups) {
        String output = execute("furnish ");
        assertErrorState();
        assertThat(output, containsString("All leprecondo rearrangements used today"));
      }
    }

    @Test
    void errorsWithNoFurniture() {
      var cleanups = hasLeprecondo();

      try (cleanups) {
        String output = execute("furnish");
        assertErrorState();
        assertThat(output, containsString("Usage: leprecondo furnish a,b,c,d"));
      }
    }

    @Test
    void errorsWithOnlySpacesForFurniture() {
      var cleanups = hasLeprecondo();

      try (cleanups) {
        String output = execute("furnish   ");
        assertErrorState();
        assertThat(output, containsString("Usage: leprecondo furnish a,b,c,d"));
      }
    }

    @Test
    void errorsWithWrongNumberOfFurniture() {
      var cleanups = hasLeprecondo();

      try (cleanups) {
        String output = execute("furnish trogdor");
        assertErrorState();
        assertThat(output, containsString("Usage: leprecondo furnish a,b,c,d"));
      }
    }

    @Test
    void errorsWithMissingFurniture() {
      var cleanups = hasLeprecondo();

      try (cleanups) {
        String output = execute("furnish home,star,run,ner");
        assertErrorState();
        assertThat(output, containsString("Unrecognised furniture name: home"));
      }
    }

    @Test
    void errorsWithAmbiguousFurniture() {
      var cleanups = new Cleanups(hasLeprecondo(), withProperty("leprecondoDiscovered", "2,7"));

      try (cleanups) {
        String output = execute("furnish i,am,a,fish");
        assertErrorState();
        assertThat(output, containsString("Ambiguous furniture name: i"));
      }
    }

    @Test
    void succeedsWithAvailableFurniture() {
      var cleanups =
          new Cleanups(hasLeprecondo(), withProperty("leprecondoDiscovered", "17,25,26,27"));

      try (cleanups) {
        String output = execute("furnish manc,four,wet,omni");
        assertContinueState();
        assertThat(
            output,
            containsString(
                "Furnishing Leprecondo with ManCave&trade; sports bar set, four-poster bed, fully-stocked wet bar, Omnipot"));
      }
    }
  }

  @Nested
  class Available {
    @Test
    public void showsAvailableFurniture() {
      var cleanups =
          new Cleanups(hasLeprecondo(), withProperty("leprecondoDiscovered", "3,4,5,17,20"));

      try (cleanups) {
        String output = execute("available");
        assertContinueState();
        assertThat(
            output,
            matchesRegex(
                Pattern.compile(
                    ".*<td>fine upholstered dining table set</td><td>(food|sleep)</td><td>(food|sleep)</td>.*",
                    Pattern.DOTALL)));
        assertThat(output, not(containsString("gigantic chess set")));
      }
    }
  }

  @Nested
  class Missing {
    @Test
    public void showsMissingFurnitureWithLocations() {
      var cleanups =
          new Cleanups(hasLeprecondo(), withProperty("leprecondoDiscovered", "3,4,5,17,20"));

      try (cleanups) {
        String output = execute("missing");
        assertContinueState();
        assertThat(
            output, containsString("<td>gigantic chess set</td><td>An Octopus's Garden</td>"));
        assertThat(output, not(containsString("fine upholstered dining table set")));
      }
    }
  }
}
