/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        courier: ['"Courier Prime"', "monospace"],
        montserrat: ["Montserrat", "sans-serif"],
      },
      backgroundImage: {
        "custom-radial":
          "radial-gradient(circle at center, var(--tw-gradient-stops))",
      },
    },
  },
  plugins: ["prettier-plugin-tailwindcss"],
};
