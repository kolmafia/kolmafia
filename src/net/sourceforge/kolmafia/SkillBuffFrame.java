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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.SortedListModel;

public class SkillBuffFrame extends KoLFrame
{
	private JComboBox skillSelect;
	private JTextField amountField;
	private JComboBox targetSelect;
	private ShowDescriptionList effectList;

	public SkillBuffFrame()
	{	this( "" );
	}

	public SkillBuffFrame( String recipient )
	{
		super( "Skill Casting" );

		framePanel.add( new SkillBuffPanel(), BorderLayout.NORTH );

		effectList = new ShowDescriptionList( activeEffects, 12 );
		effectList.addListSelectionListener( new SkillReselector() );

		framePanel.add( new SimpleScrollPane( effectList ), BorderLayout.CENTER );

		if ( !recipient.equals( "" ) )
			setRecipient( recipient );
	}

	public void setRecipient( String recipient )
	{
		targetSelect.addItem( recipient );
		targetSelect.getEditor().setItem( recipient );
		targetSelect.setSelectedItem( recipient );
	}

	private class SkillReselector implements ListSelectionListener
	{
		public void valueChanged( ListSelectionEvent e )
		{
			AdventureResult effect = (AdventureResult) effectList.getSelectedValue();
			if ( effect == null )
				return;

			skillSelect.setSelectedItem( UseSkillRequest.getInstance( UneffectRequest.effectToSkill( effect.getName() ) ) );
		}
	}

	private class SkillBuffPanel extends KoLPanel
	{
		public SkillBuffPanel()
		{
			super( "cast", "maxcast", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			skillSelect = new JComboBox( usableSkills );
			amountField = new JTextField();
			targetSelect = new MutableComboBox( (SortedListModel) contactList.clone(), true );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Skill Name: ", skillSelect );
			elements[1] = new VerifiableElement( "# of Casts: ", amountField );
			elements[2] = new VerifiableElement( "The Victim: ", targetSelect );

			setContent( elements );
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( skillSelect == null || targetSelect == null )
				return;

			super.setEnabled( isEnabled );

			skillSelect.setEnabled( isEnabled );
			targetSelect.setEnabled( isEnabled );
		}

		public void actionConfirmed()
		{	buff( false );
		}

		public void actionCancelled()
		{	buff( true );
		}

		private void buff( boolean maxBuff )
		{
			String buffName = ((UseSkillRequest) skillSelect.getSelectedItem()).getSkillName();
			if ( buffName == null )
				return;

			String [] targets = StaticEntity.getClient().extractTargets( (String) targetSelect.getSelectedItem() );

			int buffCount = !maxBuff ? getValue( amountField, 1 ) : Integer.MAX_VALUE;
			if ( buffCount == 0 )
				return;

			RequestThread.openRequestSequence();
			SpecialOutfit.createImplicitCheckpoint();

			if ( targets.length == 0 )
			{
				RequestThread.postRequest( UseSkillRequest.getInstance( buffName, KoLCharacter.getUserName(), buffCount ) );
			}
			else
			{
				for ( int i = 0; i < targets.length && KoLmafia.permitsContinue(); ++i )
					if ( targets[i] != null )
						RequestThread.postRequest( UseSkillRequest.getInstance( buffName, targets[i], buffCount ) );
			}

			SpecialOutfit.restoreImplicitCheckpoint();
			RequestThread.closeRequestSequence();
		}
	}
}
