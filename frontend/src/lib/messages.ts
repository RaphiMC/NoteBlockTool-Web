export interface Message {
    type: string;
}

// Frontend messages

export interface FrontendPlayerStateMessage extends Message {
    type: "frontend-player-state";
    paused: boolean | null;
    progress: number | null;
    volume: number | null;
}

export interface FrontendVisualizerStateMessage extends Message {
    type: "frontend-visualizer-state";
    width: number;
    height: number;
}

export interface LoadFileMessage extends Message {
    type: "load-file";
    file: File;
}

export interface LoadNbwMessage extends Message {
    type: "load-nbw";
    id: string;
}

export interface SetCustomSoundsMessage extends Message {
    type: "set-custom-sounds";
    files: File[];
}

export interface StartPlayerMessage extends Message {
    type: "start-player";
    playbackOptions: Pick<PlaybackOptions, "timingJitter">;
    rendererOptions: Pick<RendererOptions, "resampler">;
    advancedOptions: AdvancedOptions & {audioBufferMillis: number};
}

export interface StartRenderMessage extends Message {
    type: "start-render";
    audioFileOptions: WavAudioFileOptions | FlacAudioFileOptions | Mp3AudioFileOptions | OggOpusAudioFileOptions;
    playbackOptions: PlaybackOptions;
    rendererOptions: RendererOptions;
    extraOptions: ExtraOptions;
    advancedOptions: AdvancedOptions;
}

export interface AudioFileOptions {
    format: string;
    sampleRate: number;
    stereo: boolean;
}

export type AudioFormat = StartRenderMessage["audioFileOptions"]["format"];

export interface WavAudioFileOptions extends AudioFileOptions {
    format: "wav";
    encoding: WavEncoding;
}

export type WavEncoding = "u8" | "s16le" | "s24le" | "s32le" | "f32le";

export interface FlacAudioFileOptions extends AudioFileOptions {
    format: "flac";
    bitsPerSample: number;
    compressionLevel: number;
}

export type BitRateEncoding = "cbr" | "abr" | "vbr";

export type Mp3AudioFileOptions = AudioFileOptions & {format: "mp3"} & ({encoding: "cbr" | "abr"; bitRate: number} | {encoding: "vbr"; quality: number});

export interface OggOpusAudioFileOptions extends AudioFileOptions {
    format: "ogg-opus";
    encoding: BitRateEncoding;
    bitRate: number;
}

export interface PlaybackOptions {
    volume: number;
    timingJitter: boolean;
}

export interface RendererOptions {
    resampler: Resampler;
    limiter: Limiter;
}

export type Resampler = "point" | "linear" | "cubic";

export type Limiter = "windowed" | "global" | "none";

export interface ExtraOptions {
    includeGroups: string;
}

export interface AdvancedOptions {
    maxSimultaneousSoundCount: number;
    preRenderCacheSize: number;
}

export interface StartVisualizerMessage extends Message {
    type: "start-visualizer";
    canvas: OffscreenCanvas;
}

export interface StopPlayerMessage extends Message {
    type: "stop-player";
}

export interface StopRenderMessage extends Message {
    type: "stop-render";
}

export interface StopVisualizerMessage extends Message {
    type: "stop-visualizer";
}

export interface UnloadMessage extends Message {
    type: "unload";
}

export type LoadMessage = LoadFileMessage | LoadNbwMessage;

export type FrontendMessage = FrontendPlayerStateMessage | FrontendVisualizerStateMessage | LoadMessage | SetCustomSoundsMessage | StartPlayerMessage | StartRenderMessage | StartVisualizerMessage | StopPlayerMessage | StopRenderMessage | StopVisualizerMessage | UnloadMessage;

// Backend messages

export interface BackendPlayerStateMessage extends Message {
    type: "backend-player-state";
    paused: boolean;
    progress: number;
    humanReadablePosition: string;
    activeSoundCount: number;
    processingLoad: number;
}

export interface CreateSourceDataLineMessage extends Message {
    type: "create-source-data-line";
    port: MessagePort;
}

export interface ErrorMessage extends Message {
    type: "error";
    context: MessageContext;
    message: string;
}

export interface RenderFinishedMessage extends Message {
    type: "render-finished";
    fileName: string;
    fileSize: string;
    data: Int8Array<ArrayBuffer>;
    mimeType: string;
}

export interface RenderProgressMessage extends Message {
    type: "render-progress";
    progress: number;
}

export interface SongLoadedMessage extends Message {
    type: "song-loaded";
    fileName: string;
    fileSize: string;
    title: string;
    author: string | null;
    originalAuthor: string | null;
    description: string | null;
    humanReadableLength: string;
    lengthInSeconds: number;
    noteCount: number;
    humanReadableTempoRange: string;
    instruments: string[];
    nbsVersion?: number | null;
}

export interface StatusMessage extends Message {
    type: "status";
    context: MessageContext;
    message: string;
}

export type MessageContext = "main" | "render" | "player";

export interface WarningMessage extends Message {
    type: "warning";
    context: MessageContext;
    message: string;
}

export interface WorkerReadyMessage extends Message {
    type: "worker-ready";
}

export type BackendMessage = BackendPlayerStateMessage | CreateSourceDataLineMessage | ErrorMessage | RenderFinishedMessage | RenderProgressMessage | SongLoadedMessage | StatusMessage | WarningMessage | WorkerReadyMessage;
