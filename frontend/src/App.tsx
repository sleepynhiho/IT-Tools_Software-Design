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

// Create a wrapper component that applies MainLayout
const WithMainLayout = ({ component: Component, ...props }) => {
  return (
    <MainLayout>
      <Component {...props} />
    </MainLayout>
  );
};

function App() {
  return (
    <AuthProvider>
      <ThemeProvider theme={theme}>
        <LocalizationProvider dateAdapter={AdapterDayjs}>
          <Router>
            <Routes>
              {/* Routes without MainLayout */}
              <Route path="/signup" element={<SignUpPage />} />
              <Route path="/login" element={<LoginPage />} />

              {/* Routes with MainLayout */}
              <Route path="/" element={<WithMainLayout component={Home} />} />
              <Route path="/tools/:id" element={<WithMainLayout component={ToolRenderer} />} />
              
              {/* 404 route with MainLayout */}
              <Route path="*" element={<WithMainLayout component={() => <div>404 Not Found</div>} />} />
            </Routes>
          </Router>
        </LocalizationProvider>
      </ThemeProvider>
    </AuthProvider>
  );
}

export default App;