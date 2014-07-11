/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;


public class BanishManager
{
	private static final ArrayList<BanishedMonster> banishedMonsters = new ArrayList<BanishedMonster>();

	private enum Reset
	{
		TURN_RESET,
		ROLLOVER_RESET,
		AVATAR_RESET,
		NEVER_RESET,
	}
	
	private static class Banisher
	{
		final String name;
		final int duration;
		final int queueSize;
		final boolean isTurnFree;
		final Reset resetType;
		
		public Banisher( final String name, final int duration, final int queueSize, final boolean isTurnFree, final Reset resetType )
		{
			this.name = name;
			this.duration = duration;
			this.queueSize = queueSize;
			this.isTurnFree = isTurnFree;
			this.resetType = resetType;
		}
		
		public final String getName()
		{
			return this.name;
		}
		
		public final int getDuration()
		{
			// returns actual duration of banish after the turn used, which varies depending if that turn is free
			int turnCost = this.isTurnFree ? 0 : 1;
			return this.duration - turnCost;
		}
		
		public final int getQueueSize()
		{
			return this.queueSize;
		}
		
		public final boolean isTurnFree()
		{
			return this.isTurnFree;
		}

		public final Reset getResetType()
		{
			return this.resetType;
		}
	}
	
	// Format is name of banisher, duration of banisher, how many monsters can be banished at once from this source,
	// whether banish is turn free, type of reset.
	private static final Banisher[] BANISHER = new Banisher[]
	{
		new Banisher( "banishing shout", -1, 3, false, Reset.AVATAR_RESET ),
		new Banisher( "batter up!", -1, 1, false, Reset.ROLLOVER_RESET ),
		new Banisher( "chatterboxing", 20, 1, true, Reset.TURN_RESET ),
		new Banisher( "classy monkey", 20, 1, false, Reset.TURN_RESET ),
		new Banisher( "cocktail napkin", 20, 1, true, Reset.TURN_RESET ),
		new Banisher( "crystal skull", 20, 1, false, Reset.TURN_RESET ),
		new Banisher( "deathchucks", -1, 1, true, Reset.ROLLOVER_RESET ),
		new Banisher( "dirty stinkbomb",-1, 1, true, Reset.ROLLOVER_RESET ),
		new Banisher( "divine champagne popper", 5, 1, true, Reset.TURN_RESET ),
		new Banisher( "harold's bell", 20, 1, false, Reset.TURN_RESET ),
		new Banisher( "howl of the alpha", -1, 3, false, Reset.AVATAR_RESET ),
		new Banisher( "ice house", -1, 1, false, Reset.NEVER_RESET ),
		new Banisher( "louder than bomb", 20, 1, true, Reset.TURN_RESET ),
		new Banisher( "nanorhino", -1, 1, false, Reset.ROLLOVER_RESET ),
		new Banisher( "pantsgiving", 30, 1, false, Reset.TURN_RESET ),
		new Banisher( "peel out", -1, 1, true, Reset.AVATAR_RESET ),
		new Banisher( "pulled indigo taffy", 20, 1, true, Reset.TURN_RESET ),
		new Banisher( "smoke grenade", 20, 1, false, Reset.TURN_RESET ),
		new Banisher( "spooky music box mechanism", -1, 1, false, Reset.ROLLOVER_RESET ),
		new Banisher( "staff of the standalone cheese", -1, 5, false, Reset.AVATAR_RESET ),
		new Banisher( "stinky cheese eye", 10, 1, true, Reset.TURN_RESET ),
		new Banisher( "v for vivala mask", 10, 1, true, Reset.TURN_RESET ),
		new Banisher( "walk away from explosion", 30, 1, false, Reset.TURN_RESET ),
	};

	private static class BanishedMonster
	{
		final String monsterName;
		final String banishName;
		final int turnBanished;

		public BanishedMonster( final String monsterName, final String banishName, final int turnBanished )
		{
			this.monsterName = monsterName;
			this.banishName = banishName;
			this.turnBanished = turnBanished;
		}

		public final String getMonsterName()
		{
			return this.monsterName;
		}

		public final String getBanishName()
		{
			return this.banishName;
		}

		public final int getTurnBanished()
		{
			return this.turnBanished;
		}
	}

	public static final void clearCache()
	{
		BanishManager.banishedMonsters.clear();
	}

