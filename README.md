# Currency Converter

A simple and fast Android currency converter app built with Jetpack Compose.

## Features

### Currency Conversion
- **Live conversion**: Edit any currency value and all others update instantly
- **160+ currencies**: Supports all major world currencies with country flags
- **Offline support**: Works without internet using cached exchange rates

### Exchange Rates
- **Automatic sync**: Rates update hourly in the background via WorkManager
- **Manual refresh**: Pull-to-refresh or tap the refresh button
- **Last updated indicator**: Shows when rates were last fetched (e.g., "5 minutes ago")

### Customization
- **Show/hide currencies**: Choose which currencies appear in your converter
- **Reorder currencies**: Long-press and drag to reorder the list
- **Search currencies**: Find currencies by code or name

### Number Formatting
Choose your preferred number format:
- `1,234.56` (US/UK style)
- `1.234,56` (European style)
- `1 234.56` (SI style with period)
- `1 234,56` (SI style with comma)

### Persistence
- **Remembers last conversion**: App restores your last entered value on restart
- **Saves preferences**: Number format and currency order are preserved

### UI
- **Dark mode**: Clean dark theme
- **Fast startup**: Minimal loading time
- **Pull-to-refresh**: Swipe down to update rates

## Setup

1. Get a free API key from [OpenExchangeRates](https://openexchangerates.org/)
2. Copy `local.properties.example` to `local.properties`
3. Add your API key: `OPENEXCHANGERATES_API_KEY=your_key_here`
4. Build and run

## Tech Stack

- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModels and StateFlow
- **Database**: Room for offline storage
- **Network**: Retrofit + OkHttp
- **Background sync**: WorkManager
- **Preferences**: DataStore
- **Navigation**: Compose Navigation
- **DI**: Manual dependency injection (AppContainer)

## Project Structure

```
app/src/main/java/com/example/myapplication/
├── CurrencyExchangeApp.kt      # Application class
├── MainActivity.kt             # Single activity host
├── data/
│   ├── api/                    # Retrofit API interface
│   ├── db/                     # Room database, entities, DAOs
│   └── repository/             # Data repositories
├── di/
│   └── AppContainer.kt         # Dependency injection
├── domain/
│   └── model/                  # Domain models
├── ui/
│   ├── navigation/             # Navigation setup
│   ├── screens/
│   │   ├── converter/          # Main converter screen
│   │   ├── editlist/           # Currency list editor
│   │   └── settings/           # Settings screen
│   └── theme/                  # Colors, typography, theme
└── worker/
    └── SyncRatesWorker.kt      # Background sync
```

## Requirements

- Android 7.0 (API 24) or higher
- Internet connection for initial rate fetch and updates

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
