import {defineConfig, globalIgnores} from "eslint/config";
import tsParser from "@typescript-eslint/parser";
import svelteParser from "svelte-eslint-parser";

export default defineConfig(
    globalIgnores(["dist/**", "src/generated/*.js"]),
    {
        files: ["**/*.{js,ts,svelte}"],
        languageOptions: {
            parser: tsParser,
        },
        rules: {
            "arrow-body-style": ["error", "as-needed"],
            curly: "error",
        },
    },
    {
        files: ["**/*.svelte"],
        languageOptions: {
            parser: svelteParser,
            parserOptions: {
                parser: tsParser,
            },
        },
    }
);
