function changeSelection( selectToChange, desiredValue )
{
	for ( var i = 0; i < selectToChange.options.length; ++i )
		if ( selectToChange.options[i].value == desiredValue )
			selectToChange.selectedIndex = i;
}

function palinshelve()
{
	var selectors = document.getElementsByTagName( "select" );

	for ( var i = 0; i < selectors.length; ++i )
	{
		if ( selectors[i].name == "photo1" )
			changeSelection( selectors[i], 2259 );
		else if ( selectors[i].name == "photo2" )
			changeSelection( selectors[i], 7264 );
		else if ( selectors[i].name == "photo3" )
			changeSelection( selectors[i], 7263 );
		else if ( selectors[i].name == "photo4" )
			changeSelection( selectors[i], 7265 );
	}
}
