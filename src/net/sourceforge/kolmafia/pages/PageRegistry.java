package net.sourceforge.kolmafia.pages;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PageRegistry {
  private static final Set<String> seenLocations = new HashSet<>();
  private static final Map<String, Page> pagesByLocation = new HashMap<>();

  public static final boolean isGameAction(String path, String queryString) {
    if (PageRegistry.isExternalLocation(path)) {
      return false;
    }

    Page page = PageRegistry.getPage(path);

    if (page == null) {
      return true;
    }

    return page.isGameAction(queryString);
  }

  private static synchronized Page getPage(String path) {

    if (PageRegistry.seenLocations.contains(path)) {
      return pagesByLocation.get(path);
    }

    PageRegistry.seenLocations.add(path);

    Class<?> pageClass = null;

    try {
      String className = "net.sourceforge.kolmafia.pages." + path.substring(0, path.length() - 4);

      pageClass = Class.forName(className);
    } catch (ClassNotFoundException e) {
    }

    if (pageClass == null) {
      return null;
    }

    Page page = null;

    try {
      page = (Page) pageClass.getDeclaredConstructor().newInstance();

      pagesByLocation.put(path, page);
    } catch (Exception e) {
    }

    return page;
  }

  private static boolean isExternalLocation(String path) {
    return path.length() == 0
        || path.startsWith("http:")
        || path.startsWith("https:")
        || !path.endsWith(".php");
  }
}
