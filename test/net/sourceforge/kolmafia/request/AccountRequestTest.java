package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AccountRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("AccountRequestTest");
    Preferences.reset("AccountRequestTest");
  }

  @Test
  public void testTimezoneIsSet() {
    // The default of 'timezone' is empty as mafia does not check for timezone unless the user
    // navigates to the page
    var cleanups =
        new Cleanups(
            withProperty("timezone", ""),
            withNextResponse(200, html("request/test_account_timezone.html")));

    try (cleanups) {
      var request = new GenericRequest("account.php?action=loadtab&value=account&pwd");
      request.run();

      assertThat("timezone", isSetTo("Pacific/Auckland"));
    }
  }
}
