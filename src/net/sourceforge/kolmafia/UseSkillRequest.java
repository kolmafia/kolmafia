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

/**
 * In order to be friendly to the server, this class is
 * designed to request a logout whenever the user exits.
 * It also notifies the client that a logout request was
 * sent so that the active frame can be switched back
 * to the <code>LoginFrame</code>.
 */

public class UseSkillRequest extends KoLRequest
{
	private int consumedMP;

	/**
	 * Constructs a new <code>UneffectRequest</code>.
	 * @param	client	The client to be notified of completion
	 * @param	effectDescription	The description of the effect to be removed
	 */

	public UseSkillRequest( KoLmafia client, String skillName, String target, int quantity )
	{
		super( client, "skills.php" );
		addFormField( "action", "Skillz." );
		addFormField( "pwd", client.getPasswordHash() );

		int skillID = ClassSkillsDatabase.getSkillID( skillName );
		addFormField( "whichskill", "" + skillID );
		addFormField( "quantity", "" + quantity );
		addFormField( "bufftimes", "" + quantity );

		if ( target == null || target.trim().length() == 0 )
			addFormField( "targetplayer", "" + client.getCharacterData().getUserID() );
		else
			addFormField( "specificplayer", target );

		this.consumedMP = ClassSkillsDatabase.getMPConsumptionByID( skillID ) * quantity;
	}

	public void run()
	{
		super.run();

		// If it does not notify you that you didn't have enough mana points,
		// then the skill was successfully used.

		if ( replyContent != null && replyContent.indexOf( "You don't have enough" ) == -1 )
			client.addToResultTally( new AdventureResult( AdventureResult.MP, 0 - consumedMP ) );
	}
}