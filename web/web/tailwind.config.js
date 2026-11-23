/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        bio: {
          50: '#f0f9f4',
          100: '#dcf1e4',
          500: '#2f9e63',
          600: '#237d4e',
          700: '#1d6440',
          900: '#123d27'
        }
      }
    }
  },
  plugins: []
};
