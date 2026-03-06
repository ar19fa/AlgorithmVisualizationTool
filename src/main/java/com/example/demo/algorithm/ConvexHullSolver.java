package com.example.demo.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Convex Hull solver used by the visualizer.
 * Computes the convex hull using the Monotonic Chain algorithm and records step-by-step actions for animation.
 */
public class ConvexHullSolver {

    /** Raw integer point as read from input (matches file coordinates). */
    public record IntPoint(int x, int y) {}

    /** Normalized point (0..1) used by the frontend canvas renderer. */
    public record Point(double x, double y) {}

    public record Step(
            String phase,      // "upper" or "lower"
            String action,     // "push" or "pop"
            Point candidate,   // normalized candidate point
            Point removed,     // normalized removed point (null if none)
            List<Point> stack, // normalized stack after the action
            long cross         // cross product used when popping (0 when not applicable)
    ) {}

    /**
     * Full solver result returned to the frontend:
     * - meta: raw bounds for scaling/labels
     * - inputPoints/hullPoints: normalized points for drawing
     * - inputRaw/hullRaw: raw integer points for debugging / text output
     * - steps: step-by-step actions for animation
     */
    public record Result(
            Map<String, Integer> meta,
            List<Point> inputPoints,
            List<Point> hullPoints,
            List<IntPoint> inputRaw,
            List<IntPoint> hullRaw,
            List<Step> steps
    ) {}

    /**
     * Parses points, computes hull, and returns both raw + normalized data plus animation steps.
     */
    public static Result solveFromText(String input) {
        List<IntPoint> pts = parsePoints(input);

        // Compute raw bounds for normalization metadata
        int minX = pts.stream().mapToInt(IntPoint::x).min().orElse(0);
        int maxX = pts.stream().mapToInt(IntPoint::x).max().orElse(0);
        int minY = pts.stream().mapToInt(IntPoint::y).min().orElse(0);
        int maxY = pts.stream().mapToInt(IntPoint::y).max().orElse(0);

        // Scale factors (avoid divide-by-zero)
        double dx = Math.max(1, maxX - minX);
        double dy = Math.max(1, maxY - minY);

        // Run hull computation + step tracking
        HullComputation hullComputation = convexHullWithSteps(pts, minX, minY, dx, dy);
        List<IntPoint> hull = hullComputation.hullRaw();
        List<Step> steps = hullComputation.steps();

        // Normalize input points for frontend drawing
        List<Point> inputNorm = new ArrayList<>();
        for (IntPoint p : pts) {
            inputNorm.add(normalize(p, minX, minY, dx, dy));
        }

        // Normalize input points for frontend drawing
        List<Point> hullNorm = new ArrayList<>();
        for (IntPoint p : hull) {
            hullNorm.add(normalize(p, minX, minY, dx, dy));
        }

        // Metadata returned to the frontend (useful for scaling/labels/debug)
        Map<String, Integer> meta = Map.of(
                "minX", minX, "maxX", maxX,
                "minY", minY, "maxY", maxY
        );

        return new Result(meta, inputNorm, hullNorm, pts, hull, steps);
    }

    // ----------------------------
    // Parsing
    // ----------------------------

    /**
     * Reads raw (x, y) integer points from text input.
     * - Ignores blank lines and comment lines starting with '#'
     * - Accepts comma and/or whitespace separators
     * - Ignores single-value lines (often a count like "10")
     */
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

    // ----------------------------
    // Convex Hull (Monotonic Chain) + Step Tracking
    // ----------------------------

    /**
     * Computes the convex hull using the Monotonic Chain algorithm.
     *
     * Also records animation steps:
     * - "push": when a point is added to the current stack
     * - "pop": when a point is removed due to a non-left turn (cross <= 0)
     *
     * Returns hull points in CCW order.
     */
    private static HullComputation convexHullWithSteps(List<IntPoint> points, int minX, int minY, double dx, double dy) {
        if (points.size() <= 1) return new HullComputation(points, List.of());

        // Sort points by x, then y (required by monotonic chain)
        List<IntPoint> pts = new ArrayList<>(points);
        pts.sort(Comparator.comparingInt(IntPoint::x).thenComparingInt(IntPoint::y));

        List<Step> steps = new ArrayList<>();

        //Build lower hull
        List<IntPoint> lower = new ArrayList<>();
        for (IntPoint p : pts) {
            while (lower.size() >= 2) {
                long c = cross(lower.get(lower.size()-2), lower.get(lower.size()-1), p);
                //Non left turn: pop last point
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
        // Build upper hull (iterate in reverse)
        List<IntPoint> upper = new ArrayList<>();
        for (int i = pts.size() - 1; i >= 0; i--) {
            IntPoint p = pts.get(i);
            while (upper.size() >= 2) {
                long c = cross(upper.get(upper.size()-2), upper.get(upper.size()-1), p);
                //non left turn: pop last point
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
            //Push candidate point
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

        // Remove duplicate end points (first/last overlap between lower + upper)
        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);

        // Concatenate lower + upper to form full hull (CCW)
        List<IntPoint> hull = new ArrayList<>();
        hull.addAll(lower);
        hull.addAll(upper);

        return new HullComputation(hull, steps);
    }

     /**
     * Cross product of OA x OB (signed area * 2).
     * Positive => left turn, negative => right turn, zero => collinear.
     */
    private static long cross(IntPoint o, IntPoint a, IntPoint b) {
        return (long)(a.x - o.x) * (b.y - o.y) - (long)(a.y - o.y) * (b.x - o.x);
    }

   // ----------------------------
    // Normalization Helpers
    // ----------------------------

    /** Converts a raw integer point into normalized [0,1] coordinates for canvas rendering. */
    private static Point normalize(IntPoint p, int minX, int minY, double dx, double dy) {
        return new Point((p.x - minX) / dx, (p.y - minY) / dy);
    }

    /** Normalizes an entire stack for step snapshots used by the frontend animation. */
    private static List<Point> normalizeStack(List<IntPoint> stack, int minX, int minY, double dx, double dy) {
        List<Point> norm = new ArrayList<>(stack.size());
        for (IntPoint p : stack) {
            norm.add(normalize(p, minX, minY, dx, dy));
        }
        return norm;
    }

    /**
     * Internal return type for hull computation:
     * - hullRaw: raw hull points (integer coordinates)
     * - steps: recorded actions for animation
     */
    private record HullComputation(List<IntPoint> hullRaw, List<Step> steps) {}
}
