package net.sourceforge.kolmafia.textui;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.ScriptData.InvalidScriptData;
import net.sourceforge.kolmafia.textui.ScriptData.ValidScriptData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tries to parse valid and invalid ASH programs. */
public class ParserTest {

  @BeforeEach
  public void setRevision() {
    StaticEntity.overrideRevision(10000);
  }

  @AfterEach
  public void clearRevision() {
    StaticEntity.overrideRevision(null);
  }

  public static Stream<Arguments> data() {
    /**
     * @return A list containing arrays with the following spec: String description String errorText
     *     A substring of the expected error message. List<String> tokens that are expected for a
     *     successful parse.
     *     <p>Note that exactly one of (errorText, tokens) should be non-null.
     */
    return Stream.of(
        invalid("Unterminated Java for-loop", "for (", "Expected ), found end of file"),
        invalid("Java for-loop, bad incrementer", "for (;;1)", "Variable reference expected"),
        valid(
            "valid empty Java for-loop",
            "for (;;) {}",
            Arrays.asList("for", "(", ";", ";", ")", "{", "}"),
            Arrays.asList("1-1", "1-5", "1-6", "1-7", "1-8", "1-10", "1-11")),
        valid(
            "Java for-loop with new variable",
            "for (int i = 0; i < 5; ++i) {}",
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "++", "i", ")", "{",
                "}"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-12", "1-14", "1-15", "1-17", "1-19", "1-21", "1-22",
                "1-24", "1-26", "1-27", "1-29", "1-30")),
        invalid("Multiline string, end of line not escaped", "'\n'", "No closing ' found"),
        valid(
            "Multiline string, end of line properly escaped",
            "'\\\n'",
            Arrays.asList("'\\", "'"),
            Arrays.asList("1-1", "2-1")),
        valid(
            "Multiline string, end of line properly escaped + empty lines",
            "'\\\n\n   \n\n\n'",
            Arrays.asList("'\\", "'"),
            Arrays.asList("1-1", "6-1")),
        invalid(
            "Multiline string, end of line properly escaped + empty lines + comment",
            "'\\\n\n\n//Comment\n\n'",
            "No closing ' found"),
        invalid(
            "Unterminated string template",
            "`{`",
            // The parser tries to start a new string template inside the expression
            "No closing ` found"),
        invalid(
            "Abruptly unterminated string template, expecting expression",
            "`{",
            "Expression expected"),
        invalid(
            "Abruptly unterminated string template, parsed expression",
            "`{1",
            "Expected }, found end of file"),
        invalid(
            // Idem

            "Typed constant, no bracket", "$int", "Expected [, found end of file"),
        valid(
            "Typed constant non-successive characters",
            "$		boolean 	 [ 		false ]",
            Arrays.asList("$", "boolean", "[", "false ", "]"),
            Arrays.asList("1-1", "1-4", "1-14", "1-18", "1-24")),
        valid(
            "Typed constant escaped characters",
            "$boolean[\\f\\a\\l\\s\\e]",
            Arrays.asList("$", "boolean", "[", "\\f\\a\\l\\s\\e", "]"),
            Arrays.asList("1-1", "1-2", "1-9", "1-10", "1-20")),
        invalid("Typed constant bad typecast", "$boolean['']", "Bad boolean value: \"''\""),
        invalid("Typed constant, unknown type", "$foo[]", "Unknown type foo"),
        invalid(
            "Typed constant, non-primitive type",
            "record r {int i;}; $r[]",
            "Non-primitive type r"),
        invalid("Typed constant multiline", "$boolean[\n]", "No closing ] found"),
        invalid("Typed constant, escaped multiline", "$boolean[\\\n]", "No closing ] found"),
        valid(
            "Typed constant, nested brackets, proper",
            "$item[[8042]rock]",
            Arrays.asList("$", "item", "[", "[8042]rock", "]"),
            Arrays.asList("1-1", "1-2", "1-6", "1-7", "1-17")),
        invalid(
            "Typed constant, nested brackets, improper",
            "$item[[abc]]",
            "Bad item value: \"[abc]\""),
        valid(
            "Typed constant, numeric literal",
            "$item[1]",
            Arrays.asList("$", "item", "[", "1", "]"),
            Arrays.asList("1-1", "1-2", "1-6", "1-7", "1-8")),
        valid(
            "Typed constant literal correction",
            "$item[rock]; $effect[buy!]; $monster[eldritch]; $skill[boon]; $location[dire warren]",
            Arrays.asList(
                "$",
                "item",
                "[",
                "rock",
                "]",
                ";",
                "$",
                "effect",
                "[",
                "buy!",
                "]",
                ";",
                "$",
                "monster",
                "[",
                "eldritch",
                "]",
                ";",
                "$",
                "skill",
                "[",
                "boon",
                "]",
                ";",
                "$",
                "location",
                "[",
                "dire warren",
                "]"),
            Arrays.asList(
                "1-1", "1-2", "1-6", "1-7", "1-11", "1-12", "1-14", "1-15", "1-21", "1-22", "1-26",
                "1-27", "1-29", "1-30", "1-37", "1-38", "1-46", "1-47", "1-49", "1-50", "1-55",
                "1-56", "1-60", "1-61", "1-63", "1-64", "1-72", "1-73", "1-84")),
        valid(
            "empty typed constant",
            "$string[]",
            Arrays.asList("$", "string", "[", "]"),
            Arrays.asList("1-1", "1-2", "1-8", "1-9")),
        invalid("Plural constant, abrupt end", "$booleans[", "No closing ] found"),
        invalid("Plural constant, unknown plural type", "$kolmafia[]", "Unknown type kolmafia"),
        invalid("Plural constant, RAM-protection", "$strings[]", "Can't enumerate all strings"),
        valid(
            "Plural constant, non... \"traditional\" plurals",
            "$bounties[]; $classes[]; $phyla[]",
            Arrays.asList(
                "$",
                "bounties",
                "[",
                "]",
                ";",
                "$",
                "classes",
                "[",
                "]",
                ";",
                "$",
                "phyla",
                "[",
                "]"),
            Arrays.asList(
                "1-1", "1-2", "1-10", "1-11", "1-12", "1-14", "1-15", "1-22", "1-23", "1-24",
                "1-26", "1-27", "1-32", "1-33")),
        valid(
            "Plural constant non-successive characters",
            "$		booleans 	 [ 		 ]",
            Arrays.asList("$", "booleans", "[", "]"),
            Arrays.asList("1-1", "1-4", "1-15", "1-20")),
        valid(
            "Plural constant multiline",
            // End-of-lines can appear anywhere
            "$booleans[true\n\n\n,\nfalse, false\n,false,\ntrue,tr\nue]",
            Arrays.asList(
                "$", "booleans", "[", "true", ",", "false, false", ",false,", "true,tr", "ue", "]"),
            Arrays.asList("1-1", "1-2", "1-10", "1-11", "4-1", "5-1", "6-1", "7-1", "8-1", "8-3")),
        invalid(
            "Plural constant w/ escape characters",
            // *Escaped* end-of-lines and escaped "\n"s get added to the value
            "$booleans[tr\\\nu\\ne]",
            "Bad boolean value: \"tr\nu\ne\""),
        valid(
            "Plural constant, nested brackets, proper",
            "$items[[8042]rock]",
            Arrays.asList("$", "items", "[", "[8042]rock", "]"),
            Arrays.asList("1-1", "1-2", "1-7", "1-8", "1-18")),
        invalid(
            "Plural constant, nested brackets, improper",
            "$items[[abc]]",
            "Bad item value: \"[abc]\""),
        invalid(
            "Plural constant, single slash",
            "$booleans[tr/Comment\nue]",
            "Bad boolean value: \"tr/Commentue\""),
        valid(
            "Plural constant, comment",
            "$booleans[tr//Comment\nue]",
            Arrays.asList("$", "booleans", "[", "tr", "//Comment", "ue", "]"),
            Arrays.asList("1-1", "1-2", "1-10", "1-11", "1-13", "2-1", "2-3")),
        invalid(
            "Plural constant, two line-separated slashes",
            "$booleans[tr/\n/ue]",
            "Bad boolean value: \"tr//ue\""),
        invalid(
            "Plural constant, line-separated multiline comment",
            "$booleans[tr/\n**/ue]",
            "Bad boolean value: \"tr/**/ue\""),
        valid(
            "Mid-line // comment",
            "int x = // interrupting comment\n  5;",
            Arrays.asList("int", "x", "=", "// interrupting comment", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "2-3", "2-4")),
        valid(
            "Mid-line # comment",
            // This ought to only accept full-line comments, but it's incorrectly implemented,
            // and at this point, widely used enough that this isn't feasible to change.
            "int x = # interrupting comment\n  5;",
            Arrays.asList("int", "x", "=", "# interrupting comment", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "2-3", "2-4")),
        valid(
            "Multiline comment",
            "int x =/* this\n    is a comment\n   */ 5;",
            // Note that this drops some leading whitespace.
            Arrays.asList("int", "x", "=", "/* this", "is a comment", "*/", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-8", "2-5", "3-4", "3-7", "3-8")),
        valid(
            "Multiline comment on one line",
            "int x =/* this is a comment */ 5;",
            Arrays.asList("int", "x", "=", "/* this is a comment */", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-8", "1-32", "1-33")),
        valid(
            "Simple map literal",
            "int[item] { $item[seal-clubbing club]: 1, $item[helmet turtle]: 2}",
            Arrays.asList(
                "int",
                "[",
                "item",
                "]",
                "{",
                "$",
                "item",
                "[",
                "seal-clubbing club",
                "]",
                ":",
                "1",
                ",",
                "$",
                "item",
                "[",
                "helmet turtle",
                "]",
                ":",
                "2",
                "}"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-9", "1-11", "1-13", "1-14", "1-18", "1-19", "1-37", "1-38",
                "1-40", "1-41", "1-43", "1-44", "1-48", "1-49", "1-62", "1-63", "1-65", "1-66")),
        invalid("Unterminated aggregate literal", "int[int] {", "Expected }, found end of file"),
        invalid("Interrupted map literal", "int[int] {:", "Script parsing error"),
        valid(
            "Simple array literal",
            "int[5] { 1, 2, 3, 4, 5}",
            Arrays.asList(
                "int", "[", "5", "]", "{", "1", ",", "2", ",", "3", ",", "4", ",", "5", "}"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-8", "1-10", "1-11", "1-13", "1-14", "1-16", "1-17",
                "1-19", "1-20", "1-22", "1-23")),
        valid(
            "Array literal with trailing comma",
            "int[2] { 1, 2, }",
            Arrays.asList("int", "[", "2", "]", "{", "1", ",", "2", ",", "}"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-8", "1-10", "1-11", "1-13", "1-14", "1-16")),
        valid(
            "Array literal with variable",
            "int x = 10; int[5] { 1, 2, x, 4, 5}",
            Arrays.asList(
                "int", "x", "=", "10", ";", "int", "[", "5", "]", "{", "1", ",", "2", ",", "x", ",",
                "4", ",", "5", "}"),
            Arrays.asList(
                "1-1", "1-5", "1-7", "1-9", "1-11", "1-13", "1-16", "1-17", "1-18", "1-20", "1-22",
                "1-23", "1-25", "1-26", "1-28", "1-29", "1-31", "1-32", "1-34", "1-35")),
        /*
        invalid(
          // We ought to check for this case too, but we don't...
          "Array literal not enough elements",
          "int[10] { 1, 2, 3, 4, 5}",
          "Array has 10 elements but 5 initializers."),
        */
        invalid(
            "Array literal too many elements",
            "int[1] { 1, 2, 3, 4, 5 }",
            "Array has 1 elements but 5 initializers."),
        valid(
            "Empty multidimensional map literal",
            "int[int, int]{}",
            Arrays.asList("int", "[", "int", ",", "int", "]", "{", "}"),
            Arrays.asList("1-1", "1-4", "1-5", "1-8", "1-10", "1-13", "1-14", "1-15")),
        valid(
            "Non-empty multidimensional map literal",
            "int[int, int, int]{ {}, { 1:{2, 3} } }",
            Arrays.asList(
                "int", "[", "int", ",", "int", ",", "int", "]", "{", "{", "}", ",", "{", "1", ":",
                "{", "2", ",", "3", "}", "}", "}"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-8", "1-10", "1-13", "1-15", "1-18", "1-19", "1-21", "1-22",
                "1-23", "1-25", "1-27", "1-28", "1-29", "1-30", "1-31", "1-33", "1-34", "1-36",
                "1-38")),
        invalid(
            "Interrupted multidimensional map literal",
            "int[int, int] {1:",
            "Script parsing error"),
        invalid("Invalid array literal coercion", "int[int]{ 'foo' }", "Invalid array literal"),
        invalid("Invalid map literal coercion", "int[int]{ 'foo':'bar' }", "Invalid map literal"),
        invalid(
            "Ambiguity between array and map literal",
            "boolean[5]{ 0:true, 1:true, 2:false, true, 4:false }",
            "Expected :, found ,"),
        invalid(
            "Ambiguity between array and map literal 2: index and data are both integers",
            "int[5]{ 0, 1, 2, 3:3, 4 }",
            "Cannot include keys when making an array literal"),
        invalid(
            "Ambiguity between array and map literal 3: that can't be a key",
            "string[5]{ '0', '1', '2', '3':'3', '4' }",
            "Expected , or }, found :"),
        valid(
            // This... exercises a different code path.
            "Parenthesized map literal",
            "(int[int]{})",
            Arrays.asList("(", "int", "[", "int", "]", "{", "}", ")"),
            Arrays.asList("1-1", "1-2", "1-5", "1-6", "1-9", "1-10", "1-11", "1-12")),
        valid(
            // Why is this allowed...? This is the only way I could think of to exercise the
            // rewind functionality.
            "Typedef of existing variable",
            "int foo; typedef int foo; (\nfoo\n\n + 2);",
            Arrays.asList(
                "int", "foo", ";", "typedef", "int", "foo", ";", "(", "foo", "+", "2", ")", ";"),
            Arrays.asList(
                "1-1", "1-5", "1-8", "1-10", "1-18", "1-22", "1-25", "1-27", "2-1", "4-2", "4-4",
                "4-5", "4-6")),
        invalid("interrupted script directive", "script", "Expected <, found end of file"),
        valid(
            "script directive delimited with <>",
            "script <zlib.ash>;",
            Arrays.asList("script", "<zlib.ash>", ";"),
            Arrays.asList("1-1", "1-8", "1-18")),
        valid(
            "script directive delimited with \"\"",
            "script \"zlib.ash\"",
            Arrays.asList("script", "\"zlib.ash\""),
            Arrays.asList("1-1", "1-8")),
        valid(
            "script directive without delimiter",
            "script zlib.ash",
            Arrays.asList("script", "zlib.ash"),
            Arrays.asList("1-1", "1-8")),
        invalid("Unterminated script directive", "script \"zlib.ash", "No closing \" found"),
        valid(
            "Script with bom",
            "\ufeff    'hello world'",
            Arrays.asList("'hello world'"),
            Arrays.asList("1-6")),
        valid(
            "Simple operator assignment",
            "int x = 3;",
            Arrays.asList("int", "x", "=", "3", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "1-10")),
        valid(
            "Compound operator assignment",
            "int x; x += 3;",
            Arrays.asList("int", "x", ";", "x", "+=", "3", ";"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-10", "1-13", "1-14")),
        invalid(
            "Aggregate assignment to primitive",
            // What is this, C?
            "int x; x = {1};",
            "Cannot use an aggregate literal for type int"),
        invalid(
            "Primitive assignment to aggregate",
            "int[4] x = 1;",
            "Cannot store int in x of type int [4]"),
        invalid(
            "Compound assignment to aggregate",
            "int[4] x; x += 1;",
            "Cannot use '+=' on an aggregate"),
        valid(
            "Aggregate assignment to aggregate",
            "int[4] x; x = {1, 2, 3, 4};",
            Arrays.asList(
                "int", "[", "4", "]", "x", ";", "x", "=", "{", "1", ",", "2", ",", "3", ",", "4",
                "}", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-8", "1-9", "1-11", "1-13", "1-15", "1-16", "1-17",
                "1-19", "1-20", "1-22", "1-23", "1-25", "1-26", "1-27")),
        valid(
            "since passes for low revision",
            "since r100;",
            Arrays.asList("since", "r100", ";"),
            Arrays.asList("1-1", "1-7", "1-11")),
        invalid(
            "since fails for high revision",
            "since r2000000000;",
            "requires revision r2000000000 of kolmafia or higher"),
        invalid("Invalid since version", "since 10;", "invalid 'since' format"),
        valid(
            "since passes for low version",
            "since 1.0;",
            Arrays.asList("since", "1.0", ";"),
            Arrays.asList("1-1", "1-7", "1-10")),
        invalid("since fails for high version", "since 2000000000.0;", "final point release"),
        invalid("since fails for not-a-number", "since yesterday;", "invalid 'since' format"),
        valid(
            "Basic function with one argument",
            "void f(int a) {}",
            Arrays.asList("void", "f", "(", "int", "a", ")", "{", "}"),
            Arrays.asList("1-1", "1-6", "1-7", "1-8", "1-12", "1-13", "1-15", "1-16")),
        valid(
            "Basic function with forward reference",
            "void foo(); void bar() { foo(); } void foo() { return; } bar();",
            Arrays.asList(
                "void", "foo", "(", ")", ";", "void", "bar", "(", ")", "{", "foo", "(", ")", ";",
                "}", "void", "foo", "(", ")", "{", "return", ";", "}", "bar", "(", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-11", "1-13", "1-18", "1-21", "1-22", "1-24", "1-26",
                "1-29", "1-30", "1-31", "1-33", "1-35", "1-40", "1-43", "1-44", "1-46", "1-48",
                "1-54", "1-56", "1-58", "1-61", "1-62", "1-63")),
        invalid(
            "Invalid function name",
            "void float() {}",
            "Reserved word 'float' cannot be used as a function name"),
        invalid("Basic function interrupted", "void f(", "Expected ), found end of file"),
        invalid(
            "Basic function parameter interrupted",
            "void f(int",
            "Expected identifier, found end of file"),
        invalid(
            "Basic function duplicate parameter",
            "void f(int a, float a) {}",
            "Parameter a is already defined"),
        invalid(
            "Basic function missing parameter separator",
            "void f(int a float a) {}",
            "Expected ,, found float"), // Uh... doesn't quite stand out does it...
        valid(
            "Basic function with vararg",
            "void f(int a, string b, float... c) {} f(5, 'foo', 1.2, 6.3, 4.9, 10, -0)",
            Arrays.asList(
                "void", "f", "(", "int", "a", ",", "string", "b", ",", "float", "...", "c", ")",
                "{", "}", "f", "(", "5", ",", "'foo'", ",", "1", ".", "2", ",", "6", ".", "3", ",",
                "4", ".", "9", ",", "10", ",", "-", "0", ")"),
            Arrays.asList(
                "1-1", "1-6", "1-7", "1-8", "1-12", "1-13", "1-15", "1-22", "1-23", "1-25", "1-30",
                "1-34", "1-35", "1-37", "1-38", "1-40", "1-41", "1-42", "1-43", "1-45", "1-50",
                "1-52", "1-53", "1-54", "1-55", "1-57", "1-58", "1-59", "1-60", "1-62", "1-63",
                "1-64", "1-65", "1-67", "1-69", "1-71", "1-72", "1-73")),
        invalid(
            "Basic function with multiple varargs",
            "void f(int ... a, int ... b) {}",
            "Only one vararg parameter is allowed"),
        invalid(
            "Basic function with non-terminal vararg",
            "void f(int ... a, float b) {}",
            "The vararg parameter must be the last one"),
        invalid(
            "Basic function overrides library",
            "void round(float n) {}",
            "Function 'round(float)' overrides a library function."),
        invalid(
            "Basic function defined multiple times",
            "void f() {} void f() {}",
            "Function 'f()' defined multiple times."),
        invalid(
            "Basic function vararg clash",
            "void f(int a, int ... b) {} void f(int ... a) {}",
            "Function 'f(int ...)' clashes with existing function 'f(int, int ...)'."),
        valid(
            "Complex expression parsing",
            // Among other things, this tests that true / false are case insensitive, in case
            // you want to pretend you're working in Python, C, or both.
            "(!(~-5 == 10) && True || FALSE);",
            Arrays.asList(
                "(", "!", "(", "~", "-", "5", "==", "10", ")", "&&", "True", "||", "FALSE", ")",
                ";"),
            Arrays.asList(
                "1-1", "1-2", "1-3", "1-4", "1-5", "1-6", "1-8", "1-11", "1-13", "1-15", "1-18",
                "1-23", "1-26", "1-31", "1-32")),
        invalid("Interrupted ! expression", "(!", "Value expected"),
        invalid("Non-boolean ! expression", "(!'abc');", "\"!\" operator requires a boolean value"),
        invalid("Interrupted ~ expression", "(~", "Value expected"),
        invalid(
            "Non-boolean/integer ~ expression",
            "(~'abc');",
            "\"~\" operator requires an integer or boolean value"),
        invalid("Interrupted - expression", "(-", "Value expected"),
        invalid("Interrupted expression after operator", "(1 +", "Value expected"),
        valid(
            "String concatenation with left-side coercion",
            "(1 + 'abc');",
            Arrays.asList("(", "1", "+", "'abc'", ")", ";"),
            Arrays.asList("1-1", "1-2", "1-4", "1-6", "1-11", "1-12")),
        valid(
            "String concatenation with right-side coercion",
            "('abc' + 1);",
            Arrays.asList("(", "'abc'", "+", "1", ")", ";"),
            Arrays.asList("1-1", "1-2", "1-8", "1-10", "1-11", "1-12")),
        invalid(
            "Invalid expression coercion",
            "(true + 1);",
            "Cannot apply operator + to true (boolean) and 1 (int)"),
        valid(
            "Numeric literal split after negative",
            "-/*negative \nnumber*/1.23;",
            Arrays.asList("-", "/*negative", "number*/", "1", ".", "23", ";"),
            Arrays.asList("1-1", "1-2", "2-1", "2-9", "2-10", "2-11", "2-13")),
        valid(
            "Float literal split after decimal",
            "1./*decimal\n*/23;",
            Arrays.asList("1", ".", "/*decimal", "*/", "23", ";"),
            Arrays.asList("1-1", "1-2", "1-3", "2-1", "2-3", "2-5")),
        valid(
            "Float literal with no integral component",
            "-.123;",
            Arrays.asList("-", ".", "123", ";"),
            Arrays.asList("1-1", "1-2", "1-3", "1-6")),
        invalid(
            "Float literal with no integral part, non-numeric fractional part",
            "-.123abc;",
            "Expected numeric value, found 123abc"),
        valid(
            "unary negation",
            "int x; (-x);",
            Arrays.asList("int", "x", ";", "(", "-", "x", ")", ";"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-9", "1-10", "1-11", "1-12")),
        /*
          There's code for this case, but we encounter a separate error ("Record expected").
        valid(
          "Float literal with no decimal component",
          "123.;",
          Arrays.asList( "123", ".", ";" )),
        */
        valid(
            "Int literal with method call.",
            "123.to_string();",
            Arrays.asList("123", ".", "to_string", "(", ")", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-14", "1-15", "1-16")),
        valid(
            "Unary minus",
            "int x; (-x);",
            Arrays.asList("int", "x", ";", "(", "-", "x", ")", ";"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-9", "1-10", "1-11", "1-12")),
        valid(
            "Chained if/else-if/else",
            "if (false) {} else if (false) {} else if (false) {} else {}",
            Arrays.asList(
                "if", "(", "false", ")", "{", "}", "else", "if", "(", "false", ")", "{", "}",
                "else", "if", "(", "false", ")", "{", "}", "else", "{", "}"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-10", "1-12", "1-13", "1-15", "1-20", "1-23", "1-24", "1-29",
                "1-31", "1-32", "1-34", "1-39", "1-42", "1-43", "1-48", "1-50", "1-51", "1-53",
                "1-58", "1-59")),
        invalid("Multiple else", "if (false) {} else {} else {}", "Else without if"),
        invalid("else-if after else", "if (false) {} else {} else if (true) {}", "Else without if"),
        invalid("else without if", "else {}", "Unknown variable 'else'"),
        valid(
            "Multiline cli_execute script",
            "cli_execute {\n  echo hello world;\n  echo sometimes we don't have a semicolon\n}",
            Arrays.asList(
                "cli_execute",
                "{",
                "echo hello world;",
                "echo sometimes we don't have a semicolon",
                "}"),
            Arrays.asList("1-1", "1-13", "2-3", "3-3", "4-1")),
        invalid("Interrupted cli_execute script", "cli_execute {", "Expected }, found end of file"),
        valid(
            "Non-basic-script cli_execute",
            "int cli_execute; cli_execute++",
            Arrays.asList("int", "cli_execute", ";", "cli_execute", "++"),
            Arrays.asList("1-1", "1-5", "1-16", "1-18", "1-29")),
        invalid(
            "For loop, no/bad initial expression",
            "for x from",
            "Expression for initial value expected"),
        invalid(
            "For loop, no/bad ceiling expression",
            "for x from 0 to",
            "Expression for floor/ceiling value expected"),
        invalid(
            "For loop, no/bad increment expression",
            "for x from 0 to 1 by",
            "Expression for increment value expected"),
        valid(
            "basic template string",
            "`this is some math: {4 + 7}`",
            Arrays.asList("`this is some math: {", "4", "+", "7", "}`"),
            Arrays.asList("1-1", "1-22", "1-24", "1-26", "1-27")),
        invalid(
            "template string with a new variable",
            "`this is some math: {int x = 4; x + 7}`",
            "Unknown variable 'int'"),
        valid(
            "template string with predefined variable",
            "int x; `this is some math: {(x = 4) + 7}`",
            Arrays.asList(
                "int", "x", ";", "`this is some math: {", "(", "x", "=", "4", ")", "+", "7", "}`"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-8", "1-29", "1-30", "1-32", "1-34", "1-35", "1-37", "1-39",
                "1-40")),
        invalid(
            "template string with unclosed comment",
            "`this is some math: {7 // what determines the end?}`",
            "Expected }, found end of file"),
        valid(
            "template string with terminated comment",
            "`this is some math: {7 // turns out you need a newline\r\n\t}`",
            Arrays.asList(
                "`this is some math: {", "7",
                "// turns out you need a newline", "}`"),
            Arrays.asList(
                "1-1", "1-22",
                "1-24", "2-2")),
        valid(
            "template string with multiple templates",
            "`{'hello'} {'world'}`",
            Arrays.asList("`{", "'hello'", "} {", "'world'", "}`"),
            Arrays.asList("1-1", "1-3", "1-10", "1-13", "1-20")),
        valid(
            "string with uninteresting escape",
            "'\\z'",
            Arrays.asList("'\\z'"),
            Arrays.asList("1-1")),
        valid(
            "string with unclearly terminated octal escape",
            "'\\1131'",
            // \113 is char code 75, which maps to 'K'
            Arrays.asList("'\\1131'"),
            Arrays.asList("1-1")),
        invalid(
            "string with insufficient octal digits",
            "'\\11'",
            "Octal character escape requires 3 digits"),
        invalid(
            "string with invalid octal digits",
            "'\\118'",
            "Octal character escape requires 3 digits"),
        valid(
            "string with hex digits",
            "'\\x3fgh'",
            Arrays.asList("'\\x3fgh'"),
            Arrays.asList("1-1")),
        invalid(
            "string with invalid hex digits",
            "'\\xhello'",
            "Hexadecimal character escape requires 2 digits"),
        invalid(
            "string with insufficient hex digits",
            "'\\x1'",
            "Hexadecimal character escape requires 2 digits"),
        valid(
            "string with unicode digits",
            "'\\u0041'",
            Arrays.asList("'\\u0041'"),
            Arrays.asList("1-1")),
        invalid(
            "string with invalid unicode digits",
            "'\\uzzzz'",
            "Unicode character escape requires 4 digits"),
        invalid(
            "string with insufficient unicode digits",
            "'\\u1'",
            "Unicode character escape requires 4 digits"),
        invalid("string with escaped eof", "'\\", "No closing ' found"),
        invalid("plural typed constant with escaped eof", "$items[true\\", "No closing ] found"),
        valid(
            "plural typed constant with escaped space",
            "$effects[\n\tBuy!\\ \\ Sell!\\ \\ Buy!\\ \\ Sell!\n]",
            Arrays.asList("$", "effects", "[", "Buy!\\ \\ Sell!\\ \\ Buy!\\ \\ Sell!", "]"),
            Arrays.asList("1-1", "1-2", "1-9", "2-2", "3-1")),
        invalid("unterminated plural aggregate typ", "int[", "Missing index token"),
        // TODO: add tests for size == 0, size < 0, which ought to fail.
        valid(
            "array with explicit size",
            "int[2] x;",
            Arrays.asList("int", "[", "2", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-8", "1-9")),
        valid(
            "array with unspecified size",
            "int[] x;",
            Arrays.asList("int", "[", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-7", "1-8")),
        valid(
            "multidimensional array with partially specified size (1)",
            "int[2][] x;",
            Arrays.asList("int", "[", "2", "]", "[", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-7", "1-8", "1-10", "1-11")),
        valid(
            "multidimensional array with partially specified size (2)",
            "int[][2] x;",
            Arrays.asList("int", "[", "]", "[", "2", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-7", "1-8", "1-10", "1-11")),
        valid(
            "multidimensional array with explicit size",
            "int[2, 2] x;",
            Arrays.asList("int", "[", "2", ",", "2", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-8", "1-9", "1-11", "1-12")),
        // TODO: `typedef int[] intArray` and aggregate shouldn't be valid keys.
        invalid(
            "map with non-primitive key type",
            "record a{int b;}; int[a] x;",
            "Index type 'a' is not a primitive type"),
        invalid(
            "multidimensional map with partially specified comma-separated type",
            "int[2,] x;",
            "Missing index token"),
        invalid(
            "multidimensional map with unspecified comma-separated type",
            "int[,,,] x;",
            "Missing index token"),
        invalid(
            "multidimensional map with partially-specified comma-separated type",
            "int[int,] x;",
            "Missing index token"),
        invalid("abruptly terminated 1-d map", "int[int", "Expected , or ], found end of file"),
        invalid("abruptly terminated 1-d array", "int[4", "Expected , or ], found end of file"),
        invalid("map with unknown type", "int[a] x;", "Invalid type name 'a'"),
        invalid("map containing aggregate type", "int[int[]] x;", "Expected , or ], found ["),
        valid(
            "multidimensional map with comma-separated type",
            "int[int, int] x;",
            Arrays.asList("int", "[", "int", ",", "int", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-8", "1-10", "1-13", "1-15", "1-16")),
        valid(
            "multidimensional map with chained brackets with types",
            "int[int][int] x;",
            Arrays.asList("int", "[", "int", "]", "[", "int", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-8", "1-9", "1-10", "1-13", "1-15", "1-16")),
        valid(
            "multidimensional map with unspecified type in chained empty brackets",
            "int[][] x;",
            Arrays.asList("int", "[", "]", "[", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-7", "1-9", "1-10")),
        valid(
            "single static declaration",
            "static int x;",
            Arrays.asList("static", "int", "x", ";"),
            Arrays.asList("1-1", "1-8", "1-12", "1-13")),
        valid(
            "single static definition",
            "static int x = 1;",
            Arrays.asList("static", "int", "x", "=", "1", ";"),
            Arrays.asList("1-1", "1-8", "1-12", "1-14", "1-16", "1-17")),
        valid(
            "multiple static definition",
            "static int x = 1, y = 2;",
            Arrays.asList("static", "int", "x", "=", "1", ",", "y", "=", "2", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-12", "1-14", "1-16", "1-17", "1-19", "1-21", "1-23", "1-24")),
        invalid(
            "static type but no declaration",
            "static int;",
            "Type given but not used to declare anything"),
        valid(
            "static command",
            "static print('hello world');",
            Arrays.asList("static", "print", "(", "'hello world'", ")", ";"),
            Arrays.asList("1-1", "1-8", "1-13", "1-14", "1-27", "1-28")),
        valid(
            "simple static block",
            "static { int x; }",
            Arrays.asList("static", "{", "int", "x", ";", "}"),
            Arrays.asList("1-1", "1-8", "1-10", "1-14", "1-15", "1-17")),
        invalid("unterminated static declaration", "static int x\nint y;", "Expected ;, found int"),
        invalid("unterminated static block", "static {", "Expected }, found end of file"),
        invalid("lone static", "static;", "command or declaration required"),
        invalid("unterminated typedef", "typedef int foo\n foo bar;", "Expected ;, found foo"),
        invalid("typedef with no type", "typedef;", "Missing data type for typedef"),
        invalid("typedef with no name", "typedef int;", "Type name expected"),
        invalid("typedef with non-identifier name", "typedef int 1;", "Invalid type name '1'"),
        invalid(
            "typedef with reserved name",
            "typedef int remove;",
            "Reserved word 'remove' cannot be a type name"),
        invalid(
            "typedef with existing name",
            "typedef float double; typedef int double;",
            "Type name 'double' is already defined"),
        valid(
            "equivalent typedef redefinition",
            "typedef float double; typedef float double;",
            Arrays.asList("typedef", "float", "double", ";", "typedef", "float", "double", ";"),
            Arrays.asList("1-1", "1-9", "1-15", "1-21", "1-23", "1-31", "1-37", "1-43")),
        invalid(
            "inner main", "void foo() { void main() {} }", "main method must appear at top level"),
        invalid("unterminated top-level declaration", "int x\nint y", "Expected ;, found int"),
        invalid("type but no declaration", "int;", "Type given but not used to declare anything"),
        valid(
            "aggregate-initialized definition",
            "int[1] x { 1 };",
            Arrays.asList("int", "[", "1", "]", "x", "{", "1", "}", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-8", "1-10", "1-12", "1-14", "1-15")),
        invalid("record with no body", "record a;", "Expected {, found ;"),
        invalid("record with no name or body", "record;", "Record name expected"),
        invalid(
            "record with non-identifier name", "record 1 { int a;};", "Invalid record name '1'"),
        invalid(
            "record with reserved name",
            "record int { int a;};",
            "Reserved word 'int' cannot be a record name"),
        invalid(
            "record redefinition",
            "record a { int a;}; record a { int a;};",
            "Record name 'a' is already defined"),
        invalid("record without fields", "record a {};", "Record field(s) expected"),
        invalid(
            "record with void field type", "record a { void a;};", "Non-void field type expected"),
        invalid("record with unknown field type", "record a { foo a;};", "Type name expected"),
        invalid("record with no field name", "record a { int;};", "Field name expected"),
        invalid(
            "record with non-identifier field name",
            "record a { int 1;};",
            "Invalid field name '1'"),
        invalid(
            "record with reserved field name",
            "record a { int class;};",
            "Reserved word 'class' cannot be used as a field name"),
        invalid(
            "record with repeated field name",
            "record a { int b; int b;};",
            "Field name 'b' is already defined"),
        invalid(
            "record with missing semicolon",
            "record a { int b\n float c;};",
            "Expected ;, found float"),
        invalid("unterminated record", "record a { int b;", "Expected }, found end of file"),
        invalid(
            "declaration with non-identifier name",
            "int 1;",
            "Type given but not used to declare anything"),
        invalid(
            "declaration with reserved name",
            "int class;",
            "Reserved word 'class' cannot be a variable name"),
        invalid("multiple-definition", "int x = 1; int x = 2;", "Variable x is already defined"),
        invalid(
            "variable declaration followed by definition",
            // One might expect this to be acceptable.
            "int x; int x = 1;",
            "Variable x is already defined"),
        invalid("parameter initialization", "int f(int x = 1) {}", "Cannot initialize parameter x"),
        invalid(
            "brace assignment of primitive",
            "int x = {1}",
            "Cannot initialize x of type int with an aggregate literal"),
        valid(
            "brace assignment of array",
            "int[] x = {1, 2, 3};",
            Arrays.asList("int", "[", "]", "x", "=", "{", "1", ",", "2", ",", "3", "}", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-7", "1-9", "1-11", "1-12", "1-13", "1-15", "1-16", "1-18",
                "1-19", "1-20")),
        invalid("invalid coercion", "int x = 'hello';", "Cannot store string in x of type int"),
        valid(
            "float->int assignment",
            "int x = 1.0;",
            Arrays.asList("int", "x", "=", "1", ".", "0", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "1-10", "1-11", "1-12")),
        valid(
            "int->float assignment",
            "float y = 1;",
            Arrays.asList("float", "y", "=", "1", ";"),
            Arrays.asList("1-1", "1-7", "1-9", "1-11", "1-12")),
        invalid("assignment missing rhs", "int x =", "Expression expected"),
        invalid("assignment missing rhs 2", "int x; x =", "Expression expected"),
        invalid(
            "Invalid assignment coercion - logical",
            "int x; x &= true",
            "&= requires an integer or boolean expression and an integer or boolean variable reference"),
        invalid(
            "Invalid assignment coercion - integer",
            "boolean x; x >>= 1",
            ">>= requires an integer expression and an integer variable reference"),
        invalid(
            "Invalid assignment coercion - assignment",
            "boolean x; x += 'foo'",
            "Cannot store string in x of type boolean"),
        invalid("break outside loop", "break;", "Encountered 'break' outside of loop"),
        invalid("continue outside loop", "continue;", "Encountered 'continue' outside of loop"),
        valid(
            // Catch in ASH is comparable to a function that catches and
            // returns the text of any errors thrown by its block.
            "catch without try",
            "catch {}",
            Arrays.asList("catch", "{", "}"),
            Arrays.asList("1-1", "1-7", "1-8")),
        invalid(
            "finally without try",
            "finally {}",
            // "Encountered 'finally' without try",
            "Unknown variable 'finally'"),
        invalid(
            "try without catch or finally",
            "try {}",
            // This ought to allow catch, but it's not implemented yet.
            "\"try\" without \"finally\" is pointless"),
        invalid(
            "try-catch",
            "try {} catch {}",
            // This ought to allow catch, but it's not implemented yet.
            "\"try\" without \"finally\" is pointless"),
        valid(
            "try-finally",
            "try {} finally {}",
            Arrays.asList("try", "{", "}", "finally", "{", "}"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-16", "1-17")),
        valid(
            "catch value block",
            "string error_message = catch {}",
            Arrays.asList("string", "error_message", "=", "catch", "{", "}"),
            Arrays.asList("1-1", "1-8", "1-22", "1-24", "1-30", "1-31")),
        valid(
            "catch value expression",
            "string error_message = catch ( print(__FILE__) )",
            Arrays.asList(
                "string", "error_message", "=", "catch", "(", "print", "(", "__FILE__", ")", ")"),
            Arrays.asList(
                "1-1", "1-8", "1-22", "1-24", "1-30", "1-32", "1-37", "1-38", "1-46", "1-48")),
        invalid(
            "catch value interrupted",
            "string error_message = catch",
            "\"catch\" requires a block or an expression"),
        invalid(
            // This has been permitted historically...
            "return outside function",
            "return;",
            // "Return needs null value"
            "Cannot return when outside of a function"
            // Arrays.asList( "return", ";" )
            ),
        valid("top-level exit", "exit;", Arrays.asList("exit", ";"), Arrays.asList("1-1", "1-5")),
        valid("empty block", "{}", Arrays.asList("{", "}"), Arrays.asList("1-1", "1-2")),
        invalid("exit with parameter", "exit 1;", "Expected ;, found 1"),
        valid(
            "break inside while-loop",
            "while (true) break;",
            Arrays.asList("while", "(", "true", ")", "break", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-12", "1-14", "1-19")),
        valid(
            "continue inside while-loop",
            "while (true) continue;",
            Arrays.asList("while", "(", "true", ")", "continue", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-12", "1-14", "1-22")),
        valid(
            "break inside switch",
            "switch (true) { default: break; }",
            Arrays.asList("switch", "(", "true", ")", "{", "default", ":", "break", ";", "}"),
            Arrays.asList(
                "1-1", "1-8", "1-9", "1-13", "1-15", "1-17", "1-24", "1-26", "1-31", "1-33")),
        invalid(
            "continue inside switch",
            "switch (true) { default: continue; }",
            "Encountered 'continue' outside of loop"),
        valid(
            "empty foreach",
            "foreach cls in $classes[];",
            Arrays.asList("foreach", "cls", "in", "$", "classes", "[", "]", ";"),
            Arrays.asList("1-1", "1-9", "1-13", "1-16", "1-17", "1-24", "1-25", "1-26")),
        valid(
            "for-from-to",
            "for i from 1 to 10000;",
            Arrays.asList("for", "i", "from", "1", "to", "10000", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-12", "1-14", "1-17", "1-22")),
        valid(
            "for-from-upto",
            "for i from 1 upto 10000;",
            Arrays.asList("for", "i", "from", "1", "upto", "10000", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-12", "1-14", "1-19", "1-24")),
        valid(
            "for-from-to-by",
            "for i from 1 to 10000 by 100;",
            Arrays.asList("for", "i", "from", "1", "to", "10000", "by", "100", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-12", "1-14", "1-17", "1-23", "1-26", "1-29")),
        valid(
            "for-from-downto",
            // This is valid, but will immediately return.
            "for i from 1 downto 10000;",
            Arrays.asList("for", "i", "from", "1", "downto", "10000", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-12", "1-14", "1-21", "1-26")),
        invalid("no return from int function", "int f() {}", "Missing return value"),
        invalid("return void from int function", "int f() { return; }", "Return needs int value"),
        invalid(
            "return string from int function",
            "int f() { return 'str'; }",
            "Cannot return string value from int function"),
        invalid(
            "return int from void function",
            "void f() { return 1; }",
            "Cannot return a value from a void function"),
        invalid("non-expression return", "int f() { return (); }", "Expression expected"),
        valid(
            "single-command if",
            "if (true) print('msg');",
            Arrays.asList("if", "(", "true", ")", "print", "(", "'msg'", ")", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-9", "1-11", "1-16", "1-17", "1-22", "1-23")),
        valid(
            "empty if",
            "if (true);",
            Arrays.asList("if", "(", "true", ")", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-9", "1-10")),
        invalid("unclosed block scope", "{", "Expected }, found end of file"),
        // if / else-if / else
        invalid("if without condition", "if true", "Expected (, found true"),
        invalid("if with empty condition", "if ()", "\"if\" requires a boolean condition"),
        invalid("if with numeric condition", "if (1)", "\"if\" requires a boolean condition"),
        invalid("if with unclosed condition", "if (true", "Expected ), found end of file"),
        // These probably shouldn't need to be separate test cases...
        invalid("else if without condition", "if (false); else if true", "Expected (, found true"),
        invalid(
            "else if with empty condition",
            "if (false); else if ()",
            "\"if\" requires a boolean condition"),
        invalid(
            "else if with unclosed condition",
            "if (false); else if (true",
            "Expected ), found end of file"),
        // while
        invalid("while without condition", "while true", "Expected (, found true"),
        invalid("while with empty condition", "while ()", "\"while\" requires a boolean condition"),
        invalid("while with unclosed condition", "while (true", "Expected ), found end of file"),
        invalid("while with unclosed loop", "while (true) {", "Expected }, found end of file"),
        invalid(
            "while with multiple statements but no semicolon",
            "while (true) print(5)\nprint(6)",
            "Expected ;, found print"),
        // repeat
        valid(
            "repeat statement",
            "repeat print('hello'); until(true);",
            Arrays.asList(
                "repeat", "print", "(", "'hello'", ")", ";", "until", "(", "true", ")", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-13", "1-14", "1-21", "1-22", "1-24", "1-29", "1-30", "1-34",
                "1-35")),
        invalid("repeat without until", "repeat {}", "Expected until, found end of file"),
        invalid("repeat without condition", "repeat {} until true", "Expected (, found true"),
        invalid(
            "repeat with empty condition",
            "repeat {} until ('done')",
            // This should probably read as "'until' requires a
            // boolean condition"...
            "\"repeat\" requires a boolean condition"),
        // So many cases of identical tests for duplicate code...
        invalid(
            "repeat with unclosed condition",
            "repeat {} until (true",
            "Expected ), found end of file"),
        // switch
        invalid(
            "switch without condition or block", "switch true {}", "Expected ( or {, found true"),
        invalid("switch with empty condition", "switch ()", "\"switch ()\" requires an expression"),
        invalid("switch with unclosed condition", "switch (true", "Expected ), found end of file"),
        invalid(
            "switch with condition but no block", "switch (true)", "Expected {, found end of file"),
        invalid(
            "switch with condition but unclosed block",
            "switch (true) {",
            "Expected }, found end of file"),
        valid(
            "switch with block and no condition",
            "switch { }",
            Arrays.asList("switch", "{", "}"),
            Arrays.asList("1-1", "1-8", "1-10")),
        valid(
            "switch with block, non-const label",
            "boolean x; switch { case x: }",
            Arrays.asList("boolean", "x", ";", "switch", "{", "case", "x", ":", "}"),
            Arrays.asList("1-1", "1-9", "1-10", "1-12", "1-19", "1-21", "1-26", "1-27", "1-29")),
        valid(
            "switch with block, label expression",
            "boolean x; switch { case !x: }",
            Arrays.asList("boolean", "x", ";", "switch", "{", "case", "!", "x", ":", "}"),
            Arrays.asList(
                "1-1", "1-9", "1-10", "1-12", "1-19", "1-21", "1-26", "1-27", "1-28", "1-30")),
        valid(
            "switch with block, nested variable",
            "switch { case true: int x; }",
            Arrays.asList("switch", "{", "case", "true", ":", "int", "x", ";", "}"),
            Arrays.asList("1-1", "1-8", "1-10", "1-15", "1-19", "1-21", "1-25", "1-26", "1-28")),
        invalid(
            "switch with block, nested type but no variable",
            "switch { case true: int; }",
            "Type given but not used to declare anything"),
        invalid(
            "switch with block, nested variable but missing semicolon",
            "switch { case true: int x }",
            "Expected ;, found }"),
        invalid(
            "switch, case label without expression",
            "switch { case: }",
            "Case label needs to be followed by an expression"),
        invalid(
            "switch case not terminated by colon", "switch { case true; }", "Expected :, found ;"),
        invalid(
            "switch default not terminated by colon",
            "switch { default true; }",
            "Expected :, found true"),
        invalid(
            "switch type mismatch",
            "switch (1) { case true: }",
            "Switch conditional has type int but label expression has type boolean"),
        invalid(
            "switch block type mismatch",
            // Note that the implicit switch type is boolean here.
            "switch { case 1: }",
            "Switch conditional has type boolean but label expression has type int"),
        invalid(
            "duplicate switch label", "switch (1) { case 0: case 0: }", "Duplicate case label: 0"),
        invalid(
            "switch, multiple default labels",
            "switch (1) { default: default: }",
            "Only one default label allowed in a switch statement"),
        invalid(
            "switch block, multiple default labels",
            "switch { default: default: }",
            "Only one default label allowed in a switch statement"),
        valid(
            "variable definition of sort",
            "int sort = 0;",
            Arrays.asList("int", "sort", "=", "0", ";"),
            Arrays.asList("1-1", "1-5", "1-10", "1-12", "1-13")),
        valid(
            "variable declaration of sort",
            "int sort;",
            Arrays.asList("int", "sort", ";"),
            Arrays.asList("1-1", "1-5", "1-9")),
        valid(
            "function named sort",
            "void sort(){} sort();",
            Arrays.asList("void", "sort", "(", ")", "{", "}", "sort", "(", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-10", "1-11", "1-12", "1-13", "1-15", "1-19", "1-20", "1-21")),
        invalid(
            "sort not-a-variable primitive", "sort 2 by value;", "Aggregate reference expected"),
        invalid("sort without by", "int[] x {3,2,1}; sort x;", "Expected by, found ;"),
        invalid("Sort, no sorting expression", "int[] x; sort x by", "Expression expected"),
        valid(
            "valid sort",
            "int[] x; sort x by value*3;",
            Arrays.asList("int", "[", "]", "x", ";", "sort", "x", "by", "value", "*", "3", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-7", "1-8", "1-10", "1-15", "1-17", "1-20", "1-25", "1-26",
                "1-27")),
        invalid(
            "foreach with non-identifier key",
            "foreach 'key' in $items[];",
            "Key variable name expected"),
        invalid(
            "foreach with reserved key",
            "foreach item in $items[];",
            "Reserved word 'item' cannot be a key variable"),
        invalid("foreach missing `in`", "foreach it;", "Expected in, found ;"),
        invalid(
            "foreach missing key variable name", "foreach in it;", "Key variable name expected"),
        invalid(
            "foreach key variable named 'in'",
            "foreach in in it;",
            "Reserved word 'in' cannot be a key variable name"),
        invalid(
            "foreach key variable named 'in' 2",
            "foreach in, on, under, below, through in it;",
            "Reserved word 'in' cannot be a key variable name"),
        invalid(
            "foreach in not-a-reference",
            "foreach it in $item[none];",
            "Aggregate reference expected"),
        invalid(
            "foreach with duplicate key",
            "foreach it, it in $items[];",
            "Key variable 'it' is already defined"),
        valid(
            "foreach with multiple keys",
            "foreach key, value in int[int]{} {}",
            Arrays.asList(
                "foreach", "key", ",", "value", "in", "int", "[", "int", "]", "{", "}", "{", "}"),
            Arrays.asList(
                "1-1", "1-9", "1-12", "1-14", "1-20", "1-23", "1-26", "1-27", "1-30", "1-31",
                "1-32", "1-34", "1-35")),
        invalid(
            "foreach with too many keys",
            "foreach a, b, c in $items[];",
            "Too many key variables specified"),
        invalid(
            "for with reserved index",
            "for int from 1 upto 10;",
            "Reserved word 'int' cannot be an index variable"),
        invalid(
            "for with existing index",
            // Oddly, this is unsupported, when other for loops will create
            // a nested scope.
            "int i; for i from 1 upto 10;",
            "Index variable 'i' is already defined"),
        invalid("for without from", "for i in range(10):\n  print(i)", "Expected from, found in"),
        invalid(
            "for with invalid dest keyword",
            "for i from 1 until 10;",
            "Expected to, upto, or downto, found until"),
        valid(
            "javaFor with multiple declarations",
            "for (int i=0, int length=5; i < length; i++);",
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ",", "int", "length", "=", "5", ";", "i", "<",
                "length", ";", "i", "++", ")", ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-11", "1-12", "1-13", "1-15", "1-19", "1-25", "1-26",
                "1-27", "1-29", "1-31", "1-33", "1-39", "1-41", "1-42", "1-44", "1-45")),
        invalid(
            "javaFor with empty initializer", "for (int i=0,; i < 5; ++i);", "Identifier expected"),
        valid(
            "javaFor with compound increment",
            "for (int i=0; i < 5; i+=1);",
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "i", "+=", "1", ")",
                ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-11", "1-12", "1-13", "1-15", "1-17", "1-19", "1-20",
                "1-22", "1-23", "1-25", "1-26", "1-27")),
        valid(
            "javaFor with existing variable",
            "int i; for (i=0; i < 5; i++);",
            Arrays.asList(
                "int", "i", ";", "for", "(", "i", "=", "0", ";", "i", "<", "5", ";", "i", "++", ")",
                ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-8", "1-12", "1-13", "1-14", "1-15", "1-16", "1-18", "1-20",
                "1-22", "1-23", "1-25", "1-26", "1-28", "1-29")),
        invalid(
            "javaFor with unknown existing variable",
            "for (i=0; i < 5; i++);",
            "Unknown variable 'i'"),
        invalid(
            "javaFor with redefined existing variable",
            "int i; for (int i=0; i < 5; i++);",
            "Variable 'i' already defined"),
        invalid(
            "javaFor with not-an-increment",
            "for (int i=0; i < 5; i==1);",
            "Variable 'i' not incremented"),
        valid(
            "javaFor with constant assignment",
            // I guess this technically works... but will be an infinite
            // loop in practice.
            "for (int i=0; i < 5; i=1);",
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "i", "=", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-11", "1-12", "1-13", "1-15", "1-17", "1-19", "1-20",
                "1-22", "1-23", "1-24", "1-25", "1-26")),
        invalid(
            "javaFor missing initial identifier", "for (0; i < 5; i++);", "Identifier required"),
        invalid(
            "javaFor missing initializer expression",
            "for (int i =; i < 5; i++);",
            "Expression expected"),
        invalid(
            "javaFor invalid assignment",
            "for (int i ='abc'; i < 5; i++);",
            "Cannot store string in i of type int"),
        invalid(
            "javaFor non-boolean condition",
            "for (int i; i + 5; i++);",
            "\"for\" requires a boolean conditional expression"),
        valid(
            "javaFor multiple increments",
            "for (int i, int j; i + j < 5; i++, j++);",
            Arrays.asList(
                "for", "(", "int", "i", ",", "int", "j", ";", "i", "+", "j", "<", "5", ";", "i",
                "++", ",", "j", "++", ")", ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-11", "1-13", "1-17", "1-18", "1-20", "1-22", "1-24",
                "1-26", "1-28", "1-29", "1-31", "1-32", "1-34", "1-36", "1-37", "1-39", "1-40")),
        invalid(
            "javaFor interrupted multiple increments",
            "for (int i; i < 5; i++,);",
            "Identifier expected"),
        invalid(
            "undefined function call",
            "prin();",
            "Function 'prin( )' undefined.  This script may require a more recent version of KoLmafia and/or its supporting scripts."),
        invalid("function call interrupted", "print(", "Expected ), found end of file"),
        invalid(
            "function call interrupted after comma",
            "print(1,",
            "Expected parameter, found end of file"),
        invalid("function call closed after comma", "print(1,);", "Expected parameter, found )"),
        invalid(
            "function call interrupted after param", "print(1", "Expected ), found end of file"),
        invalid("function call with non-comma separator", "print(1; 2);", "Expected ), found ;"),
        valid(
            "function parameter coercion to ANY_TYPE",
            "dump('foo', 'bar');",
            Arrays.asList("dump", "(", "'foo'", ",", "'bar'", ")", ";"),
            Arrays.asList("1-1", "1-5", "1-6", "1-11", "1-13", "1-18", "1-19")),
        valid(
            "function parameter no typedef coercion",
            "typedef int foo; foo a = 1; void bar(int x, foo y) {} bar(a, 1);",
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "void", "bar", "(", "int",
                "x", ",", "foo", "y", ")", "{", "}", "bar", "(", "a", ",", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-9", "1-13", "1-16", "1-18", "1-22", "1-24", "1-26", "1-27", "1-29",
                "1-34", "1-37", "1-38", "1-42", "1-43", "1-45", "1-49", "1-50", "1-52", "1-53",
                "1-55", "1-58", "1-59", "1-60", "1-62", "1-63", "1-64")),
        valid(
            "function parameter typedef-to-simple typedef coercion",
            // Mmh... there's no real way to "prove" the function was used other than
            // seeing it checked from clover...
            "typedef int foo; foo a = 1; int to_int(foo x) {return 1;} void bar(int x) {} bar(a);",
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "int", "to_int", "(",
                "foo", "x", ")", "{", "return", "1", ";", "}", "void", "bar", "(", "int", "x", ")",
                "{", "}", "bar", "(", "a", ")", ";"),
            Arrays.asList(
                "1-1", "1-9", "1-13", "1-16", "1-18", "1-22", "1-24", "1-26", "1-27", "1-29",
                "1-33", "1-39", "1-40", "1-44", "1-45", "1-47", "1-48", "1-55", "1-56", "1-57",
                "1-59", "1-64", "1-67", "1-68", "1-72", "1-73", "1-75", "1-76", "1-78", "1-81",
                "1-82", "1-83", "1-84")),
        valid(
            "function parameter simple-to-typedef typedef coercion",
            "typedef int foo; foo a = 1; foo to_foo(int x) {return a;} void bar(foo x) {} bar(1);",
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "foo", "to_foo", "(",
                "int", "x", ")", "{", "return", "a", ";", "}", "void", "bar", "(", "foo", "x", ")",
                "{", "}", "bar", "(", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-9", "1-13", "1-16", "1-18", "1-22", "1-24", "1-26", "1-27", "1-29",
                "1-33", "1-39", "1-40", "1-44", "1-45", "1-47", "1-48", "1-55", "1-56", "1-57",
                "1-59", "1-64", "1-67", "1-68", "1-72", "1-73", "1-75", "1-76", "1-78", "1-81",
                "1-82", "1-83", "1-84")),
        valid(
            "record function match",
            "record rec {int i;}; void foo(rec x) {} foo(new rec());",
            Arrays.asList(
                "record", "rec", "{", "int", "i", ";", "}", ";", "void", "foo", "(", "rec", "x",
                ")", "{", "}", "foo", "(", "new", "rec", "(", ")", ")", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-12", "1-13", "1-17", "1-18", "1-19", "1-20", "1-22", "1-27",
                "1-30", "1-31", "1-35", "1-36", "1-38", "1-39", "1-41", "1-44", "1-45", "1-49",
                "1-52", "1-53", "1-54", "1-55")),
        valid(
            "coerced function match",
            "void foo(float x) {} foo(1);",
            Arrays.asList(
                "void", "foo", "(", "float", "x", ")", "{", "}", "foo", "(", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-16", "1-17", "1-19", "1-20", "1-22", "1-25", "1-26",
                "1-27", "1-28")),
        valid(
            "vararg function match",
            "void foo(int... x) {} foo(1, 2, 3);",
            Arrays.asList(
                "void", "foo", "(", "int", "...", "x", ")", "{", "}", "foo", "(", "1", ",", "2",
                ",", "3", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-13", "1-17", "1-18", "1-20", "1-21", "1-23", "1-26",
                "1-27", "1-28", "1-30", "1-31", "1-33", "1-34", "1-35")),
        valid(
            "coerced vararg function match",
            "void foo(float... x) {} foo(1, 2, 3);",
            Arrays.asList(
                "void", "foo", "(", "float", "...", "x", ")", "{", "}", "foo", "(", "1", ",", "2",
                ",", "3", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-15", "1-19", "1-20", "1-22", "1-23", "1-25", "1-28",
                "1-29", "1-30", "1-32", "1-33", "1-35", "1-36", "1-37")),
        invalid(
            "function invocation interrupted",
            "call",
            "Variable reference expected for function name"),
        invalid(
            "function invocation non-string expression",
            "call (2)()",
            "String expression expected for function name"),
        invalid(
            "function invocation interrupted after name expression",
            "call ('foo')",
            "Expected (, found end of file"),
        valid(
            "function invocation with non-void function",
            // ummm this should insist that the variable is a string...
            "int x; call string x('foo')",
            Arrays.asList("int", "x", ";", "call", "string", "x", "(", "'foo'", ")"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-13", "1-20", "1-21", "1-22", "1-27")),
        invalid(
            "preincrement with non-numeric variable",
            "string x; ++x;",
            "++X requires a numeric variable reference"),
        invalid("preincrement requires a variable", "++1;", "Variable reference expected"),
        valid(
            "predecrement with float variable",
            "float x; --x;",
            Arrays.asList("float", "x", ";", "--", "x", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-10", "1-12", "1-13")),
        invalid(
            "postincrement with non-numeric variable",
            "string x; x++;",
            "X++ requires a numeric variable reference"),
        /* Currently fails with "Expected ;, found ++" which is asymmetric.
        invalid(
          "postincrement requires a variable",
          "1++;",
          "Variable reference expected"),
        */
        valid(
            "postdecrement with float variable",
            "float x; x--;",
            Arrays.asList("float", "x", ";", "x", "--", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-10", "1-11", "1-13")),
        invalid(
            "ternary with non-boolean condition",
            "int x = 1 ? 1 : 2;",
            "Non-boolean expression 1 (int)"),
        invalid("ternary without lhs", "int x = true ? : 2;", "Value expected in left hand side"),
        invalid("ternary without colon", "int x = true ? 1;", "Expected :, found ;"),
        invalid(
            "ternary without rhs",
            "int x = true ? 1:;",
            // Another asymmetry: not "Value expected in right hand side"
            "Value expected"),
        invalid(
            "non-coercible value mismatch",
            "(true ? 1 : $item[none];",
            "Cannot choose between 1 (int) and none (item)"),
        // parseValue
        invalid("unclosed parenthetical expression", "(true", "Expected ), found end of file"),
        invalid("aggregate literal without braces", "(int[])", "Expected {, found )"),
        valid(
            "indexed variable reference",
            "int[5] x; x[0];",
            Arrays.asList("int", "[", "5", "]", "x", ";", "x", "[", "0", "]", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-8", "1-9", "1-11", "1-12", "1-13", "1-14", "1-15")),
        invalid("indexed primitive", "int x; x[0];", "Variable 'x' cannot be indexed"),
        invalid("over-indexed variable reference", "int[5] x; x[0,1];", "Too many keys for 'x'"),
        invalid("empty indexed variable reference", "int[5] x; x[];", "Index for 'x' expected"),
        invalid(
            "unterminated aggregate variable reference",
            "int[5] x; x[0",
            "Expected ], found end of file"),
        invalid(
            "type-mismatched indexed variable reference",
            "int[5] x; x['str'];",
            "Index for 'x' has wrong data type (expected int, got string)"),
        invalid(
            "type-mismatched indexed composite reference",
            "int[5, 5] x; x[0]['str'];",
            "Index for 'x[]' has wrong data type (expected int, got string)"),
        valid(
            "multidimensional comma-separated array index",
            "int[5,5] x; x[0,1];",
            Arrays.asList(
                "int", "[", "5", ",", "5", "]", "x", ";", "x", "[", "0", ",", "1", "]", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-7", "1-8", "1-10", "1-11", "1-13", "1-14", "1-15",
                "1-16", "1-17", "1-18", "1-19")),
        valid(
            "multidimensional bracket-separated array index",
            "int[5,5] x; x[0][1];",
            Arrays.asList(
                "int", "[", "5", ",", "5", "]", "x", ";", "x", "[", "0", "]", "[", "1", "]", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-7", "1-8", "1-10", "1-11", "1-13", "1-14", "1-15",
                "1-16", "1-17", "1-18", "1-19", "1-20")),
        valid(
            "method call of primitive var",
            "string x = 'hello'; x.print();",
            Arrays.asList("string", "x", "=", "'hello'", ";", "x", ".", "print", "(", ")", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-10", "1-12", "1-19", "1-21", "1-22", "1-23", "1-28", "1-29",
                "1-30")),
        valid(
            "method call of aggregate index",
            "string[2] x; x[0].print();",
            Arrays.asList(
                "string", "[", "2", "]", "x", ";", "x", "[", "0", "]", ".", "print", "(", ")", ";"),
            Arrays.asList(
                "1-1", "1-7", "1-8", "1-9", "1-11", "1-12", "1-14", "1-15", "1-16", "1-17", "1-18",
                "1-19", "1-24", "1-25", "1-26")),
        invalid("non-record property reference", "int i; i.a;", "Record expected"),
        valid(
            "record field reference",
            "record {int a;} r; r.a;",
            Arrays.asList("record", "{", "int", "a", ";", "}", "r", ";", "r", ".", "a", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-9", "1-13", "1-14", "1-15", "1-17", "1-18", "1-20", "1-21", "1-22",
                "1-23")),
        invalid(
            "record field reference without field", "record {int a;} r; r.", "Field name expected"),
        invalid(
            "record unknown field reference", "record {int a;} r; r.b;", "Invalid field name 'b'"),
        invalid(
            "Illegal record creation",
            "void f( record foo {int a; int b;} bar )",
            "Existing type expected for function parameter"),
        valid(
            "array of record",
            "record {int a;}[] r;",
            Arrays.asList("record", "{", "int", "a", ";", "}", "[", "]", "r", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-9", "1-13", "1-14", "1-15", "1-16", "1-17", "1-19", "1-20")),
        invalid("standalone new", "new;", "Expected Record name, found ;"),
        invalid("new non-record", "int x = new int();", "'int' is not a record type"),
        valid(
            "new record without parens",
            // Yields a default-constructed record.
            "record r {int a;}; new r;",
            Arrays.asList("record", "r", "{", "int", "a", ";", "}", ";", "new", "r", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-10", "1-11", "1-15", "1-16", "1-17", "1-18", "1-20", "1-24",
                "1-25")),
        invalid(
            "new record with semicolon",
            "record r {int a;}; new r(;",
            "Expression expected for field #1 (a)"),
        valid(
            "new with aggregate field",
            "record r {int[] a;}; new r({1,2});",
            Arrays.asList(
                "record", "r", "{", "int", "[", "]", "a", ";", "}", ";", "new", "r", "(", "{", "1",
                ",", "2", "}", ")", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-10", "1-11", "1-14", "1-15", "1-17", "1-18", "1-19", "1-20",
                "1-22", "1-26", "1-27", "1-28", "1-29", "1-30", "1-31", "1-32", "1-33", "1-34")),
        invalid(
            "new with field type mismatch",
            "record r {int a;}; new r('str');",
            "string found when int expected for field #1 (a)"),
        invalid(
            "new with too many void fields",
            "record r {int a;}; new r(,,,,,,,);",
            "Too many field initializers for record r"),
        invalid(
            "new without closing paren",
            "record r {int a;}; new r(",
            "Expected ), found end of file"),
        invalid(
            "new without closing paren 2",
            "record r {int a;}; new r(4",
            "Expected , or ), found end of file"),
        invalid(
            "new without closing comma separator",
            "record r {int a; int b;}; new r(4 5)",
            "Expected , or ), found 5"),
        invalid("improper remove", "int i; remove i;", "Aggregate reference expected"),
        valid(
            "proper remove",
            "int[] map; remove map[0];",
            Arrays.asList("int", "[", "]", "map", ";", "remove", "map", "[", "0", "]", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-7", "1-10", "1-12", "1-19", "1-22", "1-23", "1-24",
                "1-25")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    if (script instanceof InvalidScriptData) {
      testInvalidScript((InvalidScriptData) script);
      return;
    }

    testValidScript((ValidScriptData) script);
  }

  private static void testInvalidScript(final InvalidScriptData script) {
    ScriptException e = assertThrows(ScriptException.class, script.parser::parse, script.desc);
    assertThat(script.desc, e.getMessage(), containsString(script.errorText));
  }

  private static void testValidScript(final ValidScriptData script) {
    // This will fail if an exception is thrown.
    script.parser.parse();
    assertEquals(script.tokens, getTokensContents(script.parser), script.desc);
    assertEquals(script.positions, getTokensPositions(script.parser), script.desc);
  }

  private static List<String> getTokensContents(final Parser parser) {
    return parser.getTokens().stream().map(token -> token.content).collect(Collectors.toList());
  }

  private static List<String> getTokensPositions(final Parser parser) {
    return parser.getTokens().stream()
        .map(token -> token.getStart().getLine() + 1 + "-" + (token.getStart().getCharacter() + 1))
        .collect(Collectors.toList());
  }
}
