package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.*;
import static internal.helpers.Player.*;
import static internal.matchers.Maximizer.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MaximizerCreatableTest {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("creator");
    Preferences.reset("creator");
  }

  @Test
  public void canPasteAsshat() {
    try (var _ =
        new Cleanups(
            withItem("bum cheek", 2), withItem("meat paste", 1), withConcoctionRefresh())) {
      maximizeCreatable("sleaze dmg");
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "asshat")));
    }
  }

  @Test
  public void doesNotRecommendTinyBlackHoleInStandard() {
    try (var _ =
        new Cleanups(
            withRestricted(true),
            withItem("coconut shell", 1),
            withItem("lime", 1),
            withItem("meat paste", 1),
            withProperty("unknownRecipe5069", false),
            withNotAllowedInStandard(RestrictedItemType.ITEMS, "tiny black hole"),
            withConcoctionRefresh())) {
      maximizeCreatable("item +offhand");
      assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND, "tiny black hole"))));
    }
  }

  @Nested
  class BarrelShrine {
    @Test
    public void canCreateBarrelItems() {
      try (var _ = withProperty("barrelShrineUnlocked", true)) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("ml");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "barrel lid")));
      }
    }

    @Test
    public void cannotCreateBarrelItemsTwice() {
      try (var _ =
          new Cleanups(
              withProperty("barrelShrineUnlocked", true),
              withProperty("prayedForProtection", true))) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("ml");
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
      }
    }

    @Test
    public void cannotCreateBarrelItemsAfterPrayer() {
      try (var _ =
          new Cleanups(
              withProperty("barrelShrineUnlocked", true), withProperty("_barrelPrayer", true))) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("ml");
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
      }
    }

    @Test
    public void cannotCreateBarrelItemsInStandard() {
      try (var _ =
          new Cleanups(
              withProperty("barrelShrineUnlocked", true),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "shrine to the Barrel god"))) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("ml");
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
      }
    }
  }

  @Nested
  class FantasyRealm {
    @ParameterizedTest
    @ValueSource(strings = {"frAlways", "_frToday"})
    public void canCreateFantasyRealmItems(String pref) {
      try (var _ = withProperty(pref, true)) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("moxie");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "FantasyRealm Rogue's Mask")));
      }
    }

    @Test
    public void cannotCreateFantasyRealmItemsIfAlreadyUsed() {
      try (var _ = new Cleanups(withProperty("frAlways", true), withProperty("_frHoursLeft", 5))) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("moxie");
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
      }
    }

    @Test
    public void cannotCreateFantasyRealmItemsInStandard() {
      try (var _ =
          new Cleanups(
              withProperty("frAlways", true),
              withRestricted(true),
              withNotAllowedInStandard(
                  RestrictedItemType.ITEMS, "FantasyRealm membership packet"))) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("moxie");
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
      }
    }
  }

  @Nested
  class Floundry {
    @Test
    public void canCreateFloundryItems() {
      try (var _ =
          new Cleanups(
              withClanLoungeItem(ItemPool.CLAN_FLOUNDRY), withClanLoungeItem(ItemPool.CARPE))) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("meat");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.CONTAINER, "carpe")));
      }
    }

    @Test
    public void cannotCreateFloundryItemsInStandard() {
      try (var _ =
          new Cleanups(
              withClanLoungeItem(ItemPool.CLAN_FLOUNDRY),
              withClanLoungeItem(ItemPool.CARPE),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Clan Floundry"))) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("meat");
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.CONTAINER))));
      }
    }
  }

  @Nested
  class NPCStore {
    @Test
    public void canBuyUtensil() {
      try (var _ =
          new Cleanups(
              withProperty("autoSatisfyWithNPCs", true),
              withProperty("autoBuyPriceLimit", 2_000),
              withMeat(2000))) {
        maximizeCreatable("spell dmg");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "rubber spatula")));
      }
    }

    @Test
    public void buyBestUtensil() {
      try (var _ =
          new Cleanups(
              withProperty("autoSatisfyWithNPCs", true),
              withProperty("autoBuyPriceLimit", 2_000),
              withMeat(2000),
              withStats(100, 100, 100))) {
        maximizeCreatable("spell dmg");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "obsidian nutcracker")));
      }
    }

    @Test
    public void canOnlyBuyOneSphygmayomanometer() {
      try (var _ =
          new Cleanups(
              withProperty("autoSatisfyWithNPCs", true),
              withWorkshedItem(ItemPool.MAYO_CLINIC),
              withStats(100, 100, 100))) {
        maximizeCreatable("muscle");
        assertThat(getBoosts(), hasItem(recommends("sphygmayomanometer")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.ACCESSORY2))));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.ACCESSORY3))));
      }
    }

    @Test
    public void canOnlyBuyOneOversizedSparkler() {
      try (var _ =
          new Cleanups(
              withProperty("autoSatisfyWithNPCs", true),
              withSkill("Double-Fisted Skull Smashing"),
              withProperty("_fireworksShop", true))) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("item drop");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "oversized sparkler")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
      }
    }

    @Test
    public void cannotCreateFireworkHatIfAlreadyHave() {
      try (var _ =
          new Cleanups(
              withProperty("autoSatisfyWithNPCs", true),
              withProperty("_fireworksShop", true),
              withProperty("_fireworksShopHatBought", true))) {
        ConcoctionDatabase.refreshConcoctions();
        maximizeCreatable("-combat");
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
      }
    }
  }

  @Nested
  class Foldables {
    @Test
    public void willFoldIfBetter() {
      try (var _ = withEquippableItem(ItemPool.TURTLE_WAX_GREAVES)) {
        maximizeCreatable("hot res");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "turtle wax helmet")));
      }
    }

    @Test
    public void willNotFoldIfPreferenceFalse() {
      try (var _ =
          new Cleanups(
              withProperty("maximizerFoldables", false),
              withEquippableItem(ItemPool.TURTLE_WAX_GREAVES))) {
        maximizeCreatable("hot res");
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
      }
    }

    @Test
    public void canFoldFromGarbageTote() {
      try (var _ = new Cleanups(withItem(ItemPool.GARBAGE_TOTE))) {
        maximizeCreatable("ml");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "tinsel tights")));
      }
    }

    @Test
    public void canFoldFromReplicaGarbageTote() {
      try (var _ =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING), withItem(ItemPool.REPLICA_GARBAGE_TOTE))) {
        maximizeCreatable("ml");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "tinsel tights")));
      }
    }

    @Test
    public void willFoldForStinkyCheese() {
      try (var _ = withItem(ItemPool.STINKY_CHEESE_STAFF)) {
        maximizeCreatable("stinky cheese");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "stinky cheese sword")));
      }
    }
  }
}
