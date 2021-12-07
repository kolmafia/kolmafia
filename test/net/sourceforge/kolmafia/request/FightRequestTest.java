package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.*;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.Test;

/** Coverage driven collection of tests for FightRequest. */
public class FightRequestTest {
  private final FightRequest fr = FightRequest.INSTANCE;

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
  public void commerceGhostIncrementsByOneOnFight() {
    KoLCharacter.reset("the Tristero");
    FamiliarData fam = new FamiliarData(FamiliarPool.GHOST_COMMERCE);
    KoLCharacter.setFamiliar(fam);
    assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
    FightRequest.currentRound = 0;
    FightRequest.updateCombatData(null, null, "You twiddle your thumbs.");
    assertEquals(1, Preferences.getInteger("commerceGhostCombats"));
  }

  // If mafia has miscounted we should move our count
  @Test
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
  public void commerceGhostResetsTo0() {
    KoLCharacter.reset("the Tristero");
    FamiliarData fam = new FamiliarData(FamiliarPool.GHOST_COMMERCE);
    KoLCharacter.setFamiliar(fam);
    Preferences.setInteger("commerceGhostCombats", 10);
    FightRequest.updateCombatData(null, null, "Nice, you bought a foo!");
    assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
  }

  @Test
  public void temptest() {
    Preferences.setInteger("commerceGhostCombats", 10);
    FightRequest.TagStatus t = new FightRequest.TagStatus();
    FightRequest.handleGhostOfCommerce("Nice, you bought a foo!", t);
    assertEquals(0, Preferences.getInteger("commerceGhostCombats"));
  }
}
