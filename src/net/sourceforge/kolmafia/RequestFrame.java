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

import java.awt.GridLayout;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RequestFrame extends KoLFrame
{
	private String title;
	protected JEditorPane display;
	private LimitedSizeChatBuffer buffer;

	public RequestFrame( KoLmafia client, String title, KoLRequest request )
	{
		super( title, client );

		this.title = title;
		this.display = new JEditorPane();
		this.display.setEditable( false );
		this.display.addHyperlinkListener( new KoLHyperlinkAdapter() );
		this.display.setText( "Retrieving..." );

		JScrollPane scrollPane = new JScrollPane( display, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JComponentUtilities.setComponentSize( scrollPane, 400, 300 );
		getContentPane().setLayout( new GridLayout( 1, 1 ) );
		getContentPane().add( scrollPane );

		(new DisplayRequestThread( request )).start();
	}

	private class DisplayRequestThread extends RequestThread
	{
		private KoLRequest request;

		public DisplayRequestThread( KoLRequest request )
		{	this.request = request;
		}

		public void run()
		{
			request.run();
			buffer = new LimitedSizeChatBuffer( title );
			buffer.setChatDisplay( display );
			buffer.append( request.responseText );
		}
	}
}
