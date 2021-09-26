/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
import java.awt.SystemTray;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ClassLoader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.DescriptionFrame;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.RelayServer;

public abstract class StaticEntity
{
	// Version information for the current version of KoLmafia.

	private static final String PRODUCT_NAME = "KoLmafia";

	private static int usesSystemTray = 0;
	private static int usesRelayWindows = 0;

	private static boolean isGUIRequired = false;
	private static final boolean isHeadless = System.getProperty( "java.awt.headless", "" ).equals( "true" );

	public static final ArrayList<ActionPanel> existingPanels = new ArrayList<>();
	private static ActionPanel[] panelArray = new GenericPanel[ 0 ];

	public static String backtraceTrigger = null;
	private static Integer cachedRevisionNumber = null;
	private static String cachedversionName = null;
	private static String cachedBuildInfo = null;

	public static boolean userAborted = false;
	private static MafiaState globalContinuationState = MafiaState.CONTINUE;
	private static final ThreadLocal<MafiaState> threadLocalContinuationState = new ThreadLocal<MafiaState>()
	{
		protected MafiaState initialValue()
		{
			return MafiaState.CONTINUE;
		}
	};

	public static final String getVersion()
	{
		if ( StaticEntity.cachedversionName == null )
		{
			StringBuilder versionName = new StringBuilder( PRODUCT_NAME ).append( " r" ).append( StaticEntity.getRevision() );
			StaticEntity.cachedversionName = versionName.toString();
		}

		return StaticEntity.cachedversionName;
	}

	public static final int getRevision()
	{
		if ( StaticEntity.cachedRevisionNumber == null )
		{
			// Get the revision from the jar manifest attributes
			try
			{
				ClassLoader classLoader = StaticEntity.class.getClassLoader();
				if ( classLoader != null )
				{
					URL resource = classLoader.getResource( "META-INF/MANIFEST.MF" );
					if ( resource != null )
					{
						Manifest manifest = new Manifest( resource.openStream() );
						Attributes attributes = manifest.getMainAttributes();
						if ( attributes != null )
						{
							String buildRevision = attributes.getValue( "Build-Revision" );
							if ( buildRevision != null && StringUtilities.isNumeric( buildRevision ) )
							{
								try
								{
									StaticEntity.cachedRevisionNumber = Integer.parseInt( buildRevision );
								}
								catch ( NumberFormatException e )
								{
									// fall through
								}
							}
						}
					}
				}
			}
			catch ( IOException e )
			{
				// fall through
			}

			if ( StaticEntity.cachedRevisionNumber == null )
			{
				StaticEntity.cachedRevisionNumber = Integer.valueOf( 0 );
			}
		}

		return StaticEntity.cachedRevisionNumber;
	}

	public static final String getBuildInfo()
	{
		if ( StaticEntity.cachedBuildInfo == null )
		{
			StringBuilder cachedBuildInfo = new StringBuilder( "Build" );

			// Get the revision from the jar manifest attributes
			try
			{
				ClassLoader classLoader = StaticEntity.class.getClassLoader();
				if ( classLoader != null )
				{
					URL resource = classLoader.getResource( "META-INF/MANIFEST.MF" );
					if ( resource != null )
					{
						Manifest manifest = new Manifest( resource.openStream() );
						Attributes attributes = manifest.getMainAttributes();
						if ( attributes != null )
						{
							String attribute = attributes.getValue( "Build-Branch" );
							if ( attribute != null )
							{
								cachedBuildInfo.append( " " ).append( attribute );
							}
							attribute = attributes.getValue( "Build-Commit" );
							if ( attribute != null )
							{
								cachedBuildInfo.append( " " ).append( attribute );
							}
							attribute = attributes.getValue( "Build-Jdk" );
							if ( attribute != null )
							{
								cachedBuildInfo.append( " " ).append( attribute );
							}
							attribute = attributes.getValue( "Build-OS" );
							if ( attribute != null )
							{
								cachedBuildInfo.append( " " ).append( attribute );
							}
						}
					}
				}
			}
			catch ( IOException e )
			{
				// fall through
			}

			if ( cachedBuildInfo.toString().equals( "Build" ) )
			{
				cachedBuildInfo.append( " Unknown" );
			}

			StaticEntity.cachedBuildInfo = cachedBuildInfo.toString();
		}

		return StaticEntity.cachedBuildInfo;
	}

	public static final void overrideRevision( Integer revision )
	{
		StaticEntity.cachedRevisionNumber = revision;
		if ( revision == null )
		{
			StaticEntity.getRevision();
		}
	}

	public static final void setGUIRequired( boolean isGUIRequired )
	{
		StaticEntity.isGUIRequired = isGUIRequired;
	}

	public static final boolean isGUIRequired()
	{
		return StaticEntity.isGUIRequired && !StaticEntity.isHeadless;
	}

	public static final void registerPanel( final ActionPanel panel )
	{
		synchronized ( StaticEntity.existingPanels )
		{
			StaticEntity.existingPanels.add( panel );
			StaticEntity.getExistingPanels();
		}
	}

