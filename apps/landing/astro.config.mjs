import { defineConfig } from "astro/config";

export default defineConfig({
  site: "https://unes.app",
  output: "static",
  build: {
    format: "directory",
  },
  server: {
    port: 4321,
  },
});
