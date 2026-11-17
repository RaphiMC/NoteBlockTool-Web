<script lang="ts">
    import Upload from "@lucide/svelte/icons/upload";

    let {enabled, onfiles}: {enabled: boolean; onfiles: (files: File[]) => void} = $props();
    let dragDepth = $state(0);

    $effect(() => {
        if (!enabled) {
            dragDepth = 0;
        }
    });

    function isFileDrag(event: DragEvent): event is DragEvent & {dataTransfer: DataTransfer} {
        return event.dataTransfer?.types.includes("Files") ?? false;
    }

    function dragEnter(event: DragEvent): void {
        if (!isFileDrag(event) || !enabled) {
            return;
        }
        event.preventDefault();
        dragDepth++;
    }

    function dragOver(event: DragEvent): void {
        if (!isFileDrag(event)) {
            return;
        }
        event.preventDefault();
        event.dataTransfer.dropEffect = enabled ? "copy" : "none";
    }

    function dragLeave(event: DragEvent): void {
        if (isFileDrag(event)) {
            dragDepth = Math.max(0, dragDepth - 1);
        }
    }

    function drop(event: DragEvent): void {
        if (!isFileDrag(event)) {
            return;
        }
        event.preventDefault();
        dragDepth = 0;
        if (enabled) {
            onfiles(Array.from(event.dataTransfer.files));
        }
    }
</script>

<svelte:window
    ondragenter={dragEnter}
    ondragover={dragOver}
    ondragleave={dragLeave}
    ondrop={drop}
    ondragend={() => {
        dragDepth = 0;
    }}
    onblur={() => {
        dragDepth = 0;
    }} />

{#if dragDepth > 0 && enabled}
    <div class="pointer-events-none fixed inset-0 z-50 bg-neutral/95 p-4 backdrop-blur-sm" aria-hidden="true">
        <div class="grid h-full place-items-center rounded-2xl border-2 border-dashed border-primary p-6 text-center">
            <div>
                <span class="icon-box mx-auto mb-6"><Upload /></span>
                <h2 class="text-2xl font-semibold">Drop file to load</h2>
            </div>
        </div>
    </div>
{/if}
