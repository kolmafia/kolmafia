package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.session.DreadScrollManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SushiRequest extends CreateItemRequest {
  private static final Pattern SUSHI_PATTERN = Pattern.compile("whichsushi=(\\d+)");
  private static final Pattern TOPPING_PATTERN = Pattern.compile("whichtopping=(\\d+)");
  private static final Pattern FILLING1_PATTERN = Pattern.compile("whichfilling1=(\\d+)");
  private static final Pattern VEGGIE_PATTERN = Pattern.compile("veggie=(\\d+)");
  private static final Pattern DIPPING_PATTERN = Pattern.compile("dipping=(\\d+)");

  private static final Pattern CONSUME_PATTERN = Pattern.compile("You eat the ([^.]*)\\.");

  public static final String[] SUSHI = {
    "beefy nigiri",
    "glistening nigiri",
    "slick nigiri",
    "beefy maki",
    "glistening maki",
    "slick maki",
    "ancient serpent roll",
    "giant dragon roll",
    "musclebound rabbit roll",
    "python roll",
    "slippery snake roll",
    "sneaky rabbit roll",
    "tricky dragon roll",
    "white rabbit roll",
    "wise dragon roll",
    "Jack LaLanne roll",
    "wizened master roll",
    "eleven oceans roll",
    "jacked Santa roll",
    "omniscient Santa roll",
    "sneaky Santa roll",
    "magical ancient serpent roll",
    "magical beefy maki",
    "magical giant dragon roll",
    "magical glistening maki",
    "magical musclebound rabbit roll",
    "magical python roll",
    "magical slick maki",
    "magical slippery snake roll",
    "magical sneaky rabbit roll",
    "magical tricky dragon roll",
    "magical white rabbit roll",
    "magical wise dragon roll",
    "magical Jack LaLanne roll",
    "magical wizened master roll",
    "magical eleven oceans roll",
    "magical jacked Santa roll",
    "magical omniscient Santa roll",
    "magical sneaky Santa roll",
    "salty ancient serpent roll",
    "salty beefy maki",
    "salty giant dragon roll",
    "salty glistening maki",
    "salty musclebound rabbit roll",
    "salty python roll",
    "salty slick maki",
    "salty slippery snake roll",
    "salty sneaky rabbit roll",
    "salty tricky dragon roll",
    "salty white rabbit roll",
    "salty wise dragon roll",
    "salty Jack LaLanne roll",
    "salty wizened master roll",
    "salty eleven oceans roll",
    "salty jacked Santa roll",
    "salty omniscient Santa roll",
    "salty sneaky Santa roll",
    "electric ancient serpent roll",
    "electric beefy maki",
    "electric giant dragon roll",
    "electric glistening maki",
    "electric musclebound rabbit roll",
    "electric python roll",
    "electric slick maki",
    "electric slippery snake roll",
    "electric sneaky rabbit roll",
    "electric tricky dragon roll",
    "electric white rabbit roll",
    "electric wise dragon roll",
    "electric Jack LaLanne roll",
    "electric wizened master roll",
    "electric eleven oceans roll",
    "electric jacked Santa roll",
    "electric omniscient Santa roll",
    "electric sneaky Santa roll",
    "Yuletide beefy maki",
    "Yuletide glistening maki",
    "Yuletide slick maki",
    "Yuletide ancient serpent roll",
    "Yuletide giant dragon roll",
    "Yuletide musclebound rabbit roll",
    "Yuletide python roll",
    "Yuletide slippery snake roll",
    "Yuletide sneaky rabbit roll",
    "Yuletide tricky dragon roll",
    "Yuletide white rabbit roll",
    "Yuletide wise dragon roll",
    "Yuletide Jack LaLanne roll",
    "Yuletide wizened master roll",
    "Yuletide eleven oceans roll",
    "Yuletide jacked Santa roll",
    "Yuletide omniscient Santa roll",
    "Yuletide sneaky Santa roll",
    "tempura avocado bento box with Mer-kin weaksauce",
    "tempura avocado bento box with anemone sauce",
    "tempura avocado bento box with eel sauce",
    "tempura avocado bento box with inky squid sauce",
    "tempura avocado bento box with peanut sauce",
    "tempura avocado bento box with peppermint eel sauce",
    "tempura broccoli bento box with Mer-kin weaksauce",
    "tempura broccoli bento box with anemone sauce",
    "tempura broccoli bento box with eel sauce",
    "tempura broccoli bento box with inky squid sauce",
    "tempura broccoli bento box with peanut sauce",
    "tempura broccoli bento box with peppermint eel sauce",
    "tempura carrot bento box with Mer-kin weaksauce",
    "tempura carrot bento box with anemone sauce",
    "tempura carrot bento box with eel sauce",
    "tempura carrot bento box with inky squid sauce",
    "tempura carrot bento box with peanut sauce",
    "tempura carrot bento box with peppermint eel sauce",
    "tempura cauliflower bento box with Mer-kin weaksauce",
    "tempura cauliflower bento box with anemone sauce",
    "tempura cauliflower bento box with eel sauce",
    "tempura cauliflower bento box with inky squid sauce",
    "tempura cauliflower bento box with peanut sauce",
    "tempura cauliflower bento box with peppermint eel sauce",
    "tempura cucumber bento box with Mer-kin weaksauce",
    "tempura cucumber bento box with anemone sauce",
    "tempura cucumber bento box with eel sauce",
    "tempura cucumber bento box with inky squid sauce",
    "tempura cucumber bento box with peanut sauce",
    "tempura cucumber bento box with peppermint eel sauce",
    "tempura green and red bean bento box with Mer-kin weaksauce",
    "tempura green and red bean bento box with anemone sauce",
    "tempura green and red bean bento box with eel sauce",
    "tempura green and red bean bento box with inky squid sauce",
    "tempura green and red bean bento box with peanut sauce",
    "tempura green and red bean bento box with peppermint eel sauce",
    "tempura radish bento box with Mer-kin weaksauce",
    "tempura radish bento box with anemone sauce",
    "tempura radish bento box with eel sauce",
    "tempura radish bento box with inky squid sauce",
    "tempura radish bento box with peanut sauce",
    "tempura radish bento box with peppermint eel sauce",
  };

  public static final String[] CANONICAL_SUSHI = new String[SUSHI.length];

  static {
    for (int i = 0; i < SUSHI.length; ++i) {
      CANONICAL_SUSHI[i] = StringUtilities.getCanonicalName(SUSHI[i]);
    }
  }

  public static String isSushiName(final String name) {
    String canonical = StringUtilities.getCanonicalName(name);
    for (int i = 0; i < CANONICAL_SUSHI.length; ++i) {
      if (CANONICAL_SUSHI[i].equals(name)) {
        return SUSHI[i];
      }
    }
    return null;
  }

  private record Base(int id, String name) {}

  private static final Base[] BASE_SUSHI = {
    new Base(1, "beefy nigiri"),
    new Base(2, "glistening nigiri"),
    new Base(3, "slick nigiri"),
    new Base(4, "beefy maki"),
    new Base(5, "glistening maki"),
    new Base(6, "slick maki"),
    new Base(7, "bento box"),
  };

  private static String idToName(final int id) {
    for (Base sushi : BASE_SUSHI) {
      if (sushi.id == id) {
        return sushi.name;
      }
    }

    return null;
  }

  private static int nameToId(final String name) {
    // Bento Boxes do not have special names based on veggie or dipping sauce
    if (name.contains("bento box")) {
      return 7;
    }

    // Check for base sushi
    for (Base sushi : BASE_SUSHI) {
      if (name.equals(sushi.name)) {
        return sushi.id;
      }
    }

    // Check for filled sushi
    for (Filling sushi : FILLING1) {
      if (name.contains(sushi.finalName)) {
        return SushiRequest.nameToId(sushi.baseName);
      }
    }

    // Check for topped sushi
    for (Topping sushi : TOPPING) {
      if (!name.startsWith(sushi.name)) {
        continue;
      }
      int index = name.indexOf(" ");
      if (index != -1) {
        return SushiRequest.nameToId(name.substring(index + 1));
      }
    }

    return -1;
  }

  private record Topping(String name, int id) {}

  private static final Topping[] TOPPING = {
    new Topping("salty", ItemPool.SEA_SALT_CRYSTAL),
    new Topping("magical", ItemPool.DRAGONFISH_CAVIAR),
    new Topping("electric", ItemPool.EEL_SAUCE),
    new Topping("Yuletide", ItemPool.PEPPERMINT_EEL_SAUCE),
  };

  private static String toppingToName(final String baseName, final int topping) {
    for (Topping sushi : TOPPING) {
      if (topping == sushi.id) {
        return sushi.name + " " + baseName;
      }
    }

    return baseName;
  }

  private static int nameToTopping(final String name) {
    for (Topping sushi : TOPPING) {
      if (name.startsWith(sushi.name)) {
        return sushi.id;
      }
    }

    return -1;
  }

  private record Filling(String finalName, String baseName, int id) {}

  private static final Filling[] FILLING1 = {
    new Filling("giant dragon roll", "beefy maki", ItemPool.SEA_CUCUMBER),
    new Filling("musclebound rabbit roll", "beefy maki", ItemPool.SEA_CARROT),
    new Filling("python roll", "beefy maki", ItemPool.SEA_AVOCADO),
    new Filling("Jack LaLanne roll", "beefy maki", ItemPool.SEA_RADISH),
    new Filling("jacked Santa roll", "beefy maki", ItemPool.GREEN_AND_RED_BEAN),
    new Filling("wise dragon roll", "glistening maki", ItemPool.SEA_CUCUMBER),
    new Filling("white rabbit roll", "glistening maki", ItemPool.SEA_CARROT),
    new Filling("ancient serpent roll", "glistening maki", ItemPool.SEA_AVOCADO),
    new Filling("wizened master roll", "glistening maki", ItemPool.SEA_RADISH),
    new Filling("omniscient Santa roll", "glistening maki", ItemPool.GREEN_AND_RED_BEAN),
    new Filling("tricky dragon roll", "slick maki", ItemPool.SEA_CUCUMBER),
    new Filling("sneaky rabbit roll", "slick maki", ItemPool.SEA_CARROT),
    new Filling("slippery snake roll", "slick maki", ItemPool.SEA_AVOCADO),
    new Filling("eleven oceans roll", "slick maki", ItemPool.SEA_RADISH),
    new Filling("sneaky Santa roll", "slick maki", ItemPool.GREEN_AND_RED_BEAN),
  };

  private static String filling1ToName(final String baseName, final int filling1) {
    for (Filling sushi : FILLING1) {
      if (baseName.equals(sushi.baseName) && filling1 == sushi.id) {
        return sushi.finalName;
      }
    }

    return baseName;
  }

  private static int nameToFilling1(final String name) {
    for (Filling sushi : FILLING1) {
      if (name.contains(sushi.finalName)) {
        return sushi.id;
      }
    }

    return -1;
  }

  private record Veggie(String name, int id) {}

  private static final Veggie[] VEGGIE = {
    new Veggie("tempura avocado", ItemPool.TEMPURA_AVOCADO),
    new Veggie("tempura broccoli", ItemPool.TEMPURA_BROCCOLI),
    new Veggie("tempura carrot", ItemPool.TEMPURA_CARROT),
    new Veggie("tempura cauliflower", ItemPool.TEMPURA_CAULIFLOWER),
    new Veggie("tempura cucumber", ItemPool.TEMPURA_CUCUMBER),
    new Veggie("tempura green and red bean", ItemPool.TEMPURA_GREEN_AND_RED_BEAN),
    new Veggie("tempura radish", ItemPool.TEMPURA_RADISH),
  };

  private static String veggieToName(final String baseName, final int veggie) {
    for (Veggie sushi : VEGGIE) {
      if (veggie == sushi.id) {
        return sushi.name + " " + baseName;
      }
    }

    return baseName;
  }

  private static int nameToVeggie(final String name) {
    for (Veggie sushi : VEGGIE) {
      if (name.startsWith(sushi.name)) {
        return sushi.id;
      }
    }

    return -1;
  }

  private record Dipping(String name, int id) {}

  private static final Dipping[] DIPPING = {
    new Dipping("anemone sauce", ItemPool.ANEMONE_SAUCE),
    new Dipping("eel sauce", ItemPool.EEL_SAUCE),
    new Dipping("inky squid sauce", ItemPool.INKY_SQUID_SAUCE),
    new Dipping("Mer-kin weaksauce", ItemPool.MERKIN_WEAKSAUCE),
    new Dipping("peanut sauce", ItemPool.PEANUT_SAUCE),
    new Dipping("peppermint eel sauce", ItemPool.PEPPERMINT_EEL_SAUCE),
  };

  private static String dippingToName(final String baseName, final int dipping) {
    for (Dipping sushi : DIPPING) {
      if (dipping == sushi.id) {
        return baseName + " with " + sushi.name;
      }
    }

    return baseName;
  }

  private static int nameToDipping(final String name) {
    for (Dipping sushi : DIPPING) {
      if (name.endsWith(sushi.name)) {
        return sushi.id;
      }
    }

    return -1;
  }

  private static String sushiName(
      final int id, final int topping, final int filling1, final int veggie, final int dipping) {
    String name = SushiRequest.idToName(id);

    if (name == null) {
      return "unknown";
    }

    if (id == 7) {
      // Bento Boxes
      if (veggie > 0) {
        name = SushiRequest.veggieToName(name, veggie);
      }

      if (dipping > 0) {
        name = SushiRequest.dippingToName(name, dipping);
      }
    } else {
      if (filling1 > 0) {
        name = SushiRequest.filling1ToName(name, filling1);
      }

      if (topping > 0) {
        name = SushiRequest.toppingToName(name, topping);
      }
    }

    return name;
  }

  public static final String sushiName(final String urlString) {
    Matcher matcher = SushiRequest.SUSHI_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      return null;
    }

    int id = StringUtilities.parseInt(matcher.group(1));
    int topping = 0;
    int filling1 = 0;
    int veggie = 0;
    int dipping = 0;

    matcher = SushiRequest.TOPPING_PATTERN.matcher(urlString);
    if (matcher.find()) {
      topping = StringUtilities.parseInt(matcher.group(1));
    }

    matcher = SushiRequest.FILLING1_PATTERN.matcher(urlString);
    if (matcher.find()) {
      filling1 = StringUtilities.parseInt(matcher.group(1));
    }

    matcher = SushiRequest.VEGGIE_PATTERN.matcher(urlString);
    if (matcher.find()) {
      veggie = StringUtilities.parseInt(matcher.group(1));
    }

    matcher = SushiRequest.DIPPING_PATTERN.matcher(urlString);
    if (matcher.find()) {
      dipping = StringUtilities.parseInt(matcher.group(1));
    }

    return SushiRequest.sushiName(id, topping, filling1, veggie, dipping);
  }

  public SushiRequest(Concoction conc) {
    super("sushi.php", conc);
    this.addFormField("action", "Yep.");

    String name = conc.getName();

    int sushi = SushiRequest.nameToId(name);
    if (sushi > 0) {
      this.addFormField("whichsushi", String.valueOf(sushi));
    }

    int topping = SushiRequest.nameToTopping(name);
    if (topping > 0) {
      this.addFormField("whichtopping", String.valueOf(topping));
    }

    int filling1 = SushiRequest.nameToFilling1(name);
    if (filling1 > 0) {
      this.addFormField("whichfilling1", String.valueOf(filling1));
    }

    int veggie = SushiRequest.nameToVeggie(name);
    if (veggie > 0) {
      this.addFormField("veggie", String.valueOf(veggie));
    }

    int dipping = SushiRequest.nameToDipping(name);
    if (dipping > 0) {
      this.addFormField("dipping", String.valueOf(dipping));
    }
  }

  @Override
  public boolean noCreation() {
    return true;
  }

  @Override
  public void run() {
    // Make sure a sushi-rolling mat is available.

    if (!KoLCharacter.hasSushiMat()) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "You need a sushi rolling mat installed in your kitchen in order to roll sushi.");
      return;
    }

    // Attempting to make the ingredients will pull the
    // needed items from the closet if they are missing.

    if (!this.makeIngredients()) {
      return;
    }

    for (int i = 1; i <= this.getQuantityNeeded(); ++i) {
      KoLmafia.updateDisplay(
          "Creating/consuming "
              + this.getName()
              + " ("
              + i
              + " of "
              + this.getQuantityNeeded()
              + ")...");
      super.run();
      SushiRequest.parseConsumption(this.getURLString(), this.responseText, false);
    }
  }

  public static void parseConsumption(
      final String location, final String responseText, final boolean updateFullness) {
    if (!location.startsWith("sushi.php")) {
      return;
    }

    // "That looks good, but you're way too full to eat it right
    // now."

    if (responseText.contains("too full to eat it")) {
      return;
    }

    String name = SushiRequest.sushiName(location);
    if (name == null) {
      return;
    }

    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(name);
    for (AdventureResult ingredient : ingredients) {
      ResultProcessor.processResult(ingredient.getInstance(-1 * ingredient.getCount()));
    }

    if (updateFullness) {
      int fullness = ConsumablesDatabase.getFullness(name);
      if (fullness > 0 && !responseText.contains("Fullness")) {
        // ResultProcessor will handle fullness gain if fullness display is enabled
        KoLCharacter.setFullness(KoLCharacter.getFullness() + fullness);
        KoLCharacter.updateStatus();
      }
    }

    // Eating it off of a fancy doily makes it even <i>more</i> delicious!
    if (responseText.contains("fancy doily")) {
      ResultProcessor.processItem(ItemPool.SUSHI_DOILY, -1);
    }

    // See if you had worktea in inventory when you ate this sushi
    DreadScrollManager.handleWorktea(responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("sushi.php")) {
      return false;
    }

    String name = SushiRequest.sushiName(urlString);
    if (name == null) {
      return false;
    }

    StringBuilder buf = new StringBuilder();
    buf.append(name.contains("bento") ? "Pack" : "Roll");
    buf.append(" and eat ");
    buf.append(name);
    buf.append(" from ");

    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(name);
    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult ingredient = ingredients[i];
      if (i > 0) {
        buf.append(", ");
      }

      buf.append(ingredient.getCount());
      buf.append(" ");
      buf.append(ingredient.getName());
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(buf.toString());

    return true;
  }
}
