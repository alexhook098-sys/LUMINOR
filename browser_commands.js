const {
  createPage,
  open,
  evaluate,
  getPageInfo,
  getText,
  getLinks,
  click,
  type,
  closePage
} = require("./browser_worker");

async function runCommand(page, command) {
  const action = String(command.action || "").toUpperCase();

  switch (action) {
    case "OPEN":
      return await open(page, command.url);

    case "READ":
      return await getText(page);

    case "LINKS":
      return await getLinks(page);

    case "CLICK":
      return await click(page, command.selector);

    case "TYPE":
      return await type(
        page,
        command.selector,
        command.text || ""
      );

    case "EVALUATE":
      return await evaluate(
        page,
        command.script || "null"
      );

    case "INFO":
      return await getPageInfo(page);

    default:
      throw new Error(
        "Unknown browser action: " + action
      );
  }
}

async function main() {
  let page = null;
  let targetId = null;

  try {
    console.log("[COMMAND] Starting...");

    const result = await createPage("about:blank");

    page = result.page;
    targetId = result.targetId;

    console.log("[COMMAND] BROWSER READY");

    // Локальная тестовая страница
    await evaluate(
      page,
      `
      document.title = "ASTRA Command Test";

      document.body.innerHTML = \`
        <h1>ASTRA COMMAND INTERFACE</h1>

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
          "COMMAND OK: " + name;
      };
      `
    );

    // TYPE
    console.log("[COMMAND] TYPE");

    console.log(
      await runCommand(page, {
        action: "TYPE",
        selector: "#name",
        text: "ASTRA"
      })
    );

    // CLICK
    console.log("[COMMAND] CLICK");

    console.log(
      await runCommand(page, {
        action: "CLICK",
        selector: "#hello"
      })
    );

    // READ
    console.log("[COMMAND] READ");

    console.log(
      await runCommand(page, {
        action: "READ"
      })
    );

    // INFO
    console.log("[COMMAND] INFO");

    console.log(
      await runCommand(page, {
        action: "INFO"
      })
    );

    console.log("[COMMAND] SUCCESS");

  } catch (error) {

    console.error(
      "[COMMAND] ERROR:",
      error.stack || error.message
    );

    process.exitCode = 1;

  } finally {

    if (page && targetId) {
      await closePage(page, targetId);
      console.log("[COMMAND] CLOSED");
    }
  }
}

if (require.main === module) {
  main();
}

module.exports = {
  runCommand
};
