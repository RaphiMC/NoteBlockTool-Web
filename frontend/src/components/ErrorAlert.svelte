<script lang="ts">
    import CircleExclamationPoint from "@lucide/svelte/icons/circle-exclamation-point";
    import X from "@lucide/svelte/icons/x";

    let {message = $bindable()}: {message: string | null} = $props();
</script>

{#if message}
    <div class="mb-4 alert flex items-start alert-soft alert-error" role="alert">
        <CircleExclamationPoint />
        <div class="min-w-0 flex-1">
            <strong class="font-semibold">Error</strong>
            <p class="wrap-anywhere whitespace-pre-wrap">{message}</p>
        </div>
        <button
            class="btn btn-square shrink-0 btn-ghost btn-sm"
            type="button"
            aria-label="Dismiss error"
            onclick={() => {
                message = null;
            }}>
            <X />
        </button>
    </div>
{/if}
