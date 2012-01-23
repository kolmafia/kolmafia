/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.awt.Container;
import java.awt.Frame;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.swingui.DescriptionFrame;
import net.sourceforge.kolmafia.swingui.RequestFrame;
import net.sourceforge.kolmafia.swingui.RequestSynchFrame;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public abstract class StaticEntity
{
	private static KoLmafia client;
	private static int usesSystemTray = 0;
	private static int usesRelayWindows = 0;
	private static boolean isHeadless = System.getProperty( "java.awt.headless", "" ).equals( "true" );

	public static final ArrayList existingPanels = new ArrayList();
	private static ActionPanel[] panelArray = new GenericPanel[ 0 ];

	public static String backtraceTrigger = null;

	public static final String getVersion()
	{
		return StaticEntity.getVersion( false );
	}

	public static final String getVersion( final boolean forceRevision )
	{
		String version = KoLConstants.VERSION_NAME;
		if ( !KoLConstants.RELEASED || forceRevision )
		{
			int revision = StaticEntity.getRevision();
			if ( revision != 0 )
			{
				version += " r" + revision;
			}
		}
		return version;
	}

	public static final int getRevision()
	{
		if ( KoLConstants.REVISION == null )
		{
			return 0;
		}

		int colonIndex = KoLConstants.REVISION.indexOf( ":" );
		String revision = KoLConstants.REVISION;
		if ( colonIndex != -1 )
		{
			revision = KoLConstants.REVISION.substring( 0, colonIndex );
		}
		else if ( KoLConstants.REVISION.endsWith( "M" ) )
		{
			revision = KoLConstants.REVISION.substring( 0, KoLConstants.REVISION.length() - 1 );
		}

		return StringUtilities.isNumeric( revision ) ? StringUtilities.parseInt( revision ) : 0;
	}

	public static final int parseRevision( String version )
	{
		if ( version == null )
		{
			return 0;
		}
		if ( version.startsWith( "KoLmafia r" ) )
		{
			version = version.substring( 10 );
		}
		return StringUtilities.isNumeric( version ) ? StringUtilities.parseInt( version ) : 0;
	}

	public static final void setClient( final KoLmafia client )
	{
		StaticEntity.client = client;
	}

	public static final KoLmafia getClient()
	{
		return StaticEntity.client;
	}

	public static final void registerPanel( final ActionPanel panel )
	{
		synchronized ( StaticEntity.existingPanels )
		{
			StaticEntity.existingPanels.add( panel );
			StaticEntity.getExistingPanels();
		}
	}

	public static final void unregisterPanels( final Container container )
	{
		boolean removedPanel = false;

		synchronized ( StaticEntity.existingPanels )
		{
			Iterator panelIterator = StaticEntity.existingPanels.iterator();

			while ( panelIterator.hasNext() )
			{
				ActionPanel panel = (ActionPanel) panelIterator.next();

				if ( container.isAncestorOf( panel ) )
				{
					panel.dispose();
					panelIterator.remove();
					removedPanel = true;
				}
			}
		}

		if ( removedPanel )
		{
			StaticEntity.getExistingPanels();
		}
	}

	public static final ActionPanel[] getExistingPanels()
	{
		synchronized ( StaticEntity.existingPanels )
		{
			boolean needsRefresh = StaticEntity.panelArray.length != StaticEntity.existingPanels.size();

			if ( !needsRefresh )
			{
				for ( int i = 0; i < StaticEntity.panelArray.length && !needsRefresh; ++i )
				{
					needsRefresh = StaticEntity.panelArray[ i ] != StaticEntity.existingPanels.get( i );
				}
			}

			if ( needsRefresh )
			{
				StaticEntity.panelArray = new ActionPanel[ StaticEntity.existingPanels.size() ];
				StaticEntity.existingPanels.toArray( StaticEntity.panelArray );
			}

			return StaticEntity.panelArray;
		}
	}

	public static final boolean isHeadless()
	{
		return StaticEntity.isHeadless;
	}

	public static final boolean usesSystemTray()
	{
		if ( StaticEntity.usesSystemTray == 0 )
		{
			StaticEntity.usesSystemTray = 2;

			boolean useTrayIcon = Preferences.getBoolean( "useSystemTrayIcon" );

			if ( !System.getProperty( "os.name" ).startsWith( "Windows" ) )
			{
				useTrayIcon = false;
			}

			String javaArchitecture = System.getProperty( "sun.arch.data.model" );

			if ( javaArchitecture == null || !javaArchitecture.equals( "32" ) )
			{
				useTrayIcon = false;
			}

			if ( useTrayIcon )
			{
				try
				{
					FileUtilities.loadLibrary( UtilityConstants.IMAGE_LOCATION, "", "TrayIcon12.dll" );
					StaticEntity.usesSystemTray = 1;
				}
				catch ( Exception e )
				{
				}
			}
		}

		return StaticEntity.usesSystemTray == 1;
	}

	public static final boolean usesRelayWindows()
	{
		if ( StaticEntity.usesRelayWindows == 0 )
		{
			StaticEntity.usesRelayWindows = Preferences.getBoolean( "useRelayWindows" ) ? 1 : 2;
		}

		return StaticEntity.usesRelayWindows == 1;
	}

	/**
	 * A method used to open a new <code>RequestFrame</code> which displays the given location, relative to the KoL home
	 * directory for the current session. This should be called whenever <code>RequestFrame</code>s need to be created
	 * in order to keep code modular.
	 */

	public static final void openRequestFrame( final String location )
	{
		GenericRequest request = RequestEditorKit.extractRequest( location );

		if ( location.startsWith( "search" ) || location.startsWith( "desc" ) || location.startsWith( "static" ) || location.startsWith( "show" ) )
		{
			DescriptionFrame.showRequest( request );
			return;
		}

		Frame[] frames = Frame.getFrames();
		RequestFrame requestHolder = null;

		for ( int i = frames.length - 1; i >= 0; --i )
		{
			if ( frames[ i ].getClass() == RequestFrame.class && ( (RequestFrame) frames[ i ] ).hasSideBar() )
			{
				requestHolder = (RequestFrame) frames[ i ];
			}
		}

		if ( requestHolder == null )
		{
			RequestSynchFrame.showRequest( request );
			return;
		}

		if ( !location.equals( "main.php" ) )
		{
			requestHolder.refresh( request );
		}
	}

	public static final boolean executeCountdown( final String message, final int seconds )
	{
		PauseObject pauser = new PauseObject();

		StringBuffer actualMessage = new StringBuffer( message );

		for ( int i = seconds; i > 0 && KoLmafia.permitsContinue(); --i )
		{
			boolean shouldDisplay = false;

			// If it's the first count, then it should definitely be shown
			// for the countdown.

			if ( i == seconds )
			{
				shouldDisplay = true;
			}
			else if ( i >= 1800 )
			{
				shouldDisplay = i % 600 == 0;
			}
			else if ( i >= 600 )
			{
				shouldDisplay = i % 300 == 0;
			}
			else if ( i >= 300 )
			{
				shouldDisplay = i % 120 == 0;
			}
			else if ( i >= 60 )
			{
				shouldDisplay = i % 60 == 0;
			}
			else if ( i >= 15 )
			{
				shouldDisplay = i % 15 == 0;
			}
			else if ( i >= 5 )
			{
				shouldDisplay = i % 5 == 0;
			}
			else
			{
				shouldDisplay = true;
			}

			// Only display the message if it should be displayed based on
			// the above checks.

			if ( shouldDisplay )
			{
				actualMessage.setLength( message.length() );

				if ( i >= 60 )
				{
					int minutes = i / 60;
					actualMessage.append( minutes );
					actualMessage.append( minutes == 1 ? " minute" : " minutes" );

					if ( i % 60 != 0 )
					{
						actualMessage.append( ", " );
					}
				}

				if ( i % 60 != 0 )
				{
					actualMessage.append( i % 60 );
					actualMessage.append( i % 60 == 1 ? " second" : " seconds" );
				}

				actualMessage.append( "..." );
				KoLmafia.updateDisplay( actualMessage.toString() );
			}

			pauser.pause( 1000 );
		}

		return KoLmafia.permitsContinue();
	}

	public static final void printStackTrace()
	{
		StaticEntity.printStackTrace( "Forced stack trace" );
	}

	public static final void printStackTrace( final String message )
	{
		StaticEntity.printStackTrace( new Exception( message ), message );
	}

	public static final void printStackTrace( final Throwable t )
	{
		StaticEntity.printStackTrace( t, "" );
	}

	public static final void printStackTrace( final Throwable t, final String message )
	{
		printStackTrace( t, message, false );
	}

	public static final void printStackTrace( final Throwable t, final String message, boolean printOnlyCause )
	{
		// Next, print all the information to the debug log so that
		// it can be sent.

		boolean shouldOpenStream = !RequestLogger.isDebugging();
		if ( shouldOpenStream )
		{
			RequestLogger.openDebugLog();
		}

		if ( message.startsWith( "Backtrace" ) )
		{
			StaticEntity.backtraceTrigger = null;
			KoLmafia.updateDisplay( "Backtrace triggered, debug log printed." );
		}
		else if ( !message.equals( "" ) )
		{
			KoLmafia.updateDisplay( message );
		}
		else
		{
			KoLmafia.updateDisplay( "Unexpected error, debug log printed." );
		}

		Throwable cause = t.getCause();

		if ( cause == null || !printOnlyCause )
		{
			StaticEntity.printStackTrace( t, message, System.err );
			StaticEntity.printStackTrace( t, message, RequestLogger.getDebugStream() );
		}

		if ( cause != null )
		{
			StaticEntity.printStackTrace( cause, message, System.err );
			StaticEntity.printStackTrace( cause, message, RequestLogger.getDebugStream() );
		}

		try
		{
			if ( shouldOpenStream )
			{
				RequestLogger.closeDebugLog();
			}
		}
		catch ( Exception e )
		{
			// Okay, since you're in the middle of handling an
			// exception and got a new one, just return from here.
		}
	}

	private static final void printStackTrace( final Throwable t, final String message, final PrintStream ostream )
	{
		ostream.println( t.getClass() + ": " + t.getMessage() );
		t.printStackTrace( ostream );
		ostream.println( message );
	}

	public static final void printRequestData( final GenericRequest request )
	{
		if ( request == null )
		{
			return;
		}

		boolean shouldOpenStream = !RequestLogger.isDebugging();
		if ( shouldOpenStream )
		{
			RequestLogger.openDebugLog();
		}

		RequestLogger.updateDebugLog();
		RequestLogger.updateDebugLog( "" + request.getClass() + ": " + request.getURLString() );
		RequestLogger.updateDebugLog( KoLConstants.LINE_BREAK_PATTERN.matcher( request.responseText ).replaceAll( "" ) );
		RequestLogger.updateDebugLog();

		if ( shouldOpenStream )
		{
			RequestLogger.closeDebugLog();
		}
	}

	public static final String[] getPastUserList()
	{
		ArrayList pastUserList = new ArrayList();

		String user;
		File[] files = DataUtilities.listFiles( UtilityConstants.SETTINGS_LOCATION );

		for ( int i = 0; i < files.length; ++i )
		{
			user = files[ i ].getName();
			if ( user.startsWith( "GLOBAL" ) || !user.endsWith( "_prefs.txt" ) )
			{
				continue;
			}

			user = user.substring( 0, user.length() - 10 );
			if ( !user.equals( "GLOBAL" ) && !pastUserList.contains( user ) )
			{
				pastUserList.add( user );
			}
		}

		String[] pastUsers = new String[ pastUserList.size() ];
		pastUserList.toArray( pastUsers );
		return pastUsers;
	}

	public static final void disable( final String name )
	{
		String functionName;
		StringTokenizer tokens = new StringTokenizer( name, ", " );

		while ( tokens.hasMoreTokens() )
		{
			functionName = tokens.nextToken();
			if ( !KoLConstants.disabledScripts.contains( functionName ) )
			{
				KoLConstants.disabledScripts.add( functionName );
			}
		}
	}

	public static final void enable( final String name )
	{
		if ( name.equals( "all" ) )
		{
			KoLConstants.disabledScripts.clear();
			return;
		}

		StringTokenizer tokens = new StringTokenizer( name, ", " );
		while ( tokens.hasMoreTokens() )
		{
			KoLConstants.disabledScripts.remove( tokens.nextToken() );
		}
	}

	public static final boolean isDisabled( final String name )
	{
		if ( name.equals( "enable" ) || name.equals( "disable" ) )
		{
			return false;
		}

		return KoLConstants.disabledScripts.contains( "all" ) || KoLConstants.disabledScripts.contains( name );
	}
}
