import { mkdir } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const here = dirname(fileURLToPath(import.meta.url));
const SOURCES = join(here, "..", "icons");
const OUTPUT = join(here, "..", "public", "icons");

const renderPng = async (svgPath: string, size: number, outPath: string) => {
  const density = Math.ceil((size / 512) * 384);
  await sharp(svgPath, { density })
    .resize(size, size, {
      fit: "contain",
      background: { r: 0, g: 0, b: 0, alpha: 0 },
    })
    .png({ compressionLevel: 9 })
    .toFile(outPath);
  console.log(`✓ ${outPath} (${size}×${size})`);
};

await mkdir(OUTPUT, { recursive: true });

const iconSrc = join(SOURCES, "icon.svg");
const maskableSrc = join(SOURCES, "icon-maskable.svg");

await Promise.all([
  renderPng(iconSrc, 180, join(OUTPUT, "apple-touch-icon.png")),
  renderPng(iconSrc, 192, join(OUTPUT, "icon-192.png")),
  renderPng(iconSrc, 512, join(OUTPUT, "icon-512.png")),
  renderPng(maskableSrc, 192, join(OUTPUT, "icon-maskable-192.png")),
  renderPng(maskableSrc, 512, join(OUTPUT, "icon-maskable-512.png")),
]);
