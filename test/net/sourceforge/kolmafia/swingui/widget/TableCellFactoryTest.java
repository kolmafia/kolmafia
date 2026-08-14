package net.sourceforge.kolmafia.swingui.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import org.junit.jupiter.api.Test;

class TableCellFactoryTest {
  private String getType(final String itemName) {
    int itemId = ItemDatabase.getItemId(itemName);
    AdventureResult item = ItemPool.get(itemId, 1);
    var model = new SortedListModel<AdventureResult>();

    return (String) TableCellFactory.get(7, model, item, new boolean[] {false, false}, false, true);
  }

  @Test
  void displaysUsableTypes() {
    assertThat(getType("11-leaf clover"), is("usable"));
    assertThat(getType("all-year sucker"), is("usable"));
    assertThat(getType("milk of magnesium"), is("usable"));
    assertThat(getType("ebony wand"), is("usable"));
    assertThat(getType("cloaca-cola"), is("usable"));
    assertThat(getType("metandienone"), is("usable"));
    assertThat(getType("disintegrating sheet music"), is("usable"));
    assertThat(getType("towel"), is("usable"));
    assertThat(getType("Loathing Legion jackhammer"), is("usable"));
    assertThat(getType("mountain lion skin"), is("usable"));
    assertThat(getType("quicksilver spurs"), is("usable"));
    assertThat(getType("Scratch 'n' sniff unicorn sticker"), is("usable"));
    assertThat(getType("Folder (blue)"), is("usable"));
    assertThat(getType("seal tooth"), is("usable"));
  }

  @Test
  void displaysCombatTypes() {
    assertThat(getType("4:20 bomb"), is("combat item"));
    assertThat(getType("baconstone-handled sixgun"), is("combat item"));
    assertThat(getType("33398 scroll"), is("combat item"));
  }

  @Test
  void displaysSpecializedTypes() {
    assertThat(getType("agua de vida"), is("spleen item"));
    assertThat(getType("abstraction: action"), is("spleen item"));
    assertThat(getType("alien autoautopsy kit"), is("avatar potion"));
    assertThat(getType("abominable blubber"), is("potion"));
  }

  @Test
  void displaysEquipmentTypes() {
    assertThat(getType("makeshift cape"), is("back item"));
    assertThat(getType("shock collar"), is("familiar equipment"));
    assertThat(getType("halibut"), is("weapon"));
    assertThat(getType("3-ball"), is("off-hand item"));
  }

  @Test
  void displaysMiscTypes() {
    assertThat(getType("squashed frog"), is("miscellaneous"));
    assertThat(getType("El Vibrato power sphere"), is("miscellaneous"));
    assertThat(getType("Alice's Army Guard"), is("miscellaneous"));
  }

  @Test
  void displaysQuestTypes() {
    assertThat(getType("Dolphin King's map"), is("quest item"));
  }
}
