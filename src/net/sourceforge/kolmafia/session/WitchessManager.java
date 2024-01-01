package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.WitchessSolutionDatabase;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.WitchessRequest;

public abstract class WitchessManager {
	public WitchessManager() {}

	public static final String solveDailyPuzzles() {
		var campground = new CampgroundRequest();
		campground.run();
		if (!campground.responseText.contains("chesstable.gif")) {
			KoLmafia.updateDisplay("You don't have a Witchess Set installed in your campground.");
			return null;
		}

		String[] solvedPuzzles = new String[5];

		for (int i = 1; i <= 5; i++) {
			WitchessRequest request = new WitchessRequest(String.valueOf(i));
			request.run();
			int puzzleId = request.getThisPuzzle();

			if (request.getIsSolved()) {
				solvedPuzzles[i - 1] = String.valueOf(puzzleId);
				continue;
			}

			var solution = WitchessSolutionDatabase.getWitchessSolution(puzzleId);

			var solRequest = new GenericRequest("witchess.php");
			solRequest.addFormField("sol", solution.getCoords());
			solRequest.addFormField("ajax", "1");
			solRequest.addFormField("number", String.valueOf(puzzleId));
			solRequest.run();

			solvedPuzzles[i] = String.valueOf(puzzleId);
		}

		KoLmafia.updateDisplay("Solved daily Witchess puzzles: " + String.join(", ", solvedPuzzles));
		return null;
	}
}
