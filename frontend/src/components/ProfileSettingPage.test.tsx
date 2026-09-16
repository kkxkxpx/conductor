import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ProfileSettingPage } from './ProfileSettingPage'
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

describe('ProfileSettingPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('carries the version from the Contact save into the following Address save', async () => {
    const afterContactSave = { ...initialProfile, phone: '0899999999', version: 'v2' }
    const afterAddressSave = { ...afterContactSave, addressLine1: '999 Moo 9', version: 'v3' }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(initialProfile))
      .mockResolvedValueOnce(jsonResponse(afterContactSave))
      .mockResolvedValueOnce(jsonResponse(afterAddressSave))
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(<ProfileSettingPage />)
    await waitFor(() => expect(screen.getByText('Profile Setting')).toBeInTheDocument())

    const contactSection = within(screen.getByRole('region', { name: 'Contact' }))
    await user.clear(contactSection.getByLabelText('Phone number'))
    await user.type(contactSection.getByLabelText('Phone number'), '0899999999')
    await user.click(contactSection.getByRole('button', { name: /save/i }))
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))

    const addressSection = within(screen.getByRole('region', { name: 'Address' }))
    await user.clear(addressSection.getByLabelText('Address No.'))
    await user.type(addressSection.getByLabelText('Address No.'), '999 Moo 9')
    await user.click(addressSection.getByRole('button', { name: /save/i }))
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3))

    const addressSaveInit = fetchMock.mock.calls[2][1] as RequestInit
    const headers = addressSaveInit.headers as Record<string, string>
    expect(headers['If-Match']).toBe('v2')
  })

  it('re-fetches the profile from the service on reload instead of keeping the saved value in memory', async () => {
    const afterContactSave = { ...initialProfile, phone: '0899999999', version: 'v2' }
    const firstMountFetch = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(initialProfile))
      .mockResolvedValueOnce(jsonResponse(afterContactSave))
    vi.stubGlobal('fetch', firstMountFetch)
    const user = userEvent.setup()

    const { unmount } = render(<ProfileSettingPage />)
    await waitFor(() => expect(screen.getByText('Profile Setting')).toBeInTheDocument())
    const contactSection = within(screen.getByRole('region', { name: 'Contact' }))
    await user.clear(contactSection.getByLabelText('Phone number'))
    await user.type(contactSection.getByLabelText('Phone number'), '0899999999')
    await user.click(contactSection.getByRole('button', { name: /save/i }))
    await waitFor(() => expect(firstMountFetch).toHaveBeenCalledTimes(2))
    unmount()

    const reloadFetch = vi.fn().mockResolvedValueOnce(jsonResponse(afterContactSave))
    vi.stubGlobal('fetch', reloadFetch)
    render(<ProfileSettingPage />)

    await waitFor(() =>
      expect(within(screen.getByRole('region', { name: 'Contact' })).getByLabelText('Phone number'))
        .toHaveValue('0899999999'),
    )
    expect(reloadFetch).toHaveBeenCalledTimes(1)
    const [, init] = reloadFetch.mock.calls[0] as [string, RequestInit | undefined]
    expect(init?.method ?? 'GET').toBe('GET')
  })
})
