<script lang="ts">
    import ArrowRight from "@lucide/svelte/icons/arrow-right";
    import AudioLines from "@lucide/svelte/icons/audio-lines";
    import CircleExclamationPoint from "@lucide/svelte/icons/circle-exclamation-point";
    import CirclePlay from "@lucide/svelte/icons/circle-play";
    import EighthNotes from "@lucide/svelte/icons/eighth-notes";
    import {onMount} from "svelte";
    import CustomSounds from "./components/CustomSounds.svelte";
    import ErrorAlert from "./components/ErrorAlert.svelte";
    import FileDropOverlay from "./components/FileDropOverlay.svelte";
    import PlayerDialog from "./components/PlayerDialog.svelte";
    import RenderDialog from "./components/RenderDialog.svelte";
    import SongDetails from "./components/SongDetails.svelte";
    import SongLoader from "./components/SongLoader.svelte";
    import StatusLine from "./components/StatusLine.svelte";
    import WarningList from "./components/WarningList.svelte";
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
    <ErrorAlert bind:message={appState.mainError} />
    <WarningList warnings={appState.mainWarnings} />

    {#if appState.workerState === "failed"}
        <section class="state-panel panel card" aria-labelledby="worker-error-title" role="alert">
            <span class="icon-box bg-error/10 text-error" aria-hidden="true"><CircleExclamationPoint /></span>
            <h2 class="card-title" id="worker-error-title">Worker unavailable</h2>
            <button class="btn btn-primary" type="button" onclick={() => location.reload()}>Reload page</button>
        </section>
    {:else if appState.workerState === "starting" || appState.songLoading}
        <section class="state-panel panel card" aria-labelledby="loading-title" aria-busy="true">
            <div class="loading-indicator" aria-hidden="true"><EighthNotes /></div>
            <h2 class="card-title" id="loading-title">
                {appState.workerState === "starting" ? "Loading worker..." : "Loading song"}
            </h2>
            <p class="min-h-5 text-base-content/60">{appState.mainStatus}</p>
        </section>
    {:else if appState.songMetadata}
        <SongDetails metadata={appState.songMetadata} />
    {:else}
        <SongLoader />
    {/if}

    {#if appState.workerState === "ready"}<CustomSounds />{/if}

    {#if appState.songMetadata}
        <aside class="panel card mt-4">
            <div class="card-body sm:flex-row sm:items-center">
                <div class="flex flex-1 items-center gap-4">
                    <span class="icon-box size-10" aria-hidden="true"><CirclePlay class="size-6" /></span>
                    <div class="min-w-0">
                        <h2 class="card-title">Play song</h2>
                        <p class="text-base-content/60">Play the song in real time</p>
                    </div>
                </div>
                <button
                    class="btn btn-primary"
                    type="button"
                    aria-haspopup="dialog"
                    onclick={() => {
                        appState.playerDialogOpen = true;
                    }}>
                    Play <ArrowRight />
                </button>
            </div>
        </aside>
        <aside class="panel card mt-4">
            <div class="card-body sm:flex-row sm:items-center">
                <div class="flex flex-1 items-center gap-4">
                    <span class="icon-box size-10" aria-hidden="true"><AudioLines class="size-6" /></span>
                    <div class="min-w-0">
                        <h2 class="card-title">Render audio</h2>
                        <p class="text-base-content/60">Render the song to an audio file</p>
                    </div>
                </div>
                <button
                    class="btn btn-primary"
                    type="button"
                    aria-haspopup="dialog"
                    onclick={() => {
                        appState.renderDialogOpen = true;
                    }}>
                    Render
                    <ArrowRight />
                </button>
            </div>
        </aside>
    {/if}

    <footer class="my-8 flex justify-center"><StatusLine status={appState.mainStatus} /></footer>
</main>

<RenderDialog />
<PlayerDialog />

<FileDropOverlay enabled={appState.workerState === "ready" && !appState.songLoading && !appState.playerDialogOpen && !appState.renderDialogOpen} onfiles={loadFiles} />
