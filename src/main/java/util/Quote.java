package util;

import java.time.OffsetDateTime;
import java.util.Date;

public class Quote {

    private final String channel;
    private final String attachment;
    private final String nickname;
    private final OffsetDateTime time;
    private final String id;

    public Quote(String channel, String attachment, String nickname, OffsetDateTime time, String id) {
        this.channel = channel;
        this.attachment = attachment;
        this.nickname = nickname;
        this.time = time;
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAttachment() {
        return attachment;
    }

    public String getChannel() {
        return channel;
    }

    public OffsetDateTime getTime() {
        return time;
    }

    public String getId() {
        return id;
    }
}
