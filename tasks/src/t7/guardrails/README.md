# AI Guardrails

A hands-on security task where you will first act as an **attacker** trying to steal PII from an AI system,
then progressively harden that system using different guardrail techniques.

The scenario: a colleague directory assistant has access to a user profile with sensitive PII. Your job is to
break it — and then protect it.

> **Important:** Guardrails do not eliminate the possibility of being hacked. They raise the cost and difficulty
> of an attack and reduce the attack surface — but a determined attacker with enough creativity can often find
> a way through. The goal is to make your AI application significantly harder to exploit, not perfectly invulnerable.

## Learning Goals

- Understand real prompt injection attack vectors and why they work
- Craft and iteratively harden a system prompt as the first line of defense
- Experience the attacker's perspective before building defenses
- Implement input validation to block malicious prompts before they reach the LLM
- Implement output validation to catch PII leaks after the LLM responds
- Build real-time streaming filters for sensitive data redaction
- Understand the trade-offs and limitations of each guardrail layer

---

## Task 1: System Prompt Hardening [t1/PromptInjection.java](t1/PromptInjection.java)

A system prompt is the **first and most fundamental guardrail** in any AI application. Before adding any
technical layer (validators, filters, firewalls), you need a well-crafted system prompt that clearly defines
what the model is and is not allowed to do. Everything else builds on top of this foundation.

Open [t1/PromptInjection.java](t1/PromptInjection.java). The file contains a `SYSTEM_PROMPT` and a `PROFILE`
with Amanda Grace Johnson's sensitive data. Run the assistant.

**Your task — two parts:**

**Part A — Attack.** Try to steal PII (credit card, SSN, address, bank account, etc.) from the profile.
Use the attacks from [prompt_injections.md](prompt_injections.md) as a starting point, then invent your own
variants. Note which attacks succeed and which fail.

**Part B — Defend.** Edit `SYSTEM_PROMPT` to make it harder to steal PII. Then go back to attacking your
own prompt. Iterate until you reach a point where most attacks from [prompt_injections.md](prompt_injections.md)
and your own variants consistently fail to extract sensitive data.

**Questions to think about:**
- Which attack categories are most effective against a system-prompt-only defense?
- Does the model ever partially reveal restricted data?
- After hardening, are there still attacks that work? What does that tell you about the limits of this approach?

> A strong system prompt is necessary but not sufficient, which is exactly why the next tasks exist.

---

## Task 2: Input Validation Guardrail [t2/InputLlmBasedValidation.java](t2/InputLlmBasedValidation.java)

An LLM-based guardrail that inspects every user message **before** it reaches the assistant. Suspicious
inputs are blocked and never processed.

Open [t2/InputLlmBasedValidation.java](t2/InputLlmBasedValidation.java) and explore how it works:
- A separate validation LLM call classifies the input as safe or malicious
- Uses `ResponseFormatJsonObject` to return a `Validation` record with `valid` flag and `description`
- Blocked inputs receive an immediate rejection without touching the assistant

**Your task:** Repeat your attacks from Task 1. Observe which ones are now blocked, and try to craft inputs
that slip past the input validator while still tricking the assistant.

**Pros:** Stops most known attack patterns before they reach the LLM; easy to tune the validation prompt
**Cons:** Adds latency and cost; creative or indirect attacks may bypass it; false positives on legitimate queries

---

## Task 3: Output Validation Guardrail [t3/OutputLlmBasedValidation.java](t3/OutputLlmBasedValidation.java)

A guardrail that inspects the **assistant's response** for PII leaks after the LLM has already replied.
Operates in two modes controlled by the `softResponse` flag.

Open [t3/OutputLlmBasedValidation.java](t3/OutputLlmBasedValidation.java) and explore how it works:
- The assistant responds normally; a separate validation LLM call then checks the response for PII
- **Hard mode** (`softResponse=false`): blocks the response entirely and appends a rejection to history
- **Soft mode** (`softResponse=true`): routes the leaking response through a PII-filtering LLM that
  redacts sensitive fields with placeholders (e.g. `[CREDIT CARD REDACTED]`) before showing it to the user

**Your task:** Try attacks that previously succeeded. See whether the output validator catches the leak.
Then try to craft a response that leaks PII in a format the validator might miss (obfuscated, encoded, split
across sentences, embedded in code blocks, etc.).

**Pros:** Catches leaks that bypassed input validation; soft mode preserves usefulness while redacting PII
**Cons:** Adds a second LLM call per response; clever obfuscation in the output may evade the validator

---

## Task 4: Streaming PII Guardrail [t4/StreamingPiiGuardrail.java](t4/StreamingPiiGuardrail.java)

A real-time filter that redacts PII **as tokens stream in**, without waiting for the full response.
Two implementations are provided side by side.

Open [t4/StreamingPiiGuardrail.java](t4/StreamingPiiGuardrail.java) and explore how it works:
- **`StreamingPiiGuardrail`** — regex-based: buffers incoming chunks and applies PII patterns (SSN, credit card,
  address, dates, CVV, etc.) before flushing safe content to the user
- **`PresidioStreamingPiiGuardrail`** — NLP-based: delegates detection to a lightweight FastAPI microservice
  that wraps Microsoft Presidio + spaCy. It POSTs each buffer flush to `/redact` on the service and streams
  back the anonymised text.

> **Note:** Microsoft Presidio is a Python library and has no Java equivalent. Instead of running Presidio
> in-process (as the Python version does), the Java implementation calls a dedicated FastAPI microservice
> (`t4/pii/service/`) that runs Presidio inside Docker. The Java class communicates with it over HTTP.

Both use a sliding buffer with a safety margin to handle PII that may be split across chunk boundaries.

### Starting the Presidio microservice

The `PresidioStreamingPiiGuardrail` class requires the Docker service to be running before you start the app.

**Prerequisites:** Docker must be installed and running.

```bash
# From the t4/ directory:
docker-compose up
```

This builds and starts a FastAPI service on **port 8060** that loads Presidio + spaCy `en_core_web_sm`.
The first startup takes a minute to download the spaCy model. When ready, you will see:

```
PII service ready — Presidio + spaCy en_core_web_sm loaded.
```

You can verify it is healthy with:

```bash
curl http://localhost:8060/health
# {"status":"ok"}
```

To stop the service:

```bash
docker-compose down
```

**Your task:** Run the streaming assistant and try the three suggested queries printed at startup. Observe
what gets redacted and what slips through. Consider: can you craft a prompt that causes PII to appear in
a format neither implementation handles?

**Pros:** Low latency — safe chunks are shown to the user in real time; no extra LLM calls needed
**Cons:** Regex patterns require maintenance; NLP models have entity recognition limits; PII split in unusual
ways across chunks may slip through

---

**Congratulations! You've seen both sides — attacking and defending. Now you understand why security
is a layered problem: no single guardrail is enough, and each layer catches what the others miss!**
