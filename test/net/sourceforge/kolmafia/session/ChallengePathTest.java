package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.getPostRequestBody;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ChallengePathTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ChallengePath");
  }

  @Test
  void testCoatOfPaintCheckedAfterLeavingAvatarPath() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPasswordHash("test"),
            withEquipped(ItemPool.COAT_OF_PAINT),
            withProperty(
                "_coatOfPaintModifier",
                "Experience (Muscle): +5, Cold Resistance: +2, Stench Damage: +20, Stench Spell Damage: +20, Familiar Weight: +5"));

    try (cleanups) {
      // Perform a reset as if we're leaving an avatar path
      KoLmafia.resetAfterAvatar();

      // Fetch the requests made
      var requests = client.getRequests();

      // Find a request for coat of paint's description
      var coatOfPaintRequest =
          requests.stream()
              .filter(
                  req ->
                      req.method().equals("POST")
                          && "whichitem=640494952&pwd=test".equals(getPostRequestBody(req)))
              .findAny();

      // Assert that the request was found
      assertTrue(coatOfPaintRequest.isPresent());
    }
  }
}
