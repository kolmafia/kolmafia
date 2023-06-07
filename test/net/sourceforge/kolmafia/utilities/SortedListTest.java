package net.sourceforge.kolmafia.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SortedListTest {
    /** purpose: test add method with input null
     *  Input: add null
     *  Expected:
     *      return false
     */
    @Test void addNULL() {
        SortedList<String> list = new SortedList<String>();
        // list.add will return false if the element is null
        // make test that add null
        assertFalse(list.add(null));

    }

}