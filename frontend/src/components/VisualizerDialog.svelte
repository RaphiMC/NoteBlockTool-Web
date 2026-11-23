<script lang="ts">
    import type {Snippet} from "svelte";
    import Dialog from "./Dialog.svelte";
    import ErrorAlert from "./ErrorAlert.svelte";
    import {startVisualizer, state as appState, stopVisualizer, updateVisualizerState} from "../lib/app.svelte";

    let {onclose, playbackButton, seekBar, volumeControl}: {onclose: () => void; playbackButton: Snippet<[boolean]>; seekBar: Snippet; volumeControl: Snippet<[string]>} = $props();

    function initialize(dialog: HTMLDialogElement): () => void {
        const canvas = dialog.querySelector("canvas")!;
        startVisualizer(canvas.transferControlToOffscreen());
        const resize = () => updateVisualizerState({width: Math.round(canvas.clientWidth * devicePixelRatio), height: Math.round(canvas.clientHeight * devicePixelRatio)});
        resize();
        const observer = new ResizeObserver(resize);
        observer.observe(canvas);
        window.addEventListener("resize", resize);
        return () => {
            observer.disconnect();
            window.removeEventListener("resize", resize);
            stopVisualizer();
        };
    }
</script>

<Dialog class="visualizer-dialog" title="Visualizer" description={appState.songMetadata!.title} closeLabel="Close visualizer dialog" {onclose} {initialize}>
    <div class="flex min-h-0 flex-1 flex-col gap-4 p-6">
        <ErrorAlert bind:message={appState.playerError} />
        <div class="min-h-0 flex-1 overflow-hidden rounded-box bg-black">
            <canvas class="block h-full w-full" aria-label="Song visualizer"></canvas>
        </div>
        <div class="flex shrink-0 flex-wrap items-center gap-4">
            {@render playbackButton(false)}
            <div class="min-w-0 flex-1 basis-48">
                {@render seekBar()}
            </div>
            {@render volumeControl("visualizer-volume")}
        </div>
    </div>
</Dialog>

<style>
    :global(.visualizer-dialog .modal-box) {
        height: 100%;
        max-width: none;
    }
</style>
