# agent-access-control

[![Build Status](https://travis-ci.org/hmrc/agent-access-control.svg?branch=master)](https://travis-ci.org/hmrc/agent-access-control) [ ![Download](https://api.bintray.com/packages/hmrc/releases/agent-access-control/images/download.svg) ](https://bintray.com/hmrc/releases/agent-access-control/_latestVersion)

Delegated auth rules for [auth-client](https://github.com/hmrc/auth-client) library to allow access
to agents to their clients's data. Currently supports:
* PAYE (IR-PAYE)
* Self-Assessment (IR-SA)
* MTD Self-Assessment (MTDITID)
* MTD Value Added Tax (VAT)
* MTD Income Record Viewer (AFI)
* MTD Trusts (TERS)
* MTD Capital Gains (CGT)


### Testing
In Terminal, Run the following profile:
```
sm --start AGENT_AUTHORISATION -r
```
1. Log In as Agent

2. Create Relationship Via Curl using Agent Arn, Client Service and Identifier.
```markdown
curl -v -X PUT -H 'Authorization: Bearer Token' http://localhost:9434/agent-client-relationships/agent/AARN0002908/service/HMRC-MTD-VAT/client/VRN/267729808
```

3. Curl Agent Access Control To Test Relationship Exists. E.g:
```markdown
curl -v -H'Authorization: Bearer Token' http://localhost:9431/agent-access-control/mtd-vat-auth/agent/9AK6XC1JX8NE/client/267729808
```

Alternatively create the relationship via UI (Please speak to team about this).

### Endpoints

##### GET /agent-access-control/epaye-auth/agent/:agentCode/client/:empRef

### Example usage
```scala
authorised(
   Enrolment("IR-PAYE")
     .withIdentifier("TaxOfficeNumber", "123")
     .withIdentifier("TaxOfficeReference", "123")
     .withDelegatedAuthRule("epaye-auth")) { // your protected logic }
```

##### GET /agent-access-control/sa-auth/agent/:agentCode/client/:saUtr

### Example usage
```scala
authorised(
   Enrolment("IR-SA")
     .withIdentifier("UTR", "123")
     .withDelegatedAuthRule("sa-auth")) { // your protected logic }
```


##### GET /agent-access-control/mtd-it-auth/agent/:agentCode/client/:mtdItId

### Example usage
```scala
authorised(
   Enrolment("HMRC-MTD-IT")
     .withIdentifier("MTDITID", "123")
     .withDelegatedAuthRule("mtd-it-auth")) { // your protected logic }
```

##### GET /agent-access-control/mtd-vat-auth/agent/:agentCode/client/:vrn

### Example usage
```scala
authorised(
   Enrolment("HMRC-MTD-VAT")
     .withIdentifier("VRN", "123")
     .withDelegatedAuthRule("mtd-vat-auth")) { // your protected logic }
```

##### GET /agent-access-control/afi-auth/agent/:agentCode/client/:nino

### Example usage
```scala
authorised(
   Enrolment("HMRC-NI")
     .withIdentifier("NINO", "123")
     .withDelegatedAuthRule("afi-auth")) { // your protected logic }
```

##### GET /agent-access-control/trust-auth/agent/:agentCode/client/:utr

### Example usage
```scala
authorised(
   Enrolment("HMRC-TERS-ORG")
     .withIdentifier("SAUTR", "123")
     .withDelegatedAuthRule("trust-auth")) { // your protected logic }
```

##### GET /agent-access-control/cgt-auth/agent/:agentCode/client/:cgtRef

### Example usage
```scala
authorised(
   Enrolment("HMRC-CGT-PD")
     .withIdentifier("CGTPDRef", "123")
     .withDelegatedAuthRule("cgt-auth")) { // your protected logic }
```

Note: POSTs function exactly the same.

### Response
Headers: need to contain a valid `Authorization` header.

Each API Will Respond with the following:

code | scenario
---- | ---
200 | _{client identifier}_ is assigned to logged in agent for {Supported Service} (Enrolment Store Proxy check) AND the logged in user's agency (_agentCode_) has a valid authorisation to act on behalf of _{client identifier}_ for dealing with {Supported Service} (HODs check).
401 | The conditions are not met.
502 | In case of any error responses in downstream services.
504 | In case of a timeout while querying downstream services.

##### GET /ping/ping

Always `200` with empty body.

##### GET /admin/metrics

Displays metrics as JSON.

##### GET /admin/details

Displays `META-INF/MANIFEST.MF` as JSON.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
