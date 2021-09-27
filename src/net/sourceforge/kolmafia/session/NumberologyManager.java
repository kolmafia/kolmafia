package net.sourceforge.kolmafia.session;

import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.request.NumberologyRequest;

public class NumberologyManager
{
	public static String TRY_AGAIN = "Try Again";

	private static final String[] PRIZES =
	{
		"0 Meat",			// 00
		"seal-clubbing club",		// 01
		"100 Sleepy",			// 02
		"100 Confused",			// 03
		"100 Embarrassed",		// 04
		"100 Far Out",			// 05
		"100 Wings",			// 06
		"100 Beaten Up",		// 07
		"100 Hardly Poisoned at All",	// 08
		"100 Knob Goblin Perfume",	// 09
		"100 Steroid Boost",		// 10
		"Drunkenness (-1,+3)",		// 11
		"fight Gnollish Gearhead",	// 12
		"Nothing. Maybe.",		// 13
		"moxie weed",			// 14
		"15 Meat",			// 15
		"magicalness-in-a-can",		// 16
		"1 Adventure",			// 17
		"bottle of booze",		// 18
		"+ Moxie",			// 19
		"- Mainstate",			// 20
		"1 Fite",			// 21
		"pygmy phone number",		// 22
		"+ Muscle",			// 23
		TRY_AGAIN,			// 24
		TRY_AGAIN,			// 25
		TRY_AGAIN,			// 26
		"+ Moxie",			// 27
		TRY_AGAIN,			// 28
		TRY_AGAIN,			// 29
		"fight ghuol",			// 30
		TRY_AGAIN,			// 31
		TRY_AGAIN,			// 32
		"magicalness-in-a-can",		// 33
		"+ Mainstat",			// 34
		"+ Muscle",			// 35
		"2 Adventures",			// 36
		"3 Fites",			// 37
		"+ Mysticality",		// 38
		TRY_AGAIN,			// 39
		"40 Meat",			// 40
		TRY_AGAIN,			// 41
		TRY_AGAIN,			// 42
		"+ Muscle",			// 43
		"bottle of booze",		// 44
		"magicalness-in-a-can",		// 45
		TRY_AGAIN,			// 46
		"+ Moxie",			// 47
		"fight your butt",		// 48
		"+ Mysticality",		// 49
		"- Mysticality",		// 50
		"fight War Frat 151st Infantryman",	// 51
		TRY_AGAIN,			// 52
		TRY_AGAIN,			// 53
		TRY_AGAIN,			// 54
		TRY_AGAIN,			// 55
		TRY_AGAIN,			// 56
		"+ Moxie",			// 57
		"10 Teleportitis",		// 58
		TRY_AGAIN,			// 59
		TRY_AGAIN,			// 60
		TRY_AGAIN,			// 61
		TRY_AGAIN,			// 62
		"+ Muscle",			// 63
		TRY_AGAIN,			// 64
		TRY_AGAIN,			// 65
		"magicalness-in-a-can",		// 66
		TRY_AGAIN,			// 67
		"+ Mysticality",		// 68
		"3 Adventures",			// 69
		"+ Mainstat",			// 70
		"- Mysticality",		// 71
		TRY_AGAIN,			// 72
		TRY_AGAIN,			// 73
		TRY_AGAIN,			// 74
		"bottle of booze",		// 75
		TRY_AGAIN,			// 76
		"spooky stick",			// 77
		"+ Mysticality",		// 78
		TRY_AGAIN,			// 79
		TRY_AGAIN,			// 80
		TRY_AGAIN,			// 81
		TRY_AGAIN,			// 82
		"+ Muscle",			// 83
		TRY_AGAIN,			// 84
		TRY_AGAIN,			// 85
		TRY_AGAIN,			// 86
		"+ Moxie",			// 87
		"magicalness-in-a-can",		// 88
		"+ Mainstat",			// 89
		"- Muscle",			// 90
		TRY_AGAIN,			// 91
		TRY_AGAIN,			// 92
		"93 Meat",			// 93
		TRY_AGAIN,			// 94
		TRY_AGAIN,			// 95
		TRY_AGAIN,			// 96
		"magicalness-in-a-can",		// 97
		"+ Mysticality",		// 98
		"bottle of booze",		// 99
	};

	public static final String numberologyPrize( final int result )
	{
		return PRIZES[ result % PRIZES.length ];
	}

	public static final int rawNumberology( final int seed, final int adventureDelta, final int spleenDelta )
	{
		int ascensions = KoLCharacter.getAscensions();
		int sign = KoLCharacter.getSignIndex();
		int spleen = KoLCharacter.getSpleenUse();
		int level =  KoLCharacter.getLevel();
		int adventures = KoLCharacter.getAdventuresLeft();
		return ( Math.abs( seed ) + ascensions + sign ) * ( spleen + spleenDelta + level ) + ( adventures - adventureDelta );
	}

	public static final int numberology( final int seed )
	{
		return NumberologyManager.numberology( seed, 0, 0 );
	}

	public static final int numberology( final int seed, final int adventureDelta, final int spleenDelta )
	{
		return NumberologyManager.rawNumberology( seed, adventureDelta, spleenDelta ) % 100;
	}

	public static final Map<Integer,Integer> reverseNumberology()
	{
		return NumberologyManager.reverseNumberology( 0, 0 );
	}

	public static final Map<Integer,Integer> reverseNumberology( final int adventureDelta, final int spleenDelta )
	{
		Map<Integer,Integer> results = new TreeMap<Integer,Integer>();

		for ( int seed = 0; seed < 100; ++seed )
		{
			int result = NumberologyManager.numberology( seed, adventureDelta, spleenDelta );
			if ( results.containsKey( result) )
			{
				return results;
			}
			results.put( result, seed );
		}

		return results;
	}

	public static final boolean calculateTheUniverse( final int seed )
	{
		NumberologyRequest request = new NumberologyRequest( seed );
		// This will automate a fight
		request.run();
		return KoLmafia.permitsContinue();
	}
}
