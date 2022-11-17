package net.sourceforge.kolmafia.webui;

import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInStash;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ValhallaDecoratorTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ValhallaDecoratorTest");
    Preferences.reset("ValhallaDecoratorTest");
  }

  @Test
  void doesntConsiderStashForGifts() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, "");

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withInteractivity(true),
            withProperty("autoSatisfyWithCloset", true),
            withProperty("autoSatisfyWithStorage", true),
            withProperty("autoSatisfyWithStash", true),
            withItemInCloset("toast", 1),
            withItemInStorage("toast", 10),
            withItemInStash("toast", 100),
            withItem("toast", 1000));

    try (cleanups) {
      var buffer =
          new StringBuffer(
              "<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)");
      ValhallaDecorator.decorateGashJump("ascend.php", buffer);

      assertThat(
          buffer.toString(),
          containsString("<a href=\"/KoLmafia/redirectedCommand?cmd=acquire+1011+toast"));
    }
  }
}
