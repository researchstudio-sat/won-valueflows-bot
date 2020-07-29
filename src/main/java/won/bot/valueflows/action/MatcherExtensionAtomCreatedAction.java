package won.bot.valueflows.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.extensions.matcher.MatcherExtensionAtomCreatedEvent;
import won.bot.valueflows.context.ValueFlowsBotContextWrapper;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.vocabulary.WONCON;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Map;
import java.util.Set;

public class MatcherExtensionAtomCreatedAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public MatcherExtensionAtomCreatedAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if(!(event instanceof MatcherExtensionAtomCreatedEvent) || !(getEventListenerContext().getBotContextWrapper() instanceof ValueFlowsBotContextWrapper)) {
            logger.error("MatcherExtensionAtomCreatedAction can only handle MatcherExtensionAtomCreatedEvent and only works with SkeletonBotContextWrapper");
            return;
        }
        ValueFlowsBotContextWrapper botContextWrapper = (ValueFlowsBotContextWrapper) ctx.getBotContextWrapper();
        MatcherExtensionAtomCreatedEvent atomCreatedEvent = (MatcherExtensionAtomCreatedEvent) event;

        Map<URI, Set<URI>> connectedSocketsMapSet = botContextWrapper.getConnectedSockets();

        for(Map.Entry<URI, Set<URI>> entry : connectedSocketsMapSet.entrySet()) {
            URI senderSocket = entry.getKey();
            Set<URI> targetSocketsSet = entry.getValue();
            for(URI targetSocket : targetSocketsSet) {
                WonMessage wonMessage = WonMessageBuilder
                                            .connectionMessage()
                                            .sockets()
                                            .sender(senderSocket)
                                            .recipient(targetSocket)
                                            .content()
                                            .text("We registered that an Atom was created")
                                            .content()
                                            .addToMessageResource(WONCON.suggestedAtom, atomCreatedEvent.getAtomURI())
                                            .build();

                ctx.getWonMessageSender().prepareAndSendMessage(wonMessage);
            }
        }
    }
}
