package net.sourceforge.kolmafia.chat;

public class DisableMessage
	extends ChatMessage
{
	private final boolean isTalkChannel;

	public DisableMessage( String channel, boolean isTalkChannel )
	{
		this.setSender( channel );

		this.isTalkChannel = isTalkChannel;
	}

	public boolean isTalkChannel()
	{
		return this.isTalkChannel;
	}
}
