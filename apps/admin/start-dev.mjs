import { createServer } from 'vite'
import { fileURLToPath } from 'url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))

const server = await createServer({
  root: __dirname,
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/qualification-files': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
await server.listen()
console.log('Vite dev server running on http://localhost:5174/')

// Keep the process alive
process.on('SIGTERM', async () => {
  await server.close()
  process.exit(0)
})
