"use client";

import React, { useEffect, useRef } from "react";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import { SVGLoader } from "three/examples/jsm/loaders/SVGLoader.js";
import { BARK_BROWN, pieceColor } from "@/lib/palette";

interface CircleClip {
  /** Disc centre in SVG/piece coordinates. */
  cx: number;
  cy: number;
  /** Outer (coaster) radius and inner (puzzle area) radius, in mm. */
  outerR: number;
  innerR: number;
}

interface Props {
  widthMm: number;
  heightMm: number;
  piecePaths: string[];
  material?: string;
  /** piece thickness in mm */
  thickness?: number;
  /** When set, the panel is a round coaster: pieces are clipped to the disc. */
  clip?: CircleClip;
  /** Per-piece colours (adjacency-aware). Falls back to the index palette. */
  pieceColors?: string[];
}

const MATERIAL_STYLE: Record<string, { color?: string; opacity: number; usePalette: boolean }> = {
  BIRCH_PLY: { opacity: 1, usePalette: true },
  WALNUT: { color: "#5d4030", opacity: 1, usePalette: false },
  ACRYLIC_CLEAR: { color: "#bcd8e8", opacity: 0.45, usePalette: false },
  ACRYLIC_BLACK: { color: "#1b1b1f", opacity: 1, usePalette: false },
};

/**
 * Interactive 3D preview: every generated piece outline is extruded to its
 * physical thickness and laid on a baseboard. Drag to orbit, scroll/pinch
 * to zoom — works with mouse and touch.
 *
 * For a circular ("coaster") panel the baseboard is a disc and the pieces are
 * clipped to the puzzle circle with a ring of radial clipping planes (a 96-gon
 * that reads as a clean circle).
 */
