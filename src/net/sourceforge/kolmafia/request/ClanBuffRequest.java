/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import net.java.dev.spellcast.utilities.LockableListModel;

public class ClanBuffRequest
	extends GenericRequest
{
	private final int buffId;

	/**
	 * Constructs a new <code>ClanBuffRequest</code> with the specified buff identifier. This constructor is only
	 * available internally. Note that no descendents are possible because of the nature of the constructor.
	 *
	 * @param buffId The unique numeric identifier of the buff
	 */

	private ClanBuffRequest( final int buffId )
	{
		super( "clan_stash.php" );

		this.buffId = buffId;
		this.addFormField( "action", "buyround" );
		this.addFormField( "size", String.valueOf( buffId % 10 ) );
		this.addFormField( "whichgift", String.valueOf( ( buffId / 10 ) ) );
	}

	/**
	 * Returns a list of all the possible requests available through the current implementation of
	 * <code>ClanBuffRequest</code>.
	 *
	 * @return A complete <code>ListModel</code>
	 */

	public static final LockableListModel<ClanBuffRequest> getRequestList()
	{
		LockableListModel<ClanBuffRequest> requestList = new LockableListModel<ClanBuffRequest>();
		for ( int i = 1; i < 9; ++i )
		{
			for ( int j = 1; j <= 3; ++j )
			{
				requestList.add( new ClanBuffRequest( 10 * i + j ) );
			}
		}

		requestList.add( new ClanBuffRequest( 91 ) );

		return requestList;
	}

	/**
	 * Returns the string form of this request, which is the formal name of the buff that this buff request represents.
	 *
	 * @return The formal name of the clan buff requested
	 */

	@Override
	public String toString()
	{
		StringBuilder stringForm = new StringBuilder();
		int size = this.buffId % 10;
		int gift = this.buffId / 10;

		if ( gift != 9 )
		{
			switch ( size )
			{
			case 1:
				stringForm.append( "Cheap " );
				break;
			case 2:
				stringForm.append( "Normal " );
				break;
			case 3:
				stringForm.append( "Expensive " );
				break;
			}
		}

		switch ( gift )
		{
		case 1:
			stringForm.append( "Muscle Training" );
			break;
		case 2:
			stringForm.append( "Mysticality Training" );
			break;
		case 3:
			stringForm.append( "Moxie Training" );
			break;
		case 4:
			stringForm.append( "Temporary Muscle Boost" );
			break;
		case 5:
			stringForm.append( "Temporary Mysticality Boost" );
			break;
		case 6:
			stringForm.append( "Temporary Moxie Boost" );
			break;
		case 7:
			stringForm.append( "Temporary Item Drop Boost" );
			break;
		case 8:
			stringForm.append( "Temporary Meat Drop Boost" );
			break;
		case 9:
			stringForm.append( "Adventure Massage" );
			break;
		}

		return stringForm.toString();
	}
}
