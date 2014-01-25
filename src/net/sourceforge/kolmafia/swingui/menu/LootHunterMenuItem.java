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

package net.sourceforge.kolmafia.swingui.menu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.swingui.AdventureFrame;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.PossibleSelection;

import net.sourceforge.kolmafia.utilities.IntWrapper;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LootHunterMenuItem
	extends ThreadedMenuItem
{
	public LootHunterMenuItem()
	{
		super( "Visit Bounty Hunter", new LootHunterListener() );
	}

	private static class LootHunterListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			GenericRequest hunterRequest = new BountyHunterHunterRequest();
			RequestThread.postRequest( hunterRequest );

			IntWrapper wrapper = new IntWrapper();

			List bounties = new ArrayList();
			String[] results = new String[2];
			
			// Add Easy Bounty Item
			String untakenBounty = Preferences.getString( "_untakenEasyBountyItem" );
			if ( !untakenBounty.equals( "" ) )
			{
				results = LootHunterMenuItem.buildInformation( "easy", untakenBounty, 0 );
				bounties.add( new PossibleSelection( results[ 0 ], results[ 1 ], 1, wrapper ) );
			}

			// Add Hard Bounty Item
			untakenBounty = Preferences.getString( "_untakenHardBountyItem" );
			if ( !untakenBounty.equals( "" ) )
			{
				results = LootHunterMenuItem.buildInformation( "hard", untakenBounty, 0 );
				bounties.add( new PossibleSelection( results[ 0 ], results[ 1 ], 2, wrapper ) );
			}

			// Add Speciality Bounty Item
			untakenBounty = Preferences.getString( "_untakenSpecialBountyItem" );
			if ( !untakenBounty.equals( "" ) )
			{
				results = LootHunterMenuItem.buildInformation( "speciality", untakenBounty, 0 );
				bounties.add( new PossibleSelection( results[ 0 ], results[ 1 ], 3, wrapper ) );
			}

			if ( bounties.isEmpty() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "No more bounties available today." );
				return;
			}

			boolean selectedAValue = LootHunterMenuItem.getSelectedValueFromList( "Select bounty", "Choose a bounty to collect:", "Choose this bounty", "Don't choose a bounty", bounties );
			if ( !selectedAValue )
			{
				return;
			}
			
			switch ( wrapper.getChoice() )
			{
			case 1:
				RequestThread.postRequest( new BountyHunterHunterRequest( "takelow" ) );
				break;
			case 2:
				RequestThread.postRequest( new BountyHunterHunterRequest( "takehigh" ) );
				break;
			case 3:
				RequestThread.postRequest( new BountyHunterHunterRequest( "takespecial" ) );
				break;
			}
		}
	}

 	/**
	 * Asks the user to make a selection from a fixed list.  The choices are
	 * presented to the user as radio buttons in a dialog.  Note that this method
	 * only returns an indication of success or failure.  The actual value
	 * selected will be stored in the {@link IntWrapper} assigned to the
	 * choices.
	 *
	 * @param title	a string to show in the titlebar
	 * @param message	a message to preface the list of choices with
	 * @param yesLabel	a label to place on the button denoting confirmation
	 * @param noLabel	a label to place on the button denoting cancellation
	 * @param choices	a list of possible choices
	 * @return	<code>true<code> if a choice was made, <code>false</code> otherwise.
	 */
	private static final boolean getSelectedValueFromList( final String title, final String message, final String yesLabel, final String noLabel, final List choices )
	{
		JPanel choicePanel = new JPanel();
		choicePanel.setLayout( new BoxLayout( choicePanel, BoxLayout.Y_AXIS ) );

		choicePanel.add( new JLabel( message ) );

		ButtonGroup buttonGroup = new ButtonGroup();

		Iterator it = choices.iterator();
		while ( it.hasNext() )
		{
			PossibleSelection c = (PossibleSelection) it.next();

			JRadioButton radio = new JRadioButton( "<html>" + c.getLabel() + "<br>" + c.getDescription() + "</html>" );
			radio.addActionListener( c );

			if ( choicePanel.getComponentCount() < 2 )
				radio.doClick();
			
			choicePanel.add( radio );
			buttonGroup.add( radio );
		}

		String[] dialogOptions = { yesLabel, noLabel };

		int result = JOptionPane.showOptionDialog( null, choicePanel, title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, dialogOptions, null );

		return (result == 0);
	}
	
	private static final String[] buildInformation( String type, String item, int number )
	{
		StringBuffer label = new StringBuffer();
		StringBuffer description = new StringBuffer();

		if ( item == null || item.equals( "" ) )
		{
			label.setLength( 0 );
			label.append( "<b>No " );
			label.append( type );
			label.append( " bounty available." );
			description.setLength( 0 );
		}
		else
		{
			String location = BountyDatabase.getLocation( item );
			if ( location != null )
			{
				KoLAdventure adventure = AdventureDatabase.getAdventure( location );
				if ( adventure != null )
				{
					AreaCombatData locationInfo = adventure.getAreaSummary();

					int totalNumber = BountyDatabase.getNumber( item );
					String plural = BountyDatabase.getPlural( item );
					if ( plural != null )
					{
						label.setLength( 0 );
						label.append( "Get <b>" );
						if ( number != 0 )
						{
							label.append( String.valueOf( totalNumber - number ) );
							label.append( " of " );
						}
						label.append( String.valueOf( totalNumber ) );
						label.append( " " );
						label.append( plural );
						label.append( "</b> from " );
						label.append( adventure.getAdventureName() );

						description.setLength( 0 );
						description.append( "<i>Combat rate: " );
						description.append( String.valueOf( Math.round( locationInfo.areaCombatPercent() ) ) );
						description.append( "%; " );
						description.append( "1/" );
						description.append( String.valueOf( locationInfo.getAvailableMonsterCount() ) );
						description.append( " monsters drop bounty item.</i>" );
					}
				}
			}
		}
		String[] results = new String[2];
		results[ 0 ] = label.toString();
		results[ 1 ] = description.toString();
		return results;
	}
}
