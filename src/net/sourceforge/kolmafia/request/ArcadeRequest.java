/**
 * Copyright (c) 2005-2010, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ArcadeRequest
	extends GenericRequest
{
	public static final AdventureResult TOKEN = ItemPool.get( ItemPool.GG_TOKEN, 1 );

	public ArcadeRequest()
	{
		super( "arcade.php" );
	}

	public void reconstructFields()
	{
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
		       game.equals( "4" ) ) )
		{
			return 1;
		}

		return 0;
	}

	private static final Pattern GAME_PATTERN = Pattern.compile( "whichgame=(\\d+)" );

	private static final int getGame( final String urlString  )
	{
		Matcher matcher = ArcadeRequest.GAME_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group(1) ) : 0;
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

			KoLmafia.saveDataOverride();
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
			if ( action.equals( "redeem" ) )
			{
				// Buy stuff at the ticket counter
				// Let CoinmasterRequest claim this
				return false;
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
					name = "Fighters of Fighting";
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

	/*
	 * Support for individual games
	 */

	private static final void logText( final String text )
	{
		RequestLogger.printLine( text );
		RequestLogger.updateSessionLog( text );
	}

	private final static Pattern CHOICE_PATTERN = Pattern.compile( "<form name=choiceform.*?name=option value=(\\d+)>.*?class=button type=submit value=\"(.*?)\".*?></form>", Pattern.DOTALL );

	// The strings tagging each available choice
	private static String [] choices = null;

	private static void parseChoiceNames( final String responseText )
	{
		ArrayList tags = new ArrayList();
		Matcher matcher = CHOICE_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			tags.add( matcher.group( 2 ) );
		}
		ArcadeRequest.choices = new String [ tags.size() ];
		tags.toArray( ArcadeRequest.choices );
	}

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

	private final static Pattern FINAL_SCORE_PATTERN = Pattern.compile( "FINAL SCORE: ([0123456789,]*)", Pattern.DOTALL );

	public static final void postChoiceDungeonFist( final GenericRequest request )
	{
		// Called when we have taken a choice in Dungeon Fist!
		int choice = ChoiceManager.lastDecision;
		if ( choice >= 1 && choice <= ArcadeRequest.choices.length )
		{
			// Log the action the player took
			String action = ArcadeRequest.choices[ choice - 1 ];
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
			ArcadeRequest.choices = null;
			return;
		}

		// Parse out the new choice names
		ArcadeRequest.parseChoiceNames( responseText );
	}

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
}
