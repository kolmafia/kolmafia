package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CoinmasterCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("testUser");
    Preferences.reset("testUser");
  }

  public CoinmasterCommandTest() {
    this.command = "coinmaster";
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
  }

  @Test
  public void neitherBuyNorSell() {
    String output;
    output = execute("somethingelse");

    assertErrorState();
    assertThat(output, containsString("Invalid coinmaster command."));
  }

  @Test
  public void noCoinmaster() {
    String output;
    output = execute("");

    assertErrorState();
    assertThat(output, containsString("Invalid coinmaster command."));
  }

  @Test
  public void unknownCoinmaster() {
    String output;
    output = execute("buy someguyIdontknow 1 thing");

    assertErrorState();
    assertThat(output, containsString("Which coinmaster is"));
  }

  @Test
  public void sellToSeller() {
    String output;
    output = execute("sell blackmarket 1 meat");

    assertErrorState();
    assertThat(output, containsString("You can't sell to"));
  }

  @Test
  public void buyBadItem() {
    String output;
    output = execute("buy blackmarket 1 rose");

    assertErrorState();
    assertThat(output, containsString("You can't buy rose from"));
  }

  @Test
  public void cantAccess() {
    String output;
    output = execute("buy socp 1 smoking pope");

    assertErrorState();
    assertThat(output, containsString("You do not have a Skeleton of Crimbo Past"));
  }

  @Test
  public void canBuy() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarDatabase.getFamiliarId("Skeleton of crimbo past")),
            withItem(ItemPool.KNUCKLEBONE, 5));
    String output;
    try (cleanups) {
      output = execute("buy socp 1 smoking pope");
    }
    var requests = getRequests();

    assertContinueState();
    assertThat(output, containsString("Visiting the Skeleton of Crimbo Past"));

    assertThat(requests, not(empty()));
  }
}
