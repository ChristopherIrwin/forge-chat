<p align="center">
  <img src="assets/logo.webp" width="160" alt="ForgeChat logo">
</p>

<h1 align="center">ForgeChat</h1>

<p align="center"><i>Channels, PMs, mentions, mutes, anti-spam, and rich MiniMessage formatting — the all-in-one chat suite.</i></p>

<p align="center">
  <img src="https://img.shields.io/badge/version-1.0.0-ff7b2e?style=for-the-badge" alt="version 1.0.0">
  <img src="https://img.shields.io/badge/Paper-26.3-2f9e6e?style=for-the-badge" alt="Paper 26.3">
  <img src="https://img.shields.io/badge/Java-25-f89820?style=for-the-badge" alt="Java 25">
  <img src="https://img.shields.io/badge/3_channels-2563eb?style=for-the-badge" alt="3 channels">
  <img src="https://img.shields.io/badge/14_commands-2563eb?style=for-the-badge" alt="14 commands">
  <img src="https://img.shields.io/badge/dependencies-zero-6b7280?style=for-the-badge" alt="zero dependencies">
</p>

<p align="center"><sub>Not affiliated with <a href="https://minecraftforge.net">MinecraftForge</a> — "Forge" is just a name.</sub></p>

---

An all-in-one chat suite for Paper servers: channels, private messages, mentions, moderation, anti-spam, and rich MiniMessage formatting. Original implementation, zero runtime dependencies beyond Paper itself.

## Features

- **Channels** — Global, local (radius-based), and staff channels. Set a default with `/ch` or fire one-off messages with `/g`, `/l`, `/staff`.
- **Private messages** — `/msg`, `/r` (reply), `/ignore` and `/unignore`. Social-spy lets staff see all PMs.
- **Mentions** — `@name` pings with sound, highlight, and fuzzy/prefix matching; `@here` and `@everyone` for permitted staff.
- **Moderation** — Persistent and timed mutes with reasons, per-channel slow mode.
- **Anti-spam** — Rate limiting, repeated-character detection, caps detection, and link blocking with a whitelist.
- **Word filter** — Replace or cancel messages containing blocked words.
- **Permission-group formatting** — Configurable prefixes, name colors, and suffixes; first matching permission wins.
- **Hover cards** — Configurable multi-line hover tooltip on player names (name, group, channel, session time).
- **Format flexibility** — Channel and PM templates accept both `{tag}` and `<tag>` placeholder styles; malformed MiniMessage never crashes the pipeline (safe fallbacks).

## Requirements

- Paper 26.3 or newer (`api-version: '26.3'`)
- Java 25

## Installation

Drop `ForgeChat-1.0.0.jar` into your server's `plugins/` folder and restart. A default `config.yml` is generated on first run.

## Commands

| Command | Usage | Description | Permission |
|---|---|---|---|
| `/ch` | `/ch <global\|local\|staff>` | Set your default chat channel | `forgechat.use` |
| `/g` | `/g <message>` | One-off message in global chat | `forgechat.use` |
| `/l` | `/l <message>` | One-off message in local chat | `forgechat.use` |
| `/staff` | `/staff <message>` | One-off message in staff chat | `forgechat.staff` |
| `/msg` | `/msg <player> <message>` | Send a private message (`/tell`, `/w`) | `forgechat.use` |
| `/r` | `/r <message>` | Reply to your last private message | `forgechat.use` |
| `/ignore` | `/ignore <player>` | Ignore a player's private messages | `forgechat.use` |
| `/unignore` | `/unignore <player>` | Stop ignoring a player | `forgechat.use` |
| `/spy` | `/spy` | Toggle social-spy (see all private messages) | `forgechat.spy` |
| `/mute` | `/mute <player> [time] [reason...]` | Mute a player (permanent if no time) | `forgechat.mod` |
| `/tempmute` | `/tempmute <player> <time> [reason...]` | Temporarily mute a player | `forgechat.mod` |
| `/unmute` | `/unmute <player>` | Unmute a player | `forgechat.mod` |
| `/slowmode` | `/slowmode <global\|local\|staff> <seconds>` | Set per-channel slow mode (0 disables) | `forgechat.mod` |
| `/fchat` | `/fchat reload` | Reload configuration (`/forgechat`) | `forgechat.admin` |

Time format for mutes: `10s`, `5m`, `2h`, `1d`, `1w`, `perm` / `permanent` / `forever`.

## Permissions

