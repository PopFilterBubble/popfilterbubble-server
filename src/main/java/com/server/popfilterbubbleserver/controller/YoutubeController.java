package com.server.popfilterbubbleserver.controller;

import com.server.popfilterbubbleserver.service.YoutubeService;
import com.server.popfilterbubbleserver.service.api_response.channel.ChannelApiResult;
import com.server.popfilterbubbleserver.service.api_response.video.VideoApiResult;
import com.server.popfilterbubbleserver.service.api_response.video_comment.VideoCommentApiResult;
import com.server.popfilterbubbleserver.service.api_response.video_info.VideoInfoApiResult;
import com.server.popfilterbubbleserver.util.ErrorMessages;
import java.io.IOException;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}