package net.sourceforge.kolmafia.request.coinmaster.shop;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withCurrentRun;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLocation;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ShadowForgeRequestTest {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("ShadowForgeRequestTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("ShadowForgeRequestTest");
  }

  @Test
  void openingShadowForgeAllowsCreation() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("lastShadowForgeUnlockAdventure", -1),
            withCurrentRun(666),
            withItem(ItemPool.SHADOW_FLUID),
            withItem(ItemPool.SHADOW_FLAME),
            withItem(ItemPool.RUFUS_SHADOW_LODESTONE),
            withLocation("Shadow Rift"));

    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_follow_rufus_lodestone.html"));
      client.addResponse(200, ""); // api.php
      client.addResponse(302, Map.of("location", List.of("shop.php?whichshop=shadowforge")), "");
      client.addResponse(200, html("request/test_visit_shadow_forge.html"));

      // We have the ingredients for a shadow pill, but the forge is not open
      var concoction = ConcoctionPool.get(ItemPool.SHADOW_PILL);
      assertFalse(ShadowForgeRequest.DATA.isAccessible());

      var req = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.SHADOW_RIFT);
      req.run();

      // We followed the lodestone and are now in choice 1500.
      assertTrue(ChoiceManager.handlingChoice);
      assertEquals(1500, ChoiceManager.lastChoice);
      assertFalse(InventoryManager.hasItem(ItemPool.RUFUS_SHADOW_LODESTONE));

      // Go to The Shadow Forge
      var choice = new GenericRequest("choice.php?whichchoice=1500&option=1");
      choice.run();

      // We have unlocked the forge and can craft things
      assertThat("lastShadowForgeUnlockAdventure", isSetTo(666));
      assertTrue(ShadowForgeRequest.DATA.isAccessible());
    }
  }
}
