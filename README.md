Algorithm Visualization Tool
Skyline • BFS • DFS • Convex Hull (Graham Scan)

Overview
This repository contains a small Java web app that helps students visualize algorithms. A student chooses an algorithm from a dropdown, uploads an input file, and clicks Apply. The canvas shows a BEFORE view (the raw input) and then an AFTER view (the algorithm output).
Algorithms included
•	Skyline Problem — draws input buildings as rectangles, then draws the computed skyline polyline.
•	BFS (Breadth-First Search) — draws the input graph (from an adjacency matrix), then animates BFS tree edges in discovery order.
•	DFS (Iterative Depth-First Search) — same graph input; animates DFS tree edges in the order edges are discovered.
•	Convex Hull (Graham Scan) — draws the input points, then draws the hull polygon.
Tech stack
•	Backend: Spring Boot (Java) — endpoint POST /run that accepts multipart form-data.
•	Frontend: HTML + CSS + vanilla JavaScript — uses <canvas> for drawing/animation.
•	Build tool: Maven (via mvnw).
Project structure (typical)
demo/
  src/main/java/com/example/demo/
    RunController.java
    service/AlgorithmService.java
    algorithm/
      SkylineSolver.java
      BfsSolver.java
      DfsSolver.java
      ConvexHullSolver.java
  src/main/resources/static/
    index.html
    style.css
    app.js
  pom.xml
  mvnw / mvnw.cmd
How to run
From the project folder (the one containing mvnw and pom.xml):
./mvnw spring-boot:run
Then open in your browser:
http://localhost:8080
Using the web app
•	Choose an algorithm in the dropdown.
•	Choose an input file.
•	The canvas immediately shows the INPUT (before).
•	Click Apply to run the algorithm on the backend and show the OUTPUT (after).
•	The right panel prints the input text and the output values in a readable format.
Input file formats
1) Skyline input format
First line is the number of buildings (can be ignored by the parser). Each following line is:
L, H, R
Example:
10
1,11,5
2,6,7
3,13,9
12,7,16
14,3,25
19,18,22
23,13,29
24,4,28
26,8,35
30,5,40
2) BFS / DFS input format (Adjacency matrix)
First line is n (number of vertices). Next n lines are n numbers (0/1) separated by spaces (or commas).
Example (n=4):
4
0 1 0 0
1 0 1 1
0 1 0 0
0 1 0 0
3) Convex Hull input format
First line is the number of points (can be ignored by the parser). Each following line is:
x y
Example (8 points):
8
0 0
1 0
2 1
1 2
0 2
2 2
3 1
3 0
Backend API (for the frontend)
The frontend sends a multipart POST to /run:
FormData:
  algorithm = SKYLINE | BFS | DFS | HULL
  file      = uploaded input file
The backend returns JSON. The exact fields depend on the algorithm; examples:
Skyline JSON
{
  "meta": { "minX": 1, "maxX": 40, "maxY": 18 },
  "points": [ { "x": 0.0, "y": 0.6111 }, ... ]
}
BFS / DFS JSON
{
  "algo": "BFS",
  "n": 10,
  "source": 0,
  "order": [0, 5, 6, 7, 2, 8, 1, 3, 4, 9],
  "edges": [[0,5], [0,6], [0,7], [5,2], ...]
}
Convex Hull JSON
{
  "algo": "HULL",
  "meta": { "minX": 0, "maxX": 3, "minY": 0, "maxY": 2 },
  "inputPoints": [{"x":0.0,"y":0.0}, ...],  // normalized 0..1
  "hullPoints":  [{"x":0.0,"y":0.0}, ...],  // normalized 0..1
  "inputRaw": [{"x":0,"y":0}, ...],
  "hullRaw":  [{"x":0,"y":0}, ...]
}
Frontend drawing/animation (high-level)
•	On file selection (change event): parse the raw text and draw the INPUT view.
•	On Apply (click): fetch /run, print readable output, and animate/draw the OUTPUT view.
•	BFS/DFS animation: reveal one traversal edge every ~400ms using setInterval.
•	Hull animation: draw all points, then gradually draw the hull polyline edges until closed.
Adding another algorithm later
•	Backend: create a new solver in src/main/java/.../algorithm/ and return JSON from AlgorithmService.
•	Frontend: add a new <option> in index.html and add a new branch in app.js to draw/animate.
•	Keep the JSON shape consistent: include raw output values for printing, plus normalized values for canvas drawing.
Troubleshooting
•	If localhost shows Whitelabel/404: confirm index.html is under src/main/resources/static/ and the app is running on port 8080.
•	If Apply seems to do nothing: open browser DevTools console and look for JS errors, and check the Network tab for the /run request.
•	If VS Code asks to sync classpath after pom.xml changes: click Yes (it keeps Java tooling in sync).
•	If your canvas looks blurry: keep the DPR scaling pattern (canvas width/height in device pixels and ctx.setTransform(dpr,...)).
