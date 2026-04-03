package com.bench;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the HistoryLog class.
 */
class HistoryLogTest {

    /**
     * Test recording and retrieving a single entry.
     */
    @Test
    void testRecordAndRetrieve() {
        HistoryLog log = new HistoryLog();
        log.record(2, 3, "add", 5);
        
        var entries = log.getEntries();
        assertEquals(1, entries.size());
        assertEquals(2, entries.get(0).a());
        assertEquals(3, entries.get(0).b());
        assertEquals("add", entries.get(0).operation());
        assertEquals(5, entries.get(0).result());
    }

    /**
     * Test formatting a single entry.
     */
    @Test
    void testFormatSingleEntry() {
        HistoryLog log = new HistoryLog();
        log.record(2, 3, "add", 5);
        
        String formatted = log.format();
        assertEquals("2 add 3 = 5", formatted);
    }

    /**
     * Test formatting multiple entries.
     */
    @Test
    void testFormatMultipleEntries() {
        HistoryLog log = new HistoryLog();
        log.record(2, 3, "add", 5);
        log.record(4, 5, "mul", 20);
        
        String formatted = log.format();
        String expected = "2 add 3 = 5\n4 mul 5 = 20";
        assertEquals(expected, formatted);
    }

    /**
     * Test clearing all entries.
     */
    @Test
    void testClear() {
        HistoryLog log = new HistoryLog();
        log.record(2, 3, "add", 5);
        log.record(4, 5, "mul", 20);
        
        log.clear();
        
        var entries = log.getEntries();
        assertEquals(0, entries.size());
        assertEquals("", log.format());
    }

    /**
     * Test that getEntries() returns an unmodifiable copy.
     */
    @Test
    void testGetEntriesIsUnmodifiable() {
        HistoryLog log = new HistoryLog();
        log.record(2, 3, "add", 5);
        
        var entries = log.getEntries();
        assertEquals(1, entries.size());
        
        try {
            entries.clear();
        } catch (UnsupportedOperationException e) {
            // Expected behavior for unmodifiable list
        }
        
        assertEquals(1, log.getEntries().size());
    }

    /**
     * Test format() returns empty string when no entries exist.
     */
    @Test
    void testFormatEmptyLog() {
        HistoryLog log = new HistoryLog();
        assertEquals("", log.format());
    }
}
