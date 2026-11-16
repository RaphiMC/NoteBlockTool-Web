export interface Request {
    type: string;
    id: number;
}

export interface OpenRequest extends Request {
    type: "open";
    sampleRate: number;
    channels: number;
    buffer: SharedArrayBuffer;
}

export interface StartRequest extends Request {
    type: "start";
}

export interface FlushRequest extends Request {
    type: "flush";
}

export interface StopRequest extends Request {
    type: "stop";
}

export interface CloseRequest extends Request {
    type: "close";
}

export interface Response {
    id: number;
}

export interface OpenResponse extends Response {
    port: MessagePort;
}

export interface ErrorResponse extends Response {
    id: -1;
    errorMessage: string;
}

export type SourceDataLineRequest = OpenRequest | CloseRequest;
export type SourceDataLineProcessorRequest = StartRequest | FlushRequest | StopRequest;
export type SourceDataLineProcessorOptions = Pick<OpenRequest, "buffer">;
