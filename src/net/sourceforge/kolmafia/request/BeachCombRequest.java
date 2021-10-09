package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.BeachManager;
import net.sourceforge.kolmafia.session.BeachManager.BeachHead;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BeachCombRequest extends GenericRequest {
  public enum BeachCombCommand {
    VISIT(0),
    WANDER(1),
    RANDOM(2),
    HEAD(3),
    COMB(4),
    EXIT(5),
    COMMON(6);

    private final int option;

    BeachCombCommand(int option) {
      this.option = option;
    }

    public int option() {
      return this.option;
    }
  }

  private static BeachCombCommand optionToCommand(final int option) {
    switch (option) {
      case 1:
        return BeachCombCommand.WANDER;
      case 2:
        return BeachCombCommand.RANDOM;
      case 3:
        return BeachCombCommand.HEAD;
      case 4:
        return BeachCombCommand.COMB;
      case 5:
        return BeachCombCommand.EXIT;
      case 6:
        return BeachCombCommand.COMMON;
    }
    return BeachCombCommand.VISIT;
  }

  public static BeachCombCommand extractCommandFromURL(final String urlString) {
    return BeachCombRequest.optionToCommand(ChoiceManager.extractOptionFromURL(urlString));
  }

  public static class Coords {
    private static final Pattern COORDS_PATTERN = Pattern.compile("coords=((\\d+)%2C(\\d+))");
    public final int beach;
    public final int row;
    public final int col;

    public Coords(int beach, int row, int col) {
      this.beach = beach;
      this.row = row;
      this.col = col;
    }

    public Coords(String coords) {
      Matcher matcher = COORDS_PATTERN.matcher(coords);
      if (matcher.find()) {
        this.row = StringUtilities.parseInt(matcher.group(2));
        int rest = StringUtilities.parseInt(matcher.group(3));
        int mod = (rest % 10);
        this.beach = (rest / 10) + (mod == 0 ? 0 : 1);
        this.col = (mod == 0) ? 0 : (10 - mod);
      } else {
        this.beach = 0;
        this.row = 0;
        this.col = 0;
      }
    }

    @Override
    public String toString() {
      return this.row + "," + ((this.beach * 10) - this.col);
    }
  }

  private static Coords extractCoordsFromURL(final String urlString) {
    return new Coords(urlString);
  }

  private static final Pattern URL_BUFF_PATTERN = Pattern.compile("buff=(\\d+)");

  private static BeachHead extractBeachHeadFromURL(final String urlString) {
    Matcher matcher = BeachCombRequest.URL_BUFF_PATTERN.matcher(urlString);
    return matcher.find()
        ? BeachManager.idToBeachHead.get(StringUtilities.parseInt(matcher.group(1)))
        : null;
  }

  private static final Pattern URL_MINUTES_PATTERN = Pattern.compile("minutes=(\\d+)");

  private static int extractMinutesFromURL(final String urlString) {
    Matcher matcher = BeachCombRequest.URL_MINUTES_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  // Expected sequence of requests:
  //
  //	VISIT, HEAD, EXIT
  //	VISIT, COMMON, EXIT
  //	VISIT, RANDOM, COMB, EXIT
  //	VISIT, WANDER, COMB, EXIT
  //
  // Everything between VISIT and EXIT can be mixed and matched multiple
  // times, as desired, which is why VISIT and EXIT are independently
  // available.

  // Everything about the Beach Comb is handled in this choice
  public static final int WHICHCHOICE = 1388;

  // The following properties are maintained by BeachManager and ChoiceManager
  //
  // beachHeadsUnlocked
  //
  // Beach heads (1, 2, ... 11) for which you have unlocked a shortcut
  // This is a Set<Integer>
  //
  // _beachHeadsUsed
  //
  // Beach heads (1, 2, ... 11) which you have already combed today
  // This is a Set<Integer>
  //
  // Use this to retrieve: Set<Integer> getBeachHeadPreference( String property )
  //
  // _beachCombing
  //
  //  Boolean: true if We are actively combing a segment of beach: we
  //  have visited it (via RANDOM or WANDER) and it is ready to COMB
  //
  // _beachMinutes
  //
  // Integer: The beach number you last visited. It may or may not be active,
  // depending on _beachCombing
  //
  // _beachLayout
  //
  // The layout of the beach you last visited. It may or may not be
  // active, depending on _beachCombing
  //
  // Use this to retrieve: Map<Integer, String> getBeachLayout()
  //
  // _freeBeachWalksUsed
  //
  // Integer: how many (0-11) of your free beach walks you have used
  // today. You require an available turn to comb the beach, even it the
  // visit will be free, but if you have no turns left when you comb a
  // beach, KoL will exit the choice. Otherwise, you can continue to
  // issue commands

  private BeachCombCommand command = null;
  private BeachHead head = null;
  private int beach = -1;
  private int row = -1;
  private int col = -1;

  public BeachCombRequest() {
    // Public choice: VISIT
    super("main.php");
    this.addFormField("comb", "1");
    // Must use GET
    this.constructURLString(this.getFullURLString(), false);
    this.command = BeachCombCommand.VISIT;
    // After this request is run, we'll either be sitting in choice
    // 1388 at top level - or not, if we are out of turns.
  }

  public BeachCombRequest(final BeachCombCommand command) {
    // Public choices: RANDOM, COMMON, EXIT (no additional arguments)
    super("choice.php");
    this.addFormField("whichchoice", String.valueOf(WHICHCHOICE));
    this.addFormField("option", String.valueOf(command.option()));
    this.command = command;
    // After this request is run:
    //     COMMON leaves us sitting in choice 1388 at top level
    //     RANDOM leaves us sitting in choice 1388 at the beach, like WANDER
    //     EXIT takes us out of choice 1388

  }

  public BeachCombRequest(final BeachHead head) {
    // Public choices: HEAD (which requires number, location, etc.)
    this(BeachCombCommand.HEAD);
    this.head = head;
    // We'll decide at run time whether and how to visit the beach
    // head: via shortcut or via wander + comb
    // After this request is run, we'll either be sitting in choice
    // 1388 at top level - or not, if we are out of turns.
  }

  public BeachCombRequest(final int beach) {
    // Private choice: WANDER (which requires additional arguments)
    this(BeachCombCommand.WANDER);
    this.beach = beach;
    // After this request is run, we will be sitting in choice 1388
    // on the requested segment of the beach
  }

  public BeachCombRequest(final int row, final int col) {
    // Private choice: COMB (which requires additional arguments)
    this(BeachCombCommand.COMB);
    this.row = row;
    this.col = col;
    // After this request is run, we'll either be sitting in choice
    // 1388 at top level - or not, if we are out of turns.
  }

  public static boolean visitIfNecessary() {
    if (GenericRequest.abortIfInFight(true)) {
      return false;
    }

    if (ChoiceManager.handlingChoice) {
      if (ChoiceManager.lastChoice == WHICHCHOICE) {
        return false;
      }
      if (!ChoiceManager.canWalkAway()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You are currently in a choice.");
        return false;
      }
    }

    KoLmafia.forceContinue();
    (new BeachCombRequest()).run();
    return ChoiceManager.handlingChoice;
  }

  public static void exitIfNecessary(boolean necessary) {
    if (necessary && ChoiceManager.handlingChoice && ChoiceManager.lastChoice == WHICHCHOICE) {
      KoLmafia.forceContinue();
      (new BeachCombRequest(BeachCombCommand.EXIT)).run();
    }
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    // Can't do this if we are in a fight
    if (GenericRequest.abortIfInFight(true)) {
      return;
    }

    // We can do this if we are in the Beach Comb choice
    boolean usingComb = false;
    if (ChoiceManager.handlingChoice) {
      if (ChoiceManager.lastChoice != WHICHCHOICE) {
        if (!ChoiceManager.canWalkAway()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "You are currently in a choice.");
          return;
        }
        // We are in a choice that we can walk away from. Fall through.
      } else {
        // We are in the Beach Comb choice. Carry on from here.
        usingComb = true;
      }
    }

    if (!usingComb) {
      // Ensure we have access to a Beach Comb or dirftwood beach comb
      if (InventoryManager.getAccessibleCount(ItemPool.BEACH_COMB) > 0) {
        if (!KoLCharacter.hasEquipped(ItemPool.BEACH_COMB)) {
          InventoryManager.retrieveItem(ItemPool.BEACH_COMB, true, false);
        }
      } else if (InventoryManager.getAccessibleCount(ItemPool.DRIFTWOOD_BEACH_COMB) <= 0) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You need either a Beach Comb or a driftwood beach comb");
        return;
      }
    }

    if (KoLCharacter.getAdventuresLeft() <= 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You don't have time to comb the beach.");
      return;
    }

    if (this.command == BeachCombCommand.VISIT) {
      // If we are not using the comb, tell KoL to use it
      if (!usingComb) {
        super.run();
      }
      return;
    }

    if (this.command == BeachCombCommand.EXIT) {
      // If we are using the comb, tell KoL to exit
      if (usingComb) {
        super.run();
      }
      // Otherwise, it's OK; KoL may have exited from the
      // choice when you ran out of turns.
      return;
    }

    // ARGUMENT CHECKING

    if (this.command == BeachCombCommand.COMMON) {
      // This requires at least 10 free wanders
      if (Preferences.getInteger("_freeBeachWalksUsed") > 1) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You must have 10 free wanders available to claim all common items.");
        return;
      }
    }

    if (this.command == BeachCombCommand.WANDER) {
      if (this.beach < 1 || this.beach > 10000) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You must wander from 1 to 10000 minutes.");
        return;
      }
    }

    if (this.command == BeachCombCommand.COMB) {
      // We must be visiting a beach segment
      if (!Preferences.getBoolean("_beachCombing")) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You must WANDER or visit a RANDOM beach before you can comb.");
        return;
      }

      // Get current layout of the beach
      Map<Integer, String> layout = BeachManager.getBeachLayout();

      // Validate row
      int minRow = 10;
      int maxRow = 1;

      for (int row : layout.keySet()) {
        if (row < minRow) {
          minRow = row;
        }
        if (row > maxRow) {
          maxRow = row;
        }
      }

      if (this.row < minRow || this.row > maxRow) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Only rows " + minRow + "-" + maxRow + " are available today");
        return;
      }
      if (this.col < 0 || this.beach > 9) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You must visit column 0-9.");
        return;
      }
    }

    if (this.command == BeachCombCommand.HEAD) {
      Set<Integer> visited = BeachManager.getBeachHeadPreference("_beachHeadsUsed");
      if (visited.contains(this.head.id)) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You've already combed beach head #" + this.head.id);
        return;
      }
    }

    // All these commands require that you VISIT the comb first
    if (!usingComb) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have not VISITed the Beach Comb yet.");
      return;
    }

    // Commands with no additional arguments:

    if (this.command == BeachCombCommand.COMMON || this.command == BeachCombCommand.RANDOM) {
      super.run();
      return;
    }

    if (this.command == BeachCombCommand.WANDER) {
      this.addFormField("minutes", String.valueOf(this.beach));
      super.run();
      return;
    }

    if (this.command == BeachCombCommand.COMB) {
      Coords coords = new Coords(Preferences.getInteger("_beachMinutes"), row, col);
      this.addFormField("coords", coords.toString());
      super.run();
      return;
    }

    if (this.command == BeachCombCommand.HEAD) {
      int id = this.head.id;
      Set<Integer> unlocked = BeachManager.getBeachHeadPreference("beachHeadsUnlocked");

      if (unlocked.contains(id)) {
        this.removeFormField("minutes");
        this.removeFormField("coords");
        this.addFormField("option", String.valueOf(BeachCombCommand.HEAD.option()));
        this.addFormField("buff", String.valueOf(id));
        super.run();
      } else {
        this.removeFormField("buff");

        // Wander
        this.removeFormField("coords");
        this.addFormField("option", String.valueOf(BeachCombCommand.WANDER.option()));
        this.addFormField("minutes", String.valueOf(this.head.beach));
        super.run();

        // Comb
        this.removeFormField("minutes");
        this.addFormField("option", String.valueOf(BeachCombCommand.COMB.option()));
        this.addFormField("coords", this.head.coords);
        super.run();
      }
    }
  }

  @Override
  public void processResults() {}

  @Override
  public int getAdventuresUsed() {
    switch (this.command) {
      case VISIT:
      case EXIT:
      case COMMON:
        return 0;
      case WANDER:
      case RANDOM:
      case HEAD:
      case COMB:
        return Preferences.getInteger("_freeBeachWalksUsed") < 11 ? 0 : 1;
    }
    return 0;
  }

  public static final boolean containsEncounter(final String urlString) {
    // Only wand to log "Encounter:" in interesting cases
    BeachCombCommand command = BeachCombRequest.extractCommandFromURL(urlString);
    switch (command) {
      case VISIT:
      case EXIT:
        // Picking up or putting down the comb are uninteresting
        return false;
      case COMMON:
      case HEAD:
      case COMB:
        // These are the result of combing a square. Not interesting encounter
        return false;
      case WANDER:
      case RANDOM:
        // These bring us to a particular section of beach. The
        // encounter tells us which section.
        return true;
    }
    return false;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=1388")) {
      return false;
    }

    String message = null;
    boolean turns = false;

    BeachCombCommand command = BeachCombRequest.extractCommandFromURL(urlString);
    switch (command) {
      case VISIT:
        // Using Beach Comb
        return true;
      case EXIT:
        // Putting Down Beach Comb
        // Took choice 1388/5: (secret choice)
        // choice.php?whichchoice=1388&option=5&pwd
        return true;
      case COMMON:
        // Not really, but log turn # anyway
        turns = true;
        message = "Collecting common items";
        break;
      case WANDER:
        {
          int minutes = BeachCombRequest.extractMinutesFromURL(urlString);
          turns = true;
          message = "Wandering " + minutes + " minutes down the beach";
          break;
        }
      case RANDOM:
        turns = true;
        message = "Wandering to a random section of the beach";
        break;
      case HEAD:
        {
          turns = true;
          BeachHead head = BeachCombRequest.extractBeachHeadFromURL(urlString);
          message = "Combing (" + (head == null ? "unknown" : head.desc) + ") Beach Head";
          break;
        }
      case COMB:
        {
          Coords coords = BeachCombRequest.extractCoordsFromURL(urlString);
          // Internally indexed from 0-9, but display as 1-10
          message =
              "Combing square "
                  + coords.row
                  + ","
                  + (coords.col + 1)
                  + " ("
                  + coords.beach
                  + " minutes down the beach)";
          break;
        }
    }

    if (turns) {
      message = "[" + KoLAdventure.getAdventureCount() + "] " + message;
      RequestLogger.printLine();
      RequestLogger.updateSessionLog();
    }

    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
