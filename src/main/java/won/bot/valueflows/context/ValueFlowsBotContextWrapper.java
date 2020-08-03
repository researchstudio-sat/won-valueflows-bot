package won.bot.valueflows.context;

import won.bot.framework.bot.context.BotContext;
import won.bot.framework.extensions.serviceatom.ServiceAtomEnabledBotContextWrapper;

import java.net.URI;
import java.util.*;

public class ValueFlowsBotContextWrapper extends ServiceAtomEnabledBotContextWrapper {
    private final String connectedSocketsMap;
    private final String connectedServiceAtomSocketsMap;
    private final String helperAtomsMap;

    public ValueFlowsBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
        this.connectedSocketsMap = botName + ":connectedSocketsMap";
        this.connectedServiceAtomSocketsMap = botName + ":connectedServiceAtomSocketsMap";
        this.helperAtomsMap = botName + ":helperAtomsMap";
    }

    public Map<URI, Set<URI>> getConnectedSockets() {
        Map<String, List<Object>> connectedSockets = getBotContext().loadListMap(connectedSocketsMap);
        Map<URI, Set<URI>> connectedSocketsMapSet = new HashMap<>(connectedSockets.size());

        for(Map.Entry<String, List<Object>> entry : connectedSockets.entrySet()) {
            URI senderSocket = URI.create(entry.getKey());
            Set<URI> targetSocketsSet = new HashSet<>(entry.getValue().size());
            for(Object o : entry.getValue()) {
                targetSocketsSet.add((URI) o);
            }
            connectedSocketsMapSet.put(senderSocket, targetSocketsSet);
        }

        return connectedSocketsMapSet;
    }

    public Map<URI, Set<URI>> getServiceAtomConnectedSockets() {
        Map<String, List<Object>> connectedSockets = getBotContext().loadListMap(connectedServiceAtomSocketsMap);
        Map<URI, Set<URI>> connectedSocketsMapSet = new HashMap<>(connectedSockets.size());

        for(Map.Entry<String, List<Object>> entry : connectedSockets.entrySet()) {
            URI senderSocket = URI.create(entry.getKey());
            Set<URI> targetSocketsSet = new HashSet<>(entry.getValue().size());
            for(Object o : entry.getValue()) {
                targetSocketsSet.add((URI) o);
            }
            connectedSocketsMapSet.put(senderSocket, targetSocketsSet);
        }

        return connectedSocketsMapSet;
    }

    public Map<URI, URI> getHelperAtomEntries() {
        Map<String, Object> helperAtomEntries = getBotContext().loadObjectMap(helperAtomsMap);
        Map<URI, URI> helperAtomsMap = new HashMap<>(helperAtomEntries.size());

        for(Map.Entry<String, Object> entry : helperAtomEntries.entrySet()) {
            URI atomUri = URI.create(entry.getKey());
            helperAtomsMap.put(atomUri, (URI) entry.getValue());
        }

        return helperAtomsMap;
    }

    public URI getHelperAtomUri(URI atomUri) {
        Map<URI, URI> helperAtomEntries = getHelperAtomEntries();
        return helperAtomEntries.get(atomUri);
    }

    public void addHelperAtomUriEntry(URI atomUri, URI helperAtomUri) {
        getBotContext().addToListMap(helperAtomsMap, atomUri.toString(), helperAtomUri);
    };

    public void removeHelperAtomUriEntry(URI atomUri) {
        getBotContext().removeFromObjectMap(helperAtomsMap, atomUri.toString());
    }

    public void addServiceAtomConnectedSocket(URI senderSocket, URI targetSocket) {
        getBotContext().addToListMap(connectedServiceAtomSocketsMap, senderSocket.toString(), targetSocket);
    }

    public void removeServiceAtomConnectedSocket(URI senderSocket, URI targetSocket) {
        getBotContext().removeFromListMap(connectedServiceAtomSocketsMap, senderSocket.toString(), targetSocket);
    }

    public void addConnectedSocket(URI senderSocket, URI targetSocket) {
        getBotContext().addToListMap(connectedSocketsMap, senderSocket.toString(), targetSocket);
    }

    public void removeConnectedSocket(URI senderSocket, URI targetSocket) {
        getBotContext().removeFromListMap(connectedSocketsMap, senderSocket.toString(), targetSocket);
    }
}
