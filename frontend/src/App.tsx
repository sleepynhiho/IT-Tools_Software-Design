import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { ThemeProvider } from "@mui/material/styles";
import Home from "./pages/Home";
import theme from "./theme/theme";
import MainLayout from "./layouts/MainLayout";
import ToolRenderer from "./components/ToolRenderer";

function App() {
  return (
    <ThemeProvider theme={theme}>
      <Router>
        <MainLayout>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/tools/:id" element={<ToolRenderer />} />
            <Route path="*" element={<div>404 Not Found</div>} />
          </Routes>
        </MainLayout>
      </Router>
    </ThemeProvider>
  );
}

export default App;
