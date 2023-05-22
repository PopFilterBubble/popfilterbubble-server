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

    private boolean isProcessing = false;

    @GetMapping("/politics")
    public TotalDTO getPolitics(@RequestParam String[] channelId) throws IOException {
        if(isProcessing)
            return null;

        isProcessing = true;
        try{
        PoliticsDTO politicsDTO = youtubeService.getPoliticsDto(channelId);
        List<VideoListDTO> videoListDTOS = youtubeService.getVideoListDto(channelId);
        return TotalDTO.builder()
                .politicsDTO(politicsDTO)
                .videoListDTO(videoListDTOS)
                .build();
        } finally {
            isProcessing = false;
        }
    }
}