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

  it('acceptance: contact save then address save then reload shows both new values from the service, with neither save rejected', async () => {
    const afterContactSave = { ...initialProfile, phone: '0899999999', version: 'v2' }
    const afterAddressSave = { ...afterContactSave, addressLine1: '999 Moo 9', version: 'v3' }
    const firstVisitFetch = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(initialProfile))
      .mockResolvedValueOnce(jsonResponse(afterContactSave))
      .mockResolvedValueOnce(jsonResponse(afterAddressSave))
    vi.stubGlobal('fetch', firstVisitFetch)
    const user = userEvent.setup()

    const { unmount } = render(<ProfileSettingPage />)
    await waitFor(() => expect(screen.getByText('Profile Setting')).toBeInTheDocument())

    const contactSection = within(screen.getByRole('region', { name: 'Contact' }))
    await user.clear(contactSection.getByLabelText('Phone number'))
    await user.type(contactSection.getByLabelText('Phone number'), '0899999999')
    await user.click(contactSection.getByRole('button', { name: /save/i }))
    await waitFor(() => expect(contactSection.getByRole('status')).toHaveTextContent('Saved.'))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()

    const addressSection = within(screen.getByRole('region', { name: 'Address' }))
    await user.clear(addressSection.getByLabelText('Address No.'))
    await user.type(addressSection.getByLabelText('Address No.'), '999 Moo 9')
    await user.click(addressSection.getByRole('button', { name: /save/i }))
    await waitFor(() => expect(addressSection.getByRole('status')).toHaveTextContent('Saved.'))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()

    const addressSaveInit = firstVisitFetch.mock.calls[2][1] as RequestInit
    expect((addressSaveInit.headers as Record<string, string>)['If-Match']).toBe('v2')
    unmount()

    const reloadFetch = vi.fn().mockResolvedValueOnce(jsonResponse(afterAddressSave))
    vi.stubGlobal('fetch', reloadFetch)

    render(<ProfileSettingPage />)
    await waitFor(() =>
      expect(within(screen.getByRole('region', { name: 'Contact' })).getByLabelText('Phone number'))
        .toHaveValue('0899999999'),
    )
    expect(
      within(screen.getByRole('region', { name: 'Address' })).getByLabelText('Address No.'),
    ).toHaveValue('999 Moo 9')

    expect(reloadFetch).toHaveBeenCalledTimes(1)
    const [, reloadInit] = reloadFetch.mock.calls[0] as [string, RequestInit | undefined]
    expect(reloadInit?.method ?? 'GET').toBe('GET')
  })

  it('shows "This profile changed while you were editing" with a reload button when a save returns 409', async () => {
    const conflictProfile = { ...initialProfile, phone: '0899999999', version: 'v-current789' }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(initialProfile))
      .mockResolvedValueOnce(jsonResponse(conflictProfile, 409))
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(<ProfileSettingPage />)
    await waitFor(() => expect(screen.getByText('Profile Setting')).toBeInTheDocument())

    const contactSection = within(screen.getByRole('region', { name: 'Contact' }))
    await user.clear(contactSection.getByLabelText('Phone number'))
    await user.type(contactSection.getByLabelText('Phone number'), '0888888888')
    await user.click(contactSection.getByRole('button', { name: /save/i }))

    const banner = await screen.findByRole('alert')
    expect(banner).toHaveTextContent('This profile changed while you were editing')
    expect(within(banner).getByRole('button', { name: /reload/i })).toBeInTheDocument()
  })

  it('clicking reload after a conflict dismisses the banner', async () => {
    const conflictProfile = { ...initialProfile, phone: '0899999999', version: 'v-current789' }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(initialProfile))
      .mockResolvedValueOnce(jsonResponse(conflictProfile, 409))
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(<ProfileSettingPage />)
    await waitFor(() => expect(screen.getByText('Profile Setting')).toBeInTheDocument())

    const contactSection = within(screen.getByRole('region', { name: 'Contact' }))
    await user.clear(contactSection.getByLabelText('Phone number'))
    await user.type(contactSection.getByLabelText('Phone number'), '0888888888')
    await user.click(contactSection.getByRole('button', { name: /save/i }))

    const banner = await screen.findByRole('alert')
    await user.click(within(banner).getByRole('button', { name: /reload/i }))

    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument())
  })

  it('adopts the conflicting profile as the If-Match baseline for the next save after reload', async () => {
    const conflictProfile = { ...initialProfile, phone: '0899999999', version: 'v-current789' }
    const afterRetrySave = { ...conflictProfile, addressLine1: '999 Moo 9', version: 'v-next' }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(initialProfile))
      .mockResolvedValueOnce(jsonResponse(conflictProfile, 409))
      .mockResolvedValueOnce(jsonResponse(afterRetrySave))
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(<ProfileSettingPage />)
    await waitFor(() => expect(screen.getByText('Profile Setting')).toBeInTheDocument())

    const contactSection = within(screen.getByRole('region', { name: 'Contact' }))
    await user.clear(contactSection.getByLabelText('Phone number'))
    await user.type(contactSection.getByLabelText('Phone number'), '0888888888')
    await user.click(contactSection.getByRole('button', { name: /save/i }))

    const banner = await screen.findByRole('alert')
    await user.click(within(banner).getByRole('button', { name: /reload/i }))
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument())

    const addressSection = within(screen.getByRole('region', { name: 'Address' }))
    await user.clear(addressSection.getByLabelText('Address No.'))
    await user.type(addressSection.getByLabelText('Address No.'), '999 Moo 9')
    await user.click(addressSection.getByRole('button', { name: /save/i }))
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3))

    const retryInit = fetchMock.mock.calls[2][1] as RequestInit
    expect((retryInit.headers as Record<string, string>)['If-Match']).toBe('v-current789')
  })
})
