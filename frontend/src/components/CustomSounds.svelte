<script lang="ts">
    import FolderOpen from "@lucide/svelte/icons/folder-open";
    import X from "@lucide/svelte/icons/x";
    import {setCustomSounds, state as appState} from "../lib/app.svelte";
    import {formatNumber} from "../lib/format";

    let directoryInput: HTMLInputElement;

    function selectDirectory(input: HTMLInputElement): void {
        const files = Array.from(input.files!);
        if (files.length === 0) {
            return;
        }
        setCustomSounds(files);
        input.value = "";
    }
</script>

<input bind:this={directoryInput} type="file" webkitdirectory onchange={event => selectDirectory(event.currentTarget)} hidden />

<section class="panel card mt-4" aria-labelledby="custom-sounds-title">
    <div class="card-body gap-4 sm:flex-row sm:items-center">
        <div class="flex min-w-0 flex-1 items-center gap-4">
            <span class="icon-box size-10" aria-hidden="true"><FolderOpen class="size-6" /></span>
            <div class="min-w-0">
                <h2 class="card-title" id="custom-sounds-title">Custom sounds</h2>
                <p class="wrap-anywhere text-base-content/60">
                    {#if appState.customSoundsDirectory}Selected folder <span class="text-base-content">{appState.customSoundsDirectory.name}</span>
                        with
                        <span class="text-base-content">{formatNumber(appState.customSoundsDirectory.fileCount)} {appState.customSoundsDirectory.fileCount === 1 ? "file" : "files"}</span>{:else}Use custom sounds from a local folder{/if}
                </p>
            </div>
        </div>
        <div class="flex flex-wrap gap-2">
            <button class="btn border-primary/25 btn-soft btn-primary" type="button" onclick={() => directoryInput.click()}><FolderOpen />{appState.customSoundsDirectory ? "Change folder" : "Choose folder"}</button>
            {#if appState.customSoundsDirectory}<button class="btn border-base-content/20 btn-outline" type="button" onclick={() => setCustomSounds()}><X />Remove</button>{/if}
        </div>
    </div>
</section>
