package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UntinkerRequest extends GenericRequest {
  private static final GenericRequest AVAILABLE_CHECKER =
      new PlaceRequest("forestvillage", "fv_untinker", true);

  private static boolean canUntinker;
  private static int lastUserId = -1;

  private static final AdventureResult SCREWDRIVER = ItemPool.get(ItemPool.RUSTY_SCREWDRIVER, -1);

  private final int itemId;
  private int iterationsNeeded;
  private AdventureResult item;

  public static final void reset() {
    UntinkerRequest.canUntinker = false;
    UntinkerRequest.lastUserId = -1;
  }

  public UntinkerRequest(final int itemId) {
    this(itemId, Integer.MAX_VALUE);
  }

  public UntinkerRequest(final int itemId, final int itemCount) {
    super("place.php");
    this.addFormField("whichplace", "forestvillage");
    this.addFormField("action", "fv_untinker");
    this.addFormField("preaction", "untinker");
    this.addFormField("whichitem", String.valueOf(itemId));

    this.itemId = itemId;
    this.iterationsNeeded = 1;

    this.item = ItemPool.get(itemId, itemCount);

    if (itemCount == Integer.MAX_VALUE) {
      this.item = this.item.getInstance(this.item.getCount(KoLConstants.inventory));
    }

    if (itemCount > 5 || this.item.getCount(KoLConstants.inventory) == itemCount) {
      this.addFormField("untinkerall", "on");
    } else {
      this.iterationsNeeded = itemCount;
    }
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public void run() {
    // Check to see if the item can be constructed using meat
    // paste, and only execute the request if it is known to be
    // creatable through combination.

    CraftingType mixMethod = ConcoctionDatabase.getMixingMethod(this.itemId);
    if (mixMethod != CraftingType.COMBINE && mixMethod != CraftingType.JEWELRY) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You cannot untinker that item.");
      return;
    }

    // Retrieving item to untinker may need an outfit change
    if (!InventoryManager.checkpointedRetrieveItem(this.item)) {
      return;
    }

    KoLmafia.updateDisplay("Untinkering " + this.item + "...");

    super.run();

    if (!this.responseText.contains("You acquire")) {
      UntinkerRequest.AVAILABLE_CHECKER.run();

      if (!UntinkerRequest.AVAILABLE_CHECKER.responseText.contains("<select")) {
        UntinkerRequest.canUntinker = UntinkerRequest.completeQuest();

        if (!UntinkerRequest.canUntinker) {
          return;
        }

        UntinkerRequest.AVAILABLE_CHECKER.run();
      }

      super.run();
    }

    for (int i = 1; i < this.iterationsNeeded; ++i) {
      super.run();
    }

    KoLmafia.updateDisplay("Successfully untinkered " + this.item);
  }

  public static final void parseResponse(final String location, final String responseText) {
    if (!location.contains("fv_untinker") && !location.contains("screwquest")) {
      return;
    }

    // "Thanks! I'll tell ya, I'm just lost without my screwdriver. Here, lemme mark the Knoll on
    // your map."
    if (responseText.contains("I'm just lost without my screwdriver")
        || responseText.contains("I'll go find your screwdriver for you") // Zombie Slayer
    ) {
      QuestDatabase.setQuestProgress(Quest.UNTINKER, QuestDatabase.STARTED);
    }

    // If the quest is still in progross, no need to check anything else
    if (location.contains("fv_untinker_quest")) {
      return;
    }

    // Visiting the untinker removes screwdriver from inventory.

    if (KoLConstants.inventory.contains(UntinkerRequest.SCREWDRIVER)) {
      ResultProcessor.processResult(UntinkerRequest.SCREWDRIVER);
      QuestDatabase.setQuestProgress(Quest.UNTINKER, QuestDatabase.FINISHED);
    }

    UntinkerRequest.lastUserId = KoLCharacter.getUserId();
    UntinkerRequest.canUntinker =
        responseText.contains("you don't have anything like that")
            || responseText.contains("<select name=whichitem>");

    if (responseText.contains("You acquire")) {
      Matcher matcher = TransferItemRequest.ITEMID_PATTERN.matcher(location);
      if (!matcher.find()) {
        return;
      }

      int itemId = StringUtilities.parseInt(matcher.group(1));
      AdventureResult result = ItemPool.get(itemId, -1);

      if (location.contains("untinkerall=on")) {
        result = result.getInstance(0 - result.getCount(KoLConstants.inventory));
      }

      ResultProcessor.processResult(result);
    }
  }

  public static final boolean canUntinker() {
    if (UntinkerRequest.lastUserId == KoLCharacter.getUserId()) {
      return UntinkerRequest.canUntinker;
    }

    UntinkerRequest.lastUserId = KoLCharacter.getUserId();

    // If the person does not have the accomplishment, visit
    // the untinker to ensure that they get the quest.

    UntinkerRequest.AVAILABLE_CHECKER.run();

    // "I can take apart anything that's put together with meat
    // paste, but you don't have anything like that..."

    UntinkerRequest.canUntinker =
        UntinkerRequest.AVAILABLE_CHECKER.responseText.contains("you don't have anything like that")
            || UntinkerRequest.AVAILABLE_CHECKER.responseText.contains("<select name=whichitem>");

    return UntinkerRequest.canUntinker;
  }

  public static final boolean completeQuest() {
    // If they are in a muscle sign, this is a trivial task;
    // just have them visit Innabox.

    if (KoLCharacter.knollAvailable()) {
      PlaceRequest tinkVisit = new PlaceRequest("forestvillage", "fv_untinker_quest", true);
      tinkVisit.addFormField("preaction", "screwquest");
      tinkVisit.run();
      PlaceRequest knollVisit = new PlaceRequest("knoll_friendly", "dk_innabox");
      knollVisit.run();

      return true;
    }

    if (GenericFrame.instanceExists()) {
      if (!InputFieldUtilities.confirm(
          "KoLmafia thinks you haven't completed the screwdriver quest.  Would you like to have KoLmafia automatically complete it now?")) {
        return false;
      }
    }

    // Okay, so they don't have one yet. Complete the
    // untinkerer's quest automatically.

    PlaceRequest tinkVisit = new PlaceRequest("forestvillage", "fv_untinker_quest", true);
    tinkVisit.addFormField("preaction", "screwquest");
    tinkVisit.run();

    KoLAdventure sideTripLocation =
        AdventureDatabase.getAdventureByURL("adventure.php?snarfblat=354");
    AdventureResult sideTripItem = UntinkerRequest.SCREWDRIVER.getNegation();

    String action = Preferences.getString("battleAction");
    if (action.contains("dictionary")) {
      KoLmafiaCLI.DEFAULT_SHELL.executeCommand("set", "battleAction=attack");
    }

    GoalManager.makeSideTrip(sideTripLocation, sideTripItem);

    if (KoLmafia.refusesContinue()) {
      KoLmafiaCLI.DEFAULT_SHELL.executeCommand("set", "battleAction=" + action);
      return false;
    }

    KoLmafiaCLI.DEFAULT_SHELL.executeCommand("set", "battleAction=" + action);

    // You should now have a screwdriver in your inventory.
    // Go ahead and rerun the untinker request and you will
    // have the needed accomplishment.

    UntinkerRequest.AVAILABLE_CHECKER.run();
    return !UntinkerRequest.AVAILABLE_CHECKER.responseText.contains("Degrassi Knoll");
  }

  private static final String[] UNTINKER_STRINGS = {
    "Here, lemme mark the Knoll on your map.&quot;",
    "I lost it at Degrassi Knoll, you'll recall.&quot;",
    // Zombiecore
    "Sure, no problem.&quot;",
    "Oh right, Degrassi Knoll. Okay, I'll be right back.&quot;"
  };

  public static void decorate(final StringBuffer buffer) {
    String test = "";
    int index = -1;
    for (int i = 0; index == -1 && i < UNTINKER_STRINGS.length; ++i) {
      test = UNTINKER_STRINGS[i];
      index = buffer.indexOf(test);
    }

    if (index == -1) {
      return;
    }

    String link;
    if (KoLCharacter.knollAvailable()) {
      link =
          "<font size=1>[<a href=\"place.php?whichplace=knoll_friendly&action=dk_innabox\">visit Innabox</a>]</font>";
    } else {
      link =
          "<font size=1>[<a href=\"adventure.php?snarfblat=354\">Degrassi Knoll Garage</a>]</font>";
    }

    buffer.insert(index + test.length(), link);
  }

  public static final boolean registerRequest(final String urlString) {
    // Either place=untinker or action=untinker

    if (!urlString.startsWith("place.php") || !urlString.contains("whichplace=forestvillage")) {
      return false;
    }

    String message;
    if (urlString.contains("preaction=screwquest")) {
      message = "Accepting quest to find the Untinker's screwdriver";
    } else if (urlString.contains("action=fv_untinker")) {
      Matcher matcher = TransferItemRequest.ITEMID_PATTERN.matcher(urlString);
      if (!matcher.find()) {
        RequestLogger.printLine("");
        RequestLogger.updateSessionLog();
        message = "Visiting the Untinker";
      } else {
        String name = ItemDatabase.getItemName(StringUtilities.parseInt(matcher.group(1)));
        message = "untinker " + (urlString.contains("untinkerall=on") ? "*" : "1") + " " + name;
      }
    } else {
      return false;
    }

    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
