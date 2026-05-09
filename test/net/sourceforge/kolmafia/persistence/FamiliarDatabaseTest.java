package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
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

    assertThat(FamiliarDatabase.getFamiliarType(familiarId), is("none"));

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

    assertThat(FamiliarDatabase.getFamiliarType(familiarId), is("none"));
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
    assertThat(FamiliarDatabase.getFamiliarType(familiarId), is("combat0,block,mp0,meat1"));
    assertThat(FamiliarDatabase.isCombat0Type(familiarId), is(true));
    assertThat(FamiliarDatabase.isCombatType(familiarId), is(true));
    assertThat(FamiliarDatabase.isBlockType(familiarId), is(true));
    assertThat(FamiliarDatabase.isMp0Type(familiarId), is(true));
    assertThat(FamiliarDatabase.isMeat1Type(familiarId), is(true));
  }

  @Test
  void returnsExpectedTypeOrderAndFlagsForFairyType() {
    int familiarId = FamiliarPool.COFFEE_PIXIE;
    assertThat(FamiliarDatabase.getFamiliarType(familiarId), is("item0,meat0"));
    assertThat(FamiliarDatabase.isCombatType(familiarId), is(false));
    assertThat(FamiliarDatabase.isMeatDropType(familiarId), is(true));
    assertThat(FamiliarDatabase.isFairyType(familiarId), is(true));
    assertThat(FamiliarDatabase.isFairyType(familiarId, DoubleModifier.FAIRY_WEIGHT), is(true));
    assertThat(
        FamiliarDatabase.isFairyType(familiarId, DoubleModifier.FOOD_FAIRY_WEIGHT), is(false));
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
    assertThat(
        data,
        containsString(
            "286\tSynthetic Rock\tsynthrock.gif\tnone\tsynthetic rock\t\t0\t0\t0\t0\tmineral,object,hard\n"));
  }
}
