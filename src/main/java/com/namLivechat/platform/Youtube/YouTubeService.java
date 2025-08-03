package com.namLivechat.platform.Youtube;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveChatMessageListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeService {

    private final YouTube youtube;
    private final String apiKey;
    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    // ## เพิ่มเติม: เพิ่ม videoTitle เข้าไปในคลาสเก็บข้อมูล ##
    public static class LiveStreamInfo {
        private final String liveChatId;
        private final String channelTitle;
        private final String videoTitle;

        public LiveStreamInfo(String liveChatId, String channelTitle, String videoTitle) {
            this.liveChatId = liveChatId;
            this.channelTitle = channelTitle;
            this.videoTitle = videoTitle;
        }

        public String getLiveChatId() { return liveChatId; }
        public String getChannelTitle() { return channelTitle; }
        public String getVideoTitle() { return videoTitle; }
    }

    public YouTubeService(String apiKey) throws GeneralSecurityException, IOException {
        this.apiKey = apiKey;
        this.youtube = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                null)
                .setApplicationName("NamLivechat-Minecraft-Plugin")
                .build();
    }

    public String getVideoIdFromUrl(String url) {
        if (url == null) return null;
        Matcher matcher = YOUTUBE_VIDEO_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    public LiveStreamInfo getLiveChatId(String videoId) throws IOException {
        YouTube.Videos.List request = youtube.videos().list("liveStreamingDetails,snippet");
        VideoListResponse response = request.setId(videoId).setKey(apiKey).execute();

        if (response.getItems().isEmpty()) {
            return null;
        }

        Video video = response.getItems().get(0);
        if (video.getLiveStreamingDetails() != null && video.getLiveStreamingDetails().getActiveLiveChatId() != null) {
            String liveChatId = video.getLiveStreamingDetails().getActiveLiveChatId();
            String channelTitle = video.getSnippet().getChannelTitle();
            // ## เพิ่มเติม: ดึงชื่อไลฟ์ (Video Title) จาก snippet ##
            String videoTitle = video.getSnippet().getTitle();
            return new LiveStreamInfo(liveChatId, channelTitle, videoTitle);
        }
        return null;
    }

    public LiveChatMessageListResponse getLiveChatMessages(String liveChatId, String pageToken) throws IOException {
        YouTube.LiveChatMessages.List chatRequest = youtube.liveChatMessages()
                .list(liveChatId, "snippet,authorDetails")
                .setKey(apiKey);

        if (pageToken != null) {
            chatRequest.setPageToken(pageToken);
        }
        return chatRequest.execute();
    }
}