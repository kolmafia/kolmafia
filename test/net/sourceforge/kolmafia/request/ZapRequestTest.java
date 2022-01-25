package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.addItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.Test;

public class ZapRequestTest {
  @Test
  public void itShouldBuildARequestUnderSeveralCases() {
    // with null
    ZapRequest zapRequest = new ZapRequest(null);
    assertNotNull(zapRequest);
    // with unzappable item but no wand
    AdventureResult accord = ItemPool.get(ItemPool.HERMIT_PERMIT);
    zapRequest = new ZapRequest(accord);
    assertNotNull(zapRequest);
    // with Zappable item (baconstone) but no wand
    AdventureResult bacon = ItemPool.get(ItemPool.BACONSTONE);
    zapRequest = new ZapRequest(bacon);
    assertNotNull(zapRequest);
    // get a wand so can zap
    addItem("pine wand");
    zapRequest = new ZapRequest(accord);
    assertNotNull(zapRequest);
    zapRequest = new ZapRequest(bacon);
    assertNotNull(zapRequest);
  }

  @Test
  public void itShouldHaveOtherZapData() {
    // Zap baconstone with wand. both in inventory
    AdventureResult bacon = ItemPool.get(ItemPool.BACONSTONE);
    addItem("pine wand");
    addItem("baconstone");
    LockableListModel<AdventureResult> items = ZapRequest.getZappableItems();
    assertThat(items, hasItem(bacon));
    List<String> zapGroup = ZapRequest.getZapGroup(ItemPool.BACONSTONE);
    ;
    assertThat(zapGroup, contains("baconstone", "hamethyst", "porquoise"));
    assertThat(zapGroup, hasSize(3));
    zapGroup = ZapRequest.getZapGroup(ItemPool.HERMIT_PERMIT);
    assertThat(zapGroup, hasSize(0));
  }

  @Test
  public void shouldTrimZappableItemsIfRequested() {
    Preferences.setBoolean("relayTrimsZapList", true);
    addItem("baconstone");
    addItem("hermit permit");
    AdventureResult baconstone = ItemPool.get(ItemPool.BACONSTONE);
    assertThat(ZapRequest.getZappableItems(), hasItem(baconstone));
  }

  @Test
  public void parsesEscapedCommaSeparatedData() {
    int TP_MUMBLEBEE = 5944;
    List<String> zapGroup = ZapRequest.getZapGroup(TP_MUMBLEBEE);
    assertThat(zapGroup, hasItem("tiny plastic Hank North, Photojournalist"));
    assertThat(zapGroup, hasSize(19));
  }
}
