package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.request.ChateauRequest;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RestChateauMenuItem
	extends ThreadedMenuItem
{
	public RestChateauMenuItem()
	{
		super( "Rest in the Chateau", new RestChateauListener() );
	}

	private static class RestChateauListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			String turnCount = InputFieldUtilities.input( "Rest for how many turns?", "1" );
			if ( turnCount == null )
			{
				return;
			}

			ChateauRequest request = new ChateauRequest( "chateau_restbox" );
			int turnCountValue = StringUtilities.parseInt( turnCount );

			KoLmafia.makeRequest( request, turnCountValue );
		}
	}
}
