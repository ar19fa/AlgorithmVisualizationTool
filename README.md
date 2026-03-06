# Algorithm Visualization Tool — Java/Spring Boot Web App

A full-stack web application designed to help students visualize complex computer science algorithms through interactive animations. Users can upload custom data sets and see step-by-step executions of graph traversals and geometric algorithms.

* **Interactive Visualizations:** Watch algorithms unfold on a `<canvas>` element.
* **Dual-View Interface:** Compare raw "Before" data with processed "After" results.
* **Multiple Algorithms:** Support for Skyline, BFS, DFS, and Convex Hull.
* **Real-time Parsing:** Instantly previews input data upon file selection.

---

## Included Algorithms

* **Skyline Problem:** Renders building coordinates as rectangles and computes the resulting silhouette.
* **BFS (Breadth-First Search):** Animates graph discovery using an adjacency matrix, highlighting edges in breadth-first order.
* **DFS (Iterative Depth-First Search):** Visualizes depth-first traversal with edge-by-edge discovery animations.
* **Convex Hull (Graham Scan):** Plots a set of points and draws the minimal convex polygon that encloses them.

---

## Project Structure

### Backend (Spring Boot)
```text
src/main/java/com/example/demo/
  ├── RunController.java           # API Endpoint (POST /run)
  ├── service/AlgorithmService.java # Orchestrates solver logic
  └── algorithm/                   # Core Logic
        ├── SkylineSolver.java
        ├── BfsSolver.java
        ├── DfsSolver.java
        └── ConvexHullSolver.java
```

### Frontend (Vanilla JS)
```text
  src/main/resources/static/
  ├── index.html   # Main UI structure
  ├── style.css    # Canvas and layout styling
  └── app.js       # Drawing logic and API communication
```

## How to Run

### 1. Start the Server
From the project root (containing `mvnw`):
```bash
./mvnw spring-boot:run
```
### 2. Access the Tool
Open your browser and navigate to: http://localhost:8080

## Input File Formats

The tool accepts `.txt` files in the following structures:

| Algorithm | Format Example | Description |
| :--- | :--- | :--- |
| **Skyline** | `1, 11, 5` | `L, H, R` (Left, Height, Right) |
| **BFS/DFS** | `0 1 0 0` | Adjacency matrix (Space or comma separated) |
| **Convex Hull** | `0 2` | `x y` coordinates |

Tech Stack
Backend: Java 17+, Spring Boot

Frontend: HTML5, CSS3, Vanilla JavaScript (Canvas API)

Build Tool: Maven
## Development & Troubleshooting

* **Adding Algorithms:** To add a new algorithm, implement a new `Solver` class in the backend, add a corresponding `<option>` in `index.html`, and define the drawing logic in `app.js`.
* **Blurry Canvas:** The app uses **Device Pixel Ratio (DPR)** scaling to ensure sharp rendering on high-resolution displays.
* **Connection Errors:** If the "Apply" button fails, check the browser DevTools (F12) **Network** tab to ensure the `/run` endpoint is responding with valid JSON.



---

## Author
**Akshay Rash** Bsc Computer Science, Brock University
