package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.hasStringValue;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.util.EnumSet;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase.Attribute;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ItemDatabaseTest {
  @Nested
  class AttrsToSecondary {
    @Test
    public void noneIsBlank() {
      var none = ItemDatabase.attrsToSecondaryUsage(EnumSet.noneOf(Attribute.class));
      assertThat(none, is(emptyString()));
    }

    @Test
    public void onlyAccessIsBlank() {
      var none =
          ItemDatabase.attrsToSecondaryUsage(
              EnumSet.of(
                  Attribute.TRADEABLE, Attribute.GIFT, Attribute.QUEST, Attribute.DISCARDABLE));
      assertThat(none, is(emptyString()));
    }

    @Test
    public void oneIsString() {
      var one = ItemDatabase.attrsToSecondaryUsage(EnumSet.of(Attribute.CURSE));
      assertThat(one, is(", curse"));
    }

    @Test
    public void twoIsCommaSeparated() {
      var two =
          ItemDatabase.attrsToSecondaryUsage(EnumSet.of(Attribute.CHOCOLATE, Attribute.BOUNTY));
      assertThat(two, is(", bounty, chocolate"));
    }

    @Test
    public void ignoresItemFlags() {
      var one =
          ItemDatabase.attrsToSecondaryUsage(
              EnumSet.of(
                  Attribute.CHOCOLATE,
                  Attribute.TRADEABLE,
                  Attribute.GIFT,
                  Attribute.QUEST,
                  Attribute.DISCARDABLE));
      assertThat(one, is(", chocolate"));
    }

    @Test
    public void allIsFull() {
      var none = ItemDatabase.attrsToSecondaryUsage(EnumSet.allOf(Attribute.class));
      assertThat(
          none,
          is(
              ", combat, combat reusable, usable, multiple, reusable, message, single, solo, curse, bounty, package, candy, candy1, candy2, matchable, fancy, chocolate, paste, smith, cook, mix"));
    }
  }

  @Test
  public void itemStringIsValid() {
    var item =
        ItemDatabase.itemString(
            1959,
            "tattered scrap of paper",
            "695577512",
            "tatpaper.gif",
            ConsumptionType.NONE,
            EnumSet.of(Attribute.COMBAT, Attribute.SOLO, Attribute.PASTE),
            "t,d",
            45,
            "tattered scraps of paper");
    assertThat(
        item,
        is(
            "1959\ttattered scrap of paper\t695577512\ttatpaper.gif\tnone, combat, solo, paste\tt,d\t45\ttattered scraps of paper"));
  }

  @Test
  public void checksItemFlags() {
    assertThat(
        ItemDatabase.typeToPrimaryUsage(ItemDatabase.getConsumptionType(ItemPool.SEAL_CLUB)),
        is("weapon"));
    assertThat(ItemDatabase.isTradeable(ItemPool.SEAL_CLUB), is(true));
    assertThat(ItemDatabase.isGiftItem(ItemPool.SEAL_CLUB), is(false));
    assertThat(ItemDatabase.isPasteable(ItemPool.SEAL_CLUB), is(true));
    assertThat(ItemDatabase.isChocolateItem(ItemPool.SEAL_CLUB), is(false));
  }

  @Nested
  class EverfullDartPerks {
    @BeforeEach
    public void beforeEach() {
      KoLCharacter.reset("EverfullDartPerks");
      Preferences.reset("EverfullDartPerks");
    }

    @Test
    public void parsesNoPerks() {
      var cleanups = withProperty("everfullDartPerks", "Throw a second dart quickly");

      try (cleanups) {
        var response = html("request/test_desc_item_everfull_dart_holster_no_perks.html");
        ItemDatabase.parseDartPerks(response);
        assertThat("everfullDartPerks", hasStringValue(equalTo("")));
      }
    }

    @Test
    public void parsesPerks() {
      var cleanups = withProperty("everfullDartPerks", "");

      try (cleanups) {
        var response = html("request/test_desc_item_everfull_dart_holster_perks.html");
        ItemDatabase.parseDartPerks(response);
        assertThat(
            "everfullDartPerks",
            hasStringValue(
                equalTo(
                    "Deal 25-50% more damage,You are less impressed by bullseyes,25% Better bullseye targeting,Extra stats from stats targets,Butt awareness,Add Sleaze Damage")));
      }
    }
  }

  @Nested
  class MimicEgg {
    @BeforeEach
    public void beforeEach() {
      KoLCharacter.reset("MimicEgg");
      Preferences.reset("MimicEgg");
    }

    @ParameterizedTest
    @CsvSource({
      "full,'374:1,378:2'",
      "empty,''",
    })
    void parsesEggOptionsCorrectly(final String file, final String expected) {
      var cleanups = new Cleanups(withProperty("mimicEggMonsters", ""));
      try (cleanups) {
        var response = html("request/test_desc_item_mimic_egg_" + file + ".html");
        ItemDatabase.parseMimicEgg(response);
        assertThat("mimicEggMonsters", isSetTo(expected));
      }
    }
  }
}
