package net.sourceforge.kolmafia.textui.command;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.utilities.PrefixMap.KeyType;

public class CliRefCommand extends AbstractCommand {
  public CliRefCommand() {
    this.usage = " [<filter>] - list CLI commands [that match filter].";
  }

  private static final Pattern PLACEHOLDER = Pattern.compile("<(.+?)>");

  @Override
  public void run(final String cmd, String filter) {
    filter = filter.toLowerCase();
    if (filter.equals("help")) {
      RequestLogger.printLine(
          "Square brackets [ ] enclose optional elements of "
              + "commands.  In command descriptions, they may also enclose the effects of  "
              + "using those optional elements.");
      RequestLogger.printLine();
      RequestLogger.printLine(
          "Vertical bars | separate alternative elements -  "
              + "choose any one.  (But note that || is an actual part of a few commands.)");
      RequestLogger.printLine();
      RequestLogger.printLine(
          "An ellipsis ... after an element means that it  "
              + "can be repeated as many times as needed.");
      RequestLogger.printLine();
      RequestLogger.printLine(
          "Elements in <i>italics</i> are placeholders -  "
              + "replace them with an actual name you want the command to operate on.");
      RequestLogger.printLine();
      RequestLogger.printLine(
          "Commands with an asterisk * after the name are "
              + "abbreviations - you can type them in a longer form if desired.");
      RequestLogger.printLine();
      RequestLogger.printLine(
          "Some command names can be followed by a question  "
              + "mark (shown as [?] ), in which case the command will just display what it  "
              + "would do, rather than actually doing it.");
      RequestLogger.printLine();
      RequestLogger.printLine(
          "When adventuring, or using an item or skill, the  "
              + "name can be preceded by a number specifying how many times to do it.  An  "
              + "asterisk in place of this number means \"as many as possible\" or \"the  "
              + "current quantity in inventory\", depending on context.  Negative numbers  "
              + "mean to do that many less than the maximum.");
      RequestLogger.printLine();
      RequestLogger.printLine(
          "Usually, multiple commands can be given on the  "
              + "same line, separated by semicolons.  The exceptions ("
              + AbstractCommand.fullLineCmds
              + ") treat the entire remainder of the line as a parameter.");
      RequestLogger.printLine();
      RequestLogger.printLine(
          "A few commands ("
              + AbstractCommand.flowControlCmds
              + ") treat at least one following command as a block that is executed  "
              + "conditionally or repetitively.  The block consists of the remainder of the  "
              + "line, or the entire next line if that's empty.  The block is extended by "
              + "additional lines if it would otherwise end with one of these special  "
              + "commands.");
      return;
    }
    boolean anymatches = false;
    HashMap<String, String> alreadySeen =
        new HashMap<>(); // usage => name of cmd already printed out
    Iterator<Map.Entry<String, AbstractCommand>> i = AbstractCommand.lookup.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry<String, AbstractCommand> e = i.next();
      String name = e.getKey();
      KeyType type = AbstractCommand.lookup.getKeyType(name);
      if (type == KeyType.NOT_A_KEY) {
        continue;
      }
      AbstractCommand handler = e.getValue();
      if (handler == null) { // shouldn't happen
        continue;
      }
      String usage = handler.getUsage(name);
      if (usage == null
          || name.indexOf(filter) == -1 && usage.toLowerCase().indexOf(filter) == -1) {
        continue;
      }
      if (type == KeyType.PREFIX_KEY) {
        name += "*";
      }
      if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
        RequestLogger.printLine(
            DataUtilities.convertToHTML(name) + " @ " + handler.getClass().getName());
        anymatches = true;
        continue;
      }
      String previouslySeen = alreadySeen.get(usage);
      if (previouslySeen == null) {
        // This isn't turning out very useful
        // alreadySeen.put( usage, name );
      } else {
        usage = " => " + previouslySeen;
      }
      anymatches = true;
      Matcher m = CliRefCommand.PLACEHOLDER.matcher(usage);
      while (m.find()) {
        usage =
            Pattern.compile("<?(\\Q" + m.group(1) + "\\E)>?")
                .matcher(usage)
                .replaceAll("<i>$1</i>");
      }
      RequestLogger.printLine(DataUtilities.convertToHTML(name) + usage);
    }
    if (!anymatches) {
      KoLmafia.updateDisplay("No matching commands found!");
    }
  }
}
