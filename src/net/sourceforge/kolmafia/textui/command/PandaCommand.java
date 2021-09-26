package net.sourceforge.kolmafia.textui.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;

import net.sourceforge.kolmafia.request.PandamoniumRequest;

public class PandaCommand
	extends AbstractCommand
{
	private static final Pattern COMMAND_PATTERN1 = Pattern.compile( "^\\s*(moan|temple)\\s*$" );
	private static final Pattern COMMAND_PATTERN2 = Pattern.compile( "^\\s*comedy\\s*([^\\s]+)\\s*$" );
	private static final Pattern COMMAND_PATTERN3 = Pattern.compile( "^\\s*arena\\s*([^\\s]+)\\s*(.+)\\s*$" );

	public PandaCommand()
	{
		this.usage = " moan | temple | comedy <type> | arena <bandmember> <item> - interact with NPCs in Pandamonium";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What do you want to do in Pandamonium?" );
			return;
		}

		PandamoniumRequest request = null;
		Matcher m = COMMAND_PATTERN1.matcher( parameters );
		if ( m.find() )
		{
			// Visit a place in Pandamonium
			String location = m.group(1);
			int place = 0;
			if ( location.equalsIgnoreCase( "moan" ) )
			{
				place = PandamoniumRequest.MOAN;
			}
			else if ( location.equalsIgnoreCase( "temple" ) )
			{
				place = PandamoniumRequest.TEMPLE;
			}
			request = new PandamoniumRequest( place );
		}

		m =  COMMAND_PATTERN2.matcher( parameters );
		if ( request == null && m.find() )
		{
			// Attempt comedy on Mourn in the comedy club
			String type = m.group(1);
			String comedy = PandamoniumRequest.getComedyType( type );
			if ( comedy == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "What kind of comedy is \"" + type + "\"?" );
				return;
			}
			request = new PandamoniumRequest( type );
		}

		m =  COMMAND_PATTERN3.matcher( parameters );
		if ( request == null && m.find() )
		{
			// Give an item to a bandmember
			String demon = m.group(1);
			String member = PandamoniumRequest.getBandMember( demon );
			if ( member == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "I don't think \"" +	demon + "\" is a member of the band." );
				return;
			}

			String itemName = m.group(2);
			AdventureResult item = ItemFinder.getFirstMatchingItem( itemName, Match.ANY );
			if ( item == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "WHAT did you want to give to " + member + "?" );
				return;
			}
			request = new PandamoniumRequest( member, item.getItemId() );
		}

		if ( request == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What do you want to do in Pandamonium?" );
			return;
		}

		RequestThread.postRequest( request );
	}
}
