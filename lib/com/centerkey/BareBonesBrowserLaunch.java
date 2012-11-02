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
		File file = new File( browser );

		if ( file.exists() )
		{
			return file;
		}

		String executable = null;

		if ( browser.equalsIgnoreCase( "opera" ) )
		{
			executable = "Opera\\opera.exe";
		}
		else if ( browser.equalsIgnoreCase( "firefox" ) )
		{
			executable = "Mozilla Firefox\firefox.exe";
		}
		else if ( browser.equalsIgnoreCase( "chrome" ) )
		{
			executable = "Google\\Chrome\\Application\\chrome.exe";
		}
		else if ( browser.equalsIgnoreCase( "iexplore" ) )
		{
			executable = "Internet Explorer\\iexplore.exe";
		}

		if ( executable == null )
		{
			return null;
		}

		file = new File( System.getProperty( "user.home" ), "Local Settings\\Application Data\\" + executable );

		if ( file.exists() )
		{
			return file;
		}

		file = new File( System.getProperty( "user.home" ), "AppData\\Local\\" + executable );

		if ( file.exists() )
		{
			return file;
		}

		for ( char drive = 'C'; drive <= 'Z'; ++drive )
		{
			file = new File( drive + ":\\Windows\\system32\\" + executable );

			if ( file.exists() )
			{
				return file;
			}

			file = new File( drive + ":\\Program Files\\" + executable );

			if ( file.exists() )
			{
				return file;
			}

			file = new File( drive + ":\\Program Files (x86)\\" + executable );

			if ( file.exists() )
			{
				return file;
			}
		}

		return null;
	}

	private static void loadWindowsBrowser( String browser, String url )
	{
		Runtime runtime = Runtime.getRuntime();

		if ( browser != null && !browser.equals( "" ) )
		{
			File browserFile = findWindowsBrowser( browser );

			if ( browserFile != null )
			{
				try
				{
					browser = "\"" + browserFile.getCanonicalPath() + "\"";
				}
				catch ( Exception e )
				{
					browser = browserFile.getAbsolutePath();
				}
			}

			try
			{
				Process process = runtime.exec( new String[] { "cmd.exe", "/c", browser, url } );

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
			Process process = runtime.exec( new String[] { "cmd.exe", "/c", "rundll32.exe", "url.dll,FileProtocolHandler", url } );
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
		Runtime runtime = Runtime.getRuntime();

		if ( browser != null && !browser.equals( "" ) )
		{
			try
			{
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

		if ( unixDefaultBrowser == null )
		{
			for ( String unixBrowser : UNIX_BROWSERS )
			{
				try
				{
					Process process = runtime.exec( "which " + unixBrowser );

					BufferedReader stream = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
					String whichResult = stream.readLine();

					process.waitFor();
					process.exitValue();

					if ( whichResult != null && !whichResult.equals( "" ) && !whichResult.contains( " " ) )
					{
						unixDefaultBrowser = unixBrowser;
					}
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
			}

			if ( unixDefaultBrowser == null )
			{
				unixDefaultBrowser = "";
			}
		}

		if ( !unixDefaultBrowser.equals( "" ) )
		{
			try
			{
				Process process = runtime.exec( unixDefaultBrowser + " " + url );
				process.waitFor();
				process.exitValue();
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
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

	private static boolean macUseReflection = true;
	private static Method macOpenURLMethod = null;
	private static String unixDefaultBrowser = null;

	private static boolean awtUseReflection = true;
	private static Object awtDesktopObject = null;
	private static Method awtBrowseMethod = null;

	private static final String[] UNIX_BROWSERS =
	{
		"sensible-browser", "xdg-open", "exo-open", "kde-open", "gnome-open", "gvfs-open", "open", "firefox"
	};
}
