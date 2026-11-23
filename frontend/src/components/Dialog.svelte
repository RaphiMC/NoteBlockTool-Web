<script lang="ts">
    import X from "@lucide/svelte/icons/x";
    import type {Snippet} from "svelte";

    let {title, description, closeLabel, onclose, initialize, class: className, children}: {title: string; description?: string; closeLabel: string; onclose: () => void; initialize?: (dialog: HTMLDialogElement) => () => void; class?: string; children: Snippet} = $props();

    const dialogId = $props.id();

    function modal(dialog: HTMLDialogElement): () => void {
        dialog.showModal();
        const cleanup = initialize?.(dialog);
        return () => {
            cleanup?.();
            dialog.close();
        };
    }
</script>

<dialog {@attach modal} class={["app-dialog modal bg-neutral/80 p-4 backdrop-blur-sm", className]} aria-labelledby={`${dialogId}-title`} aria-describedby={description ? `${dialogId}-description` : undefined} {onclose}>
    <div class="modal-box">
        <header class="dialog-header">
            <div class="min-w-0">
                <h2 class="card-title" id={`${dialogId}-title`}>{title}</h2>
                {#if description}<p class="truncate text-base-content/60" id={`${dialogId}-description`}>{description}</p>{/if}
            </div>
            <form method="dialog">
                <button class="btn btn-square border-base-content/20 btn-outline" type="submit" aria-label={closeLabel}><X /></button>
            </form>
        </header>
        {@render children()}
    </div>
</dialog>

<style lang="postcss">
    @reference "../styles.css";

    @layer utilities {
        .modal-box {
            @apply flex max-h-full w-full max-w-2xl flex-col overflow-hidden rounded-2xl border border-primary/20 p-0 shadow-2xl;
        }

        .dialog-header {
            @apply flex shrink-0 items-start justify-between gap-4 border-b border-base-300 p-6;
        }

        .app-dialog {
            :global(.dialog-form) {
                @apply flex min-h-0 flex-col;
            }

            :global(.dialog-body) {
                @apply min-h-0 overflow-y-auto overscroll-contain p-6;
            }

            :global(.dialog-options > .fieldset:not(:last-child)) {
                @apply border-b border-base-300 pb-4;
            }
        }
    }
</style>
