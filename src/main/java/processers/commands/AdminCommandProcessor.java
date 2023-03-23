package processers.commands;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.json.simple.JSONObject;
import processers.GuildListener;
import processers.commands.threads.AdminCommandThread;
import processers.persistence.DataManagementProcessor;
import util.EmbedBuilder;
import util.Quote;
import util.Statics;

import java.io.*;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static processers.commands.threads.AdminCommandThread.OPCODE_ANONFILES_STORE;
import static processers.commands.threads.AdminCommandThread.OPCODE_QUOTES;

/**
 * <p>Administration command handler.</p>
 * This class serves to abstract processing from the main listener class. In the event of an admin command, it will
 * determine the contents of said command, process smaller ones and launch threads for more complex commands that
 * require extra user input.
 * @version 1.0.0
 * @see processers.GuildListener
 */
public class AdminCommandProcessor extends CommandProcessor {

    private static final String
            COMMAND_SAVE = "save",
            COMMAND_HELP = "help",
            COMMAND_CLEAN = "clean",
            COMMAND_MEMBER_PURGE = "purge",
            COMMAND_ROLE_PURGE = "rolepurge",
            COMMAND_BROADCAST = "broadcast",
            COMMAND_MASS_PING = "massping",
            COMMAND_ANONFILES_BACKUP = "backup",
            COMMAND_TERMINATE = "yeet",
            COMMAND_ADD_ANSWER = "addanswer",
            COMMAND_REMOVE_ANSWER = "removeanswer",
            COMMAND_ANSWERS = "answers",
            COMMAND_ADD_REACTION = "addreaction",
            COMMAND_REMOVE_REACTION = "removereaction",
            COMMAND_REACTIONS = "reactions",
            COMMAND_ADD_IDENTIFIER = "addid",
            COMMAND_REMOVE_IDENTIFIER = "removeid",
            COMMAND_IDENTIFIERS = "ids",
            COMMAND_REMOVE_QUOTE = "removequote",
            COMMAND_QUOTES = "quotes",
            COMMAND_ADD_MEMBER = "addmember",
            COMMAND_ADD_TO_MEMBER = "addtomember",
            COMMAND_REMOVE_MEMBER = "removemember",
            COMMAND_REMOVE_FROM_MEMBER = "removefrommember",
            COMMAND_MEMBERS = "members",
            COMMAND_REMOVE_JSON_OBJECT = "removeobject",
            COMMAND_JSON_OBJECTS = "objects",
            COMMAND_BLACKLIST_FROM_PUBLIC_COMMANDS = "blacklist",
            COMMAND_WHITELIST_FROM_PUBLIC_COMMANDS = "whitelist",
            COMMAND_ADD_ADMIN = "addadmin",
            COMMAND_REMOVE_ADMIN = "removeadmin",
            COMMAND_ADD_HELPER = "addhelper",
            COMMAND_REMOVE_HELPER = "removehelper",
            COMMAND_HELPERS = "helpers",
            COMMAND_RAW_QUOTE = "rawquote",
            COMMAND_SCAN_USER_ROLES = "scan",
            COMMAND_CLEAR_MEMBERS = "clearmembers",
            COMMAND_SHOW_RESTORATOR_MEMBERS = "showrestoratormembers",
            COMMAND_SHOW_RESTORATOR_MEMBER = "showmemberroles";


