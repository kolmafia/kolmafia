package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.Modifiers.Modifier;
import net.sourceforge.kolmafia.Modifiers.ModifierList;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LatteRequest extends GenericRequest {
  private static final Pattern REFILL_PATTERN = Pattern.compile("You've got <b>(\\d+)</b> refill");
  private static final Pattern LINE_PATTERN = Pattern.compile("<tr style=.*?</tr>", Pattern.DOTALL);
  private static final Pattern INPUT_PATTERN =
      Pattern.compile("name=\\\"l(\\d)\\\" (?:|checked) value=\\\"(.*?)\\\">\\s(.*?)\\s</td>");
  private static final Pattern RESULT_PATTERN =
      Pattern.compile("You get your mug filled with a delicious (.*?) Latte (.*?)\\.</span>");

  public static class Latte {
    public final String ingredient;
    public final String location;
    public final String first;
    public final String second;
    public final String third;
    public final String modifier;
    public final String discovery;

    public Latte(
        String ingredient,
        String location,
        String first,
        String second,
        String third,
        String modifier,
        String discovery) {
      this.ingredient = ingredient;
      this.location = location;
      this.first = first;
      this.second = second;
      this.third = third;
      this.modifier = modifier;
      this.discovery = discovery;
    }
  }

  private static final Latte[] LATTE =
      new Latte[] {
        // Ingredient, Location, First group name, second group name, third group name, modifier
        // string, discovery text
        new Latte(
            "ancient",
            "The Mouldering Mansion",
            "Ancient exotic spiced",
            "ancient/spicy",
            "with ancient spice",
            "Spooky Damage: 50",
            "urn full of ancient spices"),
        new Latte(
            "basil",
            "The Overgrown Lot",
            "Basil and",
            "basil",
            "with basil",
            "HP Regen Min: 5, HP Regen Max: 5",
            "clump of wild basil"),
        new Latte(
            "belgian",
            "Whitey's Grove",
            "Belgian vanilla",
            "Belgian vanilla",
            "with a shot of Belgian vanilla",
            "Muscle Percent: 20, Mysticality Percent: 20, Moxie Percent: 20",
            "a large vanilla bean pod"),
        new Latte(
            "bug-thistle",
            "The Bugbear Pen",
            "Bug-thistle",
            "bug-thistle",
            "with a sprig of bug-thistle",
            "Mysticality: 20",
            "patch of bug-thistle"),
        new Latte(
            "butternut",
            "Madness Bakery",
            "Butternutty",
            "butternut-spice",
            "with butternut",
            "Spell Damage: 10",
            "find a butternut squash"),
        new Latte(
            "cajun",
            "The Black Forest",
            "Cajun",
            "cajun spice",
            "with cajun spice",
            "Meat Drop: 40",
            "Cayenne you add this to the menu"),
        new Latte(
            "carrot",
            "The Dire Warren",
            "Carrot",
            "carrot",
            "with carrot",
            "Item Drop: 20",
            "bunch of carrots"),
        new Latte(
            "carrrdamom",
            "Barrrney's Barrr",
            "Carrrdamom-scented",
            "carrrdamom",
            "with carrrdamom",
            "MP Regen Min: 4, MP Regen Max: 6",
            "A carrrdamom"),
        new Latte(
            "chalk",
            "The Haunted Billiards Room",
            "Blue chalk and",
            "blue chalk",
            "with blue chalk",
            "Cold Damage: 25",
            "box of blue chalk cubes"),
        new Latte(
            "chili",
            "The Haunted Kitchen",
            "Chili",
            "chili seeds",
            "with a kick",
            "Hot Resistance: 3",
            "jar of chili seeds"),
        new Latte(
            "cinnamon",
            null,
            "Cinna-",
            "cinnamon",
            "with a shake of cinnamon",
            "Experience (Moxie): 1, Moxie Percent: 5, Pickpocket Chance: 5",
            null),
        new Latte(
            "cloves",
            "The Sleazy Back Alley",
            "Cloven",
            "cloves",
            "with a puff of cloves",
            "Stench Resistance: 3",
            "little tin of ground cloves"),
        new Latte(
            "coal",
            "The Haunted Boiler Room",
            "Coal-boiled",
            "coal",
            "with a lump of hot coal",
            "Hot Damage: 25",
            "brazier of burning coals"),
        new Latte(
            "cocoa",
            "The Icy Peak",
            "Cocoa",
            "cocoa powder",
            "mocha loca",
            "Cold Resistance: 3",
            "packet of cocoa powder"),
        new Latte(
            "diet",
            "Battlefield (No Uniform)",
            "Diet",
            "diet soda",
            "with diet soda syrup",
            "Initiative: 50",
            "jug of diet soda syrup"),
        new Latte(
            "dwarf",
            "Itznotyerzitz Mine",
            "Dwarf creamed",
            "dwarf cream",
            "with dwarf cream",
            "Muscle: 30",
            "milking a stalactite"),
        new Latte(
            "dyspepsi",
            "Battlefield (Dyspepsi Uniform)",
            "Dyspepsi-flavored",
            "Dyspepsi",
            "with a shot of Dyspepsi syrup",
            "Initiative: 25",
            "pure Dyspepsi syrup"),
        new Latte(
            "filth",
            "The Feeding Chamber",
            "Filthy",
            "filth milk",
            "with filth milk",
            "Damage Reduction: 20",
            "filth milk"),
        new Latte(
            "flour",
            "The Road to the White Citadel",
            "Floured",
            "white flour",
            "dusted with flour",
            "Sleaze Resistance: 3",
            "bag of all-purpose flour"),
        new Latte(
            "fungus",
            "The Fungal Nethers",
            "Fungal",
            "fungus",
            "with fungal scrapings",
            "Maximum MP: 30",
            "patch of mushrooms"),
        new Latte(
            "grass",
            "The Hidden Park",
            "Fresh grass and",
            "fresh grass",
            "with fresh-cut grass",
            "Experience: 3",
            "pile of fresh lawn clippings"),
        new Latte(
            "greasy",
            "Cobb's Knob Barracks",
            "Extra-greasy",
            "hot sausage",
            "with extra gristle",
            "Muscle Percent: 50",
            "big greasy sausage"),
        new Latte(
            "greek",
            "Wartime Frat House (Hippy Disguise)",
            "Greek spice",
            "greek spice",
            "with greek spice",
            "Sleaze Damage: 25",
            "ΣΠΙΣΗΣ"),
        new Latte(
            "grobold",
            "The Old Rubee Mine",
            "Grobold rum and",
            "grobold rum",
            "with a shot of grobold rum",
            "Sleaze Damage: 25",
            "stash of grobold rum"),
        new Latte(
            "guarna",
            "The Bat Hole Entrance",
            "Guarna and",
            "guarna",
            "infused with guarna",
            "Adventures: 4",
            "patch of guarana plants"),
        new Latte(
            "gunpowder",
            "1st Floor, Shiawase-Mitsuhama Building",
            "Gunpowder and",
            "gunpowder",
            "with gunpowder",
            "Weapon Damage: 50",
            "jar of gunpowder"),
        new Latte(
            "healing",
            "The Daily Dungeon",
            "Extra-healthy",
            "health potion",
            "with a shot of healing elixir",
            "HP Regen Min: 10, HP Regen Max: 20",
            "jug full of red syrup"),
        new Latte(
            "hellion",
            "The Dark Neck of the Woods",
            "Hellish",
            "hellion",
            "with hellion",
            "PvP Fights: 6",
            "small pile of hellion cubes"),
        new Latte(
            "hobo",
            "Hobopolis Town Square",
            "Hobo-spiced",
            "hobo spice",
            "with hobo spice",
            "Damage Absorption: 50",
            "Hobo Spices"),
        new Latte(
            "ink",
            "The Haunted Library",
            "Inky",
            "ink",
            "with ink",
            "Combat Rate: -10",
            "large bottle of india ink"),
        new Latte(
            "kombucha",
            "Wartime Hippy Camp (Frat Disguise)",
            "Kombucha-infused",
            "kombucha",
            "with a kombucha chaser",
            "Stench Damage: 25",
            "bottle of homemade kombucha"),
        new Latte(
            "lihc",
            "The Defiled Niche",
            "Lihc-licked",
            "lihc saliva",
            "with lihc spit",
            "Spooky Damage: 25",
            "collect some of the saliva in a jar"),
        new Latte(
            "lizard",
            "The Arid, Extra-Dry Desert",
            "Lizard milk and",
            "lizard milk",
            "with lizard milk",
            "MP Regen Min: 5, MP Regen Max: 15",
            "must be lizard milk"),
        new Latte(
            "mega",
            "Cobb's Knob Laboratory",
            "Super-greasy",
            "mega sausage",
            "with super gristle",
            "Moxie Percent: 50",
            "biggest sausage you've ever seen"),
        new Latte(
            "mold",
            "The Unquiet Garves",
            "Moldy",
            "grave mold",
            "with grave mold",
            "Spooky Damage: 20",
            "covered with mold"),
        new Latte(
            "msg",
            "The Briniest Deepests",
            "MSG-Laced",
            "MSG",
            "with flavor",
            "Critical Hit Percent: 15",
            "pure MSG from the ocean"),
        new Latte(
            "noodles",
            "The Haunted Pantry",
            "Carb-loaded",
            "macaroni",
            "with extra noodles",
            "Maximum HP: 20",
            "espresso-grade noodles"),
        new Latte(
            "norwhal",
            "The Ice Hole",
            "Norwhal milk and",
            "norwhal milk",
            "with norwhal milk",
            "Maximum HP Percent: 200",
            "especially Nordic flavor to it"),
        new Latte(
            "oil",
            "The Old Landfill",
            "Motor oil and",
            "motor oil",
            "with motor oil",
            "Sleaze Damage: 20",
            "puddle of old motor oil"),
        new Latte(
            "paint",
            "The Haunted Gallery",
            "Oil-paint and",
            "oil paint",
            "with oil paint",
            "Cold Damage: 5, Hot Damage: 5, Sleaze Damage: 5, Spooky Damage: 5, Stench Damage: 5",
            "large painter's pallette"),
        new Latte(
            "paradise",
            "The Stately Pleasure Dome",
            "Paradise milk",
            "paradise milk",
            "with milk of paradise",
            "Muscle: 20, Mysticality: 20, Moxie: 20",
            "Milk of Paradise"),
        new Latte(
            "pumpkin",
            null,
            "Autumnal",
            "pumpkin spice",
            "with a hint of autumn",
            "Experience (Mysticality): 1, Mysticality Percent: 5, Spell Damage: 5",
            null),
        new Latte(
            "rawhide",
            "The Spooky Forest",
            "Rawhide",
            "rawhide",
            "with rawhide",
            "Familiar Weight: 5",
            "stash of rawhide dog chews"),
        new Latte(
            "rock",
            "The Brinier Deepers",
            "Extra-salty",
            "rock salt",
            "with rock salt",
            "Critical Hit Percent: 10",
            "large salt deposits"),
        new Latte(
            "salt",
            "The Briny Deeps",
            "Salted",
            "salt",
            "with salt",
            "Critical Hit Percent: 5",
            "distill some of the salt"),
        new Latte(
            "sandalwood",
            "Noob Cave",
            "Sandalwood-infused",
            "sandalwood splinter",
            "with sandalwood splinters",
            "Muscle: 5, Mysticality: 5, Moxie: 5",
            "made of sandalwood"),
        new Latte(
            "sausage",
            "Cobb's Knob Kitchens",
            "Greasy",
            "sausage",
            "with gristle",
            "Mysticality Percent: 50",
            "full of sausages"),
        new Latte(
            "space",
            "The Hole in the Sky",
            "Space pumpkin and",
            "space pumpkin",
            "with space pumpkin juice",
            "Muscle: 10, Mysticality: 10, Moxie: 10",
            "some kind of brown powder"),
        new Latte(
            "squamous",
            "The Caliginous Abyss",
            "Squamous-salted",
            "squamous",
            "with squamous salt",
            "Spooky Resistance: 3",
            "break off a shard"),
        new Latte(
            "squash",
            "The Copperhead Club",
            "Spaghetti-squashy",
            "spaghetti squash spice",
            "with extra squash",
            "Spell Damage: 20",
            "steal a spaghetti squash"),
        new Latte(
            "teeth",
            "The VERY Unquiet Garves",
            "Teeth",
            "teeth",
            "with teeth in it",
            "Spooky Damage: 25, Weapon Damage: 25",
            "handful of loose teeth"),
        new Latte(
            "vanilla",
            null,
            "Vanilla",
            "vanilla",
            "with a shot of vanilla",
            "Experience (Muscle): 1, Muscle Percent: 5, Weapon Damage Percent: 5",
            null),
        new Latte(
            "venom",
            "The Middle Chamber",
            "Envenomed",
            "asp venom",
            "with extra poison",
            "Weapon Damage: 25",
            "wring the poison out of it into a jar"),
        new Latte(
            "vitamins",
            "The Dark Elbow of the Woods",
            "Fortified",
            "vitamin",
            "enriched with vitamins",
            "Experience (familiar): 3",
            "specifically vitamins G, L, P, and W"),
        new Latte(
            "wing",
            "The Dark Heart of the Woods",
            "Hot wing and",
            "hot wing",
            "with a hot wing in it",
            "Combat Rate: 10",
            "plate of hot wings"),
      };

  private static final Map<Latte, String[]> radio = new HashMap<>();

  public LatteRequest() {
    super("choice.php");
  }

  public static void refill(final String first, final String second, final String third) {
    Latte[] ingredients = new Latte[3];

    for (Latte latte : LATTE) {
      if (latte.ingredient.equals(first)) {
        ingredients[0] = latte;
      }
      if (latte.ingredient.equals(second)) {
        ingredients[1] = latte;
      }
      if (latte.ingredient.equals(third)) {
        ingredients[2] = latte;
      }
    }

    for (int i = 0; i < 3; ++i) {
      if (ingredients[i] == null) {
        String message = null;
        switch (i) {
          case 0:
            message =
                "Cannot find ingredient "
                    + first
                    + ". Use 'latte unlocked' to see available ingredients.";
            break;
          case 1:
            message =
                "Cannot find ingredient "
                    + second
                    + ". Use 'latte unlocked' to see available ingredients.";
            break;
          case 2:
            message =
                "Cannot find ingredient "
                    + third
                    + ". Use 'latte unlocked' to see available ingredients.";
            break;
        }
        KoLmafia.updateDisplay(MafiaState.ERROR, message);
        continue;
      }

      String ingredient = ingredients[i].ingredient;
      if (!Preferences.getString("latteUnlocks").contains(ingredient)) {
        String message =
            "Ingredient "
                + ingredient
                + " is not unlocked. Use 'latte unlocks' to see how to unlock it.";
        KoLmafia.updateDisplay(MafiaState.ERROR, message);
      }
    }

    if (!KoLmafia.permitsContinue()) {
      return;
    }

    // Get unlocks and choice options
    GenericRequest latteRequest = new GenericRequest("main.php?latte=1", false);
    RequestThread.postRequest(latteRequest);
    LatteRequest.parseChoiceOptions(latteRequest.responseText);

    // Refill Latte
    String message =
        "Filling mug with "
            + ingredients[0].first
            + " "
            + ingredients[1].second
            + " Latte "
            + ingredients[2].third
            + ".";
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    String l1 = radio.get(ingredients[0])[0];
    String l2 = radio.get(ingredients[1])[1];
    String l3 = radio.get(ingredients[2])[2];

    GenericRequest fillRequest = new GenericRequest("choice.php");
    fillRequest.addFormField("whichchoice", "1329");
    fillRequest.addFormField("option", "1");
    fillRequest.addFormField("l1", l1);
    fillRequest.addFormField("l2", l2);
    fillRequest.addFormField("l3", l3);
    RequestThread.postRequest(fillRequest);
  }

  public static final void listUnlocks(final boolean all) {
    StringBuilder output = new StringBuilder();
    output.append("<table border=2 cols=3>");
    output.append("<tr>");
    output.append("<th>Ingredient</th>");
    output.append("<th>Unlock</th>");
    output.append("<th>Modifier</th>");
    output.append("</tr>");

    for (Latte latte : LATTE) {
      boolean unlocked = Preferences.getString("latteUnlocks").contains(latte.ingredient);
      if (all || unlocked) {
        output.append("<tr>");
        output.append("<td>");
        output.append(latte.ingredient);
        output.append("</td>");
        output.append("<td>");
        if (unlocked) {
          output.append("unlocked");
        } else {
          output.append("Unlock in ");
          output.append(latte.location);
        }
        output.append("</td>");
        output.append("<td>");
        output.append(latte.modifier);
        output.append("</td>");
        output.append("</tr>");
      }
    }
    output.append("</table>");

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=1329")) {
      return;
    }

    if (!urlString.contains("option=1")) {
      return;
    }

    // Find Latte result
    Matcher matcher = RESULT_PATTERN.matcher(responseText);
    if (matcher.find()) {
      String first = null;
      String second = null;
      String third = null;
      String[] mods = new String[3];
      String start = matcher.group(1).trim();
      String middle = null;
      String end = matcher.group(2).trim();

      for (Latte latte : LATTE) {
        if (start.startsWith(latte.first)) {
          mods[0] = latte.modifier;
          first = latte.first;
          middle = start.replace(latte.first, "").trim();
          break;
        }
      }

      for (Latte latte : LATTE) {
        if (middle.equals(latte.second)) {
          mods[1] = latte.modifier;
          second = latte.second;
          if (third != null) {
            break;
          }
        }
        if (end.equals(latte.third)) {
          mods[2] = latte.modifier;
          third = latte.third;
          if (second != null) {
            break;
          }
        }
      }

      String message = "Filled your mug with " + first + " " + second + " Latte " + third + ".";
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);

      ModifierList modList = new ModifierList();
      for (int i = 0; i < 3; ++i) {
        ModifierList addModList = Modifiers.splitModifiers(mods[i]);
        for (Modifier modifier : addModList) {
          modList.addToModifier(modifier);
        }
      }

      Preferences.setString("latteModifier", modList.toString());
      Modifiers.overrideModifier("Item:[" + ItemPool.LATTE_MUG + "]", modList.toString());
      KoLCharacter.recalculateAdjustments();
      KoLCharacter.updateStatus();
      Preferences.increment("_latteRefillsUsed", 1, 3, false);
      Preferences.setBoolean("_latteBanishUsed", false);
      Preferences.setBoolean("_latteCopyUsed", false);
      Preferences.setBoolean("_latteDrinkUsed", false);
    }
  }

  public static final void parseFight(final String location, final String responseText) {
    if (location == null || responseText == null) {
      return;
    }

    for (Latte latte : LATTE) {
      if (latte.location == null) {
        continue;
      }
      if (location.equals(latte.location)) {
        if (responseText.contains(latte.discovery)) {
          String pref = Preferences.getString("latteUnlocks");
          String ingredient = latte.ingredient;
          if (!pref.contains(ingredient)) {
            Preferences.setString("latteUnlocks", pref + "," + ingredient);
            String message = "Unlocked " + ingredient + " for Latte.";
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }
        }
        break;
      }
    }
  }

  public static final void parseVisitChoice(final String text) {
    Matcher matcher = LatteRequest.REFILL_PATTERN.matcher(text);
    if (matcher.find()) {
      Preferences.setInteger("_latteRefillsUsed", 3 - StringUtilities.parseInt(matcher.group(1)));
    }
    matcher = LatteRequest.LINE_PATTERN.matcher(text);
    StringBuilder unlocks = new StringBuilder();
    String sep = "";
    while (matcher.find()) {
      String line = matcher.group(0);
      Matcher lineMatcher = LatteRequest.INPUT_PATTERN.matcher(line);
      if (lineMatcher.find()) {
        String description = lineMatcher.group(3).trim();
        for (Latte latte : LATTE) {
          if (description.equals(latte.first)) {
            if (!line.contains("&Dagger;")) {
              unlocks.append(sep);
              unlocks.append(latte.ingredient);
              sep = ",";
            }
            break;
          }
        }
      }
    }
    Preferences.setString("latteUnlocks", unlocks.toString());
  }

  public static final void parseChoiceOptions(final String text) {
    Matcher matcher = LatteRequest.REFILL_PATTERN.matcher(text);
    if (matcher.find()) {
      Preferences.setInteger("_latteRefillsUsed", 3 - StringUtilities.parseInt(matcher.group(1)));
    }

    matcher = LatteRequest.LINE_PATTERN.matcher(text);
    radio.clear();
    StringBuilder unlocks = new StringBuilder();
    String sep = "";
    while (matcher.find()) {
      String line = matcher.group(0);
      Matcher lineMatcher = LatteRequest.INPUT_PATTERN.matcher(line);
      while (lineMatcher.find()) {
        int button = StringUtilities.parseInt(lineMatcher.group(1));
        String value = lineMatcher.group(2);
        String description = lineMatcher.group(3).trim();
        for (int i = 0; i < LATTE.length; ++i) {
          Latte latte = LATTE[i];
          boolean matched =
              button == 1
                  ? description.equals(latte.first)
                  : button == 2
                      ? description.equals(latte.second)
                      : button == 3 ? description.equals(latte.third) : false;
          if (matched) {
            String[] buttons = radio.get(latte);
            if (buttons == null) {
              buttons = new String[3];
            }
            buttons[button - 1] = value;
            radio.put(latte, buttons);
            if (button == 1 && !line.contains("&Dagger;")) {
              unlocks.append(sep);
              unlocks.append(latte.ingredient);
              sep = ",";
            }
            break;
          }
        }
      }
    }
    Preferences.setString("latteUnlocks", unlocks.toString());
  }
}
