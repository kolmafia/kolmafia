package net.sourceforge.kolmafia.swingui;

import net.sourceforge.kolmafia.request.GenericRequest;

public class ProfileFrame
	extends RequestFrame
{
	private static ProfileFrame INSTANCE = null;

	public ProfileFrame()
	{
		super( "Player Profile" );
		ProfileFrame.INSTANCE = this;
	}

	public static final void showRequest( final GenericRequest request )
	{
		if ( !GenericFrame.instanceExists() )
		{
			return;
		}

		if ( ProfileFrame.INSTANCE == null )
		{
			GenericFrame.createDisplay( ProfileFrame.class );
		}

		ProfileFrame.INSTANCE.refresh( request );
	}

	@Override
	public boolean hasSideBar()
	{
		return false;
	}
}
