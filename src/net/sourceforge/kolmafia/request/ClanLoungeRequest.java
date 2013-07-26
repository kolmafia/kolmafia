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

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ClanLoungeSwimmingPoolRequest;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ConsequenceManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanLoungeRequest
	extends GenericRequest
{
	private static final int SEARCH = 0;

	public static final int KLAW = 1;
	public static final int HOTTUB = 2;
	public static final int POOL_TABLE = 3;
	public static final int CRIMBO_TREE = 4;
	public static final int LOOKING_GLASS = 5;
	public static final int FAX_MACHINE = 6;
	public static final int APRIL_SHOWER = 7;
	public static final int SWIMMING_POOL = 8;
	public static final int HOT_DOG_STAND = 9;

	// Pool options
	public static final int AGGRESSIVE_STANCE = 1;
	public static final int STRATEGIC_STANCE = 2;
	public static final int STYLISH_STANCE = 3;

	// Fax options
	public static final int SEND_FAX = 1;
	public static final int RECEIVE_FAX = 2;

	// Shower options
	public static final int COLD_SHOWER = 1;
	public static final int COOL_SHOWER = 2;
	public static final int LUKEWARM_SHOWER = 3;
	public static final int WARM_SHOWER = 4;
	public static final int HOT_SHOWER = 5;

	// Swimming Pool options
	public static final int CANNONBALL = 1;
	public static final int LAPS = 2;
	public static final int SPRINTS = 3;

	private int action;
	private int option;

	private static final Pattern STANCE_PATTERN = Pattern.compile( "stance=(\\d*)" );
	private static final Pattern WHICHDOG_PATTERN = Pattern.compile( "whichdog=(-\\d*)" );
	private static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	private static final Pattern TREE_PATTERN = Pattern.compile( "Check back in (\\d+) day" );
	private static final Pattern FAX_PATTERN = Pattern.compile( "preaction=(.+?)fax" );
	private static final Pattern TEMPERATURE_PATTERN = Pattern.compile( "temperature=(\\d*)" );
	private static final Pattern SWIMMING_POOL_PATTERN = Pattern.compile( "subaction=([^&]+)" );
	private static final Pattern LAPS_PATTERN = Pattern.compile( "manage to swim (\\d+) before" );
	private static final Pattern SPRINTS_PATTERN = Pattern.compile( "you do (\\d+) of them" );

	public static final Object [][] POOL_GAMES = new Object[][]
	{
		{
			"aggressive",
			"muscle",
			"billiards belligerence",
			IntegerPool.get( AGGRESSIVE_STANCE )
		},
		{
			"strategic",
			"mysticality",
			"mental a-cue-ity",
			IntegerPool.get( STRATEGIC_STANCE )
		},
		{
			"stylish",
			"moxie",
			"hustlin'",
			IntegerPool.get( STYLISH_STANCE )
		},
	};

	public static final Object [][] FAX_OPTIONS = new Object[][]
	{
		{
			"send",
			"put",
			IntegerPool.get( SEND_FAX )
		},
		{
			"receive",
			"get",
			IntegerPool.get( RECEIVE_FAX )
		},
	};

	public static final Object [][] SHOWER_OPTIONS = new Object[][]
	{
		{
			"cold",
			"ice",
			IntegerPool.get( COLD_SHOWER )
		},
		{
			"cool",
			"moxie",
			IntegerPool.get( COOL_SHOWER )
		},
		{
			"lukewarm",
			"mysticality",
			IntegerPool.get( LUKEWARM_SHOWER )
		},
		{
			"warm",
			"muscle",
			IntegerPool.get( WARM_SHOWER )
		},
		{
			"hot",
			"mp",
			IntegerPool.get( HOT_SHOWER )
		},
	};

	public static final Object [][] SWIMMING_OPTIONS = new Object[][]
	{
		{
			"cannonball",
			"item",
			IntegerPool.get( CANNONBALL )
		},
		{
			"laps",
			"ml",
			IntegerPool.get( LAPS )
		},
		{
			"sprints",
			"noncombat",
			IntegerPool.get( SPRINTS )
		},
	};

	public static final Object [][] HOTDOGS = new Object[][]
	{
		{
			"basic hot dog",
			IntegerPool.get( -92 ),
			IntegerPool.get( 1 )
		},
		{
			"savage macho dog",
			IntegerPool.get( -93 ),
			IntegerPool.get( 2 ),
			ItemPool.get( ItemPool.FURRY_FUR, 10 )
		},
		{
			"one with everything",
			IntegerPool.get( -94 ),
			IntegerPool.get( 2 ),
			ItemPool.get( ItemPool.CRANBERRIES, 10 )
		},
		{
			"sly dog",
			IntegerPool.get( -95 ),
			IntegerPool.get( 2 ),
			ItemPool.get( ItemPool.SKELETON_BONE, 10 )
		},
		{
			"devil dog",
			IntegerPool.get( -96 ),
			IntegerPool.get( 3 ),
			ItemPool.get( ItemPool.HOT_WAD, 25 )
		},
		{
			"chilly dog",
			IntegerPool.get( -97 ),
			IntegerPool.get( 3 ),
			ItemPool.get( ItemPool.COLD_WAD, 25 )
		},
		{
			"ghost dog",
			IntegerPool.get( -98 ),
			IntegerPool.get( 3 ),
			ItemPool.get( ItemPool.SPOOKY_WAD, 25 )
		},
		{
			"junkyard dog",
			IntegerPool.get( -99 ),
			IntegerPool.get( 3 ),
			ItemPool.get( ItemPool.STENCH_WAD, 25 )
		},
		{
			"wet dog",
			IntegerPool.get( -100 ),
			IntegerPool.get( 3 ),
			ItemPool.get( ItemPool.SLEAZE_WAD, 25 )
		},
		{
			"optimal dog",
			IntegerPool.get( -102 ),
			IntegerPool.get( 1 ),
			ItemPool.get( ItemPool.SCRAP_OF_PAPER, 25 )
		},
		{
			"sleeping dog",
			IntegerPool.get( -101 ),
			IntegerPool.get( 2 ),
			ItemPool.get( ItemPool.GAUZE_HAMMOCK, 10 )
		},
		{
			"video games hot dog",
			IntegerPool.get( -103 ),
			IntegerPool.get( 3 ),
			ItemPool.get( ItemPool.GAMEPRO_MAGAZINE, 3 )
		},
	};

	public static final LockableListModel ALL_HOT_DOGS = new LockableListModel();

	static
	{
		for ( int i = 0; i < HOTDOGS.length; ++i )
		{
			ClanLoungeRequest.ALL_HOT_DOGS.add( HOTDOGS[i][0] );
		}
	};

	public static final int hotdogNameToIndex( final String name )
	{
		return ClanLoungeRequest.ALL_HOT_DOGS.indexOf( name );
	}

	public static final int hotdogIdToIndex( int id )
	{
		for ( int i = 0; i < HOTDOGS.length; ++i )
		{
			if ( id == ((Integer)ClanLoungeRequest.HOTDOGS[i][1]).intValue() )
			{
				return i;
			}
		}
		return -1;
	}

	public static final String hotdogIdToName( int id )
	{
		int index = ClanLoungeRequest.hotdogIdToIndex( id );
		return index < 0 ? null : (String)ClanLoungeRequest.HOTDOGS[index][0];
	}

	public static final String hotdogIndexToName( int index )
	{
		return ( index < 0 || index > ClanLoungeRequest.HOTDOGS.length ) ? null : (String)ClanLoungeRequest.HOTDOGS[ index ][0];
	}

	public static final Integer hotdogIndexToId( int index )
	{
		return ( index < 0 || index > ClanLoungeRequest.HOTDOGS.length ) ? -1 : (Integer)ClanLoungeRequest.HOTDOGS[ index ][1];
	}

	public static final Integer hotdogIndexToFullness( int index )
	{
		return ( index < 0 || index > ClanLoungeRequest.HOTDOGS.length ) ? -1 : (Integer)ClanLoungeRequest.HOTDOGS[ index ][2];
	}

	public static final AdventureResult hotdogIndexToItem( int index )
	{
		return ( index < 0 || index > ClanLoungeRequest.HOTDOGS.length ) ? null : (AdventureResult)ClanLoungeRequest.HOTDOGS[ index ][3];
	}

	public static final boolean isHotDog( String name )
	{
		return ClanLoungeRequest.ALL_HOT_DOGS.contains( name );
	}

	private static void addHotDog( final String itemName )
	{
		// Don't bother adding fancy hot dogs if you've already eaten one today.
		int index = ClanLoungeRequest.hotdogNameToIndex( itemName );
		if ( index > 0 && Preferences.getBoolean( "_fancyHotDogEaten" ) )
		{
			return;
		}

		LockableListModel usables = ConcoctionDatabase.getUsables();
		Concoction item = new Concoction( itemName );
		if ( !usables.contains( item ) )
		{
			usables.add( item );
		}
	}

	private static final void resetHotdogs( final int start )
	{
		// Remove all hot dogs from the usable list
		LockableListModel usables = ConcoctionDatabase.getUsables();
		for ( int i = start; i < ClanLoungeRequest.ALL_HOT_DOGS.size(); ++i )
		{
			String itemName = (String) ClanLoungeRequest.ALL_HOT_DOGS.get( i );
			Concoction junk = new Concoction( itemName );
			usables.remove( junk );
		}
	}

	public static final void resetHotdogs()
	{
		// Remove all hot dogs from the list
		ClanLoungeRequest.resetHotdogs( 0 );
	}

	public static final int findPoolGame( String tag )
	{
		if ( StringUtilities.isNumeric( tag ) )
		{
			int index = StringUtilities.parseInt( tag );
			if ( index >= 1 && index <= POOL_GAMES.length )
			{
				return index;
			}
		}

		tag = tag.toLowerCase();
		for ( int i = 0; i < POOL_GAMES.length; ++i )
		{
			Object [] game = POOL_GAMES[i];
			Integer index = (Integer) game[3];
			String stance = (String) game[0];
			if ( stance.startsWith( tag ) )
			{
				return index.intValue();
			}
			String stat = (String) game[1];
			if ( stat.startsWith( tag ) )
			{
				return index.intValue();
			}
			String effect = (String) game[2];
			if ( effect.startsWith( tag ) )
			{
				return index.intValue();
			}
		}

		return 0;
	}

	public static final int findFaxOption( String tag )
	{
		tag = tag.toLowerCase();
		for ( int i = 0; i < FAX_OPTIONS.length; ++i )
		{
			Object [] faxOption = FAX_OPTIONS[i];
			Integer index = (Integer) faxOption[2];
			String faxCommand0 = (String) faxOption[0];
			if ( faxCommand0.startsWith( tag ) )
			{
				return index.intValue();
			}
			String faxCommand1 = (String) faxOption[1];
			if ( faxCommand1.startsWith( tag ) )
			{
				return index.intValue();
			}
		}

		return 0;
	}

	public static final int findShowerOption( String tag )
	{
		tag = tag.toLowerCase();
		for ( int i = 0; i < SHOWER_OPTIONS.length; ++i )
		{
			Object [] showerOption = SHOWER_OPTIONS[i];
			Integer index = (Integer) showerOption[2];
			String temp = (String) showerOption[0];
			if ( temp.startsWith( tag ) )
			{
				return index.intValue();
			}
			String effect = (String) showerOption[1];
			if ( effect.startsWith( tag ) )
			{
				return index.intValue();
			}
		}

		return 0;
	}

	public static final int findSwimmingOption( String tag )
	{
		tag = tag.toLowerCase();
		for ( int i = 0; i < SWIMMING_OPTIONS.length; ++i )
		{
			Object [] swimmingOption = SWIMMING_OPTIONS[i];
			Integer index = (Integer) swimmingOption[2];
			String action = (String) swimmingOption[0];
			if ( action.startsWith( tag ) )
			{
				return index.intValue();
			}
			String effect = (String) swimmingOption[1];
			if ( effect.startsWith( tag ) )
			{
				return index.intValue();
			}
		}

		return 0;
	}

	public static final String prettyStanceName( final int stance )
	{
		switch ( stance )
		{
		case AGGRESSIVE_STANCE:
			return "an aggressive stance";
		case STRATEGIC_STANCE:
			return "a strategic stance";
		case STYLISH_STANCE:
			return "a stylish stance";
		}
		return "an unknown stance";
	}

	public static final String prettyFaxCommand( final int faxCommand )
	{
		switch ( faxCommand )
		{
		case SEND_FAX:
			return "Sending a fax.";
		case RECEIVE_FAX:
			return "Receiving a fax.";
		}
		return "Unknown fax command.";
	}

	public static final String prettyTemperatureName( final int temp )
	{
		switch ( temp )
		{
		case COLD_SHOWER:
			return "a cold";
		case COOL_SHOWER:
			return "a cool";
		case LUKEWARM_SHOWER:
			return "a lukewarm";
		case WARM_SHOWER:
			return "a warm";
		case HOT_SHOWER:
			return "a hot";
		}
		return "an unknown";
	}

	public static final String prettySwimmingName( final int action )
	{
		switch ( action )
		{
		case CANNONBALL:
			return "cannonball";
		case LAPS:
			return "swim laps";
		case SPRINTS:
			return "do submarine sprints";
		}
		return "do something";
	}

	/**
	 * Constructs a new <code>ClanLoungeRequest</code>.
	 *
	 * @param action The identifier for the action you're requesting
	 */

	private ClanLoungeRequest()
	{
		this( SEARCH );
	}

	public ClanLoungeRequest( final int action )
	{
		super( "clan_viplounge.php" );
		this.action = action;
	}

	public ClanLoungeRequest( final int action, final int option )
	{
		super( "clan_viplounge.php" );
		this.action = action;
		this.option = option;
	}

	public static final ClanLoungeRequest buyHotDogRequest( final String name )
	{
		int index = ClanLoungeRequest.hotdogNameToIndex( name );
		if ( index < 0 )
		{
			return null;
		}
		ClanLoungeRequest request = new ClanLoungeRequest( HOT_DOG_STAND, ClanLoungeRequest.hotdogIndexToId( index ) );
		return request;
	}

	public static final AdventureResult VIP_KEY = ItemPool.get( ItemPool.VIP_LOUNGE_KEY, 1 );
	private static final GenericRequest VIP_KEY_REQUEST =
		new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, new AdventureResult[] { ClanLoungeRequest.VIP_KEY } );
	private static GenericRequest VISIT_REQUEST = new ClanLoungeRequest();

	private static void pullVIPKey()
	{
		if ( VIP_KEY.getCount( KoLConstants.inventory ) > 0 || KoLCharacter.inBadMoon() )
		{
			// Do not pull a VIP key if you cannot use it
			return;
		}

		// If you have a VIP Lounge Key in storage, pull it.
		if ( VIP_KEY.getCount( KoLConstants.freepulls ) > 0 )
		{
			RequestThread.postRequest( VIP_KEY_REQUEST );
		}
	}

	public static boolean visitLounge( final int location )
	{
		// Pull a key from storage, if necessary
		ClanLoungeRequest.pullVIPKey();

		// If we have no Clan VIP Lounge key, nothing to do
		if ( VIP_KEY.getCount( KoLConstants.inventory ) == 0 )
		{
			return false;
		}

		VISIT_REQUEST = new ClanLoungeRequest( location );
		RequestThread.postRequest( VISIT_REQUEST );

		// If you are not in a clan, KoL redirects you to
		// clan_signup.php - which we do not follow.
		return VISIT_REQUEST.redirectLocation == null;
	}

	public static boolean visitLounge()
	{
		return ClanLoungeRequest.visitLounge( SEARCH );
	}

	/**
	 * Runs the request. Note that this does not report an error if it fails; it merely parses the results to see if any
	 * gains were made.
	 */

	public static String equipmentName( final String urlString )
	{
		if ( urlString.indexOf( "klaw" ) != -1 )
		{
			return "Deluxe Mr. Klaw \"Skill\" Crane Game";
		}
		if ( urlString.indexOf( "hottub" ) != -1 )
		{
			return "Relaxing Hot Tub";
		}
		if ( urlString.indexOf( "pooltable" ) != -1 )
		{
			return "Pool Table";
		}
		if ( urlString.indexOf( "crimbotree" ) != -1 )
		{
			return "Crimbo Tree";
		}
		if ( urlString.indexOf( "lookingglass" ) != -1 )
		{
			return "Looking Glass";
		}
		if ( urlString.indexOf( "faxmachine" ) != -1 )
		{
			return "Fax Machine";
		}
		if ( urlString.indexOf( "action=shower" ) != -1 )
		{
			return "April Shower";
		}
		if ( urlString.indexOf( "action=swimmingpool" ) != -1 )
		{
			return "Swimming Pool";
		}
		if ( urlString.indexOf( "hotdogstand" ) != -1 )
		{
			return "Hot Dog Stand";
		}
		return null;
	}

	private static String equipmentVisit( final String urlString )
	{
		String name = ClanLoungeRequest.equipmentName( urlString );
		if ( name != null )
		{
			return "Visiting " + name + " in clan VIP lounge";
		}
		return null;
	}

	@Override
	public void run()
	{
		switch ( this.action )
		{
		case ClanLoungeRequest.SEARCH:
			break;

		case ClanLoungeRequest.KLAW:
			this.constructURLString( "clan_viplounge.php" );
			this.addFormField( "action", "klaw" );
			break;

		case ClanLoungeRequest.HOTTUB:
			this.constructURLString( "clan_viplounge.php" );
			this.addFormField( "action", "hottub" );
			break;

		case ClanLoungeRequest.POOL_TABLE:
			RequestLogger.printLine( "Approaching pool table with " + ClanLoungeRequest.prettyStanceName( this.option ) + "." );

			this.constructURLString( "clan_viplounge.php" );
			if ( this.option != 0 )
			{
				this.addFormField( "preaction", "poolgame" );
				this.addFormField( "stance", String.valueOf( this.option ) );
			}
			else
			{
				this.addFormField( "action", "pooltable" );
			}
			break;

		case ClanLoungeRequest.CRIMBO_TREE:
			this.constructURLString( "clan_viplounge.php" );
			this.addFormField( "action", "crimbotree" );
			break;

		case ClanLoungeRequest.LOOKING_GLASS:
			this.constructURLString( "clan_viplounge.php" );
			this.addFormField( "action", "lookingglass" );
			break;

		case ClanLoungeRequest.FAX_MACHINE:
			this.constructURLString( "clan_viplounge.php" );
			switch ( this.option )
			{
			case SEND_FAX:
				KoLmafia.updateDisplay( "Sending a fax." );
				this.addFormField( "preaction", "sendfax" );
				break;
			case RECEIVE_FAX:
				KoLmafia.updateDisplay( "Receiving a fax." );
				this.addFormField( "preaction", "receivefax" );
				break;
			default:
				this.addFormField( "action", "faxmachine" );
				break;
			}
			break;

		case ClanLoungeRequest.APRIL_SHOWER:
			RequestLogger.printLine( "Let's take " + ClanLoungeRequest.prettyTemperatureName( this.option ) + " shower." );

			this.constructURLString( "clan_viplounge.php" );
			if ( this.option != 0 )
			{
				this.addFormField( "preaction", "takeshower" );
				this.addFormField( "temperature", String.valueOf( option ) );
			}
			else
			{
				this.addFormField( "action", "shower" );
			}
			break;

		case ClanLoungeRequest.SWIMMING_POOL:
			RequestLogger.printLine( "Let's " + ClanLoungeRequest.prettySwimmingName( this.option ) + " in the swimming pool." );

			this.constructURLString( "clan_viplounge.php" );
			switch ( this.option )
			{
			case CANNONBALL:
				this.addFormField( "preaction", "goswimming" );
				this.addFormField( "subaction", "screwaround" );
				break;
			case LAPS:
				this.addFormField( "preaction", "goswimming" );
				this.addFormField( "subaction", "laps" );
				break;
			case SPRINTS:
				this.addFormField( "preaction", "goswimming" );
				this.addFormField( "subaction", "submarine" );
				break;
			default:
				this.addFormField( "action", "swimmingpool" );
				break;
			}
			break;

		case ClanLoungeRequest.HOT_DOG_STAND:
			this.constructURLString( "clan_viplounge.php" );
			if ( this.option != 0 )
			{
				this.addFormField( "preaction", "eathotdog" );
				this.addFormField( "whichdog", String.valueOf( option ) );
			}
			else
			{
				this.addFormField( "action", "hotdogstand" );
			}
			break;

		default:
			break;
		}

		super.run();

		if ( this.redirectLocation != null && this.redirectLocation.equals( "clan_signup.php" ) )
		{
			RequestLogger.printLine( "You don't seem to be in a clan!" );
			return;
		}
		
		if ( this.responseText == null )
		{
			// KoL returned a blank page; complain to Jick, not us.
			return;
		}

		switch ( this.action )
		{
		case ClanLoungeRequest.POOL_TABLE:
			if ( responseText.indexOf( "You skillfully defeat" ) != -1 )
			{
				RequestLogger.printLine( "You won the pool game!" );
			}
			else if ( responseText.indexOf( "You play a game of pool against yourself" ) != -1 )
			{
				RequestLogger.printLine( "You beat yourself at pool. Is that a win or a loss?" );
			}
			else if ( responseText.indexOf( "you are unable to defeat" ) != -1 )
			{
				RequestLogger.printLine( "You lost. Boo hoo." );
			}
			else if ( responseText.indexOf( "kind of pooled out" ) != -1 )
			{
				RequestLogger.printLine( "You decided not to play." );
			}
			// Those things are old news.  You only care about the <i>latest</i> gadgets.
			else if ( responseText.indexOf( "Those things are old news" ) != -1 )
			{
				KoLmafia.updateDisplay( "Boring! Nobody plays <i>that</i> any more." );
			}
			else
			{
				RequestLogger.printLine( "Huh? Unknown response." );
			}
			break;

		case ClanLoungeRequest.FAX_MACHINE:
			if ( responseText.indexOf( "Your photocopy slowly slides into the machine" ) != -1 )
			{
				String monster = Preferences.getString( "photocopyMonster" );
				if ( !monster.equals( "" ) )
				{
					KoLmafia.updateDisplay( "You load your photocopied " + monster + " in the fax machine." );
				}
				else
				{
					KoLmafia.updateDisplay( "You load your photocopied monster in the fax machine." );
				}
			}
			else if ( responseText.indexOf( "just be a blank sheet of paper" ) != -1 )
			{
				KoLmafia.updateDisplay( "Your fax machine doesn't have any monster." );
			}
			else if ( responseText.indexOf( "top half of a document prints out" ) != -1 )
			{
				// the message is printed by parseResponse()
			}
			else if ( responseText.indexOf( "waiting for an important fax" ) != -1 )
			{
				KoLmafia.updateDisplay( "You already had a photocopied monster in your inventory." );
			}
			// Those things are old news.  You only care about the <i>latest</i> gadgets.
			else if ( responseText.indexOf( "Those things are old news" ) != -1 )
			{
				KoLmafia.updateDisplay( "The fax machine is <i>so</i> last year." );
			}
			// You approach the fax machine.  Loathing wells up
			// within you and fear clutches at your heart...  What
			// do you want to do?
			else if ( responseText.indexOf( "What do you want to do?" ) != -1 )
			{
				// Simple visit.
			}
			else
			{
				KoLmafia.updateDisplay( "Huh? Unknown response." );
			}
			break;

		case ClanLoungeRequest.APRIL_SHOWER:
			if ( responseText.indexOf( "this is way too hot" ) != -1 )
			{
				RequestLogger.printLine( "You took a hot shower." );
			}
			else if ( responseText.indexOf( "relaxes your muscles" ) != -1 )
			{
				RequestLogger.printLine( "You took a warm shower." );
			}
			else if ( responseText.indexOf( "mind expands" ) != -1 )
			{
				RequestLogger.printLine( "You took a lukewarm shower." );
			}
			else if ( responseText.indexOf( "your goosebumps absorb" ) != -1 )
			{
				RequestLogger.printLine( "You took a cool shower." );
			}
			else if ( responseText.indexOf( "shards of frosty double-ice" ) != -1 )
			{
				RequestLogger.printLine( "You took a cold shower." );
			}
			else if ( responseText.indexOf( "already had a shower today" ) != -1 ||
				  responseText.indexOf( "<table><tr><td></td></tr></table>" ) != -1 )
			{
				RequestLogger.printLine( "You already took a shower today." );
			}
			else if ( responseText.indexOf( "Shower!" ) != -1 )
			{
				// Simple visit.
			}
			else
			{
				RequestLogger.printLine( "Huh? Unknown response." );
			}
			break;

		case ClanLoungeRequest.SWIMMING_POOL:
			if ( this.redirectLocation != null && this.redirectLocation.startsWith( "choice.php" ) )
			{
				RequestLogger.printLine( "You start screwing around in the swimming pool." );
			}
			else if ( responseText.indexOf( "manage to swim" ) != -1 )
			{
				// the message is printed by parseResponse()
			}
			else if ( responseText.indexOf( "doing submarine sprints" ) != -1 )
			{
				// the message is printed by parseResponse()
			}
			else if ( responseText.indexOf( "already worked out in the pool today" ) != -1 ||
				  responseText.indexOf( "<table><tr><td></td></tr></table>" ) != -1 )
			{
				RequestLogger.printLine( "You already worked out in the pool today." );
			}
			else if ( responseText.indexOf( "change into your swimsuit" ) != -1 )
			{
				// Simple visit.
			}
			else
			{
				RequestLogger.printLine( "Huh? Unknown response." );
			}
			break;
		}
	}

	@Override
	public void processResults()
	{
		ClanLoungeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private static final Pattern HOTTUB_PATTERN = Pattern.compile( "hottub(\\d).gif" );

	private static void parseLounge( final String action, final String clan, final String responseText )
	{
		if ( !clan.equals( ClanManager.getClanName( false ) ) )
		{
			RequestLogger.printLine( "You are currently a member of " + clan );
			ClanManager.setClanName( clan );
		}

		Matcher hottubMatcher = HOTTUB_PATTERN.matcher( responseText );
		if ( hottubMatcher.find() )
		{
			Preferences.setInteger( "_hotTubSoaks", 5 - Integer.parseInt( hottubMatcher.group( 1 ) ) );
		}

		// Look at the Crimbo tree and report on whether there is a present waiting.
		if ( responseText.indexOf( "tree5.gif" ) != -1 )
		{
			Preferences.setInteger( "crimboTreeDays", 0 );
			Preferences.setBoolean( "_crimboTree", true );
			// Only log it for a simple visit.
			if ( action == null )
			{
				RequestLogger.printLine( "You have a present under the Crimbo tree in your clan's VIP lounge!" );
			}
		}
		else if ( responseText.indexOf( "crimbotree" ) != -1 )
		{
			if ( !Preferences.getBoolean( "_crimboTree" ) )
			{
				ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.CRIMBO_TREE );
				Preferences.setBoolean( "_crimboTree", true );
				request.run();
			}
		}
		else
		{
			Preferences.setBoolean( "_crimboTree", false );
		}
	}

	// The HTML for the Hot Dog Stand is - surprise! - malformed.
	// The table rows contain a <span></td> rather than <span></span>
	//
	// To eat a hot dog:
	//   clan_viplounge.php?preaction=eathotdog&whichdog=xxx
	//
	// To contribute supplies for a hotdog:
	//   clan_viplounge.php?preaction=hotdogsupply&whichdog=xxx&quantity=yyy
	//
	// whichdog = xxx
	//   -92  basic hot dog
	//   -93  savage macho dog
	//   -94  one with everything
	//   -95  sly dog
	//   -96  devil dog
	//   -97  chilly dog
	//   -98  ghost dog
	//   -99  junkyard dog
	//  -100  wet dog
	//  -102  optimal dog
	//  -101  sleeping dog
	//  -103  video games hot dog

	// <input class=button type=submit value=Eat>
	// <input class=button type=submit value=Eat disabled=disabled style='color: #cccccc'>

	private static void registerHotDog( String name, int id, boolean available, String supply, int needed, int stocked )
	{
		StringBuilder buffer = new StringBuilder();
		if ( !available)
		{
			buffer.append( "(unavailable) " );
		}
		buffer.append( name );
		buffer.append( " (" );
		buffer.append( String.valueOf( id ) );
		buffer.append( ")" );
		if ( !available && needed == 0 )
		{
			buffer.append( " can be unlocked with " );
			buffer.append( supply == null ? "(unknown)" : supply );
		}
		if ( needed > 0 )
		{
			buffer.append( " requires " );
			buffer.append( String.valueOf( needed ) );
			buffer.append( " " );
			buffer.append( supply );
			buffer.append( " (" );
			buffer.append( String.valueOf( stocked ) );
			buffer.append( " in stock)" );
		}
		// System.out.println( buffer.toString() );

		if ( available )
		{
			ClanLoungeRequest.addHotDog( name );
		}
	}

	private static final Pattern HOTDOG_PATTERN = Pattern.compile( 
		".*?<input class=button type=submit value=Eat( disabled.*?)?>.*?<span onclick='descitem.\"(.*?)_food\".*?<b>(.*?)</b>(?:.*?<img.*?title=\"(.*?)\"(?:.*?<b>x (.*?)</b>.*?([0123456789,]*) in stock)?)?", Pattern.DOTALL );

	private static void parseHotDog( final String hotdog )
	{
		Matcher matcher = HOTDOG_PATTERN.matcher( hotdog );
		if ( !matcher.find() )
		{
			return;
		}

		boolean disabled = matcher.group(1) != null;
		String itemIdString = matcher.group(2);
		int itemId = StringUtilities.parseInt( itemIdString );
		String name = matcher.group(3);
		String supply = matcher.group(4);
		String neededString = matcher.group(5);
		int needed = neededString == null ? 0 : StringUtilities.parseInt( neededString );
		String stockedString = matcher.group(6);
		int stocked = stockedString == null ? 0 : StringUtilities.parseInt( stockedString );

		ClanLoungeRequest.registerHotDog( name, itemId, !disabled, supply, needed, stocked );
	}

	private static final Pattern HOTDOG_STAND_PATTERN = Pattern.compile( "<table>(<tr><form action=clan_viplounge.php method=post>.*?)</table>", Pattern.DOTALL );
	private static final Pattern HOTDOG_ROW_PATTERN = Pattern.compile( "<tr>.*?</tr>", Pattern.DOTALL );

	private static void parseHotDogStand( final String responseText )
	{
		// Rebuild list of available hot dogs every time we visit
		ClanLoungeRequest.resetHotdogs();

		// Extract the table for the hot dog stand
		Matcher standMatcher = HOTDOG_STAND_PATTERN.matcher( responseText );
		if ( !standMatcher.find() )
		{
			return;
		}

		String stand = standMatcher.group(1);
		Matcher hotdogMatcher = HOTDOG_ROW_PATTERN.matcher( stand );
		while ( hotdogMatcher.find() )
		{
			ClanLoungeRequest.parseHotDog( hotdogMatcher.group(0) );
		}
	}

	private static final Pattern LOUNGE_PATTERN = Pattern.compile( "<table.*?<b>Clan VIP Lounge</b>.*?<center><b>(?:<a.*?>)?(.*?)(?:</a>)?</b>.*?</center>(<table.*?</table>)", Pattern.DOTALL );

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "clan_viplounge.php" ) || responseText == null )
		{
			return;
		}

		String action = GenericRequest.getAction( urlString );
		Matcher loungeMatcher = LOUNGE_PATTERN.matcher( responseText );
		if ( loungeMatcher.find() )
		{
			ClanLoungeRequest.parseLounge( action, loungeMatcher.group(1), loungeMatcher.group(2) );
		}

		if ( action == null )
		{
			return;
		}

		if ( action.equals( "hottub" ) )
		{
			// No action needed here because this is handled already
			return;
		}

		if ( action.equals( "hotdogstand" ) )
		{
			// Visiting the hot dog stand. See what's on offer
			ClanLoungeRequest.parseHotDogStand( responseText );
			return;
		}

		if ( action.equals( "klaw" ) )
		{
			// You carefully guide the claw over a prize and press
			// the button (which is mahogany inlaid with
			// mother-of-pearl -- very nice!) -- the claw slowly
			// descends...
			if ( responseText.indexOf( "claw slowly descends" ) != -1 )
			{
				Preferences.increment( "_deluxeKlawSummons", 1 );
			}
			// You probably shouldn't play with this machine any
			// more today -- you wouldn't want to look greedy in
			// front of the other VIPs, would you?
			else if ( responseText.indexOf( "you wouldn't want to look greedy" ) != -1 )
			{
				Preferences.setInteger( "_deluxeKlawSummons", 3 );
			}

			return;
		}

		if ( action.equals( "pooltable" ) )
		{
			// You've already played quite a bit of pool today, so
			// you just watch with your hands in your pockets.

			if ( responseText.indexOf( "hands in your pockets" ) != -1 )
			{
				Preferences.setInteger( "_poolGames", 3 );
			}

			return;
		}

		if ( action.equals( "poolgame" ) )
		{
			// You skillfully defeat (player) and take control of
			// the table. Go you!
			//
			// You play a game of pool against yourself.
			// Unsurprisingly, you win! Inevitably, you lose.
			//
			// Try as you might, you are unable to defeat
			// (player). Ah well. You gave it your best.

			if ( responseText.indexOf( "take control of the table" ) != -1 ||
			     responseText.indexOf( "play a game of pool against yourself" ) != -1 ||
			     responseText.indexOf( "you are unable to defeat" ) != -1 )
			{
				Preferences.increment( "_poolGames", 1, 3, false );
			}

			// You're kind of pooled out for today. Maybe you'll be
			// in the mood to play again tomorrow.
			else if ( responseText.indexOf( "pooled out for today" ) != -1 )
			{
				Preferences.setInteger( "_poolGames", 3 );
			}

			return;
		}

		if ( action.equals( "crimbotree" ) )
		{
			// You look under the Crimbo Tree and find a present
			// with your name on it! You excitedly tear it open.
			if ( responseText.indexOf( "You look under the Crimbo Tree and find a present" ) != -1 )
			{
				Preferences.setInteger( "crimboTreeDays", 7 );
			}
			else if ( responseText.indexOf( "Check back tomorrow" ) != -1 )
			{
				Preferences.setInteger( "crimboTreeDays", 1 );
			}
			else if ( responseText.indexOf( "There's nothing under the Crimbo Tree" ) != -1 )
			{
				int ctd;
				String groupStr = "";
				Matcher m = TREE_PATTERN.matcher( responseText );
				boolean matchFound = m.find();
				if( matchFound )
				{
					for (int i=0; i<=m.groupCount(); i++)
					{
						groupStr = m.group(i);
						if( !StringUtilities.isNumeric(groupStr) ) continue;
						ctd = Integer.parseInt( groupStr );
						Preferences.setInteger( "crimboTreeDays", ctd );
						return;
					}
				}
			}

			return;
		}

		if ( action.equals( "lookingglass" ) )
		{
			Preferences.setBoolean( "_lookingGlass", true );
			return;
		}

		if ( action.equals( "faxmachine" ) )
		{
			return;
		}

		if ( action.equals( "sendfax" ) )
		{
			if ( responseText.indexOf( "Your photocopy slowly slides into the machine" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.PHOTOCOPIED_MONSTER, -1 );
				Preferences.setString( "photocopyMonster", "" );
				return;
			}

			return;
		}

		if ( action.equals( "receivefax" ) )
		{
			if ( responseText.indexOf( "top half of a document prints out" ) != -1 )
			{
				String description = DebugDatabase.rawItemDescriptionText( ItemPool.PHOTOCOPIED_MONSTER, true );
				ConsequenceManager.parseItemDesc( ItemDatabase.getDescriptionId( ItemPool.PHOTOCOPIED_MONSTER ), description );

				String monster = Preferences.getString( "photocopyMonster" );
				if ( monster.equals( "" ) )
				{
					monster = "monster";
				}
				KoLmafia.updateDisplay( "You receive a photocopied " + monster + " from the fax machine." );
			}
			return;
		}

		if ( action.equals( "shower" ) )
		{
			if ( responseText.indexOf( "already had a shower today" ) != -1 )
			{
				Preferences.setBoolean( "_aprilShower", true );
			}
			return;
		}

		if ( action.equals( "takeshower" ) )
		{
			if ( responseText.indexOf( "this is way too hot" ) != -1 ||
			     responseText.indexOf( "relaxes your muscles" ) != -1 ||
			     responseText.indexOf( "mind expands" ) != -1 ||
			     responseText.indexOf( "your goosebumps absorb" ) != -1 ||
			     responseText.indexOf( "shards of frosty double-ice" ) != -1 ||
			     responseText.indexOf( "already had a shower today" ) != -1 ||
			     responseText.indexOf( "<table><tr><td></td></tr></table>" ) != -1 )
			{
				Preferences.setBoolean( "_aprilShower", true );
				ResponseTextParser.learnRecipe( urlString, responseText );
			}
			return;
		}

		if ( action.equals( "swimmingpool" ) )
		{
			if ( responseText.indexOf( "already worked out in the pool today" ) != -1 )
			{
				Preferences.setBoolean( "_olympicSwimmingPool", true );
			}
			return;
		}

		if ( action.equals( "goswimming" ) )
		{
			if ( responseText.indexOf( "<table><tr><td></td></tr></table>" ) != -1 )
			{
				Preferences.setBoolean( "_olympicSwimmingPool", true );
				return;
			}

			Matcher m = LAPS_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				KoLmafia.updateDisplay( "You swam " + m.group(1) + " laps." );
				Preferences.setBoolean( "_olympicSwimmingPool", true );
				return;
			}

			m = SPRINTS_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				KoLmafia.updateDisplay( "You did " + m.group(1) + " submarine sprints." );
				Preferences.setBoolean( "_olympicSwimmingPool", true );
				return;
			}
			return;
		}

		if ( action.equals( "eathotdog" ) )
		{
			// Do nothing if consumption of a basic hot dog failed
			// Do nothing if overeating on basic hot dogs causes you to lose stats
			// Don't be fooled!
			//   You lose some of an effect: Got Milk
			if ( responseText.indexOf( "You don't feel up to eating that" ) != -1 ||
			     ( responseText.indexOf( "You lose some of an effect" ) == -1 &&
			       responseText.indexOf( "You lose" ) != -1 ) )
			{
				return;
			}
			// If consumption of a fancy hot dog failed, remember
			if ( responseText.indexOf( "You aren't in the mood for any more fancy dogs today" ) != -1 )
			{
				// Remove fancy hot dogs from the list
				ClanLoungeRequest.resetHotdogs( 1 );
				Preferences.setBoolean( "_fancyHotDogEaten", true );
				return;
			}
			Matcher m = WHICHDOG_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return;
			}
			int index = ClanLoungeRequest.hotdogIdToIndex( StringUtilities.parseInt( m.group( 1 ) ) );
			if ( index < 0 )
			{
				return;
			}
			int fullness = ClanLoungeRequest.hotdogIndexToFullness( index );
			if ( fullness > 0 && !responseText.contains( "Fullness" ) ) // if fullness display is on, ResultProcessor will handle incrementing fullness
			{
				Preferences.increment( "currentFullness", fullness );
			}
			String name = ClanLoungeRequest.hotdogIndexToName( index );
			if ( name.equals( "optimal dog" ) )
			{
				// Remove existing Fortune Cookie counters and set one to 0.
				TurnCounter.stopCounting( "Fortune Cookie" );
				TurnCounter.stopCounting( "Semirare window begin" );
				TurnCounter.stopCounting( "Semirare window end" );
				TurnCounter.startCounting( 0, "Fortune Cookie", "fortune.gif" );
			}
			if ( index > 0 )
			{
				// Remove fancy hot dogs from the list
				ClanLoungeRequest.resetHotdogs( 1 );
				Preferences.setBoolean( "_fancyHotDogEaten", true );
			}

			return;
		}
		else if ( action.equals( "hotdogsupply" ) )
		{
			// You have put some hot dog making supplies into the
			// hot dog cart man's hot dog cart supply crate.
			if ( responseText.indexOf( "You have put some hot dog making supplies" ) == -1 )
			{
				return;
			}

			//   clan_viplounge.php?preaction=hotdogsupply&whichdog =-101&quantity=10
			Matcher m = WHICHDOG_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return;
			}
			int index = ClanLoungeRequest.hotdogIdToIndex( StringUtilities.parseInt( m.group( 1 ) ) );
			if ( index < 0 )
			{
				return;
			}
			AdventureResult item = ClanLoungeRequest.hotdogIndexToItem( index );
			if ( item == null )
			{
				return;
			}
			m = QUANTITY_PATTERN.matcher( urlString );
			int quantity = m.find() ? StringUtilities.parseInt( m.group(1) ) : 1;
			ResultProcessor.processItem( item.getItemId(), -1 * quantity );
			return;
		}
	}

	public static void getBreakfast()
	{
		// No Clan Lounge in Bad Moon
		if ( KoLCharacter.inBadMoon() && !KoLCharacter.kingLiberated() )
		{
			return;
		}

		// Visit the lounge to see what furniture is available
		if ( !visitLounge() )
		{
			return;
		}

		// The Klaw can be accessed regardless of whether or not
		// you are in hardcore, so handle it first.
		//
		// Unlike the regular Klaw, there is no message to tell you
		// that you are done for the day except when you try one too
		// many times: "You probably shouldn't play with this machine
		// any more today -- you wouldn't want to look greedy in front
		// of the other VIPs, would you?"

		ClanLoungeRequest request = new ClanLoungeRequest( ClanLoungeRequest.KLAW );
		while ( Preferences.getInteger( "_deluxeKlawSummons" ) < 3 )
		{
			request.run();
		}

		// Not every clan has a looking glass
		if ( VISIT_REQUEST.responseText.indexOf( "lookingglass.gif" ) != -1 &&
		     !Preferences.getBoolean( "_lookingGlass" ) )
		{
			request = new ClanLoungeRequest( ClanLoungeRequest.LOOKING_GLASS );
			request.run();
		}

		// Not every clan has a crimbo tree
		if ( VISIT_REQUEST.responseText.indexOf( "crimbotree" ) != -1 )
		{
			Preferences.setBoolean( "_crimboTree", true );
		}

		if ( VISIT_REQUEST.responseText.indexOf( "tree5.gif" ) != -1 )
		{
			// Get the crimbo gift now whenever breakfast is run, since there is
			// no reason not to anymore.
			request = new ClanLoungeRequest( ClanLoungeRequest.CRIMBO_TREE );
			request.run();
		}

		// Not every clan has a swimming pool
		if ( VISIT_REQUEST.responseText.contains( "vippool.gif" ) && !Preferences.getBoolean( "_olympicSwimmingPoolItemFound" ) )
		{
			try
			{
				RequestThread.postRequest( new ClanLoungeRequest( ClanLoungeRequest.SWIMMING_POOL, CANNONBALL ) );
				RequestThread.postRequest( new ClanLoungeSwimmingPoolRequest( ClanLoungeSwimmingPoolRequest.HANDSTAND ) );
				RequestThread.postRequest( new ClanLoungeSwimmingPoolRequest( ClanLoungeSwimmingPoolRequest.TREASURE ) );
			}
			finally
			{
				RequestThread.postRequest( new ClanLoungeSwimmingPoolRequest( ClanLoungeSwimmingPoolRequest.GET_OUT ) );
			}
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "clan_viplounge.php" ) )
		{
			return false;
		}

		String message = ClanLoungeRequest.equipmentVisit( urlString );

		if ( message == null )
		{
			String action = GenericRequest.getAction( urlString );
			if ( action.equals( "poolgame" ) )
			{
				Matcher m = STANCE_PATTERN.matcher( urlString );
				if ( !m.find() )
				{
					return false;
				}
				int stance = StringUtilities.parseInt( m.group(1) );
				if ( stance < 1 || stance > POOL_GAMES.length )
				{
					return false;
				}
				message = "pool " + (String)POOL_GAMES[ stance - 1 ][0];
			}
			else if ( action.equals( "sendfax" ) ||
				  action.equals( "receivefax" ) )
			{
				Matcher m = FAX_PATTERN.matcher( urlString );
				if ( !m.find() )
				{
					return false;
				}
				String faxCommand = m.group(1) ;
				if ( !faxCommand.equals( "send") && !faxCommand.equals( "receive" ) )
				{
					return false;
				}
				message = "fax " + faxCommand;
			}
			else if ( action.equals( "takeshower" ) )
			{
				Matcher m = TEMPERATURE_PATTERN.matcher( urlString );
				if ( !m.find() )
				{
					return false;
				}
				int temp = StringUtilities.parseInt( m.group(1) );
				if ( temp < 1 || temp > SHOWER_OPTIONS.length )
				{
					return false;
				}
				message = "shower " + (String)SHOWER_OPTIONS[ temp - 1 ][0];
			}
			else if ( action.equals( "goswimming" ) )
			{
				Matcher m = SWIMMING_POOL_PATTERN.matcher( urlString );
				if ( !m.find() )
				{
					return false;
				}
				String poolCommand = m.group(1) ;
				if ( !poolCommand.equals( "screwaround") &&
				     !poolCommand.equals( "laps" ) &&
				     !poolCommand.equals( "submarine" ) )
				{
					return false;
				}
				message = "swimming pool " + poolCommand;
			}
			else if ( action.equals( "eathotdog" ) )
			{
				//   clan_viplounge.php?preaction=eathotdog&whichdog=xxx
				Matcher m = WHICHDOG_PATTERN.matcher( urlString );
				if ( !m.find() )
				{
					return false;
				}
				String hotdog = ClanLoungeRequest.hotdogIdToName( StringUtilities.parseInt( m.group( 1 ) ) );
				if ( hotdog == null )
				{
					return false;
				}
				message = "eat " + hotdog;
			}
			else if ( action.equals( "hotdogsupply" ) )
			{
				//   clan_viplounge.php?preaction=hotdogsupply&whichdog =-101&quantity=10
				Matcher m = WHICHDOG_PATTERN.matcher( urlString );
				if ( !m.find() )
				{
					return false;
				}
				int index = ClanLoungeRequest.hotdogIdToIndex( StringUtilities.parseInt( m.group( 1 ) ) );
				AdventureResult item = ClanLoungeRequest.hotdogIndexToItem( index );
				if ( item == null )
				{
					return false;
				}
				m = QUANTITY_PATTERN.matcher( urlString );
				int quantity = m.find() ? StringUtilities.parseInt( m.group(1) ) : 1;
				message = "stock Hot Dog Stand with " + quantity + " " + item.getPluralName( quantity );
			}
			else
			{
				return false;
			}
		}
		else
		{
			RequestLogger.printLine( message );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
