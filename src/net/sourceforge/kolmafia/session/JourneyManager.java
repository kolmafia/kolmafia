package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class JourneyManager {

  // This package provides support for the "Journeyman" ascension path.
  //
  // In this path, you start with no skills.
  //
  // There are "out of the way" (not needed for ascension) adventuring locations where you can gain
  // skills.
  //
  // Each such location will offer 6 different skills, gained after 4, 8,
  // 12, 16, 20, 24 turns adventuring, in free non-combat encounters.
  //
  // Which location/turn generates which skill is class dependent.
  //
  // There are 30 locations offering 6 skills each = 180 different skills
  // There are 6 classes * 180 skills = 1080 datapoints
  //
  // zarqon generously provided a map containing all 1080 {class, location, index, skill} tuples.

  private JourneyManager() {}

  // Support for a "journeyman" command to display these tuples

  // journeyman locations Accordion Thief
  //
  //                       Class Name
  // location | Skill 1 | Skill 2 | Skill 3 | Skill 4 | Skill 5| Skill 6
  // ---------+---------+---------+---------+---------+--------+-------+
  //   loc 1  |  name   |  name   |  name   |  name   |  name  |  name |
  //   loc 2  |  name   |  name   |  name   |  name   |  name  |  name |
  //    ...
  //   loc 30 |  name   |  name   |  name   |  name   |  name  |  name |

  // (30 locations) -> (6 classes) -> (6 indices) -> skill

  // Indexed by sorted adventure name
  public static final Map<KoLAdventure, Map<AscensionClass, String[]>> journeymanData =
      new TreeMap<>(KoLAdventure.NameComparator);

  // journeyman skills Accordion Thief
  //
  // skill name | Class 1 | Class 2 | Class 3 | Class 4 | Class 5 | Class 6
  // -----------+---------+---------+---------+---------+---------+---------+
  //   skill 1  | loc+idx | loc+idx | loc+idx | loc+idx | loc+idx | loc+idx |
  //     ...
  //  skill 180 | loc+idx | loc+idx | loc+idx | loc+idx | loc+idx | loc+idx |

  // (180 skills) -> (6 classes) -> (location + index)

  // Indexed by sorted skill name
  public static final Map<String, Map<AscensionClass, Integer>> journeymanSkills = new TreeMap<>();

  // AdventureRequest provides a way to look up a KoLAdventure by name.
  // It does not provide a way to look one up by adventureId.
  // It does provide a way do look up by URL.
  //
  // All the adventure zones in Journeyman are adventure.php.
  // Rather than crafting adventure.php?snarfblat=XXX, lets provide a little map

  public static final Map<Integer, KoLAdventure> journeymanZones = new HashMap<>();

  static {
    String filename = "journeyman.txt";
    try (BufferedReader reader = FileUtilities.getReader(filename, false)) {
      String[] data;

      // Seal Clubber	Barrrney's Barrr	1	[6032]Accordion Bash
      int count = 0;
      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 4) {
          continue;
        }

        String className = data[0];
        AscensionClass aclass = AscensionClass.find(className);
        if (aclass == null) {
          RequestLogger.printLine("Unknown class \"" + className + "\" in " + filename);
          continue;
        }

        String locationName = data[1];
        KoLAdventure location = AdventureDatabase.getAdventure(locationName);
        if (location == null) {
          RequestLogger.printLine("Unknown location \"" + locationName + "\" in " + filename);
          continue;
        }
        int adventureId = location.getSnarfblat();

        if (!journeymanZones.containsKey(adventureId)) {
          journeymanZones.put(adventureId, location);
        }

        String indexString = data[2];
        int index = StringUtilities.parseInt(indexString);
        if (index < 1 || index > 6) {
          RequestLogger.printLine("Bad skill index \"" + indexString + "\" in " + filename);
          continue;
        }

        String skillName = data[3];
        int skillId = SkillDatabase.getSkillId(skillName);
        if (skillId == -1) {
          RequestLogger.printLine("Unknown skill \"" + skillName + "\" in " + filename);
          continue;
        }
        // Normalize
        skillName = SkillDatabase.getSkillName(skillId);

        // (30 locations) -> (6 classes) -> (6 indices) -> skill
        Map<AscensionClass, String[]> slice1 = journeymanData.get(location);
        if (slice1 == null) {
          slice1 = new HashMap<>();
          journeymanData.put(location, slice1);
        }
        String[] slice2 = slice1.get(aclass);
        if (slice2 == null) {
          slice2 = new String[6];
          slice1.put(aclass, slice2);
        }
        if (slice2[index - 1] != null) {
          RequestLogger.printLine(
              "Skill \"" + skillName + "\" already has an entry for class " + aclass);
          continue;
        }
        slice2[index - 1] = skillName;

        // (180 skills) -> (6 classes) -> (location + index)
        Map<AscensionClass, Integer> slice3 = journeymanSkills.get(skillName);
        if (slice3 == null) {
          slice3 = new HashMap<>();
          journeymanSkills.put(skillName, slice3);
        }
        if (slice3.containsKey(aclass)) {
          RequestLogger.printLine(
              "Skill \"" + skillName + "\" already has an entry for class " + aclass);
          continue;
        }
        int advPlusIndex = (adventureId * 6) + (index - 1);
        slice3.put(aclass, advPlusIndex);

        count++;
      }

      if (journeymanData.size() != 30) {
        RequestLogger.printLine(
            "Expecting 30 adventuring zones in \""
                + filename
                + "\" but only "
                + journeymanData.size()
                + " found.");
      }

      if (journeymanSkills.size() != 180) {
        RequestLogger.printLine(
            "Expecting 180 skills in \""
                + filename
                + "\" but only "
                + journeymanSkills.size()
                + " found.");
      }

      if (count != 1080) {
        RequestLogger.printLine(
            "Expecting 1080 entries in \"" + filename + "\" but only " + count + " found.");
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }
}
