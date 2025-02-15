package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsIterableContaining.hasItem;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FamTeamRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("FamTeamRequestTest");
    Preferences.reset("FamTeamRequestTest");
  }

  private FamTeamRequestTest() {}

  @Test
  public void canParseFamTeam() {
    FamTeamRequest.parseResponse("famteam.php", html("request/test_famteam.html"));
    // check team
    var team = KoLCharacter.getPokeTeam();
    assertThat(team, arrayWithSize(3));
    assertThat(team[0].getId(), is(FamiliarPool.WERETURTLE));
    assertThat(team[1].getId(), is(FamiliarPool.BARRRNACLE));
    assertThat(team[2].getId(), is(FamiliarPool.KILLER_BEE));
    // check bullpen
    var available = KoLCharacter.usableFamiliars();
    assertThat(available, hasSize(7));
    assertThat(available, hasItem(hasProperty("id", is(FamiliarPool.BURLY_BODYGUARD))));
    // should have registered the Burly Bodyguard's moves
    assertThat(
        FamiliarDatabase.getPokeDataById(FamiliarPool.BURLY_BODYGUARD).getMove2(), is("Hug"));
  }
}
