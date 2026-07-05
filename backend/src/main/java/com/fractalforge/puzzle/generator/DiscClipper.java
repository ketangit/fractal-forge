package com.fractalforge.puzzle.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trims piece geometry to the inner disc of a circular ("coaster") panel so
 * exported SVGs contain no cut lines outside the puzzle circle. Arcs and lines
 * are split exactly at their intersections with the disc boundary; segments
 * that fall outside are discarded.
 *
 * <p>
 * All trigonometry uses {@link StrictMath} so the output is deterministic
 * across platforms, matching the generator's fdlibm-based pipeline.
 */
final class DiscClipper {

	/** Tolerance for point-coincidence checks (millimetres). */
	static final double EPS = 1e-6;

	/** Minimum parametric length of a kept sub-segment (avoids slivers). */
	private static final double MIN_T = 1e-9;

	private static final double TAN225 = 0.4142135623730950488016887242097;

	private DiscClipper() {
	}

	/** A drawable primitive after clipping: a straight line or a circular arc. */
	static final class Seg {
		final boolean isArc;
		final double sx, sy, ex, ey;
		final double r; // arc radius (arc only)
		final int sweep; // SVG sweep flag (arc only)

		private Seg(boolean isArc, double sx, double sy, double ex, double ey, double r, int sweep) {
			this.isArc = isArc;
			this.sx = sx;
			this.sy = sy;
			this.ex = ex;
			this.ey = ey;
			this.r = r;
			this.sweep = sweep;
		}

		static Seg line(double sx, double sy, double ex, double ey) {
			return new Seg(false, sx, sy, ex, ey, 0, 0);
		}

		static Seg arc(double sx, double sy, double ex, double ey, double r, int sweep) {
			return new Seg(true, sx, sy, ex, ey, r, sweep);
		}

		/** SVG path fragment (same format as {@link Arc#svg}). */
		String svg() {
			if (isArc) {
				return "A " + Svg.fmt(r) + " " + Svg.fmt(r) + " 0 0," + sweep + " " + Svg.fmt(ex) + " " + Svg.fmt(ey)
						+ " ";
			}
			return "L " + Svg.fmt(ex) + " " + Svg.fmt(ey) + " ";
		}
	}