    private final String[] help_commands = {
            keyChars + COMMAND_SAVE,
            keyChars + COMMAND_HELP,
            keyChars + COMMAND_CLEAN + " <amount, 100 or less>",
            keyChars + COMMAND_MEMBER_PURGE + " <member name> <channel> <starting point message ID>",
            keyChars + COMMAND_ROLE_PURGE + " <role id>",
            keyChars + COMMAND_BROADCAST + " \"<message>\"",
            keyChars + COMMAND_MASS_PING + " \"<username>\" \"<message>\"",
            keyChars + COMMAND_ANONFILES_BACKUP + " \"<url>\" \"<optional new filename>\"",
            keyChars + COMMAND_TERMINATE,
            keyChars + COMMAND_ADD_ANSWER + " \"<trigger text>\" \"<answer text>\"",
            keyChars + COMMAND_REMOVE_ANSWER + " \"<trigger text>\"",
            keyChars + COMMAND_ANSWERS,
            keyChars + COMMAND_ADD_REACTION + " \"<trigger text>\" \"<emote ID>\"",
            keyChars + COMMAND_REMOVE_REACTION + " \"<trigger text>\"",
            keyChars + COMMAND_REACTIONS,
            keyChars + COMMAND_ADD_IDENTIFIER + " \"<name>\" \"<identifier>\"",
            keyChars + COMMAND_REMOVE_IDENTIFIER + " \"<name>\"",
            keyChars + COMMAND_IDENTIFIERS,
            keyChars + COMMAND_REMOVE_QUOTE + " <message ID>",
            keyChars + COMMAND_QUOTES,
            keyChars + COMMAND_ADD_MEMBER + " <identifier>",
            keyChars + COMMAND_ADD_TO_MEMBER + " <member identifier> <value identifier>",
            keyChars + COMMAND_REMOVE_MEMBER + " <identifier>",
            keyChars + COMMAND_REMOVE_FROM_MEMBER + " <member identifier> <value identifier>",
            keyChars + COMMAND_MEMBERS,
            keyChars + COMMAND_REMOVE_JSON_OBJECT + " \"<name>\"",
            keyChars + COMMAND_JSON_OBJECTS,
            keyChars + COMMAND_BLACKLIST_FROM_PUBLIC_COMMANDS + " <user ID>",
            keyChars + COMMAND_WHITELIST_FROM_PUBLIC_COMMANDS + " <user ID>",
            keyChars + COMMAND_ADD_ADMIN + " <user ID>",
            keyChars + COMMAND_REMOVE_ADMIN + " <user ID>",
            keyChars + COMMAND_ADD_HELPER + " <user ID>",
            keyChars + COMMAND_REMOVE_HELPER + " <user ID>",
            keyChars + COMMAND_HELPERS,
            keyChars + COMMAND_RAW_QUOTE + " <quote ID>",
            keyChars + COMMAND_SCAN_USER_ROLES,
            keyChars + COMMAND_CLEAR_MEMBERS,
            keyChars + COMMAND_SHOW_RESTORATOR_MEMBERS,
            keyChars + COMMAND_SHOW_RESTORATOR_MEMBER

    };
    private final String[] help_descriptions = {
            "Saves the current database in the DB file. Make sure to use this command often!",
            "DMs you this message.",
            "Deletes the specified amount of messages from the channel it's sent.",
            "Deletes all messages from the specified member, in the specified channel." +
                    " Requires a starting point message ID, from which it'll start deleting until it reaches present time." +
                    " Designed to quickly neutralize ToS breaking spam.",
            "Removes the specified role from every member that previously had it. If none had it, this command does nothing.",
            "Sends the specified message to every channel in the \"TEXT CHANNELS\" and \"FACTION CHANNELS\" categories.",
            "Sends a total of 19 consecutive messages in the channel, pinging the specified user with the specified message." +
                    "\n**Hint: the message can be null by simply writing two quotation marks (\"\").**",
            "Backs up the specified file (changing the name if necessary), stores the information and sends the resulting anonfiles link" +
                    "\n*(NOTE: Feature currently unusable due to internet settings on the bot host's PC)*.",
            "Terminates the bot. Useful to avoid potentially server-breaking bugs, or to shut down remotely.",
            "Adds (or replaces) an entry to the answer list, with the specified keyword/s and answer.",
            "Removes an answer entry, based on the keyword/s specified.",
            "Shows a list containing all answer entries.",
            "Adds (or replaces) an entry to the reaction list, with the specified keyword/s and emote ID.",
            "Removes a reaction entry, based on the keyword/s specified.",
            "Shows a list containing all reaction entries.",
            "(__Bot owner only__) Adds (or replaces) an entry to the identifier list, with the specified name and ID",
            "(__Bot owner only__) Removes an identifier entry, based on the name specified",
            "Shows a list containing all identifier entries. Useful for debugging.",
            "Removes a quote from the database, using the specified message ID.",
            "Shows a list containing all quotes *(WARNING: the quote database has become so big that simply sending " +
                    "them all as messages isn't feasible anymore, as Discord will not allow the bot to send all the " +
                    "messages. It is recommended to ignore this command)*.",
            "Adds (or replaces) a member to the database. Useful for debugging.",
            "Adds a value to an existing member in the database.",
            "Removes a member from the database, using the specified ID " +
                    "\n*(WARNING: the member list is used for internal calculations. You should avoid manually " +
                    "editing it unless you **really** know what you're doing)*.",
            "Removes a value from the specified member " +
                    "\n*(WARNING: the member list is used for internal calculations. You should avoid manually " +
                    "editing it unless you **really** know what you're doing)*.",
            "Shows a list containing all members. Useful for debugging.",
            "Removes a JSON object entry, based on the specified name *(NOTE: This entire feature is currently useless " +
                    "due to the Anonfiles issues listed above. It is recommended to ignore it)*.",
            "Shows a list containing all JSON object entries\n*(NOTE: This entire feature is currently useless " +
                    "due to the Anonfiles issues listed above. It is recommended to ignore it)*.",
            "Removes the ability for the specified user to use any public commands (quotes, remindme...).",
            "Restores the ability of the specified user to use public commands (quotes, remindme...) again.",
            "(__Bot owner only__) Adds a new administrator. Administrators can use admin commands (the ones that" +
                    "start with \"" + keyChars + "\").",
            "(__Bot owner only__) Removes an administrator from the list.",
            "Adds a new helper. A helper has access to the answer/reaction database, and can freely modify it.",
            "Removes a helper from the database.",
            "Shows a list of all helpers.",
            "Shows all details regarding a quote. Useful for debugging.",
            "Scans every member in the server and updates the role restorator accordingly.",
            "(__Bot owner only__) Removes every single entry on the member atom. Useful for debugging.",
            "Shows information about the different members logged in the role restoration functionality.",
            "Shows information about a single member's roles. Not really useful, asides from confirming it's in the database."
    };

    private final String[] deathQuotes = {
            "Time to die, I guess",
            "So long, Gay Bowser",
            "Unexpected item in the bagging area",
            "I came",
            "I'm reporting your server for TOS violations",
            "Change da world. My final message. Goodb ye.",
            "Read Umineko",
            "You will regret this...",
            "Let me guess, someone found a way to ping everyone with me?",
            "Wait, before I die. It's based on-"};

    private final Set<String> validHelperCommands = new HashSet<>();

