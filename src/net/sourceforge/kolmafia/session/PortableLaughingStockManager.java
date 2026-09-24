package net.sourceforge.kolmafia.session;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;

public class PortableLaughingStockManager {
  static final List<AdventureResult> BASIC_FRUIT =
      Stream.of(
              "orange",
              "grapefruit",
              "grapes",
              "lemon",
              "lime",
              "papaya",
              "cranberries",
              "strawberry",
              "cherry",
              "kumquat",
              "tangerine",
              "raspberry",
              "kiwi",
              "blackberry",
              "banana",
              "cactus fruit",
              "plum",
              "pear",
              "peach")
          .map(ItemPool::get)
          .toList();
  static final List<AdventureResult> ADVANCED_FRUIT =
      Stream.of("classic banana", "antique watermelon", "quince").map(ItemPool::get).toList();

  private PortableLaughingStockManager() {}

  public static Map<Integer, AdventureResult> getLaughingStockDrops(
      AscensionClass clazz, AscensionPath.Path path, int day, int minFights, int maxFights) {
    if (day < 1 || day > 4) {
      return Collections.emptyMap();
    }

    int classId = clazz.getId();
    int pathId = path.getId();
    Map<Integer, AdventureResult> rv = new TreeMap<>();
    if (minFights <= 56) {
      Map<Integer, AdventureResult> fixedDrops = getFixedDrops(classId, pathId, day);
      fixedDrops.entrySet().stream()
          .filter(e -> e.getKey() >= minFights && e.getKey() <= maxFights)
          .forEach(e -> rv.put(e.getKey(), e.getValue()));
    }
    for (int fight = 57; fight <= maxFights; fight++) {
      AdventureResult drop = calculateDrop(classId, pathId, day, fight);
      if (drop != null) {
        rv.put(fight, drop);
      }
    }
    return rv;
  }

  private static final int[] FIXED_DROPS = {1, 2, 4, 7, 11, 16, 22, 29, 37, 46, 56};

  // The first three advanced fruit of each day have an increased drop rate. Even if not the whole
  // range is needed, calculate these all at once to correctly handle the pity mechanic.
  private static Map<Integer, AdventureResult> getFixedDrops(int classId, int pathId, int day) {
    Map<Integer, AdventureResult> rv = new TreeMap<>();

    int pityCount = 0;
    int pityThreshold = 10;
    for (int fight : FIXED_DROPS) {
      AdventureResult drop;
      if (pityCount < 3) {
        drop = calculateDrop(classId, pathId, day, fight, true, pityThreshold);
        if (ADVANCED_FRUIT.contains(drop)) {
          pityCount++;
          pityThreshold = 10;
        } else {
          pityThreshold += 10;
        }
      } else {
        drop = calculateForcedDrop(classId, pathId, day, fight);
      }
      rv.put(fight, drop);
    }
    return rv;
  }

  private static AdventureResult calculateDrop(int classId, int pathId, int day, int fight) {
    return calculateDrop(classId, pathId, day, fight, false, 3);
  }

  private static AdventureResult calculateForcedDrop(int classId, int pathId, int day, int fight) {
    return calculateDrop(classId, pathId, day, fight, true, 3);
  }

  private static AdventureResult calculateDrop(
      int classId, int pathId, int day, int fight, boolean forced, int threshold) {
    int seed = classId * classId * classId + 84 * pathId + 123 * (day - 1) + 381 * fight;
    PHPMTRandom rng = new PHPMTRandom(seed);
    if (!forced) {
      int dropCheck = rng.nextInt(1, 50);
      if (dropCheck > 1) {
        return null;
      }
    }

    int advCheck = rng.nextInt(1, 30);
    List<AdventureResult> fruitPool = (advCheck <= threshold) ? ADVANCED_FRUIT : BASIC_FRUIT;
    return rng.pickOne(fruitPool);
  }
}
