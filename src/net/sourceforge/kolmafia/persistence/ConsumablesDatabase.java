package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.PrintStream;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.request.UseSkillRequest;


import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class ConsumablesDatabase
{
	public static final AdventureResult ODE = EffectPool.get( EffectPool.ODE );
	public static final AdventureResult MILK = EffectPool.get( EffectPool.MILK );
	public static final AdventureResult GLORIOUS_LUNCH = EffectPool.get( EffectPool.GLORIOUS_LUNCH );
	public static final AdventureResult BARREL_OF_LAUGHS = EffectPool.get( EffectPool.BARREL_OF_LAUGHS );
	public static final AdventureResult BEER_BARREL_POLKA = EffectPool.get( EffectPool.BEER_BARREL_POLKA );
	public static final AdventureResult RECORD_HUNGER = EffectPool.get( EffectPool.RECORD_HUNGER );
	public static final AdventureResult DRUNK_AVUNCULAR = EffectPool.get( EffectPool.DRUNK_AVUNCULAR );
	public static final AdventureResult REFINED_PALATE = EffectPool.get( EffectPool.REFINED_PALATE );

	private static final Map<String, Integer> levelReqByName = new HashMap<String, Integer>();
	public static final Map<String, Integer> fullnessByName = new TreeMap<String, Integer>( KoLConstants.ignoreCaseComparator );
	public static final Map<String, Integer> inebrietyByName = new TreeMap<String, Integer>( KoLConstants.ignoreCaseComparator );
	public static final Map<String, Integer> spleenHitByName = new TreeMap<String, Integer>( KoLConstants.ignoreCaseComparator );
	private static final Map<String, String> qualityByName = new HashMap<String, String>();
	private static final Map<String, String> notesByName = new HashMap<String, String>();

	private static final Map<String, Double>[][][][][] advsByName = new HashMap[ 2 ][ 2 ][ 2 ][ 2 ][ 2 ];
	private static final Map<String, String> advRangeByName = new HashMap<String, String>();
	private static final Map<String, Integer> unitCostByName = new HashMap<String, Integer>();
	private static final Map<String, Integer> advStartByName = new HashMap<String, Integer>();
	private static final Map<String, Integer> advEndByName = new HashMap<String, Integer>();

	private static Set<String> advNames = null;

	public static final String NONE = "";
	public static final String CRAPPY = "crappy";
	public static final String DECENT = "decent";
	public static final String GOOD = "good";
	public static final String AWESOME = "awesome";
	public static final String EPIC = "EPIC";

	static
	{
		for ( int i1 = 0; i1 <= 1; ++i1 )
		{
			for ( int i2 = 0; i2 <= 1; ++i2 )
			{
				for ( int i3 = 0; i3 <= 1; ++i3 )
				{
					for ( int i4 = 0; i4 <= 1; ++i4 )
					{
						for ( int i5 = 0; i5 <= 1; ++i5 )
						{
							ConsumablesDatabase.advsByName[ i1 ][ i2 ][ i3 ][ i4 ][ i5 ] = new HashMap<>();
						}
					}
				}
			}
		}
	}

	public static Object[][] DUSTY_BOTTLES =
	{
		{ IntegerPool.get( ItemPool.DUSTY_BOTTLE_OF_MERLOT ),
		  "dusty bottle of Merlot",
		  "dusty bottle of average Merlot",
		},
		{ IntegerPool.get( ItemPool.DUSTY_BOTTLE_OF_PORT ),
		  "dusty bottle of Port",
		  "dusty bottle of vinegar Port",
		},
		{ IntegerPool.get( ItemPool.DUSTY_BOTTLE_OF_PINOT_NOIR ),
		  "dusty bottle of Pinot Noir",
		  "dusty bottle of spooky Pinot Noir",
		},
		{ IntegerPool.get( ItemPool.DUSTY_BOTTLE_OF_ZINFANDEL ),
		  "dusty bottle of Zinfandel",
		  "dusty bottle of great Zinfandel",
		},
		{ IntegerPool.get( ItemPool.DUSTY_BOTTLE_OF_MARSALA ),
		  "dusty bottle of Marsala",
		  "dusty bottle of glassy Marsala",
		},
		{ IntegerPool.get( ItemPool.DUSTY_BOTTLE_OF_MUSCAT ),
		  "dusty bottle of Muscat",
		  "dusty bottle of bad Muscat",
		}
	};

	private static final Map<String, String> muscleByName = new HashMap<String, String>();
	private static final Map<String, String> mysticalityByName = new HashMap<String, String>();
	private static final Map<String, String> moxieByName = new HashMap<String, String>();

	public static void reset()
	{
		ConsumablesDatabase.readConsumptionData( "fullness.txt", KoLConstants.FULLNESS_VERSION, ConsumablesDatabase.fullnessByName );
		ConsumablesDatabase.readConsumptionData( "inebriety.txt", KoLConstants.INEBRIETY_VERSION, ConsumablesDatabase.inebrietyByName );
		ConsumablesDatabase.readConsumptionData( "spleenhit.txt", KoLConstants.SPLEENHIT_VERSION , ConsumablesDatabase.spleenHitByName );
		ConsumablesDatabase.readNonfillingData();
	}

	static
	{
		ConsumablesDatabase.reset();
	}

	public static void writeConsumable( final PrintStream writer, final String name, final int size,
					    final int level, final String quality, final String adv,
					    final String mus, final String mys, final String mox,
					    final String notes )
	{
		writer.println( ConsumablesDatabase.consumableString( name, size, level, quality, adv, mus, mys, mox, notes ) );
	}

	public static String consumableString( final String name, final int size,
					       final int level, final String quality, final String adv,
					       final String mus, final String mys, final String mox,
					       final String notes )
	{
		return name +
			"\t" + size +
			"\t" + level +
			"\t" + quality +
			"\t" + adv +
			"\t" + mus +
			"\t" + mys +
			"\t" + mox +
			( notes == null ? "" : ("\t" + notes ) );
	}

	private static void readConsumptionData( String filename, int version, Map<String, Integer> map )
	{
		map.clear();

		BufferedReader reader = FileUtilities.getVersionedReader( filename, version );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			ConsumablesDatabase.saveConsumptionValues( data, map );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static void setConsumptionData( final String name, final int size, final String adventures,
                                            final String muscle, final String mysticality, final String moxie, final String note )
	{
		ConsumablesDatabase.setConsumptionData( name, size, 1, "", adventures, muscle, mysticality, moxie, note );
	}

	private static void setConsumptionData( final String name, final int size, final int level, final String quality,
                                            final String adventures, final String muscle, final String mysticality, final String moxie, final String note )
	{
		ConsumablesDatabase.levelReqByName.put( name, level );
		ConsumablesDatabase.qualityByName.put( name, ConsumablesDatabase.qualityValue( quality ) );
		ConsumablesDatabase.saveAdventureRange( name, size, adventures );
		ConsumablesDatabase.calculateAdventureRange( name );
		ConsumablesDatabase.muscleByName.put( name, muscle );
		ConsumablesDatabase.mysticalityByName.put( name, mysticality );
		ConsumablesDatabase.moxieByName.put( name, moxie );
		if ( note != null && note.length() > 0 )
		{
			ConsumablesDatabase.notesByName.put( name, note );
		}
		int itemId = ItemDatabase.getItemId( name, 1, false );
		Concoction c = ConcoctionPool.get( itemId, name );
		if ( c != null )
		{
			c.setConsumptionData();
		}
	}

	public static final void cloneConsumptionData( final String name, final String alias )
	{
		int size = 0;

		Integer fullness = ConsumablesDatabase.getRawFullness( name );
		if ( fullness != null )
		{
			ConsumablesDatabase.fullnessByName.put( alias, fullness );
			size = fullness.intValue();
		}

		Integer inebriety = ConsumablesDatabase.getRawInebriety( name );
		if ( inebriety != null )
		{
			ConsumablesDatabase.inebrietyByName.put( alias, inebriety );
			size = inebriety.intValue();
		}

		Integer spleenhit = ConsumablesDatabase.getRawSpleenHit( name );
		if ( spleenhit != null )
		{
			ConsumablesDatabase.spleenHitByName.put( alias, spleenhit );
			size = spleenhit.intValue();
		}

		if ( size == 0 )
		{
			return;
		}

		Integer level = ConsumablesDatabase.levelReqByName.get( name );
		String quality = ConsumablesDatabase.qualityByName.get( name );
		String adventures = ConsumablesDatabase.getAdvRangeByName( name );
		String muscle = ConsumablesDatabase.muscleByName.get( name );
		String mysticality = ConsumablesDatabase.mysticalityByName.get( name );
		String moxie = ConsumablesDatabase.moxieByName.get( name );
		String note = ConsumablesDatabase.notesByName.get( name );

		ConsumablesDatabase.setConsumptionData( alias, size, level == null ? 1 : level.intValue(), quality, adventures, muscle, mysticality, moxie, note );
	}

	private static void readNonfillingData()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "nonfilling.txt", KoLConstants.NONFILLING_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 2 )
				continue;

			String name = data[ 0 ];
			ConsumablesDatabase.levelReqByName.put( name, Integer.valueOf( data[ 1 ] ) );

			if ( data.length < 3 )
				continue;

			String notes = data[ 2 ];
			if ( notes.length() > 0 )
			{
				ConsumablesDatabase.notesByName.put( name, notes );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	private static void saveConsumptionValues( String[] data, Map<String, Integer> map )
	{
		if ( data.length < 2 )
		{
			return;
		}

		String name = data[ 0 ];
		map.put( name, Integer.valueOf( data[ 1 ] ) );

		if ( data.length < 8 )
		{
			return;
		}

		String holiday = HolidayDatabase.getHoliday();
		boolean isBorisDay = ( holiday.contains( "Feast of Boris" ) || holiday.contains( "Drunksgiving" ) );

		ConsumablesDatabase.levelReqByName.put( name, Integer.valueOf( data[ 2 ] ) );

		// Some items different on Feast of Boris
		if ( !isBorisDay )
		{
			ConsumablesDatabase.qualityByName.put( name, ConsumablesDatabase.qualityValue( data[ 3 ] ) );
			ConsumablesDatabase.saveAdventureRange( name, StringUtilities.parseInt( data[ 1 ] ), data[ 4 ] );
		}
		else if( name.equals( "cranberries" ) )
		{
			ConsumablesDatabase.qualityByName.put( name, ConsumablesDatabase.qualityValue( "good" ) );
			ConsumablesDatabase.saveAdventureRange( name, StringUtilities.parseInt( data[ 1 ] ), "2-4" );
		}
		else if( name.equals( "redrum" ) )
		{
			ConsumablesDatabase.qualityByName.put( name, ConsumablesDatabase.qualityValue( "good" ) );
			ConsumablesDatabase.saveAdventureRange( name, StringUtilities.parseInt( data[ 1 ] ), "5-9" );
		}
		else if( name.equals( "vodka and cranberries" ) )
		{
			ConsumablesDatabase.qualityByName.put( name, ConsumablesDatabase.qualityValue( "good" ) );
			ConsumablesDatabase.saveAdventureRange( name, StringUtilities.parseInt( data[ 1 ] ), "6-9" );
		}
		else
		{
			ConsumablesDatabase.qualityByName.put( name, ConsumablesDatabase.qualityValue( data[ 3 ] ) );
			ConsumablesDatabase.saveAdventureRange( name, StringUtilities.parseInt( data[ 1 ] ), data[ 4 ] );
		}

		ConsumablesDatabase.muscleByName.put( name, data[ 5 ] );
		ConsumablesDatabase.mysticalityByName.put( name, data[ 6 ] );
		ConsumablesDatabase.moxieByName.put( name, data[ 7 ] );

		// When we reset consumption data, we must reset Concoctions
		ConsumablesDatabase.calculateAdventureRange( name );
		int itemId = ItemDatabase.getItemId( name, 1, false );
		Concoction c = ConcoctionPool.get( itemId, name );
		if ( c != null )
		{
			c.setConsumptionData();
		}

		if ( data.length < 9 )
			return;

		String notes = data[ 8 ];
		if ( notes.length() > 0 )
		{
			ConsumablesDatabase.notesByName.put( name, notes );
		}
	}

	public static final String qualityValue( String value )
	{
		// Reduce string allocations...
		return  value == null ? ConsumablesDatabase.NONE :
			value.equals( "crappy" ) ? ConsumablesDatabase.CRAPPY :
			value.equals( "decent" ) ? ConsumablesDatabase.DECENT :
			value.equals( "good" ) ? ConsumablesDatabase.GOOD :
			value.equals( "awesome" ) ? ConsumablesDatabase.AWESOME :
			value.equals( "EPIC" ) ? ConsumablesDatabase.EPIC :
			ConsumablesDatabase.NONE;
	}

	private static void saveAdventureRange( final String name, final int unitCost, String range )
	{
		range = range.trim();

		int dashIndex = range.indexOf( "-" );
		int start = StringUtilities.parseInt( dashIndex == -1 ? range : range.substring( 0, dashIndex ) );
		int end = dashIndex == -1 ? start : StringUtilities.parseInt( range.substring( dashIndex + 1 ) );
		ConsumablesDatabase.advRangeByName.put( name, range );
		ConsumablesDatabase.unitCostByName.put( name, IntegerPool.get( unitCost ) );
		ConsumablesDatabase.advStartByName.put( name, IntegerPool.get( start ) );
		ConsumablesDatabase.advEndByName.put( name, IntegerPool.get( end ) );
		ConsumablesDatabase.advNames = null;
	}

	public static final String getAdvRangeByName( final String name )
	{
		if ( name == null )
		{
			return "";
		}

		String range = ConsumablesDatabase.advRangeByName.get( name );
		if ( range == null )
		{
			return "";
		}
		if ( KoLCharacter.inSlowcore() )
		{
			return "0";
		}
		if ( KoLCharacter.inNuclearAutumn() )
		{
			//int dashIndex = range.indexOf( "-" );
			//int start = StringUtilities.parseInt( dashIndex == -1 ? range : range.substring( 0, dashIndex ) );
			//int end = dashIndex == -1 ? start : StringUtilities.parseInt( range.substring( dashIndex + 1 ) );
		}
		return range;
	}

	public static final void calculateAdventureRanges()
	{
		if ( ConsumablesDatabase.advNames == null )
		{
			ConsumablesDatabase.advNames = ConsumablesDatabase.unitCostByName.keySet();
		}

		for ( String name : ConsumablesDatabase.advNames )
		{
			ConsumablesDatabase.calculateAdventureRange( name );
		}
	}

	private static void calculateAdventureRange( final String name )
	{
		ConsumablesDatabase.calculateAdventureRange( name, EquipmentDatabase.getItemType( ItemDatabase.getItemId( name ) ) );
	}

	private static void calculateAdventureRange( final String name, final String type )
	{
		int itemId = ItemDatabase.getItemId( name, 1, false );
		Concoction c = ConcoctionPool.get( itemId, name );
		int advs = ( c == null ) ? 0 : c.getAdventuresNeeded( 1, true );

		int unitCost = ConsumablesDatabase.unitCostByName.get( name ).intValue();
		int start = ConsumablesDatabase.advStartByName.get( name ).intValue();
		int end = ConsumablesDatabase.advEndByName.get( name ).intValue();

		if ( KoLCharacter.inNuclearAutumn() )
		{
			if ( type.equals( "food" )  )
			{
				int multiplier = 1;
				if ( KoLCharacter.hasSkill( "Extra Gall Bladder" ) ) multiplier += 1;
				if ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.RECORD_HUNGER ) ) ) multiplier += 1;
				start *= multiplier;
				end *= multiplier;
			}
			//&& KoLCharacter.hasSkill( "Extra Kidney" )
			else if ( type.equals( "booze" ) )
			{
				int multiplier = 1;
				if ( KoLCharacter.hasSkill( "Extra Kidney" ) ) multiplier += 1;
				if ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.DRUNK_AVUNCULAR ) ) ) multiplier += 1;
				start *= multiplier;
				end *= multiplier;
			}
		}

		// Adventure gain modifier #1 is ode or milk, which adds
		// unitCost adventures to the result.

		// Adventure gain modifier #2 is Song of the Glorious Lunch, Rowdy Drinker, 
		// Barrel of Laughs or Beer Barrel Polka, which add
		// unitCost adventures to the result.

		// Adventure gain modifier #3 is Gourmand or Neurogourmet, which adds
		// unitCost adventures to the result.

		// Adventure gain modifier #4 is the munchies pill, which adds
		// 1-3 adventures

		// Consumables that generate no adventures do not benefit from ode or milk.
		double average = ( start + end ) / 2.0 - advs;
		boolean benefit = ( average != 0.0 );

		double gain0 = benefit ? ( average ) : 0.0;
		double gain1 = benefit ? ( average + unitCost ) : 0.0;
		double gain2 = benefit ? ( average + unitCost * 2.0 ) : 0.0;

		// With no effects active, average
		ConsumablesDatabase.addAdventureRange( name, unitCost, false, false, false, false, gain0 );

		// With only one effect, average + unitCost
		ConsumablesDatabase.addAdventureRange( name, unitCost, true, false, false, false, gain1 );
		ConsumablesDatabase.addAdventureRange( name, unitCost, false, true, false, false, gain1 );

		// With two effects, average + unitCost * 2
		ConsumablesDatabase.addAdventureRange( name, unitCost, true, true, false, false, gain2 );

		// Only foods have effects 3-4
		if ( !type.equals( "food" ) )
		{
			return;
		}

		// calculate munchies pill effect
		double munchieBonus;
		if ( end <= 3 )
		{
			munchieBonus = 3.0;
		}
		else if (start >= 7 )
		{
			munchieBonus = 1.0;
		}
		else
		{
			int munchieTotal = 0;
			for( int i = start; i <= end ; i++ )
			{
				munchieTotal += Math.max( (12-i)/3, 1 );
			}
			munchieBonus = (double) munchieTotal / ( end-start + 1 );
		}

		double gain3 = benefit ? ( average + unitCost * 3.0 ) : 0.0;
		double gain0a = benefit ? ( average + munchieBonus ) : 0.0;
		double gain1a = benefit ? ( average + unitCost + munchieBonus ) : 0.0;
		double gain2a = benefit ? ( average + unitCost * 2.0 + munchieBonus ) : 0.0;
		double gain3a = benefit ? ( average + unitCost * 3.0 + munchieBonus ) : 0.0;

		ConsumablesDatabase.addAdventureRange( name, unitCost, false, true, false, false, gain1 );
		ConsumablesDatabase.addAdventureRange( name, unitCost, false, false, true, false, gain1 );

		// With two effects, average + unitCost * 2
		ConsumablesDatabase.addAdventureRange( name, unitCost, true, false, true, false, gain2 );
		ConsumablesDatabase.addAdventureRange( name, unitCost, false, true, true, false, gain2 );

		// With three effects, average + unitCost * 3
		ConsumablesDatabase.addAdventureRange( name, unitCost, true, true, true, false, gain3 );

		// With only munchies pill, average + 2
		ConsumablesDatabase.addAdventureRange( name, unitCost, false, false, false, true, gain0a );

		// With one effect and munchies pill, average + unitCost + 2
		ConsumablesDatabase.addAdventureRange( name, unitCost, true, false, false, true, gain1a );
		ConsumablesDatabase.addAdventureRange( name, unitCost, false, true, false, true, gain1a );
		ConsumablesDatabase.addAdventureRange( name, unitCost, false, false, true, true, gain1a );

		// With two effects and munchies pill, average + unitCost * 2 + 2
		ConsumablesDatabase.addAdventureRange( name, unitCost, true, true, false, true, gain2a );
		ConsumablesDatabase.addAdventureRange( name, unitCost, true, false, true, true, gain2a );
		ConsumablesDatabase.addAdventureRange( name, unitCost, false, true, true, true, gain2a );

		// With three effects and munchies pill, average + unitCost * 3 + 2
		ConsumablesDatabase.addAdventureRange( name, unitCost, true, true, true, true, gain3a );
	}

	private static void addAdventureRange( final String name, int unitCost, final boolean gainEffect1, final boolean gainEffect2, final boolean gainEffect3, final boolean gainEffect4, final double result )
	{
		// Remove adventure gains from zodiac signs
		ConsumablesDatabase.getAdventureMap( false, gainEffect1, gainEffect2, gainEffect3, gainEffect4 ).put( name, Double.valueOf( result ) );
		ConsumablesDatabase.getAdventureMap( true, gainEffect1, gainEffect2, gainEffect3, gainEffect4 ).put( name, Double.valueOf( result / ( unitCost == 0 ? 1 : unitCost ) ) );
	}

	private static Map<String, Double> getAdventureMap( final boolean perUnit,
                                                        final boolean gainEffect1, final boolean gainEffect2,
                                                        final boolean gainEffect3, final boolean gainEffect4)
	{
		return ConsumablesDatabase.advsByName[ perUnit ? 1 : 0 ][ gainEffect1 ? 1 : 0 ][ gainEffect2 ? 1 : 0 ][ gainEffect3 ? 1 : 0 ][ gainEffect4 ? 1 : 0 ];
	}

	private static String extractStatRange( String range, double statFactor, int statUnit, int statBonus )
	{
		if ( range == null )
		{
			return null;
		}

		range = range.trim();

		boolean isNegative = range.startsWith( "-" );
		if ( isNegative )
		{
			range = range.substring( 1 );
		}

		int dashIndex = range.indexOf( "-" );
		int start = StringUtilities.parseInt( dashIndex == -1 ? range : range.substring( 0, dashIndex ) );

		if ( dashIndex == -1 )
		{
			double num = ( isNegative ? 0 - start : start ) + statBonus;
			return KoLConstants.SINGLE_PRECISION_FORMAT.format( statFactor * num / statUnit );
		}

		int end = StringUtilities.parseInt( range.substring( dashIndex + 1 ) );
		double num = ( start + end ) / ( isNegative ? -2.0 : 2.0 ) + statBonus;
		return KoLConstants.SINGLE_PRECISION_FORMAT.format( ( isNegative ? 1 : statFactor ) * num / statUnit );
	}

	public static void updateConsumableSize( final String itemName, final int usage, final int size )
	{
		if ( usage == KoLConstants.CONSUME_EAT )
		{
			ConsumablesDatabase.fullnessByName.put( itemName, size );
		}
		else if ( usage == KoLConstants.CONSUME_DRINK )
		{
			ConsumablesDatabase.inebrietyByName.put( itemName, size );
		}
		else if ( usage == KoLConstants.CONSUME_SPLEEN )
		{
			ConsumablesDatabase.spleenHitByName.put( itemName, size );
		}
	}

	public static void registerConsumable( final String itemName, final int usage, final String text )
	{
		// Get information from description
		int size;
		if ( usage == KoLConstants.CONSUME_EAT )
		{
			size = DebugDatabase.parseFullness( text );
		}
		else if ( usage == KoLConstants.CONSUME_DRINK )
		{
			size = DebugDatabase.parseInebriety( text );
		}
		else if ( usage == KoLConstants.CONSUME_SPLEEN )
		{
			size = DebugDatabase.parseToxicity( text );
		}
		else
		{
			return;
		}

		int level = DebugDatabase.parseLevel( text );
		String quality = DebugDatabase.parseQuality( text );

		// Add consumption data for this session
		ConsumablesDatabase.updateConsumableSize( itemName, usage, size );

		ConsumablesDatabase.setConsumptionData( itemName, size, level, quality, "0", "0", "0", "0", "Unspaded" );

		// Print what goes in fullness.txt
		String printMe = ConsumablesDatabase.consumableString( itemName, size, level, quality, "0", "0", "0", "0", "Unspaded" );
		RequestLogger.printLine( printMe );
		RequestLogger.updateSessionLog( printMe );
	}

	public static void updateConsumable( final String itemName, final int size, final int level, final String quality,
					     final String advs, final String mus, final String mys, final String mox,
					     final String comment )
	{
		ConsumablesDatabase.setConsumptionData( itemName, size, level, quality, advs, mus, mys, mox, comment );
	}

	public static final Integer getLevelReqByName( final String name )
	{
		if ( name == null )
		{
			return null;
		}

		return ConsumablesDatabase.levelReqByName.get( name );
	}

	public static final boolean meetsLevelRequirement( final String name )
	{
		if ( name == null )
		{
			return false;
		}

		Integer requirement = ConsumablesDatabase.levelReqByName.get( name );
		if ( requirement == null )
		{
			return true;
		}
		int req = requirement;
		if ( KoLCharacter.getLevel() < req )
		{
			return false;
		}
		if ( req >= 13 && !KoLCharacter.canInteract() )
		{
			return false;
		}
		return true;
	}

	public static final Integer getRawFullness( final String name )
	{
		if ( name == null )
		{
			return null;
		}
		return ConsumablesDatabase.fullnessByName.get( name );
	}

	public static final int getFullness( final String name )
	{
		Integer fullness = ConsumablesDatabase.getRawFullness( name );
		return fullness == null ? 0 : fullness.intValue();
	}

	public static final Integer getRawInebriety( final String name )
	{
		if ( name == null )
		{
			return null;
		}
		return ConsumablesDatabase.inebrietyByName.get( name );
	}

	public static final int getInebriety( final String name )
	{
		Integer inebriety = ConsumablesDatabase.getRawInebriety( name );
		return inebriety == null ? 0 : inebriety.intValue();
	}

	public static final Integer getRawSpleenHit( final String name )
	{
		if ( name == null )
		{
			return null;
		}
		return ConsumablesDatabase.spleenHitByName.get( name );
	}

	public static final int getSpleenHit( final String name )
	{
		Integer spleenhit = ConsumablesDatabase.getRawSpleenHit( name );
		return spleenhit == null ? 0 : spleenhit.intValue();
	}

	public static final String getRawQuality( final String name )
	{
		if ( name == null )
		{
			return null;
		}

		return ConsumablesDatabase.qualityByName.get( name );
	}

	public static final String getQuality( final String name )
	{
		String quality = ConsumablesDatabase.getRawQuality( name );
		return quality != null ? quality : "";
	}

	public static final String getNotes( final String name )
	{
		if ( name == null )
		{
			return null;
		}

		return ConsumablesDatabase.notesByName.get( name );
	}

	private static final Pattern PVP_NOTES_PATTERN = Pattern.compile( "\\+?(\\d+) PvP fights?", Pattern.CASE_INSENSITIVE );

	public static final int getPvPFights( final String name )
	{
		int PvPFights = 0;
		String notes = ConsumablesDatabase.getNotes( name );

		if ( notes != null ) {
			Matcher matcher = PVP_NOTES_PATTERN.matcher( ConsumablesDatabase.getNotes( name ) );

			if ( matcher.find() ) {
				PvPFights = Integer.parseInt( matcher.group(1) );
			}
		}

		return PvPFights;
	}

	private static double conditionalExtraAdventures( final String name, final boolean perUnit )
	{
		int itemId = ItemDatabase.getItemId( name );
		int fullness = ConsumablesDatabase.getFullness( name );
		int inebriety = ConsumablesDatabase.getInebriety( name );
		if ( KoLCharacter.inBondcore() && "martini.gif".equals( ItemDatabase.getImage( itemId ) ) )
		{
			double bonus = 0.0;
			// If we have Tuxedo Shirt equipped, or can get it equipped and have autoTuxedo set, apply 1-3 bonus adventures
			if ( ConsumablesDatabase.isMartini ( itemId ) &&
				( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.TUXEDO_SHIRT, 1 ) ) ||
			     Preferences.getBoolean( "autoTuxedo" ) &&
			     EquipmentManager.canEquip( ItemPool.TUXEDO_SHIRT ) &&
			     InventoryManager.itemAvailable( ItemPool.TUXEDO_SHIRT ) ) )
			{
				bonus += 2.0;
			}
			// +1 Turn from Martini-Drinks from Exotic Bartender, Barry L. Eagle
			if ( Preferences.getBoolean( "bondMartiniTurn" ) )
			{
				bonus += 1.0;
			}
			// +4 Turns (?) Improves Low Quality Martinis from Exotic Olive Procurer, Ben Dover
			if ( Preferences.getBoolean( "bondMartiniPlus" ) )
			{
				// If Martini would have given 10 or more adventures at base, give 4 extra
				int start = ConsumablesDatabase.advStartByName.get( name ).intValue();
				int end = ConsumablesDatabase.advEndByName.get( name ).intValue();
				for ( int i = start; i <= end ; i++ )
				{
					if ( i < 10 )
					{
						bonus += 4.0 / ( end - start + 1 );
					}
				}
			}
			return perUnit ? ( bonus / inebriety ) : bonus;
		}
		if ( ConsumablesDatabase.isMartini ( itemId ) )
		{
			// If we have Tuxedo Shirt equipped, or can get it equipped and have autoTuxedo set, apply 1-3 bonus adventures
			if ( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.TUXEDO_SHIRT, 1 ) ) ||
			     Preferences.getBoolean( "autoTuxedo" ) &&
			     EquipmentManager.canEquip( ItemPool.TUXEDO_SHIRT ) &&
			     InventoryManager.itemAvailable( ItemPool.TUXEDO_SHIRT ) )
			{
				return perUnit ? ( 2.0 / inebriety ) : 2.0;
			}
			return 0.0;
		}
		if ( ConsumablesDatabase.isWine ( itemId ) )
		{
			boolean refinedPalate = KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.REFINED_PALATE ) );
			int start = ConsumablesDatabase.advStartByName.get( name ).intValue();
			int end = ConsumablesDatabase.advEndByName.get( name ).intValue();
			double bonus = 0.0;
			// With Refined Palate, apply 25% bonus adventures
			// If we have mafia pinky ring equipped, or can get it equipped and have autoPinkyRing set, apply 12.5% bonus adventures
			for ( int i = start; i <= end ; i++ )
			{
				bonus += refinedPalate ? Math.floor( i * 0.25 ) / ( end - start + 1 ) : 0.0;
				if ( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.MAFIA_PINKY_RING, 1 ) ) ||
					 Preferences.getBoolean( "autoPinkyRing" ) &&
					 EquipmentManager.canEquip( ItemPool.MAFIA_PINKY_RING ) &&
					 InventoryManager.itemAvailable( ItemPool.MAFIA_PINKY_RING ) )
				{
					double adjustedBase = refinedPalate ? Math.floor( i * 1.25 ) : i;
					bonus += Math.rint( adjustedBase * 0.125 ) / ( end - start + 1 );
				}
			}
			return perUnit ? ( bonus / inebriety ) : bonus;
		}
		if ( ConsumablesDatabase.isLasagna( itemId ) )
		{
			// If we have Gar-ish effect, or can get the effect and have autoGarish set, apply 5 bonus adventures
			Calendar date = Calendar.getInstance( TimeZone.getTimeZone( "GMT-0700" ) );
			if ( date.get( Calendar.DAY_OF_WEEK ) != Calendar.MONDAY &&
			     ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.GARISH ) ) ||
			       Preferences.getBoolean( "autoGarish" ) &&
			       ( KoLCharacter.hasSkill( SkillPool.CLIP_ART ) &&
				 UseSkillRequest.getUnmodifiedInstance( SkillPool.CLIP_ART ).getMaximumCast() > 0 ||
				 InventoryManager.itemAvailable( ItemPool.FIELD_GAR_POTION ) ) ) )
			{
				return perUnit ? ( 5.0 / fullness ) : 5.0;
			}
			return 0.0;
		}
		if ( ConsumablesDatabase.isPizza( itemId ) && KoLCharacter.hasSkill( SkillPool.PIZZA_LOVER ) )
		{
			return perUnit ? 1.0 : fullness;
		}
		if ( ConsumablesDatabase.isBeans( itemId ) && KoLCharacter.hasSkill( SkillPool.BEANWEAVER ) )
		{
			return 2.0;
		}
		if ( ConsumablesDatabase.isSaucy( itemId ) && KoLCharacter.hasSkill( SkillPool.SAUCEMAVEN ) )
		{
			if ( KoLCharacter.isMysticalityClass() )
			{
				return perUnit ? ( 5.0 / fullness ) : 5.0;
			}
			else
			{
				return perUnit ? ( 3.0 / fullness ) : 3.0;
			}
		}
		return 0.0;
	}

	private static int conditionalExtraStats( final String name )
	{
		int itemId = ItemDatabase.getItemId( name );
		if ( ConsumablesDatabase.isBeans( itemId ) && KoLCharacter.hasSkill( SkillPool.BEANWEAVER ) )
		{
			return 25;
		}
		return 0;
	}

	private static double conditionalStatMultiplier( final String name )
	{
		int itemId = ItemDatabase.getItemId( name );
		// No stat gains from consumables in The Source
		if ( KoLCharacter.inTheSource() && !( itemId == ItemPool.HACKED_GIBSON || itemId == ItemPool.BROWSER_COOKIE ) )
		{
			return 0.0;
		}

		double factor = 1.0;
		if ( ConsumablesDatabase.isPizza( itemId ) && KoLCharacter.hasSkill( SkillPool.PIZZA_LOVER ) )
		{
			factor *= 2.0;
		}
		if ( ItemDatabase.isFood( itemId ) && KoLCharacter.isPlumber() )
		{
			factor *= 10.0;
		}
		return factor;
	}

	public static final double getAdventureRange( final String name )
	{
		if ( name == null )
		{
			return 0.0;
		}

		if ( KoLCharacter.inSlowcore() )
		{
			return 0.0;
		}

		String cname = StringUtilities.getCanonicalName( name );
		boolean perUnit = Preferences.getBoolean( "showGainsPerUnit" );
		Double range = null;

		if ( ConsumablesDatabase.getRawFullness( name ) != null )
		{
			int itemId = ItemDatabase.getItemId( name, 1, false );
			boolean sushi = ConcoctionDatabase.getMixingMethod( itemId, name ) == CraftingType.SUSHI;
			boolean milk = KoLConstants.activeEffects.contains( ConsumablesDatabase.MILK );
			boolean lunch = KoLConstants.activeEffects.contains( ConsumablesDatabase.GLORIOUS_LUNCH ) ||
							ConsumablesDatabase.BARREL_OF_LAUGHS.getCount( KoLConstants.activeEffects ) >= 5;
			boolean gourmand = KoLCharacter.hasSkill( "Gourmand" ) || KoLCharacter.hasSkill( "Neurogourmet" );
			boolean munchies = Preferences.getInteger( "munchiesPillsUsed" ) > 0;
			range = ConsumablesDatabase.getAdventureMap( perUnit,
								      !sushi && milk,
								      !sushi && lunch,
								      !sushi && gourmand,
								      !sushi && munchies ).get( name );
		}
		else if ( ConsumablesDatabase.getRawInebriety( name ) != null )
		{
			boolean odeEffect = KoLConstants.activeEffects.contains( ConsumablesDatabase.ODE );
			boolean rowdyDrinker = KoLCharacter.hasSkill( "Rowdy Drinker" ) ||
								ConsumablesDatabase.BEER_BARREL_POLKA.getCount( KoLConstants.activeEffects ) >= 5;
			range = ConsumablesDatabase.getAdventureMap(
				perUnit, odeEffect, rowdyDrinker, false, false ).get( name );
		}
		else if ( ConsumablesDatabase.getRawSpleenHit( name ) != null )
		{
			range = ConsumablesDatabase.getAdventureMap(
				perUnit, false, false, false, false ).get( name );
		}

		if ( range == null )
		{
			return 0.0;
		}

		range += ConsumablesDatabase.conditionalExtraAdventures( name, perUnit );

		return range.doubleValue();
	}

	private static int getStatUnit( final String name )
	{
		if ( !Preferences.getBoolean( "showGainsPerUnit" ) )
		{
			return 1;
		}
		int unit = 0;
		Integer fullness = ConsumablesDatabase.getRawFullness( name );
		Integer inebriety = ConsumablesDatabase.getRawInebriety( name );
		Integer spleenhit = ConsumablesDatabase.getRawSpleenHit( name );

		if ( fullness != null )
		{
			unit += fullness.intValue();
		}
		if ( inebriety != null )
		{
			unit += inebriety.intValue();
		}
		if ( spleenhit != null )
		{
			unit += spleenhit.intValue();
		}
		if ( unit == 0 )
		{
			unit = 1;
		}
		return unit;
	}

	public static final String getMuscleByName( final String name )
	{
		if ( name == null )
		{
			return "";
		}

		String range = ConsumablesDatabase.muscleByName.get( name );
		return range == null ? "" : range;
	}

	public static final String getMuscleRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String muscle = ConsumablesDatabase.muscleByName.get( name );
		double muscleFactor = ( KoLCharacter.currentNumericModifier( Modifiers.MUS_EXPERIENCE_PCT ) + 100.0 ) / 100.0;
		muscleFactor *= ConsumablesDatabase.conditionalStatMultiplier( name );
		int statUnit = ConsumablesDatabase.getStatUnit( name );
		int statBonus = ConsumablesDatabase.conditionalExtraStats( name );
		String range = ConsumablesDatabase.extractStatRange( muscle, muscleFactor, statUnit, statBonus );
		return range == null ? "+0.0" : range;
	}

	public static final String getMysticalityByName( final String name )
	{
		if ( name == null )
		{
			return "";
		}

		String range = ConsumablesDatabase.mysticalityByName.get( name );
		return range == null ? "" : range;
	}

	public static final String getMysticalityRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String mysticality = ConsumablesDatabase.mysticalityByName.get( name );
		double mysticalityFactor = ( KoLCharacter.currentNumericModifier( Modifiers.MYS_EXPERIENCE_PCT ) + 100.0 ) / 100.0;
		mysticalityFactor *= ConsumablesDatabase.conditionalStatMultiplier( name );
		int statUnit = ConsumablesDatabase.getStatUnit( name );
		int statBonus = ConsumablesDatabase.conditionalExtraStats( name );
		String range = ConsumablesDatabase.extractStatRange( mysticality, mysticalityFactor, statUnit, statBonus );
		return range == null ? "+0.0" : range;
	}

	public static final String getMoxieByName( final String name )
	{
		if ( name == null )
		{
			return "";
		}

		String range = ConsumablesDatabase.moxieByName.get( name );
		return range == null ? "" : range;
	}

	public static final String getMoxieRange( final String name )
	{
		if ( name == null )
		{
			return "+0.0";
		}

		String moxie = ConsumablesDatabase.moxieByName.get( name );
		double moxieFactor = ( KoLCharacter.currentNumericModifier( Modifiers.MOX_EXPERIENCE_PCT ) + 100.0 ) / 100.0;
		moxieFactor *= ConsumablesDatabase.conditionalStatMultiplier( name );
		int statUnit = ConsumablesDatabase.getStatUnit( name );
		int statBonus = ConsumablesDatabase.conditionalExtraStats( name );
		String range = ConsumablesDatabase.extractStatRange( moxie, moxieFactor, statUnit, statBonus );
		return range == null ? "+0.0" : range;
	}

	public static final boolean hasAttribute( final int itemId, final String attribute )
	{
		String name = ItemDatabase.getItemName( itemId );
		String notes = ConsumablesDatabase.getNotes( name );
		return notes != null && notes.contains( attribute );
	}

	public static final boolean isMartini( final int itemId )
	{
		return hasAttribute( itemId, "MARTINI" );
	}

	public static final boolean isLasagna( final int itemId )
	{
		return hasAttribute( itemId, "LASAGNA" );
	}

	public static final boolean isSaucy( final int itemId )
	{
		return hasAttribute( itemId, "SAUCY" );
	}

	public static final boolean isPizza( final int itemId )
	{
		return hasAttribute( itemId, "PIZZA" );
	}

	public static final boolean isBeans( final int itemId )
	{
		return hasAttribute( itemId, "BEANS" );
	}

	public static final boolean isWine( final int itemId )
	{
		return hasAttribute( itemId, "WINE" );
	}

	public static final boolean isSalad( final int itemId )
	{
		return hasAttribute( itemId, "SALAD" );
	}

	public static final boolean isBeer( final int itemId )
	{
		return hasAttribute( itemId, "BEER" );
	}

	public static final boolean isCannedBeer( final int itemId )
	{
		return hasAttribute( itemId, "CANNED" );
	}

	// Support for astral consumables and other level dependant consumables

	public static final void setVariableConsumables()
	{
		int level = Math.min( 11, Math.max( 3, KoLCharacter.getLevel() ) );

		// astral pilsner:
		//
		// You gain X Adventures.
		// You gain 0-2X Strongness.
		// You gain 0-2X Enchantedness.
		// You gain 0-2X Chutzpah.
		// You gain 1 Drunkenness.
		//
		// X is equal to your level with a minimum of 3 and a maximum of 11

		String name = "astral pilsner";
		int size = ConsumablesDatabase.getInebriety( name );
		String adventures = String.valueOf( level );
		String statGain = "0-" + 2 * level;
		String muscle = statGain;
		String mysticality = statGain;
		String moxie = statGain;
		String note = "";

		ConsumablesDatabase.setConsumptionData( name, size, adventures, muscle, mysticality, moxie, note );

		// astral hot dog
		//
		// You gain X Adventures.
		// You gain Y Beefiness.
		// You gain Y Enchantedness.
		// You gain Y Cheek.
		// (You gain 3 Fullness.)

		// X and Y are based off of your current level.
		// Levels 1 and 2 use Level 3 stats. The level is capped at level 11.
		// X ranges between 1.8 times your level (rounded up) and 2.2
		//   times your level (rounded down).
		// Y will be between 16 and 20 times your level.

		name = "astral hot dog";
		size = ConsumablesDatabase.getFullness( name );
		int a1 = (int) Math.ceil( 1.8 * level );
		int a2 = (int) Math.floor( 2.2 * level );
		adventures = a1 + "-" + a2;
		statGain = 16 * level + "-" + 20 * level;
		muscle = statGain;
		mysticality = statGain;
		moxie = statGain;
		note = "";

		ConsumablesDatabase.setConsumptionData( name, size, adventures, muscle, mysticality, moxie, note );

		// astral energy drink
		//
		// You gain X Adventures.
		// (You gain 8 Spleen.)
		//
		// Adventure gains appear to be 10 + (your level * 2) +/- 3. Gains are
		// (probably) capped at level 11 giving 29-35 adventures, and levels 1-3
		// are (probably) lumped together giving 13-19 adventures.

		name = "astral energy drink";
		size = ConsumablesDatabase.getSpleenHit( name );
		int a = 10 + level * 2;
		adventures = ( a - 3 ) + "-" + ( a + 3 );
		muscle = "0";
		mysticality = "0";
		moxie = "0";
		note = "";
		ConsumablesDatabase.setConsumptionData( name, size, adventures, muscle, mysticality, moxie, note );

		// spaghetti breakfast
		//
		// You gain X Adventures.
		// (You gain 1 Fullness.)
		//
		// Adventure gains appear to be 0.5 + (your level/2), capped at level 11.

		name = "spaghetti breakfast";
		size = ConsumablesDatabase.getFullness( name );
		float sbAdv = ( level + 1 ) / 2;
		adventures = String.valueOf( sbAdv );
		muscle = "0";
		mysticality = "0";
		moxie = "0";
		note = "";
		ConsumablesDatabase.setConsumptionData( name, size, adventures, muscle, mysticality, moxie, note );

		// cold one
		//
		// You gain X Adventures.
		// (You gain 1 Fullness.)
		//
		// Adventure gains appear to be 0.5 + (your level/2), capped at level 11.

		name = "Cold One";
		size = ConsumablesDatabase.getInebriety( name );
		float coAdv = ( level + 1 ) / 2;
		adventures = String.valueOf( coAdv );
		muscle = "0";
		mysticality = "0";
		moxie = "0";
		note = "";
		ConsumablesDatabase.setConsumptionData( name, size, adventures, muscle, mysticality, moxie, note );
	}

	public static final void setSmoresData()
	{
		// s'more
		String name = "s'more";
		int size = Preferences.getInteger( "smoresEaten" ) + 1 + ConcoctionDatabase.queuedSmores;
		String adventures = String.valueOf( (int) Math.ceil( Math.pow( size, 1.75 ) ) );
		String muscle = "0";
		String mysticality = "0";
		String moxie = "0";
		String note = "";
		ConsumablesDatabase.setConsumptionData( name, size, 1, "crappy", adventures, muscle, mysticality, moxie, note );
		ConsumablesDatabase.fullnessByName.put( name, size );
		Concoction c = ConcoctionPool.get( ItemPool.SMORE );
		if ( c != null )
		{
			c.setConsumptionData();
		}
	}

	public static final void setAffirmationCookieData()
	{
		// Affirmation CookieHandler
		String name = "Affirmation Cookie";
		int size = 1;
		// We don't consider queued cookies as you can't eat two in same day anyway
		int count = Math.min( 4, Preferences.getInteger( "affirmationCookiesEaten" ) + 1 );
		String adventures = String.valueOf( 2 * count + 1 );
		String muscle = String.valueOf( 30 * count );
		String mysticality = String.valueOf( 30 * count );
		String moxie = String.valueOf( 30 * count );
		String note = "";
		ConsumablesDatabase.setConsumptionData( name, size, 1, "good", adventures, muscle, mysticality, moxie, note );
	}

	// Support for dusty bottles of wine

	public static final String dustyBottleType( final int itemId )
	{
		return
			( itemId == ItemPool.DUSTY_BOTTLE_OF_MERLOT ) ? "average" :
			( itemId == ItemPool.DUSTY_BOTTLE_OF_PORT ) ? "vinegar" :
			( itemId == ItemPool.DUSTY_BOTTLE_OF_PINOT_NOIR ) ? "spooky" :
			( itemId == ItemPool.DUSTY_BOTTLE_OF_ZINFANDEL ) ? "great" :
			( itemId == ItemPool.DUSTY_BOTTLE_OF_MARSALA ) ? "glassy" :
			( itemId == ItemPool.DUSTY_BOTTLE_OF_MUSCAT ) ? "bad" :
			"dusty";
	}

	public static final String dustyBottleName( final int itemId )
	{
		String name = ItemDatabase.getItemName( itemId );
		String type = ConsumablesDatabase.dustyBottleType( itemId );
		return type.equals( "dusty" ) ? name : StringUtilities.globalStringReplace( name, " of", " of " + type );
	}

	public static final boolean consumableOnlyByVampyres( final String name )
	{
		String notes = ConsumablesDatabase.getNotes( name );
		return ( notes != null && notes.startsWith( "Vampyre" ) );
	}

	public static final boolean consumableByVampyres( final String name )
	{
		return ( name.equals( "magical sausage" ) || ConsumablesDatabase.consumableOnlyByVampyres( name ) );
	}

	public static final boolean consumableByVampyres( final int itemId )
	{
		return ConsumablesDatabase.consumableByVampyres( ItemDatabase.getDisplayName( itemId ) );
	}
}