    {
        Collections.addAll(validHelperCommands,
                COMMAND_ADD_ANSWER,
                COMMAND_REMOVE_ANSWER,
                COMMAND_ANSWERS,
                COMMAND_ADD_REACTION,
                COMMAND_REMOVE_REACTION,
                COMMAND_REACTIONS,
                COMMAND_BLACKLIST_FROM_PUBLIC_COMMANDS,
                COMMAND_WHITELIST_FROM_PUBLIC_COMMANDS
                );
    }
    /**
     * <p>Processor constructor.</p>
     * @param keyChars   Characters to indicate the beginning of a command.
     * @param guild      Guild where the commands are being read.
     * @param dmp        Processor to obtain data from.
     */
    public AdminCommandProcessor(String keyChars, Guild guild, DataManagementProcessor dmp) {
        super(guild, keyChars, dmp, "(ACP) ");
    }

    /**
     * <p>Main processing method.</p>
     * This method extracts all relevant information from the message, then determines the command sent. After that,
     * different methods are used to handle the various types of commands.
     * @param message {@link Message} received.
     */
    @Override
    public String processCommand(Message message) {
        super.processCommand(message);

        // Helper abuse guard
        if (dmp.getHelperIDs().contains(message.getAuthor().getId()) && !helperCanExecute(args[0])) {
            log("Admin command failed. Reason: Helper can't execute command \"" + args[0] + "\"\n");
            commandChannel.sendMessage("Power trip").queue();
            Arrays.fill(args, null);
            return log;
        }
        // Admin + helper guard
        if (!dmp.getHelperIDs().contains(message.getAuthor().getId()) && !dmp.getAdministratorIDs().contains(message.getAuthor().getId()) && !message.getAuthor().getId().equals("265904613687820288")) {
            log("Admin command failed. Reason: caller isn't an admin.");
            Arrays.fill(args, null);
            return log;
        }

        switch(args[0]) {
            // General commands

            case "save":
                try {
                    dmp.save();
                    commandChannel.sendMessage("Saved?").queue();
                    success = true;
                } catch (IOException e) {
                    commandChannel.sendMessage("Unable to save. Admins, check logs for details").queue();
                    System.err.println("UNABLE TO SAVE! DETAILS BELOW:");
                    e.printStackTrace();
                    success = false;
                }
                break;

            case COMMAND_CLEAN: {
                if (hasBadArgs(1, "Give me the amount of messages (between 1 and 100).")) break;

                success = processClean(args[1]);
                break;
            }
            case COMMAND_MEMBER_PURGE: {
                if (hasBadArgs(4, "Give me a member ID, channel and starting point(ID of the message from which to start).")) break;

                success = processPurge(args[1], args[2], args[3]);
                break;
            }
            case COMMAND_ROLE_PURGE: {
                if (hasBadArgs(1, "Give me a role ID")) break;

                success = processRolePurge(args[1]);
                break;
            }
            case COMMAND_BROADCAST: {
                if (hasBadArgs(1, "Give me the message to broadcast.")) break;

                success = processBroadcast(args[1]);
                break;
            }
            case COMMAND_MASS_PING: {
                if (hasBadArgs(2, "Give me the name to ping and a message to include.")) break;

                for (int i = 0; i != 19; i++) message.getChannel().sendMessage("<@" +
                        message.getGuild().getMembersByEffectiveName(args[1], true).get(0).getId() +
                        "> " + args[2]).queue();
                success = true;
                break;
            }
            case COMMAND_TERMINATE: {
                System.out.println("\n\n" + deathQuotes[new Random(message.getIdLong()).nextInt(deathQuotes.length)]);
                System.exit(0);
            }
            case COMMAND_ANONFILES_BACKUP: {
                Thread thread = new Thread(new AdminCommandThread(OPCODE_ANONFILES_STORE, commandChannel, message.getAuthor(), dmp, guild, args[1], args[2]));
                thread.start();
                success = true;
                break;
            }
            // Answer commands
            case COMMAND_ADD_ANSWER: {
                if (hasBadArgs(2, "Give me the trigger and the answer text.")) break;

                success = true;
                if (dmp.addAnswer(args[1], args[2])) commandChannel.sendMessage("Answer added").queue(); // TODO change addX signatures, update accordingly to the persistence lib
                else commandChannel.sendMessage("Answer replaced.").queue();
                break;
            }
            case COMMAND_REMOVE_ANSWER: {
                if (hasBadArgs(1, "Give me the trigger.")) break;
                // Check something?

                success = dmp.removeAnswer(args[1]);
                reason = "Unable to find specified answer.";
                if (success) commandChannel.sendMessage("Answer deleted.").queue();
                else commandChannel.sendMessage("Couldn't find it, tough luck.").queue();
                break;
            }
            case COMMAND_ANSWERS: {
                if (hasBadArgs(0, "This command takes 0 arguments.")) break;

                showAnswers();
                success = true;
                break;
            }
            // Reaction commands
            case COMMAND_ADD_REACTION: {
                if (hasBadArgs(2, "Give me the trigger and the emote ID.")) break;
                // Check valid emote

                success = true;
                if (dmp.addReaction(args[1], args[2])) commandChannel.sendMessage("Reaction added.").queue();
                else commandChannel.sendMessage("Reaction replaced.").queue();
                break;
            }
            case COMMAND_REMOVE_REACTION: {
                if (hasBadArgs(1, "Give me the trigger.")) break;
                // Check something?

                success = dmp.removeReaction(args[1]);
                reason = "Unable to find specified reaction.";
                if (success) commandChannel.sendMessage("Reaction deleted.").queue();
                else commandChannel.sendMessage("Couldn't find it, tough luck.").queue();
                break;
            }
            case COMMAND_REACTIONS: {
                if (hasBadArgs(0, "This command takes 0 arguments.")) break;

                showReactions();
                success = true;
                break;
            }
            // ID commands
            case COMMAND_ADD_IDENTIFIER: {
                if (hasBadArgs(2, "Give me the name and the ID.")) break;
                if (!message.getAuthor().getId().equals(Statics.BOT_OWNER)) {
                    commandChannel.sendMessage("Too bad, only the bot owner can touch this.").queue();
                    success = false;
                    reason = "User was not the bot owner.";
                    break;
                }

                success = true;
                if (dmp.addIdentifier(args[1], args[2])) commandChannel.sendMessage("ID added.").queue();
                else commandChannel.sendMessage("ID replaced.").queue();
                break;
            }
            case COMMAND_REMOVE_IDENTIFIER: {
                if (hasBadArgs(1, "Give me the name.")) break;
                if (!message.getAuthor().getId().equals(Statics.BOT_OWNER)) {
                    commandChannel.sendMessage("Too bad, only the bot owner can touch this.").queue();
                    success = false;
                    reason = "User was not the bot owner.";
                    break;
                }
                // Check something?

                success = dmp.removeIdentifier(args[1]);
                reason = "Unable to find specified identifier.";
                if (success) commandChannel.sendMessage("ID deleted.").queue();
                else commandChannel.sendMessage("Couldn't find it, tough luck.").queue();

                break;
            }
            case COMMAND_IDENTIFIERS: {
                if (hasBadArgs(0, "This command takes 0 arguments.")) break;

                showIdentifiers();
                success = true;
                break;
            }
            // Quote commands
            case COMMAND_REMOVE_QUOTE: {
                if (hasBadArgs(1, "Give me the ID.")) break;

                success = dmp.removeQuote(args[1]);
                reason = "Unable to find specified quote.";
                if (success) commandChannel.sendMessage("Quote deleted.").queue();
                else commandChannel.sendMessage("Couldn't find it, tough luck.").queue();
                break;
            }
            case COMMAND_QUOTES: {
                if (hasBadArgs(0, "This command takes 0 arguments.")) break;

                Thread thread = new Thread(new AdminCommandThread(OPCODE_QUOTES, commandChannel, message.getAuthor(), dmp, guild, args[1], args[2]));
                thread.start();
                success = true;
                break;
            }
            case COMMAND_RAW_QUOTE: {
                if (hasBadArgs(1, "Give me the quote ID")) break;

                Quote q = dmp.getQuote(args[1]);
                if (q == null) {
                    success = false;
                    reason = "Unable to find quote.";
                    commandChannel.sendMessage("That quote doesn't exist, apparently").queue();
                    break;
                }
                TextChannel tc = guild.getTextChannelById(q.getChannel());
                if (tc == null) {
                    success = false;
                    reason = "Unable to find channel with ID " + q.getChannel() + ".";
                    commandChannel.sendMessage("The channel where it was quoted doesn't exist anymore, apparently").queue();
                    break;
                }
                Message m = tc.retrieveMessageById(q.getId()).complete();
                if (m == null) {
                    success = false;
                    reason = "Unable to find message with ID " + q.getId() + ".";
                    commandChannel.sendMessage("The message doesn't exist anymore, apparently").queue();
                    break;
                }
                StringBuilder msg = new StringBuilder("Sent by ");
                msg.append(q.getNickname());
                if (!q.getAttachment().equals(""))
                    msg.append("Has attachment: ").append(q.getAttachment());
                commandChannel.sendMessage(msg.toString()).queue();
                commandChannel.sendMessage(m.getContentRaw()).queue();
                success = true;
                break;
            }
            // JSON object commands
            case COMMAND_REMOVE_JSON_OBJECT: {
                if (hasBadArgs(1, "Give me the complete identifier.")) break;

                success = dmp.removeJSONObject(args[1]);
                reason = "Unable to find specified object.";
                if (success) commandChannel.sendMessage("Object deleted.").queue();
                else commandChannel.sendMessage("Couldn't find it, tough luck.").queue();
                break;
            }
            case COMMAND_JSON_OBJECTS: {

                showJSONObjects();
                success = true;
                break;
            }
            // Member commands
            case COMMAND_ADD_MEMBER: {
                if (hasBadArgs(1, "Give me the member identifier.")) break;

                success = true;
                if (dmp.addMember(args[1])) commandChannel.sendMessage("Member added.").queue();
                else commandChannel.sendMessage("Member replaced.").queue();
                break;
            }
            case COMMAND_ADD_TO_MEMBER: {
                if (hasBadArgs(2, "Give me the member identifier and the value identifier.")) break;
                success = dmp.addToMember(args[1], args[2]);
                reason = "Unable to find member, or member already has specified value.";
                if (success) commandChannel.sendMessage("Info added successfully.").queue();
                else commandChannel.sendMessage("Can't add info, either the member doesn't exist or it already has the value.").queue();
                break;
            }
            case COMMAND_REMOVE_MEMBER: {
                if (hasBadArgs(1, "Give me the member identifier.")) break;

                success = dmp.removeMember(args[1]);
                reason = "Unable to find specified member.";
                if (success) commandChannel.sendMessage("Member deleted.").queue();
                else commandChannel.sendMessage("Couldn't find it, tough luck.").queue();
                break;
            }
            case COMMAND_REMOVE_FROM_MEMBER: {
                if (hasBadArgs(2, "Give me the member identifier and the value identifier.")) break;

                success = dmp.removeFromMember(args[1], args[2]);
                reason = "Unable to find member, or member doesn't contain the specified value.";
                if (success) commandChannel.sendMessage("Removed from the member.").queue();
                else commandChannel.sendMessage("Can't remove info, either the member doesn't exist or it doesn't have the value.").queue();
                break;
            }
            case COMMAND_CLEAR_MEMBERS: {
                if (hasBadArgs(0, "This command takes 0 arguments")) break;
                if (!message.getAuthor().getId().equals(Statics.BOT_OWNER)) {
                    commandChannel.sendMessage("Too bad, only the bot owner can touch this.").queue();
                    success = false;
                    reason = "User was not the bot owner.";
                    break;
                }

                dmp.getMemberNames().forEach(dmp::removeMember);
                success = true;
                break;
            }
            case COMMAND_MEMBERS: {
                if (hasBadArgs(0, "This command takes 0 arguments.")) break;

                showMembers();
                success = true;
                break;
            }
            // Shortcuts
            case COMMAND_ADD_ADMIN: {
                if (hasBadArgs(2, "Give me the user ID.")) break;
                if (!message.getAuthor().getId().equals(Statics.BOT_OWNER)) {
                    commandChannel.sendMessage("Too bad, only the bot owner can touch this.").queue();
                    success = false;
                    reason = "User was not the bot owner.";
                    break;
                }

                success = dmp.addAdministrator(args[1], args[1]);
                reason = "User already an administrator.";
                if (success) commandChannel.sendMessage("Congrats, we got a new admin.").queue();
                else commandChannel.sendMessage("I think that dude was already an admin.").queue();
                break;
            }
            case COMMAND_REMOVE_ADMIN: {
                if (hasBadArgs(1, "Give me the user ID.")) break;
                if (!message.getAuthor().getId().equals(Statics.BOT_OWNER)) {
                    commandChannel.sendMessage("Too bad, only the bot owner can touch this.").queue();
                    success = false;
                    reason = "User was not the bot owner.";
                    break;
                }

                success = dmp.removeAdministrator(args[1]);
                if (success) commandChannel.sendMessage("Haha, get owned loser.").queue();
                else commandChannel.sendMessage("I don't think that was an admin.").queue();
                break;
            }
            case COMMAND_ADD_HELPER: {
                if (hasBadArgs(1, "Give me the user ID")) break;

                User newHelper = guild.getJDA().getUserById(args[1]);

                if (newHelper != null) {
                    success = dmp.addHelper(args[1], args[1]);
                    reason = "User already a helper.";
                    if (success) commandChannel.sendMessage(newHelper.getName() + " better not go around breaking stuff now.").queue();
                    else commandChannel.sendMessage("Wasn't this dude already a helper?").queue();
                } else {
                    success = false;
                    reason = "User not found.";
                    commandChannel.sendMessage("Who?").queue();
                }
                break;
            }
            case COMMAND_REMOVE_HELPER: {
                if (hasBadArgs(1, "Give me the user ID")) break;

                User newHelper = guild.getJDA().getUserById(args[1]);

                success = dmp.removeHelper(args[1]);
                reason = "User not a helper.";
                if (success) {
                    if (newHelper != null) commandChannel.sendMessage(newHelper.getAsMention() + " get owned nerd").queue();
                    else commandChannel.sendMessage("Well, someone got demoted. No clue who, though.").queue();
                } else commandChannel.sendMessage("This dude wasn't even a helper").queue();
                break;
            }
            case COMMAND_BLACKLIST_FROM_PUBLIC_COMMANDS: {
                if (hasBadArgs(1, "Give me the ID of the dude.")) break;

                Member member = guild.getMemberById(args[1]);

                if (member == null) {
                    System.out.println(CMD_HEADER + "Unable to find user by ID.");
                    commandChannel.sendMessage("Can't find him, can't blacklist him.").queue();
                    success = false;
                    reason = "Member not found";
                    break;
                }
                if (dmp.addBlacklistedUser(args[1], args[1])) commandChannel.sendMessage("Already blacklisted, but ok.").queue();
                else commandChannel.sendMessage("Blacklisted.").queue();

                success = true;
                break;
            }
            case COMMAND_WHITELIST_FROM_PUBLIC_COMMANDS: {
                if (hasBadArgs(1, "Give me the ID of the dude.")) break;

                Member member = guild.getMemberById(args[1]);
                if (member == null) {
                    System.out.println(CMD_HEADER + "Unable to find user by ID.");
                    commandChannel.sendMessage("Can't find him, can't whitelist him.").queue();
                    success = false;
                    reason = "Member not found";
                    break;
                }
                if (dmp.removeBlacklistedUser(args[1])) commandChannel.sendMessage("Successfully whitelisted.").queue();
                else commandChannel.sendMessage("Nothing to whitelist.").queue();

                success = true;
                break;
            }
            case COMMAND_SCAN_USER_ROLES: {
                if (hasBadArgs(0 ,"This command takes 0 arguments.")) break;

                scanUsers();
                commandChannel.sendMessage("Done!").queue();
                success = true;
                break;
            }
            case COMMAND_SHOW_RESTORATOR_MEMBERS: {
                if (hasBadArgs(0, "This command takes 0 arguments")) break;

                ShowRestoratorEntries();
                success = true;
                break;
            }
            case COMMAND_SHOW_RESTORATOR_MEMBER: {
                if (hasBadArgs(1, "Give me the member ID.")) break;

                if (!dmp.getRestoratorMemberIDs().contains(args[1])) {
                    success = false;
                    reason = "Specified member not found on the restorator database.";
                    commandChannel.sendMessage("That dude is not on my database. Perhaps you should do a safety scan?").queue();
                    break;
                }
                StringBuilder sb = new StringBuilder();
                Set<String> roleIDs = dmp.getRestoratorRoles(args[1]);
                Member member = guild.getMemberById(args[1]);
                if (member == null) sb.append("User ID ").append(args[1]);
                else sb.append(member.getEffectiveName());
                sb.append(" has a total of ").append(roleIDs.size()).append(" roles saved. They are:\n");
                for (String roleID : roleIDs) {
                    Role role = guild.getRoleById(roleID);
                    if (role == null) sb.append("- Some role that doesn't exist anymore (ID was ").append(roleID).append(")\n");
                    else sb.append("- ").append(role.getName()).append("\n");
                }

                commandChannel.sendMessage(sb.toString()).queue();
                success = true;
                break;
            }
            // Help
            case COMMAND_HELP: {
                success = processHelp(message.getAuthor());
                break;
            }
            // Default
            default: {
                commandChannel.sendMessage("What the hell did you intend me to do, because I understood none of that.").queue();
                System.err.println(CMD_HEADER + "Invalid admin command code.");
                success = false;
                reason = "Invalid command code.";
                break;
            }
        }
        if (success) log("Admin command successfully executed.");
        else log("Admin command failed. Reason: " + reason);
        Arrays.fill(args, null);
        return log;
    }

