<template>
  <div ref="containerRef" class="shader-container" />
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'

export default defineComponent({
  name: 'ShaderAnimation',
  setup() {
    const containerRef = ref<HTMLDivElement | null>(null)

    let camera: THREE.Camera
    let scene: THREE.Scene
    let renderer: THREE.WebGLRenderer
    let uniforms: { time: { value: number }; resolution: { value: THREE.Vector2 } }
    let animationId = 0

    const vertexShader = `
      void main() {
        gl_Position = vec4( position, 1.0 );
      }
    `

    const fragmentShader = `
      #define TWO_PI 6.2831853072
      #define PI 3.14159265359

      precision highp float;
      uniform vec2 resolution;
      uniform float time;

      void main(void) {
        vec2 uv = (gl_FragCoord.xy * 2.0 - resolution.xy) / min(resolution.x, resolution.y);
        float t = time*0.05;
        float lineWidth = 0.002;

        vec3 color = vec3(0.0);
        for(int j = 0; j < 3; j++){
          for(int i=0; i < 5; i++){
            color[j] += lineWidth*float(i*i) / abs(fract(t - 0.01*float(j)+float(i)*0.01)*5.0 - length(uv) + mod(uv.x+uv.y, 0.2));
          }
        }

        gl_FragColor = vec4(color[0],color[1],color[2],1.0);
      }
    `

    let geometry: THREE.PlaneGeometry
    let material: THREE.ShaderMaterial

    const onWindowResize = () => {
      if (!containerRef.value || !renderer) return
      const width = containerRef.value.clientWidth
      const height = containerRef.value.clientHeight
      renderer.setSize(width, height)
      uniforms.resolution.value.x = renderer.domElement.width
      uniforms.resolution.value.y = renderer.domElement.height
    }

    const animate = () => {
      animationId = requestAnimationFrame(animate)
      uniforms.time.value += 0.05
      renderer.render(scene, camera)
    }

    onMounted(() => {
      if (!containerRef.value) return

      camera = new THREE.Camera()
      camera.position.z = 1

      scene = new THREE.Scene()
      geometry = new THREE.PlaneGeometry(2, 2)

      uniforms = {
        time: { value: 1.0 },
        resolution: { value: new THREE.Vector2() },
      }

      material = new THREE.ShaderMaterial({
        uniforms,
        vertexShader,
        fragmentShader,
      })

      const mesh = new THREE.Mesh(geometry, material)
      scene.add(mesh)

      renderer = new THREE.WebGLRenderer({ antialias: true })
      renderer.setPixelRatio(window.devicePixelRatio)

      containerRef.value.appendChild(renderer.domElement)

      onWindowResize()
      window.addEventListener('resize', onWindowResize, false)

      animate()
    })

    onUnmounted(() => {
      window.removeEventListener('resize', onWindowResize)
      cancelAnimationFrame(animationId)
      if (renderer) {
        renderer.dispose()
      }
      if (geometry) {
        geometry.dispose()
      }
      if (material) {
        material.dispose()
      }
    })

    return { containerRef }
  },
})
</script>

<style scoped>
.shader-container {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
</style>
