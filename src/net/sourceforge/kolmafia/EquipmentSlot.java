package net.sourceforge.kolmafia;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EquipmentSlot {
  private EquipmentSlot() {}

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
    private static final Slot[] VALUES = values();

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

  /** "Normal" slots visible on equipment page: HAT to FAMILIAR. */
  public static final EnumSet<Slot> SLOTS = EnumSet.range(Slot.HAT, Slot.FAMILIAR);
  /** Player slots visible on equipment page: HAT to ACCESSORY3. SLOTS without FAMILIAR. */
  public static final EnumSet<Slot> CORE_EQUIP_SLOTS = EnumSet.range(Slot.HAT, Slot.ACCESSORY3);

  /** All slots except NONE and FAKEHAND. HAT to BOOTSPUR. */
  public static final EnumSet<Slot> ALL_SLOTS = EnumSet.range(Slot.HAT, Slot.BOOTSPUR);

  /** All folder slots: 1 to 5. */
  public static final EnumSet<Slot> FOLDER_SLOTS =
      EnumSet.of(Slot.FOLDER1, Slot.FOLDER2, Slot.FOLDER3, Slot.FOLDER4, Slot.FOLDER5);

  /** All folder slots available in aftercore (i.e. not KoLHS): 1 to 3. */
  public static final Set<Slot> FOLDER_SLOTS_AFTERCORE =
      EnumSet.of(Slot.FOLDER1, Slot.FOLDER2, Slot.FOLDER3);

  /** All accessory slots: 1 to 3. */
  public static final EnumSet<Slot> ACCESSORY_SLOTS =
      EnumSet.of(Slot.ACCESSORY1, Slot.ACCESSORY2, Slot.ACCESSORY3);

  /** All sticker slots: 1 to 3. */
  public static final EnumSet<Slot> STICKER_SLOTS =
      EnumSet.of(Slot.STICKER1, Slot.STICKER2, Slot.STICKER3);

  /** Slot names as they should appear when prompting for a slot input to a script */
  public static final String[] NAMES =
      Arrays.stream(Slot.VALUES)
          .filter(x -> x != Slot.NONE)
          .map(x -> x.name)
          .toArray(String[]::new);
}
