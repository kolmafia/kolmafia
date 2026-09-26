string what_do_I_now_know()
{
	return "my ABC";
}

String when = "Now";
string who = 'I';

print(

	`{//Comment
		when

	} {
		who
	} {false?"knew":true?"know":"knee"}\
	  	 

	`

	+

	` { what_do_I_now_know() + "!" }`

);
