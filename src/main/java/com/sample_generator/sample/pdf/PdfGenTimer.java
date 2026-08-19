package com.sample_generator.sample.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-local timing buckets for one {@code generatePdf()} call.
 */
public final class PdfGenTimer {

    public interface IoWork {
        void run() throws IOException;
    }

    private static final ThreadLocal<Map<String, Long>> NANOS = ThreadLocal.withInitial(LinkedHashMap::new);

    private PdfGenTimer() {
    }

    public static void reset() {
        NANOS.remove();
    }

    public static void time(String name, IoWork work) throws IOException {
        long start = System.nanoTime();
        work.run();
        add(name, System.nanoTime() - start);
    }

    public static void add(String name, long nanos) {
        Map<String, Long> map = NANOS.get();
        map.merge(name, nanos, Long::sum);
    }

    public static void dump(String label) {
        Map<String, Long> map = NANOS.get();
        if (map == null || map.isEmpty()) {
            System.out.println(label + " timings: (none)");
            return;
        }
        List<Map.Entry<String, Long>> ranked = new ArrayList<>(map.entrySet());
        ranked.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        System.out.println(label + " timings (ms):");
        int shown = 0;
        for (Map.Entry<String, Long> entry : ranked) {
            System.out.println("  " + entry.getKey() + ": " + (entry.getValue() / 1_000_000L) + " ms");
            if (++shown >= 12) {
                break;
            }
        }
    }

    public static String topThreeSummary() {
        Map<String, Long> map = NANOS.get();
        if (map == null || map.isEmpty()) {
            return "(no timings)";
        }
        List<Map.Entry<String, Long>> ranked = new ArrayList<>(map.entrySet());
        ranked.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        StringBuilder sb = new StringBuilder();
        int n = Math.min(3, ranked.size());
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append("; ");
            }
            Map.Entry<String, Long> entry = ranked.get(i);
            sb.append(entry.getKey()).append('=').append(entry.getValue() / 1_000_000L).append("ms");
        }
        return sb.toString();
    }
}
