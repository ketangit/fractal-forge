import type { Metadata, Viewport } from "next";
import "./globals.css";
import { CartProvider } from "@/components/CartContext";
import SiteNav from "@/components/SiteNav";

export const metadata: Metadata = {
  title: "FractalForge — laser-cut fractal jigsaw puzzles",
  description:
    "Procedurally generated fractal jigsaw puzzles, laser-cut in wood and acrylic. Design your own or pick a classic.",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <CartProvider>
          <SiteNav />
          <main>{children}</main>
          <footer className="site-footer">
            FractalForge — every puzzle is generated, none are alike. Cut to order in wood &amp; acrylic.
          </footer>
        </CartProvider>
      </body>
    </html>
  );
}
