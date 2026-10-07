
const {
  evaluate
} = require("./browser_worker");


async function inspectPage(page) {

  const result = await evaluate(
    page,
    `
    (() => {

      function clean(value) {
        return String(value || "")
          .replace(/\\s+/g, " ")
          .trim();
      }

      function visible(el) {

        const style =
          window.getComputedStyle(el);

        const rect =
          el.getBoundingClientRect();

        return (
          style.display !== "none" &&
          style.visibility !== "hidden" &&
          rect.width > 0 &&
          rect.height > 0
        );
      }

      const elements = [];

      const selector =
        "input, textarea, select, button, a, [role], [contenteditable='true']";

      document
        .querySelectorAll(selector)
        .forEach((el, index) => {

          if (!visible(el)) {
            return;
          }

          const rect =
            el.getBoundingClientRect();

          elements.push({

            index,

            tag:
              el.tagName.toLowerCase(),

            id:
              el.id || "",

            name:
              el.getAttribute("name") || "",

            type:
              el.getAttribute("type") || "",

            role:
              el.getAttribute("role") || "",

            text:
              clean(el.innerText),

            value:
              "value" in el
                ? String(el.value || "")
                : "",

            placeholder:
              el.getAttribute("placeholder") || "",

            ariaLabel:
              el.getAttribute("aria-label") || "",

            href:
              el.href || "",

            disabled:
              !!el.disabled,

            x:
              Math.round(rect.x),

            y:
              Math.round(rect.y),

            width:
              Math.round(rect.width),

            height:
              Math.round(rect.height)
          });
        });

      return {

        title:
          document.title || "",

        url:
          location.href || "",

        text:
          document.body
            ? document.body.innerText || ""
            : "",

        elements

      };

    })()
    `
  );

  return result;
}


module.exports = {
  inspectPage
};
