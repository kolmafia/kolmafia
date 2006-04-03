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

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class GreenMessageFrame extends SendMessageFrame
{
	private static final String [] HEADERS = { "Send this message:" };

	public GreenMessageFrame()
	{	this( "" );
	}

	public GreenMessageFrame( String recipient )
	{	this( recipient, "" );
	}

	public GreenMessageFrame( String recipient, String quotedMessage )
	{
		super( "Send a Green Message", recipient );
		messageEntry[0].setText( quotedMessage );
	}

	protected String [] getEntryHeaders()
	{	return HEADERS;
	}

	protected boolean sendMessage( String recipient, String [] messages )
	{
		GreenMessageFrame.this.setEnabled( false );
		(new GreenMessageRequest( StaticEntity.getClient(), recipient, messages[0], getAttachedItems(), getAttachedMeat() )).run();
		GreenMessageFrame.this.setEnabled( true );

		if ( StaticEntity.getClient().permitsContinue() )
		{
			DEFAULT_SHELL.updateDisplay( "Message sent to " + recipient );
			setTitle( "Message sent to " + recipient );
			return true;
		}
		else
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Failed to send message to " + recipient );
			setTitle( "Failed to send message to " + recipient );
			return false;
		}
	}
}