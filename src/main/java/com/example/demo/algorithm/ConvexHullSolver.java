package com.example.demo.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ConvexHullSolver {

    public record IntPoint(int x, int y) {}
    public record Point(double x, double y) {}

    public record Step(
            String phase,      // "upper" or "lower"
            String action,     // "push" or "pop"
            Point candidate,   // normalized candidate point
            Point removed,     // normalized removed point (null if none)
            List<Point> stack, // normalized stack after the action
            long cross         // cross product used when popping (0 when not applicable)
    ) {}

    public record Result(
            Map<String, Integer> meta,
            List<Point> inputPoints,
            List<Point> hullPoints,
            List<IntPoint> inputRaw,
            List<IntPoint> hullRaw,
            List<Step> steps
    ) {}

    public static Result solveFromText(String input) {
        List<IntPoint> pts = parsePoints(input);

        // meta for normalization
        int minX = pts.stream().mapToInt(IntPoint::x).min().orElse(0);
        int maxX = pts.stream().mapToInt(IntPoint::x).max().orElse(0);
        int minY = pts.stream().mapToInt(IntPoint::y).min().orElse(0);
        int maxY = pts.stream().mapToInt(IntPoint::y).max().orElse(0);

        double dx = Math.max(1, maxX - minX);
        double dy = Math.max(1, maxY - minY);

        HullComputation hullComputation = convexHullWithSteps(pts, minX, minY, dx, dy);
        List<IntPoint> hull = hullComputation.hullRaw();
        List<Step> steps = hullComputation.steps();

        List<Point> inputNorm = new ArrayList<>();
        for (IntPoint p : pts) {
            inputNorm.add(normalize(p, minX, minY, dx, dy));
        }

        List<Point> hullNorm = new ArrayList<>();
        for (IntPoint p : hull) {
            hullNorm.add(normalize(p, minX, minY, dx, dy));
        }

        Map<String, Integer> meta = Map.of(
                "minX", minX, "maxX", maxX,
                "minY", minY, "maxY", maxY
        );

        return new Result(meta, inputNorm, hullNorm, pts, hull, steps);
    }

    // -------- parsing --------
    private static List<IntPoint> parsePoints(String input) {
        List<IntPoint> pts = new ArrayList<>();
        String[] lines = input.split("\\R");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("[,\\s]+");

            // ignore count line like "10"
            if (parts.length == 1) continue;
            if (parts.length < 2) continue;

            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            pts.add(new IntPoint(x, y));
        }
        return pts;
    }

    // -------- hull algorithm (Monotonic Chain) with step tracking --------
    private static HullComputation convexHullWithSteps(List<IntPoint> points, int minX, int minY, double dx, double dy) {
        if (points.size() <= 1) return new HullComputation(points, List.of());

        // sort copy
        List<IntPoint> pts = new ArrayList<>(points);
        pts.sort(Comparator.comparingInt(IntPoint::x).thenComparingInt(IntPoint::y));

        List<Step> steps = new ArrayList<>();

        List<IntPoint> lower = new ArrayList<>();
        for (IntPoint p : pts) {
            while (lower.size() >= 2) {
                long c = cross(lower.get(lower.size()-2), lower.get(lower.size()-1), p);
                if (c <= 0) {
                    IntPoint removed = lower.remove(lower.size()-1);
                    steps.add(new Step(
                            "lower",
                            "pop",
                            normalize(p, minX, minY, dx, dy),
                            normalize(removed, minX, minY, dx, dy),
                            normalizeStack(lower, minX, minY, dx, dy),
                            c
                    ));
                } else {
                    break;
                }
            }
            lower.add(p);
            steps.add(new Step(
                    "lower",
                    "push",
                    normalize(p, minX, minY, dx, dy),
                    null,
                    normalizeStack(lower, minX, minY, dx, dy),
                    0
            ));
        }

        List<IntPoint> upper = new ArrayList<>();
        for (int i = pts.size() - 1; i >= 0; i--) {
            IntPoint p = pts.get(i);
            while (upper.size() >= 2) {
                long c = cross(upper.get(upper.size()-2), upper.get(upper.size()-1), p);
                if (c <= 0) {
                    IntPoint removed = upper.remove(upper.size()-1);
                    steps.add(new Step(
                            "upper",
                            "pop",
                            normalize(p, minX, minY, dx, dy),
                            normalize(removed, minX, minY, dx, dy),
                            normalizeStack(upper, minX, minY, dx, dy),
                            c
                    ));
                } else {
                    break;
                }
            }
            upper.add(p);
            steps.add(new Step(
                    "upper",
                    "push",
                    normalize(p, minX, minY, dx, dy),
                    null,
                    normalizeStack(upper, minX, minY, dx, dy),
                    0
            ));
        }

        // remove duplicates at ends
        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);

        List<IntPoint> hull = new ArrayList<>();
        hull.addAll(lower);
        hull.addAll(upper);

        return new HullComputation(hull, steps); // CCW order
    }

    private static long cross(IntPoint o, IntPoint a, IntPoint b) {
        return (long)(a.x - o.x) * (b.y - o.y) - (long)(a.y - o.y) * (b.x - o.x);
    }

    private static Point normalize(IntPoint p, int minX, int minY, double dx, double dy) {
        return new Point((p.x - minX) / dx, (p.y - minY) / dy);
    }

    private static List<Point> normalizeStack(List<IntPoint> stack, int minX, int minY, double dx, double dy) {
        List<Point> norm = new ArrayList<>(stack.size());
        for (IntPoint p : stack) {
            norm.add(normalize(p, minX, minY, dx, dy));
        }
        return norm;
    }

    private record HullComputation(List<IntPoint> hullRaw, List<Step> steps) {}
}
