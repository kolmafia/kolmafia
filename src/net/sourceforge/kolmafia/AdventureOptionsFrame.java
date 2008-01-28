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
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

import net.sourceforge.kolmafia.swingui.panel.CustomCombatPanel;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.MoodOptionsPanel;
import net.sourceforge.kolmafia.swingui.panel.RestoreOptionsPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightSpinner;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public abstract class AdventureOptionsFrame
	extends KoLFrame
{
	private ExecuteButton begin;
	private boolean isHandlingConditions = false;

	private JComboBox actionSelect;

	private TreeMap zoneMap;
	private AdventureCountSpinner countField;
	private final LockableListModel matchingAdventures;

	protected JList locationSelect;
	protected JComponent zoneSelect;

	protected KoLAdventure lastAdventure = null;

	protected JCheckBox autoSetCheckBox = new JCheckBox();
	protected AutoHighlightTextField conditionField = new AutoHighlightTextField();

	public AdventureOptionsFrame( final String title )
	{
		super( title );
		this.matchingAdventures = AdventureDatabase.getAsLockableListModel().getMirrorImage();
	}

	public JTabbedPane getSouthernTabs()
	{

		// Components of custom combat and choice adventuring,
		// combined into one friendly panel.

		SimpleScrollPane restoreScroller = new SimpleScrollPane( new RestoreOptionsPanel() );
		JComponentUtilities.setComponentSize( restoreScroller, 560, 400 );

		this.tabs.addTab( "HP/MP Usage", restoreScroller );

		this.tabs.addTab( "Mood Setup", new MoodOptionsPanel() );
		this.tabs.addTab( "Custom Combat", new CustomCombatPanel() );

		return this.tabs;
	}

	/**
	 * An internal class which represents the panel used for adventure selection in the <code>AdventureFrame</code>.
	 */

	public class AdventureSelectPanel
		extends JPanel
	{
		public AdventureSelectPanel( final boolean enableAdventures )
		{
			super( new BorderLayout( 10, 10 ) );

			// West pane is a scroll pane which lists all of the available
			// locations -- to be included is a map on a separate tab.

			AdventureOptionsFrame.this.locationSelect = new JList( AdventureOptionsFrame.this.matchingAdventures );
			AdventureOptionsFrame.this.locationSelect.setVisibleRowCount( 4 );

			JPanel zonePanel = new JPanel( new BorderLayout( 5, 5 ) );

			boolean useZoneComboBox = Preferences.getBoolean( "useZoneComboBox" );
			if ( useZoneComboBox )
			{
				AdventureOptionsFrame.this.zoneSelect = new FilterAdventureComboBox();
				AdventureOptionsFrame.this.matchingAdventures.setFilter( (FilterAdventureComboBox) AdventureOptionsFrame.this.zoneSelect );
			}
			else
			{
				AdventureOptionsFrame.this.zoneSelect = new FilterAdventureField();
			}

			AdventureOptionsFrame.this.zoneMap = new TreeMap();
			Object[] zones = AdventureDatabase.PARENT_LIST.toArray();

			Object currentZone;

			for ( int i = 0; i < zones.length; ++i )
			{
				currentZone = AdventureDatabase.ZONE_DESCRIPTIONS.get( zones[ i ] );
				AdventureOptionsFrame.this.zoneMap.put( currentZone, zones[ i ] );

				if ( useZoneComboBox )
				{
					( (JComboBox) AdventureOptionsFrame.this.zoneSelect ).addItem( currentZone );
				}
			}

			JComponentUtilities.setComponentSize( AdventureOptionsFrame.this.zoneSelect, 200, 24 );
			zonePanel.add( AdventureOptionsFrame.this.zoneSelect, BorderLayout.CENTER );

			if ( enableAdventures )
			{
				AdventureOptionsFrame.this.countField = new AdventureCountSpinner();
				AdventureOptionsFrame.this.countField.setHorizontalAlignment( AutoHighlightTextField.RIGHT );
				JComponentUtilities.setComponentSize( AdventureOptionsFrame.this.countField, 56, 24 );
				zonePanel.add( AdventureOptionsFrame.this.countField, BorderLayout.EAST );
			}

			JPanel locationPanel = new JPanel( new BorderLayout( 5, 5 ) );
			locationPanel.add( zonePanel, BorderLayout.NORTH );
			locationPanel.add( new SimpleScrollPane( AdventureOptionsFrame.this.locationSelect ), BorderLayout.CENTER );

			if ( enableAdventures )
			{
				JPanel locationHolder = new JPanel( new CardLayout( 10, 10 ) );
				locationHolder.add( locationPanel, "" );

				this.add( locationHolder, BorderLayout.WEST );
				this.add( new ObjectivesPanel(), BorderLayout.CENTER );

				AdventureOptionsFrame.this.zoneSelect.addKeyListener( AdventureOptionsFrame.this.begin );
				AdventureOptionsFrame.this.countField.addKeyListener( AdventureOptionsFrame.this.begin );
			}
			else
			{
				this.add( locationPanel, BorderLayout.WEST );
			}
		}
	}

	protected class FilterAdventureField
		extends AutoFilterTextField
	{
		public FilterAdventureField()
		{
			super( AdventureOptionsFrame.this.locationSelect );
		}

		public boolean isVisible( final Object element )
		{
			return ( (KoLAdventure) element ).toLowerCaseString().indexOf( this.text ) != -1;
		}
	}

	protected class FilterAdventureComboBox
		extends JComboBox
		implements ListElementFilter
	{
		private Object selectedZone;

		public void setSelectedItem( final Object element )
		{
			super.setSelectedItem( element );
			this.selectedZone = element;
			AdventureOptionsFrame.this.matchingAdventures.updateFilter( false );
		}

		public boolean isVisible( final Object element )
		{
			return ( (KoLAdventure) element ).getParentZoneDescription().equals( this.selectedZone );
		}
	}

	private class ObjectivesPanel
		extends GenericPanel
	{
		public ObjectivesPanel()
		{
			super( new Dimension( 70, 20 ), new Dimension( 200, 20 ) );

			AdventureOptionsFrame.this.actionSelect = new AutoFilterComboBox( KoLCharacter.getBattleSkillNames(), false );

			AdventureOptionsFrame.this.locationSelect.addListSelectionListener( new ConditionchangeListener() );

			JPanel conditionPanel = new JPanel( new BorderLayout( 5, 5 ) );
			conditionPanel.add( AdventureOptionsFrame.this.conditionField, BorderLayout.CENTER );
			conditionPanel.add( AdventureOptionsFrame.this.autoSetCheckBox, BorderLayout.EAST );

			AdventureOptionsFrame.this.autoSetCheckBox.setSelected( Preferences.getBoolean( "autoSetConditions" ) );
			AdventureOptionsFrame.this.conditionField.setEnabled( Preferences.getBoolean( "autoSetConditions" ) );

			AdventureOptionsFrame.this.addActionListener(
				AdventureOptionsFrame.this.autoSetCheckBox, new EnableObjectivesListener() );

			JPanel buttonWrapper = new JPanel();
			buttonWrapper.add( AdventureOptionsFrame.this.begin = new ExecuteButton() );
			buttonWrapper.add( new InvocationButton( "stop", RequestThread.class, "declareWorldPeace" ) );

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Action:  ", AdventureOptionsFrame.this.actionSelect );
			elements[ 1 ] = new VerifiableElement( "Goals:  ", conditionPanel );

			this.setContent( elements );
			this.container.add( buttonWrapper, BorderLayout.SOUTH );

			JComponentUtilities.addHotKey( this, KeyEvent.VK_ENTER, AdventureOptionsFrame.this.begin );
		}

		public void actionConfirmed()
		{
			String value = (String) AdventureOptionsFrame.this.actionSelect.getSelectedItem();
			if ( value != null )
			{
				Preferences.setString( "battleAction", value  );
			}
		}

		public void actionCancelled()
		{
		}

		public void addStatusLabel()
		{
		}

		public void setEnabled( final boolean isEnabled )
		{
			AdventureOptionsFrame.this.begin.setEnabled( isEnabled );
		}

		private class EnableObjectivesListener
			extends ThreadedListener
		{
			public void run()
			{
				Preferences.setBoolean(
					"autoSetConditions", AdventureOptionsFrame.this.autoSetCheckBox.isSelected() );

				AdventureOptionsFrame.this.conditionField.setEnabled( AdventureOptionsFrame.this.autoSetCheckBox.isSelected() && !KoLmafia.isAdventuring() );
			}
		}
	}

	private class ConditionchangeListener
		implements ListSelectionListener, ListDataListener
	{
		public ConditionchangeListener()
		{
			KoLConstants.conditions.addListDataListener( this );
		}

		public void valueChanged( final ListSelectionEvent e )
		{
			if ( KoLmafia.isAdventuring() )
			{
				return;
			}

			KoLConstants.conditions.clear();
			AdventureOptionsFrame.this.fillCurrentConditions();
		}

		public void intervalAdded( final ListDataEvent e )
		{
			if ( AdventureOptionsFrame.this.isHandlingConditions )
			{
				return;
			}

			AdventureOptionsFrame.this.fillCurrentConditions();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
			if ( AdventureOptionsFrame.this.isHandlingConditions )
			{
				return;
			}

			AdventureOptionsFrame.this.fillCurrentConditions();
		}

		public void contentsChanged( final ListDataEvent e )
		{
			if ( AdventureOptionsFrame.this.isHandlingConditions )
			{
				return;
			}

			AdventureOptionsFrame.this.fillCurrentConditions();
		}
	}

	private class ExecuteButton
		extends ThreadedButton
	{
		private boolean isProcessing = false;

		public ExecuteButton()
		{
			super( "begin" );
			this.setToolTipText( "Start Adventuring" );
		}

		public void run()
		{
			if ( this.isProcessing )
			{
				return;
			}

			KoLmafia.updateDisplay( "Validating adventure sequence..." );

			KoLAdventure request = (KoLAdventure) AdventureOptionsFrame.this.locationSelect.getSelectedValue();
			if ( request == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No location selected." );
				return;
			}

			this.isProcessing = true;

			// If there are conditions in the condition field, be
			// sure to process them.

			if ( KoLConstants.conditions.isEmpty() || AdventureOptionsFrame.this.lastAdventure != null && AdventureOptionsFrame.this.lastAdventure != request )
			{
				Object stats = null;
				int substatIndex = KoLConstants.conditions.indexOf( KoLConstants.tally.get( 2 ) );

				if ( substatIndex != 0 )
				{
					stats = KoLConstants.conditions.get( substatIndex );
				}

				KoLConstants.conditions.clear();

				if ( stats != null )
				{
					KoLConstants.conditions.add( stats );
				}

				AdventureOptionsFrame.this.lastAdventure = request;

				RequestThread.openRequestSequence();
				AdventureOptionsFrame.this.isHandlingConditions = true;
				boolean shouldAdventure = this.handleConditions( request );
				AdventureOptionsFrame.this.isHandlingConditions = false;
				RequestThread.closeRequestSequence();

				if ( !shouldAdventure )
				{
					this.isProcessing = false;
					return;
				}
			}

			int requestCount =
				Math.min(
					KoLFrame.getValue( AdventureOptionsFrame.this.countField, 1 ), KoLCharacter.getAdventuresLeft() );
			AdventureOptionsFrame.this.countField.setValue( requestCount );

			boolean resetCount = requestCount == KoLCharacter.getAdventuresLeft();

			StaticEntity.getClient().makeRequest( request, requestCount );

			if ( resetCount )
			{
				AdventureOptionsFrame.this.countField.setValue( KoLCharacter.getAdventuresLeft() );
			}

			this.isProcessing = false;
		}

		private boolean handleConditions( final KoLAdventure request )
		{
			if ( KoLmafia.isAdventuring() )
			{
				return false;
			}

			String conditionList = AdventureOptionsFrame.this.conditionField.getText().trim().toLowerCase();
			KoLConstants.conditions.clear();

			if ( !AdventureOptionsFrame.this.autoSetCheckBox.isSelected() || conditionList.length() == 0 || conditionList.equalsIgnoreCase( "none" ) )
			{
				return true;
			}

			String[] splitConditions = conditionList.split( "\\s*,\\s*" );

			// First, figure out whether or not you need to do a disjunction
			// on the conditions, which changes how KoLmafia handles them.

			for ( int i = 0; i < splitConditions.length; ++i )
			{
				if ( splitConditions[ i ] == null )
				{
					continue;
				}

				if ( splitConditions[ i ].equals( "check" ) )
				{
					// Postpone verification of conditions
					// until all other conditions added.
				}
				else if ( splitConditions[ i ].equals( "outfit" ) )
				{
					// Determine where you're adventuring and use
					// that to determine which components make up
					// the outfit pulled from that area.

					if ( !EquipmentDatabase.addOutfitConditions( request ) )
					{
						return true;
					}
				}
				else if ( splitConditions[ i ].startsWith( "+" ) )
				{
					if ( !KoLmafiaCLI.DEFAULT_SHELL.executeConditionsCommand( "add " + splitConditions[ i ].substring( 1 ) ) )
					{
						return false;
					}
				}
				else if ( !KoLmafiaCLI.DEFAULT_SHELL.executeConditionsCommand( "set " + splitConditions[ i ] ) )
				{
					return false;
				}
			}

			if ( KoLConstants.conditions.isEmpty() )
			{
				KoLmafia.updateDisplay( "All conditions already satisfied." );
				return false;
			}

			if ( KoLFrame.getValue( AdventureOptionsFrame.this.countField ) == 0 )
			{
				AdventureOptionsFrame.this.countField.setValue( KoLCharacter.getAdventuresLeft() );
			}

			return true;
		}
	}

	public void fillDefaultConditions()
	{
		AdventureOptionsFrame.this.conditionField.setText( AdventureDatabase.getDefaultConditions( (KoLAdventure) this.locationSelect.getSelectedValue() ) );
	}

	public void fillCurrentConditions()
	{
		StringBuffer conditionString = new StringBuffer();

		for ( int i = 0; i < KoLConstants.conditions.getSize(); ++i )
		{
			if ( i > 0 )
			{
				conditionString.append( ", " );
			}

			conditionString.append( ( (AdventureResult) KoLConstants.conditions.getElementAt( i ) ).toConditionString() );
		}

		if ( conditionString.length() == 0 )
		{
			this.fillDefaultConditions();
		}
		else
		{
			this.conditionField.setText( conditionString.toString() );
		}
	}

	protected JPanel getAdventureSummary( final String property, final JList locationSelect )
	{
		int selectedIndex = Preferences.getInteger( property );

		CardLayout resultCards = new CardLayout();
		JPanel resultPanel = new JPanel( resultCards );
		JComboBox resultSelect = new JComboBox();

		int cardCount = 0;

		resultSelect.addItem( "Session Results" );
		resultPanel.add( new SimpleScrollPane( KoLConstants.tally, 4 ), String.valueOf( cardCount++ ) );

		if ( property.startsWith( "default" ) )
		{
			resultSelect.addItem( "Location Details" );
			resultPanel.add( new SafetyField( locationSelect ), String.valueOf( cardCount++ ) );

			resultSelect.addItem( "Conditions Left" );
			resultPanel.add( new SimpleScrollPane( KoLConstants.conditions, 4 ), String.valueOf( cardCount++ ) );
		}

		resultSelect.addItem( "Available Skills" );
		resultPanel.add( new SimpleScrollPane( KoLConstants.availableSkills, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Active Effects" );
		resultPanel.add( new SimpleScrollPane( KoLConstants.activeEffects, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Encounter Listing" );
		resultPanel.add( new SimpleScrollPane( KoLConstants.encounterList, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Visited Locations" );
		resultPanel.add( new SimpleScrollPane( KoLConstants.adventureList, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addActionListener( new ResultSelectListener( resultCards, resultPanel, resultSelect, property ) );

		if ( selectedIndex >= cardCount )
		{
			selectedIndex = cardCount - 1;
		}

		resultSelect.setSelectedIndex( selectedIndex );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( resultSelect, BorderLayout.NORTH );
		containerPanel.add( resultPanel, BorderLayout.CENTER );

		return containerPanel;
	}

	private class ResultSelectListener
		implements ActionListener
	{
		private final String property;
		private final CardLayout resultCards;
		private final JPanel resultPanel;
		private final JComboBox resultSelect;

		public ResultSelectListener( final CardLayout resultCards, final JPanel resultPanel,
			final JComboBox resultSelect, final String property )
		{
			this.resultCards = resultCards;
			this.resultPanel = resultPanel;
			this.resultSelect = resultSelect;
			this.property = property;
		}

		public void actionPerformed( final ActionEvent e )
		{
			String index = String.valueOf( this.resultSelect.getSelectedIndex() );
			this.resultCards.show( this.resultPanel, index );
			Preferences.setString( this.property, index );

		}
	}

	protected class SafetyField
		extends JPanel
		implements Runnable, ListSelectionListener
	{
		private final LimitedSizeChatBuffer safetyText;
		private String savedText = " ";
		private final JList locationSelect;

		public SafetyField( final JList locationSelect )
		{
			super( new BorderLayout() );

			this.safetyText = new LimitedSizeChatBuffer( false );
			this.locationSelect = locationSelect;

			JScrollPane textScroller = this.safetyText.setChatDisplay( new RequestPane() );
			JComponentUtilities.setComponentSize( textScroller, 100, 100 );
			this.add( textScroller, BorderLayout.CENTER );

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( this ) );
			locationSelect.addListSelectionListener( this );

			this.setSafetyString();
		}

		public void run()
		{
			this.setSafetyString();
		}

		public void valueChanged( final ListSelectionEvent e )
		{
			this.setSafetyString();
		}

		private void setSafetyString()
		{
			KoLAdventure request = (KoLAdventure) this.locationSelect.getSelectedValue();

			if ( request == null )
			{
				return;
			}

			AreaCombatData combat = request.getAreaSummary();
			String text = combat == null ? " " : combat.toString( true );

			// Avoid rendering and screen flicker if no change.
			// Compare with our own copy of what we set, since
			// getText() returns a modified version.

			if ( text.equals( this.savedText ) )
			{
				return;
			}

			this.savedText = text;
			this.safetyText.clearBuffer();
			this.safetyText.append( text );
		}
	}

	private class AdventureCountSpinner
		extends AutoHighlightSpinner
		implements ChangeListener
	{
		public AdventureCountSpinner()
		{
			super();
			this.addChangeListener( this );
		}

		public void stateChanged( ChangeEvent e )
		{
			int maximum = KoLCharacter.getAdventuresLeft();
			int desired = KoLFrame.getValue( this, maximum );
			if ( desired == maximum + 1 )
				this.setValue( new Integer( 1 ) );
			else if ( desired <= 0 || desired > maximum )
				this.setValue( new Integer( maximum ) );
		}
	}
}
