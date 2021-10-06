package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/* Checks for consistency across datafiles.

  For instance, items marked as equipment in items.txt should also have
  corresponding entries in equipment.txt.
*/
public class DataFileConsistencyTest {

  Set<String> datafileItems(String file, int version) throws IOException {
    Set<String> items = new HashSet<String>();
    try (BufferedReader reader = FileUtilities.getVersionedReader(file, version)) {
      String[] fields;
      while ((fields = FileUtilities.readData(reader)) != null) {
        if (fields.length == 1) {
          continue;
        }
        items.add(fields[0]);
      }
    }
    return items;
  }

  List<Integer> allItems() {
    ArrayList<Integer> items = new ArrayList<Integer>();

    int limit = ItemDatabase.maxItemId();
    for (int i = 1; i <= limit; ++i) {
      String name = ItemDatabase.getItemDataName(i);
      if (i != 13 && name != null) {
        items.add(i);
      }
    }

    return items;
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
  public void testItemPresence(String dataFile, int version, Function<Integer, Boolean> predicate) {
    Set<String> filteredItems;
    try {
      filteredItems = datafileItems(dataFile, version);
    } catch (IOException exception) {
      fail("failed initialization of " + dataFile);
      return;
    }
    List<Integer> items = allItems();

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
}
