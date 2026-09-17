import { useCallback, useEffect, useState } from 'react'
import { fetchProfile, ProfileConflictError, updateProfile } from '../api/profileApi'
import type { CustomerProfile, ProfileUpdate } from '../api/types'
import { CUSTOMER_ID } from '../config'

interface UseCustomerProfileResult {
  profile: CustomerProfile | null
  loading: boolean
  loadError: string | null
  /** R-15: the current profile from a 409, or null when there is no unresolved conflict. */
  conflict: CustomerProfile | null
  save: (update: ProfileUpdate) => Promise<void>
  /** Adopts the conflicting profile as the new baseline without a full page reload. */
  reloadAfterConflict: () => void
}

/**
 * Single source of truth for the profile + its version. Both Profile Setting
 * sections share this hook's `save`, which always closes over the version
 * from whichever save (Contact or Address) resolved last - a save can never
 * self-conflict against a version the customer never saw change.
 */
export function useCustomerProfile(): UseCustomerProfileResult {
  const [profile, setProfile] = useState<CustomerProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [conflict, setConflict] = useState<CustomerProfile | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setLoadError(null)
    fetchProfile(CUSTOMER_ID)
      .then((result) => {
        if (!cancelled) setProfile(result)
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setLoadError(error instanceof Error ? error.message : 'Failed to load profile')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const save = useCallback(
    async (update: ProfileUpdate) => {
      if (!profile) {
        throw new Error('Profile has not loaded yet')
      }
      try {
        const result = await updateProfile(CUSTOMER_ID, profile.version, update)
        setProfile(result)
        setConflict(null)
      } catch (error) {
        if (error instanceof ProfileConflictError) {
          setConflict(error.currentProfile)
        }
        throw error
      }
    },
    [profile],
  )

  const reloadAfterConflict = useCallback(() => {
    if (conflict) {
      setProfile(conflict)
      setConflict(null)
    }
  }, [conflict])

  return { profile, loading, loadError, conflict, save, reloadAfterConflict }
}
