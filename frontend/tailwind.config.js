/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        sage: {
          50: '#E7F5DC',
          100: '#CFE1B9',
          200: '#B6C99B',
          300: '#98A77C',
          400: '#88976C',
          500: '#728156',
          600: '#5F6D44',
          700: '#4B5633',
          800: '#353D23',
          900: '#1F2617',
        },
        olive: {
          deep: '#1F2617',
          forest: '#728156',
          mid: '#88976C',
          soft: '#98A77C',
          tint: '#B6C99B',
          light: '#CFE1B9',
          pale: '#E7F5DC',
        },
        crimson: {
          DEFAULT: '#C63D3D',
          light: '#FDECEC',
        }
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'Inter', 'system-ui', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
