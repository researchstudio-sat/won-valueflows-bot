package won.bot.valueflows.impl;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.behaviour.ExecuteWonMessageCommandBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.filter.impl.SocketTypeFilter;
import won.bot.framework.eventbot.filter.impl.AndFilter;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.filter.impl.NotFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.bot.framework.extensions.matcher.MatcherBehaviour;
import won.bot.framework.extensions.matcher.MatcherExtension;
import won.bot.framework.extensions.matcher.MatcherExtensionAtomCreatedEvent;
import won.bot.framework.extensions.serviceatom.ServiceAtomBehaviour;
import won.bot.framework.extensions.serviceatom.ServiceAtomContent;
import won.bot.framework.extensions.serviceatom.ServiceAtomExtension;
import won.bot.framework.extensions.textmessagecommand.TextMessageCommandBehaviour;
import won.bot.framework.extensions.textmessagecommand.TextMessageCommandExtension;
import won.bot.framework.extensions.textmessagecommand.command.TextMessageCommand;

import won.bot.valueflows.action.CreateHelperAtomAction;
import won.bot.valueflows.action.MatcherExtensionAtomCreatedAction;
import won.bot.valueflows.context.ValueFlowsBotContextWrapper;
import won.bot.valueflows.event.CreateHelperAtomEvent;
import won.protocol.model.SocketType;

public class ValueFlowsBot extends EventBot implements MatcherExtension, TextMessageCommandExtension, ServiceAtomExtension {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private int registrationMatcherRetryInterval;
    private MatcherBehaviour matcherBehaviour;
    private ServiceAtomBehaviour serviceAtomBehaviour;
    private TextMessageCommandBehaviour textMessageCommandBehaviour;

    // bean setter, used by spring
    public void setRegistrationMatcherRetryInterval(final int registrationMatcherRetryInterval) {
        this.registrationMatcherRetryInterval = registrationMatcherRetryInterval;
    }

    @Override
    public ServiceAtomBehaviour getServiceAtomBehaviour() {
        return serviceAtomBehaviour;
    }

    @Override
    public MatcherBehaviour getMatcherBehaviour() {
        return matcherBehaviour;
    }

