/**
 * Copyright (c) 2006, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.StringTokenizer;

public class AreaCombatData implements KoLConstants
{
	private boolean valid;
	private int minHit;
	private int maxHit;
	private int minEvade;
	private int maxEvade;

	public AreaCombatData( String data )
	{	this.valid = parse( data );
	}

	private boolean parse( String s )
	{
		int minHit = 0;
		int maxHit = 0;
		int minEvade = 0;
		int maxEvade = 0;

		// Hit: 10 Evade: 12-13

		StringTokenizer tokens = new StringTokenizer( s, " " );
		while ( tokens.hasMoreTokens() )
		{
			try
			{
				String option = tokens.nextToken();
				if ( option.equals( "Hit:" ) )
				{
					if ( !tokens.hasMoreTokens() )
						return false;
					String value = tokens.nextToken();
					int dash = value.indexOf( "-" );
					if ( dash != -1 )
					{
						minHit = Integer.parseInt( value.substring( 0, dash ) );
						maxHit = Integer.parseInt( value.substring( dash + 1 ) );
					}
					else
					{
						minHit = Integer.parseInt( value );
						maxHit = minHit;
					}
					continue;
				}

				if ( option.equals( "Evade:" ) )
				{
					if ( !tokens.hasMoreTokens() )
						return false;
					String value = tokens.nextToken();
					int dash = value.indexOf( "-" );
					if ( dash != -1 )
					{
						minEvade = Integer.parseInt( value.substring( 0, dash ) );
						maxEvade = Integer.parseInt( value.substring( dash + 1 ) );
					}
					else
					{
						minEvade = Integer.parseInt( value );
						maxEvade = minEvade;
					}
					continue;
				}
			}
			catch ( Exception e )
			{
			}

			return false;
		}

		this.minHit = minHit;
		this.maxHit = maxHit;
		this.minEvade = minEvade;
		this.maxEvade = maxEvade;
		return true;
	}

	public boolean valid()
	{	return valid;
	}

	public int minHit()
	{	return minHit;
	}

	public int maxHit()
	{	return maxHit;
	}

	public int minEvade()
	{	return minEvade;
	}

	public int maxEvade()
	{	return maxEvade;
	}

	public String safetyString()
	{
		int ml = monsterLevelAdjustment();
		int moxie = KoLCharacter.getAdjustedMoxie() - ml;
		boolean ranged = KoLCharacter.rangedWeapon();
		int hitstat = ranged ? moxie : ( KoLCharacter.getAdjustedMuscle() - ml );
		double minHitPercent = hitPercent( hitstat, minHit );
		double maxHitPercent = hitPercent( hitstat, maxHit );
		int minPerfectHit = perfectHit( hitstat, minHit );
		int maxPerfectHit = perfectHit( hitstat, maxHit );
		double minEvadePercent = hitPercent( moxie, minEvade );
		double maxEvadePercent = hitPercent( moxie, maxEvade );
		int minPerfectEvade = perfectHit( moxie, minEvade );
		int maxPerfectEvade = perfectHit( moxie, maxEvade );

		StringBuffer buffer = new StringBuffer();
		buffer.append( "Hit: " );
		buffer.append( ff.format( minHitPercent ) + "%-" + ff.format( maxHitPercent ) + "%");
		buffer.append( " (" + ( ranged ? "Moxie" : "Muscle" ) + " " );
		buffer.append( minPerfectHit + "/" + maxPerfectHit + ") " );
		buffer.append( "Evade: " );
		buffer.append( ff.format( minEvadePercent ) + "%-" + ff.format( maxEvadePercent ) + "%" );
		buffer.append( " (Moxie " );
		buffer.append( minPerfectEvade + "/" + maxPerfectEvade + ") " );

		return buffer.toString();
	}

	public double hitPercent( int attack, int defense )
	{
		// ( (Attack - Defense) / 18 ) * 100 + 50 = Hit% 
		double percent = 100.0 * ( attack - defense ) / 18 + 50.0;
		if ( percent < 0.0 )
			return 0.0;
		if ( percent > 100.0 )
			return 100.0;
		return percent;
	}

	public int perfectHit( int attack, int defense )
	{	return attack - defense - 9;
        }

	private static final AdventureResult ARIA = new AdventureResult( "Ur-Kel's Aria of Annoyance", 0 );
	private static final int ICE_SICKLE = 1424;
	private static final int HIPPO_WHIP = 1029;
	private static final int GIANT_NEEDLE = 619;
	private static final int GOTH_KID = 703;
	private static final int HOCKEY_STICK = 1236;
	private static final int SCARF = 1227;
	private static final int AGGRAVATE_MONSTER = 835;
	private static final int PITCHFORK = 1116;

	public static int monsterLevelAdjustment()
	{
		int ml = KoLCharacter.getMindControlLevel();

		for ( int slot = KoLCharacter.WEAPON; slot <= KoLCharacter.FAMILIAR; ++slot )
		{
			if ( slot == KoLCharacter.PANTS )
				continue;

			AdventureResult item = KoLCharacter.getCurrentEquipment( slot );
			if ( item == null )
				continue;

			switch ( item.getItemID() )
			{
			case ICE_SICKLE:
				ml += 15;
				break;
			case HIPPO_WHIP:
				ml += 10;
				break;
			case GIANT_NEEDLE:
				ml += 5;
				break;
			case GOTH_KID:
				ml += 5;
				break;
			case HOCKEY_STICK:
				ml += 30;
				break;
			case SCARF:
				ml += 20;
				break;
			case AGGRAVATE_MONSTER:
				ml += 5;
				break;
			case PITCHFORK:
				ml += 5;
				break;
			}
		}

		// Effects: Aria of Annoyance
		if ( KoLCharacter.getEffects().contains( ARIA ) )
			ml += 2 * KoLCharacter.getLevel();

		return ml;
	}
}
