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
import java.util.TreeSet;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.ZapRequest;

import net.sourceforge.kolmafia.session.StoreManager;

public class ComparisonShopCommand
	extends AbstractCommand
	implements Comparator
{
	public ComparisonShopCommand()
	{
		this.usage = "[?] [+]<item> [,[-]item]... [; <cmds>] - compare prices, do cmds with \"it\" replaced with best.";
		this.flags = KoLmafiaCLI.FULL_LINE_CMD;
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		boolean expensive = cmd.equals( "expensive" );
		String commands = null;
		int pos = parameters.indexOf( ";" );
		if ( pos != -1 )
		{
			commands = parameters.substring( pos + 1 ).trim();
			parameters = parameters.substring( 0, pos ).trim();
		}
		String[] pieces = parameters.split( "\\s*,\\s*" );
		TreeSet<String> names = new TreeSet<String>();
		for ( int i = 0; i < pieces.length; ++i )
		{
			String piece = pieces[ i ];
			if ( piece.startsWith( "+" ) )
			{
				AdventureResult item = ItemFinder.getFirstMatchingItem( piece.substring( 1 ).trim() );
				if ( item == null )
				{
					return;
				}
				names.addAll( Arrays.asList( ZapRequest.getZapGroup( item.getItemId() ) ) );
			}
			else if ( piece.startsWith( "-" ) )
			{
				names.removeAll( ItemDatabase.getMatchingNames( piece.substring( 1 ).trim() ) );
			}
			else
			{
				names.addAll( ItemDatabase.getMatchingNames( piece ) );
			}
		}
		if ( names.size() == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "No matching items!" );
			return;
		}
		if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
		{
			RequestLogger.printList( Arrays.asList( names.toArray() ) );
			return;
		}
		ArrayList<AdventureResult> results = new ArrayList<AdventureResult>();
		Iterator i = names.iterator();
		while ( i.hasNext() )
		{
			AdventureResult item = new AdventureResult( (String) i.next() );
			if ( !ItemDatabase.isTradeable( item.getItemId() ) || StoreManager.getMallPrice( item ) <= 0 )
			{
				continue;
			}
			if ( !KoLmafia.permitsContinue() )
			{
				return;
			}
			results.add( item );
		}
		if ( results.size() == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "No tradeable items!" );
			return;
		}
		Collections.sort( results, this );
		if ( expensive )
		{
			Collections.reverse( results );
		}
		if ( commands != null )
		{
			this.CLI.executeLine( commands.replaceAll( "\\bit\\b", ( (AdventureResult) results.get( 0 ) ).getName() ) );
			return;
		}
		i = results.iterator();
		while ( i.hasNext() )
		{
			AdventureResult item = (AdventureResult) i.next();
			RequestLogger.printLine( item.getName() + " @ " + KoLConstants.COMMA_FORMAT.format( StoreManager.getMallPrice( item ) ) );
		}
	}

	public int compare( final Object o1, final Object o2 )
	{
		return StoreManager.getMallPrice( (AdventureResult) o1 ) - StoreManager.getMallPrice( (AdventureResult) o2 );
	}
}
