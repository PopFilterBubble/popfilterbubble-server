package com.server.popfilterbubbleserver.service;

import com.server.popfilterbubbleserver.util.ErrorMessages;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class YoutubeService {

    public String convertCustomIdToChannelId(String customId) throws IOException {
        String url = "https://www.youtube.com/" + customId;
        return extractChannelIdFromHtml(url);
    }

    public String extractChannelIdFromHtml(String url) throws IOException {
        Document document = Jsoup.connect(url).get();
        Element channelIdElement = document.selectFirst("meta[itemprop=channelId]");
        if (channelIdElement != null) {
            return channelIdElement.attr("content");
        } else {
            throw new IOException(ErrorMessages.CHANNEL_ID_NOT_FOUND);
        }
    }
}