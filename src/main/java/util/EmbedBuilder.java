package util;

import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;

public class EmbedBuilder {

    private String url;
    private String title;
    private String description;
    private EmbedType embedType;
    private OffsetDateTime time;
    private int color;
    private MessageEmbed.Thumbnail thumbnail;
    private MessageEmbed.Provider provider;
    private MessageEmbed.AuthorInfo authorInfo;
    private MessageEmbed.VideoInfo videoInfo;
    private MessageEmbed.Footer footer;
    private MessageEmbed.ImageInfo image;
    private final List<MessageEmbed.Field> fields = new LinkedList<>();

    public MessageEmbed toEmbed() {
        return new MessageEmbed(url, title, description, embedType, time, color, thumbnail, provider, authorInfo, videoInfo, footer, image, fields);
    }

    public EmbedBuilder setUrl(String url) {
        this.url = url;
        return this;
    }

    public EmbedBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public EmbedBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public EmbedBuilder setEmbedType(EmbedType embedType) {
        this.embedType = embedType;
        return this;
    }

    public EmbedBuilder setTime(OffsetDateTime time) {
        this.time = time;
        return this;
    }

    public EmbedBuilder setColor(int color) {
        this.color = color;
        return this;
    }

    public EmbedBuilder setThumbnail(MessageEmbed.Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
        return this;
    }

    public EmbedBuilder setProvider(MessageEmbed.Provider provider) {
        this.provider = provider;
        return this;
    }

    public EmbedBuilder setAuthorInfo(MessageEmbed.AuthorInfo authorInfo) {
        this.authorInfo = authorInfo;
        return this;
    }

    public EmbedBuilder setVideoInfo(MessageEmbed.VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
        return this;
    }

    public EmbedBuilder setFooter(MessageEmbed.Footer footer) {
        this.footer = footer;
        return this;
    }

    public EmbedBuilder setImage(MessageEmbed.ImageInfo image) {
        this.image = image;
        return this;
    }

    public EmbedBuilder addField(MessageEmbed.Field field) {
        fields.add(field);
        return this;
    }
}
