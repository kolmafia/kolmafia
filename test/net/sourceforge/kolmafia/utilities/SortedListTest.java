package net.sourceforge.kolmafia.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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