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

package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.AdventureFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BountyHunterHunterRequest
	extends CoinMasterRequest
{
	public static final String master = "Bounty Hunter Hunter"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( BountyHunterHunterRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( BountyHunterHunterRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have.*?<b>([\\d,]+)</b> filthy lucre" );
	public static final AdventureResult LUCRE = ItemPool.get( ItemPool.LUCRE, 1 );
	public static final CoinmasterData BHH =
		new CoinmasterData(
			BountyHunterHunterRequest.master,
			"hunter",
			BountyHunterHunterRequest.class,
			"bounty.php",
			"lucre",
			"You don't have any filthy lucre",
			false,
			BountyHunterHunterRequest.TOKEN_PATTERN,
			BountyHunterHunterRequest.LUCRE,
			null,
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			"howmany",
			GenericRequest.HOWMANY_PATTERN,
			"buy",
			BountyHunterHunterRequest.buyItems,
			BountyHunterHunterRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			null
			);

	public BountyHunterHunterRequest()
	{
		super( BountyHunterHunterRequest.BHH );
	}

	public BountyHunterHunterRequest( final String action )
	{
		super( BountyHunterHunterRequest.BHH, action );
	}

	public BountyHunterHunterRequest( final String action, final AdventureResult [] attachments )
	{
		super( BountyHunterHunterRequest.BHH, action, attachments );
	}

	public BountyHunterHunterRequest( final String action, final AdventureResult attachment )
	{
		super( BountyHunterHunterRequest.BHH, action, attachment );
	}

	public BountyHunterHunterRequest( final String action, final int itemId, final int quantity )
	{
		super( BountyHunterHunterRequest.BHH, action, itemId, quantity );
	}

	public BountyHunterHunterRequest( final String action, final int itemId )
	{
		super( BountyHunterHunterRequest.BHH, action, itemId );
	}

	@Override
	public void processResults()
	{
		BountyHunterHunterRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private static final Pattern EASY_PATTERN = Pattern.compile( "Easy Bounty!  Come back when you've collected (\\d+) (.*?) from" );
	private static final Pattern HARD_PATTERN = Pattern.compile( "Hard Bounty!  Come back when you've collected (\\d+) (.*?) from" );
	private static final Pattern SPECIAL_PATTERN = Pattern.compile( "Specialty Bounty!  Come back when you've collected (\\d+) (.*?) from" );
	private static final Pattern UNTAKEN_EASY_PATTERN = Pattern.compile( "Easy Bounty:.*?/itemimages/(.*?) width.*?>(\\d+) (.*?) from.*?takelow" );
	private static final Pattern UNTAKEN_HARD_PATTERN = Pattern.compile( "Hard Bounty:.*?/itemimages/(.*?) width.*?>(\\d+) (.*?) from.*?takehigh" );
	private static final Pattern UNTAKEN_SPECIAL_PATTERN = Pattern.compile( "Specialty Bounty:.*?/itemimages/(.*?) width.*?>(\\d+) (.*?) from.*?takespecial" );
	private static final Pattern EASY_QTY_PATTERN = Pattern.compile( "Easy Bounty.*?You have collected (\\d+) .*?giveup_low" );
	private static final Pattern HARD_QTY_PATTERN = Pattern.compile( "Hard Bounty.*?You have collected (\\d+) .*?giveup_high" );
	private static final Pattern SPECIAL_QTY_PATTERN = Pattern.compile( "Specialty Bounty.*?You have collected (\\d+) .*?giveup_spe" );

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = BountyHunterHunterRequest.BHH;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			BountyHunterHunterRequest.parseEasy( responseText );
			BountyHunterHunterRequest.parseHard( responseText );
			BountyHunterHunterRequest.parseSpecial( responseText );
			return;
		}
		
		if ( action.equals( "takelow" ) )
		{
			String currentUntakenBounty = Preferences.getString( "_untakenEasyBountyItem" );
			if ( !currentUntakenBounty.equals( "" ) )
			{
				Preferences.setString( "currentEasyBountyItem", currentUntakenBounty + ":0" );
				Preferences.setString( "_untakenEasyBountyItem", "" );
			}

			String bountyLocation = BountyDatabase.getLocation( currentUntakenBounty );
			if ( bountyLocation != null )
			{
				KoLAdventure adventure = AdventureDatabase.getAdventure( bountyLocation );
				if ( adventure != null )
				{
					AdventureFrame.updateSelectedAdventure( adventure );
				}
			}
			return;
		}

		if ( action.equals( "takehigh" ) )
		{
			String currentUntakenBounty = Preferences.getString( "_untakenHardBountyItem" );
			if ( !currentUntakenBounty.equals( "" ) )
			{
				Preferences.setString( "currentHardBountyItem", currentUntakenBounty + ":0" );
				Preferences.setString( "_untakenHardBountyItem", "" );
			}

			String bountyLocation = BountyDatabase.getLocation( currentUntakenBounty );
			if ( bountyLocation != null )
			{
				KoLAdventure adventure = AdventureDatabase.getAdventure( bountyLocation );
				if ( adventure != null )
				{
					AdventureFrame.updateSelectedAdventure( adventure );
				}
			}
			return;
		}

		if ( action.equals( "takespecial" ) )
		{
			String currentUntakenBounty = Preferences.getString( "_untakenSpecialBountyItem" );
			if ( !currentUntakenBounty.equals( "" ) )
			{
				Preferences.setString( "currentSpecialBountyItem", currentUntakenBounty + ":0" );
				Preferences.setString( "_untakenSpecialBountyItem", "" );
			}

			String bountyLocation = BountyDatabase.getLocation( currentUntakenBounty );
			if ( bountyLocation != null )
			{
				KoLAdventure adventure = AdventureDatabase.getAdventure( bountyLocation );
				if ( adventure != null )
				{
					AdventureFrame.updateSelectedAdventure( adventure );
				}
			}
			return;
		}

		if ( action.equals( "giveup_low" ) )
		{
			Preferences.setString( "currentEasyBountyItem", "" );
			return;
		}
		
		if ( action.equals( "giveup_high" ) )
		{
			Preferences.setString( "currentHardBountyItem", "" );
			return;
		}
		
		if ( action.equals( "giveup_spe" ) )
		{
			Preferences.setString( "currentSpecialBountyItem", "" );
			return;
		}
		
		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	private static final void parseEasy( final String responseText )
	{
		Matcher bountyItemMatcher = BountyHunterHunterRequest.EASY_PATTERN.matcher( responseText );

		if ( !bountyItemMatcher.find() )
		{
			Preferences.setString( "currentEasyBountyItem", "" );
			Matcher bountyUntakenMatcher = BountyHunterHunterRequest.UNTAKEN_EASY_PATTERN.matcher( responseText );
			
			if( bountyUntakenMatcher.find() )
			{
				String plural = bountyUntakenMatcher.group( 3 );
				String bountyItem = BountyDatabase.getName( plural );
				if ( bountyItem != null )
				{
					Preferences.setString( "_untakenEasyBountyItem", bountyItem );
				}
				else
				{
					Preferences.setString( "_unknownEasyBountyItem", bountyUntakenMatcher.group( 1 ) + ":" +
						bountyUntakenMatcher.group( 2 ) + ":" + bountyUntakenMatcher.group( 3 ) );
				}
			}
			else
			{
				Preferences.setString( "_untakenEasyBountyItem", "" );
			}
			return;
		}

		String plural = bountyItemMatcher.group( 2 );
		String bountyItem = BountyDatabase.getName( plural );

		Matcher bountyQtyMatcher = BountyHunterHunterRequest.EASY_QTY_PATTERN.matcher( responseText );

		int bountyQty;
		if ( !bountyQtyMatcher.find() )
		{
			bountyQty = 0;
		}
		else
		{
			bountyQty = StringUtilities.parseInt( bountyQtyMatcher.group( 1 ) );
		}
		if ( bountyItem != null )
		{
			Preferences.setString( "currentEasyBountyItem", bountyItem + ":" + bountyQty );
		}
	}

	private static final void parseHard( final String responseText )
	{
		Matcher bountyItemMatcher = BountyHunterHunterRequest.HARD_PATTERN.matcher( responseText );

		if ( !bountyItemMatcher.find() )
		{
			Preferences.setString( "currentHardBountyItem", "" );
			Matcher bountyUntakenMatcher = BountyHunterHunterRequest.UNTAKEN_HARD_PATTERN.matcher( responseText );
			
			if( bountyUntakenMatcher.find() )
			{
				String plural = bountyUntakenMatcher.group( 2 );
				String bountyItem = BountyDatabase.getName( plural );
				if ( bountyItem != null )
				{
					Preferences.setString( "_untakenHardBountyItem", bountyItem );
				}
				else
				{
					Preferences.setString( "_unknownHardBountyItem", bountyUntakenMatcher.group( 1 ) + ":" +
						bountyUntakenMatcher.group( 2 ) + ":" + bountyUntakenMatcher.group( 3 ) );
				}
			}
			else
			{
				Preferences.setString( "_untakenHardBountyItem", "" );
			}
			return;
		}

		String plural = bountyItemMatcher.group( 2 );
		String bountyItem = BountyDatabase.getName( plural );
		
		Matcher bountyQtyMatcher = BountyHunterHunterRequest.HARD_QTY_PATTERN.matcher( responseText );

		int bountyQty;
		if ( !bountyQtyMatcher.find() )
		{
			bountyQty = 0;
		}
		else
		{
			bountyQty = StringUtilities.parseInt( bountyQtyMatcher.group( 1 ) );
		}

		if ( bountyItem != null )
		{
			Preferences.setString( "currentHardBountyItem", bountyItem + ":" + bountyQty );
		}
	}

	private static final void parseSpecial( final String responseText )
	{
		Matcher bountyItemMatcher = BountyHunterHunterRequest.SPECIAL_PATTERN.matcher( responseText );

		if ( !bountyItemMatcher.find() )
		{
			Preferences.setString( "currentSpecialBountyItem", "" );
			Matcher bountyUntakenMatcher = BountyHunterHunterRequest.UNTAKEN_SPECIAL_PATTERN.matcher( responseText );
			
			if( bountyUntakenMatcher.find() )
			{
				String plural = bountyUntakenMatcher.group( 2 );
				String bountyItem = BountyDatabase.getName( plural );
				if ( bountyItem != null )
				{
					Preferences.setString( "_untakenSpecialBountyItem", bountyItem );
				}
				else
				{
					Preferences.setString( "_unknownSpecialBountyItem", bountyUntakenMatcher.group( 1 ) + ":" +
						bountyUntakenMatcher.group( 2 ) + ":" + bountyUntakenMatcher.group( 3 ) );
				}
			}
			else
			{
				Preferences.setString( "_untakenSpecialBountyItem", "" );
			}
			return;
		}

		String plural = bountyItemMatcher.group( 2 );
		String bountyItem = BountyDatabase.getName( plural );
		
		Matcher bountyQtyMatcher = BountyHunterHunterRequest.SPECIAL_QTY_PATTERN.matcher( responseText );

		int bountyQty;
		if ( !bountyQtyMatcher.find() )
		{
			bountyQty = 0;
		}
		else
		{
			bountyQty = StringUtilities.parseInt( bountyQtyMatcher.group( 1 ) );
		}

		if ( bountyItem != null )
		{
			Preferences.setString( "currentSpecialBountyItem", bountyItem + ":" + bountyQty );
		}
	}

	private static final Pattern BOUNTY_ITEM_PATTERN =
		Pattern.compile( "itemimages/(.*?) width=30 height=30></td><td align=center>You acquire a bounty item: <b>(.*?)</b></td></tr></table>\\((\\d+) of" );

	public static final void parseFight( final String monster, final String location, final String responseText )
	{
		// First cut down responseText to the last image before the bounty, assumes image name is less than 30 characters,
		// and that another won't be seen in that time
		int imageIndex = responseText.indexOf( ".gif width=30 height=30></td><td align=center>You acquire a bounty item" );
		String reducedResponseText = responseText;
		if ( imageIndex > 30 )
		{
			reducedResponseText = responseText.substring( imageIndex - 30 );
		}
		
		// If known bounty item we can set the preference correctly based on number found so far
		Matcher bountyItemMatcher = BountyHunterHunterRequest.BOUNTY_ITEM_PATTERN.matcher( reducedResponseText );
		if( bountyItemMatcher.find() )
		{
			String bountyItem = bountyItemMatcher.group( 2 );
			int bountyCount = StringUtilities.parseInt( bountyItemMatcher.group( 3 ) );
			String bountyType = BountyDatabase.getType( bountyItem );

			if ( bountyType == null )
			{
				String bountyImage = bountyItemMatcher.group( 1 );
				// Try to work out what the item should be
				String unknownEasyBountyString = Preferences.getString( "_unknownEasyBountyItem" );
				String unknownHardBountyString = Preferences.getString( "_unknownHardBountyItem" );
				String unknownSpecialBountyString = Preferences.getString( "_unknownSpecialBountyItem" );
				if ( !unknownEasyBountyString.equals( "" ) )
				{
					int bountyIndex = unknownEasyBountyString.indexOf( ":" );
					int bountyIndex2 = unknownEasyBountyString.indexOf( ":", bountyIndex + 1 );
					String unknownBountyImage = unknownEasyBountyString.substring( 0, bountyIndex );
					int unknownBountyNumber = StringUtilities.parseInt( unknownEasyBountyString.substring( bountyIndex + 1, bountyIndex2 ) );
					String unknownBountyPlural = unknownEasyBountyString.substring( bountyIndex2 + 1 );
					if ( bountyImage.equals( unknownBountyImage ) )
					{
						// Looks like a match !
						BountyDatabase.setValue( bountyItem, unknownBountyPlural, "easy", unknownBountyImage,
							unknownBountyNumber, monster, location );
						Preferences.setString( "currentEasyBountyItem", bountyItem + ":" + bountyCount );
						Preferences.setString( "_unknownEasyBountyItem", "" );
					}
				}
				else if ( !unknownHardBountyString.equals( "" ) )
				{
					int bountyIndex = unknownHardBountyString.indexOf( ":" );
					int bountyIndex2 = unknownHardBountyString.indexOf( ":", bountyIndex + 1 );
					String unknownBountyImage = unknownHardBountyString.substring( 0, bountyIndex );
					int unknownBountyNumber = StringUtilities.parseInt( unknownHardBountyString.substring( bountyIndex + 1, bountyIndex2 ) );
					String unknownBountyPlural = unknownHardBountyString.substring( bountyIndex2 + 1 );
					if ( bountyImage.equals( unknownBountyImage ) )
					{
						// Looks like a match !
						BountyDatabase.setValue( bountyItem, unknownBountyPlural, "easy", unknownBountyImage,
							unknownBountyNumber, monster, location );
						Preferences.setString( "currentHardBountyItem", bountyItem + ":" + bountyCount );
						Preferences.setString( "_unknownHardBountyItem", "" );
					}
				}
				else if ( !unknownSpecialBountyString.equals( "" ) )
				{
					int bountyIndex = unknownSpecialBountyString.indexOf( ":" );
					int bountyIndex2 = unknownSpecialBountyString.indexOf( ":", bountyIndex + 1 );
					String unknownBountyImage = unknownSpecialBountyString.substring( 0, bountyIndex );
					int unknownBountyNumber = StringUtilities.parseInt( unknownSpecialBountyString.substring( bountyIndex + 1, bountyIndex2 ) );
					String unknownBountyPlural = unknownSpecialBountyString.substring( bountyIndex2 + 1 );
					if ( bountyImage.equals( unknownBountyImage ) )
					{
						// Looks like a match !
						BountyDatabase.setValue( bountyItem, unknownBountyPlural, "easy", unknownBountyImage,
							unknownBountyNumber, monster, location );
						Preferences.setString( "currentSpecialBountyItem", bountyItem + ":" + bountyCount );
						Preferences.setString( "_unknownSpecialBountyItem", "" );
					}
				}
				else
				{
					KoLmafia.updateDisplay( "Bounty Item " + bountyItem + " not yet known to KoLMafia." );
				}
			}
			else if ( bountyType.equals( "easy" ) )
			{
				Preferences.setString( "currentEasyBountyItem", bountyItem + ":" + bountyCount );
			}
			else if ( bountyType.equals( "hard" ) )
			{
				Preferences.setString( "currentHardBountyItem", bountyItem + ":" + bountyCount );
			}
			else if ( bountyType.equals( "special" ) )
			{
				Preferences.setString( "currentSpecialBountyItem", bountyItem + ":" + bountyCount );
			}
			String updateMessage = "You acquire a bounty item: " + bountyItem;
			AdventureResult result = AdventureResult.tallyItem( bountyItem, false );
			AdventureResult.addResultToList( KoLConstants.tally, result );
			RequestLogger.updateSessionLog( updateMessage );
			KoLmafia.updateDisplay( updateMessage );
		}
	}
	
	public static String accessible()
	{
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "bounty.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Visiting the Bounty Hunter Hunter" );
			return true;
		}

		if ( action.equals( "takelow" ) )
		{
			String bountyName = Preferences.getString( "_untakenEasyBountyItem" );
			if ( bountyName.equals( "" ) )
			{
				String bounty = Preferences.getString( "_unknownEasyBountyItem" );
				if ( !bounty.equals( "" ) )
				{
					int bountyIndex = bounty.indexOf( ":" );
					int bountyIndex2 = bounty.indexOf( ":", bountyIndex + 1 );
					int bountyNumber = StringUtilities.parseInt( bounty.substring( bountyIndex + 1, bountyIndex2 ) );
					String plural = bounty.substring( bountyIndex2 + 1 );
					RequestLogger.updateSessionLog();
					RequestLogger.updateSessionLog( "accept unknown easy bounty assignment to collect " + bountyNumber + " " + plural );
				}
				else
				{
					RequestLogger.printLine( "no easy bounty accepted" );
				}
				return true;
			}
			int bountyNumber = BountyDatabase.getNumber( bountyName );
			String plural = BountyDatabase.getPlural( bountyName );
			
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "accept easy bounty assignment to collect " + bountyNumber + " " + plural );
			
			return true;
		}

		if ( action.equals( "takehigh" ) )
		{
			String bountyName = Preferences.getString( "_untakenHardBountyItem" );
			if ( bountyName.equals( "" ) )
			{
				String bounty = Preferences.getString( "_unknownHardBountyItem" );
				if ( !bounty.equals( "" ) )
				{
					int bountyIndex = bounty.indexOf( ":" );
					int bountyIndex2 = bounty.indexOf( ":", bountyIndex + 1 );
					int bountyNumber = StringUtilities.parseInt( bounty.substring( bountyIndex + 1, bountyIndex2 ) );
					String plural = bounty.substring( bountyIndex2 + 1 );
					RequestLogger.updateSessionLog();
					RequestLogger.updateSessionLog( "accept unknown hard bounty assignment to collect " + bountyNumber + " " + plural );
				}
				else
				{
					RequestLogger.printLine( "no hard bounty accepted" );
				}
				return true;
			}
			int bountyNumber = BountyDatabase.getNumber( bountyName );
			String plural = BountyDatabase.getPlural( bountyName );
			
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "accept hard bounty assignment to collect " + bountyNumber + " " + plural );
			
			return true;
		}

		if ( action.equals( "takespecial" ) )
		{
			String bountyName = Preferences.getString( "_untakenSpecialBountyItem" );
			if ( bountyName.equals( "" ) )
			{
				String bounty = Preferences.getString( "_unknownSpecialBountyItem" );
				if ( !bounty.equals( "" ) )
				{
					int bountyIndex = bounty.indexOf( ":" );
					int bountyIndex2 = bounty.indexOf( ":", bountyIndex + 1 );
					int bountyNumber = StringUtilities.parseInt( bounty.substring( bountyIndex + 1, bountyIndex2 ) );
					String plural = bounty.substring( bountyIndex2 + 1 );
					RequestLogger.updateSessionLog();
					RequestLogger.updateSessionLog( "accept unknown speciality bounty assignment to collect " + bountyNumber + " " + plural );
				}
				else
				{
					RequestLogger.printLine( "no speciality bounty accepted" );
				}
				return true;
			}
			int bountyNumber = BountyDatabase.getNumber( bountyName );
			String plural = BountyDatabase.getPlural( bountyName );
			
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "accept specialty bounty assignment to collect " + bountyNumber + " " + plural );
			
			return true;
		}

		if ( action.equals( "giveup_low" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "abandon easy bounty assignment" );
			return true;
		}

		if ( action.equals( "giveup_high" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "abandon hard bounty assignment" );
			return true;
		}

		if ( action.equals( "giveup_spe" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "abandon special bounty assignment" );
			return true;
		}

		CoinmasterData data = BountyHunterHunterRequest.BHH;
		return CoinMasterRequest.registerRequest( data, urlString );
	}
}
