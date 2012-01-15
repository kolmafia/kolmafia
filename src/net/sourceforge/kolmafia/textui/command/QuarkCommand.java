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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;

public class QuarkCommand
	extends AbstractCommand
	implements Comparator
{
	public QuarkCommand()
	{
		this.usage =
			"[?] [<itemList>...] - gain MP by pasting unstable quark with best item from itemList (or your junk list).";
	}

	public void run( final String cmd, final String parameters )
	{
		if ( ItemPool.get( ItemPool.UNSTABLE_QUARK, 1 ).getCount( KoLConstants.inventory ) < 1 )
		{
			KoLmafia.updateDisplay( "You have no unstable quarks." );
			return;
		}
		if ( !KoLCharacter.knollAvailable() )
		{
			AdventureResult paste = ItemPool.get( ItemPool.MEAT_PASTE, 1 );

			if ( !InventoryManager.retrieveItem( paste ) )
			{
				KoLmafia.updateDisplay( "Can't afford gluons." );
				return;
			}
		}

		List items = KoLConstants.junkList;
		if ( !parameters.equals( "" ) )
		{
			items = Arrays.asList( ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters ) );
			if ( items.size() == 0 )
			{
				return;
			}
		}

		ArrayList usables = new ArrayList();
		Iterator i = items.iterator();
		while ( i.hasNext() )
		{
			AdventureResult item = (AdventureResult) i.next();
			if ( item.getCount( KoLConstants.inventory ) < ( KoLConstants.singletonList.contains( item ) ? 2 : 1 ) )
			{
				continue;
			}
			int price = ItemDatabase.getPriceById( item.getItemId() );
			if ( price < 20 || KoLCharacter.getCurrentMP() + price > KoLCharacter.getMaximumMP() )
			{
				continue;
			}
			if ( this.isPasteable( item ) )
			{
				usables.add( item.getInstance( price ) );
			}
		}
		if ( usables.size() == 0 )
		{
			KoLmafia.updateDisplay( "No suitable quark-pasteable items found." );
			return;
		}

		Collections.sort( usables, this );
		if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
		{
			RequestLogger.printLine( usables.get( 0 ).toString() );
			return;
		}

		AdventureResult item = (AdventureResult) usables.get( 0 );
		RequestLogger.printLine( "Pasting unstable quark with " + item );
		GenericRequest visitor =
			new GenericRequest( "craft.php?action=craft&mode=combine&ajax=1&pwd&qty=1&a=3743&b=" + item.getItemId() );
		RequestThread.postRequest( visitor );
		ResponseTextParser.externalUpdate( visitor.getURLString(), visitor.responseText );
	}

	private boolean isPasteable( final AdventureResult item )
	{
		Iterator i = ConcoctionDatabase.getKnownUses( item ).iterator();
		while ( i.hasNext() )
		{
			AdventureResult use = (AdventureResult) i.next();
			if ( (ConcoctionDatabase.getMixingMethod( use.getItemId() ) & KoLConstants.CT_MASK) == KoLConstants.COMBINE )
			{
				return true;
			}
		}
		return false;
	}

	public int compare( final Object o1, final Object o2 )
	{
		return ( (AdventureResult) o2 ).getCount() - ( (AdventureResult) o1 ).getCount();
	}
}
