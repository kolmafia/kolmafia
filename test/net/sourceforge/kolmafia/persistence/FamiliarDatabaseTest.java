package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

public class FamiliarDatabaseTest {
  @Test
  void returnsExpectedFieldsForKnownRow() {
    int familiarId = FamiliarPool.BLOOD_FACED_VOLLEYBALL;
    String name = "Blood-Faced Volleyball";
    String image = "familiar12.gif";
    int larva = ItemPool.BLOOD_FACED_VOLLEYBALL;
    String item = "palm-frond toupee";

    assertThat(FamiliarDatabase.getFamiliarName(familiarId), is(name));
    assertThat(FamiliarDatabase.getFamiliarId(name), is(familiarId));
    assertThat(FamiliarDatabase.getFamiliarType(familiarId), is("stat0"));
    assertThat(FamiliarDatabase.isVolleyType(familiarId), is(true));

    assertThat(FamiliarDatabase.getFamiliarImageLocation(familiarId), is(image));
    assertThat(FamiliarDatabase.getFamiliarByImageLocation(image), is(familiarId));

    assertThat(FamiliarDatabase.getFamiliarLarva(familiarId), is(larva));

    assertThat(FamiliarDatabase.getFamiliarItem(familiarId), is(item));
    assertThat(FamiliarDatabase.getFamiliarItemId(familiarId), is(ItemDatabase.getItemId(item)));
    assertThat(FamiliarDatabase.getFamiliarByItem(item), is(familiarId));

    assertThat(FamiliarDatabase.getFamiliarSkills(familiarId), is(new int[] {0, 1, 3, 2}));
    assertThat(
        FamiliarDatabase.getFamiliarAttributes(familiarId),
        contains("mineral", "object", "haseyes", "hovers", "orb", "spooky"));
  }

  @Test
  void returnsDefaultsForMissingValues() {
    int familiarId = FamiliarPool.PLASTIC_GROCERY_BAG;

    assertThat(FamiliarDatabase.getFamiliarType(familiarId), is("none"));

    assertThat(FamiliarDatabase.getFamiliarLarva(familiarId), is(-1));

    assertThat(FamiliarDatabase.getFamiliarItem(familiarId), is(""));
    assertThat(FamiliarDatabase.getFamiliarItemId(familiarId), is(-1));

    assertThat(FamiliarDatabase.getFamiliarSkills(familiarId), is(new int[] {0, 0, 0, 0}));
  }

  @Test
  void returnsDefaultsForMissingFamiliar() {
    int familiarId = 13;

    assertThat(FamiliarDatabase.getFamiliarType(familiarId), is("none"));

    assertThat(FamiliarDatabase.getFamiliarLarva(familiarId), is(0));

    assertThat(FamiliarDatabase.getFamiliarItem(familiarId), nullValue());
    assertThat(FamiliarDatabase.getFamiliarItemId(familiarId), is(-1));

    // assertThat(FamiliarDatabase.getFamiliarSkills(familiarId), is(new int[] {0, 0, 0, 0}));
  }

  @Test
  void invalidItemDefaultsToMinusOne() {
    assertThat(FamiliarDatabase.getFamiliarByItem("stuffed club"), is(-1));
  }
}
