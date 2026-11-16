import {defineConfig, loadEnv} from "vite";
import {svelte} from "@sveltejs/vite-plugin-svelte";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig(({mode}) => {
    const env = loadEnv(mode, ".", "");
    return {
        plugins: [svelte(), tailwindcss()],
        base: "./",
        server: {headers: {"Cross-Origin-Opener-Policy": "same-origin", "Cross-Origin-Embedder-Policy": "require-corp"}},
        preview: {headers: {"Cross-Origin-Opener-Policy": "same-origin", "Cross-Origin-Embedder-Policy": "require-corp"}},
        define: {
            __CORS_PROXY_API_KEY__: JSON.stringify(env.CORS_PROXY_API_KEY),
        },
    };
});
