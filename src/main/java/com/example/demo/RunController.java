package com.example.demo;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.AlgorithmService;

@RestController
public class RunController {

    private final AlgorithmService algorithmService;

    public RunController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    @PostMapping(value = "/run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> run(
            @RequestParam("algorithm") String algorithm,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return algorithmService.run(algorithm, content);
    }
}