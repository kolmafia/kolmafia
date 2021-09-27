package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;

public class FloundryRequest
	extends CreateItemRequest
{
	public FloundryRequest( final Concoction conc )
	{
		super( "clan_viplounge.php", conc );
	}

	@Override
	public void run()
	{
		if ( !KoLmafia.permitsContinue() || this.getQuantityNeeded() <= 0 )
		{
			return;
		}

		KoLmafia.updateDisplay( "Creating " + this.getName() + "..." );
		ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.FLOUNDRY, this.getItemId() );

		RequestThread.postRequest( request );
	}
}
