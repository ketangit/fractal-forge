"use client";

import React, { useEffect, useRef } from "react";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import { SVGLoader } from "three/examples/jsm/loaders/SVGLoader.js";
import { pieceColor } from "@/lib/palette";

interface Props {
  widthMm: number;
  heightMm: number;
  piecePaths: string[];
  material?: string;
  /** piece thickness in mm */
  thickness?: number;
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
 */
export default function Puzzle3D({ widthMm, heightMm, piecePaths, material = "BIRCH_PLY", thickness = 3 }: Props) {
  const mountRef = useRef<HTMLDivElement>(null);

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

    // Baseboard (the tray the puzzle sits in)
    const baseGeo = new THREE.BoxGeometry(widthMm, heightMm, 2);
    const baseMat = new THREE.MeshStandardMaterial({
      color: "#8a7259",
      roughness: 0.9,
      side: THREE.DoubleSide, // group has a negative Y scale (SVG y-down)
    });
    const base = new THREE.Mesh(baseGeo, baseMat);
    base.position.set(widthMm / 2, heightMm / 2, -1.01);
    group.add(base);
    disposables.push(baseGeo, baseMat);

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
        color: style.usePalette ? pieceColor(i) : style.color,
        roughness: 0.65,
        metalness: 0.05,
        transparent: style.opacity < 1,
        opacity: style.opacity,
        side: THREE.DoubleSide, // group has a negative Y scale (SVG y-down)
      });
      const mesh = new THREE.Mesh(geometry, mat);
      // tiny random lift so pieces read as individually cut
      mesh.position.z = 0.05 + ((i * 37) % 5) * 0.04;
      group.add(mesh);
      disposables.push(geometry, mat);
    });

    // SVG is y-down; flip to a natural tabletop orientation and center.
    group.scale.set(1, -1, 1);
    group.position.set(-widthMm / 2, heightMm / 2, 0);

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
  }, [widthMm, heightMm, piecePaths, material, thickness]);

  return (
    <div
      ref={mountRef}
      aria-label="Interactive 3D puzzle preview"
      style={{ width: "100%", aspectRatio: "4 / 3", borderRadius: 12, overflow: "hidden", touchAction: "none" }}
    />
  );
}
