package com.example.demo.algorithm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SkylineSolver {

    // JSON-friendly point (x,y in 0..1 after normalization)
    public record Point(double x, double y) {}

    // Internal key point (raw coordinates)
    public record KeyPoint(int x, int y) {}

    // Building: [L, R, H]
    public record Building(int l, int r, int h) {}

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

    public static Result solveFromText(String input) {
        List<Building> buildings = parseBuildings(input);
        List<KeyPoint> skyline = computeSkyline(buildings);
        System.out.println("Raw skyline keypoints: " + skyline);

        return normalize(skyline);
    }

    private static List<Building> parseBuildings(String input) {
    List<Building> list = new ArrayList<>();
    String[] lines = input.split("\\R");

    for (String line : lines) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;

        // Split on commas OR whitespace
        String[] parts = line.split("[,\\s]+");

        // If the line is just "10" (count), ignore it
        if (parts.length == 1) continue;

        if (parts.length < 3) continue;

        int l = Integer.parseInt(parts[0]);
        int h = Integer.parseInt(parts[1]); // HEIGHT (middle)
        int r = Integer.parseInt(parts[2]); // RIGHT (last)

        if (l < r && h > 0) {
            list.add(new Building(l, r, h)); // your Building is (l,r,h)
        }
    }
    return list;
}


    // Classic sweep line:
    // events: (x, height, entering/leaving)
    // entering uses -height to sort before leaving at same x, etc.
    private static List<KeyPoint> computeSkyline(List<Building> buildings) {
        if (buildings.isEmpty()) return List.of();

        List<int[]> events = new ArrayList<>();
        for (Building b : buildings) {
            // entering event: height negative
            events.add(new int[]{b.l, -b.h});
            // leaving event: height positive
            events.add(new int[]{b.r, b.h});
        }

        events.sort((a, b) -> {
            if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
            return Integer.compare(a[1], b[1]); // entering (-h) before leaving (+h)
        });

        // max-heap using counts (multiset)
        TreeMap<Integer, Integer> heightCount = new TreeMap<>();
        heightCount.put(0, 1);

        int prevMax = 0;
        List<KeyPoint> result = new ArrayList<>();

        for (int[] e : events) {
            int x = e[0];
            int h = e[1];

            if (h < 0) { // entering
                int height = -h;
                heightCount.put(height, heightCount.getOrDefault(height, 0) + 1);
            } else { // leaving
                int height = h;
                int c = heightCount.getOrDefault(height, 0);
                if (c <= 1) heightCount.remove(height);
                else heightCount.put(height, c - 1);
            }

            int currMax = heightCount.lastKey();
            if (currMax != prevMax) {
                result.add(new KeyPoint(x, currMax));
                prevMax = currMax;
            }
        }

        // Optional: ensure it ends at height 0 (usually already included)
        return compressSameX(result);
    }

    // If multiple keypoints share same x, keep only the last one
    private static List<KeyPoint> compressSameX(List<KeyPoint> pts) {
        if (pts.isEmpty()) return pts;
        Map<Integer, KeyPoint> lastAtX = new LinkedHashMap<>();
        for (KeyPoint p : pts) lastAtX.put(p.x, p);
        return new ArrayList<>(lastAtX.values());
    }

    private static Result normalize(List<KeyPoint> skyline) {
        if (skyline.isEmpty()) return new Result(List.of(), 0, 0, 0);

        int minX = skyline.stream().mapToInt(KeyPoint::x).min().orElse(0);
        int maxX = skyline.stream().mapToInt(KeyPoint::x).max().orElse(0);
        int maxY = skyline.stream().mapToInt(KeyPoint::y).max().orElse(0);

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