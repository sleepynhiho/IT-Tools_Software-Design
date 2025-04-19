import React, { useState, ChangeEvent, FormEvent, JSX } from "react";
import {
  Container,
  Box,
  TextField,
  Button,
  Typography,
  Avatar,
  CssBaseline,
  SxProps,
  Theme,
  CircularProgress,
} from "@mui/material";

import Grid from "@mui/material/Grid"; // Import Grid for layout
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import { Link as MuiLink } from "@mui/material";
import { AuthError, createUserWithEmailAndPassword } from "firebase/auth";
import { auth } from "../firebaseConfig";
import { useNavigate, Link as RouterLink } from "react-router-dom";

interface FormData {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

interface FormErrors {
  username?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
  general?: string; // Add general error property
}

// The specific gradient you requested
const pageBackgroundGradient: string =
  "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)";

// Define styles using SxProps for type safety
const pageBackgroundStyles: SxProps<Theme> = {
  minHeight: "100vh",
  background: pageBackgroundGradient,
  display: "flex",
  alignItems: "center", // Vertically center
  justifyContent: "center", // Horizontally center
  width: "100%",
};

const formBoxStyles: SxProps<Theme> = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  bgcolor: "background.paper", // White background for the form card
  padding: 4,
  borderRadius: 2,
  boxShadow: 3, // Add some shadow for depth
};

