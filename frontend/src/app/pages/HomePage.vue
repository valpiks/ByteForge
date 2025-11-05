<template>
  <div>
    <section class="gradient-hero py-20 md:py-32">
      <div class="containter mx-auto px-4">
        <div class="max-w-4xl mx-auto text-center">
          <div
            class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/20 mb-8"
          >
            <Icon icon="material-symbols:electric-bolt-rounded" class="h-4 w-4 text-primary" />
            <span class="text-sm font-medium text-primary">Online C++ Development</span>
          </div>
          <h1 class="text-5xl md:text-7xl font-bold mb-6 leading-tight">
            Code C++ Projects
            <br />
            <span class="bg-gradient-primary bg-clip-text text-transparent">Anywhere, Anytime</span>
          </h1>
          <p class="text-xl text-muted-foreground mb-12 max-w-2xl mx-auto">
            ByteForge is a modern online IDE designed for C++ developers. Write, compile, and run
            your code with powerful tools in the browser.
          </p>
          <div class="flex flex-wrap gap-4 justify-center">
            <router-link to="/register">
              <SimpleButton size="lg">
                <Icon icon="tabler:play" />
                Start Codding Free
              </SimpleButton>
            </router-link>
            <router-link to="/projects">
              <SimpleButton variant="outline" size="lg"
                ><Icon icon="material-symbols:edit-document-outline" /> View Projects</SimpleButton
              >
            </router-link>
          </div>
        </div>
      </div>
    </section>
    <section class="py-20 md:py-32">
      <div class="container mx-auto px-4">
        <div class="text-center mb-16">
          <h2 class="text-3xl md:text-4xl font-bold mb-4">
            Everything You Need to Build C++ Projects
          </h2>
          <p class="text-lg text-muted-foreground max-w-2xl mx-auto">
            Professional-grade tools and features for modern C++ development
          </p>
        </div>
        <div class="grid md:grid-cols-3 gap-8 max-w-5xl mx-auto">
          <AboutCard :key="card.title" v-for="card in cardInfo">
            <Icon :icon="card.icon" class="h-6 w-6 text-primary" />
            <template #title>
              {{ card.title }}
            </template>
            <template #description>
              {{ card.description }}
            </template>
          </AboutCard>
        </div>
      </div>
    </section>

    <section class="py-20 md:py-32">
      <div class="container mx-auto px-4">
        <div class="max-w-3xl mx-auto text-center text-primary-foreground">
          <h1 class="text-3xl md:text-5xl font-bold mb-4">Ready to Start Building?</h1>
          <p className="text-xl mb-8 opacity-90">
            Join thousands of developers using ByteForge for their C++ projects
          </p>
          <router-link to="/register">
            <SimpleButton size="lg" variant="secondary" class="shadow-glow"
              ><Icon icon="tabler:play" className="mr-2 h-5 w-5" /> Create Free Account
            </SimpleButton>
          </router-link>
        </div>
      </div>
    </section>
    <footer class="border-t py-8">
      <div class="container mx-auto px-4 text-center text-sm text-muted-foreground">
        <p>© 2025 ByteForge. Built for C++ developers.</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import AboutCard from '../components/Home/AboutCard.vue'
import SimpleButton from '../components/UI/buttons/SimpleButton.vue'

const cardInfo = [
  {
    icon: 'material-symbols:frame-source-rounded',
    title: 'Smart Code Editor',
    description: 'Syntax highlighting, auto-completion, and intelligent code analysis',
  },
  {
    icon: 'material-symbols:electric-bolt-rounded',
    title: 'Instant Compilation',
    description: 'Fast compilation with real-time error detection and debugging',
  },
  {
    icon: 'tabler:git-branch',
    title: 'Project Management',
    description: 'Organize multiple files and manage complex project structures',
  },
]
</script>

<style scoped></style>
