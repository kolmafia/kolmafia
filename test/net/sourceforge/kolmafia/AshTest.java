package net.sourceforge.kolmafia;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.StringBuilder;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.textui.command.CallScriptCommand;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

@RunWith( Parameterized.class )
public class AshTest
{
	// Directory containing expected output.
	private static final File EXPECTED_LOCATION = new File( UtilityConstants.ROOT_LOCATION, "expected/" );

	@Parameter
	public String script;

	@Parameters
	public static Object[] data()
	{
		return new Object[] {"test_evilometer.txt"};
	}

	// Looks for the file "test/root/out/" + script + ".out".
	private static String getExpectedOutput( String script )
	{
		BufferedReader reader =
			DataUtilities.getReader( new File( EXPECTED_LOCATION, script + ".out" ) );
		StringBuilder sb = new StringBuilder();
		for ( Object line : reader.lines().toArray() )
		{
			sb.append( ( (String) line ) + "\n" );
		}
		return sb.toString();
	}

	@Test
	public void testScript()
	{
		String expectedOutput = getExpectedOutput( script );
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		PrintStream out = new PrintStream( ostream );
		// Inject custom output stream.
		RequestLogger.openCustom( out );

		CallScriptCommand command = new CallScriptCommand();
		command.run( "call", script );

		String output = ostream.toString();
		assertEquals( expectedOutput, output );
	}
}
