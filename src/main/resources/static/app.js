// --------------------------------------------------
// DOM References
// --------------------------------------------------

// Main controls
const runBtn = document.getElementById("runBtn");
const algoEl = document.getElementById("algo");
const fileEl = document.getElementById("file");
// Side-panel text output areas
const inputPrintEl = document.getElementById("inputPrint");
const outputPrintEl = document.getElementById("outputPrint");

// Canvas setup
const canvas = document.getElementById("canvas");
const ctx = canvas.getContext("2d");

// Stores the most recently uploaded input text so it can be reused
// when drawing/animating BFS, DFS, and Hull results
let lastInputText = "";


// --------------------------------------------------
// Canvas Sizing / High-DPI Support
// --------------------------------------------------

// CSS display size of the canvas
const CSS_SIZE = 500;
// Device pixel ratio (used so canvas looks sharp on retina/high-DPI screens)
const dpr = window.devicePixelRatio || 1;

// Set visible canvas size in CSS pixels
canvas.style.width = CSS_SIZE + "px";
canvas.style.height = CSS_SIZE + "px";

// Set actual drawing resolution in device pixels
canvas.width = Math.round(CSS_SIZE * dpr);
canvas.height = Math.round(CSS_SIZE * dpr);

// Draw using CSS pixel coordinates even though backing resolution is scaled
ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

//Canvas drawing constants
const W = CSS_SIZE;
const H = CSS_SIZE;
const PAD = 20; //padding

// Shared timer used for BFS / DFS / Hull animations
let animationTimer = null;

// --------------------------------------------------
// Animation Control
// --------------------------------------------------

/**
 * Stops any currently running animation timer.
 * Reused by BFS, DFS, and Convex Hull animations.
 */
function stopAnimation() {
  if (animationTimer !== null) {
    clearInterval(animationTimer);
    animationTimer = null;
  }
}

// --------------------------------------------------
// Main Run Button Handler
// --------------------------------------------------

/**
 * Sends the selected file + algorithm to the backend,
 * receives the solver output, and dispatches to the proper renderer.
 */
runBtn.addEventListener("click", async (e) => {
  e.preventDefault();
  const file = fileEl.files[0];
  if (!file) {
    outputPrintEl.textContent = "Pick an input file first.";
    return;
  }

  const form = new FormData();
  form.append("algorithm", algoEl.value);
  form.append("file", file);

  // Send the uploaded file and algorithm choice to the backend /run endpoint
  const res = await fetch("/run", { method: "POST", body: form });

let data;

// Try to parse JSON response safely
try {
  data = await res.json();
} catch (err) {
  const txt = await res.text();
  outputPrintEl.textContent = "Server did not return JSON:\n" + txt;
  console.error("Non-JSON response:", txt);
  return;
}

// Handle backend error responses
if (!res.ok || data.error) {
  outputPrintEl.textContent = "Server error:\n" + (data.error || ("HTTP " + res.status));
  console.error("Run error:", data);
  return;
}

  // ----------------------------
  // Skyline Output + Drawing
  // ----------------------------
  if (algoEl.value === "SKYLINE") {
    // Convert normalized skyline points back into raw coordinate values for display
    const raw = denormalizeSkyline(data);
    outputPrintEl.textContent = raw.map(pt => `${pt.x} ${pt.y}`).join("\n");
    drawSkyline(data);
  }

  // ----------------------------
  // BFS / DFS Output + Animation
  // ----------------------------
  if (algoEl.value === "BFS" || algoEl.value === "DFS") {
  stopAnimation(); // stop any prior animation

  const edges = data.edges || [];
  const orderText = (data.order || []).join(" ");
  outputPrintEl.textContent =
    `Order: ${orderText}\n\n` +
    edges.map(e => `${e[0]}, ${e[1]}`).join("\n");

  const graph = parseAdjMatrix(lastInputText);

  // draw step 0 (no BFS edges yet, just the graph)
  let k = 0;
  drawGraphTraversalStep(graph, data, k);

  // animate: reveal one edge every 400ms
  animationTimer = setInterval(() => {
    k++;
    drawGraphTraversalStep(graph, data, k);

    if (k >= edges.length) {
      stopAnimation();
    }
  }, 400);

  return;
}

// ----------------------------
// Convex Hull Output + Animation
// ----------------------------
if (algoEl.value === "HULL") {
  stopAnimation(); //stop any existing animation

  // Print raw hull coordinates in the output panel
  const hullRaw = data.hullRaw || [];
  outputPrintEl.textContent = hullRaw.map(p => `${p.x} ${p.y}`).join("\n");

  animateHull(data);
  return;
}


});

