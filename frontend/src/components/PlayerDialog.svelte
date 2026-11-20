<script lang="ts">
    import ChevronDown from "@lucide/svelte/icons/chevron-down";
    import Pause from "@lucide/svelte/icons/pause";
    import Play from "@lucide/svelte/icons/play";
    import SlidersHorizontal from "@lucide/svelte/icons/sliders-horizontal";
    import Square from "@lucide/svelte/icons/square";
    import Volume2 from "@lucide/svelte/icons/volume-2";
    import VolumeX from "@lucide/svelte/icons/volume-x";
    import X from "@lucide/svelte/icons/x";
    import Bowser from "bowser";
    import ErrorAlert from "./ErrorAlert.svelte";
    import StatusLine from "./StatusLine.svelte";
    import WarningList from "./WarningList.svelte";
    import {closePlayerDialog, startPlayer, state as appState, stopPlayer, updatePlayerState} from "../lib/app.svelte";
    import {formatNumber, formatProgress} from "../lib/format";
    import type {Resampler} from "../lib/messages";

    const USE_LARGE_AUDIO_BUFFER = Bowser.getParser(navigator.userAgent).isEngine("Gecko"); // Use a large buffer to absorb Firefox GC pauses with large live heaps

    interface Settings {
        volume: number;
        timingJitter: boolean;
        resampler: Resampler;
        maxSimultaneousSoundCount: number;
        preRenderCacheSize: number;
        audioBufferMillis: number;
    }

    function defaultSettings(): Settings {
        return {volume: 90, timingJitter: true, resampler: "linear", maxSimultaneousSoundCount: 2048, preRenderCacheSize: 512, audioBufferMillis: USE_LARGE_AUDIO_BUFFER ? 500 : 100};
    }

    let settings = $state(defaultSettings());
    let previousVolume = defaultSettings().volume;
    let settingsOpen = $state(false);
    let playerStatus = $derived(appState.playerState ? (appState.playerState.paused ? "Paused song" : "Playing song") : appState.playerStatus);
    let playerPositionInSeconds = $derived(appState.playerState ? (Math.min(Math.max(appState.playerState.progress, 0), 100) / 100) * appState.songMetadata!.lengthInSeconds : 0);

    $effect(() => {
        if (!("mediaSession" in navigator) || !appState.playerStarted) {
            return;
        }
        const session = navigator.mediaSession;
        session.metadata = new MediaMetadata({title: appState.songMetadata!.title, artist: appState.songMetadata!.author ?? ""});
        session.setActionHandler("play", () => updatePlayerState({paused: false}));
        session.setActionHandler("pause", () => updatePlayerState({paused: true}));
        const seekToSeconds = (position: number) => updatePlayerState({progress: Math.min(Math.max((position / appState.songMetadata!.lengthInSeconds) * 100, 0), 100)});
        session.setActionHandler("seekto", ({seekTime}) => seekToSeconds(seekTime!));
        session.setActionHandler("seekbackward", ({seekOffset}) => seekToSeconds(playerPositionInSeconds - (seekOffset ?? 10)));
        session.setActionHandler("seekforward", ({seekOffset}) => seekToSeconds(playerPositionInSeconds + (seekOffset ?? 10)));
        return () => {
            session.setActionHandler("play", null);
            session.setActionHandler("pause", null);
            session.setActionHandler("seekto", null);
            session.setActionHandler("seekbackward", null);
            session.setActionHandler("seekforward", null);
            session.setPositionState?.();
            session.playbackState = "none";
            session.metadata = null;
        };
    });

    $effect(() => {
        if ("mediaSession" in navigator && appState.playerState) {
            const session = navigator.mediaSession;
            session.playbackState = appState.playerState.paused ? "paused" : "playing";
            session.setPositionState?.({duration: appState.songMetadata!.lengthInSeconds, position: playerPositionInSeconds, playbackRate: 1});
        }
    });

    function modal(dialog: HTMLDialogElement): () => void {
        dialog.showModal();
        return () => dialog.close();
    }

    function close(): void {
        closePlayerDialog();
        settingsOpen = false;
    }

    function setVolume(volume: number): void {
        if (settings.volume > 0) {
            previousVolume = settings.volume;
        }
        settings.volume = volume;
        updatePlayerState({volume});
    }

    function submit(event: SubmitEvent): void {
        event.preventDefault();
        startPlayer({
            type: "start-player",
            playbackOptions: {timingJitter: settings.timingJitter},
            rendererOptions: {resampler: settings.resampler},
            advancedOptions: {maxSimultaneousSoundCount: settings.maxSimultaneousSoundCount, preRenderCacheSize: settings.preRenderCacheSize, audioBufferMillis: settings.audioBufferMillis},
        });
        updatePlayerState({paused: false, volume: settings.volume});
    }
