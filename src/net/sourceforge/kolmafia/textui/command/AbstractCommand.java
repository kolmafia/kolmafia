package net.sourceforge.kolmafia.textui.command;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLmafiaCLI.ParameterHandling;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.utilities.PrefixMap;

public abstract class AbstractCommand {
  // Assign 'flags' in an instance initializer if the command needs one
  // of these:
  // ParameterHandling.FULL_LINE_CMD - the command's parameters are the entire
  //	remainder of the line, semicolons do not end the command.
  // ParameterHandling.FLOW_CONTROL - the remainder of the command line,
  //	plus additional lines as needed to ensure that at least one
  //	command is included, and that the final command is not itself
  //	flagged as FLOW_CONTROL, are made available to this command
  //	via its 'continuation' field, rather than being executed.  The
  //	command can examine and modify the continuation, and execute it
  //	zero or more times by calling CLI.executeLine(continuation).

  public ParameterHandling flags = ParameterHandling.NONE;

  // Assign 'usage' in an instance initializer to set the help usage text.
  // If usage is null, this command won't be shown in the command list.

  public String usage = " - no help available.";

  // Usage strings should start with a space, or [?] if they support the
  // isExecutingCheckOnlyCommand flag, followed by any parameters (with
  // placeholder names enclosed in angle brackets - they'll be italicized
  // in HTML output).
  // There should then be a dash and a brief description of the command.
  // Or, override getUsage(cmd) to dynamically construct the usage text
  // (but it would probably be better to have separate commands in that
  // case).

  public String getUsage(final String cmd) {
    return this.usage;
  }

  // If the command is being called from an ASH Interpreter, here is
  // where it will be.

  public ScriptRuntime callerController = null;

  // Override one of run(cmd, parameters), run(cmd, parameters[]), or
  // run(cmd) to specify the command's action, with different levels of
  // parameter processing.

  public abstract void run(final String cmd, final String parameters);

  // 'CLI' is provided as a reference back to the invoking instance of
  // KoLmafiaCLI, for convenience if the command needs to call any of its
  // non-static methods.
  // Note that this reference can become invalid if another CLI instance
  // is recursively invoked, and happens to execute the same command; any
  // command that uses 'CLI' more than once should put it in a local
  // variable first.

  public KoLmafiaCLI CLI;

  // FLOW_CONTROL_CMDs will have the command line they're to operate on
  // stored here:

  public String continuation;

  // Each command class must be instantiated (probably in a static
  // initializer), and at least one of these methods called on it to add
  // it to the command table.  These methods return 'this', for easy
  // chaining.

  public AbstractCommand register(final String name) {
    // For commands that must be typed with an exact name
    AbstractCommand.lookup.putExact(name.toLowerCase(), this);
    this.registerFlags(name);
    return this;
  }

  public AbstractCommand registerPlural(final String name) {
    // For commands that can be typed with either an exact name or
    // that name with "s" appended
    AbstractCommand.lookup.putExact(name.toLowerCase(), this);
    this.registerFlags(name);
    AbstractCommand.lookup.putExact(name.toLowerCase() + "s", this);
    this.registerFlags(name + "s");
    return this;
  }

  public AbstractCommand registerPrefix(final String prefix) {
    // For commands that are parsed as startsWith(...)
    AbstractCommand.lookup.putPrefix(prefix.toLowerCase(), this);
    this.registerFlags(prefix);
    return this;
  }

  public AbstractCommand registerSubstring(String substring) {
    // For commands that are parsed as indexOf(...)!=-1.  Use sparingly!
    substring = substring.toLowerCase();
    AbstractCommand.substringLookup.put(substring, this);

    // Make it visible in the normal lookup map:
    AbstractCommand.lookup.putExact("*" + substring + "*", this);
    this.registerFlags(substring);
    return this;
  }

  // Internal implementation thingies:

  public static final PrefixMap<AbstractCommand> lookup = new PrefixMap<>();
  public static final Map<String, AbstractCommand> substringLookup = new TreeMap<>();
  public static String fullLineCmds = "";
  public static String flowControlCmds = "";

  public static AbstractCommand getSubstringMatch(final String cmd) {
    for (String key : AbstractCommand.substringLookup.keySet()) {
      if (cmd.contains(key)) {
        return AbstractCommand.substringLookup.get(key);
      }
    }
    return null;
  }

  public static void clear() {
    AbstractCommand.lookup.clear();
    AbstractCommand.substringLookup.clear();
  }

  private void registerFlags(final String name) {
    if (this.flags == ParameterHandling.FULL_LINE) {
      AbstractCommand.fullLineCmds +=
          AbstractCommand.fullLineCmds.length() == 0 ? name : ", " + name;
    }
    if (this.flags == ParameterHandling.FLOW_CONTROL) {
      AbstractCommand.flowControlCmds +=
          AbstractCommand.flowControlCmds.length() == 0 ? name : ", " + name;
    }
  }

  protected static String[] splitCountAndName(final String parameters) {
    String nameString;
    String countString;

    if (parameters.startsWith("\"")) {
      nameString = parameters.substring(1, parameters.length() - 1);
      countString = null;
    } else if (parameters.startsWith("*")
        || parameters.indexOf(" ") != -1 && Character.isDigit(parameters.charAt(0))) {
      countString = parameters.split(" ")[0];
      String rest = parameters.substring(countString.length()).trim();

      if (rest.startsWith("\"")) {
        nameString = rest.substring(1, rest.length() - 1);
      } else {
        nameString = rest;
      }
    } else {
      nameString = parameters;
      countString = null;
    }

    return new String[] {countString, nameString};
  }

  protected static final AdventureResult itemParameter(final String parameter) {
    List<String> potentialItems = ItemDatabase.getMatchingNames(parameter);
    if (potentialItems.isEmpty()) {
      return null;
    }

    int itemId = ItemDatabase.getItemId(potentialItems.get(0));
    return ItemPool.get(itemId, 0);
  }

  protected static final AdventureResult effectParameter(final String parameter) {
    List<String> potentialEffects = EffectDatabase.getMatchingNames(parameter);
    if (potentialEffects.isEmpty()) {
      return null;
    }

    int effectId = EffectDatabase.getEffectId(potentialEffects.get(0));
    return EffectPool.get(effectId, 0);
  }
}
