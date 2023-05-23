package com.server.popfilterbubbleserver.service.api_response.channel;

import java.math.BigInteger;
import lombok.Getter;

@Getter
public class Statistics {
    private BigInteger viewCount;
    private Integer subscriberCount;
    private Integer videoCount;
    private Boolean hiddenSubscriberCount;
}