// --------------------------------------------------
// Canvas Utilities
// --------------------------------------------------

/**
 * Clears the canvas and redraws the background, grid, and border.
 */
function clearCanvas() {
  ctx.clearRect(0, 0, W, H);

  // background
  ctx.fillStyle = "#fff";
  ctx.fillRect(0, 0, W, H);

  // subtle grid
  ctx.lineWidth = 1;
  ctx.strokeStyle = "rgba(0,0,0,0.06)";
  for (let x = 0; x <= W; x += 25) {
    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke();
  }
  for (let y = 0; y <= H; y += 25) {
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke();
  }

  // border
  ctx.strokeStyle = "rgba(0,0,0,0.35)";
  ctx.lineWidth = 2;
  ctx.strokeRect(0, 0, W, H);
}

// --------------------------------------------------
// Skyline Input Parsing + Input Drawing
// --------------------------------------------------

/**
 * Parses building data from input text.
 * Expected format per building line:
 *   L H R
 * or
 *   L, H, R
 *
 * Ignores:
 * - blank lines
 * - comment lines starting with '#'
 * - single-number count lines
 */
function parseBuildings(text) {
  const lines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l && !l.startsWith("#"));
  const buildings = [];

  for (const line of lines) {
    const parts = line.split(/[, \t]+/).filter(Boolean);

    // ignore count line like "10"
    if (parts.length === 1) continue;
    if (parts.length < 3) continue;

    // Your format: L, H, R
    const L = parseInt(parts[0], 10);
    const HH = parseInt(parts[1], 10);
    const R = parseInt(parts[2], 10);

    if (Number.isFinite(L) && Number.isFinite(R) && Number.isFinite(HH) && L < R && HH > 0) {
      buildings.push({ L, R, H: HH });
    }
  }
  return buildings;
}

/**
 * Draws the raw input buildings as rectangles on the canvas.
 */
function drawBuildings(buildings) {
  clearCanvas();
  if (!buildings.length) return;

  const minX = Math.min(...buildings.map(b => b.L));
  const maxX = Math.max(...buildings.map(b => b.R));
  const maxY = Math.max(...buildings.map(b => b.H));

  const dx = Math.max(1, maxX - minX);
  const dy = Math.max(1, maxY);

  ctx.strokeStyle = "#000";
  ctx.lineWidth = 1;

  for (const b of buildings) {
    const x = PAD + ((b.L - minX) / dx) * (W - 2 * PAD);
    const w = ((b.R - b.L) / dx) * (W - 2 * PAD);
    const h = (b.H / dy) * (H - 2 * PAD);
    const y = (H) - h;


    ctx.strokeRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
  }
}

// --------------------------------------------------
// File Selection Handler
// --------------------------------------------------

/**
 * When the user selects a file:
 * - read and print the input text
 * - clear old output
 * - immediately preview the raw input visualization for the selected algorithm
 */
fileEl.addEventListener("change", async () => {
  const file = fileEl.files[0];
  stopAnimation();

  if (!file) return;

  const text = await file.text();
  lastInputText = text;

  // Show cleaned input text in the side panel
  inputPrintEl.textContent = text
    .split(/\r?\n/)
    .map(l => l.trim())
    .filter(l => l.length > 0)
    .join("\n");

  outputPrintEl.textContent = "";

  if (algoEl.value === "SKYLINE") {
    const buildings = parseBuildings(text);
    drawBuildings(buildings);
  } else if (algoEl.value === "BFS" || algoEl.value === "DFS") {
  const graph = parseAdjMatrix(text);
  //drawGraphInput(graph);
  } else if (algoEl.value === "HULL") {

  const pts = parsePoints(text);
  drawPointsInput(pts);
}

});

// --------------------------------------------------
// Skyline Output Drawing
// --------------------------------------------------

/**
 * Draws the skyline polyline returned by the backend.
 * Input points are normalized to [0,1] space.
 */
