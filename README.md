## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
./docker-start.sh
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

![peter griffin's turn to sing](https://thumbs.gfycat.com/CreepyCrispAntbear-max-1mb.gif)
## Valeri's part now :information_desk_person:

I :heart: Kotlin!!! I'd love to get this job! :sob:
### Invoice processing feature
#### Strategic design
On the first day of each month, charge all unpaid invoices. At the end, all invoices should be charged, except for the cases when the customer does not exist in the payment service or the service is down. Such cases should be recorded.
Use a service to exchange invoice currency that does not match customer's currency. :point_down:

#### Tactical design
* Problem: How we will schedule and run the procedure? 
* Solution: Expose REST endpoint to trigger the procedure. Use SNS and CloudWatch to call the endpoint at specific time and call it every first day of a month at a time when traffic to the service is minimal (night time).
* Alternatives: CRON if we have only one instance of the service, otherwise it can cause race conditions.
---
* Problem: How do we get all unpaid invoices?
* Solution: Add a procedure to select all Invoices with unpaid status. Use it in the solution's procedure.
* Further improvements: If the Invoices have a date we can select a date range for the previous month and have the problematic invoices handled by separate schedule. Otherwise each month the DB scan will grow larger.  
---
* Problem: How do we charge the invoices?
* Solution: Call the external API for every invoice that is not paid. Do it in parallel.
---
* Problem: How we handle failed calls with CustomerNotFoundException to the external API for charging an invoice?
* Solution: Do not retry this call, email or at least log the fact so that the appropriate department can resolve it.
---
* Problem: How we handle failed calls with CurrencyMismatchException to the external API for charging an invoice?
* Solution: Call external API to convert the amount of the Invoice to the currency of the customer. I would check the customer currency and invoice currency and send request to exchange the currencies in the invoice without actually persisting it on our end with the customer's currency.
* Alternatives: Do the conversion ourselves ...not a good alternative..:)
---
* Problem: How we handle failed calls with NetworkException to the external API for charging an invoice?
* Solution: Retry 3 times...This may not be enough to finish our work on the invoices...See below.
---
* Problem: What if the Payment service is not available?
* Solution: We should really use a message queue between us and the payment service to make sure that we have some temporary storage for the invoice charge events if the service is not available.
---
* Problem: How to make sure that REST endpoint to trigger the billing is omnipotent and we do not charge customer more than once?
* Solution: After fetching the unpaid invoices we can mark them as 'sent for processing' in a transaction with the fetching itself. That way, the next transaction would find the DB in a state where the invoices are not 'pending'(not paid)
Of course, the DB becomes a huge bottleneck, but in the fintech world it is better to be safe than sorry :)
Some DBs provide row level locking, so we could definitely improve the performance.
---
* Problem: Should we fire-and-forget the REST call to charge invoices or wait it out?
* Answer: From the perspective of the client of our service, it is better to return immediately and then give another endpoint to poll for the status of the charged invoices or just use GET /invoices for all of them and do some filtering.
---
---

#### Initial Flow diagram
![sequence diagram](https://github.com/quenchaman/antaeus/blob/valeri-hristov/invoice-billing/CustomerPaymentsDesign.svg)

#### My initial plan
1. Method to fetch invoice by status. DAO-level method.
2. Method to fetch unpaid invoices in InvoiceService that returns a Pair<Invoice, Customer>
3. ExchangeInvoice method in Invoice Service that will check if there is a mismatch and send to ExchangeService for fix.
4. a method in BillingService that gathers invoices, checks currency mismatch and calls a method to dispatch to PaymentService
5. method in BillingService that receives invoices and fires off requests to payment service.
6. Create REST endpoint to activate the procedure.

(Note: methods 4 and 5 may be one method)

#### Good ideas
- Return immediately from the controller that activates the billing, so that the client can do other useful work
- (Done)Introduce logging.
- Test coverage report.
- (Done) It does not seem like a good design to fetch customer inside exchange service and other services, this should be non-nullable parameter to the methods

#### What I will not do and justification
* What: Test DAL level methods
* Why: There should not be any business logic in a DAL method, so nothing to unit test. The connection to database will be tested during integration tests via other services that call the DAL.
---
* What: I will not use JOINs between Invoice and Customer and change the customerId to customer in Invoice.
* Why: With in memory DB I will just map the customers to invoices where needed, no memory overhead, but surely performance will suffer. If I go the route of changing Invoice model I will break the contract with PaymentProvider and it has to rebuild and redeploy. I can invent a new invoice class used just for carrying the Customer for the currency check, but for simplicity I will not.

#### I won't have time for...
- ExchangeProvider will make network requests to convert currency and there many things can go wrong, but I will cover only the happy path.
- Having separate database for testing is a good practice. I will just mock the current one when testing.
- Creating a Dal abstraction for the common operations on a table.
- Create a REST endpoint on which the client can check the status of the billing.
- Handle case when customer is null for an invoice.

#### Things I could not figure out how to do
- I could not map a Pair<T, U?> to Pair<T, U> with the help of filter with U != null .... :/
- How to mock a method in the same class that I am testing?

#### 11.03.2021
I moved the joining of Invoices and Customer to the data-access layer and I am happy with the result.

#### 12.03.2021
Implemented synchronous calls to ExchangeProvider::charge method, but I do not handle retries...I will attempt to make the calls in parallel and handle retries too.

#### 13.03.2021
What else?
1. ~~E2E tests~~
2. ~~DSL~~ not much opportunities here, maybe some other time...
3. ~~Abstract DAL class~~
4. Test DB in container
5. Multi-layer docker build
6. ~~Configuration file~~ (Kinda...)
7. ~~Gradle task for integration and e2e tests~~

I copy-pasted the gradle configuration for extracting the integration tests in their own source dir.
Gradle is obfuscated as it is and using it via kotlin script makes it even "better" :D Integration test does not compile now because it cannot find references...

Woo-hoo! It behaves!!! Now I will create a separate source and task for e2e tests. They will be run against a running service URL which we provide.

This commit will be quite huge :D I do not do such commits in my professional practice, but sometimes it happens. I will go ahead just because I am working alone on this :) Forgive me.

