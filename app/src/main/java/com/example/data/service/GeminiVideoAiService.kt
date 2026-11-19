package com.example.data.service

import android.util.Log
import com.example.data.model.CaptionStyle
import com.example.data.model.GeneratedClip
import com.example.data.model.SubtitleLanguage
import com.example.data.model.TransitionStyle
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiVideoAnalysisResult(
    val transcript: String,
    val summary: String,
    val detectedDurationSeconds: Int,
    val clips: List<AiClipDraft>
)

data class AiClipDraft(
    val startSec: Int,
    val endSec: Int,
    val viralScore: Int,
    val hookReason: String,
    val emotionTag: String,
    val viralTitle: String,
    val seoDescription: String,
    val youtubeCaption: String,
    val instagramCaption: String,
    val thumbnailConcept: String,
    val hashtags: List<String>,
    val keywords: List<String>,
    val subtitleEn: String,
    val subtitleHi: String,
    val subtitleHinglish: String,
    val transitionStyle: String
)

data class RegeneratedClipCopy(
    val viralTitle: String,
    val seoDescription: String,
    val youtubeCaption: String,
    val instagramCaption: String,
    val thumbnailConcept: String,
    val hashtags: List<String>,
    val keywords: List<String>,
    val subtitleEn: String,
    val subtitleHi: String,
    val subtitleHinglish: String
)

class GeminiVideoAiService {

    private val videoAnalysisSchema = Schema.obj(
        properties = mapOf(
            "transcript" to Schema.string(description = "Full clean video transcript or scene-by-scene narration without cut sentences"),
            "summary" to Schema.string(description = "Concise 2-3 sentence summary of the video's core topic and highlights"),
            "detectedDurationSeconds" to Schema.integer(description = "Estimated or confirmed total video duration in seconds"),
            "clips" to Schema.array(
                description = "High-retention short-form vertical clips extracted from engaging moments",
                items = Schema.obj(
                    properties = mapOf(
                        "startSec" to Schema.integer(description = "Start timestamp in seconds aligned to sentence start"),
                        "endSec" to Schema.integer(description = "End timestamp in seconds aligned to sentence end (15 to 60 seconds after startSec)"),
                        "viralScore" to Schema.integer(description = "Viral potential score from 70 to 99"),
                        "hookReason" to Schema.string(description = "Why this moment hooks viewers (emotion, twist, punchline, topic shift)"),
                        "emotionTag" to Schema.string(description = "Single uppercase emotion/hook tag like SURPRISE, INSIGHT, DEBATE, EMOTIONAL, VIRAL_HOOK, TUTORIAL"),
                        "viralTitle" to Schema.string(description = "High-CTR viral short-form title under 75 characters"),
                        "seoDescription" to Schema.string(description = "SEO-optimized description with natural keywords"),
                        "youtubeCaption" to Schema.string(description = "YouTube Shorts caption with call-to-action and #Shorts"),
                        "instagramCaption" to Schema.string(description = "Instagram Reels engaging caption with hook and hashtags"),
                        "thumbnailConcept" to Schema.string(description = "Suggested thumbnail visual composition and bold overlay text"),
                        "hashtags" to Schema.array(items = Schema.string(), description = "5 to 8 relevant hashtags starting with #"),
                        "keywords" to Schema.array(items = Schema.string(), description = "5 to 8 SEO keywords"),
                        "subtitleEn" to Schema.string(description = "Complete synchronized English spoken subtitle script for this clip"),
                        "subtitleHi" to Schema.string(description = "Natural Hindi (Devanagari) subtitle translation of this clip"),
                        "subtitleHinglish" to Schema.string(description = "Natural Hinglish (Roman Hindi + English) subtitle script for Indian social audiences"),
                        "transitionStyle" to Schema.enumeration(
                            values = listOf("SMOOTH_CUT", "ZOOM_PUNCH", "WHIP_PAN", "FADE_GLOW"),
                            description = "Best transition style for this clip's pacing"
                        )
                    )
                )
            )
        )
    )

