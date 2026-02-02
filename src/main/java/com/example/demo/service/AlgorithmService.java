package com.example.demo.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.algorithm.BfsSolver;
import com.example.demo.algorithm.DfsSolver;
import com.example.demo.algorithm.SkylineSolver;



@Service
public class AlgorithmService {

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
            default -> {
                return Map.of("error", "Unknown algorithm: " + algorithm);
            }
        }
    }
}