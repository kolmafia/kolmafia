package net.sourceforge.kolmafia.persistence;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;

public record MonsterDrop(AdventureResult item, double chance, DropFlag flag) {

  static final Pattern DROP = Pattern.compile("(.+) \\(([pncfa])?([0-9.]+)\\)");

  public enum DropFlag {
    NONE(""),
    UNKNOWN_RATE("0"),
    PICKPOCKET_ONLY("p"),
    NO_PICKPOCKET("n"),
    CONDITIONAL("c"),
    FIXED("f"),
    STEAL_ACCORDION("a");

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
}
