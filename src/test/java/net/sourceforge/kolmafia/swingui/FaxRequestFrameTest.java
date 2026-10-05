package net.sourceforge.kolmafia.swingui;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withDataFile;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FaxRequestFrameTest {
  private static Cleanups globalCleanup;

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("FaxRequestFrameTest");
    Preferences.reset("FaxRequestFrameTest");
    ChatManager.setChatLiteracy(true);

    globalCleanup =
        new Cleanups(
            withDataFile("cheesefax.xml"),
            withDataFile("easyfax.xml"),
            withDataFile("onlyfax.xml"));
    FaxBotDatabase.configure();
  }

  @AfterAll
  static void afterAll() {
    globalCleanup.close();
  }

  private static Monster easyfaxMonster(final String name) {
    return FaxBotDatabase.getFaxbot("Easyfax").getMonsterByActualName(name);
  }

  @Nested
  class NoResponse {
    @Test
    void keepsRequestedMonsterFromFaxMachine() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, "You approach the fax machine.");
      builder.client.addResponse(200, ""); // submitnewchat.php
      builder.client.addResponse(200, html("request/test_clan_fax_receive.html"));
      builder.client.addResponse(200, html("request/test_desc_item_photocopied_mariachi.html"));
      builder.client.addResponse(200, ""); // api.php

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withItem(ItemPool.PHOTOCOPIED_MONSTER, 0),
              withProperty("photocopyMonster", ""),
              withProperty("lastSuccessfulFaxbot", ""));

      try (cleanups) {
        boolean result =
            FaxRequestFrame.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false, 0);

        var requests = builder.client.getRequests();
        assertThat(result, is(true));
        assertThat(requests, hasSize(5));
        assertPostRequest(
            requests.get(2), "/clan_viplounge.php", "preaction=receivefax&whichfloor=2");
        assertThat("photocopyMonster", isSetTo("handsome mariachi"));
        assertThat("lastSuccessfulFaxbot", isSetTo("Easyfax"));
      }
    }

    @Test
    void returnsOtherMonsterToFaxMachine() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, "You approach the fax machine.");
      builder.client.addResponse(200, ""); // submitnewchat.php
      builder.client.addResponse(200, html("request/test_clan_fax_receive.html"));
      builder.client.addResponse(200, html("request/test_desc_item_photocopied_mariachi.html"));
      builder.client.addResponse(200, ""); // api.php
      builder.client.addResponse(200, "Your photocopy slowly slides into the machine");

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withItem(ItemPool.PHOTOCOPIED_MONSTER, 0),
              withProperty("photocopyMonster", ""),
              withProperty("lastSuccessfulFaxbot", ""));

      try (cleanups) {
        boolean result =
            FaxRequestFrame.requestFax(
                "Easyfax", easyfaxMonster("Knob Goblin Embezzler"), false, 0);

        var requests = builder.client.getRequests();
        assertThat(result, is(false));
        assertThat(requests, hasSize(6));
        assertPostRequest(requests.get(5), "/clan_viplounge.php", "preaction=sendfax&whichfloor=2");
        assertThat(InventoryManager.hasItem(ItemPool.PHOTOCOPIED_MONSTER), is(false));
        assertThat("photocopyMonster", isSetTo(""));
        assertThat("lastSuccessfulFaxbot", isSetTo(""));
      }
    }

    @Test
    void failsWhenFaxMachineIsEmpty() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, "You approach the fax machine.");
      builder.client.addResponse(200, ""); // submitnewchat.php
      builder.client.addResponse(200, "just be a blank sheet of paper");

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withItem(ItemPool.PHOTOCOPIED_MONSTER, 0),
              withProperty("lastSuccessfulFaxbot", ""));

      try (cleanups) {
        boolean result =
            FaxRequestFrame.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false, 0);

        var requests = builder.client.getRequests();
        assertThat(result, is(false));
        assertThat(requests, hasSize(3));
        assertPostRequest(
            requests.get(2), "/clan_viplounge.php", "preaction=receivefax&whichfloor=2");
        assertThat("lastSuccessfulFaxbot", isSetTo(""));
      }
    }
  }

  @Test
  void cannotRequestFaxWithoutVipKey() {
    var builder = new FakeHttpClientBuilder();

    var cleanups = new Cleanups(withHttpClientBuilder(builder));

    try (cleanups) {
      boolean result =
          FaxRequestFrame.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false);

      assertThat(result, is(false));
      assertThat(builder.client.getRequests(), empty());
    }
  }
}
