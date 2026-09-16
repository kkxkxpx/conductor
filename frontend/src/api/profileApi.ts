import { API_BASE_URL } from '../config'
import type { CustomerProfile, ProfileUpdate } from './types'

export class ProfileApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

async function throwForResponse(response: Response): Promise<never> {
  const body: unknown = await response.json().catch(() => null)
  const message =
    body && typeof body === 'object' && 'message' in body && typeof body.message === 'string'
      ? body.message
      : `Request failed with status ${response.status}`
  throw new ProfileApiError(message, response.status)
}

export async function fetchProfile(customerId: string): Promise<CustomerProfile> {
  const response = await fetch(`${API_BASE_URL}/v1/customers/${customerId}/profile`)
  if (!response.ok) {
    await throwForResponse(response)
  }
  return response.json() as Promise<CustomerProfile>
}

export async function updateProfile(
  customerId: string,
  version: string,
  update: ProfileUpdate,
): Promise<CustomerProfile> {
  const response = await fetch(`${API_BASE_URL}/v1/customers/${customerId}/profile`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'If-Match': version,
    },
    body: JSON.stringify(update),
  })
  if (!response.ok) {
    await throwForResponse(response)
  }
  return response.json() as Promise<CustomerProfile>
}
