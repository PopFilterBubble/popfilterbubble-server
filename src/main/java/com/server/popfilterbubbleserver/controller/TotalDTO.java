package com.server.popfilterbubbleserver.controller;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TotalDTO {
    private PoliticsDTO politicsDTO;
    private List<VideoListDTO> videoListDTO;
}
