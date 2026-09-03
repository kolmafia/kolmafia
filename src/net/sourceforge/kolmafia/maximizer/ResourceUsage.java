package net.sourceforge.kolmafia.maximizer;

import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
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
      List<SharedResource> resources, Map<Slot, AdventureResult> equipment) {
    if (resources.isEmpty()) return EMPTY;
    int[] costs = new int[resources.size()];
    for (int i = 0; i < costs.length; i++) {
      costs[i] = resources.get(i).equipmentCost().applyAsInt(equipment);
    }
    return new ResourceUsage(costs);
  }

  static ResourceUsage forItem(List<SharedResource> resources, String itemName) {
    if (resources.isEmpty()) return EMPTY;
    int[] costs = new int[resources.size()];
    for (int i = 0; i < costs.length; i++) {
      costs[i] = resources.get(i).itemCost().applyAsInt(itemName);
    }
    return new ResourceUsage(costs);
  }

  boolean exceeds(List<SharedResource> resources) {
    for (int i = 0; i < this.costs.length; i++) {
      if (this.costs[i] > resources.get(i).limit()) return true;
    }
    return false;
  }

  boolean hasRemainingCapacityFor(ResourceUsage candidate, List<SharedResource> resources) {
    if (this.costs.length != candidate.costs.length || this.costs.length != resources.size()) {
      throw new IllegalArgumentException("Cannot compare different equipment resource sets");
    }
    for (int i = 0; i < this.costs.length; i++) {
      if (candidate.costs[i] != 0 && this.costs[i] >= resources.get(i).limit()) return false;
    }
    return true;
  }

  boolean isZero() {
    for (int cost : this.costs) {
      if (cost != 0) return false;
    }
    return true;
  }

  ResourceUsage plus(ResourceUsage other) {
    if (this.costs.length != other.costs.length) {
      throw new IllegalArgumentException("Cannot combine different equipment resource sets");
    }
    if (this.costs.length == 0) return EMPTY;
    int[] sum = new int[this.costs.length];
    for (int i = 0; i < sum.length; i++) {
      sum[i] = this.costs[i] + other.costs[i];
    }
    return new ResourceUsage(sum);
  }

  ResourceUsage times(int multiplier) {
    if (this.costs.length == 0) return EMPTY;
    int[] product = new int[this.costs.length];
    for (int i = 0; i < product.length; i++) {
      product[i] = this.costs[i] * multiplier;
    }
    return new ResourceUsage(product);
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

record SharedResource(
    String name,
    int limit,
    ToIntFunction<Map<Slot, AdventureResult>> equipmentCost,
    ToIntFunction<String> itemCost) {}
