print("Can show true comparison: " + (to_path("Grey You") == $path[Grey You]));
print("Can show false comparison: " + (to_path("Grey You") != $path[Zombie Slayer]));
print("Can to_path from id: " + (to_path(21) == $path[Picky]));
print("Can get path from class: " + ($class[Vampyre].path == $path[Dark Gyffte]));
print("Can see paths allow familiars: " + ($path[Path of the Plumber].familiars == true));
print("Can see paths don't allow familiars: " + ($path[License to Adventure].familiars == false));
print("Can find null path: " + ($path[none].name == "none"));