Woo-hoo!! E2E tests are up and running! Man, what is going on today...usually nothing works...

Now I have three nice tasks in the Gradle drawer - test, integrationTest and e2eTest...This makes me gitty

Running the e2e tests I am like...
![yeah](https://i.kym-cdn.com/photos/images/newsfeed/000/649/315/8a1.gif)
Now I am doing some TDD endpoint development using Retrofit...this library is genius!

Hmm.. there are a lot of endpoints that can be tested, but I am not trying to be exhaustive here, but to showcase the best practices and techniques that I know.

It might seam that in some areas I overdid it, but I just got carried away coding in Kotlin! If I put enough time and the project is used a bit it would improve, but now it is just a good starting point.

Configuring the different environments is not an easy task...It would be good to have a configurable app, for example, when passing a different property when launching the application we can change the DB which the app uses or the JVM settings.

In Spring configuring the environments is a breeze, but with this brick and mortar setup it is not easy...

I keep saying something is not easy...such a baby... :baby:

A benefit of using Docker that I realised just now - Stopping the app is Blazingly fast!!! When you run it on your PC it takes time for the JVM to cleanup and do some other black magic I have no clue of, but when you run it in container you just throw it away..no need to care about the OS's state.

Made refactoring of the DAL. Can't say that I am pleased with the results - I wanted to abtract the transaction thing, but I did not know what it does and I cannot put it in the base DAL class. At least I learned about generics and casting in Kotlin! :boom:

#### 14.03.2021
I woke up with the idea to code authentication in this app.

The easiest option would be something like basic auth or JWT or session based, but I assume that this service will be a part of a mesh of microservices that works together in a kubernetis cluster or AWS VPC, so it does not make sense for every client of the service to register in it...If I have to implement login it has to be OAuth based.

By using OAuth protocol, we will have one account for each service that is authorised to work with our service. When this service wants to call us, it would send, for example, LDAP credentials to the SSO provider and the provider would send a token back. The service would then send us this token and we will verify it that is a valid token for our app.

[pretty cool visualisations for the OAuth 2 and OpenID flow](https://developer.okta.com/blog/2019/10/21/illustrated-guide-to-oauth-and-oidc)

Let's map our actors to the OAuth terminologies mapped [here](https://developer.okta.com/blog/2019/10/21/illustrated-guide-to-oauth-and-oidc)

Let's say that our service is 'A' and the one that wants to use us is called 'B'.

* Resource Owner = A team or organisation that has permissions in service B
* Client = Service B
* Authorization Server = Okta
* Resource Server = Service A
* Redirect URL = we don't need that
* Response Type = default 'code' type is fine
* Scope = we want global scopes for starters
* Consent = we do not need that? :confused:
* Client ID = We will go in Okta to create app and get this
* Client Secret = from Okta too.
* Authorization Code = Service B has to get this somehow ??? and sent it to Okta
* Access Token = Service B will recieve this from Okta and send it to Service A

(Side note) It is bad practice to keep passwords in source code. In my practice I use env. variables for the passwords and inject them in the environments in which the code is deployed, for example, by passing them as -e arguments to docker image. For the DB connection I will not bother to extract the password in env. var because it is in-memory and does not expose any ports.

For the client id and secret, however, I will extract them as env. variables. 

```diff
- Design changed quite a bit during development, so it is better to look at the code, because Docs quickly get outdated.
```
