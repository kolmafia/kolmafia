/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;


public class BanishManager
{
	private static final ArrayList<BanishedMonster> banishedMonsters = new ArrayList<BanishedMonster>();

	private static class Banisher
	{
		final String name;
		final int duration;
		final int queueSize;
		
		public Banisher( final String name, final int duration, final int queueSize )
		{
			this.name = name;
			this.duration = duration;
			this.queueSize = queueSize;
		}
		
		public final String getName()
		{
			return this.name;
		}
		
		public final int getDuration()
		{
			return this.duration;
		}
		
		public final int getQueueSize()
		{
			return this.queueSize;
		}
	}
	
	// Format is name of banisher, duration of banisher (999 resets at rollover, 9999 resets at ascension/avatar drop),
	// how many monsters can be banished at once from this source.
	private static final Banisher[] BANISHER = new Banisher[]
	{
		new Banisher( "crystal skull", 20, 1 ),
		new Banisher( "divine champagne popper", 5, 1 ),
		new Banisher( "nanorhino", 999, 1 ),
		new Banisher( "harold's bell", 20, 1 ),
		new Banisher( "batter up!", 999, 1 ),
		new Banisher( "v for vivala mask", 10, 1 ),
		new Banisher( "stinky cheese eye", 10, 1 ),
		new Banisher( "pantsgiving", 30, 1 ),
		new Banisher( "pulled indigo taffy", 20, 1 ),
		new Banisher( "banishing shout", 9999, 3 ),
		new Banisher( "howl of the alpha", 9999, 3 ),
		new Banisher( "classy monkey", 20, 1 ),
		new Banisher( "staff of the standalone cheese", 999, 5 ),
		new Banisher( "dirty stinkbomb",999, 1 ),
		new Banisher( "deathchucks", 999, 1 ),
		new Banisher( "cocktail napkin", 20, 1 ),
		new Banisher( "chatterboxing", 20, 1 ),
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
			if ( banishDuration >= 999 || turnBanished + banishDuration >= KoLCharacter.getCurrentRun() )
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
			if ( BanishManager.findBanisher( current.getBanishName() ).getDuration() == 999 )
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
			if ( BanishManager.findBanisher( current.getBanishName() ).getDuration() == 9999 )
			{
				it.remove();
			}
		}

		BanishManager.saveBanishedMonsters();
	}

	public static final void resetAscension()
	{
		BanishManager.banishedMonsters.clear();
		Preferences.setString( "banishedMonsters", "" );
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
			if ( banisherDuration < 999 && current.getTurnBanished() + banisherDuration <= KoLCharacter.getCurrentRun() )
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
		BanishManager.addBanishedMonster( monsterName, banishName, KoLCharacter.getCurrentRun() + 1 );
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
			if ( current.getMonsterName().equals( monster ) )
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
}
