type Props = {
  className?: string;
};

function ClockLogo({ className }: Props) {
  return (
    <svg viewBox="0 0 512 512" className={className} aria-hidden="true">
      <rect width="512" height="512" rx="100" fill="#ea580c" />

      <rect
        x="138"
        y="60"
        width="24"
        height="90"
        rx="12"
        fill="#ffffff"
        transform="rotate(-25 150 105)"
      />
      <rect
        x="350"
        y="60"
        width="24"
        height="90"
        rx="12"
        fill="#ffffff"
        transform="rotate(25 362 105)"
      />

      <circle cx="256" cy="280" r="160" fill="#ffffff" />
      <circle cx="256" cy="280" r="140" fill="#ea580c" />
      <circle cx="256" cy="280" r="132" fill="#ffffff" />

      <rect
        x="249"
        y="200"
        width="14"
        height="80"
        rx="7"
        fill="#ea580c"
        transform="rotate(-60 256 280)"
      />
      <rect
        x="250"
        y="160"
        width="12"
        height="120"
        rx="6"
        fill="#ea580c"
        transform="rotate(60 256 280)"
      />
      <circle cx="256" cy="280" r="14" fill="#ea580c" />
    </svg>
  );
}

export default ClockLogo;
