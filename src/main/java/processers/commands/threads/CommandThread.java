package processers.commands.threads;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import processers.persistence.DataManagementProcessor;

public abstract class CommandThread implements Runnable {

    protected final int opcode;
    protected final MessageChannel commandChannel;
    protected final User commandAuthor;
    protected final String[] args;
    protected final DataManagementProcessor dmp;
    protected final Guild guild;

    public CommandThread(int opcode, MessageChannel commandChannel, User commandAuthor, DataManagementProcessor dmp, Guild guild, String... args) {
        this.opcode = opcode;
        this.commandChannel = commandChannel;
        this.commandAuthor = commandAuthor;
        this.args = args;
        this.dmp = dmp;
        this.guild = guild;
    }

}
