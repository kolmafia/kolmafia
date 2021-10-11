package net.sourceforge.kolmafia.session;

import java.io.File;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class SpadingManager {
  public enum SpadingEvent {
    COMBAT_ROUND,
    CHOICE_VISIT,
    CHOICE,
    CONSUME_DRINK,
    CONSUME_EAT,
    CONSUME_SPLEEN,
    CONSUME_USE,
    CONSUME_MULTIPLE,
    CONSUME_REUSABLE,
    CONSUME_MESSAGE,
    DESC_ITEM,
    MEAT_DROP,
    PVP,
    PLACE,
    ;

    public static SpadingEvent fromKoLConstant(final int constant) {
      switch (constant) {
        case KoLConstants.CONSUME_EAT:
          return SpadingEvent.CONSUME_EAT;
        case KoLConstants.CONSUME_DRINK:
          return SpadingEvent.CONSUME_DRINK;
        case KoLConstants.CONSUME_SPLEEN:
          return SpadingEvent.CONSUME_SPLEEN;
        case KoLConstants.CONSUME_USE:
          return SpadingEvent.CONSUME_USE;
        case KoLConstants.CONSUME_MULTIPLE:
          return SpadingEvent.CONSUME_MULTIPLE;
        case KoLConstants.INFINITE_USES:
          return SpadingEvent.CONSUME_REUSABLE;
        case KoLConstants.MESSAGE_DISPLAY:
          return SpadingEvent.CONSUME_MESSAGE;
        default:
          return null;
      }
    }
  }

  private static String getScriptName() {
    String scriptName = Preferences.getString("spadingScript").trim();
    if (scriptName.length() == 0) {
      return null;
    }

    return scriptName;
  }

  public static boolean hasSpadingScript() {
    return SpadingManager.getScriptName() != null;
  }

  public static boolean processCombatRound(final String monsterName, final String responseText) {
    return SpadingManager.invokeSpadingScript(SpadingEvent.COMBAT_ROUND, monsterName, responseText);
  }

  public static boolean processMeatDrop(final String meatDrop) {
    return SpadingManager.invokeSpadingScript(SpadingEvent.MEAT_DROP, "", meatDrop);
  }

  public static boolean processChoiceVisit(final int choiceNumber, final String responseText) {
    return SpadingManager.invokeSpadingScript(
        SpadingEvent.CHOICE_VISIT, Integer.toString(choiceNumber), responseText);
  }

  public static boolean processChoice(final String url, final String responseText) {
    return SpadingManager.invokeSpadingScript(SpadingEvent.CHOICE, url, responseText);
  }

  public static boolean processConsume(
      final int consumptionType, final String itemName, final String responseText) {
    SpadingEvent event = SpadingEvent.fromKoLConstant(consumptionType);

    if (event == null) {
      return false;
    }

    return SpadingManager.invokeSpadingScript(event, itemName, responseText);
  }

  public static boolean processConsumeItem(final AdventureResult item, final String responseText) {
    if (item == null) {
      return false;
    }

    int consumptionType = UseItemRequest.getConsumptionType(item);

    return SpadingManager.processConsume(
        consumptionType, item.getDisambiguatedName(), responseText);
  }

  public static boolean processPeeVPee(final String location, final String responseText) {
    return SpadingManager.invokeSpadingScript(SpadingEvent.PVP, location, responseText);
  }

  public static boolean processPlace(final String url, final String responseText) {
    return SpadingManager.invokeSpadingScript(SpadingEvent.PLACE, url, responseText);
  }

  public static boolean processDescItem(final AdventureResult item, final String responseText) {
    if (item == null) {
      return false;
    }

    return SpadingManager.invokeSpadingScript(
        SpadingEvent.DESC_ITEM, item.getDisambiguatedName(), responseText);
  }

  private static boolean invokeSpadingScript(
      final SpadingEvent event, final String meta, final String responseText) {
    String scriptName = SpadingManager.getScriptName();

    if (responseText == null || scriptName == null) {
      return false;
    }

    List<File> scriptFiles = KoLmafiaCLI.findScriptFile(scriptName);
    ScriptRuntime interpreter = KoLmafiaASH.getInterpreter(scriptFiles);

    if (interpreter == null) {
      return false;
    }

    File scriptFile = scriptFiles.get(0);

    Object[] parameters = new Object[3];
    parameters[0] = event.toString();
    parameters[1] = meta;
    parameters[2] = responseText;

    KoLmafiaASH.logScriptExecution("Starting spading script: ", scriptFile.getName(), interpreter);

    // Since we are automating, let the script execute without interruption
    KoLmafia.forceContinue();

    interpreter.execute("main", parameters);

    KoLmafiaASH.logScriptExecution("Finished spading script: ", scriptFile.getName(), interpreter);

    return true;
  }
}
