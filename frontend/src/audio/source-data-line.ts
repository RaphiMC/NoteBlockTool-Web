import workletUrl from "../workers/source-data-line.worklet.ts?worker&url";
import type {ErrorResponse, OpenResponse, Response, SourceDataLineProcessorOptions, SourceDataLineRequest} from "./source-data-line-messages";

export class SourceDataLineHost {
    private readonly port: MessagePort;
    private context: AudioContext | null = null;

    constructor(port: MessagePort) {
        this.port = port;
        this.port.onmessage = ({data}: MessageEvent<SourceDataLineRequest>) => {
            this.handle(data).catch(error => this.handleError(error));
        };
        this.port.onmessageerror = event => {
            console.error("The source data line received an unreadable request", event);
            this.handleError(new Error("The source data line received an unreadable request"));
        };
    }

    private async handle(request: SourceDataLineRequest): Promise<void> {
        switch (request.type) {
            case "open": {
                this.context = new AudioContext({sampleRate: request.sampleRate});
                await Promise.all([this.context.resume(), this.context.audioWorklet.addModule(workletUrl)]);
                const node = new AudioWorkletNode(this.context, "source-data-line", {
                    numberOfInputs: 0,
                    numberOfOutputs: 1,
                    outputChannelCount: [request.channels],
                    processorOptions: {buffer: request.buffer} satisfies SourceDataLineProcessorOptions,
                });
                node.onprocessorerror = () => this.handleError(new Error("The source data line processor encountered an unexpected error"));
                node.connect(this.context.destination);
                this.port.postMessage({id: request.id, port: node.port} satisfies OpenResponse, [node.port]);
                break;
            }
            case "close":
                await this.context?.close();
                this.context = null;
                this.port.postMessage({id: request.id} satisfies Response);
                this.port.close();
                break;
            default:
                throw new Error("Unhandled source data line request type");
        }
    }

    private handleError(error: unknown): void {
        const errorMessage = error instanceof Error ? error.message : String(error);
        this.port.postMessage({id: -1, errorMessage} satisfies ErrorResponse);
    }
}
