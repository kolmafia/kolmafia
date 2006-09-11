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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemStorageRequest extends SendMessageRequest
{
	private int moveType;

	public static final int EMPTY_STORAGE = -2;
	public static final int CLOSET_YOUR_CLOVERS = -1;
	public static final int RETRIEVE_STORAGE = 0;
	public static final int INVENTORY_TO_CLOSET = 1;
	public static final int CLOSET_TO_INVENTORY = 2;
	public static final int MEAT_TO_CLOSET = 4;
	public static final int MEAT_TO_INVENTORY = 5;
	public static final int STORAGE_TO_INVENTORY = 6;
	public static final int PULL_MEAT_FROM_STORAGE = 7;

	public ItemStorageRequest( KoLmafia client )
	{
		super( client, "storage.php" );
		this.moveType = RETRIEVE_STORAGE;
	}

	/**
	 * Constructs a new <code>ItemStorageRequest</code>.
	 * @param	client	The client to be notified of the results
	 * @param	amount	The amount of meat involved in this transaction
	 * @param	moveType	Whether or not this is a deposit or withdrawal, or if it's to the clan stash
	 */

	public ItemStorageRequest( KoLmafia client, int amount, int moveType )
	{
		super( client, moveType == PULL_MEAT_FROM_STORAGE ? "storage.php" : "closet.php",
			new AdventureResult( AdventureResult.MEAT, moveType == PULL_MEAT_FROM_STORAGE ? amount : 0 ) );

		addFormField( "pwd" );
		addFormField( "amt", String.valueOf( amount ) );
		addFormField( "action", moveType == MEAT_TO_CLOSET ? "addmeat" : "takemeat" );

		this.moveType = moveType;

		if ( moveType == PULL_MEAT_FROM_STORAGE )
		{
			source = KoLCharacter.getStorage();
			destination = KoLCharacter.getInventory();
		}
	}

	public ItemStorageRequest( KoLmafia client, int moveType )
	{
		this( client, moveType, new Object[0] );
		this.moveType = moveType;
	}

	/**
	 * Constructs a new <code>ItemStorageRequest</code>.
	 * @param	client	The client to be notified of the results
	 * @param	moveType	The identifier for the kind of action taking place
	 * @param	attachments	The list of attachments involved in the request
	 */

	public ItemStorageRequest( KoLmafia client, int moveType, Object [] attachments )
	{
		super( client, moveType == STORAGE_TO_INVENTORY || moveType == EMPTY_STORAGE ? "storage.php" : "closet.php", attachments, 0 );

		addFormField( "pwd" );
		addFormField( "action", moveType == EMPTY_STORAGE ? "takeall" : moveType == INVENTORY_TO_CLOSET ? "put" : "take" );

		this.moveType = moveType;

		if ( moveType == CLOSET_TO_INVENTORY )
		{
			source = KoLCharacter.getCloset();
			destination = KoLCharacter.getInventory();
		}
		else if ( moveType == INVENTORY_TO_CLOSET )
		{
			source = KoLCharacter.getInventory();
			destination = KoLCharacter.getCloset();
		}
		else if ( moveType == STORAGE_TO_INVENTORY )
		{
			source = KoLCharacter.getStorage();
			destination = KoLCharacter.getInventory();
		}
	}

	public int getMoveType()
	{	return moveType;
	}

	public List getItems()
	{
		List itemList = new ArrayList();

		if ( attachments == null )
			return itemList;

		for ( int i = 0; i < attachments.length; ++i )
			itemList.add( attachments[i] );

		return itemList;
	}

	protected int getCapacity()
	{	return 11;
	}

	protected void repeat( Object [] attachments )
	{	(new ItemStorageRequest( client, moveType, attachments )).run();
	}

	protected String getSuccessMessage()
	{
		switch ( moveType )
		{
			case STORAGE_TO_INVENTORY:
				return "moved from storage to inventory";
			case INVENTORY_TO_CLOSET:
				return "moved from inventory to closet";
			case CLOSET_TO_INVENTORY:
				return "moved from closet to inventory";
		}

		return "";
	}

	protected void processResults()
	{
		switch ( moveType )
		{
			case EMPTY_STORAGE:
				while ( !KoLCharacter.getStorage().isEmpty() )
				{
					AdventureResult item = (AdventureResult) KoLCharacter.getStorage().remove(0);
					AdventureResult.addResultToList( KoLCharacter.getInventory(), item );
					AdventureResult.addResultToList( StaticEntity.getClient().getSessionTally(), item );

				}

				break;

			case STORAGE_TO_INVENTORY:
			case RETRIEVE_STORAGE:
				parseStorage();
				break;

			case PULL_MEAT_FROM_STORAGE:
				parseStorage();
				// Fall through and handle meat changes.

			case MEAT_TO_CLOSET:
			case MEAT_TO_INVENTORY:
				handleMeat();
				break;

		}

		super.processResults();
		KoLCharacter.refreshCalculatedLists();
		KoLCharacter.updateStatus();
	}

	/**
	 * Executes the <code>ItemStorageRequest</code>.
	 */

	public void run()
	{
		switch ( moveType )
		{
			case EMPTY_STORAGE:
				KoLmafia.updateDisplay( "Emptying storage..." );
				break;

			case STORAGE_TO_INVENTORY:
				KoLmafia.updateDisplay( "Moving items..." );
				break;

			case RETRIEVE_STORAGE:
				KoLmafia.updateDisplay( "Retrieving storage contents..." );
				break;

			case INVENTORY_TO_CLOSET:
			case CLOSET_TO_INVENTORY:
				KoLmafia.updateDisplay( "Moving items..." );
				break;

			case CLOSET_YOUR_CLOVERS:
				KoLmafia.updateDisplay( "Ladies and gentlemen of the Kingdom of Loathing. KoLmafia is closeting your clovers..." );
				break;

			case MEAT_TO_CLOSET:
			case MEAT_TO_INVENTORY:
			case PULL_MEAT_FROM_STORAGE:
				KoLmafia.updateDisplay( "Executing transaction..." );
				break;
		}

		super.run();
	}

	private void handleMeat()
	{
		if ( moveType == PULL_MEAT_FROM_STORAGE )
			return;

		// Now, determine how much is left in your closet
		// by locating "Your closet contains x meat" and
		// update the display with that information.

		int beforeMeatInCloset = KoLCharacter.getClosetMeat();
		int afterMeatInCloset = 0;

		Matcher meatInClosetMatcher = Pattern.compile( "<b>Your closet contains ([\\d,]+) meat\\.</b>" ).matcher( responseText );

		if ( meatInClosetMatcher.find() )
			afterMeatInCloset = StaticEntity.parseInt( meatInClosetMatcher.group(1) );

		KoLCharacter.setClosetMeat( afterMeatInCloset );
		client.processResult( new AdventureResult( AdventureResult.MEAT, beforeMeatInCloset - afterMeatInCloset ) );
	}

	private void parseStorage()
	{
		List storageContents = KoLCharacter.getStorage();
		Matcher storageMatcher = null;

		// Compute the number of pulls remaining based
		// on the response text.

		if ( !existingFrames.isEmpty() )
		{
			storageMatcher = Pattern.compile( "(\\d+) more" ).matcher( responseText );
			if ( storageMatcher.find() )
				HagnkStorageFrame.setPullsRemaining( StaticEntity.parseInt( storageMatcher.group(1) ) );
			else if ( KoLCharacter.isHardcore() || !KoLCharacter.canInteract() )
				HagnkStorageFrame.setPullsRemaining( 0 );
			else
				HagnkStorageFrame.setPullsRemaining( -1 );
		}

		// Start with an empty list

		if ( !storageContents.isEmpty() )
			return;

		// If there's nothing inside storage, return
		// because there's nothing to parse.

		storageMatcher = Pattern.compile( "name=\"whichitem1\".*?</select>", Pattern.DOTALL ).matcher( responseText );
		if ( !storageMatcher.find() )
			return;

		int lastFindIndex = 0;
		Matcher optionMatcher = Pattern.compile( "<option[^>]* value='([\\d]+)'>(.*?)\\(([\\d,]+)\\)" ).matcher( storageMatcher.group() );
		while ( optionMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = optionMatcher.end();
			int itemID = StaticEntity.parseInt( optionMatcher.group(1) );

			if ( TradeableItemDatabase.getItemName( itemID ) == null )
				TradeableItemDatabase.registerItem( itemID, optionMatcher.group(2).trim() );

			AdventureResult result = new AdventureResult( itemID, StaticEntity.parseInt( optionMatcher.group(3) ) );
			AdventureResult.addResultToList( storageContents, result );
		}
	}

	public String getCommandForm( int iterations )
	{
		StringBuffer commandString = new StringBuffer();

		AdventureResult [] items = new AdventureResult[ getItems().size() ];
		getItems().toArray( items );

		// If this is not a handled command form, then
		// return the empty string.

		switch ( moveType )
		{
			case ItemStorageRequest.INVENTORY_TO_CLOSET:
			case ItemStorageRequest.CLOSET_TO_INVENTORY:
			case ItemStorageRequest.STORAGE_TO_INVENTORY:
				break;

			default:
				return "";
		}

		// Otherwise, because commands cannot be strung
		// together on one line, print out one line at
		// a time to the buffered string.

		switch ( moveType )
		{
			case ItemStorageRequest.INVENTORY_TO_CLOSET:
				commandString.append( "closet put " );
				break;

			case ItemStorageRequest.CLOSET_TO_INVENTORY:
				commandString.append( "closet take " );
				break;

			case ItemStorageRequest.STORAGE_TO_INVENTORY:
				commandString.append( "pull " );
				break;
		}

		boolean needsComma = false;
		if ( this.meatAttachment != 0 )
		{
			commandString.append( this.meatAttachment );
			commandString.append( " meat" );
			needsComma = true;
		}

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[i] == null )
				continue;

			if ( needsComma )
				commandString.append( ", " );

			commandString.append( items[i].getCount() );
			commandString.append( " \"" );
			commandString.append( items[i].getName() );
			commandString.append( '\"' );
			needsComma = true;
		}

		return commandString.toString();
	}

	public static boolean processRequest( String urlString )
	{
		if ( urlString.indexOf( "storage.php" ) == -1 || urlString.indexOf( "action=takeall" ) != -1 || urlString.indexOf( "action=takemeat" ) != -1 )
			return false;

		ArrayList itemList = new ArrayList();
		StringBuffer itemListBuffer = new StringBuffer();

		Matcher itemMatcher = Pattern.compile( "whichitem\\d*=(\\d*)" ).matcher( urlString );
		Matcher quantityMatcher = Pattern.compile( "howmany\\d*=(\\d*)" ).matcher( urlString );

		while ( itemMatcher.find() && quantityMatcher.find() )
		{
			int itemID = StaticEntity.parseInt( itemMatcher.group(1) );
			int quantity = StaticEntity.parseInt( quantityMatcher.group(1) );
			AdventureResult item = new AdventureResult( itemID, quantity );

			itemList.add (item);

			if ( quantity == 0 )
				quantity = item.getCount( KoLCharacter.getStorage() );

			if ( itemListBuffer.length() > 0 )
				itemListBuffer.append( ", " );

			itemListBuffer.append( quantity );
			itemListBuffer.append( " " );
			itemListBuffer.append( TradeableItemDatabase.getItemName( itemID ) );
		}

		KoLmafia.getSessionStream().println( "pull " + itemListBuffer.toString() );
		for ( int i = 0; i < itemList.size(); ++i )
			StaticEntity.getClient().processResult( (AdventureResult) itemList.get(i) );

		return true;
	}
}
