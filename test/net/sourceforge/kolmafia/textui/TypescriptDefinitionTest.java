package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.CharSequenceLength.hasLength;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TypescriptDefinitionTest {
  @Test
  void producesAnyOutput() {
    assertThat(TypescriptDefinition.getContents(), hasLength(greaterThan(0)));
  }

  private static Stream<Arguments> provideStringsForFormatFunction() {
    return Stream.of(
        Arguments.of("abort", "()", "export function abort(): never;"),
        Arguments.of(
            "adv1", "(location)", "export function adv1(locationValue: Location): boolean;"),
        Arguments.of(
            "adv1",
            "(location, int, string)",
            "export function adv1(locationValue: Location, adventuresUsedValue: number, filterFunction: string | ((round: number, monster: Monster, text: string) => string)): boolean;"),
        Arguments.of(
            "run_combat",
            "(string)",
            "export function runCombat(filterFunction: string | ((round: number, monster: Monster, text: string) => string)): string;"));
  }

  @ParameterizedTest
  @MethodSource("provideStringsForFormatFunction")
  void canFormatFunctions(String name, String signature, String formatted) {
    var fn =
        Arrays.stream(RuntimeLibrary.functions.findFunctions(name))
            .filter(f -> f.getSignature().equals(name + signature))
            .findFirst();

    assertThat(fn.isPresent(), is(true));
    LibraryFunction libFn = (LibraryFunction) fn.get();
    assertThat(TypescriptDefinition.formatFunction(libFn), equalTo(formatted));
  }
}
