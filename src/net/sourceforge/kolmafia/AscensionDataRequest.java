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

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;

import java.io.File;
import java.io.BufferedReader;

public class AscensionDataRequest extends KoLRequest implements Comparable
{
	private static boolean isSoftcoreComparator = true;

	private static final SimpleDateFormat ASCEND_DATE_FORMAT = new SimpleDateFormat( "MM/dd/yy", Locale.US );
	private static final Pattern FIELD_PATTERN = Pattern.compile( "</tr><td>.*?</tr>" );

	private String playerName;
	private String playerID;
	private List ascensionData;
	private int hardcoreCount, softcoreCount;

	public AscensionDataRequest( String playerName, String playerID )
	{
		super( "ascensionhistory.php" );

		addFormField( "back", "self" );
		addFormField( "who", KoLmafia.getPlayerID( playerName ) );

		this.playerName = playerName;
		this.playerID = playerID;

		this.ascensionData = new ArrayList();
	}

	public static void setComparator( boolean isSoftcoreComparator )
	{	AscensionDataRequest.isSoftcoreComparator = isSoftcoreComparator;
	}

	public String toString()
	{
		StringBuffer stringForm = new StringBuffer();
		stringForm.append( "<tr><td><a href=\"ascensions/" + this.playerID + ".htm\"><b>" );

		String name = KoLmafia.getPlayerName( this.playerID );
		stringForm.append( name.equals( this.playerID ) ? this.playerName : name );

		stringForm.append( "</b></a></td>" );
		stringForm.append( "<td align=right>" );
		stringForm.append( isSoftcoreComparator ? softcoreCount : hardcoreCount );
		stringForm.append( "</td></tr>" );
		return stringForm.toString();
	}

	public int compareTo( Object o )
	{
		return o == null || !(o instanceof AscensionDataRequest) ? -1 :
			isSoftcoreComparator ? ((AscensionDataRequest)o).softcoreCount - softcoreCount :
			((AscensionDataRequest)o).hardcoreCount - hardcoreCount;
	}

	protected void processResults()
	{
		responseText = responseText.replaceAll( "<a[^>]*?>Back[^<?]</a>", "" );
		refreshFields();
	}

	private String getBackupFileData()
	{
		File clan = new File( "clan" );
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
					File checkFile = new File( ascensionFolders[j], "ascensions/" + playerID + ".htm");
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
		ascensionData.clear();
		Matcher fieldMatcher = FIELD_PATTERN.matcher( responseText );

		StringBuffer ascensionBuffer = new StringBuffer();
		ascensionBuffer.append( getBackupFileData() );

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
				columnsNew = fieldMatcher.group().replaceAll( "</tr><td>", "" ).replaceAll( "&nbsp;", "" ).replaceAll( " ", "" ).split( "(<.*?>)+" );
			}

