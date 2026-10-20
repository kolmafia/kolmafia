package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.PHPRandom;

public class AscensionSeedManager {
  private static final int ASCENSION_SEED_MIN = 1_000_000;
  private static final int ASCENSION_SEED_MAX = 9_999_999;

  // Give up after finding this many matching seeds
  private static final int MAX_CALCULATED_SEEDS = 1000;

  // Dispatch batches of this many seed candidates to each processing thread
  private static final int BATCH_SIZE = 100_000;

  private static final List<Supplier<Criterion>> CRITERIA =
      List.of(BangPotions::new, DailyDungeon::new, Dreadscroll::new, SeahorseName::new);

  // Determine which ascension seeds are possible given the state of the character's current prefs.
  // For performance reasons, limit this to finding MAX_CALCULATED_SEEDS (to cover the case where
  // this is called with not nearly enough information available).
  public static List<Integer> possibleAscensionSeeds() {
    // Derive the criteria based on current account state. Sort it such that the criteria that are
    // most likely to reject a seed candidate are checked first.
    //
    // While it is the case that some criteria give partially- or wholly-redundant data (for example
    // if they are based entirely on the same sequence of RNG draws), attmpting to account for this
    // has proven to be too much effort for the small amount of extra processing it saves.
    List<Criterion> criteria =
        CRITERIA.stream()
            .map(Supplier::get)
            .filter(c -> c.reductionFactor() < 1)
            .sorted(Comparator.comparingDouble(Criterion::reductionFactor))
            .collect(Collectors.toCollection(ArrayList::new));
    // TODO remove this before final
    RequestLogger.printLine(
        criteria.stream()
            .map(c -> c.getClass().getSimpleName() + ": " + c.reductionFactor())
            .collect(Collectors.joining("\n")));

    final Collection<Integer> matchingSeeds = new ConcurrentLinkedQueue<>();

    // Chunk the seed candidate search space out into parcels of size BATCH_SIZE and run them in
    // parallel. The only side effect these threads should have is adding elements to matchingSeeds,
    // which is chosen to be thread-safe. Each thread will self-terminate early if
    // MAX_CALCULATED_SEEDS is reached, so we don't need to worry about explicitly killing them.
    IntStream.iterate(ASCENSION_SEED_MIN, n -> n <= ASCENSION_SEED_MAX, n -> n + BATCH_SIZE)
        .parallel()
        .forEach(
            min -> {
              int max = Math.min(min + BATCH_SIZE - 1, ASCENSION_SEED_MAX);
              seedloop:
              for (int seedCandidate = min; seedCandidate <= max; seedCandidate++) {
                // Give up early if we've already found the maximum number of matching seeds
                // allowed.
                if (matchingSeeds.size() >= MAX_CALCULATED_SEEDS) {
                  break;
                }

                // Check against each of the criteria. Because these are ordered by the statistical
                // likelihood to reject a seed candidate, most candidates get weeded out early.
                for (Criterion criterion : criteria) {
                  if (!criterion.matchesSeed(seedCandidate)) {
                    continue seedloop;
                  }
                }
                matchingSeeds.add(seedCandidate);
              }
            });

    // It's possible we overshot the limit slightly, so trim it if necessary.
    return matchingSeeds.stream().limit(MAX_CALCULATED_SEEDS).sorted().toList();
  }

  // Determine what we expect the relevant prefs to be for all of the things that are determined by
  // the ascension seed, given a specific seed.
  public static Map<String, String> getPropertiesForSeed(int seed) {
    Map<String, String> rv = new HashMap<>();
    // nb: Technically the constructors for the criteria waste a little effort parsing out the
    // current state, which is not needed for calculating the expected values for a particular seed,
    // this is a small enough amount of work that we expect to be called few enough times that it
    // isn't worth trying to work around.
    CRITERIA.stream().map(Supplier::get).map(c -> c.toProperties(seed)).forEach(rv::putAll);
    return rv;
  }

  // region: Ascension Seed criteria

  // Interface representing an aspect of the game that is determined by the ascension seed. Each
  // implementation has the knowledge of how to determine whether a particular seed is consistent
  // with the current account state as well as determine the expected state of relevant prefs for
  // any given ascension seed.
  private interface Criterion {
    // Check whether the current account state is consistent with the state expected for the
    // specified ascension seed. Implementations of this method should be side-effect-free.
    boolean matchesSeed(int seed);

    // Return the expected values of all properties that are associated with this aspect, based on
    // the specified ascension seed.
    Map<String, String> toProperties(int seed);

    // A heuristic indicating what proportion of ascension seed candidates are consistent with the
    // current value of the prefs associated with this aspect. A lower value indicates that more
    // candidate seeds are likely to be weeded out by checking this criterion.
    //
    // In any reasonable case, the return value should be in the interval (0,1].
    double reductionFactor();
  }

