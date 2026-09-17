import { API_BASE_URL } from '../config'
import type { CustomerProfile, FieldValidationError, ProfileUpdate } from './types'

export class ProfileApiError extends Error {
  readonly status: number
  readonly fieldErrors: FieldValidationError[] | null

  constructor(message: string, status: number, fieldErrors: FieldValidationError[] | null = null) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

function asFieldValidationErrors(value: unknown): FieldValidationError[] | null {
  if (!Array.isArray(value) || value.length === 0) {
    return null
  }
  const isValid = value.every(
    (item): item is FieldValidationError =>
      typeof item === 'object' &&
      item !== null &&
      typeof (item as FieldValidationError).field === 'string' &&
      typeof (item as FieldValidationError).message === 'string',
  )
  return isValid ? (value as FieldValidationError[]) : null
}

async function throwForResponse(response: Response): Promise<never> {
  const body: unknown = await response.json().catch(() => null)

  if (response.status === 422 && body && typeof body === 'object' && 'errors' in body) {
    const fieldErrors = asFieldValidationErrors((body as { errors: unknown }).errors)
    if (fieldErrors) {
      throw new ProfileApiError(fieldErrors.map((fieldError) => fieldError.message).join(' '), response.status, fieldErrors)
    }
  }

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
