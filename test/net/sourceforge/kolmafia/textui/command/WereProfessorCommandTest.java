package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withPath;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class WereProfessorCommandTest extends AbstractCommandTestBase {

  @BeforeAll
  public static void init() {
    KoLCharacter.reset("WereProfessorCommandTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("WereProfessorCommandTest");
  }

  public WereProfessorCommandTest() {
    this.command = "wereprofessor";
  }

  @Nested
  class Research {
    @Nested
    class Show {
      @Test
      public void coloursNothingIfNotWereProfessor() {
        // If we are not a Mild-Mannered Professor, we will not make requests
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;

        var cleanups = new Cleanups(withHttpClientBuilder(builder), withPath(Path.NONE));

        try (cleanups) {
          var output = execute("research");
          assertThat(output, not(containsString("Visiting the Research Bench")));
          // All skills are not coloured and contain rp
          assertThat(output, containsString("<td>mus1 (10 rp)</td>"));
          assertThat(output, containsString("<td>res1 (20 rp)</td>"));
          assertThat(output, containsString("<td>punt (100 rp)</td>"));

          var requests = client.getRequests();
          assertThat(requests, hasSize(0));
        }
      }

      @Test
      public void verboseShowsDescriptions() {
        // If we are not a Mild-Mannered Professor, we will not make requests
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;

        var cleanups = new Cleanups(withHttpClientBuilder(builder), withPath(Path.NONE));

        try (cleanups) {
          var output = execute("research verbose");
          assertThat(output, not(containsString("Visiting the Research Bench")));
          // All skills are not coloured and contain rp and description
          assertThat(output, containsString("<td>mus1 (10 rp)<div>Mus +20%</div></td>"));
          assertThat(output, containsString("<td>res1 (20 rp)<div>Resist All +20%</div></td>"));
          assertThat(output, containsString("<td>punt (100 rp)<div>Punt (Banish)</div></td>"));

          var requests = client.getRequests();
          assertThat(requests, hasSize(0));
        }
      }

      @Test
      public void coloursResearchAsMildManneredProfessor() {
        // If we are a Mild-Mannered Professor, we will make requests
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;

        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withHandlingChoice(0),
                withContinuationState(),
                withPath(Path.WEREPROFESSOR),
                withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR));

        try (cleanups) {
          builder.client.addResponse(
              302, Map.of("location", List.of("choice.php?forceoption=0")), "");
          builder.client.addResponse(
              200, html("request/test_choice_wereprofessor_one_of_each_organ.html"));
          builder.client.addResponse(200, "");

          var output = execute("research");
          assertThat(
              "beastSkillsAvailable", isSetTo("rend1,skin1,stomach2,myst1,kick1,meat1,liver2"));
          assertThat(
              "beastSkillsKnown",
              isSetTo(
                  "mus1,mus2,mus3,hp1,hp2,hp3,stomach1,mox1,mox2,mox3,init1,init2,init3,liver1"));
          assertThat("wereProfessorResearchPoints", isSetTo(720));

          assertThat(output, containsString("Visiting the Research Bench"));
          // Known skills are black and do not contain rp
          assertThat(
              output,
              containsString("<td><span style=\"color:black font-weight:bold\">mus1</span></td>"));
          // Available skills are red and contain rp
          assertThat(
              output, containsString("<td><span style=\"color:red\">rend1 (20 rp)</span></td>"));
          // Unavailable skills are grey and contain rp
          assertThat(
              output, containsString("<td><span style=\"color:gray\">rend2 (30 rp)</span></td>"));
          // We print available research points
          assertThat(output, containsString("You have 720 Research Points available."));

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

    @Nested
    class Learn {
      @Test
      public void canLearnSkillAsMildManneredProfessor() {
        // If we are a Mild-Mannered Professor, we will make requests
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;

        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withHandlingChoice(0),
                withContinuationState(),
                withPath(Path.WEREPROFESSOR),
                withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR));

        try (cleanups) {
          builder.client.addResponse(
              302, Map.of("location", List.of("choice.php?forceoption=0")), "");
          builder.client.addResponse(200, html("request/test_research_bench_visit.html"));
          builder.client.addResponse(200, html("request/test_research_bench_research.html"));
          builder.client.addResponse(200, "");

          var output = execute("research perfecthair");
          assertThat(output, containsString("Visiting the Research Bench"));
          assertThat(
              output,
              containsString("Researching Janus kinase blockers (perfecthair) for 100 rp."));
          assertThat(output, containsString("You spent 100 rp to research Janus kinase blockers."));

          assertThat("beastSkillsAvailable", isSetTo("feed,feasting"));

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
      public void willNotLearnBogusSkill() {
        // If we are a Mild-Mannered Professor, we will make requests
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;

        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withPath(Path.WEREPROFESSOR),
                withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR));

        try (cleanups) {
          builder.client.addResponse(200, "");

          var output = execute("research bogus");
          assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
          assertThat(output, containsString("'bogus' is not known research"));

          var requests = client.getRequests();
          assertThat(requests, hasSize(0));
        }
      }

      @Test
      public void willNotLearnIfNotWereProfessor() {
        // If we are a Mild-Mannered Professor, we will make requests
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;

        var cleanups = new Cleanups(withHttpClientBuilder(builder), withPath(Path.NONE));

        try (cleanups) {
          builder.client.addResponse(200, "");

          var output = execute("research mus1");
          assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
          assertThat(output, containsString("Only WereProfessors can use their Research Bench."));

          var requests = client.getRequests();
          assertThat(requests, hasSize(0));
        }
      }

      @Test
      public void willNotLearnIfSavageBeast() {
        // If we are a Mild-Mannered Professor, we will make requests
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;

        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withPath(Path.WEREPROFESSOR),
                withIntrinsicEffect(EffectPool.SAVAGE_BEAST));

        try (cleanups) {
          builder.client.addResponse(200, "");

          var output = execute("research res1");
          assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
          assertThat(output, containsString("You are locked out of your Humble Cottage."));

          var requests = client.getRequests();
          assertThat(requests, hasSize(0));
        }
      }

      @Test
      public void willNotLearnAlreadyResearchedSkill() {
        // If we are a Mild-Mannered Professor, we will make requests
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;

        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withHandlingChoice(0),
                withContinuationState(),
                withPath(Path.WEREPROFESSOR),
                withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR));

        try (cleanups) {
          builder.client.addResponse(
              302, Map.of("location", List.of("choice.php?forceoption=0")), "");
          builder.client.addResponse(
              200, html("request/test_choice_wereprofessor_one_of_each_organ.html"));
          builder.client.addResponse(200, "");

          var output = execute("research stomach1");
          assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
          assertThat(output, containsString("You've already researched 'stomach1'."));

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

      @Test
      public void willNotLearnUnavailableSkill() {
        // If we are a Mild-Mannered Professor, we will make requests
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;

        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withHandlingChoice(0),
                withContinuationState(),
                withPath(Path.WEREPROFESSOR),
                withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR));

        try (cleanups) {
          builder.client.addResponse(
              302, Map.of("location", List.of("choice.php?forceoption=0")), "");
          builder.client.addResponse(
              200, html("request/test_choice_wereprofessor_one_of_each_organ.html"));
          builder.client.addResponse(200, "");

          var output = execute("research stomach3");
          assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
          assertThat(output, containsString("'stomach3' is not currently available to research."));

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

      @Test
      public void willNotLearnSkillWithoutSufficientResearchPoints() {
        // If we are a Mild-Mannered Professor, we will make requests
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;

        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withHandlingChoice(0),
                withContinuationState(),
                withPath(Path.WEREPROFESSOR),
                withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR));

        try (cleanups) {
          builder.client.addResponse(
              302, Map.of("location", List.of("choice.php?forceoption=0")), "");
          builder.client.addResponse(200, html("request/test_research_bench_research_known.html"));

          var output = execute("research feasting");
          assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
          assertThat(output, containsString("'feasting' requires 100 rp, but you only have 53."));

          var requests = client.getRequests();

          assertPostRequest(
              requests.get(0),
              "/place.php",
              "whichplace=wereprof_cottage&action=wereprof_researchbench");
          assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        }
      }
    }
  }
}
