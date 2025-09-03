package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.combat.CombatActionManager;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.BastilleBattalionManager;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ConsequenceManager;
import net.sourceforge.kolmafia.session.CrystalBallManager;
import net.sourceforge.kolmafia.session.DvorakManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.LimitMode;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.WumpusManager;
import net.sourceforge.kolmafia.swingui.RequestSynchFrame;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

public class AdventureRequest extends GenericRequest {
  public static final String NOT_IN_A_FIGHT = "Not in a Fight";

  private static final Pattern AREA_PATTERN =
      Pattern.compile("(adv|snarfblat)=(\\d*)", Pattern.DOTALL);

  // <img id='monpic' src="http://images.kingdomofloathing.com/adventureimages/ssd_sundae.gif"
  // width=100 height=100>
  private static final Pattern MONSTER_IMAGE =
      Pattern.compile("<img +id='monpic'.*?adventureimages/(.*?\\.gif)");

  private static final GenericRequest ZONE_UNLOCK = new GenericRequest("");

  private final String adventureName;
  private final String formSource;
  private final String adventureId;
  private final int adventureNumber;

  private int override = -1;

  public enum ShadowRift {
    BEACH("Desert Beach", "desertbeach", "db_shadowrift"),
    VILLAGE("Forest Village", "forestvillage", "fv_shadowrift"),
    MCLARGEHUGE("Mt. McLargeHuge", "mclargehuge", "mcl_shadowrift"),
    BEANSTALK("Somewhere Over the Beanstalk", "beanstalk", "stalk_rift"),
    MANOR("Spookyraven Manor Third Floor", "manor3", "manor3_shadowrift"),
    REALM("The 8-Bit Realm", "8bit", "8rift"),
    PYRAMID("The Ancient Buried Pyramid", "pyramid", "pyramid_shadowrift"),
    CASTLE("The Castle in the Clouds in the Sky", "giantcastle", "castle_shadowrift"),
    WOODS("The Distant Woods", "woods", "woods_shadowrift"),
    CITY("The Hidden City", "hiddencity", "hc_shadowrift"),
    CEMETARY("The Misspelled Cemetary", "cemetery", "cem_shadowrift"),
    PLAINS("The Nearby Plains", "plains", "plains_shadowrift"),
    TOWN("The Right Side of the Tracks", "town_right", "townright_shadowrift");

    // Class fields
    private final String container;
    private final String place;
    private final String action;

    // Derived fields
    private final String adventureName;
    private final String URL;
    private final String freeAction;
    private final String freeURL;

    // Lookups for Shadow Rifts
    private static final Map<String, ShadowRift> adventureNameToRift = new HashMap<>();
    private static final Map<String, ShadowRift> placeToRift = new HashMap<>();

    private static final AdventureResult SHADOW_AFFINITY =
        EffectPool.get(EffectPool.SHADOW_AFFINITY);

    private ShadowRift(String container, String place, String action) {
      this.container = container;
      this.place = place;
      this.action = action;

      // Derived fields
      this.adventureName = "Shadow Rift (" + container + ")";
      this.URL = "place.php?whichplace=" + place + "&action=" + action;
      this.freeAction = action + "_free";
      this.freeURL = "place.php?whichplace=" + place + "&action=" + this.freeAction;
    }

    public String getContainer() {
      return this.container;
    }

    public String getPlace() {
      return this.place;
    }

    public String getAction() {
      return this.action;
    }

    public String getFreeAction() {
      return this.freeAction;
    }

    public String getCurrentAction() {
      boolean free = KoLConstants.activeEffects.contains(SHADOW_AFFINITY);
      return free ? this.freeAction : this.action;
    }

    public String getAdventureName() {
      return this.adventureName;
    }

    public String getURL() {
      return this.URL;
    }

    public String getFreeURL() {
      return this.freeURL;
    }

    public String getCurrentURL() {
      boolean free = KoLConstants.activeEffects.contains(SHADOW_AFFINITY);
      return free ? this.freeURL : this.URL;
    }

    public void populateMaps() {
      ShadowRift.placeToRift.put(this.place, this);
      ShadowRift.adventureNameToRift.put(this.adventureName, this);
    }

    public static ShadowRift findPlace(String place) {
      return ShadowRift.placeToRift.get(place);
    }

    public static ShadowRift findAdventureName(String adventureName) {
      return ShadowRift.adventureNameToRift.get(adventureName);
    }
  }

  static {
    for (var rift : EnumSet.allOf(ShadowRift.class)) {
      rift.populateMaps();
    }
  }

  /**
   * Constructs a new <code>AdventureRequest</code> which executes the adventure designated by the
   * given Id by posting to the provided form, notifying the givenof results (or errors).
   *
   * @param adventureName The name of the adventure location
   * @param formSource The form to which the data will be posted
   * @param adventureId The identifier for the adventure to be executed
   */
  public AdventureRequest(
      final String adventureName, final String formSource, final String adventureId) {
    super(formSource);
    this.adventureName = adventureName;
    this.formSource = formSource;
    this.adventureId = adventureId;
    this.adventureNumber =
        StringUtilities.isNumeric(adventureId) ? StringUtilities.parseInt(adventureId) : 0;

    // The adventure Id is all you need to identify the adventure;
    // posting it in the form sent to adventure.php will handle
    // everything for you.
    // Those that change mid session should be added to run() also.

    switch (formSource) {
      case "adventure.php" -> {
        this.addFormField("snarfblat", adventureId);
      }
      case "casino.php" -> {
        this.addFormField("action", "slot");
        this.addFormField("whichslot", adventureId);
      }
      case "crimbo10.php" -> {
        this.addFormField("place", adventureId);
      }
      case "cobbsknob.php" -> {
        this.addFormField("action", "throneroom");
      }
      case "friars.php" -> {
        this.addFormField("action", "ritual");
      }
      case "invasion.php" -> {
        this.addFormField("action", adventureId);
      }
      case "mining.php" -> {
        this.addFormField("mine", adventureId);
      }
      case "place.php" -> {
        switch (adventureId) {
          case "cloudypeak2" -> {
            this.addFormField("whichplace", "mclargehuge");
            this.addFormField("action", adventureId);
          }
          case "crimbo22_engine" -> {
            this.addFormField("whichplace", "crimbo22");
            this.addFormField("action", adventureId);
          }
          case "ioty2014_wolf" -> {
            this.addFormField("whichplace", "ioty2014_wolf");
            this.addFormField("action", "wolf_houserun");
          }
          case "manor4_chamberboss" -> {
            this.addFormField("whichplace", "manor4");
            this.addFormField("action", adventureId);
          }
          case "ns_01_crowd1",
              "ns_01_crowd2",
              "ns_01_crowd3",
              "ns_03_hedgemaze",
              "ns_05_monster1",
              "ns_06_monster2",
              "ns_07_monster3",
              "ns_08_monster4",
              "ns_09_monster5",
              "ns_10_sorcfight" -> {
            this.addFormField("whichplace", "nstower");
            this.addFormField("action", adventureId);
          }
          case "pyramid_state" -> {
            this.addFormField("whichplace", "pyramid");
            StringBuilder action = new StringBuilder();
            action.append(adventureId);
            action.append(Preferences.getString("pyramidPosition"));
            if (Preferences.getBoolean("pyramidBombUsed")) {
              action.append("a");
            }
            this.addFormField("action", action.toString());
          }
          case "shadow_rift" -> {
            // This is a pseudo-place. Lookup adventureName to get place/action
            ShadowRift rift = ShadowRift.findAdventureName(adventureName);
            if (rift != null) {
              this.addFormField("whichplace", rift.getPlace());
              this.addFormField("action", rift.getAction());
            }
          }
          case "town_eincursion", "town_eicfight2" -> {
            this.addFormField("whichplace", "town");
            this.addFormField("action", adventureId);
          }
          case "townwrong_tunnel" -> {
            this.addFormField("whichplace", "town_wrong");
            this.addFormField("action", "townwrong_tunnel");
          }
        }
      }
      case "sea_merkin.php" -> {
        this.addFormField("action", "temple");
        String subaction =
            switch (adventureName) {
              case "Mer-kin Temple (Left Door)" -> "left";
              case "Mer-kin Temple (Center Door)" -> "center";
              case "Mer-kin Temple (Right Door)" -> "right";
              default -> null;
            };
        if (subaction != null) {
          this.addFormField("subaction", subaction);
        }
      }
      case "basement.php", "cellar.php" -> {}
      default -> {
        this.addFormField("action", adventureId);
      }
    }
  }

