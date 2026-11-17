<script lang="ts">
    import FolderOpen from "@lucide/svelte/icons/folder-open";
    import Upload from "@lucide/svelte/icons/upload";
    import {loadFiles, loadNbw} from "../lib/app.svelte";

    let fileInput: HTMLInputElement;
    let nbwUrl = $state("");
</script>

<input
    bind:this={fileInput}
    type="file"
    accept=".nbs,.mid,.midi,.txt,.mcsp,.mcsp2"
    onchange={event => {
        if (event.currentTarget.files!.length) {
            loadFiles(Array.from(event.currentTarget.files!));
        }
    }}
    hidden />

<section class="panel card" aria-label="Open a song">
    <div class="card-body min-h-96 items-center justify-center gap-4 text-center">
        <span class="icon-box" aria-hidden="true"><Upload /></span>
        <h2 class="card-title">Load a song</h2>
        <p class="grow-0 text-base-content/60">Drag and drop a song file anywhere, or select one below.</p>
        <button class="btn btn-primary" type="button" onclick={() => fileInput.click()}><FolderOpen />Choose file</button>
    </div>
</section>

<form
    class="panel card mt-4"
    onsubmit={event => {
        event.preventDefault();
        loadNbw(nbwUrl);
    }}>
    <div class="card-body sm:flex-row sm:items-end">
        <div class="fieldset flex-1">
            <label class="label" for="nbw-url">NoteBlockWorld URL</label>
            <input class="input w-full" id="nbw-url" type="url" bind:value={nbwUrl} placeholder="https://noteblock.world/song/id" required spellcheck="false" autocapitalize="none" />
        </div>
        <button class="btn btn-primary sm:mb-1" type="submit">Load song</button>
    </div>
</form>
