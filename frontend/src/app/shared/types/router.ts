export interface RouteMeta {
  title?: string
  layout?: string
  requiresAuth?: boolean
}

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    layout?: string
    requiresAuth?: boolean
  }
}
