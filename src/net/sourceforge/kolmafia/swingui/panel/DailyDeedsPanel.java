package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.ItemListenerRegistry;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.MomRequest;
import net.sourceforge.kolmafia.request.PottedTeaTreeRequest;
import net.sourceforge.kolmafia.request.PottedTeaTreeRequest.PottedTea;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.session.RabbitHoleManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager.Hat;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.widget.DisabledItemsComboBox;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DailyDeedsPanel extends Box implements Listener {
  public static final AdventureResult GREAT_PANTS = ItemPool.get(ItemPool.GREAT_PANTS, 1);
  public static final AdventureResult INFERNAL_SEAL_CLAW =
      ItemPool.get(ItemPool.INFERNAL_SEAL_CLAW, 1);
  public static final AdventureResult NAVEL_RING = ItemPool.get(ItemPool.NAVEL_RING, 1);
  public static final AdventureResult SNOW_SUIT = ItemPool.get(ItemPool.SNOW_SUIT, 1);
  public static final AdventureResult STAFF_OF_LIFE = ItemPool.get(ItemPool.STAFF_OF_LIFE, 1);
  public static final AdventureResult STAFF_OF_CHEESE = ItemPool.get(ItemPool.STAFF_OF_CHEESE, 1);
  public static final AdventureResult STAFF_OF_STEAK = ItemPool.get(ItemPool.STAFF_OF_STEAK, 1);
  public static final AdventureResult STAFF_OF_CREAM = ItemPool.get(ItemPool.STAFF_OF_CREAM, 1);

  private static final String comboBoxSizeString = "Available Hatter Buffs: BLAH";
  private static final String[] STRING_ARRAY = new String[0];

  /*
   * Built-in deeds. {Type, Name, ...otherArgs}
   * Type: one of { Command, Item, Skill, Special }
   *
   * NOTE: when adding a new built-in deed, also add an appropriate entry for it in getVersion and increment dailyDeedsVersion
   * in defaults.txt.
   */
  public static final String[][] BUILTIN_DEEDS = {
    {
      "Command",
      "Breakfast",
      "breakfastCompleted",
      "breakfast",
      "1",
      "Perform typical daily tasks - use 1/day items, visit 1/day locations like various clan furniture, use item creation skills, etc. Configurable in preferences.",
      "You have completed breakfast"
    },
    {
      "Command",
      "Daily Dungeon",
      "dailyDungeonDone",
      "adv * Daily Dungeon",
      "1",
      "Adventure in Daily Dungeon",
      "You have adventured in the Daily Dungeon"
    },
    {
      "Special", "Submit Spading Data",
    },
    {
      "Special", "Crimbo Tree",
    },
    {
      "Special", "Chips",
    },
    {
      "Item",
      "Library Card",
      "libraryCardUsed",
      "Library Card",
      "1",
      "40-50 stat gain of one of Mus/Myst/Mox, randomly chosen"
    },
    {
      "Special", "Telescope",
    },
    {
      "Special", "Ball Pit",
    },
    {
      "Special", "Styx Pixie",
    },
    {
      "Special", "VIP Pool",
    },
    {
      "Special", "April Shower",
    },
    {
      "Item",
      "Bag o' Tricks",
      "_bagOTricksUsed",
      "Bag o' Tricks",
      "1",
      "5 random current effects extended by 3 turns",
      "Bag o' Tricked used"
    },
    {
      "Item",
      "Legendary Beat",
      "_legendaryBeat",
      "Legendary Beat",
      "1",
      "+50% items, 20 turns",
      "Legendary Beat used"
    },
    {
      "Item",
      "Outrageous Sombrero",
      "outrageousSombreroUsed",
      "Outrageous Sombrero",
      "1",
      "+3% items, 5 turns",
      "Outrageous Sombrero used"
    },
    {
      "Special", "Friars",
    },
    {
      "Special", "Mom",
    },
    {
      "Special", "Skate Park",
    },
    {
      "Item",
      "Fishy Pipe",
      "_fishyPipeUsed",
      "Fishy Pipe",
      "1",
      "Fishy, 10 turns",
      "Fishy Pipe used"
    },
    {
      "Special", "Concert",
    },
    {
      "Special", "Demon Summoning",
    },
    {
      "Skill",
      "Rage Gland",
      "rageGlandVented",
      "Rage Gland",
      "1",
      "-10% Mus/Myst/Mox, randomly chosen, and each turn of combat do level to 2*level damage, 5 turns",
      "Rage Gland used"
    },
    {
      "Special", "Free Rests",
    },
    {
      "Special", "Hot Tub",
    },
    {
      "Special", "Nuns",
    },
    {"Item", "Oscus' Soda", "oscusSodaUsed", "Oscus' Soda", "1", "200-300 MP", "Oscus' Soda used"},
    {
      "Item",
      "Express Card",
      "expressCardUsed",
      "Express Card",
      "1",
      "extends duration of all current effects by 5 turns, restores all MP, cools zapped wands",
      "Express Card used"
    },
    {
      "Item",
      "Brass Dreadsylvanian Flask",
      "_brassDreadFlaskUsed",
      "Brass Dreadsylvanian flask",
      "1",
      "100 turns of +100% Physical Damage in Dreadsylvania",
      "Brass flask used"
    },
    {
      "Item",
      "Silver Dreadsylvanian Flask",
      "_silverDreadFlaskUsed",
      "Silver Dreadsylvanian flask",
      "1",
      "100 turns of +200 Spell Damage in Dreadsylvania",
      "Silver flask used"
    },
    {
      "Special", "Flush Mojo",
    },
    {
      "Special", "Feast",
    },
    {
      "Special", "Pudding",
    },
    {
      "Special", "Melange",
    },
    {
      "Special", "Ultra Mega Sour Ball",
    },
    {
      "Special", "Stills",
    },
    {
      "Special", "Tea Party",
    },
    {
      "Special", "Photocopy",
    },
    {
      "Special", "Putty",
    },
    {
      "Special", "Envyfish Egg",
    },
    {
      "Special", "Camera",
    },
    {
      "Special", "Romantic Arrow",
    },
    {
      "Special", "Bonus Adventures",
    },
    {
      "Special", "Familiar Drops",
    },
    {
      "Special", "Free Fights",
    },
    {
      "Special", "Free Runaways",
    },
    {"Special", "Hatter"},
    {"Special", "Banished Monsters"},
    {"Special", "Swimming Pool"},
    {"Special", "Jick Jar"},
    {"Special", "Avatar of Jarlberg Staves"},
    {"Special", "Defective Token"},
    {"Special", "Chateau Desk"},
    {"Special", "Deck of Every Card"},
    {"Special", "Shrine to the Barrel god"},
    {"Special", "Potted Tea Tree"},
    {"Special", "Terminal Educate"},
    {"Special", "Terminal Enhance"},
    {"Special", "Terminal Enquiry"},
    {"Special", "Terminal Extrude"},
    {"Special", "Terminal Summary"}
  };

  private static int getVersion(String deed) {
    // Add a method to return the proper version for the deed given.
    // i.e. if ( deed.equals( "Breakfast" ) ) return 1;

    if (deed.equals("Terminal Educate")) return 13;
    else if (deed.equals("Terminal Enhance")) return 13;
    else if (deed.equals("Terminal Enquiry")) return 13;
    else if (deed.equals("Terminal Extrude")) return 13;
    else if (deed.equals("Terminal Summary")) return 13;
    else if (deed.equals("Potted Tea Tree")) return 12;
    else if (deed.equals("Shrine to the Barrel god")) return 11;
    else if (deed.equals("Deck of Every Card")) return 10;
    else if (deed.equals("Chateau Desk")) return 9;
    else if (deed.equals("Ultra Mega Sour Ball")) return 8;
    else if (deed.equals("Avatar of Jarlberg Staves")) return 6;
    else if (deed.equals("Swimming Pool")) return 5;
    else if (deed.equals("Banished Monsters")) return 4;
    else if (deed.equals("Hatter")) return 3;
    else if (deed.equals(("Romantic Arrow"))) return 2;
    else if (deed.equals(("Feast"))) return 1;
    else return 0;
  }

  public DailyDeedsPanel() {
    super(BoxLayout.Y_AXIS);

    int currentVersion = Preferences.getInteger("dailyDeedsVersion");
    Preferences.resetToDefault("dailyDeedsVersion");
    int releaseVersion = Preferences.getInteger("dailyDeedsVersion");

    // Version handling: if our version is older than the one in defaults.txt,
    // add deeds with newer version numbers to the end of dailyDeedsOptions.

    if (currentVersion < releaseVersion) {
      for (String[] builtinDeed : BUILTIN_DEEDS) {
        String builtinDeedName = builtinDeed[1];

        if (getVersion(builtinDeedName) > currentVersion) {
          String oldString = Preferences.getString("dailyDeedsOptions");
          Preferences.setString("dailyDeedsOptions", oldString + "," + builtinDeedName);
          RequestLogger.printLine(
              "New deed found.  Adding " + builtinDeedName + " to the end of your deeds panel.");
        }
      }
      RequestLogger.printLine("Deeds updated.  Now version " + releaseVersion + ".");
    }

    RequestThread.executeMethodAfterInitialization(this, "populate");
    PreferenceListenerRegistry.registerPreferenceListener("dailyDeedsOptions", this);
  }

  public void populate() {
    // If we're not logged in, don't populate daily deeds.
    if (KoLCharacter.baseUserName().equals("GLOBAL")) {
      return;
    }

    int sCount = 0;

    String deedsString = Preferences.getString("dailyDeedsOptions");
    // REGEX: splits deedsString by commas that are NOT immediately followed by a pipe.
    // This is necessary to allow commas in custom text deeds.
    String[] pieces = deedsString.split(",(?!\\|)");

    // This loop iterates over all of the elements in the dailyDeedsOptions preference.
    for (String deed : pieces) {
      /*
       * This loop iterates down the full list of built-in deeds. Once it finds the deed in question, it
       * checks what kind of deed we're handling. Currently there is generalized handling for BooleanPref,
       * BooleanItem, Multipref, Skill, and Text types; all the other built-ins are marked as Special and require
       * their own function in dailyDeedsPanel to handle.
       */
      for (String[] builtinDeed : DailyDeedsPanel.BUILTIN_DEEDS) {
        /*
         * Built-in handling
         */
        if (deed.equals(builtinDeed[1])) {
          /*
           * Generalized handling
           */
          if (builtinDeed[0].equalsIgnoreCase("Command")) {
            parseCommandDeed(builtinDeed);
            break;
          } else if (builtinDeed[0].equalsIgnoreCase("Item")) {
            parseItemDeed(builtinDeed);
            break;
          } else if (builtinDeed[0].equalsIgnoreCase("Skill")) {
            parseSkillDeed(builtinDeed);
            break;
          }

          /*
           * Special Handling
           */
          else if (builtinDeed[0].equalsIgnoreCase("Special")) {
            parseSpecialDeed(builtinDeed);
            break;
          }

          // we'll only get here if an unknown deed type was set in BUILTIN_DEEDS.
          // Shouldn't happen.

          RequestLogger.printLine("Unknown deed type: " + builtinDeed[0]);
          break;
        }
      }
      /*
       * Custom handling
       */
      if (deed.startsWith("$CUSTOM|") && deed.split("\\|").length > 1) {
        String cString = deed.substring(8); // remove $CUSTOM|
        String[] customPieces = cString.split("\\|");

        if (customPieces[0].equalsIgnoreCase("Command")) {
          parseCommandDeed(customPieces);
        } else if (customPieces[0].equalsIgnoreCase("Item")) {
          parseItemDeed(customPieces);
        } else if (customPieces[0].equalsIgnoreCase("Skill")) {
          parseSkillDeed(customPieces);
        } else if (customPieces[0].equalsIgnoreCase("Text")) {
          parseTextDeed(customPieces);
        } else if (customPieces[0].equalsIgnoreCase("Combo")) {
          parseComboDeed(customPieces);
        } else if (customPieces[0].equalsIgnoreCase("Simple")) {
          parseSimpleDeed(customPieces, sCount);
          ++sCount;
        }
      }
    }
  }

  private void parseComboDeed(String[] deedsString) {
    boolean isMulti = false;
    int maxUses = 1;
    if (deedsString.length > 3) {
      if (deedsString[3].equalsIgnoreCase("$ITEM")) {

      } else {
        try {
          maxUses = Integer.parseInt(deedsString[3]);
          isMulti = true;
        } catch (NumberFormatException e) {
          // not sure what you did.  Possibly used the wrong number of arguments, or specified a
          // non-integer max
          return;
        }
      }
    }
    if (deedsString.length > (isMulti ? 4 : 3)
        && (deedsString.length - (isMulti ? 4 : 3)) % 4 != 0) {
      RequestLogger.printLine(
          "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Combo.");
      return;
    }
    /*
     * !isMulti First 3:
     * Combo|displayText|preference
     * this first pref is used to enable/disable the whole combodeed.
     *
     * isMulti First 4:
     * Combo|displayText|preference|maxUses
     */

    String displayText = deedsString[1];
    String pref = deedsString[2];

    // pack up the rest of the deed into an ArrayList.
    // .get( element ) gives a string array containing { "$ITEM", displayText, preference, command }

    ArrayList<String[]> packedDeed = new ArrayList<String[]>();
    for (int i = (isMulti ? 4 : 3); i < deedsString.length; i += 4) {
      if (!deedsString[i].equals("$ITEM")) {
        RequestLogger.printLine(
            "Each combo item must start with $ITEM, you used " + deedsString[i]);
        return;
      }
      packedDeed.add(
          new String[] {
            deedsString[i], deedsString[i + 1], deedsString[i + 2], deedsString[i + 3]
          });
    }

    if (isMulti) {
      this.add(new ComboDaily(displayText, pref, packedDeed, maxUses));
    } else {
      this.add(new ComboDaily(displayText, pref, packedDeed));
    }
  }

  private void parseTextDeed(String[] deedsString) {
    // No error handling here, really.  0-length strings don't do anything;
    // blank strings end up working like a \n

    this.add(new TextDeed(deedsString));
  }

  private void parseCommandDeed(String[] deedsString) {
    if (deedsString.length < 3 || deedsString.length > 7) {
      RequestLogger.printLine(
          "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Command. (3, 4, 5, 6 or 7)");
      return;
    }

    String pref = deedsString[2];

    if (deedsString.length == 3) {
      /*
       * BooleanPref|displayText|preference
       * command is the same as displayText
       */
      // Use the display text for the command if none was specified
      String command = deedsString[1];

      this.add(new CommandDaily(pref, command));
    } else if (deedsString.length == 4) {
      /*
       * BooleanPref|displayText|preference|command
       */
      String displayText = deedsString[1];
      String command = deedsString[3];
      this.add(new CommandDaily(displayText, pref, command));
    } else if (deedsString.length == 5) {
      /*
       * MultiPref|displayText|preference|command|maxPref
       */

      String displayText = deedsString[1];
      String command = deedsString[3];
      try {
        String maxString = deedsString[4];
        int maxPref = 1;
        if (!maxString.equals("")) {
          maxPref = Integer.parseInt(maxString);
        }

        this.add(new CommandDaily(displayText, pref, command, maxPref));
      } catch (NumberFormatException e) {
        RequestLogger.printLine(
            "Daily Deeds error: Command deeds require an int for the fifth parameter.");
      }
    } else if (deedsString.length == 6) {
      /*
       * MultiPref|displayText|preference|command|maxPref|toolTip
       */

      String displayText = deedsString[1];
      String command = deedsString[3];
      String toolTip = deedsString[5];
      try {
        String maxString = deedsString[4];
        int maxPref = 1;
        if (!maxString.equals("")) {
          maxPref = Integer.parseInt(maxString);
        }

        this.add(new CommandDaily(displayText, pref, command, maxPref, toolTip));
      } catch (NumberFormatException e) {
        RequestLogger.printLine(
            "Daily Deeds error: Command deeds require an int for the fifth parameter.");
      }
    } else if (deedsString.length == 7) {
      /*
       * MultiPref|displayText|preference|command|maxPref|toolTip|compMessage
       */

      String displayText = deedsString[1];
      String command = deedsString[3];
      String toolTip = deedsString[5];
      String compMessage = deedsString[6];
      if (command.equals("")) {
        command = displayText;
      }
      try {
        String maxString = deedsString[4];
        int maxPref = 1;
        if (!maxString.equals("")) {
          maxPref = Integer.parseInt(maxString);
        }

        this.add(new CommandDaily(displayText, pref, command, maxPref, toolTip, compMessage));
      } catch (NumberFormatException e) {
        RequestLogger.printLine(
            "Daily Deeds error: Command deeds require an int for the fifth parameter.");
      }
    }
  }

  private void parseItemDeed(String[] deedsString) {
    if (deedsString.length < 3 || deedsString.length > 7) {
      RequestLogger.printLine(
          "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Item. (3, 4, 5, 6 or 7)");
      return;
    }

    String pref = deedsString[2];

    String displayText = deedsString[1];
    String itemName = "";
    int itemId = -1;
    String itemCommand = "";

    // 3 arguments uses the displayText as the itemName
    // 4, 5, 6, 7 arguments use optional itemName, otherwise displayText
    // Use substring matching in getItemId because itemName may not
    // be the canonical name of the item
    if (deedsString.length == 3 || deedsString[3].equals("")) {
      // optional itemName not specified; use display text
      itemName = displayText;
      itemId = ItemDatabase.getItemId(itemName);
      itemCommand = "use " + ItemDatabase.getItemName(itemId);
    } else {
      // itemName is specified
      String[] split = deedsString[3].split(";");
      itemName = split[0];
      itemId = ItemDatabase.getItemId(itemName);
      itemCommand = "use " + ItemDatabase.getItemName(itemId);

      // Additional arbitrary commands allowed
      if (split.length > 1) {
        for (int i = 1; i < split.length; ++i) {
          itemCommand += ";" + split[i];
        }
      }
    }

    if (itemId == -1) {
      RequestLogger.printLine("Daily Deeds error: unable to resolve item " + itemName);
      return;
    }

    // 5, 6, or 7 arguments include an optional maxUses
    int maxUses = 1;

    if (deedsString.length >= 5) {
      try {
        String maxString = deedsString[4];
        if (!maxString.equals("")) {
          maxUses = Integer.parseInt(maxString);
        }

      } catch (NumberFormatException e) {
        RequestLogger.printLine(
            "Daily Deeds error: Item deeds require an int for the fifth parameter.");
        return;
      }
    }

    if (deedsString.length == 3) {
      /*
       * BooleanItem|displayText|preference
       * itemId is found from displayText
       */
      this.add(new ItemDaily(pref, itemId, itemCommand));
    } else if (deedsString.length == 4) {
      /*
       * BooleanItem|displayText|preference|itemName
       * itemId is found from itemName if present, otherwise display text
       */
      this.add(new ItemDaily(displayText, pref, itemId, itemCommand));
    } else if (deedsString.length == 5) {
      /*
       * BooleanItem|displayText|preference|itemName|maxUses
       * itemId is found from itemName if present, otherwise display text
       */
      this.add(new ItemDaily(displayText, pref, itemId, itemCommand, maxUses));
    } else if (deedsString.length == 6) {
      /*
       * BooleanItem|displayText|preference|itemName|maxUses|toolTip
       * itemId is found from itemName if present, otherwise display text
       */
      String toolTip = deedsString[5];
      this.add(new ItemDaily(displayText, pref, itemId, itemCommand, maxUses, toolTip));
    } else if (deedsString.length == 7) {
      /*
       * BooleanItem|displayText|preference|itemName|maxUses|toolTip|compMessage
       * itemId is found from itemName if present, otherwise display text
       */
      String toolTip = deedsString[5];
      String compMessage = deedsString[6];
      this.add(
          new ItemDaily(displayText, pref, itemId, itemCommand, maxUses, toolTip, compMessage));
    }
  }

  private void parseSkillDeed(String[] deedsString) {
    if (deedsString.length < 3 || deedsString.length > 7) {
      RequestLogger.printLine(
          "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Skill. (3, 4, 5, or 6)");
      return;
    }

    String pref = deedsString[2];

    if (deedsString.length == 3) {
      /*
       * Skill|displayText|preference
       * skillName is found from displayText
       */
      List<String> skillNames = SkillDatabase.getMatchingNames(deedsString[1]);

      if (skillNames.size() != 1) {
        RequestLogger.printLine("Daily Deeds error: unable to resolve skill " + deedsString[1]);
        return;
      }

      this.add(new SkillDaily(pref, skillNames.get(0), "cast " + skillNames.get(0)));
    } else if (deedsString.length == 4) {
      /*
       * Skill|displayText|preference|skillName
       */
      String displayText = deedsString[1];
      List<String> skillNames = SkillDatabase.getMatchingNames(deedsString[3]);

      if (skillNames.size() != 1) {
        RequestLogger.printLine("Daily Deeds error: unable to resolve skill " + deedsString[3]);
        return;
      }
      this.add(new SkillDaily(displayText, pref, skillNames.get(0), "cast " + skillNames.get(0)));
    } else if (deedsString.length == 5) {
      String displayText = deedsString[1];
      List<String> skillNames = SkillDatabase.getMatchingNames(deedsString[3]);

      try {
        int maxCasts = Integer.parseInt(deedsString[4]);

        if (skillNames.size() != 1) {
          RequestLogger.printLine("Daily Deeds error: unable to resolve skill " + deedsString[3]);
          return;
        }
        this.add(
            new SkillDaily(
                displayText, pref, skillNames.get(0), "cast " + skillNames.get(0), maxCasts));
      } catch (NumberFormatException e) {
        RequestLogger.printLine(
            "Daily Deeds error: Skill deeds require an int for the fifth parameter.");
      }
    } else if (deedsString.length == 6) {
      String displayText = deedsString[1];
      List<String> skillNames = SkillDatabase.getMatchingNames(deedsString[3]);
      String toolTip = deedsString[5];

      try {
        int maxCasts = Integer.parseInt(deedsString[4]);

        if (skillNames.size() != 1) {
          RequestLogger.printLine("Daily Deeds error: unable to resolve skill " + deedsString[3]);
          return;
        }
        this.add(
            new SkillDaily(
                displayText,
                pref,
                skillNames.get(0),
                "cast " + skillNames.get(0),
                maxCasts,
                toolTip));
      } catch (NumberFormatException e) {
        RequestLogger.printLine(
            "Daily Deeds error: Skill deeds require an int for the fifth parameter.");
      }
    } else if (deedsString.length == 7) {
      String displayText = deedsString[1];
      String skillString = deedsString[3];
      if (skillString.equals("")) {
        skillString = displayText;
      }
      List<String> skillNames = SkillDatabase.getMatchingNames(skillString);
      String toolTip = deedsString[5];
      String compMessage = deedsString[6];

      try {
        String maxString = deedsString[4];
        int maxCasts = 1;
        if (!maxString.equals("")) {
          maxCasts = Integer.parseInt(maxString);
        }

        if (skillNames.size() != 1) {
          RequestLogger.printLine("Daily Deeds error: unable to resolve skill " + skillString);
          return;
        }
        this.add(
            new SkillDaily(
                displayText,
                pref,
                skillNames.get(0),
                "cast " + skillNames.get(0),
                maxCasts,
                toolTip,
                compMessage));
      } catch (NumberFormatException e) {
        RequestLogger.printLine(
            "Daily Deeds error: Skill deeds require an int for the fifth parameter.");
      }
    }
  }

  private void parseSimpleDeed(String[] deedsString, int sCount) {
    if (deedsString.length < 2 || deedsString.length > 6) {
      RequestLogger.printLine(
          "Daily Deeds error: You did not pass the proper number of parameters for a deed of type Simple. (2, 3, or 4)");
      return;
    }

    if (deedsString.length == 2) {
      /*
       * Simple|displayText
       * command is the same as displayText
       */
      // Use the display text for the command if none was specified
      String command = deedsString[1];

      this.add(new SimpleDaily(command, sCount));
    } else if (deedsString.length == 3) {
      /*
       * Simple|displayText|command
       */
      String displayText = deedsString[1];
      String command = deedsString[2];

      this.add(new SimpleDaily(displayText, command, sCount));
    } else if (deedsString.length == 4) {
      /*
       * Simple|displayText|command|maxPref
       */

      String displayText = deedsString[1];
      String command = deedsString[2];
      try {
        int maxPref = Integer.parseInt(deedsString[3]);

        this.add(new SimpleDaily(displayText, command, maxPref, sCount));
      } catch (NumberFormatException e) {
        RequestLogger.printLine(
            "Daily Deeds error: Simple deeds require an int for the fourth parameter.");
      }
    } else if (deedsString.length == 6) {
      /*
       * Simple|displayText|command|maxPref|tooltip|compMessage
       */

      String displayText = deedsString[1];
      String command = deedsString[2];
      String tooltip = deedsString[4];
      String compMessage = deedsString[5];
      try {
        String maxString = deedsString[3];
        int maxPref = 1;
        if (!maxString.equals("")) {
          maxPref = Integer.parseInt(maxString);
        }

        this.add(new SimpleDaily(displayText, command, maxPref, sCount, tooltip, compMessage));
      } catch (NumberFormatException e) {
        RequestLogger.printLine(
            "Daily Deeds error: Simple deeds require an int for the fourth parameter.");
      }
    }
  }

  private void parseSpecialDeed(String[] deedsString) {
    if (deedsString[1].equals("Submit Spading Data")) {
      this.add(new SpadeDaily());
    } else if (deedsString[1].equals("Crimbo Tree")) {
      this.add(new CrimboTreeDaily());
    } else if (deedsString[1].equals("Chips")) {
      this.add(new ChipsDaily());
    } else if (deedsString[1].equals("Telescope")) {
      this.add(new TelescopeDaily());
    } else if (deedsString[1].equals("Ball Pit")) {
      this.add(new PitDaily());
    } else if (deedsString[1].equals("Styx Pixie")) {
      this.add(new StyxDaily());
    } else if (deedsString[1].equals("VIP Pool")) {
      this.add(new PoolDaily());
    } else if (deedsString[1].equals("April Shower")) {
      this.add(new ShowerCombo());
    } else if (deedsString[1].equals("Friars")) {
      this.add(new FriarsDaily());
    } else if (deedsString[1].equals("Mom")) {
      this.add(new MomCombo());
    } else if (deedsString[1].equals("Skate Park")) {
      this.add(new SkateDaily("lutz", "ice", "_skateBuff1", "Fishy"));
      this.add(new SkateDaily("comet", "roller", "_skateBuff2", "-30% to Sea penalties"));
      this.add(new SkateDaily("band shell", "peace", "_skateBuff3", "+sand dollars"));
      this.add(new SkateDaily("eels", "peace", "_skateBuff4", "+10 lbs. underwater"));
      this.add(new SkateDaily("merry-go-round", "peace", "_skateBuff5", "+25% items underwater"));

    } else if (deedsString[1].equals("Concert")) {
      this.add(new ConcertDaily());
    } else if (deedsString[1].equals("Demon Summoning")) {
      this.add(new DemonCombo());
    } else if (deedsString[1].equals("Free Rests")) {
      this.add(new RestsDaily());
    } else if (deedsString[1].equals("Hot Tub")) {
      this.add(new HotTubDaily());
    } else if (deedsString[1].equals("Nuns")) {
      this.add(new NunsDaily());
    } else if (deedsString[1].equals("Flush Mojo")) {
      this.add(new MojoDaily());
    } else if (deedsString[1].equals("Feast")) {
      this.add(new FeastDaily());
    } else if (deedsString[1].equals("Pudding")) {
      this.add(new PuddingDaily());
    } else if (deedsString[1].equals("Melange")) {
      this.add(new MelangeDaily());
    } else if (deedsString[1].equals("Ultra Mega Sour Ball")) {
      this.add(new UltraMegaSourBallDaily());
    } else if (deedsString[1].equals("Stills")) {
      this.add(new StillsDaily());
    } else if (deedsString[1].equals("Tea Party")) {
      this.add(new TeaPartyDaily());
    } else if (deedsString[1].equals("Photocopy")) {
      this.add(new PhotocopyDaily());
    } else if (deedsString[1].equals("Putty")) {
      this.add(new PuttyDaily());
    } else if (deedsString[1].equals("Camera")) {
      this.add(new CameraDaily());
    } else if (deedsString[1].equals("Envyfish Egg")) {
      this.add(new EnvyfishDaily());
    } else if (deedsString[1].equals("Romantic Arrow")) {
      this.add(new RomanticDaily());
    } else if (deedsString[1].equals("Bonus Adventures")) {
      this.add(new AdvsDaily());
    } else if (deedsString[1].equals("Familiar Drops")) {
      this.add(new DropsDaily());
    } else if (deedsString[1].equals("Free Fights")) {
      this.add(new FreeFightsDaily());
    } else if (deedsString[1].equals("Free Runaways")) {
      this.add(new RunawaysDaily());
    } else if (deedsString[1].equals("Hatter")) {
      this.add(new HatterDaily());
    } else if (deedsString[1].equals("Banished Monsters")) {
      this.add(new BanishedDaily());
    } else if (deedsString[1].equals("Swimming Pool")) {
      this.add(new SwimmingPoolDaily());
    } else if (deedsString[1].equals("Jick Jar")) {
      this.add(new JickDaily());
    } else if (deedsString[1].equals("Avatar of Jarlberg Staves")) {
      this.add(new JarlsbergStavesDaily());
    } else if (deedsString[1].equals("Defective Token")) {
      this.add(new DefectiveTokenDaily());
    } else if (deedsString[1].equals("Chateau Desk")) {
      this.add(new ChateauDeskDaily());
    } else if (deedsString[1].equals("Deck of Every Card")) {
      this.add(new DeckOfEveryCardDaily());
    } else if (deedsString[1].equals("Potted Tea Tree")) {
      this.add(new TeaTreeDaily());
    } else if (deedsString[1].equals("Shrine to the Barrel god")) {
      this.add(new BarrelGodDaily());
    } else if (deedsString[1].equals("Terminal Educate")) {
      this.add(new TerminalEducateDaily());
    } else if (deedsString[1].equals("Terminal Enhance")) {
      this.add(new TerminalEnhanceDaily());
    } else if (deedsString[1].equals("Terminal Enquiry")) {
      this.add(new TerminalEnquiryDaily());
    } else if (deedsString[1].equals("Terminal Extrude")) {
      this.add(new TerminalExtrudeDaily());
    } else if (deedsString[1].equals("Terminal Summary")) {
      this.add(new TerminalSummaryDaily());
    } else { // you added a special deed to BUILTIN_DEEDS but didn't add a method call.
      RequestLogger.printLine(
          "Couldn't match a deed: " + deedsString[1] + " does not have a built-in method.");
    }
  }

  @Override
  public void update() {
    // Called whenever the dailyDeedsOptions preference is changed.
    this.removeAll();
    this.populate();
    this.revalidate();
    this.repaint();
  }

  public void add(Daily daily) {
    daily.add(Box.createHorizontalGlue());
    RequestThread.runInParallel(new InitialUpdateRunnable(daily));
    super.add(daily);
  }

  private static class InitialUpdateRunnable implements Runnable {
    private final Daily daily;

    public InitialUpdateRunnable(Daily daily) {
      this.daily = daily;
    }

    @Override
    public void run() {
      daily.update();
    }
  }

  public abstract static class Daily extends Box implements ActionListener, Listener {
    private ArrayList<JButton> buttons;
    private JLabel label;

    public Daily() {
      super(BoxLayout.X_AXIS);
    }

    public void addListener(String preference) {
      PreferenceListenerRegistry.registerPreferenceListener(preference, this);
    }

    public void addItem(int itemId) {
      ItemListenerRegistry.registerItemListener(itemId, this);
    }

    public JButton addButton(String command) {
      JButton button = new JButton(command);
      button.setActionCommand(command);
      button.addActionListener(this);
      button.setBackground(this.getBackground());
      button.setDefaultCapable(false);
      button.putClientProperty("JButton.buttonType", "segmented");

      if (this.buttons == null) {
        this.buttons = new ArrayList<JButton>();
        button.putClientProperty("JButton.segmentPosition", "only");
      } else {
        button.putClientProperty("JButton.segmentPosition", "last");
        int last = this.buttons.size() - 1;
        this.buttons
            .get(last)
            .putClientProperty("JButton.segmentPosition", last == 0 ? "first" : "middle");
      }
      this.buttons.add(button);
      this.add(button);
      return button;
    }

    public void addButton(String command, String tip) {
      this.addButton(command).setToolTipText(tip);
    }

    public JButton addComboButton(String command, String displaytext) {
      JButton button = new JButton(command);
      button.setActionCommand(command);
      button.setText(displaytext);
      button.addActionListener(this);
      button.setBackground(this.getBackground());
      button.setDefaultCapable(false);
      button.putClientProperty("JButton.buttonType", "segmented");
      if (this.buttons == null) {
        this.buttons = new ArrayList<JButton>();
        button.putClientProperty("JButton.segmentPosition", "only");
      } else {
        button.putClientProperty("JButton.segmentPosition", "last");
        int last = this.buttons.size() - 1;
        this.buttons
            .get(last)
            .putClientProperty("JButton.segmentPosition", last == 0 ? "first" : "middle");
      }
      this.buttons.add(button);
      this.add(button);
      return button;
    }

    public void addComboButton(String command, String displaytext, String tip) {
      this.addComboButton(command, displaytext).setToolTipText(tip);
    }

    public DisabledItemsComboBox<String> addComboBox(
        String[] choice, List<String> tooltips, String lengthString) {
      DisabledItemsComboBox<String> comboBox = new DisabledItemsComboBox<>();
      int ht = comboBox.getFontMetrics(comboBox.getFont()).getHeight();
      int len = comboBox.getFontMetrics(comboBox.getFont()).stringWidth(lengthString);

      // pseudo magic numbers here, but maximumsize will likely never
      // be looked at by the layout manager. If  maxsize is not set,
      // the layout manager isn't happy.
      // The combobox is ultimately sized by setPrototypeDisplayValue().

      comboBox.setMaximumSize(new Dimension(Math.round(len + 100), (int) Math.round(ht * 1.5)));
      comboBox.setPrototypeDisplayValue(lengthString);

      for (int i = 0; i < choice.length; ++i) {
        comboBox.addItem(choice[i]);
      }

      comboBox.setTooltips(tooltips);
      this.add(comboBox);
      return comboBox;
    }

    public void setComboTarget(JButton b, String act) {
      b.setActionCommand(act);
    }

    public JButton buttonText(int idx, String command) {
      JButton button = this.buttons.get(idx);
      button.setText(command);
      button.setActionCommand(command);
      return button;
    }

    public void buttonText(int idx, String command, String tip) {
      this.buttonText(idx, command).setToolTipText(tip);
    }

    public void addLabel(String text) {
      this.label = new JLabel(text);
      this.add(this.label);
    }

    public void setText(String text) {
      this.label.setText(text);
    }

    @Override
    public void setEnabled(boolean enabled) {
      if (this.buttons != null) {
        Iterator<JButton> i = this.buttons.iterator();

        while (i.hasNext()) {
          JButton button = i.next();

          button.setEnabled(enabled);
        }
      }
    }

    public void setEnabled(int index, boolean enabled) {
      this.buttons.get(index).setEnabled(enabled);
    }

    public void setShown(boolean shown) {
      if (shown != this.isVisible()) {
        this.setVisible(shown);
        this.revalidate();
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      CommandDisplayFrame.executeCommand(e.getActionCommand());
      // Try to avoid having a random button, possibly with a high associated
      // cost, set as the default button when this one is disabled.
      KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
    }

    @Override
    public abstract void update();
  }

  public static class ShowerCombo extends Daily {
    // We don't really need the ability to disable items within
    // the shower combo box, but it's implemented here for consistency

    DisabledItemsComboBox<String> box = new DisabledItemsComboBox<>();
    Component space;
    JButton btn;

    public ShowerCombo() {
      List<String> ttips = new ArrayList<String>();
      String[] choices = {"April Shower", "Muscle", "Mysticality", "Moxie", "Ice", "MP"};
      String[] tips = {
        "Take a shower",
        "+5% to all Muscle Gains, 50 turns",
        "+5% to all Mysticality Gains, 50 turns",
        "+5% to all Moxie Gains, 50 turns",
        "shards of double-ice",
        "mp or amazing idea"
      };

      ttips.addAll(Arrays.asList(tips));

      this.addItem(ItemPool.VIP_LOUNGE_KEY);
      this.addListener("_aprilShower");
      this.addListener("kingLiberated");
      this.addListener("(character)");

      // the string is used to set the combobox width. pick the largest, add a space
      box = this.addComboBox(choices, ttips, "April Shower ");
      box.addActionListener(new ShowerComboListener());
      space = this.add(Box.createRigidArea(new Dimension(5, 1))); // small 5px spacer

      btn = this.addComboButton("", "Go!"); // initialize GO button to do nothing
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0;
      boolean as = Preferences.getBoolean("_aprilShower");
      boolean allowed = StandardRequest.isAllowed("Clan Item", "April Shower");
      boolean limited = Limitmode.limitClan();
      this.setShown((!bm || kf) && (have || as) && allowed && !limited);
      this.setEnabled(true);
      box.setEnabled(true);
      if (as) {
        this.setText("You have showered today");
        box.setVisible(false);
        space.setVisible(false);
        btn.setVisible(false);
        return;
      }
    }

    // can probably generalize these combo listeners and put them somewhere else.
    // for now they're individual to each combo.
    private class ShowerComboListener implements ActionListener {
      // the combo listeners exist solely to update the GO button with
      // the combo box target
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();
        if (cb.getSelectedIndex() <= 0) {
          setComboTarget(btn, "");
        } else {
          String Choice = cb.getSelectedItem().toString();
          setComboTarget(btn, "shower " + Choice);
        }
      }
    }
  }

  public static class DemonCombo extends Daily {
    DisabledItemsComboBox<String> box = new DisabledItemsComboBox<>();
    Component space;
    JButton btn = null;

    public DemonCombo() {
      int len = KoLAdventure.DEMON_TYPES.length;
      List<String> ttips = new ArrayList<String>();
      String[] choices = new String[len + 1];
      choices[0] = "Summoning Chamber";
      String[] tips = {
        "Summon a demon",
        "Yum!",
        "+100% meat, 30 turns",
        "+5-16 HP/MP, 30 turns",
        "+20 hot damage, +5 DR, 30 turns",
        "+30 stench damage, 30 turns",
        null,
        "Booze!",
        "Why ARE you here?",
        "+80-100 hot damage, 30 turns",
        "Stat boost, 30 turns",
        "+200% booze drop, 30 turns"
      };

      for (int i = 1; i <= len; ++i) {
        this.addListener("demonName" + i);
        choices[i] = KoLAdventure.DEMON_TYPES[i - 1][1];
      }

      ttips.addAll(Arrays.asList(tips));

      this.addListener("(character)");
      this.addListener("demonSummoned");
      this.addListener(Quest.MANOR.getPref());

      box = this.addComboBox(choices, ttips, "Summoning Chamber ");
      box.addActionListener(new DemonComboListener());
      space = this.add(Box.createRigidArea(new Dimension(5, 1)));

      // Initialize the GO button to do nothing.
      btn = this.addComboButton("", "Go!");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean summoned = Preferences.getBoolean("demonSummoned");
      boolean have = QuestDatabase.isQuestFinished(Quest.MANOR);
      this.setShown(have);
      this.setEnabled(true);
      box.setEnabled(true); // this.setEnabled will not disable the combo box, for whatever reason
      if (summoned) {
        this.setText("You have summoned a demon today");
        box.setVisible(false);
        space.setVisible(false);
        btn.setVisible(false);
        return;
      }

      // Disable individual choices if we don't have the demon names
      // Don't touch the first list element
      for (int i = 1; i <= KoLAdventure.DEMON_TYPES.length; ++i) {
        box.setDisabledIndex(i, Preferences.getString("demonName" + i).equals(""));
      }
    }

    private class DemonComboListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();
        if (cb.getSelectedIndex() <= 0) {
          setComboTarget(btn, "");
        } else {
          String Choice = cb.getSelectedItem().toString();
          setComboTarget(btn, "summon " + Choice);
        }
      }
    }
  }

  public static class ComboDaily extends Daily {
    DisabledItemsComboBox<String> box = new DisabledItemsComboBox<>();
    JButton btn = null;

    ArrayList<String[]> packedDeed;
    String preference;
    int maxPref = 1;

    public ComboDaily(String displayText, String pref, ArrayList<String[]> packedDeed) {
      this.packedDeed = packedDeed;
      this.preference = pref;

      int len = packedDeed.size();
      List<String> ttips = new ArrayList<String>();
      String[] tips = new String[len + 1];
      String[] choices = new String[len + 1];
      choices[0] = displayText;
      tips[0] = "";
      String lengthString = "ABCDEFGH";

      for (int i = 1; i <= len; ++i) {
        String[] item = packedDeed.get(i - 1);

        tips[i] = item[3];
        this.addListener(item[2]);
        choices[i] = item[1];

        if (item[1].length() > lengthString.length()) {
          lengthString = item[1];
        }
      }
      ttips.addAll(Arrays.asList(tips));

      this.addListener(pref);
      this.box = this.addComboBox(choices, ttips, lengthString + " ");
      this.box.addActionListener(new ComboListener());
      this.add(Box.createRigidArea(new Dimension(5, 1)));

      btn = this.addComboButton("", "Go!");
    }

    public ComboDaily(
        String displayText, String pref, ArrayList<String[]> packedDeed, int maxUses) {
      this(displayText, pref, packedDeed);
      this.maxPref = maxUses;
      this.addLabel("");
    }

    @Override
    public void update() {
      int prefToInt = 1;
      String pref = Preferences.getString(this.preference);
      if (pref.equalsIgnoreCase("true")
          || pref.equalsIgnoreCase("false")
          || pref.equalsIgnoreCase("")) {
        prefToInt = pref.equalsIgnoreCase("true") ? 1 : 0;
      } else {
        try {
          prefToInt = Integer.parseInt(pref);
        } catch (NumberFormatException e) {
        }
      }
      this.setEnabled(prefToInt < this.maxPref);
      this.box.setEnabled(prefToInt < this.maxPref);
      if (this.maxPref > 1) {
        this.setText(prefToInt + "/" + this.maxPref);
      }
      this.setShown(true);

      for (int i = 1; i <= packedDeed.size(); ++i) {
        prefToInt = 1;
        String[] item = packedDeed.get(i - 1);
        pref = Preferences.getString(item[2]);
        if (pref.equalsIgnoreCase("true")
            || pref.equalsIgnoreCase("false")
            || pref.equalsIgnoreCase("")) {
          prefToInt = pref.equalsIgnoreCase("true") ? 1 : 0;
        } else {
          try {
            prefToInt = Integer.parseInt(pref);
          } catch (NumberFormatException e) {
          }
        }
        this.box.setDisabledIndex(i, prefToInt > 0);
      }
    }

    private class ComboListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();
        int choice = cb.getSelectedIndex();
        if (choice <= 0) {
          setComboTarget(btn, "");
        } else {
          String[] item = packedDeed.get(choice - 1);
          setComboTarget(btn, item[3]);
        }
      }
    }
  }

  public static class SimpleDaily extends Daily {
    JButton button;
    String preference;
    int maxPref = 1;
    String compMessage = "";

    /**
     * @param command the command to execute. This will also be the displayed button text.
     * @param sCount
     */
    public SimpleDaily(String command, int sCount) {
      this.preference = "_simpleDeed" + sCount;
      this.addListener(preference);
      button = this.addComboButton(command, command);
      button.addActionListener(new SimpleListener(this.preference));
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param command the command to execute.
     * @param sCount
     */
    public SimpleDaily(String displayText, String command, int sCount) {
      this.preference = "_simpleDeed" + sCount;
      this.addListener(preference);
      button = this.addComboButton(command, displayText);
      button.addActionListener(new SimpleListener(this.preference));
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param command the command to execute.
     * @param maxPref the integer at which to disable the button.
     * @param sCount
     */
    public SimpleDaily(String displayText, String command, int maxPref, int sCount) {
      this.preference = "_simpleDeed" + sCount;
      this.maxPref = maxPref;
      this.addListener(preference);
      button = this.addComboButton(command, displayText);
      button.addActionListener(new SimpleListener(this.preference));
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param command the command to execute.
     * @param maxPref the integer at which to disable the button.
     * @param sCount
     */
    public SimpleDaily(
        String displayText,
        String command,
        int maxPref,
        int sCount,
        String tooltip,
        String compMessage) {
      this.preference = "_simpleDeed" + sCount;
      this.maxPref = maxPref;
      this.compMessage = compMessage;
      this.addListener(preference);
      button = this.addComboButton(command, displayText);
      button.addActionListener(new SimpleListener(this.preference));
      button.setToolTipText(tooltip);
      this.addLabel("");
    }

    @Override
    public void update() {
      int prefToInt = 1;
      boolean hideOnComplete = compMessage.equals("");
      String pref = Preferences.getString(this.preference);
      if (pref.equalsIgnoreCase("true")
          || pref.equalsIgnoreCase("false")
          || pref.equalsIgnoreCase("")) {
        prefToInt = pref.equalsIgnoreCase("true") ? 1 : 0;
      } else {
        try {
          prefToInt = Integer.parseInt(pref);
        } catch (NumberFormatException e) {
        }
      }
      this.setEnabled(true);
      if (prefToInt >= this.maxPref) {
        if (hideOnComplete) {
          this.setShown(false);
          return;
        }
        this.setShown(true);
        this.setText(compMessage);
        button.setVisible(false);
        return;
      }
      this.setShown(true);
      this.setText("");
      button.setVisible(true);
      if (this.maxPref > 1) {
        this.setText(prefToInt + "/" + this.maxPref);
      }
    }

    private class SimpleListener implements ActionListener {
      String preference;

      public SimpleListener(String pref) {
        this.preference = pref;
      }

      @Override
      public void actionPerformed(ActionEvent arg0) {
        String pref = this.preference;
        int value = Preferences.getInteger(pref);
        Preferences.setInteger(pref, ++value);
      }
    }
  }

  public static class CommandDaily extends Daily {
    JButton button;
    String preference;
    int maxPref = 1;
    String compMessage = "";

    /**
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param command the command to execute. This will also be the displayed button text.
     */
    public CommandDaily(String preference, String command) {
      this.preference = preference;
      this.addListener(preference);
      button = this.addComboButton(command, command);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param command the command to execute.
     */
    public CommandDaily(String displayText, String preference, String command) {
      this.preference = preference;
      this.addListener(preference);
      button = this.addComboButton(command, displayText);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param command the command to execute.
     * @param maxPref the integer at which to disable the button.
     */
    public CommandDaily(String displayText, String preference, String command, int maxPref) {
      this.preference = preference;
      this.maxPref = maxPref;
      this.addListener(preference);
      button = this.addComboButton(command, displayText);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param command the command to execute.
     * @param maxPref the integer at which to disable the button.
     * @param toolTip tooltip to display for button on mouseover, for extended information.
     */
    public CommandDaily(
        String displayText, String preference, String command, int maxPref, String toolTip) {
      this.preference = preference;
      this.maxPref = maxPref;
      this.addListener(preference);
      button = this.addComboButton(command, displayText);
      button.setToolTipText(toolTip);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param command the command to execute.
     * @param maxPref the integer at which to disable the button.
     * @param toolTip tooltip to display for button on mouseover, for extended information.
     * @param compMessage message to display on completion
     */
    public CommandDaily(
        String displayText,
        String preference,
        String command,
        int maxPref,
        String toolTip,
        String compMessage) {
      this.preference = preference;
      this.maxPref = maxPref;
      this.compMessage = compMessage;
      this.addListener(preference);
      button = this.addComboButton(command, displayText);
      button.setToolTipText(toolTip);
      this.addLabel("");
    }

    @Override
    public void update() {
      int prefToInt = 1;
      String pref = Preferences.getString(this.preference);
      boolean hideOnComplete = compMessage.equals("");
      if (pref.equalsIgnoreCase("true")
          || pref.equalsIgnoreCase("false")
          || pref.equalsIgnoreCase("")) {
        prefToInt = pref.equalsIgnoreCase("true") ? 1 : 0;
      } else {
        try {
          prefToInt = Integer.parseInt(pref);
        } catch (NumberFormatException e) {
        }
      }
      this.setEnabled(true);
      if (prefToInt >= this.maxPref) {
        if (hideOnComplete) {
          this.setShown(false);
          return;
        }
        this.setShown(true);
        this.setText(compMessage);
        button.setVisible(false);
        return;
      }
      this.setShown(true);
      this.setText("");
      button.setVisible(true);
      if (this.maxPref > 1) {
        this.setText(prefToInt + "/" + this.maxPref);
      }
    }
  }

  public static class ItemDaily extends Daily {
    JButton button;
    String preference;
    int itemId;
    int maxUses = 1;
    String compMessage = "";

    /**
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param itemId the ID of the item. the item is used to set the visibility of the element.
     * @param command the command to execute.
     */
    public ItemDaily(String preference, int itemId, String command) {
      this.preference = preference;
      this.itemId = itemId;
      this.addItem(itemId);
      this.addListener(preference);
      this.addListener("(character)");
      button = this.addComboButton(command, command);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param itemId the ID of the item. the item is used to set the visibility of the element.
     * @param command the command to execute.
     */
    public ItemDaily(String displayText, String preference, int itemId, String command) {
      this.preference = preference;
      this.itemId = itemId;
      this.addItem(itemId);
      this.addListener(preference);
      this.addListener("(character)");
      button = this.addComboButton(command, displayText);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param itemId the ID of the item. the item is used to set the visibility of the element.
     * @param command the command to execute.
     * @param maxUses maximum number of uses of the item per day.
     */
    public ItemDaily(
        String displayText, String preference, int itemId, String command, int maxUses) {
      this.preference = preference;
      this.itemId = itemId;
      this.maxUses = maxUses;
      this.addItem(itemId);
      this.addListener(preference);
      this.addListener("(character)");
      button = this.addComboButton(command, displayText);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param itemId the ID of the item. the item is used to set the visibility of the element.
     * @param command the command to execute.
     * @param maxUses maximum number of uses of the item per day.
     * @param toolTip tooltip to display for button on mouseover, for extended information.
     */
    public ItemDaily(
        String displayText,
        String preference,
        int itemId,
        String command,
        int maxUses,
        String toolTip) {
      this.preference = preference;
      this.itemId = itemId;
      this.maxUses = maxUses;
      this.addItem(itemId);
      this.addListener(preference);
      this.addListener("(character)");
      button = this.addComboButton(command, displayText);
      button.setToolTipText(toolTip);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param itemId the ID of the item. the item is used to set the visibility of the element.
     * @param command the command to execute.
     * @param maxUses maximum number of uses of the item per day.
     * @param toolTip tooltip to display for button on mouseover, for extended information.
     * @param compMessage message to display on completion.
     */
    public ItemDaily(
        String displayText,
        String preference,
        int itemId,
        String command,
        int maxUses,
        String toolTip,
        String compMessage) {
      this.preference = preference;
      this.itemId = itemId;
      this.maxUses = maxUses;
      this.compMessage = compMessage;
      this.addItem(itemId);
      this.addListener(preference);
      this.addListener("(character)");
      button = this.addComboButton(command, displayText);
      button.setToolTipText(toolTip);
      this.addLabel("");
    }

    @Override
    public void update() {

      int prefToInt = 1;
      String pref = Preferences.getString(this.preference);
      boolean hideOnComplete = compMessage.equals("");
      boolean haveItem = InventoryManager.getCount(this.itemId) > 0;

      if (pref.equalsIgnoreCase("true")
          || pref.equalsIgnoreCase("false")
          || pref.equalsIgnoreCase("")) {
        prefToInt = pref.equalsIgnoreCase("true") ? 1 : 0;
      } else {
        try {
          prefToInt = Integer.parseInt(pref);
        } catch (NumberFormatException e) {
        }
      }
      this.setShown(prefToInt > 0 || haveItem);

      this.setEnabled(true);
      if (prefToInt >= this.maxUses) {
        if (hideOnComplete) {
          this.setShown(false);
          return;
        }
        this.setText(compMessage);
        button.setVisible(false);
        return;
      }

      this.setEnabled(haveItem && prefToInt < this.maxUses);
      this.setText("");
      button.setVisible(true);
      if (this.maxUses > 1) {
        this.setText(prefToInt + "/" + this.maxUses);
      }
    }
  }

  public static class SkillDaily extends Daily {
    JButton button;
    String preference;
    String skill;
    int maxCasts = 1;
    String compMessage = "";

    /**
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param skill the skill used to set the visibility of the element.
     * @param command the command to execute.
     */
    public SkillDaily(String preference, String skill, String command) {
      this.preference = preference;
      this.skill = skill;
      this.addListener(preference);
      this.addListener("(skill)");
      button = this.addComboButton(command, command);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param skill the skill used to set the visibility of the element.
     * @param command the command to execute.
     */
    public SkillDaily(String displayText, String preference, String skill, String command) {
      this.preference = preference;
      this.skill = skill;
      this.addListener(preference);
      this.addListener("(skill)");
      button = this.addComboButton(command, displayText);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param skill the skill used to set the visibility of the element.
     * @param command the command to execute.
     * @param maxCasts the number of skill uses before the button is disabled.
     */
    public SkillDaily(
        String displayText, String preference, String skill, String command, int maxCasts) {
      this.preference = preference;
      this.skill = skill;
      this.maxCasts = maxCasts;
      this.addListener(preference);
      this.addListener("(skill)");
      button = this.addComboButton(command, displayText);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param skill the skill used to set the visibility of the element.
     * @param command the command to execute.
     * @param maxCasts the number of skill uses before the button is disabled.
     * @param toolTip tooltip to display for button on mouseover, for extended information.
     */
    public SkillDaily(
        String displayText,
        String preference,
        String skill,
        String command,
        int maxCasts,
        String toolTip) {
      this.preference = preference;
      this.skill = skill;
      this.maxCasts = maxCasts;
      this.addListener(preference);
      this.addListener("(skill)");
      button = this.addComboButton(command, displayText);
      button.setToolTipText(toolTip);
      this.addLabel("");
    }

    /**
     * @param displayText the text that will be displayed on the button
     * @param preference the preference to look at. The preference is used to set the availability
     *     of the element.
     * @param skill the skill used to set the visibility of the element.
     * @param command the command to execute.
     * @param maxCasts the number of skill uses before the button is disabled.
     * @param toolTip tooltip to display for button on mouseover, for extended information.
     * @param compMessage message to display on completion.
     */
    public SkillDaily(
        String displayText,
        String preference,
        String skill,
        String command,
        int maxCasts,
        String toolTip,
        String compMessage) {
      this.preference = preference;
      this.skill = skill;
      this.maxCasts = maxCasts;
      this.compMessage = compMessage;
      this.addListener(preference);
      this.addListener("(skill)");
      button = this.addComboButton(command, displayText);
      button.setToolTipText(toolTip);
      this.addLabel("");
    }

    @Override
    public void update() {
      int prefToInt = 1;
      boolean hideOnComplete = compMessage.equals("");
      String pref = Preferences.getString(this.preference);
      if (pref.equalsIgnoreCase("true")
          || pref.equalsIgnoreCase("false")
          || pref.equalsIgnoreCase("")) {
        prefToInt = pref.equalsIgnoreCase("true") ? 1 : 0;
      } else {
        try {
          prefToInt = Integer.parseInt(pref);
        } catch (NumberFormatException e) {
        }
      }
      this.setShown(KoLCharacter.hasSkill(this.skill));
      this.setEnabled(true);
      if (prefToInt >= this.maxCasts) {
        if (hideOnComplete) {
          this.setShown(false);
          return;
        }
        this.setText(compMessage);
        button.setVisible(false);
        return;
      }

      this.setText("");
      button.setVisible(true);
      if (this.maxCasts > 1) {
        this.setText(prefToInt + "/" + this.maxCasts);
      }
    }
  }

  public class TextDeed extends Daily {
    String[] deedsString;

    public TextDeed(String[] deedString) {
      for (int i = 1; i < deedString.length; ++i) {
        if (!KoLCharacter.baseUserName().equals("GLOBAL")
            && !Preferences.getString(deedString[i]).equals("")) {
          this.addListener(deedString[i]);
        }
      }
      this.deedsString = deedString;
      this.addLabel("");
    }

    @Override
    public void update() {
      String text = "";

      for (int i = 1; i < deedsString.length; ++i) {
        if (!KoLCharacter.baseUserName().equals("GLOBAL")
            && !Preferences.getString(deedsString[i]).equals("")) {
          text += Preferences.getString(deedsString[i]);
        } else {
          text += deedsString[i];
        }
      }
      this.setText(text);
    }
  }

  public static class NunsDaily extends Daily {
    JButton button;

    public NunsDaily() {
      this.addListener("nunsVisits");
      this.addListener("sidequestNunsCompleted");
      this.addListener("(character)");
      button = this.addComboButton("Nuns", "nuns");
      this.addLabel("");
    }

    @Override
    public void update() {
      int nv = Preferences.getInteger("nunsVisits");
      boolean snc = Preferences.getString("sidequestNunsCompleted").equals("none");
      boolean limited = Limitmode.limitZone("IsleWar");
      this.setShown(!snc && !limited);
      this.setEnabled(true);
      if (nv >= 3) {
        this.setText("The sisters are too busy right now, try tomorrow");
        button.setVisible(false);
        return;
      }
      this.setText(nv + "/3");
    }
  }

  public static class SkateDaily extends Daily {
    JButton button;
    private final String state, visited;

    public SkateDaily(String name, String state, String visited, String desc) {
      this.state = state;
      this.visited = visited;
      this.addListener("skateParkStatus");
      this.addListener("(character)");
      this.addListener(visited);
      button = this.addComboButton("skate " + name, "skate " + name);
      this.addLabel(desc);
    }

    @Override
    public void update() {
      boolean limited = Limitmode.limitZone("The Sea");
      this.setShown(Preferences.getString("skateParkStatus").equals(this.state) && !limited);
      this.setEnabled(true);
      if (Preferences.getBoolean(this.visited)) {
        this.setText("You have visited the Skate Park today");
        button.setVisible(false);
        return;
      }
    }
  }

  public static class SpadeDaily extends Daily {
    public SpadeDaily() {
      this.addListener("spadingData");
      this.addButton("spade");
      this.addLabel("");
    }

    @Override
    public void update() {
      int ns = Preferences.getString("spadingData").split("\\|").length / 3;
      this.setShown(ns > 0);
      this.setText(ns == 1 ? "one item to submit" : (ns + " items to submit"));
    }
  }

  public static class TelescopeDaily extends Daily {
    JButton button;

    public TelescopeDaily() {
      this.addListener("telescopeLookedHigh");
      this.addListener("telescopeUpgrades");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      button = this.addComboButton("telescope high", "telescope high");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean na = KoLCharacter.inNuclearAutumn();
      boolean kf = KoLCharacter.kingLiberated();
      boolean limited = Limitmode.limitCampground();
      int nu = Preferences.getInteger("telescopeUpgrades");
      this.setShown((!bm || kf) && (nu > 0) && !limited && !na);
      this.setEnabled(nu > 0);
      if (Preferences.getBoolean("telescopeLookedHigh")) {
        this.setText("You have stared into space today");
        button.setVisible(false);
        return;
      }
      this.setText("+" + nu * 5 + "% all, 10 turns");
    }
  }

  public static class ConcertDaily extends Daily {
    JButton button1;
    JButton button2;
    JButton button3;

    public ConcertDaily() {
      this.addListener("concertVisited");
      this.addListener("sidequestArenaCompleted");
      this.addListener("(character)");
      button1 = this.addComboButton("concert ?", "concert ?");
      button2 = this.addComboButton("concert ?", "concert ?");
      button3 = this.addComboButton("concert ?", "concert ?");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean cv = Preferences.getBoolean("concertVisited");
      String side = Preferences.getString("sidequestArenaCompleted");
      boolean limited = Limitmode.limitZone("IsleWar");
      this.setShown((side.equals("fratboy") || side.equals("hippy")) && !limited);
      this.setEnabled(true);
      if (cv) {
        this.setText("You have 'enjoyed' a concert today");
        button1.setVisible(false);
        button2.setVisible(false);
        button3.setVisible(false);
        return;
      }
      if (side.equals("fratboy")) {
        button1.setActionCommand("concert Elvish");
        button1.setText("concert Elvish");
        button1.setToolTipText("+10% all stats, 20 turns");
        button2.setActionCommand("concert Winklered");
        button2.setText("concert Winklered");
        button2.setToolTipText("+40% meat, 20 turns");
        button3.setActionCommand("concert White-boy Angst");
        button3.setText("concert White-boy Angst");
        button3.setToolTipText("+50% initiative, 20 turns");
      } else if (side.equals("hippy")) {
        button1.setActionCommand("concert Moon'd");
        button1.setText("concert Moon'd");
        button1.setToolTipText("+5 stats per fight, 20 turns");
        button2.setActionCommand("concert Dilated Pupils");
        button2.setText("concert Dilated Pupils");
        button2.setToolTipText("+20% items, 20 turns");
        button3.setActionCommand("concert Optimist Primal");
        button3.setText("concert Optimist Primal");
        button3.setToolTipText("+5 lbs., 20 turns");
      }
    }
  }

  public static class RestsDaily extends Daily {
    JButton button;

    public RestsDaily() {
      this.addListener("timesRested");
      this.addListener("(skill)");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      button = this.addComboButton("rest free", "rest free");
      this.addLabel("");
    }

    @Override
    public void update() {
      int nr = Preferences.getInteger("timesRested");
      int fr = KoLCharacter.freeRestsAvailable();
      boolean limited = Limitmode.limitCampground() && Limitmode.limitZone("Mountain");
      this.setShown(fr > 0 && !limited);
      this.setEnabled(true);
      if (nr >= fr) {
        this.setText("You have had all " + fr + " free rests today");
        button.setVisible(false);
        return;
      }
      this.setText(nr + "/" + fr);
      button.setVisible(true);
    }
  }

  public static class FriarsDaily extends Daily {
    JButton food;
    JButton familiar;
    JButton booze;

    public FriarsDaily() {
      this.addListener("friarsBlessingReceived");
      this.addListener("lastFriarCeremonyAscension");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      food = this.addComboButton("friars food", "friars food");
      familiar = this.addComboButton("friars familiar", "friars familiar");
      booze = this.addComboButton("friars booze", "friars booze");
      food.setToolTipText("+30% food drops, 20 turns");
      familiar.setToolTipText("+2 familiar exp per fight, 20 turns");
      booze.setToolTipText("+30% booze drops, 20 turns");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean kf = KoLCharacter.kingLiberated();
      boolean fqc = QuestDatabase.isQuestFinished(Quest.FRIAR);
      int lfc = Preferences.getInteger("lastFriarCeremonyAscension");
      int ka = Preferences.getInteger("knownAscensions");
      boolean limited = Limitmode.limitZone("Friars");
      this.setShown((kf || lfc == ka) && !limited && fqc);
      this.setEnabled(true);
      if (Preferences.getBoolean("friarsBlessingReceived")) {
        this.setText("You have had a friar's blessing today");
        food.setVisible(false);
        familiar.setVisible(false);
        booze.setVisible(false);
        return;
      }
    }
  }

  public static class MomCombo extends Daily {
    DisabledItemsComboBox<String> box = new DisabledItemsComboBox<>();
    Component space;
    JButton btn = null;

    public MomCombo() {
      int len = MomRequest.FOOD.length;
      List<String> ttips = new ArrayList<String>();
      String[] choices = new String[len + 1];
      choices[0] = "Mom Food";
      String[] tips = {
        "Get Food from Mom",
        "+7 Hot Resist, 50 turns",
        "+7 Cold Resist, 50 turns",
        "+7 Stench Resist, 50 turns",
        "+7 Spooky Resist, 50 turns",
        "+7 Sleaze Resist, 50 turns",
        "+20% critical chance, 50 turns",
        "+200 stats per fight, 50 turns",
      };

      for (int i = 1; i <= len; ++i) {
        choices[i] = MomRequest.EFFECT[i - 1];
      }

      ttips.addAll(Arrays.asList(tips));

      this.addListener("_momFoodReceived");
      this.addListener("questS02Monkees");
      this.addListener("(character)");

      box = this.addComboBox(choices, ttips, "Get Food from Mom");
      box.addActionListener(new MomComboListener());
      space = this.add(Box.createRigidArea(new Dimension(5, 1)));

      // Initialize the GO button to do nothing.
      btn = this.addComboButton("", "Go!");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean limited = Limitmode.limitZone("The Sea");
      this.setShown(QuestDatabase.isQuestFinished(Quest.SEA_MONKEES) && !limited);
      this.setEnabled(true);
      if (Preferences.getBoolean("_momFoodReceived")) {
        this.setText("You have had some of Mom's food today");
        box.setVisible(false);
        space.setVisible(false);
        btn.setVisible(false);
        return;
      }
    }

    private class MomComboListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();
        int index = cb.getSelectedIndex();
        if (index < 2) {
          setComboTarget(btn, "");
        } else {
          String Choice = MomRequest.FOOD[index - 1];
          setComboTarget(btn, "mom " + Choice);
        }
      }
    }
  }

  public static class StyxDaily extends Daily {
    JButton btnMus;
    JButton btnMys;
    JButton btnMox;

    public StyxDaily() {
      this.addListener("styxPixieVisited");
      this.addListener("(character)");
      btnMus = this.addComboButton("styx muscle", "styx muscle");
      btnMys = this.addComboButton("styx mysticality", "styx mysticality");
      btnMox = this.addComboButton("styx moxie", "styx moxie");
      btnMus.setToolTipText("+25% musc, +10 weapon dmg, +5 DR, 10 turns");
      btnMys.setToolTipText("+25% myst, +15 spell dmg, 10-15 MP regen, 10 turns");
      btnMox.setToolTipText("+25% mox, +40% meat, +20% item, 10 turns");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean limited = Limitmode.limitZone("BadMoon");
      this.setShown(bm && !limited);
      this.setEnabled(!Preferences.getBoolean("styxPixieVisited") && bm);
      if (Preferences.getBoolean("styxPixieVisited")) {
        this.setText("You have sampled the Styx today");
        btnMus.setVisible(false);
        btnMys.setVisible(false);
        btnMox.setVisible(false);
        return;
      }
    }
  }

  public static class MojoDaily extends Daily {
    JButton button;

    public MojoDaily() {
      this.addListener("currentMojoFilters");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      this.addItem(ItemPool.MOJO_FILTER);
      button = this.addComboButton("use mojo filter", "use mojo filter");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean have = InventoryManager.getCount(ItemPool.MOJO_FILTER) > 0;
      int nf = Preferences.getInteger("currentMojoFilters");
      this.setShown(have || nf > 0);
      this.setEnabled(true);
      if (nf >= 3) {
        this.setText("You can handle no more mojo filtering today");
        button.setVisible(false);
        return;
      }
      this.setText(nf + "/3");
    }
  }

  public static class HotTubDaily extends Daily {
    JButton button;

    public HotTubDaily() {
      this.addItem(ItemPool.VIP_LOUNGE_KEY);
      this.addListener("_hotTubSoaks");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      button = this.addComboButton("hottub", "hottub");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0;
      boolean limited = Limitmode.limitClan();
      int nf = Preferences.getInteger("_hotTubSoaks");
      this.setShown((!bm || kf) && (have || nf > 0) && !limited);
      this.setEnabled(true);
      if (nf >= 5) {
        this.setText("You've spent enough time in the hot tub today");
        button.setVisible(false);
        return;
      }
      this.setText(nf + "/5");
    }
  }

  public static class PoolDaily extends Daily {
    JButton pool1;
    JButton pool2;
    JButton pool3;

    public PoolDaily() {
      this.addItem(ItemPool.VIP_LOUNGE_KEY);
      this.addListener("_poolGames");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      pool1 = this.addComboButton("pool 1", "pool 1");
      pool2 = this.addComboButton("pool 2", "pool 2");
      pool3 = this.addComboButton("pool 3", "pool 3");
      pool1.setToolTipText("weapon dmg +50%, +5 lbs, 10 turns");
      pool2.setToolTipText("spell dmg +50%, 10 MP per Adv, 10 turns");
      pool3.setToolTipText("init +50%, +10% item, 10 turns");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0;
      boolean allowed = StandardRequest.isAllowed("Clan Item", "Pool Table");
      boolean limited = Limitmode.limitClan();
      int nf = Preferences.getInteger("_poolGames");
      this.setShown((!bm || kf) && (have || nf > 0) && allowed && !limited);
      this.setEnabled(true);
      if (nf >= 3) {
        this.setText("You've played enough pool today");
        pool1.setVisible(false);
        pool2.setVisible(false);
        pool3.setVisible(false);
        return;
      }
      this.setText(nf + "/3");
    }
  }

  public static class CrimboTreeDaily extends Daily {
    JButton button;

    public CrimboTreeDaily() {
      this.addListener("_crimboTree");
      this.addListener("crimboTreeDays");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      button = this.addComboButton("crimbotree get", "crimbotree get");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean tree = Preferences.getBoolean("_crimboTree");
      boolean allowed = StandardRequest.isAllowed("Clan Item", "Crimbo Tree");
      boolean limited = Limitmode.limitClan();
      int ctd = Preferences.getInteger("crimboTreeDays");
      this.setShown((!bm || kf) && tree && allowed && !limited);
      this.setEnabled(true);
      if (ctd > 0) {
        if (ctd == 1) {
          this.setText("\"Crimbo\" is tomorrow!");
        } else {
          this.setText(ctd + " days to go til \"Crimbo\"");
        }
        button.setVisible(false);
        return;
      }
      this.setText("\"Crimbo\" is here!");
    }
  }

  public static class MelangeDaily extends Daily {
    public MelangeDaily() {
      this.addItem(ItemPool.SPICE_MELANGE);
      this.addListener("spiceMelangeUsed");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      int have = InventoryManager.getCount(ItemPool.SPICE_MELANGE);
      if (Preferences.getBoolean("spiceMelangeUsed")) {
        this.setShown(true);
        this.setText("SPICE MELANGE USED, have " + have);
      } else {
        this.setShown(have > 0);
        this.setText("spice melange not used, have " + have);
      }
    }
  }

  public static class UltraMegaSourBallDaily extends Daily {
    public UltraMegaSourBallDaily() {
      this.addItem(ItemPool.ULTRA_MEGA_SOUR_BALL);
      this.addListener("_ultraMegaSourBallUsed");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      int have = InventoryManager.getCount(ItemPool.ULTRA_MEGA_SOUR_BALL);
      if (Preferences.getBoolean("_ultraMegaSourBallUsed")) {
        this.setShown(true);
        this.setText("ULTRA MEGA SOUR BALL USED, have " + have);
      } else {
        this.setShown(have > 0);
        this.setText("ultra mega sour ball not used, have " + have);
      }
    }
  }

  public static class StillsDaily extends Daily {
    public StillsDaily() {
      this.addListener("(stills)");
      this.addListener("kingLiberated");
      this.addListener("lastGuildStoreOpen");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      this.setShown(
          (KoLCharacter.isMoxieClass()
                  && KoLCharacter.hasSkill("Superhuman Cocktailcrafting")
                  && KoLCharacter.getGuildStoreOpen())
              || KoLCharacter.hasSkill("Mixologist"));
      this.setText((10 - KoLCharacter.getStillsAvailable()) + "/10 stills used");
    }
  }

  public static class TeaPartyDaily extends Daily {
    public TeaPartyDaily() {
      this.addItem(ItemPool.DRINK_ME_POTION);
      this.addListener("_madTeaParty");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean limited = Limitmode.limitZone("RabbitHole");
      int have = InventoryManager.getCount(ItemPool.DRINK_ME_POTION);
      if (Preferences.getBoolean("_madTeaParty")) {
        this.setShown((!bm || kf) && !limited);
        if (have == 1) {
          this.setText("Mad Tea Party used, have " + have + " potion");
        } else {
          this.setText("Mad Tea Party used, have " + have + " potions");
        }
      } else {
        this.setShown(have > 0 && !limited);
        if (have == 1) {
          this.setText("Mad Tea Party not used, have " + have + " potion");
        } else {
          this.setText("Mad Tea Party not used, have " + have + " potions");
        }
      }
    }
  }

  public static class FreeFightsDaily extends Daily {
    public FreeFightsDaily() {
      this.addListener("_brickoFights");
      this.addListener("_hipsterAdv");
      this.addListener("_sealsSummoned");
      this.addListener("_machineTunnelsAdv");
      this.addListener("_snojoFreeFights");
      this.addListener("_witchessFights");
      this.addListener("_eldritchTentacleFought");
      this.addListener("_godLobsterFights");
      this.addListener("_loveTunnelUsed");
      this.addListener("_neverendingPartyFreeTurns");
      this.addListener("_voteFreeFights");
      this.addListener("(character)");
      this.addLabel("");
    }

    private int count;
    private boolean shown;
    private static final int fightsPerLine = 4;

    private void addFightCounter(StringBuilder buffer, String text) {
      if (count >= fightsPerLine) {
        buffer.append("<br>");
        count = 0;
      }

      if (count == 0) {
        buffer.append("Fights: ");
      } else if (count < fightsPerLine) {
        buffer.append(", ");
      }
      buffer.append(text);
      ++count;
      shown = true;
    }

    @Override
    public void update() {
      boolean limited = Limitmode.limitMall();
      boolean bf =
          (!KoLCharacter.isHardcore()
                  && !limited
                  && StandardRequest.isAllowed("Items", "Libram of BRICKOs"))
              || KoLCharacter.hasSkill("Summon BRICKOs");
      FamiliarData hipster = KoLCharacter.findFamiliar(FamiliarPool.HIPSTER);
      FamiliarData goth = KoLCharacter.findFamiliar(FamiliarPool.ARTISTIC_GOTH_KID);
      FamiliarData machineElf = KoLCharacter.findFamiliar(FamiliarPool.MACHINE_ELF);
      FamiliarData godLobster = KoLCharacter.findFamiliar(FamiliarPool.GOD_LOBSTER);
      boolean hh = hipster != null && hipster.canEquip();
      boolean hg = goth != null && goth.canEquip();
      boolean hf = hh || hg;
      String ff = "";
      if (hh && hg) ff = "hipster+goth";
      else if (hh) ff = "hipster";
      else if (hg) ff = "goth";
      boolean sc = KoLCharacter.isSealClubber();
      boolean me = machineElf != null && machineElf.canEquip();
      boolean gl = godLobster != null && godLobster.canEquip();
      boolean sj =
          Preferences.getBoolean("snojoAvailable")
              && StandardRequest.isAllowed("Items", "X-32-F snowman crate")
              && !Limitmode.limitZone("The Snojo")
              && !KoLCharacter.inBadMoon();
      boolean wc =
          KoLConstants.campground.contains(ItemPool.get(ItemPool.WITCHESS_SET, 1))
              && StandardRequest.isAllowed("Items", "Witchess Set")
              && !Limitmode.limitCampground()
              && !KoLCharacter.inBadMoon();
      boolean et = !(Preferences.getBoolean("_eldritchTentacleFought"));
      boolean lv =
          Preferences.getBoolean("loveTunnelAvailable")
              && StandardRequest.isAllowed("Items", "LOV Entrance Pass")
              && !Limitmode.limitZone("Town")
              && !KoLCharacter.inBadMoon();
      boolean np =
          Preferences.getBoolean("neverendingPartyAlways")
              && StandardRequest.isAllowed("Items", "Neverending Party invitation envelope")
              && !Limitmode.limitZone("Town")
              && !KoLCharacter.inBadMoon();
      boolean vb =
          (Preferences.getBoolean("_voteToday") || Preferences.getBoolean("voteAlways"))
              && StandardRequest.isAllowed("Items", "voter registration form")
              && !Limitmode.limitZone("Town")
              && !KoLCharacter.inBadMoon();

      StringBuilder buffer = new StringBuilder();
      count = 0;
      shown = false;

      buffer.append("<html>");

      int maxSummons = 5;
      if (KoLCharacter.hasEquipped(DailyDeedsPanel.INFERNAL_SEAL_CLAW)
          || DailyDeedsPanel.INFERNAL_SEAL_CLAW.getCount(KoLConstants.inventory) > 0) {
        maxSummons = 10;
      }
      if (bf) addFightCounter(buffer, Preferences.getInteger("_brickoFights") + "/10 BRICKO");
      if (hf) addFightCounter(buffer, Preferences.getInteger("_hipsterAdv") + "/7 " + ff);
      if (sc)
        addFightCounter(
            buffer,
            Preferences.getInteger("_sealsSummoned") + "/" + maxSummons + " seals summoned");
      if (me)
        addFightCounter(buffer, Preferences.getInteger("_machineTunnelsAdv") + "/5 machine elf");
      if (sj) addFightCounter(buffer, Preferences.getInteger("_snojoFreeFights") + "/10 snojo");
      if (wc) addFightCounter(buffer, Preferences.getInteger("_witchessFights") + "/5 witchess");
      if (gl)
        addFightCounter(buffer, Preferences.getInteger("_godLobsterFights") + "/3 god lobster");
      if (lv)
        addFightCounter(buffer, (Preferences.getBoolean("_loveTunnelUsed") ? 3 : 0) + "/3 lov");
      if (np)
        addFightCounter(buffer, Preferences.getInteger("_neverendingPartyFreeTurns") + "/10 party");
      if (vb) addFightCounter(buffer, Preferences.getInteger("_voteFreeFights") + "/3 vote");
      if (et) addFightCounter(buffer, "tentacle");
      buffer.append("</html>");

      this.setShown(shown);
      this.setText(buffer.toString());
    }
  }

  public static class RunawaysDaily extends Daily {
    public RunawaysDaily() {
      this.addListener("_banderRunaways");
      this.addListener("_navelRunaways");
      this.addListener("_petePeeledOut");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      FamiliarData bander = KoLCharacter.findFamiliar(FamiliarPool.BANDER);
      boolean hba = bander != null && bander.canEquip();
      FamiliarData boots = KoLCharacter.findFamiliar(FamiliarPool.BOOTS);
      boolean hbo = boots != null && boots.canEquip();
      boolean run = Preferences.getInteger("_navelRunaways") > 0;
      boolean gp =
          InventoryManager.getCount(ItemPool.GREAT_PANTS) > 0
              || KoLCharacter.hasEquipped(DailyDeedsPanel.GREAT_PANTS);
      boolean nr =
          InventoryManager.getCount(ItemPool.NAVEL_RING) > 0
              || KoLCharacter.hasEquipped(DailyDeedsPanel.NAVEL_RING);
      boolean pp = InventoryManager.getCount(ItemPool.PEPPERMINT_PARASOL) > 0;
      boolean pl = KoLCharacter.hasSkill(SkillPool.PEEL_OUT);
      boolean big = KoLCharacter.inBigcore();
      boolean shown = !big && (hba || hbo || gp || nr || pp || pl);
      this.setShown(shown);
      if (!shown) return;
      boolean needComma = false;
      String text = "Runaways: ";
      if (hba && !hbo) {
        text += Preferences.getInteger("_banderRunaways") + " bandersnatch";
        needComma = true;
      }
      if (hba && hbo) {
        text += Preferences.getInteger("_banderRunaways") + " bandersnatch+boots";
        needComma = true;
      }
      if (hbo && !hba) {
        text += Preferences.getInteger("_banderRunaways") + " stomping boots";
        needComma = true;
      }
      if (needComma && (run || gp || nr || pp)) {
        text += ", ";
        needComma = false;
      }
      if (run && !nr && !gp && !pp) {
        text += Preferences.getInteger("_navelRunaways") + " navel ring";
        needComma = true;
      }
      if (nr && !gp && !pp) {
        text += Preferences.getInteger("_navelRunaways") + " navel ring";
        needComma = true;
      }
      if (nr && !gp && pp) {
        text += Preferences.getInteger("_navelRunaways") + " navel+parasol";
        needComma = true;
      }
      if (nr && gp && !pp) {
        text += Preferences.getInteger("_navelRunaways") + " gap+navel";
        needComma = true;
      }
      if (nr && gp && pp) {
        text += Preferences.getInteger("_navelRunaways") + " gap+navel+parasol";
        needComma = true;
      }
      if (!nr && gp && !pp) {
        text += Preferences.getInteger("_navelRunaways") + " gap pants";
        needComma = true;
      }
      if (!nr && gp && pp) {
        text += Preferences.getInteger("_navelRunaways") + " gap+parasol";
        needComma = true;
      }
      if (!nr && !gp && pp) {
        text += Preferences.getInteger("_navelRunaways") + " peppermint parasol";
        needComma = true;
      }
      if (needComma && pl) {
        text += ", ";
        needComma = false;
      }
      if (pl) {
        text += Preferences.getInteger("_petePeeledOut") + " peelouts";
        needComma = true;
      }
      this.setText(text);
    }
  }

  public static class DropsDaily extends Daily {
    public DropsDaily() {
      for (FamiliarData.DropInfo info : FamiliarData.DROP_FAMILIARS) {
        this.addListener(info.dropTracker);
      }
      this.addListener("_carrotNoseDrops");
      this.addListener("_mediumSiphons");
      this.addListener("_pieDrops");
      this.addListener("_piePartsCount");
      this.addListener("_bootStomps");
      this.addListener("bootsCharged");
      this.addListener("_grimstoneMaskDropsCrown");
      this.addListener("_grimFairyTaleDropsCrown");
      this.addListener("(character)");
      this.addItem(ItemPool.SNOW_SUIT);
      this.addLabel("");
    }

    private int count;
    private boolean shown;
    private static final int dropsPerLine = 4;

    private void addDropCounter(StringBuilder buffer, String text) {
      if (count >= dropsPerLine) {
        buffer.append("<br>");
        count = 0;
      }

      if (count == 0) {
        buffer.append("Drops: ");
      } else if (count < dropsPerLine) {
        buffer.append(", ");
      }
      buffer.append(text);
      ++count;
      shown = true;
    }

    @Override
    public void update() {
      StringBuilder buffer = new StringBuilder();
      count = 0;
      shown = false;

      buffer.append("<html>");

      HashSet<String> dropTrackers = new HashSet<String>();
      for (FamiliarData.DropInfo info : FamiliarData.DROP_FAMILIARS) {
        if (!dropTrackers.contains(info.dropTracker)) {
          FamiliarData fam = KoLCharacter.findFamiliar(info.id);
          if (fam != null && fam.canEquip()) {
            dropTrackers.add(info.dropTracker);
            StringBuilder addition = new StringBuilder();
            int drops = info.dropsToday();
            addition.append(drops);
            if (info.dailyCap != -1) {
              addition.append("/");
              addition.append(info.dailyCap);
            }
            addition.append(" ");
            addition.append(info.dropName);
            addDropCounter(buffer, addition.toString());
          }
        }
      }

      boolean snowsuit =
          InventoryManager.getCount(ItemPool.SNOW_SUIT) > 0
              || KoLCharacter.hasEquipped(DailyDeedsPanel.SNOW_SUIT);
      int noseDrops = Preferences.getInteger("_carrotNoseDrops");
      if (snowsuit || noseDrops > 0) {
        addDropCounter(buffer, noseDrops + " carrot nose");
      }

      FamiliarData grinder = KoLCharacter.findFamiliar(FamiliarPool.GRINDER);
      int pieDrops = Preferences.getInteger("_pieDrops");

      if (grinder != null && (grinder.canEquip() || pieDrops > 0)) {
        StringBuilder addition = new StringBuilder();
        addition.append(pieDrops);
        if (pieDrops == 1) {
          addition.append(" pie (");
        } else {
          addition.append(" pies (");
        }
        addition.append(Preferences.getString("_piePartsCount"));
        addition.append("/");
        int drops = Preferences.getInteger("_pieDrops");
        int need;
        if (drops < 1) {
          need = 5;
        } else {
          drops -= 1;
          need = 5 + (10 + drops) * (drops + 1) / 2;
          need = Math.min(need, 50);
          AdventureResult item = grinder.getItem();
          if (item != null && item.getItemId() == ItemPool.MICROWAVE_STOGIE) {
            need -= 5;
          }
        }
        addition.append(need);
        addition.append(")");
        addDropCounter(buffer, addition.toString());
      }

      FamiliarData hm = KoLCharacter.findFamiliar(FamiliarPool.HAPPY_MEDIUM);
      int mediumSiphons = Preferences.getInteger("_mediumSiphons");
      if ((hm != null && hm.canEquip()) || mediumSiphons > 0) {
        addDropCounter(buffer, mediumSiphons + " siphon" + (mediumSiphons != 1 ? "s" : ""));
      }

      FamiliarData boots = KoLCharacter.findFamiliar(FamiliarPool.BOOTS);
      if (boots != null && boots.canEquip()) {
        StringBuilder addition = new StringBuilder();
        addition.append(Preferences.getString("_bootStomps"));
        addition.append(" stomp");
        if (Preferences.getInteger("_bootStomps") != 1) addition.append("s");
        if (Preferences.getBoolean("bootsCharged")) addition.append(" (C)");
        addDropCounter(buffer, addition.toString());
      }

      buffer.append("</html>");

      this.setShown(shown);
      this.setText(buffer.toString());
    }
  }

  public static class AdvsDaily extends Daily {
    public AdvsDaily() {
      // this.addItem( ItemPool.TIME_HELMET );
      // this.addItem( ItemPool.V_MASK );
      this.addListener("_gibbererAdv");
      this.addListener("_hareAdv");
      this.addListener("_riftletAdv");
      this.addListener("_timeHelmetAdv");
      this.addListener("_vmaskAdv");
      this.addListener("_gnomeAdv");
      this.addListener("_mafiaThumbRingAdvs");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      FamiliarData gibberer = KoLCharacter.findFamiliar(FamiliarPool.GIBBERER);
      boolean hf1 = gibberer != null && gibberer.canEquip();
      FamiliarData hare = KoLCharacter.findFamiliar(FamiliarPool.HARE);
      boolean hf2 = hare != null && hare.canEquip();
      FamiliarData riftlet = KoLCharacter.findFamiliar(FamiliarPool.RIFTLET);
      boolean hf3 = riftlet != null && riftlet.canEquip();
      boolean hf4 =
          InventoryManager.getCount(ItemPool.TIME_HELMET) > 0
              || Preferences.getInteger("_timeHelmetAdv") > 0
              || KoLCharacter.hasEquipped(ItemPool.TIME_HELMET);
      boolean hf5 =
          InventoryManager.getCount(ItemPool.V_MASK) > 0
              || Preferences.getInteger("_vmaskAdv") > 0
              || KoLCharacter.hasEquipped(ItemPool.V_MASK);
      FamiliarData gnome = KoLCharacter.findFamiliar(FamiliarPool.REAGNIMATED_GNOME);
      boolean hf6 = gnome != null && gnome.canEquip();
      boolean hf7 =
          InventoryManager.getCount(ItemPool.MAFIA_THUMB_RING) > 0
              || Preferences.getInteger("_mafiaThumbRingAdvs") > 0
              || KoLCharacter.hasEquipped(ItemPool.MAFIA_THUMB_RING);
      String text = "Advs: ";
      if (hf1) text = text + Preferences.getInteger("_gibbererAdv") + " gibberer";
      if (hf1 && (hf2 || hf3 || hf4 || hf5 || hf6 || hf7)) text = text + ", ";
      if (hf2) text = text + Preferences.getInteger("_hareAdv") + " hare";
      if (hf2 && (hf3 || hf4 || hf5 || hf6 || hf7)) text = text + ", ";
      if (hf3) text = text + Preferences.getInteger("_riftletAdv") + " riftlet";
      if (hf3 && (hf4 || hf5 || hf6 || hf7)) text = text + ", ";
      if (hf4) text = text + Preferences.getInteger("_timeHelmetAdv") + " time helmet";
      if (hf4 && (hf5 || hf6 || hf7)) text = text + ", ";
      if (hf5) text = text + Preferences.getInteger("_vmaskAdv") + " V mask";
      if (hf5 && (hf6 || hf7)) text = text + ", ";
      if (hf6) text = text + Preferences.getInteger("_gnomeAdv") + " gnome";
      if (hf6 && hf7) text = text + ", ";
      if (hf7) text = text + Preferences.getInteger("_mafiaThumbRingAdvs") + " thumb ring";
      this.setShown(hf1 || hf2 || hf3 || hf4 || hf5 || hf6 || hf7);
      this.setText(text);
    }
  }

  public static class PuttyDaily extends Daily {
    public PuttyDaily() {
      this.addListener("spookyPuttyCopiesMade");
      this.addListener("spookyPuttyMonster");
      this.addListener("_raindohCopiesMade");
      this.addListener("rainDohMonster");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      this.addItem(ItemPool.SPOOKY_PUTTY_SHEET);
      this.addItem(ItemPool.RAIN_DOH_BOX);
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean kf = KoLCharacter.kingLiberated();
      boolean hc = KoLCharacter.isHardcore();
      boolean hadPutty = Preferences.getInteger("spookyPuttyCopiesMade") > 0;
      boolean hadRainDoh = Preferences.getInteger("_raindohCopiesMade") > 0;
      boolean shown = false;
      boolean havePutty =
          InventoryManager.getCount(ItemPool.SPOOKY_PUTTY_MITRE) > 0
              || InventoryManager.getEquippedCount(ItemPool.get(ItemPool.SPOOKY_PUTTY_MITRE, 1)) > 0
              || InventoryManager.getCount(ItemPool.SPOOKY_PUTTY_LEOTARD) > 0
              || InventoryManager.getEquippedCount(ItemPool.get(ItemPool.SPOOKY_PUTTY_LEOTARD, 1))
                  > 0
              || InventoryManager.getCount(ItemPool.SPOOKY_PUTTY_BALL) > 0
              || InventoryManager.getEquippedCount(ItemPool.get(ItemPool.SPOOKY_PUTTY_BALL, 1)) > 0
              || InventoryManager.getCount(ItemPool.SPOOKY_PUTTY_SHEET) > 0
              || InventoryManager.getCount(ItemPool.SPOOKY_PUTTY_SNAKE) > 0
              || InventoryManager.getEquippedCount(ItemPool.get(ItemPool.SPOOKY_PUTTY_SNAKE, 1)) > 0
              || InventoryManager.getCount(ItemPool.SPOOKY_PUTTY_MONSTER) > 0;
      boolean haveRainDoh =
          InventoryManager.getCount(ItemPool.RAIN_DOH_BOX) > 0
              || InventoryManager.getCount(ItemPool.RAIN_DOH_MONSTER) > 0;
      String text = "";

      if (havePutty || hadPutty) {
        text += Preferences.getInteger("spookyPuttyCopiesMade") + "/";
        text += Math.min(5, 6 - Preferences.getInteger("_raindohCopiesMade")) + " ";
        text += "putty uses";
        String monster = Preferences.getString("spookyPuttyMonster");
        if (!monster.equals("")) {
          text += ", now " + monster;
        }
        shown = true;
      }
      if (haveRainDoh || hadRainDoh) {
        if (shown) text += "; ";
        text += Preferences.getInteger("_raindohCopiesMade") + "/";
        text += Math.min(5, 6 - Preferences.getInteger("spookyPuttyCopiesMade")) + " ";
        text += "rain-doh uses";
        String monster = Preferences.getString("rainDohMonster");
        if (!monster.equals("")) {
          text += ", now " + monster;
        }
      }
      this.setShown((kf || !hc) && (hadPutty || havePutty || haveRainDoh || hadRainDoh));
      this.setText(text);
    }
  }

  public static class CameraDaily extends Daily {
    public CameraDaily() {
      this.addListener("_cameraUsed");
      this.addListener("cameraMonster");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      String text =
          Preferences.getBoolean("_cameraUsed") ? "4-d camera used" : "4-d camera not used yet";
      String monster = Preferences.getString("cameraMonster");
      if (!monster.equals("")) {
        text = text + ", now " + monster;
      }
      this.setText(text);
    }
  }

  public static class RomanticDaily extends Daily {
    public RomanticDaily() {
      this.addListener("_badlyRomanticArrows");
      this.addListener("_romanticFightsLeft");
      this.addListener("romanticTarget");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      FamiliarData angel = KoLCharacter.findFamiliar(FamiliarPool.OBTUSE_ANGEL);
      FamiliarData reanimator = KoLCharacter.findFamiliar(FamiliarPool.REANIMATOR);
      boolean show =
          (angel != null && angel.canEquip()) || (reanimator != null && reanimator.canEquip());
      String text = "";
      if (show) {
        text =
            Preferences.getInteger("_badlyRomanticArrows") > 0
                ? "Romantic Arrow used"
                : "Romantic Arrow not used yet";
        String monster = Preferences.getString("romanticTarget");
        int left = Preferences.getInteger("_romanticFightsLeft");
        if (!monster.equals("") && left > 0) {
          text = text + ", now " + monster + " (" + left + " left)";
        }
      }
      this.setText(text);
      this.setShown(show);
    }
  }

  public static class PhotocopyDaily extends Daily {
    public PhotocopyDaily() {
      this.addItem(ItemPool.VIP_LOUNGE_KEY);
      this.addItem(ItemPool.PHOTOCOPIER);
      this.addItem(ItemPool.PHOTOCOPIED_MONSTER);
      this.addListener("_photocopyUsed");
      this.addListener("photocopyMonster");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0;
      boolean allowed = StandardRequest.isAllowed("Clan Item", "Fax Machine");
      boolean limited = Limitmode.limitClan();
      boolean photo =
          InventoryManager.getCount(ItemPool.PHOTOCOPIER) > 0
              || InventoryManager.getCount(ItemPool.PHOTOCOPIED_MONSTER) > 0
              || Preferences.getBoolean("_photocopyUsed");
      String text =
          Preferences.getBoolean("_photocopyUsed")
              ? "photocopied monster used"
              : "photocopied monster not used yet";
      String monster = Preferences.getString("photocopyMonster");
      if (!monster.equals("")) {
        text = text + ", now " + monster;
      }
      this.setText(text);
      this.setShown(photo || (!bm || kf) && have && allowed && !limited);
    }
  }

  public static class EnvyfishDaily extends Daily {
    public EnvyfishDaily() {
      this.addItem(ItemPool.GREEN_TAFFY);
      this.addItem(ItemPool.ENVYFISH_EGG);
      this.addListener("_envyfishEggUsed");
      this.addListener("envyfishMonster");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean envy =
          InventoryManager.getCount(ItemPool.GREEN_TAFFY) > 0
              || InventoryManager.getCount(ItemPool.ENVYFISH_EGG) > 0
              || Preferences.getBoolean("_envyfishEggUsed");
      String text =
          Preferences.getBoolean("_envyfishEggUsed")
              ? "envyfish monster used"
              : "envyfish monster not used yet";
      String monster = Preferences.getString("envyfishMonster");
      if (!monster.equals("")) {
        text = text + ", now " + monster;
      }
      this.setText(text);
      this.setShown(envy);
    }
  }

  public static class FeastDaily extends Daily {
    JButton button;

    public FeastDaily() {
      this.addItem(ItemPool.MOVEABLE_FEAST);
      this.addListener("_feastUsed");
      this.addListener("_feastedFamiliars");
      this.addListener("(character)");
      button = this.addComboButton("use moveable feast", "use moveable feast");
      this.addLabel("");
    }

    @Override
    public void update() {
      int fu = Preferences.getInteger("_feastUsed");
      String list = Preferences.getString("_feastedFamiliars");
      boolean have = InventoryManager.getCount(ItemPool.MOVEABLE_FEAST) > 0;
      for (int i = 0; !have && i < KoLCharacter.getFamiliarList().size(); ++i) {
        FamiliarData current = KoLCharacter.getFamiliarList().get(i);
        if (current.getItem() != null && current.getItem().getItemId() == ItemPool.MOVEABLE_FEAST) {
          have = true;
        }
      }
      button.setToolTipText(fu + "/5");
      this.setText(list);
      this.setShown(have);
      this.setEnabled(true);
      if (fu >= 5) {
        this.setText("You have no more feasts for your familiar today");
        button.setVisible(false);
        return;
      }
    }
  }

  public static class PuddingDaily extends Daily {
    public PuddingDaily() {
      this.addListener("blackPuddingsDefeated");
      this.addListener("(character)");
      this.addButton("eat black pudding");
      this.addLabel("");
    }

    @Override
    public void update() {
      int bpd = Preferences.getInteger("blackPuddingsDefeated");
      this.setText(bpd + " defeated!");
      this.setShown(bpd < 240 && KoLCharacter.canEat());
    }
  }

  public static class ChipsDaily extends Daily {
    JButton btnMox;
    JButton btnMus;
    JButton btnMys;

    public ChipsDaily() {
      this.addListener("_chipBags");
      this.addListener("(character)");
      btnMox = this.addComboButton("chips radium", "chips radium");
      btnMus = this.addComboButton("chips wintergreen", "chips wintergreen");
      btnMys = this.addComboButton("chips ennui", "chips ennui");
      btnMox.setToolTipText("moxie +30 for 10");
      btnMus.setToolTipText("muscle +30 for 10");
      btnMys.setToolTipText("mysticality +30 for 10");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean limited = Limitmode.limitClan();
      int nf = Preferences.getInteger("_chipBags");
      this.setShown(KoLCharacter.hasClan() && KoLCharacter.canInteract() && !limited);
      this.setEnabled(true);
      if (nf >= 3) {
        this.setText("You have collected chips today");
        btnMox.setVisible(false);
        btnMus.setVisible(false);
        btnMys.setVisible(false);
        return;
      }
      this.setText(nf + "/3");
    }
  }

  public static class PitDaily extends Daily {
    JButton button;

    public PitDaily() {
      this.addListener("_ballpit");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      button = this.addComboButton("ballpit", "ballpit");
      button.setToolTipText("stat boost for 20");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean dun = Preferences.getBoolean("_ballpit");
      boolean limited = Limitmode.limitClan();
      this.setShown(KoLCharacter.hasClan() && KoLCharacter.canInteract() && !limited);
      this.setEnabled(true);
      if (dun) {
        this.setText("You have jumped into the ballpit today");
        button.setVisible(false);
        return;
      }
    }
  }

  public static class HatterDaily extends Daily {
    private final DisabledItemsComboBox<String> box;
    private final Component space;
    private final JButton button;

    private final List<String> effectHats = new ArrayList<String>();
    private final List<String> modifiers = new ArrayList<String>();

    private final HatterComboListener listener = new HatterComboListener();

    public HatterDaily() {
      this.addListener("_madTeaParty");
      this.addListener("(hats)");
      this.addListener("kingLiberated");
      this.addListener("(character)");

      this.addItem(ItemPool.DRINK_ME_POTION);
      this.addItem(ItemPool.VIP_LOUNGE_KEY);

      this.modifiers.add(null);
      this.box = this.addComboBox(new String[0], this.modifiers, comboBoxSizeString);
      this.space = this.add(Box.createRigidArea(new Dimension(5, 1)));

      // Initialize the GO button to do nothing.
      this.button = this.addComboButton("", "Go!");

      this.addLabel("");

      this.update();
    }

    @Override
    public final void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have =
          (InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0)
              || (InventoryManager.getCount(ItemPool.DRINK_ME_POTION) > 0);
      boolean active =
          KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.DOWN_THE_RABBIT_HOLE));
      boolean limited = Limitmode.limitZone("RabbitHole");

      this.setShown(
          StandardRequest.isAllowed("Clan Item", "Looking Glass")
              && (have || active)
              && (!bm || kf)
              && !limited);

      if (Preferences.getBoolean("_madTeaParty")) {
        this.box.setVisible(false);
        this.space.setVisible(false);
        this.button.setVisible(false);
        this.setText("You have visited the tea party today");
        this.setEnabled(false);
        return;
      }

      this.setEnabled(true);

      this.box.removeActionListener(listener);
      this.box.removeAllItems();
      this.box.addActionListener(listener);

      this.effectHats.clear();
      this.modifiers.clear();

      this.box.addItem("Available Hatter Buffs: ");
      this.effectHats.add(null);
      this.modifiers.add(null);

      // build hat options here
      List<AdventureResult> hats = EquipmentManager.getEquipmentLists().get(EquipmentManager.HAT);
      FamiliarData current = KoLCharacter.getFamiliar();

      if (current.getItem() != null && EquipmentDatabase.isHat(current.getItem())) {
        hats.add(current.getItem());
      }

      // Iterate across hatter buffs (i.e. hat character-lengths) first
      if (hats.size() > 0) {
        for (Hat hat : RabbitHoleManager.HAT_DATA) {
          // iterate down inventory second
          for (AdventureResult ad : hats) {
            if (ad != null && !ad.getName().equals("(none)") && EquipmentManager.canEquip(ad)) {
              if (hat.getLength() == RabbitHoleManager.hatLength(ad.getName())) {
                this.box.addItem(hat.getEffect(), false);
                this.effectHats.add(ad.getName());
                this.modifiers.add(hat.getModifier());
                break;
              }
            }
          }
        }
      }

      box.setTooltips(this.modifiers);
      box.setEnabled(true);

      setComboTarget(button, "");
    }

    public String getEffectHat(int index) {
      return this.effectHats.get(index);
    }

    private class HatterComboListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();

        if (cb.getItemCount() == 0) {
          return;
        }

        if (cb.getSelectedIndex() <= 0) {
          setComboTarget(button, "");
        } else {
          String Choice = cb.getSelectedItem().toString();
          setComboTarget(button, "hatter " + HatterDaily.this.getEffectHat(cb.getSelectedIndex()));
        }
      }
    }
  }

  public static class BanishedDaily extends Daily {
    public BanishedDaily() {
      this.addListener("banishedMonsters");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean borisBan = KoLCharacter.hasSkill("Banishing Shout");
      boolean zombieBan = KoLCharacter.hasSkill("Howl of the Alpha");
      boolean jarlBan =
          InventoryManager.getCount(ItemPool.STAFF_OF_CHEESE) > 0
              || KoLCharacter.hasEquipped(DailyDeedsPanel.STAFF_OF_CHEESE);
      boolean nanoBan = KoLCharacter.findFamiliar(FamiliarPool.NANORHINO) != null;

      String list = BanishManager.getBanishList();
      String text = "Banished monsters: " + list;

      this.setText(text);
      this.setShown(borisBan || zombieBan || jarlBan || nanoBan || list.length() > 0);
    }
  }

  public static class SwimmingPoolDaily extends Daily {
    JButton laps;
    JButton sprints;

    public SwimmingPoolDaily() {
      this.addItem(ItemPool.VIP_LOUNGE_KEY);
      this.addListener("_olympicSwimmingPool");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      laps = this.addComboButton("swim laps", "swim laps");
      sprints = this.addComboButton("swim sprints", "swim sprints");
      laps.setToolTipText("init +30%, +25 stench dmg, +20 ml, 50 turns");
      sprints.setToolTipText("-5% combat, 50 turns");
      this.addLabel("");
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = InventoryManager.getCount(ItemPool.VIP_LOUNGE_KEY) > 0;
      boolean sp = Preferences.getBoolean("_olympicSwimmingPool");
      boolean allowed = StandardRequest.isAllowed("Clan Item", "Clan Swimming Pool");
      boolean limited = Limitmode.limitClan();
      this.setShown((!bm || kf) && (have || sp) && allowed && !limited);
      if (sp) {
        this.setText("You have swum in the pool today");
        laps.setVisible(false);
        sprints.setVisible(false);
        return;
      }
      this.setEnabled(true);
    }
  }

  public static class JickDaily extends Daily {
    private final JButton button;

    public JickDaily() {
      this.addItem(ItemPool.PSYCHOANALYTIC_JAR);
      this.addListener("_jickJarAvailable");
      this.addListener("(character)");
      this.button = this.addButton("Jick Jar");
      this.button.setActionCommand("ashq visit_url(\"showplayer.php?who=1\");");
      this.addLabel("click to check");
    }

    @Override
    public void update() {
      String status = Preferences.getString("_jickJarAvailable");
      boolean haveJar = InventoryManager.hasItem(ItemPool.PSYCHOANALYTIC_JAR);

      if (status.equals("false")) {
        this.setText("Jick Jar not available");
        this.button.setVisible(false);
      } else if (status.equals("true")) {
        this.setText("<html>Jick Jar is <b>AVAILABLE</b></html>");
        this.button.setVisible(false);
      } else {
        this.setText("click to check");
        this.button.setVisible(true);
      }

      this.setShown(!status.equals("unknown") || haveJar);
    }
  }

  public static class JarlsbergStavesDaily extends Daily {
    public JarlsbergStavesDaily() {
      this.addListener("_jiggleCheese");
      this.addListener("_jiggleLife");
      this.addListener("_jiggleSteak");
      this.addListener("_jiggleCream");
      this.addListener("_jiggleCreamedMonster");
      this.addListener("_jiggleCheesedMonsters");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      this.setShown(KoLCharacter.isJarlsberg());
      if (!KoLCharacter.isJarlsberg()) {
        return;
      }

      boolean haveCheese =
          InventoryManager.getCount(ItemPool.STAFF_OF_CHEESE) > 0
              || KoLCharacter.hasEquipped(DailyDeedsPanel.STAFF_OF_CHEESE);
      boolean haveLife =
          InventoryManager.getCount(ItemPool.STAFF_OF_LIFE) > 0
              || KoLCharacter.hasEquipped(DailyDeedsPanel.STAFF_OF_LIFE);
      boolean haveSteak =
          InventoryManager.getCount(ItemPool.STAFF_OF_STEAK) > 0
              || KoLCharacter.hasEquipped(DailyDeedsPanel.STAFF_OF_STEAK);
      boolean haveCream =
          InventoryManager.getCount(ItemPool.STAFF_OF_CREAM) > 0
              || KoLCharacter.hasEquipped(DailyDeedsPanel.STAFF_OF_CREAM);
      int jiggledCheese = Preferences.getInteger("_jiggleCheese");
      int jiggledLife = Preferences.getInteger("_jiggleLife");
      int jiggledSteak = Preferences.getInteger("_jiggleSteak");
      int jiggledCream = Preferences.getInteger("_jiggleCream");
      String creamedMonster = Preferences.getString("_jiggleCreamedMonster");

      boolean shown = false;
      String text = "Staff jiggles: ";

      if (haveSteak) {
        text = text + jiggledSteak + "/5 Steak Staff (item)";
        shown = true;
      }
      if (haveLife) {
        if (shown) {
          text = text + ", ";
        }
        text = text + jiggledLife + "/5 Life Staff (hp)";
        shown = true;
      }
      if (haveCheese) {
        if (shown) {
          text = text + ", ";
        }
        text = text + jiggledCheese + "/5 Cheese Staff (banish)";
        shown = true;
      }
      if (haveCream) {
        if (shown) {
          text = text + ", ";
        }
        text = text + jiggledCream + "/5 Cream Staff (olfact)";
        if (jiggledCream > 0) {
          text = text + " currently " + creamedMonster;
        }
        shown = true;
      }

      this.setText(text);
    }
  }

  public static class DefectiveTokenDaily extends Daily {
    private final JButton button;

    public DefectiveTokenDaily() {
      this.addItem(ItemPool.GG_TICKET);
      this.addItem(ItemPool.GG_TOKEN);
      this.addListener("_defectiveTokenChecked");
      this.addListener("(character)");
      this.button = this.addButton("defective token");
      this.button.setActionCommand(
          "ashq visit_url(\"place.php?whichplace=arcade&action=arcade_plumber\",false);");
      this.addLabel("click to check");
    }

    @Override
    public void update() {
      boolean checked = Preferences.getBoolean("_defectiveTokenChecked");

      if (checked) {
        this.setText("You have checked for a defective token today");
        this.button.setVisible(false);
        return;
      }

      boolean unlocked =
          Preferences.getInteger("lastArcadeAscension") == KoLCharacter.getAscensions();
      boolean unlockable =
          unlocked
              // Having those items doesn't matter if it's already unlocked
              || InventoryManager.hasItem(ItemPool.GG_TOKEN)
              || InventoryManager.hasItem(ItemPool.GG_TICKET);
      boolean limited = Limitmode.limitClan();

      if (limited) {
        this.setShown(false);
      }

      if (!unlockable) {
        this.setText("Game Grid Arcade is not accessible");
        this.button.setVisible(false);
      } else {
        this.setText("click to check");
        this.button.setVisible(true);
      }
    }
  }

  public static class ChateauDeskDaily extends Daily {
    private final JButton button;

    public ChateauDeskDaily() {
      this.addListener("_chateauDeskHarvested");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      this.button = this.addButton("Chateau desk");
      this.button.setActionCommand(
          "ashq visit_url(\"place.php?whichplace=chateau&action=chateau_desk\",false);");
      this.addLabel("Click to harvest");
    }

    @Override
    public void update() {
      boolean available = ChateauRequest.chateauAvailable();
      this.setShown(available);

      boolean harvested = Preferences.getBoolean("_chateauDeskHarvested");

      if (harvested) {
        this.setText("You have harvested the chateau desk today");
        this.button.setVisible(false);
        return;
      }

      for (AdventureResult item : KoLConstants.chateau) {
        switch (item.getItemId()) {
          case ItemPool.CHATEAU_BANK:
            this.setText("1,000 meat");
            this.button.setActionCommand(
                "ashq visit_url(\"place.php?whichplace=chateau&action=chateau_desk1\",false);");
            this.button.setVisible(true);
            break;
          case ItemPool.CHATEAU_JUICE_BAR:
            this.setText("3 random potions");
            this.button.setActionCommand(
                "ashq visit_url(\"place.php?whichplace=chateau&action=chateau_desk2\",false);");
            this.button.setVisible(true);
            break;
          case ItemPool.CHATEAU_PENS:
            this.setText("3 fancy calligraphy pens");
            this.button.setActionCommand(
                "ashq visit_url(\"place.php?whichplace=chateau&action=chateau_desk3\",false);");
            this.button.setVisible(true);
            break;
        }
      }
    }
  }

  private static final String[][] DECK_COMBO_DATA = {
    {"Deck of Every Card: ", "", ""},
    {"Random", "play random", "Draw cards randomly"},
    {"- Stat Gain -", "", ""},
    {"Muscle stat", "play stat muscle", "Gain 500 muscle substats"},
    {"Myst stat", "play stat mysticality", "Gain 500 mysticality substats"},
    {"Moxie stat", "play stat moxie", "Gain 500 moxie substats"},
    {"- Buffs -", "", ""},
    {"Muscle buff", "play buff muscle", "Muscle +200% (20 Adventures)"},
    {"Myst buff", "play buff mysticality", "Mysticality +200% (20 Adventures)"},
    {"Moxie buff", "play buff moxie", "Moxie +200% (20 Adventures)"},
    {"Item buff", "play buff items", "+100% Item Drops from Monsters (20 Adventures)"},
    {"Init buff", "play buff initiative", "+200% Combat Initiative (20 Adventures)"},
    {"- Items -", "", ""},
    {"Clubs", "play X of Clubs", "Get X seal-clubbing clubs and 3 PvP fights"},
    {"Diamonds", "play X of Diamonds", "Get X cubic zirconia (100 Meat)"},
    {"Hearts", "play X of Hearts", "Get X bubblegum hearts (80-100 HP)"},
    {"Spades", "play X of Spades", "Get X grave-robbing shovels and spade puzzle clue"},
    {"Papayas", "play X of Papayas", "Get X papayas"},
    {"Kumquats", "play X of Kumquats", "Get X kumquats"},
    {"Salads", "play X of Salads", "Get X delicious salads"},
    {"Cups", "play X of Cups", "Get X random booze"},
    {"Coins", "play X of Coins", "Get X valuable coins (500 Meat)"},
    {"Swords", "play X of Swords", "Get X random swords"},
    {"Wands", "play X of Wands", "Get 5 turns of X random buffs"},
    {"Tower", "play XVI - The Tower", "Get a hero key"},
    {"Plum", "play Professor Plum", "Get 10 plums"},
    {"Tire", "play Spare Tire", "Get tires"},
    {"Tank", "play Extra Tank", "Get a full meat tank"},
    {"Sheep", "play Sheep", "Get 3 stone wool"},
    {"Plenty", "play Year of Plenty", "Get 5 random foods"},
    {"Mine", "play Mine", "Get one each of asbestos ore, linoleum ore, and chrome ore"},
    {"Laboratory", "play Laboratory", "Get 5 random potions"},
    {"Gift", "play Gift Card", "Get a gift card"},
    {"1952 card", "play 1952 Mickey Mantle", "Get a 1952 Mickey Mantle card (10000 Meat)"},
    {"- Weapons -", "", ""},
    {"Lead Pipe", "play Lead Pipe", "Club, +100% Muscle, +50 Max HP"},
    {"Rope", "play Rope", "Whip, +2 Muscle Stat(s) Per Fight, +10 Familiar Weight"},
    {"Wrench", "play Wrench", "Utensil, +100% Spell Damage, +50 Max MP"},
    {
      "Candlestick", "play Candlestick", "Wand, +100% Mysticality, +2 Mysticality Stat(s) Per Fight"
    },
    {"Knife", "play Knife", "Knife, +100% Moxie, +50% Meat"},
    {"Revolver", "play Revolver", "Pistol, +2 Moxie Stat(s) Per Fight, +50% Combat Initiative"},
    {"- Skills/Mana -", "", ""},
    {"Healing Salve", "play Healing Salve", "Learn Healing Salve or get white mana"},
    {"Dark Ritual", "play Dark Ritual", "Learn Dark Ritual or get black mana"},
    {"Lightning Bolt", "play Lightning Bolt", "Learn Lightning Bolt or get red mana"},
    {"Giant Growth", "play Giant Growth", "Learn Giant Growth or get green mana"},
    {"Ancestral Recall", "play Ancestral Recall", "Learn Ancestral Recall or get blue mana"},
    {"Plains", "play Plains", "Get white mana (Healing Salve)"},
    {"Swamp", "play Swamp", "Get black mana (Dark Ritual)"},
    {"Mountain", "play Mountain", "Get red mana (Lightning Bolt)"},
    {"Forest", "play Forest", "Get green mana (Giant Growth)"},
    {"Island", "play Island", "Get blue mana (Ancestral Recall)"},
    {"- Special Fights -", "", ""},
    {"Fight alien", "play Green Card", "Fight \"legal alien\""},
    {"Fight emperor", "play IV - The Emperor", "Fight \"The Emperor\""},
    {"Fight hermit", "play IX - The Hermit", "Fight \"Hermit\""},
    {"- Phylum Fights -", "", ""},
  };

  public static class DeckOfEveryCardDaily extends Daily {
    private static final List<String> choices = new ArrayList<String>();
    private static final List<String> commands = new ArrayList<String>();
    private static final List<String> tooltips = new ArrayList<String>();

    DisabledItemsComboBox<String> box = new DisabledItemsComboBox<>();
    Component space;
    JButton btn;

    static {
      for (String[] combodata : DECK_COMBO_DATA) {
        choices.add(combodata[0]);
        commands.add(combodata[1]);
        tooltips.add(combodata[2]);
      }
      for (String phylum : MonsterDatabase.PHYLUM_ARRAY) {
        if (!phylum.equals("none")) {
          choices.add("Fight " + phylum);
          commands.add("play phylum " + phylum);
          tooltips.add("Fight a random \"" + phylum + "\" monster");
        }
      }
    }

    public DeckOfEveryCardDaily() {
      this.addItem(ItemPool.DECK_OF_EVERY_CARD);
      this.addListener("_deckCardsDrawn");
      this.addListener("kingLiberated");
      this.addListener("(character)");

      box = this.addComboBox(choices.toArray(STRING_ARRAY), tooltips, comboBoxSizeString);
      box.addActionListener(new DeckComboListener());
      space = this.add(Box.createRigidArea(new Dimension(5, 1)));
      btn = this.addComboButton("", "Draw");
      this.addLabel("X/15 cards");
      this.setEnabled(false);
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = InventoryManager.getCount(ItemPool.DECK_OF_EVERY_CARD) > 0;
      boolean nocards = Preferences.getInteger("_deckCardsDrawn") >= 15;
      boolean allowed = StandardRequest.isAllowed("Items", "Deck of Every Card");
      boolean limited = Limitmode.limitItem(ItemPool.DECK_OF_EVERY_CARD);
      this.setShown((!bm || kf) && (have || nocards) && allowed && !limited);
      if (nocards) {
        this.setText("You have drawn all your cards today");
        box.setVisible(false);
        space.setVisible(false);
        btn.setVisible(false);
        return;
      }
      box.setEnabled(!nocards);
      box.setSelectedIndex(0);
      this.setText(Preferences.getInteger("_deckCardsDrawn") + "/15 cards");
    }

    private class DeckComboListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();
        String command = commands.get(cb.getSelectedIndex());
        int cardsdrawn = Preferences.getInteger("_deckCardsDrawn");
        if (command.equals("")) {
          setComboTarget(btn, "");
          setEnabled(false);
        } else {
          if (cb.getSelectedIndex() > 1 && cardsdrawn > 10) {
            // Can't cheat with less than 5 remaining
            setComboTarget(btn, "");
            setEnabled(false);
          } else {
            setComboTarget(btn, command);
            setEnabled(true);
          }
        }
      }
    }
  }

  public static class TeaTreeDaily extends Daily {
    private static final List<String> choices = new ArrayList<String>();
    private static final List<String> commands = new ArrayList<String>();
    private static final List<String> tooltips = new ArrayList<String>();

    static {
      choices.add("Potted Tea Tree:");
      commands.add("");
      tooltips.add("");

      choices.add("shake");
      commands.add("teatree shake");
      tooltips.add("3 random teas");

      for (PottedTea tea : PottedTeaTreeRequest.teas) {
        choices.add(tea.toString());
        commands.add("teatree " + tea.toString());
        tooltips.add(tea.effect());
      }
    }

    DisabledItemsComboBox<String> box = new DisabledItemsComboBox<>();
    Component space;
    JButton btn;

    public TeaTreeDaily() {
      this.addListener("_pottedTeaTreeUsed");
      this.addListener("kingLiberated");
      this.addListener("(character)");

      box = this.addComboBox(choices.toArray(STRING_ARRAY), tooltips, comboBoxSizeString);
      box.addActionListener(new TeaTreeListener());
      space = this.add(Box.createRigidArea(new Dimension(5, 1)));
      btn = this.addComboButton("", "Pick");
      this.addLabel("");
      this.setEnabled(false);
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = KoLConstants.campground.contains(ItemPool.get(ItemPool.POTTED_TEA_TREE, 1));
      boolean available = !Preferences.getBoolean("_pottedTeaTreeUsed");
      // Can pick in standard at present
      // boolean allowed = StandardRequest.isAllowed( "Items", "potted tea tree" );
      boolean allowed = true;
      boolean limited = Limitmode.limitItem(ItemPool.POTTED_TEA_TREE);
      this.setShown((!bm || kf) && have && allowed && !limited);
      if (!available) {
        this.setText("You have picked your tea for today");
        box.setVisible(false);
        btn.setVisible(false);
        space.setVisible(false);
        return;
      }
      box.setEnabled(available);
      box.setSelectedIndex(0);
    }

    private class TeaTreeListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();
        String command = commands.get(cb.getSelectedIndex());
        setComboTarget(btn, command);
        setEnabled(cb.getSelectedIndex() > 0 && !Preferences.getBoolean("_pottedTeaTreeUsed"));
      }
    }
  }

  public static class BarrelGodDaily extends Daily {
    JButton btnMus;
    JButton btnMys;
    JButton btnMox;
    JButton btnBuff;

    public BarrelGodDaily() {
      this.addListener("barrelShrineUnlocked");
      this.addListener("_barrelPrayer");
      this.addListener("prayedForGlamour");
      this.addListener("prayedForProtection");
      this.addListener("prayedForVigor");
      this.addListener("kingLiberated");
      this.addListener("(character)");

      btnMus = this.addComboButton("barrelprayer protection", "Protection");
      btnMys = this.addComboButton("barrelprayer glamour", "Glamour");
      btnMox = this.addComboButton("barrelprayer vigor", "Vigor");
      btnBuff = this.addComboButton("barrelprayer buff", "Class buff");
      btnMus.setToolTipText("Shield, +25% Muscle, +50 Monster Level, +100 Max HP");
      btnMys.setToolTipText("Accessory, +25% Mysticality, +50% Item Drops, 5-10 MP regen");
      btnMox.setToolTipText("Pants, +25% Moxie, +50 Combat Initiative, 10-20 HP regen");
      this.addLabel("Pray to the Barrel god");
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = Preferences.getBoolean("barrelShrineUnlocked");
      boolean prayed = Preferences.getBoolean("_barrelPrayer");
      boolean allowed = StandardRequest.isAllowed("Items", "shrine to the Barrel god");
      boolean limited = Limitmode.limitZone("Dungeon Full of Dungeons");
      this.setShown((!bm || kf) && (have || prayed) && allowed && !limited);

      if (prayed) {
        this.setText("You have prayed to the Barrel god today");
        btnMus.setVisible(false);
        btnMys.setVisible(false);
        btnMox.setVisible(false);
        btnBuff.setVisible(false);
        return;
      }
      boolean hasProtection = !Preferences.getBoolean("prayedForProtection");
      boolean hasGlamour = !Preferences.getBoolean("prayedForGlamour");
      boolean hasVigor = !Preferences.getBoolean("prayedForVigor");
      btnMus.setVisible(hasProtection);
      btnMys.setVisible(hasGlamour);
      btnMox.setVisible(hasVigor);
      btnBuff.setVisible(true);
      this.setText("Pray to the Barrel god");

      String buffText = null;
      AscensionClass ascensionClass = KoLCharacter.getAscensionClass();
      if (ascensionClass != null) {
        switch (ascensionClass) {
          case SEAL_CLUBBER:
            buffText = "Weapon Damage +150%";
            break;
          case TURTLE_TAMER:
            buffText = "Maximum HP +90, Makes food more delicious!";
            break;
          case PASTAMANCER:
            buffText = "+90% Item Drops from Monsters";
            break;
          case SAUCEROR:
            buffText = "Spell Damage +150%";
            break;
          case DISCO_BANDIT:
            buffText = "Ranged Damage +150%";
            break;
          case ACCORDION_THIEF:
            buffText = "+45% Booze Drops from Monsters, Makes booze more effective!";
            break;
        }
      }

      if (buffText != null) {
        btnBuff.setToolTipText(buffText);
        return;
      }

      if (!(hasProtection || hasGlamour || hasVigor)) {
        this.setText("The Barrel god will not answer your prayers");
      }

      btnBuff.setVisible(false);
    }
  }

  private static final String[][] TERMINAL_ENHANCE_DATA = {
    {"Terminal Enhance: ", "", ""},
    {"items.enh", "terminal enhance items.enh", "+30% item drop"},
    {"meat.enh", "terminal enhance meat.enh", "+60% meat drop"},
    {"init.enh", "terminal enhance init.enh", "+50% init"},
    {
      "critical.enh",
      "terminal enhance critical.enh",
      "+10% critical chance, +10% spell critical chance"
    },
    {"damage.enh", "terminal enhance damage.enh", "+5 prismatic damage"},
    {"substats.enh", "terminal enhance substats.enh", "+3 stats per fight"},
  };

  public static class TerminalEnhanceDaily extends Daily {
    private static final List<String> choices = new ArrayList<String>();
    private static final List<String> commands = new ArrayList<String>();
    private static final List<String> tooltips = new ArrayList<String>();

    DisabledItemsComboBox<String> box = new DisabledItemsComboBox<>();
    Component space;
    JButton btn;

    static {
      for (String[] combodata : TERMINAL_ENHANCE_DATA) {
        choices.add(combodata[0]);
        commands.add(combodata[1]);
        tooltips.add(combodata[2]);
      }
    }

    public TerminalEnhanceDaily() {
      this.addListener("sourceTerminalChips");
      this.addListener("sourceTerminalEnhanceKnown");
      this.addListener("_sourceTerminalEnhanceUses");
      this.addListener("kingLiberated");
      this.addListener("(character)");

      box = this.addComboBox(choices.toArray(STRING_ARRAY), tooltips, comboBoxSizeString);
      box.addActionListener(new TerminalEnhanceComboListener());
      space = this.add(Box.createRigidArea(new Dimension(5, 1)));
      btn = this.addComboButton("", "Enhance");
      this.addLabel("X/3 enhances");
      this.setEnabled(false);
    }

    @Override
    public void update() {
      String chips = Preferences.getString("sourceTerminalChips");
      int limit = 1 + (chips.contains("CRAM") ? 1 : 0) + (chips.contains("SCRAM") ? 1 : 0);
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean noenhance = Preferences.getInteger("_sourceTerminalEnhanceUses") >= limit;
      boolean have = !Preferences.getString("sourceTerminalEnhanceKnown").equals("");
      boolean allowed = StandardRequest.isAllowed("Items", "Source terminal");
      boolean limited = Limitmode.limitCampground();
      this.setShown((!bm || kf) && have && allowed && !limited);
      if (noenhance) {
        this.setText("You have used your enhancements today");
        box.setVisible(false);
        space.setVisible(false);
        btn.setVisible(false);
        return;
      }
      box.setVisible(true);
      space.setVisible(true);
      btn.setVisible(true);
      box.setEnabled(true);
      box.setSelectedIndex(0);
      this.setText(
          Preferences.getInteger("_sourceTerminalEnhanceUses") + "/" + limit + " enhances");
      for (int i = 1; i < TERMINAL_ENHANCE_DATA.length; ++i) {
        box.setDisabledIndex(
            i,
            !Preferences.getString("sourceTerminalEnhanceKnown")
                .contains(TERMINAL_ENHANCE_DATA[i][0]));
      }
    }

    private class TerminalEnhanceComboListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();
        String command = commands.get(cb.getSelectedIndex());
        if (command.equals("")) {
          setComboTarget(btn, "");
          setEnabled(false);
        } else {
          setComboTarget(btn, command);
          setEnabled(true);
        }
      }
    }
  }

  private static final String[][] TERMINAL_ENQUIRY_DATA = {
    {"Terminal Enquiry: ", "", ""},
    {"familiar.enq", "terminal enquiry familiar.enq", "+5 familiar weight"},
    {"monsters.enq", "terminal enquiry monsters.enq", "+25 ML"},
    {"protect.enq", "terminal enquiry protect.enq", "+3 all elemental resistance"},
    {"stats.enq", "terminal enquiry stats.enq", "+100% all stats"},
  };

  public static class TerminalEnquiryDaily extends Daily {
    private static final List<String> choices = new ArrayList<String>();
    private static final List<String> commands = new ArrayList<String>();
    private static final List<String> tooltips = new ArrayList<String>();

    DisabledItemsComboBox<String> box = new DisabledItemsComboBox<>();
    JButton btn;

    static {
      for (String[] combodata : TERMINAL_ENQUIRY_DATA) {
        choices.add(combodata[0]);
        commands.add(combodata[1]);
        tooltips.add(combodata[2]);
      }
    }

    public TerminalEnquiryDaily() {
      this.addListener("sourceTerminalEnquiryKnown");
      this.addListener("sourceTerminalEnquiry");
      this.addListener("kingLiberated");
      this.addListener("(character)");

      box = this.addComboBox(choices.toArray(STRING_ARRAY), tooltips, comboBoxSizeString);
      box.addActionListener(new TerminalEnquiryComboListener());
      this.add(Box.createRigidArea(new Dimension(5, 1)));
      btn = this.addComboButton("", "Enquiry");
      this.addLabel("currently ");
      this.setEnabled(false);
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = !Preferences.getString("sourceTerminalEnquiryKnown").equals("");
      boolean allowed = StandardRequest.isAllowed("Items", "Source terminal");
      boolean limited = Limitmode.limitCampground();
      this.setShown((!bm || kf) && have && allowed && !limited);
      box.setEnabled(true);
      box.setSelectedIndex(0);
      String currentSetting = Preferences.getString("sourceTerminalEnquiry");
      this.setText("currently " + currentSetting);
      for (int i = 1; i < TERMINAL_ENQUIRY_DATA.length; ++i) {
        if (currentSetting.equals(TERMINAL_ENQUIRY_DATA[i][0])) {
          box.setSelectedIndex(i);
        }
        box.setDisabledIndex(
            i,
            !Preferences.getString("sourceTerminalEnquiryKnown")
                .contains(TERMINAL_ENQUIRY_DATA[i][0]));
      }
    }

    private class TerminalEnquiryComboListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();
        String command = commands.get(cb.getSelectedIndex());
        String choice = choices.get(cb.getSelectedIndex());
        if (command.equals("") || choice.equals(Preferences.getString("sourceTerminalEnquiry"))) {
          setComboTarget(btn, "");
          setEnabled(false);
        } else {
          setComboTarget(btn, command);
          setEnabled(true);
        }
      }
    }
  }

  private static final String[][] TERMINAL_EXTRUDE_DATA = {
    {"Terminal Extrude: ", "", "", "0"},
    {"food.ext", "terminal extrude food", "browser cookie", "10"},
    {"booze.ext", "terminal extrude booze", "hacked gibson", "10"},
    {"goggles.ext", "terminal extrude goggles", "Source shades", "100"},
    {"gram.ext", "terminal extrude gram", "Source terminal GRAM chip", "100"},
    {"pram.ext", "terminal extrude pram", "Source terminal PRAM chip", "100"},
    {"spam.ext", "terminal extrude spam", "Source terminal SPAM chip", "100"},
    {"cram.ext", "terminal extrude cram", "Source terminal CRAM chip", "1000"},
    {"dram.ext", "terminal extrude dram", "Source terminal DRAM chip", "1000"},
    {"tram.ext", "terminal extrude tram", "Source terminal TRAM chip", "1000"},
    {"familiar.ext", "terminal extrude familiar", "software bug", "10000"},
  };

  public static class TerminalExtrudeDaily extends Daily {
    private static final List<String> choices = new ArrayList<String>();
    private static final List<String> commands = new ArrayList<String>();
    private static final List<String> tooltips = new ArrayList<String>();

    DisabledItemsComboBox<String> box = new DisabledItemsComboBox<>();
    Component space;
    JButton btn;

    static {
      for (String[] combodata : TERMINAL_EXTRUDE_DATA) {
        choices.add(combodata[0]);
        commands.add(combodata[1]);
        tooltips.add(combodata[2]);
      }
    }

    public TerminalExtrudeDaily() {
      this.addListener("sourceTerminalExtrudeKnown");
      this.addListener("_sourceTerminalExtrudes");
      this.addListener("kingLiberated");
      this.addListener("(character)");

      box = this.addComboBox(choices.toArray(STRING_ARRAY), tooltips, comboBoxSizeString);
      box.addActionListener(new TerminalExtrudeComboListener());
      space = this.add(Box.createRigidArea(new Dimension(5, 1)));
      btn = this.addComboButton("", "Extrude");
      this.addLabel("X/3 extrudes");
      this.setEnabled(false);
    }

    @Override
    public void update() {
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = !Preferences.getString("sourceTerminalExtrudeKnown").equals("");
      boolean allowed = StandardRequest.isAllowed("Items", "Source terminal");
      boolean limited = Limitmode.limitCampground();
      int extrudes = Preferences.getInteger("_sourceTerminalExtrudes");
      boolean noextrude = extrudes >= 3;
      this.setShown((!bm || kf) && have && allowed && !limited);
      if (noextrude) {
        this.setText("You have extruded your items today");
        box.setVisible(false);
        space.setVisible(false);
        btn.setVisible(false);
        return;
      }
      this.setText(extrudes + "/3 extrudes");
      box.setEnabled(true);
      box.setSelectedIndex(0);
      for (int i = 1; i < TERMINAL_EXTRUDE_DATA.length; ++i) {
        boolean known =
            Preferences.getString("sourceTerminalExtrudeKnown")
                .contains(TERMINAL_EXTRUDE_DATA[i][0]);
        int sourceEssence = InventoryManager.getCount(ItemPool.SOURCE_ESSENCE);
        boolean enough = (sourceEssence >= StringUtilities.parseInt(TERMINAL_EXTRUDE_DATA[i][3]));
        box.setDisabledIndex(i, !known || !enough);
      }
    }

    private class TerminalExtrudeComboListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();
        String command = commands.get(cb.getSelectedIndex());
        String choice = choices.get(cb.getSelectedIndex());
        if (command.equals("")) {
          setComboTarget(btn, "");
          setEnabled(false);
        } else {
          setComboTarget(btn, command);
          setEnabled(true);
        }
      }
    }
  }

  private static final String[][] TERMINAL_EDUCATE_DATA = {
    {"Terminal Educate: ", "", ""},
    {"extract.edu", "terminal educate extract", "collect source essence"},
    {"digitize.edu", "terminal educate digitize", "copy monster into future"},
    {"compress.edu", "terminal educate compress", "deals 25% of enemy HP"},
    {"duplicate.edu", "terminal educate duplicate", "doubles monster difficulty and drops"},
    {"portscan.edu", "terminal educate portscan", "makes Agent attack next combat"},
    {"turbo.edu", "terminal educate turbo", "increase MP by 100% and recover up to 1000 MP"},
  };

  public static class TerminalEducateDaily extends Daily {
    private static final List<String> choices = new ArrayList<String>();
    private static final List<String> commands = new ArrayList<String>();
    private static final List<String> tooltips = new ArrayList<String>();

    DisabledItemsComboBox<String> box = new DisabledItemsComboBox<>();
    JButton btn;

    static {
      for (String[] combodata : TERMINAL_EDUCATE_DATA) {
        choices.add(combodata[0]);
        commands.add(combodata[1]);
        tooltips.add(combodata[2]);
      }
    }

    public TerminalEducateDaily() {
      this.addListener("sourceTerminalEducateKnown");
      this.addListener("sourceTerminalEducate1");
      this.addListener("kingLiberated");
      this.addListener("(character)");

      box = this.addComboBox(choices.toArray(STRING_ARRAY), tooltips, comboBoxSizeString);
      box.addActionListener(new TerminalEducateComboListener());
      this.add(Box.createRigidArea(new Dimension(5, 1)));
      btn = this.addComboButton("", "Educate");
      this.addLabel("currently ");
      this.setEnabled(false);
    }

    @Override
    public void update() {
      String chips = Preferences.getString("sourceTerminalChips");
      String educate1 = Preferences.getString("sourceTerminalEducate1");
      String educate2 = Preferences.getString("sourceTerminalEducate2");
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = !Preferences.getString("sourceTerminalEducateKnown").equals("");
      boolean allowed = StandardRequest.isAllowed("Items", "Source terminal");
      boolean limited = Limitmode.limitCampground();
      this.setShown((!bm || kf) && have && allowed && !limited);
      box.setEnabled(true);
      box.setSelectedIndex(0);
      String currentSetting = educate1;
      if (chips.contains("DRAM") && !currentSetting.equals("")) {
        currentSetting = educate1 + "," + educate2;
      }
      this.setText("currently " + currentSetting);
      for (int i = 1; i < TERMINAL_EDUCATE_DATA.length; ++i) {
        box.setDisabledIndex(
            i,
            !Preferences.getString("sourceTerminalEducateKnown")
                .contains(TERMINAL_EDUCATE_DATA[i][0]));
      }
    }

    private class TerminalEducateComboListener implements ActionListener {
      @Override
      public void actionPerformed(final ActionEvent e) {
        DisabledItemsComboBox<?> cb = (DisabledItemsComboBox<?>) e.getSource();
        String command = commands.get(cb.getSelectedIndex());
        String choice = choices.get(cb.getSelectedIndex());
        String chips = Preferences.getString("sourceTerminalChips");
        boolean disable =
            (chips.contains("DRAM")
                ? choice.equals(Preferences.getString("sourceTerminalEducate2"))
                : choice.equals(Preferences.getString("sourceTerminalEducate1")));
        if (command.equals("") || disable) {
          setComboTarget(btn, "");
          setEnabled(false);
        } else {
          setComboTarget(btn, command);
          setEnabled(true);
        }
      }
    }
  }

  public static class TerminalSummaryDaily extends Daily {
    public TerminalSummaryDaily() {
      this.addListener("_sourceTerminalDigitizeUses");
      this.addListener("_sourceTerminalDuplicateUses");
      this.addListener("_sourceTerminalPortscanUses");
      this.addListener("sourceTerminalChips");
      this.addListener("kingLiberated");
      this.addListener("(character)");
      this.addLabel("");
    }

    @Override
    public void update() {
      String chips = Preferences.getString("sourceTerminalChips");
      int digitizeCount = Preferences.getInteger("_sourceTerminalDigitizeUses");
      int digitizeLimit =
          1 + (chips.contains("TRAM") ? 1 : 0) + (chips.contains("TRIGRAM") ? 1 : 0);
      String digitizeMonster = Preferences.getString("_sourceTerminalDigitizeMonster");
      int portscanCount = Preferences.getInteger("_sourceTerminalPortscanUses");
      int duplicateCount = Preferences.getInteger("_sourceTerminalDuplicateUses");
      int duplicateLimit = 1 + (KoLCharacter.inTheSource() ? 4 : 0);
      boolean bm = KoLCharacter.inBadMoon();
      boolean kf = KoLCharacter.kingLiberated();
      boolean have = !Preferences.getString("sourceTerminalEducateKnown").equals("");
      boolean allowed = StandardRequest.isAllowed("Items", "Source terminal");
      boolean limited = Limitmode.limitCampground();
      this.setShown((!bm || kf) && have && allowed && !limited);

      StringBuilder text = new StringBuilder();
      text.append("Duplicate: " + duplicateCount + "/" + duplicateLimit + ", ");
      text.append("Portscan: " + portscanCount + "/3, ");
      text.append("Digitize: " + digitizeCount + "/" + digitizeLimit);
      if (digitizeCount > 0) {
        text.append(" currently " + digitizeMonster);
      }

      this.setText(text.toString());
    }
  }
}
