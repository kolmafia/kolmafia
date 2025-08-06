package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DemonName14ManagerTest {
  @Test
  void canSolveDemonNameWithNoRepetitions() {
    var answer = "MorNixArgPhaDarHutRogBalKru";
    var segments =
        Set.of(
            "Mor", "rNi", "Nix", "xAr", "Arg", "gPh", "Pha", "aDa", "Dar", "arH", "Hut", "utR",
            "Rog", "ogB", "gBa", "alK", "Kru");
    var result = DemonName14Manager.solve(segments);
    assertThat(result, hasSize(1));
    assertThat(result, contains(answer));
  }

  @Test
  void canSolveDemonNameWithRepetitions() {
    var answer = "MorNixArgPhaDarHutRogNixKru";
    var segments =
        Set.of(
            "Mor", "rNi", "Nix", "xAr", "Arg", "gPh", "Pha", "aDa", "Dar", "arH", "Hut", "utR",
            "Rog", "ogN", "gNi", "ixK", "Kru");
    var result = DemonName14Manager.solve(segments);
    assertThat(result, hasSize(1));
    assertThat(result, contains(answer));
  }

  @Test
  void doesntTryToSolveWithFewSegments() {
    var segments = Set.of("Mor", "rNi", "Nix", "xAr", "Arg");
    var result = DemonName14Manager.solve(segments);
    assertThat(result, hasSize(0));
  }

  @Test
  void canSolveFake4CharSyllableExample() {
    var segments =
        Set.of(
            "Hut", "utR", "tRo", "Rog", "ogN", "all", "llN", "lNi", "Nix", "ixA", "Arg", "rgP",
            "gPh", "Pha", "haD", "arH", "aDa", "ixK", "xKr");
    var result = DemonName14Manager.solve(segments);
    assertThat(result, hasSize(2));
    assertThat(result, hasItem("CallNixArgPhaDarHutRogNixKru"));
    assertThat(result, hasItem("BallNixArgPhaDarHutRogNixKru"));
  }

  @Test
  void canSolveDemonNameWithAdjacentRepetitions() {
    var answer = "NixNixArgPhaDarHutRogNixKru";
    var segments =
        Set.of(
            "Nix", "xNi", "ixN", "xAr", "Arg", "gPh", "Pha", "aDa", "Dar", "arH", "Hut", "utR",
            "Rog", "ogN", "gNi", "ixK", "Kru");
    var result = DemonName14Manager.solve(segments);
    assertThat(result, hasItem(answer));
  }

  @ParameterizedTest
  @CsvSource({
    "'Hut,utR,tRo,Rog,ogN,orN,rNi,Nix,ixA,Arg,rgP,gPh,Pha,haD,arH,aDa,ixK,xKr', MorNixArgPhaDarHutRogNixKru", // wRAR
    "'Kir,irK,rKi,Kil,ilL,lLa,Lar,arD,Dar,arL,rLa,Lag,gYe,erG,rGr,Gra,raN,aNu,utL', KirKilLarDarLagYerGraNutLag", // Name Guy Man
  })
  void canSolveRealExample(final String segments, final String expected) {
    var result =
        DemonName14Manager.solve(Arrays.stream(segments.split(",")).collect(Collectors.toSet()));
    assertThat(result, hasItem(expected));
  }
}