    private val clipCopySchema = Schema.obj(
        properties = mapOf(
            "viralTitle" to Schema.string(description = "Fresh high-CTR viral title"),
            "seoDescription" to Schema.string(description = "Refined SEO-friendly description"),
            "youtubeCaption" to Schema.string(description = "YouTube Shorts caption"),
            "instagramCaption" to Schema.string(description = "Instagram Reels caption"),
            "thumbnailConcept" to Schema.string(description = "Eye-catching thumbnail concept"),
            "hashtags" to Schema.array(items = Schema.string(), description = "6 to 8 relevant hashtags"),
            "keywords" to Schema.array(items = Schema.string(), description = "6 to 8 SEO keywords"),
            "subtitleEn" to Schema.string(description = "Polished English subtitles"),
            "subtitleHi" to Schema.string(description = "Polished Hindi (Devanagari) subtitles"),
            "subtitleHinglish" to Schema.string(description = "Polished Hinglish subtitles")
        )
    )

    private val analysisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = videoAnalysisSchema
                temperature = 0.7f
            }
        )
    }

    private val copyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = clipCopySchema
                temperature = 0.8f
            }
        )
    }

    suspend fun analyzeVideoAndGenerateClips(
        videoInfo: InspectedVideoInfo,
        requestedClipCount: Int,
        creatorNotes: String,
        preferredLanguage: SubtitleLanguage
    ): Result<AiVideoAnalysisResult> {
        val safeClipCount = requestedClipCount.coerceIn(1, 20)
        val totalDurationSec = videoInfo.durationSeconds.coerceAtLeast(30)

        return try {
            val prompt = buildString {
                appendLine("You are ClipGenius AI, a world-class viral short-form video producer and multilingual subtitle director.")
                appendLine("Analyze the provided video metadata, visual frames, and context to generate EXACTLY $safeClipCount non-overlapping high-retention short-form vertical clips.")
                appendLine()
                appendLine("VIDEO DETAILS:")
                appendLine("- Title: ${videoInfo.title}")
                appendLine("- Channel / Source: ${videoInfo.channelName} (${videoInfo.sourceType})")
                appendLine("- Reference URL/URI: ${videoInfo.sourceUrl}")
                appendLine("- Detected Duration: ${totalDurationSec} seconds")
                appendLine("- Source Resolution: ${videoInfo.resolutionLabel}")
                appendLine("- Preferred Primary Subtitle Language: ${preferredLanguage.label}")
                if (creatorNotes.isNotBlank()) {
                    appendLine("- Creator Focus / Context Notes: ${creatorNotes.trim()}")
                }
                appendLine()
                appendLine("STRICT REQUIREMENTS:")
                appendLine("1. Generate a complete, coherent transcript / scene breakdown covering the video.")
                appendLine("2. Identify the top $safeClipCount engaging moments: emotional reactions, topic changes, surprising facts, quotable conversations, and viral hooks.")
                appendLine("3. Keep all clip timestamps strictly within 0..$totalDurationSec seconds, with endSec > startSec, and NEVER cut sentences in the middle.")
                appendLine("4. For EVERY clip, generate natural synchronized subtitles in ALL THREE languages: English (subtitleEn), Hindi Devanagari (subtitleHi), and Hinglish (subtitleHinglish).")
                appendLine("5. Generate viralTitle, seoDescription, youtubeCaption, instagramCaption, thumbnailConcept, 5-8 hashtags, and 5-8 keywords for each clip.")
            }

            val response = withTimeout(60_000L) {
                analysisModel.generateContent(
                    content {
                        videoInfo.previewFrames.take(6).forEach { frame ->
                            image(frame)
                        }
                        text(prompt)
                    }
                )
            }

            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty analysis response."))

            val root = JSONObject(jsonText)
            val transcript = root.optString("transcript", "")
            val summary = root.optString("summary", "")
            val detectedDuration = root.optInt("detectedDurationSeconds", totalDurationSec)
                .coerceAtLeast(15)
            val clipsArray = root.optJSONArray("clips")
                ?: return Result.failure(IllegalStateException("No clips array returned by Gemini."))

            val clips = mutableListOf<AiClipDraft>()
            for (i in 0 until clipsArray.length()) {
                val obj = clipsArray.getJSONObject(i)
                val rawStart = obj.optInt("startSec", (i * 25).coerceAtMost(detectedDuration - 10))
                    .coerceIn(0, (detectedDuration - 5).coerceAtLeast(0))
                val rawEnd = obj.optInt("endSec", rawStart + 25)
                    .coerceIn((rawStart + 5).coerceAtMost(detectedDuration), detectedDuration.coerceAtLeast(rawStart + 5))

                val hashtagsJson = obj.optJSONArray("hashtags")
                val hashtags = buildList {
                    if (hashtagsJson != null) {
                        for (h in 0 until hashtagsJson.length()) {
                            val tag = hashtagsJson.optString(h).trim()
                            if (tag.isNotBlank()) add(if (tag.startsWith("#")) tag else "#$tag")
                        }
                    }
                }.take(15)

                val keywordsJson = obj.optJSONArray("keywords")
                val keywords = buildList {
                    if (keywordsJson != null) {
                        for (k in 0 until keywordsJson.length()) {
                            val kw = keywordsJson.optString(k).trim()
                            if (kw.isNotBlank()) add(kw)
                        }
                    }
                }.take(15)

                clips.add(
                    AiClipDraft(
                        startSec = rawStart,
                        endSec = rawEnd,
                        viralScore = obj.optInt("viralScore", 88).coerceIn(50, 100),
                        hookReason = obj.optString("hookReason", "High-retention engaging hook"),
                        emotionTag = obj.optString("emotionTag", "VIRAL_HOOK").uppercase().take(32),
                        viralTitle = obj.optString("viralTitle", "Viral Highlight #${i + 1}"),
                        seoDescription = obj.optString("seoDescription", ""),
                        youtubeCaption = obj.optString("youtubeCaption", ""),
                        instagramCaption = obj.optString("instagramCaption", ""),
                        thumbnailConcept = obj.optString("thumbnailConcept", ""),
                        hashtags = hashtags,
                        keywords = keywords,
                        subtitleEn = obj.optString("subtitleEn", ""),
                        subtitleHi = obj.optString("subtitleHi", ""),
                        subtitleHinglish = obj.optString("subtitleHinglish", ""),
                        transitionStyle = TransitionStyle.fromRaw(obj.optString("transitionStyle")).name
                    )
                )
            }

            if (clips.isEmpty()) {
                return Result.failure(IllegalStateException("Gemini did not produce any clips. Please try again."))
            }

            Result.success(
                AiVideoAnalysisResult(
                    transcript = transcript,
                    summary = summary,
                    detectedDurationSeconds = detectedDuration,
                    clips = clips
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini video analysis timed out", e)
            Result.failure(Exception("AI analysis timed out after 60 seconds. Try fewer clips or retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini video analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun regenerateClipCopy(
        clip: GeneratedClip,
        toneGoal: String
    ): Result<RegeneratedClipCopy> {
        return try {
            val prompt = buildString {
                appendLine("Rewrite and upgrade the viral metadata and multilingual subtitles for this short-form clip.")
                appendLine("Target Tone / Style: $toneGoal")
                appendLine("Original Title: ${clip.viralTitle}")
                appendLine("Hook Reason: ${clip.hookReason}")
                appendLine("English Subtitles: ${clip.subtitleEn}")
                appendLine("Keep the spoken meaning faithful while maximizing retention for YouTube Shorts and Instagram Reels.")
                appendLine("Return fresh viralTitle, seoDescription, youtubeCaption, instagramCaption, thumbnailConcept, hashtags, keywords, and natural English, Hindi (Devanagari), and Hinglish subtitles.")
            }

            val response = withTimeout(60_000L) {
                copyModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))
            val obj = JSONObject(jsonText)

            val hashtags = buildList {
                val arr = obj.optJSONArray("hashtags")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i).trim()
                        if (s.isNotBlank()) add(if (s.startsWith("#")) s else "#$s")
                    }
                }
            }.take(15)

            val keywords = buildList {
                val arr = obj.optJSONArray("keywords")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i).trim()
                        if (s.isNotBlank()) add(s)
                    }
                }
            }.take(15)

            Result.success(
                RegeneratedClipCopy(
                    viralTitle = obj.optString("viralTitle", clip.viralTitle),
                    seoDescription = obj.optString("seoDescription", clip.seoDescription),
                    youtubeCaption = obj.optString("youtubeCaption", clip.youtubeCaption),
                    instagramCaption = obj.optString("instagramCaption", clip.instagramCaption),
                    thumbnailConcept = obj.optString("thumbnailConcept", clip.thumbnailConcept),
                    hashtags = hashtags.ifEmpty { clip.hashtags },
                    keywords = keywords.ifEmpty { clip.keywords },
                    subtitleEn = obj.optString("subtitleEn", clip.subtitleEn),
                    subtitleHi = obj.optString("subtitleHi", clip.subtitleHi),
                    subtitleHinglish = obj.optString("subtitleHinglish", clip.subtitleHinglish)
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Copy regeneration timed out", e)
            Result.failure(Exception("AI copy regeneration timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Copy regeneration failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "GeminiVideoAiService"
    }
}
