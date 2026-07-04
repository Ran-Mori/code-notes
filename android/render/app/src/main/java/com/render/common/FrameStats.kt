package com.render.common

import java.util.Locale

private const val SIXTY_HZ_NS = 16_666_666L
private const val THIRTY_HZ_NS = 33_333_333L

class FrameStats {
    private var frames = 0L
    private var slow16 = 0L
    private var slow33 = 0L
    private var totalNs = 0L
    private var drawNs = 0L
    private var syncNs = 0L
    private var commandNs = 0L
    private var swapNs = 0L
    private var gpuNs = 0L
    private var gpuSamples = 0L
    private var lastTotalNs = 0L

    fun record(
        totalNs: Long,
        drawNs: Long,
        syncNs: Long,
        commandNs: Long,
        swapNs: Long,
        gpuNs: Long
    ): FrameStatsSnapshot {
        frames++
        if (totalNs > SIXTY_HZ_NS) slow16++
        if (totalNs > THIRTY_HZ_NS) slow33++
        this.totalNs += totalNs.coerceAtLeast(0L)
        this.drawNs += drawNs.coerceAtLeast(0L)
        this.syncNs += syncNs.coerceAtLeast(0L)
        this.commandNs += commandNs.coerceAtLeast(0L)
        this.swapNs += swapNs.coerceAtLeast(0L)
        if (gpuNs >= 0L) {
            this.gpuNs += gpuNs
            gpuSamples++
        }
        lastTotalNs = totalNs
        return snapshot()
    }

    fun reset() {
        frames = 0L
        slow16 = 0L
        slow33 = 0L
        totalNs = 0L
        drawNs = 0L
        syncNs = 0L
        commandNs = 0L
        swapNs = 0L
        gpuNs = 0L
        gpuSamples = 0L
        lastTotalNs = 0L
    }

    private fun snapshot(): FrameStatsSnapshot =
        FrameStatsSnapshot(
            frames = frames,
            slow16 = slow16,
            slow33 = slow33,
            avgTotalNs = average(totalNs, frames),
            avgDrawNs = average(drawNs, frames),
            avgSyncNs = average(syncNs, frames),
            avgCommandNs = average(commandNs, frames),
            avgSwapNs = average(swapNs, frames),
            avgGpuNs = if (gpuSamples > 0L) average(gpuNs, gpuSamples) else -1L,
            lastTotalNs = lastTotalNs
        )

    private fun average(total: Long, count: Long): Long =
        if (count == 0L) 0L else total / count
}

data class FrameStatsSnapshot(
    val frames: Long,
    val slow16: Long,
    val slow33: Long,
    val avgTotalNs: Long,
    val avgDrawNs: Long,
    val avgSyncNs: Long,
    val avgCommandNs: Long,
    val avgSwapNs: Long,
    val avgGpuNs: Long,
    val lastTotalNs: Long
) {
    fun format(
        note: String = "Interpretation: draw is mostly UI-thread recording work; sync/command/swap point at HWUI/RenderThread/GPU handoff."
    ): String =
        "frames=$frames  >16.6ms=$slow16  >33.3ms=$slow33\n" +
            "avg total=${ms(avgTotalNs)}  last total=${ms(lastTotalNs)}\n" +
            "avg draw=${ms(avgDrawNs)}  sync=${ms(avgSyncNs)}  command=${ms(avgCommandNs)}\n" +
            "avg swap=${ms(avgSwapNs)}  gpu=${if (avgGpuNs >= 0L) ms(avgGpuNs) else "API31+"}\n" +
            note

    companion object {
        fun empty(): FrameStatsSnapshot =
            FrameStatsSnapshot(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, -1L, 0L)

        private fun ms(ns: Long): String =
            String.format(Locale.US, "%.2fms", ns / 1_000_000.0)
    }
}
