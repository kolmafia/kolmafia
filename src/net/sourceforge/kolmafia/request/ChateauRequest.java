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
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EncounterManager;

public class ChateauRequest
	extends GenericRequest
{
	private static final Pattern PAINTING_PATTERN = Pattern.compile( "Painting of a[n]? (.*?) \\(1\\)\" title" );

	private static final AdventureResult CHATEAU_MUSCLE = ItemPool.get( ItemPool.CHATEAU_MUSCLE, 1 );
	private static final AdventureResult CHATEAU_MYST = ItemPool.get( ItemPool.CHATEAU_MYST, 1 );
	private static final AdventureResult CHATEAU_MOXIE = ItemPool.get( ItemPool.CHATEAU_MOXIE, 1 );
	private static final AdventureResult CHATEAU_FAN = ItemPool.get( ItemPool.CHATEAU_FAN, 1 );
	private static final AdventureResult CHATEAU_CHANDELIER = ItemPool.get( ItemPool.CHATEAU_CHANDELIER, 1 );
	private static final AdventureResult CHATEAU_SKYLIGHT = ItemPool.get( ItemPool.CHATEAU_SKYLIGHT, 1 );
	private static final AdventureResult CHATEAU_BANK = ItemPool.get( ItemPool.CHATEAU_BANK, 1 );
	private static final AdventureResult CHATEAU_JUICE_BAR = ItemPool.get( ItemPool.CHATEAU_JUICE_BAR, 1 );

	public static final AdventureResult CHATEAU_PAINTING = ItemPool.get( ItemPool.CHATEAU_WATERCOLOR, 1 );

	public static String ceiling = null;

	public ChateauRequest()
	{
		super( "place.php" );
		this.addFormField( "whichplace", "chateau" );
	}

	public ChateauRequest( final String action )
	{
		this();
		this.addFormField( "action", action );
	}

	public static void reset()
	{
		KoLConstants.chateau.clear();
		ChateauRequest.ceiling = null;
	}

	public static void refresh()
	{
		ChateauRequest.reset();
		if ( Preferences.getBoolean( "chateauAvailable" ) && StandardRequest.isAllowed( "Items", "Chateau Mantegna room key" ) )
		{
			RequestThread.postRequest( new ChateauRequest() );
		}
	}

	@Override
	public void processResults()
	{
		ChateauRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		ChateauRequest.reset();

		Matcher paintingMatcher = ChateauRequest.PAINTING_PATTERN.matcher( responseText );
		if ( paintingMatcher.find() )
		{
			Preferences.setString( "chateauMonster", paintingMatcher.group(1) );
		}

		// nightstand
		if ( responseText.contains( "nightstand_mus.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MUSCLE );
		}
		else if ( responseText.contains( "nightstand_mag.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MYST );
		}
		else if ( responseText.contains( "nightstand_moxie.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MOXIE );
		}

		// ceiling
		if ( responseText.contains( "ceilingfan.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_FAN );
			ChateauRequest.ceiling = "ceiling fan";
		}
		else if ( responseText.contains( "chandelier.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_CHANDELIER );
			ChateauRequest.ceiling = "antler chandelier";
		}
		else if ( responseText.contains( "skylight.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_SKYLIGHT );
			ChateauRequest.ceiling = "artificial skylight";
		}

		// desk
		if ( responseText.contains( "desk_bank.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_BANK );
		}
		else if ( responseText.contains( "desk_juice.gif" ) )
		{
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_JUICE_BAR );
		}

		String action = GenericRequest.getAction( urlString );

		// Nothing more to do for a simple visit
		if ( action == null )
		{
			return;
		}

		// place.php?whichplace=chateau&action=chateau_restlabelfree
		// or action=cheateau_restlabel
		// or action=chateau_restbox
		if ( action.startsWith( "chateau_rest" ) ||
		     // It will be nice when KoL fixes this misspelling
		     action.startsWith( "cheateau_rest" ) )
		{
			Preferences.increment( "timesRested" );
			KoLCharacter.updateStatus();
		}

		// *** Detect and remember if desk item is been used
	}

	public static final void gainItem( final AdventureResult result )
	{
		switch ( result.getItemId () )
		{
		case ItemPool.CHATEAU_MUSCLE:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MUSCLE );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MYST );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MOXIE );
			break;
		case ItemPool.CHATEAU_MYST:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MYST );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MUSCLE );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MOXIE );
			break;
		case ItemPool.CHATEAU_MOXIE:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_MOXIE );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MUSCLE );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_MOXIE );
			break;
		case ItemPool.CHATEAU_FAN:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_FAN );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_CHANDELIER );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_SKYLIGHT );
			ChateauRequest.ceiling = "ceiling fan";
			break;
		case ItemPool.CHATEAU_CHANDELIER:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_CHANDELIER );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_FAN );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_SKYLIGHT );
			ChateauRequest.ceiling = "antler chandelier";
			break;
		case ItemPool.CHATEAU_SKYLIGHT:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_SKYLIGHT );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_FAN );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_CHANDELIER );
			ChateauRequest.ceiling = "artificial skylight";
			break;
		case ItemPool.CHATEAU_BANK:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_BANK );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_JUICE_BAR );
			break;
		case ItemPool.CHATEAU_JUICE_BAR:
			KoLConstants.chateau.add( ChateauRequest.CHATEAU_JUICE_BAR );
			KoLConstants.chateau.remove( ChateauRequest.CHATEAU_BANK );
			break;
		}
	}

	public static final void parseShopResponse( final String urlString, final String responseText )
	{
		// Adjust for changes in rollover adventures/fights or free rests
		KoLCharacter.recalculateAdjustments();
		KoLCharacter.updateStatus();
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) || !urlString.contains( "whichplace=chateau" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return true;
		}

		String message = null;

		if ( action.equals( "chateau_desk1" ) )
		{
			message = "Collecting Meat from Swiss piggy bank";
		}
		else if ( action.equals( "chateau_desk2" ) )
		{
			message = "Collecting juice from continental juice bar";
		}
		else if ( action.equals( "chateau_desk" ) )
		{
			message = "Collecting swag from the item on your desk";
		}
		else if ( action.startsWith( "chateau_rest" ) ||
			  // It will be nice when KoL fixes this misspelling
			  action.startsWith( "cheateau_rest" ))
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Rest in your bed in the Chateau";
		}

		if ( message == null )
		{
			// Log URL for anything else
			return false;
		}

		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
