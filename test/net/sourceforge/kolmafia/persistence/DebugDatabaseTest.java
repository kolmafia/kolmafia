package net.sourceforge.kolmafia.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DebugDatabaseTest {

  private static final String LS = System.lineSeparator();

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
  @Disabled("Accesses Coldfront which is returning malformed XML")
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
    // deal with zapgroups
    File zfo = new File(KoLConstants.DATA_LOCATION, "zapreport.txt");
    if (zfo.exists()) {
      assertEquals(0, zfo.length(), "zapgroups.out expected to be empty.");
      zfo.delete();
    }
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
  @Disabled("Relies on external resources (wiki)")
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

  @Test
  public void itShouldFindSVNDuplicatesSimple() {
    var outputStream = new ByteArrayOutputStream();
    RequestLogger.openCustom(new PrintStream(outputStream));
    File svnRoot = mockSimpleSystem();
    DebugDatabase.checkLocalSVNRepository(svnRoot);
    String expected = "Found 1 repo files." + LS;
    assertEquals(expected, outputStream.toString(), "Output off");
    RequestLogger.closeCustom();
  }

  private File mockSimpleSystem() {
    File mockDot = mockFile(".svn");
    File mockDep = mockFile("dependencies.txt");
    File mockOne = mockFile("file.txt");
    File[] contents = {mockDep, mockDot, mockOne};
    return mockDir("Root", contents);
  }

  @Test
  public void itShouldFindSVNDuplicatesMoreComplex() {
    var outputStream = new ByteArrayOutputStream();
    RequestLogger.openCustom(new PrintStream(outputStream));
    File svnRoot = mockMoreComplexSystem();
    DebugDatabase.checkLocalSVNRepository(svnRoot);
    String expected = "Found 3 repo files." + LS;
    assertEquals(expected, outputStream.toString(), "Output off");
    RequestLogger.closeCustom();
  }

  private File mockMoreComplexSystem() {
    File mockDot = mockFile(".svn");
    File mockDep = mockFile("dependencies.txt");
    File mockOne = mockFile("file.txt");
    File a = mockFile("meatfarm.ash");
    File b = mockFile("farmmeat.ash");
    File[] moreContents = {a, b};
    File mockDir = mockDir("scripts", moreContents);
    File[] contents = {mockDep, mockDot, mockOne, mockDir};
    return mockDir("root", contents);
  }

  private File mockFile(String name) {
    File retVal = Mockito.mock(File.class);
    Mockito.when(retVal.getName()).thenReturn(name);
    Mockito.when(retVal.isDirectory()).thenReturn(false);
    Mockito.when(retVal.toString()).thenReturn(name);
    return retVal;
  }

  @Test
  public void itShouldFindSVNDuplicatesWhenThereAreSome() {
    var outputStream = new ByteArrayOutputStream();
    RequestLogger.openCustom(new PrintStream(outputStream));
    File svnRoot = mockDupes();
    DebugDatabase.checkLocalSVNRepository(svnRoot);
    String expected =
        "Found 2 repo files." + LS + "***" + LS + "test.ash" + LS + "test.ash" + LS + "***" + LS;
    assertEquals(expected, outputStream.toString(), "Output off");
    RequestLogger.closeCustom();
  }

  private File mockDupes() {
    File a = mockFile("test.ash");
    File b = mockFile("test.ash");
    File[] x = {a};
    File relay = mockDir("relay", x);
    File[] y = {b};
    File scripts = mockDir("scripts", y);
    File[] z = {relay};
    File one = mockDir("cheeks", z);
    File[] xx = {scripts};
    File two = mockDir("bail", xx);
    File[] yy = {one, two, mockFile(".svn"), mockFile("dependencies.txt")};
    return mockDir("root", yy);
  }

  private File mockDir(String dirname, File[] contents) {
    File retVal = mockFile(dirname);
    Mockito.when(retVal.isDirectory()).thenReturn(true);
    Mockito.when(retVal.listFiles()).thenReturn(contents);
    return retVal;
  }
}
