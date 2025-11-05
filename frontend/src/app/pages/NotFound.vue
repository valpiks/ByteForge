<template>
  <div class="min-h-screen bg-background">
    <NavBar />

    <main>
      <section class="relative gradient-hero py-20 md:py-32 overflow-hidden">
        <div
          class="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-accent/5 animate-gradient-shift"
        />

        <div class="absolute inset-0 opacity-10">
          <div class="grid-pattern"></div>
        </div>

        <div class="absolute inset-0 pointer-events-none overflow-hidden">
          <div
            v-for="icon in floatingIcons"
            :key="icon.id"
            class="absolute opacity-15 animate-float"
            :style="{
              left: `${icon.x}%`,
              top: `${icon.y}%`,
              animationDelay: `${icon.delay}s`,
              animationDuration: `${6 + Math.random() * 4}s`,
              transform: `scale(${1 + (icon.id % 3) * 0.2})`,
            }"
          >
            <Icon
              icon="tabler:code"
              v-if="icon.id % 3 === 0"
              class="w-12 h-12 text-primary animate-spin-slow"
            />
            <Icon
              icon="tabler:terminal-2"
              v-else-if="icon.id % 3 === 1"
              class="w-12 h-12 text-accent"
            />
            <Icon
              icon="tabler:sparkles"
              v-else
              class="w-12 h-12 text-primary-glow animate-pulse-slow"
            />
          </div>
        </div>

        <div class="absolute inset-0 pointer-events-none">
          <div
            v-for="particle in particles"
            :key="particle.id"
            class="absolute w-1 h-1 bg-primary/30 rounded-full animate-pulse"
            :style="{
              left: `${particle.x}%`,
              top: `${particle.y}%`,
              animationDelay: `${particle.delay}s`,
              animationDuration: `${2 + Math.random() * 3}s`,
            }"
          ></div>
        </div>

        <div class="container mx-auto px-4 relative z-10">
          <div class="max-w-4xl mx-auto text-center">
            <div
              class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-destructive/10 border border-destructive/20 mb-8 animate-fade-in backdrop-blur-sm"
            >
              <Icon icon="tabler:alert-circle" class="h-4 w-4 text-destructive animate-pulse" />
              <span class="text-sm font-medium text-destructive">Page Not Found</span>
            </div>

            <div class="relative mb-6">
              <h1
                class="text-[150px] md:text-[250px] font-black leading-none z-10 mb-4 text-primary/20 animate-scale-in select-none relative"
              >
                {{ glitchText }}
                <span
                  class="absolute top-0 left-0 w-full h-full text-primary/40 animate-glitch-1"
                  >{{ glitchText }}</span
                >
                <span class="absolute top-0 left-0 w-full h-full text-accent/30 animate-glitch-2">{{
                  glitchText
                }}</span>
              </h1>

              <div class="absolute inset-0 blur-3xl opacity-30 bg-primary/20 animate-pulse" />
              <div class="absolute inset-0 text-shadow-glow opacity-60" />

              <div class="absolute inset-0">
                <div
                  class="absolute top-1/2 left-1/4 w-3 h-3 bg-primary rounded-full animate-orbit-1 shadow-glow"
                />
                <div
                  class="absolute top-1/2 right-1/4 w-2 h-2 bg-accent rounded-full animate-orbit-2 shadow-glow"
                />
                <div
                  class="absolute top-1/4 left-1/2 w-2.5 h-2.5 bg-primary-glow rounded-full animate-orbit-3 shadow-glow"
                />
                <div
                  class="absolute bottom-1/4 right-1/3 w-2 h-2 bg-accent rounded-full animate-orbit-4 shadow-glow"
                />
              </div>
            </div>

            <h2
              class="text-3xl md:text-5xl font-bold mb-6 animate-fade-in animate-slide-up"
              style="animation-delay: 0.1s"
            >
              <span
                v-for="(char, index) in titleText"
                :key="index"
                class="inline-block"
                :style="{
                  animationDelay: `${index * 0.05}s`,
                  color: index >= 10 ? 'transparent' : '',
                  backgroundImage:
                    index >= 10
                      ? 'linear-gradient(135deg, hsl(262 83% 58%), hsl(262 83% 70%))'
                      : '',
                  WebkitBackgroundClip: index >= 10 ? 'text' : '',
                }"
              >
                {{ char }}
              </span>
            </h2>

            <p
              class="text-xl text-muted-foreground mb-4 max-w-2xl mx-auto animate-fade-in animate-slide-up"
              style="animation-delay: 0.2s"
            >
              {{ animatedMessage }}
            </p>

            <div
              class="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-card/50 border backdrop-blur-sm mb-12 animate-fade-in"
              style="animation-delay: 0.3s"
            >
              <Icon icon="tabler:terminal" class="h-4 w-4 text-primary" />
              <code class="text-sm text-muted-foreground font-mono">
                <span class="text-destructive">{{ displayedPath }}</span>
                <span class="animate-pulse ml-1">_</span>
              </code>
            </div>

            <div
              class="flex flex-wrap gap-4 justify-center animate-fade-in"
              style="animation-delay: 0.4s"
            >
              <router-link to="/">
                <SimpleButton size="lg" class="shadow-glow hover-scale group">
                  <Icon
                    icon="tabler:home"
                    class="mr-2 h-5 w-5 group-hover:rotate-12 transition-transform"
                  />
                  Back to Home
                </SimpleButton>
              </router-link>
              <SimpleButton
                size="lg"
                variant="outline"
                @click="$router.back()"
                class="hover-scale group"
              >
                <Icon
                  icon="tabler:arrow-left"
                  class="mr-2 h-5 w-5 group-hover:-rotate-12 transition-transform"
                />
                Go Back
              </SimpleButton>
            </div>
          </div>
        </div>
      </section>

      <section class="py-20 md:py-32 relative">
        <div class="absolute inset-0 opacity-5">
          <div class="circuit-pattern"></div>
        </div>

        <div class="container mx-auto px-4 relative z-10">
          <div class="max-w-3xl mx-auto">
            <div
              class="p-8 md:p-12 rounded-2xl bg-gradient-to-br from-card via-card to-card/50 border shadow-elegant backdrop-blur-sm"
            >
              <div class="flex items-center justify-center mb-6">
                <Icon icon="tabler:sparkles" class="h-8 w-8 text-primary animate-pulse" />
              </div>
              <h3
                class="text-2xl md:text-3xl font-semibold mb-4 text-center bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent"
              >
                Quick Navigation
              </h3>
              <p class="text-muted-foreground text-center mb-8">
                Don't worry! Let's get you back on track. Explore our main sections below.
              </p>
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <router-link
                  v-for="(nav, index) in quickNav"
                  :key="nav.name"
                  :to="nav.path"
                  class="group animate-fade-in"
                  :style="`animation-delay: ${0.5 + index * 0.1}s`"
                >
                  <div
                    class="p-6 rounded-xl bg-background/50 border hover:border-primary/50 transition-all hover-scale hover:shadow-glow relative overflow-hidden h-full"
                  >
                    <div
                      class="absolute inset-0 bg-gradient-to-r from-primary/10 to-accent/10 opacity-0 group-hover:opacity-100 transition-opacity duration-500"
                    />
                    <Icon
                      :icon="nav.icon"
                      class="h-6 w-6 mb-3 text-primary transition-all duration-300 relative z-10"
                      :class="nav.iconClass"
                    />
                    <h4 class="font-semibold mb-1 relative z-10">{{ nav.name }}</h4>
                    <p class="text-sm text-muted-foreground relative z-10">{{ nav.description }}</p>
                  </div>
                </router-link>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <footer class="border-t py-8">
      <div class="container mx-auto px-4 text-center text-sm text-muted-foreground">
        <p>© 2025 ByteForge. Built for C++ developers.</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SimpleButton from '../components/UI/buttons/SimpleButton.vue'
