package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.Modifiers.Modifier;

import net.sourceforge.kolmafia.utilities.PHPRandom;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;

public class VotingBoothManager
{
	private static final Modifier[] VOTING_BOOTH_POSITIVE_MODIFIERS = {
		new Modifier( "Monster Level", "+10" ),
		new Modifier( "Food Drop", "+30" ),
		new Modifier( "Monster Level", "-10" ),
		new Modifier( "Initiative", "+25" ),
		new Modifier( "Stench Damage", "+10" ),
		new Modifier( "Sleaze Damage", "+10" ),
		new Modifier( "Pants Drop", "+30" ),
		new Modifier( "Maximum MP Percent", "+30" ),
		new Modifier( "Moxie Percent", "+25" ),
		new Modifier( "Ranged Damage Percent", "+100" ),
		new Modifier( "Experience (Mysticality)", "+4" ),
		new Modifier( "Experience (Moxie)", "+4" ),
		new Modifier( "Weapon Damage Percent", "+100" ),
		new Modifier( "Stench Resistance", "+3" ),
		new Modifier( "Booze Drop", "+30" ),
		new Modifier( "Item Drop", "+15" ),
		new Modifier( "Cold Damage", "+10" ),
		new Modifier( "Hot Resistance", "+3" ),
		new Modifier( "Weapon Damage Unarmed", "+20" ),
		new Modifier( "Muscle Percent", "+25" ),
		new Modifier( "Experience", "+3" ),
		new Modifier( "Spell Damage Percent", "+20" ),
		new Modifier( "Spooky Resistance", "+3" ),
		new Modifier( "Hot Damage", "+10" ),
		new Modifier( "Meat Drop", "+30" ),
		new Modifier( "Experience (familiar)", "+2" ),
		new Modifier( "Mysticality Percent", "+25" ),
		new Modifier( "Cold Resistance", "+3" ),
		new Modifier( "Experience (Muscle)", "+4" ),
		new Modifier( "Gear Drop", "+30" ),
		new Modifier( "Adventures", "+1" ),
		new Modifier( "Candy Drop", "+30" ),
		new Modifier( "Maximum HP Percent", "+30" ),
		new Modifier( "Sleaze Resistanc", "+3" ),
	};

	private static final Modifier[] VOTING_BOOTH_NEGATIVE_MODIFIERS = {
		new Modifier( "Maximum MP Percent", "-50" ),
		new Modifier( "Initiative", "-30" ),
		new Modifier( "Moxie", "-20" ),
		new Modifier( "Experience", "-3" ),
		new Modifier( "Spell Damage Percent", "-50" ),
		new Modifier( "Muscle", "-20" ),
		new Modifier( "Meat Drop", "-30" ),
		new Modifier( "Adventures", "-2" ),
		new Modifier( "Item Drop", "-20" ),
		new Modifier( "Critical Hit Percent", "-10" ),
		new Modifier( "Experience (familiar)", "-2" ),
		new Modifier( "Gear Drop", "-50" ),
		new Modifier( "Maximum HP Percent", "-50" ),
		new Modifier( "Mysticality", "-20" ),
		new Modifier( "Weapon Damage Percent", "-50" ),
	};

	public static final int calculateSeed( final int clss, final int path, final int daycount )
	{
		return ( 4 * path ) + ( 9 * clss ) + ( 79 * daycount );
	}

	public static final Modifier[] getPositiveInitiatives( final int seed )
	{
		PHPRandom rng = new PHPRandom( seed );

		int[] positives = rng.array( VOTING_BOOTH_POSITIVE_MODIFIERS.length, 3 );

		Modifier[] modifiers = new Modifier[3];

		for ( int p = 0; p < modifiers.length; p++ )
		{
			modifiers[ p ] = VOTING_BOOTH_POSITIVE_MODIFIERS[ positives[ p ] ];
		}

		return modifiers;
	}

	public static final Modifier getNegativeInitiative( final int seed )
	{
		PHPMTRandom mtRng = new PHPMTRandom( seed );

		int n = 15;
		while ( n > 14 )
		{
			n = mtRng.nextInt( 0, 15 );
		}

		return VOTING_BOOTH_NEGATIVE_MODIFIERS[ n ];
	}

	public static final Modifier[] getInitiatives( final int clss, final int path, final int daycount )
	{
		int seed = calculateSeed( clss, path, daycount );

		Modifier[] modifiers = new Modifier[4];

		Modifier[] positive = getPositiveInitiatives( seed );

		for ( int i = 0; i < positive.length; i++ )
		{
			modifiers[ i ] = positive[ i ];
		}

		modifiers[ 3 ] = getNegativeInitiative( seed );

		return modifiers;
	}
}
