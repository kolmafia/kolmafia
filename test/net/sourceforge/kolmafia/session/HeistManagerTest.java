package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HeistManagerTest {

  HeistManager manager;

  private static final String TESTUSERNAME = "HeistManagerTestUser";

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset(TESTUSERNAME);
    manager = new FakeHeistManager();
  }

  @Test
  void parsesHeistPage() {
    var heistable = manager.getHeistTargets();

    assertThat(heistable.heists, equalTo(42));
    assertThat(
        heistable.heistables.keySet().stream().map(x -> x.name).collect(Collectors.toList()),
        contains("bigface", "jock", "burnout"));
  }

  @Test
  void doesNotHeistInvalidItem() {
    assertFalse(manager.heist(ItemDatabase.getItemId("Brimstone Bludgeon")));
  }

  @Test
  void heistsValidItem() {
    assertTrue(manager.heist(ItemDatabase.getItemId("ratty knitted cap")));
  }

  @Test
  void heistsManyValidItem() {
    assertTrue(manager.heist(12, ItemDatabase.getItemId("ratty knitted cap")));
  }

  static class FakeHeistManager extends HeistManager {
    @Override
    protected String heistRequest() {
      return html("request/test_heist_command.html");
    }
  }
}
