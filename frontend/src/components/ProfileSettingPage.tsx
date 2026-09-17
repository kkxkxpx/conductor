import { useCustomerProfile } from '../hooks/useCustomerProfile'
import { AddressSection } from './AddressSection'
import { ContactSection } from './ContactSection'

export function ProfileSettingPage() {
  const { profile, loading, loadError, conflict, save, reloadAfterConflict } = useCustomerProfile()

  if (loading) {
    return <p>Loading profile…</p>
  }

  if (loadError || !profile) {
    return <p role="alert">{loadError ?? 'Profile is unavailable.'}</p>
  }

  return (
    <main>
      <h1>Profile Setting</h1>
      {conflict && (
        <div role="alert">
          <p>This profile changed while you were editing</p>
          <button type="button" onClick={reloadAfterConflict}>
            Reload
          </button>
        </div>
      )}
      <ContactSection profile={profile} onSave={save} />
      <AddressSection profile={profile} onSave={save} />
    </main>
  )
}
