package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Utilities.verboseDelete;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class FamiliarDatabaseTest {

  // writing familiars.txt is a side effect of one or more of these tests but it is never cleaned up
  // elsewhere.
  private static final String famFile = KoLConstants.ROOT_LOCATION + "/data/" + "familiars.txt";

  @AfterAll
  public static void afterAll() {
    verboseDelete(famFile);
  }

  @Test
  void returnsExpectedFieldsForKnownRow() {
    int familiarId = FamiliarPool.BLOOD_FACED_VOLLEYBALL;
    String name = "Blood-Faced Volleyball";
    String image = "familiar12.gif";
    int larva = ItemPool.BLOOD_FACED_VOLLEYBALL;
    String item = "palm-frond toupee";

    var data = FamiliarDatabase.getFamiliarRaceData(familiarId);

    assertThat(FamiliarDatabase.getFamiliarName(familiarId), is(name));
    assertThat(FamiliarDatabase.getFamiliarId(name), is(familiarId));
    assertThat(data.types(), is("stat0"));
    assertThat(data.isVolleyType(), is(true));

    assertThat(FamiliarDatabase.getFamiliarImageLocation(familiarId), is(image));
    assertThat(FamiliarDatabase.getFamiliarByImageLocation(image), is(familiarId));

    assertThat(FamiliarDatabase.getFamiliarLarva(familiarId), is(larva));

    assertThat(FamiliarDatabase.getFamiliarItem(familiarId), is(item));
    assertThat(FamiliarDatabase.getFamiliarItemId(familiarId), is(ItemDatabase.getItemId(item)));
    assertThat(FamiliarDatabase.getFamiliarByItem(item), is(familiarId));

    assertThat(FamiliarDatabase.getFamiliarSkills(familiarId), is(new int[] {0, 1, 3, 2}));
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 1), is(0));
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 2), is(1));
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 3), is(3));
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 4), is(2));

    assertThat(
        FamiliarDatabase.getFamiliarAttributes(familiarId),
        contains("mineral", "object", "haseyes", "hovers", "orb", "spooky"));
    assertThat(FamiliarDatabase.hasAttribute(familiarId, "orb"), is(true));
  }

  @Test
  void returnsDefaultsForMissingValues() {
    int familiarId = FamiliarPool.PLASTIC_GROCERY_BAG;

    var data = FamiliarDatabase.getFamiliarRaceData(familiarId);

    assertThat(data.types(), is("none"));

    assertThat(FamiliarDatabase.getFamiliarLarva(familiarId), is(-1));

    assertThat(FamiliarDatabase.getFamiliarItem(familiarId), is(""));
    assertThat(FamiliarDatabase.getFamiliarItemId(familiarId), is(-1));

    assertThat(FamiliarDatabase.getFamiliarSkills(familiarId), is(new int[] {0, 0, 0, 0}));

    assertThat(FamiliarDatabase.getFamiliarAttributes(familiarId), empty());
    assertThat(FamiliarDatabase.hasAttribute(familiarId, "sentient"), is(false));
  }

  @Test
  void returnsDefaultsForMissingFamiliar() {
    int familiarId = 13;

    assertThat(FamiliarDatabase.getFamiliarImageLocation(familiarId), is("debug.gif"));

    assertThat(FamiliarDatabase.getFamiliarLarva(familiarId), is(0));

    assertThat(FamiliarDatabase.getFamiliarItem(familiarId), nullValue());
    assertThat(FamiliarDatabase.getFamiliarItemId(familiarId), is(-1));

    assertThat(FamiliarDatabase.getFamiliarSkills(familiarId), is(new int[] {0, 0, 0, 0}));
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 1), nullValue());
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 2), nullValue());
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 3), nullValue());
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 4), nullValue());

    assertThat(FamiliarDatabase.getFamiliarAttributes(familiarId), empty());
    assertThat(FamiliarDatabase.hasAttribute(familiarId, "sentient"), is(false));
  }

  @Test
  void invalidItemDefaultsToMinusOne() {
    assertThat(FamiliarDatabase.getFamiliarByItem("stuffed club"), is(-1));
  }

  @Test
  void settingSkillsShouldOverride() {
    int familiarId = FamiliarPool.LEPRECHAUN;
    FamiliarDatabase.setFamiliarSkills(familiarId, new int[] {0, 1, 2, 3});

    assertThat(FamiliarDatabase.getFamiliarSkills(familiarId), is(new int[] {0, 1, 2, 3}));
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 1), is(0));
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 2), is(1));
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 3), is(2));
    assertThat(FamiliarDatabase.getFamiliarSkill(familiarId, 4), is(3));
  }

  @Test
  void settingImageShouldOverride() {
    int familiarId = FamiliarPool.LEPRECHAUN;
    FamiliarDatabase.setFamiliarImageLocation(familiarId, "leprechaun.gif");

    assertThat(FamiliarDatabase.getFamiliarImageLocation(familiarId), is("leprechaun.gif"));
    assertThat(FamiliarDatabase.getFamiliarByImageLocation("leprechaun.gif"), is(familiarId));
  }

  @Test
  void returnsExpectedTypeOrderAndFlagsForMultiTypeFamiliar() {
    int familiarId = FamiliarPool.HANUKKIMBO_DREIDL;

    var data = FamiliarDatabase.getFamiliarRaceData(familiarId);

    assertThat(data.types(), is("combat0,block,mp0,meat1"));
    assertThat(data.isCombat0Type(), is(true));
    assertThat(data.isCombatType(), is(true));
    assertThat(data.isBlockType(), is(true));
    assertThat(data.isMp0Type(), is(true));
    assertThat(data.isMeat1Type(), is(true));
  }

  @Test
  void returnsExpectedTypeOrderAndFlagsForFairyType() {
    int familiarId = FamiliarPool.COFFEE_PIXIE;

    var data = FamiliarDatabase.getFamiliarRaceData(familiarId);

    assertThat(data.types(), is("item0,meat0"));
    assertThat(data.isCombatType(), is(false));
    assertThat(data.isMeatDropType(), is(true));
    assertThat(data.isFairyType(), is(true));
    assertThat(data.isFairyType(DoubleModifier.FAIRY_WEIGHT), is(true));
    assertThat(data.isFairyType(DoubleModifier.FOOD_FAIRY_WEIGHT), is(false));
  }

  @Test
  void returnsExpectedSubtypeFairyFlags() {
    var vintner = FamiliarDatabase.getFamiliarRaceData(FamiliarPool.VAMPIRE_VINTNER);
    assertThat(vintner.types(), is("item2,combat0,hp0,drop"));
    assertThat(vintner.isFairyType(DoubleModifier.BOOZE_FAIRY_WEIGHT), is(true));
    assertThat(
        FamiliarDatabase.getFamiliarRaceData(FamiliarPool.PEPPERMINT_RHINO)
            .isFairyType(DoubleModifier.CANDY_FAIRY_WEIGHT),
        is(true));
    assertThat(
        FamiliarDatabase.getFamiliarRaceData(FamiliarPool.COOKBOOKBAT)
            .isFairyType(DoubleModifier.FOOD_FAIRY_WEIGHT),
        is(true));
  }

  @Test
  public void itShouldWriteFamiliars() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(os);
    FamiliarDatabase.reset();
    FamiliarDatabase.writeFamiliars(ps);
    String data = os.toString();

    assertThat(
        data,
        containsString(
            "1\tMosquito\tfamiliar1.gif\tcombat0,hp0\tmosquito larva\thypodermic needle\t2\t1\t3\t0\tsentient,organic,insect,animal,haseyes,bite,haswings,flies,fast\n"));
    assertThat(data, containsString("13\n"));
    assertThat(data, containsString("132\tSnowhitman\tsnowhitman.gif\tnone\t\t\t0\t0\t0\t0\n"));
    assertThat(
        data,
        containsString(
            "286\tSynthetic Rock\tsynthrock.gif\tnone\tsynthetic rock\t\t0\t0\t0\t0\tmineral,object,hard\n"));
  }
}
