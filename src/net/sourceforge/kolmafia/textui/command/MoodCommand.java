package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MoodCommand
	extends AbstractCommand
{
	{
		this.usage = " list | listall | clear | autofill | execute | repeat [<numTimes>] | <moodName> [<numTimes>] - mood management.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.toLowerCase();

		if ( parameters.length() == 0 || parameters.equals( "list" ) )
		{
			RequestLogger.printList( MoodManager.getTriggers() );
			return;
		}
		else if ( parameters.equals( "listall" ) )
		{
			RequestLogger.printList( MoodManager.getAvailableMoods() );
		}
		else if ( parameters.equals( "clear" ) )
		{
			MoodManager.removeTriggers( MoodManager.getTriggers().toArray() );
			MoodManager.saveSettings();
			RequestLogger.printLine( "Cleared mood." );
		}
		else if ( parameters.equals( "autofill" ) )
		{
			MoodManager.maximalSet();
			MoodManager.saveSettings();
			RequestLogger.printList( MoodManager.getTriggers() );
		}
		else if ( parameters.equals( "execute" ) )
		{
			if ( RecoveryManager.isRecoveryActive() || MoodManager.isExecuting() )
			{
				return;
			}

			MoodManager.checkpointedExecute( 0 );
			RequestLogger.printLine( "Mood swing complete." );
		}
		else if ( parameters.startsWith( "repeat" ) )
		{
			if ( RecoveryManager.isRecoveryActive() || MoodManager.isExecuting() )
			{
				return;
			}

			int multiplicity = 0;
			int spaceIndex = parameters.lastIndexOf( " " );

			if ( spaceIndex != -1 )
			{
				multiplicity = StringUtilities.parseInt( parameters.substring( spaceIndex + 1 ) );
			}

			MoodManager.checkpointedExecute( multiplicity );
			RequestLogger.printLine( "Mood swing complete." );
		}
		else
		{
			int multiplicity = 0;
			int spaceIndex = parameters.lastIndexOf( " " );

			if ( spaceIndex != -1 )
			{
				String possibleMultiplicityString = parameters.substring( spaceIndex + 1 );
				
				if ( StringUtilities.isNumeric( possibleMultiplicityString ) )
				{
					multiplicity = StringUtilities.parseInt( possibleMultiplicityString );
					parameters = parameters.substring( 0, spaceIndex );
				}
			}

			String previousMood = Preferences.getString( "currentMood" );
			MoodManager.setMood( parameters );

			if ( multiplicity > 0 )
			{
				this.CLI.executeCommand( "mood", "repeat " + multiplicity );
				MoodManager.setMood( previousMood );
			}
		}
	}
}
