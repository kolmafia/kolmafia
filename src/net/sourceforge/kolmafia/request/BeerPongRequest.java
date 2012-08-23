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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BeerPongRequest
	extends GenericRequest
{
	private static final Pattern RESPONSE_PATTERN = Pattern.compile( "response=(\\d*)" );

	// The first string is an insult you hear from Rickets.
	// The second string is the insult you must use in reply.

	public static final String [][] PIRATE_INSULTS =
	{
		{
			"Arrr, the power of me serve'll flay the skin from yer bones!",
			"Obviously neither your tongue nor your wit is sharp enough for the job."
		},
		{
			"Do ye hear that, ye craven blackguard?  It be the sound of yer doom!",
			"It can't be any worse than the smell of your breath!"
		},
		{
			"Suck on <i>this</i>, ye miserable, pestilent wretch!",
			"That reminds me, tell your wife and sister I had a lovely time last night."
		},
		{
			"The streets will run red with yer blood when I'm through with ye!",
			"I'd've thought yellow would be more your color."
		},
		{
			"Yer face is as foul as that of a drowned goat!",
			"I'm not really comfortable being compared to your girlfriend that way."
		},
		{
			"When I'm through with ye, ye'll be crying like a little girl!",
			"It's an honor to learn from such an expert in the field."
		},
		{
			"In all my years I've not seen a more loathsome worm than yerself!",
			"Amazing!  How do you manage to shave without using a mirror?"
		},
		{
			"Not a single man has faced me and lived to tell the tale!",
			"It only seems that way because you haven't learned to count to one."
		},
		// Everything after this is a failure
		{
			null,
			"How appropriate, you fight like a cow."
		},
		{
			null,
			"Look, a three-headed monkey!"
		},
		{
			null,
			"I'm rubber and you're glue."
		},
		{
			null,
			"I know you are, but what am I?"
		},
		{
			null,
			"First you'd better stop waving it around like a feather-duster."
		},
	};

	public static final int VALID_PIRATE_INSULTS = 8;

	static {
		for ( int i = 0; i < BeerPongRequest.VALID_PIRATE_INSULTS; ++i )
		{
			StringUtilities.registerPrepositions( BeerPongRequest.PIRATE_INSULTS[ i ][ 0 ] );
			StringUtilities.registerPrepositions( BeerPongRequest.PIRATE_INSULTS[ i ][ 1 ] );
		}
	}

	private static final Pattern ROUND1_PATTERN = Pattern.compile( "The pirate lobs his ball \\w+ your cups. &quot;(.*?)&quot; he taunts" );
	private static final Pattern ROUND2_PATTERN = Pattern.compile( "&quot;However -- (.*?)&quot;" );
	private static final Pattern ROUND3_PATTERN = Pattern.compile( "and growls &quot;(.*?)&quot;" );

	public static final String findRicketsInsult( final String text )
	{
		Matcher matcher = BeerPongRequest.ROUND1_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return matcher.group(1);
		}

		matcher = BeerPongRequest.ROUND2_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return matcher.group(1);
		}

		matcher = BeerPongRequest.ROUND3_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return matcher.group(1);
		}

		return null;
	}

	public static final int findPirateInsult( String insult )
	{
		if ( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId() ==
			ItemPool.SWORD_PREPOSITIONS )
		{
			insult = StringUtilities.lookupPrepositions( insult );
		}

		for ( int i = 0; i < BeerPongRequest.VALID_PIRATE_INSULTS; ++i )
		{
			if ( insult.equals( BeerPongRequest.PIRATE_INSULTS[i][0] ) )
			{
				return i + 1;
			}
		}
		return 0;
	}

	public static final int findPirateRetort( String insult )
	{
		if ( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId() ==
			ItemPool.SWORD_PREPOSITIONS )
		{
			insult = StringUtilities.lookupPrepositions( insult );
		}

		for ( int i = 0; i < BeerPongRequest.VALID_PIRATE_INSULTS; ++i )
		{
			if ( insult.equals( BeerPongRequest.PIRATE_INSULTS[i][1] ) )
			{
				return i + 1;
			}
		}
		return 0;
	}

	public static final String pirateRetort( final int insult )
	{
		if ( insult > 0 && insult <= BeerPongRequest.PIRATE_INSULTS.length )
		{
			return BeerPongRequest.PIRATE_INSULTS[insult - 1][1];
		}
		return null;
	}

	public static final String knownPirateRetort( final int insult )
	{
		KoLCharacter.ensureUpdatedPirateInsults();
		if ( Preferences.getBoolean( "lastPirateInsult" + insult ) )
		{
			return BeerPongRequest.PIRATE_INSULTS[insult - 1][1];
		}
		return null;
	}

	public static final int countPirateInsults()
	{
		KoLCharacter.ensureUpdatedPirateInsults();

		int count = 0;
		for ( int i = 1; i <= BeerPongRequest.VALID_PIRATE_INSULTS; ++i )
		{
			if ( Preferences.getBoolean( "lastPirateInsult" + i ) )
			{
				count += 1;
			}
		}

		return count;
	}

	public static final float pirateInsultOdds()
	{
		return BeerPongRequest.pirateInsultOdds( BeerPongRequest.countPirateInsults() );
	}

	public static final float pirateInsultOdds( int count )
	{
		// If you know less than three insults, you can't possibly win.
		if ( count < 3 )
		{
			return 0.0f;
		}

		// Otherwise, your probability of winning is:
		//   ( count ) / 8	the first contest
		//   ( count - 1 ) / 7	the second contest
		//   ( count - 2 ) / 6	the third contest

		float odds = 1.0f;
		float fcount = count * 1.0f;

		odds *= ( fcount ) / BeerPongRequest.VALID_PIRATE_INSULTS;
		odds *= ( fcount - 1 ) / ( BeerPongRequest.VALID_PIRATE_INSULTS - 1 );
		odds *= ( fcount - 2 ) / ( BeerPongRequest.VALID_PIRATE_INSULTS - 2 );

		return odds;
	}

	public BeerPongRequest()
	{
		super( "beerpong.php" );
	}

	public BeerPongRequest( final int response)
	{
		this();
		this.addFormField( "response", String.valueOf( response ) );
	}

	@Override
	public void processResults()
	{
		BeerPongRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private static final Pattern RETORT_FORM_PATTERN = Pattern.compile( "<form action=beerpong.php.*?</form>" );
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<option value=(\\d+)>" );

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "beerpong.php" ) )
		{
			return;
		}
		
		// winwinwin
		if ( responseText.indexOf( "After a few victory laps atop the ocean of revelers" ) != -1 )
		{
			QuestDatabase.setQuestIfBetter( Quest.PIRATE, "step5" );
		}

		// See if Rickets is offering an insult
		String insult = BeerPongRequest.findRicketsInsult( responseText );
		if ( insult != null )
		{
			RequestLogger.updateSessionLog( "Insult: " + insult );
		}

		// See if there is a Retort! form
		Matcher formMatcher = RETORT_FORM_PATTERN.matcher( responseText );
		if ( !formMatcher.find() )
		{
			return;
		}

		// There is. Pick out the insults that we know.
		KoLCharacter.ensureUpdatedPirateInsults();
		Matcher optionMatcher = OPTION_PATTERN.matcher( formMatcher.group( 0 ) );
		while ( optionMatcher.find() )
		{
			int option = StringUtilities.parseInt( optionMatcher.group( 1 ) );
			if ( option < 1 )
			{
				// This should never happen.
				continue;
			}
			if ( option > BeerPongRequest.VALID_PIRATE_INSULTS )
			{
				// Skip failing insults
				break;
			}

			// Mark this insult as known
			Preferences.setBoolean( "lastPirateInsult" + option, true );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "beerpong.php" ) )
		{
			return false;
		}

		Matcher matcher = RESPONSE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Starting a game of beer pong" );
			return true;
		}

		int insult = StringUtilities.parseInt( matcher.group( 1 ) );
		String retort = BeerPongRequest.pirateRetort( insult );

		RequestLogger.updateSessionLog( "Retort: " + ( retort == null ? "unknown!" : retort ) );

		return true;
	}
}
