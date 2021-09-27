package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;

import net.sourceforge.kolmafia.request.CurseRequest;

public class ThrowItemCommand
	extends AbstractCommand
{
	public ThrowItemCommand()
	{
		this.usage = "[?] <item> at <player> [ || <message part 1> | <message part 2> ... ] - use item on someone else";
	}

	@Override
	public void run( String command, String parameters )
	{
		String msg = "";
		int splitPos = parameters.indexOf( "||" );
		if ( splitPos != -1 )
		{
			msg = parameters.substring( splitPos + 2 ).trim();
			parameters = parameters.substring( 0, splitPos ).trim();
		}
		splitPos = parameters.indexOf( " at " );
		if ( splitPos == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "No <s>victim</s>recipient specified." );
			return;
		}
		String target = parameters.substring( splitPos + 4 ).trim();
		parameters = parameters.substring( 0, splitPos ).trim();
		AdventureResult item = ItemFinder.getFirstMatchingItem( parameters, Match.ANY );
		if ( item != null )
		{
			if ( !ItemDatabase.getAttribute( item.getItemId(), ItemDatabase.ATTR_CURSE ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "The " +
					item.getName() + " is not properly balanced for throwing." );
				return;
			}
			RequestThread.postRequest( new CurseRequest( item, target, msg ) );
		}
	}
}
