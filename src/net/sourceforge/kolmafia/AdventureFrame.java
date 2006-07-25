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

// layout
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

// containers
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

// other imports
import java.util.Date;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extended <code>KoLFrame</code> which presents the user with the ability to
 * adventure in the Kingdom of Loathing.  As the class is developed, it will also
 * provide other adventure-related functionality, such as inventoryManage management
 * and mall purchases.  Its content panel will also change, pending the activity
 * executed at that moment.
 */

public class AdventureFrame extends KoLFrame
{
	private JComboBox locationSelect;
	private JComboBox dropdown1, dropdown2;
	private AdventureSelectPanel adventureSelect;

	/**
	 * Constructs a new <code>AdventureFrame</code>.  All constructed panels
	 * are placed into their corresponding tabs, with the content panel being
	 * defaulted to the adventure selection panel.
	 */

	public AdventureFrame()
	{
		super( "Adventure" );
		tabs = new JTabbedPane();

		// Construct the adventure select container
		// to hold everything related to adventuring.

		JPanel adventureContainer = new JPanel( new BorderLayout( 10, 10 ) );

		this.adventureSelect = new AdventureSelectPanel();

		JPanel southPanel = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
		southPanel.add( getAdventureSummary( StaticEntity.parseInt( getProperty( "defaultDropdown1" ) ) ) );
		southPanel.add( getAdventureSummary( StaticEntity.parseInt( getProperty( "defaultDropdown2" ) ) ) );

		adventureContainer.add( adventureSelect, BorderLayout.NORTH );
		adventureContainer.add( southPanel, BorderLayout.CENTER );

		getContentPane().add( adventureContainer, BorderLayout.CENTER );
	}

	public boolean useSidePane()
	{	return true;
	}

	private JPanel getAdventureSummary( int selectedIndex )
	{
		CardLayout resultCards = new CardLayout();
		JPanel resultPanel = new JPanel( resultCards );
		JComboBox resultSelect = new JComboBox();

		resultSelect.addItem( "Session Results" );
		resultPanel.add( new AdventureResultsPanel( StaticEntity.getClient().getSessionTally() ), "0" );

		resultSelect.addItem( "Location Details" );
		resultPanel.add( new SafetyField(), "1" );

		resultSelect.addItem( "Conditions Left" );
		resultPanel.add( new AdventureResultsPanel( StaticEntity.getClient().getConditions() ), "2" );

		resultSelect.addItem( "Active Effects" );
		resultPanel.add( new AdventureResultsPanel( KoLCharacter.getEffects() ), "3" );

		resultSelect.addItem( "Visited Locations" );
		resultPanel.add( new AdventureResultsPanel( StaticEntity.getClient().getAdventureList() ), "4" );

		resultSelect.addItem( "Encounter Listing" );
		resultPanel.add( new AdventureResultsPanel( StaticEntity.getClient().getEncounterList() ), "5" );

		resultSelect.addActionListener( new ResultSelectListener( resultCards, resultPanel, resultSelect ) );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( resultSelect, BorderLayout.NORTH );
		containerPanel.add( resultPanel, BorderLayout.CENTER );

		if ( dropdown1 == null )
		{
			dropdown1 = resultSelect;
			dropdown1.setSelectedIndex( selectedIndex );
		}
		else
		{
			dropdown2 = resultSelect;
			dropdown2.setSelectedIndex( selectedIndex );
		}

		return containerPanel;
	}

	public void requestFocus()
	{
		super.requestFocus();
		locationSelect.requestFocus();
	}

	private class ResultSelectListener implements ActionListener
	{
		private CardLayout resultCards;
		private JPanel resultPanel;
		private JComboBox resultSelect;

		public ResultSelectListener( CardLayout resultCards, JPanel resultPanel, JComboBox resultSelect )
		{
			this.resultCards = resultCards;
			this.resultPanel = resultPanel;
			this.resultSelect = resultSelect;
		}

		public void actionPerformed( ActionEvent e )
		{
			String index = String.valueOf( resultSelect.getSelectedIndex() );
			resultCards.show( resultPanel, index );
			setProperty( resultSelect == dropdown1 ? "defaultDropdown1" : "defaultDropdown2", index );

		}
	}

	private class SafetyField extends JPanel implements Runnable, ActionListener
	{
		private JLabel safetyText = new JLabel( " " );
		private String savedText = " ";

		public SafetyField()
		{
			super( new BorderLayout() );
			safetyText.setVerticalAlignment( JLabel.TOP );

			JScrollPane textScroller = new JScrollPane( safetyText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

			JComponentUtilities.setComponentSize( textScroller, 100, 100 );
			add( textScroller, BorderLayout.CENTER );

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( this ) );
			locationSelect.addActionListener( this );

			setSafetyString();
		}

		public void run()
		{	setSafetyString();
		}

		public void actionPerformed( ActionEvent e )
		{	setSafetyString();
		}

