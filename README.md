# Currency Converter

A simple and fast Android currency converter app built with Jetpack Compose.

## Features

### Currency Conversion
- **Live conversion**: Edit any currency value and all others in your list update instantly
- **Works offline**: Works without internet using cached exchange rates
- **No ads**: The whole reason why I built this app

### Exchange Rates
- **Automatic sync**: Rates update hourly in the background via WorkManager (aligned to :06 past the hour)
- **Manual refresh**: Pull-to-refresh or tap the refresh button
- **Freshness indicator**: Shows when rates were last fetched (e.g., "today 14:30 (fully up-to-date)")
- **Flexible backend**: Supports both Cloudflare Worker or direct OpenExchangeRates API

### Customization
- **Show/hide currencies**: Choose which currencies appear in your converter
- **Reorder currencies**: Long-press and drag to reorder the list
- **Search currencies**: Find currencies by code or name

### Number Formatting
Choose your preferred number format in settings:
- `1,234.56` (US/UK style)
- `1.234,56` (European style)
- `1 234.56` (SI style with period)
- `1 234,56` (SI style with comma)

## Architecture

### Tech Stack
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModels and StateFlow
- **Database**: Room for offline storage
- **Network**: Retrofit + OkHttp
- **Background sync**: WorkManager
- **Preferences**: DataStore
- **Navigation**: Compose Navigation
- **DI**: Manual dependency injection (AppContainer)

### Project Structure

```
app/src/main/java/com/example/myapplication/
├── CurrencyExchangeApp.kt      # Application class, initializes dependencies
├── MainActivity.kt             # Single activity host for Compose
├── data/
│   ├── api/                    # Retrofit API interface
│   ├── db/                     # Room database, entities, DAOs
│   └── repository/             # Data repositories (rates, settings)
├── di/
│   └── AppContainer.kt         # Manual dependency injection container
├── domain/
│   └── model/                  # Domain models (Currency, CurrencyInfo)
├── ui/
│   ├── navigation/             # Navigation graph setup
│   ├── screens/
│   │   ├── converter/          # Main converter screen + ViewModel
│   │   ├── editlist/           # Currency list editor + ViewModel
│   │   └── settings/           # Settings screen + ViewModel
│   └── theme/                  # Colors, typography, Material 3 theme
└── worker/
    └── SyncRatesWorker.kt      # Hourly background sync with WorkManager
```

### Data Flow

1. **Exchange rates** are fetched from either a Cloudflare Worker or OpenExchangeRates directly
2. Rates are stored locally in Room database for offline access
3. ViewModels expose UI state as StateFlow, collected by Compose screens
4. User currency preferences (visibility, order) stored separately in Room

### Background Sync

- WorkManager schedules hourly sync aligned to :06 past the hour
- Only runs when network is available
- Uses exponential backoff on failure
- Note: Android's Doze mode and battery optimization may delay background work

## Requirements

- Android 7.0 (API 24) or higher
- Internet connection for initial rate fetch and updates

## Setup

1. Clone the repository
2. Copy `local.properties.example` to `local.properties`
3. Configure ONE of the following options:

### Option 1: Direct API (easiest)
Get a free API key from [OpenExchangeRates](https://openexchangerates.org/) (1,000 requests/month free):
```properties
OPENEXCHANGERATES_API_KEY=your_api_key_here
```

### Option 2: Cloudflare Worker (recommended for heavy usage)
Deploy your own Cloudflare Worker that fetches and caches rates:
```properties
EXCHANGE_RATE_WORKER_URL=https://your-worker.your-subdomain.workers.dev/
```

The Worker should return JSON in this format:
```json
{
  "timestamp": 1234567890,
  "base": "USD",
  "rates": { "EUR": 0.85, "GBP": 0.73, ... },
  "fetched_at": 1234567890000
}
```

## Building

```bash
./gradlew assembleDebug
```

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

Copyright (C) 2026 Sjors Haanen (shaanen)
