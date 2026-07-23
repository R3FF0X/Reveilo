import { useEffect, useRef, useState } from "react";

type Option<T extends string> = {
  value: T;
  label: string;
};

type Props<T extends string> = {
  value: T;
  options: Option<T>[];
  onChange: (value: T) => void;
};

function Select<T extends string>({ value, options, onChange }: Props<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const selected = options.find((option) => option.value === value);

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        className="w-full flex items-center justify-between bg-neutral-900 border border-neutral-700 rounded-lg px-3 py-2 text-white active:scale-[0.98] transition-transform duration-100 focus:outline-none focus:ring-2 focus:ring-white/30"
      >
        <span>{selected?.label}</span>
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className={`w-4 h-4 text-neutral-400 transition-transform duration-200 ${
            isOpen ? "rotate-180" : ""
          }`}
        >
          <path d="M6 9l6 6 6-6" />
        </svg>
      </button>

      <div
        className={`absolute left-0 right-0 mt-1.5 origin-top bg-neutral-900 border border-neutral-700 rounded-lg shadow-xl overflow-hidden z-10 transition-all duration-200 ease-out ${
          isOpen
            ? "opacity-100 scale-y-100"
            : "opacity-0 scale-y-0 pointer-events-none"
        }`}
      >
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => {
              onChange(option.value);
              setIsOpen(false);
            }}
            className={`w-full text-left px-3 py-2 text-sm transition-colors ${
              option.value === value
                ? "bg-orange-600 text-white"
                : "text-neutral-200 hover:bg-neutral-800"
            }`}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
}

export default Select;
