#!/usr/bin/env node

const puppeteer = require('puppeteer');

(async function run() {
  let failures = 0;

  console.log("START TESTS")

  const browser = await puppeteer.launch({headless: "new"});
  const page = await browser.newPage();

  console.log("NEW PAGE");

  page.on("console", async (m) => {
    if (m.type() === "error") {
      console.error(`${m.text()} in ${m.location().url}:${m.location().lineNumber}`);
    } else {
      const values = await Promise.all(m.args().map(h => h.jsonValue()));
      console.log(...values);
    }
  });

  console.log("await testsFailed");

  await page.exposeFunction("testsFailed", n => {
      failures = n;
    }
  );

  console.log("await testsDone");

  await page.exposeFunction("testsDone", async () => {
      await page.close();
      await browser.close();

      if (failures > 0) {
        process.exit(1);
      }
    }
  );

  console.log("RUNNING TESTS");

  await page.goto(`file://${process.argv[2]}/index.html`);

  console.log("BROWSER OPENED");
})();

setTimeout(() => {
  console.log("EXITING");
  process.exit(1);
}, 10000);
