package net.sourceforge.kolmafia.combat;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList.MPRestoreItem;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.DiscoCombatHelper;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Macrofier {
  private static String macroOverride = null;
  private static ScriptRuntime macroInterpreter = null;
  private static BaseFunction macroFunction = null;
  private static Scriptable macroScope = null;
  private static Scriptable macroThisArg = null;

  private static final Pattern ALLCALLS_PATTERN = Pattern.compile("call (\\w+)");
  private static final Pattern ALLSUBS_PATTERN = Pattern.compile("sub (\\w+)([\\s;\\n]+endsub)?");

  private Macrofier() {}

  public static void resetMacroOverride() {
    Macrofier.macroOverride = null;
    Macrofier.macroInterpreter = null;
    Macrofier.macroFunction = null;
    Macrofier.macroScope = null;
    Macrofier.macroThisArg = null;
  }

  public static void setMacroOverride(String macroOverride, ScriptRuntime interpreter) {
    if (macroOverride == null || macroOverride.length() == 0) {
      Macrofier.macroOverride = null;
      Macrofier.macroInterpreter = null;
    } else if (macroOverride.indexOf(';') != -1) {
      Macrofier.macroOverride = macroOverride;
      Macrofier.macroInterpreter = null;
    } else {
      Macrofier.macroOverride = macroOverride;
      Macrofier.macroInterpreter = interpreter;
    }
  }

  public static void setJavaScriptMacroOverride(
      BaseFunction function, Scriptable scope, Scriptable thisArg) {
    Macrofier.macroFunction = function;
    Macrofier.macroScope = scope;
    Macrofier.macroThisArg = thisArg;
  }

  public static boolean usingCombatFilter() {
    return Macrofier.macroInterpreter != null;
  }

  private static boolean isSimpleAction(String action) {
    if (action.startsWith("consult")) {
      return false;
    }

    if (action.startsWith("delevel")) {
      return false;
    }

    if (action.startsWith("twiddle")) {
      return false;
    }

    return true;
  }

  public static String macrofy() {
    boolean debug = Preferences.getBoolean("macroDebug");

    // If there's an override, always use it

    if (Macrofier.macroInterpreter == null
        && Macrofier.macroOverride != null
        && Macrofier.macroOverride.length() > 0) {
      if (debug) {
        RequestLogger.printLine("Using macroOverride");
      }
      return Macrofier.macroOverride;
    }

    // Begin monster-specific macrofication.

    MonsterData monster = MonsterStatusTracker.getLastMonster();
    String monsterName = (monster != null) ? monster.getName() : "";

    if (Macrofier.macroInterpreter != null) {
      Object[] parameters = new Object[3];
      parameters[0] = FightRequest.getRoundIndex();
      parameters[1] = monster;
      parameters[2] = FightRequest.lastResponseText;

      Value returnValue;

      if (Macrofier.macroInterpreter instanceof JavascriptRuntime) {
        // Execute a function from the JavaScript runtime maintaining the scope, thisObj etc
        JavascriptRuntime interpreter = (JavascriptRuntime) Macrofier.macroInterpreter;
        returnValue =
            interpreter.executeFunction(
                macroScope,
                () -> {
                  Context cx = Context.getCurrentContext();
                  return macroFunction.call(cx, macroScope, macroThisArg, parameters);
                });
      } else {
        // Execute a single function in the scope of the
        // currently executing file.  Do not re-execute
        // top-level code in that file.
        returnValue = Macrofier.macroInterpreter.execute(macroOverride, parameters, false);
      }

      if (KoLmafia.refusesContinue()) {
        return "abort";
      }

      if (returnValue == null || returnValue.getType().equals(DataTypes.TYPE_VOID)) {
        String message = "Macro override \"" + macroOverride + "\" returned void.";
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      } else {
        String result = returnValue.toString();
        if (result.length() > 0) {
          if (result.startsWith("\"") && result.charAt(result.length() - 1) == '\"') {
            StringBuffer macro = new StringBuffer();
            macro.append("#macro action\n");
            macro.append(result, 1, result.length() - 1);
            macro.append('\n');

            if (debug) {
              RequestLogger.printLine("Generated macro:");
              Macrofier.indentify(macro.toString(), false);
              RequestLogger.printLine("");
            }

            return macro.toString();
          }

          // FightRequest.combatFilterThatDidNothing is cleared
          // at the beginning of each round. If we are called a
          // second time within a round (because the first action
          // we returned was rejected) it is pointless to return
          // the same action. Return "abort" instead.
          if (result.equals(FightRequest.combatFilterThatDidNothing)) {
            return "abort";
          }

          FightRequest.combatFilterThatDidNothing = result;

          return result;
        }
      }
    }

    StringBuffer macro = new StringBuffer();

    if (monsterName.equals("rampaging adding machine")
        && !KoLConstants.activeEffects.contains(FightRequest.BIRDFORM)
        && !FightRequest.waitingForSpecial) {
      if (debug) {
        RequestLogger.printLine("(unable to macrofy vs. RAM)");
      }

      return null;
    }

    if (monsterName.equals("hulking construct")) {
      // use ATTACK & WALL punchcards
      macro.append("if hascombatitem 3146 && hascombatitem 3155\n");
      if (KoLCharacter.hasSkill("Ambidextrous Funkslinging")) {
        macro.append("  use 3146,3155\n");
      } else {
        macro.append("  use 3146; use 3155\n");
      }
      macro.append("endif\nrunaway; repeat\n");

      if (debug) {
        RequestLogger.printLine("Generated macro:");
        Macrofier.indentify(macro.toString(), false);
        RequestLogger.printLine("");
      }

      return macro.toString();
    }

    float thresh = Preferences.getFloat("autoAbortThreshold");
    if (thresh > 0.0f) {
      macro.append("abort hppercentbelow ");
      macro.append((int) (thresh * 100.0f));
      macro.append('\n');
    }

    Macrofier.macroCommon(macro);

    macro.append("#mafiaheader\n");

    // Load up the "global prefix", if there is one
    if (CombatActionManager.hasGlobalPrefix()) {
      for (int i = 0; i < 10000; ++i) {
        String action = CombatActionManager.getCombatAction("global prefix", i, true);

        if (!Macrofier.isSimpleAction(action)) {
          if (debug) {
            RequestLogger.printLine(
                "(unable to macrofy global prefix due to action: " + action + ")");
          }

          return null;
        }

        Macrofier.macroAction(macro, action, 0);

        if (CombatActionManager.atEndOfStrategy()) {
          break; // continue with actual CCS section
        }
      }
    }

    int macrolen = FightRequest.getMacroPrefixLength();
    int start = Math.max(macrolen, 0);

    for (int i = start; i < 10000; ++i) {
      String action = CombatActionManager.getCombatAction(monsterName, i, true);

      if (!Macrofier.isSimpleAction(action)) {
        if (debug) {
          RequestLogger.printLine("stopping macrofication due to action: " + action);
        }

        if (i == start) {
          return null;
        }

        FightRequest.setMacroPrefixLength(i);

        break;
      }

      int finalRound = 0;

      if (CombatActionManager.atEndOfStrategy()) {
        macro.append("mark mafiafinal\n");
        finalRound = macro.length();
      }

      Macrofier.macroAction(macro, action, finalRound);

      if (finalRound != 0) {
        if (finalRound == macro.length()) {
          // last line of CCS generated no action!
          macro.append("call mafiaround; attack\n");
        }
        macro.append("goto mafiafinal");
        FightRequest.setMacroPrefixLength(0);
        break;
      }
    }

    if (debug) {
      RequestLogger.printLine("Generated macro:");
      Macrofier.indentify(macro.toString(), false);
      RequestLogger.printLine("");
    }

    HashSet<String> allCalls = new HashSet<String>();
    Matcher m = Macrofier.ALLCALLS_PATTERN.matcher(macro);

    while (m.find()) {
      allCalls.add(m.group(1));
    }

    m = Macrofier.ALLSUBS_PATTERN.matcher(macro.toString());

    while (m.find()) {
      String label = m.group(1);
      if (m.group(2) != null || !allCalls.contains(label)) {
        // this sub is useless!
        Matcher del =
            Pattern.compile("call " + label + "\\b|sub " + label + "\\b.*?endsub", Pattern.DOTALL)
                .matcher(macro.toString());

        macro.setLength(0);

        while (del.find()) {
          del.appendReplacement(macro, "");
        }

        del.appendTail(macro);
      }
    }

    if (debug) {
      RequestLogger.updateDebugLog("Optimized macro:");
      Macrofier.indentify(macro.toString(), true);
    }

    return macro.toString();
  }

  protected static void macroAction(StringBuffer macro, String action, final int finalRound) {
    if (action.length() == 0 || action.equals("skip") || action.startsWith("note ")) {
      return;
    }

    if (CombatActionManager.isMacroAction(action)) {
      if (action.startsWith("\"")) {
        action = action.substring(1);
      }

      if (action.charAt(action.length() - 1) == '\"') {
        action = action.substring(0, action.length() - 1);
      }

      macro.append(action);
      macro.append('\n');

      return;
    }

    action = CombatActionManager.getShortCombatOptionName(action);

    if (action.equals("skip")) {
      return;
    }

    if (action.equals("special")) {
      if (FightRequest.waitingForSpecial) {
        // only allow once per combat
        FightRequest.waitingForSpecial = false;
        String specialAction = FightRequest.getSpecialAction();

        if (specialAction != null) {
          if (specialAction.startsWith("skill")) {
            Macrofier.macroSkill(macro, StringUtilities.parseInt(specialAction.substring(5)));
          } else {
            macro.append("call mafiaround; use " + specialAction + "\n");
            // TODO
          }
        }
      }
    } else if (action.equals("abort")) {
      if (finalRound != 0) {
        macro.append("abort \"KoLmafia CCS abort\"\n");
      } else {
        macro.append("abort \"Click Script button again to continue\"\n");
        macro.append("#mafiarestart\n");
      }
    } else if (action.equals("abort after")) {
      KoLmafia.abortAfter("Aborted by CCS request");
    } else if (action.equals("runaway")) {
      macro.append("runaway\n");
    } else if (action.startsWith("runaway")) {
      int runaway = StringUtilities.parseInt(action.substring(7));
      if (FightRequest.freeRunawayChance() >= runaway) {
        macro.append("runaway\n");
      }
    } else if (action.startsWith("attack")) {
      // Cannot attack as Jarlsberg
      if (KoLCharacter.isJarlsberg()) {
        macro.append("abort \"KoLmafia CCS abort - Jarlsberg cannot attack\"\n");
      } else {
        macro.append("call mafiaround; attack\n");
      }
    } else if (action.equals("steal")) {
      if (MonsterStatusTracker.shouldSteal()) {
        macro.append("pickpocket\n");
      }
    } else if (action.equals("jiggle")) {
      if (EquipmentManager.usingChefstaff()) {
        macro.append("call mafiaround; jiggle\n");
      }
    } else if (action.startsWith("combo ")) {
      int[] combo = DiscoCombatHelper.getCombo(action.substring(6));
      if (combo != null) {
        String name = action.substring(6);
        String raveSteal = DiscoCombatHelper.COMBOS[DiscoCombatHelper.RAVE_STEAL][0];
        if (DiscoCombatHelper.disambiguateCombo(name).equals(raveSteal)
            && !DiscoCombatHelper.canRaveSteal()) {
          // There the limit on the number of Rave Steals has been reached,
          // no point in executing the combo.
        } else {
          Macrofier.macroCombo(macro, combo);
        }
      }
    } else if (action.startsWith("skill")) {
      int skillId = StringUtilities.parseInt(action.substring(5));
      String skillName = SkillDatabase.getSkillName(skillId);

      if (skillName.equals("Transcendent Olfaction")) {
        // You can't sniff if you are already on the trail.

        // You can't sniff in Bad Moon, even though the skill
        // shows up on the char sheet, unless you've recalled
        // your skills.

        if ((KoLCharacter.inBadMoon() && !KoLCharacter.skillsRecalled())
            || !KoLCharacter.availableCombatSkill(SkillPool.OLFACTION)) { // ignore
        } else {
          Macrofier.macroSkill(macro, skillId);
        }
      } else if (skillName.equals(
          "CLEESH")) { // Macrofied combat will continue with the same CCS after
        // a CLEESH, unlike round-by-round combat which switches
        // sections.  Make sure there's something to finish off
        // the amphibian.
        Macrofier.macroSkill(macro, skillId);
        if (finalRound != 0) {
          macro.append("attack; repeat\n");
        }
      } else {
        Macrofier.macroSkill(macro, skillId);
      }
    } else if (!KoLConstants.activeEffects.contains(FightRequest.BIRDFORM)) {
      // Must be an item use
      // Can't use items in Birdform
      int comma = action.indexOf(",");
      int item1 =
          StringUtilities.parseInt(comma != -1 ? action.substring(0, comma).trim() : action);
      int item2 = comma != -1 ? StringUtilities.parseInt(action.substring(comma + 1).trim()) : -1;

      macro.append("call mafiaround; use ");
      macro.append(item1);
      if (item2 != -1) {
        macro.append(",");
        macro.append(item2);
      }
      macro.append("\n");
    } else {
      // Trying to use an item in Birdform. Ignore it.
    }
  }

  public static void indentify(String macro, boolean debug) {
    String indent = "";
    String element = debug ? "\t" : "\u00A0\u00A0\u00A0\u00A0";
    String[] pieces = macro.split("\n");
    for (int i = 0; i < pieces.length; ++i) {
      String line = pieces[i].trim();
      if (line.startsWith("end") && indent.length() > 0) {
        indent = indent.substring(element.length());
      }
      if (debug) {
        RequestLogger.updateDebugLog(indent + line);
      } else {
        RequestLogger.printLine(indent + line);
      }
      if (line.startsWith("if ") || line.startsWith("while ") || line.startsWith("sub ")) {
        indent = indent + element;
      }
    }
  }

  public static void macroCommon(StringBuffer macro) {
    macro.append("sub mafiaround\n");
    Macrofier.macroUseAntidote(macro);
    macro.append("endsub#mafiaround\n");

    macro.append("sub mafiamp\n");
    Macrofier.macroManaRestore(macro);
    macro.append("endsub#mafiamp\n");
  }

  public static void macroSkill(StringBuffer macro, int skillId) {
    long cost = SkillDatabase.getMPConsumptionById(skillId);
    if (cost > KoLCharacter.getMaximumMP()) {
      return; // no point in even trying
    }

    if (cost > 0 && Preferences.getBoolean("autoManaRestore")) {
      macro.append("while mpbelow ");
      macro.append(cost);
      macro.append("\ncall mafiamp\nendwhile\n");
    }
    macro.append("if hasskill ");
    macro.append(skillId);
    macro.append("\ncall mafiaround; skill ");
    macro.append(skillId);
    macro.append("\nendif\n");
  }

  public static void macroCombo(StringBuffer macro, int[] combo) {
    long cost = 0;
    for (int i = 0; i < combo.length; ++i) {
      cost += SkillDatabase.getMPConsumptionById(combo[i]);
    }

    if (cost > KoLCharacter.getMaximumMP()) {
      return; // no point in even trying
    }

    boolean restore = Preferences.getBoolean("autoManaRestore");

    if (restore) {
      macro.append("while mpbelow ");
      macro.append(cost);
      macro.append("\ncall mafiamp\nendwhile\n");
    } else {
      macro.append("if !mpbelow ");
      macro.append(cost);
      macro.append("\n");
    }
    macro.append("call mafiaround; ");
    for (int i = 0; i < combo.length; ++i) {
      macro.append("skill ");
      macro.append(combo[i]);
      macro.append("; ");
    }
    macro.append("\n");
    if (!restore) {
      macro.append("endif\n");
    }
  }

  public static final void macroUseAntidote(StringBuffer macro) {
    if (KoLCharacter.inGLover()) {
      return;
    }
    if (!KoLConstants.inventory.contains(FightRequest.ANTIDOTE)) {
      return;
    }
    if (KoLConstants.activeEffects.contains(FightRequest.BIRDFORM)) {
      return; // can't use items!
    }
    int minLevel = Preferences.getInteger("autoAntidote");
    int poison = MonsterStatusTracker.getPoisonLevel();
    if (poison > minLevel || minLevel == 0) {
      return; // no poison expected that the user wants to remove
    }

    macro.append("if hascombatitem ");
    macro.append(ItemPool.ANTIDOTE);
    macro.append(" && (");
    boolean first = true;
    for (int i = minLevel; i > 0; --i) {
      if (poison != 0 && i != poison) { // only check for the monster's known poison attack
        continue;
      }
      if (!first) {
        macro.append(" || ");
      }
      first = false;
      macro.append("haseffect ");
      macro.append(EffectDatabase.POISON_ID[i]);
    }
    macro.append(")\n  use ");
    macro.append(ItemPool.ANTIDOTE);
    macro.append("\nendif\n");
  }

  public static void macroManaRestore(StringBuffer macro) {
    if (KoLConstants.activeEffects.contains(FightRequest.BIRDFORM)) {
      macro.append("abort \"Cannot use combat items while in Birdform!\"\n");
      return;
    }

    int cumulative = 0;
    for (int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i) {
      MPRestoreItem restorer = MPRestoreItemList.CONFIGURES[i];
      if (restorer.isCombatUsable()) {
        AdventureResult restoreItem = restorer.getItem();
        if (restoreItem == null) {
          continue;
        }

        int count = restoreItem.getCount(KoLConstants.inventory);
        if (count <= 0) {
          continue;
        }

        String itemId = String.valueOf(restoreItem.getItemId());
        cumulative += count;
        if (cumulative >= 30) {
          // Assume this item will be sufficient for all requests
          macro.append("call mafiaround; use ");
          macro.append(itemId);
          macro.append("\nmark mafiampexit\n");
          return;
        }

        macro.append("if hascombatitem ");
        macro.append(itemId);
        macro.append("\ncall mafiaround; use ");
        macro.append(itemId);
        macro.append("\ngoto mafiampexit\nendif\n");
      }
    }

    macro.append("abort \"No MP restoratives!\"\n");
    macro.append("mark mafiampexit\n");
  }
}
