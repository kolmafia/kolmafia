package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.session.ContactManager;
import org.junit.jupiter.api.Test;

class UseSkillRequestTest extends RequestTestBase {

  private static int EXPERIENCE_SAFARI = SkillDatabase.getSkillId("Experience Safari");

  @Test
  void errorDoesNotIncrementSkillUses() {
    ContactManager.registerPlayerId("targetPlayer", "123");
    KoLCharacter.setMP(1000, 1000, 1000);
    KoLCharacter.addAvailableSkill(EXPERIENCE_SAFARI);
    int startingCasts = SkillDatabase.getCasts(EXPERIENCE_SAFARI);

    // We only want to mock network activity, not the rest of UseSkillRequest behavior.
    UseSkillRequest req = spy(UseSkillRequest.getInstance(EXPERIENCE_SAFARI, "targetPlayer", 1));

    expectSuccess(req, "You don't have enough mana to cast that skill.");

    req.run();

    verify(req).externalExecute();
    assertEquals("Not enough mana to cast Experience Safari.", UseSkillRequest.lastUpdate);
    assertEquals(startingCasts, SkillDatabase.getCasts(EXPERIENCE_SAFARI));

    KoLmafia.forceContinue();
  }

  @Test
  void successIncrementsSkillUses() {
    ContactManager.registerPlayerId("targetPlayer", "123");
    KoLCharacter.setMP(1000, 1000, 1000);
    KoLCharacter.addAvailableSkill(EXPERIENCE_SAFARI);
    int startingCasts = SkillDatabase.getCasts(EXPERIENCE_SAFARI);

    UseSkillRequest req = spy(UseSkillRequest.getInstance(EXPERIENCE_SAFARI, "targetPlayer", 1));
    expectSuccess(
        req,
        "You bless your friend, targetPlayer, with the ability to experience a safari adventure.");

    req.run();

    verify(req).externalExecute();
    assertEquals("", UseSkillRequest.lastUpdate);
    assertEquals(startingCasts + 1, SkillDatabase.getCasts(EXPERIENCE_SAFARI));
  }
}
