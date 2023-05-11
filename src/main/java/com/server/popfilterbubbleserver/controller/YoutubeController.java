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

    @GetMapping("/commentInfo")
    public VideoCommentApiResult commentInfo(@RequestParam String videoId) {
        return youtubeService.getCommentInfoByVideoId(videoId).getBody();
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

    @PostMapping("/saveInfo")
    public ResponseEntity<String> saveYoutubeChannelInfo(@RequestParam String channelId, @RequestBody ChannelApiResult channelApiResult) {
        youtubeService.saveYoutubeChannelInfo(channelId, channelApiResult);
        return ResponseEntity.ok("Success");
    }

    @GetMapping("/politics")
    public PoliticsDTO getPolitics(@RequestParam String[] channelId) throws IOException {
        return youtubeService.getPoliticsDto(channelId);
    }

    @GetMapping("/test")
    public Map<String, Integer> test(@RequestParam String t) throws IOException {
        return youtubeService.test(t);
    }

}