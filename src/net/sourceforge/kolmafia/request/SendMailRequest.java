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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.utilities.CharacterEntities;

public class SendMailRequest
	extends TransferItemRequest
{
	private final boolean isInternal;
	private final String recipient, message;

	public SendMailRequest( final String recipient, final String message )
	{
		super( "sendmessage.php" );

		this.recipient = recipient;
		this.message = message;

		this.addFormField( "action", "send" );
		this.addFormField( "towho", ContactManager.getPlayerId( this.recipient ) );
		this.addFormField( "message", this.message );

		this.isInternal = true;
	}

	public SendMailRequest( final String recipient, final Interpreter script )
	{
		super( "sendmessage.php" );

		this.recipient = recipient;
		this.message =
			"I have opted to let you know that I have chosen to run <" + script.getParser().getScriptName() + ">.  Thanks for writing this script!";

		this.addFormField( "action", "send" );
		this.addFormField( "towho", ContactManager.getPlayerId( this.recipient ) );
		this.addFormField( "message", this.message );

		this.isInternal = true;
	}

	public SendMailRequest( final String recipient, final String message, final AdventureResult attachment )
	{
		super( "sendmessage.php", attachment );

		this.recipient = recipient;
		this.message = CharacterEntities.unescape( message );

		this.addFormField( "action", "send" );
		this.addFormField( "towho", ContactManager.getPlayerId( this.recipient ) );
		this.addFormField( "message", this.message );

		this.isInternal = true;
	}

	public SendMailRequest( final String recipient, final String message, final Object[] attachments,
		boolean isInternal )
	{
		super( "sendmessage.php", attachments );

		this.recipient = recipient;
		this.message = CharacterEntities.unescape( message );

		this.addFormField( "action", "send" );
		this.addFormField( "towho", ContactManager.getPlayerId( this.recipient ) );
		this.addFormField( "message", this.message );

		if ( !isInternal )
		{
			this.addFormField( "savecopy", "on" );
		}

		this.isInternal = isInternal;
	}

	public String getRecipient()
	{
		return this.recipient;
	}

	@Override
	public int getCapacity()
	{
		return 11;
	}

	@Override
	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		return new SendMailRequest( this.recipient, this.message, attachments, this.isInternal );
	}

	@Override
	public String getStatusMessage()
	{
		return "Sending kmail to " + ContactManager.getPlayerName( this.recipient );
	}

	@Override
	public String getItemField()
	{
		return "whichitem";
	}

	@Override
	public String getQuantityField()
	{
		return "howmany";
	}

	@Override
	public String getMeatField()
	{
		return "sendmeat";
	}

	@Override
	public boolean allowMementoTransfer()
	{
		return true;
	}

	@Override
	public boolean allowUntradeableTransfer()
	{
		return true;
	}

        @Override
	public boolean parseTransfer()
	{
                return SendMailRequest.parseTransfer( this.getURLString(), this.responseText );
        }

	public static boolean parseTransfer( final String urlString, final String responseText )
	{
		if ( responseText.indexOf( "<center>Message " ) == -1 )
		{
			return false;
		}
		return SendMailRequest.parseTransfer( urlString );
	}

	public static boolean parseTransfer( final String urlString )
	{
		int meat = TransferItemRequest.transferredMeat( urlString, "sendmeat" );
		if ( meat > 0 )
		{
			ResultProcessor.processMeat( 0 - meat );
		}

		TransferItemRequest.transferItems( urlString, KoLConstants.inventory, null, 0 );
		KoLCharacter.updateStatus();
		return true;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "sendmessage.php" ) ||
		     urlString.indexOf( "action=send" ) == -1 )
		{
			return false;
		}

		return TransferItemRequest.registerRequest(
			"send a kmail", urlString, KoLConstants.inventory, 0, "sendmeat" );
	}
}
