package processers;

import events.Event;
import interfaces.ClownActions;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import processers.administration.BlacklistProcessor;
import processers.commands.AdminCommandProcessor;
import processers.commands.PublicCommandProcessor;
import processers.persistence.DataManagementProcessor;
import util.Buffer;

import javax.annotation.Nonnull;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class GuildListener extends ListenerAdapter implements ClownActions {

    public static final String GUILD_ID = "672705813646671891";
    public static final String BLACKLISTED_ROLE_ID = "672816873208807444";

    public static Buffer<Event> callBuffer;
    public static final String adminKeyChars = "e!";                            // Key string to detect admin commands
    public static final String userKeyChars = ",";                              // Key string to detect user commands
    private AdminCommandProcessor adminCommandProcessor;
    private final BlacklistProcessor blacklistProcessor;
    private PublicCommandProcessor publicCommandProcessor;

    private final processers.persistence.DataManagementProcessor dmp;

    public GuildListener(DataManagementProcessor dmp, Buffer<Event> callBuffer) {
        GuildListener.callBuffer = callBuffer;
        this.dmp = dmp;
        this.blacklistProcessor = new BlacklistProcessor(dmp);
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        Guild guild = event.getJDA().getGuildById(GUILD_ID);
        adminCommandProcessor = new AdminCommandProcessor(adminKeyChars, guild, dmp);
        publicCommandProcessor = new PublicCommandProcessor(userKeyChars, guild, dmp);
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent msg) {
        // Ignore conditions
        if (msg.getAuthor().isBot() || msg.isWebhookMessage()) return;

        // Variable initializing
        Message message = msg.getMessage();
        String content = message.getContentRaw();
        Member member = msg.getMember();
        MessageChannel channel = msg.getChannel();
        Guild guild = msg.getGuild();

        // Message logging
        cmdLogMessage(message);

        // Admin commands
        if (content.startsWith(adminKeyChars)) {
            String log = adminCommandProcessor.processCommand(message);
            if (adminCommandProcessor.lastCommandFailure())
                requireNonNull(guild.getMemberById(dmp.getBotOwner())).getUser().openPrivateChannel().queue(c -> c.sendMessage(log).queue());
            return;
        }

        // User commands
        else if (content.startsWith(userKeyChars)) {
            String log = publicCommandProcessor.processCommand(message);
            if (publicCommandProcessor.lastCommandFailure())
                requireNonNull(guild.getMemberById(dmp.getBotOwner())).getUser().openPrivateChannel().queue(c -> c.sendMessage(log).queue());
            return;
        }

        // Blacklisted members
        if (requireNonNull(msg.getMember(), "User has no roles!").getRoles().contains(msg.getAuthor().getJDA().getRoleById(BLACKLISTED_ROLE_ID))) {
            blacklistProcessor.processMessage(message);
        }


        // As long as it's not in botspam...
        if (!Objects.requireNonNull(dmp.getTextChannel("botspam"), "\"botspam\" channel not found! Contact the bot owner").equals(channel.getId())) {
            // Answer functionality
            dmp.getAnswerTriggers().stream()
                    .filter(s -> content.matches("(?s)" + s))
                    .findFirst().ifPresent(s -> {
                        System.out.println("\t* Message contains answer keyword:" + s);
                        channel.sendMessage(dmp.getAnswer(s)).queue();
                    });
        }

        // Reaction functionality
        dmp.getReactionTriggers().stream()
                .filter(s -> content.matches("(?s)" + s))
                .forEach(s -> {
                    System.out.println("\t* Message contains reaction keyword:" + s);
                    message.addReaction(requireNonNull(guild.getEmoteById(dmp.getReaction(s)), "Emote not found!")).queue();
                });

        // @everyone functionality
        if (content.contains("<@&" + dmp.getRole("everyone") + ">")) {
            guild.addRoleToMember(requireNonNull(member).getIdLong(), requireNonNull(guild.getRoleById(dmp.getRole("everyone")), "@everyone role not found! Perhaps the ID changed? Contact the bot owner.")).queue();
            dmp.addToRestoratorMember(msg.getMember().getId(), requireNonNull(dmp.getRole("everyone"), "@everyone role not found! Perhaps the ID changed? Contact the bot owner."));
            System.out.println("\t* User \"" + member.getEffectiveName() + "\" now has @everyone role. Also updated restorator.");
        }

        System.out.println();
    }
    
    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {

        cmdLogEvent(event.getUser(), "User joined.");

        // Another user joined the chat quick implementation
        requireNonNull(event.getGuild().getTextChannelById(dmp.getTextChannel("general")))
                .sendMessage(event.getUser().getAsMention() + " joined, probably someone's alt\n").queue();

        // New user role
        event.getGuild().addRoleToMember(event.getMember().getId(), requireNonNull(event.getGuild().getRoleById(dmp.getRole("new-users")))).queue();
        if (dmp.getRestoratorMemberIDs().contains(event.getMember().getId())) {
            cmdLogEvent(event.getUser(), "User is registered in the restorator. Restoring roles...");
            for (String roleID : dmp.getRestoratorRoles(event.getMember().getId())) {
                Role role = event.getGuild().getRoleById(roleID);
                if (role != null) event.getGuild().addRoleToMember(event.getMember(), role).reason("Role restored").queue();
            }
        } else {
            cmdLogEvent(event.getUser(), "User isn't registered on the restorator. Registering...");
            dmp.addRestoratorMember(event.getMember().getId());
        }
        System.out.println();
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        cmdLogEvent(event.getUser(), "User removed from server.\n");

        // Another user left the chat quick implementation
        requireNonNull(event.getGuild().getTextChannelById(dmp.getTextChannel("general")))
                .sendMessage("**" + event.getUser().getName() + "** left\n").queue();
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        cmdLogEvent(event.getUser(), "User received one or more roles. Logging...");
        event.getRoles().forEach(role -> dmp.addToRestoratorMember(event.getMember().getId(), role.getId()));
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        cmdLogEvent(event.getUser(), "User removed one or more roles from itself. Logging...");
        event.getRoles().forEach(role -> dmp.removeFromRestoratorMember(event.getMember().getId(), role.getId()));
    }

    private void cmdLogMessage(Message message) {
        // Header
        System.out.print("[" + message.getTimeCreated().getHour() + ":" + message.getTimeCreated().getMinute() + "] " +
                        "@" + message.getAuthor().getName() + "-> " + message.getGuild().getName() + ", " +
                        "in  #" + message.getChannel().getName() + ":\n");
        // Message
        if (message.getContentRaw().length() == 0)
            System.out.println("(No text was sent)");
        else
            System.out.println(message.getContentRaw());
        // Attachments
        if (message.getAttachments().size() != 0) {
            System.out.println("Includes attachments:");
            for (Message.Attachment a : message.getAttachments()) System.out.println("- " + a.getUrl());
        }
    }

    private void cmdLogEvent(User user, String event) {
        System.out.println("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) +
                "]@" + user.getName() + "-> " + event);
    }

}
