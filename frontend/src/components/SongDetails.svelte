<script lang="ts">
    import type {LucideIcon} from "@lucide/svelte";
    import Clock from "@lucide/svelte/icons/clock";
    import EighthNotes from "@lucide/svelte/icons/eighth-notes";
    import Layers from "@lucide/svelte/icons/layers";
    import Metronome from "@lucide/svelte/icons/metronome";
    import X from "@lucide/svelte/icons/x";
    import {state, unloadSong} from "../lib/app.svelte";
    import {formatNumber} from "../lib/format";
    import type {SongLoadedMessage} from "../lib/messages";

    let {metadata}: {metadata: SongLoadedMessage} = $props();
    const hasCredits = $derived(Boolean(metadata.author || metadata.originalAuthor));
    const hasDescription = $derived(Boolean(metadata.description));
</script>

{#snippet statistic(title: string, value: string | number, Icon: LucideIcon)}
    <div class="stat min-w-0">
        <div class="stat-title flex items-center justify-between gap-2 whitespace-normal">
            <span>{title}</span>
            <Icon />
        </div>
        <p class="stat-value text-2xl wrap-anywhere whitespace-normal tabular-nums">{value}</p>
    </div>
{/snippet}

<section class="grid gap-4" aria-label="Song details">
    <div class="panel card">
        <div class="card-body flex-row items-start gap-4">
            <span class="icon-box" aria-hidden="true"><EighthNotes /></span>
            <div class="min-w-0 flex-1">
                <p class="eyebrow">LOADED SONG</p>
                <h2 class="card-title wrap-anywhere">{metadata.title}</h2>
                <p class="flex flex-wrap items-center gap-2 text-base-content/60">
                    <span class="truncate" title={metadata.fileName}>{metadata.fileName}</span>
                    <span>·</span>
                    <span class="whitespace-nowrap">{metadata.fileSize}</span>
                </p>
            </div>
            <button class="btn btn-square border-base-content/20 btn-outline" type="button" aria-label="Unload song" title="Unload song" disabled={state.phase === "rendering"} onclick={unloadSong}><X /></button>
        </div>
    </div>

    <div class={["panel stats grid grid-flow-row grid-cols-2", metadata.nbsVersion != null ? "md:grid-cols-4" : "md:grid-cols-3"]}>
        {@render statistic("Duration", metadata.length, Clock)}
        {@render statistic("Notes", formatNumber(metadata.noteCount), EighthNotes)}
        {@render statistic("Tempo", `${metadata.tempo} TPS`, Metronome)}
        {#if metadata.nbsVersion != null}{@render statistic("NBS version", metadata.nbsVersion, Layers)}{/if}
    </div>

    <div class={["grid gap-4", hasCredits && hasDescription && "md:grid-cols-2"]} hidden={!hasCredits && !hasDescription}>
        {#if hasCredits}
            <article class="panel card">
                <div class="card-body">
                    <h3 class="card-title">Credits</h3>
                    <dl class="grid gap-4">
                        {#if metadata.author}<div class="grid grid-cols-3 gap-4">
                                <dt class="text-base-content/60">Author</dt>
                                <dd class="col-span-2 wrap-anywhere">{metadata.author}</dd>
                            </div>{/if}{#if metadata.originalAuthor}<div class="grid grid-cols-3 gap-4">
                                <dt class="text-base-content/60">Original author</dt>
                                <dd class="col-span-2 wrap-anywhere">{metadata.originalAuthor}</dd>
                            </div>{/if}
                    </dl>
                </div>
            </article>
        {/if}
        {#if hasDescription}
            <article class="panel card">
                <div class="card-body">
                    <h3 class="card-title">Description</h3>
                    <p class="wrap-anywhere whitespace-pre-wrap">{metadata.description}</p>
                </div>
            </article>
        {/if}
    </div>

    {#if metadata.instruments.length}
        <article class="panel card">
            <div class="card-body">
                <h3 class="card-title">
                    Instruments
                    <span class="badge badge-neutral">{formatNumber(metadata.instruments.length)}</span>
                </h3>
                <ul class="flex flex-wrap gap-2" aria-label="Used instruments">
                    {#each metadata.instruments as name}<li class="badge h-auto max-w-full badge-soft wrap-anywhere whitespace-normal badge-info">
                            {name}
                        </li>{/each}
                </ul>
            </div>
        </article>
    {/if}
</section>
