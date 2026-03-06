package com.example.demo;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.AlgorithmService;

/**
 * REST API controller that executes algorithms.
 * Accepts a file upload + algorithm choice and returns JSON results for the frontend visualizer.
 */
@RestController
public class RunController {

    private final AlgorithmService algorithmService;

    /**
     * Dependency injection constructor.
     * AlgorithmService performs the actual algorithm dispatch + execution.
     */
    public RunController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /**
     * Runs the selected algorithm using the uploaded file content as input.
     *
     * Request: multipart/form-data
     *  - algorithm: String (e.g., SKYLINE, BFS, DFS, HULL)
     *  - file: uploaded .txt/.csv containing the dataset
     *
     * Response:
     *  - JSON-friendly Map payload containing solver output (used for canvas animations + text output panels)
     */
    @PostMapping(value = "/run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> run(
            @RequestParam("algorithm") String algorithm,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        // Convert the uploaded file bytes into a UTF-8 string for parsing by the solver
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // Dispatch to the correct solver and return a JSON-ready response
        return algorithmService.run(algorithm, content);
    }
}