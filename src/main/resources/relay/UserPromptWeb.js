function UserPromptWeb( promptForm, commandForm, commandInput, submitCommand )
{
	var promptId = 0;

	function hide()
	{
		promptForm.hidden = true;
		commandForm.hidden = false;
		commandInput.focus();
	}

	function show( prompt )
	{
		var id = prompt ? prompt.id : 0;
		if ( id == promptId )
			return;

		promptId = id;
		promptForm.replaceChildren();
		if ( !prompt )
		{
			hide();
			return;
		}
		promptForm.hidden = false;
		commandForm.hidden = true;

		var text = document.createElement( "p" );
		text.textContent = prompt.message;
		promptForm.appendChild( text );

		var field = null;
		var options = null;
		var searchable = prompt.kind == "select" && prompt.options.length > 12;
		if ( prompt.kind == "text" || searchable )
		{
			field = document.createElement( "input" );
			field.type = "text";
			field.value = prompt.initial || "";
		}
		if ( searchable )
		{
			options = document.createElement( "datalist" );
			options.id = "promptoptions";
			field.setAttribute( "list", options.id );
			field.addEventListener( "input", function() { field.setCustomValidity( "" ); } );
			promptForm.appendChild( options );
		}
		else if ( prompt.kind == "select" || prompt.kind == "multiple" )
		{
			field = options = document.createElement( "select" );
			field.multiple = prompt.kind == "multiple";
		}
		if ( options )
			prompt.options.forEach( function( option ) { options.appendChild( new Option( option ) ); } );
		if ( field )
			promptForm.appendChild( field );

		var buttons = prompt.kind == "confirm" ? [ "Yes", "No" ] : prompt.kind == "alert" ? [ "OK" ] : [ "OK", "Cancel" ];
		buttons.forEach( function( label )
		{
			var button = document.createElement( "button" );
			button.value = label;
			button.textContent = label;
			promptForm.appendChild( button );
		} );

		function answer( label )
		{
			hide();

			var command = "promptanswer " + prompt.id;
			if ( prompt.kind == "confirm" )
				command += " " + ( label == "Yes" );
			else if ( label == "OK" && prompt.kind == "multiple" )
				command += " " + Array.from( field.selectedOptions, function( option ) { return option.index; } ).join( "," );
			else if ( label == "OK" && prompt.kind == "select" )
				command += " " + prompt.options.indexOf( field.value );
			else if ( label == "OK" && field )
				command += " " + field.value;
			submitCommand( command );
		}

		promptForm.onsubmit = function( event )
		{
			event.preventDefault();
			var label = event.submitter ? event.submitter.value : buttons[ 0 ];
			if ( label == "OK" && prompt.kind == "select" && prompt.options.indexOf( field.value ) < 0 )
			{
				field.setCustomValidity( "Pick one of the options" );
				field.reportValidity();
				return;
			}
			answer( label );
		};
		promptForm.onkeydown = function( event )
		{
			if ( event.key == "Escape" )
			{
				event.preventDefault();
				answer( buttons[ 1 ] );
			}
		};

		( field || promptForm.querySelector( "button" ) ).focus();
	}

	return {
		show: show,
		id: function() { return promptId; }
	};
}
