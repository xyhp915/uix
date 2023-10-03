#!/usr/bin/env node

const puppeteer = require('puppeteer');

(async function run() {
  let failures = 0;

  const browser = await puppeteer.launch({headless: "new"});
  const page = await browser.newPage();

  page.on("console", async (m) => {
    if (m.type() === "error") {
      console.error(`${m.text()} in ${m.location().url}:${m.location().lineNumber}`);
    } else {
      const values = await Promise.all(m.args().map(h => h.jsonValue()));
      console.log(...values);
    }
  });

  await page.exposeFunction("testsFailed", n => {
      failures = n;
    }
  );

  await page.exposeFunction("testsDone", async () => {
      await page.close();
      await browser.close();

      if (failures > 0) {
        process.exit(1);
      }
    }
  );

  await page.goto(`file://${process.argv[2]}/index.html`);
})();
