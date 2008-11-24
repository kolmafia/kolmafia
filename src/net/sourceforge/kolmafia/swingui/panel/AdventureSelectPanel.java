/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacterAdapter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.LimitedSizeChatBuffer;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.DailyDeedsPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightSpinner;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class AdventureSelectPanel
	extends JPanel
{
	private ExecuteButton begin;
	private JComboBox actionSelect;

	private boolean isUpdating = false;

	private TreeMap zoneMap;
	private AdventureCountSpinner countField;
	private final LockableListModel matchingAdventures;

	private JList locationSelect;
	private JComponent zoneSelect;

	private final LockableListModel locationConditions = new LockableListModel();
	private JCheckBox conditionsFieldActive = new JCheckBox();
	private ConditionsComboBox conditionField = new ConditionsComboBox();

	public AdventureSelectPanel( final boolean enableAdventures )
	{
		super( new BorderLayout( 10, 10 ) );

		this.matchingAdventures = AdventureDatabase.getAsLockableListModel().getMirrorImage();

		// West pane is a scroll pane which lists all of the available
		// locations -- to be included is a map on a separate tab.

		this.locationSelect = new JList( this.matchingAdventures );
		this.locationSelect.setVisibleRowCount( 4 );

		JPanel zonePanel = new JPanel( new BorderLayout( 5, 5 ) );

		boolean useZoneComboBox = Preferences.getBoolean( "useZoneComboBox" );
		if ( useZoneComboBox )
		{
			this.zoneSelect = new FilterAdventureComboBox();
			this.matchingAdventures.setFilter( (FilterAdventureComboBox) this.zoneSelect );
		}
		else
		{
			this.zoneSelect = new AutoFilterTextField( AdventureSelectPanel.this.locationSelect );
		}

		this.zoneMap = new TreeMap();
		Object[] zones = AdventureDatabase.PARENT_LIST.toArray();

		Object currentZone;

		for ( int i = 0; i < zones.length; ++i )
		{
			currentZone = AdventureDatabase.ZONE_DESCRIPTIONS.get( zones[ i ] );
			this.zoneMap.put( currentZone, zones[ i ] );

			if ( useZoneComboBox )
			{
				( (JComboBox) this.zoneSelect ).addItem( currentZone );
			}
		}

		JComponentUtilities.setComponentSize( this.zoneSelect, 200, 24 );
		zonePanel.add( this.zoneSelect, BorderLayout.CENTER );

		if ( enableAdventures )
		{
			this.countField = new AdventureCountSpinner();
			this.countField.setHorizontalAlignment( AutoHighlightTextField.RIGHT );
			JComponentUtilities.setComponentSize( this.countField, 56, 24 );
			zonePanel.add( this.countField, BorderLayout.EAST );
		}

		JPanel locationPanel = new JPanel( new BorderLayout( 5, 5 ) );
		locationPanel.add( zonePanel, BorderLayout.NORTH );
		locationPanel.add( new GenericScrollPane( this.locationSelect ), BorderLayout.CENTER );

		if ( enableAdventures )
		{
			JPanel locationHolder = new JPanel( new CardLayout( 10, 10 ) );
			locationHolder.add( locationPanel, "" );

			this.add( locationHolder, BorderLayout.WEST );
			this.add( new ObjectivesPanel(), BorderLayout.CENTER );

			this.zoneSelect.addKeyListener( this.begin );
			this.countField.addKeyListener( this.begin );
		}
		else
		{
			this.add( locationPanel, BorderLayout.WEST );
		}
	}

	public void updateFromPreferences()
	{
                if ( AdventureSelectPanel.this.actionSelect != null )
                {
                        String battleAction = Preferences.getString( "battleAction" );
                        int battleIndex = KoLCharacter.getBattleSkillNames().indexOf( battleAction );
                        KoLCharacter.getBattleSkillNames().setSelectedIndex( battleIndex == -1 ? 0 : battleIndex );
                }
	}

	public void updateSelectedAdventure( final KoLAdventure location )
	{
		if ( location == null )
		{
			return;
		}

		if ( !KoLConstants.conditions.isEmpty() )
		{
			return;
		}

		if ( this.zoneSelect instanceof AutoFilterTextField )
		{
			( (AutoFilterTextField) this.zoneSelect ).setText( location.getZone() );
		}
		else
		{
			( (JComboBox) this.zoneSelect ).setSelectedItem( location.getParentZoneDescription() );
		}

		if ( this.locationSelect.getSelectedValue() == location && Preferences.getInteger( "currentBountyItem" ) == 0 )
		{
			return;
		}

		this.locationSelect.setSelectedValue( location, true );
		this.locationSelect.ensureIndexIsVisible( this.locationSelect.getSelectedIndex() );
	}

	public void addSelectedLocationListener( ListSelectionListener listener )
	{
		this.locationSelect.addListSelectionListener( listener );
	}

	private class FilterAdventureComboBox
		extends JComboBox
		implements ListElementFilter
	{
		private Object selectedZone;

		public void setSelectedItem( final Object element )
		{
			super.setSelectedItem( element );
			this.selectedZone = element;
			AdventureSelectPanel.this.matchingAdventures.updateFilter( false );
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

			AdventureSelectPanel.this.actionSelect = new AutoFilterComboBox( KoLCharacter.getBattleSkillNames(), false );

			AdventureSelectPanel.this.locationSelect.addListSelectionListener( new ConditionChangeListener() );

			JPanel conditionPanel = new JPanel( new BorderLayout( 5, 5 ) );
			conditionPanel.add( AdventureSelectPanel.this.conditionField, BorderLayout.CENTER );
			conditionPanel.add( AdventureSelectPanel.this.conditionsFieldActive, BorderLayout.EAST );

			AdventureSelectPanel.this.conditionsFieldActive.setSelected( Preferences.getBoolean( "autoSetConditions" ) );
			AdventureSelectPanel.this.conditionField.setEnabled( Preferences.getBoolean( "autoSetConditions" ) );

			AdventureSelectPanel.this.conditionsFieldActive.addActionListener( new EnableObjectivesListener() );

			JPanel buttonWrapper = new JPanel();
			buttonWrapper.add( AdventureSelectPanel.this.begin = new ExecuteButton() );
			buttonWrapper.add( new InvocationButton( "stop", RequestThread.class, "declareWorldPeace" ) );

			VerifiableElement[] elements = new VerifiableElement[ 2 ];
			elements[ 0 ] = new VerifiableElement( "Action:  ", AdventureSelectPanel.this.actionSelect );
			elements[ 1 ] = new VerifiableElement( "Goals:  ", conditionPanel );

			this.setContent( elements );
			this.container.add( buttonWrapper, BorderLayout.SOUTH );

			JComponentUtilities.addHotKey( this, KeyEvent.VK_ENTER, AdventureSelectPanel.this.begin );
		}

		public void actionConfirmed()
		{
			String value = (String) AdventureSelectPanel.this.actionSelect.getSelectedItem();
			if ( value != null )
			{
				Preferences.setString( "battleAction", value );
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
			if ( AdventureSelectPanel.this.begin != null )
			{
				AdventureSelectPanel.this.begin.setEnabled( isEnabled );
			}
		}

		private class EnableObjectivesListener
			extends ThreadedListener
		{
			public void run()
			{
				Preferences.setBoolean(
					"autoSetConditions", AdventureSelectPanel.this.conditionsFieldActive.isSelected() );

				AdventureSelectPanel.this.conditionField.setEnabled( AdventureSelectPanel.this.conditionsFieldActive.isSelected() && !KoLmafia.isAdventuring() );
			}
		}
	}

	private boolean isUpdating()
	{
		return ( KoLmafia.isAdventuring() && KoLConstants.conditions.isEmpty() ) || this.isUpdating;
	}

	private class ConditionChangeListener
		implements ListSelectionListener, ListDataListener
	{
		public ConditionChangeListener()
		{
			KoLConstants.conditions.addListDataListener( this );
		}

		public void valueChanged( final ListSelectionEvent e )
		{
			if ( AdventureSelectPanel.this.isUpdating() )
			{
				return;
			}

			KoLConstants.conditions.clear();
			AdventureSelectPanel.this.fillCurrentConditions();
		}

		public void intervalAdded( final ListDataEvent e )
		{
			if ( AdventureSelectPanel.this.isUpdating() )
			{
				return;
			}

			AdventureSelectPanel.this.fillCurrentConditions();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
			if ( AdventureSelectPanel.this.isUpdating() )
			{
				return;
			}

			AdventureSelectPanel.this.fillCurrentConditions();
		}

		public void contentsChanged( final ListDataEvent e )
		{
			if ( AdventureSelectPanel.this.isUpdating() )
			{
				return;
			}

			AdventureSelectPanel.this.fillCurrentConditions();
		}
	}

	private class ExecuteButton
		extends ThreadedButton
	{
		public ExecuteButton()
		{
			super( "begin" );
			this.setToolTipText( "Start Adventuring" );
		}

		public void run()
		{
			KoLmafia.updateDisplay( "Validating adventure sequence..." );

			KoLAdventure request = (KoLAdventure) AdventureSelectPanel.this.locationSelect.getSelectedValue();
			if ( request == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No location selected." );
				return;
			}

			RequestThread.openRequestSequence();
			AdventureSelectPanel.this.isUpdating = true;

			// If there are conditions in the condition field, be
			// sure to process them.

			boolean conditionsActive = AdventureSelectPanel.this.conditionsFieldActive.isSelected();
			String conditionList = ((String) AdventureSelectPanel.this.conditionField.getText()).trim().toLowerCase();

			Object stats = null;
			int substatIndex = KoLConstants.conditions.indexOf( KoLConstants.tally.get( 2 ) );

			if ( substatIndex != 0 )
			{
				stats = KoLConstants.conditions.get( substatIndex );
			}

			if ( conditionsActive )
			{
				KoLConstants.conditions.clear();
			}

			if ( stats != null )
			{
				KoLConstants.conditions.add( stats );
			}

			boolean shouldAdventure = true;

			if ( conditionsActive && conditionList.length() > 0 && !conditionList.equals( "none" ) )
			{
				shouldAdventure = this.handleConditions( conditionList, request );
			}

			AdventureSelectPanel.this.isUpdating = false;
			RequestThread.closeRequestSequence();

			if ( !shouldAdventure )
			{
				return;
			}

			int requestCount =
				Math.min(
					InputFieldUtilities.getValue( AdventureSelectPanel.this.countField, 1 ), KoLCharacter.getAdventuresLeft() );

			AdventureSelectPanel.this.countField.setValue( requestCount );
			boolean resetCount = requestCount == KoLCharacter.getAdventuresLeft();

			StaticEntity.getClient().makeRequest( request, requestCount );

			if ( resetCount )
			{
				AdventureSelectPanel.this.countField.setValue( KoLCharacter.getAdventuresLeft() );
			}
		}

		private boolean handleConditions( final String conditionList, final KoLAdventure request )
		{
			if ( KoLmafia.isAdventuring() )
			{
				return false;
			}

			KoLConstants.conditions.clear();

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

					if ( !EquipmentManager.addOutfitConditions( request ) )
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

			if ( InputFieldUtilities.getValue( AdventureSelectPanel.this.countField ) == 0 )
			{
				AdventureSelectPanel.this.countField.setValue( KoLCharacter.getAdventuresLeft() );
			}

			return true;
		}
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

		String text;

		if ( conditionString.length() > 0 )
		{
			text = conditionString.toString();
		}
		else
		{
			KoLAdventure location = (KoLAdventure) this.locationSelect.getSelectedValue();
			AdventureDatabase.getDefaultConditionsList( location, this.locationConditions );
			text = (String) this.locationConditions.get(0);
		}

                this.conditionField.setText( text );
	}

	public JPanel getAdventureSummary( final String property )
	{
		int selectedIndex = Preferences.getInteger( property );

		CardLayout resultCards = new CardLayout();
		JPanel resultPanel = new JPanel( resultCards );
		JComboBox resultSelect = new JComboBox();

		int cardCount = 0;

		resultSelect.addItem( "Session Results" );
		resultPanel.add( new GenericScrollPane( KoLConstants.tally, 4 ), String.valueOf( cardCount++ ) );

		if ( property.startsWith( "default" ) )
		{
			resultSelect.addItem( "Location Details" );
			resultPanel.add( new SafetyField(), String.valueOf( cardCount++ ) );

			resultSelect.addItem( "Conditions Left" );
			resultPanel.add( new GenericScrollPane( KoLConstants.conditions, 4 ), String.valueOf( cardCount++ ) );
		}

		resultSelect.addItem( "Available Skills" );
		resultPanel.add( new GenericScrollPane( KoLConstants.availableSkills, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Active Effects" );
		resultPanel.add( new GenericScrollPane( KoLConstants.activeEffects, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Encounter Listing" );
		resultPanel.add( new GenericScrollPane( KoLConstants.encounterList, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Visited Locations" );
		resultPanel.add( new GenericScrollPane( KoLConstants.adventureList, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Daily Deeds" );
		resultPanel.add( new GenericScrollPane( new DailyDeedsPanel() ),
			String.valueOf( cardCount++ ) );

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

	private class SafetyField
		extends JPanel
		implements Runnable, ListSelectionListener
	{
		private final LimitedSizeChatBuffer safetyText;
		private String savedText = " ";

		public SafetyField()
		{
			super( new BorderLayout() );

			this.safetyText = new LimitedSizeChatBuffer( false );

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
			KoLAdventure request = (KoLAdventure) AdventureSelectPanel.this.locationSelect.getSelectedValue();

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

	private class ConditionsComboBox
		extends AutoFilterComboBox
	{
		public ConditionsComboBox()
		{
			super( AdventureSelectPanel.this.locationConditions, true );
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
			if ( maximum == 0 )
			{
				this.setValue( new Integer( 0 ) );
				return;
			}

			int desired = InputFieldUtilities.getValue( this, maximum );
			if ( desired == maximum + 1 )
				this.setValue( new Integer( 1 ) );
			else if ( desired <= 0 || desired > maximum )
				this.setValue( new Integer( maximum ) );
		}
	}
}
