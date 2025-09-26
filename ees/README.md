AreaAgents:

Ein AreaAgent im Single-Agent-Szenario hat folgende Goals:

    1) MaintainDistributeCSVJobs - überprüft, ob die Simulation Time die vaTime vom Job erreicht hat. 
        Falls ja, dann findet er einen Trike, schickt ihm einen REQUEST mit Job Informationen.
        Dann wartet der AreaAgent auf eine ACK Nachricht, also eine Bestätigung, dass der Job übernommen wurde.
        Wenn eine NACK kommt oder keine Bestätigung, dann sucht der AreaAgent wieder nach einem TrikeAgent in seinem Bereich.
        Bei NACK heißt es, dass der TrikeAgent nicht mehr zu diesem AreaAgent gehört und sich schon abgemeldet hat.
        Bei Multi-Area-Setup wird der Job an andere AreaAgents delegiert, falls der AreaAgent keine Trikes hat. 

    2) PrintSimTime - drcukt die Simulationszeit, wie viele Trikes der AreaAgent hat, und bei Multi-Area-Setup auch die Last.

    3) CheckRequests - ist ein Teil von Request-Response, der eine Request-basierte Message nochmal versendet, 
        bis max attempt erreicht wird oder eine Antwort kommt.

    4) ReceivedMessages - der AreaAgent speichert alle erhaltenen Messages in einem Puffer. Dieses Goal löscht vom Puffer die alten Nachrichten.

    5) MaintainDistributeFirebaseJobs - funktioniert nur wenn FIREBASE_ENABLED in config auf true gesetzt wird. Es liest die Jobs von Firebase.

Folgende Goals gehören zu Multi-Agent-Szenario:

    1) TrikeCount - überprüft, wie viele Trikes der AreaAgent hat und wie groß die Last ist. 
        Bei einer großen Last oder einer kleinen Trikes Anzahl erstellt der AreaAgent einen künstlichen Trip, 
        also ein rebalancing Trip. Dieser Trip wird als DelegateInfo Object in jobsToDelegate Liste eingefügt.
    
    2) DelegateJobs - broadcastet an alle Nachbarn des AreaAgents eine CALL_FOR_PROPOSAL mit Job Info

    3) CheckDelegateInfo - überprüft alle DelegateInfo Objekte in jobsToDelegate, ob die Nachbarn geantwortet haben.
    Die Nachbarn antworten mit ihrem eigenen Load und wie viele Hops sie entfernt sind. 
    Falls es um einen Kunden-Trip geht, haben die Nachbarn mit der kleinsten Hops Anzahl eine höhere Priorität. 
    Bei Rebalance-Trips ist wird der Nachbar mit niedrigster Last bevorzugt. 
    Der AreaAgent schickt eine ACCEPT_PROPOSAL an dem besten Nachbarn und warte auf ACK, eine Bestätigung.
    Falls nichts ankommt, dann geht der Trip verloren.

    4) TripsLoad - verringert den Load des AreaAgents mit der Zeit, also eine Dämpfung

    5) MaintainDistributeAssignedJobs - Wenn eine ACCEPT_PROPOSAL kommt, dann wird es diesem AreaAgenten ein Job zugewiesen. 
    Der AreaAgent schickt diesen Job an einen von seinen Trikes.

CNP bei TrikeAgents:

    MANAGER:

    1) NEW -> COMMIT oder DELEGATE
    2) Falls COMMIT, dann wird es zu COMMITED
    3) Falls Delegate, dann wird es zu WAITING_FOR_NEIGHBOURLIST.

