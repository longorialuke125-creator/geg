# Bankroll Buddy

Client-side Fabric mod for **Minecraft 26.1.x** that tracks your gambling session on DonutSMP: net profit, win/loss counts and streaks, stop-loss / take-profit alerts, a HUD panel, and bet-sizing calculators.

It only reads messages the server sends you and shows numbers. It does not click, bet, or predict anything for you. The server does not need the mod. Check the server's rules on client mods before you use it.

## Get the .jar (pick one)

### A) GitHub builds it for you (no installs)

1. Create a new GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Open the repo's **Actions** tab. The `build` workflow runs on every push (or press **Run workflow**).
3. When it turns green, open the run and download the **bankroll-buddy-jar** artifact. Unzip it: the `.jar` is inside.

### B) Build on your own PC

You need **JDK 25** and **Gradle 9.4 or newer** (Loom for 26.1 requires it).

```
gradle build
```

The jar is `build/libs/bankroll-buddy-1.0.0.jar` (ignore the `-sources` jar). To get a Gradle wrapper first, run `gradle wrapper` once, then use `./gradlew build`.

## Install

1. Install **Fabric Loader 0.19.2+** and **Fabric API** for Minecraft 26.1.x. (Java 25 is required to run 26.1.)
2. Drop `bankroll-buddy-1.0.0.jar` into your `.minecraft/mods` folder.

## Menu

Press **B** in game to open the Bankroll Buddy menu. It shows your live session stats and has buttons for the HUD panel, auto-tracking, stop-loss, take-profit (each click steps through 10K, 50K, 100K, 500K, 1M, 5M, 10M, 50M, then off), undo last result, and reset session.

To change the key: **Options > Controls > Key Binds**, then look under the **Bankroll Buddy** category. The menu only opens while no other screen (chat, inventory) is open.

## Commands

| Command | What it does |
|---|---|
| `/bankroll` | Session stats |
| `/bankroll win <amount>` / `loss <amount>` | Add a result by hand (`5k`, `2.5M` work) |
| `/bankroll undo` / `reset` | Remove last result / clear the session |
| `/bankroll start <amount>` | Set your starting bankroll |
| `/bankroll stoploss <amount\|off>` | Alert when session net drops to that loss |
| `/bankroll takeprofit <amount\|off>` | Alert when session net reaches that profit |
| `/bankroll hud on\|off` | Show or hide the HUD panel |
| `/bankroll track on\|off` | Turn automatic chat tracking on or off |
| `/bankroll debug on\|off` | Print server lines containing `$` that did not match (for tuning) |
| `/bankroll kelly <win%> <netOdds>` | Bet sizing. `netOdds` is profit per 1 staked on a win (1 = even money) |
| `/bankroll martingale <base> <multiplier> <losses>` | What a "double after a loss" ladder costs, and how many losses your bankroll covers |

If the odds have no edge (for example a coinflip that pays less than even money), `kelly` says so and recommends betting nothing.

## How automatic tracking works

Server messages like "`<player> paid you $1.5K`" count as a win, and "`You paid <player> $1.5K`" as a loss. Only server/system messages are read, not player chat, so nobody can fake a payout by typing it.

**The default message patterns are guesses at DonutSMP's wording and are not verified.** If nothing is being tracked:

1. Run `/bankroll debug on` and do a payment. Unmatched lines with a `$` are printed in chat.
2. Open `config/bankroll-buddy.json` and edit `inRegex` and `outRegex` to fit. Each needs a named group `amount` (and optionally `player`), for example `(?<player>\w+) paid you \$(?<amount>[\d,.]+[KMBT]?)`.
3. Restart the game. Until it matches, `/bankroll win` and `/bankroll loss` always work.

Payments that are not bets (rakeback, trades) also look like pays, so use `/bankroll undo` or `/bankroll track off` when you are not gambling.

## Project layout

- `src/main/java/com/bankrollbuddy/core/` - plain Java logic (amount parsing, stats, calculators, chat matching)
- `src/main/java/com/bankrollbuddy/client/` - Minecraft/Fabric glue (HUD, commands, chat listener, saving)
- `tests/CoreSelfTest.java` - checks for the core logic: `javac -d out src/main/java/com/bankrollbuddy/core/*.java && java -cp out tests/CoreSelfTest.java`

Tracks gambling results, not a recommendation to gamble: set a stop-loss and treat your bankroll as money you can afford to lose.
