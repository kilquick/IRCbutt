package net.alureon.ircbutt.command.commands.fact;

import net.alureon.ircbutt.IRCbutt;
import net.alureon.ircbutt.command.Command;
import net.alureon.ircbutt.response.BotIntention;
import net.alureon.ircbutt.response.BotResponse;
import net.alureon.ircbutt.util.IRCUtils;
import net.alureon.ircbutt.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pircbotx.User;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Provides all functionality for dealing with facts.  The bot's knowledge functionality
 * works by using a ~ followed immediately by the name of the fact (no space).
 */
public final class FactCommand implements Command {


    /**
     * The logger for this class.
     */
    private static final Logger log = LogManager.getLogger(FactCommand.class);
    /**
     * The max possible fact size we will store in the database.
     */
    private static final int MAX_FACT_SIZE = 500;
    /**
     * When doing argument parsing, this is the largest argument that will be parsed.
     * That means a fact could have $1, $2, $3 ... $10.  That seems reasonable.
     */
    private static final int MAX_ARG_NUM = 10;

    /**
     * Adds new knowledge to the database.
     *
     * @param butt The IRCbutt instance for getting the database.
     * @param user The user who wants to store the data.
     * @param data The fact to add.
     * @return The bot's response to the action.
     */
    private BotResponse addKnowledge(final IRCbutt butt, final User user, final String[] data) {
        if (data.length > 2) {
            String command = StringUtils.getArgs(data);
            String[] split = command.split(" ", 2);
            if (split[0].endsWith(":")) {
                split[0] = split[0].replace(":", "");
            } // above is the colon method (which I happen to prefer) !learn fact: thing
            String item = split[0].substring(0, split[0].length()).trim();
            if (getFact(butt, item) == null) {
                String information = StringUtils.getArgsOverOne(data);
                if (information.length() > MAX_FACT_SIZE) {
                    return new BotResponse(BotIntention.HIGHLIGHT, user, "fact longer than 500 characters");
                }
                log.trace("Item: " + item);
                log.trace("Data: " + information);
                log.trace("User: " + user.getNick());
                butt.getFactTable().insertKnowledge(item, information, user.getNick());
                return new BotResponse(BotIntention.HIGHLIGHT, user, "ok got it!");
            } else {
                return new BotResponse(BotIntention.HIGHLIGHT, user, butt.getYamlConfigurationFile().getBotNickName()
                        + " already know about " + item);
            }
        }
        return new BotResponse(BotIntention.HIGHLIGHT, user, "!learn key: value");
    }

    /**
     * Appends data to an existing fact.
     *
     * @param butt The IRCbutt instance for accessing the database.
     * @param user The user who is appending the data.
     * @param data The data to append to the fact.
     * @return the bot's response to the event.
     * // TODO this should pass in the fact name and the data, not just one string.  We're parsing the data repeatedly.
     */
    private BotResponse appendToKnowledge(final IRCbutt butt, final User user, final String[] data) {
        if (data.length > 2) {
            String command = StringUtils.getArgs(data);
            String[] split = command.split(" ", 2);
            if (split[0].endsWith(":")) {
                split[0] = split[0].replace(":", "");
            } //backwards compatibility
            String item = split[0].substring(0, split[0].length()).trim();
            if (getFact(butt, item) != null) {
                String information = StringUtils.getArgsOverOne(data);
                if (information.length() > MAX_FACT_SIZE) {
                    return new BotResponse(BotIntention.HIGHLIGHT, user, "fact longer than 500 characters");
                }
                log.trace("Item: " + item);
                log.trace("Data: " + information);
                butt.getFactTable().appendKnowledge(item, information);
                return new BotResponse(BotIntention.HIGHLIGHT, user, "ok got it!");
            } else {
                return new BotResponse(BotIntention.HIGHLIGHT, user, butt.getYamlConfigurationFile().getBotNickName()
                        + " don't know nothin bout " + item);
            }
        }
        return new BotResponse(BotIntention.HIGHLIGHT, user, "!append key: value");
    }

    /**
     * Retrieves a fact from the database, or if it doesn't exist, returns null.
     * This function also parses arguments from a fact.
     *
     * @param butt The IRCbutt instance for accessing the database.
     * @param cmd The array of command data.
     * @return The resulting data, or null if the fact doesn't exist.
     */
    private String getFactParseArgs(final IRCbutt butt, final String[] cmd) {
        log.debug("Parsing args for following fact request: " + cmd[0]);
        if (!cmd[0].isEmpty()) {
            String result = butt.getFactTable().queryKnowledge(cmd[0]);
            log.debug("Got result from database: " + result);
            if (result != null) {
                if (result.contains("$1")) {
                    log.debug("Detected argument...parsing...");
                    result = parseArguments(result, StringUtils.getArgs(cmd));
                }
                return result;
            }
        }
        return null;
    }

    /**
     * Retrieves a fact from the database, or if it doesn't exist, returns null.
     * @param butt The IRCbutt instance for accessing the database.
     * @param item The item to get from the database.
     * @return The resulting data, or null if the fact doesn't exist.
     */
    private String getFact(final IRCbutt butt, final String item) {
        if (!item.isEmpty()) {
            return butt.getFactTable().queryKnowledge(item);
        }
        return null;
    }

    /**
     * Parses arguments to the fact request much like the BASH shell does.
     * @param fact The fact received from the database.
     * @param arguments The arguments given with the fact request.
     * @return The fact string with all arguments parsed and replaced.
     */
   private String parseArguments(final String fact, final String arguments) {
        String[] args = arguments.split("\\s");
        log.debug("Received the following arguments: " + arguments);
        String result = fact;
        for (int i = 0; i <= MAX_ARG_NUM; i++) {
            if (args.length > i) {
                result = result.replace("$" + String.valueOf(i + 1), args[i]);
            }
        }
        log.debug("final result is: " + result);
        return result;
   }

