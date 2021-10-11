package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.swingui.listener.RelayBrowserListener;

/**
 * An internal class which displays KoLmafia's current version information. This is passed to the constructor for
 * the <code>LicenseDisplay</code>.
 */

public class VersionDataPanel
	extends JPanel
{
	private final String[] versionData =
	{
		StaticEntity.getVersion(),
		StaticEntity.getBuildInfo(),
		" ",
		"Copyright \u00a9 2005-2021 KoLmafia development team",
		"Berkeley Software Development (BSD) License",
		"https://kolmafia.us/",
		" ",
		"Current Running on " + System.getProperty( "os.name" ),
		"Local Directory is " + System.getProperty( "user.dir" ),
		"Settings in " + KoLConstants.ROOT_LOCATION.getAbsolutePath(),
		"Using Java v" + System.getProperty( "java.runtime.version" ),
		"Default system file encoding is " + System.getProperty( "file.encoding" ),
	};

	public VersionDataPanel()
	{
		JPanel versionPanel = new JPanel( new BorderLayout( 20, 20 ) );
		versionPanel.add(
			new JLabel( JComponentUtilities.getImage( "penguin.gif" ), JLabel.CENTER ), BorderLayout.NORTH );

		JPanel labelPanel = new JPanel( new GridLayout( this.versionData.length, 1 ) );
		for ( int i = 0; i < this.versionData.length; ++i )
		{
			labelPanel.add( new JLabel( this.versionData[ i ], JLabel.CENTER ) );
		}

		versionPanel.add( labelPanel, BorderLayout.CENTER );

		JPanel centerPanel = new JPanel( new BorderLayout( 20, 20 ) );
		centerPanel.add( versionPanel, BorderLayout.CENTER );

		this.setLayout( new CardLayout( 20, 20 ) );
		this.add( centerPanel, "" );
	}
}
