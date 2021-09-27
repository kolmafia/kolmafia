package net.sourceforge.kolmafia.swingui;

import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.imageio.ImageIO;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.LogoutManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;

public abstract class SystemTrayFrame
{
	private static TrayIcon icon = null;

	public synchronized static final void addTrayIcon()
	{
		if ( SystemTrayFrame.icon != null )
		{
			return;
		}

		// Now, make calls to SystemTrayIconManager in order
		// to make use of the system tray.

		FileUtilities.loadLibrary( KoLConstants.IMAGE_LOCATION, "images/", "TrayIcon12.gif" );

		try
		{
			Image image = JComponentUtilities.getImage( "images/", "TrayIcon12.gif" ).getImage();

			TrayIcon icon = new TrayIcon( image.getScaledInstance( 16, 16, Image.SCALE_DEFAULT ), "KoLmafia" );
			icon.addMouseListener( new SetVisibleListener() );

			PopupMenu popup = new PopupMenu();
			popup.add( new ShowMainWindowPopupItem() );
			popup.add( new ConstructFramePopupItem( "Graphical CLI", "CommandDisplayFrame" ) );
			popup.add( new ConstructFramePopupItem( "Preferences", "OptionsFrame" ) );
			popup.add( new ConstructFramePopupItem( "Relay Browser", "LocalRelayServer" ) );
			popup.add( new ConstructFramePopupItem( "KoLmafia Chat", "ChatManager" ) );
			popup.add( new LogoutPopupItem() );
			popup.add( new EndSessionPopupItem() );

			icon.setPopupMenu( popup );
			SystemTray.getSystemTray().add( icon );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return;
		}
	}

	public static final void removeTrayIcon()
	{
		if ( SystemTrayFrame.icon != null )
		{
			SystemTray.getSystemTray().remove( icon );
		}
	}

	public static final void updateToolTip()
	{
		if ( KoLCharacter.getUserName().equals( "" ) )
		{
			SystemTrayFrame.updateToolTip( StaticEntity.getVersion() );
		}
		else
		{
			SystemTrayFrame.updateToolTip( StaticEntity.getVersion() + ": " + KoLCharacter.getUserName() );
		}
	}

	public static final void updateToolTip( final String message )
	{
		if ( SystemTrayFrame.icon == null )
		{
			return;
		}

		SystemTrayFrame.icon.setToolTip( message );
	}

	public static final void showBalloon( final String message )
	{
		if ( SystemTrayFrame.icon == null )
		{
			return;
		}

		Frame[] frames = Frame.getFrames();
		boolean anyFrameVisible = false;
		for ( int i = 0; i < frames.length; ++i )
		{
			anyFrameVisible |= frames[ i ].isVisible();
		}

		anyFrameVisible |= KoLDesktop.instanceExists() && KoLDesktop.getInstance().isVisible();

		if ( anyFrameVisible )
		{
			return;
		}

		try
		{
			SystemTrayFrame.icon.displayMessage( message, StaticEntity.getVersion(), TrayIcon.MessageType.INFO );
		}
		catch ( Exception e )
		{
			// Just an error when alerting the user.  It's
			// not important, so ignore the error for now.
		}
	}

	private static class SetVisibleListener
		extends MouseAdapter
	{
		@Override
		public void mousePressed( final MouseEvent e )
		{
			if ( e.isPopupTrigger() )
			{
				return;
			}

			SystemTrayFrame.showDisplay();
		}
	}

	public static final void showDisplay()
	{
		KoLmafiaGUI.checkFrameSettings();
		KoLDesktop.getInstance().setVisible( true );
	}

	private static abstract class ThreadedTrayIconMenuItem
		extends MenuItem
		implements ActionListener, Runnable
	{
		public ThreadedTrayIconMenuItem( final String title )
		{
			super( title );
			this.addActionListener( this );
		}

		public final void actionPerformed( final ActionEvent e )
		{
			RequestThread.runInParallel( this, false );
		}
	}

	private static class ShowMainWindowPopupItem
		extends ThreadedTrayIconMenuItem
	{
		public ShowMainWindowPopupItem()
		{
			super( "Main Interface" );
		}

		public void run()
		{
			SystemTrayFrame.showDisplay();
		}
	}

	private static class ConstructFramePopupItem
		extends ThreadedTrayIconMenuItem
	{
		private final String frame;

		public ConstructFramePopupItem( final String title, final String frame )
		{
			super( title );
			this.frame = frame;
		}

		public void run()
		{
			KoLmafiaGUI.constructFrame( this.frame );
		}
	}

	private static class LogoutPopupItem
		extends ThreadedTrayIconMenuItem
	{
		public LogoutPopupItem()
		{
			super( "Logout of KoL" );
		}

		public void run()
		{
			LogoutManager.logout();
		}
	}

	private static class EndSessionPopupItem
		extends ThreadedTrayIconMenuItem
	{
		public EndSessionPopupItem()
		{
			super( "Exit KoLmafia" );
		}

		public void run()
		{
			KoLmafia.quit();
		}
	}
}
