/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.PortalRequest;
import net.sourceforge.kolmafia.request.TelescopeRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CampgroundRequest
	extends GenericRequest
{
	public static final Pattern ACTION_PATTERN = Pattern.compile( "action=([^&]*)" );
	private static final Pattern LIBRAM_PATTERN =
		Pattern.compile( "Summon (Candy Heart|Party Favor|Love Song) *.[(]([\\d,]+) MP[)]" );
	private static final Pattern HOUSING_PATTERN =
		Pattern.compile( "/rest(\\d+)(tp)?(_free)?.gif" );
	private static final Pattern FURNISHING_PATTERN =
		Pattern.compile( "<b>(?:an? )?(.*?)</b>" );

	private static int currentDwellingLevel = 0;
	private static AdventureResult currentDwelling = null;
	private static AdventureResult currentBed = null;

	public static void reset()
	{
		KoLConstants.campground.clear();
		CampgroundRequest.currentDwellingLevel = 0;
		CampgroundRequest.currentDwelling = null;
		CampgroundRequest.currentBed = null;
	}

	private final String action;

	/**
	 * Constructs a new <code>CampgroundRequest</code> with the specified action in mind.
	 */

	public CampgroundRequest( final String action )
	{
		super( "campground.php" );
		this.addFormField( "action", action );
		this.action = action;
	}

	/**
	 * Constructs a new <code>CampgroundRequest</code>.
	 */

	public CampgroundRequest()
	{
		this( "inspectdwelling" );
	}

	public int getAdventuresUsed()
	{
		return this.action.equals( "rest" ) ? 1 : 0;
	}

	public void run()
	{
		if ( this.action.equals( "rest" ) &&
		     KoLCharacter.getCurrentHP() == KoLCharacter.getMaximumHP() &&
		     KoLCharacter.getCurrentMP() == KoLCharacter.getMaximumMP() )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "You don't need to rest." );
			return;
		}

		super.run();
	}

	public void processResults()
	{
		CampgroundRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "campground.php" ) )
		{
			return;
		}

		Matcher matcher= CampgroundRequest.ACTION_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			CampgroundRequest.parseCampground( responseText );
			return;
		}

		String action = matcher.group(1);

		if ( action.endsWith( "powerelvibratoportal" ) )
		{
			PortalRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( action.startsWith( "telescope" ) )
		{
			TelescopeRequest.parseResponse( urlString, responseText );
			return;
		}

		// Using a book skill from the Mystic Bookshelf does this:
		//   campground.php?quantity=1&preaction=summonlovesongs&pwd
		// 
		// Using a book skill from the skill menu redirects to the
		// above URL with an additional field:
		//   skilluse=1

		if ( action.startsWith( "summon" ) )
		{
			UseSkillRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( action.equals( "bookshelf" ) )
		{
			CampgroundRequest.parseBookTitles( responseText );
			return;
		}

		if ( action.equals( "rest" ) )
		{
			Preferences.increment( "timesRested", 1 );
			return;
		}

		if ( action.equals( "inspectdwelling" ) )
		{
			CampgroundRequest.parseCampground( responseText );
			CampgroundRequest.parseDwelling( responseText );
			return;
		}
	}

	private static final void parseCampground( final String responseText )
	{

		KoLCharacter.setChef( responseText.indexOf( "mode=cook" ) != -1 );
		KoLCharacter.setBartender( responseText.indexOf( "mode=cocktail" ) != -1 );
		KoLCharacter.setTelescope( responseText.indexOf( "action=telescope" ) != -1 );
		KoLCharacter.setBookshelf( responseText.indexOf( "action=bookshelf" ) != -1 );
	}

	private static final void parseDwelling( final String responseText )
	{
		Matcher m = HOUSING_PATTERN.matcher( responseText );
		if ( !m.find() )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Unable to parse housing!" );
			return;
		}

		CampgroundRequest.reset();
		CampgroundRequest.currentDwellingLevel = StringUtilities.parseInt( m.group( 1 ) );

		int itemId = -1;
		switch ( CampgroundRequest.currentDwellingLevel )
		{
		case 0:
			itemId = ItemPool.BIG_ROCK;	// placeholder for "the ground"
			break;
		case 1:
			itemId = ItemPool.NEWBIESPORT_TENT;
			break;
		case 2:
			itemId = ItemPool.BARSKIN_TENT;
			break;
		case 3:
			itemId = ItemPool.COTTAGE;
			break;
		case 4:
			itemId = ItemPool.HOUSE;
			break;
		case 5:
			itemId = ItemPool.SANDCASTLE;
			break;
		case 6:
			itemId = ItemPool.TWIG_HOUSE;
			break;
		case 7:
			itemId = ItemPool.HOBO_FORTRESS;
			break;
		default:
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Unrecognized housing type!" );
		}

		if ( itemId != -1 )
		{
			CampgroundRequest.currentDwelling = ItemPool.get( itemId, 1 );
			KoLConstants.campground.add( CampgroundRequest.currentDwelling );
		}
			
		if ( m.group( 2 ) != null )
		{
			KoLConstants.campground.add( ItemPool.get( ItemPool.TOILET_PAPER, 1 ) );
		}
			
		// TODO: check free rest status (m.group(3)!=null)
		// against timesRested, adjust it if there appear to
		// have been rests used outside of KoLmafia.
			
		int startIndex = responseText.indexOf( "Your dwelling has the following stuff" );
		int endIndex = responseText.indexOf( "<b>Your Campsite</b>", startIndex + 1 );
		if ( startIndex > 0 && endIndex > 0 )
		{
			m = FURNISHING_PATTERN.matcher( responseText.substring( startIndex, endIndex ) ); 
			while ( m.find() )
			{
				String name = m.group(1);

				if ( name.equals( "Really Good Feng Shui" ) )
				{
					name = "Feng Shui for Big Dumb Idiots";
				}

				AdventureResult ar = ItemPool.get( name, 1 );
				if ( CampgroundRequest.isBedding( ar.getItemId() ) )
				{
					CampgroundRequest.currentBed = ar;
				}

				KoLConstants.campground.add( ar );
			}
		}
			
		findImage( responseText, "pagoda.gif", ItemPool.PAGODA_PLANS );
		findImage( responseText, "bartender.gif", ItemPool.BARTENDER );
		findImage( responseText, "bartender2.gif", ItemPool.CLOCKWORK_BARTENDER );
		findImage( responseText, "chef.gif", ItemPool.CHEF );
		findImage( responseText, "chef2.gif", ItemPool.CLOCKWORK_CHEF );
		findImage( responseText, "maid.gif", ItemPool.MAID );
		findImage( responseText, "maid2.gif", ItemPool.CLOCKWORK_MAID );
		findImage( responseText, "scarecrow.gif", ItemPool.SCARECROW );
		findImage( responseText, "golem.gif", ItemPool.MEAT_GOLEM );
		findImage( responseText, "bouquet.gif", ItemPool.PRETTY_BOUQUET );
		findImage( responseText, "pfsection.gif", ItemPool.PICKET_FENCE );
		findImage( responseText, "bfsection.gif", ItemPool.BARBED_FENCE );
	}

	private static void findImage( final String responseText, final String filename, final int itemId )
	{
		int count = 0;
		int i = responseText.indexOf( filename );
		while ( i != -1 )
		{
			++count;
			i = responseText.indexOf( filename, i + 1 );		
		}

		if ( count > 0 )
		{
			KoLConstants.campground.add( ItemPool.get( itemId, count ) );
		}
	}

	public static AdventureResult getCurrentDwelling()
	{
		return currentDwelling;
	}

	public static int getCurrentDwellingLevel()
	{
		return currentDwellingLevel;
	}

	public static AdventureResult getCurrentBed()
	{
		return currentBed;
	}

	public static boolean isDwelling( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.NEWBIESPORT_TENT:
		case ItemPool.BARSKIN_TENT:
		case ItemPool.COTTAGE:
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.TWIG_HOUSE:
		case ItemPool.HOBO_FORTRESS:
			return true;
		}
		return false;
	}

	public static int dwellingLevel( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.NEWBIESPORT_TENT:
			return 1;
		case ItemPool.BARSKIN_TENT:
			return 2;
		case ItemPool.COTTAGE:
			return 3;
		case ItemPool.HOUSE:
			return 4;
		case ItemPool.SANDCASTLE:
			return 5;
		case ItemPool.TWIG_HOUSE:
			return 6;
		case ItemPool.HOBO_FORTRESS:
			return 7;
		}
		return 0;
	}

	public static boolean isBedding( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
		case ItemPool.HOT_BEDDING:
		case ItemPool.COLD_BEDDING:
		case ItemPool.STENCH_BEDDING:
		case ItemPool.SPOOKY_BEDDING:
		case ItemPool.SLEAZE_BEDDING:
			return true;
		}
		return false;
	}

	private static final String[][] BOOKS =
	{
		{
			"Tome of Snowcone Summoning",
			"Summon Snowcones"
		},
		{
			"Tome of Sticker Summoning",
			"Summon Stickers"
		},
		{
			// The bookshelf currently says:
			// "McPhee's Grimoire of Hilarious Item Summoning"
			// gives access to "Summon Hilarious Items".
			//
			// The item is currently named:
			// "McPhee's Grimoire of Hilarious Object Summoning"
			// and gives access to "Summon Hilarious Objects".
			//
			// One or the other will eventually change, I predict.
			"McPhee's Grimoire",
			"Summon Hilarious Objects"
		},
		{
			"Sp'n-Zor's Grimoire",
			"Summon Tasteful Items"
		},
		{
			"Libram of Candy Heart Summoning",
			"Summon Candy Hearts"
		},
		{
			"Libram of Divine Favors",
			"Summon Party Favor"
		},
		{
			"Libram of Love Songs",
			"Summon Love Song"
		},
	};

	private static void parseBookTitles( final String responseText )
	{
		// You can't use Mr. Skills in bad moon, so don't check

		if ( KoLCharacter.inBadMoon() && !KoLCharacter.skillsRecalled() )
			return;

		String libram = null;
		for ( int i = 0; i < BOOKS.length; ++i )
		{
			String book = BOOKS[i][0];
			if ( responseText.indexOf( book ) != -1 )
			{
				String skill = BOOKS[i][1];
				KoLCharacter.addAvailableSkill( skill );
				if ( book.startsWith( "Libram" ) )
				{
					libram = skill;
				}
			}
		}

		if ( libram != null )
		{
			Matcher matcher = CampgroundRequest.LIBRAM_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				int cost = StringUtilities.parseInt( matcher.group(2) );
				SkillDatabase.setLibramSkillCasts( cost );
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "campground.php" ) )
		{
			return false;
		}

		Matcher matcher= CampgroundRequest.ACTION_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			// Simple visit. Nothing to log.
			return false;
		}

		String action = matcher.group(1);

		// Dispatch campground requests to other classes

		if ( action.endsWith( "elvibratoportal" ) )
		{
			return PortalRequest.registerRequest( urlString );
		}

		if ( action.startsWith( "telescope" ) )
		{
			return TelescopeRequest.registerRequest( urlString );
		}

		if ( action.startsWith( "summon" ) )
		{
			return UseSkillRequest.registerRequest( urlString );
		}

		// Dispatch campground requests from this class

		if ( action.equals( "bookshelf" ) )
		{
			// Nothing to log.
			return false;
		}

		if ( action.equals( "inspectdwelling" ) )
		{
			// Nothing to log.
			return false;
		}

		if ( action.equals( "rest" ) )
		{
			String message = "[" + KoLAdventure.getAdventureCount() + "] Rest in your dwelling";
			RequestLogger.printLine( "" );
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
			return true;
		}

                // Unknown action.
		return false;
	}
}
