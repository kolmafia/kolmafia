package net.sourceforge.kolmafia.equipment;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Slot {
  NONE("none"),

  // Mutable equipment slots
  HAT("hat"),
  WEAPON("weapon"),
  HOLSTER("holster"),
  OFFHAND("off-hand", "offhand"),
  CONTAINER("back", "container"),
  SHIRT("shirt"),
  PANTS("pants"),
  ACCESSORY1("acc1"),
  ACCESSORY2("acc2"),
  ACCESSORY3("acc3"),
  FAMILIAR("familiar", "familiarequip"),

  // Pseudo-equipment slots
  CROWNOFTHRONES("crown-of-thrones", "crownofthrones"),

  STICKER1("sticker1", "st1"),
  STICKER2("sticker2", "st2"),
  STICKER3("sticker3", "st3"),

  CARDSLEEVE("card-sleeve", "cardsleeve"),

  FOLDER1("folder1"),
  FOLDER2("folder2"),
  FOLDER3("folder3"),
  FOLDER4("folder4"),
  FOLDER5("folder5"),

  BUDDYBJORN("buddy-bjorn", "buddybjorn"),

  BOOTSKIN("bootskin"),
  BOOTSPUR("bootspur"),

  FAKEHAND("fakehand");

  public final String name;
  public final String phpName;
  static final Slot[] VALUES = values();

  Slot(String name) {
    this.name = name;
    this.phpName = name;
  }

  Slot(String name, String phpName) {
    this.name = name;
    this.phpName = phpName;
  }

  private static final Map<String, Slot> nameToSlot =
      Arrays.stream(VALUES).collect(Collectors.toMap(type -> type.name, Function.identity()));

  private static final Map<String, Slot> phpNameToSlot =
      Arrays.stream(VALUES).collect(Collectors.toMap(type -> type.phpName, Function.identity()));

  public static Slot byCaselessName(String name) {
    return nameToSlot.getOrDefault(name.toLowerCase(), Slot.NONE);
  }

  public static Slot byCaselessPhpName(String name) {
    return phpNameToSlot.getOrDefault(name.toLowerCase(), Slot.NONE);
  }

  public static Slot byOrdinal(int ordinal) {
    return VALUES[ordinal];
  }
}
