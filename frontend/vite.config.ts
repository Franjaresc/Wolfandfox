import react from '@vitejs/plugin-react';
import { loadEnv } from 'vite';
import { defineConfig } from 'vitest/config';

const DEFAULT_BACKEND_PORT = '8080';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backendPort = env.BACKEND_PORT ?? DEFAULT_BACKEND_PORT;

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': { target: `http://localhost:${backendPort}`, changeOrigin: true },
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      restoreMocks: true,
      // Times are shown in the viewer's zone, so tests fix it to get the same output everywhere.
      env: { TZ: 'UTC' },
    },
  };
});
