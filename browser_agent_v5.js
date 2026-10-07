
const {
  createPage,
  evaluate,
  closePage
} = require("./browser_worker");

const {
  inspectPage
} = require("./browser_inspector");

const {
  createPlan
} = require("./browser_llm_planner");

const {
  runActions
} = require("./browser_runner");


async function observe(page) {
  return await inspectPage(page);
}


function goalComplete(goal, observation) {
  const text =
    String(observation?.text || "");

  return (
    text.includes("ASTRA RESULT")
  );
}


async function agentLoop(page, goal) {
  for (let step = 1; step <= 5; step++) {

    console.log(
      `\n[AGENT V3] ===== STEP ${step} =====`
    );

    const observation =
      await observe(page);

    console.log(
      "[AGENT V3] OBSERVATION:"
    );

    console.log(
      JSON.stringify(
        observation,
        null,
        2
      )
    );


    if (
      goalComplete(
        goal,
        observation
      )
    ) {
      console.log(
        "[AGENT V3] GOAL COMPLETE"
      );

      return {
        success: true,
        step,
        observation
      };
    }


    const plan =
      await createPlan(
        goal,
        observation
      );


    if (
      !plan.actions.length
    ) {
      throw new Error(
        "Planner V3 returned empty plan."
      );
    }


    console.log(
      "[AGENT V3] EXECUTING PLAN"
    );

    await runActions(
      page,
      plan.actions
    );
  }


  throw new Error(
    "Maximum agent steps exceeded."
  );
}


async function runTest(
  name,
  goal,
  html
) {
  console.log(
    `\n\n========== ${name} ==========`
  );

  const pageHandle =
    await createPage(
      "about:blank"
    );

  const page =
    pageHandle.page;

  try {

    await evaluate(
      page,
      `
      document.open();
      document.write(${JSON.stringify(html)});
      document.close();
      true;
      `
    );

    const result =
      await agentLoop(
        page,
        goal
      );

    console.log(
      "[AGENT V3] FINAL RESULT:"
    );

    console.log(
      JSON.stringify(
        result,
        null,
        2
      )
    );

    return result;

  } finally {

    await closePage(page);

    console.log(
      "[AGENT V3] PAGE CLOSED"
    );
  }
}


async function main() {

  console.log(
    "[AGENT V3] ASTRA Browser Agent v3"
  );


  const greetingHTML = `
    <!doctype html>
    <html>
      <head>
        <title>ASTRA Greeting V3</title>
      </head>

      <body>

        <h1>Добро пожаловать</h1>

        <input
          id="userName"
          name="username"
          placeholder="Ваше имя"
        >

        <button
          id="sendGreeting"
          onclick="
            document.body.insertAdjacentHTML(
              'beforeend',
              '<div>ASTRA RESULT: Привет, ' +
              document.querySelector('#userName').value +
              '</div>'
            )
          "
        >
          Представиться
        </button>

      </body>
    </html>
  `;


  const searchHTML = `
    <!doctype html>
    <html>
      <head>
        <title>ASTRA Search V3</title>
      </head>

      <body>

        <h1>Поиск</h1>

        <input
          id="queryBox"
          name="query"
          placeholder="Что найти?"
        >

        <button
          id="searchButton"
          onclick="
            document.body.insertAdjacentHTML(
              'beforeend',
              '<div>ASTRA RESULT: найдено ' +
              document.querySelector('#queryBox').value +
              '</div>'
            )
          "
        >
          Найти
        </button>

      </body>
    </html>
  `;


  await runTest(
    "TEST 1 — GREETING",
    "Представиться сайту",
    greetingHTML
  );


  await runTest(
    "TEST 2 — SEARCH",
    "Найди ASTRA",
    searchHTML
  );


  console.log(
    "\n[AGENT V3] ALL TESTS COMPLETE"
  );
}


main().catch(error => {

  console.error(
    "[AGENT V3] ERROR:",
    error
  );

  process.exit(1);
});
