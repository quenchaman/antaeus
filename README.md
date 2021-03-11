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
docker build -t antaeus .
docker run antaeus
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

I :heart: Kotlin!!! I'd love to get this job! :sob:
### Invoice processing feature
#### Strategic design
On the first day of each month, charge all unpaid invoices. At the end, all invoices should be charged, except for the cases when the customer does not exist in the payment service. Such cases should be recorded.

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
* Solution: Retry 3 times...This may not be enough to finish our work on the invoices...
---
* Problem: What if the Payment service is not available?
* Solution: We should really use a message queue between us and the payment service to make sure that we have some temporary storage for the invoice charge events if the service is not available.
---
* Problem: How to make sure that REST endpoint to trigger the billing is omnipotent and we do not charge customer more than once?
* Solution: ???...Marking the invoices with a status in a transactional way will prevent billing them twice, because we get only the unpaid invoices.
---
---

#### Initial Flow diagram
![sequence diagram](https://github.com/quenchaman/antaeus/blob/master/CustomerPaymentsDesign.svg?raw=true)

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
- Introduce logging.
- Test coverage report.
- It does not seem like a good design to fetch customer inside exchange service and other services, this should be non-nullable parameter to the methods

#### What I will not do and why
What: Test DAL level methods
Why: There should not be any business logic in a DAL method, so nothing to unit test. The connection to database will be tested during integration tests via other services that call the DAL.
---
What: I will not use JOINs between Invoice and Customer and change the customerId to customer in Invoice.
Why: With in memory DB I will just map the customers to invoices where needed, no memory overhead, but surely performance will suffer. If I go the route of changing Invoice model I will break the contract with PaymentProvider and it has to rebuild and redeploy. I can invent a new invoice class used just for carrying the Customer for the currency check, but for simplicity I will not.

#### I won't have time for...
- ExchangeProvider will make network requests to convert currency and there many things can go wrong, but I will cover only the happy path.
- Having separate database for testing is a good practice. I will just cleanup the current one when testing.
- Creating a Dal abstraction for the common operations on a table.
- Create a REST endpoint on which the client can check the status of the billing.
- Handle case when customer is null for an invoice.

#### Things I could not figure out how to do
- I could not map a Pair<T, U?> to Pair<T, U> with the help of filter with U != null .... :/

#### 11.03.2021
I moved the joining of Invoices and Customer to the data-access layer and I am happy with the result.