package com.moodfit.app.ai

import android.net.Uri

data class MakeoverRequest(
    val identityImage: Uri,
    val lookImage: Uri,
    val transferOutfit: Boolean = true,
    val transferHair: Boolean = true,
    val transferMakeup: Boolean = true,
    val transferPose: Boolean = true,
    val transferBackground: Boolean = false,
    val identityStrength: Float = 0.9f,
    val lookStrength: Float = 0.85f,
    val creativity: Float = 0.45f,
    val prompt: String = ""
)

sealed interface MakeoverResult {
    data class Success(val imageUrl: String) : MakeoverResult
    data class Error(val message: String) : MakeoverResult
}

interface MakeoverProvider {
    suspend fun generate(request: MakeoverRequest): MakeoverResult
}

class DemoMakeoverProvider : MakeoverProvider {
    override suspend fun generate(request: MakeoverRequest): MakeoverResult =
        MakeoverResult.Error("No AI provider configured yet.")
}
