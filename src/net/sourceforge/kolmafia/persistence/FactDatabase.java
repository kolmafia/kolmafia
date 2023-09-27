package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.Expression;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FactDatabase {
  private static final List<String> HEAP_ITEMS =
      List.of(
          "big rock",
          "pretty flower",
          "ice-cold sir schlitz",
          "hermit permit",
          "worthless trinket",
          "worthless gewgaw",
          "worthless knick-knack",
          "ice-cold Willer",
          "rusty metal ring",
          "rusty metal shaft",
          "meat from yesterday",
          "spring",
          "sprocket",
          "cog",
          "empty meat tank",
          "ice-cold six-pack",
          "valuable trinket",
          "barbed-wire fence",
          "ghuol egg",
          "skeleton bone",
          "skewer",
          "lihc eye",
          "uncooked chorizo",
          "ice-cold fotie",
          "batgut",
          "briefcase",
          "fat stacks of cash",
          "loose teeth",
          "bat guano",
          "rat appendix",
          "hemp string",
          "gnoll teeth",
          "ten-leaf clover",
          "dead guy's watch",
          "white lightning",
          "mullet wig",
          "tenderizing hammer",
          "linoleum sword hilt",
          "linoleum stick",
          "linoleum crossbow string",
          "asbestos sword hilt",
          "asbestos stick",
          "asbestos crossbow string",
          "chrome sword hilt",
          "chrome stick",
          "chrome crossbow string",
          "yeti fur",
          "penguin skin",
          "yak skin",
          "hippopotamus skin",
          "pirate pelvis",
          "box",
          "bloody clown pants",
          "beer lens",
          "disease",
          "flaming crutch",
          "cast",
          "leather mask",
          "mesh cap",
          "enormous belt buckle",
          "catgut",
          "pr0n legs",
          "pine-fresh air freshener",
          "razor-sharp can lid",
          "Mad Train wine",
          "dirty hobo gloves",
          "furry pants",
          "disturbing fanfic",
          "fruitcake",
          "spiked femur",
          "filthy hippy poncho",
          "broken skull",
          "foon",
          "mob penguin cellular phone",
          "lead necklace",
          "pine tar",
          "tasket",
          "urinal cake",
          "blood flower",
          "lovecat tail",
          "plastic passion fruit",
          "picture of a dead guy's girlfriend",
          "length of string",
          "googly eye",
          "stuffing",
          "felt",
          "wooden block",
          "stench powder",
          "sleaze nuggets",
          "squashed frog",
          "alphabet gum",
          "old leather wallet",
          "old coin purse",
          "gob of wet hair",
          "rock",
          "stringy sinew",
          "stick",
          "tooth",
          "filthy poultice");

  private FactDatabase() {}

  public enum FactType {
    NONE,
    EFFECT,
    ITEM,
    HEAP,
    STATS,
    HP,
    MP,
    MEAT,
    MODIFIER;

    @Override
    public String toString() {
      return this.name();
    }

    static FactType find(final String name) {
      return Arrays.stream(FactType.values())
          .filter(f -> f.name().equalsIgnoreCase(name))
          .findFirst()
          .orElse(null);
    }
  }

  public static class Fact {
    private final FactType type;
    private String value;

    Fact(FactType type, String value) {
      this.type = type;
      this.value = value;
    }

    public FactType getType() {
      return type;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return this.value;
    }

    public boolean isGummi() {
      return false;
    }

    public boolean isPinata() {
      return false;
    }

    public boolean isWish() {
      return false;
    }

    public boolean isTatter() {
      return false;
    }

    public Fact resolve(
        final AscensionClass ascensionClass,
        final Path path,
        final MonsterData monster,
        final boolean stateful) {
      return this;
    }
  }

  public static class AdventureResultFact extends Fact {
    protected List<AdventureResult> results;
    private final String condition;

    AdventureResultFact(FactType type, List<AdventureResult> results, String condition) {
      super(
          type,
          results.stream().map(AdventureResult::toString).collect(Collectors.joining(" or ")));
      this.condition = condition;
      this.results = results;
    }

    AdventureResultFact(FactType type, List<AdventureResult> results) {
      this(type, results, null);
    }

    AdventureResultFact(FactType type, AdventureResult result) {
      this(type, List.of(result));
    }

    public AdventureResult getResult() {
      return results.get(0);
    }

    @Override
    public boolean isGummi() {
      var ar = this.getResult();
      if (!ar.isStatusEffect()) return false;
      var effectId = ar.getEffectId();
      return effectId == EffectPool.GUMMIBRAIN
          || effectId == EffectPool.GUMMIHEART
          || effectId == EffectPool.GUMMISKIN;
    }

    @Override
    public boolean isPinata() {
      var ar = this.getResult();
      if (!ar.isStatusEffect()) return false;
      var effectId = ar.getEffectId();
      return effectId == EffectPool.SWEET_AND_GREEN
          || effectId == EffectPool.SWEET_AND_YELLOW
          || effectId == EffectPool.SWEET_AND_RED;
    }

    @Override
    public boolean isWish() {
      var ar = this.getResult();
      if (!ar.isItem()) return false;
      return ar.getItemId() == ItemPool.POCKET_WISH;
    }

    @Override
    public boolean isTatter() {
      var ar = this.getResult();
      if (!ar.isItem()) return false;
      return ar.getItemId() == ItemPool.SCRAP_OF_PAPER;
    }

    private boolean evaluateCondition() {
      return new Expression(condition.substring(1, condition.length() - 2), "fact").eval() > 0;
    }

    @Override
    public Fact resolve(
        final AscensionClass ascensionClass,
        final Path path,
        final MonsterData monster,
        final boolean stateful) {
      if (stateful && condition != null && !evaluateCondition()) return new Fact(FactType.NONE, "");
      var seed = calculateSeed(ascensionClass, path, monster) + 13L;
      var rng = new PHPMTRandom(seed);
      return new AdventureResultFact(
          this.getType(), results.get(rng.nextInt(0, results.size() - 1)));
    }
  }

  public static class MeatFact extends Fact {
    private final boolean baseMeat;
    private final int meat;

    MeatFact(final int meat, final boolean baseMeat) {
      super(FactType.MEAT, meat + " Meat");
      this.meat = meat;
      this.baseMeat = baseMeat;
    }

    MeatFact(final boolean baseMeat) {
      this(0, baseMeat);
    }

    public int getMeat() {
      return this.meat;
    }

    @Override
    public Fact resolve(
        final AscensionClass ascensionClass,
        final Path path,
        final MonsterData monster,
        final boolean stateful) {
      int resolvedMeat;
      if (baseMeat) {
        resolvedMeat = monster.getBaseMeat();
      } else {
        var seed = calculateSeed(ascensionClass, path, monster) + 12L;
        var rng = new PHPMTRandom(seed);
        resolvedMeat = rng.nextInt(0, 50) + 100;
      }
      return new MeatFact(resolvedMeat, baseMeat);
    }
  }

  private static class HeapFact extends AdventureResultFact {
    HeapFact() {
      super(FactType.HEAP, HEAP_ITEMS.stream().map(name -> ItemPool.get(name, 1)).toList());
    }

    @Override
    public String toString() {
      return "an item from The Heap";
    }

    @Override
    public Fact resolve(
        final AscensionClass ascensionClass,
        final Path path,
        final MonsterData monster,
        final boolean stateful) {
      var seed = calculateSeed(ascensionClass, path, monster) + 11L;
      var rng = new PHPMTRandom(seed);
      return new AdventureResultFact(
          FactType.ITEM, results.get(rng.nextInt(0, results.size() - 1)));
    }
  }

  private static class StatsFact extends Fact {
    private final int muscle;
    private final int mysticality;
    private final int moxie;

    static String toString(int muscle, int mysticality, int moxie) {
      if (muscle == mysticality && mysticality == moxie) {
        return "+" + muscle + " all stats";
      }

      if (muscle > 0) {
        return "+" + muscle + " muscle";
      }

      if (mysticality > 0) {
        return "+" + mysticality + " mysticality";
      }

      return "+" + moxie + " moxie";
    }

    StatsFact(int muscle, int mysticality, int moxie) {
      super(FactType.STATS, StatsFact.toString(muscle, mysticality, moxie));
      this.muscle = muscle;
      this.mysticality = mysticality;
      this.moxie = moxie;
    }

    public int getMuscle() {
      return this.muscle;
    }

    public int getMysticality() {
      return this.mysticality;
    }

    public int getMoxie() {
      return this.moxie;
    }
  }

  private static final EnumMap<Phylum, List<Fact>> facts = new EnumMap<>(Phylum.class);

  static {
    FactDatabase.reset();
  }

  protected static void reset() {
    facts.clear();

    boolean error = false;
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("bookoffacts.txt", KoLConstants.BOOKOFFACTS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length >= 2) {
          var phylum = Phylum.find(data[0]);

          var type = FactType.find(data[1]);

          if (type == null) {
            RequestLogger.printLine("Invalid fact type: " + data[1]);
            error = true;
            continue;
          }

          var fact = parseFactData(type, data);
          if (fact == null) {
            error = true;
            continue;
          }

          FactDatabase.addToDatabase(phylum, fact);
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
      error = true;
    }

    if (error) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Error loading fact database.");
    }
  }

  private static Fact parseFactData(FactType type, String[] data) {
    return switch (type) {
      case NONE -> null;
      case EFFECT, ITEM -> {
        var noValuesError = "Fact for " + data[0] + " stats must specify at least one " + type;
        if (data.length < 3) {
          RequestLogger.printLine(noValuesError);
          yield null;
        }

        String condition = null;
        var thirdPart = data[2];
        if (thirdPart.startsWith("[") && thirdPart.endsWith("]")) {
          // Has a condition
          condition = thirdPart;
          data[2] = null;
          if (data.length < 4) {
            RequestLogger.printLine(noValuesError);
            yield null;
          }
        }

        var results =
            Arrays.stream(data)
                .skip(2)
                .filter(Objects::nonNull)
                .map(
                    type == FactType.ITEM
                        ? AdventureResult::parseItemString
                        : AdventureResult::parseEffectString)
                .toList();

        if (results.contains(null)) {
          RequestLogger.printLine("Fact for " + data[0] + " specifies an invalid " + type);
          yield null;
        }

        yield new AdventureResultFact(type, results, condition);
      }
      case MEAT -> new MeatFact(data.length >= 3 && data[2].equals("Base"));
      case HEAP -> new HeapFact();
      case HP, MP, MODIFIER -> {
        if (data.length < 3) {
          RequestLogger.printLine("Fact for " + data[0] + " must specify a value for " + type);
          yield null;
        }
        yield new Fact(type, data[2]);
      }
      case STATS -> {
        if (data.length < 5) {
          RequestLogger.printLine(
              "Fact for " + data[0] + " stats does not have muscle, mysticality, and moxie");
          yield null;
        }
        var stats =
            Arrays.stream(data)
                .skip(2)
                .map(d -> StringUtilities.isNumeric(d) ? StringUtilities.parseInt(d) : null)
                .toList();

        if (stats.contains(null)) {
          RequestLogger.printLine("Fact for " + data[0] + " has a bad stats value");
          yield null;
        }

        yield new StatsFact(stats.get(0), stats.get(1), stats.get(2));
      }
    };
  }

  private static void addToDatabase(Phylum phylum, Fact fact) {
    facts.computeIfAbsent(phylum, p -> new ArrayList<>()).add(fact);
  }

  private static boolean isPhylumEffect(final Path path, final Phylum phylum, final int seed) {
    var mod = (path == Path.SMALL && phylum == Phylum.BUG) ? 2 : 3;
    return seed % mod == 1;
  }

  public static int calculateSeed(
      final AscensionClass ascensionClass, final Path path, final MonsterData monster) {
    var classId = ascensionClass == null ? 0 : ascensionClass.getId();
    return (421 * classId) + (11 * path.getId()) + monster.getId();
  }

  public static Fact getFact(
      final AscensionClass ascensionClass,
      final Path path,
      final MonsterData monster,
      final boolean stateful) {
    var seed = calculateSeed(ascensionClass, path, monster);
    var rng = new PHPMTRandom(seed);

    var phylum = monster.getPhylum();
    var effectivePhylum = isPhylumEffect(path, phylum, seed) ? phylum : Phylum.NONE;
    var factPool = facts.get(effectivePhylum);

    var fact = factPool.get(rng.nextInt(0, factPool.size() - 1));
    return fact.resolve(ascensionClass, path, monster, stateful);
  }

  public static Fact getFact(final MonsterData monster, final boolean stateful) {
    return getFact(KoLCharacter.getAscensionClass(), KoLCharacter.getPath(), monster, stateful);
  }

  public static Fact getFact(final MonsterData monster) {
    return getFact(monster, true);
  }
}
