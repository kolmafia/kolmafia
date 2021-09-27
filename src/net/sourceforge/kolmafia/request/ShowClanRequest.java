package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.session.ClanManager;

public class ShowClanRequest
{
	// showclan.php?action=joinclan&pwd&whichclan=38808&ajax=1&confirm=1

	public static void parseResponse( String location, String responseText )
	{
		if ( !location.startsWith( "showclan.php" ) )
		{
			return;
		}

		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			return;
		}

		if ( !action.equals( "joinclan" ) )
		{
			return;
		}

		// Normally, this redirects to clan_hall.php. However, if done
		// via "/whitelist", it uses ajax=1 and simply returns a
		// message.

		// You have now changed your allegiance.

		if ( responseText.contains( "You have now changed your allegiance" ) )
		{
			// We need to refresh information
			ClanManager.resetClanId();
			ClanManager.getClanId();
		}
	}
}
