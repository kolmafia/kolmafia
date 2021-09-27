package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RestClanSofaMenuItem
	extends ThreadedMenuItem
{
	public RestClanSofaMenuItem()
	{
		super( "Sleep in Sofa", new RestClanSofaListener() );
	}

	private static class RestClanSofaListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			String turnCount = InputFieldUtilities.input( "Sleep for how many turns?", "1" );
			if ( turnCount == null )
			{
				return;
			}

			ClanRumpusRequest request = new ClanRumpusRequest( RequestType.SOFA );
			int turnCountValue = StringUtilities.parseInt( turnCount );

			request.setTurnCount( turnCountValue );
			RequestThread.postRequest( request );
		}
	}
}
