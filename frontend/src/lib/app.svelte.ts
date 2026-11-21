import BackendWorker from "../workers/backend.worker?worker";
import type {BackendMessage, BackendPlayerStateMessage, FrontendPlayerStateMessage, LoadMessage, MessageContext, RenderFinishedMessage, SetCustomSoundsMessage, SongLoadedMessage, StartPlayerMessage, StartRenderMessage, StopPlayerMessage, StopRenderMessage} from "./messages";
import {SourceDataLineHost} from "../audio/source-data-line";

interface CustomSoundsDirectory {
    name: string;
    fileCount: number;
}

interface RenderResult {
    url: string;
    fileName: string;
    fileSize: string;
}

class AppState {
    workerState = $state<"starting" | "ready" | "failed">("starting");
    mainStatus = $state("Loading worker");
    mainWarnings = $state<string[]>([]);
    mainError = $state<string | null>(null);

    songMetadata = $state<SongLoadedMessage | null>(null);
    songLoading = $state(false);
    customSoundsDirectory = $state<CustomSoundsDirectory | null>(null);

    renderDialogOpen = $state(false);
    renderStatus = $state("Waiting for action");
    renderWarnings = $state<string[]>([]);
    renderError = $state<string | null>(null);
    renderStarted = $state(false);
    renderProgress = $state(0);
    renderResult = $state<RenderResult | null>(null);

    playerDialogOpen = $state(false);
    playerStatus = $state("Waiting for action");
    playerWarnings = $state<string[]>([]);
    playerError = $state<string | null>(null);
    playerStarted = $state(false);
    playerState = $state<BackendPlayerStateMessage | null>(null);
}

export const state = new AppState();

let worker: Worker | null = null;

function showError(message: string, context: MessageContext = "main"): void {
    state[`${context}Error`] = message;
}

function clearRender(): void {
    if (state.renderResult) {
        URL.revokeObjectURL(state.renderResult.url);
    }
    state.renderResult = null;
    state.renderProgress = 0;
}

function returnToEmpty(): void {
    state.songLoading = false;
    closeRenderDialog();
    closePlayerDialog();
    clearRender();
    state.songMetadata = null;
}

function stopWorker(): void {
    state.renderStarted = false;
    state.playerStarted = false;
    state.playerState = null;
    worker?.terminate();
    worker = null;
    state.customSoundsDirectory = null;
}

function failWorker(message: string): void {
    stopWorker();
    returnToEmpty();
    state.workerState = "failed";
    showError(message);
}

function failLoading(message: string): void {
    returnToEmpty();
    showError(message);
}

function finishRendering({fileName, fileSize, data, mimeType}: RenderFinishedMessage): void {
    state.renderStarted = false;
    const blob = new Blob([data], {type: mimeType});
    state.renderResult = {url: URL.createObjectURL(blob), fileName, fileSize};
    state.renderProgress = 100;
}

function handleMessage(data: BackendMessage): void {
    if ("context" in data && data.context !== "main" && !state[`${data.context}DialogOpen`]) {
        return;
    }
    switch (data.type) {
        case "backend-player-state":
            if (state.playerStarted) {
                state.playerState = data;
            }
            break;
        case "create-source-data-line":
            new SourceDataLineHost(data.port);
            break;
        case "worker-ready":
            state.workerState = "ready";
            state.mainStatus = "Waiting for action";
            break;
        case "status":
            if (data.context !== "player" || state.playerStarted) {
                state[`${data.context}Status`] = data.message;
            }
            break;
        case "warning":
            state[`${data.context}Warnings`].push(data.message);
            break;
        case "render-progress":
            if (state.renderStarted) {
                state.renderProgress = data.progress;
            }
            break;
        case "render-finished":
            try {
                finishRendering(data);
            } catch (error) {
                console.error("Failed to prepare the audio download", error);
                clearRender();
                showError("Failed to prepare the audio download", "render");
            }
            break;
        case "song-loaded":
            state.songMetadata = data;
            state.songLoading = false;
            break;
        case "error":
            if (data.context === "main" && state.songLoading) {
                failLoading(data.message);
            } else {
                showError(data.message, data.context);
            }
            break;
        default:
            console.error("Unhandled worker message", data);
            break;
    }
}

function startWorker(): void {
    state.workerState = "starting";
    state.mainStatus = "Loading worker";
    let current: Worker;
    try {
        current = new BackendWorker();
        worker = current;
    } catch (error) {
        console.error("Failed to start the worker", error);
        failWorker("Failed to start the worker. Reload the page and try again.");
        return;
    }
    current.onmessage = ({data}: MessageEvent<BackendMessage>) => {
        if (worker === current) {
            handleMessage(data);
        }
    };
    current.onerror = event => {
        if (worker === current) {
            console.error("The worker encountered an unexpected error", event.error ?? event.message);
            failWorker("The worker encountered an unexpected error. Reload the page and try again.");
        }
    };
    current.onmessageerror = event => {
        if (worker === current) {
            console.error("The worker returned an unreadable message", event);
            failWorker("The worker returned an unreadable message. Reload the page and try again.");
        }
    };
}

