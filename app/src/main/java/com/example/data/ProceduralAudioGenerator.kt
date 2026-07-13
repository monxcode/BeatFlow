package com.example.data

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object ProceduralAudioGenerator {
    private const val TAG = "ProceduralAudioGenerator"
    private const val SAMPLE_RATE = 11025 // 11.025 kHz for compact high-quality lo-fi files
    private const val DURATION_SEC = 65    // 65 seconds to bypass the 1-minute filter
    private const val NUM_SAMPLES = SAMPLE_RATE * DURATION_SEC // ~716,625 samples (~716 KB)

    /**
     * Seeds the local app storage with 3 premium lo-fi synth tracks if they do not exist.
     * Returns a list of generated file paths.
     */
    fun seedMusicIfEmpty(context: Context): List<File> {
        val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) 
            ?: context.filesDir
        
        val tracks = listOf(
            TrackDefinition("sunset_chill.wav", "Sunset Chill", "Mohan Parmar", "Retro Anthems", 120, "Ambient Pentatonic"),
            TrackDefinition("cyber_pulse.wav", "Cyber Pulse", "Mohan Parmar", "Future Soundscapes", 140, "Synthwave"),
            TrackDefinition("midnight_rain.wav", "Midnight Rain", "Mohan Parmar", "Nature Lo-Fi", 90, "Atmospheric Rain")
        )

        val seededFiles = mutableListOf<File>()

        for (track in tracks) {
            val file = File(musicDir, track.fileName)
            if (!file.exists() || file.length() < 100 * 1024) {
                try {
                    Log.d(TAG, "Generating procedural track: ${track.title} at ${file.absolutePath}")
                    generateTrackWav(file, track)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate procedural track: ${track.title}", e)
                }
            }
            if (file.exists()) {
                seededFiles.add(file)
            }
        }
        return seededFiles
    }

    private data class TrackDefinition(
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val bpm: Int,
        val genre: String
    )

    private fun generateTrackWav(file: File, track: TrackDefinition) {
        FileOutputStream(file).use { out ->
            // 1. Write standard 44-byte WAV header
            writeWavHeader(out, NUM_SAMPLES)

            // 2. Generate and write PCM 8-bit samples (0-255 range, 128 is center)
            val bpm = track.bpm
            val beatsPerSec = bpm / 60.0
            val samplesPerBeat = (SAMPLE_RATE / beatsPerSec).toInt()

            when (track.fileName) {
                "sunset_chill.wav" -> generateSunsetChill(out, samplesPerBeat)
                "cyber_pulse.wav" -> generateCyberPulse(out, samplesPerBeat)
                "midnight_rain.wav" -> generateMidnightRain(out, samplesPerBeat)
                else -> generateSunsetChill(out, samplesPerBeat)
            }
        }
    }

    private fun writeWavHeader(out: OutputStream, dataSize: Int) {
        val totalSize = 36 + dataSize
        val byteRate = SAMPLE_RATE * 1 * 1 // SampleRate * 1 Channel * 1 Byte/Sample (8-bit)

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        header[4] = (totalSize and 0xff).toByte()
        header[5] = ((totalSize shr 8) and 0xff).toByte()
        header[6] = ((totalSize shr 16) and 0xff).toByte()
        header[7] = ((totalSize shr 24) and 0xff).toByte()

        header[8] = 'W'.code.toByte() // WAVE
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        header[12] = 'f'.code.toByte() // fmt 
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        header[16] = 16 // format length (16 bytes)
        header[17] = 0
        header[18] = 0
        header[19] = 0

        header[20] = 1 // format type (1 = PCM)
        header[21] = 0

        header[22] = 1 // channels (1 = mono)
        header[23] = 0

        header[24] = (SAMPLE_RATE and 0xff).toByte()
        header[25] = ((SAMPLE_RATE shr 8) and 0xff).toByte()
        header[26] = ((SAMPLE_RATE shr 16) and 0xff).toByte()
        header[27] = ((SAMPLE_RATE shr 24) and 0xff).toByte()

        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()

        header[32] = 1 // block align
        header[33] = 0

        header[34] = 8 // bits per sample (8-bit)
        header[35] = 0

        header[36] = 'd'.code.toByte() // data
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        header[40] = (dataSize and 0xff).toByte()
        header[41] = ((dataSize shr 8) and 0xff).toByte()
        header[42] = ((dataSize shr 16) and 0xff).toByte()
        header[43] = ((dataSize shr 24) and 0xff).toByte()

        out.write(header)
    }

    /**
     * Sunset Chill: Pentatonic scale slow arpeggiator with an 808-style deep bass
     */
    private fun generateSunsetChill(out: OutputStream, samplesPerBeat: Int) {
        val scale = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25) // C4, D4, E4, G4, A4, C5
        val chordBases = doubleArrayOf(130.81, 146.83, 164.81, 196.00) // C3, D3, E3, G3

        val sampleBuffer = ByteArray(4096)
        var bufferIdx = 0

        for (i in 0 until NUM_SAMPLES) {
            val t = i.toDouble() / SAMPLE_RATE
            
            // Core rhythmic clocks
            val beatIdx = i / samplesPerBeat
            val noteIdx = beatIdx % scale.size
            val chordIdx = (beatIdx / 8) % chordBases.size

            // Lead Synth Arpeggio
            val leadFreq = scale[noteIdx]
            val leadPhase = 2.0 * Math.PI * leadFreq * t
            val sampleInBeat = i % samplesPerBeat
            val beatT = sampleInBeat.toDouble() / samplesPerBeat
            val envelope = exp(-4.0 * beatT) // Quick decay decay envelope
            val leadValue = sin(leadPhase) * envelope

            // Sub Bass Pad
            val bassFreq = chordBases[chordIdx] / 2.0 // Deep sub-bass (C2 range)
            val bassPhase = 2.0 * Math.PI * bassFreq * t
            val bassValue = sin(bassPhase) * 0.5 // Milder bass volume to prevent clipping

            // Combine audio layers
            val combined = (leadValue * 0.4) + (bassValue * 0.4)
            // Scale and map from [-1.0, 1.0] to [0, 255]
            val byteVal = ((combined * 127.0) + 128.0).coerceIn(0.0, 255.0).toInt().toByte()

            sampleBuffer[bufferIdx++] = byteVal
            if (bufferIdx == sampleBuffer.size) {
                out.write(sampleBuffer)
                bufferIdx = 0
            }
        }
        if (bufferIdx > 0) {
            out.write(sampleBuffer, 0, bufferIdx)
        }
    }

    /**
     * Cyber Pulse: Rythmic dark electronic synthwave loop
     */
    private fun generateCyberPulse(out: OutputStream, samplesPerBeat: Int) {
        // E minor progression
        val chords = doubleArrayOf(164.81, 196.00, 146.83, 220.00) // E3, G3, D3, A3
        val melodies = arrayOf(
            doubleArrayOf(329.63, 392.00, 369.99, 440.00), // E4, G4, F#4, A4
            doubleArrayOf(493.88, 440.00, 392.00, 369.99)  // B4, A4, G4, F#4
        )

        val sampleBuffer = ByteArray(4096)
        var bufferIdx = 0

        for (i in 0 until NUM_SAMPLES) {
            val t = i.toDouble() / SAMPLE_RATE

            val beatIdx = i / samplesPerBeat
            val chordIdx = (beatIdx / 4) % chords.size
            val melodySet = melodies[(beatIdx / 8) % 2]
            val noteIdx = beatIdx % melodySet.size

            // Rhythmic square wave synth lead
            val leadFreq = melodySet[noteIdx]
            val leadPhase = 2.0 * Math.PI * leadFreq * t
            val leadWav = if (sin(leadPhase) > 0.0) 1.0 else -1.0 // Square wave edge
            val sampleInBeat = i % samplesPerBeat
            val envelope = exp(-5.0 * (sampleInBeat.toDouble() / samplesPerBeat))
            val leadValue = leadWav * envelope * 0.3

            // Pulsing techno bass
            val bassFreq = chords[chordIdx]
            val bassPhase = 2.0 * Math.PI * bassFreq * t
            // Bass pulses 4 times per beat
            val bassPulseIdx = (i * 4) / samplesPerBeat
            val bassEnvelope = exp(-3.0 * ((i * 4) % samplesPerBeat).toDouble() / samplesPerBeat)
            val bassValue = sin(bassPhase) * bassEnvelope * 0.4

            // Combine layers
            val combined = leadValue + bassValue
            val byteVal = ((combined * 127.0) + 128.0).coerceIn(0.0, 255.0).toInt().toByte()

            sampleBuffer[bufferIdx++] = byteVal
            if (bufferIdx == sampleBuffer.size) {
                out.write(sampleBuffer)
                bufferIdx = 0
            }
        }
        if (bufferIdx > 0) {
            out.write(sampleBuffer, 0, bufferIdx)
        }
    }

    /**
     * Midnight Rain: Soft low-pass filtered rain noise background with gentle piano-like atmospheric chords
     */
    private fun generateMidnightRain(out: OutputStream, samplesPerBeat: Int) {
        val scale = doubleArrayOf(349.23, 440.00, 392.00, 523.25, 493.88, 349.23) // F4, A4, G4, C5, B4, F4
        val chordBases = doubleArrayOf(174.61, 220.00, 196.00, 261.63) // F3, A3, G3, C4

        val sampleBuffer = ByteArray(4096)
        var bufferIdx = 0

        var rainState = 0.0
        val random = Random(42)

        for (i in 0 until NUM_SAMPLES) {
            val t = i.toDouble() / SAMPLE_RATE

            val beatIdx = i / (samplesPerBeat * 2) // Slow tempo
            val noteIdx = beatIdx % scale.size
            val chordIdx = (beatIdx / 4) % chordBases.size

            // Soft melodic tone with long decay envelope (bell/piano)
            val leadFreq = scale[noteIdx]
            val leadPhase = 2.0 * Math.PI * leadFreq * t
            val sampleInBeat = i % (samplesPerBeat * 2)
            val envelope = exp(-1.5 * (sampleInBeat.toDouble() / (samplesPerBeat * 2))) // slow decay
            val leadValue = sin(leadPhase) * envelope * 0.35

            // Background drone bass
            val bassFreq = chordBases[chordIdx] / 2.0 // F2, A2...
            val bassPhase = 2.0 * Math.PI * bassFreq * t
            val bassValue = sin(bassPhase) * 0.25

            // Low-pass filtered noise for "rain" effect
            val rawNoise = random.nextDouble() * 2.0 - 1.0 // [-1.0, 1.0]
            rainState = (rainState * 0.94) + (rawNoise * 0.06) // Low-pass filter smoothing
            val rainValue = rainState * 0.25 // Rain background volume

            // Combine layers
            val combined = leadValue + bassValue + rainValue
            val byteVal = ((combined * 127.0) + 128.0).coerceIn(0.0, 255.0).toInt().toByte()

            sampleBuffer[bufferIdx++] = byteVal
            if (bufferIdx == sampleBuffer.size) {
                out.write(sampleBuffer)
                bufferIdx = 0
            }
        }
        if (bufferIdx > 0) {
            out.write(sampleBuffer, 0, bufferIdx)
        }
    }
}
