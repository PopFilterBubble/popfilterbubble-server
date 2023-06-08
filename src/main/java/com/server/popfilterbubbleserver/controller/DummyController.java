package com.server.popfilterbubbleserver.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class DummyController {

    @GetMapping("/dummy/politics")
    public TotalDTO getPolitics(@RequestParam String[] channelId) {
        PoliticsDTO politicsDTO = PoliticsDTO.builder()
                .conservative(1)
                .progressive(2)
                .unclassified(3)
                .etc(4)
                .build();
        VideoListDTO videoListDTO = VideoListDTO.builder()
                .videoId("QnKLXUHjIHo")
                .title("[EN] 수지 찾으러 오는 학과 [숭실대 건축학부] | 전과자 ep.20")
                .description("\\\"수지 있어요?\\\" 음대생 수지 찾는 질문에 영원히 시달리는 건축학부 시멘트 가루 날리는 공사판에서 수업 듣고 교수님이 치킨 사주 ...")
                .thumbnailUrl("https://i.ytimg.com/vi/QnKLXUHjIHo/hqdefault.jpg")
                .channelId("UCRmm8763aqO0CoeLCA2jNSA")
                .channelTitle("ootb STUDIO")
                .publishedAt("2023-04-24T09:00:28Z").build();
        List<VideoListDTO> videoListDTOS = new ArrayList<>();
        videoListDTOS.add(videoListDTO);
        return TotalDTO.builder()
                .politicsDTO(politicsDTO)
                .videoListDTO(videoListDTOS)
                .build();
    }

}