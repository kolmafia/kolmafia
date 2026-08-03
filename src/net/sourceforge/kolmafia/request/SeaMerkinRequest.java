package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class SeaMerkinRequest extends GenericRequest {
  public SeaMerkinRequest() {
    super("sea_merkin.php");
  }

  public SeaMerkinRequest(final String action) {
    this();
    this.addFormField("action", action);
  }

  @Override
  public void processResults() {
    SeaMerkinRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("sea_merkin.php")) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action != null && action.equals("temple")) {
      // Normally, this redirects to choice.php?forceoption=0
      // If you have already won, you will come here.

      if (KoLCharacter.inSeaPath()
          && responseText.contains("This part of the temple is now empty")) {
        if (urlString.contains("subaction=left"))
          Preferences.setBoolean("shubJigguwattDefeated", true);
        if (urlString.contains("subaction=right")) Preferences.setBoolean("yogUrtDefeated", true);
      } else {
        if (responseText.contains("The temple is empty")) {
          Preferences.setString("merkinQuestPath", "done");
        }
      }
    }
  }

  private static final String COLOSSEUM = "snarfblat=" + AdventurePool.MERKIN_COLOSSEUM;

  public static final void parseColosseumResponse(
      final String urlString, final String responseText) {
    if (!urlString.contains(SeaMerkinRequest.COLOSSEUM)) {
      return;
    }

    // If we have already finished the quest, we don't care what it
    // says when you visit the Colosseum
    if (Preferences.getString("merkinQuestPath").equals("done")) {
      return;
    }

    // The Colosseum is empty -- your crowd of Mer-kin admirers
    // (or, for all you know, your crowd of Mer-kin who totally,
    // totally hate you,) has gone home.

    if (responseText.contains("your crowd of Mer-kin admirers")) {
      Preferences.setBoolean("isMerkinGladiatorChampion", true);
      // The following is not applicable in the Sea path
      Preferences.setString("merkinQuestPath", "gladiator");
      Preferences.setInteger("lastColosseumRoundWon", 15);
    }

    // As you approach the Colosseum, the guards in the
    // front whisper "Praise be to the High Priest!" and
    // kneel before you. Unfortunately, they kneel in a way
    // that crosses their spears in front of the Colosseum
    // entrance, and you can't get in.

    else if (responseText.contains("Praise be to the High Priest")) {
      Preferences.setBoolean("isMerkinHighPriest", true);
      // The following is not applicable in the Sea path
      Preferences.setString("merkinQuestPath", "scholar");
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("sea_merkin.php")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action != null && action.equals("temple")) {
      // Defer to AdventureDatabase, since it is an adventure
      return false;
    }

    return false;
  }
}
