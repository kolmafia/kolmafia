/**
 * Copyright (c) 2005-2017, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.CandyDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.SweetSynthesisRequest;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.StoreManager;

public class SynthesizeCommand
	extends AbstractCommand
{
	public SynthesizeCommand()
	{
		this.usage = "[?] CANDY1, CANDY2";
	}

	public static final float AGE_LIMIT = ( 60.0f * 60.0f ) / 86400.0f;	// One hour

	private void updatePrices( final AdventureResult candy1, final AdventureResult candy2 )
	{
		StoreManager.getMallPrice( candy1, AGE_LIMIT );
		StoreManager.getMallPrice( candy2, AGE_LIMIT );
	}

	private boolean analyzeCandy( final AdventureResult candy )
	{
		StringBuilder message = new StringBuilder();
		int itemId = candy.getItemId();
		String candyType = CandyDatabase.getCandyType( itemId );

		if ( candyType != CandyDatabase.SIMPLE && candyType != CandyDatabase.COMPLEX )
		{
			message.append( "Item '" );
			message.append( candy.getName() );
			message.append( "' has candy type " );
			message.append( candyType );
			KoLmafia.updateDisplay( message.toString() );
			return false;
		}

		int count = InventoryManager.getAccessibleCount( candy );
		boolean tradeable = ItemDatabase.isTradeable( itemId );
		int cost = !tradeable ? 0 : StoreManager.getMallPrice( candy, AGE_LIMIT );

		message.append( "Item '" );
		message.append( candy.getName() );
		message.append( " is a " );
		if ( !tradeable )
		{
			message.append( "non-tradeable " );
		}
		message.append( candyType );
		message.append( " candy. You have ");
		message.append( String.valueOf( count ) );
		message.append( " of it available to you" );
		if ( !tradeable )
		{
			message.append( "." );
		}
		else
		{
			message.append( " without using the mall, where it costs " );
			message.append( String.valueOf( cost ) );
			message.append( " Meat." );
		}

		KoLmafia.updateDisplay( message.toString() );
		return true;
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		boolean checking = KoLmafiaCLI.isExecutingCheckOnlyCommand;
		KoLmafiaCLI.isExecutingCheckOnlyCommand = false;

		if ( parameters.equals( "" ) )
		{
			return;
		}

		int filter = ItemFinder.CANDY_MATCH;

		AdventureResult[] itemList = ItemFinder.getMatchingItemList( parameters, true, null, filter );

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		int length = itemList.length;

		AdventureResult candy1 = ( length > 0 ) ? itemList[ 0 ] : null;
		AdventureResult candy2 = ( length > 1 ) ? itemList[ 1 ] : ( length == 1 && candy1.getCount() == 2 ) ? candy1 : null;

		if ( candy1 == null || candy2 == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You must specify two candies" );
			return;
		}

		int itemId1 = candy1.getItemId();
		int itemId2 = candy2.getItemId();

		if ( checking )
		{
			updatePrices( candy1, candy2 );

			boolean valid = false;

			valid |= analyzeCandy( candy1 );
			valid |= analyzeCandy( candy2 );

			if ( !valid )
			{
				return;
			}

			int effectId = CandyDatabase.synthesisResult( itemId1, itemId2 );
			if ( effectId != -1 )
			{
				String effectName = EffectDatabase.getEffectName( effectId );
				KoLmafia.updateDisplay( "Synthesizing those two candies will give you 30 turns of " + effectName );
			}

			return;
		}

		SweetSynthesisRequest request = new SweetSynthesisRequest( itemId1, itemId2 );
		RequestThread.postRequest( request );
		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Done!" );
		}
	}
}
