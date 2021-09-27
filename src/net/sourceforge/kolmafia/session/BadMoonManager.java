package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class BadMoonManager
{
	public final static String STAT1 = "+20 to one stat, -5 to others";
	public final static String STAT2 = "+40 to one stat, -50% Familiar Weight";
	public final static String STAT3 = "+50% to one stat, -50% to another";
	public final static String DAMAGE1 = "+10 damage, Damage Reduction -2";
	public final static String DAMAGE2 = "+20 damage, 1-3 damage/round to self";
	public final static String RESIST1 = "So-So resistance to one, Vulnerability to opposites";
	public final static String RESIST2 = "Resistance to all, -attributes";
	public final static String ITEM_DROP = "Item Drop";
	public final static String MEAT_DROP = "Meat Drop";
	public final static String DAMAGE_REDUCTION = "+ Damage Reduction, - Weapon Damage";
	public final static String MEAT = "Meat";
	public final static String ITEMS = "Items";

	public static final String [] TYPES =
	{
		STAT1,
		STAT2,
		STAT3,
		DAMAGE1,
		DAMAGE2,
		RESIST1,
		RESIST2,
		ITEM_DROP,
		MEAT_DROP,
		DAMAGE_REDUCTION,
		MEAT,
		ITEMS,
	};

	public static final Object [][] SPECIAL_ENCOUNTERS =
	{
		// Encounter Name
		// Adventure Zone
		// Prerequisites
		// Status Effect
		// Description
		// Type
		// Setting

		// Effects that grant +20 to one stat and -5 to the others
		{
			"O Goblin, Where Art Thou?",
			"Outskirts of Cobb's Knob",
			"receiving Knob Goblin encryption key",
			EffectPool.get( EffectPool.MINIONED, 10 ),
			"Muscle +20, Mysticality -5, Moxie -5",
			BadMoonManager.STAT1,
			"badMoonEncounter01",
		},
		{
			"Pantry Raid!",
			"The Haunted Pantry",
			"opening Spookyraven Manor",
			EffectPool.get( EffectPool.ENHANCED_ARCHAEOLOGIST, 10 ),
			"Mysticality +20, Muscle -5, Moxie -5",
			BadMoonManager.STAT1,
			"badMoonEncounter02",
		},
		{
			"Sandwiched in the Club",
			"The Sleazy Back Alley",
			null,
			EffectPool.get( EffectPool.CHRONOLOGICALLY_PUMMELLED, 10 ),
			"Moxie +20, Muscle -5, Mysticality -5",
			BadMoonManager.STAT1,
			"badMoonEncounter03",
		},

		// Effects that grant +40 to one stat and -50% Familiar weight
		{
			"It's So Heavy",
			"Cobb's Knob Treasury",
			null,
			EffectPool.get( EffectPool.ANIMAL_EXPLOITER, 10 ),
			"Muscle +40, Familiar Weight -50%",
			BadMoonManager.STAT2,
			"badMoonEncounter04",
		},
		{
			"KELF! I Need Somebody!",
			"Cobb's Knob Kitchens",
			null,
			EffectPool.get( EffectPool.SCENT_OF_A_KITCHEN_ELF, 10 ),
			"Mysticality +40, Familiar Weight -50%",
			BadMoonManager.STAT2,
			"badMoonEncounter05",
		},
		{
			"On The Whole, the Bark is Better",
			"Cobb's Knob Harem",
			null,
			EffectPool.get( EffectPool.ONCE_BITTEN_TWICE_SHY, 10 ),
			"Moxie +40, Familiar Weight -50%",
			BadMoonManager.STAT2,
			"badMoonEncounter06",
		},

		// Effects that adjust one stat by +50% and another by -50%
		{
			"It's All The Rage",
			"Orcish Frat House (Undisguised)",
			null,
			EffectPool.get( EffectPool.THE_RAGE, 10 ),
			"Muscle +50%, Mysticality -50%",
			BadMoonManager.STAT3,
			"badMoonEncounter07",
		},
		{
			"Double-Secret Initiation",
			"Orcish Frat House (In Disguise)",
			null,
			EffectPool.get( EffectPool.SHAMED_AND_MANIPULATED, 10 ),
			"Muscle +50%, Moxie -50%",
			BadMoonManager.STAT3,
			"badMoonEncounter08",
		},
		{
			"Better Dread Than Dead",
			"The Hippy Camp (Undisguised)",
			null,
			EffectPool.get( EffectPool.DREADLOCKED, 10 ),
			"Mysticality +50%, Moxie -50%",
			BadMoonManager.STAT3,
			"badMoonEncounter09",
		},
		{
			"Drumroll, Please",
			"The Hippy Camp (In Disguise)",
			null,
			EffectPool.get( EffectPool.DRUMMED_OUT, 10 ),
			"Mysticality +50%, Muscle -50%",
			BadMoonManager.STAT3,
			"badMoonEncounter10",
		},
		{
			"How Far Down Do You Want To Go?",
			"The Obligatory Pirate's Cove (Undisguised)",
			null,
			EffectPool.get( EffectPool.HORNSWAGGLED, 10 ),
			"Moxie +50%, Muscle -50%",
			BadMoonManager.STAT3,
			"badMoonEncounter11",
		},
		{
			"Mind Your Business",
			"The Obligatory Pirate's Cove (In Disguise)",
			null,
			EffectPool.get( EffectPool.THIRD_EYE_BLIND, 10 ),
			"Moxie +50%, Mysticality -50%",
			BadMoonManager.STAT3,
			"badMoonEncounter12",
		},

		// Effects that grant +10 damage and Damage Reduction -2
		{
			"Vole Call!",
			"The Haunted Billiards Room",
			"opening Haunted Library",
			EffectPool.get( EffectPool.RE_POSSESSED, 10 ),
			"Bonus Weapon Damage +10, Damage Reduction -2",
			BadMoonManager.DAMAGE1,
			"badMoonEncounter13",
		},
		{
			"Frost Bitten, Twice Shy",
			"The Goatlet",
			"opening The eXtreme Slope",
			EffectPool.get( EffectPool.FROSTBITTEN, 10 ),
			"+10 Cold Damage, Damage Reduction -2",
			BadMoonManager.DAMAGE1,
			"badMoonEncounter14",
		},
		{
			"If You Smell Something Burning, It's My Heart",
			"The Haunted Kitchen",
			null,
			EffectPool.get( EffectPool.BURNING_HEART, 10 ),
			"+10 Hot Damage, Damage Reduction -2",
			BadMoonManager.DAMAGE1,
			"badMoonEncounter15",
		},
		{
			"Oil Be Seeing You",
			"Pandamonium Slums",
			"completed Azazel Quest",
			EffectPool.get( EffectPool.BASTED, 10 ),
			"+10 Sleaze Damage, Damage Reduction -2",
			BadMoonManager.DAMAGE1,
			"badMoonEncounter16",
		},
		{
			"Back Off, Man. I'm a Scientist.",
			"The Haunted Library",
			null,
			EffectPool.get( EffectPool.FREAKED_OUT, 10 ),
			"+10 Spooky Damage, Damage Reduction -2",
			BadMoonManager.DAMAGE1,
			"badMoonEncounter17",
		},
		{
			"Oh Guanoes!",
			"Guano Junction",
			null,
			EffectPool.get( EffectPool.GUANIFIED, 10 ),
			"+10 Stench Damage, Damage Reduction -2",
			BadMoonManager.DAMAGE1,
			"badMoonEncounter18",
		},

		// Effects that grant +20 damage at the cost of taking the
		// same kind of damage yourself every round.
		{
			"Do You Think You're Better Off Alone",
			"The Castle in the Clouds in the Sky",
			"completed Giant Trash Quest",
			EffectPool.get( EffectPool.RAVING_LUNATIC, 10 ),
			"Melee Damage +20, Lose 1-3 HP per combat round",
			BadMoonManager.DAMAGE2,
			"badMoonEncounter19",
		},
		{
			"The Big Chill",
			"The Icy Peak",
			null,
			EffectPool.get( EffectPool.HYPERBOLIC_HYPOTHERMIA, 10 ),
			"+20 Cold Damage, Lose 1-3 HP (cold damage) per combat round",
			BadMoonManager.DAMAGE2,
			"badMoonEncounter20",
		},
		{
			"Mr. Sun Is Not Your Friend",
			"An Oasis",
			"receiving worm-riding hooks",
			EffectPool.get( EffectPool.SOLAR_FLAIR, 10 ),
			"+20 Hot Damage, Lose 1-3 HP (hot damage) per combat round",
			BadMoonManager.DAMAGE2,
			"badMoonEncounter21",
		},
		{
			"Pot Jacked",
			"The Hole in the Sky",
			"made Richard's star key",
			EffectPool.get( EffectPool.GREASED, 10 ),
			"+20 Sleaze Damage, Lose 1-3 HP (sleaze damage) per combat round",
			BadMoonManager.DAMAGE2,
			"badMoonEncounter22",
		},
		{
			"Party Crasher",
			"The Haunted Ballroom",
			"opening Haunted Wine Cellar",
			EffectPool.get( EffectPool.SLIMED, 10 ),
			"+20 Spooky Damage, Lose 1-3 HP (spooky damage) per combat round",
			BadMoonManager.DAMAGE2,
			"badMoonEncounter23",
		},
		{
			"A Potentially Offensive Reference Has Been Carefully Avoided Here",
			"The Black Forest",
			"opening The Black Market",
			EffectPool.get( EffectPool.TAR_STRUCK, 10 ),
			"+20 Stench Damage, Lose 1-3 HP (stench damage) per combat round",
			BadMoonManager.DAMAGE2,
			"badMoonEncounter24",
		},

		// Effects that grant resistance to one element and
		// vulnerability to the opposite elements
		{
			"Strategy: Get Arts",
			"Inside the Palindome",
			"defeating Dr. Awkward",
			EffectPool.get( EffectPool.PAW_SWAP, 10 ),
			"So-So Cold Resistance. Double damage from Hot and Spooky",
			BadMoonManager.RESIST1,
			"badMoonEncounter25",
		},
		{
			"Pot-Unlucky",
			"The Hidden City",
			"opening A Smallish Temple",
			EffectPool.get( EffectPool.DEEP_FRIED, 10 ),
			"So-So Hot Resistance, Double damage from Stench and Sleaze",
			BadMoonManager.RESIST1,
			"badMoonEncounter26",
		},
		{
			"Mistaken Identity, LOL",
			"The Valley of Rof L'm Fao",
			"receiving facsimile dictionary",
			EffectPool.get( EffectPool.SCARED_STIFF, 10 ),
			"So-So Sleaze Resistance, Double damage from Cold and Spooky",
			BadMoonManager.RESIST1,
			"badMoonEncounter27",
		},
		{
			"Mind the Fine Print",
			"Tower Ruins",
			null,
			EffectPool.get( EffectPool.SIDE_AFFECTATION, 10 ),
			"So-So Spooky Resistance, Double damage from Stench and Hot",
			BadMoonManager.RESIST1,
			"badMoonEncounter28",
		},
		{
			"Sweatin' Like a Vet'ran",
			"The Arid, Extra-Dry Desert (Ultrahydrated)",
			"receiving worm-riding hooks",
			EffectPool.get( EffectPool.SHIRTLESS_IN_SEATTLE, 10 ),
			"So-So Stench Resistance, Double damage from Cold and Sleaze",
			BadMoonManager.RESIST1,
			"badMoonEncounter29",
		},

		// Effects that grant elemental resistance and reduce attributes
		{
			"Elementally, My Deal Watson",
			"Beanbat Chamber",
			"opening The Beanstalk",
			EffectPool.get( EffectPool.BATIGUE, 10 ),
			"Slight Resistance to All Elements, All Attributes -10%",
			BadMoonManager.RESIST2,
			"badMoonEncounter30",
		},
		{
			"Hair of the Hellhound",
			"The Haunted Wine Cellar",
			"defeating Lord Spookyraven",
			EffectPool.get( EffectPool.CUPSHOTTEN, 10 ),
			"So-So Resistance to All Elements, All Attributes -20%",
			BadMoonManager.RESIST2,
			"badMoonEncounter31",
		},

		// Effects that improve Item Drop
		{
			"Shall We Dance",
			"Cobb's Knob Laboratory",
			null,
			EffectPool.get( EffectPool.THE_VITUS_VIRUS, 10 ),
			"+50% Items from Monsters, -5 Stats Per Fight",
			BadMoonManager.ITEM_DROP,
			"badMoonEncounter32",
		},
		{
			"You Look Flushed",
			"The Haunted Bathroom",
			null,
			EffectPool.get( EffectPool.YOUR_NUMBER_1_PROBLEM, 10 ),
			"+100% Items from Monsters, All Attributes -20",
			BadMoonManager.ITEM_DROP,
			"badMoonEncounter33",
		},

		// Effects that improve Meat Drop
		{
			"What Do We Want?",
			"The Misspelled Cemetary (Pre-Cyrpt)",
			null,
			EffectPool.get( EffectPool.BRAAAINS, 10 ),
			"+50% Meat from Monsters, -50% Combat Initiative",
			BadMoonManager.MEAT_DROP,
			"badMoonEncounter34",
		},
		{
			"When Do We Want It?",
			"The Misspelled Cemetary (Post-Cyrpt)",
			null,
			EffectPool.get( EffectPool.BRAAAAAAINS, 10 ),
			"+200% Meat from Monsters, -50% Items from Monsters",
			BadMoonManager.MEAT_DROP,
			"badMoonEncounter35",
		},

		// Effects that grant Damage Reduction but reduce Weapon Damage
		{
			"Getting Hammered",
			"The Inexplicable Door",
			"receiving digital key",
			EffectPool.get( EffectPool.MIDGETIZED, 10 ),
			"Damage Reduction: 4. Weapon Damage -8",
			BadMoonManager.DAMAGE_REDUCTION,
			"badMoonEncounter36",
		},
		{
			"Obligatory Mascot Cameo",
			"The Penultimate Fantasy Airship",
			"opening The Castle in the Clouds in the Sky",
			EffectPool.get( EffectPool.SYNTHESIZED, 10 ),
			"Damage Reduction: 8, Weapon Damage -8",
			BadMoonManager.DAMAGE_REDUCTION,
			"badMoonEncounter37",
		},

		// Encounters that grant meat
		{
			"This Doesn't Look Like Candy Mountain",
			"The Spooky Forest",
			null,
			EffectPool.get( EffectPool.MISSING_KIDNEY ),
			"1,000 Meat",
			BadMoonManager.MEAT,
			"badMoonEncounter38",
		},
		{
			"Flowers For ",		// (Familiar Name)
			"Degrassi Knoll",
			"returned the bitchin' meatcar to the guild.",
			EffectPool.get( EffectPool.DUHHH, 10 ),
			"2,000 Meat, Lose 12-56(?) MP, Mysticality -20",
			BadMoonManager.MEAT,
			"badMoonEncounter39",
		},
		{
			"Onna Stick",
			"The Bat Hole Entrance",
			"opening The Boss Bat's Lair",
			EffectPool.get( EffectPool.AFFRONTED_DECENCY, 10 ),
			"3,000 Meat, Moxie -20",
			BadMoonManager.MEAT,
			"badMoonEncounter40",
		},
		{
			"The Beaten-Senseless Man's Hand",
			"South of The Border",
			null,
			EffectPool.get( EffectPool.BEATEN_UP, 10 ),
			"4,000 Meat, All Attributes -50%",
			BadMoonManager.MEAT,
			"badMoonEncounter41",
		},
		{
			"A White Lie",
			"Whitey's Grove",
			"opening The Road to the White Citadel",
			EffectPool.get( EffectPool.MAID_DISSERVICE, 10 ),
			"5,000 Meat, All Attributes -20%",
			BadMoonManager.MEAT,
			"badMoonEncounter42",
		},

		// Encounters that grant items or skills
		{
			"Surprising!",
			"Noob Cave",
			null,
			null,
			"Familiar-Gro&trade; Terrarium, black kitten, 14 Drunkenness",
			BadMoonManager.ITEMS,
			"badMoonEncounter43",
		},
		{
			"That's My Favorite Kind of Contraption",
			"The Spooky Forest",
			"opening The Hidden Temple",
			EffectPool.get( EffectPool.DANG_NEAR_CUT_IN_HALF, 5 ),
			"Muscle -50%, Gain Torso Awareness",
			BadMoonManager.ITEMS,
			"badMoonEncounter44",
		},
		{
			"Say Cheese!",
			"The Arid, Extra-Dry Desert (unhydrated)",
			null,
			null,
			"anticheese, lose 50 HP",
			BadMoonManager.ITEMS,
			"badMoonEncounter45",
		},
		{
			"Because Stereotypes Are Awesome",
			"The Typical Tavern (Post-Quest)",
			null,
			null,
			"leprechaun hatchling, 1 Drunkenness",
			BadMoonManager.ITEMS,
			"badMoonEncounter46",
		},
		{
			"Why Did It Have To Be Snake Eyes?",
			"The Hidden Temple",
			null,
			null,
			"loaded dice",
			BadMoonManager.ITEMS,
			"badMoonEncounter47",
		},
		{
			"The Placebo Defect",
			"The Haunted Conservatory",
			null,
			null,
			"potato sprout, Lose 75% HP & MP",
			BadMoonManager.ITEMS,
			"badMoonEncounter48",
		},
	};

	private static String dataEncounterName( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[0] );
	}

	private static String dataLocation( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[1] );
	}

	private static String dataPrereq( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[2] );
	}

	private static AdventureResult dataEffect( final Object[] data )
	{
		return ( data == null ) ? null : ((AdventureResult) data[3] );
	}

	private static String dataDescription( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[4] );
	}

	private static String dataType( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[5] );
	}

	private static String dataSetting( final Object[] data )
	{
		return ( data == null ) ? null : ((String) data[6] );
	}

	private static Object[] encounterToData( final String encounter )
	{
		for ( int i = 0; i < SPECIAL_ENCOUNTERS.length; ++i )
		{
			Object [] data = SPECIAL_ENCOUNTERS[i];
			String encounterName = dataEncounterName( data );
			if ( encounter.startsWith( encounterName ) )
			{
				return data;
			}
		}
		return null;
	}

	public static final void validateBadMoon()
	{
		int lastAscension = Preferences.getInteger( "lastBadMoonReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastBadMoonReset", KoLCharacter.getAscensions() );
			for ( int i = 0; i < SPECIAL_ENCOUNTERS.length; ++i )
			{
				Object [] data = SPECIAL_ENCOUNTERS[i];
				String setting = BadMoonManager.dataSetting( data );
				if ( setting != null )
				{
					Preferences.setBoolean( setting, false );
				}
			}
		}
	}

	public static final boolean specialAdventure( final String encounterName )
	{
		if ( !KoLCharacter.inBadMoon() )
		{
			return false;
		}

		Object [] data = BadMoonManager.encounterToData( encounterName );
		return data != null;
	}

	public static final void registerAdventure( final String encounterName )
	{

		Object [] data = BadMoonManager.encounterToData( encounterName );
		if ( data != null )
		{
			BadMoonManager.validateBadMoon();
			Preferences.setBoolean( dataSetting( data ), true );
		}
	}

	public static final void report()
	{
		BadMoonManager.validateBadMoon();
		StringBuffer output = new StringBuffer();

		BadMoonManager.startReport( output );

		for ( int i = 0; i < BadMoonManager.TYPES.length; ++i )
		{
			String type = BadMoonManager.TYPES[ i ];
			BadMoonManager.reportType( type, output );
		}

		BadMoonManager.endReport( output );

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}

	private static void startReport( final StringBuffer output )
	{
		output.append( "<table border=2 cols=2>" );
	}

	private static void endReport( final StringBuffer output )
	{
		output.append( "</table>" );
	}

	private static void reportType( final String type, final StringBuffer output )
	{
		// The "type" is a descriptive string
		output.append( "<tr><th colspan=2>" );
		output.append( type );
		output.append( "</th></tr>" );

		for ( int i = 0; i < SPECIAL_ENCOUNTERS.length; ++i )
		{
			Object [] data = SPECIAL_ENCOUNTERS[i];
			if ( type != BadMoonManager.dataType( data ) )
			{
				continue;
			}

			String name = BadMoonManager.dataEncounterName( data );
			String location = BadMoonManager.dataLocation( data );
			String prereq = BadMoonManager.dataPrereq( data );
			AdventureResult effect = BadMoonManager.dataEffect( data );
			String description = BadMoonManager.dataDescription( data );
			String setting = BadMoonManager.dataSetting( data );
			boolean value = setting != null ? Preferences.getBoolean( setting ) : false;
			output.append( "<tr><td rowspan=3>" );
			output.append( value );
			output.append( "</td><td align=left>" );
			output.append( name );
			output.append( "</td></tr><tr><td align=left>" );
			output.append( location );
			if ( prereq != null )
			{
				output.append( " after " );
				output.append( prereq );
			}
			output.append( "</td></tr><tr><td align=left>" );
			if ( effect != null )
			{
				output.append( effect.toString() );
				output.append( ": " );
			}
			output.append( description );
			output.append( "</td></tr>" );
		}
	}
}
