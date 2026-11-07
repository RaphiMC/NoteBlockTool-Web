<script lang="ts">
    import ArrowRight from "@lucide/svelte/icons/arrow-right";
    import AudioLines from "@lucide/svelte/icons/audio-lines";
    import CircleAlert from "@lucide/svelte/icons/circle-alert";
    import EighthNotes from "@lucide/svelte/icons/eighth-notes";
    import X from "@lucide/svelte/icons/x";
    import {onMount} from "svelte";
    import CustomSounds from "./components/CustomSounds.svelte";
    import FileDropOverlay from "./components/FileDropOverlay.svelte";
    import RenderDialog from "./components/RenderDialog.svelte";
    import SongDetails from "./components/SongDetails.svelte";
    import SongLoader from "./components/SongLoader.svelte";
    import StatusLine from "./components/StatusLine.svelte";
    import {initializeWorker, loadFiles, resumeWorker, state as appState, suspendWorker} from "./lib/app.svelte";

    onMount(initializeWorker);
</script>

<svelte:window onpagehide={suspendWorker} onpageshow={resumeWorker} />

<header class="border-b border-base-300">
    <div class="navbar mx-auto max-w-7xl px-4 sm:px-6">
        <div class="flex flex-1 items-center gap-2 font-semibold sm:text-lg">
            <img class="size-8" src={`${import.meta.env.BASE_URL}icon.png`} width="32" height="32" alt="" />
            <h1>NoteBlockTool</h1>
        </div>
        <a class="btn btn-ghost" href="https://github.com/RaphiMC/NoteBlockTool-Web" target="_blank" rel="noopener noreferrer" aria-label="GitHub repository (opens in a new tab)">GitHub</a>
    </div>
</header>

<main class="mx-auto my-8 max-w-6xl px-4 sm:px-6">
    {#if appState.error && appState.phase !== "failed"}
        <div class="mb-4 alert flex items-start alert-soft alert-error">
            <CircleAlert />
            <div class="min-w-0 flex-1">
                <strong class="font-semibold">Error</strong>
                <p class="wrap-anywhere whitespace-pre-wrap">{appState.error}</p>
            </div>
            <button
                class="btn btn-square shrink-0 btn-ghost btn-sm"
                type="button"
                aria-label="Dismiss error"
                onclick={() => {
                    appState.error = null;
                }}>
                <X />
            </button>
        </div>
    {/if}

    {#if appState.phase === "empty"}
        <SongLoader />
    {:else if appState.phase === "starting" || appState.phase === "loading"}
        <section class="state-panel panel card" aria-labelledby="loading-title" aria-busy="true">
            <div class="loader-art" aria-hidden="true"><EighthNotes /></div>
            <h2 class="card-title" id="loading-title" tabindex="-1" {@attach element => element.focus({preventScroll: true})}>
                {appState.phase === "starting" ? "Loading worker..." : "Loading song"}
            </h2>
            <p class="min-h-5 text-base-content/60">{appState.status.message}</p>
        </section>
    {:else if appState.phase === "failed"}
        <section class="state-panel panel card" aria-labelledby="worker-error-title" role="alert">
            <span class="icon-box bg-error/10 text-error" aria-hidden="true"><CircleAlert /></span>
            <h2 class="card-title" id="worker-error-title">Worker unavailable</h2>
            <p class="max-w-lg wrap-anywhere whitespace-pre-wrap text-error">{appState.error}</p>
            <button class="btn btn-primary" type="button" onclick={() => location.reload()} {@attach element => element.focus({preventScroll: true})}>Reload page</button>
        </section>
    {:else if appState.song}
        <SongDetails metadata={appState.song} />
    {/if}

    {#if appState.workerState === "ready"}<CustomSounds />{/if}

    {#if appState.song}
        <aside class="panel card mt-4">
            <div class="card-body sm:flex-row sm:items-center">
                <div class="flex flex-1 items-center gap-4">
                    <span class="icon-box size-10" aria-hidden="true"><AudioLines class="size-6" /></span>
                    <div class="min-w-0">
                        <h2 class="card-title">Render audio</h2>
                        <p class="text-base-content/60">Render this song to an audio file</p>
                    </div>
                </div>
                <button
                    class="btn btn-primary"
                    type="button"
                    aria-haspopup="dialog"
                    onclick={() => {
                        appState.dialogOpen = true;
                    }}
                    {@attach element => element.focus({preventScroll: true})}>
                    <span>{appState.phase === "rendering" ? "View rendering" : "Render"}</span>
                    <ArrowRight />
                </button>
            </div>
        </aside>
    {/if}

    <footer class="my-8 flex justify-center"><StatusLine status={appState.status} /></footer>
</main>

<RenderDialog />

<FileDropOverlay enabled={appState.phase === "empty"} onfiles={loadFiles} />
