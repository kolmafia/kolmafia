package net.sourceforge.kolmafia;

public enum RestrictedItemType {
  ITEMS,
  CAMPGROUND,
  BOOKSHELF_BOOKS,
  SKILLS,
  FAMILIARS,
  CLAN_ITEMS,
  MISCELLANEOUS;

  public static RestrictedItemType fromString(final String type) {
    return switch (type) {
      case "Items" -> RestrictedItemType.ITEMS;
      case "Campground" -> RestrictedItemType.CAMPGROUND;
      case "Bookshelf", "Bookshelf Books" -> RestrictedItemType.BOOKSHELF_BOOKS;
      case "Skills" -> RestrictedItemType.SKILLS;
      case "Familiars" -> RestrictedItemType.FAMILIARS;
      case "Clan Item", "Clan Items" -> RestrictedItemType.CLAN_ITEMS;
      case "Miscellaneous" -> RestrictedItemType.MISCELLANEOUS;
      default -> null;
    };
  }
}
