export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/**
 * No auth/session layer exists yet, so there is no way to resolve the
 * logged-in customer's id. Hardcoded until that lands.
 */
export const CUSTOMER_ID = import.meta.env.VITE_CUSTOMER_ID ?? 'cust-1'
