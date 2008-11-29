/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CampgroundRequest
	extends GenericRequest
{
	private static final Pattern LIBRAM_PATTERN =
		Pattern.compile( "Summon (Candy Heart|Party Favor) *.[(]([\\d,]+) MP[)]" );
	private static final Pattern HOUSING_PATTERN =
		Pattern.compile( "/rest(\\d+)(tp)?(_free)?.gif" );
	private static final Pattern FURNISHING_PATTERN =
		Pattern.compile( "<b>(?:an? )?(.*?)</b>" );

	private final String action;

	/**
	 * Constructs a new <code>CampgroundRequest</code>.
	 */

	public CampgroundRequest()
	{
		this( "inspectdwelling" );
	}

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
	 * Runs the campground request, updating theas appropriate.
	 */

	public void run()
	{
		if ( this.action.equals( "rest" ) )
		{
			if ( KoLCharacter.getCurrentHP() == KoLCharacter.getMaximumHP() && KoLCharacter.getCurrentMP() == KoLCharacter.getMaximumMP() )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "You don't need to rest." );
				return;
			}
		}

		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( this.responseCode != 200 )
		{
			return;
		}

		// Parse the results and determine campground equipment.
	}

	public void processResults()
	{
		KoLCharacter.setChef( this.responseText.indexOf( "mode=cook" ) != -1 );
		KoLCharacter.setBartender( this.responseText.indexOf( "mode=cocktail" ) != -1 );
		KoLCharacter.setTelescope( this.responseText.indexOf( "action=telescope" ) != -1 );
		KoLCharacter.setBookshelf( this.responseText.indexOf( "action=bookshelf" ) != -1 );

		// Make sure that the character received something if
		// they were looking for toast

		if ( this.action.equals( "toast" ) )
		{
			if ( this.responseText.indexOf( "acquire" ) == -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "No more toast left." );
			}
		}

		// Parse skills from names of books

		else if ( this.action.equals( "bookshelf" ) )
		{
			parseBookTitles();
		}
		else if ( this.action.equals( "rest" ) )
		{
			Preferences.increment( "timesRested", 1 );
		}
		else if ( this.action.equals( "inspectdwelling" ) )
		{
			Matcher m = HOUSING_PATTERN.matcher( this.responseText );
			if ( !m.find() )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Unable to parse housing!" );
				return;
			}
			KoLConstants.campground.clear();
			int itemId = 666;
			switch ( StringUtilities.parseInt( m.group( 1 ) ) )
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
			KoLConstants.campground.add( ItemPool.get( itemId, 1 ) );
			
			if ( m.group( 2 ) != null )
			{
				KoLConstants.campground.add( ItemPool.get( ItemPool.TOILET_PAPER, 1 ) );
			}
			
			// TODO: check free rest status (m.group(3)!=null) against timesRested,
			// adjust it if there appear to have been rests used outside of KoLmafia.
			
			int startIndex = this.responseText.indexOf( "Your dwelling has the following stuff" );
			int endIndex = this.responseText.indexOf( "<b>Your Campsite</b>", startIndex + 1 );
			if ( startIndex > 0 && endIndex > 0 )
			{
				m = FURNISHING_PATTERN.matcher( this.responseText.substring( startIndex, endIndex ) );
				while ( m.find() )
				{
					KoLConstants.campground.add( ItemPool.get( m.group( 1 ), 1 ) );
				}
			}
			
			findImage( "fengshui.gif", ItemPool.FENG_SHUI );
			findImage( "pagoda.gif", ItemPool.PAGODA_PLANS );
			findImage( "bartender.gif", ItemPool.BARTENDER );
			findImage( "bartender2.gif", ItemPool.CLOCKWORK_BARTENDER );
			findImage( "chef.gif", ItemPool.CHEF );
			findImage( "chef2.gif", ItemPool.CLOCKWORK_CHEF );
			findImage( "maid.gif", ItemPool.MAID );
			findImage( "maid2.gif", ItemPool.CLOCKWORK_MAID );
			findImage( "scarecrow.gif", ItemPool.SCARECROW );
			findImage( "golem.gif", ItemPool.MEAT_GOLEM );
			findImage( "bouquet.gif", ItemPool.PRETTY_BOUQUET );
			findImage( "pfsection.gif", ItemPool.PICKET_FENCE );
			findImage( "bfsection.gif", ItemPool.BARBED_FENCE );
		}
	}

	private void findImage( String filename, int itemId )
	{
		String text = this.responseText;
		int count = 0;
		int i = text.indexOf( filename );
		while ( i != -1 )
		{
			++count;
			i = text.indexOf( filename, i + 1 );		
		}
		if ( count > 0 )
		{
			KoLConstants.campground.add( ItemPool.get( itemId, count ) );
		}
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
	};

	private void parseBookTitles()
	{
		// You can't use Mr. Skills in bad moon, so don't check

		if ( KoLCharacter.inBadMoon() )
			return;

		String libram = null;
		for ( int i = 0; i < BOOKS.length; ++i )
		{
			String book = BOOKS[i][0];
			if ( this.responseText.indexOf( book ) != -1 )
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
			Matcher matcher = CampgroundRequest.LIBRAM_PATTERN.matcher( this.responseText );
			if ( matcher.find() )
			{
				int cost = StringUtilities.parseInt( matcher.group(2) );
				SkillDatabase.setLibramSkillCasts( cost );
			}
		}
	}

	/**
	 * An alternative method to doing adventure calculation is determining how many adventures are used by the given
	 * request, and subtract them after the request is done. This number defaults to <code>zero</code>; overriding
	 * classes should change this value to the appropriate amount.
	 *
	 * @return The number of adventures used by this request.
	 */

	public int getAdventuresUsed()
	{
		return this.responseCode != 200 || !this.action.equals( "rest" ) ? 0 : 1;
	}
}
