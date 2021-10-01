package net.sourceforge.kolmafia.textui.command;

import java.io.File;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.WumpusManager;
import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class WumpusCommand extends AbstractCommand {
  public WumpusCommand() {
    this.usage = " status - Display status of last wumpus cave.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] tokens = parameters.split("\\s+");
    if (tokens.length < 1) {
      return;
    }

    String option = tokens[0];

    if (option.equals("status")) {
      WumpusManager.printStatus();
      return;
    }

    if (option.equals("code")) {
      String code = WumpusManager.getWumpinatorCode();
      RequestLogger.printLine(code);
      return;
    }

    if (option.equals("reset")) {
      WumpusManager.reset();
      return;
    }

    if (option.equals("replay")) {
      if (tokens.length < 2) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Replay from what file?");
        return;
      }

      String fileName = tokens[1];
      File file = new File(KoLConstants.DATA_LOCATION, fileName);

      if (!file.exists()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "File " + file + " does not exist");
        return;
      }

      byte[] bytes = ByteBufferUtilities.read(file);
      String text = StringUtilities.getEncodedString(bytes, "UTF-8");

      KoLmafia.updateDisplay(
          "Read "
              + KoLConstants.COMMA_FORMAT.format(bytes.length)
              + " bytes into a "
              + KoLConstants.COMMA_FORMAT.format(text.length())
              + " character string");

      String[] lines = text.split("\\n");

      // Start with fresh cave data
      WumpusManager.reset();

      int index = 0;
      while (index < lines.length) {
        String encounter = null, exits = null, sounds = null;
        String line = lines[index++];

        // Find the next encounter
        if (!line.startsWith("Encounter:")) {
          continue;
        }

        encounter = line;

        // Look at all lines up to next encounter, collecting exits and sounds
        while (index < lines.length) {
          line = lines[index];
          if (line.startsWith("Encounter:")) {
            break;
          }

          index += 1;

          if (line.startsWith("Exits:")) {
            exits = line;
          } else if (line.startsWith("Sounds:")) {
            sounds = line;
          }
        }

        RequestLogger.printLine(encounter);
        String responseText = WumpusManager.reconstructResponseText(encounter, exits, sounds);
        if (responseText != null) {
          WumpusManager.visitChoice(responseText);
          String buffer =
              "Wumpinator: "
                  + "<a href=\""
                  + WumpusManager.getWumpinatorURL()
                  + "\">"
                  + "&lt;click here&gt;"
                  + "</a>";
          RequestLogger.printLine(buffer);
        }
      }
    }
  }
}
