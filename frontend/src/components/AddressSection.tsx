import { useState, type FormEvent } from 'react'
import type { CustomerProfile, ProfileUpdate } from '../api/types'

interface AddressSectionProps {
  profile: CustomerProfile
  onSave: (update: ProfileUpdate) => Promise<void>
}

export function AddressSection({ profile, onSave }: AddressSectionProps) {
  const [addressLine1, setAddressLine1] = useState(profile.addressLine1)
  const [addressLine2, setAddressLine2] = useState(profile.addressLine2)
  const [subDistrict, setSubDistrict] = useState(profile.subDistrict)
  const [district, setDistrict] = useState(profile.district)
  const [province, setProvince] = useState(profile.province)
  const [postalCode, setPostalCode] = useState(profile.postalCode)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSaving(true)
    setError(null)
    setSaved(false)
    try {
      await onSave({
        addressLine1,
        addressLine2,
        subDistrict,
        district,
        province,
        postalCode,
      })
      setSaved(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save address')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section aria-label="Address">
      <h2>Address</h2>
      <form onSubmit={handleSubmit}>
        <label htmlFor="addressLine1">Address No.</label>
        <input
          id="addressLine1"
          value={addressLine1}
          onChange={(event) => setAddressLine1(event.target.value)}
        />

        <label htmlFor="addressLine2">Street</label>
        <input
          id="addressLine2"
          value={addressLine2}
          onChange={(event) => setAddressLine2(event.target.value)}
        />

        <label htmlFor="subDistrict">Sub-district</label>
        <input
          id="subDistrict"
          value={subDistrict}
          onChange={(event) => setSubDistrict(event.target.value)}
        />

        <label htmlFor="district">District</label>
        <input
          id="district"
          value={district}
          onChange={(event) => setDistrict(event.target.value)}
        />

        <label htmlFor="province">Province</label>
        <input
          id="province"
          value={province}
          onChange={(event) => setProvince(event.target.value)}
        />

        <label htmlFor="postalCode">Postal code</label>
        <input
          id="postalCode"
          value={postalCode}
          onChange={(event) => setPostalCode(event.target.value)}
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
