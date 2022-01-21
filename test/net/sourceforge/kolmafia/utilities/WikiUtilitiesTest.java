package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import org.junit.jupiter.api.Test;

public class WikiUtilitiesTest {
  @Test
  public void getWikiLocationForMonster() {
    var vampire = MonsterDatabase.findMonster("spooky vampire");
    String link = WikiUtilities.getWikiLocation(vampire);
    assertEquals(
        "https://kol.coldfront.net/thekolwiki/index.php/Data:Spooky_vampire_%28Spooky_Forest%29",
        link);
  }

  @Test
  public void getWikiLocationWithQuestionMark() {
    var drippyPlum = AdventureResult.tallyItem("drippy plum(?)");
    String link = WikiUtilities.getWikiLocation(drippyPlum);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Drippy_plum(%3F)", link);
  }

  @Test
  public void getWikiLocationForItemThatIsAlsoMonster() {
    var flange = AdventureResult.tallyItem("flange");
    String link = WikiUtilities.getWikiLocation(flange);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Flange_(item)", link);
  }

  @Test
  public void getWikiLocationForMonsterThatIsAlsoItem() {
    var flange = MonsterDatabase.findMonster("Flange");
    String link = WikiUtilities.getWikiLocation(flange);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Data:Flange_%28monster%29", link);
  }

  @Test
  public void icePorterItemHasANonStandardDisambiguation() {
    var icePorter = AdventureResult.tallyItem("ice porter");
    String link = WikiUtilities.getWikiLocation(icePorter);
    assertEquals("https://kol.coldfront.net/thekolwiki/index.php/Ice_porter_(drink)", link);
  }
}
