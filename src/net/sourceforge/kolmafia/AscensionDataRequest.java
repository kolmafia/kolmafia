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

import java.io.BufferedReader;
import java.io.File;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AscensionDataRequest extends KoLRequest implements Comparable
{
	private static boolean isSoftcoreComparator = true;

	private static final SimpleDateFormat ASCEND_DATE_FORMAT = new SimpleDateFormat( "MM/dd/yy", Locale.US );
	private static final Pattern FIELD_PATTERN = Pattern.compile( "</tr><td class=small.*?</tr>" );

	private String playerName;
	private String playerId;
	private List ascensionData;
	private int hardcoreCount, softcoreCount;

	public AscensionDataRequest( String playerName, String playerId )
	{
		super( "ascensionhistory.php" );

		this.addFormField( "back", "self" );
		this.addFormField( "who", KoLmafia.getPlayerId( playerName ) );

		this.playerName = playerName;
		this.playerId = playerId;

		this.ascensionData = new ArrayList();
	}

	public static void setComparator( boolean isSoftcoreComparator )
	{	AscensionDataRequest.isSoftcoreComparator = isSoftcoreComparator;
	}

	public String toString()
	{
		StringBuffer stringForm = new StringBuffer();
		stringForm.append( "<tr><td><a href=\"ascensions/" + ClanManager.getURLName( this.playerName ) + "\"><b>" );

		String name = KoLmafia.getPlayerName( this.playerId );
		stringForm.append( name.equals( this.playerId ) ? this.playerName : name );

		stringForm.append( "</b></a></td>" );
		stringForm.append( "<td align=right>" );
		stringForm.append( isSoftcoreComparator ? this.softcoreCount : this.hardcoreCount );
		stringForm.append( "</td></tr>" );
		return stringForm.toString();
	}

	public int compareTo( Object o )
	{
		return o == null || !(o instanceof AscensionDataRequest) ? -1 :
			isSoftcoreComparator ? ((AscensionDataRequest)o).softcoreCount - this.softcoreCount :
			((AscensionDataRequest)o).hardcoreCount - this.hardcoreCount;
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public void processResults()
	{
		this.responseText = this.responseText.replaceAll( "<a[^>]*?>Back[^<?]</a>", "" ).replaceAll( "<td></td>",
			"<td><img src=\"http://images.kingdomofloathing.com/itemimages/confused.gif\" height=30 width=30></td>" );

		this.refreshFields();
	}

	private String getBackupFileData()
	{
		File clan = new File( ROOT_LOCATION, "clan" );
		if ( !clan.exists() )
			return "";

		File [] resultFolders = clan.listFiles();

		File backupFile = null;
		int bestMonth = 0, bestWeek = 0;
		int currentMonth, currentWeek;

		for ( int i = 0; i < resultFolders.length; ++i )
		{
			if ( !resultFolders[i].isDirectory() )
				continue;

			File [] ascensionFolders = resultFolders[i].listFiles();

			for ( int j = 0; j < ascensionFolders.length; ++j )
			{
				if ( !ascensionFolders[j].getName().startsWith( "2005" ) )
					continue;

				currentMonth = StaticEntity.parseInt( ascensionFolders[j].getName().substring( 4, 6 ) );
				currentWeek = StaticEntity.parseInt( ascensionFolders[j].getName().substring( 8, 9 ) );

				boolean shouldReplace = false;

				shouldReplace |= currentMonth > bestMonth;
				shouldReplace |= currentMonth == bestMonth && currentWeek > bestWeek;
				shouldReplace &= currentMonth == 9 || currentMonth == 10;

				if ( shouldReplace )
				{
					File checkFile = new File( ascensionFolders[j], "ascensions/" + this.playerId + ".htm");
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
			return "";

		try
		{
			BufferedReader istream = KoLDatabase.getReader( backupFile );
			StringBuffer ascensionBuffer = new StringBuffer();
			String currentLine;

			while ( (currentLine = istream.readLine()) != null )
			{
				ascensionBuffer.append( currentLine );
				ascensionBuffer.append( LINE_BREAK );
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
	 * Internal method used to refresh the fields of the profile
	 * request based on the response text.  This should be called
	 * after the response text is already retrieved.
	 */

	private void refreshFields()
	{
		this.ascensionData.clear();
		Matcher fieldMatcher = FIELD_PATTERN.matcher( this.responseText );

		StringBuffer ascensionBuffer = new StringBuffer();
		ascensionBuffer.append( this.getBackupFileData() );

		int lastFindIndex = 0;
		AscensionDataField lastField;

		if ( ascensionBuffer.length() != 0 )
		{
			int oldFindIndex = 0;
			boolean inconsistency = false;
			boolean newDataAvailable = true;
			String [] columnsNew = null;

			Matcher oldDataMatcher = FIELD_PATTERN.matcher( ascensionBuffer );
			if ( !fieldMatcher.find( lastFindIndex ) )
			{
				newDataAvailable = false;
			}
			else
			{
				lastFindIndex = fieldMatcher.end() - 5;
				columnsNew = extractColumns( fieldMatcher.group() );
			}

			while ( oldDataMatcher.find( oldFindIndex ) )
			{
				oldFindIndex = oldDataMatcher.end() - 5;

				String [] columnsOld = extractColumns( oldDataMatcher.group() );
				if ( !newDataAvailable )
				{
					lastField = new AscensionDataField( this.playerName, this.playerId, columnsOld );
					this.ascensionData.add( lastField );

					if ( lastField.isSoftcore )
						++this.softcoreCount;
					else
						++this.hardcoreCount;
				}

				else if ( columnsNew != null && columnsNew[0].equals( columnsOld[0] ) )
				{
					if ( !fieldMatcher.find( lastFindIndex ) )
					{
						newDataAvailable = false;
					}
					else
					{
						lastFindIndex = fieldMatcher.end() - 5;
						columnsNew = extractColumns( fieldMatcher.group() );
					}

					lastField = new AscensionDataField( this.playerName, this.playerId, columnsOld );
					this.ascensionData.add( lastField );

					if ( lastField.isSoftcore )
						++this.softcoreCount;
					else
						++this.hardcoreCount;
				}
				else
				{
					lastField = new AscensionDataField( this.playerName, this.playerId, columnsOld );
					this.ascensionData.add( lastField );

					if ( lastField.isSoftcore )
						++this.softcoreCount;
					else
						++this.hardcoreCount;

					try
					{
						// Subtract columns[turns] from columnsNew[turns];
						// currently, this is [5]

						inconsistency = true;
						columnsNew[5] = String.valueOf( StaticEntity.parseInt( columnsNew[5] ) - StaticEntity.parseInt( columnsOld[5] ) );

						// Subtract columns[days] from columnsNew[days];
						// currently, this is [6].  Ascensions count
						// both first day and last day, so remember to
						// add it back in.

						long timeDifference = ASCEND_DATE_FORMAT.parse( columnsNew[1] ).getTime() -
							ASCEND_DATE_FORMAT.parse( columnsOld[1] ).getTime();

						columnsNew[6] = String.valueOf( Math.round( timeDifference / 86400000l ) + 1 );
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

				if ( lastField.isSoftcore )
					++this.softcoreCount;
				else
					++this.hardcoreCount;

				lastFindIndex = fieldMatcher.end() - 5;
			}
		}

		while ( fieldMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = fieldMatcher.end() - 5;

			String [] columns = extractColumns( fieldMatcher.group() );
			lastField = new AscensionDataField( this.playerName, this.playerId, columns );
			this.ascensionData.add( lastField );

			if ( lastField.isSoftcore )
				++this.softcoreCount;
			else
				++this.hardcoreCount;
		}
	}

	/**
	 * Static method used by the clan manager in order to
	 * get an instance of a profile request based on the
	 * data already known.
	 */

	public static AscensionDataRequest getInstance( String playerName, String playerId, String responseText )
	{
		AscensionDataRequest instance = new AscensionDataRequest( playerName, playerId );

		instance.responseText = responseText;
		instance.refreshFields();

		return instance;
	}

	public String getPlayerName()
	{	return this.playerName;
	}

	public String getPlayerId()
	{	return this.playerId;
	}

	public void initialize()
	{
		if ( this.responseText == null )
			RequestThread.postRequest( this );
	}

	public List getAscensionData()
	{	return this.ascensionData;
	}

	private static String [] extractColumns( String rowData )
	{	return rowData.replaceFirst( "</tr><td.*?>", "" ).replaceAll( "&nbsp;", "" ).replaceAll( " ", "" ).split( "(</?t[rd].*?>)+" );
	}

	public static class AscensionDataField implements Comparable
	{
		private String playerName;
		private String playerId;
		private StringBuffer stringForm;

		private Date timestamp;
		private boolean isSoftcore;
		private int level, classId, pathId;
		private int dayCount, turnCount;

		public AscensionDataField( String playerName, String playerId, String rowData )
		{	this.setData( playerName, playerId, extractColumns( rowData ) );
		}

		public AscensionDataField( String playerName, String playerId, String [] columns )
		{	this.setData( playerName, playerId, columns );
		}

		private void setData( String playerName, String playerId, String [] columns )
		{
			this.playerId = playerId;
			this.playerName = KoLmafia.getPlayerName( playerId );

			if ( this.playerName.equals( this.playerId ) )
				this.playerName = playerName;

			// The level at which the ascension took place is found
			// in the third column, or index 2 in the array.

			try
			{
				this.timestamp = ASCEND_DATE_FORMAT.parse( columns[1] );
				this.level = StaticEntity.parseInt( columns[2] );
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}

			this.turnCount = StaticEntity.parseInt( columns[5] );
			this.dayCount = StaticEntity.parseInt( columns[6] );

			if ( columns.length == 9 )
				this.setCurrentColumns( columns );
			else
				this.setHistoricColumns( columns );

			this.stringForm = new StringBuffer();
			this.stringForm.append( "<tr><td><a href=\"ascensions/" + ClanManager.getURLName( this.playerName ) + "\"><b>" );
			this.stringForm.append( this.playerName );
			this.stringForm.append( "</b></a>&nbsp;(" );

			switch ( this.classId )
			{
			case AscensionSnapshotTable.SEAL_CLUBBER:
				this.stringForm.append( "SC" );
				break;

			case AscensionSnapshotTable.TURTLE_TAMER:
				this.stringForm.append( "TT" );
				break;

			case AscensionSnapshotTable.PASTAMANCER:
				this.stringForm.append( "P" );
				break;

			case AscensionSnapshotTable.SAUCEROR:
				this.stringForm.append( "S" );
				break;

			case AscensionSnapshotTable.DISCO_BANDIT:
				this.stringForm.append( "DB" );
				break;

			case AscensionSnapshotTable.ACCORDION_THIEF:
				this.stringForm.append( "AT" );
				break;
			}

			this.stringForm.append( ")&nbsp;&nbsp;&nbsp;&nbsp;</td><td align=right>" );
			this.stringForm.append( this.dayCount );
			this.stringForm.append( "</td><td align=right>" );
			this.stringForm.append( this.turnCount );
			this.stringForm.append( "</td></tr>" );
		}

		private void setHistoricColumns( String [] columns )
		{
			this.classId = columns[3].startsWith( "SC" ) ? AscensionSnapshotTable.SEAL_CLUBBER :
				columns[3].startsWith( "T" ) ? AscensionSnapshotTable.TURTLE_TAMER :
				columns[3].startsWith( "P" ) ? AscensionSnapshotTable.PASTAMANCER :
				columns[3].startsWith( "S" ) ? AscensionSnapshotTable.SAUCEROR :
				columns[3].startsWith( "D" ) ? AscensionSnapshotTable.DISCO_BANDIT : AscensionSnapshotTable.ACCORDION_THIEF;

			String [] path = columns[7].split( "," );

			this.isSoftcore = path[0].equals( "Normal" );
			this.pathId = path[1].equals( "No Path" ) ? AscensionSnapshotTable.NOPATH :
				path[1].equals( "Teetotaler" ) ? AscensionSnapshotTable.TEETOTALER :
				path[1].equals( "Boozetafarian" ) ? AscensionSnapshotTable.BOOZETAFARIAN : AscensionSnapshotTable.OXYGENARIAN;
		}

		private void setCurrentColumns( String [] columns )
		{
			try
			{
				this.classId = columns[3].indexOf( "club" ) != -1 ? AscensionSnapshotTable.SEAL_CLUBBER :
					columns[3].indexOf( "turtle" ) != -1 ? AscensionSnapshotTable.TURTLE_TAMER :
					columns[3].indexOf( "pasta" ) != -1 ? AscensionSnapshotTable.PASTAMANCER :
					columns[3].indexOf( "sauce" ) != -1 ? AscensionSnapshotTable.SAUCEROR :
					columns[3].indexOf( "disco" ) != -1 ? AscensionSnapshotTable.DISCO_BANDIT :
					AscensionSnapshotTable.ACCORDION_THIEF;

				this.isSoftcore = columns[8].indexOf( "hardcore" ) == -1;
				this.pathId = columns[8].indexOf( "bowl" ) != -1 ? AscensionSnapshotTable.TEETOTALER :
					columns[8].indexOf( "martini" ) != -1 ? AscensionSnapshotTable.BOOZETAFARIAN :
					columns[8].indexOf( "oxy" ) != -1 ? AscensionSnapshotTable.OXYGENARIAN : AscensionSnapshotTable.NOPATH;
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}

		public String getDateAsString()
		{	return ProfileRequest.OUTPUT_FORMAT.format( this.timestamp );
		}

		public int getAge()
		{
			long ascensionDate = this.timestamp.getTime();
			float difference = System.currentTimeMillis() - ascensionDate;
			int days = (Math.round((difference/(1000*60*60*24))));
			return days;
		}


		public String toString()
		{	return this.stringForm.toString();
		}

		public boolean equals( Object o )
		{	return o != null && o instanceof AscensionDataField && this.playerId.equals( ((AscensionDataField)o).playerId );
		}

		public boolean matchesFilter( boolean isSoftcore, int pathFilter, int classFilter, int maxAge )
		{
			return isSoftcore == this.isSoftcore && (pathFilter == AscensionSnapshotTable.NO_FILTER || pathFilter == this.pathId) &&
				(classFilter == AscensionSnapshotTable.NO_FILTER || classFilter == this.classId) &&
				(maxAge == 0 || maxAge >= this.getAge());
		}

		public boolean matchesFilter( boolean isSoftcore, int pathFilter, int classFilter )
		{
			return isSoftcore == this.isSoftcore && (pathFilter == AscensionSnapshotTable.NO_FILTER || pathFilter == this.pathId) &&
				(classFilter == AscensionSnapshotTable.NO_FILTER || classFilter == this.classId);
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof AscensionDataField) )
				return -1;

			AscensionDataField adf = (AscensionDataField) o;

			// First, compare the number of days between
			// ascension runs.

			int dayDifference = this.dayCount - adf.dayCount;
			if ( dayDifference != 0 )
				return dayDifference;

			// Next, compare the number of turns it took
			// in order to complete the ascension.

			int turnDifference = this.turnCount - adf.turnCount;
			if ( turnDifference != 0 )
				return turnDifference;

			// Earlier ascensions take priority.  Therefore,
			// compare the timestamp.  Later, this will also
			// take the 60-day sliding window into account.

			if ( this.timestamp.before( adf.timestamp ) )
				return -1;
			if ( this.timestamp.after( adf.timestamp ) )
				return 1;

			// If it still is equal, then check the difference
			// in levels, and return that -- effectively, if all
			// comparable elements are the same, then they are equal.

			return this.level - adf.level;
		}
	}
}
