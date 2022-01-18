package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Modifiers.Modifier;
import net.sourceforge.kolmafia.Modifiers.ModifierList;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.PastaThrallData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.MallSearchFrame;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.textui.command.SnowsuitCommand;
import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CharPaneRequest extends GenericRequest {
  private static final AdventureResult ABSINTHE = EffectPool.get(EffectPool.ABSINTHE);
  private static final AdventureResult CHILLED_TO_THE_BONE =
      EffectPool.get(EffectPool.CHILLED_TO_THE_BONE);

  private static long lastResponseTimestamp = 0;
  private static String lastResponse = "";

  private static boolean canInteract = false;
  private static int turnsThisRun = 0;

  private static boolean inValhalla = false;

  public static boolean compactCharacterPane = false;
  public static boolean familiarBelowEffects = false;

  private static final HtmlCleaner cleaner = HTMLParserUtils.configureDefaultParser();

  public CharPaneRequest() {
    super("charpane.php");
  }

  public static final void reset() {
    CharPaneRequest.lastResponseTimestamp = 0;
    CharPaneRequest.lastResponse = "";
    CharPaneRequest.canInteract = false;
    CharPaneRequest.turnsThisRun = 0;
    CharPaneRequest.inValhalla = false;
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public String getHashField() {
    return null;
  }

  public static final boolean canInteract() {
    return CharPaneRequest.canInteract;
  }

  public static final boolean inValhalla() {
    return CharPaneRequest.inValhalla;
  }

  public static final void setInValhalla(final boolean inValhalla) {
    CharPaneRequest.inValhalla = inValhalla;
  }

  public static final void liberateKing() {
    // Set variables without making requests
    CharPaneRequest.canInteract = true;
    KoLCharacter.setRestricted(false);
  }

  public static final void setInteraction() {
    CharPaneRequest.setInteraction(CharPaneRequest.checkInteraction());
  }

  public static final void setInteraction(final boolean interaction) {
    if (CharPaneRequest.canInteract != interaction) {
      if (interaction && KoLCharacter.getRestricted()) {
        // Refresh skills & familiars when leaving
        // ronin or hardcore from a restricted path
        RequestThread.postRequest(new CharSheetRequest());
        RequestThread.postRequest(new CampgroundRequest("bookshelf"));
        RequestThread.postRequest(new FamiliarRequest());
        KoLCharacter.setRestricted(false);
      }
      CharPaneRequest.canInteract = interaction;
      MallSearchFrame.updateMeat();
    }
    if (interaction) {
      ConcoctionDatabase.setPullsRemaining(-1);
    }
  }

  public static boolean processResults(String responseText) {
    return CharPaneRequest.processResults(CharPaneRequest.lastResponseTimestamp, responseText);
  }

  public static boolean processResults(long responseTimestamp, String responseText) {
    if (CharPaneRequest.lastResponseTimestamp > responseTimestamp) {
      return false;
    }

    CharPaneRequest.lastResponseTimestamp = responseTimestamp;
    CharPaneRequest.lastResponse = responseText;

    // Are we in a limitmode?
    if (responseText.contains(">Last Spelunk</a>")) {
      KoLCharacter.setLimitmode(Limitmode.SPELUNKY);
      SpelunkyRequest.parseCharpane(responseText);
      return true;
    }

    if (responseText.contains("You're Batfellow")) {
      KoLCharacter.setLimitmode(Limitmode.BATMAN);
      BatManager.parseCharpane(responseText);
      return true;
    }

    if (KoLCharacter.getLimitmode() != null
        && KoLCharacter.getLimitmode().equals(Limitmode.SPELUNKY)) {
      KoLCharacter.setLimitmode(null);
    }

    // We can deduce whether we are in compact charpane mode

    CharPaneRequest.compactCharacterPane = responseText.contains("<br>Lvl. ");

    // If we are in Valhalla, do special processing
    if (KoLCharacter.getLimitmode() == null
        && (responseText.contains("otherimages/spirit.gif")
            || responseText.contains("<br>Lvl. <img"))) {
      processValhallaCharacterPane(responseText);
      return true;
    }

    CharPaneRequest.inValhalla = false;

    // KoL now includes Javascript variables in each charpane
    //
    // var turnsplayed = 232576;
    // var turnsthisrun = 232576;
    // var rollover = 1268537400;
    // var rightnow = 1268496181;
    // var pwdhash = "...";
    //
    // "turnsThisRun" is of interest for several reasons: we can
    // use it to order (some) charpane requests, even if the
    // timestamp is the same, and we can use it to synchronize
    // KolMafia with KoL's turn counter

    int turnsThisRun = parseTurnsThisRun(responseText);
    int mafiaTurnsThisRun = KoLCharacter.getCurrentRun();

    if (turnsThisRun < CharPaneRequest.turnsThisRun || turnsThisRun < mafiaTurnsThisRun) {
      // turnsThisRun = 426 CharPaneRequest.turnsThisRun = 426 mafiaTurnsThisRun = 427
      // And yet, this was a new charpane. Don't process it, but don't respond with
      // 304 Not Modified
      return true;
    }

    CharPaneRequest.turnsThisRun = turnsThisRun;
    KoLCharacter.setTurnsPlayed(parseTurnsPlayed(responseText));

    // Since we believe this update, synchronize with it
    ResultProcessor.processAdventuresUsed(turnsThisRun - mafiaTurnsThisRun);

    CharPaneRequest.parseAvatar(responseText);

    if (KoLCharacter.inDisguise()) {
      CharPaneRequest.checkMask(responseText);
    }

    CharPaneRequest.setLastAdventure(responseText);
    CharPaneRequest.refreshEffects(responseText);
    CharPaneRequest.setInteraction();

    // Refresh effects and modifiers before updating stats, since new effects
    // can mean that we should not check for incorrect substat values
    KoLCharacter.recalculateAdjustments();

    // The easiest way to retrieve the character pane data is to
    // use regular expressions. But, the only data that requires
    // synchronization is the modified stat values, health and
    // mana.

    if (CharPaneRequest.compactCharacterPane) {
      CharPaneRequest.handleCompactMode(responseText);
    } else {
      CharPaneRequest.handleExpandedMode(responseText);
    }

    KoLCharacter.updateStatus();

    CharPaneRequest.checkVYKEACompanion(responseText);

    if (KoLCharacter.inAxecore()) {
      CharPaneRequest.checkClancy(responseText);
    } else if (KoLCharacter.isJarlsberg()) {
      CharPaneRequest.checkCompanion(responseText);
    } else if (KoLCharacter.isSneakyPete()) {
      // No familiar-type checking needed
    } else if (KoLCharacter.isEd()) {
      CharPaneRequest.checkServant(responseText);
    } else if (KoLCharacter.inPokefam()) {
      CharPaneRequest.checkPokeFam(responseText);
    } else {
      CharPaneRequest.checkFamiliar(responseText);
    }

    CharPaneRequest.checkPastaThrall(responseText);

    CharPaneRequest.checkRadSickness(responseText);

    CharPaneRequest.checkAbsorbs(responseText);

    CharPaneRequest.checkFantasyRealmHours(responseText);

    CharPaneRequest.checkEnsorcelee(responseText);

    CharPaneRequest.checkYouRobot(responseText);

    // Mana cost adjustment may have changed

    LockableListFactory.sort(KoLConstants.summoningSkills);
    LockableListFactory.sort(KoLConstants.usableSkills);
    RequestFrame.refreshStatus();

    return true;
  }

  public static final String getLastResponse() {
    return CharPaneRequest.lastResponse;
  }

  // <a class='nounder ' target=mainpane href="charsheet.php"><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/classav41_f.gif"
  // width=60 height=100 border=0></a>

  public static final Pattern AVATAR_PATTERN =
      Pattern.compile(
          "<img +src=[^>]*?(?:cloudfront.net|images.kingdomofloathing.com|/images)/([^>'\"\\s]+)");

  public static final void parseAvatar(final String responseText) {
    Matcher avatarMatcher = CharPaneRequest.AVATAR_PATTERN.matcher(responseText);
    if (avatarMatcher.find()) {
      KoLCharacter.setAvatar(avatarMatcher.group(1));
    }
  }

  // <td align=center><img src="http://images.kingdomofloathing.com/itemimages/karma.gif" width=30
  // height=30 alt="Karma" title="Karma"><br>0</td>
  public static final Pattern KARMA_PATTERN = Pattern.compile("karma.gif.*?<br>([^<]*)</td>");
  // <td align=right>Karma:</td><td align=left><b>122</b></td>
  public static final Pattern KARMA_PATTERN_COMPACT = Pattern.compile("Karma:.*?<b>([^<]*)</b>");

  private static void processValhallaCharacterPane(final String responseText) {
    // We are in Valhalla
    CharPaneRequest.inValhalla = true;

    KoLCharacter.setAvatar("otherimages/spirit.gif");

    // We have no stats as an Astral Spirit
    KoLCharacter.setStatPoints(1, 0L, 1, 0L, 1, 0L);
    KoLCharacter.setHP(1, 1, 1);
    KoLCharacter.setMP(1, 1, 1);
    KoLCharacter.setAvailableMeat(0);
    KoLCharacter.setAdventuresLeft(0);
    KoLCharacter.setMindControlLevel(0);

    // No active status effects
    KoLConstants.recentEffects.clear();
    KoLConstants.activeEffects.clear();

    // No modifiers
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();

    // You certainly can't interact with the "real world"
    CharPaneRequest.setInteraction(false);

    // You do, however, have Karma available to spend in Valhalla.
    Pattern pattern =
        CharPaneRequest.compactCharacterPane
            ? CharPaneRequest.KARMA_PATTERN_COMPACT
            : CharPaneRequest.KARMA_PATTERN;
    Matcher matcher = pattern.matcher(responseText);
    int karma = matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
    Preferences.setInteger("bankedKarma", karma);
  }

  public static final Pattern TURNS_THIS_RUN_PATTERN =
      Pattern.compile("var turnsthisrun = (\\d*);");

  private static int parseTurnsThisRun(final String responseText) {
    Matcher matcher = CharPaneRequest.TURNS_THIS_RUN_PATTERN.matcher(responseText);
    if (matcher.find()) {
      return StringUtilities.parseInt(matcher.group(1));
    }

    return -1;
  }

  public static final Pattern TURNS_PLAYED_PATTERN = Pattern.compile("var turnsplayed = (\\d*);");

  private static int parseTurnsPlayed(final String responseText) {
    Matcher matcher = CharPaneRequest.TURNS_PLAYED_PATTERN.matcher(responseText);
    if (matcher.find()) {
      return StringUtilities.parseInt(matcher.group(1));
    }

    return -1;
  }

  private static boolean checkInteraction() {
    // If he's freed the king, that's good enough
    if (KoLCharacter.kingLiberated()) {
      return true;
    }

    // If he's in Hardcore, nope
    if (KoLCharacter.isHardcore()) {
      return false;
    }

    // If he's in Bad Moon, nope
    if (KoLCharacter.inBadMoon()) {
      return false;
    }

    // If the charsheet does not say he can't interact or api.php
    // says roninleft == 0, ok.
    // (this will be true for any Casual run, for an unascended
    // character, or for a sufficiently lengthy softcore run)
    if (!KoLCharacter.inRonin()) {
      return true;
    }

    // Last time we checked the char sheet or api.php, he was still
    // in ronin. See if he still is.
    // Spending turns does not let you break Ronin in Pocket Familiars
    if (KoLCharacter.getCurrentRun() >= KoLCharacter.initialRonin() && !KoLCharacter.inPokefam()) {
      return true;
    }

    // Otherwise, no way.
    return false;
  }

  private static void handleCompactMode(final String responseText) {
    try {
      CharPaneRequest.handleStatPoints(responseText, CharPaneRequest.compactStatsPattern);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
    try {
      int index =
          KoLCharacter.inZombiecore()
              ? 2
              : KoLCharacter.isPlumber() ? 4 : KoLCharacter.inRobocore() ? 6 : 0;
      CharPaneRequest.handleMiscPoints(responseText, CharPaneRequest.MISC_PATTERNS[index]);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
    try {
      CharPaneRequest.handleMindControl(responseText, CharPaneRequest.compactMCPatterns);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }

    try {
      CharPaneRequest.handleInebriety(responseText, CharPaneRequest.compactInebrietyPatterns);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }

    // Do NOT read fullness from the charpane; it is optional, so
    // we have to track it manually, anyway
  }

  private static void handleExpandedMode(final String responseText) {
    try {
      CharPaneRequest.handleStatPoints(responseText, CharPaneRequest.expandedStatsPattern);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
    try {
      int index =
          KoLCharacter.inZombiecore()
              ? 3
              : KoLCharacter.isPlumber() ? 5 : KoLCharacter.inRobocore() ? 7 : 1;
      CharPaneRequest.handleMiscPoints(responseText, CharPaneRequest.MISC_PATTERNS[index]);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
    try {
      CharPaneRequest.handleMindControl(responseText, CharPaneRequest.expandedMCPatterns);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }
    try {
      CharPaneRequest.handleInebriety(responseText, CharPaneRequest.expandedInebrietyPatterns);
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }

    // Do NOT read fullness from the charpane; it is optional, so
    // we have to track it manually, anyway
  }

  private static Pattern makeStatPattern(
      final String musString, final String mysString, final String moxString) {
    return Pattern.compile(
        ">"
            + musString
            + ".*?<b>(.*?)</b>.*?>"
            + mysString
            + ".*?<b>(.*?)</b>.*?>"
            + moxString
            + ".*?<b>(.*?)</b>");
  }

  private static final Pattern compactStatsPattern =
      CharPaneRequest.makeStatPattern("Mus", "Mys", "Mox");
  private static final Pattern expandedStatsPattern =
      CharPaneRequest.makeStatPattern("Muscle", "Mysticality", "Moxie");

  private static final Pattern modifiedPattern =
      Pattern.compile("<font color=(?:red|blue)>(\\d+)</font>&nbsp;\\((\\d+)\\)");

  private static void handleStatPoints(final String responseText, final Pattern pattern)
      throws Exception {
    Matcher statMatcher = pattern.matcher(responseText);
    if (!statMatcher.find()) {
      return;
    }

    int[] modified = new int[3];
    int[] unmodified = new int[3];
    for (int i = 0; i < 3; ++i) {
      Matcher modifiedMatcher = modifiedPattern.matcher(statMatcher.group(i + 1));

      if (modifiedMatcher.find()) {
        modified[i] = StringUtilities.parseInt(modifiedMatcher.group(1));
        unmodified[i] = StringUtilities.parseInt(modifiedMatcher.group(2));
      } else {
        modified[i] =
            unmodified[i] =
                StringUtilities.parseInt(
                    statMatcher.group(i + 1).replaceAll("<[^>]*>", "").replaceAll("[^\\d]+", ""));
      }
    }

    Modifiers mods = KoLCharacter.getCurrentModifiers();
    boolean equalize = mods.getString(Modifiers.EQUALIZE).length() != 0;
    boolean mus_equalize = mods.getString(Modifiers.EQUALIZE_MUSCLE).length() != 0;
    boolean mys_equalize = mods.getString(Modifiers.EQUALIZE_MYST).length() != 0;
    boolean mox_equalize = mods.getString(Modifiers.EQUALIZE_MOXIE).length() != 0;
    boolean mus_limit = (int) mods.get(Modifiers.MUS_LIMIT) != 0;
    boolean mys_limit = (int) mods.get(Modifiers.MYS_LIMIT) != 0;
    boolean mox_limit = (int) mods.get(Modifiers.MOX_LIMIT) != 0;

    boolean checkMus = !equalize && !mus_equalize && !mus_limit;
    boolean checkMys = !equalize && !mys_equalize && !mys_limit;
    boolean checkMox = !equalize && !mox_equalize && !mox_limit;

    long baseMus =
        checkMus
            ? CharPaneRequest.checkStat(KoLCharacter.getTotalMuscle(), unmodified[0])
            : KoLCharacter.getTotalMuscle();
    long baseMys =
        checkMys
            ? CharPaneRequest.checkStat(KoLCharacter.getTotalMysticality(), unmodified[1])
            : KoLCharacter.getTotalMysticality();
    long baseMox =
        checkMox
            ? CharPaneRequest.checkStat(KoLCharacter.getTotalMoxie(), unmodified[2])
            : KoLCharacter.getTotalMoxie();

    KoLCharacter.setStatPoints(modified[0], baseMus, modified[1], baseMys, modified[2], baseMox);
  }

  private static long checkStat(long currentSubstat, final int baseStat) {
    if (currentSubstat < KoLCharacter.calculatePointSubpoints(baseStat)) {
      currentSubstat = KoLCharacter.calculatePointSubpoints(baseStat);
    } else if (currentSubstat >= KoLCharacter.calculatePointSubpoints(baseStat + 1)) {
      currentSubstat = KoLCharacter.calculatePointSubpoints(baseStat + 1) - 1;
    }
    return currentSubstat;
  }

  private static final Pattern[][] MISC_PATTERNS = {
    // Compact
    {
      Pattern.compile("HP:.*?<b>(.*?)/(.*?)</b>"),
      Pattern.compile("MP:.*?<b>(.*?)/(.*?)</b>"),
      Pattern.compile("Meat.*?<b>(.*?)</b>"),
      Pattern.compile("Adv.*?<b>(.*?)</b>"),
    },

    // Expanded
    {
      Pattern.compile("/(?:slim)?hp\\.gif.*?<span.*?>(.*?)&nbsp;/&nbsp;(.*?)</span>"),
      Pattern.compile("/(?:slim)?mp\\.gif.*?<span.*?>(.*?)&nbsp;/&nbsp;(.*?)</span>"),
      Pattern.compile("/(?:slim)?meat\\.gif.*?<span.*?>(.*?)</span>"),
      Pattern.compile("/(?:slim)?hourglass\\.gif.*?<span.*?>(.*?)</span>"),
    },

    // Compact Zombiecore
    {
      Pattern.compile("HP.*?<b>(.*?)/(.*?)</b>"),
      Pattern.compile("Horde: (\\d+)"),
      Pattern.compile("Meat.*?<b>(.*?)</b>"),
      Pattern.compile("Adv.*?<b>(.*?)</b>"),
    },

    // Expanded Zombiecore
    {
      Pattern.compile("/(?:slim)?hp\\.gif.*?<span.*?>(.*?)&nbsp;/&nbsp;(.*?)</span>"),
      Pattern.compile("/(?:slim)?zombies/horde.*?\\.gif.*?Horde: (\\d+)"),
      Pattern.compile("/(?:slim)?meat\\.gif.*?<span.*?>(.*?)</span>"),
      Pattern.compile("/(?:slim)?hourglass\\.gif.*?<span.*?>(.*?)</span>"),
    },

    // Compact Plumber
    {
      Pattern.compile("HP:.*?<b>(.*?)/(.*?)</b>"),
      Pattern.compile("PP:.*?<b>(.*?)/(.*?)</b>"),
      Pattern.compile("Meat.*?<b>(.*?)</b>"),
      Pattern.compile("Adv.*?<b>(.*?)</b>"),
    },

    // Expanded Plumber
    {
      Pattern.compile("/(?:slim)?hp\\.gif.*?<span.*?>(.*?)&nbsp;/&nbsp;(.*?)</span>"),
      Pattern.compile("/(?:slim)?pp\\.gif.*?(\\d+) / (\\d+)<"),
      Pattern.compile("/(?:slim)?meat\\.gif.*?<span.*?>(.*?)</span>"),
      Pattern.compile("/(?:slim)?hourglass\\.gif.*?<span.*?>(.*?)</span>"),
    },

    // Compact You, Robot
    {
      Pattern.compile("HP:.*?<b>(.*?)/(.*?)</b>"),
      Pattern.compile("E:.*?<b>(\\d+) / (?:.*?)</b>"),
      Pattern.compile("Meat.*?<b>(.*?)</b>"),
      Pattern.compile("Adv.*?<b>(.*?)</b>"),
    },

    // Expanded You, Robot
    {
      Pattern.compile("/(?:slim)?hp\\.gif.*?<span.*?>(.*?)&nbsp;/&nbsp;(.*?)</span>"),
      Pattern.compile("/(?:slim)?jigawatts\\.gif.*?(\\d+)"),
      Pattern.compile("/(?:slim)?meat\\.gif.*?<span.*?>(.*?)</span>"),
      Pattern.compile("/(?:slim)?hourglass\\.gif.*?<span.*?>(.*?)</span>"),
    },
  };

  private static final int HP = 0;
  private static final int MP = 1;
  private static final int MEAT = 2;
  private static final int ADV = 3;

  private static void handleMiscPoints(final String responseText, final Pattern[] patterns)
      throws Exception {
    // Health and all that good stuff is complicated, has nested
    // images, and lots of other weird stuff. Handle it in a
    // non-modular fashion.

    Pattern pattern = patterns[HP];
    Matcher matcher = pattern == null ? null : pattern.matcher(responseText);
    if (matcher != null && matcher.find()) {
      long currentHP =
          StringUtilities.parseLong(
              matcher.group(1).replaceAll("<[^>]*>", "").replaceAll("[^\\d]+", ""));
      long maximumHP =
          StringUtilities.parseLong(
              matcher.group(2).replaceAll("<[^>]*>", "").replaceAll("[^\\d]+", ""));
      KoLCharacter.setHP(currentHP, maximumHP, maximumHP);
    }

    pattern = patterns[MP];
    matcher = pattern == null ? null : pattern.matcher(responseText);
    if (matcher != null && matcher.find()) {
      if (KoLCharacter.inZombiecore()) {
        String currentHorde = matcher.group(1);
        int horde = StringUtilities.parseInt(currentHorde);
        KoLCharacter.setMP(horde, horde, horde);
      } else if (KoLCharacter.isPlumber()) {
        int currentPP =
            StringUtilities.parseInt(
                matcher.group(1).replaceAll("<[^>]*>", "").replaceAll("[^\\d]+", ""));
        int maximumPP =
            StringUtilities.parseInt(
                matcher.group(2).replaceAll("<[^>]*>", "").replaceAll("[^\\d]+", ""));
        KoLCharacter.setPP(currentPP, maximumPP);
      } else if (KoLCharacter.inRobocore()) {
        int energy = StringUtilities.parseInt(matcher.group(1));
        KoLCharacter.setYouRobotEnergy(energy);
      } else {
        long currentMP =
            StringUtilities.parseLong(
                matcher.group(1).replaceAll("<[^>]*>", "").replaceAll("[^\\d]+", ""));
        long maximumMP =
            StringUtilities.parseLong(
                matcher.group(2).replaceAll("<[^>]*>", "").replaceAll("[^\\d]+", ""));
        KoLCharacter.setMP(currentMP, maximumMP, maximumMP);
      }
    }

    pattern = patterns[MEAT];
    matcher = pattern == null ? null : pattern.matcher(responseText);
    if (matcher != null && matcher.find()) {
      long availableMeat =
          StringUtilities.parseLong(
              matcher.group(1).replaceAll("<[^>]*>", "").replaceAll("[^\\d]+", ""));
      KoLCharacter.setAvailableMeat(availableMeat);
    }

    pattern = patterns[ADV];
    matcher = pattern == null ? null : pattern.matcher(responseText);
    if (matcher != null && matcher.find()) {
      int oldAdventures = KoLCharacter.getAdventuresLeft();
      int newAdventures =
          StringUtilities.parseInt(
              matcher.group(1).replaceAll("<[^>]*>", "").replaceAll("[^\\d]+", ""));
      ResultProcessor.processAdventuresLeft(newAdventures - oldAdventures);
    }

    if (KoLCharacter.isSealClubber()) {
      pattern = Pattern.compile(">(\\d+) gal.</span>");
      matcher = pattern.matcher(responseText);
      if (matcher != null && matcher.find()) {
        int fury = StringUtilities.parseInt(matcher.group(1));
        KoLCharacter.setFuryNoCheck(fury);
      } else {
        KoLCharacter.setFuryNoCheck(0);
      }
    } else if (KoLCharacter.isSauceror()) {
      pattern =
          Pattern.compile(
              "auce:(?:</small>)?</td><td align=left><b><font color=black>(?:<span>)?(\\d+)<");
      matcher = pattern.matcher(responseText);
      if (matcher != null && matcher.find()) {
        int soulsauce = StringUtilities.parseInt(matcher.group(1));
        KoLCharacter.setSoulsauce(soulsauce);
      } else {
        KoLCharacter.setSoulsauce(0);
      }
    } else if (KoLCharacter.isSneakyPete()) {
      pattern = Pattern.compile("<b>(\\d+ )?(Love|Hate|Bored)</td>");
      matcher = pattern.matcher(responseText);
      if (matcher != null && matcher.find()) {
        if (matcher.group(2).equals("Love")) {
          KoLCharacter.setAudience(StringUtilities.parseInt(matcher.group(1)));
        } else if (matcher.group(2).equals("Hate")) {
          KoLCharacter.setAudience(-StringUtilities.parseInt(matcher.group(1)));
        } else if (matcher.group(2).equals("Bored")) {
          KoLCharacter.setAudience(0);
        }
      } else {
        KoLCharacter.setAudience(0);
      }
    } else if (KoLCharacter.inNoobcore()) {
      pattern = Pattern.compile("<b>Absorptions:</b> (\\d+) / (\\d+)</span>");
      matcher = pattern.matcher(responseText);
      if (matcher != null && matcher.find()) {
        int absorbs = StringUtilities.parseInt(matcher.group(1));
        KoLCharacter.setAbsorbs(absorbs);
      }
    }

    // Path rather than class restricted matchers
    if (KoLCharacter.inRaincore()) {
      pattern = Pattern.compile("Thunder:</td><td align=left><b><font color=black>(\\d+) dBs");
      matcher = pattern.matcher(responseText);
      if (matcher != null && matcher.find()) {
        int thunder = StringUtilities.parseInt(matcher.group(1));
        KoLCharacter.setThunder(thunder);
      } else {
        KoLCharacter.setThunder(0);
      }
      pattern = Pattern.compile("Rain:</td><td align=left><b><font color=black>(\\d+) drops");
      matcher = pattern.matcher(responseText);
      if (matcher != null && matcher.find()) {
        int rain = StringUtilities.parseInt(matcher.group(1));
        KoLCharacter.setRain(rain);
      } else {
        KoLCharacter.setRain(0);
      }
      pattern = Pattern.compile("Lightning:</td><td align=left><b><font color=black>(\\d+) bolts");
      matcher = pattern.matcher(responseText);
      if (matcher != null && matcher.find()) {
        int lightning = StringUtilities.parseInt(matcher.group(1));
        KoLCharacter.setLightning(lightning);
      } else {
        KoLCharacter.setLightning(0);
      }
    } else if (KoLCharacter.inFirecore()) {
      pattern = Pattern.compile("Water(?: Collected)?:</td><td align=left><b>([\\d,]+)</b>");
      matcher = pattern.matcher(responseText);
      if (matcher.find()) {
        int water = StringUtilities.parseInt(matcher.group(1).replaceAll(",", ""));
        KoLCharacter.setWildfireWater(water);
      } else {
        KoLCharacter.setWildfireWater(0);
      }
    }
  }

  private static void handleMindControl(final String text, final Pattern[] patterns) {
    for (int i = 0; i < patterns.length; ++i) {
      int level = CharPaneRequest.handleMindControl(text, patterns[i]);
      if (level > 0) {
        KoLCharacter.setMindControlLevel(level);
        return;
      }
    }

    KoLCharacter.setMindControlLevel(0);
  }

  private static Pattern makeMCPattern(final String mcString) {
    return Pattern.compile(mcString + "</a>: ?(?:</td><td>)?<b>(\\d+)</b>");
  }

  private static final Pattern[] compactMCPatterns = {
    CharPaneRequest.makeMCPattern("MC"),
    CharPaneRequest.makeMCPattern("Radio"),
    CharPaneRequest.makeMCPattern("AOT5K"),
    CharPaneRequest.makeMCPattern("HH"),
  };

  private static final Pattern[] expandedMCPatterns = {
    CharPaneRequest.makeMCPattern("Mind Control"),
    CharPaneRequest.makeMCPattern("Detuned Radio"),
    CharPaneRequest.makeMCPattern("Annoy-o-Tron 5k"),
    CharPaneRequest.makeMCPattern("Heartbreaker's"),
  };

  private static int handleMindControl(final String responseText, final Pattern pattern) {
    Matcher matcher = pattern.matcher(responseText);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  private static Pattern makeConsumptionPattern(final String consumptionString) {
    return Pattern.compile(
        consumptionString
            + ":</span></td><td(?: align=left)?><b><span class=\"(?:blur.)?\">(\\d+) / (\\d+)</span>");
  }

  private static final Pattern[] compactInebrietyPatterns = {
    CharPaneRequest.makeConsumptionPattern("Drunk"),
  };

  private static final Pattern[] expandedInebrietyPatterns = {
    CharPaneRequest.makeConsumptionPattern("Drunkenness"),
    CharPaneRequest.makeConsumptionPattern("Inebriety"),
    CharPaneRequest.makeConsumptionPattern("Temulency"),
    CharPaneRequest.makeConsumptionPattern("Tipsiness"),
  };

  private static int handleConsumption(final String responseText, final Pattern pattern) {
    Matcher matcher = pattern.matcher(responseText);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  private static void handleInebriety(final String text, final Pattern[] patterns) {
    for (int i = 0; i < patterns.length; ++i) {
      int level = CharPaneRequest.handleConsumption(text, patterns[i]);
      if (level > 0) {
        KoLCharacter.setInebriety(level);
        return;
      }
    }

    KoLCharacter.setInebriety(0);
  }

  public static final AdventureResult extractEffect(final String responseText, int searchIndex) {
    String effectName = null;
    int durationIndex = -1;

    if (CharPaneRequest.compactCharacterPane) {
      int startIndex = responseText.indexOf("alt=\"", searchIndex) + 5;
      effectName = responseText.substring(startIndex, responseText.indexOf("\"", startIndex));
      durationIndex = responseText.indexOf("<td>(", startIndex) + 5;
    } else {
      int startIndex = responseText.indexOf("<font size=2", searchIndex);
      startIndex = responseText.indexOf(">", startIndex) + 1;
      durationIndex = responseText.indexOf("</font", startIndex);
      durationIndex = responseText.lastIndexOf("(", durationIndex) + 1;
      if (durationIndex < 0) {
        return null;
      }
      effectName = responseText.substring(startIndex, durationIndex - 1).trim();
    }

    searchIndex = responseText.indexOf("onClick='eff", searchIndex);
    searchIndex = responseText.indexOf("(", searchIndex) + 1;

    String descId =
        responseText.substring(searchIndex + 1, responseText.indexOf(")", searchIndex) - 1);
    String durationString =
        responseText.substring(durationIndex, responseText.indexOf(")", durationIndex));

    int duration;
    if (durationString.equals("&infin;")) {
      duration = Integer.MAX_VALUE;
    } else if (durationString.indexOf("&") != -1 || durationString.indexOf("<") != -1) {
      return null;
    } else {
      duration = StringUtilities.parseInt(durationString);
    }

    return CharPaneRequest.extractEffect(descId, effectName, duration);
  }

  public static final AdventureResult extractEffect(
      final String descId, String effectName, int duration) {
    int effectId = EffectDatabase.getEffectIdFromDescription(descId);

    if (effectId == -1) {
      effectId = EffectDatabase.learnEffectId(effectName, descId);
    } else if (effectId == EffectPool.BLESSING_OF_THE_BIRD) {
      ResultProcessor.updateBird(EffectPool.BLESSING_OF_THE_BIRD, effectName, "_birdOfTheDay");
    } else if (effectId == EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD) {
      ResultProcessor.updateBird(
          EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD, effectName, "yourFavoriteBird");
    }

    if (duration == Integer.MAX_VALUE) {
      // Intrinsic effect
    }

    return EffectPool.get(effectId, duration);
  }

  private static void refreshEffects(final String responseText) {
    int searchIndex = 0;
    int onClickIndex = 0;

    ArrayList<AdventureResult> visibleEffects = new ArrayList<AdventureResult>();

    while (onClickIndex != -1) {
      onClickIndex = responseText.indexOf("onClick='eff", onClickIndex + 1);

      if (onClickIndex == -1) {
        continue;
      }

      searchIndex = responseText.lastIndexOf("<", onClickIndex);

      AdventureResult effect = CharPaneRequest.extractEffect(responseText, searchIndex);
      if (effect == null) {
        continue;
      }

      int currentCount = effect.getCount();
      if (currentCount == 0) {
        // This is an expired effect. We don't need to
        // explicitly remove it from activeEffects,
        // since we'll simply not retain it.
        continue;
      }

      int activeCount = effect.getCount(KoLConstants.activeEffects);

      if (currentCount != activeCount) {
        ResultProcessor.processResult(effect.getInstance(currentCount - activeCount));
      }

      visibleEffects.add(effect);
    }

    KoLConstants.recentEffects.clear();
    KoLConstants.activeEffects.clear();
    KoLConstants.activeEffects.addAll(visibleEffects);
    LockableListFactory.sort(KoLConstants.activeEffects);

    CharPaneRequest.checkChilledToTheBone();
  }

  private static void checkChilledToTheBone() {
    // Update chilled to the bone - consequences.txt should populate it from description
    int chilledCount = CharPaneRequest.CHILLED_TO_THE_BONE.getCount(KoLConstants.activeEffects);
    if (chilledCount > 0 && Preferences.getInteger("chilledToTheBone") == 0) {
      String descId = EffectDatabase.getDescriptionId(EffectPool.CHILLED_TO_THE_BONE);
      GenericRequest req = new GenericRequest("desc_effect.php?whicheffect=" + descId);
      RequestThread.postRequest(req);
    }
    if (chilledCount == 0 && Preferences.getInteger("chilledToTheBone") > 0) {
      Preferences.setInteger("chilledToTheBone", 0);
    }
  }

  private static final Pattern compactLastAdventurePattern =
      Pattern.compile(
          "<td align=right><a onclick=[^<]+ title=\"Last Adventure: ([^\"]+)\" target=mainpane href=\"([^\"]*)\">.*?</a>:</td>");

  // <a onclick='if (top.mainpane.focus) top.mainpane.focus();' class=nounder
  // href="place.php?whichplace=airport_stench" target=mainpane>Last
  // Adventure:</a></b></font><br><table cellspacing=0 cellpadding=0><tr><td><font size=2><a
  // onclick='if (top.mainpane.focus) top.mainpane.focus();' target=mainpane
  // href="adventure.php?snarfblat=442">Barf Mountain</a><br></font></td></tr></table>

  private static final Pattern expandedLastAdventurePattern =
      Pattern.compile(
          "<a .*?href=\"([^\"]*)\"[^>]*>Last Adventure:</a>.*?<a .*?href=\"([^\"]*)\">([^<]*)</a>.*?</table>");

  private static void setLastAdventure(final String responseText) {
    if (KoLCharacter.inFightOrChoice()) {
      return;
    }

    String adventureName = null;
    String adventureURL = null;
    String container = null;
    if (CharPaneRequest.compactCharacterPane) {
      Matcher matcher = CharPaneRequest.compactLastAdventurePattern.matcher(responseText);
      if (matcher.find()) {
        adventureName = matcher.group(1);
        adventureURL = matcher.group(2);
      }
    } else {
      Matcher matcher = CharPaneRequest.expandedLastAdventurePattern.matcher(responseText);
      if (matcher.find()) {
        adventureName = matcher.group(3);
        adventureURL = matcher.group(2);
        container = matcher.group(1);
      }
    }

    if (adventureName == null || adventureName.equals("The Naughty Sorceress' Tower")) {
      return;
    }

    CharPaneRequest.setLastAdventure("", adventureName, adventureURL, container);
  }

  private static void setLastAdventure(
      final String adventureId,
      final String adventureName,
      final String adventureURL,
      final String container) {
    if (KoLCharacter.inFightOrChoice()) {
      return;
    }

    if (adventureName == null || adventureName.equals("The Naughty Sorceress' Tower")) {
      return;
    }

    KoLAdventure adventure =
        KoLAdventure.setLastAdventure(adventureId, adventureName, adventureURL, container);

    if (KoLmafia.isRefreshing()) {
      KoLAdventure.setNextAdventure(adventure);
    }

    if (AdventureSpentDatabase.getNoncombatEncountered()
        && KoLCharacter.getCurrentRun() > AdventureSpentDatabase.getLastTurnUpdated()) {
      AdventureSpentDatabase.addTurn(KoLAdventure.lastLocationName);
    }
    AdventureSpentDatabase.setNoncombatEncountered(false);
    AdventureSpentDatabase.setLastTurnUpdated(KoLCharacter.getCurrentRun());
  }

  private static final Pattern compactFamiliarWeightPattern = Pattern.compile("<br>([\\d]+) lb");
  private static final Pattern expandedFamiliarWeightPattern =
      Pattern.compile("<b>([\\d]+)</b> pound");
  private static final Pattern familiarImagePattern =
      Pattern.compile(
          "<a.*?class=\"familiarpick\"><img.*?((?:item|other)images)/(.*?\\.(?:gif|png))");
  private static final AdventureResult somePigs = EffectPool.get(EffectPool.SOME_PIGS);

  private static void checkFamiliar(final String responseText) {
    if (KoLConstants.activeEffects.contains(CharPaneRequest.somePigs)) {
      KoLCharacter.setFamiliarImage("somepig.gif");
      return;
    }

    Pattern pattern =
        CharPaneRequest.compactCharacterPane
            ? CharPaneRequest.compactFamiliarWeightPattern
            : CharPaneRequest.expandedFamiliarWeightPattern;
    Matcher matcher = pattern.matcher(responseText);
    if (matcher.find()) {
      int weight = StringUtilities.parseInt(matcher.group(1));
      boolean feasted = responseText.contains("well-fed");
      KoLCharacter.getFamiliar().checkWeight(weight, feasted);
    }

    pattern = CharPaneRequest.familiarImagePattern;
    matcher = pattern.matcher(responseText);
    if (matcher.find()) {
      String directory = matcher.group(1);
      String image = matcher.group(2);
      int familiarId = KoLCharacter.getFamiliar().getId();
      if (image.startsWith("snow")) {
        CharPaneRequest.checkSnowsuit(image);
      }
      // Left-Hand Man's image is composed of body + an item image
      // Melodramedary's image has left, middle, right images
      // Perhaps we could handle this, but let's not bother
      else if (familiarId != FamiliarPool.LEFT_HAND && familiarId != FamiliarPool.MELODRAMEDARY) {
        KoLCharacter.setFamiliarImage(directory, image);
        CharPaneRequest.checkMedium(responseText);
        CharPaneRequest.checkMiniAdventurer(image);
        if (image.startsWith("commacha")) {
          CharPaneRequest.checkComma(responseText);
        }
      }
    }
  }

  private static final Pattern compactClancyPattern =
      Pattern.compile("otherimages/clancy_([123])(_att)?.gif.*?L\\. (\\d+)", Pattern.DOTALL);
  private static final Pattern expandedClancyPattern =
      Pattern.compile(
          "<b>Clancy</b>.*?Level <b>(\\d+)</b>.*?otherimages/clancy_([123])(_att)?.gif",
          Pattern.DOTALL);

  public static AdventureResult SACKBUT = ItemPool.get(ItemPool.CLANCY_SACKBUT, 1);
  public static AdventureResult CRUMHORN = ItemPool.get(ItemPool.CLANCY_CRUMHORN, 1);
  public static AdventureResult LUTE = ItemPool.get(ItemPool.CLANCY_LUTE, 1);

  private static void checkClancy(final String responseText) {
    Pattern pattern =
        CharPaneRequest.compactCharacterPane
            ? CharPaneRequest.compactClancyPattern
            : CharPaneRequest.expandedClancyPattern;
    Matcher clancyMatcher = pattern.matcher(responseText);
    if (clancyMatcher.find()) {
      String level = clancyMatcher.group(CharPaneRequest.compactCharacterPane ? 3 : 1);
      String image = clancyMatcher.group(CharPaneRequest.compactCharacterPane ? 1 : 2);
      boolean att = clancyMatcher.group(CharPaneRequest.compactCharacterPane ? 2 : 3) != null;
      AdventureResult instrument =
          image.equals("1")
              ? CharPaneRequest.SACKBUT
              : image.equals("2")
                  ? CharPaneRequest.CRUMHORN
                  : image.equals("3") ? CharPaneRequest.LUTE : null;
      KoLCharacter.setClancy(StringUtilities.parseInt(level), instrument, att);
    }
  }

  private static final Pattern compactServantPattern =
      Pattern.compile(
          "<b>Servant:</b>.*?target=\"mainpane\">(.*?) \\(lvl (\\d+)\\).*?edserv(\\d+).gif",
          Pattern.DOTALL);
  private static final Pattern expandedServantPattern =
      Pattern.compile(
          "<b>Servant:</b>.*?target=\"mainpane\">(.*?) the (\\d+) level.*?edserv(\\d+).gif",
          Pattern.DOTALL);

  private static void checkServant(final String responseText) {
    Pattern pattern =
        CharPaneRequest.compactCharacterPane
            ? CharPaneRequest.compactServantPattern
            : CharPaneRequest.expandedServantPattern;
    Matcher servantMatcher = pattern.matcher(responseText);
    if (servantMatcher.find()) {
      EdServantData.setEdServant(servantMatcher);
    }
  }

  private static final Pattern PokeFamPattern =
      Pattern.compile(
          "img align=\"absmiddle\" src=(?:cloudfront.net|images.kingdomofloathing.com)/itemimages/(.*?)>&nbsp;(.*?) \\(Lvl (\\d+)\\)",
          Pattern.DOTALL);

  private static void checkPokeFam(final String responseText) {
    Matcher PokeFamMatcher = CharPaneRequest.PokeFamPattern.matcher(responseText);
    for (int i = 0; i < 3; i++) {
      if (PokeFamMatcher.find()) {
        int id = FamiliarDatabase.getFamiliarByImageLocation(PokeFamMatcher.group(1));
        String name = PokeFamMatcher.group(2);
        int level = StringUtilities.parseInt(PokeFamMatcher.group(3));
        FamiliarData familiar = KoLCharacter.findFamiliar(id);
        if (familiar == null) {
          // Add new familiar to list
          familiar = new FamiliarData(id, name, level);
          KoLCharacter.addFamiliar(familiar);
        } else {
          // Update existing familiar
          familiar.update(name, level);
        }
        KoLCharacter.addFamiliar(familiar);
        KoLCharacter.setPokeFam(i, familiar);
      } else {
        KoLCharacter.setPokeFam(i, FamiliarData.NO_FAMILIAR);
      }
    }
  }

  public enum Companion {
    EGGMAN("Eggman", "jarl_eggman.gif"),
    RADISH("Radish Horse", "jarl_horse.gif"),
    HIPPO("Hippotatomous", "jarl_hippo.gif"),
    CREAM("Cream Puff", "jarl_creampuff.gif");

    private final String name;
    private final String image;

    Companion(String name, String image) {
      this.name = name;
      this.image = image;
    }

    @Override
    public String toString() {
      return this.name;
    }

    public String imageName() {
      return this.image;
    }
  }

  private static void checkCompanion(final String responseText) {
    if (responseText.contains("the Eggman")) {
      KoLCharacter.setCompanion(Companion.EGGMAN);
    } else if (responseText.contains("the Radish Horse")) {
      KoLCharacter.setCompanion(Companion.RADISH);
    } else if (responseText.contains("the Hippotatomous")) {
      KoLCharacter.setCompanion(Companion.HIPPO);
    } else if (responseText.contains("the Cream Puff")) {
      KoLCharacter.setCompanion(Companion.CREAM);
    } else {
      KoLCharacter.setCompanion(null);
    }
  }

  // <font size=2><b>VYKEA Companion</b></font><br><font size=2><b>&Aring;VOB&Eacute;</b> the level
  // 5 lamp<br>
  private static final Pattern VYKEACompanionPattern =
      Pattern.compile("<b>VYKEA Companion</b></font><br><font size=2>(.*?)<br>", Pattern.DOTALL);

  private static void checkVYKEACompanion(final String responseText) {
    // Since you can't change companions once you have built one,
    // no need to parse the charpane if we know the companion.
    if (VYKEACompanionData.currentCompanion() != VYKEACompanionData.NO_COMPANION) {
      return;
    }

    Pattern pattern = CharPaneRequest.VYKEACompanionPattern;
    Matcher matcher = pattern.matcher(responseText);
    if (matcher.find()) {
      VYKEACompanionData.parseCharpaneCompanion(matcher.group(1));
    }
  }

  private static final Pattern pastaThrallPattern =
      Pattern.compile(
          "desc_guardian.php.*?itemimages/(.*?.gif).*?<b>(.*?)</b>.*?the Lvl. (\\d+) (.*?)</font>",
          Pattern.DOTALL);

  private static void checkPastaThrall(final String responseText) {
    if (!KoLCharacter.isPastamancer()) {
      return;
    }
    Pattern pattern = CharPaneRequest.pastaThrallPattern;
    Matcher matcher = pattern.matcher(responseText);
    if (matcher.find()) {
      // String image = matcher.group( 1 );
      String name = matcher.group(2);
      String levelString = matcher.group(3);
      String type = matcher.group(4);
      PastaThrallData thrall = KoLCharacter.findPastaThrall(type);
      if (thrall != null && thrall != PastaThrallData.NO_THRALL) {
        KoLCharacter.setPastaThrall(thrall);
        thrall.update(StringUtilities.parseInt(levelString), name);
      }
    } else {
      KoLCharacter.setPastaThrall(PastaThrallData.NO_THRALL);
    }
  }

  private static final Pattern radSicknessPattern =
      Pattern.compile(
          "Rad(?:iation| Sickness):</td><td align=left><b><font color=black><span alt=\"-(\\d+) to All Stats");

  private static void checkRadSickness(final String responseText) {
    if (!KoLCharacter.inNuclearAutumn()) {
      return;
    }
    Pattern pattern = CharPaneRequest.radSicknessPattern;
    Matcher matcher = pattern.matcher(responseText);
    if (matcher.find()) {
      KoLCharacter.setRadSickness(StringUtilities.parseInt(matcher.group(1)));
    } else {
      KoLCharacter.setRadSickness(0);
    }
  }

  private static final Pattern fantasyRealmPattern =
      Pattern.compile("G. E. M.<br>(\\d+) hour(|s) remaining");

  private static void checkFantasyRealmHours(final String responseText) {
    if (!KoLCharacter.hasEquipped(ItemPool.FANTASY_REALM_GEM)) {
      return;
    }

    Pattern pattern = CharPaneRequest.fantasyRealmPattern;
    Matcher matcher = pattern.matcher(responseText);
    if (matcher.find()) {
      Preferences.setInteger("_frHoursLeft", StringUtilities.parseInt(matcher.group(1)));
    }
  }

  private static void checkAbsorbs(final String responseText) {
    if (!KoLCharacter.inNoobcore()) {
      return;
    }

    TagNode doc;
    doc = cleaner.clean(responseText);

    Object[] result;
    String xpath = "//div[@class='gnoob small']/font/text()";
    try {
      result = doc.evaluateXPath(xpath);
    } catch (XPatherException e) {
      StaticEntity.printStackTrace(e);
      return;
    }
    if (result.length == 0) {
      return;
    }

    ModifierList modList = new ModifierList();
    for (Object res : result) {
      String mod = Modifiers.parseModifier(res.toString());
      if (mod == null) {
        // this shouldn't happen...
        continue;
      }
      // Split into modifiers as some, like regeneration, get two values from one mod string
      ModifierList newModList = Modifiers.splitModifiers(mod);

      // Iterate over modifiers
      for (Modifier modifier : newModList) {
        String key = modifier.getName();
        String value = modifier.getValue();
        int modVal = StringUtilities.parseInt(value);

        // If modifier exists in ModList, get modifier value, add to it and replace
        if (modList.containsModifier(key)) {
          int oldVal = StringUtilities.parseInt(modList.getModifierValue(key));
          modList.removeModifier(key);
          modList.addModifier(key, Integer.toString(oldVal + modVal));
        }
        // Otherwise just add it
        else {
          modList.addModifier(modifier);
        }
      }
    }
    Modifiers.overrideModifier("Generated:Enchantments Absorbed", modList.toString());
  }

  private static final Pattern disguisePattern = Pattern.compile("masks/mask(\\d+).png");

  private static void checkMask(final String responseText) {
    if (!KoLCharacter.inDisguise()) {
      return;
    }

    String mask = null;
    Pattern pattern = CharPaneRequest.disguisePattern;
    Matcher matcher = pattern.matcher(responseText);
    if (matcher.find()) {
      switch (StringUtilities.parseInt(matcher.group(1))) {
        case 1:
          mask = "Mr. Mask";
          break;
        case 2:
          mask = "devil mask";
          break;
        case 3:
          mask = "protest mask";
          break;
        case 4:
          mask = "batmask";
          break;
        case 5:
          mask = "punk mask";
          break;
        case 6:
          mask = "hockey mask";
          break;
        case 7:
          mask = "bandit mask";
          break;
        case 8:
          mask = "plague doctor mask";
          break;
        case 9:
          mask = "robot mask";
          break;
        case 10:
          mask = "skull mask";
          break;
        case 11:
          mask = "monkey mask";
          break;
        case 12:
          mask = "luchador mask";
          break;
        case 13:
          mask = "welding mask";
          break;
        case 14:
          mask = "ninja mask";
          break;
        case 15:
          mask = "snowman mask";
          break;
        case 16:
          mask = "gasmask";
          break;
        case 17:
          mask = "fencing mask";
          break;
        case 18:
          mask = "opera mask";
          break;
        case 19:
          mask = "scary mask";
          break;
        case 20:
          mask = "alien mask";
          break;
        case 21:
          mask = "murderer mask";
          break;
        case 22:
          mask = "pumpkin mask";
          break;
        case 23:
          mask = "rabbit mask";
          break;
        case 24:
          mask = "ski mask";
          break;
        case 25:
          mask = "tiki mask";
          break;
        case 26:
          mask = "motorcycle mask";
          break;
        case 27:
          mask = "magical cartoon princess mask";
          break;
        case 28:
          mask = "catcher's mask";
          break;
        case 29:
          mask = "&quot;sexy&quot; mask";
          break;
        case 30:
          mask = "werewolf mask";
          break;
        case 100:
          mask = "Bonerdagon mask";
          break;
        case 101:
          mask = "Naughty Sorceress mask";
          break;
        case 102:
          mask = "Groar mask";
          break;
        case 103:
          mask = "Ed the Undying mask";
          break;
        case 104:
          mask = "Big Wisniewski mask";
          break;
        case 105:
          mask = "The Man mask";
          break;
        case 106:
          mask = "Boss Bat mask";
          break;
      }
    }
    KoLCharacter.setMask(mask);
  }

  private static final Pattern commaPattern =
      Pattern.compile("pound (.*?), Chameleon", Pattern.DOTALL);

  private static void checkComma(final String responseText) {
    Pattern pattern = CharPaneRequest.commaPattern;
    Matcher commaMatcher = pattern.matcher(responseText);
    if (commaMatcher.find()) {
      String newRace = commaMatcher.group(1);
      if (!newRace.equals(Preferences.getString("commaFamiliar"))) {
        Preferences.setString("commaFamiliar", commaMatcher.group(1));
        KoLCharacter.recalculateAdjustments();
      }
    } else {
      if (!Preferences.getString("commaFamiliar").equals("")) {
        Preferences.setString("commaFamiliar", "");
        KoLCharacter.recalculateAdjustments();
      }
    }
  }

  private static final Pattern mediumPattern =
      Pattern.compile("images/medium_([0123]).gif", Pattern.DOTALL);

  private static void checkMedium(final String responseText) {
    Pattern pattern = CharPaneRequest.mediumPattern;
    Matcher mediumMatcher = pattern.matcher(responseText);
    if (mediumMatcher.find()) {
      int aura = StringUtilities.parseInt(mediumMatcher.group(1));
      FamiliarData fam = KoLCharacter.findFamiliar(FamiliarPool.HAPPY_MEDIUM);
      if (fam == null) {
        // Another familiar has turned into a Happy Medium
        return;
      }
      fam.setCharges(aura);
    }
  }

  public static void checkMiniAdventurer(final String image) {
    if (image.startsWith("miniadv")) {
      String miniAdvClass = image.substring(7, 8);
      if (!miniAdvClass.equals(Preferences.getString("miniAdvClass"))) {
        Preferences.setString("miniAdvClass", miniAdvClass);
        KoLCharacter.recalculateAdjustments();
      }
    }
  }

  private static final Pattern snowsuitPattern = Pattern.compile("snowface([1-5]).gif");

  private static void checkSnowsuit(final String responseText) {
    Matcher matcher = CharPaneRequest.snowsuitPattern.matcher(responseText);
    if (matcher.find()) {
      int id = StringUtilities.parseInt(matcher.group(1)) - 1;
      Preferences.setString("snowsuit", SnowsuitCommand.DECORATION[id][0]);
    }
  }

  private static final Pattern ensorceleePattern =
      Pattern.compile("Ensorcelee:</b><br><img src=\"?(.*?)\"?><br>", Pattern.DOTALL);

  private static void checkEnsorcelee(final String responseText) {
    if (!KoLCharacter.isVampyre()) {
      return;
    }

    Matcher matcher = ensorceleePattern.matcher(responseText);
    if (matcher.find()) {
      String image = matcher.group(1);
      MonsterData ensorcelee = MonsterDatabase.findMonsterByImage(image);
      if (ensorcelee != null) {
        if (!ensorcelee.toString().equals(Preferences.getString("ensorcelee"))) {
          Preferences.setString("ensorcelee", ensorcelee.toString());
          // If we discovered a new ensorcelee we don't know its level
          // but we can't zero out that level here because of race conditions with FightRequest
        }
      }
    } else {
      Preferences.setString("ensorcelee", "");
      Preferences.setInteger("ensorceleeLevel", 0);
    }
  }

  private static final Pattern YOU_ROBOT_SCRAPS_EXPANDED =
      Pattern.compile("scrap\\.gif.*?>([\\d,]+)<");
  private static final Pattern YOU_ROBOT_SCRAPS_COMPACT =
      Pattern.compile("Scrap.*?<b>([\\d,]+)</b>");

  private static void checkYouRobot(final String responseText) {
    if (!KoLCharacter.inRobocore()) {
      return;
    }

    if (!CharPaneRequest.compactCharacterPane) {
      ScrapheapRequest.parseConfiguration(responseText);
    }

    // Energy is handled in the handleMiscPoints function as it replaces MP

    Pattern pattern =
        (CharPaneRequest.compactCharacterPane)
            ? CharPaneRequest.YOU_ROBOT_SCRAPS_COMPACT
            : CharPaneRequest.YOU_ROBOT_SCRAPS_EXPANDED;

    Matcher matcher = pattern.matcher(responseText);

    if (matcher.find()) {
      int scraps = StringUtilities.parseInt(matcher.group(1));
      KoLCharacter.setYouRobotScraps(scraps);
    }
  }

  public static final void parseStatus(final JSONObject JSON) throws JSONException {
    int turnsThisRun = JSON.getInt("turnsthisrun");
    CharPaneRequest.turnsThisRun = turnsThisRun;

    int turnsPlayed = JSON.getInt("turnsplayed");
    KoLCharacter.setTurnsPlayed(turnsPlayed);

    if (KoLmafia.isRefreshing()) {
      // If we are refreshing status, simply set turns used
      KoLCharacter.setCurrentRun(turnsThisRun);
    } else {
      // Otherwise, increment them
      int mafiaTurnsThisRun = KoLCharacter.getCurrentRun();
      ResultProcessor.processAdventuresUsed(turnsThisRun - mafiaTurnsThisRun);
    }

    Object lmo = JSON.get("limitmode");
    KoLCharacter.setLimitmode(lmo.toString());

    JSONObject lastadv = JSON.getJSONObject("lastadv");
    String adventureId = lastadv.getString("id");
    String adventureName = lastadv.getString("name");
    String adventureURL = lastadv.getString("link");
    String container = lastadv.optString("container");
    CharPaneRequest.setLastAdventure(adventureId, adventureName, adventureURL, container);

    int fury = JSON.getInt("fury");
    KoLCharacter.setFuryNoCheck(fury);

    int soulsauce = JSON.getInt("soulsauce");
    KoLCharacter.setSoulsauce(soulsauce);

    if (KoLCharacter.isSneakyPete()) {
      int audience = 0;
      audience += JSON.getInt("petelove");
      audience -= JSON.getInt("petehate");
      KoLCharacter.setAudience(audience);
    }

    long hp = JSON.getLong("hp");
    long maxhp = JSON.getLong("maxhp");
    KoLCharacter.setHP(hp, maxhp, maxhp);

    if (KoLCharacter.inZombiecore()) {
      int horde = JSON.getInt("horde");
      KoLCharacter.setMP(horde, horde, horde);
    } else {
      long mp = JSON.getLong("mp");
      long maxmp = JSON.getLong("maxmp");
      KoLCharacter.setMP(mp, maxmp, maxmp);
    }

    long meat = JSON.getLong("meat");
    KoLCharacter.setAvailableMeat(meat);

    int drunk = JSON.getInt("drunk");
    KoLCharacter.setInebriety(drunk);

    int full = JSON.getInt("full");
    KoLCharacter.setFullness(full);

    int spleen = JSON.getInt("spleen");
    KoLCharacter.setSpleenUse(spleen);

    int adventures = JSON.getInt("adventures");
    KoLCharacter.setAdventuresLeft(adventures);

    int mcd = JSON.getInt("mcd");
    KoLCharacter.setMindControlLevel(mcd);

    int classType = JSON.getInt("class");
    KoLCharacter.setAscensionClass(classType);

    int pvpFights = JSON.getInt("pvpfights");
    KoLCharacter.setAttacksLeft(pvpFights);

    CharPaneRequest.refreshEffects(JSON);

    boolean hardcore = JSON.getInt("hardcore") == 1;
    KoLCharacter.setHardcore(hardcore);

    boolean casual = JSON.getInt("casual") == 1;
    KoLCharacter.setCasual(casual);

    // boolean casual = JSON.getInt( "casual" ) == 1;
    int roninLeft = JSON.getInt("roninleft");

    if (KoLCharacter.inRaincore()) {
      int thunder = JSON.getInt("thunder");
      KoLCharacter.setThunder(thunder);
      int rain = JSON.getInt("rain");
      KoLCharacter.setRain(rain);
      int lightning = JSON.getInt("lightning");
      KoLCharacter.setLightning(lightning);
    }

    // *** Assume that roninleft always equals 0 if casual
    KoLCharacter.setRonin(roninLeft > 0);

    CharPaneRequest.setInteraction();

    if (Limitmode.limitFamiliars()) {
      // No familiar
    } else if (KoLCharacter.inAxecore()) {
      int level = JSON.getInt("clancy_level");
      int itype = JSON.getInt("clancy_instrument");
      boolean att = JSON.getBoolean("clancy_wantsattention");
      AdventureResult instrument =
          itype == 1
              ? CharPaneRequest.SACKBUT
              : itype == 2 ? CharPaneRequest.CRUMHORN : itype == 3 ? CharPaneRequest.LUTE : null;
      KoLCharacter.setClancy(level, instrument, att);
    } else if (KoLCharacter.isJarlsberg()) {
      if (JSON.has("jarlcompanion")) {
        int companion = JSON.getInt("jarlcompanion");
        switch (companion) {
          case 1:
            KoLCharacter.setCompanion(Companion.EGGMAN);
            break;
          case 2:
            KoLCharacter.setCompanion(Companion.RADISH);
            break;
          case 3:
            KoLCharacter.setCompanion(Companion.HIPPO);
            break;
          case 4:
            KoLCharacter.setCompanion(Companion.CREAM);
            break;
        }
      } else {
        KoLCharacter.setCompanion(null);
      }
    } else if (KoLCharacter.isEd()) {
      // No familiar, but may have a servant.  Unfortunately,
      // details of such are not in api.php
    } else if (!KoLCharacter.isSneakyPete()
        && !KoLCharacter.inBondcore()
        && !KoLCharacter.isVampyre()) {
      int famId = JSON.getInt("familiar");
      int famExp = JSON.getInt("familiarexp");
      FamiliarData familiar = FamiliarData.registerFamiliar(famId, famExp);
      KoLCharacter.setFamiliar(familiar);

      String image = JSON.getString("familiarpic");
      if (famId != FamiliarPool.MELODRAMEDARY) {
        KoLCharacter.setFamiliarImage(image.equals("") ? null : image + ".gif");
      }

      int weight = JSON.getInt("famlevel");
      boolean feasted = JSON.getInt("familiar_wellfed") == 1;
      familiar.checkWeight(weight, feasted);

      // Set charges from the Medium's image

      if (famId == FamiliarPool.HAPPY_MEDIUM) {
        int aura = StringUtilities.parseInt(image.substring(7, 8));
        FamiliarData medium = KoLCharacter.findFamiliar(FamiliarPool.HAPPY_MEDIUM);
        medium.setCharges(aura);
      }
    }

    int thrallId = JSON.getInt("pastathrall");
    int thrallLevel = JSON.getInt("pastathralllevel");

    PastaThrallData thrall = KoLCharacter.findPastaThrall(thrallId);
    if (thrall != null) {
      KoLCharacter.setPastaThrall(thrall);
      if (thrall != PastaThrallData.NO_THRALL) {
        thrall.update(thrallLevel, null);
      }
    }

    if (KoLCharacter.inNuclearAutumn()) {
      if (JSON.has("radsickness")) {
        int rads = JSON.getInt("radsickness");
        KoLCharacter.setRadSickness(rads);
      } else {
        KoLCharacter.setRadSickness(0);
      }
    }
  }

  private static void refreshEffects(final JSONObject JSON) throws JSONException {
    ArrayList<AdventureResult> visibleEffects = new ArrayList<AdventureResult>();

    Object o = JSON.get("effects");
    if (o instanceof JSONObject) {
      // KoL returns an empty JSON array if there are no effects
      JSONObject effects = (JSONObject) o;

      Iterator<String> keys = effects.keys();
      while (keys.hasNext()) {
        String descId = keys.next();
        JSONArray data = effects.getJSONArray(descId);
        String effectName = data.getString(0);
        int count = data.getInt(1);

        AdventureResult effect = CharPaneRequest.extractEffect(descId, effectName, count);
        if (effect != null) {
          visibleEffects.add(effect);
        }
      }
    }

    o = JSON.get("intrinsics");
    if (o instanceof JSONObject) {
      JSONObject intrinsics = (JSONObject) o;

      Iterator<String> keys = intrinsics.keys();
      while (keys.hasNext()) {
        String descId = keys.next();
        JSONArray data = intrinsics.getJSONArray(descId);
        String effectName = data.getString(0);

        AdventureResult effect =
            CharPaneRequest.extractEffect(descId, effectName, Integer.MAX_VALUE);
        if (effect != null) {
          visibleEffects.add(effect);
        }
      }
    }

    KoLConstants.recentEffects.clear();
    KoLConstants.activeEffects.clear();
    KoLConstants.activeEffects.addAll(visibleEffects);
    LockableListFactory.sort(KoLConstants.activeEffects);

    CharPaneRequest.checkChilledToTheBone();

    // Do this now so that familiar weight effects are accounted for
    KoLCharacter.recalculateAdjustments();
  }
}
