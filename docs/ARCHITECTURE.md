# MOODFIT architecture

The Android client is Jetpack Compose. AI generation is abstracted behind `MakeoverProvider`.

v0.2 implements `FalFlux2Provider`, which submits the LOOK reference first and the YOU identity reference second to FLUX.2 multi-reference image editing. The prompt explicitly instructs the model to preserve selected styling attributes from LOOK while using the face identity from YOU.

This keeps the UI independent from the model provider and allows later replacement with a self-hosted ComfyUI/PuLID/InstantID pipeline or another API.
