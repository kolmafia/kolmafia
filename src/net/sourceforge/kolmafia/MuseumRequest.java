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

/**
 * An extension of <code>KoLRequest</code> which retrieves a list of
 * the character's equipment from the server.  At the current time,
 * there is no support for actually equipping items, so only the items
 * which are currently equipped are retrieved.
 */

public class MuseumRequest extends SendMessageRequest
{
	private boolean isDeposit;
	private boolean isManagement;

	public MuseumRequest( KoLmafia client )
	{
		super( client, "managecollectionshelves.php" );
		this.isManagement = false;
	}

	public MuseumRequest( KoLmafia client, Object [] attachments, boolean isDeposit )
	{
		super( client, "managecollection.php", attachments, 0 );
		addFormField( "pwd" );
		addFormField( "action", isDeposit ? "put" : "take" );

		this.isManagement = true;
		this.isDeposit = isDeposit;

		this.source = isDeposit ? KoLCharacter.getInventory() : KoLCharacter.getCollection();
		this.destination = isDeposit ? KoLCharacter.getCollection() : KoLCharacter.getInventory();
	}

	public MuseumRequest( KoLmafia client, AdventureResult [] items, int [] shelves )
	{
		this( client );
		addFormField( "pwd" );
		addFormField( "action", "arrange" );

		for ( int i = 0; i < items.length; ++i )
			addFormField( "whichshelf" + items[i].getItemID(), String.valueOf( shelves[i] ) );

		isManagement = true;
	}

	protected int getCapacity()
	{	return 11;
	}

	protected SendMessageRequest getSubInstance( Object [] attachments )
	{	return new MuseumRequest( client, attachments, isDeposit );
	}

	protected String getSuccessMessage()
	{	return "";
	}

	public void run()
	{
		if ( isManagement && isDeposit )
			KoLmafia.updateDisplay( "Placing items inside display case..." );
		else if ( isManagement )
			KoLmafia.updateDisplay( "Moving items from display case..." );
		else
			KoLmafia.updateDisplay( "Updating collection..." );

		super.run();
	}

	protected void processResults()
	{
		super.processResults();
		if ( !isManagement )
			MuseumManager.update( responseText );
	}

	protected boolean allowUntradeableTransfer()
	{	return true;
	}
}
