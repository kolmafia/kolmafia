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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

public class SewerRequest
	extends GenericRequest
{
	public static final int TEN_LEAF_CLOVER = 24;
	public static final int DISASSEMBLED_CLOVER = 196;
	public static final AdventureResult CLOVER = new AdventureResult( SewerRequest.TEN_LEAF_CLOVER, -1 );
	public static final AdventureResult GUM = new AdventureResult( "chewing gum on a string", -1 );

	private final boolean isLuckySewer;

	public SewerRequest( final boolean isLuckySewer )
	{
		super( "sewer.php" );
		this.isLuckySewer = isLuckySewer;

		if ( isLuckySewer )
		{
			this.addFormField( "doodit", "1" );
		}
	}

	/**
	 * Runs the <code>SewerRequest</code>. This method determines whether or not the lucky sewer adventure will be
	 * used and attempts to run the appropriate adventure. Note that the display will be updated in the event of
	 * failure.
	 */

	public void run()
	{
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		if ( this.isLuckySewer )
		{
			if ( Preferences.getBoolean( "cloverProtectActive" ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Turn off clover protection if you want to go here." );
			}
			else
			{
				this.runLuckySewer();
			}
		}
		else
		{
			this.runUnluckySewer();
		}

		if ( KoLmafia.permitsContinue() )
		{
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.ADV, -1 ) );
		}
	}

	/**
	 * Utility method which runs the lucky sewer adventure.
	 */

	private void runLuckySewer()
	{
		if ( !InventoryManager.retrieveItem( SewerRequest.CLOVER.getInstance( 1 ) ) )
		{
			return;
		}

		// The Sewage Gnomes insist on giving precisely three
		// items, so if you have fewer than three items, report
		// an error.

		String thirdItemString = Preferences.getString( "luckySewerAdventure" );
		int thirdItem =
			thirdItemString.indexOf( "random" ) != -1 ? KoLConstants.RNG.nextInt( 11 ) + 1 : Character.isDigit( thirdItemString.charAt( 0 ) ) ? StaticEntity.parseInt( thirdItemString ) : ItemDatabase.getItemId( thirdItemString );

		if ( thirdItem < 1 || thirdItem > 12 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You must select a third item from the gnomes." );
			return;
		}

		// Rather than giving people flexibility, it seems like
		// a better idea to assume everyone wants trinkets and
		// spices and let them specify the third item.

		this.addFormField( "i43", "on" );
		this.addFormField( "i8", "on" );
		this.addFormField( "i" + thirdItem, "on" );

		// Enter the sewer

		super.run();
	}

	/**
	 * Utility method which runs the normal sewer adventure.
	 */

	private void runUnluckySewer()
	{
		if ( StaticEntity.getClient().isLuckyCharacter() )
		{
			( new UseItemRequest(
				SewerRequest.CLOVER.getInstance( SewerRequest.CLOVER.getCount( KoLConstants.inventory ) ) ) ).run();
		}

		super.run();

		// You may have randomly received a clover from some other
		// source - detect this occurence and notify the user

		if ( this.responseText.indexOf( "Sewage Gnomes" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You have an unaccounted for ten-leaf clover." );
			return;
		}
	}

	public void processResults()
	{
		if ( this.responseText.indexOf( "You acquire" ) != -1 )
		{
			if ( StaticEntity.getClient().isLuckyCharacter() )
			{
				StaticEntity.getClient().processResult( SewerRequest.CLOVER );
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
		return this.responseCode == 200 && this.responseText.indexOf( "You acquire" ) != -1 ? 1 : 0;
	}
}
