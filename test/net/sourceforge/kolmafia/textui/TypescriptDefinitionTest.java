package net.sourceforge.kolmafia.textui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.text.CharSequenceLength.hasLength;

import java.util.Arrays;
import java.util.List;
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

  private List<LibraryFunction> findFunctionOverloads(final String signaturePrefix) {
    var name = signaturePrefix.substring(0, signaturePrefix.indexOf("("));

    return Arrays.stream(RuntimeLibrary.functions.findFunctions(name))
        .filter(f -> f.getSignature().startsWith(signaturePrefix))
        .filter(LibraryFunction.class::isInstance)
        .map(LibraryFunction.class::cast)
        .toList();
  }

  @Test
  void producesAnyOutput() {
    assertThat(TypescriptDefinition.getTypeDefContents(), hasLength(greaterThan(0)));
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
            "export function runCombat(filterFunction: string | ((round: number, monster: Monster, text: string) => string)): string;"),
        Arguments.of(
            "fact_type(class, path, monster)",
            "export function factType(cls: Class, path: Path, monster: Monster): \"none\" | \"effect\" | \"item\" | \"stats\" | \"hp\" | \"mp\" | \"meat\" | \"modifier\";"));
  }

  @ParameterizedTest
  @MethodSource("provideStringsForFormatFunction")
  void canFormatFunctions(final String signature, final String formatted) {
    var fn = findFunction(signature);
    assertThat(TypescriptDefinition.formatFunction(fn), equalTo(formatted));
  }

  private static Stream<Arguments> provideStringsForMergeFunction() {
    return Stream.of(
        Arguments.of(
            "adv1(location",
            List.of(
                "export function adv1(locationValue: Location, adventuresUsedValue?: number): boolean;",
                "export function adv1(locationValue: Location, adventuresUsedValue: number, filterFunction?: string | ((round: number, monster: Monster, text: string) => string)): boolean;")),
        Arguments.of(
            "buy(item",
            List.of(
                "export function buy(item: Item, quantity?: number): boolean;",
                "export function buy(item: Item, quantity: number, price: number): number;")));
  }

  @ParameterizedTest
  @MethodSource("provideStringsForMergeFunction")
  void mergesOverloadsToOptionalParameters(final String name, final List<String> lines) {
    var fns = findFunctionOverloads(name);
    assertThat(TypescriptDefinition.formatFunction(fns), equalTo(lines));
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
    var contents = TypescriptDefinition.getTypeDefContents();
    var firstLine = contents.substring(0, contents.indexOf("\n"));
    var version = firstLine.substring(4);
    assertThat(version, matchesPattern("^\\d+\\.\\d+\\.\\d+$"));
  }

  @Test
  void containsEnvironmentUnion() {
    assertThat(TypescriptDefinition.getEnvironmentUnion(), startsWith("\"indoor\""));
  }

  @Test
  void headerFile() {
    var contents = TypescriptDefinition.getHeaderFileContents();
    assertThat(contents, containsString("module.exports.visitUrl = "));
  }
}
