package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class BigBrotherRequestTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("Big Brother");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("Big Brother");
  }

  @Nested
  class Quests {

    private Cleanups withSeaQuestProgress() {
      return new Cleanups(
          // Minimum to get to The Sea
          withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
          // Minimum to talk to Big Brother
          withQuestProgress(Quest.SEA_MONKEES, "step3"),
          withProperty("bigBrotherRescued", true),
          withProperty("dampOldBootPurchased", false),
          withProperty("mapToAnemoneMinePurchased", false),
          withProperty("mapToMadnessReefPurchased", false),
          withProperty("mapToTheDiveBarPurchased", false),
          withProperty("mapToTheMarinaraTrenchPurchased", false),
          withProperty("mapToTheSkateParkPurchased", false));
    }

    @Test
    public void visitingBigBrotherAdvancesOldGuyQuest() {
      var builder = new FakeHttpClientBuilder();
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withSeaQuestProgress());
      try (cleanups) {
        // This response text does not contain a damp old boot
        builder.client.addResponse(200, html("request/test_visit_big_brother.html"));
        String URL = "monkeycastle.php?who=2";
        GenericRequest request = new GenericRequest(URL);
        request.run();
        assertThat("dampOldBootPurchased", isSetTo(true));
        assertThat(Quest.SEA_OLD_GUY, isStep("step1"));
      }
    }

    @Test
    public void visitingBigBrotherDetectsOptionalMapsPurchased() {
      var builder = new FakeHttpClientBuilder();
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withSeaQuestProgress());
      try (cleanups) {
        // This response text does not contain a map to Madness Reef
        // This response text does not contain a map to The Skate Park
        builder.client.addResponse(200, html("request/test_visit_big_brother.html"));
        String URL = "monkeycastle.php?who=2";
        GenericRequest request = new GenericRequest(URL);
        request.run();
        assertThat("mapToMadnessReefPurchased", isSetTo(true));
        assertThat("mapToTheSkateParkPurchased", isSetTo(true));
      }
    }

    @ParameterizedTest
    @EnumSource(
        value = AscensionClass.class,
        names = {"SEAL_CLUBBER", "SAUCEROR", "ACCORDION_THIEF"})
    public void visitingBigBrotherDetectsQuestMapsPurchased(AscensionClass clazz) {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withSeaQuestProgress(), withClass(clazz));
      try (cleanups) {
        // This response text does not contain a map to Anemone Mine
        // This response text does not contain a map to The Marinara Trench
        // This response text does not contain a map to The Five Bar
        builder.client.addResponse(200, html("request/test_visit_big_brother.html"));
        String URL = "monkeycastle.php?who=2";
        GenericRequest request = new GenericRequest(URL);
        request.run();
        assertThat("mapToAnemoneMinePurchased", isSetTo(clazz.getMainStat() != Stat.MUSCLE));
        assertThat(
            "mapToTheMarinaraTrenchPurchased", isSetTo(clazz.getMainStat() != Stat.MYSTICALITY));
        assertThat("mapToTheDiveBarPurchased", isSetTo(clazz.getMainStat() != Stat.MOXIE));
      }
    }
  }
}
