# Security notes

MOODFIT v0.2 does not commit or hard-code the fal.ai API key. For this personal test build the key is entered at runtime and held only in memory for the current app session.

For any distributed/public release, replace direct provider authentication with a server-side proxy and apply per-user quotas/rate limiting.
