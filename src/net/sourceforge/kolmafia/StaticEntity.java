/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TimeZone;

import javax.swing.SwingUtilities;
import edu.stanford.ejalbert.BrowserLauncher;

public abstract class StaticEntity implements KoLConstants
{
	private static KoLSettings settings = new KoLSettings( "" );
	private static final String [] EMPTY_STRING_ARRAY = new String[0];

	protected static KoLmafia client;
	private static int usesSystemTray = 0;
	private static int usesRelayWindows = 0;

	public static final void setClient( KoLmafia client )
	{	StaticEntity.client = client;
	}

	public static KoLmafia getClient()
	{	return client;
	}

	public static boolean usesSystemTray()
	{
		if ( usesSystemTray == 0 )
			usesSystemTray = System.getProperty( "os.name" ).startsWith( "Windows" ) &&
				StaticEntity.getProperty( "useSystemTrayIcon" ).equals( "true" ) ? 1 : 2;

		return usesSystemTray == 1;
	}

	public static boolean usesRelayWindows()
	{
		if ( usesRelayWindows == 0 )
			usesRelayWindows = StaticEntity.getProperty( "useRelayWindows" ).equals( "true" ) ? 1 : 2;

		return usesRelayWindows == 1;
	}

	public static void closeSession()
	{
		if ( client == null )
			return;

		Object [] frames = existingFrames.toArray();

		for ( int i = 0; i < frames.length; ++i )
			((KoLFrame)frames[i]).dispose();

		ConcoctionsDatabase.getConcoctions().clear();
		BuffBotHome.setBuffBotActive( false );
		client.closeMacroStream();

		KoLCharacter.reset( "" );
		(new Thread( new LogoutRequest( client ) )).start();
	}

	public static void reloadSettings()
	{	settings = new KoLSettings( KoLCharacter.getUsername() );
	}

	public static final void setProperty( String name, String value )
	{	settings.setProperty( name, value );
	}

	public static final String getProperty( String name )
	{	return settings.getProperty( name );
	}

	public static void openSystemBrowser( String location )
	{
		try
		{
			BrowserLauncher.openURL( location );
		}
		catch ( java.io.IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Failed to open system browser" );
		}
	}

	/**
	 * A method used to open a new <code>RequestFrame</code> which displays
	 * the given location, relative to the KoL home directory for the current
	 * session.  This should be called whenever <code>RequestFrame</code>s
	 * need to be created in order to keep code modular.
	 */

	public static void openRequestFrame( String location )
	{
		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		RequestFrame requestHolder = null;

		for ( int i = frames.length - 1; i >= 0; --i )
			if ( frames[i].getClass() == RequestFrame.class && ((RequestFrame)frames[i]).hasSideBar() )
				requestHolder = (RequestFrame) frames[i];

		Object [] parameters;
		KoLRequest request = RequestEditorKit.extractRequest( location );

		if ( location.startsWith( "search" ) || location.startsWith( "desc" ) || location.startsWith( "static" ) || location.startsWith( "show" ) )
		{
			parameters = new Object[2];
			parameters[0] = requestHolder;
			parameters[1] = request;
		}
		else if ( requestHolder != null )
		{
			if ( !location.equals( "main.php" ) )
				requestHolder.refresh( request );

			requestHolder.requestFocus();
			return;
		}
		else
		{
			parameters = new Object[1];
			parameters[0] = request;
		}

		(new Thread( new CreateFrameRunnable( RequestFrame.class, parameters ) )).start();
	}

	public static void externalUpdate( String location, String responseText )
	{
		// Keep the client updated of your current equipment and
		// familiars, if you visit the appropriate pages.

		if ( location.startsWith( "inventory.php" ) && location.indexOf( "which=2" ) != -1 )
			EquipmentRequest.parseEquipment( responseText );

		if ( location.indexOf( "familiar.php" ) != -1 )
			FamiliarData.registerFamiliarData( client, responseText );

		if ( location.indexOf( "charsheet.php" ) != -1 )
			CharsheetRequest.parseStatus( responseText );

		// See if the person learned a new skill from using a
		// mini-browser frame.

		Matcher learnedMatcher = Pattern.compile( "<td>You learn a new skill: <b>(.*?)</b>" ).matcher( responseText );
		if ( learnedMatcher.find() )
		{
			KoLCharacter.addAvailableSkill( new UseSkillRequest( client, learnedMatcher.group(1), "", 1 ) );
			KoLCharacter.addDerivedSkills();
			KoLCharacter.refreshCalculatedLists();
		}

		learnedMatcher = Pattern.compile( "You emerge with a (.*?) of Steel" ).matcher( responseText );
		if ( learnedMatcher.find() )
			KoLCharacter.addAvailableSkill( new UseSkillRequest( client, learnedMatcher.group(1) + " of Steel", "", 1 ) );

		// Unfortunately, if you learn a new skill from Frank
		// the Regnaissance Gnome at the Gnomish Gnomads
		// Camp, it doesn't tell you the name of the skill.
		// It simply says: "You leargn a new skill. Whee!"

		if ( responseText.indexOf( "You leargn a new skill." ) != -1 )
			(new CharsheetRequest( client )).run();
	}

	public static final void executeCountdown( String message, int seconds )
	{
		for ( int i = 0; i < seconds && KoLmafia.permitsContinue(); ++i )
		{
			KoLmafia.updateDisplay( message + (seconds - i) + " seconds..." );
			KoLRequest.delay( 1000 );
		}
	}

	public static final void printStackTrace( Throwable t )
	{	printStackTrace( t, "UNEXPECTED ERROR", EMPTY_STRING_ARRAY );
	}


	public static final void printStackTrace( Throwable t, String message )
	{	printStackTrace( t, message, EMPTY_STRING_ARRAY );
	}

	public static final void printStackTrace( Throwable t, String message, String [] logAssistMessages )
	{
		boolean shouldOpenStream = KoLmafia.getDebugStream() instanceof NullStream;

		if ( shouldOpenStream )
			KoLmafia.openDebugStream();

		KoLmafia.updateDisplay( message + ".  Debug log printed." );
		for ( int i = 0; i < logAssistMessages.length; ++i )
		{
			if ( logAssistMessages[i] != null )
			{
				System.out.println( logAssistMessages[i] );
				KoLmafia.getDebugStream().println( logAssistMessages[i] );
			}
		}

		t.printStackTrace( KoLmafia.getDebugStream() );
		t.printStackTrace();

		if ( client.getCurrentRequest() != null )
			KoLmafia.getDebugStream().println( "" + client.getCurrentRequest().responseText );

		if ( shouldOpenStream )
			KoLmafia.closeDebugStream();
	}

	public static final int parseInt( String string )
	{
		if ( string == null )
			return 0;

		String clean = string.replaceAll( "[^\\-0-9]", "" );
		return clean.equals( "" ) ? 0 : Integer.parseInt( clean );
	}

	public static final double parseDouble( String string )
	{
		if ( string == null )
			return 0.0;

		String clean = string.replaceAll( "[^\\-\\.0-9]", "" );
		return clean.equals( "" ) ? 0.0 : Double.parseDouble( clean );
	}
}
