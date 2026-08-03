package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import org.junit.jupiter.api.Test;

public class RestoresDatabaseTest {
  @Test
  void returnsExpectedFieldsForSimpleRestore() {
    String name = "green pixel potion";

    assertThat(RestoresDatabase.getType(name), is("item"));
    assertThat(RestoresDatabase.getHPMin(name), is(40L));
    assertThat(RestoresDatabase.getHPMax(name), is(60L));
    assertThat(RestoresDatabase.getMPMin(name), is(30L));
    assertThat(RestoresDatabase.getMPMax(name), is(40L));
  }

  @Test
  void returnsExpectedFieldsForStatelyRestore() {
    var cleanups = new Cleanups(withProperty("_aprilShower", 0));

    try (cleanups) {
      String name = "April Shower";

      assertThat(RestoresDatabase.getType(name), is("loc"));
      assertThat(RestoresDatabase.getHPMin(name), is(0L));
      assertThat(RestoresDatabase.getHPMax(name), is(0L));
      assertThat(RestoresDatabase.getMPMin(name), is(1000L));
      assertThat(RestoresDatabase.getMPMax(name), is(1000L));
      assertThat(RestoresDatabase.getAdvCost(name), is(0));
      assertThat(RestoresDatabase.getUsesLeft(name), is(1));
      assertThat(RestoresDatabase.getNotes(name), is(""));
    }
  }
}
