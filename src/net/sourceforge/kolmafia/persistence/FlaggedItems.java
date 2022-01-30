package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.utilities.LogStream;

public class FlaggedItems {
  private static final File itemFlagsFile = new File(KoLConstants.DATA_LOCATION, "itemflags.txt");

  public static final String[] COMMON_JUNK = {
    // Items which usually get autosold by people, regardless of
    // the situation.  This includes the various meat combinables,
    // sewer items, and stat boosters.

    "meat stack",
    "dense meat stack",
    "twinkly powder",
    "seal-clubbing club",
    "seal tooth",
    "helmet turtle",
    "pasta spoon",
    "ravioli hat",
    "disco mask",
    "mariachi pants",
    "moxie weed",
    "strongness elixir",
    "magicalness-in-a-can",
    "enchanted barbell",
    "concentrated magicalness pill",
    "giant moxie weed",
    "extra-strength strongness elixir",
    "jug-o-magicalness",
    "suntan lotion of moxiousness",

    // Next, some common drops in low level areas that are farmed
    // for other reasons other than those items.

    "Mad Train wine",
    "ice-cold fotie",
    "ice-cold Willer",
    "ice-cold Sir Schlitz",
    "bowl of cottage cheese",
    "Knob Goblin firecracker",
    "Knob Goblin pants",
    "Knob Goblin scimitar",
    "viking helmet",
    "bar skin",
    "spooky shrunken head",
    "dried face",
    "barskin hat",
    "spooky stick",
    "batgut",
    "bat guano",
    "ratgut",
    "briefcase",
    "taco shell",
    "uncooked chorizo",
    "Gnollish plunger",
    "gnoll teeth",
    "gnoll lips",
    "Gnollish toolbox",

    // Next, some common drops in medium level areas that are also
    // farmed for other reasons beyond these items.

    "hill of beans",
    "Knob Goblin love potion",
    "Knob Goblin steroids",
    "Imp Ale",
    "hot wing",
    "evil golden arch",
    "leather mask",
    "necklace chain",
    "hemp string",
    "piercing post",
    "phat turquoise bead",
    "carob chunks",
    "Feng Shui for Big Dumb Idiots",
    "crowbarrr",
    "sunken chest",
    "barrrnacle",
    "safarrri hat",
    "arrrgyle socks",
    "charrrm",
    "leotarrrd",
    "pirate pelvis",
    "grave robbing shovel",
    "ghuol ears",
    "ghuol egg",
    "ghuol guolash",
    "lihc eye",
    "mind flayer corpse",
    "royal jelly",
    "goat beard",
    "sabre teeth",
    "t8r tots",
    "pail",
    "Trollhouse cookies",
    "Spam Witch sammich",
    "white satin pants",
    "white chocolate chips",
    "catgut",
    "white snake skin",
    "mullet wig",

    // High level area item drops which tend to be autosold or
    // auto-used.

    "cocoa eggshell fragment",
    "glowing red eye",
    "amulet of extreme plot significance",
    "Penultimate Fantasy chest",
    "Warm Subject gift certificate",
    "disturbing fanfic",
    "probability potion",
    "procrastination potion",
    "Mick's IcyVapoHotness Rub"
  };
  public static final String[] SINGLETON_ITEMS = {
    "bugbear beanie",
    "bugbear bungguard",
    "filthy knitted dread sack",
    "filthy corduroys",
    "Orcish frat-paddle",
    "Orcish baseball cap",
    "Orcish cargo shorts",
    "Knob Goblin harem veil",
    "Knob Goblin harem pants",
    "Knob Goblin elite helm",
    "Knob Goblin elite polearm",
    "Knob Goblin elite pants",
    "eyepatch",
    "swashbuckling pants",
    "stuffed shoulder parrot",
    "Cloaca-Cola fatigues",
    "Cloaca-Cola helmet",
    "Cloaca-Cola shield",
    "Dyspepsi-Cola fatigues",
    "Dyspepsi-Cola helmet",
    "Dyspepsi-Cola shield",
    "bullet-proof corduroys",
    "round purple sunglasses",
    "reinforced beaded headband",
    "beer helmet",
    "distressed denim pants",
    "bejeweled pledge pin"
  };
  public static final String[] COMMON_MEMENTOS = {
    // Crimbo 2005/2006 accessories, if they're still around,
    // probably shouldn't be placed in the player's store.

    "tiny plastic Crimbo wreath",
    "tiny plastic Uncle Crimbo",
    "tiny plastic Crimbo elf",
    "tiny plastic sweet nutcracker",
    "tiny plastic Crimbo reindeer",
    "wreath-shaped Crimbo cookie",
    "bell-shaped Crimbo cookie",
    "tree-shaped Crimbo cookie",
    "candy stake",
    "spooky eggnog",
    "ancient unspeakable fruitcake",
    "gingerbread horror",
    "bat-shaped Crimboween cookie",
    "skull-shaped Crimboween cookie",
    "tombstone-shaped Crimboween cookie",
    "tiny plastic gift-wrapping vampire",
    "tiny plastic ancient yuletide troll",
    "tiny plastic skeletal reindeer",
    "tiny plastic Crimboween pentagram",
    "tiny plastic Scream Queen",
    "orange and black Crimboween candy",

    // Certain items tend to be used throughout an ascension, so
    // they probably shouldn't get sold, either.

    "sword behind inappropriate prepositions",
    "toy mercenary",

    // Collectible items should probably be sent to other players
    // rather than be autosold for no good reason.

    "stuffed cocoabo",
    "stuffed baby gravy fairy",
    "stuffed flaming gravy fairy",
    "stuffed frozen gravy fairy",
    "stuffed stinky gravy fairy",
    "stuffed spooky gravy fairy",
    "stuffed sleazy gravy fairy",
    "stuffed astral badger",
    "stuffed MagiMechTech MicroMechaMech",
    "stuffed hand turkey",
    "stuffed snowy owl",
    "stuffed scary death orb",
    "stuffed mind flayer",
    "stuffed undead elbow macaroni",
    "stuffed angry cow",
    "stuffed Cheshire bitten",
    "stuffed yo-yo",
    "rubber WWJD? bracelet",
    "rubber WWBD? bracelet",
    "rubber WWSPD? bracelet",
    "rubber WWtNSD? bracelet",
    "heart necklace",
    "spade necklace",
    "diamond necklace",
    "club necklace",
  };

