package net.sourceforge.kolmafia.session;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Modifiers.Modifier;
import net.sourceforge.kolmafia.Modifiers.ModifierList;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.BeachCombRequest;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest.Mushroom;
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest;
import net.sourceforge.kolmafia.request.DecorateTentRequest;
import net.sourceforge.kolmafia.request.EatItemRequest;
import net.sourceforge.kolmafia.request.EdBaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.FloristRequest.Florist;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GenieRequest;
import net.sourceforge.kolmafia.request.LatteRequest;
import net.sourceforge.kolmafia.request.MummeryRequest;
import net.sourceforge.kolmafia.request.PantogramRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SaberRequest;
import net.sourceforge.kolmafia.request.ScrapheapRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.SpelunkyRequest;
import net.sourceforge.kolmafia.request.SweetSynthesisRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.WildfireCampRequest;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.command.EdPieceCommand;
import net.sourceforge.kolmafia.textui.command.SnowsuitCommand;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.ClanFortuneDecorator;
import net.sourceforge.kolmafia.webui.MemoriesDecorator;
import net.sourceforge.kolmafia.webui.VillainLairDecorator;

public abstract class ChoiceManager {
  public static final GenericRequest CHOICE_HANDLER =
      new GenericRequest("choice.php") {
        @Override
        protected boolean shouldFollowRedirect() {
          return false;
        }
      };

  public static boolean handlingChoice = false;
  public static int lastChoice = 0;
  public static int lastDecision = 0;
  public static String lastResponseText = "";
  public static String lastDecoratedResponseText = "";

  private static int skillUses = 0;
  private static boolean canWalkAway;
  private static int abooPeakLevel = 0;

  private enum PostChoiceAction {
    NONE,
    INITIALIZE,
    ASCEND
  }

  private static PostChoiceAction action = PostChoiceAction.NONE;

  public static int currentChoice() {
    return ChoiceManager.handlingChoice ? ChoiceManager.lastChoice : 0;
  }

  public static int extractChoice(final String responseText) {
    int choice = ChoiceUtilities.extractChoice(responseText);

    if (choice == 0 && responseText.contains("<b>Lyle, LyleCo CEO</b>")) {
      // We still don't know the choice number, so take action here instead
      // We will either now, or in the past, have had Favored By Lyle
      Preferences.setBoolean("_lyleFavored", true);
    }

    return choice;
  }

  public static final Pattern URL_CHOICE_PATTERN = Pattern.compile("whichchoice=(\\d+)");

  public static int extractChoiceFromURL(final String urlString) {
    Matcher matcher = ChoiceManager.URL_CHOICE_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static final Pattern URL_OPTION_PATTERN = Pattern.compile("(?<!force)option=(\\d+)");

  public static int extractOptionFromURL(final String urlString) {
    Matcher matcher = ChoiceManager.URL_OPTION_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static final Pattern URL_IID_PATTERN = Pattern.compile("iid=(\\d+)");

  public static int extractIidFromURL(final String urlString) {
    Matcher matcher = ChoiceManager.URL_IID_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  public static final Pattern URL_QTY_PATTERN = Pattern.compile("qty=(\\d+)");

  public static int extractQtyFromURL(final String urlString) {
    Matcher matcher = ChoiceManager.URL_QTY_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  private static final Pattern URL_SKILLID_PATTERN = Pattern.compile("skillid=(\\d+)");
  private static final Pattern TATTOO_PATTERN =
      Pattern.compile("otherimages/sigils/hobotat(\\d+).gif");
  private static final Pattern REANIMATOR_ARM_PATTERN = Pattern.compile("(\\d+) arms??<br>");
  private static final Pattern REANIMATOR_LEG_PATTERN = Pattern.compile("(\\d+) legs??<br>");
  private static final Pattern REANIMATOR_SKULL_PATTERN = Pattern.compile("(\\d+) skulls??<br>");
  private static final Pattern REANIMATOR_WEIRDPART_PATTERN =
      Pattern.compile("(\\d+) weird random parts??<br>");
  private static final Pattern REANIMATOR_WING_PATTERN = Pattern.compile("(\\d+) wings??<br>");
  private static final Pattern CHAMBER_PATTERN = Pattern.compile("Chamber <b>#(\\d+)</b>");
  private static final Pattern YEARBOOK_TARGET_PATTERN =
      Pattern.compile("<b>Results:</b>.*?<b>(.*?)</b>");
  private static final Pattern UNPERM_PATTERN =
      Pattern.compile("Turning (.+)(?: \\(HP\\)) into (\\d+) karma.");
  private static final Pattern ICEHOUSE_PATTERN =
      Pattern.compile("perfectly-preserved (.*?), right");
  private static final Pattern CINDERELLA_TIME_PATTERN =
      Pattern.compile("<i>It is (\\d+) minute(?:s) to midnight.</i>");
  private static final Pattern CINDERELLA_SCORE_PATTERN =
      Pattern.compile("score (?:is now|was) <b>(\\d+)</b>");
  private static final Pattern RUMPLE_MATERIAL_PATTERN =
      Pattern.compile("alt=\"(.*?)\"></td><td valign=center>(\\d+)<");
  private static final Pattern MOTORBIKE_TIRES_PATTERN = Pattern.compile("<b>Tires:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_GASTANK_PATTERN =
      Pattern.compile("<b>Gas Tank:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_HEADLIGHT_PATTERN =
      Pattern.compile("<b>Headlight:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_COWLING_PATTERN =
      Pattern.compile("<b>Cowling:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_MUFFLER_PATTERN =
      Pattern.compile("<b>Muffler:</b> (.*?)?\\(");
  private static final Pattern MOTORBIKE_SEAT_PATTERN = Pattern.compile("<b>Seat:</b> (.*?)?\\(");
  private static final Pattern CRIMBOT_CHASSIS_PATTERN =
      Pattern.compile("base chassis is the (.*?),");
  private static final Pattern CRIMBOT_ARM_PATTERN =
      Pattern.compile("(?:My arm is the|</i> equipped with a) (.*?),");
  private static final Pattern CRIMBOT_PROPULSION_PATTERN =
      Pattern.compile(
          "(?:provided by a|am mobilized by an|equipped with a pair of|move via) (.*?),");
  private static final Pattern EDPIECE_PATTERN =
      Pattern.compile("<p>The crown is currently adorned with a golden (.*?).<center>");
  private static final Pattern ED_RETURN_PATTERN =
      Pattern.compile("Return to the fight! \\((\\d+) Ka\\)");
  private static final Pattern POOL_SKILL_PATTERN = Pattern.compile("(\\d+) Pool Skill</b>");
  private static final Pattern BENCH_WARRANT_PATTERN =
      Pattern.compile("creep <font color=blueviolet><b>(\\d+)</b></font> of them");
  private static final Pattern LYNYRD_PATTERN =
      Pattern.compile(
          "(?:scare|group of|All) <b>(\\d+)</b> (?:of the protesters|protesters|of them)");
  private static final Pattern PINK_WORD_PATTERN =
      Pattern.compile(
          "scrawled in lipstick on a cocktail napkin:  <b><font color=pink>(.*?)</font></b>");
  private static final Pattern OMEGA_PATTERN =
      Pattern.compile("<br>Current power level: (\\d+)%</td>");
  private static final Pattern RADIO_STATIC_PATTERN =
      Pattern.compile("<p>(?!(?:<form|</center>))(.+?)(?=<[^i</])");
  private static final Pattern STILL_PATTERN =
      Pattern.compile("toss (.*?) cocktail onions into the still");
  private static final Pattern QTY_PATTERN = Pattern.compile("qty(\\d+)=(\\d+)");
  private static final Pattern ITEMID_PATTERN = Pattern.compile("itemid(\\d+)=(\\d+)");
  private static final Pattern DINSEY_ROLLERCOASTER_PATTERN =
      Pattern.compile("rollercoaster is currently set to (.*?) Mode");
  private static final Pattern DINSEY_PIRATE_PATTERN =
      Pattern.compile("'Updated Pirate' is (lit|dark)");
  private static final Pattern DINSEY_TEACUP_PATTERN =
      Pattern.compile("'Current Teacup Spin Rate' points to (\\d+),000 RPM");
  private static final Pattern DINSEY_SLUICE_PATTERN =
      Pattern.compile("'Sluice Swishers' is currently in the (.*?) position");
  private static final Pattern MAYO_MINDER_PATTERN =
      Pattern.compile("currently loaded up with packets of (.*?)<p>");
  private static final Pattern DESCID_PATTERN = Pattern.compile("descitem\\((.*?)\\)");
  private static final Pattern WLF_PATTERN =
      Pattern.compile(
          "<form action=choice.php>.*?<b>(.*?)</b>.*?descitem\\((.*?)\\).*?>(.*?)<.*?name=option value=([\\d]*).*?</form>",
          Pattern.DOTALL);
  private static final Pattern WLF_COUNT_PATTERN = Pattern.compile(".*? \\(([\\d]+)\\)$");
  private static final Pattern WALFORD_PATTERN =
      Pattern.compile("\\(Walford's bucket filled by (\\d+)%\\)");
  private static final Pattern SNOJO_CONSOLE_PATTERN = Pattern.compile("<b>(.*?) MODE</b>");
  private static final Pattern TELEGRAM_PATTERN = Pattern.compile("value=\"RE: (.*?)\"");
  private static final Pattern ENLIGHTENMENT_PATTERN =
      Pattern.compile("achieved <b>(\\d+)</b> enlightenment");
  private static final Pattern ORACLE_QUEST_PATTERN =
      Pattern.compile("don't remember leaving any spoons in (.*?)&quot;");
  private static final Pattern CASE_PATTERN = Pattern.compile("\\((\\d+) more case");
  private static final Pattern TIME_SPINNER_PATTERN = Pattern.compile("have (\\d+) minute");
  private static final Pattern TIME_SPINNER_MEDALS_PATTERN =
      Pattern.compile("memory of earning <b>(\\d+) medal");
  private static final Pattern LOV_EXIT_PATTERN =
      Pattern.compile("a sign above it that says <b>(.*?)</b>");
  private static final Pattern LOV_LOGENTRY_PATTERN = Pattern.compile("you scrawl <b>(.*?)</b>");
  private static final Pattern VACCINE_PATTERN =
      Pattern.compile("option value=(\\d+).*?class=button type=submit value=\"([^\"]*)");
  private static final Pattern DECEASED_TREE_PATTERN =
      Pattern.compile("Looks like it has (.*?) needle");
  private static final Pattern BROKEN_CHAMPAGNE_PATTERN =
      Pattern.compile("Looks like it has (\\d+) ounce");
  private static final Pattern GARBAGE_SHIRT_PATTERN =
      Pattern.compile("Looks like you can read roughly (\\d+) scrap");
  private static final Pattern BOOMBOX_PATTERN = Pattern.compile("you can do <b>(\\d+)</b> more");
  private static final Pattern BOOMBOX_SONG_PATTERN =
      Pattern.compile("&quot;(.*?)&quot;( \\(Keep playing\\)|)");
  private static final Pattern HEIST_PATTERN =
      Pattern.compile("He shows you a list of potential targets:<p><i>\\((\\d+) more");
  private static final Pattern SHEN_PATTERN =
      Pattern.compile(
          "(?:Bring me|artifact known only as) <b>(.*?)</b>, hidden away for centuries");
  private static final Pattern BASTILLE_PATTERN = Pattern.compile("You can play <b>(\\d+)</b>");
  private static final Pattern GERALD_PATTERN =
      Pattern.compile("Gerald wants (\\d+)<table>.*?descitem\\((\\d+)\\)");
  private static final Pattern GERALDINE_PATTERN =
      Pattern.compile("Geraldine wants (\\d+)<table>.*?descitem\\((\\d+)\\)");
  private static final Pattern SAFE_PATTERN = Pattern.compile("find ([\\d,]+) Meat in the safe");
  private static final Pattern TRASH_PATTERN =
      Pattern.compile("must have been (\\d+) pieces of trash");
  private static final Pattern VOTE_PATTERN =
      Pattern.compile(
          "<label><input .*? value=\\\"(\\d)\\\" class=\\\"locals\\\" /> (.*?)<br /><span .*? color: blue\\\">(.*?)</span><br /></label>");
  private static final Pattern VOTE_SPEECH_PATTERN =
      Pattern.compile(
          "<p><input type='radio' name='g' value='(\\d+)' /> <b>(.*?)</b>(.*?)<br><blockquote>(.*?)</blockquote>");
  private static final Pattern URL_VOTE_PATTERN = Pattern.compile("local\\[\\]=(\\d)");
  private static final Pattern EARLY_DAYCARE_PATTERN =
      Pattern.compile("mostly empty. (.*?) toddlers are training with (.*?) instructor");
  private static final Pattern DAYCARE_PATTERN =
      Pattern.compile(
          "(?:Looks like|Probably around) (.*?) pieces in all. (.*?) toddlers are training with (.*?) instructor");
  private static final Pattern DAYCARE_RECRUITS_PATTERN =
      Pattern.compile("<font color=blue><b>[(.*?) Meat]</b></font>");
  private static final Pattern DAYCARE_RECRUIT_PATTERN =
      Pattern.compile("attract (.*?) new children");
  private static final Pattern DAYCARE_EQUIPMENT_PATTERN =
      Pattern.compile("manage to find (.*?) used");
  private static final Pattern DAYCARE_ITEM_PATTERN =
      Pattern.compile("<td valign=center>You lose an item: </td>.*?<b>(.*?)</b> \\((.*?)\\)</td>");
  private static final Pattern SAUSAGE_PATTERN =
      Pattern.compile(
          "grinder needs (.*?) of the (.*?) required units of filling to make a sausage.  Your grinder reads \\\"(\\d+)\\\" units.");
  private static final Pattern DOCTOR_BAG_PATTERN =
      Pattern.compile("We've received a report of a patient (.*?), in (.*?)\\.");
  private static final Pattern RED_SNAPPER_PATTERN =
      Pattern.compile("guiding you towards: <b>(.*?)</b>.  You've found <b>(\\d+)</b> of them");
  private static final Pattern MUSHROOM_COSTUME_PATTERN =
      Pattern.compile(
          "<form.*?name=option value=(\\d).*?type=submit value=\"(.*?) Costume\".*?>(\\d+) coins<.*?</form>");
  private static final Pattern MUSHROOM_BADGE_PATTERN =
      Pattern.compile("Current cost: (\\d+) coins.");
  private static final Pattern GUZZLR_TIER_PATTERN =
      Pattern.compile("You have completed ([0-9,]+) (Bronze|Gold|Platinum) Tier deliveries");
  private static final Pattern GUZZLR_QUEST_PATTERN =
      Pattern.compile("<p>You are currently tasked with taking a (.*?) to (.*?) in (.*?)\\.<p>");

  public static final Pattern DECISION_BUTTON_PATTERN =
      Pattern.compile(
          "<input type=hidden name=option value=(\\d+)>(?:.*?)<input +class=button type=submit value=\"(.*?)\">");

  private static final AdventureResult PAPAYA = ItemPool.get(ItemPool.PAPAYA, 1);
  private static final AdventureResult MAIDEN_EFFECT = EffectPool.get(EffectPool.DREAMS_AND_LIGHTS);
  private static final AdventureResult MODEL_AIRSHIP = ItemPool.get(ItemPool.MODEL_AIRSHIP, 1);

  private static final AdventureResult CURSE1_EFFECT = EffectPool.get(EffectPool.ONCE_CURSED);
  private static final AdventureResult CURSE2_EFFECT = EffectPool.get(EffectPool.TWICE_CURSED);
  private static final AdventureResult CURSE3_EFFECT = EffectPool.get(EffectPool.THRICE_CURSED);
  private static final AdventureResult MCCLUSKY_FILE = ItemPool.get(ItemPool.MCCLUSKY_FILE, 1);
  private static final AdventureResult MCCLUSKY_FILE_PAGE5 =
      ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE5, 1);
  private static final AdventureResult BINDER_CLIP = ItemPool.get(ItemPool.BINDER_CLIP, 1);
  private static final AdventureResult STONE_TRIANGLE = ItemPool.get(ItemPool.STONE_TRIANGLE, 1);

  private static final AdventureResult JOCK_EFFECT =
      EffectPool.get(EffectPool.JAMMING_WITH_THE_JOCKS);
  private static final AdventureResult NERD_EFFECT = EffectPool.get(EffectPool.NERD_IS_THE_WORD);
  private static final AdventureResult GREASER_EFFECT = EffectPool.get(EffectPool.GREASER_LIGHTNIN);

  // Dreadsylvania items and effects
  private static final AdventureResult MOON_AMBER_NECKLACE =
      ItemPool.get(ItemPool.MOON_AMBER_NECKLACE, 1);
  private static final AdventureResult BLOODY_KIWITINI = ItemPool.get(ItemPool.BLOODY_KIWITINI, 1);
  private static final AdventureResult KIWITINI_EFFECT =
      EffectPool.get(EffectPool.FIRST_BLOOD_KIWI);
  private static final AdventureResult AUDITORS_BADGE = ItemPool.get(ItemPool.AUDITORS_BADGE, 1);
  private static final AdventureResult WEEDY_SKIRT = ItemPool.get(ItemPool.WEEDY_SKIRT, 1);
  private static final AdventureResult GHOST_SHAWL = ItemPool.get(ItemPool.GHOST_SHAWL, 1);
  private static final AdventureResult SHEPHERDS_PIE = ItemPool.get(ItemPool.SHEPHERDS_PIE, 1);
  private static final AdventureResult PIE_EFFECT = EffectPool.get(EffectPool.SHEPHERDS_BREATH);
  private static final AdventureResult MAKESHIFT_TURBAN =
      ItemPool.get(ItemPool.MAKESHIFT_TURBAN, 1);
  private static final AdventureResult TEMPORARY_BLINDNESS =
      EffectPool.get(EffectPool.TEMPORARY_BLINDNESS);
  private static final AdventureResult HELPS_YOU_SLEEP = ItemPool.get(ItemPool.HELPS_YOU_SLEEP, 1);
  private static final AdventureResult SLEEP_MASK = ItemPool.get(ItemPool.SLEEP_MASK, 1);

  private static final AdventureResult[] MISTRESS_ITEMS =
      new AdventureResult[] {
        ItemPool.get(ItemPool.CHINTZY_SEAL_PENDANT, 1),
        ItemPool.get(ItemPool.CHINTZY_TURTLE_BROOCH, 1),
        ItemPool.get(ItemPool.CHINTZY_NOODLE_RING, 1),
        ItemPool.get(ItemPool.CHINTZY_SAUCEPAN_EARRING, 1),
        ItemPool.get(ItemPool.CHINTZY_DISCO_BALL_PENDANT, 1),
        ItemPool.get(ItemPool.CHINTZY_ACCORDION_PIN, 1),
        ItemPool.get(ItemPool.ANTIQUE_HAND_MIRROR, 1),
      };

  private static final Pattern HELLEVATOR_PATTERN =
      Pattern.compile(
          "the (lobby|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|eleventh) (button|floor)");

  private static final String[] FLOORS =
      new String[] {
        "lobby",
        "first",
        "second",
        "third",
        "fourth",
        "fifth",
        "sixth",
        "seventh",
        "eighth",
        "ninth",
        "tenth",
        "eleventh",
      };

  private static final String[][] OLD_MAN_PSYCHOSIS_SPOILERS = {
    {"Draw a Monster with a Crayon", "-1 Crayon, Add Cray-Kin"},
    {"Build a Bubble Mountain", "+3 crew, -8-10 bubbles"},
    {"Ask Mom for More Bath Toys", "+2 crayons, +8-11 bubbles"},
    {"Draw a Bunch of Coconuts with Crayons", "Block Ferocious roc, -2 crayons"},
    {"Splash in the Water", "Add Bristled Man-O-War"},
    {"Draw a Big Storm Cloud on the Shower Wall", "Block Deadly Hydra, -3 crayons"},
    {"Knock an Action Figure Overboard", "+20-23 bubbles, -1 crew"},
    {"Submerge Some Bubbles", "Block giant man-eating shark, -16 bubbles"},
    {"Turn on the Shower Wand", "Add Deadly Hydra"},
    {"Dump Bubble Bottle and Turn on the Faucet", "+13-19 bubbles"},
    {"Put the Toy Boat on the Side of the Tub", "+4 crayon, -1 crew"},
    {"Cover the Ship in Bubbles", "Block fearsome giant squid, -13-20 bubbles"},
    {"Pull the Drain Plug", "-8 crew, -3 crayons, -17 bubbles, increase NC rate"},
    {"Open a New Bathtub Crayon Box", "+3 crayons"},
    {"Sing a Bathtime Tune", "+3 crayons, +16 bubbles, -2 crew"},
    {"Surround Bubbles with Crayons", "+5 crew, -6-16 bubbles, -2 crayons"},
  };

  public static final Map<Quest, String> conspiracyQuestMessages = new HashMap<>();

  static {
    ChoiceManager.conspiracyQuestMessages.put(
        Quest.CLIPPER,
        "&quot;Attention any available operative. Attention any available operative. A reward has been posted for DNA evidence gathered from Lt. Weirdeaux's subjects inside Site 15. The DNA is to be gathered via keratin extraction. Message repeats.&quot;");
    ChoiceManager.conspiracyQuestMessages.put(
        Quest.EVE,
        "&quot;Attention Operative 01-A-A. General Sitterson reports a... situation involving experiment E-V-E-6. Military intervention has been requested. Message repeats.&quot;");
    ChoiceManager.conspiracyQuestMessages.put(
        Quest.FAKE_MEDIUM,
        "&quot;Attention Operative EC-T-1. An outside client has expressed interest in the acquisition of an ESP suppression collar from the laboratory. Operationally significant sums of money are involved. Message repeats.&quot;");
    ChoiceManager.conspiracyQuestMessages.put(
        Quest.GORE,
        "&quot;Attention any available operative. Attention any available operative. Laboratory overseer General Sitterson reports unacceptable levels of environmental gore. Several elevator shafts are already fully clogged, limiting staff mobility, and several surveillance camera lenses have been rendered opaque, placing the validity of experimental data at risk. Immediate janitorial assistance is requested. Message repeats.&quot;");
    ChoiceManager.conspiracyQuestMessages.put(
        Quest.JUNGLE_PUN,
        "&quot;Attention any available operative. Attention any available operative. The director of Project Buena Vista has posted a significant bounty for the collection of jungle-related puns. Repeat: Jungle-related puns. Non-jungle puns or jungle non-puns will not be accepted. Non-jungle non-puns, by order of the director, are to be rewarded with summary execution. Message repeats.&quot;");
    ChoiceManager.conspiracyQuestMessages.put(
        Quest.OUT_OF_ORDER,
        "&quot;Attention Operative QZ-N-0. Colonel Kurzweil at Jungle Interior Camp 4 reports the theft of Project T. L. B. materials. Requests immediate assistance. Is confident that it has not yet been removed from the jungle. Message repeats.&quot;");
    ChoiceManager.conspiracyQuestMessages.put(
        Quest.SERUM,
        "&quot;Attention Operative 21-B-M. Emergency deployment orders have been executed due to a shortage of experimental serum P-00. Repeat: P Zero Zero. Lt. Weirdeaux is known to have P-00 manufacturing facilities inside the Site 15 mansion. Message repeats.&quot;");
    ChoiceManager.conspiracyQuestMessages.put(
        Quest.SMOKES,
        "&quot;Attention Operative 00-A-6. Colonel Kurzweil at Jungle Interior Camp 4 reports that they have run out of smokes. Repeat: They have run out of smokes. Requests immediate assistance. Message repeats.&quot;");
  }

  public static class Option {
    private final String name;
    private final int option;
    private final AdventureResult item;

    public Option(final String name) {
      this(name, 0, null);
    }

    public Option(final String name, final int option) {
      this(name, option, null);
    }

    public Option(final String name, final String item) {
      this(name, 0, item);
    }

    public Option(final String name, final int option, final String item) {
      this.name = name;
      this.option = option;
      int itemId = ItemDatabase.getItemId(item);
      this.item = item != null ? ItemPool.get(itemId) : null;
    }

    public String getName() {
      return this.name;
    }

    public int getOption() {
      return this.option;
    }

    public int getDecision(final int def) {
      return this.option == 0 ? def : this.option;
    }

    public AdventureResult getItem() {
      return this.item;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public static class ChoiceAdventure implements Comparable<ChoiceAdventure> {
    private final int choice;
    private final String zone;
    private final String setting;
    private final String name;
    private final int ordering;

    private final Object[] options;
    private final Object[][] spoilers;

    public ChoiceAdventure(
        final String zone, final String setting, final String name, final Object[] options) {
      this(zone, setting, name, options, 0);
    }

    public ChoiceAdventure(
        final String zone,
        final String setting,
        final String name,
        final Object[] options,
        final int ordering) {
      this.zone = zone;
      this.setting = setting;
      this.choice = setting.equals("none") ? 0 : StringUtilities.parseInt(setting.substring(15));
      this.name = name;
      this.options = options;
      this.spoilers = new Object[][] {new String[] {setting}, new String[] {name}, options};
      this.ordering = ordering;
    }

    public int getChoice() {
      return this.choice;
    }

    public String getZone() {
      return this.zone;
    }

    public String getSetting() {
      return this.setting;
    }

    public String getName() {
      return this.name;
    }

    public Object[] getOptions() {
      return (this.options == null)
          ? ChoiceManager.dynamicChoiceOptions(this.setting)
          : this.options;
    }

    public Object[][] getSpoilers() {
      return this.spoilers;
    }

    public int compareTo(final ChoiceAdventure o) {
      // Choices can have a specified relative ordering
      // within zone regardless of name or choice number
      if (this.ordering != o.ordering) {
        return this.ordering - o.ordering;
      }

      if (ChoiceManager.choicesOrderedByName) {
        int result = this.name.compareToIgnoreCase(o.name);

        if (result != 0) {
          return result;
        }
      }

      return this.choice - o.choice;
    }
  }

  // A ChoiceSpoiler is a ChoiceAdventure that isn't user-configurable.
  // The zone is optional, since it doesn't appear in the choiceadv GUI.
  public static class ChoiceSpoiler extends ChoiceAdventure {
    public ChoiceSpoiler(final String setting, final String name, final Object[] options) {
      super("Unsorted", setting, name, options);
    }

    public ChoiceSpoiler(
        final String zone, final String setting, final String name, final Object[] options) {
      super(zone, setting, name, options);
    }
  }

  // NULLCHOICE is returned for failed lookups, so the caller doesn't have to do null checks.
  public static final ChoiceSpoiler NULLCHOICE = new ChoiceSpoiler("none", "none", new String[] {});

  private static boolean choicesOrderedByName = true;

  public static final void setChoiceOrdering(final boolean choicesOrderedByName) {
    ChoiceManager.choicesOrderedByName = choicesOrderedByName;
  }

  private static final Object[] CHOICE_DATA = {
    // Choice 1 is unknown

    // Denim Axes Examined
    new ChoiceSpoiler(
        "choiceAdventure2",
        "Palindome",
        new Object[] {
          new Option("denim axe", "denim axe"), new Option("skip adventure", "rubber axe")
        }),
    // Denim Axes Examined
    new Object[] {IntegerPool.get(2), IntegerPool.get(1), ItemPool.get(ItemPool.RUBBER_AXE, -1)},

    // The Oracle Will See You Now
    new ChoiceSpoiler(
        "choiceAdventure3",
        "Teleportitis",
        new Object[] {"skip adventure", "randomly sink 100 meat", "make plus sign usable"}),

    // Finger-Lickin'... Death.
    new ChoiceAdventure(
        "Beach",
        "choiceAdventure4",
        "South of the Border",
        new Object[] {
          "small meat boost", new Option("try for poultrygeist", "poultrygeist"), "skip adventure"
        }),
    // Finger-Lickin'... Death.
    new Object[] {
      IntegerPool.get(4), IntegerPool.get(1), new AdventureResult(AdventureResult.MEAT, -500)
    },
    new Object[] {
      IntegerPool.get(4), IntegerPool.get(2), new AdventureResult(AdventureResult.MEAT, -500)
    },

    // Heart of Very, Very Dark Darkness
    new ChoiceAdventure(
        "MusSign",
        "choiceAdventure5",
        "Gravy Barrow",
        new Object[] {"fight the fairy queen", "skip adventure"}),

    // Darker Than Dark
    new ChoiceSpoiler(
        "choiceAdventure6", "Gravy Barrow", new Object[] {"get Beaten Up", "skip adventure"}),

    // Choice 7 is How Depressing

    // On the Verge of a Dirge -> Self Explanatory
    new ChoiceSpoiler(
        "choiceAdventure8",
        "Gravy Barrow",
        new Object[] {"enter the chamber", "enter the chamber", "enter the chamber"}),

    // Wheel In the Sky Keep on Turning: Muscle Position
    new ChoiceSpoiler(
        "choiceAdventure9",
        "Castle Wheel",
        new Object[] {"Turn to mysticality", "Turn to moxie", "Leave at muscle"}),

    // Wheel In the Sky Keep on Turning: Mysticality Position
    new ChoiceSpoiler(
        "choiceAdventure10",
        "Castle Wheel",
        new Object[] {"Turn to Map Quest", "Turn to muscle", "Leave at mysticality"}),

    // Wheel In the Sky Keep on Turning: Map Quest Position
    new ChoiceSpoiler(
        "choiceAdventure11",
        "Castle Wheel",
        new Object[] {"Turn to moxie", "Turn to mysticality", "Leave at map quest"}),

    // Wheel In the Sky Keep on Turning: Moxie Position
    new ChoiceSpoiler(
        "choiceAdventure12",
        "Castle Wheel",
        new Object[] {"Turn to muscle", "Turn to map quest", "Leave at moxie"}),

    // Choice 13 is unknown

    // A Bard Day's Night
    new ChoiceAdventure(
        "Knob",
        "choiceAdventure14",
        "Cobb's Knob Harem",
        new Object[] {
          new Option("Knob goblin harem veil", "Knob goblin harem veil"),
          new Option("Knob goblin harem pants", "Knob goblin harem pants"),
          "small meat boost",
          "complete the outfit"
        }),

    // Yeti Nother Hippy
    new ChoiceAdventure(
        "McLarge",
        "choiceAdventure15",
        "eXtreme Slope",
        new Object[] {
          new Option("eXtreme mittens", "eXtreme mittens"),
          new Option("eXtreme scarf", "eXtreme scarf"),
          "small meat boost",
          "complete the outfit"
        }),

    // Saint Beernard
    new ChoiceAdventure(
        "McLarge",
        "choiceAdventure16",
        "eXtreme Slope",
        new Object[] {
          new Option("snowboarder pants", "snowboarder pants"),
          new Option("eXtreme scarf", "eXtreme scarf"),
          "small meat boost",
          "complete the outfit"
        }),

    // Generic Teen Comedy
    new ChoiceAdventure(
        "McLarge",
        "choiceAdventure17",
        "eXtreme Slope",
        new Object[] {
          new Option("eXtreme mittens", "eXtreme mittens"),
          new Option("snowboarder pants", "snowboarder pants"),
          "small meat boost",
          "complete the outfit"
        }),

    // A Flat Miner
    new ChoiceAdventure(
        "McLarge",
        "choiceAdventure18",
        "Itznotyerzitz Mine",
        new Object[] {
          new Option("miner's pants", "miner's pants"),
          new Option("7-Foot Dwarven mattock", "7-Foot Dwarven mattock"),
          "small meat boost",
          "complete the outfit"
        }),

    // 100% Legal
    new ChoiceAdventure(
        "McLarge",
        "choiceAdventure19",
        "Itznotyerzitz Mine",
        new Object[] {
          new Option("miner's helmet", "miner's helmet"),
          new Option("miner's pants", "miner's pants"),
          "small meat boost",
          "complete the outfit"
        }),

    // See You Next Fall
    new ChoiceAdventure(
        "McLarge",
        "choiceAdventure20",
        "Itznotyerzitz Mine",
        new Object[] {
          new Option("miner's helmet", "miner's helmet"),
          new Option("7-Foot Dwarven mattock", "7-Foot Dwarven mattock"),
          "small meat boost",
          "complete the outfit"
        }),

    // Under the Knife
    new ChoiceAdventure(
        "Town",
        "choiceAdventure21",
        "Sleazy Back Alley",
        new Object[] {"switch genders", "skip adventure"}),
    // Under the Knife
    new Object[] {
      IntegerPool.get(21), IntegerPool.get(1), new AdventureResult(AdventureResult.MEAT, -500)
    },

    // The Arrrbitrator
    new ChoiceAdventure(
        "Island",
        "choiceAdventure22",
        "Pirate's Cove",
        new Object[] {
          new Option("eyepatch", "eyepatch"),
          new Option("swashbuckling pants", "swashbuckling pants"),
          "small meat boost",
          "complete the outfit"
        }),

    // Barrie Me at Sea
    new ChoiceAdventure(
        "Island",
        "choiceAdventure23",
        "Pirate's Cove",
        new Object[] {
          new Option("stuffed shoulder parrot", "stuffed shoulder parrot"),
          new Option("swashbuckling pants", "swashbuckling pants"),
          "small meat boost",
          "complete the outfit"
        }),

    // Amatearrr Night
    new ChoiceAdventure(
        "Island",
        "choiceAdventure24",
        "Pirate's Cove",
        new Object[] {
          new Option("stuffed shoulder parrot", "stuffed shoulder parrot"),
          "small meat boost",
          new Option("eyepatch", "eyepatch"),
          "complete the outfit"
        }),

    // Ouch! You bump into a door!
    new ChoiceAdventure(
        "Dungeon",
        "choiceAdventure25",
        "Dungeon of Doom",
        new Object[] {
          new Option("magic lamp", "magic lamp"),
          new Option("dead mimic", "dead mimic"),
          "skip adventure"
        }),
    // Ouch! You bump into a door!
    new Object[] {
      IntegerPool.get(25), IntegerPool.get(1), new AdventureResult(AdventureResult.MEAT, -50)
    },
    new Object[] {
      IntegerPool.get(25), IntegerPool.get(2), new AdventureResult(AdventureResult.MEAT, -5000)
    },

    // A Three-Tined Fork
    new ChoiceSpoiler(
        "Woods",
        "choiceAdventure26",
        "Spooky Forest",
        new Object[] {"muscle classes", "mysticality classes", "moxie classes"}),

    // Footprints
    new ChoiceSpoiler(
        "Woods",
        "choiceAdventure27",
        "Spooky Forest",
        new Object[] {AscensionClass.SEAL_CLUBBER, AscensionClass.TURTLE_TAMER}),

    // A Pair of Craters
    new ChoiceSpoiler(
        "Woods",
        "choiceAdventure28",
        "Spooky Forest",
        new Object[] {AscensionClass.PASTAMANCER, AscensionClass.SAUCEROR}),

    // The Road Less Visible
    new ChoiceSpoiler(
        "Woods",
        "choiceAdventure29",
        "Spooky Forest",
        new Object[] {AscensionClass.DISCO_BANDIT, AscensionClass.ACCORDION_THIEF}),

    // Choices 30 - 39 are unknown

    // The Effervescent Fray
    new ChoiceAdventure(
        "Rift",
        "choiceAdventure40",
        "Cola Wars",
        new Object[] {
          new Option("Cloaca-Cola fatigues", "Cloaca-Cola fatigues"),
          new Option("Dyspepsi-Cola shield", "Dyspepsi-Cola shield"),
          "mysticality substats"
        }),

    // Smells Like Team Spirit
    new ChoiceAdventure(
        "Rift",
        "choiceAdventure41",
        "Cola Wars",
        new Object[] {
          new Option("Dyspepsi-Cola fatigues", "Dyspepsi-Cola fatigues"),
          new Option("Cloaca-Cola helmet", "Cloaca-Cola helmet"),
          "muscle substats"
        }),

    // What is it Good For?
    new ChoiceAdventure(
        "Rift",
        "choiceAdventure42",
        "Cola Wars",
        new Object[] {
          new Option("Dyspepsi-Cola helmet", "Dyspepsi-Cola helmet"),
          new Option("Cloaca-Cola shield", "Cloaca-Cola shield"),
          "moxie substats"
        }),

    // Choices 43 - 44 are unknown

    // Maps and Legends
    new ChoiceSpoiler(
        "Woods",
        "choiceAdventure45",
        "Spooky Forest",
        new Object[] {
          new Option("Spooky Temple map", "Spooky Temple map"), "skip adventure", "skip adventure"
        }),

    // An Interesting Choice
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure46",
        "Spooky Forest Vampire",
        new Object[] {
          "moxie substats", "muscle substats", new Option("vampire heart", "vampire heart")
        }),

    // Have a Heart
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure47",
        "Spooky Forest Vampire Hunter",
        new Object[] {
          new Option("bottle of used blood", "bottle of used blood"),
          new Option("skip adventure and keep vampire hearts", "vampire heart")
        }),
    // Have a Heart
    // This trades all vampire hearts for an equal number of
    // bottles of used blood.
    new Object[] {IntegerPool.get(47), IntegerPool.get(1), ItemPool.get(ItemPool.VAMPIRE_HEART, 1)},

    // Choices 48 - 70 are violet fog adventures
    // Choice 71 is A Journey to the Center of Your Mind

    // Lording Over The Flies
    new ChoiceAdventure(
        "Island",
        "choiceAdventure72",
        "Frat House",
        new Object[] {
          new Option("around the world", "around the world"),
          new Option("skip adventure", "Spanish fly")
        }),
    // Lording Over The Flies
    // This trades all Spanish flies for around the worlds,
    // in multiples of 5.  Excess flies are left in inventory.
    new Object[] {IntegerPool.get(72), IntegerPool.get(1), ItemPool.get(ItemPool.SPANISH_FLY, 5)},

    // Don't Fence Me In
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure73",
        "Whitey's Grove",
        new Object[] {
          "muscle substats",
          new Option("white picket fence", "white picket fence"),
          new Option("wedding cake, white rice 3x (+2x w/ rice bowl)", "piece of wedding cake")
        }),

    // The Only Thing About Him is the Way That He Walks
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure74",
        "Whitey's Grove",
        new Object[] {
          "moxie substats",
          new Option("boxed wine", "boxed wine"),
          new Option("mullet wig", "mullet wig")
        }),

    // Rapido!
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure75",
        "Whitey's Grove",
        new Object[] {
          "mysticality substats",
          new Option("white lightning", "white lightning"),
          new Option("white collar", "white collar")
        }),

    // Junction in the Trunction
    new ChoiceAdventure(
        "Knob",
        "choiceAdventure76",
        "Knob Shaft",
        new Object[] {
          new Option("cardboard ore", "cardboard ore"),
          new Option("styrofoam ore", "styrofoam ore"),
          new Option("bubblewrap ore", "bubblewrap ore")
        }),

    // History is Fun!
    new ChoiceSpoiler(
        "choiceAdventure86",
        "Haunted Library",
        new Object[] {"Spookyraven Chapter 1", "Spookyraven Chapter 2", "Spookyraven Chapter 3"}),

    // History is Fun!
    new ChoiceSpoiler(
        "choiceAdventure87",
        "Haunted Library",
        new Object[] {"Spookyraven Chapter 4", "Spookyraven Chapter 5", "Spookyraven Chapter 6"}),

    // Naughty, Naughty
    new ChoiceSpoiler(
        "choiceAdventure88",
        "Haunted Library",
        new Object[] {"mysticality substats", "moxie substats", "Fettucini / Scarysauce"}),
    new ChoiceSpoiler(
        "choiceAdventure89",
        "Haunted Gallery",
        new Object[] {"Wolf Knight", "Snake Knight", "Dreams and Lights", "skip adventure"}),

    // Curtains
    new ChoiceAdventure(
        "Manor2",
        "choiceAdventure90",
        "Haunted Ballroom",
        new Object[] {"enter combat", "moxie substats", "skip adventure"}),

    // Having a Medicine Ball
    new ChoiceAdventure(
        "Manor2",
        "choiceAdventure105",
        "Haunted Bathroom",
        new Object[] {"mysticality substats", "other options", "guy made of bees"}),

    // Strung-Up Quartet
    new ChoiceAdventure(
        "Manor2",
        "choiceAdventure106",
        "Haunted Ballroom",
        new Object[] {
          "increase monster level",
          "decrease combat frequency",
          "increase item drops",
          "disable song"
        }),

    // Bad Medicine is What You Need
    new ChoiceAdventure(
        "Manor2",
        "choiceAdventure107",
        "Haunted Bathroom",
        new Object[] {
          new Option("antique bottle of cough syrup", "antique bottle of cough syrup"),
          new Option("tube of hair oil", "tube of hair oil"),
          new Option("bottle of ultravitamins", "bottle of ultravitamins"),
          "skip adventure"
        }),

    // Aww, Craps
    new ChoiceAdventure(
        "Town",
        "choiceAdventure108",
        "Sleazy Back Alley",
        new Object[] {"moxie substats", "meat and moxie", "random effect", "skip adventure"}),

    // Dumpster Diving
    new ChoiceAdventure(
        "Town",
        "choiceAdventure109",
        "Sleazy Back Alley",
        new Object[] {
          "enter combat", "meat and moxie", new Option("Mad Train wine", "Mad Train wine")
        }),

    // The Entertainer
    new ChoiceAdventure(
        "Town",
        "choiceAdventure110",
        "Sleazy Back Alley",
        new Object[] {"moxie substats", "moxie and muscle", "small meat boost", "skip adventure"}),

    // Malice in Chains
    new ChoiceAdventure(
        "Knob",
        "choiceAdventure111",
        "Outskirts of The Knob",
        new Object[] {"muscle substats", "muscle substats", "enter combat"}),

    // Please, Hammer
    new ChoiceAdventure(
        "Town",
        "choiceAdventure112",
        "Sleazy Back Alley",
        new Object[] {"accept hammer quest", "reject quest", "muscle substats"}),

    // Knob Goblin BBQ
    new ChoiceAdventure(
        "Knob",
        "choiceAdventure113",
        "Outskirts of The Knob",
        new Object[] {"complete cake quest", "enter combat", "get a random item"}),

    // The Baker's Dilemma
    new ChoiceAdventure(
        "Manor1",
        "choiceAdventure114",
        "Haunted Pantry",
        new Object[] {"accept cake quest", "reject quest", "moxie and meat"}),

    // Oh No, Hobo
    new ChoiceAdventure(
        "Manor1",
        "choiceAdventure115",
        "Haunted Pantry",
        new Object[] {"enter combat", "Good Karma", "mysticality, moxie, and meat"}),

    // The Singing Tree
    new ChoiceAdventure(
        "Manor1",
        "choiceAdventure116",
        "Haunted Pantry",
        new Object[] {"mysticality substats", "moxie substats", "random effect", "skip adventure"}),

    // Tresspasser
    new ChoiceAdventure(
        "Manor1",
        "choiceAdventure117",
        "Haunted Pantry",
        new Object[] {"enter combat", "mysticality substats", "get a random item"}),

    // When Rocks Attack
    new ChoiceAdventure(
        "Knob",
        "choiceAdventure118",
        "Outskirts of The Knob",
        new Object[] {"accept unguent quest", "skip adventure"}),

    // Choice 119 is Check It Out Now

    // Ennui is Wasted on the Young
    new ChoiceAdventure(
        "Knob",
        "choiceAdventure120",
        "Outskirts of The Knob",
        new Object[] {
          "muscle and Pumped Up",
          new Option("ice cold Sir Schlitz", "ice cold Sir Schlitz"),
          new Option("moxie and lemon", "lemon"),
          "skip adventure"
        }),

    // Choice 121 is Next Sunday, A.D.
    // Choice 122 is unknown

    // At Least It's Not Full Of Trash
    new ChoiceSpoiler(
        "choiceAdventure123",
        "Hidden Temple",
        new Object[] {"lose HP", "Unlock Quest Puzzle", "lose HP"}),

    // Choice 124 is unknown

    // No Visible Means of Support
    new ChoiceSpoiler(
        "choiceAdventure125",
        "Hidden Temple",
        new Object[] {"lose HP", "lose HP", "Unlock Hidden City"}),

    // Sun at Noon, Tan Us
    new ChoiceAdventure(
        "Plains",
        "choiceAdventure126",
        "Palindome",
        new Object[] {"moxie", "chance of more moxie", "sunburned"}),

    // No sir, away!  A papaya war is on!
    new ChoiceSpoiler(
        "Plains",
        "choiceAdventure127",
        "Palindome",
        new Object[] {new Option("3 papayas", "papaya"), "trade 3 papayas for stats", "stats"}),
    // No sir, away!  A papaya war is on!
    new Object[] {IntegerPool.get(127), IntegerPool.get(2), ItemPool.get(ItemPool.PAPAYA, -3)},

    // Choice 128 is unknown

    // Do Geese See God?
    new ChoiceSpoiler(
        "Plains",
        "choiceAdventure129",
        "Palindome",
        new Object[] {new Option("photograph of God", "photograph of God"), "skip adventure"}),
    // Do Geese See God?
    new Object[] {
      IntegerPool.get(129), IntegerPool.get(1), new AdventureResult(AdventureResult.MEAT, -500)
    },

    // Choice 133 is unknown

    // Peace Wants Love
    new ChoiceAdventure(
        "Island",
        "choiceAdventure136",
        "Hippy Camp",
        new Object[] {
          new Option("filthy corduroys", "filthy corduroys"),
          new Option("filthy knitted dread sack", "filthy knitted dread sack"),
          "small meat boost",
          "complete the outfit"
        }),

    // An Inconvenient Truth
    new ChoiceAdventure(
        "Island",
        "choiceAdventure137",
        "Hippy Camp",
        new Object[] {
          new Option("filthy knitted dread sack", "filthy knitted dread sack"),
          new Option("filthy corduroys", "filthy corduroys"),
          "small meat boost",
          "complete the outfit"
        }),

    // Purple Hazers
    new ChoiceAdventure(
        "Island",
        "choiceAdventure138",
        "Frat House",
        new Object[] {
          new Option("Orcish cargo shorts", "Orcish cargo shorts"),
          new Option("Orcish baseball cap", "Orcish baseball cap"),
          new Option("Orcish frat-paddle", "Orcish frat-paddle"),
          "complete the outfit"
        }),

    // Bait and Switch
    new ChoiceAdventure(
        "IsleWar",
        "choiceAdventure139",
        "War Hippies",
        new Object[] {"muscle substats", new Option("ferret bait", "ferret bait"), "enter combat"}),

    // The Thin Tie-Dyed Line
    new ChoiceAdventure(
        "IsleWar",
        "choiceAdventure140",
        "War Hippies",
        new Object[] {
          new Option("water pipe bombs", "water pipe bomb"), "moxie substats", "enter combat"
        }),

    // Blockin' Out the Scenery
    new ChoiceAdventure(
        "IsleWar",
        "choiceAdventure141",
        "War Hippies",
        new Object[] {"mysticality substats", "get some hippy food", "waste a turn"}),

    // Blockin' Out the Scenery
    new ChoiceAdventure(
        "IsleWar",
        "choiceAdventure142",
        "War Hippies",
        new Object[] {"mysticality substats", "get some hippy food", "start the war"}),

    // Catching Some Zetas
    new ChoiceAdventure(
        "IsleWar",
        "choiceAdventure143",
        "War Fraternity",
        new Object[] {"muscle substats", new Option("sake bombs", "sake bomb"), "enter combat"}),

    // One Less Room Than In That Movie
    new ChoiceAdventure(
        "IsleWar",
        "choiceAdventure144",
        "War Fraternity",
        new Object[] {"moxie substats", new Option("beer bombs", "beer bomb"), "enter combat"}),

    // Fratacombs
    new ChoiceAdventure(
        "IsleWar",
        "choiceAdventure145",
        "War Fraternity",
        new Object[] {"muscle substats", "get some frat food", "waste a turn"}),

    // Fratacombs
    new ChoiceAdventure(
        "IsleWar",
        "choiceAdventure146",
        "War Fraternity",
        new Object[] {"muscle substats", "get some frat food", "start the war"}),

    // Cornered!
    new ChoiceAdventure(
        "Farm",
        "choiceAdventure147",
        "McMillicancuddy's Barn",
        new Object[] {"Open The Granary (meat)", "Open The Bog (stench)", "Open The Pond (cold)"}),

    // Cornered Again!
    new ChoiceAdventure(
        "Farm",
        "choiceAdventure148",
        "McMillicancuddy's Barn",
        new Object[] {"Open The Back 40 (hot)", "Open The Family Plot (spooky)"}),

    // How Many Corners Does this Stupid Barn Have!?
    new ChoiceAdventure(
        "Farm",
        "choiceAdventure149",
        "McMillicancuddy's Barn",
        new Object[] {"Open The Shady Thicket (booze)", "Open The Other Back 40 (sleaze)"}),

    // Choice 150 is Another Adventure About BorderTown

    // Adventurer, $1.99
    new ChoiceAdventure(
        "Plains",
        "choiceAdventure151",
        "Fun House",
        new Object[] {"fight the clownlord", "skip adventure"}),

    // Lurking at the Threshold
    new ChoiceSpoiler(
        "Plains",
        "choiceAdventure152",
        "Fun House",
        new Object[] {"fight the clownlord", "skip adventure"}),

    // Turn Your Head and Coffin
    new ChoiceAdventure(
        "Cyrpt",
        "choiceAdventure153",
        "Defiled Alcove",
        new Object[] {
          "muscle substats",
          "small meat boost",
          new Option("half-rotten brain", "half-rotten brain"),
          "skip adventure"
        }),

    // Choice 154 used to be Doublewide

    // Skull, Skull, Skull
    new ChoiceAdventure(
        "Cyrpt",
        "choiceAdventure155",
        "Defiled Nook",
        new Object[] {
          "moxie substats",
          "small meat boost",
          new Option("rusty bonesaw", "rusty bonesaw"),
          new Option("debonair deboner", "debonair deboner"),
          "skip adventure"
        }),

    // Choice 156 used to be Pileup

    // Urning Your Keep
    new ChoiceAdventure(
        "Cyrpt",
        "choiceAdventure157",
        "Defiled Niche",
        new Object[] {
          "mysticality substats",
          new Option("plus-sized phylactery", "plus-sized phylactery"),
          "small meat boost",
          "skip adventure"
        }),

    // Choice 158 used to be Lich in the Niche
    // Choice 159 used to be Go Slow Past the Drawers
    // Choice 160 used to be Lunchtime

    // Choice 161 is Bureaucracy of the Damned

    // Between a Rock and Some Other Rocks
    new ChoiceSpoiler(
        "choiceAdventure162", "Goatlet", new Object[] {"Open Goatlet", "skip adventure"}),

    // Melvil Dewey Would Be Ashamed
    new ChoiceAdventure(
        "Manor1",
        "choiceAdventure163",
        "Haunted Library",
        new Object[] {
          new Option("Necrotelicomnicon", "Necrotelicomnicon"),
          new Option("Cookbook of the Damned", "Cookbook of the Damned"),
          new Option("Sinful Desires", "Sinful Desires"),
          "skip adventure"
        }),

    // The Wormwood choices always come in order

    // 1: 164, 167, 170
    // 2: 165, 168, 171
    // 3: 166, 169, 172

    // Some first-round choices give you an effect for five turns:

    // 164/2 -> Spirit of Alph
    // 167/3 -> Bats in the Belfry
    // 170/1 -> Rat-Faced

    // First-round effects modify some second round options and
    // give you a second effect for five rounds. If you do not have
    // the appropriate first-round effect, these second-round
    // options do not consume an adventure.

    // 165/1 + Rat-Faced -> Night Vision
    // 165/2 + Bats in the Belfry -> Good with the Ladies
    // 168/2 + Spirit of Alph -> Feelin' Philosophical
    // 168/2 + Rat-Faced -> Unusual Fashion Sense
    // 171/1 + Bats in the Belfry -> No Vertigo
    // 171/3 + Spirit of Alph -> Dancing Prowess

    // Second-round effects modify some third round options and
    // give you an item. If you do not have the appropriate
    // second-round effect, most of these third-round options do
    // not consume an adventure.

    // 166/1 + No Vertigo -> S.T.L.T.
    // 166/3 + Unusual Fashion Sense -> albatross necklace
    // 169/1 + Night Vision -> flask of Amontillado
    // 169/3 + Dancing Prowess -> fancy ball mask
    // 172/1 + Good with the Ladies -> Can-Can skirt
    // 172/1 -> combat
    // 172/2 + Feelin' Philosophical -> not-a-pipe

    // Down by the Riverside
    new ChoiceAdventure(
        "Wormwood",
        "choiceAdventure164",
        "Pleasure Dome",
        new Object[] {"muscle substats", "MP & Spirit of Alph", "enter combat"}),

    // Beyond Any Measure
    new ChoiceAdventure(
        "Wormwood",
        "choiceAdventure165",
        "Pleasure Dome",
        new Object[] {
          "Rat-Faced -> Night Vision",
          "Bats in the Belfry -> Good with the Ladies",
          "mysticality	     substats",
          "skip adventure"
        }),

    // Death is a Boat
    new ChoiceAdventure(
        "Wormwood",
        "choiceAdventure166",
        "Pleasure Dome",
        new Object[] {
          new Option("No Vertigo -> S.T.L.T.", "S.T.L.T."),
          "moxie substats",
          new Option("Unusual Fashion Sense -> albatross necklace", "albatross necklace")
        }),

    // It's a Fixer-Upper
    new ChoiceAdventure(
        "Wormwood",
        "choiceAdventure167",
        "Moulder Mansion",
        new Object[] {"enter combat", "mysticality substats", "HP & MP & Bats in the Belfry"}),

    // Midst the Pallor of the Parlor
    new ChoiceAdventure(
        "Wormwood",
        "choiceAdventure168",
        "Moulder Mansion",
        new Object[] {
          "moxie substats",
          "Spirit of Alph -> Feelin' Philosophical",
          "Rat-Faced -> Unusual Fashion Sense"
        }),

    // A Few Chintz Curtains, Some Throw Pillows, It
    new ChoiceAdventure(
        "Wormwood",
        "choiceAdventure169",
        "Moulder Mansion",
        new Object[] {
          new Option("Night Vision -> flask of Amontillado", "flask of Amontillado"),
          "muscle substats",
          new Option("Dancing Prowess -> fancy ball mask", "fancy ball mask")
        }),

    // La Vie Boheme
    new ChoiceAdventure(
        "Wormwood",
        "choiceAdventure170",
        "Rogue Windmill",
        new Object[] {"HP & Rat-Faced", "enter combat", "moxie substats"}),

    // Backstage at the Rogue Windmill
    new ChoiceAdventure(
        "Wormwood",
        "choiceAdventure171",
        "Rogue Windmill",
        new Object[] {
          "Bats in the Belfry -> No Vertigo", "muscle substats", "Spirit of Alph -> Dancing Prowess"
        }),

    // Up in the Hippo Room
    new ChoiceAdventure(
        "Wormwood",
        "choiceAdventure172",
        "Rogue Windmill",
        new Object[] {
          new Option("Good with the Ladies -> Can-Can skirt", "Can-Can skirt"),
          new Option("Feelin' Philosophical -> not-a-pipe", "not-a-pipe"),
          "mysticality substats"
        }),

    // Choice 173 is The Last Stand, Man
    // Choice 174 is The Last Stand, Bra
    // Choice 175-176 are unknown

    // Choice 177 was The Blackberry Cobbler

    // Hammering the Armory
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure178",
        "Fantasy Airship Shirt",
        new Object[] {new Option("bronze breastplate", "bronze breastplate"), "skip adventure"}),

    // Choice 179 is unknown

    // A Pre-War Dresser Drawer, Pa!
    new ChoiceAdventure(
        "Plains",
        "choiceAdventure180",
        "Palindome Shirt",
        new Object[] {new Option("Ye Olde Navy Fleece", "Ye Olde Navy Fleece"), "skip adventure"}),

    // Chieftain of the Flies
    new ChoiceAdventure(
        "Island",
        "choiceAdventure181",
        "Frat House (Stone Age)",
        new Object[] {
          new Option("around the world", "around the world"),
          new Option("skip adventure", "Spanish fly")
        }),
    // Chieftain of the Flies
    // This trades all Spanish flies for around the worlds,
    // in multiples of 5.  Excess flies are left in inventory.
    new Object[] {IntegerPool.get(181), IntegerPool.get(1), ItemPool.get(ItemPool.SPANISH_FLY, 5)},

    // Random Lack of an Encounter
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure182",
        "Fantasy Airship",
        new Object[] {
          "enter combat",
          new Option("Penultimate Fantasy chest", "Penultimate Fantasy chest"),
          "stats",
          new Option("model airship and combat", "model airship"),
          new Option("model airship and chest", "model airship"),
          new Option("model airship and stats", "model airship")
        }),

    // That Explains All The Eyepatches
    // Dynamically calculate options based on mainstat
    new ChoiceAdventure("Island", "choiceAdventure184", "Barrrney's Barrr", null),

    // Yes, You're a Rock Starrr
    new ChoiceAdventure("Island", "choiceAdventure185", "Barrrney's Barrr", null),

    // A Test of Testarrrsterone
    new ChoiceAdventure(
        "Island",
        "choiceAdventure186",
        "Barrrney's Barrr",
        new Object[] {"stats", "drunkenness and stats", "moxie"}),

    // Choice 187 is Arrr You Man Enough?

    // The Infiltrationist
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure188",
        "Frathouse Blueprints",
        new Object[] {
          "frat boy ensemble", "mullet wig and briefcase", "frilly skirt and hot wings"
        }),

    //  O Cap'm, My Cap'm
    new Object[] {
      IntegerPool.get(189), IntegerPool.get(1), new AdventureResult(AdventureResult.MEAT, -977)
    },

    // Choice 190 is unknown

    // Chatterboxing
    new ChoiceAdventure(
        "Island",
        "choiceAdventure191",
        "F'c'le",
        new Object[] {
          "moxie substats",
          "use valuable trinket to banish, or lose hp",
          "muscle substats",
          "mysticality substats",
          "use valuable trinket to banish, or moxie",
          "use valuable trinket to banish, or muscle",
          "use valuable trinket to banish, or mysticality",
          "use valuable trinket to banish, or mainstat"
        }),
    new Object[] {
      IntegerPool.get(191), IntegerPool.get(2), ItemPool.get(ItemPool.VALUABLE_TRINKET, -1)
    },

    // Choice 192 is unknown
    // Choice 193 is Modular, Dude

    // Somewhat Higher and Mostly Dry
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure197",
        "A Maze of Sewer Tunnels",
        new Object[] {"take the tunnel", "sewer gator", "turn the valve"}),

    // Disgustin' Junction
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure198",
        "A Maze of Sewer Tunnels",
        new Object[] {"take the tunnel", "giant zombie goldfish", "open the grate"}),

    // The Former or the Ladder
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure199",
        "A Maze of Sewer Tunnels",
        new Object[] {"take the tunnel", "C. H. U. M.", "head down the ladder"}),

    // Enter The Hoboverlord
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure200",
        "Hobopolis Town Square",
        new Object[] {"enter combat with Hodgman", "skip adventure"}),

    // Home, Home in the Range
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure201",
        "Burnbarrel Blvd.",
        new Object[] {"enter combat with Ol' Scratch", "skip adventure"}),

    // Bumpity Bump Bump
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure202",
        "Exposure Esplanade",
        new Object[] {"enter combat with Frosty", "skip adventure"}),

    // Deep Enough to Dive
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure203",
        "The Heap",
        new Object[] {"enter combat with Oscus", "skip adventure"}),

    // Welcome To You!
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure204",
        "The Ancient Hobo Burial Ground",
        new Object[] {"enter combat with Zombo", "skip adventure"}),

    // Van, Damn
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure205",
        "The Purple Light District",
        new Object[] {"enter combat with Chester", "skip adventure"}),

    // Getting Tired
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure206",
        "Burnbarrel Blvd.",
        new Object[] {"start tirevalanche", "add tire to stack", "skip adventure"}),

    // Hot Dog! I Mean... Door!
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure207",
        "Burnbarrel Blvd.",
        new Object[] {"increase hot hobos & get clan meat", "skip adventure"}),

    // Ah, So That's Where They've All Gone
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure208",
        "The Ancient Hobo Burial Ground",
        new Object[] {"increase spooky hobos & decrease stench", "skip adventure"}),

    // Choice 209 is Timbarrrr!
    // Choice 210 is Stumped

    // Despite All Your Rage
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure211",
        "A Maze of Sewer Tunnels",
        new Object[] {"gnaw through the bars"}),

    // Choice 212 is also Despite All Your Rage, apparently after you've already
    // tried to wait for rescue?
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure212",
        "A Maze of Sewer Tunnels",
        new Object[] {"gnaw through the bars"}),

    // Piping Hot
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure213",
        "Burnbarrel Blvd.",
        new Object[] {"increase sleaze hobos & decrease heat", "skip adventure"}),

    // You vs. The Volcano
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure214",
        "The Heap",
        new Object[] {"decrease stench hobos & increase stench", "skip adventure"}),

    // Piping Cold
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure215",
        "Exposure Esplanade",
        new Object[] {"decrease heat", "decrease sleaze hobos", "increase number of icicles"}),

    // The Compostal Service
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure216",
        "The Heap",
        new Object[] {"decrease stench & spooky", "skip adventure"}),

    // There Goes Fritz!
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure217",
        "Exposure Esplanade",
        new Object[] {"yodel a little", "yodel a lot", "yodel your heart out"}),

    // I Refuse!
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure218",
        "The Heap",
        new Object[] {"explore the junkpile", "skip adventure"}),

    // The Furtivity of My City
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure219",
        "The Purple Light District",
        new Object[] {
          "fight sleaze hobo", "increase stench", "increase sleaze hobos & get clan meat"
        }),

    // Returning to the Tomb
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure220",
        "The Ancient Hobo Burial Ground",
        new Object[] {"increase spooky hobos & get clan meat", "skip adventure"}),

    // A Chiller Night
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure221",
        "The Ancient Hobo Burial Ground",
        new Object[] {"study the dance moves", "dance with hobo zombies", "skip adventure"}),

    // A Chiller Night (2)
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure222",
        "The Ancient Hobo Burial Ground",
        new Object[] {"dance with hobo zombies", "skip adventure"}),

    // Getting Clubbed
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure223",
        "The Purple Light District",
        new Object[] {
          "try to get inside", "try to bamboozle the crowd", "try to flimflam the crowd"
        }),

    // Exclusive!
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure224",
        "The Purple Light District",
        new Object[] {"fight sleaze hobo", "start barfight", "gain stats"}),

    // Attention -- A Tent!
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure225",
        "Hobopolis Town Square",
        new Object[] {"perform on stage", "join the crowd", "skip adventure"}),

    // Choice 226 is Here You Are, Up On Stage (use the same system as 211 & 212)
    // Choice 227 is Working the Crowd (use the same system as 211 & 212)

    // Choices 228 & 229 are unknown

    // Mind Yer Binder
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure230",
        "Hobopolis Town Square",
        new Object[] {new Option("hobo code binder", "hobo code binder"), "skip adventure"}),
    // Mind Yer Binder
    new Object[] {
      IntegerPool.get(230), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -30)
    },

    // Choices 231-271 are subchoices of Choice 272

    // Food, Glorious Food
    new ChoiceSpoiler(
        "choiceAdventure235",
        "Hobopolis Marketplace",
        new Object[] {"muscle food", "mysticality food", "moxie food"}),

    // Booze, Glorious Booze
    new ChoiceSpoiler(
        "choiceAdventure240",
        "Hobopolis Marketplace",
        new Object[] {"muscle booze", "mysticality booze", "moxie booze"}),

    // The Guy Who Carves Driftwood Animals
    new Object[] {
      IntegerPool.get(247), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -10)
    },

    // A Hattery
    new ChoiceSpoiler(
        "choiceAdventure250",
        "Hobopolis Marketplace",
        new Object[] {
          new Option("crumpled felt fedora", "crumpled felt fedora"),
          new Option("battered old top-hat", "battered old top-hat"),
          new Option("shapeless wide-brimmed hat", "shapeless wide-brimmed hat")
        }),
    // A Hattery
    new Object[] {
      IntegerPool.get(250), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -250)
    },
    new Object[] {
      IntegerPool.get(250), IntegerPool.get(2), ItemPool.get(ItemPool.HOBO_NICKEL, -150)
    },
    new Object[] {
      IntegerPool.get(250), IntegerPool.get(3), ItemPool.get(ItemPool.HOBO_NICKEL, -200)
    },

    // A Pantry
    new ChoiceSpoiler(
        "choiceAdventure251",
        "Hobopolis Marketplace",
        new Object[] {
          new Option("mostly rat-hide leggings", "mostly rat-hide leggings"),
          new Option("hobo dungarees", "hobo dungarees"),
          new Option("old patched suit-pants", "old patched suit-pants")
        }),
    // A Pantry
    new Object[] {
      IntegerPool.get(251), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -200)
    },
    new Object[] {
      IntegerPool.get(251), IntegerPool.get(2), ItemPool.get(ItemPool.HOBO_NICKEL, -150)
    },
    new Object[] {
      IntegerPool.get(251), IntegerPool.get(3), ItemPool.get(ItemPool.HOBO_NICKEL, -250)
    },

    // Hobo Blanket Bingo
    new ChoiceSpoiler(
        "choiceAdventure252",
        "Hobopolis Marketplace",
        new Object[] {
          new Option("old soft shoes", "old soft shoes"),
          new Option("hobo stogie", "hobo stogie"),
          new Option("rope with some soap on it", "rope with some soap on it")
        }),
    // Hobo Blanket Bingo
    new Object[] {
      IntegerPool.get(252), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -250)
    },
    new Object[] {
      IntegerPool.get(252), IntegerPool.get(2), ItemPool.get(ItemPool.HOBO_NICKEL, -200)
    },
    new Object[] {
      IntegerPool.get(252), IntegerPool.get(3), ItemPool.get(ItemPool.HOBO_NICKEL, -150)
    },

    // Black-and-Blue-and-Decker
    new ChoiceSpoiler(
        "choiceAdventure255",
        "Hobopolis Marketplace",
        new Object[] {
          new Option("sharpened hubcap", "sharpened hubcap"),
          new Option("very large caltrop", "very large caltrop"),
          new Option("The Six-Pack of Pain", "The Six-Pack of Pain")
        }),
    // Black-and-Blue-and-Decker
    new Object[] {
      IntegerPool.get(255), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -10)
    },
    new Object[] {
      IntegerPool.get(255), IntegerPool.get(2), ItemPool.get(ItemPool.HOBO_NICKEL, -10)
    },
    new Object[] {
      IntegerPool.get(255), IntegerPool.get(3), ItemPool.get(ItemPool.HOBO_NICKEL, -10)
    },

    // Instru-mental
    new Object[] {
      IntegerPool.get(258), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -99)
    },

    // We'll Make Great...
    new ChoiceSpoiler(
        "choiceAdventure259",
        "Hobopolis Marketplace",
        new Object[] {"hobo monkey", "stats", "enter combat"}),

    // Everybody's Got Something To Hide
    new Object[] {
      IntegerPool.get(261), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -1000)
    },

    // Tanning Salon
    new ChoiceSpoiler(
        "choiceAdventure264",
        "Hobopolis Marketplace",
        new Object[] {"20 adv of +50% moxie", "20 adv of +50% mysticality"}),
    // Tanning Salon
    new Object[] {IntegerPool.get(264), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},
    new Object[] {IntegerPool.get(264), IntegerPool.get(2), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},

    // Let's All Go To The Movies
    new ChoiceSpoiler(
        "choiceAdventure267",
        "Hobopolis Marketplace",
        new Object[] {"20 adv of +5 spooky resistance", "20 adv of +5 sleaze resistance"}),
    // Let's All Go To The Movies
    new Object[] {IntegerPool.get(267), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},
    new Object[] {IntegerPool.get(267), IntegerPool.get(2), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},

    // It's Fun To Stay There
    new ChoiceSpoiler(
        "choiceAdventure268",
        "Hobopolis Marketplace",
        new Object[] {"20 adv of +5 stench resistance", "20 adv of +50% muscle"}),
    // It's Fun To Stay There
    new Object[] {IntegerPool.get(268), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},
    new Object[] {IntegerPool.get(268), IntegerPool.get(2), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},

    // Marketplace Entrance
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure272",
        "Hobopolis Town Square",
        new Object[] {"enter marketplace", "skip adventure"}),

    // Piping Cold
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure273",
        "Exposure Esplanade",
        new Object[] {
          new Option("frozen banquet", "frozen banquet"),
          "increase cold hobos & get clan meat",
          "skip adventure"
        }),

    // Choice 274 is Tattoo Redux, a subchoice of Choice 272 when
    // you've started a tattoo

    // Choice 275 is Triangle, Man, a subchoice of Choice 272 when
    // you've already purchased your class instrument
    // Triangle, Man
    new Object[] {
      IntegerPool.get(275), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -10)
    },

    // Choices 278-290 are llama lama gong related choices

    // The Gong Has Been Bung
    new ChoiceSpoiler(
        "choiceAdventure276",
        "Gong",
        new Object[] {"3 adventures", "12 adventures", "15 adventures"}),

    // Welcome Back!
    new ChoiceSpoiler(
        "choiceAdventure277", "Gong", new Object[] {"finish journey", "also finish journey"}),

    // Enter the Roach
    new ChoiceSpoiler(
        "choiceAdventure278",
        "Gong",
        new Object[] {"muscle substats", "mysticality substats", "moxie substats"}),

    // It's Nukyuhlur - the 'S' is Silent.
    new ChoiceSpoiler(
        "choiceAdventure279",
        "Gong",
        new Object[] {"moxie substats", "muscle substats", "gain MP"}),

    // Eek! Eek!
    new ChoiceSpoiler(
        "choiceAdventure280",
        "Gong",
        new Object[] {"mysticality substats", "muscle substats", "gain MP"}),

    // A Meta-Metamorphosis
    new ChoiceSpoiler(
        "choiceAdventure281",
        "Gong",
        new Object[] {"moxie substats", "mysticality substats", "gain MP"}),

    // You've Got Wings, But No Wingman
    new ChoiceSpoiler(
        "choiceAdventure282", "Gong", new Object[] {"+30% muscle", "+10% all stats", "+30 ML"}),

    // Time Enough at Last!
    new ChoiceSpoiler(
        "choiceAdventure283",
        "Gong",
        new Object[] {"+30% muscle", "+10% all stats", "+50% item drops"}),

    // Scavenger Is Your Middle Name
    new ChoiceSpoiler(
        "choiceAdventure284", "Gong", new Object[] {"+30% muscle", "+50% item drops", "+30 ML"}),

    // Bugging Out
    new ChoiceSpoiler(
        "choiceAdventure285",
        "Gong",
        new Object[] {"+30% mysticality", "+30 ML", "+10% all stats"}),

    // A Sweeping Generalization
    new ChoiceSpoiler(
        "choiceAdventure286",
        "Gong",
        new Object[] {"+50% item drops", "+10% all stats", "+30% mysticality"}),

    // In the Frigid Aire
    new ChoiceSpoiler(
        "choiceAdventure287",
        "Gong",
        new Object[] {"+30 ML", "+30% mysticality", "+50% item drops"}),

    // Our House
    new ChoiceSpoiler(
        "choiceAdventure288", "Gong", new Object[] {"+30 ML", "+30% moxie", "+10% all stats"}),

    // Workin' For The Man
    new ChoiceSpoiler(
        "choiceAdventure289", "Gong", new Object[] {"+30 ML", "+30% moxie", "+50% item drops"}),

    // The World's Not Fair
    new ChoiceSpoiler(
        "choiceAdventure290",
        "Gong",
        new Object[] {"+30% moxie", "+10% all stats", "+50% item drops"}),

    // A Tight Squeeze
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure291",
        "Burnbarrel Blvd.",
        new Object[] {new Option("jar of squeeze", "jar of squeeze"), "skip adventure"}),
    // A Tight Squeeze - jar of squeeze
    new Object[] {IntegerPool.get(291), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},

    // Cold Comfort
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure292",
        "Exposure Esplanade",
        new Object[] {new Option("bowl of fishysoisse", "bowl of fishysoisse"), "skip adventure"}),
    // Cold Comfort - bowl of fishysoisse
    new Object[] {IntegerPool.get(292), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},

    // Flowers for You
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure293",
        "The Ancient Hobo Burial Ground",
        new Object[] {new Option("deadly lampshade", "deadly lampshade"), "skip adventure"}),
    // Flowers for You - deadly lampshade
    new Object[] {IntegerPool.get(293), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},

    // Maybe It's a Sexy Snake!
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure294",
        "The Purple Light District",
        new Object[] {new Option("lewd playing card", "lewd playing card"), "skip adventure"}),
    // Maybe It's a Sexy Snake! - lewd playing card
    new Object[] {IntegerPool.get(294), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},

    // Juicy!
    new ChoiceAdventure(
        "Hobopolis",
        "choiceAdventure295",
        "The Heap",
        new Object[] {
          new Option("concentrated garbage juice", "concentrated garbage juice"), "skip adventure"
        }),
    // Juicy! - concentrated garbage juice
    new Object[] {IntegerPool.get(295), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -5)},

    // Choice 296 is Pop!

    // Gravy Fairy Ring
    new ChoiceAdventure(
        "Dungeon",
        "choiceAdventure297",
        "Haiku Dungeon",
        new Object[] {
          "mushrooms", new Option("fairy gravy boat", "fairy gravy boat"), "skip adventure"
        }),

    // In the Shade
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure298",
        "An Octopus's Garden",
        new Object[] {"plant seeds", "skip adventure"}),

    // Down at the Hatch
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure299",
        "The Wreck of the Edgar Fitzsimmons",
        new Object[] {
          "release creatures", "skip adventure", "unlock tarnished luggage key adventure"
        }),

    // Choice 300 is Merry Crimbo!
    // Choice 301 is And to All a Good Night
    // Choice 302 is You've Hit Bottom (Sauceror)
    // Choice 303 is You've Hit Bottom (Pastamancer)

    // A Vent Horizon
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure304",
        "The Marinara Trench",
        new Object[] {
          new Option("bubbling tempura batter", "bubbling tempura batter"), "skip adventure"
        }),
    // A Vent Horizon
    new Object[] {
      IntegerPool.get(304),
      IntegerPool.get(1),
      new AdventureLongCountResult(AdventureResult.MP, -200)
    },

    // There is Sauce at the Bottom of the Ocean
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure305",
        "The Marinara Trench",
        new Object[] {new Option("globe of Deep Sauce", "globe of Deep Sauce"), "skip adventure"}),
    // There is Sauce at the Bottom of the Ocean
    new Object[] {
      IntegerPool.get(305), IntegerPool.get(1), ItemPool.get(ItemPool.MERKIN_PRESSUREGLOBE, -1)
    },

    // Choice 306 is [Grandpa Mine Choice]
    // Choice 307 is Ode to the Sea
    // Choice 308 is Boxing the Juke

    // Barback
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure309",
        "The Dive Bar",
        new Object[] {new Option("seaode", "seaode"), "skip adventure"}),

    // The Economist of Scales
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure310",
        "Madness Reef",
        new Object[] {
          new Option("get 1 rough fish scale", 1, "rough fish scale"),
          new Option("get 1 pristine fish scale", 2, "pristine fish scale"),
          new Option("get multiple rough fish scales", 4, "rough fish scale"),
          new Option("get multiple pristine fish scales", 5, "pristine fish scale"),
          new Option("skip adventure", 6)
        }),
    // The Economist of Scales
    // This trades 10 dull fish scales in.
    new Object[] {
      IntegerPool.get(310), IntegerPool.get(1), ItemPool.get(ItemPool.DULL_FISH_SCALE, -10)
    },
    new Object[] {
      IntegerPool.get(310), IntegerPool.get(2), ItemPool.get(ItemPool.ROUGH_FISH_SCALE, -10)
    },
    new Object[] {
      IntegerPool.get(310), IntegerPool.get(4), ItemPool.get(ItemPool.DULL_FISH_SCALE, 10)
    },
    new Object[] {
      IntegerPool.get(310), IntegerPool.get(5), ItemPool.get(ItemPool.ROUGH_FISH_SCALE, 10)
    },

    // Heavily Invested in Pun Futures
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure311",
        "Madness Reef",
        new Object[] {"The Economist of Scales", "skip adventure"}),

    // Choice 312 is unknown
    // Choice 313 is unknown
    // Choice 314 is unknown
    // Choice 315 is unknown
    // Choice 316 is unknown

    // Choice 317 is No Man, No Hole
    // Choice 318 is C'mere, Little Fella
    // Choice 319 is Turtles of the Universe
    // Choice 320 is A Rolling Turtle Gathers No Moss
    // Choice 321 is Boxed In
    // Choice 322 is Capital!

    // Choice 323 is unknown
    // Choice 324 is unknown
    // Choice 325 is unknown

    // Showdown
    new ChoiceAdventure(
        "Clan Basement",
        "choiceAdventure326",
        "The Slime Tube",
        new Object[] {"enter combat with Mother Slime", "skip adventure"}),

    // Choice 327 is Puttin' it on Wax
    // Choice 328 is Never Break the Chain
    // Choice 329 is Don't Be Alarmed, Now

    // A Shark's Chum
    new ChoiceAdventure(
        "Manor1",
        "choiceAdventure330",
        "Haunted Billiards Room",
        new Object[] {
          "stats and pool skill", new Option("cube of billiard chalk", "cube of billiard chalk")
        }),

    // Choice 331 is Like That Time in Tortuga
    // Choice 332 is More eXtreme Than Usual
    // Choice 333 is Cleansing your Palette
    // Choice 334 is O Turtle Were Art Thou
    // Choice 335 is Blue Monday
    // Choice 336 is Jewel in the Rough

    // Engulfed!
    new ChoiceAdventure(
        "Clan Basement",
        "choiceAdventure337",
        "The Slime Tube",
        new Object[] {
          "+1 rusty -> slime-covered item conversion", "raise area ML", "skip adventure"
        }),

    // Choice 338 is Duel Nature
    // Choice 339 is Kick the Can
    // Choice 340 is Turtle in peril
    // Choice 341 is Nantucket Snapper
    // Choice 342 is The Horror...
    // Choice 343 is Turtles All The Way Around
    // Choice 344 is Silent Strolling
    // Choice 345 is Training Day

    // Choice 346 is Soup For You
    // Choice 347 is Yes, Soup For You
    // Choice 348 is Souped Up

    // The Primordial Directive
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure349",
        "The Primordial Soup",
        new Object[] {"swim upwards", "swim in circles", "swim downwards"}),

    // Soupercharged
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure350",
        "The Primordial Soup",
        new Object[] {"Fight Cyrus", "skip adventure"}),

    // Choice 351 is Beginner's Luck

    // Savior Faire
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure352",
        "Seaside Megalopolis",
        new Object[] {
          "Moxie -> Bad Reception Down Here",
          "Muscle -> A Diseased Procurer",
          "Mysticality -> Give it a Shot"
        }),

    // Bad Reception Down Here
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure353",
        "Seaside Megalopolis",
        new Object[] {
          new Option("Indigo Party Invitation", "Indigo Party Invitation"),
          new Option("Violet Hunt Invitation", "Violet Hunt Invitation")
        }),

    // You Can Never Be Too Rich or Too in the Future
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure354",
        "Seaside Megalopolis",
        new Object[] {"Moxie", "Serenity"}),

    // I'm on the Hunt, I'm After You
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure355",
        "Seaside Megalopolis",
        new Object[] {"Stats", "Phairly Pheromonal"}),

    // A Diseased Procurer
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure356",
        "Seaside Megalopolis",
        new Object[] {
          new Option("Blue Milk Club Card", "Blue Milk Club Card"),
          new Option("Mecha Mayhem Club Card", "Mecha Mayhem Club Card")
        }),

    // Painful, Circuitous Logic
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure357",
        "Seaside Megalopolis",
        new Object[] {"Muscle", "Nano-juiced"}),

    // Brings All the Boys to the Blue Yard
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure358",
        "Seaside Megalopolis",
        new Object[] {"Stats", "Dance Interpreter"}),

    // Choice 359 is unknown

    // Cavern Entrance
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure360",
        "Jungles: Wumpus Cave",
        new Object[] {new Option("skip adventure", 2)}),

    // Give it a Shot
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure361",
        "Seaside Megalopolis",
        new Object[] {
          new Option("'Smuggler Shot First' Button", "'Smuggler Shot First' Button"),
          new Option("Spacefleet Communicator Badge", "Spacefleet Communicator Badge")
        }),

    // A Bridge Too Far
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure362",
        "Seaside Megalopolis",
        new Object[] {"Stats", "Meatwise"}),

    // Does This Bug You? Does This Bug You?
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure363",
        "Seaside Megalopolis",
        new Object[] {"Mysticality", "In the Saucestream"}),

    // 451 Degrees! Burning Down the House!
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure364",
        "Seaside Megalopolis",
        new Object[] {
          "Moxie", new Option("Supreme Being Glossary", "Supreme Being Glossary"), "Muscle"
        }),

    // None Shall Pass
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure365",
        "Seaside Megalopolis",
        new Object[] {"Muscle", new Option("multi-pass", "multi-pass")}),

    // Entrance to the Forgotten City
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure366",
        "Jungles: Forgotten City",
        new Object[] {new Option("skip adventure", 2)}),

    // Choice 367 is Ancient Temple (unlocked)
    // Choice 368 is City Center
    // Choice 369 is North Side of the City
    // Choice 370 is East Side of the City
    // Choice 371 is West Side of the City
    // Choice 372 is An Ancient Well
    // Choice 373 is Northern Gate
    // Choice 374 is An Ancient Tower
    // Choice 375 is Northern Abandoned Building

    // Ancient Temple
    new ChoiceAdventure(
        "Memories",
        "choiceAdventure376",
        "Jungles: Ancient Temple",
        new Object[] {"Enter the Temple", "leave"}),

    // Choice 377 is Southern Abandoned Building
    // Choice 378 is Storehouse
    // Choice 379 is Northern Building (Basement)
    // Choice 380 is Southern Building (Upstairs)
    // Choice 381 is Southern Building (Basement)
    // Choice 382 is Catacombs Entrance
    // Choice 383 is Catacombs Junction
    // Choice 384 is Catacombs Dead-End
    // Choice 385 is Sore of an Underground Lake
    // Choice 386 is Catacombs Machinery

    // Choice 387 is Time Isn't Holding Up; Time is a Doughnut
    // Choice 388 is Extra Savoir Faire
    // Choice 389 is The Unbearable Supremeness of Being
    // Choice 390 is A Winning Pass
    // Choice 391 is OMG KAWAIII
    // Choice 392 is The Elements of Surprise . . .

    // The Collector
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure393",
        "big bumboozer marble",
        new Object[] {"1 of each marble -> 32768 Meat", "skip adventure"}),

    // Choice 394 is Hellevator Music
    // Choice 395 is Rumble On

    // Woolly Scaly Bully
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure396",
        "Mer-kin Elementary School",
        new Object[] {"lose HP", "lose HP", "unlock janitor's closet"}),

    // Bored of Education
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure397",
        "Mer-kin Elementary School",
        new Object[] {"lose HP", "unlock the bathrooms", "lose HP"}),

    // A Mer-kin Graffiti
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure398",
        "Mer-kin Elementary School",
        new Object[] {"unlock teacher's lounge", "lose HP", "lose HP"}),

    // The Case of the Closet
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure399",
        "Mer-kin Elementary School",
        new Object[] {"fight a Mer-kin monitor", new Option("Mer-kin sawdust", "Mer-kin sawdust")}),

    // No Rest for the Room
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure400",
        "Mer-kin Elementary School",
        new Object[] {
          "fight a Mer-kin teacher", new Option("Mer-kin cancerstick", "Mer-kin cancerstick")
        }),

    // Raising Cane
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure401",
        "Mer-kin Elementary School",
        new Object[] {
          "fight a Mer-kin punisher ", new Option("Mer-kin wordquiz", "Mer-kin wordquiz")
        }),

    // Don't Hold a Grudge
    new ChoiceAdventure(
        "Manor2",
        "choiceAdventure402",
        "Haunted Bathroom",
        new Object[] {"muscle substats", "mysticality substats", "moxie substats"}),

    // Picking Sides
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure403",
        "Skate Park",
        new Object[] {
          new Option("skate blade", "skate blade"), new Option("brand new key", "brand new key")
        }),

    // Choice 409 is The Island Barracks
    //	1 = only option
    // Choice 410 is A Short Hallway
    //	1 = left, 2 = right, 3 = exit
    // Choice 411 is Hallway Left
    //	1 = kitchen, 2 = dining room, 3 = storeroom, 4 = exit
    // Choice 412 is Hallway Right
    //	1 = bedroom, 2 = library, 3 = parlour, 4 = exit
    // Choice 413 is Kitchen
    //	1 = cupboards, 2 = pantry, 3 = fridges, 4 = exit
    // Choice 414 is Dining Room
    //	1 = tables, 2 = sideboard, 3 = china cabinet, 4 = exit
    // Choice 415 is Store Room
    //	1 = crates, 2 = workbench, 3 = gun cabinet, 4 = exit
    // Choice 416 is Bedroom
    //	1 = beds, 2 = dressers, 3 = bathroom, 4 = exit
    // Choice 417 is Library
    //	1 = bookshelves, 2 = chairs, 3 = chess set, 4 = exit
    // Choice 418 is Parlour
    //	1 = pool table, 2 = bar, 3 = fireplace, 4 = exit

    // Choice 423 is A Wrenching Encounter
    // Choice 424 is Get Your Bolt On, Michael
    // Choice 425 is Taking a Proper Gander
    // Choice 426 is It's Electric, Boogie-oogie-oogie
    // Choice 427 is A Voice Crying in the Crimbo Factory
    // Choice 428 is Disguise the Limit
    // Choice 429 is Diagnosis: Hypnosis
    // Choice 430 is Secret Agent Penguin
    // Choice 431 is Zapatos Con Crete
    // Choice 432 is Don We Now Our Bright Apparel
    // Choice 433 is Everything is Illuminated?
    // Choice 435 is Season's Beatings
    // Choice 436 is unknown
    // Choice 437 is Flying In Circles

    // From Little Acorns...
    new Object[] {
      IntegerPool.get(438), IntegerPool.get(1), ItemPool.get(ItemPool.UNDERWORLD_ACORN, -1)
    },

    // Choice 439 is unknown
    // Choice 440 is Puttin' on the Wax
    // Choice 441 is The Mad Tea Party
    // Choice 442 is A Moment of Reflection
    new ChoiceAdventure(
        "RabbitHole",
        "choiceAdventure442",
        "A Moment of Reflection",
        new Object[] {
          "Seal Clubber/Pastamancer/custard",
          "Accordion Thief/Sauceror/comfit",
          "Turtle Tamer/Disco Bandit/croqueteer",
          "Ittah bittah hookah",
          "Chessboard",
          "nothing"
        }),

    // Choice 443 is Chess Puzzle

    // Choice 444 is The Field of Strawberries (Seal Clubber)
    new ChoiceAdventure(
        "RabbitHole",
        "choiceAdventure444",
        "Reflection of Map (Seal Clubber)",
        new Object[] {
          new Option("walrus ice cream", "walrus ice cream"),
          new Option("yellow matter custard", "yellow matter custard")
        }),

    // Choice 445 is The Field of Strawberries (Pastamancer)
    new ChoiceAdventure(
        "RabbitHole",
        "choiceAdventure445",
        "Reflection of Map (Pastamancer)",
        new Object[] {
          new Option("eggman noodles", "eggman noodles"),
          new Option("yellow matter custard", "yellow matter custard")
        }),

    // Choice 446 is The Caucus Racetrack (Accordion Thief)
    new ChoiceAdventure(
        "RabbitHole",
        "choiceAdventure446",
        "Reflection of Map (Accordion Thief)",
        new Object[] {
          new Option("missing wine", "missing wine"),
          new Option("delicious comfit?", "delicious comfit?")
        }),

    // Choice 447 is The Caucus Racetrack (Sauceror)
    new ChoiceAdventure(
        "RabbitHole",
        "choiceAdventure447",
        "Reflection of Map (Sauceror)",
        new Object[] {
          new Option("Vial of <i>jus de larmes</i>", "Vial of <i>jus de larmes</i>"),
          new Option("delicious comfit?", "delicious comfit?")
        }),

    // Choice 448 is The Croquet Grounds (Turtle Tamer)
    new ChoiceAdventure(
        "RabbitHole",
        "choiceAdventure448",
        "Reflection of Map (Turtle Tamer)",
        new Object[] {new Option("beautiful soup", "beautiful soup"), "fight croqueteer"}),

    // Choice 449 is The Croquet Grounds (Disco Bandit)
    new ChoiceAdventure(
        "RabbitHole",
        "choiceAdventure449",
        "Reflection of Map (Disco Bandit)",
        new Object[] {
          new Option("Lobster <i>qua</i> Grill", "Lobster <i>qua</i> Grill"), "fight croqueteer"
        }),

    // Choice 450 is The Duchess' Cottage

    // Typographical Clutter
    new ChoiceAdventure(
        "Dungeon",
        "choiceAdventure451",
        "Greater-Than Sign",
        new Object[] {
          new Option("left parenthesis", "left parenthesis"),
          "moxie, alternately lose then gain meat",
          new Option("plus sign, then muscle", "plus sign"),
          "mysticality substats",
          "get teleportitis"
        }),

    // Leave a Message and I'll Call You Back
    new ChoiceAdventure(
        "Jacking",
        "choiceAdventure452",
        "Small-O-Fier",
        new Object[] {"combat", new Option("tiny fly glasses", "tiny fly glasses"), "fruit"}),

    // Getting a Leg Up
    new ChoiceAdventure(
        "Jacking",
        "choiceAdventure453",
        "Small-O-Fier",
        new Object[] {"combat", "stats", new Option("hair of the calf", "hair of the calf")}),

    // Just Like the Ocean Under the Moon
    new ChoiceAdventure(
        "Jacking", "choiceAdventure454", "Small-O-Fier", new Object[] {"combat", "HP and MP"}),

    // Double Trouble in the Stubble
    new ChoiceAdventure(
        "Jacking", "choiceAdventure455", "Small-O-Fier", new Object[] {"stats", "quest item"}),

    // Made it, Ma!	 Top of the World!
    new ChoiceAdventure(
        "Jacking",
        "choiceAdventure456",
        "Huge-A-Ma-tron",
        new Object[] {
          "combat",
          "Hurricane Force",
          new Option("a dance upon the palate", "a dance upon the palate"),
          "stats"
        }),

    // Choice 457 is Oh, No! Five-Oh!
    // Choice 458 is ... Grow Unspeakable Horrors
    // Choice 459 is unknown
    // Choice 460 is Space Trip (Bridge)
    // Choice 461 is Space Trip (Navigation)
    // Choice 462 is Space Trip (Diagnostics)
    // Choice 463 is Space Trip (Alpha Quadrant)
    // Choice 464 is Space Trip (Beta Quadrant)
    // Choice 465 is Space Trip (Planet)
    // Choice 466 is unknown
    // Choice 467 is Space Trip (Combat)
    // Choice 468 is Space Trip (Starbase Hub)
    // Choice 469 is Space Trip (General Store)
    // Choice 470 is Space Trip (Military Surplus Store)
    // Choice 471 is DemonStar
    // Choice 472 is Space Trip (Astrozorian Trade Vessel: Alpha)
    // Choice 473 is Space Trip (Murderbot Miner: first encounter)
    // Choice 474 is Space Trip (Slavers: Alpha)
    // Choice 475 is Space Trip (Astrozorian Trade Vessel: Beta)
    // Choice 476 is Space Trip (Astrozorian Trade Vessel: Gamma)
    // Choice 477 is Space Trip (Gamma Quadrant)
    // Choice 478 is Space Trip (The Source)
    // Choice 479 is Space Trip (Slavers: Beta)
    // Choice 480 is Space Trip (Scadian ship)
    // Choice 481 is Space Trip (Hipsterian ship)
    // Choice 482 is Space Trip (Slavers: Gamma)
    // Choice 483 is Space Trip (Scadian Homeworld)
    // Choice 484 is Space Trip (End)
    // Choice 485 is Fighters of Fighting
    // Choice 486 is Dungeon Fist!
    // Choice 487 is unknown
    // Choice 488 is Meteoid (Bridge)
    // Choice 489 is Meteoid (SpaceMall)
    // Choice 490 is Meteoid (Underground Complex)
    // Choice 491 is Meteoid (End)
    // Choice 492 is unknown
    // Choice 493 is unknown
    // Choice 494 is unknown
    // Choice 495 is unknown

    // Choice 496 is Crate Expectations
    // -> can skip if have +20 hot damage

    // Choice 497 is SHAFT!
    // Choice 498 is unknown
    // Choice 499 is unknown
    // Choice 500 is unknown
    // Choice 501 is unknown

    // Choice 502 is Arboreal Respite

    // The Road Less Traveled
    new ChoiceSpoiler(
        "choiceAdventure503",
        "Spooky Forest",
        new Object[] {
          "gain some meat",
          new Option("gain stakes or trade vampire hearts", "wooden stakes"),
          new Option("gain spooky sapling or trade bar skins", "spooky sapling")
        }),

    // Tree's Last Stand
    new ChoiceSpoiler(
        "choiceAdventure504",
        "Spooky Forest",
        new Object[] {
          new Option("bar skin", "bar skin"),
          new Option("bar skins", "bar skin"),
          new Option("buy spooky sapling", "spooky sapling"),
          "skip adventure"
        }),
    // Tree's Last Stand
    new Object[] {IntegerPool.get(504), IntegerPool.get(1), ItemPool.get(ItemPool.BAR_SKIN, -1)},
    new Object[] {IntegerPool.get(504), IntegerPool.get(2), ItemPool.get(ItemPool.BAR_SKIN, 1)},
    new Object[] {
      IntegerPool.get(504), IntegerPool.get(3), new AdventureResult(AdventureResult.MEAT, -100)
    },

    // Consciousness of a Stream
    new ChoiceSpoiler(
        "choiceAdventure505",
        "Spooky Forest",
        new Object[] {
          new Option("gain mosquito larva then 3 spooky mushrooms", "mosquito larva"),
          "gain 300 meat & tree-holed coin then nothing",
          "fight a spooky vampire"
        }),

    // Through Thicket and Thinnet
    new ChoiceSpoiler(
        "choiceAdventure506",
        "Spooky Forest",
        new Object[] {
          "gain a starter item",
          new Option("gain Spooky-Gro fertilizer", "Spooky-Gro fertilizer"),
          new Option("gain spooky temple map", "spooky temple map")
        }),

    // O Lith, Mon
    new ChoiceSpoiler(
        "choiceAdventure507",
        "Spooky Forest",
        new Object[] {"gain Spooky Temple map", "skip adventure", "skip adventure"}),
    // O Lith, Mon
    new Object[] {
      IntegerPool.get(507), IntegerPool.get(1), ItemPool.get(ItemPool.TREE_HOLED_COIN, -1)
    },

    // Choice 508 is Pants-Gazing
    // Choice 509 is Of Course!
    // Choice 510 is Those Who Came Before You

    // If it's Tiny, is it Still a Mansion?
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure511",
        "Typical Tavern",
        new Object[] {"Baron von Ratsworth", "skip adventure"}),

    // Hot and Cold Running Rats
    new ChoiceAdventure(
        "Woods", "choiceAdventure512", "Typical Tavern", new Object[] {"fight", "skip adventure"}),

    // Choice 513 is Staring Down the Barrel
    // -> can skip if have +20 cold damage
    // Choice 514 is 1984 Had Nothing on This Cellar
    // -> can skip if have +20 stench damage
    // Choice 515 is A Rat's Home...
    // -> can skip if have +20 spooky damage

    // Choice 516 is unknown
    // Choice 517 is Mr. Alarm, I Presarm

    // Clear and Present Danger
    new ChoiceAdventure(
        "Crimbo10",
        "choiceAdventure518",
        "Elf Alley",
        new Object[] {"enter combat with Uncle Hobo", "skip adventure"}),

    // What a Tosser
    new ChoiceAdventure(
        "Crimbo10",
        "choiceAdventure519",
        "Elf Alley",
        new Object[] {new Option("gift-a-pult", "gift-a-pult"), "skip adventure"}),
    // What a Tosser - gift-a-pult
    new Object[] {
      IntegerPool.get(519), IntegerPool.get(1), ItemPool.get(ItemPool.HOBO_NICKEL, -50)
    },

    // Choice 520 is A Show-ho-ho-down
    // Choice 521 is A Wicked Buzz

    // Welcome to the Footlocker
    new ChoiceAdventure(
        "Knob",
        "choiceAdventure522",
        "Cobb's Knob Barracks",
        new Object[] {"outfit piece or donut", "skip adventure"}),

    // Death Rattlin'
    new ChoiceAdventure(
        "Cyrpt",
        "choiceAdventure523",
        "Defiled Cranny",
        new Object[] {
          "small meat boost",
          "stats & HP & MP",
          new Option("can of Ghuol-B-Gone&trade;", "can of Ghuol-B-Gone&trade;"),
          "fight swarm of ghuol whelps",
          "skip adventure"
        }),

    // Choice 524 is The Adventures of Lars the Cyberian
    // Choice 525 is Fiddling with a Puzzle
    // Choice 526 is unknown

    // Choice 527 is The Haert of Darkness
    new ChoiceAdventure(
        "Cyrpt",
        "choiceAdventure527",
        "Haert of the Cyrpt",
        new Object[] {"fight the Bonerdagon", "skip adventure"}),

    // Choice 528 is It Was Then That a Hideous Monster Carried You

    // A Swarm of Yeti-Mounted Skeletons
    new ChoiceAdventure(
        "Events",
        "choiceAdventure529",
        "Skeleton Swarm",
        new Object[] {"Weapon Damage", "Spell Damage", "Ranged Damage"}),

    // It Was Then That...	Aaaaaaaah!
    new ChoiceAdventure(
        "Events",
        "choiceAdventure530",
        "Icy Peak",
        new Object[] {new Option("hideous egg", "hideous egg"), "skip the adventure"}),

    // The Bonewall Is In
    new ChoiceAdventure(
        "Events", "choiceAdventure531", "Bonewall", new Object[] {"Item Drop", "HP Bonus"}),

    // You'll Sink His Battleship
    new ChoiceAdventure(
        "Events",
        "choiceAdventure532",
        "Battleship",
        new Object[] {"Class Skills", "Accordion Thief Songs"}),

    // Train, Train, Choo-Choo Train
    new ChoiceAdventure(
        "Events",
        "choiceAdventure533",
        "Supply Train",
        new Object[] {"Meat Drop", "Pressure Penalty Modifiers"}),

    // That's No Bone Moon...
    new ChoiceAdventure(
        "Events",
        "choiceAdventure534",
        "Bone Star",
        new Object[] {
          new Option("Torpedos", "photoprotoneutron torpedo"), "Initiative", "Monster Level"
        }),

    // Deep Inside Ronald, Baby
    new ChoiceAdventure(
        "Spaaace", "choiceAdventure535", "Deep Inside Ronald", SafetyShelterManager.RonaldGoals),

    // Deep Inside Grimace, Bow Chick-a Bow Bow
    new ChoiceAdventure(
        "Spaaace", "choiceAdventure536", "Deep Inside Grimace", SafetyShelterManager.GrimaceGoals),

    // Choice 537 is Play Porko!
    // Choice 538 is Big-Time Generator
    // Choice 539 is An E.M.U. for Y.O.U.
    // Choice 540 is Big-Time Generator - game board
    // Choice 541 is unknown
    // Choice 542 is Now's Your Pants!  I Mean... Your Chance!
    // Choice 543 is Up In Their Grill
    // Choice 544 is A Sandwich Appears!
    // Choice 545 is unknown

    // Interview With You
    new ChoiceAdventure(
        "Item-Driven", "choiceAdventure546", "Interview With You", VampOutManager.VampOutGoals),

    // Behind Closed Doors
    new ChoiceAdventure(
        "Events",
        "choiceAdventure548",
        "Sorority House Necbromancer",
        new Object[] {"enter combat with The Necbromancer", "skip adventure"}),

    // Dark in the Attic
    new ChoiceSpoiler(
        "Events",
        "choiceAdventure549",
        "Dark in the Attic",
        new Object[] {
          new Option("staff guides", "Haunted Sorority House staff guide"),
          new Option("ghost trap", "ghost trap"),
          "raise area ML",
          "lower area ML",
          new Option("mass kill werewolves with silver shotgun shell", "silver shotgun shell")
        }),

    // The Unliving Room
    new ChoiceSpoiler(
        "Events",
        "choiceAdventure550",
        "The Unliving Room",
        new Object[] {
          "raise area ML",
          "lower area ML",
          new Option("mass kill zombies with chainsaw chain", "chainsaw chain"),
          new Option("mass kill skeletons with funhouse mirror", "funhouse mirror"),
          "get costume item"
        }),

    // Debasement
    new ChoiceSpoiler(
        "Events",
        "choiceAdventure551",
        "Debasement",
        new Object[] {
          "Prop Deportment",
          "mass kill vampires with plastic vampire fangs",
          "raise area ML",
          "lower area ML"
        }),

    // Prop Deportment
    new ChoiceSpoiler(
        "Events",
        "choiceAdventure552",
        "Prop Deportment",
        new Object[] {
          new Option("chainsaw chain", "chainsaw chain"),
          new Option("create a silver shotgun shell", "silver shotgun shell"),
          new Option("funhouse mirror", "funhouse mirror")
        }),

    // Relocked and Reloaded
    new ChoiceSpoiler(
        "Events",
        "choiceAdventure553",
        "Relocked and Reloaded",
        new Object[] {
          new Option("", "Maxwell's Silver hammer"),
          new Option("", "silver tongue charrrm bracelet"),
          new Option("", "silver cheese-slicer"),
          new Option("", "silver shrimp fork"),
          new Option("", "silver pat&eacute; knife"),
          "exit adventure"
        }),

    // Behind the Spooky Curtain
    new ChoiceSpoiler(
        "Events",
        "choiceAdventure554",
        "Behind the Spooky Curtain",
        new Object[] {
          "staff guides, ghost trap, kill werewolves",
          "kill zombies, kill skeletons, costume item",
          "chainsaw chain, silver item, funhouse mirror, kill vampires"
        }),

    // More Locker Than Morlock
    new ChoiceAdventure(
        "McLarge",
        "choiceAdventure556",
        "Itznotyerzitz Mine",
        new Object[] {"get an outfit piece", "skip adventure"}),

    // Gingerbread Homestead
    new ChoiceAdventure(
        "The Candy Diorama",
        "choiceAdventure557",
        "Gingerbread Homestead",
        new Object[] {
          "get candies",
          new Option("licorice root", "licorice root"),
          new Option("skip adventure or make a lollipop stick item", "lollipop stick")
        }),

    // Tool Time
    new ChoiceAdventure(
        "The Candy Diorama",
        "choiceAdventure558",
        "Tool Time",
        new Object[] {
          new Option("sucker bucket", "sucker bucket"),
          new Option("sucker kabuto", "sucker kabuto"),
          new Option("sucker hakama", "sucker hakama"),
          new Option("sucker tachi", "sucker tachi"),
          new Option("sucker scaffold", "sucker scaffold"),
          "skip adventure"
        }),

    // Fudge Mountain Breakdown
    new ChoiceAdventure(
        "The Candy Diorama",
        "choiceAdventure559",
        "Fudge Mountain Breakdown",
        new Object[] {
          new Option("fudge lily", "fudge lily"),
          "fight a swarm of fudgewasps or skip adventure",
          new Option("frigid fudgepuck or skip adventure", "frigid fudgepuck"),
          new Option("superheated fudge or skip adventure", "superheated fudge")
        }),

    // Foreshadowing Demon!
    new ChoiceAdventure(
        "Suburbs",
        "choiceAdventure560",
        "The Clumsiness Grove",
        new Object[] {"head towards boss", "skip adventure"}),

    // You Must Choose Your Destruction!
    new ChoiceAdventure(
        "Suburbs",
        "choiceAdventure561",
        "The Clumsiness Grove",
        new Object[] {"The Thorax", "The Bat in the Spats"}),

    // Choice 562 is You're the Fudge Wizard Now, Dog

    // A Test of your Mettle
    new ChoiceAdventure(
        "Suburbs",
        "choiceAdventure563",
        "The Clumsiness Grove",
        new Object[] {"Fight Boss", "skip adventure"}),

    // A Maelstrom of Trouble
    new ChoiceAdventure(
        "Suburbs",
        "choiceAdventure564",
        "The Maelstrom of Lovers",
        new Object[] {"head towards boss", "skip adventure"}),

    // To Get Groped or Get Mugged?
    new ChoiceAdventure(
        "Suburbs",
        "choiceAdventure565",
        "The Maelstrom of Lovers",
        new Object[] {"The Terrible Pinch", "Thug 1 and Thug 2"}),

    // A Choice to be Made
    new ChoiceAdventure(
        "Suburbs",
        "choiceAdventure566",
        "The Maelstrom of Lovers",
        new Object[] {"Fight Boss", "skip adventure"}),

    // You May Be on Thin Ice
    new ChoiceAdventure(
        "Suburbs",
        "choiceAdventure567",
        "The Glacier of Jerks",
        new Object[] {"Fight Boss", "skip adventure"}),

    // Some Sounds Most Unnerving
    new ChoiceAdventure(
        "Suburbs",
        "choiceAdventure568",
        "The Glacier of Jerks",
        new Object[] {"Mammon the Elephant", "The Large-Bellied Snitch"}),

    // One More Demon to Slay
    new ChoiceAdventure(
        "Suburbs",
        "choiceAdventure569",
        "The Glacier of Jerks",
        new Object[] {"head towards boss", "skip adventure"}),

    // Choice 571 is Your Minstrel Vamps
    // Choice 572 is Your Minstrel Clamps
    // Choice 573 is Your Minstrel Stamps
    // Choice 574 is The Minstrel Cycle Begins

    // Duffel on the Double
    new ChoiceAdventure(
        "McLarge",
        "choiceAdventure575",
        "eXtreme Slope",
        new Object[] {
          "get an outfit piece",
          new Option("jar of frostigkraut", "jar of frostigkraut"),
          "skip adventure",
          new Option("lucky pill", "lucky pill")
        }),

    // Choice 576 is Your Minstrel Camps
    // Choice 577 is Your Minstrel Scamp
    // Choice 578 is End of the Boris Road

    // Such Great Heights
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure579",
        "Hidden Temple Heights",
        new Object[] {
          "mysticality substats",
          new Option("Nostril of the Serpent then skip adventure", "Nostril of the Serpent"),
          "gain 3 adv then skip adventure"
        }),

    // Choice 580 is The Hidden Heart of the Hidden Temple (4 variations)

    // Such Great Depths
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure581",
        "Hidden Temple Depths",
        new Object[] {
          new Option("glowing fungus", "glowing fungus"),
          "+15 mus/mys/mox then skip adventure",
          "fight clan of cave bars"
        }),

    // Fitting In
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure582",
        "Hidden Temple",
        new Object[] {"Such Great Heights", "heart of the Hidden Temple", "Such Great Depths"}),

    // Confusing Buttons
    new ChoiceSpoiler(
        "Woods", "choiceAdventure583", "Hidden Temple", new Object[] {"Press a random button"}),

    // Unconfusing Buttons
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure584",
        "Hidden Temple",
        new Object[] {
          "Hidden Temple (Stone) - muscle substats",
          "Hidden Temple (Sun) - gain ancient calendar fragment",
          "Hidden Temple (Gargoyle) - MP",
          "Hidden Temple (Pikachutlotal) - Hidden City unlock"
        }),

    // A Lost Room
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure594",
        "Lost Key",
        new Object[] {
          new Option("lost glasses", "lost glasses"),
          new Option("lost comb", "lost comb"),
          new Option("lost pill bottle", "lost pill bottle")
        }),

    // Choice 585 is Screwing Around!
    // Choice 586 is All We Are Is Radio Huggler

    // Choice 588 is Machines!
    // Choice 589 is Autopsy Auturvy
    // Choice 590 is Not Alone In The Dark

    // Fire! I... have made... fire!
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure595",
        "CSA fire-starting kit",
        new Object[] {"pvp fights", "hp/mp regen"}),

    // Choice 596 is Dawn of the D'oh

    // Cake Shaped Arena
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure597",
        "Reagnimated Gnome",
        new Object[] {
          new Option("gnomish swimmer's ears (underwater)", "gnomish swimmer's ears"),
          new Option("gnomish coal miner's lung (block)", "gnomish coal miner's lung"),
          new Option("gnomish tennis elbow (damage)", "gnomish tennis elbow"),
          new Option("gnomish housemaid's kgnee (gain advs)", "gnomish housemaid's kgnee"),
          new Option("gnomish athlete's foot (delevel)", "gnomish athlete's foot")
        }),

    // Choice 598 is Recruitment Jive
    // Choice 599 is A Zombie Master's Bait
    // Choice 600 is Summon Minion
    // Choice 601 is Summon Horde
    // Choice 602 is Behind the Gash

    // Skeletons and The Closet
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure603",
        "Skeleton",
        new Object[] {
          "warrior (dmg, delevel)",
          "cleric (hot dmg, hp)",
          "wizard (cold dmg, mp)",
          "rogue (dmg, meat)",
          "buddy (delevel, exp)",
          "ignore this adventure"
        }),

    // Choice 604 is unknown
    // Choice 605 is Welcome to the Great Overlook Lodge
    // Choice 606 is Lost in the Great Overlook Lodge
    // Choice 607 is Room 237
    // Choice 608 is Go Check It Out!
    // Choice 609 is There's Always Music In the Air
    // Choice 610 is To Catch a Killer
    // Choice 611 is The Horror... (A-Boo Peak)
    // Choice 612 is Behind the world there is a door...
    // Choice 613 is Behind the door there is a fog
    // Choice 614 is Near the fog there is an... anvil?
    // Choice 615 is unknown

    // Choice 616 is He Is the Arm, and He Sounds Like This
    // Choice 617 is Now It's Dark
    // Choice 618 is Cabin Fever
    // Choice 619 is To Meet a Gourd
    // Choice 620 is A Blow Is Struck!
    // Choice 621 is Hold the Line!
    // Choice 622 is The Moment of Truth
    // Choice 623 is Return To the Fray!
    // Choice 624 is Returning to Action
    // Choice 625 is The Table
    // Choice 626 is Super Crimboman Crimbo Type is Go!
    // Choice 627 is unknown
    // Choice 628 is unknown
    // Choice 629 is unknown
    // Choice 630 is unknown
    // Choice 631 is unknown
    // Choice 632 is unknown
    // Choice 633 is ChibiBuddy&trade;
    // Choice 634 is Goodbye Fnord
    // Choice 635 is unknown
    // Choice 636 is unknown
    // Choice 637 is unknown
    // Choice 638 is unknown
    // Choice 639 is unknown

    // Choice 640 is Tailor the Snow Suit
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure640",
        "Snow Suit",
        new Object[] {
          "Familiar does physical damage",
          "Familiar does cold damage",
          "+10% item drops, can drop carrot nose",
          "Heals 1-20 HP after combat",
          "Restores 1-10 MP after combat"
        }),

    // Choice 641 is Stupid Pipes.
    // Choice 642 is You're Freaking Kidding Me
    // Choice 643 is Great. A Stupid Door. What Next?
    // Choice 644 is Snakes.
    // Choice 645 is So... Many... Skulls...
    // Choice 646 is Oh No... A Door...
    // Choice 647 is A Stupid Dummy. Also, a Straw Man.
    // Choice 648 is Slings and Arrows
    // Choice 649 is A Door. Figures.
    // Choice 650 is This Is Your Life. Your Horrible, Horrible Life.
    // Choice 651 is The Wall of Wailing
    // Choice 652 is A Door. Too Soon...
    // Choice 653 is unknown
    // Choice 654 is Courier? I don't even...
    // Choice 655 is They Have a Fight, Triangle Loses
    // Choice 656 is Wheels Within Wheel

    // You Grind 16 Rats, and Whaddya Get?
    new ChoiceAdventure(
        "Psychoses",
        "choiceAdventure657",
        "Chinatown Tenement",
        new Object[] {"Fight Boss", "skip adventure"}),

    // Choice 658 is Debasement
    // Choice 659 is How Does a Floating Platform Even Work?
    // Choice 660 is It's a Place Where Books Are Free
    // Choice 661 is Sphinx For the Memories
    // Choice 662 is Think or Thwim
    // Choice 663 is When You're a Stranger
    // Choice 664 is unknown
    // Choice 665 is A Gracious Maze
    // Choice 666 is unknown
    // Choice 667 is unknown
    // Choice 668 is unknown

    // The Fast and the Furry-ous
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure669",
        "Basement Furry",
        new Object[] {
          "Open Ground Floor with titanium umbrella, otherwise Neckbeard Choice",
          "200 Moxie substats",
          "???",
          "skip adventure and guarantee this adventure will reoccur"
        }),

    // You Don't Mess Around with Gym
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure670",
        "Basement Fitness",
        new Object[] {
          new Option("massive dumbbell, then skip adventure", "massive dumbbell"),
          "Muscle stats",
          "Items",
          "Open Ground Floor with amulet, otherwise skip",
          "skip adventure and guarantee this adventure will reoccur"
        }),

    // Out in the Open Source
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure671",
        "Basement Neckbeard",
        new Object[] {
          new Option(
              "With massive dumbbell, open Ground Floor, otherwise skip adventure",
              "massive dumbbell"),
          "200 Mysticality substats",
          "O'RLY manual, open sauce",
          "Fitness Choice"
        }),

    // There's No Ability Like Possibility
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure672",
        "Ground Possibility",
        new Object[] {"3 random items", "Nothing Is Impossible", "skip adventure"}),

    // Putting Off Is Off-Putting
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure673",
        "Ground Procrastination",
        new Object[] {
          new Option("very overdue library book, then skip adventure", "very overdue library book"),
          "Trash-Wrapped",
          "skip adventure"
        }),

    // Huzzah!
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure674",
        "Ground Renaissance",
        new Object[] {
          new Option("pewter claymore, then skip adventure", "pewter claymore"),
          "Pretending to Pretend",
          "skip adventure"
        }),

    // Melon Collie and the Infinite Lameness
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure675",
        "Top Goth",
        new Object[] {
          "Fight a Goth Giant",
          new Option("complete quest", "drum 'n' bass 'n' drum 'n' bass record"),
          new Option("3 thin black candles", "thin black candle"),
          "Steampunk Choice"
        }),

    // Flavor of a Raver
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure676",
        "Top Raver",
        new Object[] {
          "Fight a Raver Giant",
          "Restore 1000 hp & mp",
          new Option(
              "drum 'n' bass 'n' drum 'n' bass record, then skip adventure",
              "drum 'n' bass 'n' drum 'n' bass record"),
          "Punk Rock Choice"
        }),

    // Copper Feel
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure677",
        "Top Steampunk",
        new Object[] {
          new Option(
              "With model airship, complete quest, otherwise fight Steampunk Giant",
              "model airship"),
          new Option(
              "steam-powered model rocketship, then skip adventure",
              "steam-powered model rocketship"),
          new Option("brass gear", "brass gear"),
          "Goth Choice"
        }),

    // Yeah, You're for Me, Punk Rock Giant
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure678",
        "Top Punk Rock",
        new Object[] {
          "Wearing mohawk wig, turn wheel, otherwise fight Punk Rock Giant",
          "500 meat",
          "Steampunk Choice",
          "Raver Choice"
        }),

    // Choice 679 is Keep On Turnin' the Wheel in the Sky
    // Choice 680 is Are you a Man or a Mouse?
    // Choice 681 is F-F-Fantastic!
    // Choice 682 is Now Leaving Jarlsberg, Population You

    // Choice 686 is Of Might and Magic

    // Choice 689 is The Final Chest
    new ChoiceAdventure(
        "Dungeon",
        "choiceAdventure689",
        "Daily Dungeon: Chest 3",
        new Object[] {"Get fat loot token"}),

    // The First Chest Isn't the Deepest.
    new ChoiceAdventure(
        "Dungeon",
        "choiceAdventure690",
        "Daily Dungeon: Chest 1",
        new Object[] {
          "Get item", "Skip to 8th chamber, no turn spent", "Skip to 6th chamber, no turn spent"
        }),

    // Second Chest
    new ChoiceAdventure(
        "Dungeon",
        "choiceAdventure691",
        "Daily Dungeon: Chest 2",
        new Object[] {
          "Get item", "Skip to 13th chamber, no turn spent", "Skip to 11th chamber, no turn spent"
        }),

    // Choice 692 is I Wanna Be a Door

    // It's Almost Certainly a Trap
    new ChoiceAdventure(
        "Dungeon",
        "choiceAdventure693",
        "Daily Dungeon: Traps",
        new Object[] {
          "Suffer elemental damage, get stats",
          "Avoid trap with eleven-foot pole, no turn spent",
          "Leave, no turn spent"
        }),

    // Choice 695 is A Drawer of Chests

    // Choice 696 is Stick a Fork In It
    new ChoiceAdventure(
        "Le Marais D&egrave;gueulasse",
        "choiceAdventure696",
        "Edge of the Swamp",
        new Object[] {"unlock The Dark and Spooky Swamp", "unlock The Wildlife Sanctuarrrrrgh"}),

    // Choice 697 is Sophie's Choice
    new ChoiceAdventure(
        "Le Marais D&egrave;gueulasse",
        "choiceAdventure697",
        "Dark and Spooky Swamp",
        new Object[] {"unlock The Corpse Bog", "unlock The Ruined Wizard Tower"}),

    // Choice 698 is From Bad to Worst
    new ChoiceAdventure(
        "Le Marais D&egrave;gueulasse",
        "choiceAdventure698",
        "Wildlife Sanctuarrrrrgh",
        new Object[] {"unlock Swamp Beaver Territory", "unlock The Weird Swamp Village"}),

    // Choice 701 is Ators Gonna Ate
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure701",
        "Mer-kin Gymnasium",
        new Object[] {"get an item", "skip adventure"}),

    // Choice 703 is Mer-kin dreadscroll
    // Choice 704 is Playing the Catalog Card
    // Choice 705 is Halls Passing in the Night
    new ChoiceAdventure(
        "The Sea",
        "choiceAdventure705",
        "Mer-kin Elementary School",
        new Object[] {
          "fight a Mer-kin spectre",
          new Option("Mer-kin sawdust", "Mer-kin sawdust"),
          new Option("Mer-kin cancerstick", "Mer-kin cancerstick"),
          new Option("Mer-kin wordquiz", "Mer-kin wordquiz")
        }),

    //     Shub-Jigguwatt (Violence) path
    // Choice 706 is In The Temple of Violence, Shine Like Thunder
    // Choice 707 is Flex Your Pecs in the Narthex
    // Choice 708 is Don't Falter at the Altar
    // Choice 709 is You Beat Shub to a Stub, Bub

    //     Yog-Urt (Hatred) path
    // Choice 710 is They've Got Fun and Games
    // Choice 711 is They've Got Everything You Want
    // Choice 712 is Honey, They Know the Names
    // Choice 713 is You Brought Her To Her Kn-kn-kn-kn-knees, Knees.

    //     Dad Sea Monkee (Loathing) path
    // Choice 714 is An Unguarded Door (1)
    // Choice 715 is Life in the Stillness
    // Choice 716 is An Unguarded Door (2)
    // Choice 717 is Over. Over Now.

    // The Cabin in the Dreadsylvanian Woods
    new ChoiceAdventure(
        "Dreadsylvania",
        "choiceAdventure721",
        "Cabin",
        new Object[] {new Option("learn shortcut", 5), new Option("skip adventure", 6)},
        1),

    // Choice 722 is The Kitchen in the Woods
    // Choice 723 is What Lies Beneath (the Cabin)
    // Choice 724 is Where it's Attic

    // Tallest Tree in the Forest
    new ChoiceAdventure(
        "Dreadsylvania",
        "choiceAdventure725",
        "Tallest Tree",
        new Object[] {new Option("learn shortcut", 5), new Option("skip adventure", 6)},
        2),

    // Choice 726 is Top of the Tree, Ma!
    // Choice 727 is All Along the Watchtower
    // Choice 728 is Treebasing

    // Below the Roots
    new ChoiceAdventure(
        "Dreadsylvania",
        "choiceAdventure729",
        "Burrows",
        new Object[] {new Option("learn shortcut", 5), new Option("skip adventure", 6)},
        3),

    // Choice 730 is Hot Coals
    // Choice 731 is The Heart of the Matter
    // Choice 732 is Once Midden, Twice Shy

    // Dreadsylvanian Village Square
    new ChoiceAdventure(
        "Dreadsylvania",
        "choiceAdventure733",
        "Village Square",
        new Object[] {new Option("learn shortcut", 5), new Option("skip adventure", 6)},
        4),

    // Choice 734 is Fright School
    // Choice 735 is Smith, Black as Night
    // Choice 736 is Gallows

    // The Even More Dreadful Part of Town
    new ChoiceAdventure(
        "Dreadsylvania",
        "choiceAdventure737",
        "Skid Row",
        new Object[] {new Option("learn shortcut", 5), new Option("skip adventure", 6)},
        5),

    // Choice 738 is A Dreadful Smell
    // Choice 739 is The Tinker's. Damn.
    // Choice 740 is Eight, Nine, Tenement

    // The Old Duke's Estate
    new ChoiceAdventure(
        "Dreadsylvania",
        "choiceAdventure741",
        "Old Duke's Estate",
        new Object[] {new Option("learn shortcut", 5), new Option("skip adventure", 6)},
        6),

    // Choice 742 is The Plot Thickens
    // Choice 743 is No Quarter
    // Choice 744 is The Master Suite -- Sweet!

    // This Hall is Really Great
    new ChoiceAdventure(
        "Dreadsylvania",
        "choiceAdventure745",
        "Great Hall",
        new Object[] {new Option("learn shortcut", 5), new Option("skip adventure", 6)},
        8),

    // Choice 746 is The Belle of the Ballroom
    // Choice 747 is Cold Storage
    // Choice 748 is Dining In (the Castle)

    // Tower Most Tall
    new ChoiceAdventure(
        "Dreadsylvania",
        "choiceAdventure749",
        "Tower",
        new Object[] {new Option("learn shortcut", 5), new Option("skip adventure", 6)},
        7),

    // Choice 750 is Working in the Lab, Late One Night
    // Choice 751 is Among the Quaint and Curious Tomes.
    // Choice 752 is In The Boudoir

    // The Dreadsylvanian Dungeon
    new ChoiceAdventure(
        "Dreadsylvania",
        "choiceAdventure753",
        "Dungeons",
        new Object[] {new Option("learn shortcut", 5), new Option("skip adventure", 6)},
        9),

    // Choice 754 is Live from Dungeon Prison
    // Choice 755 is The Hot Bowels
    // Choice 756 is Among the Fungus

    // Choice 757 is ???

    // Choice 758 is End of the Path
    // Choice 759 is You're About to Fight City Hall
    // Choice 760 is Holding Court
    // Choice 761 is Staring Upwards...
    // Choice 762 is Try New Extra-Strength Anvil
    // Choice 763 is ???
    // Choice 764 is The Machine
    // Choice 765 is Hello Gallows
    // Choice 766 is ???
    // Choice 767 is Tales of Dread

    // Choice 768 is The Littlest Identity Crisis
    // Choice 771 is It Was All a Horrible, Horrible Dream

    // Choice 772 is Saved by the Bell
    // Choice 774 is Opening up the Folder Holder

    // Choice 778 is If You Could Only See
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure778",
        "Tonic Djinn",
        new Object[] {
          new Option("gain 400-500 meat", 1),
          new Option("gain 50-60 muscle stats", 2),
          new Option("gain 50-60 mysticality stats", 3),
          new Option("gain 50-60 moxie stats", 4),
          new Option("don't use it", 6)
        }),

    // Choice 780 is Action Elevator
    // Choice 781 is Earthbound and Down
    // Choice 783 is Water You Dune
    // Choice 784 is You, M. D.
    // Choice 785 is Air Apparent
    // Choice 786 is Working Holiday
    // Choice 787 is Fire when Ready
    // Choice 788 is Life is Like a Cherry of Bowls
    // Choice 789 is Where Does The Lone Ranger Take His Garbagester?
    // Choice 791 is Legend of the Temple in the Hidden City

    // Choice 793 is Welcome to The Shore, Inc.
    new ChoiceAdventure(
        "Beach",
        "choiceAdventure793",
        "The Shore",
        new Object[] {"Muscle Vacation", "Mysticality Vacation", "Moxie Vacation"}),

    // Choice 794 is Once More Unto the Junk
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure794",
        "The Old Landfill",
        new Object[] {
          "The Bathroom of Ten Men", "The Den of Iquity", "Let's Workshop This a Little"
        }),
    // Choice 795 is The Bathroom of Ten Men
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure795",
        "The Bathroom of Ten Men",
        new Object[] {
          new Option("old claw-foot bathtub", "old claw-foot bathtub"),
          "fight junksprite",
          "make lots of noise"
        }),
    // Choice 796 is The Den of Iquity
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure796",
        "The Den of Iquity",
        new Object[] {
          "make lots of noise",
          new Option("old clothesline pole", "old clothesline pole"),
          new Option("tangle of copper wire", "tangle of copper wire")
        }),
    // Choice 797 is Let's Workshop This a Little
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure797",
        "Let's Workshop This a Little",
        new Object[] {
          new Option("Junk-Bond", "Junk-Bond"),
          "make lots of noise",
          new Option("antique cigar sign", "antique cigar sign")
        }),

    // Choice 801 is A Reanimated Conversation

    // Choice 803 is Behind the Music.  Literally.
    new ChoiceAdventure(
        "Events",
        "choiceAdventure803",
        "The Space Odyssey Discotheque",
        new Object[] {
          new Option("gain 2-3 horoscopes", 1),
          new Option("find interesting room", 3),
          new Option("investigate interesting room", 4),
          new Option("investigate trap door", 5),
          new Option("investigate elevator", 6)
        }),

    // Choice 804 is Trick or Treat!

    // Choice 805 is A Sietch in Time
    new ChoiceAdventure(
        "Beach", "choiceAdventure805", "Arid, Extra-Dry Desert", new Object[] {"talk to Gnasir"}),

    // Choice 808 is Silence at Last.
    new ChoiceAdventure(
        "Events",
        "choiceAdventure808",
        "The Spirit World",
        new Object[] {new Option("gain spirit bed piece"), new Option("fight spirit alarm clock")}),

    // Choice 809 is Uncle Crimbo's Trailer
    // Choice 810 is K.R.A.M.P.U.S. facility

    // Choice 813 is What Warbears Are Good For
    new ChoiceAdventure(
        "Crimbo13",
        "choiceAdventure813",
        "Warbear Fortress (First Level)",
        new Object[] {"Open K.R.A.M.P.U.S. facility"}),

    // Choice 822 is The Prince's Ball (In the Restroom)
    // Choice 823 is The Prince's Ball (On the Dance Floor)
    // Choice 824 is The Prince's Ball (In the Kitchen)
    // Choice 825 is The Prince's Ball (On the Balcony)
    // Choice 826 is The Prince's Ball (In the Lounge)
    // Choice 827 is The Prince's Ball (At the Canap&eacute;s Table)

    // Choice 829 is We All Wear Masks

    // Choice 830 is Cooldown
    new ChoiceAdventure(
        "Skid Row",
        "choiceAdventure830",
        "Cooldown",
        new Object[] {
          "+Wolf Offence or +Wolf Defence",
          "+Wolf Elemental Attacks or +Rabbit",
          "Improved Howling! or +Wolf Lung Capacity",
          new Option("Leave", 6)
        }),
    // Choice 832 is Shower Power
    new ChoiceAdventure(
        "Skid Row",
        "choiceAdventure832",
        "Shower Power",
        new Object[] {"+Wolf Offence", "+Wolf Defence"}),
    // Choice 833 is Vendie, Vidi, Vici
    new ChoiceAdventure(
        "Skid Row",
        "choiceAdventure833",
        "Vendie, Vidi, Vici",
        new Object[] {"+Wolf Elemental Attacks", "+Rabbit"}),
    // Choice 834 is Back Room Dealings
    new ChoiceAdventure(
        "Skid Row",
        "choiceAdventure834",
        "Back Room Dealings",
        new Object[] {new Option("Improved Howling!", 2), new Option("+Wolf Lung Capacity", 3)}),

    // Choice 835 is Barely Tales
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure835",
        "Grim Brother",
        new Object[] {
          "30 turns of +20 initiative",
          "30 turns of +20 max HP, +10 max MP",
          "30 turns of +10 Weapon Damage, +20 Spell Damage"
        }),

    // Choice 836 is Adventures Who Live in Ice Houses...

    // Choice 837 is On Purple Pond
    new ChoiceAdventure(
        "The Candy Witch and the Relentless Child Thieves",
        "choiceAdventure837",
        "On Purple Pond",
        new Object[] {"find out the two children not invading", "+1 Moat", "gain Candy"}),
    // Choice 838 is General Mill
    new ChoiceAdventure(
        "The Candy Witch and the Relentless Child Thieves",
        "choiceAdventure838",
        "General Mill",
        new Object[] {"+1 Moat", "gain Candy"}),
    // Choice 839 is On The Sounds of the Undergrounds
    new ChoiceAdventure(
        "The Candy Witch and the Relentless Child Thieves",
        "choiceAdventure839",
        "The Sounds of the Undergrounds",
        new Object[] {
          "learn what the first two waves will be", "+1 Minefield Strength", "gain Candy"
        }),
    // Choice 840 is Hop on Rock Pops
    new ChoiceAdventure(
        "The Candy Witch and the Relentless Child Thieves",
        "choiceAdventure840",
        "Hop on Rock Pops",
        new Object[] {"+1 Minefield Strength", "gain Candy"}),
    // Choice 841 is Building, Structure, Edifice
    new ChoiceAdventure(
        "The Candy Witch and the Relentless Child Thieves",
        "choiceAdventure841",
        "Building, Structure, Edifice",
        new Object[] {"increase candy in another location", "+2 Random Defense", "gain Candy"}),
    // Choice 842 is The Gingerbread Warehouse
    new ChoiceAdventure(
        "The Candy Witch and the Relentless Child Thieves",
        "choiceAdventure842",
        "The Gingerbread Warehouse",
        new Object[] {
          "+1 Wall Strength", "+1 Poison Jar", "+1 Anti-Aircraft Turret", "gain Candy"
        }),

    // Choice 844 is The Portal to Horrible Parents
    // Choice 845 is Rumpelstiltskin's Workshop
    // Choice 846 is Bartering for the Future of Innocent Children
    // Choice 847 is Pick Your Poison
    // Choice 848 is Where the Magic Happens
    // Choice 850 is World of Bartercraft

    // Choice 851 is Shen Copperhead, Nightclub Owner
    // Choice 852 is Shen Copperhead, Jerk
    // Choice 853 is Shen Copperhead, Huge Jerk
    // Choice 854 is Shen Copperhead, World's Biggest Jerk
    // Choice 855 is Behind the 'Stache
    new ChoiceAdventure(
        "Town",
        "choiceAdventure855",
        "Behind the 'Stache",
        new Object[] {
          "don't take initial damage in fights",
          "can get priceless diamond",
          "can make Flamin' Whatshisname",
          "get 4-5 random items"
        }),
    // Choice 856 is This Looks Like a Good Bush for an Ambush
    new ChoiceAdventure(
        "The Red Zeppelin's Mooring",
        "choiceAdventure856",
        "This Looks Like a Good Bush for an Ambush",
        new Object[] {"scare protestors (more with lynyrd gear)", "skip adventure"}),
    // Choice 857 is Bench Warrant
    new ChoiceAdventure(
        "The Red Zeppelin's Mooring",
        "choiceAdventure857",
        "Bench Warrant",
        new Object[] {
          "creep protestors (more with sleaze damage/sleaze spell damage)", "skip adventure"
        }),
    // Choice 858 is Fire Up Above
    new ChoiceAdventure(
        "The Red Zeppelin's Mooring",
        "choiceAdventure858",
        "Fire Up Above",
        new Object[] {"set fire to protestors (more with Flamin' Whatshisname)", "skip adventure"}),
    // Choice 866 is Methinks the Protesters Doth Protest Too Little
    new ChoiceAdventure(
        "The Red Zeppelin's Mooring",
        "choiceAdventure866",
        "Methinks the Protesters Doth Protest Too Little",
        new Object[] {
          "scare protestors (more with lynyrd gear)",
          "creep protestors (more with sleaze damage/sleaze spell damage)",
          "set fire to protestors (more with Flamin' Whatshisname)"
        }),

    // Rod Nevada, Vendor
    new ChoiceSpoiler(
        "Plains",
        "choiceAdventure873",
        "The Palindome",
        new Object[] {
          new Option("photograph of a red nugget", "photograph of a red nugget"), "skip adventure"
        }),
    // Rod Nevada, Vendor
    new Object[] {
      IntegerPool.get(873), IntegerPool.get(1), new AdventureResult(AdventureResult.MEAT, -500)
    },

    // Welcome To Our ool Table
    new ChoiceAdventure(
        "Manor1",
        "choiceAdventure875",
        "Pool Table",
        new Object[] {"try to beat ghost", "improve pool skill", "skip"}),

    // One Simple Nightstand
    new ChoiceAdventure(
        "Manor2",
        "choiceAdventure876",
        "One Simple Nightstand",
        new Object[] {
          new Option("old leather wallet", 1),
          new Option("muscle substats", 2),
          new Option("muscle substats (with ghost key)", 3),
          new Option("skip", 6)
        }),

    // One Mahogany Nightstand
    new ChoiceAdventure(
        "Manor2",
        "choiceAdventure877",
        "One Mahogany Nightstand",
        new Object[] {
          new Option("old coin purse or half a memo", 1),
          new Option("take damage", 2),
          new Option("quest item", 3),
          new Option("gain more meat (with ghost key)", 4),
          new Option("skip", 6)
        }),

    // One Ornate Nightstand
    new ChoiceAdventure(
        "Manor2",
        "choiceAdventure878",
        "One Ornate Nightstand",
        new Object[] {
          new Option("small meat boost", 1),
          new Option("mysticality substats", 2),
          new Option("Lord Spookyraven's spectacles", 3, "Lord Spookyraven's spectacles"),
          new Option("disposable instant camera", 4, "disposable instant camera"),
          new Option("mysticality substats (with ghost key)", 5),
          new Option("skip", 6)
        }),

    // One Rustic Nightstand
    new ChoiceAdventure(
        "Manor2",
        "choiceAdventure879",
        "One Rustic Nightstand",
        new Object[] {
          new Option("moxie", 1),
          new Option("grouchy restless spirit or empty drawer", 2, "grouchy restless spirit"),
          new Option("enter combat with mistress (1)", 3),
          new Option("Engorged Sausages and You or moxie", 4),
          new Option("moxie substats (with ghost key)", 5),
          new Option("skip", 6)
        }),

    // One Elegant Nightstand
    new ChoiceAdventure(
        "Manor2",
        "choiceAdventure880",
        "One Elegant Nightstand",
        new Object[] {
          new Option(
              "Lady Spookyraven's finest gown (once only)", 1, "Lady Spookyraven's finest gown"),
          new Option("elegant nightstick", 2, "elegant nightstick"),
          new Option("stats (with ghost key)", 3),
          new Option("skip", 6)
        }),

    // Off the Rack
    new ChoiceAdventure(
        "Manor2", "choiceAdventure882", "Bathroom Towel", new Object[] {"get towel", "skip"}),

    // Take a Look, it's in a Book!
    new ChoiceSpoiler(
        "choiceAdventure888",
        "Haunted Library",
        new Object[] {"background history", "cooking recipe", "other options", "skip adventure"}),

    // Take a Look, it's in a Book!
    new ChoiceSpoiler(
        "choiceAdventure889",
        "Haunted Library",
        new Object[] {
          new Option("background history", 1),
          new Option("cocktailcrafting recipe", 2),
          new Option("muscle substats", 3),
          new Option("dictionary", 4, "dictionary"),
          new Option("skip", 5)
        }),

    // Choice 890 is Lights Out in the Storage Room
    // Choice 891 is Lights Out in the Laundry Room
    // Choice 892 is Lights Out in the Bathroom
    // Choice 893 is Lights Out in the Kitchen
    // Choice 894 is Lights Out in the Library
    // Choice 895 is Lights Out in the Ballroom
    // Choice 896 is Lights Out in the Gallery
    // Choice 897 is Lights Out in the Bedroom
    // Choice 898 is Lights Out in the Nursery
    // Choice 899 is Lights Out in the Conservatory
    // Choice 900 is Lights Out in the Billiards Room
    // Choice 901 is Lights Out in the Wine Cellar
    // Choice 902 is Lights Out in the Boiler Room
    // Choice 903 is Lights Out in the Laboratory

    // Choices 904-913 are Escher print adventures

    // Louvre It or Leave It
    new ChoiceSpoiler(
        "choiceAdventure914",
        "Haunted Gallery",
        new Object[] {"Enter the Drawing", "skip adventure"}),

    // Choice 918 is Yachtzee!
    new ChoiceAdventure(
        "Spring Break Beach",
        "choiceAdventure918",
        "Yachtzee!",
        new Object[] {
          "get cocktail ingredients (sometimes Ultimate Mind Destroyer)",
          "get 5k meat and random item",
          "get Beach Bucks"
        }),

    // Choice 919 is Break Time!
    new ChoiceAdventure(
        "Spring Break Beach",
        "choiceAdventure919",
        "Break Time!",
        new Object[] {
          "get Beach Bucks",
          "+15ML on Sundaes",
          "+15ML on Burgers",
          "+15ML on Cocktails",
          "reset ML on monsters",
          "leave without using a turn"
        }),

    // Choice 920 is Eraser
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure920",
        "Eraser",
        new Object[] {
          "reset Buff Jimmy quests", "reset Taco Dan quests", "reset Broden quests", "don't use it"
        }),

    // Choice 921 is We'll All Be Flat

    // Choice 923 is All Over the Map
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure923",
        "Black Forest",
        new Object[] {
          "fight blackberry bush, visit cobbler, or raid beehive",
          "visit blacksmith",
          "visit black gold mine",
          "visit black church"
        }),

    // Choice 924 is You Found Your Thrill
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure924",
        "Blackberry",
        new Object[] {
          "fight blackberry bush", "visit cobbler", "head towards beehive (1)",
        }),

    // Choice 925 is The Blackest Smith
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure925",
        "Blacksmith",
        new Object[] {
          new Option("get black sword", 1, "black sword"),
          new Option("get black shield", 2, "black shield"),
          new Option("get black helmet", 3, "black helmet"),
          new Option("get black greaves", 4, "black greaves"),
          new Option("return to main choice", 6)
        }),

    // Choice 926 is Be Mine
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure926",
        "Black Gold Mine",
        new Object[] {
          new Option("get black gold", 1, "black gold"),
          new Option("get Texas tea", 2, "Texas tea"),
          new Option("get Black Lung effect", 3),
          new Option("return to main choice", 6)
        }),

    // Choice 927 is Sunday Black Sunday
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure927",
        "Black Church",
        new Object[] {
          new Option("get 13 turns of Salsa Satanica or beaten up", 1),
          new Option("get black kettle drum", 2, "black kettle drum"),
          new Option("return to main choice", 6)
        }),

    // Choice 928 is The Blackberry Cobbler
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure928",
        "Blackberry Cobbler",
        new Object[] {
          new Option("get blackberry slippers", 1, "blackberry slippers"),
          new Option("get blackberry moccasins", 2, "blackberry moccasins"),
          new Option("get blackberry combat boots", 3, "blackberry combat boots"),
          new Option("get blackberry galoshes", 4, "blackberry galoshes"),
          new Option("return to main choice", 6)
        }),

    // Choice 929 is Control Freak
    new ChoiceAdventure(
        "Pyramid",
        "choiceAdventure929",
        "Control Room",
        new Object[] {
          new Option("turn lower chamber, lose wheel", 1),
          new Option("turn lower chamber, lose ratchet", 2),
          new Option("enter lower chamber", 5),
          new Option("leave", 6)
        }),

    // Choice 930 is Another Errand I Mean Quest
    // Choice 931 is Life Ain't Nothin But Witches and Mummies
    // Choice 932 is No Whammies
    // Choice 935 is Lost in Space... Ship
    // Choice 936 is The Nerve Center
    // Choice 937 is The Spacement
    // Choice 938 is The Ship's Kitchen

    // Choice 940 is Let Your Fists Do The Walking
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure940",
        "white page",
        new Object[] {
          "fight whitesnake",
          "fight white lion",
          "fight white chocolate golem",
          "fight white knight",
          "fight white elephant",
          "skip"
        }),

    // Choice 950 is Time-Twitching Tower Voting / Phone Booth

    // Choice 955 is Time Cave.  Period.
    new ChoiceAdventure(
        "Twitch",
        "choiceAdventure955",
        "Time Cave",
        new Object[] {
          "fight Adventurer echo",
          new Option("twitching time capsule", "twitching time capsule"),
          "talk to caveman"
        }),

    // Choice 973 is Shoe Repair Store
    new ChoiceAdventure(
        "Twitch",
        "choiceAdventure973",
        "Shoe Repair Store",
        new Object[] {
          new Option("visit shop", 1),
          new Option("exchange hooch for Chroners", 2),
          new Option("leave", 6)
        }),

    // Choice 974 is Around The World
    new ChoiceAdventure(
        "Twitch",
        "choiceAdventure974",
        "Bohemian Party",
        new Object[] {"get up to 5 hooch", "leave"}),

    // Choice 975 is Crazy Still After All These Years
    new ChoiceAdventure(
        "Twitch",
        "choiceAdventure975",
        "Moonshriner's Woods",
        new Object[] {"swap 5 cocktail onions for 10 hooch", "leave"}),

    // Choice 979 is The Agora
    new ChoiceAdventure(
        "Twitch",
        "choiceAdventure979",
        "The Agora",
        new Object[] {
          new Option("get blessing", 1), new Option("visit store", 2), new Option("play dice", 6)
        }),

    // Choice 980 is Welcome to Blessings Hut
    new ChoiceAdventure(
        "Twitch",
        "choiceAdventure980",
        "Blessings Hut",
        new Object[] {
          new Option("Bruno's blessing of Mars", "Bruno's blessing of Mars"),
          new Option("Dennis's blessing of Minerva", "Dennis's blessing of Minerva"),
          new Option("Burt's blessing of Bacchus", "Burt's blessing of Bacchus"),
          new Option("Freddie's blessing of Mercury", "Freddie's blessing of Mercury"),
          new Option("return to Agora", 6)
        }),

    // Choice 982 is The 99-Centurion Store
    new ChoiceAdventure(
        "Twitch",
        "choiceAdventure982",
        "The 99-Centurion Store",
        new Object[] {
          new Option("centurion helmet", "centurion helmet"),
          new Option("pteruges", "pteruges"),
          new Option("return to Agora", 6)
        }),

    // Choice 983 is Playing Dice With Romans
    new ChoiceAdventure(
        "Twitch",
        "choiceAdventure983",
        "Playing Dice With Romans",
        new Object[] {
          new Option("make a bet and throw dice", 1), new Option("return to Agora", 6)
        }),

    // Choice 984 is A Radio on a Beach
    // Choice 988 is The Containment Unit
    // Choice 989 is Paranormal Test Lab

    // Choice 993 is Tales of Spelunking

    // Choice 996 is (Untitled) Crimbomega

    // Choice 998 is Game of Cards
    new ChoiceAdventure(
        "Twitch",
        "choiceAdventure998",
        "Game of Cards",
        new Object[] {
          new Option("Gain 7 Chroner"),
          new Option("Gain 9 Chroner"),
          new Option("Gain 13 Chroner (80% chance)"),
          new Option("Gain 17 Chroner (60% chance)"),
          new Option("Gain 21 Chroner, lose pocket ace")
        }),

    // Choice 1000 is Everything in Moderation
    // Choice 1001 is Hot and Cold Dripping Rats

    // Choice 1003 is Test Your Might And Also Test Other Things

    // Choice 1004 is This Maze is... Mazelike...

    // 'Allo
    new ChoiceAdventure(
        "Sorceress",
        "choiceAdventure1005",
        "Hedge Maze 1",
        new Object[] {
          new Option("topiary nugglet and advance to Room 2", "topiary nugglet"),
          new Option("Test #1 and advance to Room 4")
        }),

    // One Small Step For Adventurer
    new ChoiceAdventure(
        "Sorceress",
        "choiceAdventure1006",
        "Hedge Maze 2",
        new Object[] {
          new Option("topiary nugglet and advance to Room 3", "topiary nugglet"),
          new Option("Fight topiary gopher and advance to Room 4")
        }),

    // Twisty Little Passages, All Hedge
    new ChoiceAdventure(
        "Sorceress",
        "choiceAdventure1007",
        "Hedge Maze 3",
        new Object[] {
          new Option("topiary nugglet and advance to Room 4", "topiary nugglet"),
          new Option("Fight topiary chihuahua herd and advance to Room 5")
        }),

    // Pooling Your Resources
    new ChoiceAdventure(
        "Sorceress",
        "choiceAdventure1008",
        "Hedge Maze 4",
        new Object[] {
          new Option("topiary nugglet and advance to Room 5", "topiary nugglet"),
          new Option("Test #2 and advance to Room 7")
        }),

    // Good Ol' 44% Duck
    new ChoiceAdventure(
        "Sorceress",
        "choiceAdventure1009",
        "Hedge Maze 5",
        new Object[] {
          new Option("topiary nugglet and advance to Room 6", "topiary nugglet"),
          new Option("Fight topiary duck and advance to Room 7")
        }),

    // Another Day, Another Fork
    new ChoiceAdventure(
        "Sorceress",
        "choiceAdventure1010",
        "Hedge Maze 6",
        new Object[] {
          new Option("topiary nugglet and advance to Room 7", "topiary nugglet"),
          new Option("Fight topiary kiwi and advance to Room 8")
        }),

    // Of Mouseholes and Manholes
    new ChoiceAdventure(
        "Sorceress",
        "choiceAdventure1011",
        "Hedge Maze 7",
        new Object[] {
          new Option("topiary nugglet and advance to Room 8", "topiary nugglet"),
          new Option("Test #3 and advance to Room 9")
        }),

    // The Last Temptation
    new ChoiceAdventure(
        "Sorceress",
        "choiceAdventure1012",
        "Hedge Maze 8",
        new Object[] {
          new Option("topiary nugglet and advance to Room 9", "topiary nugglet"),
          new Option("Lose HP for no benefit and advance to Room 9")
        }),

    // Choice 1013 is Mazel Tov!

    // The Mirror in the Tower has the View that is True
    new ChoiceAdventure(
        "Sorceress",
        "choiceAdventure1015",
        "Tower Mirror",
        new Object[] {
          new Option("Gain Confidence! intrinsic until leave tower (1)"),
          new Option("Make Sorceress tougher (0 turns)")
        }),

    // Choice 1016 is Frank Gets Earnest
    // Choice 1017 is Bear Verb Orgy

    // Bee Persistent
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure1018",
        "Bees 1",
        new Object[] {"head towards beehive (1)", "give up"}),

    // Bee Rewarded
    new ChoiceAdventure(
        "Woods",
        "choiceAdventure1019",
        "Bees 2",
        new Object[] {new Option("beehive (1)", "beehive"), "give up"}),

    // Choice 1020 is Closing Ceremony
    // Choice 1021 is Meet Frank
    // Choice 1022 is Meet Frank
    // Choice 1023 is Like a Bat Into Hell
    // Choice 1024 is Like a Bat out of Hell

    // Home on the Free Range
    new ChoiceAdventure(
        "Beanstalk",
        "choiceAdventure1026",
        "Ground Floor Foodie",
        new Object[] {
          "4 pieces of candy",
          new Option("electric boning knife, then skip adventure", "electric boning knife"),
          "skip adventure"
        }),

    // Choice 1027 is The End of the Tale of Spelunking

    // Choice 1028 is A Shop
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1028",
        "A Shop",
        new Object[] {new Option("chance to fight shopkeeper", 5), new Option("leave", 6)}),

    // Choice 1029 is An Old Clay Pot
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1029",
        "An Old Clay Pot",
        new Object[] {new Option("gain 18-20 gold", 1), new Option("gain pot", 5, "pot")}),

    // Choice 1030 is It's a Trap!  A Dart Trap.
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1030",
        "It's a Trap!  A Dart Trap.",
        new Object[] {
          new Option("escape with whip", 1),
          new Option("unlock The Snake Pit using bomb", 2),
          new Option("unlock The Spider Hole using rope", 3),
          new Option("escape using offhand item", 4),
          new Option("take damage", 6)
        }),

    // Choice 1031 is A Tombstone
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1031",
        "A Tombstone",
        new Object[] {
          new Option("gain 20-25 gold or buddy", 1),
          new Option("gain shotgun with pickaxe", 2, "shotgun"),
          new Option("gain Clown Crown with x-ray specs", 3, "The Clown Crown")
        }),

    // Choice 1032 is It's a Trap!  A Tiki Trap.
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1032",
        "It's a Trap!  A Tiki Trap.",
        new Object[] {
          new Option("escape with spring boots", 1),
          new Option("unlock The Beehive using bomb, take damage without sticky bomb", 2),
          new Option(
              "unlock The Ancient Burial Ground using rope, take damage without back item", 3),
          new Option("lose 30 hp", 6)
        }),

    // Choice 1033 is A Big Block of Ice
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1033",
        "A Big Block of Ice",
        new Object[] {
          new Option("gain 50-60 gold and restore health (with cursed coffee cup)", 1),
          new Option("gain buddy (or 60-70 gold) with torch", 2)
        }),

    // Choice 1034 is A Landmine
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1034",
        "A Landmine",
        new Object[] {
          new Option("unlock An Ancient Altar and lose 10 HP", 2),
          new Option("unlock The Crashed UFO using 3 ropes", 3),
          new Option("lose 30 hp", 6)
        }),

    // Choice 1035 is A Crate

    // Choice 1036 is Idolatry
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1036",
        "Idolatry",
        new Object[] {
          new Option("gain 250 gold with Resourceful Kid", 1),
          new Option("gain 250 gold with spring boots and yellow cloak", 2),
          new Option("gain 250 gold with jetpack", 3),
          new Option("gain 250 gold and lose 50 hp", 4),
          new Option("leave", 6)
        }),

    // Choice 1037 is It's a Trap!  A Smashy Trap.
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1037",
        "It's a Trap!  A Smashy Trap.",
        new Object[] {
          new Option("unlock The City of Goooold with key, or take damage", 2),
          new Option("lose 40 hp", 6)
        }),

    // Choice 1038 is A Wicked Web
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1038",
        "A Wicked Web",
        new Object[] {
          new Option("gain 15-20 gold", 1),
          new Option("gain buddy (or 20-30 gold) with machete", 2),
          new Option("gain 30-50 gold with torch", 3)
        }),

    // Choice 1039 is A Golden Chest
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1039",
        "A Golden Chest",
        new Object[] {
          new Option("gain 150 gold with key", 1),
          new Option("gain 80-100 gold with bomb", 2),
          new Option("gain 50-60 gold and lose 20 hp", 3)
        }),

    // Choice 1040 is It's Lump. It's Lump.
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1040",
        "It's Lump. It's Lump",
        new Object[] {
          new Option("gain heavy pickaxe with bomb", 1, "heavy pickaxe"), new Option("leave", 6)
        }),

    // choice 1041 is Spelunkrifice
    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1041",
        "Spelunkrifice",
        new Object[] {new Option("sacrifice buddy", 1), new Option("leave", 6)}),

    // choice 1042 is Pick a Perk!
    // choice 1044 is The Gates of Hell

    new ChoiceAdventure(
        "Spelunky Area",
        "choiceAdventure1045",
        "Hostile Work Environment",
        new Object[] {new Option("fight shopkeeper", 1), new Option("take damage", 6)}),

    // Choice 1046 is Actually Ed the Undying
    // Choice 1048 is Twitch Event #8 Time Period
    // Choice 1049 is Tomb of the Unknown Your Class Here
    // Choice 1051 is The Book of the Undying
    // Choice 1052 is Underworld Body Shop
    // Choice 1053 is The Servants' Quarters
    // Choice 1054 is Returning the MacGuffin
    // Choice 1055 is Returning the MacGuffin
    // Choice 1056 is Now It's Dark
    // Choice 1057 is A Stone Shrine
    // Choice 1059 is Helping Make Ends Meat

    // Choice 1060 is Temporarily Out of Skeletons
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1060",
        "Skeleton Store",
        new Object[] {
          new Option("gain office key, then ~35 meat", 1, "Skeleton Store office key"),
          new Option(
              "gain ring of telling skeletons what to do, then 300 meat, with skeleton key",
              2,
              "ring of telling skeletons what to do"),
          new Option("gain muscle stats", 3),
          new Option("fight former owner of the Skeleton Store, with office key", 4)
        }),

    // Choice 1061 is Heart of Madness
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1061",
        "Madness Bakery",
        new Object[] {
          new Option("try to enter office", 1),
          new Option("bagel machine", 2),
          new Option("popular machine", 3),
          new Option("learn recipe", 4),
          new Option("gain mysticality stats", 5)
        }),

    // Choice 1062 is Lots of Options
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1062",
        "Overgrown Lot",
        new Object[] {
          new Option("acquire flowers", 1),
          new Option("acquire food", 2),
          new Option("acquire drinks", 3),
          new Option("gain moxie stats", 4),
          new Option("acquire more booze with map", 5)
        }),

    // Choice 1063 is Adjust your 'Edpiece
    new ChoiceSpoiler(
        "choiceAdventure1063",
        "Crown of Ed the Undying",
        new Object[] {
          "Muscle +20, +2 Muscle Stats Per Fight",
          "Mysticality +20, +2 Mysticality Stats Per Fight",
          "Moxie +20, +2 Moxie Stats Per Fight",
          "+20 to Monster Level",
          "+10% Item Drops from Monsters, +20% Meat from Monsters",
          "The first attack against you will always miss, Regenerate 10-20 HP per Adventure",
          "Lets you breathe underwater"
        }),

    // Choice 1065 is Lending a Hand (and a Foot)
    // Choice 1067 is Maint Misbehavin'
    // Choice 1068 is Barf Mountain Breakdown
    // Choice 1069 is The Pirate Bay
    // Choice 1070 is In Your Cups
    // Choice 1071 is Gator Gamer
    // Choice 1073 is This Ride Is Like... A Rollercoaster Baby Baby
    new ChoiceAdventure(
        "Dinseylandfill",
        "choiceAdventure1073",
        "This Ride Is Like... A Rollercoaster Baby Baby",
        new Object[] {
          new Option("gain stats and meat", 1),
          new Option("skip adventure and guarantees this adventure will reoccur", 6)
        }),

    // Choice 1076 is Mayo Minder&trade;

    // Choice 1080 is Bagelmat-5000
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1080",
        "Bagelmat-5000",
        new Object[] {
          new Option("make 3 plain bagels using wad of dough", 1),
          new Option("return to Madness Bakery", 2)
        }),

    // Choice 1081 is Assault and Baguettery
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure1081",
        "magical baguette",
        new Object[] {
          new Option("breadwand", 1, "breadwand"),
          new Option("loafers", 2, "loafers"),
          new Option("bread basket", 3, "bread basket"),
          new Option("make nothing", 4)
        }),

    // Choice 1084 is The Popular Machine
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1084",
        "Popular Machine",
        new Object[] {
          new Option("make popular tart", 1), new Option("return to Madness Bakery", 2)
        }),

    // Choice 1090 is The Towering Inferno Discotheque

    // Choice 1091 is The Floor Is Yours
    new ChoiceAdventure(
        "That 70s Volcano",
        "choiceAdventure1091",
        "LavaCo Lamp Factory",
        new Object[] {
          new Option("1,970 carat gold -> thin gold wire", 1, "thin gold wire"),
          new Option("New Age healing crystal -> empty lava bottle", 2, "empty lava bottle"),
          new Option("empty lava bottle -> full lava bottle", 3, "full lava bottle"),
          new Option("make colored lava globs", 4),
          new Option(
              "glowing New Age crystal -> crystalline light bulb", 5, "crystalline light bulb"),
          new Option(
              "crystalline light bulb + insulated wire + heat-resistant sheet metal -> LavaCo&trade; Lamp housing",
              6,
              "LavaCo&trade; Lamp housing"),
          new Option("fused fuse", 7, "fused fuse"),
          new Option("leave", 9)
        }),

    // Choice 1092 is Dyer Maker
    // Choice 1093 is The WLF Bunker

    // Choice 1094 is Back Room SMOOCHing
    new ChoiceAdventure(
        "That 70s Volcano",
        "choiceAdventure1094",
        "The SMOOCH Army HQ",
        new Option[] {
          new Option("fight Geve Smimmons", 1),
          new Option("fight Raul Stamley", 2),
          new Option("fight Pener Crisp", 3),
          new Option("fight Deuce Freshly", 4),
          new Option("acquire SMOOCH coffee cup", 5, "SMOOCH coffee cup")
        }),

    // Choice 1095 is Tin Roof -- Melted
    new ChoiceAdventure(
        "That 70s Volcano",
        "choiceAdventure1095",
        "The Velvet / Gold Mine",
        new Object[] {
          new Option("fight Mr. Choch", 1),
          new Option("acquire half-melted hula girl", 2, "half-melted hula girl")
        }),

    // Choice 1096 is Re-Factory Period
    new ChoiceAdventure(
        "That 70s Volcano",
        "choiceAdventure1096",
        "LavaCo Lamp Factory",
        new Object[] {
          new Option("fight Mr. Cheeng", 1),
          new Option("acquire glass ceiling fragments", 2, "glass ceiling fragments")
        }),

    // Choice 1097 is Who You Gonna Caldera?
    new ChoiceAdventure(
        "That 70s Volcano",
        "choiceAdventure1097",
        "The Bubblin' Caldera",
        new Object[] {
          new Option("acquire The One Mood Ring", 1, "The One Mood Ring"),
          new Option("fight Lavalos", 2)
        }),

    // Choice 1102 is The Biggest Barrel

    // Choice 1106 is Wooof! Wooooooof!
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure1106",
        "Haunted Doghouse 1",
        new Object[] {
          new Option("gain stats", 1),
          new Option("+50% all stats for 30 turns", 2),
          new Option("acquire familiar food", 3, "Ghost Dog Chow")
        }),

    // Choice 1107 is Playing Fetch*
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure1107",
        "Haunted Doghouse 2",
        new Object[] {
          new Option("acquire tennis ball", 1, "tennis ball"),
          new Option("+50% init for 30 turns", 2),
          new Option("acquire ~500 meat", 3)
        }),

    // Choice 1108 is Your Dog Found Something Again
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure1108",
        "Haunted Doghouse 3",
        new Object[] {
          new Option("acquire food", 1),
          new Option("acquire booze", 2),
          new Option("acquire cursed thing", 3)
        }),

    // Choice 1110 is Spoopy
    // Choice 1114 is Walford Rusley, Bucket Collector

    // Choice 1115 is VYKEA!
    new ChoiceAdventure(
        "The Glaciest",
        "choiceAdventure1115",
        "VYKEA!",
        new Object[] {
          new Option("acquire VYKEA meatballs and mead (1/day)", 1),
          new Option("acquire VYKEA hex key", 2, "VYKEA hex key"),
          new Option("fill bucket by 10-15%", 3),
          new Option(
              "acquire 3 Wal-Mart gift certificates (1/day)", 4, "Wal-Mart gift certificate"),
          new Option("acquire VYKEA rune", 5),
          new Option("leave", 6)
        }),

    // Choice 1116 is All They Got Inside is Vacancy (and Ice)
    new ChoiceAdventure(
        "The Glaciest",
        "choiceAdventure1116",
        "All They Got Inside is Vacancy (and Ice)",
        new Object[] {
          new Option("fill bucket by 10-15%", 3),
          new Option("acquire cocktail ingredients", 4),
          new Option(
              "acquire 3 Wal-Mart gift certificates (1/day)", 5, "Wal-Mart gift certificate"),
          new Option("leave", 6)
        }),

    // Choice 1118 is X-32-F Combat Training Snowman Control Console
    new ChoiceAdventure(
        "The Snojo",
        "choiceAdventure1118",
        "Control Console",
        new Object[] {
          new Option("muscle training", 1),
          new Option("mysticality training", 2),
          new Option("moxie training", 3),
          new Option("tournament", 4),
          new Option("leave", 6)
        }),

    // Choice 1119 is Shining Mauve Backwards In Time
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1119",
        "Deep Machine Tunnels",
        new Object[] {
          new Option("acquire some abstractions", 1),
          new Option("acquire abstraction: comprehension", 2, "abstraction: comprehension"),
          new Option("acquire modern picture frame", 3, "modern picture frame"),
          new Option("duplicate one food, booze, spleen or potion", 4),
          new Option("leave", 6)
        }),

    // Choice 1120 is Some Assembly Required
    // Choice 1121 is Some Assembly Required
    // Choice 1122 is Some Assembly Required
    // Choice 1123 is Some Assembly Required
    // Choice 1127 is The Crimbo Elf Commune
    // Choice 1128 is Reindeer Commune
    // Choice 1129 is The Crimbulmination
    // Choice 1130 is The Crimbulmination
    // Choice 1131 is The Crimbulmination
    // Choice 1132 is The Crimbulmination

    // Choice 1188 is The Call is Coming from Outside the Simulation
    // Choice 1190 is The Oracle

    // Choice 1195 is Spinning Your Time-Spinner
    // Choice 1196 is Travel to a Recent Fight
    // Choice 1197 is Travel back to a Delicious Meal
    // Choice 1198 is Play a Time Prank
    // Choice 1199 is The Far Future

    // Choice 1202 is Noon in the Civic Center
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1202",
        "Noon in the Civic Center",
        new Object[] {
          new Option("fancy marzipan briefcase", 1, "fancy marzipan briefcase"),
          new Option("acquire 50 sprinkles and unlock judge fudge", 2, "sprinkles"),
          new Option("enter Civic Planning Office (costs 1000 sprinkles)", 3),
          new Option("acquire briefcase full of sprinkles (with gingerbread blackmail photos)", 4)
        }),

    // Choice 1203 is Midnight in Civic Center
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1203",
        "Midnight in the Civic Center",
        new Object[] {
          new Option("gain 500 mysticality", 1),
          new Option("acquire counterfeit city (costs 300 sprinkles)", 2, "counterfeit city"),
          new Option(
              "acquire gingerbread moneybag (with creme brulee torch)", 3, "gingerbread moneybag"),
          new Option(
              "acquire 5 gingerbread cigarettes (costs 5 sprinkles)", 4, "gingerbread cigarette"),
          new Option("acquire chocolate puppy (with gingerbread dog treat)", 5, "chocolate puppy")
        }),

    // Choice 1204 is Noon at the Train Station
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1204",
        "Noon at the Train Station",
        new Object[] {
          new Option("gain 8-11 candies", 1),
          new Option("increase size of sewer gators (with sewer unlocked)", 2),
          new Option("gain 250 mysticality", 3)
        }),

    // Choice 1205 is Midnight at the Train Station
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1205",
        "Midnight at the Train Station",
        new Object[] {
          new Option("gain 500 muscle and add track", 1),
          new Option(
              "acquire broken chocolate pocketwatch (with pumpkin spice candle)",
              2,
              "broken chocolate pocketwatch"),
          new Option("enter The Currency Exchange (with candy crowbar)", 3),
          new Option(
              "acquire fruit-leather negatives (with track added)", 4, "fruit-leather negatives"),
          new Option("acquire various items (with teethpick)", 5)
        }),

    // Choice 1206 is Noon in the Industrial Zone
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1206",
        "Noon in the Industrial Zone",
        new Object[] {
          new Option("acquire creme brulee torch (costs 25 sprinkles)", 1, "creme brulee torch"),
          new Option("acquire candy crowbar (costs 50 sprinkles)", 2, "candy crowbar"),
          new Option("acquire candy screwdriver (costs 100 sprinkles)", 3, "candy screwdriver"),
          new Option("acquire teethpick (costs 1000 sprinkles after studying law)", 4, "teethpick"),
          new Option("acquire 400-600 sprinkles (with gingerbread mask, pistol and moneybag)", 5)
        }),

    // Choice 1207 is Midnight in the Industrial Zone
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1207",
        "Midnight in the Industrial Zone",
        new Object[] {
          new Option("enter Seedy Seedy Seedy", 1),
          new Option("enter The Factory Factor", 2),
          new Option("acquire tattoo (costs 100000 sprinkles)", 3)
        }),

    // Choice 1208 is Upscale Noon
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1208",
        "Upscale Noon",
        new Object[] {
          new Option(
              "acquire gingerbread dog treat (costs 200 sprinkles)", 1, "gingerbread dog treat"),
          new Option(
              "acquire pumpkin spice candle (costs 150 sprinkles)", 2, "pumpkin spice candle"),
          new Option(
              "acquire gingerbread spice latte (costs 50 sprinkles)", 3, "gingerbread spice latte"),
          new Option(
              "acquire gingerbread trousers (costs 500 sprinkles)", 4, "gingerbread trousers"),
          new Option(
              "acquire gingerbread waistcoat (costs 500 sprinkles)", 5, "gingerbread waistcoat"),
          new Option("acquire gingerbread tophat (costs 500 sprinkles)", 6, "gingerbread tophat"),
          new Option("acquire 400-600 sprinkles (with gingerbread mask, pistol and moneybag)", 7),
          new Option(
              "acquire gingerbread blackmail photos (drop off fruit-leather negatives and pick up next visit)",
              8,
              "gingerbread blackmail photos"),
          new Option("leave", 9)
        }),

    // Choice 1209 is Upscale Midnight
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1209",
        "Upscale Midnight",
        new Object[] {
          new Option("acquire fake cocktail", 1, "fake cocktail"),
          new Option("enter The Gingerbread Gallery (wearing Gingerbread Best", 2)
        }),

    // Choice 1210 is Civic Planning Office
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1210",
        "Civic Planning Office",
        new Object[] {
          new Option("unlock Gingerbread Upscale Retail District", 1),
          new Option("unlock Gingerbread Sewers", 2),
          new Option("unlock 10 extra City adventures", 3),
          new Option("unlock City Clock", 4)
        }),

    // Choice 1211 is The Currency Exchange
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1211",
        "The Currency Exchange",
        new Object[] {
          new Option("acquire 5000 meat", 1),
          new Option("acquire fat loot token", 2, "fat loot token"),
          new Option("acquire 250 sprinkles", 3, "sprinkles"),
          new Option("acquire priceless diamond", 4, "priceless diamond"),
          new Option("acquire 5 pristine fish scales)", 5, "pristine fish scales")
        }),

    // Choice 1212 is Seedy Seedy Seedy
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1212",
        "Seedy Seedy Seedy",
        new Object[] {
          new Option("acquire gingerbread pistol (costs 300 sprinkles)", 1, "gingerbread pistol"),
          new Option("gain 500 moxie", 2),
          new Option("ginger beer (with gingerbread mug)", 3, "ginger beer")
        }),

    // Choice 1213 is The Factory Factor
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1213",
        "The Factory Factor",
        new Object[] {
          new Option("acquire spare chocolate parts", 1, "spare chocolate parts"),
          new Option("fight GNG-3-R (with gingerservo", 2)
        }),

    // Choice 1214 is The Gingerbread Gallery
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1214",
        "The Gingerbread Gallery",
        new Object[] {
          new Option("acquire high-end ginger wine", 1, "high-end ginger wine"),
          new Option(
              "acquire fancy chocolate sculpture (costs 300 sprinkles)",
              2,
              "fancy chocolate sculpture"),
          new Option("acquire Pop Art: a Guide (costs 1000 sprinkles)", 3, "Pop Art: a Guide"),
          new Option("acquire No Hats as Art (costs 1000 sprinkles)", 4, "No Hats as Art")
        }),

    // Choice 1215 is Setting the Clock
    new ChoiceAdventure(
        "Gingerbread City",
        "choiceAdventure1215",
        "Setting the Clock",
        new Object[] {new Option("move clock forward", 1), new Option("leave", 2)}),

    // Choice 1217 is Sweet Synthesis
    // Choice 1218 is Wax On

    // Choice 1222 is The Tunnel of L.O.V.E.

    // Choice 1223 is L.O.V. Entrance
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1223",
        "L.O.V.E Fight 1",
        new Object[] {new Option("(free) fight LOV Enforcer", 1), new Option("avoid fight", 2)}),

    // Choice 1224 is L.O.V. Equipment Room
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1224",
        "L.O.V.E Choice 1",
        new Object[] {
          new Option("acquire LOV Eardigan", 1, "LOV Eardigan"),
          new Option("acquire LOV Epaulettes", 2, "LOV Epaulettes"),
          new Option("acquire LOV Earrings", 3, "LOV Earrings"),
          new Option("take nothing", 4)
        }),

    // Choice 1225 is L.O.V. Engine Room
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1225",
        "L.O.V.E Fight 2",
        new Object[] {new Option("(free) fight LOV Engineer", 1), new Option("avoid fight", 2)}),

    // Choice 1226 is L.O.V. Emergency Room
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1226",
        "L.O.V.E Choice 2",
        new Object[] {
          new Option("50 adv of Lovebotamy (+10 stats/fight)", 1),
          new Option("50 adv of Open Heart Surgery (+10 fam weight)", 2),
          new Option("50 adv of Wandering Eye Surgery (+50 item drop)", 3),
          new Option("get no buff", 4)
        }),

    // Choice 1227 is L.O.V. Elbow Room
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1227",
        "L.O.V.E Fight 3",
        new Object[] {new Option("(free) fight LOV Equivocator", 1), new Option("avoid fight", 2)}),

    // Choice 1228 is L.O.V. Emporium
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1228",
        "L.O.V.E Choice 3",
        new Object[] {
          new Option("acquire LOV Enamorang", 1, "LOV Enamorang"),
          new Option("acquire LOV Emotionizer", 2, "LOV Emotionizer"),
          new Option("acquire LOV Extraterrestrial Chocolate", 3, "LOV Extraterrestrial Chocolate"),
          new Option("acquire LOV Echinacea Bouquet", 4, "LOV Echinacea Bouquet"),
          new Option("acquire LOV Elephant", 5, "LOV Elephant"),
          new Option("acquire 2 pieces of toast (if have Space Jellyfish)", 6, "toast"),
          new Option("take nothing", 7)
        }),

    // Choice 1229 is L.O.V. Exit

    // Choice 1236 is Space Cave
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1236",
        "Space Cave",
        new Object[] {
          new Option("acquire some alien rock samples", 1, "alien rock sample"),
          new Option(
              "acquire some more alien rock samples (with geology kit)", 2, "alien rock sample"),
          new Option("skip adventure", 6)
        }),

    // Choice 1237 is A Simple Plant
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1237",
        "A Simple Plant",
        new Object[] {
          new Option("acquire edible alien plant bit", 1, "edible alien plant bit"),
          new Option("acquire alien plant fibers", 2, "alien plant fibers"),
          new Option("acquire alien plant sample (with botany kit)", 3, "alien plant sample"),
          new Option("skip adventure", 6)
        }),

    // Choice 1238 is A Complicated Plant
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1238",
        "A Complicated Plant",
        new Object[] {
          new Option("acquire some edible alien plant bit", 1, "edible alien plant bit"),
          new Option("acquire some alien plant fibers", 2, "alien plant fibers"),
          new Option(
              "acquire complex alien plant sample (with botany kit)",
              3,
              "complex alien plant sample"),
          new Option("skip adventure", 6)
        }),

    // Choice 1239 is What a Plant!
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1239",
        "What a Plant!",
        new Object[] {
          new Option("acquire some edible alien plant bit", 1, "edible alien plant bit"),
          new Option("acquire some alien plant fibers", 2, "alien plant fibers"),
          new Option(
              "acquire fascinating alien plant sample (with botany kit)",
              3,
              "fascinating alien plant sample"),
          new Option("skip adventure", 6)
        }),

    // Choice 1240 is The Animals, The Animals
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1240",
        "The Animals, The Animals",
        new Object[] {
          new Option("acquire alien meat", 1, "alien meat"),
          new Option("acquire alien toenails", 2, "alien toenails"),
          new Option(
              "acquire alien zoological sample (with zoology kit)", 3, "alien zoological sample"),
          new Option("skip adventure", 6)
        }),

    // Choice 1241 is Buffalo-Like Animal, Won't You Come Out Tonight
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1241",
        "Buffalo-Like Animal, Won't You Come Out Tonight",
        new Object[] {
          new Option("acquire some alien meat", 1, "alien meat"),
          new Option("acquire some alien toenails", 2, "alien toenails"),
          new Option(
              "acquire complex alien zoological sample (with zoology kit)",
              3,
              "complex alien zoological sample"),
          new Option("skip adventure", 6)
        }),

    // Choice 1242 is House-Sized Animal
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1242",
        "House-Sized Animal",
        new Object[] {
          new Option("acquire some alien meat", 1, "alien meat"),
          new Option("acquire some alien toenails", 2, "alien toenails"),
          new Option(
              "acquire fascinating alien zoological sample (with zoology kit)",
              3,
              "fascinating alien zoological sample"),
          new Option("skip adventure", 6)
        }),

    // Choice 1243 is Interstellar Trade
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1243",
        "Interstellar Trade",
        new Object[] {new Option("purchase item", 1), new Option("leave", 6)}),

    // Choice 1244 is Here There Be No Spants
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1244",
        "Here There Be No Spants",
        new Object[] {new Option("acquire spant egg casing", 1, "spant egg casing")}),

    // Choice 1245 is Recovering the Satellites
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1245",
        "Recovering the Satellite",
        new Object[] {new Option("acquire murderbot data core", 1, "murderbot data core")}),

    // Choice 1246 is Land Ho
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1246",
        "Land Ho",
        new Object[] {new Option("gain 10% Space Pirate language", 1), new Option("leave", 6)}),

    // Choice 1247 is Half The Ship it Used to Be
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1247",
        "Half The Ship it Used to Be",
        new Object[] {
          new Option(
              "acquire space pirate treasure map (with enough Space Pirate language)",
              1,
              "space pirate treasure map"),
          new Option("leave", 6)
        }),

    // Choice 1248 is Paradise Under a Strange Sun
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1248",
        "Paradise Under a Strange Sun",
        new Object[] {
          new Option(
              "acquire Space Pirate Astrogation Handbook (with space pirate treasure map)",
              1,
              "Space Pirate Astrogation Handbook"),
          new Option("gain 1000 moxie stats", 2),
          new Option("leave", 6)
        }),

    // Choice 1249 is That's No Moonlith, it's a Monolith!
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1249",
        "That's No Moonlith, it's a Monolith!",
        new Object[] {
          new Option("gain 20% procrastinator language (with murderbot data core)", 1),
          new Option("leave", 6)
        }),

    // Choice 1250 is I'm Afraid It's Terminal
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1250",
        "I'm Afraid It's Terminal",
        new Object[] {
          new Option(
              "acquire procrastinator locker key (with enough procrastinator language)",
              1,
              "Procrastinator locker key"),
          new Option("leave", 6)
        }),

    // Choice 1251 is Curses, a Hex
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1251",
        "Curses, a Hex",
        new Object[] {
          new Option(
              "acquire Non-Euclidean Finance (with procrastinator locker key)",
              1,
              "Non-Euclidean Finance"),
          new Option("leave", 6)
        }),

    // Choice 1252 is Time Enough at Last
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1252",
        "Time Enough at Last",
        new Object[] {
          new Option("acquire Space Baby childrens' book", 1, "Space Baby childrens' book"),
          new Option("leave", 6)
        }),

    // Choice 1253 is Mother May I
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1253",
        "Mother May I",
        new Object[] {
          new Option(
              "acquire Space Baby bawbaw (with enough Space Baby language)",
              1,
              "Space Baby bawbaw"),
          new Option("leave", 6)
        }),

    // Choice 1254 is Please Baby Baby Please
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1254",
        "Please Baby Baby Please",
        new Object[] {
          new Option("acquire Peek-a-Boo! (with Space Baby bawbaw)", 1, "Peek-a-Boo!"),
          new Option("leave", 6)
        }),

    // Choice 1255 is Cool Space Rocks
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1255",
        "Cool Space Rocks",
        new Object[] {
          new Option("acquire some alien rock samples", 1, "alien rock sample"),
          new Option(
              "acquire some more alien rock samples (with geology kit)", 2, "alien rock sample")
        }),

    // Choice 1256 is Wide Open Spaces
    new ChoiceAdventure(
        "The Spacegate",
        "choiceAdventure1256",
        "Wide Open Spaces",
        new Object[] {
          new Option("acquire some alien rock samples", 1, "alien rock sample"),
          new Option(
              "acquire some more alien rock samples (with geology kit)", 2, "alien rock sample")
        }),

    // Choice 1280 is Welcome to FantasyRealm
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1280",
        "Welcome to FantasyRealm",
        new Object[] {
          new Option("acquire FantasyRealm Warrior's Helm", 1, "FantasyRealm Warrior's Helm"),
          new Option("acquire FantasyRealm Mage's Hat", 2, "FantasyRealm Mage's Hat"),
          new Option("acquire FantasyRealm Rogue's Mask", 3, "FantasyRealm Rogue's Mask"),
          new Option("leave", 6)
        }),

    // Choice 1281 is You'll See You at the Crossroads
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1281",
        "You'll See You at the Crossroads",
        new Object[] {
          new Option("unlock The Towering Mountains", 1),
          new Option("unlock The Mystic Wood", 2),
          new Option("unlock The Putrid Swamp", 3),
          new Option("unlock Cursed Village", 4),
          new Option("unlock The Sprawling Cemetery", 5),
          new Option("leave", 8)
        }),

    // Choice 1282 is Out of Range
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1282",
        "Out of Range",
        new Object[] {
          new Option("unlock The Old Rubee Mine (using FantasyRealm key)", 1),
          new Option("unlock The Foreboding Cave", 2),
          new Option("unlock The Master Thief's Chalet (with FantasyRealm Rogue's Mask)", 3),
          new Option("charge druidic orb (need orb)", 4, "charged druidic orb"),
          new Option("unlock The Ogre Chieftain's Keep (with FantasyRealm Warrior's Helm)", 5),
          new Option("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
          new Option("leave", 11)
        }),

    // Choice 1283 is Where Wood You Like to Go
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1283",
        "Where Wood You Like to Go",
        new Object[] {
          new Option("unlock The Faerie Cyrkle", 1),
          new Option("unlock The Druidic Campsite (with LyleCo premium rope)", 2),
          new Option("unlock The Ley Nexus (with Cheswick Copperbottom's compass)", 3),
          new Option("acquire plump purple mushroom", 5, "plump purple mushroom"),
          new Option("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
          new Option("leave", 11)
        }),

    // Choice 1284 is Swamped with Leisure
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1284",
        "Swamped with Leisure",
        new Object[] {
          new Option("unlock Near the Witch's House", 1),
          new Option("unlock The Troll Fortress (using FantasyRealm key)", 2),
          new Option("unlock The Dragon's Moor (with FantasyRealm Warrior's Helm)", 3),
          new Option("acquire tainted marshmallow", 5, "tainted marshmallow"),
          new Option("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
          new Option("leave", 11)
        }),

    // Choice 1285 is It Takes a Cursed Village
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1285",
        "It Takes a Cursed Village",
        new Object[] {
          new Option("unlock The Evil Cathedral", 1),
          new Option(
              "unlock The Cursed Village Thieves' Guild (using FantasyRealm Rogue's Mask)", 2),
          new Option("unlock The Archwizard's Tower (with FantasyRealm Mage's Hat)", 3),
          new Option("get 20 adv of +2-3 Rubee&trade; drop", 4),
          new Option("acquire 40-60 Rubees&trade; (with LyleCo premium rope)", 5, "Rubee&trade;"),
          new Option(
              "acquire dragon slaying sword (with dragon aluminum ore)", 6, "dragon slaying sword"),
          new Option(
              "acquire notarized arrest warrant (with arrest warrant)",
              7,
              "notarized arrest warrant"),
          new Option("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
          new Option("leave", 11)
        }),

    // Choice 1286 is Resting in Peace
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1286",
        "Resting in Peace",
        new Object[] {
          new Option("unlock The Labyrinthine Crypt", 1),
          new Option("unlock The Barrow Mounds", 2),
          new Option("unlock Duke Vampire's Chateau (with FantasyRealm Rogue's Mask)", 3),
          new Option(
              "acquire 40-60 Rubees&trade; (need LyleCo premium pickaxe)", 4, "Rubee&trade;"),
          new Option(
              "acquire Chewsick Copperbottom's notes (with FantasyRealm Mage's Hat)",
              5,
              "Chewsick Copperbottom's notes"),
          new Option("1/5 to fight Skeleton Lord (with FantasyRealm outfit)", 10),
          new Option("leave", 11)
        }),

    // Choice 1288 is What's Yours is Yours
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1288",
        "What's Yours is Yours",
        new Object[] {
          new Option("acquire 20-30 Rubees&trade;", 1, "Rubee&trade;"),
          new Option(
              "acquire dragon aluminum ore (need LyleCo premium pickaxe)",
              2,
              "dragon aluminum ore"),
          new Option("acquire grolblin rum", 3, "grolblin rum"),
          new Option("leave", 6)
        }),

    // Choice 1289 is A Warm Place
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1289",
        "A Warm Place",
        new Object[] {
          new Option("acquire 90-110 Rubees&trade; (with FantasyRealm key)", 1, "Rubee&trade;"),
          new Option("acquire sachet of strange powder", 2, "sachet of strange powder"),
          new Option("unlock The Lair of the Phoenix (with FantasyRealm Mage's Hat)", 3),
          new Option("leave", 6)
        }),

    // Choice 1290 is The Cyrkle Is Compleat
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1290",
        "The Cyrkle Is Compleat",
        new Object[] {
          new Option("get 100 adv of Fantasy Faerie Blessing", 1),
          new Option("acquire faerie dust", 2, "faerie dust"),
          new Option("unlock The Spider Queen's Lair (with FantasyRealm Rogue's Mask)", 3),
          new Option("leave", 6)
        }),

    // Choice 1291 is Dudes, Where's My Druids?
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1291",
        "Dudes, Where's My Druids?",
        new Object[] {
          new Option("acquire druidic s'more", 1, "druidic s'more"),
          new Option(
              "acquire poisoned druidic s'more (with tainted marshmallow)",
              2,
              "poisoned druidic s'more"),
          new Option("acquire druidic orb (with FantasyRealm Mage's Hat)", 3, "druidic orb"),
          new Option("leave", 6)
        }),

    // Choice 1292 is Witch One You Want?
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1292",
        "Witch One You Want?",
        new Object[] {
          new Option("get 50 adv of +200% init", 1),
          new Option("get 10 adv of Poison for Blood (with plump purple mushroom)", 2),
          new Option("acquire to-go brew", 3, "to-go brew"),
          new Option("acquire 40-60 Rubees&trade;", 4, "Rubee&trade;"),
          new Option("leave", 6)
        }),

    // Choice 1293 is Altared States
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1293",
        "Altared States",
        new Object[] {
          new Option("acquire 20-30 Rubees&trade;", 1, "Rubee&trade;"),
          new Option("get 100 adv of +200% HP", 2),
          new Option("acquire sanctified cola", 3, "sanctified cola"),
          new Option(
              "acquire flask of holy water (with FantasyRealm Mage's Hat)",
              4,
              "flask of holy water"),
          new Option("leave", 6)
        }),

    // Choice 1294 is Neither a Barrower Nor a Lender Be
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1294",
        "Neither a Barrower Nor a Lender Be",
        new Object[] {
          new Option("acquire 20-30 Rubees&trade;", 1, "Rubee&trade;"),
          new Option("acquire mourning wine", 2, "mourning wine"),
          new Option("unlock The Ghoul King's Catacomb (with FantasyRealm Warrior's Helm)", 3),
          new Option("leave", 6)
        }),

    // Choice 1295 is Honor Among You
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1295",
        "Honor Among You",
        new Object[] {
          new Option("acquire 40-60 Rubees&trade;", 1, "Rubee&trade;"),
          new Option("acquire universal antivenin", 2, "universal antivenin"),
          new Option("leave", 6)
        }),

    // Choice 1296 is For Whom the Bell Trolls
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1296",
        "For Whom the Bell Trolls",
        new Object[] {
          new Option("nothing happens", 1),
          new Option("acquire nasty haunch", 2, "nasty haunch"),
          new Option(
              "acquire Cheswick Copperbottom's compass (with Chewsick Copperbottom's notes)",
              3,
              "Cheswick Copperbottom's compass"),
          new Option(
              "acquire 40-60 Rubees&trade; (with LyleCo premium pickaxe)", 4, "Rubee&trade;"),
          new Option("leave", 6)
        }),

    // Choice 1297 is Stick to the Crypt
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1297",
        "Stick to the Crypt",
        new Object[] {
          new Option("acquire hero's skull", 1, "hero's skull"),
          new Option("acquire 40-60 Rubees&trade;", 2, "Rubee&trade;"),
          new Option(
              "acquire arrest warrant (with FantasyRealm Rogue's Mask)", 3, "arrest warrant"),
          new Option("leave", 6)
        }),

    // Choice 1298 is The "Phoenix"
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1298",
        "The \"Phoenix\"",
        new Object[] {
          new Option("fight \"Phoenix\" (with 5+ hot res and flask of holy water)", 1),
          new Option("get beaten up", 2),
          new Option("leave", 6)
        }),

    // Choice 1299 is Stop Dragon Your Feet
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1299",
        "Stop Dragon Your Feet",
        new Object[] {
          new Option(
              "fight Sewage Treatment Dragon (with 5+ stench res and dragon slaying sword)", 1),
          new Option("get beaten up", 2),
          new Option("leave", 6)
        }),

    // Choice 1300 is Just Vamping
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1300",
        "Just Vamping",
        new Object[] {
          new Option("fight Duke Vampire (with 250%+ init and Poison for Blood)", 1),
          new Option("get beaten up", 2),
          new Option("leave", 6)
        }),

    // Choice 1301 is Now You've Spied Her
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1301",
        "Now You've Spied Her",
        new Object[] {
          new Option("fight Spider Queen (with 500+ mox and Fantastic Immunity)", 1),
          new Option("get beaten up", 2),
          new Option("leave", 6)
        }),

    // Choice 1302 is Don't Be Arch
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1302",
        "Don't Be Arch",
        new Object[] {
          new Option("fight Archwizard (with 5+ cold res and charged druidic orb)", 1),
          new Option("get beaten up", 2),
          new Option("leave", 6)
        }),

    // Choice 1303 is Ley Lady Ley
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1303",
        "Ley Lady Ley",
        new Object[] {
          new Option("fight Ley Incursion (with 500+ mys and Cheswick Copperbottom's compass)", 1),
          new Option("get beaten up", 2),
          new Option("leave", 6)
        }),

    // Choice 1304 is He Is the Ghoul King, He Can Do Anything
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1304",
        "He Is the Ghoul King, He Can Do Anything",
        new Object[] {
          new Option("fight Ghoul King (with 5+ spooky res and Fantasy Faerie Blessing)", 1),
          new Option("get beaten up", 2),
          new Option("leave", 6)
        }),

    // Choice 1305 is The Brogre's Progress
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1305",
        "The Brogre's Progress",
        new Object[] {
          new Option("fight Ogre Chieftain (with 500+ mus and poisoned druidic s'more)", 1),
          new Option("get beaten up", 2),
          new Option("leave", 6)
        }),

    // Choice 1307 is It Takes a Thief
    new ChoiceAdventure(
        "FantasyRealm",
        "choiceAdventure1307",
        "It Takes a Thief",
        new Object[] {
          new Option(
              "fight Ted Schwartz, Master Thief (with 5+ sleaze res and notarized arrest warrant)",
              1),
          new Option("get beaten up", 2),
          new Option("leave", 6)
        }),

    // Choice 1310 is Granted a Boon
    // Choice 1312 is Choose a Soundtrack

    // Choice 1313 is Bastille Battalion
    // Choice 1314 is Bastille Battalion (turn #x)
    // Choice 1315 is Castle vs. Castle
    // Choice 1316 is GAME OVER
    // Choice 1317 is A Hello to Arms
    // Choice 1318 is Defensive Posturing
    // Choice 1319 is Cheese Seeking Behavior

    // Choice 1321 is Disguises Delimit

    // Choice 1322 is The Beginning of the Neverend
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1322",
        "Neverending Party Intro",
        new Object[] {
          new Option("accept quest", 1), new Option("reject quest", 2), new Option("leave", 6)
        }),

    // Choice 1323 is All Done!

    // Choice 1324 is It Hasn't Ended, It's Just Paused
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1324",
        "Neverending Party Pause",
        new Object[] {
          new Option(
              "Full HP/MP heal, +Mys Exp (20adv), clear partiers (quest), DJ meat (quest), megawoots (quest)",
              1),
          new Option("Mys stats, +Mus Exp (20 adv), snacks quest, burn trash (quest)", 2),
          new Option("Mox stats, +30 ML (50 adv), clear partiers (quest), booze quest", 3),
          new Option("Mus stats, +Mox Exp (20 adv), chainsaw, megawoots (quest)", 4),
          new Option("fight random partier", 5)
        }),

    // Choice 1325 is A Room With a View...  Of a Bed
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1325",
        "Neverending Party Bedroom",
        new Object[] {
          new Option("full HP/MP heal", 1),
          new Option("get 20 adv of +20% mys exp", 2),
          new Option("remove partiers (with jam band bootleg)", 3),
          new Option("get meat for dj (with 300 Moxie)", 4),
          new Option("increase megawoots", 5)
        }),

    // Choice 1326 is Gone Kitchin'
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1326",
        "Neverending Party Kitchen",
        new Object[] {
          new Option("gain mys stats", 1),
          new Option("get 20 adv of +20% Mus exp", 2),
          new Option("find out food to collect", 3),
          new Option("give collected food", 4),
          new Option("reduce trash", 5)
        }),

    // Choice 1327 is Forward to the Back
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1327",
        "Neverending Party Back Yard",
        new Object[] {
          new Option("gain mox stats", 1),
          new Option("get 50 adv of +30 ML", 2),
          new Option("find out booze to collect", 3),
          new Option("give collected booze", 4),
          new Option("remove partiers (with Purple Beast energy drink)", 5)
        }),

    // Choice 1328 is Basement Urges
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1328",
        "Neverending Party Basement",
        new Object[] {
          new Option("gain mus stats", 1),
          new Option("get 20 adv of +20% Mox exp", 2),
          new Option("acquire intimidating chainsaw", 3, "intimidating chainsaw"),
          new Option("increase megawoots", 4)
        }),

    // Choice 1331 is Daily Loathing Ballot
    // Choice 1332 is government requisition form

    // Choice 1333 is Canadian Cabin
    new ChoiceAdventure(
        "Crimbo18",
        "choiceAdventure1333",
        "Canadian Cabin",
        new Object[] {
          new Option("gain 50 adv of +100% weapon and spell damage", 1),
          new Option("acquire grilled mooseflank (with mooseflank)", 2, "grilled mooseflank"),
          new Option(
              "acquire antique Canadian lantern (with 10 thick walrus blubber)",
              3,
              "antique Canadian lantern"),
          new Option("acquire muskox-skin cap (with 10 tiny bombs)", 4, "muskox-skin cap"),
          new Option("acquire antique beer (with Yeast-Hungry)", 5, "antique beer"),
          new Option("skip adventure", 10)
        }),

    // Choice 1334 is Boxing Daycare (Lobby)
    // Choice 1335 is Boxing Day Spa
    new ChoiceAdventure(
        "Town",
        "choiceAdventure1335",
        "Boxing Day Spa",
        new Object[] {
          new Option("gain 100 adv of +200% muscle and +15 ML"),
          new Option("gain 100 adv of +200% moxie and +50% init"),
          new Option("gain 100 adv of +200% myst and +25% item drop"),
          new Option(
              "gain 100 adv of +100 max hp, +50 max mp, +25 dr, 5-10 mp regen, 10-20 hp regen"),
          new Option("skip")
        }),
    // Choice 1336 is Boxing Daycare

    // Choice 1339 is A Little Pump and Grind

    // Choice 1340 is Is There A Doctor In The House?
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure1340",
        "Lil' Doctor&trade; bag Quest",
        new Object[] {
          new Option("get quest", 1),
          new Option("refuse quest", 2),
          new Option("stop offering quest", 3)
        }),

    // Choice 1341 is A Pound of Cure
    new ChoiceAdventure(
        "Item-Driven",
        "choiceAdventure1341",
        "Lil' Doctor&trade; bag Cure",
        new Object[] {new Option("cure patient", 1)}),

    // Choice 1342 is Torpor

    // Choice 1345 is Blech House
    new ChoiceAdventure(
        "Mountain",
        "choiceAdventure1345",
        "Blech House",
        new Object[] {
          new Option("use muscle/weapon damage", 1),
          new Option("use myst/spell damage", 2),
          new Option("use mox/sleaze res", 3)
        }),

    // Choice 1392 is Decorate your Tent
    new ChoiceSpoiler(
        "choiceAdventure1392",
        "Decorate your Tent",
        new Object[] {
          "gain 20 adv of +3 mus xp", "gain 20 adv of +3 mys xp", "gain 20 adv of +3 mox xp"
        }),

    // Choice 1397 is Kringle workshop
    new ChoiceAdventure(
        "Tammy's Offshore Platform",
        "choiceAdventure1397",
        "Kringle workshop",
        new Object[] {
          new Option("craft stuff", 1),
          new Option("get waterlogged items", 2),
          new Option("fail at life", 3)
        }),

    // Choice 1411 is The Hall in the Hall
    new ChoiceAdventure(
        "The Drip",
        "choiceAdventure1411",
        "The Hall in the Hall",
        new Object[] {
          new Option("drippy pool table", 1),
          new Option("drippy vending machine", 2),
          new Option("drippy humanoid", 3),
          new Option("drippy keg", 4),
          new Option("Driplets", 5)
        }),

    // Choice 1415 is Revolting Vending
    new ChoiceAdventure(
        "The Drip",
        "choiceAdventure1415",
        "Revolting Vending",
        new Object[] {
          new Option("drippy candy bar", 1, "drippy candy bar"), new Option("Driplets", 2)
        }),
    new Object[] {
      IntegerPool.get(1415), IntegerPool.get(1), new AdventureResult(AdventureResult.MEAT, -10000)
    },

    // Choice 1427 is The Hidden Junction
    new ChoiceAdventure(
        "Guano Junction",
        "choiceAdventure1427",
        "The Hidden Junction",
        new Object[] {new Option("fight screambat", 1), new Option("gain ~360 meat", 2)}),

    // Choice 1428 is Your Neck of the Woods
    new ChoiceAdventure(
        "The Dark Neck of the Woods",
        "choiceAdventure1428",
        "Your Neck of the Woods",
        new Object[] {
          new Option("advance quest 1 step and gain 1000 meat", 1),
          new Option("advance quest 2 steps", 2)
        }),

    // Choice 1429 is No Nook Unknown
    new ChoiceAdventure(
        "Defiled Nook",
        "choiceAdventure1429",
        "No Nook Unknown",
        new Object[] {new Option("acquire 2 evil eyes", 1), new Option("fight party skeleton", 2)}),

    // Choice 1430 is Ghostly Memories
    new ChoiceAdventure(
        "Ghostly Memories",
        "choiceAdventure1430",
        "A-Boo Peak",
        new Object[] {
          new Option("the Horror, spooky/cold res recommended", 1),
          new Option("fight oil baron", 2),
          new Option("lost overlook lodge", 3)
        }),

    // Choice 1431 is Here There Be Giants
    new ChoiceAdventure(
        "Here There Be Giants",
        "choiceAdventure1431",
        "The Castle in the Clouds in the Sky (Top Floor)",
        new Object[] {
          new Option("complete trash quest, unlock HiTS", 1),
          new Option("fight goth giant, acquire black candles", 2),
          new Option("fight raver, restore hp/mp", 3),
          new Option("complete quest w/ mohawk wig, gain ~500 meat", 4)
        }),

    // Choice 1432 is Mob Maptality
    new ChoiceAdventure(
        "Mob Maptality",
        "choiceAdventure1432",
        "A Mob of Zeppelin Protesters",
        new Object[] {
          new Option("creep protestors (more with sleaze damage/sleaze spell damage)", 1),
          new Option("scare protestors (more with lynyrd gear)", 2),
          new Option("set fire to protestors (more with Flamin' Whatshisname)", 3)
        }),

    // Choice 1433 is Hippy camp verge of war Sneaky Sneaky
    new ChoiceAdventure(
        "Sneaky Sneaky",
        "choiceAdventure1433",
        "The Hippy Camp (Verge of War)",
        new Object[] {
          new Option("fight a war hippy drill sargent", 1),
          new Option("fight a war hippy space cadet", 2),
          new Option("start the war", 3)
        }),

    // Choice 1434 is frat camp verge of war Sneaky Sneaky
    new ChoiceAdventure(
        "Sneaky Sneaky",
        "choiceAdventure1434",
        "The Frat Camp (Verge of War)",
        new Object[] {
          new Option("fight a war pledge/acquire sake bombers", 1),
          new Option("start the war", 2),
          new Option("fight a frat warrior drill sergeant/acquire beer bombs", 3)
        }),

    // Choice 1436 is Billiards Room Options
    new ChoiceAdventure(
        "Billiards Room Options",
        "choiceAdventure1436",
        "The Haunted Billiards Room",
        new Object[] {
          new Option("aquire pool cue", 1),
          new Option("play pool with the ghost", 2),
          new Option("fight a chalkdust wraith", 3)
        })
  };

  public static final ChoiceAdventure[] CHOICE_ADVS;

  // We choose to not make some choice adventures configurable, but we
  // want to provide spoilers in the browser for them.

  public static final ChoiceAdventure[] CHOICE_ADV_SPOILERS;

  // Some choice adventures have options that cost meat or items

  public static final Object[][] CHOICE_COST;

  static {
    ArrayList choices = new ArrayList();
    ArrayList spoils = new ArrayList();
    ArrayList costs = new ArrayList();
    for (int i = 0; i < CHOICE_DATA.length; ++i) {
      Object it = CHOICE_DATA[i];
      (it instanceof ChoiceSpoiler ? spoils : it instanceof ChoiceAdventure ? choices : costs)
          .add(it);
    }
    CHOICE_ADVS = (ChoiceAdventure[]) choices.toArray(new ChoiceAdventure[choices.size()]);
    CHOICE_ADV_SPOILERS = (ChoiceAdventure[]) spoils.toArray(new ChoiceAdventure[spoils.size()]);
    CHOICE_COST = (Object[][]) costs.toArray(new Object[costs.size()][]);

    Arrays.sort(ChoiceManager.CHOICE_ADVS);
  }

  public static void initializeAfterChoice() {
    ChoiceManager.action = PostChoiceAction.INITIALIZE;
    GenericRequest request = ChoiceManager.CHOICE_HANDLER;
    request.constructURLString("choice.php");
    request.run();
    ChoiceUtilities.printChoices(ChoiceManager.lastResponseText);
  }

  public static boolean initializingAfterChoice() {
    return ChoiceManager.action == PostChoiceAction.INITIALIZE;
  }

  public static void ascendAfterChoice() {
    ChoiceManager.action = PostChoiceAction.ASCEND;
  }

  private static AdventureResult getCost(final int choice, final int decision) {
    for (int i = 0; i < ChoiceManager.CHOICE_COST.length; ++i) {
      if (choice == ((Integer) ChoiceManager.CHOICE_COST[i][0]).intValue()
          && decision == ((Integer) ChoiceManager.CHOICE_COST[i][1]).intValue()) {
        return (AdventureResult) ChoiceManager.CHOICE_COST[i][2];
      }
    }

    return null;
  }

  private static void payCost(final int choice, final int decision) {
    AdventureResult cost = ChoiceManager.getCost(choice, decision);

    // No cost for this choice/decision
    if (cost == null) {
      return;
    }

    long costCount = cost.getLongCount();

    // No cost for this choice/decision
    if (costCount == 0) {
      return;
    }

    if (cost.isItem()) {
      int inventoryCount = cost.getCount(KoLConstants.inventory);
      // Make sure we have enough in inventory
      if (costCount + inventoryCount < 0) {
        return;
      }

      if (costCount > 0) {
        long multiplier = inventoryCount / costCount;
        cost = cost.getInstance(multiplier * costCount * -1);
      }
    } else if (cost.isMeat()) {
      long purseCount = KoLCharacter.getAvailableMeat();
      // Make sure we have enough in inventory
      if (costCount + purseCount < 0) {
        return;
      }
    } else if (cost.isMP()) {
      long current = KoLCharacter.getCurrentMP();
      // Make sure we have enough mana
      if (costCount + current < 0) {
        return;
      }
    } else {
      return;
    }

    ResultProcessor.processResult(cost);
  }

  public static final void decorateChoice(final int choice, final StringBuffer buffer) {
    if (choice >= 48 && choice <= 70) {
      // Add "Go To Goal" button for the Violet Fog
      VioletFogManager.addGoalButton(buffer);
      return;
    }

    if (choice >= 904 && choice <= 913) {
      // Add "Go To Goal" button for the Louvre.
      LouvreManager.addGoalButton(buffer);
      return;
    }

    switch (choice) {
      case 360:
        WumpusManager.decorate(buffer);
        break;
      case 392:
        MemoriesDecorator.decorateElements(choice, buffer);
        break;
      case 443:
        // Chess Puzzle
        RabbitHoleManager.decorateChessPuzzle(buffer);
        break;
      case 485:
        // Fighters of Fighting
        ArcadeRequest.decorateFightersOfFighting(buffer);
        break;
      case 486:
        // Dungeon Fist
        ArcadeRequest.decorateDungeonFist(buffer);
        break;

      case 535:
        // Add "Go To Goal" button for a Safety Shelter Map
        SafetyShelterManager.addRonaldGoalButton(buffer);
        break;

      case 536:
        // Add "Go To Goal" button for a Safety Shelter Map
        SafetyShelterManager.addGrimaceGoalButton(buffer);
        break;

      case 537:
        // Play Porko!
      case 540:
        // Big-Time Generator
        SpaaaceRequest.decoratePorko(buffer);
        break;

      case 546:
        // Add "Go To Goal" button for Interview With You
        VampOutManager.addGoalButton(buffer);
        break;

      case 594:
        // Add "Go To Goal" button for a Lost Room
        LostKeyManager.addGoalButton(buffer);
        break;

      case 665:
        // Add "Solve" button for A Gracious Maze
        GameproManager.addGoalButton(buffer);
        break;

      case 703:
        // Load the options of the dreadscroll with the correct responses
        DreadScrollManager.decorate(buffer);
        break;

      case 850:
        RumpleManager.decorateWorkshop(buffer);
        break;

      case 872:
        ChoiceManager.decorateDrawnOnward(buffer);
        break;

      case 989:
        // Highlight valid card
        ChoiceManager.decorateParanormalTestLab(buffer);
        break;

      case 1023:
        // Like a Bat Into Hell
        StringUtilities.globalStringReplace(buffer, "Go right back to the fight!", "UNDYING!");
        break;

      case 1024:
        // Like a Bat out of Hell
        StringUtilities.globalStringReplace(buffer, "Return to the fight!", "UNDYING!");
        break;

      case 1094:
        // Back Room SMOOCHing
        ChoiceManager.decorateBackRoomSMOOCHing(buffer);
        break;

      case 1278:
        // Madame Zatara’s Relationship Fortune Teller
        ClanFortuneDecorator.decorateQuestion(buffer);
        break;

      case 1331:
        // Daily Loathing Ballot
        ChoiceManager.decorateVote(buffer);
        break;
      case 1435:
        // Leading Yourself Right to Them
        ChoiceManager.decorateMonsterMap(buffer);
        break;
    }
  }

  private static final Pattern PHOTO_PATTERN =
      Pattern.compile("<select name=\"(.*?)\".*?</select>");

  public static final void decorateDrawnOnward(final StringBuffer buffer) {
    Matcher matcher = ChoiceManager.PHOTO_PATTERN.matcher(buffer.toString());
    while (matcher.find()) {
      String photo = matcher.group(1);
      String find = matcher.group(0);
      String replace = null;
      if (photo.equals("photo1")) {
        if (find.contains("2259")) {
          replace =
              StringUtilities.singleStringReplace(
                  find, "<option value=\"2259\">", "<option value=\"2259\" selected>");
        }
      } else if (photo.equals("photo2")) {
        if (find.contains("7264")) {
          replace =
              StringUtilities.singleStringReplace(
                  find, "<option value=\"7264\">", "<option value=\"7264\" selected>");
        }
      } else if (photo.equals("photo3")) {
        if (find.contains("7263")) {
          replace =
              StringUtilities.singleStringReplace(
                  find, "<option value=\"7263\">", "<option value=\"7263\" selected>");
        }
      } else if (photo.equals("photo4")) {
        if (find.contains("7265")) {
          replace =
              StringUtilities.singleStringReplace(
                  find, "<option value=\"7265\">", "<option value=\"7265\" selected>");
        }
      }

      if (replace != null) {
        StringUtilities.singleStringReplace(buffer, find, replace);
      }
    }
  }

  public static final void decorateParanormalTestLab(final StringBuffer buffer) {
    String pageText = buffer.toString();
    int answer = 0;
    if (pageText.contains("ever-changing constellation")) {
      answer = 1;
    } else if (pageText.contains("card in the circle of light")) {
      answer = 2;
    } else if (pageText.contains("waves a fly away")) {
      answer = 3;
    } else if (pageText.contains("back to square one")) {
      answer = 4;
    } else if (pageText.contains("adds to your anxiety")) {
      answer = 5;
    }
    String find = "espcard" + answer + ".gif";
    String replace = "espcard" + answer + ".gif style=\"border: 2px solid blue;\"";
    if (pageText.contains(find)) {
      StringUtilities.singleStringReplace(buffer, find, replace);
    }
  }

  public static final void decorateBackRoomSMOOCHing(final StringBuffer buffer) {
    int choice = Preferences.getInteger("choiceAdventure1094");
    String find = "smoochdoor" + choice + ".gif";
    String replace = "smoochdoor" + choice + ".gif style=\"border: 2px solid blue;\"";
    if (buffer.toString().contains(find)) {
      StringUtilities.singleStringReplace(buffer, find, replace);
    }
    StringUtilities.globalStringReplace(buffer, "Door #1", "Geve Smimmons");
    StringUtilities.globalStringReplace(buffer, "Door #2", "Raul Stamley");
    StringUtilities.globalStringReplace(buffer, "Door #3", "Pener Crisp");
    StringUtilities.globalStringReplace(buffer, "Door #4", "Deuce Freshly");
  }

  public static final String parseVoteSpeech(final String party, final String speech) {
    if (party.contains("Pork Elf Historical Preservation Party")) {
      if (speech.contains("strict curtailing of unnatural modern technologies")) {
        return "government bureaucrat";
      } else if (speech.contains("reintroduce Pork Elf DNA")) {
        return "terrible mutant";
      } else if (speech.contains("kingdom-wide seance")) {
        return "angry ghost";
      } else if (speech.contains("very interested in snakes")) {
        return "annoyed snake";
      } else if (speech.contains("lots of magical lard")) {
        return "slime blob";
      }
    } else if (party.contains("Clan Ventrilo")) {
      if (speech.contains("bringing this blessing to the entire population")) {
        return "slime blob";
      } else if (speech.contains("see your deceased loved ones again")) {
        return "angry ghost";
      } else if (speech.contains("stronger and more vigorous")) {
        return "terrible mutant";
      } else if (speech.contains("implement healthcare reforms")) {
        return "government bureaucrat";
      } else if (speech.contains("flavored drink in a tube")) {
        return "annoyed snake";
      }
    } else if (party.contains("Bureau of Efficient Government")) {
      if (speech.contains("graveyards are a terribly inefficient use of space")) {
        return "angry ghost";
      } else if (speech.contains("strictly enforced efficiency laws")) {
        return "government bureaucrat";
      } else if (speech.contains("distribute all the medications for all known diseases ")) {
        return "terrible mutant";
      } else if (speech.contains("introduce an influx of snakes")) {
        return "annoyed snake";
      } else if (speech.contains("releasing ambulatory garbage-eating slimes")) {
        return "slime blob";
      }
    } else if (party.contains("Scions of Ich'Xuul'kor")) {
      if (speech.contains("increase awareness of our really great god")) {
        return "terrible mutant";
      } else if (speech.contains("hunt these evil people down")) {
        return "government bureaucrat";
      } else if (speech.contains("sound of a great hissing")) {
        return "annoyed snake";
      } else if (speech.contains("make things a little bit more like he's used to")) {
        return "slime blob";
      } else if (speech.contains("kindness energy")) {
        return "angry ghost";
      }
    } else if (party.contains("Extra-Terrific Party")) {
      if (speech.contains("wondrous chemical")) {
        return "terrible mutant";
      } else if (speech.contains("comprehensive DNA harvesting program")) {
        return "government bureaucrat";
      } else if (speech.contains("mining and refining processes begin")) {
        return "slime blob";
      } else if (speech.contains("warp engines will not destabilize")) {
        return "angry ghost";
      } else if (speech.contains("breeding pair of these delightful creatures")) {
        return "annoyed snake";
      }
    }

    return null;
  }

  public static final void decorateVote(final StringBuffer buffer) {
    Matcher matcher = ChoiceManager.VOTE_SPEECH_PATTERN.matcher(buffer.toString());

    int count = 1;

    while (matcher.find()) {
      String find = matcher.group(0);
      String monsterName = Preferences.getString("_voteMonster" + count);

      if (monsterName != "") {
        String replace =
            StringUtilities.singleStringReplace(
                find,
                "</blockquote>",
                "<br />(vote for " + monsterName + " tomorrow)</blockquote>");
        StringUtilities.singleStringReplace(buffer, find, replace);
      }

      count++;
    }
  }

  public static final Pattern MAPPED_MONSTER_PATTERN =
      Pattern.compile(
          "(<input type=\"hidden\" name=\"heyscriptswhatsupwinkwink\" value=\"(\\d+)\" />\\s+<input type=\"submit\" class=\"button\" value=\").*?(\" />\\s+</form>)");

  public static final void decorateMonsterMap(final StringBuffer buffer) {
    Matcher matcher = ChoiceManager.MAPPED_MONSTER_PATTERN.matcher(buffer.toString());

    while (matcher.find()) {
      String find = matcher.group(0);
      Integer monsterId = Integer.parseInt(matcher.group(2));
      String monsterName = MonsterDatabase.getMonsterName(monsterId);

      String replace = matcher.group(1) + monsterName + matcher.group(3);
      StringUtilities.singleStringReplace(buffer, find, replace);
    }
  }

  public static final Object[][] choiceSpoilers(final int choice, final StringBuffer buffer) {
    Object[][] spoilers;

    // See if spoilers are dynamically generated
    spoilers = ChoiceManager.dynamicChoiceSpoilers(choice);
    if (spoilers != null) {
      return spoilers;
    }

    // Nope. See if it's in the Violet Fog
    spoilers = VioletFogManager.choiceSpoilers(choice);
    if (spoilers != null) {
      return spoilers;
    }

    // Nope. See if it's in the Louvre
    spoilers = LouvreManager.choiceSpoilers(choice);
    if (spoilers != null) {
      return spoilers;
    }

    // Nope. See if it's On a Downtown Train
    spoilers = MonorailManager.choiceSpoilers(choice, buffer);
    if (spoilers != null) {
      return spoilers;
    }

    // Nope. See if it's a Safety Shelter Map
    if (choice == 535 || choice == 536) {
      return null;
    }

    // Nope. See if it's Interview with you.
    if (choice == 546) {
      return null;
    }

    // See if it's A Lost Room
    if (choice == 594) {
      return null;
    }
    // See if this choice is controlled by user option
    for (int i = 0; i < ChoiceManager.CHOICE_ADVS.length; ++i) {
      ChoiceAdventure choiceAdventure = ChoiceManager.CHOICE_ADVS[i];
      if (choiceAdventure.getChoice() == choice) {
        return choiceAdventure.getSpoilers();
      }
    }

    // Nope. See if we know this choice
    for (int i = 0; i < ChoiceManager.CHOICE_ADV_SPOILERS.length; ++i) {
      ChoiceAdventure choiceAdventure = ChoiceManager.CHOICE_ADV_SPOILERS[i];
      if (choiceAdventure.getChoice() == choice) {
        return choiceAdventure.getSpoilers();
      }
    }

    // Unknown choice
    return null;
  }

  private static Object[][] dynamicChoiceSpoilers(final int choice) {
    switch (choice) {
      case 5:
        // How Depressing
      case 7:
        // Heart of Very, Very Dark Darkness
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Spooky Gravy Burrow");

      case 184:
        // Yes, You're a Rock Starrr
      case 185:
        // Arrr You Man Enough?
      case 187:
        // That Explains All The Eyepatches
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Barrrney's Barrr");

      case 188:
        // The Infiltrationist
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Orcish Frat House Blueprints");

      case 272:
        // Marketplace Entrance
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Hobo Marketplace");

      case 298:
        // In the Shade
        return ChoiceManager.dynamicChoiceSpoilers(choice, "An Octopus's Garden");

      case 304:
        // A Vent Horizon
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Marinara Trench");

      case 305:
        // There is Sauce at the Bottom of the Ocean
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Marinara Trench");

      case 309:
        // Barback
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Dive Bar");

      case 360:
        // Wumpus Hunt
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Jungles of Ancient Loathing");

      case 410:
      case 411:
      case 412:
      case 413:
      case 414:
      case 415:
      case 416:
      case 417:
      case 418:
        // The Barracks
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Barracks");

      case 442:
        // A Moment of Reflection
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Rabbit Hole");

      case 522:
        // Welcome to the Footlocker
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Welcome to the Footlocker");

      case 502:
        // Arboreal Respite
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Arboreal Respite");

      case 579:
        // Such Great Heights
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Such Great Heights");

      case 580:
        // The Hidden Heart of the Hidden Temple
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Hidden Heart of the Hidden Temple");

      case 581:
        // Such Great Depths
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Such Great Depths");

      case 582:
        // Fitting In
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Fitting In");

      case 606:
        // Lost in the Great Overlook Lodge
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Lost in the Great Overlook Lodge");

      case 611:
        // The Horror...(A-Boo Peak)
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Horror...");

      case 636:
      case 637:
      case 638:
      case 639:
        // Old Man psychoses
        return ChoiceManager.dynamicChoiceSpoilers(choice, "First Mate's Log Entry");

      case 641:
        // Stupid Pipes. (Mystic's psychoses)
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Stupid Pipes.");

      case 642:
        // You're Freaking Kidding Me (Mystic's psychoses)
        return ChoiceManager.dynamicChoiceSpoilers(choice, "You're Freaking Kidding Me");

      case 644:
        // Snakes. (Mystic's psychoses)
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Snakes.");

      case 645:
        // So... Many... Skulls... (Mystic's psychoses)
        return ChoiceManager.dynamicChoiceSpoilers(choice, "So... Many... Skulls...");

      case 647:
        // A Stupid Dummy. Also, a Straw Man. (Mystic's psychoses)
        return ChoiceManager.dynamicChoiceSpoilers(choice, "A Stupid Dummy. Also, a Straw Man.");

      case 648:
        // Slings and Arrows (Mystic's psychoses)
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Slings and Arrows");

      case 650:
        // This Is Your Life. Your Horrible, Horrible Life. (Mystic's psychoses)
        return ChoiceManager.dynamicChoiceSpoilers(
            choice, "This Is Your Life. Your Horrible, Horrible Life.");

      case 651:
        // The Wall of Wailing (Mystic's psychoses)
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Wall of Wailing");

      case 669:
        // The Fast and the Furry-ous
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Fast and the Furry-ous");

      case 670:
        // You Don't Mess Around with Gym
        return ChoiceManager.dynamicChoiceSpoilers(choice, "You Don't Mess Around with Gym");

      case 678:
        // Yeah, You're for Me, Punk Rock Giant
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Yeah, You're for Me, Punk Rock Giant");

      case 692:
        // I Wanna Be a Door
        return ChoiceManager.dynamicChoiceSpoilers(choice, "I Wanna Be a Door");

      case 696:
        // Stick a Fork In It
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Stick a Fork In It");

      case 697:
        // Sophie's Choice
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Sophie's Choice");

      case 698:
        // From Bad to Worst
        return ChoiceManager.dynamicChoiceSpoilers(choice, "From Bad to Worst");

      case 700:
        // Delirium in the Cafeterium
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Delirium in the Cafeterium");

      case 721:
        // The Cabin in the Dreadsylvanian Woods
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Cabin in the Dreadsylvanian Woods");

      case 722:
        // The Kitchen in the Woods
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Kitchen in the Woods");

      case 723:
        // What Lies Beneath (the Cabin)
        return ChoiceManager.dynamicChoiceSpoilers(choice, "What Lies Beneath (the Cabin)");

      case 724:
        // Where it's Attic
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Where it's Attic");

      case 725:
        // Tallest Tree in the Forest
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Tallest Tree in the Forest");

      case 726:
        // Top of the Tree, Ma!
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Top of the Tree, Ma!");

      case 727:
        // All Along the Watchtower
        return ChoiceManager.dynamicChoiceSpoilers(choice, "All Along the Watchtower");

      case 728:
        // Treebasing
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Treebasing");

      case 729:
        // Below the Roots
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Below the Roots");

      case 730:
        // Hot Coals
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Hot Coals");

      case 731:
        // The Heart of the Matter
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Heart of the Matter");

      case 732:
        // Once Midden, Twice Shy
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Once Midden, Twice Shy");

      case 733:
        // Dreadsylvanian Village Square
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Dreadsylvanian Village Square");

      case 734:
        // Fright School
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Fright School");

      case 735:
        // Smith, Black as Night
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Smith, Black as Night");

      case 736:
        // Gallows
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Gallows");

      case 737:
        // The Even More Dreadful Part of Town
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Even More Dreadful Part of Town");

      case 738:
        // A Dreadful Smell
        return ChoiceManager.dynamicChoiceSpoilers(choice, "A Dreadful Smell");

      case 739:
        // The Tinker's. Damn.
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Tinker's. Damn.");

      case 740:
        // Eight, Nine, Tenement
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Eight, Nine, Tenement");

      case 741:
        // The Old Duke's Estate
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Old Duke's Estate");

      case 742:
        // The Plot Thickens
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Plot Thickens");

      case 743:
        // No Quarter
        return ChoiceManager.dynamicChoiceSpoilers(choice, "No Quarter");

      case 744:
        // The Master Suite -- Sweet!
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Master Suite -- Sweet!");

      case 745:
        // This Hall is Really Great
        return ChoiceManager.dynamicChoiceSpoilers(choice, "This Hall is Really Great");

      case 746:
        // The Belle of the Ballroom
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Belle of the Ballroom");

      case 747:
        // Cold Storage
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Cold Storage");

      case 748:
        // Dining In (the Castle)
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Dining In (the Castle)");

      case 749:
        // Tower Most Tall
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Tower Most Tall");

      case 750:
        // Working in the Lab, Late One Night
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Working in the Lab, Late One Night");

      case 751:
        // Among the Quaint and Curious Tomes.
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Among the Quaint and Curious Tomes.");

      case 752:
        // In The Boudoir
        return ChoiceManager.dynamicChoiceSpoilers(choice, "In The Boudoir");

      case 753:
        // The Dreadsylvanian Dungeon
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Dreadsylvanian Dungeon");

      case 754:
        // Live from Dungeon Prison
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Live from Dungeon Prison");

      case 755:
        // The Hot Bowels
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Hot Bowels");

      case 756:
        // Among the Fungus
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Among the Fungus");

      case 758:
        // End of the Path
        return ChoiceManager.dynamicChoiceSpoilers(choice, "End of the Path");

      case 759:
        // You're About to Fight City Hall
        return ChoiceManager.dynamicChoiceSpoilers(choice, "You're About to Fight City Hall");

      case 760:
        // Holding Court
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Holding Court");

        // Choice 761 is Staring Upwards...
        // Choice 762 is Try New Extra-Strength Anvil
        // Choice 764 is The Machine
        // Choice 765 is Hello Gallows

      case 772:
        // Saved by the Bell
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Saved by the Bell");

      case 780:
        // Action Elevator
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Action Elevator");

      case 781:
        // Earthbound and Down
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Earthbound and Down");

      case 783:
        // Water You Dune
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Water You Dune");

      case 784:
        // You, M. D.
        return ChoiceManager.dynamicChoiceSpoilers(choice, "You, M. D.");

      case 785:
        // Air Apparent
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Air Apparent");

      case 786:
        // Working Holiday
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Working Holiday");

      case 787:
        // Fire when Ready
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Fire when Ready");

      case 788:
        // Life is Like a Cherry of Bowls
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Life is Like a Cherry of Bowls");

      case 789:
        // Where Does The Lone Ranger Take His Garbagester?
        return ChoiceManager.dynamicChoiceSpoilers(
            choice, "Where Does The Lone Ranger Take His Garbagester?");

      case 791:
        // Legend of the Temple in the Hidden City
        return ChoiceManager.dynamicChoiceSpoilers(
            choice, "Legend of the Temple in the Hidden City");

      case 801:
        // A Reanimated Conversation
        return ChoiceManager.dynamicChoiceSpoilers(choice, "A Reanimated Conversation");

      case 918:
        // Yachtzee!
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Yachtzee!");

      case 988:
        // The Containment Unit
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Containment Unit");

      case 1049:
        // Tomb of the Unknown Your Class Here
        return ChoiceManager.dynamicChoiceSpoilers(choice, "Tomb of the Unknown Your Class Here");

      case 1411:
        // The Hall in the Hall
        return ChoiceManager.dynamicChoiceSpoilers(choice, "The Hall in the Hall");
    }

    return null;
  }

  private static Object[][] dynamicChoiceSpoilers(final int choice, final String name) {
    Object[][] result = new Object[3][];

    // The choice option is the first element
    result[0] = new String[1];
    result[0][0] = "choiceAdventure" + choice;

    // The name of the choice is second element
    result[1] = new String[1];
    result[1][0] = name;

    // An array of choice spoilers is the third element
    result[2] = ChoiceManager.dynamicChoiceOptions(choice);

    return result;
  }

  public static final Object[] dynamicChoiceOptions(final int choice) {
    Object[] result;
    switch (choice) {
      case 5:
        // Heart of Very, Very Dark Darkness
        result = new Object[2];

        boolean rock = InventoryManager.getCount(ItemPool.INEXPLICABLY_GLOWING_ROCK) >= 1;

        result[0] = "You " + (rock ? "" : "DON'T ") + " have an inexplicably glowing rock";
        result[1] = "skip adventure";

        return result;

      case 7:
        // How Depressing
        result = new Object[2];

        boolean glove = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.SPOOKY_GLOVE, 1));

        result[0] = "spooky glove " + (glove ? "" : "NOT ") + "equipped";
        result[1] = "skip adventure";

        return result;

      case 184:
        // That Explains All The Eyepatches
        result = new Object[6];

        // The choices are based on character class.
        // Mus: combat, shot of rotgut (2948), drunkenness
        // Mys: drunkenness, shot of rotgut (2948), shot of rotgut (2948)
        // Mox: combat, drunkenness, shot of rotgut (2948)

        result[0] =
            KoLCharacter.isMysticalityClass()
                ? "3 drunk and stats (varies by class)"
                : "enter combat (varies by class)";
        result[1] =
            KoLCharacter.isMoxieClass()
                ? "3 drunk and stats (varies by class)"
                : new Option("shot of rotgut (varies by class)", "shot of rotgut");
        result[2] =
            KoLCharacter.isMuscleClass()
                ? "3 drunk and stats (varies by class)"
                : new Option("shot of rotgut (varies by class)", "shot of rotgut");
        result[3] = "always 3 drunk & stats";
        result[4] = "always shot of rotgut";
        result[5] = "combat (or rotgut if Myst class)";
        return result;

      case 185:
        // Yes, You're a Rock Starrr
        result = new Object[3];

        int drunk = KoLCharacter.getInebriety();

        // 0 drunk: base booze, mixed booze, fight
        // More than 0 drunk: base booze, mixed booze, stats

        result[0] = "base booze";
        result[1] = "mixed booze";
        result[2] = drunk == 0 ? "combat" : "stats";
        return result;

      case 187:
        // Arrr You Man Enough?

        result = new Object[2];
        float odds = BeerPongRequest.pirateInsultOdds() * 100.0f;

        result[0] = KoLConstants.FLOAT_FORMAT.format(odds) + "% chance of winning";
        result[1] = odds == 100.0f ? "Oh come on. Do it!" : "Try later";
        return result;

      case 188:
        // The Infiltrationist
        result = new Object[3];

        // Attempt a frontal assault
        boolean ok1 = EquipmentManager.isWearingOutfit(OutfitPool.FRAT_OUTFIT);
        result[0] = "Frat Boy Ensemble (" + (ok1 ? "" : "NOT ") + "equipped)";

        // Go in through the side door
        boolean ok2a = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MULLET_WIG, 1));
        boolean ok2b = InventoryManager.getCount(ItemPool.BRIEFCASE) >= 1;
        result[1] =
            "mullet wig ("
                + (ok2a ? "" : "NOT ")
                + "equipped) + briefcase ("
                + (ok2b ? "OK)" : "0 in inventory)");

        // Catburgle
        boolean ok3a = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.FRILLY_SKIRT, 1));
        int wings = InventoryManager.getCount(ItemPool.HOT_WING);
        result[2] =
            "frilly skirt ("
                + (ok3a ? "" : "NOT ")
                + "equipped) + 3 hot wings ("
                + wings
                + " in inventory)";

        return result;

      case 191:
        // Chatterboxing
        result = new Object[4];

        int trinks = InventoryManager.getCount(ItemPool.VALUABLE_TRINKET);
        result[0] = "moxie substats";
        result[1] =
            trinks == 0
                ? "lose hp (no valuable trinkets)"
                : "use valuable trinket to banish (" + trinks + " in inventory)";
        result[2] = "muscle substats";
        result[3] = "mysticality substats";

        return result;

      case 272:
        // Marketplace Entrance
        result = new Object[2];

        int nickels = InventoryManager.getCount(ItemPool.HOBO_NICKEL);
        boolean binder = KoLCharacter.hasEquipped(ItemPool.get(ItemPool.HOBO_CODE_BINDER, 1));

        result[0] = nickels + " nickels, " + (binder ? "" : "NO ") + " hobo code binder equipped";
        result[1] = "skip adventure";

        return result;

      case 298:
        // In the Shade
        result = new Object[2];

        int seeds = InventoryManager.getCount(ItemPool.SEED_PACKET);
        int slime = InventoryManager.getCount(ItemPool.GREEN_SLIME);

        result[0] = seeds + " seed packets, " + slime + " globs of green slime";
        result[1] = "skip adventure";

        return result;

      case 304:
        // A Vent Horizon
        result = new Object[2];

        int summons = 3 - Preferences.getInteger("tempuraSummons");

        result[0] = summons + " summons left today";
        result[1] = "skip adventure";

        return result;

      case 305:
        // There is Sauce at the Bottom of the Ocean
        result = new Object[2];

        int globes = InventoryManager.getCount(ItemPool.MERKIN_PRESSUREGLOBE);

        result[0] = globes + " Mer-kin pressureglobes";
        result[1] = "skip adventure";

        return result;

      case 309:
        // Barback
        result = new Object[2];

        int seaodes = 3 - Preferences.getInteger("seaodesFound");

        result[0] = seaodes + " more seodes available today";
        result[1] = "skip adventure";

        return result;

      case 360:
        // Wumpus Hunt
        return WumpusManager.dynamicChoiceOptions(ChoiceManager.lastResponseText);

      case 410:
      case 411:
      case 412:
      case 413:
      case 414:
      case 415:
      case 416:
      case 417:
      case 418:
        // The Barracks
        result = HaciendaManager.getSpoilers(choice);
        return result;

      case 442:
        // A Moment of Reflection
        result = new Object[6];
        int count = 0;
        if (InventoryManager.getCount(ItemPool.BEAUTIFUL_SOUP) > 0) {
          ++count;
        }
        if (InventoryManager.getCount(ItemPool.LOBSTER_QUA_GRILL) > 0) {
          ++count;
        }
        if (InventoryManager.getCount(ItemPool.MISSING_WINE) > 0) {
          ++count;
        }
        if (InventoryManager.getCount(ItemPool.WALRUS_ICE_CREAM) > 0) {
          ++count;
        }
        if (InventoryManager.getCount(ItemPool.HUMPTY_DUMPLINGS) > 0) {
          ++count;
        }
        result[0] = "Seal Clubber/Pastamancer item, or yellow matter custard";
        result[1] = "Sauceror/Accordion Thief item, or delicious comfit?";
        result[2] = "Disco Bandit/Turtle Tamer item, or fight croqueteer";
        result[3] = "you have " + count + "/5 of the items needed for an ittah bittah hookah";
        result[4] = "get a chess cookie";
        result[5] = "skip adventure";
        return result;

      case 502:
        // Arboreal Respite
        result = new Object[3];

        // meet the vampire hunter, trade bar skins or gain a spooky sapling
        int stakes = InventoryManager.getCount(ItemPool.WOODEN_STAKES);
        int hearts = InventoryManager.getCount(ItemPool.VAMPIRE_HEART);
        String hunterAction =
            (stakes > 0 ? "and get wooden stakes" : "and trade " + hearts + " hearts");

        int barskins = InventoryManager.getCount(ItemPool.BAR_SKIN);
        int saplings = InventoryManager.getCount(ItemPool.SPOOKY_SAPLING);

        result[0] =
            "gain some meat, meet the vampire hunter "
                + hunterAction
                + ", sell bar skins ("
                + barskins
                + ") or buy a spooky sapling ("
                + saplings
                + ")";

        // gain mosquito larva, gain quest coin or gain a vampire heart
        boolean haveMap = InventoryManager.getCount(ItemPool.SPOOKY_MAP) > 0;
        boolean haveCoin = InventoryManager.getCount(ItemPool.TREE_HOLED_COIN) > 0;
        boolean getCoin = (!haveCoin && !haveMap && !KoLCharacter.getTempleUnlocked());
        String coinAction = (getCoin ? "gain quest coin" : "skip adventure");

        result[1] =
            "gain mosquito larva or spooky mushrooms, "
                + coinAction
                + ", get stats or fight a vampire";

        // gain a starter item, gain Spooky-Gro fertilizer or gain spooky temple map
        int fertilizer = InventoryManager.getCount(ItemPool.SPOOKY_FERTILIZER);
        String mapAction = (haveCoin ? ", gain spooky temple map" : "");

        result[2] =
            "gain a starter item, gain Spooky-Gro fertilizer (" + fertilizer + ")" + mapAction;

        return result;

      case 522:
        // Welcome to the Footlocker
        result = new Object[2];

        boolean havePolearm =
            (InventoryManager.getCount(ItemPool.KNOB_GOBLIN_POLEARM) > 0
                || InventoryManager.getEquippedCount(ItemPool.KNOB_GOBLIN_POLEARM) > 0);
        boolean havePants =
            (InventoryManager.getCount(ItemPool.KNOB_GOBLIN_PANTS) > 0
                || InventoryManager.getEquippedCount(ItemPool.KNOB_GOBLIN_PANTS) > 0);
        boolean haveHelm =
            (InventoryManager.getCount(ItemPool.KNOB_GOBLIN_HELM) > 0
                || InventoryManager.getEquippedCount(ItemPool.KNOB_GOBLIN_HELM) > 0);

        result[0] =
            !havePolearm
                ? new Option("knob goblin elite polearm", "knob goblin elite polearm")
                : !havePants
                    ? new Option("knob goblin elite pants", "knob goblin elite pants")
                    : !haveHelm
                        ? new Option("knob goblin elite helm", "knob goblin elite helm")
                        : new Option("knob jelly donut", "knob jelly donut");
        result[1] = "skip adventure";
        return result;

      case 579:
        // Such Great Heights
        result = new Object[3];

        boolean haveNostril = (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0);
        boolean gainNostril =
            (!haveNostril
                && Preferences.getInteger("lastTempleButtonsUnlock")
                    != KoLCharacter.getAscensions());
        boolean templeAdvs =
            (Preferences.getInteger("lastTempleAdventures") == KoLCharacter.getAscensions());

        result[0] = "mysticality substats";
        result[1] = (gainNostril ? "gain the Nostril of the Serpent" : "skip adventure");
        result[2] = (templeAdvs ? "skip adventure" : "gain 3 adventures");
        return result;

      case 580:
        // The Hidden Heart of the Hidden Temple
        result = new Object[3];

        haveNostril = (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0);
        boolean buttonsUnconfused =
            (Preferences.getInteger("lastTempleButtonsUnlock") == KoLCharacter.getAscensions());

        if (ChoiceManager.lastResponseText.contains("door_stone.gif")) {
          result[0] = "muscle substats";
          result[1] =
              (buttonsUnconfused || haveNostril
                  ? "choose Hidden Heart adventure"
                  : "randomise Hidden Heart adventure");
          result[2] = "moxie substats and 5 turns of Somewhat poisoned";
        } else if (ChoiceManager.lastResponseText.contains("door_sun.gif")) {
          result[0] = "gain ancient calendar fragment";
          result[1] =
              (buttonsUnconfused || haveNostril
                  ? "choose Hidden Heart adventure"
                  : "randomise Hidden Heart adventure");
          result[2] = "moxie substats and 5 turns of Somewhat poisoned";
        } else if (ChoiceManager.lastResponseText.contains("door_gargoyle.gif")) {
          result[0] = "gain mana";
          result[1] =
              (buttonsUnconfused || haveNostril
                  ? "choose Hidden Heart adventure"
                  : "randomise Hidden Heart adventure");
          result[2] = "moxie substats and 5 turns of Somewhat poisoned";
        } else if (ChoiceManager.lastResponseText.contains("door_pikachu.gif")) {
          result[0] = "unlock Hidden City";
          result[1] =
              (buttonsUnconfused || haveNostril
                  ? "choose Hidden Heart adventure"
                  : "randomise Hidden Heart adventure");
          result[2] = "moxie substats and 5 turns of Somewhat poisoned";
        }

        return result;

      case 581:
        // Such Great Depths
        result = new Object[3];

        int fungus = InventoryManager.getCount(ItemPool.GLOWING_FUNGUS);

        result[0] = "gain a glowing fungus (" + fungus + ")";
        result[1] =
            (Preferences.getBoolean("_templeHiddenPower")
                ? "skip adventure"
                : "5 advs of +15 mus/mys/mox");
        result[2] = "fight clan of cave bars";
        return result;

      case 582:
        // Fitting In
        result = new Object[3];

        // mysticality substats, gain the Nostril of the Serpent or gain 3 adventures
        haveNostril = (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0);
        gainNostril =
            (!haveNostril
                && Preferences.getInteger("lastTempleButtonsUnlock")
                    != KoLCharacter.getAscensions());
        String nostrilAction = (gainNostril ? "gain the Nostril of the Serpent" : "skip adventure");

        templeAdvs =
            (Preferences.getInteger("lastTempleAdventures") == KoLCharacter.getAscensions());
        String advAction = (templeAdvs ? "skip adventure" : "gain 3 adventures");

        result[0] = "mysticality substats, " + nostrilAction + " or " + advAction;

        // Hidden Heart of the Hidden Temple
        result[1] = "Hidden Heart of the Hidden Temple";

        // gain glowing fungus, gain Hidden Power or fight a clan of cave bars
        String powerAction =
            (Preferences.getBoolean("_templeHiddenPower") ? "skip adventure" : "Hidden Power");

        result[2] = "gain a glowing fungus, " + powerAction + " or fight a clan of cave bars";

        return result;

      case 606:
        // Lost in the Great Overlook Lodge
        result = new Object[6];

        result[0] =
            "need +4 stench resist, have "
                + KoLCharacter.getElementalResistanceLevels(Element.STENCH);

        // annoyingly, the item drop check does not take into account fairy (or other sidekick)
        // bonus.
        // This is just a one-off implementation, but should be standardized somewhere in Modifiers
        // if kol adds more things like this.
        double bonus = 0;
        // Check for familiars
        if (!KoLCharacter.getFamiliar().equals(FamiliarData.NO_FAMILIAR)) {
          bonus = Modifiers.getNumericModifier(KoLCharacter.getFamiliar(), "Item Drop");
        }
        // Check for Clancy
        else if (KoLCharacter.getCurrentInstrument() != null
            && KoLCharacter.getCurrentInstrument().equals(CharPaneRequest.LUTE)) {
          int weight = 5 * KoLCharacter.getMinstrelLevel();
          bonus = Math.sqrt(55 * weight) + weight - 3;
        }
        // Check for Eggman
        else if (KoLCharacter.getCompanion() == Companion.EGGMAN) {
          bonus = KoLCharacter.hasSkill("Working Lunch") ? 75 : 50;
        }
        // Check for Cat Servant
        else if (KoLCharacter.isEd()) {
          EdServantData servant = EdServantData.currentServant();
          if (servant != null && servant.getId() == 1) {
            int level = servant.getLevel();
            if (level >= 7) {
              bonus = Math.sqrt(55 * level) + level - 3;
            }
          }
        }
        // Check for Throne
        FamiliarData throned = KoLCharacter.getEnthroned();
        if (!throned.equals(FamiliarData.NO_FAMILIAR)) {
          bonus += Modifiers.getNumericModifier("Throne", throned.getRace(), "Item Drop");
        }
        // Check for Bjorn
        FamiliarData bjorned = KoLCharacter.getBjorned();
        if (!bjorned.equals(FamiliarData.NO_FAMILIAR)) {
          bonus += Modifiers.getNumericModifier("Throne", bjorned.getRace(), "Item Drop");
        }
        // Check for Florist
        if (FloristRequest.haveFlorist()) {
          List<Florist> plants = FloristRequest.getPlants("Twin Peak");
          if (plants != null) {
            for (Florist plant : plants) {
              bonus += Modifiers.getNumericModifier("Florist", plant.toString(), "Item Drop");
            }
          }
        }
        result[1] =
            "need +50% item drop, have "
                + Math.round(
                    KoLCharacter.getItemDropPercentAdjustment()
                        + KoLCharacter.currentNumericModifier(Modifiers.FOODDROP)
                        - bonus)
                + "%";
        result[2] = new Option("need jar of oil", "jar of oil");
        result[3] = "need +40% init, have " + KoLCharacter.getInitiativeAdjustment() + "%";
        result[4] = null; // why is there a missing button 5?
        result[5] = "flee";

        return result;

      case 611:
        // The Horror... (A-Boo Peak)
        result = new Object[2];
        result[0] = ChoiceManager.booPeakDamage();
        result[1] = "Flee";
        return result;

      case 636:
      case 637:
      case 638:
      case 639:
        // Old Man psychosis choice adventures are randomized and may not include all elements.
        return oldManPsychosisSpoilers();

      case 641:
        // Stupid Pipes. (Mystic's psychoses)
        result = new Object[3];
        {
          StringBuilder buffer = new StringBuilder();
          int resistance = KoLCharacter.getElementalResistanceLevels(Element.HOT);
          int damage = (int) (2.50 * (100.0 - KoLCharacter.elementalResistanceByLevel(resistance)));
          long hp = KoLCharacter.getCurrentHP();
          buffer.append("take ");
          buffer.append(damage);
          buffer.append(" hot damage, current HP = ");
          buffer.append(hp);
          buffer.append(", current hot resistance = ");
          buffer.append(resistance);
          result[0] = buffer.toString();
        }
        result[1] = "flickering pixel";
        result[2] = "skip adventure";
        return result;

      case 642:
        // You're Freaking Kidding Me (Mystic's psychoses)
        result = new Object[3];
        {
          String buffer =
              "50 buffed Muscle/Mysticality/Moxie required, have "
                  + KoLCharacter.getAdjustedMuscle()
                  + "/"
                  + KoLCharacter.getAdjustedMysticality()
                  + "/"
                  + KoLCharacter.getAdjustedMoxie();
          result[0] = buffer;
        }
        result[1] = "flickering pixel";
        result[2] = "skip adventure";
        return result;

      case 644:
        // Snakes. (Mystic's psychoses)
        result = new Object[3];
        {
          String buffer = "50 buffed Moxie required, have " + KoLCharacter.getAdjustedMoxie();
          result[0] = buffer;
        }
        result[1] = "flickering pixel";
        result[2] = "skip adventure";
        return result;

      case 645:
        // So... Many... Skulls... (Mystic's psychoses)
        result = new Object[3];
        {
          StringBuilder buffer = new StringBuilder();
          int resistance = KoLCharacter.getElementalResistanceLevels(Element.SPOOKY);
          int damage = (int) (2.50 * (100.0 - KoLCharacter.elementalResistanceByLevel(resistance)));
          long hp = KoLCharacter.getCurrentHP();
          buffer.append("take ");
          buffer.append(damage);
          buffer.append(" spooky damage, current HP = ");
          buffer.append(hp);
          buffer.append(", current spooky resistance = ");
          buffer.append(resistance);
          result[0] = buffer.toString();
        }
        result[1] = "flickering pixel";
        result[2] = "skip adventure";
        return result;

      case 647:
        // A Stupid Dummy. Also, a Straw Man. (Mystic's psychoses)
        result = new Object[3];
        {
          StringBuilder buffer = new StringBuilder();
          String current = String.valueOf(KoLCharacter.currentBonusDamage());
          buffer.append("100 weapon damage required");
          result[0] = buffer.toString();
        }
        result[1] = "flickering pixel";
        result[2] = "skip adventure";
        return result;

      case 648:
        // Slings and Arrows (Mystic's psychoses)
        result = new Object[3];
        {
          String buffer = "101 HP required, have " + KoLCharacter.getCurrentHP();
          result[0] = buffer;
        }
        result[1] = "flickering pixel";
        result[2] = "skip adventure";
        return result;

      case 650:
        // This Is Your Life. Your Horrible, Horrible Life. (Mystic's psychoses)
        result = new Object[3];
        {
          String buffer = "101 MP required, have " + KoLCharacter.getCurrentMP();
          result[0] = buffer;
        }
        result[1] = "flickering pixel";
        result[2] = "skip adventure";
        return result;

      case 651:
        // The Wall of Wailing (Mystic's psychoses)
        result = new Object[3];
        {
          String buffer =
              "10 prismatic damage required, have " + KoLCharacter.currentPrismaticDamage();
          result[0] = buffer;
        }
        result[1] = "flickering pixel";
        result[2] = "skip adventure";
        return result;

      case 669:
        // The Fast and the Furry-ous
        result = new Object[4];
        result[0] =
            KoLCharacter.hasEquipped(ItemPool.get(ItemPool.TITANIUM_UMBRELLA, 1))
                ? "open Ground Floor (titanium umbrella equipped)"
                : "Neckbeard Choice (titanium umbrella not equipped)";
        result[1] = "200 Moxie substats";
        result[2] = "";
        result[3] = "skip adventure and guarantees this adventure will reoccur";
        return result;

      case 670:
        // You Don't Mess Around with Gym
        result = new Object[5];
        result[0] = "massive dumbbell, then skip adventure";
        result[1] = "200 Muscle substats";
        result[2] = "pec oil, giant jar of protein powder, Squat-Thrust Magazine";
        result[3] =
            KoLCharacter.hasEquipped(ItemPool.get(ItemPool.EXTREME_AMULET, 1))
                ? "open Ground Floor (amulet equipped)"
                : "skip adventure (amulet not equipped)";
        result[4] = "skip adventure and guarantees this adventure will reoccur";
        return result;

      case 678:
        // Yeah, You're for Me, Punk Rock Giant
        result = new Object[4];
        result[0] =
            KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MOHAWK_WIG, 1))
                ? "Finish quest (mohawk wig equipped)"
                : "Fight Punk Rock Giant (mohawk wig not equipped)";
        result[1] = "500 meat";
        result[2] = "Steampunk Choice";
        result[3] = "Raver Choice";
        return result;

      case 692:
        // I Wanna Be a Door
        result = new String[9];
        result[0] = "suffer trap effects";
        result[1] = "unlock door with key, no turn spent";
        result[2] = "pick lock with lockpicks, no turn spent";
        result[3] =
            KoLCharacter.getAdjustedMuscle() >= 30
                ? "bypass trap with muscle"
                : "suffer trap effects";
        result[4] =
            KoLCharacter.getAdjustedMysticality() >= 30
                ? "bypass trap with mysticality"
                : "suffer trap effects";
        result[5] =
            KoLCharacter.getAdjustedMoxie() >= 30
                ? "bypass trap with moxie"
                : "suffer trap effects";
        result[6] = "open door with card, no turn spent";
        result[7] = "leave, no turn spent";
        return result;

      case 696:
        // Stick a Fork In It
        result = new String[2];
        result[0] =
            Preferences.getBoolean("maraisDarkUnlock")
                ? "Dark and Spooky Swamp already unlocked"
                : "unlock Dark and Spooky Swamp";
        result[1] =
            Preferences.getBoolean("maraisWildlifeUnlock")
                ? "The Wildlife Sanctuarrrrrgh already unlocked"
                : "unlock The Wildlife Sanctuarrrrrgh";
        return result;

      case 697:
        // Sophie's Choice
        result = new String[2];
        result[0] =
            Preferences.getBoolean("maraisCorpseUnlock")
                ? "The Corpse Bog already unlocked"
                : "unlock The Corpse Bog";
        result[1] =
            Preferences.getBoolean("maraisWizardUnlock")
                ? "The Ruined Wizard Tower already unlocked"
                : "unlock The Ruined Wizard Tower";
        return result;

      case 698:
        // From Bad to Worst
        result = new String[2];
        result[0] =
            Preferences.getBoolean("maraisBeaverUnlock")
                ? "Swamp Beaver Territory already unlocked"
                : "unlock Swamp Beaver Territory";
        result[1] =
            Preferences.getBoolean("maraisVillageUnlock")
                ? "The Weird Swamp Village already unlocked"
                : "unlock The Weird Swamp Village";
        return result;

      case 700:
        // Delirium in the Cafeteria
        result = new String[9];
        result[0] =
            KoLConstants.activeEffects.contains(ChoiceManager.JOCK_EFFECT)
                ? "Gain stats"
                : "Lose HP";
        result[1] =
            KoLConstants.activeEffects.contains(ChoiceManager.NERD_EFFECT)
                ? "Gain stats"
                : "Lose HP";
        result[2] =
            KoLConstants.activeEffects.contains(ChoiceManager.GREASER_EFFECT)
                ? "Gain stats"
                : "Lose HP";
        return result;

      case 721:
        {
          // The Cabin in the Dreadsylvanian Woods

          result = new Object[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("dread tarragon");
          if (KoLCharacter.isMuscleClass()) {
            buffer.append(", old dry bone (");
            buffer.append(InventoryManager.getCount(ItemPool.OLD_DRY_BONE));
            buffer.append(") -> bone flour");
          }
          buffer.append(", -stench");
          result[0] = buffer.toString(); // The Kitchen

          buffer.setLength(0);
          buffer.append("Freddies");
          buffer.append(", Bored Stiff (+100 spooky damage)");
          buffer.append(", replica key (");
          buffer.append(InventoryManager.getCount(ItemPool.REPLICA_KEY));
          buffer.append(") -> Dreadsylvanian auditor's badge");
          buffer.append(", wax banana (");
          buffer.append(InventoryManager.getCount(ItemPool.WAX_BANANA));
          buffer.append(") -> complicated lock impression");
          result[1] = buffer.toString(); // The Cellar

          buffer.setLength(0);
          ChoiceManager.lockSpoiler(buffer);
          buffer.append("-spooky");
          if (KoLCharacter.isAccordionThief()) {
            buffer.append(" + intricate music box parts");
          }
          buffer.append(", fewer werewolves");
          buffer.append(", fewer vampires");
          buffer.append(", +Moxie");
          result[2] = buffer.toString(); // The Attic (locked)

          result[4] = ChoiceManager.shortcutSpoiler("ghostPencil1");
          result[5] = "Leave this noncombat";
          return result;
        }

      case 722:
        // The Kitchen in the Woods
        result = new Object[6];
        result[0] = "dread tarragon";
        result[1] =
            "old dry bone (" + InventoryManager.getCount(ItemPool.OLD_DRY_BONE) + ") -> bone flour";
        result[2] = "-stench";
        result[5] = "Return to The Cabin";
        return result;

      case 723:
        // What Lies Beneath (the Cabin)
        result = new Object[6];
        result[0] = "Freddies";
        result[1] = "Bored Stiff (+100 spooky damage)";
        result[2] =
            "replica key ("
                + InventoryManager.getCount(ItemPool.REPLICA_KEY)
                + ") -> Dreadsylvanian auditor's badge";
        result[3] =
            "wax banana ("
                + InventoryManager.getCount(ItemPool.WAX_BANANA)
                + ") -> complicated lock impression";
        result[5] = "Return to The Cabin";
        return result;

      case 724:
        // Where it's Attic
        result = new Object[6];
        result[0] =
            "-spooky" + (KoLCharacter.isAccordionThief() ? " + intricate music box parts" : "");
        result[1] = "fewer werewolves";
        result[2] = "fewer vampires";
        result[3] = "+Moxie";
        result[5] = "Return to The Cabin";
        return result;

      case 725:
        {
          // Tallest Tree in the Forest

          result = new Object[6];

          StringBuilder buffer = new StringBuilder();
          if (KoLCharacter.isMuscleClass()) {
            buffer.append("drop blood kiwi");
            buffer.append(", -sleaze");
            buffer.append(", moon-amber");
          } else {
            buffer.append("unavailable (Muscle class only)");
          }
          result[0] = buffer.toString(); // Climb tree (muscle only)

          buffer.setLength(0);
          ChoiceManager.lockSpoiler(buffer);
          buffer.append("fewer ghosts");
          buffer.append(", Freddies");
          buffer.append(", +Muscle");
          result[1] = buffer.toString(); // Fire Tower (locked)

          buffer.setLength(0);
          buffer.append("blood kiwi (from above)");
          buffer.append(", Dreadsylvanian seed pod");
          if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.FOLDER_HOLDER, 1))) {
            buffer.append(", folder (owl)");
          }

          result[2] = buffer.toString(); // Base of tree

          result[4] = ChoiceManager.shortcutSpoiler("ghostPencil2");
          result[5] = "Leave this noncombat";
          return result;
        }

      case 726:
        // Top of the Tree, Ma!
        result = new Object[6];
        result[0] = "drop blood kiwi";
        result[1] = "-sleaze";
        result[2] = "moon-amber";
        result[5] = "Return to The Tallest Tree";
        return result;

      case 727:
        // All Along the Watchtower
        result = new Object[6];
        result[0] = "fewer ghosts";
        result[1] = "Freddies";
        result[2] = "+Muscle";
        result[5] = "Return to The Tallest Tree";
        return result;

      case 728:
        // Treebasing
        result = new Object[6];
        result[0] = "blood kiwi (from above)";
        result[1] = "Dreadsylvanian seed pod";
        result[2] = "folder (owl)";
        result[5] = "Return to The Tallest Tree";
        return result;

      case 729:
        {
          // Below the Roots

          result = new Object[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("-hot");
          buffer.append(", Dragged Through the Coals (+100 hot damage)");
          buffer.append(", old ball and chain (");
          buffer.append(InventoryManager.getCount(ItemPool.OLD_BALL_AND_CHAIN));
          buffer.append(") -> cool iron ingot");
          result[0] = buffer.toString(); // Hot

          buffer.setLength(0);
          buffer.append("-cold");
          buffer.append(", +Mysticality");
          buffer.append(", Nature's Bounty (+300 max HP)");
          result[1] = buffer.toString(); // Cold

          buffer.setLength(0);
          buffer.append("fewer bugbears");
          buffer.append(", Freddies");
          result[2] = buffer.toString(); // Smelly

          result[4] = ChoiceManager.shortcutSpoiler("ghostPencil3");
          result[5] = "Leave this noncombat";
          return result;
        }

      case 730:
        // Hot Coals
        result = new Object[6];
        result[0] = "-hot";
        result[1] = "Dragged Through the Coals (+100 hot damage)";
        result[2] =
            "old ball and chain ("
                + InventoryManager.getCount(ItemPool.OLD_BALL_AND_CHAIN)
                + ") -> cool iron ingot";
        result[5] = "Return to The Burrows";
        return result;

      case 731:
        // The Heart of the Matter
        result = new Object[6];
        result[0] = "-cold";
        result[1] = "+Mysticality";
        result[2] = "Nature's Bounty (+300 max HP)";
        result[5] = "Return to The Burrows";
        return result;

      case 732:
        // Once Midden, Twice Shy
        result = new Object[6];
        result[0] = "fewer bugbears";
        result[1] = "Freddies";
        result[5] = "Return to The Burrows";
        return result;

      case 733:
        {
          // Dreadsylvanian Village Square

          result = new Object[6];

          StringBuilder buffer = new StringBuilder();
          ChoiceManager.lockSpoiler(buffer);
          buffer.append("fewer ghosts");
          buffer.append(", ghost pencil");
          buffer.append(", +Mysticality");
          result[0] = buffer.toString(); // Schoolhouse (locked)

          buffer.setLength(0);
          buffer.append("-cold");
          buffer.append(", Freddies");
          if (InventoryManager.getCount(ItemPool.HOTHAMMER) > 0) {
            buffer.append(", cool iron ingot (");
            buffer.append(InventoryManager.getCount(ItemPool.COOL_IRON_INGOT));
            buffer.append(") + warm fur (");
            buffer.append(InventoryManager.getCount(ItemPool.WARM_FUR));
            buffer.append(") -> cooling iron equipment");
          }
          result[1] = buffer.toString(); // Blacksmith

          buffer.setLength(0);
          buffer.append("-spooky");
          buffer.append(", gain ");
          String item =
              KoLCharacter.isMuscleClass()
                  ? "hangman's hood"
                  : KoLCharacter.isMysticalityClass()
                      ? "cursed ring finger ring"
                      : KoLCharacter.isMoxieClass() ? "Dreadsylvanian clockwork key" : "nothing";
          buffer.append(item);
          buffer.append(" with help of clannie");
          buffer.append(" or help clannie gain an item");
          result[2] = buffer.toString(); // Gallows

          result[4] = ChoiceManager.shortcutSpoiler("ghostPencil4");
          result[5] = "Leave this noncombat";
          return result;
        }

      case 734:
        // Fright School
        result = new Object[6];
        result[0] = "fewer ghosts";
        result[1] = "ghost pencil";
        result[2] = "+Mysticality";
        result[5] = "Return to The Village Square";
        return result;

      case 735:
        // Smith, Black as Night
        result = new Object[6];
        result[0] = "-cold";
        result[1] = "Freddies";
        result[2] =
            "cool iron ingot ("
                + InventoryManager.getCount(ItemPool.COOL_IRON_INGOT)
                + ") + warm fur ("
                + InventoryManager.getCount(ItemPool.WARM_FUR)
                + ") -> cooling iron equipment";
        result[5] = "Return to The Village Square";
        return result;

      case 736:
        // Gallows
        result = new Object[6];
        result[0] = "-spooky ";
        result[1] =
            "gain "
                + (KoLCharacter.isMuscleClass()
                    ? "hangman's hood"
                    : KoLCharacter.isMysticalityClass()
                        ? "cursed ring finger ring"
                        : KoLCharacter.isMoxieClass() ? "Dreadsylvanian clockwork key" : "nothing")
                + " with help of clannie";
        result[3] = "help clannie gain an item";
        result[5] = "Return to The Village Square";
        return result;

      case 737:
        {
          // The Even More Dreadful Part of Town

          result = new Object[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("-stench");
          buffer.append(", Sewer-Drenched (+100 stench damage)");
          result[0] = buffer.toString(); // Sewers

          buffer.setLength(0);
          buffer.append("fewer skeletons");
          buffer.append(", -sleaze");
          buffer.append(", +Muscle");
          result[1] = buffer.toString(); // Tenement

          buffer.setLength(0);
          if (KoLCharacter.isMoxieClass()) {
            buffer.append("Freddies");
            buffer.append(", lock impression (");
            buffer.append(InventoryManager.getCount(ItemPool.WAX_LOCK_IMPRESSION));
            buffer.append(") + music box parts (");
            buffer.append(InventoryManager.getCount(ItemPool.INTRICATE_MUSIC_BOX_PARTS));
            buffer.append(") -> replica key");
            buffer.append(", moon-amber (");
            buffer.append(InventoryManager.getCount(ItemPool.MOON_AMBER));
            buffer.append(") -> polished moon-amber");
            buffer.append(", 3 music box parts (");
            buffer.append(InventoryManager.getCount(ItemPool.INTRICATE_MUSIC_BOX_PARTS));
            buffer.append(") + clockwork key (");
            buffer.append(InventoryManager.getCount(ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY));
            buffer.append(") -> mechanical songbird");
            buffer.append(", 3 lengths of old fuse");
          } else {
            buffer.append("unavailable (Moxie class only)");
          }
          result[2] = buffer.toString(); // Ticking Shack (moxie only)

          result[4] = ChoiceManager.shortcutSpoiler("ghostPencil5");
          result[5] = "Leave this noncombat";
          return result;
        }

      case 738:
        // A Dreadful Smell
        result = new Object[6];
        result[0] = "-stench";
        result[1] = "Sewer-Drenched (+100 stench damage)";
        result[5] = "Return to Skid Row";
        return result;

      case 739:
        // The Tinker's. Damn.
        result = new Object[6];
        result[0] = "Freddies";
        result[1] =
            "lock impression ("
                + InventoryManager.getCount(ItemPool.WAX_LOCK_IMPRESSION)
                + ") + music box parts ("
                + InventoryManager.getCount(ItemPool.INTRICATE_MUSIC_BOX_PARTS)
                + ") -> replica key";
        result[2] =
            "moon-amber ("
                + InventoryManager.getCount(ItemPool.MOON_AMBER)
                + ") -> polished moon-amber";
        result[3] =
            "3 music box parts ("
                + InventoryManager.getCount(ItemPool.INTRICATE_MUSIC_BOX_PARTS)
                + ") + clockwork key ("
                + InventoryManager.getCount(ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY)
                + ") -> mechanical songbird";
        result[4] = "3 lengths of old fuse";
        result[5] = "Return to Skid Row";
        return result;

      case 740:
        // Eight, Nine, Tenement
        result = new Object[6];
        result[0] = "fewer skeletons";
        result[1] = "-sleaze";
        result[2] = "+Muscle";
        result[5] = "Return to Skid Row";
        return result;

      case 741:
        {
          // The Old Duke's Estate

          result = new Object[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("fewer zombies");
          buffer.append(", Freddies");
          buffer.append(", Fifty Ways to Bereave Your Lover (+100 sleaze damage)");
          result[0] = buffer.toString(); // Cemetery

          buffer.setLength(0);
          buffer.append("-hot");
          if (KoLCharacter.isMysticalityClass()) {
            buffer.append(", dread tarragon (");
            buffer.append(InventoryManager.getCount(ItemPool.DREAD_TARRAGON));
            buffer.append(") + dreadful roast (");
            buffer.append(InventoryManager.getCount(ItemPool.DREADFUL_ROAST));
            buffer.append(") + bone flour (");
            buffer.append(InventoryManager.getCount(ItemPool.BONE_FLOUR));
            buffer.append(") + stinking agaricus (");
            buffer.append(InventoryManager.getCount(ItemPool.STINKING_AGARICUS));
            buffer.append(") -> Dreadsylvanian shepherd's pie");
          }
          buffer.append(", +Moxie");
          result[1] = buffer.toString(); // Servants' Quarters

          buffer.setLength(0);
          ChoiceManager.lockSpoiler(buffer);
          buffer.append("fewer werewolves");
          buffer.append(", eau de mort");
          buffer.append(", 10 ghost thread (");
          buffer.append(InventoryManager.getCount(ItemPool.GHOST_THREAD));
          buffer.append(") -> ghost shawl");
          result[2] = buffer.toString(); // Master Suite (locked)

          result[4] = ChoiceManager.shortcutSpoiler("ghostPencil6");
          result[5] = "Leave this noncombat";
          return result;
        }

      case 742:
        // The Plot Thickens
        result = new Object[6];
        result[0] = "fewer zombies";
        result[1] = "Freddies";
        result[2] = "Fifty Ways to Bereave Your Lover (+100 sleaze damage)";
        result[5] = "Return to The Old Duke's Estate";
        return result;

      case 743:
        // No Quarter
        result = new Object[6];
        result[0] = "-hot";
        result[1] =
            "dread tarragon ("
                + InventoryManager.getCount(ItemPool.DREAD_TARRAGON)
                + ") + dreadful roast ("
                + InventoryManager.getCount(ItemPool.DREADFUL_ROAST)
                + ") + bone flour ("
                + InventoryManager.getCount(ItemPool.BONE_FLOUR)
                + ") + stinking agaricus ("
                + InventoryManager.getCount(ItemPool.STINKING_AGARICUS)
                + ") -> Dreadsylvanian shepherd's pie";
        result[2] = "+Moxie";
        result[5] = "Return to The Old Duke's Estate";
        return result;

      case 744:
        // The Master Suite -- Sweet!
        result = new Object[6];
        result[0] = "fewer werewolves";
        result[1] = "eau de mort";
        result[2] =
            "10 ghost thread ("
                + InventoryManager.getCount(ItemPool.GHOST_THREAD)
                + ") -> ghost shawl";
        result[5] = "Return to The Old Duke's Estate";
        return result;

      case 745:
        {
          // This Hall is Really Great

          result = new Object[6];

          StringBuilder buffer = new StringBuilder();
          ChoiceManager.lockSpoiler(buffer);
          buffer.append("fewer vampires");
          buffer.append(", ");
          if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MUDDY_SKIRT, 1))) {
            buffer.append("equipped muddy skirt -> weedy skirt and ");
          } else if (InventoryManager.getCount(ItemPool.MUDDY_SKIRT) > 0) {
            buffer.append("(muddy skirt in inventory but not equipped) ");
          }
          buffer.append("+Moxie");
          result[0] = buffer.toString(); // Ballroom (locked)

          buffer.setLength(0);
          buffer.append("-cold");
          buffer.append(", Staying Frosty (+100 cold damage)");
          result[1] = buffer.toString(); // Kitchen

          buffer.setLength(0);
          buffer.append("dreadful roast");
          buffer.append(", -stench");
          if (KoLCharacter.isMysticalityClass()) {
            buffer.append(", wax banana");
          }
          result[2] = buffer.toString(); // Dining Room

          result[4] = ChoiceManager.shortcutSpoiler("ghostPencil7");
          result[5] = "Leave this noncombat";
          return result;
        }

      case 746:
        // The Belle of the Ballroom
        result = new Object[6];
        result[0] = "fewer vampires";
        result[1] =
            (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.MUDDY_SKIRT, 1))
                    ? "equipped muddy skirt -> weedy skirt and "
                    : InventoryManager.getCount(ItemPool.MUDDY_SKIRT) > 0
                        ? "(muddy skirt in inventory but not equipped) "
                        : "")
                + "+Moxie";
        result[5] = "Return to The Great Hall";
        return result;

      case 747:
        // Cold Storage
        result = new Object[6];
        result[0] = "-cold";
        result[1] = "Staying Frosty (+100 cold damage)";
        result[5] = "Return to The Great Hall";
        return result;

      case 748:
        // Dining In (the Castle)
        result = new Object[6];
        result[0] = "dreadful roast";
        result[1] = "-stench";
        result[2] = "wax banana";
        result[5] = "Return to The Great Hall";
        return result;

      case 749:
        {
          // Tower Most Tall

          result = new Object[6];

          StringBuilder buffer = new StringBuilder();
          ChoiceManager.lockSpoiler(buffer);
          buffer.append("fewer bugbears");
          buffer.append(", fewer zombies");
          buffer.append(", visit The Machine");
          if (KoLCharacter.isMoxieClass()) {
            buffer.append(", blood kiwi (");
            buffer.append(InventoryManager.getCount(ItemPool.BLOOD_KIWI));
            buffer.append(") + eau de mort (");
            buffer.append(InventoryManager.getCount(ItemPool.EAU_DE_MORT));
            buffer.append(") -> bloody kiwitini");
          }
          result[0] = buffer.toString(); // Laboratory (locked)

          buffer.setLength(0);
          if (KoLCharacter.isMysticalityClass()) {
            buffer.append("fewer skeletons");
            buffer.append(", +Mysticality");
            buffer.append(", learn recipe for moon-amber necklace");
          } else {
            buffer.append("unavailable (Mysticality class only)");
          }
          result[1] = buffer.toString(); // Books (mysticality only)

          buffer.setLength(0);
          buffer.append("-sleaze");
          buffer.append(", Freddies");
          buffer.append(", Magically Fingered (+150 max MP, 40-50 MP regen)");
          result[2] = buffer.toString(); // Bedroom

          result[4] = ChoiceManager.shortcutSpoiler("ghostPencil8");
          result[5] = "Leave this noncombat";
          return result;
        }

      case 750:
        // Working in the Lab, Late One Night
        result = new Object[6];
        result[0] = "fewer bugbears";
        result[1] = "fewer zombies";
        result[2] = "visit The Machine";
        result[3] =
            "blood kiwi ("
                + InventoryManager.getCount(ItemPool.BLOOD_KIWI)
                + ") + eau de mort ("
                + InventoryManager.getCount(ItemPool.EAU_DE_MORT)
                + ") -> bloody kiwitini";
        result[5] = "Return to The Tower";
        return result;

      case 751:
        // Among the Quaint and Curious Tomes.
        result = new Object[6];
        result[0] = "fewer skeletons";
        result[1] = "+Mysticality";
        result[2] = "learn recipe for moon-amber necklace";
        result[5] = "Return to The Tower";
        return result;

      case 752:
        // In The Boudoir
        result = new Object[6];
        result[0] = "-sleaze";
        result[1] = "Freddies";
        result[2] = "Magically Fingered (+150 max MP, 40-50 MP regen)";
        result[5] = "Return to The Tower";
        return result;

      case 753:
        {
          // The Dreadsylvanian Dungeon

          result = new Object[6];

          StringBuilder buffer = new StringBuilder();
          buffer.append("-spooky");
          buffer.append(", +Muscle");
          buffer.append(", +MP");
          result[0] = buffer.toString(); // Prison

          buffer.setLength(0);
          buffer.append("-hot");
          buffer.append(", Freddies");
          buffer.append(", +Muscle/Mysticality/Moxie");
          result[1] = buffer.toString(); // Boiler Room

          buffer.setLength(0);
          buffer.append("stinking agaricus");
          buffer.append(", Spore-wreathed (reduce enemy defense by 20%)");
          result[2] = buffer.toString(); // Guard room

          result[4] = ChoiceManager.shortcutSpoiler("ghostPencil9");
          result[5] = "Leave this noncombat";
          return result;
        }

      case 754:
        // Live from Dungeon Prison
        result = new Object[6];
        result[0] = "-spooky";
        result[1] = "+Muscle";
        result[2] = "+MP";
        result[5] = "Return to The Dungeons";
        return result;

      case 755:
        // The Hot Bowels
        result = new Object[6];
        result[0] = "-hot";
        result[1] = "Freddies";
        result[2] = "+Muscle/Mysticality/Moxie";
        result[5] = "Return to The Dungeons";
        return result;

      case 756:
        // Among the Fungus
        result = new Object[6];
        result[0] = "stinking agaricus";
        result[1] = "Spore-wreathed (reduce enemy defense by 20%)";
        result[5] = "Return to The Dungeons";
        return result;

      case 758:
        {
          // End of the Path

          StringBuilder buffer = new StringBuilder();
          boolean necklaceEquipped = KoLCharacter.hasEquipped(ChoiceManager.MOON_AMBER_NECKLACE);
          boolean necklaceAvailable =
              InventoryManager.getCount(ChoiceManager.MOON_AMBER_NECKLACE) > 0;
          boolean hasKiwiEffect =
              KoLConstants.activeEffects.contains(ChoiceManager.KIWITINI_EFFECT);
          boolean isBlind =
              KoLConstants.activeEffects.contains(ChoiceManager.TEMPORARY_BLINDNESS)
                  || KoLCharacter.hasEquipped(ChoiceManager.MAKESHIFT_TURBAN)
                  || KoLCharacter.hasEquipped(ChoiceManager.HELPS_YOU_SLEEP)
                  || KoLCharacter.hasEquipped(ChoiceManager.SLEEP_MASK);
          boolean kiwitiniAvailable = InventoryManager.getCount(ChoiceManager.BLOODY_KIWITINI) > 0;

          buffer.append(
              necklaceEquipped
                  ? "moon-amber necklace equipped"
                  : necklaceAvailable
                      ? "moon-amber necklace NOT equipped but in inventory"
                      : "moon-amber necklace neither equipped nor available");
          buffer.append(" / ");
          buffer.append(
              hasKiwiEffect
                  ? (isBlind ? "First Blood Kiwi and blind" : "First Blood Kiwi but NOT blind")
                  : kiwitiniAvailable
                      ? "bloody kiwitini in inventory"
                      : "First Blood Kiwi neither active nor available");

          result = new Object[2];
          result[0] = buffer.toString();
          result[1] = "Run away";
          return result;
        }

      case 759:
        {
          // You're About to Fight City Hall

          StringBuilder buffer = new StringBuilder();
          boolean badgeEquipped = KoLCharacter.hasEquipped(ChoiceManager.AUDITORS_BADGE);
          boolean badgeAvailable = InventoryManager.getCount(ChoiceManager.AUDITORS_BADGE) > 0;
          boolean skirtEquipped = KoLCharacter.hasEquipped(ChoiceManager.WEEDY_SKIRT);
          boolean skirtAvailable = InventoryManager.getCount(ChoiceManager.WEEDY_SKIRT) > 0;

          buffer.append(
              badgeEquipped
                  ? "Dreadsylvanian auditor's badge equipped"
                  : badgeAvailable
                      ? "Dreadsylvanian auditor's badge NOT equipped but in inventory"
                      : "Dreadsylvanian auditor's badge neither equipped nor available");
          buffer.append(" / ");
          buffer.append(
              skirtEquipped
                  ? "weedy skirt equipped"
                  : skirtAvailable
                      ? "weedy skirt NOT equipped but in inventory"
                      : "weedy skirt neither equipped nor available");

          result = new Object[2];
          result[0] = buffer.toString();
          result[1] = "Run away";
          return result;
        }

      case 760:
        {
          // Holding Court

          StringBuilder buffer = new StringBuilder();
          boolean shawlEquipped = KoLCharacter.hasEquipped(ChoiceManager.GHOST_SHAWL);
          boolean shawlAvailable = InventoryManager.getCount(ChoiceManager.GHOST_SHAWL) > 0;
          boolean hasPieEffect = KoLConstants.activeEffects.contains(ChoiceManager.PIE_EFFECT);
          boolean pieAvailable = InventoryManager.getCount(ChoiceManager.SHEPHERDS_PIE) > 0;

          buffer.append(
              shawlEquipped
                  ? "ghost shawl equipped"
                  : shawlAvailable
                      ? "ghost shawl NOT equipped but in inventory"
                      : "ghost shawl neither equipped nor available");
          buffer.append(" / ");
          buffer.append(
              hasPieEffect
                  ? "Shepherd's Breath active"
                  : pieAvailable
                      ? "Dreadsylvanian shepherd's pie in inventory"
                      : "Shepherd's Breath neither active nor available");

          result = new Object[2];
          result[0] = buffer.toString();
          result[1] = "Run away";
          return result;
        }

      case 772:
        {
          // Saved by the Bell

          // If you reach this encounter and Mafia things you've not spend 40 adventures in KOL High
          // school, correct this
          Preferences.setInteger("_kolhsAdventures", 40);

          result = new String[10];
          String buffer =
              "Get "
                  + (Preferences.getInteger("kolhsTotalSchoolSpirited") + 1) * 10
                  + " turns of School Spirited (+100% Meat drop, +50% Item drop)";
          result[0] =
              Preferences.getBoolean("_kolhsSchoolSpirited")
                  ? "Already got School Spirited today"
                  : buffer;
          result[1] =
              Preferences.getBoolean("_kolhsPoeticallyLicenced")
                  ? "Already got Poetically Licenced today"
                  : "50 turns of Poetically Licenced (+20% Myst, -20% Muscle, +2 Myst stats/fight, +10% Spell damage)";
          result[2] =
              InventoryManager.getCount(ItemPool.YEARBOOK_CAMERA) > 0
                      || KoLCharacter.hasEquipped(ItemPool.get(ItemPool.YEARBOOK_CAMERA, 1))
                  ? "Turn in yesterday's photo (if you have it)"
                  : "Get Yearbook Camera";
          result[3] =
              Preferences.getBoolean("_kolhsCutButNotDried")
                  ? "Already got Cut But Not Dried today"
                  : "50 turns of Cut But Not Dried (+20% Muscle, -20% Moxie, +2 Muscle stats/fight, +10% Weapon damage)";
          result[4] =
              Preferences.getBoolean("_kolhsIsskayLikeAnAshtray")
                  ? "Already got Isskay Like An Ashtray today"
                  : "50 turns of Isskay Like An Ashtray (+20% Moxie, -20% Myst, +2 Moxie stats/fight, +10% Pickpocket chance)";
          result[5] = "Make items";
          result[6] = "Make items";
          result[7] = "Make items";
          result[9] = "Leave";
          return result;
        }

      case 780:
        {
          // Action Elevator

          int hiddenApartmentProgress = Preferences.getInteger("hiddenApartmentProgress");
          boolean hasOnceCursed = KoLConstants.activeEffects.contains(ChoiceManager.CURSE1_EFFECT);
          boolean hasTwiceCursed = KoLConstants.activeEffects.contains(ChoiceManager.CURSE2_EFFECT);
          boolean hasThriceCursed =
              KoLConstants.activeEffects.contains(ChoiceManager.CURSE3_EFFECT);
          boolean pygmyLawyersRelocated =
              Preferences.getInteger("relocatePygmyLawyer") == KoLCharacter.getAscensions();

          result = new String[6];
          result[0] =
              (hiddenApartmentProgress >= 7
                  ? "penthouse empty"
                  : hasThriceCursed
                      ? "Fight ancient protector spirit"
                      : "Need Thrice-Cursed to fight ancient protector spirit");
          result[1] =
              (hasThriceCursed
                  ? "Increase Thrice-Cursed"
                  : hasTwiceCursed
                      ? "Get Thrice-Cursed"
                      : hasOnceCursed ? "Get Twice-Cursed" : "Get Once-Cursed");
          result[2] =
              (pygmyLawyersRelocated
                  ? "Waste adventure"
                  : "Relocate pygmy witch lawyers to Hidden Park");
          result[5] = "skip adventure";
          return result;
        }

      case 781:
        // Earthbound and Down
        result = new String[6];
        result[0] = "Unlock Hidden Apartment Building";
        result[1] = "Get stone triangle";
        result[2] = "Get Blessing of Bulbazinalli";
        result[5] = "skip adventure";
        return result;

      case 783:
        // Water You Dune
        result = new String[6];
        result[0] = "Unlock Hidden Hospital";
        result[1] = "Get stone triangle";
        result[2] = "Get Blessing of Squirtlcthulli";
        result[5] = "skip adventure";
        return result;

      case 784:
        // You, M. D.
        result = new String[6];
        result[0] = "Fight ancient protector spirit";
        result[5] = "skip adventure";
        return result;

      case 785:
        // Air Apparent
        result = new String[6];
        result[0] = "Unlock Hidden Office Building";
        result[1] = "Get stone triangle";
        result[2] = "Get Blessing of Pikachutlotal";
        result[5] = "skip adventure";
        return result;

      case 786:
        {
          // Working Holiday

          int hiddenOfficeProgress = Preferences.getInteger("hiddenOfficeProgress");
          boolean hasBossUnlock = hiddenOfficeProgress >= 6;
          boolean hasMcCluskyFile = InventoryManager.getCount(ChoiceManager.MCCLUSKY_FILE) > 0;
          boolean hasBinderClip = InventoryManager.getCount(ChoiceManager.BINDER_CLIP) > 0;

          result = new String[6];
          result[0] =
              (hiddenOfficeProgress >= 7
                  ? "office empty"
                  : hasMcCluskyFile || hasBossUnlock
                      ? "Fight ancient protector spirit"
                      : "Need McClusky File (complete) to fight ancient protector spirit");
          result[1] =
              (hasBinderClip || hasMcCluskyFile || hasBossUnlock)
                  ? "Get random item"
                  : "Get boring binder clip";
          result[2] = "Fight pygmy witch accountant";
          result[5] = "skip adventure";
          return result;
        }

      case 787:
        // Fire when Ready
        result = new String[6];
        result[0] = "Unlock Hidden Bowling Alley";
        result[1] = "Get stone triangle";
        result[2] = "Get Blessing of Charcoatl";
        result[5] = "skip adventure";
        return result;

      case 788:
        {
          // Life is Like a Cherry of Bowls
          int hiddenBowlingAlleyProgress = Preferences.getInteger("hiddenBowlingAlleyProgress");

          StringBuilder buffer = new StringBuilder();
          buffer.append("Get stats, on 5th visit, fight ancient protector spirit (");
          buffer.append((6 - hiddenBowlingAlleyProgress));
          buffer.append(" visit");
          if (hiddenBowlingAlleyProgress < 5) {
            buffer.append("s");
          }
          buffer.append(" left");

          result = new String[6];
          result[0] =
              (hiddenBowlingAlleyProgress > 6
                  ? "Get stats"
                  : hiddenBowlingAlleyProgress == 6
                      ? "fight ancient protector spirit"
                      : buffer.toString());
          result[5] = "skip adventure";
          return result;
        }

      case 789:
        {
          boolean pygmyJanitorsRelocated =
              Preferences.getInteger("relocatePygmyJanitor") == KoLCharacter.getAscensions();

          // Where Does The Lone Ranger Take His Garbagester?
          result = new String[6];
          result[0] = "Get random items";
          result[1] =
              (pygmyJanitorsRelocated
                  ? "Waste adventure"
                  : "Relocate pygmy janitors to Hidden Park");
          result[5] = "skip adventure";
          return result;
        }

      case 791:
        {
          // Legend of the Temple in the Hidden City

          int stoneTriangles = InventoryManager.getCount(ChoiceManager.STONE_TRIANGLE);

          result = new String[6];
          String buffer =
              "Need 4 stone triangles to fight Protector Spectre (" + stoneTriangles + ")";
          result[0] = (stoneTriangles == 4 ? "fight Protector Spectre" : buffer);
          result[5] = "skip adventure";
          return result;
        }

      case 801:

        // A Reanimated Conversation
        result = new String[7];
        result[0] = "skulls increase meat drops";
        result[1] = "arms deal extra damage";
        result[2] = "legs increase item drops";
        result[3] = "wings sometimes delevel at start of combat";
        result[4] = "weird parts sometimes block enemy attacks";
        result[5] = "get rid of all collected parts";
        result[6] = "no changes";
        return result;

      case 918:

        // Yachtzee
        result = new String[3];
        // Is it 7 or more days since the last time you got the Ultimate Mind Destroyer?
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT-0700"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String lastUMDDateString = Preferences.getString("umdLastObtained");
        if (lastUMDDateString != null && lastUMDDateString != "") {
          try {
            Date lastUMDDate = sdf.parse(lastUMDDateString);
            Calendar compareDate = Calendar.getInstance(TimeZone.getTimeZone("GMT-0700"));
            compareDate.setTime(lastUMDDate);
            compareDate.add(Calendar.DAY_OF_MONTH, 7);
            if (date.compareTo(compareDate) >= 0) {
              result[0] = "get Ultimate Mind Destroyer";
            } else {
              result[0] = "get cocktail ingredients";
            }
          } catch (ParseException ex) {
            result[0] = "get cocktail ingredients (sometimes Ultimate Mind Destroyer)";
            KoLmafia.updateDisplay("Unable to parse " + lastUMDDateString);
          }
        } else {
          // Change to "get Ultimate Mind Destroyer" after 12th August 2014
          result[0] = "get cocktail ingredients (sometimes Ultimate Mind Destroyer)";
        }
        result[1] = "get 5k meat and random item";
        result[2] = "get Beach Bucks";
        return result;

      case 988:

        // The Containment Unit
        result = new String[2];
        String containment = Preferences.getString("EVEDirections");
        if (containment.length() != 6) {
          return result;
        }
        int progress = StringUtilities.parseInt(containment.substring(5, 6));
        if (progress < 0 && progress > 5) {
          return result;
        }
        if (containment.charAt(progress) == 'L') {
          result[0] = "right way";
          result[1] = null;
        } else if (containment.charAt(progress) == 'R') {
          result[0] = null;
          result[1] = "right way";
        } else {
          result[0] = "unknown";
          result[1] = "unknown";
        }
        return result;

      case 1049:
        {
          // Tomb of the Unknown Your Class Here

          String responseText = ChoiceManager.lastResponseText;
          Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
          int options = choices.size();
          if (options == 1) {
            return new String[0];
          }

          int decision = ChoiceManager.getDecision(choice, responseText);
          if (decision == 0) {
            return new String[0];
          }

          result = new String[options];
          for (int i = 0; i < options; ++i) {
            result[i] = (i == decision - 1) ? "right answer" : "wrong answer";
          }

          return result;
        }

      case 1411:
        {
          // The Hall in the Hall
          result = new String[5];
          {
            boolean haveStaff = InventoryManager.getCount(ItemPool.DRIPPY_STAFF) > 0;
            int inebriety = KoLCharacter.getInebriety();
            int totalPoolSkill = KoLCharacter.estimatedPoolSkill();
            String buf =
                (haveStaff ? "M" : "A drippy staff and m")
                    + "aybe a drippy orb (Pool Skill at "
                    + Integer.valueOf(inebriety)
                    + " inebriety = "
                    + Integer.valueOf(totalPoolSkill)
                    + ")";
            result[0] = buf;
          }
          result[1] = "Buy a drippy candy bar for 10,000 Meat or get Driplets";
          {
            String item =
                KoLCharacter.hasSkill("Drippy Eye-Sprout")
                    ? "a drippy seed"
                    : KoLCharacter.hasSkill("Drippy Eye-Stone")
                        ? "a drippy bezoar"
                        : KoLCharacter.hasSkill("Drippy Eye-Beetle") ? "a drippy grub" : "nothing";
            result[2] = "Get " + item;
          }
          {
            int steins = InventoryManager.getCount(ItemPool.DRIPPY_STEIN);
            result[3] = (steins > 0) ? "Trade a drippy stein for a drippy pilsner" : "Get nothing";
          }
          result[4] = "Get some Driplets";
          return result;
        }
    }
    return null;
  }

  private static String shortcutSpoiler(final String setting) {
    return Preferences.getBoolean(setting) ? "shortcut KNOWN" : "learn shortcut";
  }

  private static void lockSpoiler(StringBuilder buffer) {
    buffer.append("possibly locked,");
    if (InventoryManager.getCount(ItemPool.DREADSYLVANIAN_SKELETON_KEY) == 0) {
      buffer.append(" no");
    }
    buffer.append(" key in inventory: ");
  }

  private static Object[] dynamicChoiceOptions(final String option) {
    if (!option.startsWith("choiceAdventure")) {
      return null;
    }
    int choice = StringUtilities.parseInt(option.substring(15));
    return ChoiceManager.dynamicChoiceOptions(choice);
  }

  public static final Object choiceSpoiler(
      final int choice, final int decision, final Object[] spoilers) {
    switch (choice) {
      case 105:
        // Having a Medicine Ball
        if (decision == 3) {
          KoLCharacter.ensureUpdatedGuyMadeOfBees();
          boolean defeated = Preferences.getBoolean("guyMadeOfBeesDefeated");
          if (defeated) {
            return "guy made of bees: defeated";
          }
          return "guy made of bees: called "
              + Preferences.getString("guyMadeOfBeesCount")
              + " times";
        }
        break;
      case 182:
        if (decision == 4) {
          return "model airship";
        }
        break;
      case 793:
        if (decision == 4) {
          return "gift shop";
        }
        break;
    }

    if (spoilers == null) {
      return null;
    }

    // Iterate through the spoilers and find the one corresponding to the decision
    for (int i = 0; i < spoilers.length; ++i) {
      Object spoiler = spoilers[i];

      // If this is an Option object, it may specify the option
      if (spoiler instanceof Option) {
        int option = ((Option) spoiler).getOption();
        if (option == decision) {
          return spoiler;
        }
        if (option != 0) {
          continue;
        }
        // option of 0 means use positional index
      }

      // If we get here, match positionalindex
      if ((i + 1) == decision) {
        return spoiler;
      }
    }

    // If we get here, we ran out of spoilers.
    return null;
  }

  public static final void processRedirectedChoiceAdventure(final String redirectLocation) {
    ChoiceManager.processChoiceAdventure(ChoiceManager.CHOICE_HANDLER, redirectLocation, null);
  }

  public static final void processChoiceAdventure(final String responseText) {
    ChoiceManager.processChoiceAdventure(ChoiceManager.CHOICE_HANDLER, "choice.php", responseText);
  }

  public static final String processChoiceAdventure(
      final String decision, final String extraFields, final boolean tryToAutomate) {
    return ChoiceManager.processChoiceAdventure(
        StringUtilities.parseInt(decision), extraFields, tryToAutomate);
  }

  public static final String processChoiceAdventure(
      final int decision, final String extraFields, final boolean tryToAutomate) {
    GenericRequest request = ChoiceManager.CHOICE_HANDLER;

    request.constructURLString("choice.php");
    request.addFormField("whichchoice", String.valueOf(ChoiceManager.lastChoice));
    request.addFormField("option", String.valueOf(decision));
    if (!extraFields.equals("")) {
      String[] fields = extraFields.split("&");
      for (String field : fields) {
        int equals = field.indexOf("=");
        if (equals != -1) {
          request.addFormField(field.substring(0, equals), field.substring(equals + 1));
        }
      }
    }
    request.addFormField("pwd", GenericRequest.passwordHash);
    request.run();

    if (tryToAutomate) {
      ChoiceManager.processChoiceAdventure(request, "choice.php", request.responseText);
      return "";
    }

    return request.responseText;
  }

  public static final boolean stillInChoice() {
    return ChoiceManager.stillInChoice(ChoiceManager.lastResponseText);
  }

  private static boolean stillInChoice(final String responseText) {
    // Doing the Maths has a choice form but, somehow, does not specify choice.php

    // <form method="get" id="">
    //   <input type="hidden" name="whichchoice" value="1103" />
    //   <input type="hidden" name="pwd" value="xxxxxx" />
    //   <input type="hidden" name="option" value="1" />
    //   <input type="text" name="num" value="" maxlen="6" size="6" />
    //   <input type="submit" value="Calculate the Universe" class="button" />
    //   <div style="clear:both"></div>
    // </form>

    return responseText.contains("action=choice.php")
        || responseText.contains("href=choice.php")
        || responseText.contains("name=\"whichchoice\"")
        || responseText.contains("href=\"choice.php");
  }

  public static final void processChoiceAdventure(
      final GenericRequest request, final String initialURL, String responseText) {
    // You can no longer simply ignore a choice adventure.  One of
    // the options may have that effect, but we must at least run
    // choice.php to find out which choice it is.

    // Get rid of extra fields - like "action=auto"
    request.constructURLString(initialURL);

    if (responseText == null) {
      GoalManager.updateProgress(GoalManager.GOAL_CHOICE);
      request.run();

      if (request.responseCode == 302) {
        return;
      }

      responseText = request.responseText;
    } else {
      request.responseText = responseText;
    }

    if (GenericRequest.passwordHash.equals("")) {
      return;
    }

    for (int stepCount = 0;
        !KoLmafia.refusesContinue() && ChoiceManager.stillInChoice(responseText);
        ++stepCount) {
      int choice = ChoiceManager.extractChoice(responseText);
      if (choice == 0) {
        // choice.php did not offer us any choices.
        // This would be a bug in KoL itself.
        // Bail now and let the user finish by hand.

        KoLmafia.updateDisplay(MafiaState.ABORT, "Encountered choice adventure with no choices.");
        request.showInBrowser(true);
        return;
      }

      if (ChoiceManager.invokeChoiceAdventureScript(choice, responseText)) {
        if (FightRequest.choiceFollowsFight) {
          // The choice redirected to a fight, which was immediately lost,
          // but which leads to another choice.
          // Let the caller automate that one, if desired.
          return;
        }

        if (!ChoiceManager.handlingChoice) {
          // The choiceAdventureScript processed this choice.
          return;
        }

        // We are still handling a choice. Maybe it is a different one.
        if (ChoiceManager.lastResponseText != null
            && choice != ChoiceManager.extractChoice(ChoiceManager.lastResponseText)) {
          responseText = ChoiceManager.lastResponseText;
          continue;
        }
      }

      // Either no choiceAdventure script or it left us in the same choice.
      if (!ChoiceManager.automateChoice(choice, request, stepCount)) {
        return;
      }

      // We automated one choice. If it redirected to a
      // fight, quit automating the choice.
      if (request.redirectLocation != null) {
        return;
      }

      responseText = request.responseText;
    }
  }

  private static boolean invokeChoiceAdventureScript(final int choice, final String responseText) {
    if (responseText == null) {
      return false;
    }

    String scriptName = Preferences.getString("choiceAdventureScript").trim();
    if (scriptName.length() == 0) {
      return false;
    }

    List<File> scriptFiles = KoLmafiaCLI.findScriptFile(scriptName);
    ScriptRuntime interpreter = KoLmafiaASH.getInterpreter(scriptFiles);

    if (interpreter == null) {
      return false;
    }

    File scriptFile = scriptFiles.get(0);

    Object[] parameters = new Object[2];
    parameters[0] = Integer.valueOf(choice);
    parameters[1] = responseText;

    KoLmafiaASH.logScriptExecution(
        "Starting choice adventure script: ", scriptFile.getName(), interpreter);

    // Since we are automating, let the script execute without interruption
    KoLmafia.forceContinue();

    interpreter.execute("main", parameters);
    KoLmafiaASH.logScriptExecution(
        "Finished choice adventure script: ", scriptFile.getName(), interpreter);

    return true;
  }

  private static boolean automateChoice(
      final int choice, final GenericRequest request, final int stepCount) {
    // If this choice has special handling that can't be
    // handled by a single preference (extra fields, for
    // example), handle it elsewhere.

    if (ChoiceManager.specialChoiceHandling(choice, request)) {
      // Should we abort?
      return false;
    }

    String option = "choiceAdventure" + choice;
    String optionValue = Preferences.getString(option);
    int amp = optionValue.indexOf("&");

    String decision = (amp == -1) ? optionValue : optionValue.substring(0, amp);
    String extraFields = (amp == -1) ? "" : optionValue.substring(amp + 1);

    // If choice zero is not "Manual Control", adjust it to an actual choice

    decision =
        ChoiceManager.specialChoiceDecision1(choice, decision, stepCount, request.responseText);

    // If one of the decisions will satisfy a goal, take it

    decision = ChoiceManager.pickGoalChoice(option, decision);

    // If this choice has special handling based on
    // character state, convert to real decision index

    decision =
        ChoiceManager.specialChoiceDecision2(choice, decision, stepCount, request.responseText);

    // Let user handle the choice manually, if requested

    if (decision.equals("0")) {
      KoLmafia.updateDisplay(MafiaState.ABORT, "Manual control requested for choice #" + choice);
      ChoiceUtilities.printChoices(ChoiceManager.lastResponseText);
      request.showInBrowser(true);
      return false;
    }

    if (KoLCharacter.isEd()
        && Preferences.getInteger("_edDefeats") >= Preferences.getInteger("edDefeatAbort")) {
      KoLmafia.updateDisplay(
          MafiaState.ABORT,
          "Hit Ed defeat threshold - Manual control requested for choice #" + choice);
      ChoiceUtilities.printChoices(ChoiceManager.lastResponseText);
      request.showInBrowser(true);
      return false;
    }

    // Bail if no setting determines the decision

    if (decision.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ABORT, "Unsupported choice adventure #" + choice);
      ChoiceManager.logChoices();
      request.showInBrowser(true);
      return false;
    }

    // Make sure that KoL currently allows the chosen choice/decision/extraFields
    String error =
        ChoiceUtilities.validateChoiceFields(decision, extraFields, request.responseText);
    if (error != null) {
      KoLmafia.updateDisplay(MafiaState.ABORT, error);
      ChoiceUtilities.printChoices(ChoiceManager.lastResponseText);
      request.showInBrowser(true);
      return false;
    }

    request.clearDataFields();
    request.addFormField("whichchoice", String.valueOf(choice));
    request.addFormField("option", decision);
    if (!extraFields.equals("")) {
      String[] fields = extraFields.split("&");
      for (String field : fields) {
        int equals = field.indexOf("=");
        if (equals != -1) {
          request.addFormField(field.substring(0, equals), field.substring(equals + 1));
        }
      }
    }
    request.addFormField("pwd", GenericRequest.passwordHash);

    request.run();

    return true;
  }

  public static final int getDecision(int choice, String responseText) {
    String option = "choiceAdventure" + choice;
    String optionValue = Preferences.getString(option);
    int amp = optionValue.indexOf("&");

    String decision = (amp == -1) ? optionValue : optionValue.substring(0, amp);
    String extraFields = (amp == -1) ? "" : optionValue.substring(amp + 1);

    // If choice decision is not "Manual Control", adjust it to an actual option

    decision =
        ChoiceManager.specialChoiceDecision1(choice, decision, Integer.MAX_VALUE, responseText);

    // If one of the decisions will satisfy a goal, take it

    decision = ChoiceManager.pickGoalChoice(option, decision);

    // If this choice has special handling based on
    // character state, convert to real decision index

    decision =
        ChoiceManager.specialChoiceDecision2(choice, decision, Integer.MAX_VALUE, responseText);

    // Currently unavailable decision, manual choice requested, or unsupported choice
    if (decision.equals("0")
        || decision.equals("")
        || ChoiceUtilities.validateChoiceFields(decision, extraFields, responseText) != null) {
      return 0;
    }

    return StringUtilities.parseInt(decision);
  }

  public static final int getLastChoice() {
    return ChoiceManager.lastChoice;
  }

  public static final int getLastDecision() {
    return ChoiceManager.lastDecision;
  }

  public static final void preChoice(final GenericRequest request) {
    FightRequest.choiceFollowsFight = false;
    ChoiceManager.handlingChoice = true;
    FightRequest.currentRound = 0;

    String choice = request.getFormField("whichchoice");
    String option = request.getFormField("option");

    if (choice == null || option == null) {
      // Visiting a choice page but not yet making a decision
      ChoiceManager.lastChoice = 0;
      ChoiceManager.lastDecision = 0;
      ChoiceManager.lastResponseText = null;
      ChoiceManager.lastDecoratedResponseText = null;
      return;
    }

    // We are about to take a choice option
    ChoiceManager.lastChoice = StringUtilities.parseInt(choice);
    ChoiceManager.lastDecision = StringUtilities.parseInt(option);

    switch (ChoiceManager.lastChoice) {
        // Wheel In the Sky Keep on Turning: Muscle Position
      case 9:
        Preferences.setString(
            "currentWheelPosition",
            ChoiceManager.lastDecision == 1
                ? "mysticality"
                : ChoiceManager.lastDecision == 2 ? "moxie" : "muscle");
        break;

        // Wheel In the Sky Keep on Turning: Mysticality Position
      case 10:
        Preferences.setString(
            "currentWheelPosition",
            ChoiceManager.lastDecision == 1
                ? "map quest"
                : ChoiceManager.lastDecision == 2 ? "muscle" : "mysticality");
        break;

        // Wheel In the Sky Keep on Turning: Map Quest Position
      case 11:
        Preferences.setString(
            "currentWheelPosition",
            ChoiceManager.lastDecision == 1
                ? "moxie"
                : ChoiceManager.lastDecision == 2 ? "mysticality" : "map quest");
        break;

        // Wheel In the Sky Keep on Turning: Moxie Position
      case 12:
        Preferences.setString(
            "currentWheelPosition",
            ChoiceManager.lastDecision == 1
                ? "muscle"
                : ChoiceManager.lastDecision == 2 ? "map quest" : "moxie");
        break;

        // Maidens: disambiguate the Knights
      case 89:
        AdventureRequest.setNameOverride(
            "Knight", ChoiceManager.lastDecision == 1 ? "Knight (Wolf)" : "Knight (Snake)");
        break;

        // Strung-Up Quartet
      case 106:
        Preferences.setInteger("lastQuartetAscension", KoLCharacter.getAscensions());
        Preferences.setInteger("lastQuartetRequest", ChoiceManager.lastDecision);

        if (KoLCharacter.recalculateAdjustments()) {
          KoLCharacter.updateStatus();
        }

        break;

      case 123: // At Least It's Not Full Of Trash
        if (ChoiceManager.lastDecision == 2) {
          // Raise your hands up towards the heavens
          // This takes take a turn and advances to the tiles
          ResultProcessor.processAdventuresUsed(1);
        }
        break;

        // Start the Island War Quest
      case 142:
      case 146:
        if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestProgress(Quest.ISLAND_WAR, "step1");
          Preferences.setString("warProgress", "started");
          if (KoLCharacter.inPokefam()) {
            // The following is a guess. Since all
            // sidequests are open, it is at least
            // 458, and surely both sides are equal
            Preferences.setInteger("hippiesDefeated", 500);
            Preferences.setInteger("fratboysDefeated", 500);
          }
        }
        break;

        // The Gong Has Been Bung
      case 276:
        ResultProcessor.processItem(ItemPool.GONG, -1);
        Preferences.setInteger("moleTunnelLevel", 0);
        Preferences.setInteger("birdformCold", 0);
        Preferences.setInteger("birdformHot", 0);
        Preferences.setInteger("birdformRoc", 0);
        Preferences.setInteger("birdformSleaze", 0);
        Preferences.setInteger("birdformSpooky", 0);
        Preferences.setInteger("birdformStench", 0);
        break;

        // The Horror...
      case 611:
        // To find which step we're on, look at the responseText from the _previous_ request.  This
        // should still be in lastResponseText.
        ChoiceManager.abooPeakLevel =
            ChoiceManager.findBooPeakLevel(
                ChoiceManager.findChoiceDecisionText(1, ChoiceManager.lastResponseText));
        // Handle changing the progress level in postChoice1 where we know the result.
        break;

        // Behind the world there is a door...
      case 612:
        TurnCounter.stopCounting("Silent Invasion window begin");
        TurnCounter.stopCounting("Silent Invasion window end");
        TurnCounter.startCounting(35, "Silent Invasion window begin loc=*", "lparen.gif");
        TurnCounter.startCounting(40, "Silent Invasion window end loc=*", "rparen.gif");
        break;

      case 794:
        ResultProcessor.removeItem(ItemPool.FUNKY_JUNK_KEY);
        break;

      case 866:
        // Choice 866 is Methinks the Protesters Doth Protest Too Little
        // If you have a clover, this is a clover adventure.
        // Otherwise it is a semirare
        if (InventoryManager.getCount(ItemPool.TEN_LEAF_CLOVER) > 0) {
          ResultProcessor.removeItem(ItemPool.TEN_LEAF_CLOVER);
        } else {
          KoLCharacter.registerSemirare();
        }
        break;

      case 931:
        // Life Ain't Nothin But Witches and Mummies
        QuestDatabase.setQuestIfBetter(Quest.CITADEL, "step6");
        break;

      case 932:
        // No Whammies
        QuestDatabase.setQuestIfBetter(Quest.CITADEL, "step8");
        break;

      case 1005: // 'Allo
      case 1006: // One Small Step For Adventurer
      case 1007: // Twisty Little Passages, All Hedge
      case 1008: // Pooling Your Resources
      case 1009: // Good Ol' 44% Duck
      case 1010: // Another Day, Another Fork
      case 1011: // Of Mouseholes and Manholes
      case 1012: // The Last Temptation
      case 1013: // Mazel Tov!
        // Taking any of these takes a turn. We'll eventually
        // be informed of that in a charpane/api refresh, but
        // that's too late for logging.
        ResultProcessor.processAdventuresUsed(1);
        break;

      case 1023: // Like a Bat into Hell
        if (ChoiceManager.lastDecision == 2) {
          int edDefeats = Preferences.getInteger("_edDefeats");
          int kaCost = edDefeats > 2 ? (int) (Math.pow(2, Math.min(edDefeats - 3, 5))) : 0;
          AdventureResult cost = ItemPool.get(ItemPool.KA_COIN, -kaCost);
          ResultProcessor.processResult(cost);
          KoLCharacter.setLimitmode(null);
        }
        break;

      case 1024: // Like a Bat out of Hell
        switch (ChoiceManager.lastDecision) {
          case 2:
            Preferences.setInteger("_edDefeats", 0);
            Preferences.setBoolean("edUsedLash", false);
            MonsterStatusTracker.reset();
            KoLCharacter.setLimitmode(null);
            break;
          case 1:
            int edDefeats = Preferences.getInteger("_edDefeats");
            int kaCost = edDefeats > 2 ? (int) (Math.pow(2, Math.min(edDefeats - 3, 5))) : 0;
            AdventureResult cost = ItemPool.get(ItemPool.KA_COIN, -kaCost);
            ResultProcessor.processResult(cost);
            KoLCharacter.setLimitmode(null);
            break;
        }
        break;

      case 1028:
        // A Shop
        SpelunkyRequest.logShop(ChoiceManager.lastResponseText, ChoiceManager.lastDecision);
        break;

      case 1085: // Deck of Every Card
        if (ChoiceManager.lastDecision == 1) {
          Preferences.increment("_deckCardsDrawn", 1, 15, false);
        }
        break;

      case 1086: // Pick a Card
        if (ChoiceManager.lastDecision == 1) {
          // The extra 1 will be covered in choice 1085
          Preferences.increment("_deckCardsDrawn", 4, 15, false);
        }
        break;

      case 1171: // LT&T Office
        if (ChoiceManager.lastDecision < 4) {
          QuestDatabase.setQuestProgress(Quest.TELEGRAM, QuestDatabase.STARTED);
          Preferences.setInteger("lttQuestDifficulty", ChoiceManager.lastDecision);
          Preferences.setInteger("lttQuestStageCount", 0);
          Matcher matcher = TELEGRAM_PATTERN.matcher(ChoiceManager.lastResponseText);
          for (int i = 0; i < ChoiceManager.lastDecision; i++) {
            if (!matcher.find()) {
              break;
            }
          }
          Preferences.setString("lttQuestName", matcher.group(1));
        } else if (ChoiceManager.lastDecision == 5) {
          QuestDatabase.setQuestProgress(Quest.TELEGRAM, QuestDatabase.UNSTARTED);
          Preferences.setInteger("lttQuestDifficulty", 0);
          Preferences.setInteger("lttQuestStageCount", 0);
          Preferences.setString("lttQuestName", "");
        }
        break;

      case 1197:
        // Travel back to a Delicious Meal
        if (ChoiceManager.lastDecision == 1 && !request.getURLString().contains("foodid=0")) {
          EatItemRequest.timeSpinnerUsed = true;
        }
        break;

      case 1261:
        // Which Door?
        if (ChoiceManager.lastResponseText.contains("Boris")) {
          Preferences.setString("_villainLairKey", "boris");
        } else if (ChoiceManager.lastResponseText.contains("Jarlsberg")) {
          Preferences.setString("_villainLairKey", "jarlsberg");
        } else if (ChoiceManager.lastResponseText.contains("Sneaky Pete")) {
          Preferences.setString("_villainLairKey", "pete");
        }
        break;

      case 1345:
        // Blech House
        Preferences.setInteger("smutOrcNoncombatProgress", 0);
        break;
    }
  }

  public static void postChoice0(final String urlString, final GenericRequest request) {
    if (ChoiceManager.nonInterruptingRequest(urlString, request)) {
      return;
    }

    // If this is not actually a choice page, nothing to do here.
    if (!urlString.startsWith("choice.php")) {
      return;
    }

    // Things that have to be done before we register the encounter.

    String text = request.responseText;
    int choice =
        ChoiceManager.lastChoice == 0
            ? ChoiceManager.extractChoice(text)
            : ChoiceManager.lastChoice;

    if (choice == 0) {
      // choice.php did not offer us any choices.
      // This would be a bug in KoL itself.
      return;
    }

    switch (choice) {
      case 125: // No Visible Means of Support
        if (ChoiceManager.lastChoice == 0) {
          // If we are visiting for the first time,
          // finish the tiles
          DvorakManager.lastTile(text);
        }
        break;

      case 360: // Wumpus Cave
        WumpusManager.preWumpus(ChoiceManager.lastDecision);
        break;

      case 1019: // Bee Rewarded
        if (ChoiceManager.lastDecision == 1) {
          // This does not contain an "Encounter", so was
          // not logged.
          RequestLogger.registerLocation("The Black Forest");
        }
        break;

      case 1308:
        {
          // place.php?whichplace=monorail&action=monorail_downtown
          //
          // On a Downtown Train
          //
          // Everything you do at this location uses this choice
          // adventure #. As you select options, you stay in the
          // same choice, but the available options change.
          //
          // We must deduce what is happening by looking at the
          // response text

          if (text.contains("muffin is not yet ready")) {
            Preferences.setBoolean("_muffinOrderedToday", true);

            // We submitted a choice. If we are buying a muffin,
            // remove the muffin tin here, since the response is
            // indistinguishable from simply visiting the Breakfast
            // Counter with a muffin on order.
            //
            // Therefore, look at the previous response. If it is
            // selling muffins and we jut bought a muffin, remove
            // the muffin tin.

            if (ChoiceManager.lastResponseText != null
                && ChoiceManager.lastResponseText.contains("Order a blueberry muffin")) {
              ResultProcessor.processResult(ItemPool.get(ItemPool.EARTHENWARE_MUFFIN_TIN, -1));
            }
          }
          break;
        }
      case 1451:
        // Fire Captain Hagnk
        WildfireCampRequest.parseCaptain(text);
        break;
      case 1452:
        // Sprinkler Joe
        if (text.contains("Thanks again for your help!")) {
          Preferences.setBoolean("wildfireSprinkled", true);
        }
        break;
      case 1453:
        // Fracker Dan
        if (text.contains("Thanks for the help!")) {
          Preferences.setBoolean("wildfireFracked", true);
        }
        break;
      case 1454:
        // Cropduster Dusty
        if (text.contains("Thanks for helping out.")) {
          Preferences.setBoolean("wildfireDusted", true);
        }
        break;
    }
  }

  /**
   * Certain requests do not interrupt a choice (i.e. are accessible and do not walk away from the
   * choice)
   */
  public static boolean nonInterruptingRequest(
      final String urlString, final GenericRequest request) {
    return request.isExternalRequest
        || request.isRootsetRequest
        || request.isTopmenuRequest
        || request.isChatRequest
        || request.isChatLaunchRequest
        || request.isDescRequest
        || request.isStaticRequest
        || request.isQuestLogRequest
        ||
        // Daily Reminders
        urlString.startsWith("main.php?checkbfast")
        ||
        // Choice 1414 uses Lock Picking
        urlString.equals("skillz.php?oneskillz=195")
        ||
        // Choice 1399 uses Seek out a Bird
        urlString.equals("skillz.php?oneskillz=7323");
  }

  private static final Pattern SKELETON_PATTERN =
      Pattern.compile("You defeated <b>(\\d+)</b> skeletons");
  private static final Pattern FOG_PATTERN = Pattern.compile("<font.*?><b>(.*?)</b></font>");

  private static final Pattern TOSSID_PATTERN = Pattern.compile("tossid=(\\d+)");
  // You toss the space wine off the edge of the floating battlefield and 21 frat boys jump off
  // after it.
  private static final Pattern FRAT_RATIONING_PATTERN =
      Pattern.compile(
          "You toss the (.*?) off the edge of the floating battlefield and (\\d+) frat boys? jumps? off after it.");
  // You toss the mana curds into the crowd.  10 hippies dive onto it, greedily consume it, and pass
  // out.
  private static final Pattern HIPPY_RATIONING_PATTERN =
      Pattern.compile(
          "You toss the (.*?) into the crowd.  (\\d+) hippies dive onto it, greedily consume it, and pass out.");
  // You select a Gold Tier client, Norma "Smelly" Jackson, a hobo from The Oasis.
  private static final Pattern GUZZLR_LOCATION_PATTERN =
      Pattern.compile(
          "You select a (Bronze|Gold|Platinum) Tier client, (.*?), +an? (.*?) from (.*?)\\.<p>");

  public static void postChoice1(final String urlString, final GenericRequest request) {
    if (ChoiceManager.nonInterruptingRequest(urlString, request)) {
      return;
    }

    // If you walked away from the choice, this is not the result of a choice.
    if (ChoiceManager.canWalkAway
        && !urlString.startsWith("choice.php")
        && !urlString.startsWith("fight.php")) {
      return;
    }

    // Things that can or need to be done BEFORE processing results.
    // Remove spent items or meat here.

    if (ChoiceManager.lastChoice == 0) {
      // We are viewing the choice page for the first time.
      ChoiceManager.visitChoice(request);
      return;
    }

    String text = request.responseText;

    // If this is not actually a choice page, we were redirected.
    // Do not save this responseText
    if (urlString.startsWith("choice.php")) {
      ChoiceManager.lastResponseText = text;
    }

    switch (ChoiceManager.lastChoice) {
      case 188:
        // The Infiltrationist

        // Once you're inside the frat house, it's a simple
        // matter of making your way down to the basement and
        // retrieving Caronch's dentures from the frat boys'
        // ridiculous trophy case.

        if (ChoiceManager.lastDecision == 3 && text.contains("ridiculous trophy case")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.HOT_WING, -3));
        }
        break;

      case 189:
        // O Cap'm, My Cap'm
        if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step26");
        }
        break;

      case 191:
        // Chatterboxing
        if (ChoiceManager.lastDecision == 2
            && text.contains("find a valuable trinket that looks promising")) {
          BanishManager.banishMonster("chatty pirate", "chatterboxing");
        }
        break;

      case 237:
        // Big Merv's Protein Shakes
      case 238:
        // Suddenly Salad!
      case 239:
        // Sizzling Weasel On a Stick
        if (ChoiceManager.lastDecision == 1 && text.contains("You gain")) {
          // You spend 20 hobo nickels
          AdventureResult cost = ItemPool.get(ItemPool.HOBO_NICKEL, -20);
          ResultProcessor.processResult(cost);

          // You gain 5 fullness
          KoLCharacter.setFullness(KoLCharacter.getFullness() + 5);
        }
        break;

      case 242:
        // Arthur Finn's World-Record Homebrew Stout
      case 243:
        // Mad Jack's Corn Squeezery
      case 244:
        // Bathtub Jimmy's Gin Mill
        if (ChoiceManager.lastDecision == 1 && text.contains("You gain")) {
          // You spend 20 hobo nickels
          AdventureResult cost = ItemPool.get(ItemPool.HOBO_NICKEL, -20);
          ResultProcessor.processResult(cost);

          // You gain 5 drunkenness.  This will be set
          // when we refresh the charpane.
        }
        break;

      case 271:
        // Tattoo Shop
      case 274:
        // Tattoo Redux
        if (ChoiceManager.lastDecision == 1) {
          Matcher matcher = ChoiceManager.TATTOO_PATTERN.matcher(request.responseText);
          if (matcher.find()) {
            int tattoo = StringUtilities.parseInt(matcher.group(1));
            AdventureResult cost = ItemPool.get(ItemPool.HOBO_NICKEL, -20 * tattoo);
            ResultProcessor.processResult(cost);
          }
        }
        break;

      case 298:
        // In the Shade

        // You carefully plant the packet of seeds, sprinkle it
        // with gooey green algae, wait a few days, and then
        // you reap what you sow. Sowed. Sew?

        if (ChoiceManager.lastDecision == 1 && text.contains("you reap what you sow")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SEED_PACKET, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.GREEN_SLIME, -1));
        }
        break;

      case 346:
        // Soup For You
      case 347:
        // Yes, Soup For You...
      case 348:
        // Souped Up
      case 351:
        // Beginner's Luck
        QuestDatabase.setQuestIfBetter(Quest.PRIMORDIAL, QuestDatabase.STARTED);
        break;

      case 349:
        // The Primordial Directive
        // You swam upward, into a brighter and warmer part of the soup
        if (ChoiceManager.lastDecision == 1
            && text.contains("a brighter and warmer part of the soup")) {
          QuestDatabase.setQuestIfBetter(Quest.PRIMORDIAL, "step1");
        }
        break;

      case 350:
        // Soupercharged
        if (ChoiceManager.lastDecision == 1 && text.contains("You've fixed me all up")) {
          Preferences.setInteger("aminoAcidsUsed", 0);
          QuestDatabase.setQuestProgress(Quest.PRIMORDIAL, QuestDatabase.FINISHED);
        }
        break;

      case 354:
        // You Can Never Be Too Rich or Too in the Future
        ResultProcessor.processResult(ItemPool.get(ItemPool.INDIGO_PARTY_INVITATION, -1));
        break;

      case 355:
        // I'm on the Hunt, I'm After You
        ResultProcessor.processResult(ItemPool.get(ItemPool.VIOLET_HUNT_INVITATION, -1));
        break;

      case 357:
        // Painful, Circuitous Logic
        ResultProcessor.processResult(ItemPool.get(ItemPool.MECHA_MAYHEM_CLUB_CARD, -1));
        break;

      case 358:
        // Brings All the Boys to the Blue Yard
        ResultProcessor.processResult(ItemPool.get(ItemPool.BLUE_MILK_CLUB_CARD, -1));
        break;

      case 362:
        // A Bridge Too Far
        ResultProcessor.processResult(ItemPool.get(ItemPool.SPACEFLEET_COMMUNICATOR_BADGE, -1));
        break;

      case 363:
        // Does This Bug You? Does This Bug You?
        ResultProcessor.processResult(ItemPool.get(ItemPool.SMUGGLER_SHOT_FIRST_BUTTON, -1));
        break;

      case 373:
        // Choice 373 is Northern Gate

        // Krakrox plugged the small stone block he found in
        // the basement of the abandoned building into the hole
        // on the left side of the gate

        if (text.contains("Krakrox plugged the small stone block")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SMALL_STONE_BLOCK, -1));
        }

        // Krakrox plugged the little stone block he had found
        // in the belly of the giant snake into the hole on the
        // right side of the gate

        else if (text.contains("Krakrox plugged the little stone block")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.LITTLE_STONE_BLOCK, -1));
        }
        break;

      case 376:
        // Choice 376 is Ancient Temple

        // You fit the two halves of the stone circle together,
        // and slot them into the depression on the door. After
        // a moment's pause, a low rumbling becomes audible,
        // and then the stone slab lowers into the ground. The
        // temple is now open to you, if you are ready to face
        // whatever lies inside.

        if (text.contains("two halves of the stone circle")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.HALF_STONE_CIRCLE, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.STONE_HALF_CIRCLE, -1));
        }
        break;

      case 389:
        // Choice 389 is The Unbearable Supremeness of Being

        // "Of course I understand," Jill says, in fluent
        // English. "I learned your language in the past five
        // minutes. I know where the element is, but we'll have
        // to go offworld to get it. Meet me at the Desert
        // Beach Spaceport." And with that, she gives you a
        // kiss and scampers off. Homina-homina.

        if (text.contains("Homina-homina")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SUPREME_BEING_GLOSSARY, -1));
        }
        break;

      case 392:
        // Choice 392 is The Elements of Surprise . . .

        // And as the two of you walk toward the bed, you sense
        // your ancestral memories pulling you elsewhere, ever
        // elsewhere, because your ancestral memories are
        // total, absolute jerks.

        if (text.contains("total, absolute jerks")) {
          EquipmentManager.discardEquipment(ItemPool.RUBY_ROD);
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_HEAT, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_KINK, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_COLD, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_STENCH, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_FRIGHT, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.ESSENCE_OF_CUTE, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.SECRET_FROM_THE_FUTURE, 1));
        }
        break;

      case 393:
        // The Collector
        if (ChoiceManager.lastDecision == 1) {
          for (int i = ItemPool.GREEN_PEAWEE_MARBLE; i <= ItemPool.BIG_BUMBOOZER_MARBLE; ++i) {
            ResultProcessor.processResult(ItemPool.get(i, -1));
          }
        }
        break;

      case 394:
        {
          // Hellevator Music
          // Parse response
          Matcher matcher = HELLEVATOR_PATTERN.matcher(text);
          if (!matcher.find()) {
            break;
          }
          String floor = matcher.group(1);
          for (int mcd = 0; mcd < FLOORS.length; ++mcd) {
            if (floor.equals(FLOORS[mcd])) {
              String message = "Setting monster level to " + mcd;
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
              break;
            }
          }
          break;
        }

      case 413:
      case 414:
      case 415:
      case 416:
      case 417:
      case 418:
        HaciendaManager.parseRoom(ChoiceManager.lastChoice, ChoiceManager.lastDecision, text);
        break;

      case 440:
        // Puttin' on the Wax
        if (ChoiceManager.lastDecision == 1) {
          HaciendaManager.parseRecording(urlString, text);
        }
        break;

      case 443:
        // Chess Puzzle
        if (ChoiceManager.lastDecision == 1) {
          // Option 1 is "Play"
          RabbitHoleManager.parseChessMove(urlString, text);
        }
        break;

      case 450:
        // The Duchess' Cottage
        if (ChoiceManager.lastDecision == 1
            && text.contains("Delectable and pulchritudinous!")) { // Option 1 is Feed the Duchess
          ResultProcessor.processItem(ItemPool.BEAUTIFUL_SOUP, -1);
          ResultProcessor.processItem(ItemPool.LOBSTER_QUA_GRILL, -1);
          ResultProcessor.processItem(ItemPool.MISSING_WINE, -1);
          ResultProcessor.processItem(ItemPool.WALRUS_ICE_CREAM, -1);
          ResultProcessor.processItem(ItemPool.HUMPTY_DUMPLINGS, -1);
        }
        break;

      case 457:
        // Oh, No! Five-Oh!
        int count = InventoryManager.getCount(ItemPool.ORQUETTES_PHONE_NUMBER);
        if (ChoiceManager.lastDecision == 1 && count > 0) {
          ResultProcessor.processItem(ItemPool.ORQUETTES_PHONE_NUMBER, -count);
          ResultProcessor.processItem(ItemPool.KEGGER_MAP, -1);
        }

      case 460:
      case 461:
      case 462:
      case 463:
      case 464:
      case 465:
      case 467:
      case 468:
      case 469:
      case 470:
      case 472:
      case 473:
      case 474:
      case 475:
      case 476:
      case 477:
      case 478:
      case 479:
      case 480:
      case 481:
      case 482:
      case 483:
      case 484:
        // Space Trip
        ArcadeRequest.postChoiceSpaceTrip(
            request, ChoiceManager.lastChoice, ChoiceManager.lastDecision);
        break;

      case 471:
        // DemonStar
        ArcadeRequest.postChoiceDemonStar(request, ChoiceManager.lastDecision);
        break;

      case 485:
        // Fighters Of Fighting
        ArcadeRequest.postChoiceFightersOfFighting(request, ChoiceManager.lastDecision);
        break;

      case 486:
        // Dungeon Fist!
        ArcadeRequest.postChoiceDungeonFist(request, ChoiceManager.lastDecision);
        break;

      case 488:
      case 489:
      case 490:
      case 491:
        // Meteoid
        ArcadeRequest.postChoiceMeteoid(
            request, ChoiceManager.lastChoice, ChoiceManager.lastDecision);
        break;

      case 529:
      case 531:
      case 532:
      case 533:
      case 534:
        Matcher skeletonMatcher = SKELETON_PATTERN.matcher(text);
        if (skeletonMatcher.find()) {
          String message = "You defeated " + skeletonMatcher.group(1) + " skeletons";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
        break;

      case 539:
        // Choice 539 is An E.M.U. for Y.O.U.
        EquipmentManager.discardEquipment(ItemPool.SPOOKY_LITTLE_GIRL);
        break;

      case 540:
        // Choice 540 is Big-Time Generator - game board
        //
        // Win:
        //
        // The generator starts to hum and the well above you
        // begins to spin, slowly at first, then faster and
        // faster. The humming becomes a deep, sternum-rattling
        // thrum, a sub-audio *WHOOMP WHOOMP WHOOMPWHOOMPWHOOMP.*
        // Brilliant blue light begins to fill the well, and
        // you feel like your bones are turning to either
        // powder, jelly, or jelly with powder in it.<p>Then
        // you fall through one of those glowy-circle
        // transporter things and end up back on Grimace, and
        // boy, are they glad to see you! You're not sure where
        // one gets ticker-tape after an alien invasion, but
        // they seem to have found some.
        //
        // Lose 3 times:
        //
        // Your E.M.U.'s getting pretty beaten up from all the
        // pinballing between obstacles, and you don't like
        // your chances of getting back to the surface if you
        // try again. You manage to blast out of the generator
        // well and land safely on the surface. After that,
        // though, the E.M.U. gives an all-over shudder, a sad
        // little servo whine, and falls apart.

        if (text.contains("WHOOMP") || text.contains("a sad little servo whine")) {
          EquipmentManager.discardEquipment(ItemPool.EMU_UNIT);
          QuestDatabase.setQuestIfBetter(Quest.GENERATOR, QuestDatabase.FINISHED);
        }
        break;

      case 542:
        // The Now's Your Pants!  I Mean... Your Chance!

        // Then you make your way back out of the Alley,
        // clutching your pants triumphantly and trying really
        // hard not to think about how oddly chilly it has
        // suddenly become.

        // When you steal your pants, they are unequipped, you
        // gain a "you acquire" message", and they appear in
        // inventory.
        //
        // Treat this is simply discarding the pants you are
        // wearing
        if (text.contains("oddly chilly")) {
          EquipmentManager.discardEquipment(EquipmentManager.getEquipment(EquipmentManager.PANTS));
          QuestDatabase.setQuestProgress(Quest.MOXIE, "step1");
        }
        break;

      case 546:
        // Interview With You
        VampOutManager.postChoiceVampOut(text);
        break;

      case 559:
        // Fudge Mountain Breakdown
        if (ChoiceManager.lastDecision == 2) {
          if (text.contains("but nothing comes out")) {
            Preferences.setInteger("_fudgeWaspFights", 3);
          } else if (text.contains("trouble has taken a holiday")) {
            // The Advent Calendar hasn't been punched out enough to find fudgewasps yet
          } else {
            Preferences.increment("_fudgeWaspFights", 1);
          }
        }
        break;

      case 571:
        // Choice 571 is Your Minstrel Vamps
        QuestDatabase.setQuestProgress(Quest.CLANCY, QuestDatabase.STARTED);
        break;

      case 572:
        // Choice 572 is Your Minstrel Clamps
        QuestDatabase.setQuestProgress(Quest.CLANCY, "step2");
        break;

      case 573:
        // Choice 573 is Your Minstrel Stamps
        QuestDatabase.setQuestProgress(Quest.CLANCY, "step4");
        break;

      case 576:
        // Choice 576 is Your Minstrel Camps
        QuestDatabase.setQuestProgress(Quest.CLANCY, "step6");
        break;

      case 577:
        // Choice 577 is Your Minstrel Scamp
        QuestDatabase.setQuestProgress(Quest.CLANCY, "step8");
        break;

      case 588:
        // Machines!
        if (text.contains("The batbugbears around you start acting weird")) {
          BugbearManager.clearShipZone("Sonar");
        }
        break;

      case 589:
        // Autopsy Auturvy
        // The tweezers you used dissolve in the caustic fluid. Rats.
        if (text.contains("dissolve in the caustic fluid")) {
          ResultProcessor.processItem(ItemPool.AUTOPSY_TWEEZERS, -1);
        }
        return;

      case 594:
        // A Lost Room
        if (text.contains("You acquire")) {
          ResultProcessor.processItem(ItemPool.LOST_KEY, -1);
        }
        break;

      case 595:
        // Fire! I... have made... fire!
        if (text.contains("rubbing the two stupid sticks together")
            || text.contains("pile the sticks up on top of the briefcase")) {
          ResultProcessor.processItem(ItemPool.CSA_FIRE_STARTING_KIT, -1);
        }
        return;

      case 599:
        // A Zombie Master's Bait
        if (request.getFormField("quantity") == null) {
          return;
        }

        AdventureResult brain;
        if (ChoiceManager.lastDecision == 1) {
          brain = ItemPool.get(ItemPool.CRAPPY_BRAIN, 1);
        } else if (ChoiceManager.lastDecision == 2) {
          brain = ItemPool.get(ItemPool.DECENT_BRAIN, 1);
        } else if (ChoiceManager.lastDecision == 3) {
          brain = ItemPool.get(ItemPool.GOOD_BRAIN, 1);
        } else if (ChoiceManager.lastDecision == 4) {
          brain = ItemPool.get(ItemPool.BOSS_BRAIN, 1);
        } else {
          return;
        }

        int quantity = StringUtilities.parseInt(request.getFormField("quantity"));
        int inventoryCount = brain.getCount(KoLConstants.inventory);
        brain = brain.getInstance(-1 * Math.min(quantity, inventoryCount));

        ResultProcessor.processResult(brain);

        return;

      case 603:
        // Skeletons and The Closet
        if (ChoiceManager.lastDecision != 6) {
          ResultProcessor.removeItem(ItemPool.SKELETON);
        }
        return;

      case 607:
        // Room 237
        // Twin Peak first choice
        if (text.contains("You take a moment to steel your nerves.")) {
          int prefval = Preferences.getInteger("twinPeakProgress");
          prefval |= 1;
          Preferences.setInteger("twinPeakProgress", prefval);
        }
        return;

      case 608:
        // Go Check It Out!
        // Twin Peak second choice
        if (text.contains("All work and no play")) {
          int prefval = Preferences.getInteger("twinPeakProgress");
          prefval |= 2;
          Preferences.setInteger("twinPeakProgress", prefval);
        }
        return;

      case 611:
        // The Horror...
        // We need to detect if the choiceadv step was completed OR we got beaten up.
        // If we Flee, nothing changes
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("That's all the horror you can take")) { // AKA beaten up
            Preferences.decrement("booPeakProgress", 2, 0);
          } else {
            Preferences.decrement("booPeakProgress", 2 * ChoiceManager.abooPeakLevel, 0);
          }
          if (Preferences.getInteger("booPeakProgress") < 0) {
            Preferences.setInteger("booPeakProgress", 0);
          }
        }
        return;

      case 614:
        // Near the fog there is an... anvil?
        if (!text.contains("You acquire")) {
          return;
        }
        int souls =
            ChoiceManager.lastDecision == 1
                ? 3
                : ChoiceManager.lastDecision == 2
                    ? 11
                    : ChoiceManager.lastDecision == 3
                        ? 23
                        : ChoiceManager.lastDecision == 4 ? 37 : 0;
        if (souls > 0) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.MIME_SOUL_FRAGMENT, 0 - souls));
        }
        return;

      case 616:
        // He Is the Arm, and He Sounds Like This
        // Twin Peak third choice
        if (text.contains("You attempt to mingle")) {
          int prefval = Preferences.getInteger("twinPeakProgress");
          prefval |= 4;
          Preferences.setInteger("twinPeakProgress", prefval);
          ResultProcessor.processResult(ItemPool.get(ItemPool.JAR_OF_OIL, -1));
        }
        return;

      case 617:
        // Now It's Dark
        // Twin Peak fourth choice
        if (text.contains("When the lights come back")) {
          // the other three must be completed at this point.
          Preferences.setInteger("twinPeakProgress", 15);
        }
        return;

      case 618:
        // Cabin Fever
        if (text.contains("mercifully, the hotel explodes")) {
          // the other three must be completed at this point.
          Preferences.setInteger("twinPeakProgress", 15);
        }
        return;

      case 669:
      case 670:
      case 671:
        // All New Area Unlocked messages unlock the Ground Floor but check for it specifically in
        // case future changes unlock areas with message.
        if (text.contains("New Area Unlocked") && text.contains("The Ground Floor")) {
          Preferences.setInteger("lastCastleGroundUnlock", KoLCharacter.getAscensions());
          QuestDatabase.setQuestProgress(Quest.GARBAGE, "step8");
        }
        break;

      case 675:
        // Melon Collie and the Infinite Lameness
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.DRUM_N_BASS_RECORD);
        }
        break;

      case 679:
        // Keep On Turnin' the Wheel in the Sky
        QuestDatabase.setQuestProgress(Quest.GARBAGE, "step10");
        break;

      case 689:
        // The Final Reward
        if (text.contains("claim your rightful reward")) {
          // Daily Dungeon Complete
          Preferences.setBoolean("dailyDungeonDone", true);
          Preferences.setInteger("_lastDailyDungeonRoom", 15);
        }
        return;

      case 690:
      case 691:
        // The First Chest Isn't the Deepest and Second Chest
        if (ChoiceManager.lastDecision == 2) {
          Preferences.increment("_lastDailyDungeonRoom", 3);
        } else {
          Preferences.increment("_lastDailyDungeonRoom", 1);
        }
        return;

      case 692:
        // I Wanna Be a Door
        if (text.contains("key breaks off in the lock")) {
          // Unfortunately, the key breaks off in the lock.
          ResultProcessor.processItem(ItemPool.SKELETON_KEY, -1);
        }
        if (ChoiceManager.lastDecision != 8) {
          Preferences.increment("_lastDailyDungeonRoom", 1);
        }
        return;

      case 693:
        // It's Almost Certainly a Trap
        if (ChoiceManager.lastDecision != 3) {
          Preferences.increment("_lastDailyDungeonRoom", 1);
        }
        return;

      case 699:
        // Lumber-related Pun
        if (text.contains("hand him the branch")) {
          // Marty's eyes widen when you hand him the
          // branch from the Great Tree.
          ResultProcessor.processItem(ItemPool.GREAT_TREE_BRANCH, -1);
        } else if (text.contains("hand him the rust")) {
          // At first Marty looks disappointed when you
          // hand him the rust-spotted, rotten-handled
          // axe, but after a closer inspection he gives
          // an impressed whistle.
          ResultProcessor.processItem(ItemPool.PHIL_BUNION_AXE, -1);
        } else if (text.contains("hand him the bouquet")) {
          // Marty looks delighted when you hand him the
          // bouquet of swamp roses.
          ResultProcessor.processItem(ItemPool.SWAMP_ROSE_BOUQUET, -1);
        }
        return;

      case 700:
        // Delirium in the Cafeterium
        Preferences.increment("_kolhsAdventures", 1);
        return;

      case 703:
        // Mer-kin dreadscroll
        if (text.contains("I guess you're the Mer-kin High Priest now")) {
          Preferences.setString("merkinQuestPath", "scholar");
          ResultProcessor.processItem(ItemPool.DREADSCROLL, -1);
          return;
        }
        return;

      case 709:
        // You Beat Shub to a Stub, Bub
      case 713:
        // You Brought Her To Her Kn-kn-kn-kn-knees, Knees.
      case 717:
        // Over. Over Now.
        Preferences.setString("merkinQuestPath", "done");
        return;

      case 720:
        // The Florist Friar's Cottage
        FloristRequest.parseResponse(urlString, text);
        return;

      case 721:
        // The Cabin in the Dreadsylvanian Woods
        if (ChoiceManager.lastDecision == 3) {
          // Try the Attic

          // You use your skeleton key to unlock the padlock on the attic trap door.
          // Then you use your legs to climb the ladder into the attic.
          // Then you use your stupidity to lose the skeleton key.  Crap.

          if (text.contains("lose the skeleton key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 722:
        // The Kitchen in the Woods
        if (ChoiceManager.lastDecision == 2) {
          // Screw around with the flour mill
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.OLD_DRY_BONE, -1));
          }
        }
        return;

      case 723:
        // What Lies Beneath (the Cabin)
        if (ChoiceManager.lastDecision == 3) {
          // Check out the lockbox
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.REPLICA_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 4) {
          // Stick a wax banana in the lock
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.WAX_BANANA, -1));
          }
        }
        return;

      case 725:
        // Tallest Tree in the Forest
        if (ChoiceManager.lastDecision == 2) {
          // Check out the fire tower

          // You climb the rope ladder and use your skeleton key to
          // unlock the padlock on the door leading into the little room
          // at the top of the watchtower. Then you accidentally drop
          // your skeleton key and lose it in a pile of leaves. Rats.

          if (text.contains("you accidentally drop your skeleton key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }

        return;

      case 729:
        // Below the Roots
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 730:
        // Hot Coals
        if (ChoiceManager.lastDecision == 3) {
          // Melt down an old ball and chain
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.OLD_BALL_AND_CHAIN, -1));
          }
        }
        return;

      case 733:
        // Dreadsylvanian Village Square
        if (ChoiceManager.lastDecision == 1) {
          // The schoolhouse

          // You try the door of the schoolhouse, but it's locked. You
          // try your skeleton key in the lock, but it works. I mean and
          // it works. But it breaks. That was the but.

          if (text.contains("But it breaks")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 737:
        // The Even More Dreadful Part of Town
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 739:
        // The Tinker's. Damn.
        if (ChoiceManager.lastDecision == 2) {
          // Make a key using the wax lock impression
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.WAX_LOCK_IMPRESSION, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.INTRICATE_MUSIC_BOX_PARTS, -1));
          }
        } else if (ChoiceManager.lastDecision == 3) {
          // Polish the moon-amber
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.MOON_AMBER, -1));
          }
        } else if (ChoiceManager.lastDecision == 4) {
          // Assemble a clockwork bird
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.INTRICATE_MUSIC_BOX_PARTS, -3));
          }
        }
        return;

      case 741:
        // The Old Duke's Estate
        if (ChoiceManager.lastDecision == 3) {
          // Make your way to the master suite

          // You find the door to the old Duke's master bedroom and
          // unlock it with your skeleton key.

          if (text.contains("unlock it with your skeleton key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 743:
        // No Quarter
        if (ChoiceManager.lastDecision == 2) {
          // Make a shepherd's pie
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREAD_TARRAGON, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.BONE_FLOUR, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADFUL_ROAST, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.STINKING_AGARICUS, -1));
          }
        }
        return;

      case 744:
        // The Master Suite -- Sweet!
        if (ChoiceManager.lastDecision == 3) {
          // Mess with the loom
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_THREAD, -10));
          }
        }
        return;

      case 745:
        // This Hall is Really Great
        if (ChoiceManager.lastDecision == 1) {
          // Head to the ballroom

          // You unlock the door to the ballroom with your skeleton
          // key. You open the door, and are so impressed by the site of
          // the elegant ballroom that you drop the key down a nearby
          // laundry chute.

          if (text.contains("you drop the key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 746:
        // The Belle of the Ballroom
        if (ChoiceManager.lastDecision == 2) {
          // Trip the light fantastic

          // You twirl around on the dance floor to music only you can
          // hear, your muddy skirt whirling around you filthily. You get
          // so caught up in the twirling that you drop your seed pod. It
          // breaks open, spreading weed seeds all over your skirt, which
          // immediately take root and grow.

          if (text.contains("spreading weed seeds all over your skirt")) {
            EquipmentManager.discardEquipment(ItemPool.MUDDY_SKIRT);
            EquipmentManager.setEquipment(
                EquipmentManager.PANTS, ItemPool.get(ItemPool.WEEDY_SKIRT, 1));
          }
        }
        return;

      case 749:
        // Tower Most Tall
        if (ChoiceManager.lastDecision == 1) {
          // Go to the laboratory

          // You use your skeleton key to unlock the door to the
          // laboratory. Unfortunately, the lock is electrified, and it
          // incinerates the key shortly afterwards.

          if (text.contains("it incinerates the key")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1));
          }
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 750:
        // Working in the Lab, Late One Night
        if (ChoiceManager.lastDecision == 4) {
          // Use the still
          if (text.contains("You acquire")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.BLOOD_KIWI, -1));
            ResultProcessor.processResult(ItemPool.get(ItemPool.EAU_DE_MORT, -1));
          }
        }
        return;

      case 753:
        // The Dreadsylvanian Dungeon
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GHOST_PENCIL, -1));
        }
        return;

      case 762:
        // Try New Extra-Strength Anvil
        // All of the options that craft something use the same ingredients
        if (text.contains("You acquire")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.COOL_IRON_INGOT, -1));
          ResultProcessor.processResult(ItemPool.get(ItemPool.WARM_FUR, -1));
        }
        return;

      case 772:
        // Saved by the Bell
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_kolhsSchoolSpirited", true);
          Preferences.increment("kolhsTotalSchoolSpirited", 1);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("_kolhsPoeticallyLicenced", true);
        } else if (ChoiceManager.lastDecision == 3) {
          // You walk into the Yearbook Club and collar the kid with all
          // the camera equipment from yesterday. "Let me check your
          // memory card," he says, plugging the camera into a computer.
          // "Yup! You got it! Nice work. Here's your reward -- a nice
          // new accessory for that camera! If you're interested, now we
          // need a picture of a <b>monster</b>. You up for it?"
          //
          // You walk back into the Yearbook Club, a little tentatively.
          // "All right! Let's see what you've got!" the camera kid
          // says, and plugs your camera into a computer. "Aw, man, you
          // didn't get it? Well, I'll give you another chance.  If you
          // can still get us a picture of a <b>monster</b> and bring it
          // in tomorrow, you're still in the Club."
          //
          // You poke your head into the Yearbook Club room, but the
          // camera kid's packing up all the equipment and putting it
          // away. "Sorry, gotta go," he says, "but remember, you've
          // gotta get a picture of a <b>monster</b> for tomorrow, all
          // right? We're counting on you."

          if (text.contains("You got it!")) {
            Preferences.setString("yearbookCameraTarget", "");
            Preferences.setBoolean("yearbookCameraPending", false);
            Preferences.increment("yearbookCameraUpgrades", 1, 20, false);
            if (KoLCharacter.getAscensions()
                != Preferences.getInteger("lastYearbookCameraAscension")) {
              Preferences.setInteger("lastYearbookCameraAscension", KoLCharacter.getAscensions());
              Preferences.increment("yearbookCameraAscensions", 1, 20, false);
            }
          }

          Matcher matcher = YEARBOOK_TARGET_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("yearbookCameraTarget", matcher.group(1));
          }
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_kolhsCutButNotDried", true);
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("_kolhsIsskayLikeAnAshtray", true);
        }
        if (ChoiceManager.lastDecision != 8) {
          Preferences.increment("_kolhsSavedByTheBell", 1);
        }
        return;

      case 778:
        // If You Could Only See
        if (ChoiceManager.lastDecision != 6) {
          Preferences.setBoolean("_tonicDjinn", true);
          if (!text.contains("already had a wish today")) {
            ResultProcessor.processResult(ItemPool.get(ItemPool.TONIC_DJINN, -1));
          }
        }
        return;

      case 780:
        // Action Elevator
        if (ChoiceManager.lastDecision == 1 && text.contains("penthouse is empty now")) {
          if (Preferences.getInteger("hiddenApartmentProgress") < 7) {
            Preferences.setInteger("hiddenApartmentProgress", 7);
          }
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setInteger("relocatePygmyLawyer", KoLCharacter.getAscensions());
        }
        return;

      case 781:
        // Earthbound and Down
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenApartmentProgress", 1);
          QuestDatabase.setQuestProgress(Quest.CURSES, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.MOSS_COVERED_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenApartmentProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SIX_BALL, -1));
        }
        return;

      case 783:
        // Water You Dune
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenHospitalProgress", 1);
          QuestDatabase.setQuestProgress(Quest.DOCTOR, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.DRIPPING_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenHospitalProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.TWO_BALL, -1));
        }
        return;

      case 785:
        // Air Apparent
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenOfficeProgress", 1);
          QuestDatabase.setQuestProgress(Quest.BUSINESS, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.CRACKLING_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenOfficeProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.ONE_BALL, -1));
        }
        return;

      case 786:
        // Working Holiday
        if (ChoiceManager.lastDecision == 1 && text.contains("boss's office is empty")) {
          if (Preferences.getInteger("hiddenOfficeProgress") < 7) {
            Preferences.setInteger("hiddenOfficeProgress", 7);
          }
        }
        // if you don't get the expected binder clip, don't have one, and don't have a mcclusky
        // file, you must have unlocked the boss at least
        else if (ChoiceManager.lastDecision == 2
            && !text.contains("boring binder clip")
            && InventoryManager.getCount(ChoiceManager.MCCLUSKY_FILE) == 0
            && InventoryManager.getCount(ChoiceManager.BINDER_CLIP) == 0
            && Preferences.getInteger("hiddenOfficeProgress") < 6) {
          Preferences.setInteger("hiddenOfficeProgress", 6);
        }
        return;

      case 787:
        // Fire when Ready
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("hiddenBowlingAlleyProgress", 1);
          QuestDatabase.setQuestProgress(Quest.SPARE, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SCORCHED_STONE_SPHERE, -1));
          Preferences.setInteger("hiddenBowlingAlleyProgress", 8);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.FIVE_BALL, -1));
        }
        return;

      case 788:
        // Life is Like a Cherry of Bowls
        if (ChoiceManager.lastDecision == 1
            && text.contains("without a frustrated ghost to torment")) {
          if (Preferences.getInteger("hiddenBowlingAlleyProgress") < 7) {
            Preferences.setInteger("hiddenBowlingAlleyProgress", 7);
          }
        }
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.BOWLING_BALL);
          int bowlCount = Preferences.getInteger("hiddenBowlingAlleyProgress");
          if (bowlCount < 6) {
            Preferences.setInteger(
                "hiddenBowlingAlleyProgress", (bowlCount < 2 ? 2 : bowlCount + 1));
          }
        }
        return;

      case 789:
        // Where Does The Lone Ranger Take His Garbagester?
        if (ChoiceManager.lastDecision == 2) {
          Preferences.setInteger("relocatePygmyJanitor", KoLCharacter.getAscensions());
        }
        return;

      case 801:
        // A Reanimated Conversation
        if (ChoiceManager.lastDecision == 6) {
          Preferences.setInteger("reanimatorArms", 0);
          Preferences.setInteger("reanimatorLegs", 0);
          Preferences.setInteger("reanimatorSkulls", 0);
          Preferences.setInteger("reanimatorWeirdParts", 0);
          Preferences.setInteger("reanimatorWings", 0);
        }
        return;

      case 805:
        // A Sietch in Time
        int gnasirProgress = Preferences.getInteger("gnasirProgress");

        // Annoyingly, the option numbers change as you turn
        // things in. Therefore, we must look at response text

        if (text.contains("give the stone rose to Gnasir")) {
          ResultProcessor.removeItem(ItemPool.STONE_ROSE);
          gnasirProgress |= 1;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        } else if (text.contains("hold up the bucket of black paint")) {
          ResultProcessor.removeItem(ItemPool.BLACK_PAINT);
          gnasirProgress |= 2;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        } else if (text.contains("hand Gnasir the glass jar")) {
          ResultProcessor.removeItem(ItemPool.KILLING_JAR);
          gnasirProgress |= 4;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        }
        // You hand him the pages, and he shuffles them into their correct order and inspects them
        // carefully.
        else if (text.contains("hand him the pages")) {
          ResultProcessor.processItem(ItemPool.WORM_RIDING_MANUAL_PAGE, -15);
          gnasirProgress |= 8;
          Preferences.setInteger("gnasirProgress", gnasirProgress);
        }
        return;

      case 812:
        if (ChoiceManager.lastDecision == 1) {
          Matcher matcher = ChoiceManager.UNPERM_PATTERN.matcher(text);
          if (matcher.find()) {
            KoLCharacter.removeAvailableSkill(matcher.group(1));
            Preferences.increment("bankedKarma", Integer.parseInt(matcher.group(2)));
          }
        }
        return;
      case 821:
        // LP-ROM burner
        if (ChoiceManager.lastDecision == 1) {
          HaciendaManager.parseRecording(urlString, text);
        }
        break;

      case 835:
        // Barely Tales
        if (ChoiceManager.lastDecision != 0) {
          Preferences.setBoolean("_grimBuff", true);
        }
        break;

      case 836:
        // Adventures Who Live in Ice Houses...
        if (ChoiceManager.lastDecision == 1) {
          BanishManager.removeBanishByBanisher("ice house");
        }
        break;

      case 851:
        // Shen Copperhead, Nightclub Owner
        QuestDatabase.setQuestProgress(Quest.SHEN, "step1");
        Preferences.setInteger("shenInitiationDay", KoLCharacter.getCurrentDays());
        if (Preferences.getString("shenQuestItem") == "") {
          // We didn't recognise quest text before accepting quest, so get it from quest log
          RequestThread.postRequest(new QuestLogRequest());
        }

        break;

      case 852:
        // Shen Copperhead, Jerk
        // Deliberate fallthrough
      case 853:
        { // Shen Copperhead, Huge Jerk
          Matcher matcher = ChoiceManager.SHEN_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("shenQuestItem", matcher.group(1));
          }
        }
        // Deliberate fallthrough
      case 854:
        // Shen Copperhead, World's Biggest Jerk
        QuestDatabase.advanceQuest(Quest.SHEN);
        if (ChoiceManager.lastChoice == 854) {
          Preferences.setString("shenQuestItem", "");
        }

        // You will have exactly one of these items to ger rid of
        ResultProcessor.removeItem(ItemPool.FIRST_PIZZA);
        ResultProcessor.removeItem(ItemPool.LACROSSE_STICK);
        ResultProcessor.removeItem(ItemPool.EYE_OF_THE_STARS);
        ResultProcessor.removeItem(ItemPool.STANKARA_STONE);
        ResultProcessor.removeItem(ItemPool.MURPHYS_FLAG);
        ResultProcessor.removeItem(ItemPool.SHIELD_OF_BROOK);
        break;

      case 872:
        // Drawn Onward - Handle quest in Ed
        if (text.contains("Rot in a jar of dog paws!")) {
          QuestDatabase.setQuestProgress(Quest.PALINDOME, QuestDatabase.FINISHED);
          ResultProcessor.removeItem(ItemPool.ED_FATS_STAFF);
          if (InventoryManager.getCount(ItemPool.ED_EYE) == 0
              && InventoryManager.getCount(ItemPool.ED_AMULET) == 0) {
            QuestDatabase.setQuestProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED);
          }
        }
        break;

      case 890:
        // Lights Out in the Storage Room
        if (text.contains("BUT AIN'T NO ONE CAN GET A STAIN OUT LIKE OLD AGNES!")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Laundry Room");
        }
        break;

      case 891:
        // Lights Out in the Laundry Room
        if (text.contains("DO YOU SEE THE STAIN UPON MY TOWEL?")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Bathroom");
        }
        break;

      case 892:
        // Lights Out in the Bathroom
        if (text.contains("THE STAIN HAS BEEN LIFTED")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Kitchen");
        }
        break;

      case 893:
        // Lights Out in the Kitchen
        if (text.contains("If You Give a Demon a Brownie")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Library");
        }
        break;

      case 894:
        // Lights Out in the Library
        if (text.contains("If You Give a Demon a Brownie")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Ballroom");
        }
        break;

      case 895:
        // Lights Out in the Ballroom
        if (text.contains("The Flowerbed of Unearthly Delights")
            && !Preferences.getString("nextSpookyravenElizabethRoom").equals("none")) {
          Preferences.setString("nextSpookyravenElizabethRoom", "The Haunted Gallery");
        }
        break;

      case 896:
        // Lights Out in the Gallery

        // The correct option leads to a combat with Elizabeth.
        // If you win, we will set "nextSpookyravenElizabethRoom" to "none"
        break;

      case 897:
        // Lights Out in the Bedroom
        if (text.contains("restock his medical kit in the nursery")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Nursery");
        }
        break;

      case 898:
        // Lights Out in the Nursery
        if (text.contains("This afternoon we're burying Crumbles")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Conservatory");
        }
        break;

      case 899:
        // Lights Out in the Conservatory
        if (text.contains("Crumbles isn't buried very deep")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Billiards Room");
        }
        break;

      case 900:
        // Lights Out in the Billiards Room
        if (text.contains("The wolf head has a particularly nasty expression on its face")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Wine Cellar");
        }
        break;

      case 901:
        // Lights Out in the Wine Cellar
        if (text.contains("Crumbles II (Wolf)")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Boiler Room");
        }
        break;

      case 902:
        // Lights Out in the Boiler Room
        if (text.contains("CRUMBLES II")
            && !Preferences.getString("nextSpookyravenStephenRoom").equals("none")) {
          Preferences.setString("nextSpookyravenStephenRoom", "The Haunted Laboratory");
        }
        break;

      case 903:
        // Lights Out in the Laboratory

        // The correct option leads to a combat with Stephen.
        // If you win, we will set "nextSpookyravenStephenRoom" to "none"
        break;

      case 915:
        // Choice 915 is Et Tu, Buff Jimmy?
        if (text.contains("skinny mushroom girls")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.STARTED);
        } else if (text.contains(
            "But here's a few Beach Bucks as a token of my changes in gratitude")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.PENCIL_THIN_MUSHROOM, -10);
        } else if (text.contains("not really into moving out of this hammock")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.STARTED);
          Preferences.setInteger("buffJimmyIngredients", 0);
        } else if (text.contains("So I'll just give you some Beach Bucks instead")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.FINISHED);
          Preferences.setInteger("buffJimmyIngredients", 0);
          ResultProcessor.processItem(ItemPool.CHEESEBURGER_RECIPE, -1);
        } else if (text.contains("sons of sons of sailors are")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_SALT, QuestDatabase.STARTED);
        } else if (text.contains("So here's some Beach Bucks instead")) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_SALT, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.SAILOR_SALT, -50);
        }
        break;
      case 916:
        // Choice 916 is Taco Dan's Taco Stand's Taco Dan
        if (text.contains("find those receipts")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.STARTED);
        } else if (text.contains("Here's a little Taco Dan's Taco Stand gratitude for ya")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.TACO_DAN_RECEIPT, -10);
        } else if (text.contains("fill it up with as many cocktail drippings")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.STARTED);
          Preferences.setInteger("tacoDanCocktailSauce", 0);
        } else if (text.contains("sample of Taco Dan's Taco Stand's Tacoriffic Cocktail Sauce")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.FINISHED);
          Preferences.setInteger("tacoDanCocktailSauce", 0);
          ResultProcessor.processItem(ItemPool.TACO_DAN_SAUCE_BOTTLE, -1);
        } else if (text.contains("get enough taco fish")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_FISH, QuestDatabase.STARTED);
          Preferences.setInteger("tacoDanFishMeat", 0);
        } else if (text.contains("batch of those Taco Dan's Taco Stand's Taco Fish Tacos")) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_FISH, QuestDatabase.FINISHED);
          Preferences.setInteger("tacoDanFishMeat", 0);
        }
        break;
      case 917:
        // Choice 917 is Do You Even Brogurt
        if (text.contains("need about ten shots of it")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_BACTERIA, QuestDatabase.STARTED);
          Preferences.setInteger("brodenBacteria", 0);
        } else if (text.contains("YOLO cup to spit the bacteria into")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_BACTERIA, QuestDatabase.FINISHED);
          Preferences.setInteger("brodenBacteria", 0);
        } else if (text.contains("loan you my sprinkle shaker to fill up")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.STARTED);
          Preferences.setInteger("brodenSprinkles", 0);
        } else if (text.contains("can sell some <i>deluxe</i> brogurts")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.FINISHED);
          Preferences.setInteger("brodenSprinkles", 0);
          ResultProcessor.processItem(ItemPool.SPRINKLE_SHAKER, -1);
        } else if (text.contains("There were like fifteen of these guys")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_DEBT, QuestDatabase.STARTED);
        } else if (text.contains("And they all had broupons, huh")) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_DEBT, QuestDatabase.FINISHED);
          ResultProcessor.processItem(ItemPool.BROUPON, -15);
        }
        break;
      case 918:
        // Yachtzee!
        if (ChoiceManager.lastDecision == 3 && text.contains("You open the captain's door")) {
          int beads = Math.min(InventoryManager.getCount(ItemPool.MOIST_BEADS), 100);
          ResultProcessor.processResult(ItemPool.get(ItemPool.MOIST_BEADS, -beads));
        }
        break;
      case 919:
        // Choice 919 is Break Time!
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You've already thoroughly")) {
            Preferences.setInteger("_sloppyDinerBeachBucks", 4);
          } else {
            Preferences.increment("_sloppyDinerBeachBucks", 1);
          }
        }
        break;
      case 920:
        // Choice 920 is Eraser
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.JIMMY_SALT, QuestDatabase.UNSTARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.TACO_DAN_FISH, QuestDatabase.UNSTARTED);
        } else if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestProgress(Quest.BRODEN_BACTERIA, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.BRODEN_DEBT, QuestDatabase.UNSTARTED);
        }
        if (ChoiceManager.lastDecision != 4) {
          ResultProcessor.processItem(ItemPool.MIND_DESTROYER, -1);
        }
        break;

      case 930:
        // Another Errand I Mean Quest
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestIfBetter(Quest.CITADEL, QuestDatabase.STARTED);
        }
        break;

      case 932:
        // No Whammies
        if (text.contains("steel your nerves for what lies ahead")) {
          QuestDatabase.setQuestProgress(Quest.CITADEL, "step9");
        }
        break;

      case 967:
        // The Thunder Rolls...
        if (ChoiceManager.lastDecision != 8) {
          ResultProcessor.removeItem(ItemPool.THUNDER_THIGH);
        }
        break;

      case 968:
        // The Rain Falls Down With Your Help...
        if (ChoiceManager.lastDecision != 8) {
          ResultProcessor.removeItem(ItemPool.AQUA_BRAIN);
        }
        break;

      case 969:
        // And The Lightning Strikes...
        if (ChoiceManager.lastDecision != 8) {
          ResultProcessor.removeItem(ItemPool.LIGHTNING_MILK);
        }
        break;

      case 970:
        // Rainy Fax Dreams on your Wedding Day
        if (ChoiceManager.lastDecision == 1) {
          EncounterManager.ignoreSpecialMonsters();
          KoLAdventure.lastVisitedLocation = null;
          KoLAdventure.lastLocationName = null;
          KoLAdventure.lastLocationURL = urlString;
          KoLAdventure.setLastAdventure("None");
          KoLAdventure.setNextAdventure("None");
          GenericRequest.itemMonster = "Rain Man";
        }
        break;

      case 984:
        {
          // A Radio on a Beach
          // Clear quests when accepting a new one as you can only have one
          if (text.contains("your best paramilitary-sounding radio lingo")) {
            QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED);
            QuestDatabase.setQuestProgress(Quest.GORE, QuestDatabase.UNSTARTED);
            QuestDatabase.setQuestProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED);
          }
          // Also clear repeatable quests if there is no quest active at the radio
          if (text.contains("Maybe try again tomorrow")) {
            QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED);
            QuestDatabase.setQuestProgress(Quest.GORE, QuestDatabase.UNSTARTED);
            QuestDatabase.setQuestProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED);
          }
          // EVE quest started
          else if (text.contains("navigation protocol")) {
            QuestDatabase.setQuestProgress(Quest.EVE, QuestDatabase.STARTED);
            Preferences.setString("EVEDirections", "LLRLR0");
          }
          // EVE quest finished
          else if (text.contains("a tiny parachute")) {
            QuestDatabase.setQuestProgress(Quest.EVE, QuestDatabase.FINISHED);
            Preferences.resetToDefault("EVEDirections");
          }
          // Jungle Pun quest finished (start handled in ResultProcessor)
          else if (text.contains(
              "tape recorder self-destructs with a shower of sparks and a puff of smoke")) {
            EquipmentManager.discardEquipment(ItemPool.MINI_CASSETTE_RECORDER);
            ResultProcessor.removeItem(ItemPool.MINI_CASSETTE_RECORDER);
            QuestDatabase.setQuestProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED);
            Preferences.resetToDefault("junglePuns");
          }
          // Gore quest finished (start handled in ResultProcessor)
          else if (text.contains("bucket came from")) {
            EquipmentManager.discardEquipment(ItemPool.GORE_BUCKET);
            ResultProcessor.removeItem(ItemPool.GORE_BUCKET);
            QuestDatabase.setQuestProgress(Quest.GORE, QuestDatabase.UNSTARTED);
            Preferences.resetToDefault("goreCollected");
          }
          // Clipper quest finished (start handled in ResultProcessor)
          else if (text.contains("return the fingernails and the clippers")) {
            ResultProcessor.removeItem(ItemPool.FINGERNAIL_CLIPPERS);
            QuestDatabase.setQuestProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED);
            Preferences.resetToDefault("fingernailsClipped");
          }
          // Fake Medium quest started
          else if (text.contains("maximal discretion")) {
            QuestDatabase.setQuestProgress(Quest.FAKE_MEDIUM, QuestDatabase.STARTED);
          }
          // Fake Medium quest finished
          else if (text.contains("toss the device into the ocean")) {
            ResultProcessor.removeItem(ItemPool.ESP_COLLAR);
            QuestDatabase.setQuestProgress(Quest.FAKE_MEDIUM, QuestDatabase.FINISHED);
          }
          // Serum quest started
          else if (text.contains("wonder how many vials they want")) {
            if (InventoryManager.getCount(ItemPool.EXPERIMENTAL_SERUM_P00) >= 5) {
              QuestDatabase.setQuestProgress(Quest.SERUM, "step1");
            } else {
              QuestDatabase.setQuestProgress(Quest.SERUM, QuestDatabase.STARTED);
            }
          }
          // Serum quest finished
          else if (text.contains("drop the vials into it")) {
            QuestDatabase.setQuestProgress(Quest.SERUM, QuestDatabase.FINISHED);
          }
          // Smokes quest started
          else if (text.contains("acquire cigarettes")) {
            QuestDatabase.setQuestProgress(Quest.SMOKES, QuestDatabase.STARTED);
          }
          // Smokes quest finished
          else if (text.contains("cigarettes with a grappling gun")) {
            QuestDatabase.setQuestProgress(Quest.SMOKES, QuestDatabase.FINISHED);
          }
          // Out of Order quest finished
          else if (text.contains("takes your nifty new watch")) {
            EquipmentManager.discardEquipment(ItemPool.GPS_WATCH);
            ResultProcessor.removeItem(ItemPool.GPS_WATCH);
            ResultProcessor.removeItem(ItemPool.PROJECT_TLB);
            QuestDatabase.setQuestProgress(Quest.OUT_OF_ORDER, QuestDatabase.FINISHED);
          }
          // Can't parse quest due to static so visit quest log
          else {
            RequestThread.postRequest(new QuestLogRequest());
          }
          break;
        }

      case 986:
        // Control Panel
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 9) {
          Preferences.setBoolean("_controlPanelUsed", true);
          if (!text.contains("minimum of 24 hours")) {
            Preferences.increment("controlPanelOmega", 11, 100, false);
          }
        }
        break;
      case 987:
        // The Post-Apocalyptic Survivor Encampment
        if (!text.contains("accept your donation")) {
          break;
        }
        int qty = -1;
        if (urlString.contains("giveten")) {
          qty = -10;
        }
        // Declare the pattern here instead of globally because this is available infrequently
        Pattern encampmentPattern = Pattern.compile("whichfood=(\\d+)");
        Matcher encampmentMatcher = encampmentPattern.matcher(urlString);
        if (encampmentMatcher.find()) {
          int encampmentId = StringUtilities.parseInt(encampmentMatcher.group(1));
          ResultProcessor.processItem(encampmentId, qty);
        }
        break;

      case 988:
        // The Containment Unit
        String containment = Preferences.getString("EVEDirections");
        if (containment.length() != 6) {
          break;
        }
        if (text.contains("another pair of doors")) {
          int progress = StringUtilities.parseInt(containment.substring(5, 6));
          if (progress < 0 && progress > 4) {
            break;
          }
          progress++;
          Preferences.setString("EVEDirections", containment.substring(0, 5) + progress);
        } else {
          Preferences.setString("EVEDirections", containment.substring(0, 5) + "0");
        }
        break;

      case 991:
        // Build a Crimbot!
        if (lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.CRIMBONIUM_FUEL_ROD);
        }
        break;

      case 993:
        // Tales of Spelunking
        if (lastDecision == 1) {
          KoLCharacter.enterLimitmode(Limitmode.SPELUNKY);
        }
        break;

      case 994:
        // Hide a gift!
        if (text.contains("You hide")) {
          HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>(3);
          HashMap<Integer, Integer> qtyMap = new HashMap<Integer, Integer>(3);
          int index;
          int id;
          int giftQty;

          Matcher idMatcher = ChoiceManager.ITEMID_PATTERN.matcher(urlString);
          while (idMatcher.find()) {
            index = StringUtilities.parseInt(idMatcher.group(1));
            if (index < 1) continue;
            id = StringUtilities.parseInt(idMatcher.group(2));
            if (id < 1) continue;
            idMap.put(index, id);
          }

          Matcher qtyMatcher = ChoiceManager.QTY_PATTERN.matcher(urlString);
          while (qtyMatcher.find()) {
            index = StringUtilities.parseInt(qtyMatcher.group(1));
            if (index < 1) continue;
            giftQty = StringUtilities.parseInt(qtyMatcher.group(2));
            if (giftQty < 1) continue;
            qtyMap.put(index, giftQty);
          }

          for (int i = 1; i <= 3; i++) {
            Integer itemId = idMap.get(i);
            Integer giftQuantity = qtyMap.get(i);
            if (itemId == null || giftQuantity == null) continue;
            ResultProcessor.processResult(ItemPool.get(itemId, -giftQuantity));
          }
          ResultProcessor.removeItem(ItemPool.SNEAKY_WRAPPING_PAPER);
        }
        break;

      case 998:
        if (text.contains("confiscate your deuces")) {
          int removeDeuces = ChoiceManager.lastDecision - 1;
          ResultProcessor.processResult(ItemPool.get(ItemPool.SLEEVE_DEUCE, removeDeuces));
          break;
        }
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.POCKET_ACE);
        }
        break;

      case 999:
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_shrubDecorated", true);
          Pattern topperPattern = Pattern.compile("topper=(\\d)");
          Pattern lightsPattern = Pattern.compile("lights=(\\d)");
          Pattern garlandPattern = Pattern.compile("garland=(\\d)");
          Pattern giftPattern = Pattern.compile("gift=(\\d)");
          int decoration;

          Matcher matcher = topperPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1:
                Preferences.setString("shrubTopper", "Muscle");
                break;
              case 2:
                Preferences.setString("shrubTopper", "Mysticality");
                break;
              case 3:
                Preferences.setString("shrubTopper", "Moxie");
                break;
            }
          }

          matcher = lightsPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1:
                Preferences.setString("shrubLights", "prismatic");
                break;
              case 2:
                Preferences.setString("shrubLights", "Hot");
                break;
              case 3:
                Preferences.setString("shrubLights", "Cold");
                break;
              case 4:
                Preferences.setString("shrubLights", "Stench");
                break;
              case 5:
                Preferences.setString("shrubLights", "Spooky");
                break;
              case 6:
                Preferences.setString("shrubLights", "Sleaze");
                break;
            }
          }

          matcher = garlandPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1:
                Preferences.setString("shrubGarland", "HP");
                break;
              case 2:
                Preferences.setString("shrubGarland", "PvP");
                break;
              case 3:
                Preferences.setString("shrubGarland", "blocking");
                break;
            }
          }

          matcher = giftPattern.matcher(urlString);
          if (matcher.find()) {
            decoration = StringUtilities.parseInt(matcher.group(1));
            switch (decoration) {
              case 1:
                Preferences.setString("shrubGifts", "yellow");
                break;
              case 2:
                Preferences.setString("shrubGifts", "meat");
                break;
              case 3:
                Preferences.setString("shrubGifts", "gifts");
                break;
            }
          }
        }
        break;

      case 1002:
        // Temple of the Legend in the Hidden City - Handle quest in Ed
        if (text.contains("The spectre nods emphatically")) {
          QuestDatabase.setQuestProgress(Quest.WORSHIP, QuestDatabase.FINISHED);
          ResultProcessor.removeItem(ItemPool.ED_AMULET);
          if (InventoryManager.getCount(ItemPool.ED_EYE) == 0
              && InventoryManager.getCount(ItemPool.ED_FATS_STAFF) == 0) {
            QuestDatabase.setQuestProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED);
          }
        }
        break;

      case 1003:
        // Test Your Might And Also Test Other Things
        SorceressLairManager.parseContestBooth(ChoiceManager.lastDecision, text);
        break;

      case 1005: // 'Allo
      case 1008: // Pooling Your Resources
      case 1011: // Of Mouseholes and Manholes
        SorceressLairManager.parseMazeTrap(ChoiceManager.lastChoice, text);
        break;

      case 1013: // Mazel Tov!
        // Then you both giggle and head through the exit at the same time.
        QuestDatabase.setQuestProgress(Quest.FINAL, "step5");
        break;

      case 1015: // The Mirror in the Tower has the View that is True
        QuestDatabase.setQuestProgress(Quest.FINAL, "step10");
        break;

      case 1022: // Meet Frank
        // Frank bobs his head toward the hedge maze in front of you.
        QuestDatabase.setQuestProgress(Quest.FINAL, "step4");
        break;

      case 1023: // Like a Bat Into Hell
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.setLimitmode(Limitmode.ED);
        }
        break;

      case 1027: // The End of the Tale of Spelunking
        if (ChoiceManager.lastDecision == 1) {
          // Remove all virtual items from inventory/tally
          SpelunkyRequest.resetItems();
        }
        break;

      case 1028: // A Shop
      case 1029: // An Old Clay Pot
      case 1030: // It's a Trap!  A Dart Trap.
      case 1031: // A Tombstone
      case 1032: // It's a Trap!  A Tiki Trap.
      case 1033: // A Big Block of Ice
      case 1034: // A Landmine
      case 1035: // A Crate
      case 1036: // Idolatry
      case 1037: // It's a Trap!  A Smashy Trap.
      case 1038: // A Wicked Web
      case 1039: // A Golden Chest
      case 1040: // It's Lump. It's Lump.
      case 1041: // Spelunkrifice
      case 1045: // Hostile Work Environment
        SpelunkyRequest.parseChoice(ChoiceManager.lastChoice, text, ChoiceManager.lastDecision);
        break;

      case 1042: // Pick a Perk
        SpelunkyRequest.upgrade(ChoiceManager.lastDecision);
        break;

      case 1044:
        // The Gates of Hell
        if (text.contains("unlock the padlock")) {
          SpelunkyRequest.unlock("Hell", "Hell");
        }
        break;

      case 1049:
        // Tomb of the Unknown Your Class Here
        if (text.contains("Also in this room is a ghost")) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step1");
        } else if (text.contains("You acquire")) {
          AscensionClass ascensionClass = KoLCharacter.getAscensionClass();
          int starterWeaponId = ascensionClass == null ? -1 : ascensionClass.getStarterWeapon();
          ResultProcessor.processItem(starterWeaponId, -1);
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step4");
        }
        break;

      case 1052:
        // Underworld Body Shop
        Matcher skillidMatcher = ChoiceManager.URL_SKILLID_PATTERN.matcher(urlString);
        if (skillidMatcher.find()) {
          int cost = 0;
          switch (StringUtilities.parseInt(skillidMatcher.group(1))) {
            case 30:
              cost = 5;
              break;
            case 31:
            case 36:
            case 39:
            case 40:
            case 43:
            case 44:
              cost = 10;
              break;
            case 32:
              cost = 15;
              break;
            case 33:
            case 37:
            case 38:
            case 41:
            case 42:
            case 45:
            case 48:
              cost = 20;
              break;
            case 34:
              cost = 25;
              break;
            case 28:
            case 29:
            case 35:
            case 46:
              cost = 30;
              break;
          }
          ResultProcessor.processResult(ItemPool.get(ItemPool.KA_COIN, -cost));
        }
        break;

      case 1053: // The Servants' Quarters
        EdServantData.manipulateServants(request, text);
        break;

      case 1056:
        // Now It's Dark
        // Twin Peak fourth choice
        if (text.contains("When the lights come back")) {
          // the other three must be completed at this point.
          Preferences.setInteger("twinPeakProgress", 15);
        }
        break;

      case 1057:
        // A Stone Shrine
        if (text.contains("shatter the")) {
          KoLCharacter.setHippyStoneBroken(true);
        }
        break;

      case 1059:
        // Helping Make Ends Meat
        if (text.contains("excitedly takes the check")) {
          QuestDatabase.setQuestProgress(Quest.MEATSMITH, QuestDatabase.FINISHED);
          ResultProcessor.removeItem(ItemPool.MEATSMITH_CHECK);
        } else if (text.contains("skeleton store is right next door")
            || text.contains("I'll be here")) {
          QuestDatabase.setQuestProgress(Quest.MEATSMITH, QuestDatabase.STARTED);
        }
        break;

      case 1060:
        // Temporarily Out of Skeletons
        if (text.contains("it snaps off")) {
          ResultProcessor.removeItem(ItemPool.SKELETON_KEY);
        }
        break;

      case 1061:
        // Heart of Madness
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestIfBetter(Quest.ARMORER, "step1");
        }
        if (text.contains("place the popular part")) {
          ResultProcessor.removeItem(ItemPool.POPULAR_PART);
          Preferences.setBoolean("popularTartUnlocked", true);
        }
        break;

      case 1064:
        // The Doctor is Out.  Of Herbs.
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.DOC, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 2) {
          QuestDatabase.setQuestProgress(Quest.DOC, QuestDatabase.FINISHED);
          ResultProcessor.processResult(ItemPool.get(ItemPool.FRAUDWORT, -3));
          ResultProcessor.processResult(ItemPool.get(ItemPool.SHYSTERWEED, -3));
          ResultProcessor.processResult(ItemPool.get(ItemPool.SWINDLEBLOSSOM, -3));
          HPRestoreItemList.updateHealthRestored();
          MPRestoreItemList.updateManaRestored();
        }
        break;

      case 1065:
        // Lending a Hand (and a Foot)
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.ARMORER, QuestDatabase.STARTED);
        } else if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestIfBetter(Quest.ARMORER, QuestDatabase.STARTED);
        }
        break;

      case 1066:
        // Employee Assignment Kiosk
        if (text.contains("Performance Review:  Sufficient")) {
          EquipmentManager.discardEquipment(ItemPool.TRASH_NET);
          ResultProcessor.removeItem(ItemPool.TRASH_NET);
          Preferences.setInteger("dinseyFilthLevel", 0);
          QuestDatabase.setQuestProgress(Quest.FISH_TRASH, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Unobjectionable")) {
          ResultProcessor.processItem(ItemPool.TOXIC_GLOBULE, -20);
          QuestDatabase.setQuestProgress(Quest.GIVE_ME_FUEL, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Bearable")) {
          Preferences.setInteger("dinseyNastyBearsDefeated", 0);
          QuestDatabase.setQuestProgress(Quest.NASTY_BEARS, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Acceptable")) {
          Preferences.setInteger("dinseySocialJusticeIProgress", 0);
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_I, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Fair")) {
          Preferences.setInteger("dinseySocialJusticeIIProgress", 0);
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_II, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Average")) {
          EquipmentManager.discardEquipment(ItemPool.LUBE_SHOES);
          ResultProcessor.removeItem(ItemPool.LUBE_SHOES);
          QuestDatabase.setQuestProgress(Quest.SUPER_LUBER, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Adequate")) {
          Preferences.setInteger("dinseyTouristsFed", 0);
          ResultProcessor.removeItem(ItemPool.DINSEY_REFRESHMENTS);
          QuestDatabase.setQuestProgress(Quest.WORK_WITH_FOOD, QuestDatabase.UNSTARTED);
        } else if (text.contains("Performance Review:  Tolerable")) {
          EquipmentManager.discardEquipment(ItemPool.MASCOT_MASK);
          ResultProcessor.removeItem(ItemPool.MASCOT_MASK);
          Preferences.setInteger("dinseyFunProgress", 0);
          QuestDatabase.setQuestProgress(Quest.ZIPPITY_DOO_DAH, QuestDatabase.UNSTARTED);
        } else if (text.contains("weren't kidding about the power")) {
          if (InventoryManager.getCount(ItemPool.TOXIC_GLOBULE) >= 20) {
            QuestDatabase.setQuestProgress(Quest.GIVE_ME_FUEL, "step1");
          } else {
            QuestDatabase.setQuestProgress(Quest.GIVE_ME_FUEL, QuestDatabase.STARTED);
          }
        } else if (text.contains("anatomical diagram of a nasty bear")) {
          QuestDatabase.setQuestProgress(Quest.NASTY_BEARS, QuestDatabase.STARTED);
        } else if (text.contains("lists all of the sexist aspects of the ride")) {
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_I, QuestDatabase.STARTED);
        } else if (text.contains("ideas are all themselves so racist")) {
          QuestDatabase.setQuestProgress(Quest.SOCIAL_JUSTICE_II, QuestDatabase.STARTED);
        } else if (text.contains("box of snacks issues forth")) {
          Preferences.setInteger("dinseyTouristsFed", 0);
          QuestDatabase.setQuestProgress(Quest.WORK_WITH_FOOD, QuestDatabase.STARTED);
        }
        break;

      case 1067:
        // Maint Misbehavin'
        if (text.contains("throw a bag of garbage into it")) {
          ResultProcessor.processItem(ItemPool.GARBAGE_BAG, -1);
          Preferences.setBoolean("_dinseyGarbageDisposed", true);
        }
        break;

      case 1076:
        // Mayo Minder&trade;
        switch (ChoiceManager.lastDecision) {
          case 1:
            Preferences.setString("mayoMinderSetting", "Mayonex");
            break;
          case 2:
            Preferences.setString("mayoMinderSetting", "Mayodiol");
            break;
          case 3:
            Preferences.setString("mayoMinderSetting", "Mayostat");
            break;
          case 4:
            Preferences.setString("mayoMinderSetting", "Mayozapine");
            break;
          case 5:
            Preferences.setString("mayoMinderSetting", "Mayoflex");
            break;
          case 6:
            Preferences.setString("mayoMinderSetting", "");
            break;
        }
        break;

      case 1080:
        // Bagelmat-5000
        if (text.contains("shove a wad of dough into the slot")) {
          ResultProcessor.removeItem(ItemPool.DOUGH);
        }
        break;

      case 1081:
        // Assault and Baguettery
        if (ChoiceManager.lastDecision == 1
            || ChoiceManager.lastDecision == 2
            || ChoiceManager.lastDecision == 3) {
          ResultProcessor.removeItem(ItemPool.MAGICAL_BAGUETTE);
        }
        break;

      case 1084:
        // The Popular Machine
        if (text.contains("popular tart springs")) {
          ResultProcessor.removeItem(ItemPool.DOUGH);
          ResultProcessor.removeItem(ItemPool.STRAWBERRY);
          ResultProcessor.removeItem(ItemPool.ENCHANTED_ICING);
        }
        break;

      case 1085:
      case 1086:
        // The Deck of Every Card
        DeckOfEveryCardRequest.postChoice1(text);
        return;

      case 1087:
        // The Dark and Dank and Sinister Cave Entrance
        if (text.contains("stumpy-legged mushroom creatures")) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step12");
        }
        break;

      case 1088:
        // Rubble, Rubble, Toil and Trouble
        if (text.contains("BOOOOOOM!")) {
          ResultProcessor.processItem(ItemPool.FIZZING_SPORE_POD, -6);
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step15");
        }
        break;

      case 1118:
        // X-32-F Combat Training Snowman Control Console
        switch (ChoiceManager.lastDecision) {
          case 1:
            Preferences.setString("snojoSetting", "MUSCLE");
            break;
          case 2:
            Preferences.setString("snojoSetting", "MYSTICALITY");
            break;
          case 3:
            Preferences.setString("snojoSetting", "MOXIE");
            break;
          case 4:
            Preferences.setString("snojoSetting", "TOURNAMENT");
            break;
        }
        break;

      case 1120:
      case 1121:
      case 1122:
      case 1123:
        // Some Assembly Required
        VYKEACompanionData.assembleCompanion(
            ChoiceManager.lastChoice, ChoiceManager.lastDecision, text);
        break;

      case 1133:
        // Batfellow Begins
        if (lastDecision == 1) {
          KoLCharacter.enterLimitmode(Limitmode.BATMAN);
        }
        break;

      case 1137:
        // Bat-Suit Upgrades
        BatManager.batSuitUpgrade(ChoiceManager.lastDecision, text);
        break;

      case 1138:
        // Bat-Sedan Upgrades
        BatManager.batSedanUpgrade(ChoiceManager.lastDecision, text);
        break;

      case 1139:
        // Bat-Cavern Upgrades
        BatManager.batCavernUpgrade(ChoiceManager.lastDecision, text);
        break;

      case 1140:
        // Casing the Conservatory
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.GLOB_OF_BAT_GLUE);
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
        }
        break;

      case 1141:
        // Researching the Reservoir
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_AID_BANDAGE);
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
        }
        break;

      case 1142:
        // Combing the Cemetery
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_BEARING);
        } else if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
        }
        break;

      case 1143:
        // Searching the Sewers
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_OOMERANG);
        }
        break;

      case 1144:
        // Assessing the Asylum
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_O_MITE);
        }
        break;

      case 1145:
        // Looking over the Library
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.BAT_JUTE);
        }
        break;

      case 1146:
        // Considering the Clock Factory
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
        }
        break;

      case 1147:
        // Frisking the Foundry
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
        }
        break;

      case 1148:
        // Taking Stock of the Trivia Company
        if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.removeItem(ItemPool.FINGERPRINT_DUSTING_KIT);
        }
        break;

      case 1176:
        // Go West, Young Adventurer!
        if (ChoiceManager.lastDecision == 3) {
          // Snake Oilers start with extra
          Preferences.setInteger("awolMedicine", 3);
          Preferences.setInteger("awolVenom", 3);
        }
        break;

      case 1182:
        // Play against the Witchess Pieces
        if (ChoiceManager.lastDecision == 1) {
          KoLAdventure.lastVisitedLocation = null;
          KoLAdventure.lastLocationName = null;
          KoLAdventure.lastLocationURL = urlString;
          KoLAdventure.setLastAdventure("None");
          KoLAdventure.setNextAdventure("None");
          GenericRequest.itemMonster = "Your Witchess Set";
        }
        break;

      case 1190:
        // The Oracle
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.NO_SPOON);
          QuestDatabase.setQuestProgress(Quest.ORACLE, QuestDatabase.UNSTARTED);
          Preferences.increment("sourceEnlightenment");
          Preferences.setString("sourceOracleTarget", "");
        } else if (ChoiceManager.lastDecision <= 3) {
          QuestDatabase.setQuestProgress(Quest.ORACLE, QuestDatabase.STARTED);
          Matcher matcher = ChoiceManager.ORACLE_QUEST_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("sourceOracleTarget", matcher.group(1));
          }
        }
        break;

      case 1191:
        // Source Terminal
        request.setHasResult(false);
        if (ChoiceManager.lastDecision != 1) {
          break;
        }
        String input = request.getFormField("input");
        if (input == null) {
          break;
        }
        ChoiceManager.handleSourceTerminal(input, text);
        break;

      case 1195:
        // Spinning Your Time-Spinner
        if (ChoiceManager.lastDecision == 3) {
          Preferences.increment("_timeSpinnerMinutesUsed");
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.increment("_timeSpinnerMinutesUsed", 2);
        }
        break;

      case 1196:
        // Travel to a Recent Fight
        if (ChoiceManager.lastDecision == 1 && !urlString.contains("monid=0")) {
          Preferences.increment("_timeSpinnerMinutesUsed", 3);
        }
        break;

      case 1215:
        // Setting the Clock
        Preferences.setBoolean("_gingerbreadClockVisited", true);

        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_gingerbreadClockAdvanced", true);
          Preferences.increment("_gingerbreadCityTurns");
        }
        break;

      case 1217:
        // Sweet Synthesis
        SweetSynthesisRequest.postChoice1(urlString, text);
        return;

      case 1222:
        // The Tunnel of L.O.V.E.
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_loveTunnelUsed", true);
        }
        return;

      case 1229:
        {
          // L.O.V. Exit

          Matcher matcher = ChoiceManager.LOV_LOGENTRY_PATTERN.matcher(text);
          if (matcher.find()) {
            String message = "Your log entry: " + matcher.group(1);
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }
          return;
        }

      case 1231:
        // Gummi-Memories, In the Corner of Your Mind
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.GUMMY_MEMORY);
          Preferences.increment("noobDeferredPoints", 5);
        }
        return;

      case 1234:
        // Spacegate Vaccinator
        //
        // option 1 = Rainbow Vaccine
        // option 2 = Broad-Spectrum Vaccine
        // option 3 = Emotional Vaccine
        //
        // You can unlock it (by turning in enough research) or
        // Select it (if previously unlocked).
        //
        if (text.contains("New vaccine unlocked!")) {
          Preferences.setBoolean("spacegateVaccine" + ChoiceManager.lastDecision, true);
        } else if (text.contains("You acquire an effect")) {
          Preferences.setBoolean("_spacegateVaccine", true);
        }
        break;

      case 1235:
        // Spacegate Terminal
        QuestManager.parseSpacegateTerminal(text, true);
        return;

      case 1246:
        // Land Ho
        if (ChoiceManager.lastDecision == 1) {
          ChoiceManager.parseLanguageFluency(text, "spacePirateLanguageFluency");
        }
        break;

      case 1247:
        // Half the Ship it used to Be
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You acquire an item")) {
            Preferences.setInteger("spacePirateLanguageFluency", 0);
          } else {
            ChoiceManager.parseLanguageFluency(text, "spacePirateLanguageFluency");
          }
        }
        break;

      case 1248:
        // Paradise Under a Strange Sun
        if (text.contains("You acquire an item")) {
          ResultProcessor.removeItem(ItemPool.SPACE_PIRATE_TREASURE_MAP);
        }
        break;

      case 1249:
        // That's No Monolith, it's a Monolith
        if (ChoiceManager.lastDecision == 1) {
          ChoiceManager.parseLanguageFluency(text, "procrastinatorLanguageFluency");
          ResultProcessor.removeItem(ItemPool.MURDERBOT_DATA_CORE);
        }
        break;

      case 1250:
        // I'm Afraid It's Terminal
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You acquire an item")) {
            Preferences.setInteger("procrastinatorLanguageFluency", 0);
          }
        }
        break;

      case 1251:
        // Curses, A Hex
        if (text.contains("You acquire an item")) {
          ResultProcessor.removeItem(ItemPool.PROCRASTINATOR_LOCKER_KEY);
        }
        break;

      case 1252:
        // Time Enough at Last
        if (ChoiceManager.lastDecision == 1) {
          // You get a Space Baby children's book which
          // will grants spaceBabyLanguageFluency +10
          // when read
        }
        break;

      case 1253:
        // Mother May I
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("You acquire an item")) {
            Preferences.setInteger("spaceBabyLanguageFluency", 0);
          }
        }
        break;

      case 1254:
        // Please Baby Baby Please
        if (text.contains("You acquire an item")) {
          ResultProcessor.removeItem(ItemPool.SPACE_BABY_BAWBAW);
        }
        break;

      case 1260:
        // A Strange Panel
        if (ChoiceManager.lastDecision < 1 || ChoiceManager.lastDecision > 3) {
          break;
        }
        if (text.contains("10 casualties")
            || text.contains("10 crew")
            || text.contains("10 minions")
            || text.contains("10 ski")
            || text.contains("10 members")
            || text.contains("ten techs")
            || text.contains("10 soldiers")) {
          Preferences.increment("_villainLairProgress", 10);
        } else if (text.contains("5 casualties")
            || text.contains("5 souls")
            || text.contains("5 minions")
            || text.contains("five minions")
            || text.contains("group of ski")
            || text.contains("5 members")
            || text.contains("five people")
            || text.contains("five of us")) {
          Preferences.increment("_villainLairProgress", 5);
        } else {
          Preferences.decrement("_villainLairProgress", 7);
        }
        Preferences.setBoolean("_villainLairColorChoiceUsed", true);
        break;

      case 1261:
        // Which Door?
        if (ChoiceManager.lastDecision == 1) {
          if (text.contains("drop 1000")) {
            Preferences.increment("_villainLairProgress", 5);
            Preferences.setBoolean("_villainLairDoorChoiceUsed", true);
          }
        } else if (ChoiceManager.lastDecision == 2) {
          if (text.contains("insert the key")) {
            int itemId = -1;
            String key = Preferences.getString("_villainLairKey");
            if (key.equals("boris")) {
              itemId = ItemPool.BORIS_KEY;
            } else if (key.equals("jarlsberg")) {
              itemId = ItemPool.JARLSBERG_KEY;
            }
            if (key.equals("pete")) {
              itemId = ItemPool.SNEAKY_PETE_KEY;
            }
            ResultProcessor.removeItem(itemId);
            Preferences.increment("_villainLairProgress", 15);
            Preferences.setBoolean("_villainLairDoorChoiceUsed", true);
          }
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.decrement("_villainLairProgress", 13);
          Preferences.setBoolean("_villainLairDoorChoiceUsed", true);
        }
        break;

      case 1262:
        // What Setting?
        if (ChoiceManager.lastDecision < 1 || ChoiceManager.lastDecision > 3) {
          break;
        }
        if (text.contains("20 of the")
            || text.contains("20 minions")
            || text.contains("20 or so")
            || text.contains("20 soldiers")) {
          Preferences.increment("_villainLairProgress", 20);
        } else if (text.contains("10 or so")
            || text.contains("10 injured")
            || text.contains("10 patrol-sicles")
            || text.contains("10 soldiers")) {
          Preferences.increment("_villainLairProgress", 10);
        } else if (text.contains("15 aquanats")
            || text.contains("15 reserve")
            || text.contains("15 previously")
            || text.contains("15 Soldiers")) {
          Preferences.decrement("_villainLairProgress", 15);
        }
        Preferences.setBoolean("_villainLairSymbologyChoiceUsed", true);
        break;

      case 1266:
        // The Hostler
        if (ChoiceManager.lastDecision < 5) {
          Pattern pattern = Pattern.compile("You rent(|ed) the (.*?)!");
          Matcher matcher = pattern.matcher(text);
          if (matcher.find()) {
            String horse = matcher.group(2);
            Preferences.setString("_horsery", horse);
            String message = "Chose the " + horse;
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
            String setting =
                horse.equals("crazy horse")
                    ? "_horseryCrazyName"
                    : horse.equals("dark horse")
                        ? "_horseryDarkName"
                        : horse.equals("normal horse")
                            ? "_horseryNormalName"
                            : horse.equals("pale horse") ? "_horseryPaleName" : null;
            if (setting != null) {
              String name = Preferences.getString(setting);
              Preferences.setString("_horseryCurrentName", name);
            }
          }
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setString("_horsery", "");
          Preferences.setString("_horseryCurrentName", "");
          String message = "Returned your horse";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
        break;

      case 1270:
        // Pantagramming
        // The item that we get has a procedurally-generated name
        request.setHasResult(false);
        PantogramRequest.parseResponse(urlString, text);
        break;

      case 1271:
        // Mummery
        MummeryRequest.parseResponse(ChoiceManager.lastDecision, text);
        break;

      case 1275:
        {
          // Rummaging through the Garbage
          if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 5) {
            // Remove all of the items before parsing the newly-received one
            EquipmentManager.removeEquipment(ItemPool.DECEASED_TREE);
            ResultProcessor.removeItem(ItemPool.DECEASED_TREE);
            EquipmentManager.removeEquipment(ItemPool.BROKEN_CHAMPAGNE);
            ResultProcessor.removeItem(ItemPool.BROKEN_CHAMPAGNE);
            EquipmentManager.removeEquipment(ItemPool.TINSEL_TIGHTS);
            ResultProcessor.removeItem(ItemPool.TINSEL_TIGHTS);
            EquipmentManager.removeEquipment(ItemPool.WAD_OF_TAPE);
            ResultProcessor.removeItem(ItemPool.WAD_OF_TAPE);
            EquipmentManager.removeEquipment(ItemPool.MAKESHIFT_GARBAGE_SHIRT);
            ResultProcessor.removeItem(ItemPool.MAKESHIFT_GARBAGE_SHIRT);
            if (!Preferences.getBoolean("_garbageItemChanged")) {
              Preferences.setInteger("garbageTreeCharge", 1000);
              Preferences.setInteger("garbageChampagneCharge", 11);
              Preferences.setInteger("garbageShirtCharge", 37);
            }
            Preferences.setBoolean("_garbageItemChanged", true);
          }
          // Do some parsing of needles/wine/scraps here
          Matcher matcher = ChoiceManager.DECEASED_TREE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("garbageTreeCharge", StringUtilities.parseInt(matcher.group(1)));
          }
          matcher = ChoiceManager.BROKEN_CHAMPAGNE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "garbageChampagneCharge", StringUtilities.parseInt(matcher.group(1)));
          }
          matcher = ChoiceManager.GARBAGE_SHIRT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "garbageShirtCharge", StringUtilities.parseInt(matcher.group(1)));
          }
          break;
        }

      case 1277:
        // Extra, Extra
        if (ChoiceManager.lastDecision >= 1
            && ChoiceManager.lastDecision <= 5
            && text.contains("You acquire")) {
          ResultProcessor.removeItem(ItemPool.BURNING_NEWSPAPER);
        }
        break;

      case 1280:
        // Welcome to FantasyRealm
        if (ChoiceManager.lastDecision != 6) {
          Preferences.setInteger("_frHoursLeft", 5);
          StringBuffer unlocks = new StringBuffer();
          unlocks.append("The Bandit Crossroads,");
          if (Preferences.getBoolean("frMountainsUnlocked")) {
            unlocks.append("The Towering Mountains,");
          }
          if (Preferences.getBoolean("frWoodUnlocked")) {
            unlocks.append("The Mystic Wood,");
          }
          if (Preferences.getBoolean("frSwampUnlocked")) {
            unlocks.append("The Putrid Swamp,");
          }
          if (Preferences.getBoolean("frVillageUnlocked")) {
            unlocks.append("The Cursed Village,");
          }
          if (Preferences.getBoolean("frCemetaryUnlocked")) {
            unlocks.append("The Sprawling Cemetery,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
        }
        break;

      case 1281:
        {
          // You'll See You at the Crossroads
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Bandit Crossroads,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("The Towering Mountains,");
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Mystic Wood,");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Putrid Swamp,");
          }
          if (ChoiceManager.lastDecision == 4) {
            unlocks.append("The Cursed Village,");
          }
          if (ChoiceManager.lastDecision == 5) {
            unlocks.append("The Sprawling Cemetery,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1282:
        {
          // Out of Range
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 11) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Towering Mountains,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("The Old Rubee Mine,");
            ResultProcessor.removeItem(ItemPool.FR_KEY);
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Foreboding Cave,");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Master Thief's Chalet,");
          }
          if (ChoiceManager.lastDecision == 4) {
            ResultProcessor.removeItem(ItemPool.FR_DRUIDIC_ORB);
          }
          if (ChoiceManager.lastDecision == 5) {
            unlocks.append("The Ogre Chieftain's Keep,");
          }
          if (ChoiceManager.lastDecision == 10) {
            Preferences.increment("_frButtonsPressed");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1283:
        {
          // Where Wood You Like to Go
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 11) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Mystic Wood,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("The Faerie Cyrkle,");
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Druidic Campsite,");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Ley Nexus,");
          }
          if (ChoiceManager.lastDecision == 10) {
            Preferences.increment("_frButtonsPressed");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1284:
        {
          // Swamped with Leisure
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 11) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Putrid Swamp,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("Near the Witch's House,");
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Troll Fortress,");
            ResultProcessor.removeItem(ItemPool.FR_KEY);
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Dragon's Moor,");
          }
          if (ChoiceManager.lastDecision == 10) {
            Preferences.increment("_frButtonsPressed");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1285:
        {
          // It Takes a Cursed Village
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 11) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Cursed Village,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("The Evil Cathedral,");
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Cursed Village Thieves' Guild,");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Archwizard's Tower,");
          }
          if (ChoiceManager.lastDecision == 6) {
            ResultProcessor.removeItem(ItemPool.FR_DRAGON_ORE);
          }
          if (ChoiceManager.lastDecision == 7) {
            ResultProcessor.removeItem(ItemPool.FR_ARREST_WARRANT);
          }
          if (ChoiceManager.lastDecision == 10) {
            Preferences.increment("_frButtonsPressed");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1286:
        {
          // Resting in Peace
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 11) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Sprawling Cemetery,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            unlocks.append("The Labyrinthine Crypt,");
          }
          if (ChoiceManager.lastDecision == 2) {
            unlocks.append("The Barrow Mounds,");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("Duke Vampire's Chateau,");
          }
          if (ChoiceManager.lastDecision == 10) {
            Preferences.increment("_frButtonsPressed");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1288:
        {
          // What's Yours is Yours
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Old Rubee Mine,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1289:
        {
          // A Warm Place
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Foreboding Cave,", "");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Lair of the Phoenix,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1290:
        {
          // The Cyrkle Is Compleat
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Faerie Cyrkle,", "");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Spider Queen's Lair,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1291:
        {
          // Dudes, Where's My Druids?
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Druidic Campsite,", "");
          }
          if (ChoiceManager.lastDecision == 2) {
            ResultProcessor.removeItem(ItemPool.FR_TAINTED_MARSHMALLOW);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1292:
        {
          // Witch One You Want?
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "Near the Witch's House,", "");
          }
          if (ChoiceManager.lastDecision == 2) {
            ResultProcessor.removeItem(ItemPool.FR_PURPLE_MUSHROOM);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1293:
        {
          // Altared States
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Evil Cathedral,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1294:
        {
          // Neither a Barrower Nor a Lender Be
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Barrow Mounds,", "");
          }
          if (ChoiceManager.lastDecision == 3) {
            unlocks.append("The Ghoul King's Catacomb,");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1295:
        {
          // Honor Among You
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Cursed Village Thieves' Guild,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1296:
        {
          // For Whom the Bell Trolls
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Troll Fortress,", "");
          }
          if (ChoiceManager.lastDecision == 3) {
            ResultProcessor.removeItem(ItemPool.FR_CHESWICKS_NOTES);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1297:
        {
          // Stick to the Crypt
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            Preferences.decrement("_frHoursLeft");
            StringUtilities.singleStringReplace(unlocks, "The Labyrinthine Crypt,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1298:
        {
          // The "Phoenix"
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Lair of the Phoenix,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            ResultProcessor.removeItem(ItemPool.FR_HOLY_WATER);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1299:
        {
          // Stop Dragon Your Feet
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Dragon's Moor,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1300:
        {
          // Just Vamping
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "Duke Vampire's Chateau,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1301:
        {
          // Now You've Spied Her
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Spider Queen's Lair,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1302:
        {
          // Don't Be Arch
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Archwizard's Tower,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            ResultProcessor.removeItem(ItemPool.FR_CHARGED_ORB);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1303:
        {
          // Ley Lady Ley
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Ley Nexus,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            ResultProcessor.removeItem(ItemPool.FR_CHESWICKS_COMPASS);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1304:
        {
          // He Is the Ghoul King, He Can Do Anything
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Ghoul King's Catacomb,", "");
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1305:
        {
          // The Brogre's Progress
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Ogre Chieftain's Keep,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            ResultProcessor.removeItem(ItemPool.FR_POISONED_SMORE);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1307:
        {
          // It Takes a Thief
          StringBuffer unlocks = new StringBuffer(Preferences.getString("_frAreasUnlocked"));
          if (ChoiceManager.lastDecision != 6) {
            StringUtilities.singleStringReplace(unlocks, "The Master Thief's Chalet,", "");
          }
          if (ChoiceManager.lastDecision == 1) {
            ResultProcessor.removeItem(ItemPool.FR_NOTARIZED_WARRANT);
          }
          Preferences.setString("_frAreasUnlocked", unlocks.toString());
          break;
        }

      case 1312:
        {
          // Choose a Soundtrack
          if (!text.contains("decide not to change the station")) {
            String songChosen = "";
            switch (ChoiceManager.lastDecision) {
              case 1:
                songChosen = "Eye of the Giger";
                break;
              case 2:
                songChosen = "Food Vibrations";
                break;
              case 3:
                songChosen = "Remainin' Alive";
                break;
              case 4:
                songChosen = "These Fists Were Made for Punchin'";
                break;
              case 5:
                songChosen = "Total Eclipse of Your Meat";
                break;
            }
            if (!songChosen.equals("")) {
              if (!KoLCharacter.hasSkill("Sing Along")) {
                KoLCharacter.addAvailableSkill("Sing Along");
              }
              if (!Preferences.getString("boomBoxSong").equals(songChosen)) {
                Preferences.setString("boomBoxSong", songChosen);
                Preferences.decrement("_boomBoxSongsLeft");
                String message = "Setting soundtrack to " + songChosen;
                RequestLogger.printLine(message);
                RequestLogger.updateSessionLog(message);
              }
            } else {
              if (KoLCharacter.hasSkill("Sing Along")) {
                KoLCharacter.removeAvailableSkill("Sing Along");
              }
              if (!Preferences.getString("boomBoxSong").equals("")) {
                Preferences.setString("boomBoxSong", "");
                String message = "Switching soundtrack off";
                RequestLogger.printLine(message);
                RequestLogger.updateSessionLog(message);
              }
            }
          }
          break;
        }

      case 1322:
        {
          // The Beginning of the Neverend
          boolean hard = KoLCharacter.hasEquipped(ItemPool.PARTY_HARD_T_SHIRT);
          Preferences.setBoolean("_partyHard", hard);
          if (ChoiceManager.lastDecision == 1) {
            // Decided to quest
            Preferences.setInteger("encountersUntilNEPChoice", 7);

            String quest = Preferences.getString("_questPartyFairQuest");
            if (quest.equals("booze") || quest.equals("food")) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED);
              Preferences.setString("_questPartyFairProgress", "");
            } else {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step1");
              if (quest.equals("woots")) {
                Preferences.setInteger("_questPartyFairProgress", 10);
              } else if (quest.equals("partiers")) {
                if (hard) {
                  Preferences.setInteger("_questPartyFairProgress", 100);
                } else {
                  Preferences.setInteger("_questPartyFairProgress", 50);
                }
              } else if (quest.equals("dj")) {
                if (hard) {
                  Preferences.setInteger("_questPartyFairProgress", 10000);
                } else {
                  Preferences.setInteger("_questPartyFairProgress", 5000);
                }
              } else if (quest.equals("trash")) {
                // The amount isn't known, so check quest log
                (new GenericRequest("questlog.php?which=1")).run();
              }
            }
          } else if (ChoiceManager.lastDecision == 2) {
            // Decided to party
            Preferences.setString("_questPartyFair", "");
            Preferences.setString("_questPartyFairQuest", "");
            Preferences.setString("_questPartyFairProgress", "");
          }
          break;
        }

      case 1323:
        // All Done!
        QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, QuestDatabase.FINISHED);
        Preferences.setString("_questPartyFairQuest", "");
        Preferences.setString("_questPartyFairProgress", "");
        break;

      case 1324:
        // It Hasn't Ended, It's Just Paused
        // Decision 5 is followed by a fight, which will decrement the free turns available itself
        if (ChoiceManager.lastDecision != 5) {
          Preferences.decrement("encountersUntilNEPChoice", 1, 0);
          Preferences.increment("_neverendingPartyFreeTurns", 1, 10, false);
        }
        break;

      case 1325:
        // A Room With a View...  Of a Bed
        if (ChoiceManager.lastDecision == 3) {
          // Removes 30% of current partiers
          int current = Preferences.getInteger("_questPartyFairProgress");
          Preferences.setInteger(
              "_questPartyFairProgress", current - (int) Math.floor(current * 0.3));
          ResultProcessor.removeItem(ItemPool.JAM_BAND_BOOTLEG);
        } else if (ChoiceManager.lastDecision == 4) {
          // On dj quest (choice number guessed)
          Matcher matcher = ChoiceManager.SAFE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.decrement(
                "_questPartyFairProgress", StringUtilities.parseInt(matcher.group(1)), 0);
            if (Preferences.getInteger("_questPartyFairProgress") < 1) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
            }
          }
        } else if (ChoiceManager.lastDecision == 5) {
          // On woots quest
          Preferences.increment("_questPartyFairProgress", 20, 100, false);
          if (Preferences.getInteger("_questPartyFairProgress") == 100) {
            QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
          }
          ResultProcessor.removeItem(ItemPool.VERY_SMALL_RED_DRESS);
        }
        break;

      case 1326:
        // Gone Kitchin'
        if (ChoiceManager.lastDecision == 3) {
          Matcher matcher = ChoiceManager.GERALDINE_PATTERN.matcher(text);
          if (matcher.find()) {
            int itemCount = StringUtilities.parseInt(matcher.group(1));
            int itemId = ItemDatabase.getItemIdFromDescription(matcher.group(2));
            Preferences.setString("_questPartyFairProgress", itemCount + " " + itemId);
            if (InventoryManager.getCount(itemId) >= itemCount) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
            }
          }
          QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, "step1");
        } else if (ChoiceManager.lastDecision == 4) {
          String pref = Preferences.getString("_questPartyFairProgress");
          String itemIdString = null;
          int position = pref.indexOf(" ");
          if (position > 0) {
            itemIdString = pref.substring(position);
            if (itemIdString != null) {
              ResultProcessor.processItem(StringUtilities.parseInt(itemIdString), -10);
            }
          }
          QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, QuestDatabase.FINISHED);
          Preferences.setString("_questPartyFairQuest", "");
          Preferences.setString("_questPartyFairProgress", "");
        } else if (ChoiceManager.lastDecision == 5) {
          Matcher matcher = ChoiceManager.TRASH_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.decrement(
                "_questPartyFairProgress", StringUtilities.parseInt(matcher.group(1)), 0);
          }
        }
        break;

      case 1327:
        // Forward to the Back
        if (ChoiceManager.lastDecision == 3) {
          Matcher matcher = ChoiceManager.GERALD_PATTERN.matcher(text);
          if (matcher.find()) {
            int itemCount = StringUtilities.parseInt(matcher.group(1));
            int itemId = ItemDatabase.getItemIdFromDescription(matcher.group(2));
            Preferences.setString("_questPartyFairProgress", itemCount + " " + itemId);
            if (InventoryManager.getCount(itemId) >= itemCount) {
              QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
            }
          }
          QuestDatabase.setQuestIfBetter(Quest.PARTY_FAIR, "step1");
        } else if (ChoiceManager.lastDecision == 4) {
          String pref = Preferences.getString("_questPartyFairProgress");
          String itemIdString = null;
          int position = pref.indexOf(" ");
          if (position > 0) {
            itemIdString = pref.substring(position);
            if (itemIdString != null) {
              int itemCount = Preferences.getBoolean("_partyHard") ? 20 : 10;
              ResultProcessor.processItem(StringUtilities.parseInt(itemIdString), -itemCount);
            }
          }
          QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, QuestDatabase.FINISHED);
          Preferences.setString("_questPartyFairQuest", "");
          Preferences.setString("_questPartyFairProgress", "");
        } else if (ChoiceManager.lastDecision == 5) {
          // Removes 20% of current partiers
          int current = Preferences.getInteger("_questPartyFairProgress");
          Preferences.setInteger(
              "_questPartyFairProgress", current - (int) Math.floor(current * 0.2));
          ResultProcessor.removeItem(ItemPool.PURPLE_BEAST_ENERGY_DRINK);
        }
        break;

      case 1328:
        // Basement Urges
        if (ChoiceManager.lastDecision == 4) {
          // On woots quest
          Preferences.increment("_questPartyFairProgress", 20, 100, false);
          if (Preferences.getInteger("_questPartyFairProgress") == 100) {
            QuestDatabase.setQuestProgress(Quest.PARTY_FAIR, "step2");
          }
          ResultProcessor.removeItem(ItemPool.ELECTRONICS_KIT);
        }
        break;

      case 1329:
        // Latte Shop
        // The item that we get has a procedurally-generated name
        request.setHasResult(false);
        LatteRequest.parseResponse(urlString, text);
        break;

      case 1331:
        // Daily Loathing Ballot
        if (ChoiceManager.lastDecision == 1 && !text.contains("must vote for a candidate")) {
          ModifierList modList = new ModifierList();
          Matcher matcher = ChoiceManager.URL_VOTE_PATTERN.matcher(urlString);
          while (matcher.find()) {
            int vote = StringUtilities.parseInt(matcher.group(1)) + 1;
            String pref = Preferences.getString("_voteLocal" + vote);
            ModifierList addModList = Modifiers.splitModifiers(pref);
            for (Modifier modifier : addModList) {
              modList.addToModifier(modifier);
            }
          }
          Preferences.setString("_voteModifier", modList.toString());
          String message = "You have cast your votes";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
        break;

      case 1332:
        // government requisition form
        ResultProcessor.removeItem(ItemPool.GOVERNMENT_REQUISITION_FORM);
        break;

      case 1333:
        // Canadian cabin
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.removeItem(ItemPool.MOOSEFLANK);
        } else if (ChoiceManager.lastDecision == 3) {
          ResultProcessor.processItem(ItemPool.WALRUS_BLUBBER, -10);
        } else if (ChoiceManager.lastDecision == 4) {
          ResultProcessor.processItem(ItemPool.TINY_BOMB, -10);
        }
        break;

      case 1334:
        // Boxing Daycare (Lobby)
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_daycareNap", true);
        } else if (ChoiceManager.lastDecision == 2
            && text.contains("only allowed one spa treatment")) {
          Preferences.setBoolean("_daycareSpa", true);
        }
        break;

      case 1335:
        // Boxing Day Spa
        if (ChoiceManager.lastDecision != 5) {
          Preferences.setBoolean("_daycareSpa", true);
        }
        break;

      case 1336:
        {
          // Boxing Daycare
          String message1 = null;
          String message2 = null;
          if (ChoiceManager.lastDecision == 1) {
            Matcher matcher = ChoiceManager.DAYCARE_RECRUIT_PATTERN.matcher(text);
            if (matcher.find()) {
              message1 = "Activity: Recruit toddlers";
              message2 = "You have recruited " + matcher.group(1) + " toddlers";
              Preferences.increment("_daycareRecruits");
            }
          } else if (ChoiceManager.lastDecision == 2) {
            Matcher matcher = ChoiceManager.DAYCARE_EQUIPMENT_PATTERN.matcher(text);
            if (matcher.find()) {
              AdventureResult effect = EffectPool.get(EffectPool.BOXING_DAY_BREAKFAST, 0);
              boolean haveBreakfast = effect.getCount(KoLConstants.activeEffects) > 0;
              String countString = matcher.group(1);
              int equipment = StringUtilities.parseInt(countString.replaceAll(",", ""));
              Preferences.setInteger("daycareLastScavenge", equipment / (haveBreakfast ? 2 : 1));

              message1 = "Activity: Scavenge for gym equipment";
              message2 = "You have found " + countString + " pieces of gym equipment";
              Preferences.increment("_daycareGymScavenges");
            }
          } else if (ChoiceManager.lastDecision == 3) {
            if (text.contains("new teacher joins the staff")) {
              message1 = "Activity: Hire an instructor";
              message2 = "You have hired a new instructor";
            }
            Matcher matcher = ChoiceManager.DAYCARE_ITEM_PATTERN.matcher(text);
            if (matcher.find()) {
              String itemName = matcher.group(1);
              int itemCount = StringUtilities.parseInt(matcher.group(2).replaceAll(",", ""));
              ResultProcessor.processItem(ItemDatabase.getItemId(itemName), -itemCount);
            }
          } else if (ChoiceManager.lastDecision == 4) {
            if (text.contains("step into the ring")) {
              message1 = "Activity: Spar";
              Preferences.setBoolean("_daycareFights", true);
            }
          }
          if (message1 != null) {
            RequestLogger.printLine(message1);
            RequestLogger.updateSessionLog(message1);
          }
          if (message2 != null) {
            RequestLogger.printLine(message2);
            RequestLogger.updateSessionLog(message2);
          }
          break;
        }

      case 1340:
        // Is There A Doctor In The House?
        if (ChoiceManager.lastDecision == 1) {
          if (Preferences.getString("doctorBagQuestItem") == "") {
            // We didn't recognise quest text, so get it from quest log
            RequestThread.postRequest(new QuestLogRequest());
          }
          int itemId = ItemDatabase.getItemId(Preferences.getString("doctorBagQuestItem"));

          if (itemId > 0) {
            if (InventoryManager.getCount(itemId) > 0) {
              QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, "step1");
            } else {
              QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.STARTED);
            }
          } else {
            QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.STARTED);
          }
        } else {
          QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.UNSTARTED);
          Preferences.setString("doctorBagQuestItem", "");
          Preferences.setString("doctorBagQuestLocation", "");
        }
        break;

      case 1341:
        // A Pound of Cure
        if (ChoiceManager.lastDecision == 1) {
          String itemName = Preferences.getString("doctorBagQuestItem");
          if (!itemName.equals("")) {
            ResultProcessor.processItem(ItemDatabase.getItemId(itemName), -1);
          } else {
            // Don't know item to remove so refresh inventory instead
            ApiRequest.updateInventory();
          }
          QuestDatabase.setQuestProgress(Quest.DOCTOR_BAG, QuestDatabase.UNSTARTED);
          Preferences.setString("doctorBagQuestItem", "");
          Preferences.setString("doctorBagQuestLocation", "");
          if (text.contains("One of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 1);
          } else if (text.contains("second of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 2);
          } else if (text.contains("third of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 3);
          } else if (text.contains("fourth of the five green lights")) {
            Preferences.setInteger("doctorBagQuestLights", 4);
          } else if (text.contains("lights go dark again")) {
            Preferences.setInteger("doctorBagQuestLights", 0);
          }
          if (text.contains("bag has been permanently upgraded")) {
            Preferences.increment("doctorBagUpgrades");
          }
        }
        break;

      case 1342:
        // Torpor
        if (ChoiceManager.lastDecision == 2) {
          // You can learn or forget Vampyre skills
          for (int i = 10; i < 39; i++) {
            if (urlString.contains("sk[]=" + i) && !KoLCharacter.hasSkill(24000 + i)) {
              String skillName = SkillDatabase.getSkillName(24000 + i);
              KoLCharacter.addAvailableSkill(skillName);
              String message = "You have learned " + skillName;
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
            }
            if (!urlString.contains("sk[]=" + i) && KoLCharacter.hasSkill(24000 + i)) {
              String skillName = SkillDatabase.getSkillName(24000 + i);
              KoLCharacter.removeAvailableSkill(skillName);
              String message = "You have forgotten " + skillName;
              RequestLogger.printLine(message);
              RequestLogger.updateSessionLog(message);
            }
          }
        }
        break;

      case 1360:
        // Like Shops in the Night
        if (ChoiceManager.lastDecision == 5 && text.contains("You gain 500 gold")) {
          // Sell them the cursed compass
          // Remove from equipment (including checkpoints)
          if (EquipmentManager.discardEquipment(ItemPool.CURSED_COMPASS) == -1) {
            // Remove from inventory
            ResultProcessor.removeItem(ItemPool.CURSED_COMPASS);
          }
        }
        break;

      case 1386:
        SaberRequest.parseUpgrade(urlString, text);
        break;

      case 1387:
        SaberRequest.parseForce(urlString, text);
        break;

      case 1388:
        BeachManager.parseBeachHeadCombing(text);
        BeachManager.parseCombUsage(urlString, text);
        break;

      case 1391:
        {
          // Rationing out Destruction
          //    choice.php?whichchoice=1391&option=1&pwd&tossid=10321

          Matcher tossMatcher = TOSSID_PATTERN.matcher(urlString);
          if (!tossMatcher.find()) {
            break;
          }
          int itemId = StringUtilities.parseInt(tossMatcher.group(1));
          ResultProcessor.processItem(itemId, -1);

          String army = null;
          String property = null;
          String consumable = null;
          int casualties = 0;

          if (casualties == 0) {
            Matcher fratMatcher = FRAT_RATIONING_PATTERN.matcher(text);
            if (fratMatcher.find()) {
              army = "frat boys";
              property = "fratboysDefeated";
              consumable = fratMatcher.group(1);
              casualties = StringUtilities.parseInt(fratMatcher.group(2));
            }
          }
          if (casualties == 0) {
            Matcher hippyMatcher = HIPPY_RATIONING_PATTERN.matcher(text);
            if (hippyMatcher.find()) {
              army = "hippies";
              property = "hippiesDefeated";
              consumable = hippyMatcher.group(1);
              casualties = StringUtilities.parseInt(hippyMatcher.group(2));
            }
          }

          Preferences.increment(property, casualties);

          String message = "You defeated " + casualties + " " + army + " with some " + consumable;
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          break;
        }

      case 1392:
        DecorateTentRequest.parseDecoration(urlString, text);
        break;

      case 1394:
        // Send up a Smoke Signal
        if (ChoiceManager.lastDecision == 1
            && text.contains("You send a smoky message to the sky.")) {
          // Remove from inventory
          ResultProcessor.removeItem(ItemPool.CAMPFIRE_SMOKE);
        }
        break;

      case 1396:
        // Adjusting Your Fish
        // choice.php?pwd&whichchoice=1396&option=1&cat=fish
        if (urlString.contains("option=1") && urlString.contains("cat=")) {
          Pattern pattern = Pattern.compile("cat=([^&]*)");
          Matcher matcher = pattern.matcher(urlString);
          if (matcher.find()) {
            String phylum = matcher.group(1);
            String fixed = phylum.equals("merkin") ? "mer-kin" : phylum;
            Preferences.setString("redSnapperPhylum", fixed);
            Preferences.setInteger("redSnapperProgress", 0);
          }
        }
        break;

      case 1399:
        // New Favorite Bird?
        // Auto correct, in case we cast this outside of KoLmafia
        Preferences.setInteger("_birdsSoughtToday", 6);
        if (ChoiceManager.lastDecision == 1) {
          String bird = Preferences.getString("_birdOfTheDay");
          Preferences.setString("yourFavoriteBird", bird);
          ResponseTextParser.learnSkill("Visit your Favorite Bird");
          ResultProcessor.updateBirdModifiers(
              EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD, "yourFavoriteBird");
        }
        break;

      case 1406:
        {
          // Drippy House on the Prairie
          // 1 = Explore the House
          // Even though the house doesn't have a door, you check under the mat for a key anyway.
          // You don't find one, but you <i>do</i> find a little puddle of those Driplet things
          // Jeremy told you about.
          // 1 = Keep Exploring
          // Just inside the door of the house, you discover a colony of nasty bat-looking things
          // nesting in the rafters.
          // In one of the side rooms of the house, you find a giant spiral shell stuck to the wall.
          //  You pry it loose -- Jeremy will probably want to see this.
          // In the house's kitchen, you find a bucket underneath a dripping sink pipe. The
          // oddly-solid drops make a -thunk-... -thunk-... -thunk-... sound as they fall.
          // In the backyard of the house, you notice a strange fruit tree you hadn't seen before.
          // It's maybe... plums? Kinda hard to tell, what with everything being made out of the
          // same gross stuff.
          // In one of the back rooms, you find a workbench covered in drippy woodworking supplies.
          // Underneath a shattered bed frame in one of the back rooms, you find a trap door in the
          // floor. You press your ear to it and hear some... Thing... slithering around underneath
          // it.
          // 2 = Dislodge some bats
          // You flush some of the vile bat-things out of the rafters and into the nearby forest.
          // No way that'll come back to bite you in the ass!
          // 3 = Check the bucket under the sink
          // You look in the bucket and find a few Driplets.  You collect them and put the bucket
          // back under the sink.
          // 4 - Pick a nasty fruit
          // You pluck the least nasty-looking plum(?) from the tree and pocket it.
          // 5 - Check out the woodworking bench
          // You use the tools to carve your truncheon into a sharp stake.
          // 6 - Go down to the basement
          // 9 = Leave
          if (text.contains("vile bat-things")) {
            Preferences.increment("drippyBatsUnlocked", 7);
          }
          if (text.contains("sharp stake")) {
            EquipmentManager.discardEquipment(ItemPool.DRIPPY_TRUNCHEON);
          }

          // Since this choice appears on a schedule - the 16th
          // adventure in The Dripping Trees and then every 15
          // turns thereafter - "fix" the adventure count as needed.
          int advs = Preferences.getInteger("drippingTreesAdventuresSinceAscension");
          if (advs < 16) {
            Preferences.setInteger("drippingTreesAdventuresSinceAscension", 16);
            advs = 16;
          }
          int mod = (advs - 1) % 15;
          if (mod != 0) {
            Preferences.increment("drippingTreesAdventuresSinceAscension", 15 - mod);
          }

          break;
        }

      case 1407:
        {
          // Mushroom District Costume Shop

          String costume = "none";
          if (text.contains("You slip into something a little more carpentable")) {
            costume = "muscle";
          } else if (text.contains("You let down your guard and put on the gardener costume")) {
            costume = "mysticality";
          } else if (text.contains("Todge holds out a tutu and you jump into it")) {
            costume = "moxie";
          } else {
            break;
          }

          int cost = Preferences.getInteger("plumberCostumeCost");
          ResultProcessor.processItem(ItemPool.COIN, -cost);
          Preferences.increment("plumberCostumeCost", 50);
          Preferences.setString("plumberCostumeWorn", costume);
          break;
        }

      case 1408:
        {
          // Mushroom District Badge Shop
          // If successful, deduct coins.

          if (text.contains("You acquire a skill")) {
            int cost = Preferences.getInteger("plumberBadgeCost");
            ResultProcessor.processItem(ItemPool.COIN, -cost);
            Preferences.increment("plumberBadgeCost", 25);
          }
          break;
        }

      case 1410:
        {
          // The Mushy Center
          int mushroomLevel = 1; // Tomorrow's mushroom
          switch (ChoiceManager.lastDecision) {
            case 1:
              // Fertilize the mushroom
              mushroomLevel = Preferences.increment("mushroomGardenCropLevel", 1, 11, false);
              break;
            case 2:
              // Pick the mushroom
              Preferences.setInteger("mushroomGardenCropLevel", 1);
              break;
          }
          CampgroundRequest.clearCrop();
          CampgroundRequest.setCampgroundItem(new Mushroom(mushroomLevel));
          Preferences.setBoolean("_mushroomGardenVisited", true);
          break;
        }

      case 1411:
        {
          // The Hall in the Hall
          switch (ChoiceManager.lastDecision) {
            case 1:
              Preferences.setBoolean("_drippingHallDoor1", true);
              // If you acquire a drippy org, count it
              if (text.contains("drippy orb")) {
                Preferences.increment("drippyOrbsClaimed");
              } else {
                int known = Preferences.getInteger("drippyOrbsClaimed");
                int min = KoLCharacter.estimatedPoolSkill() / 20;
                Preferences.setInteger("drippyOrbsClaimed", Math.max(known, min));
              }
              break;
            case 2:
              Preferences.setBoolean("_drippingHallDoor2", true);
              break;
            case 3:
              Preferences.setBoolean("_drippingHallDoor3", true);
              break;
            case 4:
              Preferences.setBoolean("_drippingHallDoor4", true);

              // If you acquire drippy pilsner, uses a drippy stein
              if (text.contains("drippy pilsner")) {
                ResultProcessor.processItem(ItemPool.DRIPPY_STEIN, -1);
              }
              break;
          }

          // This only advances upon defeating a dripping reveler
          // or encountering this choice
          int advs = Preferences.increment("drippingHallAdventuresSinceAscension");

          // Since this choice appears on a schedule - the 12th
          // adventure in The Dripping Hall and then every 12
          // turns thereafter - "fix" the adventure count as needed.
          if (advs < 12) {
            Preferences.setInteger("drippingHallAdventuresSinceAscension", 12);
            advs = 12;
          }
          int mod = advs % 12;
          if (mod != 0) {
            Preferences.increment("drippingHallAdventuresSinceAscension", 12 - mod);
          }

          break;
        }

      case 1412:
        {
          // Guzzlr Client Selection
          switch (ChoiceManager.lastDecision) {
            case 1:
              // Abandon
              Preferences.setBoolean("_guzzlrQuestAbandoned", true);

              Preferences.setString("guzzlrQuestBooze", "");
              Preferences.setString("guzzlrQuestClient", "");
              Preferences.setString("guzzlrQuestLocation", "");
              Preferences.setString("guzzlrQuestTier", "");
              QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.UNSTARTED);
              break;
            case 2:
            case 3:
            case 4:
              {
                Matcher locationMatcher = GUZZLR_LOCATION_PATTERN.matcher(text);
                Matcher boozeMatcher = DESCID_PATTERN.matcher(text);

                String tier = "";

                if (locationMatcher.find()) {
                  tier = locationMatcher.group(1).toLowerCase();
                  Preferences.setString("guzzlrQuestClient", locationMatcher.group(2));
                  Preferences.setString("guzzlrQuestLocation", locationMatcher.group(4));
                }

                // If we didn't capture from the ouput we can determine from the choice
                if (tier.equals("")) {
                  tier =
                      ChoiceManager.lastDecision == 2
                          ? "bronze"
                          : ChoiceManager.lastDecision == 3 ? "gold" : "platinum";
                }

                // Remember the tier of the current quest
                Preferences.setString("guzzlrQuestTier", tier);

                // Increment the number of gold or platinum deliveres STARTED today
                if (!tier.equals("bronze")) {
                  Preferences.increment(
                      "_guzzlr" + StringUtilities.toTitleCase(tier) + "Deliveries",
                      tier == "gold" ? 3 : 1);
                }

                if (boozeMatcher.find()) {
                  int itemId = ItemDatabase.getItemIdFromDescription(boozeMatcher.group(1));
                  Preferences.setString("guzzlrQuestBooze", ItemDatabase.getItemName(itemId));
                }

                if (Preferences.getString("guzzlrQuestBooze") == ""
                    || Preferences.getString("guzzlrQuestLocation") == "") {
                  RequestThread.postRequest(new QuestLogRequest());
                }

                int itemId = ItemDatabase.getItemId(Preferences.getString("guzzlrQuestBooze"));

                if (itemId > 0 && itemId != ItemPool.GUZZLR_COCKTAIL_SET) {
                  if (InventoryManager.getCount(itemId) > 0) {
                    QuestDatabase.setQuestProgress(Quest.GUZZLR, "step1");
                  } else {
                    QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
                  }
                } else {
                  QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
                }
                break;
              }
          }

          break;
        }

      case 1414:
        Preferences.setBoolean("lockPicked", true);
        break;

      case 1420:
        // Cargo Cultist Shorts
        if (ChoiceManager.lastDecision == 1) {
          CargoCultistShortsRequest.parsePocketPick(urlString, text);
        }
        break;

      case 1435:
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("mappingMonsters", false);
        }
        break;
      case 1437:
        {
          // Configuring the retro cape washing instructions
          String instructions =
              ChoiceManager.lastDecision == 2
                  ? "hold"
                  : ChoiceManager.lastDecision == 3
                      ? "thrill"
                      : ChoiceManager.lastDecision == 4
                          ? "kiss"
                          : ChoiceManager.lastDecision == 5 ? "kill" : null;

          if (instructions != null) {
            Preferences.setString("retroCapeWashingInstructions", instructions);
            ItemDatabase.setCapeSkills();
          }
          break;
        }
      case 1438:
        {
          // Configuring the retro cape superhero
          String hero =
              ChoiceManager.lastDecision == 1
                  ? "vampire"
                  : ChoiceManager.lastDecision == 2
                      ? "heck"
                      : ChoiceManager.lastDecision == 3 ? "robot" : null;

          if (hero != null) {
            Preferences.setString("retroCapeSuperhero", hero);
            ItemDatabase.setCapeSkills();
          }
          break;
        }
      case 1442:
        {
          if (text.contains("You fill out all the appropriate forms")) {
            ResultProcessor.removeItem(ItemPool.GOVERNMENT_FOOD_SHIPMENT);
          }
          break;
        }
      case 1443:
        {
          if (text.contains("You fill out all the appropriate forms")) {
            ResultProcessor.removeItem(ItemPool.GOVERNMENT_BOOZE_SHIPMENT);
          }
          break;
        }
      case 1444:
        {
          if (text.contains("You fill out all the appropriate forms")) {
            ResultProcessor.removeItem(ItemPool.GOVERNMENT_CANDY_SHIPMENT);
          }
          break;
        }
      case 1445:
        {
          // KoL may have unequipped some items based on our selection
          Matcher partMatcher = Pattern.compile("part=([^&]*)").matcher(urlString);
          Matcher chosenPartMatcher = Pattern.compile("p=([^&]*)").matcher(urlString);
          String part = partMatcher.find() ? partMatcher.group(1) : null;
          int chosenPart =
              chosenPartMatcher.find() ? StringUtilities.parseInt(chosenPartMatcher.group(1)) : 0;

          if (part != null && !part.equals("cpus") && chosenPart != 0) {
            // If we have set our "top" to anything other than 2, we now have no familiar
            if (part.equals("top") && chosenPart != 2) {
              KoLCharacter.setFamiliar(FamiliarData.NO_FAMILIAR);
            }

            // If we've set any part of the main body to anything other than 4, we are now missing
            // an equip
            if (chosenPart != 4) {
              int slot = -1;

              switch (part) {
                case "top":
                  slot = EquipmentManager.HAT;
                  break;
                case "right":
                  slot = EquipmentManager.OFFHAND;
                  break;
                case "bottom":
                  slot = EquipmentManager.PANTS;
                  break;
                case "left":
                  slot = EquipmentManager.WEAPON;
                  break;
              }

              if (slot != -1) {
                EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);
              }
            }
          }

          ScrapheapRequest.parseConfiguration(text);

          if (urlString.contains("show=cpus")) {
            ScrapheapRequest.parseCPUUpgrades(text);
          }

          KoLCharacter.updateStatus();
          break;
        }
      case 1447:
        {
          KoLCharacter.updateStatus();
          break;
        }
      case 1448:
        {
          if (text.contains("You acquire an item:")) {
            Matcher stalkMatcher = Pattern.compile("pp=(\\d+)").matcher(urlString);
            if (stalkMatcher.find()) {
              int pp = Integer.parseInt(stalkMatcher.group(1)) - 1;
              String[] stalkStatus = Preferences.getString("_pottedPowerPlant").split(",");
              stalkStatus[pp] = "0";
              Preferences.setString("_pottedPowerPlant", String.join(",", stalkStatus));
            }
          }
          break;
        }
      case 1449:
        {
          switch (ChoiceManager.lastDecision) {
            case 1:
              Preferences.setString("backupCameraMode", "ml");
              break;
            case 2:
              Preferences.setString("backupCameraMode", "meat");
              break;
            case 3:
              Preferences.setString("backupCameraMode", "init");
              break;
            case 4:
              Preferences.setBoolean("backupCameraReverserEnabled", true);
              break;
            case 5:
              Preferences.setBoolean("backupCameraReverserEnabled", false);
              break;
          }

          break;
        }
      case 1451:
        // Fire Captain Hagnk
        if (ChoiceManager.lastDecision == 1) {
          Matcher locationIdMatcher = Pattern.compile("zid=([^&]*)").matcher(urlString);
          if (locationIdMatcher.find()) {
            int zid = StringUtilities.parseInt(locationIdMatcher.group(1));
            KoLAdventure location =
                AdventureDatabase.getAdventureByURL("adventure.php?snarfblat=" + zid);
            WildfireCampRequest.reduceFireLevel(location);
          }
        } else if (ChoiceManager.lastDecision == 3) {
          if (text.contains("Hagnk takes your fire extinguisher")) {
            Preferences.setInteger("_fireExtinguisherCharge", 100);
            Preferences.setBoolean("_fireExtinguisherRefilled", true);
          }
        }
        WildfireCampRequest.parseCaptain(text);
        break;
      case 1452:
        // Sprinkler Joe
        if (ChoiceManager.lastDecision == 1 && text.contains("raindrop.gif")) {
          Preferences.setBoolean("wildfireSprinkled", true);
        }
        break;
      case 1453:
        // Fracker Dan
        if (ChoiceManager.lastDecision == 1 && text.contains("raindrop.gif")) {
          Preferences.setBoolean("wildfireFracked", true);
        }
        break;
      case 1454:
        // Cropduster Dusty
        if (ChoiceManager.lastDecision == 1 && text.contains("raindrop.gif")) {
          Preferences.setBoolean("wildfireDusted", true);
        }
        break;
    }

    // Certain choices cost meat or items when selected
    ChoiceManager.payCost(ChoiceManager.lastChoice, ChoiceManager.lastDecision);
  }

  // <td align="center" valign="middle"><a href="choice.php?whichchoice=810&option=1&slot=7&pwd=xxx"
  // style="text-decoration:none"><img alt='Toybot (Level 3)' title='Toybot (Level 3)' border=0
  // src='http://images.kingdomofloathing.com/otherimages/crimbotown/krampus_toybot.gif' /></a></td>
  private static final Pattern URL_SLOT_PATTERN = Pattern.compile("slot=(\\d+)");
  private static final Pattern BOT_PATTERN = Pattern.compile("<td.*?<img alt='([^']*)'.*?</td>");

  private static String findKRAMPUSBot(final String urlString, final String responseText) {
    Matcher slotMatcher = ChoiceManager.URL_SLOT_PATTERN.matcher(urlString);
    if (!slotMatcher.find()) {
      return null;
    }
    String slotString = slotMatcher.group(0);
    Matcher botMatcher = ChoiceManager.BOT_PATTERN.matcher(responseText);
    while (botMatcher.find()) {
      if (botMatcher.group(0).contains(slotString)) {
        return botMatcher.group(1);
      }
    }
    return null;
  }

  public static void postChoice2(final String urlString, final GenericRequest request) {
    if (ChoiceManager.nonInterruptingRequest(urlString, request)) {
      return;
    }

    // The following are requests that may or may not be allowed at
    // any time, but we do them in automation during result
    // processing and they do not count as "walking away"
    if (urlString.startsWith("diary.php")) {
      return;
    }

    // Things that can or need to be done AFTER processing results.
    String text = request.responseText;

    // If you walked away from the choice (or we automated during
    // result processing), this is not a choice page
    if (ChoiceManager.canWalkAway
        && !urlString.startsWith("choice.php")
        && !urlString.startsWith("fight.php")) {
      // I removed the following line, but it caused issues.
      ChoiceManager.handlingChoice = false;
      return;
    }

    ChoiceManager.handlingChoice = ChoiceManager.stillInChoice(text);

    if (ChoiceManager.lastChoice == 0 || ChoiceManager.lastDecision == 0) {
      // This was a visit
      return;
    }

    switch (ChoiceManager.lastChoice) {
      case 3:
        // The Oracle Will See You Now
        if (text.contains("actually a book")) {
          Preferences.setInteger("lastPlusSignUnlock", KoLCharacter.getAscensions());
        }
        break;
      case 7:
        // How Depressing

        if (ChoiceManager.lastDecision == 1) {
          EquipmentManager.discardEquipment(ItemPool.SPOOKY_GLOVE);
        }
        break;

      case 21:
        // Under the Knife
        if (ChoiceManager.lastDecision == 1 && text.contains("anaesthetizes you")) {
          Preferences.increment("sexChanges", 1);
          Preferences.setBoolean("_sexChanged", true);
          KoLCharacter.setGender(
              text.contains("in more ways than one") ? KoLCharacter.FEMALE : KoLCharacter.MALE);
          ConcoctionDatabase.setRefreshNeeded(false);
        }
        break;

      case 48:
      case 49:
      case 50:
      case 51:
      case 52:
      case 53:
      case 54:
      case 55:
      case 56:
      case 57:
      case 58:
      case 59:
      case 60:
      case 61:
      case 62:
      case 63:
      case 64:
      case 65:
      case 66:
      case 67:
      case 68:
      case 69:
      case 70:
        // Choices in the Violet Fog
        VioletFogManager.mapChoice(ChoiceManager.lastChoice, ChoiceManager.lastDecision, text);
        break;

      case 73:
        // Don't Fence Me In
        if (ChoiceManager.lastDecision == 3) {
          if (text.contains("you pick") || text.contains("you manage")) {
            Preferences.increment("_whiteRiceDrops", 1);
          }
        }
        break;

      case 89:
        if (ChoiceManager.lastDecision == 4) {
          TurnCounter.startCounting(10, "Garden Banished loc=*", "wolfshield.gif");
        }
        break;

      case 105:
        if (ChoiceManager.lastDecision == 3) {
          checkGuyMadeOfBees(request);
        }
        break;

      case 112:
        // Please, Hammer
        if (ChoiceManager.lastDecision == 1 && KoLmafia.isAdventuring()) {
          InventoryManager.retrieveItem(ItemPool.get(ItemPool.HAROLDS_HAMMER, 1));
        }
        break;

      case 125:
        // No visible means of support
        if (ChoiceManager.lastDecision == 3) {
          QuestDatabase.setQuestProgress(Quest.WORSHIP, "step3");
        }
        break;

      case 132:
        // Let's Make a Deal!
        if (ChoiceManager.lastDecision == 2) {
          QuestDatabase.setQuestProgress(Quest.PYRAMID, "step1");
        }
        break;

      case 162:
        // Between a Rock and Some Other Rocks
        if (KoLmafia.isAdventuring()
            && !EquipmentManager.isWearingOutfit(OutfitPool.MINING_OUTFIT)
            && !KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.EARTHEN_FIST))) {
          QuestManager.unlockGoatlet();
        }
        break;

      case 197:
        // Somewhat Higher and Mostly Dry
      case 198:
        // Disgustin' Junction
      case 199:
        // The Former or the Ladder
        if (ChoiceManager.lastDecision == 1) {
          ChoiceManager.checkDungeonSewers(request);
        }
        break;

      case 200:
        // Enter The Hoboverlord
      case 201:
        // Home, Home in the Range
      case 202:
        // Bumpity Bump Bump
      case 203:
        // Deep Enough to Dive
      case 204:
        // Welcome To You!
      case 205:
        // Van, Damn

        // Stop for Hobopolis bosses
        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(
              MafiaState.PENDING,
              ChoiceManager.hobopolisBossName(ChoiceManager.lastChoice) + " waits for you.");
        }
        break;

      case 299:
        // Down at the Hatch
        if (ChoiceManager.lastDecision == 2) {
          // The first time you take option 2, you
          // release Big Brother. Subsequent times, you
          // release other creatures.
          Preferences.setBoolean("bigBrotherRescued", true);
          QuestDatabase.setQuestProgress(Quest.SEA_MONKEES, "step2");
          ConcoctionDatabase.setRefreshNeeded(false);
        }
        break;

      case 304:
        // A Vent Horizon

        // "You conjure some delicious batter from the core of
        // the thermal vent. It pops and sizzles as you stick
        // it in your sack."

        if (text.contains("pops and sizzles")) {
          Preferences.increment("tempuraSummons", 1);
        }
        break;

      case 309:
        // Barback

        // "You head down the tunnel into the cave, and manage
        // to find another seaode. Sweet! I mean... salty!"

        if (text.contains("salty!")) {
          Preferences.increment("seaodesFound", 1);
        }
        break;

      case 326:
        // Showdown

        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(MafiaState.ABORT, "Mother Slime waits for you.");
        }
        break;

      case 330:
        // A Shark's Chum
        if (ChoiceManager.lastDecision == 1) {
          Preferences.increment("poolSharkCount", 1);
        }
        break;

      case 360:
        WumpusManager.takeChoice(ChoiceManager.lastDecision, text);
        break;

      case 441:
        // The Mad Tea Party

        // I'm sorry, but there's a very strict dress code for
        // this party

        if (ChoiceManager.lastDecision == 1 && !text.contains("very strict dress code")) {
          Preferences.setBoolean("_madTeaParty", true);
        }
        break;

      case 442:
        // A Moment of Reflection
        if (ChoiceManager.lastDecision == 5) {
          // Option 5 is Chess Puzzle
          RabbitHoleManager.parseChessPuzzle(text);
        }

        if (ChoiceManager.lastDecision != 6) {
          // Option 6 does not consume the map. Others do.
          ResultProcessor.processItem(ItemPool.REFLECTION_OF_MAP, -1);
        }
        break;

      case 508:
        // Pants-Gazing
        if (text.contains("You acquire an effect")) {
          Preferences.increment("_gapBuffs", 1);
        }
        break;

      case 517:
        // Mr. Alarm, I presarm
        QuestDatabase.setQuestIfBetter(Quest.PALINDOME, "step3");
        break;

      case 518:
        // Clear and Present Danger

        // Stop for Hobopolis bosses
        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(
              MafiaState.PENDING,
              ChoiceManager.hobopolisBossName(ChoiceManager.lastChoice) + " waits for you.");
        }
        break;

      case 524:
        // The Adventures of Lars the Cyberian
        if (text.contains("Skullhead's Screw")) {
          // You lose the book if you receive the reward.
          // I don't know if that's always the result of
          // the same choice option
          ResultProcessor.processItem(ItemPool.LARS_THE_CYBERIAN, -1);
        }
        break;

      case 548:
        // Behind Closed Doors
        if (ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring()) {
          KoLmafia.updateDisplay(MafiaState.ABORT, "The Necbromancer waits for you.");
        }
        break;

      case 549:
        // Dark in the Attic
        if (text.contains("The silver pellets tear through the sorority werewolves")) {
          ResultProcessor.processItem(ItemPool.SILVER_SHOTGUN_SHELL, -1);
          RequestLogger.printLine("You took care of a bunch of werewolves.");
        } else if (text.contains("quietly sneak away")) {
          RequestLogger.printLine("You need a silver shotgun shell to kill werewolves.");
        } else if (text.contains("a loose shutter")) {
          RequestLogger.printLine("All the werewolves have been defeated.");
        } else if (text.contains("crank up the volume on the boombox")) {
          RequestLogger.printLine("You crank up the volume on the boombox.");
        } else if (text.contains("a firm counterclockwise twist")) {
          RequestLogger.printLine("You crank down the volume on the boombox.");
        }
        break;

      case 550:
        // The Unliving Room
        if (text.contains("you pull out the chainsaw blades")) {
          ResultProcessor.processItem(ItemPool.CHAINSAW_CHAIN, -1);
          RequestLogger.printLine("You took out a bunch of zombies.");
        } else if (text.contains("a wet tearing noise")) {
          RequestLogger.printLine("You need a chainsaw chain to kill zombies.");
        } else if (text.contains("a bloody tangle")) {
          RequestLogger.printLine("All the zombies have been defeated.");
        } else if (text.contains("the skeletons collapse into piles of loose bones")) {
          ResultProcessor.processItem(ItemPool.FUNHOUSE_MIRROR, -1);
          RequestLogger.printLine("You made short work of some skeletons.");
        } else if (text.contains("couch in front of the door")) {
          RequestLogger.printLine("You need a funhouse mirror to kill skeletons.");
        } else if (text.contains("just coats")) {
          RequestLogger.printLine("All the skeletons have been defeated.");
        } else if (text.contains("close the windows")) {
          RequestLogger.printLine("You close the windows.");
        } else if (text.contains("open the windows")) {
          RequestLogger.printLine("You open the windows.");
        }
        break;

      case 551:
        // Debasement
        if (text.contains("the vampire girls shriek")) {
          RequestLogger.printLine("You slew some vampires.");
        } else if (text.contains("gets back in her coffin")) {
          RequestLogger.printLine("You need to equip plastic vampire fangs to kill vampires.");
        } else if (text.contains("they recognize you")) {
          RequestLogger.printLine("You have already killed some vampires.");
        } else if (text.contains("crank up the fog machine")) {
          RequestLogger.printLine("You crank up the fog machine.");
        } else if (text.contains("turn the fog machine way down")) {
          RequestLogger.printLine("You crank down the fog machine.");
        }
        break;

      case 553:
        // Relocked and Reloaded
        if (text.contains("You melt")) {
          int item = 0;
          switch (ChoiceManager.lastDecision) {
            case 1:
              item = ItemPool.MAXWELL_HAMMER;
              break;
            case 2:
              item = ItemPool.TONGUE_BRACELET;
              break;
            case 3:
              item = ItemPool.SILVER_CHEESE_SLICER;
              break;
            case 4:
              item = ItemPool.SILVER_SHRIMP_FORK;
              break;
            case 5:
              item = ItemPool.SILVER_PATE_KNIFE;
              break;
          }
          if (item > 0) {
            ResultProcessor.processItem(item, -1);
          }
        }
        break;

      case 558:
        // Tool Time
        if (text.contains("You acquire an item")) {
          int amount = 3 + ChoiceManager.lastDecision;
          ResultProcessor.processItem(ItemPool.LOLLIPOP_STICK, -amount);
        }
        break;

      case 578:
        // End of the Boris Road
        ChoiceManager.handleAfterAvatar();
        break;

      case 579:
        // Such Great Heights
        if (ChoiceManager.lastDecision == 3
            && Preferences.getInteger("lastTempleAdventures") != KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastTempleAdventures", KoLCharacter.getAscensions());
        }
        break;

      case 581:
        // Such Great Depths
        if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("_templeHiddenPower", true);
        }
        break;

      case 584:
        // Unconfusing Buttons
        if (Preferences.getInteger("lastTempleButtonsUnlock") != KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastTempleButtonsUnlock", KoLCharacter.getAscensions());
        }
        if (InventoryManager.getCount(ItemPool.NOSTRIL_OF_THE_SERPENT) > 0) {
          ResultProcessor.processItem(ItemPool.NOSTRIL_OF_THE_SERPENT, -1);
        }
        if (ChoiceManager.lastDecision == 4) {
          QuestDatabase.setQuestIfBetter(Quest.WORSHIP, "step2");
        }
        break;

      case 595:
        // Fire! I... have made... fire!
        Preferences.setBoolean("_fireStartingKitUsed", true);
        break;

      case 602:
        // Behind the Gash
        // This is a multi-part choice adventure, and we only want to handle the last choice
        if (text.contains("you shout into the blackness")) {
          ChoiceManager.handleAfterAvatar();
        }
        break;

      case 613:
        // Behind the door there is a fog
        if (ChoiceManager.lastDecision == 1) {
          Matcher fogMatcher = FOG_PATTERN.matcher(text);
          if (fogMatcher.find()) {
            String message = "Message: \"" + fogMatcher.group(1) + "\"";
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }
        }
        break;

      case 633:
        // ChibiBuddy&trade;
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.processItem(ItemPool.CHIBIBUDDY_OFF, -1);
          ResultProcessor.processItem(ItemPool.CHIBIBUDDY_ON, 1);
        }
        break;

      case 640:
        // Tailor the Snow Suit
        Preferences.setString(
            "snowsuit", SnowsuitCommand.DECORATION[ChoiceManager.lastDecision - 1][0]);
        break;

      case 641:
        // Stupid Pipes.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel1", true);
        }
        break;

      case 642:
        // You're Freaking Kidding Me
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel2", true);
        }
        break;

      case 644:
        // Snakes.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel3", true);
        }
        break;

      case 645:
        // So... Many... Skulls...
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel4", true);
        }
        break;

      case 647:
        // A Stupid Dummy. Also, a Straw Man.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel5", true);
        }
        break;

      case 648:
        // Slings and Arrows
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel6", true);
        }
        break;

      case 650:
        // This Is Your Life. Your Horrible, Horrible Life.
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel7", true);
        }
        break;

      case 651:
        // The Wall of Wailing
        if (ChoiceManager.lastDecision == 2 && text.contains("flickering pixel")) {
          Preferences.setBoolean("flickeringPixel8", true);
        }
        break;

      case 677:
        // Copper Feel
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.removeItem(ItemPool.MODEL_AIRSHIP);
        }
        break;

      case 682:
        // Now Leaving Jarlsberg, Population You
        ChoiceManager.handleAfterAvatar();
        break;

      case 696:
        // Stick a Fork In It
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("maraisDarkUnlock", true);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("maraisWildlifeUnlock", true);
        }
        break;

      case 697:
        // Sophie's Choice
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("maraisCorpseUnlock", true);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("maraisWizardUnlock", true);
        }
        break;

      case 698:
        // From Bad to Worst
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("maraisBeaverUnlock", true);
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setBoolean("maraisVillageUnlock", true);
        }
        break;

      case 704:
        // Playing the Catalog Card
        DreadScrollManager.handleLibrary(text);
        break;

      case 774:
        // Opening up the Folder Holder

        // Choice 1 is adding a folder.
        if (ChoiceManager.lastDecision == 1
            && text.contains("You carefully place your new folder in the holder")) {
          // Figure out which one it was from the URL
          String id = request.getFormField("folder");
          AdventureResult folder = EquipmentRequest.idToFolder(id);
          ResultProcessor.removeItem(folder.getItemId());
        }

        // Choice 2 is removing a folder. Since the folder is
        // destroyed, it does not go back to inventory.

        // Set all folder slots from the response text
        EquipmentRequest.parseFolders(text);
        break;

      case 786:
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.autoCreate(ItemPool.MCCLUSKY_FILE);
        }
        break;

      case 798:
        // Hippy Talkin'
        if (text.contains("Point me at the landfill")) {
          QuestDatabase.setQuestProgress(Quest.HIPPY, QuestDatabase.STARTED);
        }
        break;

      case 810:
        if (ChoiceManager.lastDecision == 2) {
          ResultProcessor.processItem(ItemPool.WARBEAR_WHOSIT, -100);
        } else if (ChoiceManager.lastDecision == 4 && text.contains("You upgrade the robot!")) {
          String bot = ChoiceManager.findKRAMPUSBot(request.getURLString(), text);
          int cost =
              (bot == null) ? 0 : bot.contains("Level 2") ? 250 : bot.contains("Level 3") ? 500 : 0;
          if (cost != 0) {
            ResultProcessor.processItem(ItemPool.WARBEAR_WHOSIT, -cost);
          }
        }
        break;

      case 822:
      case 823:
      case 824:
      case 825:
      case 826:
      case 827:
        {
          // The Prince's Ball
          if (ChoiceManager.parseCinderellaTime() == false) {
            Preferences.decrement("cinderellaMinutesToMidnight");
          }
          Matcher matcher = ChoiceManager.CINDERELLA_SCORE_PATTERN.matcher(text);
          if (matcher.find()) {
            int score = StringUtilities.parseInt(matcher.group(1));
            if (score != -1) {
              Preferences.setInteger("cinderellaScore", score);
            }
          }
          if (text.contains("Your final score was")) {
            Preferences.setInteger("cinderellaMinutesToMidnight", 0);
            Preferences.setString("grimstoneMaskPath", "");
          }
          break;
        }

      case 829:
        // We all wear masks
        if (ChoiceManager.lastDecision != 6) {
          ResultProcessor.processItem(ItemPool.GRIMSTONE_MASK, -1);
          Preferences.setInteger("cinderellaMinutesToMidnight", 0);
        }
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("cinderellaMinutesToMidnight", 30);
          Preferences.setInteger("cinderellaScore", 0);
          Preferences.setString("grimstoneMaskPath", "stepmother");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("grimstoneMaskPath", "wolf");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("grimstoneMaskPath", "witch");
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setString("grimstoneMaskPath", "gnome");
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setString("grimstoneMaskPath", "hare");
        }
        RumpleManager.reset(ChoiceManager.lastDecision);
        break;

      case 844:
        // The Portal to Horrible Parents
        if (ChoiceManager.lastDecision == 1) {
          RumpleManager.spyOnParents(text);
        }
        break;

      case 846:
        // Bartering for the Future of Innocent Children
        RumpleManager.pickParent(ChoiceManager.lastDecision);
        break;

      case 847:
        // Pick Your Poison
        RumpleManager.pickSin(ChoiceManager.lastDecision);
        break;

      case 848:
        // Where the Magic Happens
        if (ChoiceManager.lastDecision != 4) {
          RumpleManager.recordTrade(text);
        }
        break;

      case 854:
        // Shen Copperhead, World's Biggest Jerk
        if (InventoryManager.hasItem(ItemPool.COPPERHEAD_CHARM)
            && InventoryManager.hasItem(ItemPool.COPPERHEAD_CHARM_RAMPANT)) {
          ResultProcessor.autoCreate(ItemPool.TALISMAN);
        }
        break;

      case 855:
        {
          // Behind the 'Stache
          String hazard = Preferences.getString("copperheadClubHazard");
          switch (ChoiceManager.lastDecision) {
            case 1:
              hazard = "gong";
              break;
            case 2:
              hazard = "ice";
              break;
            case 3:
              hazard = "lantern";
              break;
          }
          Preferences.setString("copperheadClubHazard", hazard);
          break;
        }

      case 856:
        {
          // This Looks Like a Good Bush for an Ambush
          Matcher lynyrdMatcher = ChoiceManager.LYNYRD_PATTERN.matcher(text);
          if (lynyrdMatcher.find()) {
            int protestersScared = StringUtilities.parseInt(lynyrdMatcher.group(1));
            Preferences.increment("zeppelinProtestors", protestersScared);
            RequestLogger.printLine("Scared off " + protestersScared + " protesters");
          }
          break;
        }

      case 857:
        {
          // Bench Warrant
          Matcher benchWarrantMatcher = ChoiceManager.BENCH_WARRANT_PATTERN.matcher(text);
          if (benchWarrantMatcher.find()) {
            int protestersCreeped = StringUtilities.parseInt(benchWarrantMatcher.group(1));
            Preferences.increment("zeppelinProtestors", protestersCreeped);
            RequestLogger.printLine("Creeped out " + protestersCreeped + " protesters");
          }
          break;
        }

      case 858:
        // Fire Up Above
        if (text.contains("three nearest protesters")) {
          Preferences.increment("zeppelinProtestors", 3);
          RequestLogger.printLine("Soaked 3 protesters");
        } else if (text.contains("Flamin' Whatshisname")) {
          Preferences.increment("zeppelinProtestors", 10);
          ResultProcessor.processItem(ItemPool.FLAMIN_WHATSHISNAME, -1);
          RequestLogger.printLine("Set fire to 10 protesters");
        }
        break;

      case 860:
        // Another Tired Retread
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeTires", "Racing Slicks");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeTires", "Spiky Tires");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeTires", "Snow Tires");
        }
        break;
      case 861:
        // Station of the Gas
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeGasTank", "Large Capacity Tank");
          KoLCharacter.setDesertBeachAvailable();
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeGasTank", "Extra-Buoyant Tank");
          Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeGasTank", "Nitro-Burnin' Funny Tank");
        }
        break;
      case 862:
        // Me and Cinderella Put It All Together
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeHeadlight", "Ultrabright Yellow Bulb");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeHeadlight", "Party Bulb");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeHeadlight", "Blacklight Bulb");
        }
        break;
      case 863:
        // Endowing the Cowling
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeCowling", "Ghost Vacuum");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeCowling", "Rocket Launcher");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeCowling", "Sweepy Red Light");
        }
        break;
      case 864:
        // Diving into the Mufflers
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeMuffler", "Extra-Loud Muffler");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeMuffler", "Extra-Quiet Muffler");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeMuffler", "Extra-Smelly Muffler");
        }
        break;
      case 865:
        // Ayy, Sit on It
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setString("peteMotorbikeSeat", "Massage Seat");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("peteMotorbikeSeat", "Deep Seat Cushions");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("peteMotorbikeSeat", "Sissy Bar");
        }
        break;

      case 869:
        // End of Pete Road
        ChoiceManager.handleAfterAvatar();
        break;

      case 875:
        {
          // Welcome To Our ool Table
          Matcher poolSkillMatcher = ChoiceManager.POOL_SKILL_PATTERN.matcher(text);
          if (poolSkillMatcher.find()) {
            Preferences.increment("poolSkill", StringUtilities.parseInt(poolSkillMatcher.group(1)));
          }
          break;
        }

      case 918:
        // Yachtzee!
        if (text.contains("Ultimate Mind Destroyer")) {
          Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT-0700"));
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
          String today = sdf.format(date.getTime());
          Preferences.setString("umdLastObtained", today);
        }
        break;

      case 921:
        // We'll All Be Flat
        QuestDatabase.setQuestProgress(Quest.MANOR, "step1");
        break;

      case 928:
        // The Blackberry Cobbler
        if (ChoiceManager.lastDecision != 6) {
          ResultProcessor.processItem(ItemPool.BLACKBERRY, -3);
        }
        break;

      case 929:
        // Control Freak
        if (ChoiceManager.lastDecision == 1 && text.contains("wooden wheel disintegrating")) {
          ResultProcessor.processItem(ItemPool.CRUMBLING_WHEEL, -1);
          PyramidRequest.advancePyramidPosition();
        } else if (ChoiceManager.lastDecision == 2
            && text.contains("snap the ratchet onto the peg")) {
          ResultProcessor.processItem(ItemPool.TOMB_RATCHET, -1);
          PyramidRequest.advancePyramidPosition();
        }
        break;

      case 940:
        // Let Your Fists Do The Walking
        if (ChoiceManager.lastDecision == 6) {
          ResultProcessor.processItem(ItemPool.WHITE_PAGE, 1);
        }
        break;

      case 974:
        {
          // Around The World
          Matcher pinkWordMatcher = ChoiceManager.PINK_WORD_PATTERN.matcher(text);
          if (pinkWordMatcher.find()) {
            String pinkWord = pinkWordMatcher.group(1);
            String message =
                "Bohemian Party Pink Word found: "
                    + pinkWord
                    + " in clan "
                    + ClanManager.getClanName(false)
                    + ".";
            RequestLogger.printLine("<font color=\"blue\">" + message + "</font>");
            RequestLogger.updateSessionLog(message);
          }
          break;
        }

      case 975:
        {
          // Crazy Still After All These Years
          Matcher stillMatcher = ChoiceManager.STILL_PATTERN.matcher(text);
          if (stillMatcher.find()) {
            ResultProcessor.processItem(
                ItemPool.COCKTAIL_ONION, -StringUtilities.parseInt(stillMatcher.group(1)));
          }
          break;
        }

      case 977:
        // [Chariot Betting]
        if (ChoiceManager.lastDecision < 12) {
          ResultProcessor.processItem(ItemPool.CHRONER, -10);
        }
        break;

      case 981:
        // [back room]
        if (ChoiceManager.lastDecision < 12) {
          ResultProcessor.processItem(ItemPool.MERCURY_BLESSING, -1);
        }
        break;

      case 982:
      case 983:
        // Playing Dice With Romans & The 99-Centurion Store
        if (ChoiceManager.lastDecision < 6) {
          ResultProcessor.processItem(ItemPool.CHRONER, -1);
        }
        break;

      case 1042: // Pick a Perk
        KoLmafia.resetAfterLimitmode();
        break;

      case 1054:
        // Returning the MacGuffin
        QuestDatabase.setQuestProgress(Quest.WAREHOUSE, QuestDatabase.FINISHED);
        if (ChoiceManager.lastDecision == 1) {
          ResultProcessor.processItem(ItemPool.ED_HOLY_MACGUFFIN, -1);
        }
        break;

      case 1055:
        // Returning the MacGuffin
        KoLCharacter.liberateKing();
        ChoiceManager.handleAfterAvatar();
        break;

      case 1062:
        // Lots of Options
        if (ChoiceManager.lastDecision == 5) {
          ResultProcessor.processItem(ItemPool.BOOZE_MAP, -1);
        }
        break;

      case 1073:
        // This Ride Is Like... A Rollercoaster Baby Baby
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("dinseyRollercoasterNext", false);
        }
        if (text.contains("lubricating every inch of the tracks")) {
          QuestDatabase.setQuestProgress(Quest.SUPER_LUBER, "step2");
        }
        break;

      case 1089: // Community Service
        if (text.contains("You acquire")) {
          String quest = null;
          switch (ChoiceManager.lastDecision) {
            case 1:
              quest = "Donate Blood";
              break;
            case 2:
              quest = "Feed The Children";
              break;
            case 3:
              quest = "Build Playground Mazes";
              break;
            case 4:
              quest = "Feed Conspirators";
              break;
            case 5:
              quest = "Breed More Collies";
              break;
            case 6:
              quest = "Reduce Gazelle Population";
              break;
            case 7:
              quest = "Make Sausage";
              break;
            case 8:
              quest = "Be a Living Statue";
              break;
            case 9:
              quest = "Make Margaritas";
              break;
            case 10:
              quest = "Clean Steam Tunnels";
              break;
            case 11:
              quest = "Coil Wire";
              break;
          }
          if (quest != null) {
            String current = Preferences.getString("csServicesPerformed");
            if (current.equals("")) {
              Preferences.setString("csServicesPerformed", quest);
            } else {
              Preferences.setString("csServicesPerformed", current + "," + quest);
            }
          }
        }
        break;

      case 1090:
        // The Towering Inferno Discotheque
        if (ChoiceManager.lastDecision > 1) {
          Preferences.setBoolean("_infernoDiscoVisited", true);
        }
        break;

      case 1091:
        // Choice 1091 is The Floor Is Yours
        if (text.contains("You acquire")) {
          switch (ChoiceManager.lastDecision) {
            case 1:
              ResultProcessor.processResult(ItemPool.get(ItemPool.GOLD_1970, -1));
              break;
            case 2:
              ResultProcessor.processResult(ItemPool.get(ItemPool.NEW_AGE_HEALING_CRYSTAL, -1));
              break;
            case 3:
              ResultProcessor.processResult(ItemPool.get(ItemPool.EMPTY_LAVA_BOTTLE, -1));
              break;
            case 5:
              ResultProcessor.processResult(ItemPool.get(ItemPool.GLOWING_NEW_AGE_CRYSTAL, -1));
              break;
            case 6:
              ResultProcessor.processResult(ItemPool.get(ItemPool.CRYSTALLINE_LIGHT_BULB, -1));
              ResultProcessor.processResult(ItemPool.get(ItemPool.INSULATED_GOLD_WIRE, -1));
              ResultProcessor.processResult(ItemPool.get(ItemPool.HEAT_RESISTANT_SHEET_METAL, -1));
              break;
          }
        }
        break;

      case 1092:
        // Choice 1092 is Dyer Maker
        if (text.contains("You acquire")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.VISCOUS_LAVA_GLOBS, -1));
        }
        break;

      case 1093:
        // The WLF Bunker

        // A woman in an orange denim jumpsuit emerges from a
        // hidden trap door, smiles, and hands you a coin in
        // exchange for your efforts.

        if (text.contains("hands you a coin")) {
          String index = String.valueOf(ChoiceManager.lastDecision);
          int itemId = Preferences.getInteger("_volcanoItem" + index);
          int count = Preferences.getInteger("_volcanoItemCount" + index);
          if (itemId > 0 && count > 0) {
            ResultProcessor.processResult(ItemPool.get(itemId, -count));
          }
          Preferences.setBoolean("_volcanoItemRedeemed", true);
          Preferences.setInteger("_volcanoItem1", 0);
          Preferences.setInteger("_volcanoItem2", 0);
          Preferences.setInteger("_volcanoItem3", 0);
          Preferences.setInteger("_volcanoItemCount1", 0);
          Preferences.setInteger("_volcanoItemCount2", 0);
          Preferences.setInteger("_volcanoItemCount3", 0);
        }

        break;

      case 1100:
        // Pray to the Barrel God
        if (ChoiceManager.lastDecision <= 4) {
          Preferences.setBoolean("_barrelPrayer", true);
          ConcoctionDatabase.refreshConcoctions();
        }
        break;

      case 1101:
        {
          // It's a Barrel Smashing Party!
          if (ChoiceManager.lastDecision == 2) {
            // We're smashing 100 barrels
            // The results don't say which barrels are being smashed, but it seems to happen in item
            // order
            int count = 100;
            int itemId = ItemPool.LITTLE_FIRKIN;
            while (count > 0 && itemId <= ItemPool.BARNACLED_BARREL) {
              int smashNumber = Math.min(count, InventoryManager.getCount(itemId));
              if (smashNumber > 0) {
                ResultProcessor.processResult(ItemPool.get(itemId, -smashNumber));
                count -= smashNumber;
              }
              itemId++;
            }
            break;
          }
          int itemId = ChoiceManager.extractIidFromURL(request.getURLString());
          String name = ItemDatabase.getItemName(itemId);
          if (name != null) {
            ResultProcessor.removeItem(itemId);
          }
          break;
        }

      case 1103:
        // Doing the Maths
        if (!text.contains("Try again")) {
          Preferences.increment("_universeCalculated");
        }
        break;

      case 1104:
        // Tree Tea
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_pottedTeaTreeUsed", true);
        }
        break;

      case 1105:
        // Specifici Tea
        if (request.getURLString().contains("itemid")) {
          Preferences.setBoolean("_pottedTeaTreeUsed", true);
        }
        break;

      case 1110:
        // Spoopy
        if (ChoiceManager.lastDecision == 5) {
          if (text.contains("You board up the doghouse")) {
            Preferences.setBoolean("doghouseBoarded", true);
          } else if (text.contains("You unboard-up the doghouse")) {
            Preferences.setBoolean("doghouseBoarded", false);
          }
        }
        break;

      case 1114:
        // Walford Rusley, Bucket Collector
        if (ChoiceManager.lastDecision == 1) {
          QuestDatabase.setQuestProgress(Quest.BUCKET, QuestDatabase.UNSTARTED);
          Preferences.setInteger("walfordBucketProgress", 0);
          Preferences.setString("walfordBucketItem", "");
        } else if (ChoiceManager.lastDecision < 5) {
          QuestDatabase.setQuestProgress(Quest.BUCKET, QuestDatabase.STARTED);
          Preferences.setInteger("walfordBucketProgress", 0);
          Preferences.setBoolean("_walfordQuestStartedToday", true);
          if (text.contains("Bucket of balls")) {
            Preferences.setString("walfordBucketItem", "balls");
          } else if (text.contains("bucket with blood")) {
            Preferences.setString("walfordBucketItem", "blood");
          } else if (text.contains("Bolts, mainly")) {
            Preferences.setString("walfordBucketItem", "bolts");
          } else if (text.contains("bucket of chicken")) {
            Preferences.setString("walfordBucketItem", "chicken");
          } else if (text.contains("Here y'go -- chum")) {
            Preferences.setString("walfordBucketItem", "chum");
          } else if (text.contains("fill that with ice")) {
            Preferences.setString("walfordBucketItem", "ice");
          } else if (text.contains("fill it up with milk")) {
            Preferences.setString("walfordBucketItem", "milk");
          } else if (text.contains("bucket of moonbeams")) {
            Preferences.setString("walfordBucketItem", "moonbeams");
          } else if (text.contains("bucket with rain")) {
            Preferences.setString("walfordBucketItem", "rain");
          }
        }
        break;

      case 1115:
        // VYKEA!
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_VYKEACafeteriaRaided", true);
        } else if (ChoiceManager.lastDecision == 3) {
          Matcher WalfordMatcher = ChoiceManager.WALFORD_PATTERN.matcher(text);
          if (WalfordMatcher.find()) {
            Preferences.increment(
                "walfordBucketProgress", StringUtilities.parseInt(WalfordMatcher.group(1)));
            if (Preferences.getInteger("walfordBucketProgress") >= 100) {
              QuestDatabase.setQuestProgress(Quest.BUCKET, "step2");
            }
          }
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setBoolean("_VYKEALoungeRaided", true);
        }
        break;

      case 1116:
        // All They Got Inside is Vacancy (and Ice)
        if (ChoiceManager.lastDecision == 3) {
          Matcher WalfordMatcher = ChoiceManager.WALFORD_PATTERN.matcher(text);
          if (WalfordMatcher.find()) {
            Preferences.increment(
                "walfordBucketProgress", StringUtilities.parseInt(WalfordMatcher.group(1)));
            if (Preferences.getInteger("walfordBucketProgress") >= 100) {
              QuestDatabase.setQuestProgress(Quest.BUCKET, "step2");
            }
          }
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setBoolean("_iceHotelRoomsRaided", true);
        }
        break;

      case 1119:
        // Shining Mauve Backwards In Time
        Preferences.setInteger("encountersUntilDMTChoice", 49);
        if (ChoiceManager.lastDecision == 4
            && Preferences.getInteger("lastDMTDuplication") != KoLCharacter.getAscensions()) {
          Preferences.setInteger("lastDMTDuplication", KoLCharacter.getAscensions());
        }
        break;

      case 1134:
        // Batfellow Ends
        // (choosing to exit)
        if (ChoiceManager.lastDecision == 1) {
          KoLCharacter.setLimitmode(null);
        }
        break;

      case 1168:
        // Batfellow Ends
        // (from running out of time)
        KoLCharacter.setLimitmode(null);
        break;

      case 1171: // LT&T Office
        if (ChoiceManager.lastDecision < 4) {
          QuestDatabase.setQuestProgress(Quest.TELEGRAM, QuestDatabase.STARTED);
        }
        break;

      case 1172: // The Investigation Begins
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step1");
        Preferences.setInteger("lttQuestStageCount", 0);
        break;

      case 1173: // The Investigation Continues
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step2");
        Preferences.setInteger("lttQuestStageCount", 0);
        break;

      case 1174: // The Investigation Continues
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step3");
        Preferences.setInteger("lttQuestStageCount", 0);
        break;

      case 1175: // The Investigation Thrillingly Concludes!
        QuestDatabase.setQuestProgress(Quest.TELEGRAM, "step4");
        Preferences.setInteger("lttQuestStageCount", 0);
        break;

      case 1188: // The Call is Coming from Outside the Simulation
        if (ChoiceManager.lastDecision == 1) {
          // Skill learned
          Preferences.decrement("sourceEnlightenment", 1, 0);
        }
        break;

      case 1191:
        // Source Terminal
        request.setHasResult(true);
        break;

      case 1198:
        // Play a Time Prank
        if (ChoiceManager.lastDecision == 1 && text.contains("paradoxical time copy")) {
          Preferences.increment("_timeSpinnerMinutesUsed");
        }
        break;

      case 1199:
        {
          // The Far Future
          if (text.contains("item appears in the replicator")
              || text.contains("convoluted nature of time-travel")) {
            Preferences.setBoolean("_timeSpinnerReplicatorUsed", true);
            break;
          }
          Matcher medalMatcher = ChoiceManager.TIME_SPINNER_MEDALS_PATTERN.matcher(text);
          if (medalMatcher.find()) {
            Preferences.setInteger(
                "timeSpinnerMedals", StringUtilities.parseInt(medalMatcher.group(1)));
          }
          break;
        }

      case 1202:
        // Noon in the Civic Center

        // You knock the column over and collect the sprinkles from its shattered remains.
        // A booming voice from behind you startles you. "YOU ARE IN CONTEMPT OF COURT!"
        if (text.contains("CONTEMPT OF COURT!")) {
          Preferences.setBoolean("_gingerbreadColumnDestroyed", true);
          break;
        }

        // You bribe the clerk and he lets you into the office with a sneer. And a key. The key was
        // probably the important part.
        if (text.contains("bribe the clerk")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
          break;
        }

        // He squints at you and pushes a briefcase across the table.
        if (text.contains("briefcase full of sprinkles")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GINGERBREAD_BLACKMAIL_PHOTOS, -1));
          Preferences.setBoolean("gingerBlackmailAccomplished", true);
          break;
        }
        break;

      case 1203:
        // Midnight in Civic Center
        // You step into the library and spend a few hours studying law. It's surprisingly
        // difficult! You gain a new respect for lawyers.
        // Haha no you don't.
        if (text.contains("few hours studying law")) {
          Preferences.increment("gingerLawChoice");
          break;
        }

        // You pay the counterfeiter to make you a fake version of Gingerbread City to pawn off on
        // some rube as the real thing
        if (text.contains("fake version of Gingerbread City")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -300));
          break;
        }

        // You quickly melt the lock on the cell and the criminal inside thanks you as he runs off
        // into the night.
        // "Hey," you shout after him, "you forgot your..." but he's already gone.
        // Oh well. He almost certainly stole this thing, anyway.
        if (text.contains("melt the lock on the cell")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.CREME_BRULEE_TORCH, -1));
          break;
        }

        // You insert your sprinkles and buy your cigarettes.
        if (text.contains("buy your cigarettes")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -5));
          break;
        }

        // You feed the treat to the puppy and he immediately becomes a loyal friend to you. Dogs
        // are so easy!
        if (text.contains("Dogs are so easy")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.GINGERBREAD_DOG_TREAT, -1));
          break;
        }
        break;

      case 1204:
        // Noon at the Train Station
        // You pull the lever and hear a rumbling from underneath you as the sewer gets much larger.
        // Now there's more room for the alligators!
        if (text.contains("more room for the alligators")) {
          Preferences.setBoolean("_gingerBiggerAlligators", true);
          break;
        }

        // You can't make heads or tails of it
        if (text.contains("can't make heads or tails of it")) {
          Preferences.increment("gingerTrainScheduleStudies");
        }
        // You're starting to get a feel for it, but it's still really confusing
        else if (text.contains("starting to get a feel for it")) {
          Preferences.increment("gingerTrainScheduleStudies");
          if (Preferences.getInteger("gingerTrainScheduleStudies") < 4) {
            Preferences.setInteger("gingerTrainScheduleStudies", 4);
          }
        }
        // You're starting to get a handle on how it all works.
        else if (text.contains("starting to get a handle")) {
          Preferences.increment("gingerTrainScheduleStudies");
          if (Preferences.getInteger("gingerTrainScheduleStudies") < 7) {
            Preferences.setInteger("gingerTrainScheduleStudies", 7);
          }
        }
        // You think you've got a pretty good understanding of it at this point.
        else if (text.contains("pretty good understanding")) {
          Preferences.increment("gingerTrainScheduleStudies");
          if (Preferences.getInteger("gingerTrainScheduleStudies") < 10) {
            Preferences.setInteger("gingerTrainScheduleStudies", 10);
          }
        }
        // What next?
        break;

      case 1205:
        // Midnight at the Train Station
        if (text.contains("provide a great workout")) {
          Preferences.increment("gingerMuscleChoice");
        }

        if (text.contains("new line to the subway system")) {
          Preferences.setBoolean("gingerSubwayLineUnlocked", true);
        }

        if (text.contains("what looks like a sweet roll")) {
          Preferences.increment("gingerDigCount");
        } else if (text.contains("piece of rock candy")) {
          Preferences.increment("gingerDigCount");
          if (Preferences.getInteger("gingerDigCount") < 4) {
            Preferences.setInteger("gingerDigCount", 4);
          }
        } else if (text.contains("sugar raygun")) {
          Preferences.setInteger("gingerDigCount", 7);
          ResultProcessor.processResult(ItemPool.get(ItemPool.TEETHPICK, -1));
        }

        break;

      case 1206:
        // Noon in the Industrial Zone
        // You buy the tool.
        if (!text.contains("buy the tool")) {
          break;
        }

        switch (ChoiceManager.lastDecision) {
          case 1:
            // creme brulee torch
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -25));
            break;
          case 2:
            // candy crowbar
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -50));
            break;
          case 3:
            // candy screwdriver
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -100));
            break;
          case 4:
            // teethpick
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
            break;
        }
        break;

      case 1207:
        // Midnight in the Industrial Zone
        // You can't afford a tattoo.
        if (ChoiceManager.lastDecision == 3 && !text.contains("can't afford a tattoo")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -100000));
        }
        break;

      case 1208:
        // Upscale Noon
        if (text.contains("drop off the negatives")) {
          Preferences.setBoolean("gingerNegativesDropped", true);
          ResultProcessor.processResult(ItemPool.get(ItemPool.FRUIT_LEATHER_NEGATIVE, -1));
          break;
        }

        if (!text.contains("You acquire an item")) {
          break;
        }

        switch (ChoiceManager.lastDecision) {
          case 1:
            // gingerbread dog treat
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -200));
            break;
          case 2:
            // pumpkin spice candle
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -150));
            break;
          case 3:
            // gingerbread spice latte
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -50));
            break;
          case 4:
            // gingerbread trousers
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -500));
            break;
          case 5:
            // gingerbread waistcoat
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -500));
            break;
          case 6:
            // gingerbread tophat
            ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -500));
            break;
          case 8:
            // gingerbread blackmail photos
            Preferences.setBoolean("gingerNegativesDropped", false);
            break;
        }
        break;

      case 1210:
        // Civic Planning Office
        // You move the policy to the front of the drawer, and by the time you leave the office
        // they've already enacted it!
        if (!text.contains("they've already enacted it")) {
          break;
        }

        switch (ChoiceManager.lastDecision) {
          case 1:
            Preferences.setBoolean("gingerRetailUnlocked", true);
            break;
          case 2:
            Preferences.setBoolean("gingerSewersUnlocked", true);
            break;
          case 3:
            Preferences.setBoolean("gingerExtraAdventures", true);
            break;
          case 4:
            Preferences.setBoolean("gingerAdvanceClockUnlocked", true);
            break;
        }
        break;

      case 1212:
        // Seedy Seedy Seedy
        if (!text.contains("You acquire an item")) {
          break;
        }

        if (ChoiceManager.lastDecision == 1) {
          // gingerbread pistol
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -300));
        } else if (ChoiceManager.lastDecision == 3) {
          // gingerbread beer
          ResultProcessor.processResult(ItemPool.get(ItemPool.GINGERBREAD_MUG, -1));
        }
        break;

      case 1214:
        // The Gingerbread Gallery
        if (!text.contains("You acquire an item")) {
          break;
        }

        if (ChoiceManager.lastDecision == 1) {
          // high-end ginger wine
        } else if (ChoiceManager.lastDecision == 2) {
          // chocolate sculpture
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -300));
        } else if (ChoiceManager.lastDecision == 3) {
          // Pop Art: a Guide
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
        } else if (ChoiceManager.lastDecision == 4) {
          // No Hats as Art
          ResultProcessor.processResult(ItemPool.get(ItemPool.SPRINKLES, -1000));
        }
        break;

      case 1218:
        // Wax On
        if (text.contains("You acquire an item")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.WAX_GLOB, -1));
        }
        break;

      case 1219:
        // Approach the Jellyfish
        // You acquire an item: sea jelly
        // You think it'd be best to leave them alone for the rest of the day.
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setBoolean("_seaJellyHarvested", true);
        }
        break;

      case 1232: // Finally Human
        ChoiceManager.handleAfterAvatar();
        break;

      case 1264: // Meteor Metal Machinations
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 6) {
          // Exchanging for equipment
          ResultProcessor.removeItem(ItemPool.METAL_METEOROID);
        }
        break;

      case 1267: // Rubbed it the Right Way
        String wish = request.getFormField("wish");
        GenieRequest.postChoice(text, wish);
        break;

      case 1272:
        // R&D
        int letterId = ChoiceManager.extractIidFromURL(urlString);
        String letterName = ItemDatabase.getItemName(letterId);
        if (letterName != null) {
          // Turning in letter
          String message = "handed over " + letterName;
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          ResultProcessor.removeItem(letterId);
        }
        break;

      case 1273:
        // R&D
        if (text.contains("slide the weird key into the weird lock")) {
          ResultProcessor.removeItem(ItemPool.WAREHOUSE_KEY);
        }
        break;

      case 1320:
        // A Heist!
        if (ChoiceManager.lastDecision == 1) {
          Preferences.increment("_catBurglarHeistsComplete");
        }
        break;

      case 1339:
        {
          // A Little Pump and Grind
          if (ChoiceManager.lastDecision == 1 && text.contains("filling counter increments")) {
            int itemId = ChoiceManager.extractIidFromURL(request.getURLString());
            int qty = ChoiceManager.extractQtyFromURL(request.getURLString());
            ResultProcessor.processResult(ItemPool.get(itemId, -qty));
          } else if (ChoiceManager.lastDecision == 2 && text.contains("You acquire an item")) {
            ResultProcessor.removeItem(ItemPool.MAGICAL_SAUSAGE_CASING);
          }
          break;
        }

      case 1344: // Thank You, Come Again
        ChoiceManager.handleAfterAvatar();
        break;

      case 1395: // Take your Pills
        {
          if (!text.contains("day's worth of pills")) {
            // Something failed.  Not enough spleen space left, already have
            // Everything Looks Yellow active, or maybe some other failure condition
            break;
          }
          if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 8) {
            if (!Preferences.getBoolean("_freePillKeeperUsed")) {
              Preferences.setBoolean("_freePillKeeperUsed", true);
            } else {
              KoLCharacter.setSpleenUse(KoLCharacter.getSpleenUse() + 3);
            }
          }
          if (ChoiceManager.lastDecision == 7) {
            TurnCounter.stopCounting("Fortune Cookie");
            TurnCounter.stopCounting("Semirare window begin");
            TurnCounter.stopCounting("Semirare window end");
            TurnCounter.startCounting(0, "Fortune Cookie", "fortune.gif");
            Preferences.setString("semirareLocation", "");
          }
        }
        break;

      case 1409:
        // Your Quest is Over
        ChoiceManager.handleAfterAvatar();
        break;
      case 1449:
        // If you change the mode with the item equipped, you need to un-equip and re-equip it to
        // get the modifiers
        if (ChoiceManager.lastDecision >= 1 && ChoiceManager.lastDecision <= 3) {
          for (int i = EquipmentManager.ACCESSORY1; i <= EquipmentManager.ACCESSORY3; ++i) {
            AdventureResult item = EquipmentManager.getEquipment(i);
            if (item != null && item.getItemId() == ItemPool.BACKUP_CAMERA) {
              RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, i));
              RequestThread.postRequest(new EquipmentRequest(item, i));
            }
          }
        }
        break;
      case 1452: // Sprinkler Joe
      case 1453: // Fracker Dan
      case 1454: // Cropduster Dusty
        WildfireCampRequest.refresh();
        break;
    }

    if (ChoiceManager.handlingChoice) {
      ChoiceManager.visitChoice(request);
      return;
    }

    SpadingManager.processChoice(urlString, text);

    if (text.contains("charpane.php")) {
      // Since a charpane refresh was requested, a turn might have been spent
      AdventureSpentDatabase.setNoncombatEncountered(true);
    }

    PostChoiceAction action = ChoiceManager.action;
    if (action != PostChoiceAction.NONE) {
      ChoiceManager.action = PostChoiceAction.NONE;
      switch (action) {
        case INITIALIZE:
          LoginManager.login(KoLCharacter.getUserName());
          break;
        case ASCEND:
          ValhallaManager.postAscension();
          break;
      }
    }

    // visitChoice() gets the decorated response text, but this is not a visit.
    // If this is not actually a choice page, we were redirected.
    // Do not save this responseText
    if (urlString.startsWith("choice.php")) {
      ChoiceManager.lastDecoratedResponseText =
          RequestEditorKit.getFeatureRichHTML(request.getURLString(), text);
    }
  }

  public static void handleWalkingAway(final String urlString) {
    // If we are not handling a choice, nothing to do
    if (!ChoiceManager.handlingChoice) {
      return;
    }

    // If the choice doesn't let you walk away, normal redirect
    // processing will take care of it
    if (!ChoiceManager.canWalkAway) {
      return;
    }

    // If you walked away from the choice, we're done with the choice
    if (!urlString.startsWith("choice.php")) {
      ChoiceManager.handlingChoice = false;
      return;
    }
  }

  private static void handleAfterAvatar() {
    String newClass = "Unknown";
    switch (ChoiceManager.lastDecision) {
      case 1:
        newClass = "Seal Clubber";
        break;
      case 2:
        newClass = "Turtle Tamer";
        break;
      case 3:
        newClass = "Pastamancer";
        break;
      case 4:
        newClass = "Sauceror";
        break;
      case 5:
        newClass = "Disco Bandit";
        break;
      case 6:
        newClass = "Accordion Thief";
        break;
    }

    String message = "Now walking on the " + newClass + " road.";

    KoLmafia.updateDisplay(message);
    RequestLogger.updateSessionLog(message);

    KoLmafia.resetAfterAvatar();
  }

  // Looks like your order for a <muffin type> muffin is not yet ready.
  public static final Pattern MUFFIN_TYPE_PATTERN =
      Pattern.compile("Looks like your order for a (.*? muffin) is not yet ready");

  public static void visitChoice(final GenericRequest request) {
    String text = request.responseText;
    ChoiceManager.lastChoice = ChoiceManager.extractChoice(text);

    if (ChoiceManager.lastChoice == 0) {
      // choice.php did not offer us any choices and we couldn't work out which choice it was.
      // This happens if taking a choice gives a response with a "next" link to choice.php.
      ChoiceManager.lastDecoratedResponseText =
          RequestEditorKit.getFeatureRichHTML(request.getURLString(), text);
      return;
    }

    SpadingManager.processChoiceVisit(ChoiceManager.lastChoice, text);

    // Must do this BEFORE we decorate the response text
    ChoiceManager.setCanWalkAway(ChoiceManager.lastChoice);

    ChoiceManager.lastResponseText = text;

    // Clear lastItemUsed, to prevent the item being "prcessed"
    // next time we simply visit the inventory.
    UseItemRequest.clearLastItemUsed();

    switch (ChoiceManager.lastChoice) {
      case 360:
        // Wumpus Hunt
        WumpusManager.visitChoice(text);
        break;

      case 440:
        // Puttin' on the Wax
        HaciendaManager.preRecording(text);
        break;

      case 460:
        // Space Trip
        ArcadeRequest.visitSpaceTripChoice(text);
        break;

      case 471:
        // DemonStar
        ArcadeRequest.visitDemonStarChoice(text);
        break;

      case 485:
        // Fighters Of Fighting
        ArcadeRequest.visitFightersOfFightingChoice(text);
        break;

      case 486:
        // DungeonFist!
        ArcadeRequest.visitDungeonFistChoice(text);
        break;

      case 488:
        // Meteoid
        ArcadeRequest.visitMeteoidChoice(text);
        break;

      case 496:
        // Crate Expectations
      case 509:
        // Of Course!
      case 510:
        // Those Who Came Before You
      case 511:
        // If it's Tiny, is it Still a Mansion?
      case 512:
        // Hot and Cold Running Rats
      case 513:
        // Staring Down the Barrel
      case 514:
        // 1984 Had Nothing on This Cellar
      case 515:
        // A Rat's Home...
      case 1000:
        // Everything in Moderation
      case 1001:
        // Hot and Cold Dripping Rats
        TavernRequest.postTavernVisit(request);
        break;

      case 537:
        // Play Porko!
        SpaaaceRequest.visitPorkoChoice(text);
        break;

      case 540:
        // Big-Time Generator
        SpaaaceRequest.visitGeneratorChoice(text);
        break;

      case 570:
        GameproManager.parseGameproMagazine(text);
        break;

      case 641:
        // Stupid Pipes.
        if (!text.contains("Dive Down")
            && KoLCharacter.getElementalResistanceLevels(Element.HOT) >= 25) {
          Preferences.setBoolean("flickeringPixel1", true);
        }
        break;

      case 642:
        // You're Freaking Kidding Me
        if (!text.contains("Wait a minute...")
            && KoLCharacter.getAdjustedMuscle() >= 500
            && KoLCharacter.getAdjustedMysticality() >= 500
            && KoLCharacter.getAdjustedMoxie() >= 500) {
          Preferences.setBoolean("flickeringPixel2", true);
        }
        break;

      case 644:
        // Snakes.
        if (!text.contains("Tie the snakes in a knot.") && KoLCharacter.getAdjustedMoxie() >= 300) {
          Preferences.setBoolean("flickeringPixel3", true);
        }
        break;

      case 645:
        // So... Many... Skulls...
        if (!text.contains("You fear no evil")
            && KoLCharacter.getElementalResistanceLevels(Element.SPOOKY) >= 25) {
          Preferences.setBoolean("flickeringPixel4", true);
        }
        break;

      case 647:
        // A Stupid Dummy. Also, a Straw Man.

        // *** unspaded
        if (!text.contains("Graaaaaaaaargh!") && KoLCharacter.currentBonusDamage() >= 1000) {
          Preferences.setBoolean("flickeringPixel5", true);
        }
        break;

      case 648:
        // Slings and Arrows
        // *** Yes, there supposed to be two spaces there.
        if (!text.contains("Arrows?  Ha.") && KoLCharacter.getCurrentHP() >= 1000) {
          Preferences.setBoolean("flickeringPixel6", true);
        }
        break;

      case 650:
        // This Is Your Life. Your Horrible, Horrible Life.
        if (!text.contains("Then watch it again with the commentary on!")
            && KoLCharacter.getCurrentMP() >= 1000) {
          Preferences.setBoolean("flickeringPixel7", true);
        }
        break;

      case 651:
        // The Wall of Wailing
        if (!text.contains("Make the tide resist you")
            && KoLCharacter.currentPrismaticDamage() >= 60) {
          Preferences.setBoolean("flickeringPixel8", true);
        }

        break;

      case 658:
        // Debasement
        ResultProcessor.processItem(ItemPool.GOLD_PIECE, -30);
        break;

      case 689:
        // The Final Reward
        Preferences.setInteger("_lastDailyDungeonRoom", 14);
        break;

      case 690:
        // The First Chest Isn't the Deepest
        Preferences.setInteger("_lastDailyDungeonRoom", 4);
        break;

      case 691:
        // Second Chest
        Preferences.setInteger("_lastDailyDungeonRoom", 9);
        break;

      case 692:
      case 693:
        // I Wanna Be a Door and It's Almost Certainly a Trap
        Matcher chamberMatcher = ChoiceManager.CHAMBER_PATTERN.matcher(text);
        if (chamberMatcher.find()) {
          int round = StringUtilities.parseInt(chamberMatcher.group(1));
          Preferences.setInteger("_lastDailyDungeonRoom", round - 1);
        }
        break;

      case 705:
        // Halls Passing in the Night
        ResultProcessor.processItem(ItemPool.MERKIN_HALLPASS, -1);
        break;

      case 764:
        // The Machine

        // You approach The Machine, and notice that the
        // capacitor you're carrying fits perfectly into an
        // obviously empty socket on the base of it. You plug
        // it in, and The Machine whirs ominously to life.......

        if (text.contains("You plug it in")) {
          ResultProcessor.processResult(ItemPool.get(ItemPool.SKULL_CAPACITOR, -1));
        }
        break;

      case 774:
        // Opening up the Folder Holder

        String option = request.getFormField("forceoption");
        if (option != null) {
          ChoiceManager.lastDecision = StringUtilities.parseInt(option);
        }
        break;

      case 781:
        // Earthbound and Down
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenApartmentProgress") == 0) {
            Preferences.setInteger("hiddenApartmentProgress", 1);
          }
        }
        break;

      case 783:
        // Water You Dune
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenHospitalProgress") == 0) {
            Preferences.setInteger("hiddenHospitalProgress", 1);
          }
        }
        break;

      case 785:
        // Air Apparent
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenOfficeProgress") == 0) {
            Preferences.setInteger("hiddenOfficeProgress", 1);
          }
        }
        break;

      case 787:
        // Fire When Ready
        if (!text.contains("option value=1")) {
          if (Preferences.getInteger("hiddenBowlingAlleyProgress") == 0) {
            Preferences.setInteger("hiddenBowlingAlleyProgress", 1);
          }
        }
        break;

      case 798:
        // Hippy Talkin'
        if (text.contains("You should totally keep it!")) {
          QuestDatabase.setQuestProgress(Quest.HIPPY, QuestDatabase.FINISHED);
          Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
        }
        break;

      case 801:
        {
          // A Reanimated Conversation
          Matcher matcher = ChoiceManager.REANIMATOR_ARM_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorArms", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorArms", 0);
          }
          matcher = ChoiceManager.REANIMATOR_LEG_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorLegs", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorLegs", 0);
          }
          matcher = ChoiceManager.REANIMATOR_SKULL_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorSkulls", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorSkulls", 0);
          }
          matcher = ChoiceManager.REANIMATOR_WEIRDPART_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "reanimatorWeirdParts", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorWeirdParts", 0);
          }
          matcher = ChoiceManager.REANIMATOR_WING_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger("reanimatorWings", StringUtilities.parseInt(matcher.group(1)));
          } else {
            Preferences.setInteger("reanimatorWings", 0);
          }
          break;
        }

      case 836:
        {
          // Adventures Who Live in Ice Houses...
          Matcher matcher = ChoiceManager.ICEHOUSE_PATTERN.matcher(text);
          if (matcher.find()) {
            String icehouseMonster = matcher.group(1).toLowerCase();
            String knownBanishes = Preferences.getString("banishedMonsters");
            if (!knownBanishes.contains(icehouseMonster)) {
              // If not already known to be banished, add it
              BanishManager.banishMonster(icehouseMonster, "ice house");
            }
          }
          break;
        }

      case 822:
      case 823:
      case 824:
      case 825:
      case 826:
      case 827:
        // The Prince's Ball
        ChoiceManager.parseCinderellaTime();
        Preferences.setString("grimstoneMaskPath", "stepmother");
        break;

      case 848:
      case 849:
      case 850:
        {
          // Where the Magic Happens & The Practice & World of Bartercraft
          Preferences.setString("grimstoneMaskPath", "gnome");
          // Update remaining materials
          Matcher matcher = ChoiceManager.RUMPLE_MATERIAL_PATTERN.matcher(text);
          while (matcher.find()) {
            String material = matcher.group(1);
            int number = StringUtilities.parseInt(matcher.group(2));
            if (material.equals("straw")) {
              int straw = InventoryManager.getCount(ItemPool.STRAW);
              if (straw != number) {
                ResultProcessor.processItem(ItemPool.STRAW, number - straw);
              }
            } else if (material.equals("leather")) {
              int leather = InventoryManager.getCount(ItemPool.LEATHER);
              if (leather != number) {
                ResultProcessor.processItem(ItemPool.LEATHER, number - leather);
              }
            } else if (material.equals("clay")) {
              int clay = InventoryManager.getCount(ItemPool.CLAY);
              if (clay != number) {
                ResultProcessor.processItem(ItemPool.CLAY, number - clay);
              }
            } else if (material.equals("filling")) {
              int filling = InventoryManager.getCount(ItemPool.FILLING);
              if (filling != number) {
                ResultProcessor.processItem(ItemPool.FILLING, number - filling);
              }
            } else if (material.equals("parchment")) {
              int parchment = InventoryManager.getCount(ItemPool.PARCHMENT);
              if (parchment != number) {
                ResultProcessor.processItem(ItemPool.PARCHMENT, number - parchment);
              }
            } else if (material.equals("glass")) {
              int glass = InventoryManager.getCount(ItemPool.GLASS);
              if (glass != number) {
                ResultProcessor.processItem(ItemPool.GLASS, number - glass);
              }
            }
          }
          break;
        }

      case 851:
        {
          // Shen Copperhead, Nightclub Owner
          Matcher matcher = ChoiceManager.SHEN_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("shenQuestItem", matcher.group(1));
          }
          break;
        }

      case 871:
        {
          // inspecting Motorbike
          Matcher matcher = ChoiceManager.MOTORBIKE_TIRES_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeTires", matcher.group(1).trim());
          }
          matcher = ChoiceManager.MOTORBIKE_GASTANK_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeGasTank", matcher.group(1).trim());
            if (Preferences.getString("peteMotorbikeGasTank").equals("Large Capacity Tank")) {
              KoLCharacter.setDesertBeachAvailable();
            } else if (Preferences.getString("peteMotorbikeGasTank").equals("Extra-Buoyant Tank")) {
              Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
            }
          }
          matcher = ChoiceManager.MOTORBIKE_HEADLIGHT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeHeadlight", matcher.group(1).trim());
          }
          matcher = ChoiceManager.MOTORBIKE_COWLING_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeCowling", matcher.group(1).trim());
          }
          matcher = ChoiceManager.MOTORBIKE_MUFFLER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeMuffler", matcher.group(1).trim());
          }
          matcher = ChoiceManager.MOTORBIKE_SEAT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("peteMotorbikeSeat", matcher.group(1).trim());
          }
          break;
        }

      case 890: // Lights Out in the Storage Room
      case 891: // Lights Out in the Laundry Room
      case 892: // Lights Out in the Bathroom
      case 893: // Lights Out in the Kitchen
      case 894: // Lights Out in the Library
      case 895: // Lights Out in the Ballroom
      case 896: // Lights Out in the Gallery
      case 897: // Lights Out in the Bedroom
      case 898: // Lights Out in the Nursery
      case 899: // Lights Out in the Conservatory
      case 900: // Lights Out in the Billiards Room
      case 901: // Lights Out in the Wine Cellar
      case 902: // Lights Out in the Boiler Room
      case 903: // Lights Out in the Laboratory
        // Remove the counter if it exists so a new one can be made
        // as soon as the next adventure is started
        TurnCounter.stopCounting("Spookyraven Lights Out");
        Preferences.setInteger("lastLightsOutTurn", KoLCharacter.getTurnsPlayed());
        break;

      case 984:
        if (text.contains("Awaiting mission")) {
          break;
        }

        Matcher staticMatcher = ChoiceManager.RADIO_STATIC_PATTERN.matcher(text);

        String snippet = ".*";

        while (staticMatcher.find()) {
          String section = staticMatcher.group(1);

          if (section.contains("You turn the biggest knob on the radio")) {
            continue;
          }

          for (String part : section.split("&lt;.*?&gt;")) {
            if (part.startsWith("&lt;") || part.length() < 3) {
              continue;
            }

            part = part.replaceAll("\\.* *$", "");
            part = part.replace("  ", " ");
            snippet += Pattern.quote(part) + ".*";
          }
        }

        Iterator<Entry<Quest, String>> iterator = conspiracyQuestMessages.entrySet().iterator();

        Quest todaysQuest = null;

        while (iterator.hasNext()) {
          Map.Entry<Quest, String> entry = iterator.next();

          if (Pattern.matches(snippet, entry.getValue())) {
            if (todaysQuest == null) {
              todaysQuest = entry.getKey();
            } else {
              // Multiple matches
              todaysQuest = null;
              break;
            }
          }
        }

        if (todaysQuest != null) {
          Preferences.setString("_questESp", todaysQuest.getPref());
        }

        break;

      case 986:
        // Control Panel
        Preferences.setBoolean(
            "controlPanel1", !text.contains("All-Ranchero FM station: VOLUNTARY"));
        Preferences.setBoolean(
            "controlPanel2", !text.contains("&pi; sleep-hypnosis generators: OFF"));
        Preferences.setBoolean(
            "controlPanel3", !text.contains("Simian Ludovico Wednesdays: CANCELLED"));
        Preferences.setBoolean(
            "controlPanel4", !text.contains("Monkey food safety protocols: OBEYED"));
        Preferences.setBoolean("controlPanel5", !text.contains("Shampoo Dispensers: CHILD-SAFE"));
        Preferences.setBoolean("controlPanel6", !text.contains("Assemble-a-Bear kiosks: CLOSED"));
        Preferences.setBoolean("controlPanel7", !text.contains("Training algorithm: ROUND ROBIN"));
        Preferences.setBoolean(
            "controlPanel8", !text.contains("Re-enactment supply closet: LOCKED"));
        Preferences.setBoolean("controlPanel9", !text.contains("Thermostat setting: 76 DEGREES"));
        Matcher omegaMatcher = ChoiceManager.OMEGA_PATTERN.matcher(text);
        if (omegaMatcher.find()) {
          Preferences.setInteger(
              "controlPanelOmega", StringUtilities.parseInt(omegaMatcher.group(1)));
        }
        if (text.contains("Omega device activated")) {
          Preferences.setInteger("controlPanelOmega", 0);
          QuestDatabase.setQuestProgress(Quest.EVE, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.FAKE_MEDIUM, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.SERUM, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.SMOKES, QuestDatabase.UNSTARTED);
          QuestDatabase.setQuestProgress(Quest.OUT_OF_ORDER, QuestDatabase.UNSTARTED);
        }
        break;

      case 1003:
        // Test Your Might And Also Test Other Things
        SorceressLairManager.parseContestBooth(0, text);
        break;

      case 1005: // 'Allo
      case 1006: // One Small Step For Adventurer
      case 1007: // Twisty Little Passages, All Hedge
      case 1008: // Pooling Your Resources
      case 1009: // Good Ol' 44% Duck
      case 1010: // Another Day, Another Fork
      case 1011: // Of Mouseholes and Manholes
      case 1012: // The Last Temptation
      case 1013: // Mazel Tov!
        SorceressLairManager.visitChoice(ChoiceManager.lastChoice, text);
        break;

      case 1023:
      case 1024:
        {
          // Like a Bat into Hell
          // Like a Bat out of Hell
          Matcher matcher = ChoiceManager.ED_RETURN_PATTERN.matcher(text);
          if (matcher.find()) {
            int cost = StringUtilities.parseInt(matcher.group(1));
            int defeats = 3 + (int) (Math.log(cost) / Math.log(2));
            Preferences.setInteger("_edDefeats", defeats);
          }
          break;
        }

      case 1025:
        {
          // Reconfigure your Mini-Crimbot
          Matcher matcher = ChoiceManager.CRIMBOT_CHASSIS_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("crimbotChassis", matcher.group(1));
          }
          matcher = ChoiceManager.CRIMBOT_ARM_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("crimbotArm", matcher.group(1));
          }
          matcher = ChoiceManager.CRIMBOT_PROPULSION_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("crimbotPropulsion", matcher.group(1));
          }
          break;
        }

      case 1049: // Tomb of the Unknown Your Class Here
        if (text.contains("The Epic Weapon's yours")) {
          QuestDatabase.setQuestProgress(Quest.NEMESIS, "step3");
        }
        break;

      case 1051: // The Book of the Undying
        EdBaseRequest.inspectBook(text);
        break;

      case 1053: // The Servants' Quarters
        EdBaseRequest.inspectServants(text);
        EdServantData.inspectServants(text);
        break;

      case 1063:
        {
          // Adjust your 'Edpiece
          Matcher matcher = ChoiceManager.EDPIECE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("edPiece", matcher.group(1).trim());
          }
          break;
        }

      case 1068:
        {
          // Barf Mountain Breakdown
          Matcher matcher = ChoiceManager.DINSEY_ROLLERCOASTER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("dinseyRollercoasterStats", matcher.group(1).trim());
          }
          Preferences.setBoolean(
              "dinseyRapidPassEnabled", text.contains("Disable Rapid-Pass System"));
          break;
        }

      case 1069:
        {
          // The Pirate Bay
          Matcher matcher = ChoiceManager.DINSEY_PIRATE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setBoolean("dinseyGarbagePirate", (matcher.group(1).equals("lit")));
          }
          break;
        }

      case 1070:
        {
          // In Your Cups
          Matcher matcher = ChoiceManager.DINSEY_TEACUP_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("dinseyToxicMultiplier", matcher.group(1).trim());
          }
          Preferences.setBoolean(
              "dinseySafetyProtocolsLoose", text.contains("protocols seem pretty loose"));
          break;
        }

      case 1071:
        {
          // Gator Gamer
          Matcher matcher = ChoiceManager.DINSEY_SLUICE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("dinseyGatorStenchDamage", matcher.group(1).trim());
          }
          Preferences.setBoolean("dinseyAudienceEngagement", text.contains("High Engagement Mode"));
          break;
        }

      case 1075:
        {
          // Mmmmmmayonnaise
          TurnCounter.stopCounting("Mmmmmmayonnaise window begin");
          TurnCounter.stopCounting("Mmmmmmayonnaise window end");
          break;
        }

      case 1076:
        {
          // Mayo Minder&trade;
          Matcher matcher = ChoiceManager.MAYO_MINDER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("mayoMinderSetting", matcher.group(1).trim());
          } else {
            Preferences.setString("mayoMinderSetting", "");
          }
          break;
        }

      case 1087:
        // The Dark and Dank and Sinister Cave Entrance
        QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step11");
        break;

      case 1088:
        // Rubble, Rubble, Toil and Trouble
        QuestDatabase.setQuestIfBetter(Quest.NEMESIS, "step13");
        break;

      case 1093:
        {
          // The WLF Bunker

          // There is no choice if this happens, but we recognise title in visitChoice()
          // You enter the bunker, but the speaker is silent. You've already done your day's work,
          // soldier!
          if (text.contains("the speaker is silent")) {
            Preferences.setBoolean("_volcanoItemRedeemed", true);
            Preferences.setInteger("_volcanoItem1", 0);
            Preferences.setInteger("_volcanoItem2", 0);
            Preferences.setInteger("_volcanoItem3", 0);
            Preferences.setInteger("_volcanoItemCount1", 0);
            Preferences.setInteger("_volcanoItemCount2", 0);
            Preferences.setInteger("_volcanoItemCount3", 0);
            break;
          }

          // On the other hand, if there IS a choice on the page,
          // it will be whichchoice=1093 asking to redeem items

          Preferences.setBoolean("_volcanoItemRedeemed", false);

          Matcher matcher = ChoiceManager.WLF_PATTERN.matcher(text);
          while (matcher.find()) {
            // String challenge = matcher.group( 1 );
            String descid = matcher.group(2);
            int itemId = ItemDatabase.getItemIdFromDescription(descid);
            if (itemId != -1) {
              String itemName = matcher.group(3).trim();
              Matcher countMatcher = ChoiceManager.WLF_COUNT_PATTERN.matcher(itemName);
              int count = countMatcher.find() ? StringUtilities.parseInt(countMatcher.group(1)) : 1;
              String index = matcher.group(4);
              Preferences.setInteger("_volcanoItem" + index, itemId);
              Preferences.setInteger("_volcanoItemCount" + index, count);
            }
          }

          break;
        }

      case 1100:
        // Pray to the Barrel God
        if (text.contains("You already prayed to the Barrel god today")) {
          Preferences.setBoolean("_barrelPrayer", true);
          break;
        }
        if (!text.contains("barrel lid shield")) {
          Preferences.setBoolean("prayedForProtection", true);
        }
        if (!text.contains("barrel hoop earring")) {
          Preferences.setBoolean("prayedForGlamour", true);
        }
        if (!text.contains("bankruptcy barrel")) {
          Preferences.setBoolean("prayedForVigor", true);
        }
        break;

      case 1110:
        // Spoopy
        Preferences.setBoolean("doghouseBoarded", !text.contains("Board up the doghouse"));
        break;

      case 1118:
        {
          // X-32-F Combat Training Snowman Control Console
          Matcher matcher = ChoiceManager.SNOJO_CONSOLE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("snojoSetting", matcher.group(1).trim());
          } else {
            Preferences.setString("snojoSetting", "");
          }
          break;
        }

      case 1181:
        // Witchess Set
        if (!text.contains("Examine the shrink ray")) {
          Preferences.setInteger("_witchessFights", 5);
        }
        break;

      case 1188:
        {
          // The Call is Coming from Outside the Simulation
          Matcher matcher = ChoiceManager.ENLIGHTENMENT_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "sourceEnlightenment", StringUtilities.parseInt(matcher.group(1)));
          }
          break;
        }

      case 1191:
        // Source Terminal
        request.setHasResult(false);
        break;

      case 1193:
        {
          // The Precinct
          Matcher matcher = ChoiceManager.CASE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_detectiveCasesCompleted", 3 - StringUtilities.parseInt(matcher.group(1)));
          }
          break;
        }

      case 1195:
        {
          // Spinning Your Time-Spinner
          Matcher matcher = ChoiceManager.TIME_SPINNER_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_timeSpinnerMinutesUsed", 10 - StringUtilities.parseInt(matcher.group(1)));
          }
          break;
        }

      case 1202: // Noon in the Civic Center
      case 1203: // Midnight in Civic Center
      case 1204: // Noon at the Train Station
      case 1205: // Midnight at the Train Station
      case 1206: // Noon in the Industrial Zone
      case 1207: // Midnight in the Industrial Zone
      case 1208: // Upscale Noon
      case 1209: // Upscale Midnight
        {
          Preferences.increment("_gingerbreadCityTurns");
          break;
        }

      case 1229:
        {
          // L.O.V. Exit

          // As you are about to leave the station, you notice a
          // data entry pad and a sign above it that says <WORD>.
          // Huh, that's odd.

          Matcher matcher = ChoiceManager.LOV_EXIT_PATTERN.matcher(text);
          if (matcher.find()) {
            String message = "L.O.V. Exit word: " + matcher.group(1);
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }

          break;
        }

      case 1234:
        {
          // Spacegate Vaccinator

          Matcher matcher = ChoiceManager.VACCINE_PATTERN.matcher(text);

          while (matcher.find()) {
            String setting = "spacegateVaccine" + matcher.group(1);
            String button = matcher.group(2);
            if (button.startsWith("Select Vaccine")) {
              Preferences.setBoolean(setting, true);
            } else if (button.startsWith("Unlock Vaccine")) {
              Preferences.setBoolean(setting, false);
            }
          }
          break;
        }

      case 1259: // LI-11 HQ
        if (text.contains("LI-11 HQ")) {
          Pattern capitalPattern = Pattern.compile("<td><b>(.*?)</b>(.*?)</td></tr>");
          boolean bondAdv = false,
              bondWpn = false,
              bondInit = false,
              bondDR = false,
              bondHP = false,
              bondItem2 = false,
              bondStat = false,
              bondDrunk1 = false,
              bondBooze = false,
              bondSymbols = false,
              bondDrunk2 = false,
              bondJetpack = false,
              bondMartiniTurn = false,
              bondMeat = false,
              bondItem1 = false,
              bondMus1 = false,
              bondMys1 = false,
              bondMox1 = false,
              bondBeach = false,
              bondBeat = false,
              bondMartiniDelivery = false,
              bondMus2 = false,
              bondMys2 = false,
              bondMox2 = false,
              bondStealth = false,
              bondMartiniPlus = false,
              bondBridge = false,
              bondWar = false,
              bondMPregen = false,
              bondWeapon2 = false,
              bondItem3 = false,
              bondStealth2 = false,
              bondSpleen = false,
              bondStat2 = false,
              bondDesert = false,
              bondHoney = false;
          Matcher matcher = capitalPattern.matcher(text);
          while (matcher.find()) {
            if (matcher.group(2).contains("Active") || matcher.group(2).contains("Connected")) {
              if (matcher.group(1).equals("Super-Accurate Spy Watch")) {
                bondAdv = true;
              } else if (matcher.group(1).equals("Razor-Sharp Tie")) {
                bondWpn = true;
              } else if (matcher.group(1).equals("Jet-Powered Skis")) {
                bondInit = true;
              } else if (matcher.group(1).equals("Kevlar-Lined Pants")) {
                bondDR = true;
              } else if (matcher.group(1).equals("Injected Nanobots")) {
                bondHP = true;
              } else if (matcher.group(1).equals("Sticky Climbing Gloves")) {
                bondItem2 = true;
              } else if (matcher.group(1).equals("Retinal Knowledge HUD")) {
                bondStat = true;
              } else if (matcher.group(1).equals("Belt-Implanted Still")) {
                bondDrunk1 = true;
              } else if (matcher.group(1).equals("Alcohol Absorbent Underwear")) {
                bondBooze = true;
              } else if (matcher.group(1).equals("Universal Symbology Guide")) {
                bondSymbols = true;
              } else if (matcher.group(1).equals("Soberness Injection Pen")) {
                bondDrunk2 = true;
              } else if (matcher.group(1).equals("Short-Range Jetpack")) {
                bondJetpack = true;
              } else if (matcher.group(1).equals("Invisible Meat Car, the Vanish")) {
                bondStealth = true;
              } else if (matcher.group(1).equals("Portable Pocket Bridge")) {
                bondBridge = true;
              } else if (matcher.group(1).equals("Static-Inducing, Bug-Shorting Underpants")) {
                bondMPregen = true;
              } else if (matcher.group(1).equals("Exotic Bartender, Barry L. Eagle")) {
                bondMartiniTurn = true;
              } else if (matcher.group(1).equals("Renowned Meat Thief, Ivanna Cuddle")) {
                bondMeat = true;
              } else if (matcher.group(1).equals("Master Art Thief, Sly Richard")) {
                bondItem1 = true;
              } else if (matcher.group(1).equals("Personal Trainer, Debbie Dallas")) {
                bondMus1 = true;
              } else if (matcher.group(1).equals("Rocket Scientist, Crimbo Jones")) {
                bondMys1 = true;
              } else if (matcher.group(1).equals("Licensed Masseur, Oliver Closehoff")) {
                bondMox1 = true;
              } else if (matcher.group(1).equals("Professional Cabbie, Rock Hardy")) {
                bondBeach = true;
              } else if (matcher.group(1).equals("Fellow Spy, Daisy Duke")) {
                bondBeat = true;
              } else if (matcher.group(1).equals("Fellow Spy, Prince O'Toole")) {
                bondMartiniDelivery = true;
              } else if (matcher.group(1).equals("Personal Kinesiologist, Doctor Kittie")) {
                bondMus2 = true;
              } else if (matcher.group(1).equals("Computer Hacker, Mitt Jobs")) {
                bondMys2 = true;
              } else if (matcher.group(1).equals("Spa Owner, Fatima Jiggles")) {
                bondMox2 = true;
              } else if (matcher.group(1).equals("Exotic Olive Procurer, Ben Dover")) {
                bondMartiniPlus = true;
              } else if (matcher.group(1).equals("Trained Sniper, Felicity Snuggles")) {
                bondWar = true;
              } else if (matcher.group(1).equals("Martial Arts Trainer, Jaques Trappe")) {
                bondWeapon2 = true;
              } else if (matcher.group(1).equals("Electromagnetic Ring")) {
                bondItem3 = true;
              } else if (matcher.group(1).equals("Robo-Spleen")) {
                bondSpleen = true;
              } else if (matcher.group(1).equals("Universal GPS")) {
                bondDesert = true;
              } else if (matcher.group(1).equals("Mission Controller, Maeby Moneypenny")) {
                bondStealth2 = true;
              } else if (matcher.group(1).equals("Sage Advisor, London McBrittishman")) {
                bondStat2 = true;
              } else if (matcher.group(1).equals("True Love, Honey Potts")) {
                bondHoney = true;
              }
            }
          }
          Preferences.setBoolean("bondAdv", bondAdv);
          Preferences.setBoolean("bondWpn", bondWpn);
          Preferences.setBoolean("bondInit", bondInit);
          Preferences.setBoolean("bondDA", bondDR);
          Preferences.setBoolean("bondHP", bondHP);
          Preferences.setBoolean("bondItem2", bondItem2);
          Preferences.setBoolean("bondStat", bondStat);
          Preferences.setBoolean("bondDrunk1", bondDrunk1);
          Preferences.setBoolean("bondBooze", bondBooze);
          Preferences.setBoolean("bondSymbols", bondSymbols);
          Preferences.setBoolean("bondDrunk2", bondDrunk2);
          Preferences.setBoolean("bondJetpack", bondJetpack);
          Preferences.setBoolean("bondStealth", bondStealth);
          Preferences.setBoolean("bondMartiniTurn", bondMartiniTurn);
          Preferences.setBoolean("bondMeat", bondMeat);
          Preferences.setBoolean("bondItem1", bondItem1);
          Preferences.setBoolean("bondMus1", bondMus1);
          Preferences.setBoolean("bondMys1", bondMys1);
          Preferences.setBoolean("bondMox1", bondMox1);
          Preferences.setBoolean("bondBeach", bondBeach);
          Preferences.setBoolean("bondBeat", bondBeat);
          Preferences.setBoolean("bondMartiniDelivery", bondMartiniDelivery);
          Preferences.setBoolean("bondMus2", bondMus2);
          Preferences.setBoolean("bondMys2", bondMys2);
          Preferences.setBoolean("bondMox2", bondMox2);
          Preferences.setBoolean("bondMartiniPlus", bondMartiniPlus);
          Preferences.setBoolean("bondBridge", bondBridge); // need to handle when this is selected
          Preferences.setBoolean("bondWar", bondWar);
          Preferences.setBoolean("bondMPregen", bondMPregen);
          Preferences.setBoolean("bondWeapon2", bondWeapon2);
          Preferences.setBoolean("bondItem3", bondItem3);
          Preferences.setBoolean("bondStealth2", bondStealth2);
          Preferences.setBoolean("bondSpleen", bondSpleen);
          Preferences.setBoolean("bondStat2", bondStat2);
          Preferences.setBoolean("bondDesert", bondDesert); // Do something with this
          Preferences.setBoolean("bondHoney", bondHoney);
          if (bondBeach) {
            KoLCharacter.setDesertBeachAvailable();
          }
          KoLCharacter.recalculateAdjustments();
          KoLCharacter.updateStatus();
        }
        break;

      case 1266:
        {
          // The Hostler
          Preferences.setBoolean("horseryAvailable", true);

          // <td valign=top class=small><b>Drab Teddy</b> the Normal Horse<P>
          // <td valign=top class=small><b>Surreptitious Mantilla</b> the Dark Horse<P>
          // <td valign=top class=small><b>Wacky Biggles</b> the Crazy Horse<P>
          // <td valign=top class=small><b>Frightful Twiggy</b> the Pale Horse<P>

          // Save the horse names so we can recognize them in combat
          Pattern pattern =
              Pattern.compile("<td valign=top class=small><b>([^<]+)</b> the ([^ ]+) Horse<P>");
          Matcher matcher = pattern.matcher(text);
          while (matcher.find()) {
            String name = matcher.group(1);
            String type = matcher.group(2);
            String setting =
                type.equals("Crazy")
                    ? "_horseryCrazyName"
                    : type.equals("Dark")
                        ? "_horseryDarkName"
                        : type.equals("Normal")
                            ? "_horseryNormalName"
                            : type.equals("Pale") ? "_horseryPaleName" : null;
            if (setting != null) {
              Preferences.setString(setting, name);
            }
          }

          pattern =
              Pattern.compile(
                  "Gives you\\s+([+-]\\d+)% Muscle, ([+-]\\d+)% Mysticality, and ([+-]\\d+)%");
          matcher = pattern.matcher(text);
          if (matcher.find()) {
            Preferences.setString("_horseryCrazyMus", matcher.group(1));
            Preferences.setString("_horseryCrazyMys", matcher.group(2));
            Preferences.setString("_horseryCrazyMox", matcher.group(3));
          }

          if (!text.contains("name=option value=1")) {
            Preferences.setString("_horsery", "normal horse");
          } else if (!text.contains("name=option value=2")) {
            Preferences.setString("_horsery", "dark horse");
          } else if (!text.contains("name=option value=3")) {
            Preferences.setString("_horsery", "crazy horse");
          } else if (!text.contains("name=option value=4")) {
            Preferences.setString("_horsery", "pale horse");
          } else {
            Preferences.setString("_horsery", "");
          }
          break;
        }

      case 1267:
        // Rubbed it the Right Way
        GenieRequest.visitChoice(text);
        break;

      case 1308:
        {
          // place.php?whichplace=monorail&action=monorail_downtown
          //
          // On a Downtown Train
          //
          // Everything you do at this location uses this choice
          // adventure #. As you select options, you stay in the
          // same choice, but the available options change.
          //
          // We must deduce what is happening by looking at the
          // response text

          // Looks like your order for a <muffin type> muffin is not yet ready.
          Matcher muffinMatcher = MUFFIN_TYPE_PATTERN.matcher(text);
          if (muffinMatcher.find()) {
            Preferences.setString("muffinOnOrder", muffinMatcher.group(1));
          }
          // "Sorry, you placed your order a lifetime ago, so we had to throw out the actual baked
          // good. Here's your earthenware cookware.
          else if (text.contains("you placed your order a lifetime ago")) {
            Preferences.setString("muffinOnOrder", "none");
          }
          // You spot your order from the other day, neatly labelled on the pickup shelf.
          else if (text.contains("You spot your order from the other day")) {
            Preferences.setString("muffinOnOrder", "none");
          }
          // (If we have an earthenware muffin tin and the "order" buttons are present)
          else if (text.contains("Order a blueberry muffin")) {
            Preferences.setString("muffinOnOrder", "none");
          }

          // "Excellent! Here's your muffin tin! Stop by any time to order a muffin!"
          if (text.contains("Here's your muffin tin!")) {
            ResultProcessor.processItem(ItemPool.SHOVELFUL_OF_EARTH, -10);
            ResultProcessor.processItem(ItemPool.HUNK_OF_GRANITE, -10);
          }
          break;
        }

      case 1312:
        {
          // Choose a Soundtrack
          Matcher matcher = ChoiceManager.BOOMBOX_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("_boomBoxSongsLeft", matcher.group(1));
          }
          Preferences.setString("boomBoxSong", "");
          matcher = ChoiceManager.BOOMBOX_SONG_PATTERN.matcher(text);
          while (matcher.find()) {
            if (matcher.group(2) != null && matcher.group(2).contains("Keep playing")) {
              Preferences.setString("boomBoxSong", matcher.group(1));
            }
          }
          break;
        }

      case 1313:
        // Bastille Battalion
        if (!text.contains("option=5")) {
          Preferences.setInteger("_bastilleGames", 5);
        }
        break;

      case 1316:
        {
          // GAME OVER
          Matcher matcher = ChoiceManager.BASTILLE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_bastilleGames", 5 - StringUtilities.parseInt(matcher.group(1)));
          }
          break;
        }

      case 1322:
        // The Beginning of the Neverend
        if (text.contains("talk to him and help him get more booze")) {
          Preferences.setString("_questPartyFairQuest", "booze");
        } else if (text.contains("Think you can help me clean the place up?")) {
          Preferences.setString("_questPartyFairQuest", "trash");
        } else if (text.contains(
            "helping her with whatever problem she's having with the snacks")) {
          Preferences.setString("_questPartyFairQuest", "food");
        } else if (text.contains("megawoots right now")) {
          Preferences.setString("_questPartyFairQuest", "woots");
        } else if (text.contains("taking up a collection from the guests")) {
          Preferences.setString("_questPartyFairQuest", "dj");
        } else if (text.contains("all of the people to leave")) {
          Preferences.setString("_questPartyFairQuest", "partiers");
        }
        break;

      case 1324:
        // It Hasn't Ended, It's Just Paused
        Preferences.setInteger("encountersUntilNEPChoice", 7);
        break;

      case 1329:
        // Latte Shop
        LatteRequest.parseVisitChoice(text);
        break;

      case 1331:
        {
          // Daily Loathing Ballot
          Matcher localMatcher = ChoiceManager.VOTE_PATTERN.matcher(text);
          while (localMatcher.find()) {
            int voteValue = StringUtilities.parseInt(localMatcher.group(1)) + 1;
            String voteMod = Modifiers.parseModifier(localMatcher.group(3));
            if (voteMod != null) {
              Preferences.setString("_voteLocal" + voteValue, voteMod);
            }
          }

          Matcher platformMatcher = ChoiceManager.VOTE_SPEECH_PATTERN.matcher(text);

          int count = 1;

          while (platformMatcher.find()) {
            String party = platformMatcher.group(3);
            String speech = platformMatcher.group(4);

            String monsterName = ChoiceManager.parseVoteSpeech(party, speech);

            if (monsterName != null) {
              Preferences.setString("_voteMonster" + count, monsterName);
            }

            count++;
          }
          break;
        }

      case 1336:
        {
          // Boxing Daycare
          Matcher matcher = ChoiceManager.DAYCARE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setString("daycareEquipment", matcher.group(1).replaceAll(",", ""));
            Preferences.setString("daycareToddlers", matcher.group(2).replaceAll(",", ""));
            String instructors = matcher.group(3);
            if (instructors.equals("an")) {
              instructors = "1";
            }
            Preferences.setString("daycareInstructors", instructors);
          } else {
            matcher = ChoiceManager.EARLY_DAYCARE_PATTERN.matcher(text);
            if (matcher.find()) {
              Preferences.setString("daycareToddlers", matcher.group(1).replaceAll(",", ""));
              String instructors = matcher.group(2);
              if (instructors.equals("an")) {
                instructors = "1";
              }
              Preferences.setString("daycareInstructors", instructors);
            }
          }
          Matcher recruitsToday = ChoiceManager.DAYCARE_RECRUITS_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_daycareRecruits", (recruitsToday.group(1).replaceAll(",", "")).length() - 3);
          }
          // *** Update _daycareScavenges
          // *** Update daycareInstructorCost (new)
          break;
        }

      case 1339:
        {
          // A Little Pump and Grind
          Matcher matcher = ChoiceManager.SAUSAGE_PATTERN.matcher(text);
          if (matcher.find()) {
            Preferences.setInteger(
                "_sausagesMade",
                StringUtilities.parseInt(matcher.group(2).replaceAll(",", "")) / 111 - 1);
            Preferences.setString("sausageGrinderUnits", matcher.group(3));
          }
          break;
        }

      case 1340:
        {
          // Is There A Doctor In The House?
          Matcher matcher = ChoiceManager.DOCTOR_BAG_PATTERN.matcher(text);
          if (matcher.find()) {
            String malady = matcher.group(1);
            String item = "";
            if (malady.contains("tropical heatstroke")) {
              item = "palm-frond fan";
            } else if (malady.contains("archaic cough")) {
              item = "antique bottle of cough syrup";
            } else if (malady.contains("broken limb")) {
              item = "cast";
            } else if (malady.contains("low vim and vigor")) {
              item = "Doc Galaktik's Vitality Serum";
            } else if (malady.contains("bad clams")) {
              item = "anti-anti-antidote";
            } else if (malady.contains("criss-cross laceration")) {
              item = "plaid bandage";
            } else if (malady.contains("knocked out by a random encounter")) {
              item = "phonics down";
            } else if (malady.contains("Thin Blood Syndrome")) {
              item = "red blood cells";
            } else if (malady.contains("a blood shortage")) {
              item = "bag of pygmy blood";
            }
            Preferences.setString("doctorBagQuestItem", item);
            Preferences.setString("doctorBagQuestLocation", matcher.group(2));
          }
          break;
        }

      case 1388:
        BeachManager.parseCombUsage(text);
        BeachManager.parseBeachMap(text);
        break;

      case 1396:
        {
          // Adjusting Your Fish
          Matcher matcher = ChoiceManager.RED_SNAPPER_PATTERN.matcher(text);
          if (matcher.find()) {
            Phylum phylum = Phylum.find(matcher.group(1));
            int progress = StringUtilities.parseInt(matcher.group(2));
            Preferences.setString("redSnapperPhylum", phylum.toString());
            Preferences.setInteger("redSnapperProgress", progress);
          }
          break;
        }

      case 1407:
        {
          // Mushroom District Costume Shop
          Matcher matcher = ChoiceManager.MUSHROOM_COSTUME_PATTERN.matcher(text);
          int cost = 0;
          boolean carpenter = false;
          boolean gardener = false;
          boolean ballerina = false;
          while (matcher.find()) {
            String costume = matcher.group(2);
            cost = StringUtilities.parseInt(matcher.group(3));
            if (costume.equals("Carpenter")) {
              carpenter = true;
            } else if (costume.equals("Gardener")) {
              gardener = true;
            } else if (costume.equals("Ballerina")) {
              ballerina = true;
            }
          }
          String wearing =
              !carpenter ? "muscle" : !gardener ? "mysticality" : !ballerina ? "moxie" : "none";
          Preferences.setInteger("plumberCostumeCost", cost);
          Preferences.setString("plumberCostumeWorn", wearing);
          break;
        }

      case 1408:
        {
          // Mushroom District Badge Shop
          Matcher matcher = ChoiceManager.MUSHROOM_BADGE_PATTERN.matcher(text);
          int cost = 0;
          if (matcher.find()) {
            cost = StringUtilities.parseInt(matcher.group(1));
          }
          Preferences.setInteger("plumberBadgeCost", cost);
          break;
        }

      case 1410:
        {
          // The Mushy Center

          // The mushroom in your garden is now large enough to walk around inside.  Also there's a
          // door on it, which is convenient for that purpose.

          int mushroomMessageLevel =
              text.contains("walk around inside")
                  ? 11
                  : text.contains("immense mushroom")
                      ? 5
                      : text.contains("giant mushroom")
                          ? 4
                          : text.contains("bulky mushroom")
                              ? 3
                              : text.contains("plump mushroom")
                                  ? 2
                                  : text.contains("decent-sized mushroom") ? 1 : 0;

          // mushgrow5.gif is used for both the immense and colossal mushroom

          int mushroomImageLevel =
              text.contains("mushgrow5.gif")
                  ? 5
                  : text.contains("mushgrow4.gif")
                      ? 4
                      : text.contains("mushgrow3.gif")
                          ? 3
                          : text.contains("mushgrow2.gif")
                              ? 2
                              : text.contains("mushgrow1.gif") ? 1 : 0;

          int mushroomLevel = Math.max(mushroomMessageLevel, mushroomImageLevel);

          // After level 5, you can continue fertilizing for an unknown number
          // of days before you get the ultimate mushroom.

          // If we have fertilized the garden through KoLmafia,
          // this is the number of days we have fertilized.
          int currentLevel = Preferences.getInteger("mushroomGardenCropLevel");

          // If we have not fertilized consistently through KoLmafia, correct it here
          int newLevel = Math.max(mushroomLevel, currentLevel);

          Preferences.setInteger("mushroomGardenCropLevel", newLevel);
          CampgroundRequest.clearCrop();
          CampgroundRequest.setCampgroundItem(new Mushroom(newLevel));
          break;
        }
      case 1412:
        Matcher tierMatcher = GUZZLR_TIER_PATTERN.matcher(text);
        while (tierMatcher.find()) {
          Preferences.setInteger(
              "guzzlr" + tierMatcher.group(2) + "Deliveries",
              StringUtilities.parseInt(tierMatcher.group(1)));
        }

        if (text.contains("You are currently tasked with")) {
          Matcher alreadyMatcher = GUZZLR_QUEST_PATTERN.matcher(text);
          if (alreadyMatcher.find()) {
            String booze = alreadyMatcher.group(1);
            Preferences.setString(
                "guzzlrQuestBooze",
                booze.equals("special personalized cocktail") ? "Guzzlr cocktail set" : booze);
            Preferences.setString("guzzlrQuestClient", alreadyMatcher.group(2));
            Preferences.setString("guzzlrQuestLocation", alreadyMatcher.group(3));
          }

          int itemId = ItemDatabase.getItemId(Preferences.getString("guzzlrQuestBooze"));

          if (itemId > 0) {
            if (InventoryManager.getCount(itemId) > 0) {
              QuestDatabase.setQuestProgress(Quest.GUZZLR, "step1");
            } else {
              QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
            }
          } else {
            QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.STARTED);
          }

          Preferences.setBoolean(
              "_guzzlrQuestAbandoned",
              (ChoiceManager.findChoiceDecisionIndex("Abandon Client", text) == "0"));

          break;
        }

        // If we have unlocked Gold Tier but cannot accept one, we must have already accepted three.
        boolean unlockedGoldTier = Preferences.getInteger("guzzlrBronzeDeliveries") >= 5;
        if (unlockedGoldTier && ChoiceManager.findChoiceDecisionIndex("Gold Tier", text) == "0") {
          Preferences.setInteger("_guzzlrGoldDeliveries", 3);
        }

        // If we have unlocked Platinum Tier but cannot accept one, we must have already accepted
        // one.
        boolean unlockedPlatinumTier = Preferences.getInteger("guzzlrGoldDeliveries") >= 5;
        if (unlockedPlatinumTier
            && ChoiceManager.findChoiceDecisionIndex("Platinum Tier", text) == "0") {
          Preferences.setInteger("_guzzlrPlatinumDeliveries", 1);
        }

        break;

      case 1420:
        // Cargo Cultist Shorts
        CargoCultistShortsRequest.parseAvailablePockets(text);
        break;
      case 1445:
        ScrapheapRequest.parseConfiguration(text);

        if (request.getURLString().contains("show=cpus")) {
          ScrapheapRequest.parseCPUUpgrades(text);
        }
        break;
      case 1447:
        {
          ScrapheapRequest.parseStatbotCost(text);
          break;
        }
      case 1448:
        {
          String[] stalkStatus = new String[7];
          Matcher stalks =
              Pattern.compile(
                      "<button.*?name=\"pp\" value=\"(\\d+)\".*?>\\s+<img.*?src=\".*?/otherimages/powerplant/(\\d+)\\.png\"")
                  .matcher(text);
          while (stalks.find()) {
            int pp = Integer.parseInt(stalks.group(1)) - 1;
            stalkStatus[pp] = stalks.group(2);
          }
          Preferences.setString("_pottedPowerPlant", String.join(",", stalkStatus));
          break;
        }
      case 1449:
        {
          String setting =
              (!text.contains("Warning Beep"))
                  ? "ml"
                  : (!text.contains("Infrared Spectrum"))
                      ? "meat"
                      : (!text.contains("Maximum Framerate")) ? "init" : "";

          Preferences.setString("backupCameraMode", setting);
          Preferences.setBoolean("backupCameraReverserEnabled", text.contains("Disable Reverser"));
        }
    }

    // Do this after special classes (like WumpusManager) have a
    // chance to update state in their visitChoice methods.
    ChoiceManager.lastDecoratedResponseText =
        RequestEditorKit.getFeatureRichHTML(request.getURLString(), text);
  }

  private static String booPeakDamage() {
    int booPeakLevel =
        ChoiceManager.findBooPeakLevel(
            ChoiceManager.findChoiceDecisionText(1, ChoiceManager.lastResponseText));
    if (booPeakLevel < 1) return "";

    int damageTaken = 0;
    int diff = 0;

    switch (booPeakLevel) {
      case 1:
        // actual base damage is 13
        damageTaken = 30;
        diff = 17;
        break;
      case 2:
        // actual base damage is 25
        damageTaken = 30;
        diff = 5;
        break;
      case 3:
        damageTaken = 50;
        break;
      case 4:
        damageTaken = 125;
        break;
      case 5:
        damageTaken = 250;
        break;
    }

    double spookyDamage =
        KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.SPOOKYFORM))
            ? 1.0
            : Math.max(
                damageTaken
                        * (100.0
                            - KoLCharacter.elementalResistanceByLevel(
                                KoLCharacter.getElementalResistanceLevels(Element.SPOOKY)))
                        / 100.0
                    - diff,
                1);
    if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.COLDFORM))
        || KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.SLEAZEFORM))) {
      spookyDamage *= 2;
    }

    double coldDamage =
        KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.COLDFORM))
            ? 1.0
            : Math.max(
                damageTaken
                        * (100.0
                            - KoLCharacter.elementalResistanceByLevel(
                                KoLCharacter.getElementalResistanceLevels(Element.COLD)))
                        / 100.0
                    - diff,
                1);
    if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.SLEAZEFORM))
        || KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.STENCHFORM))) {
      coldDamage *= 2;
    }
    return ((int) Math.ceil(spookyDamage))
        + " spooky damage, "
        + ((int) Math.ceil(coldDamage))
        + " cold damage";
  }

  private static int findBooPeakLevel(String decisionText) {
    if (decisionText == null) {
      return 0;
    }
    if (decisionText.equals("Ask the Question")
        || decisionText.equals("Talk to the Ghosts")
        || decisionText.equals("I Wanna Know What Love Is")
        || decisionText.equals("Tap Him on the Back")
        || decisionText.equals("Avert Your Eyes")
        || decisionText.equals("Approach a Raider")
        || decisionText.equals("Approach the Argument")
        || decisionText.equals("Approach the Ghost")
        || decisionText.equals("Approach the Accountant Ghost")
        || decisionText.equals("Ask if He's Lost")) {
      return 1;
    } else if (decisionText.equals("Enter the Crypt")
        || decisionText.equals("Try to Talk Some Sense into Them")
        || decisionText.equals("Put Your Two Cents In")
        || decisionText.equals("Talk to the Ghost")
        || decisionText.equals("Tell Them What Werewolves Are")
        || decisionText.equals("Scream in Terror")
        || decisionText.equals("Check out the Duel")
        || decisionText.equals("Watch the Fight")
        || decisionText.equals("Approach and Reproach")
        || decisionText.equals("Talk Back to the Robot")) {
      return 2;
    } else if (decisionText.equals("Go down the Steps")
        || decisionText.equals("Make a Suggestion")
        || decisionText.equals("Tell Them About True Love")
        || decisionText.equals("Scold the Ghost")
        || decisionText.equals("Examine the Pipe")
        || decisionText.equals("Say What?")
        || decisionText.equals("Listen to the Lesson")
        || decisionText.equals("Listen in on the Discussion")
        || decisionText.equals("Point out the Malefactors")
        || decisionText.equals("Ask for Information")) {
      return 3;
    } else if (decisionText.equals("Hurl Some Spells of Your Own")
        || decisionText.equals("Take Command")
        || decisionText.equals("Lose Your Patience")
        || decisionText.equals("Fail to Stifle a Sneeze")
        || decisionText.equals("Ask for Help")
        || decisionText.equals(
            "Ask How Duskwalker Basketball Is Played, Against Your Better Judgment")
        || decisionText.equals("Knights in White Armor, Never Reaching an End")
        || decisionText.equals("Own up to It")
        || decisionText.equals("Approach the Poor Waifs")
        || decisionText.equals("Look Behind You")) {
      return 4;
    } else if (decisionText.equals("Read the Book")
        || decisionText.equals("Join the Conversation")
        || decisionText.equals("Speak of the Pompatus of Love")
        || decisionText.equals("Ask What's Going On")
        || decisionText.equals("Interrupt the Rally")
        || decisionText.equals("Ask What She's Doing Up There")
        || decisionText.equals("Point Out an Unfortunate Fact")
        || decisionText.equals("Try to Talk Sense")
        || decisionText.equals("Ask for Directional Guidance")
        || decisionText.equals("What?")) {
      return 5;
    }

    return 0;
  }

  private static String swampNavigation(final String responseText) {
    if (responseText.contains("facing north")
        || responseText.contains("face north")
        || responseText.contains("indicate north")) {
      return "1";
    }
    if (responseText.contains("facing east")
        || responseText.contains("face east")
        || responseText.contains("indicate east")) {
      return "2";
    }
    if (responseText.contains("facing south")
        || responseText.contains("face south")
        || responseText.contains("indicate south")) {
      return "3";
    }
    if (responseText.contains("facing west")
        || responseText.contains("face west")
        || responseText.contains("indicate west")) {
      return "4";
    }
    if (responseText.contains("And then...")) {
      return "1";
    }
    return "0";
  }

  private static String lightsOutAutomation(final int choice, final String responseText) {
    int automation = Preferences.getInteger("lightsOutAutomation");
    if (automation == 0) {
      return "0";
    }
    switch (choice) {
      case 890:
        if (automation == 1 && responseText.contains("Look Out the Window")) {
          return "3";
        }
        return "1";
      case 891:
        if (automation == 1 && responseText.contains("Check a Pile of Stained Sheets")) {
          return "3";
        }
        return "1";
      case 892:
        if (automation == 1 && responseText.contains("Inspect the Bathtub")) {
          return "3";
        }
        return "1";
      case 893:
        if (automation == 1 && responseText.contains("Make a Snack")) {
          return "4";
        }
        return "1";
      case 894:
        if (automation == 1 && responseText.contains("Go to the Children's Section")) {
          return "2";
        }
        return "1";
      case 895:
        if (automation == 1 && responseText.contains("Dance with Yourself")) {
          return "2";
        }
        return "1";
      case 896:
        if (automation == 1
            && responseText.contains("Check out the Tormented Damned Souls Painting")) {
          return "4";
        }
        return "1";
      case 897:
        if (responseText.contains("Search for a light")) {
          return automation == 1 ? "1" : "2";
        }
        if (responseText.contains("Search a nearby nightstand")) {
          return "3";
        }
        if (responseText.contains("Check a nightstand on your left")) {
          return "1";
        }
        return "2";
      case 898:
        if (responseText.contains("Search for a lamp")) {
          return automation == 1 ? "1" : "2";
        }
        if (responseText.contains("Search over by the (gaaah) stuffed animals")) {
          return "2";
        }
        if (responseText.contains("Examine the Dresser")) {
          return "2";
        }
        if (responseText.contains("Open the bear and put your hand inside")) {
          return "1";
        }
        if (responseText.contains("Unlock the box")) {
          return "1";
        }
        return "2";
      case 899:
        if (responseText.contains("Make a torch")) {
          return automation == 1 ? "1" : "2";
        }
        if (responseText.contains("Examine the Graves")) {
          return "2";
        }
        if (responseText.contains("Examine the grave marked \"Crumbles\"")) {
          return "2";
        }
        return "2";
      case 900:
        if (responseText.contains("Search for a light")) {
          return automation == 1 ? "1" : "2";
        }
        if (responseText.contains("What the heck, let's explore a bit")) {
          return "2";
        }
        if (responseText.contains("Examine the taxidermy heads")) {
          return "2";
        }
        return "2";
      case 901:
        if (responseText.contains("Try to find a light")) {
          return automation == 1 ? "1" : "2";
        }
        if (responseText.contains("Keep your cool")) {
          return "2";
        }
        if (responseText.contains("Investigate the wine racks")) {
          return "2";
        }
        if (responseText.contains("Examine the Pinot Noir rack")) {
          return "3";
        }
        return "2";
      case 902:
        if (responseText.contains("Look for a light")) {
          return automation == 1 ? "1" : "2";
        }
        if (responseText.contains("Search the barrel")) {
          return "2";
        }
        if (responseText.contains("No, but I will anyway")) {
          return "2";
        }
        return "2";
      case 903:
        if (responseText.contains("Search for a light")) {
          return automation == 1 ? "1" : "2";
        }
        if (responseText.contains("Check it out")) {
          return "1";
        }
        if (responseText.contains("Examine the weird machines")) {
          return "3";
        }
        if (responseText.contains("Enter 23-47-99 and turn on the machine")) {
          return "1";
        }
        if (responseText.contains("Oh god")) {
          return "1";
        }
        return "2";
    }
    return "2";
  }

  private static void checkGuyMadeOfBees(final GenericRequest request) {
    KoLCharacter.ensureUpdatedGuyMadeOfBees();
    Preferences.increment("guyMadeOfBeesCount", 1, 5, true);

    String text = request.responseText;
    String urlString = request.getPath();

    if (urlString.startsWith("fight.php")) {
      if (text.contains("guy made of bee pollen")) {
        // Record that we beat the guy made of bees.
        Preferences.setBoolean("guyMadeOfBeesDefeated", true);
      }
    } else if (urlString.startsWith("choice.php") && text.contains("that ship is sailed")) {
      // For some reason, we didn't notice when we
      // beat the guy made of bees. Record it now.
      Preferences.setBoolean("guyMadeOfBeesDefeated", true);
    }
  }

  private static void handleSourceTerminal(final String input, String text) {
    if (input.startsWith("educate")) {
      int successIndex = text.lastIndexOf("active skills");
      int failIndex = text.lastIndexOf("missing educate");
      int listIndex = text.lastIndexOf("usage: educate [target file]");

      if (listIndex > successIndex && listIndex > failIndex) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern EDUCATE_PATTERN = Pattern.compile("rel=\"educate (.*?).edu\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = EDUCATE_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1)).append(".edu");
        }
        Preferences.setString("sourceTerminalEducateKnown", knownString.toString());
        return;
      }

      if (failIndex > successIndex) return;

      String skill = input.substring(7).trim();

      if (Preferences.getString("sourceTerminalChips").contains("DRAM")) {
        Preferences.setString(
            "sourceTerminalEducate1", Preferences.getString("sourceTerminalEducate2"));
        Preferences.setString("sourceTerminalEducate2", skill);
      } else {
        Preferences.setString("sourceTerminalEducate1", skill);
      }
    } else if (input.startsWith("enhance")) {
      int successIndex = text.lastIndexOf("You acquire an effect");
      int badInputIndex = text.lastIndexOf("missing enhance");
      int limitIndex = text.lastIndexOf("enhance limit exceeded");
      int listIndex = text.lastIndexOf("usage: enhance [target file]");

      if (listIndex > limitIndex && listIndex > badInputIndex && listIndex > successIndex) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern ENHANCE_PATTERN = Pattern.compile("rel=\"enhance (.*?).enh\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = ENHANCE_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1) + ".enh");
        }
        Preferences.setString("sourceTerminalEnhanceKnown", knownString.toString());
        return;
      }

      if (limitIndex > badInputIndex && limitIndex > successIndex) {
        String chips = Preferences.getString("sourceTerminalChips");
        int limit = 1 + (chips.contains("CRAM") ? 1 : 0) + (chips.contains("SCRAM") ? 1 : 0);
        Preferences.setInteger("_sourceTerminalEnhanceUses", limit);
        return;
      }

      if (badInputIndex > successIndex) return;

      int startIndex = text.lastIndexOf("You acquire");
      int endIndex = text.lastIndexOf("&gt;");
      if (startIndex == -1 || endIndex == -1) {
        // this shouldn't happen...
        return;
      }
      text = text.substring(startIndex, endIndex);
      Pattern ENHANCE_PATTERN =
          Pattern.compile("acquire an effect: (.*?) \\(duration: (\\d+) Adventures\\)</div>");

      Matcher matcher = ENHANCE_PATTERN.matcher(text);
      if (matcher.find()) {
        String message =
            "You acquire an effect: " + matcher.group(1) + " (" + matcher.group(2) + ")";
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
        // Refresh status manually since KoL doesn't trigger it
        ApiRequest.updateStatus(true);
      }

      Preferences.increment("_sourceTerminalEnhanceUses");
    } else if (input.startsWith("enquiry")) {
      int successIndex = text.lastIndexOf("enquiry mode set:");
      int failIndex = text.lastIndexOf("missing enquiry target");
      int listIndex = text.lastIndexOf("usage: enquiry [target file]");

      if (listIndex > failIndex && listIndex > successIndex) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern ENQUIRY_PATTERN = Pattern.compile("rel=\"enquiry (.*?).enq\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = ENQUIRY_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1) + ".enq");
        }
        Preferences.setString("sourceTerminalEnquiryKnown", knownString.toString());
        return;
      }

      if (failIndex > successIndex) return;

      int beginIndex = successIndex + 18;
      int endIndex = text.indexOf("</div>", beginIndex);
      Preferences.setString("sourceTerminalEnquiry", text.substring(beginIndex, endIndex));
    } else if (input.startsWith("extrude")) {
      int acquire = text.lastIndexOf("You acquire");
      int invalid = text.lastIndexOf("Invalid");
      int insufficient = text.lastIndexOf("Insufficient");
      int confirm = text.lastIndexOf("to confirm");
      int exceeded = text.lastIndexOf("limits exceeded");
      int listIndex = text.lastIndexOf("usage: extrude [target file]");

      if (listIndex > acquire
          && listIndex > invalid
          && listIndex > insufficient
          && listIndex > confirm
          && listIndex > exceeded) {
        int startIndex = text.lastIndexOf("available targets:");
        int endIndex = text.lastIndexOf("&gt;");
        if (startIndex == -1 || endIndex == -1) {
          // this shouldn't happen...
          return;
        }
        text = text.substring(startIndex, endIndex);
        Pattern EXTRUDE_PATTERN = Pattern.compile("rel=\"extrude (.*?).ext\"");
        StringBuilder knownString = new StringBuilder();

        Matcher matcher = EXTRUDE_PATTERN.matcher(text);
        while (matcher.find()) {
          if (knownString.length() > 0) {
            knownString.append(",");
          }
          knownString.append(matcher.group(1) + ".ext");
        }
        Preferences.setString("sourceTerminalExtrudeKnown", knownString.toString());
        return;
      }

      if (exceeded > acquire
          && exceeded > invalid
          && exceeded > insufficient
          && exceeded > confirm) {
        Preferences.setInteger("_sourceTerminalExtrudes", 3);
        return;
      }

      if (invalid > acquire || insufficient > acquire || confirm > acquire) return;

      // Creation must have succeeded
      String message = "";
      if (input.contains("food")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.BROWSER_COOKIE));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -10));
        message = "You acquire an item: browser cookie";
      } else if (input.contains("booze")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.HACKED_GIBSON));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -10));
        message = "You acquire an item: hacked gibson";
      } else if (input.contains("goggles")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_SHADES));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source shades";
      } else if (input.contains("gram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_GRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source terminal GRAM chip";
      } else if (input.contains("pram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_PRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source terminal PRAM chip";
      } else if (input.contains("spam")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_SPAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -100));
        message = "You acquire an item: Source terminal SPAM chip";
      } else if (input.contains("cram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_CRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -1000));
        message = "You acquire an item: Source terminal CRAM chip";
      } else if (input.contains("dram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_DRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -1000));
        message = "You acquire an item: Source terminal DRAM chip";
      } else if (input.contains("tram")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_TERMINAL_TRAM_CHIP));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -1000));
        message = "You acquire an item: Source terminal TRAM chip";
      } else if (input.contains("familiar")) {
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOFTWARE_BUG));
        ResultProcessor.processResult(ItemPool.get(ItemPool.SOURCE_ESSENCE, -10000));
        message = "You acquire an item: software bug";
      }
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      Preferences.increment("_sourceTerminalExtrudes");
    } else if (input.startsWith("status")) {
      int startIndex = text.lastIndexOf("Installed Hardware");
      int endIndex = text.lastIndexOf("&gt;");
      if (startIndex == -1 || endIndex == -1) {
        // this shouldn't happen...
        return;
      }
      text = text.substring(startIndex, endIndex);
      Pattern CHIPS_PATTERN =
          Pattern.compile(
              "PRAM chips installed: (\\d+).*?GRAM chips installed: (\\d+).*?SPAM chips installed: (\\d+)");
      Pattern CHIP_PATTERN = Pattern.compile("<div>(.*?) chip installed");
      StringBuilder chipString = new StringBuilder();

      Matcher matcher = CHIPS_PATTERN.matcher(text);
      if (matcher.find()) {
        Preferences.setInteger("sourceTerminalPram", StringUtilities.parseInt(matcher.group(1)));
        Preferences.setInteger("sourceTerminalGram", StringUtilities.parseInt(matcher.group(2)));
        Preferences.setInteger("sourceTerminalSpam", StringUtilities.parseInt(matcher.group(3)));
      }

      text = text.substring(text.indexOf("reduced"));
      matcher = CHIP_PATTERN.matcher(text);
      while (matcher.find()) {
        if (chipString.length() > 0) {
          chipString.append(",");
        }
        chipString.append(matcher.group(1));
      }
      Preferences.setString("sourceTerminalChips", chipString.toString());
    }
  }

  private static String hobopolisBossName(final int choice) {
    switch (choice) {
      case 200:
        // Enter The Hoboverlord
        return "Hodgman";
      case 201:
        // Home, Home in the Range
        return "Ol' Scratch";
      case 202:
        // Bumpity Bump Bump
        return "Frosty";
      case 203:
        // Deep Enough to Dive
        return "Oscus";
      case 204:
        // Welcome To You!
        return "Zombo";
      case 205:
        // Van, Damn
        return "Chester";
      case 518:
        // Clear and Present Danger
        return "Uncle Hobo";
    }

    return "nobody";
  }

  private static void checkDungeonSewers(final GenericRequest request) {
    // Somewhat Higher and Mostly Dry
    // Disgustin' Junction
    // The Former or the Ladder

    String text = request.responseText;
    int explorations = 0;

    int dumplings = InventoryManager.getAccessibleCount(ItemPool.DUMPLINGS);
    int wads = InventoryManager.getAccessibleCount(ItemPool.SEWER_WAD);
    int oozeo = InventoryManager.getAccessibleCount(ItemPool.OOZE_O);
    int oil = InventoryManager.getAccessibleCount(ItemPool.OIL_OF_OILINESS);
    int umbrella = InventoryManager.getAccessibleCount(ItemPool.GATORSKIN_UMBRELLA);

    // You steel your nerves and descend into the darkened tunnel.
    if (!text.contains("You steel your nerves and descend into the darkened tunnel.")) {
      return;
    }

    // *** CODE TESTS ***

    // You flip through your code binder, and figure out that one
    // of the glyphs is code for 'shortcut', while the others are
    // the glyphs for 'longcut' and 'crewcut', respectively. You
    // head down the 'shortcut' tunnel.

    if (text.contains("'crewcut'")) {
      explorations += 1;
    }

    // You flip through your code binder, and gain a basic
    // understanding of the sign: "This ladder just goes in a big
    // circle. If you climb it you'll end up back where you
    // started." You continue down the tunnel, instead.

    if (text.contains("in a big circle")) {
      explorations += 3;
    }

    // You consult your binder and translate the glyphs -- one of
    // them says "This way to the Great Egress" and the other two
    // are just advertisements for Amalgamated Ladderage, Inc. You
    // head toward the Egress.

    if (text.contains("Amalgamated Ladderage")) {
      explorations += 5;
    }

    // *** ITEM TESTS ***

    // "How about these?" you ask, offering the fish some of your
    // unfortunate dumplings.
    if (text.contains("some of your unfortunate dumplings")) {
      // Remove unfortunate dumplings from inventory
      ResultProcessor.processItem(ItemPool.DUMPLINGS, -1);
      ++explorations;
      dumplings = InventoryManager.getAccessibleCount(ItemPool.DUMPLINGS);
      if (dumplings <= 0) {
        RequestLogger.printLine("That was your last unfortunate dumplings.");
      }
    }

    // Before you can ask him what kind of tribute he wants, you
    // see his eyes light up at the sight of your sewer wad.
    if (text.contains("the sight of your sewer wad")) {
      // Remove sewer wad from inventory
      ResultProcessor.processItem(ItemPool.SEWER_WAD, -1);
      ++explorations;
      wads = InventoryManager.getAccessibleCount(ItemPool.SEWER_WAD);
      if (wads <= 0) {
        RequestLogger.printLine("That was your last sewer wad.");
      }
    }

    // He finds a bottle of Ooze-O, and begins giggling madly. He
    // uncorks the bottle, takes a drink, and passes out in a heap.
    if (text.contains("He finds a bottle of Ooze-O")) {
      // Remove bottle of Ooze-O from inventory
      ResultProcessor.processItem(ItemPool.OOZE_O, -1);
      ++explorations;
      oozeo = InventoryManager.getAccessibleCount(ItemPool.OOZE_O);
      if (oozeo <= 0) {
        RequestLogger.printLine("That was your last bottle of Ooze-O.");
      }
    }

    // You grunt and strain, but you can't manage to get between
    // the bars. In a flash of insight, you douse yourself with oil
    // of oiliness (it takes three whole bottles to cover your
    // entire body) and squeak through like a champagne cork. Only
    // without the bang, and you're not made out of cork, and
    // champagne doesn't usually smell like sewage. Anyway. You
    // continue down the tunnel.
    if (text.contains("it takes three whole bottles")) {
      // Remove 3 bottles of oil of oiliness from inventory
      ResultProcessor.processItem(ItemPool.OIL_OF_OILINESS, -3);
      ++explorations;
      oil = InventoryManager.getAccessibleCount(ItemPool.OIL_OF_OILINESS);
      if (oil < 3) {
        RequestLogger.printLine("You have less than 3 bottles of oil of oiliness left.");
      }
    }

    // Fortunately, your gatorskin umbrella allows you to pass
    // beneath the sewagefall without incident. There's not much
    // left of the umbrella, though, and you discard it before
    // moving deeper into the tunnel.
    if (text.contains("your gatorskin umbrella allows you to pass")) {
      // Unequip gatorskin umbrella and discard it.

      ++explorations;
      AdventureResult item = ItemPool.get(ItemPool.GATORSKIN_UMBRELLA, 1);
      int slot = EquipmentManager.WEAPON;
      if (KoLCharacter.hasEquipped(item, EquipmentManager.WEAPON)) {
        slot = EquipmentManager.WEAPON;
      } else if (KoLCharacter.hasEquipped(item, EquipmentManager.OFFHAND)) {
        slot = EquipmentManager.OFFHAND;
      }

      EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);

      AdventureResult.addResultToList(KoLConstants.inventory, item);
      ResultProcessor.processItem(ItemPool.GATORSKIN_UMBRELLA, -1);
      umbrella = InventoryManager.getAccessibleCount(item);
      if (umbrella > 0) {
        RequestThread.postRequest(new EquipmentRequest(item, slot));
      } else {
        RequestLogger.printLine("That was your last gatorskin umbrella.");
      }
    }

    // *** GRATE ***

    // Further into the sewer, you encounter a halfway-open grate
    // with a crank on the opposite side. What luck -- looks like
    // somebody else opened this grate from the other side!

    if (text.contains("somebody else opened this grate")) {
      explorations += 5;
    }

    // Now figure out how to say what happened. If the player wants
    // to stop if runs out of test items, generate an ERROR and
    // list the missing items in the status message. Otherwise,
    // simply tell how many explorations were accomplished.

    AdventureResult result =
        AdventureResult.tallyItem("sewer tunnel explorations", explorations, false);
    AdventureResult.addResultToList(KoLConstants.tally, result);

    MafiaState state = MafiaState.CONTINUE;
    String message = "+" + explorations + " Explorations";

    if (Preferences.getBoolean("requireSewerTestItems")) {
      String missing = "";
      String comma = "";

      if (dumplings < 1) {
        missing = missing + comma + "unfortunate dumplings";
        comma = ", ";
      }
      if (wads < 1) {
        missing = missing + comma + "sewer wad";
        comma = ", ";
      }
      if (oozeo < 1) {
        missing = missing + comma + "bottle of Ooze-O";
        comma = ", ";
      }
      if (oil < 1) {
        missing = missing + comma + "oil of oiliness";
        comma = ", ";
      }
      if (umbrella < 1) {
        missing = missing + comma + "gatorskin umbrella";
        comma = ", ";
      }
      if (!missing.equals("")) {
        state = MafiaState.ERROR;
        message += ", NEED: " + missing;
      }
    }

    KoLmafia.updateDisplay(state, message);
  }

  private static boolean specialChoiceHandling(final int choice, final GenericRequest request) {
    String decision = null;
    switch (choice) {
      case 485:
        // Fighters of Fighting
        decision = ArcadeRequest.autoChoiceFightersOfFighting(request);
        break;

      case 600:
        // Summon Minion
        if (ChoiceManager.skillUses > 0) {
          // Add the quantity field here and let the decision get added later
          request.addFormField("quantity", String.valueOf(ChoiceManager.skillUses));
        }
        break;

      case 770:
      case 792:
        // Workout in Gyms - no need to do anything further?
        return true;
    }

    if (decision == null) {
      return false;
    }

    request.addFormField("whichchoice", String.valueOf(choice));
    request.addFormField("option", decision);
    request.addFormField("pwd", GenericRequest.passwordHash);
    request.run();

    ChoiceManager.lastResponseText = request.responseText;
    ChoiceManager.lastDecoratedResponseText =
        RequestEditorKit.getFeatureRichHTML(request.getURLString(), request.responseText);

    return true;
  }

  private static String specialChoiceDecision1(
      final int choice, String decision, final int stepCount, final String responseText) {
    // A few choices have non-standard options: 0 is not Manual Control
    switch (choice) {
      case 48:
      case 49:
      case 50:
      case 51:
      case 52:
      case 53:
      case 54:
      case 55:
      case 56:
      case 57:
      case 58:
      case 59:
      case 60:
      case 61:
      case 62:
      case 63:
      case 64:
      case 65:
      case 66:
      case 67:
      case 68:
      case 69:
      case 70:
        // Choices in the Violet Fog

        if (decision.equals("")) {
          return VioletFogManager.handleChoice(choice);
        }

        return decision;

        // Out in the Garden
      case 89:

        // Handle the maidens adventure in a less random
        // fashion that's actually useful.

        switch (StringUtilities.parseInt(decision)) {
          case 0:
            return String.valueOf(KoLConstants.RNG.nextInt(2) + 1);
          case 1:
          case 2:
            return decision;
          case 3:
            return KoLConstants.activeEffects.contains(ChoiceManager.MAIDEN_EFFECT)
                ? String.valueOf(KoLConstants.RNG.nextInt(2) + 1)
                : "3";
          case 4:
            return KoLConstants.activeEffects.contains(ChoiceManager.MAIDEN_EFFECT) ? "1" : "3";
          case 5:
            return KoLConstants.activeEffects.contains(ChoiceManager.MAIDEN_EFFECT) ? "2" : "3";
          case 6:
            return "4";
        }
        return decision;

        // Dungeon Fist!
      case 486:
        if (ChoiceManager.action
            == PostChoiceAction
                .NONE) { // Don't automate this if we logged in in the middle of the game -
          // the auto script isn't robust enough to handle arbitrary starting points.
          return ArcadeRequest.autoDungeonFist(stepCount, responseText);
        }
        return decision;

        // Interview With You
      case 546:
        if (ChoiceManager.action
            == PostChoiceAction
                .NONE) { // Don't automate this if we logged in in the middle of the game -
          // the auto script isn't robust enough to handle arbitrary starting points.
          return VampOutManager.autoVampOut(
              StringUtilities.parseInt(decision), stepCount, responseText);
        }
        return "0";

        // Summon Minion is a skill
      case 600:
        if (ChoiceManager.skillUses > 0) {
          ChoiceManager.skillUses = 0;
          return "1";
        }
        return "2";

        // Summon Horde is a skill
      case 601:
        if (ChoiceManager.skillUses > 0) {
          // This skill has to be done 1 cast at a time
          ChoiceManager.skillUses--;
          return "1";
        }
        return "2";

      case 665:
        if (ChoiceManager.action == PostChoiceAction.NONE) {
          return GameproManager.autoSolve(stepCount);
        }
        return "0";

      case 702:
        // No Corn, Only Thorns
        return ChoiceManager.swampNavigation(responseText);

      case 890:
      case 891:
      case 892:
      case 893:
      case 894:
      case 895:
      case 896:
      case 897:
      case 898:
      case 899:
      case 900:
      case 901:
      case 902:
      case 903:
        // Lights Out adventures
        return ChoiceManager.lightsOutAutomation(choice, responseText);

      case 904:
      case 905:
      case 906:
      case 907:
      case 908:
      case 909:
      case 910:
      case 911:
      case 912:
      case 913:
        // Choices in the Louvre

        if (decision.equals("")) {
          return LouvreManager.handleChoice(choice, stepCount);
        }

        return decision;

      case 1049:
        {
          // Tomb of the Unknown Your Class Here

          // This handles every choice in the "The Unknown Tomb"
          // Many of them have a single option.
          Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
          if (choices.size() == 1) {
            return "1";
          }

          // The only one that has more than one option is the initial riddle.
          // The option numbers are randomized each time, although the correct
          // answer remains the same.
          String answer = null;
          switch (KoLCharacter.getAscensionClass()) {
            case SEAL_CLUBBER:
              answer = "Boredom.";
              break;
            case TURTLE_TAMER:
              answer = "Friendship.";
              break;
            case PASTAMANCER:
              answer = "Binding pasta thralls.";
              break;
            case SAUCEROR:
              answer = "Power.";
              break;
            case DISCO_BANDIT:
              answer = "Me. Duh.";
              break;
            case ACCORDION_THIEF:
              answer = "Music.";
              break;
          }

          // Only standard classes can join the guild, so we
          // should not fail. But, if we do, cope.
          if (answer == null) {
            return "0";
          }

          // Iterate over the option strings and find the one
          // that matches the correct answer.
          for (Map.Entry<Integer, String> entry : choices.entrySet()) {
            if (entry.getValue().contains(answer)) {
              return String.valueOf(entry.getKey());
            }
          }

          // Again, we should not fail, but cope.
          return "0";
        }

      case 1087:
        // The Dark and Dank and Sinister Cave Entrance
        Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
        if (choices.size() == 1) {
          return "1";
        }

        String answer = null;
        switch (KoLCharacter.getAscensionClass()) {
          case SEAL_CLUBBER:
            answer = "Freak the hell out like a wrathful wolverine.";
            break;
          case TURTLE_TAMER:
            answer = "Sympathize with an amphibian.";
            break;
          case PASTAMANCER:
            answer = "Entangle the wall with noodles.";
            break;
          case SAUCEROR:
            answer = "Shoot a stream of sauce at the wall.";
            break;
          case DISCO_BANDIT:
            answer = "Focus on your disco state of mind.";
            break;
          case ACCORDION_THIEF:
            answer = "Bash the wall with your accordion.";
            break;
        }

        // Only standard classes can join the guild, so we
        // should not fail. But, if we do, cope.
        if (answer == null) {
          return "0";
        }

        // Iterate over the option strings and find the one
        // that matches the correct answer.
        for (Map.Entry<Integer, String> entry : choices.entrySet()) {
          if (entry.getValue().contains(answer)) {
            return String.valueOf(entry.getKey());
          }
        }

        // Again, we should not fail, but cope.
        return "0";
    }

    return decision;
  }

  private static String specialChoiceDecision2(
      final int choice, String decision, final int stepCount, final String responseText) {
    // If the user wants manual control, let 'em have it.
    if (decision.equals("0")) {
      return decision;
    }

    // Otherwise, modify the decision based on character state
    switch (choice) {
        // Heart of Very, Very Dark Darkness
      case 5:
        if (InventoryManager.getCount(ItemPool.INEXPLICABLY_GLOWING_ROCK) < 1) {
          return "2";
        }
        return "1";

        // How Depressing
      case 7:
        if (!KoLCharacter.hasEquipped(ItemPool.get(ItemPool.SPOOKY_GLOVE, 1))) {
          return "2";
        }
        return "1";

        // A Three-Tined Fork
        // Footprints
      case 26:
      case 27:

        // Check if we can satisfy one of user's conditions
        for (int i = 0; i < 12; ++i) {
          if (GoalManager.hasGoal(AdventureDatabase.WOODS_ITEMS[i])) {
            return choice == 26 ? String.valueOf(i / 4 + 1) : String.valueOf(i % 4 / 2 + 1);
          }
        }

        return decision;

        // No sir, away! A papaya war is on!
      case 127:
        switch (StringUtilities.parseInt(decision)) {
          case 1:
          case 2:
          case 3:
            return decision;
          case 4:
            return ChoiceManager.PAPAYA.getCount(KoLConstants.inventory) >= 3 ? "2" : "1";
          case 5:
            return ChoiceManager.PAPAYA.getCount(KoLConstants.inventory) >= 3 ? "2" : "3";
        }
        return decision;

        // Skull, Skull, Skull
      case 155:
        // Option 4 - "Check the shiny object" - is not always available.
        if (decision.equals("4") && !responseText.contains("Check the shiny object")) {
          return "5";
        }
        return decision;

        // Bureaucracy of the Damned
      case 161:
        // Check if we have all of Azazel's objects of evil
        for (int i = 2566; i <= 2568; ++i) {
          AdventureResult item = ItemPool.get(i);
          if (!KoLConstants.inventory.contains(item)) {
            return "4";
          }
        }
        return "1";

        // Choice 162 is Between a Rock and Some Other Rocks
      case 162:

        // If you are wearing the outfit, have Worldpunch, or
        // are in Axecore, take the appropriate decision.
        // Otherwise, auto-skip the goatlet adventure so it can
        // be tried again later.

        return decision.equals("2")
            ? "2"
            : EquipmentManager.isWearingOutfit(OutfitPool.MINING_OUTFIT)
                ? "1"
                : KoLCharacter.inFistcore()
                        && KoLConstants.activeEffects.contains(
                            EffectPool.get(EffectPool.EARTHEN_FIST))
                    ? "1"
                    : KoLCharacter.inAxecore() ? "3" : "2";
        // Random Lack of an Encounter
      case 182:

        // If the player is looking for the model airship,
        // then update their preferences so that KoLmafia
        // automatically switches things for them.
        int option4Mask = (responseText.contains("Gallivant down to the head") ? 1 : 0) << 2;

        if (option4Mask > 0 && GoalManager.hasGoal(ChoiceManager.MODEL_AIRSHIP)) {
          return "4";
        }
        if (Integer.parseInt(decision) < 4) return decision;

        return (option4Mask & Integer.parseInt(decision)) > 0
            ? "4"
            : String.valueOf(Integer.parseInt(decision) - 3);
        // That Explains All The Eyepatches
      case 184:
        switch (KoLCharacter.getPrimeIndex() * 10 + StringUtilities.parseInt(decision)) {
            // Options 4-6 are mapped to the actual class-specific options:
            // 4=drunk & stats, 5=rotgut, 6=combat (not available to Myst)
            // Mus
          case 04:
            return "3";
          case 05:
            return "2";
          case 06:
            return "1";
            // Mys
          case 14:
            return "1";
          case 15:
            return "2";
          case 16:
            return "3";
            // Mox
          case 24:
            return "2";
          case 25:
            return "3";
          case 26:
            return "1";
        }
        return decision;

        // Chatterboxing
      case 191:
        boolean trink = InventoryManager.getCount(ItemPool.VALUABLE_TRINKET) > 0;
        switch (StringUtilities.parseInt(decision)) {
          case 5: // banish or mox
            return trink ? "2" : "1";
          case 6: // banish or mus
            return trink ? "2" : "3";
          case 7: // banish or mys
            return trink ? "2" : "4";
          case 8: // banish or mainstat
            if (trink) return "2";
            switch (KoLCharacter.mainStat()) {
              case MUSCLE:
                return "3";
              case MYSTICALITY:
                return "4";
              case MOXIE:
                return "1";
              default:
                return "0";
            }
        }
        return decision;

        // In the Shade
      case 298:
        if (decision.equals("1")) {
          int seeds = InventoryManager.getCount(ItemPool.SEED_PACKET);
          int slime = InventoryManager.getCount(ItemPool.GREEN_SLIME);
          if (seeds < 1 || slime < 1) {
            return "2";
          }
        }
        return decision;

        // A Vent Horizon
      case 304:

        // If we've already summoned three batters today or we
        // don't have enough MP, ignore this choice adventure.

        if (decision.equals("1")
            && (Preferences.getInteger("tempuraSummons") == 3
                || KoLCharacter.getCurrentMP() < 200)) {
          return "2";
        }
        return decision;

        // There is Sauce at the Bottom of the Ocean
      case 305:

        // If we don't have a Mer-kin pressureglobe, ignore
        // this choice adventure.

        if (decision.equals("1") && InventoryManager.getCount(ItemPool.MERKIN_PRESSUREGLOBE) < 1) {
          return "2";
        }
        return decision;

        // Barback
      case 309:

        // If we've already found three seaodes today,
        // ignore this choice adventure.

        if (decision.equals("1") && Preferences.getInteger("seaodesFound") == 3) {
          return "2";
        }
        return decision;

        // Arboreal Respite
      case 502:
        if (decision.equals("2")) {
          // mosquito larva, tree-holed coin, vampire
          if (!Preferences.getString("choiceAdventure505").equals("2")) {
            return decision;
          }

          // We want a tree-holed coin. If we already
          // have one, get Spooky Temple Map instead
          if (InventoryManager.getCount(ItemPool.TREE_HOLED_COIN) > 0) {
            return "3";
          }

          // We don't have a tree-holed coin. Either
          // obtain one or exit without consuming an
          // adventure
        }
        return decision;

        // Tree's Last Stand
      case 504:

        // If we have Bar Skins, sell them all
        if (InventoryManager.getCount(ItemPool.BAR_SKIN) > 1) {
          return "2";
        } else if (InventoryManager.getCount(ItemPool.BAR_SKIN) > 0) {
          return "1";
        }

        // If we don't have a Spooky Sapling, buy one
        // unless we've already unlocked the Hidden Temple
        //
        // We should buy one if it is on our conditions - i.e.,
        // the player is intentionally collecting them - but we
        // have to make sure that each purchased sapling
        // decrements the condition so we don't loop and buy
        // too many.

        if (InventoryManager.getCount(ItemPool.SPOOKY_SAPLING) == 0
            && !KoLCharacter.getTempleUnlocked()
            && KoLCharacter.getAvailableMeat() >= 100) {
          return "3";
        }

        // Otherwise, exit this choice
        return "4";

      case 535:
        if (ChoiceManager.action
            == PostChoiceAction
                .NONE) { // Don't automate this if we logged in in the middle of the game -
          // the auto script isn't robust enough to handle arbitrary starting points.
          return SafetyShelterManager.autoRonald(decision, stepCount, responseText);
        }
        return "0";

      case 536:
        if (ChoiceManager.action
            == PostChoiceAction
                .NONE) { // Don't automate this if we logged in in the middle of the game -
          // the auto script isn't robust enough to handle arbitrary starting points.
          return SafetyShelterManager.autoGrimace(decision, stepCount, responseText);
        }
        return "0";

        // Dark in the Attic
      case 549:

        // Some choices appear depending on whether
        // the boombox is on or off

        // 1 - acquire staff guides
        // 2 - acquire ghost trap
        // 3 - turn on boombox (raise area ML)
        // 4 - turn off boombox (lower area ML)
        // 5 - mass kill werewolves

        boolean boomboxOn = responseText.contains("sets your heart pounding and pulse racing");

        switch (StringUtilities.parseInt(decision)) {
          case 0: // show in browser
          case 1: // acquire staff guides
          case 2: // acquire ghost trap
            return decision;
          case 3: // mass kill werewolves with silver shotgun shell
            return "5";
          case 4: // raise area ML, then acquire staff guides
            return !boomboxOn ? "3" : "1";
          case 5: // raise area ML, then acquire ghost trap
            return !boomboxOn ? "3" : "2";
          case 6: // raise area ML, then mass kill werewolves
            return !boomboxOn ? "3" : "5";
          case 7: // raise area ML, then mass kill werewolves or ghost trap
            return !boomboxOn
                ? "3"
                : InventoryManager.getCount(ItemPool.SILVER_SHOTGUN_SHELL) > 0 ? "5" : "2";
          case 8: // lower area ML, then acquire staff guides
            return boomboxOn ? "4" : "1";
          case 9: // lower area ML, then acquire ghost trap
            return boomboxOn ? "4" : "2";
          case 10: // lower area ML, then mass kill werewolves
            return boomboxOn ? "4" : "5";
          case 11: // lower area ML, then mass kill werewolves or ghost trap
            return boomboxOn
                ? "4"
                : InventoryManager.getCount(ItemPool.SILVER_SHOTGUN_SHELL) > 0 ? "5" : "2";
        }
        return decision;

        // The Unliving Room
      case 550:

        // Some choices appear depending on whether
        // the windows are opened or closed

        // 1 - close the windows (raise area ML)
        // 2 - open the windows (lower area ML)
        // 3 - mass kill zombies
        // 4 - mass kill skeletons
        // 5 - get costume item

        boolean windowsClosed = responseText.contains("covered all their windows");
        int chainsaw = InventoryManager.getCount(ItemPool.CHAINSAW_CHAIN);
        int mirror = InventoryManager.getCount(ItemPool.FUNHOUSE_MIRROR);

        switch (StringUtilities.parseInt(decision)) {
          case 0: // show in browser
            return decision;
          case 1: // mass kill zombies with chainsaw chain
            return "3";
          case 2: // mass kill skeletons with funhouse mirror
            return "4";
          case 3: // get costume item
            return "5";
          case 4: // raise area ML, then mass kill zombies
            return !windowsClosed ? "1" : "3";
          case 5: // raise area ML, then mass kill skeletons
            return !windowsClosed ? "1" : "4";
          case 6: // raise area ML, then mass kill zombies/skeletons
            return !windowsClosed ? "1" : chainsaw > mirror ? "3" : "4";
          case 7: // raise area ML, then get costume item
            return !windowsClosed ? "1" : "5";
          case 8: // lower area ML, then mass kill zombies
            return windowsClosed ? "2" : "3";
          case 9: // lower area ML, then mass kill skeletons
            return windowsClosed ? "2" : "4";
          case 10: // lower area ML, then mass kill zombies/skeletons
            return windowsClosed ? "2" : chainsaw > mirror ? "3" : "4";
          case 11: // lower area ML, then get costume item
            return windowsClosed ? "2" : "5";
        }
        return decision;

        // Debasement
      case 551:

        // Some choices appear depending on whether
        // the fog machine is on or off

        // 1 - Prop Deportment (choice adventure 552)
        // 2 - mass kill vampires
        // 3 - turn up the fog machine (raise area ML)
        // 4 - turn down the fog machine (lower area ML)

        boolean fogOn = responseText.contains("white clouds of artificial fog");

        switch (StringUtilities.parseInt(decision)) {
          case 0: // show in browser
          case 1: // Prop Deportment
          case 2: // mass kill vampires with plastic vampire fangs
            return decision;
          case 3: // raise area ML, then Prop Deportment
            return fogOn ? "1" : "3";
          case 4: // raise area ML, then mass kill vampires
            return fogOn ? "2" : "3";
          case 5: // lower area ML, then Prop Deportment
            return fogOn ? "4" : "1";
          case 6: // lower area ML, then mass kill vampires
            return fogOn ? "4" : "2";
        }
        return decision;

        // Prop Deportment
      case 552:

        // Allow the user to let Mafia pick
        // which prop to get

        // 1 - chainsaw
        // 2 - Relocked and Reloaded
        // 3 - funhouse mirror
        // 4 - chainsaw chain OR funhouse mirror

        chainsaw = InventoryManager.getCount(ItemPool.CHAINSAW_CHAIN);
        mirror = InventoryManager.getCount(ItemPool.FUNHOUSE_MIRROR);

        switch (StringUtilities.parseInt(decision)) {
          case 0: // show in browser
          case 1: // chainsaw chain
          case 2: // Relocked and Reloaded
          case 3: // funhouse mirror
            return decision;
          case 4: // chainsaw chain OR funhouse mirror
            return chainsaw < mirror ? "1" : "3";
        }
        return decision;

        // Relocked and Reloaded
      case 553:

        // Choices appear depending on whether
        // you have the item to melt

        // 1 - Maxwell's Silver Hammer
        // 2 - silver tongue charrrm bracelet
        // 3 - silver cheese-slicer
        // 4 - silver shrimp fork
        // 5 - silver pat&eacute; knife
        // 6 - don't melt anything

        int item = 0;

        switch (StringUtilities.parseInt(decision)) {
          case 0: // show in browser
          case 6: // don't melt anything
            return decision;
          case 1: // melt Maxwell's Silver Hammer
            item = ItemPool.MAXWELL_HAMMER;
            break;
          case 2: // melt silver tongue charrrm bracelet
            item = ItemPool.TONGUE_BRACELET;
            break;
          case 3: // melt silver cheese-slicer
            item = ItemPool.SILVER_CHEESE_SLICER;
            break;
          case 4: // melt silver shrimp fork
            item = ItemPool.SILVER_SHRIMP_FORK;
            break;
          case 5: // melt silver pat&eacute; knife
            item = ItemPool.SILVER_PATE_KNIFE;
            break;
        }

        if (item == 0) {
          return "6";
        }
        return InventoryManager.getCount(item) > 0 ? decision : "6";

        // Tool Time
      case 558:

        // Choices appear depending on whether
        // you have enough lollipop sticks

        // 1 - sucker bucket (4 lollipop sticks)
        // 2 - sucker kabuto (5 lollipop sticks)
        // 3 - sucker hakama (6 lollipop sticks)
        // 4 - sucker tachi (7 lollipop sticks)
        // 5 - sucker scaffold (8 lollipop sticks)
        // 6 - skip adventure

        if (decision.equals("0") || decision.equals("6")) {
          return decision;
        }

        int amount = 3 + StringUtilities.parseInt(decision);
        return InventoryManager.getCount(ItemPool.LOLLIPOP_STICK) >= amount ? decision : "6";

        // Duffel on the Double
      case 575:
        // Option 2 - "Dig deeper" - is not always available.
        if (decision.equals("2") && !responseText.contains("Dig deeper")) {
          return "3";
        }
        return decision;

      case 594:
        if (ChoiceManager.action
            == PostChoiceAction
                .NONE) { // Don't automate this if we logged in in the middle of the game -
          // the auto script isn't robust enough to handle arbitrary starting points.
          return LostKeyManager.autoKey(decision, stepCount, responseText);
        }
        return "0";

      case 678:
        // Option 3 isn't always available, but decision to take isn't clear if it's selected, so
        // show in browser
        if (decision.equals("3") && !responseText.contains("Check behind the trash can")) {
          return "0";
        }
        return decision;

      case 690:
        // The First Chest Isn't the Deepest.
      case 691:
        // Second Chest

        // *** These are chests in the daily dungeon.

        // If you have a Ring of Detect Boring Doors equipped,
        // "go through the boring door"

        return decision;

      case 692:
        // I Wanna Be a Door

        // *** This is the locked door in the daily dungeon.

        // If you have a Platinum Yendorian Express Card, use it.
        // Otherwise, if you have pick-o-matic lockpicks, use them
        // Otherwise, if you have a skeleton key, use it.

        if (decision.equals("11")) {
          if (InventoryManager.getCount(ItemPool.EXPRESS_CARD) > 0) {
            return "7";
          } else if (InventoryManager.getCount(ItemPool.PICKOMATIC_LOCKPICKS) > 0) {
            return "3";
          } else if (InventoryManager.getCount(ItemPool.SKELETON_KEY) > 0) {
            return "2";
          } else {
            // Cannot unlock door
            return "0";
          }
        }

        // Use highest stat to try to pass door
        if (decision.equals("12")) {
          int buffedMuscle = KoLCharacter.getAdjustedMuscle();
          int buffedMysticality = KoLCharacter.getAdjustedMysticality();
          int buffedMoxie = KoLCharacter.getAdjustedMoxie();

          if (buffedMuscle >= buffedMysticality && buffedMuscle >= buffedMoxie) {
            return "4";
          } else if (buffedMysticality >= buffedMuscle && buffedMysticality >= buffedMoxie) {
            return "5";
          } else {
            return "6";
          }
        }
        return decision;

      case 693:
        // It's Almost Certainly a Trap

        // *** This is a trap in the daily dungeon.

        // If you have an eleven-foot pole, use it.

        return decision;

        // Delirium in the Cafeterium
      case 700:
        if (decision.equals("1")) {
          return (KoLConstants.activeEffects.contains(ChoiceManager.JOCK_EFFECT)
              ? "1"
              : KoLConstants.activeEffects.contains(ChoiceManager.NERD_EFFECT) ? "2" : "3");
        }
        return decision;

        // Halls Passing in the Night
      case 705:
        // Option 2-4 aren't always available, but decision to take isn't clear if it's selected, so
        // show in browser
        if (decision.equals("2") && !responseText.contains("Go to the janitor's closet")) {
          return "0";
        }
        if (decision.equals("3") && !responseText.contains("Head to the bathroom")) {
          return "0";
        }
        if (decision.equals("4") && !responseText.contains("Check out the teacher's lounge")) {
          return "0";
        }
        return decision;

        // The Cabin in the Dreadsylvanian Woods
      case 721:
        // Option 5 - "Use a ghost pencil" - is not always available.
        // Even if it is, if you already have this shortcut, skip it
        if (decision.equals("5")
            && (!responseText.contains("Use a ghost pencil")
                || Preferences.getBoolean("ghostPencil1"))) {
          return "6";
        }
        return decision;

        // Tallest Tree in the Forest
      case 725:
        // Option 5 - "Use a ghost pencil" - is not always available.
        // Even if it is, if you already have this shortcut, skip it
        if (decision.equals("5")
            && (!responseText.contains("Use a ghost pencil")
                || Preferences.getBoolean("ghostPencil2"))) {
          return "6";
        }
        return decision;

        // Below the Roots
      case 729:
        // Option 5 - "Use a ghost pencil" - is not always available.
        // Even if it is, if you already have this shortcut, skip it
        if (decision.equals("5")
            && (!responseText.contains("Use a ghost pencil")
                || Preferences.getBoolean("ghostPencil3"))) {
          return "6";
        }
        return decision;

        // Dreadsylvanian Village Square
      case 733:
        // Option 5 - "Use a ghost pencil" - is not always available.
        // Even if it is, if you already have this shortcut, skip it
        if (decision.equals("5")
            && (!responseText.contains("Use a ghost pencil")
                || Preferences.getBoolean("ghostPencil4"))) {
          return "6";
        }
        return decision;

        // The Even More Dreadful Part of Town
      case 737:
        // Option 5 - "Use a ghost pencil" - is not always available.
        // Even if it is, if you already have this shortcut, skip it
        if (decision.equals("5")
            && (!responseText.contains("Use a ghost pencil")
                || Preferences.getBoolean("ghostPencil5"))) {
          return "6";
        }
        return decision;

        // The Old Duke's Estate
      case 741:
        // Option 5 - "Use a ghost pencil" - is not always available.
        // Even if it is, if you already have this shortcut, skip it
        if (decision.equals("5")
            && (!responseText.contains("Use a ghost pencil")
                || Preferences.getBoolean("ghostPencil6"))) {
          return "6";
        }
        return decision;

        // This Hall is Really Great
      case 745:
        // Option 5 - "Use a ghost pencil" - is not always available.
        // Even if it is, if you already have this shortcut, skip it
        if (decision.equals("5")
            && (!responseText.contains("Use a ghost pencil")
                || Preferences.getBoolean("ghostPencil7"))) {
          return "6";
        }
        return decision;

        // Tower Most Tall
      case 749:
        // Option 5 - "Use a ghost pencil" - is not always available.
        // Even if it is, if you already have this shortcut, skip it
        if (decision.equals("5")
            && (!responseText.contains("Use a ghost pencil")
                || Preferences.getBoolean("ghostPencil8"))) {
          return "6";
        }
        return decision;

        // The Dreadsylvanian Dungeon
      case 753:
        // Option 5 - "Use a ghost pencil" - is not always available.
        // Even if it is, if you already have this shortcut, skip it
        if (decision.equals("5")
            && (!responseText.contains("Use a ghost pencil")
                || Preferences.getBoolean("ghostPencil9"))) {
          return "6";
        }
        return decision;

        // Action Elevator
      case 780:
        // If Boss dead, skip, else if thrice-cursed, fight spirit, if not, get cursed.
        if (decision.equals("1")) {
          return (Preferences.getInteger("hiddenApartmentProgress") >= 7
              ? "6"
              : KoLConstants.activeEffects.contains(ChoiceManager.CURSE3_EFFECT) ? "1" : "2");
        }
        // Only relocate pygmy lawyers once, then leave
        if (decision.equals("3")) {
          return (Preferences.getInteger("relocatePygmyLawyer") == KoLCharacter.getAscensions()
              ? "6"
              : "3");
        }
        return decision;

        // Earthbound and Down
      case 781:
        {
          // Option 1 and 2 are not always available. Take appropriate one if option to
          // take action is selected. If not,leave.
          if (decision.equals("1")) {
            int hiddenApartmentProgress = Preferences.getInteger("hiddenApartmentProgress");
            return (hiddenApartmentProgress == 7 ? "2" : hiddenApartmentProgress < 1 ? "1" : "6");
          }
          return decision;
        }

        // Water You Dune
      case 783:
        {
          // Option 1 and 2 are not always available. Take appropriate one if option to
          // take action is selected. If not, leave.
          if (decision.equals("1")) {
            int hiddenHospitalProgress = Preferences.getInteger("hiddenHospitalProgress");
            return (hiddenHospitalProgress == 7 ? "2" : hiddenHospitalProgress < 1 ? "1" : "6");
          }
          return decision;
        }

        // Air Apparent
      case 785:
        {
          // Option 1 and 2 are not always available. Take appropriate one if option to
          // take action is selected. If not, leave.
          if (decision.equals("1")) {
            int hiddenOfficeProgress = Preferences.getInteger("hiddenOfficeProgress");
            return (hiddenOfficeProgress == 7 ? "2" : hiddenOfficeProgress < 1 ? "1" : "6");
          }
          return decision;
        }

        // Working Holiday
      case 786:
        {
          // If boss dead, fight accountant, fight boss if available, if not, get binder clip if you
          // lack it, if not, fight accountant if you still need file
          if (decision.equals("1")) {
            int hiddenOfficeProgress = Preferences.getInteger("hiddenOfficeProgress");
            boolean hasMcCluskyFile = InventoryManager.getCount(ChoiceManager.MCCLUSKY_FILE) > 0;
            boolean hasMcCluskyFilePage5 =
                InventoryManager.getCount(ChoiceManager.MCCLUSKY_FILE_PAGE5) > 0;
            boolean hasBinderClip = InventoryManager.getCount(ChoiceManager.BINDER_CLIP) > 0;
            return (hiddenOfficeProgress >= 7
                ? "3"
                : hasMcCluskyFile ? "1" : !hasBinderClip ? "2" : !hasMcCluskyFilePage5 ? "3" : "0");
          }
          return decision;
        }

        // Fire when Ready
      case 787:
        {
          // Option 1 and 2 are not always available. Take appropriate one if option to
          // take action is selected. If not, leave.
          if (decision.equals("1")) {
            int hiddenBowlingAlleyProgress = Preferences.getInteger("hiddenBowlingAlleyProgress");
            return (hiddenBowlingAlleyProgress == 7
                ? "2"
                : hiddenBowlingAlleyProgress < 1 ? "1" : "6");
          }
          return decision;
        }

        // Where Does The Lone Ranger Take His Garbagester?
      case 789:
        // Only relocate pygmy janitors once, then get random items
        if (decision.equals("2")
            && Preferences.getInteger("relocatePygmyJanitor") == KoLCharacter.getAscensions()) {
          return "1";
        }
        return decision;

        // Legend of the Temple in the Hidden City
      case 791:
        {
          // Leave if not enough triangles to fight spectre
          int stoneTriangles = InventoryManager.getCount(ChoiceManager.STONE_TRIANGLE);
          if (decision.equals("1") && stoneTriangles < 4) {
            return "6";
          }
          return decision;
        }

        // Silence at last
      case 808:
        // Abort if you want to fight spirit alarm clock but it isn't available.
        if (decision.equals("2") && !responseText.contains("nightstand wasn't here before")) {
          return "0";
        }
        return decision;

        // One Rustic Nightstand
      case 879:
        boolean sausagesAvailable =
            responseText != null && responseText.contains("Check under the nightstand");

        // If the player wants the sausage book and it is
        // available, take it.
        if (decision.equals("4")) {
          return sausagesAvailable ? "4" : "1";
        }

        // Otherwise, if the player is specifically looking for
        // things obtained from the combat, fight!
        for (int i = 0; i < ChoiceManager.MISTRESS_ITEMS.length; ++i) {
          if (GoalManager.hasGoal(ChoiceManager.MISTRESS_ITEMS[i])) {
            return "3";
          }
        }

        return decision;

      case 914:

        // Sometimes, the choice adventure for the louvre
        // loses track of whether to ignore the louvre or not.

        LouvreManager.resetDecisions();
        return Preferences.getInteger("louvreGoal") != 0 ? "1" : "2";

        // Break Time!
      case 919:
        // Abort if you have plundered the register too many times today
        if (decision.equals("1") && responseText.contains("You've already thoroughly")) {
          return "6";
        }
        return decision;

        // All Over the Map
      case 923:
        // Manual control if the choice you want isn't available
        if ((decision.equals("2") && !responseText.contains("Visit the blacksmith's cottage"))
            || (decision.equals("3") && !responseText.contains("Go to the black gold mine"))
            || (decision.equals("4") && !responseText.contains("Check out the black church"))) {
          return "0";
        }
        return decision;

        // Shoe Repair Store
      case 973:
        // Leave if you have no hooch but have chosen to exchange hooch for chroners
        if (decision.equals("2") && !responseText.contains("Turn in Hooch")) {
          return "6";
        }
        return decision;

        // Crazy Still After All These Years
      case 975:
        // Leave if you have less than 5 cocktail onions, even if you haven't decided to
        if (!responseText.contains("Stick in the onions")) {
          return "2";
        }
        return decision;

      case 988:
        // The Containment Unit
        String containment = Preferences.getString("EVEDirections");
        if (containment.length() != 6) {
          return decision;
        }
        int progress = StringUtilities.parseInt(containment.substring(5, 6));
        if (progress < 0 && progress > 5) {
          return decision;
        }
        if (containment.charAt(progress) == 'L') {
          return "1";
        } else if (containment.charAt(progress) == 'R') {
          return "2";
        }
        return decision;

      case 989:
        // Paranormal Test Lab
        if (responseText.contains("ever-changing constellation")) {
          return "1";
        } else if (responseText.contains("card in the circle of light")) {
          return "2";
        } else if (responseText.contains("waves a fly away")) {
          return "3";
        } else if (responseText.contains("back to square one")) {
          return "4";
        } else if (responseText.contains("adds to your anxiety")) {
          return "5";
        }
        return "0";

      case 1026:
        // Home on the Free Range

        // Option 2 is electric boning knife - until you get
        // it, at which point the option is not available.
        if (decision.equals("2") && !responseText.contains("Investigate the noisy drawer")) {
          return "3";
        }
        return decision;

      case 1060:
        // Temporarily Out of Skeletons
        if (decision.equals("4")
            && QuestDatabase.isQuestLaterThan(Quest.MEATSMITH, QuestDatabase.STARTED)) {
          // Can only fight owner til defeated
          return "0";
        }
        return decision;

      case 1061:
        // Heart of Madness
        if (decision.equals("1") && QuestDatabase.isQuestLaterThan(Quest.ARMORER, "step4")) {
          // Can only enter office til Cake Lord is defeated
          return "0";
        } else if (decision.equals("3") && !QuestDatabase.isQuestFinished(Quest.ARMORER)) {
          // Can only access Popular machine after quest complete
          return "0";
        }
        return decision;

        // The Floor Is Yours
      case 1091:
        if (decision.equals("1") && InventoryManager.getCount(ItemPool.GOLD_1970) < 1) {
          // Manual Control if don't have 1,970 carat gold
          return "0";
        } else if (decision.equals("2")
            && InventoryManager.getCount(ItemPool.NEW_AGE_HEALING_CRYSTAL) < 1) {
          // Manual Control if don't have New Age healing crystal
          return "0";
        } else if (decision.equals("3")
            && InventoryManager.getCount(ItemPool.EMPTY_LAVA_BOTTLE) < 1) {
          // Manual Control if don't have empty lava bottle
          return "0";
        } else if (decision.equals("4")
            && InventoryManager.getCount(ItemPool.VISCOUS_LAVA_GLOBS) < 1) {
          // Manual Control if don't have viscous lava globs
          return "0";
        } else if (decision.equals("5")
            && InventoryManager.getCount(ItemPool.GLOWING_NEW_AGE_CRYSTAL) < 1) {
          // Manual Control if don't have glowing New Age crystal
          return "0";
        }

        // 6: "crystalline light bulb + insulated wire + heat-resistant sheet metal -> LavaCo&trade;
        // Lamp housing"
        // This exits choice if you don't have the ing1redients
        // 7: "fused fuse"
        // Doesn't require materials
        // 9: "leave"

        return decision;

      case 1222:
        // Walk away from The Tunnel of L.O.V.E. if you've already had a trip
        if (responseText.contains("You've already gone through the Tunnel once today")) {
          return "2";
        }
        return decision;

      case 1260:
        // A Strange Panel
        return VillainLairDecorator.spoilColorChoice();

      case 1262:
        // What Setting?
        return VillainLairDecorator.Symbology(responseText);
    }
    return decision;
  }

  public static final Object findOption(final Object[] options, final int decision) {
    for (int i = 0; i < options.length; ++i) {
      Object obj = options[i];
      if (obj instanceof Option) {
        Option opt = (Option) obj;
        if (opt.getDecision(i + 1) == decision) {
          return obj;
        }
      } else if (obj instanceof String) {
        if ((i + 1) == decision) {
          return obj;
        }
      }
    }

    return null;
  }

  private static String pickGoalChoice(final String option, final String decision) {
    // If the user wants manual control, let 'em have it.
    if (decision.equals("0")) {
      return decision;
    }

    // Find the options for the choice we've encountered

    Object[] options = null;

    for (int i = 0; i < ChoiceManager.CHOICE_ADVS.length && options == null; ++i) {
      ChoiceAdventure choiceAdventure = ChoiceManager.CHOICE_ADVS[i];
      if (choiceAdventure.getSetting().equals(option)) {
        options = choiceAdventure.getOptions();
        break;
      }
    }

    for (int i = 0; i < ChoiceManager.CHOICE_ADV_SPOILERS.length && options == null; ++i) {
      ChoiceAdventure choiceAdventure = ChoiceManager.CHOICE_ADV_SPOILERS[i];
      if (choiceAdventure.getSetting().equals(option)) {
        options = choiceAdventure.getOptions();
        break;
      }
    }

    // If it's not in the table, return the player's chosen decision.

    if (options == null) {
      return decision;
    }

    // Choose an item in the conditions first, if it's available.
    // This allows conditions to override existing choices.

    boolean items = false;
    for (int i = 0; i < options.length; ++i) {
      Object obj = options[i];
      if (!(obj instanceof Option)) {
        continue;
      }

      Option opt = (Option) obj;
      AdventureResult item = opt.getItem();
      if (item == null) {
        continue;
      }

      if (GoalManager.hasGoal(item)) {
        return String.valueOf(opt.getDecision(i + 1));
      }

      items = true;
    }

    // If none of the options have an associated item, nothing to do.
    if (!items) {
      return decision;
    }

    // Find the spoiler corresponding to the chosen decision
    Object chosen = ChoiceManager.findOption(options, StringUtilities.parseInt(decision));

    // If the player doesn't want to "complete the outfit", nothing to do
    if (chosen == null || !chosen.toString().equals("complete the outfit")) {
      return decision;
    }

    // Pick an item that the player doesn't have yet
    for (int i = 0; i < options.length; ++i) {
      Object obj = options[i];
      if (!(obj instanceof Option)) {
        continue;
      }

      Option opt = (Option) obj;
      AdventureResult item = opt.getItem();
      if (item == null) {
        continue;
      }

      if (!InventoryManager.hasItem(item)) {
        return String.valueOf(opt.getDecision(i + 1));
      }
    }

    // If they have everything, then just return choice 1
    return "1";
  }

  public static final boolean hasGoalButton(final int choice) {
    switch (choice) {
      case 48:
      case 49:
      case 50:
      case 51:
      case 52:
      case 53:
      case 54:
      case 55:
      case 56:
      case 57:
      case 58:
      case 59:
      case 60:
      case 61:
      case 62:
      case 63:
      case 64:
      case 65:
      case 66:
      case 67:
      case 68:
      case 69:
      case 70:
        // Violet Fog
      case 904:
      case 905:
      case 906:
      case 907:
      case 908:
      case 909:
      case 910:
      case 911:
      case 912:
      case 913:
        // The Louvre.
      case 535:
        // Ronald Safety Shelter Map
      case 536:
        // Grimace Safety Shelter Map
      case 546:
        // Interview With You
      case 594:
        // A Lost Room
      case 665:
        // A Gracious Maze
        return true;
    }

    return false;
  }

  public static final void addGoalButton(final StringBuffer buffer, final String goal) {
    // Insert a "Goal" button in-line
    int index = buffer.lastIndexOf("name=choiceform1");
    if (index == -1) {
      return;
    }
    index = buffer.lastIndexOf("<form", index);
    if (index == -1) {
      return;
    }

    // Build a "Goal" button
    StringBuffer button = new StringBuffer();
    String url = "/KoLmafia/specialCommand?cmd=choice-goal&pwd=" + GenericRequest.passwordHash;
    button.append("<form name=goalform action='").append(url).append("' method=post>");
    button.append("<input class=button type=submit value=\"Go To Goal\">");

    // Add the goal
    button.append("<br><font size=-1>(");
    button.append(goal);
    button.append(")</font></form>");

    // Insert it into the page
    buffer.insert(index, button);
  }

  public static final String gotoGoal() {
    String responseText = ChoiceManager.lastResponseText;
    GenericRequest request = ChoiceManager.CHOICE_HANDLER;
    ChoiceManager.processChoiceAdventure(request, "choice.php", responseText);
    RelayRequest.specialCommandResponse = ChoiceManager.lastDecoratedResponseText;
    RelayRequest.specialCommandIsAdventure = true;
    return request.responseText;
  }

  private static String choiceDescription(final int choice, final int decision) {
    // If we have spoilers for this choice, use that
    Object[][] spoilers = ChoiceManager.choiceSpoilers(choice, null);
    if (spoilers != null && spoilers.length > 2) {
      Object spoiler = ChoiceManager.choiceSpoiler(choice, decision, spoilers[2]);
      if (spoiler != null) {
        return spoiler.toString();
      }
    }

    // If we didn't find a spoiler, use KoL's label for the option
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(ChoiceManager.lastResponseText);
    String desc = choices.get(decision);

    // If we still can't find it, throw up our hands
    return (desc == null) ? "unknown" : desc;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php")) {
      return false;
    }

    if (urlString.equals("choice.php")) {
      // Continuing after a multi-fight.
      // Handle those when the real choice comes up.
      return true;
    }

    GenericRequest.itemMonster = null;

    int choice = ChoiceManager.extractChoiceFromURL(urlString);
    int decision = ChoiceManager.extractOptionFromURL(urlString);
    if (choice != 0) {
      switch (choice) {
        case 443:
          // Chess Puzzle
          return RabbitHoleManager.registerChessboardRequest(urlString);

        case 460:
        case 461:
        case 462:
        case 463:
        case 464:
        case 465:
        case 467:
        case 468:
        case 469:
        case 470:
        case 472:
        case 473:
        case 474:
        case 475:
        case 476:
        case 477:
        case 478:
        case 479:
        case 480:
        case 481:
        case 482:
        case 483:
        case 484:
          // Space Trip
        case 471:
          // DemonStar
        case 485:
          // Fighters Of Fighting
        case 486:
          // Dungeon Fist!
        case 488:
        case 489:
        case 490:
        case 491:
          // Meteoid
          return true;

        case 535: // Deep Inside Ronald
        case 536: // Deep Inside Grimace
          return true;

        case 546: // Interview With You
          return true;

        case 1003: // Test Your Might And Also Test Other Things
        case 1015: // The Mirror in the Tower has the View that is True
        case 1020: // Closing Ceremony
        case 1021: // Meet Frank
        case 1022: // Meet Frank
        case 1005: // 'Allo
        case 1006: // One Small Step For Adventurer
        case 1007: // Twisty Little Passages, All Hedge
        case 1008: // Pooling Your Resources
        case 1009: // Good Ol' 44% Duck
        case 1010: // Another Day, Another Fork
        case 1011: // Of Mouseholes and Manholes
        case 1012: // The Last Temptation
        case 1013: // Mazel Tov!
          return SorceressLairManager.registerChoice(choice, urlString);

        case 1053: // The Servants' Quarters
          return EdServantData.registerRequest(urlString);

        case 1063: // Adjust your 'Edpiece
          {
            int index = decision - 1;
            if (index < 0 || index > EdPieceCommand.ANIMAL.length) {
              // Doing nothing
              return true;
            }
            String decoration = EdPieceCommand.ANIMAL[index][0];
            RequestLogger.updateSessionLog();
            RequestLogger.updateSessionLog("edpiece " + decoration);
            return true;
          }

        case 1101: // It's a Barrel Smashing Party!
          {
            if (decision == 2) {
              // We're smashing 100 barrels
              // The results don't say which barrels are being smashed, but it seems to happen in
              // item order
              int count = 100;
              int itemId = ItemPool.LITTLE_FIRKIN;
              RequestLogger.updateSessionLog("smashing 100 barrels");
              while (count > 0 && itemId <= ItemPool.BARNACLED_BARREL) {
                int smashNumber = Math.min(count, InventoryManager.getCount(itemId));
                String name = ItemDatabase.getItemName(itemId);
                if (smashNumber > 0 && name != null) {
                  RequestLogger.updateSessionLog("smash " + smashNumber + " " + name);
                  count -= smashNumber;
                }
                itemId++;
              }
              return true;
            }
            int itemId = ChoiceManager.extractIidFromURL(urlString);
            String name = ItemDatabase.getItemName(itemId);
            if (name != null) {
              RequestLogger.updateSessionLog("smash " + name);
              return true;
            }
            break;
          }

        case 1181: // Your Witchess Set
          {
            String desc = ChoiceManager.choiceDescription(choice, decision);
            RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
            return true;
          }

        case 1182: // Play against the Witchess Pieces
          // These will redirect to a fight. The encounter will suffice.
          if (decision >= 1 && decision <= 7) {
            String desc = "Play against the Witchess pieces";
            RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
          }
          return true;

        case 1334: // Boxing Daycare (Lobby)
        case 1335: // Boxing Daycare Spa
        case 1336: // Boxing Daycare
          // Special logging done elsewhere
          return true;

        case 1339: // A Little Pump and Grind
          if (decision == 1) {
            int itemId = ChoiceManager.extractIidFromURL(urlString);
            int qty = ChoiceManager.extractQtyFromURL(urlString);
            String name = ItemDatabase.getItemName(itemId);
            if (name != null) {
              RequestLogger.updateSessionLog("grinding " + qty + " " + name);
              return true;
            }
          }
          return true;

        case 1352: //  Island #1, Who Are You?
        case 1353: //  What's Behind Island #2?
        case 1354: //  Third Island's the Charm
          {
            String desc = ChoiceManager.choiceDescription(choice, decision);
            RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
            if (desc != null && !desc.equals("Decide Later")) {
              Preferences.setString("_LastPirateRealmIsland", desc);
            }
            return true;
          }

        case 1388: // Comb the Beach
          {
            return BeachCombRequest.registerRequest(urlString);
          }
      }

      if (decision != 0) {
        // Figure out which decision we took
        String desc = ChoiceManager.choiceDescription(choice, decision);
        RequestLogger.updateSessionLog("Took choice " + choice + "/" + decision + ": " + desc);
      }
    } else if (decision == 0) {
      // forceoption=0 will redirect to the real choice.
      // Don't bother logging it.
      return true;
    }

    // By default, we log the url of any choice we take
    RequestLogger.updateSessionLog(urlString);

    switch (choice) {
      case 1195:
        if (decision == 3) {
          KoLAdventure.lastVisitedLocation = null;
          KoLAdventure.lastLocationName = null;
          KoLAdventure.lastLocationURL = urlString;
          KoLAdventure.setLastAdventure("None");
          KoLAdventure.setNextAdventure("None");
          GenericRequest.itemMonster = "Time-Spinner";

          String message = "[" + KoLAdventure.getAdventureCount() + "] Way Back in Time";
          RequestLogger.printLine();
          RequestLogger.printLine(message);

          RequestLogger.updateSessionLog();
          RequestLogger.updateSessionLog(message);
        }
        return true;

      case 1196:
        if (ChoiceManager.lastDecision == 1 && !urlString.contains("monid=0")) {
          KoLAdventure.lastVisitedLocation = null;
          KoLAdventure.lastLocationName = null;
          KoLAdventure.lastLocationURL = urlString;
          KoLAdventure.setLastAdventure("None");
          KoLAdventure.setNextAdventure("None");
          GenericRequest.itemMonster = "Time-Spinner";

          String message = "[" + KoLAdventure.getAdventureCount() + "] A Recent Fight";
          RequestLogger.printLine();
          RequestLogger.printLine(message);

          RequestLogger.updateSessionLog();
          RequestLogger.updateSessionLog(message);
        }
        return true;
    }

    // Special cases
    if (choice == 879 && decision == 3) {
      // One Rustic Nightstand
      //
      // Option 3 redirects to a fight with the remains of a
      // jilted mistress. Unlike other such redirections,
      // this takes a turn.
      RequestLogger.registerLocation("The Haunted Bedroom");
    }

    return true;
  }

  public static final void registerDeferredChoice(final int choice, final String encounter) {
    // If we couldn't find an encounter, do nothing
    if (encounter == null) {
      return;
    }

    switch (choice) {
      case 123: // At Least It's Not Full Of Trash
        RequestLogger.registerLocation("The Hidden Temple");
        break;

      case 125: // No Visible Means of Support
        // The tiles took a turn to get here
        ResultProcessor.processAdventuresUsed(1);
        RequestLogger.registerLocation("The Hidden Temple");
        break;

      case 437: // Flying In Circles
        ResultProcessor.processAdventuresUsed(1);
        RequestLogger.registerLocation("The Nemesis' Lair");
        break;

      case 620: // A Blow Is Struck!
      case 621: // Hold the Line!
      case 622: // The Moment of Truth
      case 634: // Goodbye Fnord
        // These all arise out of a multifight, rather than by
        // visiting a location.
        RequestLogger.registerLastLocation();
        break;

      case 1005: // 'Allo
      case 1006: // One Small Step For Adventurer
      case 1007: // Twisty Little Passages, All Hedge
      case 1008: // Pooling Your Resources
      case 1009: // Good Ol' 44% Duck
      case 1010: // Another Day, Another Fork
      case 1011: // Of Mouseholes and Manholes
      case 1012: // The Last Temptation
      case 1013: // Mazel Tov!
        // This is chain of choices that either immediately
        // follow a fight or the previous choice, either of
        // which takes a turn (unlike normal choice chains)
        String location = "The Hedge Maze (Room " + (choice - 1004) + ")";
        RequestLogger.registerLocation(location);
        break;

      case 1018: // Bee Persistent
      case 1019: // Bee Rewarded
        // Getting here took a turn
        ResultProcessor.processAdventuresUsed(1);
        RequestLogger.registerLocation("The Black Forest");
        break;

      case 1223: // L.O.V. Entrance
      case 1224: // L.O.V. Equipment Room
      case 1225: // L.O.V. Engine Room
      case 1226: // L.O.V. Emergency Room
      case 1227: // L.O.V. Elbow Room
      case 1228: // L.O.V. Emporium
        // This is chain of choices that either immediately
        // follow a fight or the previous choice.
        RequestLogger.registerLastLocation();
        break;

      case 1310: // Granted a Boon
        {
          // Boon after fight, location is currently null, so don't log under that name
          String message = "[" + KoLAdventure.getAdventureCount() + "] God Lobster";
          RequestLogger.printLine();
          RequestLogger.printLine(message);

          RequestLogger.updateSessionLog();
          RequestLogger.updateSessionLog(message);
          break;
        }

      case 1334: // Boxing Daycare (Lobby)
      case 1335: // Boxing Daycare Spa
      case 1336: // Boxing Daycare
        RequestLogger.registerLocation("Boxing Daycare");
        break;
    }
  }

  public static final String findChoiceDecisionIndex(final String text, final String responseText) {
    Matcher matcher = ChoiceManager.DECISION_BUTTON_PATTERN.matcher(responseText);
    while (matcher.find()) {
      String decisionText = matcher.group(2);

      if (decisionText.contains(text)) {
        return StringUtilities.getEntityDecode(matcher.group(1));
      }
    }

    return "0";
  }

  public static final String findChoiceDecisionText(final int index, final String responseText) {
    Matcher matcher = ChoiceManager.DECISION_BUTTON_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int decisionIndex = Integer.parseInt(matcher.group(1));

      if (decisionIndex == index) {
        return matcher.group(2);
      }
    }

    return null;
  }

  public static final void setSkillUses(int uses) {
    // Used for casting skills that lead to a choice adventure
    ChoiceManager.skillUses = uses;
  }

  private static String[] oldManPsychosisSpoilers() {
    Matcher matcher = ChoiceManager.DECISION_BUTTON_PATTERN.matcher(ChoiceManager.lastResponseText);

    String[][] buttons = new String[4][2];
    int i = 0;
    while (matcher.find()) {
      buttons[i][0] = matcher.group(1);
      buttons[i][1] = matcher.group(2);
      ++i;
    }

    // we need to return a string array with len=4 - even if there are buttons missing
    // the missing buttons are just "hidden" and thus the later buttons have the appropriate form
    // field
    // i.e. button 2 may be the first button.

    // As it turns out, I think all this cancels out and they could just be implemented as standard
    // choice adventures,
    // since the buttons are not actually randomized, they are consistent within the four choice
    // adventures that make up the 10 log entry non-combats.
    // Ah well.  Leavin' it here.
    String[] spoilers = new String[4];

    for (int j = 0; j < spoilers.length; j++) {
      for (String[] s : OLD_MAN_PSYCHOSIS_SPOILERS) {
        if (s[0].equals(buttons[j][1])) {
          spoilers[Integer.parseInt(buttons[j][0]) - 1] =
              s[1]; // button 1 text should be in index 0, 2 -> 1, etc.
          break;
        }
      }
    }

    return spoilers;
  }

  private static boolean parseCinderellaTime() {
    Matcher matcher = ChoiceManager.CINDERELLA_TIME_PATTERN.matcher(ChoiceManager.lastResponseText);
    while (matcher.find()) {
      int time = StringUtilities.parseInt(matcher.group(1));
      if (time != -1) {
        Preferences.setInteger("cinderellaMinutesToMidnight", time);
        return true;
      }
    }
    return false;
  }

  private static final Pattern FLUENCY_PATTERN = Pattern.compile("Fluency is now (\\d+)%");

  public static void parseLanguageFluency(final String text, final String setting) {
    Matcher m = FLUENCY_PATTERN.matcher(text);
    if (m.find()) {
      Preferences.setInteger(setting, StringUtilities.parseInt(m.group(1)));
    }
  }

  public static boolean noRelayChoice(int choice) {
    // Some choices are so clear (or non-standard) that we don't want to mark them up
    // but do want a choice in Mafia GUI
    switch (choice) {
      case 1223: // L.O.V. Entrance
      case 1224: // L.O.V. Equipment Room
      case 1225: // L.O.V. Engine Room
      case 1226: // L.O.V. Emergency Room
      case 1227: // L.O.V. Elbow Room
      case 1228: // L.O.V. Emporium
        return true;

      default:
        return false;
    }
  }

  public static boolean canWalkAway() {
    return ChoiceManager.canWalkAway;
  }

  private static void setCanWalkAway(final int choice) {
    ChoiceManager.canWalkAway = ChoiceManager.canWalkFromChoice(choice);
  }

  public static boolean canWalkFromChoice(int choice) {
    switch (choice) {
      case 570: // GameInformPowerDailyPro Walkthru
      case 603: // Skeletons and The Closet
      case 627: // ChibiBuddy&trade; (on)
      case 632: // Add an E-Mail Address
      case 633: // ChibiBuddy&trade; (off)
      case 664: // The Crackpot Mystic's Shed
      case 720: // The Florist Friar's Cottage
      case 767: // Tales of Dread
      case 769: // The Super-Secret Canadian Mind-Control Device
      case 770: // The Institute for Canadian Studies
      case 774: // Opening up the Folder Holder
      case 792: // The Degrassi Knoll Gym
      case 793: // Welcome to The Shore, Inc.
      case 801: // A Reanimated Conversation
      case 804: // Trick or Treat!
      case 810: // K.R.A.M.P.U.S. facility
      case 812: // The Unpermery
      case 821: // LP-ROM burner
      case 835: // Barely Tales
      case 836: // Adventures Who Live in Ice Houses...
      case 844: // The Portal to Horrible Parents
      case 845: // Rumpelstiltskin's Workshop
      case 859: // Upping your grade
      case 867: // Sneaky Peterskills
      case 870: // Hair Today
      case 871: // inspecting Motorbike
      case 872: // Drawn Onward
      case 922: // Summoning Chamber
      case 929: // Control Freak
      case 984: // A Radio on a Beach
      case 985: // The Odd Jobs Board
      case 986: // Control Panel
      case 987: // The Post-Apocalyptic Survivor Encampment
      case 991: // Build a Crimbot!
      case 994: // Hide a Gift!
      case 999: // Shrubberatin'
      case 1003: // Contest Booth
      case 1025: // Reconfigure your Mini-Crimbot
      case 1051: // The Book of the Undying
      case 1053: // The Servants' Quarters
      case 1059: // Helping Make Ends Meat
      case 1063: // Adjust your 'Edpiece
      case 1064: // The Doctor is Out.  Of Herbs.
      case 1065: // Lending a Hand (and a Foot)
      case 1066: // Employee Assignment Kiosk
      case 1067: // Maint Misbehavin'
      case 1076: // Mayo Minder&trade;
      case 1089: // Community Service
      case 1090: // The Towering Inferno Discotheque
      case 1093: // The WLF Bunker
      case 1099: // The Barrel Full of Barrels
      case 1100: // Pray to the Barrel God
      case 1101: // It's a Barrel Smashing Party!
      case 1103: // Doing the Maths
      case 1104: // Tree Tea
      case 1105: // Specifici Tea
      case 1110: // Spoopy
      case 1114: // Walford Rusley, Bucket Collector
      case 1118: // X-32-F Combat Training Snowman Control Console
      case 1171: // LT&T Office
      case 1177: // Book of the West: Cow Punching
      case 1178: // Book of the West: Beanslinging
      case 1179: // Book of the West: Snake Oiling
      case 1181: // Witchess Set
      case 1188: // The Call is Coming from Outside the Simulation
      case 1190: // The Oracle
      case 1191: // Source Terminal
      case 1193: // The Precinct
      case 1195: // Spinning Your Time-Spinner
      case 1197: // Travel back to a Delicious Meal
      case 1217: // Sweet Synthesis
      case 1218: // Wax On
      case 1233: // Equipment Requisition
      case 1234: // Spacegate Vaccination Machine
      case 1235: // Spacegate Terminal
      case 1259: // LI-11 HQ
      case 1264: // Meteor Metal Machinations
      case 1266: // The Hostler
      case 1267: // Rubbed it the Right Way
      case 1270: // Pantagramming
      case 1271: // Mummery
      case 1272: // R&D
      case 1273: // The Cursed Warehouse
      case 1275: // Rummaging through the Garbage
      case 1277: // Extra, Extra
      case 1278: // Madame Zatara’s Relationship Fortune Teller
      case 1320: // A Heist!
      case 1322: // The Beginning of the Neverend
      case 1329: // Latte Shop
      case 1334: // Boxing Daycare (Lobby)
      case 1335: // Boxing Day Spa
      case 1336: // Boxing Daycare
      case 1339: // A Little Pump and Grind
      case 1389: // The Council of Exploathing
      case 1395: // Take your Pills
      case 1396: // Adjusting Your Fish
      case 1407: // Mushroom District Costume Shop
      case 1408: // Mushroom District Badge Shop
      case 1420: // What has it got in its pocketses?
      case 1437: // Setup Your knock-off retro superhero cape
      case 1438: // Setup your knock-off retro superhero cape
      case 1439: // Spread Crimbo Spirit
      case 1445: // Reassembly Station
      case 1447: // Statbot 5000
      case 1448: // Potted Power Plant
      case 1449: // Set Backup Camera Mode
      case 1451: // Fire Captain Hagnk
      case 1452: // Sprinkler Joe
      case 1453: // Fracker Dan
      case 1454: // Cropduster Dusty
        return true;

      default:
        return false;
    }
  }

  public static void logChoices() {
    // Log choice options to the session log
    int choice = ChoiceManager.currentChoice();
    Map<Integer, String> choices =
        ChoiceUtilities.parseChoicesWithSpoilers(ChoiceManager.lastResponseText);
    for (Map.Entry<Integer, String> entry : choices.entrySet()) {
      RequestLogger.updateSessionLog(
          "choice " + choice + "/" + entry.getKey() + ": " + entry.getValue());
    }
    // Give prettier and more verbose output to the gCLI
    ChoiceUtilities.printChoices(ChoiceManager.lastResponseText);
  }
}
