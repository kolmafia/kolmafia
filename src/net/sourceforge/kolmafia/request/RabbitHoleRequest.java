package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.preferences.Preferences;

public class RabbitHoleRequest
	extends PlaceRequest
{
	private final String action;

	public RabbitHoleRequest()
	{
		super( "rabbithole" );
		this.action = null;
	}

	public RabbitHoleRequest( final String action )
	{
		super( "rabbithole", action );
		this.action = action;
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return action != null && action.equals( "rabbithole_teaparty" );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return;
		}

		if ( action.equals( "rabbithole_teaparty" ) )
		{
			// You've already attended a Tea Party today, and it
			// was weird enough that you're not inclined to attend
			// another one.

			if ( responseText.indexOf( "already attended a Tea Party today" ) != -1 )
			{
				Preferences.setBoolean( "_madTeaParty", true );
			}
		}
	}
}
