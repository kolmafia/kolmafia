package net.sourceforge.kolmafia.webui;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextMonster;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class NemesisDecoratorTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("NemesisDecoratorTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("NemesisDecoratorTest");
  }

  @Nested
  class DiscoBandit {
    @Test
    public void decoratesRaverSpecialMove() {
      var cleanups = withNextMonster("breakdancing raver");

      try (cleanups) {
        StringBuffer text =
            new StringBuffer(
                html("request/test_raver_special_move_no_previous_selected_skill.html"));

        String before = text.toString();
        assertThat(
            before,
            containsString("<option picurl=tooth selected value=2>seal tooth (1)</option>"));
        assertThat(
            before,
            containsString(
                "<option value=\"49\" picurl=\"loop\" >Gothy Handwave (1 Mojo Point)</option>"));
        assertThat(
            before,
            containsString(
                "<td>The raver drops to the ground and starts spinning his legs wildly."));

        NemesisDecorator.decorateRaverFight(text);

        String after = text.toString();
        assertThat(
            after, containsString("<option picurl=tooth selected value=2>seal tooth (1)</option>"));
        assertThat(
            after,
            containsString(
                "<option value=\"49\" picurl=\"loop\" selected>Gothy Handwave (1 Mojo Point)</option>"));
        assertThat(
            after,
            containsString(
                "<td><font color=#DD00FF>The raver drops to the ground and starts spinning his legs wildly</font>."));
      }
    }

    @Test
    public void decoratesRaverSpecialMoveAndRemovesPreviousSelection() {
      var cleanups = withNextMonster("pop-and-lock raver");

      try (cleanups) {
        StringBuffer text =
            new StringBuffer(html("request/test_raver_special_move_previous_selected_skill.html"));

        String before = text.toString();
        assertThat(
            before,
            containsString("<option picurl=tooth selected value=2>seal tooth (1)</option>"));
        assertThat(
            before,
            containsString(
                "<option value=\"7514\" picurl=\"nicedart\" selected>Darts: Throw at leg (5 darts)</option>"));
        assertThat(
            before,
            containsString(
                "<option value=\"49\" picurl=\"loop\" >Gothy Handwave (1 Mojo Point)</option>"));
        assertThat(
            before, containsString("<td>The raver's movements suddenly become spastic and jerky."));

        NemesisDecorator.decorateRaverFight(text);

        String after = text.toString();
        assertThat(
            after, containsString("<option picurl=tooth selected value=2>seal tooth (1)</option>"));
        assertThat(
            after,
            containsString(
                "<option value=\"7514\" picurl=\"nicedart\" >Darts: Throw at leg (5 darts)</option>"));
        assertThat(
            after,
            containsString(
                "<option value=\"49\" picurl=\"loop\" selected>Gothy Handwave (1 Mojo Point)</option>"));
        assertThat(
            after,
            containsString(
                "<td><font color=#DD00FF>The raver's movements suddenly become spastic and jerky</font>."));
      }
    }

    @Test
    public void doesntDecorateRaverNoSpecialMove() {
      var cleanups = withNextMonster("pop-and-lock raver raver");

      try (cleanups) {
        StringBuffer text = new StringBuffer(html("request/test_raver_no_special_move.html"));

        String before = text.toString();
        assertThat(
            before,
            containsString("<option picurl=tooth selected value=2>seal tooth (1)</option>"));
        assertThat(
            before,
            containsString(
                "<option value=\"7514\" picurl=\"nicedart\" selected>Darts: Throw at leg (5 darts)</option>"));
        assertThat(
            before,
            containsString(
                "<option value=\"49\" picurl=\"loop\" >Gothy Handwave (1 Mojo Point)</option>"));

        NemesisDecorator.decorateRaverFight(text);

        String after = text.toString();
        assertThat(
            after, containsString("<option picurl=tooth selected value=2>seal tooth (1)</option>"));
        assertThat(
            after,
            containsString(
                "<option value=\"7514\" picurl=\"nicedart\" selected>Darts: Throw at leg (5 darts)</option>"));
        assertThat(
            after,
            containsString(
                "<option value=\"49\" picurl=\"loop\" >Gothy Handwave (1 Mojo Point)</option>"));
      }
    }
  }
}
