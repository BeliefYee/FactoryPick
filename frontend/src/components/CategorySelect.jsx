import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'

export default function CategorySelect({ value, categories, onChange }) {
  const options = ['', ...(categories || [])]
  const id = useId(), trigger = useRef(null), list = useRef(null)
  const [open, setOpen] = useState(false), [active, setActive] = useState(0)
  const close = () => { setOpen(false); trigger.current?.focus() }
  const show = () => { setActive(Math.max(0, options.indexOf(value))); setOpen(true) }
  const choose = index => { onChange(options[index]); close() }

  useLayoutEffect(() => {
    if (!open) return
    const position = () => {
      const rect = trigger.current.getBoundingClientRect(), viewport = window.visualViewport
      const topEdge = (viewport?.offsetTop || 0) + 8
      const leftEdge = (viewport?.offsetLeft || 0) + 8
      const bottomEdge = topEdge + (viewport?.height || window.innerHeight) - 16
      const rightEdge = leftEdge + (viewport?.width || window.innerWidth) - 16
      const below = Math.max(0, bottomEdge - rect.bottom - 6)
      const above = Math.max(0, rect.top - topEdge - 6)
      const upward = below < 260 && above > below
      const height = Math.min(300, upward ? above : below)
      const width = Math.min(Math.max(rect.width, 200), rightEdge - leftEdge)
      Object.assign(list.current.style, {
        width: `${width}px`, maxHeight: `${height}px`,
        left: `${Math.max(leftEdge, Math.min(rect.left, rightEdge - width))}px`,
        top: upward ? 'auto' : `${Math.max(topEdge, Math.min(rect.bottom + 6, bottomEdge))}px`,
        bottom: upward ? `${window.innerHeight - Math.min(rect.top - 6, bottomEdge)}px` : 'auto',
      })
    }
    position()
    list.current.focus({ preventScroll: true })
    const observer = new ResizeObserver(position)
    observer.observe(trigger.current)
    window.addEventListener('resize', position)
    window.addEventListener('scroll', position, true)
    window.visualViewport?.addEventListener('resize', position)
    window.visualViewport?.addEventListener('scroll', position)
    const outside = event => {
      if (!trigger.current.contains(event.target) && !list.current.contains(event.target)) setOpen(false)
    }
    document.addEventListener('pointerdown', outside)
    return () => {
      observer.disconnect()
      window.removeEventListener('resize', position)
      window.removeEventListener('scroll', position, true)
      window.visualViewport?.removeEventListener('resize', position)
      window.visualViewport?.removeEventListener('scroll', position)
      document.removeEventListener('pointerdown', outside)
    }
  }, [open])

  useEffect(() => {
    if (!open) return
    const option = list.current.children[active]
    if (!option) return
    const bottom = option.offsetTop + option.offsetHeight
    if (option.offsetTop < list.current.scrollTop) list.current.scrollTop = option.offsetTop
    else if (bottom > list.current.scrollTop + list.current.clientHeight) list.current.scrollTop = bottom - list.current.clientHeight
  }, [open, active])

  const keyboard = event => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      setActive(index => Math.max(0, Math.min(options.length - 1, index + (event.key === 'ArrowDown' ? 1 : -1))))
    } else if (event.key === 'Home' || event.key === 'End') {
      event.preventDefault(); setActive(event.key === 'Home' ? 0 : options.length - 1)
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault(); choose(active)
    } else if (event.key === 'Escape') {
      event.preventDefault(); close()
    } else if (event.key === 'Tab') close()
  }
  return <>
    <button ref={trigger} type="button" className="category-select" aria-label="업종 카테고리"
      aria-haspopup="listbox" aria-expanded={open} aria-controls={open ? id : undefined}
      onClick={() => open ? close() : show()}
      onKeyDown={event => { if (['ArrowDown','ArrowUp'].includes(event.key)) { event.preventDefault(); show() } }}>
      <span>{value || '전체 카테고리'}</span><span aria-hidden="true">⌄</span>
    </button>
    {open && createPortal(<div ref={list} id={id} className="category-options" role="listbox"
      tabIndex={-1} aria-label="업종 카테고리" aria-activedescendant={`${id}-${active}`} onKeyDown={keyboard}>
      {options.map((option, index) => <div key={option} id={`${id}-${index}`} role="option"
        aria-selected={option === value} className={active === index ? 'highlighted' : ''}
        onMouseMove={() => setActive(index)} onClick={() => choose(index)}>
        <span>{option || '전체 카테고리'}</span>{option === value && <span aria-hidden="true">✓</span>}
      </div>)}
    </div>, document.body)}
  </>
}
