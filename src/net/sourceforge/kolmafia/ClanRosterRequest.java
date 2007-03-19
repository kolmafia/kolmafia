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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClanRosterRequest extends KoLRequest
{
	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>(.*?)</tr>", Pattern.DOTALL );
	private static final Pattern CELL_PATTERN = Pattern.compile( "<td.*?>(.*?)</td>", Pattern.DOTALL );

	public ClanRosterRequest()
	{	super( "clan_detailedroster.php" );
	}

	public void run()
	{
		KoLmafia.updateDisplay( "Retrieving detailed roster..." );
		super.run();

		Matcher rowMatcher = ROW_PATTERN.matcher( responseText.substring( responseText.lastIndexOf( "clan_detailedroster.php" ) ) );

		String currentRow;
		String currentName;
		Matcher dataMatcher;

		while ( rowMatcher.find() )
		{
			currentRow = rowMatcher.group(1);

			if ( !currentRow.equals( "<td height=4></td>" ) )
			{
				dataMatcher = CELL_PATTERN.matcher( currentRow );

				// The name of the player occurs in the first
				// field of the table.  Use this to index the
				// roster map.

				dataMatcher.find();
				currentName = StaticEntity.globalStringReplace( ANYTAG_PATTERN.matcher( dataMatcher.group(1) ).replaceAll( "" ).trim(), "&nbsp;", "" ).toLowerCase();
				ClanSnapshotTable.addToRoster( currentName, currentRow );
			}
		}

		KoLmafia.updateDisplay( "Detail roster retrieved." );
	}
}