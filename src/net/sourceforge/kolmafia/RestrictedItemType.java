package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.List;

public enum RestrictedItemType {
  ITEMS("Items"),
  CAMPGROUND("Campground"),
  BOOKSHELF_BOOKS("Bookshelf", "Bookshelf Books"),
  SKILLS("Skills"),
  FAMILIARS("Familiars"),
  CLAN_ITEMS("Clan Item", "Clan Items"),
  MISCELLANEOUS("Miscellaneous");

  private final List<String> types;

  RestrictedItemType(String... types) {
    this.types = Arrays.asList(types);
  }

  public List<String> getTypes() {
    return types;
  }

  public static RestrictedItemType fromString(final String type) {
    return Arrays.stream(values())
        .filter(itemType -> itemType.getTypes().stream().anyMatch(t -> t.equals(type)))
        .findFirst()
        .orElse(null);
  }
}
