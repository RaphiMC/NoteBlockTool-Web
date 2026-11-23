<script lang="ts">
    import ChevronDown from "@lucide/svelte/icons/chevron-down";
    import X from "@lucide/svelte/icons/x";

    let {warnings = $bindable()}: {warnings: string[]} = $props();
</script>

{#if warnings.length}
    <details class="group collapse mb-4 bg-warning/10 text-warning">
        <summary class="collapse-title flex items-center gap-3 p-3">
            <ChevronDown class="size-4 group-open:rotate-180" />
            <span class="flex-1">{warnings.length} {warnings.length === 1 ? "warning" : "warnings"}</span>
            <button
                class="btn btn-square shrink-0 btn-ghost btn-sm"
                type="button"
                aria-label="Dismiss warnings"
                onclick={() => {
                    warnings = [];
                }}>
                <X />
            </button>
        </summary>
        <div class="collapse-content">
            <ul class="grid list-disc gap-2 pl-4 wrap-anywhere whitespace-pre-wrap">
                {#each warnings as warning}<li>{warning}</li>{/each}
            </ul>
        </div>
    </details>
{/if}
