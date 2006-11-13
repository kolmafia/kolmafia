package net.sourceforge.kolmafia;

import java.io.File;
import javax.swing.ImageIcon;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

import com.jeans.trayicon.WindowsTrayIcon;
import com.jeans.trayicon.TrayIconPopup;
import com.jeans.trayicon.TrayIconPopupSimpleItem;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public abstract class SystemTrayFrame implements KoLConstants
{
	private static WindowsTrayIcon icon = null;

	public static void addTrayIcon()
	{
		if ( icon != null )
			return;

		// Now, make calls to SystemTrayIconManager in order
		// to make use of the system tray.

		if ( !StaticEntity.loadLibrary( "TrayIcon12.dll" ) )
		{
			KoLmafia.updateDisplay( "Failed to load tray icon." );
			return;
		}

		System.load( (new File( "images/TrayIcon12.dll" )).getAbsolutePath() );
		WindowsTrayIcon.initTrayIcon( "KoLmafia" );

		try
		{
			StaticEntity.loadLibrary( "TrayIcon12.gif" );
			ImageIcon image = JComponentUtilities.getImage( "", "TrayIcon12.gif" );

			icon = new WindowsTrayIcon( image.getImage(), 16, 16 );
			icon.addMouseListener( new SetVisibleListener() );

			TrayIconPopup popup = new TrayIconPopup();
			popup.addMenuItem( new ShowMainWindowPopupItem() );
			popup.addMenuItem( new ConstructFramePopupItem( "Graphical CLI", "CommandDisplayFrame" ) );
			popup.addMenuItem( new ConstructFramePopupItem( "Preferences", "OptionsFrame" ) );
			popup.addMenuItem( new ConstructFramePopupItem( "Relay Browser", "LocalRelayServer" ) );
			popup.addMenuItem( new ConstructFramePopupItem( "KoLmafia Chat", "KoLMessenger" ) );
			popup.addMenuItem( new EndSessionPopupItem() );

			icon.setPopup( popup );
			updateToolTip();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return;
		}
	}

	public static void removeTrayIcon()
	{
		if ( icon != null )
			WindowsTrayIcon.cleanUp();
	}

	public static void updateToolTip()
	{
		if ( icon == null )
			return;

		icon.setVisible( true );
		if ( KoLCharacter.getUserName().equals( "" ) )
			icon.setToolTipText( VERSION_NAME );
		else
			icon.setToolTipText( VERSION_NAME + ": " + KoLCharacter.getUserName() );
	}

	public static void showBalloon( String message )
	{
		if ( icon == null )
			return;

		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		boolean anyFrameVisible = false;
		for ( int i = 0; i < frames.length; ++i )
			anyFrameVisible |= frames[i].isVisible();

		anyFrameVisible |= KoLDesktop.instanceExists() && KoLDesktop.getInstance().isVisible();

		if ( anyFrameVisible )
			return;

		try
		{
			icon.showBalloon( message, VERSION_NAME, 0, WindowsTrayIcon.BALLOON_INFO );
		}
		catch ( Exception e )
		{
			// Just an error when alerting the user.  It's
			// not important, so ignore the error for now.
		}
	}

	private static class SetVisibleListener extends MouseAdapter
	{
		public void mousePressed( MouseEvent e )
		{
			if ( e.isPopupTrigger() )
				return;

			showDisplay();
		}
	}

	public static void showDisplay()
	{
		if ( KoLRequest.sessionId == null )
		{
			KoLFrame.createDisplay( LoginFrame.class );
			return;
		}

		if ( existingFrames.isEmpty() )
		{
			KoLmafiaGUI.constructFrame( "LocalRelayServer" );
			return;
		}

		if ( KoLDesktop.instanceExists() )
		{
			KoLDesktop.getInstance().pack();
			KoLDesktop.getInstance().setVisible( true );
			KoLDesktop.getInstance().setExtendedState( KoLFrame.NORMAL );
		}

	}

	private static class ShowMainWindowPopupItem extends TrayIconPopupSimpleItem implements ActionListener
	{
		public ShowMainWindowPopupItem()
		{
			super( "Main Interface" );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	showDisplay();
		}
	}

	private static class ConstructFramePopupItem extends TrayIconPopupSimpleItem implements ActionListener
	{
		private String frame;

		public ConstructFramePopupItem( String title, String frame )
		{
			super( title );

			this.frame = frame;
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	KoLmafiaGUI.constructFrame( frame );
		}
	}

	private static class EndSessionPopupItem extends TrayIconPopupSimpleItem implements ActionListener
	{
		public EndSessionPopupItem()
		{
			super( "End Session" );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	System.exit(0);
		}
	}
}
