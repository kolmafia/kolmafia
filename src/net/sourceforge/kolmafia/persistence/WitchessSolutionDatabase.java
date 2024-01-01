package net.sourceforge.kolmafia.persistence;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WitchessSolutionDatabase {
	private static final Map<Integer, WitchessSolution> witchessSolutions = new HashMap<>();

	private static final Map<Character, int[]> moveDict = new HashMap<>();

	private WitchessSolutionDatabase() {}

	static {
		moveDict.put('r', new int[]{0,2});
		moveDict.put('l', new int[]{0,-2});
		moveDict.put('u', new int[]{-2,0});
		moveDict.put('d', new int[]{2,0});
		WitchessSolutionDatabase.reset();
	}

	public static void reset() {
		try (BufferedReader reader =
					 FileUtilities.getVersionedReader("witchess_solutions.txt", KoLConstants.WITCHESS_SOLUTIONS_VERSION)) {
			String[] data;

			while ((data = FileUtilities.readData(reader)) != null) {
				if (data.length < 2) {
					continue;
				}

				int puzzleId = StringUtilities.parseInt((data[0]));
				var solution = parseSolution(puzzleId, data);

				WitchessSolutionDatabase.witchessSolutions.put(puzzleId, solution);
			}
		} catch (IOException e) {
			StaticEntity.printStackTrace(e);
		}
	}

	public static class WitchessSolution {
		protected final int puzzleId;
		protected final Character[] moves;
		protected final String coords;

		public WitchessSolution(int puzzleId, Character[] moves, String coords) {
			this.puzzleId = puzzleId;
			this.moves = moves;
			this.coords = coords;
		}

		public int getId() {
			return this.puzzleId;
		}

		public String getCoords() {
			return this.coords;
		}
	}

	private static WitchessSolution parseSolution(Integer puzzleId, String[] data) {
		String[] solutionSteps = data[1].trim().split(" ");
		Character[] solution = new Character[solutionSteps.length];
		for (int i = 0; i < solutionSteps.length; i++) {
			solution[i] = solutionSteps[i].charAt(0);
		}

		var coords = solvePath(solution);
		return new WitchessSolution(puzzleId, solution, coords);
	}

	private static int[] calculateMidpoint(int[] start, int[] end) {
		int midX = (start[0] + end[0]) / 2;
		int midY = (start[1] + end[1]) / 2;
		return new int[]{midX, midY};
	}

	private static String solvePath(Character[] moves) {
		/*
			We could hard-code the size of each puzzle and therefore the starting coordinate,
			but to reduce the need for transcribed data, we can instead determine the size of each grid
			based on the moves made. Since you move from one extreme of the grid to the opposite,
			we can assume that the max grid size will always be the absolute value of the difference between
			each move in the cardinal directions (up/down and left/right)

			However, each move increments the grid position by 2, so we need to double the difference
			to get the real size and coordinates.
		 */
		Map<Character, Integer> moveCounts = new HashMap<>();
		for (char move : moves) {
			moveCounts.put(move, moveCounts.getOrDefault(move, 0) + 1);
		}

		int netVerticalDisplacement = Math.abs(moveCounts.getOrDefault('u', 0) - moveCounts.getOrDefault('d', 0));
		int netHorizontalDisplacement = Math.abs(moveCounts.getOrDefault('l', 0) - moveCounts.getOrDefault('r', 0));

		int maxX = netVerticalDisplacement * 2;
		int maxY = netHorizontalDisplacement * 2;

		int x = maxX, y = 0;
		Set<String> path = new HashSet<>();

		for (char move: moves) {
			int[] start = new int[]{x,y};
			int[] delta = moveDict.get(move);
			x = Math.max(0, Math.min(x + delta[0], maxX));
			y = Math.max(0, Math.min(y + delta[1], maxY));
			int[] end = new int[]{x, y};
			int[] midpoint = calculateMidpoint(start, end);
			path.add(midpoint[0] + "," + midpoint[1]);
		}

		List<String> sortedPath = new ArrayList<>(path);
		sortedPath.sort(Comparator.comparingInt((String s) -> Integer.parseInt(s.split(",")[0]))
				.thenComparing(s -> Integer.parseInt(s.split(",")[1])));

		return String.join("|", sortedPath);
	}

	public static WitchessSolution getWitchessSolution(final Integer puzzleId) {
		return WitchessSolutionDatabase.witchessSolutions.get(puzzleId);
	}
}