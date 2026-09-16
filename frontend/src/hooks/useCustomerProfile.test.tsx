import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useCustomerProfile } from './useCustomerProfile'
import type { CustomerProfile } from '../api/types'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const initialProfile: CustomerProfile = {
  phone: '0812345678',
  addressLine1: '123 Moo 4',
  addressLine2: 'Soi 5',
  subDistrict: 'Bang Rak',
  district: 'Bang Rak',
  province: 'Bangkok',
  postalCode: '10500',
  version: 'v1',
}

describe('useCustomerProfile', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads the profile from the service on mount', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(initialProfile)))

    const { result } = renderHook(() => useCustomerProfile())

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.profile).toEqual(initialProfile)
  })

  it('saves using the version from the last loaded profile', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(initialProfile)))
    const { result } = renderHook(() => useCustomerProfile())
    await waitFor(() => expect(result.current.loading).toBe(false))

    const patchResponse = { ...initialProfile, phone: '0899999999', version: 'v2' }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(patchResponse)))

    await act(async () => {
      await result.current.save({ phone: '0899999999' })
    })

    const fetchMock = fetch as unknown as ReturnType<typeof vi.fn>
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    const headers = init.headers as Record<string, string>
    expect(headers['If-Match']).toBe('v1')
  })

  it('adopts the version returned by a save for the very next save', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(initialProfile)))
    const { result } = renderHook(() => useCustomerProfile())
    await waitFor(() => expect(result.current.loading).toBe(false))

    const afterContactSave = { ...initialProfile, phone: '0899999999', version: 'v2' }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(afterContactSave)))
    await act(async () => {
      await result.current.save({ phone: '0899999999' })
    })

    const secondFetch = vi
      .fn()
      .mockResolvedValue(jsonResponse({ ...afterContactSave, addressLine1: '999 Moo 9', version: 'v3' }))
    vi.stubGlobal('fetch', secondFetch)
    await act(async () => {
      await result.current.save({ addressLine1: '999 Moo 9' })
    })

    const [, init] = secondFetch.mock.calls[0] as [string, RequestInit]
    const headers = init.headers as Record<string, string>
    expect(headers['If-Match']).toBe('v2')
  })

  it('exposes the load error message when the initial fetch fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse({ message: 'boom' }, 502)),
    )

    const { result } = renderHook(() => useCustomerProfile())

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.loadError).toBeTruthy()
    expect(result.current.profile).toBeNull()
  })
})
