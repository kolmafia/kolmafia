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
