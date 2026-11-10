package net.sourceforge.kolmafia.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.persistence.PocketDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest;
import net.sourceforge.kolmafia.session.AscensionSeedManager.Criterion.CompareResult;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.PHPRandom;

public class AscensionSeedManager {
  private static final int ASCENSION_SEED_MIN = 1_000_000;
  private static final int ASCENSION_SEED_MAX = 9_999_999;

  // Give up after finding this many matching seeds
  public static final int MAX_CALCULATED_SEEDS = 1000;

  // Dispatch batches of this many seed candidates to each processing thread
  private static final int BATCH_SIZE = 100_000;

  public static AscensionSeedManager INSTANCE;

  public static synchronized void initialize() {
    if (INSTANCE == null) {
      INSTANCE = new AscensionSeedManager();
    }
  }

  private static final Map<String, Supplier<Criterion>> CRITERIA = new HashMap<>();

  static {
    CRITERIA.put("BP", BangPotions::new);
    CRITERIA.put("CC", CardCatalog::new);
    CRITERIA.put("DD", DailyDungeon::new);
    CRITERIA.put("DS", Dreadscroll::new);
    CRITERIA.put("LC", LeprecondoNeeds::new);
    CRITERIA.put("RC", RaveCombos::new);
    CRITERIA.put("SH", SeahorseName::new);
    CRITERIA.put("SP", SlimePotions::new);
    CRITERIA.put("VF", VioletFog::new);
    CRITERIA.put("YD", YegDemon::new);
  }

  private final Map<String, Listener> LISTENERS = new HashMap<>();

  // Listener for updates to ascension seed-related preferences. Each Criterion class reports the
  // preferences that should be watched, and when ascension seed automation is enabled and one of
  // those preferences is updated, only the associated Criterion needs to be checked for
  // newly-narrowed (or widened) data.
  private class AscensionSeedPreferenceListener implements Listener {
    private final String code;

    private AscensionSeedPreferenceListener(final String code) {
      this.code = code;
    }

    public void update() {
      if (Preferences.getBoolean("ascensionSeedAutomation")) {
        if (code.equals("Manager")) {
          // When ascension seed automation is first enabled, check whether it makes sense to start
          // narrowing the candidates immediately.
          incrementalReset();
          return;
        }

        String prevState = getState(code);
        Criterion c = CRITERIA.get(code).get();
        // Set up the Criterion based on the current preferences and determine whether the relevant
        // state has actually changed.
        CompareResult res = c.compareState(prevState);
        setState(code, res.newState());
        // If the state is narrower, use the Criterion to update the list of candidates. If it's
        // wider, start over since we don't have a way of knowing which candidates need to be
        // re-instated.
        switch (res.type()) {
          case NARROWER -> incrementalUpdate(c);
          case WIDER -> incrementalReset();
        }
      } else {
        // If any of the ascension seed-related preferences is updated while ascension seed
        // automation is disabled, there is no way to know whether it's a narrow or widen, so clear
        // the state preferences and start from scratch if ascension seed automation is ever
        // re-enabled.
        Preferences.resetToDefault(
            "ascensionSeed", "ascensionSeedAutomationState", "ascensionSeedCandidates");
      }
    }
  }

  private AscensionSeedManager() {
    // Set up the listeners for each Criterion's preferences
    CRITERIA.forEach(
        (code, supp) ->
            PreferenceListenerRegistry.registerPreferenceListener(
                supp.get().getStatePrefs(),
                LISTENERS.computeIfAbsent(code, AscensionSeedPreferenceListener::new)));

    // Set up a listener to track when ascension seed automation is enabled or disabled
    LISTENERS.put("Manager", new AscensionSeedPreferenceListener("Manager"));
    PreferenceListenerRegistry.registerPreferenceListener(
        "ascensionSeedAutomation", LISTENERS.get("Manager"));
  }

  private String getState(String code) {
    Map<String, String> states = Preferences.getStringMap("ascensionSeedAutomationState");
    return states.getOrDefault(code, "");
  }

  private void setState(String code, String state) {
    // If we get a request to update state when ascension seed automation is disabled, just reset it
    // since we're not tracking candidates anyway.
    if (!Preferences.getBoolean("ascensionSeedAutomation")) {
      Preferences.resetToDefault("ascensionSeedAutomationState");
      return;
    }

    Map<String, String> states = Preferences.getStringMap("ascensionSeedAutomationState");

    // Add or update the specified state, or remove it if the state string is blank (i.e. the
    // default state)
    if (state.isBlank()) {
      states.remove(code);
    } else {
      states.put(code, state);
    }
    Preferences.setStringMap("ascensionSeedAutomationState", states);
  }

  // Get the current candidate seeds. If the candidates have not been narrowed down yet, return
  // null. If the candidates have been narrowed down to 0 seeds (which indicates that the
  // preferences are not consistent with how we expect things to work), return an empty list.
  private List<Integer> getCandidateSeeds() {
    String candidateString = Preferences.getString("ascensionSeedCandidates");
    if (candidateString.isBlank()) {
      return null;
    } else if (candidateString.equals("none")) {
      return Collections.emptyList();
    }
    return Arrays.stream(candidateString.split(",")).map(Integer::parseInt).toList();
  }

