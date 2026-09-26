// The following should generate a warning
buffer to_buffer(string initial)
{
    print("initial = '" + initial + "'");
    buffer buf;
    buf.append(initial);
    return buf;
}

buffer buf1 = to_buffer("Initial content 1");
print("buf1 content = '" + buf1.to_string() + "'");

buffer buf1a = to_buffer(buf1);
print("buf1a content = '" + buf1a.to_string() + "'");

buffer buf2 = "Initial content 2";
print("buf2 content = '" + buf2.to_string() + "'");

buffer buf3 = $path[Heavy Rains];
print("buf3 content = '" + buf3.to_string() + "'");
