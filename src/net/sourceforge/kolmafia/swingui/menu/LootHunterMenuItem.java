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
import net.sourceforge.kolmafia.persistence.ItemDatabase;

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
			// Needs COMPLETE rewrite
			KoLmafia.updateDisplay( MafiaState.ERROR, "This option not currently available due to KoL Bounty Hunter Hunter change." );
			/* GenericRequest hunterRequest = new BountyHunterHunterRequest();
			RequestThread.postRequest( hunterRequest );

			StringBuffer label = new StringBuffer();
			StringBuffer description = new StringBuffer();
			IntWrapper wrapper = new IntWrapper();

			Matcher bountyMatcher = Pattern.compile( "name=whichitem value=(\\d+)" ).matcher( hunterRequest.responseText );
			List bounties = new ArrayList();
			while ( bountyMatcher.find() && bounties.size() < 3 )
			{
				int itemId = StringUtilities.parseInt( bountyMatcher.group( 1 ) );
				String item = ItemDatabase.getItemName( itemId );
				if ( item == null )
				{
					continue;
				}

				KoLAdventure location = AdventureDatabase.getBountyLocation( item );
				if ( location == null )
				{
					continue;
				}

				AdventureResult bountyInfo = AdventureDatabase.getBounty( itemId );
				AreaCombatData locationInfo = location.getAreaSummary();

				label.setLength( 0 );
				label.append( "<b>" );
				label.append( String.valueOf( bountyInfo.getCount() ) );
				label.append( " " );
				label.append( StringUtilities.getEntityEncode( ItemDatabase.getPluralById( itemId ) ) );
				label.append( "</b> from " );
				label.append( location.getAdventureName() );

				description.setLength( 0 );
				description.append( "<i>Combat rate: " );
				description.append( String.valueOf( Math.round( locationInfo.areaCombatPercent() ) ) );
				description.append( "%; " );
				description.append( String.valueOf( locationInfo.countMonstersDroppingItem( itemId ) ) );
				description.append( "/" );
				description.append( String.valueOf( locationInfo.getAvailableMonsterCount() ) );
				description.append( " monsters drop bounty item.</i>" );

				bounties.add( new PossibleSelection( label.toString(), description.toString(), itemId, wrapper ) );
			}

			if ( bounties.isEmpty() )
			{
				int bounty = Preferences.getInteger( "currentBountyItem" );
				if ( hunterRequest.responseText.indexOf( "already turned in a Bounty today" ) != -1 )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "You've already turned in a bounty today." );
					return;
				}

				if ( bounty > 0 )
				{
					KoLAdventure location = AdventureDatabase.getBountyLocation( bounty );
					AdventureFrame.updateSelectedAdventure( location );
					KoLmafia.updateDisplay( MafiaState.ERROR, "You're already hunting " + ItemDatabase.getPluralName( bounty ) + " in " + location.getAdventureName() + "." );
				}
				else
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "You're already on a bounty hunt." );
				}

				return;
			}

			boolean selectedAValue = LootHunterMenuItem.getSelectedValueFromList( "Select bounty", "Choose a bounty to collect:", "Choose this bounty", "Don't choose a bounty", bounties );
			if ( !selectedAValue )
			{
				return;
			}

			RequestThread.postRequest( new BountyHunterHunterRequest( "takebounty", wrapper.getChoice() ) );*/
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
}
