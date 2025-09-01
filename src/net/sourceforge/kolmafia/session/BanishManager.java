package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

@SuppressWarnings("incomplete-switch")
public class BanishManager {
  private BanishManager() {}

  private enum BanishType {
    MONSTER,
    PHYLUM
  }

  private enum Reset {
    TURN_RESET,
    TURN_ROLLOVER_RESET,
    ROLLOVER_RESET,
    EFFECT_RESET,
    AVATAR_RESET,
    NEVER_RESET,
    COSMIC_BOWLING_BALL_RESET;

    private Boolean rolloverReset;
    private Boolean turnReset;

    final boolean isRolloverReset() {
      if (rolloverReset == null) {
        this.rolloverReset =
            this == TURN_ROLLOVER_RESET
                || this == ROLLOVER_RESET
                || this == COSMIC_BOWLING_BALL_RESET;
      }

      return rolloverReset;
    }

    final boolean isTurnReset() {
      if (turnReset == null) {
        this.turnReset = this == TURN_RESET || this == TURN_ROLLOVER_RESET;
      }

      return turnReset;
    }
  }

  public enum Banisher {
    ANCHOR_BOMB("anchor bomb", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    BALEFUL_HOWL("baleful howl", -1, 1, true, Reset.ROLLOVER_RESET),
    BANISHING_SHOUT("banishing shout", -1, 3, false, Reset.AVATAR_RESET),
    BATTER_UP("batter up!", -1, 1, false, Reset.ROLLOVER_RESET),
    BEANCANNON("beancannon", -1, 5, false, Reset.ROLLOVER_RESET),
    BE_A_MIND_MASTER("Be a Mind Master", 80, 1, true, Reset.TURN_RESET),
    BLART_SPRAY_WIDE("B. L. A. R. T. Spray (wide)", -1, 1, true, Reset.ROLLOVER_RESET),
    BOWL_A_CURVEBALL("Bowl a Curveball", -1, 1, true, Reset.COSMIC_BOWLING_BALL_RESET),
    BREATHE_OUT("breathe out", 20, 1, true, Reset.TURN_ROLLOVER_RESET),
    BUNDLE_OF_FRAGRANT_HERBS(
        "bundle of &quot;fragrant&quot; herbs", -1, 3, true, Reset.ROLLOVER_RESET),
    CHATTERBOXING("chatterboxing", 20, 1, true, Reset.TURN_ROLLOVER_RESET),
    CLASSY_MONKEY("classy monkey", 20, 1, false, Reset.TURN_RESET),
    COCKTAIL_NAPKIN("cocktail napkin", 20, 1, true, Reset.TURN_RESET),
    CRIMBUCCANEER_RIGGING_LASSO(
        "Crimbuccaneer rigging lasso", 100, 1, false, Reset.TURN_ROLLOVER_RESET),
    CRYSTAL_SKULL("crystal skull", 20, 1, false, Reset.TURN_RESET),
    CURSE_OF_VACATION("curse of vacation", -1, 1, false, Reset.ROLLOVER_RESET),
    DEATHCHUCKS("deathchucks", -1, 1, true, Reset.ROLLOVER_RESET),
    DIRTY_STINKBOMB("dirty stinkbomb", -1, 1, true, Reset.ROLLOVER_RESET),
    DIVINE_CHAMPAGNE_POPPER("divine champagne popper", 5, 1, true, Reset.TURN_RESET),
    FEEL_HATRED("Feel Hatred", 50, 1, true, Reset.TURN_ROLLOVER_RESET),
    GINGERBREAD_RESTRAINING_ORDER(
        "gingerbread restraining order", -1, 1, false, Reset.ROLLOVER_RESET),
    GLITCHED_MALWARE("Deploy Glitched Malware", -1, 1, false, Reset.ROLLOVER_RESET),
    HAROLDS_BELL("harold's bell", 20, 1, false, Reset.TURN_RESET),
    HOWL_OF_THE_ALPHA("howl of the alpha", -1, 3, false, Reset.AVATAR_RESET),
    HUMAN_MUSK("human musk", -1, 1, true, Reset.ROLLOVER_RESET),
    ICE_HOTEL_BELL("ice hotel bell", -1, 1, true, Reset.ROLLOVER_RESET),
    ICE_HOUSE("ice house", -1, 1, false, Reset.NEVER_RESET),
    KGB_TRANQUILIZER_DART("KGB tranquilizer dart", 20, 1, true, Reset.TURN_ROLLOVER_RESET),
    LICORICE_ROPE("licorice rope", -1, 1, false, Reset.ROLLOVER_RESET),
    LOUDER_THAN_BOMB("louder than bomb", 20, 1, true, Reset.TURN_ROLLOVER_RESET),
    MAFIA_MIDDLEFINGER_RING("mafia middle finger ring", 60, 1, true, Reset.TURN_ROLLOVER_RESET),
    MONKEY_SLAP("Monkey Slap", -1, 1, false, Reset.ROLLOVER_RESET),
    NANORHINO("nanorhino", -1, 1, false, Reset.ROLLOVER_RESET),
    PANTSGIVING("pantsgiving", 30, 1, false, Reset.TURN_ROLLOVER_RESET),
    PEEL_OUT("peel out", -1, 1, true, Reset.AVATAR_RESET),
    PEPPERMINT_BOMB("peppermint bomb", 100, 1, false, Reset.TURN_ROLLOVER_RESET),
    PULLED_INDIGO_TAFFY("pulled indigo taffy", 40, 1, true, Reset.TURN_RESET),
    PUNCH_OUT_YOUR_FOE("Punch Out your Foe", 20, 1, true, Reset.TURN_RESET),
    PUNT_AOSOL("[28021]Punt", -1, 1, false, Reset.ROLLOVER_RESET),
    PUNT_WEREPROF("[7510]Punt", 40, 1, false, Reset.TURN_RESET),
    REFLEX_HAMMER("Reflex Hammer", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    ROAR_LIKE_A_LION("Roar like a Lion", -1, 1, false, Reset.EFFECT_RESET),
    SEADENT_LIGHTNING("Sea *dent", -1, 1, false, Reset.ROLLOVER_RESET),
    SPLIT_PEA_SOUP("handful of split pea soup", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    STUFFED_YAM_STINKBOMB("stuffed yam stinkbomb", 15, 1, true, Reset.TURN_ROLLOVER_RESET),
    PATRIOTIC_SCREECH("Patriotic Screech", 100, 1, false, Reset.TURN_RESET, BanishType.PHYLUM),
    SABER_FORCE("Saber Force", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    SHOW_YOUR_BORING_FAMILIAR_PICTURES(
        "Show your boring familiar pictures", 100, 1, true, Reset.TURN_RESET),
    SMOKE_GRENADE("smoke grenade", 20, 1, false, Reset.TURN_RESET),
    SNOKEBOMB("snokebomb", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    SPOOKY_MUSIC_BOX_MECHANISM("spooky music box mechanism", -1, 1, false, Reset.ROLLOVER_RESET),
    SPRING_KICK("Spring Kick", -1, 1, true, Reset.ROLLOVER_RESET),
    SPRING_LOADED_FRONT_BUMPER(
        "Spring-Loaded Front Bumper", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    STAFF_OF_THE_STANDALONE_CHEESE(
        "staff of the standalone cheese", -1, 5, false, Reset.AVATAR_RESET),
    STINKY_CHEESE_EYE("stinky cheese eye", 10, 1, true, Reset.TURN_RESET),
    SYSTEM_SWEEP("System Sweep", -1, 1, false, Reset.ROLLOVER_RESET),
    TENNIS_BALL("tennis ball", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    THROWIN_EMBER("throwin' ember", 30, 1, false, Reset.TURN_ROLLOVER_RESET),
    THROW_LATTE_ON_OPPONENT("Throw Latte on Opponent", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    THUNDER_CLAP("thunder clap", 40, 1, false, Reset.TURN_RESET),
    TRYPTOPHAN_DART("tryptophan dart", -1, 1, false, Reset.ROLLOVER_RESET),
    ULTRA_HAMMER("Ultra Hammer", -1, 1, false, Reset.ROLLOVER_RESET),
    V_FOR_VIVALA_MASK("v for vivala mask", 10, 1, true, Reset.TURN_RESET),
    WALK_AWAY_FROM_EXPLOSION("walk away from explosion", 30, 1, false, Reset.TURN_RESET),
    // turncount is at most 100, but is given in a combat message, or is derivable from fam tags
    LEFT_ZOOT_KICK("Left %n Kick", 100, 1, true, Reset.TURN_RESET),
    RIGHT_ZOOT_KICK("Right %n Kick", 100, 1, true, Reset.TURN_RESET);

    final String name;
    final int duration;
    final int queueSize;
    final boolean isTurnFree;
    final Reset resetType;
    final BanishType banishType;

    public static Banisher find(final String banisherName) {
      return Arrays.stream(values())
          .filter(b -> b.getName().equalsIgnoreCase(banisherName))
          .findAny()
          .orElse(null);
    }

    Banisher(
        final String name,
        final int duration,
        final int queueSize,
        final boolean isTurnFree,
        final Reset resetType) {
      this(name, duration, queueSize, isTurnFree, resetType, BanishType.MONSTER);
    }

    Banisher(
        final String name,
        final int duration,
        final int queueSize,
        final boolean isTurnFree,
        final Reset resetType,
        final BanishType banishType) {
      this.name = name;
      this.duration = duration;
      this.queueSize = queueSize;
      this.isTurnFree = isTurnFree;
      this.resetType = resetType;
      this.banishType = banishType;
    }

    public final String getName() {
      return this.name;
    }

    public final int getDuration() {
      if (this == LEFT_ZOOT_KICK) {
        return FamiliarDatabase.zootomistBanishDuration(
            Preferences.getInteger("zootGraftedFootLeftFamiliar"));
      } else if (this == RIGHT_ZOOT_KICK) {
        return FamiliarDatabase.zootomistBanishDuration(
            Preferences.getInteger("zootGraftedFootRightFamiliar"));
      }
      // returns actual duration of banish after the turn used, which varies depending on if that
      // turn is free
      int turnCost = this.isTurnFree ? 0 : 1;
      return this.duration - turnCost;
    }

    public final int getQueueSize() {
      return this.queueSize;
    }

    public final boolean isTurnFree() {
      return this.isTurnFree;
    }

    public final Reset getResetType() {
      return this.resetType;
    }

    public final BanishType getBanishType() {
      return this.banishType;
    }

    public final boolean isEffective() {
      if (this == Banisher.ICE_HOUSE) {
        return StandardRequest.isAllowed(RestrictedItemType.ITEMS, "ice house");
      }
      return true;
    }
  }

  private record Banished(String banished, Banisher banisher, int turnBanished) {
    private Integer turnsLeft() {
      return (turnBanished + banisher.getDuration()) - KoLCharacter.getCurrentRun();
    }

    public boolean isValid() {
      return switch (banisher.getResetType()) {
        case TURN_RESET, TURN_ROLLOVER_RESET -> turnsLeft() > 0;
        case COSMIC_BOWLING_BALL_RESET -> Preferences.getInteger("cosmicBowlingBallReturnCombats")
            > 0;
        case EFFECT_RESET -> KoLConstants.activeEffects.contains(
            EffectPool.get(EffectPool.HEAR_ME_ROAR));
        default -> true;
      };
    }

    public String getDescription() {
      return switch (banisher.getResetType()) {
        case TURN_RESET -> turnsLeft().toString();
        case ROLLOVER_RESET -> "Until Rollover";
        case TURN_ROLLOVER_RESET -> turnsLeft() + " or Until Rollover";
        case EFFECT_RESET -> "Until Hear Me Roar expires";
        case AVATAR_RESET -> "Until Prism Break";
        case NEVER_RESET -> "Until Ice House opened";
        case COSMIC_BOWLING_BALL_RESET -> "Until Ball returns ("
            + Preferences.getInteger("cosmicBowlingBallReturnCombats")
            + " combats) or Until Rollover";
      };
    }
  }

  private static Set<Banished> getBanishedSet(Banisher banisher) {
    return switch (banisher.getBanishType()) {
      case MONSTER -> prefToSet("banishedMonsters");
      case PHYLUM -> prefToSet("banishedPhyla");
    };
  }

  private static Set<Banished> prefToSet(String prefName) {
    String banishes = Preferences.getString(prefName);
    if (banishes.isEmpty()) {
      return new LinkedHashSet<>();
    }

    StringTokenizer tokens = new StringTokenizer(banishes, ":");
    var set = new LinkedHashSet<Banished>();

    while (tokens.hasMoreTokens()) {
      String monsterName = tokens.nextToken();
      if (!tokens.hasMoreTokens()) break;
      String banisherName = tokens.nextToken();
      if (!tokens.hasMoreTokens()) break;
      int turnBanished = StringUtilities.parseInt(tokens.nextToken());

      var banisher = Banisher.find(banisherName);

      if (banisher == null) {
        KoLmafia.updateDisplay("Attempted to parse unknown banisher " + banisherName + ".");
        continue;
      }

      var banished = new Banished(monsterName, banisher, turnBanished);
      set.add(banished);
    }
    return set;
  }

  private static String setToPref(Set<Banished> banished) {
    return banished.stream()
        .flatMap(m -> Stream.of(m.banished(), m.banisher().getName(), m.turnBanished()))
        .map(Object::toString)
        .collect(Collectors.joining(":"));
  }

  private static void updatePref(String pref, Consumer<Set<Banished>> func) {
    var set = prefToSet(pref);
    func.accept(set);
    Preferences.setString(pref, setToPref(set));
  }

  /**
   * Iterate through all banished monsters removing entries if the supplied predicate matches.
   *
   * @param predicate Predicate dictating removal
   */
  private static void resetIf(Predicate<Banished> predicate) {
    updatePref("banishedMonsters", m -> m.removeIf(predicate));
    updatePref("banishedPhyla", m -> m.removeIf(predicate));
  }

  /**
   * Iterate through all banished monsters removing entries if the supplied predicate matches their
   * Reset type.
   *
   * @param predicate Predicate dictating removal
   */
  private static void resetIfType(Predicate<Reset> predicate) {
    resetIf(m -> predicate.test(m.banisher().getResetType()));
  }

  public static void resetRollover() {
    resetIfType(Reset::isRolloverReset);
  }

  public static void resetAvatar() {
    resetIfType(r -> r == Reset.AVATAR_RESET);
  }

  public static void resetAscension() {
    resetIfType(r -> r != Reset.NEVER_RESET);
  }

  public static void resetCosmicBowlingBall() {
    resetIfType(r -> r == Reset.COSMIC_BOWLING_BALL_RESET);
  }

  public static void recalculate() {
    resetIf(Predicate.not(Banished::isValid));
  }

  public static void banishCurrentMonster(final Banisher banisher) {
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    if (monster == null) {
      return;
    }
    BanishManager.banishMonster(monster, banisher);
  }

  /**
   * Banish a monster
   *
   * @param monsterName Name of the monster to banish
   * @param banisher Banisher type
   * @param adventureResult Whether this banish is the result of an adventure (vs observed through
   *     some other means)
   */
  public static void banishMonster(
      final String monsterName, final Banisher banisher, final boolean adventureResult) {
    MonsterData monster = MonsterDatabase.findMonster(monsterName, false, false);

    if (monster == null) {
      KoLmafia.updateDisplay("Couldn't find monster by the name " + monsterName + ".");
      return;
    }

    banishMonster(monster, banisher, adventureResult);
  }

  /**
   * Banish a monster
   *
   * @param monster Instance of the monster to banish
   * @param banisher Banisher type
   * @param adventureResult Whether this banish is the result of an adventure (vs observed through
   *     some other means)
   */
  public static void banishMonster(
      final MonsterData monster, final Banisher banisher, final boolean adventureResult) {
    String banished =
        switch (banisher.getBanishType()) {
          case MONSTER -> monster.getName();
          case PHYLUM -> monster.getPhylum().toString();
        };

    int queueSize = banisher.getQueueSize();

    if (BanishManager.countBanishes(banisher) >= queueSize) {
      // If we've rebanished a monster that wasn't going to run out anyway, there's nothing to do.
      if (queueSize == 1
          && banished.equals(getFirstBanished(banisher))
          && !banisher.getResetType().isTurnReset()) {
        return;
      }

      BanishManager.removeOldestBanish(banisher);
    }

    if (banisher == Banisher.LEFT_ZOOT_KICK) {
      BanishManager.removeOldestBanish(Banisher.RIGHT_ZOOT_KICK);
    } else if (banisher == Banisher.RIGHT_ZOOT_KICK) {
      BanishManager.removeOldestBanish(Banisher.LEFT_ZOOT_KICK);
    }

    // Banishes fail in some areas, monsters in them cannot be banished

    if (banisher.getBanishType() == BanishType.MONSTER) {
      if (monster.isNoBanish()) {
        KoLmafia.updateDisplay(
            "Banish of "
                + banished
                + " by "
                + banisher.getName()
                + " failed, as monsters from this area cannot be banished.");
        return;
      }
    }

    KoLmafia.updateDisplay(banished + " banished by " + banisher.getName() + ".");

    // Never-reset banishes are set to turn 0 so that they last the next ascension
    var turnBanished =
        banisher.getResetType() == Reset.NEVER_RESET
            ? 0
            : KoLCharacter.getCurrentRun() + (banisher.isTurnFree() || !adventureResult ? 0 : 1);

    BanishManager.addBanished(banished, banisher, turnBanished);

    BanishManager.recalculate();

    // Legacy support
    switch (banisher) {
      case NANORHINO -> Preferences.setString("_nanorhinoBanishedMonster", banished);
      case BANISHING_SHOUT, HOWL_OF_THE_ALPHA -> Preferences.setString(
          "banishingShoutMonsters",
          Stream.concat(
                  Stream.of(banished),
                  Arrays.stream(Preferences.getString("banishingShoutMonsters").split("\\|"))
                      .limit(2)
                      .filter(Predicate.not(String::isEmpty)))
              .collect(Collectors.joining("|")));
      case STAFF_OF_THE_STANDALONE_CHEESE -> Preferences.setString(
          "_jiggleCheesedMonsters",
          Stream.concat(
                  Stream.of(banished),
                  Arrays.stream(Preferences.getString("_jiggleCheesedMonsters").split("\\|"))
                      .filter(Predicate.not(String::isEmpty)))
              .collect(Collectors.joining("|")));
    }
  }

  public static void banishMonster(final MonsterData monster, final Banisher banisher) {
    banishMonster(monster, banisher, true);
  }

  private static void addBanished(
      final String entry, final Banisher banisher, final int turnBanished) {
    var banished = new Banished(entry, banisher, turnBanished);
    if (!banished.isValid()) {
      KoLmafia.updateDisplay(
          "Banish of "
              + entry
              + " by "
              + banisher.getName()
              + " failed, as the banish was invalid.");
      return;
    }

    var pref =
        switch (banisher.getBanishType()) {
          case MONSTER -> "banishedMonsters";
          case PHYLUM -> "banishedPhyla";
        };
    updatePref(pref, x -> x.add(banished));
  }

  public static void removeBanishByBanisher(final Banisher banisher) {
    resetIf(m -> m.banisher() == banisher);
  }

  private static void removeOldestBanish(final Banisher banisher) {
    var monsters = prefToSet("banishedMonsters");
    var phyla = prefToSet("banishedPhyla");

    Stream.concat(monsters.stream(), phyla.stream())
        .filter(b -> b.banisher() == banisher)
        .min(Comparator.comparingInt(Banished::turnBanished))
        .ifPresent(b -> resetIf(m -> m.equals(b)));
  }

  public static boolean isBanished(final String monster) {
    BanishManager.recalculate();

    var monsters = prefToSet("banishedMonsters");

    if (monsters.stream()
        .filter(m -> m.banisher().isEffective())
        .anyMatch(m -> m.banished().equalsIgnoreCase(monster))) {
      return true;
    }

    MonsterData data = MonsterDatabase.findMonster(monster, false, false);
    if (data == null) {
      return false;
    }
    if (data.isNoBanish()) {
      return false;
    }
    return isBanishedPhylum(data.getPhylum());
  }

  public static boolean isBanishedPhylum(final Phylum phylum) {
    var phyla = prefToSet("banishedPhyla");

    return phyla.stream()
        .filter(m -> m.banisher().isEffective())
        .anyMatch(m -> m.banished().equalsIgnoreCase(phylum.toString()));
  }

  public static Banisher[] banishedBy(final MonsterData data) {
    if (data == null) {
      return new Banisher[0];
    }
    if (data.isNoBanish()) {
      return new Banisher[0];
    }

    BanishManager.recalculate();

    var monsters = prefToSet("banishedMonsters");
    var phyla = prefToSet("banishedPhyla");

    var monsterBanishes =
        monsters.stream()
            .filter(m -> m.banisher().isEffective())
            .filter(m -> m.banished().equalsIgnoreCase(data.getName()));
    var phylaBanishes =
        phyla.stream()
            .filter(m -> m.banisher().isEffective())
            .filter(m -> m.banished().equalsIgnoreCase(data.getPhylum().toString()));
    return Stream.concat(monsterBanishes, phylaBanishes)
        .map(Banished::banisher)
        .toArray(Banisher[]::new);
  }

  private static int countBanishes(final Banisher banisher) {
    Set<Banished> banished = getBanishedSet(banisher);
    return (int) banished.stream().filter(m -> m.banisher() == banisher).count();
  }

  public static List<String> getBanishedMonsters() {
    return getBanishedMonsters(true);
  }

  public static List<String> getBanishedMonsters(final boolean recalculate) {
    if (recalculate) {
      BanishManager.recalculate();
    }

    var monsters = prefToSet("banishedMonsters");

    return monsters.stream().map(Banished::banished).collect(Collectors.toList());
  }

  public static List<String> getBanishedPhyla() {
    BanishManager.recalculate();

    var phyla = prefToSet("banishedPhyla");

    return phyla.stream().map(Banished::banished).collect(Collectors.toList());
  }

  public static List<String> getBanished(Banisher banisher) {
    BanishManager.recalculate();

    Set<Banished> banished = getBanishedSet(banisher);

    return banished.stream()
        .filter(m -> m.banisher() == banisher)
        .map(Banished::banished)
        .collect(Collectors.toList());
  }

  public static String getFirstBanished(Banisher banisher) {
    var banished = getBanished(banisher);
    return !banished.isEmpty() ? banished.get(0) : null;
  }

  public static String[][] getBanishedMonsterData() {
    return getBanishedData(prefToSet("banishedMonsters"));
  }

  public static String[][] getBanishedPhylaData() {
    return getBanishedData(prefToSet("banishedPhyla"));
  }

  private static String[][] getBanishedData(Set<Banished> banished) {
    BanishManager.recalculate();

    return banished.stream()
        .map(
            b ->
                new String[] {
                  b.banished(),
                  b.banisher().getName(),
                  String.valueOf(b.turnBanished()),
                  b.getDescription()
                })
        .toArray(String[][]::new);
  }
}
