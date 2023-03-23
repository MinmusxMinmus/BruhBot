import events.Event;
import events.handler.EventHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import processers.GuildListener;
import processers.persistence.DataManagementProcessor;
import util.Buffer;
import util.Pair;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainC {

    public static final Buffer<Event> callBuffer = new Buffer<>();
    private static final Buffer<Event> eventExecutionBuffer = new Buffer<>();
    private static final String CMD_HEADER = "(MAIN) ";

    public static void main(String[] args) {


        // Timed events manager
        ScheduledThreadPoolExecutor timedEventsExecutor = new ScheduledThreadPoolExecutor(100);


        // Data management processor
        DataManagementProcessor dmp;
        try {
            dmp = new DataManagementProcessor("BruhBotDB");
        } catch (IOException e) {
            System.err.println("UNABLE TO INITIALIZE STORAGE MANAGER");
            e.printStackTrace();
            return;
        }
        dmp.initialize();

        // Event listener(s)
        GuildListener listener1 = new GuildListener(dmp, callBuffer);

        try {
            // Initialization
            // For now I will be using all the intents, to avoid any potential issues
            // TODO research about intents to optimize the bot
            JDA jda = JDABuilder.createDefault(args[0], EnumSet.allOf(GatewayIntent.class))
                    .addEventListeners(listener1)
                    /*
                     * Status suggestions:
                     *
                     * - Listening to the screams of the damned
                     * - Watching you suffer
                     * - Playing one against the other
                     * - Playing into the other party's hands
                     * - Playing for keeps
                     * - Playing the victim card
                     */
                    .setActivity(Activity.playing("with your feelings"))
                    .build()
                    .awaitReady();

            // Event handler thread
            Thread eventProcessor = new Thread(new EventHandler(eventExecutionBuffer, jda));
            eventProcessor.start();

            // Listening station
            while (true) {
                Pair<Integer, Event> command = callBuffer.getData();
                switch (command.first()) {
                    case 1: { // Timer event(remindme). Data: the TimerEvent
                        timedEventsExecutor.schedule(() -> eventExecutionBuffer.queue(command.second()), command.second().getDelay(), TimeUnit.SECONDS);
                        System.out.println(CMD_HEADER + "Timer event scheduled. ID: ");
                        break;
                    }
                }
            }
        }
        catch (LoginException l) {
            System.out.println("Error logging in. Perhaps the bot token changed? Contact the bot owner.");
        } catch (InterruptedException e) {
            System.out.println("Connection interrupted. Your ethernet probably crashed or something. Trace below:");
            e.printStackTrace();
        }
    }
}
