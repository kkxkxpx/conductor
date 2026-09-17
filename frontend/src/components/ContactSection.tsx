import { useState, type FormEvent } from 'react'
import { ProfileConflictError } from '../api/profileApi'
import type { CustomerProfile, ProfileUpdate } from '../api/types'

interface ContactSectionProps {
  profile: CustomerProfile
  onSave: (update: ProfileUpdate) => Promise<void>
}

export function ContactSection({ profile, onSave }: ContactSectionProps) {
  const [phone, setPhone] = useState(profile.phone)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError(null)
    setSaved(false)
    try {
      await onSave({ phone })
      setSaved(true)
    } catch (err) {
      // R-15: the page-level banner already reports conflicts; this typed
      // value must stay on screen rather than being replaced by an error.
      if (!(err instanceof ProfileConflictError)) {
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
        />
        <button type="submit" disabled={saving}>
          {saving ? 'Saving…' : 'Save'}
        </button>
      </form>
      {error && <p role="alert">{error}</p>}
      {saved && !error && <p role="status">Saved.</p>}
    </section>
  )
}
