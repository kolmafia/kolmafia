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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.RestorativeItemPanel;
import net.sourceforge.kolmafia.swingui.panel.StatusEffectPanel;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class SkillBuffFrame
	extends GenericFrame
{
	private LockableListModel contacts;

	private SkillTypeComboBox typeSelect;
	private AutoFilterComboBox skillSelect;
	private AutoHighlightTextField amountField;
	private AutoFilterComboBox targetSelect;
	private final ShowDescriptionList effectList;

	public SkillBuffFrame()
	{
		this( "" );
	}

	public SkillBuffFrame( final String recipient )
	{
		super( "Skill Casting" );

		JPanel skillWrapper = new JPanel( new BorderLayout() );
		skillWrapper.add( new SkillBuffPanel(), BorderLayout.NORTH );

		this.effectList = new ShowDescriptionList( KoLConstants.activeEffects, 12 );
		this.effectList.addListSelectionListener( new SkillReselector() );

		this.tabs.addTab( "Active Effects", new StatusEffectPanel( this.effectList ) );
		this.tabs.addTab( "Recovery Items", new RestorativeItemPanel() );

		skillWrapper.add( this.tabs, BorderLayout.CENTER );

		this.setCenterComponent( skillWrapper );

		this.setRecipient( recipient );
	}

	public void setRecipient( String recipient )
	{
		if ( !this.contacts.contains( recipient ) )
		{
			recipient = ContactManager.getPlayerName( recipient );
			this.contacts.add( 0, recipient );
		}

		this.targetSelect.getEditor().setItem( recipient );
		this.targetSelect.setSelectedItem( recipient );
	}

	private class SkillReselector
		implements ListSelectionListener
	{
		public void valueChanged( final ListSelectionEvent e )
		{
			AdventureResult effect = (AdventureResult) SkillBuffFrame.this.effectList.getSelectedValue();
			if ( effect == null )
			{
				return;
			}

			SkillBuffFrame.this.skillSelect.setSelectedItem( UseSkillRequest.getUnmodifiedInstance( UneffectRequest.effectToSkill( effect.getName() ) ) );
		}
	}

	private class SkillBuffPanel
		extends GenericPanel
		implements Listener
	{
		public SkillBuffPanel()
		{
			super( "cast", "maxcast", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			SkillBuffFrame.this.typeSelect = new SkillTypeComboBox();
			SkillBuffFrame.this.skillSelect = new AutoFilterComboBox( KoLConstants.usableSkills, false );
			SkillBuffFrame.this.amountField = new AutoHighlightTextField();

			SkillBuffFrame.this.contacts = (LockableListModel) ContactManager.getMailContacts().getMirrorImage();
			SkillBuffFrame.this.targetSelect = new AutoFilterComboBox( SkillBuffFrame.this.contacts, true );

			VerifiableElement[] elements = new VerifiableElement[ 4 ];
			elements[ 0 ] = new VerifiableElement( "Skill Type: ", SkillBuffFrame.this.typeSelect );
			elements[ 1 ] = new VerifiableElement( "Skill Name: ", SkillBuffFrame.this.skillSelect );
			elements[ 2 ] = new VerifiableElement( "# of Casts: ", SkillBuffFrame.this.amountField );
			elements[ 3 ] = new VerifiableElement( "The Victim: ", SkillBuffFrame.this.targetSelect );

			this.setContent( elements );
			
			this.update();
			this.setSkillListeners();
		}

		private final String[][] DAILY_LIMITED_SKILLS =
		{
			{
				"The Smile of Mr. A.", "_smilesOfMrA", "integer", "5",
			},
			{
				"Rainbow Gravitation", "prismaticSummons", "integer", "3",
			},
			{
				"Vent Rage Gland", "rageGlandVented", "boolean", "true",
			},
			{
				"Summon Crimbo Candy", "_candySummons", "integer", "1",
			},
			{
				"Lunch Break", "_lunchBreak", "boolean", "true",
			},
			{
				"Summon &quot;Boner Battalion&quot;", "_bonersSummoned", "boolean", "true",
			},
			{
				"Request Sandwich", "_requestSandwichSucceeded", "boolean", "true",
			},
			{
				"Grab a Cold One", "_coldOne", "boolean", "true",
			},
			{
				"Spaghetti Breakfast", "_spaghettiBreakfast", "boolean", "true",
			},
			{
				"Pastamastery", "noodleSummons", "integer", "1",
			},
			{
				"Canticle of Carboloading", "_carboLoaded", "boolean", "true",
			},
			{
				"Advanced Saucecrafting", "reagentSummons", "integer", "1",
			},
			{
				"That's Not a Knife", "_discoKnife", "boolean", "true",
			},
			{
				"Advanced Cocktailcrafting", "cocktailSummons", "integer", "1",
			},
			{
				"The Ballad of Richie Thingfinder", "_thingfinderCasts", "integer", "10",
			},
			{
				"Benetton's Medley of Diversity", "_benettonsCasts", "integer", "10",
			},
			{
				"Elron's Explosive Etude", "_elronsCasts", "integer", "10",
			},
			{
				"Chorale of Companionship", "_companionshipCasts", "integer", "10",
			},
			{
				"Prelude of Precision", "_precisionCasts", "integer", "10",
			},
			{
				"Donho's Bubbly Ballad", "_donhosCasts", "integer", "50",
			},
			{
				"Inigo's Incantation of Inspiration", "_inigosCasts", "integer", "5",
			},
			{
				"Summon Snowcones", "_snowconeSummons", "variable", "tomesummons",
			},
			{
				"Summon Stickers", "_stickerSummons", "variable", "tomesummons",
			},
			{
				"Summon Sugar Sheets", "_sugarSummons", "variable", "tomesummons",
			},
			{
				"Summon Clip Art", "_clipartSummons", "variable", "tomesummons",
			},
			{
				"Summon Rad Libs", "_radlibSummons", "variable", "tomesummons",
			},
			{
				"Summon Smithsness", "_smithsnessSummons", "variable", "tomesummons",
			},
			{
				"Dummy", "tomeSummons", "variable", "tomesummons",
			},
			{
				"Summon Hilarious Objects", "grimoire1Summons", "integer", "1",
			},
			{
				"Summon Tasteful Items", "grimoire2Summons", "integer", "1",
			},
			{
				"Summon Alice's Army Cards", "grimoire3Summons", "integer", "1",
			},
			{
				"Summon Geeky Gifts", "_grimoireGeekySummons", "integer", "1",
			},
			{
				"Summon Confiscated Things", "_grimoireConfiscatorSummons", "integer", "1",
			},
			{
				"Demand Sandwich", "_demandSandwich", "integer", "3",
			},
			{
				"Conjure Eggs", "_jarlsEggsSummoned", "boolean", "true",
			},
			{
				"Conjure Dough", "_jarlsDoughSummoned", "boolean", "true",
			},
			{
				"Conjure Vegetables", "_jarlsVeggiesSummoned", "boolean", "true",
			},
			{
				"Conjure Cheese", "_jarlsCheeseSummoned", "boolean", "true",
			},
			{
				"Conjure Meat Product", "_jarlsMeatSummoned", "boolean", "true",
			},
			{
				"Conjure Potato", "_jarlsPotatoSummoned", "boolean", "true",
			},
			{
				"Conjure Cream", "_jarlsCreamSummoned", "boolean", "true",
			},
			{
				"Conjure Fruit", "_jarlsFruitSummoned", "boolean", "true",
			},
		};
		
		private void setSkillListeners()
		{
			for ( int i = 0; i < DAILY_LIMITED_SKILLS.length; ++i )
			{
				PreferenceListenerRegistry.registerPreferenceListener( DAILY_LIMITED_SKILLS[ i ][ 1 ], this );
			}
		}
		
		public void update()
		{
			for ( int i = 0; i < DAILY_LIMITED_SKILLS.length; ++i )
			{
				boolean skillDisable = false;
				
				// Handle Boolean Preferences
				if ( DAILY_LIMITED_SKILLS[ i ][ 2 ].equals( "boolean" ) )
				{
					skillDisable = Preferences.getBoolean( DAILY_LIMITED_SKILLS[ i ][ 1 ] ) == Boolean.valueOf( DAILY_LIMITED_SKILLS[ i ][ 3 ] );
				}
				else if ( DAILY_LIMITED_SKILLS[ i ][ 2 ].equals( "integer" ) )
				{
					skillDisable = Preferences.getInteger( DAILY_LIMITED_SKILLS[ i ][ 1 ] ) >= Integer.valueOf( DAILY_LIMITED_SKILLS[ i ][ 3 ] );
				}
				else if ( DAILY_LIMITED_SKILLS[ i ][ 2 ].equals( "variable" ) )
				{
					if ( DAILY_LIMITED_SKILLS[ i ][ 3 ].equals( "tomesummons" ) )
					{
						int maxCast = KoLCharacter.canInteract() ? Math.max( 3 - Preferences.getInteger( DAILY_LIMITED_SKILLS[ i ][ 1 ] ), 0 ) :
							Math.max( 3 - Preferences.getInteger( "tomeSummons" ), 0 );
						skillDisable = maxCast == 0;
					}
				}
				
				for ( int j = 0 ; j < SkillBuffFrame.this.skillSelect.getItemCount() ; j++ )
				{
					if ( SkillBuffFrame.this.skillSelect.getItemAt( j ).toString().contains( DAILY_LIMITED_SKILLS[ i ][ 0 ] ) )
					{
						
						SkillBuffFrame.this.skillSelect.setDisabledIndex( j, skillDisable );
						if ( skillDisable && j == SkillBuffFrame.this.skillSelect.getSelectedIndex() )
						{
							SkillBuffFrame.this.skillSelect.setSelectedIndex( -1 );
						}
						continue;
					}
				}
			}
		}
		
		@Override
		public void setEnabled( final boolean isEnabled )
		{
			if ( SkillBuffFrame.this.skillSelect == null || SkillBuffFrame.this.targetSelect == null )
			{
				return;
			}

			super.setEnabled( isEnabled );

			SkillBuffFrame.this.skillSelect.setEnabled( isEnabled );
			SkillBuffFrame.this.targetSelect.setEnabled( isEnabled );
		}

		@Override
		public void actionConfirmed()
		{
			this.buff( false );
		}

		@Override
		public void actionCancelled()
		{
			this.buff( true );
		}

		private void buff( boolean maxBuff )
		{
			UseSkillRequest request = (UseSkillRequest) SkillBuffFrame.this.skillSelect.getSelectedItem();
			if ( request == null )
			{
				return;
			}

			String buffName = request.getSkillName();
			if ( buffName == null )
			{
				return;
			}

			String[] targets =
				ContactManager.extractTargets( (String) SkillBuffFrame.this.targetSelect.getSelectedItem() );

			int buffCount = !maxBuff ? InputFieldUtilities.getValue( SkillBuffFrame.this.amountField, 1 ) : Integer.MAX_VALUE;
			if ( buffCount == 0 )
			{
				return;
			}

			SpecialOutfit.createImplicitCheckpoint();

			if ( targets.length == 0 )
			{
				RequestThread.postRequest( UseSkillRequest.getInstance( buffName, KoLCharacter.getUserName(), buffCount ) );
			}
			else
			{
				for ( int i = 0; i < targets.length && KoLmafia.permitsContinue(); ++i )
				{
					if ( targets[ i ] != null )
					{
						RequestThread.postRequest( UseSkillRequest.getInstance( buffName, targets[ i ], buffCount ) );
					}
				}
			}

			SpecialOutfit.restoreImplicitCheckpoint();
		}
	}

	private class SkillTypeComboBox
		extends JComboBox
	{
		public SkillTypeComboBox()
		{
			super();
			addItem( "All Castable Skills" );
			addItem( "Summoning Skills" );
			addItem( "Remedies" );
			addItem( "Self-Only" );
			addItem( "Buffs" );
			addItem( "Songs" );
			addItem( "Expressions" );
			addActionListener( new SkillTypeListener() );
		}

		private class SkillTypeListener
			implements ActionListener
		{
			public void actionPerformed( final ActionEvent e )
			{
				int index = SkillTypeComboBox.this.getSelectedIndex();
				switch ( index )
				{
				case 0:
					// All skills
					SkillBuffFrame.this.skillSelect.setModel( KoLConstants.usableSkills );
					break;
				case 1:
					// Summoning skills
					SkillBuffFrame.this.skillSelect.setModel( KoLConstants.summoningSkills );
					break;
				case 2:
					// Remedy skills
					SkillBuffFrame.this.skillSelect.setModel( KoLConstants.remedySkills );
					break;
				case 3:
					// Self-only skills
					SkillBuffFrame.this.skillSelect.setModel( KoLConstants.selfOnlySkills );
					break;
				case 4:
					// Buff skills
					SkillBuffFrame.this.skillSelect.setModel( KoLConstants.buffSkills );
					break;
				case 5:
					// Song skills
					SkillBuffFrame.this.skillSelect.setModel( KoLConstants.songSkills );
					break;
				case 6:
					// Expression skills
					SkillBuffFrame.this.skillSelect.setModel( KoLConstants.expressionSkills );
					break;
				}
			}
		}
	}
}
