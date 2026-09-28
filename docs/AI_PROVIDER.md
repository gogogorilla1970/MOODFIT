# MOODFIT v0.2 AI provider

MOODFIT v0.2 uses `fal-ai/flux-2/edit` for multi-reference image editing.

Image order:
1. LOOK reference (base composition/style)
2. YOU reference (face identity)

The app sends both images as base64 data URIs and asks FLUX.2 to preserve selected look attributes while replacing the identity with the adult person from the YOU reference.

## API key

For v0.2 the fal.ai API key is entered at runtime and kept only in Compose state for the current app session. It is not hard-coded or committed to GitHub.

For a public production app, move API authentication to a server-side proxy so the provider key is never exposed to the mobile client.

## Privacy

The request sets `X-Fal-Store-IO: 0` to disable normal request input/output payload retention at fal where supported. Generated output URLs may still be hosted by the provider/CDN according to provider policy.
