package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.textui.AshRuntime;

import net.sourceforge.kolmafia.utilities.ByteArrayStream;

public class AshMultiLineCommand
	extends AbstractCommand
{
	public AshMultiLineCommand()
	{
		this.usage = " - embed an ASH script in a CLI script.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		ByteArrayStream ostream = new ByteArrayStream();

		String currentLine = this.CLI.getNextLine( null );

		while ( currentLine != null && !currentLine.equals( "</inline-ash-script>" ) )
		{
			try
			{
				ostream.write( currentLine.getBytes() );
				ostream.write( KoLConstants.LINE_BREAK.getBytes() );
			}
			catch ( Exception e )
			{
				// Byte array output streams do not throw errors,
				// other than out of memory errors.

				StaticEntity.printStackTrace( e );
			}

			currentLine = this.CLI.getNextLine( null );
		}

		if ( currentLine == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unterminated inline ASH script." );
			return;
		}

		AshRuntime interpreter = new AshRuntime();
		interpreter.validate( null, ostream.getByteArrayInputStream() );

		try
		{
			interpreter.cloneRelayScript( this.callerController );
			interpreter.execute( "main", null );
		}
		finally
		{
			interpreter.finishRelayScript();
		}
	}
}
