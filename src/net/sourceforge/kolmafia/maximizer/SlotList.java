package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.session.EquipmentManager;

/**
 * A list of lists of T. Indexed by:
 *
 * <ul>
 *   <li>EquipmentManager slots
 *       <ul>
 *         <li>except that the slots acc2, acc3, sticker2, sticker3 are instead the Evaluator reused
 *             slots: OFFHAND_MELEE, OFFHAND_RANGED, WATCHES, WEAPON_1H
 *       </ul>
 *   <li>extra familiar equipment being considered. One per familiar.
 * </ul>
 *
 * @param <T> Interior list content
 */
public class SlotList<T> {
  private final List<List<T>> slotList;
  private final List<List<T>> familiarList;

  public SlotList(int famSize) {
    slotList = new ArrayList<>(EquipmentManager.ALL_SLOTS);
    for (int i = 0; i < EquipmentManager.ALL_SLOTS; ++i) {
      slotList.add(new ArrayList<>());
    }
    familiarList = new ArrayList<>(famSize);
    for (int i = 0; i < famSize; ++i) {
      familiarList.add(new ArrayList<>());
    }
  }

  public List<T> get(int key) {
    if (key >= EquipmentManager.ALL_SLOTS) {
      return familiarList.get(key - EquipmentManager.ALL_SLOTS);
    }
    return slotList.get(key);
  }

  public List<T> getFamiliar(int key) {
    return familiarList.get(key);
  }

  public void set(int key, List<T> val) {
    if (key >= EquipmentManager.ALL_SLOTS) {
      familiarList.set(key - EquipmentManager.ALL_SLOTS, val);
    }
    slotList.set(key, val);
  }

  public int size() {
    return slotList.size() + familiarList.size();
  }
}
