import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Le chiamate a /api/... vanno al gateway (8080), che le gira ai servizi
      "/api": "http://localhost:8080",
    },
  },
});
