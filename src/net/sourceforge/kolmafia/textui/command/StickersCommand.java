package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

public class StickersCommand
	extends AbstractCommand
{
	public StickersCommand()
	{
		this.usage = " <sticker1> [, <sticker2> [, <sticker3>]] - replace worn stickers.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] stickers = parameters.split( "\\s*,\\s*" );
		for ( int i = 0; i < 3; ++i )
		{
			if ( EquipmentManager.getEquipment( EquipmentManager.STICKER1 + i ) == EquipmentRequest.UNEQUIP && i < stickers.length )
			{
				String item = stickers[ i ].toLowerCase();
				if ( item.indexOf( "stick" ) == -1 )
				{
					item = item + " sticker";
				}
				EquipCommand.equip( "st" + ( i + 1 ) + " " + item );
			}
		}
	}
}
