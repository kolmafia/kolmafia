package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withSign;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FloristRequestTest {
  @BeforeEach
  protected void beforeEach() {
    KoLCharacter.reset("FloristRequestTest");
    Preferences.reset("FloristRequestTest");
  }

  private static Cleanups woodsOpen() {
    return withQuestProgress(QuestDatabase.Quest.LARVA, QuestDatabase.STARTED);
  }

  @Test
  public void unowned() {
    try (var cleanups = new Cleanups(woodsOpen(), withProperty("ownsFloristFriar", false))) {
      assertThat(FloristRequest.haveFlorist(), is(false));
    }
  }

  @Test
  public void unavailableWithoutWoods() {
    try (var cleanups = new Cleanups(withProperty("ownsFloristFriar", true))) {
      assertThat(FloristRequest.haveFlorist(), is(false));
    }
  }

  @Test
  public void available() {
    try (var cleanups = new Cleanups(woodsOpen(), withProperty("ownsFloristFriar", true))) {
      assertThat(FloristRequest.haveFlorist(), is(true));
    }
  }

  @Test
  public void unavailableIfRestricted() {
    try (var cleanups =
        new Cleanups(
            woodsOpen(),
            withProperty("ownsFloristFriar", true),
            withPath(Path.STANDARD),
            withRestricted(true),
            withNotAllowedInStandard(RestrictedItemType.BOOKSHELF_BOOKS, "Florist Friar"))) {
      assertThat(FloristRequest.haveFlorist(), is(false));
    }
  }

  @Test
  public void availableIfLoLReplicaEvenIfRestricted() {
    try (var cleanups =
        new Cleanups(
            woodsOpen(),
            withPath(Path.LEGACY_OF_LOATHING),
            withProperty("ownsReplicaFloristFriar", true),
            withRestricted(true),
            withNotAllowedInStandard(
                RestrictedItemType.ITEMS, "Order of the Green Thumb Order Form"))) {
      assertThat(FloristRequest.haveFlorist(), is(true));
    }
  }

  @Test
  public void unavailableIfNoLoLReplica() {
    try (var cleanups =
        new Cleanups(
            woodsOpen(),
            withPath(Path.LEGACY_OF_LOATHING),
            withProperty("ownsReplicaFloristFriar", false))) {
      assertThat(FloristRequest.haveFlorist(), is(false));
    }
  }

  @Test
  public void unavailableInBadMoon() {
    try (var cleanups =
        new Cleanups(
            woodsOpen(),
            withProperty("ownsFloristFriar", true),
            withPath(Path.BAD_MOON),
            withSign(ZodiacSign.BAD_MOON))) {
      assertThat(FloristRequest.haveFlorist(), is(false));
    }
  }

  @Test
  public void unavailableInExploathing() {
    try (var cleanups =
        new Cleanups(
            woodsOpen(),
            withProperty("ownsFloristFriar", true),
            withPath(Path.KINGDOM_OF_EXPLOATHING))) {
      assertThat(FloristRequest.haveFlorist(), is(false));
    }
  }

  @Test
  public void setsBackwardsCompatibilityPreferences() {
    try (var cleanups = new Cleanups(woodsOpen(), withProperty("ownsFloristFriar", true))) {
      assertThat(FloristRequest.haveFlorist(), is(true));
      assertThat("floristFriarChecked", isSetTo(true));
      assertThat("floristFriarAvailable", isSetTo(true));
    }
  }
}
