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

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanRumpusRequest
	extends GenericRequest
{
	private static final Pattern SPOT_PATTERN = Pattern.compile( "spot=(\\d*)" );
	private static final Pattern FURNI_PATTERN = Pattern.compile( "furni=(\\d*)" );

	private static final int SEARCH = 0;

	public static final int MUSCLE = 1;
	public static final int MYSTICALITY = 2;
	public static final int MOXIE = 3;
	public static final int SOFA = 4;
	public static final int CHIPS = 5;
	public static final int BALLS = 6;
	public static final int JUKEBOX = 7;

	public static final int RADIUM = 1;
	public static final int WINTERGREEN = 2;
	public static final int ENNUI = 3;

	public static final Object [][] CHIP_FLAVORS = new Object[][]
	{
		{ "radium", IntegerPool.get( RADIUM ) },
		{ "wintergreen", IntegerPool.get( WINTERGREEN ) },
		{ "ennui", IntegerPool.get( ENNUI ) },
	};

	public static final Object [][] SONGS = new Object[][]
	{
		{ "meat", "Material Witness", IntegerPool.get( 1 ) },
		{ "stats", "No Worries", IntegerPool.get( 2 ) },
		{ "item", "Techno Bliss", IntegerPool.get( 3 ) },
		{ "initiative", "Metal Speed", IntegerPool.get( 4 ) },
	};

	public static final int findChips( final String name )
	{
		for ( int i = 0; i < CHIP_FLAVORS.length; ++i )
		{
			String flavor = (String) CHIP_FLAVORS[i][0];
			if ( name.equals( flavor ) )
			{
				Integer index = (Integer) CHIP_FLAVORS[i][1];
				return index.intValue();
			}
		}

		return 0;
	}

	public static final int findSong( final String name )
	{
		if ( StringUtilities.isNumeric( name ) )
		{
			int n = StringUtilities.parseInt( name );
			return n > 0 && n <= SONGS.length ? n : 0;
		}

		for ( int i = 0; i < SONGS.length; ++i )
		{
			String modifier = (String) SONGS[i][0];
			String effect = (String) SONGS[i][1];
			if ( name.equals( modifier ) || name.equals( effect ) )
			{
				return i + 1;
			}
		}

		return 0;
	}

	private static final Pattern TURN_PATTERN = Pattern.compile( "numturns=(\\d+)" );

	public static final String[][] EQUIPMENT =
	{
		// Row 1, Column 1: Spot 1
		{
			"Girls of Loathing Calendar",
			"Boys of Loathing Calendar",
			"Infuriating Painting",
			"Exotic Hanging Meat Orchid",
		},
		// Row 1, Column 2: Spot 2
		{
			"Collection of Arcane Tomes and Whatnot",
			"Collection of Sports Memorabilia",
			"Collection of Self-Help Books"
		},
		// Row 1, Column 3: Spot 3
		{
			"Soda Machine",
			"Jukebox",
			"Mr. Klaw \"Skill\" Crane Game",
		},
		// Row 2, Column 1: Spot 4
		{
			"Old-Timey Radio",
			"Potted Meat Bush",
			"Inspirational Desk Calendar",
		},
		// Row 2, Column 2: Spot 5
		{
			"Wrestling Mat",
			"Tan-U-Lots Tanning Bed",
			"Comfy Sofa",
		},
		// Row 2, Column 3: Spot 6
		{},
		// Row 3, Column 1: Spot 7
		{
			"Awesome Ball Pit",
		},
		// Row 3, Column 2: Spot 8
		{},
		// Row 3, Column 3: Spot 9
		{
			"Hobo-Flex Workout System",
			"Snack Machine",
			"Potted Meat Tree",
		},
	};

	public static final int[][] MAXIMUM_USAGE =
	{
		// Row 1, Column 1: Spot 1
		{ 0, 0, 0, 1 },
		// Row 1, Column 2: Spot 2
		{ 0, 0, 0 },
		// Row 1, Column 3: Spot 3
		{ 3, 0, 3 },
		// Row 2, Column 1: Spot 4
		{ 1, 1, 0 },
		// Row 2, Column 2: Spot 5
		{ 0, 0, 0 },
		// Row 2, Column 3: Spot 6
		{},
		// Row 3, Column 1: Spot 7
		{ 0 },
		// Row 3, Column 2: Spot 8
		{},
		// Row 3, Column 3: Spot 9
		{ 0, 0, 1 },
	};

	private int action;
	private int option;
	private int turnCount;

	/**
	 * Constructs a new <code>ClanRumpusRequest</code>.
	 *
	 * @param action The identifier for the action you're requesting
	 */

	private ClanRumpusRequest()
	{
		this( SEARCH );
	}

	public ClanRumpusRequest( final int action )
	{
		super( "clan_rumpus.php" );
		this.action = action;
	}

	public ClanRumpusRequest( final int action, final int option )
	{
		super( "clan_rumpus.php" );
		this.action = action;
		this.option = option;
	}

	/**
	 * Runs the request. Note that this does not report an error if it fails; it merely parses the results to see if any
	 * gains were made.
	 */

	public ClanRumpusRequest setTurnCount( final int turnCount )
	{
		this.turnCount = turnCount;
		return this;
	}

	private void visitEquipment( final int spot, final int furniture )
	{
		this.clearDataFields();
		this.addFormField( "action", "click" );
		this.addFormField( "spot", String.valueOf( spot ) );
		this.addFormField( "furni", String.valueOf( furniture ) );
	}

	public static String equipmentName( final int spot, final int furniture )
	{
		if ( spot < 1 || spot > 9 )
		{
			return null;
		}

		String [] equipment = EQUIPMENT[ spot - 1];
		if ( furniture < 1 || furniture > equipment.length )
		{
			return null;
		}

		return equipment[ furniture - 1 ];
	}

	public static int dailyUses( final int spot, final int furniture )
	{
		if ( spot < 1 || spot > 9 )
		{
			return 0;
		}

		int [] usage = MAXIMUM_USAGE[ spot - 1];
		if ( furniture < 1 || furniture > usage.length )
		{
			return 0;
		}

		return usage[ furniture - 1 ];
	}

	@Override
	public int getAdventuresUsed()
	{
		return this.turnCount;
	}

	@Override
	public void run()
	{
		switch ( this.action )
		{
		case SEARCH:
			break;

		case MUSCLE:
			// If we can do inside Degrassi Knoll use the gym.
			if ( KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore() )
			{
				this.constructURLString( "knoll.php" );
				this.addFormField( "action", "gym" );
			}
			// Otherwise, use the one in our clan - if we're in one.
			else
			{
				this.constructURLString( "clan_rumpus.php" );
				this.addFormField( "preaction", "gym" );
				this.addFormField( "whichgym", "3" );
			}
			break;

		case MYSTICALITY:
			// If we can go to Little Canadia, use the gym.
			if ( KoLCharacter.canadiaAvailable() )
			{
				this.constructURLString( "canadia.php" );
				this.addFormField( "action", "institute" );
			}
			// Otherwise, use the one in our clan - if we're in one.
			else
			{
				this.constructURLString( "clan_rumpus.php" );
				this.addFormField( "preaction", "gym" );
				this.addFormField( "whichgym", "1" );
			}
			break;

		case MOXIE:
			// If we can go to the Gnomish Gnomads Camp, use the gym
			if ( KoLCharacter.gnomadsAvailable() )
			{
				this.constructURLString( "gnomes.php" );
				this.addFormField( "action", "train" );
			}
			// Otherwise, use the one in our clan - if we're in one.
			else
			{
				this.constructURLString( "clan_rumpus.php" );
				this.addFormField( "preaction", "gym" );
				this.addFormField( "whichgym", "2" );
			}
			break;

		case SOFA:
			this.constructURLString( "clan_rumpus.php" );
			this.addFormField( "preaction", "nap" );
			break;

		case CHIPS:
			this.constructURLString( "clan_rumpus.php" );
			this.addFormField( "preaction", "buychips" );
			this.addFormField( "whichbag", String.valueOf( this.option ) );
			break;

		case BALLS:
			this.constructURLString( "clan_rumpus.php" );
			this.addFormField( "preaction", "ballpit" );
			break;

		case JUKEBOX:
			this.constructURLString( "clan_rumpus.php" );
			this.addFormField( "preaction", "jukebox" );
			this.addFormField( "whichsong", String.valueOf( this.option ) );
			break;

		default:
			break;
		}

		if ( this.turnCount > 0 )
		{
			this.addFormField( "numturns", String.valueOf( this.turnCount ) );

			if ( KoLCharacter.getAdventuresLeft() < this.turnCount )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Insufficient adventures." );
				return;
			}
		}
		else if ( this.action == ClanRumpusRequest.SOFA )
		{
			return;
		}

		if ( this.action != ClanRumpusRequest.SEARCH )
		{
			KoLmafia.updateDisplay( "Executing request..." );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		ClanRumpusRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "clan_rumpus.php" ) )
		{
			return;
		}

		Matcher matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		if ( action == null )
		{
			return;
		}

		if ( action.equals( "gym" ) )
		{
			if ( responseText.contains( "You work it on out." ) ||
				 responseText.contains( "You study the secrets of the cosmos." ) ||
				 responseText.contains( "You bake under the artificial sunlight." ) )
			{
				KoLmafia.updateDisplay( "Workout completed." );
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "You can't access that gym" );
			}
		}

		if ( action.equals( "nap" ) )
		{
			// You take a nap on the comfy sofa.
			if ( responseText.contains( "You take a nap" ) )
			{
				KoLmafia.updateDisplay( "Resting completed." );
			}
			// Either you aren't in a clan or your clan doesn't have a sofa
			else
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Resting failed - no Clan Sofa available." );
			}
			return;
		}

		if ( action.equals( "ballpit" ) )
		{
			// You play in the ball pit. Wheeeeeee!
			// (You've already played in the ball pit today.)
			if ( responseText.indexOf( "play in the ball pit" ) != -1 ||
			     responseText.indexOf( "already played in the ball pit" ) != -1 )
			{
				Preferences.setBoolean( "_ballpit", true );
			}
			return;
		}

		if ( action.equals( "buychips" ) )
		{
			// a bag of chips drops into the tray at the bottom
			if ( responseText.indexOf( "a bag of chips drops" ) != -1 )
			{
				Preferences.increment( "_chipBags", 1 );
			}
			// You press the button and the big metal coil rotates,
			// but not far enough to actually drop the
			// chips. Dangit!
			else if ( responseText.indexOf( "but not far enough" ) != -1 )
			{
				Preferences.setInteger( "_chipBags", 3 );
			}

			return;
		}

		if ( action.equals( "jukebox" ) )
		{
			// Whether we get a song or not, we are done for the
			// day with the Jukebox, unless we ascend, which will
			// reset the preference.
			Preferences.setBoolean( "_jukebox", true );
			return;
		}

		if ( urlString.indexOf( "spot=3" ) != -1 && urlString.indexOf( "furni=3" ) != -1 )
		{
			// You carefully guide the claw over the prize that
			// looks the easiest to grab. You press the button and
			// the claw slowly descends.
			if ( responseText.indexOf( "slowly descends" ) != -1 )
			{
				Preferences.increment( "_klawSummons", 1 );
			}
			// The machine makes a horrible clanking noise, and a
			// wisp of smoke pours out of the prize chute.
			else if ( responseText.indexOf( "seems to be broken down" ) != -1 )
			{
				Preferences.setInteger( "_klawSummons", 3 );
			}

			return;
		}
	}

	public static void getBreakfast()
	{
		ClanRumpusRequest request = new ClanRumpusRequest();

		// Search for available equipment
		RequestThread.postRequest( request );

		if ( request.responseText == null )
		{
			return;
		}

		// The Klaw can be accessed regardless of whether or not
		// you are in hardcore, so handle it first.

		if ( request.responseText.indexOf( "rump3_3.gif" ) != -1 )
		{
			request.visitEquipment( 3, 3 );

			while ( Preferences.getInteger( "_klawSummons" ) < 3 )
			{
				request.run();
			}
		}

		if ( !KoLCharacter.canInteract() )
		{
			return;
		}

		for ( int i = 1; i <= ClanRumpusRequest.MAXIMUM_USAGE.length; ++i )
		{
			int [] usage = ClanRumpusRequest.MAXIMUM_USAGE[ i - 1 ];
			for ( int j = 1; j <= usage.length; ++j )
			{
				if ( i == 3 && j == 3 )
				{
					continue;
				}

				int maximum = usage[ j - 1 ];

				// If the equipment is not usable, skip it
				if ( maximum == 0 )
				{
					continue;
				}


				// If the equipment is not present, skip it
				if ( request.responseText.indexOf( "rump" + i + "_" + j + ".gif" ) == -1 )
				{
					continue;
				}

				request.visitEquipment( i, j );

				for ( int k = 0; k < maximum; ++k )
				{
					request.run();
				}
			}
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		String action = null;

		if ( urlString.startsWith( "knoll.php" ) && urlString.indexOf( "action=gym" ) != -1 )
		{
			action = "Pump Up Muscle";
		}
		else if ( urlString.startsWith( "canadia.php" ) && urlString.indexOf( "action=institute" ) != -1 )
		{
			action = "Pump Up Mysticality";
		}
		else if ( urlString.startsWith( "gnomes.php" ) && urlString.indexOf( "action=train" ) != -1 )
		{
			action = "Pump Up Moxie";
		}
		else if ( urlString.startsWith( "clan_rumpus.php" ) && urlString.indexOf( "whichgym=3" ) != -1 )
		{
			action = "Pump Up Muscle";
		}
		else if ( urlString.startsWith( "clan_rumpus.php" ) && urlString.indexOf( "whichgym=1" ) != -1 )
		{
			action = "Pump Up Mysticality";
		}
		else if ( urlString.startsWith( "clan_rumpus.php" ) && urlString.indexOf( "whichgym=2" ) != -1 )
		{
			action = "Pump Up Moxie";
		}
		else if ( urlString.startsWith( "clan_rumpus.php" ) && urlString.indexOf( "preaction=nap" ) != -1 )
		{
			action = "Rest in Clan Sofa";
		}

		if ( action != null )
		{
			Matcher matcher = ClanRumpusRequest.TURN_PATTERN.matcher( urlString );
			if ( !matcher.find() )
			{
				return true;
			}

			// If not enough turns available, nothing will happen.
			int turns = StringUtilities.parseInt( matcher.group( 1 ) );
			int available = KoLCharacter.getAdventuresLeft();
			if ( turns > available )
			{
				return true;
			}

			String message = "[" + KoLAdventure.getAdventureCount() + "] " + action + " (" + turns + " turns)";

			RequestLogger.printLine();
			RequestLogger.updateSessionLog();

			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			return true;
		}

		if ( !urlString.startsWith( "clan_rumpus.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "action=buychips" ) != -1 )
		{
			String message = "Buying chips from the Snack Machine in the clan rumpus room";
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			return true;
		}

		if ( urlString.indexOf( "preaction=ballpit" ) != -1 )
		{
			String message = "Jumping into the Awesome Ball Pit in the clan rumpus room";
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			return true;
		}

		if ( urlString.indexOf( "preaction=jukebox" ) != -1 )
		{
			String message = "Playing a song on the Jukebox in the clan rumpus room";
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			return true;
		}

		// The only other actions we handle here are clicking on clan
		// furniture

		if ( urlString.indexOf( "action=click" ) == -1 )
		{
			return false;
		}

		Matcher matcher = SPOT_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		int spot = StringUtilities.parseInt( matcher.group( 1 ) );

		matcher = FURNI_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		int furniture = StringUtilities.parseInt( matcher.group( 1 ) );

		String equipment = ClanRumpusRequest.equipmentName( spot, furniture );

		if ( equipment == null )
		{
			return false;
		}

		String message = "Visiting " + equipment + " in clan rumpus room";
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
