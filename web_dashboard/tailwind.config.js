/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
    "./public/index.html",
  ],
  theme: {
    extend: {
        keyframes: {
            fadeInOut: {
                '0%, 100%': { opacity: 0 },
                '10%, 90%': { opacity: 1 },
            },
            pulseFade: {
                '0%, 100%': { opacity: 0.8 },
                '50%': { opacity: 1 },
            }
        },
        animation: {
            fadeInOut: 'fadeInOut 4s ease-in-out forwards',
            pulseFade: 'pulseFade 2s ease-in-out infinite',
            'ping-slow': 'ping 2s cubic-bezier(0, 0, 0.2, 1) infinite',
        }
    },
  },
  plugins: [],
}