import NavBar from '../components/UI/navbars/NavBar.vue'

const route = useRoute()
const glitchText = ref('404')
const floatingIcons = ref<{ id: number; x: number; y: number; delay: number }[]>([])
const particles = ref<{ id: number; x: number; y: number; delay: number }[]>([])
const displayedPath = ref('')
const animatedMessage = ref('')
const currentMessageIndex = ref(0)

const titleText = 'Lost in the Code'
const messages = [
  "The page you're looking for doesn't exist or has been moved to another dimension.",
  'Looks like this code path leads to nowhere...',
  'This route seems to have encountered a segmentation fault.',
  'Error 404: Page not found in current memory space.',
]

const quickNav = [
  {
    name: 'Home',
    path: '/',
    icon: 'tabler:home',
    description: 'Start here',
    iconClass: 'group-hover:rotate-12',
  },
  {
    name: 'Projects',
    path: '/projects',
    icon: 'tabler:code-dots',
    description: 'Browse projects',
    iconClass: 'group-hover:-rotate-12',
  },
  {
    name: 'Profile',
    path: '/profile',
    icon: 'tabler:terminal-2',
    description: 'Your account',
    iconClass: 'group-hover:rotate-12',
  },
]

let glitchInterval: NodeJS.Timeout
let pathInterval: NodeJS.Timeout
let messageInterval: NodeJS.Timeout

