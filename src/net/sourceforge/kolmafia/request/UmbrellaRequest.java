package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.preferences.Preferences;

public class UmbrellaRequest extends GenericRequest {
  public UmbrellaRequest() {
    super("choice.php");
  }

  public static final void parseUmbrella(final String urlString, final String responseText) {
    if (!urlString.contains("whichchoice=1466")) {
      return;
    }

    // Choice 7, walk away
    // You decide to stop messing with your umbrella, lest you break it.
    if (urlString.contains("option=7")) {
      return;
    }

    // Choice 1, +25% ML
    // You attempt to operate the umbrella normally, which causes it to explode into a howling mass
    // of chaos.
    if (urlString.contains("option=1") && responseText.contains("howling mass of chaos")) {
      Preferences.setInteger("umbrellaState", 1);
    }

    // Choice 2, makes it a shield with +25 DR
    // You carefully open the umbrella and point it towards the stuff in front of you.
    else if (urlString.contains("option=2") && responseText.contains("the stuff in front of you")) {
      Preferences.setInteger("umbrellaState", 2);
    }

    // Choice 3, +25% Item Drop
    // You open the umbrella and let it dangle by the handle.
    else if (urlString.contains("option=3") && responseText.contains("dangle by the handle")) {
      Preferences.setInteger("umbrellaState", 3);
    }

    // Choice 4, +25 Weapon Damage
    // You open the umbrella so hard it pops inside out.
    else if (urlString.contains("option=4") && responseText.contains("pops inside out")) {
      Preferences.setInteger("umbrellaState", 4);
    }

    // Choice 5, +25 Spell Damage
    // You open the umbrella inside, which causes it to become cursed. Then you begin twirling it,
    // which evenly distributes the curse throughout the umbrella's metal skeleton.
    else if (urlString.contains("option=5")
        && responseText.contains("evenly distributes the curse")) {
      Preferences.setInteger("umbrellaState", 5);
    }

    // Choice 6, -10% Combat
    // You open the umbrella, step inside it, and close it.
    else if (urlString.contains("option=6") && responseText.contains("step inside it")) {
      Preferences.setInteger("umbrellaState", 6);
    }
  }
}
