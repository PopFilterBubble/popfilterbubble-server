package com.server.popfilterbubbleserver.service;

import com.server.popfilterbubbleserver.controller.PoliticsDTO;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

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

    public PoliticsDTO getPoliticsDto(String[] channelIds) throws IOException {
        int conservativeCount = 0;
        int progressiveCount = 0;
        int unclassifiedCount = 0;
        int etcCount = 0;
        for(String channelId : channelIds) {
            if(channelId.contains("@"))
                channelId = convertCustomIdToChannelId(channelId);
            ChannelApiResult channelApiResult = getChannelInfoByChannelId(channelId).getBody();
            saveYoutubeChannelInfo(channelId, channelApiResult);
            YoutubeChannelEntity youtubeChannelEntity = youtubeRepository.findById(channelId).get();
            if(youtubeChannelEntity.getTopicId() == CONSERVATIVE)
                conservativeCount++;
            else if(youtubeChannelEntity.getTopicId() == PROGRESSIVE)
                progressiveCount++;
            else if(youtubeChannelEntity.getTopicId() == UNCLASSIFIED)
                unclassifiedCount++;
            else if(youtubeChannelEntity.getTopicId() == ETC)
                etcCount++;
        }
        return PoliticsDTO.builder()
            .conservative(conservativeCount)
            .progressive(progressiveCount)
            .unclassified(unclassifiedCount)
            .etc(etcCount)
            .build();
    }

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

    public void saveYoutubeChannelInfo(String channelId, ChannelApiResult channelApiResult) {
        if(youtubeRepository.existsById(channelId))
            return;
        if (channelApiResult != null && channelApiResult.getItems() != null) {
            for (Items item : channelApiResult.getItems()) {
                Snippet snippet = item.getSnippet();
                Statistics statistics = item.getStatistics();
                TopicDetails topicDetails = item.getTopicDetails();

                YoutubeChannelEntity entity = new YoutubeChannelEntity();

                // topicId 분류 및 저장
                if (isPolitic(topicDetails)) {
                    entity.setPolitic(true);
                    if (checkVideoCount(statistics)) {
                        // todo 최근 100개의 비디오 정보 가져오기
                        VideoApiResult videoApiResult = getVideoInfoByChannelId(channelId).getBody();
                        com.server.popfilterbubbleserver.service.api_response.video.Items[] videoItems = videoApiResult.getItems();

                        ArrayList<String> videoInfos = new ArrayList<>();
                        for(int i = 0; i < videoItems.length; i++) {
                            VideoInfoApiResult videoInfoApiResult = getVideoDetailInfoByVideoId(videoItems[i].getId().getVideoId()).getBody();
                            VideoCommentApiResult videoCommentApiResult = getCommentInfoByVideoId(videoItems[i].getId().getVideoId()).getBody();
                            videoInfos.add(videoInfoApiResult.getItems()[0].getSnippet().getTitle());
                            videoInfos.add(videoInfoApiResult.getItems()[0].getSnippet().getDescription());
                            videoInfos.add(videoCommentApiResult.getItems()[0].getSnippet().getTopLevelComment().getSnippet().getTextOriginal());
                            com.server.popfilterbubbleserver.service.api_response.video_comment.Items[] commentItems = videoCommentApiResult.getItems();
                            for(int j = 1; j < commentItems.length; j++)
                                videoInfos.add(commentItems[j].getSnippet().getTopLevelComment().getSnippet().getTextOriginal());
                            System.out.println(videoInfos);
                        }
                        entity.setTopicId(CONSERVATIVE);
                    } else entity.setTopicId(UNCLASSIFIED);
                } else {
                    entity.setPolitic(false);
                    entity.setTopicId(ETC);
                }
                System.out.println(entity.getTopicId());

                // 기타 정보 저장
                entity.setChannelId(channelId);
                entity.setTitle(snippet.getTitle());
                entity.setDescription(snippet.getDescription());
                entity.setCustomId(snippet.getCustomUrl());
                entity.setSubscriberCount(statistics.getSubscriberCount());
                entity.setVideoCount(statistics.getVideoCount());
                youtubeRepository.save(entity);
            }
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