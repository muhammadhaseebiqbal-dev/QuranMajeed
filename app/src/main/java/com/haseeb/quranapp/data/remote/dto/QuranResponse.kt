package com.haseeb.quranapp.data.remote.dto

data class QuranResponse(
    val verses: List<VerseDto>?
)

data class VerseDto(
    val id: Int,
    val verse_key: String,
    val text_uthmani: String
)

data class RecitationResponse(
    val audio_files: List<AudioFileDto>
)

data class AudioFileDto(
    val verse_key: String,
    val url: String,
    // Segments are not always present or needed if we just play verse by verse
    // But for full chapter audio with timestamps:
    // "format": "mp3", "audio_url": "...", "duration": 123, "segments": [[start, end, segment_index], ...]
    // Quran.com v4 recitation by chapter returns one audio file?
    // Let's check Recitation by Chapter endpoint structure.
    // It returns "audio_files": [ { "verse_key": "1:1", "url": "..." }, ... ] for "Ayah by Ayah" recitations
    // OR "audio_file": { "chapter_id": 1, "file_size": ..., "format": "mp3", "audio_url": "..." } for "Chapter" recitations?
    // We want Ayah by Ayah for easier sync at this stage OR valid timestamps.
    // Let's use Ayah by Ayah recitation (Reciter 7 - Mishari Rashid Al-Afasy is available as VerseByVerse?)
    // Actually, Reciter 7 (Mishari) in v4 is often serving full surah files.
    // Let's stick to the structure that supports VerseByVerse for now to perform "Karaoke".
)
