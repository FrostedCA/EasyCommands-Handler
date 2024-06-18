package ca.tristan.testbot;


import ca.tristan.easycommands.EasyCommands;
import ca.tristan.easycommands.commands.defaults.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

import java.io.IOException;
import java.net.URISyntaxException;

public class BotMain {

    private static JDA jda;

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
        EasyCommands easyCommands = new EasyCommands("exampletoken");
        BotMain botMain = new BotMain();
        jda = botMain.setupJDA(easyCommands);

        jda.getPresence().setActivity(Activity.playing("with commands"));
    }

    public static JDA getJDA() {
        return jda;
    }

    private JDA setupJDA(EasyCommands easyCommands) throws InterruptedException {
        return easyCommands.registerListeners(

        ).addExecutor(
                new HelpCmd(easyCommands)
        ).addEnabledCacheFlags().addGatewayIntents().buildJDA();
    }

}