function SignUpPage(): JSX.Element {
  // Explicitly type the return value
  const [formData, setFormData] = useState<FormData>({
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const navigate = useNavigate();

  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState<boolean>(false);

  // Type the validate function's return value
  const validate = (): boolean => {
    // Explicitly type tempErrors
    const tempErrors: FormErrors = {};
    tempErrors.username = !formData.username
      ? "Username is required."
      : undefined; // Use undefined for no error
    tempErrors.email = !/\S+@\S+\.\S+/.test(formData.email)
      ? "Email is not valid."
      : undefined;
    tempErrors.password =
      formData.password.length < 6
        ? "Password must be at least 6 characters long."
        : undefined;
    tempErrors.confirmPassword =
      formData.confirmPassword !== formData.password
        ? "Passwords do not match."
        : undefined;

    // Filter out undefined error messages more type-safely
    const filteredErrors = Object.entries(tempErrors).reduce(
      (acc, [key, value]) => {
        if (value) {
          // Assert key is a valid key for FormErrors
          acc[key as keyof FormErrors] = value;
        }
        return acc;
      },
      {} as FormErrors
    ); // Initialize accumulator with the correct type

    setErrors(filteredErrors);
    // Return true if there are no errors, false otherwise
    return Object.keys(filteredErrors).length === 0;
  };

  // Type the event parameter for handleChange
  const handleChange = (
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    // Ensure 'name' is a key of FormData before updating state
    const { name, value } = event.target;
    if (name in formData) {
      setFormData((prevData) => ({ ...prevData, [name]: value }));

      // Optional: Clear specific error on change, asserting the key type
      const errorKey = name as keyof FormErrors;
      if (errors[errorKey]) {
        setErrors((prevErrors) => ({
          ...prevErrors,
          [errorKey]: undefined, // Remove the error by setting to undefined
        }));
      }

      // Clear confirm password error when password changes
      if (name === "password" && errors.confirmPassword) {
        setErrors((prevErrors) => ({
          ...prevErrors,
          confirmPassword: undefined,
        }));
      }
    } else {
      console.warn(`Input name "${name}" does not exist in FormData state.`);
    }
  };

  const getFirebaseErrorMessage = (errorCode: string): string => {
    switch (errorCode) {
      case "auth/email-already-in-use":
        return "This email address is already registered.";
      case "auth/invalid-email":
        return "Please enter a valid email address.";
      case "auth/weak-password":
        return "Password is too weak (at least 6 characters).";
      // Add other specific codes if needed
      default:
        console.error("Unhandled Firebase Auth Error Code:", errorCode);
        return "An unexpected error occurred. Please try again.";
    }
  };
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrors({}); // Clear previous errors

    // Perform local validation first
    if (!validate()) {
      console.log("Local Validation Failed");
      return;
    }

    setLoading(true); // Start loading indicator

    try {
      console.log(`Attempting Firebase signup for: ${formData.email}`);
      // Use Firebase Authentication to create the user
      const userCredential = await createUserWithEmailAndPassword(
        auth,
        formData.email,
        formData.password
      );

      console.log(
        "Firebase User Created Successfully:",
        userCredential.user.uid
      );

      // Optional TODO: Send user info (like UID, maybe username) to your Spring Boot backend
      // This is where you'd associate the Firebase user with a profile in your own DB.
      // Example: await fetch('/api/users/register', { method: 'POST', ... });

      alert("Sign up successful! Please log in."); // Inform user
      navigate("/login"); // Redirect to login page
    } catch (error) {
      console.error("Firebase Sign Up Error:", error);
      // Handle Firebase specific errors
      if (error instanceof Error && "code" in error) {
        const firebaseError = error as AuthError; // Type assertion
        setErrors({ general: getFirebaseErrorMessage(firebaseError.code) });
      } else {
        // Handle generic errors
        setErrors({ general: "An unknown error occurred during sign up." });
      }
    } finally {
      setLoading(false); // Stop loading indicator regardless of outcome
    }
  };

  return (
    <Box sx={pageBackgroundStyles}>
      <CssBaseline /> {/* Ensures baseline styles & background compatibility */}
      <Container component="main" maxWidth="xs">
        {/* Apply form box styles using typed styles */}
        <Box sx={formBoxStyles}>
          <Avatar sx={{ m: 1, bgcolor: "secondary.main" }}>
            <LockOutlinedIcon />
          </Avatar>
          <Typography component="h1" variant="h5">
            Sign Up
          </Typography>
          <Box
            component="form"
            onSubmit={handleSubmit}
            noValidate
            sx={{ mt: 3, width: "100%" }} // Ensure full width for the form
          >
            <Grid container spacing={2}>
              <Grid item xs={12} sx={{ width: "100%" }}>
                <TextField
                  required
                  fullWidth
                  id="username"
                  label="Username"
                  name={"username" satisfies keyof FormData}
                  autoComplete="username"
                  value={formData.username}
                  onChange={handleChange}
                  error={!!errors.username}
                  helperText={errors.username}
                  autoFocus
                  disabled={loading} 
                />
              </Grid>
              <Grid item xs={12} sx={{ width: "100%" }}>
                <TextField
                  required
                  fullWidth
                  id="email"
                  label="Email Address"
                  name={"email" satisfies keyof FormData}
                  autoComplete="email"
                  value={formData.email}
                  onChange={handleChange}
                  error={!!errors.email}
                  helperText={errors.email}
                />
              </Grid>
              <Grid item xs={12} sx={{ width: "100%" }}>
                <TextField
                  required
                  fullWidth
                  name={"password" satisfies keyof FormData}
                  label="Password"
                  type="password"
                  id="password"
                  autoComplete="new-password"
                  value={formData.password}
                  onChange={handleChange}
                  error={!!errors.password}
                  helperText={errors.password}
                />
              </Grid>
              <Grid item xs={12} sx={{ width: "100%" }}>
                <TextField
                  required
                  fullWidth
                  name={"confirmPassword" satisfies keyof FormData}
                  label="Confirm Password"
                  type="password"
                  id="confirmPassword"
                  autoComplete="new-password"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  error={!!errors.confirmPassword}
                  helperText={errors.confirmPassword}
                />
              </Grid>
            </Grid>
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={loading} // Disable button when loading
              // Optional: Style button to somewhat match the gradient theme
              // style={{ background: '#337d5e' }} // style prop works too
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
              {loading ? "Signing Up..." : "Sign Up"}{" "}
              {/* Change text when loading */}
            </Button>
            <Grid container justifyContent="flex-end">
              <Grid item>
                {/* Use MuiLink for basic link styling, replace with Router Link if using react-router */}
                <MuiLink component={RouterLink} to="/login" variant="body2">
                  Already have an account? Sign in
                </MuiLink>
              </Grid>
            </Grid>
          </Box>
        </Box>
      </Container>
    </Box>
  );
}

export default SignUpPage;
