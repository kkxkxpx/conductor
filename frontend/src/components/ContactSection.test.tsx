import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ContactSection } from './ContactSection'
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

describe('ContactSection', () => {
  it('saves only the phone field when the form is submitted', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined)
    render(<ContactSection profile={profile} onSave={onSave} />)
    const user = userEvent.setup()

    await user.clear(screen.getByLabelText('Phone number'))
    await user.type(screen.getByLabelText('Phone number'), '0899999999')
    await user.click(screen.getByRole('button', { name: /save/i }))

    expect(onSave).toHaveBeenCalledWith({ phone: '0899999999' })
  })

  it('shows a saved confirmation once the save resolves', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined)
    render(<ContactSection profile={profile} onSave={onSave} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /save/i }))

    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Saved.'))
  })

  it('shows an error message instead of a confirmation when the save is rejected', async () => {
    const onSave = vi.fn().mockRejectedValue(new Error('version conflict'))
    render(<ContactSection profile={profile} onSave={onSave} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /save/i }))

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('version conflict'))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('does not show its own error banner when the save is rejected with a ProfileConflictError', async () => {
    const conflictProfile = { ...profile, phone: '0899999999', version: 'v2' }
    const onSave = vi.fn().mockRejectedValue(new ProfileConflictError(conflictProfile))
    render(<ContactSection profile={profile} onSave={onSave} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /save/i }))

    await waitFor(() => expect(onSave).toHaveBeenCalled())
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('keeps the typed phone value on screen when the save is rejected with a ProfileConflictError', async () => {
    const conflictProfile = { ...profile, phone: '0899999999', version: 'v2' }
    const onSave = vi.fn().mockRejectedValue(new ProfileConflictError(conflictProfile))
    render(<ContactSection profile={profile} onSave={onSave} />)
    const user = userEvent.setup()

    await user.clear(screen.getByLabelText('Phone number'))
    await user.type(screen.getByLabelText('Phone number'), '0877777777')
    await user.click(screen.getByRole('button', { name: /save/i }))

    await waitFor(() => expect(onSave).toHaveBeenCalled())
    expect(screen.getByLabelText('Phone number')).toHaveValue('0877777777')
  })
})
