import type {
  AppetiteLevel,
  WaterIntake,
  EnergyLevel,
  ActivityLevel,
  SleepStatus,
  StoolStatus,
  UrinationStatus,
} from '../api/healthRecords'

export const healthStatusOptions = {
  appetite: [
    { value: 'EXCELLENT', label: '很好' },
    { value: 'NORMAL', label: '正常' },
    { value: 'REDUCED', label: '下降' },
    { value: 'POOR', label: '很差' },
  ],
  waterIntake: [
    { value: 'INCREASED', label: '比平时多' },
    { value: 'NORMAL', label: '正常' },
    { value: 'REDUCED', label: '比平时少' },
    { value: 'UNKNOWN', label: '不确定' },
  ],
  energyLevel: [
    { value: 'ACTIVE', label: '活跃' },
    { value: 'NORMAL', label: '正常' },
    { value: 'LOW', label: '较差' },
    { value: 'VERY_LOW', label: '很差' },
  ],
  activityLevel: [
    { value: 'HIGH', label: '活动较多' },
    { value: 'NORMAL', label: '正常' },
    { value: 'LOW', label: '活动较少' },
    { value: 'VERY_LOW', label: '几乎不动' },
  ],
  sleepStatus: [
    { value: 'LESS', label: '比平时少' },
    { value: 'NORMAL', label: '正常' },
    { value: 'MORE', label: '比平时多' },
    { value: 'UNKNOWN', label: '不确定' },
  ],
  stoolStatus: [
    { value: 'NORMAL', label: '正常' },
    { value: 'SOFT', label: '偏软' },
    { value: 'DIARRHEA', label: '腹泻' },
    { value: 'CONSTIPATED', label: '便秘' },
    { value: 'UNKNOWN', label: '不确定' },
  ],
  urinationStatus: [
    { value: 'NORMAL', label: '正常' },
    { value: 'INCREASED', label: '比平时多' },
    { value: 'REDUCED', label: '比平时少' },
    { value: 'ABNORMAL', label: '有异常' },
    { value: 'UNKNOWN', label: '不确定' },
  ],
} as const

const labels = {
  appetite: Object.fromEntries(healthStatusOptions.appetite.map(({ value, label }) => [value, label])) as Record<AppetiteLevel, string>,
  waterIntake: Object.fromEntries(healthStatusOptions.waterIntake.map(({ value, label }) => [value, label])) as Record<WaterIntake, string>,
  energyLevel: Object.fromEntries(healthStatusOptions.energyLevel.map(({ value, label }) => [value, label])) as Record<EnergyLevel, string>,
  activityLevel: Object.fromEntries(healthStatusOptions.activityLevel.map(({ value, label }) => [value, label])) as Record<ActivityLevel, string>,
  sleepStatus: Object.fromEntries(healthStatusOptions.sleepStatus.map(({ value, label }) => [value, label])) as Record<SleepStatus, string>,
  stoolStatus: Object.fromEntries(healthStatusOptions.stoolStatus.map(({ value, label }) => [value, label])) as Record<StoolStatus, string>,
  urinationStatus: Object.fromEntries(healthStatusOptions.urinationStatus.map(({ value, label }) => [value, label])) as Record<UrinationStatus, string>,
}

export type HealthStatusField = keyof typeof labels

export function healthStatusLabel(field: 'appetite', value: AppetiteLevel): string
export function healthStatusLabel(field: 'waterIntake', value: WaterIntake): string
export function healthStatusLabel(field: 'energyLevel', value: EnergyLevel): string
export function healthStatusLabel(field: 'activityLevel', value: ActivityLevel): string
export function healthStatusLabel(field: 'sleepStatus', value: SleepStatus): string
export function healthStatusLabel(field: 'stoolStatus', value: StoolStatus): string
export function healthStatusLabel(field: 'urinationStatus', value: UrinationStatus): string
export function healthStatusLabel(field: HealthStatusField, value: string): string {
  return (labels[field] as Record<string, string>)[value] ?? '未填写'
}
