package com.example.demo.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BFS (Breadth-First Search) solver for an unweighted graph given as an adjacency matrix.
 *
 * Input format expected (after stripping blanks/comments):
 *   Line 1: n
 *   Next n lines: n integers (0/1) representing the adjacency matrix
 *
 * This implementation matches your existing logic:
 * - Uses a simple int[] queue
 * - Uses dist[] as both distance + visitation check (via "infinite" default)
 * - Uses pred[] to reconstruct discovery edges in BFS order
 */
public class BfsSolver {
    
       /**
     * Encapsulates everything needed by the UI/visualizer:
     *  - n: number of vertices
     *  - source: start node for BFS
     *  - adj: adjacency matrix
     *  - dist: BFS distance (100000000 represents "unvisited")
     *  - pred: predecessor array (parent in BFS tree, -1 if none)
     *  - discoveredOrder: nodes in the order they were first discovered
     *  - edgesPredV: edges of the BFS tree as (pred[v], v) in discovery order
     */
    public record Result(
            int n,
            int source,
            int[][] adj,
            int[] dist,
            int[] pred,
            List<Integer> discoveredOrder,
            List<int[]> edgesPredV
    ) {}

    /**
     * Runs BFS from a given source on a graph described by text input.
     *
     * @param input  text containing n + adjacency matrix lines
     * @param source starting vertex index
     * @return Result object containing traversal data for animation/output
     */

    public static Result solveFromText(String input, int source) {
        int[][] adj = parseAdjMatrix(input);
        int n = adj.length;

        // dist[v] = shortest number of edges from source to v (or "infinite" if unvisited)
        int[] dist = new int[n];

        // pred[v] = predecessor of v in the BFS tree (parent), or -1 if none
        int[] pred = new int[n];

        // Initialize arrays
        Arrays.fill(dist, 100000000); // sentinel "infinity" used by your code
        Arrays.fill(pred, -1);
        dist[source] = 0;

        // Fixed-size queue (capacity n is enough for BFS over n vertices)
        int[] q = new int[n];
        int front = 0, back = 0;

        // Tracks the discovery order (important for animation/edge ordering)
        List<Integer> order = new ArrayList<>();
        order.add(source);

        // Enqueue source
        q[back++] = source;

        // Standard BFS loop
        while (front < back) {
            int u = q[front++];

            // Scan adjacency row u
            for (int v = 0; v < n; v++) {
                if (adj[u][v] == 1) {
                    // Discovery / relaxation condition (matches your existing logic)
                    if (dist[v] > dist[u] + 1) { // matches your code
                        dist[v] = dist[u] + 1;
                        pred[v] = u;

                        // Enqueue newly discovered vertex
                        q[back++] = v;

                        // Record discovery order for visualization
                        order.add(v);
                    }
                }
            }
        }

        // Build BFS tree edges in the same order your output prints them:
        // for each discovered vertex after the source, add (pred[v], v)
        List<int[]> edges = new ArrayList<>();
        for (int i = 1; i < order.size(); i++) {
            int v = order.get(i);
            edges.add(new int[]{pred[v], v});
        }

        return new Result(n, source, adj, dist, pred, order, edges);
    }

    /**
     * Parses an adjacency matrix from the provided text.
     * - Ignores blank lines
     * - Ignores comment lines starting with '#'
     * - Accepts commas and/or whitespace as separators
     *
     * @param input raw text from uploaded file or textarea
     * @return adjacency matrix adj[n][n]
     */
    private static int[][] parseAdjMatrix(String input) {
        List<String> lines = new ArrayList<>();

        // Normalize input into cleaned lines
        for (String raw : input.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            lines.add(line);
        }
        //First line =n
        int n = Integer.parseInt(lines.get(0).trim());
        int[][] adj = new int[n][n];

        // Next n lines = matrix rows
        for (int i = 0; i < n; i++) {
            String[] parts = lines.get(i + 1).trim().split("[,\\s]+");
            for (int j = 0; j < n; j++) {
                adj[i][j] = Integer.parseInt(parts[j]);
            }
        }
        return adj;
    }
}
