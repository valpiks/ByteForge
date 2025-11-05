export const formatDateToLocal = (date: string) => {
  const newDate = new Date(date)
  const localDate = newDate.toLocaleDateString()
  return localDate
}

export const getUpdatedTimeAgo = (date: string) => {
  const newDate = new Date(date)
  const now = new Date()
  const diffInMs = now.getTime() - newDate.getTime()
  const diffInSeconds = Math.floor(diffInMs / 1000)
  const diffInMinutes = Math.floor(diffInSeconds / 60)
  const diffInHours = Math.floor(diffInMinutes / 60)
  const diffInDays = Math.floor(diffInHours / 24)

  if (diffInDays === 0) {
    if (diffInHours === 0) {
      if (diffInMinutes === 0) {
        return 'just now'
      }
      return `${diffInMinutes} min ago`
    }
    return `${diffInHours} hours ago`
  }

  if (diffInDays === 1) {
    return `yesterday at ${newDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  }

  if (diffInDays < 7) {
    return `${diffInDays} days ago`
  }

  if (newDate.getFullYear() === now.getFullYear()) {
    return newDate.toLocaleDateString([], { month: 'short', day: 'numeric' })
  }

  return newDate.toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}
