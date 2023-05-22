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
    public PoliticsDTO getPolitics(@RequestParam String[] channelId) throws IOException {
        return youtubeService.getPoliticsDto(channelId);
    }

    @GetMapping("/recommends")
    public List<VideoListDTO> getRecommends(@RequestParam String[] channelId) throws IOException {
        return youtubeService.getVideoListDto(channelId);
    }
}