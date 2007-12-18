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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;
import edu.stanford.ejalbert.BrowserLauncher;

public abstract class StaticEntity
	implements KoLConstants
{
	private static final Pattern NONDIGIT_PATTERN = Pattern.compile( "[^\\-0-9]" );
	private static final Pattern NONFLOAT_PATTERN = Pattern.compile( "[^\\-\\.0-9]" );
	private static final Pattern NEWSKILL1_PATTERN = Pattern.compile( "<td>You learn a new skill: <b>(.*?)</b>" );
	private static final Pattern NEWSKILL2_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern NEWSKILL3_PATTERN = Pattern.compile( "<td>You acquire a skill: <[bB]>(.*?)</[bB]>" );

	private static final ArrayList relayCounters = new ArrayList();

	private static KoLmafia client;
	private static int usesSystemTray = 0;
	private static int usesRelayWindows = 0;

	public static final ArrayList existingPanels = new ArrayList();

	private static KoLFrame[] frameArray = new KoLFrame[ 0 ];
	private static ActionPanel[] panelArray = new KoLPanel[ 0 ];

	public static class TurnCounter
	{
		private final int value;
		private final String label, image;

		public TurnCounter( final int value, final String label, final String image )
		{
			this.value = KoLCharacter.getCurrentRun() + value;
			this.label = label;
			this.image = image;
		}

		public boolean isExempt( final String adventureId )
		{
			if ( this.label.equals( "Wormwood" ) )
			{
				return adventureId.equals( "151" ) || adventureId.equals( "152" ) || adventureId.equals( "153" );
			}

			return false;
		}

		public String getLabel()
		{
			return this.label;
		}

		public String getImage()
		{
			return this.image;
		}

		public boolean equals( final Object o )
		{
			if ( o == null || !( o instanceof TurnCounter ) )
			{
				return false;
			}

			return this.label.equals( ( (TurnCounter) o ).label ) && this.value == ( (TurnCounter) o ).value;
		}
	}

	public static final boolean isCounting( final String label )
	{
		TurnCounter current;
		Iterator it = StaticEntity.relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();
			if ( current.label.equals( label ) )
			{
				return true;
			}
		}

		return false;
	}

	public static final boolean isCounting( final String label, final int value )
	{
		TurnCounter current;
		int searchValue = KoLCharacter.getCurrentRun() + value;

		Iterator it = StaticEntity.relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();
			if ( current.label.equals( label ) && current.value == searchValue )
			{
				return true;
			}
		}

		return false;
	}

	public static final void stopCounting( final String label )
	{
		TurnCounter current;
		Iterator it = StaticEntity.relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();
			if ( current.label.equals( label ) )
			{
				it.remove();
			}
		}

		StaticEntity.saveCounters();
	}

	public static final void startCounting( final int value, final String label, final String image )
	{
		if ( value < 0 )
		{
			return;
		}

		TurnCounter counter = new TurnCounter( value, label, image );

		if ( !StaticEntity.relayCounters.contains( counter ) )
		{
			StaticEntity.relayCounters.add( counter );
			StaticEntity.saveCounters();
		}
	}

	public static final String getUnexpiredCounters()
	{
		TurnCounter current;
		int currentTurns = KoLCharacter.getCurrentRun();

		StringBuffer counters = new StringBuffer();
		Iterator it = StaticEntity.relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();

			if ( current.value < currentTurns )
			{
				it.remove();
				continue;
			}

			if ( counters.length() > 0 )
			{
				counters.append( KoLConstants.LINE_BREAK );
			}

			counters.append( current.label );
			counters.append( " (" );
			counters.append( current.value - currentTurns );
			counters.append( ")" );
		}

		return counters.toString();
	}

	public static final TurnCounter getExpiredCounter( final String adventureId )
	{
		TurnCounter current;
		int currentTurns = KoLCharacter.getCurrentRun();

		TurnCounter expired = null;
		Iterator it = StaticEntity.relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();

			if ( current.value > currentTurns )
			{
				continue;
			}

			it.remove();
			if ( current.value == currentTurns && !current.isExempt( adventureId ) )
			{
				expired = current;
			}
		}

		return expired;
	}

	public static final void saveCounters()
	{
		TurnCounter current;
		int currentTurns = KoLCharacter.getCurrentRun();

		StringBuffer counters = new StringBuffer();
		Iterator it = StaticEntity.relayCounters.iterator();

		while ( it.hasNext() )
		{
			current = (TurnCounter) it.next();

			if ( current.value < currentTurns )
			{
				it.remove();
				continue;
			}

			if ( counters.length() > 0 )
			{
				counters.append( ":" );
			}

			counters.append( current.value );
			counters.append( ":" );
			counters.append( current.label );
			counters.append( ":" );
			counters.append( current.image );
		}

		KoLSettings.setUserProperty( "relayCounters", counters.toString() );
	}

	public static final void loadCounters()
	{
		StaticEntity.relayCounters.clear();

		String counters = KoLSettings.getUserProperty( "relayCounters" );
		if ( counters.length() == 0 )
		{
			return;
		}

		StringTokenizer tokens = new StringTokenizer( counters, ":" );
		while ( tokens.hasMoreTokens() )
		{
			StaticEntity.startCounting(
				StaticEntity.parseInt( tokens.nextToken() ) - KoLCharacter.getCurrentRun(), tokens.nextToken(),
				tokens.nextToken() );
		}
	}

	public static final String getVersion()
	{
		if ( KoLConstants.REVISION == null )
		{
			return KoLConstants.VERSION_NAME;
		}

		int colonIndex = KoLConstants.REVISION.indexOf( ":" );
		if ( colonIndex != -1 )
		{
			return "KoLmafia r" + KoLConstants.REVISION.substring( 0, colonIndex );
		}

		if ( KoLConstants.REVISION.endsWith( "M" ) )
		{
			return "KoLmafia r" + KoLConstants.REVISION.substring( 0, KoLConstants.REVISION.length() - 1 );
		}

		return "KoLmafia r" + KoLConstants.REVISION;
	}

	public static final void setClient( final KoLmafia client )
	{
		StaticEntity.client = client;
	}

	public static final KoLmafia getClient()
	{
		return StaticEntity.client;
	}

	public static final void registerFrame( final KoLFrame frame )
	{
		synchronized ( KoLConstants.existingFrames )
		{
			KoLConstants.existingFrames.add( frame );
			StaticEntity.getExistingFrames();
		}
	}

	public static final void unregisterFrame( final KoLFrame frame )
	{
		synchronized ( KoLConstants.existingFrames )
		{
			KoLConstants.existingFrames.remove( frame );
			KoLConstants.removedFrames.remove( frame );
			StaticEntity.getExistingFrames();
		}
	}

	public static final void registerPanel( final ActionPanel frame )
	{
		synchronized ( StaticEntity.existingPanels )
		{
			StaticEntity.existingPanels.add( frame );
			StaticEntity.getExistingPanels();
		}
	}

	public static final void unregisterPanel( final ActionPanel frame )
	{
		synchronized ( StaticEntity.existingPanels )
		{
			StaticEntity.existingPanels.remove( frame );
			StaticEntity.getExistingPanels();
		}
	}

	public static final KoLFrame[] getExistingFrames()
	{
		synchronized ( KoLConstants.existingFrames )
		{
			boolean needsRefresh = StaticEntity.frameArray.length != KoLConstants.existingFrames.size();

			if ( !needsRefresh )
			{
				for ( int i = 0; i < StaticEntity.frameArray.length && !needsRefresh; ++i )
				{
					needsRefresh |= StaticEntity.frameArray[ i ] != KoLConstants.existingFrames.get( i );
				}
			}

			if ( needsRefresh )
			{
				StaticEntity.frameArray = new KoLFrame[ KoLConstants.existingFrames.size() ];
				KoLConstants.existingFrames.toArray( StaticEntity.frameArray );
			}

			return StaticEntity.frameArray;
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
					needsRefresh |= StaticEntity.panelArray[ i ] != StaticEntity.existingPanels.get( i );
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

	public static final boolean usesSystemTray()
	{
		if ( StaticEntity.usesSystemTray == 0 )
		{
			StaticEntity.usesSystemTray =
				System.getProperty( "os.name" ).startsWith( "Windows" ) && KoLSettings.getBooleanProperty( "useSystemTrayIcon" ) ? 1 : 2;
		}

		return StaticEntity.usesSystemTray == 1;
	}

	public static final boolean usesRelayWindows()
	{
		if ( StaticEntity.usesRelayWindows == 0 )
		{
			StaticEntity.usesRelayWindows = KoLSettings.getBooleanProperty( "useRelayWindows" ) ? 1 : 2;
		}

		return StaticEntity.usesRelayWindows == 1;
	}

	public static final void openSystemBrowser( final String location )
	{
		( new SystemBrowserThread( location ) ).start();
	}

	private static class SystemBrowserThread
		extends Thread
	{
		private final String location;

		public SystemBrowserThread( final String location )
		{
			this.location = location;
		}

		public void run()
		{
			String browser = KoLSettings.getUserProperty( "preferredWebBrowser" );
			if ( !browser.equals( "" ) )
			{
				System.setProperty( "os.browser", browser );
			}

			BrowserLauncher.openURL( this.location );
		}
	}

	/**
	 * A method used to open a new <code>RequestFrame</code> which displays the given location, relative to the KoL
	 * home directory for the current session. This should be called whenever <code>RequestFrame</code>s need to be
	 * created in order to keep code modular.
	 */

	public static final void openRequestFrame( final String location )
	{
		KoLRequest request = RequestEditorKit.extractRequest( location );

		if ( location.startsWith( "search" ) || location.startsWith( "desc" ) || location.startsWith( "static" ) || location.startsWith( "show" ) )
		{
			DescriptionFrame.showRequest( request );
			return;
		}

		KoLFrame[] frames = StaticEntity.getExistingFrames();
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
			FightFrame.showRequest( request );
			return;
		}

		if ( !location.equals( "main.php" ) )
		{
			requestHolder.refresh( request );
		}
	}

	public static final void externalUpdate( final String location, final String responseText )
	{
		if ( location.startsWith( "account.php" ) )
		{
			boolean wasHardcore = KoLCharacter.isHardcore();
			boolean hadRestrictions = !KoLCharacter.canEat() || !KoLCharacter.canDrink();

			AccountRequest.parseAccountData( responseText );

			if ( wasHardcore && !KoLCharacter.isHardcore() )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "dropped hardcore" );
				RequestLogger.updateSessionLog();
			}

			if ( hadRestrictions && KoLCharacter.canEat() && KoLCharacter.canDrink() )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "dropped consumption restrictions" );
				RequestLogger.updateSessionLog();
			}
		}

		if ( location.startsWith( "questlog.php" ) )
		{
			QuestLogRequest.registerQuests( true, location, responseText );
		}

		// Keep theupdated of your current equipment and
		// familiars, if you visit the appropriate pages.

		if ( location.startsWith( "inventory.php" ) && location.indexOf( "which=2" ) != -1 )
		{
			EquipmentRequest.parseEquipment( responseText );
		}

		if ( location.indexOf( "familiar.php" ) != -1 )
		{
			FamiliarData.registerFamiliarData( responseText );
		}

		if ( location.indexOf( "charsheet.php" ) != -1 )
		{
			CharsheetRequest.parseStatus( responseText );
		}

		if ( location.startsWith( "sellstuff_ugly.php" ) )
		{
			// New autosell interface.

			// "You sell your 2 disturbing fanfics to an organ
			// grinder's monkey for 264 Meat."

			Matcher matcher = AutoSellRequest.AUTOSELL_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				StaticEntity.client.processResult( new AdventureResult(
					AdventureResult.MEAT, StaticEntity.parseInt( matcher.group( 1 ) ) ) );
			}
		}

		// See if the request would have used up an item.

		if ( location.indexOf( "inventory.php" ) != -1 && location.indexOf( "action=message" ) != -1 )
		{
			ConsumeItemRequest.parseConsumption( responseText, false );
		}
		if ( ( location.indexOf( "multiuse.php" ) != -1 || location.indexOf( "skills.php" ) != -1 ) && location.indexOf( "useitem" ) != -1 )
		{
			ConsumeItemRequest.parseConsumption( responseText, false );
		}
		if ( location.indexOf( "hermit.php" ) != -1 )
		{
			HermitRequest.parseHermitTrade( location, responseText );
		}

		// See if the person learned a new skill from using a
		// mini-browser frame.

		Matcher learnedMatcher = StaticEntity.NEWSKILL1_PATTERN.matcher( responseText );
		if ( learnedMatcher.find() )
		{
			String skillName = learnedMatcher.group( 1 );

			KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( skillName ) );
			KoLCharacter.addDerivedSkills();
			KoLConstants.usableSkills.sort();
		}

		learnedMatcher = StaticEntity.NEWSKILL3_PATTERN.matcher( responseText );
		if ( learnedMatcher.find() )
		{
			String skillName = learnedMatcher.group( 1 );

			KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( skillName ) );
			KoLCharacter.addDerivedSkills();
			KoLConstants.usableSkills.sort();
		}

		// Unfortunately, if you learn a new skill from Frank
		// the Regnaissance Gnome at the Gnomish Gnomads
		// Camp, it doesn't tell you the name of the skill.
		// It simply says: "You leargn a new skill. Whee!"

		if ( responseText.indexOf( "You leargn a new skill." ) != -1 )
		{
			learnedMatcher = StaticEntity.NEWSKILL2_PATTERN.matcher( location );
			if ( learnedMatcher.find() )
			{
				KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( StaticEntity.parseInt( learnedMatcher.group( 1 ) ) ) );
			}
		}

		// Player vs. player results should be recorded to the
		// KoLmafia log.

		if ( location.startsWith( "pvp.php" ) && location.indexOf( "who=" ) != -1 )
		{
			FlowerHunterRequest.processOffenseContests( responseText );
		}

		// If this is the hippy store, check to see if any of the
		// items offered in the hippy store are special.

		if ( location.startsWith( "store.php" ) && location.indexOf( "whichstore=h" ) != -1 && KoLSettings.getIntegerProperty( "lastFilthClearance" ) != KoLCharacter.getAscensions() )
		{
			String side = "none";
			if ( responseText.indexOf( "peach" ) != -1 && responseText.indexOf( "pear" ) != -1 && responseText.indexOf( "plum" ) != -1 )
			{
				KoLSettings.setUserProperty( "lastFilthClearance", String.valueOf( KoLCharacter.getAscensions() ) );
				side = "hippy";
			}
			else if ( responseText.indexOf( "bowl of rye sprouts" ) != -1 && responseText.indexOf( "cob of corn" ) != -1 && responseText.indexOf( "juniper berries" ) != -1 )
			{
				KoLSettings.setUserProperty( "lastFilthClearance", String.valueOf( KoLCharacter.getAscensions() ) );
				side = "fratboy";
			}
			KoLSettings.setUserProperty( "currentHippyStore", side );
			KoLSettings.setUserProperty( "sidequestOrchardCompleted", side );
		}
	}

	public static final boolean executeCountdown( final String message, final int seconds )
	{
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

			RequestThread.waitOneSecond();
		}

		return KoLmafia.permitsContinue();
	}

	public static final void printStackTrace()
	{
		StaticEntity.printStackTrace( "Forced stack trace" );
	}

	public static final void printStackTrace( final String message )
	{
		try
		{
			throw new Exception( message );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public static final void printStackTrace( final Throwable t )
	{
		StaticEntity.printStackTrace( t, "" );
	}

	public static final void printStackTrace( final Throwable t, final String message )
	{
		// Next, print all the information to the debug log so that
		// it can be sent.

		boolean shouldOpenStream = !RequestLogger.isDebugging();
		if ( shouldOpenStream )
		{
			RequestLogger.openDebugLog();
		}

		KoLmafia.updateDisplay( "Unexpected error, debug log printed." );

		StaticEntity.printStackTrace( t, message, System.err );
		StaticEntity.printStackTrace( t, message, RequestLogger.getDebugStream() );

		if ( t.getCause() != null )
		{
			StaticEntity.printStackTrace( t.getCause(), message );
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
			// Okay, since you're in the middle of handling an exception
			// and got a new one, just return from here.
		}
	}

	private static final void printStackTrace( final Throwable t, final String message, final PrintStream ostream )
	{
		ostream.println( t.getClass() + ": " + t.getMessage() );
		t.printStackTrace( ostream );
	}

	public static final void printRequestData( final KoLRequest request )
	{
		if ( request == null )
		{
			return;
		}

		boolean shouldOpenStream = RequestLogger.isDebugging();
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

	public static final int parseInt( final String string )
	{
		if ( string == null )
		{
			return 0;
		}

		String clean = StaticEntity.NONDIGIT_PATTERN.matcher( string ).replaceAll( "" );
		return clean.equals( "" ) || clean.equals( "-" ) ? 0 : Integer.parseInt( clean );
	}

	public static final float parseFloat( final String string )
	{
		if ( string == null )
		{
			return 0.0f;
		}

		String clean = StaticEntity.NONFLOAT_PATTERN.matcher( string ).replaceAll( "" );
		return clean.equals( "" ) ? 0.0f : Float.parseFloat( clean );
	}

	public static final boolean loadLibrary( final File parent, final String directory, final String filename )
	{
		try
		{
			// Next, load the icon which will be used by KoLmafia
			// in the system tray.  For now, this will be the old
			// icon used by KoLmelion.

			parent.mkdirs();
			File library = new File( parent, filename );

			if ( library.exists() )
			{
				if ( parent == KoLConstants.RELAY_LOCATION && !KoLSettings.getUserProperty( "lastRelayUpdate" ).equals(
					StaticEntity.getVersion() ) )
				{
					library.delete();
				}
				else
				{
					return true;
				}
			}

			InputStream input = DataUtilities.getInputStream( directory, filename );
			if ( input == null )
			{
				return false;
			}

			library.createNewFile();
			OutputStream output = new FileOutputStream( library );

			byte[] buffer = new byte[ 1024 ];
			int bufferLength;
			while ( ( bufferLength = input.read( buffer ) ) != -1 )
			{
				output.write( buffer, 0, bufferLength );
			}

			input.close();
			output.close();
			return true;

		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	public static final String singleStringDelete( final String originalString, final String searchString )
	{
		return StaticEntity.singleStringReplace( originalString, searchString, "" );
	}

	public static final String singleStringReplace( final String originalString, final String searchString,
		final String replaceString )
	{
		// Using a regular expression, while faster, results
		// in a lot of String allocation overhead.  So, use
		// a static finalally-allocated StringBuffers.

		int lastIndex = originalString.indexOf( searchString );
		if ( lastIndex == -1 )
		{
			return originalString;
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append( originalString.substring( 0, lastIndex ) );
		buffer.append( replaceString );
		buffer.append( originalString.substring( lastIndex + searchString.length() ) );
		return buffer.toString();
	}

	public static final void singleStringDelete( final StringBuffer buffer, final String searchString )
	{
		StaticEntity.singleStringReplace( buffer, searchString, "" );
	}

	public static final void singleStringReplace( final StringBuffer buffer, final String searchString,
		final String replaceString )
	{
		int index = buffer.indexOf( searchString );
		if ( index != -1 )
		{
			buffer.replace( index, index + searchString.length(), replaceString );
		}
	}

	public static final String globalStringDelete( final String originalString, final String searchString )
	{
		return StaticEntity.globalStringReplace( originalString, searchString, "" );
	}

	public static final String globalStringReplace( final String originalString, final String searchString,
		final String replaceString )
	{
		// Using a regular expression, while faster, results
		// in a lot of String allocation overhead.  So, use
		// a static finalally-allocated StringBuffers.

		int lastIndex = originalString.indexOf( searchString );
		if ( lastIndex == -1 )
		{
			return originalString;
		}

		StringBuffer buffer = new StringBuffer( originalString );
		while ( lastIndex != -1 )
		{
			buffer.replace( lastIndex, lastIndex + searchString.length(), replaceString );
			lastIndex = buffer.indexOf( searchString, lastIndex + replaceString.length() );
		}

		return buffer.toString();
	}

	public static final void globalStringReplace( final StringBuffer buffer, final String tag, final int replaceWith )
	{
		StaticEntity.globalStringReplace( buffer, tag, String.valueOf( replaceWith ) );
	}

	public static final void globalStringDelete( final StringBuffer buffer, final String tag )
	{
		StaticEntity.globalStringReplace( buffer, tag, "" );
	}

	public static final void globalStringReplace( final StringBuffer buffer, final String tag, String replaceWith )
	{
		if ( replaceWith == null )
		{
			replaceWith = "";
		}

		// Using a regular expression, while faster, results
		// in a lot of String allocation overhead.  So, use
		// a static finalally-allocated StringBuffers.

		int lastIndex = buffer.indexOf( tag );
		while ( lastIndex != -1 )
		{
			buffer.replace( lastIndex, lastIndex + tag.length(), replaceWith );
			lastIndex = buffer.indexOf( tag, lastIndex + replaceWith.length() );
		}
	}

	public static final String[] getPastUserList()
	{
		Matcher pathMatcher = null;
		ArrayList pastUserList = new ArrayList();

		if ( !UtilityConstants.SETTINGS_LOCATION.exists() )
		{
			return new String[ 0 ];
		}

		String user;
		File[] files = UtilityConstants.SETTINGS_LOCATION.listFiles();

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
