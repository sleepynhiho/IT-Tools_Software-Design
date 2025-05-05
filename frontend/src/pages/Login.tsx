import React, { useState, ChangeEvent, FormEvent, JSX } from "react";
import { useNavigate, Link as RouterLink } from "react-router-dom";
import {
  Container,
  Box,
  TextField,
  Button,
  Typography,
  Avatar,
  CssBaseline,
  Link as MuiLink, // MUI Link for external or non-router links
  Grid,
  CircularProgress, // For loading state
  SxProps,
  Theme,
} from "@mui/material";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import { AuthError, signInWithEmailAndPassword } from "firebase/auth";
import { auth } from "../firebaseConfig";

// Define the types for form data and errors
interface FormData {
  email: string; // Assuming login with email
  password: string;
}

interface FormErrors {
  email?: string;
  password?: string;
  general?: string; // For general errors like "Invalid credentials"
}

// Re-use the gradient from SignUpPage
const pageBackgroundGradient: string =
  "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)";

// Define styles using SxProps for type safety
const pageBackgroundStyles: SxProps<Theme> = {
  minHeight: "100vh",
  background: pageBackgroundGradient,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  p: 2,
};

const formBoxStyles: SxProps<Theme> = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  bgcolor: "background.paper",
  padding: 4,
  borderRadius: 2,
  boxShadow: 3,
};

function LoginPage(): JSX.Element {
  const navigate = useNavigate();
  const [formData, setFormData] = useState<FormData>({
    email: "",
    password: "",
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState<boolean>(false);

  const validate = (): boolean => {
    const tempErrors: FormErrors = {};
    tempErrors.email = !formData.email
      ? "Email is required."
      : !/\S+@\S+\.\S+/.test(formData.email)
      ? "Email is not valid."
      : undefined;
    tempErrors.password = !formData.password
      ? "Password is required."
      : undefined;

    const filteredErrors = Object.entries(tempErrors).reduce(
      (acc, [key, value]) => {
        if (value) {
          acc[key as keyof FormErrors] = value;
        }
        return acc;
      },
      {} as FormErrors
    );

    setErrors(filteredErrors);
    return Object.keys(filteredErrors).length === 0;
  };

  const handleChange = (
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = event.target;

    if (name in formData) {
      setFormData((prevData) => ({
        ...prevData,
        [name]: value,
      }));

      // Clear specific field error on change
      const errorKey = name as keyof FormErrors;
      if (errors[errorKey]) {
        setErrors((prevErrors) => ({
          ...prevErrors,
          [errorKey]: undefined,
        }));
      }
      // Also clear general error when user types again
      if (errors.general) {
        setErrors((prevErrors) => ({ ...prevErrors, general: undefined }));
      }
    }
  };

  const getFirebaseErrorMessage = (code: string): string => {
    switch (code) {
      case "auth/user-not-found":
        return "No account found with this email address.";
      case "auth/wrong-password":
        return "Incorrect password. Please try again.";
      case "auth/invalid-email":
        return "Please enter a valid email address.";
      case "auth/user-disabled":
        return "This account has been disabled. Please contact support.";
      case "auth/too-many-requests":
        return "Too many failed login attempts. Please try again later.";
      default:
        console.error("Unhandled Firebase Auth Error Code:", code);
        return "Login failed. Please check your credentials and try again.";
    }
  };

  // --- Updated handleSubmit for Firebase ---
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrors({}); // Clear previous errors

    if (!validate()) {
      console.log("Local Validation Failed");
      return;
    }

    setLoading(true);

    try {
      console.log(`Attempting Firebase login for: ${formData.email}`);
      // Use Firebase Authentication to sign in
      const userCredential = await signInWithEmailAndPassword(
        auth,
        formData.email,
        formData.password
      );

      console.log(
        "Firebase User Logged In Successfully:",
        userCredential.user.uid
      );

      // User is logged in client-side by Firebase now.
      // The AuthProvider (if you set one up) will detect this change.
      // TODO: Ensure you have an AuthProvider listening to onAuthStateChanged.

      // Redirect to the main application area
      navigate("/"); // Or navigate('/dashboard') or wherever appropriate
    } catch (error) {
      console.error("Firebase Login Error:", error);
      if (error instanceof Error && "code" in error) {
        const firebaseError = error as AuthError;
        setErrors({ general: getFirebaseErrorMessage(firebaseError.code) });
      } else {
        setErrors({ general: "An unknown error occurred during login." });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={pageBackgroundStyles}>
      <CssBaseline />
      <Container component="main" maxWidth="xs">
        <Box sx={formBoxStyles}>
          <Avatar sx={{ m: 1, bgcolor: "secondary.main" }}>
            <LockOutlinedIcon />
          </Avatar>
          <Typography component="h1" variant="h5">
            Log In
          </Typography>

          {/* Display general errors */}
          {errors.general && (
            <Typography
              color="error"
              variant="body2"
              sx={{ mt: 2, textAlign: "center" }}
            >
              {errors.general}
            </Typography>
          )}

          <Box
            component="form"
            onSubmit={handleSubmit}
            noValidate
            sx={{ mt: 1, width: "100%" }}
          >
            {/* Email Field */}
            <TextField
              margin="normal"
              required
              fullWidth
              id="email"
              label="Email Address"
              name="email"
              autoComplete="email"
              autoFocus
              value={formData.email}
              onChange={handleChange}
              error={!!errors.email}
              helperText={errors.email}
              disabled={loading} // Disable during loading
            />
            {/* Password Field */}
            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label="Password"
              type="password"
              id="password"
              autoComplete="current-password"
              value={formData.password}
              onChange={handleChange}
              error={!!errors.password}
              helperText={errors.password}
              disabled={loading} // Disable during loading
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              disabled={loading}
              sx={{ mt: 3, mb: 2, position: "relative" }}
            >
              {/* Loading Spinner */}
              {loading && (
                <CircularProgress
                  size={24}
                  sx={{
                    color: "primary.contrastText",
                    position: "absolute",
                    top: "50%",
                    left: "50%",
                    marginTop: "-12px",
                    marginLeft: "-12px",
                  }}
                />
              )}
              {loading ? "Logging In..." : "Log In"}{" "}
              {/* Change text when loading */}
            </Button>
            <Grid container>
              <Grid item xs>
                {/* TODO: Implement Forgot Password flow if needed */}
                <MuiLink href="#" variant="body2">
                  Forgot password?
                </MuiLink>
              </Grid>
              <Grid item>
                {/* Use RouterLink for internal navigation */}
                <MuiLink component={RouterLink} to="/signup" variant="body2">
                  {"Don't have an account? Sign Up"}
                </MuiLink>
              </Grid>
            </Grid>
          </Box>
        </Box>
      </Container>
    </Box>
  );
}

export default LoginPage;