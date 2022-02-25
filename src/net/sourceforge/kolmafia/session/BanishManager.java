package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BanishManager {
  private static final Set<BanishedMonster> banishedMonsters = new LinkedHashSet<>();

  private BanishManager() {}

  private enum Reset {
    TURN_RESET,
    TURN_ROLLOVER_RESET,
    ROLLOVER_RESET,
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
    CRYSTAL_SKULL("crystal skull", 20, 1, false, Reset.TURN_RESET),
    CURSE_OF_VACATION("curse of vacation", -1, 1, false, Reset.ROLLOVER_RESET),
    DEATHCHUCKS("deathchucks", -1, 1, true, Reset.ROLLOVER_RESET),
    DIRTY_STINKBOMB("dirty stinkbomb", -1, 1, true, Reset.ROLLOVER_RESET),
    DIVINE_CHAMPAGNE_POPPER("divine champagne popper", 5, 1, true, Reset.TURN_RESET),
    FEEL_HATRED("Feel Hatred", 50, 1, true, Reset.TURN_ROLLOVER_RESET),
    GINGERBREAD_RESTRAINING_ORDER(
        "gingerbread restraining order", -1, 1, false, Reset.ROLLOVER_RESET),
    HAROLDS_BELL("harold's bell", 20, 1, false, Reset.TURN_RESET),
    HOWL_OF_THE_ALPHA("howl of the alpha", -1, 3, false, Reset.AVATAR_RESET),
    HUMAN_MUSK("human musk", -1, 1, true, Reset.ROLLOVER_RESET),
    ICE_HOTEL_BELL("ice hotel bell", -1, 1, true, Reset.ROLLOVER_RESET),
    ICE_HOUSE("ice house", -1, 1, false, Reset.NEVER_RESET),
    KGB_TRANQUILIZER_DART("KGB tranquilizer dart", 20, 1, true, Reset.TURN_ROLLOVER_RESET),
    LICORICE_ROPE("licorice rope", -1, 1, false, Reset.ROLLOVER_RESET),
    LOUDER_THAN_BOMB("louder than bomb", 20, 1, true, Reset.TURN_ROLLOVER_RESET),
    MAFIA_MIDDLEFINGER_RING("mafia middle finger ring", 60, 1, true, Reset.TURN_ROLLOVER_RESET),
    NANORHINO("nanorhino", -1, 1, false, Reset.ROLLOVER_RESET),
    PANTSGIVING("pantsgiving", 30, 1, false, Reset.TURN_ROLLOVER_RESET),
    PEEL_OUT("peel out", -1, 1, true, Reset.AVATAR_RESET),
    PULLED_INDIGO_TAFFY("pulled indigo taffy", 40, 1, true, Reset.TURN_RESET),
    REFLEX_HAMMER("Reflex Hammer", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    SABER_FORCE("Saber Force", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    SHOW_YOUR_BORING_FAMILIAR_PICTURES(
        "Show your boring familiar pictures", 100, 1, true, Reset.TURN_RESET),
    SMOKE_GRENADE("smoke grenade", 20, 1, false, Reset.TURN_RESET),
    SNOKEBOMB("snokebomb", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    SPOOKY_MUSIC_BOX_MECHANISM("spooky music box mechanism", -1, 1, false, Reset.ROLLOVER_RESET),
    SPRING_LOADED_FRONT_BUMPER(
        "Spring-Loaded Front Bumper", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    STAFF_OF_THE_STANDALONE_CHEESE(
        "staff of the standalone cheese", -1, 5, false, Reset.AVATAR_RESET),
    STINKY_CHEESE_EYE("stinky cheese eye", 10, 1, true, Reset.TURN_RESET),
    TENNIS_BALL("tennis ball", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    THROW_LATTE_ON_OPPONENT("Throw Latte on Opponent", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
    THUNDER_CLAP("thunder clap", 40, 1, false, Reset.TURN_RESET),
    TRYPTOPHAN_DART("tryptophan dart", -1, 1, false, Reset.ROLLOVER_RESET),
    ULTRA_HAMMER("Ultra Hammer", -1, 1, false, Reset.ROLLOVER_RESET),
    V_FOR_VIVALA_MASK("v for vivala mask", 10, 1, true, Reset.TURN_RESET),
    WALK_AWAY_FROM_EXPLOSION("walk away from explosion", 30, 1, false, Reset.TURN_RESET);

    final String name;
    final int duration;
    final int queueSize;
    final boolean isTurnFree;
    final Reset resetType;

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
      this.name = name;
      this.duration = duration;
      this.queueSize = queueSize;
      this.isTurnFree = isTurnFree;
      this.resetType = resetType;
    }

    public final String getName() {
      return this.name;
    }

    public final int getDuration() {
      // returns actual duration of banish after the turn used, which varies depending if that turn
      // is free
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
  }

  private static class BanishedMonster {
    private final String monsterName;
    private final Banisher banisher;
    private final int turnBanished;

    public BanishedMonster(
        final String monsterName, final Banisher banisher, final int turnBanished) {
      this.monsterName = monsterName;
      this.banisher = banisher;
      this.turnBanished = turnBanished;
    }

    public final String getMonsterName() {
      return this.monsterName;
    }

    public final Banisher getBanisher() {
      return this.banisher;
    }

    public final int getTurnBanished() {
      return this.turnBanished;
    }

    public final Integer turnsLeft() {
      return (turnBanished + banisher.getDuration()) - KoLCharacter.getCurrentRun();
    }

    public final boolean isValid() {
      int duration = banisher.getDuration();
      switch (banisher.getResetType()) {
        case TURN_RESET:
        case TURN_ROLLOVER_RESET:
          return turnsLeft() >= 0;
        case COSMIC_BOWLING_BALL_RESET:
          return Preferences.getInteger("cosmicBowlingBallReturnCombats") > 0;
        default:
          return true;
      }
    }

    public final String getDescription() {
      int duration = banisher.getDuration();

      switch (banisher.getResetType()) {
        case TURN_RESET:
          return turnsLeft().toString();
        case ROLLOVER_RESET:
          return "Until Rollover";
        case TURN_ROLLOVER_RESET:
          return turnsLeft() + " or Until Rollover";
        case AVATAR_RESET:
          return "Until Prism Break";
        case NEVER_RESET:
          return "Until Ice House opened";
        case COSMIC_BOWLING_BALL_RESET:
          return "Until Ball returns ("
              + Preferences.getInteger("cosmicBowlingBallReturnCombats")
              + " combats) or Until Rollover";
        default:
          return "";
      }
    }
  }

  public static final void clearCache() {
    BanishManager.banishedMonsters.clear();
  }

  public static final void loadBanishedMonsters() {
    BanishManager.banishedMonsters.clear();

    String banishes = Preferences.getString("banishedMonsters");
    if (banishes.length() == 0) {
      return;
    }

    StringTokenizer tokens = new StringTokenizer(banishes, ":");

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

      BanishManager.addBanishedMonster(monsterName, banisher, turnBanished);
    }
  }

  private static final void saveBanishedMonsters() {
    Preferences.setString(
        "banishedMonsters",
        banishedMonsters.stream()
            .flatMap(
                m -> Stream.of(m.getMonsterName(), m.getBanisher().getName(), m.getTurnBanished()))
            .map(Object::toString)
            .collect(Collectors.joining(":")));
  }

  /**
   * Iterate through all banished monsters removing entries if the supplied predicate matches.
   *
   * @param predicate Predicate dictating removal
   */
  private static final void resetIf(Predicate<BanishedMonster> predicate) {
    BanishManager.banishedMonsters.removeIf(predicate);
    BanishManager.saveBanishedMonsters();
  }

  /**
   * Iterate through all banished monsters removing entries if the supplied predicate matches their
   * Reset type.
   *
   * @param predicate Predicate dictating removal
   */
  private static final void resetIfType(Predicate<Reset> predicate) {
    resetIf(m -> predicate.test(m.getBanisher().getResetType()));
  }

  public static final void resetRollover() {
    resetIfType(Reset::isRolloverReset);
  }

  public static final void resetAvatar() {
    resetIfType(r -> r == Reset.AVATAR_RESET);
  }

  public static final void resetAscension() {
    resetIfType(r -> r != Reset.NEVER_RESET);
  }

  public static final void resetCosmicBowlingBall() {
    resetIfType(r -> r == Reset.COSMIC_BOWLING_BALL_RESET);
  }

  public static void recalculate() {
    resetIf(Predicate.not(BanishedMonster::isValid));
  }

  public static final void banishCurrentMonster(final Banisher banisher) {
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    if (monster == null) {
      return;
    }
    BanishManager.banishMonster(monster, banisher);
  }

  public static final void banishMonster(final String monsterName, final Banisher banisher) {
    MonsterData monster = MonsterDatabase.findMonster(monsterName, false, false);

    if (monster == null) {
      KoLmafia.updateDisplay("Couldn't find monster by the name " + monsterName + ".");
      return;
    }

    banishMonster(monster, banisher);
  }

  public static final void banishMonster(final MonsterData monster, final Banisher banisher) {
    int queueSize = banisher.getQueueSize();

    if (BanishManager.countBanishes(banisher) >= queueSize) {
      // If we've rebanished a monster that wasn't going to run out anyway, there's nothing to do.
      if (queueSize == 1
          && monster.getName().equals(getBanishedMonster(banisher))
          && !banisher.getResetType().isTurnReset()) {
        return;
      }

      BanishManager.removeOldestBanish(banisher);
    }

    // Banishes fail in some areas, monsters in them cannot be banished

    if (monster.isNoBanish()) {
      KoLmafia.updateDisplay(
          "Banish of "
              + monster.getName()
              + " by "
              + banisher.getName()
              + " failed, as monsters from this area cannot be banished.");
      return;
    }

    KoLmafia.updateDisplay(monster.getName() + " banished by " + banisher.getName() + ".");

    int turnCost = banisher.isTurnFree() ? 0 : 1;
    BanishManager.addBanishedMonster(
        monster.getName(), banisher, KoLCharacter.getCurrentRun() + turnCost);

    BanishManager.recalculate();

    // Legacy support
    switch (banisher) {
      case NANORHINO:
        Preferences.setString("_nanorhinoBanishedMonster", monster.getName());
        break;
      case BANISHING_SHOUT:
      case HOWL_OF_THE_ALPHA:
        {
          Preferences.setString(
              "banishingShoutMonsters",
              Stream.concat(
                      Stream.of(monster.getName()),
                      Arrays.stream(Preferences.getString("banishingShoutMonsters").split("\\|"))
                          .limit(2)
                          .filter(Predicate.not(String::isEmpty)))
                  .collect(Collectors.joining("|")));
          break;
        }
      case STAFF_OF_THE_STANDALONE_CHEESE:
        {
          Preferences.setString(
              "_jiggleCheesedMonsters",
              Stream.concat(
                      Stream.of(monster.getName()),
                      Arrays.stream(Preferences.getString("_jiggleCheesedMonsters").split("\\|"))
                          .filter(Predicate.not(String::isEmpty)))
                  .collect(Collectors.joining("|")));
          break;
        }
    }
  }

  private static boolean addBanishedMonster(
      final String monsterName, final Banisher banisher, final int turnBanished) {
    var banishedMonster = new BanishedMonster(monsterName, banisher, turnBanished);
    if (!banishedMonster.isValid()) {
      return false;
    }

    banishedMonsters.add(banishedMonster);
    return true;
  }

  public static final void removeBanishByBanisher(final Banisher banisher) {
    resetIf(m -> m.getBanisher().equals(banisher));
  }

  public static final void removeBanishByMonster(final String monster) {
    resetIf(m -> m.getMonsterName().equals(monster));
  }

  private static final void removeOldestBanish(final Banisher banisher) {
    banishedMonsters.stream()
        .filter(b -> b.getBanisher().equals(banisher))
        .min(Comparator.comparingInt(BanishedMonster::getTurnBanished))
        .map(BanishedMonster::getMonsterName)
        .ifPresent(BanishManager::removeBanishByMonster);
  }

  public static final boolean isBanished(final String monster) {
    BanishManager.recalculate();

    boolean iceHouseEffective = StandardRequest.isAllowed("Items", "ice house");

    return banishedMonsters.stream()
        .anyMatch(
            m -> {
              if (!m.getMonsterName().equalsIgnoreCase(monster)) return false;
              return (!m.getBanisher().equals(Banisher.ICE_HOUSE) || iceHouseEffective);
            });
  }

  private static int countBanishes(final Banisher banisher) {
    return (int) banishedMonsters.stream().filter(m -> m.getBanisher().equals(banisher)).count();
  }

  public static final List<String> getBanishedMonsters() {
    BanishManager.recalculate();

    return banishedMonsters.stream()
        .map(BanishedMonster::getMonsterName)
        .collect(Collectors.toList());
  }

  public static final List<String> getBanishedMonsters(Banisher banisher) {
    BanishManager.recalculate();

    return banishedMonsters.stream()
        .filter(m -> m.getBanisher().equals(banisher))
        .map(BanishedMonster::getMonsterName)
        .collect(Collectors.toList());
  }

  public static final String getBanishedMonster(Banisher banisher) {
    var monsters = getBanishedMonsters(banisher);
    return (monsters.size() > 0) ? monsters.get(0) : null;
  }

  public static final String[][] getBanishData() {
    BanishManager.recalculate();

    return banishedMonsters.stream()
        .map(
            b ->
                new String[] {
                  b.getMonsterName(),
                  b.getBanisher().getName(),
                  String.valueOf(b.getTurnBanished()),
                  b.getDescription()
                })
        .toArray(String[][]::new);
  }
}
