Diese README ist eine von ChatGPT bearbeitete Version von README.md.

# AreaAgents

## Inhaltsverzeichnis
- [Übersicht](#übersicht)
- [Goals im Single-Agent-Szenario](#goals-im-single-agent-szenario)
- [Goals im Multi-Agent-Szenario](#goals-im-multi-agent-szenario)
- [CNP bei TrikeAgents](#cnp-bei-trikeagents)
   - [Manager](#manager)
   - [Worker](#worker)
- [Nachrichtenstruktur](#nachrichtenstruktur)
- [Utils](#utils)
   - [areaagent/Utils](#areaagentutils)
   - [trikeagent/Utils](#trikeagentutils)
- [SharedUtils & Services](#sharedutils--services)
- [Events & Logging](#events--logging)
   - [Events mit old- und newValues](#events-mit-old--und-newvalues)
   - [XAG Events](#xag-events)
   - [Ausführung von Events](#ausführung-von-events)
- [TL;DR](#tldr)

---

## Übersicht
Ein **AreaAgent** übernimmt die Verteilung und Verwaltung von Jobs für TrikeAgents.  
Es gibt Unterschiede zwischen **Single-Agent-Szenarien** und **Multi-Agent-Szenarien**.  
Zusätzlich verwenden TrikeAgents ein **CNP (Contract Net Protocol)**.

---

## Goals im Single-Agent-Szenario

1. **MaintainDistributeCSVJobs**
   - Überprüft, ob die Simulation Time die `vaTime` vom Job erreicht hat.
   - Findet einen Trike und schickt ihm einen `REQUEST` mit Job-Informationen.
   - Wartet auf `ACK`.
   - Bei `NACK` oder fehlender Antwort: neuer TrikeAgent wird gesucht.
   - In Multi-Area-Setups: Weiterleitung des Jobs an andere AreaAgents, wenn keine Trikes verfügbar sind.

2. **PrintSimTime**
   - Druckt Simulationszeit, Anzahl Trikes, und bei Multi-Area-Setup auch die Last.

3. **CheckRequests**
   - Teil der Request-Response-Logik.
   - Sendet Requests mehrfach, bis max. Versuchszahl erreicht ist oder Antwort kommt.

4. **ReceivedMessages**
   - Speichert eingehende Nachrichten in Puffer.
   - Alte Nachrichten werden gelöscht.

5. **MaintainDistributeFirebaseJobs**
   - Nur aktiv bei `FIREBASE_ENABLED = true` in der Config.
   - Liest Jobs von Firebase.

---

## Goals im Multi-Agent-Szenario

1. **TrikeCount**
   - Prüft Anzahl Trikes und Last.
   - Bei hoher Last oder zu wenigen Trikes: erstellt Rebalancing-Trip (`DelegateInfo`).

2. **DelegateJobs**
   - Broadcast `CALL_FOR_PROPOSAL` an Nachbarn.

3. **CheckDelegateInfo**
   - Überprüft `jobsToDelegate`.
   - Nachbarn antworten mit Last + Hops.
   - Auswahl:
      - Kunden-Trip → geringste Hops.
      - Rebalance-Trip → niedrigste Last.
   - Bester Nachbar bekommt `ACCEPT_PROPOSAL`.
   - Keine Antwort → Trip geht verloren.

4. **TripsLoad**
   - Reduziert Last über Zeit (Dämpfung).

5. **MaintainDistributeAssignedJobs**
   - Bei `ACCEPT_PROPOSAL`: Job zugewiesen.
   - AreaAgent schickt Job an eigenen Trike.

---

## CNP bei TrikeAgents

### Manager

1. **NEW → COMMIT oder DELEGATE**
2. Bei COMMIT → `COMMITED`.
3. Bei DELEGATE → `WAITING_FOR_NEIGHBOURLIST`.
   - Lokale Nachbarn bei Rebalance-Trips oder lokalen Jobs.
   - Sonst Broadcast mit Radius `r`.
4. `WAITING_FOR_NEIGHBOURLIST → READY_FOR_CFP` (bei Antworten oder Timeout).
5. Mindestanzahl Trikes: `MIN_CNP_TRIKES`.
   - Wenn zu wenige: erneutes DELEGATE, diesmal mit Broadcast.
   - Keine Antworten → Trike committet selbst.
6. `WAITING_FOR_PROPOSALS → READY_FOR_DECISION` (Antworten oder Timeout).
7. **READY_FOR_DECISION**
   - Bester Kandidat → `ACCEPT_PROPOSAL`.
   - Andere → `REJECT_PROPOSAL`.
   - Höchster Score selbst → Selbst-Commit.
8. **WAITING_FOR_CONFIRMATIONS**
   - Wartet `CONFIRM_WAIT_TIME` auf `ACK`.
   - Timeout → Selbst-Commit.

### Worker

1. Bei `CALL_FOR_PROPOSAL` → Zustand `PROPOSED`, sendet Utility Score.
2. `WAITING_FOR_MANAGER` → wartet Antwort.
   - `ACCEPT_PROPOSAL` → Commit.
   - `REJECT_PROPOSAL` oder Timeout → `NOT_ASSIGNED`.
3. Bei `ACCEPT_PROPOSAL` → Worker committet Trip.

---

## Nachrichtenstruktur

```
UUID id;           // automatisch generiert
String senderId;
String receiverId;
ComAct comAct;
long timeStamp;    // in Millisekunden
int attempts = 1;  // Nachricht wird nur einmal geschickt
MessageContent content;
```

**MessageContent**
- `String action`
- `ArrayList<String> values`

**ComAct**  
`INFORM, REQUEST, ACK, NACK, CALL_FOR_PROPOSAL, PROPOSE, REFUSE, ACCEPT_PROPOSAL, REJECT_PROPOSAL`

---

## Utils

### areaagent/Utils

- **checkTrikeMessagesBuffer**
   - `INFORM`: Update Location
   - `ACK`: Bestätigung
   - `NACK`: verweigert
   - `REQUEST`: Trike fragt nach anderen Trikes
- **checkAreaMessagesBuffer**: CNP bei AreaAgents (`CALL_FOR_PROPOSAL`, `REJECT_PROPOSAL`)
- **checkProposalBuffer**: CNP (`PROPOSE`, `REFUSE`)
- **checkAssignedJobs**: akzeptierte Jobs (`ACCEPT_PROPOSAL`)

### trikeagent/Utils

- **checkCNPBuffer**: CNP Nachrichten (`CALL_FOR_PROPOSAL`, `PROPOSE`, `ACCEPT_PROPOSAL`, `REJECT_PROPOSAL`, `REFUSE`, `ACK`)
- **checkMessagesBuffer**: Antworten vom AreaAgent (z.B. Neighbor-Anfragen → `INFORM`)
- **checkJobBuffer**: Job vom AreaAgent (`REQUEST`)

---

## SharedUtils & Services

- Nachrichtenversand über **SharedUtils**.
- Einheitliche `sendMessage`-Methode für AreaAgents und TrikeAgents.
- Kopien existieren noch in **AreaTrikeService**.
- Umschaltbar über Config `SERVICES_USED`:

```xml
<class class_name="SharedConstants.java">
    ...
    <field field_name="SERVICES_USED">true</field>
</class>
```

- `SERVICES_USED = true` → Service vom Sender wird genutzt.
- `SERVICES_USED = false` → direkte Kommunikation mit Empfänger.

---

## Events & Logging

### Events mit old- und newValues

- Normale Events → `addEvent`.
- Beispielmethoden:
   - `estimateBatteryAfterTIP_BeliefUpdated` (für externe Objekte wie Trip, DecisionTask, etc.)
   - `AgentPosition_BeliefUpdated` (für interne Werte wie Location, Battery, etc.)
- Wichtig: `oldValue` und `newValue` müssen denselben Datentyp haben.
- Beispiel Initialisierung von `oldValue`:

```
if(oldValuesMap.get(event.content.eventType) == null){
    event.content.data.oldValue = new Location("", -1, -1); // für Location
}
```

oder

```
if(oldValuesMap.get(event.content.eventType) == null){
    event.content.data.oldValue = 0.9; // für Battery
}
```

### XAG Events

- Erstellung über Methoden wie `CustomerTripCreation`.
- `queries` und `actions` können in `Map<String, Object>` ergänzt werden.

### Ausführung von Events

- In `TrikeAgent.Utils` existiert ein `eventTracker`.
- Nutzung:
   - `utils.eventTracker.blabla()` in TrikeAgent & Plans.
   - `eventTracker.blabla()` in Utils.

---

## TL;DR

- **AreaAgents** verwalten Jobs, Trike-Zuweisungen und Multi-Agent-Koordination.
- **TrikeAgents** arbeiten mit CNP (Manager/Worker).
- **Nachrichten** folgen standardisiertem Aufbau.
- **Utils** kapseln Message-Handling.
- **Events** loggen alte & neue Werte für Transparenz.  
