package processers.administration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import processers.persistence.DataManagementProcessor;

import java.util.Objects;

public class BlacklistProcessor {

    private static final String CMD_HEADER = "(BP)> ";
    private static final String URL_REGEX = "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)";

    private final DataManagementProcessor dmp;

    public BlacklistProcessor(DataManagementProcessor dmp) {
        this.dmp = dmp;
    }

    public void processMessage(Message message) {
        // Only if there's a link detected
        if (!message.getContentRaw().toLowerCase().matches(URL_REGEX)) return;

        // Attempting to log infraction
        String infractionMessage = "Infamous poster " + message.getAuthor().getName() + " sent bad stuff in <#" + message.getChannel().getId() + ">.";
        if (dmp.getTextChannel("logs") != null) {
            try {
                TextChannel logsChannel = Objects.requireNonNull(message.getGuild().getTextChannelById(dmp.getIdentifier("logs")));
                logsChannel.sendMessage(infractionMessage).queue();
            } catch (NullPointerException e) {
                sendToAdmins(message.getJDA(), "No logs channel in memory. Remember to use \"logs\" when declaring it.");
                System.err.println(CMD_HEADER + "No logs channel available.");
            }

        } else sendToAdmins(message.getJDA(), "Update the logs ID. A blacklisted user sent a link, I deleted it.");

        // Attempting to repost message
        if (dmp.getTextChannel("archive") != null)
            repost(message.getGuild(), dmp.getTextCategory("archive"), message);
        else
            sendToAdmins(message.getJDA(), "No archive channel in memory. Remember to use \"archive\" when declaring it");

        message.delete().reason("Link from a blacklisted user").queue();
        System.out.println("* Deleted message with reason \"Link from a blacklisted user\"");
    }

    public void repost(Guild guild, String archive, Message message) {
        TextChannel channel;
        try {
            channel = Objects.requireNonNull(guild.getTextChannelById(archive));
            channel.sendMessage("Sent by " + message.getAuthor().getName() + ":\n" + message.getContentRaw()).queue();
            System.out.println(CMD_HEADER + "Reposted message in #" + archive);
        } catch (NullPointerException e) {
            sendToAdmins(message.getJDA(), "Archive channel broke, update ASAP.");
            System.err.println(CMD_HEADER + "Archive channel not found.");
        }
    }

    public void sendToAdmins(JDA jda, String message) {
        for (String id : dmp.getAdministratorIDs()) {
            try {
                Objects.requireNonNull(jda.getUserById(id)).openPrivateChannel().queue((chn)-> chn.sendMessage(message).queue());
            } catch (NullPointerException e) {
                System.err.println(CMD_HEADER + "Unable to find admin!(ID: " + id + "). Did the user leave?");
            }
        }
    }
}
