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
  Paper,
  Fade,
  IconButton,
  Grid,
  InputAdornment,
  Divider,
  useTheme,
  alpha,
} from "@mui/material";

import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import HomeIcon from "@mui/icons-material/Home";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import PersonIcon from "@mui/icons-material/Person";
import EmailIcon from "@mui/icons-material/Email";
import { Link as MuiLink } from "@mui/material";
import { AuthError, createUserWithEmailAndPassword } from "firebase/auth";
import { auth } from "../firebaseConfig";
import { useNavigate, Link as RouterLink } from "react-router-dom";

// Current date and time information
const CURRENT_DATE_TIME = "2025-05-06 17:42:16";
const CURRENT_USER_LOGIN = "hanhiho";

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
  general?: string;
}

// The specific gradient you requested
const pageBackgroundGradient: string =
  "linear-gradient(48deg,#1f525a 0%,#337d5e 60%,#108048 100%)";

// Define styles using SxProps for type safety
const pageBackgroundStyles: SxProps<Theme> = {
  minHeight: "100vh",
  background: pageBackgroundGradient,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  p: 3,
  position: "relative",
};

const formBoxStyles = (theme: Theme): SxProps<Theme> => ({
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  bgcolor: alpha("#222", 0.9),
  padding: { xs: 3, sm: 4 },
  borderRadius: 3,
  boxShadow: "0 8px 32px rgba(0,0,0,0.2)",
  backdropFilter: "blur(8px)",
  width: "100%",
  maxWidth: "450px",
  position: "relative",
  overflow: "hidden",
  "&::before": {
    content: '""',
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    height: "4px",
    background: "linear-gradient(90deg, #3b956f, #1ea54c, #3b956f)",
  },
});

const textFieldStyles = (theme: Theme): SxProps<Theme> => ({
  "& .MuiOutlinedInput-root": {
    borderRadius: 2,
    backgroundColor: alpha(theme.palette.background.paper, 0.6),
    transition: theme.transitions.create([
      "border-color",
      "background-color",
      "box-shadow",
    ]),
    "&:hover": {
      backgroundColor: alpha(theme.palette.background.paper, 0.8),
    },
    "&.Mui-focused": {
      backgroundColor: alpha(theme.palette.background.paper, 0.8),
      boxShadow: `0 0 0 2px ${alpha(theme.palette.primary.main, 0.25)}`,
    },
  },
  "& .MuiInputLabel-root.Mui-focused": {
    color: theme.palette.primary.main,
  },
  "& .MuiInputAdornment-root": {
    color: theme.palette.text.secondary,
  },
});

const homeButtonStyles: SxProps<Theme> = {
  position: "absolute",
  top: 16,
  left: 16,
  backgroundColor: "rgba(255, 255, 255, 0.2)",
  backdropFilter: "blur(8px)",
  color: "white",
  "&:hover": {
    backgroundColor: "rgba(255, 255, 255, 0.3)",
  },
};

