import { createMDX } from "fumadocs-mdx/next";

const withMDX = createMDX();

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "export",
  basePath: "/jindong",
  assetPrefix: "/jindong",
  images: {
    unoptimized: true,
  },
  reactStrictMode: true,
};

export default withMDX(nextConfig);
