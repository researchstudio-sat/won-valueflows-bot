package won.bot.valueflows.util;

import org.apache.jena.rdf.model.Resource;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.vocabulary.WONCON;
import won.protocol.vocabulary.WXHOLD;
import won.protocol.vocabulary.WXVALUEFLOWS;

import java.net.URI;

public class HelperAtomModelWrapper extends DefaultAtomModelWrapper {
    public HelperAtomModelWrapper(URI helperAtomUri, URI actorToSupportUri) {
        super(helperAtomUri);

        this.setTitle("VF-Supporter Atom");

        Resource atom = this.getAtomModel().createResource(helperAtomUri.toString());
        Resource actorToSupport = atom.getModel().createResource(actorToSupportUri.toString());

        atom.addProperty(WONCON.inResponseTo, actorToSupport);

        /** The query that is generated looks like this:
         PREFIX won: <https://w3id.org/won/core#>
         PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
         PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         PREFIX vf: <https://w3id.org/valueflows#>
         PREFIX wx-vf: <https://w3id.org/won/ext/valueflows#>

         SELECT DISTINCT ?result WHERE {
             ?result rdf:type won:Atom, vf:EconomicResource.
             ?result won:socket/won:socketDefinition wx-vf:SupportableSocket .
             ?result won:connections ?connections .
             ?connections rdfs:member ?conn .
             ?conn won:connectionState won:Connected .
             ?conn won:socket/won:socketDefinition wx-vf:PrimaryAccountableSocket .
             ?conn won:targetAtom <[atomToSupportUri]> .
         }
         */

        this.setQuery("PREFIX won: <https://w3id.org/won/core#>\n" +
             "         PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
             "         PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
             "         PREFIX vf: <https://w3id.org/valueflows#>\n" +
             "         PREFIX wx-vf: <https://w3id.org/won/ext/valueflows#>\n" +
             "         SELECT DISTINCT ?result WHERE {\n" +
             "             ?result rdf:type won:Atom, vf:EconomicResource.\n" +
             "             ?result won:socket/won:socketDefinition wx-vf:SupportableSocket .\n" +
             "             ?result won:connections ?connections .\n" +
             "             ?connections rdfs:member ?conn .\n" +
             "             ?conn won:connectionState won:Connected .\n" +
             "             ?conn won:socket/won:socketDefinition wx-vf:PrimaryAccountableSocket .\n" +
             "             ?conn won:targetAtom <"+actorToSupportUri+"> .\n" +
             "         }");

        this.addSocket("#HoldableSocket", WXHOLD.HoldableSocket.asString());
        this.addSocket("#vfSupporterSocket", WXVALUEFLOWS.SupporterSocket.asString());
    }

    public HelperAtomModelWrapper(String helperAtomUri, String actorToSupportUri) {
        this(URI.create(helperAtomUri), URI.create(actorToSupportUri));
    }

    public HelperAtomModelWrapper(String helperAtomUri, URI actorToSupportUri) {
        this(URI.create(helperAtomUri), actorToSupportUri);
    }
}
