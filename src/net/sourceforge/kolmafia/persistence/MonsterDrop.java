package net.sourceforge.kolmafia.persistence;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;

public interface MonsterDrop {
  Pattern DROP = Pattern.compile("(.+) \\(([pncfam])?([0-9.]+)\\)");

  enum DropFlag {
    NONE(""),
    UNKNOWN_RATE("0"),
    PICKPOCKET_ONLY("p"),
    NO_PICKPOCKET("n"),
    CONDITIONAL("c"),
    FIXED("f"),
    STEAL_ACCORDION("a"),
    MULTI_DROP("m");

    final String id;
    private static final DropFlag[] VALUES = values();
    private static final Map<String, DropFlag> flagMap =
        Arrays.stream(VALUES).collect(Collectors.toMap(type -> type.id, Function.identity()));

    DropFlag(String id) {
      this.id = id;
    }

    static DropFlag fromFlag(String id) {
      return flagMap.getOrDefault(id, DropFlag.NONE);
    }

    @Override
    public String toString() {
      return this.id;
    }
  }

  AdventureResult item();

  String itemCount();

  double chance();

  DropFlag flag();

  record SimpleMonsterDrop(AdventureResult item, double chance, DropFlag flag)
      implements MonsterDrop {

    @Override
    public String itemCount() {
      return "";
    }
  }

  record MultiDrop(String itemCount, AdventureResult item, double chance, DropFlag flag)
      implements MonsterDrop {
    public static Pattern ITEM = Pattern.compile("(\\d+(?:-\\d+)?) (.+)");
  }
}
