/* Uses string format_date_time(string, string, string) as reference */
string str_in_format = "YYYY";
string str_date = "2000";
string str_out_format = "YY";

string format_date_time_private(string inFormat, string dateTimeString, string outFormat) {
	return format_date_time(inFormat, dateTimeString, outFormat);
}

print("> original format_date_time_private(string-string-string)");
print(format_date_time_private(str_in_format, str_date, str_out_format)); /*Should print "00"*/
print();

/* Define 2 different layers of typedef */
typedef string str2;
typedef str2 str3;

str2 str2_in_format = str_in_format;
str2 str2_date = str_date;
str2 str2_out_format = str_out_format;
str3 str3_in_format = str2_in_format;
str3 str3_date = str2_date;
str3 str3_out_format = str2_out_format;

print("> pre-overload; all coerce to original");
print(format_date_time_private(str2_in_format, str2_date, str2_out_format));
print(format_date_time_private(str3_in_format, str3_date, str3_out_format));
print();

string format_date_time_private(str2 inFormat, str2 dateTimeString, str2 outFormat) {
	return "str2-str2-str2";
}

print("> overloaded with str2-str2-str2");
print(format_date_time_private(str2_in_format, str2_date, str2_out_format));
print("> str3-str3-str3 -> string-string-string (accessing str2-str2-str2 without an exact match is impossible)");
print(format_date_time_private(str3_in_format, str3_date, str3_out_format));
