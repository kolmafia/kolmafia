package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BountyCommand extends AbstractCommand {
  public BountyCommand() {
    this.usage = " (easy|hard|special) - List or optionally accept bounties";
  }

  @Override
  public void run(final String cmd, String parameter) {
    parameter = parameter.trim();

    StringBuilder output = new StringBuilder();

    if (parameter.equals("")) {
      GenericRequest hunterRequest = new BountyHunterHunterRequest();
      RequestThread.postRequest(hunterRequest);

      output.append("<table border=2 cols=4>");
      output.append("<tr>");
      output.append("<th>Type</th>");
      output.append("<th>Bounty</th>");
      output.append("<th>Monster</th>");
      output.append("<th>Location</th>");
      output.append("</tr>");

      output.append(
          BountyCommand.showDetail(
              "Easy",
              Preferences.getString("currentEasyBountyItem"),
              Preferences.getString("_untakenEasyBountyItem")));
      output.append(
          BountyCommand.showDetail(
              "Hard",
              Preferences.getString("currentHardBountyItem"),
              Preferences.getString("_untakenHardBountyItem")));
      output.append(
          BountyCommand.showDetail(
              "Special",
              Preferences.getString("currentSpecialBountyItem"),
              Preferences.getString("_untakenSpecialBountyItem")));
      output.append("</table>");
    } else if (parameter.equalsIgnoreCase("easy")) {
      RequestThread.postRequest(new BountyHunterHunterRequest("takelow"));
    } else if (parameter.equalsIgnoreCase("hard")) {
      RequestThread.postRequest(new BountyHunterHunterRequest("takehigh"));
    } else if (parameter.equalsIgnoreCase("special")) {
      RequestThread.postRequest(new BountyHunterHunterRequest("takespecial"));
    } else {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "bounty (easy|hard|special) - List or optionally accept bounties");
      return;
    }

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }

  private static String showDetail(String type, String bounty, String untaken) {
    StringBuilder output = new StringBuilder();
    int bountyIndex = bounty.indexOf(":");

    if (bountyIndex == -1 && untaken.equals("")) {
      output.append("<tr>");
      output.append("<td>" + type + "</td>");
      output.append("<td>No bounty available</td><td></td><td></td>");
      return output.toString();
    } else if (bountyIndex != -1) {
      String bountyName = bounty.substring(0, bountyIndex);
      int bountyCount = StringUtilities.parseInt(bounty.substring(bountyIndex + 1));
      output.append("<tr>");
      output.append("<td>" + type + "</td>");
      if (bountyName != null && !bountyName.equals("")) {
        int bountyNumber = BountyDatabase.getNumber(bountyName);
        String bountyPlural = BountyDatabase.getPlural(bountyName);
        if (bountyPlural != null && !bountyPlural.equals("")) {
          output.append(
              "<td>Need " + (bountyNumber - bountyCount) + " more " + bountyPlural + "</td>");
        } else {
          output.append(
              "<td>Need " + (bountyNumber - bountyCount) + " more " + bountyName + "</td>");
        }
        String bountyMonster = BountyDatabase.getMonster(bountyName);
        if (bountyMonster != null && !bountyMonster.equals("")) {
          output.append("<td>" + bountyMonster + "</td>");
        } else {
          output.append("<td></td>");
        }
        String bountyLocation = BountyDatabase.getLocation(bountyName);
        if (bountyLocation != null && !bountyLocation.equals("")) {
          output.append("<td>" + bountyLocation + "</td>");
        } else {
          output.append("<td></td>");
        }
      } else {
        output.append("<td></td><td></td><td></td>");
      }
      output.append("</tr>");
      return output.toString();
    } else {
      output.append("<tr>");
      output.append("<td>" + type + "</td>");
      int bountyNumber = BountyDatabase.getNumber(untaken);
      String bountyPlural = BountyDatabase.getPlural(untaken);
      if (bountyPlural != null && !bountyPlural.equals("")) {
        output.append("<td>Accept and get " + bountyNumber + " " + bountyPlural + "</td>");
      } else {
        output.append("<td></td>");
      }
      String bountyMonster = BountyDatabase.getMonster(untaken);
      if (bountyMonster != null && !bountyMonster.equals("")) {
        output.append("<td>" + bountyMonster + "</td>");
      } else {
        output.append("<td></td>");
      }
      String bountyLocation = BountyDatabase.getLocation(untaken);
      if (bountyLocation != null && !bountyLocation.equals("")) {
        output.append("<td>" + bountyLocation + "</td>");
      } else {
        output.append("<td></td>");
      }
      output.append("</tr>");
      return output.toString();
    }
  }
}