	public static final void unregisterPanel( final ActionPanel panel )
	{
		synchronized ( StaticEntity.existingPanels )
		{
			StaticEntity.existingPanels.remove( panel );
			StaticEntity.getExistingPanels();
		}
	}

	public static final void unregisterPanels( final Container container )
	{
		boolean removedPanel = false;

		synchronized ( StaticEntity.existingPanels )
		{
			Iterator<ActionPanel> panelIterator = StaticEntity.existingPanels.iterator();

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

			if ( SystemTray.isSupported() )
			{
				useTrayIcon = false;
			}

			if ( useTrayIcon )
			{
				StaticEntity.usesSystemTray = 1;
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
	 * A method used to open a new <code>DescriptionFrame</code> which
	 * displays the given location, relative to the KoL home directory for
	 * the current session.
	 */

	public static final void openDescriptionFrame( final String location )
	{
		DescriptionFrame.showRequest( RequestEditorKit.extractRequest( location ) );
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
		StaticEntity.printStackTrace( t, message, false );
	}

	public static final void printStackTrace( final Throwable t, final String message, final boolean printOnlyCause )
	{
		// Next, print all the information to the debug log so that
		// it can be sent.

		boolean shouldOpenStream = !RequestLogger.isDebugging();
		if ( shouldOpenStream )
		{
			RequestLogger.openDebugLog();
		}

		String printMsg;
		if ( message.startsWith( "Backtrace" ) )
		{
			StaticEntity.backtraceTrigger = null;
			printMsg = "Backtrace triggered, debug log printed.";
		}
		else if ( !message.equals( "" ) )
		{
			printMsg = message;
		}
		else
		{
			printMsg = "Unexpected error, debug log printed.";
		}
		KoLmafia.updateDisplay( printMsg );
		RequestLogger.updateSessionLog( printMsg );

		Throwable cause = t.getCause();

		if ( cause == null || !printOnlyCause )
		{
			StaticEntity.printStackTrace( t, message, RequestLogger.getDebugStream() );
		}

		if ( cause != null )
		{
			StaticEntity.printStackTrace( cause, message, RequestLogger.getDebugStream() );
		}

		if ( shouldOpenStream )
		{
			RequestLogger.closeDebugLog();
		}
	}

	public static final void printDebugText( final String message, final String text )
	{
		boolean shouldOpenStream = !RequestLogger.isDebugging();
		try
		{
			if ( shouldOpenStream )
			{
				RequestLogger.openDebugLog();
			}
			KoLmafia.updateDisplay( message );
			RequestLogger.updateSessionLog( message );
			RequestLogger.updateDebugLog( message );
			RequestLogger.updateDebugLog( text );
		}
		finally
		{
			if ( shouldOpenStream )
			{
				RequestLogger.closeDebugLog();
			}
		}
	}

	private static void printStackTrace( final Throwable t, final String message, final PrintStream ostream )
	{
		ostream.println( t.getClass() + ": " + t.getMessage() );
		t.printStackTrace( ostream );
		ostream.println( message );
	}

	private static File getJDKWorkingDirectory()
	{
		File currentJavaHome = new File( System.getProperty( "java.home" ) );

		if ( StaticEntity.hasJDKBinaries( currentJavaHome ) )
		{
			return currentJavaHome;
		}

		File javaInstallFolder = currentJavaHome.getParentFile();

		if ( StaticEntity.hasJDKBinaries( javaInstallFolder ) )
		{
			return javaInstallFolder;
		}

		File[] possibleJavaHomes = javaInstallFolder.listFiles();

		for ( int i = 0; i < possibleJavaHomes.length; ++i )
		{
			if ( StaticEntity.hasJDKBinaries( possibleJavaHomes[ i ] ) )
			{
				return possibleJavaHomes[ i ];
			}
		}

		return null;
	}

	private static boolean hasJDKBinaries( File javaHome )
	{
		if ( System.getProperty( "os.name" ).startsWith( "Windows" ) )
		{
			return new File( javaHome, "bin/javac.exe" ).exists();
		}
		else
		{
			return new File( javaHome, "bin/javac" ).exists();
		}
	}

	public static final String getProcessId()
	{
		File javaHome = StaticEntity.getJDKWorkingDirectory();

		if ( javaHome == null )
		{
			KoLmafia.updateDisplay( "To use this feature, you must run KoLmafia with a JDK instead of a JRE." );

			return null;
		}

		Runtime runtime = Runtime.getRuntime();

		String pid = null;

		try
		{
			String[] command = new String[ 2 ];

			if ( System.getProperty( "os.name" ).startsWith( "Windows" ) )
			{
				command[ 0 ] = new File( javaHome, "bin/jps.exe" ).getPath();
			}
			else
			{
				command[ 0 ] = new File( javaHome, "bin/jps" ).getPath();
			}

			command[ 1 ] = "-l";

			Process process = runtime.exec( command );
			BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );

			String line;

			StringBuffer sb = new StringBuffer();

			while ( ( pid == null ) && ( line = reader.readLine() ) != null )
			{
				sb.append( line );
				sb.append( KoLConstants.LINE_BREAK );

				if ( line.indexOf( "KoLmafia" ) != -1 )
				{
					pid = line.substring( 0, line.indexOf( ' ' ) );
				}

				boolean shouldOpenStream = !RequestLogger.isDebugging();

				if ( shouldOpenStream )
				{
					RequestLogger.openDebugLog();
				}

				RequestLogger.getDebugStream().println( sb.toString() );

				if ( shouldOpenStream )
				{
					RequestLogger.closeDebugLog();
				}
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		if ( pid != null )
		{
			return pid;
		}

		KoLmafia.updateDisplay( "Unable to determine KoLmafia process id." );

		return null;
	}

	public static final void printThreadDump()
	{
		File javaHome = StaticEntity.getJDKWorkingDirectory();

		if ( javaHome == null )
		{
			KoLmafia.updateDisplay( "To use this feature, you must run KoLmafia with a JDK instead of a JRE." );
			return;
		}

		String pid = StaticEntity.getProcessId();

		if ( pid == null )
		{
			return;
		}

		KoLmafia.updateDisplay( "Generating thread dump for KoLmafia process id " + pid + "..." );

		Runtime runtime = Runtime.getRuntime();

		StringBuffer sb = new StringBuffer();

		try
		{
			String[] command = new String[ 2 ];

			if ( System.getProperty( "os.name" ).startsWith( "Windows" ) )
			{
				command[ 0 ] = new File( javaHome, "bin/jstack.exe" ).getPath();
			}
			else
			{
				command[ 0 ] = new File( javaHome, "bin/jstack" ).getPath();
			}

			command[ 1 ] = pid;

			Process process = runtime.exec( command );
			BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );

			String line;

			while ( ( line = reader.readLine() ) != null )
			{
				sb.append( line );
				sb.append( KoLConstants.LINE_BREAK );
			}

			reader.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		boolean shouldOpenStream = !RequestLogger.isDebugging();

		if ( shouldOpenStream )
		{
			RequestLogger.openDebugLog();
		}

		RequestLogger.getDebugStream().println( sb.toString() );

		if ( shouldOpenStream )
		{
			RequestLogger.closeDebugLog();
		}
	}

	public static final void generateHeapDump()
	{
		File javaHome = StaticEntity.getJDKWorkingDirectory();

		if ( javaHome == null )
		{
			KoLmafia.updateDisplay( "To use this feature, you must run KoLmafia with a JDK instead of a JRE." );
			return;
		}

		String pid = StaticEntity.getProcessId();

		if ( pid == null )
		{
			return;
		}

		KoLmafia.updateDisplay( "Generating heap dump for KoLmafia process id " + pid + "..." );

		Runtime runtime = Runtime.getRuntime();

		StringBuffer sb = new StringBuffer();

		try
		{
			String[] command = new String[ 3 ];

			if ( System.getProperty( "os.name" ).startsWith( "Windows" ) )
			{
				command[ 0 ] = new File( javaHome, "bin/jmap.exe" ).getPath();
			}
			else
			{
				command[ 0 ] = new File( javaHome, "bin/jmap" ).getPath();
			}

			String javaVersion = System.getProperty( "java.runtime.version" );

			if ( javaVersion.contains( "1.5.0_" ) )
			{
				command[ 1 ] = "-heap:format=b";
			}
			else
			{
				int fileIndex = 0;
				String jmapFileName = null;
				File jmapFile = null;

				do
				{
					++fileIndex;
					jmapFileName = "kolmafia" + fileIndex + ".hprof";
					jmapFile = new File( KoLConstants.ROOT_LOCATION, jmapFileName );
				}
				while ( jmapFile.exists() );

				command[ 1 ] = "-dump:format=b,file=" + jmapFileName;
			}

			command[ 2 ] = pid;

			Process process = runtime.exec( command, new String[ 0 ], KoLConstants.ROOT_LOCATION );

			BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) );

			String line;

			while ( ( line = reader.readLine() ) != null )
			{
				sb.append( line );
				sb.append( KoLConstants.LINE_BREAK );
			}

			reader.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		boolean shouldOpenStream = !RequestLogger.isDebugging();

		if ( shouldOpenStream )
		{
			RequestLogger.openDebugLog();
		}

		RequestLogger.getDebugStream().println( sb.toString() );

		if ( shouldOpenStream )
		{
			RequestLogger.closeDebugLog();
		}
	}

	public static final String[] getPastUserList()
	{
		ArrayList<String> pastUserList = new ArrayList<String>();

		String user;
		File[] files = DataUtilities.listFiles( KoLConstants.SETTINGS_LOCATION );

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

	public static final MafiaState getContinuationState()
	{
		return  isRelayThread() ?
			StaticEntity.threadLocalContinuationState.get() :
			StaticEntity.globalContinuationState;
	}

	public static void setContinuationState( MafiaState state )
	{
		if ( isRelayThread() )
		{
			StaticEntity.threadLocalContinuationState.set( state );
		}
		else
		{
			StaticEntity.globalContinuationState = state;
		}
	}

	static final boolean isRelayThread()
	{
		return RelayServer.agentThreads.contains( Thread.currentThread() );
	}
}
