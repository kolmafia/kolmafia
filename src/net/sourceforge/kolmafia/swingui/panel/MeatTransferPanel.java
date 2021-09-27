package net.sourceforge.kolmafia.swingui.panel;

import java.awt.Dimension;

import javax.swing.JLabel;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.listener.CharacterListener;
import net.sourceforge.kolmafia.listener.CharacterListenerRegistry;

import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StorageRequest;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MeatTransferPanel
	extends LabeledPanel
{
	private final int transferType;
	private final AutoHighlightTextField amountField;
	private final JLabel closetField;

	public static final int MEAT_TO_CLOSET = 1;
	public static final int MEAT_TO_INVENTORY = 2;
	public static final int PULL_MEAT_FROM_STORAGE = 3;

	public MeatTransferPanel( final int transferType )
	{
		super( MeatTransferPanel.getTitle( transferType ),
			"transfer", "transfer all", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

		this.amountField = new AutoHighlightTextField();
		this.closetField = new JLabel( " " );

		VerifiableElement[] elements = new VerifiableElement[ 2 ];
		elements[ 0 ] = new VerifiableElement( "Amount: ", this.amountField );
		elements[ 1 ] = new VerifiableElement( "Available: ", this.closetField );

		this.setContent( elements );

		this.transferType = transferType;
		this.refreshCurrentAmount();

		CharacterListenerRegistry.addCharacterListener( new CharacterListener( new AmountRefresher() ) );
	}

	private static String getTitle( final int transferType )
	{
		switch ( transferType )
		{
		case MeatTransferPanel.MEAT_TO_CLOSET:
			return "Put Meat in Your Closet";
		case MeatTransferPanel.MEAT_TO_INVENTORY:
			return "Take Meat from Your Closet";
		case MeatTransferPanel.PULL_MEAT_FROM_STORAGE:
			return "Pull Meat from Hagnk's";
		}
		return "Unknown Transfer Type";
	}

	private GenericRequest getRequest( final long amount )
	{
		switch ( transferType )
		{
		case MeatTransferPanel.MEAT_TO_CLOSET:
			return new ClosetRequest( ClosetRequest.MEAT_TO_CLOSET, amount );
		case MeatTransferPanel.MEAT_TO_INVENTORY:
			return new ClosetRequest( ClosetRequest.MEAT_TO_INVENTORY, amount );
		case MeatTransferPanel.PULL_MEAT_FROM_STORAGE:
			return new StorageRequest( StorageRequest.PULL_MEAT_FROM_STORAGE, amount );
		}
		return null;
	}

	private long currentAvailable()
	{
		switch ( this.transferType )
		{
		case MeatTransferPanel.MEAT_TO_CLOSET:
			return KoLCharacter.getAvailableMeat();
		case MeatTransferPanel.MEAT_TO_INVENTORY:
			return KoLCharacter.getClosetMeat();
		case MeatTransferPanel.PULL_MEAT_FROM_STORAGE:
			return KoLCharacter.getStorageMeat();
		}

		return 0;
	}

	private void refreshCurrentAmount()
	{
		switch ( this.transferType )
		{
		case MeatTransferPanel.MEAT_TO_CLOSET:
		case MeatTransferPanel.MEAT_TO_INVENTORY:
		case MeatTransferPanel.PULL_MEAT_FROM_STORAGE:
			long amount = this.currentAvailable();
			this.closetField.setText( KoLConstants.COMMA_FORMAT.format( amount ) + " meat" );
			break;

		default:
			this.closetField.setText( "Information not available" );
			break;
		}
	}

	@Override
	public void actionConfirmed()
	{
		int amountToTransfer = InputFieldUtilities.getValue( this.amountField );

		RequestThread.postRequest( this.getRequest( amountToTransfer ) );
	}

	@Override
	public void actionCancelled()
	{
		RequestThread.postRequest( this.getRequest( this.currentAvailable() ) );
	}

	public boolean shouldAddStatusLabel( final VerifiableElement[] elements )
	{
		return false;
	}

	private class AmountRefresher
		implements Runnable
	{
		public void run()
		{
			MeatTransferPanel.this.refreshCurrentAmount();
		}
	}
}