  private FlaggedItems() {}

  private static void initializeList(final List<AdventureResult> model, final String[] defaults) {
    model.clear();
    AdventureResult item;

    for (int i = 0; i < defaults.length; ++i) {
      int itemId = ItemDatabase.getItemId(defaults[i]);
      if (itemId == -1) {
        continue;
      }
      item = ItemPool.get(itemId);
      if (!model.contains(item)) {
        model.add(item);
      }

      if (model == KoLConstants.singletonList && !KoLConstants.junkList.contains(item)) {
        KoLConstants.junkList.add(item);
      }
    }
  }

  public static final void initializeLists() {
    if (!FlaggedItems.itemFlagsFile.exists()) {
      FlaggedItems.initializeList(KoLConstants.junkList, FlaggedItems.COMMON_JUNK);
      FlaggedItems.initializeList(KoLConstants.singletonList, FlaggedItems.SINGLETON_ITEMS);
      FlaggedItems.initializeList(KoLConstants.mementoList, FlaggedItems.COMMON_MEMENTOS);

      KoLConstants.profitableList.clear();
      return;
    }

    AdventureResult item;
    try (BufferedReader reader = DataUtilities.getReader(FlaggedItems.itemFlagsFile)) {
      String line;
      List<AdventureResult> model = null;

      while ((line = reader.readLine()) != null) {
        if (line.equals("")) {
          continue;
        }

        if (line.startsWith(" > ")) {
          if (line.endsWith("junk")) {
            model = KoLConstants.junkList;
          } else if (line.endsWith("singleton")) {
            model = KoLConstants.singletonList;
          } else if (line.endsWith("mementos")) {
            model = KoLConstants.mementoList;
          } else if (line.endsWith("profitable")) {
            model = KoLConstants.profitableList;
          }

          if (model != null) {
            model.clear();
          }
        } else if (model != null && ItemDatabase.contains(line)) {
          int itemId = ItemDatabase.getItemId(line);
          item = ItemPool.get(itemId);

          if (!model.contains(item)) {
            model.add(item);
          }

          if (model == KoLConstants.singletonList && !KoLConstants.junkList.contains(item)) {
            KoLConstants.junkList.add(item);
          }
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static final void saveFlaggedItemList() {
    PrintStream ostream = LogStream.openStream(FlaggedItems.itemFlagsFile, true);

    ostream.println(" > junk");
    ostream.println();

    for (AdventureResult item : KoLConstants.junkList) {
      if (!KoLConstants.singletonList.contains(item)) {
        ostream.println(item.getName());
      }
    }

    ostream.println();
    ostream.println(" > singleton");
    ostream.println();

    for (AdventureResult item : KoLConstants.singletonList) {
      ostream.println(item.getName());
    }

    ostream.println();
    ostream.println(" > mementos");
    ostream.println();

    for (AdventureResult item : KoLConstants.mementoList) {
      ostream.println(item.getName());
    }

    ostream.println();
    ostream.println(" > profitable");
    ostream.println();

    for (AdventureResult item : KoLConstants.profitableList) {
      ostream.println(item.getCount() + " " + item.getName());
    }

    ostream.close();
  }
}
