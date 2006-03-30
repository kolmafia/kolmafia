package net.sourceforge.kolmafia;

import javax.swing.JPopupMenu;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

import com.gc.systray.SystemTrayIconManager;
import net.java.dev.spellcast.utilities.DataUtilities;


public class SystemTrayFrame extends KoLDesktop implements Runnable
{
	private static int icon;
	private static SystemTrayIconManager manager = null;

	private SystemTrayFrame()
	{
	}

	public void setVisible( boolean isVisible )
	{
		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
			frames[i].setVisible( isVisible );
	}

	public static void updateTooltip()
	{
		if ( manager != null )
			manager.update( icon, VERSION_NAME + ": " + KoLCharacter.getUsername() );
	}

	public void run()
	{
		try
		{
			// First load the DesktopIndicator library to allow
			// for system tray usage.

			File library = new File( "data/DesktopIndicator.dll" );

			if ( !library.exists() )
			{
				InputStream input = DataUtilities.getFileInputStream( "", "", "DesktopIndicator.dll" );
				OutputStream output = new FileOutputStream( library );

				byte [] buffer = new byte[ 1024 ];
				int bufferLength;
				while ( (bufferLength = input.read( buffer )) != -1 )
					output.write( buffer, 0, bufferLength );

				output.close();
			}

			// Next, load the icon which will be used by KoLmafia
			// in the system tray.  For now, this will be the old
			// icon used by KoLmelion.

			File trayicon = new File( "images/KoLmelionIcon.ico" );

			if ( !trayicon.exists() )
			{
				java.io.InputStream input = DataUtilities.getFileInputStream( "", "", "KoLmelionIcon.ico" );
				java.io.OutputStream output = new java.io.FileOutputStream( trayicon );

				byte [] buffer = new byte[ 1024 ];
				int bufferLength;
				while ( (bufferLength = input.read( buffer )) != -1 )
					output.write( buffer, 0, bufferLength );

				output.close();
			}

			// Now, make calls to SystemTrayIconManager in order
			// to make use of the system tray.

			System.load( library.getAbsolutePath() );
			icon = SystemTrayIconManager.loadImage( trayicon.getAbsolutePath() );

			SystemTrayFrame.manager = new SystemTrayIconManager( icon, VERSION_NAME + ": " + KoLCharacter.getUsername() );

			JPopupMenu popup = new JPopupMenu();
			KoLMenuBar menubar = new KoLMenuBar( popup );

			manager.setLeftClickView( this );
			manager.setRightClickView( popup );
			manager.setVisible( true );
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}
	}

	public static void addTrayIcon()
	{
		if ( manager == null )
			(new Thread( new SystemTrayFrame() )).start();
		else
			updateTooltip();
	}

	public static void removeTrayIcon()
	{
		if ( manager != null )
		{
			KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
			existingFrames.toArray( frames );

			for ( int i = 0; i < frames.length; ++i )
				frames[i].dispose();

			manager.setVisible( false );
			manager = null;
		}
	}
}
