package net.sourceforge.kolmafia.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.PHPRandom;

public class ShrunkenHeadDatabase {
  private ShrunkenHeadDatabase() {}

  private static final List<String> abilities =
      List.of(
          "Item Drop Bonus",
          "Meat Drop Bonus",
          "Physical Attack",
          "Hot Attack",
          "Cold Attack",
          "Sleaze Attack",
          "Stench Attack",
          "Spooky Attack",
          "MP Regen",
          "HP Regen");

  private static List<Integer> selectZombieMods(PHPMTRandom mtRand, PHPRandom rand) {
    var count = mtRand.nextInt(1, 2) + mtRand.nextInt(1, 2);
    var lst = IntStream.range(0, 10).boxed().collect(Collectors.toList());
    rand.shuffle(lst);
    return lst.subList(0, count);
  }

  public static List<String> shrunkenHeadZombie(int monsterId, int pathId) {
    var seed = monsterId * 12345 + pathId * 99;
    var mtRand = new PHPMTRandom(seed);
    var rand = new PHPRandom(seed);
    List<Integer> chosen = selectZombieMods(mtRand, rand);

    Collections.sort(chosen);
    return chosen.stream().map(abilities::get).toList();
  }

  public static Map<String, Integer> shrunkenHeadZombieWithWeights(int monsterId, int pathId) {
    var seed = monsterId * 12345 + pathId * 99;
    var mtRand = new PHPMTRandom(seed);
    var rand = new PHPRandom(seed);
    List<Integer> chosen = selectZombieMods(mtRand, rand);

    List<Integer> weightList = new ArrayList<>(100 * chosen.size());
    for (int c : chosen) {
      for (int i = 0; i < 100; i++) {
        weightList.add(c);
      }
    }
    rand.shuffle(weightList);
    weightList = weightList.subList(0, 100);
    Collections.sort(weightList);

    return weightList.stream()
        .map(abilities::get)
        .collect(Collectors.toMap(Function.identity(), s -> 1, Integer::sum));
  }
}
