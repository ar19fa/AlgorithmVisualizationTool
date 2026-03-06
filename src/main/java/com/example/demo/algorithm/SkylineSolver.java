package com.example.demo.algorithm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Skyline problem solver.
 *
 * Reads buildings as triples and computes skyline "key points" using a sweep line.
 * Then normalizes the skyline to 0..1 coordinates for canvas drawing.
 */
public class SkylineSolver {

    /**
     * JSON-friendly point (x, y) normalized to the range 0..1.
     * Used directly by the frontend for drawing.
     */
    public record Point(double x, double y) {}

    /** Internal skyline key point in raw coordinates. */
    public record KeyPoint(int x, int y) {}

    /** Building representation: [L, R, H]. */
    public record Building(int l, int r, int h) {}

    /**
     * Returned to the UI:
     * - normalizedPoints: skyline points scaled to fit the canvas cleanly
     * - minX/maxX/maxY: raw bounds (useful for labels/scales)
     */
    public record Result(
            List<Point> normalizedPoints,
            int minX,
            int maxX,
            int maxY
    ) {
        public List<Point> normalizedPoints() { return normalizedPoints; }
        public int minX() { return minX; }
        public int maxX() { return maxX; }
        public int maxY() { return maxY; }
    }

    //Entry point: parse buildings->compute skyline->normalize for canvas
    public static Result solveFromText(String input) {
        List<Building> buildings = parseBuildings(input);
        List<KeyPoint> skyline = computeSkyline(buildings);

        System.out.println("Raw skyline keypoints: " + skyline);

        return normalize(skyline);
    }

    /**
     * Parses buildings from text.
     *
     * Expected per-building line formats (commas or whitespace):
     *   L H R
     * or
     *   L, H, R
     *
     * Notes:
     * - Ignores blank lines and lines starting with '#'
     * - If a line is a single number (often a count like "10"), it is ignored
     * - The middle number is treated as HEIGHT, and the last number as RIGHT
     */
    private static List<Building> parseBuildings(String input) {
    List<Building> list = new ArrayList<>();
    String[] lines = input.split("\\R");

    for (String line : lines) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;

        // Split on commas or whitespace
        String[] parts = line.split("[,\\s]+");

        // If the line is just "10" (count), ignore it
        if (parts.length == 1) continue;

        // Need at least L, H, R
        if (parts.length < 3) continue;

        int l = Integer.parseInt(parts[0]);
        int h = Integer.parseInt(parts[1]); // HEIGHT (middle)
        int r = Integer.parseInt(parts[2]); // RIGHT (last)

        // Basic validity check: positive height and non-empty interval
        if (l < r && h > 0) {
            list.add(new Building(l, r, h)); // your Building recordis (l,r,h)
        }
    }
    return list;
}


    /**
     * Classic sweep line skyline algorithm.
     *
     * Creates "events" for:
     * - entering: (x = L, height = -H)  -> negative heights sort before leaving
     * - leaving:  (x = R, height = +H)
     *
     * Uses a multiset of active heights implemented with a TreeMap (height -> count),
     * so we can quickly get the current maximum height via lastKey().
     */
    private static List<KeyPoint> computeSkyline(List<Building> buildings) {
        if (buildings.isEmpty()) return List.of();
    
        // Each event is [x, signedHeight]
        List<int[]> events = new ArrayList<>();
        for (Building b : buildings) {
            // entering event: height negative
            events.add(new int[]{b.l, -b.h});
            // leaving event: height positive
            events.add(new int[]{b.r, b.h});
        }

        // Sort by x, then by signed height so:
        // - entering (-h) happens before leaving (+h) at the same x
        events.sort((a, b) -> {
            if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
            return Integer.compare(a[1], b[1]);
        });

        // Active heights (multiset) using counts.
        // Start with ground level height 0 present once.
        TreeMap<Integer, Integer> heightCount = new TreeMap<>();
        heightCount.put(0, 1);

        int prevMax = 0;
        List<KeyPoint> result = new ArrayList<>();

        for (int[] e : events) {
            int x = e[0];
            int h = e[1];

            if (h < 0) { 
                // entering (add this height)
                int height = -h;
                heightCount.put(height, heightCount.getOrDefault(height, 0) + 1);
            } else { 
                // leaving(remove this height)
                int height = h;
                int c = heightCount.getOrDefault(height, 0);
                if (c <= 1) heightCount.remove(height);
                else heightCount.put(height, c - 1);
            }

            // Current skyline height is the max active height
            int currMax = heightCount.lastKey();

            // Skyline changes only when the max height changes
            if (currMax != prevMax) {
                result.add(new KeyPoint(x, currMax));
                prevMax = currMax;
            }
        }

        // Compress repeated x-values (keep only last keypoint at a given x)
        return compressSameX(result);
    }

    /**
     * If multiple keypoints share the same x, keep only the last one.
     * Uses a LinkedHashMap to preserve insertion order.
     */
    private static List<KeyPoint> compressSameX(List<KeyPoint> pts) {
        if (pts.isEmpty()) return pts;

        Map<Integer, KeyPoint> lastAtX = new LinkedHashMap<>();
        for (KeyPoint p : pts) lastAtX.put(p.x, p);

        return new ArrayList<>(lastAtX.values());
    }

    /**
     * Normalizes raw skyline keypoints to 0..1 space for canvas rendering.
     * - x is normalized based on (x - minX) / (maxX - minX)
     * - y is normalized based on y / maxY
     */
    private static Result normalize(List<KeyPoint> skyline) {
        if (skyline.isEmpty()) return new Result(List.of(), 0, 0, 0);

        int minX = skyline.stream().mapToInt(KeyPoint::x).min().orElse(0);
        int maxX = skyline.stream().mapToInt(KeyPoint::x).max().orElse(0);
        int maxY = skyline.stream().mapToInt(KeyPoint::y).max().orElse(0);

         // Avoid division by zero if all x's or y's are identical
        double dx = Math.max(1, maxX - minX);
        double dy = Math.max(1, maxY);

        List<Point> normalized = skyline.stream()
                .map(p -> new Point((p.x - minX) / dx, p.y / dy))
                .collect(Collectors.toList());

        // Add a starting point at left baseline if you want a nicer shape (optional)
        // Add an ending point at baseline (optional)
        return new Result(normalized, minX, maxX, maxY);
    }
}