</script>

{#if appState.playerDialogOpen}
    <dialog {@attach modal} class="options-dialog modal bg-neutral/80 p-4 backdrop-blur-sm" aria-labelledby="player-dialog-title" aria-describedby="player-song-name" onclose={close}>
        <div class="modal-box">
            <header class="dialog-header">
                <div class="min-w-0">
                    <h2 class="card-title" id="player-dialog-title">Play song</h2>
                    <p class="truncate text-base-content/60" id="player-song-name">{appState.songMetadata!.title}</p>
                </div>
                <form method="dialog">
                    <button class="btn btn-square border-base-content/20 btn-outline" type="submit" aria-label="Close player dialog"><X /></button>
                </form>
            </header>
            <form class="dialog-form" onsubmit={submit}>
                <div class="dialog-body">
                    <ErrorAlert bind:message={appState.playerError} />
                    <WarningList warnings={appState.playerWarnings} />
                    <input class="w-full accent-primary" aria-label="Playback position" aria-valuetext={appState.playerState?.humanReadablePosition ?? "00:00"} type="range" min="0" max="100" step="0.1" value={appState.playerState?.progress ?? 0} disabled={!appState.playerState} oninput={event => updatePlayerState({progress: event.currentTarget.valueAsNumber})} />
                    <div class="mt-2 flex justify-between text-base-content/60 tabular-nums">
                        <span>{appState.playerState?.humanReadablePosition ?? "00:00"}</span>
                        <span>{appState.songMetadata!.humanReadableLength}</span>
                    </div>
                    <div class="mt-4 flex flex-wrap items-center gap-2">
                        {#if appState.playerStarted}
                            <button class="btn btn-circle btn-lg btn-primary" type="button" disabled={!appState.playerState} aria-label={!appState.playerState ? "Starting playback" : appState.playerState.paused ? "Resume playback" : "Pause playback"} onclick={() => updatePlayerState({paused: !appState.playerState!.paused})}>
                                {#if !appState.playerState}<span class="loading-indicator mb-0 size-6" aria-label="Starting playback"></span>{:else if appState.playerState.paused}<Play class="size-6" />{:else}<Pause class="size-6" />{/if}
                            </button>
                            <button class="btn btn-square btn-ghost" type="button" aria-label="Stop playback" onclick={stopPlayer}><Square /></button>
                        {:else}
                            <button class="btn btn-circle btn-lg btn-primary" type="submit" aria-label="Start playback"><Play class="size-6" /></button>
                        {/if}
                        <div class="ml-auto flex items-center gap-2">
                            <button class="btn btn-square shrink-0 btn-ghost btn-sm" type="button" aria-label={settings.volume === 0 ? "Unmute" : "Mute"} title={settings.volume === 0 ? "Unmute" : "Mute"} onclick={() => setVolume(settings.volume === 0 ? previousVolume : 0)}>
                                {#if settings.volume === 0}<VolumeX />{:else}<Volume2 />{/if}
                            </button>
                            <input class="w-24 accent-primary sm:w-32" id="player-volume" aria-label="Volume" type="range" min="0" max="100" step="1" value={settings.volume} oninput={event => setVolume(event.currentTarget.valueAsNumber)} />
                            <output class="w-10 text-right text-base-content/60 tabular-nums" for="player-volume">{settings.volume}%</output>
                        </div>
                    </div>
                    <dl class="mt-6 flex flex-wrap gap-x-6 gap-y-2 text-base-content/60">
                        <div class="flex gap-2">
                            <dt>Active sounds:</dt>
                            <dd class="text-base-content tabular-nums">{formatNumber(appState.playerState?.activeSoundCount ?? 0)}</dd>
                        </div>
                        <div class="flex gap-2">
                            <dt>Processing load:</dt>
                            <dd class="text-base-content tabular-nums">{formatProgress(appState.playerState?.processingLoad ?? 0)}</dd>
                        </div>
                    </dl>
                    <div class="mt-6 border-t border-base-300 pt-4">
                        <div class="flex items-center justify-between gap-4">
                            <StatusLine status={playerStatus} />
                            <button
                                class="btn shrink-0 btn-ghost btn-sm"
                                type="button"
                                aria-expanded={settingsOpen}
                                aria-controls="player-settings"
                                onclick={() => {
                                    settingsOpen = !settingsOpen;
                                }}>
                                <SlidersHorizontal />Settings<ChevronDown class={["transition-transform", settingsOpen && "rotate-180"]} />
                            </button>
                        </div>
                        <fieldset
                            class="dialog-options mt-4 space-y-4"
                            id="player-settings"
                            hidden={!settingsOpen}
                            disabled={appState.playerStarted}
                            oninvalidcapture={event => {
                                settingsOpen = true;
                                event.currentTarget.hidden = false;
                            }}>
                            <legend class="sr-only">Player options</legend>
                            <fieldset class="fieldset">
                                <legend class="fieldset-legend">Playback</legend>
                                <label class="label items-start whitespace-normal" title="Adds slight timing jitter (±1ms) to make the song sound more natural and less artificial.&#10;This emulates the behaviour of playing the song in Note Block Studio.">
                                    <input class="size-4 shrink-0 accent-primary" type="checkbox" bind:checked={settings.timingJitter} />
                                    <span>
                                        Timing jitter
                                        <small class="block text-xs text-base-content/60">Add slight variation to note timing.</small>
                                    </span>
                                </label>
                            </fieldset>
                            <fieldset class="fieldset">
                                <legend class="fieldset-legend">Rendering</legend>
                                <div class="fieldset">
                                    <label class="label" for="player-resampler">Resampler</label>
                                    <select class="select w-full" id="player-resampler" bind:value={settings.resampler}>
                                        <option value="point">Point</option>
                                        <option value="linear">Linear</option>
                                        <option value="cubic">Cubic</option>
                                    </select>
                                </div>
                            </fieldset>
                            <fieldset class="fieldset">
                                <legend class="fieldset-legend">Advanced options</legend>
                                <div class="form-grid">
                                    <div class="fieldset">
                                        <label class="label" for="player-max-simultaneous-sound-count">Maximum simultaneous sounds</label>
                                        <input class="input w-full" id="player-max-simultaneous-sound-count" type="number" min="1" step="1" bind:value={settings.maxSimultaneousSoundCount} required />
                                    </div>
                                    <div class="fieldset">
                                        <label class="label" for="player-pre-render-cache-size">Pre-render cache size (MiB)</label>
                                        <input class="input w-full" id="player-pre-render-cache-size" type="number" min="0" step="1" bind:value={settings.preRenderCacheSize} required />
                                    </div>
                                    <div class="fieldset">
                                        <label class="label" for="player-audio-buffer-millis">Audio buffer (ms)</label>
                                        <input class="input w-full" id="player-audio-buffer-millis" type="number" min="10" step="1" bind:value={settings.audioBufferMillis} required />
                                    </div>
                                </div>
                            </fieldset>
                            <button
                                class="btn btn-secondary"
                                type="button"
                                onclick={() => {
                                    settings = defaultSettings();
                                }}>
                                Reset defaults
                            </button>
                        </fieldset>
                    </div>
                </div>
            </form>
        </div>
    </dialog>
{/if}
