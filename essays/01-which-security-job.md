# Which security job uses what you already know?

Your first choice is the security responsibility you want to carry. That choice determines which parts of your engineering experience count, what you must learn next, and what evidence you should produce.

Consider a constructed example. A Java service lets customers export documents. A user requests an export, a background worker gathers the files, and a download link appears when the job finishes. Everything works in the ordinary test: the right customer receives the right documents.

Now change one condition. The user supplies an identifier belonging to another customer. The service accepts the request, and the worker retrieves the document using its own broad storage permissions. The export succeeds. Its success is the security failure.

Several people could work on this problem. One reproduces the unauthorized export and writes a finding. Another traces the missing ownership check, repairs it and tests related endpoints. Another creates an export component that carries the requesting user's identity through the queue and enforces the access policy. Someone also has to establish whether existing exports exposed data, coordinate the release and communicate what remains uncertain.

These activities meet at the same defect, but they ask for different kinds of competence. Choosing a security career starts by deciding which of those activities you want to become dependable at.

## Read the work behind the title

A job title is a poor substitute for the verbs in its description. Read what the person must inspect, build, decide and maintain. Then ask what happens after a problem is found.

At the time of inspection, Flexport's Amsterdam Product Security Engineer II posting combines design and code review with vulnerability reproduction, remediation, security tooling and developer guidance. Its experience requirement includes software development with a security focus. It also names cloud knowledge and security on-call responsibilities. That is a possible transition route for a developer, provided the security evidence is real. It is not evidence that any experienced developer already qualifies. [Flexport role](https://job-boards.greenhouse.io/flexport/jobs/7921061?gh_jid=7921061)

