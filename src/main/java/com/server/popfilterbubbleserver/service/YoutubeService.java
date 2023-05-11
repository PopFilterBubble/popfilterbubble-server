package com.server.popfilterbubbleserver.service;

import com.server.popfilterbubbleserver.util.ErrorMessages;
import java.io.IOException;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class YoutubeService {

    @Autowired
    private YoutubeRepository youtubeRepository;

    private final int CONSERVATIVE = 0;
    private final int PROGRESSIVE = 1;
    private final int UNCLASSIFIED = 2;
    private final int ETC = 3;

    private final SentiWord_infoDTO sentiWordInfoDTO;

    @Autowired
    private YoutubeRepository youtubeRepository;

    @Value("${youtube_api_key}")
    private String youtube_api_key;

    public HttpEntity<String> setHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }
    public Map<String, Integer> test(String channelId) throws IOException {
        if(channelId.contains("@"))
            channelId = convertCustomIdToChannelId(channelId);
        ArrayList<String> ast = getAllInfoOfChannel(channelId);
        Map<String, Integer> m = getPolicitalScore(ast);
        return m;
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
                if(token.getPos().contains("NN"))
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
        }
        return videoInfos;
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