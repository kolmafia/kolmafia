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

/**
 * An extension of a <code>KoLRequest</code> which specifically handles
 * donating to the Hall of the Legends of the Times of Old.
 */

public class ProposeTradeRequest extends SendMessageRequest
{
	private boolean isCounterOffer;
	private String recipient, message;

	public ProposeTradeRequest( KoLmafia client )
	{	super( client, "makeoffer.php", new Object[0], 0 );
	}

	public ProposeTradeRequest( KoLmafia client, int offerID, String message, Object [] attachments, int meatAttachment )
	{
		super( client, "counteroffer.php", attachments, meatAttachment );
		addFormField( "action", "counter" );
		addFormField( "whichoffer", String.valueOf( offerID ) );
		addFormField( "pwd" );
		addFormField( "memo", message.replaceAll( "Meat:", "Please respond with " ) );
		addFormField( "offermeat", String.valueOf( this.meatAttachment ) );

		this.message = message;
		this.isCounterOffer = true;
		this.recipient = KoLmafia.getPlayerID( recipient );
	}

	public ProposeTradeRequest( KoLmafia client, String recipient, String message, Object [] attachments, int meatAttachment )
	{
		super( client, "makeoffer.php", attachments, meatAttachment );
		addFormField( "action", "proposeoffer" );
		addFormField( "pwd" );
		addFormField( "towho", recipient );
		addFormField( "memo", message.replaceAll( "Meat:", "Please respond with " ) );
		addFormField( "offermeat", String.valueOf( this.meatAttachment ) );

		this.message = message;
		this.isCounterOffer = false;
		this.recipient = KoLmafia.getPlayerID( recipient );
	}

	protected int getCapacity()
	{	return 11;
	}

	protected SendMessageRequest getSubInstance( Object [] attachments )
	{
		// This request cannot be repeated.  Therefore, if the person attaches
		// too much to the request, only handle the first 11 and do nothing
		// if the repeat method is called.

		return this;
	}

	protected String getSuccessMessage()
	{	return "";
	}

	protected final boolean allowUntradeableTransfer()
	{	return false;
	}

	protected void processResults()
	{	responseText = responseText.substring( 0, responseText.lastIndexOf( "<b>Propose" ) ).replaceAll( "[Mm]eat:", "Please respond with " );
	}
}
