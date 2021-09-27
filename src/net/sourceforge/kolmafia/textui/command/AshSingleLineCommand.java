package net.sourceforge.kolmafia.textui.command;

import java.io.ByteArrayInputStream;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;

import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;

import net.sourceforge.kolmafia.textui.parsetree.CompositeValue;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public class AshSingleLineCommand
	extends AbstractCommand
{
	public AshSingleLineCommand()
	{
		this.flags = KoLmafiaCLI.FULL_LINE_CMD;
		this.usage = " <statement> - test a line of ASH code without having to edit a script.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( !parameters.endsWith( ";" ) && !parameters.endsWith( "}" ) )
		{
			parameters += ";";
		}

		ByteArrayInputStream istream = new ByteArrayInputStream( ( parameters + KoLConstants.LINE_BREAK ).getBytes() );

		AshRuntime interpreter = new AshRuntime();
		interpreter.validate( null, istream );
		Value rv;

		try
		{
			interpreter.cloneRelayScript( this.callerController );
			rv = interpreter.execute( "main", null );
		}
		finally
		{
			interpreter.finishRelayScript();
		}

		if ( cmd.endsWith( "q" ) )
		{
			return;
		}

		KoLmafia.updateDisplay( "Returned: " + rv );

		rv = Value.asProxy( rv );
		if ( rv instanceof CompositeValue )
		{
			RuntimeLibrary.dump( (CompositeValue) rv );
		}
	}
}
