import React, { useEffect, useRef, useState } from 'react';

function normalize(options) {
  return (options || []).map((o) => (typeof o === 'string' ? { value: o, label: o } : o));
}

export default function PrettySelect({ options = [], value, onChange, placeholder = 'Select...', className = '', id }) {
  const opts = normalize(options);
  const containerRef = useRef(null);
  const listRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [highlight, setHighlight] = useState(0);

  const selected = opts.find((o) => String(o.value) === String(value));

  useEffect(() => {
    if (open) {
      const selectedIndex = Math.max(
        0,
        opts.findIndex((o) => String(o.value) === String(value))
      );
      setHighlight(selectedIndex >= 0 ? selectedIndex : 0);
    }
  }, [open, value, opts]);

  useEffect(() => {
    if (open && listRef.current) {
      const el = listRef.current.querySelector('[data-highlight]');
      if (el) el.scrollIntoView({ block: 'nearest' });
    }
  }, [open, highlight]);

  useEffect(() => {
    const onDoc = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  const toggle = () => {
    setOpen((v) => !v);
  };

  const handleKey = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setOpen(true);
      setHighlight((h) => Math.min(h + 1, opts.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setOpen(true);
      setHighlight((h) => Math.max(h - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const pick = opts[highlight];
      if (pick) {
        onChange && onChange(pick.value);
      }
      setOpen(false);
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  };

  const onSelect = (opt, idx) => {
    onChange && onChange(opt.value);
    setHighlight(idx);
    setOpen(false);
  };

  return (
    <div ref={containerRef} className={`relative ${className}`} id={id}>
      <button
        type="button"
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={toggle}
        onKeyDown={handleKey}
        className="input-field select-field flex items-center justify-between gap-3 text-left shadow-glow"
      >
        <span className="min-w-0 flex-1 truncate">
          <span className={`${selected ? 'text-white' : 'text-slate-400'}`}>{selected ? selected.label : placeholder}</span>
        </span>
        <svg className={`h-4 w-4 shrink-0 text-white transition-transform ${open ? 'rotate-180' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>

      <div
        ref={listRef}
        role="listbox"
        tabIndex={-1}
        className={`absolute left-0 right-0 z-50 mt-2 max-h-64 w-full overflow-auto rounded-[1.5rem] border border-white/10 bg-[#071425]/95 p-2 shadow-[0_24px_70px_rgba(0,0,0,0.45)] backdrop-blur-2xl transition-transform duration-200 transform origin-top ${open ? 'opacity-100 scale-100' : 'pointer-events-none opacity-0 scale-95'}`}
      >
        {opts.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-white/10 px-4 py-4 text-sm text-slate-400">
            No options available.
          </div>
        ) : null}
        {opts.map((opt, idx) => {
          const isHighlighted = idx === highlight;
          return (
            <div
              key={`${opt.value}-${idx}`}
              role="option"
              aria-selected={String(opt.value) === String(value)}
              data-highlight={isHighlighted ? '1' : undefined}
              onMouseEnter={() => setHighlight(idx)}
              onClick={() => onSelect(opt, idx)}
              className={`block cursor-pointer rounded-2xl px-4 py-3 text-sm transition ${isHighlighted ? 'bg-accent/18 text-white ring-1 ring-accent/20' : 'text-slate-200 hover:bg-white/5 hover:text-white'}`}
            >
              <div className="font-semibold">{opt.label}</div>
              {opt.description ? <div className="mt-1 text-xs leading-5 text-slate-400">{opt.description}</div> : null}
            </div>
          );
        })}
      </div>
    </div>
  );
}
