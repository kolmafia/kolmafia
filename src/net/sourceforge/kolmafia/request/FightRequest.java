package net.sourceforge.kolmafia.request;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.combat.Macrofier;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FamTeamRequest.PokeBoost;
import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.BugbearManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.DadManager;
import net.sourceforge.kolmafia.session.DreadScrollManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.IslandManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.session.LoginManager;
import net.sourceforge.kolmafia.session.MonsterManuelManager;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SpadingManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.UnusualConstructManager;
import net.sourceforge.kolmafia.session.WumpusManager;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.DiscoCombatHelper;
import net.sourceforge.kolmafia.webui.HobopolisDecorator;
import net.sourceforge.kolmafia.webui.NemesisDecorator;
import net.sourceforge.kolmafia.webui.VillainLairDecorator;
import org.htmlcleaner.BaseToken;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CommentNode;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class FightRequest extends GenericRequest {
  // Character-class permissions
  private static final PauseObject PAUSER = new PauseObject();
  public static final FightRequest INSTANCE = new FightRequest();

  private static final AdventureResult AMNESIA = EffectPool.get(EffectPool.AMNESIA);
  private static final AdventureResult CUNCTATITIS = EffectPool.get(EffectPool.CUNCTATITIS);
  public static final AdventureResult ONTHETRAIL = EffectPool.get(EffectPool.ON_THE_TRAIL);
  public static final AdventureResult BIRDFORM = EffectPool.get(EffectPool.FORM_OF_BIRD);
  public static final AdventureResult MOLEFORM = EffectPool.get(EffectPool.SHAPE_OF_MOLE);
  public static final AdventureResult INFERNO = EffectPool.get(EffectPool.TASTE_THE_INFERNO);
  public static final AdventureResult COWRRUPTION = EffectPool.get(EffectPool.COWRRUPTION, 0);

  public static final AdventureResult DICTIONARY1 = ItemPool.get(ItemPool.DICTIONARY, 1);
  public static final AdventureResult DICTIONARY2 = ItemPool.get(ItemPool.FACSIMILE_DICTIONARY, 1);
  private static final AdventureResult TEQUILA = ItemPool.get(ItemPool.TEQUILA, -1);

  public static AdventureResult haikuEffect = EffectPool.get(EffectPool.HAIKU_STATE_OF_MIND);
  public static AdventureResult anapestEffect = EffectPool.get(EffectPool.JUST_THE_BEST_ANAPESTS);

  private static final int HEALTH = 0;
  private static final int ATTACK = 1;
  private static final int DEFENSE = 2;

  private static int lastUserId = 0;
  private static String lostInitiativeMessage = "";
  private static String wonInitiativeMessage = "";
  public static String lastMacroUsed = "";

  private static final String lostBatfellowInitiativeMessage =
      "Round 0: Batfellow loses initiative!";
  private static final String wonBatfellowInitiativeMessage = "Round 0: Batfellow wins initiative!";

  // Extra rounds used by KoLmafia doing special actions
  private static int preparatoryRounds = 0;

  // Index into current CCS section since last macro generated
  private static int macroPrefixLength = 0;

  private static String consultScriptThatDidNothing = null;
  public static String combatFilterThatDidNothing = null;
  public static boolean waitingForSpecial;

  public static String lastResponseText = "";
  public static String lastDecoratedResponseText = "";
  public static String currentEncounter = "";
  private static boolean transformed = false;
  private static boolean haveFought = false;
  private static boolean shouldRefresh = false;
  private static boolean initializeAfterFight = false;

  private static boolean isAutomatingFight = false;
  private static boolean isUsingConsultScript = false;

  private static int dreadWoodsKisses = 0;
  private static int dreadVillageKisses = 0;
  private static int dreadCastleKisses = 0;

  private static int startingAttack = 0;

  private static final Pattern COMBATITEM_PATTERN =
      Pattern.compile("<option[^>]*?value=(\\d+)[^>]*?>[^>]*?\\((\\d+)\\)</option>");
  private static final Pattern AVAILABLE_COMBATSKILL_PATTERN =
      Pattern.compile("<option[^>]*?value=\"(\\d+)[^>]*?>(.*?) \\((\\d+)[^<]*</option>");

  // fambattle.php?pwd&famaction[backstab-209]=Backstab
  private static final Pattern FAMBATTLE_PATTERN = Pattern.compile("famaction.*?-(\\d+).*?=(.*)");

  public static final Pattern SKILL_PATTERN = Pattern.compile("whichskill=(\\d+)");
  private static final Pattern ITEM1_PATTERN = Pattern.compile("whichitem=(\\d+)");
  private static final Pattern ITEM2_PATTERN = Pattern.compile("whichitem2=(\\d+)");
  // <script>newpic("https://s3.amazonaws.com/images.kingdomofloathing.com/adventureimages/newt.gif", "a newt",100,100);</script>
  private static final Pattern CLEESH_PATTERN =
      Pattern.compile("newpic\\(\"(.*?)\", \"(.*?)\".*?\\);");
  private static final Pattern WORN_STICKER_PATTERN =
      Pattern.compile("A sticker falls off your weapon, faded and torn");
  private static final Pattern BEEP_PATTERN = Pattern.compile("Your Evilometer beeps (\\d+) times");
  private static final Pattern BALLROOM_SONG_PATTERN =
      Pattern.compile("You hear strains of (?:(lively)|(mellow)|(lovely)) music in the distance");
  private static final Pattern WHICHMACRO_PATTERN = Pattern.compile("whichmacro=(\\d+)");
  private static final Pattern MACRO_PATTERN =
      Pattern.compile(
          "\\G.*?<!-- macroaction: *(\\w+) ++(\\d+)?,? *(\\d+)?.*?(?=$|<!-- macroaction)",
          Pattern.DOTALL);
  private static final Pattern FULLPAGE_PATTERN = Pattern.compile("^.*$", Pattern.DOTALL);
  private static final Pattern MACRO_COMPACT_PATTERN =
      Pattern.compile("(?:#.*?)?([;\\n])[\\s;\\n]*");
  private static final Pattern INTERGNAT1_PATTERN =
      Pattern.compile("used to be a ([A-Za-z0-9 '_]*?) but then I took");
  private static final Pattern INTERGNAT2_PATTERN =
      Pattern.compile("All your ([A-Za-z0-9 '_]*?) are belong to us");
  private static final Pattern INTERGNAT3_PATTERN =
      Pattern.compile("I'm a' chargin' mah ([A-Za-z0-9 '_]*?)!\" it shouts.");
  private static final Pattern INTERGNAT4_PATTERN =
      Pattern.compile("I made you a ([A-Za-z0-9 '_]*?) but I eated it!");

  private static final Pattern NS_ML_PATTERN =
      Pattern.compile(
          "The Sorceress pauses for a moment\\, mutters some words under her breath\\, and straightens out her dress\\. Her skin seems to shimmer for a moment\\.");

  private static final Pattern DETECTIVE_PATTERN =
      Pattern.compile("I deduce that this monster has approximately (\\d+) hit points");
  private static final Pattern YELLOW_WORD_PATTERN =
      Pattern.compile("She said...  <font color=yellow>(.*?)</font>...");
  private static final Pattern BLUE_WORD_PATTERN =
      Pattern.compile("drinks that don't exist, like <font color=blue>(.*?)</font>");
  private static final Pattern SPACE_HELMET_PATTERN = Pattern.compile("Opponent HP: (\\d+)");
  private static final Pattern SLIMED_PATTERN =
      Pattern.compile(
          "it blasts you with a massive loogie that sticks to your (.*?), pulls it off of you");
  private static final Pattern MULTIFIGHT_PATTERN = Pattern.compile("href=\"?/?fight.php");
  private static final Pattern FIGHTCHOICE_PATTERN = Pattern.compile("href=\"?choice.php");

  private static final Pattern KEYOTRON_PATTERN = Pattern.compile("key-o-tron emits (\\d) short");

  private static final Pattern DISCO_MOMENTUM_PATTERN = Pattern.compile("discomo(\\d).gif");

  private static final Pattern SOULSAUCE_PATTERN = Pattern.compile("You absorb (\\d+) Soulsauce");

  private static final Pattern THUNDER_PATTERN = Pattern.compile("swallow <b>(\\d+)</b> dB of it");

  private static final Pattern RAIN_PATTERN = Pattern.compile("recovering <b>(\\d+)</b> drops");

  private static final Pattern LIGHTNING_PATTERN =
      Pattern.compile("recovering <b>(\\d+)</b> bolts");

  private static final Pattern SEAHORSE_PATTERN =
      Pattern.compile("I shall name you &quot;(.*?),&quot; you say.");

  private static final Pattern TIMEPRANK_PATTERN =
      Pattern.compile("A figure steps out from behind this morning and says, &quot;(.*?)&quot;");

  private static final Pattern NANORHINO_CHARGE1_PATTERN = Pattern.compile("(\\d+)% charge");
  private static final Pattern NANORHINO_CHARGE2_PATTERN = Pattern.compile("charge to (\\d+)%");
  private static final Pattern NANORHINO_BUFF_PATTERN =
      Pattern.compile("title=\\\"Nano(?:brawny|brainy|ballsy)\\\"");

  private static final Pattern SHORT_ORDER_EXP_PATTERN =
      Pattern.compile("and tosses a plate of food to (.*?), who gains ([0-9,]+) exp!");

  private static final Pattern RED_BUTTON_PATTERN =
      Pattern.compile("manage to find and recover all but (\\d+) of the buttons");

  private static final Pattern PROSELYTIZATION_PATTERN =
      Pattern.compile("^\\+1 ([^<]+) Proselytization$");

  private static final Pattern CLOG_PATTERN =
      Pattern.compile("Your oil extractor is (\\d+)% clogged up");

  private static final Pattern SOURCE_INTERVAL_PATTERN =
      Pattern.compile("var matrix_speed = (\\d+);");

  private static final Pattern DECEASED_TREE_PATTERN =
      Pattern.compile("Your crimbo tree has (\\d+) needle");

  private static final Pattern BROKEN_CHAMPAGNE_PATTERN =
      Pattern.compile("going wild with the (\\d+)");

  private static final Pattern GARBAGE_SHIRT_PATTERN = Pattern.compile(" (\\d+) more useful scrap");

  private static final Pattern SHARPEN_SAW_PATTERN =
      Pattern.compile(
          "You're really sharpening the old saw.  Looks like you've done (\\d+) out of (\\d+)!");

  private static final Pattern MASK_SWAP_PATTERN =
      Pattern.compile("swap your mask for the monster's (.*?)<script>");

  private static final Pattern BONERDAGON_BLOCK_PATTERN =
      Pattern.compile("pulling the (.*?) out of your pocket");

  private static final Pattern NS1_BLOCK1_PATTERN =
      Pattern.compile("you pull the (.*?) out of your pocket");
  private static final Pattern NS1_BLOCK2_PATTERN =
      Pattern.compile("start to use the (.*?), but the Sorceress");
  private static final Pattern NS1_BLOCK3_PATTERN =
      Pattern.compile("grabs the (.*?) out of your hands");

  private static final Pattern NS2_BLOCK1_PATTERN =
      Pattern.compile("tears the (.*?) out of your hands");
  private static final Pattern NS2_BLOCK2_PATTERN = Pattern.compile("the (.*?) is shattered");
  private static final Pattern NS2_BLOCK3_PATTERN =
      Pattern.compile("use the (.*?), a nasty-looking pseudopod");

  private static final AdventureResult TOOTH = ItemPool.get(ItemPool.SEAL_TOOTH, 1);
  private static final AdventureResult SPICES = ItemPool.get(ItemPool.SPICES, 1);
  private static final AdventureResult MERCENARY = ItemPool.get(ItemPool.TOY_MERCENARY, 1);
  private static final AdventureResult STOMPER = ItemPool.get(ItemPool.MINIBORG_STOMPER, 1);
  private static final AdventureResult LASER = ItemPool.get(ItemPool.MINIBORG_LASER, 1);
  private static final AdventureResult DESTROYER = ItemPool.get(ItemPool.MINIBORG_DESTROYOBOT, 1);
  public static final AdventureResult ANTIDOTE = ItemPool.get(ItemPool.ANTIDOTE, 1);
  private static final AdventureResult EXTRACTOR = ItemPool.get(ItemPool.ODOR_EXTRACTOR, 1);
  private static final AdventureResult PUTTY_SHEET = ItemPool.get(ItemPool.SPOOKY_PUTTY_SHEET, 1);
  private static final AdventureResult RAINDOH_BOX = ItemPool.get(ItemPool.RAIN_DOH_BOX, 1);
  private static final AdventureResult CAMERA = ItemPool.get(ItemPool.CAMERA, 1);
  private static final AdventureResult CRAPPY_CAMERA = ItemPool.get(ItemPool.CRAPPY_CAMERA, 1);
  private static final AdventureResult SHAKING_CAMERA = ItemPool.get(ItemPool.SHAKING_CAMERA, 1);
  private static final AdventureResult SHAKING_CRAPPY_CAMERA =
      ItemPool.get(ItemPool.SHAKING_CAMERA, 1);
  private static final AdventureResult PHOTOCOPIER = ItemPool.get(ItemPool.PHOTOCOPIER, 1);
  private static final AdventureResult PHOTOCOPIED_MONSTER =
      ItemPool.get(ItemPool.PHOTOCOPIED_MONSTER, 1);

  private static final String TOOTH_ACTION = "item" + ItemPool.SEAL_TOOTH;
  private static final String SPICES_ACTION = "item" + ItemPool.SPICES;
  private static final String MERCENARY_ACTION = "item" + ItemPool.TOY_MERCENARY;
  private static final String STOMPER_ACTION = "item" + ItemPool.MINIBORG_STOMPER;
  private static final String LASER_ACTION = "item" + ItemPool.MINIBORG_LASER;
  private static final String DESTROYER_ACTION = "item" + ItemPool.MINIBORG_DESTROYOBOT;
  private static final String OLFACTION_ACTION = "skill" + SkillPool.OLFACTION;

  private static boolean castNoodles = false;
  private static boolean castClubFoot = false;
  private static boolean castShellUp = false;
  private static boolean castAccordionBash = false;
  private static boolean castCleesh = false;
  private static boolean castParaffinPrism = false;
  private static boolean castTerracottaArmy = false;
  private static boolean insultedPirate = false;
  private static boolean usedFlyer = false;
  private static boolean usedBasePair = false;
  private static boolean jiggledChefstaff = false;
  private static boolean handledCan = false;
  private static boolean shotSixgun = false;
  private static boolean squeezedStressBall = false;
  private static boolean canOlfact = true;
  private static boolean canStomp = false;
  public static boolean haiku = false;
  public static boolean anapest = false;
  public static boolean machineElf = false;
  public static boolean innerWolf = false;
  public static boolean pokefam = false;
  public static boolean papier = false;
  public static int currentRound = 0;
  public static boolean won = false;
  public static boolean inMultiFight = false;
  public static boolean choiceFollowsFight = false;
  public static boolean fightFollowsChoice = false;
  public static boolean fightingCopy = false;

  private static String nextAction = null;
  private static String macroErrorMessage = null;

  private static AdventureResult desiredScroll = null;

  private static final AdventureResult SCROLL_334 = ItemPool.get(ItemPool.SCROLL_334, 1);
  private static final AdventureResult SCROLL_668 = ItemPool.get(ItemPool.SCROLL_668, 1);
  private static final AdventureResult SCROLL_30669 = ItemPool.get(ItemPool.SCROLL_30669, 1);
  private static final AdventureResult SCROLL_33398 = ItemPool.get(ItemPool.SCROLL_33398, 1);
  private static final AdventureResult SCROLL_64067 = ItemPool.get(ItemPool.SCROLL_64067, 1);
  private static final AdventureResult SCROLL_64735 = ItemPool.get(ItemPool.GATES_SCROLL, 1);
  private static final AdventureResult SCROLL_31337 = ItemPool.get(ItemPool.ELITE_SCROLL, 1);

  private static final Object[][] NEMESIS_WEAPONS = { // class, LEW, ULEW
    {
      AscensionClass.SEAL_CLUBBER,
      ItemPool.get(ItemPool.HAMMER_OF_SMITING, 1),
      ItemPool.get(ItemPool.SLEDGEHAMMER_OF_THE_VAELKYR, 1)
    },
    {
      AscensionClass.TURTLE_TAMER,
      ItemPool.get(ItemPool.CHELONIAN_MORNINGSTAR, 1),
      ItemPool.get(ItemPool.FLAIL_OF_THE_SEVEN_ASPECTS, 1)
    },
    {
      AscensionClass.PASTAMANCER,
      ItemPool.get(ItemPool.GREEK_PASTA_OF_PERIL, 1),
      ItemPool.get(ItemPool.WRATH_OF_THE_PASTALORDS, 1)
    },
    {
      AscensionClass.SAUCEROR,
      ItemPool.get(ItemPool.SEVENTEEN_ALARM_SAUCEPAN, 1),
      ItemPool.get(ItemPool.WINDSOR_PAN_OF_THE_SOURCE, 1)
    },
    {
      AscensionClass.DISCO_BANDIT,
      ItemPool.get(ItemPool.SHAGADELIC_DISCO_BANJO, 1),
      ItemPool.get(ItemPool.SEEGERS_BANJO, 1)
    },
    {
      AscensionClass.ACCORDION_THIEF,
      ItemPool.get(ItemPool.SQUEEZEBOX_OF_THE_AGES, 1),
      ItemPool.get(ItemPool.TRICKSTER_TRIKITIXA, 1)
    },
  };

  // Skills which require a shield
  private static final HashSet<String> INVALID_WITHOUT_SHIELD = new HashSet<String>();

  static {
    INVALID_WITHOUT_SHIELD.add("2005");
    INVALID_WITHOUT_SHIELD.add("skill shieldbutt");
  }

  private static final HashSet<String> INVALID_OUT_OF_WATER = new HashSet<String>();

  static {
    INVALID_OUT_OF_WATER.add("2024");
    INVALID_OUT_OF_WATER.add("skill summon leviatuga");
  }

  private static final String[][] EVIL_ZONES = {
    {
      "The Defiled Alcove", "cyrptAlcoveEvilness",
    },
    {
      "The Defiled Cranny", "cyrptCrannyEvilness",
    },
    {
      "The Defiled Niche", "cyrptNicheEvilness",
    },
    {
      "The Defiled Nook", "cyrptNookEvilness",
    },
  };

  public enum SpecialMonster {
    // Individual monsters
    ANCIENT_PROTECTOR_SPIRIT("Ancient Protector Spirit"),
    CONJOINED_ZMOMBIE("conjoined zmombie"),
    CYRUS_THE_VIRUS("Cyrus the Virus"),
    DAD_SEA_MONKEE("Dad Sea Monkee"),
    DRIPPY_BAT("drippy bat"),
    DRIPPY_REVELER("drippy reveler"),
    FAMILY_OF_KOBOLDS("family of kobolds"),
    GARGANTULIHC("gargantulihc"),
    GIANT_OCTOPUS("giant octopus"),
    GIANT_SKEELTON("giant skeelton"),
    GLITCH_MONSTER("%monster%"),
    HUGE_GHUOL("huge ghoul"),
    PIRANHA_PLANT("piranha plant"),
    PYGMY_JANITOR("pygmy janitor"),
    PYGMY_WITCH_LAWYER("pygmy witch lawyer"),
    SAUSAGE_GOBLIN("sausage goblin"),
    TIME_SPINNER_PRANK("time-spinner prank"),
    UNUSUAL_CONSTRUCT("unusual construct"),

    // Categories of monsters
    BEE("bee"),
    DMT("Deep Machine Tunnels"),
    EVENT("event monster"),
    GINGERBREAD("Gingerbread City"),
    HIPSTER("hipsters"),
    HOLIDAY("holiday monster"),
    NEMESIS("nemesis assassin"),
    PORTSCAN("portscan.edu"),
    RAIN("Heavy Rains"),
    SEWER("Sewer Tunnel"),
    TACO_ELF("taco elf"),
    WAR_FRATBOY("War Fratboy"),
    WAR_HIPPY("War Hippy"),
    WITCHESS("witchess"),
    WOL("West of Loathing");

    private final String name;

    SpecialMonster(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  private static final Map<String, SpecialMonster> specialMonsters = new HashMap<>();

  // When AdventureRequest initializes AreaCombatData, if calls methods in FightRequest
  // When FightRequest initializes the above map, if calls methods in AdventureRequest
  // Therefore, initialize this map only when it is actually accessed.

  public static final SpecialMonster specialMonsterCategory(final MonsterData monster) {
    FightRequest.initializeSpecialMonsters();
    return FightRequest.specialMonsters.get(monster.getName());
  }

  public static final SpecialMonster specialMonsterCategory(final String name) {
    FightRequest.initializeSpecialMonsters();
    return FightRequest.specialMonsters.get(name);
  }

  private static void initializeSpecialMonsters() {
    if (specialMonsters.size() > 0) {
      return;
    }

    FightRequest.specialMonsters.put("%monster%", SpecialMonster.GLITCH_MONSTER);
    FightRequest.specialMonsters.put(
        "Ancient Protector Spirit", SpecialMonster.ANCIENT_PROTECTOR_SPIRIT);
    FightRequest.specialMonsters.put("conjoined zmombie", SpecialMonster.CONJOINED_ZMOMBIE);
    FightRequest.specialMonsters.put("Cyrus the Virus", SpecialMonster.CYRUS_THE_VIRUS);
    FightRequest.specialMonsters.put("Dad Sea Monkee", SpecialMonster.DAD_SEA_MONKEE);
    FightRequest.specialMonsters.put("drippy bat", SpecialMonster.DRIPPY_BAT);
    FightRequest.specialMonsters.put("drippy reveler", SpecialMonster.DRIPPY_REVELER);
    FightRequest.specialMonsters.put("family of kobolds", SpecialMonster.FAMILY_OF_KOBOLDS);
    FightRequest.specialMonsters.put("gargantulihc", SpecialMonster.GARGANTULIHC);
    FightRequest.specialMonsters.put("giant octopus", SpecialMonster.GIANT_OCTOPUS);
    FightRequest.specialMonsters.put("giant skeelton", SpecialMonster.GIANT_SKEELTON);
    FightRequest.specialMonsters.put("huge ghoul", SpecialMonster.HUGE_GHUOL);
    FightRequest.specialMonsters.put("piranha plant", SpecialMonster.PIRANHA_PLANT);
    FightRequest.specialMonsters.put("pygmy janitor", SpecialMonster.PYGMY_JANITOR);
    FightRequest.specialMonsters.put("pygmy witch lawyer", SpecialMonster.PYGMY_WITCH_LAWYER);
    FightRequest.specialMonsters.put("sausage goblin", SpecialMonster.SAUSAGE_GOBLIN);
    FightRequest.specialMonsters.put("time-spinner prank", SpecialMonster.TIME_SPINNER_PRANK);
    FightRequest.specialMonsters.put("unusual construct", SpecialMonster.UNUSUAL_CONSTRUCT);

    FightRequest.specialMonsters.put("angry bassist", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("blue-haired girl", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("evil ex-girlfriend", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("peeved roommate", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("random scenester", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Beast", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Beetle", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Constellation", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Crimbo Elf", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Demon", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Elemental", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Fish", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Flower", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Frat Orc", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Goblin", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Golem", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Hippy", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Hobo", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Man", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Manloid", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Mer-kin", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Penguin", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Pirate", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Shambling Monstrosity", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Slime", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Spiraling Shape", SpecialMonster.HIPSTER);
    FightRequest.specialMonsters.put("Black Crayon Undead Thing", SpecialMonster.HIPSTER);

    FightRequest.specialMonsters.put("Witchess Bishop", SpecialMonster.WITCHESS);
    FightRequest.specialMonsters.put("Witchess King", SpecialMonster.WITCHESS);
    FightRequest.specialMonsters.put("Witchess Knight", SpecialMonster.WITCHESS);
    FightRequest.specialMonsters.put("Witchess Ox", SpecialMonster.WITCHESS);
    FightRequest.specialMonsters.put("Witchess Pawn", SpecialMonster.WITCHESS);
    FightRequest.specialMonsters.put("Witchess Queen", SpecialMonster.WITCHESS);
    FightRequest.specialMonsters.put("Witchess Rook", SpecialMonster.WITCHESS);
    FightRequest.specialMonsters.put("Witchess Witch", SpecialMonster.WITCHESS);

    FightRequest.specialMonsters.put("beebee gunners", SpecialMonster.BEE);
    FightRequest.specialMonsters.put("moneybee", SpecialMonster.BEE);
    FightRequest.specialMonsters.put("mumblebee", SpecialMonster.BEE);
    FightRequest.specialMonsters.put("beebee queue", SpecialMonster.BEE);
    FightRequest.specialMonsters.put("bee swarm", SpecialMonster.BEE);
    FightRequest.specialMonsters.put("buzzerker", SpecialMonster.BEE);
    FightRequest.specialMonsters.put("Beebee King", SpecialMonster.BEE);
    FightRequest.specialMonsters.put("bee thoven", SpecialMonster.BEE);
    FightRequest.specialMonsters.put("Queen Bee", SpecialMonster.BEE);

    FightRequest.specialMonsters.put("Performer of Actions", SpecialMonster.DMT);
    FightRequest.specialMonsters.put("Thinker of Thoughts", SpecialMonster.DMT);
    FightRequest.specialMonsters.put("Perceiver of Sensations", SpecialMonster.DMT);

    FightRequest.specialMonsters.put("animal cracker", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread alligator", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread convict", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread finance bro", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread gentrifier", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread lawyer", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread mad dog", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread mugger", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread mutant", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread pigeon", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread rat", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread tech bro", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread vagrant", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread vigilante", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread welder robot", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("gingerbread wino", SpecialMonster.GINGERBREAD);
    FightRequest.specialMonsters.put("Judge Fudge", SpecialMonster.GINGERBREAD);
    // The following monster appears following a choice which already counted the turn
    // FightRequest.specialMonsters.put( "GNG-3-R" , SpecialMonster.GINGERBREAD );

    FightRequest.specialMonsters.put("Candied Yam Golem", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Malevolent Tofurkey", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Possessed Can of Cranberry Sauce", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Stuffing Golem", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Hammered Yam Golem", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Inebriated Tofurkey", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Plastered Can of Cranberry Sauce", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Soused Stuffing Golem", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Novio Cad&aacute;ver", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Padre Cad&aacute;ver", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Novia Cad&aacute;ver", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Persona Inocente Cad&aacute;ver", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("ambulatory pirate", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("migratory pirate", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("peripatetic pirate", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("giant pumpkin-head", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("large-headed werewolf", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("oddly-proportioned ghost", SpecialMonster.HOLIDAY);
    FightRequest.specialMonsters.put("Cinco de Mayo reveler", SpecialMonster.HOLIDAY);

    FightRequest.specialMonsters.put("sign-twirling Crimbo elf", SpecialMonster.TACO_ELF);
    FightRequest.specialMonsters.put("tacobuilding elf", SpecialMonster.TACO_ELF);
    FightRequest.specialMonsters.put("taco-clad Crimbo elf", SpecialMonster.TACO_ELF);

    FightRequest.specialMonsters.put("depressing French accordionist", SpecialMonster.EVENT);
    FightRequest.specialMonsters.put("lively Cajun accordionist", SpecialMonster.EVENT);
    FightRequest.specialMonsters.put("quirky indie-rock accordionist", SpecialMonster.EVENT);
    FightRequest.specialMonsters.put("Possessed Can of Linguine-Os", SpecialMonster.EVENT);
    FightRequest.specialMonsters.put("Possessed Can of Creepy Pasta", SpecialMonster.EVENT);
    FightRequest.specialMonsters.put("Frozen Bag of Tortellini", SpecialMonster.EVENT);
    FightRequest.specialMonsters.put("Possessed Jar of Alphredo&trade;", SpecialMonster.EVENT);
    FightRequest.specialMonsters.put("Box of Crafty Dinner", SpecialMonster.EVENT);

    FightRequest.specialMonsters.put("menacing thug", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("Mob Penguin hitman", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("hunting seal", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("turtle trapper", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("evil spaghetti cult assassin", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("b&eacute;arnaise zombie", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("flock of seagulls", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("mariachi bandolero", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("Argarggagarg the Dire Hellseal", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("Safari Jack, Small-Game Hunter", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("Yakisoba the Executioner", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("Heimandatz, Nacho Golem", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("Jocko Homo", SpecialMonster.NEMESIS);
    FightRequest.specialMonsters.put("The Mariachi With No Name", SpecialMonster.NEMESIS);

    FightRequest.specialMonsters.put("alley catfish", SpecialMonster.RAIN);
    FightRequest.specialMonsters.put("aquaconda", SpecialMonster.RAIN);
    FightRequest.specialMonsters.put("freshwater bonefish", SpecialMonster.RAIN);
    FightRequest.specialMonsters.put("giant isopod", SpecialMonster.RAIN);
    FightRequest.specialMonsters.put("giant tardigrade", SpecialMonster.RAIN);
    FightRequest.specialMonsters.put("gourmet gourami", SpecialMonster.RAIN);
    FightRequest.specialMonsters.put("piranhadon", SpecialMonster.RAIN);
    FightRequest.specialMonsters.put("storm cow", SpecialMonster.RAIN);

    FightRequest.specialMonsters.put("aggressive grass snake", SpecialMonster.WOL);
    FightRequest.specialMonsters.put("emaciated rodeo clown", SpecialMonster.WOL);
    FightRequest.specialMonsters.put("furious cow", SpecialMonster.WOL);
    FightRequest.specialMonsters.put("furious giant cow", SpecialMonster.WOL);
    FightRequest.specialMonsters.put("grizzled rodeo clown", SpecialMonster.WOL);
    FightRequest.specialMonsters.put("king snake", SpecialMonster.WOL);
    FightRequest.specialMonsters.put("menacing rodeo clown", SpecialMonster.WOL);
    FightRequest.specialMonsters.put("prince snake", SpecialMonster.WOL);
    FightRequest.specialMonsters.put("ungulith", SpecialMonster.WOL);

    FightRequest.specialMonsters.put("Source Agent", SpecialMonster.PORTSCAN);
    FightRequest.specialMonsters.put("Government agent", SpecialMonster.PORTSCAN);

    FightRequest.addAreaMonsters("A Maze of Sewer Tunnels", SpecialMonster.SEWER);
    FightRequest.addAreaMonsters("The Battlefield (Frat Uniform)", SpecialMonster.WAR_HIPPY);
    FightRequest.addAreaMonsters("The Battlefield (Hippy Uniform)", SpecialMonster.WAR_FRATBOY);
  }

  private static void addAreaMonsters(String area, SpecialMonster category) {
    AreaCombatData combatData = AdventureDatabase.getAreaCombatData(area);
    if (combatData == null) {
      RequestLogger.printLine(
          "Invalid adventure area while initializing special monsters: \"" + area + "\"");
      return;
    }
    for (MonsterData monster : combatData.getMonsters()) {
      FightRequest.specialMonsters.put(monster.getName(), category);
    }
    for (MonsterData monster : combatData.getSuperlikelyMonsters()) {
      FightRequest.specialMonsters.put(monster.getName(), category);
    }
  }

  private static final SimpleDateFormat COMBAT_START = new SimpleDateFormat("yyyyMMddHHmmss");

  static {
    FightRequest.COMBAT_START.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  // Make an HTML cleaner
  private static final HtmlCleaner cleaner = HTMLParserUtils.configureDefaultParser();

  static {
    CleanerProperties props = cleaner.getProperties();
    // Need to parse the forms in a fambattle
    // props.setPruneTags( "form" );
  }

  /**
   * Constructs a new <code>FightRequest</code>. User settings will be used to determine the kind of
   * action to be taken during the battle.
   */
  private FightRequest() {
    super("fight.php");
  }

  public static final void initialize() {}

  public static final void resetKisses() {
    FightRequest.dreadWoodsKisses = 0;
    FightRequest.dreadVillageKisses = 0;
    FightRequest.dreadCastleKisses = 0;
  }

  public static final int dreadKisses(final KoLAdventure location) {
    return FightRequest.dreadKisses(location.getAdventureName());
  }

  public static final int dreadKisses(final String name) {
    return name.endsWith("Woods")
        ? Math.max(FightRequest.dreadWoodsKisses, 1)
        : name.endsWith("Village")
            ? Math.max(FightRequest.dreadVillageKisses, 1)
            : name.endsWith("Castle") ? Math.max(FightRequest.dreadCastleKisses, 1) : 0;
  }

  public static final AdventureResult[] PAPIER_EQUIPMENT = {
    ItemPool.get(ItemPool.PAPIER_MACHETE, 1),
    ItemPool.get(ItemPool.PAPIER_MACHINE_GUN, 1),
    ItemPool.get(ItemPool.PAPIER_MASK, 1),
    ItemPool.get(ItemPool.PAPIER_MITRE, 1),
    ItemPool.get(ItemPool.PAPIER_MACHURIDARS, 1),
  };

  private static boolean usingPapierEquipment() {
    for (int i = 0; i < PAPIER_EQUIPMENT.length; ++i) {
      if (KoLCharacter.hasEquipped(PAPIER_EQUIPMENT[i])) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  protected boolean shouldFollowRedirect() {
    // In Pokefam at least, this can happen as we redirect to fambattle.php
    return true;
  }

  private static final Pattern CAN_STEAL_PATTERN =
      Pattern.compile(
          "value=\"(Pick (?:His|Her|Their|Its) Pocket(?: Again)?|Look for Shiny Objects)\"");

  public static final boolean canStillSteal() {
    // Return true if you can still steal during this battle.

    // Must be a Moxie class character or any character in Birdform
    // or have a tiny black hole equipped.in the offhand slot
    if (!KoLCharacter.canPickpocket()) {
      return false;
    }

    // Look for buttons that allow you to pickpocket
    String responseText = FightRequest.lastResponseText;
    Matcher matcher = FightRequest.CAN_STEAL_PATTERN.matcher(responseText);
    return matcher.find();
  }

  public static final boolean canOlfact() {
    return FightRequest.canOlfact
        && !KoLCharacter.inGLover()
        && !KoLConstants.activeEffects.contains(FightRequest.ONTHETRAIL);
  }

  public static final boolean isSourceAgent() {
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    return monster != null && FightRequest.isSourceAgent(monster);
  }

  public static final boolean isSourceAgent(MonsterData monster) {
    String name = monster.getName();
    return name.equals("Source Agent") || name.equals("One Thousand Source Agents");
  }

  public static final boolean isPirate() {
    return FightRequest.isPirate(MonsterStatusTracker.getLastMonster());
  }

  public static final boolean isPirate(MonsterData monster) {
    return (AdventureDatabase.getAreaCombatData("The Obligatory Pirate's Cove").hasMonster(monster)
        || AdventureDatabase.getAreaCombatData("Barrrney's Barrr").hasMonster(monster)
        || AdventureDatabase.getAreaCombatData("The F'c'le").hasMonster(monster)
        || AdventureDatabase.getAreaCombatData("The Poop Deck").hasMonster(monster)
        || AdventureDatabase.getAreaCombatData("Belowdecks").hasMonster(monster));
  }

  public static final boolean canPirateInsult() {
    return (InventoryManager.getCount(ItemPool.get(ItemPool.PIRATE_INSULT_BOOK, 1)) > 0
            || InventoryManager.getCount(ItemPool.get(ItemPool.MARAUDER_MOCKERY_MANUAL, 1)) > 0)
        && BeerPongRequest.countPirateInsults() != 8
        && FightRequest.insultedPirate == false
        && FightRequest.isPirate();
  }

  public static final boolean canJamFlyer() {
    return KoLConstants.inventory.contains(ItemPool.get(ItemPool.JAM_BAND_FLYERS, 1))
        && Preferences.getInteger("flyeredML") < 10000
        && usedFlyer == false
        && !IslandManager.isBattlefieldMonster()
        && !QuestDatabase.isQuestFinished(QuestDatabase.Quest.ISLAND_WAR)
        && !KoLCharacter.inGLover();
  }

  public static final boolean canRockFlyer() {
    return KoLConstants.inventory.contains(ItemPool.get(ItemPool.ROCK_BAND_FLYERS, 1))
        && Preferences.getInteger("flyeredML") < 10000
        && usedFlyer == false
        && !IslandManager.isBattlefieldMonster()
        && !QuestDatabase.isQuestFinished(QuestDatabase.Quest.ISLAND_WAR)
        && !KoLCharacter.inGLover();
  }

  public static void initializeAfterFight() {
    FightRequest.initializeAfterFight = true;
  }

  public static boolean initializingAfterFight() {
    return FightRequest.initializeAfterFight;
  }

  public static final boolean wonInitiative() {
    return FightRequest.currentRound == 1
        && (FightRequest.pokefam || FightRequest.wonInitiative(FightRequest.lastResponseText));
  }

  public static final boolean wonInitiative(String text) {
    // Regular encounter or on Seahorse underwater
    if (text.toLowerCase().contains("you get the jump")) return true;

    // Can Has Cyborger
    if (text.contains("The Jump: ")) return true;

    // Blavious Kloop

    // You leap into combat, as quick as a wink,
    // attacking the monster before he can blink!

    if (text.contains("as quick as a wink")) return true;

    // Who got the jump? Oh please, who, tell me, who?
    // It wasn't your foe, so it must have been you!

    if (text.contains("It wasn't your foe, so it must have been you")) return true;

    // Your foe is so slow! So slow is your foe!
    // Much slower than you, who are ready to go!

    if (text.contains("Your foe is so slow")) return true;

    // Haiku dungeon

    //    Before he sees you,
    //    you're already attacking.
    //    You're sneaky like that.

    if (text.contains("You're sneaky like that.")) return true;

    //    You leap at your foe,
    //    throwing caution to the wind,
    //    and get the first strike.

    if (text.contains("and get the first strike.")) return true;

    //    You jump at your foe
    //    and strike before he's ready.
    //    Nice and sportsmanlike.

    if (text.contains("Nice and sportsmanlike.")) return true;

    // The Deep Machine Tunnels

    //    It hesitates. It <vocalizes>. It <moves>.

    if (text.contains("It hesitates.")) return true;

    return false;
  }

  public static int getMacroPrefixLength() {
    return FightRequest.macroPrefixLength;
  }

  public static void setMacroPrefixLength(final int lines) {
    FightRequest.macroPrefixLength = lines;
  }

  private void skipRound() {
    --FightRequest.preparatoryRounds;
    this.nextRound(null);
  }

  private void nextRound(String desiredAction) {
    if (KoLmafia.refusesContinue()) {
      FightRequest.nextAction = "abort";
      return;
    }

    // First round, KoLmafia does not decide the action.
    // Update accordingly.

    if (FightRequest.currentRound == 0) {
      FightRequest.macroPrefixLength = 0;
      FightRequest.nextAction = null;
      return;
    }

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = monster != null ? monster.getName() : "";

    // Always let the user see rare monsters

    if (EncounterManager.isUltrarareMonster(monsterName)) {
      KoLmafia.updateDisplay(MafiaState.ABORT, "You have encountered the " + this.encounter);
      FightRequest.nextAction = "abort";
      return;
    }

    // Desired action overrides any internal logic not related to macros

    if (desiredAction != null && desiredAction.length() > 0) {
      if (CombatActionManager.isMacroAction(desiredAction)) {
        this.handleMacroAction(desiredAction);
        return;
      }

      FightRequest.nextAction = CombatActionManager.getShortCombatOptionName(desiredAction);
    } else {
      // Fight automation is still considered automation.
      // If the player drops below the threshold, then go
      // ahead and halt the battle.

      if (!RecoveryManager.runThresholdChecks()) {
        FightRequest.nextAction = "abort";
        return;
      }

      FightRequest.nextAction = null;

      String macro = Macrofier.macrofy();

      if (macro != null && macro.length() > 0) {
        if (macro.contains("\n") || macro.contains(";")) {
          this.handleMacroAction(macro);
          return;
        }

        FightRequest.nextAction = CombatActionManager.getShortCombatOptionName(macro);
      }

      // Adding machine should override custom combat scripts as well,
      // since it's conditions-driven.

      else if (monsterName.equals("rampaging adding machine")
          && !KoLConstants.activeEffects.contains(FightRequest.BIRDFORM)
          && !FightRequest.waitingForSpecial) {
        this.handleAddingMachine();
      }

      // Hulking Constructs also require special handling

      else if (monsterName.equals("hulking construct")) {
        if (problemFamiliar()) {
          KoLmafia.updateDisplay(
              MafiaState.ABORT,
              "Aborting combat automation due to Familiar that can stop automatic item usage.");
          return;
        }
        this.handleHulkingConstruct();
      }

      if (FightRequest.nextAction == null) {
        String name = MonsterStatusTracker.getLastMonsterName();
        int roundIndex = FightRequest.macroPrefixLength;

        String combatAction = CombatActionManager.getCombatAction(name, roundIndex, false);
        FightRequest.nextAction = CombatActionManager.getShortCombatOptionName(combatAction);
      }
    }

    // If the person wants to use their own script,
    // then this is where it happens.

    if (FightRequest.nextAction.startsWith("consult")) {
      FightRequest.isUsingConsultScript = true;
      String scriptName = FightRequest.nextAction.substring("consult".length()).trim();
      List<File> scriptFiles = KoLmafiaCLI.findScriptFile(scriptName);

      ScriptRuntime consultInterpreter = KoLmafiaASH.getInterpreter(scriptFiles);
      if (consultInterpreter != null) {
        int initialRound = FightRequest.currentRound;

        Object[] parameters = new Object[3];
        parameters[0] = Integer.valueOf(FightRequest.currentRound);
        parameters[1] = MonsterStatusTracker.getLastMonster();
        parameters[2] = FightRequest.lastResponseText;

        File scriptFile = scriptFiles.get(0);
        KoLmafiaASH.logScriptExecution(
            "Starting consult script: ", scriptFile.getName(), consultInterpreter);

        // Since we are automating, let the script execute without interruption
        KoLmafia.forceContinue();

        consultInterpreter.execute("main", parameters, true);
        KoLmafiaASH.logScriptExecution(
            "Finished consult script: ", scriptFile.getName(), consultInterpreter);

        ++FightRequest.macroPrefixLength;

        if (KoLmafia.refusesContinue()) {
          FightRequest.nextAction = "abort";
        } else if (initialRound == FightRequest.currentRound) {
          if (FightRequest.nextAction == null) {
            FightRequest.nextAction = "abort";
          } else if (FightRequest.nextAction.equals(FightRequest.consultScriptThatDidNothing)) {
            FightRequest.nextAction = "abort";
          } else {
            FightRequest.consultScriptThatDidNothing = FightRequest.nextAction;
          }
        }

        // Continue running after the consult script
        this.responseCode = 200;
        return;
      }

      KoLmafia.updateDisplay(MafiaState.ABORT, "Consult script '" + scriptName + "' not found.");
      FightRequest.nextAction = "abort";
      return;
    }

    // Let the de-level action figure out what
    // should be done, and then re-process.

    if (FightRequest.nextAction.startsWith("delevel")) {
      ++FightRequest.macroPrefixLength;
      FightRequest.nextAction = this.getMonsterWeakenAction();
    }

    this.updateCurrentAction();
  }

  private void handleMacroAction(String macro) {
    FightRequest.nextAction = "macro";

    this.addFormField("action", "macro");

    // In case the player continues the script from the relay browser,
    // insert a jump to the next restart point.

    if (macro.contains("#mafiarestart")) {
      String label = "mafiaskip" + macro.length();

      StringUtilities.singleStringReplace(macro, "#mafiarestart", "mark " + label);
      StringUtilities.singleStringReplace(macro, "#mafiaheader", "#mafiaheader\ngoto " + label);
    }

    this.addFormField(
        "macrotext", FightRequest.MACRO_COMPACT_PATTERN.matcher(macro).replaceAll("$1"));
  }

  public static final String getCurrentKey() {
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    return monster == null ? "default" : CombatActionManager.encounterKey(monster.getName());
  }

  private void updateCurrentAction() {
    // We use this to force the fight page to be reloaded
    if (FightRequest.nextAction.startsWith("twiddle")) {
      ++FightRequest.macroPrefixLength;
      this.addFormField("action", "twiddle");
      return;
    }

    if (FightRequest.shouldUseAntidote()) {
      FightRequest.nextAction = String.valueOf(ItemPool.ANTIDOTE);
      ++FightRequest.preparatoryRounds;
    }

    if (FightRequest.nextAction.equals("special")) {
      FightRequest.waitingForSpecial = false;
      String specialAction = FightRequest.getSpecialAction();

      if (specialAction == null) {
        this.skipRound();
        return;
      }

      FightRequest.nextAction = specialAction;
    }

    if (FightRequest.nextAction.equals("abort")) {
      // If the user has chosen to abort combat, flag it.
      --FightRequest.preparatoryRounds;
      return;
    }

    if (FightRequest.nextAction.equals("abort after")) {
      KoLmafia.abortAfter("Aborted by CCS request");
      this.skipRound();
      return;
    }

    if (FightRequest.nextAction.equals("skip")) {
      this.skipRound();
      return;
    }

    // User wants to run away
    if (FightRequest.nextAction.contains("run") && FightRequest.nextAction.contains("away")) {
      Matcher runAwayMatcher =
          CombatActionManager.TRY_TO_RUN_AWAY_PATTERN.matcher(FightRequest.nextAction);

      int runaway = 0;

      if (runAwayMatcher.find()) {
        runaway = StringUtilities.parseInt(runAwayMatcher.group(1));
      }

      FightRequest.nextAction = "runaway";

      if (runaway > FightRequest.freeRunawayChance()) {
        this.skipRound();
        return;
      }

      this.addFormField("action", FightRequest.nextAction);
      return;
    }

    // User wants a regular attack
    if (FightRequest.nextAction.startsWith("attack")) {
      if (KoLCharacter.isJarlsberg()) {
        KoLmafia.updateDisplay(MafiaState.ABORT, "Cannot use 'attack' as Jarlsberg.");
        this.skipRound();
        return;
      }
      FightRequest.nextAction = "attack";
      this.addFormField("action", FightRequest.nextAction);
      return;
    }

    if (KoLConstants.activeEffects.contains(FightRequest.AMNESIA)) {
      if (!MonsterStatusTracker.willUsuallyMiss() && !KoLCharacter.isJarlsberg()) {
        FightRequest.nextAction = "attack";
        this.addFormField("action", FightRequest.nextAction);
        return;
      }

      FightRequest.nextAction = "abort";
      return;
    }

    // Actually steal if the action says to steal

    if (FightRequest.nextAction.contains("steal")
        && !FightRequest.nextAction.contains("stealth")
        && !FightRequest.nextAction.contains("accordion")) {
      if (FightRequest.canStillSteal() && MonsterStatusTracker.shouldSteal()) {
        FightRequest.nextAction = "steal";
        this.addFormField("action", "steal");
        return;
      }

      this.skipRound();
      return;
    }

    // Jiggle chefstaff if the action says to jiggle and we're
    // wielding a chefstaff. Otherwise, skip this action.

    if (FightRequest.nextAction.startsWith("jiggle")) {
      if (!FightRequest.jiggledChefstaff && EquipmentManager.usingChefstaff()) {
        this.addFormField("action", "chefstaff");
        return;
      }

      // You can only jiggle once per round.
      this.skipRound();
      return;
    }

    // Handle DB combos.

    if (FightRequest.nextAction.startsWith("combo ")) {
      String name = FightRequest.nextAction.substring(6);
      int[] combo = DiscoCombatHelper.getCombo(name);
      if (combo == null) {
        KoLmafia.updateDisplay(MafiaState.ABORT, "Invalid combo '" + name + "' requested");
        this.skipRound();
        return;
      }

      // There is a limit on the number of Rave Steals
      String raveSteal = DiscoCombatHelper.COMBOS[DiscoCombatHelper.RAVE_STEAL][0];
      if (DiscoCombatHelper.disambiguateCombo(name).equals(raveSteal)
          && !DiscoCombatHelper.canRaveSteal()) {
        RequestLogger.printLine("rave steal identified");
        this.skipRound();
        return;
      }

      StringBuffer macro = new StringBuffer();

      Macrofier.macroCommon(macro);
      Macrofier.macroCombo(macro, combo);

      this.addFormField("action", "macro");
      this.addFormField("macrotext", macro.toString());

      FightRequest.preparatoryRounds += combo.length - 1;

      return;
    }

    // If the player wants to use an item, make sure he has one
    if (!FightRequest.nextAction.startsWith("skill")) {
      if (KoLConstants.activeEffects.contains(
          FightRequest.BIRDFORM)) { // Can't use items in Birdform
        this.skipRound();
        return;
      }

      int item1, item2;
      boolean funksling = KoLCharacter.hasSkill("Ambidextrous Funkslinging");

      int commaIndex = FightRequest.nextAction.indexOf(",");
      if (commaIndex != -1) {
        item1 = StringUtilities.parseInt(FightRequest.nextAction.substring(0, commaIndex));
        item2 = StringUtilities.parseInt(FightRequest.nextAction.substring(commaIndex + 1));
        if (item2 == -1) {
          // Specifically asking for just one item
          funksling = false;
        }
      } else {
        item1 = StringUtilities.parseInt(FightRequest.nextAction);
        item2 = -1;
      }

      int itemCount = (ItemPool.get(item1)).getCount(KoLConstants.inventory);

      if (itemCount == 0 && item2 != -1) {
        item1 = item2;
        item2 = -1;

        itemCount = (ItemPool.get(item1)).getCount(KoLConstants.inventory);
      }

      if (itemCount == 0) {
        KoLmafia.updateDisplay(
            MafiaState.ABORT, "You don't have enough " + ItemDatabase.getItemName(item1));
        FightRequest.nextAction = "abort";
        return;
      }

      MonsterData monster = MonsterStatusTracker.getLastMonster();
      String monsterName = monster != null ? monster.getName() : "";

      if (item1 == ItemPool.DICTIONARY || item1 == ItemPool.FACSIMILE_DICTIONARY) {
        if (itemCount < 1) {
          KoLmafia.updateDisplay(MafiaState.ABORT, "You don't have a dictionary.");
          FightRequest.nextAction = "abort";
          return;
        }

        if (monsterName.equals("rampaging adding machine")) {
          FightRequest.nextAction = "attack";
          this.addFormField("action", FightRequest.nextAction);
          return;
        }
      }

      this.addFormField("action", "useitem");
      this.addFormField("whichitem", String.valueOf(item1));

      if (!funksling) {
        return;
      }

      if (item2 != -1) {
        itemCount = (ItemPool.get(item2)).getCount(KoLConstants.inventory);

        if (itemCount > 1 || item1 != item2 && itemCount > 0) {
          FightRequest.nextAction += "," + item2;
          this.addFormField("whichitem2", String.valueOf(item2));
        } else {
          KoLmafia.updateDisplay(
              MafiaState.ABORT, "You don't have enough " + ItemDatabase.getItemName(item2));
          FightRequest.nextAction = "abort";
        }

        return;
      }

      if (singleUseCombatItem(item1)) {
        if (KoLConstants.inventory.contains(FightRequest.MERCENARY)) {
          FightRequest.nextAction += "," + FightRequest.MERCENARY_ACTION;
          this.addFormField("whichitem2", String.valueOf(FightRequest.MERCENARY.getItemId()));
        } else if (KoLConstants.inventory.contains(FightRequest.DESTROYER)) {
          FightRequest.nextAction += "," + FightRequest.DESTROYER_ACTION;
          this.addFormField("whichitem2", String.valueOf(FightRequest.DESTROYER.getItemId()));
        } else if (KoLConstants.inventory.contains(FightRequest.LASER)) {
          FightRequest.nextAction += "," + FightRequest.LASER_ACTION;
          this.addFormField("whichitem2", String.valueOf(FightRequest.LASER.getItemId()));
        } else if (KoLConstants.inventory.contains(FightRequest.STOMPER)) {
          FightRequest.nextAction += "," + FightRequest.STOMPER_ACTION;
          this.addFormField("whichitem2", String.valueOf(FightRequest.STOMPER.getItemId()));
        } else if (KoLConstants.inventory.contains(FightRequest.TOOTH)) {
          FightRequest.nextAction += "," + FightRequest.TOOTH_ACTION;
          this.addFormField("whichitem2", String.valueOf(FightRequest.TOOTH.getItemId()));
        } else if (KoLConstants.inventory.contains(FightRequest.SPICES)) {
          FightRequest.nextAction += "," + FightRequest.SPICES_ACTION;
          this.addFormField("whichitem2", String.valueOf(FightRequest.SPICES.getItemId()));
        }
      } else if (itemCount >= 2 && !soloUseCombatItem(item1)) {
        FightRequest.nextAction += "," + FightRequest.nextAction;
        this.addFormField("whichitem2", String.valueOf(item1));
      }

      return;
    }

    // We do not verify that the character actually knows the skill
    // or that it is currently available. It can be complicated:
    // birdform skills are available only in birdform., but some
    // are available only if you've prepped them by eating the
    // appropriate kind of bug.

    // We do ensure that it is a combat skill.

    String skillIdString = FightRequest.nextAction.substring(5);
    int skillId = StringUtilities.parseInt(skillIdString);
    String skillName = SkillDatabase.getSkillName(skillId);

    if (skillName == null || !SkillDatabase.isCombat(skillId)) {
      if (this.isAcceptable(0, 0)) {
        FightRequest.nextAction = "attack";
        this.addFormField("action", FightRequest.nextAction);
        return;
      }

      FightRequest.nextAction = "abort";
      return;
    }

    switch (skillName) {
      case "Transcendent Olfaction":
        // You can't sniff if you are already on the trail.

        // You can't sniff in Bad Moon, even though the skill
        // shows up on the char sheet, unless you've recalled
        // your skills.

        if ((KoLCharacter.inBadMoon() && !KoLCharacter.skillsRecalled())
            || KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.ON_THE_TRAIL))) {
          this.skipRound();
          return;
        }
        break;
      case "Consume Burrowgrub":
        // You can only consume 3 burrowgrubs per day

        if (Preferences.getInteger("burrowgrubSummonsRemaining") <= 0) {
          this.skipRound();
          return;
        }
        break;
      case "Entangling Noodles":
      case "Shadow Noodles":
        // You can only use this skill once per combat
        if (FightRequest.castNoodles) {
          this.skipRound();
          return;
        }
        break;
      case "Club Foot":
        // You can only use this skill once per combat
        if (FightRequest.castClubFoot) {
          this.skipRound();
          return;
        }
        break;
      case "Shell Up":
        // You can only use this skill once per combat
        if (FightRequest.castShellUp) {
          this.skipRound();
          return;
        }
        break;
      case "Accordion Bash":
        // You can only use this skill once per combat
        if (FightRequest.castAccordionBash) {
          this.skipRound();
          return;
        }
        break;
      case "Unleash Terra Cotta Army":
        // You can only use this skill once per combat
        if (FightRequest.castTerracottaArmy) {
          this.skipRound();
          return;
        }
        break;
      case "Paraffin Prism":
        // You can only use this skill once per combat
        if (FightRequest.castParaffinPrism) {
          this.skipRound();
          return;
        }
        break;
      case "Squeeze Stress Ball":
        // You can only use this skill once per combat
        if (FightRequest.squeezedStressBall) {
          this.skipRound();
          return;
        }
        break;
      case "Fire a badly romantic arrow":
        // You can only shoot 1 badly romantic arrow per day

        if (Preferences.getInteger("_badlyRomanticArrows") >= 1
            || KoLCharacter.getEffectiveFamiliar().getId() != FamiliarPool.OBTUSE_ANGEL) {
          this.skipRound();
          return;
        }
        break;
      case "Wink at":
        // You can only shoot 1 badly romantic arrow per day

        if (Preferences.getInteger("_badlyRomanticArrows") >= 1
            || KoLCharacter.getEffectiveFamiliar().getId() != FamiliarPool.REANIMATOR) {
          this.skipRound();
          return;
        }
        break;
      case "Fire a boxing-glove arrow":
        // You can only shoot 5 boxing-glove arrows per day

        if (Preferences.getInteger("_boxingGloveArrows") >= 5
            || KoLCharacter.getEffectiveFamiliar().getId() != FamiliarPool.OBTUSE_ANGEL) {
          this.skipRound();
          return;
        }
        break;
      case "Fire a poison arrow":
        // You can only shoot 10 poison arrows per day

        if (Preferences.getInteger("_poisonArrows") >= 1
            || KoLCharacter.getEffectiveFamiliar().getId() != FamiliarPool.OBTUSE_ANGEL) {
          this.skipRound();
          return;
        }
        break;
      case "Fire a fingertrap arrow":
        // You can only shoot 10 fingertrap arrows per day

        if (Preferences.getInteger("_fingertrapArrows") >= 10
            || KoLCharacter.getEffectiveFamiliar().getId() != FamiliarPool.OBTUSE_ANGEL) {
          this.skipRound();
          return;
        }
        break;
      case "Talk About Politics":
        // You can only use 5 Pantsgiving banishes per day

        if (Preferences.getInteger("_pantsgivingBanish") >= 5
            || !KoLCharacter.hasEquipped(ItemPool.get(ItemPool.PANTSGIVING, 1))) {
          this.skipRound();
          return;
        }
        break;
      case "Release the Boots":
        // You can only release the boots 7 times per day

        if (!FightRequest.canStomp) {
          this.skipRound();
          return;
        }
        break;
      case "Fix Jukebox":
        // You can only fix 3 Jukeboxes per day

        if (Preferences.getInteger("_peteJukeboxFixed") >= 3) {
          this.skipRound();
          return;
        }
        break;
      case "Peel Out":
        // You can only Peel Out 10 times per day, 30 with Racing Slicks
        int max =
            10 + (Preferences.getString("peteMotorbikeTires").equals("Racing Slicks") ? 20 : 0);
        if (Preferences.getInteger("_petePeeledOut") >= max) {
          this.skipRound();
          return;
        }
        break;
      case "Jump Shark":
        // You can only jump the shark 3 times per day

        if (Preferences.getInteger("_peteJumpedShark") >= 3) {
          this.skipRound();
          return;
        }
        break;
      case "Snokebomb":
        // You can only snokebomb 3 times per day

        if (Preferences.getInteger("_snokebombUsed") >= 3) {
          this.skipRound();
          return;
        }
        break;
      case "Shattering Punch":
        if (Preferences.getInteger("_shatteringPunchUsed") >= 3) {
          this.skipRound();
          return;
        }
        break;
      case "Gingerbread Mob Hit":
        if (Preferences.getBoolean("_gingerbreadMobHitUsed")) {
          this.skipRound();
          return;
        }
        break;
      case "Throw Latte on Opponent":
        if (Preferences.getBoolean("_latteBanishUsed")) {
          this.skipRound();
          return;
        }
        break;
      case "Offer Latte to Opponent":
        if (Preferences.getBoolean("_latteCopyUsed")) {
          this.skipRound();
          return;
        }
        break;
      case "Gulp Latte":
        if (Preferences.getBoolean("_latteDrinkUsed")) {
          this.skipRound();
          return;
        }
        break;
      case "Walk Away From Explosion":
        // You can't walk away from explosions whilst bored of them

        if (KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.BORED_WITH_EXPLOSIONS))) {
          this.skipRound();
          return;
        }
        break;
      case "Nuclear Breath":
        // You can only use this skill if you have the Taste the Inferno effect

        if (!KoLConstants.activeEffects.contains(FightRequest.INFERNO)) {
          this.skipRound();
          return;
        }
        break;
      case "Carbohydrate Cudgel":
        // You can only use this skill if you have dry noodles

        if (!KoLConstants.inventory.contains(ItemPool.get(ItemPool.DRY_NOODLES, 1))) {
          this.skipRound();
          return;
        }
        break;
      case "Unload Tommy Gun":
        // You can only use this skill if you have ammunition

        if (!KoLConstants.inventory.contains(ItemPool.get(ItemPool.TOMMY_AMMO, 1))) {
          this.skipRound();
          return;
        }
        break;
      case "Shovel Hot Coal":
        // You can only use this skill if you have hot coal

        if (!KoLConstants.inventory.contains(ItemPool.get(ItemPool.HOT_COAL, 1))) {
          this.skipRound();
          return;
        }
        break;
      case "Lash of the Cobra":
        // You can only use this skill successfully once per Ed combat

        if (Preferences.getBoolean("edUsedLash")) {
          this.skipRound();
          return;
        }
        break;
      case "Curse of Fortune":
        // You can only use this skill if you have ka coins

        if (!KoLConstants.inventory.contains(ItemPool.get(ItemPool.KA_COIN, 1))) {
          this.skipRound();
          return;
        }
        break;
      case "One-Two Punch":
        // You can only use One-Two Punch unarmed

        if (!KoLCharacter.isUnarmed()) {
          this.skipRound();
          return;
        }
        break;
      case "Pistolwhip":
      case "Fan Hammer":
        // You can only use Fan Hammer with a Holstered Pistol
        // You can only use Pistolwhip with a Holstered Pistol

        if (!EquipmentManager.holsteredSixgun()) {
          this.skipRound();
          return;
        }
        break;
      case "Unleash Cowrruption":
        // You can only use Unleash Cowrruption with 30 or more Cowrruption
        if (FightRequest.COWRRUPTION.getCount(KoLConstants.activeEffects) < 30) {
          this.skipRound();
          return;
        }
        break;
      case "Beancannon":
        // You can only use Beancannon with an offhand Can of Beans

        if (Preferences.getInteger("_beanCannonUses") >= 5 || !EquipmentManager.usingCanOfBeans()) {
          this.skipRound();
          return;
        }
        break;
      case "Extract Oil":
        // You can only extract 15 oil a day

        if (Preferences.getInteger("_oilExtracted") > 14) {
          this.skipRound();
          return;
        }
        break;
      case "Long Con":
        // You can only use Long Con 5 times per day

        if (Preferences.getInteger("_longConUsed") >= 5) {
          this.skipRound();
          return;
        }
        break;
      case "Fire the Jokester's Gun":
        // You can only fire the gun once per day per day

        if (Preferences.getBoolean("_firedJokestersGun")) {
          this.skipRound();
          return;
        }
        break;
      case "CHEAT CODE: Replace Enemy":
        // Replace Enemy takes 10% of your daily battery power
        if (EquipmentManager.powerfulGloveUsableBatteryPower() < 10) {
          this.skipRound();
          return;
        }
        break;
      case "CHEAT CODE: Shrink Enemy":
        // Shrink Enemy takes 5% of your daily battery power
        if (EquipmentManager.powerfulGloveUsableBatteryPower() < 5) {
          this.skipRound();
          return;
        }
        break;
      case "Feel Nostalgic":
        if (Preferences.getInteger("_feelNostalgicUsed") >= 3) {
          this.skipRound();
          return;
        }
        break;
      case "Feel Hatred":
        if (Preferences.getInteger("_feelHatredUsed") >= 3) {
          this.skipRound();
          return;
        }
        break;
      case "Feel Pride":
        if (Preferences.getInteger("_feelPrideUsed") >= 3) {
          this.skipRound();
          return;
        }
        break;
      case "Feel Envy":
        if (Preferences.getInteger("_feelEnvyUsed") >= 3) {
          this.skipRound();
          return;
        }
        break;
      case "Feel Superior":
        if (Preferences.getInteger("_feelSuperiorUsed") >= 3) {
          this.skipRound();
          return;
        }
        break;
      case "Fire Extinguisher: Foam 'em Up":
      case "Fire Extinguisher: Blast the Area":
        if (EquipmentManager.fireExtinguisherAvailableFoam() < 5) {
          this.skipRound();
          return;
        }
        break;
      case "Fire Extinguisher: Foam Yourself":
      case "Fire Extinguisher: Polar Vortex":
        if (EquipmentManager.fireExtinguisherAvailableFoam() < 10) {
          this.skipRound();
          return;
        }
        break;
      case "Fire Extinguisher: Zone Specific":
        if (EquipmentManager.fireExtinguisherAvailableFoam() < 20) {
          this.skipRound();
          return;
        }
        break;
    }

    // Skills use MP. Make sure the character has enough.
    if (KoLCharacter.getCurrentMP() < FightRequest.getActionCost()) {
      if (!Preferences.getBoolean("autoManaRestore")) {
        this.skipRound();
        return;
      }

      if (KoLConstants.activeEffects.contains(FightRequest.BIRDFORM)) {
        FightRequest.nextAction = "abort";
        return;
      }

      for (int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i) {
        if (MPRestoreItemList.CONFIGURES[i].isCombatUsable()
            && KoLConstants.inventory.contains(MPRestoreItemList.CONFIGURES[i].getItem())) {
          FightRequest.nextAction =
              String.valueOf(MPRestoreItemList.CONFIGURES[i].getItem().getItemId());

          ++FightRequest.preparatoryRounds;
          this.updateCurrentAction();
          return;
        }
      }

      FightRequest.nextAction = "abort";
      return;
    }

    if (skillName.equals("CLEESH")) {
      if (FightRequest.castCleesh && !KoLCharacter.isJarlsberg()) {
        FightRequest.nextAction = "attack";
        this.addFormField("action", FightRequest.nextAction);
        return;
      }

      FightRequest.castCleesh = true;
    }

    if (FightRequest.isInvalidAttack(FightRequest.nextAction)) {
      FightRequest.nextAction = "abort";
      return;
    }

    this.addFormField("action", "skill");
    this.addFormField("whichskill", skillIdString);
  }

  private static boolean problemFamiliar() {
    return (KoLCharacter.getEffectiveFamiliar().getId() == FamiliarPool.BLACK_CAT
            || KoLCharacter.getEffectiveFamiliar().getId() == FamiliarPool.OAF)
        && !KoLCharacter.hasEquipped(ItemPool.get(ItemPool.TINY_COSTUME_WARDROBE, 1));
  }

  private boolean singleUseCombatItem(int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_SINGLE);
  }

  private boolean soloUseCombatItem(int itemId) {
    return ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_SOLO);
  }

  public static final boolean isInvalidShieldlessAttack(final String action) {
    if (!INVALID_WITHOUT_SHIELD.contains(action.toLowerCase())) {
      return false;
    }

    if (!EquipmentManager.usingShield()) {
      KoLmafia.updateDisplay(MafiaState.ABORT, "This skill is useless without a shield.");
      return true;
    }

    return false;
  }

  public static final boolean isInvalidLocationAttack(final String action) {
    if (!INVALID_OUT_OF_WATER.contains(action.toLowerCase())) {
      return false;
    }

    KoLAdventure location = KoLAdventure.lastVisitedLocation();
    String environment = location != null ? location.getEnvironment() : null;

    if (environment != null && !environment.equals("underwater")) {
      KoLmafia.updateDisplay(MafiaState.ABORT, "This skill is useless out of water.");
      return true;
    }

    return false;
  }

  public static final boolean isInvalidAttack(final String action) {
    return FightRequest.isInvalidShieldlessAttack(action)
        || FightRequest.isInvalidLocationAttack(action);
  }

  public static final boolean containsMacroError(final String str) {
    return str.contains("Macro Abort")
        || str.contains("Macro abort")
        || str.contains("macro abort")
        || str.contains("Could not match item(s) for use")
        || str.contains("Invalid Macro")
        || str.contains("Invalid macro");
  }

  public synchronized void runOnce(final String desiredAction) {
    this.clearDataFields();

    FightRequest.nextAction = null;
    FightRequest.isUsingConsultScript = false;

    if (!KoLmafia.refusesContinue()) {
      combatFilterThatDidNothing = null;
      this.nextRound(desiredAction);
    }

    if (!FightRequest.isUsingConsultScript) {
      if (FightRequest.nextAction != null && !FightRequest.nextAction.equals("abort")) {
        super.run();

        if (containsMacroError(responseText)) {
          FightRequest.nextAction = "abort";
        }

        // If the fight is over, we don't care whether a macro aborted, say.
        if (FightRequest.currentRound == 0) {
          return;
        }
      }
    }

    if (FightRequest.nextAction != null && FightRequest.nextAction.equals("abort")) {
      String message = "You're on your own, partner.";
      if (FightRequest.macroErrorMessage != null) {
        message += " (" + macroErrorMessage + ")";
      }
      KoLmafia.updateDisplay(MafiaState.ABORT, message);
    }
  }

  public static void preFight(boolean fromChoice) {
    FightRequest.fightFollowsChoice = fromChoice;
    FightRequest.currentRound = 0;
    FightRequest.won = false;
    FightRequest.macroPrefixLength = 0;
    FightRequest.nextAction = null;
  }

  @Override
  public void run() {
    this.run(null);
  }

  public synchronized void run(final String redirectLocation) {
    String url = FightRequest.inMultiFight ? "fight.php" : redirectLocation;

    if (url != null) {
      // A multifight starts with am unadorned call to
      // fight.php.
      //
      // A non-multifight starts with a redirect to
      // "ireallymeanit" and KoL needs to follow the redirect
      // before the actual round zero is ready to go.

      this.constructURLString(url);
      FightRequest.preFight(false);
      super.run();

      // Carry on with the rest of the automation
    }

    this.constructURLString("fight.php");

    try {
      FightRequest.isAutomatingFight = true;

      do {
        this.runOnce(null);
      } while (this.responseCode == 200
          && FightRequest.currentRound != 0
          && !KoLmafia.refusesContinue());

      if (this.responseCode == 302) {
        FightRequest.clearInstanceData();
      }

      if (KoLmafia.refusesContinue() && FightRequest.currentRound != 0) {
        this.showInBrowser(true);
      }
    } finally {
      FightRequest.isAutomatingFight = false;
    }
  }

  private static String removeGothy(String text) {
    text =
        text.replaceAll(
            " <i>(?:agony|anguish|blackness|despair|fear|gloom|heart|loneliness|miasma|rain|soul|torment)</i><br>&nbsp;&nbsp;",
            "");
    text =
        text.replaceAll(
            " <i>(?:agony|anguish|blackness|despair|fear|gloom|heart|loneliness|miasma|rain|soul|torment)</i><br>&nbsp;",
            "");
    text =
        text.replaceAll(
            " <i>(?:agony|anguish|blackness|despair|fear|gloom|heart|loneliness|miasma|rain|soul|torment)</i><br>",
            "");
    text =
        text.replaceAll(
            " <i>(?:agony|anguish|blackness|despair|fear|gloom|heart|loneliness|miasma|rain|soul|torment)</i>",
            "");
    return text;
  }

  private static String removeWordReplacements(String text) {
    return text.replaceAll("<i title=\"([A-Za-z]+)\">[A-Za-z]+</i>", "$1");
  }

  public static final boolean processResults(
      final String urlString, final String encounter, String responseText) {
    responseText = FightRequest.removeGothy(responseText);
    responseText = FightRequest.removeWordReplacements(responseText);
    FightRequest.updateCombatData(urlString, encounter, responseText);
    FightRequest.parseCombatItems(responseText);
    FightRequest.parseAvailableCombatSkills(responseText);

    // Now that we have processed the page, generated the decorated HTML
    FightRequest.lastDecoratedResponseText =
        RequestEditorKit.getFeatureRichHTML("fight.php", responseText);

    return FightRequest.shouldRefresh;
  }

  private boolean isAcceptable(final int offenseModifier, final int defenseModifier) {
    if (MonsterStatusTracker.willUsuallyMiss(defenseModifier)
        || MonsterStatusTracker.willUsuallyDodge(offenseModifier)) {
      return false;
    }

    return RecoveryManager.getRestoreCount() == 0;
  }

  private void handleAddingMachine() {
    int action = Preferences.getInteger("addingScrolls");
    // 0: show in browser
    // 1: create goal scrolls only
    // 2: create goal & 668
    // 3: create goal, 31337, 668
    if (action == 0) {
      FightRequest.nextAction = "abort";
      return;
    } else if (FightRequest.desiredScroll != null) {
      this.createAddingScroll(FightRequest.desiredScroll);
    } else if (GoalManager.hasGoal(FightRequest.SCROLL_64735)) {
      this.createAddingScroll(FightRequest.SCROLL_64735);
    } else if (GoalManager.hasGoal(FightRequest.SCROLL_64067)) {
      this.createAddingScroll(FightRequest.SCROLL_64067);
    } else if (GoalManager.hasGoal(FightRequest.SCROLL_31337)) {
      this.createAddingScroll(FightRequest.SCROLL_31337);
    } else if (GoalManager.hasGoal(FightRequest.SCROLL_668)) {
      this.createAddingScroll(FightRequest.SCROLL_668);
    } else if (action >= 3) {
      this.createAddingScroll(FightRequest.SCROLL_31337);
    } else if (action >= 2) {
      this.createAddingScroll(FightRequest.SCROLL_668);
    }
  }

  private boolean createAddingScroll(final AdventureResult scroll) {
    // If the familiar can break automation, skip creation
    if (problemFamiliar()) {
      return false;
    }

    AdventureResult part1 = null;
    AdventureResult part2 = null;

    if (scroll == FightRequest.SCROLL_64735) {
      part2 = FightRequest.SCROLL_64067;
      part1 = FightRequest.SCROLL_668;
    } else if (scroll == FightRequest.SCROLL_64067) {
      if (!GoalManager.hasGoal(FightRequest.SCROLL_64067)
          && KoLConstants.inventory.contains(FightRequest.SCROLL_64067)) {
        return false;
      }

      part1 = FightRequest.SCROLL_30669;
      part2 = FightRequest.SCROLL_33398;
    } else if (scroll == FightRequest.SCROLL_668) {
      part1 = FightRequest.SCROLL_334;
      part2 = FightRequest.SCROLL_334;
    } else if (scroll == FightRequest.SCROLL_31337) {
      part1 = FightRequest.SCROLL_30669;
      part2 = FightRequest.SCROLL_668;
    } else {
      return false;
    }

    if (FightRequest.desiredScroll != null) {
      ++FightRequest.preparatoryRounds;
      FightRequest.nextAction = String.valueOf(part2.getItemId());

      FightRequest.desiredScroll = null;
      return true;
    }

    if (part1 == part2 && part1.getCount(KoLConstants.inventory) < 2) {
      return false;
    }

    if (!KoLConstants.inventory.contains(part1)) {
      return this.createAddingScroll(part1) || this.createAddingScroll(part2);
    }

    if (!KoLConstants.inventory.contains(part2)) {
      return this.createAddingScroll(part2);
    }

    if (!KoLCharacter.hasSkill("Ambidextrous Funkslinging")) {
      ++FightRequest.preparatoryRounds;
      FightRequest.nextAction = String.valueOf(part1.getItemId());

      FightRequest.desiredScroll = scroll;
      return true;
    }

    ++FightRequest.preparatoryRounds;
    FightRequest.nextAction = part1.getItemId() + "," + part2.getItemId();
    return true;
  }

  private void handleHulkingConstruct() {
    if (FightRequest.currentRound > 1) {
      ++FightRequest.preparatoryRounds;
      FightRequest.nextAction = "3155";
      return;
    }

    AdventureResult card1 = ItemPool.get(ItemPool.PUNCHCARD_ATTACK, 1);
    AdventureResult card2 = ItemPool.get(ItemPool.PUNCHCARD_WALL, 1);

    if (!KoLConstants.inventory.contains(card1) || !KoLConstants.inventory.contains(card2)) {
      FightRequest.nextAction = "runaway";
      return;
    }

    ++FightRequest.preparatoryRounds;
    if (!KoLCharacter.hasSkill("Ambidextrous Funkslinging")) {
      FightRequest.nextAction = "3146";
    } else {
      FightRequest.nextAction = "3146,3155";
    }
  }

  private String getMonsterWeakenAction() {
    if (this.isAcceptable(0, 0)) {
      return "attack";
    }

    int desiredSkill = 0;
    boolean isAcceptable = false;

    // Disco Eye-Poke
    if (!isAcceptable && KoLCharacter.hasSkill("Disco Eye-Poke")) {
      desiredSkill = 5003;
      isAcceptable = this.isAcceptable(-1, -1);
    }

    // Disco Dance of Doom
    if (!isAcceptable && KoLCharacter.hasSkill("Disco Dance of Doom")) {
      desiredSkill = 5005;
      isAcceptable = this.isAcceptable(-3, -3);
    }

    // Disco Dance II: Electric Boogaloo
    if (!isAcceptable && KoLCharacter.hasSkill("Disco Dance II: Electric Boogaloo")) {
      desiredSkill = 5008;
      isAcceptable = this.isAcceptable(-5, -5);
    }

    // Tango of Terror
    if (!isAcceptable && KoLCharacter.hasSkill("Tango of Terror")) {
      desiredSkill = 5019;
      isAcceptable = this.isAcceptable(-6, -6);
    }

    // Disco Face Stab
    if (!isAcceptable && KoLCharacter.hasSkill("Disco Face Stab")) {
      desiredSkill = 5012;
      isAcceptable = this.isAcceptable(-7, -7);
    }

    return desiredSkill == 0 ? "attack" : "skill" + desiredSkill;
  }

  private static boolean checkForInitiative(final String responseText) {
    if (FightRequest.pokefam) {
      return false;
    }

    if (FightRequest.isAutomatingFight) {
      String action = Preferences.getString("battleAction");

      if (action.startsWith("custom")) {
        MonsterData monster = MonsterStatusTracker.getLastMonster();
        String monsterName = monster != null ? monster.getName() : "";
        String file =
            Preferences.getBoolean("debugPathnames")
                ? CombatActionManager.getStrategyLookupFile().getAbsolutePath()
                : CombatActionManager.getStrategyLookupName();
        action = file + " [" + CombatActionManager.getBestEncounterKey(monsterName) + "]";
      }

      RequestLogger.printLine("Strategy: " + action);
    }

    if (FightRequest.lastUserId != KoLCharacter.getUserId()) {
      FightRequest.lastUserId = KoLCharacter.getUserId();
      FightRequest.lostInitiativeMessage =
          "Round 0: " + KoLCharacter.getUserName() + " loses initiative!";
      FightRequest.wonInitiativeMessage =
          "Round 0: " + KoLCharacter.getUserName() + " wins initiative!";
    }

    boolean shouldLogAction = Preferences.getBoolean("logBattleAction");
    String limitmode = KoLCharacter.getLimitmode();
    boolean isBatfellow = (limitmode == Limitmode.BATMAN);

    // The response tells you if you won initiative.

    if (!FightRequest.wonInitiative(responseText)) {
      // If you lose initiative, there's nothing very
      // interesting to print to the session log.

      if (shouldLogAction) {
        String message =
            isBatfellow
                ? FightRequest.lostBatfellowInitiativeMessage
                : FightRequest.lostInitiativeMessage;
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      }

      return false;
    }

    // Now that you've won initiative, figure out what actually
    // happened in that first round based on player settings.

    if (shouldLogAction) {
      String message =
          isBatfellow
              ? FightRequest.wonBatfellowInitiativeMessage
              : FightRequest.wonInitiativeMessage;
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    int autoAttackAction = KoLCharacter.getAutoAttackAction();

    // If no default action is made by the player, then the round
    // remains the same.  Simply report winning/losing initiative.
    // Same applies for autoattack macros, we detect their action elsewhere.

    if (autoAttackAction == 0
        || String.valueOf(autoAttackAction).startsWith("99")
        || responseText.contains("<!-- macroaction:")) {
      return false;
    }

    if (autoAttackAction == 1) {
      FightRequest.registerRequest(false, "fight.php?[AA]attack");
    } else if (autoAttackAction == 3) {
      FightRequest.registerRequest(false, "fight.php?[AA]steal");
    } else {
      FightRequest.registerRequest(false, "fight.php?[AA]whichskill=" + autoAttackAction);
    }

    return true;
  }

  public static final void setCurrentEncounter(String encounter) {
    currentEncounter = encounter;
  }

  public static final Pattern ONTURN_PATTERN = Pattern.compile("onturn = (\\d+)");

  private static void synchronizeRoundNumber(final String responseText, final boolean logit) {
    Matcher m = ONTURN_PATTERN.matcher(responseText);
    if (m.find()) {
      int round = StringUtilities.parseInt(m.group(1));
      if (round == FightRequest.currentRound) {
        return;
      }

      if (logit) {
        RequestLogger.printLine(
            "KoLmafia thinks it is round "
                + FightRequest.currentRound
                + " but KoL thinks it is round "
                + round);
      }

      // Synchronize with KoL
      int delta = FightRequest.currentRound - round;
      FightRequest.currentRound = round;
      FightRequest.preparatoryRounds -= delta;
    }
  }

  public static final Pattern ROUND_PATTERN = Pattern.compile("<b>\"Round (\\d+)!\"</b>");
  private static final Pattern CHAMBER_PATTERN = Pattern.compile("chamber <b>#(\\d+)</b>");

  public static final void updateCombatData(
      final String location, String encounter, final String responseText) {
    FightRequest.lastResponseText = responseText;

    // Silly check for silly situation
    if (encounter == AdventureRequest.NOT_IN_A_FIGHT) {
      FightRequest.clearInstanceData();
      FightRequest.inMultiFight = false;
      FightRequest.choiceFollowsFight = false;
      FightRequest.fightFollowsChoice = false;
      return;
    }

    boolean autoAttacked = false;

    if (FightRequest.transformed) {
      // Reparse the encounter, since random modifiers,
      // intergnat, etc. could have changed the name.
      encounter = AdventureRequest.parseCombatEncounter(responseText);
      MonsterData newMonster = AdventureRequest.extractMonster(encounter, responseText);
      MonsterStatusTracker.transformMonster(newMonster);
      FightRequest.transformed = false;
    }

    // If you twiddled, nothing more to do with this round.  We may
    // have reparsed the monster, but the round does not advance.
    if (responseText.contains("You twiddle your thumbs.")) {
      FightRequest.synchronizeRoundNumber(responseText, false);
      FightRequest.shouldRefresh = false;
      return;
    }

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = monster != null ? monster.getName() : "";
    SpecialMonster special = FightRequest.specialMonsterCategory(monsterName);

    if (FightRequest.currentRound == 0) {
      Preferences.setString("_lastCombatStarted", FightRequest.COMBAT_START.format(new Date()));
      int adventure = KoLAdventure.lastAdventureId();

      // Pocket Familiars changes everything
      FightRequest.pokefam = KoLCharacter.inPokefam() && !location.contains("fight.php");

      // Adventuring in the Haiku Dungeon
      // Currently have Haiku State of Mind
      // Acquiring Haiku State of Mind can happen in the middle of a macro
      // combat, so is detected elsewhere.
      FightRequest.haiku =
          adventure == AdventurePool.HAIKU_DUNGEON
              || KoLConstants.activeEffects.contains(FightRequest.haikuEffect);

      // Adventuring in the Suburbs of Dis
      // Currently have Just the Best Anapests
      FightRequest.anapest =
          adventure == AdventurePool.CLUMSINESS_GROVE
              || adventure == AdventurePool.MAELSTROM_OF_LOVERS
              || adventure == AdventurePool.GLACIER_OF_JERKS
              || KoLConstants.activeEffects.contains(FightRequest.anapestEffect);

      // Unleash Your Inner Wolf
      FightRequest.innerWolf =
          KoLAdventure.lastLocationName != null
              && KoLAdventure.lastLocationName.equals("Unleash Your Inner Wolf");

      // Adventuring in the Deep Machine Tunnels
      FightRequest.machineElf = adventure == AdventurePool.DEEP_MACHINE_TUNNELS;

      // Adventuring in the Mer-kin Colosseum
      switch (adventure) {
        case AdventurePool.MERKIN_COLOSSEUM:
          Matcher roundMatcher = FightRequest.ROUND_PATTERN.matcher(responseText);
          if (roundMatcher.find()) {
            int round = StringUtilities.parseInt(roundMatcher.group(1));
            Preferences.setInteger("lastColosseumRoundWon", round - 1);
          }
          break;
        case AdventurePool.THE_DAILY_DUNGEON:
          Matcher chamberMatcher = FightRequest.CHAMBER_PATTERN.matcher(responseText);
          if (chamberMatcher.find()) {
            int round = StringUtilities.parseInt(chamberMatcher.group(1));
            Preferences.setInteger("_lastDailyDungeonRoom", round - 1);
          }
          break;
        case AdventurePool.WARBEAR_FORTRESS_LEVEL_THREE:
          ResultProcessor.processItem(ItemPool.WARBEAR_BADGE, -1);
          break;
      }

      // Wearing any piece of papier equipment really messes up the results
      FightRequest.papier = FightRequest.usingPapierEquipment();

      KoLCharacter.getFamiliar().recognizeCombatUse();

      FightRequest.haveFought = true;

      if (responseText.contains(
          "There is a blinding flash of light, and a chorus of heavenly voices rises in counterpoint to the ominous organ music.")) {
        FightRequest.transmogrifyNemesisWeapon(false);
      }

      if (responseText.contains("stomps in place restlessly")) {
        FightRequest.canStomp = true;
      }

      QuestManager.updateQuestFightStarted(responseText, monsterName);

      // http://kol.coldfront.net/thekolwiki/index.php/Encounter#Encounter_Flowchart (image link
      // there
      // is regularly updated) shows the order is Digitize, Arrow, Enamorang, so check in that order
      if (EncounterManager.isDigitizedEncounter(responseText, true)) {
        EncounterManager.ignoreSpecialMonsters();
        Preferences.increment("_sourceTerminalDigitizeMonsterCount");
        TurnCounter.stopCounting("Digitize Monster");
        if (Preferences.getBoolean("stopForFixedWanderer")) {
          TurnCounter.startCounting(
              10 + 10 * Preferences.getInteger("_sourceTerminalDigitizeMonsterCount"),
              "Digitize Monster type=wander",
              "watch.gif");
        } else {
          TurnCounter.startCounting(
              10 + 10 * Preferences.getInteger("_sourceTerminalDigitizeMonsterCount"),
              "Digitize Monster loc=* type=wander",
              "watch.gif");
        }
      } else if (EncounterManager.isRomanticEncounter(responseText, true)) {
        EncounterManager.ignoreSpecialMonsters();
        Preferences.increment("_romanticFightsLeft", -1);
        TurnCounter.stopCounting("Romantic Monster window begin");
        TurnCounter.stopCounting("Romantic Monster window end");
        if (Preferences.getInteger("_romanticFightsLeft") > 0) {
          TurnCounter.startCounting(15, "Romantic Monster window begin loc=*", "lparen.gif");
          TurnCounter.startCounting(
              25, "Romantic Monster window end loc=* type=wander", "rparen.gif");
        } else {
          Preferences.setString("romanticTarget", "");
        }
      } else if (EncounterManager.isEnamorangEncounter(responseText, true)) {
        EncounterManager.ignoreSpecialMonsters();
        TurnCounter.stopCounting("Enamorang Monster");
        TurnCounter.stopCounting("Enamorang unknown monster window begin");
        TurnCounter.stopCounting("Enamorang unknown monster window end");
        Preferences.setString("enamorangMonster", "");
      } else if (EncounterManager.isSaberForceMonster()) {
        // This is earlier in the chain than the things above, but since
        // there's no message it's easiest to check it after
        Preferences.decrement("_saberForceMonsterCount");
      } else if (EncounterManager.isGregariousEncounter(responseText, true)) {
        EncounterManager.ignoreSpecialMonsters();
        Preferences.decrement("beGregariousFightsLeft", 1, 0);
      }

      // Increment stinky cheese counter
      int stinkyCount = EquipmentManager.getStinkyCheeseLevel();
      if (stinkyCount > 0) {
        Preferences.increment("_stinkyCheeseCount", stinkyCount);
      }
      // Increment Pantsgiving counter
      if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.PANTSGIVING, 1), EquipmentManager.PANTS)) {
        Preferences.increment("_pantsgivingCount", 1);
      }

      // Increment Turtle Blessing counter
      int blessingLevel = KoLCharacter.getBlessingLevel();
      if (blessingLevel > 0 && blessingLevel < 4) {
        Preferences.increment("turtleBlessingTurns", 1);
      } else {
        Preferences.setInteger("turtleBlessingTurns", 0);
      }

      if (special != null) {
        switch (special) {
          case ANCIENT_PROTECTOR_SPIRIT:
            // Update appropriate quest to status 6 if lower.
            if (adventure == AdventurePool.HIDDEN_APARTMENT) {
              if (Preferences.getInteger("hiddenApartmentProgress") < 6) {
                Preferences.setInteger("hiddenApartmentProgress", 6);
                QuestDatabase.setQuestProgress(Quest.CURSES, "step1");
              }
            } else if (adventure == AdventurePool.HIDDEN_HOSPITAL) {
              if (Preferences.getInteger("hiddenHospitalProgress") < 6) {
                Preferences.setInteger("hiddenHospitalProgress", 6);
                QuestDatabase.setQuestProgress(Quest.DOCTOR, "step1");
              }
            } else if (adventure == AdventurePool.HIDDEN_OFFICE) {
              if (Preferences.getInteger("hiddenOfficeProgress") < 6) {
                Preferences.setInteger("hiddenOfficeProgress", 6);
                QuestDatabase.setQuestProgress(Quest.BUSINESS, "step1");
              }
            } else if (adventure == AdventurePool.HIDDEN_BOWLING_ALLEY) {
              if (Preferences.getInteger("hiddenBowlingAlleyProgress") < 6) {
                Preferences.setInteger("hiddenBowlingAlleyProgress", 6);
                QuestDatabase.setQuestProgress(Quest.SPARE, "step1");
              }
            }
            break;

          case PYGMY_JANITOR:
            // If you're meeting these in Park, then they've been relocated
            if (adventure == AdventurePool.HIDDEN_PARK) {
              Preferences.setInteger("relocatePygmyJanitor", KoLCharacter.getAscensions());
            }
            break;

          case PYGMY_WITCH_LAWYER:
            // If you're meeting these in Park, then they've been relocated
            if (adventure == AdventurePool.HIDDEN_PARK) {
              Preferences.setInteger("relocatePygmyLawyer", KoLCharacter.getAscensions());
            }
            break;

            // Correct Crypt Evilness if encountering boss when we think we're at more than 25 evil
          case CONJOINED_ZMOMBIE:
            if (Preferences.getInteger("cyrptAlcoveEvilness") > 25) {
              Preferences.increment(
                  "cyrptTotalEvilness", -Preferences.getInteger("cyrptAlcoveEvilness") + 25);
              Preferences.setInteger("cyrptAlcoveEvilness", 25);
            }
            break;
          case HUGE_GHUOL:
            if (Preferences.getInteger("cyrptCrannyEvilness") > 25) {
              Preferences.increment(
                  "cyrptTotalEvilness", -Preferences.getInteger("cyrptCrannyEvilness") + 25);
              Preferences.setInteger("cyrptCrannyEvilness", 25);
            }
            break;
          case GARGANTULIHC:
            if (Preferences.getInteger("cyrptNicheEvilness") > 25) {
              Preferences.increment(
                  "cyrptTotalEvilness", -Preferences.getInteger("cyrptNicheEvilness") + 25);
              Preferences.setInteger("cyrptNicheEvilness", 25);
            }
            break;
          case GIANT_SKEELTON:
            if (Preferences.getInteger("cyrptNookEvilness") > 25) {
              Preferences.increment(
                  "cyrptTotalEvilness", -Preferences.getInteger("cyrptNookEvilness") + 25);
              Preferences.setInteger("cyrptNookEvilness", 25);
            }
            break;

          case CYRUS_THE_VIRUS:
            Preferences.setInteger("aminoAcidsUsed", 0);
            break;

          case GIANT_OCTOPUS:
            if (KoLConstants.inventory.contains(ItemPool.get(ItemPool.GRAPPLING_HOOK, 1))) {
              ResultProcessor.processItem(ItemPool.GRAPPLING_HOOK, -1);
            }
            break;

          case DAD_SEA_MONKEE:
            DadManager.solve(responseText);
            break;

          case DRIPPY_BAT:
            Preferences.decrement("drippyBatsUnlocked");
            break;

          case PIRANHA_PLANT:
            if (!EncounterManager.ignoreSpecialMonsters) {
              Preferences.increment("_mushroomGardenFights");
            }
            break;

          case TIME_SPINNER_PRANK:
            {
              Matcher m = FightRequest.TIMEPRANK_PATTERN.matcher(responseText);
              if (m.find()) {
                String message = m.group(1);
                StringBuilder buffer = new StringBuilder("Round 0: ");
                buffer.append(FightRequest.currentEncounter);
                buffer.append(" says: \"");
                buffer.append(message);
                buffer.append("\"");
                FightRequest.logText(buffer);
              }
              break;
            }

          case DRIPPY_REVELER:
            if (!EncounterManager.ignoreSpecialMonsters) {
              Preferences.increment("drippingHallAdventuresSinceAscension");
            }
            break;

          case FAMILY_OF_KOBOLDS:
            // Remove 100 D4's from inventory
            ResultProcessor.processItem(ItemPool.D4, -100);
            break;

          case GLITCH_MONSTER:
            // This appears to be NOCOPY.
            Preferences.increment("_glitchMonsterFights", 1);
            break;

          case SAUSAGE_GOBLIN:
            if (!EncounterManager.ignoreSpecialMonsters) {
              Preferences.increment("_sausageFights");
              Preferences.setInteger("_lastSausageMonsterTurn", KoLCharacter.getTurnsPlayed());
            }
            break;

          case HIPSTER:
            if (!EncounterManager.ignoreSpecialMonsters) {
              Preferences.increment("_hipsterAdv", 1);
            }
            break;

          case WITCHESS:
            if (!EncounterManager.ignoreSpecialMonsters) {
              Preferences.increment("_witchessFights", 1);
            }
            break;

          case BEE:
            if (!EncounterManager.ignoreSpecialMonsters && KoLCharacter.inBeecore()) {
              Preferences.setInteger("beeCounter", KoLCharacter.getCurrentRun() + 1);
              TurnCounter.stopCounting("Bee window begin");
              TurnCounter.stopCounting("Bee window end");
              TurnCounter.startCounting(15, "Bee window begin loc=*", "lparen.gif");
              TurnCounter.startCounting(20, "Bee window end loc=* type=wander", "rparen.gif");
            }
            break;

          case GINGERBREAD:
            if (!EncounterManager.ignoreSpecialMonsters) {
              Preferences.increment("_gingerbreadCityTurns");
            }
            break;

          case HOLIDAY:
            if (!EncounterManager.ignoreSpecialMonsters) {
              TurnCounter.stopCounting("Holiday Monster window begin");
              TurnCounter.stopCounting("Holiday Monster window end");
              TurnCounter.startCounting(25, "Holiday Monster window begin loc=*", "lparen.gif");
              TurnCounter.startCounting(
                  35, "Holiday Monster window end loc=* type=wander", "rparen.gif");
            }
            break;

          case TACO_ELF:
            if (!EncounterManager.ignoreSpecialMonsters) {
              TurnCounter.stopCounting("Taco Elf window begin");
              TurnCounter.stopCounting("Taco Elf window end");
              TurnCounter.startCounting(35, "Taco Elf window begin loc=*", "lparen.gif");
              TurnCounter.startCounting(40, "Taco Elf window end loc=*", "rparen.gif");
            }
            break;

          case EVENT:
            if (!EncounterManager.ignoreSpecialMonsters) {
              TurnCounter.stopCounting("Event Monster window begin");
              TurnCounter.stopCounting("Event Monster window end");
              TurnCounter.startCounting(35, "Event Monster window begin loc=*", "lparen.gif");
              TurnCounter.startCounting(40, "Event Monster window end loc=*", "rparen.gif");
            }
            break;

          case NEMESIS:
            if (!EncounterManager.ignoreSpecialMonsters) {
              TurnCounter.stopCounting("Nemesis Assassin window begin");
              TurnCounter.stopCounting("Nemesis Assassin window end");
              TurnCounter.startCounting(35, "Nemesis Assassin window begin loc=*", "lparen.gif");
              TurnCounter.startCounting(
                  50, "Nemesis Assassin window end loc=* type=wander", "rparen.gif");
            }
            break;

          case PORTSCAN:
            if (!EncounterManager.ignoreSpecialMonsters) {
              // Add special text for The Source when known
              if (responseText.contains("government man runs up to you")
                  || KoLCharacter.inTheSource()) {
                TurnCounter.stopCounting("portscan.edu");
              }
            }
            break;

          case RAIN:
            if (!EncounterManager.ignoreSpecialMonsters) {
              TurnCounter.stopCounting("Rain Monster window begin");
              TurnCounter.stopCounting("Rain Monster window end");
              TurnCounter.startCounting(35, "Rain Monster window begin loc=*", "lparen.gif");
              TurnCounter.startCounting(
                  45, "Rain Monster window end loc=* type=wander", "rparen.gif");
            }
            break;

          case WOL:
            if (!EncounterManager.ignoreSpecialMonsters) {
              TurnCounter.stopCounting("WoL Monster window begin");
              TurnCounter.stopCounting("WoL Monster window end");
              TurnCounter.startCounting(15, "WoL Monster window begin loc=*", "lparen.gif");
              TurnCounter.startCounting(
                  20, "WoL Monster window end loc=* type=wander", "rparen.gif");
            }
            break;
        }
      }

      FightRequest.inMultiFight = false;
      FightRequest.waitingForSpecial = FightRequest.waitingForSpecial(monster);

      autoAttacked = FightRequest.checkForInitiative(responseText);
      FightRequest.fightingCopy = EncounterManager.ignoreSpecialMonsters;
      EncounterManager.ignoreSpecialMonsters = false;

      if (KoLCharacter.isPlumber()) {
        KoLCharacter.resetCurrentPP();
      }

      if (KoLCharacter.hasEquipped(ItemPool.MINIATURE_CRYSTAL_BALL, EquipmentManager.FAMILIAR)) {
        String predictedMonster = parseCrystalBall(responseText);
        String zone = KoLAdventure.lastLocationName;

        if (predictedMonster == null) {
          zone = null;
        }

        Preferences.setString(
            "crystalBallMonster", predictedMonster == null ? "" : predictedMonster);
        Preferences.setString("crystalBallLocation", zone == null ? "" : zone);

        AdventureQueueDatabase.enqueue(KoLAdventure.lastVisitedLocation(), predictedMonster);
      }
    }

    // Figure out various things by examining the responseText. Ideally,
    // these could be done while walking the HTML parse tree.

    FightRequest.parseBangPotion(responseText);
    FightRequest.parsePirateInsult(responseText);
    FightRequest.parseGrubUsage(responseText);
    FightRequest.parseLassoUsage(responseText);

    Matcher macroMatcher = FightRequest.MACRO_PATTERN.matcher(responseText);
    if (macroMatcher.find()) {
      FightRequest.registerMacroAction(macroMatcher);
    } else { // no macro results
      // replace with dummy matcher that matches the full page
      macroMatcher = FightRequest.FULLPAGE_PATTERN.matcher(responseText);
      macroMatcher.find();
    }

    // We've started a new round
    ++FightRequest.currentRound;

    // Sanity check: compare our round with what KoL claims it is
    FightRequest.synchronizeRoundNumber(responseText, true);

    // Track monster's start-of-combat attack value
    if (currentRound == 1) {
      FightRequest.startingAttack = monster.getAttack();
    }

    // Assume this response does not warrant a refresh
    FightRequest.shouldRefresh = false;

    // Preprocess results and register new items
    ResultProcessor.processItems(true, responseText, null);

    // Track disco skill sequences
    DiscoCombatHelper.parseFightRound(FightRequest.nextAction, macroMatcher);

    // Clean HTML and process it
    FightRequest.processNormalResults(responseText, macroMatcher);

    // Perform other processing for the final round
    FightRequest.updateRoundData(macroMatcher);

    // Report combat round to spading manager
    SpadingManager.processCombatRound(MonsterStatusTracker.getLastMonsterName(), responseText);

    if (responseText.contains("Macro Abort")
        || responseText.contains("Macro abort")
        || responseText.contains("macro abort")
        || responseText.contains("Could not match item(s) for use")) {
      FightRequest.nextAction = "abort";
    }
  }

  private static boolean waitingForSpecial(MonsterData monster) {
    String monsterName = monster != null ? monster.getName() : "";

    // Really? Just look for "special" in the next 10 actions?
    for (int i = 0; i < 10; ++i) {
      if (CombatActionManager.getShortCombatOptionName(
              CombatActionManager.getCombatAction(monsterName, i, false))
          .equals("special")) {
        return true;
      }
    }

    return false;
  }

  // This performs checks that have to be applied to a single round of
  // combat results, and that aren't (yet) part of the
  // processNormalResults loop.  responseText will be a fragment of the
  // page; anything that needs to check something outside of the round
  // (such as looking at the action menus to see if an item or skill is
  // still available) should use FightRequest.lastResponseText instead.
  private static void updateRoundData(final Matcher macroMatcher) {
    String responseText;
    try {
      responseText = macroMatcher.group();
    } catch (
        IllegalStateException e) { // page structure is botched - should have already been reported
      return;
    }

    // Spend MP and consume items
    FightRequest.payActionCost(responseText);

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = monster != null ? monster.getName() : "";
    SpecialMonster special = FightRequest.specialMonsterCategory(monsterName);

    String limitmode = KoLCharacter.getLimitmode();
    boolean finalRound = macroMatcher.end() == FightRequest.lastResponseText.length();
    boolean won = finalRound && responseText.contains("<!--WINWINWIN-->");
    KoLAdventure location = KoLAdventure.lastVisitedLocation();
    String locationName = (location != null) ? location.getAdventureName() : null;

    // If we won, the fight is over for sure. It might be over
    // anyway. We can detect this in one of two ways: if you have
    // the CAB enabled, there will be no link to the old combat
    // form. Otherwise, a link to fight.php indicates that the
    // fight is continuing.
    // Disguises Delimit always has one link, so check for a second

    Pattern fightPattern = Pattern.compile("action=fight.php");
    Matcher fightMatcher = fightPattern.matcher(responseText);
    int fightCount = 0;
    while (fightMatcher.find()) {
      fightCount++;
    }

    boolean stillInBattle =
        finalRound
            && !won
            && (FightRequest.pokefam
                ? responseText.contains("action=fambattle.php")
                : (limitmode == Limitmode.BATMAN || FightRequest.innerWolf)
                    ? responseText.contains("action=\"fight.php\"")
                    : Preferences.getBoolean("serverAddsCustomCombat")
                        ? responseText.contains("(show old combat form)")
                        : KoLCharacter.inDisguise() ? fightCount > 1 : fightCount > 0);

    if (limitmode == Limitmode.BATMAN || limitmode == Limitmode.SPELUNKY) {
      if (!finalRound) {
        return;
      }

      if (stillInBattle) {
        MonsterStatusTracker.applyManuelStats();
        return;
      }

      if (won) {
        if (limitmode == Limitmode.BATMAN) {
          BatManager.wonFight(monsterName, responseText);
        } else {
          SpelunkyRequest.wonFight(monsterName, responseText);
        }
      }

      FightRequest.clearInstanceData();
      FightRequest.won = won;
      FightRequest.inMultiFight = FightRequest.MULTIFIGHT_PATTERN.matcher(responseText).find();
      FightRequest.choiceFollowsFight =
          FightRequest.FIGHTCHOICE_PATTERN.matcher(responseText).find();

      return;
    }

    // Look for special effects
    FightRequest.updateMonsterHealth(responseText);

    // Look for Mer-kin clues
    DreadScrollManager.handleKillscroll(responseText);
    DreadScrollManager.handleHealscroll(responseText);

    // Check for Disco Momentum
    Matcher DiscoMatcher =
        FightRequest.DISCO_MOMENTUM_PATTERN.matcher(FightRequest.lastResponseText);
    if (DiscoMatcher.find()) {
      KoLCharacter.setDiscoMomentum(StringUtilities.parseInt(DiscoMatcher.group(1)));
    }

    // Parse unusual construct puzzle
    if (special == SpecialMonster.UNUSUAL_CONSTRUCT) {
      UnusualConstructManager.solve(responseText);
    }

    // Check for equipment breakage that can happen at any time.
    if (responseText.contains("Your antique helmet, weakened")) {
      EquipmentManager.breakEquipment(ItemPool.ANTIQUE_HELMET, "Your antique helmet broke.");
    }

    if (responseText.contains("sunders your antique spear")) {
      EquipmentManager.breakEquipment(ItemPool.ANTIQUE_SPEAR, "Your antique spear broke.");
    }

    if (responseText.contains("Your antique shield, weakened")) {
      EquipmentManager.breakEquipment(ItemPool.ANTIQUE_SHIELD, "Your antique shield broke.");
    }

    if (responseText.contains("Your antique greaves, weakened")) {
      EquipmentManager.breakEquipment(ItemPool.ANTIQUE_GREAVES, "Your antique greaves broke.");
    }

    // You try to unlock your cyber-mattock, but the battery's
    // dead.  Since the charger won't be invented for several
    // hundred years, you chuck the useless hunk of plastic as far
    // from you as you can.

    if (responseText.contains("You try to unlock your cyber-mattock")) {
      EquipmentManager.breakEquipment(ItemPool.CYBER_MATTOCK, "Your cyber-mattock broke.");
    }

    // "You sigh and discard the belt in a nearby trash can."
    if (responseText.contains("You sigh and discard the belt")) {
      EquipmentManager.breakEquipment(
          ItemPool.CHEAP_STUDDED_BELT, "Your cheap studded belt broke.");
    }

    // "The adhesive on the fake piercing comes loose and it falls
    // off. Looks like those things weren't meant to withstand as
    // much sweat as your eyebrow is capable of producing."
    if (responseText.contains("The adhesive on the fake piercing comes loose")) {
      EquipmentManager.breakEquipment(
          ItemPool.STICK_ON_EYEBROW_PIERCING, "Your stick-on eyebrow piercing broke.");
    }

    // Your crimbo tree has 987 needles left.
    if (responseText.contains("Your crimbo tree has")) {
      Matcher treeMatcher = FightRequest.DECEASED_TREE_PATTERN.matcher(responseText);
      if (treeMatcher.find()) {
        Preferences.setInteger("garbageTreeCharge", StringUtilities.parseInt(treeMatcher.group(1)));
      }
    }
    // Your crimbo tree is now 100% naked, so you toss it away
    else if (responseText.contains("Your crimbo tree is now 100% naked")) {
      Preferences.setInteger("garbageTreeCharge", 0);
      EquipmentManager.breakEquipment(ItemPool.DECEASED_TREE, "You toss your crimbo tree away.");
    }

    // "The Slime draws back and shudders, as if it's about to sneeze.
    // Then it blasts you with a massive loogie that sticks to your
    // rusty grave robbing shovel, pulls it off of you, and absorbs
    // it back into the mass."

    Matcher m = FightRequest.SLIMED_PATTERN.matcher(responseText);
    if (m.find()) {
      int id = ItemDatabase.getItemId(m.group(1));
      if (id > 0) {
        EquipmentManager.discardEquipment(id);
        KoLmafia.updateDisplay(MafiaState.PENDING, "Your " + m.group(1) + " got slimed.");
      }
    }

    if (responseText.contains("Axel screams, and lets go of your hand")
        || responseText.contains("Axel Ottal wanders off")) {
      EquipmentManager.discardEquipment(ItemPool.SPOOKY_LITTLE_GIRL);
      KoLmafia.updateDisplay(MafiaState.PENDING, "Your spooky little girl ran off.");
    }

    // He flicks his oiled switchblade at you and wrenches your weapon out of your hand.
    // Luckily, it lands in your sack instead of on the grimy sea floor.
    if (responseText.contains("sack instead of on the grimy sea floor")) {
      EquipmentManager.removeEquipment(EquipmentManager.getEquipment(EquipmentManager.WEAPON));
    }

    // The little hellseal gives you an aggrieved look, raises its head, and emits a high-pitched
    // screeching wail.
    // You hear a loud growling noise somewhere nearby. Uh-oh.
    if (responseText.contains("high-pitched screeching wail")) {
      Preferences.increment("_sealScreeches", 1, 10, false);
    }

    // "[slimeling] leaps on your opponent, sliming it for XX damage.  It's inspiring!"
    if (responseText.contains("leaps on your opponent")) {
      float fullness = Math.max(Preferences.getFloat("slimelingFullness") - 1.0F, 0.0F);
      Preferences.setFloat("slimelingFullness", fullness);
    }

    // "As you're trying to get away, you sink in the silty muck on
    // the sea floor. You manage to get yourself unmired, but your
    // greaves seem to have gotten instantly rusty in the process..."
    if (responseText.contains("have gotten instantly rusty")) {
      EquipmentManager.discardEquipment(ItemPool.ANTIQUE_GREAVES);
      KoLmafia.updateDisplay(MafiaState.PENDING, "Your antique greaves got rusted.");
    }
    // You walloped your enemy so thoroughly, thanks to your weighty thumb ring, that you actually
    // knocked them
    // back into last week.... guess this current fight didn't take any time!
    if (responseText.contains("thanks to your weighty thumb ring")) {
      Preferences.increment("_mafiaThumbRingAdvs", 1);
      RequestLogger.printLine("Your weighty thumb ring walloped.");
      RequestLogger.updateSessionLog("Your weighty thumb ring walloped.");
    }

    // You're really sharpening the old saw.  Looks like you've done 1 out of 14!
    if (responseText.contains("You're really sharpening the old saw.")) {
      Matcher sawMatcher = FightRequest.SHARPEN_SAW_PATTERN.matcher(responseText);
      if (sawMatcher.find()) {
        Preferences.setString("_newYouQuestSharpensDone", sawMatcher.group(1));
        Preferences.setString("_newYouQuestSharpensToDo", sawMatcher.group(2));
        String message = "You're really sharpening the old saw.  " + sawMatcher.group(0);
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      }
    }
    // You did it!  Your saw is so sharp!
    else if (responseText.contains("Your saw is so sharp!")) {
      Preferences.setString("_newYouQuestMonster", "");
      Preferences.setString("_newYouQuestSkill", "");
      Preferences.setInteger("_newYouQuestSharpensDone", 0);
      Preferences.setInteger("_newYouQuestSharpensToDo", 0);
      Preferences.setBoolean("_newYouQuestCompleted", true);
      QuestDatabase.setQuestProgress(Quest.NEW_YOU, QuestDatabase.UNSTARTED);
      String message = "You did it!  Your saw is so sharp!";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    if (responseText.contains("into last week. It saves you some time, because you already beat")) {
      Preferences.increment("_vmaskAdv", 1);
    }

    // Your Detective Skull's eyes glow yellow and it murmurs &quot;The dame said... That dame...
    // She...
    // She said... <font color=yellow>sword</font>...&quot;
    if (responseText.contains("Detective Skull's eyes glow yellow")) {
      Matcher yellowWordMatcher = FightRequest.YELLOW_WORD_PATTERN.matcher(responseText);
      if (yellowWordMatcher.find()) {
        String yellowWord = yellowWordMatcher.group(1);
        String message =
            "Detective Skull Yellow Word found: "
                + yellowWord
                + " in clan "
                + ClanManager.getClanName(false)
                + ".";
        RequestLogger.printLine("<font color=\"blue\">" + message + "</font>");
        RequestLogger.updateSessionLog(message);
      }
    }

    // If they're not shooting you, they're ordering drinks that don't exist,
    // like <font color=blue>spinach margaritas</font>.  can you believe it?
    if (responseText.contains("ordering drinks that don't exist")) {
      Matcher blueWordMatcher = FightRequest.BLUE_WORD_PATTERN.matcher(responseText);
      if (blueWordMatcher.find()) {
        String blueWord = blueWordMatcher.group(1);
        String message =
            "Copperhead Bartender Blue Word found: "
                + blueWord
                + " in clan "
                + ClanManager.getClanName(false)
                + ".";
        RequestLogger.printLine("<font color=\"blue\">" + message + "</font>");
        RequestLogger.updateSessionLog(message);
      }
    }

    if (KoLCharacter.hasEquipped(ItemPool.BAG_O_TRICKS, EquipmentManager.OFFHAND)) {
      if (responseText.contains("You reach into the bag and pull out ")) {
        Preferences.increment("_bagOTricksBuffs");
        Preferences.setInteger("bagOTricksCharges", 0);
      } else if (responseText.contains("The Bag o' Tricks")) {
        if (responseText.contains("The Bag o' Tricks suddenly feels a little heavier.")) {
          Preferences.setInteger("bagOTricksCharges", 1);
        } else if (responseText.contains(
            "The Bag o' Tricks begins to wriggle around in your hand.")) {
          Preferences.setInteger("bagOTricksCharges", 2);
        } else if (responseText.contains("The Bag o' Tricks begins squirming around more urgently.")
            || responseText.contains(
                "The Bag o' Tricks continues to wriggle around in your hand.")) {
          Preferences.setInteger("bagOTricksCharges", 3);
        }
      }
    }

    FamiliarData familiar = KoLCharacter.getEffectiveFamiliar();
    int familiarId = familiar.getId();

    switch (familiarId) {
      case FamiliarPool.BOOTS:
        // <name> rubs its soles together, then stomps in place
        // restlessly. Clearly, the violence it's done so far is
        // only making it ache for some quality stomping.
        if (responseText.contains("making it ache for some quality stomping")) {
          Preferences.setBoolean("bootsCharged", true);
        }
        break;

      case FamiliarPool.NANORHINO:
        {
          int currentCharge = Preferences.getInteger("_nanorhinoCharge");
          // Did a skill use trigger a buff ?
          // We cannot tell in Haiku/Anapest, so if we think it's fully charged but the image
          // doesn't show that, assume buff gained.
          Matcher NanorhinoBuffMatcher = FightRequest.NANORHINO_BUFF_PATTERN.matcher(responseText);
          if (NanorhinoBuffMatcher.find()
              || ((FightRequest.haiku || FightRequest.anapest)
                  && !KoLCharacter.getFamiliarImage().equals("nanorhinoc.gif")
                  && currentCharge == 100)) {
            Preferences.setInteger("_nanorhinoCharge", 0);
          }
          // Is it charged but we think it isn't?
          else if (KoLCharacter.getFamiliarImage().equals("nanorhinoc.gif")
              && currentCharge != 100) {
            Preferences.setInteger("_nanorhinoCharge", 100);
          }
          int nanorhinoCharge = Preferences.getInteger("_nanorhinoCharge");
          familiar.setCharges(nanorhinoCharge);
          break;
        }

      case FamiliarPool.FIST_TURKEY:
        {
          if (responseText.contains("challenges you to a push-up contest, which he wins")
              || responseText.contains("gives you a fist-bump and a chest-bump")
              || responseText.contains("It's quite the workout")
              || responseText.contains("challenges you to a flex-off")
              || responseText.contains(
                  "folds into a fist and armwrestles you to help you build bulk")) {
            Preferences.increment("_turkeyMuscle");
          } else if (responseText.contains("have a profound and mystical dream")
              || responseText.contains("accidentally scratched a deeply mystical rune")
              || responseText.contains("the stars reveal mystic secrets")
              || responseText.contains("saying something incredibly profound")
              || responseText.contains("looks profoundly supernatural")) {
            Preferences.increment("_turkeyMyst");
          } else if (responseText.contains("inspiring you to work on your own flexing")
              || responseText.contains("teaches you some suave new obscene gestures")
              || responseText.contains("teaches you a cool new fist-bump")
              || responseText.contains("teaches you a surprisingly cool turkey-in-the-straw dance")
              || responseText.contains("helps you work on your intimidating stare")) {
            Preferences.increment("_turkeyMoxie");
          }
        }
    }

    int blindIndex = responseText.indexOf("... something.</div>");
    while (blindIndex != -1) {
      RequestLogger.printLine("You acquire... something.");
      if (Preferences.getBoolean("logAcquiredItems")) {
        RequestLogger.updateSessionLog("You acquire... something.");
      }

      blindIndex = responseText.indexOf("... something.</div>", blindIndex + 1);
    }

    switch (KoLAdventure.lastAdventureId()) {
      case AdventurePool.FRAT_UNIFORM_BATTLEFIELD:
      case AdventurePool.HIPPY_UNIFORM_BATTLEFIELD:
      case AdventurePool.EXPLOADED_BATTLEFIELD:
        IslandManager.handleBattlefield(responseText);
        break;

      case AdventurePool.HOBOPOLIS_TOWN_SQUARE:
        HobopolisDecorator.handleTownSquare(responseText);
        break;
    }

    // Reset round information if the battle is complete.
    if (!finalRound) {
      return;
    }

    // If this was an item-generated monster, reset
    KoLAdventure.setNextAdventure(KoLAdventure.lastVisitedLocation);

    if (stillInBattle) {
      // The fight is not over, none of the stuff below needs to be checked
      MonsterStatusTracker.applyManuelStats();
      return;
    }

    if (responseText.contains("Your sugar chapeau slides")) {
      EquipmentManager.breakEquipment(ItemPool.SUGAR_CHAPEAU, "Your sugar chapeau shattered.");
    }

    if (responseText.contains("your sugar shank handle")) {
      EquipmentManager.breakEquipment(ItemPool.SUGAR_SHANK, "Your sugar shank shattered.");
    }

    if (responseText.contains("drop something as sticky as the sugar shield")) {
      EquipmentManager.breakEquipment(ItemPool.SUGAR_SHIELD, "Your sugar shield shattered.");
    }

    if (responseText.contains("Your sugar shillelagh absorbs the shock")) {
      EquipmentManager.breakEquipment(
          ItemPool.SUGAR_SHILLELAGH, "Your sugar shillelagh shattered.");
    }

    if (responseText.contains("Your sugar shirt falls apart")) {
      EquipmentManager.breakEquipment(ItemPool.SUGAR_SHIRT, "Your sugar shirt shattered.");
    }

    if (responseText.contains("Your sugar shotgun falls apart")) {
      EquipmentManager.breakEquipment(ItemPool.SUGAR_SHOTGUN, "Your sugar shotgun shattered.");
    }

    if (responseText.contains("Your sugar shorts crack")) {
      EquipmentManager.breakEquipment(ItemPool.SUGAR_SHORTS, "Your sugar shorts shattered.");
    }

    // Your crimbo tree has 987 needles left.
    if (responseText.contains("Your crimbo tree has")) {
      Matcher treeMatcher = FightRequest.DECEASED_TREE_PATTERN.matcher(responseText);
      if (treeMatcher.find()) {
        Preferences.setInteger("garbageTreeCharge", StringUtilities.parseInt(treeMatcher.group(1)));
      }
    }

    // The champagne is flowing and the party is going wild with the 9 ounces of champagne left in
    // your broken bottle.
    if (responseText.contains("champagne is flowing and the party is going wild")) {
      Matcher champagneMatcher = FightRequest.BROKEN_CHAMPAGNE_PATTERN.matcher(responseText);
      if (champagneMatcher.find()) {
        Preferences.setInteger(
            "garbageChampagneCharge", StringUtilities.parseInt(champagneMatcher.group(1)));
      }
    }
    // The last drop of your party champagne dripped out during this fight, so you toss the bottle
    // away.
    else if (responseText.contains("last drop of your party champagne dripped out")) {
      Preferences.setInteger("garbageChampagneCharge", 0);
      EquipmentManager.breakEquipment(
          ItemPool.BROKEN_CHAMPAGNE, "You toss away the broken champagne bottle.");
    }

    // You read a useful bit of information off your shirt and improve your rate of knowledge gain.
    // Looks like there are 36 more useful scraps.
    if (responseText.contains("read a useful bit of information off your shirt")) {
      Matcher garbageShirtMatcher = FightRequest.GARBAGE_SHIRT_PATTERN.matcher(responseText);
      if (garbageShirtMatcher.find()) {
        Preferences.setInteger(
            "garbageShirtCharge", StringUtilities.parseInt(garbageShirtMatcher.group(1)));
      }
    }
    // You rip the last bit of usefully informative garbage off your shirt, and it falls to scraps
    else if (responseText.contains("last bit of usefully informative garbage off your shirt")) {
      Preferences.setInteger("garbageShirtCharge", 0);
      EquipmentManager.breakEquipment(
          ItemPool.MAKESHIFT_GARBAGE_SHIRT, "Your makeshirt garbage shirt falls apart.");
    }

    // You hear a whirring as your unicorn horn begins to inflate. Yeah!
    if (responseText.contains("your unicorn horn begins to inflate")) {
      Preferences.setInteger("unicornHornInflation", 5);
    }
    // Your unicorn horn becomes slightly more inflated as a result of your increasing confidence
    // and pride. Yay!
    else if (responseText.contains("Your unicorn horn becomes slightly more inflated")) {
      Preferences.increment("unicornHornInflation", 5);
    }
    // Your unicorn horn squeaks as it fills to capacity. You feel great!
    else if (responseText.contains("Your unicorn horn squeaks")) {
      Preferences.setInteger("unicornHornInflation", 100);
    }
    // Your unicorn horn shrivels in shame.
    else if (responseText.contains("Your unicorn horn shrivels")) {
      Preferences.setInteger("unicornHornInflation", 0);
    }

    // The Great Wolf of the Air emits an ear-splitting final
    // howl. Your necklace shatters like a champagne flute in a
    // Memorex comercial[sic].
    if (responseText.contains("Your necklace shatters")) {
      EquipmentManager.discardEquipment(ItemPool.MOON_AMBER_NECKLACE);
    }

    // You look down and notice that your shawl got ripped to
    // shreds during the fight. Dangit.
    if (responseText.contains("your shawl got ripped to shreds")) {
      EquipmentManager.discardEquipment(ItemPool.GHOST_SHAWL);
    }

    // As he fades away, Mayor Ghost clutches at your badge, which fades away with him. Rats.
    if (responseText.contains("clutches at your badge")) {
      EquipmentManager.discardEquipment(ItemPool.AUDITORS_BADGE);
    }

    // With a final lurch, one of the zombies hurls a bag of weed
    // killer at your skirt, dissolving it instantly.
    if (responseText.contains("hurls a bag of weed killer")) {
      EquipmentManager.discardEquipment(ItemPool.WEEDY_SKIRT);
    }

    // You fall to the ground, overcome by your wounds.  As you lose consciousness,
    // the last thing you see is your first-ait kit opening of its own accord.
    if (responseText.contains("first-ait kit opening of its own accord")) {
      EquipmentManager.discardEquipment(ItemPool.FIRST_AID_POUCH);
    }

    // A bit of flaming paper drifts into your unnamed cocktail and sets it ablaze. Whoah!
    if (responseText.contains("flaming paper drifts into your unnamed cocktail")) {
      ResultProcessor.processItem(ItemPool.UNNAMED_COCKTAIL, -1);
    }

    if (responseText.contains("You wore out your weapon cozy...")) {
      // Cozy weapons are two-handed, so they are necessarily in the weapon slot
      int cozyId = EquipmentManager.getEquipment(EquipmentManager.WEAPON).getItemId();
      EquipmentManager.breakEquipment(cozyId, "Your cozy wore out.");
    }

    // You hurl your entire collection of buttons at it, dealing X damage.
    // You manage to find and recover all but Y of the buttons.
    // If it's the last round, we can't correct from combat drop down, so we'll think only one was
    // used
    Matcher redButtonMatcher =
        FightRequest.RED_BUTTON_PATTERN.matcher(FightRequest.lastResponseText);
    if (redButtonMatcher.find()) {
      ResultProcessor.processItem(
          ItemPool.RED_BUTTON, 1 - StringUtilities.parseInt(redButtonMatcher.group(1)));
    }

    // The turtle appears to suffer some kind of mental breakdown
    // -- it collapses to the ground, sobbing. You help it to its
    // feet and escort it out of the compound.
    //
    // The turtle blinks at you with gratitude for freeing it from
    // its brainwashing, and trudges off over the horizon.
    // ...Eventually.
    if (responseText.contains("freeing it from its brainwashing")) {
      int free = Preferences.increment("guardTurtlesFreed");
      String message = "Freed guard turtle #" + free;
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    } else if (responseText.contains("some kind of mental breakdown")) {
      int free = Preferences.increment("frenchGuardTurtlesFreed");
      String message = "Freed French guard turtle #" + free;
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    if (responseText.contains(
        "your Epic Weapon reverts to its original form in a puff of failure")) {
      FightRequest.transmogrifyNemesisWeapon(true);
    }

    if (responseText.contains("acquire a bounty item:")) {
      BountyHunterHunterRequest.parseFight(monsterName, locationName, responseText);
    }
    // Check for bounty item not dropping from a monster
    // that is known to drop the item.
    else {
      String easyBountyString = Preferences.getString("currentEasyBountyItem");
      int index = easyBountyString.indexOf(":");
      if (index != -1) {
        String easyBountyItemName = easyBountyString.substring(0, index);
        String easyBountyMonsterName = BountyDatabase.getMonster(easyBountyItemName);

        if (monsterName.equals(easyBountyMonsterName)
            && !responseText.contains(easyBountyItemName)
            && !problemFamiliar()) {
          KoLmafia.updateDisplay(
              MafiaState.PENDING, "Easy bounty item failed to drop from expected monster.");
        }
      }

      String hardBountyString = Preferences.getString("currentHardBountyItem");
      index = hardBountyString.indexOf(":");
      if (index != -1) {
        String hardBountyItemName = hardBountyString.substring(0, index);
        String hardBountyMonsterName = BountyDatabase.getMonster(hardBountyItemName);

        if (monsterName.equals(hardBountyMonsterName)
            && !responseText.contains(hardBountyItemName)
            && !problemFamiliar()) {
          KoLmafia.updateDisplay(
              MafiaState.PENDING, "Hard bounty item failed to drop from expected monster.");
        }
      }

      String specialBountyString = Preferences.getString("currentSpecialBountyItem");
      index = specialBountyString.indexOf(":");
      if (index != -1) {
        String specialBountyItemName = specialBountyString.substring(0, index);
        String specialBountyMonsterName = BountyDatabase.getMonster(specialBountyItemName);

        if (monsterName.equals("Sorrowful Hickory")
            && specialBountyItemName.equals("hickory daiquiri")) {
          // There is no bounty text for hickory daiquiri, so assume success instead
          int bountyCount = StringUtilities.parseInt(specialBountyString.substring(index + 1));
          if (bountyCount < 6) {
            bountyCount++;
            Preferences.setString("currentSpecialBountyItem", "hickory daiquiri:" + bountyCount);
            String updateMessage = "You acquire a bounty item: hickory daiquiri";
            AdventureResult result = AdventureResult.tallyItem("hickory daiquiri", false);
            AdventureResult.addResultToList(KoLConstants.tally, result);
            RequestLogger.updateSessionLog(updateMessage);
            KoLmafia.updateDisplay(updateMessage);
          }
        } else if (monsterName.equals(specialBountyMonsterName)
            && !responseText.contains(specialBountyItemName)
            && !problemFamiliar()) {
          KoLmafia.updateDisplay(
              MafiaState.PENDING, "Special bounty item failed to drop from expected monster.");
        }
      }
    }

    // Check if a lecture on relativity is about to give you a copy
    // of the monster you just fought.
    if (responseText.contains("STEP INTO FOLD IN SPACETIME")) {
      Preferences.setBoolean("_relativityMonster", true);
    }

    // Check for runaways. Only a free runaway decreases chance
    if ((responseText.contains("shimmers as you quickly float away")
            || responseText.contains("your pants suddenly activate"))
        && !KoLCharacter.inBigcore()) {
      Preferences.increment("_navelRunaways", 1);
    } else if ((responseText.contains("his back, and flooms away")
            || responseText.contains("speed your escape.  Thanks"))
        && !KoLCharacter.inBigcore()) {
      Preferences.increment("_banderRunaways", 1);
    }

    // You hug him with your filthy rotting arms.
    if (responseText.contains("with your filthy rotting arms.")) {
      Preferences.increment("_bearHugs", 1);
    }

    if (responseText.contains("undefined constant itemprocess")) {
      Preferences.setBoolean("_softwareGlitchTurnReceived", true);
    }

    // Check for worn-out stickers
    int count = 0;
    m = WORN_STICKER_PATTERN.matcher(responseText);
    while (m.find()) {
      ++count;
    }
    if (count > 0) {
      KoLmafia.updateDisplay(
          (count == 1 ? "A sticker" : count + " stickers") + " fell off your weapon.");
      EquipmentManager.stickersExpired(count);
    }

    // Check for ballroom song hint
    m = BALLROOM_SONG_PATTERN.matcher(responseText);
    if (m.find()) {
      Preferences.setInteger("lastQuartetAscension", KoLCharacter.getAscensions());
      Preferences.setInteger("lastQuartetRequest", m.start(1) != -1 ? 1 : m.start(2) != -1 ? 2 : 3);
    }

    // Check for special familiar actions

    // Check for weapon-specific cases
    if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.LEAFBLOWER, 1))) {
      Preferences.increment("_leafblowerML", 1, 25, false);
    }

    // Cancel any combat modifiers
    Modifiers.overrideModifier("Generated:fightMods", null);

    if (KoLCharacter.isSauceror()) {
      // Check for Soulsauce gain
      Matcher SoulsauceMatcher =
          FightRequest.SOULSAUCE_PATTERN.matcher(FightRequest.lastResponseText);
      if (SoulsauceMatcher.find()) {
        String gainSoulsauce = SoulsauceMatcher.group(1);
        KoLCharacter.incrementSoulsauce(StringUtilities.parseInt(gainSoulsauce));
        String updateMessage = "You gain " + gainSoulsauce + " Soulsauce";
        RequestLogger.updateSessionLog(updateMessage);
        KoLmafia.updateDisplay(updateMessage);
      }
    }

    // Lose Disco Momentum
    KoLCharacter.resetDiscoMomentum();

    // "You pull out your personal massager and use it to work the
    // kinks out of your neck and your back. You stop there,
    // though, as nothing below that point is feeling particularly
    // kinky. Unfortunately, it looks like the batteries in the
    // thing were only good for that one use."

    if (responseText.contains("You pull out your personal massager")) {
      ResultProcessor.processItem(ItemPool.PERSONAL_MASSAGER, -1);
      KoLConstants.activeEffects.remove(KoLAdventure.BEATEN_UP);
    }

    // You groan and loosen your overtaxed belt.
    // Y'know, you're full, but you could probably make room for <i>one more thing</i>...
    if (responseText.contains("could probably make room for <i>one more thing</i>")) {
      Preferences.increment("_pantsgivingFullness");
      String updateMessage =
          "Pantsgiving increases max fullness by one to " + KoLCharacter.getFullnessLimit() + ".";
      RequestLogger.updateSessionLog(updateMessage);
      KoLmafia.updateDisplay(updateMessage);
    }

    // Check for Latte unlocks
    if (KoLCharacter.hasEquipped(ItemPool.LATTE_MUG, EquipmentManager.OFFHAND)) {
      LatteRequest.parseFight(locationName, responseText);
    }

    AdventureSpentDatabase.addTurn(KoLAdventure.lastLocationName);

    int adventure = KoLAdventure.lastAdventureId();

    if (KOLHSRequest.isKOLHSLocation(adventure)) {
      Preferences.increment("_kolhsAdventures", 1);
    }

    if (adventure == AdventurePool.YE_OLDE_MEDIEVALE_VILLAGEE) {
      Preferences.increment("rumpelstiltskinTurnsUsed", 1);
    }

    if (adventure == AdventurePool.DEEP_MACHINE_TUNNELS) {
      Preferences.decrement("encountersUntilDMTChoice");
    }

    if (adventure == AdventurePool.NEVERENDING_PARTY) {
      Preferences.decrement("encountersUntilNEPChoice", 1, 0);
    }

    if (monsterName.equals("unusual construct")) {
      ResultProcessor.removeItem(ItemPool.STRANGE_DISC_WHITE);
      ResultProcessor.removeItem(ItemPool.STRANGE_DISC_BLACK);
      ResultProcessor.removeItem(ItemPool.STRANGE_DISC_RED);
      ResultProcessor.removeItem(ItemPool.STRANGE_DISC_GREEN);
      ResultProcessor.removeItem(ItemPool.STRANGE_DISC_BLUE);
      ResultProcessor.removeItem(ItemPool.STRANGE_DISC_YELLOW);
    }

    if (KoLCharacter.inTheSource()) {
      Matcher intervalMatcher = FightRequest.SOURCE_INTERVAL_PATTERN.matcher(responseText);
      if (intervalMatcher.find()) {
        Preferences.setInteger(
            "sourceInterval", (int) (0.8 * StringUtilities.parseInt(intervalMatcher.group(1))));
      } else {
        Preferences.setInteger("sourceInterval", 0);
      }
    }

    // A monster only gets added to your nostalgic/backup camera buffer if the fight is "completed"
    if (monster != null && !monster.isNoCopy()) {
      Preferences.setString("lastCopyableMonster", monsterName);
    }

    if (!won) {
      if (responseText.contains("FREEFREEFREE")) {
        String updateMessage = "This combat did not cost a turn";
        RequestLogger.updateSessionLog(updateMessage);
        KoLmafia.updateDisplay(updateMessage);
      }
      QuestManager.updateQuestFightLost(responseText, monsterName);
    } else {
      if (responseText.contains("monstermanuel.gif")) {
        GoalManager.updateProgress(GoalManager.GOAL_FACTOID);
        MonsterManuelManager.reset(monster);
      }

      KoLCharacter.getFamiliar().addCombatExperience(responseText);
      EdServantData.currentServant().addCombatExperience(responseText);

      switch (familiar.getId()) {
        case FamiliarPool.RIFTLET:
          if (responseText.contains("shimmers briefly, and you feel it getting earlier.")) {
            Preferences.increment("_riftletAdv", 1);
          }
          break;

        case FamiliarPool.REAGNIMATED_GNOME:
          if (responseText.contains("You gain 1 Adventure")) {
            Preferences.increment("_gnomeAdv", 1);
          }
          break;

        case FamiliarPool.HARE:
          // <name> pulls an oversized pocketwatch out of his
          // waistcoat and winds it. "Two days slow, that's what
          // it is," he says.
          if (responseText.contains("oversized pocketwatch")) {
            Preferences.increment("extraRolloverAdventures", 1);
            Preferences.increment("_hareAdv", 1);
            Preferences.setInteger("_hareCharge", 0);
          } else {
            Preferences.increment("_hareCharge", 1);
          }
          int hareCharge = Preferences.getInteger("_hareCharge");
          familiar.setCharges(hareCharge);
          break;

        case FamiliarPool.GIBBERER:
          // <name> mutters dark secrets under his breath, and
          // you feel time slow down.
          KoLAdventure lastLocation = KoLAdventure.lastVisitedLocation();
          boolean underwater =
              lastLocation != null && lastLocation.getEnvironment().equals("underwater");
          Preferences.increment("_gibbererCharge", underwater ? 2 : 1, 15, true);
          if (responseText.contains("you feel time slow down")) {
            Preferences.increment("extraRolloverAdventures", 1);
            Preferences.increment("_gibbererAdv", 1);
            // Normally the updating below is wasted, but it allows things
            // to get in sync if progress is missed for some reason
            if (underwater) {
              Preferences.setInteger(
                  "_gibbererCharge", Math.min(1, Preferences.getInteger("_gibbererCharge")));
            } else {
              Preferences.setInteger("_gibbererCharge", 0);
            }
          }
          int gibbererCharge = Preferences.getInteger("_gibbererCharge");
          familiar.setCharges(gibbererCharge);
          break;

        case FamiliarPool.STOCKING_MIMIC:
          // <name> reaches deep inside himself and pulls out a
          // big bag of candy. Cool!
          if (responseText.contains("pulls out a big bag of candy")) {
            AdventureResult item = ItemPool.get(ItemPool.BAG_OF_MANY_CONFECTIONS, 1);
            // The Stocking Mimic will do this once a day
            Preferences.setBoolean("_bagOfCandy", true);
            // Add bag of many confections to inventory
            ResultProcessor.processItem(ItemPool.BAG_OF_MANY_CONFECTIONS, 1);
            // Equip familiar with it
            familiar.setItem(item);
          }

          // <name> gorges himself on candy from his bag.
          if (responseText.contains("gorges himself on candy from his bag")) {
            familiar.addNonCombatExperience(1);
          }
          break;

        case FamiliarPool.JACK_IN_THE_BOX:
          // 1st JitB charge: You turn <name>'s crank for a while.
          // This will fail if a SBIP is equipped, but the message is too short
          if (responseText.contains("'s crank for a while.")) {
            Preferences.setInteger("_jitbCharge", 1);
          }
          // 2nd JitB charge: The tension builds as you turn <name>'s crank some more.
          else if (responseText.contains("'s crank some more.")) {
            Preferences.setInteger("_jitbCharge", 2);
          }
          // 3rd JitB charge, popping it: You turn <name>'s crank a little more, and
          // all of a sudden a horrible grinning clown head emerges with a loud bang.
          // It wobbles back and forth on the end of its spring, as though dancing to
          // some sinister calliope music you can't actually hear...
          else if (responseText.contains("a horrible grinning clown head emerges")) {
            Preferences.setInteger("_jitbCharge", 0);
          }
          int jitbCharge = Preferences.getInteger("_jitbCharge");
          familiar.setCharges(jitbCharge);
          break;

        case FamiliarPool.HATRACK:
          if (responseText.contains(
                  "sees that you're about to get attacked and trips it before it can attack you.")
              || responseText.contains(
                  "does the Time Warp, then does the Time Warp again. Clearly, madness has taken its toll on him.")
              || responseText.contains("The air shimmers around you.")) {
            Preferences.increment("_timeHelmetAdv", 1);
          }
          break;

        case FamiliarPool.HIPSTER:
          //  The words POWER UP appear above <name>'s head as he
          //  instantly grows a stupid-looking moustache.
          if (responseText.contains("instantly grows a stupid-looking moustache")) {
            AdventureResult item = ItemPool.get(ItemPool.IRONIC_MOUSTACHE, 1);
            // The Mini-Hipster will do this once a day
            Preferences.setBoolean("_ironicMoustache", true);
            // Add ironic moustache to inventory
            ResultProcessor.processItem(ItemPool.IRONIC_MOUSTACHE, 1);
            // Equip familiar with it
            familiar.setItem(item);
          }
          break;

        case FamiliarPool.BOOTS:
          if (responseText.contains("stomps your opponent into paste")
              || responseText.contains("stomps your opponents into paste")
              || responseText.contains("shuffles its heels, gets a running start, then leaps on")) {
            Preferences.setBoolean("bootsCharged", false);
          }
          break;

        case FamiliarPool.HAPPY_MEDIUM:
          if (responseText.contains(
                  "waves her fingers in front of her face and her aura glows blue.")
              || responseText.contains("A flickering blue aura appears around ")
              || responseText.contains("A blue aura appears around ")
              || responseText.contains("and her aura glows blue.")
              || responseText.contains(
                  "rolls her eyes back in her head and her aura glows blue.")) {
            KoLCharacter.setFamiliarImage("medium_1.gif");
            familiar.setCharges(1);
          }

          if (responseText.contains(
                  "presses her fingers to her temples, and her aura changes from blue to orange.")
              || responseText.contains(
                  "changes to orange. She presses her palms together tightly and murmurs")
              || responseText.contains(
                  "lights a stick of sage and her aura changes from blue to orange.")
              || responseText.contains("changes from blue to orange and she begins to tremble.")
              || responseText.contains(
                  "mutters under her breath, her aura changing from blue to orange.")) {
            KoLCharacter.setFamiliarImage("medium_2.gif");
            familiar.setCharges(2);
          }

          if (responseText.contains(
                  "shakes like a leaf on the wind as her aura changes from orange to a deep, angry red.")
              || responseText.contains(
                  "levitates a few feet off of the ground, her aura changing from orange to a violent red.")
              || responseText.contains(
                  "drops to the ground and twitches, her aura changing from orange to red.")
              || responseText.contains(
                  "squeezes her eyes shut and shudders as her aura changes from orange to red.")
              || responseText.contains(" changes to a deep red.")) {
            KoLCharacter.setFamiliarImage("medium_3.gif");
            familiar.setCharges(3);
          }
          /*
                  if ( responseText.indexOf( "waves her hands and extracts some of your opponent aura into a cocktail shaker") != -1
                    || responseText.indexOf( "holds out a cocktail glass and siphons some of his aura into the glass" ) != -1
                    || responseText.indexOf( "draws out some of its spirit and makes a cocktail with it." ) != -1
                    || responseText.indexOf( "Let's see. . . a little of this, a little of that, and some of that creature's aura, and presto!" ) != -1
                    || responseText.indexOf( "conjurs the spirit of your opponent into a cocktail glass and mixes you a drink." ) != -1 )
                  {
                    KoLCharacter.setFamiliarImage( "medium_0.gif" );
                    familiar.setCharges( 0 );
                  }
          */
          break;
        case FamiliarPool.GRINDER:
          // Increment Organ Grinder combat counter
          String piediv = "".equals(Preferences.getString("pieStuffing")) ? "" : ",";
          if (responseText.contains("some grinder fodder, muttering")) {
            Preferences.increment("_piePartsCount", 1);
            String s = Preferences.getString("pieStuffing") + piediv + "fish";
            if (Preferences.getInteger("_piePartsCount") != 0)
              Preferences.setString("pieStuffing", s);
          } else if (responseText.contains("harvests a few choice bits for his grinder")) {
            Preferences.increment("_piePartsCount", 1);
            String s = Preferences.getString("pieStuffing") + piediv + "boss";
            if (Preferences.getInteger("_piePartsCount") != 0)
              Preferences.setString("pieStuffing", s);
          } else if (responseText.contains("a few choice bits")) {
            Preferences.increment("_piePartsCount", 1);
            String s = Preferences.getString("pieStuffing") + piediv + "normal";
            if (Preferences.getInteger("_piePartsCount") != 0)
              Preferences.setString("pieStuffing", s);
          } else if (responseText.contains("your opponent and tosses them")) {
            Preferences.increment("_piePartsCount", 1);
            String s = Preferences.getString("pieStuffing") + piediv + "stench";
            if (Preferences.getInteger("_piePartsCount") != 0)
              Preferences.setString("pieStuffing", s);
          } else if (responseText.contains("insides, squealing something")) {
            Preferences.increment("_piePartsCount", 1);
            String s = Preferences.getString("pieStuffing") + piediv + "hot";
            if (Preferences.getInteger("_piePartsCount") != 0)
              Preferences.setString("pieStuffing", s);
          } else if (responseText.contains("grind, chattering")) {
            Preferences.increment("_piePartsCount", 1);
            String s = Preferences.getString("pieStuffing") + piediv + "spooky";
            if (Preferences.getInteger("_piePartsCount") != 0)
              Preferences.setString("pieStuffing", s);
          } else if (responseText.contains("My Hampton has a funny feeling")) {
            Preferences.increment("_piePartsCount", 1);
            String s = Preferences.getString("pieStuffing") + piediv + "sleaze";
            if (Preferences.getInteger("_piePartsCount") != 0)
              Preferences.setString("pieStuffing", s);
          } else if (responseText.contains("grindable organs, muttering")) {
            Preferences.increment("_piePartsCount", 1);
            String s = Preferences.getString("pieStuffing") + piediv + "cold";
            if (Preferences.getInteger("_piePartsCount") != 0)
              Preferences.setString("pieStuffing", s);
          }
          break;

        case FamiliarPool.ARTISTIC_GOTH_KID:
          if (KoLCharacter.getHippyStoneBroken()) {
            if (responseText.contains("You gain 1 PvP Fight")) {
              Preferences.setInteger("_gothKidCharge", 0);
              Preferences.increment("_gothKidFights");
            } else {
              Preferences.increment("_gothKidCharge", 1);
            }
            int gothKidCharge = Preferences.getInteger("_gothKidCharge");
            familiar.setCharges(gothKidCharge);
          }
          break;

        case FamiliarPool.CRIMBO_SHRUB:
          if (KoLCharacter.getHippyStoneBroken()
              && Preferences.getString("shrubGarland").equals("PvP")) {
            if (responseText.contains("You gain 1 PvP Fight")) {
              Preferences.setInteger("_shrubCharge", 0);
            } else {
              Preferences.increment("_shrubCharge", 1);
            }
            int shrubCharge = Preferences.getInteger("_shrubCharge");
            familiar.setCharges(shrubCharge);
          }
          break;

        case FamiliarPool.BADGER:
          if (responseText.contains("produces a rainbow-colored mushroom")) {
            Preferences.setInteger("_badgerCharge", 0);
          } else {
            Preferences.increment("_badgerCharge", 1);
          }
          int badgerCharge = Preferences.getInteger("_badgerCharge");
          familiar.setCharges(badgerCharge);
          break;

        case FamiliarPool.PIXIE:
          if (responseText.contains("He tosses you a bottle of absinthe")) {
            Preferences.setInteger("_pixieCharge", 0);
          } else if (!KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.ABSINTHE))) {
            Preferences.increment("_pixieCharge", 1);
          }
          int pixieCharge = Preferences.getInteger("_pixieCharge");
          familiar.setCharges(pixieCharge);
          break;

        case FamiliarPool.LLAMA:
          if (responseText.contains("This gong will enable you to see things")) {
            Preferences.setInteger("_llamaCharge", 0);
          } else {
            Preferences.increment("_llamaCharge", 1);
          }
          int llamaCharge = Preferences.getInteger("_llamaCharge");
          familiar.setCharges(llamaCharge);
          break;

        case FamiliarPool.SANDWORM:
          if (responseText.contains(
              "he belches some murky fluid back into the bottle and hands it to you")) {
            Preferences.setInteger("_sandwormCharge", 0);
          } else {
            Preferences.increment("_sandwormCharge", 1);
          }
          int sandwormCharge = Preferences.getInteger("_sandwormCharge");
          familiar.setCharges(sandwormCharge);
          break;

        case FamiliarPool.TRON:
          if (responseText.contains("hands you an actual, literal token")) {
            Preferences.setInteger("_rogueProgramCharge", 0);
          } else {
            Preferences.increment("_rogueProgramCharge", 1);
          }
          int rogueProgramCharge = Preferences.getInteger("_rogueProgramCharge");
          familiar.setCharges(rogueProgramCharge);
          break;

        case FamiliarPool.ALIEN:
          if (responseText.contains("coughs up something covered in corrosive goo")) {
            Preferences.setInteger("_xenomorphCharge", 0);
          } else {
            Preferences.increment("_xenomorphCharge", 1);
          }
          int alienCharge = Preferences.getInteger("_xenomorphCharge");
          familiar.setCharges(alienCharge);
          break;

        case FamiliarPool.GROOSE:
          if (responseText.contains("he produces a small glob of grease")) {
            Preferences.setInteger("_grooseCharge", 0);
          } else {
            Preferences.increment("_grooseCharge", 1);
          }
          int grooseCharge = Preferences.getInteger("_grooseCharge");
          familiar.setCharges(grooseCharge);
          break;

        case FamiliarPool.KLOOP:
          if (responseText.contains("drops at your feet a small leatherbound book")) {
            Preferences.setInteger("_kloopCharge", 0);
          } else {
            Preferences.increment("_kloopCharge", 1);
          }
          int kloopCharge = Preferences.getInteger("_kloopCharge");
          familiar.setCharges(kloopCharge);
          break;

        case FamiliarPool.UNCONSCIOUS_COLLECTIVE:
          if (responseText.contains("dream stuff")) {
            Preferences.setInteger("_unconsciousCollectiveCharge", 0);
          } else {
            Preferences.increment("_unconsciousCollectiveCharge", 1);
          }
          int unconsciousCollectiveCharge = Preferences.getInteger("_unconsciousCollectiveCharge");
          familiar.setCharges(unconsciousCollectiveCharge);
          break;

        case FamiliarPool.ANGRY_JUNG_MAN:
          Preferences.increment("jungCharge", 1);
          int newCharges = Preferences.getInteger("jungCharge");
          familiar.setCharges(newCharges);
          break;

        case FamiliarPool.GRIM_BROTHER:
          if (responseText.contains("finishes an illustrated manuscript with a final flourish")) {
            Preferences.setInteger("_grimBrotherCharge", 0);
          } else {
            Preferences.increment("_grimBrotherCharge", 1);
          }
          int grimBrotherCharge = Preferences.getInteger("_grimBrotherCharge");
          familiar.setCharges(grimBrotherCharge);
          break;

        case FamiliarPool.GRIMSTONE_GOLEM:
          // Only charges if no mask has dropped today
          if (Preferences.getInteger("_grimstoneMaskDrops") == 0) {
            Preferences.increment("grimstoneCharge", 1);
            int grimCharges = Preferences.getInteger("grimstoneCharge");
            familiar.setCharges(grimCharges);
          }
          break;

        case FamiliarPool.GOLDEN_MONKEY:
          if (responseText.contains("You sweep it up and take it with you.")) {
            Preferences.setInteger("_goldenMoneyCharge", 0);
          } else {
            Preferences.increment("_goldenMoneyCharge", 1);
          }
          int goldenMoneyCharge = Preferences.getInteger("_goldenMoneyCharge");
          familiar.setCharges(goldenMoneyCharge);
          break;

        case FamiliarPool.ADVENTUROUS_SPELUNKER:
          // Only charges if no Tale has dropped today
          if (Preferences.getInteger("_spelunkingTalesDrops") == 0) {
            Preferences.increment("_spelunkerCharges", 1);
            int adventurousSpelunkerCharges = Preferences.getInteger("_spelunkerCharges");
            familiar.setCharges(adventurousSpelunkerCharges);
          }
          break;

        case FamiliarPool.STEAM_CHEERLEADER:
          int dec =
              KoLCharacter.hasEquipped(ItemPool.SPIRIT_SOCKET_SET, EquipmentManager.FAMILIAR)
                  ? 1
                  : 2;
          int currentSteam = Preferences.getInteger("_cheerleaderSteam");
          if (currentSteam - dec < 0) {
            dec = currentSteam;
          }
          Preferences.decrement("_cheerleaderSteam", dec);
          break;

        case FamiliarPool.NANORHINO:
          int currentCharge = Preferences.getInteger("_nanorhinoCharge");
          int newCharge =
              currentCharge
                  + (KoLCharacter.hasEquipped(
                          ItemPool.NANORHINO_CREDIT_CARD, EquipmentManager.FAMILIAR)
                      ? 3
                      : 2);
          // Verify value if text visible
          Matcher nanorhinoCharge1Matcher =
              FightRequest.NANORHINO_CHARGE1_PATTERN.matcher(responseText);
          Matcher nanorhinoCharge2Matcher =
              FightRequest.NANORHINO_CHARGE2_PATTERN.matcher(responseText);
          if (nanorhinoCharge1Matcher.find()) {
            newCharge = StringUtilities.parseInt(nanorhinoCharge1Matcher.group(1));
          } else if (nanorhinoCharge2Matcher.find()) {
            newCharge = StringUtilities.parseInt(nanorhinoCharge2Matcher.group(1));
          }
          if (newCharge > 100) {
            newCharge = 100;
          }
          Preferences.setInteger("_nanorhinoCharge", newCharge);
          break;

        case FamiliarPool.CUBELING:
          Preferences.increment("cubelingProgress");
          break;

        case FamiliarPool.REANIMATOR:
          if (responseText.contains("injects an arm")
              || responseText.contains("reanimates an arm")
              || responseText.contains("injects one of your opponent's arms")
              || responseText.contains("injects the arm")
              || responseText.contains("arm drags itself off")
              || responseText.contains("grabs  your opponent's left arm")
              || responseText.contains("reanimate an arm")
              || responseText.contains("reanimates one of your opponent's arms")
              || responseText.contains("grabs the arm and reanimates it")) {
            Preferences.increment("reanimatorArms", 1);
          } else if (responseText.contains("injects one of your opponent's legs")
              || responseText.contains("animates one of your opponent's legs")
              || responseText.contains("severs one of your opponent's legs")
              || responseText.contains("This leg is precisely what the swarm needs!")
              || responseText.contains("grabs a right leg to animate")) {
            Preferences.increment("reanimatorLegs", 1);
          } else if (responseText.contains("grabs  your opponent's head")
              || responseText.contains("grabs your  your opponent's head")
              || responseText.contains("lops off  your opponent's head")
              || responseText.contains("takes a skull to use later")) {
            Preferences.increment("reanimatorSkulls", 1);
          } else if (responseText.contains("grabs a part off  your opponent")
              || responseText.contains("grabs a chunk of  your opponent")
              || responseText.contains("animates a weird bit of  your opponent")
              || responseText.contains("animates a weird part from  your opponent")
              || responseText.contains("animates a weird part of  your opponent")
              || responseText.contains("animates a...thing...from your opponent")
              || responseText.contains("animates an unconventional part of  your opponent")
              || responseText.contains("cuts a chunk off of  your opponent")) {
            Preferences.increment("reanimatorWeirdParts", 1);
          } else if (responseText.contains("yanks off one of your opponent's wings")
              || responseText.contains("into one of your opponent's wings")
              || responseText.contains("The wing detaches")
              || responseText.contains("wing would be a perfect addition")
              || responseText.contains("reanimates a wing")
              || responseText.contains("reanimates one of your opponent's wings")
              || responseText.contains("takes one of your opponent's wings")
              || responseText.contains("injects one of your opponent's wings")
              || responseText.contains("wing is aerodynamically perfect")) {
            Preferences.increment("reanimatorWings", 1);
          }
          break;

        case FamiliarPool.MACHINE_ELF:
          if (responseText.contains("time starts passing again")) {
            Preferences.increment("_machineTunnelsAdv", 1, 5, false);
          } else if (adventure == AdventurePool.DEEP_MACHINE_TUNNELS) {
            Preferences.setInteger("_machineTunnelsAdv", 5);
          }
          break;

        case FamiliarPool.ROCKIN_ROBIN:
          Preferences.increment("rockinRobinProgress");
          break;

        case FamiliarPool.CANDLE:
          Preferences.increment("optimisticCandleProgress");
          break;

        case FamiliarPool.GARBAGE_FIRE:
          Preferences.increment("garbageFireProgress");
          break;

        case FamiliarPool.XO_SKELETON:
          Preferences.increment("xoSkeleltonXProgress");
          Preferences.increment("xoSkeleltonOProgress");
          break;

        case FamiliarPool.INTERGNAT:
          if (monster != null) {
            for (String s : monster.getRandomModifiers()) {
              if (s.equals("eldritch")) {
                String demonName = "";
                Matcher gnatMatcher;
                if ((gnatMatcher = FightRequest.INTERGNAT1_PATTERN.matcher(responseText)).find()) {
                  demonName = gnatMatcher.group(1);
                } else if ((gnatMatcher = FightRequest.INTERGNAT2_PATTERN.matcher(responseText))
                    .find()) {
                  demonName = gnatMatcher.group(1);
                } else if ((gnatMatcher = FightRequest.INTERGNAT3_PATTERN.matcher(responseText))
                    .find()) {
                  demonName = gnatMatcher.group(1);
                } else if ((gnatMatcher = FightRequest.INTERGNAT4_PATTERN.matcher(responseText))
                    .find()) {
                  demonName = gnatMatcher.group(1);
                }

                if (demonName.equals("Neil") || demonName.equals("")) {
                  break;
                } else if (demonName.contains("'")) {
                  SummoningChamberRequest.updateIntergnatName(demonName, false);
                } else {
                  SummoningChamberRequest.updateIntergnatName(demonName, true);
                }
              }
            }
          }
          break;

        case FamiliarPool.CAT_BURGLAR:
          if (responseText.contains("takes note of any security cameras in the area")
              || responseText.contains(
                  "watches carefully to see if there are any guards and when they change shifts")
              || responseText.contains("looks around for unlocked windows and accessible vents")
              || responseText.contains(
                  "stands around casually, definitely just loitering and not casing the joint at all")) {
            Preferences.increment("_catBurglarCharge");
          }
          if (responseText.contains(
                  "grabs a quick nap with his sleep mask, so he'll be fresh for the upcoming heist")
              || responseText.contains("takes advantage of the downtime to grab a few z's")
              || responseText.contains("disguises himself as someone who is asleep")) {
            Preferences.increment("_catBurglarCharge");
          }
          if (responseText.contains("Looks like he's ready for a heist")
              || responseText.contains(
                  "cracks his knuckles and looks around for something to steal")
              || responseText.contains(
                  "does some stretching exercises to prepare for his upcoming heist")) {
            // Current theory is that heist message can happen even when the charge is reached from
            // sleepmask trigger later
            // in the combat text. And that it resets charge to next heist to 0. We round to catch
            // other small errors too.
            int charge = Preferences.getInteger("_catBurglarCharge") + 1;
            Preferences.setInteger("_catBurglarCharge", Math.round(charge / 10) * 10);
          }
          int catBurglarCharge = Preferences.getInteger("_catBurglarCharge");
          familiar.setCharges(catBurglarCharge);
          break;

        case FamiliarPool.RED_SNAPPER:
          String monsterPhylum = MonsterStatusTracker.getLastMonster().getPhylum().toString();
          if (Preferences.getString("redSnapperPhylum").equals(monsterPhylum)) {
            Preferences.increment("redSnapperProgress");
          }
          break;

        case FamiliarPool.SHORT_ORDER_COOK:
          int charge = Preferences.getInteger("_shortOrderCookCharge");
          if (responseText.contains("shortbeer.gif")
              || responseText.contains("shortstack.gif")
              || responseText.contains("shortbutter.gif")
              || responseText.contains("shortwater.gif")
              || responseText.contains("shortcoffee.gif")) {
            if (KoLCharacter.hasEquipped(ItemPool.BLUE_PLATE, EquipmentManager.FAMILIAR)) {
              charge = 2;
            } else {
              charge = 0;
            }
          } else {
            charge += 1;
          }
          Preferences.setInteger("_shortOrderCookCharge", charge);
          familiar.setCharges(charge);

          Matcher otherFamiliarExp = SHORT_ORDER_EXP_PATTERN.matcher(responseText);
          if (otherFamiliarExp.find()) {
            FamiliarData fam = KoLCharacter.findFamiliar(otherFamiliarExp.group(1));

            if (fam != null) {
              int exp = StringUtilities.parseInt(otherFamiliarExp.group(2));
              fam.addNonCombatExperience(exp);
            }
          }
          break;
      }

      if (KoLCharacter.inRaincore()) {
        // Check for Thunder gain
        Matcher thunderMatcher =
            FightRequest.THUNDER_PATTERN.matcher(FightRequest.lastResponseText);
        if (thunderMatcher.find()) {
          String gainThunder = thunderMatcher.group(1);
          KoLCharacter.incrementThunder(StringUtilities.parseInt(gainThunder));
          String updateMessage = "You swallow " + gainThunder + " dB of Thunder";
          RequestLogger.updateSessionLog(updateMessage);
          KoLmafia.updateDisplay(updateMessage);
        }

        Matcher rainMatcher = FightRequest.RAIN_PATTERN.matcher(FightRequest.lastResponseText);
        if (rainMatcher.find()) {
          String gain = rainMatcher.group(1);
          KoLCharacter.incrementRain(StringUtilities.parseInt(gain));
          String updateMessage = "You recover " + gain + " drops of Rain";
          RequestLogger.updateSessionLog(updateMessage);
          KoLmafia.updateDisplay(updateMessage);
        }

        Matcher lightningMatcher =
            FightRequest.LIGHTNING_PATTERN.matcher(FightRequest.lastResponseText);
        if (lightningMatcher.find()) {
          String gain = lightningMatcher.group(1);
          KoLCharacter.incrementLightning(StringUtilities.parseInt(gain));
          String updateMessage = "You recover " + gain + " bolts of lightning";
          RequestLogger.updateSessionLog(updateMessage);
          KoLmafia.updateDisplay(updateMessage);
        }
      }

      // You see a strange cartouche painted on a nearby wall.
      if (KoLCharacter.hasEquipped(ItemPool.CROWN_OF_ED, EquipmentManager.HAT)
          && responseText.contains("You see a strange cartouche")) {
        FightRequest.handleCartouche(responseText);
      }

      // Booze Filler surveys the scene from atop the throne, and gains 1 Experience
      if (KoLCharacter.hasEquipped(ItemPool.HATSEAT, EquipmentManager.HAT)
          && responseText.contains("throne, and gains 1 Experience")) {
        KoLCharacter.getEnthroned().addNonCombatExperience(1);
      }

      // Llama surveys the scene from your back, and gains 1 Experience.
      if (KoLCharacter.hasEquipped(ItemPool.BUDDY_BJORN, EquipmentManager.CONTAINER)
          && responseText.contains("back, and gains 1 Experience")) {
        KoLCharacter.getBjorned().addNonCombatExperience(1);
      }

      if (Preferences.getInteger("_spookyJellyUses") > 0
          && responseText.contains("Spooked by the emanations")) {
        Preferences.decrement("_spookyJellyUses");
      }

      if (KoLCharacter.hasEquipped(ItemPool.SNOW_SUIT, EquipmentManager.FAMILIAR)) {
        Preferences.increment("_snowSuitCount", 1, 75, false);
      }

      if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.XIBLAXIAN_HOLOWRIST_PUTER, 1))) {
        Preferences.increment("_holoWristProgress");
      }

      if (QuestDatabase.isQuestLaterThan(Quest.GUZZLR, QuestDatabase.UNSTARTED)
          && Preferences.getString("guzzlrQuestLocation").equals(locationName)
          && responseText.contains(Preferences.getString("guzzlrQuestClient"))) {
        int incr = Math.max(3, 10 - Preferences.getInteger("_guzzlrDeliveries"));
        if (KoLCharacter.hasEquipped(ItemPool.GUZZLR_SHOES)) {
          incr = (int) Math.floor(1.5 * incr);
        }
        Preferences.increment("guzzlrDeliveryProgress", incr);
      }

      if (QuestDatabase.isQuestLaterThan(Quest.GUZZLR, QuestDatabase.UNSTARTED)
          && responseText.contains("You finally manage to track down")) {
        String tier = Preferences.getString("guzzlrQuestTier");
        int itemId = ItemDatabase.getItemId(Preferences.getString("guzzlrQuestBooze"));

        // For platinum deliveries, the cocktail with the highest item number is taken
        if (itemId == ItemPool.GUZZLR_COCKTAIL_SET) {
          tier = "platinum";

          for (int i = 10545; i >= 10541; i--) {
            if (InventoryManager.getCount(i) > 0) {
              ResultProcessor.processItem(i, -1);
              break;
            }
          }
        } else {
          ResultProcessor.processItem(itemId, -1);
        }

        // Increment number of completed deliveries for this tier
        if (tier != null && !tier.equals("")) {
          Preferences.increment("guzzlr" + StringUtilities.toTitleCase(tier) + "Deliveries");
        }

        // Increment the number of completed deliveries today.
        // Useful for determining how many combats the next delivery will take.
        Preferences.increment("_guzzlrDeliveries");

        // Reset the quest
        Preferences.setString("guzzlrQuestBooze", "");
        Preferences.setString("guzzlrQuestClient", "");
        Preferences.setString("guzzlrQuestLocation", "");
        Preferences.setString("guzzlrQuestTier", "");
        Preferences.setInteger("guzzlrDeliveryProgress", 0);
        QuestDatabase.setQuestProgress(Quest.GUZZLR, QuestDatabase.UNSTARTED);
      }

      if (responseText.contains(
          "The outdoors is so refreshing, that adventure just flew right by!")) {
        Preferences.decrement("breathitinCharges", 1, 0);
      }

      if (responseText.contains("FREEFREEFREE")) {
        String updateMessage = "This combat did not cost a turn";
        RequestLogger.updateSessionLog(updateMessage);
        KoLmafia.updateDisplay(updateMessage);
      } else {
        if (responseText.contains("playing on your SongBoom")) {
          Preferences.increment("_boomBoxFights");
        }
      }

      if (IslandManager.isBattlefieldMonster(monsterName)) {
        IslandManager.handleBattlefieldMonster(responseText, monsterName);
      } else if (monsterName.equals("black pudding")) {
        Preferences.increment("blackPuddingsDefeated", 1);
      } else if (monsterName.equals("general seal")) {
        ResultProcessor.removeItem(ItemPool.ABYSSAL_BATTLE_PLANS);
      } else if (monsterName.equals("Frank &quot;Skipper&quot; Dan, the Accordion Lord")) {
        ResultProcessor.removeItem(ItemPool.SUSPICIOUS_ADDRESS);
      } else if (monsterName.equals("Chef Boy, R&amp;D")) {
        ResultProcessor.removeItem(ItemPool.CHEF_BOY_BUSINESS_CARD);
      } else if (monsterName.equals("drunk pygmy")) {
        if (responseText.contains("notices the Bowl of Scorpions")) {
          ResultProcessor.removeItem(ItemPool.BOWL_OF_SCORPIONS);
          Preferences.increment("_drunkPygmyBanishes");
        }
      } else if (monsterName.equals("bugbear robo-surgeon")) {
        BugbearManager.clearShipZone("Medbay");
      } else if (monsterName.equals("wumpus")) {
        WumpusManager.reset();
      } else if (monsterName.equals("Baron von Ratsworth")) {
        TavernRequest.addTavernLocation('6');
      } else if (monsterName.equals("the invader")) {
        Preferences.setBoolean("spaceInvaderDefeated", true);
      } else if (monsterName.equals("Eldritch Tentacle")) {
        Preferences.increment("eldritchTentaclesFought", 1);
      } else if (special == SpecialMonster.SEWER && !EncounterManager.ignoreSpecialMonsters) {
        AdventureResult result = AdventureResult.tallyItem("sewer tunnel explorations", false);
        AdventureResult.addResultToList(KoLConstants.tally, result);
      }

      if (KoLCharacter.hasEquipped(ItemPool.BONE_ABACUS, EquipmentManager.OFFHAND)
          && responseText.contains("You move a bone on the abacus to record your victory")) {
        Preferences.increment("boneAbacusVictories", 1);
      }

      if (KoLCharacter.getAscensionClass() == AscensionClass.SNAKE_OILER) {
        if (responseText.contains("+1 Venom")) {
          Preferences.increment("awolVenom");
        } else if (responseText.contains("+1 Medicine")) {
          Preferences.increment("awolMedicine");
        }
      }

      QuestManager.updateQuestData(FightRequest.lastResponseText, monsterName);
    }

    if (KoLCharacter.isEd()) {
      if (responseText.contains("Continue to the Underworld")) {
        Preferences.increment("_edDefeats");
      } else {
        Preferences.setInteger("_edDefeats", 0);
        Preferences.setBoolean("edUsedLash", false);
      }
    } else if (KoLCharacter.isPlumber()) {
      KoLCharacter.resetCurrentPP();
    }

    FightRequest.inMultiFight = won && FightRequest.MULTIFIGHT_PATTERN.matcher(responseText).find();
    FightRequest.choiceFollowsFight = FightRequest.FIGHTCHOICE_PATTERN.matcher(responseText).find();

    // Do this AFTER we set the above so it does not continue
    // logging in if you are still in a fight or choice
    FightRequest.clearInstanceData();
    FightRequest.won = won;

    // <a href="fight.php" id="againlink">The barrier between world is torn...</a>
    if (FightRequest.inMultiFight && responseText.contains("The barrier between world")) {
      KoLAdventure.lastLocationName = "Eldritch Attunement";
    }
  }

  // <p>You see a strange cartouche painted on a nearby wall.<div style='position: relative;
  // display: inline-block; z-index 0;'><img src=/images/otherimages/cartouche.gif><div
  // style='position: absolute; left: 15; top: 30; z-index 1;'><img
  // src=/images/itemimages/hiero12.gif></div><div style='position: absolute; left: 15; top: 62;
  // z-index 1;'><img src=/images/itemimages/hiero24.gif></div><div style='position: absolute; left:
  // 15; top: 94; z-index 1;'><img src=/images/itemimages/hiero21.gif></div></div>

  private static final Pattern HIERO_PATTERN = Pattern.compile("itemimages/hiero(.*?)\\.gif");

  private static void handleCartouche(final String responseText) {
    StringBuilder buffer = new StringBuilder();
    Matcher matcher = FightRequest.HIERO_PATTERN.matcher(responseText);
    while (matcher.find()) {
      buffer.append(buffer.length() == 0 ? "Cartouche: " : ", ");
      buffer.append(matcher.group(1));
    }
    if (buffer.length() > 0) {
      String message = buffer.toString();
      RequestLogger.updateSessionLog(message);
      KoLmafia.updateDisplay(message);
    }
  }

  public static final String getSpecialAction() {
    ArrayList<Integer> items = new ArrayList<Integer>();

    String pref = Preferences.getString("autoOlfact");
    if (!pref.equals("")
        && !KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.ON_THE_TRAIL))) {
      boolean haveSkill =
          KoLCharacter.hasSkill("Transcendent Olfaction")
              && !KoLCharacter.inGLover()
              && (Preferences.getBoolean("autoManaRestore")
                  || KoLCharacter.getCurrentMP()
                      >= SkillDatabase.getMPConsumptionById(SkillPool.OLFACTION));
      boolean haveItem = KoLConstants.inventory.contains(FightRequest.EXTRACTOR);
      if ((haveSkill || haveItem) && shouldTag(pref, "autoOlfact triggered")) {
        if (haveSkill) {
          return OLFACTION_ACTION;
        }

        items.add(ItemPool.ODOR_EXTRACTOR);
      }
    }

    pref = Preferences.getString("autoPutty");
    if (!pref.equals("")) {
      int totalCopies =
          Preferences.getInteger("spookyPuttyCopiesMade")
              + Preferences.getInteger("_raindohCopiesMade");
      boolean haveItem =
          KoLConstants.inventory.contains(FightRequest.PUTTY_SHEET)
              && Preferences.getInteger("spookyPuttyCopiesMade") < 5
              && totalCopies < 6;
      boolean haveItem2 =
          KoLConstants.inventory.contains(FightRequest.RAINDOH_BOX)
              && Preferences.getInteger("_raindohCopiesMade") < 5
              && totalCopies < 6;
      boolean haveItem3 =
          KoLConstants.inventory.contains(FightRequest.CAMERA)
              && !KoLConstants.inventory.contains(FightRequest.SHAKING_CAMERA);
      boolean haveItem4 =
          KoLConstants.inventory.contains(FightRequest.CRAPPY_CAMERA)
              && !KoLConstants.inventory.contains(FightRequest.SHAKING_CRAPPY_CAMERA);
      boolean haveItem5 =
          KoLConstants.inventory.contains(FightRequest.PHOTOCOPIER)
              && !KoLConstants.inventory.contains(FightRequest.PHOTOCOPIED_MONSTER);
      if ((haveItem || haveItem2 || haveItem3 || haveItem4 || haveItem5)
          && shouldTag(pref, "autoPutty triggered")) {
        if (haveItem) {
          items.add(ItemPool.SPOOKY_PUTTY_SHEET);
        } else if (haveItem2) {
          items.add(ItemPool.RAIN_DOH_BOX);
        } else if (haveItem3) {
          items.add(ItemPool.CAMERA);
        } else if (haveItem4) {
          items.add(ItemPool.CRAPPY_CAMERA);
        } else {
          items.add(ItemPool.PHOTOCOPIER);
        }
      }
    }

    if (Preferences.getBoolean("autoPotionID")) {
      ItemPool.suggestIdentify(items, 819, 827, "lastBangPotion");
    }

    int itemsSize = items.size();
    if (itemsSize == 0) {
      return null;
    } else if (itemsSize == 1 || !KoLCharacter.hasSkill("Ambidextrous Funkslinging")) {
      return String.valueOf(items.get(0));
    } else {
      return items.get(0) + "," + items.get(1);
    }
  }

  private static boolean shouldTag(String pref, String msg) {
    boolean isAbort = false;
    boolean isMonster = false;
    List<AdventureResult> items = null;

    if (pref.endsWith(" abort")) {
      isAbort = true;
      pref = pref.substring(0, pref.length() - 6).trim();
    }

    if (pref.equals("goals")) {
      items = GoalManager.getGoals();
    } else if (pref.startsWith("monster ")) {
      isMonster = true;
      pref = pref.substring(8).trim();
    } else {
      if (pref.startsWith("item ")) {
        pref = pref.substring(5);
      }
      AdventureResult[] temp = ItemFinder.getMatchingItemList(pref, false, KoLConstants.inventory);
      if (temp == null) {
        return false;
      }
      items = Arrays.asList(temp);
    }

    boolean rv;
    if (isMonster) {
      MonsterData monster = MonsterStatusTracker.getLastMonster();
      if (monster == null) {
        return false;
      }
      rv = monster.getName().contains(pref);
    } else {
      rv = MonsterStatusTracker.dropsItems(items);
    }

    if (rv && isAbort) {
      KoLmafia.abortAfter(msg);
    }

    return rv;
  }

  private static void transmogrifyNemesisWeapon(boolean reverse) {
    for (int i = 0; i < FightRequest.NEMESIS_WEAPONS.length; ++i) {
      Object[] data = FightRequest.NEMESIS_WEAPONS[i];
      if (KoLCharacter.getAscensionClass() == data[0]) {
        EquipmentManager.transformEquipment(
            (AdventureResult) data[reverse ? 2 : 1], (AdventureResult) data[reverse ? 1 : 2]);
        return;
      }
    }
  }

  private static final Pattern BANG_POTION_PATTERN =
      Pattern.compile(
          "You throw the (.*?) potion at your opponent.?.  It shatters against .*?[,\\.] (.*?)\\.");

  private static void parseBangPotion(final String responseText) {
    if (FightRequest.anapest) {
      return;
    }

    Matcher bangMatcher = FightRequest.BANG_POTION_PATTERN.matcher(responseText);
    while (bangMatcher.find()) {
      int potionId = ItemDatabase.getItemId(bangMatcher.group(1) + " potion");

      String effectText = bangMatcher.group(2);
      String[][] strings = ItemPool.bangPotionStrings;

      for (int i = 0; i < strings.length; ++i) {
        if (effectText.contains(strings[i][1])) {
          if (ItemPool.eliminationProcessor(
              strings, i, potionId, 819, 827, "lastBangPotion", " of ")) {
            KoLmafia.updateDisplay("All bang potions have been identified!");
          }
          break;
        }
      }
    }
  }

  // The pirate sneers at you and replies &quot;<insult>&quot;

  private static final Pattern PIRATE_INSULT_PATTERN =
      Pattern.compile("The pirate sneers \\w+ you and replies &quot;(.*?)&quot;");

  private static void parsePirateInsult(final String responseText) {
    if (FightRequest.anapest) {
      return;
    }

    Matcher insultMatcher = FightRequest.PIRATE_INSULT_PATTERN.matcher(responseText);

    if (!insultMatcher.find()) {
      if (responseText.contains("The pirate stammers for a moment")) {
        insultedPirate = true;
      }
      return;
    }

    insultedPirate = true;

    int insult = BeerPongRequest.findPirateRetort(insultMatcher.group(1));
    if (insult <= 0) {
      return;
    }

    KoLCharacter.ensureUpdatedPirateInsults();
    if (!Preferences.getBoolean("lastPirateInsult" + insult)) { // it's a new one
      Preferences.setBoolean("lastPirateInsult" + insult, true);
      AdventureResult result = AdventureResult.tallyItem("pirate insult", false);
      AdventureResult.addResultToList(KoLConstants.tally, result);
      GoalManager.updateProgress(result);
      int count = BeerPongRequest.countPirateInsults();
      float odds = BeerPongRequest.pirateInsultOdds(count) * 100.0f;
      RequestLogger.printLine(
          "Pirate insults known: " + count + " (" + KoLConstants.FLOAT_FORMAT.format(odds) + "%)");
    }
  }

  private static void parseGrubUsage(final String responseText) {
    // You concentrate on one of the burrowgrubs digging its way
    // through your body, and absorb it into your bloodstream.
    // It's refreshingly disgusting!

    int pos = responseText.indexOf("refreshingly disgusting");
    if (pos != -1) {
      int uses = Preferences.getInteger("burrowgrubSummonsRemaining") - 1;

      while ((pos = responseText.indexOf("refreshingly disgusting", pos + 23)) != -1) {
        --uses;
      }

      // We have used our burrowgrub hive today
      Preferences.setBoolean("burrowgrubHiveUsed", true);

      // <option value="7074" picurl="nopic" selected>Consume
      // Burrowgrub (0 Mojo Points)</option>

      if (!responseText.contains("option value=\"7074\"")) {
        // We can't actually conclude anything from the lack of an
        // option to consume another one - it's possible that the
        // combat finished with no further user input.
        uses = Math.max(0, uses);
      } else { // At least one more use today

        uses = Math.max(1, uses);
      }

      Preferences.setInteger("burrowgrubSummonsRemaining", uses);
    }
  }

  private static final Pattern LASSO_PATTERN =
      Pattern.compile("You twirl the lasso, and (\\w+) toss");

  private static void parseLassoUsage(String responseText) {
    if (!responseText.contains("lasso")) {
      return;
    }
    // You twirl the lasso, and clumsily/carefully/deftly/expertly toss
    // it over the dogie's head. You yank the rope and knock him down.
    Matcher matcher = LASSO_PATTERN.matcher(responseText);
    if (matcher.find()) {
      Preferences.setString("lassoTraining", matcher.group(1));
    }
  }

  private static final Pattern[] CRYSTAL_BALL_PATTERNS = {
    Pattern.compile("your next fight will be against <b>an? (.*?)</b>"),
    Pattern.compile("next monster in this (?:zone is going to|area will) be <b>an? (.*?)</b>"),
    Pattern.compile("Look out, there's <b>an? (.*?)</b> right around the next corner"),
    Pattern.compile("There's a little you fighting a little <b>(.*?)</b>"),
    Pattern.compile("How do you feel about fighting <b>an? (.*?)</b>\\? Coz that's"),
    Pattern.compile("the next monster in this area will be <b>an? (.*?)</b>"),
    Pattern.compile("and see a tiny you fighting a tiny <b>(.*?)</b> in a tiny"),
    Pattern.compile("it looks like there's <b>an? (.*?)</b> prowling around"),
    Pattern.compile("and see yourself running into <b>an? (.*?)</b> soon"),
    Pattern.compile("showing you an image of yourself fighting <b>an? (.*?)</b>"),
    Pattern.compile("you're going to run into <b>an? (.*?)</b>"),
  };

  private static String parseCrystalBall(final String responseText) {
    for (Pattern p : CRYSTAL_BALL_PATTERNS) {
      Matcher matcher = p.matcher(responseText);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }

    return null;
  }

  public static final void parseCombatItems(String responseText) {
    // The current round will be zero when the fight is over.
    // If you run with the WOWbar, the combat item dropdown will
    // still be on the page. Don't look at it.
    if (FightRequest.currentRound < 1) {
      return;
    }

    int startIndex = responseText.indexOf("<select name=whichitem>");
    if (startIndex == -1) {
      return;
    }
    int endIndex = responseText.indexOf("</select>", startIndex);
    if (endIndex == -1) {
      return;
    }
    Matcher m =
        FightRequest.COMBATITEM_PATTERN.matcher(responseText.substring(startIndex, endIndex));
    while (m.find()) {
      int itemId = StringUtilities.parseInt(m.group(1));
      if (itemId <= 0) {
        continue;
      }

      // KoL has a bug: if you initiate combat by using a
      // d10, the number of d10s in the combat item dropdown
      // will be incorrect. Therefore, don't believe it.
      if (itemId == ItemPool.D10) {
        continue;
      }

      int actualQty = StringUtilities.parseInt(m.group(2));
      AdventureResult ar = ItemPool.get(itemId, 1);
      int currentQty = ar.getCount(KoLConstants.inventory);
      if (actualQty != currentQty) {
        ar = ar.getInstance(actualQty - currentQty);
        ResultProcessor.processResult(ar);
        RequestLogger.updateSessionLog("Adjusted combat item count: " + ar);
      }
    }
  }

  public static final void parseAvailableCombatSkills(String responseText) {
    // The current round will be zero when the fight is over.
    // If you run with the WOWbar, the skills dropdown will
    // still be on the page. Don't look at it.
    if (FightRequest.currentRound < 1) {
      return;
    }

    int startIndex = responseText.indexOf("<select name=whichskill>");
    if (startIndex == -1) {
      return;
    }
    int endIndex = responseText.indexOf("</select>", startIndex);
    if (endIndex == -1) {
      return;
    }

    KoLConstants.availableCombatSkills.clear();
    KoLConstants.availableCombatSkillsMap.clear();

    Matcher m =
        FightRequest.AVAILABLE_COMBATSKILL_PATTERN.matcher(
            responseText.substring(startIndex, endIndex));
    while (m.find()) {
      int skillId = StringUtilities.parseInt(m.group(1));
      String skillName = SkillDatabase.getSkillName(skillId);
      if (skillName == null) {
        skillName = m.group(2);
        SkillDatabase.registerSkill(skillId, skillName);
      }
      KoLCharacter.addAvailableCombatSkill(skillId);
      // If lovebug skills present, they've been unlocked
      if (skillId >= 7245 && skillId <= 7247) {
        Preferences.setBoolean("lovebugsUnlocked", true);
      }
      // If Gladiator skills present, they've been unlocked
      if (skillId >= 7085 && skillId <= 7093) {
        if (skillId <= 7087 && Preferences.getInteger("gladiatorBallMovesKnown") + 7084 < skillId) {
          Preferences.setInteger("gladiatorBallMovesKnown", skillId - 7084);
        }
        if (skillId >= 7088
            && skillId <= 7090
            && Preferences.getInteger("gladiatorNetMovesKnown") + 7087 < skillId) {
          Preferences.setInteger("gladiatorNetMovesKnown", skillId - 7087);
        }
        if (skillId >= 7091
            && Preferences.getInteger("gladiatorBladeMovesKnown") + 7090 < skillId) {
          Preferences.setInteger("gladiatorBallMovesKnown", skillId - 7090);
        }
      }
    }
  }

  private static void getRound(final StringBuffer action) {
    action.setLength(0);
    if (FightRequest.currentRound == 0) {
      action.append("After Battle: ");
    } else {
      action.append("Round ");
      action.append(FightRequest.currentRound);
      action.append(": ");
    }
  }

  private static void updateMonsterHealth(final String responseText) {
    StringBuffer action = new StringBuffer();
    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = monster != null ? monster.getName() : FightRequest.currentEncounter;

    Matcher m = FightRequest.NS_ML_PATTERN.matcher(responseText);
    if (m.find()) {
      MonsterStatusTracker.resetAttackAndDefense();
      if (Preferences.getBoolean("logMonsterHealth")) {
        action.append(monsterName);
        action.append(" resets her attack power and defense modifiers!");
      }
    }

    if (!Preferences.getBoolean("logMonsterHealth")) {
      return;
    }

    Matcher detectiveMatcher = FightRequest.DETECTIVE_PATTERN.matcher(responseText);
    if (detectiveMatcher.find()) {
      FightRequest.getRound(action);
      action.append(monsterName);
      action.append(" shows detective skull health estimate of ");
      action.append(detectiveMatcher.group(1));

      String message = action.toString();
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    Matcher helmetMatcher = FightRequest.SPACE_HELMET_PATTERN.matcher(responseText);
    if (helmetMatcher.find()) {
      FightRequest.getRound(action);
      action.append(monsterName);
      action.append(" shows toy space helmet health estimate of ");
      action.append(helmetMatcher.group(1));

      String message = action.toString();
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    int hp = DwarfFactoryRequest.deduceHP(responseText);
    if (hp > 0) {
      FightRequest.getRound(action);
      action.append(monsterName);
      action.append(" shows dwarvish war mattock health estimate of ");
      action.append(hp);

      String message = action.toString();
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    int attack = DwarfFactoryRequest.deduceAttack(responseText);
    if (attack > 0) {
      FightRequest.getRound(action);
      action.append(monsterName);
      action.append(" shows dwarvish war helmet attack rating of ");
      action.append(attack);

      String message = action.toString();
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    int defense = DwarfFactoryRequest.deduceDefense(responseText);
    if (defense > 0) {
      FightRequest.getRound(action);
      action.append(monsterName);
      action.append(" shows dwarvish war kilt defense rating of ");
      action.append(defense);

      String message = action.toString();
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }
  }

  private static void logPlayerAttribute(final TagStatus status, String message) {
    if (message.startsWith("You gain a") || message.startsWith("You gain some")) {
      // Ability points or levels
      status.shouldRefresh |= ResultProcessor.processGainLoss(message, null);
      return;
    }

    // Trim message, just like ResultProcessor.processGainLoss
    int index = message.indexOf(".");
    if (index != -1) {
      message = message.substring(0, index);
    }

    index = message.indexOf("(");
    if (index != -1) {
      message = message.substring(0, index);
    }

    message = message.trim();

    // Parse message
    AdventureResult result = ResultProcessor.parseResult(message);
    if (result == null) {
      // We don't recognize this. Assume we need a status refresh.
      status.shouldRefresh = true;
      return;
    }

    StringBuffer action = status.action;
    FightRequest.getRound(action);
    action.append(message);

    if (status.limitmode == Limitmode.SPELUNKY) {
      // If we lose HP in battle, annotate with attack/defense
      if (result.getName().equals(AdventureResult.HP)) {
        action.append(" (");
        action.append(MonsterStatusTracker.getMonsterAttack());
        action.append(" attack vs. ");
        action.append(KoLCharacter.getAdjustedMoxie());
        action.append(" moxie)");
      }
    } else if (status.limitmode == Limitmode.BATMAN) {
      // If we gain or lose HP in battle, track it
      if (result.getName().equals(AdventureResult.HP)) {
        BatManager.changeBatHealth(result);
      }
    }

    message = action.toString();
    RequestLogger.printLine(message);
    if (Preferences.getBoolean("logStatGains")) {
      RequestLogger.updateSessionLog(message);
    }

    status.shouldRefresh |= ResultProcessor.processResult(result);
  }

  private static void logMonsterAttribute(
      final TagStatus status, final int damage, final int type) {
    if (!status.logMonsterHealth) {
      return;
    }

    if (damage == 0) {
      return;
    }

    StringBuffer action = status.action;

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = monster != null ? monster.getName() : FightRequest.currentEncounter;

    FightRequest.getRound(action);
    action.append(monsterName);

    if (damage > 0) {
      action.append(type == HEALTH ? " takes " : " drops ");
      action.append(damage);
      action.append(type == ATTACK ? " attack power." : type == DEFENSE ? " defense." : " damage.");
    } else {
      action.append(type == HEALTH ? " heals " : " raises ");
      action.append(-1 * damage);
      action.append(
          type == ATTACK ? " attack power." : type == DEFENSE ? " defense." : " hit points.");
    }

    if (status.limitmode == Limitmode.SPELUNKY) {
      // additional logging when decrease monster's HP, attack, or defense
      AdventureResult weapon = EquipmentManager.getEquipment(EquipmentManager.WEAPON);
      Stat stat = EquipmentDatabase.getWeaponStat(weapon.getItemId());
      int hitStat =
          stat == Stat.MOXIE ? KoLCharacter.getAdjustedMoxie() : KoLCharacter.getAdjustedMuscle();
      int attack = MonsterStatusTracker.getMonsterAttack();
      int defense = MonsterStatusTracker.getMonsterDefense();
      String next = FightRequest.nextAction;
      switch (type) {
        case HEALTH:
          if (next != null && next.equals("attack")) {
            action.append(" (");
            action.append(hitStat);
            action.append(" ");
            action.append(stat == Stat.MOXIE ? "moxie" : "muscle");
            action.append(" vs. ");
            action.append(defense);
            action.append(" defense)");
          }
          break;
        case ATTACK:
          action.append(" (");
          action.append(attack);
          action.append(" -> ");
          action.append((attack - damage));
          action.append(")");
          break;
        case DEFENSE:
          action.append(" (");
          action.append(defense);
          action.append(" -> ");
          action.append((defense - damage));
          action.append(")");
          break;
      }
    }

    String message = action.toString();
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  // NOTE: All of the non-empty patterns that can match in the first group
  // imply that the entire expression should be ignored.	If you add one
  // and this is not the case, then correct the use of this Pattern below.

  private static final Pattern PHYSICAL_PATTERN =
      Pattern.compile(
          "(your blood, to the tune of|stabs you for|sown|You lose|You gain|strain your neck|approximately|roughly|)\\s*#?(\\d[\\d,]*) (\\([^.]*\\) |)((?:[^\\s]+ ){0,3})(?:\"?damage|points?|bullets|hollow|notch(?:es)?|to your opponent|to the foul demon|force damage|tiny holes|like this, it's bound|from the power|tiny lead pellets? that do(?:es)? 1 point of damage)");

  private static final Pattern ELEMENTAL_PATTERN =
      Pattern.compile(
          "(sown|) \\+?([\\d,]+) (\\([^.]*\\) |)(?:months worth of concentrated palm sweat|(?:slimy, (?:clammy|gross) |hotsy-totsy |)damage|points|HP worth)");

  private static final Pattern SECONDARY_PATTERN = Pattern.compile("\\+([\\d,]+)");

  private static int parseNormalDamage(final String text) {
    if (text.equals("")) {
      return 0;
    }

    int damage = 0;

    Matcher m = FightRequest.PHYSICAL_PATTERN.matcher(text);
    if (m.find()) {
      // Currently, all of the explicit attack messages that
      // preceed the number all imply that this is not damage
      // against the monster or is damage that should not
      // count (reap/sow X damage.)

      if (!m.group(1).equals("")) {
        return 0;
      }

      // "shambles up to your opponent" following a number is
      // most likely a familiar naming problem, so it should
      // not count.
      //
      // Similarly, using a number scroll on something other
      // than an adding machine does not do physical damage:
      //
      // You hand the 33398 scroll to your opponent. It
      // unrolls it, reads it, and looks slightly confused by
      // it. Then it tears it up and throws the bits into the
      // wind.

      if (m.group(4).equals("shambles up ") || m.group(4).equals("scroll ")) {
        return 0;
      }

      damage += StringUtilities.parseInt(m.group(2));

      // The last string contains all of the extra damage
      // from dual-wielding or elemental damage, e.g. "(+3)
      // (+10)".

      Matcher secondaryMatcher = FightRequest.SECONDARY_PATTERN.matcher(m.group(3));
      while (secondaryMatcher.find()) {
        damage += StringUtilities.parseInt(secondaryMatcher.group(1));
      }

      return damage;
    }

    m = FightRequest.ELEMENTAL_PATTERN.matcher(text);
    if (m.find()) {
      if (!m.group(1).equals("")) {
        return 0;
      }

      damage += StringUtilities.parseInt(m.group(2));

      Matcher secondaryMatcher = FightRequest.SECONDARY_PATTERN.matcher(m.group(3));
      while (secondaryMatcher.find()) {
        damage += StringUtilities.parseInt(secondaryMatcher.group(1));
      }
      return damage;
    }

    return 0;
  }

  private static void logSpecialDamage(final String text, TagStatus status) {
    if (text.contains("continues to bleed")
        || text.contains("from the poison")
        || text.contains("wave of toddlers")
        || text.contains("second wave follows")
        || text.contains("third group arrives")
        || text.contains("Bone Homie floats")
        || text.contains("Your stomach gurgles")
        || text.contains("Your insides vibrate")
        || text.contains("Your pygmy buddy staggers")
        || text.contains("disgusting coating of mayonnaise")
        || text.contains("EVISCERATE")
        || text.contains("sound of distant thunder")
        || text.contains("horrible stench of your armpits")
        || text.contains("burning beard hair")
        || text.contains("belch flames at your foe")
        || text.contains("damage from the demonic fire")
        || text.contains("Your dalmatian bites your opponent")
        || text.contains("burns you from head to toe")
        || false) {
      FightRequest.logText(text, status);
    }
  }

  public static final Pattern HAIKU_PATTERN = Pattern.compile("<td valign=center[^>]*>(.*?)</td>");
  private static final Pattern INT_PATTERN = Pattern.compile("\\d[\\d,]*");
  private static final Pattern SPACE_INT_PATTERN = Pattern.compile(" \\d[\\d,]*");
  private static final Pattern EFF_PATTERN = Pattern.compile("eff\\(['\"](.*?)['\"]");
  private static final Pattern DURATION_PATTERN =
      Pattern.compile("\\((?:duration: )?(\\d+) Adventures?\\)");

  private static int parseHaikuDamage(final String text) {
    if (!text.contains("damage") && !text.contains("from you to your foe")) {
      return 0;
    }

    Matcher damageMatcher = FightRequest.INT_PATTERN.matcher(text);
    if (damageMatcher.find()) {
      return StringUtilities.parseInt(damageMatcher.group());
    }
    return 0;
  }

  private static boolean extractVerse(
      final TagNode node, final StringBuffer buffer, final String tag) {
    boolean hasTag = false;
    String nodeName = node.getName();

    if (nodeName.equals("br")) {
      buffer.append(" / ");
    }

    if (tag != null && nodeName.equals(tag)) {
      hasTag = true;
    }

    Iterator<? extends BaseToken> it = node.getAllChildren().iterator();
    while (it.hasNext()) {
      BaseToken child = it.next();

      if (child instanceof ContentNode) {
        buffer.append(((ContentNode) child).getContent());
      } else if (child instanceof TagNode) {
        hasTag |= FightRequest.extractVerse((TagNode) child, buffer, tag);
      }
    }

    return hasTag;
  }

  private static void processHaikuResult(
      final TagNode node, final TagNode inode, final String image, final TagStatus status) {
    if (image.equals(status.familiar)
        || image.equals(status.enthroned)
        || image.equals(status.bjorned)) {
      FightRequest.processFamiliarAction(node, inode, status);
      return;
    }

    StringBuffer action = status.action;
    action.setLength(0);

    boolean hasBold = FightRequest.extractVerse(node, action, "b");
    String haiku = action.toString();

    if (FightRequest.foundVerseDamage(inode, status)) {
      return;
    }

    Matcher m = INT_PATTERN.matcher(haiku);
    if (!m.find()) {
      if (image.equals("strboost.gif") && hasBold) {
        String message = "You gain a Muscle point!";
        FightRequest.logPlayerAttribute(status, message);
      }

      if (image.equals("snowflakes.gif") && hasBold) {
        String message = "You gain a Mysticality point!";
        FightRequest.logPlayerAttribute(status, message);
      }

      if (image.equals("wink.gif") && hasBold) {
        String message = "You gain a Moxie point!";
        FightRequest.logPlayerAttribute(status, message);
      }
      return;
    }

    if (image.equals("nun.gif")) {
      status.nunnery = false;
      return;
    }

    String points = m.group();

    if (image.equals("meat.gif")) {
      String message = "You gain " + points + " Meat";
      ResultProcessor.processMeat(message, status.won, status.nunnery);
      status.won = false;
      status.shouldRefresh = true;
      return;
    }

    if (image.equals("hp.gif")) {
      // Gained or lost HP

      String gain = "lose";

      // Your wounds fly away
      // on a refreshing spring breeze.
      // You gain <b>X</b> hit points.

      // When <b><font color=black>XX</font></b> hit points<
      // are restored to your body,
      // you make an "ahhhhhhhh" sound.

      // You're feeling better --
      // <b><font color=black>XXX</font></b> hit points better -
      // than you were before.

      if (haiku.contains("Your wounds fly away")
          || haiku.contains("restored to your body")
          || haiku.contains("You're feeling better")) {
        gain = "gain";
      }

      String message = "You " + gain + " " + points + " hit points";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    if (image.equals("mp.gif")) {
      // Gained or lost MP

      String gain = "lose";

      // You feel quite refreshed,
      // like a frog drinking soda,
      // gaining <b>X</b> MP.

      // A contented belch.
      // Ripples in a mystic pond.
      // You gain <b>X</b> MP.

      // Like that wimp Dexter,
      // you have become ENERGIZED,
      // with <b>XX</b> MP.

      // <b>XX</b> magic points
      // fall upon you like spring rain.
      // Mana from heaven.

      // Spring rain falls within
      // metaphorically, I mean.
      // <b>XXX</b> mp.

      // <b>XXX</b> MP.
      // Like that sports drink commercial --
      // is it in you?  Yes.

      if (haiku.contains("You feel quite refreshed")
          || haiku.contains("A contented belch")
          || haiku.contains("ENERGIZED")
          || haiku.contains("Mana from heaven")
          || haiku.contains("Spring rain falls within")
          || haiku.contains("sports drink")) {
        gain = "gain";
      }
      String message = "You " + gain + " " + points + " Mojo points";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    if (image.equals("strboost.gif")) {
      String message = "You gain " + points + " Strongness";
      status.shouldRefresh |= ResultProcessor.processStatGain(message, null);
      return;
    }

    if (image.equals("snowflakes.gif")) {
      String message = "You gain " + points + " Magicalness";
      status.shouldRefresh |= ResultProcessor.processStatGain(message, null);
      return;
    }

    if (image.equals("wink.gif")) {
      String message = "You gain " + points + " Roguishness";
      status.shouldRefresh |= ResultProcessor.processStatGain(message, null);
      return;
    }

    if (haiku.contains("damage")) {
      // Using a combat item
      int damage = StringUtilities.parseInt(points);
      FightRequest.logMonsterAttribute(status, damage, HEALTH);
      MonsterStatusTracker.damageMonster(damage);
      return;
    }
  }

  private static void processMachineElfResult(
      final TagNode node, final TagNode inode, final String image, final TagStatus status) {
    if (image.equals(status.familiar)
        || image.equals(status.enthroned)
        || image.equals(status.bjorned)) {
      FightRequest.processFamiliarAction(node, inode, status);
      return;
    }

    StringBuffer action = status.action;
    action.setLength(0);

    boolean hasBold = FightRequest.extractVerse(node, action, "b");
    String machineElf = action.toString();

    if (image.equals("strboost.gif") && machineElf.contains("feel as though")) {
      String message = "You gain a Muscle point!";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    if (image.equals("snowflakes.gif") && machineElf.contains("feel as though")) {
      String message = "You gain a Mysticality point!";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    if (image.equals("wink.gif") && machineElf.contains("feel as though")) {
      String message = "You gain a Moxie point!";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    if (image.equals("hp.gif")) {
      // Gained or lost HP

      String gain = "lose";

      // Your mind sings a piercing lullaby as you are healed.

      if (machineElf.contains("you are healed")) {
        gain = "gain";
      }

      String message = "You " + gain + " 1 or more hit points";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    if (image.equals("mp.gif")) {
      // Gained or lost MP

      String gain = "lose";

      // A bracing salmon prophecy echoes through your thoughts, revitalizing you.

      if (machineElf.contains("revitalizing you")) {
        gain = "gain";
      }
      String message = "You " + gain + " 1 or more Mojo points";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    Matcher m = INT_PATTERN.matcher(machineElf);
    if (m.find()) {
      String points = m.group();

      if (image.equals("meat.gif")) {
        String message = "You gain " + points + " Meat";
        ResultProcessor.processMeat(message, status.won, status.nunnery);
        status.won = false;
        status.shouldRefresh = true;
        return;
      }
    }
  }

  private static int parseVerseDamage(final TagNode inode) {
    if (inode == null) {
      return 0;
    }

    // Don't return damage value in Machine Elf tunnels, as only familiar attacks have it.
    if (FightRequest.machineElf) {
      return 0;
    }

    // Look for Damage: title in the image
    String title = inode.getAttributeByName("title");
    if (title == null || !title.startsWith("Damage: ")) {
      return 0;
    }

    int damage = 0;

    String[] pieces = title.substring(8).split("[^\\d,]+");
    for (int i = 0; i < pieces.length; ++i) {
      damage += StringUtilities.parseInt(pieces[i]);
    }

    return damage;
  }

  private static boolean foundVerseDamage(final TagNode inode, final TagStatus status) {
    int damage = parseVerseDamage(inode);
    if (damage == 0) {
      return false;
    }

    FightRequest.logMonsterAttribute(status, damage, HEALTH);
    MonsterStatusTracker.damageMonster(damage);

    return true;
  }

  private static void processAnapestResult(
      final TagNode node, final TagNode inode, final String image, final TagStatus status) {
    if (image.equals(status.familiar)
        || image.equals(status.enthroned)
        || image.equals(status.bjorned)) {
      FightRequest.processFamiliarAction(node, inode, status);
      return;
    }

    StringBuffer action = status.action;
    action.setLength(0);

    boolean hasFont = FightRequest.extractVerse(node, action, "font");
    String verse = action.toString();

    if (FightRequest.foundVerseDamage(inode, status)) {
      return;
    }

    Matcher m = INT_PATTERN.matcher(verse);
    if (!m.find()) {
      if (image.equals("strboost.gif")) {
        String message = "You gain a Muscle point!";
        status.shouldRefresh |= ResultProcessor.processGainLoss(message, null);
      }

      if (image.equals("snowflakes.gif")) {
        String message = "You gain a Mysticality point!";
        status.shouldRefresh |= ResultProcessor.processGainLoss(message, null);
      }

      if (image.equals("wink.gif")) {
        String message = "You gain a Moxie point!";
        status.shouldRefresh |= ResultProcessor.processGainLoss(message, null);
      }
      return;
    }

    if (image.equals("nun.gif")) {
      status.nunnery = false;
      return;
    }

    String points = m.group();

    if (image.equals("meat.gif")) {
      String message = "You gain " + points + " Meat";
      ResultProcessor.processMeat(message, status.won, status.nunnery);
      status.won = false;
      status.shouldRefresh = true;
      return;
    }

    if (image.equals("hp.gif")) {
      // Gained or lost HP

      String gain = "lose";

      // You heal <b><font color=black>4</font></b> hit points, which may not be a lot,
      // But let's face it, right now, it's the best that you've got.

      // You've been beat up and beat down and beat sideways, too,
      // But you heal 7 hitpoints, and you feel less blue.

      // You've gained 7 hitpoints, and feel a lot better.
      // Go out there and get 'em, you going go-getter!

      // With <b><font color=black>21</font></b> hitpoints added onto your score,
      // you're raring to go and you're ready for more!

      // You apply a fresh bandage to stop blood from spurting,
      // and regenerate 21 points worth of hurting.

      // You've got 12 more hit points than you had before!
      // Now go out and fight and get off of the floor!

      // You were starting to feel a bit down in the dumps,
      // but those 7 HP should help clear up the lumps.

      // You heal a few damage -- not much, I'll admit,
      // but the hitpoints you've added will help you a bit.

      if (verse.contains("You heal")
          || verse.contains("you heal")
          || verse.contains("feel a lot better")
          || verse.contains("added onto your score")
          || verse.contains("regenerate")
          || verse.contains("more hit points")
          || verse.contains("help clear up the lumps")) {
        gain = "gain";
      }

      String message = "You " + gain + " " + points + " hit points";
      FightRequest.logPlayerAttribute(status, message);

      if (gain.equals("gain")) {
        if (status.mosquito) {
          status.mosquito = false;

          int damage = StringUtilities.parseInt(points);
          FightRequest.logMonsterAttribute(status, damage, HEALTH);
          MonsterStatusTracker.damageMonster(damage);
        }
      }

      return;
    }

    if (image.equals("mp.gif")) {
      // Gained or lost MP

      String gain = "lose";

      // Magical energy floods into your veins!
      // Not a whole lot -- 10 points -- but it's good for your brains.

      // You regain 15 MP of mystical fuel,
      // and prepare to escort some more monsters to school.

      // You've just gotten 10 of your MP restored!
      // Now you can show all those creeps what you've learned!

      // Your MP's restored, it's now 16 points higher.
      // Now you can set some more monsters on fire!

      // 17 MP should add spring to your step,
      // and lift up your spirits with gusto and pep!

      // Your MP just went up by 11 quarts!
      // (I'm not sure how you measure amounts of this sort.)

      if (verse.contains("Magical energy floods into your veins")
          || verse.contains("mystical fuel")
          || verse.contains("your MP restored")
          || verse.contains("set some more monsters on fire")
          || verse.contains("add spring to your step")
          || verse.contains("Your MP just went up")) {
        gain = "gain";
      }

      String message = "You " + gain + " " + points + " Mojo points";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    if (image.equals("strboost.gif")) {
      String message = "You gain " + points + " Strongness";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    if (image.equals("snowflakes.gif")) {
      String message = "You gain " + points + " Magicalness";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    if (image.equals("wink.gif")) {
      String message = "You gain " + points + " Roguishness";
      FightRequest.logPlayerAttribute(status, message);
      return;
    }

    // You hurl a thing that you took from your pack,
    // and deal <b>1</b> points with a thingly attack.

    if (verse.contains("thingly attack")) {
      // Using a combat item
      int damage = StringUtilities.parseInt(points);
      FightRequest.logMonsterAttribute(status, damage, HEALTH);
      MonsterStatusTracker.damageMonster(damage);
      return;
    }
  }

  public static class TagStatus {
    public String name;
    public String familiar;
    public String familiarName;
    public String enthroned;
    public String enthronedName;
    public String bjorned;
    public String bjornedName;
    public final boolean camel;
    public final boolean doppel;
    public final boolean crimbo;
    public String diceMessage;
    public final String ghost;
    public final boolean logFamiliar;
    public final boolean logMonsterHealth;
    public final StringBuffer action;
    public boolean shouldRefresh;
    public boolean famaction = false;
    public boolean mosquito = false;
    public boolean dice = false;
    public boolean nunnery = false;
    public boolean glitch = false;
    public boolean ravers = false;
    public boolean won = false;
    public Matcher macroMatcher;
    public int lastCombatItem = -1;
    public MonsterData monster;
    public String monsterName;
    public String encounter;
    public boolean seahorse;
    public boolean mayowasp;
    public boolean dolphin;
    public boolean eldritchHorror;
    public String limitmode;
    public String VYKEACompanion;
    public String horse;
    public boolean hookah = false;
    public boolean meteors;
    public String location;
    public int monsterId;

    public TagStatus() {
      FamiliarData current = KoLCharacter.getFamiliar();
      int familiarId = current.getId();
      this.familiar = current.getImageLocation();
      this.familiarName = current.getName();
      this.camel = (familiarId == FamiliarPool.MELODRAMEDARY);
      this.doppel =
          (familiarId == FamiliarPool.DOPPEL)
              || KoLCharacter.hasEquipped(
                  ItemPool.TINY_COSTUME_WARDROBE, EquipmentManager.FAMILIAR);
      this.crimbo = (familiarId == FamiliarPool.CRIMBO_SHRUB);

      this.diceMessage =
          (current.getId() == FamiliarPool.DICE) ? (familiarName + " begins to roll.") : null;

      FamiliarData enthroned = KoLCharacter.getEnthroned();
      String enthronedName = enthroned.getName();
      this.enthroned = enthroned.getImageLocation();
      this.enthronedName =
          (enthronedName == null || enthronedName.equals("")) ? null : enthronedName;

      FamiliarData bjorned = KoLCharacter.getBjorned();
      String bjornedName = bjorned.getName();
      this.bjorned = bjorned.getImageLocation();
      this.bjornedName = (bjornedName == null || bjornedName.equals("")) ? null : bjornedName;

      String VYKEAName = Preferences.getString("_VYKEACompanionName");
      this.VYKEACompanion = (VYKEAName == null || VYKEAName.equals("")) ? null : VYKEAName;

      String horseName = Preferences.getString("_horseryCurrentName");
      this.horse = (horseName == null || horseName.equals("")) ? null : horseName;

      this.logFamiliar = Preferences.getBoolean("logFamiliarActions");
      this.logMonsterHealth = Preferences.getBoolean("logMonsterHealth");
      this.action = new StringBuffer();

      this.shouldRefresh = false;

      this.monster = MonsterStatusTracker.getLastMonster();
      this.monsterName = this.monster != null ? monster.getName() : "";
      this.monsterId = this.monster != null ? monster.getId() : 0;

      this.encounter = FightRequest.currentEncounter;

      // Note if we are taming a wild seahorse
      this.seahorse = this.monsterName.equals("wild seahorse");

      // Note if we are fighting a mayonnaise wasp
      this.mayowasp = this.monsterName.equals("mayonnaise wasp");

      // Note if we are fighting Sssshhsssblllrrggghsssssggggrrgglsssshhssslblgl
      this.eldritchHorror =
          this.monsterName.equals("Sssshhsssblllrrggghsssssggggrrgglsssshhssslblgl");

      // Note if we are fighting in The Themthar Hills
      this.nunnery = this.monsterName.equals("dirty thieving brigand");

      // Note if we are fighting the glitch %monster%
      this.glitch = this.monsterName.equals("%monster%");

      // Note if we are fighting Outside the Club
      this.ravers = (KoLAdventure.lastAdventureId() == AdventurePool.OUTSIDE_THE_CLUB);

      // If we have the Meteor Lore skill
      this.meteors = KoLCharacter.hasSkill("Meteor Lore");

      this.ghost = null;

      // Save limitmode so we can log appropriately
      this.limitmode = KoLCharacter.getLimitmode();
      boolean isBatfellow = (this.limitmode == Limitmode.BATMAN);
      this.name = isBatfellow ? "Batfellow" : KoLCharacter.getUserName();

      this.location = KoLAdventure.lastLocationName == null ? "" : KoLAdventure.lastLocationName;
    }

    public void setFamiliar(final String image) {
      FamiliarData current = KoLCharacter.getFamiliar();
      int id = FamiliarDatabase.getFamiliarByImageLocation(image);
      if (id == current.getId()) {
        KoLCharacter.resetEffectiveFamiliar();
      } else {
        KoLCharacter.setEffectiveFamiliar(new FamiliarData(id));
      }
      FamiliarData effective = KoLCharacter.getEffectiveFamiliar();
      this.familiar = image;
      this.diceMessage =
          (effective.getId() == FamiliarPool.DICE)
              ? (current.getName() + " begins to roll.")
              : null;
    }
  }

  private static void processNormalResults(final String text, final Matcher macroMatcher) {
    TagNode fight =
        FightRequest.pokefam ? parseFamBattleHTML(text, true) : parseFightHTML(text, true);
    if (fight == null) {
      // Do normal result processing and hope for the best.
      FightRequest.shouldRefresh = ResultProcessor.processResults(true, text);
      return;
    }

    if (RequestLogger.isDebugging() && Preferences.getBoolean("logCleanedHTML")) {
      HTMLParserUtils.logHTML(fight);
    }

    TagStatus status = new TagStatus();
    status.macroMatcher = macroMatcher;

    if (FightRequest.pokefam) {
      // Parse and log the pokefam part of the battle. This
      // will return the node following that to continue with
      // normal processing.
      FightRequest.processFamBattle(fight, status);
    }

    FightRequest.processNode(fight, status);

    FightRequest.shouldRefresh = status.shouldRefresh;
  }

  public static final void parseFamBattleHTML(final String text) {
    TagNode node = FightRequest.parseFamBattleHTML(text, false);
    HTMLParserUtils.logHTML(node);
    FightRequest.processFamBattle(node, new TagStatus());
    HTMLParserUtils.logHTML(node);
  }

  private static TagNode parseFamBattleHTML(String text, boolean logIt) {
    // Clean the HTML on the Fight page
    TagNode node = FightRequest.cleanFightHTML(text);
    if (node == null) {
      if (logIt) {
        RequestLogger.printLine("HTML cleaning failed.");
      }
      return null;
    }

    // Find the top of the parse tree. Unlike normal fights, there
    // is not always a "monster" at the top. There is always a
    // header for the opponent's "team"
    //
    // <center><b>a fleet woodsman's Team:</b>

    for (Object bnode : node.getElementListByName("b", true)) {
      // Should be unnecessary. We need a more modern version of this package
      if (bnode instanceof TagNode) {
        TagNode b = (TagNode) bnode;
        if (b.getText().toString().indexOf(" Team:") != -1) {
          return b.getParent();
        }
      }
    }

    return null;
  }

  public static final void processFamBattle(final TagNode node, final TagStatus status) {
    // The node is a "center" node with the following children:
    //
    // <b>XXX's Team:</b>
    // (nodes describing that team)
    // (nodes describing your team)
    // <b>Your Team</b>
    // (nodes describing end-of-battle stuff)
    //
    // If we want to anything with the teams, this is the place.

    // Remove all the team-related stuff from the "center" node and
    // allow regular node processing to glean whatever it wants
    // from what remains.

    Iterator<? extends BaseToken> it = node.getAllChildren().iterator();
    boolean done = false;
    int pokindex = 0;
    while (it.hasNext() && !done) {
      BaseToken child = it.next();
      if (child instanceof TagNode) {
        TagNode tnode = (TagNode) child;
        String name = tnode.getName();

        // Each familiar is in a table
        if (name.equals("table")) {
          FightRequest.processPokefam(++pokindex, tnode, status);
        } else if (name.equals("b")) {
          if (tnode.getText().toString().equals("Your Team")) {
            done = true;
          }
        }
      }
      it.remove();
    }

    HTMLParserUtils.logHTML(node);
  }

  // Here is the HTML tree of a familiar on your foe's team with three moves
  /*
    <table class="">
      <tbody>
        <tr>
          <td rowspan="2">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/frankengnome.gif">
          <td class="tiny" width="150">
            Vincentenstein
          <td rowspan="2" width="120">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blacksword.gif">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blacksword.gif">
          <td rowspan="2" align="center" width="60">
          <td rowspan="2" width="150">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blackheart.gif">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blackheart.gif">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blackheart.gif">
            <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blackheart.gif">
        <tr>
          <td class="tiny">
            Lv. 5 Reagnimated Gnome
        <tr>
          <td height="10">
        <tr>
          <td>
          <td colspan="5" class="small" valign="center">
            <b>
              Skills:
            <span title="Deal [power] damage to the frontmost enemy and reduce its power by 1.">
              [Punch]
            &nbsp;&nbsp;
            <span title="Heal the frontmost ally by [power].">
              [Hug]
            &nbsp;&nbsp;
            <span title="Deal 5 damage to the frontmost enemy.">
              [ULTIMATE: Deluxe Impale]
            &nbsp;&nbsp;
  */
  // Here is the HTML tree of a familiar on your team with two moves and an attribute

  /*
   <table class="">
     <tbody>
       <tr>
         <td rowspan="2">
           <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/familiar7.gif">
         <td class="tiny" width="150">
           Boney Grrl
         <td rowspan="2" width="120">
           <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blacksword.gif">
         <td rowspan="2" align="center" width="60">
           <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/whiteshield.gif" alt="Armor:  This familiar will take 1 less damage from attacks (minimum of 1)." title="Armor:  This familiar will take 1 less damage from attacks (minimum of 1).">
         <td rowspan="2" width="150">
           <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blackheart.gif">
           <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blackheart.gif">
           <img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/blackheart.gif">
       <tr>
         <td class="tiny">
           Lv. 2 Spooky Pirate Skeleton
       <tr>
         <td height="10">
       <tr>
         <td>
         <td colspan="5" class="small" valign="center">
           <form style="display: inline" action="fambattle.php" method="post">
             <input type="hidden" name="pwd" value="2d6e81e1b8495aa91f5a931d114089ab">
             <input title="Deal [power] damage to the frontmost enemy and reduce its power by 1." class="button skb" type="submit" value="Punch" name="famaction[punch-7]">
             &nbsp;
             <input title="Knock the frontmost enemy to the back." class="button skb" type="submit" value="Tackle" name="famaction[tackle-7]">
             &nbsp;
  */

  public static final String imgToString(final TagNode node) {
    if (node == null) {
      return "";
    }

    if (!node.getName().equals("img")) {
      return "";
    }
    String src = node.getAttributeByName("src");
    if (src == null) {
      return "";
    }
    int index = src.lastIndexOf("/");
    if (index == -1) {
      return src;
    }
    return src.substring(index + 1);
  }

  private static final int ULTIMATE = "ULTIMATE: ".length();

  public static final void processPokefam(
      final int index, final TagNode node, final TagStatus status) {
    // For now only look at the familiar at the very beginning of the fight.
    //
    // Certainly, don't call FamiliarDatabase to check the attributes of a familiar
    // part way through the battle, since  Power, HP, and attributes will change
    // as the battle progresses.
    if (FightRequest.currentRound > 1) {
      return;
    }

    // If this is your team:
    // - Account for famboost items
    // - Moves are in "input" fields of a form
    //
    // If this is the enemy team:
    // - Familiars are not boosted (on round 1 - which is all we look at, for now)
    // - Moves are in "span" fields enclosed in []
    //
    // We separate those out, since the Team Management page has
    // only your familiars - which can be perma-boosted by items -
    // but use the "span" and [] format for moves

    boolean myteam = index > 3;
    FightRequest.parsePokefam(node, myteam, !myteam);
  }

  public static final void parsePokefam(final TagNode node, boolean myFamiliar, boolean moveSpans) {
    // Get all the rows from the table
    TagNode[] rows = node.getElementsByName("tr", true);

    // Row 1: familiar image, familiar name, power, attribute, hp
    String image = "";
    String name = "";
    int power = 0;
    List<String> attributes = new ArrayList<String>();
    String attribute = "None";
    int hp = 0;

    TagNode[] row1Tags = rows[0].getChildTags();
    int td = 1;
    for (TagNode tdnode : row1Tags) {
      switch (td++) {
        case 1:
          {
            // Familiar Image:
            TagNode inode = tdnode.findElementByName("img", true);
            image = imgToString(inode);
            break;
          }
        case 2:
          {
            // Familiar name
            name = tdnode.getText().toString();
            break;
          }
        case 3:
          {
            // Familiar power: one image (blacksword.gif) per
            power = tdnode.getElementsByName("img", true).length;
            break;
          }
        case 4:
          {
            // Familiar attribute: distinct images, can have two
            TagNode[] inodes = tdnode.getElementsByName("img", true);
            for (TagNode inode : inodes) {
              String title = inode.getAttributeByName("title");
              if (title != null) {
                int colon = title.indexOf(":");
                String aname = title.substring(0, colon);
                attributes.add(aname);
              }
            }
            break;
          }
        case 5:
          {
            // Familiar HP: one image (blackheart.gif) per
            hp = tdnode.getElementsByName("img", true).length;
            break;
          }
      }
    }

    // Row 2: familiar level and race
    int level = 0;
    String race = "";

    TagNode[] row2Tags = rows[1].getChildTags();
    if (row2Tags.length > 0) {
      String famtype = row2Tags[0].getText().toString();
      level = StringUtilities.parseInt(famtype.substring(4, 5));
      race = famtype.substring(6);
    }

    // If this is your familiar, it might have been boosted with a pokepill
    // Read the "" setting and look for "<race>:<boost>" where boost can be:
    // Power, HP, Armor, Regenerating, Smart, Spiked
    PokeBoost boost = myFamiliar ? FamTeamRequest.getPokeBoost(race) : PokeBoost.NONE;
    switch (boost) {
      case POWER:
        power -= 1;
        break;
      case HP:
        hp -= 1;
        break;
      case ARMOR:
      case REGENERATING:
      case SMART:
      case SPIKED:
        attributes.remove(boost.toString());
        break;
    }

    if (attributes.size() > 0) {
      attribute = attributes.get(0);
    }

    // Row 3: nothing?

    // Row 4: moves
    String[] moves = new String[3];
    String[] actions = new String[3];
    String[] descriptions = new String[3];

    TagNode[] row4Tags = rows[3].getChildTags();
    if (row4Tags.length > 1) {
      TagNode tdnode = row4Tags[1];
      if (moveSpans) {
        // Enemy team
        // <span title="Deal 5 damage to the frontmost enemy.">
        //  [ULTIMATE: Deluxe Impale]
        TagNode[] spans = tdnode.getElementsByName("span", false);
        for (int i = 0; i < spans.length; ++i) {
          // <span style="background-color: lightblue;">
          //   <b>
          //     <span title="Heal the frontmost ally by [power]. (This is what the enemy team will
          // do next round.)">
          //        [Hug]

          TagNode span = spans[i];
          if (span.getAttributeByName("title") == null) {
            // Next action is nested
            // *** remember that this is the designated enemy move?
            span = span.findElementByName("span", true);
          }
          String str = span.getText().toString();
          if (str.startsWith("[")) {
            int start = 1 + (i == 2 ? ULTIMATE : 0);
            moves[i] = str.substring(start, str.length() - 1);
          }
          // *** Get the title and log it, if the move is previously unknown?
        }
      } else {
        // Your team
        // <input title="Knock the frontmost enemy to the back." class="button skb" type="submit"
        // value="Tackle" name="famaction[tackle-7]">
        TagNode[] inputs = rows[3].getElementsByName("input", true);
        int move = 0;
        for (int i = 0; i < inputs.length; ++i) {
          TagNode input = inputs[i];
          String type = input.getAttributeByName("class");
          if (type == null || !type.startsWith("button")) {
            continue;
          }
          String str = input.getAttributeByName("value");
          String action = input.getAttributeByName("name");
          String description = input.getAttributeByName("title");
          if (move == 2) {
            str = str.substring(ULTIMATE);
          }
          if (action != null) {
            // "famaction[tackle-7]"
            int lb = action.indexOf("[");
            int dash = action.indexOf("-");
            if (lb != -1 && dash != -1) {
              action = action.substring(lb + 1, dash);
              actions[move] = action;
              descriptions[move] = description;
            }
          }
          moves[move++] = str;
          // *** Get the title and log it, if the move is previously unknown?
        }
      }
    }

    for (int i = 0; i < 3; i++) {
      String move = moves[i];
      String action = actions[i];
      String description = descriptions[i];
      FightRequest.registerPokefamMove(i + 1, move, action, description);
    }

    // System.out.println( "image = " + image );
    // System.out.println( "name = " + name );
    // System.out.println( "power = " + power );
    // System.out.println( "attribute = " + attribute );
    // System.out.println( "hp = " + hp );
    // System.out.println( "level = " + level );
    // System.out.println( "race = " + race );
    // System.out.println( "move 1 = " + moves[0] );
    // System.out.println( "move 2 = " + moves[1] );
    // System.out.println( "move 3 = " + moves[2] );

    FamiliarDatabase.registerPokefam(
        race, level, power, hp, attribute, moves[0], moves[1], moves[2]);
  }

  public static final void parseFightHTML(final String text) {
    HTMLParserUtils.logHTML(parseFightHTML(text, false));
  }

  private static TagNode parseFightHTML(String text, boolean logIt) {
    // Clean the HTML on the Fight page
    TagNode node = FightRequest.cleanFightHTML(text);
    if (node == null) {
      if (logIt) {
        RequestLogger.printLine("HTML cleaning failed.");
      }
      return null;
    }

    // Find the monster tag
    TagNode mon = findMonsterTag(node, logIt);
    if (mon != null) {
      return findFightNode(mon, logIt);
    }

    // Attempt to find top of fight the hard way.

    // <td style="color: white;" align=center bgcolor=blue><b>Combat!</b></td></tr><tr><td
    // style="padding: 5px; border: 1px solid blue;"><center><table><tr><td>

    // We (probably) want the second <center> tag; the first one
    // holds the whole combat table

    TagNode[] nodes = node.getElementsByName("center", true);
    if (nodes != null || nodes.length >= 2) {
      return nodes[1].getParent();
    }

    if (logIt) {
      RequestLogger.printLine("Cannot find combat results.");
    }

    return null;
  }

  private static TagNode cleanFightHTML(final String text) {
    // Clean the HTML on this fight response page
    return cleaner.clean(text);
  }

  private static TagNode findMonsterTag(final TagNode node, final boolean logIt) {
    // Look first for 'monpic' image.
    // All haiku monsters and most normal monsters have that.
    TagNode mon = node.findElementByAttValue("id", "monpic", true, false);

    // If that fails, look for 'monname' span.
    if (mon == null) {
      mon = node.findElementByAttValue("id", "monname", true, false);
    }

    if (mon == null && logIt) {
      RequestLogger.printLine("Cannot find monster.");
    }

    return mon;
  }

  private static TagNode findFightNode(final TagNode mon, final boolean logIt) {
    // Walk up the tree and find <center>
    //
    // The parent of that node has everything interesting about the
    // fight.
    TagNode fight = mon;
    while (fight != null) {
      fight = fight.getParent();
      if (fight != null && fight.getName().equals("center")) {
        // One more level
        return fight.getParent();
      }
    }

    if (logIt) {
      RequestLogger.printLine("Cannot find combat results.");
    }

    return null;
  }

  // Utility to reproduce the behavior of TagNode.getText() from the
  // earlier version we upgraded from.
  private static String getContentNodeText(TagNode node) {
    StringBuilder text = new StringBuilder();
    for (Object item : node.getAllChildren()) {
      if (item instanceof ContentNode) {
        text.append(((ContentNode) item).getContent());
      }
    }
    return text.toString();
  }

  private static final Pattern FUMBLE_PATTERN =
      Pattern.compile("You drop your .*? on your .*?, doing ([\\d,]+) damage");
  private static final Pattern MOSQUITO_PATTERN =
      Pattern.compile("sucks some blood out of your opponent and injects it into you.");
  private static final Pattern ADORABLE_SEAL_PATTERN =
      Pattern.compile("greedily sucks the vital juices from the wound");
  private static final Pattern STABBAT_PATTERN = Pattern.compile(" stabs you for ([\\d,]+) damage");
  private static final Pattern CARBS_PATTERN =
      Pattern.compile("some of your blood, to the tune of ([\\d,]+) damage");

  private static void specialFamiliarDamage(final StringBuffer text, TagStatus status) {
    int familiarId = KoLCharacter.getEffectiveFamiliar().getId();

    if (FightRequest.anapest || FightRequest.haiku) {
      switch (familiarId) {
        case FamiliarPool.MOSQUITO:
        case FamiliarPool.ADORABLE_SEAL_LARVA:
          status.mosquito = true;
          break;
      }

      return;
    }

    // Mosquito can muck with the monster's HP, but doesn't have
    // normal text.

    switch (familiarId) {
      case FamiliarPool.MOSQUITO:
        {
          Matcher m = FightRequest.MOSQUITO_PATTERN.matcher(text);
          if (m.find()) {
            status.mosquito = true;
          }
          break;
        }

      case FamiliarPool.ADORABLE_SEAL_LARVA:
        {
          Matcher m = FightRequest.ADORABLE_SEAL_PATTERN.matcher(text);

          if (m.find()) {
            status.mosquito = true;
          }
          break;
        }

      case FamiliarPool.STAB_BAT:
        {
          Matcher m = FightRequest.STABBAT_PATTERN.matcher(text);

          if (m.find()) {
            String message = "You lose " + m.group(1) + " hit points";
            FightRequest.logPlayerAttribute(status, message);
          }
          break;
        }

      case FamiliarPool.ORB:
        {
          Matcher m = FightRequest.CARBS_PATTERN.matcher(text);

          if (m.find()) {
            String message = "You lose " + m.group(1) + " hit points";
            FightRequest.logPlayerAttribute(status, message);
          }
          break;
        }
    }
  }

  private static void processNode(final TagNode node, final TagStatus status) {
    String name = node.getName();
    // StringBuffer action = status.action;

    // Skip html links
    if (name.equals("a")) {
      return;
    }

    // Skip forms. We used to prune them from the tree, but we need
    // to see them for fambattle parsing.
    if (name.equals("form")) {
      return;
    }

    /// node-specific processing
    if (name.equals("script")) {
      Matcher m = CLEESH_PATTERN.matcher(node.getText());
      if (!m.find()) {
        return;
      }

      String monsterName = m.group(2);

      FightRequest.clearInstanceData(true);
      FightRequest.logText("your opponent becomes " + monsterName + "!", status);

      return;
    }

    if (name.equals("b")) {
      if (FightRequest.handleProselytization(node, status)) {
        return;
      }
    }

    if (name.equals("table")) {
      String id = node.getAttributeByName("id");
      if (id != null && id.equals("monpic")) {
        // Don't process the monster picture
        return;
      }

      // Items have "rel" strings.
      String cl = node.getAttributeByName("class");
      String rel = node.getAttributeByName("rel");
      if (cl != null && cl.equals("item") && rel != null) {
        AdventureResult result = ItemDatabase.itemFromRelString(rel);
        ResultProcessor.processItem(true, "You acquire an item:", result, null);
        if (node.getText().toString().startsWith("Item unequipped")) {
          EquipmentManager.discardEquipment(result);
        }
        return;
      }

      if (status.famaction) {
        TagNode inode = node.findElementByName("img", true);
        FightRequest.processFamiliarAction(node, inode, status);
        return;
      }

      TagNode[] tables = node.getElementsByName("table", true);
      for (int i = 0; i < tables.length; ++i) {
        TagNode table = tables[i];
        table.getParent().removeChild(table);
      }

      if (FightRequest.processTable(node, status)) {
        FightRequest.processChildren(node, status);
      }

      for (int i = 0; i < tables.length; ++i) {
        TagNode table = tables[i];
        FightRequest.processNode(table, status);
      }

      return;
    }

    if (name.equals("hr")) {
      FightRequest.updateRoundData(status.macroMatcher);
      if (status.macroMatcher.find()) {
        FightRequest.registerMacroAction(status.macroMatcher);
        ++FightRequest.currentRound;
      } else {
        FightRequest.logText("unspecified macro action?", status);
      }
      DiscoCombatHelper.parseFightRound(FightRequest.nextAction, status.macroMatcher);
    } else if (name.equals("p")) {
      if (FightRequest.handleEldritchHorror(node, status)) {
        return;
      }

      FightRequest.handleKisses(node, status);
      FightRequest.handleCrimboPresent(node, status);

      if (FightRequest.handleChakra(node, status)) {
        return;
      }

      String str = FightRequest.getContentNodeText(node);

      if (containsMacroError(str)) {
        FightRequest.macroErrorMessage = str;
        Preferences.setString("lastMacroError", str);
        return;
      }

      // Camera flashes
      // A monster caught on the film
      // Back to yearbook club.

      if (FightRequest.haiku && str.contains("Back to yearbook club")) {
        FightRequest.handleYearbookCamera(status);
        return;
      }

      if (FightRequest.handleFuzzyDice(str, status)) {
        return;
      }

      if (FightRequest.processFumble(str, status)) {
        return;
      }

      if (FightRequest.handleEvilometer(str, status)) {
        return;
      }

      if (FightRequest.handleKeyotron(str, status)) {
        return;
      }

      if (FightRequest.handleSeahorse(str, status)) {
        return;
      }

      if (FightRequest.handleMayoWasp(str, status)) {
        return;
      }

      if (FightRequest.handleSpelunky(str, status)) {
        return;
      }

      if (FightRequest.handleFamiliarInteraction(str, status)) {
        return;
      }

      FightRequest.handleVillainLairRadio(node, status);

      if (status.meteors && (str.contains("meteor") || str.contains("falling star"))) {
        FightRequest.logText(str, status);
      }

      if ( // KoL Con 13 Snowglobe
      str.contains("KoL Con")
          || str.contains("You notice some extra Meat")
          ||
          // Mr. Screege's spectacles
          str.contains("You notice something valuable hidden")
          ||
          // Mr. Cheeng's spectacles
          str.contains("You see a weird thing out of the corner of your eye, and you grab it")
          || str.contains("You think you see a weird thing out of the corner of your eye")
          ||
          // lucky gold ring
          str.contains("Your lucky gold ring gets warmer for a moment.")) {
        FightRequest.logText(str, status);
      }

      // Retrospecs
      if (str.contains("notice an item you missed earlier")) {
        FightRequest.logText(str, status);
      }

      boolean VYKEAaction = status.VYKEACompanion != null && str.contains(status.VYKEACompanion);
      if (VYKEAaction && status.logFamiliar) {
        // VYKEA companion action
        FightRequest.logText(str, status);
      }

      boolean ghostAction = status.ghost != null && str.contains(status.ghost);
      if (ghostAction && status.logFamiliar) {
        // Pastamancer ghost action
        FightRequest.logText(str, status);
      }

      boolean horseAction = status.horse != null && str.contains(status.horse);
      if (horseAction && status.logFamiliar) {
        // Horsery horse action
        FightRequest.logText(str, status);
      }

      int damage = FightRequest.parseNormalDamage(str);
      if (damage != 0) {
        FightRequest.logSpecialDamage(str, status);
        FightRequest.logMonsterAttribute(status, damage, HEALTH);
        MonsterStatusTracker.damageMonster(damage);
        FightRequest.processComments(node, status);
        return;
      }
    }

    FightRequest.processChildren(node, status);
  }

  private static void processChildren(final TagNode node, final TagStatus status) {
    StringBuffer action = status.action;
    Iterator<? extends BaseToken> it = node.getAllChildren().iterator();
    while (it.hasNext()) {
      BaseToken child = it.next();

      if (child instanceof CommentNode) {
        CommentNode object = (CommentNode) child;
        FightRequest.processComment(object, status);
        continue;
      }

      if (child instanceof ContentNode) {
        ContentNode object = (ContentNode) child;
        String str = object.getContent().trim();

        if (str.equals("")) {
          continue;
        }

        if (FightRequest.handleFuzzyDice(str, status)) {
          continue;
        }

        if (FightRequest.processFumble(str, status)) {
          continue;
        }

        if (FightRequest.handleSpelunky(str, status)) {
          continue;
        }

        if (str.contains("you feel all warm and fuzzy")) {
          if (status.logFamiliar) {
            FightRequest.logText("A freed guard turtle returns.", status);
          }
          continue;
        }

        boolean VYKEAaction = status.VYKEACompanion != null && str.contains(status.VYKEACompanion);
        if (VYKEAaction && status.logFamiliar) {
          // VYKEA companion action
          FightRequest.logText(str, status);
        }

        boolean ghostAction = status.ghost != null && str.contains(status.ghost);
        if (ghostAction && status.logFamiliar) {
          // Pastamancer ghost action
          FightRequest.logText(str, status);
        }

        boolean camelAction = status.camel && str.contains(status.familiarName);
        if (camelAction && status.logFamiliar) {
          // Melodramedary action
          FightRequest.handleMelodramedary(str, status);
        }

        int damage = FightRequest.parseNormalDamage(str);
        if (damage != 0) {
          FightRequest.logSpecialDamage(str, status);
          FightRequest.logMonsterAttribute(status, damage, HEALTH);
          MonsterStatusTracker.damageMonster(damage);
          continue;
        }

        if (str.startsWith("You acquire a skill")) {
          TagNode bnode = node.findElementByName("b", true);
          if (bnode != null) {
            String skill = bnode.getText().toString();
            ResponseTextParser.learnSkill(skill);
          }
          continue;
        }

        if (str.startsWith("You gain")) {
          FightRequest.logPlayerAttribute(status, str);
          continue;
        }

        if (str.startsWith("You can has")) {
          // Adjust for Can Has Cyborger
          str = StringUtilities.singleStringReplace(str, "can has", "gain");
          FightRequest.logPlayerAttribute(status, str);
          continue;
        }
        continue;
      }

      if (child instanceof TagNode) {
        TagNode object = (TagNode) child;
        FightRequest.processNode(object, status);
      }
    }
  }

  private static boolean processTable(TagNode node, TagStatus status) {
    // Tables often appear in fight results to hold images.
    TagNode inode = node.findElementByName("img", true);
    String onclick = null;

    if (inode != null) {
      String alt = inode.getAttributeByName("alt");
      if (alt != null && alt.startsWith("Enemy's")) {
        // This is Monster Manuel stuff
        int attack = 0, defense = 0, hp = 0;
        TagNode[] cells = node.getElementsByName("td", true);
        for (int i = 0; i < cells.length; i++) {
          TagNode cell = cells[i];
          TagNode img = cell.findElementByName("img", false);
          if (img == null) continue;
          String stat = img.getAttributeByName("alt");
          if (stat == null || !stat.startsWith("Enemy's")) continue;
          i++;
          cell = cells[i];
          int value = StringUtilities.parseInt(cell.getText().toString());
          if (stat.equals("Enemy's Attack Power")) attack = value;
          else if (stat.equals("Enemy's Defense")) defense = value;
          else if (stat.equals("Enemy's Hit Points")) hp = value;
        }
        MonsterStatusTracker.setManuelStats(attack, defense, hp);
        return false;
      }

      onclick = inode.getAttributeByName("onclick");
    }

    if (status.dolphin) {
      status.dolphin = false;

      if (onclick == null) {
        return false;
      }

      Matcher m = INT_PATTERN.matcher(onclick);
      String descid = m.find() ? m.group() : null;

      if (descid == null) {
        return false;
      }

      int itemId = ItemDatabase.getItemIdFromDescription(descid);
      if (itemId == -1) {
        return false;
      }

      AdventureResult result = ItemPool.get(itemId, 1);
      String message = "A dolphin stole: " + result.getName();
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
      Preferences.setString("dolphinItem", result.getName());
      return false;
    }

    StringBuffer action = status.action;
    String str = node.getText().toString();

    if (inode == null) {
      FightRequest.handleRaver(str, status);

      boolean VYKEAaction = status.VYKEACompanion != null && str.contains(status.VYKEACompanion);
      if (VYKEAaction && status.logFamiliar) {
        // VYKEA companion action
        FightRequest.logText(str, status);
      }

      int damage =
          (FightRequest.haiku || FightRequest.anapest)
              ? FightRequest.parseHaikuDamage(str)
              : FightRequest.parseNormalDamage(str);
      if (damage != 0) {
        FightRequest.handleSpelunky(str, status);
        FightRequest.logSpecialDamage(str, status);
        FightRequest.logMonsterAttribute(status, damage, HEALTH);
        MonsterStatusTracker.damageMonster(damage);
      }

      // If it's not combat damage, perhaps it's a stat gain or loss
      else if (str.startsWith("You gain") || str.startsWith("You lose")) {
        FightRequest.logPlayerAttribute(status, str);
      }

      // The tootlers tootle! The singers all sing!
      // You've accomplished a wonderful, glorious thing!
      // Come raise a glass high, and come join in the revel!
      // We're all celebrating! You went up a level!

      else if (FightRequest.anapest && str.contains("You went up a level")) {
        String msg = "You gain a Level!";
        FightRequest.logPlayerAttribute(status, msg);
      }

      // If it is something else, let caller process the
      // children and attempt to figure it out

      else {
        return true;
      }

      return false;
    }

    // Look for items and effects first
    if (onclick != null) {
      if (onclick.startsWith("descitem") && !str.contains("An item drops:")) {
        Matcher m = INT_PATTERN.matcher(onclick);
        if (!m.find()) {
          return false;
        }

        int itemId = ItemDatabase.getItemIdFromDescription(m.group());
        AdventureResult result = ItemPool.get(itemId);

        boolean autoEquip = str.contains("automatically equipped");
        String acquisition = autoEquip ? "You acquire and equip an item:" : "You acquire an item:";
        ResultProcessor.processItem(true, acquisition, result, null);

        if (autoEquip) {
          EquipmentManager.autoequipItem(result);
        }

        if (str.contains("Item unequipped:")) { // Item removed by Zombo
          EquipmentManager.discardEquipment(itemId);
        }
        return false;
      }

      if (onclick.contains("desc_skill.php")) {
        if (str.startsWith("You acquire a skill")) {
          TagNode bnode = node.findElementByName("b", true);
          if (bnode != null) {
            String skill = bnode.getText().toString();
            ResponseTextParser.learnSkill(skill);
          }
          return false;
        }
      }

      Matcher m = EFF_PATTERN.matcher(onclick);
      if (m.find()) {
        // Gain/loss of effect or intrinsic
        status.shouldRefresh = true;

        String descId = m.group(1);
        int effectId = EffectDatabase.getEffectIdFromDescription(descId);
        if (effectId == -1) {
          return false;
        }

        int colon = str.indexOf(":");
        String acquisition = str.substring(0, colon + 1);

        if (acquisition.contains("intrinsic")) {
          AdventureResult intrinsic =
              acquisition.startsWith("You lose")
                  ? EffectPool.get(effectId, 0)
                  : EffectPool.get(effectId, Integer.MAX_VALUE);
          ResultProcessor.processIntrinsic(true, acquisition, intrinsic, null);
          return false;
        }

        Matcher d = DURATION_PATTERN.matcher(str);
        int duration = d.find() ? StringUtilities.parseInt(d.group(1)) : 1;
        AdventureResult result = EffectPool.get(effectId, duration);

        ResultProcessor.processEffect(true, acquisition, result, null);

        if (acquisition.startsWith("You lose")) {
          return false;
        }

        if (status.hookah) {
          String message = null;
          int quality = EffectDatabase.getQuality(effectId);

          if (EffectDatabase.hasAttribute(effectId, "nohookah")) {
            message =
                result.getName() + " is available from the hookah, but KoLmafia thought it was not";
          } else if (quality != EffectDatabase.GOOD) {
            message =
                result.getName()
                    + " is good quality, but KoLmafia thought it was "
                    + EffectDatabase.getQualityDescription(effectId);
          }

          if (message != null) {
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }

          status.hookah = false;
        }

        if (effectId == EffectPool.HAIKU_STATE_OF_MIND) {
          FightRequest.haiku = true;
          FightRequest.logMonsterAttribute(status, 17, HEALTH);
          MonsterStatusTracker.damageMonster(17);
        } else if (effectId == EffectPool.JUST_THE_BEST_ANAPESTS) {
          FightRequest.anapest = true;
        }
        return false;
      }
    }

    String src = inode.getAttributeByName("src");

    if (src == null) return false;

    String image = src.substring(src.lastIndexOf("/") + 1);

    if (FightRequest.handleSpelunkyGold(image, str, status)) {
      return false;
    }

    // Attempt to identify combat items
    String itemName = inode.getAttributeByName("title");
    int itemId = ItemDatabase.getItemId(itemName);
    if (itemId != -1 && image.equals(ItemDatabase.getImage(itemId))) {
      status.lastCombatItem = itemId;
    }

    if (status.limitmode == Limitmode.BATMAN
        && image.equals("briefcase.gif")
        && str.contains("You lose an item")) {
      AdventureResult result = ItemPool.get(ItemPool.FINGERPRINT_DUSTING_KIT, -1);
      ResultProcessor.processItem(true, "You lose an item:", result, null);
      return false;
    }

    if (image.equals("hp.gif") && (str.contains("regains") || str.contains("She looks about"))) {
      // The monster heals itself
      Matcher m = INT_PATTERN.matcher(str);
      int healAmount = m.find() ? StringUtilities.parseInt(m.group()) : 0;
      FightRequest.logMonsterAttribute(status, -healAmount, HEALTH);
      MonsterStatusTracker.healMonster(healAmount);

      status.shouldRefresh = true;
      return false;
    }

    if (image.equals("nicesword.gif")) {
      // You modify monster attack power
      Matcher m = SPACE_INT_PATTERN.matcher(str);
      int damage = m.find() ? StringUtilities.parseInt(m.group()) : 0;
      FightRequest.logMonsterAttribute(status, damage, ATTACK);
      MonsterStatusTracker.lowerMonsterAttack(damage);
      return false;
    }

    if (image.equals("whiteshield.gif")) {
      // You modify monster defense
      Matcher m = INT_PATTERN.matcher(str);
      int damage = m.find() ? StringUtilities.parseInt(m.group()) : 0;
      FightRequest.logMonsterAttribute(status, damage, DEFENSE);
      MonsterStatusTracker.lowerMonsterDefense(damage);
      return false;
    }

    // If you have Just the Best Anapests and go to the
    // haiku dungeon, you see ... anapests!

    if (FightRequest.anapest) {
      FightRequest.processAnapestResult(node, inode, image, status);
      return false;
    }

    if (FightRequest.haiku) {
      FightRequest.processHaikuResult(node, inode, image, status);
      return false;
    }

    if (FightRequest.machineElf) {
      FightRequest.processMachineElfResult(node, inode, image, status);
      return false;
    }

    if (image.equals("nun.gif")) {
      // A nun announces that she is taking the Meat. Subsequent Meat gains are not taken.
      status.nunnery = false;
      return false;
    }

    if (image.equals("meat.gif")) {
      // Adjust for Can Has Cyborger
      str = StringUtilities.singleStringReplace(str, "gets", "gain");
      str = StringUtilities.singleStringReplace(str, "Meets", "Meat");

      // Adjust for the 1000 Meat cap for free fights:
      //   There's a lot of Meat here, but you're in too big of a hurry to grab more than 1,000 of
      // it.
      if (str.contains("too big of a hurry to grab more than 1,000")) {
        str = "You gain 1,000 Meat";
      }

      // Adjust for The Sea
      str = StringUtilities.singleStringReplace(str, "manage to grab", "gain");

      // Process the Meat gain. If we have seen WINWINWIN
      // (status.won), it is the monster's Meat drop, as
      // opposed to from your Hobo Monkey or similar extra
      // source. If we are in the Themthar Hills
      // (status.nunnery) and we have won, the nuns take it.

      status.shouldRefresh |= ResultProcessor.processMeat(str, status.won, status.nunnery);

      // We can deduce how implemented your glitch item is
      // from the Meat dropped after defeating %monster%
      FightRequest.handleGlitchMonster(status, str);

      // Only the first Meat drop after the WINWINWIN comment is the
      // monster's drop. Don't do meat drop handling for later drops.
      status.won = false;

      return false;
    }

    if (image.equals("hp.gif") || image.equals("mp.gif")) {
      // You gain HP or MP
      if (status.mosquito) {
        status.mosquito = false;
        Matcher m = INT_PATTERN.matcher(str);
        int damage = m.find() ? StringUtilities.parseInt(m.group()) : 0;
        FightRequest.logMonsterAttribute(status, damage, HEALTH);
        MonsterStatusTracker.damageMonster(damage);
      }

      FightRequest.logPlayerAttribute(status, str);
      return false;
    }

    if (image.equals(status.familiar)
        || image.equals(status.enthroned)
        || image.equals(status.bjorned)) {
      FightRequest.processFamiliarAction(node, inode, status);
      return false;
    }

    if (image.equals("hkatana.gif")) {
      // You struck with your haiku katana. Pull the
      // damage out of the img tag if we can
      if (FightRequest.foundVerseDamage(inode, status)) {
        return false;
      }
    }

    if (image.equals("realdolphin_r.gif")) {
      // You are slowed too much by the water, and a
      // stupid dolphin swims up and snags a seaweed
      // before you can grab it.

      // Inside this table is another table with
      // another image of the stolen dolphin item.

      status.dolphin = true;
      return false;
    }

    if (image.equals("camera.gif")) {
      if (status.lastCombatItem == ItemPool.CAMERA) {
        // Your camera begins to shake, rattle and roll.
        // You've already got a camera with a monster in it.
        // This moment is too horrifying to commit to film.

        if (!str.contains("shake, rattle and roll")) {
          return false;
        }
        action.append(status.monsterName);
        action.append(" copied");
        FightRequest.logText(action, status);
        status.lastCombatItem = -1;
      } else if (status.lastCombatItem == ItemPool.CRAPPY_CAMERA) {
        // With a dim flash of light and an accompanying old-timey -POOF- noise,
        // you snap a picture of it. Your camera begins to shake, disconcertingly.

        if (!str.contains("shake, disconcertingly")) {
          return false;
        }
        action.append(status.monsterName);
        action.append(" copied");
        FightRequest.logText(action, status);
        status.lastCombatItem = -1;
      } else if (status.lastCombatItem == ItemPool.DISPOSABLE_CAMERA) {
        // do nothing, this isn't the Yearbook Club Camera
      } else {
        FightRequest.handleYearbookCamera(status);
      }
      return false;
    }

    // ammo.gif is a combat item but has weird HTML structure: the
    // damage dealt is not in a child node
    if (image.equals("ammo.gif")) {
      int damage = FightRequest.parseNormalDamage(str);
      if (damage != 0) {
        FightRequest.logMonsterAttribute(status, damage, HEALTH);
        MonsterStatusTracker.damageMonster(damage);
        return false;
      }
    }

    if (image.equals("tc_soldier.gif")) {
      // Terra Cotta Soldier. Log the effect.
      FightRequest.logText(str, status);
      int damage = FightRequest.parseNormalDamage(str);
      if (damage != 0) {
        FightRequest.logMonsterAttribute(status, damage, HEALTH);
        MonsterStatusTracker.damageMonster(damage);
        return false;
      }
    }

    if (image.equals("sig_receiver.gif") || image.equals("nopic.gif")) {
      if (str.contains("signal receiver")) {
        // Your signal receiver emits quiet static.
        // The low static on your signal receiver is suddenly broken by a series of beeps and boops
        // and buzzes: <i>boopbeepbuzzboopbuzzhissbuzzbeepbeepbeep</i>
        FightRequest.logText(str, status);
      }
      return false;
    }

    // Combat item usage: process the children of this node
    // to pick up damage to the monster and stat gains
    return true;
  }

  private static boolean handleEldritchHorror(TagNode node, TagStatus status) {
    if (!status.eldritchHorror) {
      return false;
    }

    String str = node.getText().toString();

    if (str.startsWith("You hear in your mind")
        || str.startsWith("Sssshhsssblllrrggghsssssggggrrgglsssshhssslblgl is done with you")) {
      FightRequest.logText(str, status);
      return true;
    }

    if (str.startsWith("Combat rages around you")
        || str.startsWith("You survey the battle around you:")) {
      return true;
    }

    if (str.startsWith("You  plinked")
        || str.startsWith("You  damaged")
        || str.startsWith("You  bothered")
        || str.startsWith("You  whomped")
        || str.startsWith("You  epically whomped")
        || str.startsWith("You  epically blasted")) {
      str = StringUtilities.globalStringReplace(str, "  ", " ");
      int index = str.indexOf(".") + 1;
      if (index != 0) {
        String s1 = str.substring(0, index).trim();
        String s2 = str.substring(index).trim();
        FightRequest.logText(s1, status);
        FightRequest.logText(s2, status);
      } else {
        FightRequest.logText(str, status);
      }
      return true;
    }

    return false;
  }

  private static boolean handleGlitchMonster(TagStatus status, String str) {
    if (!status.glitch || !status.won) {
      return false;
    }

    Matcher m = INT_PATTERN.matcher(str);
    if (m.find()) {
      int meat = StringUtilities.parseInt(m.group());
      int have = InventoryManager.getCount(ItemPool.GLITCH_ITEM);
      // If none in inventory, something is seriously wrong,
      // but avoid divide-by-zero
      if (have > 0) {
        int count = meat / (5 * have);
        Preferences.setInteger("glitchItemImplementationCount", count);
        int level =
            (count >= 111)
                ? 7
                : (count >= 69)
                    ? 6
                    : (count >= 37)
                        ? 5
                        : (count >= 11)
                            ? 4
                            : (count >= 4) ? 3 : (count >= 2) ? 2 : (count >= 1) ? 1 : 0;
        Preferences.setInteger("glitchItemImplementationLevel", level);
      }
    }

    // Only the first Meat gain matters
    status.glitch = false;

    return true;
  }

  public static final Pattern KISS_PATTERN =
      Pattern.compile("(\\d+) kiss(?:es)? for winning(?: \\+(\\d+) for difficulty)?");

  private static boolean handleKisses(TagNode node, TagStatus status) {
    TagNode span = node.findElementByName("span", true);
    if (span == null) {
      return false;
    }

    String title = span.getAttributeByName("title");
    if (title == null || !title.contains("kiss")) {
      return false;
    }

    KoLAdventure location = KoLAdventure.lastVisitedLocation();
    if (location == null || !location.getZone().equals("Dreadsylvania")) {
      return true;
    }

    String str = span.getText().toString();

    // Log the actual kiss message
    FightRequest.logText(str, status);

    // 1 kiss for winning
    // 1 kiss for winning +1 for difficulty
    // 100 kisses for winning +100 for difficulty x2 for Hard Mode

    Matcher matcher = FightRequest.KISS_PATTERN.matcher(title);
    if (!matcher.find()) {
      return true;
    }

    int kisses = StringUtilities.parseInt(matcher.group(1));

    if (kisses > 1) {
      // It's a boss and the zone is finished
      kisses = 0;
    } else if (matcher.group(2) != null) {
      // The zone had elevated difficulty
      kisses += StringUtilities.parseInt(matcher.group(2));
    }

    String name = location.getAdventureName();

    if (name.endsWith("Woods")) {
      FightRequest.dreadWoodsKisses = kisses;
    } else if (name.endsWith("Village")) {
      FightRequest.dreadVillageKisses = kisses;
    } else if (name.endsWith("Castle")) {
      FightRequest.dreadCastleKisses = kisses;
    }

    return true;
  }

  // It's from <a href=showplayer.php?who=2379226><b>TroyMcClure2</b></a>!

  public static final Pattern CRIMBO_PATTERN = Pattern.compile("It's from (.*?)!");

  private static boolean handleCrimboPresent(TagNode node, TagStatus status) {
    if (!status.crimbo) {
      return false;
    }

    String str = node.getText().toString();

    Matcher matcher = FightRequest.CRIMBO_PATTERN.matcher(str);
    if (!matcher.find()) {
      return false;
    }

    FightRequest.logText(matcher.group(0), status);

    return true;
  }

  public static final Pattern CHAKRA_PATTERN = Pattern.compile("This Chakra is now (\\d+)% clean.");

  private static boolean handleChakra(TagNode node, TagStatus status) {
    String str = node.getText().toString();

    Matcher matcher = FightRequest.CHAKRA_PATTERN.matcher(str);
    if (!matcher.find()) {
      return false;
    }

    int cleanliness = StringUtilities.parseInt(matcher.group(1));

    String setting =
        status.location.equals("Your Bung Chakra")
            ? "crimbo16BungChakraCleanliness"
            : status.location.equals("Your Guts Chakra")
                ? "crimbo16GutsChakraCleanliness"
                : status.location.equals("Your Liver Chakra")
                    ? "crimbo16LiverChakraCleanliness"
                    : status.location.equals("Your Nipple Chakra")
                        ? "crimbo16NippleChakraCleanliness"
                        : status.location.equals("Your Nose Chakra")
                            ? "crimbo16NoseChakraCleanliness"
                            : status.location.equals("Your Hat Chakra")
                                ? "crimbo16HatChakraCleanliness"
                                : status.location.equals("Crimbo's Sack")
                                    ? "crimbo16SackChakraCleanliness"
                                    : status.location.equals("Crimbo's Boots")
                                        ? "crimbo16BootsChakraCleanliness"
                                        : status.location.equals("Crimbo's Jelly")
                                            ? "crimbo16JellyChakraCleanliness"
                                            : status.location.equals("Crimbo's Reindeer")
                                                ? "crimbo16ReindeerChakraCleanliness"
                                                : status.location.equals("Crimbo's Beard")
                                                    ? "crimbo16BeardChakraCleanliness"
                                                    : status.location.equals("Crimbo's Hat")
                                                        ? "crimbo16CrimboHatChakraCleanliness"
                                                        : null;

    if (setting != null) {
      Preferences.setString(setting, matcher.group(1));
    }

    FightRequest.logText(str, status);

    return true;
  }

  private static void handleVillainLairRadio(TagNode node, TagStatus status) {
    if (!KoLCharacter.inBondcore()) {
      return;
    }
    if (!status.location.equals("Super Villain's Lair")) {
      return;
    }
    String str = node.getText().toString();
    if (str.contains("crackle")) {
      FightRequest.logText(str);
      VillainLairDecorator.parseColorClue(str);
      return;
    }
  }

  private static boolean handleProselytization(TagNode node, TagStatus status) {
    String str = node.getText().toString();

    Matcher matcher = FightRequest.PROSELYTIZATION_PATTERN.matcher(str);
    if (!matcher.find()) {
      return false;
    }

    StringBuffer action = new StringBuffer(status.monsterName);
    action.append(" proselytized for the ");
    action.append(matcher.group(1));
    action.append(" faction.");

    FightRequest.logText(action, status);

    return true;
  }

  private static void processComments(TagNode node, TagStatus status) {
    Iterator<? extends BaseToken> it = node.getAllChildren().iterator();
    while (it.hasNext()) {
      BaseToken child = it.next();

      if (child instanceof CommentNode) {
        CommentNode object = (CommentNode) child;
        FightRequest.processComment(object, status);
      }
    }
  }

  private static void processComment(CommentNode object, TagStatus status) {
    String content = object.getContent().trim();
    if (content.equals("familiarmessage")) {
      status.famaction = true;
    } else if (content.equals("WINWINWIN")) {
      FightRequest.logText(status.name + " wins the fight!", status);
      FightRequest.currentRound = 0;
      FightRequest.won = true;
      status.won = true;
    } else if (content.startsWith("MONSTERID:")) {
      String idstr = content.substring(10).trim();
      int lastMonsterId = status.monsterId;
      int monsterId = StringUtilities.parseInt(idstr);
      status.monsterId = monsterId;

      // If this is a change, transform the monster
      if (lastMonsterId != monsterId) {
        MonsterData monster = MonsterDatabase.findMonsterById(monsterId);

        if (monster == null) {
          return;
        }

        monster = monster.transform();
        MonsterStatusTracker.setNextMonster(monster);
      }
    }
    // macroaction: comment handled elsewhere
  }

  private static void processFamiliarAction(TagNode node, TagNode inode, TagStatus status) {
    status.famaction = false;

    String image = imgToString(inode);

    StringBuffer action = status.action;

    // <img src="http://images.kingdomofloathing.com/itemimages/familiar6.gif" width=30
    // height=30></td><td valign=center>Jiggly Grrl disappears into the wardrobe, and emerges
    // dressed as a pair of Fuzzy Dice.

    // If you have a tiny costume wardrobe or a doppelshifter, it
    // can change its image mid-battle.
    if (status.doppel && !image.equals("")) {
      status.setFamiliar(image);
    }

    // Preprocess this node: remove tables and process them later.
    // This will also remove the table text from the node text and
    // thus improve the message we log.

    TagNode[] tables = node.getElementsByName("table", true);
    for (int i = 0; i < tables.length; ++i) {
      TagNode table = tables[i];
      table.getParent().removeChild(table);
    }

    // Always separate multiple lines with slashes
    StringBuffer text = new StringBuffer();
    FightRequest.extractVerse(node, text, null);
    String str = text.toString();

    // Lovebugs are tagged as <!--familiarmessage--> and can remove evil
    if (FightRequest.handleEvilometerLovebug(str)) {
      return;
    }

    // For some unknown reason, gladiator moves are tagged as <!--familiarmessage-->
    if (str.startsWith("New Special Move Unlocked")) {
      TagNode bnode = node.findElementByName("b", true);
      if (bnode != null) {
        FightRequest.logText(text, status);
        String skill = bnode.getText().toString();
        ResponseTextParser.learnCombatMove(skill);
      }
      return;
    }

    if (str.equals("Your hat gets bigger!")) {
      // Upgraded hat in Avatar of West of Loathing
      AdventureResult oldHat = EquipmentManager.getEquipment(EquipmentManager.HAT);
      // The hats are in sequential item id order, and you can only upgrade 1 level per combat
      AdventureResult newHat = ItemPool.get(oldHat.getItemId() + 1, 1);
      EquipmentManager.transformEquipment(oldHat, newHat);
      return;
    }

    if (image.equals(status.familiar) && FightRequest.handleGhostOfCommerce(str, status)) {
      return;
    }

    if (FightRequest.handleFamiliarScrapbook(str, status)) {
      return;
    }

    if (str.contains("takes a pull on the hookah")) {
      status.hookah = true;
    }

    if (!str.equals("") && !ResultProcessor.processFamiliarWeightGain(str)) {
      // Familiar combat action?
      // Don't log most familiar actions in the Deep Machine Tunnels
      if (status.logFamiliar
          && (!FightRequest.machineElf || str.contains("time starts passing again"))) {
        FightRequest.logText(text, status);
      }

      if (str.contains("(to Clan Floundry)")) {
        AdventureResult.addResultToList(KoLConstants.tally, GoalManager.GOAL_FLOUNDRY);
        GoalManager.updateProgress(GoalManager.GOAL_FLOUNDRY);
      }

      int damage = FightRequest.parseVerseDamage(inode);
      if (damage == 0) {
        damage = FightRequest.parseNormalDamage(str);
      }

      if (damage != 0) {
        FightRequest.logMonsterAttribute(status, damage, HEALTH);
        MonsterStatusTracker.damageMonster(damage);
      }

      FightRequest.specialFamiliarDamage(text, status);
    }

    // Now process additional familiar actions
    for (int i = 0; i < tables.length; ++i) {
      TagNode table = tables[i];
      FightRequest.processNode(table, status);
    }
  }

  private static boolean handleFuzzyDice(String content, TagStatus status) {
    if (status.diceMessage == null) {
      return false;
    }

    if (content.startsWith(status.diceMessage)) {
      status.dice = true;
      return true;
    }

    if (!status.dice) {
      return false;
    }

    if (content.equals("&nbsp;&nbsp;&nbsp;&nbsp;") || content.equals("")) {
      return true;
    }

    // We finally have the whole message.
    StringBuffer action = status.action;
    action.setLength(0);
    action.append(status.diceMessage);
    action.append(" ");
    action.append(" ");
    action.append(content);

    if (status.logFamiliar) {
      FightRequest.logText(action, status);
    }

    // No longer accumulating fuzzy dice message
    status.dice = false;

    // Fuzzy dice can do damage. Account for it.
    int damage = FightRequest.parseNormalDamage(content);
    if (damage != 0) {
      FightRequest.logMonsterAttribute(status, damage, HEALTH);
      MonsterStatusTracker.damageMonster(damage);
    }

    return true;
  }

  public static final Pattern SPIT_PATTERN = Pattern.compile("\\((\\d+)% full\\)");

  private static void handleMelodramedary(String str, TagStatus status) {
    // You hear a loud <i>schlurrrrrk!</i> noise, and turn to see Gogarth sucking the liquid out of
    // a dented beer keg he found somewhere. (20% full)
    if (str.contains("smiles at you")) {
      // Don't log stat gain bonus
      return;
    }

    if (str.contains("spits a tremendous globule of saliva at your foe")) {
      Preferences.setInteger("camelSpit", 0);
    }

    if (str.contains("obligingly -- ")) {
      str = str + " too obligingly -- spits in your face.";
      Preferences.setInteger("camelSpit", 0);
    }

    if (str.contains("sucking the liquid")) {
      str = "You hear a loud schlurrrrrk! " + str;
      Matcher m = SPIT_PATTERN.matcher(str);
      if (m.find()) {
        Preferences.setInteger("camelSpit", StringUtilities.parseInt(m.group(1)));
      }
    }
    FightRequest.logText(str, status);
  }

  private static boolean processFumble(String text, TagStatus status) {
    Matcher m = FightRequest.FUMBLE_PATTERN.matcher(text);

    if (m.find()) {
      String message = "You lose " + m.group(1) + " hit points";
      FightRequest.logPlayerAttribute(status, text);
      return true;
    }

    return false;
  }

  private static boolean handleEvilometer(String text, TagStatus status) {
    if (!text.contains("Evilometer")
        && !text.contains("ghost vacuum")
        && !text.contains("gravy sloshes")
        && !text.contains("the nightmare fuel")
        && !text.contains("an evil draft blows")) {
      return false;
    }

    FightRequest.logText(text, status);

    MonsterData monster = status.monster;
    String setting = getEvilZoneSetting(monster);

    if (setting == null) {
      return false;
    }

    int evilness =
        text.contains("a single beep")
            ? 1
            : text.contains("beeps three times")
                ? 3
                : text.contains("three quick beeps")
                    ? 3
                    : text.contains("five quick beeps")
                        ? 5
                        : text.contains("loud") ? Preferences.getInteger(setting) : 0;

    if (text.contains("ghost vacuum sucks up some extra evil")) {
      evilness++;
    }

    if (text.contains("Some gravy sloshes")) {
      evilness++;
    }

    // You trench cape ripples as an evil draft blows and then quiets, it feels less evil in here!
    if (text.contains("an evil draft blows")) {
      evilness++;
    }

    // The evil of the nightmare fuel in your system is in a different phase
    // from the evil emanations in this area, so they cancel each other out a bit.
    if (text.contains("the nightmare fuel")) {
      evilness += 2;
      Preferences.decrement("_nightmareFuelCharges");
    }

    if (evilness == 0) {
      Matcher m = BEEP_PATTERN.matcher(text);
      if (!m.find()) {
        return false;
      }
      evilness = StringUtilities.parseInt(m.group(1));
    }

    Preferences.decrement(setting, evilness, 0);
    Preferences.decrement("cyrptTotalEvilness", evilness, 0);

    return true;
  }

  private static String getEvilZoneSetting(final Function<String, Boolean> isLocation) {
    for (String[] data : FightRequest.EVIL_ZONES) {
      if (isLocation.apply(data[0])) {
        return data[1];
      }
    }

    return null;
  }

  private static String getEvilZoneSetting(final String location) {
    return getEvilZoneSetting(l -> l.equals(location));
  }

  private static String getEvilZoneSetting(final MonsterData monster) {
    return getEvilZoneSetting(
        l -> AdventureDatabase.getAdventure(l).getAreaSummary().hasMonster(monster));
  }

  private static String getEvilZoneSetting() {
    return getEvilZoneSetting(KoLAdventure.lastLocationName);
  }

  private static boolean handleEvilometerLovebug(final String text) {
    if (text.contains("Evilometer beeps once")) {
      String setting = getEvilZoneSetting();
      if (setting != null) {
        int evilness = 1;
        // If you have a gravy boat and Lovebugs, the gravy boat message ends up in the same node as
        // the lovebug message
        if (text.contains("Some gravy sloshes")) {
          evilness++;
        }

        Preferences.decrement(setting, evilness, 0);
        Preferences.decrement("cyrptTotalEvilness", evilness, 0);
        return true;
      }
    }
    return false;
  }

  private static boolean handleFamiliarScrapbook(final String text, TagStatus status) {
    if (
    // All stages
    text.contains("for your scrapbook")
        ||
        // Start of combat
        text.contains("You snap a picture")
        || text.contains("you just <i>have</i> to take a picture")
        || text.contains("show this photo to your grandkids")
        ||
        // Mid combat
        text.contains("too good not to photograph")
        || text.contains("for the scrapbook")
        || text.contains("You take a picture")
        || text.contains("You scrap a picture")
        ||
        // End of combat
        text.contains("more interesting photograph")
        || text.contains("both in a photograph")
        || text.contains("for the ol' scrapbook")) {
      Preferences.increment("scrapbookCharges");
      return true;
    }
    return false;
  }

  private static boolean handleKeyotron(String text, TagStatus status) {
    if (!text.contains("key-o-tron")) {
      return false;
    }

    // Your key-o-tron emits 2 short tones, indicating that it has successfully processed biometric
    // data from this subject.
    // Your key-o-tron emits a short buzz, indicating that it has already collected enough biometric
    // data of this type.

    if (text.contains("already collected")) {
      // Synchronize in case played turns out of KoLmafia
      Object[] data = BugbearManager.bugbearToData(status.monsterName);
      BugbearManager.setBiodata(data, BugbearManager.dataToLevel(data) * 3);
      return true;
    }

    FightRequest.logText(text, status);

    Matcher matcher = FightRequest.KEYOTRON_PATTERN.matcher(text);
    if (!matcher.find()) {
      return true;
    }

    Object[] data = BugbearManager.bugbearToData(status.monsterName);
    BugbearManager.setBiodata(data, matcher.group(1));

    return true;
  }

  private static boolean handleSeahorse(String text, TagStatus status) {
    if (!status.seahorse || !text.startsWith("I shall name you")) {
      return false;
    }

    Matcher m = FightRequest.SEAHORSE_PATTERN.matcher(text);
    if (m.find()) {
      String name = m.group(1);
      Preferences.setString("seahorseName", name);
      StringBuilder buffer = new StringBuilder();
      buffer.append("Seahorse name: ");
      buffer.append(name);
      FightRequest.logText(buffer, status);
    }

    return true;
  }

  private static boolean handleMayoWasp(String text, TagStatus status) {
    if (!status.mayowasp || !text.startsWith("The mayo wasp sniffs at you for a minute")) {
      return false;
    }

    // This counter is not added to resetCounters() as it persists across ascension
    TurnCounter.startCounting(300, "Mmmmmmayonnaise window begin loc=* type=wander", "lparen.gif");
    TurnCounter.startCounting(400, "Mmmmmmayonnaise window end loc=* type=wander", "rparen.gif");

    FightRequest.logText("The mayo wasp deposits an egg in your abdomen!", status);
    return true;
  }

  private static boolean handleSpelunky(String text, TagStatus status) {
    if (status.limitmode != Limitmode.SPELUNKY) {
      return false;
    }

    // Non-damage messages that should be logged
    if (text.startsWith("You leap out of your foe's reach")
        || text.startsWith("With your swoopy cape and your springy boots")
        || text.startsWith("With your spring-soled boots you leap")
        || text.startsWith("A bird stole")
        || text.startsWith("He struggles against your rope")
        || text.startsWith("It finally breaks free of your rope")) {
      FightRequest.logText(text, status);
      return true;
    }

    // Finding combat supplies and gold
    if (text.startsWith("By the light of your torch")
        || text.startsWith("By the light of your helmet")) {
      FightRequest.logText(text, status);
      if (text.contains("you find a bomb!")) {
        FightRequest.logText("You find a bomb", status);
      }
      if (text.contains("you find a rope!")) {
        FightRequest.logText("You find a rope", status);
      }
      return true;
    }

    // Damage messages that should logged
    if (text.startsWith("You blast upwards with your jetpack")
        || text.startsWith("You leap into the air")
        || text.startsWith("You leap on your foe with your spiked boots")
        || text.startsWith("You thrust your torch at your foe")
        || text.startsWith("Your skeleton buddy punches your foe")
        || text.startsWith("Enraged, your skeleton buddy pummels Yomama")
        || text.startsWith("Yomama belches up some smoke")) {
      FightRequest.logText(text, status);
      return false;
    }

    return false;
  }

  private static boolean handleSpelunkyGold(String image, String str, TagStatus status) {
    if (status.limitmode != Limitmode.SPELUNKY) {
      return false;
    }

    if (!image.equals("goldnug.gif")
        && !image.equals("coinpurse.gif")
        && !image.equals("lolmecidol.gif")) {
      return false;
    }

    Matcher m = INT_PATTERN.matcher(str);
    if (m.find()) {
      FightRequest.logText("You gain " + m.group() + " gold", status);
      return true;
    }

    return false;
  }

  private static boolean handleFamiliarInteraction(String text, TagStatus status) {
    if (status.logFamiliar
        && status.enthronedName != null
        && status.bjornedName != null
        && text.startsWith(status.enthronedName)
        && text.contains(status.bjornedName)) {
      FightRequest.logText(text, status);
      return true;
    }
    return false;
  }

  private static void handleRaver(String text, TagStatus status) {
    if (status.ravers && NemesisDecorator.specialRaverMove(text)) {
      StringBuilder buffer = new StringBuilder();
      buffer.append(status.monsterName);
      buffer.append(" uses a special move!");
      FightRequest.logText(buffer, status);
    }
  }

  private static void handleYearbookCamera(TagStatus status) {
    if (!KoLCharacter.inHighschool()) {
      return;
    }
    Preferences.setBoolean("yearbookCameraPending", true);

    StringBuilder buffer = new StringBuilder();
    buffer.append(status.monsterName);
    buffer.append(" photographed for Yearbook Club");
    FightRequest.logText(buffer, status);
  }

  private static final Pattern[] GHOST_OF_COMMERCE_QUEST = {
    Pattern.compile("Better get an? (.*?) while there's still some left!"),
    Pattern.compile("(?:B|b)uy an? (.*?) before they all sell out"),
    Pattern.compile("Don't forget to buy an? (.*?)!"),
    Pattern.compile("Did you buy an? (.*?) yet\\?"),
    Pattern.compile("Hey pal, you should buy an? (.*?)!"),
    Pattern.compile("Quick, buy an? (.*?)!"),
    Pattern.compile("Buy an? (.*?)!"),
  };

  private static final Pattern[] GHOST_OF_COMMERCE_QUEST_COMPLETE = {
    Pattern.compile("Nice, you bought an? (.*?)!"),
    Pattern.compile("Oh,? good, you got an? (.*?)(?: before they sold out)?!"),
  };

  private static boolean handleGhostOfCommerce(String text, TagStatus status) {
    if (!status.familiar.equals("cghost_commerce.gif")) {
      return false;
    }

    for (Pattern p : GHOST_OF_COMMERCE_QUEST) {
      Matcher matcher = p.matcher(text);
      if (matcher.find()) {
        String itemName = matcher.group(1);
        Preferences.setString("commerceGhostItem", itemName);
        return true;
      }
    }

    for (Pattern p : GHOST_OF_COMMERCE_QUEST_COMPLETE) {
      Matcher matcher = p.matcher(text);
      if (matcher.find()) {
        Preferences.setString("commerceGhostItem", "");
        return true;
      }
    }

    return false;
  }

  private static void logText(StringBuilder buffer, final TagStatus status) {
    FightRequest.logText(buffer.toString(), status);
  }

  private static void logText(StringBuffer buffer, final TagStatus status) {
    FightRequest.logText(buffer.toString(), status);
  }

  private static void logText(String text, final TagStatus status) {
    if (text.equals("")) {
      return;
    }

    StringBuffer action = status.action;
    FightRequest.getRound(action);
    action.append(text);
    FightRequest.logText(action);
  }

  private static void logText(StringBuilder buffer) {
    FightRequest.logText(buffer.toString());
  }

  private static void logText(StringBuffer buffer) {
    FightRequest.logText(buffer.toString());
  }

  private static void logText(String text) {
    if (text.equals("")) {
      return;
    }

    text = StringUtilities.globalStringReplace(text, "<br>", " / ");
    text = KoLConstants.ANYTAG_PATTERN.matcher(text).replaceAll(" ");
    text = StringUtilities.globalStringDelete(text, "&nbsp;");
    text = StringUtilities.globalStringReplace(text, "  ", " ");

    // Use of a tiny goth giant can cause a <br> at the start of a message
    if (text.contains(": / ")) {
      text = text.replaceFirst(": / ", ": ");
    }

    RequestLogger.printLine(text);
    RequestLogger.updateSessionLog(text);
  }

  public static final void clearInstanceData() {
    FightRequest.clearInstanceData(false);
  }

  private static void clearInstanceData(final boolean transform) {
    FightRequest.transformed = transform;
    FightRequest.fightFollowsChoice = false;

    FightRequest.castNoodles = false;
    FightRequest.castClubFoot = false;
    FightRequest.castShellUp = false;
    FightRequest.castTerracottaArmy = false;
    FightRequest.castParaffinPrism = false;
    FightRequest.castAccordionBash = false;
    FightRequest.castCleesh = false;
    FightRequest.insultedPirate = false;
    FightRequest.usedFlyer = false;
    FightRequest.usedBasePair = false;
    FightRequest.canOlfact = true;
    FightRequest.jiggledChefstaff = false;
    FightRequest.handledCan = false;
    FightRequest.shotSixgun = false;
    FightRequest.squeezedStressBall = false;
    FightRequest.canStomp = false;
    FightRequest.desiredScroll = null;
    FightRequest.won = false;
    FightRequest.startingAttack = 0;

    // In Ed we'll only clear the monster status when we have won or abandoned the fight
    if (!KoLCharacter.isEd() || Preferences.getInteger("_edDefeats") == 0) {
      MonsterStatusTracker.reset();
    }

    if (transform) {
      return;
    }

    KoLCharacter.resetEffectiveFamiliar();
    EncounterManager.ignoreSpecialMonsters = false;

    // Do not clear the following, since they are looked at after combat finishes.
    // FightRequest.haiku = false;
    // FightRequest.anapest = false;

    FightRequest.nextAction = null;
    FightRequest.macroErrorMessage = null;

    FightRequest.currentRound = 0;
    FightRequest.preparatoryRounds = 0;
    FightRequest.macroPrefixLength = 0;
    FightRequest.consultScriptThatDidNothing = null;

    if (FightRequest.initializeAfterFight
        && !FightRequest.inMultiFight
        && !FightRequest.choiceFollowsFight) {
      Runnable initializeRunner =
          new Runnable() {
            public void run() {
              LoginManager.login(KoLCharacter.getUserName());
              FightRequest.initializeAfterFight = false;
            }
          };

      RequestThread.runInParallel(initializeRunner);
    }
  }

  private static long getActionCost() {
    if (FightRequest.nextAction.startsWith("skill")) {
      String skillId = FightRequest.nextAction.substring(5);
      return SkillDatabase.getMPConsumptionById(StringUtilities.parseInt(skillId));
    }

    return 0;
  }

  public static void addItemActionsWithNoCost() {
    KoLCharacter.battleSkillNames.add("item seal tooth");
    KoLCharacter.battleSkillNames.add("item spices");

    KoLCharacter.battleSkillNames.add("item dictionary");
    KoLCharacter.battleSkillNames.add("item jam band flyers");
    KoLCharacter.battleSkillNames.add("item rock band flyers");

    KoLCharacter.battleSkillNames.add("item toy soldier");
    KoLCharacter.battleSkillNames.add("item toy mercenary");

    KoLCharacter.battleSkillNames.add("item Miniborg stomper");
    KoLCharacter.battleSkillNames.add("item Miniborg laser");
    KoLCharacter.battleSkillNames.add("item Miniborg Destroy-O-Bot");

    KoLCharacter.battleSkillNames.add("item naughty paper shuriken");
    KoLCharacter.battleSkillNames.add("item bottle of G&uuml;-Gone");
  }

  private static boolean isItemConsumed(final int itemId, final String responseText) {
    boolean itemSuccess =
        (FightRequest.anapest
                && (responseText.contains("used a thing from your bag")
                    || responseText.contains("item caused something to happen")))
            || (FightRequest.haiku
                && (responseText.contains("do some stuff with a thing")
                    || responseText.contains("some inscrutable end")));

    boolean itemDamageSuccess =
        (FightRequest.anapest
                && (responseText.contains("hurl a thing")
                    || responseText.contains("thing you hold up")
                    || responseText.contains("fling a thing")
                    || responseText.contains("pain with that thing")))
            || (FightRequest.haiku
                && (responseText.contains("like a mighty summer storm")
                    || responseText.contains("whip out a thing")
                    || responseText.contains("Like a killing frost")
                    || responseText.contains("thing you just threw")
                    || responseText.contains("combat items!")
                    || responseText.contains("sling an item")
                    || responseText.contains("item you just threw")
                    || responseText.contains("item just hit")));

    if (itemId == ItemPool.ICEBALL) {
      // First use:
      // You hurl the iceball at your opponent, dealing X damage.
      // Then you pick up the iceball and stick it back in your sack.
      // It feels a little softer than it did before.

      // Second use:
      // You hurl the iceball at your opponent, dealing X damage.
      // When you retrieve it this time, it feels pretty slushy.

      // Third use:
      // You hurl the iceball at your opponent, dealing X damage.
      // Unfortunately, the iceball completely disintegrates on impact.

      if (responseText.contains("back in your sack") || itemDamageSuccess) {
        Preferences.setInteger("_iceballUses", 1);
      } else if (responseText.contains("pretty slushy") || itemDamageSuccess) {
        Preferences.setInteger("_iceballUses", 2);
      } else if (responseText.contains("completely disintegrates") || itemDamageSuccess) {
        Preferences.setInteger("_iceballUses", 3);
        return true;
      }
      return false;
    }

    if (ItemDatabase.getAttribute(itemId, ItemDatabase.ATTR_COMBAT_REUSABLE)) {
      return false;
    }

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = monster != null ? monster.getName() : "";

    // Some monsters block items, but do not destroy them
    if (monsterName.equals("Bonerdagon")) {
      Matcher matcher = FightRequest.BONERDAGON_BLOCK_PATTERN.matcher(responseText);
      if (matcher.find()) {
        if (ItemDatabase.getItemName(itemId).equals(matcher.group(1))) {
          return false;
        }
      }
    } else if (monsterName.equals("Your Shadow")) {
      if (responseText.contains("knocks it out of your hands")) {
        // We can't tell which item is blocked, so assume both if funkslinging
        return false;
      }
    } else if (monsterName.equals("Naughty Sorceress")) {
      Matcher matcher = FightRequest.NS1_BLOCK1_PATTERN.matcher(responseText);
      if (matcher.find()) {
        if (ItemDatabase.getItemName(itemId).equals(matcher.group(1))) {
          return false;
        }
      }
      matcher = FightRequest.NS1_BLOCK2_PATTERN.matcher(responseText);
      if (matcher.find()) {
        if (ItemDatabase.getItemName(itemId).equals(matcher.group(1))) {
          return false;
        }
      }
      matcher = FightRequest.NS1_BLOCK3_PATTERN.matcher(responseText);
      if (matcher.find()) {
        if (ItemDatabase.getItemName(itemId).equals(matcher.group(1))) {
          return false;
        }
      }
    } else if (monsterName.equals("Naughty Sorceress (2)")) {
      Matcher matcher = FightRequest.NS2_BLOCK1_PATTERN.matcher(responseText);
      if (matcher.find()) {
        if (ItemDatabase.getItemName(itemId).equals(matcher.group(1))) {
          return false;
        }
      }
      matcher = FightRequest.NS2_BLOCK2_PATTERN.matcher(responseText);
      if (matcher.find()) {
        if (ItemDatabase.getItemName(itemId).equals(matcher.group(1))) {
          return false;
        }
      }
      matcher = FightRequest.NS2_BLOCK3_PATTERN.matcher(responseText);
      if (matcher.find()) {
        if (ItemDatabase.getItemName(itemId).equals(matcher.group(1))) {
          return false;
        }
      }
    }

    switch (itemId) {
      case ItemPool.COMMUNICATIONS_WINDCHIMES:

        // Only record usage in battle if you got some sort of
        // response.
        //
        // You bang out a series of chimes, (success)
        //   or
        // A nearby hippy soldier sees you about to start
        // ringing your windchimes (failure)
        if (responseText.contains("bang out a series of chimes")
            || responseText.contains("ringing your windchimes")
            || itemSuccess) {
          Preferences.setInteger("lastHippyCall", KoLAdventure.getAdventureCount());
          // "Safe" interval between uses is 10 turns
          // http://alliancefromhell.com/viewtopic.php?t=1398
          TurnCounter.stopCounting("Communications Windchimes");
          TurnCounter.startCounting(10, "Communications Windchimes loc=*", "chimes.gif");
        }

        // Then he takes your windchimes and wanders off.
        if (responseText.contains("he takes your windchimes")) {
          return true;
        }
        return false;

      case ItemPool.PADL_PHONE:

        // Only record usage in battle if you got some sort of
        // response.
        //
        // You punch a few buttons on the phone, (success)
        //   or
        // A nearby frat soldier sees you about to send a
        // message to HQ (failure)
        if (responseText.contains("punch a few buttons on the phone")
            || responseText.contains("send a message to HQ")
            || itemSuccess) {
          Preferences.setInteger("lastFratboyCall", KoLAdventure.getAdventureCount());
          // "Safe" interval between uses is 10 turns
          // http://alliancefromhell.com/viewtopic.php?t=1398
          TurnCounter.stopCounting("PADL Phone");
          TurnCounter.startCounting(10, "PADL Phone loc=*", "padl.gif");
        }

        // Then he takes your phone and wanders off.
        if (responseText.contains("he takes your phone")) {
          return true;
        }
        return false;

      case ItemPool.SAMURAI_TURTLE:

        // The turtle looks at you, shakes his head, bows, and
        // disappears into the shadows. Looks like you asked
        // too much of him.
        if (responseText.contains("disappears into the shadows")) {
          return true;
        }
        return false;

      case ItemPool.HAROLDS_BELL:
        TurnCounter.startCounting(20, "Harold's Bell loc=*", "bell.gif");
        return true;

      case ItemPool.SPOOKY_PUTTY_SHEET:

        // You press the sheet of spooky putty against
        // him/her/it and make a perfect copy, which you shove
        // into your sack. He doesn't seem to appreciate it too
        // much...

        if (responseText.contains("make a perfect copy") || itemSuccess) {
          Preferences.increment("spookyPuttyCopiesMade", 1);
          Preferences.setString("spookyPuttyMonster", monsterName);
          Preferences.setString("autoPutty", "");
          return true;
        }
        if (responseText.contains("too scared to copy any more monsters today")) {
          Preferences.setInteger("spookyPuttyCopiesMade", 5);
        }
        return false;

      case ItemPool.RAIN_DOH_BOX:

        // You push the button on the side of the box.
        // It makes a scary noise, and a tiny, ghostly image
        // of your opponent appears inside it.

        if (responseText.contains("ghostly image of your opponent") || itemSuccess) {
          Preferences.increment("_raindohCopiesMade", 1);
          Preferences.setString("rainDohMonster", monsterName);
          Preferences.setString("autoPutty", "");
          return true;
        }
        if (responseText.contains("too scared to use this box anymore today")) {
          Preferences.setInteger("_raindohCopiesMade", 5);
        }
        return false;

      case ItemPool.CAMERA:

        // With a flash of light and an accompanying old-timey
        // -POOF- noise, you take snap a picture of him. Your
        // camera begins to shake, rattle and roll.

        if (responseText.contains("old-timey <i>-POOF-</i> noise") || itemSuccess) {
          Preferences.setString("cameraMonster", monsterName);
          Preferences.increment("camerasUsed");
          Preferences.setString("autoPutty", "");
          return true;
        }
        return false;

      case ItemPool.CRAPPY_CAMERA:

        // With a dim flash of light and an accompanying old-timey
        // -POOF- noise, you snap a picture of it. Your
        // camera begins to shake, disconcertingly.

        if (responseText.contains("old-timey <i>-POOF-</i> noise") || itemSuccess) {
          Preferences.setString("crappyCameraMonster", monsterName);
          Preferences.setString("autoPutty", "");
          return true;
        }
        return false;

      case ItemPool.UNFINISHED_ICE_SCULPTURE:
        // With a flourish of chisels and chainsaws, you make a
        // passable ice sculpture in the likeness of your foe.
        if (responseText.contains("flourish of chisels") || itemSuccess) {
          Preferences.setString("iceSculptureMonster", monsterName);
          Preferences.setString("autoPutty", "");
          return true;
        }
        return false;

      case ItemPool.PHOTOCOPIER:

        // You open the lid of the photocopier, press it
        // against your opponent, and press the COPY button. He
        // is enraged, and smashes the copier to pieces, but
        // not before it produces a sheet of paper.

        if (responseText.contains("press the COPY button") || itemSuccess) {
          Preferences.setString("photocopyMonster", monsterName);
          Preferences.setString("autoPutty", "");
          return true;
        }
        return false;

      case ItemPool.GREEN_TAFFY:

        // You toss the taffy, and the salt water soaks into it.
        // A green envyfish swims up, listlessly eats it, looks at your opponent,
        // and begins to emit a long sigh.
        // At the conclusion of the sigh, the fish squirts out an egg, then swims off
        // into the distance, eyes downcast.

        // You acquire an item: envyfish egg
        // The school is distracted by a lost clownfish for a moment.

        if (responseText.contains("the fish squirts out an egg") || itemSuccess) {
          Preferences.setString("envyfishMonster", monsterName);
          Preferences.setString("autoPutty", "");
          return true;
        }
        return false;

      case ItemPool.CRAYON_SHAVINGS:

        // You toss the shavings at the bugbear, and when they hit it,
        // something strange happens -- they begin to move of their own
        // accord, melting, running down the bugbear's body in tiny streams.
        // The streams converge on the ground, forming a puddle. After a
        // moment, the puddle gathers itself up into a perfect wax replica
        // of the bugbear. You pick it up for later investigation.

        if (responseText.contains("You toss the shavings")) {
          Preferences.setString("waxMonster", monsterName);
          Preferences.setString("autoPutty", "");
          return true;
        }
        // You throw the handful of wax shavings at your opponent, gumming up
        // all of his bits and making him smell like a Kindergarten classroom.
        else if (responseText.contains("You throw the handful") || itemSuccess) {
          return true;
        }
        return false;

      case ItemPool.STICKY_CLAY_HOMUNCULUS:
        if (responseText.contains("make a crude sculpture") || itemSuccess) {
          Preferences.setString("crudeMonster", monsterName);
          return true;
        }
        return false;

      case ItemPool.CHATEAU_WATERCOLOR:
        if (responseText.contains("Chateau Mantegna approaches") || itemSuccess) {
          Preferences.setString("chateauMonster", monsterName);
          return true;
        }
        return false;

      case ItemPool.PRINT_SCREEN:
        if (responseText.contains("You copy") || itemSuccess) {
          Preferences.setString("screencappedMonster", monsterName);
          return true;
        }
        return false;

      case ItemPool.LOVE_BOOMERANG:
        if (responseText.contains("hurl an enamorrang") || itemSuccess) {
          TurnCounter.stopCounting("Enamorang Monster");
          if (Preferences.getBoolean("stopForFixedWanderer")) {
            TurnCounter.startCounting(15, "Enamorang Monster type=wander", "watch.gif");
          } else {
            TurnCounter.startCounting(15, "Enamorang Monster loc=* type=wander", "watch.gif");
          }
          Preferences.setString("enamorangMonster", monsterName);
          Preferences.increment("_enamorangs");
          return true;
        }

        if (responseText.contains("enamorrang'd someone recently")
            && !TurnCounter.isCounting("Enamorang Monster")) {
          TurnCounter.startCounting(
              0, "Enamorang unknown monster window begin loc=* type=wander", "lparen.gif");
          TurnCounter.startCounting(
              15, "Enamorang unknown monster window end loc=* type=wander", "rparen.gif");
          return false;
        }

        if (responseText.contains("enamorrang'd five times today")) {
          Preferences.setInteger("_enamorangs", 5);
          return false;
        }
        return false;

      case ItemPool.ANTIDOTE: // Anti-Anti-Antidote

        // You quickly quaff the anti-anti-antidote. You feel
        // better.

        return responseText.contains("You quickly quaff") || itemSuccess;

      case ItemPool.GLOB_OF_BLANK_OUT:

        // As you're moseying, you notice that the last of the Blank-Out
        // is gone, and that your hand is finally clean. Yay!

        if (responseText.contains("your hand is finally clean")) {
          Preferences.setInteger("blankOutUsed", 0);
          return true;
        }
        Preferences.increment("blankOutUsed");
        return false;

      case ItemPool.MERKIN_PINKSLIP:

        // You hand him the pinkslip. He reads it, frowns, and
        // swims sulkily away.

        return responseText.contains("swims sulkily away") || itemSuccess;

      case ItemPool.PUMPKIN_BOMB:

        // You light the fuse and toss the pumpkin at your
        // opponent.  After it goes off in a flash of dazzling
        // yellow and flying pumpkin guts, there's nothing left
        // of her but a stain on the ground.

        return responseText.contains("toss the pumpkin") || itemDamageSuccess;

      case ItemPool.GOLDEN_LIGHT:

        // You toss the light at your opponent. It begins to oscillate
        // wildly, growing brighter until it's all you can see, and then
        // fades away, leaving not a trace of your foe behind.
        // Or your foe's behind, for that matter.

        return responseText.contains("toss the light") || itemDamageSuccess;

      case ItemPool.GINGERBREAD_CIGARETTE:

        // You hand the gingerbread man a cigarette. He says "Oh, hey, thank you. That's just what I
        // needed!"
        // and strolls away, puffing merrily and having a slowly but surely deleterious effect on
        // his health.

        return responseText.contains("hand the gingerbread man a cigarette") || itemDamageSuccess;

      case ItemPool.GINGERBREAD_RESTRAINING_ORDER:

        // You read the restraining order to the gingerbread man, and for some reason, he agrees to
        // abide by it.

        return responseText.contains("read the restraining order") || itemSuccess;

      case ItemPool.PEPPERMINT_PARASOL:

        // You hold up the parasol, and a sudden freak gust of wind
        // sends you hurtling through the air to safety.

        if (responseText.contains("sudden freak gust") || itemSuccess) {
          Preferences.increment("_navelRunaways");
          Preferences.increment("parasolUsed");
        }

        // Man. That last gust was more than your parasol could handle.
        // You throw away the shredded (but delicious-smelling) wreck
        // that was once a useful tool.

        if (responseText.contains("You throw away the shredded")) {
          Preferences.setInteger("parasolUsed", 0);
          return true;
        }
        return false;

      default:
        return true;
    }
  }

  private static boolean shouldUseAntidote() {
    if (KoLCharacter.inGLover()) {
      return false;
    }
    if (!KoLConstants.inventory.contains(FightRequest.ANTIDOTE)) {
      return false;
    }
    if (KoLConstants.activeEffects.contains(FightRequest.BIRDFORM)) {
      return false; // can't use items!
    }
    int minLevel = Preferences.getInteger("autoAntidote");
    for (int i = 0; i < KoLConstants.activeEffects.size(); ++i) {
      if (EffectDatabase.getPoisonLevel(KoLConstants.activeEffects.get(i).getName()) <= minLevel) {
        return true;
      }
    }
    return false;
  }

  private static void payActionCost(final String responseText) {
    // If we don't know what we tried, punt now.
    if (FightRequest.nextAction == null || FightRequest.nextAction.equals("")) {
      return;
    }

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = monster != null ? monster.getName() : "";

    switch (KoLCharacter.getEffectiveFamiliar().getId()) {
      case FamiliarPool.BLACK_CAT:
        // If we are adventuring with a Black Cat, she might
        // prevent skill and item use during combat.

        // <Name> jumps onto the keyboard and causes you to
        // accidentally hit the Attack button instead of using
        // that skill.

        if (responseText.contains("jumps onto the keyboard")) {
          FightRequest.nextAction = "attack";
          return;
        }

        // Just as you're about to use that item, <name> bats
        // it out of your hand, and you have to spend the next
        // couple of minutes fishing it out from underneath a
        // couch. It's as adorable as it is annoying.

        if (responseText.contains("bats it out of your hand")) {
          return;
        }
        break;

      case FamiliarPool.OAF:
        // If we are adventuring with a O.A.F., it might
        // prevent skill and item use during combat.

        // Use of that skill has been calculated to be
        // sub-optimal. I recommend that you attack with your
        // weapon, instead.

        // Use of that item has been calculated to be
        // sub-optimal. I recommend that you attack with your
        // weapon, instead.

        if (responseText.contains("calculated to be sub-optimal")) {
          FightRequest.nextAction = "attack";
          return;
        }

        break;
    }

    if (FightRequest.nextAction.equals("attack")
        || FightRequest.nextAction.equals("runaway")
        || FightRequest.nextAction.equals("abort")
        || FightRequest.nextAction.equals("steal")
        ||
        // If we have Cunctatitis and decide to procrastinate,
        // we did nothing
        (KoLConstants.activeEffects.contains(FightRequest.CUNCTATITIS)
            && responseText.contains("You decide to"))) {
      return;
    }

    if (FightRequest.nextAction.equals("jiggle")) {
      FightRequest.jiggledChefstaff = true;

      boolean jiggleSuccess =
          (FightRequest.anapest && responseText.contains("hold up your staff"))
              || (FightRequest.haiku && responseText.contains("jiggle a stick"))
              || (FightRequest.machineElf && responseText.contains("line of power"));

      int staffId = EquipmentManager.getEquipment(EquipmentManager.WEAPON).getItemId();
      switch (staffId) {
        case ItemPool.STAFF_OF_LIFE:
          // You jiggle the staff. There is a weak coughing sound,
          // and a little puff of flour shoots out of the end of it.
          // Oh well. You rub the flour on some of your wounds, and it helps a little.
          if (responseText.contains("weak coughing sound")) {
            Preferences.setInteger("_jiggleLife", 5);
          } else {
            Preferences.increment("_jiggleLife", 1);
          }
          break;

        case ItemPool.STAFF_OF_CHEESE:
          // You jigle your staff, and a whirling wheel of cheese appears before you.
          // It bursts open, revealing the stench of untold aeons. It first turns gray,
          // then turns green, then turns tail and runs. You won't see it again for a while, that's
          // for sure.
          if (responseText.contains("turns tail and runs") || jiggleSuccess) {
            Preferences.increment("_jiggleCheese", 1);
            BanishManager.banishCurrentMonster("staff of the standalone cheese");
          }
          break;

        case ItemPool.STAFF_OF_STEAK:
          // You jiggle the staff, but there are no theatrics.
          // Just a squirt of nasty congealed grease, which surprises
          // and displays your opponent to the tune of XXX damage.
          if (responseText.contains("no theatrics")) {
            Preferences.setInteger("_jiggleSteak", 5);
          } else {
            Preferences.increment("_jiggleSteak", 1);
          }
          break;

        case ItemPool.STAFF_OF_CREAM:
          // You jiggle the staff. A wisp of creamy ghostly energy
          // drifts out of the end, into your opponent, then into your head.
          // Your mind fills with it essence. You... know it. With a capital K.
          if (responseText.contains("Your mind fills") || jiggleSuccess) {
            Preferences.increment("_jiggleCream", 1);
            Preferences.setString("_jiggleCreamedMonster", monsterName);
          }
          break;
      }

      return;
    }

    // Non damaging skills all have the same success messages for Anapest or Haiku
    boolean skillSuccess =
        (FightRequest.anapest && responseText.contains("skills don't have to cause pain"))
            || (FightRequest.haiku && responseText.contains("accomplish something"))
            || (FightRequest.machineElf && responseText.contains("You reveal your"));
    boolean familiarSkillSuccess =
        (FightRequest.anapest
                && (responseText.contains("familiar did something")
                    || responseText.contains("pet did a thing")))
            || (FightRequest.haiku
                && (responseText.contains("wish you had just seen")
                    || responseText.contains("what did your familiar do")
                    || responseText.contains("familiar does something")
                    || responseText.contains("you don't see what it does")
                    || responseText.contains("you missed what it did")))
            || (FightRequest.machineElf && responseText.contains("You reveal your"));
    boolean skillRunawaySuccess =
        (FightRequest.anapest && responseText.contains("wings on your heels"))
            || (FightRequest.haiku
                && (responseText.contains("burps taste like pride")
                    || responseText.contains("beat a retreat")))
            || responseText.contains("throws a smoke ball on the ground")
            || (FightRequest.machineElf && responseText.contains("you are no longer anywhere"));

    if (!FightRequest.nextAction.startsWith("skill")) {
      // In Beecore, using a B-item in combat fails. Even if
      // funkslinging with a non-B item, neither item is
      // consumed.

      if (KoLCharacter.inBeecore() && responseText.contains("You are too scared of Bs")) {
        FightRequest.nextAction = "abort";
        return;
      }

      String item1 = FightRequest.nextAction;
      String item2 = null;

      int commaIndex = item1.indexOf(",");

      if (commaIndex != -1) {
        item1 = FightRequest.nextAction.substring(0, commaIndex);
        item2 = FightRequest.nextAction.substring(commaIndex + 1);
      }

      int id1 = StringUtilities.parseInt(item1);
      int id2 = StringUtilities.parseInt(item2);
      FightRequest.payItemCost(id1, -1, responseText);

      if (item2 != null) {
        FightRequest.payItemCost(id2, id1, responseText);
      }

      return;
    }

    if (responseText.contains("You don't have that skill")) {
      return;
    }

    int skillId = StringUtilities.parseInt(FightRequest.nextAction.substring(5));
    long mpCost = SkillDatabase.getMPConsumptionById(skillId);

    if (mpCost > 0) {
      ResultProcessor.processResult(new AdventureLongCountResult(AdventureResult.MP, 0 - mpCost));
    }
    SkillDatabase.registerCasts(skillId, 1);

    // As you're preparing to use that skill, The Bonerdagon
    // suddenly starts furiously beating its wings. You're knocked
    // over by the gust of wind it creates, and lose track of what
    // you're doing.

    if (responseText.contains("Bonerdagon suddenly starts furiously beating its wings")) {
      return;
    }

    switch (skillId) {
      case SkillPool.GOTHY_HANDWAVE:
        NemesisDecorator.useGothyHandwave(monsterName, responseText);
        break;

      case SkillPool.VOLCANOMETEOR:
        ResultProcessor.processItem(ItemPool.VOLCANIC_ASH, -1);
        break;

      case SkillPool.ENTANGLING_NOODLES:
      case SkillPool.SHADOW_NOODLES:
        FightRequest.castNoodles = true;
        return;

      case SkillPool.CLUBFOOT:
        FightRequest.castClubFoot = true;
        return;

      case SkillPool.SHELL_UP:
        FightRequest.castShellUp = true;
        return;

      case SkillPool.TERRACOTTA_ARMY:
        FightRequest.castTerracottaArmy = true;
        return;

      case SkillPool.PARAFFIN_PRISM:
        FightRequest.castParaffinPrism = true;
        return;

      case SkillPool.ACCORDION_BASH:
        FightRequest.castAccordionBash = true;
        return;

      case SkillPool.MAYFLY_SWARM:
        if (responseText.contains("mayfly bait and swing it")
            || responseText.contains("May flies when")
            || responseText.contains("mayflies buzz in")
            || responseText.contains("mayflies, with bait")
            || responseText.contains("mayflies respond")
            || skillSuccess) {
          Preferences.increment("_mayflySummons", 1);
          Preferences.increment(
              "mayflyExperience", responseText.contains("mayfly aphrodisiac") ? 2 : 1);
        }
        break;

      case SkillPool.VICIOUS_TALON_SLASH:
      case SkillPool.WING_BUFFET:
        Preferences.increment("birdformRoc", 1);
        break;

      case SkillPool.TUNNEL_UP:
        Preferences.increment("moleTunnelLevel", 1);
        break;

      case SkillPool.TUNNEL_DOWN:
        Preferences.increment("moleTunnelLevel", -1);
        break;

      case SkillPool.RISE_FROM_YOUR_ASHES:
        Preferences.increment("birdformHot", 1);
        break;

      case SkillPool.ANTARCTIC_FLAP:
        Preferences.increment("birdformCold", 1);
        break;

      case SkillPool.STATUE_TREATMENT:
        Preferences.increment("birdformStench", 1);
        break;

      case SkillPool.FEAST_ON_CARRION:
        Preferences.increment("birdformSpooky", 1);
        break;

      case SkillPool.GIVE_OPPONENT_THE_BIRD:
        Preferences.increment("birdformSleaze", 1);
        break;

      case SkillPool.HOBO_JOKE:
        Modifiers.overrideModifier("Generated:fightMods", "Meat Drop: +100");
        KoLCharacter.recalculateAdjustments();
        KoLCharacter.updateStatus();
        break;

      case SkillPool.HOBO_DANCE:
        Modifiers.overrideModifier("Generated:fightMods", "Item Drop: +100");
        KoLCharacter.recalculateAdjustments();
        KoLCharacter.updateStatus();
        break;

      case SkillPool.BOXING_GLOVE_ARROW:
        Preferences.increment("_boxingGloveArrows", 1);
        break;

      case SkillPool.POISON_ARROW:
        Preferences.increment("_poisonArrows", 1);
        break;

      case SkillPool.FINGERTRAP_ARROW:
        Preferences.increment("_fingertrapArrows", 1);
        break;

      case SkillPool.SQUEEZE_STRESS_BALL:
        FightRequest.squeezedStressBall = true;
        Preferences.increment("_stressBallSqueezes", 1);
        return;

      case SkillPool.RELEASE_BOOTS:
        FightRequest.canStomp = false;
        Preferences.increment("_bootStomps", 1);
        break;

      case SkillPool.SIPHON_SPIRITS:
        Preferences.increment("_mediumSiphons", 1);
        KoLCharacter.setFamiliarImage("medium_0.gif");
        FamiliarData familiar = KoLCharacter.getEffectiveFamiliar();
        familiar.setCharges(0);
        break;

      case SkillPool.SHRAP:
        ResultProcessor.removeItem(ItemPool.WARBEAR_WHOSIT);
        break;

      case SkillPool.GET_A_GOOD_WHIFF:
        if (responseText.contains("floats over your opponent") || familiarSkillSuccess) {
          Preferences.setString("nosyNoseMonster", monsterName);
        }
        break;

      case SkillPool.MATING_CALL:
        if (responseText.contains("bellow the eerie mating call") || skillSuccess) {
          Preferences.setString("_gallapagosMonster", monsterName);
        }
        break;

      case SkillPool.MAKE_FRIENDS:
        if (responseText.contains("you become fast friends") || skillSuccess) {
          Preferences.setString("makeFriendsMonster", monsterName);
        }
        break;

      case SkillPool.BADLY_ROMANTIC_ARROW:
      case SkillPool.WINK:
        if (responseText.contains("fires a badly romantic")
            || responseText.contains("You point a finger")
            || familiarSkillSuccess) {
          boolean hasQuake =
              (KoLCharacter.getFamiliar().getId() == FamiliarPool.REANIMATOR)
                  || EquipmentManager.getFamiliarItem().getItemId() == ItemPool.QUAKE_OF_ARROWS;
          int fights = hasQuake ? 3 : 2;
          Preferences.setInteger("_romanticFightsLeft", fights);
          Preferences.setString("romanticTarget", monsterName);
          Preferences.increment("_badlyRomanticArrows", 1);

          TurnCounter.stopCounting("Romantic Monster window begin");
          TurnCounter.stopCounting("Romantic Monster window end");
          TurnCounter.startCountingTemporary(
              15, "Romantic Monster window begin loc=*", "lparen.gif");
          TurnCounter.startCountingTemporary(
              25, "Romantic Monster window end loc=* type=wander", "rparen.gif");
        }
        break;

      case SkillPool.OLFACTION:
        if (responseText.contains("fill your entire being") || skillSuccess) {
          Preferences.setString("olfactedMonster", monsterName);
          Preferences.setString("autoOlfact", "");
          FightRequest.canOlfact = false;
        }
        break;

      case SkillPool.LONG_CON:
        if (responseText.contains("memorize some important details") || skillSuccess) {
          Preferences.setString("longConMonster", monsterName);
          Preferences.increment("_longConUsed");
        }
        break;

        // Banishing Shout has lots of success messages.  Check for the failure message instead
      case SkillPool.BANISHING_SHOUT:
        if (!responseText.contains("but this foe refuses")) {
          BanishManager.banishMonster(monsterName, "banishing shout");
        }
        break;

      case SkillPool.HOWL_ALPHA:
        if (responseText.contains("your opponent turns and runs") || skillRunawaySuccess) {
          BanishManager.banishMonster(monsterName, "howl of the alpha");
        }
        break;

      case SkillPool.CREEPY_GRIN:
        if (responseText.contains("an even creepier grin") || skillRunawaySuccess) {
          Preferences.setBoolean("_vmaskBanisherUsed", true);
          BanishManager.banishMonster(monsterName, "v for vivala mask");
        }
        break;

      case SkillPool.STINKEYE:
        if (responseText.contains("You fix an extremely disdainful eye") || skillRunawaySuccess) {
          Preferences.setBoolean("_stinkyCheeseBanisherUsed", true);
          BanishManager.banishMonster(monsterName, "stinky cheese eye");
        }
        break;

      case SkillPool.UNLEASH_NANITES:
        if (responseText.contains("You roar with sudden power") || skillRunawaySuccess) {
          BanishManager.banishMonster(monsterName, "nanorhino");
        }
        break;

      case SkillPool.BATTER_UP:
        if (responseText.contains("knocked out of the park") || skillRunawaySuccess) {
          BanishManager.banishMonster(monsterName, "batter up!");
        }
        break;

      case SkillPool.TALK_ABOUT_POLITICS:
        if (responseText.contains("won't be seeing") || skillSuccess) {
          Preferences.increment("_pantsgivingBanish");
          BanishManager.banishMonster(monsterName, "pantsgiving");
        }
        break;

      case SkillPool.WALK_AWAY_FROM_EXPLOSION:
        if (responseText.contains("foe is obliterated in a spectacular explosion")
            || skillSuccess) {
          BanishManager.banishMonster(monsterName, "walk away from explosion");
        }
        break;

      case SkillPool.THUNDER_CLAP:
        if (responseText.contains("opponent heads for the hills") || skillRunawaySuccess) {
          BanishManager.banishMonster(monsterName, "thunder clap");
        }
        break;

      case SkillPool.LICORICE_ROPE:
        // You tie up the gingerbread man and hang him from a conveniently placed gargoyle.
        // You won't be seeing him again today! Or anybody with his same job.
        if (responseText.contains("tie up the gingerbread") || skillSuccess) {
          BanishManager.banishMonster(monsterName, "licorice rope");
        }
        break;

      case SkillPool.KGB_TRANQUILIZER_DART:
        Preferences.increment("_kgbTranquilizerDartUses");
        if (responseText.contains("press the secret switch") || skillRunawaySuccess) {
          BanishManager.banishMonster(monsterName, "KGB tranquilizer dart");
        }
        break;

      case SkillPool.POCKET_CRUMBS:
        if (responseText.contains("pocket next to the crumbs")) {
          Preferences.increment("_pantsgivingCrumbs");
        }
        break;

      case SkillPool.FIX_JUKEBOX:
        if (responseText.contains("jukebox") || skillSuccess) {
          Preferences.increment("_peteJukeboxFixed");
        }
        break;

      case SkillPool.PEEL_OUT:
        if (responseText.contains("peel out")
            || responseText.contains("peels out")
            || skillRunawaySuccess) {
          Preferences.increment("_petePeeledOut");
          if (Preferences.getString("peteMotorbikeMuffler").equals("Extra-Smelly Muffler")) {
            BanishManager.banishMonster(monsterName, "peel out");
          }
        }
        break;

      case SkillPool.JUMP_SHARK:
        if (responseText.contains("shark") || skillSuccess) {
          Preferences.increment("_peteJumpedShark");
        }
        break;

      case SkillPool.SNOKEBOMB:
        if (responseText.contains("throw the smokebomb at your feet") || skillRunawaySuccess) {
          BanishManager.banishMonster(monsterName, "snokebomb");
          Preferences.increment("_snokebombUsed");
        }
        break;

      case SkillPool.SHATTERING_PUNCH:
        if (responseText.contains("punch") || skillSuccess) {
          Preferences.increment("_shatteringPunchUsed");
        }
        break;

      case SkillPool.GINGERBREAD_MOB_HIT:
        if (responseText.contains("associates arrive") || skillSuccess) {
          Preferences.setBoolean("_gingerbreadMobHitUsed", true);
        }
        break;

      case SkillPool.BEANCANNON:
        if (responseText.contains("tide of beans") || skillSuccess) {
          Preferences.increment("_beanCannonUses");
          BanishManager.banishMonster(monsterName, "beancannon");
          EquipmentManager.discardEquipment(
              EquipmentManager.getEquipment(EquipmentManager.OFFHAND));
        }
        break;

      case SkillPool.BREATHE_OUT:
        if (responseText.contains("residual hot jelly heat") || skillSuccess) {
          BanishManager.banishMonster(monsterName, "breathe out");
          Preferences.decrement("_hotJellyUses");
        }
        break;

      case SkillPool.HUGS_KISSES:
        if (responseText.contains("yoinks something") || skillSuccess) {
          Preferences.increment("_xoHugsUsed");
        }
        break;

      case SkillPool.SHOW_RING:
        // You show him your ring. "Well, I never," he exclaims as he storms off in a huff.
        // You show them your ring. They aren't impressed.
        if (responseText.contains("Well, I never") || skillRunawaySuccess) {
          Preferences.setBoolean("_mafiaMiddleFingerRingUsed", true);
          BanishManager.banishMonster(monsterName, "mafia middle finger ring");
        }
        break;

      case SkillPool.THROW_LATTE:
        if (responseText.contains("They run off") || skillRunawaySuccess) {
          Preferences.setBoolean("_latteBanishUsed", true);
          BanishManager.banishMonster(monsterName, "Throw Latte on Opponent");
        }
        break;

      case SkillPool.OFFER_LATTE:
        if (responseText.contains("friends start following you") || skillSuccess) {
          TurnCounter.stopCounting("Latte Monster");
          TurnCounter.startCounting(30, "Latte Monster loc=*", "snout.gif");
          Preferences.setString("_latteMonster", monsterName);
          Preferences.setBoolean("_latteCopyUsed", true);
        }
        break;

      case SkillPool.GULP_LATTE:
        if (responseText.contains("take a big invigorating gulp") || skillSuccess) {
          Preferences.setBoolean("_latteDrinkUsed", true);
        }
        break;

        // Casting Carbohydrate Cudgel uses Dry Noodles
      case SkillPool.CARBOHYDRATE_CUDGEL:
        if (responseText.contains("You toss a bundle") || skillSuccess) {
          ResultProcessor.processItem(ItemPool.DRY_NOODLES, -1);
        }
        break;

        // Casting Unload Tommy Gun uses Tommy Ammo
      case SkillPool.UNLOAD_TOMMY_GUN:
        if (responseText.contains("firing the tommy gun") || skillSuccess) {
          ResultProcessor.processItem(ItemPool.TOMMY_AMMO, -1);
        }
        break;

        // Casting Shovel Hot Coal uses Hot Coal
      case SkillPool.SHOVEL_HOT_COAL:
        if (responseText.contains("hot coal into the shovel") || skillSuccess) {
          ResultProcessor.processItem(ItemPool.HOT_COAL, -1);
        }
        break;

        // Casting Crackpot Mystic item spells uses a Pixel Power Cell
      case SkillPool.RAGE_FLAME:
        if (responseText.contains("resulting torrent of flame") || skillSuccess) {
          ResultProcessor.processItem(ItemPool.PIXEL_POWER_CELL, -1);
        }
        break;

      case SkillPool.DOUBT_SHACKLES:
        if (responseText.contains("looking less confident") || skillSuccess) {
          ResultProcessor.processItem(ItemPool.PIXEL_POWER_CELL, -1);
        }
        break;

      case SkillPool.FEAR_VAPOR:
        if (responseText.contains("converts the energy into pure horror") || skillSuccess) {
          ResultProcessor.processItem(ItemPool.PIXEL_POWER_CELL, -1);
        }
        break;

      case SkillPool.TEAR_WAVE:
        if (responseText.contains("deluge of tears bursts forth") || skillSuccess) {
          ResultProcessor.processItem(ItemPool.PIXEL_POWER_CELL, -1);
        }
        break;

      case SkillPool.SUMMON_HOBO:
        // The first part is for a hobo underling being summoned
        // The second part is from using a dinged-up triangle to summon it
        if (responseText.contains("A hobo runs up to you")
            || skillSuccess
                && !responseText.contains("You give the triangle a vigorous ringing.")) {
          Preferences.increment("_hoboUnderlingSummons", 1);
        }
        break;

      case SkillPool.OVERLOAD_TEDDY_BEAR:
        EquipmentManager.discardEquipment(ItemPool.CUDDLY_TEDDY_BEAR);
        break;

      case SkillPool.THROW_SKULL:
        EquipmentManager.discardSpelunkyEquipment(ItemPool.SPELUNKY_SKULL);
        break;

      case SkillPool.THROW_ROCK:
        EquipmentManager.discardSpelunkyEquipment(ItemPool.SPELUNKY_ROCK);
        break;

      case SkillPool.THROW_POT:
        EquipmentManager.discardSpelunkyEquipment(ItemPool.SPELUNKY_POT);
        break;

      case SkillPool.THROW_TORCH:
        EquipmentManager.discardSpelunkyEquipment(ItemPool.SPELUNKY_TORCH);
        break;

      case SkillPool.LASH_OF_COBRA:
        Preferences.setBoolean("edUsedLash", true);
        if (responseText.contains("You acquire an item") || skillSuccess) {
          Preferences.increment("_edLashCount");
        }
        break;

        // Casting Curse of Fortune uses Ka Coin
      case SkillPool.CURSE_OF_FORTUNE:
        if (responseText.contains("Jackal demon shrugs and produces a large wad of meat")
            || skillSuccess) {
          ResultProcessor.processItem(ItemPool.KA_COIN, -1);
          Modifiers.overrideModifier("Generated:fightMods", "Meat Drop: +200");
          KoLCharacter.recalculateAdjustments();
          KoLCharacter.updateStatus();
        }
        break;

      case SkillPool.CURSE_OF_VACATION:
        if (responseText.contains("as the vortex disappears") || skillSuccess) {
          BanishManager.banishMonster(monsterName, "curse of vacation");
        }
        break;

      case SkillPool.CURSE_OF_STENCH:
        if (responseText.contains("cat peed in a box of rotten eggs") || skillSuccess) {
          Preferences.setString("stenchCursedMonster", monsterName);
        }
        break;

      case SkillPool.HEALING_SALVE:
        ResultProcessor.removeItem(ItemPool.WHITE_MANA);
        break;

      case SkillPool.LIGHTNING_BOLT_CARD:
        ResultProcessor.removeItem(ItemPool.RED_MANA);
        break;

      case SkillPool.GIANT_GROWTH:
        ResultProcessor.removeItem(ItemPool.GREEN_MANA);
        break;

      case SkillPool.CANHANDLE:
        if (responseText.contains("You shake the can") || skillSuccess) {
          FightRequest.handledCan = true;
        }
        break;

      case SkillPool.SHOOT:
        if (responseText.contains("You draw your sixgun") || skillSuccess) {
          FightRequest.shotSixgun = true;
        }
        break;

      case SkillPool.FAN_HAMMER:
        if (responseText.contains("You empty your sixgun") || skillSuccess) {
          FightRequest.shotSixgun = true;
        }
        break;

      case SkillPool.EXTRACT_OIL:
        if (responseText.contains("plunge your trusty oil extractor") || skillSuccess) {
          Preferences.increment("_oilExtracted");
        } else {
          Matcher matcher = FightRequest.CLOG_PATTERN.matcher(responseText);
          if (matcher.find()) {
            String clog = matcher.group(1);
            int extracts = StringUtilities.parseInt(clog) / 10 + 5;
            Preferences.setInteger("_oilExtracted", extracts);
          } else if (responseText.contains("completely clogged up")) {
            Preferences.setInteger("_oilExtracted", 15);
          }
        }
        break;

      case SkillPool.BAT_OOMERANG:
        ResultProcessor.removeItem(ItemPool.BAT_OOMERANG);
        break;

      case SkillPool.BAT_JUTE:
        ResultProcessor.removeItem(ItemPool.BAT_JUTE);
        break;

      case SkillPool.BAT_O_MITE:
        ResultProcessor.removeItem(ItemPool.BAT_O_MITE);
        break;

      case SkillPool.ULTRACOAGULATOR:
        ResultProcessor.removeItem(ItemPool.ULTRACOAGULATOR);
        break;

      case SkillPool.KICKBALL:
        ResultProcessor.removeItem(ItemPool.EXPLODING_KICKBALL);
        break;

      case SkillPool.BAT_GLUE:
        ResultProcessor.removeItem(ItemPool.GLOB_OF_BAT_GLUE);
        break;

      case SkillPool.BAT_BEARING:
        ResultProcessor.removeItem(ItemPool.BAT_BEARING);
        break;

      case SkillPool.USE_BAT_AID:
        ResultProcessor.removeItem(ItemPool.BAT_AID_BANDAGE);
        break;

      case SkillPool.FIRE_JOKESTER_GUN:
        if (responseText.contains("little flag reading BANG pops out the end of the barrel")
            || skillSuccess) {
          Preferences.setBoolean("_firedJokestersGun", true);
        }
        break;

      case SkillPool.DIGITIZE:
        if (responseText.contains("quickly copy the monster") || skillSuccess) {
          Preferences.increment("_sourceTerminalDigitizeUses");
          Preferences.setInteger("_sourceTerminalDigitizeMonsterCount", 0);
          TurnCounter.stopCounting("Digitize Monster");
          if (Preferences.getBoolean("stopForFixedWanderer")) {
            TurnCounter.startCountingTemporary(7, "Digitize Monster type=wander", "watch.gif");
          } else {
            TurnCounter.startCountingTemporary(
                7, "Digitize Monster loc=* type=wander", "watch.gif");
          }
          Preferences.setString("_sourceTerminalDigitizeMonster", monsterName);
        }
        break;

      case SkillPool.PORTSCAN:
        if (responseText.contains("scan nearby ports") || skillSuccess) {
          Preferences.increment("_sourceTerminalPortscanUses");
          if (Preferences.getBoolean("stopForFixedWanderer")) {
            TurnCounter.startCounting(0, "portscan.edu type=wander", "gyroscope.gif");
          } else {
            TurnCounter.startCounting(0, "portscan.edu loc=* type=wander", "gyroscope.gif");
          }
        }
        break;

      case SkillPool.DUPLICATE:
        if (responseText.contains("cross your eyes") || skillSuccess) {
          Preferences.increment("_sourceTerminalDuplicateUses");
        }
        break;

      case SkillPool.AM_MISSILE_LAUNCHER:
        if (responseText.contains("they're entirely gone") || skillSuccess) {
          Preferences.setBoolean("_missileLauncherUsed", true);
          CampgroundRequest.useFuel(SkillDatabase.getFuelCost(SkillPool.AM_MISSILE_LAUNCHER));
        }
        break;

      case SkillPool.AM_BEAN_BAG_CANNON:
        if (responseText.contains("beanbag cannon pops out") || skillSuccess) {
          CampgroundRequest.useFuel(SkillDatabase.getFuelCost(SkillPool.AM_BEAN_BAG_CANNON));
        }
        break;

      case SkillPool.AM_FRONT_BUMPER:
        if (responseText.contains("before flying out of sight") || skillRunawaySuccess) {
          BanishManager.banishMonster(monsterName, "Spring-Loaded Front Bumper");
          CampgroundRequest.useFuel(SkillDatabase.getFuelCost(SkillPool.AM_FRONT_BUMPER));
        }
        break;

      case SkillPool.MICROMETEOR:
        Preferences.increment("_micrometeoriteUses");
        break;

      case SkillPool.MACROMETEOR:
        if (responseText.contains("You quickly step") || skillSuccess) {
          Preferences.increment("_macrometeoriteUses");
        }
        break;

      case SkillPool.METEOR_SHOWER:
        if (responseText.contains("consult your mental index") || skillSuccess) {
          Preferences.increment("_meteorShowerUses");
        }
        break;

      case SkillPool.SWAP_MASK:
        if (responseText.contains("swap your mask") || skillSuccess) {
          // Actual change for character's equipped mask is handled by character pane update
          String message = null;
          Matcher matcher = FightRequest.MASK_SWAP_PATTERN.matcher(responseText);
          if (matcher.find()) {
            message =
                "You swap your "
                    + KoLCharacter.getMask()
                    + " for the monster's "
                    + matcher.group(1);
          } else {
            message =
                "You swap your "
                    + KoLCharacter.getMask()
                    + " for the monster's "
                    + MonsterData.lastMask;
          }
          MonsterData.lastMask = KoLCharacter.getMask();
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
        break;

      case SkillPool.OTOSCOPE:
        if (responseText.contains("jam it into your enemy's ear") || skillSuccess) {
          Preferences.increment("_otoscopeUsed");
          Modifiers.overrideModifier("Generated:fightMods", "Item Drop: +200");
          KoLCharacter.recalculateAdjustments();
          KoLCharacter.updateStatus();
        }
        break;

      case SkillPool.REFLEX_HAMMER:
        if (responseText.contains("short distance into the future") || skillRunawaySuccess) {
          Preferences.increment("_reflexHammerUsed");
          BanishManager.banishMonster(monsterName, "Reflex Hammer");
        }
        break;

      case SkillPool.CHEST_X_RAY:
        if (
        // Instant free kill
        responseText.contains("nowhere to be seen")
            ||
            // Damage to bosses or other special monsters
            responseText.contains("damage from the unshielded radiation")
            || skillSuccess) {
          Preferences.increment("_chestXRayUsed");
        }
        break;

      case SkillPool.BALEFUL_HOWL:
        if (responseText.contains("spooked by its balefulness") || skillSuccess) {
          BanishManager.banishMonster(monsterName, "baleful howl");
          Preferences.increment("_balefulHowlUses");
        }
        break;

      case SkillPool.ARMY_TODDLER:
        if (responseText.contains("You cry havoc") || skillSuccess) {
          Preferences.setBoolean("_armyToddlerCast", true);
        }
        break;

      case SkillPool.ENSORCEL:
        if (responseText.contains("You gaze into your foe's eyes...") || skillSuccess) {
          Preferences.setString("ensorcelee", monster.toString());
          Preferences.setInteger("ensorceleeLevel", FightRequest.startingAttack);
        }
        break;

      case SkillPool.BECOME_WOLF:
        if (responseText.contains("You pull your cloake up over your head") || skillSuccess) {
          Preferences.increment("_vampyreCloakeFormUses");
        }
        break;

      case SkillPool.BECOME_MIST:
        if (responseText.contains("You wrap your cloake around your face") || skillSuccess) {
          Preferences.increment("_vampyreCloakeFormUses");
        }
        break;

      case SkillPool.BECOME_BAT:
        if (responseText.contains("You wrap your cloake around your arms") || skillSuccess) {
          Preferences.increment("_vampyreCloakeFormUses");
        }
        break;

      case SkillPool.IMPLODE_UNIVERSE:
        // You close your eyes, concentrate really hard, and implode the portion of the universe
        // that contains your foe.
        if (responseText.contains("implode the portion of the universe that contains your foe")
            || skillSuccess) {
          Preferences.increment("_universeImploded");
        }
        break;

      case SkillPool.DELIVER_THESIS:
        // DECREASING WAVELENGTH. OBJECTS IN MIRROR MAY BE BLUER THAN THEY APPEAR.
        // TIME AND SPACE ARE FUNGIBLE. LET ME DEMONSTRATE WITH THIS OPPONENT.
        // BUFFER OVERFLOW. REBOOTING PREVIOUS MINUTE.
        if (responseText.contains("DECREASING WAVELENGTH")
            || responseText.contains("TIME AND SPACE ARE FUNGIBLE")
            || responseText.contains("BUFFER OVERFLOW")
            || skillSuccess) {
          Preferences.setBoolean("_thesisDelivered", true);
          KoLCharacter.getFamiliar().addNonCombatExperience(-200);
        }
        break;

      case SkillPool.LECTURE_ON_VELOCITY:
        // These all share a counter

        // ADJUSTING DOPPLER WAVELENGTH.
        if (responseText.contains("ADJUSTING DOPPLER WAVELENGTH") || skillSuccess) {
          Preferences.increment("_pocketProfessorLectures");
        }
        break;

      case SkillPool.LECTURE_ON_MASS:
        // These all share a counter

        // COLLAPSING ITEM PROBABILITY WAVEFORMS NOW. PLEASE STAND BACK.
        // CALCULATE MASS FOR DROPPED ITEMS CAREFULLY. THREE TIMES AT LEAST.
        // DO NOT STAND UNDER DROPPING ITEMS. IT IS THE LAW (OF GRAVITY).
        if (responseText.contains("COLLAPSING ITEM PROBABILITY WAVEFORMS")
            || responseText.contains("CALCULATE MASS FOR DROPPED ITEMS")
            || responseText.contains("DO NOT STAND")
            || skillSuccess) {
          Preferences.increment("_pocketProfessorLectures");
        }
        break;

      case SkillPool.LECTURE_ON_RELATIVITY:
        // These all share a counter

        // TESSERACT? I HARDLY KNEW HER."
        // NOW CONVERTING MATTER INTO TIME. PLEASE HAVE ALREADY WAITED.
        // I WILL NOW DEMONSTRATE BY FOLDING TIME AND SPACE.
        if (responseText.contains("TESSERACT")
            || responseText.contains("NOW CONVERTING MATTER INTO TIME")
            || responseText.contains("FOLDING TIME AND SPACE")
            || skillSuccess) {
          Preferences.increment("_pocketProfessorLectures");
        }
        break;

      case SkillPool.REPLACE_ENEMY:
        Preferences.increment("_powerfulGloveBatteryPowerUsed", 10, 100, false);
        break;

      case SkillPool.SHRINK_ENEMY:
        Preferences.increment("_powerfulGloveBatteryPowerUsed", 5, 100, false);
        break;

      case SkillPool.HAMMER_THROW_COMBAT:
      case SkillPool.JUGGLE_FIREBALLS_COMBAT:
      case SkillPool.SPIN_JUMP_COMBAT:
        KoLCharacter.spendPP(1);
        break;
      case SkillPool.ULTRA_SMASH_COMBAT:
        if (responseText.contains("knock your opponent into tomorrow") || skillRunawaySuccess) {
          BanishManager.banishMonster(monsterName, "Ultra Hammer");
        }
        // Fall through
      case SkillPool.FIREBALL_BARRAGE_COMBAT:
      case SkillPool.MULTI_BOUNCE_COMBAT:
        KoLCharacter.spendPP(2);
        break;

      case SkillPool.FEEL_NOSTALGIC:
        if (responseText.contains("really feeling nostalgic") || skillSuccess) {
          Preferences.increment("_feelNostalgicUsed", 1, 3, false);
        }
        break;

      case SkillPool.FEEL_HATRED:
        if (responseText.contains("walk away and decide not to see this creature again")
            || skillRunawaySuccess) {
          BanishManager.banishMonster(monsterName, "Feel Hatred");
          Preferences.increment("_feelHatredUsed", 1, 3, false);
        }
        break;

      case SkillPool.FEEL_PRIDE:
        if (responseText.contains("you are going to do a great job") || skillSuccess) {
          Preferences.increment("_feelPrideUsed", 1, 3, false);
        }
        break;

      case SkillPool.FEEL_ENVY:
        if (responseText.contains("really want what they have") || skillSuccess) {
          Preferences.increment("_feelEnvyUsed", 1, 3, false);
        }
        break;

      case SkillPool.FEEL_SUPERIOR:
        if (responseText.contains("express your superiority to the tune") || skillSuccess) {
          Preferences.increment("_feelSuperiorUsed", 1, 3, false);
        }
        break;

      case SkillPool.SHOCKING_LICK:
        if (responseText.contains("give your foe a big slobbery lick")
            || responseText.contains("worth of electricity from your tongue")
            || skillSuccess) {
          Preferences.decrement("shockingLickCharges");
        }
        break;

      case SkillPool.BACK_UP:
        if (responseText.contains("You check in your back-up camera, ") || skillSuccess) {
          Preferences.increment("_backUpUses");
        }
        break;

      case SkillPool.SHOW_SCRAPBOOK:
        if (responseText.contains(
                "You take out your scrapbook and start showing photos of your familiars to your opponent")
            || responseText.contains("waving your scrapbook")
            || responseText.contains("the two of you share a friendly handshake and part ways")
            || responseText.contains("they pass out from pure boredom")
            || skillSuccess) {
          BanishManager.banishMonster(monsterName, "Show your boring familiar pictures");
          Preferences.decrement("scrapbookCharges", 100, 0);
        }
        break;

      case SkillPool.BLART_SPRAY_WIDE:
        if (responseText.contains("nozzle all the way and blast it out of sight") || skillSuccess) {
          BanishManager.banishMonster(monsterName, "B. L. A. R. T. Spray (wide)");
        }
        break;

      case SkillPool.FIRE_EXTINGUISHER__FOAM_EM_UP:
        if (responseText.contains("both comical and immobile") || skillSuccess) {
          Preferences.decrement("_fireExtinguisherCharge", 5);
        }
        break;

      case SkillPool.FIRE_EXTINGUISHER__POLAR_VORTEX:
        if (responseText.contains("You fire a blast of frigid extinguishant at your foe")
            || skillSuccess) {
          Preferences.decrement("_fireExtinguisherCharge", 10);
        }
        break;

      case SkillPool.FIRE_EXTINGUISHER__FOAM_YOURSELF:
        if (responseText.contains("create a suit made of flame-retardant foam") || skillSuccess) {
          Preferences.decrement("_fireExtinguisherCharge", 10);
        }
        break;

      case SkillPool.FIRE_EXTINGUISHER__BLAST_THE_AREA:
        if (responseText.contains("dust and debris is kicked up into a cyclone") || skillSuccess) {
          Preferences.decrement("_fireExtinguisherCharge", 5);
        }
        break;

      case SkillPool.FIRE_EXTINGUISHER__ZONE_SPECIFIC:
        boolean success = false;
        KoLAdventure location = KoLAdventure.lastVisitedLocation();
        switch (location.getZone()) {
          case "BatHole":
            if (responseText.contains(
                    "You squeeze down the nozzle on your fire extinguisher and release a blast")
                || skillSuccess) {
              if (!QuestDatabase.isQuestLaterThan(Quest.BAT, "step2")) {
                QuestDatabase.advanceQuest(Quest.BAT);
              }
              Preferences.setBoolean("fireExtinguisherBatHoleUsed", true);
              success = true;
            }
            break;
          case "Cyrpt":
            if (responseText.contains(
                    "The chill of the refrigerant quickly replaces some of the chill of evil in the air")
                || skillSuccess) {
              String setting = getEvilZoneSetting();
              if (setting != null) {
                Preferences.decrement(setting, 10, 0);
                Preferences.decrement("cyrptTotalEvilness", 10, 0);
              }
              Preferences.setBoolean("fireExtinguisherCyrptUsed", true);
              success = true;
            }
            break;
        }

        switch (location.getAdventureName()) {
          case "Cobb's Knob Harem":
            if (responseText.contains("You fill the harem with foam") || skillSuccess) {
              Preferences.setBoolean("fireExtinguisherHaremUsed", true);
              success = true;
            }
            break;
          case "The Smut Orc Logging Camp":
            if (responseText.contains("You wantonly spray the area with your fire extinguisher")
                || skillSuccess) {
              Preferences.increment("smutOrcNoncombatProgress", 10, 15, false);
              Preferences.setBoolean("fireExtinguisherChasmUsed", true);
              success = true;
            }
            break;
          case "The Arid, Extra-Dry Desert":
            if (responseText.contains("You aim the nozzle directly into your mouth")
                || skillSuccess) {
              Preferences.setBoolean("fireExtinguisherDesertUsed", true);
              success = true;
            }
            break;
        }

        if (success) {
          Preferences.decrement("_fireExtinguisherCharge", 20);
        }
      case SkillPool.BE_GREGARIOUS:
        if (responseText.contains("You decide to put your best foot forward") || skillSuccess) {
          Preferences.setString("beGregariousMonster", monsterName);
          Preferences.decrement("beGregariousCharges", 1, 0);
          Preferences.setInteger("beGregariousFightsLeft", 3);
        }
        break;
    }
  }

  public static final void payItemCost(
      final int itemId, final int itemId2, final String responseText) {
    if (itemId <= 0) {
      return;
    }

    boolean itemSuccess =
        (FightRequest.anapest
                && (responseText.contains("used a thing from your bag")
                    || responseText.contains("item caused something to happen")))
            || (FightRequest.haiku
                && (responseText.contains("do some stuff with a thing")
                    || responseText.contains("some inscrutable end")))
            || (FightRequest.machineElf && responseText.contains("performs its function"));
    boolean itemRunawaySuccess =
        (FightRequest.anapest && responseText.contains("wings on your heels"))
            || (FightRequest.haiku
                && (responseText.contains("burps taste like pride")
                    || responseText.contains("beat a retreat")))
            || (FightRequest.machineElf && responseText.contains("are no longer anywhere"));

    switch (itemId) {
      default:
        break;

      case ItemPool.CHAOS_BUTTERFLY:
        if (responseText.contains("reality is altered in unpredictable ways") || itemSuccess) {
          Preferences.setBoolean("chaosButterflyThrown", true);
        }
        break;

      case ItemPool.TOY_SOLDIER:
        // A toy soldier consumes tequila.

        if (KoLConstants.inventory.contains(FightRequest.TEQUILA)) {
          ResultProcessor.processResult(FightRequest.TEQUILA);
        }
        break;

      case ItemPool.TOY_MERCENARY:
        // A toy mercenary consumes 5-10 meat

        // A sidepane refresh at the end of the battle will
        // re-synch everything.
        break;

      case ItemPool.SHRINKING_POWDER:
        if (responseText.contains("gets smaller and angrier") || itemSuccess) {
          MonsterStatusTracker.damageMonster(MonsterStatusTracker.getMonsterHealth() / 2);
        }
        break;

      case 819:
      case 820:
      case 821:
      case 822:
      case 823:
      case 824:
      case 825:
      case 826:
      case 827:
        if (AdventureResult.bangPotionName(itemId).contains("healing")) {
          MonsterStatusTracker.healMonster(16);
        }
        break;

        // Handle item banishers
      case ItemPool.CRYSTAL_SKULL:
        if (responseText.contains("skull explodes into a million worthless shards of glass")
            || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("crystal skull");
        }
        break;
      case ItemPool.DIVINE_CHAMPAGNE_POPPER:
        if (responseText.contains("surprisingly loud bang, and your opponent")
            || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("divine champagne popper");
        }
        break;
      case ItemPool.HAROLDS_HAMMER:
        if (responseText.contains("throw the bell away") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("harold's bell");
        }
        break;
      case ItemPool.INDIGO_TAFFY:
        if (responseText.contains("nowhere to be found") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("pulled indigo taffy");
        }
        break;
      case ItemPool.CLASSY_MONKEY:
        if (responseText.contains("EEEEEEEEEEEEEEEEEEEEEEEEK!") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("classy monkey");
        }
        break;
      case ItemPool.DIRTY_STINKBOMB:
        if (responseText.contains("don't expect to see") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("dirty stinkbomb");
        }
        break;
      case ItemPool.DEATHCHUCKS:
        if (responseText.contains("far enough away from you") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("deathchucks");
        }
        break;
      case ItemPool.COCKTAIL_NAPKIN:
        if (responseText.contains(
                "random phone number onto the napkin and hand it to the clingy pirate")
            || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("cocktail napkin");
        }
        break;
      case ItemPool.LOUDER_THAN_BOMB:
        if (responseText.contains("nowhere to be seen") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("louder than bomb");
        }
        break;
      case ItemPool.SMOKE_GRENADE:
        if (responseText.contains("flee in the ensuing confusion") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("smoke grenade");
        }
        break;
      case ItemPool.SPOOKY_MUSIC_BOX_MECHANISM:
        if (responseText.contains("wistful expression") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("spooky music box mechanism");
        }
        break;
      case ItemPool.ICE_HOUSE:
        // You toss the ice house on the ground, and your opponent enters it.
        // You slam the door and laugh all the way to the Museum, where you put
        // the house on display with it still inside it.
        if (responseText.contains("toss the ice house") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("ice house");
        }
        break;
      case ItemPool.TENNIS_BALL:
        if (responseText.contains("You won't be seeing") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("tennis ball");
        }
        break;
      case ItemPool.ICE_HOTEL_BELL:
        if (responseText.contains("a nearby door") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("ice hotel bell");
        }
        break;
      case ItemPool.GINGERBREAD_RESTRAINING_ORDER:
        // You read the restraining order to the gingerbread man, and for some reason, he agrees to
        // abide by it.
        if (responseText.contains("read the restraining order") || itemSuccess) {
          BanishManager.banishCurrentMonster("gingerbread restraining order");
        }
        break;
      case ItemPool.BUNDLE_OF_FRAGRANT_HERBS:
        if (responseText.contains("chokes and sputters and leave") || itemRunawaySuccess) {
          Preferences.increment("_fragrantHerbsUsed", 1, 10, false);
          BanishManager.banishCurrentMonster("bundle of &quot;fragrant&quot; herbs");
        }
        break;
      case ItemPool.NUCLEAR_STOCKPILE:
        if (responseText.contains("pull a nuclear bomb out of the stockpile")
            || itemRunawaySuccess) {
          Preferences.increment("_nuclearStockpileUsed", 1, 10, false);
        }
        break;
      case ItemPool.AFFIRMATION_SUPERFICIALLY_INTERESTED:
        if (responseText.contains("are feeling really, really interested") || itemSuccess) {
          MonsterData monster = MonsterStatusTracker.getLastMonster();
          Preferences.setString("superficiallyInterestedMonster", monster.getName());
          TurnCounter.stopCounting("Superficially Interested Monster");
          TurnCounter.startCounting(80, "Superficially Interested Monster loc=*", "snout.gif");
        }
        break;
      case ItemPool.AFFIRMATION_MIND_MASTER:
        if (responseText.contains("push away your opponent") || itemRunawaySuccess) {
          BanishManager.banishCurrentMonster("Be a Mind Master");
        }
        break;
      case ItemPool.TRYPTOPHAN_DART:
        if (responseText.contains("asleep on a nearby recliner") || itemSuccess) {
          BanishManager.banishCurrentMonster("tryptophan dart");
          Preferences.setBoolean("_tryptophanDartUsed", true);
        }
        break;
      case ItemPool.HUMAN_MUSK:
        if (responseText.contains("open the vial") || itemSuccess) {
          BanishManager.banishCurrentMonster("human musk");
          Preferences.increment("_humanMuskUses");
        }
        break;
      case ItemPool.ROCK_BAND_FLYERS:
      case ItemPool.JAM_BAND_FLYERS:
        // You slap a flyer up on your opponent. It enrages it.
        if (responseText.contains("You slap a flyer") || itemSuccess) {
          int ML = Math.max(0, MonsterStatusTracker.getMonsterOriginalAttack());
          Preferences.increment("flyeredML", ML);
          AdventureResult result = AdventureResult.tallyItem("Arena flyer ML", ML, false);
          AdventureResult.addResultToList(KoLConstants.tally, result);
          GoalManager.updateProgress(result);
          usedFlyer = true;
        }

        // The Rock Promoters are long gone, and the scheduled day of the show
        // has passed. You toss the stack of flyers in a trash can.
        else if (responseText.contains("Rock Promoters are long gone")) {
          ResultProcessor.removeItem(itemId);
        }
        break;
      case ItemPool.EMPTY_EYE:
        // You hold Zombo's eye out toward your opponent,
        // whose gaze is transfixed by it. (success)
        //   or
        // You hold Zombo's eye out toward your opponent,
        // but nothing happens. (failure)
        if (responseText.contains(
                "You hold Zombo's eye out toward your opponent, whose gaze is transfixed by it.")
            || itemSuccess) {
          Preferences.setInteger("_lastZomboEye", KoLAdventure.getAdventureCount());
          // "Safe" interval between uses is 50 turns
          TurnCounter.stopCounting("Zombo's Empty Eye");
          TurnCounter.startCounting(50, "Zombo's Empty Eye loc=*", "zomboeye.gif");
        }
        break;
      case ItemPool.DNA_SYRINGE:
        // "Your opponent is shocked into inaction as you plunge the syringe into it and extract a
        // sample of its DNA."
        if (responseText.contains("plunge the syringe") || itemSuccess) {
          MonsterData monster = MonsterStatusTracker.getLastMonster();
          Phylum dna = monster != null ? monster.getPhylum() : Phylum.NONE;
          Preferences.setString("dnaSyringe", dna.toString());
        }
        break;

      case ItemPool.MAYO_LANCE:
        if (responseText.contains("Everything Looks Yellow")) {
          int mayo = Math.max(Preferences.getInteger("mayoLevel") - 30, 0);
          Preferences.setInteger("mayoLevel", mayo);
        }
        break;

      case ItemPool.BEEHIVE:
        if (responseText.contains("entire wall fattens")) {
          ResultProcessor.removeItem(ItemPool.BEEHIVE);
        }
        break;

      case ItemPool.ELECTRIC_BONING_KNIFE:
        if (responseText.contains("knife's motor burns out")) {
          ResultProcessor.removeItem(ItemPool.ELECTRIC_BONING_KNIFE);
        }
        break;

      case ItemPool.SPIDER_WEB:
        if (responseText.contains("Three other minions")) {
          Preferences.increment("_villainLairProgress", 3);
          Preferences.setBoolean("_villainLairWebUsed", true);
        }
        break;

      case ItemPool.KNOB_FIRECRACKER:
        if (responseText.contains("three other minions")) {
          Preferences.increment("_villainLairProgress", 3);
          Preferences.setBoolean("_villainLairFirecrackerUsed", true);
        }
        break;

      case ItemPool.CAN_LID:
        if (responseText.contains("three other minions")) {
          Preferences.increment("_villainLairProgress", 3);
          Preferences.setBoolean("_villainLairCanLidUsed", true);
        }
        break;

      case ItemPool.BOMB_OF_UNKNOWN_ORIGIN:
        if (responseText.contains("decide to find something else to protest")) {
          Preferences.increment("zeppelinProtestors", 10);
        }
        break;

      case ItemPool.DAILY_DUNGEON_MALWARE:
        if (responseText.contains("It's a UNIX System")
            || responseText.contains("You attempt to hack the monster")) {
          Preferences.setBoolean("_dailyDungeonMalwareUsed", true);
        }
        break;

      case ItemPool.CA_BASE_PAIR:
      case ItemPool.CG_BASE_PAIR:
      case ItemPool.CT_BASE_PAIR:
      case ItemPool.AG_BASE_PAIR:
      case ItemPool.AT_BASE_PAIR:
      case ItemPool.GT_BASE_PAIR:
        if (!usedBasePair) {
          usedBasePair = true;
          QuestManager.updateCyrusAdjective(itemId);
        }
        break;
    }

    if (itemId != itemId2) {
      // If these items succeed, then the second one won't actually do anything
      // because the first one ended the fight.
      switch (itemId) {
        case ItemPool.POWER_PILL:
          if (responseText.contains("devours your foe") || itemSuccess) {
            Preferences.increment("_powerPillUses");
          } else if (responseText.contains("refuses to eat")) {
            Preferences.setInteger("_powerPillUses", 20);
          }
          break;

        case ItemPool.GLARK_CABLE:
          if (responseText.contains("neatly vaporized") || itemSuccess) {
            Preferences.increment("_glarkCableUses", 1, 5, false);
          } else if (responseText.contains("glark batteries")) {
            Preferences.setInteger("_glarkCableUses", 5);
          }
          break;

        case ItemPool.REPLICA_BAT_OOMERANG:
          if (responseText.contains("arm is too tired")) {
            Preferences.setInteger("_usedReplicaBatoomerang", 3);
          } else if (responseText.contains("throw the replica bat-oomerang") || itemSuccess) {
            Preferences.increment("_usedReplicaBatoomerang", 1, 3, false);
          }
          break;

        case ItemPool.POWDERED_MADNESS:
          if (responseText.contains("blow the madness")) {
            Preferences.increment("_powderedMadnessUses");
          }
          break;

        case ItemPool.AFFIRMATION_HATE:
          if (responseText.contains("You gain 3 PvP Fights")
              || responseText.contains("belligerent")
              || itemSuccess) {
            Preferences.setBoolean("_affirmationHateUsed", true);
          }
          break;

        case ItemPool.CA_BASE_PAIR:
        case ItemPool.CG_BASE_PAIR:
        case ItemPool.CT_BASE_PAIR:
        case ItemPool.AG_BASE_PAIR:
        case ItemPool.AT_BASE_PAIR:
        case ItemPool.GT_BASE_PAIR:
          if (!usedBasePair) {
            usedBasePair = true;
            QuestManager.updateCyrusAdjective(itemId2);
          }
          break;
      }
    }

    // May update a quest, handle in QuestManager
    QuestManager.updateQuestItemUsed(itemId, responseText);

    if (FightRequest.isItemConsumed(itemId, responseText)) {
      ResultProcessor.processResult(ItemPool.get(itemId, -1));
      return;
    }
  }

  @Override
  public int getAdventuresUsed() {
    return 0;
  }

  public static final int getCurrentRound() {
    return FightRequest.currentRound;
  }

  public static final int getRoundIndex() {
    return FightRequest.currentRound - 1 - FightRequest.preparatoryRounds;
  }

  public static final boolean edFightInProgress() {
    return KoLCharacter.isEd() && Preferences.getInteger("_edDefeats") != 0;
  }

  public static final boolean alreadyJiggled() {
    return FightRequest.jiggledChefstaff;
  }

  public static final boolean handledCan() {
    return FightRequest.handledCan;
  }

  public static final boolean canHandleCan() {
    return EquipmentManager.usingCanOfBeans() && KoLCharacter.hasSkill("Canhandle");
  }

  public static final boolean shotSixgun() {
    return FightRequest.shotSixgun;
  }

  public static final boolean haveFought() {
    boolean rv = FightRequest.haveFought;
    FightRequest.haveFought = false;
    return rv;
  }

  public static final int freeRunawayChance() {
    if (KoLCharacter.inBigcore()) {
      // Free runaways don't work in BIG!
      return 0;
    }

    // Bandersnatch + Ode = weight/5 free runaways
    if (KoLCharacter.getEffectiveFamiliar().getId() == FamiliarPool.BANDER
        && KoLConstants.activeEffects.contains(ConsumablesDatabase.ODE)) {
      if (!FightRequest.castCleesh
          && KoLCharacter.getFamiliar().getModifiedWeight() / 5
              > Preferences.getInteger("_banderRunaways")) {
        return 100;
      }
    }
    // Pair of Stomping Boots = weight/5 free runaways, on the same counter as the Bandersnatch
    else if (KoLCharacter.getEffectiveFamiliar().getId() == FamiliarPool.BOOTS) {
      if (KoLCharacter.getFamiliar().getModifiedWeight() / 5
          > Preferences.getInteger("_banderRunaways")) {
        return 100;
      }
    } else if (KoLCharacter.hasEquipped(ItemPool.get(ItemPool.NAVEL_RING, 1))
        || KoLCharacter.hasEquipped(ItemPool.get(ItemPool.GREAT_PANTS, 1))) {
      int navelRunaways = Preferences.getInteger("_navelRunaways");

      return navelRunaways < 3 ? 100 : navelRunaways < 6 ? 80 : navelRunaways < 9 ? 50 : 20;
    }

    return 0;
  }

  private static void registerMacroAction(
      Matcher m) { // In the interests of keeping action logging centralized, turn the
    // macro action (indicated via a macroaction: HTML comment) into a
    // fake fight.php URL and call registerRequest on it.

    String action = m.group(1);
    if (action.equals("attack")) {
      FightRequest.registerRequest(false, "fight.php?attack");
    } else if (action.equals("runaway")) {
      FightRequest.registerRequest(false, "fight.php?runaway");
    } else if (action.equals("steal")) {
      FightRequest.registerRequest(false, "fight.php?steal");
    } else if (action.equals("chefstaff")) {
      FightRequest.registerRequest(false, "fight.php?chefstaff");
    } else if (action.equals("skill")) {
      FightRequest.registerRequest(false, "fight.php?whichskill=" + m.group(2));
    } else if (action.equals("use")) {
      String item1 = m.group(2);
      String item2 = m.group(3);
      if (item2 == null) {
        FightRequest.registerRequest(false, "fight.php?whichitem=" + item1);
      } else {
        FightRequest.registerRequest(
            false, "fight.php?whichitem=" + item1 + "&whichitem2=" + item2);
      }
    } else {
      System.out.println("unrecognized macroaction: " + action);
    }
  }

  // ****** Move this somewhere appropriate

  private static final Map<String, String> pokefamMoveToAction1 = new HashMap<String, String>();
  private static final Map<String, String> pokefamActionToMove1 = new HashMap<String, String>();
  private static final Map<String, String> pokefamMoveToAction2 = new HashMap<String, String>();
  private static final Map<String, String> pokefamActionToMove2 = new HashMap<String, String>();
  private static final Map<String, String> pokefamMoveToAction3 = new HashMap<String, String>();
  private static final Map<String, String> pokefamActionToMove3 = new HashMap<String, String>();

  private static final Map<String, String> pokefamMoveDescriptions = new HashMap<String, String>();

  private static void mapMoveToAction(int num, String move, String action, String description) {
    Map<String, String> moveToAction;
    Map<String, String> actionToMove;
    switch (num) {
      case 1:
        moveToAction = FightRequest.pokefamMoveToAction1;
        actionToMove = FightRequest.pokefamActionToMove1;
        break;
      case 2:
        moveToAction = FightRequest.pokefamMoveToAction2;
        actionToMove = FightRequest.pokefamActionToMove2;
        break;
      case 3:
        moveToAction = FightRequest.pokefamMoveToAction3;
        actionToMove = FightRequest.pokefamActionToMove3;
        break;
      default:
        return;
    }

    moveToAction.put(move, action);
    actionToMove.put(action, move);
    FightRequest.pokefamMoveDescriptions.put(move, description);
  }

  private static String moveToAction(int num, String move) {
    Map<String, String> moveToAction;
    switch (num) {
      case 1:
        moveToAction = FightRequest.pokefamMoveToAction1;
        break;
      case 2:
        moveToAction = FightRequest.pokefamMoveToAction2;
        break;
      case 3:
        moveToAction = FightRequest.pokefamMoveToAction3;
        break;
      default:
        return "";
    }
    return moveToAction.get(move);
  }

  private static String actionToMove(int num, String action) {
    Map<String, String> actionToMove;
    switch (num) {
      case 1:
        actionToMove = FightRequest.pokefamActionToMove1;
        break;
      case 2:
        actionToMove = FightRequest.pokefamActionToMove2;
        break;
      case 3:
        actionToMove = FightRequest.pokefamActionToMove3;
        break;
      default:
        return "";
    }
    return actionToMove.get(action);
  }

  public static void registerPokefamMove(int num, String move, String action, String description) {
    if (action == null) {
      return;
    }

    String currentAction = FightRequest.moveToAction(num, move);
    if (action.equals(currentAction)) {
      return;
    }

    String printMe = "Pokefam move" + num + " '" + move + "' -> '" + action + "': " + description;
    RequestLogger.printLine(printMe);
    RequestLogger.updateSessionLog(printMe);

    FightRequest.mapMoveToAction(num, move, action, description);
  }

  static {
    FightRequest.mapMoveToAction(1, "Bite", "bite", "Deal [power] damage to a random enemy.");
    FightRequest.mapMoveToAction(1, "Bonk", "bonk", "Deal [power] damage to the frontmost enemy.");
    FightRequest.mapMoveToAction(
        1,
        "Claw",
        "claw",
        "Deal [power] damage to the frontmost enemy and 1 damage to a random enemy.");
    FightRequest.mapMoveToAction(1, "Peck", "peck", "Deal [power] damage to the frontmost enemy.");
    FightRequest.mapMoveToAction(
        1,
        "Punch",
        "punch",
        "Deal [power] damage to the frontmost enemy and reduce its power by 1.");
    FightRequest.mapMoveToAction(
        1, "Sting", "sting", "Deal [power] damage to the frontmost enemy and poison it.");

    FightRequest.mapMoveToAction(2, "Armor Up", "armorup", "Become Armored.");
    FightRequest.mapMoveToAction(
        2, "Backstab", "backstab", "Deal 1 damage to the rearmost enemy and poison it.");
    FightRequest.mapMoveToAction(2, "Breathe Fire", "flame", "Deal 1 damage to all enemies.");
    FightRequest.mapMoveToAction(2, "Chill Out", "chill", "Make a random enemy Tired.");
    FightRequest.mapMoveToAction(
        2, "Embarrass", "embarrass", "Reduce a random enemy's power by 1.");
    FightRequest.mapMoveToAction(
        2, "Encourage", "encourage", "Increase the frontmost ally's power by 1.");
    FightRequest.mapMoveToAction(
        2, "Frighten", "spook", "Reduce the frontmost enemy's power by 1.");
    FightRequest.mapMoveToAction(2, "Growl", "growl", "Reduce 2 random enemies' power by 1.");
    FightRequest.mapMoveToAction(2, "Howl", "howl", "Deal 1 damage to all enemies.");
    FightRequest.mapMoveToAction(2, "Laser Beam", "laser", "Deal 2 damage to a random enemy.");
    FightRequest.mapMoveToAction(2, "Lick", "lick", "Heal all allies for 1.");
    FightRequest.mapMoveToAction(2, "Regrow", "regrow", "Heal itself by [power]");
    FightRequest.mapMoveToAction(2, "Retreat", "retreat", "Move to the back.");
    FightRequest.mapMoveToAction(2, "Splash", "splash", "Deal 1 damage to two random enemies.");
    FightRequest.mapMoveToAction(2, "Stinkblast", "stinker", "Make a random enemy Tired.");
    FightRequest.mapMoveToAction(2, "Swoop", "swoop", "Avoid all attack damage this turn.");
    FightRequest.mapMoveToAction(2, "Tackle", "tackle", "Knock the frontmost enemy to the back.");

    FightRequest.mapMoveToAction(3, "Bear Hug", "ult_bearhug", "Heal self for 3, teammates for 2.");
    FightRequest.mapMoveToAction(
        3, "Blood Bath", "ult_bloodbath", "Deal 12 damage spread out among all foes randomly.");
    FightRequest.mapMoveToAction(3, "Defense Matrix", "ult_protect", "Give all allies Armored.");
    FightRequest.mapMoveToAction(
        3, "Deluxe Impale", "ult_impale", "Deal 5 damage to the frontmost enemy.");
    FightRequest.mapMoveToAction(
        3, "Empowering Cheer", "ult_powerall", "Give all allies +1 Power.");
    FightRequest.mapMoveToAction(
        3, "Healing Rain", "ult_regenall", "Give all allies Regeneration.");
    FightRequest.mapMoveToAction(3, "Nasty Cloud", "ult_sporecloud", "Poisons all enemies.");
    FightRequest.mapMoveToAction(
        3, "Nuclear Bomb", "ult_nuke", "Deal 5 damage to the rearmost enemy.");
    FightRequest.mapMoveToAction(
        3, "Owl Stare", "ult_owlstare", "Heal all allies for 1 and increase power by 1.");
    FightRequest.mapMoveToAction(3, "Pepperscorn", "ult_pepperscorn", "Give allies Spiked.");
    FightRequest.mapMoveToAction(
        3, "Rainbow Storm", "ult_rainbowstorm", "Give allies regeneration and armor.");
    FightRequest.mapMoveToAction(
        3, "Spiky Burst", "ult_crazyblast", "Deal 8 damage spread out among all foes randomly.");
    FightRequest.mapMoveToAction(
        3, "Stick Treats", "ult_stickytreats", "Heal allies for 1, tire front enemy.");
    FightRequest.mapMoveToAction(
        3, "Universal Backrub", "ult_superheal", "Heals all allies for 2.");
    FightRequest.mapMoveToAction(
        3, "Violent Shred", "ult_savage", "Deals 2 damage to all enemies.");
    FightRequest.mapMoveToAction(
        3, "Vulgar Display", "ult_weakenall", "Reduce all enemy Power by 1.");
  }

  // ******

  public static final boolean registerRequest(final boolean isExternal, final String urlString) {
    if (!urlString.startsWith("fight.php") && !urlString.startsWith("fambattle.php")) {
      return false;
    }

    FightRequest.nextAction = null;

    if (urlString.equals("fight.php") || urlString.contains("ireallymeanit=")) {
      if (FightRequest.inMultiFight || FightRequest.choiceFollowsFight) {
        RequestLogger.registerLastLocation();
      }
      return true;
    }

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = monster != null ? monster.getName() : "";

    // Begin logging all the different combat actions and storing
    // relevant data for post-processing.

    String limitmode = KoLCharacter.getLimitmode();
    boolean isBatfellow = (limitmode == Limitmode.BATMAN);
    String name = isBatfellow ? "Batfellow" : KoLCharacter.getUserName();

    boolean shouldLogAction = Preferences.getBoolean("logBattleAction");
    StringBuilder action = new StringBuilder();

    if (shouldLogAction) {
      action.append("Round ");
      action.append(FightRequest.currentRound);
      action.append(": ");
      action.append(name);

      if (urlString.startsWith("fambattle.php")) {
        // fambattle.php?pwd&famaction[ult_crazyblast-209]=ULTIMATE%3A+Spiky+Burst
        // fambattle.php?pwd&famaction[sting-98]=Sting
        // fambattle.php?pwd&famaction[backstab-209]=Backstab
        Matcher m = FightRequest.FAMBATTLE_PATTERN.matcher(urlString);
        if (m.find()) {
          int famtype = StringUtilities.parseInt(m.group(1));
          String famname = FamiliarDatabase.getFamiliarName(famtype);
          String skill = StringUtilities.getURLDecode(m.group(2));
          action.append("'s  ");
          action.append(famname);
          action.append(" uses ");
          action.append(skill);
          action.append("!");
          String message = action.toString();
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
        }
        return true;
      }

      action.append(" ");
    }

    if (urlString.contains("macro")) {
      Matcher m = FightRequest.WHICHMACRO_PATTERN.matcher(urlString);
      if (m.find()) {
        FightRequest.lastMacroUsed = m.group(1);
      }
      FightRequest.nextAction = "";
      if (shouldLogAction) {
        action.append("executes a macro!");
      }
    } else if (urlString.contains("runaway")) {
      FightRequest.nextAction = "runaway";
      if (shouldLogAction) {
        action.append("casts RETURN!");
      }
    } else if (urlString.contains("steal")) {
      FightRequest.nextAction = "steal";
      if (shouldLogAction) {
        action.append("tries to steal an item!");
      }
    } else if (urlString.contains("attack")) {
      FightRequest.nextAction = "attack";
      if (shouldLogAction) {
        action.append("attacks!");
      }
    } else if (urlString.contains("chefstaff")) {
      FightRequest.nextAction = "jiggle";
      if (shouldLogAction) {
        action.append("jiggles the ");
        action.append(EquipmentManager.getEquipment(EquipmentManager.WEAPON).getName());
      }
    } else if (urlString.contains("twiddle")) {
      FightRequest.nextAction = "twiddle";
      return true;
    } else {
      Matcher skillMatcher = FightRequest.SKILL_PATTERN.matcher(urlString);
      if (skillMatcher.find()) {
        String skillId = skillMatcher.group(1);
        if (FightRequest.isInvalidAttack(skillId)) {
          return true;
        }

        int skillNumber = StringUtilities.parseInt(skillId);
        String skill = SkillDatabase.getSkillName(skillNumber);
        if (skill == null) {
          if (shouldLogAction) {
            action.append("casts CHANCE!");
          }
        } else {
          FightRequest.nextAction =
              CombatActionManager.getShortCombatOptionName("skill " + skillNumber);
          if (shouldLogAction) {
            if (isBatfellow) {
              String verb = "uses ";
              switch (skillNumber) {
                case SkillPool.BAT_PUNCH:
                  verb = "throws a ";
                  break;
                case SkillPool.BAT_KICK:
                  verb = "does a ";
                  break;
                case SkillPool.BAT_OOMERANG:
                case SkillPool.BAT_JUTE:
                case SkillPool.BAT_O_MITE:
                case SkillPool.ULTRACOAGULATOR:
                case SkillPool.KICKBALL:
                case SkillPool.BAT_GLUE:
                case SkillPool.BAT_BEARING:
                case SkillPool.USE_BAT_AID:
                  break;
              }
              action.append(verb);
            } else {
              action.append("casts ");
            }
            action.append(skill.toUpperCase()).append("!");
          }
        }
      } else {
        Matcher itemMatcher = FightRequest.ITEM1_PATTERN.matcher(urlString);
        if (itemMatcher.find()) {
          int itemId = StringUtilities.parseInt(itemMatcher.group(1));
          String item = ItemDatabase.getItemName(itemId);
          if (item == null) {
            if (shouldLogAction) {
              action.append("plays Garin's Harp");
            }
          } else {
            if (item.equalsIgnoreCase("odor extractor")
                && !KoLConstants.activeEffects.contains(FightRequest.ONTHETRAIL)) {
              Preferences.setString("olfactedMonster", monsterName);
              Preferences.setString("autoOlfact", "");
              FightRequest.canOlfact = false;
            }
            FightRequest.nextAction = String.valueOf(itemId);
            if (shouldLogAction) {
              action.append("uses the ").append(item);
            }
          }

          itemMatcher = FightRequest.ITEM2_PATTERN.matcher(urlString);
          if (itemMatcher.find()) {
            itemId = StringUtilities.parseInt(itemMatcher.group(1));
            item = ItemDatabase.getItemName(itemId);
            if (item != null) {
              if (item.equalsIgnoreCase("odor extractor")
                  && !KoLConstants.activeEffects.contains(FightRequest.ONTHETRAIL)) {
                Preferences.setString("olfactedMonster", monsterName);
                Preferences.setString("autoOlfact", "");
              }

              FightRequest.nextAction += "," + itemId;
              if (shouldLogAction) {
                action.append(" and uses the ").append(item);
              }
            }
          }

          if (shouldLogAction) {
            action.append("!");
          }
        } else {
          System.out.println("unable to parse " + urlString);
        }
      }
    }

    if (shouldLogAction) {
      if (urlString.contains("[AA]")) { // pseudo-parameter for parsing an autoattack
        action.append(" (auto-attack)");
      }
      String message = action.toString();
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    return true;
  }
}
