package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.request.CakeArenaRequest;
import net.sourceforge.kolmafia.swingui.FamiliarTrainingFrame;

public class CakeArenaManager {
  private static final LockableListModel<ArenaOpponent> opponentList =
      new LockableListModel<ArenaOpponent>();

  /**
   * Registers an opponent inside of the arena manager. This should be used to update any
   * information that relates to the arena.
   */
  public static final void registerOpponent(
      final int opponentId, final String name, final String race, final int weight) {
    ArenaOpponent ao = new ArenaOpponent(opponentId, name, race, weight);

    int index = CakeArenaManager.opponentList.indexOf(ao);

    if (index != -1) {
      CakeArenaManager.opponentList.remove(ao);
    } else {
      index = CakeArenaManager.opponentList.size();
    }

    CakeArenaManager.opponentList.add(index, ao);
  }

  /** Retrieves the opponents Id based on the string description for the opponent. */
  public static final void fightOpponent(
      final String target, final int eventId, final int repeatCount) {
    for (int i = 0; i < CakeArenaManager.opponentList.size(); ++i) {
      ArenaOpponent opponent = CakeArenaManager.opponentList.get(i);
      if (target.equals(opponent.toString())) {
        FamiliarTrainingFrame.getResults().clear();

        int opponentId = opponent.getId();
        CakeArenaRequest request = new CakeArenaRequest(opponentId, eventId);

        for (int j = 1; KoLmafia.permitsContinue() && j <= repeatCount; ++j) {
          KoLmafia.updateDisplay("Arena battle, round " + j + " in progress...");
          RequestThread.postRequest(request);

          Matcher victoryMatcher = CakeArenaRequest.WIN_PATTERN.matcher(request.responseText);
          StringBuffer text = new StringBuffer();

          if (victoryMatcher.find()) {
            text.append("<font color=green><b>Round " + j + " of " + repeatCount + "</b></font>: ");
          } else {
            text.append("<font color=red><b>Round " + j + " of " + repeatCount + "</b></font>: ");
          }

          int start = request.responseText.indexOf("<body>");
          int end = request.responseText.indexOf("</table>", start);

          String body = request.responseText.substring(start, end);
          body = body.replaceAll("<p>", KoLConstants.LINE_BREAK);
          body = body.replaceAll("<.*?>", "");
          body = body.replaceAll(KoLConstants.LINE_BREAK, "<br>");
          text.append(body);

          text.append("<br><br>");
          FamiliarTrainingFrame.getResults().append(text.toString());
        }

        KoLmafia.updateDisplay("Arena battles complete.");
        return;
      }
    }
  }

  /** Returns a list of opponents are available today at the cake-shaped arena. */
  public static final LockableListModel<ArenaOpponent> getOpponentList() {
    if (CakeArenaManager.opponentList.isEmpty()) {
      RequestThread.postRequest(new CakeArenaRequest());
    }

    return CakeArenaManager.opponentList;
  }

  public static final ArenaOpponent getOpponent(final int opponentId) {
    int count = CakeArenaManager.opponentList.size();

    for (int i = 0; i < count; ++i) {
      ArenaOpponent ao = CakeArenaManager.opponentList.get(i);
      if (ao.getId() == opponentId) {
        return ao;
      }
    }

    return null;
  }

  public static final String eventIdToName(final int eventId) {
    switch (eventId) {
      case 1:
        return "Ultimate Cage Match";
      case 2:
        return "Scavenger Hunt";
      case 3:
        return "Obstacle Course";
      case 4:
        return "Hide and Seek";
      default:
        return "Unknown Event";
    }
  }

  public static final int eventNameToId(final String eventName) {
    return eventName.equals("Ultimate Cage Match")
        ? 1
        : eventName.equals("Scavenger Hunt")
            ? 2
            : eventName.equals("Obstacle Course") ? 3 : eventName.equals("Hide and Seek") ? 4 : 0;
  }

  /** An internal class which represents a single arena opponent. Used to track the opponent. */
  public static class ArenaOpponent {
    private final int id;
    private final String name;
    private final String race;
    private final int weight;
    private final String description;

    public ArenaOpponent(final int id, final String name, final String race, final int weight) {
      this.id = id;
      this.name = name;
      this.race = race;
      this.weight = weight;
      this.description = race + " (" + weight + " lbs)";
    }

    public int getId() {
      return this.id;
    }

    public String getName() {
      return this.name;
    }

    public String getRace() {
      return this.race;
    }

    public int getWeight() {
      return this.weight;
    }

    @Override
    public String toString() {
      return this.description;
    }

    @Override
    public boolean equals(final Object o) {
      return o instanceof ArenaOpponent && this.id == ((ArenaOpponent) o).id;
    }

    @Override
    public int hashCode() {
      return this.id;
    }
  }
}
