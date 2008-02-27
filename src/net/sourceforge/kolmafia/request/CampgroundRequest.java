/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CampgroundRequest
	extends GenericRequest
{
	private static final Pattern LIBRAM_PATTERN = Pattern.compile( "Summon (Candy Heart|Party Favor) *.[(]([\\d,]+) MP[)]" );
	private static boolean relaxAllowed = false;

	private final String action;

	/**
	 * Constructs a new <code>CampgroundRequest</code>.
	 */

	public CampgroundRequest()
	{
		super( "campground.php" );
		this.action = "";
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

		if ( this.action.equals( "relax" ) )
		{
			if ( !CampgroundRequest.relaxAllowed || KoLCharacter.getCurrentMP() == KoLCharacter.getMaximumMP() )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "You don't need to relax." );
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
		CampgroundRequest.relaxAllowed = this.responseText.indexOf( "relax" ) != -1;

		KoLCharacter.setChef( this.responseText.indexOf( "cook.php" ) != -1 );
		KoLCharacter.setBartender( this.responseText.indexOf( "cocktail.php" ) != -1 );
		KoLCharacter.setToaster( this.responseText.indexOf( "action=toast" ) != -1 );
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
	}

	private static final String[][] BOOKS =
	{
		{
			"Tome of Snowcone Summoning",
			"Summon Snowcone"
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
		return this.responseCode != 200 || !this.action.equals( "rest" ) && !this.action.equals( "relax" ) ? 0 : 1;
	}
}
