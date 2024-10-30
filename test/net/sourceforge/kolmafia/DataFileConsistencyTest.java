package net.sourceforge.kolmafia;

import static internal.matchers.Preference.isUserPreference;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.CafeDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.Attribute;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/* Checks for consistency across datafiles.

  For instance, items marked as equipment in items.txt should also have
  corresponding entries in equipment.txt.
*/
@SuppressWarnings("incomplete-switch")
public class DataFileConsistencyTest {
  @BeforeAll
  public static void init() {
    Preferences.reset("DataFileConsistencyTest");
  }

  Set<String> datafileItems(String file, int version, int index) throws IOException {
    var items = new HashSet<String>();
    try (BufferedReader reader = FileUtilities.getVersionedReader(file, version)) {
      String[] fields;
      while ((fields = FileUtilities.readData(reader)) != null) {
        if (fields.length == 1) {
          continue;
        }
        var thing = fields[index];

        if (!thing.isBlank()) {
          items.add(thing);
        }
      }
    }
    return items;
  }

  Set<String> datafileItems(final String file, final int version) throws IOException {
    return datafileItems(file, version, 0);
  }

  List<Integer> allItems() {
    return IntStream.range(1, ItemDatabase.maxItemId() + 1)
        .filter(i -> i != 13)
        .filter(i -> ItemDatabase.getItemDataName(i) != null)
        .boxed()
        .toList();
  }

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of(
            "equipment.txt",
            2,
            (Function<Integer, Boolean>)
                (itemId) ->
                    ItemDatabase.isEquipment(itemId) && !ItemDatabase.isFamiliarEquipment(itemId)),
        Arguments.of("inebriety.txt", 2, (Function<Integer, Boolean>) ItemDatabase::isBooze),
        Arguments.of("fullness.txt", 2, (Function<Integer, Boolean>) ItemDatabase::isFood));
  }

  @Test
  public void testPotions() throws IOException {
    boolean bogus = false;
    for (var id : allItems()) {
      var type = ItemDatabase.getConsumptionType(id);
      switch (type) {
        case POTION, AVATAR_POTION -> {
          EnumSet<Attribute> attributes = ItemDatabase.getAttributes(id);
          // All Potions And avatar potions are multiusable by default.
          // Do not allow a redundant "multiple" attribute
          if (attributes.contains(Attribute.MULTIPLE)) {
            // Print to stdout so we can see all errors, rather than
            // stopping test after first failure.
            System.out.println(
                "Item '"
                    + ItemDatabase.getItemName(id)
                    + " is a "
                    + type
                    + " which is redundantly declared to be multi-usable");
            bogus = true;
          }
        }
      }
    }
    if (bogus) {
      fail("Potions and Avatar Potions with redundant 'multiple' attribute");
    }
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testItemPresence(String dataFile, int version, Function<Integer, Boolean> predicate)
      throws IOException {
    var filteredItems = datafileItems(dataFile, version);
    var items = allItems();

    for (int id : items) {
      if (predicate.apply(id)) {
        // At least one of "seal-clubbing club", "[1]seal-clubbing club" should be present.
        String name = ItemDatabase.getItemDataName(id);
        String bracketedName = "[" + id + "]" + name;
        assertThat(
            bracketedName + " is not present in " + dataFile,
            true,
            // Explicitly apply the matcher to keep the error message manageable.
            equalTo(anyOf(hasItem(name), hasItem(bracketedName)).matches(filteredItems)));
      }
    }
  }

  @Test
  void noDuplicateEquipmentEntries() throws IOException {
    var items = new HashSet<String>();
    try (BufferedReader reader = FileUtilities.getVersionedReader("equipment.txt", 2)) {
      String[] fields;
      while ((fields = FileUtilities.readData(reader)) != null) {
        if (fields.length == 1) {
          continue;
        }
        var thing = fields[0];

        if (!thing.isBlank()) {
          var inserted = items.add(thing);
          assertThat(thing, inserted, is(true));
        }
      }
    }
  }

  @Test
  public void testFamiliarHatchlings() throws IOException {
    var hatchlings = datafileItems("familiars.txt", 4, 4);
    var items = allItems();
    for (var id : items) {
      if (ItemDatabase.getConsumptionType(id) == ConsumptionType.FAMILIAR_HATCHLING) {
        var name = ItemDatabase.getItemDataName(id);
        // replica familiars from Legacy of Loathing are special
        if (!name.startsWith("replica ")) {
          assertThat(
              String.format("%s is in items.txt but not in familiars.txt", name),
              hatchlings,
              hasItem(name));
          hatchlings.remove(name);
        }
      }
    }

    assertThat(
        String.format("%s is in familiars.txt but not in items.txt", String.join(", ", hatchlings)),
        hatchlings,
        hasSize(0));
  }

  @Test
  public void testFamiliarSpecificEquipment() throws IOException {
    var familiarSpecificEquipment = datafileItems("familiars.txt", 4, 5);

    for (var name : familiarSpecificEquipment) {
      assertThat(
          String.format("%s is in familiars.txt but not in items.txt", name),
          ItemDatabase.getItemId(name),
          greaterThan(0));
    }
  }

  @Test
  public void testCoinmasterBuyables() throws IOException {
    var buyables = datafileItems("coinmasters.txt", 2, 3);

    Pattern withNum = Pattern.compile("(.*) \\(\\d+\\)");

    for (var name : buyables) {
      var match = withNum.matcher(name);
      if (match.find()) {
        name = match.group(1);
      }
      assertThat(
          String.format("%s is in coinmasters.txt but not in items.txt", name),
          ItemDatabase.getItemId(name),
          greaterThan(0));
    }
  }

  @Test
  public void testValidZonesForCombats() {
    String file = "combats.txt";
    int version = 1;
    String[] fields;
    try (BufferedReader reader = FileUtilities.getVersionedReader(file, version)) {
      while ((fields = FileUtilities.readData(reader)) != null) {
        if (fields.length == 1) {
          continue;
        }
        // First field must be a string and a valid zone name
        String zone = fields[0];
        assertTrue(AdventureDatabase.validateAdventureArea(zone), "Problem with " + zone);
      }
    } catch (IOException e) {
      fail("Couldn't read from combats.txt");
    }
  }

  @Test
  public void testValidFrequencyForCombats() {
    String file = "combats.txt";
    int version = 1;
    String[] fields;
    try (BufferedReader reader = FileUtilities.getVersionedReader(file, version)) {
      while ((fields = FileUtilities.readData(reader)) != null) {
        if (fields.length == 1) {
          continue;
        }
        // Second field is a Combat frequency - -1 or 0 <= x <= 100
        String freq = fields[1];
        int iFreq = StringUtilities.parseInt(freq);
        assertThat(
            "Problem with frequency " + freq + " on line beginning with " + fields[0],
            iFreq,
            greaterThanOrEqualTo(-1));
        assertThat(
            "Problem with frequency " + freq + " on line beginning with " + fields[0],
            iFreq,
            lessThanOrEqualTo(100));
      }
    } catch (IOException e) {
      fail("Couldn't read from combats.txt");
    }
  }

  @Test
  public void testValidMonstersForCombats() {
    String file = "combats.txt";
    int version = 1;
    String[] fields;
    try (BufferedReader reader = FileUtilities.getVersionedReader(file, version)) {
      while ((fields = FileUtilities.readData(reader)) != null) {
        if (fields.length == 1) {
          continue;
        }
        // Third to end are monsters.
        // Appearance weights proceeded by colon not checked.
        for (int i = 2; i < fields.length; i++) {
          String name;
          String nameAll = fields[i];
          if (nameAll.contains(":")) {
            String[] parts = nameAll.split(":");
            name = parts[0];
          } else {
            name = nameAll;
          }
          MonsterData monster = MonsterDatabase.findMonster(name);
          assertNotNull(
              monster, "Problem with monster " + name + " on line beginning with " + fields[0]);
        }
      }
    } catch (IOException e) {
      fail("Couldn't read from combats.txt");
    }
  }

  @Test
  public void modifiersShouldApplyToValidItems() {
    String file = "modifiers.txt";
    int version = 3;
    String[] fields;
    try (BufferedReader reader = FileUtilities.getVersionedReader(file, version)) {
      while ((fields = FileUtilities.readData(reader)) != null) {
        String identifier = fields[0];
        String name = fields[1];
        switch (identifier) {
          case "Item", "Clancy" -> {
            var id = ItemDatabase.getExactItemId(name);
            if (id == -1) {
              if (!CafeDatabase.isCafeConsumable(name)) {
                fail("unrecognised item " + name);
              }
            }
          }
          case "Effect" -> {
            var id = EffectDatabase.getEffectId(name, true);
            if (id < 0) {
              fail("unrecognised effect " + name);
            }
          }
          case "Skill" -> {
            var id = SkillDatabase.getSkillId(name, true);
            if (id < 0) {
              fail("unrecognised skill " + name);
            }
            assertTrue(SkillDatabase.isPassive(id), "Skill " + name + " should be passive");
          }
          case "Familiar", "Throne" -> {
            if (!"(none)".equals(name)) {
              var id = FamiliarDatabase.getFamiliarId(name, false);
              if (id < 0) {
                fail("unrecognised familiar " + name);
              }
            }
          }
          case "Thrall" -> {
            var thrall = PastaThrallData.typeToData(name);
            if (thrall == null) {
              fail("unrecognised thrall " + name);
            }
          }
          case "Outfit" -> {
            var outfit =
                EquipmentDatabase.normalOutfits.values().stream()
                    .anyMatch(x -> x.getName().equals(name));
            if (!outfit) {
              fail("unrecognised outfit " + name);
            }
          }
          case "Sign" -> {
            var sign = ZodiacSign.find(name);
            if (sign == ZodiacSign.NONE) {
              fail("unrecognised sign " + name);
            }
          }
          case "Zone" -> {
            var zone = AdventureDatabase.PARENT_LIST.stream().anyMatch(x -> x.equals(name));
            if (!zone) {
              fail("unrecognised zone " + name);
            }
          }
          case "Loc" -> {
            var loc = AdventureDatabase.validateAdventureArea(name);
            if (!loc) {
              fail("unrecognised location " + name);
            }
          }
          case "Synergy", "MutexI" -> {
            assertThat(name, containsString("/"));
            for (var item : name.split("/")) {
              var id = ItemDatabase.getExactItemId(item);
              if (id < 0) {
                fail("unrecognised item " + item);
              }
            }
          }
          case "MutexE" -> {
            assertThat(name, containsString("/"));
            for (var effect : name.split("/")) {
              var id = EffectDatabase.getEffectId(effect, true);
              if (id < 0) {
                fail("unrecognised effect " + effect);
              }
            }
          }
          case "Florist" -> {
            var flower = FloristRequest.Florist.getFlower(name);
            if (flower == null) {
              fail("unrecognised flower " + name);
            }
          }
          case "Path" -> {
            var path = AscensionPath.nameToPath(name);
            if (path == Path.NONE) {
              fail("unrecognised path " + name);
            }
          }
          case "Class" -> {
            var ascensionClass = AscensionClass.find(name);
            if (ascensionClass == null) {
              fail("unrecognised class " + name);
            }
          }
          case "Motorbike",
              "Snowsuit",
              "Edpiece",
              "Rumpus",
              "Event",
              "MaxCat",
              "Horsery",
              "BoomBox",
              "RetroCape",
              "BackupCamera",
              "UnbreakableUmbrella",
              "JurassicParka",
              "LedCandle",
              "Mask",
              "Ensorcel",
              "Robot",
              "RobotTop",
              "RobotRight",
              "RobotBottom",
              "RobotLeft",
              "RobotCPU" -> {
            // all fine
          }
          default -> fail("unrecognised identifier " + identifier);
        }
      }
    } catch (IOException e) {
      fail("Couldn't read from " + file);
    }
  }

  @Test
  public void dailyLimitsShouldApplyToValidItems() {
    String file = "dailylimits.txt";
    int version = 1;
    String[] fields;
    try (BufferedReader reader = FileUtilities.getVersionedReader(file, version)) {
      while ((fields = FileUtilities.readData(reader)) != null) {
        String identifier = fields[0];
        String name = fields[1];
        switch (identifier) {
          case "Use", "Eat", "Drink", "Chew" -> {
            var id = ItemDatabase.getExactItemId(name);
            if (id == -1) {
              fail("unrecognised item " + name);
            }
          }
          case "Cast", "Tome" -> {
            var id = SkillDatabase.getSkillId(name, true);
            if (id < 0) {
              fail("unrecognised skill " + name);
            }
          }
          default -> fail("unrecognised identifier " + identifier);
        }
      }
    } catch (IOException e) {
      fail("Couldn't read from " + file);
    }
  }

  @Test
  public void dailyLimitsShouldUseValidPreferences() throws IOException {
    var preferences = datafileItems("dailylimits.txt", 1, 2);
    assertThat(preferences, everyItem(isUserPreference()));
  }

  @Test
  public void npcStoresShouldSellItems() {
    String file = "npcstores.txt";
    int version = 2;
    String[] fields;
    try (BufferedReader reader = FileUtilities.getVersionedReader(file, version)) {
      while ((fields = FileUtilities.readData(reader)) != null) {
        String item = fields[2];
        var id = ItemDatabase.getExactItemId(item);
        if (id == -1) {
          fail("unrecognised item " + item);
        }
      }
    } catch (IOException e) {
      fail("Couldn't read from " + file);
    }
  }

  @Test
  public void concoctionsAreItems() {
    String file = "concoctions.txt";
    int version = 3;
    String[] fields;
    Pattern withNum = Pattern.compile("(.*) \\(\\d+\\)");
    try (BufferedReader reader = FileUtilities.getVersionedReader(file, version)) {
      while ((fields = FileUtilities.readData(reader)) != null) {
        String item = fields[0];
        switch (fields[1]) {
          case "VYKEA", "SUSHI", "STILLSUIT" -> {
            // not items
          }
          default -> {
            var match = withNum.matcher(item);
            if (match.find()) {
              item = match.group(1);
            }
            var id = ItemDatabase.getExactItemId(item);
            if (id == -1) {
              fail("unrecognised item " + item + ".");
            }
          }
        }
      }
    } catch (IOException e) {
      fail("Couldn't read from " + file);
    }
  }

  @Test
  public void concoctionIngredientsAreItems() {
    String file = "concoctions.txt";
    int version = 3;
    String[] fields;
    Pattern withNum = Pattern.compile("(.*) \\(\\d+\\)");
    try (BufferedReader reader = FileUtilities.getVersionedReader(file, version)) {
      while ((fields = FileUtilities.readData(reader)) != null) {
        if (fields.length < 3) {
          // no ingredients
          continue;
        }
        String item = fields[0];
        switch (fields[1]) {
          case "CLIPART" -> {
            // not items
          }
          default -> {
            for (int i = 2; i < fields.length; i++) {
              var ingredient = fields[i];
              var match = withNum.matcher(ingredient);
              if (match.find()) {
                ingredient = match.group(1);
              }
              var id = ItemDatabase.getExactItemId(ingredient);
              if (id == -1) {
                fail("unrecognised item " + ingredient + " for item " + item + ".");
              }
            }
          }
        }
      }
    } catch (IOException e) {
      fail("Couldn't read from " + file);
    }
  }

  @Test
  public void everyForceNoncombatZoneHasDefault() {
    for (var adventure : AdventureDatabase.getAsLockableListModel()) {
      if (adventure.getAdventureNumber() < 0) continue;

      var pref = "lastNoncombat" + adventure.getAdventureNumber();
      assertThat(Preferences.containsDefault(pref), is(adventure.getForceNoncombat() > 0));
    }
  }

  @Test
  public void everyEncounterIsInAZone() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("encounters.txt", KoLConstants.ENCOUNTERS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        assertThat(
            "Couldn't validate zone " + data[0] + " for encounter " + data[2] + ".",
            data[0].equals("*") || AdventureDatabase.validateAdventureArea(data[0]),
            is(true));
      }
    } catch (IOException e) {
      fail("Couldn't read from encounters.txt");
    }
  }

  @Test
  public void monsterElementsAreValid() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("monsters.txt", KoLConstants.MONSTERS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        StringTokenizer tokens = new StringTokenizer(data[3], " ");
        while (tokens.hasMoreTokens()) {
          var value = tokens.nextToken();
          var attribute = MonsterData.Attribute.find(value);
          var elemental =
              value.equals("E:")
                  || attribute == MonsterData.Attribute.EA
                  || attribute == MonsterData.Attribute.ED;

          if (!elemental) continue;
          if (!tokens.hasMoreTokens()) continue;

          String next = MonsterData.parseString(tokens.nextToken(), tokens);
          var element = MonsterData.parseElement(next);
          assertThat(
              "Monster" + data[0] + " references an invalid element \"" + next + "\"",
              element,
              notNullValue());
        }
      }
    } catch (IOException e) {
      fail("Couldn't read from monsters.txt");
    }
  }
}
