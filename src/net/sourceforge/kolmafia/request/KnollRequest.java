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

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class KnollRequest
	extends GenericRequest
{
	public KnollRequest()
	{
		super( "place.php" );
		this.addFormField( "whichplace", "knoll_friendly" );
	}

	public KnollRequest( final String action)
	{
		this();
		this.addFormField( "action", action );
	}

	public static String getNPCName( final String action )
	{
		if ( action == null )
		{
			return null;
		}

		if ( action.equals( "dk_mayor" ) )
		{
			return "Mayor Zapruder";
		}

		if ( action.equals( "dk_innabox" ) )
		{
			return "Innabox";
		}

		if ( action.equals( "dk_plunger" ) )
		{
			return "The Plunger";
		}

		return null;
	}

	@Override
	public void processResults()
	{
		KnollRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "place.php" ) || !urlString.contains( "knoll_friendly" ) )
		{
			return;
		}

		String action = GenericRequest.getAction( urlString );

		if ( action == null )
		{
			return;
		}

		if ( action.equals( "dk_mayor" ) )
		{
			// Mayor Zapruder assigns quests and gives you an
			// elemental fairy or equipment.

			if ( responseText.indexOf( "flaming glowsticks" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.FLAMING_MUSHROOM, -1 );
			}
			else if ( responseText.indexOf( "iced-out bling" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.FROZEN_MUSHROOM, -1 );
			}
			else if ( responseText.indexOf( "limburger biker boots" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.STINKY_MUSHROOM, -1 );
			}
			
			// Quest handling from here down to the return;
			
			// Ah, you must be our newest Citizen! It is fortunate that you have arrived, for dire times are
			// upon us, and we require assistance.

			// As you may know, we train bugbears as pets and guards. Lately, though, something is causing
			// our bugbears to become vicious, and to attack their handlers. I would be grateful if you
			// would investigate this for me. We keep our bugbears in a pen near the Spooky Forest.
			if ( responseText.indexOf( "It is fortunate that you have arrived" ) != -1 )
			{
				QuestDatabase.setQuestIfBetter( Quest.BUGBEAR, QuestDatabase.STARTED );
			}
			// Mayor Zapruder looks at the tiny pitchfork you've brought him. 

			// "Spooky Gravy Fairies! I should've known. 
			else if ( responseText.indexOf( "Mayor Zapruder looks at the tiny pitchfork" ) != -1 )
			{
				QuestDatabase.setQuestIfBetter( Quest.BUGBEAR, "step1" );
			}
			// "Excellent, Adventurer. Please, hand me the mushroom..."
			else if ( responseText.indexOf( "Please, hand me the mushroom" ) != -1 )
			{
				QuestDatabase.setQuestIfBetter( Quest.BUGBEAR, "step2" );
			}
			// "You've done it! The bugbears have finally returned to a state of normalcy.
			// Without their Queen to lead them, the spooky gravy fairies won't cause us any more problems.
			// And now, for your reward."
			else if ( responseText.indexOf( "The bugbears have finally returned to a state of normalcy" ) != -1 )
			{
				QuestDatabase.setQuestIfBetter( Quest.BUGBEAR, QuestDatabase.FINISHED );
			}
			
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) || !urlString.contains( "knoll_friendly" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );

		// We have nothing special to do for other simple visits.
		if ( action == null )
		{
			return true;
		}

		String npc = getNPCName( action );
		if ( npc != null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Visiting " + npc );
			return true;
		}

		// Other requests handle other actions in the Knoll
		// action = gym

		return false;
	}
}
