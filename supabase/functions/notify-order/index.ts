// Supabase Edge Function: notify-order
//
// Dipanggil oleh Database Webhook pada tabel "orders" (INSERT & UPDATE).
// - INSERT: kirim notifikasi FCM ke semua device Admin ("Pesanan Baru Masuk!").
// - UPDATE (status berubah): kirim notifikasi FCM ke pelanggan pemilik order,
//   isi pesan disesuaikan dengan status baru.
//
// Env vars yang WAJIB di-set via `supabase secrets set` (JANGAN ditulis di kode/git):
//   SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY (biasanya sudah otomatis tersedia)
//   FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, FIREBASE_PRIVATE_KEY
//   WEBHOOK_SHARED_SECRET (nilai bebas, hanya untuk memastikan pemanggil sah)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
const FIREBASE_PROJECT_ID = Deno.env.get("FIREBASE_PROJECT_ID")!
const FIREBASE_CLIENT_EMAIL = Deno.env.get("FIREBASE_CLIENT_EMAIL")!
const FIREBASE_PRIVATE_KEY = (Deno.env.get("FIREBASE_PRIVATE_KEY") ?? "").replace(/\\n/g, "\n")
const WEBHOOK_SHARED_SECRET = Deno.env.get("WEBHOOK_SHARED_SECRET")

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)

function base64url(input: Uint8Array | string): string {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : input
  let binary = ""
  for (const b of bytes) binary += String.fromCharCode(b)
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")
}

async function getAccessToken(): Promise<string> {
  const header = { alg: "RS256", typ: "JWT" }
  const now = Math.floor(Date.now() / 1000)
  const claim = {
    iss: FIREBASE_CLIENT_EMAIL,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now,
  }
  const unsigned = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(claim))}`

  const pemContents = FIREBASE_PRIVATE_KEY
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "")
  const binaryDer = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0))

  const key = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  )
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned)
  )
  const jwt = `${unsigned}.${base64url(new Uint8Array(signature))}`

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=${encodeURIComponent("urn:ietf:params:oauth:grant-type:jwt-bearer")}&assertion=${jwt}`,
  })
  const data = await res.json()
  if (!data.access_token) throw new Error("Gagal ambil access token FCM: " + JSON.stringify(data))
  return data.access_token as string
}

async function sendFcm(accessToken: string, token: string, title: string, body: string) {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: { token, notification: { title, body } },
      }),
    }
  )
  if (!res.ok) {
    console.error(`FCM gagal untuk token ${token.slice(0, 12)}...: ${await res.text()}`)
  }
}

function statusMessage(status: string): { title: string; body: string } | null {
  switch (status) {
    case "Diproses":
      return { title: "Pesanan Dikonfirmasi", body: "Pesananmu sedang disiapkan di dapur." }
    case "Siap":
      return { title: "Pesanan Sudah Siap!", body: "Pesananmu sudah siap. Silakan diambil/ditunggu." }
    case "Diantar":
      return { title: "Pesanan Sedang Diantar", body: "Driver sedang menuju lokasimu." }
    case "Selesai":
      return { title: "Pesanan Selesai", body: "Terima kasih sudah memesan di Dapoer Lavana!" }
    case "Dibatalkan":
      return { title: "Pesanan Dibatalkan", body: "Pesananmu dibatalkan. Hubungi kami jika ini keliru." }
    default:
      return null
  }
}

Deno.serve(async (req) => {
  try {
    if (WEBHOOK_SHARED_SECRET) {
      const provided = req.headers.get("x-webhook-secret")
      if (provided !== WEBHOOK_SHARED_SECRET) {
        return new Response("unauthorized", { status: 401 })
      }
    }

    const payload = await req.json()
    const { type, table, record, old_record } = payload

    if (table !== "orders") {
      return new Response("ignored", { status: 200 })
    }

    const accessToken = await getAccessToken()

    if (type === "INSERT") {
      const { data: adminTokens } = await supabase
        .from("fcm_tokens")
        .select("token")
        .eq("role", "Admin")
      for (const row of adminTokens ?? []) {
        await sendFcm(
          accessToken,
          row.token,
          "Pesanan Baru Masuk!",
          `Order #${record.order_number} (${record.order_type}) menunggu konfirmasi.`
        )
      }
    } else if (type === "UPDATE") {
      if (record.status !== old_record?.status) {
        const msg = statusMessage(record.status)
        if (msg && record.user_id) {
          const { data: customerTokens } = await supabase
            .from("fcm_tokens")
            .select("token")
            .eq("owner_id", record.user_id)
            .eq("role", "Customer")
          for (const row of customerTokens ?? []) {
            await sendFcm(accessToken, row.token, msg.title, msg.body)
          }
        }
      }
    }

    return new Response("ok", { status: 200 })
  } catch (e) {
    console.error(e)
    return new Response(String(e), { status: 500 })
  }
})
