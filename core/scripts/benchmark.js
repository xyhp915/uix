#!/usr/bin/env node

const puppeteer = require('puppeteer');

(async function run() {

  const browser = await puppeteer.launch({headless: "new"});
  const page = await browser.newPage();

  page.on("pageerror", console.error);

  page.on("console", async (m) => {
    if (m.type() === "error") {
      console.error(`${m.text()} in ${m.location().url}:${m.location().lineNumber}`);
    } else {
      const values = await Promise.all(m.args().map(h => h.jsonValue()));
      console.log(...values);
    }
  });

  await page.exposeFunction("testsDone", async ([react, uix, reagent]) => {
      await browser.close();
    }
  );

  await page.goto(`file://${process.argv[2]}/index.html`);
})();
