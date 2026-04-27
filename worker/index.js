/**
 * Cloudflare Worker for caching OpenExchangeRates API responses.
 *
 * This Worker fetches exchange rates hourly and caches them in KV storage,
 * reducing API calls and providing faster responses to the app.
 *
 * Setup:
 * 1. Create a Cloudflare account and go to Workers & Pages
 * 2. Create a new Worker
 * 3. Create a KV namespace called "EXCHANGE_RATES"
 * 4. Bind the KV namespace to this Worker
 * 5. Add your OpenExchangeRates API key as a secret: OPENEXCHANGERATES_API_KEY
 * 6. Set up a Cron Trigger (e.g., "5 * * * *" for 5 minutes past every hour)
 * 7. Deploy the Worker
 *
 * wrangler.toml example:
 * ```toml
 * name = "exchange-rates"
 * main = "index.js"
 *
 * kv_namespaces = [
 *   { binding = "EXCHANGE_RATES", id = "your-kv-namespace-id" }
 * ]
 *
 * [triggers]
 * crons = ["5 * * * *"]
 * ```
 */

export default {
  // HTTP request handler - serves rates to your app
  async fetch(request, env) {
    const rates = await env.EXCHANGE_RATES.get("latest", { type: "json" });

    if (!rates) {
      return new Response(JSON.stringify({ error: "No rates available" }), {
        status: 503,
        headers: { "Content-Type": "application/json" }
      });
    }

    return new Response(JSON.stringify(rates), {
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
        "Cache-Control": "public, max-age=1800"  // Cache 30 min
      }
    });
  },

  // Scheduled trigger - fetches rates hourly
  async scheduled(event, env) {
    const API_KEY = env.OPENEXCHANGERATES_API_KEY;
    const response = await fetch(
      `https://openexchangerates.org/api/latest.json?app_id=${API_KEY}`
    );

    if (response.ok) {
      const data = await response.json();
      await env.EXCHANGE_RATES.put("latest", JSON.stringify({
        timestamp: data.timestamp,
        base: data.base,
        rates: data.rates,
        fetched_at: Date.now()
      }));
    }
  }
};