export default function Puzzle3D({
  widthMm,
  heightMm,
  piecePaths,
  material = "BIRCH_PLY",
  thickness = 3,
  clip,
  pieceColors,
}: Props) {
  const mountRef = useRef<HTMLDivElement>(null);

  // Primitive deps so the effect only re-runs when the disc actually changes.
  const clipCx = clip?.cx;
  const clipCy = clip?.cy;
  const clipOuterR = clip?.outerR;
  const clipInnerR = clip?.innerR;

  useEffect(() => {
    const mount = mountRef.current;
    if (!mount) return;

    const scene = new THREE.Scene();
    scene.background = new THREE.Color("#26211c");

    const camera = new THREE.PerspectiveCamera(40, 1, 1, 4000);
    const maxDim = Math.max(widthMm, heightMm);
    camera.position.set(0, -maxDim * 1.1, maxDim * 0.9);
    camera.up.set(0, 0, 1);

    const renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.localClippingEnabled = !!clip;
    mount.appendChild(renderer.domElement);

    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.autoRotate = true;
    controls.autoRotateSpeed = 1.2;

    scene.add(new THREE.AmbientLight(0xffffff, 0.65));
    const key = new THREE.DirectionalLight(0xffffff, 1.4);
    key.position.set(-maxDim, -maxDim, maxDim * 1.5);
    scene.add(key);
    const fill = new THREE.DirectionalLight(0xfff2dd, 0.5);
    fill.position.set(maxDim, maxDim * 0.5, maxDim);
    scene.add(fill);

    const group = new THREE.Group();
    scene.add(group);
    const disposables: Array<{ dispose: () => void }> = [];

    // Baseboard (the tray / coaster the puzzle sits on)
    const baseMat = new THREE.MeshStandardMaterial({
      color: BARK_BROWN,
      roughness: 0.9,
      side: THREE.DoubleSide, // group has a negative Y scale (SVG y-down)
    });
    let baseGeo: THREE.BufferGeometry;
    const base = new THREE.Mesh();
    if (clip) {
      // A disc lying in the XY plane (cylinder axis rotated onto Z).
      baseGeo = new THREE.CylinderGeometry(clip.outerR, clip.outerR, 2, 96);
      baseGeo.rotateX(Math.PI / 2);
      base.position.set(clip.cx, clip.cy, -1.01);
    } else {
      baseGeo = new THREE.BoxGeometry(widthMm, heightMm, 2);
      base.position.set(widthMm / 2, heightMm / 2, -1.01);
    }
    base.geometry = baseGeo;
    base.material = baseMat;
    group.add(base);
    disposables.push(baseGeo, baseMat);

    // Centre the whole group on the OrbitControls target (0,0,0). For a square
    // panel that is the canvas centre; for a circular coaster it must be the
    // DISC centre (grid centre), otherwise the view orbits about the puzzle's
    // edge instead of its middle.
    const groupPosX = clip ? -clip.cx : -widthMm / 2;
    const groupPosY = clip ? clip.cy : heightMm / 2;

    // Radial clipping planes approximating the puzzle disc, in world space.
    // With the group centred on the disc, the disc centre maps to world origin.
    const clipPlanes: THREE.Plane[] = [];
    if (clip) {
      const worldCx = clip.cx + groupPosX;
      const worldCy = -clip.cy + groupPosY;
      const N = 96;
      for (let k = 0; k < N; k++) {
        const a = (k / N) * Math.PI * 2;
        const normal = new THREE.Vector3(-Math.cos(a), -Math.sin(a), 0); // inward
        const px = worldCx + clip.innerR * Math.cos(a);
        const py = worldCy + clip.innerR * Math.sin(a);
        clipPlanes.push(new THREE.Plane(normal, -(normal.x * px + normal.y * py)));
      }
    }

    // Pieces: parse the real generated outlines and extrude them
    const style = MATERIAL_STYLE[material] ?? MATERIAL_STYLE.BIRCH_PLY;
    const loader = new SVGLoader();
    const svgText =
      `<svg xmlns="http://www.w3.org/2000/svg">` +
      piecePaths.map((d) => `<path d="${d}"/>`).join("") +
      `</svg>`;
    const parsed = loader.parse(svgText);

    parsed.paths.forEach((path, i) => {
      const shapes = SVGLoader.createShapes(path);
      if (!shapes.length) return;
      const geometry = new THREE.ExtrudeGeometry(shapes, {
        depth: thickness,
        bevelEnabled: false,
        curveSegments: 8,
      });
      const mat = new THREE.MeshStandardMaterial({
        color: pieceColors?.[i] ?? (style.usePalette ? pieceColor(i) : style.color),
        roughness: 0.65,
        metalness: 0.05,
        transparent: style.opacity < 1,
        opacity: style.opacity,
        side: THREE.DoubleSide, // group has a negative Y scale (SVG y-down)
        clippingPlanes: clipPlanes.length ? clipPlanes : undefined,
      });
      const mesh = new THREE.Mesh(geometry, mat);
      // tiny random lift so pieces read as individually cut
      mesh.position.z = 0.05 + ((i * 37) % 5) * 0.04;
      group.add(mesh);
      disposables.push(geometry, mat);
    });

    // SVG is y-down; flip to a natural tabletop orientation and center.
    group.scale.set(1, -1, 1);
    group.position.set(groupPosX, groupPosY, 0);

    controls.target.set(0, 0, 0);

    const resize = () => {
      const w = mount.clientWidth;
      const h = mount.clientHeight || Math.round(w * 0.75);
      renderer.setSize(w, h);
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
    };
    resize();
    const observer = new ResizeObserver(resize);
    observer.observe(mount);

    let frameId = 0;
    const animate = () => {
      frameId = requestAnimationFrame(animate);
      controls.update();
      renderer.render(scene, camera);
    };
    animate();

    return () => {
      cancelAnimationFrame(frameId);
      observer.disconnect();
      controls.dispose();
      disposables.forEach((d) => d.dispose());
      renderer.dispose();
      if (renderer.domElement.parentElement === mount) {
        mount.removeChild(renderer.domElement);
      }
    };
  }, [widthMm, heightMm, piecePaths, material, thickness, clip, clipCx, clipCy, clipOuterR, clipInnerR, pieceColors]);

  return (
    <div
      ref={mountRef}
      aria-label="Interactive 3D puzzle preview"
      style={{ width: "100%", aspectRatio: "4 / 3", borderRadius: 12, overflow: "hidden", touchAction: "none" }}
    />
  );
}
