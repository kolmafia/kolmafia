package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.WitchessSolutionDatabase;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.WitchessRequest;

public abstract class WitchessManager {
  public WitchessManager() {}

  public static final void solveDailyPuzzles() {
    var campground = new CampgroundRequest();
    campground.run();
    if (!campground.responseText.contains("chesstable.gif")) {
      KoLmafia.updateDisplay("You don't have a Witchess Set installed in your campground.");
      return;
    }

    // Still confirming, but it seems as though `witchess.php?num=1` might fail to load if we
    // haven't manually navigated to the witchess set this session.
    RequestThread.postRequest(new GenericRequest("campground.php?action=witchess"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1181&option=3"));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1183&option=2"));
    String[] solvedPuzzles = new String[5];

    for (int i = 1; i <= 5; i++) {
      WitchessRequest request = new WitchessRequest(String.valueOf(i), true);
      request.run();
      int puzzleId = request.getThisPuzzle();

      if (puzzleId == -1) {
        // If the puzzleId is -1, we should have already told the user why in the WitchessRequest
        // class, and should simply exit this method now.
        return;
      }

      if (request.getIsSolved()) {
        KoLmafia.updateDisplay("Already solved Witchess Puzzle #" + puzzleId + ".");
        solvedPuzzles[i - 1] = String.valueOf(puzzleId);
        continue;
      }

      KoLmafia.updateDisplay("Attempting to solve Witchess Puzzle #" + puzzleId + "...");
      var solution = WitchessSolutionDatabase.getWitchessSolution(puzzleId);

      var solRequest = new GenericRequest("witchess.php");
      solRequest.addFormField("sol", solution.getCoords());
      solRequest.addFormField("ajax", "1");
      solRequest.addFormField("number", String.valueOf(puzzleId));
      solRequest.run();

      if (solRequest.responseText.startsWith("[true")) {
        KoLmafia.updateDisplay("Solved!");
        solvedPuzzles[i - 1] = String.valueOf(puzzleId);
      } else {
        KoLmafia.updateDisplay(
            KoLConstants.MafiaState.ABORT,
            "Failed to solve the Witchess Puzzle for some reason. If this happens again, please file a bug report.");
        return;
      }
    }

    KoLmafia.updateDisplay("Solved daily Witchess puzzles: " + String.join(", ", solvedPuzzles));
  }
}