function drawSkyline(data) {
  clearCanvas();

  ctx.lineWidth = 2;

  const pts = data.points || [];
  if (pts.length === 0) return;

  ctx.beginPath();
  ctx.strokeStyle = "#000";

  // Start at the baseline under the first x-coordinate, then go up to the first height
  let x0 = PAD + pts[0].x * (W - 2 * PAD);
  let y0 = (H) - pts[0].y * (H - 2 * PAD);
 

  ctx.moveTo(x0, H);   // baseline
  ctx.lineTo(x0, y0);  // rise to first skyline height

  for (let i = 1; i < pts.length; i++) {
    const x1 = PAD + pts[i].x * (W - 2 * PAD);
    const y1 = (H) - pts[i].y * (H - 2 * PAD);

    // horizontal to next x at current height
    ctx.lineTo(x1, y0);

    // vertical to next height
    ctx.lineTo(x1, y1);

    x0 = x1;
    y0 = y1;
  }

  ctx.stroke();
}

/**
 * Converts normalized skyline points back into raw coordinate values
 * so they can be displayed in the output panel.
 */
function denormalizeSkyline(data) {
  const pts = data.points || [];
  const meta = data.meta || {};
  const minX = meta.minX ?? 0;
  const maxX = meta.maxX ?? 0;
  const maxY = meta.maxY ?? 0;

  const dx = Math.max(1, maxX - minX);

  return pts.map(p => {
    const x = Math.round(minX + p.x * dx);
    const y = Math.round(p.y * maxY);
    return { x, y };
  });
}

// --------------------------------------------------
// Graph Parsing + Layout Helpers
// --------------------------------------------------

/**
 * Parses an adjacency matrix from text input.
 *
 * Expected format:
 *   Line 1: n
 *   Next n lines: adjacency matrix rows
 */
function parseAdjMatrix(text) {
  const lines = text
    .split(/\r?\n/)
    .map(l => l.trim())
    .filter(l => l.length > 0 && !l.startsWith("#"));

  const n = parseInt(lines[0], 10);
  const adj = [];

  for (let i = 0; i < n; i++) {
    const row = lines[i + 1].split(/[, \t]+/).filter(Boolean).map(Number);
    adj.push(row);
  }
  return { n, adj };
}

/**
 * Places graph vertices evenly around a circle for graph visualization.
 */
function layoutCircle(n) {
  const left = PAD;
  const right = PAD;
  const top = PAD;
  const bottom = 0;

  const cx = (left + (W - right)) / 2;
  const cy = (top + (H - bottom)) / 2;
  const radius = 0.38 * Math.min(W - left - right, H - top - bottom);

  const pos = [];
  for (let i = 0; i < n; i++) {
    const ang = (2 * Math.PI * i) / n - Math.PI / 2;
    pos.push({
      x: cx + radius * Math.cos(ang),
      y: cy + radius * Math.sin(ang),
    });
  }
  return pos;
}

// --------------------------------------------------
// BFS / DFS Traversal Drawing
// --------------------------------------------------

/**
 * Draws one animation frame of a BFS or DFS traversal.
 *
 * k = number of traversal edges currently revealed.
 */
