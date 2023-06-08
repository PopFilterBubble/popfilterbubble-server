package com.server.popfilterbubbleserver.controller;

import com.server.popfilterbubbleserver.service.YoutubeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/youtube")
@CrossOrigin(origins = "*")
public class YoutubeController {
    private final YoutubeService youtubeService;

    @GetMapping("/politics")
    public TotalDTO getPolitics(@RequestParam String[] customId) throws IOException {
        PoliticsDTO politicsDTO = youtubeService.getPoliticsDto(customId);
        List<VideoListDTO> videoListDTOS = youtubeService.getRecommendedVideoList();
        return TotalDTO.builder()
                .politicsDTO(politicsDTO)
                .videoListDTO(videoListDTOS)
                .build();
    }
}