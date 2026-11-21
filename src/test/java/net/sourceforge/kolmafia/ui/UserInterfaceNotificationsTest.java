package net.sourceforge.kolmafia.ui;

import static internal.helpers.Player.withUserInterface;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserInterfaceNotificationsTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("UserInterfaceNotificationsTest");
  }

  private static class RecordingUserInterface implements UserInterface {
    final List<String> notified = new ArrayList<>();

    @Override
    public void meatChanged() {
      this.notified.add("meat");
    }

    @Override
    public void safetyDetailsChanged() {
      this.notified.add("safety");
    }
  }

  @Test
  void reportsMeatChanges() {
    var ui = new RecordingUserInterface();
    KoLCharacter.setAvailableMeat(0);
    KoLCharacter.setStorageMeat(0);

    try (var cleanups = withUserInterface(ui)) {
      KoLCharacter.setAvailableMeat(100);
      KoLCharacter.setAvailableMeat(100);
      KoLCharacter.setStorageMeat(50);

      assertThat(ui.notified, contains("meat", "meat"));
    } finally {
      KoLCharacter.setAvailableMeat(0);
      KoLCharacter.setStorageMeat(0);
    }
  }

  @Test
  void reportsSafetyChangesWhenMindControlChanges() {
    var ui = new RecordingUserInterface();
    KoLCharacter.setMindControlLevel(0);

    try (var cleanups = withUserInterface(ui)) {
      KoLCharacter.setMindControlLevel(0);
      assertThat(ui.notified, empty());

      KoLCharacter.setMindControlLevel(5);
      assertThat(ui.notified, contains("safety"));
    } finally {
      KoLCharacter.setMindControlLevel(0);
    }
  }
}
