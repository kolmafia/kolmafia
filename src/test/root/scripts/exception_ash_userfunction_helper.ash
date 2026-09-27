// This is just a helper to be able to call an ash user function,
// that throws an exception, from javascript

int returns_eleven() {
    return 11;
}

void throw_script_exception()
{
  session_logs(-1); // this will throw an exception
  print("This code should not be reached.");
}