package net.sourceforge.kolmafia.chat;

public class ModeratorMessage
	extends ChatMessage
{
	private String playerId;
	
	public ModeratorMessage( String channel, String messageType, String playerId, String content )
	{
		this.setRecipient( channel );
		this.setSender( messageType );
		this.setContent( content );
	}
	
	public String getModeratorId()
	{
		return playerId;
	}
}
