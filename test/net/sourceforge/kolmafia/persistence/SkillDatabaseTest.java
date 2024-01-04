package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.SkillDatabase.Category;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SkillDatabaseTest {
  // This is just one simple test to verify before and after behavior for an implicit narrowing cast
  // that was replaced by an alternative calculation.

  @Test
  public void itShouldCalculateCostCorrectlyAsAFunctionOfCasts() {
    assertEquals(SkillDatabase.stackLumpsCost(-10), 1);
    assertEquals(SkillDatabase.stackLumpsCost(0), 11);
    assertEquals(SkillDatabase.stackLumpsCost(1), 111);
    assertEquals(SkillDatabase.stackLumpsCost(2), 1111);
    assertEquals(SkillDatabase.stackLumpsCost(14), 1111111111111111L);
    // The previous calculation using Pow actually gives incorrect results for more than 14 casts
    // More than 17 casts overflows a long
    assertEquals(SkillDatabase.stackLumpsCost(15), 11111111111111111L);
    assertEquals(SkillDatabase.stackLumpsCost(17), 1111111111111111111L);
    assertEquals(SkillDatabase.stackLumpsCost(18), Long.MAX_VALUE);
  }

  @Test
  public void thrallsLastTenTurnsWhenNotPasta() {
    var cleanups = withClass(AscensionClass.ACCORDION_THIEF);
    try (cleanups) {
      assertEquals(SkillDatabase.getEffectDuration(SkillPool.BIND_LASAGMBIE), 10);
    }
  }

  @Test
  public void thrallsLastZeroTurnsWhenPasta() {
    var cleanups = withClass(AscensionClass.PASTAMANCER);
    try (cleanups) {
      assertEquals(SkillDatabase.getEffectDuration(SkillPool.BIND_LASAGMBIE), 0);
    }
  }

  @Nested
  class Categories {
    @Test
    public void identifiesAccordionThiefSkill() {
      assertEquals(SkillDatabase.getSkillCategory(SkillPool.ANTIPHON), Category.ACCORDION_THIEF);
    }

    @Test
    public void identifiesVampyreSkill() {
      assertEquals(SkillDatabase.getSkillCategory(SkillPool.BLOOD_CLOAK), Category.VAMPYRE);
    }

    @Test
    public void identifiesGreyYouSkill() {
      assertEquals(SkillDatabase.getSkillCategory(SkillPool.HARRIED), Category.GREY_YOU);
    }

    @Test
    public void identifiesPigSkinnerSkill() {
      assertEquals(SkillDatabase.getSkillCategory(SkillPool.HOT_FOOT), Category.PIG_SKINNER);
    }

    @Test
    public void identifiesCheeseWizardSkill() {
      assertEquals(SkillDatabase.getSkillCategory(SkillPool.FONDELUGE), Category.CHEESE_WIZARD);
    }

    @Test
    public void identifiesJazzAgentSkill() {
      assertEquals(SkillDatabase.getSkillCategory(SkillPool.DRUM_ROLL), Category.JAZZ_AGENT);
    }

    @Test
    public void identifiesConditionalSkill() {
      assertEquals(SkillDatabase.getSkillCategory(SkillPool.CREEPY_GRIN), Category.CONDITIONAL);
    }

    @Test
    public void identifiesGnomeSkill() {
      assertEquals(SkillDatabase.getSkillCategory(SkillPool.TORSO), Category.GNOME_SKILLS);
    }

    @Test
    public void identifiesBadMoonSkill() {
      assertEquals(SkillDatabase.getSkillCategory(SkillPool.LUST), Category.BAD_MOON);
    }

    @Test
    public void identifiesSkillPastEdgeAsUnknown() {
      var maxSkillOpt =
          SkillDatabase.getAllSkills().stream().mapToInt(UseSkillRequest::getSkillId).max();
      assertTrue(maxSkillOpt.isPresent());
      var maxSkill = maxSkillOpt.getAsInt();
      assertEquals(SkillDatabase.getSkillCategory(maxSkill + 1000), Category.UNKNOWN);
    }
  }

  @Nested
  class Permability {
    @Test
    void assumesSkillsUnder7000ArePermable() {
      assertThat(SkillDatabase.isPermable(10), is(true));
    }

    @Test
    void someSkillsUnder7000AreBlacklisted() {
      assertThat(SkillDatabase.isPermable(156), is(false));
    }

    @Test
    void assumesSkillsOver7000AreNotPermable() {
      assertThat(SkillDatabase.isPermable(7001), is(false));
    }

    @Test
    void someSkillsOver7000AreWhitelisted() {
      assertThat(SkillDatabase.isPermable(7254), is(true));
    }
  }

  @Nested
  class SkillTypes {
    @Test
    void unknown() {
      assertThat(SkillDatabase.getSkillTypeName(-1), equalTo("unknown"));
    }

    @Test
    void passive() {
      assertThat(SkillDatabase.getSkillTypeName(SkillPool.LUST), equalTo("passive"));
    }

    @Test
    void summon() {
      assertThat(SkillDatabase.getSkillTypeName(SkillPool.PASTAMASTERY), equalTo("summon"));
    }

    @Test
    void remedy() {
      assertThat(SkillDatabase.getSkillTypeName(SkillPool.COCOON), equalTo("remedy"));
    }

    @Test
    void self() {
      assertThat(SkillDatabase.getSkillTypeName(SkillPool.DISCO_FEVER), equalTo("self-only"));
    }

    @Test
    void buff() {
      assertThat(SkillDatabase.getSkillTypeName(SkillPool.EMPATHY_OF_THE_NEWT), equalTo("buff"));
    }

    @Test
    void combat() {
      assertThat(SkillDatabase.getSkillTypeName(SkillPool.TANGO_OF_TERROR), equalTo("combat"));
    }

    @Test
    void song() {
      assertThat(SkillDatabase.getSkillTypeName(SkillPool.SONG_OF_SAUCE), equalTo("song"));
    }

    @Test
    void comNoncomRemedy() {
      assertThat(
          SkillDatabase.getSkillTypeName(SkillPool.DEVOUR_MINIONS),
          equalTo("combat/noncombat remedy"));
    }

    @Test
    void comPassive() {
      assertThat(
          SkillDatabase.getSkillTypeName(SkillPool.RAVENOUS_POUNCE), equalTo("combat/passive"));
    }

    @Test
    void expression() {
      assertThat(SkillDatabase.getSkillTypeName(SkillPool.INSCRUTABLE_GAZE), equalTo("expression"));
    }

    @Test
    void walk() {
      assertThat(SkillDatabase.getSkillTypeName(SkillPool.WALK_LEISURELY_AMBLE), equalTo("walk"));
    }

    @Test
    void remedyPassive() {
      assertThat(
          SkillDatabase.getSkillTypeName(SkillPool.DISCO_NAP), equalTo("noncombat remedy/passive"));
    }
  }
}
