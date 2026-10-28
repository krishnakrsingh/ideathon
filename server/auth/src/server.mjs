import { createServer } from "node:http";
import { toNodeHandler } from "better-auth/node";
import { auth } from "./auth.mjs";

const host = process.env.SWASTHAI_AUTH_HOST ?? "127.0.0.1";
const port = Number(process.env.SWASTHAI_AUTH_PORT ?? 8766);

createServer(toNodeHandler(auth)).listen(port, host, () => {
  console.log(`SwasthAI Better Auth listening on http://${host}:${port}`);
});
