<script lang="ts">
    import Dialog from "./Dialog.svelte";
    import ErrorAlert from "./ErrorAlert.svelte";
    import StatusLine from "./StatusLine.svelte";
    import WarningList from "./WarningList.svelte";
    import {closeRenderDialog, startRender, state as appState} from "../lib/app.svelte";
    import {formatProgress} from "../lib/format";
    import type {AudioFormat, BitRateEncoding, Limiter, Resampler, StartRenderMessage, WavEncoding} from "../lib/messages";

    interface Settings {
        format: AudioFormat;
        sampleRate: number;
        stereo: boolean;
        wavEncoding: WavEncoding;
        flacBitsPerSample: number;
        flacCompressionLevel: number;
        mp3Encoding: BitRateEncoding;
        mp3BitRate: number;
        mp3Quality: number;
        oggOpusEncoding: BitRateEncoding;
        oggOpusBitRate: number;
        volume: number;
        timingJitter: boolean;
        resampler: Resampler;
        limiter: Limiter;
        includeGroups: string;
        maxSimultaneousSoundCount: number;
        preRenderCacheSize: number;
    }

    function defaultSettings(): Settings {
        return {
            format: "mp3",
            sampleRate: 48000,
            stereo: true,
            wavEncoding: "s16le",
            flacBitsPerSample: 16,
            flacCompressionLevel: 6,
            mp3Encoding: "vbr",
            mp3BitRate: 224,
            mp3Quality: 80,
            oggOpusEncoding: "vbr",
            oggOpusBitRate: 128,
            volume: 90,
            timingJitter: true,
            resampler: "linear",
            limiter: "windowed",
            includeGroups: "",
            maxSimultaneousSoundCount: 32767,
            preRenderCacheSize: 512,
        };
    }

    let settings = $state(defaultSettings());

    function submit(event: SubmitEvent): void {
        event.preventDefault();
        const base = {sampleRate: settings.sampleRate, stereo: settings.stereo};
        let audioFileOptions: StartRenderMessage["audioFileOptions"];
        switch (settings.format) {
            case "wav":
                audioFileOptions = {format: "wav", ...base, encoding: settings.wavEncoding};
                break;
            case "flac":
                audioFileOptions = {format: "flac", ...base, bitsPerSample: settings.flacBitsPerSample, compressionLevel: settings.flacCompressionLevel};
                break;
            case "mp3":
                if (settings.mp3Encoding === "vbr") {
                    audioFileOptions = {format: "mp3", ...base, encoding: "vbr", quality: settings.mp3Quality};
                } else {
                    audioFileOptions = {format: "mp3", ...base, encoding: settings.mp3Encoding, bitRate: settings.mp3BitRate};
                }
                break;
            case "ogg-opus":
                audioFileOptions = {format: "ogg-opus", ...base, encoding: settings.oggOpusEncoding, bitRate: settings.oggOpusBitRate};
                break;
        }
        startRender({
            type: "start-render",
            audioFileOptions,
            playbackOptions: {volume: settings.volume, timingJitter: settings.timingJitter},
            rendererOptions: {resampler: settings.resampler, limiter: settings.limiter},
            extraOptions: {includeGroups: settings.includeGroups},
            advancedOptions: {maxSimultaneousSoundCount: settings.maxSimultaneousSoundCount, preRenderCacheSize: settings.preRenderCacheSize},
        });
    }
</script>

