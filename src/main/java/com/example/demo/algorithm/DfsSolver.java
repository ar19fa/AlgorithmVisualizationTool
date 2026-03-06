package com.example.demo.algorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * Iterative DFS solver using an adjacency matrix input.
 *
 * Key idea:
 * - Uses an explicit stack instead of recursion
 * - Tracks discovery order for animation
 * - Records DFS tree edges (parent -> child) in the order nodes are discovered
 */
public class DfsSolver {

    /**
     * Result object returned to the visualizer/UI layer.
     *
     * @param n      number of vertices
     * @param source starting vertex index
     * @param order  vertices in the order they are first visited
     * @param edges  DFS tree edges (parent -> child) in discovery order
     */
    public record Result(
            int n,
            int source,
            List<Integer> order,
            List<int[]> edges
    ) {}

    /**
     * Runs iterative DFS from a given source on a graph described by text input.
     *
     * Input format expected (after stripping blanks/comments):
     *   Line 1: n
     *   Next n lines: n integers (0/1) representing the adjacency matrix
     *
     * @param input  raw text containing adjacency matrix
     * @param source starting vertex index
     * @return DFS traversal information for animation/output
     */
    public static Result solveFromText(String input, int source) {
        int[][] g = parseAdjMatrix(input);
        int n = g.length;

        // marked[v] == 1 means v has been visited (discovered) already
        int[] marked = new int[n];

        // Two parallel stacks:
        // - stackV stores the vertex being processed
        // - stackP stores that vertex's parent in the DFS tree
        int[] stackV = new int[n * n];
        int[] stackP = new int[n * n];
        int top = 0;

        List<Integer> order = new ArrayList<>();
        List<int[]> edges = new ArrayList<>();

        // Push source with no parent (-1)
        stackV[top] = source;
        stackP[top] = -1;
        top++;
    
        // Process until stack is empty
        while (top > 0) {
        top--;
        int v = stackV[top];
        int p = stackP[top];

            // Only process a vertex the first time we pop it (prevents repeats)
            if (marked[v] == 0) {
                marked[v] = 1;
                order.add(v);

                // Record the tree edge (parent -> v), except for the root
                if (p != -1) {
                    edges.add(new int[]{p, v});
                }

                // Push all neighbors (order matters: increasing w)
                for (int w = 0; w < n; w++) {
                    if (g[v][w] == 1) {
                        stackV[top] = w;
                        stackP[top] = v;
                     top++;
                    }
                }
            }
        }
        return new Result(n, source, order, edges);
    }

    /**
     * Parses an adjacency matrix from the provided text.
     * - Ignores blank lines
     * - Ignores comment lines starting with '#'
     * - Accepts commas and/or whitespace as separators
     */
    private static int[][] parseAdjMatrix(String input) {
        List<String> lines = new ArrayList<>();
        for (String raw : input.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            lines.add(line);
        }

        int n = Integer.parseInt(lines.get(0).trim());
        int[][] adj = new int[n][n];

        for (int i = 0; i < n; i++) {
            String[] parts = lines.get(i + 1).trim().split("[,\\s]+");
            for (int j = 0; j < n; j++) {
                adj[i][j] = Integer.parseInt(parts[j]);
            }
        }
        return adj;
    }
}
