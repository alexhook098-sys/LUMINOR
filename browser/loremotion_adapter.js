const {
  createPage,
  evaluate,
  getPageInfo,
  closePage
} = require("../browser_worker");

const LORE_URL = process.env.ASTRA_LORE_URL || "https://loremotion.com/generate/";
const TURNSTILE_TIMEOUT_MS = Number(process.env.ASTRA_TURNSTILE_TIMEOUT_MS || "600000");

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function inspectLoreMotion(page) {
  return evaluate(page, `(() => ({
    url: location.href,
    title: document.title,
    text: document.body ? document.body.innerText : "",
    turnstileToken: document.querySelector('input[name="cf-turnstile-response"]')?.value || "",
    textareas: Array.from(document.querySelectorAll("textarea")).map((e,i)=>({
      i, placeholder:e.placeholder||"", disabled:!!e.disabled, value:e.value||""
    })),
    buttons: Array.from(document.querySelectorAll("button")).map((e,i)=>({
      i, text:(e.innerText||"").trim(), disabled:!!e.disabled,
      aria:e.getAttribute("aria-label")||""
    })).filter(x=>x.text||x.aria),
    videos: Array.from(document.querySelectorAll("video")).map(v=>({
      src:v.currentSrc||v.src||"",
      readyState:v.readyState||0,
      duration:Number.isFinite(v.duration)?v.duration:null
    })),
    mp4Links: Array.from(document.querySelectorAll("a")).map(a=>a.href||"")
      .filter(h=>/\\.mp4(?:$|[?#])/i.test(h))
  }))()`);
}

async function setPrompt(page, prompt) {
  return evaluate(page, `(() => {
    const el = document.querySelector("textarea");
    if (!el) throw new Error("LoreMotion textarea not found");
    el.focus();
    const setter = Object.getOwnPropertyDescriptor(
      HTMLTextAreaElement.prototype, "value"
    )?.set;
    if (setter) setter.call(el, ${JSON.stringify(prompt)});
    else el.value = ${JSON.stringify(prompt)};
    el.dispatchEvent(new Event("input",{bubbles:true}));
    el.dispatchEvent(new Event("change",{bubbles:true}));
    return el.value;
  })()`);
}

async function chooseAspect(page, ratio) {
  return evaluate(page, `(() => {
    const wanted = ${JSON.stringify(ratio)};
    const buttons = Array.from(document.querySelectorAll("button"));
    const b = buttons.find(x => (x.innerText||"").trim() === wanted);
    if (!b) throw new Error("Aspect ratio button not found: " + wanted);
    b.click();
    return (b.innerText||"").trim();
  })()`);
}

async function clickGenerate(page) {
  return evaluate(page, `(() => {
    const b = Array.from(document.querySelectorAll("button"))
      .find(x => /generate\\s+video/i.test((x.innerText||"").trim()) && !x.disabled);
    if (!b) throw new Error("Enabled Generate Video button not found");
    b.click();
    return true;
  })()`);
}

async function waitForVerification(page, timeoutMs=TURNSTILE_TIMEOUT_MS) {
  const started = Date.now();
  while (Date.now()-started < timeoutMs) {
    const o = await inspectLoreMotion(page);
    if (o.turnstileToken) return {ok:true, observation:o};
    await sleep(1000);
  }
  return {ok:false, reason:"TURNSTILE_TIMEOUT", observation:await inspectLoreMotion(page)};
}

async function waitForVideo(page, timeoutMs=300000) {
  const started = Date.now();
  while (Date.now()-started < timeoutMs) {
    const o = await inspectLoreMotion(page);
    const video = o.videos.find(v=>v.src);
    const link = o.mp4Links[0];
    if (link || video?.src) return {ok:true, url:link||video.src, observation:o};
    if (/generation failed|error/i.test(o.text)) {
      return {ok:false, reason:"GENERATION_ERROR", observation:o};
    }
    await sleep(2500);
  }
  return {ok:false, reason:"GENERATION_TIMEOUT", observation:await inspectLoreMotion(page)};
}

async function runLoreMotion({prompt, aspect="9:16", waitForManualTurnstile=true}={}) {
  if (!prompt || !prompt.trim()) throw new Error("Prompt is required");

  const {page, targetId} = await createPage(LORE_URL);
  try {
    await sleep(2500);
    await setPrompt(page, prompt.trim());
    await chooseAspect(page, aspect);

    const before = await inspectLoreMotion(page);

    if (waitForManualTurnstile) {
      const verification = await waitForVerification(page);
      if (!verification.ok) {
        return {status:"GENERATION_BLOCKED_BY_VERIFICATION", ...verification};
      }
    }

    await clickGenerate(page);
    const result = await waitForVideo(page);

    if (!result.ok) return {status:result.reason, observation:result.observation};
    return {status:"GENERATION_COMPLETE", videoUrl:result.url, observation:result.observation};
  } finally {
    await closePage(page, targetId);
  }
}

if (require.main === module) {
  const prompt = process.argv.slice(2).join(" ");
  runLoreMotion({prompt}).then(r => {
    console.log(JSON.stringify(r,null,2));
    process.exitCode = r.status === "GENERATION_COMPLETE" ? 0 : 2;
  }).catch(e => {
    console.error(e.stack || e.message);
    process.exitCode = 1;
  });
}

module.exports = {runLoreMotion, inspectLoreMotion};
