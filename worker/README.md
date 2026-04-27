# Exchange Rates Cloudflare Worker

This Worker fetches exchange rates from OpenExchangeRates hourly and caches them in Cloudflare KV storage. This reduces API usage and provides faster responses.

## Setup

### 1. Create Cloudflare Resources

1. Sign up at [Cloudflare](https://dash.cloudflare.com/)
2. Go to **Workers & Pages** → **Create Worker**
3. Go to **Workers & Pages** → **KV** → Create a namespace called `EXCHANGE_RATES`

### 2. Configure Secrets

In your Worker settings, add a secret:
- **Name:** `OPENEXCHANGERATES_API_KEY`
- **Value:** Your API key from [OpenExchangeRates](https://openexchangerates.org/)

### 3. Bind KV Namespace

In Worker settings → Variables → KV Namespace Bindings:
- **Variable name:** `EXCHANGE_RATES`
- **KV namespace:** Select the namespace you created

### 4. Set Up Cron Trigger

In Worker settings → Triggers → Cron Triggers:
- Add: `5 * * * *` (runs at 5 minutes past every hour)

OpenExchangeRates updates at :01-02 past the hour, so fetching at :05 ensures fresh data.

### 5. Deploy

Copy `index.js` to your Worker and deploy.

## Using with Wrangler CLI

If you prefer using the Wrangler CLI:

```bash
# Install wrangler
npm install -g wrangler

# Login to Cloudflare
wrangler login

# Create wrangler.toml (see example below)
# Deploy
wrangler deploy
```

Example `wrangler.toml`:

```toml
name = "exchange-rates"
main = "index.js"
compatibility_date = "2024-01-01"

kv_namespaces = [
  { binding = "EXCHANGE_RATES", id = "your-kv-namespace-id" }
]

[triggers]
crons = ["5 * * * *"]
```

Then add your API key as a secret:
```bash
wrangler secret put OPENEXCHANGERATES_API_KEY
```

## Response Format

The Worker returns JSON in this format:

```json
{
  "timestamp": 1234567890,
  "base": "USD",
  "rates": {
    "EUR": 0.85,
    "GBP": 0.73,
    ...
  },
  "fetched_at": 1234567890000
}
```

- `timestamp`: When OpenExchangeRates published the rates (Unix seconds)
- `base`: Base currency (always USD for free tier)
- `rates`: Exchange rates relative to USD
- `fetched_at`: When this Worker fetched the rates (Unix milliseconds)
