/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        // Verde de hoja: acciones y estados activos
        bio: {
          50: '#f0f9f4',
          100: '#dcf1e4',
          500: '#2f9e63',
          600: '#237d4e',
          700: '#1d6440',
          900: '#123d27'
        },
        // Verde de bosque profundo: marco de la aplicación y panel de ingreso
        bosque: {
          700: '#1d4a33',
          800: '#16392a',
          900: '#112e21',
          950: '#0c2118'
        },
        // Fondo de página (liquen claro), texto y líneas
        liquen: '#edf1ea',
        tinta: '#1c2b22',
        musgo: '#5d6f63',
        linea: '#d8e0d3',
        // Un solo color de aviso, para estados que piden atención
        ambar: { 50: '#fbf5e6', 700: '#8a5a0c' }
      },
      fontFamily: {
        sans: ['"Segoe UI Variable Text"', '"Segoe UI"', 'system-ui', '-apple-system', 'Roboto', 'sans-serif']
      }
    }
  },
  plugins: []
};
