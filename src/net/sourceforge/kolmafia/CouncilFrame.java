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

import java.awt.BorderLayout;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A class which displays the calendar image to be used for today's
 * moon phases and today's calendar.
 */

public class CouncilFrame extends KoLFrame
{
	private JEditorPane councilDisplay;
	private LimitedSizeChatBuffer councilBuffer;

	public CouncilFrame( KoLmafia client )
	{
		super( client, "Council Quests" );
		getContentPane().setLayout( new BorderLayout() );
		councilBuffer = new LimitedSizeChatBuffer( "KoLmafia: Council" );

		councilDisplay = new JEditorPane();
		councilDisplay.setEditable( false );
		councilDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );
		councilBuffer.setChatDisplay( councilDisplay );

		JScrollPane scroller = new JScrollPane( councilDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( scroller, 400, 300 );
		getContentPane().add( scroller, BorderLayout.CENTER );

		(new UpdateCouncilThread()).start();
	}

	/**
	 * Special thread which allows the council page to be updated outside
	 * of the Swing thread -- this means images can be downloaded without
	 * locking the UI.
	 */

	private class UpdateCouncilThread extends DaemonThread
	{
		public void run()
		{
			if ( client != null )
			{
				// Visit the council and see what they have to say.

				KoLRequest request = new KoLRequest( client, "council.php", true );
				request.run();

				// Strip out the link back to the town and pretty up
				// the text a little.

				String text = request.responseText.replaceFirst( "<a href=\"town.php\">Back to Seaside Town</a>", "" ).replaceFirst( "table width=95%", "table width=100%" );

				// Clear the display buffer and append the
				// modified response text.

				councilBuffer.clearBuffer();
				councilBuffer.append( text );
				councilDisplay.setCaretPosition( 0 );

				// Process the results in case they gave us meat or items

				client.processResults( request.responseText );
			}
		}
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( CouncilFrame.class, parameters )).run();
	}
}
