package net.alureon.ircbutt.command;

import jdk.internal.joptsimple.internal.Strings;
import net.alureon.ircbutt.command.commands.fact.FactCommand;
import net.alureon.ircbutt.command.commands.quotegrabs.QuoteGrabCommand;
import net.alureon.ircbutt.response.BotIntention;
import net.alureon.ircbutt.response.BotResponse;
import net.alureon.ircbutt.IRCbutt;
import net.alureon.ircbutt.command.commands.*;
import net.alureon.ircbutt.command.commands.google.GoogleImageSearchCommand;
import net.alureon.ircbutt.command.commands.google.GoogleSearchCommand;
import net.alureon.ircbutt.command.commands.karma.KarmaCommand;
import net.alureon.ircbutt.command.commands.EvalCommand;
import net.alureon.ircbutt.math.MathLib;
import net.alureon.ircbutt.util.MathUtils;
import net.alureon.ircbutt.util.StringUtils;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.reflections.Reflections;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main CommandHandler for the program.  Any and all commands are routed here, then
 * further routed once the command's type is deduced.
 */
public class CommandHandler {

    /**
     * The instance of the IRCbutt object.
     */
    private IRCbutt butt;

    /**
     * Constructor sets the field of the IRCbutt instance.
     * @param butt The IRCbutt instance.
     */
    public CommandHandler(final IRCbutt butt) {
        this.butt = butt;
    }

    /**
     * Gets all classes that implement the Command interface.
     * @return Set of all classes implementing Command.
     */
    public static Set<Class> getCommandClasses() {
        Reflections reflections = new Reflections("net.alureon.ircbutt");
        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);
        for (Class c : classes) {
            System.out.println(c.getName());
        }
        return null;
    }

    /**
     * The main function that handles all commands passed to the bot.
     * @param event The GenericMessageEvent received from the PircBotX API.
     * @param commandString The entire command the user has entered.
     * @return The bot's intended response in a BotResponse object.
     */
    public BotResponse handleCommand(final GenericMessageEvent event, final String commandString) {
        /* Split the command on whitespace */
        String[] cmd = commandString.split("\\s");

        /* if it's prefixed with a tilde it's a fact request */
        if (cmd[0].startsWith("~")) {
            return new FactCommand().executeCommand(butt, event, cmd);
        }

        /* remove the '!' from the command */
        cmd[0] = cmd[0].replaceFirst("!", "");

        if (!cmd[0].equals("learn")) {
            String commandSubstituted = parseCommandSubstitutionAndVariables(butt, event,
                    StringUtils.arrayToString(cmd));
            String[] commandSubstitutedArray = commandSubstituted.split(" ");

            /* switch of main bot commands */
            switch (cmd[0]) {
                case "rq":
                case "grab":
                case "q":
                case "qinfo":
                case "qi":
                case "qsay":
                case "qsearch":
                case "qfind":
                case "qf":
                case "rqnouser":
                case "rqn":
                    return new QuoteGrabCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "append":
                case "forget":
                case "fact":
                case "factinfo":
                case "factfind":
                case "factsearch":
                case "ffind":
                case "fsearch":
                case "finfo":
                case "ff":
                case "fi":
                case "fs":
                    return new FactCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "echo":
                    return new BotResponse(BotIntention.CHAT, null,
                            StringUtils.getArgs(commandSubstitutedArray));
                    break;
                case "g":
                    return new GoogleSearchCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "give":
                    return new GiveCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "rot13":
                case "rot":
                    return new Rot13Command().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "yt":
                    return new YouTubeCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "ud":
                    return new UrbanDictionaryCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "version":
                    // TODO should be implemented its own class
                    return new BotResponse(BotIntention.CHAT, null, butt.getProgramVersion());
                    break;
                case "dice":
                    return new DiceCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "random":
                    // TODO should be implemented in its own class.
                    return new BotResponse(BotIntention.CHAT, null,
                            String.valueOf(MathUtils.getRandom(0, 10000)));
                    break;
                case "sqrt":
                case "pow":
                    MathLib.handleMath(response, commandSubstitutedArray);
                    break;
                case "check":
                    return new CheckCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "define":
                    return new DefineCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "invite":
                    return new InviteCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "more":
                    return new MoreCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "wr":
                    return new WakeRoomCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "eval":
                    return new EvalCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "karma":
                    return new KarmaCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "coin":
                    return new CoinCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "gi":
                    return new GoogleImageSearchCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "8":
                case "8ball":
                    return new MagicEightBallCommand().executeCommand(butt, event, commandSubstitutedArray);
                    break;
                case "butt":
                case "buttify":
                    //TODO needs its own class implementation
                    String buttified =
                            butt.getButtReplaceHandler().buttFormat(StringUtils.getArgs(commandSubstitutedArray));
                    return new BotResponse(BotIntention.CHAT, null, buttified);
                    break;
                default:
                    break;
            }
        } else {
            return new FactCommand().executeCommand(butt, event, cmd);
        }
    }

    /**
     * This function performs command substitution before actually executing a command.  Any commands
     * with the variable $USER is replaced with the nick of the person giving the command.  Any commands
     * surrounded by $() are expanded to what their value would be if the command was executed, by this
     * function.
     * @param butt The IRCbutt singleton.
     * @param event The event from PircBotX.
     * @param input The input from the user.
     * @return a string with all commands expanded to their values.
     */
    private static String parseCommandSubstitutionAndVariables(final IRCbutt butt, final GenericMessageEvent event,
                                                               final String input) {
        String result = Strings.EMPTY;
        Pattern p = Pattern.compile("\\$\\([^)]*\\)");
        Matcher m = p.matcher(input);
        while (m.find()) {
            String command = m.group().substring(2, m.group().length() - 1);
            BotResponse response = butt.getCommandHandler().handleCommand(event, command);
            result = input.replaceFirst(Pattern.quote(m.group()), response.toString());
        }
        return result.replaceAll("\\$USER", event.getUser().getNick());
    }

}