function SignUpPage(): JSX.Element {
  const theme = useTheme();
  const navigate = useNavigate();

  const [formData, setFormData] = useState<FormData>({
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState<boolean>(false);
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const [showConfirmPassword, setShowConfirmPassword] =
    useState<boolean>(false);

  // Type the validate function's return value
  const validate = (): boolean => {
    const tempErrors: FormErrors = {};
    tempErrors.username = !formData.username
      ? "Username is required."
      : undefined;
    tempErrors.email = !formData.email
      ? "Email is required."
      : !/\S+@\S+\.\S+/.test(formData.email)
      ? "Email is not valid."
      : undefined;
    tempErrors.password = !formData.password
      ? "Password is required."
      : formData.password.length < 6
      ? "Password must be at least 6 characters long."
      : undefined;
    tempErrors.confirmPassword = !formData.confirmPassword
      ? "Please confirm your password."
      : formData.confirmPassword !== formData.password
      ? "Passwords do not match."
      : undefined;

    // Filter out undefined error messages more type-safely
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
      setFormData((prevData) => ({ ...prevData, [name]: value }));

      // Clear specific error on change
      const errorKey = name as keyof FormErrors;
      if (errors[errorKey]) {
        setErrors((prevErrors) => ({
          ...prevErrors,
          [errorKey]: undefined,
        }));
      }

      // Clear confirm password error when password changes
      if (name === "password" && errors.confirmPassword) {
        setErrors((prevErrors) => ({
          ...prevErrors,
          confirmPassword: undefined,
        }));
      }

      // Clear general error
      if (errors.general) {
        setErrors((prevErrors) => ({
          ...prevErrors,
          general: undefined,
        }));
      }
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
      default:
        console.error(
          `[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Unhandled Firebase Auth Error Code:`,
          errorCode
        );
        return "An unexpected error occurred. Please try again.";
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrors({});

    // Perform local validation first
    if (!validate()) {
      console.log(
        `[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Local Validation Failed`
      );
      return;
    }

    setLoading(true);

    try {
      console.log(
        `[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Attempting Firebase signup for: ${formData.email}`
      );
      // Use Firebase Authentication to create the user
      const userCredential = await createUserWithEmailAndPassword(
        auth,
        formData.email,
        formData.password
      );

      console.log(
        `[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Firebase User Created Successfully:`,
        userCredential.user.uid
      );

      // Show success notification and redirect
      alert("Sign up successful! Please log in.");
      navigate("/login");
    } catch (error) {
      console.error(
        `[${CURRENT_DATE_TIME}] ${CURRENT_USER_LOGIN}: Firebase Sign Up Error:`,
        error
      );
      // Handle Firebase specific errors
      if (error instanceof Error && "code" in error) {
        const firebaseError = error as AuthError;
        setErrors({ general: getFirebaseErrorMessage(firebaseError.code) });
      } else {
        setErrors({ general: "An unknown error occurred during sign up." });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={pageBackgroundStyles}>
      <CssBaseline />

      {/* Home button floating in top-left corner */}
      <IconButton
        aria-label="Back to home"
        sx={homeButtonStyles}
        onClick={() => navigate("/")}
      >
        <HomeIcon />
      </IconButton>

      <Fade in={true} timeout={800}>
        <Container component="main" maxWidth="xs">
          <Paper sx={formBoxStyles(theme)} elevation={0}>
            <Avatar
              sx={{
                m: 1,
                width: 56,
                height: 56,
                bgcolor: "primary.main",
                boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
              }}
            >
              <LockOutlinedIcon fontSize="large" />
            </Avatar>

            <Typography
              component="h1"
              variant="h4"
              sx={{
                fontWeight: "bold",
                letterSpacing: 0.5,
                my: 1.5,
              }}
            >
              Sign Up
            </Typography>

            {/* Display general error if it exists */}
            {errors.general && (
              <Fade in={!!errors.general}>
                <Typography
                  color="error"
                  variant="body2"
                  sx={{
                    mt: 1,
                    mb: 2,
                    textAlign: "center",
                    backgroundColor: alpha(theme.palette.error.main, 0.1),
                    borderRadius: 1,
                    padding: "10px 16px",
                    width: "100%",
                    fontWeight: "medium",
                  }}
                >
                  {errors.general}
                </Typography>
              </Fade>
            )}

            <Box
              component="form"
              onSubmit={handleSubmit}
              noValidate
              sx={{ mt: 1.5, width: "100%" }}
            >
              <TextField
                required
                fullWidth
                id="username"
                label="Username"
                name="username"
                autoComplete="username"
                value={formData.username}
                onChange={handleChange}
                error={!!errors.username}
                helperText={errors.username}
                autoFocus
                disabled={loading}
                sx={textFieldStyles(theme)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <PersonIcon fontSize="small" />
                    </InputAdornment>
                  ),
                }}
              />

              <TextField
                required
                fullWidth
                id="email"
                label="Email Address"
                name="email"
                autoComplete="email"
                value={formData.email}
                onChange={handleChange}
                error={!!errors.email}
                helperText={errors.email}
                disabled={loading}
                sx={{
                  ...textFieldStyles(theme),
                  mt: 2,
                }}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <EmailIcon fontSize="small" />
                    </InputAdornment>
                  ),
                }}
              />

              <TextField
                required
                fullWidth
                name="password"
                label="Password"
                type={showPassword ? "text" : "password"}
                id="password"
                autoComplete="new-password"
                value={formData.password}
                onChange={handleChange}
                error={!!errors.password}
                helperText={errors.password}
                disabled={loading}
                sx={{
                  ...textFieldStyles(theme),
                  mt: 2,
                }}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label="toggle password visibility"
                        onClick={() => setShowPassword(!showPassword)}
                        edge="end"
                        size="small"
                      >
                        {showPassword ? (
                          <VisibilityOffIcon fontSize="small" />
                        ) : (
                          <VisibilityIcon fontSize="small" />
                        )}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />

              <TextField
                required
                fullWidth
                name="confirmPassword"
                label="Confirm Password"
                type={showConfirmPassword ? "text" : "password"}
                id="confirmPassword"
                autoComplete="new-password"
                value={formData.confirmPassword}
                onChange={handleChange}
                error={!!errors.confirmPassword}
                helperText={errors.confirmPassword}
                disabled={loading}
                sx={{
                  ...textFieldStyles(theme),
                  mt: 2,
                }}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label="toggle confirm password visibility"
                        onClick={() =>
                          setShowConfirmPassword(!showConfirmPassword)
                        }
                        edge="end"
                        size="small"
                      >
                        {showConfirmPassword ? (
                          <VisibilityOffIcon fontSize="small" />
                        ) : (
                          <VisibilityIcon fontSize="small" />
                        )}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />

              <Button
                type="submit"
                fullWidth
                variant="contained"
                sx={{
                  mt: 3,
                  mb: 2,
                  py: 1.5,
                  position: "relative",
                  fontWeight: "bold",
                  fontSize: "1rem",
                  textTransform: "none",
                  borderRadius: 2,
                  background: "linear-gradient(90deg, #3b956f, #1ea54c)",
                  boxShadow: "0 4px 12px rgba(59, 149, 111, 0.3)",
                  "&:hover": {
                    background: "linear-gradient(90deg, #358b66, #1c9845)",
                    boxShadow: "0 6px 14px rgba(59, 149, 111, 0.4)",
                  },
                }}
                disabled={loading}
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
                {loading ? "Creating Account..." : "Create Account"}
              </Button>

              <Divider sx={{ my: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  OR
                </Typography>
              </Divider>

              <Grid container justifyContent="center">
                <Grid item>
                  <Typography variant="body2">
                    Already have an account?{" "}
                    <MuiLink
                      component={RouterLink}
                      to="/login"
                      sx={{
                        fontWeight: "medium",
                        color: "primary.main",
                        textDecoration: "none",
                        "&:hover": {
                          textDecoration: "underline",
                        },
                      }}
                    >
                      Log In
                    </MuiLink>
                  </Typography>
                </Grid>
              </Grid>
            </Box>
          </Paper>
        </Container>
      </Fade>
    </Box>
  );
}

export default SignUpPage;
