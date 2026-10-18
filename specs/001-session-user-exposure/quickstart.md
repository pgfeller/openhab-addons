# Quickstart - Session User Exposure

This guide validates that the Jellyfin session user exposure feature works end-to-end.

## Prerequisites

- openHAB runtime (OH 5.2.3) installed and running
- Jellyfin server accessible from openHAB
- Jellyfin binding installed (`org.openhab.binding.jellyfin`)
- Feature enabled: `001-session-user-exposure`

## Setup

1. Start openHAB and ensure the Jellyfin binding is loaded
2. Configure the Jellyfin binding connection (server URL, API key if required)
3. Ensure a user is logged into the Jellyfin server

## Verification Steps

### Step 1: Verify Binding is Active

```bash
# Check that the Jellyfin binding is loaded
openhab> binding:list | grep jellyfin

# Expected: org.openhab.binding.jellyfin should appear in the list
```

### Step 2: Verify Client Thing exists

```bash
# List things and find the client thing
openhab> thing:list | grep jellyfin

# Expected: A thing with uid ending in "client-thing" should appear
```

### Step 3: Check Channel States

```bash
# Read the user-name channel state
openhab> channel:state org.openhab.binding.jellyfin:client-thing:user-name

# If user is authenticated: should display the Jellyfin user's display name
# If no user authenticated: should be NULL (NULL indicates no session state, not empty string)

# Read the user-id channel state
openhab> channel:state org.openhab.binding.jellyfin:client-thing:user-id

# If user is authenticated: should display the Jellyfin user's UUID
# If no user authenticated: should be NULL or empty
```

### Step 4: Test Session Change

```bash
# Simulate session change (depends on binding implementation)
# - User logs in: channels should update to show user name/ID
# - User logs out: channels should clear to NULL
# - User switches accounts: channels should update to new user's identity
```

## Expected Outcomes

| Scenario | user-name Channel | user-id Channel |
| ---------- | ------------------ | ----------------- |
| User authenticated | Display name string | UUID string |
| No session active | NULL | NULL |
| Anonymous session | NULL | NULL |
| **Note**: "No session active" means no Jellyfin session exists; "Anonymous session" means a session where UserName/UserId are null/absent but a session object exists. Both result in NULL channel state, but the underlying cause differs. |

## Troubleshooting

### Channels showing NULL when user is authenticated

- Check Jellyfin server logs for session errors
- Verify binding configuration (server URL, credentials)
- Ensure the session event bus is propagating events

### Channels not updating after session change

- Verify the session event bus is functioning
- Check ClientHandler implementation for event subscription
- Review binding logs for error messages

### Channels showing stale data

- Ensure the session event bus priority is correct
- Check for competing event sources
- Review the binding's event filtering logic
