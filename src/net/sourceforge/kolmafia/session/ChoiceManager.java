package net.sourceforge.kolmafia.session;

import java.io.File;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.ChoiceAdventures.ChoiceAdventure;
import net.sourceforge.kolmafia.session.ChoiceAdventures.ChoiceSpoiler;
import net.sourceforge.kolmafia.session.ChoiceAdventures.Option;
import net.sourceforge.kolmafia.session.ChoiceAdventures.Spoilers;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.VillainLairDecorator;

public abstract class ChoiceManager {

  public static boolean handlingChoice = false;
  public static int lastChoice = 0;
  public static int lastDecision = 0;
  public static String lastResponseText = "";
  public static String lastDecoratedResponseText = "";

  public static void reset() {
    ChoiceManager.lastChoice = 0;
    ChoiceManager.lastDecision = 0;
    ChoiceManager.lastResponseText = null;
    ChoiceManager.lastDecoratedResponseText = null;
  }

  private static int skillUses = 0;

  public static final void setSkillUses(int uses) {
    // Used for casting skills that lead to a choice adventure
    ChoiceManager.skillUses = uses;
  }

  private static boolean canWalkAway;

  public static boolean canWalkAway() {
    return ChoiceManager.canWalkAway;
  }

  private enum PostChoiceAction {
    NONE,
    INITIALIZE,
    ASCEND
  }

  private static PostChoiceAction action = PostChoiceAction.NONE;

  public static int currentChoice() {
    return ChoiceManager.handlingChoice ? ChoiceManager.lastChoice : 0;
  }

  public static final int getLastChoice() {
    return ChoiceManager.lastChoice;
  }

