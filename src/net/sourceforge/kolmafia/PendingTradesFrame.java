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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PendingTradesFrame extends RequestFrame
{
	public PendingTradesFrame()
	{	this( new ProposeTradeRequest( StaticEntity.getClient() ) );
	}

	public PendingTradesFrame( ProposeTradeRequest ptr )
	{
		super( "Pending Trades", ptr );
		mainDisplay.addHyperlinkListener( new TradeLinkListener() );
	}

	public boolean hasSideBar()
	{	return false;
	}
	
	private class TradeLinkListener extends KoLHyperlinkAdapter
	{
		protected void handleInternalLink( String location )
		{
			// If it's not a special kind of hyperlink, then
			// just use the original hyperlink handling that
			// is provided by the parent class.

			if ( !location.startsWith( "counteroffer.php" ) )
			{
				super.handleInternalLink( location );
				return;
			}

			// Otherwise, instantiate a brand-new trade frame
			// so that the person can respond properly to the
			// trade request.

			String offerID = location.substring( location.lastIndexOf( "=" ) + 1 );

			Object [] parameters = new Object[2];
			parameters[0] = "Offer ID # " + offerID;
			parameters[1] = offerID;

			(new CreateFrameRunnable( ProposeTradeFrame.class, parameters )).run();
			dispose();
		}
	}
}
