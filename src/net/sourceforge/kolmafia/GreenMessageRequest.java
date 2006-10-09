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

public class GreenMessageRequest extends SendMessageRequest
{
	private String recipient, message;

	public GreenMessageRequest( String recipient, String message, AdventureResult attachment )
	{
		super( "sendmessage.php", attachment );
		addFormField( "action", "send" );
		addFormField( "towho", recipient );
		addFormField( "message", message );

		if ( !message.equals( DEFAULT_KMAIL ) )
			addFormField( "savecopy", "on" );

		this.recipient = KoLmafia.getPlayerID( recipient );
		this.message = message;
	}

	public GreenMessageRequest( String recipient, String message, Object [] attachments )
	{
		super( "sendmessage.php", attachments );
		addFormField( "action", "send" );
		addFormField( "towho", recipient );
		addFormField( "message", message );

		if ( !message.equals( DEFAULT_KMAIL ) )
			addFormField( "savecopy", "on" );

		this.recipient = KoLmafia.getPlayerID( recipient );
		this.message = message;
	}

	public String getRecipient()
	{	return recipient;
	}

	protected int getCapacity()
	{	return 11;
	}

	protected SendMessageRequest getSubInstance( Object [] attachments )
	{	return new GreenMessageRequest( recipient, message, attachments );
	}

	protected String getSuccessMessage()
	{	return "<center>Message ";
	}

	protected final boolean allowUntradeableTransfer()
	{	return false;
	}

	protected String getStatusMessage()
	{	return "Sending kmail to " + KoLmafia.getPlayerName( recipient );
	}

	protected String getItemField()
	{	return "whichitem";
	}

	protected String getQuantityField()
	{	return "howmany";
	}

	protected String getMeatField()
	{	return "sendmeat";
	}

	public static boolean processRequest( String urlString )
	{
		if ( urlString.indexOf( "sendmessage.php" ) == -1 )
			return false;

		return processRequest( "send", urlString, storage, 0 );
	}
}
