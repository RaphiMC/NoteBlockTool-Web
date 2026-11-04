export interface Message {
    type: string;
}

// Requests

export interface LoadFileMessage extends Message {
    type: "load-file";
    file: File;
}

export interface LoadNbwMessage extends Message {
    type: "load-nbw";
    id: string;
}

export interface RenderMessage extends Message {
    type: "render";
    audioFileOptions: WavAudioFileOptions | FlacAudioFileOptions | Mp3AudioFileOptions | OggOpusAudioFileOptions;
    playbackOptions: PlaybackOptions;
    rendererOptions: RendererOptions;
    extraOptions: ExtraOptions;
    advancedOptions: AdvancedOptions;
}

export interface AudioFileOptions {
    format: string;
    sampleRate: number;
    channels: number;
}

export type AudioFormat = RenderMessage["audioFileOptions"]["format"];

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

export interface SetCustomSoundsMessage extends Message {
    type: "set-custom-sounds";
    files: File[];
}

export interface UnloadSongMessage extends Message {
    type: "unload-song";
}

export type LoadMessage = LoadFileMessage | LoadNbwMessage;

export type WorkerRequest = LoadMessage | RenderMessage | SetCustomSoundsMessage | UnloadSongMessage;

// Responses

export interface ErrorMessage extends Message {
    type: "error";
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
    length: string;
    noteCount: number;
    tempo: string;
    instruments: string[];
    nbsVersion?: number | null;
}

export interface StatusMessage extends Message {
    type: "status";
    message: string;
}

export interface WarningMessage extends Message {
    type: "warning";
    message: string;
}

export interface WorkerReadyMessage extends Message {
    type: "worker-ready";
}

export type WorkerResponse = ErrorMessage | RenderFinishedMessage | RenderProgressMessage | SongLoadedMessage | StatusMessage | WarningMessage | WorkerReadyMessage;
