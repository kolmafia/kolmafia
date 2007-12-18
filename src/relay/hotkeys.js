var shiftKey = false;
var ctrlKey = false;
var altKey = false;
var metaKey = false;

function getNumericKey( keyCode )
{
    if ( keyCode >= 48 && keyCode <= 57 )
    	return keyCode - 48;

    if ( keyCode >= 96 && keyCode <= 105 )
    	return keyCode - 96;

    return -1;
}

function handleCombatHotkey( e, isDown )
{
	var keyCode = window.event ? e.keyCode : e.which;

	if ( e.metaKey )
		metaKey = isDown;

	if ( isDown && (shiftKey || ctrlKey || altKey || metaKey) )
		return false;

	// Detect release of the different modifier keys
	// so we know whether or not the person has pressed
	// something in addition to the numeric key.

	if ( !isDown )
	{
		shiftKey = (keyCode == 16);
		ctrlKey = (keyCode == 17);
		altKey = (keyCode == 18);

		return false;
	}

	// Otherwise, if the person has pressed the shift
	// key, update your current state.

	if ( keyCode == 16 )
		shiftKey = true;
	if ( keyCode == 17 )
		ctrlKey = true;
	if ( keyCode == 18 )
		altKey = true;

	// Safari processes the key event twice; in order
	// to make sure this doesn't cause problems, you
	// will need to stop the propogation for each event.

	if ( e.stopPropagation )
		e.stopPropagation();

	// Finally, make sure this is a valid hotkey before
	// attempting to process it as one.

	if ( keyCode == 13 )
	{
		document.getElementById( "defaultButton" ).onclick();
		return true;
	}

	var numericKey = getNumericKey( keyCode );
	if ( numericKey == -1 )
		return false;

	document.location.href = "fight.php?hotkey=" + numericKey;
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