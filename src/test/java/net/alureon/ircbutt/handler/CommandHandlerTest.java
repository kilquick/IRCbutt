package net.alureon.ircbutt.handler;

import net.alureon.ircbutt.command.CommandHandler;
import org.junit.Test;

/**
 * A class to hold tests for the CommandHandler.
 */
public class CommandHandlerTest {

    /*
    @Test
    public void testEchoCommand() {
        GenericMessageEvent event = mock(GenericMessageEvent.class);
        User user = mock(User.class);
        when(event.getUser()).thenReturn(user);
        when(event.getUser().getNick()).thenReturn("test");
        String input = "echo this is a $USER";
        BotResponse response = new CommandHandler(null).handleCommand(event, input);
        Assert.assertEquals("this is a test", response.getMessage());
    }
    */

    @Test
    public void testRegisteringCommands() {
        new CommandHandler(null).registerCommandClasses();
    }

}
