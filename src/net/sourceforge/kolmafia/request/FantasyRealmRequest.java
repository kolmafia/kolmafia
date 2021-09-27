package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

public class FantasyRealmRequest
	extends CreateItemRequest
{
	public FantasyRealmRequest( final Concoction conc )
	{
		super( "choice.php", conc );
	}

	private static String itemIdToOption( final int itemId )
	{
		return  itemId == ItemPool.FR_WARRIOR_HELM ? "1" :
			itemId == ItemPool.FR_MAGE_HAT ? "2" :
			itemId == ItemPool.FR_ROGUE_MASK ? "3" :
			"6";
	}

	@Override
	public void run()
	{
		String name = this.getName();

		KoLmafia.updateDisplay( "Creating 1 " + name + "..." );

		GenericRequest request = new GenericRequest( "place.php?whichplace=realm_fantasy&action=fr_initcenter" );
		RequestThread.postRequest( request );
		request.constructURLString( "choice.php?whichchoice=1280&option=" + FantasyRealmRequest.itemIdToOption( this.getItemId() ) );
		RequestThread.postRequest( request );
		ConcoctionDatabase.refreshConcoctions( false );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) || !urlString.contains( "whichchoice=1280" ) )
		{
			return false;
		}

		return true;
	}
}
