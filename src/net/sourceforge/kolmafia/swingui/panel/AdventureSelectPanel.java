/**
 * Copyright (c) 2005-2011, KoLmafia development team
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

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
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightSpinner;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;

import net.sourceforge.kolmafia.textui.command.ConditionsCommand;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class AdventureSelectPanel
	extends JPanel
{
	private ThreadedButton begin;
	private JComboBox actionSelect;

	private final TreeMap zoneMap;
	private AdventureCountSpinner countField;
	private final LockableListModel matchingAdventures;

	private final JList locationSelect;
	private JComponent zoneSelect;

	private final LockableListModel locationConditions = new LockableListModel();
	private final JCheckBox conditionsFieldActive = new JCheckBox();
	private final ConditionsComboBox conditionField = new ConditionsComboBox();
	private SafetyField safetyField = null;

	private static ImageIcon stealImg, entangleImg;
	private static ImageIcon potionImg, sphereImg, olfactImg, puttyImg;
	private static ImageIcon antidoteImg, restoreImg;
	static
	{
		AdventureSelectPanel.stealImg = AdventureSelectPanel.getImage( "knobsack.gif" );
		AdventureSelectPanel.entangleImg = AdventureSelectPanel.getImage( "entnoodles.gif" );
		AdventureSelectPanel.potionImg = AdventureSelectPanel.getImage( "exclam.gif" );
		AdventureSelectPanel.sphereImg = AdventureSelectPanel.getImage( "spherecrack.gif" );
		AdventureSelectPanel.olfactImg = AdventureSelectPanel.getImage( "footprints.gif" );
		AdventureSelectPanel.puttyImg = AdventureSelectPanel.getImage( "sputtycopy.gif" );
		AdventureSelectPanel.antidoteImg = AdventureSelectPanel.getImage( "poisoncup.gif" );
		AdventureSelectPanel.restoreImg = AdventureSelectPanel.getImage( "mp.gif" );
	}

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

		JComponentUtilities.setComponentSize( this.zoneSelect, 200, -1 );
		zonePanel.add( this.zoneSelect, BorderLayout.CENTER );

		if ( enableAdventures )
		{
			this.countField = new AdventureCountSpinner();
			this.countField.setHorizontalAlignment( AutoHighlightTextField.RIGHT );
			JComponentUtilities.setComponentSize( this.countField, 56, -1 );
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

		GoalManager.clearGoals();

		String pref = Preferences.getString( "lastAdventure" );
		KoLAdventure location = AdventureDatabase.getAdventure( pref );

		this.updateSelectedAdventure( location );
	}

	public void updateSafetyDetails()
	{
		if ( this.safetyField != null )
		{
			this.safetyField.run();
		}
	}

	public void updateSelectedAdventure( final KoLAdventure location )
	{
		if ( location == null )
		{
			return;
		}

		if ( GoalManager.hasGoals() )
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

	public void addSelectedLocationListener( final ListSelectionListener listener )
	{
		this.locationSelect.addListSelectionListener( listener );
	}

	private static ImageIcon getImage( final String filename )
	{
		FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + filename );
		return JComponentUtilities.getImage( "itemimages/" + filename );
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

	public static final int[] POISON_ID = {
		0,
		EffectPool.TOAD_IN_THE_HOLE_ID,
		EffectPool.MAJORLY_POISONED_ID,
		EffectPool.REALLY_QUITE_POISONED_ID,
		EffectPool.SOMEWHAT_POISONED_ID,
		EffectPool.A_LITTLE_BIT_POISONED_ID,
		EffectPool.HARDLY_POISONED_AT_ALL_ID
	};

	public static int getPoisonLevel( String text )
	{
		text = text.toLowerCase();
		if ( text.indexOf( "toad in the hole" ) != -1 )
		{
			return 1;
		}
		if ( text.indexOf( "poisoned" ) == -1 )
		{
			return Integer.MAX_VALUE;
		}
		if ( text.indexOf( "majorly poisoned" ) != -1 )
		{
			return 2;
		}
		if ( text.indexOf( "really quite poisoned" ) != -1 )
		{
			return 3;
		}
		if ( text.indexOf( "somewhat poisoned" ) != -1 )
		{
			return 4;
		}
		if ( text.indexOf( "a little bit poisoned" ) != -1 )
		{
			return 5;
		}
		if ( text.indexOf( "hardly poisoned at all" ) != -1 )
		{
			return 6;
		}
		return Integer.MAX_VALUE;
	}

	private class ObjectivesPanel
		extends GenericPanel
		implements PreferenceListener
	{
		private final JPanel special;
		private final JPopupMenu specialPopup;

		private final JLabel stealLabel, entangleLabel;
		private final JLabel potionLabel, sphereLabel, olfactLabel, puttyLabel;
		private final JLabel antidoteLabel, restoreLabel;

		private final JCheckBoxMenuItem stealItem, entangleItem;
		private final JCheckBoxMenuItem potionItem, sphereItem, olfactItem, puttyItem;
		private final JCheckBoxMenuItem restoreItem;
		private final JMenu poisonItem;

		public ObjectivesPanel()
		{
			super( new Dimension( 70, -1 ), new Dimension( 200, -1 ) );

			AdventureSelectPanel.this.actionSelect = new AutoFilterComboBox( KoLCharacter.getBattleSkillNames(), false );

			AdventureSelectPanel.this.locationSelect.addListSelectionListener( new ConditionChangeListener() );

			JPanel conditionPanel = new JPanel( new BorderLayout( 5, 5 ) );
			conditionPanel.add( AdventureSelectPanel.this.conditionField, BorderLayout.CENTER );
			conditionPanel.add( AdventureSelectPanel.this.conditionsFieldActive, BorderLayout.EAST );

			AdventureSelectPanel.this.conditionsFieldActive.setSelected( Preferences.getBoolean( "autoSetConditions" ) );
			AdventureSelectPanel.this.conditionField.setEnabled( Preferences.getBoolean( "autoSetConditions" ) );

			AdventureSelectPanel.this.conditionsFieldActive.addActionListener( new EnableObjectivesListener() );

			JPanel buttonWrapper = new JPanel();

			AdventureSelectPanel.this.begin = new ThreadedButton( "begin", new ExecuteRunnable() );
			AdventureSelectPanel.this.begin.setToolTipText( "Start Adventuring" );

			buttonWrapper.add( AdventureSelectPanel.this.begin );
			buttonWrapper.add( new InvocationButton( "stop now", RequestThread.class, "declareWorldPeace" ) );
			buttonWrapper.add( new StopButton() );

			JPanel special = new JPanel( new FlowLayout( FlowLayout.LEADING, 0, 0 ) );
			this.special = special;
			special.setBackground( Color.WHITE );
			special.setBorder( BorderFactory.createLoweredBevelBorder() );
			MouseListener listener = new SpecialPopListener();
			special.addMouseListener( listener );

			this.stealLabel =
				this.label(
					special, listener, AdventureSelectPanel.stealImg,
					"Pickpocketing will be tried (if appropriate) with non-CCS actions." );
			this.entangleLabel =
				this.label(
					special, listener, AdventureSelectPanel.entangleImg,
					"Entangling Noodles will be cast before non-CCS actions." );
			this.olfactLabel = this.label( special, listener, AdventureSelectPanel.olfactImg, null );
			this.puttyLabel = this.label( special, listener, AdventureSelectPanel.puttyImg, null );
			this.sphereLabel =
				this.label(
					special,
					listener,
					AdventureSelectPanel.sphereImg,
					"<html>Hidden City spheres will be identified by using them in combat.<br>Requires 'special' action if a CCS is used.</html>" );
			this.potionLabel =
				this.label(
					special,
					listener,
					AdventureSelectPanel.potionImg,
					"<html>Dungeons of Doom potions will be identified by using them in combat.<br>Requires 'special' action if a CCS is used.</html>" );
			this.antidoteLabel = this.label( special, listener, AdventureSelectPanel.antidoteImg, null );
			this.restoreLabel =
				this.label(
					special, listener, AdventureSelectPanel.restoreImg, "MP restores will be used in combat if needed." );

			this.specialPopup = new JPopupMenu( "Special Actions" );
			this.stealItem = this.checkbox( this.specialPopup, listener, "Pickpocket before simple actions" );
			this.entangleItem = this.checkbox( this.specialPopup, listener, "Cast Noodles before simple actions" );
			this.specialPopup.addSeparator();

			this.olfactItem = this.checkbox( this.specialPopup, listener, "One-time automatic Olfaction..." );
			this.puttyItem =
				this.checkbox( this.specialPopup, listener, "One-time automatic Spooky Putty/4-d camera..." );
			this.sphereItem = this.checkbox( this.specialPopup, listener, "Identify stone spheres" );
			this.potionItem = this.checkbox( this.specialPopup, listener, "Identify bang potions" );
			this.specialPopup.addSeparator();

			this.poisonItem = new JMenu( "Minimum poison level for antidote use" );
			ButtonGroup group = new ButtonGroup();
			this.poison( this.poisonItem, group, listener, "No automatic use" );
			this.poison( this.poisonItem, group, listener, "Toad In The Hole (-\u00BDHP/round)" );
			this.poison( this.poisonItem, group, listener, "Majorly Poisoned (-90%, -11)" );
			this.poison( this.poisonItem, group, listener, "Really Quite Poisoned (-70%, -9)" );
			this.poison( this.poisonItem, group, listener, "Somewhat Poisoned (-50%, -7)" );
			this.poison( this.poisonItem, group, listener, "A Little Bit Poisoned (-30%, -5)" );
			this.poison( this.poisonItem, group, listener, "Hardly Poisoned at All (-10%, -3)" );
			this.specialPopup.add( this.poisonItem );
			this.restoreItem = this.checkbox( this.specialPopup, listener, "Restore MP in combat" );

			VerifiableElement[] elements = new VerifiableElement[ 3 ];
			elements[ 0 ] = new VerifiableElement( "Action:  ", AdventureSelectPanel.this.actionSelect );
			elements[ 1 ] = new VerifiableElement( "Special:  ", special );
			elements[ 2 ] = new VerifiableElement( "Goals:  ", conditionPanel );

			this.setContent( elements );
			( (BorderLayout) this.container.getLayout() ).setHgap( 0 );
			( (BorderLayout) this.container.getLayout() ).setVgap( 0 );
			this.container.add( buttonWrapper, BorderLayout.SOUTH );

			JComponentUtilities.addHotKey( this, KeyEvent.VK_ENTER, AdventureSelectPanel.this.begin );

			PreferenceListenerRegistry.registerListener( "autoSteal", this );
			PreferenceListenerRegistry.registerListener( "autoEntangle", this );
			PreferenceListenerRegistry.registerListener( "autoOlfact", this );
			PreferenceListenerRegistry.registerListener( "autoPutty", this );
			PreferenceListenerRegistry.registerListener( "autoSphereID", this );
			PreferenceListenerRegistry.registerListener( "autoPotionID", this );
			PreferenceListenerRegistry.registerListener( "autoAntidote", this );
			PreferenceListenerRegistry.registerListener( "autoManaRestore", this );
			PreferenceListenerRegistry.registerListener( "(skill)", this );
			this.update();
		}

		public void update()
		{
			if ( KoLCharacter.hasSkill( "Entangling Noodles" ) )
			{
				this.entangleItem.setEnabled( true );
			}
			else
			{
				this.entangleItem.setEnabled( false );
				Preferences.setBoolean( "autoEntangle", false );
			}

			String text;
			boolean pref;
			pref = Preferences.getBoolean( "autoSteal" );
			this.stealLabel.setVisible( pref );
			this.stealItem.setSelected( pref );
			pref = Preferences.getBoolean( "autoEntangle" );
			this.entangleLabel.setVisible( pref );
			this.entangleItem.setSelected( pref );
			text = Preferences.getString( "autoOlfact" );
			pref = text.length() > 0;
			this.olfactLabel.setVisible( pref );
			this.olfactItem.setSelected( pref );
			this.olfactLabel.setToolTipText( "<html>Automatic Olfaction or odor extractor use: " + text + "<br>Requires 'special' action if a CCS is used.</html>" );
			text = Preferences.getString( "autoPutty" );
			pref = text.length() > 0;
			this.puttyLabel.setVisible( pref );
			this.puttyItem.setSelected( pref );
			this.puttyLabel.setToolTipText( "<html>Automatic Spooky Putty sheet, 4-d camera or portable photocopier use: " + text + "<br>Requires 'special' action if a CCS is used.</html>" );
			pref = Preferences.getBoolean( "autoSphereID" );
			this.sphereLabel.setVisible( pref );
			this.sphereItem.setSelected( pref );
			pref = Preferences.getBoolean( "autoPotionID" );
			this.potionLabel.setVisible( pref );
			this.potionItem.setSelected( pref );
			int antidote = Preferences.getInteger( "autoAntidote" );
			this.antidoteLabel.setVisible( antidote > 0 );
			if ( antidote >= 0 && antidote < this.poisonItem.getMenuComponentCount() )
			{
				JRadioButtonMenuItem option = (JRadioButtonMenuItem) this.poisonItem.getMenuComponent( antidote );
				option.setSelected( true );
				this.antidoteLabel.setToolTipText( "Anti-anti-antidote will be used in combat if you get " + option.getText() + " or worse." );
			}
			pref = Preferences.getBoolean( "autoManaRestore" );
			this.restoreLabel.setVisible( pref );
			this.restoreItem.setSelected( pref );
		}

		private JLabel label( final JPanel special, final MouseListener listener, final ImageIcon img,
			final String toolTip )
		{
			JLabel rv = new JLabel( img );
			rv.setToolTipText( toolTip );
			rv.addMouseListener( listener );
			special.add( rv );
			return rv;
		}

		private JCheckBoxMenuItem checkbox( final JPopupMenu menu, final Object listener, final String text )
		{
			JCheckBoxMenuItem rv = new JCheckBoxMenuItem( text );
			menu.add( rv );
			rv.addItemListener( (ItemListener) listener );
			return rv;
		}

		private void poison( final JMenu menu, final ButtonGroup group, final Object listener, final String text )
		{
			JRadioButtonMenuItem rb = new JRadioButtonMenuItem( text );
			menu.add( rb );
			group.add( rb );
			rb.addItemListener( (ItemListener) listener );
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
			protected void execute()
			{
				Preferences.setBoolean(
					"autoSetConditions", AdventureSelectPanel.this.conditionsFieldActive.isSelected() );

				AdventureSelectPanel.this.conditionField.setEnabled( AdventureSelectPanel.this.conditionsFieldActive.isSelected() && !KoLmafia.isAdventuring() );
			}
		}

		private class SpecialPopListener
			extends MouseAdapter
			implements ItemListener
		{
			public void mousePressed( final MouseEvent e )
			{
				ObjectivesPanel.this.specialPopup.show( ObjectivesPanel.this.special, 0, 32 );
			}

			public void itemStateChanged( final ItemEvent e )
			{
				boolean state = e.getStateChange() == ItemEvent.SELECTED;
				JMenuItem source = (JMenuItem) e.getItemSelectable();
				if ( source == ObjectivesPanel.this.stealItem )
				{
					Preferences.setBoolean( "autoSteal", state );
				}
				else if ( source == ObjectivesPanel.this.entangleItem )
				{
					Preferences.setBoolean( "autoEntangle", state );
				}
				else if ( source == ObjectivesPanel.this.olfactItem )
				{
					if ( state == !Preferences.getString( "autoOlfact" ).equals( "" ) )
					{ // pref already set externally, don't prompt
						return;
					}
					String option =
						!state ? null : InputFieldUtilities.input(
							"Use Transcendent Olfaction or odor extractor when? (item, \"goals\", or \"monster\" plus name; add \"abort\" to stop adventuring)",
							"goals" );

					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "olfact", option == null ? "none" : option );
				}
				else if ( source == ObjectivesPanel.this.puttyItem )
				{
					if ( state == !Preferences.getString( "autoPutty" ).equals( "" ) )
					{ // pref already set externally, don't prompt
						return;
					}
					String option =
						!state ? null : InputFieldUtilities.input(
							"Use Spooky Putty sheet, 4-d camera or portable photocopier when? (item, \"goals\", or \"monster\" plus name; add \"abort\" to stop adventuring)",
							"goals abort" );

					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "putty", option == null ? "none" : option );
				}
				else if ( source == ObjectivesPanel.this.sphereItem )
				{
					Preferences.setBoolean( "autoSphereID", state );
				}
				else if ( source == ObjectivesPanel.this.potionItem )
				{
					Preferences.setBoolean( "autoPotionID", state );
				}
				else if ( source == ObjectivesPanel.this.restoreItem )
				{
					Preferences.setBoolean( "autoManaRestore", state );
				}
				else if ( source instanceof JRadioButtonMenuItem )
				{
					Preferences.setInteger( "autoAntidote", Arrays.asList(
						ObjectivesPanel.this.poisonItem.getMenuComponents() ).indexOf( source ) );
				}
			}
		}
	}

	private class ConditionChangeListener
		implements ListSelectionListener, ListDataListener
	{
		public ConditionChangeListener()
		{
			GoalManager.getGoals().addListDataListener( this );
			AdventureSelectPanel.this.fillDefaultConditions();
		}

		public void valueChanged( final ListSelectionEvent e )
		{
			if ( KoLmafia.isAdventuring() )
			{
				return;
			}

			AdventureSelectPanel.this.fillDefaultConditions();
		}

		public void intervalAdded( final ListDataEvent e )
		{
			AdventureSelectPanel.this.fillCurrentConditions();
		}

		public void intervalRemoved( final ListDataEvent e )
		{
			AdventureSelectPanel.this.fillCurrentConditions();
		}

		public void contentsChanged( final ListDataEvent e )
		{
			AdventureSelectPanel.this.fillCurrentConditions();
		}
	}

	private class StopButton
		extends JButton
		implements ActionListener
	{
		public StopButton()
		{
			super( "stop after" );
			this.addActionListener( this );
			this.setToolTipText( "Stop after current adventure" );
		}

		public void actionPerformed( final ActionEvent e )
		{
			KoLmafia.abortAfter( "Manual stop requested." );
		}
	}

	private class ExecuteRunnable
		implements Runnable
	{
		public void run()
		{
			KoLmafia.updateDisplay( "Validating adventure sequence..." );

			KoLAdventure request = (KoLAdventure) AdventureSelectPanel.this.locationSelect.getSelectedValue();
			if ( request == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No location selected." );
				return;
			}

			// If there are conditions in the condition field, be
			// sure to process them.

			boolean conditionsActive = AdventureSelectPanel.this.conditionsFieldActive.isSelected();
			String text = ( (String) AdventureSelectPanel.this.conditionField.getText() );
			String conditionList = text == null ? "" : text.trim().toLowerCase();

			List previousGoals = new ArrayList( GoalManager.getGoals() );
			GoalManager.clearGoals();

			// Retain any stat goal
			for ( int i = 0; i < previousGoals.size(); ++i )
			{
				AdventureResult previousGoal = (AdventureResult) previousGoals.get( i );

				if ( previousGoal.getName().equals( AdventureResult.SUBSTATS ) )
				{
					GoalManager.addGoal( previousGoal );
					break;
				}
			}

			boolean shouldAdventure = true;

			if ( conditionsActive && conditionList.length() > 0 && !conditionList.equals( "none" ) )
			{
				shouldAdventure = this.handleConditions( conditionList, request );
			}

			if ( !shouldAdventure )
			{
				return;
			}

			int requestCount =
				Math.min(
					InputFieldUtilities.getValue( AdventureSelectPanel.this.countField, 1 ),
					KoLCharacter.getAdventuresLeft() );

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

			GoalManager.clearGoals();

			String[] splitConditions = conditionList.split( "\\s*,\\s*" );

			// First, figure out whether or not you need to do a
			// disjunction on the conditions, which changes how
			// KoLmafia handles them.

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
					// Determine where you're adventuring
					// and use that to determine which
					// components make up the outfit pulled
					// from that area.

					if ( !EquipmentManager.addOutfitConditions( request ) )
					{
						return true;
					}
				}
				else if ( splitConditions[ i ].startsWith( "+" ) )
				{
					if ( !ConditionsCommand.update( "add", splitConditions[ i ].substring( 1 ) ) )
					{
						return false;
					}
				}
				else if ( !ConditionsCommand.update( "set", splitConditions[ i ] ) )
				{
					return false;
				}
			}

			if ( !GoalManager.hasGoals() )
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

	private String getDefaultConditions()
	{
		KoLAdventure location = (KoLAdventure) this.locationSelect.getSelectedValue();
		AdventureDatabase.getDefaultConditionsList( location, this.locationConditions );
		return (String) this.locationConditions.get( 0 );
	}

	public void fillCurrentConditions()
	{
		String text = GoalManager.getGoalString();

		if ( text.length() == 0 )
		{
			text = this.getDefaultConditions();
		}

		this.conditionField.setText( text );
	}

	public void fillDefaultConditions()
	{
		this.conditionField.setText( this.getDefaultConditions() );
	}

	public static JPanel getAdventureSummary( final String property )
	{
		int selectedIndex = Preferences.getInteger( property );

		CardLayout resultCards = new CardLayout();
		JPanel resultPanel = new JPanel( resultCards );
		JComboBox resultSelect = new JComboBox();

		int cardCount = 0;

		resultSelect.addItem( "Session Results" );
		resultPanel.add( new GenericScrollPane( KoLConstants.tally, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Location Details" );
		resultPanel.add( new SafetyField(), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Conditions Left" );
		resultPanel.add( new GenericScrollPane( GoalManager.getGoals(), 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Available Skills" );
		resultPanel.add( new GenericScrollPane( KoLConstants.availableSkills, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Active Effects" );
		resultPanel.add( new GenericScrollPane( KoLConstants.activeEffects, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Encounter Listing" );
		resultPanel.add( new GenericScrollPane( KoLConstants.encounterList, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Visited Locations" );
		resultPanel.add( new GenericScrollPane( KoLConstants.adventureList, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Daily Deeds" );
		resultPanel.add( new GenericScrollPane( new DailyDeedsPanel() ), String.valueOf( cardCount++ ) );

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

	private static class ResultSelectListener
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

	private static class SafetyField
		extends JPanel
		implements Runnable
	{
		private String savedText = " ";
		private final RequestPane safetyDisplay;

		public SafetyField()
		{
			super( new BorderLayout() );

			this.safetyDisplay = new RequestPane();

			JScrollPane safetyScroller =
				new JScrollPane(
					this.safetyDisplay, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

			JComponentUtilities.setComponentSize( this.safetyDisplay, 100, 100 );
			this.add( safetyScroller, BorderLayout.CENTER );

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( this ) );

			this.setSafetyString();
		}

		public void run()
		{
			this.setSafetyString();
		}

		private void setSafetyString()
		{
			KoLAdventure request = KoLCharacter.getSelectedLocation();

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
			this.safetyDisplay.setText( text );
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

		public void stateChanged( final ChangeEvent e )
		{
			int maximum = KoLCharacter.getAdventuresLeft();
			if ( maximum == 0 )
			{
				this.setValue( new Integer( 0 ) );
				return;
			}

			int desired = InputFieldUtilities.getValue( this, maximum );
			if ( desired == maximum + 1 )
			{
				this.setValue( new Integer( 1 ) );
			}
			else if ( desired <= 0 || desired > maximum )
			{
				this.setValue( new Integer( maximum ) );
			}
		}
	}
}
