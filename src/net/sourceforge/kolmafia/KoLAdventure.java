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
 * An auxiliary class which stores runnable adventures so that they
 * can be created directly from a database.  Encapsulates the nature
 * of the adventure so that they can be easily listed inside of a
 * <code>ListModel</code>, with the potential to be extended to fit
 * other requests to the Kingdom of Loathing which need to be stored
 * within a database.
 */

public class KoLAdventure implements Runnable
{
	private KoLmafia client;
	private String adventureID, formSource, adventureName;
	private Runnable request;

	/**
	 * Constructs a new <code>KoLAdventure</code> with the given
	 * specifications.
	 *
	 * @param	client	The client to which the results of the adventure are reported
	 * @param	formSource	The form associated with the given adventure
	 * @param	adventureID	The identifier for this adventure, relative to its form
	 * @param	adventureName	The string form, or name of this adventure
	 */

	public KoLAdventure( KoLmafia client, String formSource, String adventureID, String adventureName )
	{
		this.client = client;
		this.formSource = formSource;
		this.adventureID = adventureID;
		this.adventureName = adventureName;

		if ( formSource.equals( "sewer.php" ) )
			this.request = new SewerRequest( client, false );
		else if ( formSource.equals( "luckysewer.php" ) )
			this.request = new SewerRequest( client, true );
		else if ( formSource.equals( "campground.php" ) )
			this.request = new CampgroundRequest( client, adventureID );
		else
			this.request = new AdventureRequest( client, formSource, adventureID );
	}

	/**
	 * Returns the adventure ID for this adventure.
	 * @return	The adventure ID for this adventure
	 */

	public String getAdventureID()
	{	return adventureID;
	}

	/**
	 * Retrieves the string form of the adventure contained within this
	 * encapsulation, which is generally the name of the adventure.
	 *
	 * @return	The string form of the adventure
	 */

	public String toString()
	{	return adventureName;
	}

	/**
	 * Executes the appropriate <code>KoLRequest</code> for the adventure
	 * encapsulated by this <code>KoLAdventure</code>.
	 */

	public void run()
	{
		if ( request != null )
			request.run();
	}
}