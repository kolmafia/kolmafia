package net.sourceforge.kolmafia.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;


class SortedListTest {
    /** purpose: test add method with input null
     *  Input: add null
     *  Expected:
     *      return false
     */
    @Test
    void addNULL() {
        SortedList<String> list = new SortedList<String>();
        // list.add will return false if the element is null
        // make test that add null
        assertFalse(list.add(null));

    }

    /**
     * purpose: test add method with duplicate input
     * Input: add "a" -> "a" -> "a"
     * Expected:
     *     return true
     */
    @Test
    void addDuplicate() {
        SortedList<String> list = new SortedList<String>();
        //create new ArrayList that has three element "a"
        ArrayList<String> temp = new ArrayList<>();
        temp.add("a");
        temp.add("a");
        temp.add("a");


        // list.add will return true if the element is duplicate
        // make test that add duplicate
        assertTrue(list.add("a"));
        assertTrue(list.add("a"));
        assertTrue(list.add("a"));
        // check element in list is ["a","a","a"]
        assertEquals(temp, list);
    }

    /**
     * purpose: test add method with ascending order
     * Input: add "a" -> "b" -> "c"
     * Expected:
     *     return true
     *     return true
     *     return true
     *     list will be ["a","b","c"]
     */
    @Test
    void addAscendingOrder() {
        SortedList<String> list = new SortedList<String>();
        //create new ArrayList that has three element "a","b","c"
        ArrayList<String> temp = new ArrayList<>();
        temp.add("a");
        temp.add("b");
        temp.add("c");

        // list.add will return true if the element is added
        // make test that add "a" -> "b" -> "c"
        assertTrue(list.add("a"));
        assertTrue(list.add("b"));
        assertTrue(list.add("c"));
        // check element in list is ["a","b","c"]
        assertEquals(temp, list);
    }
    /**
     * purpose: test add method with descending order
     * Input: add "c" -> "b" -> "a"
     * Expected:
     *     return true
     *     return true
     *     return true
     *     list will be ["a","b","c"]
     */
    @Test
    void addDescendingOrder() {
        SortedList<String> list = new SortedList<String>();
        //create new ArrayList that has three element "a","b","c"
        ArrayList<String> temp = new ArrayList<>();
        temp.add("a");
        temp.add("b");
        temp.add("c");

        // list.add will return true if the element is added
        // make test that add "c" -> "b" -> "a"
        assertTrue(list.add("c"));
        assertTrue(list.add("b"));
        assertTrue(list.add("a"));
        // check element in list is ["a","b","c"]
        assertEquals(temp, list);
    }


    /** purpose: test indexOf method with empty list
     *  Input: indexOf "a"
     *  Expected:
     *      return -1
     */
    @Test
    void indexOfEmptyList() {
        SortedList<String> list = new SortedList<String>();
        // list.indexOf will return -1 if the list is empty
        // make test that indexOf "a" in empty list
        assertEquals(-1, list.indexOf("a"));
    }

    /** purpose: test indexOf method with list size 1
     *  Input: indexOf ("a") -> ("b") -> ("c")
     *  Expected:
     *      ("a") = -1
     *      ("b") = 0
     *      ("c") = -1
     */
    @Test
    void indexOfListSize1() {
        SortedList<String> list = new SortedList<String>();
        list.add("b");

        assertEquals(-1, list.indexOf("a"));
        assertEquals(0, list.indexOf("b"));
        assertEquals(-1, list.indexOf("c"));
    }


    /** purpose: test indexOf method with list size 2
     *  Input: indexOf ("a") -> ("b") -> ("c")
     *  Expected:
     *      ("a") = -1
     *      ("b") = 0
     *      ("c") = 1
     */
    @Test
    void indexOfListSize2() {
        SortedList<String> list = new SortedList<String>();
        list.add("b");
        list.add("c");

        assertEquals(-1, list.indexOf("a"));
        assertEquals(0, list.indexOf("b"));
        assertEquals(1, list.indexOf("c"));
    }

    /** purpose: test indexOf method with list size 3
     *  Input: indexOf ("a") -> ("b") -> ("c") -> ("d")
     *  Expected:
     *      ("a") = -1
     *      ("b") = 0
     *      ("c") = 1
     *      ("d") = 2
     */
    @Test
    void indexOfListSize3() {
        SortedList<String> list = new SortedList<String>();
        list.add("b");
        list.add("c");
        list.add("d");

        assertEquals(-1, list.indexOf("a"));
        assertEquals(0, list.indexOf("b"));
        assertEquals(1, list.indexOf("c"));
        assertEquals(2, list.indexOf("d"));
    }



}