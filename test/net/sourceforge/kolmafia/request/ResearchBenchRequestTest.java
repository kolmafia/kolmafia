package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import internal.helpers.SessionLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ResearchBenchRequestTest {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("ResearchBenchRequestTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("ResearchBenchRequestTest");
  }

  @Nested
  class VisitResearchBench {
    @Test
    void canVisitWithResearchBenchRequestAsMildManneredProfessor() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withHandlingChoice(false),
              withContinuationState(),
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withProperty("beastSkillsAvailable", ""),
              withProperty("beastSkillsKnown", ""),
              withProperty("wereProfessorBite", 0),
              withProperty("wereProfessorKick", 0),
              withProperty("wereProfessorLiver", 0),
              withProperty("wereProfessorRend", 0),
              withProperty("wereProfessorResearchPoints", 0),
              withProperty("wereProfessorStomach", 0));

      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(200, html("request/test_research_bench_visit.html"));

        var request = new ResearchBenchRequest();
        request.run();

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        assertTrue(ChoiceManager.handlingChoice);

        assertThat("beastSkillsAvailable", isSetTo("feed,feasting,perfecthair"));
        assertThat(
            "beastSkillsKnown",
            isSetTo(
                "mus1,mus2,mus3,rend1,rend2,rend3,slaughter,hp1,hp2,hp3,skin1,skin2,skin3,skinheal,stomach1,stomach2,stomach3,myst1,myst2,myst3,bite1,bite2,bite3,howl,res1,res2,res3,items1,items2,items3,hunt,ml1,ml2,ml3,mox1,mox2,mox3,kick1,kick2,kick3,punt,init1,init2,init3,meat1,meat2,meat3,liver1,liver2,liver3,pureblood"));
        assertThat("wereProfessorBite", isSetTo(3));
        assertThat("wereProfessorKick", isSetTo(3));
        assertThat("wereProfessorRend", isSetTo(3));
        assertThat("wereProfessorLiver", isSetTo(3));
        assertThat("wereProfessorStomach", isSetTo(3));
        assertThat("wereProfessorResearchPoints", isSetTo(173));

        var text = SessionLoggerOutput.stopStream();
        assertTrue(text.contains("Concoct"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(
            requests.get(0),
            "/place.php",
            "whichplace=wereprof_cottage&action=wereprof_researchbench");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      }
    }

    @Test
    void cannotVisitWithResearchBenchRequestAsSavageBeast() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withHandlingChoice(false),
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.SAVAGE_BEAST));

      try (cleanups) {
        RequestLoggerOutput.startStream();
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(200, html("request/test_research_bench_visit.html"));

        var request = new ResearchBenchRequest();
        request.run();

        var output = RequestLoggerOutput.stopStream();
        assertTrue(
            output.contains("Only Mild-Mannered Professors can research at their Research Bench."));

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
        assertFalse(ChoiceManager.handlingChoice);

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    void canVisitWithGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withHandlingChoice(false),
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withProperty("beastSkillsAvailable", ""),
              withProperty("beastSkillsKnown", ""),
              withProperty("wereProfessorBite", 0),
              withProperty("wereProfessorKick", 0),
              withProperty("wereProfessorLiver", 0),
              withProperty("wereProfessorRend", 0),
              withProperty("wereProfessorResearchPoints", 0),
              withProperty("wereProfessorStomach", 0));

      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(200, html("request/test_research_bench_visit.html"));
        builder.client.addResponse(200, "");

        var request =
            new GenericRequest(
                "place.php?whichplace=wereprof_cottage&action=wereprof_researchbench");
        request.run();

        assertThat("beastSkillsAvailable", isSetTo("feed,feasting,perfecthair"));
        assertThat(
            "beastSkillsKnown",
            isSetTo(
                "mus1,mus2,mus3,rend1,rend2,rend3,slaughter,hp1,hp2,hp3,skin1,skin2,skin3,skinheal,stomach1,stomach2,stomach3,myst1,myst2,myst3,bite1,bite2,bite3,howl,res1,res2,res3,items1,items2,items3,hunt,ml1,ml2,ml3,mox1,mox2,mox3,kick1,kick2,kick3,punt,init1,init2,init3,meat1,meat2,meat3,liver1,liver2,liver3,pureblood"));
        assertThat("wereProfessorBite", isSetTo(3));
        assertThat("wereProfessorKick", isSetTo(3));
        assertThat("wereProfessorRend", isSetTo(3));
        assertThat("wereProfessorLiver", isSetTo(3));
        assertThat("wereProfessorStomach", isSetTo(3));
        assertThat("wereProfessorResearchPoints", isSetTo(173));

        var text = SessionLoggerOutput.stopStream();
        assertTrue(text.contains("Concoct"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(
            requests.get(0),
            "/place.php",
            "whichplace=wereprof_cottage&action=wereprof_researchbench");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      }
    }
  }
}
