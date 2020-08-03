package won.bot.valueflows.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.extensions.matcher.MatcherExtensionAtomCreatedEvent;
import won.bot.valueflows.context.ValueFlowsBotContextWrapper;
import won.bot.valueflows.event.CreateHelperAtomEvent;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONCON;
import won.protocol.vocabulary.WXVALUEFLOWS;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Map;
import java.util.Set;

public class MatcherExtensionAtomCreatedAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public MatcherExtensionAtomCreatedAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (!(event instanceof MatcherExtensionAtomCreatedEvent) || !(getEventListenerContext().getBotContextWrapper() instanceof ValueFlowsBotContextWrapper)) {
            logger.error("MatcherExtensionAtomCreatedAction can only handle MatcherExtensionAtomCreatedEvent and only works with SkeletonBotContextWrapper");
            return;
        }
        ValueFlowsBotContextWrapper botContextWrapper = (ValueFlowsBotContextWrapper) ctx.getBotContextWrapper();
        MatcherExtensionAtomCreatedEvent atomCreatedEvent = (MatcherExtensionAtomCreatedEvent) event;

        URI createdAtomUri = atomCreatedEvent.getAtomURI();
        DefaultAtomModelWrapper createdAtom = new DefaultAtomModelWrapper(atomCreatedEvent.getAtomData());

        if(botContextWrapper.getHelperAtomEntries().containsValue(atomCreatedEvent.getAtomURI())) {
            logger.debug("Created Atom is a known Helper Atom" + atomCreatedEvent.getAtomURI());
        } else {

            if (createdAtom.getContentTypes().contains(URI.create(WON.Persona.getURI()))) {
                Map<URI, URI> socketTypeUriMap = createdAtom.getSocketTypeUriMap();
                if (socketTypeUriMap.containsValue(WXVALUEFLOWS.PrimaryAccountableSocket.asURI()) || socketTypeUriMap.containsValue(WXVALUEFLOWS.CustodianOfSocket.asURI()) || socketTypeUriMap.containsValue(WXVALUEFLOWS.ActorActivitySocket.asURI())) {
                    URI helperAtomUri = botContextWrapper.getHelperAtomUri(createdAtomUri);

                    if (helperAtomUri == null) {
                        logger.debug("Create a Helper Atom for atomUri: " + createdAtomUri);
                        ctx.getEventBus().publish(new CreateHelperAtomEvent(createdAtomUri));
                    } else {
                        logger.debug("Helper Atom Already exists: " + createdAtomUri + " HelperAtomUri: " + helperAtomUri);
                    }
                }
            }
        }

        Map<URI, Set<URI>> connectedSocketsMapSet = botContextWrapper.getServiceAtomConnectedSockets();

        for (Map.Entry<URI, Set<URI>> entry : connectedSocketsMapSet.entrySet()) {
            URI senderSocket = entry.getKey();
            Set<URI> targetSocketsSet = entry.getValue();
            for (URI targetSocket : targetSocketsSet) {
                WonMessage wonMessage = WonMessageBuilder
                        .connectionMessage()
                        .sockets()
                        .sender(senderSocket)
                        .recipient(targetSocket)
                        .content()
                        .text("We registered that an Atom was created")
                        .content()
                        .addToMessageResource(WONCON.suggestedAtom, createdAtomUri)
                        .build();

                ctx.getWonMessageSender().prepareAndSendMessage(wonMessage);
            }
        }
    }
}
