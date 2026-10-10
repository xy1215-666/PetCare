<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import { init, use } from 'echarts/core'
import type { EChartsType } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { WeightChartPoint } from '../domain/weightMetrics'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{ points: WeightChartPoint[] }>()
const chartElement = ref<HTMLElement | null>(null)
let chart: EChartsType | null = null
let resizeObserver: ResizeObserver | null = null

function renderChart(): void {
  if (!chart) return
  chart.setOption({
    animationDuration: 350,
    grid: { left: 42, right: 20, top: 16, bottom: 34 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e2e9df',
      textStyle: { color: '#405047', fontSize: 12 },
      formatter: (items: unknown) => {
        const item = Array.isArray(items) ? items[0] as { axisValue?: string; data?: number } : null
        return item ? `${item.axisValue}<br/><strong>${Number(item.data).toFixed(3)} kg</strong>` : ''
      },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.points.map(({ date }) => date),
      axisLine: { lineStyle: { color: '#e0e8df' } },
      axisTick: { show: false },
      axisLabel: { color: '#879188', fontSize: 11, hideOverlap: true },
    },
    yAxis: {
      type: 'value',
      scale: true,
      name: 'kg',
      nameTextStyle: { color: '#879188', padding: [0, 0, 0, 6] },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#879188', fontSize: 11 },
      splitLine: { lineStyle: { color: '#edf1eb', type: 'dashed' } },
    },
    series: [{
      type: 'line',
      smooth: 0.32,
      data: props.points.map(({ weight }) => weight),
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: '#6a9879', width: 3 },
      itemStyle: { color: '#6a9879', borderColor: '#fff', borderWidth: 2 },
      areaStyle: { color: 'rgba(118, 163, 129, 0.12)' },
    }],
  })
}

async function refreshChart(): Promise<void> {
  await nextTick()
  renderChart()
}

watch(() => props.points, () => void refreshChart(), { deep: true })

onMounted(async () => {
  await nextTick()
  if (!chartElement.value) return
  chart = init(chartElement.value, undefined, { renderer: 'canvas' })
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartElement.value)
  renderChart()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="chartElement" class="weight-chart" role="img" aria-label="体重变化趋势图" />
</template>

<style scoped>
.weight-chart { width: 100%; height: 280px; }
@media (max-width: 600px) { .weight-chart { height: 240px; } }
</style>
