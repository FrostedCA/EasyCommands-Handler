package ca.tristan.easycommands;

import ca.tristan.easycommands.commands.IExecutor;
import ca.tristan.easycommands.commands.slash.SlashCommands;
import ca.tristan.easycommands.commands.slash.SlashExecutor;
import ca.tristan.easycommands.utils.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class EasyCommands {


    public static JDA jda;
    private final JDABuilder jdaBuilder;
    
    private final Map<String, IExecutor> executorMap = new HashMap<>();

    private final List<GatewayIntent> gatewayIntents = new ArrayList<>();
    private final List<CacheFlag> enabledCacheFlags = new ArrayList<>();
    private final List<CacheFlag> disabledCacheFlags = new ArrayList<>();
    
    private final SlashCommands slashCommands;
    
    private Long millisStart;
    private Logger logger;

    /**
     * Often used when hosting on a server which can't have a config file.
     * @param token Bot Token
     */
    public EasyCommands(String token) throws IOException {

        millisStart = System.currentTimeMillis();
        
        this.logger = new Logger(this);

        loadIntents();
        
        jdaBuilder = JDABuilder.create(token, gatewayIntents);

        this.slashCommands = new SlashCommands(this);
        
        jdaBuilder.addEventListeners(slashCommands);
    }

    /**
     * Use this function to start the bot
     */
    public JDA buildJDA() throws InterruptedException {

        jdaBuilder.setEnabledIntents(gatewayIntents);
        jdaBuilder.enableCache(enabledCacheFlags);
        jdaBuilder.disableCache(disabledCacheFlags);

        long millisStart1 = System.currentTimeMillis();

        jda = jdaBuilder.build().awaitReady();

        millisStart = System.currentTimeMillis();

        Logger.log(LogType.NONE, "------- Loading EasyCommands -------");
        Logger.log(LogType.LISTENERS, jda.getRegisteredListeners().toString());

        updateCommands();
        logCurrentExecutors();

        Logger.log(LogType.OK, "EasyCommands finished loading in " + ConsoleColors.GREEN_BOLD + (System.currentTimeMillis() - millisStart) + "ms" + ConsoleColors.GREEN + "\nTotal: " + ConsoleColors.GREEN_BOLD + (System.currentTimeMillis() - millisStart1) + "ms" + ConsoleColors.GREEN + ".");
        return jda;
    }

    private void loadIntents() {
        gatewayIntents.addAll(List.of(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS));
    }

    private void loadCacheFlags() {
        enabledCacheFlags.addAll(List.of(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE));
    }

    public List<GatewayIntent> getGatewayIntents() {
        return gatewayIntents;
    }

    public EasyCommands addGatewayIntents(GatewayIntent... intents) {
        this.getGatewayIntents().addAll(List.of(intents));
        return this;
    }

    public List<CacheFlag> getEnabledCacheFlags() {
        return enabledCacheFlags;
    }

    public EasyCommands addEnabledCacheFlags(CacheFlag... flags) {
        this.getEnabledCacheFlags().addAll(List.of(flags));
        return this;
    }

    public List<CacheFlag> getDisabledCacheFlags() {
        return disabledCacheFlags;
    }

    public void addDisabledCacheFlags(CacheFlag... flags) {
        this.getDisabledCacheFlags().addAll(List.of(flags));
    }

    public Map<String, IExecutor> getExecutors() { return executorMap; }

    public EasyCommands addExecutor(IExecutor... executors) {
        for (IExecutor executor : executors) {
            if(executor.getName() == null || executor.getName().isEmpty()) {
                Logger.log(LogType.WARNING, "Command: '" + executor.getClass().getSimpleName() + "' doesn't have a name and could cause errors.");
            }
            if(executor.getDescription() == null || executor.getDescription().isEmpty()) {
                Logger.log(LogType.WARNING, "Command: '" + executor.getClass().getName() + "' doesn't have a description.");
            }
            this.executorMap.put(executor.getName(), executor);
            if(executor.getAliases() != null && !executor.getAliases().isEmpty()) {
                for (String alias : executor.getAliases()) {
                    if(alias.isEmpty()) {
                        Logger.log(LogType.WARNING, "Alias: '" + executor.getClass().getSimpleName() + "' doesn't have a name and could cause errors.");
                    }
                    this.executorMap.put(alias, executor);
                }
            }
        }
        return this;
    }

    public EasyCommands clearExecutors() {
        this.executorMap.clear();
        return this;
    }

    /**
     * Used to debug executors. Serve to identify if the commands are registered to Discord correctly.
     */
    private void logCurrentExecutors() {

        List<Command> commands = jda.retrieveCommands().complete();
        Logger.log(LogType.EXECUTORS, ConsoleColors.BLUE_BOLD + "- Logging registered Executors");
        Logger.logNoType(ConsoleColors.BLUE_BOLD + "- [Slash]");
        for (Command command : commands) {
            Logger.logNoType("/" + command.getName() + ConsoleColors.RESET + ":" + ConsoleColors.CYAN + command.getId());
        }
    }

    /**
     * Updates all SlashExecutor to Discord Guild.
     */
    private void updateCommands() {
        List<CommandData> commands = new ArrayList<>();
        getExecutors().forEach((name, executor) -> {
            if(executor instanceof SlashExecutor) {
                SlashExecutor executor1 = (SlashExecutor) executor;
                executor1.updateOptions();
                commands.add(Commands.slash(name, executor1.getDescription()).addOptions(executor1.getOptions()));
            }
            executor.updateAliases();
            executor.updateAuthorizedChannels(jda);
            executor.updateAuthorizedRoles(jda);
        });
        jda.updateCommands().addCommands(commands).queue();
    }

    public EasyCommands registerListeners(ListenerAdapter... listeners) {

        if(List.of(listeners).isEmpty()) {
            return this;
        }

        for (Object listener : listeners) {
            jdaBuilder.addEventListeners(listener);
        }

        return this;
    }

    public JDA getJDA() {
        return jda;
    }

    public Logger getLogger() {
        return logger;
    }

}
