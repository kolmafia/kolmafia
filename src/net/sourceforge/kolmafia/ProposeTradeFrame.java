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

public class ProposeTradeFrame extends SendMessageFrame
{
	private String offerId;
	private static final String [] HEADERS = { "Send this note:" };

	public ProposeTradeFrame()
	{	this( "", null );
	}

	public ProposeTradeFrame( String recipient )
	{	this( recipient, null );
	}

	public ProposeTradeFrame( String recipient, String offerId )
	{
		super( "Send a Trade Proposal", recipient );
		this.offerId = offerId;

		if ( this.offerId != null )
			recipientEntry.setEnabled( false );
	}

	protected String [] getEntryHeaders()
	{	return HEADERS;
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( recipientEntry == null )
			return;

		super.setEnabled( isEnabled );

		if ( this.offerId != null )
			recipientEntry.setEnabled( false );
	}

	protected boolean sendMessage( String recipient, String [] messages )
	{
		// Close all pending trades frames first

		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );
		for ( int i = 0; i < frames.length; ++i )
			if ( frames[i] instanceof PendingTradesFrame )
				((PendingTradesFrame)frames[i]).dispose();

		// Send the offer / response

		if ( offerId != null )
			(new ProposeTradeRequest( StaticEntity.parseInt( offerId ), messages[0], getAttachedItems() )).run();

		Object [] parameters = new Object[1];
		parameters[0] = offerId != null ? new ProposeTradeRequest() :
			new ProposeTradeRequest( recipient, messages[0], getAttachedItems() );

		createDisplay( PendingTradesFrame.class, parameters );
		return true;
	}
}
