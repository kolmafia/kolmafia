package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NemesisRequest
	extends GenericRequest
{
	private static final Pattern ITEM_PATTERN = Pattern.compile( "whichitem=(\\d+)" );

	public NemesisRequest()
	{
		super( "cave.php" );
	}

	public static String getAction( final String urlString )
	{
		Matcher matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );

		// cave.php is strange:
		// - visit door1 = action=door1
		// - offer to door 1 = action=door1&action=dodoor1

		String action = null;
		while ( matcher.find() )
		{
			action = matcher.group(1);
		}

		return action;
	}

	private static int getItem( final String urlString )
	{
		Matcher matcher = ITEM_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return -1;
		}

		return StringUtilities.parseInt( matcher.group(1) );
	}

	@Override
	public void processResults()
	{
		NemesisRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "cave.php" ) )
		{
			return;
		}

		String action = NemesisRequest.getAction( location );
		if ( action == null )
		{
			return;
		}

		int item = NemesisRequest.getItem( location );
		if ( item == -1 )
		{
			return;
		}

		// You put your viking helmet in the hole -- there's a slight
		// resistance, almost as though you're pushing it through some
		// manner of magical field. Then it drops away into darkness,
		// and after a moment the stone slab slides into the ceiling
		// with a loud grinding noise, opening the path before you.

		// The insanely spicy bean burrito slides easily into the hole,
		// and disappears into the darkness. If this cave has a pile of
		// thousands of years worth of rotting burritos in a hole in
		// the wall, that would go a long way towards explaining the
		// smell you noticed earlier.

		// You drop your clown whip into the hole, and the stone slab
		// grinds slowly upward and out of sight, revealing a large
		// cavern. A large cavern with multiple figures moving around
		// inside it. Which is perfect, because you were starting to
		// get sick of this puzzly nonsense, and could really use a
		// regular old fight right about now

		if ( responseText.indexOf( "stone slab slides" ) != -1 ||
		     responseText.indexOf( "into the darkness" ) != -1 ||
		     responseText.indexOf( "stone slab grinds" ) != -1)
		{
			ResultProcessor.processItem( item, -1 );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "cave.php" ) )
		{
			return false;
		}

		String action = NemesisRequest.getAction( urlString );
		if ( action == null )
		{
			return true;
		}

		int itemId = NemesisRequest.getItem( urlString );
		String itemName = ItemDatabase.getItemName( itemId );

		String message;

		if ( action.equals( "dodoor4" ) )
		{
			message = "Speaking password to door 4";
		}
		if ( action.equals( "sanctum" ) )
		{
			// Logged elsewhere
			return true;
		}
		else if ( itemId == -1 )
		{
			return true;
		}
		else if ( action.equals( "dodoor1" ) )
		{
			message = "Offering " + itemName + " to door 1";
		}
		else if ( action.equals( "dodoor2" ) )
		{
			message = "Offering " + itemName + " to door 2";
		}
		else if ( action.equals( "dodoor3" ) )
		{
			message = "Offering " + itemName + " to door 3";
		}
		else
		{
			return false;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
