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

	// Otherwise, if the person has pressed the shift,
	// control, or alt key, update your current state.

	if ( keyCode == 16 )
		shiftKey = true;
	if ( keyCode == 17 )
		ctrlKey = true;
	if ( keyCode == 18 )
		altKey = true;

	// Finally, make sure this is a valid hotkey before
	// attempting to process it as one.

	var numericKey = getNumericKey( keyCode );
	if ( numericKey == -1 )
		return false;

	if ( executeCommand( numericKey ) )
		return true;

	document.location.href = "fight.php?hotkey=" + numericKey;
	return true;
}

function executeCommand(numericKey)
{
	var button = document.getElementById( "defaultButton" );
	var viewer = document.getElementById( "hotkeyViewer" );

	var command = viewer.options[numericKey + 1].innerHTML;
	command = command.substring( command.indexOf( ":" ) + 2, command.length() );

	alert( command );

	if ( command.length() == 0 )
	{
		if ( numericKey == 0 )
			button.onclick();

		return true;
	}

	if ( !command.startsWith( "attack" ) && !command.startsWith( "skill" ) && !command.startsWith( "item" ) && !command.startsWith( "custom" ) && !command.startsWith( "consult" ) )
	{
		top.charpane.document.location.href = "/KoLmafia/sideCommand?cmd=" +
			URLEncode( command ) + "&MAFIAHIT";

		return true;
	}

	if ( button.value == "again" || button.value == "auto" )
	{
		button.onclick();
		return true;
	}
}

function updateCombatHotkey()
{
	var viewer = document.getElementById( "hotkeyViewer" );
	var hotkey = (viewer.selectedIndex - 1);
	var hotkeyAction = prompt( "New value for " + hotkey, "" );

	if ( hotkeyAction === false )
		return true;

	var httpObject = getHttpObject();
	if ( !httpObject )
		return true;

	httpObject.open( "GET", "/KoLmafia/submitCommand?cmd=set+combatHotkey" + hotkey + "%3D" +
		URLEncode( hotkeyAction ) + "&MAFIAHIT", true );

	httpObject.send( "" );
	viewer[ hotkey + 1 ].innerHTML = hotkey + ": " + hotkeyAction;
	return true;
}