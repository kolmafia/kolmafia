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

import java.awt.Image;
import javax.swing.ImageIcon;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.File;

import com.jeans.trayicon.TrayIconPopup;
import com.jeans.trayicon.TrayIconPopupSimpleItem;
import com.jeans.trayicon.WindowsTrayIcon;

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

		File iconfile = new File( IMAGE_DIRECTORY, "TrayIcon12.dll" );

		System.load( iconfile.getAbsolutePath() );
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
			popup.addMenuItem( new LogoutPopupItem() );
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

		KoLFrame [] frames = StaticEntity.getExistingFrames();
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

		if ( !KoLDesktop.instanceExists() )
		{
			KoLmafiaGUI.checkFrameSettings();
			KoLDesktop.getInstance().initializeTabs();
		}

		KoLDesktop.displayDesktop();
	}

	private abstract static class ThreadedTrayIconPopupSimpleItem extends TrayIconPopupSimpleItem implements ActionListener, Runnable
	{
		public ThreadedTrayIconPopupSimpleItem( String title )
		{
			super( title );
			addActionListener( this );
		}

		public final void actionPerformed( ActionEvent e )
		{	(new Thread( this )).start();
		}
	}

	private static class ShowMainWindowPopupItem extends ThreadedTrayIconPopupSimpleItem
	{
		public ShowMainWindowPopupItem()
		{	super( "Main Interface" );
		}

		public void run()
		{	showDisplay();
		}
	}

	private static class ConstructFramePopupItem extends ThreadedTrayIconPopupSimpleItem
	{
		private String frame;

		public ConstructFramePopupItem( String title, String frame )
		{
			super( title );
			this.frame = frame;
		}

		public void run()
		{	KoLmafiaGUI.constructFrame( frame );
		}
	}

	private static class LogoutPopupItem extends ThreadedTrayIconPopupSimpleItem
	{
		public LogoutPopupItem()
		{	super( "Logout of KoL" );
		}

		public void run()
		{
			if ( KoLDesktop.instanceExists() )
				KoLDesktop.getInstance().setVisible( false );

			KoLFrame [] frames = StaticEntity.getExistingFrames();
			for ( int i = 0; i < frames.length; ++i )
				frames[i].setVisible( false );

			KoLFrame.createDisplay( LoginFrame.class );
			RequestThread.postRequest( new LogoutRequest() );
		}
	}

	private static class EndSessionPopupItem extends ThreadedTrayIconPopupSimpleItem
	{
		public EndSessionPopupItem()
		{	super( "Exit KoLmafia" );
		}

		public void run()
		{	System.exit(0);
		}
	}
}