    @Override
    public TextMessageCommandBehaviour getTextMessageCommandBehaviour() {
        return textMessageCommandBehaviour;
    }

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        if (!(getBotContextWrapper() instanceof ValueFlowsBotContextWrapper)) {
            logger.error(getBotContextWrapper().getBotName() + " does not work without a ValueFlowsBotContextWrapper");
            throw new IllegalStateException(
                            getBotContextWrapper().getBotName() + " does not work without a ValueFlowsBotContextWrapper");
        }
        EventBus bus = getEventBus();
        ValueFlowsBotContextWrapper botContextWrapper = (ValueFlowsBotContextWrapper) getBotContextWrapper();
        // register listeners for event.impl.command events used to tell the bot to send
        // messages
        ExecuteWonMessageCommandBehaviour wonMessageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);
        wonMessageCommandBehaviour.activate();

        ServiceAtomContent serviceAtomContent = new ServiceAtomContent("ValueFlows Helper Service");
        serviceAtomContent.setDescription("This Bot is used to do something with ValueFlows and resources or whatever...");
        serviceAtomContent.setTags(Arrays.asList("Bot", "Service"));

        // activate ServiceAtomBehaviour
        serviceAtomBehaviour = new ServiceAtomBehaviour(ctx, serviceAtomContent);
        serviceAtomBehaviour.activate();

        EventFilter serviceAtomFilter = getServiceAtomFilter();
        SocketTypeFilter chatSocketFilter = new SocketTypeFilter(ctx, SocketType.ChatSocket.getURI());

        // Add TextMessageCommands for the ServiceAtom (hence serviceAtomFilter)
        ArrayList<TextMessageCommand> botCommands = new ArrayList<>();
        textMessageCommandBehaviour = new TextMessageCommandBehaviour(ctx, new AndFilter(serviceAtomFilter, chatSocketFilter), botCommands.toArray(new TextMessageCommand[0]));
        // activate TextMessageCommandBehaviour
        textMessageCommandBehaviour.activate();

        // set up matching extension
        // as this is an extension, it can be activated and deactivated as needed
        // if activated, a MatcherExtensionAtomCreatedEvent is sent every time a new
        // atom is created on a monitored node
        matcherBehaviour = new MatcherBehaviour(ctx, "BotSkeletonMatchingExtension", registrationMatcherRetryInterval);
        matcherBehaviour.activate();
        // filter to prevent reacting to serviceAtom<->ownedAtom events;
        NotFilter noInternalServiceAtomEventFilter = getNoInternalServiceAtomEventFilter();

        bus.subscribe(CreateHelperAtomEvent.class, new CreateHelperAtomAction(ctx));

        bus.subscribe(ConnectFromOtherAtomEvent.class, new AndFilter(noInternalServiceAtomEventFilter, serviceAtomFilter, chatSocketFilter), new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener) {
                EventListenerContext ctx = getEventListenerContext();
                ConnectFromOtherAtomEvent connectFromOtherAtomEvent = (ConnectFromOtherAtomEvent) event;
                try {
                    String message = "Hello i am the ValueFlowsBot i will send you a message everytime an atom is created...";
                    final ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(
                                    connectFromOtherAtomEvent.getRecipientSocket(),
                                    connectFromOtherAtomEvent.getSenderSocket(), message);
                    ctx.getEventBus().subscribe(ConnectCommandSuccessEvent.class, new ActionOnFirstEventListener(ctx,
                                    new CommandResultFilter(connectCommandEvent), new BaseEventBotAction(ctx) {
                                        @Override
                                        protected void doRun(Event event, EventListener executingListener) {

                                            ConnectCommandResultEvent connectionMessageCommandResultEvent = (ConnectCommandResultEvent) event;
                                            if (!connectionMessageCommandResultEvent.isSuccess()) {
                                                logger.error("Failure when trying to open a received Request: "
                                                                + connectionMessageCommandResultEvent.getMessage());
                                            } else {
                                                logger.info(
                                                                "Add an established connection " +
                                                                                connectCommandEvent.getLocalSocket()
                                                                                + " -> "
                                                                                + connectCommandEvent.getTargetSocket()
                                                                                +
                                                                                " to the botcontext ");
                                                botContextWrapper.addServiceAtomConnectedSocket(
                                                                connectCommandEvent.getLocalSocket(),
                                                                connectCommandEvent.getTargetSocket());
                                            }
                                        }
                                    }));
                    ctx.getEventBus().publish(connectCommandEvent);
                } catch (Exception te) {
                    logger.error(te.getMessage(), te);
                }
            }
        });

        bus.subscribe(HintFromMatcherEvent.class, new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener) throws Exception {
                EventListenerContext ctx = getEventListenerContext();
                if(!(ctx.getBotContextWrapper() instanceof ValueFlowsBotContextWrapper) || !(event instanceof HintFromMatcherEvent)) {
                    logger.error("CreateHelperAtomAction does not work without a ValueFlowsBotContextWrapper and CreateHelperAtomEvent");
                    throw new IllegalStateException(
                            "CreateHelperAtomAction does not work without a ValueFlowsBotContextWrapper and CreateHelperAtomEvent");
                }

                ValueFlowsBotContextWrapper botContextWrapper = (ValueFlowsBotContextWrapper) ctx.getBotContextWrapper();
                HintFromMatcherEvent hintFromMatcherEvent = (HintFromMatcherEvent) event;
                logger.debug("hintFromMatcherEvent: ", hintFromMatcherEvent);
            }
        });

        bus.subscribe(ConnectFromOtherAtomEvent.class, new AndFilter(noInternalServiceAtomEventFilter, new NotFilter(serviceAtomFilter), new NotFilter(chatSocketFilter)), new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener) {
                EventListenerContext ctx = getEventListenerContext();
                ConnectFromOtherAtomEvent connectFromOtherAtomEvent = (ConnectFromOtherAtomEvent) event;
                try {
                    // TODO: OTHER CONNECT STUFF
                    /*String message = "Hello i am the ValueFlowsBot i will send you a message everytime an atom is created...";
                    final ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(
                            connectFromOtherAtomEvent.getRecipientSocket(),
                            connectFromOtherAtomEvent.getSenderSocket(), message);
                    ctx.getEventBus().subscribe(ConnectCommandSuccessEvent.class, new ActionOnFirstEventListener(ctx,
                            new CommandResultFilter(connectCommandEvent), new BaseEventBotAction(ctx) {
                        @Override
                        protected void doRun(Event event, EventListener executingListener) {

                            ConnectCommandResultEvent connectionMessageCommandResultEvent = (ConnectCommandResultEvent) event;
                            if (!connectionMessageCommandResultEvent.isSuccess()) {
                                logger.error("Failure when trying to open a received Request: "
                                        + connectionMessageCommandResultEvent.getMessage());
                            } else {
                                logger.info(
                                        "Add an established connection " +
                                                connectCommandEvent.getLocalSocket()
                                                + " -> "
                                                + connectCommandEvent.getTargetSocket()
                                                +
                                                " to the botcontext ");
                                botContextWrapper.addConnectedSocket(
                                        connectCommandEvent.getLocalSocket(),
                                        connectCommandEvent.getTargetSocket());
                            }
                        }
                    }));
                    ctx.getEventBus().publish(connectCommandEvent);*/
                } catch (Exception te) {
                    logger.error(te.getMessage(), te);
                }
            }
        });

        // listen for the MatcherExtensionAtomCreatedEvent
        bus.subscribe(MatcherExtensionAtomCreatedEvent.class, new MatcherExtensionAtomCreatedAction(ctx));
        bus.subscribe(CloseFromOtherAtomEvent.class, new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener) {
                EventListenerContext ctx = getEventListenerContext();
                CloseFromOtherAtomEvent closeFromOtherAtomEvent = (CloseFromOtherAtomEvent) event;
                URI targetSocketUri = closeFromOtherAtomEvent.getSocketURI();
                URI senderSocketUri = closeFromOtherAtomEvent.getTargetSocketURI();
                logger.info("Remove a closed connection " + senderSocketUri + " -> " + targetSocketUri
                                + " from the botcontext ");
                botContextWrapper.removeConnectedSocket(senderSocketUri, targetSocketUri);
                botContextWrapper.removeServiceAtomConnectedSocket(senderSocketUri, targetSocketUri);
            }
        });
    }


}
