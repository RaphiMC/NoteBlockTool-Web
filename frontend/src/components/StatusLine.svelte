<script lang="ts">
    let {status, inDialog = false}: {status: string; inDialog?: boolean} = $props();
</script>

<div class={["flex min-h-5 max-w-full min-w-0 items-start gap-2 text-info", inDialog && "mt-3"]} role="status" aria-live="polite" aria-atomic="true">
    <span class="mt-1.5 status shrink-0 bg-current text-inherit"></span>
    <span class={["wrap-anywhere", inDialog ? "whitespace-pre-wrap" : "line-clamp-2"]}>{status}</span>
</div>
