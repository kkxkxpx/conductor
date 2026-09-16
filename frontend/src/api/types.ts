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