	/**
	 * Clips one quarter-arc (rendered per {@code shape}) to the disc centred on
	 * (px, py) with radius R. Returns the kept sub-segments in drawing order; empty
	 * when the whole segment lies outside the disc.
	 */
	static List<Seg> clip(Arc a, TileShape shape, double px, double py, double R) {
		List<Seg> out = new ArrayList<Seg>();
		switch (shape) {
			case CIRCULAR :
				clipQuarterArc(a, px, py, R, out);
				break;
			case SQUARE :
				clipLine(a.spx(), a.spy(), a.epx(), a.epy(), px, py, R, out);
				break;
			case OCTAGONAL :
				double[][] pts = octPoints(a);
				for (int i = 0; i < 3; i++) {
					clipLine(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], px, py, R, out);
				}
				break;
			default :
				throw new IllegalArgumentException("shape " + shape);
		}
		return out;
	}

	/**
	 * The four polyline points of the octagonal rendering of an arc, in drawing
	 * order (mirrors {@link Arc#svg} for {@link TileShape#OCTAGONAL}).
	 */
	private static double[][] octPoints(Arc a) {
		double hlen = a.radius() * TAN225;
		double sx = a.spx(), sy = a.spy(), ex = a.epx(), ey = a.epy();
		if (a.sign() == 1) {
			sx = a.epx();
			sy = a.epy();
			ex = a.spx();
			ey = a.spy();
		}
		double m1x, m1y, m2x, m2y;
		switch (a.quad()) {
			case 0 :
				m1x = sx;
				m1y = sy - hlen;
				m2x = ex + hlen;
				m2y = ey;
				break;
			case 1 :
				m1x = sx - hlen;
				m1y = sy;
				m2x = ex;
				m2y = ey - hlen;
				break;
			case 2 :
				m1x = sx;
				m1y = sy + hlen;
				m2x = ex - hlen;
				m2y = ey;
				break;
			default :
				m1x = sx + hlen;
				m1y = sy;
				m2x = ex;
				m2y = ey + hlen;
				break;
		}
		if (a.sign() == 1) {
			return new double[][]{{a.spx(), a.spy()}, {m2x, m2y}, {m1x, m1y}, {a.epx(), a.epy()}};
		}
		return new double[][]{{a.spx(), a.spy()}, {m1x, m1y}, {m2x, m2y}, {a.epx(), a.epy()}};
	}

	/** Clips the straight segment (sx,sy)-(ex,ey) to the disc. */
	private static void clipLine(double sx, double sy, double ex, double ey, double px, double py, double R,
			List<Seg> out) {
		double dx = ex - sx, dy = ey - sy;
		double a = dx * dx + dy * dy;
		if (a == 0) {
			return;
		}
		double fx = sx - px, fy = sy - py;
		double b = 2 * (fx * dx + fy * dy);
		double c = fx * fx + fy * fy - R * R;
		List<Double> ts = new ArrayList<Double>();
		ts.add(Double.valueOf(0));
		double disc = b * b - 4 * a * c;
		if (disc > 0) {
			double sq = StrictMath.sqrt(disc);
			double t1 = (-b - sq) / (2 * a);
			double t2 = (-b + sq) / (2 * a);
			if (t1 > MIN_T && t1 < 1 - MIN_T) {
				ts.add(Double.valueOf(t1));
			}
			if (t2 > MIN_T && t2 < 1 - MIN_T) {
				ts.add(Double.valueOf(t2));
			}
		}
		ts.add(Double.valueOf(1));
		for (int i = 0; i + 1 < ts.size(); i++) {
			double t0 = ts.get(i).doubleValue();
			double t1 = ts.get(i + 1).doubleValue();
			if (t1 - t0 <= MIN_T) {
				continue;
			}
			double tm = (t0 + t1) / 2;
			double mx = sx + tm * dx - px, my = sy + tm * dy - py;
			if (mx * mx + my * my <= R * R) {
				out.add(Seg.line(lerp(sx, dx, t0), lerp(sy, dy, t0), lerp(sx, dx, t1), lerp(sy, dy, t1)));
			}
		}
	}

	/** Interpolates, returning the exact original endpoints at t = 0 and t = 1. */
	private static double lerp(double s, double d, double t) {
		if (t == 0) {
			return s;
		}
		if (t == 1) {
			return s + d;
		}
		return s + t * d;
	}

	/** Clips the quarter-circle arc to the disc. */
	private static void clipQuarterArc(Arc a, double px, double py, double R, List<Seg> out) {
		double cx = a.cpx(), cy = a.cpy(), r = a.radius();
		double d = StrictMath.hypot(px - cx, py - cy);
		if (d >= R + r || r >= d + R) {
			// The arc's circle lies entirely outside the disc (or encloses it).
			return;
		}
		if (d + r <= R) {
			out.add(Seg.arc(a.spx(), a.spy(), a.epx(), a.epy(), r, a.sign()));
			return;
		}
		// Circle-circle intersection of the arc's circle with the disc boundary.
		double aa = (d * d + r * r - R * R) / (2 * d);
		double h = StrictMath.sqrt(Math.max(0, r * r - aa * aa));
		double ux = (px - cx) / d, uy = (py - cy) / d;
		double mx = cx + aa * ux, my = cy + aa * uy;
		double[] ix = {mx - h * uy, mx + h * uy};
		double[] iy = {my + h * ux, my - h * ux};
		double theta0 = StrictMath.atan2(a.spy() - cy, a.spx() - cx);
		double dir = a.sign() == 1 ? 1.0 : -1.0;
		double span = Math.PI / 2;
		List<Double> ts = new ArrayList<Double>();
		ts.add(Double.valueOf(0));
		for (int i = 0; i < 2; i++) {
			double ang = StrictMath.atan2(iy[i] - cy, ix[i] - cx);
			double off = dir * (ang - theta0);
			while (off < 0) {
				off += 2 * Math.PI;
			}
			while (off >= 2 * Math.PI) {
				off -= 2 * Math.PI;
			}
			double t = off / span;
			if (t > MIN_T && t < 1 - MIN_T) {
				ts.add(Double.valueOf(t));
			}
		}
		ts.add(Double.valueOf(1));
		Collections.sort(ts);
		for (int i = 0; i + 1 < ts.size(); i++) {
			double t0 = ts.get(i).doubleValue();
			double t1 = ts.get(i + 1).doubleValue();
			if (t1 - t0 <= MIN_T) {
				continue;
			}
			double am = theta0 + dir * span * (t0 + t1) / 2;
			double mxp = cx + r * StrictMath.cos(am) - px;
			double myp = cy + r * StrictMath.sin(am) - py;
			if (mxp * mxp + myp * myp <= R * R) {
				double x0, y0, x1, y1;
				if (t0 == 0) {
					x0 = a.spx();
					y0 = a.spy();
				} else {
					double an = theta0 + dir * span * t0;
					x0 = cx + r * StrictMath.cos(an);
					y0 = cy + r * StrictMath.sin(an);
				}
				if (t1 == 1) {
					x1 = a.epx();
					y1 = a.epy();
				} else {
					double an = theta0 + dir * span * t1;
					x1 = cx + r * StrictMath.cos(an);
					y1 = cy + r * StrictMath.sin(an);
				}
				out.add(Seg.arc(x0, y0, x1, y1, r, a.sign()));
			}
		}
	}
}
