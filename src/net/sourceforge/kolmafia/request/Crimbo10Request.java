package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.RequestLogger;

public class Crimbo10Request
	extends GenericRequest
{
	public Crimbo10Request()
	{
		super( "crimbo10.php" );
	}

	@Override
	public void processResults()
	{
		Crimbo10Request.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "crimbo10.php" ) )
		{
			return;
		}

		String action = GenericRequest.getAction( location );
		if ( action == null || action.equals( "buygift" ) )
		{
			CRIMBCOGiftShopRequest.parseResponse( location, responseText );
			return;
		}
	}

	public static String locationName( final String urlString )
	{
		if ( urlString.indexOf( "place=office" ) != -1 )
		{
			return "Mr. Mination's Office";
		}
		if ( urlString.indexOf( "place=giftshop" ) != -1 )
		{
			return "the Gift Shop";
		}
		return null;
	}

	private static String visitLocation( final String urlString )
	{
		String name = Crimbo10Request.locationName( urlString );
		if ( name != null )
		{
			return "Visiting " + name + " in CRIMBCO Headquarters";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "crimbo10.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		String message = null;

		// We want to log certain simple visits
		if ( action == null )
		{
			message = Crimbo10Request.visitLocation( urlString );
		}

		// Buy stuff in the CRIMBCO Gift Shop
		else if ( action.equals( "buygift" ) )
		{
			// Let CRIMBCOGiftShopRequest claim this
			return CRIMBCOGiftShopRequest.registerRequest( urlString );
		}

		// Unknown action
		else
		{
			return false;
		}

		if ( message == null )
		{
			return true;
		}

		RequestLogger.printLine();
		RequestLogger.updateSessionLog();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
