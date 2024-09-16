package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TrackManager {
  private static final Set<Tracked> trackedMonsters = new LinkedHashSet<>();
  private static final Set<Tracked> trackedPhyla = new LinkedHashSet<>();

  private TrackManager() {}

  private enum TrackType {
    MONSTER,
    PHYLUM
  }

  private enum Reset {
    TURN_RESET,
    TURN_ROLLOVER_RESET,
    ROLLOVER_RESET,
    EFFECT_RESET,
    AVATAR_RESET,
    AVATAR_TURN_RESET,
    AVATAR_ROLLOVER_RESET,
    ASCENSION_RESET;

    private Boolean rolloverReset;
    private Boolean turnReset;
    private Boolean avatarReset;

    final boolean isRolloverReset() {
      if (rolloverReset == null) {
        this.rolloverReset =
            this == TURN_ROLLOVER_RESET || this == ROLLOVER_RESET || this == AVATAR_ROLLOVER_RESET;
      }

      return rolloverReset;
    }

    final boolean isTurnReset() {
      if (turnReset == null) {
        this.turnReset =
            this == TURN_RESET || this == TURN_ROLLOVER_RESET || this == AVATAR_TURN_RESET;
      }

      return turnReset;
    }

    final boolean isAvatarReset() {
      if (avatarReset == null) {
        this.avatarReset =
            this == AVATAR_RESET || this == AVATAR_TURN_RESET || this == AVATAR_ROLLOVER_RESET;
      }

      return avatarReset;
    }
  }

  public enum Tracker {
    OLFACTION("Transcendent Olfaction", 3, true, -1, Reset.ASCENSION_RESET),
    NOSY_NOSE("Nosy Nose", 1, false, -1, Reset.ASCENSION_RESET),
    GALLAPAGOS("Gallapagosian Mating Call", 1, false, -1, Reset.ROLLOVER_RESET),
    LATTE("Offer Latte to Opponent", 2, false, 30, Reset.TURN_ROLLOVER_RESET),
    SUPERFICIAL("Be Superficially interested", 3, false, 80, Reset.TURN_RESET),
    CREAM_JIGGLE("Staff of the Cream of the Cream", 2, false, -1, Reset.AVATAR_ROLLOVER_RESET),
    MAKE_FRIENDS("Make Friends", 3, false, -1, Reset.AVATAR_RESET),
    CURSE_OF_STENCH("Curse of Stench", 3, false, -1, Reset.AVATAR_RESET),
    LONG_CON("Long Con", 3, true, -1, Reset.AVATAR_RESET),
    PERCEIVE_SOUL("Perceive Soul", 2, false, 30, Reset.AVATAR_TURN_RESET),
    MOTIF("Motif", 2, true, -1, Reset.AVATAR_RESET),
    MONKEY_POINT("Monkey Point", 2, false, -1, Reset.ASCENSION_RESET),
    // HOLD_HANDS, but we have no idea for the copies
    PRANK_CARD("prank Crimbo card", 3, true, 100, Reset.TURN_ROLLOVER_RESET),
    TRICK_COIN("trick coin", 3, true, 100, Reset.TURN_ROLLOVER_RESET),
    HUNT("Hunt", 3, true, -1, Reset.AVATAR_RESET), // TODO: check values
    RED_SNAPPER("Red-Nosed Snapper", 2, false, -1, Reset.ASCENSION_RESET, TrackType.PHYLUM),
    A_BEASTLY_ODOR("A Beastly Odor", 2, false, -1, Reset.EFFECT_RESET, TrackType.PHYLUM),
    EW_THE_HUMANITY("Ew, The Humanity", 2, false, -1, Reset.EFFECT_RESET, TrackType.PHYLUM),
    ;

    final String name;
    final int copies;
    final boolean ignoreQueue;
    final int duration;
    final Reset resetType;
    final TrackType trackType;

    public static Tracker find(final String name) {
      return Arrays.stream(values())
          .filter(b -> b.getName().equalsIgnoreCase(name))
          .findAny()
          .orElse(null);
    }

    Tracker(
        final String name,
        final int copies,
        final boolean ignoreQueue,
        final int duration,
        final Reset resetType) {
      this(name, copies, ignoreQueue, duration, resetType, TrackType.MONSTER);
    }

    Tracker(
        final String name,
        final int copies,
        final boolean ignoreQueue,
        final int duration,
        final Reset resetType,
        final TrackType trackType) {
      this.name = name;
      this.copies = copies;
      this.ignoreQueue = ignoreQueue;
      this.duration = duration;
      this.resetType = resetType;
      this.trackType = trackType;
    }

    public final String getName() {
      return this.name;
    }

    public final int getCopies() {
      return this.copies;
    }

    public final boolean isIgnoreQueue() {
      return this.ignoreQueue;
    }

    public final int getDuration() {
      return this.duration;
    }

    public final TrackType getTrackType() {
      return this.trackType;
    }

    public final boolean isEffective() {
      if (this == Tracker.NOSY_NOSE) {
        return KoLCharacter.getFamiliar().getId() == FamiliarPool.NOSY_NOSE;
      } else if (this == Tracker.RED_SNAPPER) {
        return KoLCharacter.getFamiliar().getId() == FamiliarPool.RED_SNAPPER;
      }
      return true;
    }
  }

  private record Tracked(String tracked, Tracker tracker, int turnTracked) {
    private Integer turnsLeft() {
      return (turnTracked + tracker.getDuration()) - KoLCharacter.getCurrentRun();
    }

    public boolean isValid() {
      return switch (tracker.resetType) {
        case TURN_RESET, TURN_ROLLOVER_RESET -> turnsLeft() > 0;
        case EFFECT_RESET -> {
          var effect =
              switch (tracker) {
                case A_BEASTLY_ODOR -> EffectPool.get(EffectPool.A_BEASTLY_ODOR);
                case EW_THE_HUMANITY -> EffectPool.get(EffectPool.EW_THE_HUMANITY);
                default -> throw new IllegalStateException("Unexpected value: " + tracker);
              };
          yield KoLConstants.activeEffects.contains(effect);
        }
        default -> true;
      };
    }
  }

  private static Set<Tracked> getTrackedSet(Tracker tracker) {
    return switch (tracker.getTrackType()) {
      case MONSTER -> trackedMonsters;
      case PHYLUM -> trackedPhyla;
    };
  }

  public static void clearCache() {
    TrackManager.trackedMonsters.clear();
    TrackManager.trackedPhyla.clear();
  }

  public static void loadTracked() {
    TrackManager.loadTrackedMonsters();
    TrackManager.loadTrackedPhyla();
  }

  static void loadTrackedMonsters() {
    loadTrackedX("trackedMonsters", TrackManager.trackedMonsters);
  }

  static void loadTrackedPhyla() {
    loadTrackedX("trackedPhyla", TrackManager.trackedPhyla);
  }

  private static void loadTrackedX(String prefName, Set<Tracked> tracked) {
    tracked.clear();

    String tracks = Preferences.getString(prefName);
    if (tracks.isEmpty()) {
      return;
    }

    StringTokenizer tokens = new StringTokenizer(tracks, ":");

    while (tokens.hasMoreTokens()) {
      String monsterName = tokens.nextToken();
      if (!tokens.hasMoreTokens()) break;
      String trackerName = tokens.nextToken();
      if (!tokens.hasMoreTokens()) break;
      int turnTracked = StringUtilities.parseInt(tokens.nextToken());

      var tracker = Tracker.find(trackerName);

      if (tracker == null) {
        KoLmafia.updateDisplay("Attempted to parse unknown tracker " + trackerName + ".");
        continue;
      }

      TrackManager.addTracked(monsterName, tracker, turnTracked);
    }
  }

  private static void saveTrackedMonsters() {
    Preferences.setString(
        "trackedMonsters",
        trackedMonsters.stream()
            .flatMap(m -> Stream.of(m.tracked(), m.tracker().getName(), m.turnTracked()))
            .map(Object::toString)
            .collect(Collectors.joining(":")));
  }

  private static void saveTrackedPhyla() {
    Preferences.setString(
        "trackedPhyla",
        trackedPhyla.stream()
            .flatMap(m -> Stream.of(m.tracked(), m.tracker().getName(), m.turnTracked()))
            .map(Object::toString)
            .collect(Collectors.joining(":")));
  }

  /**
   * Iterate through all tracked monsters removing entries if the supplied predicate matches.
   *
   * @param predicate Predicate dictating removal
   */
  private static void resetIf(Predicate<Tracked> predicate) {
    TrackManager.trackedMonsters.removeIf(predicate);
    TrackManager.saveTrackedMonsters();
    TrackManager.trackedPhyla.removeIf(predicate);
    TrackManager.saveTrackedPhyla();
  }

  /**
   * Iterate through all tracked monsters removing entries if the supplied predicate matches their
   * Reset type.
   *
   * @param predicate Predicate dictating removal
   */
  private static void resetIfType(Predicate<Reset> predicate) {
    resetIf(m -> predicate.test(m.tracker().resetType));
  }

  public static void resetRollover() {
    resetIfType(Reset::isRolloverReset);
  }

  public static void resetAvatar() {
    resetIfType(Reset::isAvatarReset);
  }

  public static void resetAscension() {
    resetIf(x -> true);
  }

  public static void recalculate() {
    resetIf(Predicate.not(Tracked::isValid));
  }

  public static void trackCurrentMonster(final Tracker tracker) {
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    if (monster == null) {
      return;
    }
    TrackManager.trackMonster(monster, tracker);
  }

  /**
   * Track a monster
   *
   * @param monster Instance of the monster to track
   * @param tracker Tracker type
   */
  public static void trackMonster(final MonsterData monster, final Tracker tracker) {
    String tracked =
        switch (tracker.getTrackType()) {
          case MONSTER -> monster.getName();
          case PHYLUM -> monster.getPhylum().toString();
        };

    // TODO: Tracks fail in some areas, monsters in them cannot be tracked, but we don't track this

    track(tracked, tracker);
  }

  public static void track(final String tracked, final Tracker tracker) {
    TrackManager.removeTrack(tracker);

    KoLmafia.updateDisplay(tracked + " tracked by " + tracker.getName() + ".");

    var turnTracked = KoLCharacter.getCurrentRun();

    TrackManager.addTracked(tracked, tracker, turnTracked);

    TrackManager.recalculate();

    // Legacy support
    switch (tracker) {
      case OLFACTION -> Preferences.setString("olfactedMonster", tracked);
      case NOSY_NOSE -> Preferences.setString("nosyNoseMonster", tracked);
      case GALLAPAGOS -> Preferences.setString("_gallapagosMonster", tracked);
      case LATTE -> Preferences.setString("_latteMonster", tracked);
      case SUPERFICIAL -> Preferences.setString("superficiallyInterestedMonster", tracked);
      case CREAM_JIGGLE -> Preferences.setString("_jiggleCreamedMonster", tracked);
      case MAKE_FRIENDS -> Preferences.setString("makeFriendsMonster", tracked);
      case CURSE_OF_STENCH -> Preferences.setString("stenchCursedMonster", tracked);
      case LONG_CON -> Preferences.setString("longConMonster", tracked);
      case MOTIF -> Preferences.setString("motifMonster", tracked);
      case MONKEY_POINT -> Preferences.setString("monkeyPointMonster", tracked);
      case PRANK_CARD -> Preferences.setString("_prankCardMonster", tracked);
      case TRICK_COIN -> Preferences.setString("_trickCoinMonster", tracked);
      case RED_SNAPPER -> Preferences.setString("redSnapperPhylum", tracked);
    }
  }

  private static void addTracked(final String entry, final Tracker tracker, final int turnTracked) {
    var tracked = new Tracked(entry, tracker, turnTracked);
    if (!tracked.isValid()) {
      KoLmafia.updateDisplay(
          "Track of " + entry + " by " + tracker.getName() + " failed, as the track was invalid.");
      return;
    }

    getTrackedSet(tracker).add(tracked);
  }

  private static void removeTrack(final Tracker tracker) {
    resetIf(t -> t.tracker() == tracker);
  }

  public static long countCopies(final String monster) {
    TrackManager.recalculate();

    var monsterCopies =
        trackedMonsters.stream()
            .filter(m -> m.tracked().equalsIgnoreCase(monster) && m.tracker().isEffective())
            .mapToInt(t -> t.tracker().copies)
            .sum();

    MonsterData data = MonsterDatabase.findMonster(monster, false, false);
    if (data == null) {
      return monsterCopies;
    }
    var phylaCopies =
        trackedPhyla.stream()
            .filter(m -> m.tracker().isEffective())
            .filter(m -> m.tracked().equalsIgnoreCase(data.getPhylum().toString()))
            .mapToInt(t -> t.tracker().copies)
            .sum();
    return monsterCopies + phylaCopies;
  }

  public static boolean isQueueIgnored(final String monster) {
    TrackManager.recalculate();

    // there is no way for a phyla copy to make the monster ignore queue
    return trackedMonsters.stream()
        .anyMatch(
            m ->
                m.tracker().isIgnoreQueue()
                    && m.tracked().equalsIgnoreCase(monster)
                    && m.tracker().isEffective());
  }

  public static Tracker[] trackedBy(final MonsterData data) {
    TrackManager.recalculate();

    if (data == null) {
      return new Tracker[0];
    }

    var monsterTracks =
        trackedMonsters.stream()
            .filter(m -> m.tracked().equalsIgnoreCase(data.getName()))
            .filter(m -> m.tracker().isEffective());
    var phylaTracks =
        trackedPhyla.stream()
            .filter(m -> m.tracked().equalsIgnoreCase(data.getPhylum().toString()))
            .filter(m -> m.tracker().isEffective());
    return Stream.concat(monsterTracks, phylaTracks).map(Tracked::tracker).toArray(Tracker[]::new);
  }
}
