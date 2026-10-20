const isDev = process.env.NODE_ENV === "development";

// Upload da foto do prato: o browser envia o arquivo direto à API do Vercel Blob
// (ver components/FotoUploadField.tsx). A imagem servida depois já cabe em img-src (https:).
const blobApiUrl = "https://vercel.com/api/blob/";

const cspHeader = `
  default-src 'self';
  script-src 'self' 'unsafe-inline'${isDev ? " 'unsafe-eval'" : ""};
  style-src 'self' 'unsafe-inline';
  img-src 'self' https: data:;
  font-src 'self';
  connect-src 'self' ${blobApiUrl} ${isDev ? "http://localhost:8080" : ""};
`;

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  // Permite que o servidor de dev do Next.js aceite conexões vindas do Cloudflare Tunnel

  reactStrictMode: true,
  poweredByHeader: false,
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "**.googleusercontent.com" },
      { protocol: "https", hostname: "images.unsplash.com" },
      { protocol: "https", hostname: "work-postposted-elderly-indicating.trycloudflare.com" },
    ],
  },
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
          {
            key: "Content-Security-Policy",
            value: cspHeader.replace(/\s{2,}/g, " ").trim(),
          },
        ],
      },
    ];
  },
};

module.exports = nextConfig;