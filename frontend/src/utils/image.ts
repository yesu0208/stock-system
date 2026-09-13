const DEFAULT_AVATAR = "https://api.dicebear.com/7.x/thumbs/svg?seed=default";

const API_ORIGIN = import.meta.env.VITE_API_BASE_URL?.replace(/\/api.*$/, '') ?? '';

export function resolveProfileImageUrl(url: string | null | undefined): string {
    if (!url) return DEFAULT_AVATAR;
    if (url.startsWith('http')) return url;
    return `${API_ORIGIN}${url}`;
}

export { DEFAULT_AVATAR };
