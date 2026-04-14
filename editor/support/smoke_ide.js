/**
 * Smoke test that opens the IDE in headless Chrome and checks for JS errors.
 *
 * Launches a local HTTP server, navigates Puppeteer to index.html, and
 * listens for uncaught exceptions (pageerror) for a short window. Exits
 * with code 1 if any errors are detected.
 *
 * Usage: node support/smoke_ide.js
 *
 * @license BSD, see LICENSE.md.
 */

const http = require("http");
const fs = require("fs");
const path = require("path");
const puppeteer = require("puppeteer");
const {execSync} = require("child_process");

const PORT = 8089;
const WAIT_MS = 10000;

const MIME_TYPES = {
  ".html": "text/html",
  ".js": "text/javascript",
  ".css": "text/css",
  ".json": "application/json",
  ".wasm": "application/wasm",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".webm": "video/webm",
};

function startServer(root) {
  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      const urlPath = req.url.split("?")[0];
      const filePath = path.join(root, urlPath === "/" ? "index.html" : urlPath);

      fs.readFile(filePath, (err, data) => {
        if (err) {
          res.writeHead(404);
          res.end("Not found");
          return;
        }
        const ext = path.extname(filePath);
        const contentType = MIME_TYPES[ext] || "application/octet-stream";
        res.writeHead(200, {"Content-Type": contentType});
        res.end(data);
      });
    });

    server.listen(PORT, () => resolve(server));
  });
}

async function main() {
  const editorRoot = path.resolve(__dirname, "..");
  const server = await startServer(editorRoot);

  let chromiumPath = null;
  try {
    chromiumPath = execSync("which chromium", {encoding: "utf8"}).trim();
  } catch (e) {
    // Fall back to Puppeteer bundled browser.
  }

  const launchOptions = {
    headless: "new",
    args: ["--no-sandbox", "--disable-setuid-sandbox"],
  };
  if (chromiumPath) {
    launchOptions.executablePath = chromiumPath;
  }

  const browser = await puppeteer.launch(launchOptions);
  const page = await browser.newPage();

  const errors = [];

  page.on("pageerror", (err) => {
    errors.push(err.message);
  });

  console.log(`Opening IDE at http://localhost:${PORT} ...`);
  await page.goto(`http://localhost:${PORT}/index.html`, {
    waitUntil: "domcontentloaded",
  });

  console.log(`Waiting ${WAIT_MS / 1000} seconds for errors...`);
  await new Promise((resolve) => setTimeout(resolve, WAIT_MS));

  await browser.close();
  server.close();

  if (errors.length > 0) {
    console.error(`\nFAILED: ${errors.length} uncaught error(s) detected:\n`);
    errors.forEach((msg, i) => console.error(`  ${i + 1}. ${msg}`));
    process.exit(1);
  } else {
    console.log("\nPASSED: No uncaught errors detected.");
  }
}

main().catch((err) => {
  console.error("Smoke test crashed:", err);
  process.exit(1);
});