function loadSong(message: LoadMessage): void {
    const current = worker;
    if (!current) {
        return;
    }
    clearRender();
    state.songLoading = true;
    try {
        current.postMessage(message);
    } catch (error) {
        console.error("Failed to send the load message to the worker", error);
        failLoading("Failed to send the load message to the worker");
    }
}

export function loadFiles(files: readonly File[]): void {
    const [file] = files;
    if (files.length !== 1 || !file) {
        return showError("Select one file at a time");
    }
    if (!/\.(nbs|mid|midi|txt|mcsp|mcsp2)$/i.test(file.name)) {
        return showError("Select an .nbs, .mid, .midi, .txt, .mcsp, or .mcsp2 file");
    }
    if (state.songMetadata) {
        unload();
    }
    loadSong({type: "load-file", file});
}

function getNbwId(value: string): string | null {
    try {
        const url = new URL(value);
        if (url.origin !== "https://noteblock.world" || url.username || url.password) {
            return null;
        }
        return url.pathname.match(/^\/song\/([^/]+)\/?$/)?.[1] ?? null;
    } catch {
        return null;
    }
}

export function loadNbw(value: string): void {
    const id = getNbwId(value);
    if (!id) {
        showError("Enter a NoteBlockWorld song URL: https://noteblock.world/song/id");
        return;
    }
    loadSong({type: "load-nbw", id});
}

export function unload(): void {
    if (!worker) {
        return;
    }
    try {
        worker.postMessage({type: "unload"});
        returnToEmpty();
        state.mainStatus = "Waiting for action";
    } catch (error) {
        console.error("Failed to send the unload message to the worker", error);
        showError("Failed to send the unload message to the worker");
    }
}

export function setCustomSounds(files: SetCustomSoundsMessage["files"] = []): void {
    const current = worker;
    if (!current) {
        return;
    }
    try {
        current.postMessage({type: "set-custom-sounds", files} satisfies SetCustomSoundsMessage);
        const directoryName = files[0]?.webkitRelativePath.split("/")[0];
        state.customSoundsDirectory = directoryName ? {name: directoryName, fileCount: files.length} : null;
    } catch (error) {
        console.error("Failed to send the custom sounds to the worker", error);
        showError("Failed to send the custom sounds to the worker");
    }
}

export function startRender(message: StartRenderMessage): void {
    if (!worker) {
        return;
    }
    clearRender();
    state.renderWarnings = [];
    state.renderStatus = "Waiting for action";
    try {
        worker.postMessage(message);
        state.renderStarted = true;
    } catch (error) {
        console.error("Failed to render the song", error);
        showError("Failed to render the song", "render");
    }
}

export function stopRender(): void {
    if (!worker || !state.renderStarted) {
        return;
    }
    try {
        worker.postMessage({type: "stop-render"} satisfies StopRenderMessage);
        state.renderStarted = false;
        state.renderProgress = 0;
        state.renderStatus = "Waiting for action";
    } catch (error) {
        console.error("Failed to stop the render", error);
        showError("Failed to stop the render", "render");
    }
}

export function startPlayer(message: StartPlayerMessage): void {
    if (!worker) {
        return;
    }
    state.playerWarnings = [];
    state.playerStatus = "Waiting for action";
    state.playerState = null;
    try {
        worker.postMessage(message);
        state.playerStarted = true;
    } catch (error) {
        console.error("Failed to start the player", error);
        showError("Failed to start the player", "player");
    }
}

export function stopPlayer(): void {
    if (!worker || !state.playerStarted) {
        return;
    }
    try {
        worker.postMessage({type: "stop-player"} satisfies StopPlayerMessage);
        state.playerStarted = false;
        state.playerState = null;
        state.playerStatus = "Waiting for action";
    } catch (error) {
        console.error("Failed to stop the player", error);
        showError("Failed to stop the player", "player");
    }
}

export function updatePlayerState(changes: Partial<Omit<FrontendPlayerStateMessage, "type">>): void {
    if (!worker || !state.playerStarted) {
        return;
    }
    try {
        worker.postMessage({type: "frontend-player-state", paused: null, progress: null, volume: null, ...changes} satisfies FrontendPlayerStateMessage);
    } catch (error) {
        console.error("Failed to send the player state to the worker", error);
        showError("Failed to send the player state to the worker", "player");
    }
}

export function closeRenderDialog(): void {
    stopRender();
    state.renderDialogOpen = false;
    state.renderStatus = "Waiting for action";
    state.renderWarnings = [];
    state.renderError = null;
}

export function closePlayerDialog(): void {
    stopPlayer();
    state.playerDialogOpen = false;
    state.playerStatus = "Waiting for action";
    state.playerWarnings = [];
    state.playerError = null;
}

export function suspendWorker(): void {
    stopWorker();
    returnToEmpty();
    state.workerState = "starting";
}

export function resumeWorker(event: PageTransitionEvent): void {
    if (event.persisted) {
        startWorker();
    }
}

export function initializeWorker(): () => void {
    startWorker();
    return suspendWorker;
}
