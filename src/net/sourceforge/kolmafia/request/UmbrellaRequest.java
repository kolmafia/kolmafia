package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.preferences.Preferences;

public class UmbrellaRequest extends GenericRequest {
  public UmbrellaRequest() {
    super("choice.php");
  }

  public static enum Form {
    // You attempt to operate the umbrella normally, which causes it to explode into a howling mass
    // of chaos.
    BROKEN(1, "broken", "howling mass of chaos"),
    // You carefully open the umbrella and point it towards the stuff in front of you.
    FORWARD(2, "forward", "the stuff in front of you"),
    // You open the umbrella and let it dangle by the handle.
    BUCKET(3, "bucket", "dangle by the handle"),
    // You open the umbrella so hard it pops inside out.
    PITCHFORK(4, "pitchfork", "pops inside out"),
    // You open the umbrella inside, which causes it to become cursed. Then you begin twirling it,
    // which evenly distributes the curse throughout the umbrella's metal skeleton.
    TWIRL(5, "constantly twirling", "evenly distributes the curse"),
    // You open the umbrella, step inside it, and close it.
    COCOON(6, "cocoon", "step inside it");

    public final int id;
    public final String name;
    public final String descriptionSnippet;

    Form(int id, String name, String descriptionSnippet) {
      this.id = id;
      this.name = name;
      this.descriptionSnippet = descriptionSnippet;
    }

    public void set() {
      Preferences.setString("umbrellaState", this.name);
    }
  }

  public static final void parseUmbrella(final String urlString, final String responseText) {
    if (!urlString.contains("whichchoice=1466")) {
      return;
    }

    for (Form form : Form.values()) {
      if (urlString.contains("option=" + form.id)
          && responseText.contains(form.descriptionSnippet)) {
        form.set();
        return;
      }
    }
  }
}
