package com.server.popfilterbubbleserver.controller;

import com.server.popfilterbubbleserver.service.YoutubeService;
import com.server.popfilterbubbleserver.service.api_response.channel.ChannelApiResult;
import com.server.popfilterbubbleserver.service.api_response.video.VideoApiResult;
import com.server.popfilterbubbleserver.service.api_response.vidoe_info.VideoInfoApiResult;
import com.server.popfilterbubbleserver.util.ErrorMessages;
import java.io.IOException;
import java.net.URISyntaxException;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/youtube")
public class YoutubeController {

    private final YoutubeService youtubeService;

    @GetMapping("/channelInfo")
    public ChannelApiResult channelInfo(@RequestParam String channelId){
        return youtubeService.getChannelInfoByChannelId(channelId).getBody();
    }

    @GetMapping("/videoInfo")
    public VideoApiResult videoInfo(@RequestParam String channelId) {
        return youtubeService.getVideoInfoByChannelId(channelId).getBody();
    }

    @GetMapping("/videoInfoByVideoId")
    public VideoInfoApiResult videoInfoByVideoId(@RequestParam String videoId) {
        return youtubeService.getVideoDetailInfoByVideoId(videoId).getBody();
    }

    @GetMapping("/customId/{customId}")
    public ResponseEntity<String> getChannelIdByCustomId(@PathVariable String customId) {
        try {
            String channelId = youtubeService.convertCustomIdToChannelId(customId);
            return ResponseEntity.ok().body(channelId);
        } catch (IOException e) {
            return ResponseEntity.status(404).body(ErrorMessages.CHANNEL_ID_NOT_FOUND);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(ErrorMessages.SERVER_ERROR);
        }
    }
}