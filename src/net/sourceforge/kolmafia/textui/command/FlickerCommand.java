package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;

public class FlickerCommand extends AbstractCommand {
  public static final String[][] PIXELS = {
    {
      "flickeringPixel1", "Anger", "Stupid Pipes", "25 hot resistance",
    },
    {
      "flickeringPixel2",
      "Anger",
      "You're Freaking Kidding Me",
      "500 buffed Muscle/Mysticality/Moxie",
    },
    {
      "flickeringPixel3", "Fear", "Snakes", "300 buffed Moxie",
    },
    {
      "flickeringPixel4", "Fear", "So... Many... Skulls...", "25 spooky resistance",
    },
    {
      "flickeringPixel5", "Doubt", "A Stupid Dummy", "+300 bonus damage",
    },
    {
      "flickeringPixel6", "Doubt", "Slings and Arrows", "1000 HP",
    },
    {
      "flickeringPixel7", "Regret", "This Is Your Life", "1000 MP",
    },
    {
      "flickeringPixel8", "Regret", "The Wall of Wailing", "60 prismatic damage",
    },
  };

  public FlickerCommand() {
    this.usage = " - List status of flickering pixels.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    StringBuilder output = new StringBuilder();

    output.append("<table border=2 cols=5>");
    output.append("<tr>");
    output.append("<th>#</th>");
    output.append("<th>Location</th>");
    output.append("<th>Choice</th>");
    output.append("<th>Requirement</th>");
    output.append("<th>Status</th>");
    output.append("</tr>");

    for (int i = 0; i < FlickerCommand.PIXELS.length; ++i) {
      String[] data = FlickerCommand.PIXELS[i];

      output.append("<tr>");

      output.append("<td>");
      output.append((i + 1));
      output.append("</td>");

      output.append("<td>");
      output.append(data[1]);
      output.append("</td>");

      output.append("<td>");
      output.append(data[2]);
      output.append("</td>");

      output.append("<td>");
      output.append(data[3]);
      output.append("</td>");

      output.append("<td>");
      output.append(Preferences.getBoolean(data[0]) ? "have" : "NEED");
      output.append("</td>");

      output.append("</tr>");
    }

    output.append("</table>");

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }
}