Der TrikeAgent überprüft die Quelle von dem Job. Wenn der Job vom eigenen AreaAgent stammt oder ein Rebalance Trip ist, dann fragt er nur seinen AreaAgent nach Nachbarn-Trikes, also lokal.
Sonst fragt er alle Nachbarn seines AreaAgents mit Radius r, also ein Broadcast.

    4) WAITING_FOR_NEIGHBOURLIST führt zu READY_FOR_CFP, wenn der Manager alle Antworten bekommt. Bei Timeout auch READY_FOR_CFP.
    5) Es gibt eine minimale Anzahl von benötigten Trikes für CNP, nämlich MIN_CNP_TRIKES. Falls es weniger als MIN_CNP_TRIKES Trikes gibt und nur lokal gefragt wurde, dann wiederholt der Manager den DELEGATE Schritt, aber diesmal broadcastet der Trike.
    Falls die agentIds Liste leer ist(niemand geantwortet), dann commited der Trike den Trip direkt. Ansonsten WAITING_FOR_PROPOSALS.

    6) WAITING_FOR_PROPOSALS führt zu READY_FOR_DECISION, wenn der Manager alle Antworten bekommt. Bei Timeout auch READY_FOR_DECISION.

    7) Bei READY_FOR_DECISION findet der Trike die beste Wahl geht zu WAITING_FOR_CONFIRMATIONS Zustand und schickt eine ACCEPT_PROPOSAL.
    Alle anderen erhalten eine REJECT_PROPOSAL. Falls er selbst den höchsten Utility Score hatte(auch der Fall wenn agentIds Liste leer ist), dann commitet er selbst.

    8) Bei WAITING_FOR_CONFIRMATIONS wartet er CONFIRM_WAIT_TIME Sekunden bis eine ACK Nachricht kommt. ACK würde bedeuten, dass der andere Trike den Job übernommen hat. Bei Timeout commitet der Manager selbst.


WORKER:
    
    1) Wenn der Trike eine CALL_FOR_PROPOSAL erhält, ist er in PROPOSED Zustand, schickt seinen Utility score und wird in WAITING_FOR_MANAGER Zustand.

    2) Bei WAITING_FOR_MANAGER wartet er auf eine Antwort(ACCEPT_PROPOSAL oder REJECT_PROPOSAL). Bei Timeout oder bei REJECT_PROPOSAL wird NOT_ASSIGNED.

    3) Bei ACCEPT_PROPOSAL commitet der Worker den Trip.



Message:
    UUID id: wird intern automatisch erstellt. Wichtig bei Request/Response verwendet.
    String senderId;
    String receiverId;
    ComAct comAct;
    long timeStamp: in Millisekunden
    int attempts: by default 1. Das heißt die Nachricht wird nur einmal geschickt(Reqeusts/Response somit ausgeschaltet)
    final MessageContent content;
    MessageContent:
    String action;
    ArrayList<String> values;

    ComAct: INFORM, REQUEST, ACK, NACK, CALL_FOR_PROPOSAL, PROPOSE, REFUSE, ACCEPT_PROPOSAL, REJECT_PROPOSAL




AreaAgents und TrikeAgents haben sendMessage Methoden in Utils in eigenen Packages.

Ein Job vom Area zu Trike ist ein Request, da es um eine wichtige Nachricht handelt.

areaagent\Utils:

    checkTrikeMessagesBuffer:

        INFORM: update trike location
        ACK: die Nachricht wurde beim Trike empfangen. Wird bei Request/Response verwendet
        NACK: Trike hat die Nachricht verweigert. Wird bei Request/Response verwendet.
        REQUEST: Trike fragt nach anderen Trikes

	checkAreaMessagesBuffer: CNP bei AreaAgents
		CALL_FOR_PROPOSAL
		REJECT_PROPOSAL

	checkProposalBuffer: auch CNP bei AreaAgent
		PROPOSE
		REFUSE

	checkAssignedJobs: der AreaAgent darf den Job in seine Liste einfügen
		ACCEPT_PROPOSAL

trikeagent\Utils:

    checkCNPBuffer: CNP Nachrichten
    CALL_FOR_PROPOSAL:
    PROPOSE:
    ACCEPT_PROPOSAL:
    REJECT_PROPOSAL:
    REFUSE:
    ACK: Die Sicht des Managers. Der andere Trike hat seinen Trip übernommen und der Manager kann als Delegated markieren.

	checkMessagesBuffer: restliche Messages. Aktuell nur antwort vom AreaAgent beim Ask for neighbors
		INFORM:

	checkJobBuffer:
		REQUEST: der AreaAgent schickt den Job



