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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.session.BuffBotManager.Offering;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BuffBotDatabase
	extends KoLDatabase
{
	public static final String OPTOUT_URL = "http://forums.kingdomofloathing.com/";

	private static final Pattern BUFFDATA_PATTERN = Pattern.compile( "<buffdata>(.*?)</buffdata>", Pattern.DOTALL );
	private static final Pattern NAME_PATTERN = Pattern.compile( "<name>(.*?)</name>", Pattern.DOTALL );
	private static final Pattern PRICE_PATTERN = Pattern.compile( "<price>(.*?)</price>", Pattern.DOTALL );
	private static final Pattern TURN_PATTERN = Pattern.compile( "<turns>(.*?)</turns>", Pattern.DOTALL );
	private static final Pattern FREE_PATTERN =
		Pattern.compile( "<philanthropic>(.*?)</philanthropic>", Pattern.DOTALL );

	private static boolean hasNameList = false;
	private static boolean isInitialized = false;

	private static final ArrayList<String> nameList = new ArrayList<String>();
	private static final TreeMap<String, String[]> buffDataMap = new TreeMap<String, String[]>();

	private static final TreeMap<String, LockableListModel<Offering>> normalOfferings = new TreeMap<String, LockableListModel<Offering>>();
	private static final TreeMap<String, LockableListModel<Offering>> freeOfferings = new TreeMap<String, LockableListModel<Offering>>();

	public static final int getOffering( String name, final int amount )
	{
		// If you have no idea what the names present in
		// the database are, go ahead and refresh it.

		if ( !BuffBotDatabase.hasNameList )
		{
			String[] data;
			BufferedReader reader = FileUtilities.getVersionedReader( "buffbots.txt", KoLConstants.BUFFBOTS_VERSION );

			while ( ( data = FileUtilities.readData( reader ) ) != null )
			{
				if ( data.length >= 2 )
				{
					ContactManager.registerPlayerId( data[ 0 ], data[ 1 ] );
	
					BuffBotDatabase.nameList.add( data[ 0 ].toLowerCase() );
					BuffBotDatabase.buffDataMap.put( data[ 0 ].toLowerCase(), data );
				}
			}
			BuffBotDatabase.hasNameList = true;

			try
			{
				reader.close();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}
		}

		// If the player is not a known buffbot, go ahead
		// and allow the amount.

		name = ContactManager.getPlayerName( name ).toLowerCase();
		if ( !BuffBotDatabase.nameList.contains( name ) )
		{
			return amount;
		}

		// Otherwise, retrieve the information for the buffbot
		// to see if there are non-philanthropic offerings.

		String[] data = (String[]) BuffBotDatabase.buffDataMap.get( name );

		if ( data[ 2 ].equals( BuffBotDatabase.OPTOUT_URL ) )
		{
			KoLmafia.updateDisplay(
				MafiaState.ABORT, data[ 0 ] + " has requested to be excluded from scripted requests." );
			return 0;
		}

		RequestThread.runInParallel( new DynamicBotFetcher( data ), false );

		// If this is clearly not a philanthropic buff, then
		// no alternative amount needs to be sent.

		LockableListModel<Offering> possibles = BuffBotDatabase.getPhilanthropicOfferings( data[ 0 ] );
		if ( possibles == null || possibles.isEmpty() )
		{
			return amount;
		}

		Offering current = null;
		boolean foundMatch = false;

		for ( int i = 0; i < possibles.size() && !foundMatch; ++i )
		{
			current = possibles.get( i );
			if ( current.getPrice() == amount )
			{
				foundMatch = true;
			}
		}

		if ( !foundMatch )
		{
			return amount;
		}

		// If this offers more than 300 turns, chances are it's not
		// a philanthropic buff.  Buff packs are also not protected
		// because the logic is complicated.

		if ( current.buffs == null || current.buffs.length > 1 )
		{
			return amount;
		}

		// If no alternative exists, go ahead and return the
		// original amount.

		LockableListModel<Offering> alternatives = BuffBotDatabase.getStandardOfferings( data[ 0 ] );
		if ( alternatives == null || alternatives.isEmpty() )
		{
			return amount;
		}

		String matchBuff = current.buffs[ 0 ];
		int matchTurns = current.turns[ 0 ];

		String testBuff = null;
		int testTurns = 0;

		Offering bestMatch = null;
		int bestTurns = 0;

		// Search for the best match, which is defined as the
		// buff which provides the closest number of turns.

		for ( int i = 0; i < alternatives.size(); ++i )
		{
			current = alternatives.get( i );

			if ( current.buffs.length > 1 )
			{
				continue;
			}

			testBuff = current.buffs[ 0 ];
			testTurns = current.turns[ 0 ];

			if ( !matchBuff.equals( testBuff ) )
			{
				continue;
			}

			if ( bestMatch == null || testTurns >= matchTurns && testTurns < bestTurns )
			{
				bestMatch = current;
				bestTurns = testTurns;
			}
		}

		if ( bestMatch == null )
		{
			return amount;
		}

		if ( KoLConstants.activeEffects.contains( EffectPool.get( bestMatch.buffs[ 0 ] ) ) )
		{
			return 0;
		}

		KoLmafia.updateDisplay( "Converted to non-philanthropic request: " +
			bestMatch.turns[ 0 ] + " turns of " + bestMatch.buffs[ 0 ] +
			" for " + bestMatch.getPrice() + " Meat." );
		return bestMatch.getPrice();
	}

	public static final boolean hasOfferings()
	{
		if ( !BuffBotDatabase.isInitialized )
		{
			BuffBotDatabase.configureBuffBots();
		}

		return !BuffBotDatabase.normalOfferings.isEmpty() || !BuffBotDatabase.freeOfferings.isEmpty();
	}

	public static final String[] getCompleteBotList()
	{
		ArrayList<String> completeList = new ArrayList<String>();
		completeList.addAll( BuffBotDatabase.normalOfferings.keySet() );

		for ( String bot : BuffBotDatabase.freeOfferings.keySet() )
		{
			if ( !completeList.contains( bot ) )
			{
				completeList.add( bot );
			}
		}

		Collections.sort( completeList, String.CASE_INSENSITIVE_ORDER );
		completeList.add( 0, "" );

		return completeList.toArray( new String[0] );
	}

	public static final LockableListModel<Offering> getStandardOfferings( final String botName )
	{
		return botName != null && BuffBotDatabase.normalOfferings.containsKey( botName ) ?
			BuffBotDatabase.normalOfferings.get( botName ) :
			new LockableListModel<Offering>();
	}

	public static final LockableListModel<Offering> getPhilanthropicOfferings( final String botName )
	{
		return botName != null && BuffBotDatabase.freeOfferings.containsKey( botName ) ?
			BuffBotDatabase.freeOfferings.get( botName ) :
			new LockableListModel<Offering>();
	}

	private static final void configureBuffBots()
	{
		if ( BuffBotDatabase.isInitialized )
		{
			return;
		}

		KoLmafia.updateDisplay( "Configuring dynamic buff prices..." );

		String[] data = null;
		BufferedReader reader = FileUtilities.getVersionedReader( "buffbots.txt", KoLConstants.BUFFBOTS_VERSION );

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length == 3 )
			{
				new DynamicBotFetcher( data ).run();
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

		KoLmafia.updateDisplay( "Buff prices fetched." );
		BuffBotDatabase.isInitialized = true;
	}

	private static class DynamicBotFetcher
		implements Runnable
	{
		private final String botName, location;

		public DynamicBotFetcher( final String[] data )
		{
			this.botName = data[ 0 ];
			this.location = data[ 2 ];

			ContactManager.registerPlayerId( data[ 0 ], data[ 1 ] );
		}

		public void run()
		{
			if ( BuffBotDatabase.freeOfferings.containsKey( this.botName ) || BuffBotDatabase.normalOfferings.containsKey( this.botName ) )
			{
				return;
			}

			if ( this.location.equals( BuffBotDatabase.OPTOUT_URL ) )
			{
				BuffBotDatabase.freeOfferings.put( this.botName, new LockableListModel<Offering>() );
				BuffBotDatabase.normalOfferings.put( this.botName, new LockableListModel<Offering>() );

				return;
			}

			StringBuilder responseText = new StringBuilder();
			BufferedReader reader = FileUtilities.getReader( this.location );

			if ( reader == null )
			{
				return;
			}

			try
			{
				String line;
				while ( ( line = reader.readLine() ) != null )
				{
					responseText.append( line );
				}
			}
			catch ( Exception e )
			{
				return;
			}

			// Now, for the infamous XML parse tree.  Rather than building
			// a tree (which would probably be smarter), simply do regular
			// expression matching and assume we have a properly-structured
			// XML file -- which is assumed because of the XSLT.

			Matcher nodeMatcher = BuffBotDatabase.BUFFDATA_PATTERN.matcher( responseText.toString() );
			LockableListModel<Offering> freeBuffs = new LockableListModel<Offering>();
			LockableListModel<Offering> normalBuffs = new LockableListModel<Offering>();

			Matcher nameMatcher, priceMatcher, turnMatcher, freeMatcher;

			while ( nodeMatcher.find() )
			{
				String buffMatch = nodeMatcher.group( 1 );

				nameMatcher = BuffBotDatabase.NAME_PATTERN.matcher( buffMatch );
				priceMatcher = BuffBotDatabase.PRICE_PATTERN.matcher( buffMatch );
				turnMatcher = BuffBotDatabase.TURN_PATTERN.matcher( buffMatch );
				freeMatcher = BuffBotDatabase.FREE_PATTERN.matcher( buffMatch );

				if ( nameMatcher.find() && priceMatcher.find() && turnMatcher.find() )
				{
					String name = nameMatcher.group( 1 ).trim();

					if ( name.startsWith( "Jaba" ) )
					{
						name = SkillDatabase.getSkillName( SkillPool.JABANERO_SAUCESPHERE );
					}
					else if ( name.startsWith( "Jala" ) )
					{
						name = SkillDatabase.getSkillName( SkillPool.JALAPENO_SAUCESPHERE );
					}

					int price = StringUtilities.parseInt( priceMatcher.group( 1 ).trim() );
					int turns = StringUtilities.parseInt( turnMatcher.group( 1 ).trim() );
					boolean philanthropic = freeMatcher.find() ? freeMatcher.group( 1 ).trim().equals( "true" ) : false;

					LockableListModel<Offering> tester = philanthropic ? freeBuffs : normalBuffs;

					Offering priceMatch = null;
					Offering currentTest = null;

					for ( int i = 0; i < tester.size(); ++i )
					{
						currentTest = (Offering) tester.get( i );
						if ( currentTest.getPrice() == price )
						{
							priceMatch = currentTest;
						}
					}

					if ( priceMatch == null )
					{
						tester.add( new Offering( name, this.botName, price, turns, philanthropic ) );
					}
					else
					{
						priceMatch.addBuff( name, turns );
					}
				}
			}

			// If the bot offers some philanthropic buffs, then
			// add them to the philanthropic bot list.

			if ( !freeBuffs.isEmpty() )
			{
				freeBuffs.sort();
				BuffBotDatabase.freeOfferings.put( this.botName, freeBuffs );
			}

			if ( !normalBuffs.isEmpty() )
			{
				normalBuffs.sort();
				BuffBotDatabase.normalOfferings.put( this.botName, normalBuffs );
			}
		}
	}
}
