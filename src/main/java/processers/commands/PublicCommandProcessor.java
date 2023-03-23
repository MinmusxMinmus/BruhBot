package processers.commands;

import events.ReminderTimerEvent;
import net.dv8tion.jda.api.entities.*;
import processers.GuildListener;
import processers.persistence.DataManagementProcessor;
import util.EmbedBuilder;
import util.Pair;
import util.Quote;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class PublicCommandProcessor extends CommandProcessor {

    private static final long FACTS_COOLDOWN = 5000;
    private static final long QUOTETHIS_COOLDOWN = 10000;

    /* TODO Commands to implement:
    command_coinflip
    command_choose
    command_8ball
     */
    private static final String COMMAND_QUOTE = "quotethis";
    private static final String COMMAND_FACTS = "facts";
    private static final String COMMAND_REMINDME = "remindme";
    private static final String COMMAND_HELP = "help";
    private static final String COMMAND_COIN_FLIP = "coinflip";
    private static final String COMMAND_CHOOSE = "choose";
    private static final String COMMAND_8BALL = "8ball";

    private final String[] help_commands = {
            keyChars + COMMAND_QUOTE,
            keyChars + COMMAND_FACTS,
            keyChars + COMMAND_REMINDME + " \"message\" <time>",
            keyChars + COMMAND_COIN_FLIP,
            keyChars + COMMAND_8BALL + " \"question\"",
            keyChars + COMMAND_CHOOSE + "\"option 1\" \"option 2\" ... (Up to 9 options)",
    };
    private final String[] help_descriptions = {
            "Quotes the last message sent.",
            "Shows a random quote from the database.",
            "Waits the specified time, then PMs you the message. The time is formatted according to the following:\n" +
                    "A number is written, followed without spacing by either 'd', 'h', 'm' or 's' (representing days, " +
                    "hours, minutes and seconds respectively). This forms a 'time unit'. Multiple time units can be " +
                    "used at the same time, and you can even use the same time unit multiple times.",
            "Flips a virtual coin, and gives you the result.",
            "Solves all of your doubts",
            "Chooses an item from the offered set of options.",
    };

    private static final String[] quoteMessages = {
            "Ohh no no no no no no",
            "Do you actually unironically find that funny?",
            "Based",
            "<:kek:672717503570640911>",
            "The deed is done",
            "You will regret that",
            "Already added",
            "I'll remove it from my database while you're not looking",
            "Ah, \\*\\*\\*\\*. I can't believe you've done this.",
            "kys on the spot"
    };

    public PublicCommandProcessor(String keyChars, Guild guild, DataManagementProcessor dmp) {
        super(guild, keyChars, dmp, "(PCP) ");
    }

    @Override
    public String processCommand(Message message) {
        super.processCommand(message);

        // Blacklist guard
        if (dmp.getBlacklistedUsers().contains(message.getAuthor().getId())) {
            log("Command failed. Reason: user is blacklisted.");
            commandChannel.sendMessage("You're black").queue();
            return log;
        }

        switch(args[0]) {
            case COMMAND_QUOTE: {

                // Cooldown check
                if (hasCooldown(message.getAuthor(), QUOTETHIS_COOLDOWN)) break;

                // Obtaining the quoted message
                MessageHistory history = commandChannel.getHistory();
                // This might not work well with spam? It'd be better to add a check "if (get(0) == message)"
                Message quote = history.retrievePast(2).complete().get(1);

                // Bot check
                if (quote.getAuthor().isBot()) {
                    commandChannel.sendMessage("No quoting bots").queue();
                    success = false;
                    reason = "User attempted to quote a bot.";
                    break;
                }

                // Guard against bad people
                if (quote.getContentRaw().matches("(.* )?@.+( .*)?") ||
                    quote.getContentRaw().matches("(.* )?<@(.+)>( .*)?")) {
                    commandChannel.sendMessage("Nice try").queue();
                    success = false;
                    reason = "User attempted to quote a ping.";
                    break;
                }
                String attachment = "";
                if (quote.getAttachments().size() != 0) {
                    attachment += quote.getAttachments().get(0).getUrl();
                }

                Quote quote1;
                try {
                    quote1 = new Quote(
                            commandChannel.getId(),
                            attachment,
                            Objects.requireNonNull(quote.getMember(), "Member not found!").getEffectiveName(),
                            quote.getTimeCreated(),
                            quote.getId());
                } catch (NullPointerException e) {
                    System.err.println(CMD_HEADER + "WARNING: Member not found. Unable to retrieve nickname information.");
                    quote1 = new Quote(
                            commandChannel.getId(),
                            attachment,
                            "Anonymous",
                            quote.getTimeCreated(),
                            quote.getId());
                }
                dmp.addQuote(quote1);
                addCooldown(message.getAuthor(), System.currentTimeMillis());
                int random = new Random(message.getIdLong()).nextInt(quoteMessages.length);
                commandChannel.sendMessage(quoteMessages[random]).queue();
                success = true;
                break;
            }
            case COMMAND_FACTS: {
                // Cooldown check
                if (!message.getAuthor().getId().equals("265904613687820288") && hasCooldown(message.getAuthor(), FACTS_COOLDOWN)) break;

                // Fact availability check
                if (dmp.getQuotes().size() == 0) {
                    commandChannel.sendMessage("No facts chief").queue();
                    success = false;
                    reason = "No facts available.";
                    break;
                }

                // Obtaining a random fact (deleting possible broken facts)
                Quote q;
                Message m;
                while(true) {
                    q = dmp.getRandomQuote(message.getIdLong());
                    try { // Attempt to obtain a message
                        m = Objects.requireNonNull(guild.getTextChannelById(q.getChannel()), "Channel not found!")
                                .retrieveMessageById(q.getId()).complete();
                        break;
                    } catch (Exception e) { // If anything goes wrong, deletes the quote and tries again
                        dmp.removeQuote(q.getId());
                    }
                }
                StringBuilder FailedEmbeds = new StringBuilder();
                EmbedBuilder builder = new EmbedBuilder()
                        .addField(new MessageEmbed.Field("ID", m.getId(), true))
                        .setEmbedType(EmbedType.RICH)
                        .setDescription(m.getContentRaw())
                        .setTime(OffsetDateTime.now())
                        .setFooter(new MessageEmbed.Footer("\"" + mockQuotes[new Random(m.getIdLong()).nextInt(mockQuotes.length)] + "\"", m.getJDA().getSelfUser().getAvatarUrl(), null))
                        .setAuthorInfo(new MessageEmbed.AuthorInfo("Sent by " + m.getAuthor().getName() + " on " + m.getTimeCreated().format(DateTimeFormatter.ofPattern("LLL dd, yyyy")), null, m.getAuthor().getAvatarUrl(), null));

                // Handling links in the message
                Pair<String, Boolean> pair = getLink(m.getContentRaw()); // If there's an unknown link, it just doesn't get detected.
                if (pair != null) {
                    String url = pair.first();
                    builder.setDescription(m.getContentRaw().replace(url, ""));
                    if (pair.second()) {
                        log("Image URL found.");
                        try {
                            BufferedImage bi = ImageIO.read(new URL(url));
                            builder.setEmbedType(EmbedType.IMAGE)
                                    .setImage(new MessageEmbed.ImageInfo(url, null, bi.getWidth(), bi.getHeight()));
                        } catch (IOException e) {
                            log("Unable to read image from URL.");
                            System.err.println(CMD_HEADER + "ERROR: Unable to read image from URL. Stopping attempts to embed. Trace below:");
                            e.printStackTrace();
                            FailedEmbeds.append(url).append(" ");
                        } catch (ArrayIndexOutOfBoundsException e) {
                            log("Strange GIF error occurred.");
                            System.err.println(CMD_HEADER + "Strange GIF error occurred. Stopping attempts to embed. You should probably tell the bot owner. Trace below:");
                            e.printStackTrace();
                            FailedEmbeds.append(url).append(" ");
                        } catch (Exception e) {
                            log("Unknown error.");
                            System.err.println(CMD_HEADER + "Unknown error. Stopping attempts to embed. You should probably tell the bot owner. Trace below:");
                            e.printStackTrace();
                            FailedEmbeds.append(url).append(" ");
                        }
                    } else {
                        log("Video URL found.");
                        FailedEmbeds.append(url);
                    }
                }

                // Handling attachments
                if (m.getAttachments().size() != 0) for (Message.Attachment a : m.getAttachments()) {
                    if (a.isImage()) {
                        log("Image attachment found.");
                        builder
                                .setEmbedType(EmbedType.IMAGE)
                                .setImage(new MessageEmbed.ImageInfo(a.getUrl(), a.getProxyUrl(), a.getWidth(), a.getHeight()));
                    } else if (a.isVideo()) {
                        log("Video attachment found.");
                        FailedEmbeds.append("\n").append(a.getUrl());
                    } else {
                        log("Unknown attachment found.");
                        builder.addField(new MessageEmbed.Field("Attachment", a.getUrl(), true));
                    }
                }

                // Video embed second message
                if (FailedEmbeds.length() != 0) {
                    commandChannel.sendMessage(FailedEmbeds.toString()).queue();
                }
                commandChannel.sendMessage(builder.toEmbed()).queue();

                addCooldown(message.getAuthor(), System.currentTimeMillis());
                success = true;
                break;
            }
            case COMMAND_REMINDME: {
                if (hasBadArgs(2, "Give me the message, and a time to remind you in format \"?d?h?m?s\".")) break;

                // Time formatting
                try {
                    long totalTime = getTotalTime(args[2]);

                    // Event creation
                    ReminderTimerEvent rte = new ReminderTimerEvent(
                            totalTime,
                            new Random(message.getIdLong()).nextInt(),
                            message.getAuthor().getId(),
                            guild.getId(),
                            message.getId(),
                            args[1]
                    );
                    log("Reminder saved, seconds until execution = " + rte.getDelay() + ".");
                    GuildListener.callBuffer.queue(1, rte);
                    success = true;
                    commandChannel.sendMessage("Saved").queue();
                } catch (NumberFormatException e) {
                    commandChannel.sendMessage("Calm down man, I'm not going to exist for that long").queue();
                    success = false;
                    reason = "Unable to parse time.";
                }

                break;
            }
            case COMMAND_COIN_FLIP: {
                if (hasBadArgs(0, "This command takes 0 arguments.")) break;

                if (new Random(message.getIdLong()).nextBoolean()) commandChannel.sendMessage("Looks like tails to me").queue();
                else commandChannel.sendMessage("That's going to be heads").queue();
                success = true;
                break;
            }
            case COMMAND_CHOOSE: {
                // TODO implement choose command
                commandChannel.sendMessage("Ping the bot owner, this command isn't implemented yet").queue();
                success = false;
                reason = "Command not implemented yet";
                break;
            }
            case COMMAND_8BALL: {
                // TODO replace argument check with (args > 1). Nobody cares about the argument anyway
                if (hasBadArgs(1, "Give me something to answer")) break;
                String prediction;
                switch (new Random(message.getIdLong()).nextInt(10)) {
                    case 0:
                        prediction = "Looks like cope to me";
                        break;
                    case 1:
                        prediction = "Probably yes";
                        break;
                    case 2:
                        prediction = "That's messed up, even for me";
                        break;
                    case 3:
                        prediction = "Only villains do that";
                        break;
                    case 4:
                        prediction = "Ask Cry about that";
                        break;
                    case 5:
                        prediction = "Probably not";
                        break;
                    case 6:
                        prediction = "Definitely";
                        break;
                    case 7:
                        prediction = "Absolutely not";
                        break;
                    case 8:
                        prediction = "I have no clue, to be fair.";
                        break;
                    case 9:
                        prediction = "You know what they say. There's a first time for everything";
                        break;
                    default:
                        prediction = "????????????";
                }
                commandChannel.sendMessage(prediction).queue();
                success = true;
                break;
            }
            case COMMAND_HELP: {
                // TODO change for 25+ fields (basically copy-paste the one in AdminCommandProcessor)
                EmbedBuilder builder = new EmbedBuilder();
                try {
                    BufferedImage bi = ImageIO.read(new URL("https://i.redd.it/6p3meftgsik41.jpg"));
                    builder.setThumbnail(new MessageEmbed.Thumbnail("https://i.redd.it/6p3meftgsik41.jpg", null, bi.getWidth(), bi.getHeight()));
                } catch (IOException ignored) { }
                builder.setEmbedType(EmbedType.RICH)
                        .setAuthorInfo(new MessageEmbed.AuthorInfo("This dude unironically doesn't know the commands", null, commandChannel.getJDA().getSelfUser().getAvatarUrl(), null))
                        .setTitle("User commands")
                        .setFooter(new MessageEmbed.Footer("BruhBot", commandChannel.getJDA().getSelfUser().getAvatarUrl(), null))
                        .setTime(OffsetDateTime.now());

                for (int i = 0; i != help_commands.length; i++)
                    builder.addField(new MessageEmbed.Field(help_commands[i], help_descriptions[i], false));
                message.getAuthor().openPrivateChannel().queue(c -> c.sendMessage(builder.toEmbed()).queue());
                commandChannel.sendMessage("Check DMs.").queue();
                success = true;
                break;
            }
            default: {
                commandChannel.sendMessage("What?").queue();
                success = false;
                reason = "Invalid command code.";
                break;
            }
        }
        if (success) log("User command successfully executed.");
        else log("User command failed. Reason: " + reason);
        Arrays.fill(args, null);
        return log;
    }

    private long getTotalTime(String arg) {
        long totalTime = 0;
        StringBuilder currentNumber = new StringBuilder();

        for (char c : arg.toCharArray()) {
            if (c >= 0x30 && c <= 0x39) currentNumber.append(c);
            else {
                if (currentNumber.length() == 0) {
                    System.err.println(CMD_HEADER + "ERROR: Consecutive letters found while parsing time");
                    return -1;
                }
                long delta = Long.parseLong(currentNumber.toString());
                switch (c) {
                    case 'd':
                        delta *= 24;
                    case 'h':
                        delta *= 60;
                    case 'm':
                        delta *= 60;
                    case 's':
                        totalTime += delta;
                        break;
                    default:
                        System.err.println(CMD_HEADER + "ERROR: Wrong letter found parsing time: must be d,h,m or s, found " + c);
                        return -1;
                }
                currentNumber = new StringBuilder();
            }
        }
        return totalTime;
    }

}
