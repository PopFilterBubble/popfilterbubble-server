package com.server.popfilterbubbleserver.service.api_response.video_info;

import java.math.BigInteger;
import lombok.Getter;

@Getter
public class Statistics {
    private BigInteger viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
}
