package com.bench;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks computation history with entries recording operands, operation, and result.
 */
public class HistoryLog {

    /**
     * Immutable record of a single computation.
     */
    public record Entry(int a, int b, String operation, int result) {}

    private final List<Entry> entries = new ArrayList<>();

    /**
     * Records a computation entry.
     *
     * @param a first operand
     * @param b second operand
     * @param operation the operation performed
     * @param result the result of the computation
     */
    public void record(int a, int b, String operation, int result) {
        entries.add(new Entry(a, b, operation, result));
    }

    /**
     * Returns an unmodifiable copy of all recorded entries.
     *
     * @return unmodifiable list of entries
     */
    public List<Entry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Formats all entries as a multi-line string.
     * Each line follows the format: 'a op b = result' (e.g., '2 add 3 = 5').
     * Returns empty string if no entries exist.
     *
     * @return formatted history string
     */
    public String format() {
        if (entries.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            sb.append(e.a).append(" ").append(e.operation).append(" ").append(e.b).append(" = ").append(e.result);
            if (i < entries.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Clears all recorded entries.
     */
    public void clear() {
        entries.clear();
    }
}
