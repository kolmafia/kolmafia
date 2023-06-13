package net.sourceforge.kolmafia.webui;

import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInStash;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
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

  @Nested
  class MilkCap {

    @Test
    public void itDecoratesWithMilkCapAvailable() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, "");

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withInteractivity(true),
              withItem(ItemPool.MILK_CAP, 1));

      try (cleanups) {
        var buffer =
            new StringBuffer(
                "<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)");
        ValhallaDecorator.decorateGashJump("ascend.php", buffer);
        assertThat(
            buffer.toString(), containsString("shop.php?whichshop=olivers\">spend milk cap(s)"));
      }
    }

    @Test
    public void itDoesNotDecoratesWithNoMilkCapAvailable() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, "");

      var cleanups = new Cleanups(withHttpClientBuilder(builder), withInteractivity(true));

      try (cleanups) {
        var buffer =
            new StringBuffer(
                "<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)");
        ValhallaDecorator.decorateGashJump("ascend.php", buffer);
        assertThat(
            buffer.toString(),
            not(containsString("shop.php?whichshop=olivers\">spend milk cap(s)")));
      }
    }
  }

  @Nested
  class MrStore2002 {
    @Test
    public void decoratesWithCredits() {
      var cleanups =
          new Cleanups(
              // trophy check
              withNextResponse(200, ""), withProperty("availableMrStore2002Credits", 2));

      try (cleanups) {
        var buffer =
            new StringBuffer(
                "<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)");
        ValhallaDecorator.decorateGashJump("ascend.php", buffer);
        assertThat(
            buffer.toString(),
            containsString(
                "<a href=\"shop.php?whichshop=mrstore2002\">Spend remaining 2002 Mr. Store Credits (2)</a>"));
      }
    }

    @Test
    public void doesNotDecorateWithoutCredits() {
      var cleanups =
          new Cleanups(
              // trophy check
              withNextResponse(200, ""), withProperty("availableMrStore2002Credits", 0));

      try (cleanups) {
        var buffer =
            new StringBuffer(
                "<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)");
        ValhallaDecorator.decorateGashJump("ascend.php", buffer);
        assertThat(buffer.toString(), not(containsString("shop.php?whichshop=mrstore2002")));
      }
    }
  }
}