    /**
     * Removes knowledge from the database.
     *
     * @param butt The IRCbutt instance for accessing the database.
     * @param item The key to remove from the database (the fact name).
     * @return True if removed, false if not.
     */
    private boolean removeKnowledge(final IRCbutt butt, final String[] item) {
        return item.length > 0 && butt.getFactTable().deleteKnowledge(StringUtils.getArgs(item));
    }

    @Override
    public BotResponse executeCommand(final IRCbutt butt, final GenericMessageEvent event, final String[] cmd) {
        log.trace("FactCommand received the following command: " + StringUtils.arrayToString(cmd));
        if (cmd[0].equals("learn")) {
            if (butt.getYamlConfigurationFile().getBotNoVerify() || event.getUser().isVerified()) {
                return addKnowledge(butt, event.getUser(), cmd);
            }
        } else if (cmd[0].equals("append")) {
            if (butt.getYamlConfigurationFile().getBotNoVerify() || event.getUser().isVerified()) {
                return appendToKnowledge(butt, event.getUser(), cmd);
            }
        } else if (cmd[0].equals("forget")) {
            if (IRCUtils.isOpInBotChannel(butt, event.getUser())) {
                if (cmd.length == 2) {
                    String old = getFact(butt, cmd[1]);
                    if (removeKnowledge(butt, cmd)) {
                        // log to console in event of accidental data loss
                        log.info("Removed fact [" + cmd[1] + "]: " + old);
                        return new BotResponse(BotIntention.HIGHLIGHT, event.getUser(),
                                "ok " + butt.getYamlConfigurationFile().getBotNickName()
                                        + " wont member that no more");
                    } else {
                        return new BotResponse(BotIntention.HIGHLIGHT, event.getUser(),
                                butt.getYamlConfigurationFile().getBotNickName() + " don't know nothin bout " + cmd[1]);
                    }
                } else {
                    return new BotResponse(BotIntention.HIGHLIGHT, event.getUser(),
                            "!forget <factname>");
                }
            } else {
                log.trace(event.getUser().getNick() + " is not a channel op");
                return new BotResponse(BotIntention.NO_REPLY, null, null);
            }
        } else if (cmd[0].startsWith("~")) {
            cmd[0] = cmd[0].replaceFirst("~", "");
            String info = getFactParseArgs(butt, cmd);
            return getFactResponse(info, event);
        } else if (cmd[0].equals("fact")) {
            String info = butt.getFactTable().getRandomData();
            if (info != null) {
                return new BotResponse(BotIntention.CHAT, null, info);
            } else {
                // the only way this should happen is if the bot doesn't know any facts
                return new BotResponse(BotIntention.HIGHLIGHT, event.getUser(),
                        butt.getYamlConfigurationFile().getBotNickName() + " dont know any facts yet!");
            }
        } else if (cmd[0].equals("factinfo") || cmd[0].equals("finfo") || cmd[0].equals("fi")) {
            String info = butt.getFactTable().getFactInfo(StringUtils.getArgs(cmd));
            return getBotResponseForQuery(butt, info);
        } else if (cmd[0].equalsIgnoreCase("factfind") || cmd[0].equalsIgnoreCase("factsearch")
                || cmd[0].equalsIgnoreCase("fsearch") || cmd[0].equalsIgnoreCase("ffind")
                || cmd[0].equals("ff") || cmd[0].equals("fs")) {
            String info = butt.getFactTable().findFact(StringUtils.getArgs(cmd));
            return getBotResponseForQuery(butt, info);
        } else {
            String info = getFactParseArgs(butt, cmd);
            return getFactResponse(info, event);
        }
        log.error("Fell through the entire switch at FactCommand without hitting a branch.");
        log.error("Received: " + StringUtils.arrayToString(cmd));
        return new BotResponse(BotIntention.NO_REPLY, null, null);
    }

    /**
     * A helper method for getting a response from a fact query operation.
     * @param info The potentially null string that contains fact information.
     * @param event The GenericMessageEvent, for access to the user's nickname.
     * @return The bot's response to the fact request event.
     */
    private BotResponse getFactResponse(final String info, final GenericMessageEvent event) {
        String result;
        if (info != null) {
            result = info.replaceAll("\\$USER", event.getUser().getNick());
            if (info.startsWith("$ME")) {
                result = result.replaceFirst("\\$ME", "");
                return new BotResponse(BotIntention.ME, null, result);
            }
            return new BotResponse(BotIntention.CHAT, null, result);
        } else {
            return new BotResponse(BotIntention.NO_REPLY, null, null);
        }
    }

    /**
     * A helper method from retrieving fact data from the database.
     *
     * @param butt The IRCbutt instance for getting the bot's nickname.
     * @param info The potentially null info from the database.
     * @return the bot's response to the query.
     */
    private BotResponse getBotResponseForQuery(final IRCbutt butt, final String info) {
        if (info != null) {
            return new BotResponse(BotIntention.CHAT, null, info);
        } else {
            return new BotResponse(BotIntention.CHAT, null,
                    butt.getYamlConfigurationFile().getBotNickName() + " find nothing");
        }
    }

    @Override
    public ArrayList<String> getCommandAliases() {
        return new ArrayList<>(Arrays.asList("ff", "fs", "fact", "forget", "factinfo", "finfo", "fi", "factfind",
                "factsearch", "fsearch", "ffind", "learn", "append"));
    }

    @Override
    public boolean allowsCommandSubstitution() {
        return false;
    }
}
