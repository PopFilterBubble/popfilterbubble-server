package com.server.popfilterbubbleserver.service;

import com.server.popfilterbubbleserver.controller.PoliticsDTO;
import com.server.popfilterbubbleserver.controller.VideoListDTO;
import com.server.popfilterbubbleserver.module.YoutubeChannelEntity;
import com.server.popfilterbubbleserver.repository.YoutubeRepository;
import com.server.popfilterbubbleserver.service.api_response.channel.*;
import com.server.popfilterbubbleserver.service.api_response.video.VideoApiResult;
import com.server.popfilterbubbleserver.service.api_response.video_comment.VideoCommentApiResult;
import com.server.popfilterbubbleserver.service.api_response.video_info.VideoInfoApiResult;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class YoutubeService implements ApplicationRunner {

    private final YoutubeRepository youtubeRepository;

    private final int CONSERVATIVE = 0;
    private final int PROGRESSIVE = 1;
    private final int UNCLASSIFIED = 2;
    private final int ETC = 3;
    private final int ERROR = 4;

    private static List<VideoListDTO> conservativeVideoList = new ArrayList<>();
    private static List<VideoListDTO> progressiveVideoList = new ArrayList<>();

    private final SentiWord_infoDTO sentiWordInfoDTO;
    private final PoliticResultDTO politicResultDTO;

    @Value("${youtube_api_key}")
    private String youtube_api_key;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        setVideoList();
    }
    @Scheduled(cron = "0 0 0 * * *")
    public void setVideoList(){
        conservativeVideoList = getVideoListDtoByTopicId(100, CONSERVATIVE);
        progressiveVideoList = getVideoListDtoByTopicId(100, PROGRESSIVE);
    }

    public HttpEntity<String> setHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }
    public Integer getPoliticScore(String channelId) {
        ArrayList<String> ast = getAllInfoOfChannel(channelId);
        Map<String, Integer> m = getPolicitalScore(ast);
        // Map의 값을 절댓값 기준으로 내림차순으로 정렬
        List<Map.Entry<String, Integer>> sortedList = m.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        // 상위 100개 요소 선택
        List<Map.Entry<String, Integer>> top100Entries = sortedList.subList(0, Math.min(100, sortedList.size()));

        // 상위 100개 요소를 새로운 Map에 추가
        Map<String, Integer> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : top100Entries) {
            newMap.put(entry.getKey(), entry.getValue());
        }
        double similarityToConservative = calculateSimilarity(newMap,politicResultDTO.getConservative());
        double similarityToProgressive = calculateSimilarity( newMap,politicResultDTO.getProgressive());
        System.out.println("conservative : " + similarityToConservative);
        System.out.println("progressive : " + similarityToProgressive);
        if(similarityToProgressive > similarityToConservative) {
            return PROGRESSIVE;
        } else if(similarityToProgressive < similarityToConservative) {
            return CONSERVATIVE;
        } else {
            return UNCLASSIFIED;
        }
    }

    private double calculateSimilarity(Map<String, Integer> map1, Map<String, Integer> map2) {
        double similarCount = 0;

        for (Map.Entry<String, Integer> entry : map1.entrySet()) {
            String key = entry.getKey();
            double value1 = entry.getValue();
            double value2 = map2.getOrDefault(key, 0);
            if(value1 > 0 && value2 > 0) {
                similarCount = similarCount + 1;
            }
            if(value1 < 0 && value2 < 0) {
                similarCount = similarCount + 1;
            }
        }

        return similarCount;
    }

    public ResponseEntity<?> getResponse(String url, Object classType) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<?> response = null;

        for (int i = 0; i < 10; i++) {
            try {
                response = restTemplate.exchange(url, HttpMethod.GET, setHeaders(), classType.getClass());
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

    public List<ResponseEntity<VideoApiResult>> getVideoInfoByChannelId(String channelID) {
        String url = "https://youtube.googleapis.com/youtube/v3/search";
        url += "?part=snippet";
        url += "&channelId=" + channelID;
        url += "&maxResults=50";
        url += "&order=date";
        url += "&key=" + youtube_api_key;

        ResponseEntity<VideoApiResult> videoApiResult = (ResponseEntity<VideoApiResult>) getResponse(url, new VideoApiResult());
        if (videoApiResult == null) {
            return null;
        }
        String nextPageToken = videoApiResult.getBody().getNextPageToken();

        url += "&pageToken=" + nextPageToken;

        ResponseEntity<VideoApiResult> videoApiResult2 = (ResponseEntity<VideoApiResult>) getResponse(url, new VideoApiResult());

        List<ResponseEntity<VideoApiResult>> list = new ArrayList<>();
        list.add(videoApiResult);
        list.add(videoApiResult2);
        return list;
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
        int errorCount = 0;
        String id = "";
        for(String channelId : channelIds) {
            if(channelId.contains("@")) id = convertCustomIdToChannelId(channelId);
            if(id.equals("")) {
                System.out.println("Error: CHANNEL_ID_NOT_FOUND - customId: " + channelId);
                errorCount++;
                continue;
            }
            ChannelApiResult channelApiResult = getChannelInfoByChannelId(id).getBody();
            saveYoutubeChannelInfo(id, channelApiResult);
            YoutubeChannelEntity youtubeChannelEntity = youtubeRepository.findById(id).orElse(null);
            if(youtubeChannelEntity != null) {
                if(youtubeChannelEntity.getTopicId() == CONSERVATIVE)
                    conservativeCount++;
                else if(youtubeChannelEntity.getTopicId() == PROGRESSIVE)
                    progressiveCount++;
                else if(youtubeChannelEntity.getTopicId() == UNCLASSIFIED)
                    unclassifiedCount++;
                else if(youtubeChannelEntity.getTopicId() == ETC)
                    etcCount++;
            }
            else throw new NoSuchElementException("YoutubeChannelEntity not found. \tchannelId: " + channelId);
        }
        return PoliticsDTO.builder()
                .conservative(conservativeCount)
                .progressive(progressiveCount)
                .unclassified(unclassifiedCount)
                .etc(etcCount)
                .error(errorCount)
                .build();
    }

    public String convertCustomIdToChannelId(String customId) throws IOException {
        String url = "https://www.youtube.com/" + customId;
        return extractChannelIdFromHtml(url, customId);
    }

    public String extractChannelIdFromHtml(String url, String customId) throws IOException {
        int retries = 5;
        int waitTime = 1000;

        for (int i = 0; i < retries; i++) {
            try {
                Document document = Jsoup.connect(url).get();
                Element channelIdElement = document.selectFirst("meta[itemprop=identifier]");
                if (channelIdElement != null) {
                    return channelIdElement.attr("content");
                }
            } catch (IOException e) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return "";
    }


    public Map<String, Integer> getPolicitalScore(ArrayList<String> videoInfos){
        Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
        Map<String, Integer> resultMap = new HashMap<>();
        Map<String, String> sentiWord_info = sentiWordInfoDTO.getSentiWord_info();

        for(String str: videoInfos) {
            if(str == null) continue;
            str = str.replace('\n',' ').replace('\t',' ').replace('\r',' ');
            KomoranResult result;
            try {
                result = komoran.analyze(str);
            }catch (NullPointerException e){
                System.out.println(str);
                continue;
            }
            if(result == null) continue;
            List<Token> tokenList;
            try {
                tokenList = result.getTokenList();
            }catch (NullPointerException e){
                System.out.println(str);
                continue;
            }
            Set<String> NNList = new HashSet<>();
            int sentiScore = 0;
            for (Token token : tokenList) {
                if(sentiWord_info.containsKey(token.getMorph()))
                    sentiScore += Integer.parseInt(sentiWord_info.get(token.getMorph()));
                if(token.getPos().contains("NNP"))
                    NNList.add(token.getMorph());
            }
            for(String st : NNList){
                if(resultMap.containsKey(st))
                    resultMap.put(st, resultMap.get(st) + sentiScore);
                else
                    resultMap.put(st, sentiScore);
            }
        }

        return resultMap;
    }

    public ArrayList<String> getAllInfoOfChannel(String channelId){

        List<ResponseEntity<VideoApiResult>> videoApiResults = getVideoInfoByChannelId(channelId);
        ArrayList<String> videoInfos = new ArrayList<>();
        for(ResponseEntity<VideoApiResult> videoApiResultResponse : videoApiResults) {
            VideoApiResult videoApiResult = videoApiResultResponse.getBody();
            com.server.popfilterbubbleserver.service.api_response.video.Items[] videoItems = videoApiResult.getItems();
            for (int i = 0; i < videoItems.length; i++) {
                VideoInfoApiResult videoInfoApiResult = getVideoDetailInfoByVideoId(videoItems[i].getId().getVideoId()).getBody();
                ResponseEntity<VideoCommentApiResult> videoCommentApiResult1 = getCommentInfoByVideoId(videoItems[i].getId().getVideoId());
                if (videoCommentApiResult1 == null) continue;
                VideoCommentApiResult videoCommentApiResult = videoCommentApiResult1.getBody();
                if (videoCommentApiResult.getItems().length != 0) {
                    videoInfos.add(videoInfoApiResult.getItems()[0].getSnippet().getTitle());
                    videoInfos.add(videoInfoApiResult.getItems()[0].getSnippet().getDescription());
                    videoInfos.add(videoCommentApiResult.getItems()[0].getSnippet().getTopLevelComment().getSnippet().getTextOriginal());
                }
                com.server.popfilterbubbleserver.service.api_response.video_comment.Items[] commentItems = videoCommentApiResult.getItems();
                for (int j = 1; j < commentItems.length; j++)
                    videoInfos.add(commentItems[j].getSnippet().getTopLevelComment().getSnippet().getTextOriginal());
            }
        }
        return videoInfos;
    }

    public void saveYoutubeChannelInfo(String channelId, ChannelApiResult channelApiResult) {
        if(youtubeRepository.existsById(channelId))
            return;
        if (channelApiResult != null && channelApiResult.getItems() != null) {
            Items item = channelApiResult.getItems()[0];
            Snippet snippet = item.getSnippet();
            Statistics statistics = item.getStatistics();
            TopicDetails topicDetails = item.getTopicDetails();
            YoutubeChannelEntity entity = new YoutubeChannelEntity();

            // topicId 분류 및 저장
            if (isPolitic(topicDetails)) {
                if (checkVideoCount(statistics)) {
                    Integer politicScore = getPoliticScore(channelId);
                    if(politicScore == CONSERVATIVE)
                        entity.saveChannelInfo(snippet, statistics, channelId, true, CONSERVATIVE);
                    else if(politicScore == PROGRESSIVE)
                        entity.saveChannelInfo(snippet, statistics, channelId, true, PROGRESSIVE);
                    else
                        entity.saveChannelInfo(snippet, statistics, channelId, true, UNCLASSIFIED);
                } else entity.saveChannelInfo(snippet, statistics, channelId, true, UNCLASSIFIED);
            } else entity.saveChannelInfo(snippet, statistics, channelId, false, ETC);
            youtubeRepository.save(entity);
        }
    }

    // 정치 카테고리 판단
    public Boolean isPolitic(TopicDetails topicDetails) {
        if (topicDetails != null && topicDetails.getTopicCategories() != null) {
            for (String category : topicDetails.getTopicCategories()) {
                if (category.contains("Politics"))
                    return true;
            }
        }
        return false;
    }

    // 비디오 개수 판단
    public Boolean checkVideoCount(Statistics statistics) {
        return statistics.getVideoCount() >= 100;
    }

    public List<VideoListDTO> getVideoListDto(String[] channelIds) throws IOException {
        int conservativeCount = 0;
        int progressiveCount = 0;
        String id = "";

        for(String channelId : channelIds) {
            if(channelId.contains("@")) id = convertCustomIdToChannelId(channelId);
            if(id.equals("")) continue;
            ChannelApiResult channelApiResult = getChannelInfoByChannelId(id).getBody();
            saveYoutubeChannelInfo(id, channelApiResult);
            YoutubeChannelEntity youtubeChannelEntity = youtubeRepository.findById(id).orElse(null);
            if(youtubeChannelEntity != null) {
                if(youtubeChannelEntity.getTopicId() == CONSERVATIVE)
                    conservativeCount++;
                else if(youtubeChannelEntity.getTopicId() == PROGRESSIVE)
                    progressiveCount++;
            }
            else throw new NoSuchElementException("YoutubeChannelEntity not found. \tchannelId: " + channelId);
        }
        if(conservativeCount > progressiveCount)
            return getVideoList(conservativeCount - progressiveCount, PROGRESSIVE);
        else if(progressiveCount > conservativeCount)
            return getVideoList(progressiveCount - conservativeCount, CONSERVATIVE);
        return new ArrayList<>();
    }

    private List<VideoListDTO> getVideoList(int diff, int topicId){
        if(diff == 0) return new ArrayList<>();
        if(topicId == CONSERVATIVE) return conservativeVideoList.subList(0, diff);
        else return progressiveVideoList.subList(0, diff);
    }

    private List<VideoListDTO> getVideoListDtoByTopicId(int diff, int topicId) {
        List<String> channelId = youtubeRepository.findTop3CustomIdByTopicIdOrderBySubscriberCountDesc(topicId);
        List<VideoListDTO> videoList = new ArrayList<>();
        System.out.println(diff + " " + topicId + " " + channelId);
        List<com.server.popfilterbubbleserver.service.api_response.video_info.Items> allVideos = new ArrayList<>();

        for (String id : channelId) {
            // 각 채널의 영상 조회
            List<ResponseEntity<VideoApiResult>> videoApiResults = getVideoInfoByChannelId(id);

            for (ResponseEntity<VideoApiResult> videoApiResultResponse : videoApiResults) {
                VideoApiResult videoApiResult = videoApiResultResponse.getBody();
                for(com.server.popfilterbubbleserver.service.api_response.video.Items item : videoApiResult.getItems()) {
                    ResponseEntity<VideoInfoApiResult> videoInfoApiResultResponse = getVideoDetailInfoByVideoId(item.getId().getVideoId());
                    if (videoInfoApiResultResponse != null && videoInfoApiResultResponse.getBody() != null
                            && videoInfoApiResultResponse.getBody().getItems() != null
                            && videoInfoApiResultResponse.getBody().getItems().length > 0) {
                        allVideos.add(videoInfoApiResultResponse.getBody().getItems()[0]);
                    }
                }
            }
        }

        // allVideos를 publishedAt 최신순으로 정렬
        allVideos.sort((o1, o2) -> {
            String publishedAt1 = o1.getSnippet().getPublishedAt();
            String publishedAt2 = o2.getSnippet().getPublishedAt();
            return publishedAt2.compareTo(publishedAt1);
        });

        // diff 수만큼 영상을 VideoListDTO에 추가
        for (int i = 0; i < diff; i++) {
            if (i >= allVideos.size()) break;
            com.server.popfilterbubbleserver.service.api_response.video_info.Items videoItem = allVideos.get(i);

            // VideoListDTO에 정보 추가
            String videoId = videoItem.getId();
            String title = videoItem.getSnippet().getTitle();
            String description = videoItem.getSnippet().getDescription();
            String thumbnailUrl = videoItem.getSnippet().getThumbnails().getHigh().getUrl();
            String publishedAt = videoItem.getSnippet().getPublishedAt();
            String channelTitle = videoItem.getSnippet().getChannelTitle();
            String id = videoItem.getSnippet().getChannelId();
            BigInteger viewCount = videoItem.getStatistics().getViewCount();

            VideoListDTO videoDto = VideoListDTO.builder()
                    .videoId(videoId)
                    .title(title)
                    .description(description)
                    .thumbnailUrl(thumbnailUrl)
                    .publishedAt(publishedAt)
                    .channelId(id)
                    .channelTitle(channelTitle)
                    .viewCount(viewCount)
                    .url("https://www.youtube.com/watch?v=" + videoId)
                    .build();

            videoList.add(videoDto);
        }
        return videoList;
    }
}