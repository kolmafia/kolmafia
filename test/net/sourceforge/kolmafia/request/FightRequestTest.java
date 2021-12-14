package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Coverage driven collection of tests for FightRequest. */
public class FightRequestTest {
  private final FightRequest fr = FightRequest.INSTANCE;

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("Test Character");
  }

  private void parseCombatData(String path, String location, String encounter) throws IOException {
    String html = Files.readString(Paths.get(path));
    FightRequest.updateCombatData(location, encounter, html);
  }

  private void parseCombatData(String path, String location) throws IOException {
    parseCombatData(path, location, null);
  }

  private void parseCombatData(String path) throws IOException {
    parseCombatData(path, null);
  }

  @Test
  public void theProvidedInstanceShouldExistAndReturnSomeThings() {
    assertNotNull(fr);
    // This does nothing but add to coverage
    FightRequest.initialize();
    assertTrue(fr.retryOnTimeout());
    assertTrue(fr.shouldFollowRedirect());
    assertFalse(FightRequest.canStillSteal());
  }

  @Test
  public void itShouldHaveAName() {
    String name = fr.toString();
    assertEquals(name, "fight.php");
  }

  @Test
  public void aSpecialMonsterShouldHaveExpectedCategory() {
    FightRequest.SpecialMonster beeOne = FightRequest.specialMonsterCategory("Queen Bee");
    MonsterData mdb = MonsterDatabase.findMonster("Queen Bee");
    FightRequest.SpecialMonster beeTwo = FightRequest.specialMonsterCategory(mdb);
    assertEquals(beeOne, beeTwo);
  }

  @Test
  public void itShouldReportDreadKisses() {
    FightRequest.resetKisses();
    assertEquals(FightRequest.dreadKisses("Woods"), 1);
    assertEquals(FightRequest.dreadKisses("Village"), 1);
    assertEquals(FightRequest.dreadKisses("Castle"), 1);
    assertEquals(FightRequest.dreadKisses("None of the above"), 0);
  }

  @Test
  public void fakeResponseTextShouldTriggerCodePaths() {
    // Not a fight
    FightRequest.updateCombatData(null, AdventureRequest.NOT_IN_A_FIGHT, null);
    assertFalse(FightRequest.inMultiFight);
    // Twiddle
    FightRequest.updateCombatData(null, null, "You twiddle your thumbs.");
  }

  // Commerce Ghost Tests
  @Test
  public void commerceGhostStartsAtProperValue() {
    KoLCharacter.reset("the Tristero");
    FamiliarData fam = new FamiliarData(FamiliarPool.GHOST_COMMERCE);
    KoLCharacter.setFamiliar(fam);
    assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
  }

  @Test
  @Disabled
  public void commerceGhostIncrementsByOneOnFight() throws IOException {
    KoLCharacter.reset("the Tristero");
    FamiliarData fam = new FamiliarData(FamiliarPool.GHOST_COMMERCE);
    KoLCharacter.setFamiliar(fam);
    assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
    FightRequest.currentRound = 0;
    // parseCombatData("request/test_fight_gnome_adv.html");
    assertEquals(1, Preferences.getInteger("commerceGhostCombats"));
  }

  // If mafia has miscounted we should move our count
  @Test
  @Disabled
  public void commerceGhostResetsTo10() {
    KoLCharacter.reset("the Tristero");
    FamiliarData fam = new FamiliarData(FamiliarPool.GHOST_COMMERCE);
    KoLCharacter.setFamiliar(fam);
    Preferences.setInteger("commerceGhostCombats", 5);
    FightRequest.updateCombatData(
        null,
        null,
        "<td style=\"color: white;\" align=center bgcolor=blue><b>Combat!</b></td></tr><tr><tdstyle=\"padding: 5px; border: 1px solid blue;\"><center><table><tr><td> Don't forget to buy a foo!");
    assertEquals(10, Preferences.getInteger("commerceGhostCombats"));
  }

  // When we turn in the quest we should reset
  @Test
  @Disabled
  public void commerceGhostResetsTo0() {
    KoLCharacter.reset("the Tristero");
    FamiliarData fam = new FamiliarData(FamiliarPool.GHOST_COMMERCE);
    KoLCharacter.setFamiliar(fam);
    Preferences.setInteger("commerceGhostCombats", 10);
    FightRequest.updateCombatData(null, null, "Nice, you bought a foo!");
    assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
  }

  @Test
  public void gnomeAdv() throws IOException {
    var familiar = FamiliarData.registerFamiliar(FamiliarPool.REAGNIMATED_GNOME, 1);
    KoLCharacter.setFamiliar(familiar);
    EquipmentManager.setEquipment(EquipmentManager.FAMILIAR, ItemPool.get(ItemPool.GNOMISH_KNEE));

    assertEquals(0, Preferences.getInteger("_gnomeAdv"));
    parseCombatData("request/test_fight_gnome_adv.html");
    assertEquals(1, Preferences.getInteger("_gnomeAdv"));
  }

  @Test
  public void mafiaThumbRingAdvs() throws IOException {
    assertEquals(0, Preferences.getInteger("_mafiaThumbRingAdvs"));
    parseCombatData("request/test_fight_mafia_thumb_ring.html");
    assertEquals(1, Preferences.getInteger("_mafiaThumbRingAdvs"));

    // Regression test for thumb ring adventures being picked up as Reagnimated Gnome adventures
    assertEquals(0, Preferences.getInteger("_gnomeAdv"));
  }

  @Test
  public void crystalBallPredictions() throws IOException {
    assertEquals("", Preferences.getString("crystalBallPredictions"));

    KoLAdventure.setLastAdventure(AdventureDatabase.getAdventure("The Neverending Party"));
    parseCombatData("request/test_fight_crystal_ball_neverending_party.html");
    FightRequest.clearInstanceData();
    // Parsing isn't initiated without the crystal ball
    assertEquals("", Preferences.getString("crystalBallPredictions"));

    // NOW equip the crystal ball
    FamiliarData familiar = FamiliarData.registerFamiliar(FamiliarPool.MOSQUITO, 1);
    KoLCharacter.setFamiliar(familiar);
    EquipmentManager.setEquipment(
        EquipmentManager.FAMILIAR, ItemPool.get(ItemPool.MINIATURE_CRYSTAL_BALL));

    parseCombatData("request/test_fight_crystal_ball_neverending_party.html");
    FightRequest.clearInstanceData();
    assertEquals(
        "0:The Neverending Party:party girl", Preferences.getString("crystalBallPredictions"));

    KoLAdventure.setLastAdventure(AdventureDatabase.getAdventure("The Red Zeppelin"));
    parseCombatData("request/test_fight_crystal_ball_zeppelin.html");
    FightRequest.clearInstanceData();
    assertEquals(
        "0:The Neverending Party:party girl|0:The Red Zeppelin:Red Snapper",
        Preferences.getString("crystalBallPredictions"));
  }
}
