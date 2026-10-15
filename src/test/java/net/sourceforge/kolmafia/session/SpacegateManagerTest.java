package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class SpacegateManagerTest {
  @ParameterizedTest
  @CsvSource({
    "AAAAAAA,toxic atmosphere,,none,friendly anomalous,none,,0,none,none,none,0,3",
    "GRDCPRN,high winds,scary noises,friendly simple,none,none,,0,none,drones,space pirates,3,3",
    "LHRSHZO,toxic atmosphere|irradiated,scary noises,none,friendly simple,hostile,primitive alien totem,0,none,drones and soldiers,none,0,2",
    "TNGVKPY,toxic atmosphere|high gravity|irradiated,hot solar flares|lewd rock formations,hostile simple,hostile complex,friendly,primitive alien mask,110000,artifact,none,none,0,2",
  })
  void predictsPlanetTraits(
      String coords,
      String envHazardsStr,
      String elemHazardsStr,
      String plants,
      String animals,
      String aliens,
      String alienItem,
      int alienItemPrice,
      String spants,
      String murderbots,
      String quest,
      int questStage,
      int rocksLevel) {
    SpacegateManager.SpacegatePlanet planet = SpacegateManager.predictPlanet(coords);

    String[] envHazards = envHazardsStr == null ? new String[0] : envHazardsStr.split("\\|");
    assertThat(planet.envHazards().size(), is(envHazards.length));
    assertThat(planet.envHazards(), contains(envHazards));

    String[] elemHazards = elemHazardsStr == null ? new String[0] : elemHazardsStr.split("\\|");
    assertThat(planet.elemHazards().size(), is(elemHazards.length));
    if (elemHazards.length > 0) {
      assertThat(planet.elemHazards(), contains(elemHazards));
    }

    assertThat(planet.plants(), is(plants));
    assertThat(planet.animals(), is(animals));
    assertThat(planet.aliens(), is(aliens));
    if (alienItem == null) {
      assertThat(planet.alienItem(), nullValue());
    } else {
      assertThat(planet.alienItem().getName(), is(alienItem));
    }
    assertThat(planet.alienItemPrice(), is(alienItemPrice));
    assertThat(planet.spants(), is(spants));
    assertThat(planet.murderbots(), is(murderbots));
    assertThat(planet.quest(), is(quest));
    assertThat(planet.questStage(), is(questStage));
    assertThat(planet.rocksLevel(), is(rocksLevel));
  }

  @Test
  void predictsPlanetCosmetics() {
    SpacegateManager.SpacegatePlanet planet = SpacegateManager.predictPlanet("TIMEWAR");

    assertThat(planet.name(), is("Xi North VII"));
    assertThat(planet.description().size(), is(3));
    assertThat(
        planet.description().get(0),
        is("The sky is a hazy blue, dominated by a system of grey rings."));
    assertThat(
        planet.description().get(1), is("A bright yellow, almost Sol-like sun hangs overhead."));
    assertThat(planet.description().get(2), is("Three oblong moons hang in the sky."));
  }
}
