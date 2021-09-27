package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.swingui.button.ThreadedButton;

public class MonsterDescriptionFrame
	extends DescriptionFrame
{
	private static MonsterDescriptionFrame INSTANCE = null;

	private JLabel variantsLabel;
	private JButton lastVariantButton;
	private JButton nextVariantButton;

	private MonsterData currentMonster;
	private String[] variants;
	private int variant;

	public MonsterDescriptionFrame()
	{
		super( "Monster" );
		MonsterDescriptionFrame.INSTANCE = this;

		JPanel southPanel = new JPanel();
		southPanel.add( this.createMonsterToolbar() );
		this.getFramePanel().add( southPanel, BorderLayout.SOUTH );
	}

	public JToolBar createMonsterToolbar()
	{
		JToolBar toolbarPanel = null;

		toolbarPanel = new JToolBar( "Monster Variants" );
		toolbarPanel.setFloatable( false );

		this.lastVariantButton = new ThreadedButton( JComponentUtilities.getImage( "back.gif" ), new LastVariantRunnable() );
		toolbarPanel.add( this.lastVariantButton );

		toolbarPanel.add( new JToolBar.Separator() );
		this.variantsLabel = new JLabel( "" );
		toolbarPanel.add( this.variantsLabel );
		toolbarPanel.add( new JToolBar.Separator() );

		this.nextVariantButton = new ThreadedButton( JComponentUtilities.getImage( "forward.gif" ), new NextVariantRunnable() );
		toolbarPanel.add( this.nextVariantButton );

		return toolbarPanel;
	}

	private class LastVariantRunnable
		implements Runnable
	{
		public void run()
		{
			MonsterDescriptionFrame frame = MonsterDescriptionFrame.this;
			if ( frame.variant > 0 )
			{
				if ( --frame.variant == 0 )
				{
					frame.lastVariantButton.setEnabled( false );
				}
				frame.nextVariantButton.setEnabled( true );
				String label = ( frame.variant + 1 ) + "/" + frame.variants.length;
				frame.variantsLabel.setText( label );
				frame.refreshMonster();
			}
		}
	}

	private class NextVariantRunnable
		implements Runnable
	{
		public void run()
		{
			MonsterDescriptionFrame frame = MonsterDescriptionFrame.this;
			if ( frame.variant < frame.variants.length - 1 )
			{
				if ( ++frame.variant == frame.variants.length - 1 )
				{
					frame.nextVariantButton.setEnabled( false );
				}
				frame.lastVariantButton.setEnabled( true );
				String label = ( frame.variant + 1 ) + "/" + frame.variants.length;
				frame.variantsLabel.setText( label );
				frame.refreshMonster();
			}
		}
	}

	private void refreshMonster()
	{
		MonsterData stats = null;

		// Kludge for a single monster...
		if ( this.currentMonster.getName().equals( "Ed the Undying" ) )
		{
			String name = "Ed the Undying (" + ( variant + 1 ) + ")";
			stats = MonsterDatabase.findMonster( name );
		}

		String path = "desc_monster.php?whichmonster=" + this.currentMonster.getId();
		GenericRequest request = new GenericRequest( path );
		request.responseText = this.currentMonster.craftDescription( this.variant, stats );
		this.refresh( request );
	}

	public static final void showMonster( final MonsterData monster )
	{
		if ( MonsterDescriptionFrame.INSTANCE == null )
		{
			GenericFrame.createDisplay( MonsterDescriptionFrame.class );
		}

		MonsterDescriptionFrame frame = MonsterDescriptionFrame.INSTANCE;

		frame.currentMonster = monster;
		frame.variants = monster.getImages();
		frame.variant = 0;

		if ( frame.variants.length > 0 )
		{
			String label = "1/" + frame.variants.length;
			frame.variantsLabel.setText( label );
		}

		frame.lastVariantButton.setEnabled( false );
		frame.nextVariantButton.setEnabled( frame.variants.length > 1 );

		frame.refreshMonster();
	}
}
