/** @type {import('next').NextConfig} */
const nextConfig = {
  // Static export served by the Spring Boot container.
  output: "export",
  images: { unoptimized: true },
};

export default nextConfig;
