package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
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

  private static final Object[][] BASE_SUSHI = {
    {IntegerPool.get(1), "beefy nigiri"},
    {IntegerPool.get(2), "glistening nigiri"},
    {IntegerPool.get(3), "slick nigiri"},
    {IntegerPool.get(4), "beefy maki"},
    {IntegerPool.get(5), "glistening maki"},
    {IntegerPool.get(6), "slick maki"},
    {IntegerPool.get(7), "bento box"},
  };

  private static String idToName(final int id) {
    for (int i = 0; i < BASE_SUSHI.length; ++i) {
      Object[] sushi = BASE_SUSHI[i];
      if (((Integer) sushi[0]).intValue() == id) {
        return (String) sushi[1];
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
    for (int i = 0; i < BASE_SUSHI.length; ++i) {
      Object[] sushi = BASE_SUSHI[i];
      if (name.equals(sushi[1])) {
        return ((Integer) sushi[0]).intValue();
      }
    }

    // Check for filled sushi
    for (int i = 0; i < FILLING1.length; ++i) {
      Object[] sushi = FILLING1[i];
      if (name.contains((String) sushi[0])) {
        return SushiRequest.nameToId((String) sushi[1]);
      }
    }

    // Check for topped sushi
    for (int i = 0; i < TOPPING.length; ++i) {
      Object[] sushi = TOPPING[i];
      if (!name.startsWith((String) sushi[0])) {
        continue;
      }
      int index = name.indexOf(" ");
      if (index != -1) {
        return SushiRequest.nameToId(name.substring(index + 1));
      }
    }

    return -1;
  }

  private static final Object[][] TOPPING = {
    {"salty", IntegerPool.get(ItemPool.SEA_SALT_CRYSTAL)},
    {"magical", IntegerPool.get(ItemPool.DRAGONFISH_CAVIAR)},
    {"electric", IntegerPool.get(ItemPool.EEL_SAUCE)},
    {"Yuletide", IntegerPool.get(ItemPool.PEPPERMINT_EEL_SAUCE)},
  };

  private static String toppingToName(final String baseName, final int topping) {
    for (int i = 0; i < TOPPING.length; ++i) {
      Object[] sushi = TOPPING[i];
      if (topping == ((Integer) sushi[1]).intValue()) {
        return sushi[0] + " " + baseName;
      }
    }

    return baseName;
  }

  private static int nameToTopping(final String name) {
    for (int i = 0; i < TOPPING.length; ++i) {
      Object[] sushi = TOPPING[i];
      if (name.startsWith((String) sushi[0])) {
        return ((Integer) sushi[1]).intValue();
      }
    }

    return -1;
  }

  private static final Object[][] FILLING1 = {
    {"giant dragon roll", "beefy maki", IntegerPool.get(ItemPool.SEA_CUCUMBER)},
    {"musclebound rabbit roll", "beefy maki", IntegerPool.get(ItemPool.SEA_CARROT)},
    {"python roll", "beefy maki", IntegerPool.get(ItemPool.SEA_AVOCADO)},
    {"Jack LaLanne roll", "beefy maki", IntegerPool.get(ItemPool.SEA_RADISH)},
    {"jacked Santa roll", "beefy maki", IntegerPool.get(ItemPool.GREEN_AND_RED_BEAN)},
    {"wise dragon roll", "glistening maki", IntegerPool.get(ItemPool.SEA_CUCUMBER)},
    {"white rabbit roll", "glistening maki", IntegerPool.get(ItemPool.SEA_CARROT)},
    {"ancient serpent roll", "glistening maki", IntegerPool.get(ItemPool.SEA_AVOCADO)},
    {"wizened master roll", "glistening maki", IntegerPool.get(ItemPool.SEA_RADISH)},
    {"omniscient Santa roll", "glistening maki", IntegerPool.get(ItemPool.GREEN_AND_RED_BEAN)},
    {"tricky dragon roll", "slick maki", IntegerPool.get(ItemPool.SEA_CUCUMBER)},
    {"sneaky rabbit roll", "slick maki", IntegerPool.get(ItemPool.SEA_CARROT)},
    {"slippery snake roll", "slick maki", IntegerPool.get(ItemPool.SEA_AVOCADO)},
    {"eleven oceans roll", "slick maki", IntegerPool.get(ItemPool.SEA_RADISH)},
    {"sneaky Santa roll", "slick maki", IntegerPool.get(ItemPool.GREEN_AND_RED_BEAN)},
  };

  private static String filling1ToName(final String baseName, final int filling1) {
    for (int i = 0; i < FILLING1.length; ++i) {
      Object[] sushi = FILLING1[i];
      if (baseName.equals(sushi[1]) && filling1 == ((Integer) sushi[2]).intValue()) {
        return (String) sushi[0];
      }
    }

    return baseName;
  }

  private static int nameToFilling1(final String name) {
    for (int i = 0; i < FILLING1.length; ++i) {
      Object[] sushi = FILLING1[i];
      if (name.contains((String) sushi[0])) {
        return ((Integer) sushi[2]).intValue();
      }
    }

    return -1;
  }

  private static final Object[][] VEGGIE = {
    {"tempura avocado", IntegerPool.get(ItemPool.TEMPURA_AVOCADO)},
    {"tempura broccoli", IntegerPool.get(ItemPool.TEMPURA_BROCCOLI)},
    {"tempura carrot", IntegerPool.get(ItemPool.TEMPURA_CARROT)},
    {"tempura cauliflower", IntegerPool.get(ItemPool.TEMPURA_CAULIFLOWER)},
    {"tempura cucumber", IntegerPool.get(ItemPool.TEMPURA_CUCUMBER)},
    {"tempura green and red bean", IntegerPool.get(ItemPool.TEMPURA_GREEN_AND_RED_BEAN)},
    {"tempura radish", IntegerPool.get(ItemPool.TEMPURA_RADISH)},
  };

  private static String veggieToName(final String baseName, final int veggie) {
    for (int i = 0; i < VEGGIE.length; ++i) {
      Object[] sushi = VEGGIE[i];
      if (veggie == ((Integer) sushi[1]).intValue()) {
        return sushi[0] + " " + baseName;
      }
    }

    return baseName;
  }

  private static int nameToVeggie(final String name) {
    for (int i = 0; i < VEGGIE.length; ++i) {
      Object[] sushi = VEGGIE[i];
      if (name.startsWith((String) sushi[0])) {
        return ((Integer) sushi[1]).intValue();
      }
    }

    return -1;
  }

  private static final Object[][] DIPPING = {
    {"anemone sauce", IntegerPool.get(ItemPool.ANEMONE_SAUCE)},
    {"eel sauce", IntegerPool.get(ItemPool.EEL_SAUCE)},
    {"inky squid sauce", IntegerPool.get(ItemPool.INKY_SQUID_SAUCE)},
    {"Mer-kin weaksauce", IntegerPool.get(ItemPool.MERKIN_WEAKSAUCE)},
    {"peanut sauce", IntegerPool.get(ItemPool.PEANUT_SAUCE)},
    {"peppermint eel sauce", IntegerPool.get(ItemPool.PEPPERMINT_EEL_SAUCE)},
  };

  private static String dippingToName(final String baseName, final int dipping) {
    for (int i = 0; i < DIPPING.length; ++i) {
      Object[] sushi = DIPPING[i];
      if (dipping == ((Integer) sushi[1]).intValue()) {
        return baseName + " with " + sushi[0];
      }
    }

    return baseName;
  }

  private static int nameToDipping(final String name) {
    for (int i = 0; i < DIPPING.length; ++i) {
      Object[] sushi = DIPPING[i];
      if (name.endsWith((String) sushi[0])) {
        return ((Integer) sushi[1]).intValue();
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

    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(-1, name);
    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult ingredient = ingredients[i];
      ResultProcessor.processResult(ingredient.getInstance(-1 * ingredient.getCount()));
    }

    if (updateFullness) {
      int fullness = ConsumablesDatabase.getFullness(name);
      if (fullness > 0 && !responseText.contains("Fullness"))
      // ResultProcessor will handle fullness gain if fullness display is enabled
      {
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

    StringBuffer buf = new StringBuffer();
    buf.append(name.contains("bento") ? "Pack" : "Roll");
    buf.append(" and eat ");
    buf.append(name);
    buf.append(" from ");

    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(-1, name);
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
