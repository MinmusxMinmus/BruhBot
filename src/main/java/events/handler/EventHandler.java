package events.handler;

import events.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import util.Buffer;

public class EventHandler implements Runnable{

    private static final String CMD_HEADER = "(EVENTS) ";

    private final Buffer<Event> eventInput;
    private final JDA jda;
    protected boolean alive = true;

    public EventHandler(Buffer<Event> buffer, JDA jda) {
        this.eventInput = buffer;
        this.jda = jda;
    }


    private void onEvent(Event e) {
        System.out.println(CMD_HEADER + "Event received.");
    }

    private void onTimerEvent(TimerEvent e) {
        if (e instanceof ReminderTimerEvent) {
            onReminderTimerEvent((ReminderTimerEvent) e);
            return;
        }
        System.out.println(CMD_HEADER + "Timer event received.");
    }
    private void onReminderTimerEvent(ReminderTimerEvent e) {
        System.out.println(CMD_HEADER + "Reminder timer event received.");

        User user = jda.getUserById(e.getUserID());
        if (user != null) user.openPrivateChannel().queue((c -> c.sendMessage("Reminder: " + e.getMessage()).queue()));
        else System.err.println("WARNING: user ID \"" + e.getUserID() + "\" not found! Unable to send reminder.");
    }

    private void onStatusEvent(StatusEvent e) {
        System.out.println(CMD_HEADER + "Status event received.");
    }

    @Override
    public void run() {
        Event e;
        while (alive) {
            e = eventInput.getData().second();

            if (e instanceof TimerEvent) onTimerEvent((TimerEvent) e);
            else if (e instanceof StatusEvent) onStatusEvent((StatusEvent) e);
            else onEvent(e);
        }

    }
}
