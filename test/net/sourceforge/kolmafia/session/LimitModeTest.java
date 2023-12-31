package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLimitMode;
import static internal.helpers.Player.withNoEffects;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class LimitModeTest {

  private static final String TESTUSERNAME = "LimitModeTestUser";

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset(TESTUSERNAME);
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset(TESTUSERNAME);
  }

  @Test
  void nameOfNoneIsBlank() {
    assertThat(LimitMode.NONE.getName(), is(""));
  }

  @Test
  void canFindByName() {
    assertThat(LimitMode.find("batman"), is(LimitMode.BATMAN));
  }

  @Test
  void findsUnknownWithUnknownName() {
    assertThat(LimitMode.find("gausie mode engage!"), is(LimitMode.UNKNOWN));
  }

  @Test
  void requiresReset() {
    assertThat(LimitMode.SPELUNKY.requiresReset(), is(true));
    assertThat(LimitMode.BATMAN.requiresReset(), is(true));
    assertThat(LimitMode.ED.requiresReset(), is(false));
  }

  @Nested
  class Skills {
    @Test
    void noLimitModeNoSkillsLimited() {
      assertThat(LimitMode.NONE.limitSkill(95), is(false));
    }

    @Test
    void spelunkyLimitsNonSpelunkySkills() {
      assertThat(LimitMode.SPELUNKY.limitSkill(7001), is(true));
      assertThat(LimitMode.SPELUNKY.limitSkill(7240), is(false));
    }

    @Test
    void batmanLimitsSkills() {
      assertThat(LimitMode.BATMAN.limitSkill(UseSkillRequest.getInstance(95)), is(true));
      assertThat(LimitMode.BATMAN.limitSkill(95), is(true));
    }
  }

  @Nested
  class Items {
    @Test
    void nothingLimitedNormally() {
      assertThat(LimitMode.NONE.limitItem(1), is(false));
    }

    @Test
    void everythingLimitedInEd() {
      assertThat(LimitMode.ED.limitItem(1), is(true));
    }

    @Test
    void itemsLimitedInSpelunky() {
      assertThat(LimitMode.SPELUNKY.limitItem(8030), is(true));
      assertThat(LimitMode.SPELUNKY.limitItem(8045), is(false));
    }

    @Test
    void itemsLimitedInBatman() {
      assertThat(LimitMode.BATMAN.limitItem(8790), is(true));
      assertThat(LimitMode.BATMAN.limitItem(8801), is(false));
      // In the range but not included (ROM of Optimality PvP reward)
      assertThat(LimitMode.BATMAN.limitItem(8800), is(true));
    }
  }

  @Nested
  class Slot {
    @Test
    void nothingLimitedNormally() {
      assertThat(LimitMode.NONE.limitSlot(net.sourceforge.kolmafia.equipment.Slot.HAT), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"BATMAN", "ED"})
    void someCantWearAnything(final LimitMode lm) {
      assertThat(lm.limitSlot(net.sourceforge.kolmafia.equipment.Slot.HAT), is(true));
    }

    @Test
    void spelunkersCanWearSomeThings() {
      assertThat(
          LimitMode.SPELUNKY.limitSlot(net.sourceforge.kolmafia.equipment.Slot.ACCESSORY1),
          is(false));
      assertThat(
          LimitMode.SPELUNKY.limitSlot(net.sourceforge.kolmafia.equipment.Slot.ACCESSORY2),
          is(true));
    }
  }

  @Nested
  class Outfits {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitOutfits(final LimitMode lm) {
      assertThat(lm.limitOutfits(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitOutfits(final LimitMode lm) {
      assertThat(lm.limitOutfits(), is(true));
    }
  }

  @Nested
  class Familiars {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitFamiliars(final LimitMode lm) {
      assertThat(lm.limitFamiliars(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitFamiliars(final LimitMode lm) {
      assertThat(lm.limitFamiliars(), is(true));
    }
  }

  @Nested
  class Adventures {
    @Test
    void mostZonesUnlimitedNormally() {
      var adventure = AdventureDatabase.getAdventureByName("The Dire Warren");
      assertThat(LimitMode.NONE.limitAdventure(adventure), is(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"The Mean Streets", "The Mines"})
    void cantAccessLimitZonesInUnlimited(final String adventureName) {
      var adventure = AdventureDatabase.getAdventureByName(adventureName);
      assertThat(LimitMode.NONE.limitAdventure(adventure), is(true));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "UNKNOWN", "BIRD", "ROACH"},
        mode = EnumSource.Mode.EXCLUDE)
    void cantAccessRegularZonesInLimitModes(final LimitMode lm) {
      var adventure = AdventureDatabase.getAdventureByName("The Dire Warren");
      assertThat(lm.limitAdventure(adventure), is(true));
    }
  }

  @Nested
  class Meat {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitMeat(final LimitMode lm) {
      assertThat(lm.limitMeat(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitMeat(final LimitMode lm) {
      assertThat(lm.limitMeat(), is(true));
    }
  }

  @Nested
  class Mall {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitMall(final LimitMode lm) {
      assertThat(lm.limitMall(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitMall(final LimitMode lm) {
      assertThat(lm.limitMall(), is(true));
    }
  }

  @Nested
  class NPCStores {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitNPCStores(final LimitMode lm) {
      assertThat(lm.limitNPCStores(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitNPCStores(final LimitMode lm) {
      assertThat(lm.limitNPCStores(), is(true));
    }
  }

  @Nested
  class Coinmasters {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitCoinmasters(final LimitMode lm) {
      assertThat(lm.limitCoinmasters(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitCoinmasters(final LimitMode lm) {
      assertThat(lm.limitCoinmasters(), is(true));
    }
  }

  @Nested
  class Clan {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitClan(final LimitMode lm) {
      assertThat(lm.limitClan(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitClan(final LimitMode lm) {
      assertThat(lm.limitClan(), is(true));
    }
  }

  @Nested
  class Campground {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitCampground(final LimitMode lm) {
      assertThat(lm.limitCampground(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitCampground(final LimitMode lm) {
      assertThat(lm.limitCampground(), is(true));
    }
  }

  @Nested
  class Storage {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitStorage(final LimitMode lm) {
      assertThat(lm.limitStorage(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitStorage(final LimitMode lm) {
      assertThat(lm.limitStorage(), is(true));
    }
  }

  @Nested
  class Eating {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitEating(final LimitMode lm) {
      assertThat(lm.limitEating(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitEating(final LimitMode lm) {
      assertThat(lm.limitEating(), is(true));
    }
  }

  @Nested
  class Drinking {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitDrinking(final LimitMode lm) {
      assertThat(lm.limitDrinking(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitDrinking(final LimitMode lm) {
      assertThat(lm.limitDrinking(), is(true));
    }
  }

  @Nested
  class Spleening {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitSpleening(final LimitMode lm) {
      assertThat(lm.limitSpleening(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitSpleening(final LimitMode lm) {
      assertThat(lm.limitSpleening(), is(true));
    }
  }

  @Nested
  class Pickpocket {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitPickpocket(final LimitMode lm) {
      assertThat(lm.limitPickpocket(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitPickpocket(final LimitMode lm) {
      assertThat(lm.limitPickpocket(), is(true));
    }
  }

  @Nested
  class MCD {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitMCD(final LimitMode lm) {
      assertThat(lm.limitMCD(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someLimitMCD(final LimitMode lm) {
      assertThat(lm.limitMCD(), is(true));
    }
  }

  @Nested
  class Recovery {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontLimitRecovery(final LimitMode lm) {
      assertThat(lm.limitRecovery(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN", "ED"})
    void someLimitRecovery(final LimitMode lm) {
      assertThat(lm.limitRecovery(), is(true));
    }
  }

  @Nested
  class CharPane {
    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"NONE", "ED", "ASTRAL", "BIRD", "MOLE", "ROACH"})
    void someDontRequireCharPane(final LimitMode lm) {
      assertThat(lm.requiresCharPane(), is(false));
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void someDoRequireCharPane(final LimitMode lm) {
      assertThat(lm.requiresCharPane(), is(true));
    }
  }

  // *** Pseudo LimitModes

  @Nested
  class Astral {
    private static final AdventureResult ASTRAL_MUSHROOM =
        ItemPool.get(ItemPool.ASTRAL_MUSHROOM, 1);
    private static final AdventureResult HALF_ASTRAL = EffectPool.get(EffectPool.HALF_ASTRAL);

    @Test
    void gainingHalfAstralEntersAstralLimitMode() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ASTRAL_MUSHROOM),
              withNoEffects(),
              withProperty("currentAstralTrip", ""),
              withLimitMode(LimitMode.NONE));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_astral_mushroom.html"));
        client.addResponse(200, ""); // api.php

        var request = UseItemRequest.getInstance(ASTRAL_MUSHROOM);
        request.run();

        assertEquals(5, HALF_ASTRAL.getCount(KoLConstants.activeEffects));
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.ASTRAL);

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.ASTRAL_MUSHROOM + "&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void losingHalfAstralLeavesAstralLimitMode() {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.HALF_ASTRAL, 1),
              withProperty("currentAstralTrip", "Great Trip"),
              withLimitMode(LimitMode.ASTRAL));
      try (cleanups) {
        AdventureResult adv = new AdventureResult(AdventureResult.ADV, -1);
        ResultProcessor.processResult(true, adv);

        assertEquals(0, HALF_ASTRAL.getCount(KoLConstants.activeEffects));
        assertThat(Preferences.getString("currentAstralTrip"), is(""));
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.NONE);
      }
    }
  }

  @Nested
  class Mole {
    private static final AdventureResult GONG = ItemPool.get(ItemPool.GONG, 1);
    private static final AdventureResult SHAPE_OF_MOLE = EffectPool.get(EffectPool.SHAPE_OF_MOLE);

    @Test
    void usingGongAndChoosingMoleEntersMoleLimitMode() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(GONG),
              withNoEffects(),
              withProperty("currentLlamaFormTrip", ""),
              withLimitMode(LimitMode.NONE));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_use_llama_lama_gong.html"));
        client.addResponse(200, html("request/test_choose_mole_form.html"));
        client.addResponse(200, ""); // api.php

        var request = UseItemRequest.getInstance(GONG);
        request.run();

        assertTrue(ChoiceManager.handlingChoice);
        assertEquals(276, ChoiceManager.lastChoice);

        // Talk to the Llama
        var choice = new GenericRequest("choice.php?whichchoice=276&option=2");
        choice.run();

        assertEquals(12, SHAPE_OF_MOLE.getCount(KoLConstants.activeEffects));
        assertThat(Preferences.getString("currentLlamaForm"), is("Mole"));
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.MOLE);

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.GONG + "&ajax=1");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=276&option=2");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void talkingWithLlamaLeavesMoleLimitMode() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withNoEffects(),
              withProperty("choiceAdventure277", 1),
              withProperty("currentLlamaForm", "Mole"),
              withLimitMode(LimitMode.MOLE));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php")), "");
        client.addResponse(200, html("request/test_leave_reincarnation.html"));
        client.addResponse(200, html("request/test_get_mole_reward.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.MT_MOLEHILL);
        request.run();

        // Talk to the Llama
        request = new GenericRequest("choice.php?whichchoice=277&option=1");
        request.run();

        assertThat(Preferences.getString("currentLlamaForm"), is(""));
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.NONE);

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.MT_MOLEHILL);
        assertGetRequest(requests.get(1), "/choice.php", null);
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=277&option=1");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Bird {
    private static final AdventureResult GONG = ItemPool.get(ItemPool.GONG, 1);
    private static final AdventureResult FORM_OF_BIRD = EffectPool.get(EffectPool.FORM_OF_BIRD);

    @Test
    void usingGongAndChoosingBirdEntersBirdLimitMode() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(GONG),
              withNoEffects(),
              withProperty("currentLlamaFormTrip", ""),
              withLimitMode(LimitMode.NONE));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_use_llama_lama_gong.html"));
        client.addResponse(200, html("request/test_choose_bird_form.html"));
        client.addResponse(200, ""); // api.php

        var request = UseItemRequest.getInstance(GONG);
        request.run();

        assertTrue(ChoiceManager.handlingChoice);
        assertEquals(276, ChoiceManager.lastChoice);

        // Talk to the Llama
        var choice = new GenericRequest("choice.php?whichchoice=276&option=3");
        choice.run();

        assertEquals(15, FORM_OF_BIRD.getCount(KoLConstants.activeEffects));
        assertThat(Preferences.getString("currentLlamaForm"), is("Bird"));
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.BIRD);

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.GONG + "&ajax=1");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=276&option=3");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void talkingWithLlamaLeavesBirdLimitMode() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withNoEffects(),
              withProperty("choiceAdventure277", 1),
              withProperty("currentLlamaForm", "Bird"),
              withLimitMode(LimitMode.BIRD));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php")), "");
        client.addResponse(200, html("request/test_leave_reincarnation.html"));
        client.addResponse(200, html("request/test_get_bird_reward.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.BARF_MOUNTAIN);
        request.run();

        // Talk to the Llama
        request = new GenericRequest("choice.php?whichchoice=277&option=1");
        request.run();

        assertThat(Preferences.getString("currentLlamaForm"), is(""));
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.NONE);

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.BARF_MOUNTAIN);
        assertGetRequest(requests.get(1), "/choice.php", null);
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=277&option=1");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Roach {
    private static final AdventureResult GONG = ItemPool.get(ItemPool.GONG, 1);
    private static final AdventureResult FORM_OF_ROACH = EffectPool.get(EffectPool.FORM_OF_ROACH);

    @Test
    void usingGongAndChoosingRoachEntersRocahLimitMode() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(GONG),
              withNoEffects(),
              withProperty("currentLlamaFormTrip", ""),
              withLimitMode(LimitMode.NONE));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_use_llama_lama_gong.html"));
        client.addResponse(200, html("request/test_choose_roach_form.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_visit_roach_1.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_visit_roach_2.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_visit_roach_3.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_get_roach_reward.html"));
        client.addResponse(200, ""); // api.php

        var request = UseItemRequest.getInstance(GONG);
        request.run();

        assertTrue(ChoiceManager.handlingChoice);
        assertEquals(276, ChoiceManager.lastChoice);

        // Talk to the Llama
        var choice = new GenericRequest("choice.php?whichchoice=276&option=1");
        choice.run();

        assertEquals(3, FORM_OF_ROACH.getCount(KoLConstants.activeEffects));
        assertThat(Preferences.getString("currentLlamaForm"), is("Roach"));
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.ROACH);

        // Three choice adventures as a roach
        choice = new GenericRequest("choice.php?whichchoice=278&option=2");
        choice.run();
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.ROACH);

        choice = new GenericRequest("choice.php?whichchoice=280&option=2");
        choice.run();
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.ROACH);

        choice = new GenericRequest("choice.php?whichchoice=286&option=1");
        choice.run();
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.ROACH);

        // Welcome Back!
        choice = new GenericRequest("choice.php?whichchoice=277&option=1");
        choice.run();
        assertThat(Preferences.getString("currentLlamaForm"), is(""));
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.NONE);

        var requests = client.getRequests();
        assertThat(requests, hasSize(12));

        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.GONG + "&ajax=1");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=276&option=1");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(4), "/choice.php", "whichchoice=278&option=2");
        assertPostRequest(requests.get(5), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(6), "/choice.php", "whichchoice=280&option=2");
        assertPostRequest(requests.get(7), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(8), "/choice.php", "whichchoice=286&option=1");
        assertPostRequest(requests.get(9), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(10), "/choice.php", "whichchoice=277&option=1");
        assertPostRequest(requests.get(11), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void talkingWithLlamaLeavesBirdLimitMode() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withNoEffects(),
              withProperty("choiceAdventure277", 1),
              withProperty("currentLlamaForm", "Bird"),
              withLimitMode(LimitMode.BIRD));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php")), "");
        client.addResponse(200, html("request/test_leave_reincarnation.html"));
        client.addResponse(200, html("request/test_get_bird_reward.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.BARF_MOUNTAIN);
        request.run();

        // Talk to the Llama
        request = new GenericRequest("choice.php?whichchoice=277&option=1");
        request.run();

        assertEquals(KoLCharacter.getLimitMode(), LimitMode.NONE);
        assertThat(Preferences.getString("currentLlamaForm"), is(""));

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.BARF_MOUNTAIN);
        assertGetRequest(requests.get(1), "/choice.php", null);
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=277&option=1");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