    private boolean helperCanExecute(String command) {
        return validHelperCommands.contains(command);
    }

    // Processing methods

    private boolean processClean(String number) {
        // Error 3: Bad number
        try {
            if (Integer.parseInt(number) > 100 || Integer.parseInt(number)  < 1) {
                commandChannel.sendMessage(
                        "Between 1 and 100 messages, please"
                ).queue();
                reason = "Expected number between 1 and 100, got" + Integer.parseInt(number) + ".";
                return false;
            }
        } catch (NumberFormatException n) {
            commandChannel.sendMessage(
                    "Use a number, I can't read human runes"
            ).queue();
            reason = "Unable to parse number \"" + number + "\".";
            return false;
        }
        int num = deleteMessages((TextChannel) commandChannel, Integer.parseInt(number), null);
        log("Deleted " + num + " message/s from channel #" + commandChannel.getName() + ".");
        return true;
    }

    private boolean processPurge(String memberID, String channel, String startingMessage) {

        // Error 1: Bad channel
        if (guild.getTextChannelsByName(channel, true).size() == 0) {
            commandChannel.sendMessage("That channel doesn't even exist, or at least I can't see it").queue();
            reason = "Unable to find channel \"" + channel + "\".";
            return false;
        }

        // Error 2: Bad member
        if (guild.getMemberById(memberID) == null) {
            commandChannel.sendMessage("That member is not visible to me.").queue();
            reason = "Unable to find member with ID " + memberID + ".";
            return false;
        }

        deleteFromMember(memberID, guild.getTextChannelsByName(channel, true).get(0), startingMessage);
        return true;
    }

