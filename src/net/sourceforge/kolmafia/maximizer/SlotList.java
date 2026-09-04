package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.equipment.Slot;

/**
 * A list of lists of T. Indexed by:
 *
 * <ul>
 *   <li>equipment slots
 *       <ul>
 *         <li>accessory 2, accessory 3, and sticker 3 are also reused as the evaluator's virtual
 *             OFFHAND_MELEE, OFFHAND_RANGED, and WEAPON_1H ranking buckets
 *       </ul>
 *   <li>extra familiar equipment being considered. One per familiar.
 * </ul>
 *
 * @param <T> Interior list content
 */
public class SlotList<T> {
  private final Map<Slot, List<T>> slotList;
  private final List<List<T>> familiarList;

  public SlotList(int famSize) {
    slotList = new EnumMap<>(Slot.class);
    familiarList = new ArrayList<>(famSize);
    for (int i = 0; i < famSize; ++i) {
      familiarList.add(new ArrayList<>());
    }
  }

  public List<T> get(Slot key) {
    return slotList.computeIfAbsent(key, k -> new ArrayList<>());
  }

  public List<T> getFamiliar(int key) {
    return familiarList.get(key);
  }

  public <S> List<T> get(Entry<S> entry) {
    if (entry.isSlot()) {
      return this.get(entry.slot());
    } else {
      return this.getFamiliar(entry.famIndex());
    }
  }

  public void set(Slot key, List<T> val) {
    slotList.put(key, val);
  }

  public int size() {
    return slotList.size() + familiarList.size();
  }

  public record Entry<T>(List<T> value, Slot slot, int famIndex, boolean isSlot) {}

  public List<Entry<T>> entries() {
    var entries = new ArrayList<Entry<T>>();
    for (var slot : slotList.entrySet()) {
      entries.add(new Entry<>(slot.getValue(), slot.getKey(), -1, true));
    }
    for (int i = 0; i < familiarList.size(); i++) {
      entries.add(new Entry<>(familiarList.get(i), Slot.NONE, i, false));
    }
    return entries;
  }
}
