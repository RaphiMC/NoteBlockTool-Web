const numberFormat = new Intl.NumberFormat();
const progressFormat = new Intl.NumberFormat(undefined, {minimumFractionDigits: 1, maximumFractionDigits: 1});

export function formatNumber(value: number): string {
    return numberFormat.format(value);
}

export function formatProgress(progress: number): string {
    return `${progressFormat.format(progress)}%`;
}