const typePath = () => {
  const path = route.path
  displayedPath.value = ''
  let index = 0

  if (pathInterval) clearInterval(pathInterval)

  pathInterval = setInterval(() => {
    if (index < path.length) {
      displayedPath.value += path[index]
      index++
    } else {
      clearInterval(pathInterval)
    }
  }, 50)
}

const cycleMessages = () => {
  currentMessageIndex.value = 0
  animatedMessage.value = ''
  const message = messages[Math.floor(Math.random() * messages.length)]

  if (messageInterval) clearInterval(messageInterval)

  messageInterval = setInterval(() => {
    if (currentMessageIndex.value < message.length) {
      animatedMessage.value += message[currentMessageIndex.value]
      currentMessageIndex.value++
    } else {
      clearInterval(messageInterval)
    }
  }, 30)
}

onMounted(() => {
  console.error('404 Error: User attempted to access non-existent route:', route.path)

  glitchInterval = setInterval(() => {
    const variants = ['404', '4Ø4', '4０4', '4０4', '404', '４０４', '4o4', '4O4']
    glitchText.value = variants[Math.floor(Math.random() * variants.length)]
  }, 2000)

  const icons = Array.from({ length: 16 }, (_, i) => ({
    id: i,
    x: Math.random() * 100,
    y: Math.random() * 100,
    delay: Math.random() * 5,
  }))
  floatingIcons.value = icons

  const particleCount = 30
  particles.value = Array.from({ length: particleCount }, (_, i) => ({
    id: i,
    x: Math.random() * 100,
    y: Math.random() * 100,
    delay: Math.random() * 2,
  }))

  setTimeout(() => {
    typePath()
    cycleMessages()
  }, 1000)
})

onUnmounted(() => {
  if (glitchInterval) clearInterval(glitchInterval)
  if (pathInterval) clearInterval(pathInterval)
  if (messageInterval) clearInterval(messageInterval)
})

watch(
  () => route.path,
  () => {
    typePath()
    cycleMessages()
  },
)
</script>

<style scoped>
.hover-scale {
  transition: transform 0.2s ease-in-out;
}
.hover-scale:hover {
  transform: scale(1.05);
}

.shadow-glow {
  box-shadow: 0 0 20px hsl(262 83% 58% / 0.3);
}
.shadow-glow:hover {
  box-shadow: 0 0 30px hsl(262 83% 58% / 0.5);
}

.text-shadow-glow {
  text-shadow:
    0 0 40px hsl(262 83% 58% / 0.6),
    0 0 80px hsl(262 83% 58% / 0.4),
    0 0 120px hsl(262 83% 58% / 0.2);
}

.shadow-elegant {
  box-shadow: 0 10px 40px -10px rgba(0, 0, 0, 0.1);
}

