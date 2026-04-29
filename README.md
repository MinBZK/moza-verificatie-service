
![Project Pre-Alpha Status](https://img.shields.io/badge/life_cycle-pre_alpha-red)

# Verificatieservice

De Verificatieservice is een op Quarkus gebaseerde microservice die verantwoordelijk is voor het genereren en verifiëren van verificatiecodes via e-mail.

## Functionaliteiten

- **Verificatieaanvraag**: Gebruikers kunnen een verificatiecode aanvragen voor een specifiek e-mailadres. Optioneel kunnen eigen NotifyNL API-sleutel en template-ID worden meegegeven.
- **Verificatie**: Validatie van de verstrekte code tegen een referentie-ID.
- **E-mailnotificaties**: Verificatiecodes worden verzonden via NotifyNL.
- **Automatische opschoning**: Verlopen en succesvol geverifieerde codes worden periodiek verplaatst naar statistieken en verwijderd uit de actieve tabel.
- **Beheerdersstatistieken**: Inzicht in de gemiddelde verificatietijd en het percentage niet-geverifieerde aanvragen.
- **Cleanup Job Monitoring**: Health check endpoint voor monitoring van de opschoonjobs.

## Architectuur & Dependencies

- **Framework**: [Quarkus](https://quarkus.io/) (Supersonic Subatomic Java Framework)
- **Database**: PostgreSQL (met Hibernate ORM & Panache)
- **E-mail Service**: NotifyNL (voor het verzenden van verificatiecodes)
- **Validatie**: Hibernate Validator (Jakarta Bean Validation)
- **API Documentatie**: OpenAPI/Swagger UI
- **Job Scheduling**: Quarkus Scheduler voor periodieke opschoning
- **HTTP Client**: Java 11+ HttpClient (managed via CDI)

## Lokale Setup

### Vereisten

- Java 21 of hoger
- Maven 3.9+
- Docker & Docker Compose

### 1. Infrastructuur opstarten

Start de benodigde services (PostgreSQL) met Docker Compose:

```shell script
docker-compose up -d
```

- **PostgreSQL**: Draait op poort `5432`.

### 2. Applicatie starten in Dev Mode

Start de Quarkus applicatie met live coding ingeschakeld:

```shell script
./mvnw quarkus:dev
```

De applicatie is beschikbaar op `http://localhost:8080`.
- **Swagger UI**: `http://localhost:8080/q/swagger-ui/`
- **Dev UI**: `http://localhost:8080/q/dev/`

## API Endpoints

### Verificatie

- `POST /request`: Vraag een nieuwe code aan. Verwacht een JSON met `email` (verplicht), en optioneel `apiKey` en `templateId` voor custom NotifyNL configuratie. Retourneert een `referenceId`.
- `POST /verify`: Verifieer een code. Verwacht een JSON met `referenceId` en `code`.

### Beheer

- `GET /admin/statistics`: Haal statistieken op over de verificatieprocessen.
- `GET /admin/cleanup-job/metrics`: Haal metrics en health informatie op over de cleanup jobs.

## Verificatie Flow

1. Bij een aanvraag (`POST /request`) wordt een `VerificationCode` gegenereerd en opgeslagen in de database.
2. De verificatiecode wordt direct verzonden via NotifyNL naar het opgegeven e-mailadres.
3. De gebruiker ontvangt de code en kan deze verifiëren via `POST /verify`.
4. Bij succesvolle verificatie wordt de code gemarkeerd als gebruikt.
5. Periodiek worden verlopen en gebruikte codes opgeschoond en verplaatst naar statistieken.

### Rate limiting

Om misbruik te voorkomen is er een rate limiter actief op het verzenden van e-mails. Per e-mailadres mag binnen een instelbaar tijdvenster een maximaal aantal verzendpogingen worden gedaan. Wordt dit maximum overschreden, dan wordt het verzoek direct afgewezen zonder dat er contact wordt opgenomen met NotifyNL. De teller wordt bijgehouden in het geheugen en automatisch opgeschoond via een periodieke taak.

### Circuit breaker

Bij herhaalde fouten in de communicatie met NotifyNL (bijvoorbeeld door netwerkproblemen of uitval van de externe dienst) wordt de circuit breaker actief. Na een configureerbaar aantal mislukte aanroepen gaat het circuit open: nieuwe verzoeken worden direct afgewezen zonder dat er opnieuw een verbinding wordt geprobeerd. Dit voorkomt dat de applicatie vastloopt op trage of niet-reagerende externe diensten. Na een wachttijd gaat het circuit in half-open toestand en worden nieuwe aanroepen opnieuw toegestaan om te testen of de externe dienst hersteld is.

## Configuratie

Belangrijke parameters in `src/main/resources/application.properties`:

### Verificatiecode instellingen
- `verification.code.validity-minutes`: Hoe lang een code geldig blijft (standaard 10 minuten).
- `verification.code.length`: Lengte van de gegenereerde verificatiecode (standaard 6 cijfers).
- `verification.cleanup.schedule`: Frequentie van de opschoonjobs (standaard elke 60 seconden).

### NotifyNL configuratie
- `notifynl.emailverificatie.url`: NotifyNL API endpoint URL.
- `notifynl.emailverificatie.api-key`: Standaard NotifyNL API-sleutel (gebruikt als fallback).
- `notifynl.emailverificatie.template-id`: Standaard NotifyNL template-ID (gebruikt als fallback).

### Rate limiter instellingen
- `rate.limit.max-requests`: Maximum aantal verzendpogingen per e-mailadres binnen het tijdvenster (standaard 5).
- `rate.limit.window-minutes`: Duur van het tijdvenster voor de rate limiter in minuten (standaard 15 minuten).
- `rate.limit.cleanup.schedule`: Frequentie van de opschoontaak voor verlopen rate limiter vermeldingen (standaard elk uur).

### Circuit breaker instellingen

De annotatieparameters in de code gelden als standaardwaarden. Ze kunnen per omgeving worden overschreven in `application.properties` via het MicroProfile Config formaat `<klasse>/<methode>/CircuitBreaker/<parameter>`. De annotatie waardes worden genegeerd als er voor een specifieke parameter een configuratiewaarde in de applicatie.properties file is ingevuld. Voorbeeld:

- `nl.rijksoverheid.moz.service.NotifyNLService/sendVerificationEmail/CircuitBreaker/requestVolumeThreshold`: Minimum aantal aanroepen voordat het circuit kan openen (standaard 5).
- `nl.rijksoverheid.moz.service.NotifyNLService/sendVerificationEmail/CircuitBreaker/failureRatio`: Drempelwaarde voor het percentage mislukte aanroepen (standaard 1.0 — circuit opent alleen bij volledige uitval).
- `nl.rijksoverheid.moz.service.NotifyNLService/sendVerificationEmail/CircuitBreaker/delay`: Wachttijd in milliseconden in de open toestand voordat het circuit half-open gaat (standaard 30000).
- `nl.rijksoverheid.moz.service.NotifyNLService/sendVerificationEmail/CircuitBreaker/successThreshold`: Aantal opeenvolgende successen in half-open toestand om het circuit te sluiten (standaard 2).

### HttpClient instellingen
- `httpclient.connect-timeout-seconds`: Timeout voor het opzetten van een verbinding (standaard 10 seconden).
- `httpclient.request-timeout-seconds`: Timeout voor het voltooien van een request (standaard 30 seconden).

### Database configuratie
- `quarkus.datasource.jdbc.url`: PostgreSQL database URL.
- `quarkus.datasource.username`: Database gebruikersnaam.
- `quarkus.datasource.password`: Database wachtwoord.
