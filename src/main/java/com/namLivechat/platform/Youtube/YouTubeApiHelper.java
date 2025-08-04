package com.namLivechat.platform.Youtube;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveChatMessageListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.namLivechat.NamLivechat;

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeApiHelper {

    private YouTube youtube;
    private final NamLivechat plugin;

    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    public YouTubeApiHelper(NamLivechat plugin) {
        this.plugin = plugin;
        initialize();
    }

    public void initialize() {
        String apiKey = plugin.getYoutubeConfig().getString("youtube-api-key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            this.youtube = null;
            return;
        }
        try {
            this.youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    null)
                    .setApplicationName("NamLivechat-Minecraft-Plugin")
                    .build();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize YouTube HTTP transport: " + e.getMessage());
            this.youtube = null;
        }
    }

    public boolean isAvailable() {
        return this.youtube != null;
    }

    public String getVideoIdFromUrl(String url) {
        if (url == null) return null;
        Matcher matcher = YOUTUBE_VIDEO_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    public YouTubeService.LiveStreamInfo getLiveChatId(String videoId) throws IOException {
        YouTube.Videos.List request = youtube.videos().list("liveStreamingDetails,snippet");
        VideoListResponse response = request.setId(videoId).setKey(plugin.getYoutubeConfig().getString("youtube-api-key")).execute();

        if (response.getItems().isEmpty()) return null;

        Video video = response.getItems().get(0);
        if (video.getLiveStreamingDetails() != null && video.getLiveStreamingDetails().getActiveLiveChatId() != null) {
            return new YouTubeService.LiveStreamInfo(
                    video.getLiveStreamingDetails().getActiveLiveChatId(),
                    video.getSnippet().getChannelTitle(),
                    video.getSnippet().getTitle()
            );
        }
        return null;
    }

    public LiveChatMessageListResponse getLiveChatMessages(String liveChatId, String pageToken) throws IOException {
        YouTube.LiveChatMessages.List chatRequest = youtube.liveChatMessages()
                .list(liveChatId, "snippet,authorDetails")
                .setKey(plugin.getYoutubeConfig().getString("youtube-api-key"));

        if (pageToken != null) chatRequest.setPageToken(pageToken);
        return chatRequest.execute();
    }
}