		private void setSafetyString()
		{
			Runnable request = (Runnable) locationSelect.getSelectedItem();
			if ( request == null )
				return;

			AreaCombatData combat = AdventureDatabase.getAreaCombatData( request.toString() );
			String text = ( combat == null ) ? " " : combat.toString();

			// Avoid rendering and screen flicker if no change.
			// Compare with our own copy of what we set, since
			// getText() returns a modified version.

			if ( !text.equals( savedText ) )
			{
				savedText = text;
				safetyText.setText( text );
			}
		}
	}

	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	private class AdventureSelectPanel extends KoLPanel
	{
		private JComboBox actionSelect;
		private JTextField countField;
		private JTextField conditionField;

		public AdventureSelectPanel()
		{
			super( "begin advs", "stop all", new Dimension( 130, 20 ), new Dimension( 270, 20 ) );

			actionSelect = new JComboBox( KoLCharacter.getBattleSkillNames() );
			LockableListModel adventureList = AdventureDatabase.getAsLockableListModel();

			locationSelect = new JComboBox( adventureList );

			countField = new JTextField();
			conditionField = new JTextField( "none" );

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Location: ", locationSelect );
			elements[1] = new VerifiableElement( "# of Visits: ", countField );
			elements[2] = new VerifiableElement( "Combat Action: ", actionSelect );
			elements[3] = new VerifiableElement( "Objective(s): ", conditionField );

			setContent( elements );
			int actionIndex = KoLCharacter.getBattleSkillIDs().indexOf( getProperty( "battleAction" ) );

			if ( KoLCharacter.getBattleSkillIDs().size() > 0 )
				actionSelect.setSelectedIndex( actionIndex );

			actionSelect.addActionListener( new BattleActionListener() );

			String lastAdventure = getProperty( "lastAdventure" );

			for ( int i = 0; i < adventureList.size(); ++i )
				if ( adventureList.get(i).toString().equals( lastAdventure ) )
					locationSelect.setSelectedItem( adventureList.get(i) );
		}

		private class BattleActionListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( actionSelect.getSelectedIndex() != -1 )
					setProperty( "battleAction", (String) KoLCharacter.getBattleSkillIDs().get( actionSelect.getSelectedIndex() ) );
			}
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( actionSelect == null )
				return;

			super.setEnabled( isEnabled );
			actionSelect.setEnabled( true );
		}

		protected void actionConfirmed()
		{
			// Once the stubs are finished, this will notify the
			// client to begin adventuring based on the values
			// placed in the input fields.

			if ( actionSelect.getSelectedItem() == null )
				DEFAULT_SHELL.executeLine( "set battleAction=attack" );

			Runnable request = (Runnable) locationSelect.getSelectedItem();
			if ( request == null )
				return;

			setProperty( "lastAdventure", request.toString() );

			// If there are conditions in the condition field, be
			// sure to process them.

			String conditionList = conditionField.getText().trim();
			if ( conditionList.equalsIgnoreCase( "none" ) )
				conditionList = "";

			if ( conditionList.length() > 0 )
			{
				DEFAULT_SHELL.executeLine( "conditions clear" );

				boolean verifyConditions = false;
				boolean useDisjunction = false;
				String [] conditions = conditionList.split( "\\s*,\\s*" );

				for ( int i = 0; i < conditions.length; ++i )
				{
					if ( conditions[i].equals( "check" ) )
					{
						// Postpone verification of conditions
						// until all other conditions added.

						verifyConditions = true;
					}
					else if ( conditions[i].equals( "outfit" ) )
					{
						// Determine where you're adventuring and use
						// that to determine which components make up
						// the outfit pulled from that area.

						if ( !(request instanceof KoLAdventure) || !EquipmentDatabase.addOutfitConditions( (KoLAdventure) request ) )
						{
							setStatusMessage( "No outfit corresponds to this zone." );
							return;
						}

						verifyConditions = true;
					}
					else if ( conditions[i].equals( "or" ) || conditions[i].equals( "and" ) || conditions[i].startsWith( "conjunction" ) || conditions[i].startsWith( "disjunction" ) )
					{
						useDisjunction = conditions[i].equals( "or" ) || conditions[i].startsWith( "disjunction" );
					}
					else
					{
						if ( !DEFAULT_SHELL.executeConditionsCommand( "add " + conditions[i] ) )
						{
							KoLmafia.enableDisplay();
							return;
						}
					}
				}

				if ( verifyConditions )
				{
					DEFAULT_SHELL.executeConditionsCommand( "check" );
					if ( StaticEntity.getClient().conditions.isEmpty() )
					{
						KoLmafia.updateDisplay( "All conditions already satisfied." );
						KoLmafia.enableDisplay();
						return;
					}
				}

				if ( StaticEntity.getClient().conditions.size() > 1 )
					DEFAULT_SHELL.executeConditionsCommand( useDisjunction ? "mode disjunction" : "mode conjunction" );

				conditionField.setText( "" );
				if ( countField.getText().equals( "" ) )
					countField.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );
			}

			(new RequestThread( request, getValue( countField, 1 ) )).start();
		}

		protected void actionCancelled()
		{
			KoLmafia.declareWorldPeace();
			locationSelect.requestFocus();
		}

		public void requestFocus()
		{	locationSelect.requestFocus();
		}
	}

	/**
	 * An internal class which represents the panel used for tallying the
	 * results in the <code>AdventureFrame</code>.  Note that all of the
	 * tallying functionality is handled by the <code>LockableListModel</code>
	 * provided, so this functions as a container for that list model.
	 */

	private class AdventureResultsPanel extends JPanel
	{
		public AdventureResultsPanel( LockableListModel resultList )
		{
			setLayout( new BorderLayout() );

			ShowDescriptionList tallyDisplay = new ShowDescriptionList( resultList );
			tallyDisplay.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			tallyDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
			tallyDisplay.setVisibleRowCount( 11 );

			add( new JScrollPane( tallyDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );
		}
	}
}
