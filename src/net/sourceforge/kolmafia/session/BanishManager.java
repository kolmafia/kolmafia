package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BanishManager {
  private static final ArrayList<BanishedMonster> banishedMonsters =
      new ArrayList<BanishedMonster>();

  private BanishManager() {}

  private enum Reset {
    TURN_RESET,
    TURN_ROLLOVER_RESET,
    ROLLOVER_RESET,
    AVATAR_RESET,
    NEVER_RESET,
  }

  private static class Banisher {
    final String name;
    final int duration;
    final int queueSize;
    final boolean isTurnFree;
    final Reset resetType;

    public Banisher(
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

  // Format is name of banisher, duration of banisher, how many monsters can be banished at once
  // from this source,
  // whether banish is turn free, type of reset.
  private static final Banisher[] BANISHER =
      new Banisher[] {
        new Banisher("Be a Mind Master", 80, 1, true, Reset.TURN_RESET),
        new Banisher("Feel Hatred", 50, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("Show your boring familiar pictures", 100, 1, true, Reset.TURN_RESET),
        new Banisher("KGB tranquilizer dart", 20, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("Reflex Hammer", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("Saber Force", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("Spring-Loaded Front Bumper", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("Throw Latte on Opponent", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("Ultra Hammer", -1, 1, false, Reset.ROLLOVER_RESET),
        new Banisher("B. L. A. R. T. Spray (wide)", -1, 1, true, Reset.ROLLOVER_RESET),
        new Banisher("baleful howl", -1, 1, true, Reset.ROLLOVER_RESET),
        new Banisher("banishing shout", -1, 3, false, Reset.AVATAR_RESET),
        new Banisher("batter up!", -1, 1, false, Reset.ROLLOVER_RESET),
        new Banisher("beancannon", -1, 5, false, Reset.ROLLOVER_RESET),
        new Banisher("breathe out", 20, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("bundle of &quot;fragrant&quot; herbs", -1, 3, true, Reset.ROLLOVER_RESET),
        new Banisher("chatterboxing", 20, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("classy monkey", 20, 1, false, Reset.TURN_RESET),
        new Banisher("cocktail napkin", 20, 1, true, Reset.TURN_RESET),
        new Banisher("crystal skull", 20, 1, false, Reset.TURN_RESET),
        new Banisher("curse of vacation", -1, 1, false, Reset.ROLLOVER_RESET),
        new Banisher("deathchucks", -1, 1, true, Reset.ROLLOVER_RESET),
        new Banisher("dirty stinkbomb", -1, 1, true, Reset.ROLLOVER_RESET),
        new Banisher("divine champagne popper", 5, 1, true, Reset.TURN_RESET),
        new Banisher("gingerbread restraining order", -1, 1, false, Reset.ROLLOVER_RESET),
        new Banisher("harold's bell", 20, 1, false, Reset.TURN_RESET),
        new Banisher("howl of the alpha", -1, 3, false, Reset.AVATAR_RESET),
        new Banisher("human musk", -1, 1, true, Reset.ROLLOVER_RESET),
        new Banisher("ice hotel bell", -1, 1, true, Reset.ROLLOVER_RESET),
        new Banisher("ice house", -1, 1, false, Reset.NEVER_RESET),
        new Banisher("licorice rope", -1, 1, false, Reset.ROLLOVER_RESET),
        new Banisher("louder than bomb", 20, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("mafia middle finger ring", 60, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("nanorhino", -1, 1, false, Reset.ROLLOVER_RESET),
        new Banisher("pantsgiving", 30, 1, false, Reset.TURN_ROLLOVER_RESET),
        new Banisher("peel out", -1, 1, true, Reset.AVATAR_RESET),
        new Banisher("pulled indigo taffy", 40, 1, true, Reset.TURN_RESET),
        new Banisher("smoke grenade", 20, 1, false, Reset.TURN_RESET),
        new Banisher("snokebomb", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("spooky music box mechanism", -1, 1, false, Reset.ROLLOVER_RESET),
        new Banisher("staff of the standalone cheese", -1, 5, false, Reset.AVATAR_RESET),
        new Banisher("stinky cheese eye", 10, 1, true, Reset.TURN_RESET),
        new Banisher("tennis ball", 30, 1, true, Reset.TURN_ROLLOVER_RESET),
        new Banisher("thunder clap", 40, 1, false, Reset.TURN_RESET),
        new Banisher("tryptophan dart", -1, 1, false, Reset.ROLLOVER_RESET),
        new Banisher("v for vivala mask", 10, 1, true, Reset.TURN_RESET),
        new Banisher("walk away from explosion", 30, 1, false, Reset.TURN_RESET),
      };

  private static class BanishedMonster {
    final String monsterName;
    final String banishName;
    final int turnBanished;

    public BanishedMonster(
        final String monsterName, final String banishName, final int turnBanished) {
      this.monsterName = monsterName;
      this.banishName = banishName;
      this.turnBanished = turnBanished;
    }

    public final String getMonsterName() {
      return this.monsterName;
    }

    public final String getBanishName() {
      return this.banishName;
    }

    public final int getTurnBanished() {
      return this.turnBanished;
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
      String banishName = tokens.nextToken();
      if (!tokens.hasMoreTokens()) break;
      int turnBanished = StringUtilities.parseInt(tokens.nextToken());
      int banishDuration = BanishManager.findBanisher(banishName).getDuration();
      Reset resetType = BanishManager.findBanisher(banishName).getResetType();
      if ((resetType != Reset.TURN_RESET && resetType != Reset.TURN_ROLLOVER_RESET)
          || (turnBanished + banishDuration >= KoLCharacter.getCurrentRun())) {
        BanishManager.addBanishedMonster(monsterName, banishName, turnBanished);
      }
    }
  }

  public static final void saveBanishedMonsters() {
    BanishManager.recalculate();

    StringBuilder banishString = new StringBuilder();
    Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

    while (it.hasNext()) {
      BanishedMonster current = it.next();

      if (banishString.length() > 0) {
        banishString.append(":");
      }

      banishString.append(current.monsterName);
      banishString.append(":");
      banishString.append(current.banishName);
      banishString.append(":");
      banishString.append(current.turnBanished);
    }

    Preferences.setString("banishedMonsters", banishString.toString());
  }

  public static final void resetRollover() {
    Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

    while (it.hasNext()) {
      BanishedMonster current = it.next();

      Reset type = BanishManager.findBanisher(current.getBanishName()).getResetType();
      if (type == Reset.ROLLOVER_RESET || type == Reset.TURN_ROLLOVER_RESET) {
        it.remove();
      }
    }

    BanishManager.saveBanishedMonsters();
  }

  public static final void resetAvatar() {

    BanishManager.banishedMonsters.removeIf(
        current ->
            BanishManager.findBanisher(current.getBanishName()).getResetType()
                == Reset.AVATAR_RESET);

    BanishManager.saveBanishedMonsters();
  }

  public static final void resetAscension() {

    BanishManager.banishedMonsters.removeIf(
        current ->
            BanishManager.findBanisher(current.getBanishName()).getResetType()
                != Reset.NEVER_RESET);

    BanishManager.saveBanishedMonsters();
  }

  public static final void update() {
    BanishManager.recalculate();
    BanishManager.saveBanishedMonsters();
  }

  private static void recalculate() {
    Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

    while (it.hasNext()) {
      BanishedMonster current = it.next();
      int banisherDuration = BanishManager.findBanisher(current.getBanishName()).getDuration();
      Reset resetType = BanishManager.findBanisher(current.getBanishName()).getResetType();
      if ((resetType == Reset.TURN_RESET || resetType == Reset.TURN_ROLLOVER_RESET)
          && (current.getTurnBanished() + banisherDuration <= KoLCharacter.getCurrentRun())) {
        it.remove();
      }
    }
  }

  public static final Banisher findBanisher(final String banisher) {
    for (Banisher ban : BANISHER) {
      if (ban.getName().equals(banisher)) {
        return ban;
      }
    }
    return null;
  }

  public static final void banishCurrentMonster(final String banishName) {
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    if (monster == null) {
      return;
    }
    BanishManager.banishMonster(monster.getName(), banishName);
  }

  public static final void banishMonster(final String monsterName, final String banishName) {
    if (BanishManager.countBanishes(banishName)
        >= BanishManager.findBanisher(banishName).getQueueSize()) {
      BanishManager.removeOldestBanish(banishName);
    }
    // Banishes fail in some areas, monsters in them cannot be banished
    MonsterData monster = MonsterDatabase.findMonster(monsterName);
    if (monster != null && monster.isNoBanish()) {
      KoLmafia.updateDisplay(
          "Banish of "
              + monsterName
              + " by "
              + banishName
              + " failed, as monsters from this area cannot be banished.");
      return;
    }
    KoLmafia.updateDisplay(monsterName + " banished by " + banishName + ".");
    int turnCost = BanishManager.findBanisher(banishName).isTurnFree() ? 0 : 1;
    BanishManager.addBanishedMonster(
        monsterName, banishName, KoLCharacter.getCurrentRun() + turnCost);
    BanishManager.saveBanishedMonsters();

    // Legacy support
    if (banishName.equals("nanorhino")) {
      Preferences.setString("_nanorhinoBanishedMonster", monsterName);
    } else if (banishName.equals("banishing shout") || banishName.equals("howl of the alpha")) {
      String pref = monsterName;
      String[] monsters = Preferences.getString("banishingShoutMonsters").split("\\|");
      for (int i = 0; i < monsters.length && i < 2; ++i) {
        if (monsters[i].length() > 0) {
          pref += "|" + monsters[i];
        }
      }
      Preferences.setString("banishingShoutMonsters", pref);
    } else if (banishName.equals("staff of the standalone cheese")) {
      String pref = monsterName;
      String[] monsters = Preferences.getString("_jiggleCheesedMonsters").split("\\|");
      for (int i = 0; i < monsters.length; ++i) {
        if (monsters[i].length() > 0) {
          pref += "|" + monsters[i];
        }
      }
      Preferences.setString("_jiggleCheesedMonsters", pref);
    }
  }

  private static void addBanishedMonster(
      final String monsterName, final String banishName, final int turnBanished) {
    BanishedMonster newBanishedMonster = new BanishedMonster(monsterName, banishName, turnBanished);
    if (!BanishManager.banishedMonsters.contains(newBanishedMonster)) {
      BanishManager.banishedMonsters.add(newBanishedMonster);
    }
  }

  public static final void removeBanishByBanisher(final String banisher) {

    BanishManager.banishedMonsters.removeIf(current -> current.getBanishName().equals(banisher));

    BanishManager.saveBanishedMonsters();
  }

  public static final void removeBanishByMonster(final String monster) {

    BanishManager.banishedMonsters.removeIf(current -> current.getMonsterName().equals(monster));

    BanishManager.saveBanishedMonsters();
  }

  public static final void removeOldestBanish(final String banisher) {
    Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();
    String target = null;
    int earliest = -1;

    while (it.hasNext()) {
      BanishedMonster current = it.next();
      if (current.getBanishName().equals(banisher)) {
        if (earliest == -1 || current.getTurnBanished() < earliest) {
          target = current.getMonsterName();
          earliest = current.getTurnBanished();
        }
      }
    }

    if (target != null) {
      BanishManager.removeBanishByMonster(target);
    }
  }

  public static final boolean isBanished(final String monster) {
    BanishManager.recalculate();

    Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

    while (it.hasNext()) {
      BanishedMonster current = it.next();
      if (current.getMonsterName().equalsIgnoreCase(monster)) {
        if (current.getBanishName().equals("ice house")
            && !StandardRequest.isAllowed("Items", "ice house")) {
          continue;
        }
        return true;
      }
    }
    return false;
  }

  private static int countBanishes(final String banisher) {
    int banishCount = 0;
    Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

    while (it.hasNext()) {
      BanishedMonster current = it.next();
      if (current.getBanishName().equals(banisher)) {
        banishCount++;
      }
    }
    return banishCount;
  }

  public static final String getBanishList() {
    BanishManager.recalculate();

    StringBuilder banishList = new StringBuilder();
    Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

    while (it.hasNext()) {
      BanishedMonster current = it.next();

      if (banishList.length() > 0) {
        banishList.append(",");
      }

      banishList.append(current.monsterName);
    }

    return banishList.toString();
  }

  public static final String getIceHouseMonster() {
    BanishManager.recalculate();

    Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

    while (it.hasNext()) {
      BanishedMonster current = it.next();

      if (current.banishName.equals("ice house")) {
        return current.monsterName;
      }
    }

    return null;
  }

  public static final String[][] getBanishData() {
    BanishManager.recalculate();

    Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

    int banish = 0;
    int count = BanishManager.banishedMonsters.size();

    if (count > 0) {
      String[][] banishData = new String[count][4];
      while (it.hasNext()) {
        BanishedMonster current = it.next();
        banishData[banish][0] = current.monsterName;
        banishData[banish][1] = current.banishName;
        banishData[banish][2] = String.valueOf(current.turnBanished);
        int banisherDuration = BanishManager.findBanisher(current.banishName).getDuration();
        Reset resetType = BanishManager.findBanisher(current.banishName).getResetType();
        if (resetType == Reset.TURN_RESET) {
          banishData[banish][3] =
              String.valueOf(
                  current.turnBanished + banisherDuration - KoLCharacter.getCurrentRun());
        } else if (resetType == Reset.ROLLOVER_RESET) {
          banishData[banish][3] = "Until Rollover";
        } else if (resetType == Reset.TURN_ROLLOVER_RESET) {
          banishData[banish][3] =
              (current.turnBanished + banisherDuration - KoLCharacter.getCurrentRun())
                  + " or Until Rollover";
        } else if (resetType == Reset.AVATAR_RESET) {
          banishData[banish][3] = "Until Prism Break";
        } else if (resetType == Reset.NEVER_RESET) {
          banishData[banish][3] = "Until Ice House opened";
        }
        banish++;
      }
      return banishData;
    }

    return null;
  }
}
