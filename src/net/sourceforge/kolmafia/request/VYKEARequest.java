/**
 * Copyright (c) 2005-2015, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class VYKEARequest
	extends CreateItemRequest
{
	public static final String [] VYKEA =
	{
		"level 1 bookshelf",
		"level 1 frenzy bookshelf",
		"level 1 blood bookshelf",
		"level 1 lightning bookshelf",
		"level 2 bookshelf",
		"level 2 frenzy bookshelf",
		"level 2 blood bookshelf",
		"level 2 lightning bookshelf",
		"level 3 bookshelf",
		"level 3 frenzy bookshelf",
		"level 3 blood bookshelf",
		"level 3 lightning bookshelf",
		"level 4 bookshelf",
		"level 4 frenzy bookshelf",
		"level 4 blood bookshelf",
		"level 4 lightning bookshelf",
		"level 5 bookshelf",
		"level 5 frenzy bookshelf",
		"level 5 blood bookshelf",
		"level 5 lightning bookshelf",

		"level 1 ceiling fan",
		"level 1 frenzy ceiling fan",
		"level 1 blood ceiling fan",
		"level 1 lightning ceiling fan",
		"level 2 ceiling fan",
		"level 2 frenzy ceiling fan",
		"level 2 blood ceiling fan",
		"level 2 lightning ceiling fan",
		"level 3 ceiling fan",
		"level 3 frenzy ceiling fan",
		"level 3 blood ceiling fan",
		"level 3 lightning ceiling fan",
		"level 4 ceiling fan",
		"level 4 frenzy ceiling fan",
		"level 4 blood ceiling fan",
		"level 4 lightning ceiling fan",
		"level 5 ceiling fan",
		"level 5 frenzy ceiling fan",
		"level 5 blood ceiling fan",
		"level 5 lightning ceiling fan",

		"level 1 couch",
		"level 1 frenzy couch",
		"level 1 blood couch",
		"level 1 lightning couch",
		"level 2 couch",
		"level 2 frenzy couch",
		"level 2 blood couch",
		"level 2 lightning couch",
		"level 3 couch",
		"level 3 frenzy couch",
		"level 3 blood couch",
		"level 3 lightning couch",
		"level 4 couch",
		"level 4 frenzy couch",
		"level 4 blood couch",
		"level 4 lightning couch",
		"level 5 couch",
		"level 5 frenzy couch",
		"level 5 blood couch",
		"level 5 lightning couch",

		"level 1 dishrack",
		"level 1 frenzy dishrack",
		"level 1 blood dishrack",
		"level 1 lightning dishrack",
		"level 2 dishrack",
		"level 2 frenzy dishrack",
		"level 2 blood dishrack",
		"level 2 lightning dishrack",
		"level 3 dishrack",
		"level 3 frenzy dishrack",
		"level 3 blood dishrack",
		"level 3 lightning dishrack",
		"level 4 dishrack",
		"level 4 frenzy dishrack",
		"level 4 blood dishrack",
		"level 4 lightning dishrack",
		"level 5 dishrack",
		"level 5 frenzy dishrack",
		"level 5 blood dishrack",
		"level 5 lightning dishrack",

		"level 1 dresser",
		"level 1 frenzy dresser",
		"level 1 blood dresser",
		"level 1 lightning dresser",
		"level 2 dresser",
		"level 2 frenzy dresser",
		"level 2 blood dresser",
		"level 2 lightning dresser",
		"level 3 dresser",
		"level 3 frenzy dresser",
		"level 3 blood dresser",
		"level 3 lightning dresser",
		"level 4 dresser",
		"level 4 frenzy dresser",
		"level 4 blood dresser",
		"level 4 lightning dresser",
		"level 5 dresser",
		"level 5 frenzy dresser",
		"level 5 blood dresser",
		"level 5 lightning dresser",

		"level 1 lamp",
		"level 1 frenzy lamp",
		"level 1 blood lamp",
		"level 1 lightning lamp",
		"level 2 lamp",
		"level 2 frenzy lamp",
		"level 2 blood lamp",
		"level 2 lightning lamp",
		"level 3 lamp",
		"level 3 frenzy lamp",
		"level 3 blood lamp",
		"level 3 lightning lamp",
		"level 4 lamp",
		"level 4 frenzy lamp",
		"level 4 blood lamp",
		"level 4 lightning lamp",
		"level 5 lamp",
		"level 5 frenzy lamp",
		"level 5 blood lamp",
		"level 5 lightning lamp",
	};

	public VYKEARequest( Concoction conc )
	{
		super( "inv_use.php", conc );
		this.addFormField( "whichitem", String.valueOf( ItemPool.VYKEA_INSTRUCTIONS ) );
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		// Will redirect to choice.php if successful
		return true;
	}

	@Override
	public boolean noCreation()
	{
		return true;
	}

	@Override
	public void run()
	{
		// Make sure you don't already have a companion
		if ( Preferences.getInteger( "_VYKEACompanionLevel" ) > 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You already have a VYKEA companion. It would get jealous and turn on you if you build another one today." );
			return;
		}

		// Make sure VYKEA instructions are available.
		if ( !KoLConstants.inventory.contains( ItemPool.get( ItemPool.VYKEA_INSTRUCTIONS, 1 ) ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need a set of VYKEA instructions in order to build a VYKEA companion." );
			return;
		}

		// Make sure a VYKEA hex key is in inventory
		if ( !KoLConstants.inventory.contains( ItemPool.get( ItemPool.VYKEA_HEX_KEY, 1 ) ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need a VYKEA hex key in order to build a VYKEA companion." );
			return;
		}

		// Get the necessary ingredients
		if ( !this.makeIngredients() )
		{
			return;
		}

		// Make a companion!
		// super.run();

		KoLmafia.updateDisplay( MafiaState.ERROR, "VYKEA companion creation not yet implemented." );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "inv_use.php" ) || !urlString.contains( "whichitem=8730" ) )
		{
			return false;
		}

		return false;
	}
}