  // Bang Potions
  //
  // Starting with the initial order (sleepiness, confusion, inebriety, teleportitis, detection,
  // ettin strength, mental acuity, blessing, healing), perform a standard shuffle and then assign
  // the effects to potions in increasing order of item ID.
  private static class BangPotions implements Criterion {
    private final List<String> state;

    public BangPotions() {
      state = new ArrayList<>(9);
      for (int i = 0; i < 9; i++) {
        String si = Preferences.getString("lastBangPotion" + (819 + i));
        state.add(si);
      }
    }

    private List<String> generateForSeed(int seed) {
      PHPRandom rng = new PHPRandom(seed);
      List<String> arr =
          new ArrayList<>(
              List.of(
                  "sleepiness",
                  "confusion",
                  "inebriety",
                  "teleportitis",
                  "detection",
                  "ettin strength",
                  "mental acuity",
                  "blessing",
                  "healing"));
      rng.shuffle(arr);
      return arr;
    }

    @Override
    public boolean matchesSeed(int seed) {
      List<String> arr = generateForSeed(seed);
      for (int i = 0; i < 9; i++) {
        if (!state.get(i).isEmpty() && !state.get(i).equals(arr.get(i))) {
          return false;
        }
      }
      return true;
    }

    @Override
    public Map<String, String> toProperties(int seed) {
      Map<String, String> rv = new HashMap<>();
      List<String> arr = generateForSeed(seed);
      for (int i = 0; i < 9; i++) {
        rv.put("lastBangPotion" + (819 + i), arr.get(i));
      }
      return rv;
    }

    @Override
    public double reductionFactor() {
      // Reduction factor is N!/9!, where N is the number of identified bang potions.
      double rv = 1;
      int nextReduction = 9;
      for (String v : state) {
        if (v != null && !v.isEmpty()) {
          rv /= nextReduction;
          nextReduction--;
        }
      }
      return rv;
    }
  }

  // Daily Dungeon Room Order
  //
  // Starting with the initial order MMMMDDDDTTTT (where M is a monster room, D is a door room, and
  // T is a trap room), perform a standard shuffle and insert the prize rooms after every fourth
  // room.
  private static class DailyDungeon implements Criterion {
    private final String state;

    public DailyDungeon() {
      state = Preferences.getString("dailyDungeonRooms").replaceAll("[^MDT?]", "");
    }

    private String generateForSeed(int seed) {
      PHPRandom rng = new PHPRandom(seed);
      List<String> arr =
          Stream.of("MMMMDDDDTTTT".split("")).collect(Collectors.toCollection(ArrayList::new));
      rng.shuffle(arr);
      return String.join("", arr);
    }

