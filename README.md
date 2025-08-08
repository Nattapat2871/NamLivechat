<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub Repo stars](https://img.shields.io/github/stars/Nattapat2871/NamLivechat?style=flat-square)](https://github.com/Nattapat2871/Namlivechat/stargazers)
![Visitor Badge](https://api.visitorbadge.io/api/VisitorHit?user=Nattapat2871&repo=NamLivechat&countColor=%237B1E7A&style=flat-square)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/Nattapat2871)

</div>

<p align= "center">
      <b>English</b> | <a href="README_TH.md">ภาษาไทย</a>
</p>

# NamLivechat

Bridge the gap between your live stream and your Minecraft server. NamLivechat is a powerful, highly configurable plugin that brings your **YouTube**, **Twitch**, and **TikTok** live stream chats directly into the game. Keep your community engaged with real-time messages and a rich, customizable alert system without ever needing to switch screens. This plugin is built for performance and stability, with dedicated support for modern Folia and Paper servers.

## How It Works (Core Concept)

The plugin uses different methods to connect to each platform, ensuring the most stable and efficient connection possible:
- **YouTube:** Connects via the official **Google YouTube Data API v3** to poll for live chat messages and events.
- **Twitch:** Connects to Twitch's real-time **IRC chat servers** for instant message delivery and uses the **Helix API** to monitor for events like new followers.
- **TikTok:** Connects to TikTok's unofficial **Webcast service** using the `TikTokLiveJava` library, mimicking a web browser to receive real-time events like comments and gifts.

## ✨ Features

- **Multi-Platform Support:** Connect to **YouTube**, **Twitch**, and **TikTok** live streams.
- **Real-Time Chat:** Displays live chat messages directly in Minecraft, featuring colored usernames based on their roles on each platform.
- **Comprehensive Event Alerts:** Get instant, highly customizable in-game notifications for all important events:
    - **YouTube:** Super Chat, Super Stickers, New Members, Gifted Memberships, and Member Milestones.
    - **Twitch:** New Followers, Subscriptions (New, Resubs), Gifted Subs, and Community Gift Bombs.
    - **TikTok:** New Followers and Gifts.
- **Advanced Alert System:** Alerts are displayed in three configurable ways:
    - **Custom Chat Messages:** Design unique alert messages for each event.
    - **Configurable Sounds:** Assign specific sounds, volumes, and pitches for each alert.
    - **Dynamic Boss Bars:** Important alerts appear on a Boss Bar, **immediately replacing** any previous alert to show the latest event.
- **Full Customization:** Almost every aspect is configurable via YAML files, including message formats, role colors, sounds, and Boss Bar styles.
- **Multi-Language Support:** All user-facing plugin messages can be translated. Default files for **English (`en.yml`)** and **Thai (`th.yml`)** are provided.
- **Semi-Automatic Updates:**
    - **Update Checker:** Automatically checks for new versions from GitHub and notifies admins upon joining.
    - **Update Command:** A simple command (`/namlivechat update`) downloads the latest version to the server's main `update` folder for a safe, automatic update on the next server restart.
- **Debug Mode:** A simple toggle in the `config.yml` to show detailed console logs for easy troubleshooting.

## ⚙️ Compatibility

- **Server Software:** **PaperMC & Folia** (100% compatible).
- **Minecraft Version:** **1.21+**

## 📚 Installation & First-Time Setup

1.  Download the latest `.jar` file from the [Releases](https://github.com/Nattapat2871/NamLivechat/releases) page.
2.  Place the `NamLivechat-X.X.jar` file into your server's `plugins` folder.
3.  Start your server once. The plugin will generate the following files and folders:
    - `plugins/NamLivechat/config.yml`
    - `plugins/NamLivechat/youtube-config.yml`
    - `plugins/NamLivechat/twitch-config.yml`
    - `plugins/NamLivechat/tiktok-config.yml`
    - `plugins/NamLivechat/messages/en.yml`
    - `plugins/NamLivechat/messages/th.yml`
4.  Stop the server.
5.  Follow the **API & Token Setup** guide below to configure the `youtube-config.yml` and `twitch-config.yml` files.
6.  Start your server again and enjoy!

## 🔧 Configuration

### `config.yml` (Main Configuration)

This file controls the global settings of the plugin.

```yml
# ========================================= #
#            NamLivechat General Settings   #
# ========================================= #

# Set the language for in-game messages.
# Available languages are defined by the .yml files in the 'messages' folder.
language: "en"

# Set to true to enable detailed console logs for debugging purposes.
# It is recommended to keep this false during normal use.
debug-mode: false

# ========================================= #
#            Update Settings                #
# ========================================= #
# Set to true to notify admins in-game when a new version is available.
update-alert: true

# Set to true to enable the /namlivechat update command.
# This will download the latest version to the server's /update/ folder.
auto-update: true
```

### `messages/` folder (Language Files)

This folder contains the language files (`en.yml`, `th.yml`). You can edit these files to change any message the plugin sends to players. You can also create new files (e.g., `es.yml` for Spanish) and set `language: "es"` in `config.yml` to use it.

### `youtube-config.yml`

This file controls all settings related to YouTube.

```yml
# Master switch for the entire YouTube module.
enabled: true

# Your YouTube Data API v3 Key.
youtube-api-key: "YOUR_API_KEY_HERE"

# Format for regular chat messages.
# Placeholders: %player%, %message%
message-format: "&c[YouTube] &f%player%&7: &e%message%"

# Colors for different user roles in chat.
role-colors:
  owner: "&6"
  moderator: "&9"
  member: "&a"
  default: "&7"

# Master switch for all event alerts below.
youtube-alerts:
  show-super-chat: true
  show-new-members: true

# --- Event Configurations ---

# Super Chat Alert
super-chat:
  # %player%, %amount%, %message%
  message: "&6[Super Chat] &e%player% &fhas donated &a%amount%&f: &d%message%"
  sound:
    name: "entity.firework_rocket.large_blast"
    volume: 1.0
    pitch: 1.2
  boss-bar:
    enabled: true
    # %player%, %amount%
    message: "&e&l%player% &f&ldonated &a&l%amount%&f&l!"
    # Color: BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW
    color: "YELLOW"
    duration: 12

# New Member Alert
new-member:
  # %player%
  message: "&b[New Member] &d%player% &ahas just subscribed!"
  # ... (sound and boss-bar settings)

# Super Sticker Alert
super-sticker:
  # %player%, %amount%
  message: "&a[Super Sticker] &e%player% &fhas sent a sticker worth &a%amount%&f!"
  # ... (sound and boss-bar settings)

# Gifted Membership Alert
gifted-membership:
  # %gifter% (The user who gifted the membership)
  message: "&d[Gifted Member] &f%gifter% &ehas gifted a membership!"
  # ... (sound and boss-bar settings)

# Member Milestone Alert
member-milestone:
  enabled: true
  # %player%, %milestone% (e.g., "12 months"), %message% (user's custom message)
  message: "&a[Milestone!] &e%player% &fhas been a member for &d%milestone%&f! Message: &d%message%"
  # ... (sound and boss-bar settings)
```

### `twitch-config.yml`

This file controls all settings related to Twitch.

```yml
# Master switch for the entire Twitch module.
enabled: true

# Your OAuth Token.
oauth-token: "YOUR_OAUTH_TOKEN_HERE"

# Format for regular chat messages.
# Placeholders: %badges%, %user%, %message%
format: "&5[Twitch] %badges%%user%&7: &f%message%"

# Colors for different user roles in chat.
role-colors:
  broadcaster: "&6"
  moderator: "&9"
  vip: "&d"
  subscriber: "&a"
  default: "&7"

# --- Event Configurations ---
events:
  enabled: true
  
  # New Follower Alert
  new-follower:
    enabled: true
    # %user%
    message: "&d[Twitch] &f%user% &ehas just followed!"
    # ... (sound and boss-bar settings)

  # New Subscription Alert
  new-subscription:
    enabled: true
    # %user%, %tier%
    message: "&d[Twitch] &f%user% &ehas just subscribed at Tier %tier%!"
    # ... (sound and boss-bar settings)
  
  # ... (and so on for resubscription, gift-subscription, community-subscription)
```

### `tiktok-config.yml`

This file controls all settings related to TikTok.

```yml
# Master switch for the entire TikTok module.
enabled: true

# Format for regular chat messages.
# Placeholders: %user%, %message%
message-format: "&b[TikTok] &f%user%&7: &f%message%"

# Colors for different user roles in chat.
role-colors:
  moderator: "&9"
  subscriber: "&a"
  default: "&7"

# --- Event Configurations ---
events:
  enabled: true

  # Gift Alert
  gift:
    enabled: true
    # %user%, %gift_name%, %amount% (combo count), %total_value% (diamonds)
    message: "&e[TikTok Gift] &f%user% &ahas sent %amount%x &d%gift_name% &a(Value: %total_value% Diamonds)!"
    # ... (sound and boss-bar settings)

  # Follow Alert
  follow:
    enabled: true
    # %user%
    message: "&b[TikTok] &f%user% &ehas followed the stream!"
    # ... (sound and boss-bar settings)
```

## 🔑 API & Token Setup

#### YouTube Data API v3 Key

1.  Go to the [Google Cloud Console](https://console.cloud.google.com/).
2.  Create a new project.
3.  In the navigation menu, go to "APIs & Services" -> "Library".
4.  Search for and enable the **"YouTube Data API v3"**.
5.  In "APIs & Services" -> "Credentials", click "Create Credentials" -> "API key".
6.  Copy the generated API key.
7.  Paste it into the `youtube-api-key` field in your `youtube-config.yml`.
8.  **Important:** For security, click on the new API key, go to "API restrictions", and restrict it to your server's IP address.

#### Twitch OAuth Tokens

1.  Go to [Twitch Token Generator](https://twitchtokengenerator.com/).
2.  Click on the **"Custom Scope"** token type.
3.  In the list of scopes, you **must** select the following:
    - `chat:read`
    - `chat:edit`
    - `moderator:read:followers`
4.  Click the "Generate Token!" button and authorize with the Twitch account you want the plugin to use.
5.  Copy the entire generated token (it starts with `oauth:`).
6.  Paste it into the `oauth-token` field in your `twitch-config.yml`.

## 💻 Commands & Permissions

| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/livechat start <url>` | Auto-detects the platform from the URL and starts the connection. | `namlivechat.use` | `true` |
| `/livechat start <platform> <url/id>` | Starts a connection to a specific platform. | `namlivechat.use` | `true` |
| `/livechat stop [platform]` | Stops the current connection(s). | `namlivechat.use` | `true` |
| `/namlivechat reload` | Reloads all configuration files. | `namlivechat.admin` | `op` |
| `/namlivechat update` | Downloads the latest plugin version. | `namlivechat.admin` | `op` |

## 📦 Libraries Used

- **Google API Client for Java**
- **Twitch4J**
- **TikTokLiveJava**

---
Created by Nattapat2871.