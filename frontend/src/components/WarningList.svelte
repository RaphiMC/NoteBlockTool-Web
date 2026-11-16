<script lang="ts">
    let {warnings}: {warnings: string[]} = $props();
</script>

{#if warnings.length}
    <details class="collapse-arrow collapse mb-4 bg-warning/10 text-warning">
        <summary class="collapse-title">{warnings.length} {warnings.length === 1 ? "warning" : "warnings"}</summary>
        <div class="collapse-content">
            <ul class="grid list-disc gap-2 pl-4 wrap-anywhere whitespace-pre-wrap">
                {#each warnings as warning}<li>{warning}</li>{/each}
            </ul>
        </div>
    </details>
{/if}