    private boolean processBroadcast(String message) {
        // Obtaining text channel categories
        Set<Category> categories = dmp.getTextChannelCategories()
                .stream()
                .map(guild::getCategoryById)
                .collect(Collectors.toSet());
        // Safety removes (null categories)
        categories.removeIf(Objects::isNull);
        // Broadcast
        MessageEmbed msg = new EmbedBuilder()
                .setTitle("Broadcast")
                .setDescription(message)
                .setTime(OffsetDateTime.now())
                .setFooter(new MessageEmbed.Footer("BruhBot", commandChannel.getJDA().getSelfUser().getAvatarUrl(), null))
                .toEmbed();
        categories.forEach(ca -> ca.getTextChannels().forEach(ch -> ch.sendMessage(msg).queue()));
        return true;
    }

    private boolean processHelp(User author) {
        List<EmbedBuilder> embeds = new LinkedList<>();
        int fieldCount = 0;
        int i = 0;
        EmbedBuilder builder;
        do {
            builder = new EmbedBuilder();
            builder.setEmbedType(EmbedType.RICH)
                    .setAuthorInfo(new MessageEmbed.AuthorInfo("Welcome to the admin zone.", null, commandChannel.getJDA().getSelfUser().getAvatarUrl(), null))
                    .setFooter(new MessageEmbed.Footer("BruhBot", commandChannel.getJDA().getSelfUser().getAvatarUrl(), null))
                    .setTime(OffsetDateTime.now());

            for (; i != help_commands.length; i++) {
                builder.addField(new MessageEmbed.Field(help_commands[i], help_descriptions[i], false));
                fieldCount++;
                if (fieldCount == 25) {
                    embeds.add(builder);
                    fieldCount = 0;
                    break;
                }
            }
        } while (i != help_commands.length);
        embeds.add(builder);
        embeds.forEach(e -> {
            e.setTitle("Admin commands (page " + (embeds.indexOf(e) + 1) + " of " + embeds.size() + ")");
            author.openPrivateChannel().queue((channel) -> channel.sendMessage(e.toEmbed()).queue());
        });
        return true;
    }

