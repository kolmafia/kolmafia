package net.sourceforge.kolmafia.webui;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StationaryButtonDecoratorTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("StationaryButtonDecoratorTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("StationaryButtonDecoratorTest");
  }

  @Nested
  class ActionName {
    @Test
    void dartSkillsArePrettified() {
      var cleanups =
          new Cleanups(withProperty("_currentDartboard", ""), withProperty("_dartsLeft", 0));
      try (cleanups) {
        String responseText = html("request/test_fight_dartboard.html");

        FightRequest.parseDartboard(responseText);
        assertEquals(
            "7513:torso,7514:butt,7515:horn,7516:tail,7517:leg,7518:arm",
            Preferences.getString("_currentDartboard"));

        assertEquals("Darts: Throw at %part1", SkillDatabase.getSkillName(SkillPool.DART_PART1));
        assertEquals(
            "Darts: Throw at torso", SkillDatabase.getPrettySkillName(SkillPool.DART_PART1));
        assertEquals("darts: throw at torso", StationaryButtonDecorator.getActionName("7513"));

        assertEquals("Darts: Throw at %part2", SkillDatabase.getSkillName(SkillPool.DART_PART2));
        assertEquals(
            "Darts: Throw at butt", SkillDatabase.getPrettySkillName(SkillPool.DART_PART2));
        assertEquals("darts: throw at butt", StationaryButtonDecorator.getActionName("7514"));

        assertEquals("Darts: Throw at %part3", SkillDatabase.getSkillName(SkillPool.DART_PART3));
        assertEquals(
            "Darts: Throw at horn", SkillDatabase.getPrettySkillName(SkillPool.DART_PART3));
        assertEquals("darts: throw at horn", StationaryButtonDecorator.getActionName("7515"));

        assertEquals("Darts: Throw at %part4", SkillDatabase.getSkillName(SkillPool.DART_PART4));
        assertEquals(
            "Darts: Throw at tail", SkillDatabase.getPrettySkillName(SkillPool.DART_PART4));
        assertEquals("darts: throw at tail", StationaryButtonDecorator.getActionName("7516"));

        assertEquals("Darts: Throw at %part5", SkillDatabase.getSkillName(SkillPool.DART_PART5));
        assertEquals("Darts: Throw at leg", SkillDatabase.getPrettySkillName(SkillPool.DART_PART5));
        assertEquals("darts: throw at leg", StationaryButtonDecorator.getActionName("7517"));

        assertEquals("Darts: Throw at %part6", SkillDatabase.getSkillName(SkillPool.DART_PART6));
        assertEquals("Darts: Throw at arm", SkillDatabase.getPrettySkillName(SkillPool.DART_PART6));
        assertEquals("darts: throw at arm", StationaryButtonDecorator.getActionName("7518"));

        assertEquals("Darts: Throw at %part7", SkillDatabase.getSkillName(SkillPool.DART_PART7));
        assertEquals(
            "Darts: Throw at %part7", SkillDatabase.getPrettySkillName(SkillPool.DART_PART7));

        assertEquals("Darts: Throw at %part8", SkillDatabase.getSkillName(SkillPool.DART_PART8));
        assertEquals(
            "Darts: Throw at %part8", SkillDatabase.getPrettySkillName(SkillPool.DART_PART8));
      }
    }

    @Test
    void zootomistSkillsArePrettified() {
      var cleanups =
          new Cleanups(
              withProperty("zootGraftedHandLeftFamiliar", 171),
              withProperty("zootGraftedHandRightFamiliar", 303),
              withProperty("zootGraftedFootLeftFamiliar", 307),
              withProperty("zootGraftedFootRightFamiliar", 311));
      try (cleanups) {
        assertEquals("Left %n Punch", SkillDatabase.getSkillName(SkillPool.LEFT_PUNCH));
        assertEquals(
            "Left Gelatinous Cubeling Punch",
            SkillDatabase.getPrettySkillName(SkillPool.LEFT_PUNCH));
        assertEquals(
            "left gelatinous cubeling punch", StationaryButtonDecorator.getActionName("7557"));

        assertEquals("Right %n Punch", SkillDatabase.getSkillName(SkillPool.RIGHT_PUNCH));
        assertEquals(
            "Right Burly Bodyguard Punch", SkillDatabase.getPrettySkillName(SkillPool.RIGHT_PUNCH));
        assertEquals(
            "right burly bodyguard punch", StationaryButtonDecorator.getActionName("7558"));

        assertEquals("Left %n Kick", SkillDatabase.getSkillName(SkillPool.LEFT_KICK));
        assertEquals(
            "Left Quantum Entangler Kick", SkillDatabase.getPrettySkillName(SkillPool.LEFT_KICK));
        assertEquals(
            "left quantum entangler kick", StationaryButtonDecorator.getActionName("7559"));

        assertEquals("Right %n Kick", SkillDatabase.getSkillName(SkillPool.RIGHT_KICK));
        assertEquals(
            "Right Significant Bit Kick", SkillDatabase.getPrettySkillName(SkillPool.RIGHT_KICK));
        assertEquals("right significant bit kick", StationaryButtonDecorator.getActionName("7560"));
      }
    }
  }
}
