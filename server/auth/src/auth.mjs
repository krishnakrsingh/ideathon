import path from "node:path";
import { mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import Database from "better-sqlite3";
import nodemailer from "nodemailer";
import { betterAuth } from "better-auth";
import { getMigrations } from "better-auth/db/migration";
import { jwt } from "better-auth/plugins";

const authDirectory = path.resolve(
  process.env.SWASTHAI_AUTH_DATA_DIR ?? path.join(path.dirname(fileURLToPath(import.meta.url)), "../../.local-data/auth"),
);
mkdirSync(authDirectory, { recursive: true });

const baseURL = process.env.BETTER_AUTH_URL;
const secret = process.env.BETTER_AUTH_SECRET;
const isProduction = process.env.NODE_ENV === "production";
const requireEmailVerification = isProduction || process.env.SWASTHAI_AUTH_REQUIRE_VERIFIED_EMAIL === "true";

if (!baseURL) throw new Error("BETTER_AUTH_URL must be set explicitly.");
if (!secret || Buffer.byteLength(secret) < 32) throw new Error("BETTER_AUTH_SECRET must contain at least 32 bytes.");
if (isProduction && !baseURL.startsWith("https://")) throw new Error("Production Better Auth requires an HTTPS base URL.");

const smtpHost = process.env.SWASTHAI_SMTP_HOST;
const smtpFrom = process.env.SWASTHAI_SMTP_FROM;
const mailer = smtpHost && smtpFrom
  ? nodemailer.createTransport({
      host: smtpHost,
      port: Number(process.env.SWASTHAI_SMTP_PORT ?? 587),
      secure: process.env.SWASTHAI_SMTP_SECURE === "true",
      auth: process.env.SWASTHAI_SMTP_USER
        ? { user: process.env.SWASTHAI_SMTP_USER, pass: process.env.SWASTHAI_SMTP_PASSWORD }
        : undefined,
    })
  : undefined;

if (requireEmailVerification && !mailer) {
  throw new Error("Email verification is required; configure SWASTHAI_SMTP_HOST and SWASTHAI_SMTP_FROM.");
}

const database = new Database(path.join(authDirectory, "accounts.sqlite3"));
database.pragma("journal_mode = WAL");
database.pragma("foreign_keys = ON");

export const auth = betterAuth({
  appName: "SwasthAI",
  baseURL,
  secret,
  database,
  trustedOrigins: (process.env.SWASTHAI_AUTH_TRUSTED_ORIGINS ?? baseURL)
    .split(",")
    .map((origin) => origin.trim())
    .filter(Boolean),
  emailAndPassword: {
    enabled: true,
    requireEmailVerification,
    minPasswordLength: 12,
    maxPasswordLength: 128,
    revokeSessionsOnPasswordReset: true,
    sendResetPassword: async ({ user, url }) => {
      if (!mailer) throw new Error("Password reset email delivery is not configured.");
      await mailer.sendMail({
        from: smtpFrom,
        to: user.email,
        subject: "Reset your SwasthAI password",
        text: `Open this link to choose a new password: ${url}`,
      });
    },
  },
  emailVerification: requireEmailVerification
    ? {
        sendOnSignUp: true,
        sendOnSignIn: true,
        autoSignInAfterVerification: true,
        sendVerificationEmail: async ({ user, url }) => {
          await mailer.sendMail({
            from: smtpFrom,
            to: user.email,
            subject: "Verify your SwasthAI account",
            text: `Open this link to verify your email address: ${url}`,
          });
        },
      }
    : undefined,
  plugins: [
    jwt({
      jwt: {
        issuer: baseURL,
        audience: baseURL,
        expirationTime: "15m",
        getSubject: (session) => session.user.id,
        definePayload: ({ user }) => ({ emailVerified: user.emailVerified }),
      },
    }),
  ],
});

const { runMigrations } = await getMigrations(auth.options);
await runMigrations();
