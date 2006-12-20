/**
 * Copyright (c) 2005-2006, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class UntinkerRequest extends KoLRequest
{
	private static final AdventureResult SCREWDRIVER = new AdventureResult( 454, -1 );

	private int itemId;
	private int iterationsNeeded;

	public UntinkerRequest( int itemId )
	{	this( itemId, Integer.MAX_VALUE );
	}

	public UntinkerRequest( int itemId, int itemCount )
	{
		super( "town_right.php" );

		addFormField( "pwd" );
		addFormField( "action", "untinker" );
		addFormField( "whichitem", String.valueOf( itemId ) );

		this.itemId = itemId;
		this.iterationsNeeded = 1;

		AdventureResult item = new AdventureResult( itemId, itemCount );

		if ( itemCount > 5 || item.getCount( inventory ) == itemCount )
			addFormField( "untinkerall", "on" );
		else
			iterationsNeeded = itemCount;
	}

	public void run()
	{
		// Check to see if the item can be constructed using meat
		// paste, and only execute the request if it is known to be
		// creatable through combination.

		if ( ConcoctionsDatabase.getMixingMethod( itemId ) != ItemCreationRequest.COMBINE )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You cannot untinker that item." );
			return;
		}

		// Check to see if the person has the untinkering accomplishment
		// before starting.

		if ( KoLCharacter.getLevel() < 4 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You cannot untinker items yet." );
			return;
		}

		// Visiting the untinker automatically deducts a
		// screwdriver from the inventory.

		if ( inventory.contains( SCREWDRIVER ) )
			StaticEntity.getClient().processResult( SCREWDRIVER );

		KoLmafia.updateDisplay( "Untinkering " + TradeableItemDatabase.getItemName( itemId ) + "..." );
		super.run();

		if ( responseText.indexOf( "You acquire" ) == -1 )
		{
			KoLRequest questCompleter = new KoLRequest( "town_right.php?place=untinker" );
			questCompleter.run();

			if ( responseText.indexOf( "Degrassi" ) != -1 )
			{
				if ( !completeQuest() )
					return;

				questCompleter.run();
			}

			super.run();
		}

		for ( int i = 1; i < iterationsNeeded; ++i )
			super.run();

		KoLmafia.updateDisplay( "Successfully untinkered " + TradeableItemDatabase.getItemName( itemId ) + "." );
	}

	public static final boolean canUntinker()
	{
		// If the person does not have the accomplishment, visit
		// the untinker to ensure that they get the quest.

		KoLRequest questCompleter = new KoLRequest( "town_right.php?place=untinker" );
		questCompleter.run();

		// "I can take apart anything that's put together with meat
		// paste, but you don't have anything like that..."

		if ( questCompleter.responseText.indexOf( "you don't have anything like that" ) != -1 )
		{
			// They've completed the quest but have nothing the
			// Untinker is willing to work on.

			return true;
		}

		return questCompleter.responseText.indexOf( "<select name=whichitem>" ) != -1;
	}

	public static final boolean completeQuest()
	{
		// If the are in a muscle sign, this is a trivial task;
		// just have them visit Innabox.

		if ( KoLCharacter.inMuscleSign() )
		{
			KoLRequest questCompleter = new KoLRequest( "knoll.php" );
			questCompleter.run();

			questCompleter.constructURLString( "knoll.php?place=smith" );
			questCompleter.run();

			return true;
		}

		if ( !existingFrames.isEmpty() && !StaticEntity.getBooleanProperty( "autoRetrieveScrewdriver" ) )
		{
			if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null, "KoLmafia thinks you haven't completed the screwdriver quest.\nWould you like to have KoLmafia automatically complete it now?",
				"Think carefully before you answer...", JOptionPane.YES_NO_OPTION ) )
					return false;
		}

		// Okay, so they don't have one yet. Complete the
		// untinkerer's quest automatically.

		ArrayList temporary = new ArrayList();
		temporary.addAll( conditions );

		conditions.clear();
		conditions.add( SCREWDRIVER.getNegation() );

		// Make sure that paco has been visited, or else
		// the knoll won't be available.

		DEFAULT_SHELL.executeLine( "adventure * degrassi" );
		if ( !conditions.isEmpty() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Unable to complete untinkerer's quest." );
			conditions.clear();

			conditions.addAll( temporary );
			return false;
		}

		conditions.clear();
		conditions.addAll( temporary );

		// You should now have a screwdriver in your inventory.
		// Go ahead and rerun the untinker request and you will
		// have the needed accomplishment.

		KoLRequest questCompleter = new KoLRequest( "town_right.php?place=untinker" );
		questCompleter.run();

		return questCompleter.responseText.indexOf( "Degrassi Knoll" ) == -1;
	}

	public void processResults()
	{
		if ( responseText.indexOf( "You acquire" ) != -1 )
			StaticEntity.getClient().processResult( new AdventureResult( itemId, -1 ) );
	}
}
