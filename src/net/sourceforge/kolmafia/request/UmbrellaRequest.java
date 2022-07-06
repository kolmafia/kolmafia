package net.sourceforge.kolmafia.request;

import java.util.Arrays;
import net.sourceforge.kolmafia.preferences.Preferences;

public class UmbrellaRequest extends GenericRequest {
  public UmbrellaRequest() {
    super("choice.php");
  }

  public static enum UmbrellaMode {
    // You attempt to operate the umbrella normally, which causes it to explode into a howling mass
    // of chaos.
    BROKEN(1, "broken", "howling mass of chaos", "ml"),
    // You carefully open the umbrella and point it towards the stuff in front of you.
    FORWARD(2, "forward-facing", "the stuff in front of you", "dr"),
    // You open the umbrella and let it dangle by the handle.
    BUCKET(3, "bucket style", "dangle by the handle", "item"),
    // You open the umbrella so hard it pops inside out.
    PITCHFORK(4, "pitchfork style", "pops inside out", "weapon"),
    // You open the umbrella inside, which causes it to become cursed. Then you begin twirling it,
    // which evenly distributes the curse throughout the umbrella's metal skeleton.
    TWIRL(5, "constantly twirling", "evenly distributes the curse", "spell"),
    // You open the umbrella, step inside it, and close it.
    COCOON(6, "cocoon", "step inside it", "nc");

    private final int id;
    private final String name;
    private final String descriptionSnippet;
    private final String shorthand;

    UmbrellaMode(int id, String name, String descriptionSnippet, String shorthand) {
      this.id = id;
      this.name = name;
      this.descriptionSnippet = descriptionSnippet;
      this.shorthand = shorthand;
    }

    public void set() {
      Preferences.setString("umbrellaState", this.getName());
    }

    public static UmbrellaMode find(final String name) {
      if (name.equals("twirling")) {
        return UmbrellaMode.TWIRL;
      }

      return Arrays.stream(values())
          .filter(m -> m.getName().startsWith(name))
          .findAny()
          .orElse(null);
    }

    public static UmbrellaMode findByShortHand(final String shorthand) {
      return Arrays.stream(values())
          .filter(m -> m.getShorthand().equalsIgnoreCase(shorthand))
          .findAny()
          .orElse(null);
    }

    public int getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getDescriptionSnippet() {
      return descriptionSnippet;
    }

    public String getShorthand() {
      return shorthand;
    }
  }

  public static void parseUmbrella(final String urlString, final String responseText) {
    if (!urlString.contains("whichchoice=1466")) {
      return;
    }

    for (UmbrellaMode mode : UmbrellaMode.values()) {
      if (urlString.contains("option=" + mode.getId())
          && responseText.contains(mode.getDescriptionSnippet())) {
        mode.set();
        return;
      }
    }
  }
}
