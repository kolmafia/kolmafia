/**
 * Based off of Bare Bones Browser Launch.
 * http://www.centerkey.com/java/browser/
 **/

package com.centerkey;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

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

	private static File findWindowsBrowser( String browser )
	{
		// Check if it's an absolute path.

		File file = new File( browser );

		if ( file.exists() )
		{
			return file;
		}

		String executable = browser.contains( "." ) ?  browser : ( browser + ".exe" );

		// Check if it's on the path.

		String pathResult = null;

		try
		{
			Runtime runtime = Runtime.getRuntime();
			Process process = runtime.exec( new String[] { "where.exe", executable } );

			BufferedReader stream = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
			pathResult = stream.readLine();

			process.waitFor();
			process.exitValue();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		if ( pathResult != null && pathResult.contains( "\\" ) )
		{
			return new File( pathResult );
		}

		// Check if it's relative to a common environment variable.

		file = findWindowsBrowser( System.getenv( "ProgramFiles" ), executable );

		if ( file != null && file.exists() )
		{
			return file;
		}

		file = findWindowsBrowser( System.getenv( "ProgramFiles(x86)" ), executable );

		if ( file != null && file.exists() )
		{
			return file;
		}

		String localAppData = System.getenv( "LocalAppData" );

		if ( localAppData == null || localAppData.equals( "" ) )
		{
			String userProfile = System.getenv( "UserProfile" );

			if ( userProfile != null && !userProfile.equals( "" ) )
			{
				localAppData = userProfile + "\\Local Settings\\Application Data";
			}
		}

		file = findWindowsBrowser( localAppData, executable );

		if ( file != null && file.exists() )
		{
			return file;
		}

		file = findWindowsBrowser( System.getenv( "AppData" ), executable );

		if ( file != null && file.exists() )
		{
			return file;
		}

		return null;
	}

	private static File findWindowsBrowser( String basePath, String executable )
	{
		if ( basePath == null || basePath.equals( "" ) )
		{
			return null;
		}

		File baseFolder = new File( basePath );

		String dirResult = null;

		try
		{
			Runtime runtime = Runtime.getRuntime();
			Process process = runtime.exec( "cmd.exe /c dir /b /s " + executable, null, baseFolder );

			BufferedReader stream = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
			dirResult = stream.readLine();

			process.waitFor();
			process.exitValue();
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		if ( dirResult == null || dirResult.equals( "" ) || dirResult.equals( "File Not Found" ) )
		{
			return null;
		}

		return new File( dirResult );
	}

	private static void loadWindowsBrowser( String browser, String url )
	{
		Runtime runtime = Runtime.getRuntime();

		if ( browser != null && !browser.equals( "" ) )
		{
			String browserPath = windowsBrowsers.get( browser );

			if ( browserPath == null )
			{
				File browserFile = findWindowsBrowser( browser );

				if ( browserFile != null )
				{
					try
					{
						browserPath = "\"" + browserFile.getCanonicalPath() + "\"";
					}
					catch ( Exception e )
					{
						browserPath = "\"" + browserFile.getAbsolutePath() + "\"";
					}
				}
				else
				{
					browserPath = "";
				}

				windowsBrowsers.put( browser, browserPath );
			}

			if ( !browserPath.equals( "" ) )
			{
				try
				{
					Process process = runtime.exec( "cmd.exe /c " + browserPath + " " + url );
					process.waitFor();
					process.exitValue();

					return;
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
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
			catch ( Exception e )
			{
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

	private static Map<String, String> windowsBrowsers = new ConcurrentHashMap<String, String>();
	private static boolean macUseReflection = true;
	private static Method macOpenURLMethod = null;
	private static String unixDefaultBrowser = null;

	private static boolean awtUseReflection = true;
	private static Object awtDesktopObject = null;
	private static Method awtBrowseMethod = null;

	private static final String[] UNIX_BROWSERS =
	{
		"sensible-browser", "xdg-open", "exo-open", "kde-open", "gnome-open", "gvfs-open", "open", "firefox", "netscape"
	};
}