	public static final void loadBanishedMonsters()
	{
		BanishManager.banishedMonsters.clear();

		String banishes = Preferences.getString( "banishedMonsters" );
		if ( banishes.length() == 0 )
		{
			return;
		}

		StringTokenizer tokens = new StringTokenizer( banishes, ":" );
		while ( tokens.hasMoreTokens() )
		{
			String monsterName = tokens.nextToken();
			if ( !tokens.hasMoreTokens() ) break;
			String banishName = tokens.nextToken();
			if ( !tokens.hasMoreTokens() ) break;
			int turnBanished = StringUtilities.parseInt( tokens.nextToken() );
			int banishDuration = BanishManager.findBanisher( banishName ).getDuration();
			Reset resetType = BanishManager.findBanisher( banishName ).getResetType();
			if ( resetType != Reset.TURN_RESET || turnBanished + banishDuration >= KoLCharacter.getCurrentRun() )
			{
				BanishManager.addBanishedMonster( monsterName, banishName, turnBanished );
			}
		}
	}

	public static final void saveBanishedMonsters()
	{
		BanishManager.recalculate();
		
		StringBuilder banishString = new StringBuilder();
		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();
		
		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();

			if ( banishString.length() > 0 )
			{
				banishString.append( ":" );
			}

			banishString.append( current.monsterName );
			banishString.append( ":" );
			banishString.append( current.banishName );
			banishString.append( ":" );
			banishString.append( current.turnBanished );
		}