  // Set the current candidate seeds. A null input indicates that the candidates have not been
  // narrowed down yet. If the list contains exactly 1 element, also set the ascension seed
  // preference.
  private void setCandidateSeeds(List<Integer> candidateSeeds) {
    int ascensionSeed = 0;
    String ascensionSeedCandidates;
    if (candidateSeeds == null || !Preferences.getBoolean("ascensionSeedAutomation")) {
      ascensionSeedCandidates = Preferences.getDefault("ascensionSeedCandidates");
    } else if (candidateSeeds.isEmpty()) {
      ascensionSeedCandidates = "none";
    } else {
      ascensionSeedCandidates =
          candidateSeeds.stream().map(Object::toString).collect(Collectors.joining(","));
      if (candidateSeeds.size() == 1) {
        ascensionSeed = candidateSeeds.getFirst();
      }
    }

    Preferences.setString("ascensionSeedCandidates", ascensionSeedCandidates);
    Preferences.setInteger("ascensionSeed", ascensionSeed);
  }

  // Reset the current ascension seed automation process. This generally occurs when either
  // ascension seed automation is first enabled, or when a Criterion is updated with a state that is
  // wider than the previous state.
  private void incrementalReset() {
    setCandidateSeeds(null);
    if (shouldAttemptFreshAutoNarrow()) {
      fullSearchAscensionSeeds();
    }
  }

  // Ascension seed automation update. If we've already narrowed the ascension seed candidates down
  // to a list, only check the updated Criterion against those rather than searching the full list.
  // If the candidate list has not been populated, use heuristics to determine whether it makes
  // sense to try to start the process of narrowing the list of candidates.
  private void incrementalUpdate(Criterion criterion) {
    List<Integer> candidates = getCandidateSeeds();
    if (candidates == null) {
      if (shouldAttemptFreshAutoNarrow()) {
        fullSearchAscensionSeeds();
      }
    } else {
      List<Integer> newCandidates = new ArrayList<>();
      for (int candidate : candidates) {
        if (criterion.matchesSeed(candidate)) {
          newCandidates.add(candidate);
        }
      }
      setCandidateSeeds(newCandidates);
    }
  }

  // Determines whether it makes sense to try to start the process of narrowing the list of
  // candidates. This is true if ascension seed automation is enabled and the expected number of
  // candidates is an order of magnitude smaller than the maximum number of seeds the full search
  // will return.
  private boolean shouldAttemptFreshAutoNarrow() {
    return Preferences.getBoolean("ascensionSeedAutomation")
        && estimatedAscensionSeedPossibilities() < MAX_CALCULATED_SEEDS / 10;
  }

  // If we've already used ascension seed automation to narrow the list of candidate seeds, just
  // return that. Otherwise, perform a full search.
  public List<Integer> possibleAscensionSeeds() {
    if (Preferences.getBoolean("ascensionSeedAutomation")) {
      List<Integer> candidateSeeds = getCandidateSeeds();
      if (candidateSeeds != null) {
        return candidateSeeds;
      }
    }
    return fullSearchAscensionSeeds();
  }

