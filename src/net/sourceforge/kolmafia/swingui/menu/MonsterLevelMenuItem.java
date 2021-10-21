package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.MindControlRequest;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterLevelMenuItem extends ThreadedMenuItem {
  public MonsterLevelMenuItem() {
    super("Monster Level", new MonsterLevelListener());
  }

  private static class MonsterLevelListener extends ThreadedListener {
    @Override
    protected void execute() {
      int maxLevel = 0;

      if (KoLCharacter.canadiaAvailable()) {
        maxLevel = 11;
      } else if (KoLCharacter.knollAvailable()) {
        maxLevel = 10;
      } else if (KoLCharacter.gnomadsAvailable()) {
        maxLevel = 10;
      } else {
        return;
      }

      String[] levelArray = new String[maxLevel + 1];

      for (int i = 0; i <= maxLevel; ++i) {
        levelArray[i] = "Level " + i;
      }

      int currentLevel = KoLCharacter.getMindControlLevel();

      String selectedLevel =
          InputFieldUtilities.input(
              "Change monster annoyance from " + currentLevel + "?", levelArray);

      if (selectedLevel == null) {
        return;
      }

      int setting = StringUtilities.parseInt(selectedLevel.split(" ")[1]);
      RequestThread.postRequest(new MindControlRequest(setting));
    }
  }
}