		Preferences.setString( "banishedMonsters", banishString.toString() );
	}

	public static final void resetRollover()
	{
		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();
			
			if ( BanishManager.findBanisher( current.getBanishName() ).getResetType() == Reset.ROLLOVER_RESET )
			{
				it.remove();
			}
		}

		BanishManager.saveBanishedMonsters();
	}

	public static final void resetAvatar()
	{
		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();

			if ( BanishManager.findBanisher( current.getBanishName() ).getResetType() == Reset.AVATAR_RESET )
			{
				it.remove();
			}
		}

		BanishManager.saveBanishedMonsters();
	}

	public static final void resetAscension()
	{
		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();

			if ( BanishManager.findBanisher( current.getBanishName() ).getResetType() != Reset.NEVER_RESET )
			{
				it.remove();
			}
		}

		BanishManager.saveBanishedMonsters();
	}

	public static final void update()
	{
		BanishManager.recalculate();
		BanishManager.saveBanishedMonsters();
	}

	private static final void recalculate()
	{
		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();
			int banisherDuration = BanishManager.findBanisher( current.getBanishName() ).getDuration();
			Reset resetType = BanishManager.findBanisher( current.getBanishName() ).getResetType();
			if ( resetType == Reset.TURN_RESET && current.getTurnBanished() + banisherDuration <= KoLCharacter.getCurrentRun() )
			{
				it.remove();
			}
		}
	}

	public static final Banisher findBanisher( final String banisher )
	{
		for ( int i = 0 ; i < BANISHER.length ; i++ )
		{
			if ( BANISHER[ i ].getName().equals( banisher ) )
			{
				return BANISHER[ i ];
			}
		}
		return null;
	}

	public static final void banishMonster( final String monsterName, final String banishName )
	{
		KoLmafia.updateDisplay( monsterName + " banished by " + banishName + "." );
		if ( BanishManager.countBanishes( banishName ) >= BanishManager.findBanisher( banishName ).getQueueSize() )
		{
			BanishManager.removeOldestBanish( banishName );
		}
		int turnCost = BanishManager.findBanisher( banishName ).isTurnFree() ? 0 : 1;
		BanishManager.addBanishedMonster( monsterName, banishName, KoLCharacter.getCurrentRun() + turnCost );
		BanishManager.saveBanishedMonsters();
		
		// Legacy support
		if ( banishName.equals( "nanorhino" ) )
		{
			Preferences.setString( "_nanorhinoBanishedMonster", monsterName );
		}
		else if ( banishName.equals( "banishing shout" ) || banishName.equals( "howl of the alpha" ) )
		{
			String pref = monsterName;
			String[] monsters = Preferences.getString( "banishingShoutMonsters" ).split( "\\|" );
			for ( int i = 0; i < monsters.length && i < 2; ++i )
			{
				if ( monsters[ i ].length() > 0 )
				{
					pref += "|" + monsters[ i ];
				}
			}
			Preferences.setString( "banishingShoutMonsters", pref );
		}
		else if ( banishName.equals( "staff of the standalone cheese" ) )
		{
			String pref = monsterName;
			String[] monsters = Preferences.getString( "_jiggleCheesedMonsters" ).split( "\\|" );
			for ( int i = 0; i < monsters.length; ++i )
			{
				if ( monsters[ i ].length() > 0 )
				{
					pref += "|" + monsters[ i ];
				}
			}
			Preferences.setString( "_jiggleCheesedMonsters", pref );
		}
	}
	
	private static final void addBanishedMonster( final String monsterName, final String banishName, final int turnBanished )
	{
		BanishedMonster newBanishedMonster = new BanishedMonster( monsterName, banishName, turnBanished );
		if ( !BanishManager.banishedMonsters.contains( newBanishedMonster ) )
		{
			BanishManager.banishedMonsters.add( newBanishedMonster );
		}
	}

	public static final void removeBanishByBanisher( final String banisher )
	{
		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();
			if ( current.getBanishName().equals( banisher ) )
			{
				it.remove();
			}
		}

		BanishManager.saveBanishedMonsters();
	}
	
	public static final void removeBanishByMonster( final String monster )
	{
		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();
			if ( current.getMonsterName().equals( monster ) )
			{
				it.remove();
			}
		}

		BanishManager.saveBanishedMonsters();
	}
	
	public static final void removeOldestBanish( final String banisher )
	{
		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();
		String target = null;
		int earliest = -1;
		
		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();
			if ( current.getBanishName().equals( banisher ) )
			{
				if ( earliest == -1 || current.getTurnBanished() < earliest )
				{
					target = current.getMonsterName();
					earliest = current.getTurnBanished();
				}
			}
		}

		if ( target != null )
		{
			BanishManager.removeBanishByMonster( target );
		}
	}
	
	public static final boolean isBanished( final String monster )
	{
		BanishManager.recalculate();

		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();
			if ( current.getMonsterName().equalsIgnoreCase( monster ) )
			{
				return true;
			}
		}
		return false;
	}

	private static final int countBanishes( final String banisher )
	{
		int banishCount = 0;
		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();
			if ( current.getBanishName().equals( banisher ) )
			{
				banishCount++;
			}
		}
		return banishCount;
	}

	public static final String getBanishList()
	{
		BanishManager.recalculate();

		StringBuilder banishList = new StringBuilder();
		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

		while ( it.hasNext() )
		{
			BanishedMonster current = it.next();

			if ( banishList.length() > 0 )
			{
				banishList.append( "," );
			}

			banishList.append( current.monsterName );
		}

		return banishList.toString();
	}

	public static final String[][] getBanishData()
	{
		BanishManager.recalculate();

		Iterator<BanishedMonster> it = BanishManager.banishedMonsters.iterator();

		int banish = 0;
		int count = BanishManager.banishedMonsters.size();
		
		if ( count > 0 )
		{
			String[][] banishData = new String[ count ][ 4 ];
			while ( it.hasNext() )
			{
				BanishedMonster current = it.next();
				banishData[ banish ][ 0 ] = current.monsterName;
				banishData[ banish ][ 1 ] = current.banishName;
				banishData[ banish ][ 2 ] = String.valueOf( current.turnBanished );
				int banisherDuration = BanishManager.findBanisher( current.banishName ).getDuration();
				Reset resetType = BanishManager.findBanisher( current.banishName ).getResetType();
				if ( resetType == Reset.TURN_RESET )
				{
					banishData[ banish ][ 3 ] = String.valueOf( current.turnBanished + banisherDuration - KoLCharacter.getCurrentRun() );
				}
				else if ( resetType == Reset.ROLLOVER_RESET )
				{
					banishData[ banish ][ 3 ] = "Until Rollover";
				}
				else if ( resetType == Reset.AVATAR_RESET )
				{
					banishData[ banish ][ 3 ] = "Until Prism Break";
				}
				else if ( resetType == Reset.NEVER_RESET )
				{
					banishData[ banish ][ 3 ] = "Until Ice House opened";
				}
				banish++;
			}
			return banishData;
		}

		return null;
	}
}
