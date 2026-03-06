package com.example.demo.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.algorithm.BfsSolver;
import com.example.demo.algorithm.ConvexHullSolver;
import com.example.demo.algorithm.DfsSolver;
import com.example.demo.algorithm.SkylineSolver;

/**
 * Backend dispatcher for algorithm runs.
 * Routes the selected algorithm name to the correct solver and returns a JSON-friendly response map.
 */
@Service
public class AlgorithmService {

    /**
     * Runs the requested algorithm on the given input text and returns
     * a Map<String, Object> that can be serialized directly into JSON.
     *
     * @param algorithm algorithm identifier (e.g., SKYLINE, BFS, DFS, HULL)
     * @param input     raw file/text input
     * @return JSON-friendly result map for the frontend
     */
    public Map<String, Object> run(String algorithm, String input) {
        switch (algorithm.toUpperCase()) {
            case "SKYLINE" -> {
                var result = SkylineSolver.solveFromText(input);
                return Map.of(
                        "meta", Map.of("minX", result.minX(), "maxX", result.maxX(), "maxY", result.maxY()),
                        "points", result.normalizedPoints()
                );
            }
            case "BFS" -> {
                var r = BfsSolver.solveFromText(input, 0);
                return Map.of(
                "algo", "BFS",
                "n", r.n(),
                "source", r.source(),
                "order", r.discoveredOrder(),
                "edges", r.edgesPredV()
                );
            }
            case "DFS" -> {
                var r = DfsSolver.solveFromText(input, 0);
                return Map.of(
                "algo", "DFS",
                "n", r.n(),
                "source", r.source(),
                "order", r.order(),
                "edges", r.edges()
                );
            }
            case "HULL" -> {
                var r = ConvexHullSolver.solveFromText(input);
                return Map.of(
                "algo", "HULL",
                "meta", r.meta(),
                "inputPoints", r.inputPoints(),
                "hullPoints", r.hullPoints(),
                "steps", r.steps(),
                "inputRaw", r.inputRaw(),
                "hullRaw", r.hullRaw()
                );
            }
            default -> {
                // If the algorithm string is not recognized, return an error payload
                return Map.of("error", "Unknown algorithm: " + algorithm);
            }
        }
    }
}