An Adyen Applied Security Engineering posting puts Java development and reusable security capabilities at the centre. The work includes building defaults that product teams adopt and owning implementation through production rollout. This particular vacancy is in Chicago, so it illustrates a role to search for rather than a local opportunity. [Adyen role](https://careers.adyen.com/fr_FR/vacancies/7537815-software-engineer-applied-security-engineering?locale=fr_FR)

GitLab's AppSec framework includes review, vulnerability handling and security releases. Its senior and staff descriptions add leadership of reviews, broader prevention and changes that help other developers build securely. The increasing responsibility is visible in the scope of decisions and influence across teams. [GitLab role framework](https://handbook.gitlab.com/job-description-library/security/application-security/)

For your own search, use this working map. It describes responsibilities to look for; employers may combine them differently.

| Centre of the work | Question in the export example | Evidence you would show |
|---|---|---|
| Application assessment | Can a user obtain another customer's document? | Reproduction, affected paths and a clear finding |
| Application/product security | Where must access be checked, and how will the repair reach production? | Design review, fix evidence, retest and release reasoning |
| Applied security engineering | How can other teams implement exports without repeating this mistake? | A usable component, tests and a migration example |
| Delivery/platform security | What authority does the worker or release pipeline actually need? | Identity policy, deployment controls and failure tests |
| Security operations and assurance | How will the organization detect, manage and explain this failure? | Investigation record, ownership, evidence and follow-through |

You can enjoy more than one row. The practical question is which row should organize your first substantial project. Trying to train for every row at once makes it difficult to finish any evidence strong enough to inspect.

## Translate experience into evidence

Years of Java development are useful because you can follow execution, understand frameworks, change code and reason about production constraints. Yet each of those strengths needs a security-specific extension.

In the export example, understanding queues helps you notice that the worker runs later and under a different identity. Security reasoning asks which user's authority should govern that later operation. Should the export still run if access has been revoked? What if the user belongs to several customer accounts? Who may retrieve the completed file?

Those questions expose a policy that must be decided before the implementation can be judged. A repair that checks access only when the request arrives might satisfy one intended policy and violate another. You must make the requirement explicit, enforce it at the relevant boundary and test the cases that would falsify your claim.

A useful experience inventory therefore has three columns:

| Existing experience | Security extension | Missing evidence |
|---|---|---|
| Built asynchronous Java services | Trace authority across request, queue and worker | A review of access changes during a queued task |
| Wrote integration tests | Test forbidden behaviour while preserving legitimate use | A regression test for cross-customer export |
| Migrated shared libraries | Make a security control difficult to misuse | Adoption by a second application, with misuse tests |
| Managed production releases | Coordinate a repair while exposure remains uncertain | A release decision with assumptions and follow-up |

Treat this as a worked example, not a description of your personal history. Replace its rows with work you actually did. An empty evidence cell is useful: it identifies a task that can become your next practice session.

Be equally precise about what an artifact proves. A test showing that the supplied example now fails proves something about that path. It does not establish that every document path enforces isolation. A shared library demonstrates engineering ability; without a second caller, it says little about whether another team can adopt it correctly.

This distinction protects you from both extremes. You need not discard your engineering experience and start your professional identity from zero. You also cannot relabel every debugging task as security experience. The connection has to survive questions about the property, the failure and your contribution.

## Keep employment and consulting open

The same investigation can support either route. The evidence changes at the boundary of the engagement.

For employment, show how you help a product team reach a better result: explain the failure, agree on the intended policy, implement or guide the repair, and check related paths. An internal transition could let you practise this beside an experienced security colleague, where your present role permits it. That is a route to investigate, not an assumption that your employer has such a position.

For consulting, the export assessment also needs a bounded promise. Which product version and workflows are included? What access and test accounts will the client provide? Does the delivery include a code change, a report, a retest, or all three? Who makes the release decision? A technically convincing finding can still be a poorly scoped engagement.

An initial practice offer could describe an assessment of authorization across document export and download, with reproduction steps, affected paths and one retest of agreed changes. This is a constructed offer for learning how to scope work. It is not a claim that clients will buy it or that completing this book qualifies you to deliver it alone.

Keep compensation separate from the technical-fit decision. A role may use your strengths while offering less than your current package. An attractive consulting rate may leave substantial time unpaid. Compare employment packages on their actual components and conditions; compare consulting revenue with delivery, review, sales and other costs. The examples above do not establish a Dutch salary range.

Your first choice can therefore be provisional: investigate application/product security roles while learning to describe a narrow assessment engagement. Both routes need the ability to produce a defensible result. Neither requires you to commit now to founding a consultancy.

## What AI changes in this choice

Use a thought experiment rather than a forecast. Suppose an AI tool identifies the export defect, drafts a test and proposes a repair. Which parts of the work remain unresolved?

Someone must establish the intended access policy. Someone must verify the generated test actually reaches the vulnerable path. The team must examine other callers, choose how queued work handles revoked access, and release the change without breaking legitimate exports. The tool's output becomes part of the investigation.

This is why the book emphasizes verification, system context and reusable controls. It is a strategy for learning useful responsibility under increasing automation. It is not a claim that those tasks will remain exclusively human, or that security employment is protected from automation.

The contrary scenario matters. If tools make routine assessments much faster, an employer might need fewer people doing them. A consultant might face lower prices for a standard report. Learning only to operate a scanner or repeat a familiar exploit would leave your evidence concentrated in that narrow activity.

Practise the whole reasoning chain instead. When an AI assistant proposes a fix, ask what property it is supposed to restore, what observation would disprove the fix and which caller still needs investigation. Later essays will turn those questions into tests. Here they help you choose the work you want to learn.

## Make a decision you can revise

For this book, start with application/product security as the centre and applied security engineering as a nearby search direction. Your Java experience can contribute to both. The next evidence to build should show that you can identify a forbidden outcome, trace its cause and explain a repair.

That recommendation changes if the work itself does not suit you. You may discover that you prefer building deployment controls to investigating application behaviour, or running a bounded assessment to supporting ongoing product decisions. Record that preference after doing the work. A catalogue of courses cannot tell you whether you enjoy tracing an ambiguous authorization rule through an unfamiliar service.

The first learning decision is small: select a responsibility worth practising and name what would count as evidence. The next essay makes that evidence possible by turning a feature requirement into a security property.

<!--mission-->

## Practice: build your first role-to-evidence map

Reserve a session within your normal study week. No certification purchase or application is needed.

Choose three employer descriptions: an application/product security role, an applied security or security-focused software role, and a contrasting role you might plausibly consider. Find current pages on employers' own sites. If a vacancy has closed, you may use it as a clearly dated example of work, but not as an available opportunity.

For each, record the location and remote conditions, required experience, published compensation or “not disclosed,” and three responsibilities in your own words. Beside each responsibility, write an artifact you could already discuss and one gap that needs practice. Use “none yet” when necessary. Do not award yourself a fit percentage: the responsibilities differ in importance, and an average can hide a decisive gap.

Then change the scenario. Imagine that a client offers you a short assessment of the document-export service, without access to its background-worker code. Write what you could assess, what would remain uncertain, and how that limitation changes the deliverable. This tests whether you can translate a role's broad responsibility into a promise with boundaries.

If stuck, first underline verbs in the descriptions. Next ask what file, test, report or production change would show that the work happened. Finally, ask who decides what to do when the evidence is incomplete.

Your result should contain three role records and a short decision: one responsibility to practise first, a genuine existing strength, a missing artifact, and a condition that would make you change direction. For the client scenario, a sound answer keeps the worker outside the claimed code-review coverage and explains why end-to-end testing may still leave its internal policy uncertain. Declining the scope until access improves is also defensible.

You have completed the exercise when the next practice task follows from the evidence gap. “Learn cybersecurity” is still too broad. “Review how an export carries the user's authority into a worker, then test a revoked-access case” is something you can begin.

Source note: employer descriptions inspected on 13 September 2026. The export service, comparison framework and practice engagement are constructed teaching examples. This essay is an orientation and decision exercise; it contains no executed software lab.
