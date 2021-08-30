package net.sourceforge.kolmafia.textui;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tries to parse valid and invalid ASH programs.
 */
@RunWith( Parameterized.class )
public class ParserTest
{
	// Short description of the test case.
	public final String desc;

	// ASH Script contents.
	public final String script;

	public final List<String> tokens;

	// Error text. If null, then we expect no error to be thrown (i.e. the provided script is
	// well-formed).
	public final String errorText;

	@Parameters(name = "{0}")
	public static Collection<Object[]> data()
	{
		/**
		 * @return A list containing arrays with the following spec:
		 *   String description
		 *   String errorText A substring of the expected error message.
		 *   List<String> tokens that are expected for a successful parse.
		 *
		 * Note that exactly one of (errorText, tokens) should be non-null.
		 */
		return Arrays.asList( new Object[][] {
			{
				"Unterminated Java for-loop",
				"for (",
				"Expected ), found end of file",
				null,
			},
			{
				"Java for-loop, bad incrementer",
				"for (;;1)",
				"Variable reference expected",
				null,
			},
			{
				"valid empty Java for-loop",
				"for (;;) {}",
				null,
				Arrays.asList( "for", "(", ";", ";", ")", "{", "}" ),
			},
			{
				"Java for-loop with new variable",
				"for (int i = 0; i < 5; ++i) {}",
				null,
				Arrays.asList( "for", "(", "int", "i", "=", "0", ";",
				               "i", "<", "5", ";",
				               "++", "i", ")",
				               "{", "}" ),
			},
			{
				"Multiline string, end of line not escaped",
				"'\n'",
				"No closing ' found",
				null,
			},
			{
				"Multiline string, end of line properly escaped",
				"'\\\n'",
				null,
				Arrays.asList( "'\\", "'" ),
			},
			{
				"Multiline string, end of line properly escaped + empty lines",
				"'\\\n\n   \n\n\n'",
				null,
				Arrays.asList( "'\\", "'" ),
			},
			{
				"Multiline string, end of line properly escaped + empty lines + comment",
				"'\\\n\n\n//Comment\n\n'",
				"No closing ' found",
				null,
			},
			{
				"Unterminated string template",
				"`{`",
				// The parser tries to start a new string template inside the expression
				"No closing ` found",
				null,
			},
			{
				"Abruptly unterminated string template, expecting expression",
				"`{",
				"Expression expected",
				null,
			},
			{
				"Abruptly unterminated string template, parsed expression",
				"`{1",
				"Expected }, found end of file",
				null,
			},
			{
				//Idem

				"Typed constant, no bracket",
				"$int",
				"Expected [, found end of file",
				null,
			},
			{
				"Typed constant non-successive characters",
				"$		boolean 	 [ 		false ]",
				null,
				Arrays.asList( "$", "boolean", "[", "false ", "]" ),
			},
			{
				"Typed constant escaped characters",
				"$boolean[\\f\\a\\l\\s\\e]",
				null,
				Arrays.asList( "$", "boolean", "[", "\\f\\a\\l\\s\\e", "]" ),
			},
			{
				"Typed constant bad typecast",
				"$boolean['']",
				"Bad boolean value: \"''\"",
				null,
			},
			{
				"Typed constant, unknown type",
				"$foo[]",
				"Unknown type foo",
				null,
			},
			{
				"Typed constant, non-primitive type",
				"record r {int i;}; $r[]",
				"Non-primitive type r",
				null,
			},
			{
				"Typed constant multiline",
				"$boolean[\n]",
				"No closing ] found",
				null,
			},
			{
				"Typed constant, escaped multiline",
				"$boolean[\\\n]",
				"No closing ] found",
				null,
			},
			{
				"Typed constant, nested brackets, proper",
				"$item[[8042]rock]",
				null,
				Arrays.asList( "$", "item", "[", "[8042]rock", "]" ),
			},
			{
				"Typed constant, nested brackets, improper",
				"$item[[abc]]",
				"Bad item value: \"[abc]\"",
				null,
			},
			{
				"empty typed constant",
				"$string[]",
				null,
				Arrays.asList( "$", "string", "[",  "]" )
			},
			{
				"Plural constant, abrupt end",
				"$booleans[",
				"No closing ] found",
				null,
			},
			{
				"Plural constant, unknown plural type",
				"$kolmafia[]",
				"Unknown type kolmafia",
				null,
			},
			{
				"Plural constant, RAM-protection",
				"$strings[]",
				"Can't enumerate all strings",
				null,
			},
			{
				"Plural constant, non... \"traditional\" plurals",
				"$bounties[]; $classes[]; $phyla[]",
				null,
				Arrays.asList( "$", "bounties", "[", "]", ";", "$", "classes", "[", "]", ";", "$", "phyla", "[", "]" ),
			},
			{
				"Plural constant non-successive characters",
				"$		booleans 	 [ 		 ]",
				null,
				Arrays.asList( "$", "booleans", "[", "]" ),
			},
			{
				"Plural constant multiline",
				// End-of-lines can appear anywhere
				"$booleans[true\n\n\n,\nfalse, false\n,false,\ntrue,tr\nue]",
				null,
				Arrays.asList( "$", "booleans", "[", "true", ",", "false, false", ",false,", "true,tr", "ue", "]" ),
			},
			{
				"Plural constant w/ escape characters",
				// *Escaped* end-of-lines and escaped "\n"s get added to the value
				"$booleans[tr\\\nu\\ne]",
				"Bad boolean value: \"tr\nu\ne\"",
				null,
			},
			{
				"Plural constant, nested brackets, proper",
				"$items[[8042]rock]",
				null,
				Arrays.asList( "$", "items", "[", "[8042]rock", "]" ),
			},
			{
				"Plural constant, nested brackets, improper",
				"$items[[abc]]",
				"Bad item value: \"[abc]\"",
				null,
			},
			{
				"Plural constant, single slash",
				"$booleans[tr/Comment\nue]",
				"Bad boolean value: \"tr/Commentue\"",
				null,
			},
			{
				"Plural constant, comment",
				"$booleans[tr//Comment\nue]",
				null,
				Arrays.asList( "$", "booleans", "[", "tr", "//Comment", "ue", "]" ),
			},
			{
				"Plural constant, two line-separated slashes",
				"$booleans[tr/\n/ue]",
				"Bad boolean value: \"tr//ue\"",
				null,
			},
			{
				"Plural constant, line-separated multiline comment",
				"$booleans[tr/\n**/ue]",
				"Bad boolean value: \"tr/**/ue\"",
				null,
			},
			{
				"Mid-line // comment",
				"int x = // interrupting comment\n  5;",
				null,
				Arrays.asList( "int", "x", "=", "// interrupting comment", "5", ";" ),
			},
			{
				"Mid-line # comment",
				// This ought to only accept full-line comments, but it's incorrectly implemented,
				// and at this point, widely used enough that this isn't feasible to change.
				"int x = # interrupting comment\n  5;",
				null,
				Arrays.asList( "int", "x", "=", "# interrupting comment", "5", ";" ),
			},
			{
				"Multiline comment",
				"int x =/* this\n    is a comment\n   */ 5;",
				null,
				// Note that this drops some leading whitespace.
				Arrays.asList( "int", "x", "=", "/* this", "is a comment", "*/", "5", ";" ),
			},
			{
				"Multiline comment on one line",
				"int x =/* this is a comment */ 5;",
				null,
				Arrays.asList( "int", "x", "=", "/* this is a comment */", "5", ";" ),
			},
			{
				"Simple map literal",
				"int[item] { $item[seal-clubbing club]: 1, $item[helmet turtle]: 2}",
				null,
				Arrays.asList( "int", "[", "item", "]", "{",
				               "$", "item", "[", "seal-clubbing club", "]", ":", "1", ",",
				               "$", "item", "[", "helmet turtle", "]", ":", "2", "}" ),
			},
			{
				"Simple array literal",
				"int[5] { 1, 2, 3, 4, 5}",
				null,
				Arrays.asList( "int", "[", "5", "]", "{",
				               "1", ",", "2", ",", "3", ",", "4",",", "5",
				               "}"),
			},
			{
				"Array literal with trailing comma",
				"int[2] { 1, 2, }",
				null,
				Arrays.asList( "int", "[", "2", "]", "{",
				               "1", ",", "2", ",",
				               "}"),
			},
			{
				"Array literal with variable",
				"int x = 10; int[5] { 1, 2, x, 4, 5}",
				null,
				Arrays.asList( "int", "x", "=", "10", ";",
				               "int", "[", "5", "]", "{",
				               "1", ",", "2", ",", "x", ",", "4",",", "5",
				               "}"),
			},
			/*
			{
				// We ought to check for this case too, but we don't...
				"Array literal not enough elements",
				"int[10] { 1, 2, 3, 4, 5}",
				"Array has 10 elements but 5 initializers.",
				null,
			},
			*/
			{
				"Array literal too many elements",
				"int[1] { 1, 2, 3, 4, 5 }",
				"Array has 1 elements but 5 initializers.",
				null,
			},
			{
				"Empty multidimensional map literal",
				"int[int, int]{}",
				null,
				Arrays.asList( "int", "[", "int", ",", "int", "]", "{", "}" ),
			},
			{
				// This... exercises a different code path.
				"Parenthesized map literal",
				"(int[int]{})",
				null,
				Arrays.asList( "(", "int", "[", "int", "]", "{", "}", ")" ),
			},
			{
				// Why is this allowed...? This is the only way I could think of to exercise the
				// rewind functionality.
				"Typedef of existing variable",
				"int foo; typedef int foo; (\nfoo\n\n + 2);",
				null,
				Arrays.asList( "int", "foo", ";",
				               "typedef", "int", "foo", ";",
				               "(", "foo", "+", "2", ")", ";" ),
			},
			{
				"script directive delimited with <>",
				"script <zlib.ash>;",
				null,
				Arrays.asList( "script", "<zlib.ash>", ";" ),
			},
			{
				"script directive delimited with \"\"",
				"script \"zlib.ash\"",
				null,
				Arrays.asList( "script", "\"zlib.ash\"" ),
			},
			{
				"script directive without delimiter",
				"script zlib.ash",
				null,
				Arrays.asList( "script", "zlib.ash" ),
			},
			{
				"Unterminated script directive",
				"script \"zlib.ash",
				"No closing \" found",
				null,
			},
			{
				"Script with bom",
				"\ufeff    'hello world'",
				null,
				Arrays.asList( "'hello world'" ),
			},
			{
				"Simple operator assignment",
				"int x = 3;",
				null,
				Arrays.asList( "int", "x", "=", "3", ";" ),
			},
			{
				"Compound operator assignment",
				"int x; x += 3;",
				null,
				Arrays.asList( "int", "x", ";", "x", "+=", "3", ";" ),
			},
			{
				"Aggregate assignment to primitive",
				// What is this, C?
				"int x; x = {1};",
				"Cannot use an aggregate literal for type int",
				null,
			},
			{
				"Primitive assignment to aggregate",
				"int[4] x = 1;",
				"Cannot store int in x of type int [4]",
				null,
			},
			{
				"Compound assignment to aggregate",
				"int[4] x; x += 1;",
				"Cannot use '+=' on an aggregate",
				null,
			},
			{
				"Aggregate assignment to aggregate",
				"int[4] x; x = {1, 2, 3, 4};",
				null,
				Arrays.asList( "int", "[", "4", "]", "x", ";",
				               "x", "=", "{",
				               "1", ",", "2", ",", "3", ",", "4",
				               "}", ";" ),
			},
			{
				"since passes for low revision",
				"since r100;",
				null,
				Arrays.asList( "since", "r100", ";" ),
			},
			{
				"since fails for high revision",
				"since r2000000000;",
				"requires revision r2000000000 of kolmafia or higher",
				null,
			},
			{
				"Invalid since version",
				"since 10;",
				"invalid 'since' format",
				null,
			},
			{
				"since passes for low version",
				"since 1.0;",
				null,
				Arrays.asList( "since", "1.0", ";" ),
			},
			{
				"since fails for high version",
				"since 2000000000.0;",
				"requires version 2000000000.0 of kolmafia or higher",
				null,
			},
			{
				"since fails for not-a-number",
				"since yesterday;",
				"invalid 'since' format",
				null,
			},
			{
				"Basic function with one argument",
				"void f(int a) {}",
				null,
				Arrays.asList( "void", "f", "(", "int", "a", ")", "{", "}" ),
			},
			{
				"Complex expression parsing",
				// Among other things, this tests that true / false are case insensitive, in case
				// you want to pretend you're working in Python, C, or both.
				"(!(~-5 == 10) && True || FALSE);",
				null,
				Arrays.asList( "(", "!",
				               "(", "~", "-", "5", "==", "10", ")",
				               "&&", "True", "||", "FALSE",
				               ")", ";" ),
			},
			{
				"Numeric literal split after negative",
				"-/*negative \nnumber*/1.23;",
				null,
				Arrays.asList( "-", "/*negative", "number*/", "1", ".", "23", ";" ),

			},
			{
				"Float literal split after decimal",
				"1./*decimal\n*/23;",
				null,
				Arrays.asList( "1", ".", "/*decimal", "*/", "23", ";" ),

			},
			{
				"Float literal with no integral component",
				"-.123;",
				null,
				Arrays.asList( "-", ".", "123", ";" ),

			},
			/*
			  There's code for this case, but we encounter a separate error ("Record expected").
			{
				"Float literal with no decimal component",
				"123.;",
				null,
				Arrays.asList( "123", ".", ";" ),

			},
			*/
			{
				"Int literal with method call.",
				"123.to_string();",
				null,
				Arrays.asList( "123", ".", "to_string", "(", ")", ";" ),

			},
			{
				"Unary minus",
				"int x; (-x);",
				null,
				Arrays.asList( "int", "x", ";", "(", "-", "x", ")", ";" ),

			},
			{
				"Chained if/else-if/else",
				"if (false) {} else if (false) {} else if (false) {} else {}",
				null,
				Arrays.asList( "if", "(", "false", ")", "{", "}",
				               "else", "if", "(", "false", ")", "{", "}",
				               "else", "if", "(", "false", ")", "{", "}",
				               "else", "{", "}" ),

			},
			{
				"Multiple else",
				"if (false) {} else {} else {}",
				"Else without if",
				null,

			},
			{
				"else-if after else",
				"if (false) {} else {} else if (true) {}",
				"Else without if",
				null,

			},
			{
				"else without if",
				"else {}",
				"Unknown variable 'else'",
				null,

			},
			{
				"Multiline cli_execute script",
				"cli_execute {\n  echo hello world;\n  echo sometimes we don't have a semicolon\n}",
				null,
				Arrays.asList("cli_execute", "{",
				              "echo hello world;",
				              "echo sometimes we don't have a semicolon",
				              "}"),

			},
			{
				"Switch, case label without expression",
				"switch { case: }",
				"Case label needs to be followed by an expression",
				null,
			},
			{
				"Switch, multiple default labels",
				"switch { default: default: }",
				"Only one default label allowed in a switch statement",
				null,
			},
			{
				"Sort, no sorting expression",
				"int[] x; sort x by",
				"Expression expected",
				null,
			},
			{
				"For loop, no/bad initial expression",
				"for x from",
				"Expression for initial value expected",
				null,
			},
			{
				"For loop, no/bad ceiling expression",
				"for x from 0 to",
				"Expression for floor/ceiling value expected",
				null,
			},
			{
				"For loop, no/bad increment expression",
				"for x from 0 to 1 by",
				"Expression for increment value expected",
				null,
			},
			{
				"basic template string",
				"`this is some math: {4 + 7}`",
				null,
				Arrays.asList( "`this is some math: {", "4", "+", "7", "}`" ),

			},
			{
				"template string with a new variable",
				"`this is some math: {int x = 4; x + 7}`",
				"Unknown variable 'int'",
				null,
			},
			{
				"template string with predefined variable",
				"int x; `this is some math: {(x = 4) + 7}`",
				null,
				Arrays.asList( "int", "x", ";", "`this is some math: {",
				               "(", "x", "=", "4", ")",
				               "+", "7", "}`" ),
			},
			{
				"template string with unclosed comment",
				"`this is some math: {7 // what determines the end?}`",
				"Expected }, found end of file",
				null,

			},
			{
				"template string with terminated comment",
				"`this is some math: {7 // turns out you need a newline\r\n\t}`",
				null,
				Arrays.asList( "`this is some math: {", "7",
				               "// turns out you need a newline", "}`" )
			},
			{
				"template string with multiple templates",
				"`{'hello'} {'world'}`",
				null,
				Arrays.asList( "`{", "'hello'", "} {", "'world'", "}`" )
			},
			{
				"string with uninteresting escape",
				"'\\z'",
				null,
				Arrays.asList( "'\\z'" )
			},
			{
				"string with unclearly terminated octal escape",
				"'\\1131'",
				null,
				// \113 is char code 75, which maps to 'K'
				Arrays.asList( "'\\1131'" )
			},
			{
				"string with insufficient octal digits",
				"'\\11'",
				"Octal character escape requires 3 digits",
				null,
			},
			{
				"string with invalid octal digits",
				"'\\118'",
				"Octal character escape requires 3 digits",
				null,
			},
			{
				"string with hex digits",
				"'\\x3fgh'",
				null,
				Arrays.asList( "'\\x3fgh'" ),
			},
			{
				"string with invalid hex digits",
				"'\\xhello'",
				"Hexadecimal character escape requires 2 digits",
				null,
			},
			{
				"string with insufficient hex digits",
				"'\\x1'",
				"Hexadecimal character escape requires 2 digits",
				null,
			},
			{
				"string with unicode digits",
				"'\\u0041'",
				null,
				Arrays.asList( "'\\u0041'" ),
			},
			{
				"string with invalid unicode digits",
				"'\\uzzzz'",
				"Unicode character escape requires 4 digits",
				null,
			},
			{
				"string with insufficient unicode digits",
				"'\\u1'",
				"Unicode character escape requires 4 digits",
				null,
			},
			{
				"string with escaped eof",
				"'\\",
				"No closing ' found",
				null,
			},
			{
				"plural typed constant with escaped eof",
				"$items[true\\",
				"No closing ] found",
				null,
			},
			{
				"plural typed constant with escaped space",
				"$effects[\n\tBuy!\\ \\ Sell!\\ \\ Buy!\\ \\ Sell!\n]",
				null,
				Arrays.asList( "$", "effects", "[",  "Buy!\\ \\ Sell!\\ \\ Buy!\\ \\ Sell!", "]" )
			},
			{
				"unterminated plural aggregate typ",
				"int[",
				"Missing index token",
				null,
			},
			// TODO: add tests for size == 0, size < 0, which ought to fail.
			{
				"array with explicit size",
				"int[2] x;",
				null,
				Arrays.asList( "int", "[", "2", "]", "x", ";" ),
			},
			{
				"array with unspecified size",
				"int[] x;",
				null,
				Arrays.asList( "int", "[", "]", "x", ";" ),
			},
			{
				"multidimensional array with partially specified size (1)",
				"int[2][] x;",
				null,
				Arrays.asList( "int", "[", "2", "]", "[", "]", "x", ";" ),
			},
			{
				"multidimensional array with partially specified size (2)",
				"int[][2] x;",
				null,
				Arrays.asList( "int", "[", "]", "[", "2", "]", "x", ";" ),
			},
			{
				"multidimensional array with explicit size",
				"int[2, 2] x;",
				null,
				Arrays.asList( "int", "[", "2", ",", "2", "]", "x", ";" ),
			},
			// TODO: `typedef int[] intArray` and aggregate shouldn't be valid keys.
			{
				"map with non-primitive key type",
				"record a{int b;}; int[a] x;",
				"Index type 'a' is not a primitive type",
				null,
			},
			{
				"multidimensional map with partially specified comma-separated type",
				"int[2,] x;",
				"Missing index token",
				null,
			},
			{
				"multidimensional map with unspecified comma-separated type",
				"int[,,,] x;",
				"Missing index token",
				null,
			},
			{
				"multidimensional map with partially-specified comma-separated type",
				"int[int,] x;",
				"Missing index token",
				null,
			},
			{
				"abruptly terminated 1-d map",
				"int[int",
				"Expected , or ], found end of file",
				null,
			},
			{
				"abruptly terminated 1-d array",
				"int[4",
				"Expected ], found end of file",
				null,
			},
			{
				"multidimensional map with comma-separated type",
				"int[int, int] x;",
				null,
				Arrays.asList( "int", "[", "int", ",", "int", "]", "x", ";" ),
			},
			{
				"multidimensional map with chained brackets with types",
				"int[int][int] x;",
				null,
				Arrays.asList( "int", "[", "int", "]", "[", "int", "]", "x", ";" ),
			},
			{
				"multidimensional map with unspecified type in chained empty brackets",
				"int[][] x;",
				null,
				Arrays.asList( "int", "[", "]", "[", "]", "x", ";" ),
			},
		} );
	}

	public ParserTest( String desc, String script, String errorMessage, List<String> resultTokens )
	{
		this.desc = desc;
		this.script = script;

		this.tokens = resultTokens;

		this.errorText = errorMessage;
	}

	@Test
	public void testScriptValidity()
	{
		ByteArrayInputStream istream = new ByteArrayInputStream( this.script.getBytes( StandardCharsets.UTF_8 ) );
		Parser p = new Parser( /*scriptFile=*/null, /*stream=*/istream, /*imports=*/null );

		if ( this.errorText != null )
		{
			ScriptException e = assertThrows( this.desc, ScriptException.class, p::parse );
			assertThat( this.desc, e.getMessage(), containsString( this.errorText ) );
			return;
		}

		// This will fail if an exception is thrown.
		p.parse();
		assertEquals( this.desc, this.tokens, p.getTokensContent() );
	}
}
