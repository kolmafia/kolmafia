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

package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.KoLDatabase.BooleanArray;

public class ZapRequest extends KoLRequest
{
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<option value=(\\d+) descid='.*?'>.*?</option>" );

	private static final BooleanArray isZappable = new BooleanArray();
	private static final SortedListModel zappableItems = new SortedListModel();

	private AdventureResult item;

	public ZapRequest( AdventureResult item )
	{
		super( "wand.php" );

		this.item = null;

		if ( KoLCharacter.getZapper() == null )
			return;

		this.item = item;

		this.addFormField( "action", "zap" );
		this.addFormField( "whichwand", String.valueOf( KoLCharacter.getZapper().getItemId() ) );
		this.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
	}

	private static final void initializeList()
	{
		if ( !zappableItems.isEmpty() )
			return;

		try
		{
			String line;
			BufferedReader reader = KoLDatabase.getVersionedReader( "zapgroups.txt", ZAPGROUPS_VERSION );

			while ( (line = KoLDatabase.readLine( reader )) != null )
			{
				String [] list = line.split( "\\s*,\\s*" );
				for ( int i = 0; i < list.length; ++i )
				{
					int itemId = TradeableItemDatabase.getItemId( list[i] );
					zappableItems.add( new AdventureResult( itemId, 1 ) );
					isZappable.set( itemId, true );
				}
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}

	public static final SortedListModel getZappableItems()
	{
		initializeList();

		SortedListModel matchingItems = new SortedListModel();
		matchingItems.addAll( inventory );
		matchingItems.retainAll( zappableItems );
		return matchingItems;
	}

	public void run()
	{
		if ( this.item == null )
			return;

		if ( KoLCharacter.getZapper() == null )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't have a wand." );
			return;
		}

		if ( KoLCharacter.hasItem( this.item, true ) )
			AdventureDatabase.retrieveItem( this.item );

		if ( !inventory.contains( this.item ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't have a " + this.item.getName() + "." );
			return;
		}

		KoLmafia.updateDisplay( "Zapping " + this.item.getName() + "..." );
		super.run();
	}

	public void processResults()
	{
		// "The Crown of the Goblin King shudders for a moment, but
		// nothing happens."

		if ( this.responseText.indexOf( "nothing happens" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "The " + this.item.getName() + " is not zappable." );
			return;
		}

		// If it blew up, remove wand
		if ( this.responseText.indexOf( "abruptly explodes" ) != -1 )
			 StaticEntity.getClient().processResult( KoLCharacter.getZapper().getNegation() );

		// Remove old item and notify the user of success.
		StaticEntity.getClient().processResult( this.item.getInstance( -1 ) );
		KoLmafia.updateDisplay( this.item.getName() + " has been transformed." );
	}

	public static final void decorate( StringBuffer buffer )
	{
		initializeList();

		int selectIndex = buffer.indexOf( ">", buffer.indexOf( "<select" ) ) + 1;
		int endSelectIndex = buffer.indexOf( "</select>" );

		Matcher optionMatcher = OPTION_PATTERN.matcher( buffer.substring( selectIndex, endSelectIndex ) );
		buffer.delete( selectIndex, endSelectIndex );

		int itemId;
		StringBuffer zappableOptions = new StringBuffer();
		while ( optionMatcher.find() )
		{
			itemId = Integer.parseInt( optionMatcher.group(1) );
			if ( itemId == 0 || isZappable.get( itemId ) )
				zappableOptions.append( optionMatcher.group() );
		}

		buffer.insert( selectIndex, zappableOptions.toString() );
	}
}

