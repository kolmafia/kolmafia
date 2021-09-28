package net.sourceforge.kolmafia.textui.command;

import java.util.List;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.FaxBot;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;

import net.sourceforge.kolmafia.swingui.FaxRequestFrame;

public class FaxbotCommand
	extends AbstractCommand
{
	public FaxbotCommand()
	{
		this.usage = " [command] - send the command to faxbot";
	}

	@Override
	public void run( final String cmd, final String command )
	{
		FaxBotDatabase.configure();

		boolean tried = false;

		for ( FaxBot bot : FaxBotDatabase.faxbots )
		{
			if ( bot == null )
			{
				continue;
			}
			String botName = bot.getName();
			if ( botName == null )
			{
				continue;
			}

			List commands = bot.findMatchingCommands( command );
			if ( commands.isEmpty() )
			{
				continue;
			}

			if ( commands.size() > 1 )
			{
				RequestLogger.printList( commands );
				RequestLogger.printLine();

				RequestLogger.printLine( "[" + command + "] has too many matches in bot " + botName );
				continue;
			}

			if ( !FaxRequestFrame.isBotOnline( botName ) )
			{
				continue;
			}

			Monster monster = bot.getMonsterByCommand( (String)commands.get( 0 ) );
			tried = true;
			if ( FaxRequestFrame.requestFax( botName, monster, false ) )
			{
				return;
			}
		}
		if ( !tried )
		{
			KoLmafia.updateDisplay( KoLConstants.MafiaState.ABORT, "No faxbots accept that command." );
		}
	}
}