.grid-pattern {
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  background-position: center center;
}

.circuit-pattern {
  background-image:
    radial-gradient(circle at 25% 25%, rgba(139, 92, 246, 0.1) 2px, transparent 2px),
    radial-gradient(circle at 75% 75%, rgba(99, 102, 241, 0.1) 2px, transparent 2px);
  background-size: 100px 100px;
}

@keyframes glitch-1 {
  0%,
  100% {
    transform: translateX(0);
    opacity: 1;
  }
  10% {
    transform: translateX(-2px) translateY(-1px);
    opacity: 0.8;
  }
  20% {
    transform: translateX(2px) translateY(1px);
    opacity: 0.6;
  }
  30% {
    transform: translateX(-1px);
    opacity: 0.9;
  }
  40% {
    transform: translateX(1px);
    opacity: 0.7;
  }
  50% {
    transform: translateX(0);
    opacity: 1;
  }
  60% {
    transform: translateX(-3px) translateY(2px);
    opacity: 0.5;
  }
  70% {
    transform: translateX(3px) translateY(-2px);
    opacity: 0.8;
  }
  80% {
    transform: translateX(-2px);
    opacity: 0.6;
  }
  90% {
    transform: translateX(2px);
    opacity: 0.9;
  }
}

@keyframes glitch-2 {
  0%,
  100% {
    transform: translateX(0);
    opacity: 0.3;
  }
  15% {
    transform: translateX(3px) translateY(-1px);
    opacity: 0.2;
  }
  25% {
    transform: translateX(-3px) translateY(1px);
    opacity: 0.4;
  }
  35% {
    transform: translateX(2px);
    opacity: 0.1;
  }
  45% {
    transform: translateX(-2px);
    opacity: 0.5;
  }
  55% {
    transform: translateX(0);
    opacity: 0.3;
  }
  65% {
    transform: translateX(4px) translateY(1px);
    opacity: 0.2;
  }
  75% {
    transform: translateX(-4px) translateY(-1px);
    opacity: 0.4;
  }
  85% {
    transform: translateX(1px);
    opacity: 0.1;
  }
  95% {
    transform: translateX(-1px);
    opacity: 0.5;
  }
}

.animate-glitch-1 {
  animation: glitch-1 0.3s ease-in-out infinite;
  mix-blend-mode: overlay;
}

.animate-glitch-2 {
  animation: glitch-2 0.4s ease-in-out infinite;
  mix-blend-mode: overlay;
}

@keyframes scale-in {
  0% {
    transform: scale(0.8);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.animate-scale-in {
  animation: scale-in 0.6s ease-out;
}

@keyframes orbit-1 {
  0% {
    transform: rotate(0deg) translateX(20px) rotate(0deg);
  }
  100% {
    transform: rotate(360deg) translateX(20px) rotate(-360deg);
  }
}

@keyframes orbit-2 {
  0% {
    transform: rotate(0deg) translateX(15px) rotate(0deg);
  }
  100% {
    transform: rotate(360deg) translateX(15px) rotate(-360deg);
  }
}

@keyframes orbit-3 {
  0% {
    transform: rotate(0deg) translateX(25px) rotate(0deg);
  }
  100% {
    transform: rotate(360deg) translateX(25px) rotate(-360deg);
  }
}

@keyframes orbit-4 {
  0% {
    transform: rotate(0deg) translateX(18px) rotate(0deg);
  }
  100% {
    transform: rotate(360deg) translateX(18px) rotate(-360deg);
  }
}

.animate-orbit-1 {
  animation: orbit-1 3s linear infinite;
}

.animate-orbit-2 {
  animation: orbit-2 4s linear infinite;
}

.animate-orbit-3 {
  animation: orbit-3 5s linear infinite;
}

.animate-orbit-4 {
  animation: orbit-4 3.5s linear infinite;
}

.animate-gradient-shift {
  background-size: 200% 200%;
  animation: gradientShift 8s ease infinite;
}

@keyframes gradientShift {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}
</style>