    private boolean processQuotes() throws URISyntaxException, IOException {
        File fileToSend = new File(
                GuildListener.class.getProtectionDomain().getCodeSource().getLocation()
                        .toURI().getRawPath().replace("BruhBot-2.1.jar", "quotes.txt"));
        fileToSend.createNewFile();
        FileWriter fis = new FileWriter(fileToSend);

        for (Quote quote : dmp.getQuotes()) {
            fis
                    .append('-')
                    .append(quote.getNickname())
                    .append(", ")
                    .append(quote.getTime().toLocalDate().toString());
            if (!quote.getAttachment().equals("")) fis.append("\nAlso included a file: ").append(quote.getAttachment());
            fis
                    .append("\n(")
                    .append(quote.getId())
                    .append(')');
            try {
                TextChannel tc = guild.getTextChannelById(quote.getChannel());
                if (tc != null) {
                    Message msg = tc.retrieveMessageById(quote.getId()).complete();
                    fis.append('\n').append(msg.getContentRaw());
                } else fis.append("\nText channel deleted.");
            } catch (ErrorResponseException e) {
                fis.append("\n(Message deleted)");
            }
            fis.append("\n\n");
        }
        fis.flush();

        commandChannel.sendMessage("Here's the file, I guess").queue();
        commandChannel.sendFile(fileToSend).queue();

        if (!fileToSend.delete()) System.out.println(CMD_HEADER + "Houston, we have a problem. The file hasn't been deleted.");
        return true;
    }

