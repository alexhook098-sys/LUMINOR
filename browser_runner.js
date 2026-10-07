const {
  createPage,
  evaluate,
  getPageInfo,
  getText,
  getLinks,
  click,
  type,
  closePage
} = require("./browser_worker");

async function runAction(page, action) {
  const name = String(action.action || "").toUpperCase();

  switch (name) {
    case "TYPE":
      return {
        action: name,
        result: await type(
          page,
          action.selector,
          action.text || ""
        )
      };

    case "CLICK":
      return {
        action: name,
        result: await click(
          page,
          action.selector
        )
      };

    case "READ":
      return {
        action: name,
        result: await getText(page)
      };

    case "LINKS":
      return {
        action: name,
        result: await getLinks(page)
      };

    case "INFO":
      return {
        action: name,
        result: await getPageInfo(page)
      };

    default:
      throw new Error(
        "Unknown action: " + name
      );
  }
}

async function runActions(page, actions) {
  const results = [];

  for (const action of actions) {
    console.log(
      "[RUNNER] ACTION:",
      action.action
    );

    const result = await runAction(
      page,
      action
    );

    results.push(result);

    console.log(
      "[RUNNER] RESULT:",
      JSON.stringify(result)
    );
  }

  return results;
}

async function main() {
  let page = null;
  let targetId = null;

  try {
    console.log("[RUNNER] Starting...");

    const created = await createPage(
      "about:blank"
    );

    page = created.page;
    targetId = created.targetId;

    console.log(
      "[RUNNER] BROWSER READY"
    );

    // Локальная тестовая страница
    await evaluate(
      page,
      `
      document.title = "ASTRA JSON Runner Test";

      document.body.innerHTML = \`
        <h1>ASTRA JSON RUNNER</h1>

        <input id="name">

        <button id="hello">
          TEST
        </button>

        <div id="result"></div>
      \`;

      document.querySelector("#hello").onclick = () => {
        const name =
          document.querySelector("#name").value;

        document.querySelector("#result").innerText =
          "JSON COMMAND OK: " + name;
      };
      `
    );

    const commands = {
      actions: [
        {
          action: "TYPE",
          selector: "#name",
          text: "ASTRA"
        },
        {
          action: "CLICK",
          selector: "#hello"
        },
        {
          action: "READ"
        }
      ]
    };

    console.log(
      "[RUNNER] EXECUTING JSON..."
    );

    const results = await runActions(
      page,
      commands.actions
    );

    console.log(
      "[RUNNER] FINAL JSON:"
    );

    console.log(
      JSON.stringify(
        {
          ok: true,
          results
        },
        null,
        2
      )
    );

    console.log(
      "[RUNNER] SUCCESS"
    );

  } catch (error) {

    console.error(
      "[RUNNER] ERROR:",
      error.stack || error.message
    );

    process.exitCode = 1;

  } finally {

    if (page && targetId) {
      await closePage(
        page,
        targetId
      );

      console.log(
        "[RUNNER] CLOSED"
      );
    }
  }
}

if (require.main === module) {
  main();
}

module.exports = {
  runAction,
  runActions
};
