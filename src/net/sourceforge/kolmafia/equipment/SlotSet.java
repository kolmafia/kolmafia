package net.sourceforge.kolmafia.equipment;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public class SlotSet {
  private SlotSet() {}

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