function drawGraphTraversalStep(graph, data, k) {
  clearCanvas();

  const { n, adj } = graph;
  const pos = layoutCircle(n);
  

  // draw all edges lightly
  ctx.lineWidth = 1;
  ctx.strokeStyle = "#bbb";
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      if (adj[i][j] !== 0 || adj[j][i] !== 0) {
        ctx.beginPath();
        ctx.moveTo(pos[i].x, pos[i].y);
        ctx.lineTo(pos[j].x, pos[j].y);
        ctx.stroke();
      }
    }
  }

  // draw traversal tree edges thick (first k edges)
  const edges = data.edges || [];
  ctx.lineWidth = 3;
  ctx.strokeStyle = "#000";
  const upto = Math.min(k, edges.length);

  for (let i = 0; i < upto; i++) {
    const [u, v] = edges[i];
    ctx.beginPath();
    ctx.moveTo(pos[u].x, pos[u].y);
    ctx.lineTo(pos[v].x, pos[v].y);
    ctx.stroke();
  }

  // Determine which nodes have been visited so far
  const order = data.order || [];
  const shownNodes = new Set(order.slice(0, Math.min(order.length, k + 1)));

  // Current node being emphasize
  const current = order[Math.min(k, order.length - 1)];
  
  //Draw graph nodes
  for (let i = 0; i < n; i++) {
    ctx.beginPath();
    ctx.arc(pos[i].x, pos[i].y, 12, 0, Math.PI * 2);

    // Fill visited nodes lightly
    if (shownNodes.has(i)) {
      ctx.fillStyle = "rgba(0,0,0,0.08)";
      ctx.fill();
    }

    // Thicker border on current node
    ctx.lineWidth = (i === current) ? 4 : 2;
    ctx.strokeStyle = "#000";
    ctx.stroke();

    ctx.font = "14px system-ui";
    ctx.fillStyle = "#000";
    ctx.fillText(String(i), pos[i].x - 4, pos[i].y + 5);
  }

  // Draw discovery rank labels (#0, #1, #2, ...)
  const orderr = data.order || [];
  const shown = Math.min(orderr.length, k + 1);
  ctx.font = "12px system-ui";
  for (let idx = 0; idx < shown; idx++) {
    const node = orderr[idx];
    ctx.fillText(`#${idx}`, pos[node].x + 14, pos[node].y - 10);
  }
}
/* If you want to draw bfs instantly without animation, use this:
function drawBfsOutput(graph, bfsData) {
  clearCanvas();
  const { n, adj } = graph;
  const pos = layoutCircle(n);

  // draw all edges lightly (optional)
  ctx.lineWidth = 1;
  ctx.strokeStyle = "#bbb";
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      if (adj[i][j] !== 0 || adj[j][i] !== 0) {
        ctx.beginPath();
        ctx.moveTo(pos[i].x, pos[i].y);
        ctx.lineTo(pos[j].x, pos[j].y);
        ctx.stroke();
      }
    }
  }

  // draw BFS tree edges thick
  ctx.lineWidth = 3;
  ctx.strokeStyle = "#000";
  const edges = bfsData.edges || [];
  for (const [u, v] of edges) {
    ctx.beginPath();
    ctx.moveTo(pos[u].x, pos[u].y);
    ctx.lineTo(pos[v].x, pos[v].y);
    ctx.stroke();
  }

  // nodes
  ctx.lineWidth = 2;
  ctx.strokeStyle = "#000";
  for (let i = 0; i < n; i++) {
    ctx.beginPath();
    ctx.arc(pos[i].x, pos[i].y, 12, 0, Math.PI * 2);
    ctx.stroke();

    ctx.font = "14px system-ui";
    ctx.fillStyle = "#000";
    ctx.fillText(String(i), pos[i].x - 4, pos[i].y + 5);
  }
  

  // show discovery order number near each node
  const order = bfsData.order || [];
  const rank = new Map();
  order.forEach((node, idx) => rank.set(node, idx));

  ctx.font = "12px system-ui";
  for (let i = 0; i < n; i++) {
    if (rank.has(i)) {
      ctx.fillText(`#${rank.get(i)}`, pos[i].x + 14, pos[i].y - 10);
    }
  }
}
 */

// --------------------------------------------------
// Convex Hull Input Parsing + Input Drawing
// --------------------------------------------------

/**
 * Parses raw (x, y) point coordinates from text input.
 * Accepts comma and/or whitespace separated values.
 */
function parsePoints(text) {
  const lines = text
    .split(/\r?\n/)
    .map(l => l.trim())
    .filter(l => l.length > 0 && !l.startsWith("#"));

  const pts = [];
  for (const line of lines) {
    const parts = line.split(/[, \t]+/).filter(Boolean);

    if (parts.length === 1) continue; // ignore count
    if (parts.length < 2) continue;

    const x = parseFloat(parts[0]);
    const y = parseFloat(parts[1]);
    if (Number.isFinite(x) && Number.isFinite(y)) pts.push({ x, y });
  }
  return pts;
}

/**
 * Draws the raw input points before convex hull processing.
 */
function drawPointsInput(pts) {
  clearCanvas();
  if (!pts.length) return;

  // scale to canvas with padding
  const minX = Math.min(...pts.map(p => p.x));
  const maxX = Math.max(...pts.map(p => p.x));
  const minY = Math.min(...pts.map(p => p.y));
  const maxY = Math.max(...pts.map(p => p.y));

  const dx = Math.max(1, maxX - minX);
  const dy = Math.max(1, maxY - minY);

  ctx.fillStyle = "#000";
  for (const p of pts) {
    const x = PAD + ((p.x - minX) / dx) * (W - PAD - PAD);
    const y = (H - PAD) - ((p.y - minY) / dy) * (H - PAD - PAD);
    ctx.beginPath();
    ctx.arc(x, y, 3, 0, Math.PI * 2);
    ctx.fill();
  }
}

// --------------------------------------------------
// Convex Hull Animation
// --------------------------------------------------

