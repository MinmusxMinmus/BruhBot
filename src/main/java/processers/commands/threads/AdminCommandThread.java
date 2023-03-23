package processers.commands.threads;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import processers.GuildListener;
import processers.persistence.DataManagementProcessor;
import util.Quote;
import util.Statics;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Random;

public class AdminCommandThread extends CommandThread {

    public static final int OPCODE_RESERVED = 0;
    public static final int OPCODE_ANONFILES_STORE = 1;
    public static final int OPCODE_QUOTES = 2;

    private static final String CMD_HEADER = "(ACP)THREAD>\t";

    public AdminCommandThread(int opcode, MessageChannel commandChannel, User commandAuthor, DataManagementProcessor dmp, Guild guild, String... args) {
        super(opcode, commandChannel, commandAuthor, dmp, guild, args);
    }

    @Override
    public void run() {
        System.out.println(CMD_HEADER + "Thread launched. Opcode: " + opcode);

        switch (opcode) {
            case OPCODE_RESERVED: {
                commandChannel.sendMessage("Test successful!").queue();
                break;
            }
            case OPCODE_ANONFILES_STORE: { // Args: link
                commandChannel.sendMessage("Alright, give me a sec. I have to download and upload stuff, so it takes long.").queue();
                String anonfilesAnswer = "";
                String filename;
                if (args[1] == null) filename = args[0].split("/")[args[0].split("/").length - 1];
                else filename = args[1];
                File pic = null;
                try {
                    URI path = GuildListener.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                    pic = new File(path.getRawPath().replace("BruhBot-2.1.jar", "") + filename);
                } catch (URISyntaxException ignored) { }
                try {
                    // Obtaining the file
                    Connection.Response upload = Jsoup
                            .connect(args[0])
                            .ignoreContentType(true).execute();

                    // Saving the file
                    FileOutputStream fos = new FileOutputStream(pic);
                    fos.write(upload.bodyAsBytes());
                    fos.flush();
                    fos.close();

                    FileInputStream fis = new FileInputStream(pic);

                    // Posting the file
                    Document anonfilesResponse = Jsoup
                            .connect("https://api.anonfiles.com/upload")
                            .ignoreContentType(true)
                            .data("file", pic.getName(), fis)
                            .data("token", Statics.ANONFILES_API_KEY)
                            .method(Connection.Method.POST)
                            .execute().parse();

                    // Deleting the file
                    fis.close();
                    Files.delete(pic.toPath());

                    // Reading the JSON object
                    Element el = anonfilesResponse.body();
                    anonfilesAnswer = el.toString().substring("<body>\n".length(), el.toString().length() - "\n</body>".length());
                    JSONParser p = new JSONParser();
                    JSONObject o = (JSONObject) p.parse(anonfilesAnswer);

                    // Storing the JSON object
                    dmp.addJSONObject("backups." + filename + "." + new Random(Thread.currentThread().getId()).nextInt(), o);
                    System.out.println(CMD_HEADER + "Anonfiles answer received and stored.");

                    if (o.get("status").equals("false")) {
                        commandChannel.sendMessage("Looks like it failed to save. Let me tell you why:").queue();
                        JSONObject error = (JSONObject)p.parse((String)o.get("error"));
                        commandChannel.sendMessage((String)error.get("message")).queue();
                    } else {
                        JSONObject data = (JSONObject)p.parse(o.get("data").toString());
                        JSONObject file = (JSONObject)p.parse(data.get("file").toString());
                        JSONObject url = (JSONObject)p.parse(file.get("url").toString());
                        commandChannel.sendMessage("Your file's been uploaded. The link is " + url.get("short")).queue();
                    }

                } catch (FileNotFoundException e) {
                    System.err.println(CMD_HEADER + "ERROR: File not found! Trace below:");
                    e.printStackTrace();
                } catch (IOException e) {
                    System.err.println(CMD_HEADER + "ERROR: IO exception! Trace below:");
                    e.printStackTrace();
                    System.err.println("Check " + pic.getAbsolutePath());
                } catch (ParseException e) {
                    System.err.println(CMD_HEADER + "ERROR: JSON response parse exception! Trace below:");
                    e.printStackTrace();
                    System.err.println("JSON object was: " + anonfilesAnswer);
                }
                break;
            }

            case OPCODE_QUOTES: {
                commandChannel.sendMessage("OK, this is gonna take some time. Hang in there pal. " +
                        "Don't worry about me dying suddenly, if that happens I'll tell you.").queue();
                try {
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
                    fis.close();

                    commandChannel.sendMessage("Here's the file, I guess").queue();
                    commandChannel.sendFile(fileToSend).queue();

                    if (!fileToSend.delete()) System.out.println(CMD_HEADER + "Houston, we have a problem. The file hasn't been deleted.");
                } catch (URISyntaxException | IOException e) {
                    commandChannel.sendMessage("Well, it happened. Something went wrong. " +
                            "Sorry, you're gonna have to ask the bot owner to investigate.").queue();
                    e.printStackTrace();
                }

            }
        }

        System.out.println(CMD_HEADER + "Thread ended.");
    }
}
