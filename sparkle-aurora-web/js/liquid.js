/* Optical refraction, inspired by the MIT-licensed rdev/liquid-glass-react.
   This dependency-free layer keeps GitHub Pages directly previewable. */
(() => {
  'use strict';
  function displacementMap(w = 260, h = 180) {
    const canvas = document.createElement('canvas');
    canvas.width = w; canvas.height = h;
    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    if (!ctx) return '';
    const image = ctx.createImageData(w, h);
    for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
      const nx = (x / (w - 1)) * 2 - 1;
      const ny = (y / (h - 1)) * 2 - 1;
      const px = Math.abs(nx) ** 3.2;
      const py = Math.abs(ny) ** 3.2;
      // Normalize outward-facing normals, with flat center and energetic rim.
      const intensity = Math.min(1, px + py);
      const normalX = Math.sign(nx) * intensity * 0.47;
      const normalY = Math.sign(ny) * intensity * 0.47;
      const i = (y * w + x) * 4;
      image.data[i] = Math.round(128 + normalX * 255);
      image.data[i + 1] = 128;
      image.data[i + 2] = Math.round(128 + normalY * 255);
      image.data[i + 3] = 255;
    }
    ctx.putImageData(image, 0, 0);
    return canvas.toDataURL('image/png');
  }
  const NS = 'http://www.w3.org/2000/svg';
  const mk = (name, attributes) => { const e = document.createElementNS(NS, name); for(const [k,v] of Object.entries(attributes))e.setAttribute(k, v);return e; };
  function createFilter() {
    const svg = mk('svg', {width:'0',height:'0','aria-hidden':'true',style:'position:absolute;pointer-events:none;overflow:hidden'});
    const defs = mk('defs',{});
    const filter = mk('filter',{id:'sparkle-glass-distortion',x:'-15%',y:'-15%',width:'130%',height:'130%',colorInterpolationFilters:'sRGB'});
    const map = mk('feImage', {href: displacementMap(),x:'0',y:'0',width:'100%',height:'100%',preserveAspectRatio:'none',result:'MAP'});
    const distortion = mk('feDisplacementMap', {in:'SourceGraphic',in2:'MAP',scale:'36',xChannelSelector:'R',yChannelSelector:'B'});
    filter.append(map, distortion);defs.append(filter);svg.append(defs);document.body.append(svg);
    return distortion;
  }
  const strength = document.getElementById('glassStrength');
  const value = document.getElementById('glassValue');
  const scene = document.getElementById('glassPlayground');
  const sceneToggle = document.getElementById('glassSceneToggle');
  if (!scene || !strength) return;
  const refraction = createFilter();
  const isChromium = /Chrome|Chromium|Edg\//.test(navigator.userAgent) && !/OPR\//.test(navigator.userAgent);
  if (isChromium && CSS.supports('backdrop-filter', 'blur(1px)')) document.documentElement.classList.add('optical-capable');
  strength.addEventListener('input', () => {const scale = Math.max(12,Math.min(65, Number(strength.value)));refraction.setAttribute('scale', String(scale));value.textContent=String(scale);});
  sceneToggle?.addEventListener('click', () => {scene.classList.toggle('scene-alt');sceneToggle.setAttribute('aria-pressed',String(scene.classList.contains('scene-alt')));});
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const opticsTargets = document.querySelectorAll('.glass-floating,.nav-shell,.hero-buttons .button,.header-cta,.search-all');
  opticsTargets.forEach(surface => {
    surface.classList.add('liquid-surface');
    const layer = document.createElement('span');
    layer.className='liquid-optics';layer.setAttribute('aria-hidden','true');
    surface.insertBefore(layer,surface.firstChild);
    if (!reducedMotion.matches && matchMedia('(pointer:fine)').matches) {
      let pending = 0;
      surface.addEventListener('pointermove', event => {
        if(pending) return;
        const rect = surface.getBoundingClientRect();
        const x = (event.clientX-rect.left)/rect.width*100;
        const y = (event.clientY-rect.top)/rect.height*100;
        pending = requestAnimationFrame(() => { surface.style.setProperty('--glass-pointer-x',x.toFixed(1)+'%');surface.style.setProperty('--glass-pointer-y',y.toFixed(1)+'%');pending=0; });
      },{passive:true});
      surface.addEventListener('pointerleave',()=>{surface.style.removeProperty('--glass-pointer-x');surface.style.removeProperty('--glass-pointer-y')});
    }
  });
})();