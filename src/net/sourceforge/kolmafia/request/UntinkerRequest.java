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

import java.util.ArrayList;
import java.util.regex.Matcher;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UntinkerRequest
	extends GenericRequest
{
	private static final GenericRequest AVAILABLE_CHECKER = new GenericRequest( "town_right.php?place=untinker" );

	private static boolean canUntinker;
	private static int lastUserId = 0;

	private static final AdventureResult SCREWDRIVER = ItemPool.get( ItemPool.RUSTY_SCREWDRIVER, -1 );

	private final int itemId;
	private int iterationsNeeded;
	private AdventureResult item;

	public UntinkerRequest( final int itemId )
	{
		this( itemId, Integer.MAX_VALUE );
	}

	public UntinkerRequest( final int itemId, final int itemCount )
	{
		super( "town_right.php" );

		this.addFormField( "action", "untinker" );
		this.addFormField( "whichitem", String.valueOf( itemId ) );

		this.itemId = itemId;
		this.iterationsNeeded = 1;
		this.item = new AdventureResult( itemId, itemCount );

		if ( itemCount == Integer.MAX_VALUE )
		{
			this.item = this.item.getInstance( this.item.getCount( KoLConstants.inventory ) );
		}

		if ( itemCount > 5 || this.item.getCount( KoLConstants.inventory ) == itemCount )
		{
			this.addFormField( "untinkerall", "on" );
		}
		else
		{
			this.iterationsNeeded = itemCount;
		}
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public void run()
	{
		// Check to see if the item can be constructed using meat
		// paste, and only execute the request if it is known to be
		// creatable through combination.

		if ( ConcoctionDatabase.getMixingMethod( this.itemId ) != KoLConstants.COMBINE )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You cannot untinker that item." );
			return;
		}

		if ( !InventoryManager.retrieveItem( this.item ) )
		{
			return;
		}

		KoLmafia.updateDisplay( "Untinkering " + ItemDatabase.getItemName( this.itemId ) + "..." );

		super.run();

		if ( this.responseText.indexOf( "You acquire" ) == -1 )
		{
			ResultProcessor.processResult( new AdventureResult( this.itemId, 1 ) );

			UntinkerRequest.AVAILABLE_CHECKER.run();

			if ( UntinkerRequest.AVAILABLE_CHECKER.responseText.indexOf( "<select" ) == -1 )
			{
				UntinkerRequest.canUntinker = UntinkerRequest.completeQuest();

				if ( !UntinkerRequest.canUntinker )
				{
					return;
				}

				UntinkerRequest.AVAILABLE_CHECKER.run();
			}

			super.run();
		}

		for ( int i = 1; i < this.iterationsNeeded; ++i )
		{
			super.run();
		}

		KoLmafia.updateDisplay( "Successfully untinkered " + this.item );
	}

	public static final boolean canUntinker()
	{
		if ( UntinkerRequest.lastUserId != KoLCharacter.getUserId() )
		{
			return UntinkerRequest.canUntinker;
		}

		UntinkerRequest.lastUserId = KoLCharacter.getUserId();

		// If the person does not have the accomplishment, visit
		// the untinker to ensure that they get the quest.

		UntinkerRequest.AVAILABLE_CHECKER.run();

		// "I can take apart anything that's put together with meat
		// paste, but you don't have anything like that..."

		UntinkerRequest.canUntinker =
			UntinkerRequest.AVAILABLE_CHECKER.responseText.indexOf( "you don't have anything like that" ) != -1 || UntinkerRequest.AVAILABLE_CHECKER.responseText.indexOf( "<select name=whichitem>" ) != -1;

		return UntinkerRequest.canUntinker;
	}

	public static final boolean completeQuest()
	{
		// If the are in a muscle sign, this is a trivial task;
		// just have them visit Innabox.

		if ( KoLCharacter.inMuscleSign() )
		{
			GenericRequest knollVisit = new GenericRequest( "knoll.php" );
			knollVisit.run();

			knollVisit.addFormField( "place=smith" );
			knollVisit.run();

			return true;
		}

		if ( !StaticEntity.isHeadless() )
		{
			if ( !InputFieldUtilities.confirm( "KoLmafia thinks you haven't completed the screwdriver quest.  Would you like to have KoLmafia automatically complete it now?" ) )
			{
				return false;
			}
		}

		// Okay, so they don't have one yet. Complete the
		// untinkerer's quest automatically.

		ArrayList temporary = new ArrayList();
		temporary.addAll( KoLConstants.conditions );

		KoLConstants.conditions.clear();
		KoLConstants.conditions.add( UntinkerRequest.SCREWDRIVER.getNegation() );

		// Make sure that paco has been visited, or else
		// the knoll won't be available.

		String action = Preferences.getString( "battleAction" );
		if ( action.indexOf( "dictionary" ) != -1 )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "battleAction=attack" );
		}

		StaticEntity.getClient().makeRequest(
			AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=18" ), KoLCharacter.getAdventuresLeft() );
		KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "battleAction=" + action );

		if ( !KoLConstants.conditions.isEmpty() )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Unable to complete untinkerer's quest." );
		}

		KoLConstants.conditions.clear();
		KoLConstants.conditions.addAll( temporary );

		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		// You should now have a screwdriver in your inventory.
		// Go ahead and rerun the untinker request and you will
		// have the needed accomplishment.

		UntinkerRequest.AVAILABLE_CHECKER.run();
		return UntinkerRequest.AVAILABLE_CHECKER.responseText.indexOf( "Degrassi Knoll" ) == -1;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "town_right.php" ) || urlString.indexOf( "action=untinker" ) == -1 )
		{
			return false;
		}

		// Visiting the untinker removes screwdriver from inventory.

		if ( KoLConstants.inventory.contains( UntinkerRequest.SCREWDRIVER ) )
		{
			UntinkerRequest.canUntinker = true;
			ResultProcessor.processResult( UntinkerRequest.SCREWDRIVER );
		}

		Matcher itemMatcher = TransferItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return true;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		AdventureResult result = new AdventureResult( itemId, -1 );

		RequestLogger.updateSessionLog();

		if ( urlString.indexOf( "untinkerall=on" ) != -1 )
		{
			result = result.getInstance( 0 - result.getCount( KoLConstants.inventory ) );
			RequestLogger.updateSessionLog( "untinker * " + result.getName() );
		}
		else
		{
			RequestLogger.updateSessionLog( "untinker 1 " + result.getName() );
		}

		ResultProcessor.processResult( result );
		return true;
	}
}
