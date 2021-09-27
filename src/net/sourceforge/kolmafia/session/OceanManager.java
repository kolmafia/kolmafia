package net.sourceforge.kolmafia.session;

import java.awt.Point;

import java.io.File;
import java.io.PrintStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class OceanManager
{
	private static final GenericRequest OCEAN_HANDLER = new GenericRequest( "ocean.php" );
	private static final Pattern OCEAN_PATTERN = Pattern.compile( "(\\d+),(\\d+)" );

	public static final void processOceanAdventure()
	{
		OceanManager.processOceanAdventure( OceanManager.OCEAN_HANDLER );
	}

	public static final Point getDestination()
	{
		String dest = Preferences.getString( "oceanDestination" );
		if ( dest.equals( "manual" ) )
		{
			return null;
		}

		int lon = 0;
		int lat = 0;

		if ( dest.equals( "muscle" ) )
		{
			lon = 12;
			lat = 84;
		}
		else if ( dest.equals( "mysticality" ) )
		{
			lon = 3;
			lat = 35;
		}
		else if ( dest.equals( "moxie" ) )
		{
			lon = 13;
			lat = 91;
		}
		else if ( dest.equals( "sphere" ) )
		{
			lon = 59;
			lat = 10;
		}
		else if ( dest.equals( "plinth" ) )
		{
			lon = 63;
			lat = 29;
		}
		else if ( dest.indexOf( "," ) != -1 )
		{
			Matcher matcher = OceanManager.OCEAN_PATTERN.matcher( dest );
			if ( matcher.find() )
			{
				lon = StringUtilities.parseInt( matcher.group( 1 ) );
				lat = StringUtilities.parseInt( matcher.group( 2 ) );
			}
		}

		return new Point( lon, lat );
	}

	public static final void processOceanAdventure( final GenericRequest request )
	{
		Point destination = OceanManager.getDestination();

		if ( destination == null )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "Pick a course." );
			request.showInBrowser( true );
			return;
		}

		int lon = destination.x;
		int lat = destination.y;

		String action = Preferences.getString( "oceanAction" );
		boolean stop = action.equals( "stop" ) || action.equals( "savestop" );
		boolean show = action.equals( "show" ) || action.equals( "saveshow" );
		boolean save = action.equals( "savecontinue" ) || action.equals( "saveshow" ) || action.equals( "savestop" );

		while ( true )
		{
			if ( lon < 1 || lon > 242 || lat < 1 || lat > 100 )
			{
				// Pick a random destination
				lon = KoLConstants.RNG.nextInt( 242 ) + 1;
				lat = KoLConstants.RNG.nextInt( 100 ) + 1;
			}

			String coords = "Coordinates: " + lon + ", " + lat;
			RequestLogger.printLine( coords );
			RequestLogger.updateSessionLog( coords );

			// ocean.php?lon=10&lat=10
			request.constructURLString( "ocean.php" );
			request.clearDataFields();
			request.addFormField( "lon", String.valueOf( lon ) );
			request.addFormField( "lat", String.valueOf( lat ) );

			request.run();

			if ( save )
			{
				// Save the response Text
				File output = new File( KoLConstants.DATA_LOCATION, "ocean.html" );
				PrintStream writer = LogStream.openStream( output, false );

				// Trim to contain only HTML body
				int start = request.responseText.indexOf( "<body>" );
				int end = request.responseText.indexOf( "</body>" );
				String text = request.responseText.substring( start + 6, end );
				writer.println( text );
				writer.close();
			}

			if ( stop )
			{
				// Show result in browser and stop automation
				KoLmafia.updateDisplay( MafiaState.ABORT, "Stop" );
				request.showInBrowser( true );
				return;
			}

			if ( show )
			{
				// Show the response in the browser
				request.showInBrowser( true );
			}

			// And continue

			// The navigator says "Sorry, Cap'm, but we can't sail
			// to those coordinates, because that's where the
			// mainland is, and we've pretty much plundered the
			// mainland dry. Perhaps a more exotic locale is in
			// order?"

			if ( request.responseText.indexOf( "that's where the mainland is" ) == -1 )
			{
				return;
			}

			// Pick a different random destination
			lon = lat = 0;
		}
	}

	private static final Pattern LON_PATTERN = Pattern.compile( "<input type=text class=text size=5 name=lon" );
	private static final Pattern LAT_PATTERN = Pattern.compile( "<input type=text class=text size=5 name=lat" );
	
	public static final void decorate( final StringBuffer buffer )
	{
		Point destination = OceanManager.getDestination();

		if ( destination == null )
		{
			return;
		}

		int lon = destination.x;
		int lat = destination.y;

		if ( lon < 1 || lon > 242 || lat < 1 || lat > 100 )
		{
			return;
		}

		Matcher lonMatcher = OceanManager.LON_PATTERN.matcher( buffer );
		if ( lonMatcher.find() )
		{
			buffer.insert( lonMatcher.end(), " value=\"" + lon + "\"" );
		}
		
		Matcher latMatcher = OceanManager.LAT_PATTERN.matcher( buffer );
		if ( latMatcher.find() )
		{
			buffer.insert( latMatcher.end(), " value=\"" + lat + "\"" );
		}
	}
}
