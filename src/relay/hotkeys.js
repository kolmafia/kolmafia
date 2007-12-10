function handleCombatHotkey( e )
{
	var key = window.event ? e.keyCode : e.which;
	var validHotkey = (key >= '0' && key <= '9');

	if ( !isValidHotkey )
		return false;

	// Safari processes the key event twice; in order
	// to make sure this doesn't cause problems, you
	// will need to stop the propogation for each event.

	if ( e.stopPropagation )
		e.stopPropagation();

	document.location.href = "fight.php?hotkey=" + key;
	return true;
}

function updateCombatHotkey()
{
}