package events;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

public class StatusEvent extends Event{

    private final JDA jda;
    private final Activity newActivity;

    public StatusEvent(long delay, long id, JDA jda, Activity newActivity) {
        super(delay, id);
        this.jda = jda;
        this.newActivity = newActivity;
    }

    public Activity getActivity() {
        return newActivity;
    }

    public JDA getJDA() {
        return jda;
    }
}
