package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getLastRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItems;

import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.HermitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HermitCommandTest extends AbstractCommandTestBase {

  public HermitCommandTest() {
    this.command = "hermit";
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
    HermitRequest.initialize();
  }

  @Test
  public void acquiresJabaneroPepper() {
    var cleanups = withItems(ItemPool.WORTHLESS_TRINKET, ItemPool.HERMIT_PERMIT);

    try (cleanups) {
      execute("jabanero");
    }

    assertPostRequest(getLastRequest(), "/hermit.php", "action=trade&quantity=1&whichitem=55");
  }
}
