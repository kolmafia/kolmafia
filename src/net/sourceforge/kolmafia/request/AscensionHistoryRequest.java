/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.io.BufferedReader;
import java.io.File;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.AscensionSnapshot;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AscensionHistoryRequest
	extends GenericRequest
	implements Comparable<AscensionHistoryRequest>
{
	private static int typeComparator = AscensionSnapshot.NORMAL;

	private static final SimpleDateFormat ASCEND_DATE_FORMAT = new SimpleDateFormat( "MM/dd/yy", Locale.US );
	private static final Pattern FIELD_PATTERN = Pattern.compile( "</tr><td class=small.*?</tr>" );
	private static final Pattern NAME_PATTERN = Pattern.compile( "who=(\\d+)\\\" class=nounder><font color=white>(.*?)</font>" );

	private final String playerName;
	private final String playerId;
	private final List<AscensionDataField> ascensionData;
	private int hardcoreCount, softcoreCount, casualCount;

	public AscensionHistoryRequest( final String playerName, final String playerId )
	{
		super( "ascensionhistory.php" );

		this.addFormField( "back", "self" );
		this.addFormField( "who", ContactManager.getPlayerId( playerName ) );

		this.playerName = playerName;
		this.playerId = playerId;

		this.ascensionData = new ArrayList<AscensionDataField>();
	}

	public static final void setComparator( final int typeComparator )
	{
		AscensionHistoryRequest.typeComparator = typeComparator;
	}

	@Override
	public String toString()
	{
		StringBuilder stringForm = new StringBuilder();
		stringForm.append( "<tr><td><a href=\"ascensions/" + ClanManager.getURLName( this.playerName ) + "\"><b>" );

		String name = ContactManager.getPlayerName( this.playerId );
		stringForm.append( name.equals( this.playerId ) ? this.playerName : name );

		stringForm.append( "</b></a></td>" );
		stringForm.append( "<td align=right>" );
		stringForm.append( typeComparator == AscensionSnapshot.NORMAL ? this.softcoreCount : typeComparator == AscensionSnapshot.HARDCORE ? this.hardcoreCount : casualCount );
		stringForm.append( "</td></tr>" );
		return stringForm.toString();
	}

	public int compareTo( final AscensionHistoryRequest o )
	{
		return o == null || !( o instanceof AscensionHistoryRequest ) ? -1 : typeComparator == AscensionSnapshot.NORMAL ? ( (AscensionHistoryRequest) o ).softcoreCount - this.softcoreCount : typeComparator == AscensionSnapshot.HARDCORE ? ( (AscensionHistoryRequest) o ).hardcoreCount - this.hardcoreCount : ( (AscensionHistoryRequest) o ).casualCount - this.casualCount;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	public static final void parseResponse( final String urlString, String responseText )
	{
		if ( responseText == null || responseText.length() == 0 || !urlString.startsWith( "ascensionhistory.php" ) )
		{
			return;
		}

		int borisPoints = 0;
		int zombiePoints = 0;
		int jarlsbergPoints = 0;
		int petePoints = 0;
		int edPoints = 0;
		int cowPuncherPoints = 0;
		int beanSlingerPoints = 0;
		int snakeOilerPoints = 0;
		int sourcePoints = 0;
		String playerName = null;
		String playerId = null;

		// Add something into familiar column if blank so later processing works
		responseText = 	responseText.replaceAll( "<a[^>]*?>Back[^<?]</a>", "" ).replaceAll( "<td></td>",
					     "<td><img src=\"" + KoLmafia.imageServerPath() + "itemimages/confused.gif\" height=30 width=30></td>" );

		Matcher nameMatcher = AscensionHistoryRequest.NAME_PATTERN.matcher( responseText );
		if ( nameMatcher.find() )
		{
			playerName = nameMatcher.group( 2 );
			playerId = nameMatcher.group( 1 );
		}

		// Only continue if looking at ourself
		if ( playerId == null || !playerId.equals( KoLCharacter.getPlayerId() ) )
		{
			return;
		}

		Matcher fieldMatcher = AscensionHistoryRequest.FIELD_PATTERN.matcher( responseText );

		int lastFindIndex = 0;
		AscensionDataField lastField;

		while ( fieldMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = fieldMatcher.end() - 5;

			String[] columns = AscensionHistoryRequest.extractColumns( fieldMatcher.group() );

			if ( columns == null )
			{
				continue;
			}

			lastField = new AscensionDataField( playerName, playerId, columns );

			switch ( lastField.pathId )
			{
			case AscensionSnapshot.AVATAR_OF_BORIS:
				borisPoints += lastField.typeId == AscensionSnapshot.HARDCORE ? 2 : 1;
				break;
			case AscensionSnapshot.ZOMBIE_SLAYER:
				zombiePoints += lastField.typeId == AscensionSnapshot.HARDCORE ? 2 : 1;
				break;
			case AscensionSnapshot.AVATAR_OF_JARLSBERG:
				jarlsbergPoints += lastField.typeId == AscensionSnapshot.HARDCORE ? 2 : 1;
				break;
			case AscensionSnapshot.AVATAR_OF_SNEAKY_PETE:
				petePoints += lastField.typeId == AscensionSnapshot.HARDCORE ? 2 : 1;
				break;
			case AscensionSnapshot.ACTUALLY_ED_THE_UNDYING:
				edPoints += lastField.typeId == AscensionSnapshot.HARDCORE ? 2 : 1;
				break;
			case AscensionSnapshot.AVATAR_OF_WEST_OF_LOATHING:
				switch ( lastField.classId )
				{
				case AscensionSnapshot.COW_PUNCHER:
					cowPuncherPoints += lastField.typeId == AscensionSnapshot.HARDCORE ? 2 : 1;
					break;
				case AscensionSnapshot.BEAN_SLINGER:
					beanSlingerPoints += lastField.typeId == AscensionSnapshot.HARDCORE ? 2 : 1;
					break;
				case AscensionSnapshot.SNAKE_OILER:
					snakeOilerPoints += lastField.typeId == AscensionSnapshot.HARDCORE ? 2 : 1;
					break;
				}
				break;
			case AscensionSnapshot.THE_SOURCE:
				sourcePoints += lastField.typeId == AscensionSnapshot.HARDCORE ? 2 : 1;
				break;
			}
		}

		// Refresh points totals based on ascension history
		Preferences.setInteger( "borisPoints", borisPoints );
		Preferences.setInteger( "zombiePoints", zombiePoints );
		Preferences.setInteger( "awolPointsCowpuncher", cowPuncherPoints );
		Preferences.setInteger( "awolPointsBeanslinger", beanSlingerPoints );
		Preferences.setInteger( "awolPointsSnakeoiler", snakeOilerPoints );
		Preferences.setInteger( "sourcePoints", sourcePoints );

		// Some can be increased by buying points, so only set these if higher than preference
		if ( jarlsbergPoints > Preferences.getInteger( "jarlsbergPoints" ) )
		{
			Preferences.setInteger( "jarlsbergPoints", jarlsbergPoints );
		}
		if ( petePoints > Preferences.getInteger( "sneakyPetePoints" ) )
		{
			Preferences.setInteger( "sneakyPetePoints", petePoints );
		}
		if ( edPoints > Preferences.getInteger( "edPoints" ) )
		{
			Preferences.setInteger( "edPoints", edPoints );
		}
	}

	@Override
	public void processResults()
	{
		this.responseText =
			this.responseText.replaceAll( "<a[^>]*?>Back[^<?]</a>", "" )
			.replaceAll( "<td></td>",
				     "<td><img src=\"" + KoLmafia.imageServerPath() + "itemimages/confused.gif\" height=30 width=30></td>" );

		this.refreshFields();
	}

	private String getBackupFileData()
	{
		File clan = new File( KoLConstants.ROOT_LOCATION, "clan" );
		if ( !clan.exists() )
		{
			return "";
		}

		File[] resultFolders = DataUtilities.listFiles( clan );

		File backupFile = null;
		int bestMonth = 0, bestWeek = 0;
		int currentMonth, currentWeek;

		for ( int i = 0; i < resultFolders.length; ++i )
		{
			if ( !resultFolders[ i ].isDirectory() )
			{
				continue;
			}

			File[] ascensionFolders = DataUtilities.listFiles( resultFolders[ i ] );

			for ( int j = 0; j < ascensionFolders.length; ++j )
			{
				if ( !ascensionFolders[ j ].getName().startsWith( "2005" ) )
				{
					continue;
				}

				currentMonth = StringUtilities.parseInt( ascensionFolders[ j ].getName().substring( 4, 6 ) );
				currentWeek = StringUtilities.parseInt( ascensionFolders[ j ].getName().substring( 8, 9 ) );

				boolean shouldReplace = false;

				shouldReplace = currentMonth > bestMonth;

				if ( !shouldReplace )
				{
					shouldReplace = currentMonth == bestMonth && currentWeek > bestWeek;
				}

				if ( shouldReplace )
				{
					shouldReplace = currentMonth == 9 || currentMonth == 10;
				}

				if ( shouldReplace )
				{
					File checkFile = new File( ascensionFolders[ j ], "ascensions/" + this.playerId + ".htm" );
					if ( checkFile.exists() )
					{
						backupFile = checkFile;
						bestMonth = currentMonth;
						bestWeek = currentWeek;
					}
				}
			}
		}

		if ( backupFile == null )
		{
			return "";
		}

		try
		{
			BufferedReader istream = FileUtilities.getReader( backupFile );
			StringBuilder ascensionBuffer = new StringBuilder();
			String currentLine;

			while ( ( currentLine = istream.readLine() ) != null )
			{
				ascensionBuffer.append( currentLine );
				ascensionBuffer.append( KoLConstants.LINE_BREAK );
			}

			return ascensionBuffer.toString();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return "";
		}
	}

	/**
	 * Internal method used to refresh the fields of the profile request based on the response text. This should be
	 * called after the response text is already retrieved.
	 */

	private void refreshFields()
	{
		if ( this.responseText == null || this.responseText.length() == 0 )
		{
			return;
		}
		
		this.ascensionData.clear();
		Matcher fieldMatcher = AscensionHistoryRequest.FIELD_PATTERN.matcher( this.responseText );

		StringBuffer ascensionBuffer = new StringBuffer();
		ascensionBuffer.append( this.getBackupFileData() );

		int lastFindIndex = 0;
		AscensionDataField lastField;

		if ( ascensionBuffer.length() != 0 )
		{
			int oldFindIndex = 0;
			boolean inconsistency = false;
			boolean newDataAvailable = true;
			String[] columnsNew = null;

			Matcher oldDataMatcher = AscensionHistoryRequest.FIELD_PATTERN.matcher( ascensionBuffer );
			if ( !fieldMatcher.find( lastFindIndex ) )
			{
				newDataAvailable = false;
			}
			else
			{
				lastFindIndex = fieldMatcher.end() - 5;
				columnsNew = AscensionHistoryRequest.extractColumns( fieldMatcher.group() );
			}

			while ( oldDataMatcher.find( oldFindIndex ) )
			{
				oldFindIndex = oldDataMatcher.end() - 5;

				String[] columnsOld = AscensionHistoryRequest.extractColumns( oldDataMatcher.group() );
				if ( !newDataAvailable )
				{
					lastField = new AscensionDataField( this.playerName, this.playerId, columnsOld );
					this.ascensionData.add( lastField );

					switch ( lastField.typeId )
					{
					case AscensionSnapshot.NORMAL:
						++this.softcoreCount;
						break;
					case AscensionSnapshot.HARDCORE:
						++this.hardcoreCount;
						break;
					case AscensionSnapshot.CASUAL:
						++this.casualCount;
						break;
					}
				}

				else if ( columnsNew != null && columnsNew[ 0 ].equals( columnsOld[ 0 ] ) )
				{
					if ( !fieldMatcher.find( lastFindIndex ) )
					{
						newDataAvailable = false;
					}
					else
					{
						lastFindIndex = fieldMatcher.end() - 5;
						columnsNew = AscensionHistoryRequest.extractColumns( fieldMatcher.group() );
					}

					lastField = new AscensionDataField( this.playerName, this.playerId, columnsOld );
					this.ascensionData.add( lastField );

					switch ( lastField.typeId )
					{
					case AscensionSnapshot.NORMAL:
						++this.softcoreCount;
						break;
					case AscensionSnapshot.HARDCORE:
						++this.hardcoreCount;
						break;
					case AscensionSnapshot.CASUAL:
						++this.casualCount;
						break;
					}
				}
				else
				{
					lastField = new AscensionDataField( this.playerName, this.playerId, columnsOld );
					this.ascensionData.add( lastField );

					switch ( lastField.typeId )
					{
					case AscensionSnapshot.NORMAL:
						++this.softcoreCount;
						break;
					case AscensionSnapshot.HARDCORE:
						++this.hardcoreCount;
						break;
					case AscensionSnapshot.CASUAL:
						++this.casualCount;
						break;
					}

					try
					{
						// Subtract columns[turns] from columnsNew[turns];
						// currently, this is [5]

						inconsistency = true;
						columnsNew[ 5 ] =
							String.valueOf( StringUtilities.parseInt( columnsNew[ 5 ] ) - StringUtilities.parseInt( columnsOld[ 5 ] ) );

						// Subtract columns[days] from columnsNew[days];
						// currently, this is [6].  Ascensions count
						// both first day and last day, so remember to
						// add it back in.

						long timeDifference =
							AscensionHistoryRequest.ASCEND_DATE_FORMAT.parse( columnsNew[ 1 ] ).getTime() - AscensionHistoryRequest.ASCEND_DATE_FORMAT.parse(
								columnsOld[ 1 ] ).getTime();

						columnsNew[ 6 ] = String.valueOf( Math.round( timeDifference / 86400000L ) + 1 );
					}
					catch ( Exception e )
					{
						// This should not happen.  Therefore, print
						// a stack trace for debug purposes.

						StaticEntity.printStackTrace( e );
					}
				}
			}

			if ( inconsistency )
			{
				lastField = new AscensionDataField( this.playerName, this.playerId, columnsNew );
				this.ascensionData.add( lastField );

				switch ( lastField.typeId )
				{
				case AscensionSnapshot.NORMAL:
					++this.softcoreCount;
					break;
				case AscensionSnapshot.HARDCORE:
					++this.hardcoreCount;
					break;
				case AscensionSnapshot.CASUAL:
					++this.casualCount;
					break;
				}

				lastFindIndex = fieldMatcher.end() - 5;
			}
		}

		while ( fieldMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = fieldMatcher.end() - 5;

			String[] columns = AscensionHistoryRequest.extractColumns( fieldMatcher.group() );

			if ( columns == null )
			{
				continue;
			}

			lastField = new AscensionDataField( this.playerName, this.playerId, columns );
			this.ascensionData.add( lastField );

			switch ( lastField.typeId )
			{
			case AscensionSnapshot.NORMAL:
				++this.softcoreCount;
				break;
			case AscensionSnapshot.HARDCORE:
				++this.hardcoreCount;
				break;
			case AscensionSnapshot.CASUAL:
				++this.casualCount;
				break;
			}
		}
	}

	/**
	 * static final method used by the clan manager in order to get an instance of a profile request based on the data
	 * already known.
	 */

	public static final AscensionHistoryRequest getInstance( final String playerName, final String playerId,
		final String responseText )
	{
		AscensionHistoryRequest instance = new AscensionHistoryRequest( playerName, playerId );

		instance.responseText = responseText;
		instance.refreshFields();

		return instance;
	}

	public String getPlayerName()
	{
		return this.playerName;
	}

	public String getPlayerId()
	{
		return this.playerId;
	}

	public void initialize()
	{
		if ( this.responseText == null )
		{
			RequestThread.postRequest( this );
		}
	}

	public List getAscensionData()
	{
		return this.ascensionData;
	}

	private static final String[] extractColumns( String rowData )
	{
		rowData = rowData.replaceFirst( "</tr><td.*?>", "" );

		rowData = StringUtilities.globalStringDelete( rowData, "&nbsp;" );
		rowData = StringUtilities.globalStringDelete( rowData, " " );

		String[] columns = rowData.split( "(</?t[rd].*?>)+" );

		if ( columns.length < 7 )
		{
			return null;
		}

		// These three columns now have text that would mess up parsing.

		columns[ 2 ] = KoLConstants.ANYTAG_PATTERN.matcher( columns[ 2 ] ).replaceAll( "" );
		columns[ 5 ] = KoLConstants.ANYTAG_PATTERN.matcher( columns[ 5 ] ).replaceAll( "" );
		columns[ 6 ] = KoLConstants.ANYTAG_PATTERN.matcher( columns[ 6 ] ).replaceAll( "" );
		
		return columns;
	}

	public static class AscensionDataField
		implements Comparable<AscensionDataField>
	{
		private String playerName;
		private String playerId;
		private StringBuffer stringForm;

		private Date timestamp;
		private int level, classId, pathId, typeId;
		private int dayCount, turnCount;

		public AscensionDataField( final String playerName, final String playerId, final String rowData )
		{
			this.setData( playerName, playerId, AscensionHistoryRequest.extractColumns( rowData ) );
		}

		public AscensionDataField( final String playerName, final String playerId, final String[] columns )
		{
			this.setData( playerName, playerId, columns );
		}

		private void setData( final String playerName, final String playerId, final String[] columns )
		{
			this.playerId = playerId;
			this.playerName = ContactManager.getPlayerName( playerId );

			if ( this.playerName.equals( this.playerId ) )
			{
				this.playerName = playerName;
			}

			// The level at which the ascension took place is found
			// in the third column, or index 2 in the array.

			try
			{
				this.timestamp = AscensionHistoryRequest.ASCEND_DATE_FORMAT.parse( columns[ 1 ] );
				this.level = StringUtilities.parseInt( columns[ 2 ] );
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}

			this.turnCount = StringUtilities.parseInt( columns[ 5 ] );
			this.dayCount = StringUtilities.parseInt( columns[ 6 ] );

			if ( columns.length == 9 )
			{
				this.setCurrentColumns( columns );
			}
			else
			{
				this.setHistoricColumns( columns );
			}

			this.stringForm = new StringBuffer();
			this.stringForm.append( "<tr><td><a href=\"ascensions/" ).append( ClanManager.getURLName( this.playerName ) ).append( "\"><b>" );
			this.stringForm.append( this.playerName );
			this.stringForm.append( "</b></a>&nbsp;(" );

			switch ( this.classId )
			{
			case AscensionSnapshot.SEAL_CLUBBER:
				this.stringForm.append( "SC" );
				break;

			case AscensionSnapshot.TURTLE_TAMER:
				this.stringForm.append( "TT" );
				break;

			case AscensionSnapshot.PASTAMANCER:
				this.stringForm.append( "PM" );
				break;

			case AscensionSnapshot.SAUCEROR:
				this.stringForm.append( "SA" );
				break;

			case AscensionSnapshot.DISCO_BANDIT:
				this.stringForm.append( "DB" );
				break;

			case AscensionSnapshot.ACCORDION_THIEF:
				this.stringForm.append( "AT" );
				break;

			case AscensionSnapshot.BORIS:
				this.stringForm.append( "B" );
				break;

			case AscensionSnapshot.ZOMBIE_MASTER:
				this.stringForm.append( "ZM" );
				break;

			case AscensionSnapshot.JARLSBERG:
				this.stringForm.append( "J" );
				break;

			case AscensionSnapshot.SNEAKY_PETE:
				this.stringForm.append( "SP" );
				break;

			case AscensionSnapshot.ED:
				this.stringForm.append( "E" );
				break;

			case AscensionSnapshot.COW_PUNCHER:
				this.stringForm.append( "CP" );
				break;

			case AscensionSnapshot.BEAN_SLINGER:
				this.stringForm.append( "BS" );
				break;

			case AscensionSnapshot.SNAKE_OILER:
				this.stringForm.append( "SO" );
				break;
			}

			this.stringForm.append( ")&nbsp;&nbsp;&nbsp;&nbsp;</td><td align=right>" );
			this.stringForm.append( this.dayCount );
			this.stringForm.append( "</td><td align=right>" );
			this.stringForm.append( this.turnCount );
			this.stringForm.append( "</td></tr>" );
		}

		private void setHistoricColumns( final String[] columns )
		{
			// Check if any data present
			if ( !columns[ 7 ].contains( "," ) )
			{
				return;
			}

			this.classId =
				columns[ 3 ].startsWith( "SC" ) ? AscensionSnapshot.SEAL_CLUBBER :
				columns[ 3 ].startsWith( "TT" ) ? AscensionSnapshot.TURTLE_TAMER :
				columns[ 3 ].startsWith( "PM" ) ? AscensionSnapshot.PASTAMANCER :
				columns[ 3 ].startsWith( "SA" ) ? AscensionSnapshot.SAUCEROR :
				columns[ 3 ].startsWith( "DB" ) ? AscensionSnapshot.DISCO_BANDIT :
				columns[ 3 ].startsWith( "AT" ) ? AscensionSnapshot.ACCORDION_THIEF :
				columns[ 3 ].startsWith( "B" ) ? AscensionSnapshot.BORIS :
				columns[ 3 ].startsWith( "ZM" ) ? AscensionSnapshot.ZOMBIE_MASTER :
				columns[ 3 ].startsWith( "J" ) ? AscensionSnapshot.JARLSBERG :
				columns[ 3 ].startsWith( "SP" ) ? AscensionSnapshot.SNEAKY_PETE :
				columns[ 3 ].startsWith( "E" ) ? AscensionSnapshot.ED :
				columns[ 3 ].startsWith( "CP" ) ? AscensionSnapshot.COW_PUNCHER :
				columns[ 3 ].startsWith( "BS" ) ? AscensionSnapshot.BEAN_SLINGER :
				columns[ 3 ].startsWith( "SO" ) ? AscensionSnapshot.SNAKE_OILER :
				AscensionSnapshot.UNKNOWN_CLASS;

			String[] path = columns[ 7 ].split( "," );

			this.typeId = path[ 0 ].equals( "Normal" ) ? AscensionSnapshot.NORMAL :
						path[ 0 ].equals( "Hardcore" ) ? AscensionSnapshot.HARDCORE :
						path[ 0 ].equals( "Casual" ) ? AscensionSnapshot.CASUAL :
						AscensionSnapshot.UNKNOWN_TYPE;

			this.pathId =
				path[ 1 ].equals( "No Path" ) ? AscensionSnapshot.NOPATH :
				path[ 1 ].equals( "Teetotaler" ) ? AscensionSnapshot.TEETOTALER :
				path[ 1 ].equals( "Boozetafarian" ) ? AscensionSnapshot.BOOZETAFARIAN :
				path[ 1 ].equals( "Oxygenarian" ) ? AscensionSnapshot.OXYGENARIAN :
				path[ 1 ].equals( "Bad Moon" ) ? AscensionSnapshot.BAD_MOON :
				path[ 1 ].equals( "Bees Hate You" ) ? AscensionSnapshot.BEES_HATE_YOU :
				path[ 1 ].equals( "Way of the Surprising Fist" ) ? AscensionSnapshot.SURPRISING_FIST :
				path[ 1 ].equals( "Trendy" ) ? AscensionSnapshot.TRENDY :
				path[ 1 ].equals( "Avatar of Boris" ) ? AscensionSnapshot.AVATAR_OF_BORIS :
				path[ 1 ].equals( "Bugbear Invasion" ) ? AscensionSnapshot.BUGBEAR_INVASION :
				path[ 1 ].equals( "Zombie Slayer" ) ? AscensionSnapshot.ZOMBIE_SLAYER :
				path[ 1 ].equals( "Class Act" ) ? AscensionSnapshot.CLASS_ACT :
				path[ 1 ].equals( "Avatar of Jarlsberg" ) ? AscensionSnapshot.AVATAR_OF_JARLSBERG  :
				path[ 1 ].equals( "BIG!" ) ? AscensionSnapshot.BIG :
				path[ 1 ].equals( "KOLHS" ) ? AscensionSnapshot.KOLHS :
				path[ 1 ].equals( "Class Act II: A Class For Pigs" ) ? AscensionSnapshot.CLASS_ACT_II :
				path[ 1 ].equals( "Avatar of Sneaky Pete" ) ? AscensionSnapshot.AVATAR_OF_SNEAKY_PETE :
				path[ 1 ].equals( "Slow and Steady" ) ? AscensionSnapshot.SLOW_AND_STEADY :
				path[ 1 ].equals( "Heavy Rains" ) ? AscensionSnapshot.HEAVY_RAINS :
				path[ 1 ].equals( "Picky" ) ? AscensionSnapshot.PICKY :
				path[ 1 ].equals( "Standard" ) ? AscensionSnapshot.STANDARD :
				path[ 1 ].equals( "Actually Ed the Undying" ) ? AscensionSnapshot.ACTUALLY_ED_THE_UNDYING :
				path[ 1 ].equals( "One Crazy Random Summer" ) ? AscensionSnapshot.CRAZY_RANDOM_SUMMER :
				path[ 1 ].equals( "Community Service" ) ? AscensionSnapshot.COMMUNITY_SERVICE :
				path[ 1 ].equals( "Avatar of West of Loathing" ) ? AscensionSnapshot.AVATAR_OF_WEST_OF_LOATHING :
				path[ 1 ].equals( "The Source" ) ? AscensionSnapshot.THE_SOURCE :
				AscensionSnapshot.UNKNOWN_PATH;
		}

		private void setCurrentColumns( final String[] columns )
		{
			try
			{
				this.classId =
					columns[ 3 ].contains( "club" ) ? AscensionSnapshot.SEAL_CLUBBER :
					columns[ 3 ].contains( "turtle" ) ? AscensionSnapshot.TURTLE_TAMER :
					columns[ 3 ].contains( "pasta" ) ? AscensionSnapshot.PASTAMANCER :
					columns[ 3 ].contains( "sauce" ) ? AscensionSnapshot.SAUCEROR :
					columns[ 3 ].contains( "disco" ) ? AscensionSnapshot.DISCO_BANDIT :
					columns[ 3 ].contains( "accordion" ) ? AscensionSnapshot.ACCORDION_THIEF :
					columns[ 3 ].contains( "trusty" ) ? AscensionSnapshot.BORIS :
					columns[ 3 ].contains( "tombstone" ) ? AscensionSnapshot.ZOMBIE_MASTER :
					columns[ 3 ].contains( "path12icon" ) ? AscensionSnapshot.JARLSBERG :
					columns[ 3 ].contains( "bigglasses" ) ? AscensionSnapshot.SNEAKY_PETE :
					columns[ 3 ].contains( "thoth" ) ? AscensionSnapshot.ED :
					columns[ 3 ].contains( "darkcow" ) ? AscensionSnapshot.COW_PUNCHER :
					columns[ 3 ].contains( "beancan" ) ? AscensionSnapshot.BEAN_SLINGER :
					columns[ 3 ].contains( "tinysnake" ) ? AscensionSnapshot.SNAKE_OILER :
					AscensionSnapshot.UNKNOWN_CLASS;

				this.typeId = columns[ 8 ].contains( "hardcore" ) ? AscensionSnapshot.HARDCORE :
							columns[ 8 ].contains( "beanbag" ) ? AscensionSnapshot.CASUAL :
							AscensionSnapshot.NORMAL;

				this.pathId =
					columns[ 8 ].contains( "bowl" ) ? AscensionSnapshot.TEETOTALER :
					columns[ 8 ].contains( "martini" ) ? AscensionSnapshot.BOOZETAFARIAN :
					columns[ 8 ].contains( "oxy" ) ? AscensionSnapshot.OXYGENARIAN :
					columns[ 8 ].contains( "badmoon" ) ? AscensionSnapshot.BAD_MOON :
					columns[ 8 ].contains( "beeicon" ) ? AscensionSnapshot.BEES_HATE_YOU :
					columns[ 8 ].contains( "wosp_fist" ) ? AscensionSnapshot.SURPRISING_FIST :
					columns[ 8 ].contains( "trendyicon" ) ? AscensionSnapshot.TRENDY :
					columns[ 8 ].contains( "trusty" ) ? AscensionSnapshot.AVATAR_OF_BORIS :
					columns[ 8 ].contains( "familiar39" ) ? AscensionSnapshot.BUGBEAR_INVASION :
					columns[ 8 ].contains( "tombstone" ) ? AscensionSnapshot.ZOMBIE_SLAYER :
					columns[ 8 ].contains( "motorboat." ) ? AscensionSnapshot.CLASS_ACT :
					columns[ 8 ].contains( "jarlhat" ) ? AscensionSnapshot.AVATAR_OF_JARLSBERG :
					columns[ 8 ].contains( "bigicon" ) ? AscensionSnapshot.BIG :
					columns[ 8 ].contains( "kolhsicon" ) ? AscensionSnapshot.KOLHS :
					columns[ 8 ].contains( "motorboat2" ) ? AscensionSnapshot.CLASS_ACT_II :
					columns[ 8 ].contains( "bigglasses" ) ? AscensionSnapshot.AVATAR_OF_SNEAKY_PETE :
					columns[ 8 ].contains( "sas" ) ? AscensionSnapshot.SLOW_AND_STEADY :
					columns[ 8 ].contains( "familiar31" ) ? AscensionSnapshot.HEAVY_RAINS :
					columns[ 8 ].contains( "pickypath" ) ? AscensionSnapshot.PICKY :
					columns[ 8 ].contains( "standardicon" ) ? AscensionSnapshot.STANDARD :
					columns[ 8 ].contains( "scarab" ) ? AscensionSnapshot.ACTUALLY_ED_THE_UNDYING :
					columns[ 8 ].contains( "dice" ) ? AscensionSnapshot.CRAZY_RANDOM_SUMMER :
					columns[ 8 ].contains( "csplaquesmall" ) ? AscensionSnapshot.COMMUNITY_SERVICE :
					columns[ 8 ].contains( "badge" ) ? AscensionSnapshot.AVATAR_OF_WEST_OF_LOATHING :
					columns[ 8 ].contains( "ss_datasiphon" ) ? AscensionSnapshot.THE_SOURCE :
					AscensionSnapshot.NOPATH;
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}

		public String getDateAsString()
		{
			return ProfileRequest.OUTPUT_FORMAT.format( this.timestamp );
		}

		public int getAge()
		{
			long ascensionDate = this.timestamp.getTime();
			float difference = System.currentTimeMillis() - ascensionDate;
			int days = Math.round( ( difference / ( 1000 * 60 * 60 * 24 ) ) );
			return days;
		}

		@Override
		public String toString()
		{
			return this.stringForm.toString();
		}

		@Override
		public boolean equals( final Object o )
		{
			return o != null && o instanceof AscensionDataField && this.playerId.equals( ( (AscensionDataField) o ).playerId );
		}

		@Override
		public int hashCode()
		{
			return this.playerId != null ? this.playerId.hashCode() : 0;
		}

		public boolean matchesFilter( final int typeFilter, final int pathFilter, final int classFilter,
			final int maxAge )
		{
			return ( typeFilter == AscensionSnapshot.NO_FILTER || typeFilter == this.typeId ) && ( pathFilter == AscensionSnapshot.NO_FILTER || pathFilter == this.pathId ) && ( classFilter == AscensionSnapshot.NO_FILTER || classFilter == this.classId ) && ( maxAge == 0 || maxAge >= this.getAge() );
		}

		public boolean matchesFilter( final int typeFilter, final int pathFilter, final int classFilter )
		{
			return ( typeFilter == AscensionSnapshot.NO_FILTER || typeFilter == this.typeId ) && ( pathFilter == AscensionSnapshot.NO_FILTER || pathFilter == this.pathId ) && ( classFilter == AscensionSnapshot.NO_FILTER || classFilter == this.classId );
		}

		public int compareTo( final AscensionDataField o )
		{
			if ( o == null || !( o instanceof AscensionDataField ) )
			{
				return -1;
			}

			AscensionDataField adf = (AscensionDataField) o;

			// First, compare the number of days between
			// ascension runs.

			int dayDifference = this.dayCount - adf.dayCount;
			if ( dayDifference != 0 )
			{
				return dayDifference;
			}

			// Next, compare the number of turns it took
			// in order to complete the ascension.

			int turnDifference = this.turnCount - adf.turnCount;
			if ( turnDifference != 0 )
			{
				return turnDifference;
			}

			// Earlier ascensions take priority.  Therefore,
			// compare the timestamp.  Later, this will also
			// take the 60-day sliding window into account.

			if ( this.timestamp.before( adf.timestamp ) )
			{
				return -1;
			}
			if ( this.timestamp.after( adf.timestamp ) )
			{
				return 1;
			}

			// If it still is equal, then check the difference
			// in levels, and return that -- effectively, if all
			// comparable elements are the same, then they are equal.

			return this.level - adf.level;
		}
	}
}
