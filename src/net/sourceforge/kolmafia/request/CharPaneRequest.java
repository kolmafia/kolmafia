package net.sourceforge.kolmafia.request;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.PastaThrallData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.CrystalBallManager;
import net.sourceforge.kolmafia.session.LimitMode;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.YouRobotManager;
import net.sourceforge.kolmafia.swingui.MallSearchFrame;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.textui.command.SnowsuitCommand;
import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

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
    CharPaneRequest.setCanInteract(true);
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
      CharPaneRequest.setCanInteract(interaction);
      MallSearchFrame.updateMeat();
    }
    if (interaction) {
      ConcoctionDatabase.setPullsRemaining(-1);
    }
  }

  public static final void setCanInteract(final boolean interaction) {
    CharPaneRequest.canInteract = interaction;
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
      KoLCharacter.setLimitMode(LimitMode.SPELUNKY);
      SpelunkyRequest.parseCharpane(responseText);
      return true;
    }

    if (responseText.contains("You're Batfellow")) {
      KoLCharacter.setLimitMode(LimitMode.BATMAN);
      BatManager.parseCharpane(responseText);
      return true;
    }

    if (KoLCharacter.getLimitMode() == LimitMode.SPELUNKY) {
      KoLCharacter.setLimitMode(LimitMode.NONE);
    }

    // We can deduce whether we are in compact charpane mode

    CharPaneRequest.compactCharacterPane = responseText.contains("<br>Lvl. ");

    // If we are in Valhalla, do special processing
    if (KoLCharacter.getLimitMode() == LimitMode.NONE
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
    CharPaneRequest.parseTitle(responseText);

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

    checkPirateRealm(responseText);

    CharPaneRequest.checkEnsorcelee(responseText);

    CharPaneRequest.checkYouRobot(responseText);

    CharPaneRequest.checkSweatiness(responseText);

    CharPaneRequest.check8BitScore(responseText);

    CharPaneRequest.checkNoncombatForcers(responseText);

    CharPaneRequest.checkWereProfessor(responseText);

    // Mana cost adjustment may have changed

    LockableListFactory.sort(KoLConstants.summoningSkills);
    LockableListFactory.sort(KoLConstants.usableSkills);
    RequestFrame.refreshStatus();

    return true;
  }

  private static final Map<String, String> NONCOMBAT_FORCERS =
      Map.of(
          "You are temporarily in the mostly-combatless world of Clara's Bell.", "clara",
          "Your spikes are scaring away most monsters.", "spikolodon",
          "With the jelly all over you, you are probably not going to encounter anything",
              "stench jelly",
          "You've engaged exit mode on your cincho and will avoid most combats.", "cincho exit",
          "You are avoiding fights until something cool happens.", "sneakisol",
          "Your tuba playing has scared away most monsters.", "band tuba");
  private static final Pattern NONCOMBAT_FORCER_PATTERN =
      Pattern.compile(
          "<b><font size=2>Adventure Modifiers:</font></b><br><div style='text-align: left'><small>(.*?)</small>");

  public static void checkNoncombatForcers(final String responseText) {
    var result = NONCOMBAT_FORCER_PATTERN.matcher(responseText);
    boolean noncombatForcerActive = result.find();
    Preferences.setBoolean("noncombatForcerActive", noncombatForcerActive);
    if (noncombatForcerActive) {
      var descriptions = result.group(1).split("<br>");
      Preferences.setString(
          "noncombatForcers",
          Arrays.stream(descriptions)
              .map(desc -> NONCOMBAT_FORCERS.get(desc.trim()))
              .filter(Objects::nonNull)
              .collect(Collectors.joining("|")));
    } else {
      Preferences.setString("noncombatForcers", "");
    }
  }

  public static final String getLastResponse() {
    return CharPaneRequest.lastResponse;
  }

  // <a class='nounder ' target=mainpane href="charsheet.php"><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/classav41_f.gif"
  // width=60 height=100 border=0></a>

  // Update June 24, 2023, now with new property...
  // <a class='nounder ' target=mainpane href="charsheet.php">
  // <div style="position: relative; height: 100px; width: 60px">
  // <img  crossorigin="Anonymous"
  // src="https://d2uyhvukfffg5a.cloudfront.net/otherimages/classav4a.gif"
  // width=60 height=100 border=0>

  public static final Pattern AVATAR_PATTERN =
      Pattern.compile(
          "<img +(?:crossorigin=\"Anonymous\" +|)src=[^>]*?(?:cloudfront.net|images.kingdomofloathing.com|/images)/([^>'\"\\s]+)");

  public static final void parseAvatar(final String responseText) {
    if (!KoLCharacter.inRobocore()) {
      Matcher avatarMatcher = CharPaneRequest.AVATAR_PATTERN.matcher(responseText);
      if (avatarMatcher.find()) {
        KoLCharacter.setAvatar(avatarMatcher.group(1));
      }
    }
  }

  public static final Pattern TITLE_PATTERN =
      Pattern.compile(
          "<a class=nounder target=mainpane href=\"charsheet.php\"><b>[^>]*?</b></a><br>(?<title>[^<]*?)<br>[^<]*?<table");

  public static void parseTitle(final String responseText) {
    Matcher titleMatcher = CharPaneRequest.TITLE_PATTERN.matcher(responseText);
    if (titleMatcher.find()) {
      KoLCharacter.setTitle(titleMatcher.group("title"));
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
    if (KoLCharacter.inSmallcore()) {
      // trust api.php
      return;
    }

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
    boolean equalize = mods.getString(StringModifier.EQUALIZE).length() != 0;
    boolean mus_equalize = mods.getString(StringModifier.EQUALIZE_MUSCLE).length() != 0;
    boolean mys_equalize = mods.getString(StringModifier.EQUALIZE_MYST).length() != 0;
    boolean mox_equalize = mods.getString(StringModifier.EQUALIZE_MOXIE).length() != 0;
    boolean mus_limit = (int) mods.getDouble(DoubleModifier.MUS_LIMIT) != 0;
    boolean mys_limit = (int) mods.getDouble(DoubleModifier.MYS_LIMIT) != 0;
    boolean mox_limit = (int) mods.getDouble(DoubleModifier.MOX_LIMIT) != 0;

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
        switch (matcher.group(2)) {
          case "Love" -> KoLCharacter.setAudience(StringUtilities.parseInt(matcher.group(1)));
          case "Hate" -> KoLCharacter.setAudience(-StringUtilities.parseInt(matcher.group(1)));
          case "Bored" -> KoLCharacter.setAudience(0);
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
            + ":</span></td><td(?: align=left)?><b><span class=\"(?:blur.)?\">(\\d+) / (-?\\d+)</span>");
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
    if (durationString.equals("&infin;") || durationString.equals("Today")) {
      duration = Integer.MAX_VALUE;
    } else if (durationString.indexOf("&") != -1 || durationString.indexOf("<") != -1) {
      return null;
    } else {
      duration = StringUtilities.parseInt(durationString);
    }

    return CharPaneRequest.extractEffect(descId, effectName, duration);
  }

  public static AdventureResult extractEffect(
      final String descId, String effectName, int duration) {
    int effectId = EffectDatabase.getEffectIdFromDescription(descId);
    if (effectId == -1) {
      effectId = EffectDatabase.learnEffectId(effectName, descId);
    }

    switch (effectId) {
      case EffectPool.BLESSING_OF_THE_BIRD -> ResultProcessor.updateBird(
          EffectPool.BLESSING_OF_THE_BIRD, effectName, "_birdOfTheDay");
      case EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD -> ResultProcessor.updateBird(
          EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD, effectName, "yourFavoriteBird");
    }

    return EffectPool.get(effectId, duration);
  }

  private static void refreshEffects(final String responseText) {
    int searchIndex = 0;
    int onClickIndex = 0;

    ArrayList<AdventureResult> visibleEffects = new ArrayList<>();

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
          "<a onclick=[^>]+ title=\"Last Adventure: ([^\"]+)\" target=mainpane href=\"([^\"]*)\">.*?</a>:");
  private static final Pattern compactTrailPattern =
      Pattern.compile(
          "<span id=\"lastadvmenu\" style=\"[^>]*?\"><font size=1>(?<trail>.*?)</font></span>");
  private static final Pattern trailElementPattern =
      Pattern.compile("<nobr><a [^>]*?href=\"(?<link>.*?)\">(?<name>[^<]*?)</a></nobr>");

  // <a onclick='if (top.mainpane.focus) top.mainpane.focus();' class=nounder
  // href="place.php?whichplace=airport_stench" target=mainpane>Last
  // Adventure:</a></b></font><br><table cellspacing=0 cellpadding=0><tr><td><font size=2><a
  // onclick='if (top.mainpane.focus) top.mainpane.focus();' target=mainpane
  // href="adventure.php?snarfblat=442">Barf Mountain</a><br></font></td></tr></table>

  private static final Pattern expandedLastAdventurePattern =
      Pattern.compile(
          "<center><font.*?><b><a .*?href=\"(?<container>[^\"]*)\"[^>]*>Last Adventure:</a></b></font><br>"
              + "<table.*?><tr><td><font.*?><a .*?href=\"(?<link>[^\"]*)\">(?<name>[^<]*)</a><br></font></td></tr></table>(?<trail>.*?)</center>");
  private static final Pattern expandedTrailElementPattern =
      Pattern.compile("<font.*?><a [^>]*?href=\"(?<link>.*?)\">(?<name>[^<]*?)</a><br></font>");

  private static void setLastAdventure(final String responseText) {
    String adventureName = null;
    String adventureURL = null;
    String container = null;
    String trailString = null;
    if (CharPaneRequest.compactCharacterPane) {
      Matcher matcher = CharPaneRequest.compactLastAdventurePattern.matcher(responseText);
      if (matcher.find()) {
        adventureName = matcher.group(1);
        adventureURL = matcher.group(2);
      }

      matcher = compactTrailPattern.matcher(responseText);
      if (matcher.find()) {
        trailString = matcher.group("trail");
      }
    } else {
      Matcher matcher = CharPaneRequest.expandedLastAdventurePattern.matcher(responseText);
      if (matcher.find()) {
        adventureName = matcher.group("name");
        adventureURL = matcher.group("link");
        container = matcher.group("container");
        trailString = matcher.group("trail");
      }
    }

    if (adventureName == null || adventureName.equals("The Naughty Sorceress' Tower")) {
      return;
    }

    if (trailString != null) {
      List<String> trail = new ArrayList<>();
      if (!CharPaneRequest.compactCharacterPane) {
        trail.add(adventureName);
      }
      Matcher matcher = trailElementPattern.matcher(trailString);
      while (matcher.find()) {
        trail.add(matcher.group("name"));
      }
      Preferences.setString("lastAdventureTrail", String.join("|", trail));
    }

    CharPaneRequest.setLastAdventure("", adventureName, adventureURL, container);
  }

  private static void setLastAdventure(
      final String adventureId,
      final String adventureName,
      final String adventureURL,
      final String container) {
    KoLAdventure.lastZoneName = adventureName;
    CrystalBallManager.updateCrystalBallPredictions();

    if (KoLCharacter.inFight()) {
      return;
    }

    if (AdventureSpentDatabase.getNoncombatEncountered()
        && KoLCharacter.getCurrentRun() > AdventureSpentDatabase.getLastTurnUpdated()) {
      AdventureSpentDatabase.addTurn(KoLAdventure.lastLocationName);
    }
    AdventureSpentDatabase.setNoncombatEncountered(false);
    AdventureSpentDatabase.setLastTurnUpdated(KoLCharacter.getCurrentRun());

    if (adventureName == null || adventureName.equals("The Naughty Sorceress' Tower")) {
      return;
    }

    if (KoLCharacter.inChoice()) {
      return;
    }

    KoLAdventure adventure =
        KoLAdventure.setLastAdventure(adventureId, adventureName, adventureURL, container);

    if (KoLmafia.isRefreshing()) {
      KoLAdventure.setNextAdventure(adventure);
    }
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
      FamiliarData familiar = KoLCharacter.getFamiliar();
      familiar.setFeasted(responseText.contains("well-fed"));
      familiar.checkWeight(StringUtilities.parseInt(matcher.group(1)));
    }

    pattern = CharPaneRequest.familiarImagePattern;
    matcher = pattern.matcher(responseText);
    if (matcher.find()) {
      String directory = matcher.group(1);
      String image = matcher.group(2);
      int familiarId = KoLCharacter.getFamiliar().getId();
      if (image.startsWith("snow")) {
        SnowsuitCommand.check(responseText);
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
        FamiliarData familiar = KoLCharacter.usableFamiliar(id);
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

  private static final Pattern PIRATE_REALM_PATTERN =
      Pattern.compile("<b>(Guns|Grub|Grog|Gold|Fun):</b></td><td class=small>(\\d+)</td>");

  private static void checkPirateRealm(final String response) {
    // Only shows when we have a relevant last adventure
    switch (KoLAdventure.lastAdventureId()) {
      case AdventurePool.PIRATEREALM_ISLAND, AdventurePool.PIRATEREALM_SAILING:
        break;
      default:
        return;
    }

    PIRATE_REALM_PATTERN
        .matcher(response)
        .results()
        .forEach(
            match -> {
              String type = match.group(1);
              int value = StringUtilities.parseInt(match.group(2));
              var pref = type.equals("Fun") ? "availableFunPoints" : "_pirateRealm" + type;
              Preferences.setInteger(pref, value);
            });
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
      String mod = ModifierDatabase.parseModifier(res.toString());
      if (mod == null) {
        // this shouldn't happen...
        continue;
      }
      // Split into modifiers as some, like regeneration, get two values from one mod string
      ModifierList newModList = ModifierDatabase.splitModifiers(mod);

      // Iterate over modifiers
      for (ModifierValue modifier : newModList) {
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
    ModifierDatabase.overrideModifier(
        ModifierType.GENERATED, "Enchantments Absorbed", modList.toString());
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
        case 1 -> mask = "Mr. Mask";
        case 2 -> mask = "devil mask";
        case 3 -> mask = "protest mask";
        case 4 -> mask = "batmask";
        case 5 -> mask = "punk mask";
        case 6 -> mask = "hockey mask";
        case 7 -> mask = "bandit mask";
        case 8 -> mask = "plague doctor mask";
        case 9 -> mask = "robot mask";
        case 10 -> mask = "skull mask";
        case 11 -> mask = "monkey mask";
        case 12 -> mask = "luchador mask";
        case 13 -> mask = "welding mask";
        case 14 -> mask = "ninja mask";
        case 15 -> mask = "snowman mask";
        case 16 -> mask = "gasmask";
        case 17 -> mask = "fencing mask";
        case 18 -> mask = "opera mask";
        case 19 -> mask = "scary mask";
        case 20 -> mask = "alien mask";
        case 21 -> mask = "murderer mask";
        case 22 -> mask = "pumpkin mask";
        case 23 -> mask = "rabbit mask";
        case 24 -> mask = "ski mask";
        case 25 -> mask = "tiki mask";
        case 26 -> mask = "motorcycle mask";
        case 27 -> mask = "magical cartoon princess mask";
        case 28 -> mask = "catcher's mask";
        case 29 -> mask = "&quot;sexy&quot; mask";
        case 30 -> mask = "werewolf mask";
        case 100 -> mask = "Bonerdagon mask";
        case 101 -> mask = "Naughty Sorceress mask";
        case 102 -> mask = "Groar mask";
        case 103 -> mask = "Ed the Undying mask";
        case 104 -> mask = "Big Wisniewski mask";
        case 105 -> mask = "The Man mask";
        case 106 -> mask = "Boss Bat mask";
      }
    }
    KoLCharacter.setMask(mask);
  }

  private static final Pattern commaPattern =
      Pattern.compile("pound (.*?), Chameleon", Pattern.DOTALL);

  private static void checkComma(final String responseText) {
    Matcher commaMatcher = CharPaneRequest.commaPattern.matcher(responseText);
    var newRace = commaMatcher.find() ? commaMatcher.group(1) : "";

    if (!Preferences.getString("commaFamiliar").equals(newRace)) {
      KoLCharacter.currentFamiliar.deactivate();
      Preferences.setString("commaFamiliar", newRace);
      KoLCharacter.currentFamiliar.activate();
      // Some familiars can have different weight calculations.
      KoLCharacter.currentFamiliar.setWeight();
      KoLCharacter.recalculateAdjustments();
    }
  }

  private static final Pattern mediumPattern =
      Pattern.compile("images/medium_([0123]).gif", Pattern.DOTALL);

  private static void checkMedium(final String responseText) {
    Pattern pattern = CharPaneRequest.mediumPattern;
    Matcher mediumMatcher = pattern.matcher(responseText);
    if (mediumMatcher.find()) {
      int aura = StringUtilities.parseInt(mediumMatcher.group(1));
      FamiliarData fam = KoLCharacter.usableFamiliar(FamiliarPool.HAPPY_MEDIUM);
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

  public static void checkYouRobot(final String responseText) {
    if (!KoLCharacter.inRobocore()) {
      return;
    }

    if (!CharPaneRequest.compactCharacterPane) {
      YouRobotManager.parseAvatar(responseText);
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

  // <td align=right>Sweatiness:</td><td align=left><b><font color=black><span alt=""
  // title="">69%</span></font></td>
  private static final Pattern SWEATINESS =
      Pattern.compile("Sweatiness:</td><td.*?><b><font.*?><span.*?>([\\d]+)%</span></font></td>");

  public static void checkSweatiness(final String responseText) {
    if (!KoLCharacter.hasEquipped(ItemPool.DESIGNER_SWEATPANTS)
        && !KoLCharacter.hasEquipped(ItemPool.REPLICA_DESIGNER_SWEATPANTS)) {
      return;
    }

    Matcher matcher = SWEATINESS.matcher(responseText);

    // If we don't find the matcher but we're wearing the pants we have zero sweatiness
    int sweatiness = (matcher.find()) ? StringUtilities.parseInt(matcher.group(1)) : 0;
    Preferences.setInteger("sweat", sweatiness);
  }

  // <td align=right><span class='nes' style='line-height: 14px; font-size:
  // 12px;'>Score:</span></td><td align=left><font color=black><span class='nes' style='line-height:
  // 14px; font-size: 12px;'>0</span></font></td>

  private static final Pattern SCORE =
      Pattern.compile("<font color=(\\w+)><span class='nes'[^>]*?>([\\d,]+)</span></font>");

  public static void check8BitScore(final String responseText) {
    if (!KoLCharacter.hasEquipped(ItemPool.TRANSFUNCTIONER)) {
      return;
    }

    Matcher matcher = SCORE.matcher(responseText);

    if (matcher.find()) {
      Preferences.setInteger("8BitScore", StringUtilities.parseInt(matcher.group(2)));
      if (!matcher.group(1).equals(Preferences.getString("8BitColor"))) {
        Preferences.setString("8BitColor", matcher.group(1));
        Preferences.resetToDefault("8BitBonusTurns");
      }
    }
  }

  // <td align=right>Research Points:</td><td align=left><b>74</b></font></td>
  // <td align=right>Research:</td><td align=left><b>15</b></font></td>

  private static final Pattern RP =
      Pattern.compile(
          "<td align=right>Research(?: Points)?:</td><td align=left><b>([^<]+)</b></font></td>");

  // <td align=right>Until Transform:</td><td align=left><b>25</b></font></td>
  // <td align=right>Tranform:</td><td align=left><b>11</b></font></td>

  private static final Pattern TRANSFORM =
      Pattern.compile(
          "<td align=right>(?:Until )?Trans?form:</td><td align=left><b>([^<]+)</b></font></td>");

  public static void checkWereProfessor(final String responseText) {
    if (!KoLCharacter.inWereProfessor()) {
      return;
    }

    Matcher rmatcher = RP.matcher(responseText);
    if (rmatcher.find()) {
      Preferences.setInteger(
          "wereProfessorResearchPoints", StringUtilities.parseInt(rmatcher.group(1)));
    }

    Matcher tmatcher = TRANSFORM.matcher(responseText);
    if (tmatcher.find()) {
      Preferences.setInteger(
          "wereProfessorTransformTurns", StringUtilities.parseInt(tmatcher.group(1)));
    }
  }

  public static final void parseStatus(final JSONObject JSON) throws JSONException {
    int turnsThisRun = JSON.getIntValue("turnsthisrun");
    CharPaneRequest.turnsThisRun = turnsThisRun;

    int turnsPlayed = JSON.getIntValue("turnsplayed");
    KoLCharacter.setTurnsPlayed(turnsPlayed);

    if (KoLmafia.isRefreshing()) {
      // If we are refreshing status, simply set turns used
      KoLCharacter.setCurrentRun(turnsThisRun);
    } else {
      // Otherwise, increment them
      int mafiaTurnsThisRun = KoLCharacter.getCurrentRun();
      ResultProcessor.processAdventuresUsed(turnsThisRun - mafiaTurnsThisRun);
    }

    // Refresh effects before we set LimitMode since our "pseudo" LimitModes
    // are derived from currently active effects.
    CharPaneRequest.refreshEffects(JSON);

    Object lmo = JSON.get("limitmode");
    if (lmo instanceof Integer && lmo.equals(0)) {
      KoLCharacter.setLimitMode(LimitMode.NONE);
    } else if (lmo instanceof String s) {
      KoLCharacter.setLimitMode(s);
    } else {
      KoLmafia.updateDisplay("Unknown limit mode " + lmo.toString() + " received from API");
      KoLCharacter.setLimitMode(LimitMode.UNKNOWN);
    }

    JSONObject lastadv = JSON.getJSONObject("lastadv");
    String adventureId = lastadv.getString("id");
    String adventureName = lastadv.getString("name");
    String adventureURL = lastadv.getString("link");
    String container = Optional.ofNullable(lastadv.getString("container")).orElse("");
    CharPaneRequest.setLastAdventure(adventureId, adventureName, adventureURL, container);

    int fury = JSON.getIntValue("fury");
    KoLCharacter.setFuryNoCheck(fury);

    int soulsauce = JSON.getIntValue("soulsauce");
    KoLCharacter.setSoulsauce(soulsauce);

    if (KoLCharacter.isSneakyPete()) {
      int audience = 0;
      audience += JSON.getIntValue("petelove");
      audience -= JSON.getIntValue("petehate");
      KoLCharacter.setAudience(audience);
    }

    long hp = JSON.getLong("hp");
    long maxhp = JSON.getLong("maxhp");
    KoLCharacter.setHP(hp, maxhp, maxhp);

    if (KoLCharacter.inZombiecore()) {
      int horde = JSON.getIntValue("horde");
      KoLCharacter.setMP(horde, horde, horde);
    } else {
      long mp = JSON.getLong("mp");
      long maxmp = JSON.getLong("maxmp");
      KoLCharacter.setMP(mp, maxmp, maxmp);
    }

    long meat = JSON.getLong("meat");
    KoLCharacter.setAvailableMeat(meat);

    int drunk = JSON.getIntValue("drunk");
    KoLCharacter.setInebriety(drunk);

    int full = JSON.getIntValue("full");
    KoLCharacter.setFullness(full);

    int spleen = JSON.getIntValue("spleen");
    KoLCharacter.setSpleenUse(spleen);

    int adventures = JSON.getIntValue("adventures");
    KoLCharacter.setAdventuresLeft(adventures);

    int mcd = JSON.getIntValue("mcd");
    KoLCharacter.setMindControlLevel(mcd);

    int classType = JSON.getIntValue("class");
    KoLCharacter.setAscensionClass(classType);

    int pvpFights = JSON.getIntValue("pvpfights");
    KoLCharacter.setAttacksLeft(pvpFights);

    boolean hardcore = JSON.getIntValue("hardcore") == 1;
    KoLCharacter.setHardcore(hardcore);

    boolean casual = JSON.getIntValue("casual") == 1;
    KoLCharacter.setCasual(casual);

    var noncombatForcers = JSON.getJSONArray("noncomforcers");
    Preferences.setBoolean("noncombatForcerActive", noncombatForcers.size() > 0);
    Preferences.setString(
        "noncombatForcers", String.join("|", noncombatForcers.toList(String.class)));

    // boolean casual = JSON.getIntValue( "casual" ) == 1;
    int roninLeft = JSON.getIntValue("roninleft");

    if (KoLCharacter.inRaincore()) {
      int thunder = JSON.getIntValue("thunder");
      KoLCharacter.setThunder(thunder);
      int rain = JSON.getIntValue("rain");
      KoLCharacter.setRain(rain);
      int lightning = JSON.getIntValue("lightning");
      KoLCharacter.setLightning(lightning);
    }

    // *** Assume that roninleft always equals 0 if casual
    KoLCharacter.setRonin(roninLeft > 0);

    CharPaneRequest.setInteraction();

    if (KoLCharacter.getLimitMode().limitFamiliars()) {
      // No familiar
    } else if (KoLCharacter.inAxecore()) {
      int level = JSON.getIntValue("clancy_level");
      int itype = JSON.getIntValue("clancy_instrument");
      boolean att = JSON.getBoolean("clancy_wantsattention");
      AdventureResult instrument =
          itype == 1
              ? CharPaneRequest.SACKBUT
              : itype == 2 ? CharPaneRequest.CRUMHORN : itype == 3 ? CharPaneRequest.LUTE : null;
      KoLCharacter.setClancy(level, instrument, att);
    } else if (KoLCharacter.isJarlsberg()) {
      if (JSON.containsKey("jarlcompanion")) {
        int companion = JSON.getIntValue("jarlcompanion");
        switch (companion) {
          case 1 -> KoLCharacter.setCompanion(Companion.EGGMAN);
          case 2 -> KoLCharacter.setCompanion(Companion.RADISH);
          case 3 -> KoLCharacter.setCompanion(Companion.HIPPO);
          case 4 -> KoLCharacter.setCompanion(Companion.CREAM);
        }
      } else {
        KoLCharacter.setCompanion(null);
      }
    } else if (KoLCharacter.isEd()) {
      // No familiar, but may have a servant.  Unfortunately,
      // details of such are not in api.php
    } else if (KoLCharacter.getPath().canUseFamiliars()) {
      int famId = JSON.getIntValue("familiar");
      int famExp = JSON.getIntValue("familiarexp");
      FamiliarData familiar = FamiliarData.registerFamiliar(famId, famExp);
      KoLCharacter.setFamiliar(familiar);

      String image = JSON.getString("familiarpic");
      if (famId != FamiliarPool.MELODRAMEDARY) {
        KoLCharacter.setFamiliarImage(image.equals("") ? null : image + ".gif");
      }

      familiar.setFeasted(JSON.getIntValue("familiar_wellfed") == 1);

      // Set charges from the Medium's image

      if (famId == FamiliarPool.HAPPY_MEDIUM) {
        int aura = StringUtilities.parseInt(image.substring(7, 8));
        FamiliarData medium = KoLCharacter.usableFamiliar(FamiliarPool.HAPPY_MEDIUM);
        medium.setCharges(aura);
      }
    }

    int thrallId = JSON.getIntValue("pastathrall");
    int thrallLevel = JSON.getIntValue("pastathralllevel");

    PastaThrallData thrall = KoLCharacter.findPastaThrall(thrallId);
    if (thrall != null) {
      KoLCharacter.setPastaThrall(thrall);
      if (thrall != PastaThrallData.NO_THRALL) {
        thrall.update(thrallLevel, null);
      }
    }

    if (KoLCharacter.inNuclearAutumn()) {
      if (JSON.containsKey("radsickness")) {
        int rads = JSON.getIntValue("radsickness");
        KoLCharacter.setRadSickness(rads);
      } else {
        KoLCharacter.setRadSickness(0);
      }
    }
  }

  public static final void checkFamiliarWeight(final JSONObject JSON) throws JSONException {
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.getFamiliar().checkWeight(JSON.getIntValue("famlevel"));
  }

  private static void refreshEffects(final JSONObject JSON) throws JSONException {
    ArrayList<AdventureResult> visibleEffects = new ArrayList<>();

    Object o = JSON.get("effects");
    if (o instanceof JSONObject effects) {
      // KoL returns an empty JSON array if there are no effects
      Iterator<String> keys = effects.keySet().iterator();
      while (keys.hasNext()) {
        String descId = keys.next();
        JSONArray data = effects.getJSONArray(descId);
        String effectName = data.getString(0);
        int count = StringUtilities.parseInt(data.get(1).toString());

        AdventureResult effect = CharPaneRequest.extractEffect(descId, effectName, count);
        if (effect != null) {
          visibleEffects.add(effect);
        }
      }
    }

    o = JSON.get("intrinsics");
    if (o instanceof JSONObject intrinsics) {
      Iterator<String> keys = intrinsics.keySet().iterator();
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
