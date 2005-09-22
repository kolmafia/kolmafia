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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;

public class AscensionDataRequest extends KoLRequest
{
	private static final SimpleDateFormat ASCEND_DATE_FORMAT = new SimpleDateFormat( "MM/dd/yy" );

	private String playerName;
	private String playerID;
	private List ascensionData;

	public AscensionDataRequest( KoLmafia client, String playerName )
	{
		super( client, "ascensionhistory.php" );
		addFormField( "back", "self" );

		if ( client != null )
			addFormField( "who", client.getPlayerID( playerName ) );

		this.playerName = playerName;

		if ( client != null )
			this.playerID = client.getPlayerID( playerName );

		this.ascensionData = new ArrayList();
	}

	public void run()
	{
		super.run();
		responseText = responseText.replaceAll( "<a.*?</a>", "" );
		refreshFields();
	}

	/**
	 * Internal method used to refresh the fields of the profile
	 * request based on the response text.  This should be called
	 * after the response text is already retrieved.
	 */

	private void refreshFields()
	{
		ascensionData.clear();
		Matcher fieldMatcher = Pattern.compile( "</tr><td>.*?</tr>" ).matcher( responseText );

		int lastFindIndex = 0;
		while ( fieldMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = fieldMatcher.end() - 5;
			ascensionData.add( new AscensionDataField( playerName, playerID, fieldMatcher.group() ) );
		}
	}

	/**
	 * Static method used by the clan manager in order to
	 * get an instance of a profile request based on the
	 * data already known.
	 */

	public static AscensionDataRequest getInstance( String playerName, String responseText )
	{
		AscensionDataRequest instance = new AscensionDataRequest( null, playerName );

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
			this.playerName = playerName;
			this.playerID = playerID;

			try
			{
				String [] columns = rowData.replaceAll( "</tr><td>", "" ).replaceAll( "&nbsp;", "" ).replaceAll( " ", "" ).split( "(<.*?>)+" );

				// The level at which the ascension took place is found
				// in the third column, or index 2 in the array.

				this.timestamp = ASCEND_DATE_FORMAT.parse( columns[1] );
				this.level = df.parse( columns[2] ).intValue();

				this.classID = columns[3].startsWith( "Se" ) ? AscensionSnapshotTable.SEAL_CLUBBER :
					columns[3].startsWith( "Tu" ) ? AscensionSnapshotTable.TURTLE_TAMER :
					columns[3].startsWith( "Pa" ) ? AscensionSnapshotTable.PASTAMANCER :
					columns[3].startsWith( "Sa" ) ? AscensionSnapshotTable.SAUCEROR :
					columns[3].startsWith( "Di" ) ? AscensionSnapshotTable.DISCO_BANDIT : AscensionSnapshotTable.ACCORDION_THIEF;

				this.sign = columns[4];
				this.turnCount = df.parse( columns[5] ).intValue();
				this.dayCount = df.parse( columns[6] ).intValue();

				String [] path = columns[7].split( "," );
				this.isSoftcore = path[0].equals( "Normal" );

				this.pathID = path[1].equals( "NoPath" ) ? AscensionSnapshotTable.NOPATH :
					path[1].equals( "Teetotaler" ) ? AscensionSnapshotTable.TEETOTALER :
					path[1].equals( "Boozefetarian" ) ? AscensionSnapshotTable.BOOZEFETARIAN : AscensionSnapshotTable.OXYGENARIAN;
			}
			catch ( Exception e )
			{
				// Because the data is properly structured,
				// this exception should never be thrown.
			}

			stringForm = new StringBuffer();
			stringForm.append( "<tr><td><a href=\"profiles/" + this.playerID + ".htm\"><b>" + this.playerName + "</b></a>  (" );

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

		public String toString()
		{	return stringForm.toString();
		}

		public boolean equals( Object o )
		{	return o != null && o instanceof AscensionDataField && playerID.equals( ((AscensionDataField)o).playerID );
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
