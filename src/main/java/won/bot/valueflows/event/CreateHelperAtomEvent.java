package won.bot.valueflows.event;

import won.bot.framework.eventbot.event.BaseAtomSpecificEvent;

import java.net.URI;

public class CreateHelperAtomEvent extends BaseAtomSpecificEvent {
    public CreateHelperAtomEvent(URI atomURI) {
        super(atomURI);
    }
}
