/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.swingui.listener.HyperlinkAdapter;
import net.sourceforge.kolmafia.swingui.panel.OptionsPanel;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;

public class AnnouncementFrame
	extends GenericFrame
{
	
	private static AnnouncementFrame INSTANCE = null;
	private static RequestPane display;
	private static OptionsPanel setting;

	public AnnouncementFrame()
	{
		super( "KoL Announcement" );
		AnnouncementFrame.INSTANCE = this;

		AnnouncementFrame.display = new RequestPane();
		AnnouncementFrame.setting = new AnnouncementOptionPanel();

		this.getFramePanel().add( AnnouncementFrame.setting, BorderLayout.NORTH );

		AnnouncementFrame.display.addHyperlinkListener( new HyperlinkAdapter() );
		JComponentUtilities.setComponentSize( AnnouncementFrame.display, 500, 300 );
		this.setCenterComponent( AnnouncementFrame.display );
	}

	public static final void showRequest( final String HTML )
	{
		if ( !GenericFrame.instanceExists() )
		{
			return;
		}

		if ( AnnouncementFrame.INSTANCE == null )
		{
			GenericFrame.createDisplay( AnnouncementFrame.class );
		}

		AnnouncementFrame.display.setText( HTML );
	}
	
	private class AnnouncementOptionPanel
		extends OptionsPanel
	{
		public AnnouncementOptionPanel()
		{
			super( new Dimension( 16, 16 ), new Dimension( 300, 16 ) );
			
			
			String[][] options =
			{
				{ "showAnnouncements", "Show special announcements from the login page" },
			};

			this.setOptions( options );
		}
	}
}
