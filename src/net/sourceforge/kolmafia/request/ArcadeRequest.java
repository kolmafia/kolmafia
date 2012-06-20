/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ArcadeRequest
	extends GenericRequest
{
	public static final AdventureResult TOKEN = ItemPool.get( ItemPool.GG_TOKEN, 1 );

	private String action = null;

	public ArcadeRequest()
	{
		super( "arcade.php" );
	}

	public ArcadeRequest( final String action )
	{
		super( "arcade.php" );
		this.action = action;
		this.addFormField( "action", action );
	}

	public static final int getTurnsUsed( GenericRequest request )
	{
		String action = request.getFormField( "action" );
		if ( action == null || !action.equals( "game" ) )
		{
			return 0;
		}

		String game = request.getFormField( "whichgame" );
		if ( game != null && 
		     ( game.equals( "1" ) ||
		       game.equals( "2" ) ||
		       game.equals( "3" ) ||
		       game.equals( "4" ) ||
		       game.equals( "5" ) ) )
		{
			return 5;
		}

		return 0;
	}

	private static final Pattern GAME_PATTERN = Pattern.compile( "whichgame=(\\d+)" );

	private static final int getGame( final String urlString  )
	{
		Matcher matcher = ArcadeRequest.GAME_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group(1) ) : 0;
	}

	@Override
	public void processResults()
	{
		ArcadeRequest.parseResponse( this.getURLString(), this.responseText );

		if ( this.action == null )
		{
			return;
		}

		if ( this.action.equals( "skeeball" ) )
		{
			// You don't have any Game Grid tokens, so you can't
			// play Skee-Ball. But don't feel bad. The Skee-Ball
			// machine is broken, so you wouldn't have been able to
			// play Skee-Ball anyway.
			if ( this.responseText.indexOf( "You don't have any Game Grid tokens" ) != -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have any Game Grid tokens." );
			}
			else
			{
				KoLmafia.updateDisplay( "Token transformed into tickets." );
			}
		}
	}

	private static final Pattern ITEM_PATTERN = Pattern.compile( "name=whichitem value=([\\d]+)>.*?descitem.([\\d]+).*?<b>([^<&]*)(?:&nbsp;)*</td>.*?<b>([\\d,]+)</b>", Pattern.DOTALL );

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "arcade.php" ) )
		{
			return;
		}

		if ( urlString.indexOf( "ticketcounter=1" ) != -1 )
		{
			// Learn new trade items by simply visiting Arcade
			Matcher matcher = ITEM_PATTERN.matcher( responseText );
			while ( matcher.find() )
			{
				int id = StringUtilities.parseInt( matcher.group(1) );
				String desc = matcher.group(2);
				String name = matcher.group(3);
				String data = ItemDatabase.getItemDataName( id );
				// String price = matcher.group(4);
				if ( data == null || !data.equals( name ) )
				{
					ItemDatabase.registerItem( id, name, desc );
				}
			}
			return;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return;
		}

		if ( action.equals( "skeeball" ) )
		{
			// You don't have any Game Grid tokens, so you can't
			// play Skee-Ball. But don't feel bad. The Skee-Ball
			// machine is broken, so you wouldn't have been able to
			// play Skee-Ball anyway.
			if ( responseText.indexOf( "you can't play Skee-Ball" ) == -1 )
			{
				ResultProcessor.processItem( ItemPool.GG_TOKEN, -1 );
			}
			return;
		}

		if ( TicketCounterRequest.parseResponse( urlString, responseText ) )
		{
			return;
		}

		if ( !action.equals( "game" ) )
		{
			return;
		}

		int game = ArcadeRequest.getGame( urlString );
		switch ( game )
		{
		case 1: // Space Trip
		case 2:	// DemonStar
		case 3:	// Dungeon Fist!
		case 4:	// Fighters of Fighting
		case 5:	// Meteoid
		case 6:	// Jackass Plumber
			// These games only take tokens, and you don't have any

			// If we succeed in playing a game, we were redirected
			// to a choice adventure and never get here. Therefore,
			// deduct tokens in registerRequest

			if ( responseText.indexOf( "These games only take tokens" ) == -1 )
			{
				// The game also took 5 turns
			}
			break;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "arcade.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		String message = null;

		if ( action != null )
		{
			if ( TicketCounterRequest.registerRequest( urlString ) )
			{
				return true;
			}

			// Other actions of interest require tokens. Do we have
			// any?

			int count = TOKEN.getCount( KoLConstants.inventory );
			if ( count < 1 )
			{
				return true;
			}

			if ( action.equals( "skeeball" ) )
			{
				message = "Visiting Broken Skee-Ball Machine";
			}
			else if ( action.equals( "game" ) )
			{
				int game = ArcadeRequest.getGame( urlString );
				String name = null;
				switch ( game )
				{
				case 1: // Space Trip
					name = "Space Trip";
					break;
				case 2:	// DemonStar
					name = "DemonStar";
					break;
				case 3:	// Dungeon Fist!
					name = "Dungeon Fist!";
					break;
				case 4:	// Fighters of Fighting
					name = "The Fighters of Fighting";
					break;
				case 5:	// Meteoid
					name = "Meteoid";
					break;
				case 6:	// Jackass Plumber
					name = "Jackass Plumber";
					break;
				default:
					return false;
				}

				if ( KoLCharacter.getAdventuresLeft() < 5 )
				{
					return true;
				}

				// We have a token and 5 adventures.
				message = "[" + KoLAdventure.getAdventureCount() + "] " + name;

				// Deduct the token here
				ResultProcessor.processItem( ItemPool.GG_TOKEN, -1 );
			}
		}
		else if ( urlString.indexOf( "ticketcounter=1" ) != -1 )
		{
			message = "Visiting Ticket Redemption Counter";
		}

		if ( message == null )
		{
			return false;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}

	public static final boolean arcadeChoice( final int choice )
	{
		// Do not look for "Encounters" inside arcade choices
		switch ( choice )
		{
		case 460: case 461: case 462: case 463: case 464:
		case 465:	    case 467: case 468: case 469:
		case 470:	    case 472: case 473: case 474:
		case 475: case 476: case 477: case 478: case 479:
		case 480: case 481: case 482: case 483: case 484:
			// Space Trip
		case 471:
			// DemonStar
		case 485:
			// Fighters Of Fighting
		case 486:
			// Dungeon Fist!
		case 488: case 489: case 490: case 491:
			// Meteoid
			return true;
		}

		return false;
	}

	/*
	 * Support for individual games
	 */

	private static final void logText( final String text )
	{
		RequestLogger.printLine( text );
		RequestLogger.updateSessionLog( text );
	}

	private final static Pattern CHOICE_PATTERN = Pattern.compile( "<form.*?name='?option'? value='?(\\d+)'?.*?>.*?class='?button'?.*?value=['\"](.*?)['\"].*?></form>", Pattern.DOTALL );

	// The strings tagging each available choice with associated index
	private static Integer [] indices = null;
	private static String [] choices = null;

	private static void parseChoiceNames( final String responseText )
	{
		ArrayList options = new ArrayList();
		ArrayList names = new ArrayList();
		Matcher matcher = CHOICE_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			options.add( new Integer( matcher.group( 1 ) ) );
			names.add( StringUtilities.globalStringReplace( matcher.group( 2 ), "&nbsp;", "" ) );
		}
		ArcadeRequest.indices = new Integer [ options.size() ];
		options.toArray( ArcadeRequest.indices );
		ArcadeRequest.choices = new String [ names.size() ];
		names.toArray( ArcadeRequest.choices );
	}

	private static String findChoiceName( final int index )
	{
		if ( indices != null && choices != null )
		{
			for ( int i = 0; i < indices.length; ++i )
			{
				if ( indices[i].intValue() == index )
				{
					return choices[i];
				}
			}
		}
		return null;
	}

	/* Space Trip */

	private static int week;
	private static int crew;
	private static int crewLost;
	private static int money;
	private static int gas;
	private static int time;

	public static final void visitSpaceTripChoice( final String responseText )
	{
		// Called when we visit Space Trip

		ArcadeRequest.week = 0;
		ArcadeRequest.crew = 30;
		ArcadeRequest.crewLost = 0;
		ArcadeRequest.money = 0;
		ArcadeRequest.gas = 100;
		ArcadeRequest.time = 52;

		// Parse out the choice names
		ArcadeRequest.parseChoiceNames( responseText );
	}

	public static final void logSpaceTripAction( final String responseText )
	{
		// Called when we are about to take a choice in SpaceTrip
		String action = ArcadeRequest.findChoiceName( ChoiceManager.lastDecision );
		if ( action == null )
		{
			return;
		}

                boolean week = false;
                boolean log = false;

		// Don't log navigation around the space ship or base
		switch ( ChoiceManager.lastChoice )
		{
		case 468:	// Starbase Hub
			if ( !action.equals( "Visit the General Store" ) &&
			     !action.equals( "Visit the Military Surplus Store" ) &&
			     !action.equals( "Back to Navigation Console" ) )
			{
				// Log any purchases
				log = true;
			}
			break;
		case 469:	// General Store
		case 470:	// Military Surplus Store
			if ( !action.equals( "Back to Starbase Hub" ) )
			{
				// Log any purchases
				log = true;
			}
			break;
		case 461:	// Navigation
			if ( !action.equals( "Back to the Bridge" ) )
			{
				// Log sector selection and starbase
				log = true;
			}
			break;
		case 460:	// Bridge
		case 462:	// Diagnostics
                        // Game control navigation
			break;

		case 463:	// Alpha Quadrant
		case 464:	// Beta Quadrant
		case 477:	// Gamma Quadrant
			// Exploring in a Quadrant advances time
			if ( action.startsWith( "Launch an Astrozorian Commerce Grenade" ) ||
			     action.startsWith( "Investigate the Source" ) )
			{
				log = true;
			}
			else if ( action.indexOf( "Scadian Homeworld" ) != -1  &&
				  responseText.indexOf( "Protector of Scadia" ) != -1 )
			{
				log = true;
			}
			else if ( !action.startsWith( "Back to Navigation" ) )
			{
				week = true;
			}
			break;
		default:
			// Log the action the player took
			log = true;
			break;
		}

                if ( week )
                {
			ArcadeRequest.week++;
			ArcadeRequest.logText( "Week " + ArcadeRequest.week + ": " + action );
                }

                if ( log )
                {
                        ArcadeRequest.logText( "Action: " + action );
                }
	}

	private final static Pattern SPACE_TRIP_RESOURCE_PATTERN = Pattern.compile( "<tr><td><b>Crew:</b>&nbsp;(\\d*)<br><b>Gas:</b>&nbsp;(\\d*)&nbsp;gal.</td><td width=50></td><td><b>Money:</b>&nbsp;([0123456789,]*)&nbsp;Crabs<br><b>Time&nbsp;Left:</b>&nbsp;(\\d*)&nbsp;weeks</td></tr>", Pattern.DOTALL );

	private final static Pattern CRABS_PATTERN = Pattern.compile( "have (?:transferred|recovered) <b>([0123456789,]*).*?Crabs</b>", Pattern.DOTALL );

	private final static Pattern CREW1_PATTERN = Pattern.compile( "(?:We lost )?([0123456789,]+) crew members(?: were lost)?", Pattern.DOTALL );
	private final static Pattern CREW2_PATTERN = Pattern.compile( "([0123456789,]+) (?:sentient )?beings", Pattern.DOTALL );

	private final static Pattern TOTAL_SCORE_PATTERN = Pattern.compile( "Total Score:.*?<b>([0123456789,]*)</b>", Pattern.DOTALL );

	public static final void postChoiceSpaceTrip( final GenericRequest request )
	{
		// Called when we have taken a choice in SpaceTrip

		// Log what we see
		String responseText = request.responseText;

		// Log action appropriately
		ArcadeRequest.logSpaceTripAction( responseText );

		// If ten of your crew members have time to come to our big
		// party, I'm sure it would be wonderful for everybody!
		//
		// The biggest parties happen here!

		if ( responseText.indexOf( "come to our big party" ) != -1 ||
		     responseText.indexOf( "The biggest parties happen here" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Slavers" );
		}

		// Biological scanners show no signs of organic life, but the
		// ship is broadcasting an identity signal: MINE-29-DEATH-149
		// -- how shall we approach it, sir?
		//
		// The Murderbots at this colony must be programmed to kill
		// intruders on sight.

		else if ( responseText.indexOf( "no signs of organic life" ) != -1 ||
			  responseText.indexOf( "Murderbots at this colony" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Murderbot Mining Ship" );
		}

		// Captain, we've discovered something interesting -- it is
		// definitely a Murderbot vessel, but it has no weapons, and is
		// equipped with a much larger than usual communications array.

		else if ( responseText.indexOf( "much larger than usual communications array" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Murderbot Control Ship" );
		}

		// Captain, we've been ambushed by another Murderbot
		// vessel. Get ready for a fight!

		else if ( responseText.indexOf( "ambushed by another Murderbot vessel" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Murderbot Cruiser" );
		}

		// Captain, we've neared the Scadian homeworld, and it is
		// currently under siege by a Murderbot Dreadnought.  Which...
		// has detected us, apparently, and is now shooting at us.
		//
		// Captain, as soon as we got near the Murderhive, we were
		// immediately attacked by this Dreadnought. We're not going to
		// be able to get inside there without getting this ID
		// Transmitter fixed.

		else if ( responseText.indexOf( "under siege by a Murderbot Dreadnought" ) != -1 || 
			  responseText.indexOf( "attacked by this Dreadnought" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Murderbot Dreadnought" );
		}

		// Captain, I've got good news and bad news. The good news is
		// that the ID Transmitter worked on the Dreadnoughts, and
		// allowed us to fly into the center of the Murderhive. The bad
		// news is that the Murderbot Mothership's computers didn't
		// fall for the trick, and now we're in some serious, serious
		// trouble.

		else if ( responseText.indexOf( "the Murderbot Mothership's computers didn't" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Murderbot Mothership" );
		}

		// Hail to thee, noble traveler!  I come a great distance, at
		// great peril, in search of aid for my Scadian countrymen.
		// Will you lend me your ear, good wanderer?

		else if ( responseText.indexOf( "aid for my Scadian countrymen" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Scadian Ship" );
			ArcadeRequest.week--;
		}

		// Captain, we're being hailed by a Hipsterian vessel.

		else if ( responseText.indexOf( "being hailed by a Hipsterian vessel" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Hipsterian Ship" );
		}

		// Ello, sah baldy. Mebbe mi can help yuh wid someting?
		//

		else if ( responseText.indexOf( "Ello, sah baldy" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Astrozorian Trade Vessel" );
		}

		// Captain, it's... If... If it wasn't so evil, it would
		// be... beautiful. They would have should have sent a poet.

		else if ( responseText.indexOf( "They should have sent a poet" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: The Source" );
		}

		// We lost 5 crew members in the attack...
		// 3 crew members were lost...
		Matcher crewMatcher = CREW1_PATTERN.matcher( responseText );
		if ( crewMatcher.find() )
		{
			int crew = StringUtilities.parseInt( crewMatcher.group(1) );
			ArcadeRequest.crewLost += crew;
			ArcadeRequest.logText( "You lost " + KoLConstants.COMMA_FORMAT.format( crew ) + " crew members" );
		}

		// We have launched the mineral payload in the direction of the
		// CHOAD company's nearest drop station, and they have
		// transferred <b>405&nbsp;Crabs</b> to your account, Captain.
		//
		// We have uploaded the biological data into the ASPCA
		// mainframe, and they have transferred <b>388&nbsp;Crabs</b>
		// to your account, Captain.
		//
		// Our salvage teams have recovered <b>322 Crabs</b> worth of
		// parts from the wreckage of the mining drone

		Matcher crabsMatcher = CRABS_PATTERN.matcher( responseText );
		if ( crabsMatcher.find() )
		{
			int crabs = StringUtilities.parseInt( crabsMatcher.group(1) );
			ArcadeRequest.logText( "You gain " + KoLConstants.COMMA_FORMAT.format( crabs ) + " Crabs" );
		}

		// Captain, we've managed to extract an intact stasis enclosure
		// from the wreck of the enemy ship, and it contained 15
		// sentient beings.  We've thawed them out, and they've
		// decided, in their gratitude, to join our crew.
		//
		// Captain, we've managed to rescue 24 beings from stasis pods
		// floating in the wreckage of the enemy ship.

		crewMatcher = CREW2_PATTERN.matcher( responseText );
		if ( crewMatcher.find() )
		{
			int crew = StringUtilities.parseInt( crewMatcher.group(1) );
			ArcadeRequest.logText( "You rescue " + KoLConstants.COMMA_FORMAT.format( crew ) + " crew members" );
		}

		// Look at current resources
		Matcher resourceMatcher = SPACE_TRIP_RESOURCE_PATTERN.matcher( responseText );
		if ( resourceMatcher.find() )
		{
			int crew = StringUtilities.parseInt( resourceMatcher.group(1) );
			int gas = StringUtilities.parseInt( resourceMatcher.group(2) );
			int money = StringUtilities.parseInt( resourceMatcher.group(3) );
			int time = StringUtilities.parseInt( resourceMatcher.group(4) );

			if ( crew != ArcadeRequest.crew ||
			     money != ArcadeRequest.money ||
			     gas != ArcadeRequest.gas ||
			     time != ArcadeRequest.time )
			{
				ArcadeRequest.logText( "Crew: " + KoLConstants.COMMA_FORMAT.format( crew ) +
						       ". Gas: " + KoLConstants.COMMA_FORMAT.format( gas ) +
						       " gallons. Money: " + KoLConstants.COMMA_FORMAT.format( money ) +
						       " Crabs. Time left: " + KoLConstants.COMMA_FORMAT.format( time ) +
						       " weeks.");
				ArcadeRequest.crew = crew;
				ArcadeRequest.money = money;
				ArcadeRequest.gas = gas;
				ArcadeRequest.time = time;
			}
		}

		// Finally, see if the game is over
		Matcher totalMatcher = ArcadeRequest.TOTAL_SCORE_PATTERN.matcher( responseText );
		if ( totalMatcher.find() )
		{
			ArcadeRequest.logText( "Total Score: " + totalMatcher.group(1) );

			// The game is over. No more choices.
			ArcadeRequest.indices = null;
			ArcadeRequest.choices = null;
			return;
		}

		// Parse out the new choice names
		ArcadeRequest.parseChoiceNames( responseText );
	}

	/* End Space Trip */
	/* DemonStar */

	private static int blurstite;
	private static int wounds;

	public static final void visitDemonStarChoice( final String responseText )
	{
		// Called when we visit DemonStar
		// Parse out the choice names
		ArcadeRequest.parseChoiceNames( responseText );
		ArcadeRequest.blurstite = 0;
		ArcadeRequest.wounds = 0;
	}

	private final static Pattern DEMONSTAR_MOVE_PATTERN = Pattern.compile( "mv=([-01]+)(,|%2C)([-01]+)" );

	private static final String parseDemonStarAction( final GenericRequest request )
	{
		String action = ArcadeRequest.findChoiceName( ChoiceManager.lastDecision );
		// Actions like "Mine" or "Fight"
		if ( action != null )
		{
			return action;
		}

		Matcher matcher = ArcadeRequest.DEMONSTAR_MOVE_PATTERN.matcher( request.getURLString() );
		if ( matcher.find() )
		{
			int dx = StringUtilities.parseInt( matcher.group( 1 ) );
			int dy = StringUtilities.parseInt( matcher.group( 3 ) );
			switch( dx )
			{
			case -1:
				switch ( dy )
				{
				case -1:
					return "Move northwest";
				case 0:
					return "Move west";
				case 1:
					return "Move southwest";
				}
				break;
			case 0:
				switch ( dy )
				{
				case -1:
					return "Move north";
				case 0:
					return "Stay put";
				case 1:
					return "Move south";
				}
				break;
			case 1:
				switch ( dy )
				{
				case -1:
					return "Move northeast";
				case 0:
					return "Move east";
				case 1:
					return "Move southeast";
				}
				break;
			}
		}

		return null;
	}

	public static final void postChoiceDemonStar( final GenericRequest request )
	{
		// Called when we have taken a choice in DemonStar
		String action = ArcadeRequest.parseDemonStarAction( request );
		if ( action != null )
		{
			// Log the action the player took
			ArcadeRequest.logText( "Action: " + action );
		}

		// Log what we see
		String responseText = request.responseText;

		// Looking out the viewscreen, you see that this region of
		// space is basically empty, except for a large gray asteroid,
		// floating serenely in the... well, in the nothing.

		if ( responseText.indexOf( "a large gray asteroid" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: an asteroid" );
		}

		// Also, there's an octagonal flying saucer with a large turret
		// on the top that quickly swivels to face you.
                //
		// You are interrupted in your chosen task by the nearby
		// tanklike flying saucer, which swoops toward you, firing red
		// bursts of energy from its central turre

		if ( responseText.indexOf( "octagonal flying saucer" ) != -1 ||
		     responseText.indexOf( "nearby tanklike flying saucer" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: a warrior" );
		}

		// Also, there's a strange red crab-like spaceship -- or
		// possibly robot? -- which is poking at the asteroid with its
		// claws.

		// Also, there's a strange red crab-like spaceship -- or
		// possibly robot? -- which has a large crystal clutched in its
		// claws and seems to be on its way somewhere.

		if ( responseText.indexOf( "strange red crab-like spaceship" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: a worker" );
		}

		// Looking out the viewscreen, you see that this region of
		// space is crowded as heck. A ton of crab-like worker drones
		// are constructing a giant demon face, of all things, for the
		// front of the massive battle-station they're building. Who
		// builds a giant space demon head? This is ridiculous.

		if ( responseText.indexOf( "ton of crab-like worker drones" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: a worker" );
			ArcadeRequest.logText( "Encounter: DemonStar under construction" );
		}

		// You're fighting the DemonStar.

		if ( responseText.indexOf( "You're fighting the DemonStar" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: the DemonStar" );
		}

                // BEWARE! I LIVE!
		if ( responseText.indexOf( "BEWARE! I LIVE!" ) != -1 )
		{
			ArcadeRequest.logText( "The DemonStar awakes." );
		}

		// Blurstite crystal collected
		if ( responseText.indexOf( "Blurstite crystal collected" ) != -1 )
		{
			ArcadeRequest.blurstite++;
			ArcadeRequest.logText( "You acquire a bomb. (" + ArcadeRequest.blurstite + ")" );
		}

		// From somewhere in the distance, you hear an explosion. "A
		// blurstium charge has been intercepted by an enemy robot,"
		// the computer reports.

		if ( responseText.indexOf( "blurstium charge has been intercepted" ) != -1 )
		{
			ArcadeRequest.logText( "A bomb has been intercepted." );
		}

		// From somewhere in the distance, you hear an explosion and a
		// loud metallic roar. <i?>&quot;1 blurstium charge has located
		// the target and successfully detonated,&quot;</i> says the
		// shipboard computer.
		if ( responseText.indexOf( "a loud metallic roar" ) != -1 )
		{
			ArcadeRequest.wounds++;
			ArcadeRequest.logText( "A bomb wounds the DemonStar. (" + ArcadeRequest.wounds + ")" );
		}

		// Finally, see if the game is over
		Matcher matcher = ArcadeRequest.FINAL_SCORE_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			String message = "";
			if ( responseText.indexOf( "YOU HAVE DESTROYED THE DEMONSTAR!" ) != -1 )
			{
				message = "YOU HAVE DESTROYED THE DEMONSTAR! ";

			}

			else
			{
				message = "YOU HAVE FAILED. ";
			}

			ArcadeRequest.logText( message + matcher.group(0) );

			// The game is over. No more choices.
			ArcadeRequest.indices = null;
			ArcadeRequest.choices = null;
			return;
		}

		// Parse out the new choice names
		ArcadeRequest.parseChoiceNames( responseText );
	}

	/* End DemonStar */
	/* Dungeon Fist! */

	/*
	  +----------+    +----------+    +----------+    +----------+
	  |          |    |          |    |          |    |          |
	  |   EXIT   X----+  Grunts  +----+  MAGIC   +----+  Ghosts  |
	  |          |    |          |    |  POTION  |    |          |
	  +----------+    +-----+----+    +----------+    +-----+----+
	                        |                               |
	  +----------+    +-----+----+    +----------+    +-----+----+
	  |          |    |          |    |          |    |          |
	  |          +----+          +----+          |    |   FOOD   |
	  |          |    |          |    |          |    |          |
	  +-----+----+    +-----+----+    +-----+----+    +----------+
	        |               |               |               
	  +-----+----+    +-----+----+    +-----+----+    +----------+
	  |          |    |  Grunts  |    |          |    |          |
	  |          +----+  COMBAT  +----+  Death   +----+  Demons  |
	  |          |    |  POTION  |    |          |    |   KEY    |
	  +----------+    +-----+----+    +-----+----+    +-----+----+
	                        |               |               |
	  +----------+    +-----+----+    +-----+----+    +-----+----+
	  |          |    |          |    |          |    |          |
	  |  Demons  +----+  START   +----+          |    |          |
	  |          |    |          |    |          |    |          |
	  +-----+----+    +----------+    +-----+----+    +-----+----+
	        |                               |               |
	  +-----+----+    +----------+    +-----X----+    +-----+----+
	  |          |    |          |    |          |    |  Ghosts  |
	  |  Ghosts  +----+  MUSCLE  |    | TREASURE |    |  FOOD    |
	  |          |    |  POTION  |    |          |    |  KEY     |
	  +----------+    +----------+    +----------+    +----------+
	*/

	public static final void visitDungeonFistChoice( final String responseText )
	{
		// Called when we visit Dungeon Fist!
		// Parse out the choice names
		ArcadeRequest.parseChoiceNames( responseText );
	}

	private final static Pattern FINAL_SCORE_PATTERN = Pattern.compile( "FINAL SCORE:? ([0123456789,]*)", Pattern.DOTALL );

	public static final void postChoiceDungeonFist( final GenericRequest request )
	{
		// Called when we have taken a choice in Dungeon Fist!
		String action = ArcadeRequest.findChoiceName( ChoiceManager.lastDecision );
		if ( action != null )
		{
			// Log the action the player took
			ArcadeRequest.logText( "Action: " + action );
		}

		// Log what we see
		String responseText = request.responseText;

		// First: look for encounters

		if ( responseText.indexOf( "bright pink" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Grunts" );
		}

		// You wipe off your blade and look thoughtfully at the big
		// stone box. Then you thoughtfully decide to smash it to bits.
		//
		// Now that that's taken care of, you turn to regard the large
		// stone box. That's probably not something you want to leave
		// intact, you reckon.
		//
		// Having dealt with the monsters, you decide it's time to deal
		// with this big stone box-thing.

		else if ( responseText.indexOf( "look thoughtfully at the big stone box" ) != -1 ||
		     responseText.indexOf( "it's time to deal with this big stone box" ) != -1 ||
		     responseText.indexOf( "turn to regard the large stone box" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Large Stone Boxes" );
		}

		else if ( responseText.indexOf( "horrible demonic creatures" ) != -1 ||
		     responseText.indexOf( "fire-breathing demons" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Demons" );
		}

		else if ( responseText.indexOf( "gray spectres" ) != -1 ||
		     responseText.indexOf( "angry tormented spirits" ) != -1)
		{
			ArcadeRequest.logText( "Encounter: Ghosts" );
		}

		// Now that all the ghosts are taken care of, you decide you
		// probably shouldn't leave those piles of bones lying around.
		//
		// Finished with those mindless wraiths, you decide to clean up
		// These piles of bones. And by 'clean' I mean 'smash'.
		//
		// Having dealt with the ghosts, you decide to bust up their
		// bones as well. That'll teach 'em!
		//
		// The ghosts are all gone, but the little piles of bones
		// remain. Probably ought to do something about that.

		else if ( responseText.indexOf( "shouldn't leave those piles of bones" ) != -1 ||
		     responseText.indexOf( "decide to clean up these piles of bones" ) != -1 ||
		     responseText.indexOf( "decide to bust up their bones" ) != -1 || 
		     responseText.indexOf( "the little piles of bones remain" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Bone Piles" );
		}

		else if ( responseText.indexOf( "A seven-foot tall humanoid figure" ) != -1 )
		{
			ArcadeRequest.logText( "Encounter: Death" );
		}

		// Second: look for items

		if ( responseText.indexOf( "you find a large brass key" ) != -1 )
		{
			ArcadeRequest.logText( "You find a key" );
		}

		else if ( responseText.indexOf( "A blue potion bottle rests on the floor in the alcove" ) != -1 )
		{
			ArcadeRequest.logText( "You find a Magic Potion" );
		}

		else if ( responseText.indexOf( "discover a large blue bottle" ) != -1 )
		{
			ArcadeRequest.logText( "You find a Muscle Potion" );
		}

		else if ( responseText.indexOf( "you find a large glowing blue bottle" ) != -1 )
		{
			ArcadeRequest.logText( "You find a Combat Potion" );
		}

		else if ( responseText.indexOf( "SOMEONE SHOT THE FOOD!" ) != -1 )
		{
			ArcadeRequest.logText( "You shoot the food" );
		}

		else if ( responseText.indexOf( "even if it isn't actually food" ) != -1 )
		{
			ArcadeRequest.logText( "You find food" );
		}

		// Third: look for room features:

		if ( responseText.indexOf( "strange light-blue metal" ) != -1 )
		{
			ArcadeRequest.logText( "You find a locked door" );

			if ( responseText.indexOf( "the wall is gone" ) != -1 || 
			     responseText.indexOf( "the wall disappears" ) != -1 )
			{
				ArcadeRequest.logText( "You unlock the door" );

				if ( responseText.indexOf( "a large wooden treasure chest" ) != -1 )
				{
					ArcadeRequest.logText( "You find treasure" );
				}

				else if ( responseText.indexOf( "a square black pit" ) != -1 )
				{
					ArcadeRequest.logText( "You find the exit" );
				}
			}
		}

		// Finally, see if the game is over
		Matcher matcher = ArcadeRequest.FINAL_SCORE_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			String message = "";
			if ( responseText.indexOf( "YOU HAVE ESCAPED THE DUNGEON!" ) != -1 )
			{
				message = "YOU HAVE ESCAPED THE DUNGEON! ";

			}

			else if ( responseText.indexOf( "YOU HAVE DIED." ) != -1 )
			{
				message = "YOU HAVE DIED. ";
			}

			ArcadeRequest.logText( message + matcher.group(0) );

			// The game is over. No more choices.
			ArcadeRequest.indices = null;
			ArcadeRequest.choices = null;
			return;
		}

		// Parse out the new choice names
		ArcadeRequest.parseChoiceNames( responseText );
	}

	public static final void decorateDungeonFist( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "arcadeGameHints" ) )
		{
			return;
		}

		if ( buffer.indexOf( "You drop your token into" ) != -1 )
		{
			ChoiceManager.addGoalButton( buffer, "30 Game Grid tickets" );
		}
		StringUtilities.singleStringReplace( buffer, "</body>",
			"<center><p><img src='/images/otherimages/arcade/DungeonFistMap.png' width=544 height=672 alt='Snapshot of initial maze' title='Snapshot of initial maze'></center></body>" );
	}
	
	private static final String FistScript = 
		"3111111111111111111111111111112112111111111111111111111111121" +
		"1111111111111111211122211111121111111111111111122211133111113";
		
	public static final String autoDungeonFist( int stepCount )
	{
		if ( stepCount < 0 || stepCount >= FistScript.length() )
		{
			return "0";
		}
		RelayRequest.specialCommandStatus = "<progress value=" + stepCount +
			" max=" + FistScript.length() +
			" style=\"width: 100%;\">Dungeon Fist! step " + stepCount +
			" of " + FistScript.length() + "</progress>";
		return FistScript.substring( stepCount, stepCount + 1 );
	}

	/* End Dungeon Fist! */
	/* Fighters of Fighting */

	// Opponents
	private static final int KITTY = 0;
	private static final int MORBIDDA = 1;
	private static final int ROO = 2;
	private static final int SERENITY = 3;
	private static final int THORNY = 4;
	private static final int VASO = 5;

	private static final String [] OSTRING = new String[]
	{
		"Kitty the Zmobie Basher",
		"Morbidda",
		"Roo",
		"Serenity",
		"Thorny Toad",
		"Vaso De Agua",
	};

	private static final int findOpponent( final String name )
	{
		for ( int i = 0; i < OSTRING.length; ++i )
		{
			if ( OSTRING[i].equals( name ) )
			{
				return i;
			}
		}
		return -1;
	}

	// Moves
	private static final int HEAD_KICK = 0;
	private static final int GROIN_KICK = 1;
	private static final int LEG_SWEEP = 2;
	private static final int THROAT_PUNCH = 3;
	private static final int GUT_PUNCH = 4;
	private static final int KNEE_PUNCH = 5;

	private static final String [] MSTRING = new String[]
	{
		"Head Kick",
		"Groin Kick",
		"Leg Sweep",
		"Throat Punch",
		"Gut Punch",
		"Knee Punch",
	};

	private static final String [] MCODE = new String[]
	{
		"hk",
		"gk",
		"lk",
		"tp",
		"gp",
		"kp",
	};

	private static final int findPlayerMove( final GenericRequest request )
	{
		String field = request.getFormField( "attack" );
		if ( field != null )
		{
			for ( int i = 0; i < MCODE.length; ++i )
			{
				if ( MCODE[i].equals( field ) )
				{
					return i;
				}
			}
		}
		return -1;
	}

	// Threat Strings: Indexed by opponent, threat
	private static final String [][] THREATS = new String[][]
	{
		// Kitty the Zmobie Basher
		{	// hk, gk, ls, tp, gp, kp
			"launches a kick straight at your forehead",
			"get some paininess in your sexy parts",
			"ready to sweep your leg",
			"about to punch you in the throat",
			"like a punch to the gut",
			"aims a punch at your kneecap",
		},
		// Morbidda
		{	// hk, gk, ls, tp, gp, kp
			"launching itself at your head",
			"aims a knee square at your groin",
			"trying to trip you up",
			"aims it at your throat",
			"aims a punch at your solar plexus",
			"fist to kneecap",
		},
		// Roo
		{	// hk, gk, ls, tp, gp, kp
			"aims one big, flat foot at your head",
			"aims a foot right at your crotch",
			"aims his tail at your ankles",
			"punch you in the throat",
			"prepares to suckerpunch you in the gut",
			"aims a punch square at your kneecap",
		},
		// Serenity
		{	// hk, gk, ls, tp, gp, kp
			"a hard boot to the head",
			"a nice, solid kick to the gonads",
			"knock your ankles out from under you",
			"launches a fist at your throat",
			"punched in the small intestine",
			"about to punch you in the knee",
		},
		// Thorny Toad
		{	// hk, gk, ls, tp, gp, kp
			"a vicious kick to the head",
			"he's going for the groin",
			"crouches to try and sweep your legs",
			"launches a fist at your throat",
			"aims it square at your gut",
			"aims a punch at your knee",
		},
		// Vaso De Agua
		{	// hk, gk, ls, tp, gp, kp
			"you see his foot flying at your head",
			"a well-placed foot to the groin",
			"my feet had been knocked out from under me",
			"aims it straight at your throat",
			"aims a helpful fist at your gut",
			"about to punch you in the knee",
		},
	};

	private static final int findThreat( final int opponent, final String challenge )
	{
		if ( opponent < 0 )
		{
			return -1;
		}

		String [] challenges = THREATS[ opponent ];
		for ( int i = 0; i < challenges.length; ++i )
		{
			if ( challenge.indexOf( challenges[ i ] ) != -1 )
			{
				return i;
			}
		}
		return -1;
	}

	private static final String findThreatName( final String name, final String challenge )
	{
		int threat = ArcadeRequest.findThreat( findOpponent( name ), challenge );
		return threat < 0 ? null : MSTRING[ threat ];
	}

	// Effectiveness
	private static final int FAIL = 0;
	private static final int POOR = 1;
	private static final int FAIR = 2;
	private static final int GOOD = 3;

	private static final String [] ESTRING = new String[]
	{
		"Fail",
		"Poor",
		"Fair",
		"Good",
	};

	private static final int [][][] EFFECTIVENESS = new int [][][]
	{
		// Kitty the Zmobie Basher
		{
			// vs HK:   hk	 gk	ls    tp    gp	  kp
			{	   GOOD, FAIR, FAIL, FAIL, FAIL, POOR },
			// vs GK:   hk	 gk	ls    tp    gp	  kp
			{	   FAIR, FAIL, GOOD, POOR, FAIL, FAIL },
			// vs LS:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, GOOD, FAIR, FAIL, POOR, FAIL },
			// vs TP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, FAIL, POOR, FAIR, FAIL, GOOD },
			// vs GP:   hk	 gk	ls    tp    gp	  kp
			{	   POOR, FAIL, FAIL, FAIL, GOOD, FAIR },
			// vs KP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, POOR, FAIL, GOOD, FAIR, FAIL },
		},
		// Morbidda
		{
			// vs HK:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, GOOD, FAIR, POOR, FAIL, FAIL },
			// vs GK:   hk	 gk	ls    tp    gp	  kp
			{	   GOOD, FAIR, FAIL, FAIL, POOR, FAIL },
			// vs LS:   hk	 gk	ls    tp    gp	  kp
			{	   FAIR, FAIL, GOOD, FAIL, FAIL, POOR },
			// vs TP:   hk	 gk	ls    tp    gp	  kp
			{	   POOR, FAIL, FAIL, GOOD, FAIR, FAIL },
			// vs GP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, POOR, FAIL, FAIR, FAIL, GOOD },
			// vs KP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, FAIL, POOR, FAIL, GOOD, FAIR },
		},
		// Roo
		{
			// vs HK:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, POOR, FAIL, FAIL, FAIR, GOOD },
			// vs GK:   hk	 gk	ls    tp    gp	  kp
			{	   POOR, FAIL, FAIL, GOOD, FAIL, FAIR },
			// vs LS:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, FAIL, POOR, FAIR, GOOD, FAIL },
			// vs TP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIR, GOOD, FAIL, FAIL, POOR, FAIL },
			// vs GP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, FAIR, GOOD, POOR, FAIL, FAIL },
			// vs KP:   hk	 gk	ls    tp    gp	  kp
			{	   GOOD, FAIL, FAIR, FAIL, FAIL, POOR },
		},
		// Serenity
		{	// *** Incomplete info in Wiki
			// vs HK:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, FAIL, GOOD, FAIL, FAIL, FAIL },
			// vs GK:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, GOOD, FAIR, FAIL, FAIL, FAIL },
			// vs LS:   hk	 gk	ls    tp    gp	  kp
			{	   GOOD, FAIL, FAIL, FAIL, FAIL, FAIL },
			// vs TP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, POOR, FAIL, FAIL, GOOD, FAIL },
			// vs GP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, FAIL, FAIL, GOOD, FAIL, FAIL },
			// vs KP:   hk	 gk	ls    tp    gp	  kp
			{	   POOR, FAIL, FAIL, FAIR, FAIL, GOOD },
		},
		// Thorny Toad
		{
			// vs HK:   hk	 gk	ls    tp    gp	  kp
			{	   POOR, FAIL, FAIL, GOOD, FAIL, FAIR },
			// vs GK:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, FAIL, POOR, FAIR, GOOD, FAIL },
			// vs LS:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, POOR, FAIL, FAIL, FAIR, GOOD },
			// vs TP:   hk	 gk	ls    tp    gp	  kp
			{	   GOOD, FAIL, FAIR, POOR, FAIL, FAIL },
			// vs GP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIR, GOOD, FAIL, FAIL, FAIL, POOR },
			// vs KP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, FAIR, GOOD, FAIL, POOR, FAIL },
		},
		// Vaso De Agua
		{
			// vs HK:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, FAIL, POOR, FAIR, GOOD, FAIL },
			// vs GK:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, POOR, FAIL, FAIL, FAIR, GOOD },
			// vs LS:   hk	 gk	ls    tp    gp	  kp
			{	   FAIR, FAIL, FAIL, GOOD, FAIL, POOR },
			// vs TP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIL, FAIR, GOOD, FAIL, FAIL, POOR },
			// vs GP:   hk	 gk	ls    tp    gp	  kp
			{	   GOOD, FAIL, FAIR, FAIL, POOR, FAIL },
			// vs KP:   hk	 gk	ls    tp    gp	  kp
			{	   FAIR, GOOD, FAIL, POOR, FAIL, FAIL },
		},
	};

	private final static Pattern MATCH_PATTERN = Pattern.compile( "&quot;(.*?) Vs. (.*?) FIGHT!&quot;", Pattern.DOTALL );

	private static int round = 0;
	public static final void visitFightersOfFightingChoice( final String responseText )
	{
		// Called when we first visit the Fighters of Fighting.
		Matcher matcher = MATCH_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

		String message = "Match: " + matcher.group( 1 ) + " vs. " + matcher.group( 2 );
		ArcadeRequest.logText( message );

		// Reset round counter
		ArcadeRequest.round = 0;
	}

	private final static Pattern ROUND_PATTERN = Pattern.compile( "Results:.*?<td>(.*?)</td>.*?Score: ([0123456789,]*)</td>.*?title=\"(\\d+) HP\".*?title=\"(\\d+) HP\".*?<b>(.*?)</b>.*?>VS<.*?<b>(.*?)</b>", Pattern.DOTALL );

	private static final void logRound( final String text )
	{
		ArcadeRequest.logRound( null, text );
	}

	private static final void logRound( final String move, final String text )
	{
		Matcher matcher = ROUND_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return;
		}

		StringBuffer buffer = new StringBuffer();
		String challenge = matcher.group( 1 );
		String score = matcher.group( 2 );
		String pHP = matcher.group( 3 );
		String oHP = matcher.group( 4 );
		String player = matcher.group( 5 );
		String opponent = matcher.group( 6 );
		String threat = findThreatName( opponent, challenge );
		buffer.append( "Round " );
		buffer.append( String.valueOf( ArcadeRequest.round ) );
		buffer.append( ": " );
		if ( move != null )
		{
			buffer.append( move );
			buffer.append( " " );
		}
		buffer.append( player );
		buffer.append( " (" );
		buffer.append( pHP );
		buffer.append( " HP) " );
		buffer.append( opponent );
		buffer.append( " (" );
		buffer.append( oHP );
		buffer.append( " HP) Score: " );
		buffer.append( score );
		buffer.append( " Threat: " );
		buffer.append( threat );
		ArcadeRequest.logText( buffer.toString() );
		ArcadeRequest.round++;
	}

	private final static Pattern FINAL_ROUND_PATTERN = Pattern.compile( "Game Over!<p>(?:<b>)?(.*?)(?:</b>)?<p>Score: ([0123456789,]*)", Pattern.DOTALL );

	private static final void logFinalRound( final String move, final String text )
	{
		Matcher matcher = FINAL_ROUND_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return;
		}

		StringBuffer buffer = new StringBuffer();
		String result = matcher.group( 1 );
		String score = matcher.group( 2 );
		buffer.append( "Round " );
		buffer.append( String.valueOf( ArcadeRequest.round ) );
		buffer.append( ": " );
		buffer.append( move );
		buffer.append( " Result: " );
		buffer.append( result );
		buffer.append( " Final Score: " );
		buffer.append( score );
		ArcadeRequest.logText( buffer.toString() );
	}

	public static final void postChoiceFightersOfFighting( final GenericRequest request )
	{
		String text = request.responseText;
		// If this is the very first round of the match, parse and log
		// the threat
		if ( ChoiceManager.lastDecision == 6 )
		{
			ArcadeRequest.logRound( text );
			return;
		}

		// Find the move the player used
		int move = findPlayerMove( request );
		if ( move < 0 )
		{
			return;
		}

		if ( text.indexOf( "Game Over!" ) != -1 )
		{
			ArcadeRequest.logFinalRound( MSTRING[ move ], text );
		}
		else
		{
			ArcadeRequest.logRound( MSTRING[ move ], text );
		}
	}

	public static final String autoChoiceFightersOfFighting( final GenericRequest request )
	{
		String text = request.responseText;

		// If this is the initial visit, decision = 6
		Matcher matcher = MATCH_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			request.clearDataFields();
			return "6";
		}

		// If it is an intermediate round, choose the best move
		matcher = ROUND_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return null;
		}

		String challenge = matcher.group( 1 );
		String oname = matcher.group( 6 );
		int opponent = findOpponent( oname );
		if ( opponent < 0 )
		{
			return null;
		}

		int threat = findThreat( opponent, challenge );
		if ( threat < 0 )
		{
			return null;
		}

		int [] effects = EFFECTIVENESS[ opponent ][ threat ];

		for ( int i = 0; i < effects.length; ++i )
		{
			if ( effects[i] == GOOD )
			{
				request.clearDataFields();
				request.addFormField( "attack", MCODE[ i ] );
				return "1";
			}
		}

		return null;
	}

	public static final void decorateFightersOfFighting( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "arcadeGameHints" ) )
		{
			return;
		}

		String text = buffer.toString();

		Matcher matcher = ROUND_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			return;
		}

		String challenge = matcher.group( 1 );
		String oname = matcher.group( 6 );
		int opponent = findOpponent( oname );
		if ( opponent < 0 )
		{
			return;
		}

		int threat = findThreat( opponent, challenge );
		if ( threat < 0 )
		{
			return;
		}

		int [] effects = EFFECTIVENESS[ opponent ][ threat ];

		int index1 = text.indexOf( "<form method=\"post\" action=\"choice.php\">" );
		if ( index1 < 0 )
		{
			return;
		}

		buffer.setLength( 0 );
		buffer.append( text.substring( 0, index1 ) );

		for ( int i = 0; i < 6; ++i )
		{
			int index2 =  text.indexOf( "</form>", index1 );

			// If KoL says we've run out of choices, quit now
			if ( index2 == -1 )
			{
				break;
			}

			// Start spoiler text
			buffer.append( text.substring( index1, index2 ) );
			buffer.append( "<br><font size=-1>(" );

			// Say what the choice will give you
			buffer.append( ESTRING[ effects[ i ] ] );

			// Finish spoiler text
			buffer.append( ")</font></form>" );
			index1 = index2 + 7;
		}

		buffer.append( text.substring( index1 ) );
	}

	/* End Fighters of Fighting */
	/* Meteoid */

	private static int energy;
	private static int bombs;
	private static int missiles;
	private static int crystals;

	public static final void visitMeteoidChoice( final String responseText )
	{
		// Called when we visit Meteoid

		ArcadeRequest.energy = 100;
		ArcadeRequest.bombs = 1;
		ArcadeRequest.missiles = 1;
		ArcadeRequest.crystals = 0;

		// Parse out the choice names
		ArcadeRequest.parseChoiceNames( responseText );
	}

	public static final void logMetoidAction( final String responseText )
	{
		// Called when we are about to take a choice in SpaceTrip
		String action = ArcadeRequest.findChoiceName( ChoiceManager.lastDecision );
		if ( action == null )
		{
			return;
		}

		// Don't log navigation around the space ship or base
		switch ( ChoiceManager.lastChoice )
		{
		case 488:	// Bridge
			if ( action.equals( "Load up SpaceMall" ) )
			{
				return;
			}
			break;
		case 489:	// SpaceMall
			if ( action.equals( "Close SpaceMall" ) )
			{
				return;
			}
			break;
		case 491:	// End
			return;
		}

		ArcadeRequest.logText( "Action: " + action );
	}

        // <b>Energy:</b> 80<br><b>Bombs:</b> 1<br><b>Missiles:</b> 1<br><b>Credcrystals:</b> 0<center>

	private final static Pattern METEOID_RESOURCE_PATTERN = Pattern.compile( "<b>Energy:</b> (\\d+)<br><b>Bombs:</b> (\\d+)<br><b>Missiles:</b> (\\d+)<br><b>Credcrystals:</b> (\\d+)<center>", Pattern.DOTALL );

	private final static Pattern ENERGY_PATTERN = Pattern.compile( "left behind a ball of plasma worth ([0123456789,]*) energy", Pattern.DOTALL );

	private final static Pattern CRYSTAL1_PATTERN = Pattern.compile( "left behind ([0123456789,]*) Credcrystal", Pattern.DOTALL );

	private final static Pattern CRYSTAL2_PATTERN = Pattern.compile( "cache of Credcrystals.*?([0123456789,]*), to be exact", Pattern.DOTALL );

	private final static Pattern GUARD_PATTERN = Pattern.compile( "The room was guarded by a fierce (.*?)!", Pattern.DOTALL );

	public static final void postChoiceMeteoid( final GenericRequest request )
	{
		// Called when we have taken a choice in Meteoid

		// Log what we see
		String responseText = request.responseText;

		// Log action appropriately
		ArcadeRequest.logMetoidAction( responseText );

		// The room was guarded by a fierce <monster>!
		Matcher guardMatcher = GUARD_PATTERN.matcher( responseText );
		if ( guardMatcher.find() )
		{
			ArcadeRequest.logText( "Encounter: " + guardMatcher.group(1) );
		}

		// The room contained a bizarre bird-man statue, seated and
		// holding a spherical container.
		else if ( responseText.indexOf( "bizarre bird-man statue") != -1 )
		{
			ArcadeRequest.logText( "Encounter: Statue" );
		}

		// The room contained a terminal whose screen displayed what
		// appeared to be map data about the surrounding environment.
		else if ( responseText.indexOf( "map data") != -1 )
		{
			ArcadeRequest.logText( "Encounter: Map Terminal" );
		}

		// The room contained a nano-charge station. My cybersuit's
		// computer beeped, notifying me that I could use the station
		// to replenish my bombs and missiles
		else if ( responseText.indexOf( "nano-charge station") != -1 )
		{
			ArcadeRequest.logText( "Encounter: Charge Station" );
		}

		// The room contained a teleporter keyed to the planetoid's
		// surface -- I could use it to get back to my ship!
		else if ( responseText.indexOf( "planetoid's surface") != -1 )
		{
			ArcadeRequest.logText( "Encounter: Teleporter" );
		}

		// The intense heat of the room leached 5 energy from my
		// cybersuit...
		if ( responseText.indexOf( "leached 5 energy" ) != -1 )
		{
			ArcadeRequest.logText( "You lose 5 energy" );
		}

		// It left behind a ball of plasma worth 20 energy.
		Matcher energyMatcher = ENERGY_PATTERN.matcher( responseText );
		if ( energyMatcher.find() )
		{
			int energy = StringUtilities.parseInt( energyMatcher.group(1) );
			ArcadeRequest.logText( "You gain " + KoLConstants.COMMA_FORMAT.format( energy ) + " energy" );
		}

		// It left behind 9 Credcrystals!
		//
		// I opened the sphere and found a cache of Credcrystals.  291,
		// to be exact.
		Matcher crystalMatcher = CRYSTAL1_PATTERN.matcher( responseText );
		if ( crystalMatcher.find() )
		{
			int crystals = StringUtilities.parseInt( crystalMatcher.group(1) );
			ArcadeRequest.logText( "You gain " + KoLConstants.COMMA_FORMAT.format( crystals ) + " Credcrystals" );
		}

		crystalMatcher = CRYSTAL2_PATTERN.matcher( responseText );
		if ( crystalMatcher.find() )
		{
			int crystals = StringUtilities.parseInt( crystalMatcher.group(1) );
			ArcadeRequest.logText( "You gain " + KoLConstants.COMMA_FORMAT.format( crystals ) + " Credcrystals" );
		}

		// Look at current resources
		Matcher resourceMatcher = METEOID_RESOURCE_PATTERN.matcher( responseText );
		if ( resourceMatcher.find() )
		{
			int energy = StringUtilities.parseInt( resourceMatcher.group(1) );
			int bombs = StringUtilities.parseInt( resourceMatcher.group(2) );
			int missiles = StringUtilities.parseInt( resourceMatcher.group(3) );
			int crystals = StringUtilities.parseInt( resourceMatcher.group(4) );

			if ( energy != ArcadeRequest.energy ||
			     bombs != ArcadeRequest.bombs ||
			     missiles != ArcadeRequest.missiles ||
			     crystals != ArcadeRequest.crystals )
			{
				ArcadeRequest.logText( "Energy: " + KoLConstants.COMMA_FORMAT.format( energy ) +
						       ". Bombs: " + KoLConstants.COMMA_FORMAT.format( bombs ) +
						       ". Missiles: " + KoLConstants.COMMA_FORMAT.format( missiles ) +
						       " Credcrystals: " + KoLConstants.COMMA_FORMAT.format( crystals ) + "." );
				ArcadeRequest.energy = energy;
				ArcadeRequest.bombs = bombs;
				ArcadeRequest.missiles = missiles;
				ArcadeRequest.crystals = crystals;
			}
		}

		// Finally, see if the game is over
		Matcher totalMatcher = ArcadeRequest.TOTAL_SCORE_PATTERN.matcher( responseText );
		if ( totalMatcher.find() )
		{
			ArcadeRequest.logText( "Total Score: " + totalMatcher.group(1) );

			// The game is over. No more choices.
			ArcadeRequest.indices = null;
			ArcadeRequest.choices = null;
			return;
		}

		// Parse out the new choice names
		ArcadeRequest.parseChoiceNames( responseText );
	}

	/* End Meteoid */
	/* Jackass Plumber */

	public static final void visitJackassPlumberChoice( final String responseText )
	{
		// Called when we visit Jackass Plumber
		// Parse out the choice names
		ArcadeRequest.parseChoiceNames( responseText );
	}

	public static final void postChoiceJackassPlumber( final GenericRequest request )
	{
		// Called when we have taken a choice in Jackass Plumber

		// Log what we see
		String responseText = request.responseText;

		// Parse out the new choice names
		ArcadeRequest.parseChoiceNames( responseText );
	}

	/* End Jackass Plumber */
}
