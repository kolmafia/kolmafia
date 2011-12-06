/**
 * 
 */
package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.listener.HyperlinkAdapter;
import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;

public class CommandDisplayPanel
	extends JPanel
{
	private final RollingLinkedList commandHistory = new RollingLinkedList( 10 );
	private final AutoHighlightTextField entryField;
	private final JButton entryButton;

	private int commandIndex = 0;
	
	public CommandDisplayPanel()
	{
		RequestPane outputDisplay = new RequestPane();
		outputDisplay.addHyperlinkListener( new HyperlinkAdapter() );
		
		JScrollPane scrollPane = KoLConstants.commandBuffer.addDisplay( outputDisplay );
		JComponentUtilities.setComponentSize( scrollPane, 400, 300 );

		JPanel entryPanel = new JPanel( new BorderLayout() );
		this.entryField = new AutoHighlightTextField();
		this.entryField.addKeyListener( new CommandEntryListener() );

		this.entryButton = new JButton( "exec" );
		this.entryButton.addActionListener( new CommandEntryListener() );

		entryPanel.add( this.entryField, BorderLayout.CENTER );
		entryPanel.add( this.entryButton, BorderLayout.EAST );

		this.setLayout( new BorderLayout( 1, 1 ) );
		this.add( scrollPane, BorderLayout.CENTER );
		this.add( entryPanel, BorderLayout.SOUTH );

		this.setFocusCycleRoot( true );
		this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( this.entryField ) );
	}

	private class CommandEntryListener
		extends KeyAdapter
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			this.submitCommand();
		}

		public void keyReleased( final KeyEvent e )
		{
			if ( e.isConsumed() )
			{
				return;
			}

			if ( e.getKeyCode() == KeyEvent.VK_UP )
			{
				if ( CommandDisplayPanel.this.commandIndex <= 0 )
				{
					return;
				}

				CommandDisplayPanel.this.entryField.setText( (String) CommandDisplayPanel.this.commandHistory.get( --CommandDisplayPanel.this.commandIndex ) );
				e.consume();
			}
			else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
			{
				if ( CommandDisplayPanel.this.commandIndex + 1 >= CommandDisplayPanel.this.commandHistory.size() )
				{
					return;
				}

				CommandDisplayPanel.this.entryField.setText( (String) CommandDisplayPanel.this.commandHistory.get( ++CommandDisplayPanel.this.commandIndex ) );
				e.consume();
			}
			else if ( e.getKeyCode() == KeyEvent.VK_ENTER )
			{
				this.submitCommand();
				e.consume();
			}
		}

		private void submitCommand()
		{
			String command = CommandDisplayPanel.this.entryField.getText().trim();
			CommandDisplayPanel.this.entryField.setText( "" );

			CommandDisplayPanel.this.commandHistory.add( command );

			CommandDisplayPanel.this.commandIndex = CommandDisplayPanel.this.commandHistory.size();
			CommandDisplayFrame.executeCommand( command );
		}
	}
}
