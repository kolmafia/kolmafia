package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.BeachCombRequest;
import net.sourceforge.kolmafia.request.BeachCombRequest.BeachCombCommand;
import net.sourceforge.kolmafia.request.BeachCombRequest.Coords;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BeachManager
{
	// main.php?comb=1
	// choice.php?forceoption=0
	// choice.php?whichchoice=1388&pwd&option=1&minutes=420
	// choice.php?whichchoice=1388&pwd&option=3&buff=3
	// choice.php?whichchoice=1388&pwd&option=4&coords=8%2C4197
	// choice.php?whichchoice=1388&pwd&option=5

	// You walk for 420 minutes and find a nice stretch of beach.  Now...  Where to comb?
	private static final Pattern MINUTES_PATTERN = Pattern.compile( "You walk for ([\\d,]+) minutes? and find a nice stretch of beach" );

	// (You have 11 free walks down the beach left today.)
	private static final Pattern FREE_WALK_PATTERN = Pattern.compile( "\\(You have ([\\d]+) free walks? down the beach left today.\\)" );

	// Visit Beach Head #10
	private static final Pattern BEACH_HEAD_PATTERN = Pattern.compile( "Visit Beach Head #([\\d]+)" );

	// You acquire an effect: <b>A Brush with Grossness</b>
	private static final Pattern EFFECT_PATTERN = Pattern.compile( "You acquire an effect: <b>([^<]+)</b>" );

	public static class BeachHead
	{
		public final int id;
		public final String effect;
		public final int beach;
		public final String coords;
		public final String desc;

		public BeachHead( int id, String effect, int beach, String coords, String desc )
		{
			this.id = id;
			this.effect = effect;
			this.beach = beach;
			this.coords = coords;
			this.desc = desc;
		}
	}

	public static final BeachHead [] BEACH_HEADS = 
	{
		new BeachHead( 1, "Hot-Headed", 420, "8,4197", "hot" ),
		new BeachHead( 2, "Cold as Nice", 2323, "8,23222", "cold" ),
		new BeachHead( 3, "A Brush with Grossness", 4242, "8,42412", "stench" ),
		new BeachHead( 4, "Does It Have a Skull In There??", 6969, "8,69682",  "spooky" ),
		new BeachHead( 5, "Oiled, Slick", 8888, "8,88879", "sleaze" ),
		new BeachHead( 6, "Lack of Body-Building", 37, "8,368", "muscle" ),
		new BeachHead( 7, "We're All Made of Starfish", 3737, "8,37368", "mysticality" ),
		new BeachHead( 8, "Pomp & Circumsands", 7114, "8,71138", "moxie" ),
		new BeachHead( 9, "Resting Beach Face", 5555, "9,55549", "initiative" ),
		new BeachHead( 10, "Do I Know You From Somewhere?", 1111, "9,11109", "familiar" ),
		new BeachHead( 11, "You Learned Something Maybe!", 9696, "9,96958", "experience" ),
	};

	public static final Map<Integer, BeachHead> idToBeachHead = new TreeMap<Integer, BeachHead>();
	public static final Map<String, BeachHead> effectToBeachHead = new TreeMap<String, BeachHead>();
	public static final List<String> beachHeadDescs = new ArrayList<String>();
	public static final String[] beachHeadDescArray;
	public static final Map<String, BeachHead> descToBeachHead = new TreeMap<String, BeachHead>();

	static
	{
		for ( BeachHead head : BEACH_HEADS )
		{
			idToBeachHead.put( head.id, head );
			effectToBeachHead.put( head.effect, head );
			beachHeadDescs.add( head.desc );
			descToBeachHead.put( head.desc, head );
		}
		beachHeadDescArray = beachHeadDescs.toArray( new String[ beachHeadDescs.size() ] );
	}

    public static Set<Integer> getBeachHeadPreference( String property )
	{
		Set<Integer> beachHeads = new TreeSet<Integer>();
		for ( String iword : Preferences.getString( property ).split( " *, *" ) )
		{
			if ( !iword.equals( "" ) )
			{
				beachHeads.add( IntegerPool.get( StringUtilities.parseInt( iword ) ) );
			}
		}
		return beachHeads;
	}

	public static String setBeachHeadPreference( String property, Set<Integer> beachHeads )
	{
		StringBuilder buf = new StringBuilder();
		for ( Integer id : beachHeads )
		{
			if ( buf.length() > 0 )
			{
				buf.append( "," );
			}
			buf.append( id );
		}
		String value = buf.toString();
		Preferences.setString( property, value );
		return value;
	}

	public static Map<Integer, String> stringToLayout( final String input )
	{
		Map<Integer, String> rowMap = new TreeMap<Integer, String>();
		for ( String rowData : input.split( "," ) )
		{
			int colon = rowData.indexOf( ":" );
			if ( colon != -1 )
			{
				int row = StringUtilities.parseInt( rowData.substring( 0, colon ) );
				String squares = rowData.substring( colon + 1 );
				rowMap.put( row, squares );
			}
		}

		return rowMap;
	}

	public static Map<Integer, String> getBeachLayout()
	{
		return BeachManager.stringToLayout( Preferences.getString( "_beachLayout" ) );
	}

	public static final String layoutToString( final Map<Integer, String> input )
	{
		StringBuilder value = new StringBuilder();

		for ( Entry<Integer, String> entry : input.entrySet() )
		{
			int row = entry.getKey();
			String cols = entry.getValue();
			if ( value.length() > 0 )
			{
				value.append( "," );
			}
			value.append( row );
			value.append( ':' );
			value.append( cols );
		}

		return value.toString();
	}

	// Choice when using the Beach Comb or after combing.
	// If you have adventures left, you are still using the comb.
	// Otherwise, you are not.
	public static final boolean parseCombUsage( final String urlString, final String text )
	{
		// If we actually combed the beach, update the beach layout
		BeachCombCommand command = BeachCombRequest.extractCommandFromURL( urlString );
		if ( command == BeachCombCommand.COMB && text.contains( "You acquire" ) )
		{
			Coords coords = new Coords( urlString );

			// Parse the _beachLayout property
			Map<Integer,String> layout = BeachManager.stringToLayout( Preferences.getString( "_beachLayout" ) );

			// Replace the combed square with 'c'
			String squares = layout.get( coords.row );
			if ( squares != null )
			{
				int col = coords.col;
				StringBuilder modified = new StringBuilder();
				if ( col > 0 ) {
					modified.append( squares, 0, col );
				}
				modified.append( 'c' );
				if ( col < squares.length() - 1 ) {
					modified.append( squares.substring( col + 1 ) );
				}

				layout.put( coords.row, modified.toString() );
			}

			// Replace the setting
			String value = BeachManager.layoutToString( layout );
			Preferences.setString( "_beachLayout", value );
		}
		return BeachManager.parseCombUsage( text );
	}

	public static final boolean parseCombUsage( final String text )
	{
		// You grab your comb and head to the start of the beach to find a good spot.
		// You grab your comb and head back to the start of the beach to find another good spot.
		if ( !text.contains( "to the start of the beach to find" ) )
		{
			return false;
		}

		// <span class='guts'>You comb the area and under the sand you find a bottle. It looks like it contains some sort of message? You pop the bottle open and look at the piece of paper inside. It says:<br><br>LIFE ON A DESSERT ISLAND -- SHOULD BE HARD, BUT REALLY IT IS A PIECE OF CAKE<br><br>Is that some sort of joke?</span>

		Matcher matcher = BeachManager.FREE_WALK_PATTERN.matcher( text );
		int walksAvailable = matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
		Preferences.setInteger( "_freeBeachWalksUsed", 11 - walksAvailable );

		// Parse the beach head shortcuts and see what we can deduce about them.

		// Start by retrieving what we know are unlocked
		Set<Integer> unlocked = getBeachHeadPreference( "beachHeadsUnlocked" );

		// Find which beach heads have available shortcuts
		Set<Integer> available = new TreeSet<Integer>();

		matcher = BeachManager.BEACH_HEAD_PATTERN.matcher( text );
		while ( matcher.find() )
		{
			available.add( StringUtilities.parseInt( matcher.group( 1 ) ) );
		}

		// All visible beach head shortcuts are unlocked
		Set<Integer> allUnlocked = new TreeSet<Integer>( unlocked );
		allUnlocked.addAll( available );
		setBeachHeadPreference( "beachHeadsUnlocked", allUnlocked );

		// All visible beach head shortcuts have not been visited today
		Set<Integer> visited = new TreeSet<Integer>( allUnlocked );
		visited.removeAll( available );
		setBeachHeadPreference( "_beachHeadsUsed", visited );

		return true;
	}

	// Choice when using the Beach Comb or after combing, if you have adventures left
	public static final boolean parseBeachHeadCombing( final String text )
	{
		// You return to the beach head and comb it once again, still trying to
		// not think too hard about what it <i>is</i>. It gives you some kind
		// of magical blessing as a tip.
		//
		// You already combed that head today.
		if ( !text.contains( "some kind of magical blessing" ) )
		{
			return false;
		}

		// You acquire an effect: <b>A Brush with Grossness</b>
		Matcher matcher = BeachManager.EFFECT_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return false;
		}

		BeachHead beachHead = effectToBeachHead.get( matcher.group( 1 ) );
		if ( beachHead == null )
		{
			return false;
		}

		Integer id = IntegerPool.get( beachHead.id );

		// Add this beach head to set of unlocked (if not already present)
		Set<Integer> unlocked = getBeachHeadPreference( "beachHeadsUnlocked" );
		if ( !unlocked.contains( id ) )
		{
			unlocked.add( id );
			setBeachHeadPreference( "beachHeadsUnlocked", unlocked );
		}

		// Add this beach head to set of visited (if not already
		// present - which might be true if we parsed the Beach Comb first)
		Set<Integer> visited = getBeachHeadPreference( "_beachHeadsUsed" );
		if ( !visited.contains( id ) )
		{
			visited.add( id );
			setBeachHeadPreference( "_beachHeadsUsed", visited );
		}

		return true;
	}

	// Beach Layout:
	//
	//     (XXXX minutes down the beach)
	//              columns
	//     0  1  2  3  4  5  6  7  8  9
	// 10  x  x  x  x  x  x  x  x  x  x
	//  9  x  x  x  x  x  x  x  x  x  x
	//  8  x  x  x  x  x  x  x  x  x  x
	//  7  x  x  x  x  x  x  x  x  x  x
	//  6  x  x  x  x  x  x  x  x  x  x
	//  5  x  x  x  x  x  x  x  x  x  x
	//  4  x  x  x  x  x  x  x  x  x  x
	//  3  x  x  x  x  x  x  x  x  x  x
	//  2  x  x  x  x  x  x  x  x  x  x
	// (1) wave washed squares
	//
	// Coordinates: <row>,(minutes*10-column)
	//
	// The number of rows on the beach changes from day to day. Tides?

	private static final Pattern MAP_PATTERN = Pattern.compile( "name=\"coords\" value=\"([\\d]+),([\\d]+)\".*?title=\"([^\"]*).*?otherimages/beachcomb/(.*?).(gif|png)", Pattern.DOTALL );

	// Known "title" names
	//
	// rough sand with a twinkle
	// rough sand
	// combed sand
	// a beach head
	// a sand castle
	//
	// There are others. whale.png is ... what? "a beached whale"?

	// Settings to hold current map:
	//
	// _beachCombing	Actually on the beach
	// _beachMinutes	Minutes of wandering
	// _beachLayout		What's there
	//
	// Unspaded, as far as I know, whether what's at a particular coordinate changes from day to day.
	// "rough sand" can change to "combed sand" via actions of other players.
	// Presumably all "combed sand" periodically becomes "rough sand" (at rollover?)
	// Presumably, "beach head" squares do not change
	//
	// So, we'll use "_" preferences

	public static final void parseBeachMap( final String text )
	{
		Matcher matcher = BeachManager.MINUTES_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			Preferences.setBoolean( "_beachCombing", false );
			return;
		}

		int minutes = StringUtilities.parseInt( matcher.group(1) );

		StringBuilder layout = new StringBuilder();

		// Since number of rows vary, make a map to hold the layout for each row
		Map<Integer, String> rowLayout = new TreeMap<Integer, String>();
		int currentRow = -1;

		matcher = BeachManager.MAP_PATTERN.matcher( text );
		while ( matcher.find() )
		{
			int row = StringUtilities.parseInt( matcher.group(1) );
			int col = StringUtilities.parseInt( matcher.group(2) );
			String type = matcher.group(3);
			String image = matcher.group(4);

			if ( row != currentRow )
			{
				if ( currentRow != -1 )
				{
					rowLayout.put( currentRow, layout.toString() );
				}
				layout.setLength( 0 );
				currentRow = row;
			}

			char ch =
				type.equals( "rough sand with a twinkle" ) ? 't' :
				type.equals( "rough sand" ) ? 'r' :
				type.equals( "combed sand" ) ? 'c' :
				type.equals( "a beach head" ) ? 'H' :
				type.equals( "a sand castle" ) ? 'C' :
				image.equals( "whale" ) ?  'W':
				'?';

			if ( ch == '?' )
			{
				logText( "Unknown beach square at " + minutes + ":" + row + "," + col + ": text = '" + text + "' image = '" + image + "'." );
			}

			layout.append( ch );
		}

		if ( currentRow != -1 )
		{
			rowLayout.put( currentRow, layout.toString() );
		}

		String value = BeachManager.layoutToString( rowLayout );

		Preferences.setBoolean( "_beachCombing", true );
		Preferences.setInteger( "_beachMinutes", minutes );
		Preferences.setString( "_beachLayout", value );
	}

	private static void logText( final String text )
	{
		RequestLogger.printLine( text );
		RequestLogger.updateSessionLog( text );
	}
}
