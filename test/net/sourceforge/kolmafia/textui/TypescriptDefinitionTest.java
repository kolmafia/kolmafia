package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.text.CharSequenceLength.hasLength;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TypescriptDefinitionTest {
  private LibraryFunction findFunction(final String signature) {
    var name = signature.substring(0, signature.indexOf("("));
    return Arrays.stream(RuntimeLibrary.functions.findFunctions(name))
        .filter(f -> f.getSignature().equals(signature))
        .map(LibraryFunction.class::cast)
        .findFirst()
        .orElse(null);
  }

  @Test
  void producesAnyOutput() {
    assertThat(TypescriptDefinition.getContents(), hasLength(greaterThan(0)));
  }

  private static Stream<Arguments> provideStringsForFormatFunction() {
    return Stream.of(
        Arguments.of("abort()", "export function abort(): never;"),
        Arguments.of("adv1(location)", "export function adv1(locationValue: Location): boolean;"),
        Arguments.of(
            "adv1(location, int, string)",
            "export function adv1(locationValue: Location, adventuresUsedValue: number, filterFunction: string | ((round: number, monster: Monster, text: string) => string)): boolean;"),
        Arguments.of(
            "run_combat(string)",
            "export function runCombat(filterFunction: string | ((round: number, monster: Monster, text: string) => string)): string;"));
  }

  @ParameterizedTest
  @MethodSource("provideStringsForFormatFunction")
  void canFormatFunctions(final String signature, final String formatted) {
    var fn = findFunction(signature);
    assertThat(TypescriptDefinition.formatFunction(fn), equalTo(formatted));
  }

  @Test
  void showsDeprecationMessage() {
    var fn = findFunction("my_path_id()");
    // This is ash styled and not JS styled, but still better than nothing
    assertThat(
        TypescriptDefinition.formatFunction(fn),
        is(
            "/** @deprecated Changing 'my_path_id()' to 'my_path().id' will remove this warning */\nexport function myPathId(): number;"));
  }

  @Test
  void firstLineContainsValidVersionNumber() {
    // We get the version number with `PACKAGE_VERSION=$(head -n 1 index.d.ts | cut -c 5-)`
    // As such, here we test that this produces a valid version number
    var contents = TypescriptDefinition.getContents();
    var firstLine = contents.substring(0, contents.indexOf("\n"));
    var version = firstLine.substring(4);
    assertThat(version, matchesPattern("^\\d+\\.\\d+\\.\\d+$"));
  }
}
