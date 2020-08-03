package won.bot.valueflows.action;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.atomlifecycle.AbstractCreateAtomAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.valueflows.context.ValueFlowsBotContextWrapper;
import won.bot.valueflows.event.CreateHelperAtomEvent;
import won.bot.valueflows.util.HelperAtomModelWrapper;
import won.protocol.message.WonMessage;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

public class CreateHelperAtomAction extends AbstractCreateAtomAction {
    private static final Logger logger = LoggerFactory.getLogger(CreateHelperAtomAction.class);

    public CreateHelperAtomAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if(!(ctx.getBotContextWrapper() instanceof ValueFlowsBotContextWrapper) || !(event instanceof CreateHelperAtomEvent)) {
            logger.error("CreateHelperAtomAction does not work without a ValueFlowsBotContextWrapper and CreateHelperAtomEvent");
            throw new IllegalStateException(
                    "CreateHelperAtomAction does not work without a ValueFlowsBotContextWrapper and CreateHelperAtomEvent");
        }

        ValueFlowsBotContextWrapper botContextWrapper = (ValueFlowsBotContextWrapper) ctx.getBotContextWrapper();
        CreateHelperAtomEvent createHelperAtomEvent = (CreateHelperAtomEvent) event;

        final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
        WonNodeInformationService wonNodeInformationService = ctx.getWonNodeInformationService();
        final URI helperAtomURI = wonNodeInformationService.generateAtomURI(wonNodeUri);
        Dataset dataset = new HelperAtomModelWrapper(helperAtomURI, createHelperAtomEvent.getAtomURI()).copyDataset();
        logger.debug("creating atom on won node {} with content {} ", wonNodeUri,
                StringUtils.abbreviate(RdfUtils.toString(dataset), 150));
        WonMessage createAtomMessage = ctx.getWonMessageSender().prepareMessage(createWonMessage(helperAtomURI, dataset));
        botContextWrapper.rememberAtomUri(helperAtomURI);
        EventBus bus = ctx.getEventBus();
        EventListener successCallback = event1 -> {
            logger.debug("atom creation successful, new atom URI is {}", helperAtomURI);
            bus.publish(new AtomCreatedEvent(helperAtomURI, wonNodeUri, dataset, null));
            botContextWrapper.addHelperAtomUriEntry(createHelperAtomEvent.getAtomURI(), helperAtomURI);
        };
        EventListener failureCallback = event12 -> {
            String textMessage = WonRdfUtils.MessageUtils
                    .getTextMessage(((FailureResponseEvent) event12).getFailureMessage());
            logger.error("atom creation failed for atom URI {}, original message URI {}: {}", helperAtomURI,
                    ((FailureResponseEvent) event12).getOriginalMessageURI(), textMessage);
            botContextWrapper.removeAtomUri(helperAtomURI);
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(createAtomMessage, successCallback, failureCallback, ctx);
        logger.debug("registered listeners for response to message URI {}", createAtomMessage.getMessageURI());
        ctx.getWonMessageSender().sendMessage(createAtomMessage);
        logger.debug("atom creation message sent with message URI {}", createAtomMessage.getMessageURI());
    }
}
