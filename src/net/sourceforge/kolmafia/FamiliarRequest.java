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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FamiliarRequest extends KoLRequest
{
	private static final Pattern EQUIP_PATTERN = Pattern.compile( "newfam=(\\d+)" );
	private FamiliarData changeTo;

	public FamiliarRequest()
	{
		super( "familiar.php" );
		this.changeTo = null;
	}

	public FamiliarRequest( FamiliarData changeTo )
	{
		super( "familiar.php" );

		if ( changeTo == FamiliarData.NO_FAMILIAR )
		{
			addFormField( "action", "putback" );
		}
		else
		{
			addFormField( "action", "newfam" );
			addFormField( "newfam", String.valueOf( changeTo.getId() ) );
		}

		this.changeTo = changeTo;
	}

	public String getFamiliarChange()
	{	return changeTo == null ? null : changeTo.toString();
	}

	public void run()
	{
		AdventureResult item = KoLCharacter.getFamiliar() != null ? KoLCharacter.getFamiliar().getItem() :
			EquipmentRequest.UNEQUIP;

		if ( changeTo == null )
			KoLmafia.updateDisplay( "Retrieving familiar data..." );
		else
		{
			FamiliarData familiar = KoLCharacter.getFamiliar();
			if ( familiar.getId() == changeTo.getId() )
				return;

			if ( familiar != FamiliarData.NO_FAMILIAR )
				KoLmafia.updateDisplay( "Putting " + familiar.getName() + " the " + familiar.getRace() + " back into terrarium..." );

			if ( changeTo != FamiliarData.NO_FAMILIAR )
				KoLmafia.updateDisplay( "Taking " + changeTo.getName() + " the " + changeTo.getRace() + " out of terrarium..." );
		}

		super.run();

		if ( changeTo != null && changeTo != FamiliarData.NO_FAMILIAR && changeTo.canEquip( item ) && FamiliarData.itemWeightModifier( changeTo.getItem().getItemId() ) <= FamiliarData.itemWeightModifier( item.getItemId() ) )
			(new EquipmentRequest( item, KoLCharacter.FAMILIAR )).run();
	}

	protected void processResults()
	{
		FamiliarData.registerFamiliarData( responseText );
		KoLCharacter.updateEquipmentList( KoLCharacter.FAMILIAR );

		if ( changeTo == null )
			KoLmafia.updateDisplay( "Familiar data retrieved." );
	}

	public String getCommandForm()
	{
		String familiarName = getFamiliarChange();
		return familiarName == null ? "" : "familiar " + familiarName;
	}

	public static boolean processRequest( String urlString )
	{
		if ( urlString.indexOf( "familiar.php?" ) == -1 )
			return false;

		if ( urlString.indexOf( "action=putback" ) != -1 )
		{
			KoLmafia.getSessionStream().println();
			KoLmafia.getSessionStream().println( "familiar none" );
			return true;
		}

		Matcher familiarMatcher = EQUIP_PATTERN.matcher( urlString );
		if ( familiarMatcher.find() )
		{
			FamiliarData changeTo = new FamiliarData( StaticEntity.parseInt( familiarMatcher.group(1) ) );
			int index = KoLCharacter.getFamiliarList().indexOf( changeTo );

			if ( index != -1 )
			{
				KoLmafia.getSessionStream().println();
				KoLmafia.getSessionStream().println( "familiar " + KoLCharacter.getFamiliarList().get(index).toString() );
				return true;
			}
		}

		return false;
	}
}
