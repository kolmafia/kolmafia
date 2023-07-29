package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import java.util.EnumSet;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase.Attribute;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
              ", combat, combat reusable, usable, multiple, reusable, message, single, solo, curse, bounty, candy, candy1, candy2, matchable, fancy, chocolate, paste, smith, cook, mix"));
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
}
