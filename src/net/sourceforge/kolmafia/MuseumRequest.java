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

public class MuseumRequest extends SendMessageRequest
{
	private boolean isDeposit;
	private boolean isWithdrawal;
	private boolean isManagement;

	public MuseumRequest()
	{
		super( "managecollectionshelves.php" );

		this.isDeposit = false;
		this.isWithdrawal = false;
		this.isManagement = false;
	}

	public MuseumRequest( Object [] attachments, boolean isDeposit )
	{
		super( "managecollection.php", attachments );
		this.addFormField( "action", isDeposit ? "put" : "take" );

		this.isManagement = true;
		this.isDeposit = isDeposit;
		this.isWithdrawal = !isDeposit;

		this.source = isDeposit ? inventory : collection;
		this.destination = isDeposit ? collection : inventory;
	}

	public MuseumRequest( AdventureResult [] items, int [] shelves )
	{
		this();
		this.addFormField( "action", "arrange" );

		for ( int i = 0; i < items.length; ++i )
			this.addFormField( "whichshelf" + items[i].getItemId(), String.valueOf( shelves[i] ) );

		this.isDeposit = false;
		this.isWithdrawal = false;
		this.isManagement = true;
	}

	protected boolean retryOnTimeout()
	{	return !isDeposit && !isWithdrawal;
	}

	public int getCapacity()
	{	return 11;
	}

	public SendMessageRequest getSubInstance( Object [] attachments )
	{	return new MuseumRequest( attachments, this.isDeposit );
	}

	public String getSuccessMessage()
	{	return "";
	}

	public String getItemField()
	{	return "whichitem";
	}

	public String getQuantityField()
	{	return "howmany";
	}

	public String getMeatField()
	{	return "";
	}

	public void processResults()
	{
		super.processResults();
		if ( !this.isManagement )
			MuseumManager.update( this.responseText );
	}

	public boolean allowMementoTransfer()
	{	return true;
	}

	public boolean allowUntradeableTransfer()
	{	return true;
	}

	public boolean allowUngiftableTransfer()
	{	return true;
	}

	public String getStatusMessage()
	{	return this.isDeposit ? "Placing items in display case" : this.isWithdrawal ? "Removing items from display case" : "Updating display case";
	}

	public static boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "managecollection.php" ) )
			return false;

		if ( urlString.indexOf( "action=take" ) != -1 )
			return registerRequest( "remove from display case", urlString, collection, inventory, null, 0 );

		if ( urlString.indexOf( "action=put" ) != -1 )
			return registerRequest( "put in display case", urlString, inventory, collection, null, 0 );

		return true;
	}
}

