/**
 * Based off of Bare Bones Browser Launch.
 * http://www.centerkey.com/java/browser/
 **/

package com.centerkey;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.URI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BareBonesBrowserLaunch
{
	public static void openURL( String url )
	{
		openURL( null, url );
	}

	public static void openURL( String browser, String url )
	{
		if ( !url.startsWith( "http" ) && !url.startsWith( "file" ) )
		{
			url = "file://" + url;
		}

		if ( url.contains( " " ) && !url.startsWith( "file" ) )
		{
			url = url.replace( ' ', '+' );
		}

		String osName = System.getProperty( "os.name" );

		if ( osName.startsWith( "Windows" ) )
		{
			loadWindowsBrowser( browser, url );
		}
		else if ( osName.startsWith( "Mac OS" ) )
		{
			loadMacOSXBrowser( browser, url );
		}
		else
		{
			loadUnixBrowser( browser, url );
		}
	}

	private static void loadWindowsBrowser( String browser, String url )
	{
		Runtime runtime = Runtime.getRuntime();

		if ( browser != null && !browser.equals( "" ) )
		{
			String executable = null;

			File file = new File( browser.contains( "." ) ? browser : ( browser + ".exe" ) );

			if ( file.exists() )
			{
				try
				{
					executable = "\"" + file.getCanonicalPath() + "\"";
				}
				catch ( Exception e )
				{
					executable = "\"" + file.getAbsolutePath() + "\"";
				}
			}
			else if ( !browser.contains( "\"" ) && ( browser.contains( " " ) || browser.contains( "%" ) ) )
			{
				executable = "start \"" + browser + "\"";
			}
			else
			{
				executable = "start " + browser;
			}

			try
			{
				Process process = runtime.exec( "cmd.exe /c " + executable + " " + url );
				process.waitFor();
				process.exitValue();

				return;
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}

		if ( loadAWTDesktopBrowser( url ) )
		{
			return;
		}

		try
		{
			Process process = runtime.exec( "cmd.exe /c rundll32.exe url.dll,FileProtocolHandler " + url );
			process.waitFor();
			process.exitValue();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}

	private static void loadMacOSXBrowser( String browser, String url )
	{
		if ( browser != null && !browser.equals( "" ) )
		{
			String command = null;

			if ( browser.startsWith( "/" ) && !browser.contains(".app" ) )
			{
				command = browser;
			}
			else if ( browser.startsWith( "-" ) )
			{
				command = "open " + browser;
			}
			else
			{
				command = "open -a " + browser;
			}

			try
			{
				Runtime runtime = Runtime.getRuntime();
				Process process = runtime.exec( command + " " + url );

				process.waitFor();
				process.exitValue();

				return;
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}

		if ( loadAWTDesktopBrowser( url ) )
		{
			return;
		}

		if ( macUseReflection && macOpenURLMethod == null )
		{
			try
			{
				Class<?> macFileManagerClass = Class.forName( "com.apple.eio.FileManager" );
				macOpenURLMethod = macFileManagerClass.getDeclaredMethod( "openURL", String.class );
			}
			catch ( Exception e )
			{
				macUseReflection = false;
			}
		}

		if ( macUseReflection )
		{
			try
			{
				macOpenURLMethod.invoke( null, url );
				return;
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}

		loadUnixBrowser( null, url );
	}

	private static void loadUnixBrowser( String browser, String url )
	{
		if ( browser != null && !browser.equals( "" ) )
		{
			try
			{
				Runtime runtime = Runtime.getRuntime();
				Process process = runtime.exec( browser + " " + url );

				process.waitFor();
				process.exitValue();

				return;
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}

		loadDefaultUnixBrowser( true, url );
	}

	private static void loadDefaultUnixBrowser( boolean checkWhichResult, String url )
	{
		Runtime runtime = Runtime.getRuntime();

		if ( unixDefaultBrowser != null )
		{
			if ( unixDefaultBrowser.equals( "" ) )
			{
				System.err.println( "Could not find a usable browser" );
				return;
			}

			try
			{
				Process process = runtime.exec( unixDefaultBrowser + " " + url );
				process.waitFor();
				process.exitValue();

				return;
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}

		if ( loadAWTDesktopBrowser( url ) )
		{
			return;
		}

		for ( String unixBrowser : UNIX_BROWSERS )
		{
			boolean tryBrowser = true;

			if ( checkWhichResult )
			{
				String whichResult = null;

				try
				{
					Process process = runtime.exec( "which " + unixBrowser );

					BufferedReader stream = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
					whichResult = stream.readLine();

					process.waitFor();
					process.exitValue();
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}

				if ( whichResult == null || whichResult.equals( "" ) || whichResult.contains( " " ) )
				{
					tryBrowser = false;
				}
			}

			if ( tryBrowser )
			{
				try
				{
					Process process = runtime.exec( unixBrowser + " " + url );
					process.waitFor();

					int exitValue = process.exitValue();

					if ( exitValue == 0 )
					{
						unixDefaultBrowser = unixBrowser;

						return;
					}
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
			}
		}

		if ( checkWhichResult )
		{
			loadDefaultUnixBrowser( false, url );
		}
		else
		{
			unixDefaultBrowser = "";
		}
	}

	private static boolean loadAWTDesktopBrowser( String url )
	{
		if ( awtUseReflection && ( awtDesktopObject == null ) )
		{
			try
			{
				Class<?> desktopClass = Class.forName( "java.awt.Desktop" );
				awtBrowseMethod = desktopClass.getDeclaredMethod( "browse", URI.class );

				Method getDesktopMethod = desktopClass.getDeclaredMethod( "getDesktop" );
				awtDesktopObject = getDesktopMethod.invoke( null );
			}
			catch ( ClassNotFoundException e )
			{
				awtUseReflection = false;
			}
			catch ( IllegalAccessException e )
			{
				e.printStackTrace();

				awtUseReflection = false;
			}
			catch ( InvocationTargetException e )
			{
				e.printStackTrace();

				awtUseReflection = false;
			}
			catch ( NoSuchMethodException e )
			{
				e.printStackTrace();

				awtUseReflection = false;
			}
		}

		if ( !awtUseReflection )
		{
			return false;
		}

		try
		{
			URI uri = new URI( url );

			awtBrowseMethod.invoke( awtDesktopObject, uri );

			return true;
		}
		catch ( Exception e )
		{
			e.printStackTrace();

			awtUseReflection = false;

			return false;
		}
	}

	private static boolean macUseReflection = true;
	private static Method macOpenURLMethod = null;
	private static String unixDefaultBrowser = null;

	private static boolean awtUseReflection = true;
	private static Object awtDesktopObject = null;
	private static Method awtBrowseMethod = null;

	private static final String[] UNIX_BROWSERS =
	{
		"xdg-open", "exo-open", "kde-open", "gnome-open", "gvfs-open", "sensible-browser", "open", "firefox", "netscape"
	};
}
