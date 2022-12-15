package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.preferences.Preferences;

public class TrainsetManager {
  public enum TrainsetPiece {
    UNKNOWN("unknown", -1),
    EMPTY_TRACK("empty", 0),
    MEAT_MINE("meat_mine", 1),
    TOWER_FIZZY("tower_fizzy", 2),
    VIEWING_PLATFORM("viewing_platform", 3),
    TOWER_FROZEN("tower_frozen", 4),
    SPOOKY_GRAVEYARD("spooky_graveyard", 5),
    LOGGING_MILL("logging_mill", 6),
    CANDY_FACTORY("candy_factory", 7),
    COAL_HOPPER("coal_hopper", 8),
    TOWER_SEWAGE("tower_sewage", 9),
    OIL_REFINERY("oil_refinery", 11),
    OIL_BRIDGE("oil_bridge", 12),
    WATER_BRIDGE("water_bridge", 13),
    GROIN_SILO("groin_silo", 14),
    GRAIN_SILO("grain_silo", 15),
    BRAIN_SILO("brain_silo", 16),
    BRAWN_SILO("brawn_silo", 17),
    PRAWN_SILO("prawn_silo", 18),
    TRACKSIDE_DINER("trackside_diner", 19),
    ORE_HOPPER("ore_hopper", 20);

    private final String name;
    private final int id;

    TrainsetPiece(String name, int id) {
      this.name = name;
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public int getId() {
      return id;
    }

    public static TrainsetPiece findPiece(int id) {
      for (TrainsetPiece piece : values()) {
        if (piece.getId() != id) continue;

        return piece;
      }

      return TrainsetPiece.UNKNOWN;
    }
  }

  private static final Pattern SELECTED_STATION =
      Pattern.compile(
          "data-slot=\"(\\d+)\" class=\"trainslot dragtospot\" style=\"position: absolute; left: \\d+px; "
              + "top: \\d+px; height: 80px; width: 80px;\"><div data-id=\"(\\d+)\"");
  private static final Pattern CURRENT_STATION =
      Pattern.compile("<br>Your train is about to pass station (\\d)\\.<");
  private static final Pattern LAPS_BEFORE_RECONFIGURE =
      Pattern.compile("Let the train finish (\\d) more laps before rearranging it.</p>");
  private static final int TURNS_BETWEEN_CONFIGURE = 40;

  private static TrainsetPiece[] getTrainsetPieces(String html) {
    Matcher matcher = SELECTED_STATION.matcher(html);
    TrainsetPiece[] pieces = new TrainsetPiece[8];
    Arrays.fill(pieces, TrainsetPiece.EMPTY_TRACK);

    while (matcher.find()) {
      int index = Integer.parseInt(matcher.group(1));
      int pieceId = Integer.parseInt(matcher.group(2));
      // Set it to UNKNOWN so that if a piece is not found, it'll be marked as unknown and not empty
      pieces[index] = TrainsetPiece.findPiece(pieceId);
    }

    return pieces;
  }

  public static void visitChoice(String html) {
    int lastPosition = Preferences.getInteger("trainsetPosition");

    Matcher matcher = CURRENT_STATION.matcher(html);

    // The station cannot be read while the configuration is locked
    // This is likely a bug in kol
    if (matcher.find()) {
      // Retrieve the current position of the train, subtract 1 so the range is 0 to 7
      int currentPosition = Integer.parseInt(matcher.group(1)) - 1;

      // If position is different from what was expected, update trainsetPosition.
      // Round it up to the next position
      if (currentPosition != (lastPosition % 8)) {
        lastPosition = lastPosition + (8 - (lastPosition % 8)) + currentPosition;

        Preferences.setInteger("trainsetPosition", lastPosition);
      }
    }

    // If the trainset has been configured, set lastTrainsetConfiguration to the last position of
    // the train.
    // The train can be reconfigured 5 * 8 = 40 positions later.
    if (html.contains(">Train set reconfigured.</span>")) {
      Preferences.setInteger(
          "lastTrainsetConfiguration", Preferences.getInteger("trainsetPosition"));
    } else {
      int lastConfiguration = Preferences.getInteger("lastTrainsetConfiguration");
      int expectedTurnConfigurable = lastConfiguration + TURNS_BETWEEN_CONFIGURE;

      Matcher laps = LAPS_BEFORE_RECONFIGURE.matcher(html);

      // If we can configure the train
      if (!laps.find()) {
        // If we expected train to be unconfigurable
        if (lastPosition < expectedTurnConfigurable) {
          // Set the last configuration to a value 40 turns behind
          Preferences.setInteger(
              "lastTrainsetConfiguration", lastPosition - TURNS_BETWEEN_CONFIGURE);
        }
      } else {
        // If we cannot reconfigure the train, we will be assuming worse case scenario of a full 5
        // laps.

        // Get the expected laps remaining, and actual laps remaining
        int expectedLapsRemaining =
            5 - (int) Math.ceil((expectedTurnConfigurable - lastPosition) / 8D);
        int actualLapsRemaining = Integer.parseInt(laps.group(1));

        // If the expected laps is different from the actual laps
        if (expectedLapsRemaining != actualLapsRemaining) {
          // Set the last configuration to laps x 8 behind.
          // So 5 laps remaining will set the last position to -39 if lastPosition is 0
          // We don't set it to -40 as it would indicate a full 5 laps has elapsed
          Preferences.setInteger(
              "lastTrainsetConfiguration", lastPosition - (((5 - actualLapsRemaining) * 8) + 1));
        }
      }
    }

    TrainsetPiece[] pieces = getTrainsetPieces(html);

    String trainsetConfiguration =
        Arrays.stream(pieces).map(TrainsetPiece::getName).collect(Collectors.joining(","));

    Preferences.setString("trainsetConfiguration", trainsetConfiguration);
  }
}
