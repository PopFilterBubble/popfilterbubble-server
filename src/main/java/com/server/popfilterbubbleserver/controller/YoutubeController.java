package com.server.popfilterbubbleserver.controller;

import com.server.popfilterbubbleserver.service.YoutubeService;
import com.server.popfilterbubbleserver.util.ErrorMessages;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/youtube")
public class YoutubeController {

    private final YoutubeService youtubeService;

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