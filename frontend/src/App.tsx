import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { ThemeProvider } from "@mui/material/styles";
import Home from "./pages/Home";
import theme from "./theme/theme";
import MainLayout from "./layouts/MainLayout";
import ToolRenderer from "./components/ToolRenderer";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import SignUpPage from "./pages/SignUp";
import LoginPage from "./pages/Login";
import { AuthProvider } from "./context/AuthContext";

function App() {
  return (
    <AuthProvider>
      <ThemeProvider theme={theme}>
        <Router>
          <LocalizationProvider dateAdapter={AdapterDayjs}>
            <Routes>
              {/* Routes không dùng MainLayout */}
              <Route path="/signup" element={<SignUpPage />} />
              <Route path="/login" element={<LoginPage />} />

              {/* Routes dùng MainLayout */}
              <Route
                path="*"
                element={
                  <MainLayout>
                    <Routes>
                      <Route path="/" element={<Home />} />
                      <Route path="/tools/:id" element={<ToolRenderer />} />
                      <Route path="*" element={<div>404 Not Found</div>} />
                    </Routes>
                  </MainLayout>
                }
              />
            </Routes>
          </LocalizationProvider>
        </Router>
      </ThemeProvider>
    </AuthProvider>
  );
}

export default App;
