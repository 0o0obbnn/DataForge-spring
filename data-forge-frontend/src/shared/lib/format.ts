export function formatNumber(value?: number) {
  return value === undefined ? "-" : new Intl.NumberFormat("en-US").format(value);
}

export function formatDuration(value?: number) {
  if (value === undefined || value === 0) {
    return "-";
  }

  return value < 1000 ? `${Math.round(value)} ms` : `${(value / 1000).toFixed(1)} s`;
}

export function formatDateTime(value?: string) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
