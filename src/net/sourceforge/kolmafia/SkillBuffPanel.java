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

import java.awt.Dimension;
import java.awt.BorderLayout;
import javax.swing.JComboBox;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * An internal class which represents the panel used for adding
 * effects to a character (yourself or others).
 */

public class SkillBuffPanel extends KoLPanel
{
	private JComboBox skillSelect;
	private JComboBox targetSelect;

	public SkillBuffPanel()
	{	this ( "" );
	}

	public SkillBuffPanel( String initialRecipient )
	{
		super( "cast", "maxcast", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

		skillSelect = new JComboBox( KoLCharacter.getUsableSkills() );
		targetSelect = new MutableComboBox( (SortedListModel) StaticEntity.getClient().getContactList().clone() );

		VerifiableElement [] elements = new VerifiableElement[2];
		elements[0] = new VerifiableElement( "Skill Name: ", skillSelect );
		elements[1] = new VerifiableElement( "The Victim: ", targetSelect );

		setContent( elements );
		add( new UneffectPanel(), BorderLayout.CENTER );

		if ( !initialRecipient.equals( "" ) )
		{
			targetSelect.addItem( initialRecipient );
			targetSelect.getEditor().setItem( initialRecipient );
			targetSelect.setSelectedItem( initialRecipient );
		}
	}

	private class UneffectPanel extends ItemManagePanel
	{
		public UneffectPanel()
		{	super( "Status Effects", "uneffect", "describe", KoLCharacter.getEffects() );
		}

		public void actionConfirmed()
		{	(new RequestThread( new UneffectRequest( StaticEntity.getClient(), (AdventureResult) elementList.getSelectedValue() ) )).start();
		}

		public void actionCancelled()
		{	FightFrame.showLocation( "desc_effect.php?whicheffect=" + StatusEffectDatabase.getEffectID( ((AdventureResult) elementList.getSelectedValue()).getName() ) );
		}
	}

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );

		if ( skillSelect != null && targetSelect != null )
		{
			skillSelect.setEnabled( isEnabled );
			targetSelect.setEnabled( isEnabled );
		}
	}

	protected void actionConfirmed()
	{	buff( false );
	}

	protected void actionCancelled()
	{	buff( true );
	}

	private void buff( boolean maxBuff )
	{
		String buffName = ((UseSkillRequest) skillSelect.getSelectedItem()).getSkillName();
		if ( buffName == null )
			return;

		String [] targets = StaticEntity.getClient().extractTargets( (String) targetSelect.getSelectedItem() );

		int buffCount = !maxBuff ? KoLFrame.getQuantity( "Casting " + buffName + "...", Integer.MAX_VALUE, 1 ) : Integer.MAX_VALUE;
		if ( buffCount == 0 )
			return;

		Runnable [] requests;

		if ( targets.length == 0 )
		{
			requests = new Runnable[1];
			requests[0] = new UseSkillRequest( StaticEntity.getClient(), buffName, "", buffCount );
		}
		else
		{
			requests = new Runnable[ targets.length ];
			for ( int i = 0; i < requests.length && StaticEntity.getClient().permitsContinue(); ++i )
				if ( targets[i] != null )
					requests[i] = new UseSkillRequest( StaticEntity.getClient(), buffName, targets[i], buffCount );
		}

		(new RequestThread( requests )).start();
	}
}
