package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class KnollRequest extends PlaceRequest {
  public KnollRequest() {
    super("knoll_friendly");
  }

  public KnollRequest(final String action) {
    super("knoll_friendly", action);
  }

  public static String getNPCName(final String action) {
    if (action == null) {
      return null;
    }

    if (action.equals("dk_mayor")) {
      return "Mayor Zapruder";
    }

    if (action.equals("dk_innabox")) {
      return "Innabox";
    }

    if (action.equals("dk_plunger")) {
      return "The Plunger";
    }

    return null;
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    String action = GenericRequest.getAction(urlString);

    if (action == null) {
      return;
    }

    if (action.equals("dk_mayor")) {
      // Mayor Zapruder assigns quests and gives you an
      // elemental fairy or equipment.

      if (responseText.indexOf("flaming glowsticks") != -1) {
        ResultProcessor.processItem(ItemPool.FLAMING_MUSHROOM, -1);
      } else if (responseText.indexOf("iced-out bling") != -1) {
        ResultProcessor.processItem(ItemPool.FROZEN_MUSHROOM, -1);
      } else if (responseText.indexOf("limburger biker boots") != -1) {
        ResultProcessor.processItem(ItemPool.STINKY_MUSHROOM, -1);
      }

      // Quest handling from here down to the return;

      // Ah, you must be our newest Citizen! It is fortunate that you have arrived, for dire times
      // are
      // upon us, and we require assistance.

      // As you may know, we train bugbears as pets and guards. Lately, though, something is causing
      // our bugbears to become vicious, and to attack their handlers. I would be grateful if you
      // would investigate this for me. We keep our bugbears in a pen near the Spooky Forest.
      if (responseText.indexOf("It is fortunate that you have arrived") != -1) {
        QuestDatabase.setQuestIfBetter(Quest.BUGBEAR, QuestDatabase.STARTED);
      }
      // Mayor Zapruder looks at the tiny pitchfork you've brought him.

      // "Spooky Gravy Fairies! I should've known.
      else if (responseText.indexOf("Mayor Zapruder looks at the tiny pitchfork") != -1) {
        QuestDatabase.setQuestIfBetter(Quest.BUGBEAR, "step1");
      }
      // "Excellent, Adventurer. Please, hand me the mushroom..."
      else if (responseText.indexOf("Please, hand me the mushroom") != -1) {
        QuestDatabase.setQuestIfBetter(Quest.BUGBEAR, "step2");
      }
      // "You've done it! The bugbears have finally returned to a state of normalcy.
      // Without their Queen to lead them, the spooky gravy fairies won't cause us any more
      // problems.
      // And now, for your reward."
      else if (responseText.indexOf("The bugbears have finally returned to a state of normalcy")
          != -1) {
        QuestDatabase.setQuestIfBetter(Quest.BUGBEAR, QuestDatabase.FINISHED);
      }

      return;
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("place.php") || !urlString.contains("knoll_friendly")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);

    // We have nothing special to do for other simple visits.
    if (action == null) {
      return true;
    }

    String npc = getNPCName(action);
    if (npc != null) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog("Visiting " + npc);
      return true;
    }

    // Other requests handle other actions in the Knoll
    // action = gym

    return false;
  }
}
