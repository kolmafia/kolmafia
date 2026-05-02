package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class BountyDatabaseTest {
  @Test
  void returnsExpectedFieldsForKnownBounty() {
    String name = "bean-shaped rock";
    String plural = "bean-shaped rocks";
    String monster = "beanbat";

    assertThat(BountyDatabase.getName(plural), is(name));
    assertThat(BountyDatabase.getNameByMonster(monster), is(name));
    assertThat(BountyDatabase.getPlural(name), is(plural));
    assertThat(BountyDatabase.getType(name), is("easy"));
    assertThat(BountyDatabase.getImage(name), is("bean.gif"));
    assertThat(BountyDatabase.getNumber(name), is(12));
    assertThat(BountyDatabase.getMonster(name), is(monster));
    assertThat(BountyDatabase.getLocation(name), is("The Beanbat Chamber"));
  }
}
