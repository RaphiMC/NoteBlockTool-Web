import {RingBuffer} from "ringbuf.js";
import type {Response, SourceDataLineProcessorOptions, SourceDataLineProcessorRequest} from "../audio/source-data-line-messages";

const now = typeof performance !== "undefined" ? performance.now.bind(performance) : Date.now;

declare const renderQuantumSize: number;
declare const sampleRate: number;

declare abstract class AudioWorkletProcessor {
    readonly port: MessagePort;
}

declare function registerProcessor(name: string, processor: new (options: {outputChannelCount: number[]; processorOptions: SourceDataLineProcessorOptions}) => AudioWorkletProcessor): void;

class SourceDataLineProcessor extends AudioWorkletProcessor {
    private readonly audioBuffer: RingBuffer;
    private readonly controlSignal: Int8Array;
    private readonly workBuffer: Float32Array;
    private running = false;
    private error: Error | null = null;

    constructor({outputChannelCount, processorOptions}: {outputChannelCount: number[]; processorOptions: SourceDataLineProcessorOptions}) {
        super();
        this.audioBuffer = new RingBuffer(processorOptions.audioBuffer, Float32Array);
        this.controlSignal = new Int8Array(processorOptions.controlBuffer);
        const quantumFrameCount = typeof renderQuantumSize === "number" ? renderQuantumSize : 128;
        this.workBuffer = new Float32Array(quantumFrameCount * outputChannelCount[0]);
        this.port.onmessage = ({data}: MessageEvent<SourceDataLineProcessorRequest>) => {
            try {
                switch (data.type) {
                    case "start":
                        this.running = true;
                        break;
                    case "stop":
                        this.running = false;
                        break;
                    case "flush":
                        while (this.audioBuffer.availableRead()) {
                            this.audioBuffer.pop(this.workBuffer);
                        }
                        break;
                    default:
                        throw new Error("Unhandled source data line processor request type");
                }
                Atomics.store(this.controlSignal, 0, 0);
                this.port.postMessage({id: data.id} satisfies Response);
            } catch (error) {
                this.error = error instanceof Error ? error : new Error(String(error));
            }
        };
        this.port.onmessageerror = event => {
            console.error("The source data line processor received an unreadable request", event);
            this.error = new Error("The source data line processor received an unreadable request");
        };
    }

    process(_inputs: Float32Array[][], outputs: Float32Array[][]): boolean {
        if (this.error) {
            throw this.error;
        }
        if (!this.running) {
            return true;
        }
        const channelSamples = outputs[0];
        const channelCount = channelSamples.length;
        const totalFrameCount = channelSamples[0].length;
        const deadline = now() + (this.audioBuffer.capacity() / channelCount / sampleRate) * 1000;
        let writtenFrameCount = 0;
        while (writtenFrameCount < totalFrameCount && Atomics.load(this.controlSignal, 0) === 0) {
            const sampleCount = this.audioBuffer.pop(this.workBuffer, Math.min((totalFrameCount - writtenFrameCount) * channelCount, this.workBuffer.length));
            if (sampleCount === 0) {
                if (now() >= deadline) {
                    break;
                }
                continue;
            }
            const frameCount = sampleCount / channelCount;
            for (let frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                for (let channelIndex = 0; channelIndex < channelCount; channelIndex++) {
                    channelSamples[channelIndex][writtenFrameCount + frameIndex] = this.workBuffer[frameIndex * channelCount + channelIndex];
                }
            }
            writtenFrameCount += frameCount;
        }
        return true;
    }
}

registerProcessor("source-data-line", SourceDataLineProcessor);
