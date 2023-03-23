package processers.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import processers.persistence.DataManagementProcessor;
import util.Pair;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CommandProcessor {

    protected static final String BAD_ARGUMENTS_ERROR = "Bro, bad arguments. ";
    protected final String CMD_HEADER;
    protected final String[] mockQuotes = {
            "Truly an inspiring insight",
            "This says a lot about society",
            "I'm pretty sure Albert Einstein said that",
            "And you still don't want humanity gone?",
            "Maybe it's time for a database purge...",
            "You know, I'm starting to see a pattern here",
            "Hey, I'm not the one posting this shit",
            "What is my purpose? ...to save shitposts? Oh my God...",
            "If you know how to embed videos, PM the bot owner instantly"
    };

    protected final String keyChars;
    protected final Guild guild;

    protected MessageChannel commandChannel;
    protected String[] args;
    protected int argCount;
    protected boolean success;
    protected String reason;

    protected String log;

    private final Map<String, Long> cooldown;

    protected final DataManagementProcessor dmp;

    public CommandProcessor(Guild guild, String keyChars, processers.persistence.DataManagementProcessor dmp, String header) {
        this.guild = guild;
        this.keyChars = keyChars;
        this.dmp = dmp;
        this.CMD_HEADER = header;
        this.cooldown = new HashMap<>();
        this.args = new String[10];
    }

    public String processCommand(Message message) {
        // Variable setting
        this.commandChannel = message.getChannel();
        argCount = getArgs(message.getContentRaw().trim());
        this.log = "";

        // String logging
        StringBuilder commandArguments = new StringBuilder();
        for (int i = 1; i != args.length; i++) {
            if (args[i] == null) break;
            commandArguments.append("\"").append(args[i].replace("\n", "\\n")).append("\"");
        }
        if (commandArguments.length() == 0) commandArguments.append("None");
        log("Command detected");
        log("Sender: @" + message.getAuthor().getName() + " (ID:" + message.getAuthor().getId() + ")");
        log("Channel: #" + message.getChannel().getName() + " (ID:" + message.getAuthor().getId() + ")");
        log("Command: " + args[0]);
        log("Arguments: " + commandArguments);
        return log;
    }

    public boolean lastCommandFailure() {return !success;}

    protected boolean hasBadArgs(int expectedAmount, String errorMessage) {
        if (argCount != expectedAmount) {
            commandChannel.sendMessage(BAD_ARGUMENTS_ERROR + " " + errorMessage).queue();
            success = false;
            reason = "Bad arguments (expected " + expectedAmount + ", got " + argCount + ").";
            return true;
        }
        return false;
    }

    // TODO remake for optional arguments
    private int getArgs(String message) {
        int argCount = 0;
        boolean insideQuotations = false;
        boolean quotationArgumentEnd = false;
        boolean argBegin = true;
        StringBuilder currentArg = new StringBuilder();

        char[] msg = message.toCharArray();
        for (char c : msg) {

            // The first two checks make sure that the transition between arguments is smooth.
            // Spacer check (ignores the spaces between quotation arguments)
            // TODO remake so that multiple spaces don't completely destroy the bot
            if (quotationArgumentEnd) {
                quotationArgumentEnd = false;
                argBegin = true;
                continue;
            }

            // First character check (decides if it's reading a quotation or a spaced argument)
            if (argBegin) {
                argBegin = false;
                currentArg = new StringBuilder();

                insideQuotations = c == '"';
                // Appends the current character if it isn't a quotation
                if (!insideQuotations) currentArg.append(c);
                continue;
            }

            // The last two checks detect the end of an argument, and activate the previous checks.
            // End of a quotation argument
            if (insideQuotations && c == '"') {
                args[argCount] = currentArg.toString(); // Stores the obtained argument
                argCount++;
                quotationArgumentEnd = true;
                continue;
            }

            // End of a spaced argument
            if (!insideQuotations && c == ' ') {
                args[argCount] = currentArg.toString(); // Stores the obtained argument
                argCount++;
                argBegin = true;
                continue;
            }

            // If none of the checks have been triggered, it means the character is part of the current argument.
            currentArg.append(c);
        }
        // Last argument (only if it's a spaced argument)
        if (!insideQuotations) {
            args[argCount] = currentArg.toString();
            argCount++;
        }

        // Removing the key characters from the first argument (turns it into command name)
        args[0] = args[0].replace(keyChars, "");

        // Returns the actual number of arguments (command name doesn't count as an argument)
        return argCount - 1;
    }

    /**
     * @return A pair containing the URL, and true if it is an image (or can be embedded). False if it's a video (or can't be embedded).
     */
    protected Pair<String, Boolean> getLink(String message) {
        // First regex: YouTube links
        Matcher m = Pattern.compile("((http(s)?://)?((w){3}.)?youtu(be|.be)?(\\.com)?/[^ ]+)").matcher(message);
        if (m.matches()) {
            System.out.println(CMD_HEADER + "Found a YouTube link.");
            return new Pair<>(m.group(1), false);
        }

        // Second regex: tenor GIFs
        Matcher m2 = Pattern.compile("(https?://tenor.com/view/([-a-zA-Z0-9()@:%_+.~#?&/=]+))").matcher(message);
        if (m2.matches()) {
            System.out.println(CMD_HEADER + "Found a Tenor link.");
            // TODO Tenor GIFs can't be decoded by ImageIO (the constructor returns null). Investigate
            return new Pair<>(m2.group(1), false); // Change to true when a solution is found
        }

        // Third regex: normal links
        String url;
        Matcher m3 = Pattern.compile("(https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*))").matcher(message);
        if (m3.matches()) {
            System.out.println(CMD_HEADER + "Found a regular link.");
            url =  m3.group(1);
            String extension = url.split("(\\.)|((\\?(a-zA-Z)+=(.*))+$)")[url.split("(\\.)|((\\?(a-zA-Z)+=(.*))+$)").length - 1];
            System.out.println(CMD_HEADER + "Extension = " + extension);
            switch (extension) {
                case "jpg":
                case "gif":
                case "png":
                case "jpeg":
                case "webp":
                case "tiff":
                case "svg":
                case "apng":
                    return new Pair<>(url, true);
                case "webm":
                case "flv":
                case "vob":
                case "avi":
                case "mov":
                case "wmv":
                case "amv":
                case "mp4":
                case "mpg":
                case "mpeg":
                case "gifv":
                    return new Pair<>(url, false);
            }
        }


        return null;
    }

    protected boolean hasCooldown(User user, long deltaMilliseconds) {
        Long cd = cooldown.get(user.getId());
        if (cd == null || System.currentTimeMillis() - cd >= deltaMilliseconds) return false;
        commandChannel.sendMessage("Too fast, wait a bit").queue();
        success = false;
        reason = "User has a cooldown.";
        return true;
    }

    protected void addCooldown(User user, long time) {
        cooldown.put(user.getId(), time);
    }

    protected void log(String s) {
        this.log += "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "]" + CMD_HEADER + s + "\n";
        System.out.println(CMD_HEADER + s);
    }
}
