package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.preferences.Preferences;

public class PyramidRequest {
  public static final String getPyramidLocationString(final String urlString) {
    if (!urlString.contains("pyramid_state")) {
      return null;
    }

    String position = PyramidRequest.getPyramidPositionString();
    return "The Lower Chambers (" + position + ")";
  }

  public static final String getPyramidPositionString() {
    switch (Preferences.getInteger("pyramidPosition")) {
      case 1:
        return !Preferences.getBoolean("pyramidBombUsed")
            ? "Empty/Rubble"
            : "Empty/Empty/Ed's Chamber";
      case 2:
        return "Rats/Token";
      case 3:
        return "Rubble/Bomb";
      case 4:
        return "Token/Empty";
      case 5:
        return "Bomb/Rats";
    }

    return "Unknown";
  }

  public static final int advancePyramidPosition() {
    int position = Preferences.getInteger("pyramidPosition");
    if (++position > 5) {
      position = 1;
    }
    Preferences.setInteger("pyramidPosition", position);
    return position;
  }
}
