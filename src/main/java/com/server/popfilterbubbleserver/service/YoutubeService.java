package com.server.popfilterbubbleserver.service;

import com.server.popfilterbubbleserver.controller.PoliticsDTO;
import com.server.popfilterbubbleserver.controller.VideoListDTO;
import com.server.popfilterbubbleserver.module.YoutubeChannelEntity;
import com.server.popfilterbubbleserver.repository.YoutubeRepository;
import com.server.popfilterbubbleserver.service.api_response.channel.ChannelApiResult;
import com.server.popfilterbubbleserver.service.api_response.channel.Items;
import com.server.popfilterbubbleserver.service.api_response.channel.Snippet;
import com.server.popfilterbubbleserver.service.api_response.channel.Statistics;
import com.server.popfilterbubbleserver.service.api_response.channel.TopicDetails;
import com.server.popfilterbubbleserver.service.api_response.video.VideoApiResult;
import com.server.popfilterbubbleserver.service.api_response.video_comment.VideoCommentApiResult;
import com.server.popfilterbubbleserver.service.api_response.video_info.VideoInfoApiResult;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class YoutubeService implements ApplicationRunner {

    private final YoutubeRepository youtubeRepository;

    private final int CONSERVATIVE = 0;
    private final int PROGRESSIVE = 1;
    private final int UNCLASSIFIED = 2;
    private final int ETC = 3;

    private int conservativeCount = 0;
    private int progressiveCount = 0;

    private static List<VideoListDTO> conservativeVideoList = new ArrayList<>();
    private static List<VideoListDTO> progressiveVideoList = new ArrayList<>();

    private final SentiWord_infoDTO sentiWordInfoDTO;
    private final PoliticResultDTO politicResultDTO;

    @Value("${youtube_api_key_1}")
    private String youtubeApiKey1;

    @Value("${youtube_api_key_2}")
    private String youtubeApiKey2;

    @Value("${youtube_api_key_3}")
    private String youtubeApiKey3;

    @Value("${youtube_api_key_4}")
    private String youtubeApiKey4;

    @Value("${youtube_api_key_5}")
    private String youtubeApiKey5;

    private String youtube_api_key;
    private int count = 0;

    public HttpEntity<String> setHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        getApiKey();
        setVideoList();
    }

    // round-robin 방식으로 api key 가져오기
    public void getApiKey() {
        switch (count % 5 + 1) {
            case 1 -> youtube_api_key = youtubeApiKey1;
            case 2 -> youtube_api_key = youtubeApiKey2;
            case 3 -> youtube_api_key = youtubeApiKey3;
            case 4 -> youtube_api_key = youtubeApiKey4;
            case 5 -> youtube_api_key = youtubeApiKey5;
        }
        count++;
        //System.out.println("key : " + youtube_api_key);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void setVideoList(){
        System.out.println("setVideoList Start");
        conservativeVideoList = getVideoListDtoByTopicId(CONSERVATIVE);
        progressiveVideoList = getVideoListDtoByTopicId(PROGRESSIVE);
        System.out.println("setVideoList End");
    }

    private List<VideoListDTO> getVideoListDtoByTopicId(int topicId) {
        List<VideoListDTO> videoList = new ArrayList<>();
        List<com.server.popfilterbubbleserver.service.api_response.video_info.Items> allVideos = new ArrayList<>();

        List<String> channelId = youtubeRepository.findTop3IdByTopicIdOrderBySubscriberCountDesc(topicId);
        System.out.println("topicId: " + topicId + " TOP3 channelId: " + channelId);

        for(String id : channelId) {
            // channelImg를 나중에 추가한 이슈로 인해 null값인 경우에만 채널 정보를 다시 저장
            if(youtubeRepository.isChannelImgNull(id) == 1) {
                ChannelApiResult channelApiResult = getChannelInfoByChannelId(id).getBody();
                saveYoutubeChannelInfo(id, channelApiResult);
            }
            // 상위 3개 채널의 영상 정보 조회
            List<ResponseEntity<VideoApiResult>> videoApiResults = getVideoInfoByChannelId(id); // 50개 조회

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

        // 최대 100개의 영상을 VideoListDTO에 추가
        for (int i = 0; i < 100; i++) {
            if (i >= allVideos.size()) break;
            com.server.popfilterbubbleserver.service.api_response.video_info.Items videoItem = allVideos.get(i);

            // VideoListDTO에 정보 추가
            String videoId = videoItem.getId();
            String title = videoItem.getSnippet().getTitle();
            String description = videoItem.getSnippet().getDescription();
            String id = videoItem.getSnippet().getChannelId();
            String channelTitle = videoItem.getSnippet().getChannelTitle();
            YoutubeChannelEntity youtubeChannelEntity = youtubeRepository.findById(id).orElse(null);
            assert youtubeChannelEntity != null;
            String channelImg = youtubeChannelEntity.getChannelImg();
            String thumbnailUrl = videoItem.getSnippet().getThumbnails().getHigh().getUrl();
            String publishedAt = videoItem.getSnippet().getPublishedAt();
            BigInteger viewCount = videoItem.getStatistics().getViewCount();

            VideoListDTO videoDto = VideoListDTO.builder()
                    .videoId(videoId)
                    .title(title)
                    .description(description)
                    .channelId(id)
                    .channelTitle(channelTitle)
                    .channelImg(channelImg)
                    .thumbnailUrl(thumbnailUrl)
                    .publishedAt(publishedAt)
                    .viewCount(viewCount)
                    .url("https://www.youtube.com/watch?v=" + videoId)
                    .build();

            videoList.add(videoDto);
        }
        return videoList;
    }

    // 정치 카테고리 분류, 각 카테고리 별 영상 개수 조회
    public PoliticsDTO getPoliticsDto(String[] customIds) throws IOException {
        int conservativeCount = 0;
        int progressiveCount = 0;
        int unclassifiedCount = 0;
        int etcCount = 0;

        for(String cid : customIds) {
            String id = getChannelId(cid);
            // 에러 발생 시 기타로 분류
            if (id.isEmpty()) {
                System.out.println("Error: CHANNEL_ID_NOT_FOUND - customId: " + cid);
                etcCount++;
                continue;
            }
            // 채널 아이디로 채널 정보 가져와 카테고리 분류 후 DB에 저장
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
            else throw new NoSuchElementException("YoutubeChannelEntity not found. \tcustomId: " + cid);
        }
        this.conservativeCount = conservativeCount;
        this.progressiveCount = progressiveCount;

        return PoliticsDTO.builder()
                .conservative(conservativeCount)
                .progressive(progressiveCount)
                .unclassified(unclassifiedCount)
                .etc(etcCount)
                .build();
    }

    // 추천 영상 리스트 조회
    public List<VideoListDTO> getRecommendedVideoList() throws IOException {
        if(conservativeCount > progressiveCount)
            return getVideoList(PROGRESSIVE, conservativeCount - progressiveCount);
        else if(progressiveCount > conservativeCount)
            return getVideoList(CONSERVATIVE, progressiveCount - conservativeCount);
        return new ArrayList<>();
    }

    private List<VideoListDTO> getVideoList(int topicId, int diff){
        if(topicId == CONSERVATIVE) return conservativeVideoList.subList(0, diff);
        else return progressiveVideoList.subList(0, diff);
    }

    private String getChannelId(String channelId) throws IOException {
        if (channelId.contains("@"))
            return convertCustomIdToChannelId(channelId);
        return "";
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

        getApiKey();

        for (int i = 0; i < 10; i++) {
            try {
                String newUrl = url + "&key=" + youtube_api_key;
                response = restTemplate.exchange(newUrl, HttpMethod.GET, setHeaders(), classType.getClass());
                return response;
            } catch (Exception e) {
                getApiKey();
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

        return (ResponseEntity<ChannelApiResult>) getResponse(url, new ChannelApiResult());
    }

    public List<ResponseEntity<VideoApiResult>> getVideoInfoByChannelId(String channelID) {
        String url = "https://youtube.googleapis.com/youtube/v3/search";
        url += "?part=snippet";
        url += "&channelId=" + channelID;
        url += "&maxResults=50";
        url += "&order=date";

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

        return (ResponseEntity<VideoInfoApiResult>) getResponse(url, new VideoInfoApiResult());
    }

    public ResponseEntity<VideoCommentApiResult> getCommentInfoByVideoId(String videoId) {
        String url = "https://youtube.googleapis.com/youtube/v3/commentThreads";
        url += "?part=snippet";
        url += "&videoId=" + videoId;
        url += "&order=relevance";
        url += "&maxResults=100";

        return (ResponseEntity<VideoCommentApiResult>) getResponse(url, new VideoCommentApiResult());
    }

    public String convertCustomIdToChannelId(String customId) throws IOException {
        List<YoutubeChannelEntity> youtubeChannelEntities = youtubeRepository.findAllByCustomId(customId);
        if(youtubeChannelEntities.size() > 0)
            return youtubeChannelEntities.get(0).getChannelId();

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

    @Synchronized
    public void saveYoutubeChannelInfo(String channelId, ChannelApiResult channelApiResult) {
        if (youtubeRepository.existsById(channelId) && youtubeRepository.isChannelImgNull(channelId) != 1) {
            return;
        }
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
}