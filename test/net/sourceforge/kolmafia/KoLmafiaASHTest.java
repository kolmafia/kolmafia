package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.File;
import net.sourceforge.kolmafia.request.RelayRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class KoLmafiaASHTest {
  @Nested
  class Choice {
    @ParameterizedTest
    @ValueSource(
        strings = {
          "choice_override_ash",
          "no_choice_override_ash",
          "choice_override_js",
          "no_choice_override_js"
        })
    void callsNumberedChoiceScriptWithBackwardsCompatibility(final String directory) {
      try (var kolmafiaAsh = Mockito.mockStatic(KoLmafiaASH.class, Mockito.CALLS_REAL_METHODS)) {
        kolmafiaAsh
            .when(() -> KoLmafiaASH.getRelayFile("choice.1.ash"))
            .thenReturn(new File(KoLConstants.RELAY_LOCATION, directory + "/choice.1.ash"));
        kolmafiaAsh
            .when(() -> KoLmafiaASH.getRelayFile("choice.1.js"))
            .thenReturn(new File(KoLConstants.RELAY_LOCATION, directory + "/choice.1.js"));
        kolmafiaAsh
            .when(() -> KoLmafiaASH.getRelayFile("choice.ash"))
            .thenReturn(new File(KoLConstants.RELAY_LOCATION, directory + "/choice.ash"));

        var request = new RelayRequest(true);
        request.constructURLString("choice.php?whichchoice=1");
        // The request response should be overwritten
        request.responseText = "fail";

        var success = KoLmafiaASH.getClientHTML(request);

        assertThat(success, is(true));
        assertThat(request.responseText, equalTo("pass"));
      }
    }
  }
}
