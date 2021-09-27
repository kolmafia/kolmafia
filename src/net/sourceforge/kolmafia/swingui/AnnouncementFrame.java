package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.preferences.Preferences;

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

		Preferences.setBoolean( "_announcementShown", true );
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
