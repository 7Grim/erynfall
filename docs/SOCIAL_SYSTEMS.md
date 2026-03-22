# SOCIAL_SYSTEMS.md - Chat & Trading (MVP)

**Source:** OSRS chat and trading mechanics  
**Status:** LOCKED IN - Both chat systems, both trading systems  
**Purpose:** Enable multiplayer economy and communication

---

## CHAT SYSTEM

### Public Chat (Global Channel)

**Mechanics:**
- All online players see messages
- Right-click player → "Chat" option opens chat interface
- Type message → broadcast to all online players
- Message format: `[Player Name]: message text`
- Example: `[Troy]: Hey everyone, selling logs!`

**Features:**
- No character limit (or 255 chars like OSRS)
- Profanity filter optional (MVP: can skip for now)
- Ignore list (players can mute other players)
- Message history in chat window (scrollable)

### Private Chat (Direct Messages)

**Mechanics:**
- Right-click player → "Private Message" opens 1-on-1 window
- Messages only visible to both players
- Notification when player sends you a PM
- Can reply via chat window

**Features:**
- Login/logout notifications (optional, can be toggled)
- Ignore list works for PMs too
- Persistent history (for session duration)

### Channel Switching

**UI:**
- Chat interface has tabs: "Public" and "Private"
- Players switch between tabs to send to different channels
- Chat input switches based on active tab
- Or: Use prefix commands like `/p` for public, `/m PlayerName` for private

**Default:** Public chat on startup

---

## GRAND EXCHANGE (GE) SYSTEM

### How It Works

**Mechanics:**
- Players create **buy orders** ("I want to buy 100 logs at 50 gp each")
- Players create **sell orders** ("I want to sell 100 logs at 55 gp each")
- System **auto-matches** when orders overlap (buy ≥ sell price)
- Transaction happens instantly, both players notified

**Example:**
```
Player A posts: "Sell 100 logs at 55 gp each"
Player B posts: "Buy 100 logs at 60 gp each"
System matches at 55 gp (middle price)
Transaction executes, A gets 5,500 gp (minus 2% fee)
```

### Fee System

**Transaction Fee:** 2% (like real OSRS)

**Example:**
```
Sell 100 logs at 100 gp = 10,000 gp
2% fee = 200 gp
Player receives: 9,800 gp
```

**Fee destination:** Removed from economy (money sink)

### Trade Limits (Prevent Manipulation)

**Quantity caps per item (example values, adjust as needed):**
- Common items (logs, fish, bones): 10,000 unit limit
- Rare items (equipment, rune items): 1,000 unit limit
- Unique items: 100 unit limit

**Purpose:** Prevent single player from cornering entire market

**Enforcement:**
- GE rejects offers exceeding limit
- Error: "You cannot offer more than 10,000 of this item"

### Auto-Matching Algorithm

**Matching order:**
1. Check all buy orders sorted by price (highest first)
2. Check all sell orders sorted by price (lowest first)
3. When buy_price ≥ sell_price, execute trade at seller's price
4. Repeat until no more matches

**Partial fills allowed:** If buy order wants 100 but sell order has 50, execute 50, leave buy order with 50 remaining

### GE Interface

**UI Components:**
- Search/filter items
- Current offers tab (shows active buy/sell orders)
- Offer history tab (shows completed trades)
- Create new offer button

**Fees displayed:** Show player "Will cost 2% fee (~200 gp)" before confirming

---

## DIRECT P2P TRADING SYSTEM

### Trade Initiation

**Mechanics:**
- Right-click player → "Trade" option
- Opens trade interface (split screen, both sides visible)
- Both players see proposed items and gold amounts

### Trade Interface

**Layout:**
```
[You]                          [Other Player]
Your Items:                    Their Items:
- 100 Logs                     - 5,000 Coins
- 0 Coins offered             - 0 Coins offered

[Clear]  [Accept - Stage 1]   [Clear]  [Accept - Stage 1]
```

**Features:**
- Drag items into offer
- Type gold amount to offer
- Real-time updates (both see changes immediately)
- Can cancel at any time (before both accept)

### Double-Accept (Scam Prevention)

**Stage 1 - Player Proposes:**
- Player A clicks "Accept" with proposed offer
- Message appears: "Player A accepts the offer"
- Both can still modify items/gold
- Either player can click "Cancel" to restart

**Stage 2 - Both Confirm:**
- Once both have clicked "Accept" (Stage 1), both see confirmation screen
- Final review: "Confirm this trade?"
- BOTH must click "Confirm" to execute
- If either player declines here, trade cancelled, back to Stage 1

