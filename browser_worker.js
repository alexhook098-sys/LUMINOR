const CDP = require("chrome-remote-interface");

const HOST = process.env.ASTRA_BROWSER_HOST || "127.0.0.1";
const PORT = Number(process.env.ASTRA_BROWSER_PORT || "9222");

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function createPage(url = "about:blank") {
  const browser = await CDP({
    host: HOST,
    port: PORT
  });

  const target = await browser.Target.createTarget({ url });
  const targetId = target.targetId;

  console.log("[BROWSER] TARGET:", targetId);

  await browser.close();

  const page = await CDP({
    host: HOST,
    port: PORT,
    target: targetId
  });

  await page.Page.enable();
  await page.Runtime.enable();

  return { page, targetId };
}

async function evaluate(page, expression) {
  const result = await page.Runtime.evaluate({
    expression,
    returnByValue: true,
    awaitPromise: true
  });

  if (result.exceptionDetails) {
    throw new Error(
      result.exceptionDetails.text || "JavaScript execution failed"
    );
  }

  return result.result.value;
}

async function open(page, url, waitMs = 1500) {
  console.log("[BROWSER] OPEN:", url);

  const expression = `
    (() => {
      location.href = ${JSON.stringify(url)};
      return location.href;
    })()
  `;

  const result = await evaluate(page, expression);

  console.log("[BROWSER] NAV STARTED:", result);

  await sleep(waitMs);

  return getPageInfo(page);
}

async function getPageInfo(page) {
  return evaluate(
    page,
    `JSON.stringify({
      title: document.title || "",
      url: location.href || "",
      text: document.body ? document.body.innerText : ""
    })`
  );
}

async function getText(page) {
  return evaluate(
    page,
    `document.body ? document.body.innerText : ""`
  );
}

async function getLinks(page) {
  return evaluate(
    page,
    `JSON.stringify(
      Array.from(document.querySelectorAll("a")).map(a => ({
        text: (a.innerText || "").trim(),
        href: a.href || ""
      })).filter(x => x.href)
    )`
  );
}


async function click(page, selector) {
  return evaluate(
    page,
    `
    (() => {
      const el = document.querySelector(${JSON.stringify(selector)});

      if (!el) {
        throw new Error("Element not found: " + ${JSON.stringify(selector)});
      }

      el.click();
      return true;
    })()
    `
  );
}

async function type(page, selector, text) {
  return evaluate(
    page,
    `
    (() => {
      const el = document.querySelector(${JSON.stringify(selector)});

      if (!el) {
        throw new Error("Element not found: " + ${JSON.stringify(selector)});
      }

      el.focus();

      if ("value" in el) {
        el.value = ${JSON.stringify(text)};
      }

      el.dispatchEvent(new Event("input", { bubbles: true }));
      el.dispatchEvent(new Event("change", { bubbles: true }));

      return el.value || "";
    })()
    `
  );
}

async function closePage(page, targetId) {
  try {
    await page.close();
  } catch {}

  try {
    const browser = await CDP({
      host: HOST,
      port: PORT
    });

    await browser.Target.closeTarget({
      targetId
    });

    await browser.close();
  } catch {}
}

async function main() {
  let page = null;
  let targetId = null;

  try {
    console.log("[BROWSER] Starting v3...");

    const result = await createPage("about:blank");

    page = result.page;
    targetId = result.targetId;

    console.log("[BROWSER] CONNECTED");
    console.log("[BROWSER] RUNTIME READY");

    await evaluate(
      page,
      `
      document.title = "ASTRA Browser Agent Test";

      document.body.innerHTML = \`
        <h1>ASTRA Browser Worker v3</h1>

        <input
          id="name"
          placeholder="Введите имя"
        >

        <button id="hello">
          Поздороваться
        </button>

        <div id="result"></div>
      \`;

      document.querySelector("#hello").addEventListener(
        "click",
        () => {
          const name =
            document.querySelector("#name").value;

          document.querySelector("#result").innerText =
            "Привет, " + name + "! ASTRA работает.";
        }
      );
      `
    );

    await sleep(200);

    console.log("[BROWSER] PAGE READY");

    console.log("[BROWSER] TYPE...");

    await type(
      page,
      "#name",
      "ASTRA"
    );

    console.log("[BROWSER] TYPE OK");

    console.log("[BROWSER] CLICK...");

    await click(
      page,
      "#hello"
    );

    console.log("[BROWSER] CLICK OK");

    await sleep(200);

    console.log("[BROWSER] RESULT:");

    const text = await getText(page);

    console.log(text);

    console.log("[BROWSER] INFO:");

    console.log(
      await getPageInfo(page)
    );

    console.log("[BROWSER] SUCCESS");
    console.log(
      "[BROWSER] ASTRA CAN TYPE AND CLICK."
    );

  } catch (error) {

    console.error(
      "[BROWSER] ERROR:",
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
        "[BROWSER] CLOSED"
      );
    }
  }
}

if (require.main === module) {
  main();
}

module.exports = {
  createPage,
  open,
  evaluate,
  getPageInfo,
  getText,
  getLinks,
  click,
  type,
  closePage
};
