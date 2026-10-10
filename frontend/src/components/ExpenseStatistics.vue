<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { init, use } from 'echarts/core'
import type { EChartsType } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ExpenseCategoryTotal, ExpenseMonthTotal, ExpensePetTotal } from '../api/expenses'
import { categoryChartPoints, categoryColor, monthlyTrendChartPoints, petExpenseChartPoints } from '../domain/expenseMetrics'

use([PieChart, LineChart, BarChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  categories: ExpenseCategoryTotal[]
  trend: ExpenseMonthTotal[]
  pets: ExpensePetTotal[]
  showPetChart: boolean
}>()

const chartRoot = ref<HTMLElement | null>(null)
const categoryElement = ref<HTMLElement | null>(null)
const trendElement = ref<HTMLElement | null>(null)
const petElement = ref<HTMLElement | null>(null)
let categoryChart: EChartsType | null = null
let trendChart: EChartsType | null = null
let petChart: EChartsType | null = null
let resizeObserver: ResizeObserver | null = null

function render(): void {
  if (categoryChart) {
    const points = categoryChartPoints(props.categories)
    categoryChart.setOption({
      color: props.categories.map(({ category }) => categoryColor(category)),
      tooltip: { trigger: 'item', formatter: '{b}<br/>¥{c} · {d}%' },
      legend: { bottom: 0, type: 'scroll', textStyle: { color: '#718076', fontSize: 11 } },
      series: [{ type: 'pie', radius: ['42%', '70%'], center: ['50%', '43%'], itemStyle: { borderColor: '#fff', borderWidth: 3 }, label: { color: '#66756b', formatter: '{b} {d}%' }, data: points }],
    }, true)
  }
  if (trendChart) {
    const points = monthlyTrendChartPoints(props.trend)
    trendChart.setOption({
      grid: { left: 46, right: 20, top: 20, bottom: 38 },
      tooltip: { trigger: 'axis', valueFormatter: (value: unknown) => `¥${Number(value).toFixed(2)}` },
      xAxis: { type: 'category', data: points.map(({ month }) => month), axisLine: { lineStyle: { color: '#e4ebe3' } }, axisTick: { show: false }, axisLabel: { color: '#849087', fontSize: 11 } },
      yAxis: { type: 'value', min: 0, axisLabel: { color: '#849087', fontSize: 11 }, splitLine: { lineStyle: { color: '#edf1eb', type: 'dashed' } } },
      series: [{ type: 'line', smooth: 0.3, data: points.map(({ value }) => value), symbolSize: 7, lineStyle: { color: '#699676', width: 3 }, itemStyle: { color: '#699676', borderColor: '#fff', borderWidth: 2 }, areaStyle: { color: 'rgba(118, 163, 129, 0.12)' } }],
    }, true)
  }
  if (petChart) {
    const points = petExpenseChartPoints(props.pets)
    petChart.setOption({
      grid: { left: 20, right: 24, top: 16, bottom: 20, containLabel: true },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (value: unknown) => `¥${Number(value).toFixed(2)}` },
      xAxis: { type: 'value', min: 0, axisLabel: { color: '#849087', fontSize: 11 }, splitLine: { lineStyle: { color: '#edf1eb', type: 'dashed' } } },
      yAxis: { type: 'category', data: points.map(({ name }) => name), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#65746a', fontSize: 12 } },
      series: [{ type: 'bar', data: points.map(({ value }) => value), barMaxWidth: 22, itemStyle: { color: '#82a889', borderRadius: [0, 8, 8, 0] } }],
    }, true)
  }
}

async function refresh(): Promise<void> {
  await nextTick()
  if (!categoryChart && categoryElement.value) categoryChart = init(categoryElement.value, undefined, { renderer: 'canvas' })
  if (!trendChart && trendElement.value) trendChart = init(trendElement.value, undefined, { renderer: 'canvas' })
  if (!petChart && petElement.value) petChart = init(petElement.value, undefined, { renderer: 'canvas' })
  render()
}

watch(() => [props.categories, props.trend, props.pets, props.showPetChart], () => void refresh(), { deep: true })

onMounted(async () => {
  if (chartRoot.value) {
    resizeObserver = new ResizeObserver(() => [categoryChart, trendChart, petChart].forEach((chart) => chart?.resize()))
    resizeObserver.observe(chartRoot.value)
  }
  await refresh()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  categoryChart?.dispose()
  trendChart?.dispose()
  petChart?.dispose()
  categoryChart = null
  trendChart = null
  petChart = null
})
</script>

<template>
  <div ref="chartRoot" class="expense-chart-grid">
    <section class="chart-card">
      <div class="chart-heading"><div><p class="eyebrow">WHERE IT GOES</p><h3>分类占比</h3></div><span>所选时间范围</span></div>
      <div ref="categoryElement" v-show="categories.length" class="chart-canvas chart-canvas--pie" role="img" aria-label="消费分类占比图" />
      <p v-if="!categories.length" class="chart-empty">这段时间还没有消费记录。</p>
    </section>
    <section class="chart-card">
      <div class="chart-heading"><div><p class="eyebrow">MONTH BY MONTH</p><h3>近几个月的变化</h3></div><span>最近 {{ trend.length }} 个月</span></div>
      <div ref="trendElement" v-show="trend.length" class="chart-canvas" role="img" aria-label="月度消费趋势图" />
      <p v-if="!trend.length" class="chart-empty">暂无趋势数据。</p>
    </section>
    <section v-if="showPetChart" class="chart-card chart-card--wide">
      <div class="chart-heading"><div><p class="eyebrow">A LITTLE FOR EVERYONE</p><h3>家庭成员消费</h3></div><span>累计支出</span></div>
      <div ref="petElement" v-show="pets.length" class="chart-canvas chart-canvas--pets" role="img" aria-label="各宠物消费统计图" />
      <p v-if="!pets.length" class="chart-empty">还没有可展示的消费记录。</p>
    </section>
  </div>
</template>

<style scoped>
.expense-chart-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.chart-card { min-width: 0; padding: 24px; border: 1px solid #e7ebe4; border-radius: 22px; background: #fff; }
.chart-card--wide { grid-column: 1 / -1; }
.chart-heading { display: flex; align-items: end; justify-content: space-between; gap: 12px; }
.chart-heading .eyebrow { margin: 0 0 7px; color: #829785; font-size: 9px; font-weight: 700; letter-spacing: .16em; }
.chart-heading h3 { margin: 0; color: #39483e; font-size: 16px; font-weight: 600; }
.chart-heading > span { color: #939d95; font-size: 11px; white-space: nowrap; }
.chart-canvas { width: 100%; height: 300px; margin-top: 12px; }
.chart-canvas--pie { height: 310px; }
.chart-canvas--pets { height: 240px; }
.chart-empty { display: grid; min-height: 245px; place-items: center; margin: 12px 0 0; color: #96a097; font-size: 13px; }
@media (max-width: 760px) { .expense-chart-grid { grid-template-columns: 1fr; } .chart-card--wide { grid-column: auto; } .chart-card { padding: 19px; } .chart-canvas { height: 260px; } }
</style>
