package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;

public class MonsterDataCommand extends AbstractCommand {
  public MonsterDataCommand() {
    this.usage = " <location> - show combat details for the specified area.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    KoLAdventure location = AdventureDatabase.getAdventure(parameters);
    if (location == null) {
      return;
    }

    AreaCombatData data = AdventureDatabase.getAreaCombatData(location.toString());
    if (data == null) {
      return;
    }

    StringBuffer buffer = new StringBuffer();

    buffer.append("<html>");
    data.appendMonsterData(buffer, false);
    buffer.append("</html>");

    KoLmafiaCLI.showHTML(buffer.toString());
  }
}
