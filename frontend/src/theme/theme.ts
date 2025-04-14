import { createTheme } from "@mui/material";

declare module "@mui/material/styles" {
  interface PaletteOptions {
    custom?: {
      icon: string;
    };
  }
}

const theme = createTheme({
  palette: {
    background: {
      default: "#000000", // Nền web màu đen
      paper: "#232323", // Nền tool
    },
    text: {
      primary: "#ffffff", // Màu chữ tên tool
      secondary: "#a3a3a3", // Màu chữ mô tả
    },
    custom: {
      icon: "#525252",
    },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: "#232323", // Nền của các tool
          color: "#ffffff", // Màu chữ trong Card
        },
      },
    },
  },
});

export default theme;