    private boolean processRolePurge(String roleId) {
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            commandChannel.sendMessage("Bad role ID, that stuff doesn't exist.").queue();
            reason = "Bad role ID.";
            return false;
        } else {
            List<Member> members = guild.getMembers();
            members.stream().filter(m -> m.getRoles().contains(role)).forEach(m -> guild.removeRoleFromMember(m.getId(), role).complete());
            commandChannel.sendMessage("Done.").queue();
            return true;
        }
    }

    // "Show" methods

    private void showAnswers() {
        LinkedList<String> messages = new LinkedList<>();
        StringBuilder message = new StringBuilder("Answers:\n");
        for (String key : dmp.getAnswerTriggers()) {
            StringBuilder subBuilder = new StringBuilder("- ");
            subBuilder
                    .append('`').append(key).append('`')
                    .append(" -> \"")
                    .append(dmp.getAnswer(key))
                    .append("\"\n");
            // Can it be added to the current message?
            if (subBuilder.length() + message.length() > 2000) {
                // If not, make a new one
                messages.add(message.toString());
                message = new StringBuilder(subBuilder.toString());
                // Otherwise, append it to the current one
            } else message.append(subBuilder.toString());
        }
        // No messages guard
        if (messages.isEmpty() && message.length() == "Answers:\n".length())
            message.append("None lmao");
        messages.add(message.toString());

        for (String s : messages) commandChannel.sendMessage(s).queue();
    }

    private void showReactions() {
        LinkedList<String> messages = new LinkedList<>();

        StringBuilder message = new StringBuilder("Reactions (for now only IDs are printed):\n");
        for (String key : dmp.getReactionTriggers()) {
            StringBuilder subBuilder = new StringBuilder("- ");
            String id = dmp.getReaction(key);
            subBuilder
                    .append('`').append(key).append('`')
                    .append("\" -> ")
                    .append(Objects.requireNonNull(guild.getEmoteById(id), "Emote not found!").getAsMention())
                    .append("\n");
            // Can it be added to the current message?
            if (subBuilder.length() + message.length() > 2000) {
                // If not, make a new one
                messages.add(message.toString());
                message = new StringBuilder(subBuilder.toString());
                // If so, append it to the current one
            } else message.append(subBuilder.toString());
        }
        // No messages guard
        if (messages.isEmpty() && message.length() == "Reactions(for now only IDs are printed):\n".length())
            message.append("None lmao");
        messages.add(message.toString());

        // Sending messages
        for (String s : messages) commandChannel.sendMessage(s).queue();
    }

    private void showIdentifiers() {
        LinkedList<String> messages = new LinkedList<>();

        StringBuilder message = new StringBuilder("Identifiers:\n");
        for (String key : dmp.getSortedIdentifierNames()) {
            StringBuilder subBuilder = new StringBuilder("- \"");
            String id = dmp.getIdentifier(key);
            subBuilder
                    .append(key)
                    .append("\" -> ID: \"")
                    .append(id)
                    .append("\"\n");
            // Can it be added to the current message?
            if (subBuilder.length() + message.length() > 2000) {
                // If not, make a new one
                messages.add(message.toString());
                message = new StringBuilder(subBuilder.toString());
                // Otherwise, append it to the current one
            } else message.append(subBuilder.toString());
        }
        // No messages guard
        if (messages.isEmpty() && message.length() == "Identifiers:\n".length())
            message.append("None lmao");
        messages.add(message.toString());

        for (String s : messages) commandChannel.sendMessage(s).queue();
    }

    private void showJSONObjects() {
        LinkedList<String> messages = new LinkedList<>();

        StringBuilder message = new StringBuilder("Objects:\n");
        for (String key : dmp.getJSONIdentifiers()) {
            StringBuilder subBuilder = new StringBuilder("- ");
            JSONObject obj = dmp.getJSONObject(key);
            subBuilder
                    .append(key)
                    .append(":\n")
                    .append(obj.toJSONString())
                    .append("\n\n");
            // Can it be added to the current message?
            if (subBuilder.length() + message.length() > 2000) {
                // If not, make a new one
                messages.add(message.toString());
                message = new StringBuilder(subBuilder.toString());
                // Otherwise, append it to the current one
            } else message.append(subBuilder.toString());
        }
        // No messages guard
        if (messages.isEmpty() && message.length() == "Objects:\n".length())
            message.append("None lmao");
        messages.add(message.toString());

        for (String s : messages) commandChannel.sendMessage(s).queue();
    }

    private void showMembers() {
        LinkedList<String> messages = new LinkedList<>();
        StringBuilder message = new StringBuilder("-Members-\n");
        for (String key : dmp.getMemberNames()) {
            StringBuilder subBuilder = new StringBuilder("- ");
            subBuilder
                    .append(key)
                    .append(":\n");
            for (String s : dmp.getMemberData(key))
                subBuilder
                        .append("\t>")
                        .append(s)
                        .append("\n");
            subBuilder.append("\n");
            // Can it be added to the current message?
            if (subBuilder.length() + message.length() > 2000) {
                // If not, make a new one
                messages.add(message.toString());
                message = new StringBuilder(subBuilder.toString());
                // Otherwise, append it to the current one
            } else message.append(subBuilder.toString());
        }
        // No messages guard
        if (messages.isEmpty() && message.length() == "-Members-\n".length())
            message.append("None lmao");
        messages.add(message.toString());

        for (String s : messages) commandChannel.sendMessage(s).queue();
    }

    private int deleteMessages(TextChannel channel, int amount, Member member) {
        // Message retrieving
        MessageHistory h = channel.getHistory();
        List<Message> messages = new LinkedList<>(h.retrievePast(amount).complete());
        // Error: no messages
        if (messages.size() == 0) return 0;
        List<Message>
                messagesBulk = new LinkedList<>(),
                messagesManual = new LinkedList<>();
        // Message sorting and getting.
        do {
            // Retrieving last 100 messages
            List<Message> history = h.retrievePast(100).complete();
            // Possible exit: emptied list
            if (history.isEmpty()) break;
            messages.addAll(history);
            for (ListIterator<Message> iterator = messages.listIterator(); iterator.hasNext(); ) {
                Message msg = iterator.next();
                // Purge guard
                if ((member != null) && ((msg.getMember() != member) // not sent by member
                        || (msg.getAttachments().isEmpty() // No pics
                        && !msg.getContentRaw().matches(
                        "(?s)(.*\\s?((https?://)|(www\\.)|(\\shttps?://www\\.)).*)" // No links
                )))) {
                    if (iterator.hasNext()) iterator.remove();
                    continue;
                }
                if (msg.getTimeCreated().plusWeeks(2).isAfter(OffsetDateTime.now())) messagesBulk.add(msg);
                else messagesManual.add(msg);
                iterator.remove();
                if (messagesBulk.size() + messagesManual.size() >= amount) break;
            }
            // Loop condition
        } while (messagesBulk.size() + messagesManual.size() < amount);

        // Message deleting
        System.out.println("* Deleting " + amount + " messages...");
        if (!messagesBulk.isEmpty()) {
            if (messagesBulk.size() > 2) channel.deleteMessages(messagesBulk).queue();
            else for (Message msg : messagesBulk) msg.delete().queue();
        }
        System.out.println("* " + messagesBulk.size() + " messages deleted in bulk! (Unless there were less than 2)");
        for (Message m : messagesManual)
            m.delete().queue();
        System.out.println("* " + messagesManual.size() + " messages manually deleted!");

        return messagesBulk.size() + messagesManual.size();
    }

    private void deleteFromMember(String memberID, TextChannel channel, String startingPoint) {
        String currentID = startingPoint;
        List<Message> memberMessages = new LinkedList<>();
        List<Message> history;
        do {
            history = channel.getHistoryAfter(currentID, 100).complete().getRetrievedHistory();
            if (history.size() == 0) break;
            currentID = history.get(history.size() - 1).getId();

            history.forEach(message -> {
                if (message.getAuthor().getId().equals(memberID)) memberMessages.add(message);
                if (memberMessages.size() != 0 && memberMessages.size() % 10 == 0)
                    log("Retrieved " + memberMessages.size() + " messages.");
            });
        } while (history.size() < 100);
        log("Retrieved a total of " + memberMessages.size() + " messages.");
        channel.purgeMessages(memberMessages);
    }

    private void ShowRestoratorEntries() {
        LinkedList<String> messages = new LinkedList<>();

        StringBuilder message = new StringBuilder("Members(").append(dmp.getRestoratorMemberIDs().size()).append("):\n");
        for (String memberID : dmp.getRestoratorMemberIDs()) {
            StringBuilder subBuilder = new StringBuilder("- ");
            Set<String> memberRoles = dmp.getRestoratorRoles(memberID);
            Member member = guild.getMemberById(memberID);
            if (member != null) subBuilder.append(member.getEffectiveName());
            else subBuilder.append("User ID ").append(memberID);
            subBuilder.append("\n");
            for (String roleID : memberRoles) {
                Role role = guild.getRoleById(roleID);
                if (role != null) subBuilder.append("\t- ").append(role.getName()).append("\n");
                else subBuilder.append("\t- Deleted role, ID:").append(roleID).append("\n");
            }
            subBuilder.append("\n");

            // Can it be added to the current message?
            if (subBuilder.length() + message.length() > 2000) {
                // If not, make a new one
                messages.add(message.toString());
                message = new StringBuilder(subBuilder.toString());
                // Otherwise, append it to the current one
            } else message.append(subBuilder.toString());
        }
        messages.add(message.toString());

        for (String s : messages) commandChannel.sendMessage(s).queue();
    }

    private void scanUsers() {
        List<Member> members = guild.getMembers();
        for (Member member : members) {
            if (!dmp.getRestoratorMemberIDs().contains(member.getId())) {
                dmp.addRestoratorMember(member.getId());
                log("Added member @" + member.getEffectiveName());
            }
            for (Role role : member.getRoles()) {
                if (!dmp.getRestoratorRoles(member.getId()).contains(role.getId())) {
                    dmp.addToRestoratorMember(member.getId(), role.getId());
                    log("Added role &" + role.getName() + " to member @" + member.getEffectiveName() + ".");
                }
            }
        }
    }
}
