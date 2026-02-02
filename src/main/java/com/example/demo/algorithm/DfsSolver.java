package com.example.demo.algorithm;

import java.util.ArrayList;
import java.util.List;

public class DfsSolver {

    public record Result(
            int n,
            int source,
            List<Integer> order,
            List<int[]> edges
    ) {}

    public static Result solveFromText(String input, int source) {
        int[][] g = parseAdjMatrix(input);
        int n = g.length;

        int[] marked = new int[n];

        // stack (oversized like yours)
        int[] stack = new int[n * n];
        int top = 0;

        List<Integer> order = new ArrayList<>();
        List<int[]> edges = new ArrayList<>();

        // push source
        stack[top++] = source;

        while (top > 0) {
            int v = stack[--top];

            if (marked[v] == 0) {
                marked[v] = 1;
                order.add(v);

                for (int w = 0; w < n; w++) {
                    if (g[v][w] == 1 && marked[w] == 0) {
                        edges.add(new int[]{v, w}); // tree edge when discovered
                        stack[top++] = w;
                    }
                }
            }
        }

        return new Result(n, source, order, edges);
    }

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
