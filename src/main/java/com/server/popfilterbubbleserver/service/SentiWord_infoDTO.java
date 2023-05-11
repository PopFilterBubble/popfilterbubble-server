package com.server.popfilterbubbleserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Component
public class SentiWord_infoDTO {

    private Map<String, String> sentiWord_info;

    public SentiWord_infoDTO() throws IOException {
        Resource resource = new ClassPathResource("SentiWord_info.json");
        InputStream inputStream = resource.getInputStream();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, String>> jsonData = objectMapper.readValue(inputStream, new TypeReference<>() {
        });


        Map<String, String> wordPolarityMap = jsonData.stream()
                .collect(Collectors.toMap(
                        map -> map.get("word"),
                        map -> map.get("polarity"),
                        (oldValue, newValue) -> newValue
                ));

        this.sentiWord_info = wordPolarityMap;
    }
}
