package com.server.popfilterbubbleserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.auth.In;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Component
public class PoliticResultDTO {
    private Map<String, Integer> conservative;
    private Map<String, Integer> progressive;

    public PoliticResultDTO() throws IOException {
        Resource resource_conservative = new ClassPathResource("CONSERVATIVE.json");
        Resource resource_progressive = new ClassPathResource("PROGRESSIVE.json");
        InputStream inputStream_conservative = resource_conservative.getInputStream();
        InputStream inputStream_progressive = resource_progressive.getInputStream();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Integer> jsonData_conservative = objectMapper.readValue(inputStream_conservative, new TypeReference<>() {
        });
        Map<String, Integer> jsonData_progressive = objectMapper.readValue(inputStream_progressive, new TypeReference<>() {
        });

        this.conservative = jsonData_conservative;
        this.progressive = jsonData_progressive;
    }
}
