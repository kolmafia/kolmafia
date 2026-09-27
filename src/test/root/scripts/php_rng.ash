rng r = php_seed(1);
rng s = php_seed(2147483647);
print(php_mt_rand(r));
print(php_mt_rand(s));
print(php_mt_rand(r));
print(php_mt_rand(s));

r = php_seed(1);
s = php_seed(2147483647);
print(php_rand(r));
print(php_rand(s));
print(php_rand(r));
print(php_rand(s));