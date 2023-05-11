package com.server.popfilterbubbleserver.service;

import com.server.popfilterbubbleserver.service.api_response.channel.ChannelApiResult;
import com.server.popfilterbubbleserver.service.api_response.channel.Items;
import com.server.popfilterbubbleserver.service.api_response.channel.Snippet;
import com.server.popfilterbubbleserver.service.api_response.channel.Statistics;
import com.server.popfilterbubbleserver.service.api_response.channel.TopicDetails;
import com.server.popfilterbubbleserver.service.api_response.video.VideoApiResult;
import com.server.popfilterbubbleserver.service.api_response.video_comment.VideoCommentApiResult;
import com.server.popfilterbubbleserver.service.api_response.video_info.VideoInfoApiResult;
import com.server.popfilterbubbleserver.module.YoutubeChannelEntity;
import com.server.popfilterbubbleserver.repository.YoutubeRepository;
import com.server.popfilterbubbleserver.util.ErrorMessages;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class YoutubeService {

    private final int CONSERVATIVE = 0;
    private final int PROGRESSIVE = 1;
    private final int UNCLASSIFIED = 2;
    private final int ETC = 3;


    @Autowired
    private YoutubeRepository youtubeRepository;

    @Value("${youtube_api_key}")
    private String youtube_api_key;

    public HttpEntity<String> setHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }

    public ResponseEntity<?> getResponse(String url, Object classType) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<?> response = null;

        for (int i = 0; i < 10; i++) {
            try {
                response = restTemplate.exchange(url, HttpMethod.GET, setHeaders(), classType.getClass());
                log.info("response : " + response.getBody());
                return response;
            } catch (Exception e) {
                log.error(i + "번 url : " + url + " error : " + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }

    public ResponseEntity<ChannelApiResult> getChannelInfoByChannelId(String channelID) {
        String url = "https://youtube.googleapis.com/youtube/v3/channels";
        url += "?part=snippet,statistics,topicDetails";
        url += "&id=" + channelID;
        url += "&key=" + youtube_api_key;

        return (ResponseEntity<ChannelApiResult>) getResponse(url, new ChannelApiResult());
    }

    public ResponseEntity<VideoApiResult> getVideoInfoByChannelId(String channelID) {
        String url = "https://youtube.googleapis.com/youtube/v3/search";
        url += "?part=snippet";
        url += "&channelId=" + channelID;
        url += "&maxResults=50";
        url += "&order=date";
        url += "&key=" + youtube_api_key;

        return (ResponseEntity<VideoApiResult>) getResponse(url, new VideoApiResult());
    }

    public ResponseEntity<VideoInfoApiResult> getVideoDetailInfoByVideoId(String videoId) {
        String url = "https://youtube.googleapis.com/youtube/v3/videos";
        url += "?part=snippet,statistics,topicDetails";
        url += "&id=" + videoId;
        url += "&key=" + youtube_api_key;

        return (ResponseEntity<VideoInfoApiResult>) getResponse(url, new VideoInfoApiResult());
    }

    public ResponseEntity<VideoCommentApiResult> getCommentInfoByVideoId(String videoId) {
        String url = "https://youtube.googleapis.com/youtube/v3/commentThreads";
        url += "?part=snippet";
        url += "&videoId=" + videoId;
        url += "&order=relevance";
        url += "&maxResults=100";
        url += "&key=" + youtube_api_key;

        return (ResponseEntity<VideoCommentApiResult>) getResponse(url, new VideoCommentApiResult());
    }

    public String convertCustomIdToChannelId(String customId) throws IOException {
        String url = "https://www.youtube.com/" + customId;
        return extractChannelIdFromHtml(url);
    }

    public String extractChannelIdFromHtml(String url) throws IOException {
        Document document = Jsoup.connect(url).get();
        Element channelIdElement = document.selectFirst("meta[itemprop=identifier]");
        if (channelIdElement != null) {
            return channelIdElement.attr("content");
        } else {
            throw new IOException(ErrorMessages.CHANNEL_ID_NOT_FOUND);
        }
    }

    public void saveYoutubeChannelInfo(String channelId, ChannelApiResult channelApiResult) {
        if (channelApiResult != null && channelApiResult.getItems() != null) {
            Items item = channelApiResult.getItems()[0];
            Snippet snippet = item.getSnippet();
            Statistics statistics = item.getStatistics();
            TopicDetails topicDetails = item.getTopicDetails();
            YoutubeChannelEntity entity = new YoutubeChannelEntity();

            // topicId 분류 및 저장
            if (isPolitic(topicDetails)) {
                if (checkVideoCount(statistics)) {
                    // todo 최근 100개의 비디오 정보 가져오기
                } else entity.saveChannelInfo(snippet, statistics, channelId, true, UNCLASSIFIED);
            } else entity.saveChannelInfo(snippet, statistics, channelId, false, ETC);
            youtubeRepository.save(entity);
        }
    }

    // 정치 카테고리 판단
    public Boolean isPolitic(TopicDetails topicDetails) {
        for (String category : topicDetails.getTopicCategories()) {
            if (category.contains("Politics")) return true;
        }
        return false;
    }

    // 비디오 개수 판단
    public Boolean checkVideoCount(Statistics statistics) {
        return Integer.parseInt(statistics.getVideoCount()) >= 100;
    }
}