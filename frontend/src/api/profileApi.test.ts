import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchProfile, ProfileApiError, ProfileConflictError, updateProfile } from './profileApi'
import type { CustomerProfile } from './types'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const sampleProfile: CustomerProfile = {
  phone: '0812345678',
  addressLine1: '123 Moo 4',
  addressLine2: 'Soi 5',
  subDistrict: 'Bang Rak',
  district: 'Bang Rak',
  province: 'Bangkok',
  postalCode: '10500',
  version: 'v-abc123',
}

describe('fetchProfile', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('issues a GET request to the customer profile endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(sampleProfile))
    vi.stubGlobal('fetch', fetchMock)

    await fetchProfile('cust-1')

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit | undefined]
    expect(url).toContain('/v1/customers/cust-1/profile')
    expect(init?.method ?? 'GET').toBe('GET')
  })

  it('throws a ProfileApiError carrying the response status when the request fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse({ message: 'not found' }, 404)),
    )

    await expect(fetchProfile('missing')).rejects.toMatchObject({
      status: 404,
    })
    await expect(fetchProfile('missing')).rejects.toBeInstanceOf(ProfileApiError)
  })
})

describe('updateProfile', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends a PATCH request carrying the version as the If-Match header', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(sampleProfile))
    vi.stubGlobal('fetch', fetchMock)

    await updateProfile('cust-1', 'v-abc123', { phone: '0899999999' })

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(init.method).toBe('PATCH')
    const headers = init.headers as Record<string, string>
    expect(headers['If-Match']).toBe('v-abc123')
  })

  it('sends only the fields passed in the update as the request body', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(sampleProfile))
    vi.stubGlobal('fetch', fetchMock)

    await updateProfile('cust-1', 'v-abc123', { phone: '0899999999' })

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    const body: unknown = JSON.parse(init.body as string)
    expect(body).toEqual({ phone: '0899999999' })
  })

  it('resolves with the updated profile returned by the service', async () => {
    const updated = { ...sampleProfile, phone: '0899999999', version: 'v-def456' }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(updated)))

    const result = await updateProfile('cust-1', 'v-abc123', { phone: '0899999999' })

    expect(result).toEqual(updated)
  })

  it('throws a ProfileApiError when the service rejects the version as stale with an error-shaped body', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse({ message: 'version conflict' }, 409)),
    )

    await expect(
      updateProfile('cust-1', 'stale-version', { phone: '0899999999' }),
    ).rejects.toMatchObject({ status: 409 })
  })

  it('throws a ProfileConflictError when the 409 body is the current profile', async () => {
    const currentProfile = { ...sampleProfile, phone: '0899999999', version: 'v-current789' }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(currentProfile, 409)))

    await expect(
      updateProfile('cust-1', 'stale-version', { phone: '0888888888' }),
    ).rejects.toBeInstanceOf(ProfileConflictError)
  })

  it('carries the current profile from the 409 body on the ProfileConflictError', async () => {
    const currentProfile = { ...sampleProfile, phone: '0899999999', version: 'v-current789' }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(currentProfile, 409)))

    const error = await updateProfile('cust-1', 'stale-version', { phone: '0888888888' }).catch(
      (caught: unknown) => caught,
    )

    expect((error as ProfileConflictError).currentProfile).toEqual(currentProfile)
  })

  it('uses the fixed R-15 conflict message on the ProfileConflictError', async () => {
    const currentProfile = { ...sampleProfile, phone: '0899999999', version: 'v-current789' }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(currentProfile, 409)))

    const error = await updateProfile('cust-1', 'stale-version', { phone: '0888888888' }).catch(
      (caught: unknown) => caught,
    )

    expect((error as Error).message).toBe('This profile changed while you were editing')
  })
})
