
const ALLOWED_ACTIONS = new Set(["TYPE", "CLICK", "READ", "LINKS", "INFO"]);

function extractJson(text) {
  if (typeof text !== "string") {
    throw new Error("LLM returned non-text content");
  }

  let s = text.trim();

  if (s.startsWith("```")) {
    s = s.replace(/^```(?:json)?\s*/i, "")
         .replace(/\s*```$/, "")
         .trim();
  }

  const start = s.indexOf("{");
  const end = s.lastIndexOf("}");

  if (start < 0 || end <= start) {
    throw new Error("No JSON object found in LLM response");
  }

  return JSON.parse(s.slice(start, end + 1));
}

function observedSelectors(observation) {
  const set = new Set();

  for (const el of observation?.elements || []) {
    if (el?.id) {
      set.add(`#${el.id}`);
    }

    if (el?.name) {
      set.add(
        `[name="${String(el.name).replace(/"/g, '\\"')}"]`
      );
    }
  }

  return set;
}

function validatePlan(plan, observation) {
  if (!plan || !Array.isArray(plan.actions)) {
    throw new Error("Plan must contain actions array");
  }

  if (plan.actions.length > 8) {
    throw new Error("Plan has too many actions");
  }

  const selectors = observedSelectors(observation);
  const elements = observation?.elements || [];

  function hasSelectorElement(selector) {
    if (!selector) return false;

    if (selectors.has(selector)) {
      return true;
    }

    return false;
  }

  for (const action of plan.actions) {
    if (!action || !ALLOWED_ACTIONS.has(action.action)) {
      throw new Error(
        `Unsafe or unknown action: ${action?.action}`
      );
    }

    if (
      (action.action === "TYPE" ||
       action.action === "CLICK") &&
      !hasSelectorElement(action.selector)
    ) {
      throw new Error(
        `Selector not present in observation: ${action.selector}`
      );
    }

    if (action.action === "TYPE") {
      if (typeof action.text !== "string") {
        throw new Error("TYPE requires text");
      }

      if (action.text.length > 1000) {
        throw new Error("TYPE text too long");
      }
    }
  }

  return plan;
}

function buildPrompt(goal, observation) {
  return `You are ASTRA's browser planner.
Return ONLY valid JSON.

Goal:
${goal}

Current browser observation:
${JSON.stringify(observation, null, 2)}

Allowed actions:

TYPE:
{"action":"TYPE","selector":"...","text":"..."}

CLICK:
{"action":"CLICK","selector":"..."}

READ:
{"action":"READ"}

LINKS:
{"action":"LINKS"}

INFO:
{"action":"INFO"}

Rules:

1. Use only selectors that exist in the observation.
2. Do not invent elements or selectors.
3. Do not use JavaScript.
4. Do not use EVALUATE.
5. Maximum 8 actions.
6. If the goal cannot safely be completed, return:
{"actions":[]}

Return JSON only.`;
}

async function callOpenRouter(prompt) {
  const key = process.env.OPENROUTER_API_KEY;

  if (!key) {
    throw new Error("OPENROUTER_API_KEY is not set");
  }

  const model =
    process.env.ASTRA_LLM_MODEL ||
    "openrouter/free";

  const response = await fetch(
    "https://openrouter.ai/api/v1/chat/completions",
    {
      method: "POST",

      headers: {
        "Authorization": `Bearer ${key}`,
        "Content-Type": "application/json",
        "HTTP-Referer":
          "https://github.com/alexhook098-sys/astra",
        "X-Title": "ASTRA Browser Agent"
      },

      body: JSON.stringify({
        model,
        temperature: 0,

        messages: [
          {
            role: "system",
            content:
              "You are a strict JSON browser planning engine."
          },
          {
            role: "user",
            content: prompt
          }
        ]
      })
    }
  );

  if (!response.ok) {
    const body = await response.text();

    throw new Error(
      `OpenRouter HTTP ${response.status}: ${body.slice(0, 500)}`
    );
  }

  const data = await response.json();

  const content =
    data?.choices?.[0]?.message?.content;

  if (!content) {
    throw new Error(
      "OpenRouter returned no message content"
    );
  }

  return content;
}

async function createPlan(goal, observation) {
  console.log("[LLM PLANNER] GOAL:", goal);

  const prompt =
    buildPrompt(goal, observation);

  const raw =
    await callOpenRouter(prompt);

  const plan =
    extractJson(raw);

  const safe =
    validatePlan(plan, observation);

  console.log("[LLM PLANNER] PLAN:");
  console.log(
    JSON.stringify(safe, null, 2)
  );

  return safe;
}

module.exports = {
  createPlan,
  buildPrompt,
  extractJson,
  validatePlan
};
