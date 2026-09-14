# Fetch authority review

Use after the [worksheet](08-fetch-worksheet.md). A supplied explanation or reference result is not independent learner evidence.

The initial URL can be valid while its response directs the client somewhere else. The vulnerable configuration authorizes only the first URI and then automatically follows. Direct internal access is denied, but the partner redirect causes one internal hit and returns the report. No API credential needs to reach that internal server.

The guided repair disables following and accepts only upstream 200. It preserves direct partner documents and rejects all redirects. It does not promise that legitimate partner redirects work. The one-hop exercise changes that product requirement without removing the destination boundary.

A satisfactory extension validates the initial URI, makes one request, resolves a 302 Location, validates the resulting URI and then makes at most one follow-up. A final redirect is rejected rather than followed again. Validate before connecting; hiding a returned body after contacting the internal fixture is too late.

Keep error mapping explicit: initial caller destination denial is 403; failure to obtain an acceptable final upstream result is 502. These are the fixture's contract, not mandatory universal SSRF response codes. Use the existing response-status mechanism or a dedicated exception mapped by the controller.

The public starter has four passing exercise cases and one failed legitimate-relative-redirect case. The private reference passes all five plus the twenty-two guided cases. Its negative variant removes redirected-target validation: the internal-target test fails with actual 200 and one recorded internal hit. Other exercise cases still pass. This verifies the property the supplied test can distinguish.

The loop and chain tests enforce an upper bound of two partner hits, not an obligation to issue a second request when denial can happen earlier. The relative-success test does require two hits and the expected document body, so rejecting every redirect cannot satisfy the whole contract.

Your changed regression should test something the supplied five cases do not. Missing Location or a different URI shape is useful; copying the same test with a new method name is not. Keep synthetic destinations local, and show that the relevant regression fails against code that violates your stated rule.

The fixture uses a literal loopback address, fixed ports and fixed redirect handlers. It does not test a DNS race, separate network reachability, egress policy or production credentials. The response-body and concurrency limits of a production fetcher are also unverified. Passing tests support the stated redirect behavior only.

An effective handover distinguishes a working destination check from a complete network security assessment. Preserve actual study time and assistance records; carry unfinished practice forward rather than declaring mastery to fit the week.
