package net.sourceforge.kolmafia.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.RequestLogger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DebugDatabaseTest {

  /* TODO: implement or delete these tests
  @Test
  public void checkItems()
  {
  }

  @Test
  public void itemDescriptionText()
  {
  }

  @Test
  public void cafeItemDescriptionText()
  {
  }

  @Test
  public void rawItemDescriptionText()
  {
  }

  @Test
  public void testRawItemDescriptionText()
  {
  }

  @Test
  public void testItemDescriptionText()
  {
  }

  @Test
  public void parseItemId()
  {
  }
  */

  @Test
  public void parseName() {
    String goodName = DebugDatabase.parseName("<b>goodName</b>");
    String badName = DebugDatabase.parseName("badName");
    assertEquals("goodName", goodName, "Could not parse name");
    assertEquals("", badName, "Name Returned " + badName);
  }
  /* TODO: implement or delete these tests
  @Test
  public void parsePrice()
  {
  }

  @Test
  public void parseAccess()
  {
  }

  @Test
  public void parseType()
  {
  }

  @Test
  public void typeToPrimary()
  {
  }

  @Test
  public void typeToSecondary()
  {
  }

  @Test
  public void parseLevel()
  {
  }

  @Test
  public void parseSize()
  {
  }

  @Test
  public void parseConsumableSize()
  {
  }

  @Test
  public void parsePower()
  {
  }

  @Test
  public void parseWeaponType()
  {
  }

  @Test
  public void parseReq()
  {
  }

  @Test
  public void parseFullness()
  {
  }

  @Test
  public void parseInebriety()
  {
  }

  @Test
  public void parseToxicity()
  {
  }

  @Test
  public void parseFamiliar()
  {
  }

  @Test
  public void parseItemEnchantments()
  {
  }

  @Test
  public void parseRestores()
  {
  }

  @Test
  public void testParseItemEnchantments()
  {
  }

  @Test
  public void testParseItemEnchantments1()
  {
  }

  @Test
  public void checkOutfits()
  {
  }

  @Test
  public void outfitDescriptionText()
  {
  }

  @Test
  public void readOutfitDescriptionText()
  {
  }

  @Test
  public void rawOutfitDescriptionText()
  {
  }

  @Test
  public void testOutfitDescriptionText()
  {
  }

  @Test
  public void parseOutfitEnchantments()
  {
  }

  @Test
  public void testParseOutfitEnchantments()
  {
  }

  @Test
  public void checkEffects()
  {
  }

  @Test
  public void parseEffectId()
  {
  }

  @Test
  public void parseImage()
  {
  }

  @Test
  public void parseEffectDescid()
  {
  }

  @Test
  public void effectDescriptionText()
  {
  }

  @Test
  public void readEffectDescriptionText()
  {
  }

  @Test
  public void parseEffectEnchantments()
  {
  }

  @Test
  public void testParseEffectEnchantments()
  {
  }

  @Test
  public void testParseEffectEnchantments1()
  {
  }

  @Test
  public void checkSkills()
  {
  }

  @Test
  public void parseSkillId()
  {
  }

  @Test
  public void parseSkillType()
  {
  }

  @Test
  public void parseSkillMPCost()
  {
  }

  @Test
  public void parseSkillEffectName()
  {
  }

  @Test
  public void parseSkillEffectId()
  {
  }

  @Test
  public void parseSkillEffectDuration()
  {
  }

  @Test
  public void skillDescriptionText()
  {
  }

  @Test
  public void readSkillDescriptionText()
  {
  }

  @Test
  public void parseSkillEnchantments()
  {
  }

  @Test
  public void testParseSkillEnchantments()
  {
  }

  @Test
  public void testParseSkillEnchantments1()
  {
  }

  @Test
  public void checkPlurals()
  {
  }

  @Test
  public void checkPowers()
  {
  }

  @Test
  public void checkShields()
  {
  }

  @Test
  public void testCheckShields()
  {
  }

  @Test
  public void checkPotions()
  {
  }

  @Test
  public void checkConsumables()
  {
  }

  @Test
  public void parseQuality()
  {
  }

  @Test
  public void checkFamiliarsInTerrarium()
  {
  }

  @Test
  public void checkFamiliarImages()
  {
  }

  @Test
  public void checkConsumptionData()
  {
  }
  */
  @Test
  @Disabled("fails due to data problems")
  public void checkPulverizationData() {
    String expectedOutput = "Checking pulverization data...\n";
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(ostream);
    // Inject custom output stream.
    RequestLogger.openCustom(out);

    DebugDatabase.checkPulverizationData();

    String output = ostream.toString();
    assertEquals(expectedOutput, output, "checkPulverizationData variances: \n" + output);
  }

  @Test
  @Disabled("Relies on external resources (wiki)")
  public void checkZapGroups() {
    String expectedOutput = "Checking zap groups...\n";
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(ostream);
    // Inject custom output stream.
    RequestLogger.openCustom(out);

    DebugDatabase.checkZapGroups();

    String output = ostream.toString();
    assertEquals(expectedOutput, output, "checkZapGroups variances: \n" + output);
  }

  @Test
  public void checkManuel() {
    String expectedOutput =
        "Checking Monster Manuel...\n"
            + "Page A\n"
            + "Page B\n"
            + "Page C\n"
            + "Page D\n"
            + "Page E\n"
            + "Page F\n"
            + "Page G\n"
            + "Page H\n"
            + "Page I\n"
            + "Page J\n"
            + "Page K\n"
            + "Page L\n"
            + "Page M\n"
            + "Page N\n"
            + "Page O\n"
            + "Page P\n"
            + "Page Q\n"
            + "Page R\n"
            + "Page S\n"
            + "Page T\n"
            + "Page U\n"
            + "Page V\n"
            + "Page W\n"
            + "Page X\n"
            + "Page Y\n"
            + "Page Z\n"
            + "Page -\n";
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(ostream);
    // Inject custom output stream.
    RequestLogger.openCustom(out);

    DebugDatabase.checkManuel();

    String output = ostream.toString();
    assertEquals(expectedOutput, output, "checkManuel variances: \n" + output);
  }

  @Test
  @Disabled("need to figure out if test is reporting valid errors.")
  public void checkMeat() {
    String expectedOutput = "";
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(ostream);
    // Inject custom output stream.
    RequestLogger.openCustom(out);

    DebugDatabase.checkMeat();

    String output = ostream.toString();
    assertEquals(expectedOutput, output, "checkMeat variances: \n" + output);
  }
}
