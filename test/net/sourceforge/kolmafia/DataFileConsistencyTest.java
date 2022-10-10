package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/* Checks for consistency across datafiles.

  For instance, items marked as equipment in items.txt should also have
  corresponding entries in equipment.txt.
*/
public class DataFileConsistencyTest {
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
  public void testFamiliarHatchlings() throws IOException {
    var hatchlings = datafileItems("familiars.txt", 4, 4);
    var items = allItems();
    for (var id : items) {
      if (ItemDatabase.getConsumptionType(id) == KoLConstants.GROW_FAMILIAR) {
        var name = ItemDatabase.getItemDataName(id);
        assertThat(
            String.format("%s is in items.txt but not in familiars.txt", name),
            hatchlings,
            hasItem(name));
        hatchlings.remove(name);
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
}
