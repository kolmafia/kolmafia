package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.FalloutShelterRequest;

import net.sourceforge.kolmafia.session.Limitmode;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RestCampgroundMenuItem
	extends ThreadedMenuItem
{
	public RestCampgroundMenuItem()
	{
		super( "Rest in House", new RestCampgroundListener() );
	}

	private static class RestCampgroundListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			String turnCount = InputFieldUtilities.input( "Rest for how many turns?", "1" );
			if ( turnCount == null || Limitmode.limitCampground() || KoLCharacter.isEd() || KoLCharacter.inNuclearAutumn() )
			{
				return;
			}

			if ( !KoLCharacter.inNuclearAutumn() )
			{
				CampgroundRequest request = new CampgroundRequest( "rest" );
				int turnCountValue = StringUtilities.parseInt( turnCount );

				KoLmafia.makeRequest( request, turnCountValue );
			}
			else
			{
				FalloutShelterRequest request = new FalloutShelterRequest( "vault1" );
				int turnCountValue = StringUtilities.parseInt( turnCount );

				KoLmafia.makeRequest( request, turnCountValue );
			}

		}
	}
}