{#if appState.renderDialogOpen}
    <Dialog title="Render audio" description={appState.songMetadata!.title} closeLabel="Close render dialog" onclose={closeRenderDialog}>
        <form class="dialog-form" onsubmit={submit}>
            <div class="dialog-body">
                <ErrorAlert bind:message={appState.renderError} />
                <WarningList bind:warnings={appState.renderWarnings} />
                <fieldset class="dialog-options space-y-4" disabled={appState.renderStarted}>
                    <legend class="sr-only">Render options</legend>
                    <fieldset class="fieldset">
                        <legend class="fieldset-legend">Audio file</legend>
                        <div class="grid gap-4 sm:grid-cols-3">
                            <div class="fieldset">
                                <label class="label" for="render-format">Format</label>
                                <select class="select w-full" id="render-format" bind:value={settings.format}>
                                    <option value="wav">WAV</option>
                                    <option value="flac">FLAC</option>
                                    <option value="mp3">MP3</option>
                                    <option value="ogg-opus">Ogg Opus</option>
                                </select>
                            </div>
                            <div class="fieldset">
                                <label class="label" for="render-sample-rate">Sample rate (Hz)</label>
                                <input class="input w-full" id="render-sample-rate" type="number" min="8000" max="192000" step="1" bind:value={settings.sampleRate} required />
                            </div>
                            <div class="fieldset">
                                <label class="label" for="render-stereo">Channels</label>
                                <select class="select w-full" id="render-stereo" bind:value={settings.stereo}>
                                    <option value={false}>Mono</option>
                                    <option value={true}>Stereo</option>
                                </select>
                            </div>
                        </div>
                        {#if settings.format === "wav"}
                            <fieldset class="form-grid">
                                <legend class="sr-only">WAV options</legend>
                                <div class="fieldset">
                                    <label class="label" for="render-wav-encoding">Encoding</label>
                                    <select class="select w-full" id="render-wav-encoding" bind:value={settings.wavEncoding}>
                                        <option value="u8">Unsigned 8-Bit PCM</option>
                                        <option value="s16le">Signed 16-Bit PCM</option>
                                        <option value="s24le">Signed 24-Bit PCM</option>
                                        <option value="s32le">Signed 32-Bit PCM</option>
                                        <option value="f32le">Float 32-Bit PCM</option>
                                    </select>
                                </div>
                            </fieldset>
                        {:else if settings.format === "flac"}
                            <fieldset class="form-grid">
                                <legend class="sr-only">FLAC options</legend>
                                <div class="fieldset">
                                    <label class="label" for="render-flac-bits-per-sample">Bits per sample</label>
                                    <input class="input w-full" id="render-flac-bits-per-sample" type="number" min="4" max="32" step="1" bind:value={settings.flacBitsPerSample} required />
                                </div>
                                <div class="fieldset">
                                    <label class="label" for="render-flac-compression-level">Compression level</label>
                                    <input class="input w-full" id="render-flac-compression-level" type="number" min="0" max="8" step="1" bind:value={settings.flacCompressionLevel} required />
                                </div>
                            </fieldset>
                        {:else if settings.format === "mp3"}
                            <fieldset class="form-grid">
                                <legend class="sr-only">MP3 options</legend>
                                <div class="fieldset">
                                    <label class="label" for="render-mp3-encoding">Encoding</label>
                                    <select class="select w-full" id="render-mp3-encoding" bind:value={settings.mp3Encoding}>
                                        <option value="cbr">Constant bitrate (CBR)</option>
                                        <option value="abr">Average bitrate (ABR)</option>
                                        <option value="vbr">Variable bitrate (VBR)</option>
                                    </select>
                                </div>
                                {#if settings.mp3Encoding === "vbr"}
                                    <div class="fieldset">
                                        <label class="label" for="render-mp3-quality">Quality (%)</label>
                                        <input class="input w-full" id="render-mp3-quality" type="number" min="0" max="100" step="1" bind:value={settings.mp3Quality} required />
                                    </div>
                                {:else}
                                    <div class="fieldset">
                                        <label class="label" for="render-mp3-bitrate">Bitrate (kbps)</label>
                                        <input class="input w-full" id="render-mp3-bitrate" type="number" min="8" max="320" step="1" bind:value={settings.mp3BitRate} required />
                                    </div>
                                {/if}
                            </fieldset>
                        {:else if settings.format === "ogg-opus"}
                            <fieldset class="form-grid">
                                <legend class="sr-only">Ogg Opus options</legend>
                                <div class="fieldset">
                                    <label class="label" for="render-opus-encoding">Encoding</label>
                                    <select class="select w-full" id="render-opus-encoding" bind:value={settings.oggOpusEncoding}>
                                        <option value="cbr">Constant bitrate (CBR)</option>
                                        <option value="abr">Average bitrate (ABR)</option>
                                        <option value="vbr">Variable bitrate (VBR)</option>
                                    </select>
                                </div>
                                <div class="fieldset">
                                    <label class="label" for="render-opus-bitrate">Bitrate (kbps)</label>
                                    <input class="input w-full" id="render-opus-bitrate" type="number" min="8" max="384" step="1" bind:value={settings.oggOpusBitRate} required />
                                </div>
                            </fieldset>
                        {/if}
                    </fieldset>
                    <fieldset class="fieldset">
                        <legend class="fieldset-legend">Playback</legend>
                        <div class="fieldset">
                            <div class="flex justify-between">
                                <label class="label" for="render-volume">Volume</label>
                                <output class="text-primary tabular-nums" for="render-volume">{settings.volume}%</output>
                            </div>
                            <input class="w-full accent-primary" id="render-volume" type="range" min="0" max="100" step="1" bind:value={settings.volume} />
                        </div>
                        <label class="label mt-4 items-start whitespace-normal" title="Adds slight timing jitter (±1ms) to make the song sound more natural and less artificial.&#10;This emulates the behaviour of playing the song in Note Block Studio.">
                            <input class="size-4 shrink-0 accent-primary" type="checkbox" bind:checked={settings.timingJitter} />
                            <span>
                                Timing jitter
                                <small class="block text-xs text-base-content/60">Add slight variation to note timing.</small>
                            </span>
                        </label>
                    </fieldset>
                    <fieldset class="fieldset">
                        <legend class="fieldset-legend">Rendering</legend>
                        <div class="form-grid">
                            <div class="fieldset">
                                <label class="label" for="render-resampler">Resampler</label>
                                <select class="select w-full" id="render-resampler" bind:value={settings.resampler}>
                                    <option value="point">Point</option>
                                    <option value="linear">Linear</option>
                                    <option value="cubic">Cubic</option>
                                </select>
                            </div>
                            <div class="fieldset">
                                <label class="label" for="render-limiter">Limiter</label>
                                <select class="select w-full" id="render-limiter" bind:value={settings.limiter}>
                                    <option value="windowed">Windowed</option>
                                    <option value="global">Global</option>
                                    <option value="none">None</option>
                                </select>
                            </div>
                        </div>
                    </fieldset>
                    <fieldset class="fieldset">
                        <legend class="fieldset-legend">Extra options</legend>
                        <div class="fieldset">
                            <label class="label" for="render-include-groups">Include layers</label>
                            <input class="input w-full" id="render-include-groups" type="text" placeholder="All layers" bind:value={settings.includeGroups} />
                            <p class="text-base-content/60">Enter layer numbers or ranges separated by commas, for example 1, 3-5. Leave empty to include all layers.</p>
                        </div>
                    </fieldset>
                    <fieldset class="fieldset">
                        <legend class="fieldset-legend">Advanced options</legend>
                        <div class="form-grid">
                            <div class="fieldset">
                                <label class="label" for="render-max-simultaneous-sound-count">Maximum simultaneous sounds</label>
                                <input class="input w-full" id="render-max-simultaneous-sound-count" type="number" min="1" step="1" bind:value={settings.maxSimultaneousSoundCount} required />
                            </div>
                            <div class="fieldset">
                                <label class="label" for="render-pre-render-cache-size">Pre-render cache size (MiB)</label>
                                <input class="input w-full" id="render-pre-render-cache-size" type="number" min="0" step="1" bind:value={settings.preRenderCacheSize} required />
                            </div>
                        </div>
                    </fieldset>
                </fieldset>
            </div>
            <footer class="dialog-footer">
                <div class="mb-2 flex justify-between text-base-content/60">
                    <label for="render-progress">Rendering progress</label>
                    <span class="tabular-nums">{formatProgress(appState.renderProgress)}</span>
                </div>
                <progress class="progress progress-primary" id="render-progress" max="100" value={appState.renderProgress}></progress>
                <StatusLine status={appState.renderStatus} inDialog />
                {#if appState.renderResult}<div class="render-result card mt-4 bg-success/10 p-4">
                        <div class="min-w-0">
                            <strong class="text-success">Audio ready</strong>
                            <p class="flex items-center gap-2 text-base-content/60">
                                <span class="truncate" title={appState.renderResult.fileName}>{appState.renderResult.fileName}</span>
                                <span class="text-base-content/30">·</span>
                                <span class="whitespace-nowrap">{appState.renderResult.fileSize}</span>
                            </p>
                        </div>
                        <a class="btn btn-success" href={appState.renderResult.url} download={appState.renderResult.fileName}>Download</a>
                        <audio class="col-span-full w-full" {@attach preview => () => preview.pause()} src={appState.renderResult.url} controls preload="metadata" aria-label="Rendered audio preview"></audio>
                    </div>{/if}
                <div class="modal-action flex-wrap justify-between">
                    <button
                        class="btn btn-secondary"
                        type="button"
                        disabled={appState.renderStarted}
                        onclick={() => {
                            settings = defaultSettings();
                        }}>
                        Reset defaults
                    </button>
                    <button class="btn btn-primary" type="submit" disabled={appState.renderStarted}>Render</button>
                </div>
            </footer>
        </form>
    </Dialog>
{/if}

<style lang="postcss">
    @reference "../styles.css";

    @layer utilities {
        .dialog-footer {
            @apply shrink-0 border-t border-base-300 bg-base-200 p-6;
        }

        .render-result {
            @apply grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4;
        }
    }

    /* Show frequent worker updates immediately instead of animating each one. */
    #render-progress::-webkit-progress-value {
        transition: none;
    }
</style>
