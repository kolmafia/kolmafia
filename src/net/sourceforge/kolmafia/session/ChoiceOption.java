package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class ChoiceOption {

  private static final String[] NO_ITEM_NAMES = new String[0];

  private final String name;
  private final int option;
  private final AdventureResult[] items;

  public ChoiceOption(final String name) {
    this(name, 0, NO_ITEM_NAMES);
  }

  public ChoiceOption(final String name, final String... itemNames) {
    this(name, 0, itemNames);
  }

  public ChoiceOption(final String name, final int option) {
    this(name, option, NO_ITEM_NAMES);
  }

  public ChoiceOption(final String name, final int option, final String... itemNames) {
    this.name = name;
    this.option = option;

    int count = itemNames.length;
    this.items = new AdventureResult[count];

    for (int index = 0; index < count; ++index) {
      this.items[index] = ItemPool.get(ItemDatabase.getItemId(itemNames[index]));
    }
  }

  public String getName() {
    return this.name;
  }

  public int getOption() {
    return this.option;
  }

  public int getDecision(final int def) {
    return this.option == 0 ? def : this.option;
  }

  public AdventureResult[] getItems() {
    return this.items;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
