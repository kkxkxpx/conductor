export interface CustomerProfile {
  phone: string
  addressLine1: string
  addressLine2: string
  subDistrict: string
  district: string
  province: string
  postalCode: string
  version: string
}

export type ProfileUpdate = Partial<Omit<CustomerProfile, 'version'>>

/** R-3 / R-14: one entry per offending field in a 422 response. */
export interface FieldValidationError {
  field: string
  message: string
}
