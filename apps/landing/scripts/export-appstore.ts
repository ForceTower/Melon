import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import puppeteer from "puppeteer-core";

type Deck = {
  dir: string;
  html: string;
  width: number;
  height: number;
  slides: readonly string[];
};

const DECKS: readonly Deck[] = [
  {
    dir: "iphone",
    html: "UNES App Store.html",
    width: 1284,
    height: 2778,
    slides: [
      "01-hero",
      "02-final-countdown",
      "03-disciplinas",
      "04-horario",
      "05-notificacoes",
      "06-granularidade",
      "07-perfil",
      "08-tudo-conectado",
      "09-numeros",
      "10-baixar",
    ],
  },
  {
    dir: "ipad",
    html: "UNES iPad App Store.html",
    width: 2048,
    height: 2732,
    slides: [
      "01-hero",
      "02-disciplinas",
      "03-horario",
      "04-final-countdown",
      "05-cada-materia",
      "06-notificacoes",
    ],
  },
];

const CHROME_CANDIDATES = [
  "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
  "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
  "/usr/bin/google-chrome",
  "/usr/bin/chromium",
];

const findChrome = () => {
  const fromEnv = process.env.CHROME_PATH;
  if (fromEnv && existsSync(fromEnv)) return fromEnv;
  const found = CHROME_CANDIDATES.find((p) => existsSync(p));
  if (!found) {
    throw new Error("Chrome not found. Set CHROME_PATH or install Chrome to a standard location.");
  }
  return found;
};

const here = dirname(fileURLToPath(import.meta.url));
const APPSTORE_ROOT = join(here, "..", "appstore");

const browser = await puppeteer.launch({
  executablePath: findChrome(),
  headless: true,
});

try {
  for (const deck of DECKS) {
    const deckDir = join(APPSTORE_ROOT, deck.dir);
    const sourceHtml = join(deckDir, deck.html);
    if (!existsSync(sourceHtml)) {
      throw new Error(`Missing source HTML: ${sourceHtml}`);
    }

    const page = await browser.newPage();
    await page.setViewport({
      width: deck.width,
      height: deck.height,
      deviceScaleFactor: 1,
    });
    await page.goto(pathToFileURL(sourceHtml).href, { waitUntil: "networkidle0" });

    await page.evaluate(() => {
      document
        .querySelectorAll<HTMLButtonElement>(".toolbar button")
        .forEach((b) => b.classList.remove("on"));
      const exportBtn = document.querySelector<HTMLButtonElement>(
        '.toolbar button[data-mode="export"]',
      );
      exportBtn?.classList.add("on");
      window.dispatchEvent(new Event("resize"));

      const toolbar = document.querySelector<HTMLElement>(".toolbar");
      if (toolbar) toolbar.style.display = "none";
      document
        .querySelectorAll<HTMLElement>(".slide-caption")
        .forEach((c) => (c.style.display = "none"));
    });

    await page.evaluate(() => document.fonts.ready);
    await new Promise((r) => setTimeout(r, 600));

    const slides = await page.$$(".slide-wrap");
    if (slides.length !== deck.slides.length) {
      throw new Error(
        `[${deck.dir}] expected ${deck.slides.length} slides, found ${slides.length}`,
      );
    }

    for (let i = 0; i < slides.length; i++) {
      const inner = await slides[i].$(".slide");
      if (!inner) throw new Error(`[${deck.dir}] slide ${i + 1} has no .slide child`);
      const out = join(deckDir, `${deck.slides[i]}.png`);
      await inner.screenshot({
        path: out,
        type: "png",
        omitBackground: false,
        captureBeyondViewport: true,
      });
      console.log(`✓ ${deck.dir}/${deck.slides[i]}.png (${deck.width}×${deck.height})`);
    }

    await page.close();
  }
} finally {
  await browser.close();
}
