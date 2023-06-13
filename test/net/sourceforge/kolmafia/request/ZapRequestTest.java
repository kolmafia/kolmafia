package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withItem;
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
  public void buildsARequestWithUnzappableAndNoWand() {
    AdventureResult accord = ItemPool.get(ItemPool.HERMIT_PERMIT);
    var zapRequest = new ZapRequest(accord);
    assertNotNull(zapRequest);
  }

  @Test
  public void buildsARequestWithZappableButNoWand() {
    AdventureResult bacon = ItemPool.get(ItemPool.BACONSTONE);
    var zapRequest = new ZapRequest(bacon);
    assertNotNull(zapRequest);
  }

  @Test
  public void buildsARequestWithUnzappableAndWand() {
    var cleanups = withItem("pine wand");

    try (cleanups) {
      AdventureResult accord = ItemPool.get(ItemPool.HERMIT_PERMIT);
      var zapRequest = new ZapRequest(accord);
      assertNotNull(zapRequest);
    }
  }

  @Test
  public void buildsARequestWithZappableAndWand() {
    var cleanups = withItem("pine wand");

    try (cleanups) {
      AdventureResult bacon = ItemPool.get(ItemPool.BACONSTONE);
      var zapRequest = new ZapRequest(bacon);
      assertNotNull(zapRequest);
    }
  }

  @Test
  public void itShouldHaveOtherZapData() {
    // Zap baconstone with wand. both in inventory
    AdventureResult bacon = ItemPool.get(ItemPool.BACONSTONE);
    withItem("pine wand");
    withItem("baconstone");
    LockableListModel<AdventureResult> items = ZapRequest.getZappableItems();
    assertThat(items, hasItem(bacon));
    List<String> zapGroup = ZapRequest.getZapGroup(ItemPool.BACONSTONE);
    assertThat(zapGroup, contains("baconstone", "hamethyst", "porquoise"));
    assertThat(zapGroup, hasSize(3));
    zapGroup = ZapRequest.getZapGroup(ItemPool.HERMIT_PERMIT);
    assertThat(zapGroup, hasSize(0));
  }

  @Test
  public void shouldTrimZappableItemsIfRequested() {
    Preferences.setBoolean("relayTrimsZapList", true);
    withItem("baconstone");
    withItem("hermit permit");
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
