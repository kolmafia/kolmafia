package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import org.junit.jupiter.api.Test;

public class FamiliarDatabaseTest {
  @Test
  void returnsExpectedFieldsForKnownFamiliar() {
    int familiarId = FamiliarPool.BLOOD_FACED_VOLLEYBALL;
    String name = "Blood-Faced Volleyball";
    String image = "familiar12.gif";
    String larva = "blood-faced volleyball";
    String item = "palm-frond toupee";

    assertThat(FamiliarDatabase.getFamiliarName(familiarId), is(name));
    assertThat(FamiliarDatabase.getFamiliarId(name), is(familiarId));
    assertThat(FamiliarDatabase.getFamiliarType(familiarId), is("stat0"));
    assertThat(FamiliarDatabase.isVolleyType(familiarId), is(true));

    assertThat(FamiliarDatabase.getFamiliarImageLocation(familiarId), is(image));
    assertThat(FamiliarDatabase.getFamiliarByImageLocation(image), is(familiarId));

    assertThat(FamiliarDatabase.getFamiliarLarva(familiarId), is(ItemDatabase.getItemId(larva)));

    assertThat(FamiliarDatabase.getFamiliarItem(familiarId), is(item));
    assertThat(FamiliarDatabase.getFamiliarItemId(familiarId), is(ItemDatabase.getItemId(item)));
    assertThat(FamiliarDatabase.getFamiliarByItem(item), is(familiarId));

    assertThat(FamiliarDatabase.getFamiliarSkills(familiarId), is(new int[] {0, 1, 3, 2}));
    assertThat(
        FamiliarDatabase.getFamiliarAttributes(familiarId),
        contains("mineral", "object", "haseyes", "hovers", "orb", "spooky"));
  }
}
