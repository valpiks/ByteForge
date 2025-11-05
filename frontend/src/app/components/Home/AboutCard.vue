<template>
  <div class="p-6 rounded-2xl bg-card border transition-smooth hover:shadow-elegant">
    <div class="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center mb-4">
      <slot></slot>
    </div>
    <h3 class="text-xl font-semibold mb-2"><slot name="title"></slot></h3>
    <p class="text-muted-foreground">
      <slot name="description"></slot>
    </p>
  </div>
</template>
