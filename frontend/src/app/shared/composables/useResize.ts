import { ref } from 'vue'

export function useResize() {
  const isSidebarResizing = ref(false)
  const isTerminalResizing = ref(false)
  const terminalHeight = ref(200)

  const startSidebarResize = (e: MouseEvent) => {
    isSidebarResizing.value = true
    const handler = e.target as HTMLElement
    const wrapper = handler.closest('.wrapper') as HTMLElement
    const sidebar = wrapper.querySelector('.sidebar-box') as HTMLElement

    const handleMouseMove = (e: MouseEvent) => {
      if (!isSidebarResizing.value) return

      const containerOffsetLeft = wrapper.getBoundingClientRect().left
      const pointerRelativeXpos = e.clientX - containerOffsetLeft
      const sidebarMinWidth = 200
      const sidebarMaxWidth = 800

      const newWidth = Math.min(Math.max(sidebarMinWidth, pointerRelativeXpos - 8), sidebarMaxWidth)
      sidebar.style.width = newWidth + 'px'
      sidebar.style.flexGrow = '0'
    }

    const handleMouseUp = () => {
      isSidebarResizing.value = false
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  const startTerminalResize = (e: MouseEvent) => {
    isTerminalResizing.value = true
    const wrapper = document.querySelector('.wrapper') as HTMLElement

    const handleMouseMove = (e: MouseEvent) => {
      if (!isTerminalResizing.value) return

      const wrapperHeight = wrapper.getBoundingClientRect().height
      const pointerRelativeYpos = e.clientY
      const wrapperTop = wrapper.getBoundingClientRect().top

      const newHeight = Math.min(
        Math.max(100, wrapperHeight - (pointerRelativeYpos - wrapperTop)),
        wrapperHeight - 200,
      )

      requestAnimationFrame(() => {
        terminalHeight.value = newHeight
      })
    }

    const handleMouseUp = () => {
      isTerminalResizing.value = false
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  return {
    isSidebarResizing,
    isTerminalResizing,
    terminalHeight,
    startSidebarResize,
    startTerminalResize,
  }
}
