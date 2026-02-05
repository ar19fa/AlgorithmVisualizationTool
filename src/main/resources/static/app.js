const runBtn = document.getElementById("runBtn");
const algoEl = document.getElementById("algo");
const fileEl = document.getElementById("file");
const inputPrintEl = document.getElementById("inputPrint");
const outputPrintEl = document.getElementById("outputPrint");

const canvas = document.getElementById("canvas");
const ctx = canvas.getContext("2d");
let lastInputText = "";


const CSS_SIZE = 500;
const dpr = window.devicePixelRatio || 1;

canvas.style.width = CSS_SIZE + "px";
canvas.style.height = CSS_SIZE + "px";
canvas.width = Math.round(CSS_SIZE * dpr);
canvas.height = Math.round(CSS_SIZE * dpr);

// draw using CSS pixel coordinates
ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

const W = CSS_SIZE;
const H = CSS_SIZE;
const PAD = 20; //padding
let bfsTimer = null;

function stopBfsAnimation() {
  if (bfsTimer !== null) {
    clearInterval(bfsTimer);
    bfsTimer = null;
  }
}

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

  //const res = await fetch("/run", { method: "POST", body: form });
  //const data = await res.json();
  const res = await fetch("/run", { method: "POST", body: form });

let data;
try {
  data = await res.json();
} catch (err) {
  const txt = await res.text();
  outputPrintEl.textContent = "Server did not return JSON:\n" + txt;
  console.error("Non-JSON response:", txt);
  return;
}

if (!res.ok || data.error) {
  outputPrintEl.textContent = "Server error:\n" + (data.error || ("HTTP " + res.status));
  console.error("Run error:", data);
  return;
}


  if (algoEl.value === "SKYLINE") {
    // print denormalized skyline values (like you already did)
    const raw = denormalizeSkyline(data);
    outputPrintEl.textContent = raw.map(pt => `${pt.x} ${pt.y}`).join("\n");
    drawSkyline(data);
  }

  if (algoEl.value === "BFS") {
  stopBfsAnimation(); // stop any prior animation

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
  bfsTimer = setInterval(() => {
    k++;
    drawGraphTraversalStep(graph, data, k);

    if (k >= edges.length) {
      stopBfsAnimation();
    }
  }, 400);

  return;
}
if (algoEl.value === "DFS") {
  stopBfsAnimation(); // you can rename this to stopTraversalAnimation later

  const edges = data.edges || [];
  const orderText = (data.order || []).join(" ");

  outputPrintEl.textContent =
    `Order: ${orderText}\n\n` +
    edges.map(e => `${e[0]}, ${e[1]}`).join("\n");

  const graph = parseAdjMatrix(lastInputText);

  let k = 0;
  drawGraphTraversalStep(graph, data, k);

  bfsTimer = setInterval(() => {
    k++;
    drawGraphTraversalStep(graph, data, k);
    if (k >= edges.length) stopBfsAnimation();
  }, 400);

  return;
}
if (algoEl.value === "HULL") {
  stopBfsAnimation(); // reuse same timer-stopper pattern (rename later if you want)

  // print hull raw values nicely
  const hullRaw = data.hullRaw || [];
  outputPrintEl.textContent = hullRaw.map(p => `${p.x} ${p.y}`).join("\n");

  animateHull(data);
  return;
}


});


function clearCanvas() {
  ctx.clearRect(0, 0, W, H);
  ctx.fillStyle = "#fff";
  ctx.fillRect(0, 0, W, H);
  ctx.strokeStyle = "#000";
  ctx.strokeRect(0, 0, W, H);
}

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
// When user selects an input file: draw the INPUT buildings immediately
fileEl.addEventListener("change", async () => {
  const file = fileEl.files[0];
  stopBfsAnimation();

  if (!file) return;

  const text = await file.text();
  lastInputText = text;

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
  drawGraphInput(graph);
  } else if (algoEl.value === "HULL") {

  const pts = parsePoints(text);
  drawPointsInput(pts);
}

});




function drawSkyline(data) {
  clearCanvas();

  ctx.lineWidth = 2;

  const pts = data.points || [];
  if (pts.length === 0) return;

  ctx.beginPath();
  ctx.strokeStyle = "#000";

  // Start at baseline under first x, then go up plus padding
  let x0 = PAD + pts[0].x * (W - 2 * PAD);
  let y0 = (H) - pts[0].y * (H - 2 * PAD);
 

  ctx.moveTo(x0, H);   // baseline
  ctx.lineTo(x0, y0);  // up to first height

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

  // draw nodes
  ctx.lineWidth = 2;
  ctx.strokeStyle = "#000";
  ctx.font = "14px system-ui";
  ctx.fillStyle = "#000";

  for (let i = 0; i < n; i++) {
    ctx.beginPath();
    ctx.arc(pos[i].x, pos[i].y, 12, 0, Math.PI * 2);
    ctx.stroke();
    ctx.fillText(String(i), pos[i].x - 4, pos[i].y + 5);
  }

  // discovery rank labels (optional)
  const order = data.order || [];
  const shown = Math.min(order.length, k + 1);
  ctx.font = "12px system-ui";
  for (let idx = 0; idx < shown; idx++) {
    const node = order[idx];
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
// Convex Hull

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
function animateHull(data) {
  stopBfsAnimation();

  const pts = data.inputPoints || [];
  const steps = data.steps || [];
  const hull = data.hullPoints || [];
  if (!pts.length) return;

  function toCanvas(p) {
    return {
      x: PAD + p.x * (W - PAD - PAD),
      y: (H - PAD) - p.y * (H - PAD - PAD),
    };
  }

  function drawAllPoints() {
    ctx.fillStyle = "#000";
    for (const p of pts) {
      const q = toCanvas(p);
      ctx.beginPath();
      ctx.arc(q.x, q.y, 3, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  function drawFinalHull() {
    if (hull.length < 2) return;
    ctx.strokeStyle = "#000";
    ctx.lineWidth = 3;
    ctx.beginPath();
    const first = toCanvas(hull[0]);
    ctx.moveTo(first.x, first.y);
    for (let k = 1; k < hull.length; k++) {
      const q = toCanvas(hull[k]);
      ctx.lineTo(q.x, q.y);
    }
    ctx.lineTo(first.x, first.y); // close polygon
    ctx.stroke();
  }

  let i = 0;

  bfsTimer = setInterval(() => {
    clearCanvas();
    drawAllPoints();

    if (i >= steps.length) {
      drawFinalHull();
      outputPrintEl.textContent =
        `Hull complete\n` +
        (data.hullRaw ? data.hullRaw.map(p => `${p.x} ${p.y}`).join("\n") : "");
      stopBfsAnimation();
      return;
    }

    const s = steps[i];
    const stack = s.stack || [];

    // draw current stack (partial hull)
    if (stack.length >= 2) {
      ctx.strokeStyle = "#000";
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

    // highlight candidate point
    if (s.candidate) {
      const c = toCanvas(s.candidate);
      ctx.beginPath();
      ctx.arc(c.x, c.y, 6, 0, Math.PI * 2);
      ctx.strokeStyle = "#000";
      ctx.lineWidth = 2;
      ctx.stroke();
    }

    // optional: print step info in output panel
    // (shows the turn result / pop reason)
    outputPrintEl.textContent =
      `Step ${i + 1}/${steps.length}\n` +
      `${s.phase.toUpperCase()} | ${s.action.toUpperCase()} | cross=${s.cross}\n\n` +
      (data.hullRaw ? data.hullRaw.map(p => `${p.x} ${p.y}`).join("\n") : "");

    i++;
  }, 400);
}


