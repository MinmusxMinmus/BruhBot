package events;

public class TimerEvent extends Event {

    private final String userID;
    private final String guildID;
    private final String messageID;

    public TimerEvent(long delay, long id, String userID, String guildID, String messageID) {
        super(delay, id);
        this.userID = userID;
        this.guildID = guildID;
        this.messageID = messageID;
    }

    public String getUserID() {
        return userID;
    }

    public String getGuildID() {
        return guildID;
    }

    public String getMessageID() {
        return messageID;
    }
}