Das verschicken der Nachricht geht über SharedUtils. Alle Agenten führen dieselbe Methode(sendMessage) aus. 


Die Kopien von sendMessage bleiben immer noch in AreaTrikeService. Wenn man zwischen Service Kommunikation und dem Workaround umschalten möchte, kann man den Flag SERVICES_USED in configs verwenden. 
So sieht es aus:

<class class_name="SharedConstants.java">
    ...
    <field field_name="SERVICES_USED">true</field>
</class>

Ohne diesen Config wird automatisch die Workaround version verwendet.


In sendMessage mit SEVICES_USED=true verwendet die Methode den Service vom Sender. Mit SEVICES_USED=false verwendet die Methode die sendMessage direkt beim Receiver.


Wie funktionieren Events:
Wenn ein Event erstellt wird, muss eine entsprechende Methode aufgerufen werden, z.B CustomerTripCreation, DecisionTaskCommit etc.

addEvent wird für normale Events mit old- und newvalues.

addXAgProcess loggt XAGProcess

addEvent() und addXAgProcess() verwenden writeObjectToJsonFile intern. writeObjectToJsonFile ist nur zu nutzen, wenn addEvent und addXAgProcess nicht passen/ausreichen.

Folgende Methoden verwenden writeObjectToJsonFile direkt: DecisionTaskCommit, CommitDespiteCNP, CommitAsCNPparticipant, commitNewCustomerRequest.

###############################################
Wie erstellt man Event mit old- und newValues:

Am besten Methode wie estimateBatteryAfterTIP_BeliefUpdated oder AgentPosition_BeliefUpdated copypasten.

2. estimateBatteryAfterTIP_BeliefUpdated nutzen, wenn der zu speichernde Event einen spezifischen Objekt braucht, der nicht ein Teil des TrikeAgents selbst ist(z.B. DecisionTask, Trip, int, bool, long etc.)

3. AgentPosition_BeliefUpdated passt gut, wenn der zu speichernde Event ein Teil des TrikeAgents selbst ist(z.B. Location, battery etc.)

Aufpassen!
In diesem Codeblock:

if(oldValuesMap.get(event.content.eventType) == null){
event.content.data.oldValue = new Location("", -1, -1); // hier gehts um Location
}

oder

if(oldValuesMap.get(event.content.eventType) == null){
event.content.data.oldValue = 0.9; // hier gehts um Akku
}

oldValue muss denselben Datentyp haben, wie newValue.

4. Die kopierte Methode modifizieren und nutzen.

###############################################
Wie erstellt man XAG Event:

Am besten Methode wie CustomerTripCreation copyparties.

In Map<String, Object> queries kann man queries hinzufügen. Dasselbe gilt für actions

Die kopierte Methode modifizieren und nutzen.

###############################################
Wie führt man die Event Methode aus:
TrikeAgent package hat eine Utils Klasse, wo eventTracker erstellt wurde. Diesen Object nutzen um die Methoden auszuführen. Utils sind bei TrikeAgent und Plans schon initialisiert. In Utils Klasse eventTracker.blabla() nutzen, in TrikeAgent und Plans utils.eventTracker.blabla().

###############################################

Wie funktioniert old- und newValue:

Bei der Verwendung von addEvent, der inhalt von Data geht in newValue

Beim nächsten Loggen, wird der vorherige newValue zu einem oldValue.

if(oldValuesMap.get(event.content.eventType) == null){
 	event.content.data.oldValue = new Location("", -1, -1); // hier gehts um Location
}

So ein Code erstellt den allerersten oldValue. Irgendeinen dummy-Wert schreiben. Hauptsache bleibt der Datentyp gleich. 

