package net.sourceforge.kolmafia.persistence;

import java.util.Collections;
import java.util.List;
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

  public static List<String> shrunkenHeadZombie(int monsterId, int pathId) {
    var seed = monsterId * 12345 + pathId * 99;
    var mtRand = new PHPMTRandom(seed);
    var rand = new PHPRandom(seed);
    var count = mtRand.nextInt(1, 2) + mtRand.nextInt(1, 2);

    var lst = IntStream.range(0, 10).boxed().collect(Collectors.toList());
    rand.shuffle(lst);
    var chosen = lst.subList(0, count);
    Collections.sort(chosen);

    return chosen.stream().map(abilities::get).collect(Collectors.toList());
  }
}