  public AdventureRequest(final String adventureName, final int snarfblat) {
    this(adventureName, "adventure.php", String.valueOf(snarfblat));
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public void run() {
    // Prevent the request from happening if they attempted
    // to cancel in the delay period.
    // *** What does that mean?

    if (!KoLmafia.permitsContinue()) {
      return;
    }

    // Pre-validate certain adventure locations
    switch (this.formSource) {
      case "adventure.php" -> {
        if (this.adventureNumber == AdventurePool.THE_SHORE) {
          // The Shore
          int adv = KoLCharacter.inFistcore() ? 5 : 3;
          if (KoLCharacter.getAdventuresLeft() < adv) {
            KoLmafia.updateDisplay(MafiaState.ERROR, "Ran out of adventures.");
            return;
          }
        }
      }
      case "cellar.php" -> {
        if (!TavernManager.shouldAutoFaucet() && TavernManager.recommendSquare() == 0) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "Don't know which square to visit in the Typical Tavern Cellar.");
          return;
        }
      }
      case "mining.php" -> {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Automated mining is not currently implemented.");
        return;
      }
    }

    // Update fields to submit, if necessary
    this.updateFields();

    super.run();
  }

  public void updateFields() {
    switch (this.formSource) {
      case "cellar.php" -> {
        if (TavernManager.shouldAutoFaucet()) {
          this.removeFormField("whichspot");
          this.addFormField("action", "autofaucet");
        } else {
          this.addFormField("whichspot", String.valueOf(TavernManager.recommendSquare()));
          this.addFormField("action", "explore");
        }
      }
      case "place.php" -> {
        switch (this.adventureId) {
          case "manor4_chamber" -> {
            this.addFormField("whichplace", "manor4");
            if (!QuestDatabase.isQuestFinished(Quest.MANOR)) {
              this.addFormField("action", "manor4_chamberboss");
            } else {
              this.addFormField("action", "manor4_chamber");
            }
          }
          case "pyramid_state" -> {
            this.addFormField("whichplace", "pyramid");
            StringBuilder action = new StringBuilder();
            action.append(adventureId);
            action.append(Preferences.getString("pyramidPosition"));
            if (Preferences.getBoolean("pyramidBombUsed")) {
              action.append("a");
            }
            this.addFormField("action", action.toString());
          }
          case "shadow_rift" -> {
            // If we are going to the "current" Shadow Rift Ingress, we can go
            // straight to adventure.php and avoid an extra redirection.
            ShadowRift rift = ShadowRift.findAdventureName(this.adventureName);
            if (rift != null) {
              String current = Preferences.getString("shadowRiftIngress");
              String desired = rift.getPlace();
              if (current.equals(desired)) {
                this.constructURLString("adventure.php");
                this.addFormField("snarfblat", String.valueOf(AdventurePool.SHADOW_RIFT));
              } else {
                this.constructURLString("place.php");
                this.addFormField("whichplace", rift.getPlace());
                this.addFormField("action", rift.getCurrentAction());
                Preferences.setString("shadowRiftIngress", desired);
              }
            }
          }
        }
      }
    }
  }

  @Override
  public void processResults() {
    // Sometimes, there's no response from the server.
    // In this case, skip and continue onto the next one.

    if (this.responseText == null
        || this.responseText.trim().length() == 0
        || this.responseText.contains("No, that isn't a place yet.")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't get to that area yet.");
      return;
    }

    if (this.formSource.equals("place.php")) {
      String location = this.getURLString();
      if (location.contains("whichplace=nstower")) {
        // nstower locations redirect to a fight or choice. If
        // it didn't do that, you can't adventure there.
        KoLmafia.updateDisplay(MafiaState.PENDING, "You can't adventure there.");
        SorceressLairManager.parseTowerResponse("", this.responseText);
        return;
      }
      if (location.contains("crimbo22_engine")) {
        // The engine redirects to a fight. If it didn't do that, you have
        // defeated the boss.
        KoLmafia.updateDisplay(MafiaState.PENDING, "Nothing more to do here.");
        Preferences.setBoolean("superconductorDefeated", true);
        return;
      }
    }

    // We're missing an item, haven't been given a quest yet, or
    // otherwise trying to go somewhere not allowed.

    int index = KoLAdventure.findAdventureFailure(this.responseText);
    if (index >= 0) {
      String failure = KoLAdventure.adventureFailureMessage(index);
      MafiaState severity = KoLAdventure.adventureFailureSeverity(index);

      // Add more details to the failure message when adventuring in the 2021 crimbo cold resistance
      // zones.
      if (this.formSource.equals("adventure.php")) {
        String zone = AdventureDatabase.getZone(this.adventureName);
        if ("Crimbo21".equals(zone)) {
          int required = Preferences.getInteger("_crimbo21ColdResistance");
          int current = KoLCharacter.getElementalResistanceLevels(MonsterDatabase.Element.COLD);
          if (current < required) {
            failure +=
                " You need " + required + " (" + (required - current) + " more than you have now).";
          }
        }
      }

      KoLmafia.updateDisplay(severity, failure);
      this.override = 0;
      return;
    }

    // This is a server error. Hope for the best and repeat the
    // request.

    if (this.responseText.contains("No adventure data exists for this location")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Server error.  Please wait and try again.");
      return;
    }

    // Nothing more to do in this area

    if (this.formSource.equals("adventure.php")) {
      if (this.adventureNumber == AdventurePool.MERKIN_COLOSSEUM) {
        SeaMerkinRequest.parseColosseumResponse(this.getURLString(), this.responseText);
      }

      if (!this.responseText.contains("adventure.php")
          && !this.responseText.contains("You acquire")) {
        if (!EncounterManager.isAutoStop(this.encounter)) {
          KoLmafia.updateDisplay(MafiaState.PENDING, "Nothing more to do here.");
        }

        return;
      }
    }

    // If you're at the casino, each of the different slot
    // machines deducts meat from your tally

    if (this.formSource.equals("casino.php")) {
      switch (this.adventureId) {
        case "1" -> ResultProcessor.processMeat(-5);
        case "2" -> ResultProcessor.processMeat(-10);
        case "11" -> ResultProcessor.processMeat(-10);
      }
    }

    if (this.adventureNumber == AdventurePool.ROULETTE_TABLES) {
      ResultProcessor.processMeat(-10);
    } else if (this.adventureId.equals(String.valueOf(AdventurePool.POKER_ROOM))) {
      ResultProcessor.processMeat(-30);
    }

    // Trick-or-treating requires a costume;
    // notify the user of this error.

    if (this.formSource.equals("trickortreat.php")
        && this.responseText.contains("without a costume")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You must wear a costume.");
      return;
    }
  }

  public static final String registerEncounter(final GenericRequest request) {
    // No encounters in chat!
    if (request.isChatRequest) {
      return "";
    }

    String urlString = request.getURLString();
    String responseText = request.responseText;
    boolean isFight = urlString.startsWith("fight.php") || urlString.startsWith("fambattle.php");
    boolean isChoice = urlString.startsWith("choice.php");

    // If we were redirected into a fight or a choice through using
    // an item, there will be an encounter in the responseText.
    // Otherwise, if KoLAdventure didn't log the location, there
    // can't be an encounter for us to log.

    if (GenericRequest.itemMonster == null
        && !KoLAdventure.recordToSession(urlString, responseText)) {
      return "";
    }

    if (!(request instanceof AdventureRequest)
        && !AdventureRequest.containsEncounter(urlString, responseText)) {
      return "";
    }

    String encounter = null;
    String type = null;

    if (isFight) {
      type = "Combat";
      encounter = parseCombatEncounter(responseText);
    } else if (isChoice) {
      int choice = ChoiceUtilities.extractChoice(responseText);
      type = choiceType(choice);
      encounter = parseChoiceEncounter(urlString, choice, responseText);
      ChoiceManager.registerDeferredChoice(choice, encounter);
    } else {
      type = "Noncombat";
      encounter = parseNoncombatEncounter(urlString, responseText);
      if (responseText.contains("charpane.php")) {
        // Since a charpane refresh was requested, this might have taken a turn
        AdventureSpentDatabase.setNoncombatEncountered(true);
      }
    }

    if (encounter == null) {
      return "";
    }

    // Silly check for silly situation
    if (encounter == AdventureRequest.NOT_IN_A_FIGHT) {
      return encounter;
    }

    if (isFight) {
      FightRequest.setCurrentEncounter(encounter);
    }

    if (KoLCharacter.inDisguise()) {
      encounter =
          switch (encounter) {
            case "The Bonerdagon" -> "Boss Bat wearing a Bonerdagon mask";
            case "The Naughty Sorceress" -> "Knob Goblin King wearing a Naughty Sorceress mask";
            case "Groar" -> "Bonerdagon wearing a Groar mask";
            case "Ed the Undying" -> "Groar wearing an Ed the Undying mask";
            case "The Big Wisniewski" -> "The Man wearing a Big Wisniewski mask";
            case "The Man" -> "The Big Wisniewski wearing a The Man mask";
            case "The Boss Bat" -> "Naughty Sorceress wearing a Boss Bat mask";
            default -> encounter;
          };
    }

    String prettyEncounter = StringUtilities.getEntityDecode(encounter);

    Preferences.setString("lastEncounter", prettyEncounter);
    RequestLogger.printLine("Encounter: " + prettyEncounter);
    RequestLogger.updateSessionLog("Encounter: " + prettyEncounter);
    AdventureRequest.registerDemonName(encounter, responseText);

    var location = KoLAdventure.lastVisitedLocation();
    var encounterData = EncounterManager.findEncounter(location, encounter);

    // We are done registering the item's encounter.
    if (type != null) {
      if (type.equals("Combat")) {
        MonsterData monster = AdventureRequest.extractMonster(encounter, responseText);

        // In Ed we'll only set the monster name the first time we encounter the monster
        if (!KoLCharacter.isEd() || Preferences.getInteger("_edDefeats") == 0) {
          MonsterStatusTracker.setNextMonster(monster);
        }

        // Because relativity monster detection relies on a preference being true that is set back
        // to false after being
        // read, we can only detect it once. So let's do it here, and then rely on ignoring special
        // monsters later.
        if (EncounterManager.isRelativityMonster() || EncounterManager.isAfterimageMonster()) {
          EncounterManager.ignoreSpecialMonsters();
        }

        encounter = monster.getName();
        // Only queue normal monster encounters
        if (EncounterManager.isGregariousEncounter(responseText)
            || (!EncounterManager.ignoreSpecialMonsters
                && !EncounterManager.isWanderingMonster(encounter)
                && !EncounterManager.isUltrarareMonster(encounter)
                && !EncounterManager.isLuckyMonster(encounter)
                && !EncounterManager.isSuperlikelyMonster(encounter)
                && !EncounterManager.isFreeCombatMonster(encounter)
                && !EncounterManager.isNoWanderMonster(encounter)
                && !EncounterManager.isEnamorangEncounter(responseText, false)
                && !EncounterManager.isDigitizedEncounter(responseText, false)
                && !EncounterManager.isRomanticEncounter(responseText, false)
                && !EncounterManager.isSaberForceMonster()
                && !CrystalBallManager.isCrystalBallMonster()
                && !EncounterManager.isRainManEncounter(responseText)
                && !EncounterManager.isSpookyVHSTapeMonster(responseText, false)
                && !EncounterManager.isMimeographEncounter(responseText)
                && !EncounterManager.isBodyguardEncounter(responseText)
                && !FightRequest.edFightInProgress())) {
          AdventureQueueDatabase.enqueue(location, encounter);
        }
      } else if (type.equals("Noncombat")
          // Don't enqueue Lucky, hallowiener, etc. adventures.
          && (encounterData == null
              || encounterData.getEncounterType() == EncounterManager.EncounterType.NONE
              || encounterData.getEncounterType() == EncounterManager.EncounterType.STOP)
          // Special-case first two NCs in the Upper Chamber, which are superlikely.
          // In a perfect world, Mafia would have a list of all superlikelies, but this works for
          // now.
          && !(AdventureSpentDatabase.getTurns(
                      AdventureDatabase.getAdventure(AdventurePool.UPPER_CHAMBER))
                  == 2
              && encounter.equals("A Wheel -- How Fortunate!"))
          && !(encounter.equals("Down Dooby-Doo Down Down"))) {
        // only log the FIRST choice that we see in a choiceadventure chain.
        if ((!urlString.startsWith("choice.php") || ChoiceManager.getLastChoice() == 0)
            && !FightRequest.edFightInProgress()) {
          AdventureQueueDatabase.enqueueNoncombat(location, encounter);
          if (location != null) {
            var preference = "lastNoncombat" + location.getAdventureId();
            if (location.getForceNoncombat() > 0 && Preferences.containsDefault(preference)) {
              Preferences.setInteger(preference, AdventureSpentDatabase.getTurns(location));
            }
          }
        }
      }
      EncounterManager.registerEncounter(encounter, type, responseText);
    }

    TurnCounter.handleTemporaryCounters(type, encounter);

    return encounter;
  }

  private static String fromName = null;
  private static String toName = null;

  public static final void setNameOverride(final String from, final String to) {
    fromName = from;
    toName = to;
  }

  private static final Pattern[] MONSTER_NAME_PATTERNS = {
    Pattern.compile("You're fighting <span id='monname'> *(.*?)</span>", Pattern.DOTALL),
    // papier weapons can change "fighting" to some other verb
    Pattern.compile("You're (?:<u>.*?</u>) <span id='monname'>(.*?)</span>", Pattern.DOTALL),
    // Shrunken adventurers have this changed for some reason
    Pattern.compile(
        "You're a tiny adventurer fighting <span id='monname'> *(.*?)</span>", Pattern.DOTALL),
    // Pocket Familiars have Pokefam battles
    // <b><center>a fleet woodsman's Team:</b>
    Pattern.compile(">([^<]*?)'s Team:<"),
    // Knob Goblin poseur has no closing </b> tag
    Pattern.compile(
        "<td id='fmsg' valign=center><Table>.*?<b>(Knob Goblin poseur)", Pattern.DOTALL),
    // haiku dungeon attempt
    Pattern.compile("<td id='fmsg' valign=center><Table>.*?<b>([^<]+)</b>", Pattern.DOTALL),
    // KoL sure generates a lot of bogus HTML
    Pattern.compile("<b>.*?(<b>.*?<(/b|/td)>.*?)<(br|/td|/tr)>", Pattern.DOTALL),
  };

  public static final String parseCombatEncounter(final String responseText) {
    // Silly check for silly situation
    if (responseText.contains("Not in a Fight")) {
      return AdventureRequest.NOT_IN_A_FIGHT;
    }

    String name = null;

    for (Pattern pattern : MONSTER_NAME_PATTERNS) {
      Matcher matcher = pattern.matcher(responseText);
      if (matcher.find()) {
        name = matcher.group(1);
        break;
      }
    }

    if (name == null) {
      return "";
    }

    // If the name has bold markup, strip formatting
    name = StringUtilities.globalStringReplace(name, "<b>", "");
    name = StringUtilities.globalStringReplace(name, "</b>", "");

    // Brute force fix for haiku dungeon monsters, which have
    // punctuation at the end because of bad HTML
    name =
        name.startsWith("amateur ninja")
            ? "amateur ninja"
            : name.startsWith("ancient insane monk")
                ? "ancient insane monk"
                : name.startsWith("Ferocious bugbear")
                    ? "ferocious bugbear"
                    : name.startsWith("gelatinous cube")
                        ? "gelatinous cube"
                        : name.startsWith("Knob Goblin poseur") ? "Knob Goblin poseur" : name;

    // Canonicalize
    name = CombatActionManager.encounterKey(name, false);

    // Coerce name if needed
    if (name.equalsIgnoreCase(fromName)) {
      name = CombatActionManager.encounterKey(toName, false);
    }
    fromName = null;

    EquipmentManager.decrementTurns();
    return name;
  }

  // <!-- MONSTERID: 112 -->
  private static final Pattern MONSTERID_PATTERN = Pattern.compile("<!-- MONSTERID: (\\d+) -->");

  private static final MonsterData WUMPUS = MonsterDatabase.findMonster("wumpus");
  private static final MonsterData THE_DARKNESS =
      MonsterDatabase.findMonster("the darkness (blind)");

  public static final MonsterData extractMonster(
      final String encounterToCheck, final String responseText) {
    // We need to extract the random modifiers and masks, since
    // those will be added to the MonsterData
    String encounter =
        AdventureRequest.handleRandomModifiers(encounterToCheck.trim(), responseText);
    encounter = AdventureRequest.handleIntergnat(encounter);
    encounter = AdventureRequest.handleNuclearAutumn(encounter);
    encounter = AdventureRequest.handleMask(encounter);
    encounter = AdventureRequest.handleDinosaurs(encounter);
    encounter = AdventureRequest.handleHats(encounter);

    // KoL now provides MONSTERID in fight responseText.
    Matcher m = MONSTERID_PATTERN.matcher(responseText);

    if (!m.find()) {
      // It does not do this if and only if you are blind, or in pokefam
      if (responseText.contains("darkness.gif")) {
        // Adventuring in the Wumpus cave while temporarily blind is
        // foolish, but since we won't clear the cave after defeating
        // it if we can't recognize it, allow for it
        return WumpusManager.isWumpus() ? WUMPUS : THE_DARKNESS;
      }

      if (!KoLCharacter.inPokefam()) {
        // As of 16-June-2020, KoL will provide MONSTERID with
        // every round of combat. If it fails to do so when you
        // are not blind, That would be a bug. Log it and
        // attempt to identify the monster by name.

        StaticEntity.printDebugText("MONSTERID not found", responseText);
      }

      encounter = ConsequenceManager.disambiguateMonster(encounter, responseText);
      MonsterData monster = MonsterDatabase.findMonster(encounter);
      return (monster != null) ? monster : MonsterDatabase.registerMonster(encounter);
    }

    int monsterId = StringUtilities.parseInt(m.group(1));
    MonsterData monster = MonsterDatabase.findMonsterById(monsterId);

    // Do we know this monster id?
    if (monster != null) {
      // Yes. Send through ConsequenceManager. This is how we
      // disambiguate Ed the Undying into Ed the Undying (4),
      // for example.
      String monsterName = monster.getName();
      String disambiguated = ConsequenceManager.disambiguateMonster(monsterName, responseText);
      if (!monsterName.equals(disambiguated)) {
        return MonsterDatabase.findMonster(disambiguated);
      }
      return monster;
    }

    // No! Is this a monster for which we have a pseudo-ID?
    encounter = ConsequenceManager.disambiguateMonster(encounter, responseText);
    monster = MonsterDatabase.findMonster(encounter);
    String image = null;
    if (monster != null) {
      // Yes. We've learned the actual monsterId!
      MonsterDatabase.setMonsterId(monster, monsterId);
      image = monster.getImage();
    } else {
      // It's a brand-new monster. Register it.
      Matcher i = AdventureRequest.MONSTER_IMAGE.matcher(responseText);
      image = i.find() ? i.group(1) : "";
      monster = MonsterDatabase.registerMonster(encounter, monsterId, image);
    }

    String message =
        "*** Monster '"
            + encounter
            + "' has monsterId = "
            + monsterId
            + " and image '"
            + image
            + "'";
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
    return monster;
  }

  public static String parseChoiceEncounter(
      final String urlString, final int choice, final String responseText) {
    if (LouvreManager.louvreChoice(choice)) {
      return LouvreManager.encounterName(choice);
    }

    int urlChoice = ChoiceUtilities.extractChoiceFromURL(urlString);
    int urlOption = ChoiceUtilities.extractOptionFromURL(urlString);

    switch (urlChoice) {
      case 1334 -> { // Boxing Daycare (Lobby)
        if (urlOption == 1) {
          // Have a Boxing Daydream
          return "Have a Boxing Daydream";
        }
        return null;
      }
      case 1335 -> { // Boxing Day Spa
        if (urlOption >= 1 && urlOption <= 4) {
          // (Get a buff)
          return "Visit the Boxing Day Spa";
        }
        return null;
      }
      case 1336 -> { // Boxing Daycare
        if (urlOption >= 1 && urlOption <= 4) {
          // (recruit, scavenge, hire, spar)
          return "Enter the Boxing Daycare";
        }
        return null;
      }
    }

    switch (choice) {
      case 443: // Chess Puzzle
        // No "encounter" when moving on the chessboard
        if (urlString.contains("xy")) {
          return null;
        }
        break;

      case 1085: // Deck of Every Card
        return DeckOfEveryCardRequest.parseCardEncounter(responseText);

      case 535: // Deep Inside Ronald, Baby
      case 536: // Deep Inside Grimace, Bow Chick-a Bow Bow
      case 585: // Screwing Around!
      case 595: // Fire! I... have made... fire!
      case 807: // Breaker Breaker!
      case 1003: // Test Your Might And Also Test Other Things
      case 1086: // Pick a Card
      case 1218: // Wax On
      case 1463: // Reminiscing About Those Monsters You Fought
        return null;

      case 1313: // Bastille Battalion
      case 1314: // Bastille Battalion (Master of None)
      case 1315: // Castle vs. Castle
      case 1316: // GAME OVER
      case 1317: // A Hello to Arms (Battalion)
      case 1318: // Defensive Posturing
      case 1319: // Cheese Seeking Behavior
        return BastilleBattalionManager.parseChoiceEncounter(choice, responseText);

      case 1135: // The Bat-Sedan
        return BatManager.parseBatSedan(responseText);

      case 1388:
        {
          if (!BeachCombRequest.containsEncounter(urlString)) {
            return null;
          }
          break;
        }
    }

    // No "encounter" for certain arcade games
    if (ArcadeRequest.arcadeChoice(choice)) {
      return null;
    }

    if (ChoiceManager.canWalkFromChoice(choice)) {
      return null;
    }

    String encounter = AdventureRequest.parseEncounter(responseText);

    // If KoL redirects to exactly "choice.php", it might contain a new
    // encounter (mostly on older choice pages) or the user could have
    // refreshed a choice page that you cannot walk away from.
    //
    // If the encounter is identical to what we saved, assume it is a refresh.
    if (urlString.equals("choice.php")) {
      String prettyEncounter = StringUtilities.getEntityDecode(encounter);
      if (prettyEncounter.equals(Preferences.getString("lastEncounter"))) {
        return null;
      }
    }

    return encounter;
  }

  private static String choiceType(final int choice) {
    if (LouvreManager.louvreChoice(choice)) {
      return null;
    }

    return "Noncombat";
  }

  private static final String[][] LIMERICKS = {
    {"Nantucket Snapper", "ancient old turtle"},
    {"The Apathetic Lizardman", "lizardman quite apathetic"},
    {"The Bleary-Eyed Cyclops", "bleary-eyed cyclops"},
    {"The Crass Goblin", "goblin is crass"},
    {"The Crotchety Wizard", "crotchety wizard"},
    {"The Dumb Minotaur", "dumb minotaur"},
    {"The Fierce-Looking Giant", "fierce-looking giant"},
    {"The Gelatinous Cube", "gelatinous cube"},
    {"The Gnome with a Truncheon", "gnome with a truncheon"},
    {"The Goblin King's Vassal", "Goblin King's vassal"},
    {"The Insatiable Maiden", "insatiable maiden"},
    {"The Jewelry Gnoll", "bejeweled it"},
    {"The Martini Booth", "martini booth"},
    {"The One-Legged Trouser", "one-pantlegged schnauzer"},
    {"The Orc With a Spork", "waving a spork"},
    {"The Slime Puddle", "slime puddle"},
    {"The Sozzled Old Dragon", "sozzled old dragon"},
    {"The Superior Ogre", "I am superior"},
    {"The Unguarded Chest", "chest full of meat"},
    {"The Unpleasant Satyr", "unpleasant satyr"},
    {"The Vampire Buffer", "vampire buffer"},
    {"The Weathered Old Chap", "weathered old chap"},
    {"The Witch", "A witch"},
    {"Thud", "hobo glyphs"},
  };

  private static String parseNoncombatEncounter(final String urlString, final String responseText) {
    // Fernswarthy's Basement
    if (urlString.startsWith("basement.php")) {
      return null;
    }

    if (urlString.startsWith("adventure.php")) {
      int area = parseArea(urlString);
      switch (area) {
        case AdventurePool.HIDDEN_TEMPLE:
          // Dvorak's revenge
          // You jump to the last letter, and put your pom-poms down with a sign of relief --
          // thank goodness that's over. Worst. Spelling bee. Ever.
          if (responseText.contains("put your pom-poms down")) {
            QuestDatabase.setQuestProgress(Quest.WORSHIP, "step2");
          }
          break;

        case AdventurePool.LIMERICK_DUNGEON:
          for (int i = 0; i < LIMERICKS.length; ++i) {
            if (responseText.contains(LIMERICKS[i][1])) {
              return LIMERICKS[i][0];
            }
          }
          return "Unrecognized Limerick";

        case AdventurePool.SPOOKY_GRAVY_BURROW:
          // The Spooky Wheelbarrow no longer shows an encounter.
          // Bug reported, but we can work around it.
          //
          // As you explore the Spooky Underground Caverns, you stop short when
          // you hear a noise. You duck behind a pillar of rock, and peek out
          // to see a Spooky Gravy Fairy pushing a small wheelbarrow, whistling
          // a gloomy dirge. As he goes past, you sneak out and thump him on
          // the head. He squeaks and falls over unconscious.
          //
          // His wheelbarrow has two little buckets in it. Score! (Maybe.)
          if (responseText.contains("pushing a small wheelbarrow")) {
            return "Spooky Wheelbarrow";
          }
          break;

        case AdventurePool.OUTSKIRTS_OF_THE_KNOB:
          // Unstubbed
          // You go back to the tree where the wounded Knob Goblin guard was resting,
          // and find him just where you left him, continuing to whine about his stubbed toe.
          //
          // "Here you go, tough guy" you say, and hand him the unguent.
          if (responseText.contains("you say, and hand him the unguent")) {
            ResultProcessor.processItem(ItemPool.PUNGENT_UNGUENT, -1);
          }
          break;
      }
    }

    return parseEncounter(responseText);
  }

  private static final Pattern BOLD_ENCOUNTER =
      Pattern.compile("<b(?:| [^>]*)>(.*?)</b>", Pattern.DOTALL);

  public static String parseEncounter(final String responseText) {
    // Look only in HTML body; the header can have scripts with
    // bold text.
    int index = responseText.indexOf("<body>");

    // Skip past the Adventure Results
    int brIndex = responseText.indexOf("Results:</b>", index);
    if (brIndex != -1) {
      int resultsIndex = responseText.indexOf("<div id=\"results\">", index);
      if (resultsIndex != -1) {
        // KoL was nice enough to put results into a div for us
        index = responseText.indexOf("</div>", resultsIndex);
      } else {
        // There is no results div, but it doesn't mean that
        // there aren't results. Nothing like consistency. Not.
        index = brIndex;
      }
    }

    if (index == -1) {
      // something has gone horribly wrong
      return "";
    }

    Matcher boldMatch = BOLD_ENCOUNTER.matcher(responseText);
    if (!boldMatch.find(index)) {
      return "";
    }

    return ChoiceUtilities.stripDevReadout(boldMatch.group(1).trim());
  }

  public static int parseArea(final String urlString) {
    Matcher matcher = AREA_PATTERN.matcher(urlString);
    if (matcher.find()) {
      return StringUtilities.parseInt(matcher.group(2));
    }

    return -1;
  }

  record Demon(String place, Pattern pattern, String text, String setting) {}

  private static final Demon[] demons = {
    new Demon(
        "Summoning Chamber",
        Pattern.compile("Did you say your name was (.*?)\\?"),
        "delicious-looking pies",
        "demonName1"),
    new Demon(
        "Hoom Hah", Pattern.compile("(.*?)! \\1, cooooome to meeeee!"), "fifty meat", "demonName2"),
    new Demon(
        "Every Seashell Has a Story to Tell If You're Listening",
        Pattern.compile("Hello\\? Is (.*?) there\\?"),
        "fish-guy",
        "demonName3"),
    new Demon(
        "Leavesdropping",
        Pattern.compile("(.*?), we call you! \\1, come to us!"),
        "bullwhip",
        "demonName4"),
    new Demon(
        "These Pipes... Aren't Clean!",
        Pattern.compile("Blurgle. (.*?). Gurgle. By the way,"),
        "coprodaemon",
        "demonName5"),
    new Demon(
        "Flying In Circles",
        // SC: Then his claws slip, and he falls
        // backwards.<p>"<Demon Name>!" he screams as he
        // tumbles backwards. "LORD OF REVENGE! GIVE ME
        // STRENGTH!"
        //
        // TT: With a scrape, her sickle slips from the
        // rock.<p>"<Demon Name>" she shrieks as she plummets
        // toward the lava. "Lord of Revenge! I accept your
        // contract! Give me your power!"
        //
        // PA: Its noodles lose their grip, and the evil
        // pastaspawn falls toward the churning
        // lava.<p><i>"<Demon Name>!"</i> it howls. "<i>Lord of
        // Revenge! Come to my aid!</i>"
        //
        // SA: As it falls, a mouth opens on its surface and
        // howls: "<Demon Name>! Revenge!"
        //
        // DB: His grip slips, and he falls.<p>"<Demon Name>!
        // Lord of Revenge! I call to you!  I pray to you! Help
        // m--"
        //
        // AT: His grip slips, and he tumbles
        // backward.<p>"<Demon Name>!" he screams. "Emperador
        // de la Venganza! Come to my aid!  I beg of you!"

        Pattern.compile(
            "(?:he falls backwards|her sickle slips from the rock|falls toward the churning lava|a mouth opens on its surface and howls|His grip slips, and he falls|he tumbles backward).*?(?:<i>)?&quot;(.*?)!?&quot;(?:</i>)?(?: he screams| she shrieks| it howls| Revenge| Lord of Revenge)"),
        "Lord of Revenge",
        "demonName8"),
    new Demon(
        "Sinister Ancient Tablet",
        Pattern.compile("<font.*?color=#cccccc>(.*?)</font>"),
        "flame-wreathed mouth",
        "demonName9"),
    new Demon(
        "Strange Cube",
        Pattern.compile("Come to me! Come to (.*?)!"),
        "writhing and twisting snake",
        "demonName10"),
    new Demon(
        "Where Have All The Drunkards Gone?",
        Pattern.compile("Is (.*?) a word?"),
        "Gary's friend",
        "demonName11"),
  };

  private static final Pattern NAME_PATTERN = Pattern.compile("<b>&quot;(.*?)&quot;</b>");

  public static final boolean registerDemonName(final String encounter, final String responseText) {
    String place = null;
    String demon = null;
    String setting = null;

    for (int i = 0; i < AdventureRequest.demons.length; ++i) {
      Demon demons = AdventureRequest.demons[i];
      place = demons.place;
      if (place == null || !place.equals(encounter)) {
        continue;
      }

      Pattern pattern = demons.pattern;
      Matcher matcher = pattern.matcher(responseText);

      if (matcher.find()) {
        // We found the name
        demon = matcher.group(1);
        setting = demons.setting;
      }

      break;
    }

    // If we didn't recognize the demon and he used a valid name in
    // the Summoning Chamber, we can deduce which one it is from
    // the result text

    if (setting == null && encounter.equals("Summoning Chamber")) {
      place = encounter;
      Matcher matcher = AdventureRequest.NAME_PATTERN.matcher(responseText);
      if (!matcher.find()) {
        return false;
      }

      // Save the name he used.
      demon = matcher.group(1);

      // Look for tell-tale string
      for (int i = 0; i < AdventureRequest.demons.length; ++i) {
        Demon demons = AdventureRequest.demons[i];
        String text = demons.text;
        if (responseText.contains(text)) {
          setting = demons.setting;
          break;
        }
      }
    }

    // Couldn't figure out which demon he called.
    if (setting == null) {
      return false;
    }

    String previousName = Preferences.getString(setting);
    if (previousName.equals(demon)) {
      // Valid demon name
      return true;
    }

    RequestLogger.printLine("Demon name: " + demon);
    RequestLogger.updateSessionLog("Demon name: " + demon);
    Preferences.setString(setting, demon);

    GoalManager.checkAutoStop(place); //

    // Valid demon name
    return true;
  }

  private static boolean containsEncounter(final String formSource, final String responseText) {
    if (formSource.startsWith("adventure.php")) {
      return true;
    } else if (formSource.startsWith("fight.php") || formSource.startsWith("fambattle.php")) {
      return FightRequest.getCurrentRound() == 0;
    } else if (formSource.startsWith("choice.php")) {
      return responseText.contains("choice.php");
    } else if (formSource.startsWith("cave.php")) {
      return formSource.contains("sanctum");
    } else if (formSource.startsWith("cobbsknob.php")) {
      return formSource.contains("throneroom");
    } else if (formSource.startsWith("crypt.php")) {
      return formSource.contains("action");
    } else if (formSource.startsWith("cellar.php")) {
      // Simply visiting the map is not an encounter.
      return !formSource.equals("cellar.php");
    } else if (formSource.startsWith("suburbandis.php")) {
      return formSource.contains("action=dothis");
    } else if (formSource.equals("elvmachine.php")) {
      return true;
    } else if (formSource.startsWith("tiles.php")) {
      // Only register initial encounter of Dvorak's Revenge
      DvorakManager.saveResponse(responseText);
      return responseText.contains("I before E, except after C");
    } else if (formSource.startsWith("mining.php")) {
      if (formSource.contains("which=")) {
        if (!formSource.contains("mine=6")) {
          // The Velvet / Gold Mine (mine=6) does not show up as the Last Adventure in
          // charpane/api, so we can't track turns_spent there.  To avoid counting it
          // for the wrong location, don't count it anywhere.
          AdventureSpentDatabase.setNoncombatEncountered(true);
        }
      }
      return false;
    }

    // It is not a known adventure.
    // Therefore, do not log the encounter yet.

    return false;
  }

  public static int getAdventuresUsed(final String urlString) {
    KoLAdventure adventure = AdventureDatabase.getAdventureByURL(urlString);
    return adventure == null ? 0 : adventure.getRequest().getAdventuresUsed();
  }

  @Override
  public int getAdventuresUsed() {
    if (this.override >= 0) {
      return this.override;
    }
    var limitmode = KoLCharacter.getLimitMode();
    if (limitmode == LimitMode.SPELUNKY || limitmode == LimitMode.BATMAN) {
      return 0;
    }
    if (this.adventureNumber == AdventurePool.THE_SHORE) {
      return KoLCharacter.inFistcore() ? 5 : 3;
    }
    if (this.adventureName.equals("The Lower Chambers")) {
      return PyramidRequest.lowerChamberTurnsUsed();
    }
    if (this.adventureName.equals("The Typical Tavern Cellar")) {
      return this.getURLString().contains("action=explore") ? 1 : 0;
    }
    if (AdventureDatabase.isUnderwater(this.adventureName)) {
      return KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.FISHY)) ? 1 : 2;
    }
    return 1;
  }

  public void overrideAdventuresUsed(int used) {
    this.override = used;
  }

  @Override
  public String toString() {
    return this.adventureName;
  }

  public static final void handleServerRedirect(String redirectLocation, boolean usePostMethod) {
    if (redirectLocation.contains("main.php")) {
      return;
    }

    // Yes, there really is a typo in the URL
    if (redirectLocation.startsWith("camground.php")) {
      redirectLocation = "campground.php";
    }

    AdventureRequest.ZONE_UNLOCK.constructURLString(redirectLocation, usePostMethod);

    if (redirectLocation.contains("palinshelves.php")) {
      AdventureRequest.ZONE_UNLOCK.run();
      AdventureRequest.ZONE_UNLOCK
          .constructURLString(
              "palinshelves.php?action=placeitems&whichitem1=2259&whichitem2=2260&whichitem3=493&whichitem4=2261")
          .run();
      return;
    }

    if (redirectLocation.contains("tiles.php")) {
      AdventureRequest.handleDvoraksRevenge(AdventureRequest.ZONE_UNLOCK);
      return;
    }

    if (redirectLocation.contains("place.php")) {
      AdventureRequest.ZONE_UNLOCK.run();
      // Don't error out if it's just a redirect to a container zone
      // e.g. Using grimstone mask, with choice adventure autoselected
      return;
    }

    if (redirectLocation.startsWith("witchess.php")) {
      // Grabbing the buff from Witchess
      AdventureRequest.ZONE_UNLOCK.run();
      return;
    }

    if (redirectLocation.startsWith("campground.php")) {
      // Submitting a Source Terminal request with a bad
      // option leaves the Source Terminal by redirecting to
      // the campground.
      AdventureRequest.ZONE_UNLOCK.run();
      return;
    }

    if (redirectLocation.startsWith("shop.php")) {
      // The Shore Inc. can redirect to the gift shop.
      AdventureRequest.ZONE_UNLOCK.run();
      return;
    }

    if (redirectLocation.startsWith("council.php") || redirectLocation.startsWith("ascend.php")) {
      // Community Service can redirect to both council.php and ascend.php
      AdventureRequest.ZONE_UNLOCK.run();
      return;
    }

    if (redirectLocation.startsWith("elvmachine.php")) {
      // El Vibrato Island Machinations
      AdventureRequest.ZONE_UNLOCK.run();
      return;
    }

    if (redirectLocation.startsWith("inventory.php")) {
      // Autumnaton's "Back to Inventory"
      AdventureRequest.ZONE_UNLOCK.run();
      return;
    }

    RequestSynchFrame.showRequest(AdventureRequest.ZONE_UNLOCK);
    RequestLogger.printLine("Unrecognized choice.php redirect: " + redirectLocation);
    RequestLogger.updateSessionLog("Unrecognized choice.php redirect: " + redirectLocation);
    KoLmafia.updateDisplay(MafiaState.ABORT, "Unknown adventure type encountered.");
  }

  public static final void handleDvoraksRevenge(final GenericRequest request) {
    EncounterManager.registerEncounter("Dvorak's Revenge", "Noncombat", null);
    RequestLogger.printLine("Encounter: Dvorak's Revenge");
    RequestLogger.updateSessionLog("Encounter: Dvorak's Revenge");

    request.run();
    DvorakManager.solve();
  }

  private static String handleRandomModifiers(String monsterName, final String responseText) {
    MonsterData.lastRandomModifiers.clear();
    if (!responseText.contains("var ocrs")) {
      return monsterName;
    }

    HtmlCleaner cleaner = HTMLParserUtils.configureDefaultParser();
    String xpath = "//script/text()";
    TagNode doc;
    doc = cleaner.clean(responseText);

    Object[] result;
    try {
      result = doc.evaluateXPath(xpath);
    } catch (XPatherException ex) {
      return monsterName;
    }

    String text = "";
    for (Object result1 : result) {
      text = result1.toString();
      if (text.startsWith("var ocrs")) {
        break;
      }
    }

    ArrayList<String> internal = new ArrayList<>();
    String[] temp = text.split("\"");

    for (int i = 1; i < temp.length - 1; i++) { // The first and last elements are never useful
      if (!temp[i].contains(":") && !temp[i].equals(",")) {
        internal.add(temp[i]);
      }
    }

    // Before we remove the random modifiers, if the monster has
    // "The " in front of the adjectives, remove it. This allows us
    // to recognize that "The 1337 N4ugh7y 50rc3r355" is actually
    // "Naughty Sorceress", which is Manuel's name for her.

    String trimmed = "";
    if (monsterName.startsWith("The ") || monsterName.startsWith("the ")) {
      trimmed = monsterName.substring(0, 4);
      monsterName = monsterName.substring(4);
    }

    // Ditto for "a " to recognize the aliases for Your Shadow

    else if (monsterName.startsWith("a ")) {
      trimmed = monsterName.substring(0, 2);
      monsterName = monsterName.substring(2);
    }

    int count = internal.size() - 1;
    boolean leet = false;

    for (int j = 0; j <= count; ++j) {
      String modifier = internal.get(j);

      // The Drippy Forest has "drippy trees" and "drippy
      // bats" (at least). "drippy" appears in the ocrs
      // array, but Manuel has drippy as part of the name.
      // Ignore "drippy" as an OCRS modifier
      if ("drippy".equals(modifier)) {
        continue;
      }

      if (MonsterData.extraModifiers.contains(modifier)) {
        MonsterData.lastRandomModifiers.add(modifier);
        continue;
      }

      String remove = MonsterData.crazySummerModifiers.get(modifier);

      if (remove == null) {
        RequestLogger.printLine("Unrecognized monster modifier: " + modifier);
        MonsterData.lastRandomModifiers.add(modifier);
        continue;
      }

      if (remove.equals("1337")) {
        leet = true;
      }

      MonsterData.lastRandomModifiers.add(remove);

      remove += (j == count) ? " " : ", ";

      monsterName = StringUtilities.singleStringDelete(monsterName, remove);
    }

    if (leet) {
      // We have an "encounterKey" which has been munged by
      // StringUtilities.getEntityEncode().  1337 monsters
      // don't have character entities, so, decode it again.
      String decoded = StringUtilities.getEntityDecode(monsterName);
      monsterName = MonsterDatabase.translateLeetMonsterName(decoded);
    }

    return trimmed + monsterName;
  }

  private static String handleIntergnat(String monsterName) {
    if (KoLCharacter.getFamiliar().getId() != FamiliarPool.INTERGNAT) {
      return monsterName;
    }
    if (monsterName.contains(" WITH BACON!!!")) {
      MonsterData.lastRandomModifiers.add("bacon");
      return StringUtilities.globalStringDelete(monsterName, " WITH BACON!!!");
    }
    if (monsterName.contains("ELDRITCH HORROR ")) {
      MonsterData.lastRandomModifiers.add("eldritch");
      return StringUtilities.globalStringDelete(monsterName, "ELDRITCH HORROR ");
    }
    if (monsterName.contains(" NAMED NEIL")) {
      MonsterData.lastRandomModifiers.add("neil");
      return StringUtilities.globalStringDelete(monsterName, " NAMED NEIL");
    }
    if (monsterName.contains(" WITH SCIENCE!")) {
      MonsterData.lastRandomModifiers.add("science");
      return StringUtilities.globalStringDelete(monsterName, " WITH SCIENCE!");
    }
    if (monsterName.contains(" AND TESLA!")) {
      MonsterData.lastRandomModifiers.add("tesla");
      return StringUtilities.globalStringDelete(monsterName, " AND TESLA!");
    }
    return monsterName;
  }

  private static final String[] dinoTypes = {
    // Dinosaurs
    "archelon",
    "chicken",
    "dilophosaur",
    "flatusaurus",
    "ghostasaurus",
    "kachungasaur",
    "pterodactyl",
    "spikolodon",
    "velociraptor"
  };

  private static final String[] dinoMods = {
    // Modifiers
    "carrion-eating",
    "chilling",
    "cold-blooded",
    "foul-smelling",
    "glass-shelled",
    "high-altitude",
    "hot-blooded",
    "mist-shrouded",
    "primitive",
    "slimy",
    "steamy",
    "supersonic",
    "swamp",
    "sweaty"
  };

  public static final String[] dinoGluttony = {
    "that consumed", "that just ate", "that recently devoured ", "that swallowed the soul of"
  };

  private static String handleDinosaurs(String monsterName) {
    if (!KoLCharacter.inDinocore()) {
      return monsterName;
    }

    for (String modifier : dinoTypes) {
      if (monsterName.contains(modifier)) {
        MonsterData.lastRandomModifiers.add(modifier);
        monsterName = StringUtilities.singleStringDelete(monsterName, modifier);
        break;
      }
    }

    for (String modifier : dinoMods) {
      if (monsterName.contains(modifier)) {
        MonsterData.lastRandomModifiers.add(modifier);
        monsterName = StringUtilities.singleStringDelete(monsterName, modifier);
        break;
      }
    }

    for (String devour : dinoGluttony) {
      if (monsterName.contains(devour)) {
        monsterName = StringUtilities.singleStringDelete(monsterName, devour);
        break;
      }
    }

    return monsterName.trim();
  }

  private static String handleNuclearAutumn(String monsterName) {
    if (!KoLCharacter.inNuclearAutumn()) {
      return monsterName;
    }
    if (monsterName.contains("mutant ")) {
      // This check isn't perfect, since a monster with "mutant " in its name
      // could show up in Nuclear Autumn eventually.  Currently, the only
      // monsters fitting that criteria are from Crimbo 2008, so this should be
      // a safe way to check.
      monsterName = StringUtilities.singleStringDelete(monsterName, "mutant ");
      MonsterData.lastRandomModifiers.add("mutant");
    }

    return monsterName;
  }

  private static final Pattern MASK_PATTERN = Pattern.compile("(.*?) wearing an? (.*?)ask");

  private static String handleMask(String monsterName) {
    if (!KoLCharacter.inDisguise()) {
      return monsterName;
    }
    Matcher matcher = MASK_PATTERN.matcher(monsterName);
    if (matcher.find()) {
      MonsterData.lastMask = matcher.group(2) + "ask";
      MonsterData.lastRandomModifiers.add(MonsterData.lastMask);
      return matcher.group(1);
    }
    return monsterName;
  }

  // a black adder wearing a construction hardhat and a terrycloth turban and a jockey's hat and a
  // sturdy pith helmet and a construction hardhat and an imposing pilgrim's hat
  private static String handleHats(String monsterName) {
    if (!KoLCharacter.inHatTrick()) {
      return monsterName;
    }

    var wearing = monsterName.split(" wearing ", 2);
    if (wearing.length == 1) {
      return monsterName;
    }
    monsterName = wearing[0];
    var hats = wearing[1];

    var and = hats.split(" and ");

    for (var hat : and) {
      if (hat.startsWith("an ")) {
        hat = hat.substring(3);
      }
      if (hat.startsWith("a ")) {
        hat = hat.substring(2);
      }
      MonsterData.lastRandomModifiers.add(hat);
    }

    return monsterName;
  }
}
