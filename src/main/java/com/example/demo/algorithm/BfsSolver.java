package com.example.demo.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BfsSolver {

    public record Result(
            int n,
            int source,
            int[][] adj,
            int[] dist,
            int[] pred,
            List<Integer> discoveredOrder,
            List<int[]> edgesPredV
    ) {}

    public static Result solveFromText(String input, int source) {
        int[][] adj = parseAdjMatrix(input);
        int n = adj.length;

        int[] dist = new int[n];
        int[] pred = new int[n];
        Arrays.fill(dist, 100000000);
        Arrays.fill(pred, -1);
        dist[source] = 0;

        // queue
        int[] q = new int[n];
        int front = 0, back = 0;

        List<Integer> order = new ArrayList<>();
        order.add(source);

        q[back++] = source;

        while (front < back) {
            int u = q[front++];

            for (int v = 0; v < n; v++) {
                if (adj[u][v] == 1) {
                    if (dist[v] > dist[u] + 1) {   // matches your code
                        dist[v] = dist[u] + 1;
                        pred[v] = u;
                        q[back++] = v;
                        order.add(v);
                    }
                }
            }
        }

        // Build edges in the same order your output file prints them
        List<int[]> edges = new ArrayList<>();
        for (int i = 1; i < order.size(); i++) {
            int v = order.get(i);
            edges.add(new int[]{pred[v], v});
        }

        return new Result(n, source, adj, dist, pred, order, edges);
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
