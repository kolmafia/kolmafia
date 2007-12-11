function handleCombatHotkey( e )
{
	var key = window.event ? e.keyCode : e.which;
	var isValidHotkey = (key >= 48 && key <= 57);

	if ( !isValidHotkey )
		return false;

	// Safari processes the key event twice; in order
	// to make sure this doesn't cause problems, you
	// will need to stop the propogation for each event.

	if ( e.stopPropagation )
		e.stopPropagation();

	document.location.href = "fight.php?hotkey=" + (key - 48);
	return true;
}

function updateCombatHotkey()
{
	var viewer = document.getElementById( "hotkeyViewer" );
	var hotkey = (viewer.selectedIndex - 1);
	var hotkeyAction = prompt( "New value for " + hotkey, "" );

	if ( hotkeyAction )
	{
		var httpObject = getHttpObject();
		if ( !httpObject )
			return true;

		httpObject.open( "GET", "/KoLmafia/submitCommand?cmd=set+combatHotkey" + hotkey + "%3D" +
			URLEncode( hotkeyAction ) + "&MAFIAHIT", true );

		httpObject.send( "" );
		viewer[ hotkey + 1 ].innerHTML = hotkey + ": " + hotkeyAction;
	}
}