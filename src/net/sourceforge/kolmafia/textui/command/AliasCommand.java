package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.Aliases;
import net.sourceforge.kolmafia.utilities.CharacterEntities;

public class AliasCommand extends AbstractCommand {
  {
    this.usage =
        " [ <filter> ] | [ <word> => <expansion> ] - list aliases [matching <filter>] or create CLI abbreviations.";
    this.flags = KoLmafiaCLI.FULL_LINE_CMD;
  }

  @Override
  public void run(final String cmd, final String parameters) {
    int spaceIndex = parameters.indexOf(" => ");

    if (parameters.length() == 0 || spaceIndex == -1) {
      Aliases.print(parameters);
      return;
    }

    String aliasString = parameters.substring(0, spaceIndex).trim();
    String aliasCommand = parameters.substring(spaceIndex + 4).trim();
    Aliases.add(aliasString, aliasCommand);

    RequestLogger.printLine("String successfully aliased.");
    RequestLogger.printLine(aliasString + " => " + CharacterEntities.escape(aliasCommand));
  }
}
