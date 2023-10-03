#!/usr/bin/env node

const puppeteer = require('puppeteer');

(async function run() {
  let failures = 0;

  console.log("START TESTS")

  const browser = await puppeteer.launch();
  const page = await browser.newPage();

  console.log("NEW PAGE");

  page.on("console", m => {
    if (m.type() === "error") {
      console.error(`${m.text()} in ${m.location().url}:${m.location().lineNumber}`);
    } else {
      console.log(...m.args().map(a => a._remoteObject.value));
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