    @Override
    public boolean matchesSeed(int seed) {
      String predicted = generateForSeed(seed);
      for (int i = 0; i < 12; i++) {
        if (state.charAt(i) != '?' && state.charAt(i) != predicted.charAt(i)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public Map<String, String> toProperties(int seed) {
      String raw = generateForSeed(seed);
      return Map.of(
          "dailyDungeonRooms",
          raw.substring(0, 4) + "_" + raw.substring(4, 8) + "_" + raw.substring(8));
    }

    @Override
    public double reductionFactor() {
      // Determine how many of each type of room are unaccounted for.
      int cntM = 4;
      int cntD = 4;
      int cntT = 4;
      for (int i = 0; i < 12; i++) {
        if (state.charAt(i) == 'M') {
          cntM--;
        } else if (state.charAt(i) == 'D') {
          cntD--;
        } else if (state.charAt(i) == 'T') {
          cntT--;
        }
      }

      // This is equal to M/N, where M=(A+B+C)!/(A!B!C!) is the number of possible arrangements of
      // so-far unassigned rooms given the current state and N=12!/(4!4!4!) is the number of
      // possible arrangements across all states.
      double rv = 1;
      for (int j = cntM + 1; j <= 4; j++) {
        rv *= j;
      }
      for (int j = cntD + 1; j <= 4; j++) {
        rv *= j;
      }
      for (int j = cntT + 1; j <= 4; j++) {
        rv *= j;
      }
      for (int j = cntM + cntD + cntT + 1; j <= 12; j++) {
        rv /= j;
      }
      return rv;
    }
  }

  // Mer-kin dreadscroll answers
  //
  // Use mt_rand(1,4) eight times to select numbers from 1 to 4.
  private static class Dreadscroll implements Criterion {
    private final int[] state;

    public Dreadscroll() {
      state = new int[8];
      for (int i = 0; i < 8; i++) {
        state[i] = Preferences.getInteger("dreadScroll" + (i + 1));
      }
    }

    private int[] generateForSeed(int seed) {
      int[] rv = new int[8];
      PHPMTRandom rng = new PHPMTRandom(seed);
      for (int i = 0; i < 8; i++) {
        rv[i] = rng.nextInt(1, 4);
      }
      return rv;
    }

    @Override
    public boolean matchesSeed(int seed) {
      int[] expected = generateForSeed(seed);
      for (int i = 0; i < 8; i++) {
        if (state[i] > 0 && state[i] != expected[i]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public Map<String, String> toProperties(int seed) {
      Map<String, String> rv = new HashMap<>();
      int[] values = generateForSeed(seed);
      for (int i = 0; i < 8; i++) {
        rv.put("dreadScroll" + (i + 1), Integer.toString(values[i]));
      }
      return rv;
    }

    @Override
    public double reductionFactor() {
      // 1/4^N, where N is the number of known-correct answers.
      double rv = 1;
      for (int v : state) {
        if (v > 0) {
          rv /= 4;
        }
      }
      return rv;
    }
  }

  // Wild seahorse name
  //
  // Use mt_rand(1,4) until a non-4 result is achieved. If the result is a 1, the name is selected
  // from SWIM_NAMES. If the result is a 2, the name is two words; the first word is selected from
  // TWOPART_NAMES_1 and the second word is selected from TWOPART_NAMES_2. If the result is a 3, the
  // name is selected from JACK_NAMES.
  private static class SeahorseName implements Criterion {
    private static final List<String> SWIM_NAMES =
        List.of(
            "Flickerswim",
            "Flitterswim",
            "Glitterswim",
            "Glimmerswim",
            "Shimmerswim",
            "Lusterswim",
            "Dazzleswim",
            "Splendorswim",
            "Fritterswim",
            "Frizzleswim",
            "Tripperswim");
    private static final List<String> TWOPART_NAMES_1 =
        List.of(
            "Morning",
            "Afternoon",
            "Evening",
            "Waterspout",
            "Dolphin",
            "Cloud",
            "Reddie",
            "Purplie",
            "Bluie",
            "Orangie",
            "Greenie",
            "Pasty",
            "Lightning",
            "Thunder",
            "Pokey",
            "Scarlet",
            "Manta",
            "Sailboat",
            "Swimmy",
            "Backstroke",
            "Butterfly",
            "Sushi",
            "Hermit",
            "Diving",
            "Swordfish",
            "Starfish",
            "Sturgeon",
            "Urchin",
            "Beluga");
    private static final List<String> TWOPART_NAMES_2 =
        List.of(
            "Splash",
            "Pie",
            "Sparkle",
            "Waves",
            "Sand",
            "Gloaming",
            "Dreams",
            "Munchies",
            "Seagrass",
            "Shipwreck",
            "Sailor",
            "Fizzy",
            "Bucket",
            "Bait",
            "Sofa",
            "Apple",
            "Urchin",
            "Star",
            "Beam",
            "Valley",
            "Blossom",
            "Scallop",
            "Coral",
            "Anemone",
            "Seaweed");
    private static final List<String> JACK_NAMES =
        List.of(
            "Bananajack",
            "Blackberryjack",
            "Blueberryjack",
            "Cantaloupejack",
            "Cherryjack",
            "Clementinejack",
            "Dragonfruitjack",
            "Durianjack",
            "Figjack",
            "Grapejack",
            "Grapefruitjack",
            "Honeydewjack",
            "Huckleberryjack",
            "Jackfruitjack",
            "Kiwijack",
            "Kumquatjack",
            "Lemonjack",
            "Limejack",
            "Mangojack",
            "Orangejack",
            "Pearjack",
            "Pineapplejack",
            "Raspberryjack",
            "Starfruitjack",
            "Strawberryjack",
            "Tangerinejack",
            "Tomatojack",
            "Watermelonjack",
            "Grapplejack",
            "Pluotjack",
            "Apricotjack",
            "Plumjack");

    private final String state;

    public SeahorseName() {
      state = Preferences.getString("seahorseName");
    }

    private String generateForSeed(int seed) {
      PHPMTRandom rng = new PHPMTRandom(seed);
      int v = 4;
      while (v == 4) {
        v = rng.nextInt(1, 4);
      }
      return switch (v) {
        case 1 -> rng.pickOne(JACK_NAMES);
        case 2 -> rng.pickOne(TWOPART_NAMES_1) + " " + rng.pickOne(TWOPART_NAMES_2);
        case 3 -> rng.pickOne(SWIM_NAMES);
        default -> "";
      };
    }

    @Override
    public boolean matchesSeed(int seed) {
      String name = generateForSeed(seed);
      return state.equals(name);
    }

    @Override
    public Map<String, String> toProperties(int seed) {
      String name = generateForSeed(seed);
      return Map.of("seahorseName", name);
    }

    @Override
    public double reductionFactor() {
      if (state.isEmpty()) {
        return 1;
      }

      // Different types of name have different predictive values, inversely-proportional to the
      // size of the list(s) from which the name is chosen.
      double rv = 1. / 3;
      if (state.endsWith("swim")) {
        rv /= SWIM_NAMES.size();
      } else if (state.endsWith("jack")) {
        rv /= JACK_NAMES.size();
      } else {
        rv /= TWOPART_NAMES_1.size();
        rv /= TWOPART_NAMES_2.size();
      }
      return rv;
    }
  }
  // endregion
}
