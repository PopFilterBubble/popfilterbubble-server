package com.server.popfilterbubbleserver.controller;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PoliticsDTO {
    private Integer conservative;
    private Integer progressive;
    private Integer unclassified;
    private Integer etc;
    private Integer error;
}
