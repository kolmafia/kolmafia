package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.session.ClanManager;

public class ClanHallRequest
{
	private static final Pattern CLAN_NAME_PATTERN = Pattern.compile( "<center><b>(.*?)</b>" );

	public static void parseResponse( String location, String responseText )
	{
		if ( !location.startsWith( "clan_hall.php" ) )
		{
			return;
		}
		Matcher m = CLAN_NAME_PATTERN.matcher( responseText );
		if ( m.find() )
		{
			// If name is different from what we know, then we need to refresh information
			String currentClan = ClanManager.getClanName( false );
			String newClan = m.group( 1 );
			if ( currentClan == null || !currentClan.equals( newClan ) )
			{
				ClanManager.resetClanId();
				ClanManager.getClanId();
			}
		}
	}
}