  // Determine which ascension seeds are possible given the state of the character's current prefs.
  // For performance reasons, limit this to finding MAX_CALCULATED_SEEDS (to cover the case where
  // this is called with not nearly enough information available).
  public List<Integer> fullSearchAscensionSeeds() {
    RequestLogger.printLine("Calculating ascension seed candidates...");

    // Derive the criteria based on current account state. Sort it such that the criteria that are
    // most likely to reject a seed candidate are checked first.
    //
    // While it is the case that some criteria give partially- or wholly-redundant data (for example
    // if they are based entirely on the same sequence of RNG draws), attempting to account for this
    // has proven to be too much effort for the small amount of extra processing it saves.
    List<Criterion> criteria =
        CRITERIA.entrySet().stream()
            .map(
                e -> {
                  Criterion c = e.getValue().get();
                  String state = c.compareState("").newState();
                  setState(e.getKey(), state);
                  return c;
                })
            .filter(c -> c.reductionFactor() < 1)
            .sorted(Comparator.comparingDouble(Criterion::sortKey))
            .toList();

    if (RequestLogger.isDebugging()) {
      RequestLogger.updateDebugLog("Ascension seed full search:");
      criteria.stream()
          .map(
              c ->
                  String.format(
                      "%-20s: %10.2f ( %10.2f )",
                      c.getClass().getSimpleName(), 1 / c.reductionFactor(), 1 / c.sortKey()))
          .forEach(RequestLogger::updateDebugLog);
      RequestLogger.updateDebugLog("Starting search at " + LocalDateTime.now());
    }

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
                // allowed. Also give up immediately if the user has aborted.
                if (matchingSeeds.size() >= MAX_CALCULATED_SEEDS || KoLmafia.refusesContinue()) {
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

    if (RequestLogger.isDebugging()) {
      RequestLogger.updateDebugLog("Finished search at " + LocalDateTime.now());
      RequestLogger.updateDebugLog("Result size: " + matchingSeeds.size());
    }

    // It's possible we overshot the limit slightly, so trim it if necessary.
    List<Integer> rv = matchingSeeds.stream().limit(MAX_CALCULATED_SEEDS).sorted().toList();
    if (rv.size() >= MAX_CALCULATED_SEEDS) {
      setCandidateSeeds(null);
    } else {
      setCandidateSeeds(rv);
    }
    return rv;
  }

  // Determine what we expect the relevant prefs to be for all of the things that are determined by
  // the ascension seed, given a specific seed.
  public Map<String, String> getPropertiesForSeed(int seed) {
    Map<String, String> rv = new HashMap<>();
    // nb: Technically the constructors for the criteria waste a little effort parsing out the
    // current state, which is not needed for calculating the expected values for a particular seed.
    // This is a small enough amount of work that we expect to be called few enough times that it
    // isn't worth trying to work around.
    CRITERIA.values().stream()
        .map(Supplier::get)
        .map(c -> c.toProperties(seed))
        .forEach(rv::putAll);
    rv.put("ascensionSeed", Integer.toString(seed));
    return rv;
  }

  // Estimate the number of matching ascension seed candidates that will be found based on the
  // character's current state. This can be useful in informing whether it makes sense to even try
  // searching for the ascension seed yet.
  //
  // Note that the return value will have a tendency to be overly-optimistic: if there are multiple
  // criteria that perform draws of similar granularity, they won't actually narrow down the
  // candidates as much as two truly independent processes would. We accept this inaccuracy for the
  // sake of simplicity.
  public int estimatedAscensionSeedPossibilities() {
    // If ascension seed automation is enabled and we've already narrowed down the list of
    // candidates, there's no need to estimate--directly return the number of possibilities.
    if (Preferences.getBoolean("ascensionSeedAutomation")) {
      List<Integer> candidates = getCandidateSeeds();
      if (candidates != null) {
        return candidates.size();
      }
    }

    double totalReductionFactor =
        CRITERIA.values().stream()
            .map(Supplier::get)
            .mapToDouble(Criterion::reductionFactor)
            .reduce(1, (a, b) -> a * b);
    int totalSeeds = ASCENSION_SEED_MAX - ASCENSION_SEED_MIN + 1;
    return (int) Math.ceil(totalReductionFactor * totalSeeds);
  }

  // region: Ascension Seed criteria

  // Abstract class representing an aspect of the game that is determined by the ascension seed.
  // Each implementation has the knowledge of how to determine whether a particular seed is
  // consistent with the current account state as well as determine the expected state of relevant
  // prefs for any given ascension seed.
  public abstract static class Criterion {
    // The state string that is considered the "default" state for this Criterion (i.e. the state
    // that provides no information for narrowing the ascension seed)
    protected final String defaultState;

    // The preferences which contain the game state tracked by this Criterion
    protected final String[] prefs;

    protected Criterion(String defaultState, String... prefs) {
      this.defaultState = defaultState;
      this.prefs = prefs;
    }

    public final String[] getStatePrefs() {
      return prefs;
    }

    public record CompareResult(UpdateType type, String newState) {}

    // An indicator for whether a new state is narrower, wider, or equal-to the previous state
    public enum UpdateType {
      NARROWER,
      EQUAL,
      WIDER;

      // Widening from any other state always results in a widen, since it means that at least some
      // candidates that were previously discarded need to be re-evaluated.
      public UpdateType widen() {
        return WIDER;
      }

      // Narrowing loses out to widening
      public UpdateType narrow() {
        if (this == WIDER) {
          return WIDER;
        } else {
          return NARROWER;
        }
      }
    }

    // Handle translating a blank state to/from the default state, and then delegate to the
    // Criterion-specific comparison method.
    public final CompareResult compareState(String prevState) {
      if (prevState.isBlank()) {
        prevState = defaultState;
      }
      CompareResult res = compareStateInner(prevState);
      if (res.newState().equals(defaultState)) {
        return new CompareResult(res.type(), "");
      } else {
        return res;
      }
    }

    // Determine whether the current state is narrower, wider, or equal to the previous state, and
    // return the string representation of the current state.
    protected abstract CompareResult compareStateInner(String prevState);

    // Check whether the current account state is consistent with the state expected for the
    // specified ascension seed. Implementations of this method should be side-effect-free.
    public abstract boolean matchesSeed(int seed);

    // Return the expected values of all properties that are associated with this aspect, based on
    // the specified ascension seed.
    public abstract Map<String, String> toProperties(int seed);

    // A heuristic indicating what proportion of ascension seed candidates are consistent with the
    // current value of the prefs associated with this aspect. A lower value indicates that more
    // candidate seeds are likely to be weeded out by checking this criterion.
    //
    // In any reasonable case, the return value should be in the interval (0,1].
    public abstract double reductionFactor();

    // The best sort key that is permitted for this criterion. This function should always return a
    // value in the range [0,1]. The reduction factor will be used to select a value in the interval
    // [bestAllowedSortKey(),1] to use as the sort key.
    //
    // This can be useful for cases where a criterion should prefer to be run later in the filter
    // chain than its reduction factor would normally indicate.
    protected double bestAllowedSortKey() {
      return 0;
    }

    // The value to use for sorting criteria. A lower value will result in a filter being checked
    // earlier. This value is calculated based on reduction factor, scaled into the interval
    // [bestAllowedSortKey(),1].
    public final double sortKey() {
      double sortFloor = bestAllowedSortKey();
      return sortFloor + (1 - sortFloor) * reductionFactor();
    }

    // Utility method used for many reductionFactor() methods. Returns M!/N!
    protected static double factMN(int m, int n) {
      if (n > m) {
        return 1 / factMN(n, m);
      }

      double rv = 1;
      for (int i = n + 1; i <= m; i++) {
        rv *= i;
      }
      return rv;
    }
  }

  // PHPMTRandom takes longer to set up, which makes criteria that use it much slower than any
  // Criterion that uses only PHPRandom. Ensure that PHPMTRandom-using criteria get sorted behind
  // any fast Criterion that rejects a meaningful number of possibilities.
  private abstract static class MTRandomCriterion extends Criterion {
    protected MTRandomCriterion(String defaultState, String... prefs) {
      super(defaultState, prefs);
    }

    @Override
    public double bestAllowedSortKey() {
      return 0.6;
    }
  }

  // Bang Potions
  //
  // Starting with the initial order (sleepiness, confusion, inebriety, teleportitis, detection,
  // ettin strength, mental acuity, blessing, healing), perform a standard shuffle and then assign
  // the effects to potions in increasing order of item ID.
  static class BangPotions extends Criterion {
    private final String[] state = new String[9];

    public BangPotions() {
      super(
          "?????????",
          IntStream.rangeClosed(819, 827)
              .mapToObj(i -> "lastBangPotion" + i)
              .toArray(String[]::new));
      for (int i = 0; i < 9; i++) {
        String si = Preferences.getString(prefs[i]);
        state[i] = si;
      }
    }

    public CompareResult compareStateInner(String prevState) {
      String newState = "";
      UpdateType type = UpdateType.EQUAL;

      for (int i = 0; i < 9; i++) {
        char old = prevState.charAt(i);
        char now = state[i].isBlank() ? '?' : state[i].charAt(0);
        newState += now;
        if (old != now) {
          type = old == '?' ? type.narrow() : type.widen();
        }
      }
      return new CompareResult(type, newState);
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
        if (!state[i].isEmpty() && !state[i].equals(arr.get(i))) {
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
      // Reduction factor is N!/9!, where N is the number of unidentified bang potions.
      int unidentified = 0;
      for (String v : state) {
        if (v.isEmpty()) {
          unidentified++;
        }
      }
      return factMN(unidentified, 9);
    }
  }

  // Playing the Catalog Card noncombat
  //
  // For each book, use mt_rand(1,26), mt_rand(1,26), mt_rand(111,999), and mt_rand(11,99) to
  // produce the card catalog code for the book. Then, skip several draws (used to generate the book
  // title) before repeating the process for the next book.
  static class CardCatalog extends MTRandomCriterion {
    private final Set<String> state;

    public CardCatalog() {
      super("", "merkinCatalogChoices");
      String pref = Preferences.getString("merkinCatalogChoices");
      if (pref.isBlank()) {
        state = Collections.emptySet();
        return;
      }

      String[] parts = pref.split(",");
      state = Arrays.stream(parts).map(s -> s.substring(0, 8)).collect(Collectors.toSet());
    }

    public CompareResult compareStateInner(String prevState) {
      Set<String> oldState =
          prevState.isBlank()
              ? Collections.emptySet()
              : Arrays.stream(prevState.split(",")).collect(Collectors.toSet());

      UpdateType type = UpdateType.EQUAL;
      if (!state.containsAll(oldState)) {
        type = type.widen();
      } else if (!oldState.containsAll(state)) {
        type = type.narrow();
      }

      return new CompareResult(type, String.join(",", state));
    }

    private List<String> generateForSeed(int seed, int limit) {
      List<String> rv = new ArrayList<>();
      PHPMTRandom rng = new PHPMTRandom(seed);
      for (int i = 0; i < limit; i++) {
        if (i > 0) {
          // Skip a variable number of draws for book title generation
          int v = rng.nextInt(1, 6);
          int skip = (v % 3 == 0) ? 4 : 3;
          for (int s = 0; s < skip; s++) {
            rng.nextInt();
          }
        }

        String book =
            String.format(
                "%s%s%3d.%2d",
                (char) rng.nextInt('A', 'Z'),
                (char) rng.nextInt('A', 'Z'),
                rng.nextInt(111, 999),
                rng.nextInt(11, 99));
        rv.add(book);
      }
      return rv;
    }

    @Override
    public boolean matchesSeed(int seed) {
      // Quick rejection: The first three books generated contain the dreadscroll clues and always
      // appear in the NC, regardless of Mer-kin vocabulary knowledge. Generate only the first book
      // and check if it was seen. This will reject almost all misses.
      String quickBook = generateForSeed(seed, 1).getFirst();
      if (!state.contains(quickBook)) {
        return false;
      }

      // Full check: All books in the NC must be one of the 13 generated books.
      List<String> validBooks = generateForSeed(seed, 13);
      for (String book : state) {
        if (!validBooks.contains(book)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public Map<String, String> toProperties(int seed) {
      List<String> books = generateForSeed(seed, 13);
      List<String> clueBooks = List.of(books.get(0), books.get(1), books.get(2));

      PHPRandom rng = new PHPRandom(seed);
      rng.shuffle(books);

      for (int i = 0; i < books.size(); i++) {
        String book = books.get(i);
        String formatted =
            String.format("%s:%d:%s", book, i + 1, clueBooks.contains(book) ? "clue" : "stats");
        books.set(i, formatted);
      }
      String generatedPref = books.stream().sorted().collect(Collectors.joining(","));

      return Map.of("merkinCatalogChoices", generatedPref);
    }

    @Override
    public double reductionFactor() {
      // Each book reduces by a factor of (26*26*889*89), but 13!/(13-N)! arrangements of the known
      // books are possible.
      double rv = Math.pow(26 * 26 * 889 * 89, -state.size());
      rv *= factMN(13, 13 - state.size());
      return rv;
    }
  }

  // Daily Dungeon Room Order
  //
  // Starting with the initial order MMMMDDDDTTTT (where M is a monster room, D is a door room, and
  // T is a trap room), perform a standard shuffle and insert the prize rooms after every fourth
  // room.
  static class DailyDungeon extends Criterion {
    private final String state;

    public DailyDungeon() {
      super("????????????", "dailyDungeonRooms");
      String pref = Preferences.getString("dailyDungeonRooms").replaceAll("[^MDT?]", "");
      if (pref.length() == 12) {
        state = pref;
      } else {
        state = defaultState;
      }
    }

    public CompareResult compareStateInner(String prevState) {
      UpdateType type = UpdateType.EQUAL;
      for (int i = 0; i < 12; i++) {
        if (prevState.charAt(i) != state.charAt(i)) {
          type = prevState.charAt(i) == '?' ? type.narrow() : type.widen();
        }
      }
      return new CompareResult(type, state);
    }

    private static final List<String> INIT_ROOM_ORDER =
        Stream.of("MMMMDDDDTTTT".split("")).toList();

    private String generateForSeed(int seed) {
      PHPRandom rng = new PHPRandom(seed);
      List<String> arr = new ArrayList<>(INIT_ROOM_ORDER);
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
      rv *= factMN(cntM + cntD + cntT, 12);
      rv *= factMN(4, cntM);
      rv *= factMN(4, cntD);
      rv *= factMN(4, cntT);
      return rv;
    }
  }

  // Mer-kin dreadscroll answers
  //
  // Use mt_rand(1,4) eight times to select numbers from 1 to 4.
  static class Dreadscroll extends MTRandomCriterion {
    private final int[] state;

    public Dreadscroll() {
      super(
          "00000000",
          IntStream.rangeClosed(1, 8).mapToObj(i -> "dreadScroll" + i).toArray(String[]::new));
      state = new int[8];
      for (int i = 0; i < 8; i++) {
        state[i] = Preferences.getInteger(prefs[i]);
      }
    }

    public CompareResult compareStateInner(String prevState) {
      UpdateType type = UpdateType.EQUAL;

      for (int i = 0; i < 8; i++) {
        int old = Character.getNumericValue(prevState.charAt(i));
        if (old != state[i]) {
          type = old == 0 ? type.narrow() : type.widen();
        }
      }
      return new CompareResult(
          type, Arrays.stream(state).mapToObj(Integer::toString).collect(Collectors.joining()));
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

  // Leprecondo needs order
  //
  // Starting with the initial order (exercise, mental stimulation, dumb entertainment, food, booze,
  // sleep), perform a standard shuffle.
  //
  // NOTE: It is possible under some conditions for mafia the record the need order incorrectly and
  // no way to definitively correct the order. As such, it is never used to calculate the ascension
  // seed and is only a predictor output.
  static class LeprecondoNeeds extends Criterion {
    public LeprecondoNeeds() {
      super("");
    }

    public CompareResult compareStateInner(String prevState) {
      return new CompareResult(UpdateType.EQUAL, "");
    }

    private List<String> generateForSeed(int seed) {
      PHPRandom rng = new PHPRandom(seed);
      List<String> arr =
          new ArrayList<>(
              List.of(
                  "exercise",
                  "mental stimulation",
                  "dumb entertainment",
                  "food",
                  "booze",
                  "sleep"));
      rng.shuffle(arr);
      return arr;
    }

    @Override
    public boolean matchesSeed(int seed) {
      // This criterion is only a predictor output.
      return true;
    }

    @Override
    public Map<String, String> toProperties(int seed) {
      List<String> needs = generateForSeed(seed);
      return Map.of("leprecondoNeedOrder", String.join(",", needs));
    }

    @Override
    public double reductionFactor() {
      // This criterion is only a predictor output.
      return 1;
    }
  }

  // Rave Combos
  //
  // Starting with the initial order (BPR, BRP, PBR, PRB, RBP, RPB)--where R, B, and P are the rave
  // combo skills--perform a standard shuffle. Then assign the skill sequences to the rave combos in
  // the following order: Rave Steal, Rave Bleeding, Rave Knockout, Rave Concentration, Rave
  // Nirvana, Rave Stats.
  static class RaveCombos extends Criterion {
    private final String[] state = new String[6];

    public RaveCombos() {
      super(
          "??????????????????",
          IntStream.rangeClosed(1, 6).mapToObj(i -> "raveCombo" + i).toArray(String[]::new));
      for (int i = 0; i < 6; i++) {
        String combo = Preferences.getString(prefs[i]);
        if (!combo.isEmpty()) {
          combo =
              Arrays.stream(combo.split(","))
                  .map(String::trim)
                  .map(s -> s.substring(0, 1))
                  .collect(Collectors.joining());
        }
        state[i] = combo;
      }
    }

    public CompareResult compareStateInner(String prevState) {
      UpdateType type = UpdateType.EQUAL;

      for (int i = 0; i < 6; i++) {
        String old = prevState.substring(3 * i, 3 * (i + 1));
        old = old.equals("???") ? "" : old;
        if (!old.equals(state[i])) {
          type = old.isBlank() ? type.narrow() : type.widen();
        }
      }
      return new CompareResult(
          type,
          Arrays.stream(state).map(s -> s.isBlank() ? "???" : s).collect(Collectors.joining()));
    }

    // The order that shuffled combos get assigned to the mafia-numbered effects
    private static final int[] RAVE_COMBO_ORDER = {3, 4, 2, 1, 0, 5};

    private List<String> generateForSeed(int seed) {
      PHPRandom rng = new PHPRandom(seed);
      List<String> arr = new ArrayList<>(List.of("BPR", "BRP", "PBR", "PRB", "RBP", "RPB"));
      rng.shuffle(arr);
      return IntStream.range(0, 6).mapToObj(i -> arr.get(RAVE_COMBO_ORDER[i])).toList();
    }

    @Override
    public boolean matchesSeed(int seed) {
      List<String> predicted = generateForSeed(seed);
      for (int i = 0; i < 6; i++) {
        if (!state[i].isBlank() && !state[i].equals(predicted.get(i))) {
          return false;
        }
      }
      return true;
    }

    @Override
    public Map<String, String> toProperties(int seed) {
      Map<String, String> rv = new HashMap<>();
      List<String> combos = generateForSeed(seed);
      for (int i = 0; i < 6; i++) {
        String combo = combos.get(i);
        combo = combo.charAt(0) + "," + combo.charAt(1) + "," + combo.charAt(2);
        combo =
            combo
                .replaceFirst("B", "Break It On Down")
                .replaceFirst("P", "Pop and Lock It")
                .replaceFirst("R", "Run Like the Wind");
        rv.put("raveCombo" + (i + 1), combo);
      }
      return rv;
    }

    @Override
    public double reductionFactor() {
      // N!/6!, where N is the number of unidentified combos
      int unidentified = 0;
      for (int i = 0; i < 6; i++) {
        if (state[i].isBlank()) {
          unidentified++;
        }
      }
      return factMN(unidentified, 6);
    }
  }

  // Wild seahorse name
  //
  // Use mt_rand(1,4) until a non-4 result is achieved. If the result is a 1, the name is selected
  // from SWIM_NAMES. If the result is a 2, the name is two words; the first word is selected from
  // TWOPART_NAMES_1 and the second word is selected from TWOPART_NAMES_2. If the result is a 3, the
  // name is selected from JACK_NAMES.
  static class SeahorseName extends MTRandomCriterion {
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
      super("", "seahorseName");
      state = Preferences.getString("seahorseName");
    }

    public CompareResult compareStateInner(String prevState) {
      UpdateType type = UpdateType.EQUAL;
      if (!prevState.equals(state)) {
        type = prevState.isBlank() ? type.narrow() : type.widen();
      }
      return new CompareResult(type, state);
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

  // Slime Potion effects
  //
  // Starting with the initial order (strong, sagacious, speedy), perform a standard shuffle and
  // then assign the effects to the red, blue, and yellow potions in that order.
  // Re-seed the RNG. Starting with the initial order (brawn, brains, briskness), perform a standard
  // shuffle and then assign the effects to the orange, green, and violet potions in that order.
  // Re-seed the RNG. Starting with the initial order (muscle, mentalism, moxiousness, intensity,
  // eyesight, slimeform), perform a standard shuffle and then assign the effects to the vermilion,
  // amber, chartreuse, teal, purple, and indigo potions in that order.
  static class SlimePotions extends Criterion {
    private final String[] state = new String[12];

    public SlimePotions() {
      super(
          "????????????",
          IntStream.rangeClosed(3885, 3896)
              .mapToObj(i -> "lastSlimeVial" + i)
              .toArray(String[]::new));
      for (int i = 0; i < 12; i++) {
        state[i] = Preferences.getString(prefs[i]);
      }
    }

    public CompareResult compareStateInner(String prevState) {
      String newState = "";

      UpdateType type = UpdateType.EQUAL;
      for (int i = 0; i < 12; i++) {
        char old = prevState.charAt(i);
        char now =
            state[i].isBlank()
                ? '?'
                : switch (i) {
                  case 3, 4, 5 -> state[i].charAt(3);
                  default -> state[i].charAt(1);
                };
        if (old != now) {
          type = old == '?' ? type.narrow() : type.widen();
        }
        newState += now;
      }
      return new CompareResult(type, newState);
    }

    private static final String[] PRIMARY_POTIONS = {"strong", "sagacious", "speedy"};
    private static final String[] SECONDARY_POTIONS = {"brawn", "brains", "briskness"};
    private static final String[] TERTIARY_POTIONS = {
      "muscle", "mentalism", "moxiousness", "intensity", "eyesight", "slimeform"
    };

    private String[] generateForSeed(int seed) {
      String[] rv = new String[12];
      PHPRandom rng = new PHPRandom(seed);
      List<Integer> arr = new ArrayList<>(List.of(0, 1, 2));
      rng.shuffle(arr);
      rv[0] = PRIMARY_POTIONS[arr.get(0)];
      // These two are swapped from what you'd expect
      rv[1] = PRIMARY_POTIONS[arr.get(2)];
      rv[2] = PRIMARY_POTIONS[arr.get(1)];

      rv[3] = SECONDARY_POTIONS[arr.get(0)];
      rv[4] = SECONDARY_POTIONS[arr.get(1)];
      rv[5] = SECONDARY_POTIONS[arr.get(2)];

      rng.setSeed(seed);
      arr = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
      rng.shuffle(arr);
      rv[6] = TERTIARY_POTIONS[arr.get(0)];
      rv[7] = TERTIARY_POTIONS[arr.get(1)];
      rv[8] = TERTIARY_POTIONS[arr.get(2)];
      rv[9] = TERTIARY_POTIONS[arr.get(3)];
      // These two are swapped from what you'd expect
      rv[10] = TERTIARY_POTIONS[arr.get(5)];
      rv[11] = TERTIARY_POTIONS[arr.get(4)];

      return rv;
    }

    @Override
    public boolean matchesSeed(int seed) {
      String[] effects = generateForSeed(seed);
      for (int i = 0; i < 12; i++) {
        if (!state[i].isBlank() && !state[i].equals(effects[i])) {
          return false;
        }
      }
      return true;
    }

    @Override
    public Map<String, String> toProperties(int seed) {
      Map<String, String> rv = new HashMap<>();
      String[] effects = generateForSeed(seed);
      for (int i = 0; i < 12; i++) {
        rv.put("lastSlimeVial" + (3885 + i), effects[i]);
      }
      return rv;
    }

    @Override
    public double reductionFactor() {
      // Primary and secondary potions are fully-correlated and provide a N!/3! reduction, where N
      // is the number of unidentified positions.
      int unidentified = 0;
      for (int i = 0; i < 3; i++) {
        if (state[i].isBlank() && state[i + 3].isBlank()) {
          unidentified++;
        }
      }
      double rv = factMN(unidentified, 3);

      // Tertiary potions are technically partially-correlated with the primary/secondary, but
      // assuming that they are independent is close enough. A further N!/6! reduction.
      unidentified = 0;
      for (int i = 6; i < 12; i++) {
        if (state[i].isBlank()) {
          unidentified++;
        }
      }
      rv *= factMN(unidentified, 6);

      return rv;
    }
  }

  // Violet Fog layout
  //
  // Each noncombat leads to three other predetermined noncombats in the Violet Fog, which will be
  // represented here as
  //    A = noncombat with the lowest choiceAdventure ID of the three
  //    B = noncombat with intermediate choiceAdventure ID
  //    C = noncombat highest choiceAdventure ID of the three
  //
  // Start with an initial order depending on your current noncombat:
  //    For the first 14 noncombats (choice IDs 48 - 61), start with [A, B, C]
  //    For some reward noncombats (choice IDs 62, 63, 66, 68, 69) start with [A, C, B]
  //    For the other reward noncombats (choice IDs 64, 65, 67, 70) start with [B, C, A]
  //
  // For each noncombat, seed the RNG with ascensionSeed + choiceAdventureID. Then perform a
  // standard shuffle and assign the final sequence to the options "This way", "That way", "The
  // other way" in that order.
  static class VioletFog extends Criterion {
    private final int[] state = new int[69];
    private final boolean[] explored = new boolean[23];

    public VioletFog() {
      super(
          IntStream.range(0, 69).mapToObj(i -> "0").collect(Collectors.joining(",")),
          "violetFogLayout");
      String layoutString = Preferences.getString("violetFogLayout");
      if (layoutString.isBlank()) {
        return;
      }

      int[] layout = Arrays.stream(layoutString.split(",")).mapToInt(Integer::parseInt).toArray();
      if (layout.length != 92) {
        return;
      }

      for (int choice = 48; choice <= 70; choice++) {
        int baseIdx = 4 * (choice - 48);
        if (choice >= 62) {
          baseIdx++;
        }
        for (int c = 0; c < 3; c++) {
          if (layout[baseIdx + c] > 0) {
            state[3 * (choice - 48) + c] = layout[baseIdx + c];
            explored[choice - 48] = true;
          }
        }
      }
    }

    public CompareResult compareStateInner(String prevState) {
      int[] oldState = Arrays.stream(prevState.split(",")).mapToInt(Integer::parseInt).toArray();

      UpdateType type = UpdateType.EQUAL;
      for (int i = 0; i < 69; i++) {
        if (oldState[i] != state[i]) {
          type = oldState[i] <= 0 ? type.narrow() : type.widen();
        }
      }
      return new CompareResult(
          type, Arrays.stream(state).mapToObj(Integer::toString).collect(Collectors.joining(",")));
    }

    // Every three numbers represent the exits for one of the violet fog NCs, in the order they
    // should be in before being shuffled.
    List<Integer> EXITS_INIT =
        List.of(
            49, 50, 51, 52, 53, 56, 53, 54, 57, 52, 54, 55, 61, 65, 68, 61, 66, 69, 61, 67, 70, 58,
            65, 70, 59, 66, 68, 60, 67, 69, 51, 52, 63, 49, 53, 62, 50, 54, 64, 49, 50, 51, 50, 61,
            52, 51, 61, 53, 54, 61, 49, 51, 54, 50, 49, 52, 51, 50, 53, 49, 49, 53, 50, 50, 54, 51,
            51, 52, 49);

    private List<Integer> generateForSeed(int seed, int choice) {
      List<Integer> exits =
          new ArrayList<>(EXITS_INIT.subList(3 * (choice - 48), 3 * (choice - 47)));
      PHPRandom rng = new PHPRandom(seed + choice);
      rng.shuffle(exits);
      return exits;
    }

    @Override
    public boolean matchesSeed(int seed) {
      for (int choice = 48; choice <= 70; choice++) {
        if (!explored[choice - 48]) {
          continue;
        }
        List<Integer> expected = generateForSeed(seed, choice);
        for (int i = 0; i < 3; i++) {
          if (state[3 * (choice - 48) + i] > 0 && state[3 * (choice - 48) + i] != expected.get(i)) {
            return false;
          }
        }
      }
      return true;
    }

    @Override
    public Map<String, String> toProperties(int seed) {
      StringBuilder sb = new StringBuilder();
      for (int choice = 48; choice <= 70; choice++) {
        List<Integer> exits = generateForSeed(seed, choice);
        if (choice >= 62) {
          sb.append(String.format("-1,%d,%d,%d,", exits.get(0), exits.get(1), exits.get(2)));
        } else {
          sb.append(String.format("%d,%d,%d,-1,", exits.get(0), exits.get(1), exits.get(2)));
        }
      }
      sb.setLength(sb.length() - 1);
      return Map.of("violetFogLayout", sb.toString());
    }

    @Override
    public double reductionFactor() {
      // Each choice reduces by N!/3!, where N is the number of unknown exits for that choice.
      double rv = 1;
      for (int choice = 48; choice <= 70; choice++) {
        int unknown = 0;
        for (int c = 0; c < 3; c++) {
          if (state[3 * (choice - 48) + c] <= 0) {
            unknown++;
          }
        }
        rv *= factMN(unknown, 3);
      }
      return rv;
    }
  }

  // Yeg demon name
  //
  // Seed the RNG with 44*ascension seed. Using the syllable array {Ya, Ga, Ye, Ge, Yi, Gi, Yo, Go,
  // Yu, Gu}, index into it using seven consecutive calls of mt_rand(0,9).
  static class YegDemon extends MTRandomCriterion {
    private static final List<String> SYLLABLES =
        List.of("ya", "ga", "ye", "ge", "yi", "gi", "yo", "go", "yu", "gu");
    private static final List<String> SYLLABLES_UPPER =
        SYLLABLES.stream().map(s -> s.substring(0, 1).toUpperCase() + s.substring(1)).toList();

    private final int[] state = {-1, -1, -1, -1, -1, -1, -1};

    public YegDemon() {
      super("-1,-1,-1,-1,-1,-1,-1", "demonName13", "cargoPocketScraps");
      String demonName =
          Preferences.getString("demonName13").toLowerCase().replaceAll("[^ygaeiou]", "");

      if (demonName.length() == 14) {
        for (int i = 0; i < 7; i++) {
          String syl = demonName.substring(2 * i, 2 * (i + 1));
          state[i] = SYLLABLES.indexOf(syl);
        }
      } else {
        Map<Integer, String> knownScraps = CargoCultistShortsRequest.knownScrapPockets();
        for (int i = 0; i < 7; i++) {
          int pocket = PocketDatabase.scrapSyllables.get(i).getPocket();
          if (knownScraps.containsKey(pocket)) {
            state[i] =
                SYLLABLES.indexOf(
                    knownScraps.get(pocket).toLowerCase().replaceAll("[^ygaeiou]", ""));
          }
        }
      }
    }

    public CompareResult compareStateInner(String prevState) {
      int[] oldState = Arrays.stream(prevState.split(",")).mapToInt(Integer::parseInt).toArray();

      UpdateType type = UpdateType.EQUAL;
      for (int i = 0; i < 7; i++) {
        if (oldState[i] != state[i]) {
          type = oldState[i] == -1 ? type.narrow() : type.widen();
        }
      }
      return new CompareResult(
          type, Arrays.stream(state).mapToObj(Integer::toString).collect(Collectors.joining(",")));
    }

    private int[] generateForSeed(int seed) {
      PHPMTRandom rng = new PHPMTRandom(44L * seed);
      int[] rv = new int[7];
      for (int i = 0; i < 7; i++) {
        rv[i] = rng.nextInt(0, 9);
      }
      return rv;
    }

    @Override
    public boolean matchesSeed(int seed) {
      int[] expected = generateForSeed(seed);
      for (int i = 0; i < 7; i++) {
        if (state[i] > -1 && state[i] != expected[i]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public Map<String, String> toProperties(int seed) {
      int[] choices = generateForSeed(seed);
      String demonName =
          String.format(
              "%s%s %s%s %s%s%s",
              SYLLABLES_UPPER.get(choices[0]),
              SYLLABLES.get(choices[1]),
              SYLLABLES_UPPER.get(choices[2]),
              SYLLABLES.get(choices[3]),
              SYLLABLES_UPPER.get(choices[4]),
              SYLLABLES.get(choices[5]),
              SYLLABLES.get(choices[6]));
      return Map.of("demonName13", demonName);
    }

    @Override
    public double reductionFactor() {
      // 1/10^N, where N is the number of known syllables
      double rv = 1;
      for (int i = 0; i < 7; i++) {
        if (state[i] > -1) {
          rv /= 10;
        }
      }
      return rv;
    }
  }
  // endregion
}
