# MOODFIT v0.2.0

This development branch adds the first real AI generation path.

- FLUX.2 multi-reference editing through fal.ai
- YOU + LOOK input images
- selectable outfit, hair, make-up, pose and background transfer
- optional prompt
- runtime API key entry (not committed)
- loading state and generated result preview
- privacy-oriented `X-Fal-Store-IO: 0` request header

The final production architecture should use a server-side proxy for provider credentials. The direct runtime key field in v0.2 is intended for personal/testing use.
