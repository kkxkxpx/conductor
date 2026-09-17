import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AddressSection } from './AddressSection'
import { ProfileConflictError } from '../api/profileApi'
import type { CustomerProfile } from '../api/types'

const profile: CustomerProfile = {
  phone: '0812345678',
  addressLine1: '123 Moo 4',
  addressLine2: 'Soi 5',
  subDistrict: 'Bang Rak',
  district: 'Bang Rak',
  province: 'Bangkok',
  postalCode: '10500',
  version: 'v1',
}

describe('AddressSection', () => {
  it('saves the edited address line without touching contact fields', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined)
    render(<AddressSection profile={profile} onSave={onSave} />)
    const user = userEvent.setup()

    await user.clear(screen.getByLabelText('Address No.'))
    await user.type(screen.getByLabelText('Address No.'), '999 Moo 9')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(onSave).toHaveBeenCalledWith({
      addressLine1: '999 Moo 9',
      addressLine2: 'Soi 5',
      subDistrict: 'Bang Rak',
      district: 'Bang Rak',
      province: 'Bangkok',
      postalCode: '10500',
    })
  })

  it('does not include phone in the saved update', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined)
    render(<AddressSection profile={profile} onSave={onSave} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /save/i }))

    const savedUpdate = onSave.mock.calls[0][0] as Record<string, unknown>
    expect(savedUpdate).not.toHaveProperty('phone')
  })

  it('does not show its own error banner when the save is rejected with a ProfileConflictError', async () => {
    const conflictProfile = { ...profile, addressLine1: '999 Moo 9', version: 'v2' }
    const onSave = vi.fn().mockRejectedValue(new ProfileConflictError(conflictProfile))
    render(<AddressSection profile={profile} onSave={onSave} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /save/i }))

    await waitFor(() => expect(onSave).toHaveBeenCalled())
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('keeps the typed address value on screen when the save is rejected with a ProfileConflictError', async () => {
    const conflictProfile = { ...profile, addressLine1: '999 Moo 9', version: 'v2' }
    const onSave = vi.fn().mockRejectedValue(new ProfileConflictError(conflictProfile))
    render(<AddressSection profile={profile} onSave={onSave} />)
    const user = userEvent.setup()

    await user.clear(screen.getByLabelText('Address No.'))
    await user.type(screen.getByLabelText('Address No.'), '555 Moo 5')
    await user.click(screen.getByRole('button', { name: /save/i }))

    await waitFor(() => expect(onSave).toHaveBeenCalled())
    expect(screen.getByLabelText('Address No.')).toHaveValue('555 Moo 5')
  })
})
