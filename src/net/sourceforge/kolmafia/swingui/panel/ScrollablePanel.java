package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

public class ScrollablePanel
	extends ActionPanel
{
	protected ConfirmedListener CONFIRM_LISTENER = new ConfirmedListener();
	protected CancelledListener CANCEL_LISTENER = new CancelledListener();

	public JPanel actualPanel;
	public JPanel centerPanel;

	public JPanel eastPanel;
	public VerifyButtonPanel buttonPanel;
	public JComponent scrollComponent;
	public JLabel titleComponent;
	public GenericScrollPane scrollPane;

	public ScrollablePanel( final String title, final JComponent scrollComponent )
	{
		this( title, null, null, scrollComponent );
	}

	public ScrollablePanel( final String title, final String confirmedText, final String cancelledText,
		final JComponent scrollComponent )
	{
		this( title, confirmedText, cancelledText, scrollComponent, true );
	}

	public ScrollablePanel( final String title, final String confirmedText, final String cancelledText,
		final JComponent scrollComponent, final boolean isRootPane )
	{
		this.scrollComponent = scrollComponent;

		this.centerPanel = new JPanel( new BorderLayout() );

		if ( !title.equals( "" ) )
		{
			this.titleComponent = JComponentUtilities.createLabel(
				title, SwingConstants.CENTER, Color.black, Color.white );
			this.centerPanel.add( this.titleComponent, BorderLayout.NORTH );
		}

		this.scrollPane = new GenericScrollPane( scrollComponent );
		this.centerPanel.add( scrollPane, BorderLayout.CENTER );
		this.actualPanel = new JPanel( new BorderLayout( 20, 10 ) );
		this.actualPanel.add( this.centerPanel, BorderLayout.CENTER );

		this.eastPanel = new JPanel( new BorderLayout() );

		if ( confirmedText != null )
		{
			this.buttonPanel = new VerifyButtonPanel( confirmedText, cancelledText, cancelledText, CONFIRM_LISTENER, CANCEL_LISTENER );
			this.buttonPanel.setBothDisabledOnClick( true );

			this.eastPanel.add( this.buttonPanel, BorderLayout.NORTH );
			this.actualPanel.add( this.eastPanel, BorderLayout.EAST );
		}

		JPanel containerPanel = new JPanel( new CardLayout( 10, 10 ) );
		containerPanel.add( this.actualPanel, "" );

		if ( isRootPane )
		{
			this.getContentPane().setLayout( new BorderLayout() );
			this.getContentPane().add( containerPanel, BorderLayout.CENTER );
		}
		else
		{
			this.setLayout( new BorderLayout() );
			this.add( containerPanel, BorderLayout.CENTER );
		}

		( (JPanel) this.getContentPane() ).setOpaque( true );
		StaticEntity.registerPanel( this );

		this.contentSet = true;
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
		if ( this.scrollComponent == null || this.buttonPanel == null )
		{
			return;
		}

		this.scrollComponent.setEnabled( isEnabled );
		this.buttonPanel.setEnabled( isEnabled );
	}

	@Override
	public void actionConfirmed()
	{
	}

	@Override
	public void actionCancelled()
	{
	}

	@Override
	public void dispose()
	{
		if ( this.buttonPanel != null )
		{
			this.buttonPanel.dispose();
		}
	}

	private class ConfirmedListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( ScrollablePanel.this.contentSet )
			{
				ScrollablePanel.this.actionConfirmed();
			}
		}
	}

	private class CancelledListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( ScrollablePanel.this.contentSet )
			{
				ScrollablePanel.this.actionCancelled();
			}
		}
	}
}
