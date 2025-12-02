package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.preferences.Preferences;

public class PyramidRequest {
  private PyramidRequest() {}

  public static final String getPyramidLocationString(final String urlString) {
    if (!urlString.contains("pyramid_state")) {
      return null;
    }

    String position = PyramidRequest.getPyramidPositionString();
    return "The Lower Chambers (" + position + ")";
  }

  public static final String getPyramidPositionString() {
    return switch (Preferences.getInteger("pyramidPosition")) {
      case 1 ->
          !Preferences.getBoolean("pyramidBombUsed") ? "Empty/Rubble" : "Empty/Empty/Ed's Chamber";
      case 2 -> "Rats/Token";
      case 3 -> "Rubble/Bomb";
      case 4 -> "Token/Empty";
      case 5 -> "Bomb/Rats";
      default -> "Unknown";
    };
  }

  public static final int advancePyramidPosition() {
    int position = Preferences.getInteger("pyramidPosition");
    if (++position > 5) {
      position = 1;
    }
    Preferences.setInteger("pyramidPosition", position);
    return position;
  }

  public static final int lowerChamberTurnsUsed() {
    int position = Preferences.getInteger("pyramidPosition");
    boolean bombed = Preferences.getBoolean("pyramidBombUsed");
    return (position == 1 && bombed) ? 7 : 1;
  }
}
