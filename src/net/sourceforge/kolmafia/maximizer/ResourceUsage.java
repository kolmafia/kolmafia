package net.sourceforge.kolmafia.maximizer;

import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.equipment.Slot;

final class ResourceUsage implements Comparable<ResourceUsage> {
  static final ResourceUsage EMPTY = new ResourceUsage(new int[0]);

  private final int[] costs;

  private ResourceUsage(int[] costs) {
    this.costs = costs;
  }

  static ResourceUsage of(int... costs) {
    return costs.length == 0 ? EMPTY : new ResourceUsage(costs.clone());
  }

  static ResourceUsage forEquipment(
      List<EquipmentResource> resources, Map<Slot, AdventureResult> equipment) {
    if (resources.isEmpty()) return EMPTY;
    int[] costs = new int[resources.size()];
    for (int i = 0; i < costs.length; i++) {
      costs[i] = resources.get(i).equipmentCost().applyAsInt(equipment);
    }
    return new ResourceUsage(costs);
  }

  static ResourceUsage forItem(List<EquipmentResource> resources, String itemName) {
    if (resources.isEmpty()) return EMPTY;
    int[] costs = new int[resources.size()];
    for (int i = 0; i < costs.length; i++) {
      costs[i] = resources.get(i).itemCost().applyAsInt(itemName);
    }
    return new ResourceUsage(costs);
  }

  boolean exceeds(List<EquipmentResource> resources) {
    for (int i = 0; i < this.costs.length; i++) {
      if (this.costs[i] > resources.get(i).limit()) return true;
    }
    return false;
  }

  int cost(int index) {
    return this.costs[index];
  }

  @Override
  public int compareTo(ResourceUsage other) {
    int length = Math.min(this.costs.length, other.costs.length);
    for (int i = 0; i < length; i++) {
      int comparison = Integer.compare(other.costs[i], this.costs[i]);
      if (comparison != 0) return comparison;
    }
    return Integer.compare(other.costs.length, this.costs.length);
  }
}
