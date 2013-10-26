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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.IslandManager;
import net.sourceforge.kolmafia.session.IslandManager.Quest;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class IslandRequest
	extends GenericRequest
{
	private static final Pattern OPTION_PATTERN = Pattern.compile( "option=(\\d+)" );

	public static final AdventureResult GUNPOWDER = ItemPool.get( ItemPool.GUNPOWDER, 1 );

	public static final String[][] HIPPY_CONCERTS =
	{
		{ "Moon'd", "+5 Stat(s) Per Fight" },
		{ "Dilated Pupils", "Item Drop +20%" },
		{ "Optimist Primal", "Familiar Weight +5" },
	};

	public static final String[][] FRATBOY_CONCERTS =
	{
		{ "Elvish", "All Attributes +10%" },
		{ "Winklered", "Meat Drop +40%" },
		{ "White-boy Angst", "Initiative +50%" },
	};

	private static int effectToConcertNumber( final String completer, final String effect )
	{
		String [][] array =
			completer.equals( "hippies" ) ?
			HIPPY_CONCERTS :
			completer.equals( "fratboys" ) ?
			FRATBOY_CONCERTS :
			null;

		if ( array == null )
		{
			return 0;
		}

		String compare = effect.toLowerCase();
		for ( int i = 0; i < array.length; ++i )
		{
			if ( array[i][0].toLowerCase().startsWith( compare ) )
			{
				return i + 1;
			}
		}

		return 0;
	}

	private Quest quest;

	public IslandRequest()
	{
		super( IslandManager.currentIsland() );
		this.quest = Quest.NONE;
	}

	public static IslandRequest getConcertRequest( final int option )
	{
		IslandRequest request = new IslandRequest();

		if ( request.getPath().equals( "bogus.php" ) )
		{
			return null;
		}

		if ( option < 0 || option > 3 )
		{
			return null;
		}

		request.quest = Quest.ARENA;

		request.addFormField( "action", "concert" );
		request.addFormField( "option", String.valueOf( option ) );

		return request;
	}

	public static IslandRequest getConcertRequest( final String effect )
	{
		String completer = IslandManager.questCompleter( "sidequestArenaCompleted" );
		int option = IslandRequest.effectToConcertNumber( completer, effect );
		return ( option == 0 ) ? null : IslandRequest.getConcertRequest( option );
	}

	public static String concertError( final String arg )
	{
		if ( IslandManager.warProgress().equals( "unstarted" ) )
		{
			return "You have not started the island war yet.";
		}

		String completer = IslandManager.questCompleter( "sidequestArenaCompleted" );
		if ( completer.equals( "none" ) )
		{
			return "The arena is not open.";
		}

		String loser = Preferences.getString( "sideDefeated" );
		if ( loser.equals( completer ) || loser.equals( "both" ) )
		{
			return "The arena's fans were defeated in the war.";
		}

		if ( Character.isDigit( arg.charAt( 0 ) ) )
		{
			// Raw concert number
			int option = StringUtilities.parseInt( arg );
			if ( option < 0 || option > 3 )
			{
				return "Invalid concert number.";
			}
		}
		else
		{
			// Effect name
			int option = IslandRequest.effectToConcertNumber( completer, arg );
			if ( option == 0 )
			{
				return "The \"" + arg + "\" effect is not available to " + completer;
			}
		}

		return "";
	}

	public static IslandRequest getPyroRequest()
	{
		IslandRequest request = new IslandRequest();

		if ( request.getPath().equals( "bogus.php" ) )
		{
			return null;
		}

		request.quest = Quest.LIGHTHOUSE;
		request.addFormField( "place", "lighthouse" );
		request.addFormField( "action", "pyro" );

		return request;
	}

	public static final String getPyroURL()
	{
		return IslandManager.currentIsland() + "?place=lighthouse&action=pyro";
	}

	public static IslandRequest getFarmerRequest()
	{
		IslandRequest request = new IslandRequest();

		if ( request.getPath().equals( "bogus.php" ) )
		{
			return null;
		}

		request.quest = Quest.FARM;
		request.addFormField( "place", "farm" );
		request.addFormField( "action", "farmer" );

		return request;
	}

	public static IslandRequest getNunneryRequest()
	{
		IslandRequest request = new IslandRequest();

		if ( request.getPath().equals( "bogus.php" ) )
		{
			return null;
		}

		request.quest = Quest.NUNS;
		request.addFormField( "place", "nunnery" );
		request.addFormField( "action", "nuns" );

		return request;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		switch ( this.quest )
		{
		case ARENA:
			KoLmafia.updateDisplay( "Visiting the Mysterious Island Arena..." );
			break;
		case LIGHTHOUSE:
			KoLmafia.updateDisplay( "Visiting the Lighthouse Keeper..." );
			break;
		case FARM:
			KoLmafia.updateDisplay( "Visiting the Farmer..." );
			break;
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		if ( this.responseText == null || this.responseText.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't find the Mysterious Island." );
			return;
		}

		IslandRequest.parseResponse( this.getURLString(), this.responseText );

		switch ( this.quest )
		{
		case ARENA:
			// Unfortunately, you think you've pretty much tapped out this
			// event's entertainment potential for today
			//
			// You're all rocked out.

			if ( this.responseText.contains( "pretty much tapped out" ) ||
			     this.responseText.contains( "You're all rocked out" ) )
			{
				KoLmafia.updateDisplay( "You can only visit the Mysterious Island Arena once a day." );
				return;
			}

			// The stage at the Mysterious Island Arena is empty
			if ( this.responseText.contains( "The stage at the Mysterious Island Arena is empty" ) )
			{
				KoLmafia.updateDisplay( "Nobody is performing." );
				return;
			}

			KoLmafia.updateDisplay( "A music lover is you." );
			break;

		case LIGHTHOUSE:
			KoLmafia.updateDisplay( "Done visiting the Lighthouse Keeper." );
			break;

		case FARM:
			KoLmafia.updateDisplay( "Done visiting the Farmer." );
			break;
		}
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		// Let the Island Manager deduce things about the state of the
		// island based on the responseText
		IslandManager.parseIsland( location, responseText );

		// Do things that depend on actual actions

		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			return;
		}

		if ( action.equals( "action=concert" ) )
		{
			Preferences.setBoolean( "concertVisited", true );
			return;
		}

		if ( action.equals( "action=pyro" ) )
		{
			// "The Lighthouse Keeper's eyes light up as he sees your
			// gunpowder.<p>&quot;Big boom!	 Big big boom!	Give me those,
			// <i>bumpty-bump</i>, and I'll make you the big
			// boom!&quot;<p>He takes the gunpowder into a back room, and
			// returns with an armload of big bombs."
			if ( responseText.contains( "eyes light up" ) )
			{
				int count = IslandRequest.GUNPOWDER.getCount( KoLConstants.inventory );
				ResultProcessor.processItem( ItemPool.GUNPOWDER, -count );
			}
			return;
		}
	}

	private static final Pattern CAMP_PATTERN = Pattern.compile( "whichcamp=(\\d+)" );
	public static CoinmasterData findCampMaster( final String urlString )
	{
		Matcher campMatcher = IslandRequest.CAMP_PATTERN.matcher( urlString );
		if ( !campMatcher.find() )
		{
			return null;
		}

		String camp = campMatcher.group(1);

		if ( camp.equals( "1" ) )
		{
			return DimemasterRequest.HIPPY;
		}

		if ( camp.equals( "2" ) )
		{
			return QuartersmasterRequest.FRATBOY;
		}

		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "bigisland.php" ) && !urlString.startsWith( "postwarisland.php" ) )
		{
			return false;
		}

		if ( urlString.startsWith( "bigisland.php" ) )
		{
			// You can only visit the two camps during the war
			CoinmasterData data = IslandRequest.findCampMaster( urlString );
			if ( data != null )
			{
				return CoinMasterRequest.registerRequest( data, urlString );
			}
			return false;
		}

		String action = GenericRequest.getAction( urlString );

		if ( action == null )
		{
			return false;
		}

		String message = null;

		if ( action.equals( "concert" ) )
		{
			Matcher matcher = OPTION_PATTERN.matcher( urlString );
			if ( !matcher.find() )
			{
				return false;
			}
			message = "concert " + matcher.group( 1 );
		}
		else if ( action.equals( "pyro" ) )
		{
			int count = IslandRequest.GUNPOWDER.getCount( KoLConstants.inventory );
			message = "Visiting the lighthouse keeper with " + count + " barrel" + ( count == 1 ? "" : "s" ) + " of gunpowder.";
			RequestLogger.printLine( message );
		}
		else
		{
			return false;
		}

		RequestLogger.updateSessionLog( message );

		return true;
	}
}
