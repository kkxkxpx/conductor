import { useState, type FormEvent } from 'react'
import { ProfileApiError } from '../api/profileApi'
import type { CustomerProfile, ProfileUpdate } from '../api/types'

interface ContactSectionProps {
  profile: CustomerProfile
  onSave: (update: ProfileUpdate) => Promise<void>
}

export function ContactSection({ profile, onSave }: ContactSectionProps) {
  const [phone, setPhone] = useState(profile.phone)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [phoneError, setPhoneError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError(null)
    setPhoneError(null)
    setSaved(false)
    try {
      await onSave({ phone })
      setSaved(true)
    } catch (err) {
      const fieldError = err instanceof ProfileApiError
        ? err.fieldErrors?.find((fieldValidationError) => fieldValidationError.field === 'phone')
        : undefined
      if (fieldError) {
        setPhoneError(fieldError.message)
      } else {
        setError(err instanceof Error ? err.message : 'Failed to save contact details')
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <section aria-label="Contact">
      <h2>Contact</h2>
      <form onSubmit={handleSubmit}>
        <label htmlFor="phone">Phone number</label>
        <input
          id="phone"
          name="phone"
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
          aria-invalid={phoneError ? true : undefined}
          aria-describedby={phoneError ? 'phone-error' : undefined}
        />
        {phoneError && (
          <p role="alert" id="phone-error">
            {phoneError}
          </p>
        )}
        <button type="submit" disabled={saving}>
          {saving ? 'Saving…' : 'Save'}
        </button>
      </form>
      {error && <p role="alert">{error}</p>}
      {saved && !error && <p role="status">Saved.</p>}
    </section>
  )
}
