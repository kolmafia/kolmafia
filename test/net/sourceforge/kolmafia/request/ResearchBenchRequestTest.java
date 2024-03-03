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
        assertTrue(text.contains("Visiting the Research Bench"));

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
    void canVisitWithGenericRequestAsMildManneredProfessor() {
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
        assertTrue(text.contains("Visiting the Research Bench"));

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
    void canVisitEmptyResearchBenchAndSetAllSkills() {
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
        builder.client.addResponse(200, html("request/test_research_bench_visit_empty.html"));

        var request = new ResearchBenchRequest();
        request.run();

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        assertFalse(ChoiceManager.handlingChoice);

        assertThat("beastSkillsAvailable", isSetTo(""));
        assertThat(
            "beastSkillsKnown",
            isSetTo(
                "mus1,mus2,mus3,rend1,rend2,rend3,slaughter,hp1,hp2,hp3,skin1,skin2,skin3,skinheal,stomach1,stomach2,stomach3,feed,myst1,myst2,myst3,bite1,bite2,bite3,howl,res1,res2,res3,items1,items2,items3,hunt,ml1,ml2,ml3,feasting,mox1,mox2,mox3,kick1,kick2,kick3,punt,init1,init2,init3,meat1,meat2,meat3,perfecthair,liver1,liver2,liver3,pureblood"));

        assertThat("wereProfessorBite", isSetTo(3));
        assertThat("wereProfessorKick", isSetTo(3));
        assertThat("wereProfessorRend", isSetTo(3));
        assertThat("wereProfessorLiver", isSetTo(3));
        assertThat("wereProfessorStomach", isSetTo(3));
        assertThat("wereProfessorResearchPoints", isSetTo(74));

        var text = SessionLoggerOutput.stopStream();
        assertTrue(text.contains("Visiting the Research Bench"));

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

  @Nested
  class ResearchAtResearchBench {
    @Test
    void canResearchWithResearchBenchRequest() {
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
              withProperty("wereProfessorResearchPoints", 0));

      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(200, html("request/test_research_bench_visit.html"));
        builder.client.addResponse(200, html("request/test_research_bench_research.html"));
        builder.client.addResponse(200, "");

        var request = new ResearchBenchRequest("perfecthair");
        request.run();

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        assertTrue(ChoiceManager.handlingChoice);

        assertThat("beastSkillsAvailable", isSetTo("feed,feasting"));
        assertThat(
            "beastSkillsKnown",
            isSetTo(
                "mus1,mus2,mus3,rend1,rend2,rend3,slaughter,hp1,hp2,hp3,skin1,skin2,skin3,skinheal,stomach1,stomach2,stomach3,myst1,myst2,myst3,bite1,bite2,bite3,howl,res1,res2,res3,items1,items2,items3,hunt,ml1,ml2,ml3,mox1,mox2,mox3,kick1,kick2,kick3,punt,init1,init2,init3,meat1,meat2,meat3,perfecthair,liver1,liver2,liver3,pureblood"));
        assertThat("wereProfessorResearchPoints", isSetTo(73));

        var text = SessionLoggerOutput.stopStream();
        assertTrue(text.contains("Visiting the Research Bench"));
        assertTrue(text.contains("Researching Janus kinase blockers (perfecthair) for 100 rp."));
        assertTrue(text.contains("You spent 100 rp to research Janus kinase blockers."));

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0),
            "/place.php",
            "whichplace=wereprof_cottage&action=wereprof_researchbench");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(
            requests.get(2), "/choice.php", "whichchoice=1523&option=1&r=wereprof_perfecthair");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void canResearchFinalSkillAndSetProperties() {
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
              withProperty("wereProfessorResearchPoints", 0));

      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(200, html("request/test_research_bench_visit.html"));
        builder.client.addResponse(200, html("request/test_research_bench_research_final.html"));
        builder.client.addResponse(200, "");

        var request = new ResearchBenchRequest("feed");
        request.run();

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        assertFalse(ChoiceManager.handlingChoice);

        assertThat("beastSkillsAvailable", isSetTo(""));
        assertThat(
            "beastSkillsKnown",
            isSetTo(
                "mus1,mus2,mus3,rend1,rend2,rend3,slaughter,hp1,hp2,hp3,skin1,skin2,skin3,skinheal,stomach1,stomach2,stomach3,feed,myst1,myst2,myst3,bite1,bite2,bite3,howl,res1,res2,res3,items1,items2,items3,hunt,ml1,ml2,ml3,feasting,mox1,mox2,mox3,kick1,kick2,kick3,punt,init1,init2,init3,meat1,meat2,meat3,perfecthair,liver1,liver2,liver3,pureblood"));
        assertThat("wereProfessorResearchPoints", isSetTo(74));

        var text = SessionLoggerOutput.stopStream();
        assertTrue(text.contains("Visiting the Research Bench"));
        assertTrue(text.contains("Researching Cholecystokinin antagonist (feed) for 100 rp."));
        assertTrue(text.contains("You spent 100 rp to research Cholecystokinin antagonist."));

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0),
            "/place.php",
            "whichplace=wereprof_cottage&action=wereprof_researchbench");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(
            requests.get(2), "/choice.php", "whichchoice=1523&option=1&r=wereprof_feed");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void canResearchWithGenericRequest() {
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
              withProperty("wereProfessorResearchPoints", 0));

      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(200, html("request/test_research_bench_visit.html"));
        builder.client.addResponse(200, html("request/test_research_bench_research.html"));
        builder.client.addResponse(200, "");

        var visit =
            new GenericRequest(
                "place.php?whichplace=wereprof_cottage&action=wereprof_researchbench");
        visit.run();

        assertThat("beastSkillsAvailable", isSetTo("feed,feasting,perfecthair"));
        assertThat(
            "beastSkillsKnown",
            isSetTo(
                "mus1,mus2,mus3,rend1,rend2,rend3,slaughter,hp1,hp2,hp3,skin1,skin2,skin3,skinheal,stomach1,stomach2,stomach3,myst1,myst2,myst3,bite1,bite2,bite3,howl,res1,res2,res3,items1,items2,items3,hunt,ml1,ml2,ml3,mox1,mox2,mox3,kick1,kick2,kick3,punt,init1,init2,init3,meat1,meat2,meat3,liver1,liver2,liver3,pureblood"));
        assertThat("wereProfessorResearchPoints", isSetTo(173));

        var research =
            new GenericRequest("choice.php?whichchoice=1523&option=1&r=wereprof_perfecthair");
        research.run();

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        assertTrue(ChoiceManager.handlingChoice);

        assertThat("beastSkillsAvailable", isSetTo("feed,feasting"));
        assertThat(
            "beastSkillsKnown",
            isSetTo(
                "mus1,mus2,mus3,rend1,rend2,rend3,slaughter,hp1,hp2,hp3,skin1,skin2,skin3,skinheal,stomach1,stomach2,stomach3,myst1,myst2,myst3,bite1,bite2,bite3,howl,res1,res2,res3,items1,items2,items3,hunt,ml1,ml2,ml3,mox1,mox2,mox3,kick1,kick2,kick3,punt,init1,init2,init3,meat1,meat2,meat3,perfecthair,liver1,liver2,liver3,pureblood"));
        assertThat("wereProfessorResearchPoints", isSetTo(73));

        var text = SessionLoggerOutput.stopStream();
        assertTrue(text.contains("Visiting the Research Bench"));
        assertTrue(text.contains("Researching Janus kinase blockers (perfecthair) for 100 rp."));
        assertTrue(text.contains("You spent 100 rp to research Janus kinase blockers."));

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0),
            "/place.php",
            "whichplace=wereprof_cottage&action=wereprof_researchbench");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(
            requests.get(2), "/choice.php", "whichchoice=1523&option=1&r=wereprof_perfecthair");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class ResearchBenchRequestFailures {
    @Test
    void mustSpecifyValidResearch() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withHandlingChoice(false),
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR));

      try (cleanups) {
        RequestLoggerOutput.startStream();
        var request = new ResearchBenchRequest("bogus");
        var output1 = RequestLoggerOutput.stopStream();
        assertTrue(output1.contains("Research 'bogus' is not valid"));
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));

        // If you ignore that and attempt to run the request, same error
        RequestLoggerOutput.startStream();
        request.run();
        var output2 = RequestLoggerOutput.stopStream();
        assertTrue(output2.contains("Research 'bogus' is not valid"));
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
        assertFalse(ChoiceManager.handlingChoice);

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    void canDetectInvalidResearchResponse() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      SessionLoggerOutput.startStream();

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withHandlingChoice(false),
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR));

      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(200, html("request/test_choice_wereprofessor_no_upgrades.html"));
        builder.client.addResponse(
            200, html("request/test_research_bench_research_unavailable.html"));

        var visit =
            new GenericRequest(
                "place.php?whichplace=wereprof_cottage&action=wereprof_researchbench");
        var research = new GenericRequest("choice.php?whichchoice=1523&option=1&r=wereprof_hunt");

        visit.run();
        research.run();

        var output = SessionLoggerOutput.stopStream();
        assertTrue(output.contains("Visiting the Research Bench"));
        assertTrue(output.contains("Researching Phantosmic tincture (hunt) for 100 rp."));
        assertTrue(output.contains("You failed to research Phantosmic tincture."));

        assertFalse(ChoiceManager.handlingChoice);

        var requests = client.getRequests();
        assertThat(requests, hasSize(3));

        assertPostRequest(
            requests.get(0),
            "/place.php",
            "whichplace=wereprof_cottage&action=wereprof_researchbench");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(
            requests.get(2), "/choice.php", "whichchoice=1523&option=1&r=wereprof_hunt");
      }
    }

    @Test
    void mustBeAMildManneredProfessor() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withHandlingChoice(false),
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.SAVAGE_BEAST));

      try (cleanups) {
        RequestLoggerOutput.startStream();

        var request = new ResearchBenchRequest("hunt");
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
    void mustBeUnknownResearch() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withHandlingChoice(false),
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withProperty("beastSkillsKnown", "hunt"));

      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(200, html("request/test_research_bench_visit.html"));

        RequestLoggerOutput.startStream();

        var request = new ResearchBenchRequest("hunt");
        request.run();

        var output = RequestLoggerOutput.stopStream();
        assertTrue(output.contains("You have already researched 'hunt'."));

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
        assertTrue(ChoiceManager.handlingChoice);

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
    void mustBeAvailableResearch() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withHandlingChoice(false),
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withProperty("beastSkillsKnown", "hunt"));

      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(
            200, html("request/test_choice_wereprofessor_three_of_each_organ.html"));

        RequestLoggerOutput.startStream();

        var request = new ResearchBenchRequest("hunt");
        request.run();

        var output = RequestLoggerOutput.stopStream();
        assertTrue(output.contains("You cannot research 'hunt' at this time."));

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
        assertTrue(ChoiceManager.handlingChoice);

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
    void mustHaveEnoughResearchPointsAvailable() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withHandlingChoice(false),
              withPath(Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR));

      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        builder.client.addResponse(200, html("request/test_research_bench_research_known.html"));

        RequestLoggerOutput.startStream();

        var request = new ResearchBenchRequest("feasting");
        request.run();

        var output = RequestLoggerOutput.stopStream();
        assertTrue(output.contains("You don't have enough rp to research 'feasting'."));

        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
        assertTrue(ChoiceManager.handlingChoice);

        var requests = client.getRequests();
        assertThat(requests, hasSize(3));

        assertPostRequest(
            requests.get(0),
            "/place.php",
            "whichplace=wereprof_cottage&action=wereprof_researchbench");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
