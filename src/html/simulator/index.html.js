<script language="Javascript">
function loadKoLmafiaData()
{
	// Load basic character information
	
	document.character.charclass.selectedIndex = /*classIndex*/;
	document.character.basemuscle.value = /*baseMuscle*/;
	document.character.basemysticality.value = /*baseMysticality*/;
	document.character.basemoxie.value = /*baseMoxie*/;
	document.character.mcd.selectedIndex = /*mindControl*/;
	document.character.weight.value = /*familiarWeight*/;

	// Effects & Passive Skills
	
	// ( Skills must come before equipment for dual weilding )
	
	var passiveSkills = "/*passiveSkills*/";
	var activeEffects = "/*activeEffects*/";
	for ( j = 0; j < document.forms.length; ++j )
		for ( i = 0; i < document.forms[j].elements.length; ++i )
		{
			if ( passiveSkills.indexOf( "\t" + document.forms[j].name + "." + document.forms[j].elements[i].name + "\t" ) != -1 )
				if ( document.forms[j].elements[i].type == "checkbox" )
				{
					document.forms[j].elements[i].checked = true;
					document.forms[j].style.display = "";
				}
			if ( activeEffects.indexOf( "\t" + document.forms[j].elements[i].name + "\t" ) != -1 )
				if ( document.forms[j].elements[i].type == "checkbox" )
				{
					document.forms[j].elements[i].checked = true;
					document.forms[j].style.display = "";
				}
		}

	AdjustForDualWield();
	
	// Snowcones
	var snowcone = 0;
	if ( activeEffects.indexOf( "\tblacktongue\t" ) != -1 )
		snowcone = 1;
	if ( activeEffects.indexOf( "\tbluetongue\t" ) != -1 )
		snowcone = 2;
	if ( activeEffects.indexOf( "\tredtongue\t" ) != -1 )
		snowcone = 3;
	if ( activeEffects.indexOf( "\torangetongue\t" ) != -1 )
		snowcone = 4;
	if ( activeEffects.indexOf( "\tgreentongue\t" ) != -1 )
		snowcone = 5;
	if ( activeEffects.indexOf( "\tpurpletongue\t" ) != -1 )
		snowcone = 6;
	if ( snowcone != 0 )
	{
		document.snowcones.style.display = "";
		document.snowcones[snowcone].checked = true;
	}

	// Familiar and Familiar Equipment

	for ( i = 0; i < document.character.familiar.options.length; ++i ) 
		if ( document.character.familiar.options[i].innerHTML.toLowerCase() == "/*familiar*/" )
			document.character.familiar.selectedIndex = i;
	for ( i = 0; i < document.equipment.familiarequip.options.length; ++i )
		if ( document.equipment.familiarequip.options[i].value.toLowerCase() == "/*familiarEquip*/" )
			document.equipment.familiarequip.selectedIndex = i;

	// Hat, Weapon, Offhand, Pants

	for ( i = 0; i < numberofitemchoices[0]; ++i )
		if ( equipment[0][i].name.toLowerCase() == "/*hat*/" )
			document.equipment.hat.selectedIndex = i;
	for ( i = 0; i < numberofitemchoices[1]; ++i )
		if ( equipment[1][i].name.toLowerCase() == "/*weapon*/" )
			document.equipment.weapon.selectedIndex = i;

	// Uhh... Is this bad?
	// this is the only way I could make dual wielding work :(
	document.equipment.offhand.value = "/*offhand*/";

	for ( i = 0; i < numberofitemchoices[3]; ++i ) 
		if ( equipment[3][i].name.toLowerCase() == "/*shirt*/" )
			document.equipment.pants.selectedIndex = i; 
	for ( i = 0; i < numberofitemchoices[4]; ++i ) 
		if ( equipment[4][i].name.toLowerCase() == "/*pants*/" )
			document.equipment.pants.selectedIndex = i; 

	// Accessories
	for ( i = 0; i < numberofitemchoices[5]; ++i ) 
		if ( equipment[5][i].name.toLowerCase() == "/*accessory1*/" )
			document.equipment.acc1.selectedIndex = i; 
	for ( i = 0; i < numberofitemchoices[6]; ++i ) 
		if ( equipment[5][i].name.toLowerCase() == "/*accessory2*/" )
			document.equipment.acc2.selectedIndex = i; 
	for ( i = 0; i < numberofitemchoices[7]; ++i ) 
		if ( equipment[5][i].name.toLowerCase() == "/*accessory3*/" )
			document.equipment.acc3.selectedIndex = i; 
	
	document.miscinput.rockandroll.checked = /*rockAndRoll*/;
}
</script>