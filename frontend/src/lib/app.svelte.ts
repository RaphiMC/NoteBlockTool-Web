import BackendWorker from "../workers/backend.worker?worker";
import type {LoadMessage, RenderFinishedMessage, RenderMessage, SetCustomSoundsMessage, SongLoadedMessage, WorkerResponse} from "./messages";

type Phase = "starting" | "empty" | "loading" | "loaded" | "rendering" | "failed";
type Tone = "idle" | "loading" | "success" | "warning" | "error";

export interface Status {
    message: string;
    tone: Tone;
}

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
    activity = $state<Status>({message: "", tone: "loading"});
    song = $state<SongLoadedMessage | null>(null);
    customSoundsDirectory = $state<CustomSoundsDirectory | null>(null);
    loading = $state(false);
    rendering = $state(false);
    error = $state<string | null>(null);
    dialogOpen = $state(false);
    progress = $state(0);
    warnings = $state<string[]>([]);
    result = $state<RenderResult | null>(null);
    phase = $derived<Phase>(this.workerState !== "ready" ? this.workerState : this.loading ? "loading" : this.rendering ? "rendering" : this.song ? "loaded" : "empty");
    status = $derived<Status>(this.error ? {message: this.error, tone: "error"} : this.activity);
}

export const state = new AppState();

let worker: Worker | null = null;
let idleMessage = "";
let loadedMessage = "";

function setStatus(message: string, tone: Tone = "idle"): void {
    state.activity = {message, tone};
}

function showError(message: string): void {
    state.error = message;
}

function clearRender(): void {
    state.rendering = false;
    if (state.result) {
        URL.revokeObjectURL(state.result.url);
    }
    state.result = null;
    state.progress = 0;
    state.warnings = [];
}

function returnToEmpty(): void {
    state.loading = false;
    state.dialogOpen = false;
    clearRender();
    state.song = null;
    loadedMessage = "";
    state.error = null;
    setStatus(idleMessage);
}

function stopWorker(): void {
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

function failRendering(message: string): void {
    state.rendering = false;
    setStatus(loadedMessage, "success");
    showError(message);
}

function finishRendering({fileName, fileSize, data, mimeType}: RenderFinishedMessage): void {
    const blob = new Blob([data], {type: mimeType});
    state.result = {url: URL.createObjectURL(blob), fileName, fileSize};
    state.progress = 100;
    state.rendering = false;
}

function handleMessage(data: WorkerResponse): void {
    if (data.type === "worker-ready" && state.phase === "starting") {
        state.workerState = "ready";
    } else if (data.type === "status") {
        if (state.phase === "empty") {
            idleMessage = data.message;
        }
        if (state.phase === "loaded") {
            loadedMessage = data.message;
        }
        setStatus(data.message, state.phase === "empty" ? "idle" : state.phase === "loaded" ? "success" : "loading");
    } else if (data.type === "warning") {
        state.warnings.push(data.message);
        setStatus(data.message, "warning");
    } else if (data.type === "render-progress" && state.phase === "rendering") {
        state.progress = data.progress;
    } else if (data.type === "render-finished" && state.rendering) {
        try {
            finishRendering(data);
        } catch (error) {
            console.error("Could not prepare the audio download.", error);
            clearRender();
            failRendering("The rendered audio could not be prepared for download. Try again.");
        }
    } else if (data.type === "song-loaded" && state.loading) {
        state.song = data;
        state.loading = false;
    } else if (data.type === "error") {
        if (state.phase === "loading") {
            failLoading(data.message);
        } else if (state.phase === "rendering") {
            failRendering(data.message);
        } else {
            showError(data.message);
        }
    }
}

function startWorker(): void {
    state.workerState = "starting";
    idleMessage = "";
    setStatus("", "loading");
    let current: Worker;
    try {
        current = new BackendWorker();
        worker = current;
    } catch (error) {
        console.error("Could not start the worker.", error);
        failWorker("The worker could not start. Reload the page and try again.");
        return;
    }
    current.onmessage = ({data}: MessageEvent<WorkerResponse>) => {
        if (worker === current) {
            handleMessage(data);
        }
    };
    current.onerror = event => {
        if (worker !== current) {
            return;
        }
        console.error("The worker encountered an unexpected error.", event.error ?? event.message);
        failWorker("The worker encountered an unexpected error. Reload the page and try again.");
    };
    current.onmessageerror = () => {
        if (worker === current) {
            failWorker("The worker returned an unreadable response. Reload the page and try again.");
        }
    };
}

function loadSong(message: LoadMessage): void {
    const current = worker;
    if (!current || state.phase !== "empty") {
        return;
    }
    state.error = null;
    clearRender();
    state.loading = true;
    setStatus("", "loading");
    try {
        current.postMessage(message);
    } catch (error) {
        console.error("Could not send the load request to the worker.", error);
        failLoading("The load request could not be sent to the worker. Try again.");
    }
}

export function loadFiles(files: readonly File[]): void {
    if (state.phase !== "empty") {
        return;
    }
    const [file] = files;
    if (files.length !== 1 || !file) {
        return showError("Select one .nbs file at a time.");
    }
    if (!/\.nbs$/i.test(file.name)) {
        return showError("Select a Note Block Studio (.nbs) file.");
    }
    if (file.size === 0) {
        return showError("The selected file is empty. Select another .nbs file.");
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
    if (state.phase !== "empty") {
        return;
    }
    const id = getNbwId(value);
    if (!id) {
        showError("Enter a NoteBlockWorld song URL: https://noteblock.world/song/id");
        return;
    }
    loadSong({type: "load-nbw", id});
}

export function unloadSong(): void {
    if (!worker || state.phase !== "loaded") {
        return;
    }
    try {
        worker.postMessage({type: "unload-song"});
        returnToEmpty();
    } catch (error) {
        console.error("Could not send the unload request to the worker.", error);
        showError("The unload request could not be sent to the worker. Try again.");
    }
}

export function setCustomSounds(files: SetCustomSoundsMessage["files"] = []): void {
    const current = worker;
    if (!current || (state.phase !== "empty" && state.phase !== "loaded")) {
        return;
    }
    try {
        current.postMessage({type: "set-custom-sounds", files} satisfies SetCustomSoundsMessage);
        const directoryName = files[0]?.webkitRelativePath.split("/")[0];
        state.customSoundsDirectory = directoryName ? {name: directoryName, fileCount: files.length} : null;
        state.error = null;
    } catch (error) {
        console.error("Could not send the custom sounds to the worker.", error);
        showError("The custom sounds could not be sent to the worker. Try again.");
    }
}

export function renderSong(message: RenderMessage): void {
    const current = worker;
    if (!current || state.phase !== "loaded") {
        return;
    }
    state.error = null;
    clearRender();
    state.rendering = true;
    setStatus("", "loading");
    try {
        current.postMessage(message);
    } catch (error) {
        console.error("Could not send the render settings to the worker.", error);
        failRendering("The render settings could not be sent to the worker. Try again.");
    }
}

export function suspendWorker(): void {
    stopWorker();
    returnToEmpty();
    state.workerState = "starting";
    setStatus("", "loading");
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
