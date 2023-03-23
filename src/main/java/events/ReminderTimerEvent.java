package events;

public class ReminderTimerEvent extends TimerEvent{

    private final String message;

    public ReminderTimerEvent(long delay, long id, String userID, String guildID, String messageID, String message) {
        super(delay, id, userID, guildID, messageID);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
