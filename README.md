# WhatsApp Message Studio (with AI Studio)

An Android app (Kotlin, Jetpack Compose, Room) for writing and organising messages for recipients,
with AI features. It **never sends WhatsApp messages by itself**: "Share" opens WhatsApp with the text
filled in, and you press Send.

## What is inside

| Part | Where | What it does |
|---|---|---|
| Messages tab | `ui/MessagesScreen.kt` | Recipients, saved messages, edit, share, delete |
| AI Studio tab | `ui/AiStudioScreen.kt` | Prompt, language, tone, category, length, 10/50/100/500/1,000 messages, emoji, optional name, duplicate removal, custom instructions, progress, cancel, final unique count |
| Batch generation | `ai/BatchGenerator.kt` | Splits big requests into batches, removes duplicates, saves each to Room, asks for replacements, stops honestly if it can't reach the number |
| Templates tab | `ui/TemplatesScreen.kt` | "Create My AI Template": describe a style, preview, edit, save, reuse, delete |
| Message tools | `ui/Components.kt` | Rewrite, fix grammar, to Urdu / Roman Urdu, shorter, longer, change tone, add/remove emoji, clearer and more polite, 3 alternatives. Your original stays until you tap "Use this" |
| Assistant tab | `ui/AssistantScreen.kt` | Chat helper; drafts open in the message editor |
| Settings tab | `ui/SettingsScreen.kt` | Backend address, consent, batch size, warning size, daily limit, usage stats, privacy notes |
| Offline mode | `ai/LocalPools.kt` | Clearly labeled template mixing, **not AI**. Makes at most 24 (short), 288 (medium) or 864 (long) different messages per language |
| Backend | `backend/` | Holds the AI provider key so the phone never does |

## 1. Start the backend (needed for AI features)

The app talks to *your* backend over HTTPS. The AI key lives only there.

1. Put the `backend` folder on any host that runs Node.js 18+ and gives you an `https://` address
   (Render, Railway, Fly.io, a VPS with Caddy, etc.).
2. Set these environment variables on the host (see `backend/.env.example`):
   - `AI_API_KEY`: your AI provider key
   - `BACKEND_TOKEN`: a long random secret (strongly recommended, or anyone who finds the address can spend your credits)
   - optional `AI_MODEL`
3. Start command: `node server.js`. Check `https://your-address/health` shows `{"ok":true}`.
4. Run `cd backend && node test.js` to check it works on your machine (needs no key or internet).

To use a different AI provider, change only `backend/provider.js`.

## 2. Build the app

**Without installing anything (GitHub):**
1. Create a free repository on github.com and upload the **contents** of this folder (the `.github`
   folder must end up at the top level of the repository).
2. Open the **Actions** tab, choose **Build APK**, press **Run workflow**.
3. When it turns green, open the run and download **message-studio-apk** at the bottom. It contains
   `app-debug.apk`. Copy it to your phone and install it (allow "unknown sources" when asked).

**With Android Studio:** open this folder, let it sync, press Run.

## 3. Connect the app

Open **Settings** in the app, enter the backend address (must start with `https://`) and the
`BACKEND_TOKEN`, then use any AI button. The first time, the app asks for your permission and shows
exactly where your text will be sent.

## Privacy and safety

- No AI key is inside the app. The backend token you type is stored only on your phone.
- Contacts, phone numbers and WhatsApp conversations are never read or sent to anyone.
- Text goes to the AI service only when you press an AI button, after you agree. The recipient's name is sent only if you switch personalization on.
- Messages, recipients, templates and usage history are stored only on the phone (backup is turned off).
- If the AI is unreachable the app shows the error. It never makes up messages and calls them AI.
- AI providers can charge for usage and set their own limits. The app has a warning for large requests and an optional daily limit, but it cannot promise unlimited generation.
- Please don't use this for unsolicited or repetitive bulk messaging. WhatsApp can restrict accounts that do.

## Honest status

The backend is tested (15 automated checks against a mock AI provider). The offline generator and
duplicate rules were compiled and run with a real Kotlin compiler. The Android app as a whole has
**not** been compiled or run on a device in the environment where it was written, so the first build
may report a small error; if it does, paste the error text and it can be fixed quickly.
