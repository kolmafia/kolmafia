package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MonkeyPawCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("testUser");
    Preferences.reset("testUser");
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
    ChoiceManager.handlingChoice = false;
  }

  public MonkeyPawCommandTest() {
    this.command = "monkeypaw";
  }

  @Test
  void providesUsageIfNoParameters() {
    var cleanups = withItem(ItemPool.CURSED_MONKEY_PAW);

    try (cleanups) {
      String output = execute("");
      assertThat(
          output,
          containsString("Usage: monkeypaw effect [effectname] | item [itemname] | wish [wish]"));
    }
  }

  @Test
  void mustHavePaw() {
    String output = execute("wish asdf");

    assertErrorState();
    assertThat(output, containsString("You do not have a cursed monkey paw"));
  }

  @Test
  void mustHaveWishes() {
    var cleanups =
        new Cleanups(withItem(ItemPool.CURSED_MONKEY_PAW), withProperty("_monkeyPawWishesUsed", 5));

    try (cleanups) {
      String output = execute("wish asdf");

      assertErrorState();
      assertThat(output, containsString("You have been cursed enough today"));
    }
  }

  @Test
  void wishIsPassedThrough() {
    var cleanups = withItem(ItemPool.CURSED_MONKEY_PAW);

    try (cleanups) {
      execute("wish asdf");

      assertContinueState();
      assertWish("asdf");
    }
  }

  @Test
  void itemErrorsIfNoMatch() {
    var cleanups = withItem(ItemPool.CURSED_MONKEY_PAW);

    try (cleanups) {
      String output = execute("item asdf");

      assertErrorState();
      assertThat(output, containsString("asdf does not match exactly one item"));
    }
  }

  @Test
  void itemIsPassedThrough() {
    var cleanups = withItem(ItemPool.CURSED_MONKEY_PAW);

    try (cleanups) {
      execute("item spices");

      assertContinueState();
      assertWish("spices");
    }
  }

  @Test
  void itemIsPassedThroughAsValidSubstring() {
    var cleanups = withItem(ItemPool.CURSED_MONKEY_PAW);

    try (cleanups) {
      execute("item El Vibrato Punchcard (165 holes)");

      assertContinueState();
      assertWish("165 holes");
    }
  }

  @Test
  void effectErrorsIfNoMatch() {
    var cleanups = withItem(ItemPool.CURSED_MONKEY_PAW);

    try (cleanups) {
      String output = execute("effect asdf");

      assertErrorState();
      assertThat(output, containsString("asdf does not match exactly one effect"));
    }
  }

  @Test
  void effectIsPassedThrough() {
    var cleanups = withItem(ItemPool.CURSED_MONKEY_PAW);

    try (cleanups) {
      execute("effect Wings");

      assertContinueState();
      assertWish("wings");
    }
  }

  @Test
  void effectIsPassedThroughAsValidSubstring() {
    var cleanups = withItem(ItemPool.CURSED_MONKEY_PAW);

    try (cleanups) {
      execute("effect Let's Go Shopping");

      assertContinueState();
      assertWish("s go shopping");
    }
  }

  @Test
  void effectIsDeniedIfCannotFindUniqueValidSubstring() {
    var cleanups = withItem(ItemPool.CURSED_MONKEY_PAW);

    try (cleanups) {
      String output = execute("effect meat.enh");

      assertErrorState();
      assertThat(output, containsString("cannot find unique valid substring to wish for meat.enh"));
    }
  }

  @Test
  public void dashesAreFineInItemAndEffectNames() {
    var cleanups = withItem(ItemPool.CURSED_MONKEY_PAW);

    try (cleanups) {
      execute("item sonar-in-a-biscuit");

      assertContinueState();
      assertWish("sonar-in-a-biscuit");
    }
  }

  private void assertWish(String encodedWish) {
    var requests = getRequests();
    int i = 0;
    assertGetRequest(requests.get(i++), "/main.php", "action=cmonk");
    assertPostRequest(
        requests.get(i), "/choice.php", "whichchoice=1501&wish=" + encodedWish + "&option=1");
  }
}
