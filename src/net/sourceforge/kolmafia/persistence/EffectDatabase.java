package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.textui.command.UseItemCommand;
import net.sourceforge.kolmafia.textui.command.UseSkillCommand;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EffectDatabase
{
	private static String [] canonicalNames = new String[0];
	private static final Map<Integer, String> nameById = new TreeMap<Integer, String>();
	private static final Map<String, int[]> effectIdSetByName = new TreeMap<String, int[]>();
	public static final HashMap<Integer, String> defaultActions = new HashMap<Integer, String>();

	private static final Map<Integer, String> imageById = new HashMap<Integer, String>();
	private static final Map<Integer, String> descriptionById = new TreeMap<Integer, String>();
	private static final Map<String, Integer> effectIdByDescription = new HashMap<String, Integer>();
	private static final Map<Integer, Integer> qualityById = new HashMap<>();
	private static final Map<Integer, List<String>> attributesById = new HashMap<>();

	public static boolean newEffects = false;

	public static final int GOOD = 0;
	public static final int NEUTRAL = 1;
	public static final int BAD = 2;

	static
	{
		EffectDatabase.reset();
	}

	public static void reset()
	{
		EffectDatabase.newEffects = false;

		BufferedReader reader =
			FileUtilities.getVersionedReader( "statuseffects.txt", KoLConstants.STATUSEFFECTS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length >= 3 )
			{
				Integer effectId = Integer.valueOf( data[ 0 ] );
				if ( effectId.intValue() < 0 )
				{
					continue;
				}

				String name = data[ 1 ];
				String image = data[ 2 ];
				String descId = data.length > 3 ? data[ 3 ] : null;
				String quality = data[4];
				String attributes = data[5];
				String defaultAction = data.length > 6 ? data[6] : null;

				EffectDatabase.addToDatabase( effectId, name, image, descId, quality, attributes, defaultAction );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		EffectDatabase.canonicalNames = new String[ EffectDatabase.effectIdSetByName.size() ];
		EffectDatabase.effectIdSetByName.keySet().toArray( EffectDatabase.canonicalNames );
	}

	private static void addIdToName( String canonicalName, int itemId )
	{
		int[] idSet = EffectDatabase.effectIdSetByName.get( canonicalName );
		int[] newSet;

		if ( idSet == null )
		{
			newSet = new int[1];
		}
		// *** This assumes the array is sorted
		else if ( Arrays.binarySearch( idSet, itemId ) >= 0 )
		{
			return;
		}
		else
		{
			newSet = Arrays.copyOf( idSet, idSet.length + 1 );
		}

		newSet[ newSet.length - 1 ] = itemId;
		// *** Make it so
		Arrays.sort( newSet );
		EffectDatabase.effectIdSetByName.put( canonicalName, newSet );
	}

	private static int parseQuality( final String quality)
 	{
		switch ( quality ) {
			case "good":
				return EffectDatabase.GOOD;
			case "bad":
				return EffectDatabase.BAD;
			default:
				return EffectDatabase.NEUTRAL;
		}
	}

	private static String describeQuality( final Integer quality )
	{
		switch ( quality ) {
			case EffectDatabase.GOOD:
				return "good";
			case EffectDatabase.BAD:
				return "bad";
			case EffectDatabase.NEUTRAL:
				return "neutral";
			default:
				return "";
 		}
	}

	private static void addToDatabase( final Integer effectId, final String name, final String image,
                                       final String descriptionId, final String quality, final String attributes, final String defaultAction)
	{
		String canonicalName = StringUtilities.getCanonicalName( name );
		EffectDatabase.nameById.put( effectId, name );
		EffectDatabase.addIdToName( canonicalName, effectId );
		EffectDatabase.imageById.put( effectId, image );

		if ( descriptionId != null && !descriptionId.equals( "" ) )
		{
			EffectDatabase.descriptionById.put( effectId, descriptionId );
			EffectDatabase.effectIdByDescription.put( descriptionId, effectId );
		}

		EffectDatabase.qualityById.put( effectId, EffectDatabase.parseQuality( quality ) );

		String[] list = attributes.split( "\\s*,\\s*" );
		List<String> attrs = new LinkedList<String>( Arrays.asList( list ) );
		attrs.remove( "none" );
		EffectDatabase.attributesById.put( effectId, attrs );

		if ( defaultAction != null )
		{
			EffectDatabase.defaultActions.put( effectId, defaultAction );
		}
	}

	public static final int getQuality( final int effectId )
	{
		Integer quality = ( effectId == -1 ) ? -1 : EffectDatabase.qualityById.get( effectId );
		return quality == null ? -1 : quality;
	}

	public static final String getQualityDescription( final int effectId )
 	{
		return EffectDatabase.describeQuality( EffectDatabase.getQuality( effectId ) );
	}

	public static final List<String> getEffectAttributes( final int effectId )
	{
		return EffectDatabase.attributesById.get( effectId );
	}

	public static final boolean hasAttribute( final String name, final String attribute )
	{
		int effectId = EffectDatabase.getEffectId( name );
		return ( effectId == -1 ) ? false : EffectDatabase.hasAttribute( effectId, attribute );
	}

	public static final boolean hasAttribute( final int effectId, final String attribute )
	{
		List<String> attrs = EffectDatabase.getEffectAttributes( effectId );
		return ( attrs == null ) ? false : attrs.contains( attribute );
	}

	public static final String getDefaultAction( final int effectId )
	{
		if ( effectId == -1 )
		{
			return null;
		}
		String rv = StringUtilities.getDisplayName( EffectDatabase.defaultActions.get( IntegerPool.get( effectId ) ) );
		if ( rv == null )
		{
			return null;
		}
		if ( rv.startsWith( "#" ) )
		{	// Callers of this API expect an actual command, not a note.
			return null;
		}
		for ( String it: rv.split( "\\|" ) )
		{
			String[] split = it.split( " ", 2 );
			boolean works = true; // assume the command works if we don't check it here

			if ( it.startsWith( "use" ) || it.startsWith( "eat" ) || it.startsWith( "drink" ) || it.startsWith( "chew" ) )
			{
				works = UseItemCommand.use( split[ 0 ], split[ 1 ], true );
			}
			else if ( it.startsWith( "cast" ) )
			{
				works = UseSkillCommand.cast( split[1], true );
			}

			if ( works )
			{
				return it;
			}
		}
		// if nothing worked, fall through and dispatch the command so that an appropriate failure can be printed
		return rv.split( "\\|" )[0];
	}

	public static final Iterator<String> getAllActions( final int effectId )
	{
		if ( effectId == -1 )
		{
			return Collections.emptyIterator();
		}
		String actions = StringUtilities.getDisplayName( EffectDatabase.defaultActions.get( IntegerPool.get( effectId ) ) );
		if ( actions == null )
		{
			return Collections.emptyIterator();
		}
		ArrayList<String> rv = new ArrayList<String>();
		String[] pieces = actions.split( "\\|" );
		for ( int i = 0; i < pieces.length; ++i )
		{
			String action = pieces[ i ];
			String[] either = action.split( " ", 3 );
			if ( either.length == 3 && either[ 1 ].equals( "either" ) )
			{	// Split commands like "use either X, Y" into "use X", "use Y"
				String cmd = either[ 0 ];
				either = either[ 2 ].split( "\\s*,\\s*" );
				for ( int j = 0; j < either.length; ++j )
				{
					rv.add( cmd + " " + either[ j ] );
				}
			}
			else
			{
				rv.add( action );
			}
		}

		return rv.iterator();
	}

	public static final String getActions( final int effectId )
	{
		return ( effectId == -1 ) ? null : EffectDatabase.getActions( IntegerPool.get( effectId ) );
	}

	public static final String getActions( final Integer effectId )
	{
		return EffectDatabase.defaultActions.get( effectId );
	}

	public static final void setActions( final int effectId, final String actions )
	{
		EffectDatabase.setActions( IntegerPool.get( effectId ), actions );
	}

	public static final void setActions( final Integer effectId, final String actions )
	{
		EffectDatabase.defaultActions.put( effectId, actions );
	}

	public static final String getActionNote( final int effectId )
	{
		if ( effectId == -1 )
		{
			return null;
		}
		String rv = StringUtilities.getDisplayName( EffectDatabase.defaultActions.get( IntegerPool.get( effectId ) ) );
		if ( rv != null && rv.startsWith( "#" ) )
		{
			return rv.substring( 1 ).trim();
		}
		return null;
	}

	/**
	 * Returns the name for an effect, given its Id.
	 *
	 * @param effectId The Id of the effect to lookup
	 * @return The name of the corresponding effect
	 */

	public static final String getEffectName( final int effectId )
	{
		return effectId == -1 ?
			null:
			EffectDatabase.nameById.get( IntegerPool.get( effectId ) );
	}

	public static final String getEffectName( final String descriptionId )
	{
		Integer effectId = EffectDatabase.effectIdByDescription.get( descriptionId );
		return effectId == null ? null : EffectDatabase.getEffectName( effectId.intValue() );
	}

	public static final String getEffectDisplayName( final String effectName )
	{
		if ( effectName.startsWith( "[" ) )
		{
			int ind = effectName.indexOf( "]" );
			if ( ind > 0 )
			{
				int effectId = StringUtilities.parseInt( effectName.substring( 1, ind ) );
				return getEffectName( effectId );
			}
		}
		return effectName;
	}

	public static final int getEffectIdFromDescription( final String descriptionId )
	{
		Integer effectId = EffectDatabase.effectIdByDescription.get( descriptionId );
		return effectId == null ? -1 : effectId.intValue();
	}

	public static final String getDescriptionId( final int effectId )
	{
		return EffectDatabase.descriptionById.get( IntegerPool.get( effectId ) );
	}

	static final Set<Integer> descriptionIdKeySet()
	{
		return EffectDatabase.descriptionById.keySet();
	}

	/**
	 * Returns the Id number for an effect, given its name.
	 *
	 * @param effectName The name of the effect to lookup
	 * @return The Id number of the corresponding effect
	 */

	public static final int getEffectId( final String effectName )
	{
		return EffectDatabase.getEffectId( effectName, false );
	}

	public static final int getEffectId( final String effectName, final boolean exact )
	{
		if ( effectName == null )
		{
			return -1;
		}

		// If name starts with [nnnn] then that is explicitly the effect id 
		if ( effectName.startsWith( "[" ) )
		{
			int index = effectName.indexOf( "]" );
			if ( index > 0 )
			{
				String idString = effectName.substring( 1, index );
				int effectId = -1;
				try 
				{
					effectId = StringUtilities.parseInt( idString );
				}
				catch (NumberFormatException e)
				{
				}
				return effectId;
			}
		}

		int[] ids = EffectDatabase.effectIdSetByName.get( StringUtilities.getCanonicalName( effectName ) );

		if ( ids != null )
		{
			if ( exact && ids.length > 1)
			{
				return -1;
			}
			return ids[ ids.length - 1 ];
		}

		if ( exact )
		{
			return -1;
		}

		List<String> names = EffectDatabase.getMatchingNames( effectName );
		if ( names.size() == 1 )
		{
			return EffectDatabase.getEffectId( names.get( 0 ), true );
		}

		return -1;
	}

	private static final int[] NO_EFFECT_IDS = new int[0];
	
	public static final int[] getEffectIds( final String effectName, final boolean exact )
	{
		if ( effectName == null )
		{
			return NO_EFFECT_IDS;
		}

		// If name starts with [nnnn] then that is explicitly the effect id 
		if ( effectName.startsWith( "[" ) )
		{
			int index = effectName.indexOf( "]" );
			if ( index > 0 )
			{
				String idString = effectName.substring( 1, index );
				int effectId = -1;
				try 
				{
					effectId = StringUtilities.parseInt( idString );
				}
				catch (NumberFormatException e)
				{
				}
				int[] ids = new int[1];
				ids[0] = effectId;
				return ids;
			}
		}

		int[] ids = EffectDatabase.effectIdSetByName.get( StringUtilities.getCanonicalName( effectName ) );

		if ( ids != null )
		{
			if ( exact && ids.length > 1)
			{
				return NO_EFFECT_IDS;
			}
			return ids;
		}

		if ( exact )
		{
			return NO_EFFECT_IDS;
		}

		List<String> names = EffectDatabase.getMatchingNames( effectName );
		if ( names.size() != 1 )
		{
			return NO_EFFECT_IDS;
		}

		ids = effectIdSetByName.get( StringUtilities.getCanonicalName( names.get( 0 ) ) );

		return ids != null ? ids : NO_EFFECT_IDS;
	}

	/**
	 * Returns the Id number for an effect, given its name.
	 *
	 * @param effectId The Id of the effect to lookup
	 * @return The name of the corresponding effect
	 */

	public static final String getImageName( final int effectId )
	{
		String imageName = effectId == -1 ? null : EffectDatabase.imageById.get( IntegerPool.get( effectId ) );
		return imageName == null ? "" : imageName;
	}

	public static final String getImage( final int effectId )
	{
		String imageName = EffectDatabase.getImageName( effectId );
		return  imageName.equals( "" ) ?
			"/images/debug.gif" :
			KoLmafia.imageServerPath() + "itemimages/" + imageName;
	}

	/**
	 * Returns the set of status effects keyed by Id
	 *
	 * @return The set of status effects keyed by Id
	 */

	public static final Set<Entry<Integer, String>> entrySet()
	{
		return EffectDatabase.nameById.entrySet();
	}

	public static final Collection<String> values()
	{
		return EffectDatabase.nameById.values();
	}

	public static final Set<Integer> keys()
	{
		return EffectDatabase.nameById.keySet();
	}

	/**
	 * Returns whether or not an effect with a given name exists in the database
	 *
	 * @param effectName The name of the effect to lookup
	 * @return <code>true</code> if the effect is in the database
	 */

	public static final boolean contains( final String effectName )
	{
		return EffectDatabase.contains( EffectDatabase.getEffectId( effectName ) );
	}

	public static final boolean containsExactly( final String effectName )
	{
		return EffectDatabase.contains( EffectDatabase.getEffectId( effectName, true ) );
	}

	public static final boolean contains( final int effectId )
	{
		if ( effectId == -1 )
		{
			return false;
		}
		return EffectDatabase.nameById.get( IntegerPool.get( effectId ) ) != null;
	}

	/**
	 * Returns a list of all items which contain the given substring. This is useful for people who are doing lookups on
	 * items.
	 */

	public static final List<String> getMatchingNames( final String substring )
	{
		// If name starts with [nnnn] then that is explicitly the effect id 
		if ( substring.startsWith( "[" ) )
		{
			int index = substring.indexOf( "]" );
			if ( index > 0 )
			{
				String idString = substring.substring( 1, index );
				try 
				{
					int effectId = StringUtilities.parseInt( idString );
					// It parsed to a number so is valid
					List<String> list = new ArrayList<String>();
					list.add( substring );
					return list;
				}
				catch (NumberFormatException e)
				{
				}
			}
		}
		return StringUtilities.getMatchingNames( EffectDatabase.canonicalNames, substring );
	}

	public static final int learnEffectId( String name, String descId )
	{
		return EffectDatabase.registerEffect( name, descId, null );
	}

	static final int registerEffect( String name, String descId, String defaultAction )
	{
		// Load the description text for this effect
		String text = DebugDatabase.readEffectDescriptionText( descId );
		if ( text == null )
		{
			return -1;
		}

		if ( name == null )
		{
			name = DebugDatabase.parseName( text );
		}

		int effectId = DebugDatabase.parseEffectId( text );
		if ( effectId == -1 )
		{
			return -1;
		}

		String image = DebugDatabase.parseImage( text );

		// Detach name, descid, and image from being substrings
		name = name;
		descId = descId;
		image = image;

		String canonicalName = StringUtilities.getCanonicalName( name );
		Integer id = IntegerPool.get( effectId );

		EffectDatabase.nameById.put( id, name );
		EffectDatabase.addIdToName( canonicalName, id );
		EffectDatabase.imageById.put( id, image );
		EffectDatabase.descriptionById.put( id, descId );
		EffectDatabase.qualityById.put( id, EffectDatabase.NEUTRAL );
		EffectDatabase.effectIdByDescription.put( descId, id );
		if ( defaultAction != null )
		{
			EffectDatabase.defaultActions.put( id, defaultAction );
		}

		String printMe;

		printMe = "--------------------";
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );

		printMe = EffectDatabase.effectString( effectId, name, image, descId, EffectDatabase.describeQuality( EffectDatabase.NEUTRAL ), "none", defaultAction );
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );

		// Let modifiers database do what it wishes with this effect
		Modifiers.registerEffect( name, text );

		// Done generating data
		printMe = "--------------------";
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );

		EffectDatabase.newEffects = true;

		return effectId;
	}

	public static final void writeEffects( final File output )
	{
		RequestLogger.printLine( "Writing data override: " + output );
		PrintStream writer = LogStream.openStream( output, true );
		writer.println( KoLConstants.STATUSEFFECTS_VERSION );

		int lastInteger = 1;

		for ( Entry<Integer, String> entry : EffectDatabase.nameById.entrySet() )
		{
			Integer nextInteger = entry.getKey();
			int effectId = nextInteger.intValue();

			// Skip pseudo effects
			if ( effectId < 1 )
			{
				continue;
			}

			for ( int i = lastInteger; i < nextInteger.intValue(); ++i )
			{
				writer.println( i );
			}

			lastInteger = effectId + 1;

			List<String> attributes = EffectDatabase.getEffectAttributes( nextInteger );

			String name = entry.getValue();
			String image = EffectDatabase.imageById.get( nextInteger );
			String descId = EffectDatabase.descriptionById.get( nextInteger );
			String quality = EffectDatabase.describeQuality( EffectDatabase.qualityById.get( nextInteger ) );
			String attributesString = ( attributes == null ) ? "none" : String.join( ",", attributes );

			String defaultAction = EffectDatabase.defaultActions.get( nextInteger );
			EffectDatabase.writeEffect( writer, effectId, name, image, descId, quality, attributesString, defaultAction );
		}

		writer.close();
	}

	private static void writeEffect( final PrintStream writer, final int effectId, final String name,
					final String image, final String descId, final String quality,
					final String attributes, final String defaultAction )
	{
		writer.println( EffectDatabase.effectString( effectId, name, image, descId, quality, attributes, defaultAction ) );
	}

	private static String effectString( final int effectId, final String name,
					   String image, String descId, final String quality,
					   final String attributes, final String defaultAction )
	{
		// The effect file can have 3, 4, or 5 fields. "image" must be
		// present, even if we don't have the actual file name.
		if ( image == null )
		{
			image = "";
		}

		if ( descId == null )
		{
			descId = "";
 		}
 
		String effectString = effectId + "\t" + name + "\t" + image + "\t" + descId + "\t" + quality + "\t" + attributes;

		if ( defaultAction != null )
		{
			effectString += "\t" + defaultAction;
 		}
 
		return effectString;
	}

	/**
	 * Utility method which determines the first effect which matches the given parameter string. Note that the string
	 * may also specify an effect duration before the string.
	 */

	public static final AdventureResult getFirstMatchingEffect( final String parameters )
	{
		return EffectDatabase.getFirstMatchingEffect( parameters, true );
	}

	public static final AdventureResult getFirstMatchingEffect( final String parameters, final boolean errorIfNone )
	{
		String effectName = null;
		int duration = 0;

		// First, allow for the person to type without specifying
		// the amount, if the amount is 1.

		List<String> matchingNames = EffectDatabase.getMatchingNames( parameters );

		if ( matchingNames.size() != 0 )
		{
			effectName = matchingNames.get( 0 );
			duration = 1;
		}
		else
		{
			String durationString = "";
			int spaceIndex = parameters.indexOf( " " );

			if ( spaceIndex != -1 )
			{
				durationString = parameters.substring( 0, spaceIndex );
			}

			if ( durationString.equals( "*" ) )
			{
				duration = 0;
			}
			else
			{
				if ( StringUtilities.isNumeric( durationString ) )
				{
					duration = StringUtilities.parseInt( durationString );
				}
				else
				{
					durationString = "";
					duration = 1;
				}
			}

			String effectNameString = parameters.substring( durationString.length() ).trim();

			matchingNames = EffectDatabase.getMatchingNames( effectNameString );

			if ( matchingNames.size() == 0 )
			{
				if ( errorIfNone )
				{
					KoLmafia.updateDisplay(
						MafiaState.ERROR,
						"[" + effectNameString + "] does not match anything in the status effect database." );
				}

				return null;
			}

			effectName = matchingNames.get( 0 );
		}

		if ( effectName == null )
		{
			KoLmafia.updateDisplay(
				MafiaState.ERROR, "[" + parameters + "] does not match anything in the status effect database." );
			return null;
		}

		int effectId = EffectDatabase.getEffectId( effectName );
		return EffectPool.get( effectId, duration );
	}

	public static final int[] POISON_ID = {
   		0,
   		EffectPool.TOAD_IN_THE_HOLE,
   		EffectPool.MAJORLY_POISONED,
   		EffectPool.REALLY_QUITE_POISONED,
   		EffectPool.SOMEWHAT_POISONED,
   		EffectPool.A_LITTLE_BIT_POISONED,
   		EffectPool.HARDLY_POISONED
   	};

	public static int getPoisonLevel( String text )
	{
		text = text.toLowerCase();
		if ( text.contains( "toad in the hole" ) )
		{
			return 1;
		}
		if ( !text.contains( "poisoned" ) )
		{
			return Integer.MAX_VALUE;
		}
		if ( text.contains( "majorly poisoned" ) )
		{
			return 2;
		}
		if ( text.contains( "really quite poisoned" ) )
		{
			return 3;
		}
		if ( text.contains( "somewhat poisoned" ) )
		{
			return 4;
		}
		if ( text.contains( "a little bit poisoned" ) )
		{
			return 5;
		}
		if ( text.contains( "hardly poisoned at all" ) )
		{
			return 6;
		}
		return Integer.MAX_VALUE;
	}
}
