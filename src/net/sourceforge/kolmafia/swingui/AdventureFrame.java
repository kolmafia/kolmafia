package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.panel.AdventureSelectPanel;
import net.sourceforge.kolmafia.swingui.panel.ChoiceOptionsPanel;
import net.sourceforge.kolmafia.swingui.panel.CustomCombatPanel;
import net.sourceforge.kolmafia.swingui.panel.MoodOptionsPanel;
import net.sourceforge.kolmafia.swingui.panel.RestoreOptionsPanel;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

public class AdventureFrame
	extends GenericFrame implements Listener
{
	private static AdventureSelectPanel adventureSelector = null;
	private static ChoiceOptionsPanel choiceOptionsPanel;
	private static CustomCombatPanel customCombatPanel;
	private static RestoreOptionsPanel restoreOptionsPanel;
	private static JProgressBar requestMeter = null;
	private static JSplitPane sessionGrid;

	/**
	 * Constructs a new <code>AdventureFrame</code>. All constructed panels are placed into their corresponding tabs,
	 * with the content panel being defaulted to the adventure selection panel.
	 */

	public AdventureFrame()
	{
		super( "Adventure" );

		// Construct the adventure select container
		// to hold everything related to adventuring.

		AdventureFrame.adventureSelector = new AdventureSelectPanel( true );

		JPanel adventureDetails = new JPanel( new BorderLayout( 20, 20 ) );
		adventureDetails.add( AdventureFrame.adventureSelector, BorderLayout.CENTER );

		AdventureFrame.requestMeter = new JProgressBar();
		AdventureFrame.requestMeter.setOpaque( true );
		AdventureFrame.requestMeter.setStringPainted( true );

		JPanel meterPanel = new JPanel( new BorderLayout( 10, 10 ) );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.WEST );
		meterPanel.add( AdventureFrame.requestMeter, BorderLayout.CENTER );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.EAST );

		adventureDetails.add( meterPanel, BorderLayout.SOUTH );

		JPanel adventurePanel = new JPanel( new BorderLayout( 20, 20 ) );
		adventurePanel.add( adventureDetails, BorderLayout.NORTH );
		adventurePanel.add( this.getSouthernTabs(), BorderLayout.CENTER );

		AdventureFrame.updateSelectedAdventure( AdventureDatabase.getAdventure( Preferences.getString( "lastAdventure" ) ) );
		AdventureFrame.adventureSelector.fillCurrentConditions();

		JComponentUtilities.setComponentSize( adventurePanel, 640, 480 );

		this.setCenterComponent( adventurePanel );
		NamedListenerRegistry.registerNamedListener( "(koladventure)", this );
	}

	@Override
	public boolean shouldAddStatusBar()
	{
		return false;
	}

	@Override
	public void setStatusMessage( final String message )
	{
		if ( AdventureFrame.requestMeter == null || message.length() == 0 )
		{
			return;
		}

		// Avoid flicker
		if ( !message.equals( AdventureFrame.requestMeter.getString() ) )
		{
			AdventureFrame.requestMeter.setString( message );
		}
	}

	public static final void updateRequestMeter( final int value, final int maximum )
	{
		if ( AdventureFrame.requestMeter == null )
		{
			return;
		}

		// Avoid flicker
		if ( value != AdventureFrame.requestMeter.getValue() )
		{
			AdventureFrame.requestMeter.setValue( value );
		}

		if ( maximum != AdventureFrame.requestMeter.getMaximum() )
		{
			AdventureFrame.requestMeter.setMaximum( maximum );
		}
	}

	public KoLAdventure getSelectedAdventure()
	{
		if ( AdventureFrame.adventureSelector == null )
		{
			return null;
		}

		return AdventureFrame.adventureSelector.getSelectedAdventure();
	}

	public static final void updateSelectedAdventure( final KoLAdventure location )
	{
		if ( AdventureFrame.adventureSelector == null )
		{
			return;
		}

		if ( AdventureFrame.adventureSelector.getSelectedAdventure() != location )
		{
			AdventureFrame.adventureSelector.updateSelectedAdventure( location );
		}
	}

	public static final void updateSafetyDetails()
	{
		if ( AdventureFrame.adventureSelector == null )
		{
			return;
		}

		AdventureFrame.adventureSelector.updateSafetyDetails();
	}

	public static final void updateFromPreferences()
	{
		if ( AdventureFrame.adventureSelector != null )
		{
			AdventureFrame.adventureSelector.updateFromPreferences();
		}

		if ( AdventureFrame.choiceOptionsPanel != null )
		{
			AdventureFrame.choiceOptionsPanel.loadSettings();
		}

		if ( AdventureFrame.customCombatPanel != null )
		{
			AdventureFrame.customCombatPanel.updateFromPreferences();
		}

		if ( AdventureFrame.restoreOptionsPanel != null )
		{
			AdventureFrame.restoreOptionsPanel.updateFromPreferences();
		}
	}

	@Override
	public boolean useSidePane()
	{
		return true;
	}

	public JTabbedPane getSouthernTabs()
	{
		// Components of custom combat and choice adventuring,
		// combined into one friendly panel.

		this.tabs.addTab( "Overview", this.getAdventureSummary() );
		AdventureFrame.choiceOptionsPanel = new ChoiceOptionsPanel();

		this.tabs.addTab( "Choice Advs", AdventureFrame.choiceOptionsPanel );

		AdventureFrame.restoreOptionsPanel = new RestoreOptionsPanel();
		GenericScrollPane restoreScroller = new GenericScrollPane( AdventureFrame.restoreOptionsPanel );
		JComponentUtilities.setComponentSize( restoreScroller, 560, 400 );
		this.tabs.addTab( "HP/MP Usage", restoreScroller );

		this.tabs.addTab( "Mood Setup", new MoodOptionsPanel() );

		AdventureFrame.customCombatPanel = new CustomCombatPanel();
		this.tabs.addTab( "Custom Combat", AdventureFrame.customCombatPanel );

		AdventureFrame.adventureSelector.addSelectedLocationListener( AdventureFrame.choiceOptionsPanel.getUpdateListener() );
		return this.tabs;
	}

	private JSplitPane getAdventureSummary()
	{
		AdventureFrame.sessionGrid =
			new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true,
				AdventureSelectPanel.getAdventureSummary( "defaultDropdown1" ),
				AdventureSelectPanel.getAdventureSummary( "defaultDropdown2" ) );

		int location = Preferences.getInteger( "defaultDropdownSplit" );

		if ( location == 0 )
		{
			AdventureFrame.sessionGrid.setDividerLocation( 0.5 );
		}
		else
		{
			AdventureFrame.sessionGrid.setDividerLocation( location );
		}

		AdventureFrame.sessionGrid.setResizeWeight( 0.5 );
		return AdventureFrame.sessionGrid;
	}

	@Override
	public void dispose()
	{
		Preferences.setInteger( "defaultDropdownSplit", AdventureFrame.sessionGrid.getLastDividerLocation() );
		super.dispose();
	}

	@Override
	public void update() {
		updateSelectedAdventure(KoLCharacter.getSelectedLocation());
	}
}