  public static final int getLastDecision() {
    return ChoiceManager.lastDecision;
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

  /*
   * Here is how we automate a choice chain.
   *
   * This is invoked by:
   *
   * - GenericRequest when redirected to choice.php
   * - UseItemRequest when redirected to choice.php
   * - CampgroundRequest when automating mushroom garden
   *
   * And by user actions:
   *
   * - RelayRequest when the user hits the "auto" button on a choice page.
   * - The "choice" command from the gCLI
   * - The run_choice() function of ASH
   */

  public static final GenericRequest CHOICE_HANDLER =
      new GenericRequest("choice.php") {
        @Override
        protected boolean shouldFollowRedirect() {
          return false;
        }
      };

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
      int choice = ChoiceUtilities.extractChoice(responseText);
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
            && choice != ChoiceUtilities.extractChoice(ChoiceManager.lastResponseText)) {
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

    decision = ChoiceManager.pickGoalChoice(choice, decision);

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

    decision = ChoiceManager.pickGoalChoice(choice, decision);

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

  private static final AdventureResult MAIDEN_EFFECT = EffectPool.get(EffectPool.DREAMS_AND_LIGHTS);

  public static String specialChoiceDecision1(
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
            return KoLConstants.activeEffects.contains(MAIDEN_EFFECT)
                ? String.valueOf(KoLConstants.RNG.nextInt(2) + 1)
                : "3";
          case 4:
            return KoLConstants.activeEffects.contains(MAIDEN_EFFECT) ? "1" : "3";
          case 5:
            return KoLConstants.activeEffects.contains(MAIDEN_EFFECT) ? "2" : "3";
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
          final String answer;
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
            default:
              // Only standard classes can join the guild, so we
              // should not fail. But, if we do, cope.
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

        final String answer;
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
          default:
            // Only standard classes can join the guild, so we
            // should not fail. But, if we do, cope.
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
        //  Lights Out in the Storage Room
        if (automation == 1 && responseText.contains("Look Out the Window")) {
          return "3";
        }
        return "1";
      case 891:
        //  Lights Out in the Laundry Room
        if (automation == 1 && responseText.contains("Check a Pile of Stained Sheets")) {
          return "3";
        }
        return "1";
      case 892:
        //  Lights Out in the Bathroom
        if (automation == 1 && responseText.contains("Inspect the Bathtub")) {
          return "3";
        }
        return "1";
      case 893:
        //  Lights Out in the Kitchen
        if (automation == 1 && responseText.contains("Make a Snack")) {
          return "4";
        }
        return "1";
      case 894:
        //  Lights Out in the Library
        if (automation == 1 && responseText.contains("Go to the Children's Section")) {
          return "2";
        }
        return "1";
      case 895:
        //  Lights Out in the Ballroom
        if (automation == 1 && responseText.contains("Dance with Yourself")) {
          return "2";
        }
        return "1";
      case 896:
        //  Lights Out in the Gallery
        if (automation == 1
            && responseText.contains("Check out the Tormented Damned Souls Painting")) {
          return "4";
        }
        return "1";
      case 897:
        //  Lights Out in the Bedroom
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
        //  Lights Out in the Nursery
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
        //  Lights Out in the Conservatory
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
        //  Lights Out in the Billiards Room
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
        //  Lights Out in the Wine Cellar
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
        //  Lights Out in the Boiler Room
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
        //  Lights Out in the Laboratory
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

  private static final AdventureResult PAPAYA = ItemPool.get(ItemPool.PAPAYA, 1);
  private static final AdventureResult MODEL_AIRSHIP = ItemPool.get(ItemPool.MODEL_AIRSHIP, 1);
  private static final AdventureResult MCCLUSKY_FILE = ItemPool.get(ItemPool.MCCLUSKY_FILE, 1);
  private static final AdventureResult BINDER_CLIP = ItemPool.get(ItemPool.BINDER_CLIP, 1);
  private static final AdventureResult MCCLUSKY_FILE_PAGE5 =
      ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE5, 1);
  private static final AdventureResult STONE_TRIANGLE = ItemPool.get(ItemPool.STONE_TRIANGLE, 1);

  private static final AdventureResult CURSE3_EFFECT = EffectPool.get(EffectPool.THRICE_CURSED);
  private static final AdventureResult JOCK_EFFECT =
      EffectPool.get(EffectPool.JAMMING_WITH_THE_JOCKS);
  private static final AdventureResult NERD_EFFECT = EffectPool.get(EffectPool.NERD_IS_THE_WORD);
  private static final AdventureResult GREASER_EFFECT = EffectPool.get(EffectPool.GREASER_LIGHTNIN);

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

  public static String specialChoiceDecision2(
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
              : KoLConstants.activeEffects.contains(CURSE3_EFFECT) ? "1" : "2");
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
            boolean hasMcCluskyFile = InventoryManager.getCount(MCCLUSKY_FILE) > 0;
            boolean hasMcCluskyFilePage5 = InventoryManager.getCount(MCCLUSKY_FILE_PAGE5) > 0;
            boolean hasBinderClip = InventoryManager.getCount(BINDER_CLIP) > 0;
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
          int stoneTriangles = InventoryManager.getCount(STONE_TRIANGLE);
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

      case 1461:
        // Hello Knob My Old Friend
        // If you can "Grab the Cheer Core!", do it.
        if (responseText.contains("Grab the Cheer Core!")) {
          return "5";
        }
        return decision;
    }
    return decision;
  }

  private static String pickGoalChoice(final int choice, final String decision) {
    // If the user wants manual control, let 'em have it.
    if (decision.equals("0")) {
      return decision;
    }

    // Find the options for the choice we've encountered

    Option[] options = null;

    // See if this choice is controlled by user option
    if (options == null) {
      ChoiceAdventure choiceAdventure = ChoiceAdventures.choiceToChoiceAdventure.get(choice);
      if (choiceAdventure != null) {
        options = choiceAdventure.getOptions();
      }
    }

    // Nope. See if we know this choice
    if (options == null) {
      ChoiceSpoiler choiceSpoiler = ChoiceAdventures.choiceToChoiceSpoiler.get(choice);
      if (choiceSpoiler != null) {
        options = choiceSpoiler.getOptions();
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
      Option opt = options[i];
      AdventureResult item[] = opt.getItems();
      if (item.length == 0) {
        continue;
      }

      // Iterate?
      if (GoalManager.hasGoal(item[0])) {
        return String.valueOf(opt.getDecision(i + 1));
      }

      items = true;
    }

    // If none of the options have an associated item, nothing to do.
    if (!items) {
      return decision;
    }

    // Find the spoiler corresponding to the chosen decision
    Option chosen = ChoiceAdventures.findOption(options, StringUtilities.parseInt(decision));

    // If the player doesn't want to "complete the outfit", nothing to do
    if (chosen == null || !chosen.toString().equals("complete the outfit")) {
      return decision;
    }

    // Pick an item that the player doesn't have yet
    for (int i = 0; i < options.length; ++i) {
      Option opt = options[i];
      AdventureResult item[] = opt.getItems();
      if (item.length == 0) {
        continue;
      }

      // Should iterate
      if (!InventoryManager.hasItem(item[0])) {
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

  public static String choiceDescription(final int choice, final int decision) {
    // If we have spoilers for this choice, use that
    Spoilers spoilers = ChoiceAdventures.choiceSpoilers(choice, null);
    if (spoilers != null) {
      Option spoiler = ChoiceAdventures.choiceSpoiler(choice, decision, spoilers.getOptions());
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

  /*
   * Here are the methods called by GenericRequest at various points while
   * processing a request.
   *
   * We are "handling a choice" from the time we submit a request to choice.php
   * until we have processed the response.
   *
   * Since choices can lead to other choices (a "choice chain") we stay in that
   * state until the final response is received - although the "current" choice
   * will change along the way.
   *
   * This would be a good place to explain which of these methods is called
   * where and what the intended purpose is.
   */

  public static final void preChoice(final GenericRequest request) {
    FightRequest.choiceFollowsFight = false;
    ChoiceManager.handlingChoice = true;
    FightRequest.currentRound = 0;

    String choice = request.getFormField("whichchoice");
    String option = request.getFormField("option");

    if (choice == null || option == null) {
      // Visiting a choice page but not yet making a decision
      ChoiceManager.reset();
    }

    // We are about to take a choice option
    ChoiceManager.lastChoice = StringUtilities.parseInt(choice);
    ChoiceManager.lastDecision = StringUtilities.parseInt(option);

    ChoiceControl.preChoice(request);
  }

  public static void postChoice0(final String urlString, final GenericRequest request) {
    if (ChoiceManager.nonInterruptingRequest(urlString, request)) {
      return;
    }

    // If this is not actually a choice page, nothing to do here.
    if (!urlString.startsWith("choice.php")) {
      return;
    }

    String text = request.responseText;
    int choice = lastChoice == 0 ? ChoiceUtilities.extractChoice(text) : lastChoice;

    if (choice == 0) {
      // choice.php did not offer us any choices.
      // This would be a bug in KoL itself.
      return;
    }

    ChoiceControl.postChoice0(choice, urlString, request);
  }

  /**
   * Determine if a request to choice.php showed that we weren't actually in a choice adventure
   *
   * @param request Completed request to check
   * @return If the player was trying to respond to a choice but was not in a choice adventure
   */
  public static boolean bogusChoice(final String urlString, final GenericRequest request) {
    if (!ChoiceManager.handlingChoice
        || !urlString.startsWith("choice.php")
        || request.responseText == null) {
      return false;
    }

    if (request.responseText.contains("Whoops!  You're not actually in a choice adventure.")) {
      // Allow a script to simply attempt to visit choice.php.
      if (!urlString.equals("choice.php")) {
        if (Preferences.getBoolean("abortOnChoiceWhenNotInChoice")) {
          KoLmafia.updateDisplay(
              MafiaState.ABORT, "Whoops! You're not actually in a choice adventure");
        } else {
          KoLmafia.updateDisplay(
              MafiaState.ERROR,
              "Script submitted " + urlString + " when KoL was not in a choice adventure");
        }
      }
      ChoiceManager.handlingChoice = false;
      return true;
    }

    return false;
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

    // If this is not actually a choice page, we were redirected.
    // Do not save this responseText

    String text = request.responseText;
    if (urlString.startsWith("choice.php")) {
      ChoiceManager.lastResponseText = text;
    }

    ChoiceControl.postChoice1(urlString, request);

    // Certain choices cost meat or items when selected
    ChoiceAdventures.payCost(ChoiceManager.lastChoice, ChoiceManager.lastDecision);
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

    if (urlString.startsWith("choice.php") && text.contains("charpane.php")) {
      // Since a charpane refresh was requested, a turn might have been spent
      AdventureSpentDatabase.setNoncombatEncountered(true);
    }

    if (ChoiceManager.lastChoice == 0 || ChoiceManager.lastDecision == 0) {
      // This was a visit
      return;
    }

    ChoiceControl.postChoice2(urlString, request);

    SpadingManager.processChoice(urlString, text);

    if (ChoiceManager.handlingChoice) {
      ChoiceManager.visitChoice(request);
      return;
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

  public static void visitChoice(final GenericRequest request) {
    String text = request.responseText;
    ChoiceManager.lastChoice = ChoiceUtilities.extractChoice(text);

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

    ChoiceControl.visitChoice(request);

    // Do this after special classes (like WumpusManager) have a
    // chance to update state in their visitChoice methods.
    ChoiceManager.lastDecoratedResponseText =
        RequestEditorKit.getFeatureRichHTML(request.getURLString(), text);
  }

  private static void setCanWalkAway(final int choice) {
    ChoiceManager.canWalkAway = ChoiceControl.canWalkFromChoice(choice);
  }

  public static boolean canWalkFromChoice(int choice) {
    return ChoiceControl.canWalkFromChoice(choice);
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

    return ChoiceControl.registerRequest(urlString);
  }

  public static final void registerDeferredChoice(final int choice, final String encounter) {
    // If we couldn't find an encounter, do nothing
    if (encounter == null) {
      return;
    }

    ChoiceControl.registerDeferredChoice(choice, encounter);
  }
}
