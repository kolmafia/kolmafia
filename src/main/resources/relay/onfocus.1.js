var inputsActive = false;

function attachFocusListeners( collection )
{
	for ( var i = collection.length - 1; i >= 0; --i )
	{
		collection[i].onfocus = function()
		{
			if ( this.type == "text" )
			{
				this.select();
			}

			inputsActive = true;
		}

		collection[i].onblur = function()
		{
			inputsActive = false;
		}
	}
}

attachFocusListeners( document.getElementsByTagName( "input" ) );
attachFocusListeners( document.getElementsByTagName( "select" ) );