/**
 * Animates the Monotonic Chain convex hull construction
 * using the recorded backend step list.
 */
function animateHull(data) {
  stopAnimation();

  const pts = data.inputPoints || [];
  const steps = data.steps || [];
  //const hull = data.hullPoints || [];
  if (!pts.length) return;

  /**
   * Converts normalized point coordinates into canvas coordinates.
   */
  function toCanvas(p) {
    return {
      x: PAD + p.x * (W - PAD - PAD),
      y: (H - PAD) - p.y * (H - PAD - PAD),
    };
  }

  /**
   * Draws all original input points.
   */
  function drawAllPoints() {
    ctx.fillStyle = "#000";
    for (const p of pts) {
      const q = toCanvas(p);
      ctx.beginPath();
      ctx.arc(q.x, q.y, 3, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  /**
   * Draws the final lower and upper hull stacks after the animation finishes.
   */
  function drawFinalHull() {
    const lowerFinal =
      [...steps].reverse().find(st => st.phase === "lower" && st.stack && st.stack.length >= 2)?.stack || [];
    const upperFinal =
      [...steps].reverse().find(st => st.phase === "upper" && st.stack && st.stack.length >= 2)?.stack || [];

    function drawStack(stack, phase) {
      if (!stack || stack.length < 2) return;
      ctx.strokeStyle = hullColor(phase);
      ctx.lineWidth = 3;
      ctx.beginPath();
      const first = toCanvas(stack[0]);
      ctx.moveTo(first.x, first.y);
      for (let k = 1; k < stack.length; k++) {
        const q = toCanvas(stack[k]);
        ctx.lineTo(q.x, q.y);
      }
      ctx.stroke();
    }

    drawStack(lowerFinal, "lower");
    drawStack(upperFinal, "upper");
  }

  /**
   * Returns the color used for each hull-building phase.
   * - lower hull = blue
   * - upper hull = green
   */
  function hullColor(phase) {
    return phase === "upper" ? "#00aa00" : "#0000cc"; 
  }

  let i = 0;

  // If all steps have been shown, draw final hull and stop animation
  animationTimer = setInterval(() => {
    clearCanvas();
    drawAllPoints();

    if (i >= steps.length) {
      drawFinalHull();
      outputPrintEl.textContent =
        `Hull complete\n` +
        (data.hullRaw ? data.hullRaw.map(p => `${p.x} ${p.y}`).join("\n") : "");
      stopAnimation();
      return;
    }

    const s = steps[i];
    const stack = s.stack || [];

    // Draw all previously completed hull segments
    if (i > 0) {
      for (let j = 0; j < i; j++) {
        const prevStep = steps[j];
        if (prevStep.stack && prevStep.stack.length >= 2) {
          ctx.strokeStyle = hullColor(prevStep.phase);
          ctx.lineWidth = 2;
          ctx.beginPath();
          const first = toCanvas(prevStep.stack[0]);
          ctx.moveTo(first.x, first.y);
          for (let k = 1; k < prevStep.stack.length; k++) {
            const q = toCanvas(prevStep.stack[k]);
            ctx.lineTo(q.x, q.y);
          }
          ctx.stroke();
        }
      }
    }

    // Draw current stack for the current step
    if (stack.length >= 2) {
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.strokeStyle = hullColor(s.phase);
      const first = toCanvas(stack[0]);
      ctx.moveTo(first.x, first.y);
      
      for (let k = 1; k < stack.length; k++) {
        const currentPoint = stack[k];
        
        const q = toCanvas(currentPoint);
        ctx.lineTo(q.x, q.y);
        ctx.stroke();
        
         // Start a new segment if there are more points left in the current stack
        if (k < stack.length - 1) {
          ctx.beginPath();
          ctx.moveTo(q.x, q.y);
        }
      }
    }

    // highlight current candidate point
    if (s.candidate) {
      const c = toCanvas(s.candidate);
      ctx.beginPath();
      ctx.arc(c.x, c.y, 6, 0, Math.PI * 2);
      ctx.strokeStyle = "#ff6600";
      ctx.lineWidth = 3;
      ctx.stroke();
    }

    outputPrintEl.textContent =
      `Step ${i + 1}/${steps.length}\n` +
      `${s.phase.toUpperCase()} | ${s.action.toUpperCase()} | cross=${s.cross}\n\n` +
      (data.hullRaw ? data.hullRaw.map(p => `${p.x} ${p.y}`).join("\n") : "");

    i++;
  }, 200);
}
