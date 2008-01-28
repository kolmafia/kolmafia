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

import javax.swing.JTabbedPane;

import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;

import net.sourceforge.kolmafia.request.GenericRequest;

public class EventsFrame
	extends KoLPanelFrame
{
	public EventsFrame()
	{
		super( "Recent Events" );
		this.setContentPane( new EventsPanel() );
	}

	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	private class EventsPanel
		extends ScrollablePanel
	{
		public EventsPanel()
		{
			super( "Recent Events", "clear", "check", new ShowDescriptionList( KoLConstants.eventHistory ) );
		}

		public void actionConfirmed()
		{
			// Clear list of events
			KoLConstants.eventHistory.clear();
		}

		public void actionCancelled()
		{
			// Connect to main map to pick up new events
			KoLmafia.updateDisplay( "Checking for new events..." );
			RequestThread.postRequest( new GenericRequest( "main.php" ) );
		}
	}
}
