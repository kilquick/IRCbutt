package net.alureon.ircbutt.command;

import net.alureon.ircbutt.IRCbutt;
import net.alureon.ircbutt.command.commands.VimSearchReplaceCommand;
import net.alureon.ircbutt.command.commands.fact.FactCommand;
import net.alureon.ircbutt.game.GuessingGame;
import net.alureon.ircbutt.game.RegexGame;
import net.alureon.ircbutt.response.BotIntention;
import net.alureon.ircbutt.response.BotResponse;
import net.alureon.ircbutt.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main CommandHandler for the program.  Any and all commands are routed here and executed.
 */
public final class CommandHandler {

    /**
     * The instance of the IRCbutt object.
     */
    private IRCbutt butt;
    /**
     * The logger for this class.
     */
    private static final Logger log = LogManager.getLogger();
    /**
     * A mapping of what command maps to what class.
     */
    private HashMap<String, Command> commandMap = new HashMap<>();
    /**
     * This holds all the extra items that will populate More.  This cannot go into the MoreCommand
     * class because it's re-instantiated every time the command executes.
     */
    private final ArrayList<String> more = new ArrayList<>();


    /**
     * Constructor sets the field of the IRCbutt instance.
     *
     * @param butt The IRCbutt instance.
     */
    public CommandHandler(final IRCbutt butt) {
        this.butt = butt;
    }

    /**
     * Gets all classes that implement the Command interface and loads them, and their associated
     * command aliases into the command map.
     */
    public void registerCommandClasses() {
        log.info("Registering Commands...");
        Reflections reflections = new Reflections("net.alureon.ircbutt.command");
        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);
        for (Class<? extends Command> c : classes) {
            try {
                Class<? extends Command> commandClass = Class.forName(c.getName()).asSubclass(Command.class);
                Constructor<? extends Command> constructor = commandClass.getConstructor();
                Command command = constructor.newInstance();
                for (String alias : command.getCommandAliases()) {
                    commandMap.put(alias, command);
                    log.info("Registered command '" + alias + "' to " + command.getClass().getSimpleName());
                }
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                    | InstantiationException | InvocationTargetException e) {
                log.error("Failed to register command class: " + e.getMessage());
            }
        }
    }

    /**
     * The main function that handles all commands passed to the bot.
     *
     * @param event         The GenericMessageEvent received from the PircBotX API.
     * @param commandString The entire command the user has entered.
     * @return The bot's intended response in a BotResponse object.
     */
    public BotResponse handleCommand(final GenericMessageEvent event, final String commandString) {
        /* check for vim search and replace */
        Pattern p = Pattern.compile("s/.*/.*/g?");
        Matcher m = p.matcher(commandString.replaceFirst("!", ""));
        if (m.find()) {
            return new VimSearchReplaceCommand().executeCommand(butt, event, commandString.replaceFirst("!", ""));
        }

        /* Split the command on whitespace */
        String[] cmd = commandString.split("\\s");

        /* remove the '!' from the command */
        cmd[0] = cmd[0].replaceFirst("!", "");

        /* guessing game */
        if (butt.getGameManager().getGameActive() && (cmd[0].equals("fs") || cmd[0].equals("ff")
                || cmd[0].equals("factfind") || cmd[0].equals("factsearch"))) {
            return new BotResponse(BotIntention.CHAT, null,
                    "FactFind disabled while game is in session!  To search facts, end game with !endgame");
        }

        /* regex game */
        if (butt.getGameManager().getGameActive() && butt.getGameManager().getActiveGame() instanceof RegexGame) {
            RegexGame regexGame = (RegexGame) butt.getGameManager().getActiveGame();
            String regex = commandString.replaceFirst("!", "");
            Pattern p2 = Pattern.compile(regex);
            Matcher m2 = p2.matcher(regexGame.getShouldMatch());
            if (m2.find()) {
                Matcher m3 = p.matcher(regexGame.getShouldNotMatch());
                if (!m3.find()) {
                    butt.getGameManager().setActiveGame(null);
                    return new BotResponse(BotIntention.HIGHLIGHT, event.getUser(), "Nice job!");
                }
            }
        }

        /* Check command map and execute command */
        if (commandMap.containsKey(cmd[0])) {
            Command command = commandMap.get(cmd[0]);
            if (command.allowsCommandSubstitution()) {
                /* Perform command substitution */
                String commandSubstituted = parseCommandSubstitutionAndVariables(event, StringUtils.arrayToString(cmd));
                cmd = commandSubstituted.split(" ");
                log.debug("CommandSubstitutedArray: " + StringUtils.arrayToString(cmd));
            }
            return command.executeCommand(butt, event, cmd);
        } else {
            // check if the command is the answer to a game in session
            if (butt.getGameManager().getGameActive() && butt.getGameManager().getActiveGame() instanceof GuessingGame) {
                GuessingGame game = (GuessingGame) butt.getGameManager().getActiveGame();
                if (cmd[0].equals("~" + game.getCurrentMysteryFactName())) {
                    return game.givePlayerPoint(event.getUser().getNick());
                }
            }
            return new FactCommand().executeCommand(butt, event, cmd);
        }
    }

    /**
     * This function performs command substitution before actually executing a command.  Any commands
     * with the variable $USER is replaced with the nick of the person giving the command.  Any commands
     * surrounded by $() are expanded to what their value would be if the command was executed, by this
     * function.
     *
     * @param event The event from PircBotX.
     * @param input The input from the user.
     * @return a string with all commands expanded to their values.
     */
    private String parseCommandSubstitutionAndVariables(final GenericMessageEvent event, final String input) {
        String result = input;
        Pattern p = Pattern.compile("\\$\\([^)].*\\)");
        Matcher m = p.matcher(input);
        while (m.find()) {
            String command = m.group().substring(2, m.group().length() - 1);
            BotResponse response = handleCommand(event, command);
            result = input.replaceFirst(Pattern.quote(m.group()), response.getMessage());
        }
        log.debug("Parsed Command Substitution result: " + result);
        return result.replaceAll("\\$USER", event.getUser().getNick());
    }

    /**
     * Returns the list of more, which is a list that contains all overflow items that won't fit
     * in a single bot message.  For example, a Google search may have more results.  The user can
     * then execute !more to get another result.  The results are stored in this list.
     * @return the list loaded with extra results for any command
     */
    public ArrayList<String> getMoreList() {
        return this.more;
    }

    /**
     * Adds a String object to the More list.
     * @param moreItem The string to add to the More list.
     */
    public void addMore(final String moreItem) {
        this.more.add(moreItem);
    }

    /**
     * Clear the more list.  This is important to do before populating More, otherwise you will
     * have elements from a different query in your list.
     */
    public void clearMore() {
        this.more.clear();
    }

}
