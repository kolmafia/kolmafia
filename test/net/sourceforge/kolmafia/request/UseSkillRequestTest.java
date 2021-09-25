package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.session.ContactManager;
import org.junit.jupiter.api.Test;

class UseSkillRequestTest {

  private static int EXPERIENCE_SAFARI = SkillDatabase.getSkillId("Experience Safari");

  @Test
  void errorDoesNotIncrementSkillUses() {
    ContactManager.registerPlayerId("targetPlayer", "123");
    KoLCharacter.setMP(1000, 1000, 1000);
    KoLCharacter.addAvailableSkill(EXPERIENCE_SAFARI);
    GenericRequest.sessionId = "fake session id";
    int startingCasts = SkillDatabase.getCasts(EXPERIENCE_SAFARI);

    // We only want to mock network activity, not the rest of UseSkillRequest behavior.
    UseSkillRequest req = spy(UseSkillRequest.getInstance(EXPERIENCE_SAFARI, "targetPlayer", 1));
    doAnswer(
            invocation -> {
              GenericRequest m = (GenericRequest) invocation.getMock();
              m.responseCode = 200;
              m.responseText = "You don't have enough mana to cast that skill.";
              // This is normally done by retrieveServerReply(), which is called by
              // externalExecute().
              m.processResponse();
              return null;
            })
        .when(req)
        .externalExecute();

    req.run();

    verify(req).externalExecute();
    assertEquals("Not enough mana to cast Experience Safari.", UseSkillRequest.lastUpdate);
    assertEquals(startingCasts, SkillDatabase.getCasts(EXPERIENCE_SAFARI));
  }

  @Test
  void successIncrementsSkillUses() {
    ContactManager.registerPlayerId("targetPlayer", "123");
    KoLCharacter.setMP(1000, 1000, 1000);
    KoLCharacter.addAvailableSkill(EXPERIENCE_SAFARI);
    GenericRequest.sessionId = "fake session id";
    int startingCasts = SkillDatabase.getCasts(EXPERIENCE_SAFARI);

    UseSkillRequest req = spy(UseSkillRequest.getInstance(EXPERIENCE_SAFARI, "targetPlayer", 1));
    doAnswer(
            invocation -> {
              GenericRequest m = (GenericRequest) invocation.getMock();
              m.responseCode = 200;
              m.responseText =
                  "You bless your friend, targetPlayer, with the ability to experience a"
                      + " safari adventure.";
              m.processResponse();
              return null;
            })
        .when(req)
        .externalExecute();

    req.run();

    verify(req).externalExecute();
    assertEquals("", UseSkillRequest.lastUpdate);
    assertEquals(startingCasts + 1, SkillDatabase.getCasts(EXPERIENCE_SAFARI));
  }
}
