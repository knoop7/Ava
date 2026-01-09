package com.example.ava.microwakeword

data class WakeWord(
    val type: String,
    val wake_word: String,
    val author: String,
    val website: String,
    val model: String,
    val trained_languages: Array<String>,
    val version: Int,
    val micro: Micro,
)

data class Micro(
    val probability_cutoff: Float,
    val feature_step_size: Int,
    val sliding_window_size: Int,
    val tensor_arena_size: Int,
    val minimum_esphome_version: String,
)
