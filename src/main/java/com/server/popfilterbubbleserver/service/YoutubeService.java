package com.server.popfilterbubbleserver.service;

import com.server.popfilterbubbleserver.module.YoutubeChannelEntity;
import com.server.popfilterbubbleserver.repository.YoutubeRepository;
import com.server.popfilterbubbleserver.util.ErrorMessages;
import java.io.IOException;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class YoutubeService {

    @Autowired
    private YoutubeRepository youtubeRepository;

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

    public void checkVideoCount(String channelId) {
        YoutubeChannelEntity youtubeChannelEntity = youtubeRepository.findById(channelId).orElse(null);

        if (youtubeChannelEntity != null) {
            int videoCount = Integer.parseInt(youtubeChannelEntity.getVideoCount());

            if (videoCount >= 100) {
                // todo 최근 100개의 비디오 정보 가져오기 로직 수행
            } else {
                // topicId를 2로 설정하고 업데이트
                youtubeChannelEntity.setTopicId(2);
                youtubeRepository.save(youtubeChannelEntity);
            }
        }
    }

}