| Permission | Description | Default |
|---|---|---|
| `forgechat.use` | Basic chat usage | everyone |
| `forgechat.color` | Use MiniMessage formatting in chat messages | everyone |
| `forgechat.staff` | Access the staff channel | op |
| `forgechat.spy` | Use social-spy | op |
| `forgechat.mod` | Mute players and manage slow mode | op |
| `forgechat.mention.everyone` | Use `@here` and `@everyone` mentions | op |
| `forgechat.spam.bypass` | Bypass anti-spam checks | op |
| `forgechat.filter.bypass` | Bypass the word filter | op |
| `forgechat.admin` | Reload configuration | op |

Group display permissions (`forgechat.group.admin`, `forgechat.group.mod`, ...) are defined by your config, not by the plugin.

## Configuration

`plugins/ForgeChat/config.yml` — all text supports MiniMessage. Mutes persist in `mutes.yml`; ignores, spy toggles, channel selections, and reply targets persist in `data.yml` (auto-saved every 5 minutes and on shutdown).

| Key | Type | Default | Description |
|---|---|---|---|
| `default-channel` | string | `global` | Channel new players chat in |
| `channels.<name>.tag` | string | per-channel | Prefix tag, e.g. `[G]` |
| `channels.<name>.format` | string | `{tag}{name}: {message}` | Line format; `{tag}`, `{name}`, `{message}` or `<tag>`, `<name>`, `<message>` |
| `channels.local.radius` | double | `50.0` | Local channel range in blocks |
| `groups` | list | 3 entries | First matching `permission` wins; the empty-permission entry is the fallback. Each entry: `permission`, `label`, `prefix`, `name-format` (supports `{name}`), `suffix` |
| `hover-format` | string list | 4 lines | Hover tooltip lines; placeholders `{name}`, `{group}`, `{channel}`, `{session}` |
| `mentions.enabled` | boolean | `true` | Enable `@name` mentions |
| `mentions.sound` | string | `entity.experience_orb_pickup` | Vanilla sound key for the mention ping |
| `mentions.volume` | double | `1.0` | Ping volume |
| `mentions.pitch` | double | `1.6` | Ping pitch |
| `mentions.highlight` | string | `<yellow><bold>@{name}</bold></yellow>` | Mention highlight template; `{name}` |
| `antispam.enabled` | boolean | `true` | Enable anti-spam checks |
| `antispam.max-messages` | int | `4` | Messages allowed per window |
| `antispam.window-seconds` | int | `5` | Rate-limit window in seconds |
| `antispam.max-repeat-chars` | int | `5` | Max repeated characters before blocking |
| `antispam.caps-percent` | int | `70` | Caps percentage threshold |
| `antispam.caps-min-length` | int | `8` | Minimum message length for caps check |
| `antispam.block-links` | boolean | `true` | Block messages containing URLs |
| `antispam.link-whitelist` | string list | `["myserver.com"]` | Domains exempt from link blocking |
| `filter.enabled` | boolean | `true` | Enable the word filter |
| `filter.action` | string | `replace` | `replace` or `cancel` |
| `filter.replacement` | string | `***` | Replacement text for filtered words |
| `filter.words` | string list | `["badword"]` | Blocked words (case-insensitive) |
| `slowmode.global` / `.local` / `.staff` | long | `0` | Slow-mode seconds per channel (managed via `/slowmode`, persisted) |
| `pm.format-to` | string | see default | Incoming PM format; `{sender}`, `{message}` |
| `pm.format-from` | string | see default | Outgoing PM format; `{recipient}`, `{message}` |
| `pm.format-spy` | string | see default | Social-spy format; `{sender}`, `{recipient}`, `{message}` |
| `messages.*` | strings | see default | Every user-facing message; MiniMessage supported |

## Building from source

```bash
bash build.sh
```

Direct `javac` build — no Gradle daemon required. Requires JDK 25 (`~/workspace/.toolchains/jdk-25.0.4.1+1`) and the Paper API on the compile classpath. Compiles with `-Werror` (warnings are errors) and uses no deprecated APIs.

## Code quality

Every package declares `@NotNullByDefault` (JetBrains annotations); sites where `null` is a legitimate value are explicitly marked `@Nullable`. Null-safety is a documented contract, not a convention.

---

<p align="center"><i>Part of the <a href="https://github.com/ChristopherIrwin">Forge</a> plugin suite — original implementations, zero dependencies.</i></p>