			while ( oldDataMatcher.find( oldFindIndex ) )
			{
				oldFindIndex = oldDataMatcher.end() - 5;

				String [] columnsOld = oldDataMatcher.group().replaceAll( "</tr><td>", "" ).replaceAll( "&nbsp;", "" ).replaceAll( " ", "" ).split( "(<.*?>)+" );
				if ( !newDataAvailable )
				{
					lastField = new AscensionDataField( playerName, playerID, columnsOld );
					ascensionData.add( lastField );

					if ( lastField.isSoftcore )
						++softcoreCount;
					else
						++hardcoreCount;
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
						columnsNew = fieldMatcher.group().replaceAll( "</tr><td>", "" ).replaceAll( "&nbsp;", "" ).replaceAll( " ", "" ).split( "(<.*?>)+" );
					}

					lastField = new AscensionDataField( playerName, playerID, columnsOld );
					ascensionData.add( lastField );

					if ( lastField.isSoftcore )
						++softcoreCount;
					else
						++hardcoreCount;
				}
				else
				{
					lastField = new AscensionDataField( playerName, playerID, columnsOld );
					ascensionData.add( lastField );

					if ( lastField.isSoftcore )
						++softcoreCount;
					else
						++hardcoreCount;

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

						columnsNew[6] = String.valueOf( (int) Math.round( timeDifference / 86400000l ) + 1 );
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
				lastField = new AscensionDataField( playerName, playerID, columnsNew );
				ascensionData.add( lastField );

				if ( lastField.isSoftcore )
					++softcoreCount;
				else
					++hardcoreCount;

				lastFindIndex = fieldMatcher.end() - 5;
			}
		}

		while ( fieldMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = fieldMatcher.end() - 5;

			String [] columns = fieldMatcher.group().replaceAll( "</tr><td>", "" ).replaceAll( "&nbsp;", "" ).replaceAll( " ", "" ).split( "(<.*?>)+" );

			lastField = new AscensionDataField( playerName, playerID, columns );
			ascensionData.add( lastField );

			if ( lastField.isSoftcore )
				++softcoreCount;
			else
				++hardcoreCount;
		}
	}

	/**
	 * Static method used by the clan manager in order to
	 * get an instance of a profile request based on the
	 * data already known.
	 */

	public static AscensionDataRequest getInstance( String playerName, String playerID, String responseText )
	{
		AscensionDataRequest instance = new AscensionDataRequest( playerName, playerID );

		instance.responseText = responseText;
		instance.refreshFields();

		return instance;
	}

	public String getPlayerName()
	{	return playerName;
	}

	public String getPlayerID()
	{	return playerID;
	}

	public void initialize()
	{
		if ( responseText == null )
			this.run();
	}

	public List getAscensionData()
	{	return ascensionData;
	}

	public static class AscensionDataField implements Comparable
	{
		private String playerName;
		private String playerID;
		private StringBuffer stringForm;

		private String sign;
		private Date timestamp;
		private boolean isSoftcore;
		private int level, classID, pathID;
		private int dayCount, turnCount;

		public AscensionDataField( String playerName, String playerID, String rowData )
		{

			String [] columns = rowData.replaceAll( "</tr><td>", "" ).replaceAll( "&nbsp;", "" ).replaceAll( " ", "" ).split( "(<.*?>)+" );
			setData( playerName, playerID, columns );

		}

		public AscensionDataField( String playerName, String playerID, String[] columns )
		{
			setData( playerName, playerID, columns );
		}

		private void setData( String playerName, String playerID, String[] columns )
		{
			this.playerID = playerID;
			this.playerName = KoLmafia.getPlayerName( playerID );

			if ( this.playerName.equals( this.playerID ) )
				this.playerName = playerName;

			try
			{
				// The level at which the ascension took place is found
				// in the third column, or index 2 in the array.

				this.timestamp = ASCEND_DATE_FORMAT.parse( columns[1] );
				this.level = StaticEntity.parseInt( columns[2] );

				this.classID = columns[3].startsWith( "SC" ) ? AscensionSnapshotTable.SEAL_CLUBBER :
					columns[3].startsWith( "T" ) ? AscensionSnapshotTable.TURTLE_TAMER :
					columns[3].startsWith( "P" ) ? AscensionSnapshotTable.PASTAMANCER :
					columns[3].startsWith( "S" ) ? AscensionSnapshotTable.SAUCEROR :
					columns[3].startsWith( "D" ) ? AscensionSnapshotTable.DISCO_BANDIT : AscensionSnapshotTable.ACCORDION_THIEF;

				this.sign = columns[4];
				this.turnCount = StaticEntity.parseInt( columns[5] );
				this.dayCount = StaticEntity.parseInt( columns[6] );

				String [] path = columns[7].split( "," );
				this.isSoftcore = path[0].equals( "Normal" );

				this.pathID = path[1].equals( "NoPath" ) ? AscensionSnapshotTable.NOPATH :
					path[1].equals( "Teetotaler" ) ? AscensionSnapshotTable.TEETOTALER :
					path[1].equals( "Boozetafarian" ) ? AscensionSnapshotTable.BOOZETAFARIAN : AscensionSnapshotTable.OXYGENARIAN;
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}

			stringForm = new StringBuffer();
			stringForm.append( "<tr><td><a href=\"ascensions/" + this.playerID + ".htm\"><b>" );
			stringForm.append( this.playerName );
			stringForm.append( "</b></a>&nbsp;(" );

			switch ( this.classID )
			{
			case AscensionSnapshotTable.SEAL_CLUBBER:
				stringForm.append( "SC" );
				break;

			case AscensionSnapshotTable.TURTLE_TAMER:
				stringForm.append( "TT" );
				break;

			case AscensionSnapshotTable.PASTAMANCER:
				stringForm.append( "P" );
				break;

			case AscensionSnapshotTable.SAUCEROR:
				stringForm.append( "S" );
				break;

			case AscensionSnapshotTable.DISCO_BANDIT:
				stringForm.append( "DB" );
				break;

			case AscensionSnapshotTable.ACCORDION_THIEF:
				stringForm.append( "AT" );
				break;
			}

			stringForm.append( ")&nbsp;&nbsp;&nbsp;&nbsp;</td><td align=right>" );
			stringForm.append( this.dayCount );
			stringForm.append( "</td><td align=right>" );
			stringForm.append( this.turnCount );
			stringForm.append( "</td></tr>" );
		}

		public int getAge()
		{
			long ascensionDate = timestamp.getTime();
			float difference = System.currentTimeMillis() - ascensionDate;
			int days = (int)(Math.round((difference/(1000*60*60*24))));
			return days;
		}


		public String toString()
		{	return stringForm.toString();
		}

		public boolean equals( Object o )
		{	return o != null && o instanceof AscensionDataField && playerID.equals( ((AscensionDataField)o).playerID );
		}

		public boolean matchesFilter( boolean isSoftcore, int pathFilter, int classFilter, int maxAge )
		{
			return isSoftcore == this.isSoftcore && (pathFilter == AscensionSnapshotTable.NO_FILTER || pathFilter == this.pathID) &&
				(classFilter == AscensionSnapshotTable.NO_FILTER || classFilter == this.classID) &&
				(maxAge == 0 || maxAge >= getAge());
		}

		public boolean matchesFilter( boolean isSoftcore, int pathFilter, int classFilter )
		{
			return isSoftcore == this.isSoftcore && (pathFilter == AscensionSnapshotTable.NO_FILTER || pathFilter == this.pathID) &&
				(classFilter == AscensionSnapshotTable.NO_FILTER || classFilter == this.classID);
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof AscensionDataField) )
				return -1;

			AscensionDataField adf = (AscensionDataField) o;

			// First, compare the number of days between
			// ascension runs.

			int dayDifference = dayCount - adf.dayCount;
			if ( dayDifference != 0 )
				return dayDifference;

			// Next, compare the number of turns it took
			// in order to complete the ascension.

			int turnDifference = turnCount - adf.turnCount;
			if ( turnDifference != 0 )
				return turnDifference;

			// Earlier ascensions take priority.  Therefore,
			// compare the timestamp.  Later, this will also
			// take the 60-day sliding window into account.

			if ( timestamp.before( adf.timestamp ) )
				return -1;
			if ( timestamp.after( adf.timestamp ) )
				return 1;

			// If it still is equal, then check the difference
			// in levels, and return that -- effectively, if all
			// comparable elements are the same, then they are equal.

			return level - adf.level;
		}
	}
}
