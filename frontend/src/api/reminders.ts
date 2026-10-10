import { http, type ApiResponse } from './http'

export type ReminderType = 'VACCINATION' | 'DEWORMING' | 'MEDICAL_REVISIT'

export interface UpcomingReminder {
  petId: number
  petName: string
  type: ReminderType
  title: string
  dueDate: string
  daysRemaining: number
}

export async function getUpcomingReminders(): Promise<UpcomingReminder[]> {
  const response = await http.get<ApiResponse<UpcomingReminder[]>>('/reminders/upcoming')
  return response.data.data
}