**Example:**
```
Player A: [Offers 100 logs] → Clicks "Accept - Stage 1"
Player B: [Offers 5,000 coins] → Clicks "Accept - Stage 1"
System: "Both players ready. Final confirmation?"
Player A: Clicks "Confirm Trade"
Player B: Clicks "Confirm Trade"
Trade executes: A gets 5,000 coins, B gets 100 logs
```

### Trade Limits

**No limits on quantity or frequency** (unlimited direct trades)

**Rationale:** GE has limits. Direct trading is for player-to-player negotiation, should be free and flexible.

### Scam Prevention Details

**What's protected:**
- Last-second item swaps (Stage 2 confirmation prevents this)
- Accidental trades (confirmation required)

**What's NOT protected (user's responsibility):**
- Unfair prices ("I'll give you 1,000 coins for a Dragon sword" - too low, but not prevented)
- "Trust trades" where one player sends first (not protected)
- Verbal agreements outside game

**Philosophy:** GE is safe (auto-matching, limits). P2P trades are flexible but require player awareness.

---

## NETWORK IMPLICATIONS (Server-Side)

### Chat Messages

**Protocol messages:**
```protobuf
message ChatMessage {
  int32 player_id = 1;
  string player_name = 2;
  string message_text = 3;
  enum ChatType {
    PUBLIC = 0;
    PRIVATE = 1;
  }
  ChatType type = 4;
  int32 target_player_id = 5; // For PRIVATE only
  int64 timestamp = 6;
}
```

**Server behavior:**
- Validate message length
- Validate sender is online
- For PUBLIC: broadcast to all players
- For PRIVATE: send only to target player (if online), queue if offline (future)

### GE Transactions

**Protocol messages:**
```protobuf
message GEOffer {
  enum OfferType { BUY = 0; SELL = 1; }
  int32 item_id = 1;
  int32 quantity = 2;
  int32 price_per_unit = 3;
  OfferType type = 4;
  int32 player_id = 5;
}

message GETradeExecuted {
  int32 buyer_id = 1;
  int32 seller_id = 2;
  int32 item_id = 3;
  int32 quantity = 4;
  int32 price = 5;
  int32 fee = 6;
}
```

**Server behavior:**
- Validate offer (item exists, quantity ≤ limit, player has items/coins)
- Check inventory space
- Add offer to GE order book
- On each new offer, scan for matches
- Execute matches, remove from order book
- Update both players' inventories
- Send notification messages

### P2P Trading

**Protocol messages:**
```protobuf
message TradeRequest {
  int32 initiator_id = 1;
  int32 target_id = 2;
}

message TradeOffer {
  int32 player_id = 1;
  repeated int32 item_ids = 2; // Items offered
  int32 gold_amount = 3;
  enum Stage { STAGE_1 = 0; STAGE_2 = 1; }
  Stage current_stage = 4;
}

message TradeExecuted {
  int32 player_a_id = 1;
  int32 player_b_id = 2;
  // Trade details...
}
```

**Server behavior:**
- Validate both players are online and in range
- Create trade session
- Sync offers in real-time
- On Stage 1 accept: mark player ready
- On Stage 2 confirm: validate inventories, execute swap, remove session

---

## IMPLEMENTATION CHECKLIST (MVP)

### Chat System
- ✅ Public chat channel (broadcast to all players)
- ✅ Private chat (1-on-1 messages)
- ✅ Channel switching (tabs or commands)
- ✅ Message display in chat window
- ✅ Ignore list (block player from seeing messages)
- ✅ Message history (scrollable, current session)
- ❌ Profanity filter (post-MVP)
- ❌ Persistent message history (post-MVP)

### Grand Exchange
- ✅ Buy/sell offer interface
- ✅ Auto-matching algorithm
- ✅ 2% transaction fee
- ✅ Quantity limits (prevent manipulation)
- ✅ Offer history display
- ✅ Search/filter items
- ✅ Partial fills (orders remaining after partial match)
- ❌ Price history charts (post-MVP)
- ❌ Price alerts (post-MVP)

### Direct P2P Trading
- ✅ Trade request (right-click player)
- ✅ Trade interface (split screen, both items visible)
- ✅ Real-time offer updates
- ✅ Stage 1 accept (player ready)
- ✅ Stage 2 confirm (final confirmation)
- ✅ Inventory validation (both players have items)
- ✅ Unlimited trade frequency/quantity
- ✅ Cancel button (at any stage)

---

**Status:** LOCKED IN - Both systems, all features, exact OSRS mechanics

