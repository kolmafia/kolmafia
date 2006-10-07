/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.LockableListModel;

public class MuseumManager extends StaticEntity
{
	private static final Pattern SELECTED_PATTERN = Pattern.compile( "(\\d+) selected>" );
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<td>([^<]*?)&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>.*?<select name=whichshelf(\\d+)>(.*?)</select>" );
	private static final Pattern SELECT_PATTERN = Pattern.compile( "<select.*?</select>" );
	private static final Pattern SHELF_PATTERN = Pattern.compile( "<option value=(\\d+).*?>(.*?)</option>" );

	private static LockableListModel headers = new LockableListModel();
	private static LockableListModel shelves = new LockableListModel();

	public static void reset()
	{
		collection.clear();
		headers.clear();
		shelves.clear();
	}

	public static LockableListModel getHeaders()
	{	return headers;
	}

	public static String getHeader( int shelf )
	{	return (String) headers.get( shelf );
	}

	public static LockableListModel getShelves()
	{	return shelves;
	}

	public static void move( Object [] moving, int sourceShelf, int destinationShelf )
	{
		// In order to take advantage of the utilities of the
		// Collections interface, place everything inside of
		// a list first.

		List movingList = new ArrayList();
		for ( int i = 0; i < moving.length; ++i )
			movingList.add( moving[i] );

		// Use the removeAll() and addAll() methods inside of
		// the Collections interface.

		((SortedListModel)shelves.get( sourceShelf )).removeAll( movingList );
		((SortedListModel)shelves.get( destinationShelf )).addAll( movingList );

		// Save the lists to the server and update the display
		// on theto reflect the change.

		save( MuseumManager.shelves );
		KoLmafia.updateDisplay( "Display case updated." );
	}

	public static void reorder( String [] headers )
	{
		headers[0] = "Display Case";

		// Unfortunately, if there are deleted shelves, the
		// shelves cannot be re-ordered directly.  What has
		// to happen is that the number of deleted shelves
		// needs to be created with some dummy name and then
		// deleted afterwards.

		boolean containsDeletedShelf = false;
		boolean [] deleted = new boolean[ headers.length ];

		for ( int i = 0; i < headers.length; ++i )
		{
			deleted[i] = headers[i].equals( "(Deleted Shelf)" );
			containsDeletedShelf |= deleted[i];
		}

		for ( int i = 0; i < deleted.length; ++i )
		{
			if ( deleted[i] )
			{
				REDIRECT_FOLLOWER.constructURLString( "managecollection.php" );
				REDIRECT_FOLLOWER.addFormField( "action", "newshelf" );
				REDIRECT_FOLLOWER.addFormField( "pwd" );
				REDIRECT_FOLLOWER.addFormField( "shelfname", "Deleted Shelf " + i );
				REDIRECT_FOLLOWER.run();
			}
		}

		// Determine where the headers are in the existing
		// list of headers to find out where the shelf contents
		// should be stored after the update.

		List shelforder = new ArrayList();
		for ( int i = 0; i < headers.length; ++i )
			shelforder.add( shelves.get( MuseumManager.headers.indexOf( headers[i] ) ) );

		// Save the lists to the server and update the display
		// on theto reflect the change.

		save( shelforder );

		// Redelete the previously deleted shelves so that the
		// user isn't stuck with shelves they aren't going to use.

		REDIRECT_FOLLOWER.constructURLString( "managecollection.php" );
		REDIRECT_FOLLOWER.addFormField( "action", "modifyshelves" );
		REDIRECT_FOLLOWER.addFormField( "pwd" );

		for ( int i = 1; i < headers.length; ++i )
		{
			REDIRECT_FOLLOWER.addFormField( "newname" + i, RequestEditorKit.getEntities( headers[i] ) );
			if ( deleted[i] )
				REDIRECT_FOLLOWER.addFormField( "delete" + i, "on" );
		}

		REDIRECT_FOLLOWER.run();
		(new MuseumRequest()).run();
		KoLmafia.updateDisplay( "Display case updated." );
	}

	private static void save( List shelfOrder )
	{
		int elementCounter = 0;
		SortedListModel currentShelf;

		// In order to ensure that all data is saved with no
		// glitches server side, all items submit their state.
		// Store the data in two parallel arrays.

		int size = collection.size();
		int [] newShelves = new int[ size ];
		AdventureResult [] newItems = new AdventureResult[ size ];

		// Iterate through each shelf and place the item into
		// the parallel arrays.

		for ( int i = 0; i < shelfOrder.size(); ++i )
		{
			currentShelf = (SortedListModel) shelfOrder.get(i);
			for ( int j = 0; j < currentShelf.size(); ++j, ++elementCounter )
			{
				newShelves[ elementCounter ] = i;
				newItems[ elementCounter ] = (AdventureResult) currentShelf.get(j);
			}
		}

		// Once the parallel arrays are properly initialized,
		// send the update request to the server.

		(new MuseumRequest( newItems, newShelves )).run();
	}

	public static void update( String data )
	{
		updateShelves( data );
		Matcher selectedMatcher;

		int itemID, itemCount;
		String [] itemString;

		Matcher optionMatcher = OPTION_PATTERN.matcher( data );
		while ( optionMatcher.find() )
		{
			selectedMatcher = SELECTED_PATTERN.matcher( optionMatcher.group(3) );

			itemID = parseInt( optionMatcher.group(2) );

			itemString = optionMatcher.group(1).split( "[\\(\\)]" );
			if ( TradeableItemDatabase.getItemName( itemID ) == null )
				TradeableItemDatabase.registerItem( itemID, itemString[0].trim() );

			itemCount = itemString.length == 1 ? 1 : parseInt( itemString[1] );

			registerItem( new AdventureResult( itemID, itemCount ),
				selectedMatcher.find() ? parseInt( selectedMatcher.group(1) ) : 0 );
		}
	}

	private static void registerItem( AdventureResult item, int shelf )
	{
		collection.add( item );
		((SortedListModel)shelves.get( shelf )).add( item );
	}

	private static void updateShelves( String data )
	{
		reset();
		Matcher selectMatcher = SELECT_PATTERN.matcher( data );
		if ( selectMatcher.find() )
		{
			int currentShelf;
			Matcher shelfMatcher = SHELF_PATTERN.matcher( selectMatcher.group() );
			while ( shelfMatcher.find() )
			{
				currentShelf = parseInt( shelfMatcher.group(1) );

				for ( int i = headers.size(); i < currentShelf; ++i )
					headers.add( "(Deleted Shelf)" );

				headers.add( RequestEditorKit.getUnicode( shelfMatcher.group(2) ) );
			}
		}

		if ( headers.size() == 0 )
			headers.add( "" );
		headers.set( 0, "Display Case" );

		for ( int i = 0; i < headers.size(); ++i )
			shelves.add( new SortedListModel() );
	}
}
