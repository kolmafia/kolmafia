package net.sourceforge.kolmafia.textui;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import java.util.Arrays;
import java.util.Collection;

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

	// Error text. If null, then we expect no error to be thrown (i.e. the provided script is
	// well-formed).
	public final String errorText;

	@Parameters
	public static Collection<Object[]> data()
	{
		return Arrays.asList( new Object[][] {
			{
				"Unterminated Java for-loop",
				"for (",
				// Note that the test harness does not specify a File, so the error message lacks
				// file name as well line number.
				"Variable reference expected ()",
			},
			{
				"valid empty Java for-loop",
				"for (;;) {}",
				null,
			},
			{
				"Java for-loop with new variable",
				"for (int i = 0; i < 5; ++i) {}",
				null,
			},
			{
				"Multiline string, end of line not escaped",
				"'\n'",
				"No closing ' found ()"
			},
			{
				"Multiline string, end of line properly escaped",
				"'\\\n'",
				null
			},
			{
				"Multiline string, end of line properly escaped + empty lines",
				"'\\\n\n   \n\n\n'",
				null
			},
			{
				"Multiline string, end of line properly escaped + empty lines + comment",
				"'\\\n\n\n//Comment\n\n'",
				"No closing ' found ()"
			},
			{
				"Unterminated string template",
				"`{`",
				// The parser tries to start a new string template inside the expression
				"No closing ` found ()"
			},
			/*{
				//Commented out, because the current behaviour prints
				// "Expected }, found ; ()"
				// , and it's just about to change (from the Tokens patch)

				"Abruptly unterminated string template",
				"`{",
				"Expected }, found end of file ()"
			},*/
			/*{
				//Idem

				"Typed constant, no bracket",
				"$int",
				"Expected [, found end of file ()"
			},*/
			{
				"Typed constant non-successive characters",
				"$		boolean 	 [ 		false ]",
				null
			},
			{
				"Typed constant escaped characters",
				"$boolean[\\f\\a\\l\\s\\e]",
				null
			},
			{
				"Typed constant bad typecast",
				"$boolean['']",
				"Bad boolean value: \"''\" ()"
			},
			{
				"Typed constant, unknown type",
				"$foo[]",
				"Unknown type foo ()"
			},
			{
				"Typed constant, non-primitive type",
				"record r {int i;}; $r[]",
				"Non-primitive type r ()"
			},
			{
				"Typed constant multiline",
				"$boolean[\n]",
				"No closing ] found ()"
			},
			{
				"Typed constant, nested brackets, proper",
				"$item[[8042]rock]",
				null
			},
			{
				"Typed constant, nested brackets, improper",
				"$item[[abc]]",
				"Bad item value: \"[abc]\" ()"
			},
			{
				"Plural constant, abrupt end",
				"$booleans[",
				"No closing ] found ()"
			},
			/*{
				//Commented out, because the current behaviour prints
				// the "transformed" type, rather than the original one
				// (i.e. current error message is "Unknown type kolmafium ()")
				// , which is just about to change in an incoming patch.

				"Plural constant, unknown plural type",
				"$kolmafia[]",
				"Unknown type kolmafia ()"
			},*/
			/*{
				// idem

				"Plural constant, RAM-protection",
				"$strings[]",
				"Can't enumerate all strings ()"
			},*/
			{
				"Plural constant, non... \"traditional\" plurals",
				"$bounties[]; $classes[]; $phyla[]",
				null
			},
			{
				"Plural constant non-successive characters",
				"$		booleans 	 [ 		 ]",
				null
			},
			{
				"Plural constant multiline",
				// End-of-lines can appear anywhere
				"$booleans[true\n\n\n,\nfalse, false\n,false,\ntrue,tr\nue]",
				null
			},
			{
				"Plural constant w/ escape characters",
				// *Escaped* end-of-lines and escaped "\n"s get added to the value
				"$booleans[tr\\\nu\\ne]",
				"Bad boolean value: \"tr\nu\ne\" ()"
			},
			{
				"Plural constant, nested brackets, proper",
				"$items[[8042]rock]",
				null
			},
			{
				"Plural constant, nested brackets, improper",
				"$items[[abc]]",
				"Bad item value: \"[abc]\" ()"
			},
			{
				"Plural constant, single slash",
				"$booleans[tr/Comment\nue]",
				"Bad boolean value: \"tr/Commentue\" ()"
			},
			{
				"Plural constant, comment",
				"$booleans[tr//Comment\nue]",
				null
			}
		} );
	}

	public ParserTest( String desc, String script, String errorText )
	{
		this.desc = desc;
		this.script = script;
		this.errorText = errorText;
	}

	@Test
	public void testScriptValidity()
	{
		ByteArrayInputStream istream = new ByteArrayInputStream( this.script.getBytes() );
		Parser p = new Parser( /*scriptFile=*/null, /*stream=*/istream, /*imports=*/null );

		if ( this.errorText == null )
		{
			// This will fail if an exception is thrown.
			p.parse();
			return;
		}

		ScriptException e = assertThrows( this.desc, ScriptException.class, p::parse );
		assertEquals( this.desc, e.getMessage(), this.errorText );
	}
}
